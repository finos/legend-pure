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
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.SourceMutation;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.navigation.imports.Imports;
import org.finos.legend.pure.m3.compiler.unload.Unbinder;
import org.finos.legend.pure.m3.compiler.unload.unbind.UnbindState;
import org.finos.legend.pure.m3.compiler.unload.walk.WalkerState;
import org.finos.legend.pure.m3.compiler.validation.ValidationType;
import org.finos.legend.pure.m3.compiler.validation.Validator;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.grammar.top.TopParser;
import org.finos.legend.pure.m3.serialization.runtime.pattern.URLPatternLibrary;
import org.finos.legend.pure.m3.statelistener.VoidM3M4StateListener;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m3.tools.forkjoin.ForkJoinTools;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.transaction.framework.ThreadLocalTransactionContext;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

public class IncrementalCompiler_New extends IncrementalCompiler
{
    private static final Function<CoreInstance, String> GET_CORE_INSTANCE_REPO = new Function<CoreInstance, String>()
    {
        @Override
        public String valueOf(CoreInstance object)
        {
            SourceInformation sourceInformation = object.getSourceInformation();
            if (sourceInformation == null)
            {
                throw new RuntimeException("Instance " + object.getName() + " of type " + object.getClassifier().getName() + " has no source information. This needs to be fixed");
            }
            String repo = PureCodeStorage.getSourceRepoName(sourceInformation.getSourceId());

            if(repo == null)
            {
                return null;
            }

            return repo.startsWith("model") ? "model-all" : repo;

        }
    };

    private static final Function<SourceState, String> GTE_SOURCE_STATE_REPO = new Function<SourceState, String>()
    {
        @Override
        public String valueOf(SourceState object)
        {
            String repo = PureCodeStorage.getSourceRepoName(object.getSource().getId());

            if(repo == null)
            {
                return null;
            }

            return repo.startsWith("model") ? "model-all" : repo;
        }
    };

    private static final Predicate2<CoreInstance, String> CORE_INSTANCE_IS_FROM_REPO = new Predicate2<CoreInstance, String>()
    {
        @Override
        public boolean accept(CoreInstance instance, String repoName)
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
        }
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
                    this.rollBack(repoTransaction, e, sources, repoTransactionInstances);
                }
                throw e;
            }
        }
        else
        {
            result = new SourceMutation();

            Multimap<String, ? extends Source> sourcesByRepo = sources.groupBy(s -> PureCodeStorage.getSourceRepoName(s.getId()));
            Multimap<String, ? extends Source> sourcesByRepoNew = sources.groupBy(PureCodeStorage.GET_SOURCE_REPO);
            Multimap<String, SourceState> sourceStatesByRepo  = this.oldSourceStates.groupBy(GTE_SOURCE_STATE_REPO);
            Multimap<String, CoreInstance> potentialRepos = potentialToProcess.groupBy(GET_COREINSTANCE_REPO_NAME);

            MutableSet<String> allReposToCompile = Sets.mutable.withAll(sourcesByRepo.keysView()).withAll(potentialRepos.keysView());

            ListIterable<String> repoCompileOrder = allReposToCompile.toSortedList(new RepositoryComparator(this.codeStorage.getAllRepositories()));
            MutableList<String> newCompileOrder = FastList.newList();
            for (String repo : repoCompileOrder)
            {
                if (repo == null)
                {
                    newCompileOrder.add(null);
                }
                else
                {
                    if(repo.startsWith("model"))
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
                        this.rollBack(repoTransaction, e, repoSources, repoTransactionInstances);
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

    private SourceMutation compileRepoSources(final IncrementalCompilerTransaction transaction, String repoName, final int repoNum, final int repoTotalCount, RichIterable<? extends Source> sources, RichIterable<CoreInstance> instancesToProcess, RichIterable<CoreInstance> instancesToUnbind, final RichIterable<SourceState> sourceStates, MutableSet<CoreInstance> repoTransactionInstances) throws PureCompilationException, PureParserException
    {
        final String repoDisplayName = repoName == null ? "non-repository" : repoName;
        try (ThreadLocalTransactionContext ignored = transaction != null ? transaction.openInCurrentThread() : null)
        {
            final AtomicInteger sourceNum = this.message == null ? null : new AtomicInteger(0);
            final int sourceTotalCount = sources.size();
            Procedure<Source> parseSource = new Procedure<Source>()
            {
                @Override
                public void value(Source source)
                {
                    try (ThreadLocalTransactionContext ignored = transaction != null ? transaction.openInCurrentThread() : null)
                    {
                        if (IncrementalCompiler_New.this.message != null)
                        {
                            int thisSourceNum = sourceNum.incrementAndGet();
                            StringBuilder message = new StringBuilder("Parsing ");
                            message.append(repoDisplayName);
                            if (repoTotalCount > 1)
                            {
                                message.append(" (");
                                message.append(repoNum);
                                message.append('/');
                                message.append(repoTotalCount);
                                message.append(')');
                            }
                            message.append(" sources (");
                            message.append(thisSourceNum);
                            message.append('/');
                            message.append(sourceTotalCount);
                            message.append(')');
                            IncrementalCompiler_New.this.message.setMessage(message.toString());
                        }
                        MutableList<SourceState> oldState = sourceStates.selectWith(SourceState.IS_SOURCE_STATE_FOR_SOURCE, source).toList();
                        ListMultimap<Parser, CoreInstance> newInstancesByParser = new TopParser().parse(source.getContent(), source.getId(), IncrementalCompiler_New.this.modelRepository, IncrementalCompiler_New.this.library, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, IncrementalCompiler_New.this.context, oldState.size() == 1 ? oldState.get(0) : null);
                        IncrementalCompiler_New.this.updateSource(source, newInstancesByParser);
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



            final MutableSet<CoreInstance> newInstances = sources.flatCollect(Source.SOURCE_NEW_INSTANCES).toSet(); // New Instances in the sources from the repo
            MutableSet<CoreInstance> oldInstances = sourceStates.flatCollect(SourceState.SOURCE_STATE_INSTANCES).toSet(); // Old Instances in the sources from repo

            MutableSet<CoreInstance> newButNotOld = newInstances.difference(oldInstances); // Instances which are newly created (added or modified)
            MutableSet<CoreInstance> oldButNotNew = oldInstances.difference(newInstances); // Instances which are not retained (deleted or modified)


            final MutableSet<String> sourcesInScope = Sets.mutable.empty(); // Source Ids in the repo within scope
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
                for (CompilerEventHandler compilerEventHandler : this.compilerEventHandlers)
                {
                    compilerEventHandler.invalidate(hereUnbind);
                }
            }

            // ToProcessGenerated - If the instances is within the scope of resources, it should be in the new instances - This can contain instances from following repos as well
            MutableSet<CoreInstance> toProcessGenerated = toUnbindGenerated.select(new Predicate<CoreInstance>()
            {
                @Override
                public boolean accept(CoreInstance each)
                {
                    return !sourcesInScope.contains(each.getSourceInformation().getSourceId()) || newInstances.contains(each);
                }
            });

            // Filter the instances which are within this repo
            MutableSet<CoreInstance> toProcessWithinRepoGenerated = toProcessGenerated.selectWith(CORE_INSTANCE_IS_FROM_REPO, repoName);

            // ToProcess from call is filtered for the existence in new instances if source is within the sourcesInScope
            MutableSet<CoreInstance> instancesToProcessFiltered = instancesToProcess.select(new Predicate<CoreInstance>()
            {
                @Override
                public boolean accept(CoreInstance each)
                {
                    return !sourcesInScope.contains(each.getSourceInformation().getSourceId()) || newInstances.contains(each);
                }
            }).toSet();

            // ToUnbind filtered for the existence in new instances if source is within the sourcesInScope
            MutableSet<CoreInstance> instancesToUnbindFiltered = instancesToUnbind.select(new Predicate<CoreInstance>()
            {
                @Override
                public boolean accept(CoreInstance each)
                {
                    return !sourcesInScope.contains(each.getSourceInformation().getSourceId()) || newInstances.contains(each);
                }
            }).toSet();

            // Final instances to be processed is (generated within repo + Filtered call parameters (toProcess, toUnbind))
            MutableList<CoreInstance> newInstancesConsolidated = this.removeNodesFromRemovedSources(toProcessWithinRepoGenerated.union(instancesToProcessFiltered).union(instancesToUnbindFiltered)).toSet().difference(newButNotOld).toList();

            // Maintain ordering to avoid errors in unit tests
            for(Source source : sources)
            {
                for (CoreInstance instance : source.getNewInstances())
                {
                    if (newButNotOld.contains(instance))
                    {
                        newInstancesConsolidated.add(instance);
                    }
                }
            }

            // Collect all instances to be registered (Even retained instances are unregistered at start)
            MutableList<CoreInstance> allInstances = this.removeNodesFromRemovedSources(toProcessWithinRepoGenerated.union(instancesToProcessFiltered).union(instancesToUnbindFiltered)).toSet().difference(newInstances).toList();
            for(Source source : sources)
            {
                allInstances.addAllIterable(source.getNewInstances());
            }

            // Store the to be processed set for rollback
            repoTransactionInstances.addAllIterable(newInstancesConsolidated);

            // Do postprocessing, validation - can throw an error
            SourceMutation result =  this.finishRepoCompilation(repoDisplayName, allInstances, newInstancesConsolidated, ValidationType.SHALLOW);

            // Repo compilation Successful

            //Remove any redundant packages of old instances
            for(CoreInstance oldInstance: oldInstances)
            {
                this.tryRemovePackage(oldInstance);
            }

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
        Procedure<CoreInstance> registerInContext = new Procedure<CoreInstance>()
        {
            @Override
            public void value(CoreInstance instance)
            {
                if (instance instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function)
                {
                    IncrementalCompiler_New.this.context.registerFunctionByName(instance);
                }
                IncrementalCompiler_New.this.context.registerInstanceByClassifier(instance);
                IncrementalCompiler_New.this.context.update(instance);
            }
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

        newInstancesConsolidated.removeIf(SourceMutation.IS_MARKED_FOR_DELETION);

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
        this.sourcesToBeRemoved.collect(Source.SOURCE_ID, excludedSourceIds);
        return toProcess.reject(new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance instance)
            {
                return instance.getSourceInformation() == null || excludedSourceIds.contains(instance.getSourceInformation().getSourceId());
            }
        });
    }

    //------------------
    //  UnLoadFromGraph
    //------------------
    @Override
    void unload()
    {
        if(!this.toUnload.isEmpty())
        {
            final MutableSet<String> removedSources = Sets.mutable.empty();
            this.sourcesToBeRemoved.collect(Source.SOURCE_ID, removedSources);
            MutableSet<CoreInstance> instancesInRemovedSources = this.toUnload.select(new Predicate<CoreInstance>()
            {
                @Override
                public boolean accept(CoreInstance instance)
                {
                    return instance.getSourceInformation() == null || removedSources.contains(instance.getSourceInformation().getSourceId());
                }
            }).toSet();

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

            SetIterable<String> sourcesIds = this.toUnload.collect(new Function<CoreInstance, String>()
            {
                @Override
                public String valueOf(CoreInstance object)
                {
                    return object.getSourceInformation().getSourceId();
                }
            });

            for (String sourceId : sourcesIds)
            {
                // Clean up import groups
                this.cleanUpImportGroups(sourceId);
            }

            this.toUnload.removeAllIterable(instancesInRemovedSources);
        }
    }

    private MutableSet<CoreInstance> walkTheGraphForUnload(MutableSet<CoreInstance> instances)
    {
        WalkerState walkerState = new WalkerState(this.processorSupport);
        Matcher walkerMatcher = new Matcher(this.modelRepository, this.context, this.processorSupport);

        for (MatchRunner walker : LazyIterate.concatenate(this.library.getParsers().asLazy().flatCollect(Parser.GET_UNLOAD_WALKERS), this.dslLibrary.getInlineDSLs().asLazy().flatCollect(InlineDSL.GET_UNLOAD_WALKERS)))
        {
            walkerMatcher.addMatchIfTypeIsKnown(walker);
        }

        for (CoreInstance instance : instances)
        {
            walkerMatcher.match(instance, walkerState);
        }
        return walkerState.getInstances().toSet();
    }

    private void rollBack(IncrementalCompilerTransaction transaction, Throwable t, Iterable<? extends Source> sources, MutableSet<CoreInstance> repoTransactionInstances) throws PureCompilationException
    {
        try
        {
            transaction.rollback();

            for (CoreInstance inst : this.toProcess.union(repoTransactionInstances))
            {
                this.context.update(inst);
            }
        }
        catch (Throwable t2)
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
        Package imports = (Package)this.processorSupport.package_getByUserPath("system::imports");
        return imports._children().selectWith(Imports.IS_IMPORT_GROUP_FOR_SOURCE, sourceId).toSet();
    }

    private void removeInstanceButNotPackage(CoreInstance instance)
    {
        Package pkg = instance instanceof PackageableElement ? ((PackageableElement)instance)._package() : null;
        if (pkg != null)
        {
            pkg._childrenRemove((PackageableElement)instance);
        }
        this.toProcess.remove(instance);
        this.context.remove(instance);
    }

    private void tryRemovePackage(CoreInstance instance)
    {
        Package pkg = instance instanceof PackageableElement ? ((PackageableElement)instance)._package() : null;
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