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

package org.finos.legend.pure.m3.coreinstance.lazy.simple;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.compiler.element.AbstractElementBuilderTest;
import org.finos.legend.pure.m3.serialization.compiler.element.ElementBuilder;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Test;

public class TestSimpleElementBuilder extends AbstractElementBuilderTest<SimpleLazyVirtualPackage, SimpleLazyConcreteElement, SimpleLazyComponentInstance>
{
    @Test
    @Override
    public void testBuildRootPackage()
    {
        testConcreteElement(M3Paths.Root, M3Paths.Package, false);
    }

    @Test
    @Override
    public void testBuildPackageClass()
    {
        testConcreteElement(M3Paths.Package, M3Paths.Class, false);
    }

    @Test
    @Override
    public void testBuildInteger()
    {
        testConcreteElement(M3Paths.Integer, M3Paths.PrimitiveType, false);
    }

    @Test
    @Override
    public void testBuildNumber()
    {
        testConcreteElement(M3Paths.Number, M3Paths.PrimitiveType, false);
    }

    @Test
    @Override
    public void testBuildString()
    {
        testConcreteElement(M3Paths.String, M3Paths.PrimitiveType, false);
    }

    @Override
    protected ElementBuilder newElementBuilder()
    {
        return SimpleElementBuilder.newElementBuilder(this.elementModelRepository);
    }

    @Override
    protected Class<? extends SimpleLazyVirtualPackage> getExpectedVirtualPackageClass()
    {
        return SimpleLazyVirtualPackage.class;
    }

    @Override
    protected Class<? extends SimpleLazyConcreteElement> getExpectedConcreteElementClass(String classifier)
    {
        return SimpleLazyConcreteElement.class;
    }

    @Override
    protected Class<? extends SimpleLazyComponentInstance> getExpectedComponentInstanceClass(String classifier)
    {
        return SimpleLazyComponentInstance.class;
    }

    @Override
    protected void testConcreteElement(String path, String classifierPath, SimpleLazyConcreteElement element)
    {
        super.testConcreteElement(path, classifierPath, element);
        testElement(path, element);
    }

    @Override
    protected void testVirtualPackage(String path, SimpleLazyVirtualPackage element)
    {
        super.testVirtualPackage(path, element);
        testElement(path, element);
    }

    private void testElement(String path, CoreInstance element)
    {
        CoreInstance srcElement = runtime.getCoreInstance(path);

        Assert.assertEquals(path, srcElement.getName(), element.getName());
        Assert.assertEquals(path, getUserPath(srcElement.getClassifier()), getUserPath(element.getClassifier()));
        Assert.assertEquals(path, srcElement.getSourceInformation(), element.getSourceInformation());

        srcElement.getKeys().toSet().withAll(element.getKeys()).forEach(key ->
        {
            String message = path + "." + key;

            ListIterable<? extends CoreInstance> srcValues = srcElement.getValueForMetaPropertyToMany(key);
            ListIterable<? extends CoreInstance> values = element.getValueForMetaPropertyToMany(key);
            Assert.assertEquals(message, srcValues.size(), values.size());

            if (srcValues.notEmpty() &&
                    !M3Properties.children.equals(key) &&
                    !M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.contains(srcElement.getRealKeyByName(key)) &&
                    !M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.contains(element.getRealKeyByName(key)))
            {
                srcValues.forEachWithIndex((srcValue, i) ->
                {
                    String message2 = message + "[" + i + "]";
                    CoreInstance value = values.get(i);
                    Assert.assertEquals(message2, getUserPath(srcValue.getClassifier()), getUserPath(value.getClassifier()));
                    if (PackageableElement.isPackageableElement(srcValue, processorSupport))
                    {
                        Assert.assertEquals(message2, getUserPath(srcValue), getUserPath(value));
                    }
                });
            }
        });
    }
}
