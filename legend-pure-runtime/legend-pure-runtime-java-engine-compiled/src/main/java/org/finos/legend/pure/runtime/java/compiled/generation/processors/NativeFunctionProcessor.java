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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.anonymous.map.GetIfAbsentPutWithKey;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.anonymous.map.GetMapStats;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.anonymous.map.GroupBy;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.anonymous.map.KeyValues;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.anonymous.map.Keys;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.anonymous.map.NewMap;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.anonymous.map.NewMapWithProperties;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.anonymous.map.Put;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.anonymous.map.PutAllMaps;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.anonymous.map.PutAllPairs;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.anonymous.map.ReplaceAll;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.anonymous.map.Values;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.anonymous.tree.ReplaceTreeNode;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.index.IndexOf;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.iteration.Find;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.operation.Add;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.index.At;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.operation.Concatenate;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.iteration.Fold;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.operation.RemoveAllOptimized;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.operation.Zip;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.order.Reverse;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.quantification.Exists;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.quantification.ForAll;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.slice.Drop;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.slice.Init;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.operation.RemoveDuplicates;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.order.Sort;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.slice.Last;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.slice.Slice;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.slice.Tail;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.slice.Take;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.date.creation.NewDate;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.date.extract.DatePart;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.date.extract.DayOfMonth;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.date.extract.Hour;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.date.extract.Minute;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.date.extract.MonthNumber;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.date.extract.Second;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.date.extract.Year;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.date.has.HasDay;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.date.has.HasHour;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.date.has.HasMinute;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.date.has.HasMonth;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.date.has.HasSecond;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.date.has.HasSubsecond;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.date.has.HasSubsecondWithAtLeastPrecision;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.date.operation.AdjustDate;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.date.operation.DateDiff;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.io.Print;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.lang.cast.Cast;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.lang.cast.ToDecimal;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.lang.cast.ToFloat;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.lang.cast.ToMultiplicity;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.lang.creation.DynamicNew;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.lang.eval.Eval;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.lang.eval.Evaluate;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.lang.eval.RawEvalProperty;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.lang.flow.If;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.lang.flow.Match;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.lang.flow.MatchWith;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.Random;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.exponential.Exp;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.exponential.Log;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.exponential.Log10;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.operation.Abs;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.operation.Mod;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.operation.Rem;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.operation.Sign;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.power.Cbrt;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.power.Pow;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.power.Sqrt;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.round.Ceiling;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.round.Floor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.round.Round;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.round.RoundWithScale;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.trigonometry.ArcCosine;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.trigonometry.ArcSine;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.trigonometry.ArcTangent;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.trigonometry.ArcTangent2;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.trigonometry.CoTangent;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.trigonometry.Cosine;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.trigonometry.Sine;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.math.trigonometry.Tangent;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.RemoveOverride;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.graph.ElementPath;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.graph.ElementToPath;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.profile.Stereotype;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.profile.Tag;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.reflect.CanReactivateDynamically;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.reflect.Deactivate;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.reflect.EvaluateAndDeactivate;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.reflect.OpenVariableValues;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.reflect.Reactivate;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.type.Generalizations;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.type.GenericType;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.type.SubTypeOf;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.type._class.GenericTypeClass;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.lang.unit.GetUnitValue;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.instance.Id;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.type.InstanceOf;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.graph.LenientPathToElement;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.lang.unit.NewUnit;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.graph.PathToElement;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.type._enum.EnumName;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.type._enum.EnumValues;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.type.relation.AddColumns;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string._boolean.Contains;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string._boolean.EndsWith;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.index.IndexOfWithFrom;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.operation.ReverseString;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.operation.ToLower;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.operation.ToUpper;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.parse.ParseBoolean;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.parse.ParseDate;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.parse.ParseDecimal;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.parse.ParseDecimalWithScalePrecision;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.parse.ParseFloat;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.parse.ParseInteger;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.toString.Format;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.index.Length;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.operation.Replace;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.split.Split;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string._boolean.StartsWith;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.operation.Substring2;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.operation.Substring3;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.toString.ToString;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.trim.LTrim;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.trim.RTrim;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.trim.Trim;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.tests.Assert;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.tests.AssertError;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar._boolean.operation.And;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar._boolean.operation.Not;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar._boolean.operation.Or;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar._boolean.equality.Eq;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar._boolean.equality.Equal;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar._boolean.equality.Is;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar._boolean.inequality.LessThan;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar._boolean.inequality.LessThanEqual;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.collection.iteration.Filter;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.collection.slice.First;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.collection.size.IsEmpty;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.collection.iteration.Map;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.math.sequence.Range;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.collection.size.Size;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.lang.Compare;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.lang.creation.Copy;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.lang._enum.ExtractEnumValue;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.lang.all.GetAll;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.lang.Let;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.lang.creation.New;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.lang.creation.NewWithKeyExpr;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.math.operation.Divide;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.math.operation.DivideDecimal;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.math.operation.Minus;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.math.operation.Plus;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.math.operation.Times;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.lang.cast.ToOne;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.lang.cast.ToOneMany;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.lang.cast.ToOneManyWithMessage;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.lang.cast.ToOneWithMessage;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.string.operation.JoinStrings;

public class NativeFunctionProcessor
{
    private static final ImmutableList<String> IMPORTS = Lists.immutable.with(
            "import org.eclipse.collections.api.list.ListIterable;\n",
            "import org.eclipse.collections.api.list.MutableList;\n",
            "import org.eclipse.collections.api.RichIterable;\n",
            "import org.eclipse.collections.api.map.MutableMap;\n",
            "import org.eclipse.collections.impl.factory.Lists;\n",
            "import org.eclipse.collections.impl.map.mutable.UnifiedMap;\n",
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.*;\n",
            "import org.eclipse.collections.api.block.function.Function2;\n",
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;\n",
            "import org.eclipse.collections.api.block.function.Function0;\n",
            "import org.eclipse.collections.api.block.function.Function;\n",
            "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n",
            "import org.eclipse.collections.impl.factory.Maps;\n",
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.*;\n",
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.*;\n",
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.*;\n",
            "import org.finos.legend.pure.runtime.java.compiled.execution.*;\n",
            "import org.finos.legend.pure.runtime.java.compiled.execution.sourceInformation.*;\n");

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
        registerGrammarCoreNatives(map);
        registerBasicsCoreNatives(map);
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

    private static void registerGrammarCoreNatives(MutableMap<String, Native> map)
    {
        //Boolean
        registerNative(map, new And());
        registerNative(map, new Not());
        registerNative(map, new Or());
        registerNative(map, new Eq());
        registerNative(map, new Equal());
        registerNative(map, new Is());
        registerNative(map, new LessThan());
        registerNative(map, new LessThanEqual());

        //Collection
        registerNative(map, new Filter());
        registerNative(map, new First());
        registerNative(map, new IsEmpty());
        registerNative(map, new Map());
        registerNative(map, new Range());
        registerNative(map, new Size());

        //Lang
        registerNative(map, new Compare());
        registerNative(map, new Copy());
        registerNative(map, new ExtractEnumValue());
        registerNative(map, new GetAll());
        registerNative(map, new Let());
        registerNative(map, new New());
        registerNative(map, new NewWithKeyExpr());

        //Math
        registerNative(map, new Divide());
        registerNative(map, new DivideDecimal());
        registerNative(map, new Minus());
        registerNative(map, new Plus());
        registerNative(map, new Times());

        //Multiplicity
        registerNative(map, new ToOne());
        registerNative(map, new ToOneWithMessage());
        registerNative(map, new ToOneMany());
        registerNative(map, new ToOneManyWithMessage());

        //String
        registerNative(map, new JoinStrings());
    }

    private static void registerBasicsCoreNatives(MutableMap<String, Native> map)
    {
        //Collection
        //  Anonymous
        //    Map
        registerNative(map, new org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.anonymous.map.Get());
        registerNative(map, new GetIfAbsentPutWithKey());
        registerNative(map, new GetMapStats());
        registerNative(map, new Keys());
        registerNative(map, new NewMap());
        registerNative(map, new KeyValues());
        registerNative(map, new NewMapWithProperties());
        registerNative(map, new Put());
        registerNative(map, new PutAllMaps());
        registerNative(map, new PutAllPairs());
        registerNative(map, new ReplaceAll());
        registerNative(map, new Values());
        registerNative(map, new GroupBy());
        //    Tree
        registerNative(map, new ReplaceTreeNode());
        //  Index
        registerNative(map, new At());
        registerNative(map, new IndexOf());
        //  Iteration
        registerNative(map, new Fold());
        registerNative(map, new Find());
        //  Operation
        registerNative(map, new Add());
        registerNative(map, new Concatenate());
        registerNative(map, new RemoveAllOptimized());
        registerNative(map, new RemoveDuplicates());
        registerNative(map, new Zip());
        //  Order
        registerNative(map, new Sort());
        registerNative(map, new Reverse());
        //  Quantification
        registerNative(map, new Exists());
        registerNative(map, new ForAll());
        //  Slice
        registerNative(map, new Drop());
        registerNative(map, new Init());
        registerNative(map, new Last());
        registerNative(map, new Slice());
        registerNative(map, new Tail());
        registerNative(map, new Take());

        //Date
        //  Creation
        registerNative(map, new NewDate());
        //  Extract
        registerNative(map, new DatePart());
        registerNative(map, new DayOfMonth());
        registerNative(map, new Hour());
        registerNative(map, new Minute());
        registerNative(map, new MonthNumber());
        registerNative(map, new Second());
        registerNative(map, new Year());
        //  Has
        registerNative(map, new HasDay());
        registerNative(map, new HasHour());
        registerNative(map, new HasMinute());
        registerNative(map, new HasMonth());
        registerNative(map, new HasSecond());
        registerNative(map, new HasSubsecond());
        registerNative(map, new HasSubsecondWithAtLeastPrecision());
        //  Operation
        registerNative(map, new AdjustDate());
        registerNative(map, new DateDiff());

        //IO
        registerNative(map, new Print());

        //Lang
        //  Cast
        registerNative(map, new Cast());
        registerNative(map, new ToDecimal());
        registerNative(map, new ToFloat());
        registerNative(map, new ToMultiplicity());
        //  Creation
        registerNative(map, new DynamicNew());
        //  Eval
        registerNative(map, new Eval());
        registerNative(map, new Evaluate());
        registerNative(map, new RawEvalProperty());
        //  Flow
        registerNative(map, new If());
        registerNative(map, new Match());
        registerNative(map, new MatchWith());
        //  Unit
        registerNative(map, new GetUnitValue());
        registerNative(map, new NewUnit());

        //Math
        registerNative(map, new Random());
        //  Exponential
        registerNative(map, new Exp());
        registerNative(map, new Log());
        registerNative(map, new Log10());
        //  Operation
        registerNative(map, new Abs());
        registerNative(map, new Mod());
        registerNative(map, new Rem());
        registerNative(map, new Sign());
        //  Power
        registerNative(map, new Cbrt());
        registerNative(map, new Pow());
        registerNative(map, new Sqrt());
        //  Round
        registerNative(map, new Ceiling());
        registerNative(map, new Floor());
        registerNative(map, new Round());
        registerNative(map, new RoundWithScale());
        //  Trigonometry
        registerNative(map, new ArcCosine());
        registerNative(map, new ArcSine());
        registerNative(map, new ArcTangent());
        registerNative(map, new ArcTangent2());
        registerNative(map, new Cosine());
        registerNative(map, new CoTangent());
        registerNative(map, new Sine());
        registerNative(map, new Tangent());

        //Meta
        registerNative(map, new RemoveOverride());
        //  Graph
        registerNative(map, new ElementPath());
        registerNative(map, new ElementToPath());
        registerNative(map, new PathToElement());
        registerNative(map, new LenientPathToElement());
        //  Id
        registerNative(map, new Id());
        //  Profile
        registerNative(map, new Stereotype());
        registerNative(map, new Tag());
        //  Reflect
        registerNative(map, new CanReactivateDynamically());
        registerNative(map, new Deactivate());
        registerNative(map, new EvaluateAndDeactivate());
        registerNative(map, new OpenVariableValues());
        registerNative(map, new Reactivate());
        //  Source
        registerNative(map, new org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.source.SourceInformation());
        //  Type
        registerNative(map, new Generalizations());
        registerNative(map, new GenericType());
        registerNative(map, new InstanceOf());
        registerNative(map, new SubTypeOf());
        //    Class
        registerNative(map, new GenericTypeClass());
        //    Enum
        registerNative(map, new EnumName());
        registerNative(map, new EnumValues());
        //    Relation
        registerNative(map, new AddColumns());

        //String
        //  Boolean
        registerNative(map, new Contains());
        registerNative(map, new EndsWith());
        registerNative(map, new StartsWith());
        //  Index
        registerNative(map, new org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.string.index.IndexOf());
        registerNative(map, new IndexOfWithFrom());
        registerNative(map, new Length());
        //  Operation
        registerNative(map, new Replace());
        registerNative(map, new ReverseString());
        registerNative(map, new Substring2());
        registerNative(map, new Substring3());
        registerNative(map, new ToLower());
        registerNative(map, new ToUpper());
        //  Parse
        registerNative(map, new ParseBoolean());
        registerNative(map, new ParseDate());
        registerNative(map, new ParseFloat());
        registerNative(map, new ParseDecimal());
        registerNative(map, new ParseDecimalWithScalePrecision());
        registerNative(map, new ParseInteger());
        //  Split
        registerNative(map, new Split());
        //  toString
        registerNative(map, new Format());
        registerNative(map, new ToString());
        //  Trim
        registerNative(map, new LTrim());
        registerNative(map, new RTrim());
        registerNative(map, new Trim());

        //Tests
        registerNative(map, new Assert());
        registerNative(map, new AssertError());
    }
}
