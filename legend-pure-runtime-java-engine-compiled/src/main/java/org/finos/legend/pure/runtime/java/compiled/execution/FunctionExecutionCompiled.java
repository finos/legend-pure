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

package org.finos.legend.pure.runtime.java.compiled.execution;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.sorted.mutable.TreeSortedMap;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.exception.PureExecutionStreamingException;
import org.finos.legend.pure.m3.execution.Console;
import org.finos.legend.pure.m3.execution.ExecutionPlatform;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.execution.OutputWriter;
import org.finos.legend.pure.m3.navigation.*;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.serialization.PureRuntimeEventHandler;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.grammar.CoreInstanceFactoriesRegistry;
import org.finos.legend.pure.m3.serialization.runtime.*;
import org.finos.legend.pure.m3.statelistener.ExecutionActivityListener;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.runtime.java.compiled.compiler.MemoryClassLoader;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompiler;
import org.finos.legend.pure.runtime.java.compiled.delta.CompilerEventHandlerMetadataProvider;
import org.finos.legend.pure.runtime.java.compiled.delta.MetadataEagerCompilerEventHandler;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.MetadataJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataEventObserver;
import org.finos.legend.pure.runtime.java.compiled.serialization.GraphSerializer;
import org.finos.legend.pure.runtime.java.compiled.serialization.PreCompiledPureGraphCache;
import org.finos.legend.pure.runtime.java.compiled.statelistener.JavaCompilerEventObserver;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

@ExecutionPlatform(name = "Java compiled")
public class FunctionExecutionCompiled implements FunctionExecution, PureRuntimeEventHandler
{
    private ModelRepository repository;
    private Context context;
    private SourceRegistry sourceRegistry;
    private final ExecutionActivityListener executionActivityListener;
    private final JavaCompilerEventObserver javaCompilerEventObserver;
    private MutableList<CompiledExtension> extensions;

    private MutableSet<String> extraSupportedTypes;

    private JavaCompilerEventHandler javaCompilerEventHandler;
    private CompilerEventHandlerMetadataProvider metadataCompilerEventHandler;

    private final ConsoleCompiled consoleCompiled = new ConsoleCompiled(new Predicate<Object>()
    {
        @Override
        public boolean accept(Object object)
        {
            return FunctionExecutionCompiled.this.isExcluded(object);
        }
    });

    private PureRuntime runtime;

    private final boolean includePureStackTrace;

    private Metadata providedMetadata = null;

    private FunctionExecutionCompiled(ExecutionActivityListener executionActivityListener,
                                      JavaCompilerEventObserver javaCompilerEventObserver,
                                      boolean includePureStackTrace,
                                      MutableList<CompiledExtension> extensions)
    {
        this.executionActivityListener = executionActivityListener;
        this.javaCompilerEventObserver = javaCompilerEventObserver;
        this.includePureStackTrace = includePureStackTrace;
        this.extensions = extensions;
    }

    @Override
    public void init(PureRuntime runtime, Message message)
    {
        this.runtime = runtime;
        this.repository = runtime.getModelRepository();
        this.context = runtime.getContext();
        this.sourceRegistry = runtime.getSourceRegistry();
        this.javaCompilerEventHandler = new JavaCompilerEventHandler(runtime, message, this.includePureStackTrace, this.javaCompilerEventObserver, this.extensions);
        this.metadataCompilerEventHandler = new MetadataEagerCompilerEventHandler(runtime.getModelRepository(), (MetadataEventObserver) this.javaCompilerEventObserver, message, runtime.getProcessorSupport());

        runtime.addEventHandler(this);
        runtime.getIncrementalCompiler().addCompilerEventHandler(this.javaCompilerEventHandler);
        runtime.getIncrementalCompiler().addCompilerEventHandler(this.metadataCompilerEventHandler);

        initializeFromRuntimeState();

        this.extraSupportedTypes = runtime.getIncrementalCompiler().getParserLibrary().getParsers().flatCollect(CoreInstanceFactoriesRegistry::getCoreInstanceFactoriesRegistry).flatCollect(CoreInstanceFactoryRegistry::allManagedTypes).toSet();

        CodeRepositorySet codeRepositorySet = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(Thread.currentThread().getContextClassLoader(), true)).build();
        this.extensions.forEach(c -> {if (!(codeRepositorySet.hasRepository(c.getRelatedRepository())))
            {
                throw new RuntimeException("The repository "+c.getRelatedRepository()+" related to the extension "+c.getClass().getSimpleName()+" can't be found");
            }
        });
    }

    @Override
    public boolean isFullyInitializedForExecution()
    {
        return this.runtime != null && this.runtime.isFullyInitialized();
    }

    @Override
    public Console getConsole()
    {
        return this.consoleCompiled;
    }

    public PureJavaCompiler getJavaCompiler()
    {
        return this.javaCompilerEventHandler.getJavaCompiler();
    }


    @Override
    public CoreInstance start(CoreInstance functionDefinition, ListIterable<? extends CoreInstance> arguments)
    {
        CompiledExecutionSupport executionSupport = new CompiledExecutionSupport(this.javaCompilerEventHandler.getJavaCompileState(),
                (CompiledProcessorSupport) this.getProcessorSupport(), this.sourceRegistry, this.runtime.getCodeStorage(),
                this.runtime.getIncrementalCompiler(), this.executionActivityListener, this.consoleCompiled,
                this.javaCompilerEventHandler.getFunctionCache(), this.javaCompilerEventHandler.getClassCache(), this.metadataCompilerEventHandler, this.extraSupportedTypes, this.extensions, this.runtime.getOptions());
        Exception exception = null;
        try
        {
            Object result = this.executeFunction(functionDefinition, arguments, executionSupport);
            return convertResult(result, this.javaCompilerEventHandler.getJavaCompiler().getClassLoader(), this.metadataCompilerEventHandler.getMetadata(), this.extraSupportedTypes);
        }
        catch (PureExecutionException ex)
        {
            exception = ex;
            throw ex;
        }
        finally
        {
            executionSupport.executionEnd(exception);
        }
    }

    @Override
    public void start(CoreInstance func, ListIterable<? extends CoreInstance> arguments, OutputStream outputStream, OutputWriter writer)
    {
        CompiledExecutionSupport executionSupport = new CompiledExecutionSupport(this.javaCompilerEventHandler.getJavaCompileState(),
                (CompiledProcessorSupport) this.getProcessorSupport(), this.sourceRegistry, this.runtime.getCodeStorage(),
                this.runtime.getIncrementalCompiler(), this.executionActivityListener, this.consoleCompiled,
                this.javaCompilerEventHandler.getFunctionCache(), this.javaCompilerEventHandler.getClassCache(), this.metadataCompilerEventHandler, this.extraSupportedTypes, this.extensions, this.runtime.getOptions());

        Exception exception = null;
        try
        {
            Object result = this.executeFunction(func, arguments, executionSupport);
            try
            {
                writer.write(result, outputStream);
            }
            catch (PureExecutionException ex)
            {
                exception = ex;
                throw new PureExecutionStreamingException(ex);
            }
            catch (IOException e)
            {
                exception = e;
                throw new RuntimeException("Failed to write results to OutputStream", e);
            }
        }
        finally
        {
            executionSupport.executionEnd(exception);
        }
    }

    private Object executeFunction(CoreInstance functionDefinition, ListIterable<? extends CoreInstance> arguments, CompiledExecutionSupport executionSupport)
    {
        ProcessorSupport processorSupport = new M3ProcessorSupport(this.context, this.repository);

        Object result;
        try
        {
            result = this.executeFunction(functionDefinition, arguments, executionSupport,
                    this.javaCompilerEventHandler.getJavaCompiler().getClassLoader(), processorSupport);
        }
        catch (PureException pe)
        {
            //Rethrow as is to keep the original error
            throw pe;
        }
        catch (Exception e)
        {
            StringBuilder builder = new StringBuilder("Error executing ");
            try
            {
                org.finos.legend.pure.m3.navigation.function.Function.print(builder, functionDefinition, processorSupport);
            }
            catch (Exception ignore)
            {
                builder = new StringBuilder("Error executing ");
                builder.append(functionDefinition);
            }
            builder.append(". ");
            if (e.getMessage() != null)
            {
                builder.append(e.getMessage());
            }
            throw new RuntimeException(builder.toString(), e);
        }
        if (result == null)
        {
            result = Lists.immutable.empty();
        }
        return result;
    }

    private static CoreInstance convertResult(Object result, ClassLoader classLoader, Metadata metadata, MutableSet<String> extraSupportedTypes)
    {
        final ProcessorSupport compiledProcessorSupport = new CompiledProcessorSupport(classLoader, metadata, extraSupportedTypes);
        if (result instanceof RichIterable<?>)
        {
            RichIterable<?> iterable = (RichIterable<?>) result;
            if (iterable.isEmpty())
            {
                return ValueSpecificationBootstrap.wrapValueSpecification((CoreInstance) null, true, compiledProcessorSupport);
            }
            //Optimized check to see if the list is size 1
            else if (CompiledSupport.isEmpty(CompiledSupport.tail(iterable)))
            {
                Object first = Iterate.getFirst(iterable);
                if (first instanceof CoreInstance)
                {
                    return ValueSpecificationBootstrap.wrapValueSpecification((CoreInstance) first, true, compiledProcessorSupport);
                }
                else
                {
                    return wrapOneOptimized(first, classLoader, metadata);
                }
            }
            else
            {
                RichIterable<CoreInstance> instances = LazyIterate.collect(iterable, new Function<Object, CoreInstance>()
                {
                    @Override
                    public CoreInstance valueOf(Object object)
                    {
                        if (object instanceof CoreInstance)
                        {
                            return (CoreInstance) object;
                        }
                        else if (object instanceof String)
                        {
                            return compiledProcessorSupport.newCoreInstance((String) object, M3Paths.String, null);
                        }
                        else if (object instanceof Boolean)
                        {
                            return compiledProcessorSupport.newCoreInstance(object.toString(), M3Paths.Boolean, null);
                        }
                        else if (object instanceof PureDate)
                        {
                            PureDate date = (PureDate) object;
                            String type = date.hasHour() ? M3Paths.DateTime : M3Paths.StrictDate;
                            return compiledProcessorSupport.newCoreInstance(object.toString(), type, null);
                        }
                        else if ((object instanceof Integer) || (object instanceof Long) || (object instanceof BigInteger))
                        {
                            return compiledProcessorSupport.newCoreInstance(object.toString(), M3Paths.Integer, null);
                        }
                        else if ((object instanceof Float) || (object instanceof Double) || (object instanceof BigDecimal))
                        {
                            return compiledProcessorSupport.newCoreInstance(object.toString(), M3Paths.Float, null);
                        }
                        else
                        {
                            throw new RuntimeException("Cannot convert to core instance: " + object);
                        }
                    }
                });
                return ValueSpecificationBootstrap.wrapValueSpecification(instances, true, compiledProcessorSupport);
            }
        }
        else if (result instanceof CoreInstance)
        {
            return ValueSpecificationBootstrap.wrapValueSpecification((CoreInstance) result, true, compiledProcessorSupport);
        }
        else
        {
            return wrapOneOptimized(result, classLoader, metadata);
        }
    }

    private static CoreInstance wrapOneOptimized(Object first, ClassLoader classLoader, Metadata metadata)
    {
        try
        {
            Class instanceValueClass = classLoader.loadClass(FullJavaPaths.InstanceValue_Impl);
            Object instanceValueObject = instanceValueClass.getConstructor(String.class).newInstance("ID");
            MutableList _values = (MutableList) instanceValueClass.getDeclaredField("_values").get(instanceValueObject);
            _values.add(first);

            CoreInstance type = getType(first, metadata);

            Class genericTypeClass = classLoader.loadClass(FullJavaPaths.GenericType_Impl);
            Object genericTypeObject = genericTypeClass.getConstructor(String.class).newInstance("ID");
            genericTypeClass.getDeclaredField("_rawType").set(genericTypeObject, type);

            Class valSpecClass = classLoader.loadClass(FullJavaPaths.ValueSpecification_Impl);
            valSpecClass.getDeclaredField("_genericType").set(instanceValueObject, genericTypeObject);

            return (CoreInstance) instanceValueObject;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static CoreInstance getType(Object object, Metadata metamodel)
    {
        if (object instanceof String)
        {
            return metamodel.getMetadata(MetadataJavaPaths.PrimitiveType, M3Paths.String);
        }
        if (object instanceof Boolean)
        {
            return metamodel.getMetadata(MetadataJavaPaths.PrimitiveType, M3Paths.Boolean);
        }
        if (object instanceof PureDate)
        {
            return metamodel.getMetadata(MetadataJavaPaths.PrimitiveType, M3Paths.Date);
        }
        if ((object instanceof Integer) || (object instanceof Long) || (object instanceof BigInteger))
        {
            return metamodel.getMetadata(MetadataJavaPaths.PrimitiveType, M3Paths.Integer);
        }
        if ((object instanceof Float) || (object instanceof Double) || (object instanceof BigDecimal))
        {
            return metamodel.getMetadata(MetadataJavaPaths.PrimitiveType, M3Paths.Float);
        }
        return metamodel.getMetadata(MetadataJavaPaths.Class, "Root::meta::pure::metamodel::type::Any");
    }


    private Object executeFunction(CoreInstance functionDefinition, ListIterable<? extends CoreInstance> coreInstances, CompiledExecutionSupport executionSupport, ClassLoader cl, ProcessorSupport processorSupport)
    {
        // Manage Parameters ----------------------------
        ListIterable<? extends CoreInstance> parameters = Instance.getValueForMetaPropertyToManyResolved(processorSupport.function_getFunctionType(functionDefinition), M3Properties.parameters, processorSupport);
        Class[] paramClasses = new Class[parameters.size()];
        Object[] params = new Object[parameters.size()];
        Metadata metamodel = this.metadataCompilerEventHandler.getMetadata();
        int i = 0;

        if (parameters.size() != coreInstances.size())
        {
            StringBuilder builder = new StringBuilder();
            org.finos.legend.pure.m3.navigation.function.Function.print(builder, functionDefinition, processorSupport);
            String message = "Error executing the function:" + builder + ". Mismatch between the number of function parameters (" + parameters.size() + ") and the number of supplied arguments (" + coreInstances.size() + ")";
            throw new PureExecutionException(message);
        }

        for (CoreInstance param : parameters)
        {
            Object val = GraphSerializer.valueSpecToJavaObject(coreInstances.get(i), this.context, this.getProcessorSupport(), metamodel);
            CoreInstance paramMult = Instance.getValueForMetaPropertyToOneResolved(param, M3Properties.multiplicity, processorSupport);
            if (Multiplicity.isToOne(paramMult, true))
            {
                String t = TypeProcessor.typeToJavaPrimitiveSingle(Instance.getValueForMetaPropertyToOneResolved(param, M3Properties.genericType, processorSupport), processorSupport);
                paramClasses[i] = CompiledSupport.convertFunctionTypeStringToClass(t, cl);
                if (val instanceof MutableList)
                {
                    MutableList valList = (MutableList) val;
                    if (valList.size() != 1)
                    {
                        throw new RuntimeException("Expected exactly one value, found " + valList.size());
                    }
                    val = valList.get(0);
                }
            }
            else if (Multiplicity.isToOne(paramMult, false))
            {
                String className = TypeProcessor.typeToJavaObjectSingle(Instance.getValueForMetaPropertyToOneResolved(param, M3Properties.genericType, processorSupport), false, processorSupport);
                paramClasses[i] = CompiledSupport.loadClass(className, cl);
                if (val instanceof MutableList)
                {
                    MutableList valList = (MutableList) val;
                    switch (valList.size())
                    {
                        case 0:
                        {
                            val = null;
                            break;
                        }
                        case 1:
                        {
                            val = valList.get(0);
                            break;
                        }
                        default:
                        {
                            throw new RuntimeException("Expected at most one value, found " + valList.size());
                        }
                    }
                }
            }
            else
            {
                paramClasses[i] = RichIterable.class;
                if (!(val instanceof MutableList))
                {
                    val = FastList.newListWith(val);
                }
            }
            params[i] = val;
            i++;
        }

        return CompiledSupport.executeFunction(functionDefinition, paramClasses, params, executionSupport);
        // -----------------------------------------------
    }

    @Override
    public void resetEventHandlers()
    {
        this.runtime.removeEventHandler(this);
        this.runtime.getIncrementalCompiler().removeCompilerEventHandler(this.javaCompilerEventHandler);
    }


    @Override
    public ProcessorSupport getProcessorSupport()
    {
        return new CompiledProcessorSupport(this.getJavaCompiler().getClassLoader(), this.providedMetadata == null ?
                this.metadataCompilerEventHandler.getMetadata() : this.providedMetadata, this.extraSupportedTypes);
    }

    @Override
    public PureRuntime getRuntime()
    {
        return this.runtime;
    }

    @Override
    public OutputWriter newOutputWriter()
    {
        return new OutputWriterCompiled();
    }

    @Override
    public void reset()
    {
        this.javaCompilerEventHandler.reset();
        this.metadataCompilerEventHandler.reset();
    }

    @Override
    public void initializedFromCache()
    {
        if (getRuntime().getCache() instanceof PreCompiledPureGraphCache)
        {
            PreCompiledPureGraphCache graphCache = (PreCompiledPureGraphCache) getRuntime().getCache();
            MemoryClassLoader classLoader = this.javaCompilerEventHandler.getJavaCompiler().getCoreClassLoader();
            graphCache.prepareClassLoader(classLoader);
            this.metadataCompilerEventHandler.buildFullMetadata();
        }
        else
        {
            initializeFromRuntimeState();
        }
    }

    private void initializeFromRuntimeState()
    {
        if (this.runtime.isInitialized())
        {
            // Group sources by repo and separate platform sources
            MutableListMultimap<String, Source> sourcesByRepo = this.sourceRegistry.getSources().groupBy((Source source) -> PureCodeStorage.getSourceRepoName(source.getId()), Multimaps.mutable.list.<String, Source>empty());

            // Generate and compile Java code
            this.javaCompilerEventHandler.generateAndCompileJavaCode(TreeSortedMap.newMap(new RepositoryComparator(this.runtime.getCodeStorage().getAllRepositories()), sourcesByRepo.toMap()));

            // Serialize the full graph
            this.metadataCompilerEventHandler.buildFullMetadata();

        }
    }

    //todo: refactor so that
    public void setProvidedMetadata(Metadata providedMetadata)
    {
        this.providedMetadata = providedMetadata;
    }

    boolean isExcluded(Object object)
    {
        return this.metadataCompilerEventHandler.getExcluded().contains(object);
    }

    static FunctionExecutionCompiled createFunctionExecutionCompiled(ExecutionActivityListener executionActivityListener, boolean includePureStackTrace, JavaCompilerEventObserver javaCompilerEventObserver)
    {
        return new FunctionExecutionCompiled(executionActivityListener, javaCompilerEventObserver, includePureStackTrace, CompiledExtensionLoader.extensions());
    }

    public int getClassCacheSize()
    {
        return 0;//this.metadataCompilerEventHandler.getInstanceBuilder().getClassCacheSize();
    }
}