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

import org.finos.legend.pure.m3.coreinstance.lazy.generator.M3GeneratedLazyElementBuilder;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompiledIncrementalCompilationElementBuilder extends M3GeneratedLazyElementBuilder
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CompiledIncrementalCompilationElementBuilder.class);

    private CompiledIncrementalCompilationElementBuilder(ClassLoader classLoader, ModelRepository repository)
    {
        super(classLoader, repository);
    }

    @Override
    protected Class<? extends CoreInstance> getConcreteElementClass(String classifierPath) throws ClassNotFoundException
    {
        try
        {
            String javaClassName = JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassReferenceFromUserPath(classifierPath);
            Class<? extends CoreInstance> cls = loadJavaClass(javaClassName);
            LOGGER.debug("Found concrete element comp class {} for {}", javaClassName, classifierPath);
            return cls;
        }
        catch (ClassNotFoundException ignore)
        {
            // no comp class, fall back to ordinary class
            return super.getConcreteElementClass(classifierPath);
        }
    }

    @Override
    protected Class<? extends CoreInstance> getComponentElementClass(String classifierPath) throws ClassNotFoundException
    {
        try
        {
            String javaClassName = JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassReferenceFromUserPath(classifierPath);
            Class<? extends CoreInstance> cls = loadJavaClass(javaClassName);
            LOGGER.debug("Found component instance comp class {} for {}", javaClassName, classifierPath);
            return cls;
        }
        catch (ClassNotFoundException ignore)
        {
            // no comp class, fall back to ordinary class
            return super.getComponentElementClass(classifierPath);
        }
    }

    public static CompiledIncrementalCompilationElementBuilder newElementBuilder(ClassLoader classLoader, ModelRepository repository)
    {
        return new CompiledIncrementalCompilationElementBuilder(classLoader, repository);
    }
}
