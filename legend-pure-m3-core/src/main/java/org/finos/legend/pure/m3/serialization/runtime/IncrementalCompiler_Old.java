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
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.SourceMutation;
import org.finos.legend.pure.m3.compiler.unload.Unbinder;
import org.finos.legend.pure.m3.compiler.unload.unbind.UnbindState;
import org.finos.legend.pure.m3.compiler.unload.walk.WalkerState;
import org.finos.legend.pure.m3.compiler.validation.ValidationType;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.grammar.top.TopParser;
import org.finos.legend.pure.m3.serialization.runtime.pattern.URLPatternLibrary;
import org.finos.legend.pure.m3.statelistener.VoidM3M4StateListener;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m3.tools.forkjoin.ForkJoinTools;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.m4.transaction.framework.ThreadLocalTransactionContext;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

public class IncrementalCompiler_Old extends IncrementalCompiler
{
    IncrementalCompiler_Old(RichIterable<? extends Parser> parsers, RichIterable<? extends InlineDSL> inlineDSLs, CodeStorage codeStorage, URLPatternLibrary urlPatternLibrary, Message message, CoreInstanceFactoryRegistry factoryRegistryOverride, ForkJoinPool forkJoinPool, boolean isTransactionalByDefault)
    {
        super(parsers, inlineDSLs, codeStorage, urlPatternLibrary, message, factoryRegistryOverride, forkJoinPool, isTransactionalByDefault);
    }


    //----------
    //  Compile
    //----------

    @Override
    SourceMutation compile(RichIterable<? extends Source> sources, Iterable<? extends CompilerEventHandler> compilerEventHandlers) throws PureCompilationException, PureParserException
    {
        this.unload();

        this.toProcess = this.removeNodesFromSourcesInScope(sources, this.toProcess);
        MutableSet<CoreInstance> copyToProcess = Sets.mutable.withAll(this.toProcess);

        IncrementalCompilerTransaction threadLocalTransaction = this.transactionManager.getThreadLocalTransaction();

        SourceMutation result;
        MutableListMultimap<String, Source> compiledSourcesByRepo = Multimaps.mutable.list.empty();
        if (sources.isEmpty())
        {
            // We must compile even if the set of sources is empty, as post-processing or validation may be required for nodes from already compiled sources.
            IncrementalCompilerTransaction repoTransaction = this.isTransactionalByDefault && threadLocalTransaction == null ? this.newTransaction(true) : threadLocalTransaction;
            try
            {
                result = this.compileRepoSources(repoTransaction, "Pure", 1, 1, sources, this.toProcess);
                if (this.isTransactionalByDefault && threadLocalTransaction == null)
                {
                    repoTransaction.commit();
                }
            }
            catch (RuntimeException e)
            {
                if (this.isTransactionalByDefault && threadLocalTransaction == null)
                {
                    this.rollBack(repoTransaction, e, sources);
                }
                throw e;
            }
        }
        else
        {
            result = new SourceMutation();

            Multimap<String, ? extends Source> sourcesByRepo = sources.groupBy((Source source) -> PureCodeStorage.getSourceRepoName(source.getId()));
            Multimap<String, CoreInstance> toProcessByRepo = this.toProcess.groupBy(GET_COREINSTANCE_REPO_NAME);
            MutableSet<String> allReposToCompile = Sets.mutable.withAll(sourcesByRepo.keysView()).withAll(toProcessByRepo.keysView());

            int repoNum = 1;
            int repoCount = allReposToCompile.size();
            ListIterable<String> repoCompileOrder = allReposToCompile.toSortedList(new RepositoryComparator(this.codeStorage.getAllRepositories()));
            for (String repo : repoCompileOrder)
            {
                IncrementalCompilerTransaction repoTransaction = this.isTransactionalByDefault && threadLocalTransaction == null ? this.newTransaction(true) : threadLocalTransaction;
                RichIterable<? extends Source> repoSources = sourcesByRepo.get(repo);
                RichIterable<CoreInstance> toProcessThisRepo = toProcessByRepo.get(repo);
                SourceMutation repoResult;
                try
                {
                    repoResult = this.compileRepoSources(repoTransaction, repo, repoNum++, repoCount, repoSources, toProcessThisRepo);
                    if (this.isTransactionalByDefault && threadLocalTransaction == null)
                    {
                        repoTransaction.commit();
                    }
                }
                catch (RuntimeException e)
                {
                    if (this.isTransactionalByDefault && threadLocalTransaction == null)
                    {
                        this.rollBack(repoTransaction, e, repoSources);
                    }
                    throw e;
                }
                result.merge(repoResult);
                compiledSourcesByRepo.putAll(repo, repoSources);
            }
        }

        this.runEventHandlers(compilerEventHandlers, copyToProcess, compiledSourcesByRepo);
        return result;
    }

    private SourceMutation compileRepoSources(final IncrementalCompilerTransaction transaction, String repoName, final int repoNum, final int repoTotalCount, RichIterable<? extends Source> sources, RichIterable<CoreInstance> instancesToProcess) throws PureCompilationException, PureParserException
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
                        if (IncrementalCompiler_Old.this.message != null)
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
                            IncrementalCompiler_Old.this.message.setMessage(message.toString());
                        }
                        ListMultimap<Parser, CoreInstance> newInstancesByParser = new TopParser().parse(source.getContent(), source.getId(), IncrementalCompiler_Old.this.modelRepository, IncrementalCompiler_Old.this.library, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, IncrementalCompiler_Old.this.context, null);
                        IncrementalCompiler_Old.this.updateSource(source, newInstancesByParser);
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
            MutableList<CoreInstance> newInstancesConsolidated = instancesToProcess.toList();
            for (Source source : sources)
            {
                newInstancesConsolidated.addAllIterable(source.getNewInstances());
            }
            return this.finishRepoCompilation(repoDisplayName, newInstancesConsolidated, ValidationType.SHALLOW);
        }
    }

    //------------------
    //  UnLoadFromGraph
    //------------------

    @Override
    void unload()
    {
        if (!this.toUnload.isEmpty())
        {
            SetIterable<CoreInstance> consolidatedCoreInstances = this.walkTheGraphForUnload();

            // Start Event
            MutableSet<CoreInstance> allUnbind = UnifiedSet.newSet(consolidatedCoreInstances);
            allUnbind.addAllIterable(this.toUnload);
            for (CompilerEventHandler compilerEventHandler : this.compilerEventHandlers)
            {
                compilerEventHandler.invalidate(allUnbind);
            }
            // Stop Event
            this.unbindGraphDependencies(allUnbind);

            // Invalidate Source Elements
            for (CoreInstance instance : this.toUnload)
            {
                this.removeInstance(instance);
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

            this.toUnload.clear();
        }
    }

    private void unbindGraphDependencies(SetIterable<CoreInstance> consolidatedCoreInstances)
    {
        Unbinder.process(consolidatedCoreInstances, this.modelRepository, this.library, this.dslLibrary, this.context, this.processorSupport, new UnbindState(this.context, this.urlPatternLibrary, this.processorSupport), this.message);
        this.toProcess.addAllIterable(consolidatedCoreInstances);
    }

    private SetIterable<CoreInstance> walkTheGraphForUnload()
    {
        WalkerState walkerState = new WalkerState(this.processorSupport);

        Matcher walkerMatcher = new Matcher(this.modelRepository, this.context, this.processorSupport);

        for (MatchRunner walker : LazyIterate.concatenate(this.library.getParsers().asLazy().flatCollect(Parser.GET_UNLOAD_WALKERS), this.dslLibrary.getInlineDSLs().asLazy().flatCollect(InlineDSL.GET_UNLOAD_WALKERS)))
        {
            walkerMatcher.addMatchIfTypeIsKnown(walker);
        }

        for (CoreInstance instance : this.toUnload)
        {
            walkerMatcher.match(instance, walkerState);
        }
        return walkerState.getInstances();
    }
}
