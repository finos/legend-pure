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
import org.eclipse.collections.api.tuple.Pair;
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
        return codeBlocks.collect(code -> compileCodeBlock(code, executionSupport));
    }

    public static CompilationResult compileCodeBlock(String code, CompiledExecutionSupport executionSupport)
    {
        IncrementalCompilerTransaction transaction = executionSupport.getIncrementalCompiler().newTransaction(false);
        try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
        {
            return buildCompilationResult(compileSource(executionSupport.getIncrementalCompiler(), createSource(code)), executionSupport.getMetadataProvider());
        }
        finally
        {
            transaction.rollback();
        }
    }

    private static Source createSource(String code)
    {
        Pair<String, String> codeBlockPair = wrapCodeBlock(code);
        return Source.createMutableInMemorySource(codeBlockPair.getOne(), codeBlockPair.getTwo());
    }

    private static IntermediateCompilationResult compileSource(IncrementalCompiler incrementalCompiler, Source source)
    {
        try
        {
            //First compile and validate that the code is actually correct
            incrementalCompiler.compileInCurrentTransaction(source);
            CoreInstance instance = source.getNewInstances().getFirst();
            String functionName = PackageableElement.getSystemPathForPackageableElement(instance);
            return new IntermediateCompilationResult(source, functionName);
        }
        catch (PureCompilationException | PureParserException e)
        {
            return IntermediateCompilationResult.createForFailure(e);
        }
    }

    private static CompilationResult buildCompilationResult(IntermediateCompilationResult intermediateResult, MetadataProvider metadataProvider)
    {
        if (!intermediateResult.wasSuccess())
        {
            return new CompilationResult(intermediateResult.failureMessage, intermediateResult.failureSourceInformation);
        }

        try
        {
            metadataProvider.startTransaction();
            metadataProvider.buildMetadata(intermediateResult.source.getNewInstances());
            Metadata metadata = metadataProvider.getMetadata();
            return new CompilationResult(metadata.getMetadata(MetadataJavaPaths.ConcreteFunctionDefinition, intermediateResult.functionName));
        }
        finally
        {
            metadataProvider.rollbackTransaction();
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

        private static IntermediateCompilationResult createForFailure(PureException e)
        {
            PureException originalEx = e.getOriginatingPureException();
            SourceInformation originalSourceInfo = originalEx.getSourceInformation();
            SourceInformation sourceInfo = (originalSourceInfo == null) ?
                                           null :
                                           new SourceInformation("",
                                                   adjustLine(originalSourceInfo.getStartLine()),
                                                   originalSourceInfo.getStartColumn(),
                                                   adjustLine(originalSourceInfo.getLine()),
                                                   originalSourceInfo.getColumn(),
                                                   adjustLine(originalSourceInfo.getEndLine()),
                                                   originalSourceInfo.getEndColumn());
            return new IntermediateCompilationResult(originalEx.getInfo(), sourceInfo);
        }

        private static int adjustLine(int line)
        {
            return line <= 0 ? line : line - 1;
        }
    }

    public static class CompilationResult
    {
        private final CoreInstance result;
        private final String failureMessage;
        private final SourceInformation failureSourceInformation;

        public CompilationResult(CoreInstance result, String failureMessage, SourceInformation failureSourceInformation)
        {
            this.result = result;
            this.failureMessage = failureMessage;
            this.failureSourceInformation = failureSourceInformation;
        }

        public CompilationResult(String failureMessage, SourceInformation failureSourceInformation)
        {
            this(null, failureMessage, failureSourceInformation);
        }

        public CompilationResult(CoreInstance result)
        {
            this(result, null, null);
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
