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

package org.finos.legend.pure.m3.serialization.runtime.binary;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class TestPureRepositoryJarMetadata
{
    // Source ids are ASCII, but element paths need not be. Greek and Chinese letters are in neither windows-1252 nor
    // ISO-8859-1, default charsets of JVMs before Java 18.
    private static final String SOURCE_ID = "/test_repo/greek.pure";
    private static final String BINARY_PATH = PureRepositoryJarTools.purePathToBinaryPath(SOURCE_ID);
    private static final String ELEMENT = "test::\u03B1\u03B2\u03B3::Greek";
    private static final String REFERENCE = "test::\u4E2D\u6587::Chinese";

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testIndexesInJarAreUtf8() throws IOException
    {
        byte[] jar = buildJar();
        try (JarInputStream stream = new JarInputStream(new ByteArrayInputStream(jar)))
        {
            JarEntry definitionIndex = stream.getNextJarEntry();
            Assert.assertEquals(PureRepositoryJarTools.DEFINITION_INDEX_NAME, definitionIndex.getName());
            assertUtf8Contains(readAll(stream), ELEMENT);

            JarEntry referenceIndex = stream.getNextJarEntry();
            Assert.assertEquals(PureRepositoryJarTools.REFERENCE_INDEX_NAME, referenceIndex.getName());
            assertUtf8Contains(readAll(stream), REFERENCE);
        }
        assertMetadata(PureRepositoryJarMetadata.getPureMetadata(jar));
    }

    @Test
    public void testIndexesInUnpackedJarAreReadAsUtf8() throws IOException
    {
        Path root = this.tmp.newFolder().toPath();
        Path metaInf = Files.createDirectories(root.resolve(PureRepositoryJarTools.META_INF_DIR_NAME));
        try (OutputStream stream = Files.newOutputStream(metaInf.resolve("MANIFEST.MF")))
        {
            PureManifest.create(null, null, "test_repo").write(stream);
        }
        Files.write(metaInf.resolve(PureRepositoryJarTools.DEFINITION_INDEX_FILENAME),
                ("{\n\t\"" + ELEMENT + "\" : \"" + BINARY_PATH + "\"\n}").getBytes(StandardCharsets.UTF_8));
        Files.write(metaInf.resolve(PureRepositoryJarTools.REFERENCE_INDEX_FILENAME),
                ("{\n\t\"" + BINARY_PATH + "\" : [\"" + REFERENCE + "\"]\n}").getBytes(StandardCharsets.UTF_8));

        assertMetadata(PureRepositoryJarMetadata.getPureMetadataFromUnpackedJar(root));
    }

    private static byte[] buildJar() throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PureRepositoryJarBuilder builder = PureRepositoryJarBuilder.newBuilder(bytes, null, null, "test_repo",
                Lists.immutable.with(new SourceSerializationResult(SOURCE_ID, Lists.immutable.with(ELEMENT), Lists.immutable.with(REFERENCE))));
        builder.close();
        return bytes.toByteArray();
    }

    private static void assertMetadata(PureRepositoryJarMetadata metadata)
    {
        Assert.assertEquals(Maps.immutable.with(ELEMENT, BINARY_PATH), metadata.getDefinitionIndex());
        Assert.assertEquals(Maps.immutable.with(BINARY_PATH, Sets.immutable.with(REFERENCE)), metadata.getExternalReferenceIndex());
    }

    private static void assertUtf8Contains(byte[] bytes, String expected)
    {
        String text = new String(bytes, StandardCharsets.UTF_8);
        Assert.assertTrue("Expected to find \"" + expected + "\" in: " + text, text.contains(expected));
    }

    private static byte[] readAll(InputStream stream) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) != -1)
        {
            bytes.write(buffer, 0, read);
        }
        return bytes.toByteArray();
    }
}
