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
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Multimaps;
import org.finos.legend.pure.m3.SourceMutation;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.unload.Unbinder;
import org.finos.legend.pure.m3.compiler.unload.unbind.UnbindState;
import org.finos.legend.pure.m3.compiler.unload.walk.WalkerState;
import org.finos.legend.pure.m3.compiler.validation.ValidationType;
import org.finos.legend.pure.m3.compiler.validation.Validator;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.navigation.imports.Imports;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.grammar.top.TopParser;
import org.finos.legend.pure.m3.serialization.runtime.pattern.URLPatternLibrary;
import org.finos.legend.pure.m3.statelistener.VoidM3M4StateListener;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m3.tools.forkjoin.ForkJoinTools;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.m4.transaction.framework.ThreadLocalTransactionContext;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

public class IncrementalCompiler_New extends IncrementalCompiler
{
    private static final Function<SourceState, String> GET_SOURCE_STATE_REPO = object ->
    {
        String repo = PureCodeStorage.getSourceRepoName(object.getSource().getId());
        if (repo == null)
        {
            return null;
        }
        return repo.startsWith("model") ? "model-all" : repo;
    };

    private static final Predicate2<CoreInstance, String> CORE_INSTANCE_IS_FROM_REPO = (instance, repoName) ->
    {
        String instanceRepository = PureCodeStorage.getSourceRepoName(instance.getSourceInformation().getSourceId());
        if ("Pure".equals(repoName))
        {
            return true;
        }
        if (instanceRepository == null)
        {
            return repoName == null;
        }
        return repoName != null && ("model-all".equals(repoName) ? instanceRepository.startsWith("model") : instanceRepository.equals(repoName));
    };

    private final MutableSet<SourceState> oldSourceStates = Sets.mutable.with();
    private final MutableSet<CoreInstance> toUnbind = Sets.mutable.with();
    private final MutableSet<CoreInstance> processed = Sets.mutable.with();

    IncrementalCompiler_New(RichIterable<? extends Parser> parsers, RichIterable<? extends InlineDSL> inlineDSLs, CodeStorage codeStorage, URLPatternLibrary urlPatternLibrary, Message message, CoreInstanceFactoryRegistry factoryRegistryOverride, ForkJoinPool forkJoinPool, boolean isTransactionalByDefault)
    {
        super(parsers, inlineDSLs, codeStorage, urlPatternLibrary, message, factoryRegistryOverride, forkJoinPool, isTransactionalByDefault);
    }

    //----------
    //  Compile
    //----------

    @Override
    SourceMutation compile(RichIterable<? extends Source> sources, Iterable<? extends CompilerEventHandler> compilerEventHandlers) throws PureCompilationException, PureParserException
    {
        MutableSet<CoreInstance> potentialToProcess = this.walkTheGraphForUnload(this.toUnload).withAll(this.toProcess).withAll(this.toUnbind);

        this.unload();
        this.toProcess = this.removeNodesFromRemovedSources(this.toProcess);

        IncrementalCompilerTransaction threadLocalTransaction = this.transactionManager.getThreadLocalTransaction();

        SourceMutation result;
        MutableListMultimap<String, Source> compiledSourcesByRepo = Multimaps.mutable.list.empty();
        if (sources.isEmpty())
        {
            // We must compile even if the set of sources is empty, as post-processing or validation may be required for nodes from already compiled sources.
            IncrementalCompilerTransaction repoTransaction = this.isTransactionalByDefault && threadLocalTransaction == null ? this.newTransaction(true) : threadLocalTransaction;
            MutableSet<CoreInstance> repoTransactionInstances = Sets.mutable.empty();
            try
            {
                result = this.compileRepoSources(repoTransaction, "Pure", 1, 1, sources, this.toProcess.toImmutable(), this.toUnbind.toImmutable(), Lists.mutable.<SourceState>with(), repoTransactionInstances);
                if (this.isTransactionalByDefault && threadLocalTransaction == null)
                {
                    repoTransaction.commit();
                }
            }
            catch (RuntimeException e)
            {
                if (this.isTransactionalByDefault && threadLocalTransaction == null)
                {
                    this.rollBack(repoTransaction, e, repoTransactionInstances);
                }
                throw e;
            }
        }
        else
        {
            result = new SourceMutation();

            Multimap<String, ? extends Source> sourcesByRepo = sources.groupBy(s -> PureCodeStorage.getSourceRepoName(s.getId()));
            Multimap<String, ? extends Source> sourcesByRepoNew = sources.groupBy(PureCodeStorage.GET_SOURCE_REPO);
            Multimap<String, SourceState> sourceStatesByRepo = this.oldSourceStates.groupBy(GET_SOURCE_STATE_REPO);
            Multimap<String, CoreInstance> potentialRepos = potentialToProcess.groupBy(GET_COREINSTANCE_REPO_NAME);

            MutableSet<String> allReposToCompile = Sets.mutable.withAll(sourcesByRepo.keysView()).withAll(potentialRepos.keysView());

            ListIterable<String> repoCompileOrder = allReposToCompile.toSortedList(new RepositoryComparator(this.codeStorage.getAllRepositories()));
            MutableList<String> newCompileOrder = Lists.mutable.empty();
            for (String repo : repoCompileOrder)
            {
                if (repo == null)
                {
                    newCompileOrder.add(null);
                }
                else
                {
                    if (repo.startsWith("model"))
                    {
                        if (!newCompileOrder.contains("model-all"))
                        {
                            newCompileOrder.add("model-all");
                        }
                    }
                    else
                    {
                        newCompileOrder.add(repo);
                    }
                }
            }
            int repoCount = newCompileOrder.size();

            int repoNum = 1;
            for (String repo : newCompileOrder)
            {
                IncrementalCompilerTransaction repoTransaction = this.isTransactionalByDefault && threadLocalTransaction == null ? this.newTransaction(true) : threadLocalTransaction;
                RichIterable<? extends Source> repoSources = sourcesByRepoNew.get(repo);
                RichIterable<CoreInstance> toProcessThisRepo = this.toProcess.selectWith(CORE_INSTANCE_IS_FROM_REPO, repo).toImmutable();
                RichIterable<CoreInstance> toUnbindThisRepo = this.toUnbind.selectWith(CORE_INSTANCE_IS_FROM_REPO, repo).toImmutable();
                MutableSet<CoreInstance> repoTransactionInstances = Sets.mutable.empty();

                SourceMutation repoResult;
                try
                {
                    repoResult = this.compileRepoSources(repoTransaction, repo, repoNum++, repoCount, repoSources, toProcessThisRepo, toUnbindThisRepo, sourceStatesByRepo.get(repo), repoTransactionInstances);
                    if (this.isTransactionalByDefault && threadLocalTransaction == null)
                    {
                        repoTransaction.commit();
                    }
                }
                catch (RuntimeException e)
                {
                    if (this.isTransactionalByDefault && threadLocalTransaction == null)
                    {
                        this.rollBack(repoTransaction, e, repoTransactionInstances);
                    }
                    throw e;
                }
                result.merge(repoResult);
                if (repo == null)
                {
                    compiledSourcesByRepo.putAll(repo, repoSources);
                }
                else if ("model-all".equals(repo))
                {
                    compiledSourcesByRepo.putAll(repoSources.groupBy((Source source) -> PureCodeStorage.getSourceRepoName(source.getId())));
                }
                else
                {
                    compiledSourcesByRepo.putAll(repo, repoSources);
                }
            }
        }

        this.runEventHandlers(compilerEventHandlers, this.processed, compiledSourcesByRepo);
        this.processed.clear();
        return result;
    }

    private SourceMutation compileRepoSources(IncrementalCompilerTransaction transaction, String repoName, int repoNum, int repoTotalCount, RichIterable<? extends Source> sources, RichIterable<CoreInstance> instancesToProcess, RichIterable<CoreInstance> instancesToUnbind, RichIterable<SourceState> sourceStates, MutableSet<CoreInstance> repoTransactionInstances) throws PureCompilationException, PureParserException
    {
        String repoDisplayName = repoName == null ? "non-repository" : repoName;
        try (ThreadLocalTransactionContext ignored = transaction != null ? transaction.openInCurrentThread() : null)
        {
            AtomicInteger sourceNum = this.message == null ? null : new AtomicInteger(0);
            int sourceTotalCount = sources.size();
            Procedure<Source> parseSource = source ->
            {
                try (ThreadLocalTransactionContext ignored1 = transaction != null ? transaction.openInCurrentThread() : null)
                {
                    if (this.message != null)
                    {
                        int thisSourceNum = sourceNum.incrementAndGet();
                        StringBuilder message = new StringBuilder("Parsing ").append(repoDisplayName);
                        if (repoTotalCount > 1)
                        {
                            message.append(" (").append(repoNum).append('/').append(repoTotalCount).append(')');
                        }
                        message.append(" sources (").append(thisSourceNum).append('/').append(sourceTotalCount).append(')');
                        this.message.setMessage(message.toString());
                    }
                    MutableList<SourceState> oldState = sourceStates.select(sourceState -> source.equals(sourceState.getSource()), Lists.mutable.empty());
                    ListMultimap<Parser, CoreInstance> newInstancesByParser = new TopParser().parse(source.getContent(), source.getId(), this.modelRepository, this.library, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context, oldState.size() == 1 ? oldState.get(0) : null);
                    this.updateSource(source, newInstancesByParser);
                    if (transaction != null)
                    {
                        transaction.noteSourceCompiled(source);
                    }
                }
                catch (RuntimeException e)
                {
                    if (PureException.canFindPureException(e))
                    {
                        throw e;
                    }
                    throw new PureParserException(new SourceInformation(source.getId(), -1, -1, -1, -1), "Error parsing " + source.getId(), e);
                }
            };
            if (this.shouldParallelize(sourceTotalCount, PARSE_SOURCES_THRESHOLD))
            {
                ForkJoinTools.forEach(this.forkJoinPool, ListHelper.wrapListIterable(sources), parseSource, PARSE_SOURCES_THRESHOLD);
            }
            else
            {
                sources.forEach(parseSource);
            }


            // Parsing for repo completed successfully


            MutableSet<CoreInstance> newInstances = sources.flatCollect(Source::getNewInstances, Sets.mutable.empty()); // New Instances in the sources from the repo
            MutableSet<CoreInstance> oldInstances = sourceStates.flatCollect(SourceState::getInstances, Sets.mutable.empty()); // Old Instances in the sources from repo

            MutableSet<CoreInstance> newButNotOld = newInstances.difference(oldInstances); // Instances which are newly created (added or modified)
            MutableSet<CoreInstance> oldButNotNew = oldInstances.difference(newInstances); // Instances which are not retained (deleted or modified)


            MutableSet<String> sourcesInScope = Sets.mutable.empty(); // Source Ids in the repo within scope
            sources.collect(Source.SOURCE_ID, sourcesInScope);


            // Unload Walk on only instances which are not retained and generate toUnbindGenerated (There can be instances from following repos as well)
            MutableSet<CoreInstance> toUnbindGenerated = this.walkTheGraphForUnload(oldButNotNew);

            // Filter the instances which are within the repo
            MutableSet<CoreInstance> toUnbindWithinRepo = toUnbindGenerated.selectWith(CORE_INSTANCE_IS_FROM_REPO, repoName);

            // Total Unbind set is ( generated here + obtained through call - non retained )
            MutableSet<CoreInstance> hereUnbind = toUnbindWithinRepo.union(oldButNotNew).union(instancesToUnbind.toSet());
            Unbinder.process(hereUnbind, this.modelRepository, this.library, this.dslLibrary, this.context, this.processorSupport, new UnbindState(this.context, this.urlPatternLibrary, this.processorSupport), this.message);

            // Invalidate the unbound instances
            if (hereUnbind.notEmpty())
            {
                this.compilerEventHandlers.forEach(eh -> eh.invalidate(hereUnbind));
            }

            // ToProcessGenerated - If the instances is within the scope of resources, it should be in the new instances - This can contain instances from following repos as well
            MutableSet<CoreInstance> toProcessGenerated = toUnbindGenerated.select(each -> !sourcesInScope.contains(each.getSourceInformation().getSourceId()) || newInstances.contains(each));

            // Filter the instances which are within this repo
            MutableSet<CoreInstance> toProcessWithinRepoGenerated = toProcessGenerated.selectWith(CORE_INSTANCE_IS_FROM_REPO, repoName);

            // ToProcess from call is filtered for the existence in new instances if source is within the sourcesInScope
            MutableSet<CoreInstance> instancesToProcessFiltered = instancesToProcess.select(each -> !sourcesInScope.contains(each.getSourceInformation().getSourceId()) || newInstances.contains(each)).toSet();

            // ToUnbind filtered for the existence in new instances if source is within the sourcesInScope
            MutableSet<CoreInstance> instancesToUnbindFiltered = instancesToUnbind.select(each -> !sourcesInScope.contains(each.getSourceInformation().getSourceId()) || newInstances.contains(each)).toSet();

            // Final instances to be processed is (generated within repo + Filtered call parameters (toProcess, toUnbind))
            MutableList<CoreInstance> newInstancesConsolidated = this.removeNodesFromRemovedSources(toProcessWithinRepoGenerated.union(instancesToProcessFiltered).union(instancesToUnbindFiltered)).toSet().difference(newButNotOld).toList();

            // Maintain ordering to avoid errors in unit tests
            sources.forEach(source -> source.getNewInstances().select(newButNotOld::contains, newInstancesConsolidated));

            // Collect all instances to be registered (Even retained instances are unregistered at start)
            MutableList<CoreInstance> allInstances = this.removeNodesFromRemovedSources(toProcessWithinRepoGenerated.union(instancesToProcessFiltered).union(instancesToUnbindFiltered)).toSet().difference(newInstances).toList();
            sources.forEach(source -> allInstances.addAllIterable(source.getNewInstances()));

            // Store the to be processed set for rollback
            repoTransactionInstances.addAllIterable(newInstancesConsolidated);

            // Do postprocessing, validation - can throw an error
            SourceMutation result = this.finishRepoCompilation(repoDisplayName, allInstances, newInstancesConsolidated, ValidationType.SHALLOW);

            // Repo compilation Successful

            //Remove any redundant packages of old instances
            oldInstances.forEach(this::tryRemovePackage);

            // Remove source states
            this.oldSourceStates.removeAllIterable(sourceStates);

            //Remove old instances from toUnload
            this.toUnload.removeAllIterable(oldInstances);

            // Update toProcess
            this.toProcess = this.toProcess.difference(oldButNotNew);
            this.toProcess.addAllIterable(this.removeNodesFromRemovedSources(toProcessGenerated.difference(toProcessWithinRepoGenerated).difference(oldButNotNew).difference(newInstancesConsolidated.toSet())));

            // Update toUnbind
            this.toUnbind.removeAllIterable(hereUnbind);
            this.toUnbind.addAllIterable(toUnbindGenerated.difference(toUnbindWithinRepo));

            // Update processed to be used by compiler event handlers
            this.processed.addAllIterable(toProcessWithinRepoGenerated.withAll(instancesToProcessFiltered).withAll(instancesToUnbindFiltered));

            return result;
        }
    }


    private SourceMutation finishRepoCompilation(String repoName, MutableList<CoreInstance> allInstances, MutableList<CoreInstance> newInstancesConsolidated, ValidationType validationType) throws PureCompilationException
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
        if (this.shouldParallelize(allInstances.size(), CONTEXT_REGISTRATION_THRESHOLD))
        {
            ForkJoinTools.forEach(this.forkJoinPool, allInstances, registerInContext, CONTEXT_REGISTRATION_THRESHOLD);
        }
        else
        {
            allInstances.forEach(registerInContext);
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


    private MutableSet<CoreInstance> removeNodesFromRemovedSources(MutableSet<CoreInstance> toProcess)
    {
        final MutableSet<String> excludedSourceIds = Sets.mutable.empty();
        this.sourcesToBeRemoved.collect(Source::getId, excludedSourceIds);
        return toProcess.reject(instance -> instance.getSourceInformation() == null || excludedSourceIds.contains(instance.getSourceInformation().getSourceId()));
    }

    //------------------
    //  UnLoadFromGraph
    //------------------
    @Override
    void unload()
    {
        if (this.toUnload.notEmpty())
        {
            MutableSet<String> removedSources = Sets.mutable.empty();
            this.sourcesToBeRemoved.collect(Source.SOURCE_ID, removedSources);
            MutableSet<CoreInstance> instancesInRemovedSources = this.toUnload.select(instance -> instance.getSourceInformation() == null || removedSources.contains(instance.getSourceInformation().getSourceId())).toSet();

            MutableSet<CoreInstance> toUnbindGeneratedFromRemovedSources = this.walkTheGraphForUnload(instancesInRemovedSources);
            toUnbindGeneratedFromRemovedSources.addAllIterable(instancesInRemovedSources);

            this.toUnbind.addAllIterable(toUnbindGeneratedFromRemovedSources);

            // Remove CIs in changed sources
            for (CoreInstance instance : instancesInRemovedSources)
            {
                this.removeInstance(instance);
            }

            for (CoreInstance instance : this.toUnload.difference(instancesInRemovedSources))
            {
                this.removeInstanceButNotPackage(instance);
            }

            this.toUnload.collect(object -> object.getSourceInformation().getSourceId()).forEach(this::cleanUpImportGroups);
            this.toUnload.removeAllIterable(instancesInRemovedSources);
        }
    }

    private MutableSet<CoreInstance> walkTheGraphForUnload(MutableSet<CoreInstance> instances)
    {
        WalkerState walkerState = new WalkerState(this.processorSupport);
        Matcher walkerMatcher = new Matcher(this.modelRepository, this.context, this.processorSupport);

        this.library.getParsers().asLazy().flatCollect(Parser::getUnLoadWalkers).concatenate(this.dslLibrary.getInlineDSLs().asLazy().flatCollect(InlineDSL::getUnLoadWalkers)).forEach(walkerMatcher::addMatchIfTypeIsKnown);
        instances.forEach(i -> walkerMatcher.match(i, walkerState));
        return walkerState.getInstances().toSet();
    }

    private void rollBack(IncrementalCompilerTransaction transaction, Throwable t, MutableSet<CoreInstance> repoTransactionInstances) throws PureCompilationException
    {
        try
        {
            transaction.rollback();
            this.toProcess.union(repoTransactionInstances).forEach(this.context::update);
        }
        catch (Throwable ignore)
        {
            // TODO Rollback failed, so runtime is corrupt. Should do something to reset the compiled state.
            throw new PureCompilationException("Compilation failed because of the embedded exception, and the attempt to roll back to the previous state also failed. Full recompilation is advisable.", t);
        }
    }


    @Override
    public void reset()
    {
        super.reset();
        this.processed.clear();
        this.oldSourceStates.clear();
    }

    private MutableSet<? extends PackageableElement> collectImportGroups(String sourceId)
    {
        Package imports = (Package) this.processorSupport.package_getByUserPath("system::imports");
        return imports._children().select(ig -> Imports.isImportGroupForSource(ig, sourceId), Sets.mutable.empty());
    }

    private void removeInstanceButNotPackage(CoreInstance instance)
    {
        Package pkg = instance instanceof PackageableElement ? ((PackageableElement) instance)._package() : null;
        if (pkg != null)
        {
            pkg._childrenRemove((PackageableElement) instance);
        }
        this.toProcess.remove(instance);
        this.context.remove(instance);
    }

    private void tryRemovePackage(CoreInstance instance)
    {
        Package pkg = instance instanceof PackageableElement ? ((PackageableElement) instance)._package() : null;
        if (pkg != null)
        {
            if (pkg._children().isEmpty() && pkg.getSourceInformation() == null)
            {
                this.removeInstance(pkg);
            }
        }
    }

    @Override
    public void updateSource(Source source, String oldContent)
    {
        if (source.isCompiled())
        {
            this.oldSourceStates.add(new SourceState(source, oldContent, source.getNewInstances().toSet(), this.collectImportGroups(source.getId())));
        }
        super.updateSource(source, oldContent);
    }
}