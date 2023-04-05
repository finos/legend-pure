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
import org.eclipse.collections.impl.factory.Lists;

public class ExternalClassBuilder
{
    private static final String BASE_EXTERNALIZABLE_IMPORTS = Lists.mutable.with(
            "import org.eclipse.collections.api.RichIterable",
            "import org.eclipse.collections.api.map.MutableMap",
            "import org.eclipse.collections.impl.factory.Lists",
            "import org.eclipse.collections.impl.factory.Maps",
            "import org.finos.legend.pure.m4.tools.DefendedFunction",
            "import org.eclipse.collections.api.block.predicate.Predicate",
            "import org.finos.legend.pure.m3.execution.ExecutionSupport",
            "import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper",
            "import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage",
            "import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage",
            "import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.VersionControlledClassLoaderCodeStorage",
            "import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.svn.PureRepositoryRevisionCache",
            "import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository",
            "import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled",
            "import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport",
            "import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader",
            "import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState",
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.*;",
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.*",
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.*",
            "import org.finos.legend.pure.runtime.java.compiled.execution.*",
            "import org.finos.legend.pure.runtime.java.compiled.execution.sourceInformation.*",
            "import org.finos.legend.pure.runtime.java.compiled.*",
            "import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy",
            "import org.finos.legend.pure.runtime.java.compiled.metadata.ClassCache",
            "import org.finos.legend.pure.runtime.java.compiled.metadata.FunctionCache",
            "import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport",
            "import org.tmatesoft.svn.core.wc.SVNWCUtil").makeString("", ";\n", ";\n");

    public static String buildExternalizableFunctionClass(RichIterable<String> functionDefinitions, String externalFunctionClass, RichIterable<String> vcsRepos, RichIterable<String> classLoaderRepos)
    {
        StringBuilder builder = new StringBuilder(BASE_EXTERNALIZABLE_IMPORTS);
        builder.append('\n');
        builder.append("public class ").append(externalFunctionClass).append("\n");
        builder.append("{\n");
        builder.append("    private static volatile ExecutionSupport EXECUTION_SUPPORT = null;\n");
        builder.append("    private static boolean disableConsole = true;\n");
        builder.append("    private static org.finos.legend.pure.m3.statelistener.ExecutionActivityListener EXECUTION_ACTIVITY_LISTENER;\n");
        builder.append("    private static MutableMap<String, CodeRepository> repoByName = CodeRepositoryProviderHelper.findCodeRepositories().groupByUniqueKey(new DefendedFunction<CodeRepository, String>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public String valueOf(CodeRepository codeRepository)\n" +
                "            {\n" +
                "                return codeRepository.getName();\n" +
                "            }\n" +
                "        }, Maps.mutable.<String, CodeRepository>empty());\n");
        builder.append("\n");
        builder.append("    private ").append(externalFunctionClass).append("()\n");
        builder.append("    {\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    public static void shutDown()\n");
        builder.append("    {\n");
        builder.append("        org.finos.legend.pure.m3.tools.ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.coreinstance.M3CoreInstanceFactoryRegistry\", \"REGISTRY\");\n");
        builder.append("        org.finos.legend.pure.m3.tools.ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.coreinstance.PlatformCoreInstanceFactoryRegistry\", \"REGISTRY\");\n");
        builder.append("        org.finos.legend.pure.m3.tools.ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.coreinstance.DiagramCoreInstanceFactoryRegistry\", \"REGISTRY\");\n");
        builder.append("        org.finos.legend.pure.m3.tools.ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.coreinstance.MappingCoreInstanceFactoryRegistry\", \"REGISTRY\");\n");
        builder.append("        org.finos.legend.pure.m3.tools.ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.coreinstance.RelationalCoreInstanceFactoryRegistry\", \"REGISTRY\");\n");
        builder.append("        org.finos.legend.pure.m3.tools.ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.coreinstance.GovernanceCoreInstanceFactoryRegistry\", \"REGISTRY\");\n");
        builder.append("        org.finos.legend.pure.m3.tools.ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.coreinstance.LakeCoreInstanceFactoryRegistry\", \"REGISTRY\");\n");
        builder.append("        org.finos.legend.pure.m3.tools.ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.coreinstance.HBaseCoreInstanceFactoryRegistry\", \"REGISTRY\");\n");
        builder.append("        org.finos.legend.pure.m3.tools.ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.coreinstance.StateMachineCoreInstanceFactoryRegistry\", \"REGISTRY\");\n");
        builder.append("        org.finos.legend.pure.m3.tools.ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.coreinstance.PathCoreInstanceFactoryRegistry\", \"REGISTRY\");\n");
        builder.append("        org.finos.legend.pure.m3.tools.ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.statelistener.VoidExecutionActivityListener\",\"VOID_EXECUTION_ACTIVITY_LISTENER\");\n");
        builder.append("        org.finos.legend.pure.m3.tools.ReflectionTools.clearStatic(\"org.finos.legend.pure.m3.navigation.linearization.TypeTypeSupport\",\"INSTANCE\");\n");
        builder.append("        org.finos.legend.pure.m3.tools.ReflectionTools.clearStatic(\"org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate\",\"instance\");\n");
        builder.append("        ").append(externalFunctionClass).append(".EXECUTION_SUPPORT = null;\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    public static ExecutionSupport _getExecutionSupport()\n");
        builder.append("    {\n");
        builder.append("        ExecutionSupport result = EXECUTION_SUPPORT;\n");
        builder.append("        if (result == null)\n");
        builder.append("        {\n");
        builder.append("            synchronized (").append(externalFunctionClass).append(".class)\n");
        builder.append("            {\n");
        builder.append("                result = EXECUTION_SUPPORT;\n");
        builder.append("                if (result == null)\n");
        builder.append("                {\n");
        classLoaderRepos = Lists.mutable.withAll(classLoaderRepos).sortThis();
        vcsRepos = Lists.mutable.withAll(vcsRepos).sortThis();
        classLoaderRepos.appendString(builder, "                    ClassLoaderCodeStorage classLoaderCodeStorage = new ClassLoaderCodeStorage(Lists.immutable.with(\"", "\", \"", "\").collect(new DefendedFunction<String, CodeRepository>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public CodeRepository valueOf(String name)\n" +
                "            {\n" +
                "                 if (repoByName.get(name) == null)" +
                "                   {" +
                "                       System.out.println(\"WARNING! The repository '\" + name + \"' has not been found on the classpath\");"+
                "                   }" +
                "                return repoByName.get(name);\n" +
                "            }\n" +
                "        }).select(new Predicate<CodeRepository>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public boolean accept(CodeRepository codeRepository)\n" +
                "            {\n" +
                "                return codeRepository != null;\n" +
                "            }\n" +
                "            public boolean test(CodeRepository each) {\n" +
                "                return this.accept(each);\n" +
                "            }\n" +
                "        }));\n");
        if (vcsRepos.isEmpty())
        {
            builder.append("                    CompositeCodeStorage codeStorage = new CompositeCodeStorage(classLoaderCodeStorage);\n");
        }
        else
        {
            vcsRepos.appendString(builder, "                    VersionControlledClassLoaderCodeStorage vcsCodeStorage = new VersionControlledClassLoaderCodeStorage(Lists.immutable.with(\"", "\", \"", "\").collect(c -> repoByName.get(c)), new PureRepositoryRevisionCache(SVNWCUtil.createDefaultAuthenticationManager(\"puresvntest\", \"Pure#Test#Access\".toCharArray()), false));\n");
            builder.append("                    CompositeCodeStorage codeStorage = new CompositeCodeStorage(classLoaderCodeStorage, vcsCodeStorage);\n");
        }
        builder.append("                    ConsoleCompiled console = new ConsoleCompiled();\n");
        builder.append("                    if (disableConsole) { console.disable(); }\n");
        builder.append("                    EXECUTION_SUPPORT = result = new CompiledExecutionSupport(new JavaCompilerState(null, PureExternal.class.getClassLoader()), new CompiledProcessorSupport(PureExternal.class.getClassLoader(), MetadataLazy.fromClassLoader(PureExternal.class.getClassLoader()), org.eclipse.collections.api.factory.Sets.mutable.<String>of()), null, codeStorage, null, EXECUTION_ACTIVITY_LISTENER, console, new FunctionCache(), new ClassCache(), null, org.eclipse.collections.api.factory.Sets.mutable.<String>of(), CompiledExtensionLoader.extensions());\n");
        builder.append("                }\n");
        builder.append("            }\n");
        builder.append("        }\n");
        builder.append("        return result;\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    public static void enableConsole()\n");
        builder.append("    {\n");
        builder.append("         disableConsole = false;\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    public static void setExecutionActivityListener(org.finos.legend.pure.m3.statelistener.ExecutionActivityListener executionActivityListener)\n");
        builder.append("    {\n");
        builder.append("        if (EXECUTION_SUPPORT != null)\n");
        builder.append("        {\n");
        builder.append("            throw new RuntimeException(\"'setExecutionActivityListener' should be called before any other method.\");\n");
        builder.append("        }\n");
        builder.append("        EXECUTION_ACTIVITY_LISTENER = executionActivityListener;\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    public static void disableConsole()\n");
        builder.append("    {\n");
        builder.append("         disableConsole = true;\n");
        builder.append("    }\n");
        for (String functionDefinition : functionDefinitions)
        {
            builder.append('\n');
            builder.append(functionDefinition);
            builder.append('\n');
        }
        builder.append("}\n");
        return builder.toString();
    }
}
