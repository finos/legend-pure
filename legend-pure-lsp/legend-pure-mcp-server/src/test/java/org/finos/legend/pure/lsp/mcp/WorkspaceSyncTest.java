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

package org.finos.legend.pure.lsp.mcp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.finos.legend.pure.lsp.LegendPureSession;
import org.finos.legend.pure.lsp.RepositoryScanner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class WorkspaceSyncTest
{
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Path repoDir;
    private RepositoryScanner scanner;
    private WorkspaceSync sync;

    @Before
    public void setUp() throws IOException
    {
        Path workspaceRoot = this.tmp.getRoot().toPath();
        Path resourcesDir = workspaceRoot.resolve("module/src/main/resources");
        this.repoDir = resourcesDir.resolve("sync_test_repo");
        Files.createDirectories(this.repoDir.resolve("model"));
        Files.write(resourcesDir.resolve("sync_test_repo.definition.json"),
                ("{\"name\":\"sync_test_repo\","
                        + "\"pattern\":\"(test::sync)(::.*)?\","
                        + "\"dependencies\":[\"platform\"]}").getBytes(StandardCharsets.UTF_8));
        writePure("model/A.pure", "Class test::sync::A\n{\n  name: String[1];\n}\n");

        this.scanner = new RepositoryScanner();
        this.scanner.scan(Collections.singletonList(workspaceRoot));
        this.sync = new WorkspaceSync(this.scanner);
        this.sync.seed();
    }

    private void writePure(String relativePath, String content) throws IOException
    {
        Path file = this.repoDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void noChangesAfterSeed() throws IOException
    {
        Assert.assertTrue(this.sync.computeChanges().isEmpty());
    }

    @Test
    public void modifiedFileDetected() throws IOException
    {
        String newContent = "Class test::sync::A\n{\n  name: String[1];\n  age: Integer[1];\n}\n";
        writePure("model/A.pure", newContent);

        List<LegendPureSession.FileChange> changes = this.sync.computeChanges();
        Assert.assertEquals(1, changes.size());
        Assert.assertEquals("/sync_test_repo/model/A.pure", changes.get(0).getSourceId());
        Assert.assertEquals(newContent, changes.get(0).getContent());
        Assert.assertEquals(LegendPureSession.FileChangeType.CREATE_OR_MODIFY, changes.get(0).getType());
    }

    @Test
    public void newAndDeletedFilesDetected() throws IOException
    {
        writePure("model/B.pure", "Class test::sync::B\n{\n}\n");
        Files.delete(this.repoDir.resolve("model/A.pure"));

        List<LegendPureSession.FileChange> changes = this.sync.computeChanges();
        Assert.assertEquals(2, changes.size());

        LegendPureSession.FileChange created = null;
        LegendPureSession.FileChange deleted = null;
        for (LegendPureSession.FileChange change : changes)
        {
            if (change.getType() == LegendPureSession.FileChangeType.CREATE_OR_MODIFY)
            {
                created = change;
            }
            else
            {
                deleted = change;
            }
        }
        Assert.assertNotNull(created);
        Assert.assertEquals("/sync_test_repo/model/B.pure", created.getSourceId());
        Assert.assertNotNull(deleted);
        Assert.assertEquals("/sync_test_repo/model/A.pure", deleted.getSourceId());
    }

    @Test
    public void computeChangesIsIdempotentUntilMarkApplied() throws IOException
    {
        writePure("model/A.pure", "Class test::sync::A\n{\n  renamed: String[1];\n}\n");

        List<LegendPureSession.FileChange> first = this.sync.computeChanges();
        Assert.assertEquals("computeChanges must not mutate state", 1, this.sync.computeChanges().size());

        this.sync.markApplied(first);
        Assert.assertTrue("After markApplied the change is known", this.sync.computeChanges().isEmpty());
    }
}
