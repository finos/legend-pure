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

package org.finos.legend.pure.runtime.java.compiled.delta;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.runtime.IncrementalCompiler;
import org.finos.legend.pure.m3.serialization.runtime.IncrementalCompiler.IncrementalCompilerTransaction;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.m4.transaction.framework.ThreadLocalTransactionContext;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.MetadataJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;

public final class CodeBlockDeltaCompiler
{
    private static final Function2<String, CompiledExecutionSupport, CompilationResult> COMPILE_CODE_BLOCK = new Function2<String, CompiledExecutionSupport, CompilationResult>()
    {
        @Override
        public CompilationResult value(String code, CompiledExecutionSupport executionSupport)
        {
            return compileCodeBlock(code, executionSupport);
        }
    };

    private static final CreateSourceFunction CREATE_SOURCE_FUNCTION = new CreateSourceFunction();

    private CodeBlockDeltaCompiler()
    {
    }

    public static Pair<String, String> wrapCodeBlock(String codeBlock)
    {
        String uniqueFunctionId = "codeBlock_" + Thread.currentThread().getId() + "_" + System.nanoTime();
        String wrappedCodeBlock = "function " + uniqueFunctionId + "():Any[*]{\n" + codeBlock + "\n}";
        return Tuples.pair(uniqueFunctionId, wrappedCodeBlock);
    }

    public static RichIterable<CompilationResult> compileCodeBlocks(RichIterable<? extends String> codeBlocks, CompiledExecutionSupport executionSupport)
    {
        //This needs to stream - so make sure it uses rich iterables
        return codeBlocks.collectWith(COMPILE_CODE_BLOCK, executionSupport);
    }

    public static CompilationResult compileCodeBlock(String code, CompiledExecutionSupport executionSupport)
    {
        IncrementalCompilerTransaction transaction = executionSupport.getIncrementalCompiler().newTransaction(false);
        try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
        {
            return new BuildMetadataFunction(executionSupport.getMetadataProvider()).valueOf(Lists.mutable.of(
                    new InterpretedCompileFunction(executionSupport.getIncrementalCompiler()).valueOf(
                            CREATE_SOURCE_FUNCTION.valueOf(code)))).getFirst();
        }
        finally
        {
            transaction.rollback();
        }
    }

    private static class CreateSourceFunction implements Function<String, Source>
    {
        @Override
        public Source valueOf(String code)
        {
            Pair<String, String> codeBlockPair = wrapCodeBlock(code);
            return Source.createMutableInMemorySource(codeBlockPair.getOne(), codeBlockPair.getTwo());
        }
    }

    private static class InterpretedCompileFunction implements Function<Source, IntermediateCompilationResult>
    {
        private final IncrementalCompiler incrementalCompiler;

        private InterpretedCompileFunction(IncrementalCompiler incrementalCompiler)
        {
            this.incrementalCompiler = incrementalCompiler;
        }

        @Override
        public IntermediateCompilationResult valueOf(Source source)
        {
            try
            {
                //First compile and validate that the code is actually correct
                this.incrementalCompiler.compileInCurrentTransaction(source);
                CoreInstance instance = source.getNewInstances().getFirst();
                String functionName = PackageableElement.getSystemPathForPackageableElement(instance);
                return new IntermediateCompilationResult(source, functionName);
            }
            catch (PureCompilationException | PureParserException ex)
            {
                return IntermediateCompilationResult.createForFailure(ex);
            }
        }
    }

    private static class BuildMetadataFunction implements Function<RichIterable<IntermediateCompilationResult>, RichIterable<CompilationResult>>
    {
        private final MetadataProvider metadataProvider;

        private BuildMetadataFunction(MetadataProvider metadataProvider)
        {
            this.metadataProvider = metadataProvider;
        }

        @Override
        public RichIterable<CompilationResult> valueOf(RichIterable<IntermediateCompilationResult> intermediateResults)
        {

            MutableList<CompilationResult> results = new FastList<>(intermediateResults.size());
            MutableMap<String, CompilationResult> functionToResultIndex = Maps.mutable.of();
            MutableSet<CoreInstance> newInstances = Sets.mutable.of();

            for (IntermediateCompilationResult compilationResult : intermediateResults)
            {
                if (compilationResult.wasSuccess())
                {
                    CompilationResult result = new CompilationResult();
                    results.add(result);
                    functionToResultIndex.put(compilationResult.functionName, result);
                    newInstances.addAllIterable(compilationResult.source.getNewInstances());
                }
                else
                {
                    results.add(new CompilationResult(compilationResult.failureMessage,
                            compilationResult.failureSourceInformation));
                }
            }
            try
            {
                this.metadataProvider.startTransaction();
                this.metadataProvider.buildMetadata(newInstances);
                Metadata metadata = this.metadataProvider.getMetadata();

                for (String functionName : functionToResultIndex.keysView())
                {
                    functionToResultIndex.get(functionName).result = metadata.getMetadata(MetadataJavaPaths.ConcreteFunctionDefinition, functionName);
                }
            }
            finally
            {
                if (this.metadataProvider != null)
                {
                    this.metadataProvider.rollbackTransaction();
                }
            }

            return results;
        }
    }

    private static class IntermediateCompilationResult
    {
        private final Source source;
        private final String functionName;

        private final String failureMessage;
        private final SourceInformation failureSourceInformation;

        private IntermediateCompilationResult(Source source, String functionName)
        {
            this.source = source;
            this.functionName = functionName;
            this.failureMessage = null;
            this.failureSourceInformation = null;
        }

        private IntermediateCompilationResult(String failureMessage, SourceInformation failureSourceInformation)
        {
            this.source = null;
            this.functionName = null;
            this.failureMessage = failureMessage;
            this.failureSourceInformation = failureSourceInformation;
        }

        public boolean wasSuccess()
        {
            return this.failureMessage == null;
        }

        private static IntermediateCompilationResult createForFailure(PureException ex)
        {
            SourceInformation orig = ex.getOriginatingPureException().getSourceInformation();
            SourceInformation adjustedSourceInfo = null;
            if (orig != null)
            {
                adjustedSourceInfo = new SourceInformation("",
                        adjustLine(orig.getStartLine()),
                        orig.getStartColumn(),
                        adjustLine(orig.getLine()),
                        orig.getColumn(),
                        adjustLine(orig.getEndLine()),
                        orig.getEndColumn());
            }

            return new IntermediateCompilationResult(ex.getOriginatingPureException().getInfo(), adjustedSourceInfo);
        }

        private static int adjustLine(int line)
        {
            return line <= 0 ? line : line - 1;
        }
    }

    public static class CompilationResult
    {
        private CoreInstance result;
        private String failureMessage;
        private SourceInformation failureSourceInformation;

        public CompilationResult()
        {
        }

        public CompilationResult(String failureMessage, SourceInformation failureSourceInformation)
        {
            this.failureMessage = failureMessage;
            this.failureSourceInformation = failureSourceInformation;
        }

        public CompilationResult(CoreInstance result, String failureMessage, SourceInformation failureSourceInformation)
        {
            this.result = result;
            this.failureMessage = failureMessage;
            this.failureSourceInformation = failureSourceInformation;
        }

        public CoreInstance getResult()
        {
            return this.result;
        }

        public String getFailureMessage()
        {
            return this.failureMessage;
        }

        public SourceInformation getFailureSourceInformation()
        {
            return this.failureSourceInformation;
        }
    }

}
