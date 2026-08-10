// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.pure.lsp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import org.finos.legend.pure.lsp.protocol.DriftChangeType;
import org.finos.legend.pure.lsp.protocol.WorkspaceDriftEntry;
import org.finos.legend.pure.lsp.protocol.WorkspaceDriftEvent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class WorkspaceDriftWatcherTest
{
    private static final long AWAIT_TIMEOUT_MS = 15_000;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private RepositoryScanner scanner;
    private UriMapper uriMapper;
    private Path resourcesRoot;
    private final CopyOnWriteArrayList<WorkspaceDriftEvent> published = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<String> openUris = new CopyOnWriteArrayList<>();
    private WorkspaceDriftWatcher watcher;

    @Before
    public void setUp() throws IOException
    {
        // The .definition.json lives directly under src/main/resources (a sibling of the repo-named
        // directory), NOT inside it - RepositoryScanner maps the repo name to the definition file's
        // *parent*, and sourceIds are derived relative to that. Getting this one level wrong silently
        // derives sourceIds without the "/myrepo/" prefix instead of failing loudly.
        File resourcesDir = tempFolder.newFolder("module", "src", "main", "resources");
        File repoDir = new File(resourcesDir, "myrepo");
        Assert.assertTrue(repoDir.mkdirs());
        try (FileWriter w = new FileWriter(new File(resourcesDir, "myrepo.definition.json")))
        {
            w.write("{\"name\": \"myrepo\"}");
        }
        this.resourcesRoot = repoDir.toPath();

        this.scanner = new RepositoryScanner();
        this.scanner.scan(Collections.singletonList(tempFolder.getRoot().toPath()));
        Assert.assertEquals(resourcesDir.toPath(), this.scanner.getMappings().get("myrepo"));

        this.uriMapper = new UriMapper();
        this.uriMapper.setRepositoryScanner(this.scanner);

        this.watcher = new WorkspaceDriftWatcher(this.scanner, this.uriMapper,
                this.openUris::contains, this.published::add);
    }

    @After
    public void tearDown()
    {
        if (this.watcher != null)
        {
            this.watcher.stop();
        }
    }

    @Test
    public void detectsNewFileAsCreated() throws Exception
    {
        this.watcher.start(this.scanner.getMappings().values());

        writeFile("New.pure", "Class my::New {}");

        awaitUntil(() -> !this.published.isEmpty(), AWAIT_TIMEOUT_MS);
        WorkspaceDriftEntry entry = onlyEntry();
        Assert.assertEquals("created", entry.getChangeType());
        Assert.assertTrue(entry.getUri().endsWith("New.pure"));
        Assert.assertTrue(this.watcher.getDirtySourceIds().contains("/myrepo/New.pure"));
    }

    @Test
    public void detectsContentChangeAsModified() throws Exception
    {
        writeFile("Existing.pure", "Class my::Existing {}");
        this.watcher.start(this.scanner.getMappings().values());

        writeFile("Existing.pure", "Class my::Existing { prop: String[1]; }");

        awaitUntil(() -> !this.published.isEmpty(), AWAIT_TIMEOUT_MS);
        WorkspaceDriftEntry entry = onlyEntry();
        Assert.assertEquals("modified", entry.getChangeType());
    }

    @Test
    public void detectsDeletionAsDeleted() throws Exception
    {
        File file = writeFile("Gone.pure", "Class my::Gone {}");
        this.watcher.start(this.scanner.getMappings().values());

        Assert.assertTrue(file.delete());

        awaitUntil(() -> !this.published.isEmpty(), AWAIT_TIMEOUT_MS);
        WorkspaceDriftEntry entry = onlyEntry();
        Assert.assertEquals("deleted", entry.getChangeType());
    }

    @Test
    public void ignoresNonPureFiles() throws Exception
    {
        this.watcher.start(this.scanner.getMappings().values());

        writeFile("readme.txt", "not pure source");
        // Give the watcher a real chance to (incorrectly) publish before concluding it didn't.
        Thread.sleep(1_500);

        Assert.assertTrue(this.published.isEmpty());
    }

    @Test
    public void excludesOpenDocumentsFromPublishedDriftButKeepsThemTrackedInternally() throws Exception
    {
        File file = writeFile("Open.pure", "Class my::Open {}");
        String uri = file.toURI().toString();
        this.openUris.add(uri);
        this.watcher.start(this.scanner.getMappings().values());

        writeFile("Open.pure", "Class my::Open { prop: String[1]; }");
        // Also touch a second, non-open file so we have a positive signal that publishing did happen
        // and simply chose to exclude the open one, rather than the watcher having missed everything.
        writeFile("NotOpen.pure", "Class my::NotOpen {}");

        awaitUntil(() -> !this.published.isEmpty(), AWAIT_TIMEOUT_MS);
        for (WorkspaceDriftEvent event : this.published)
        {
            for (WorkspaceDriftEntry entry : event.getEntries())
            {
                Assert.assertNotEquals(uri, entry.getUri());
            }
        }
        Assert.assertTrue(this.watcher.getDirtySourceIds().contains("/myrepo/Open.pure"));
    }

    @Test
    public void clearRemovesEntriesFromDirtySet() throws Exception
    {
        this.watcher.start(this.scanner.getMappings().values());
        writeFile("ToClear.pure", "Class my::ToClear {}");

        awaitUntil(() -> this.watcher.getDirtySourceIds().contains("/myrepo/ToClear.pure"), AWAIT_TIMEOUT_MS);
        this.watcher.clear(Collections.singleton("/myrepo/ToClear.pure"));

        Assert.assertFalse(this.watcher.getDirtySourceIds().contains("/myrepo/ToClear.pure"));
    }

    private File writeFile(String name, String content) throws IOException
    {
        File file = this.resourcesRoot.resolve(name).toFile();
        try (FileWriter w = new FileWriter(file))
        {
            w.write(content);
        }
        return file;
    }

    private WorkspaceDriftEntry onlyEntry()
    {
        Assert.assertEquals(1, this.published.size());
        List<WorkspaceDriftEntry> entries = this.published.get(0).getEntries();
        Assert.assertEquals(1, entries.size());
        return entries.get(0);
    }

    private static void awaitUntil(BooleanSupplier condition, long timeoutMs) throws InterruptedException
    {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline)
        {
            if (condition.getAsBoolean())
            {
                return;
            }
            Thread.sleep(50);
        }
        Assert.assertTrue("Timed out waiting for condition", condition.getAsBoolean());
    }
}
