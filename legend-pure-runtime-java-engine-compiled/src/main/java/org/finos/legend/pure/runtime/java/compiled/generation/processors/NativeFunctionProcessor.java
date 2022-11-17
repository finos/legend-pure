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

package org.finos.legend.pure.runtime.java.compiled.generation.processors;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.Hash;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core._boolean.*;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.asserts.Assert;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.cipher.Decrypt;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.cipher.Encrypt;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.collection.IndexOf;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.collection.*;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.collection.map.Get;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.collection.map.*;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.date.*;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.io.Http;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.io.Print;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.io.ReadFile;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.lang.*;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.math.*;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.meta.*;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.multiplicity.ToOne;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.multiplicity.ToOneMany;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.multiplicity.ToOneManyWithMessage;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.multiplicity.ToOneWithMessage;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.runtime.CurrentUserId;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.runtime.Guid;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.runtime.IsOptionSet;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.string.*;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.tools.Profile;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.tracing.TraceSpan;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.legend.MayExecuteAlloyTest;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.legend.MayExecuteLegendTest;

public class NativeFunctionProcessor
{
    private static final ImmutableList<String> IMPORTS = Lists.immutable.with(
            "import org.eclipse.collections.api.list.ListIterable;\n",
            "import org.eclipse.collections.api.list.MutableList;\n",
            "import org.eclipse.collections.api.RichIterable;\n",
            "import org.eclipse.collections.api.map.MutableMap;\n",
            "import org.eclipse.collections.impl.factory.Lists;\n",
            "import org.eclipse.collections.impl.map.mutable.UnifiedMap;\n",
            "import org.finos.legend.pure.runtime.java.compiled.*;\n",
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.*;\n",
            "import org.eclipse.collections.api.block.function.Function2;\n",
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;\n",
            "import org.eclipse.collections.api.block.function.Function0;\n",
            "import org.eclipse.collections.api.block.function.Function;\n",
            "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n",
            "import org.eclipse.collections.impl.factory.Maps;\n",
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.*;\n",
            "import org.finos.legend.pure.runtime.java.compiled.*;\n",
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.*;\n",
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.*;\n",
            "import org.finos.legend.pure.runtime.java.compiled.execution.*;\n",
            "import org.finos.legend.pure.runtime.java.compiled.execution.sourceInformation.*;\n",
            //tests only
            "import org.junit.Test;\n");

    private final MutableMap<String, Native> natives;

    private NativeFunctionProcessor(Iterable<? extends Native> extraNatives)
    {
        this.natives = buildNativeMap(extraNatives);
    }

    public void buildNativeLambdaFunction(CoreInstance nativeFunction, ProcessorContext processorContext)
    {
        Native nat = this.natives.get(nativeFunction.getName());
        if (nat != null)
        {
            processorContext.registerNativeLambdaFunction(nativeFunction, nat);
        }
    }

    public String processNativeFunction(CoreInstance topLevelElement, CoreInstance functionExpression, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        CoreInstance nativeFunction = Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.func, processorSupport);
        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);

        Native nat = this.natives.get(nativeFunction.getName());
        if (nat != null)
        {
            ListIterable<String> transformedParams = nat.transformParameterValues(parametersValues, topLevelElement, processorSupport, processorContext);
            return nat.build(topLevelElement, functionExpression, transformedParams, processorContext);
        }

        throw new RuntimeException(nativeFunction.getName() + " Not supported yet");
    }

    public ListIterable<String> getImports()
    {
        return IMPORTS;
    }

    public static String buildM4SourceInformation(SourceInformation sourceInformation)
    {
        return sourceInformation == null ? "null" : "new org.finos.legend.pure.m4.coreinstance.SourceInformation(\"" + sourceInformation.getSourceId() + "\", "
                + sourceInformation.getStartLine()
                + ", " + sourceInformation.getStartColumn()
                + ", " + sourceInformation.getEndLine()
                + ", " + sourceInformation.getEndColumn() + ")";
    }

    public static String buildM4LineColumnSourceInformation(SourceInformation sourceInformation)
    {
        return sourceInformation == null ? "null" : "new org.finos.legend.pure.m4.coreinstance.SourceInformation(\"" + sourceInformation.getSourceId() + "\""
                + ", -1, -1"
                + ", " + sourceInformation.getLine()
                + ", " + sourceInformation.getColumn()
                + ", -1, -1)";
    }

    public static NativeFunctionProcessor newProcessor()
    {
        return newWithExtraNatives(Lists.immutable.empty());
    }

    public static NativeFunctionProcessor newWithExtraNatives(Iterable<? extends Native> extraNatives)
    {
        return new NativeFunctionProcessor(extraNatives);
    }

    public static NativeFunctionProcessor newWithCompiledExtensions(Iterable<? extends CompiledExtension> extensions)
    {
        return newWithExtraNatives(LazyIterate.flatCollect(extensions, CompiledExtension::getExtraNatives));
    }

    private static MutableMap<String, Native> buildNativeMap(Iterable<? extends Native> extraNatives)
    {
        MutableMap<String, Native> map = Maps.mutable.empty();
        registerCoreNatives(map);
        if (extraNatives != null)
        {
            extraNatives.forEach(n -> registerNative(map, n));
        }
        return map;
    }

    private static void registerNative(MutableMap<String, Native> map, Native nativeFunc)
    {
        for (String signature : nativeFunc.signatures())
        {
            Native old = map.put(signature, nativeFunc);
            if (old != null)
            {
                throw new IllegalArgumentException("Error registering function " + nativeFunc + ": " + signature + " is already registered, using " + old);
            }
        }
    }

    private static void registerCoreNatives(MutableMap<String, Native> map)
    {
        //Boolean
        registerNative(map, new And());
        registerNative(map, new Eq());
        registerNative(map, new Equal());
        registerNative(map, new Is());
        registerNative(map, new Not());
        registerNative(map, new Or());

        //Asserts
        registerNative(map, new Assert());

        //Cipher
        registerNative(map, new Decrypt());
        registerNative(map, new Encrypt());

        //Collection - Map
        registerNative(map, new Get());
        registerNative(map, new GetIfAbsentPutWithKey());
        registerNative(map, new GetMapStats());
        registerNative(map, new Keys());
        registerNative(map, new KeyValues());
        registerNative(map, new NewMap());
        registerNative(map, new NewMapWithProperties());
        registerNative(map, new Put());
        registerNative(map, new PutAllMaps());
        registerNative(map, new PutAllPairs());
        registerNative(map, new ReplaceAll());
        registerNative(map, new Values());

        //Collection
        registerNative(map, new Add());
        registerNative(map, new At());
        registerNative(map, new Concatenate());
        registerNative(map, new Drop());
        registerNative(map, new Exists());
        registerNative(map, new Filter());
        registerNative(map, new First());
        registerNative(map, new Fold());
        registerNative(map, new ForAll());
        registerNative(map, new org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.collection.Get());
        registerNative(map, new GetAll());
        registerNative(map, new GroupBy());
        registerNative(map, new IndexOf());
        registerNative(map, new Init());
        registerNative(map, new IsEmpty());
        registerNative(map, new Last());
        registerNative(map, new Map());
        registerNative(map, new Range());
        registerNative(map, new RemoveAllOptimized());
        registerNative(map, new RemoveDuplicates());
        registerNative(map, new Repeat());
        registerNative(map, new ReplaceTreeNode());
        registerNative(map, new Reverse());
        registerNative(map, new Size());
        registerNative(map, new Slice());
        registerNative(map, new Sort());
        registerNative(map, new Tail());
        registerNative(map, new Take());
        registerNative(map, new Zip());

        //Date
        registerNative(map, new AdjustDate());
        registerNative(map, new DateDiff());
        registerNative(map, new DatePart());
        registerNative(map, new DayOfMonth());
        registerNative(map, new DayOfWeekNumber());
        registerNative(map, new HasDay());
        registerNative(map, new HasHour());
        registerNative(map, new HasMinute());
        registerNative(map, new HasMonth());
        registerNative(map, new HasSecond());
        registerNative(map, new HasSubsecond());
        registerNative(map, new HasSubsecondWithAtLeastPrecision());
        registerNative(map, new Hour());
        registerNative(map, new Minute());
        registerNative(map, new MonthNumber());
        registerNative(map, new NewDate());
        registerNative(map, new Now());
        registerNative(map, new Second());
        registerNative(map, new Today());
        registerNative(map, new WeekOfYear());
        registerNative(map, new Year());

        //IO
        registerNative(map, new Print());
        registerNative(map, new ReadFile());
        registerNative(map, new Http());

        //Lang
        registerNative(map, new Compare());
        registerNative(map, new Copy());
        registerNative(map, new DynamicNew());
        registerNative(map, new Eval());
        registerNative(map, new Evaluate());
        registerNative(map, new GetUnitValue());
        registerNative(map, new If());
        registerNative(map, new LessThan());
        registerNative(map, new LessThanEqual());
        registerNative(map, new Let());
        registerNative(map, new Match());
        registerNative(map, new MatchWith());
        registerNative(map, new MutateAdd());
        registerNative(map, new New());
        registerNative(map, new NewWithKeyExpr());
        registerNative(map, new NewUnit());
        registerNative(map, new RawEvalProperty());
        registerNative(map, new RemoveOverride());

        //Tracing
        registerNative(map, new TraceSpan());

        //Math
        registerNative(map, new Abs());
        registerNative(map, new ArcCosine());
        registerNative(map, new ArcSine());
        registerNative(map, new ArcTangent());
        registerNative(map, new ArcTangent2());
        registerNative(map, new Ceiling());
        registerNative(map, new Cosine());
        registerNative(map, new Divide());
        registerNative(map, new DivideDecimal());
        registerNative(map, new Exp());
        registerNative(map, new Floor());
        registerNative(map, new Log());
        registerNative(map, new Minus());
        registerNative(map, new Mod());
        registerNative(map, new Plus());
        registerNative(map, new Pow());
        registerNative(map, new Rem());
        registerNative(map, new Round());
        registerNative(map, new RoundWithScale());
        registerNative(map, new Sine());
        registerNative(map, new Sqrt());
        registerNative(map, new StdDev());
        registerNative(map, new Tangent());
        registerNative(map, new Times());
        registerNative(map, new ToDecimal());
        registerNative(map, new ToFloat());

        //Meta
        registerNative(map, new CanReactivateDynamically());
        registerNative(map, new Cast());
        registerNative(map, new CompileValueSpecification());
        registerNative(map, new Deactivate());
        registerNative(map, new EnumName());
        registerNative(map, new EnumValues());
        registerNative(map, new EvaluateAndDeactivate());
        registerNative(map, new ExtractEnumValue());
        registerNative(map, new FunctionDescriptorToId());
        registerNative(map, new Generalizations());
        registerNative(map, new GenericType());
        registerNative(map, new GenericTypeClass());
        registerNative(map, new Id());
        registerNative(map, new IsValidFunctionDescriptor());
        registerNative(map, new InstanceOf());
        registerNative(map, new MayExecuteAlloyTest());
        registerNative(map, new MayExecuteLegendTest());
        registerNative(map, new NewAssociation());
        registerNative(map, new NewEnumeration());
        registerNative(map, new NewLambdaFunction());
        registerNative(map, new NewClass());
        registerNative(map, new NewProperty());
        registerNative(map, new OpenVariableValues());
        registerNative(map, new Reactivate());
        registerNative(map, new org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.meta.SourceInformation());
        registerNative(map, new Stereotype());
        registerNative(map, new SubTypeOf());
        registerNative(map, new Tag());

        //Multiplicity
        registerNative(map, new ToOne());
        registerNative(map, new ToOneWithMessage());
        registerNative(map, new ToOneMany());
        registerNative(map, new ToOneManyWithMessage());

        //Runtime
        registerNative(map, new CurrentUserId());
        registerNative(map, new IsOptionSet());
        registerNative(map, new Guid());

        //String
        registerNative(map, new Chunk());
        registerNative(map, new Contains());
        registerNative(map, new EndsWith());
        registerNative(map, new Format());
        registerNative(map, new org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.string.IndexOf());
        registerNative(map, new IsSourceReadOnly());
        registerNative(map, new JoinStrings());
        registerNative(map, new Length());
        registerNative(map, new ParseBoolean());
        registerNative(map, new ParseDate());
        registerNative(map, new ParseFloat());
        registerNative(map, new ParseDecimal());
        registerNative(map, new ParseInteger());
        registerNative(map, new Replace());
        registerNative(map, new Split());
        registerNative(map, new StartsWith());
        registerNative(map, new Matches());
        registerNative(map, new Substring());
        registerNative(map, new ToLower());
        registerNative(map, new ToString());
        registerNative(map, new ToUpper());
        registerNative(map, new Trim());
        registerNative(map, new EncodeBase64());
        registerNative(map, new DecodeBase64());

        //Tools
        registerNative(map, new Profile());

        //Hash
        registerNative(map, new Hash());
    }
}