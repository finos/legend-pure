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

package org.finos.legend.pure.runtime.java.interpreted.natives.essentials.tests;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.pct.shared.PCTManifestLoader;
import org.finos.legend.pure.m3.pct.shared.model.PCTManifest;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.MapCoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

/**
 * Native implementation of {@code meta::pure::test::pct::loadPCTManifest}.
 *
 * <p>Delegates JSON parsing and validation to the shared
 * {@link PCTManifestLoader}, then converts the resulting POJO into the
 * interpreted Pure class representation ({@code PCTManifest}).
 *
 * <p>Critically, this native <b>resolves</b> the adapter path and
 * exclusion test paths to actual Pure functions at load time, ensuring
 * the JSON manifest references valid Pure model elements.
 */
public class LoadPCTManifest extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;
    private final ModelRepository repository;

    public LoadPCTManifest(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.functionExecution = functionExecution;
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance pathValue = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);
        String manifestPath = pathValue.getName();

        PCTManifest manifest;
        try (InputStream is = this.functionExecution.getPureRuntime().getCodeStorage().getContent(manifestPath))
        {
            if (is == null)
            {
                throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), "PCT manifest file not found: " + manifestPath);
            }
            manifest = PCTManifestLoader.loadFromStream(is, manifestPath);
        }
        catch (IOException e)
        {
            throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), "IO error while processing PCT manifest: " + manifestPath, e);
        }

        CoreInstance adapterFunction = processorSupport.package_getByUserPath(manifest.adapter);
        if (adapterFunction == null)
        {
            throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), "PCT manifest adapter function not found: " + manifest.adapter);
        }

        return toPureManifest(manifest, adapterFunction, functionExpressionCallStack, processorSupport);
    }

    private CoreInstance toPureManifest(PCTManifest manifest, CoreInstance adapterFunction, MutableStack<CoreInstance> functionExpressionCallStack, ProcessorSupport processorSupport)
    {
        CoreInstance mapClass = processorSupport.package_getByUserPath("meta::pure::functions::collection::Map");
        // Build Map instance using interpreted MapCoreInstance
        MapCoreInstance mapInstance = new MapCoreInstance(Lists.immutable.empty(), "Anonymous_NoProfile", functionExpressionCallStack.peek().getSourceInformation(), mapClass, -1, this.repository, false, processorSupport);

        // Populate MapCoreInstance internal map directly mapping Function -> String
        MutableMap<String, String> exclusionMap = manifest.toExclusionMap();
        MutableMap<CoreInstance, CoreInstance> internalMap = mapInstance.getMap();
        for (String testFqn : exclusionMap.keysView())
        {
            CoreInstance testFunction = processorSupport.package_getByUserPath(testFqn);
            if (testFunction == null)
            {
                throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), "PCT manifest test function not found: " + testFqn);
            }
            internalMap.put(testFunction, this.repository.newStringCoreInstance(exclusionMap.get(testFqn)));
        }

        // Build PCTManifest instance — adapter is now a Function, not a String
        CoreInstance manifestClass = processorSupport.package_getByUserPath("meta::pure::test::pct::PCTManifest");
        CoreInstance pureManifest = this.repository.newEphemeralAnonymousCoreInstance(functionExpressionCallStack.peek().getSourceInformation(), manifestClass);

        Instance.setValueForProperty(pureManifest, "adapter", adapterFunction, processorSupport);
        Instance.setValueForProperty(pureManifest, "exclusions", mapInstance, processorSupport);

        return ValueSpecificationBootstrap.wrapValueSpecification(pureManifest, true, processorSupport);
    }
}
