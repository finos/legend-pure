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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUrisTest
{
    private static final Pattern DRIVE_URI_PREFIX = Pattern.compile("^file:///([A-Za-z]):/");

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void toPath_roundTripsPathToUri() throws IOException
    {
        Path path = this.tempFolder.newFile("x.pure").toPath();
        Assert.assertEquals(path, FileUris.toPath(path.toUri().toString()));
    }

    @Test
    public void toPath_acceptsEditorSpelling() throws IOException
    {
        Path path = this.tempFolder.newFile("x.pure").toPath();
        Assert.assertEquals(path, FileUris.toPath(editorUri(path)));
    }

    @Test
    public void toPath_returnsNullForOtherSchemes()
    {
        Assert.assertNull(FileUris.toPath("pure:///core/x.pure"));
        Assert.assertNull(FileUris.toPath("untitled:Untitled-1"));
        Assert.assertNull(FileUris.toPath("/core/x.pure"));
        Assert.assertNull(FileUris.toPath(null));
    }

    @Test
    public void key_isSharedByEverySpellingOfTheSameFile() throws IOException
    {
        Path path = this.tempFolder.newFile("x.pure").toPath();
        String expected = FileUris.key(path.toUri().toString());
        Assert.assertEquals(expected, FileUris.key(path.toFile().toURI().toString()));
        Assert.assertEquals(expected, FileUris.key(editorUri(path)));
        Assert.assertEquals(expected, FileUris.key(path.getParent().resolve("sub").resolve("..").resolve("x.pure").toUri().toString()));
    }

    @Test
    public void key_distinguishesDifferentFiles() throws IOException
    {
        Path x = this.tempFolder.newFile("x.pure").toPath();
        Path y = this.tempFolder.newFile("y.pure").toPath();
        Assert.assertNotEquals(FileUris.key(x.toUri().toString()), FileUris.key(y.toUri().toString()));
    }

    @Test
    public void key_leavesOtherStringsAsTheyAre()
    {
        Assert.assertEquals("pure:///core/x.pure", FileUris.key("pure:///core/x.pure"));
        Assert.assertEquals("/core/x.pure", FileUris.key("/core/x.pure"));
    }

    @Test
    public void uriMapper_derivesTheSameSourceIdFromEverySpelling() throws IOException
    {
        Path resources = Files.createDirectories(this.tempFolder.getRoot().toPath().resolve("module/src/main/resources"));
        Files.write(resources.resolve("my_repo.definition.json"), "{\"name\": \"my_repo\"}".getBytes());
        Path file = Files.createDirectories(resources.resolve("my_repo/sub")).resolve("Model.pure");
        Files.write(file, "Class my::Model {}".getBytes());

        RepositoryScanner scanner = new RepositoryScanner();
        scanner.scan(Collections.singletonList(this.tempFolder.getRoot().toPath()));
        UriMapper mapper = new UriMapper();
        mapper.setRepositoryScanner(scanner);

        Assert.assertEquals("/my_repo/sub/Model.pure", mapper.toSourceId(editorUri(file)));
        Assert.assertEquals("/my_repo/sub/Model.pure", mapper.toSourceId(file.toUri().toString()));
        Assert.assertEquals("/my_repo/sub/Model.pure", mapper.toSourceId(file.toFile().toURI().toString()));
        Assert.assertEquals("/my_repo/sub/Model.pure", mapper.toSourceId(file.toString()));
    }

    /**
     * The URI as VS Code spells it: on Windows it lower-cases the drive letter and percent-encodes the
     * colon after it (file:///c%3A/...), where Path.toUri() gives file:///C:/...
     */
    private static String editorUri(Path path)
    {
        String uri = path.toUri().toString();
        Matcher matcher = DRIVE_URI_PREFIX.matcher(uri);
        return matcher.find()
                ? ("file:///" + matcher.group(1).toLowerCase() + "%3A/" + uri.substring(matcher.end()))
                : uri;
    }
}
