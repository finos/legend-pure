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

package org.finos.legend.pure.m3.coreinstance.lazy.generator;

import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.serialization.compiler.element.AbstractPackageableElementBuilderTest;
import org.finos.legend.pure.m3.serialization.compiler.element.ElementBuilder;
import org.junit.Test;

public class TestM3GeneratedLazyElementBuilder extends AbstractPackageableElementBuilderTest
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
        return M3GeneratedLazyElementBuilder.newElementBuilder(Thread.currentThread().getContextClassLoader(), this.elementModelRepository);
    }

    @Override
    protected String getExpectedVirtualPackageClassName()
    {
        return M3LazyCoreInstanceGenerator.buildLazyVirtualPackageClassReference();
    }

    @Override
    protected String getExpectedConcreteElementClassName(String classifierPath)
    {
        return M3LazyCoreInstanceGenerator.buildLazyConcreteElementClassReferenceFromUserPath(classifierPath);
    }

    @Override
    protected String getExpectedComponentInstanceClassName(String classifierPath)
    {
        return M3LazyCoreInstanceGenerator.buildLazyComponentInstanceClassReferenceFromUserPath(classifierPath);
    }
}
