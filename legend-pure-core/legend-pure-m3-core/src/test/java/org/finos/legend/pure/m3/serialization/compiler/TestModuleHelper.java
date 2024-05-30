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

package org.finos.legend.pure.m3.serialization.compiler;

import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.compiler.reference.AbstractReferenceTest;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m3.tools.GraphTools;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Objects;

public class TestModuleHelper extends AbstractReferenceTest
{
    @BeforeClass
    public static void setUpRuntime()
    {
        setUpRuntime(getFunctionExecution(), new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories())), getExtra());
    }

    @Test
    public void testResolveModuleName()
    {
        Assert.assertEquals(ModuleHelper.ROOT_MODULE_NAME, ModuleHelper.resolveModuleName(null));

        Assert.assertSame("platform", ModuleHelper.resolveModuleName("platform"));
        Assert.assertSame(ModuleHelper.ROOT_MODULE_NAME, ModuleHelper.resolveModuleName(ModuleHelper.ROOT_MODULE_NAME));
        Assert.assertSame("xyz", ModuleHelper.resolveModuleName("xyz"));
    }

    @Test
    public void testIsRootModule()
    {
        Assert.assertTrue(ModuleHelper.isRootModule(ModuleHelper.ROOT_MODULE_NAME));
        Assert.assertTrue(ModuleHelper.isRootModule("root"));

        Assert.assertFalse(ModuleHelper.isRootModule(null));
        Assert.assertFalse(ModuleHelper.isRootModule("platform"));
        Assert.assertFalse(ModuleHelper.isRootModule("xyz"));
    }

    @Test
    public void testNonIsRootModule()
    {
        Assert.assertFalse(ModuleHelper.isNonRootModule(ModuleHelper.ROOT_MODULE_NAME));
        Assert.assertFalse(ModuleHelper.isNonRootModule("root"));
        Assert.assertFalse(ModuleHelper.isNonRootModule(null));

        Assert.assertTrue(ModuleHelper.isNonRootModule("platform"));
        Assert.assertTrue(ModuleHelper.isNonRootModule("xyz"));
    }

    @Test
    public void testGetElementModule()
    {
        GraphTools.getTopLevelAndPackagedElements(processorSupport).forEach(element ->
        {
            String path = PackageableElement.getUserPathForPackageableElement(element);
            SourceInformation sourceInfo = element.getSourceInformation();
            if (sourceInfo == null)
            {
                Assert.assertNull(path, ModuleHelper.getElementModule(element));
            }
            else
            {
                String sourceId = sourceInfo.getSourceId();
                int secondSlash = sourceId.indexOf('/', 1);
                if (secondSlash == -1)
                {
                    Assert.assertSame(path, ModuleHelper.ROOT_MODULE_NAME, ModuleHelper.getElementModule(element));
                }
                else
                {
                    String expectedModule = sourceId.substring(1, secondSlash);
                    Assert.assertEquals(path, expectedModule, ModuleHelper.getElementModule(element));
                }
            }
        });
    }

    @Test
    public void testGetSourceModule_Source()
    {
        Assert.assertNull(ModuleHelper.getSourceModule((Source) null));
        runtime.getSourceRegistry().getSources().forEach(source ->
        {
            String sourceId = source.getId();
            int secondSlash = sourceId.indexOf('/', 1);
            if (secondSlash == -1)
            {
                Assert.assertSame(sourceId, ModuleHelper.ROOT_MODULE_NAME, ModuleHelper.getSourceModule(source));
            }
            else
            {
                String expectedModule = sourceId.substring(1, secondSlash);
                Assert.assertEquals(sourceId, expectedModule, ModuleHelper.getSourceModule(source));
            }
        });
    }

    @Test
    public void testGetSourceModule_SourceInfo()
    {
        Assert.assertNull(ModuleHelper.getSourceModule((SourceInformation) null));
        Assert.assertNull(ModuleHelper.getSourceModule(new SourceInformation(null, -1, -1, -1, -1)));

        GraphTools.getTopLevelAndPackagedElements(processorSupport)
                .collect(CoreInstance::getSourceInformation)
                .select(Objects::nonNull)
                .forEach(sourceInfo ->
                {
                    String sourceId = sourceInfo.getSourceId();
                    int secondSlash = sourceId.indexOf('/', 1);
                    if (secondSlash == -1)
                    {
                        Assert.assertSame(sourceInfo.getMessage(), ModuleHelper.ROOT_MODULE_NAME, ModuleHelper.getSourceModule(sourceInfo));
                    }
                    else
                    {
                        String expectedModule = sourceId.substring(1, secondSlash);
                        Assert.assertEquals(sourceInfo.getMessage(), expectedModule, ModuleHelper.getSourceModule(sourceInfo));
                    }
                });
    }

    @Test
    public void testGetSourceModule_SourceId()
    {
        Assert.assertNull(ModuleHelper.getSourceModule((String) null));

        Assert.assertSame(ModuleHelper.ROOT_MODULE_NAME, ModuleHelper.getSourceModule("/welcome.pure"));
        Assert.assertSame(ModuleHelper.ROOT_MODULE_NAME, ModuleHelper.getSourceModule("welcome.pure"));
        Assert.assertSame(ModuleHelper.ROOT_MODULE_NAME, ModuleHelper.getSourceModule("/xyz.pure"));
        Assert.assertSame(ModuleHelper.ROOT_MODULE_NAME, ModuleHelper.getSourceModule("xyz.pure"));

        Assert.assertEquals("platform", ModuleHelper.getSourceModule("/platform/someFile.pure"));
        Assert.assertEquals("platform", ModuleHelper.getSourceModule("platform/someFile.pure"));
        Assert.assertEquals("example", ModuleHelper.getSourceModule("/example/of/a/source/path/someFile.pure"));
        Assert.assertEquals("example", ModuleHelper.getSourceModule("example/of/a/source/path/someFile.pure"));

        runtime.getSourceRegistry().getSources().forEach(source ->
        {
            String sourceId = source.getId();
            int secondSlash = sourceId.indexOf('/', 1);
            if (secondSlash == -1)
            {
                Assert.assertSame(sourceId, ModuleHelper.ROOT_MODULE_NAME, ModuleHelper.getSourceModule(sourceId));
            }
            else
            {
                String expectedModule = sourceId.substring(1, secondSlash);
                Assert.assertEquals(sourceId, expectedModule, ModuleHelper.getSourceModule(sourceId));
            }
        });
    }

    @Test
    public void testIsElementInModule()
    {
        GraphTools.getTopLevelAndPackagedElements(processorSupport).forEach(element ->
        {
            String path = PackageableElement.getUserPathForPackageableElement(element);
            SourceInformation sourceInfo = element.getSourceInformation();
            if (sourceInfo == null)
            {
                Assert.assertFalse(path, ModuleHelper.isElementInModule(element, ModuleHelper.ROOT_MODULE_NAME));
                Assert.assertFalse(path, ModuleHelper.isElementInModule(element, null));
                Assert.assertFalse(path, ModuleHelper.isElementInModule(element, "platform"));
            }
            else
            {
                String sourceId = sourceInfo.getSourceId();
                int secondSlash = sourceId.indexOf('/', 1);
                if (secondSlash == -1)
                {
                    Assert.assertTrue(path, ModuleHelper.isElementInModule(element, ModuleHelper.ROOT_MODULE_NAME));
                    Assert.assertTrue(path, ModuleHelper.isElementInModule(element, null));
                    Assert.assertFalse(path, ModuleHelper.isElementInModule(element, "platform"));
                }
                else
                {
                    String expectedModule = sourceId.substring(1, secondSlash);
                    Assert.assertTrue(path, ModuleHelper.isElementInModule(element, expectedModule));
                }
            }
            Assert.assertFalse(path, ModuleHelper.isElementInModule(element, ""));
            Assert.assertFalse(path, ModuleHelper.isElementInModule(element, "example"));
            Assert.assertFalse(path, ModuleHelper.isElementInModule(element, "not_a_module"));
        });
    }

    @Test
    public void testIsSourceInModule_Source()
    {
        Assert.assertFalse(ModuleHelper.isSourceInModule((Source) null, ModuleHelper.ROOT_MODULE_NAME));
        Assert.assertFalse(ModuleHelper.isSourceInModule((Source) null, null));
        Assert.assertFalse(ModuleHelper.isSourceInModule((Source) null, "platform"));
        runtime.getSourceRegistry().getSources().forEach(source ->
        {
            String sourceId = source.getId();
            int secondSlash = sourceId.indexOf('/', 1);
            if (secondSlash == -1)
            {
                Assert.assertTrue(sourceId, ModuleHelper.isSourceInModule(source, ModuleHelper.ROOT_MODULE_NAME));
                Assert.assertTrue(sourceId, ModuleHelper.isSourceInModule(source, null));
                Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(source, "platform"));
            }
            else
            {
                String expectedModule = sourceId.substring(1, secondSlash);
                Assert.assertTrue(sourceId, ModuleHelper.isSourceInModule(source, expectedModule));
            }
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(source, ""));
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(source, "example"));
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(source, "not_a_module"));
        });
    }

    @Test
    public void testIsSourceInModule_SourceInfo()
    {
        Assert.assertFalse(ModuleHelper.isSourceInModule((SourceInformation) null, ModuleHelper.ROOT_MODULE_NAME));
        Assert.assertFalse(ModuleHelper.isSourceInModule((SourceInformation) null, null));
        Assert.assertFalse(ModuleHelper.isSourceInModule((SourceInformation) null, "platform"));
        GraphTools.getTopLevelAndPackagedElements(processorSupport)
                .collect(CoreInstance::getSourceInformation)
                .select(Objects::nonNull)
                .forEach(sourceInfo ->
                {
                    String sourceId = sourceInfo.getSourceId();
                    int secondSlash = sourceId.indexOf('/', 1);
                    if (secondSlash == -1)
                    {
                        Assert.assertTrue(sourceId, ModuleHelper.isSourceInModule(sourceInfo, ModuleHelper.ROOT_MODULE_NAME));
                        Assert.assertTrue(sourceId, ModuleHelper.isSourceInModule(sourceInfo, null));
                        Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceInfo, "platform"));
                    }
                    else
                    {
                        String expectedModule = sourceId.substring(1, secondSlash);
                        Assert.assertTrue(sourceId, ModuleHelper.isSourceInModule(sourceInfo, expectedModule));
                    }
                    Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceInfo, ""));
                    Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceInfo, "example"));
                    Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceInfo, "not_a_module"));
                });
    }

    @Test
    public void testIsSourceInModule_SourceId()
    {
        String[] vacuousSourceIds = {null, ""};
        String[] rootSourceIds = {"/welcome.pure", "welcome.pure", "/xyz.pure", "xyz.pure"};
        String[] platformSourceIds = {"/platform/someFile.pure", "platform/someFile.pure", "/platform/pure/grammar/functions/math/operation/minus.pure"};
        String[] exampleSourceIds = {"/example/of/a/source/path/someFile.pure", "example/of/another/source/path.pure"};

        for (String sourceId : vacuousSourceIds)
        {
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceId, ModuleHelper.ROOT_MODULE_NAME));
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceId, null));
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceId, "platform"));
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceId, "example"));
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceId, ""));
        }

        for (String sourceId : rootSourceIds)
        {
            Assert.assertTrue(sourceId, ModuleHelper.isSourceInModule(sourceId, ModuleHelper.ROOT_MODULE_NAME));
            Assert.assertTrue(sourceId, ModuleHelper.isSourceInModule(sourceId, null));
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceId, "platform"));
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceId, "example"));
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceId, ""));
        }

        for (String sourceId : platformSourceIds)
        {
            Assert.assertTrue(sourceId, ModuleHelper.isSourceInModule(sourceId, "platform"));
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceId, ModuleHelper.ROOT_MODULE_NAME));
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceId, null));
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceId, "example"));
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceId, ""));
        }

        for (String sourceId : exampleSourceIds)
        {
            Assert.assertTrue(sourceId, ModuleHelper.isSourceInModule(sourceId, "example"));
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceId, ModuleHelper.ROOT_MODULE_NAME));
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceId, null));
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceId, "platform"));
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceId, ""));
        }

        runtime.getSourceRegistry().getSources().forEach(source ->
        {
            String sourceId = source.getId();
            int secondSlash = sourceId.indexOf('/', 1);
            if (secondSlash == -1)
            {
                Assert.assertTrue(sourceId, ModuleHelper.isSourceInModule(sourceId, ModuleHelper.ROOT_MODULE_NAME));
                Assert.assertTrue(sourceId, ModuleHelper.isSourceInModule(sourceId, null));
            }
            else
            {
                String expectedModule = sourceId.substring(1, secondSlash);
                Assert.assertTrue(sourceId, ModuleHelper.isSourceInModule(sourceId, expectedModule));
            }
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceId, ""));
            Assert.assertFalse(sourceId, ModuleHelper.isSourceInModule(sourceId, "not_a_module"));
        });
    }
}
