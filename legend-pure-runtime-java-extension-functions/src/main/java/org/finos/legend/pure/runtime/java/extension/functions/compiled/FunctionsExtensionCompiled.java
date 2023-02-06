// Copyright 2022 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License",
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

package org.finos.legend.pure.runtime.java.extension.functions.compiled;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.BaseCompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.MayExecuteAlloyTest;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.MayExecuteLegendTest;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.Profile;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.anonymousCollections.ReplaceTreeNode;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.anonymousCollections.map.*;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.cipher.Decrypt;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.cipher.Encrypt;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.collection.Get;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.collection.IndexOf;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.collection.*;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.*;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.hash.Hash;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.io.ReadFile;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.io.http.Http;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.lang.MatchWith;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.lang.MutateAdd;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.lang.RawEvalProperty;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.lang.RemoveOverride;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.*;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.*;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.runtime.CurrentUserId;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.runtime.Guid;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.runtime.IsOptionSet;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.*;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.tracing.TraceSpan;


public class FunctionsExtensionCompiled extends BaseCompiledExtension
{
    public FunctionsExtensionCompiled()
    {
        super(
                "platform_functions",
                () -> Lists.fixedSize.with(
                        // Cipher
                        new Decrypt(),
                        new Encrypt(),

                        // Collection
                        new Drop(),
                        new Exists(),
                        new ForAll(),
                        new Get(),
                        new GroupBy(),
                        new IndexOf(),
                        new Last(),
                        new RemoveAllOptimized(),
                        new Repeat(),
                        new Reverse(),
                        new Slice(),
                        new Take(),
                        new Zip(),

                        //Date
                        new AdjustDate(),
                        new DateDiff(),
                        new DatePart(),
                        new DayOfMonth(),
                        new DayOfWeekNumber(),
                        new HasDay(),
                        new HasHour(),
                        new HasMinute(),
                        new HasMonth(),
                        new HasSecond(),
                        new HasSubsecond(),
                        new HasSubsecondWithAtLeastPrecision(),
                        new Hour(),
                        new Minute(),
                        new MonthNumber(),
                        new NewDate(),
                        new Now(),
                        new Second(),
                        new Today(),
                        new WeekOfYear(),
                        new Year(),

                        //Hash
                        new Hash(),

                        //IO
                        new Http(),
                        new ReadFile(),

                        //Lang
                        new MatchWith(),
                        new MutateAdd(),
                        new RawEvalProperty(),
                        new RemoveOverride(),

                        //Math
                        new ArcCosine(),
                        new ArcSine(),
                        new ArcTangent(),
                        new ArcTangent2(),
                        new Ceiling(),
                        new Cosine(),
                        new Exp(),
                        new Floor(),
                        new Log(),
                        new Mod(),
                        new Pow(),
                        new Rem(),
                        new Round(),
                        new RoundWithScale(),
                        new Sine(),
                        new Sqrt(),
                        new StdDev(),
                        new Tangent(),
                        new ToDecimal(),
                        new ToFloat(),

                        // Meta
                        new CanReactivateDynamically(),
                        new CompileValueSpecification(),
                        new Deactivate(),
                        new EnumName(),
                        new EnumValues(),
                        new FunctionDescriptorToId(),
                        new Generalizations(),
                        new IsSourceReadOnly(),
                        new IsValidFunctionDescriptor(),
                        new NewAssociation(),
                        new NewClass(),
                        new NewEnumeration(),
                        new NewLambdaFunction(),
                        new NewProperty(),
                        new OpenVariableValues(),
                        new Reactivate(),
                        new SourceInformation(),
                        new Stereotype(),
                        new SubTypeOf(),
                        new Tag(),

                        //Runtime
                        new CurrentUserId(),
                        new IsOptionSet(),
                        new Guid(),

                        //String
                        new Chunk(),
                        new Contains(),
                        new EndsWith(),
                        new org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.IndexOf(),
                        new ParseBoolean(),
                        new ParseDate(),
                        new ParseFloat(),
                        new ParseDecimal(),
                        new ParseInteger(),
                        new Matches(),
                        new ToLower(),
                        new ToUpper(),
                        new Trim(),
                        new EncodeBase64(),
                        new DecodeBase64(),

                        //Tracing
                        new TraceSpan(),

                        // LegendTests
                        new MayExecuteAlloyTest(),
                        new MayExecuteLegendTest(),

                        //Tools
                        new Profile(),

                        //Anonymous Collections
                        new org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.anonymousCollections.map.Get(),
                        new GetIfAbsentPutWithKey(),
                        new GetMapStats(),
                        new Keys(),
                        new NewMap(),
                        new KeyValues(),
                        new NewMapWithProperties(),
                        new Put(),
                        new PutAllMaps(),
                        new PutAllPairs(),
                        new ReplaceAll(),
                        new Values(),
                        new ReplaceTreeNode()

                ),


                Lists.fixedSize.with(StringJavaSource.newStringJavaSource("org.finos.legend.pure.generated", "FunctionsGen",
                        "package org.finos.legend.pure.generated;\n" +
                                "\n" +
                                "import org.eclipse.collections.api.RichIterable;\n" +
                                "import org.eclipse.collections.api.list.MutableList;\n" +
                                "import org.eclipse.collections.impl.list.mutable.FastList;\n" +
                                "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;\n" +
                                "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;\n" +
                                "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n" +
                                "import org.finos.legend.pure.m4.coreinstance.SourceInformation;\n" +
                                "import org.finos.legend.pure.runtime.java.compiled.delta.CodeBlockDeltaCompiler;\n" +
                                "import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;\n" +
                                "import org.finos.legend.pure.runtime.java.shared.http.HttpMethod;\n" +
                                "import org.finos.legend.pure.runtime.java.shared.http.HttpRawHelper;\n" +
                                "import org.finos.legend.pure.runtime.java.shared.http.URLScheme;\n" +
                                "\n" +
                                "\n" +
                                "public class FunctionsGen\n" +
                                "{\n" +
                                "    public static Root_meta_pure_functions_io_http_HTTPResponse executeHttpRaw(Root_meta_pure_functions_io_http_URL url, Object method, String mimeType, String body, ExecutionSupport executionSupport)\n" +
                                "    {\n" +
                                "        URLScheme scheme = URLScheme.http;\n" +
                                "        if (url._scheme() != null)\n" +
                                "        {\n" +
                                "            scheme = URLScheme.valueOf(url._scheme()._name());\n" +
                                "        }\n" +
                                "        return (Root_meta_pure_functions_io_http_HTTPResponse) HttpRawHelper.toHttpResponseInstance(HttpRawHelper.executeHttpService(scheme, url._host(), (int) url._port(), url._path(), HttpMethod.valueOf(((Enum) method)._name()), mimeType, body), ((CompiledExecutionSupport) executionSupport).getProcessorSupport());\n" +
                                "    }\n" +
                                "\n" +
                                "    public static Root_meta_pure_functions_meta_CompilationResult compileCodeBlock(String source, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        Root_meta_pure_functions_meta_CompilationResult result = null;\n" +
                                "        if (source != null)\n" +
                                "        {\n" +
                                "            CodeBlockDeltaCompiler.CompilationResult compilationResult = CodeBlockDeltaCompiler.compileCodeBlock(source, ((CompiledExecutionSupport) es));\n" +
                                "            result = convertCompilationResult(compilationResult);\n" +
                                "        }\n" +
                                "        return result;\n" +
                                "    }\n" +
                                "\n" +
                                "    public static RichIterable<Root_meta_pure_functions_meta_CompilationResult> compileCodeBlocks(RichIterable<? extends String> sources, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        RichIterable<CodeBlockDeltaCompiler.CompilationResult> compilationResults = CodeBlockDeltaCompiler.compileCodeBlocks(sources, ((CompiledExecutionSupport) es));\n" +
                                "        MutableList<Root_meta_pure_functions_meta_CompilationResult> results = FastList.newList(sources.size());\n" +
                                "\n" +
                                "        for (CodeBlockDeltaCompiler.CompilationResult compilationResult : compilationResults)\n" +
                                "        {\n" +
                                "            results.add(convertCompilationResult(compilationResult));\n" +
                                "        }\n" +
                                "        return results;\n" +
                                "    }\n" +
                                "\n" +
                                "\n" +
                                "    private static Root_meta_pure_functions_meta_CompilationResult convertCompilationResult(CodeBlockDeltaCompiler.CompilationResult compilationResult)\n" +
                                "    {\n" +
                                "        Root_meta_pure_functions_meta_CompilationResult result = new Root_meta_pure_functions_meta_CompilationResult_Impl(\"\");\n" +
                                "\n" +
                                "        if (compilationResult.getFailureMessage() != null)\n" +
                                "        {\n" +
                                "            Root_meta_pure_functions_meta_CompilationFailure failure = new Root_meta_pure_functions_meta_CompilationFailure_Impl(\"\");\n" +
                                "            failure._message(compilationResult.getFailureMessage());\n" +
                                "\n" +
                                "            SourceInformation si = compilationResult.getFailureSourceInformation();\n" +
                                "\n" +
                                "            if (si != null)\n" +
                                "            {\n" +
                                "                Root_meta_pure_functions_meta_SourceInformation sourceInformation = new Root_meta_pure_functions_meta_SourceInformation_Impl(\"\");\n" +
                                "                sourceInformation._column(si.getColumn());\n" +
                                "                sourceInformation._line(si.getLine());\n" +
                                "                sourceInformation._endColumn(si.getEndColumn());\n" +
                                "                sourceInformation._endLine(si.getEndLine());\n" +
                                "                sourceInformation._startColumn(si.getStartColumn());\n" +
                                "                sourceInformation._startLine(si.getStartLine());\n" +
                                "                failure._sourceInformation(sourceInformation);\n" +
                                "            }\n" +
                                "            result._failure(failure);\n" +
                                "        }\n" +
                                "        else\n" +
                                "        {\n" +
                                "            ConcreteFunctionDefinition<?> cfd = (ConcreteFunctionDefinition<?>) compilationResult.getResult();\n" +
                                "            result._result(cfd._expressionSequence().getFirst());\n" +
                                "        }\n" +
                                "        return result;\n" +
                                "    }\n" +
                                "}")),
                Lists.fixedSize.with(),
                Lists.fixedSize.with()
        );
    }

    public static CompiledExtension extension()
    {
        return new FunctionsExtensionCompiled();
    }
}
