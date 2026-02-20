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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance;

import org.eclipse.collections.api.map.MapIterable;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.serialization.compiler.element.AbstractPackageableElementBuilderTest;
import org.finos.legend.pure.m3.serialization.compiler.element.ElementBuilder;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;
import org.junit.Assert;
import org.junit.Test;

public class TestCompiledElementBuilder extends AbstractPackageableElementBuilderTest
{
    @Test
    @Override
    public void testPrimitiveValueClassifiers()
    {
        CompiledProcessorSupport compiledProcessorSupport = new CompiledProcessorSupport(
                Thread.currentThread().getContextClassLoader(),
                new Metadata()
                {
                    @Override
                    public void startTransaction()
                    {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void commitTransaction()
                    {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void rollbackTransaction()
                    {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public CoreInstance getMetadata(String classifier, String id)
                    {
                        if (M3Paths.PrimitiveType.equals(classifier))
                        {
                            return elementLoader.loadElement(id);
                        }
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public MapIterable<String, CoreInstance> getMetadata(String classifier)
                    {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public CoreInstance getEnum(String enumerationName, String enumName)
                    {
                        throw new UnsupportedOperationException();
                    }
                },
                null);
        CoreInstance stringType = this.elementLoader.loadElement(M3Paths.String);
        CoreInstance stringTypeName = stringType.getValueForMetaPropertyToOne(M3Properties.name);
        Assert.assertSame(stringType, compiledProcessorSupport.getClassifier(stringTypeName));
    }

    @Override
    protected ElementBuilder newElementBuilder()
    {
        return CompiledElementBuilder.newElementBuilder(Thread.currentThread().getContextClassLoader());
    }

    @Override
    protected String getExpectedVirtualPackageClassName()
    {
        return JavaPackageAndImportBuilder.buildLazyVirtualPackageClassReference();
    }

    @Override
    protected String getExpectedConcreteElementClassName(String classifierPath)
    {
        return JavaPackageAndImportBuilder.buildLazyConcreteElementClassReferenceFromUserPath(classifierPath);
    }

    @Override
    protected String getExpectedComponentInstanceClassName(String classifierPath)
    {
        return JavaPackageAndImportBuilder.buildLazyComponentInstanceClassReferenceFromUserPath(classifierPath);
    }
}
