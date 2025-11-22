// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.compiler.file;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.compiler.reference.AbstractReferenceTest;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.tools.GraphTools;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public abstract class AbstractFilePathProviderExtensionTest extends AbstractReferenceTest
{
    protected FilePathProviderExtension extension;
    protected FilePathProvider serializer;

    @Before
    public void setUpExtension()
    {
        this.extension = getExtension();
        this.serializer = FilePathProvider.builder().withExtension(this.extension).build();
    }

    @Test
    public void testVersions()
    {
        int expectedVersion = this.extension.version();

        Assert.assertEquals(expectedVersion, this.serializer.getDefaultVersion());
        Assert.assertTrue(this.serializer.isVersionAvailable(expectedVersion));

        MutableIntList versions = IntLists.mutable.empty();
        this.serializer.forEachVersion(versions::add);
        Assert.assertEquals(IntLists.mutable.with(expectedVersion), versions);
    }

    @Test
    public void testAllElementPaths()
    {
        MapIterable<String, Pattern> patternBySeparator = buildPatternMap(getExpectedElementPrefixDirs(), getExpectedElementFilenameExtension());
        GraphTools.getTopLevelAndPackagedElements(processorSupport).forEach(element ->
        {
            String path = PackageableElement.getUserPathForPackageableElement(element);
            forEachFSSeparator(fsSeparator ->
            {
                String filePath = this.extension.getElementFilePath(path, fsSeparator);
                Assert.assertNotNull(path, filePath);
                Pattern pattern = patternBySeparator.get(fsSeparator);
                if (!pattern.matcher(filePath).matches())
                {
                    Assert.fail("File path does not match the expected pattern\n\telement path: " + path + "\n\tfile path: " + filePath + "\n\tpattern: " + pattern.pattern());
                }
                int index = findInvalidName(filePath, fsSeparator);
                if (index > -1)
                {
                    Assert.fail("File path exceeds the file name size limit at index " + index + "\n\telement name:" + path + "\n\tfile path: " + filePath);
                }
            });
        });
    }

    @Test
    public void testAllModuleManifestPaths()
    {
        MapIterable<String, Pattern> patternBySeparator = buildPatternMap(getExpectedModuleMetadataPrefixDirs(), getExpectedModuleManifestFilenameExtension());
        runtime.getCodeStorage().getAllRepositories().forEach(module ->
        {
            String moduleName = module.getName();
            forEachFSSeparator(fsSeparator ->
            {
                String filePath = this.extension.getModuleManifestFilePath(moduleName, fsSeparator);
                Assert.assertNotNull(moduleName, filePath);
                Pattern pattern = patternBySeparator.get(fsSeparator);
                if (!pattern.matcher(filePath).matches())
                {
                    Assert.fail("File path does not match the expected pattern\n\tmodule name: " + moduleName + "\n\tfile path: " + filePath + "\n\tpattern: " + pattern.pattern());
                }
                int index = findInvalidName(filePath, fsSeparator);
                if (index > -1)
                {
                    Assert.fail("File path exceeds the file name size limit at index " + index + "\n\tmodule name:" + moduleName + "\n\tfile path: " + filePath);
                }
            });
        });
    }

    @Test
    public void testAllModuleSourceMetadataPaths()
    {
        MapIterable<String, Pattern> patternBySeparator = buildPatternMap(getExpectedModuleMetadataPrefixDirs(), getExpectedModuleSourceMetadataFilenameExtension());
        runtime.getCodeStorage().getAllRepositories().forEach(module ->
        {
            String moduleName = module.getName();
            forEachFSSeparator(fsSeparator ->
            {
                String filePath = this.extension.getModuleSourceMetadataFilePath(moduleName, fsSeparator);
                Assert.assertNotNull(moduleName, filePath);
                Pattern pattern = patternBySeparator.get(fsSeparator);
                if (!pattern.matcher(filePath).matches())
                {
                    Assert.fail("File path does not match the expected pattern\n\tmodule name: " + moduleName + "\n\tfile path: " + filePath + "\n\tpattern: " + pattern.pattern());
                }
                int index = findInvalidName(filePath, fsSeparator);
                if (index > -1)
                {
                    Assert.fail("File path exceeds the file name size limit at index " + index + "\n\tmodule name:" + moduleName + "\n\tfile path: " + filePath);
                }
            });
        });
    }

    @Test
    public void testAllModuleExternalReferencePaths()
    {
        MapIterable<String, Pattern> patternBySeparator = buildPatternMap(getExpectedModuleMetadataPrefixDirs(), getExpectedModuleExternalReferenceMetadataFilenameExtension());
        runtime.getCodeStorage().getAllRepositories().forEach(module ->
        {
            String moduleName = module.getName();
            forEachFSSeparator(fsSeparator ->
            {
                String filePath = this.extension.getModuleExternalReferenceMetadataFilePath(moduleName, fsSeparator);
                Assert.assertNotNull(moduleName, filePath);
                Pattern pattern = patternBySeparator.get(fsSeparator);
                if (!pattern.matcher(filePath).matches())
                {
                    Assert.fail("File path does not match the expected pattern\n\tmodule name: " + moduleName + "\n\tfile path: " + filePath + "\n\tpattern: " + pattern.pattern());
                }
                int index = findInvalidName(filePath, fsSeparator);
                if (index > -1)
                {
                    Assert.fail("File path exceeds the file name size limit at index " + index + "\n\tmodule name:" + moduleName + "\n\tfile path: " + filePath);
                }
            });
        });
    }

    @Test
    public void testAllModuleElementBackReferencePaths()
    {
        MutableList<String> moduleNames = runtime.getCodeStorage().getAllRepositories().collect(CodeRepository::getName, Lists.mutable.empty());
        MapIterable<String, Pattern> patternBySeparator = buildPatternMap(getExpectedModuleMetadataPrefixDirs(), getExpectedModuleElementBackReferenceMetadataFilenameExtension());
        GraphTools.getTopLevelAndPackagedElements(processorSupport).forEach(element ->
        {
            String path = PackageableElement.getUserPathForPackageableElement(element);
            moduleNames.forEach(moduleName -> forEachFSSeparator(fsSeparator ->
            {
                String filePath = this.extension.getModuleElementBackReferenceMetadataFilePath(moduleName, path, fsSeparator);
                Assert.assertNotNull(path, filePath);
                Pattern pattern = patternBySeparator.get(fsSeparator);
                if (!pattern.matcher(filePath).matches())
                {
                    Assert.fail("File path does not match the expected pattern\n\telement path: " + path + "\n\tfile path: " + filePath + "\n\tpattern: " + pattern.pattern());
                }
                int index = findInvalidName(filePath, fsSeparator);
                if (index > -1)
                {
                    Assert.fail("File path exceeds the file name size limit at index " + index + "\n\telement name:" + path + "\n\tfile path: " + filePath);
                }
            }));
        });
    }

    @Test
    public void testUnicodeInElementPaths()
    {
        String[] unicodeElementPaths = {
                "test::model::My\uD808\uDC3BClass",
                "test::model::\uD808\uDC00\uD808\uDC17::MyClass",
                "test::model::\uD808\uDC00\uD808\uDC17::My\uD808\uDC3BClass",
                "\uD801\uDE00\uD801\uDE01\uD801\uDE02::\uD801\uDE03\uD801\uDE04\uD801\uDE05\uD801\uDE06::\uD801\uDE07\uD801\uDE08\uD801\uDE09\uD801\uDE0A::" +
                        "\uD801\uDE0B\uD801\uDE0C\uD801\uDE0D\uD801\uDE0E\uD801\uDE0F\uD801\uDE10\uD801\uDE11\uD801\uDE12\uD801\uDE13\uD801\uDE14\uD801\uDE15::" +
                        "\uD801\uDE16\uD801\uDE17\uD801\uDE18\uD801\uDE19\uD801\uDE1A\uD801\uDE1B\uD801\uDE1C\uD801\uDE1D\uD801\uDE1E\uD801\uDE1F\uD801\uDE20::" +
                        "\uD801\uDE21\uD801\uDE22\uD801\uDE23\uD801\uDE24\uD801\uDE25\uD801\uDE26\uD801\uDE27\uD801\uDE28\uD801\uDE29\uD801\uDE2A\uD801\uDE2B::" +
                        "\uD801\uDE2C\uD801\uDE2D\uD801\uDE2E\uD801\uDE2F"
        };
        MapIterable<String, Pattern> patternBySeparator = buildPatternMap(getExpectedElementPrefixDirs(), getExpectedElementFilenameExtension());
        forEachFSSeparator(fsSeparator ->
        {
            Pattern pattern = patternBySeparator.get(fsSeparator);
            for (String path : unicodeElementPaths)
            {
                String filePath = this.extension.getElementFilePath(path, fsSeparator);
                Assert.assertNotNull(path, filePath);
                if (!pattern.matcher(filePath).matches())
                {
                    Assert.fail("File path does not match the expected pattern\n\telement path: " + path + "\n\tfile path: " + filePath + "\n\tpattern: " + pattern.pattern());
                }
                int index = findInvalidName(filePath, fsSeparator);
                if (index > -1)
                {
                    Assert.fail("File path exceeds the file name size limit at index " + index + ": " + filePath);
                }
            }
        });
    }

    @Test
    public void testUnicodeInModuleNames_Manifest()
    {
        String[] unicodeModuleNames = {
                "mod_\uD802\uDF00\uD802\uDF01\uD802\uDF02\uD802\uDF03_\uD802\uDF04\uD802\uDF05\uD802\uDF06\uD802\uDF07_\uD802\uDF08\uD802\uDF09\uD802\uDF0A_mod",
                "\uD802\uDF0B\uD802\uDF0C\uD802\uDF0D_\uD802\uDF0E\uD802\uDF0F\uD802\uDF10\uD802\uDF11\uD802\uDF12\uD802\uDF13_\uD802\uDF14\uD802\uDF15",
                "\uD802\uDF16\uD802\uDF17\uD802\uDF18\uD802\uDF19\uD802\uDF1A\uD802\uDF1B\uD802\uDF1C\uD802\uDF1D\uD802\uDF1E\uD802\uDF1F\uD802\uDF20",
                "\uD802\uDF21\uD802\uDF22\uD802\uDF23\uD802\uDF24\uD802\uDF25\uD802\uDF26\uD802\uDF27\uD802\uDF28\uD802\uDF29\uD802\uDF2A\uD802\uDF2B",
                "a\uD802\uDF2C\uD802\uDF2D\uD802\uDF2E\uD802\uDF2F\uD802\uDF30\uD802\uDF31\uD802\uDF32\uD802\uDF33\uD802\uDF34\uD802\uDF35z"
        };
        MapIterable<String, Pattern> patternBySeparator = buildPatternMap(getExpectedModuleMetadataPrefixDirs(), getExpectedModuleManifestFilenameExtension());
        forEachFSSeparator(fsSeparator ->
        {
            Pattern pattern = patternBySeparator.get(fsSeparator);
            for (String moduleName : unicodeModuleNames)
            {
                String filePath = this.extension.getModuleManifestFilePath(moduleName, fsSeparator);
                Assert.assertNotNull(moduleName, filePath);
                if (!pattern.matcher(filePath).matches())
                {
                    Assert.fail("File path does not match the expected pattern\n\tmodule name: " + moduleName + "\n\tfile path: " + filePath + "\n\tpattern: " + pattern.pattern());
                }
                int index = findInvalidName(filePath, fsSeparator);
                if (index > -1)
                {
                    Assert.fail("File path exceeds the file name size limit at index " + index + ": " + filePath);
                }
            }
        });
    }

    @Test
    public void testUnicodeInModuleNames_SourceMetadata()
    {
        String[] unicodeModuleNames = {
                "mod_\uD802\uDF00\uD802\uDF01\uD802\uDF02\uD802\uDF03_\uD802\uDF04\uD802\uDF05\uD802\uDF06\uD802\uDF07_\uD802\uDF08\uD802\uDF09\uD802\uDF0A_mod",
                "\uD802\uDF0B\uD802\uDF0C\uD802\uDF0D_\uD802\uDF0E\uD802\uDF0F\uD802\uDF10\uD802\uDF11\uD802\uDF12\uD802\uDF13_\uD802\uDF14\uD802\uDF15",
                "\uD802\uDF16\uD802\uDF17\uD802\uDF18\uD802\uDF19\uD802\uDF1A\uD802\uDF1B\uD802\uDF1C\uD802\uDF1D\uD802\uDF1E\uD802\uDF1F\uD802\uDF20",
                "\uD802\uDF21\uD802\uDF22\uD802\uDF23\uD802\uDF24\uD802\uDF25\uD802\uDF26\uD802\uDF27\uD802\uDF28\uD802\uDF29\uD802\uDF2A\uD802\uDF2B",
                "a\uD802\uDF2C\uD802\uDF2D\uD802\uDF2E\uD802\uDF2F\uD802\uDF30\uD802\uDF31\uD802\uDF32\uD802\uDF33\uD802\uDF34\uD802\uDF35z"
        };
        MapIterable<String, Pattern> patternBySeparator = buildPatternMap(getExpectedModuleMetadataPrefixDirs(), getExpectedModuleSourceMetadataFilenameExtension());
        forEachFSSeparator(fsSeparator ->
        {
            Pattern pattern = patternBySeparator.get(fsSeparator);
            for (String moduleName : unicodeModuleNames)
            {
                String filePath = this.extension.getModuleSourceMetadataFilePath(moduleName, fsSeparator);
                Assert.assertNotNull(moduleName, filePath);
                if (!pattern.matcher(filePath).matches())
                {
                    Assert.fail("File path does not match the expected pattern\n\tmodule name: " + moduleName + "\n\tfile path: " + filePath + "\n\tpattern: " + pattern.pattern());
                }
                int index = findInvalidName(filePath, fsSeparator);
                if (index > -1)
                {
                    Assert.fail("File path exceeds the file name size limit at index " + index + ": " + filePath);
                }
            }
        });
    }

    @Test
    public void testUnicodeInModuleNames_ExtRefMetadata()
    {
        String[] unicodeModuleNames = {
                "mod_\uD802\uDF00\uD802\uDF01\uD802\uDF02\uD802\uDF03_\uD802\uDF04\uD802\uDF05\uD802\uDF06\uD802\uDF07_\uD802\uDF08\uD802\uDF09\uD802\uDF0A_mod",
                "\uD802\uDF0B\uD802\uDF0C\uD802\uDF0D_\uD802\uDF0E\uD802\uDF0F\uD802\uDF10\uD802\uDF11\uD802\uDF12\uD802\uDF13_\uD802\uDF14\uD802\uDF15",
                "\uD802\uDF16\uD802\uDF17\uD802\uDF18\uD802\uDF19\uD802\uDF1A\uD802\uDF1B\uD802\uDF1C\uD802\uDF1D\uD802\uDF1E\uD802\uDF1F\uD802\uDF20",
                "\uD802\uDF21\uD802\uDF22\uD802\uDF23\uD802\uDF24\uD802\uDF25\uD802\uDF26\uD802\uDF27\uD802\uDF28\uD802\uDF29\uD802\uDF2A\uD802\uDF2B",
                "a\uD802\uDF2C\uD802\uDF2D\uD802\uDF2E\uD802\uDF2F\uD802\uDF30\uD802\uDF31\uD802\uDF32\uD802\uDF33\uD802\uDF34\uD802\uDF35z"
        };
        MapIterable<String, Pattern> patternBySeparator = buildPatternMap(getExpectedModuleMetadataPrefixDirs(), getExpectedModuleExternalReferenceMetadataFilenameExtension());
        forEachFSSeparator(fsSeparator ->
        {
            Pattern pattern = patternBySeparator.get(fsSeparator);
            for (String moduleName : unicodeModuleNames)
            {
                String filePath = this.extension.getModuleExternalReferenceMetadataFilePath(moduleName, fsSeparator);
                Assert.assertNotNull(moduleName, filePath);
                if (!pattern.matcher(filePath).matches())
                {
                    Assert.fail("File path does not match the expected pattern\n\tmodule name: " + moduleName + "\n\tfile path: " + filePath + "\n\tpattern: " + pattern.pattern());
                }
                int index = findInvalidName(filePath, fsSeparator);
                if (index > -1)
                {
                    Assert.fail("File path exceeds the file name size limit at index " + index + ": " + filePath);
                }
            }
        });
    }


    @Test
    public void testUnicodeInElementAndModuleNames()
    {
        String[] unicodeModuleNames = {
                "mod_\uD802\uDF00\uD802\uDF01\uD802\uDF02\uD802\uDF03_\uD802\uDF04\uD802\uDF05\uD802\uDF06\uD802\uDF07_\uD802\uDF08\uD802\uDF09\uD802\uDF0A_mod",
                "\uD802\uDF0B\uD802\uDF0C\uD802\uDF0D_\uD802\uDF0E\uD802\uDF0F\uD802\uDF10\uD802\uDF11\uD802\uDF12\uD802\uDF13_\uD802\uDF14\uD802\uDF15",
                "\uD802\uDF16\uD802\uDF17\uD802\uDF18\uD802\uDF19\uD802\uDF1A\uD802\uDF1B\uD802\uDF1C\uD802\uDF1D\uD802\uDF1E\uD802\uDF1F\uD802\uDF20",
                "\uD802\uDF21\uD802\uDF22\uD802\uDF23\uD802\uDF24\uD802\uDF25\uD802\uDF26\uD802\uDF27\uD802\uDF28\uD802\uDF29\uD802\uDF2A\uD802\uDF2B",
                "a\uD802\uDF2C\uD802\uDF2D\uD802\uDF2E\uD802\uDF2F\uD802\uDF30\uD802\uDF31\uD802\uDF32\uD802\uDF33\uD802\uDF34\uD802\uDF35z"
        };
        String[] unicodeElementPaths = {
                "test::model::My\uD808\uDC3BClass",
                "test::model::\uD808\uDC00\uD808\uDC17::MyClass",
                "test::model::\uD808\uDC00\uD808\uDC17::My\uD808\uDC3BClass",
                "\uD801\uDE00\uD801\uDE01\uD801\uDE02::\uD801\uDE03\uD801\uDE04\uD801\uDE05\uD801\uDE06::\uD801\uDE07\uD801\uDE08\uD801\uDE09\uD801\uDE0A::" +
                        "\uD801\uDE0B\uD801\uDE0C\uD801\uDE0D\uD801\uDE0E\uD801\uDE0F\uD801\uDE10\uD801\uDE11\uD801\uDE12\uD801\uDE13\uD801\uDE14\uD801\uDE15::" +
                        "\uD801\uDE16\uD801\uDE17\uD801\uDE18\uD801\uDE19\uD801\uDE1A\uD801\uDE1B\uD801\uDE1C\uD801\uDE1D\uD801\uDE1E\uD801\uDE1F\uD801\uDE20::" +
                        "\uD801\uDE21\uD801\uDE22\uD801\uDE23\uD801\uDE24\uD801\uDE25\uD801\uDE26\uD801\uDE27\uD801\uDE28\uD801\uDE29\uD801\uDE2A\uD801\uDE2B::" +
                        "\uD801\uDE2C\uD801\uDE2D\uD801\uDE2E\uD801\uDE2F"
        };

        MapIterable<String, Pattern> patternBySeparator = buildPatternMap(getExpectedModuleMetadataPrefixDirs(), getExpectedModuleElementBackReferenceMetadataFilenameExtension());
        forEachFSSeparator(fsSeparator ->
        {
            Pattern pattern = patternBySeparator.get(fsSeparator);
            for (String elementPath : unicodeElementPaths)
            {
                for (String moduleName : unicodeModuleNames)
                {
                    String filePath = this.extension.getModuleElementBackReferenceMetadataFilePath(moduleName, elementPath, fsSeparator);
                    Assert.assertNotNull(moduleName, filePath);
                    if (!pattern.matcher(filePath).matches())
                    {
                        Assert.fail("File path does not match the expected pattern\n\tmodule name: " + moduleName + "\n\telement path: " + elementPath + "\n\tfile path: " + filePath + "\n\tpattern: " + pattern.pattern());
                    }
                    int index = findInvalidName(filePath, fsSeparator);
                    if (index > -1)
                    {
                        Assert.fail("File path exceeds the file name size limit at index " + index + ": " + filePath);
                    }
                }
            }
        });
    }

    @Test
    public void testVeryLongElementPaths()
    {
        String[] longElementPaths = {
                "test::model::ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName",
                "test::model::ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName",
                "test::model::ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName",
                "test::model::ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName",
                "test::model::ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery" +
                        "VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery" +
                        "VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName",
                "test::model::packageWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName::MyClass",
                "test::model::packageWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery" +
                        "VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery" +
                        "VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName::MyClass",
                "test::model::ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery\uD808\uDC00\uD808\uDC17ryLongName",
                "test::model::ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery\uD808\uDC00\uD808\uDC17ryVeryVeryLongName",
                "test::model::ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryV\uD808\uDC00\uD808\uDC17yVeryVeryLongName",
                "test::model::ClassWithAVeryVeryVery" +
                        "\uD801\uDE00\uD801\uDE01\uD801\uDE02\uD801\uDE03\uD801\uDE04\uD801\uDE05\uD801\uDE06\uD801\uDE07\uD801\uDE08\uD801\uDE09\uD801\uDE0A" +
                        "\uD801\uDE0B\uD801\uDE0C\uD801\uDE0D\uD801\uDE0E\uD801\uDE0F\uD801\uDE10\uD801\uDE11\uD801\uDE12\uD801\uDE13\uD801\uDE14\uD801\uDE15" +
                        "\uD801\uDE16\uD801\uDE17\uD801\uDE18\uD801\uDE19\uD801\uDE1A\uD801\uDE1B\uD801\uDE1C\uD801\uDE1D\uD801\uDE1E\uD801\uDE1F\uD801\uDE20" +
                        "\uD801\uDE21\uD801\uDE22\uD801\uDE23\uD801\uDE24\uD801\uDE25\uD801\uDE26\uD801\uDE27\uD801\uDE28\uD801\uDE29\uD801\uDE2A\uD801\uDE2B" +
                        "\uD801\uDE2C\uD801\uDE2D\uD801\uDE2E\uD801\uDE2F" +
                        "VeryLongName",
                "test::model::ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery" +
                        "VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery\uD808\uDC00\uD808\uDC17" +
                        "ryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName"
        };
        MapIterable<String, Pattern> patternBySeparator = buildPatternMap(getExpectedElementPrefixDirs(), getExpectedElementFilenameExtension());
        forEachFSSeparator(fsSeparator ->
        {
            Pattern pattern = patternBySeparator.get(fsSeparator);
            for (String path : longElementPaths)
            {
                String filePath = this.extension.getElementFilePath(path, fsSeparator);
                Assert.assertNotNull(path, filePath);
                if (!pattern.matcher(filePath).matches())
                {
                    Assert.fail("File path does not match the expected pattern\n\telement path: " + path + "\n\tfile path: " + filePath + "\n\tpattern: " + pattern.pattern());
                }
                int index = findInvalidName(filePath, fsSeparator);
                if (index > -1)
                {
                    Assert.fail("File path exceeds the file name size limit at index " + index + ": " + filePath);
                }
            }
        });
    }

    @Test
    public void testVeryLongModuleNames_Manifest()
    {
        String[] longModuleNames = {
                "very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_long_name",
                "very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_long_name",
                "very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_long_name",
                "very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very\uD802\uDF00ery_very_very_very_very_long_name",
                "very_\uD802\uDF00\uD802\uDF01\uD802\uDF02\uD802\uDF03\uD802\uDF04\uD802\uDF05\uD802\uDF06\uD802\uDF07\uD802\uDF08\uD802\uDF09\uD802\uDF0A" +
                        "\uD802\uDF0B\uD802\uDF0C\uD802\uDF0D\uD802\uDF0E\uD802\uDF0F\uD802\uDF10\uD802\uDF11\uD802\uDF12\uD802\uDF13\uD802\uDF14\uD802\uDF15" +
                        "\uD802\uDF16\uD802\uDF17\uD802\uDF18\uD802\uDF19\uD802\uDF1A\uD802\uDF1B\uD802\uDF1C\uD802\uDF1D\uD802\uDF1E\uD802\uDF1F\uD802\uDF20" +
                        "\uD802\uDF21\uD802\uDF22\uD802\uDF23\uD802\uDF24\uD802\uDF25\uD802\uDF26\uD802\uDF27\uD802\uDF28\uD802\uDF29\uD802\uDF2A\uD802\uDF2B" +
                        "\uD802\uDF2C\uD802\uDF2D\uD802\uDF2E\uD802\uDF2F\uD802\uDF30\uD802\uDF31\uD802\uDF32\uD802\uDF33\uD802\uDF34\uD802\uDF35_long_name"
        };
        MapIterable<String, Pattern> patternBySeparator = buildPatternMap(getExpectedModuleMetadataPrefixDirs(), getExpectedModuleManifestFilenameExtension());
        forEachFSSeparator(fsSeparator ->
        {
            Pattern pattern = patternBySeparator.get(fsSeparator);
            for (String moduleName : longModuleNames)
            {
                String filePath = this.extension.getModuleManifestFilePath(moduleName, fsSeparator);
                Assert.assertNotNull(moduleName, filePath);
                if (!pattern.matcher(filePath).matches())
                {
                    Assert.fail("File path does not match the expected pattern\n\tmodule name: " + moduleName + "\n\tfile path: " + filePath + "\n\tpattern: " + pattern.pattern());
                }
                int index = findInvalidName(filePath, fsSeparator);
                if (index > -1)
                {
                    Assert.fail("File path exceeds the file name size limit at index " + index + ": " + filePath);
                }
            }
        });
    }

    @Test
    public void testVeryLongModuleNames_SourceMetadata()
    {
        String[] longModuleNames = {
                "very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_long_name",
                "very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_long_name",
                "very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_long_name",
                "very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very\uD802\uDF00ery_very_very_very_very_long_name",
                "very_\uD802\uDF00\uD802\uDF01\uD802\uDF02\uD802\uDF03\uD802\uDF04\uD802\uDF05\uD802\uDF06\uD802\uDF07\uD802\uDF08\uD802\uDF09\uD802\uDF0A" +
                        "\uD802\uDF0B\uD802\uDF0C\uD802\uDF0D\uD802\uDF0E\uD802\uDF0F\uD802\uDF10\uD802\uDF11\uD802\uDF12\uD802\uDF13\uD802\uDF14\uD802\uDF15" +
                        "\uD802\uDF16\uD802\uDF17\uD802\uDF18\uD802\uDF19\uD802\uDF1A\uD802\uDF1B\uD802\uDF1C\uD802\uDF1D\uD802\uDF1E\uD802\uDF1F\uD802\uDF20" +
                        "\uD802\uDF21\uD802\uDF22\uD802\uDF23\uD802\uDF24\uD802\uDF25\uD802\uDF26\uD802\uDF27\uD802\uDF28\uD802\uDF29\uD802\uDF2A\uD802\uDF2B" +
                        "\uD802\uDF2C\uD802\uDF2D\uD802\uDF2E\uD802\uDF2F\uD802\uDF30\uD802\uDF31\uD802\uDF32\uD802\uDF33\uD802\uDF34\uD802\uDF35_long_name"
        };
        MapIterable<String, Pattern> patternBySeparator = buildPatternMap(getExpectedModuleMetadataPrefixDirs(), getExpectedModuleSourceMetadataFilenameExtension());
        forEachFSSeparator(fsSeparator ->
        {
            Pattern pattern = patternBySeparator.get(fsSeparator);
            for (String moduleName : longModuleNames)
            {
                String filePath = this.extension.getModuleSourceMetadataFilePath(moduleName, fsSeparator);
                Assert.assertNotNull(moduleName, filePath);
                if (!pattern.matcher(filePath).matches())
                {
                    Assert.fail("File path does not match the expected pattern\n\tmodule name: " + moduleName + "\n\tfile path: " + filePath + "\n\tpattern: " + pattern.pattern());
                }
                int index = findInvalidName(filePath, fsSeparator);
                if (index > -1)
                {
                    Assert.fail("File path exceeds the file name size limit at index " + index + ": " + filePath);
                }
            }
        });
    }

    @Test
    public void testVeryLongModuleNames_ExtRefMetadata()
    {
        String[] longModuleNames = {
                "very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_long_name",
                "very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_long_name",
                "very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_long_name",
                "very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very\uD802\uDF00ery_very_very_very_very_long_name",
                "very_\uD802\uDF00\uD802\uDF01\uD802\uDF02\uD802\uDF03\uD802\uDF04\uD802\uDF05\uD802\uDF06\uD802\uDF07\uD802\uDF08\uD802\uDF09\uD802\uDF0A" +
                        "\uD802\uDF0B\uD802\uDF0C\uD802\uDF0D\uD802\uDF0E\uD802\uDF0F\uD802\uDF10\uD802\uDF11\uD802\uDF12\uD802\uDF13\uD802\uDF14\uD802\uDF15" +
                        "\uD802\uDF16\uD802\uDF17\uD802\uDF18\uD802\uDF19\uD802\uDF1A\uD802\uDF1B\uD802\uDF1C\uD802\uDF1D\uD802\uDF1E\uD802\uDF1F\uD802\uDF20" +
                        "\uD802\uDF21\uD802\uDF22\uD802\uDF23\uD802\uDF24\uD802\uDF25\uD802\uDF26\uD802\uDF27\uD802\uDF28\uD802\uDF29\uD802\uDF2A\uD802\uDF2B" +
                        "\uD802\uDF2C\uD802\uDF2D\uD802\uDF2E\uD802\uDF2F\uD802\uDF30\uD802\uDF31\uD802\uDF32\uD802\uDF33\uD802\uDF34\uD802\uDF35_long_name"
        };
        MapIterable<String, Pattern> patternBySeparator = buildPatternMap(getExpectedModuleMetadataPrefixDirs(), getExpectedModuleExternalReferenceMetadataFilenameExtension());
        forEachFSSeparator(fsSeparator ->
        {
            Pattern pattern = patternBySeparator.get(fsSeparator);
            for (String moduleName : longModuleNames)
            {
                String filePath = this.extension.getModuleExternalReferenceMetadataFilePath(moduleName, fsSeparator);
                Assert.assertNotNull(moduleName, filePath);
                if (!pattern.matcher(filePath).matches())
                {
                    Assert.fail("File path does not match the expected pattern\n\tmodule name: " + moduleName + "\n\tfile path: " + filePath + "\n\tpattern: " + pattern.pattern());
                }
                int index = findInvalidName(filePath, fsSeparator);
                if (index > -1)
                {
                    Assert.fail("File path exceeds the file name size limit at index " + index + ": " + filePath);
                }
            }
        });
    }

    @Test
    public void testVeryLongModuleAndElementNames()
    {
        String[] longModuleNames = {
                "very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_long_name",
                "very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_long_name",
                "very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_long_name",
                "very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very\uD802\uDF00ery_very_very_very_very_long_name",
                "very_very_\uD802\uDF00\uD802\uDF01\uD802\uDF02\uD802\uDF03\uD802\uDF04\uD802\uDF05\uD802\uDF06\uD802\uDF07\uD802\uDF08\uD802\uDF09\uD802\uDF0A" +
                        "\uD802\uDF0B\uD802\uDF0C\uD802\uDF0D\uD802\uDF0E\uD802\uDF0F\uD802\uDF10\uD802\uDF11\uD802\uDF12\uD802\uDF13\uD802\uDF14\uD802\uDF15" +
                        "\uD802\uDF16\uD802\uDF17\uD802\uDF18\uD802\uDF19\uD802\uDF1A\uD802\uDF1B\uD802\uDF1C\uD802\uDF1D\uD802\uDF1E\uD802\uDF1F\uD802\uDF20" +
                        "\uD802\uDF21\uD802\uDF22\uD802\uDF23\uD802\uDF24\uD802\uDF25\uD802\uDF26\uD802\uDF27\uD802\uDF28\uD802\uDF29\uD802\uDF2A\uD802\uDF2B" +
                        "\uD802\uDF2C\uD802\uDF2D\uD802\uDF2E\uD802\uDF2F\uD802\uDF30\uD802\uDF31\uD802\uDF32\uD802\uDF33\uD802\uDF34\uD802\uDF35_long_name"
        };
        String[] longElementPaths = {
                "test::model::ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName",
                "test::model::ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName",
                "test::model::ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName",
                "test::model::ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName",
                "test::model::ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery" +
                        "VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery" +
                        "VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName",
                "test::model::packageWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName::MyClass",
                "test::model::packageWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery" +
                        "VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery" +
                        "VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName::MyClass",
                "test::model::ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery\uD808\uDC00\uD808\uDC17ryLongName",
                "test::model::ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery\uD808\uDC00\uD808\uDC17ryVeryVeryLongName",
                "test::model::ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryV\uD808\uDC00\uD808\uDC17yVeryVeryLongName",
                "test::model::ClassWithAVeryVeryVery" +
                        "\uD801\uDE00\uD801\uDE01\uD801\uDE02\uD801\uDE03\uD801\uDE04\uD801\uDE05\uD801\uDE06\uD801\uDE07\uD801\uDE08\uD801\uDE09\uD801\uDE0A" +
                        "\uD801\uDE0B\uD801\uDE0C\uD801\uDE0D\uD801\uDE0E\uD801\uDE0F\uD801\uDE10\uD801\uDE11\uD801\uDE12\uD801\uDE13\uD801\uDE14\uD801\uDE15" +
                        "\uD801\uDE16\uD801\uDE17\uD801\uDE18\uD801\uDE19\uD801\uDE1A\uD801\uDE1B\uD801\uDE1C\uD801\uDE1D\uD801\uDE1E\uD801\uDE1F\uD801\uDE20" +
                        "\uD801\uDE21\uD801\uDE22\uD801\uDE23\uD801\uDE24\uD801\uDE25\uD801\uDE26\uD801\uDE27\uD801\uDE28\uD801\uDE29\uD801\uDE2A\uD801\uDE2B" +
                        "\uD801\uDE2C\uD801\uDE2D\uD801\uDE2E\uD801\uDE2F" +
                        "VeryLongName",
                "test::model::ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery" +
                        "VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery\uD808\uDC00\uD808\uDC17" +
                        "ryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName"
        };
        MapIterable<String, Pattern> patternBySeparator = buildPatternMap(getExpectedModuleMetadataPrefixDirs(), getExpectedModuleElementBackReferenceMetadataFilenameExtension());
        forEachFSSeparator(fsSeparator ->
        {
            Pattern pattern = patternBySeparator.get(fsSeparator);
            for (String moduleName : longModuleNames)
            {
                for (String elementPath : longElementPaths)
                {
                    String filePath = this.extension.getModuleElementBackReferenceMetadataFilePath(moduleName, elementPath, fsSeparator);
                    Assert.assertNotNull(moduleName, filePath);
                    if (!pattern.matcher(filePath).matches())
                    {
                        Assert.fail("File path does not match the expected pattern\n\tmodule name: " + moduleName + "\n\telement path: " + elementPath + "\n\tfile path: " + filePath + "\n\tpattern: " + pattern.pattern());
                    }
                    int index = findInvalidName(filePath, fsSeparator);
                    if (index > -1)
                    {
                        Assert.fail("File path exceeds the file name size limit at index " + index + ": " + filePath);
                    }
                }
            }
        });
    }

    protected abstract ListIterable<String> getExpectedElementPrefixDirs();

    protected abstract String getExpectedElementFilenameExtension();

    protected abstract ListIterable<String> getExpectedModuleMetadataPrefixDirs();

    protected abstract String getExpectedModuleManifestFilenameExtension();

    protected abstract String getExpectedModuleSourceMetadataFilenameExtension();

    protected abstract String getExpectedModuleExternalReferenceMetadataFilenameExtension();

    protected abstract String getExpectedModuleElementBackReferenceMetadataFilenameExtension();

    protected abstract FilePathProviderExtension getExtension();

    private static int findInvalidName(String filePath, String fsSeparator)
    {
        int start = 0;
        int end;
        while ((end = filePath.indexOf(fsSeparator, start)) != -1)
        {
            if (getUTF16Len(filePath, start, end) > 255)
            {
                return start;
            }
            start = end + fsSeparator.length();
        }
        return (getUTF16Len(filePath, start, filePath.length()) > 255) ? start : -1;
    }

    private static int getUTF16Len(String string, int start, int end)
    {
        return string.substring(start, end).getBytes(StandardCharsets.UTF_16).length;
    }

    private static void forEachFSSeparator(Consumer<? super String> consumer)
    {
        consumer.accept("/");
        consumer.accept("\\");
    }

    private static MapIterable<String, Pattern> buildPatternMap(ListIterable<String> expectedPrefixDirs, String expectedExtension)
    {
        MutableMap<String, Pattern> patterns = Maps.mutable.empty();
        forEachFSSeparator(sep -> patterns.put(sep, buildFilePathPattern(expectedPrefixDirs, sep, expectedExtension)));
        return patterns;
    }

    private static Pattern buildFilePathPattern(ListIterable<String> prefixDirs, String separator, String ext)
    {
        StringBuilder builder = new StringBuilder();
        if ((prefixDirs != null) && prefixDirs.notEmpty())
        {
            builder.append("\\Q");
            prefixDirs.forEach(dir -> builder.append(dir).append(separator));
            builder.append("\\E");
        }
        builder.append("([\\w$]++\\Q").append(separator).append("\\E)*+[\\w$]++");
        if ((ext != null) && !ext.isEmpty())
        {
            builder.append("\\Q").append(ext).append("\\E");
        }
        return Pattern.compile(builder.toString(), Pattern.UNICODE_CHARACTER_CLASS);
    }
}
