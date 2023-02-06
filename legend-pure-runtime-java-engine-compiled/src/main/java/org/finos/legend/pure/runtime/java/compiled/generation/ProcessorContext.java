// Copyright 2020 Goldman Sachs
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
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.finos.legend.pure.m3.generator.bootstrap.M3CoreInstanceGenerator;
import org.finos.legend.pure.m3.bootstrap.generator.M3ToJavaGenerator;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.NativeFunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;

public class ProcessorContext
{
    private final MutableMap<String, MutableMap<String, String>> lambdaFunctionsByIdBySource = Maps.mutable.empty();
    private final MutableList<StringJavaSource> classes = Lists.mutable.empty();
    private final MutableMap<Class<?>, MutableSet<CoreInstance>> processedClasses = Maps.mutable.empty();
    private final MutableMap<Class<?>, MutableSet<CoreInstance>> processedMeasures = Maps.mutable.empty();
    private final MutableMap<Class<?>, MutableSet<CoreInstance>> processedUnits = Maps.mutable.empty();
    private final MutableListMultimap<String, String> functionDefinitionsBySource = Multimaps.mutable.list.empty();
    private final MutableMap<String, MutableMap<String, String>> nativeLambdaFunctionsByNameBySource = Maps.mutable.empty();
    private final MutableIntObjectMap<CoreInstance> localLambdas = IntObjectMaps.mutable.empty();
    private final ProcessorSupport support;
    private final NativeFunctionProcessor nativeFunctionProcessor;
    private boolean inLineAllLambda = false;
    private final boolean includePureStackTrace;
    private String classImplSuffix;
    private final M3ToJavaGenerator generator;
    private final IdBuilder idBuilder;

    private int id = 0;
    private final MutableMap<String, Object> objects = Maps.mutable.empty();

    public ProcessorContext(ProcessorSupport support, Iterable<? extends CompiledExtension> compiledExtensions, IdBuilder idBuilder, boolean includePureStackTrace)
    {
        this.support = support;
        this.nativeFunctionProcessor = NativeFunctionProcessor.newWithCompiledExtensions(compiledExtensions);
        this.includePureStackTrace = includePureStackTrace;
        this.generator = M3CoreInstanceGenerator.generator(null, null, null);
        this.idBuilder = (idBuilder == null) ? IdBuilder.newIdBuilder(this.support) : idBuilder;
    }

    @Deprecated
    public ProcessorContext(ProcessorSupport support, Iterable<? extends CompiledExtension> compiledExtensions, boolean includePureStackTrace)
    {
        this(support, compiledExtensions, null, includePureStackTrace);
    }

    public ProcessorContext(ProcessorSupport support, boolean includePureStackTrace)
    {
        this(support, CompiledExtensionLoader.extensions(), null, includePureStackTrace);
    }

    public ProcessorContext(ProcessorSupport support)
    {
        this(support, false);
    }

    public M3ToJavaGenerator getGenerator()
    {
        return this.generator;
    }

    public ProcessorSupport getSupport()
    {
        return this.support;
    }

    public IdBuilder getIdBuilder()
    {
        return this.idBuilder;
    }

    public NativeFunctionProcessor getNativeFunctionProcessor()
    {
        return this.nativeFunctionProcessor;
    }

    public MutableList<StringJavaSource> getClasses()
    {
        return this.classes;
    }

    public MutableSet<CoreInstance> getProcessedClasses(Class<?> processorClass)
    {
        return this.processedClasses.getIfAbsentPut(processorClass, Sets.mutable::empty);
    }

    public MutableSet<CoreInstance> getProcessedMeasures(Class<?> processorClass)
    {
        return this.processedMeasures.getIfAbsentPut(processorClass, Sets.mutable::empty);
    }

    public MutableSet<CoreInstance> getProcessedUnits(Class<?> processorClass)
    {
        return this.processedUnits.getIfAbsentPut(processorClass, Sets.mutable::empty);
    }

    public void registerFunctionDefinition(CoreInstance function, String javaCode)
    {
        String sourceId = IdBuilder.sourceToId(function.getSourceInformation());
        this.functionDefinitionsBySource.put(sourceId, javaCode);
    }

    public RichIterable<String> getSourcesWithFunctionDefinitions()
    {
        return this.functionDefinitionsBySource.keysView();
    }

    public ListIterable<String> getFunctionDefinitionsForSource(String sourceId)
    {
        return this.functionDefinitionsBySource.get(sourceId);
    }

    public void registerLambdaFunction(CoreInstance lambda, String javaCode)
    {
        registerLambdaFunction(IdBuilder.sourceToId(lambda.getSourceInformation()), this.idBuilder.buildId(lambda), javaCode);
    }

    public void registerLambdaFunction(String sourceId, String lambdaId, String javaCode)
    {
        String oldJavaCode = this.lambdaFunctionsByIdBySource.getIfAbsentPut(sourceId, Maps.mutable::empty).put(lambdaId, javaCode);
        if ((oldJavaCode != null) && !oldJavaCode.equals(javaCode))
        {
            throw new RuntimeException("Lambda " + lambdaId + " defined more than once with different Java code in " + sourceId + ".\n\nCODE 1:\n" + oldJavaCode + "\n\n\n==================\nCODE 2:\n" + javaCode);
        }
    }

    public RichIterable<String> getSourcesWithLambdaFunctions()
    {
        return this.lambdaFunctionsByIdBySource.keysView();
    }

    public MapIterable<String, String> getLambdaFunctionsForSource(String sourceId)
    {
        MutableMap<String, String> result = this.lambdaFunctionsByIdBySource.get(sourceId);
        return (result == null) ? null : result.asUnmodifiable();
    }

    public void registerNativeLambdaFunction(CoreInstance nativeFunction, Native nat)
    {
        this.nativeLambdaFunctionsByNameBySource.getIfAbsentPut(IdBuilder.sourceToId(nativeFunction.getSourceInformation()), Maps.mutable::empty).put(nativeFunction.getName(), nat.buildBody());
    }

    public RichIterable<String> getSourcesWithNativeLambdaFunctions()
    {
        return this.nativeLambdaFunctionsByNameBySource.keysView();
    }

    public MapIterable<String, String> getNativeLambdaFunctionsForSource(String sourceId)
    {
        MutableMap<String, String> result = this.nativeLambdaFunctionsByNameBySource.get(sourceId);
        return (result == null) ? null : result.asUnmodifiable();
    }

    public boolean isInLineAllLambda()
    {
        return this.inLineAllLambda;
    }

    public void setInLineAllLambda(boolean inLineAllLambda)
    {
        this.inLineAllLambda = inLineAllLambda;
    }

    public String addObjectToPassToDynamicallyGeneratedCode(CoreInstance content)
    {
        String s_id = String.valueOf(this.id);
        this.objects.put(s_id, content);
        this.id++;
        return s_id;
    }

    public MutableMap<String, Object> getObjectToPassToDynamicallyGeneratedCode()
    {
        return this.objects;
    }

    public void registerLocalLambdas(int id, CoreInstance function)
    {
        this.localLambdas.put(id, function);
    }

    public IntObjectMap<CoreInstance> getLocalLambdas()
    {
        return this.localLambdas;
    }

    public boolean includePureStackTrace()
    {
        return this.includePureStackTrace;
    }

    public void setClassImplSuffix(String classImplSuffix)
    {
        this.classImplSuffix = classImplSuffix;
    }

    public String getClassImplSuffix()
    {
        return this.classImplSuffix;
    }
}
