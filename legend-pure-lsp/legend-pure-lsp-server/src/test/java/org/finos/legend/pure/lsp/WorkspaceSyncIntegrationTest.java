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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentItem;
import org.finos.legend.pure.lsp.protocol.SyncWorkspaceParams;
import org.finos.legend.pure.lsp.protocol.SyncWorkspaceResult;
import org.junit.Assert;
import org.junit.Test;

/**
 * Exercises legend/syncWorkspace end to end against a real compiled session. Explicitly passes the uri
 * to sync in every test rather than relying on {@link WorkspaceDriftWatcher} to have detected the disk
 * change first - watcher detection/debounce timing is covered separately in
 * {@link WorkspaceDriftWatcherTest}, so this stays focused on "given a change, does sync apply it and
 * report it correctly".
 */
public class WorkspaceSyncIntegrationTest
{
    private static final String DEFINITION = "{\"name\":\"sync_repo\","
            + "\"pattern\":\"(test::sync)(::.*)?\","
            + "\"dependencies\":[\"platform\"]}";

    @Test
    public void syncAppliesModifiedFileAndReportsIt() throws Exception
    {
        Path workspaceRoot = Files.createTempDirectory("pure-lsp-sync-modify-test");
        Path resourcesDir = workspaceRoot.resolve("sync-module/src/main/resources");
        Path repoDir = resourcesDir.resolve("sync_repo");
        Path sourceFile = repoDir.resolve("model/SyncPerson.pure");
        Files.createDirectories(sourceFile.getParent());

        String originalContent = "Class test::sync::SyncPerson\n{\n  name: String[1];\n}\n";
        Files.write(resourcesDir.resolve("sync_repo.definition.json"), DEFINITION.getBytes());
        Files.write(sourceFile, originalContent.getBytes());

        LegendPureLspServer server = new LegendPureLspServer();
        server.preconfigureAndWarm(Collections.singletonList(workspaceRoot), Collections.emptySet());
        Assert.assertTrue(server.getSession().isInitialized());

        String sourceId = "/sync_repo/model/SyncPerson.pure";
        String newContent = "Class test::sync::SyncPerson\n{\n  name: String[1];\n  age: Integer[1];\n}\n";
        Files.write(sourceFile, newContent.getBytes());

        String uri = sourceFile.toUri().toString();
        SyncWorkspaceResult result = server.syncWorkspace(new SyncWorkspaceParams(Collections.singletonList(uri))).get();

        Assert.assertTrue("Sync should succeed: " + result.getError(), result.isSuccess());
        Assert.assertEquals(1, result.getModified());
        Assert.assertEquals(0, result.getCreated());
        Assert.assertEquals(0, result.getDeleted());
        Assert.assertEquals(newContent, server.getSession().getPureRuntime().getSourceById(sourceId).getContent());
    }

    @Test
    public void syncAppliesNewFileAndReportsItAsCreated() throws Exception
    {
        Path workspaceRoot = Files.createTempDirectory("pure-lsp-sync-create-test");
        Path resourcesDir = workspaceRoot.resolve("sync-module/src/main/resources");
        Path repoDir = resourcesDir.resolve("sync_repo");
        Files.createDirectories(repoDir);
        Files.write(resourcesDir.resolve("sync_repo.definition.json"), DEFINITION.getBytes());

        LegendPureLspServer server = new LegendPureLspServer();
        server.preconfigureAndWarm(Collections.singletonList(workspaceRoot), Collections.emptySet());
        Assert.assertTrue(server.getSession().isInitialized());

        Path newFile = repoDir.resolve("model/BrandNew.pure");
        Files.createDirectories(newFile.getParent());
        String content = "Class test::sync::BrandNew\n{\n  id: Integer[1];\n}\n";
        Files.write(newFile, content.getBytes());

        String uri = newFile.toUri().toString();
        SyncWorkspaceResult result = server.syncWorkspace(new SyncWorkspaceParams(Collections.singletonList(uri))).get();

        Assert.assertTrue("Sync should succeed: " + result.getError(), result.isSuccess());
        Assert.assertEquals(1, result.getCreated());
        Assert.assertNotNull(server.getSession().getPureRuntime().getSourceById("/sync_repo/model/BrandNew.pure"));
    }

    @Test
    public void syncAppliesDeletionAndReportsIt() throws Exception
    {
        Path workspaceRoot = Files.createTempDirectory("pure-lsp-sync-delete-test");
        Path resourcesDir = workspaceRoot.resolve("sync-module/src/main/resources");
        Path repoDir = resourcesDir.resolve("sync_repo");
        Path sourceFile = repoDir.resolve("model/ToDelete.pure");
        Files.createDirectories(sourceFile.getParent());

        Files.write(resourcesDir.resolve("sync_repo.definition.json"), DEFINITION.getBytes());
        Files.write(sourceFile, "Class test::sync::ToDelete\n{\n  name: String[1];\n}\n".getBytes());

        LegendPureLspServer server = new LegendPureLspServer();
        server.preconfigureAndWarm(Collections.singletonList(workspaceRoot), Collections.emptySet());
        String sourceId = "/sync_repo/model/ToDelete.pure";
        Assert.assertNotNull(server.getSession().getPureRuntime().getSourceById(sourceId));

        String uri = sourceFile.toUri().toString();
        Files.delete(sourceFile);

        SyncWorkspaceResult result = server.syncWorkspace(new SyncWorkspaceParams(Collections.singletonList(uri))).get();

        Assert.assertTrue("Sync should succeed: " + result.getError(), result.isSuccess());
        Assert.assertEquals(1, result.getDeleted());
        Assert.assertNull(server.getSession().getPureRuntime().getSourceById(sourceId));
    }

    @Test
    public void syncSkipsFilesCurrentlyOpenInAnEditor() throws Exception
    {
        Path workspaceRoot = Files.createTempDirectory("pure-lsp-sync-open-doc-test");
        Path resourcesDir = workspaceRoot.resolve("sync-module/src/main/resources");
        Path repoDir = resourcesDir.resolve("sync_repo");
        Path sourceFile = repoDir.resolve("model/OpenDoc.pure");
        Files.createDirectories(sourceFile.getParent());

        String originalContent = "Class test::sync::OpenDoc\n{\n  name: String[1];\n}\n";
        Files.write(resourcesDir.resolve("sync_repo.definition.json"), DEFINITION.getBytes());
        Files.write(sourceFile, originalContent.getBytes());

        LegendPureLspServer server = new LegendPureLspServer();
        server.preconfigureAndWarm(Collections.singletonList(workspaceRoot), Collections.emptySet());

        String uri = sourceFile.toUri().toString();
        String openBufferContent = "Class test::sync::OpenDoc\n{\n  name: String[1];\n  fromEditor: Boolean[1];\n}\n";
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "pure", 1, openBufferContent)));

        // A conflicting disk change while the document is open in an editor.
        String diskContent = "Class test::sync::OpenDoc\n{\n  name: String[1];\n  fromDisk: Boolean[1];\n}\n";
        Files.write(sourceFile, diskContent.getBytes());

        SyncWorkspaceResult result = server.syncWorkspace(new SyncWorkspaceParams(Collections.singletonList(uri))).get();

        Assert.assertTrue("Sync should succeed (as a no-op): " + result.getError(), result.isSuccess());
        Assert.assertEquals("Open document must not be touched by sync", 0,
                result.getCreated() + result.getModified() + result.getDeleted());
    }
}
