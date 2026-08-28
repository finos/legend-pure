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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class OverlayWorkspaceCodeStorageTest
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void writeDeleteAndRestore_doNotMutateDisk() throws Exception
    {
        Path root = this.tempFolder.getRoot().toPath().resolve("overlay_repo");
        Path file = root.resolve("model/Person.pure");
        Files.createDirectories(file.getParent());
        String diskContent = "Class test::overlay::Person\n{\n  name: String[1];\n}\n";
        Files.write(file, diskContent.getBytes(StandardCharsets.UTF_8));

        CodeRepository repository = new GenericCodeRepository("overlay_repo", "(test::overlay)(::.*)?");
        OverlayWorkspaceCodeStorage storage = new OverlayWorkspaceCodeStorage(repository, root);
        String sourceId = "/overlay_repo/model/Person.pure";

        Assert.assertEquals(diskContent, storage.getContentAsText(sourceId));

        OverlayWorkspaceCodeStorage.OverlaySnapshot snapshot = storage.snapshot(sourceId);
        String overlayContent = "Class test::overlay::Person\n{\n  fullName: String[1];\n}\n";
        storage.writeContent(sourceId, overlayContent);

        Assert.assertEquals("Overlay content should be visible to the runtime", overlayContent, storage.getContentAsText(sourceId));
        Assert.assertEquals("Disk content must not be overwritten", diskContent, new String(Files.readAllBytes(file), StandardCharsets.UTF_8));

        storage.restore(snapshot);
        Assert.assertEquals("Restoring the overlay snapshot should reveal disk content again", diskContent, storage.getContentAsText(sourceId));
        Assert.assertEquals("Disk content must still be unchanged", diskContent, new String(Files.readAllBytes(file), StandardCharsets.UTF_8));

        storage.deleteFile(sourceId);
        Assert.assertFalse("Overlay delete should hide the file from PureRuntime", storage.exists(sourceId));
        Assert.assertTrue("Overlay delete must not delete the physical file", Files.exists(file));

        String createdSourceId = "/overlay_repo/model/NewPerson.pure";
        Path createdFile = root.resolve("model/NewPerson.pure");
        storage.writeContent(createdSourceId, "Class test::overlay::NewPerson {}\n");

        Assert.assertTrue(storage.exists(createdSourceId));
        Assert.assertFalse("Overlay create must not create a physical file", Files.exists(createdFile));
    }

    @Test
    public void getUserFiles_excludesTopLevelDataFixtureDir_butKeepsWelcomePure() throws Exception
    {
        // Reproduces the alloy-lakehouse-ingest-integration-test-compatibility layout: a top-level
        // "data" folder full of runtime test-fixture .pure text (template strings a Java test harness
        // reads and substitutes, never meant to be parsed by the LSP), plus a real package file and a
        // welcome.pure entry point that must always be kept even though it sits under "data".
        Path root = this.tempFolder.getRoot().toPath().resolve("fixture_repo");
        Path realModelFile = root.resolve("model/Person.pure");
        Path fixtureFile = root.resolve("data/ingest/matview/catalog/matview.pure");
        Path welcomeUnderData = root.resolve("data/welcome.pure");
        Files.createDirectories(realModelFile.getParent());
        Files.createDirectories(fixtureFile.getParent());
        Files.write(realModelFile, "Class test::fixture::Person\n{\n  name: String[1];\n}\n".getBytes(StandardCharsets.UTF_8));
        Files.write(fixtureFile, "###Lakehouse\nnot valid outside a template substitution\n".getBytes(StandardCharsets.UTF_8));
        Files.write(welcomeUnderData, "function go():Any[*]\n{\n  'hi'\n}\n".getBytes(StandardCharsets.UTF_8));

        CodeRepository repository = new GenericCodeRepository("fixture_repo", "(test::fixture)(::.*)?");
        OverlayWorkspaceCodeStorage storage = new OverlayWorkspaceCodeStorage(repository, root);

        Set<String> files = toSet(storage.getUserFiles());

        Assert.assertTrue("Real package file should be compilable, got: " + files,
                files.contains("/fixture_repo/model/Person.pure"));
        Assert.assertFalse("File under a top-level data/ fixture folder should be excluded, got: " + files,
                files.contains("/fixture_repo/data/ingest/matview/catalog/matview.pure"));
        Assert.assertTrue("welcome.pure must always be kept, even under data/, got: " + files,
                files.contains("/fixture_repo/data/welcome.pure"));
    }

    @Test
    public void getUserFiles_keepsDataAsNestedPackageSegment() throws Exception
    {
        // Guards against over-broadening the exclusion: a "data" package nested below the repo root
        // (e.g. legend-engine's real core::pure::data package, at core/pure/data/*.pure) is legitimate
        // compilable source and must not be swept up by the top-level-only fixture exclusion.
        Path root = this.tempFolder.getRoot().toPath().resolve("core_repo");
        Path nestedDataPackageFile = root.resolve("pure/data/data.pure");
        Files.createDirectories(nestedDataPackageFile.getParent());
        Files.write(nestedDataPackageFile, "function test::core::data::f(): Any[*] { [] }\n".getBytes(StandardCharsets.UTF_8));

        CodeRepository repository = new GenericCodeRepository("core_repo", "(test::core)(::.*)?");
        OverlayWorkspaceCodeStorage storage = new OverlayWorkspaceCodeStorage(repository, root);

        Set<String> files = toSet(storage.getUserFiles());

        Assert.assertTrue("Nested data/ package (not at the repo's top level) must still compile, got: " + files,
                files.contains("/core_repo/pure/data/data.pure"));
    }

    private static Set<String> toSet(RichIterable<String> files)
    {
        Set<String> result = new HashSet<>();
        files.forEach(result::add);
        return result;
    }
}
