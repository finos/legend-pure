// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m3.pct.functions.generation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m3.pct.functions.model.FunctionDefinition;
import org.finos.legend.pure.m3.pct.functions.model.Functions;
import org.finos.legend.pure.m3.pct.functions.model.Signature;
import org.finos.legend.pure.m3.pct.shared.PCTTools;
import org.finos.legend.pure.m3.pct.shared.generation.Shared;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.GraphLoader;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.binary.PureRepositoryJarLibrary;
import org.finos.legend.pure.m3.serialization.runtime.binary.SimplePureRepositoryJarLibrary;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.lang.reflect.Field;

public class FunctionsGeneration
{
    public static void main(String[] args) throws Exception
    {
        generateFunctions(args[0], args[1]);
    }

    public static void generateFunctions(String targetDir, String scopeProviderMethod)
    {
        Functions functions = generateFunctions(scopeProviderMethod);
        try
        {
            String reportStr = JsonMapper.builder().build().setSerializationInclusion(JsonInclude.Include.NON_NULL).writerWithDefaultPrettyPrinter().writeValueAsString(functions);
            Shared.writeStringToTarget(targetDir, "FUNCTIONS_" + functions.reportScope.module + ".json", reportStr);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static Functions generateFunctions(String scopeProviderMethod)
    {
        try
        {
            int lastDot = scopeProviderMethod.lastIndexOf('.');
            String className = scopeProviderMethod.substring(0, lastDot);
            String attributeName = scopeProviderMethod.substring(lastDot + 1);
            Class<?> _class = Thread.currentThread().getContextClassLoader().loadClass(className);
            Field field = _class.getField(attributeName);
            ReportScope reportScope = (ReportScope) field.get(null);

            return generateFunctions(reportScope);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static Functions generateFunctions(ReportScope reportScope)
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        CodeRepositorySet.Builder builder = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(classLoader, true));
        RichIterable<CodeRepository> codeRepositories = builder.build().getRepositories();
        CompositeCodeStorage codeStorage = new CompositeCodeStorage(new ClassLoaderCodeStorage(codeRepositories));
        PureRuntime runtime = new PureRuntimeBuilder(codeStorage).build();
        Message message = new Message("");

        PureRepositoryJarLibrary jarLibrary = SimplePureRepositoryJarLibrary.newLibrary(GraphLoader.findJars(Lists.mutable.withAll(codeRepositories.select(c -> c.getName() != null && (c.getName().startsWith("platform") || c.getName().startsWith("core"))).collect(CodeRepository::getName)), Thread.currentThread().getContextClassLoader(), message));
        GraphLoader loader = new GraphLoader(runtime.getModelRepository(), runtime.getContext(), runtime.getIncrementalCompiler().getParserLibrary(), runtime.getIncrementalCompiler().getDslLibrary(), runtime.getSourceRegistry(), runtime.getURLPatternLibrary(), jarLibrary);
        loader.loadAll(message);

        MutableMap<String, FunctionDefinition> functionsInfo = Maps.mutable.empty();
        getPCTFunctions(_Package.getByUserPath(reportScope._package, runtime.getProcessorSupport()), reportScope.filePath, runtime.getProcessorSupport()).forEach(x ->
        {
            addFunctionToFunctionsDB(x, functionsInfo, runtime.getProcessorSupport());
        });

        Functions functions = new Functions();
        functions.reportScope = reportScope;
        functions.functionDefinitions = Lists.mutable.withAll(functionsInfo.values());

        return functions;
    }


    private static void addFunctionToFunctionsDB(PackageableFunction<?> _function, MutableMap<String, FunctionDefinition> functionsDB, ProcessorSupport ps)
    {
        FunctionDefinition functionInfo = functionsDB.getIfAbsentPut(_function.getSourceInformation().getSourceId(), () -> new FunctionDefinition(_function.getSourceInformation().getSourceId()));
        // Name
        if (functionInfo.name != null && !functionInfo.name.equals(_function._functionName()))
        {
            throw new RuntimeException("Error in file " + _function.getSourceInformation().getSourceId() + ". The file contains multiple PCT functions: " + _function._functionName() + " & " + functionInfo.name);
        }
        functionInfo.name = _function._functionName();
        // Package
        String funcPackage = PackageableElement.getUserPathForPackageableElement(_function._package());
        if (functionInfo._package != null && !functionInfo._package.equals(funcPackage))
        {
            throw new RuntimeException("Error in file " + _function.getSourceInformation().getSourceId() + ". The file contains multiple PCT functions in different packages: " + funcPackage + " & " + functionInfo._package);
        }
        functionInfo._package = funcPackage;
        // Signatures
        functionInfo.signatures.add(new Signature(PackageableElement.getUserPathForPackageableElement(_function), FunctionDescriptor.getFunctionDescriptor(_function, ps), PCTTools.isPlatformOnly(_function, ps), PCTTools.getDoc(_function, ps), PCTTools.getGrammarDoc(_function, ps), PCTTools.getGrammarCharacters(_function, ps)));
    }


    private static MutableList<PackageableFunction<?>> getPCTFunctions(CoreInstance pkg, String filePath, ProcessorSupport processorSupport)
    {
        MutableList<PackageableFunction<?>> result = Lists.mutable.empty();
        getPCTFunctionsRecurse(pkg, result, filePath, processorSupport);
        return result;
    }

    private static void getPCTFunctionsRecurse(CoreInstance pkg, MutableList<PackageableFunction<?>> func, String filePath, ProcessorSupport processorSupport)
    {
        for (CoreInstance child : Instance.getValueForMetaPropertyToManyResolved(pkg, M3Properties.children, processorSupport))
        {
            if (Instance.instanceOf(child, M3Paths.PackageableFunction, processorSupport))
            {
                if (PCTTools.isPCTFunction(child, processorSupport) && child.getSourceInformation().getSourceId().startsWith(filePath))
                {
                    func.add((PackageableFunction<?>) child);
                }
            }
            else if (Instance.instanceOf(child, M3Paths.Package, processorSupport))
            {
                getPCTFunctionsRecurse(child, func, filePath, processorSupport);
            }
        }
    }


}
