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

package org.finos.legend.pure.m3.serialization.runtime;

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.partition.PartitionIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.SourceMutation;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.observer.PostProcessorObserver;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m3.navigation.function.InvalidFunctionDescriptorException;
import org.finos.legend.pure.m3.serialization.PureRuntimeEventHandler;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNode;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNodeStatus;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageTools;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.UpdateReport;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.runtime.IncrementalCompiler.IncrementalCompilerTransaction;
import org.finos.legend.pure.m3.serialization.runtime.cache.PureGraphCache;
import org.finos.legend.pure.m3.serialization.runtime.pattern.URLPatternLibrary;
import org.finos.legend.pure.m3.statelistener.VoidM3M4StateListener;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.serialization.binary.BinaryRepositorySerializer;
import org.finos.legend.pure.m4.serialization.grammar.M4Parser;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.m4.transaction.framework.ThreadLocalTransactionContext;

import java.io.File;
import java.util.concurrent.ForkJoinPool;

public class PureRuntime
{
    private static final String M3_PURE = "/platform/pure/grammar/m3.pure";

    private final URLPatternLibrary patternLibrary = new URLPatternLibrary();

    private boolean forceImmutable = false;

    private final PureRuntimeStatus pureRuntimeStatus;

    private final SourceRegistry sourceRegistry;
    private final PureGraphCache cache;
    private final IncrementalCompiler incrementalCompiler;

    private final Object initializationLock = new Object();
    private boolean initialized = false;
    private boolean initializationError = true;
    private boolean initializing = false;
    private final RuntimeOptions options;

    private final MutableList<PureRuntimeEventHandler> eventHandlers = Lists.mutable.empty();

    public final ExecutedTestTracker executedTestTracker;

    PureRuntime(MutableCodeStorage codeStorage, PureGraphCache cache,
                PureRuntimeStatus pureRuntimeStatus, Message message, CoreInstanceFactoryRegistry factoryRegistryOverride, ForkJoinPool incrementalCompilerForkJoinPool,
                boolean isTransactionByDefault, boolean useFastCompiler, ExecutedTestTracker executedTestTracker, RuntimeOptions options)
    {
        this.pureRuntimeStatus = pureRuntimeStatus;
        this.cache = cache;
        this.cache.setPureRuntime(this);

        ParserService loader = new ParserService();
        ListIterable<Parser> parsers = loader.parsers();
        ListIterable<InlineDSL> inlineDSLs = loader.inlineDSLs();

        this.incrementalCompiler =
                useFastCompiler ?
                    new IncrementalCompiler_New(parsers, inlineDSLs, codeStorage, this.patternLibrary, message, factoryRegistryOverride, incrementalCompilerForkJoinPool, isTransactionByDefault) :
                    new IncrementalCompiler_Old(parsers, inlineDSLs, codeStorage, this.patternLibrary, message, factoryRegistryOverride, incrementalCompilerForkJoinPool, isTransactionByDefault);

        this.sourceRegistry = new SourceRegistry(codeStorage, this.incrementalCompiler.getParserLibrary(), Lists.fixedSize.<SourceEventHandler>of(this.incrementalCompiler));

        this.executedTestTracker = executedTestTracker;

        this.options = options;
    }

    public void initialize(Message message)
    {
        synchronized (this.initializationLock)
        {
            this.initialized = false;
            this.initializing = true;
            try
            {
                this.initializationError = false;
                try
                {
                    message.setMessage("Initializing...");
                    this.loadAndCompileCore(message);
                    try
                    {
                        this.loadAndCompileSystem(message);
                        this.cache.cacheRepoAndSources();
                    }
                    finally
                    {
                        this.initialized = true;
                    }
                    message.setMessage("...");
                }
                catch (RuntimeException | Error e)
                {
                    this.initializationError = true;
                    throw e;
                }
            }
            finally
            {
                this.initializing = false;
            }
        }
    }

    public void reset()
    {
        this.initialized = false;
        this.initializing = false;
        this.initializationError = false;
        this.incrementalCompiler.reset();
        this.patternLibrary.clear();
        this.sourceRegistry.clear();
        // TODO consider whether to reload sources from storage
        for (PureRuntimeEventHandler eventHandler : this.eventHandlers)
        {
            eventHandler.reset();
        }
    }

    public URLPatternLibrary getURLPatternLibrary()
    {
        return this.patternLibrary;
    }

    public void addEventHandler(PureRuntimeEventHandler eventHandler)
    {
        this.eventHandlers.add(eventHandler);
    }

    public void removeEventHandler(PureRuntimeEventHandler eventHandler)
    {
        this.eventHandlers.remove(eventHandler);
    }

    public void setForceImmutable(boolean forceImmutable)
    {
        this.forceImmutable = forceImmutable;
    }

    public SourceRegistry getSourceRegistry()
    {
        return this.sourceRegistry;
    }

    public MutableCodeStorage getCodeStorage()
    {
        return this.sourceRegistry.getCodeStorage();
    }

    public ModelRepository getModelRepository()
    {
        return this.incrementalCompiler.getModelRepository();
    }

    public Context getContext()
    {
        return this.incrementalCompiler.getContext();
    }

    public ProcessorSupport getProcessorSupport()
    {
        return this.incrementalCompiler.getProcessorSupport();
    }

    //--------
    //  Load
    //--------
    public RichIterable<Source> loadAndCompileCore()
    {
        RichIterable<Source> res = loadAndCompileCore(null);
        this.initialized = true;
        return res;
    }

    public RichIterable<Source> loadAndCompileCore(Message message)
    {
        return loadAndCompileCore(message, null);
    }

    public RichIterable<Source> loadAndCompileCore(Message message, PostProcessorObserver postProcessorObserver)
    {
        this.pureRuntimeStatus.startLoadingAndCompilingCore();

        try
        {
            RichIterable<CodeRepository> allPlatform = this.getCodeStorage().getAllRepositories().select(c -> c.getName().startsWith("platform"));
            MutableList<String> sourcePaths = allPlatform.flatCollect(c -> this.getCodeStorage().getFileOrFiles(c.getName())).toList();

            if (message != null)
            {
                message.setMessage("Loading "+sourcePaths.size()+" sources...");
            }
            MutableList<Source> sources = sourcePaths.collect(this::getOrLoadSource);

            ////START READ and Serialize m3.pure
            Source m3Source = getOrLoadSource(M3_PURE);
            ModelRepository m3Repository = new ModelRepository();
            ListIterable<CoreInstance> results = new M4Parser().parse(m3Source.getContent(), m3Source.getId(), m3Repository, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER);
            ListIterable<CoreInstance> newInstances = this.serializeAndDeserializeCoreInstances(message, m3Repository, results);
            MutableListMultimap<Parser, CoreInstance> newInstancesByParser = Multimaps.mutable.list.empty();
            newInstancesByParser.putAll(new M3AntlrParser(null), newInstances); // This wasn't really parsed by the M3 parser, but it's the most appropriate parser for this case
            this.getIncrementalCompiler().updateSource(m3Source, newInstancesByParser);
            this.getIncrementalCompiler().preCompileM3(newInstances);
            ////END READ and Serialize m3.pure

            this.compile(sources.reject(s -> M3_PURE.equals(s.getId())), postProcessorObserver);

            this.getIncrementalCompiler().finishedCompilingCore(sources);
            return sources;
        }
        finally
        {
            this.pureRuntimeStatus.finishedLoadingAndCompilingCore();
        }
    }

    private ListIterable<CoreInstance> serializeAndDeserializeCoreInstances(Message message, ModelRepository m3Repository, ListIterable<CoreInstance> results)
    {
        MutableIntObjectMap<String> classifierPaths = IntObjectMaps.mutable.empty();
        results.forEach(instance -> classifierPaths.put(instance.getSyntheticId(), PackageableElement.getUserPathForPackageableElement(instance)));
        IntObjectMap<CoreInstance> instancesById = BinaryRepositorySerializer.build(m3Repository.serialize(), this.getModelRepository(), Message.newMessageCallback(message), classifierPaths);
        return results.collect(object -> instancesById.get(object.getSyntheticId()));
    }

    public RichIterable<Source> loadAndCompileSystem()
    {
        return loadAndCompileSystem(null);
    }

    public RichIterable<Source> loadAndCompileSystem(Message message)
    {
        return loadAndCompileSystem(message, null);
    }

    public RichIterable<Source> loadAndCompileSystem(Message message, PostProcessorObserver postProcessorObserver)
    {
        this.pureRuntimeStatus.startLoadingAndCompilingSystemFiles();
        try
        {
            LazyIterable<String> sourcePaths = getCodeStorage().getUserFiles().asLazy();
            if (message != null)
            {
                message.setMessage("Loading "+sourcePaths.size()+" sources...");
            }
            MutableList<Source> sources = sourcePaths.collect(this::getOrLoadSource).reject(Source.IS_COMPILED).toList();
            compile(sources, postProcessorObserver);
            return sources;
        }
        finally
        {
            this.pureRuntimeStatus.finishedLoadingAndCompilingSystemFiles();
        }
    }

    public void loadAndCompile(String... paths)
    {
        this.loadAndCompile(ArrayAdapter.adapt(paths));
    }

    public void loadAndCompile(Iterable<String> paths)
    {
        this.pureRuntimeStatus.startLoadingAndCompilingSystemFiles();
        MutableList<Source> sources = Lists.mutable.with();
        MutableCodeStorage codeStorage = getCodeStorage();
        for (String path : paths)
        {
            codeStorage.getFileOrFiles(path).collect(this::loadSource, sources);
        }
        compile(sources);
        this.pureRuntimeStatus.finishedLoadingAndCompilingSystemFiles();
    }

    public SourceMutation createInMemoryAndCompile(Pair<String, String>... inMemory)
    {
        return this.createInMemoryAndCompile(ArrayAdapter.adapt(inMemory));
    }

    public SourceMutation createInMemoryAndCompile(MapIterable<String, String> inMemory)
    {
        return this.createInMemoryAndCompile(inMemory.keyValuesView());
    }

    public SourceMutation createInMemoryAndCompile(Iterable<? extends Pair<String, String>> inMemory)
    {
        MutableList<Source> sources = Lists.mutable.empty();
        for (Pair<String, String> idSourcePair : inMemory)
        {
            String id = idSourcePair.getOne();
            sources.add(this.createInMemorySource(id, idSourcePair.getTwo()));
        }
        return this.compile(sources);
    }

    /**
     * Compile all sources which have not been compiled.
     */
    public SourceMutation compile()
    {
        return compile(this.sourceRegistry.getSources().select(this::shouldCompile, Lists.mutable.empty()));
    }

    private boolean shouldCompile(Source source)
    {
        return !source.isCompiled() && (source.isInMemory() || CodeStorageTools.isPureFilePath(source.getId()));
    }

    private SourceMutation compile(RichIterable<? extends Source> sources)
    {
        return compile(sources, null);
    }

    private SourceMutation compile(RichIterable<? extends Source> sources, PostProcessorObserver postProcessorObserver)
    {
        SourceMutation sourceMutation = this.incrementalCompiler.compile(sources, postProcessorObserver);
        if (sourceMutation != null)
        {
            try
            {
                sourceMutation.perform(this);
            }
            catch (Exception e)
            {
                StringBuilder message = new StringBuilder("Error performing source mutation");
                if (e.getMessage() != null)
                {
                    message.append(": ").append(e.getMessage());
                }
                throw new PureCompilationException(message.toString(), e);
            }
        }
        return sourceMutation;
    }

    //------------
    //  Execution
    //------------
    public CoreInstance getCoreInstance(String path)
    {
        return this.getProcessorSupport().package_getByUserPath(path);
    }

    /**
     * Get a function by id or descriptor.  Returns null if
     * no function can be found.
     *
     * @param functionIdOrDescriptor function id or descriptor
     * @return function
     */
    public CoreInstance getFunction(String functionIdOrDescriptor)
    {
        // First try getting it as an id
        CoreInstance function = this.getProcessorSupport().package_getByUserPath(functionIdOrDescriptor);
        if (function == null)
        {
            try
            {
                function = FunctionDescriptor.getFunctionByDescriptor(functionIdOrDescriptor, this.getProcessorSupport());
            }
            catch (InvalidFunctionDescriptorException e)
            {
                // It is an invalid function descriptor, but it could still be a valid function id; so we don't throw an exception.
            }
        }
        return function;
    }

    public boolean compiles(String codeBlock)
    {
        boolean compiles = true;

        Source source = this.createInMemoryCodeBlock(codeBlock);
        IncrementalCompilerTransaction transaction = this.incrementalCompiler.newTransaction(false);
        try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
        {
            this.incrementalCompiler.compileInCurrentTransaction(source);
        }
        catch (PureException e)
        {
            compiles = false;
        }
        catch (RuntimeException e)
        {
            PureException pe = PureException.findPureException(e);
            if (pe == null)
            {
                throw e;
            }
            compiles = false;
        }
        finally
        {
            transaction.rollback();
        }

        return compiles;
    }

    //-----------------
    // Source management
    //-----------------

    public Source createInMemorySource(String id, String content)
    {
        Source source = this.createInMemorySourceWithoutRegistering(id, content);
        this.sourceRegistry.registerSource(source);
        return source;
    }

    public Source createInMemoryCodeBlock(String codeBlock)
    {
        String uniqueFunctionId = "codeBlock_" + Thread.currentThread().getId() + "_" + System.currentTimeMillis();
        String wrappedCodeBlock = "function " + uniqueFunctionId + "():Any[*]{" + codeBlock + "}";
        return this.createInMemorySourceWithoutRegistering(uniqueFunctionId, wrappedCodeBlock);
    }

    private Source createInMemorySourceWithoutRegistering(String id, String content)
    {
        this.pureRuntimeStatus.createOrUpdateMemorySource(id, content);
        return new Source(id, false, true, content);
    }

    public void create(String path)
    {
        this.getCodeStorage().createFile(path);
        if (CodeStorageTools.isPureFilePath(path))
        {
            loadSource(path);
        }
    }

    private boolean willModify(String path, String code)
    {
        Source source = this.sourceRegistry.getSource(path);
        return source != null && source.isModified(code);
    }

    public void modify(String path, String code)
    {
        if(this.executedTestTracker != null)
        {
            if (this.willModify(path, code))
            {
                this.executedTestTracker.invalidate();
            }
        }
        this.pureRuntimeStatus.modifySource(path, code);
        if (CodeStorageTools.hasPureFileExtension(path))
        {
            Source source = this.sourceRegistry.getSource(path);
            if (source == null)
            {
                throw new IllegalArgumentException("Unknown source: " + path);
            }
            source.updateContent(code);
        }
        else
        {
            MutableCodeStorage codeStorage = getCodeStorage();
            if (!codeStorage.exists(path))
            {
                throw new IllegalArgumentException("Unknown file: " + path);
            }
            codeStorage.writeContent(path, code);
        }
    }

    public void delete(String sourceId)
    {
        this.delete(sourceId, false);
    }

    public void delete(String sourceId, boolean logical)
    {
        this.pureRuntimeStatus.deleteSource(sourceId);
        Source source = this.sourceRegistry.getSource(sourceId);
        if (source == null)
        {
            if (!logical)
            {
                // TODO Should we throw an error for non-existent files and non-empty folders?
                MutableCodeStorage codeStorage = getCodeStorage();
                if (codeStorage.isFile(sourceId) || codeStorage.isEmptyFolder(sourceId))
                {
                    codeStorage.deleteFile(sourceId);
                }
            }
        }
        else
        {
            source.delete(logical);
            this.cache.deleteCache();
        }
        if (this.executedTestTracker != null)
        {
            this.executedTestTracker.invalidate();
        }
    }

    public void move(String sourceId, String destinationId)
    {
        if (this.getCodeStorage().exists(destinationId))
        {
            throw new IllegalArgumentException("DestinationId '" + destinationId + "' already exist");
        }
        this.pureRuntimeStatus.moveSource(sourceId, destinationId);
        if (this.getCodeStorage().exists(sourceId))
        {
            this.getCodeStorage().moveFile(sourceId, destinationId);
        }
        if (this.getSourceRegistry().getSource(sourceId) != null)
        {
            this.getSourceRegistry().getSource(sourceId).moveSource(destinationId);
        }
        this.cache.deleteCache();
        if (this.executedTestTracker != null)
        {
            this.executedTestTracker.invalidate();
        }
    }

    public void markAsResolved(String path)
    {
        Source source = this.sourceRegistry.getSource(path);
        if (!source.isImmutable())
        {
            this.incrementalCompiler.markForUnload(source);
            if (!source.isInMemory())
            {
                this.getCodeStorage().markAsResolved(path);
                this.cache.deleteCache();
            }
            if (this.executedTestTracker != null)
            {
                this.executedTestTracker.invalidate();
            }
        }
    }

    public UpdateReport update(String path, long version)
    {
        MutableCodeStorage codeStorage = getCodeStorage();
        UpdateReport report = codeStorage.update(path, version);
        if (!report.isEmpty())
        {
            try
            {
                for (String updated : LazyIterate.concatenate(report.getModified(), report.getAdded(), report.getReplaced()))
                {
                    loadOrRefreshPureFileContent(codeStorage, updated);
                }
                for (String deleted : report.getDeleted())
                {
                    delete(deleted, true);
                }
            }
            catch (RuntimeException e)
            {
                // An error occurred while updating state, so we probably have a corrupt state.  Best to reset.
                reset();
                throw e;
            }
            finally
            {
                this.cache.deleteCache();
                if (this.executedTestTracker != null)
                {
                    this.executedTestTracker.invalidate();
                }
            }
        }
        return report;
    }

    public void commit(ListIterable<String> paths, String message)
    {
        this.getCodeStorage().commit(paths, message);
        if (this.executedTestTracker != null)
        {
            this.executedTestTracker.invalidate();
        }
    }

    public void revert(String sourceId)
    {
        MutableCodeStorage codeStorage = getCodeStorage();
        for (String possiblyModified : codeStorage.revert(sourceId))
        {
            if (CodeStorageTools.hasPureFileExtension(possiblyModified))
            {
                Source source = this.sourceRegistry.getSource(sourceId);
                if (source != null)
                {
                    source.refreshContent();
                }
                else if (codeStorage.exists(possiblyModified))
                {
                    loadSourceFromStorage(possiblyModified);
                }
            }
        }
        this.cache.deleteCache();
        if (this.executedTestTracker != null)
        {
            this.executedTestTracker.invalidate();
        }
    }

    public RichIterable<String> applyPatch(String path, File patchFile)
    {
        MutableCodeStorage codeStorage = getCodeStorage();
        codeStorage.applyPatch(path, patchFile);
        RichIterable<CodeStorageNode> modifiedUserFiles = codeStorage.getModifiedUserFiles();
        RichIterable<String> rejectFiles = modifiedUserFiles.selectWith(CodeStorageNode.HAS_STATUS, CodeStorageNodeStatus.UNKNOWN).select(CodeStorageNode.IS_REJECT_FILE).collect(CodeStorageNode.GET_PATH);
        if(!modifiedUserFiles.isEmpty())
        {
            try
            {
                PartitionIterable<CodeStorageNode> partition = modifiedUserFiles.partitionWith(CodeStorageNode.HAS_STATUS, CodeStorageNodeStatus.DELETED);
                for (CodeStorageNode node : partition.getRejected())
                {
                    String modifiedFilePath = node.getPath();
                    loadOrRefreshPureFileContent(codeStorage, modifiedFilePath);
                }
                for (CodeStorageNode node : partition.getSelected())
                {
                    delete(node.getPath(), true);
                }
            }
            catch (RuntimeException e)
            {
                // An error occurred while updating state, so we probably have a corrupt state.  Best to reset.
                reset();
                throw e;
            }
            finally
            {
                this.cache.deleteCache();
                if (this.executedTestTracker != null)
                {
                    this.executedTestTracker.invalidate();
                }
            }
        }
        return rejectFiles;
    }

    private void loadOrRefreshPureFileContent(MutableCodeStorage codeStorage, String filePath)
    {
        if (CodeStorageTools.hasPureFileExtension(filePath) && codeStorage.isFile(filePath))
        {
            Source source = getSourceById(filePath);
            if (source == null)
            {
                source = loadSourceFromStorage(filePath);
                this.pureRuntimeStatus.modifySource(filePath, source.getContent());
            }
            else if (source.refreshContent())
            {
                this.pureRuntimeStatus.modifySource(filePath, source.getContent());
            }
        }
    }

    public boolean isSourceImmutable(String sourceId)
    {
        return this.sourceRegistry.getSource(sourceId).isImmutable();
    }

    public Source getSourceById(String sourceId)
    {
        return this.sourceRegistry.getSource(sourceId);
    }

    public boolean isInitialized()
    {
        synchronized (this.initializationLock)
        {
            return this.initialized;
        }
    }

    public boolean isFullyInitialized()
    {
        synchronized (this.initializationLock)
        {
            return this.initialized && this.getIncrementalCompiler().eventHandlersFullyInitialized();
        }
    }

    public boolean isInitializedNoLock()
    {
        return this.initialized;
    }

    public boolean isInitializationError()
    {
        return this.initializationError;
    }

    public boolean isInitializing()
    {
        return this.initializing;
    }

    public void initializeFromCache(Message message)
    {
        initializeFromCache(message, true);
    }

    public void initializeFromCache(Message message, boolean reconcileSourceRegistry)
    {
        this.pureRuntimeStatus.startRuntimeInitialization();

        synchronized (this.initializationLock)
        {
            this.initialized = false;
            this.initializing = true;
            try
            {
                try
                {
                    ModelRepository repository = getModelRepository();
                    Context context = getContext();
                    ProcessorSupport processorSupport = getProcessorSupport();
                    this.initialized = this.cache.buildRepoAndSources(repository, this.sourceRegistry, this.incrementalCompiler.getParserLibrary(), context, processorSupport, message);
                    if (this.initialized)
                    {
                        if (message != null)
                        {
                            message.setMessage("  Registering service patterns ...");
                        }
                        for (CoreInstance func : context.getClassifierInstances(getCoreInstance(M3Paths.ConcreteFunctionDefinition)))
                        {
                            try
                            {
                                this.patternLibrary.possiblyRegister(func, processorSupport);
                            }
                            catch (Exception e)
                            {
                                // Ignore
                            }
                        }
                        if (message != null)
                        {
                            message.setMessage("  Notifying event handlers ...");
                        }
                        for (PureRuntimeEventHandler eventHandler : this.eventHandlers)
                        {
                            eventHandler.initializedFromCache();
                        }

                        this.initializationError = false;
                        if (message != null)
                        {
                            message.setMessage("  -> Finished building the graph from PAR files");
                        }
                    }
                    else if (message != null)
                    {
                        message.setMessage("  -> Could not initialize the graph using PAR files");
                    }
                }
                catch (RuntimeException | Error e)
                {
                    this.initializationError = true;
                    this.initialized = false;
                    throw e;
                }

                if (reconcileSourceRegistry)
                {
                    try
                    {
                        if (message != null)
                        {
                            message.setMessage("Reconciling with source files ...");
                        }
                        boolean sourcesChanged = reconcileSourceRegistryWithCodeStorage();
                        if (sourcesChanged)
                        {
                            compile();
                        }
                    }
                    catch (PureCompilationException | PureParserException e)
                    {
                        //Expected exceptions - may have some local changes
                        throw e;
                    }
                    catch (RuntimeException | Error e)
                    {
                        this.initializationError = true;
                        this.initialized = false;
                        throw e;
                    }
                }
            }
            finally
            {
                this.initializing = false;
            }
        }

        this.pureRuntimeStatus.finishRuntimeInitialization();
    }

    public PureGraphCache getCache()
    {
        return this.cache;
    }

    private Source getOrLoadSource(String path)
    {
        Source source = getSourceById(path);
        return (source == null) ? loadSourceFromStorage(path) : source;
    }

    public void loadSourceIfLoadable(String path)
    {
        if ((getSourceById(path) == null) && CodeStorageTools.isPureFilePath(path))
        {
            loadSourceFromStorage(path);
        }
    }

    Source loadSource(String path)
    {
        if (getSourceById(path) != null)
        {
            throw new RuntimeException(path + " is already loaded");
        }
        return loadSourceFromStorage(path);
    }

    private Source loadSourceFromStorage(String path)
    {
        if (!CodeStorageTools.isPureFilePath(path))
        {
            throw new IllegalArgumentException("Not a " + CodeStorage.PURE_FILE_EXTENSION + " file: " + path);
        }
        MutableCodeStorage codeStorage = getCodeStorage();
        boolean immutable = this.forceImmutable || PlatformCodeRepository.NAME.equals(codeStorage.getRepoName(path)); // TODO do something smarter here
        if (Source.isInMemory(path))
        {
            throw new RuntimeException("'" + path + "' should not be in memory!");
        }
        Source source = new Source(path, immutable, false, codeStorage.getContentAsText(path));
        this.sourceRegistry.registerSource(source);
        return source;
    }

    private boolean reconcileSourceRegistryWithCodeStorage()
    {
        boolean changed = false;
        for (Source source : this.sourceRegistry.getSources())
        {
            if (source.refreshContent())
            {
                changed = true;
            }
        }

        MutableCodeStorage codeStorage = getCodeStorage();
        for (String path : codeStorage.getUserFiles())
        {
            if (!this.sourceRegistry.hasSource(path))
            {
                changed = true;
                loadSourceFromStorage(path);
            }
        }

        return changed;
    }

    public IncrementalCompiler getIncrementalCompiler()
    {
        return this.incrementalCompiler;
    }

    public RuntimeOptions getOptions()
    {
        return this.options;
    }
}
