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
import org.junit.Test;

public class UriMapperTest
{
    @Test
    public void deriveSourceId_stripsResourcesPrefix()
    {
        UriMapper mapper = new UriMapper();
        String uri = "file:///home/user/legend-pure/legend-pure-store/legend-pure-store-relational/src/main/resources/platform_store_relational/tests/model.pure";
        Assert.assertEquals("/platform_store_relational/tests/model.pure", mapper.deriveSourceId(uri));
    }

    @Test
    public void deriveSourceId_handlesNestedResourcesPath()
    {
        UriMapper mapper = new UriMapper();
        String uri = "file:///home/user/legend-pure/legend-pure-core/legend-pure-m3-core/src/main/resources/platform/pure/corefunctions/lang.pure";
        Assert.assertEquals("/platform/pure/corefunctions/lang.pure", mapper.deriveSourceId(uri));
    }

    @Test
    public void deriveSourceId_fallsBackToFilename_whenNoResourcesMarker()
    {
        UriMapper mapper = new UriMapper();
        String uri = "file:///home/user/project/model.pure";
        Assert.assertEquals("model.pure", mapper.deriveSourceId(uri));
    }

    @Test
    public void deriveSourceId_handlesPureScheme()
    {
        UriMapper mapper = new UriMapper();
        Assert.assertEquals("/core/pure/extensions/extension.pure",
                mapper.deriveSourceId("pure:///core/pure/extensions/extension.pure"));
    }

    @Test
    public void deriveSourceId_pureScheme_roundTrips()
    {
        UriMapper mapper = new UriMapper();
        // toUri generates pure:// for platform sources
        String pureUri = mapper.toUri("/platform/pure/essential/lang.pure");
        Assert.assertNotNull(pureUri);
        Assert.assertTrue("Should be pure:// URI", pureUri.startsWith("pure://"));

        // deriveSourceId should extract the source ID back
        String sourceId = mapper.deriveSourceId(pureUri);
        Assert.assertEquals("/platform/pure/essential/lang.pure", sourceId);
    }

    @Test
    public void toSourceId_pureScheme_worksForLspFeatures()
    {
        UriMapper mapper = new UriMapper();
        // When a pure:// document sends an LSP request, toSourceId must handle it
        String sourceId = mapper.toSourceId("pure:///core/pure/extensions/extension.pure");
        Assert.assertEquals("/core/pure/extensions/extension.pure", sourceId);
    }

    @Test
    public void register_overridesDerived()
    {
        UriMapper mapper = new UriMapper();
        String uri = "file:///a/b/c.pure";
        mapper.register(uri, "/custom/c.pure");

        Assert.assertEquals("/custom/c.pure", mapper.toSourceId(uri));
        Assert.assertEquals(uri, mapper.toUri("/custom/c.pure"));
    }

    @Test
    public void toSourceId_cachesDerivedResult()
    {
        UriMapper mapper = new UriMapper();
        String uri = "file:///x/src/main/resources/core/test.pure";

        String first = mapper.toSourceId(uri);
        String second = mapper.toSourceId(uri);

        Assert.assertEquals("/core/test.pure", first);
        Assert.assertSame(first, second);
    }

    @Test
    public void toSourceId_populatesReverseMap()
    {
        UriMapper mapper = new UriMapper();
        String uri = "file:///x/src/main/resources/core/test.pure";
        mapper.toSourceId(uri);

        Assert.assertEquals(uri, mapper.toUri("/core/test.pure"));
    }

    @Test
    public void deriveSourceId_usesRepoScanner_whenInsideKnownRepo() throws Exception
    {
        // Set up a temp dir simulating a repo resources root
        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("lsp_test");
        java.nio.file.Path resourcesDir = tempDir.resolve("mod/src/main/resources");
        java.nio.file.Files.createDirectories(resourcesDir);
        java.nio.file.Files.write(resourcesDir.resolve("my_repo.definition.json"),
                "{\"name\": \"my_repo\"}".getBytes());
        java.nio.file.Path pureFile = resourcesDir.resolve("my_repo/sub/file.pure");
        java.nio.file.Files.createDirectories(pureFile.getParent());
        java.nio.file.Files.write(pureFile, "Class X {}".getBytes());

        RepositoryScanner scanner = new RepositoryScanner();
        scanner.scan(java.util.Collections.singletonList(tempDir));

        UriMapper mapper = new UriMapper();
        mapper.setRepositoryScanner(scanner);

        // File inside known repo but URI doesn't have src/main/resources marker in the standard position
        String uri = pureFile.toUri().toString();
        String sourceId = mapper.deriveSourceId(uri);
        Assert.assertEquals("/my_repo/sub/file.pure", sourceId);

        // Cleanup
        java.nio.file.Files.walk(tempDir).sorted(java.util.Comparator.reverseOrder())
                .forEach(p ->
                {
                    try
                    {
                        java.nio.file.Files.delete(p);
                    }
                    catch (Exception ignored)
                    {
                        // Best-effort cleanup for the temporary test workspace.
                    }
                });
    }

    @Test
    public void deriveSourceId_acceptsResourcesPath_whenScannerConfirmsLeadingSegmentIsARegisteredRepo() throws Exception
    {
        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("lsp_test_known_repo");
        try
        {
            java.nio.file.Path resourcesDir = tempDir.resolve("core-module/src/main/resources");
            java.nio.file.Files.createDirectories(resourcesDir.resolve("core/pure/extensions"));
            java.nio.file.Files.write(resourcesDir.resolve("core.definition.json"), "{\"name\": \"core\"}".getBytes());
            java.nio.file.Path pureFile = resourcesDir.resolve("core/pure/extensions/functions.pure");
            java.nio.file.Files.write(pureFile, "Class X {}".getBytes());

            RepositoryScanner scanner = new RepositoryScanner();
            scanner.scan(java.util.Collections.singletonList(tempDir));

            UriMapper mapper = new UriMapper();
            mapper.setRepositoryScanner(scanner);

            Assert.assertEquals("/core/pure/extensions/functions.pure", mapper.deriveSourceId(pureFile.toUri().toString()));
        }
        finally
        {
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void deriveSourceId_rejectsResourcesPath_whenLeadingSegmentIsNotARegisteredRepo() throws Exception
    {
        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("lsp_test_unknown_repo");
        try
        {
            // "core" is the only registered repo; the fixture module has no definition.json of its own,
            // so its "data" folder must never be treated as a repo name just because the path happens to
            // contain a src/main/resources segment (this is the exact shape of the alloy-lakehouse
            // integration-test-compatibility crash: a ###Lakehouse template .pure fixture, never meant
            // to be legend-pure-compilable, sitting under some other module's src/main/resources/data).
            java.nio.file.Path coreResourcesDir = tempDir.resolve("core-module/src/main/resources");
            java.nio.file.Files.createDirectories(coreResourcesDir.resolve("core"));
            java.nio.file.Files.write(coreResourcesDir.resolve("core.definition.json"), "{\"name\": \"core\"}".getBytes());

            java.nio.file.Path fixtureResourcesDir = tempDir.resolve("some-other-module/src/main/resources");
            java.nio.file.Path fixtureFile = fixtureResourcesDir.resolve("data/ingest/matview/catalog/matview.pure");
            java.nio.file.Files.createDirectories(fixtureFile.getParent());
            java.nio.file.Files.write(fixtureFile, "###Lakehouse\n<TEST_GROUP>".getBytes());

            RepositoryScanner scanner = new RepositoryScanner();
            scanner.scan(java.util.Collections.singletonList(tempDir));

            UriMapper mapper = new UriMapper();
            mapper.setRepositoryScanner(scanner);

            String sourceId = mapper.deriveSourceId(fixtureFile.toUri().toString());

            // Must NOT resolve to "/data/ingest/matview/catalog/matview.pure" (which would imply a bogus
            // "data" repo and get fed into modifyAndCompile against a real module's grammar). Must also
            // NOT fall back to an anonymous scratch file ("matview.pure") - unlike a genuinely external/
            // jar-embedded source, this file really exists on local disk, so treating it as scratch would
            // still attempt (and fail) to compile it, and leaves a later editor-close's restoreFromDisk
            // with nothing registered to restore, crashing the session. Must resolve to null: ignored
            // outright, the same as a .java file would be.
            Assert.assertNull(sourceId);
        }
        finally
        {
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void deriveSourceId_stillScratchFallsBack_forSyntheticPathThatDoesNotExistOnDisk() throws Exception
    {
        // Contrast with the test above: a scanner IS configured, but the uri's path does not correspond
        // to any real file on this disk (e.g. a jar-embedded platform source navigated to via
        // go-to-definition, whose literal "!"-joined jar-entry-style path never exists as a real file).
        // This must keep falling back to scratch/in-memory handling - only a file that genuinely exists
        // on local disk and isn't part of any module gets ignored outright.
        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("lsp_test_synthetic_path");
        try
        {
            java.nio.file.Path resourcesDir = tempDir.resolve("core-module/src/main/resources");
            java.nio.file.Files.createDirectories(resourcesDir.resolve("core"));
            java.nio.file.Files.write(resourcesDir.resolve("core.definition.json"), "{\"name\": \"core\"}".getBytes());

            RepositoryScanner scanner = new RepositoryScanner();
            scanner.scan(java.util.Collections.singletonList(tempDir));

            UriMapper mapper = new UriMapper();
            mapper.setRepositoryScanner(scanner);

            String sourceId = mapper.deriveSourceId("file:///nonexistent/synthetic/fail.pure");
            Assert.assertEquals("fail.pure", sourceId);
        }
        finally
        {
            deleteRecursively(tempDir);
        }
    }

    private static void deleteRecursively(java.nio.file.Path root) throws Exception
    {
        java.nio.file.Files.walk(root).sorted(java.util.Comparator.reverseOrder())
                .forEach(p ->
                {
                    try
                    {
                        java.nio.file.Files.delete(p);
                    }
                    catch (Exception ignored)
                    {
                        // Best-effort cleanup for the temporary test workspace.
                    }
                });
    }

    @Test
    public void toUri_returnsPureScheme_forUnknownStorageSourceId()
    {
        UriMapper mapper = new UriMapper();
        // Storage sources (start with /) should fall back to pure:// URIs
        String uri = mapper.toUri("/core/pure/extensions/extension.pure");
        Assert.assertNotNull("Should return pure:// URI for unknown storage source", uri);
        Assert.assertEquals("pure:///core/pure/extensions/extension.pure", uri);
    }

    @Test
    public void toUri_returnsNull_forUnknownInMemorySourceId()
    {
        UriMapper mapper = new UriMapper();
        // In-memory sources (no leading /) have no fallback
        Assert.assertNull(mapper.toUri("some_test.pure"));
    }

    @Test
    public void toUri_slashAgnostic_findsAlternateForm()
    {
        UriMapper mapper = new UriMapper();
        mapper.register("file:///test/foo.pure", "/foo.pure");

        // Looking up without leading slash should find the /foo.pure entry
        Assert.assertEquals("file:///test/foo.pure", mapper.toUri("foo.pure"));
    }

    @Test
    public void clear_removesCachedMappings()
    {
        UriMapper mapper = new UriMapper();
        mapper.register("file:///a.pure", "/a.pure");
        Assert.assertEquals("file:///a.pure", mapper.toUri("/a.pure"));

        mapper.clear();

        // After clear, the registered file:// mapping is gone.
        // For storage sources (leading /), the pure:// fallback kicks in.
        String uri = mapper.toUri("/a.pure");
        Assert.assertEquals("pure:///a.pure", uri);
    }
}
