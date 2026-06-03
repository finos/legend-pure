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
}
