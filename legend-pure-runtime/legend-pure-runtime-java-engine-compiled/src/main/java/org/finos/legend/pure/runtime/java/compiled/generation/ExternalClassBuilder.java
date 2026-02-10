// Copyright 2023 Goldman Sachs
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
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.statelistener.ExecutionActivityListener;
import org.finos.legend.pure.m3.tools.JavaTools;
import org.finos.legend.pure.m3.tools.ReflectionTools;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy;

public class ExternalClassBuilder
{
    private static final ImmutableList<String> BASE_EXTERNALIZABLE_IMPORTS = Lists.immutable.with(
            ClassLoaderCodeStorage.class.getName(),
            CodeRepository.class.getName(),
            CodeRepositoryProviderHelper.class.getName(),
            CompiledExecutionSupport.class.getName(),
            CompiledExtensionLoader.class.getName(),
            CompiledProcessorSupport.class.getName(),
            CompositeCodeStorage.class.getName(),
            ConsoleCompiled.class.getName(),
            ExecutionActivityListener.class.getName(),
            ExecutionSupport.class.getName(),
            JavaCompilerState.class.getName(),
            Lists.class.getName(),
            MetadataLazy.class.getName(),
            MutableList.class.getName(),
            MutableSet.class.getName(),
            PureException.class.getName(),
            ReflectionTools.class.getName(),
            RichIterable.class.getName(),
            Sets.class.getName());

    @Deprecated
    public static String buildExternalizableFunctionClass(RichIterable<String> functionDefinitions, String externalFunctionClass, RichIterable<String> vcsRepos, RichIterable<String> classLoaderRepos)
    {
        return buildExternalizableFunctionClass((String) null, externalFunctionClass, functionDefinitions, Lists.mutable.withAll(classLoaderRepos).withAll(vcsRepos));
    }

    static String buildExternalizableFunctionClass(String pkg, String className, RichIterable<String> functionDefinitions, RichIterable<String> classLoaderRepos)
    {
        StringBuilder builder = new StringBuilder();
        if (pkg != null)
        {
            builder.append("package ").append(pkg).append(";\n\n");
        }
        JavaTools.sortReduceAndPrintImports(builder, BASE_EXTERNALIZABLE_IMPORTS).append('\n');
        builder.append("public class ").append(className).append("\n");
        builder.append("{\n");
        builder.append("    private static volatile ExecutionSupport EXECUTION_SUPPORT = null;\n");
        builder.append("    private static boolean disableConsole = true;\n");
        builder.append("    private static ExecutionActivityListener EXECUTION_ACTIVITY_LISTENER;\n");
        builder.append("\n");
        builder.append("    private ").append(className).append("()\n");
        builder.append("    {\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    public static void shutDown()\n");
        builder.append("    {\n");
        builder.append("        ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.coreinstance.M3CoreInstanceFactoryRegistry\", \"REGISTRY\");\n");
        builder.append("        ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.coreinstance.PlatformCoreInstanceFactoryRegistry\", \"REGISTRY\");\n");
        builder.append("        ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.coreinstance.DiagramCoreInstanceFactoryRegistry\", \"REGISTRY\");\n");
        builder.append("        ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.coreinstance.MappingCoreInstanceFactoryRegistry\", \"REGISTRY\");\n");
        builder.append("        ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.coreinstance.RelationalCoreInstanceFactoryRegistry\", \"REGISTRY\");\n");
        builder.append("        ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.coreinstance.GovernanceCoreInstanceFactoryRegistry\", \"REGISTRY\");\n");
        builder.append("        ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.coreinstance.LakeCoreInstanceFactoryRegistry\", \"REGISTRY\");\n");
        builder.append("        ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.coreinstance.HBaseCoreInstanceFactoryRegistry\", \"REGISTRY\");\n");
        builder.append("        ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.coreinstance.StateMachineCoreInstanceFactoryRegistry\", \"REGISTRY\");\n");
        builder.append("        ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.coreinstance.PathCoreInstanceFactoryRegistry\", \"REGISTRY\");\n");
        builder.append("        ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.statelistener.VoidExecutionActivityListener\",\"VOID_EXECUTION_ACTIVITY_LISTENER\");\n");
        builder.append("        ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.navigation.linearization.TypeTypeSupport\",\"INSTANCE\");\n");
        builder.append("        ReflectionTools.clearStatic(\"org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate\",\"instance\");\n");
        builder.append("        ").append(className).append(".EXECUTION_SUPPORT = null;\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    public static ExecutionSupport _getExecutionSupport()\n");
        builder.append("    {\n");
        builder.append("        ExecutionSupport result = EXECUTION_SUPPORT;\n");
        builder.append("        if (result == null)\n");
        builder.append("        {\n");
        builder.append("            synchronized (").append(className).append(".class)\n");
        builder.append("            {\n");
        builder.append("                if ((result = EXECUTION_SUPPORT) == null)\n");
        builder.append("                {\n");
        builder.append("                    ClassLoader classLoader = PureExternal.class.getClassLoader();\n");
        MutableList<String> sortedRepoNames = classLoaderRepos.toSortedList();
        if (sortedRepoNames.notEmpty())
        {
            sortedRepoNames.appendString(builder, "                    MutableSet<String> codeRepoNames = Sets.mutable.with(\"", "\", \"", "\");\n");
            builder.append("                    MutableList<CodeRepository> codeRepos = Lists.mutable.ofInitialCapacity(").append(sortedRepoNames.size()).append(");\n");
            builder.append("                    for (CodeRepository codeRepo : CodeRepositoryProviderHelper.findCodeRepositories(classLoader))\n");
            builder.append("                    {\n");
            builder.append("                        if (codeRepoNames.remove(codeRepo.getName()))\n");
            builder.append("                        {\n");
            builder.append("                            codeRepos.add(codeRepo);\n");
            builder.append("                            if (codeRepoNames.isEmpty())\n");
            builder.append("                            {\n");
            builder.append("                                break;\n");
            builder.append("                            }\n");
            builder.append("                        }\n");
            builder.append("                    }\n");
            builder.append("                    if (codeRepoNames.notEmpty())\n");
            builder.append("                    {\n");
            builder.append("                        System.out.println(codeRepoNames.makeString(\"WARNING! The following repositories have not been found on the classpath: '\", \"', '\", \"'\"));\n");
            builder.append("                    }\n");
        }
        else
        {
            builder.append("                    ImmutableList<CodeRepository> codeRepos = Lists.immutable.empty();\n");
        }
        builder.append("                    CompositeCodeStorage codeStorage = new CompositeCodeStorage(new ClassLoaderCodeStorage(classLoader, codeRepos));\n");
        builder.append("                    ConsoleCompiled console = new ConsoleCompiled();\n");
        builder.append("                    if (disableConsole)\n");
        builder.append("                    {\n");
        builder.append("                        console.disable();\n");
        builder.append("                    }\n");
        builder.append("                    EXECUTION_SUPPORT = result = new CompiledExecutionSupport(new JavaCompilerState(null, classLoader), new CompiledProcessorSupport(classLoader, MetadataLazy.fromClassLoader(classLoader)), null, codeStorage, null, EXECUTION_ACTIVITY_LISTENER, console, null, Sets.mutable.<String>of(), CompiledExtensionLoader.extensions(classLoader));\n");
        builder.append("                }\n");
        builder.append("            }\n");
        builder.append("        }\n");
        builder.append("        return result;\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    public static void setExecutionActivityListener(ExecutionActivityListener executionActivityListener)\n");
        builder.append("    {\n");
        builder.append("        if (EXECUTION_SUPPORT != null)\n");
        builder.append("        {\n");
        builder.append("            throw new RuntimeException(\"'setExecutionActivityListener' should be called before any other method.\");\n");
        builder.append("        }\n");
        builder.append("        EXECUTION_ACTIVITY_LISTENER = executionActivityListener;\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    public static void enableConsole()\n");
        builder.append("    {\n");
        builder.append("         disableConsole = false;\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    public static void disableConsole()\n");
        builder.append("    {\n");
        builder.append("         disableConsole = true;\n");
        builder.append("    }\n");
        functionDefinitions.appendString(builder, "\n", "\n\n", "\n");
        builder.append("}\n");
        return builder.toString();
    }
}
