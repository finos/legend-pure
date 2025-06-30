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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance;

import org.finos.legend.pure.m3.coreinstance.lazy.generator.TestM3GeneratedLazyElementBuilder;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.serialization.compiler.element.ElementBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;

public class TestCompiledIncrementalCompilationElementBuilder extends TestM3GeneratedLazyElementBuilder
{
    @Override
    protected ElementBuilder newElementBuilder()
    {
        return CompiledIncrementalCompilationElementBuilder.newElementBuilder(Thread.currentThread().getContextClassLoader(), this.elementModelRepository);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Class<? extends PackageableElement> getExpectedConcreteElementClass(String classifierPath)
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            return (Class<? extends PackageableElement>) classLoader.loadClass(JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassReferenceFromUserPath(classifierPath));
        }
        catch (ClassNotFoundException ignore)
        {
            try
            {
                return (Class<? extends PackageableElement>) classLoader.loadClass(super.getExpectedConcreteElementClassName(classifierPath));
            }
            catch (ClassNotFoundException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Class<? extends Any> getExpectedComponentInstanceClass(String classifierPath)
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            return (Class<? extends PackageableElement>) classLoader.loadClass(JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassReferenceFromUserPath(classifierPath));
        }
        catch (ClassNotFoundException ignore)
        {
            try
            {
                return (Class<? extends PackageableElement>) classLoader.loadClass(super.getExpectedComponentInstanceClassName(classifierPath));
            }
            catch (ClassNotFoundException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected String getExpectedConcreteElementClassName(String classifierPath)
    {
        throw new IllegalStateException("This should not be called");
    }

    @Override
    protected String getExpectedComponentInstanceClassName(String classifierPath)
    {
        throw new IllegalStateException("This should not be called");
    }
}
