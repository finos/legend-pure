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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.map.sorted.mutable.TreeSortedMap;
import org.finos.legend.pure.m3.SourceMutation;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.validation.ValidationType;
import org.finos.legend.pure.m3.compiler.validation.Validator;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.factory.CompositeCoreInstanceFactory;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.Import;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.imports.Imports;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.CoreInstanceFactoriesRegistry;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m3.serialization.runtime.pattern.URLPatternLibrary;
import org.finos.legend.pure.m3.serialization.runtime.pattern.URLPatternLibrary.URLPatternLibraryTransaction;
import org.finos.legend.pure.m3.statelistener.VoidM3M4StateListener;
import org.finos.legend.pure.m3.tools.forkjoin.ForkJoinTools;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.m4.tools.ConcurrentHashSet;
import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;
import org.finos.legend.pure.m4.transaction.framework.MultiTransaction;
import org.finos.legend.pure.m4.transaction.framework.MultiTransactionManager;

import java.util.SortedMap;
import java.util.concurrent.ForkJoinPool;

public abstract class IncrementalCompiler implements SourceEventHandler
{
    static final int PARSE_SOURCES_THRESHOLD = 100;
    static final int CONTEXT_REGISTRATION_THRESHOLD = 100;

    static final Function<CoreInstance, String> GET_COREINSTANCE_REPO_NAME = object ->
    {
        SourceInformation sourceInformation = object.getSourceInformation();
        if (sourceInformation == null)
        {
            throw new RuntimeException("Instance " + object.getName() + " of type " + object.getClassifier().getName() + " has no source information. This needs to be fixed");
        }
        return PureCodeStorage.getSourceRepoName(sourceInformation.getSourceId());
    };

    final InlineDSLLibrary dslLibrary;
    final ParserLibrary library;
    final CodeStorage codeStorage;
    final ModelRepository modelRepository;
    final Context context = new Context();
    final M3ProcessorSupport processorSupport;
    final ForkJoinPool forkJoinPool;
    final boolean isTransactionalByDefault;


    final IncrementalCompilerTransactionManager transactionManager = new IncrementalCompilerTransactionManager();

    final MutableSet<Source> sourcesToBeRemoved = Sets.mutable.with();

    MutableSet<CoreInstance> toProcess = Sets.mutable.with();
    MutableSet<CoreInstance> toUnload = Sets.mutable.with();

    final MutableList<CompilerEventHandler> compilerEventHandlers = Lists.mutable.empty();
    final MutableList<MatchRunner> additionalValidators = Lists.mutable.empty();

    final Message message;
    final URLPatternLibrary urlPatternLibrary;

    IncrementalCompiler(RichIterable<? extends Parser> parsers, RichIterable<? extends InlineDSL> inlineDSLs, CodeStorage codeStorage, URLPatternLibrary urlPatternLibrary, Message message, CoreInstanceFactoryRegistry factoryRegistryOverride, ForkJoinPool forkJoinPool, boolean isTransactionalByDefault)
    {
        this.message = message;
        this.urlPatternLibrary = urlPatternLibrary;
        this.dslLibrary = new InlineDSLLibrary(inlineDSLs);

        RichIterable<? extends Parser> allParsers = Lists.mutable.<Parser>with(new M3AntlrParser(this.dslLibrary)).withAll(parsers);
        RichIterable<CoreInstanceFactoryRegistry> registries =
                factoryRegistryOverride != null ?
                        Lists.fixedSize.of(factoryRegistryOverride) :
                        allParsers.asLazy().flatCollect(CoreInstanceFactoriesRegistry::getCoreInstanceFactoriesRegistry).concatenate(inlineDSLs.flatCollect(CoreInstanceFactoriesRegistry::getCoreInstanceFactoriesRegistry));

        CoreInstanceFactoryRegistry registry = registries.injectInto(
                new CoreInstanceFactoryRegistry(IntObjectMaps.immutable.empty(), Maps.immutable.empty(), Maps.immutable.empty()),
                CoreInstanceFactoryRegistry::combine
        );

        this.modelRepository = new ModelRepository(new CompositeCoreInstanceFactory(registry));
        this.processorSupport = new M3ProcessorSupport(this.context, this.modelRepository);
        this.library = new ParserLibrary(allParsers);
        try
        {
            this.library.validate();
        }
        catch (ParserLibrary.InvalidParserLibraryException e)
        {
            throw new RuntimeException(e);
        }
        this.codeStorage = codeStorage;
        this.forkJoinPool = forkJoinPool;
        this.isTransactionalByDefault = isTransactionalByDefault;
    }

    public void addCompilerEventHandler(CompilerEventHandler compilerEventHandler)
    {
        this.compilerEventHandlers.add(compilerEventHandler);
    }

    public void removeCompilerEventHandler(CompilerEventHandler compilerEventHandler)
    {
        this.compilerEventHandlers.remove(compilerEventHandler);
    }

    public void addValidator(MatchRunner validator)
    {
        this.additionalValidators.add(validator);
    }

    public void removeValidator(MatchRunner validator)
    {
        this.additionalValidators.remove(validator);
    }


    //----------
    //  Compile
    //----------

    void preCompileM3(Iterable<? extends CoreInstance> toProcess)
    {
        this.toProcess = Sets.mutable.withAll(toProcess);
    }

    void finishedCompilingCore(RichIterable<? extends Source> compiledSources)
    {
        this.compilerEventHandlers.forEach(eh -> eh.finishedCompilingCore(compiledSources));
    }

    public SourceMutation compileInCurrentTransaction(Source... sources)
    {
        return compileInCurrentTransaction(ArrayAdapter.adapt(sources));
    }

    public SourceMutation compileInCurrentTransaction(RichIterable<? extends Source> sources)
    {
        IncrementalCompilerTransaction transaction = this.transactionManager.getThreadLocalTransaction();
        if (transaction == null)
        {
            throw new IllegalStateException("No current transaction");
        }
        // TODO should we use this.compilerEventHandlers?
        return this.compile(sources, Lists.immutable.empty());
    }

    SourceMutation compile(RichIterable<? extends Source> sources) throws PureCompilationException, PureParserException
    {
        return this.compile(sources, this.compilerEventHandlers);
    }

    abstract SourceMutation compile(RichIterable<? extends Source> sources, Iterable<? extends CompilerEventHandler> compilerEventHandlers) throws PureCompilationException, PureParserException;

    void runEventHandlers(Iterable<? extends CompilerEventHandler> compilerEventHandlers, SetIterable<CoreInstance> copyToProcess, Multimap<String, ? extends Source> compiledSourcesByRepo)
    {
        MutableList<CoreInstance> allUpdatedInstances = compiledSourcesByRepo.valuesView().flatCollect(Source::getNewInstances, Lists.mutable.empty()).withAll(copyToProcess);

        SortedMap<String, RichIterable<? extends Source>> sortedMap = new TreeSortedMap<>(new RepositoryComparator(this.codeStorage.getAllRepositories()), compiledSourcesByRepo.toMap());
        compilerEventHandlers.forEach(eh -> eh.compiled(sortedMap, allUpdatedInstances));
    }

    MutableSet<CoreInstance> removeNodesFromSourcesInScope(RichIterable<? extends Source> sources, MutableSet<CoreInstance> toProcess)
    {
        final MutableSet<String> excludedSourceIds = Sets.mutable.empty();
        this.sourcesToBeRemoved.collect(Source::getId, excludedSourceIds);
        sources.collect(Source::getId, excludedSourceIds);
        return toProcess.reject(instance -> (instance.getSourceInformation() == null) || excludedSourceIds.contains(instance.getSourceInformation().getSourceId()));
    }

    SourceMutation finishRepoCompilation(String repoName, MutableList<CoreInstance> newInstancesConsolidated, ValidationType validationType) throws PureCompilationException
    {
        Procedure<CoreInstance> registerInContext = instance ->
        {
            if (instance instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function)
            {
                this.context.registerFunctionByName(instance);
            }
            this.context.registerInstanceByClassifier(instance);
            this.context.update(instance);
        };
        if (shouldParallelize(newInstancesConsolidated.size(), CONTEXT_REGISTRATION_THRESHOLD))
        {
            ForkJoinTools.forEach(this.forkJoinPool, newInstancesConsolidated, registerInContext, CONTEXT_REGISTRATION_THRESHOLD);
        }
        else
        {
            newInstancesConsolidated.forEach(registerInContext);
        }

        SourceMutation sourceMutation = PostProcessor.process(newInstancesConsolidated, this.modelRepository, this.library, this.dslLibrary, this.codeStorage, this.context, this.processorSupport, this.urlPatternLibrary, this.message);

        if (validationType == ValidationType.DEEP)
        {
            this.modelRepository.validate(VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER);
        }

        newInstancesConsolidated.removeIf(SourceMutation::isMarkedForDeletion);

        this.message.setMessage("Validating " + repoName + "...");
        Validator.validateM3(newInstancesConsolidated, validationType, this.library, this.dslLibrary, this.additionalValidators.asUnmodifiable(), this.codeStorage, this.modelRepository, this.context, this.processorSupport);

        rebuildExclusionSet(this.modelRepository, this.processorSupport);

        this.toProcess.removeAll(newInstancesConsolidated);
        this.sourcesToBeRemoved.clear();
        this.message.clearMessage();

        return sourceMutation;
    }

    public static SetIterable<CoreInstance> rebuildExclusionSet(ModelRepository repository, ProcessorSupport processorSupport) throws PureCompilationException
    {
        MutableSet<CoreInstance> excludes = Sets.mutable.withAll(repository.getTopLevels());

        Package systemImports = (Package)processorSupport.package_getByUserPath("system::imports");
        for (PackageableElement impGrp : systemImports._children())
        {
            excludes.add(impGrp);
            if (impGrp instanceof ImportGroup)
            {
                for (Import _import : ((ImportGroup)impGrp)._imports())
                {
                    CoreInstance pkg = Imports.getImportPackage(_import, processorSupport);
                    if (pkg != null)
                    {
                        excludes.add(pkg);
                    }
                }
            }
        }
        repository.setExclusionSet(excludes);

        return excludes;
    }

    void rollBack(IncrementalCompilerTransaction transaction, Throwable t) throws PureCompilationException
    {
        try
        {
            transaction.rollback();
            this.toProcess.forEach(this.context::update);
        }
        catch (Throwable ignore)
        {
            // TODO Rollback failed, so runtime is corrupt. Should do something to reset the compiled state.
            throw new PureCompilationException("Compilation failed because of the embedded exception, and the attempt to roll back to the previous state also failed. Full recompilation is advisable.", t);
        }
    }

    boolean shouldParallelize(int size, int threshold)
    {
        return (this.forkJoinPool != null) && (size > threshold);
    }

    //------------------
    //  UnLoadFromGraph
    //------------------
    void markForUnload(Source source)
    {
        if (source.isCompiled())
        {
            this.toUnload.addAllIterable(source.getNewInstances());

            // Note source is not compiled
            source.setCompiled(false);
        }
    }

    abstract void unload();

    void removeInstance(CoreInstance instance)
    {
        Package pkg = instance instanceof PackageableElement ? ((PackageableElement)instance)._package() : null;
        if (pkg != null)
        {
            pkg._childrenRemove((PackageableElement)instance);
            if (pkg._children().isEmpty() && (pkg.getSourceInformation() == null))
            {
                removeInstance(pkg);
            }
        }
        this.toProcess.remove(instance);
        this.context.remove(instance);
    }

    void cleanUpImportGroups(String sourceId)
    {
        Package imports = (Package)this.processorSupport.package_getByUserPath("system::imports");
        MutableList<? extends PackageableElement> newImportGroups = imports._children().reject(ig -> Imports.isImportGroupForSource(ig, sourceId), Lists.mutable.empty());
        imports._children(newImportGroups);
    }

    public void updateSource(Source source, ListMultimap<Parser, CoreInstance> elementsByParser)
    {
        source.linkInstances(elementsByParser);
        source.setCompiled(true);
    }

    public ParserLibrary getParserLibrary()
    {
        return this.library;
    }

    public InlineDSLLibrary getDslLibrary()
    {
        return this.dslLibrary;
    }

    public ModelRepository getModelRepository()
    {
        return this.modelRepository;
    }

    public Context getContext()
    {
        return this.context;
    }

    public ProcessorSupport getProcessorSupport()
    {
        return this.processorSupport;
    }

    public void reset()
    {
        this.modelRepository.clear();
        this.context.clear();
        this.sourcesToBeRemoved.clear();
        this.toProcess.clear();
        this.message.clearMessage();
        this.toUnload.clear();
        this.transactionManager.clear();
    }

    MutableSet<Source> getSourcesToBeRemoved()
    {
        return this.sourcesToBeRemoved;
    }

    boolean eventHandlersFullyInitialized()
    {
        return this.compilerEventHandlers.allSatisfy(CompilerEventHandler::isInitialized);
    }

    @Override
    public void deleteSource(Source source)
    {
        if (source.isCompiled())
        {
            this.markForUnload(source);
            this.getSourcesToBeRemoved().add(source);
        }
    }

    @Override
    public void updateSource(Source source, String oldContent)
    {
        this.markForUnload(source);
    }

    @Override
    public void moveSource(Source source, Source destination)
    {
        if (source.isCompiled())
        {
            this.markForUnload(source);
            this.getSourcesToBeRemoved().add(source);
        }
    }

    public IncrementalCompilerTransaction newTransaction(boolean committable)
    {
        return this.transactionManager.newTransaction(committable);
    }

    class IncrementalCompilerTransactionManager extends MultiTransactionManager<IncrementalCompilerTransaction>
    {
        @Override
        protected IncrementalCompilerTransaction createTransaction(boolean committable)
        {
            ModelRepositoryTransaction modelRepositoryTransaction = null;
            URLPatternLibraryTransaction urlPatternLibraryTransaction = null;
            try
            {
                modelRepositoryTransaction = IncrementalCompiler.this.modelRepository.newTransaction(committable);
                urlPatternLibraryTransaction = IncrementalCompiler.this.urlPatternLibrary.newTransaction(committable);
                return new IncrementalCompilerTransaction(this, committable, modelRepositoryTransaction, urlPatternLibraryTransaction);
            }
            catch (RuntimeException e)
            {
                if (modelRepositoryTransaction != null)
                {
                    try
                    {
                        modelRepositoryTransaction.rollback();
                    }
                    catch (Exception ignore)
                    {
                        // ignore this exception
                    }
                }
                if (urlPatternLibraryTransaction != null)
                {
                    try
                    {
                        urlPatternLibraryTransaction.rollback();
                    }
                    catch (Exception ignore)
                    {
                        // ignore this exception
                    }
                }
                throw e;
            }
        }
    }

    public class IncrementalCompilerTransaction extends MultiTransaction
    {
        private final MutableSet<Source> sourcesCompiled = ConcurrentHashSet.newSet();
        private final ModelRepositoryTransaction modelRepositoryTransaction;

        private IncrementalCompilerTransaction(IncrementalCompilerTransactionManager manager, boolean committable, ModelRepositoryTransaction modelRepositoryTransaction, URLPatternLibraryTransaction urlPatternLibraryTransaction)
        {
            super(manager, committable, modelRepositoryTransaction, urlPatternLibraryTransaction);
            this.modelRepositoryTransaction = modelRepositoryTransaction;
        }

        @Override
        protected void doRollback()
        {
            super.doRollback();
            for (CoreInstance newInstance : this.modelRepositoryTransaction.getNewInstancesInTransaction())
            {
                IncrementalCompiler.this.context.remove(newInstance);
            }

            for (Source source : this.sourcesCompiled)
            {
                source.setCompiled(false);
            }
        }

        void noteSourceCompiled(Source source)
        {
            checkOpen();
            this.sourcesCompiled.add(source);
        }
    }
}
