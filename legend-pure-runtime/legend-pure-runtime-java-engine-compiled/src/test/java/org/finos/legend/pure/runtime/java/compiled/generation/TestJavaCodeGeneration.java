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

package org.finos.legend.pure.runtime.java.compiled.generation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.statelistener.VoidExecutionActivityListener;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.orchestrator.JavaCodeGeneration;
import org.finos.legend.pure.runtime.java.compiled.generation.orchestrator.JavaCodeGeneration.GenerationType;
import org.finos.legend.pure.runtime.java.compiled.generation.orchestrator.VoidLog;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class TestJavaCodeGeneration
{
    @ClassRule
    public static TemporaryFolder TMP = new TemporaryFolder();

    @Test
    public void testMonolithicGenerationNoExternal() throws Exception
    {
        File directory = TMP.newFolder();
        File classesDirectory = new File(directory, "classes");
        classesDirectory.mkdir();
        JavaCodeGeneration.doIt(
                Sets.mutable.with("platform"),
                Sets.fixedSize.empty(),
                Sets.fixedSize.empty(),
                GenerationType.monolithic,
                false,
                false,
                null,
                true,
                true,
                false,
                false,
                false,
                classesDirectory,
                directory,
                true,
                new VoidLog());
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{classesDirectory.toURI().toURL()}, Thread.currentThread().getContextClassLoader()))
        {
            CompiledExecutionSupport executionSupport = new CompiledExecutionSupport(
                    new JavaCompilerState(null, classLoader),
                    new CompiledProcessorSupport(classLoader, MetadataLazy.fromClassLoader(classLoader)),
                    null,
                    new ClassLoaderCodeStorage(classLoader),
                    null,
                    VoidExecutionActivityListener.VOID_EXECUTION_ACTIVITY_LISTENER,
                    new ConsoleCompiled(),
                    null,
                    null,
                    CompiledExtensionLoader.extensions()
            );

            String className = JavaPackageAndImportBuilder.getRootPackage() + ".platform_pure_essential_string_toString_joinStrings";
            Class<?> testClass = classLoader.loadClass(className);

            Method joinWithCommas = testClass.getMethod("Root_meta_pure_functions_string_joinStrings_String_MANY__String_1__String_1_", RichIterable.class, String.class, ExecutionSupport.class);
            Object result1 = joinWithCommas.invoke(null, Lists.immutable.with("a", "b", "c"), ", ", executionSupport);
            Assert.assertEquals("a, b, c", result1);
        }
    }

    @Test
    public void testMonolithicGenerationWithExternal() throws Exception
    {
        String externalPackage = "org.finos.legend.pure.runtime.java.compiled";
        File directory = TMP.newFolder();
        File classesDirectory = new File(directory, "classes");
        classesDirectory.mkdir();
        JavaCodeGeneration.doIt(
                Sets.mutable.with("platform"),
                Sets.fixedSize.empty(),
                Sets.fixedSize.empty(),
                GenerationType.monolithic,
                false,
                true,
                externalPackage,
                true,
                true,
                false,
                false,
                false,
                classesDirectory,
                directory,
                true,
                new VoidLog());
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{classesDirectory.toURI().toURL()}, Thread.currentThread().getContextClassLoader()))
        {
            Class<?> pureExternal = classLoader.loadClass(externalPackage + ".PureExternal");
            Method getExecutionSupport = pureExternal.getMethod("_getExecutionSupport");
            CompiledExecutionSupport executionSupport = (CompiledExecutionSupport) getExecutionSupport.invoke(null);

            String className = JavaPackageAndImportBuilder.getRootPackage() + ".platform_pure_essential_string_toString_joinStrings";
            Class<?> testClass = classLoader.loadClass(className);

            Method joinWithCommas = testClass.getMethod("Root_meta_pure_functions_string_joinStrings_String_MANY__String_1__String_1_", RichIterable.class, String.class, ExecutionSupport.class);
            Object result1 = joinWithCommas.invoke(null, Lists.immutable.with("a", "b", "c"), ", ", executionSupport);
            Assert.assertEquals("a, b, c", result1);
        }
    }
}
