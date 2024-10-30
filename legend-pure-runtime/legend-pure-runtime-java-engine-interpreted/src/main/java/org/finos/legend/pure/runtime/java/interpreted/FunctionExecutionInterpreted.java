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

package org.finos.legend.pure.runtime.java.interpreted;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinitionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.NativeFunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.PropertyCoreInstanceWrapper;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.Console;
import org.finos.legend.pure.m3.execution.ExecutionPlatform;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.execution.OutputWriter;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.statelistener.ExecutionActivityListener;
import org.finos.legend.pure.m3.statelistener.VoidExecutionActivityListener;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext.VariableNameConflictException;
import org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtension;
import org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtensionLoader;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.anonymous.map.ConstructorForPairList;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.anonymous.map.GetIfAbsentPutWithKey;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.anonymous.map.GetMapStats;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.anonymous.map.GroupBy;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.anonymous.map.KeyValues;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.anonymous.map.Keys;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.anonymous.map.Put;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.anonymous.map.PutAllMaps;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.anonymous.map.PutAllPairs;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.anonymous.map.ReplaceAll;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.anonymous.map.Values;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.anonymous.tree.ReplaceTreeNode;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.index.At;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.index.IndexOf;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.iteration.Find;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.iteration.Fold;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.operation.Add;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.operation.Concatenate;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.operation.RemoveAllOptimized;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.operation.RemoveDuplicates;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.operation.Zip;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.order.Reverse;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.order.Sort;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.quantification.Exists;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.quantification.ForAll;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.slice.Drop;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.slice.Init;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.slice.Last;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.slice.Slice;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.slice.Tail;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.slice.Take;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.date.creation.NewDate;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.date.extract.DatePart;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.date.extract.DayOfMonth;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.date.extract.Hour;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.date.extract.Minute;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.date.extract.MonthNumber;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.date.extract.Second;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.date.extract.Year;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.date.has.HasDay;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.date.has.HasHour;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.date.has.HasMinute;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.date.has.HasMonth;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.date.has.HasSecond;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.date.has.HasSubsecond;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.date.has.HasSubsecondWithAtLeastPrecision;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.date.operation.AdjustDate;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.date.operation.DateDiff;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.io.Print;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.lang.cast.Cast;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.lang.cast.ToDecimal;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.lang.cast.ToFloat;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.lang.cast.ToMultiplicity;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.lang.creation.DynamicNew;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.lang.creation.DynamicNewGenericType;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.lang.eval.Evaluate;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.lang.eval.EvaluateAny;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.lang.eval.RawEvalProperty;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.lang.flow.If;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.lang.flow.Match;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.lang.unit.GetUnitValue;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.lang.unit.NewUnit;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.Random;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.exponential.Exp;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.exponential.Log;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.exponential.Log10;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.operation.Abs;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.operation.Mod;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.operation.Rem;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.operation.Sign;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.power.Cbrt;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.power.Power;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.power.Sqrt;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.round.Ceiling;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.round.Floor;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.round.Round;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.round.RoundWithScale;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.trigonometry.ArcCosine;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.trigonometry.ArcSine;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.trigonometry.ArcTangent;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.trigonometry.ArcTangent2;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.trigonometry.CoTangent;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.trigonometry.Cosine;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.trigonometry.Sine;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.math.trigonometry.Tangent;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.RemoveOverride;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.graph.ElementPath;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.graph.ElementToPath;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.graph.PathToElement;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.instance.Id;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.profile.Stereotype;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.profile.Tag;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.reflect.CanReactivateDynamically;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.reflect.Deactivate;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.reflect.EvaluateAndDeactivate;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.reflect.OpenVariableValues;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.reflect.Reactivate;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.source.SourceInformation;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.type.Generalizations;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.type.GenericType;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.type.InstanceOf;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.type.SubTypeOf;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.type._class.GenericTypeClass;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.type._enum.EnumName;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.type._enum.EnumValues;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string._boolean.Contains;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string._boolean.EndsWith;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string._boolean.StartsWith;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string.index.IndexOfString;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string.index.Length;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string.operation.Replace;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string.operation.ReverseString;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string.operation.SubString;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string.operation.ToLower;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string.operation.ToUpper;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string.parse.ParsePrimitiveBoolean;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string.parse.ParsePrimitiveDate;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string.parse.ParsePrimitiveDecimal;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string.parse.ParsePrimitiveFloat;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string.parse.ParsePrimitiveInteger;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string.split.Split;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string.toString.Format;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string.toString.ToString;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string.trim.LTrim;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string.trim.RTrim;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.string.trim.Trim;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.tests.Assert;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.tests.AssertError;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar._boolean.equality.Eq;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar._boolean.equality.Equal;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar._boolean.equality.Is;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar._boolean.inequality.LessThan;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar._boolean.inequality.LessThanOrEqualTo;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar._boolean.operation.And;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar._boolean.operation.Not;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar._boolean.operation.Or;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.collection.iteration.Filter;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.collection.iteration.Map;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.collection.size.IsEmpty;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.collection.size.Size;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.collection.slice.First;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.lang.Compare;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.lang.Let;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.lang._enum.ExtractEnumValue;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.lang.all.GetAll;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.lang.cast.ToOne;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.lang.cast.ToOneMany;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.lang.creation.Copy;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.lang.creation.New;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.math.operation.Divide;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.math.operation.DivideDecimal;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.math.operation.Minus;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.math.operation.Plus;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.math.operation.Times;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.math.sequence.Range;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.string.operation.JoinStrings;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;
import org.finos.legend.pure.runtime.java.interpreted.profiler.VoidProfiler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

@ExecutionPlatform(name = "Interpreted")
public class FunctionExecutionInterpreted implements FunctionExecution
{
    private static final int DEFAULT_MAX_SQL_ROWS = 200;

    private final AtomicBoolean cancelExecution = new AtomicBoolean(false);
    private final ExecutionActivityListener executionActivityListener;
    private PureRuntime runtime;

    private final Console console = new ConsoleInterpreted();

    private ProcessorSupport processorSupport;

    private RepositoryCodeStorage storage;
    private Message message;

    private MutableMap<String, NativeFunction> nativeFunctions;
    private final int maxSQLRows;
    private final MutableList<InterpretedExtension> extensions;

    public FunctionExecutionInterpreted()
    {
        this(VoidExecutionActivityListener.VOID_EXECUTION_ACTIVITY_LISTENER);
    }

    public FunctionExecutionInterpreted(ExecutionActivityListener executionActivityListener)
    {
        this(DEFAULT_MAX_SQL_ROWS, executionActivityListener);
    }

    public FunctionExecutionInterpreted(int maxSQLRows, ExecutionActivityListener executionActivityListener)
    {
        this.maxSQLRows = Math.max(maxSQLRows, 0);
        this.executionActivityListener = executionActivityListener == null ? VoidExecutionActivityListener.VOID_EXECUTION_ACTIVITY_LISTENER : executionActivityListener;
        this.extensions = InterpretedExtensionLoader.extensions();
    }

    public void setProcessorSupport(M3ProcessorSupport processorSupport)
    {
        this.processorSupport = processorSupport;
        this.runtime.getIncrementalCompiler().setProcessorSupport(processorSupport);
    }

    @Override
    public void init(PureRuntime runtime, Message message)
    {
        this.runtime = runtime;

        this.processorSupport = new M3ProcessorSupport(this.runtime.getContext(), this.runtime.getModelRepository());

        this.nativeFunctions = Maps.mutable.empty();

        ModelRepository repository = runtime.getModelRepository();
        this.storage = runtime.getCodeStorage();
        this.message = message;


        registerGrammarNatives(repository);
        registerEssentialNatives(repository);


        this.extensions.asLazy().flatCollect(InterpretedExtension::getExtraNatives).forEach(extraNative -> this.nativeFunctions.put(extraNative.getOne(), extraNative.getTwo().apply(this, repository)));
    }

    private void registerGrammarNatives(ModelRepository repository)
    {
        // Boolean
        this.nativeFunctions.put("and_Boolean_1__Boolean_1__Boolean_1_", new And(repository, this));
        this.nativeFunctions.put("or_Boolean_1__Boolean_1__Boolean_1_", new Or(repository, this));
        this.nativeFunctions.put("not_Boolean_1__Boolean_1_", new Not(repository));
        this.nativeFunctions.put("eq_Any_1__Any_1__Boolean_1_", new Eq(repository));
        this.nativeFunctions.put("equal_Any_MANY__Any_MANY__Boolean_1_", new Equal(repository));
        this.nativeFunctions.put("is_Any_1__Any_1__Boolean_1_", new Is(repository));
        this.nativeFunctions.put("lessThan_Number_1__Number_1__Boolean_1_", new LessThan(repository));
        this.nativeFunctions.put("lessThanEqual_Number_1__Number_1__Boolean_1_", new LessThanOrEqualTo(repository));

        // Collection
        this.nativeFunctions.put("filter_T_MANY__Function_1__T_MANY_", new Filter(this));
        this.nativeFunctions.put("first_T_MANY__T_$0_1$_", new First());
        this.nativeFunctions.put("isEmpty_Any_MANY__Boolean_1_", new IsEmpty(repository));
        Map c = new Map(this);
        this.nativeFunctions.put("map_T_m__Function_1__V_m_", c);
        this.nativeFunctions.put("map_T_MANY__Function_1__V_MANY_", c);
        this.nativeFunctions.put("map_T_$0_1$__Function_1__V_$0_1$_", c);
        this.nativeFunctions.put("range_Integer_1__Integer_1__Integer_1__Integer_MANY_", new Range(repository));
        this.nativeFunctions.put("size_Any_MANY__Integer_1_", new Size(repository));

        // Lang
        this.nativeFunctions.put("compare_T_1__T_1__Integer_1_", new Compare(repository));
        this.nativeFunctions.put("copy_T_1__String_1__KeyExpression_MANY__T_1_", new Copy(repository, this));
        this.nativeFunctions.put("copy_T_1__String_1__T_1_", new Copy(repository, this));
        this.nativeFunctions.put("extractEnumValue_Enumeration_1__String_1__T_1_", new ExtractEnumValue());
        this.nativeFunctions.put("getAll_Class_1__T_MANY_", new GetAll());
        this.nativeFunctions.put("getAllVersions_Class_1__T_MANY_", new GetAll());
        this.nativeFunctions.put("getAllVersionsInRange_Class_1__Date_1__Date_1__T_MANY_", new GetAll());
        this.nativeFunctions.put("getAll_Class_1__Date_1__T_MANY_", new GetAll());
        this.nativeFunctions.put("getAll_Class_1__Date_1__Date_1__T_MANY_", new GetAll());
        this.nativeFunctions.put("deepFetchGetAll_Class_1__DeepFetchTempTable_1__T_MANY_", new GetAll());
        this.nativeFunctions.put("letFunction_String_1__T_m__T_m_", new Let());
        this.nativeFunctions.put("new_Class_1__String_1__KeyExpression_MANY__T_1_", new New(repository, this));
        this.nativeFunctions.put("new_Class_1__String_1__T_1_", new New(repository, this));

        // Math
        this.nativeFunctions.put("divide_Number_1__Number_1__Float_1_", new Divide(repository));
        this.nativeFunctions.put("divide_Decimal_1__Decimal_1__Integer_1__Decimal_1_", new DivideDecimal(repository));
        Minus minus = new Minus(repository);
        this.nativeFunctions.put("minus_Integer_MANY__Integer_1_", minus);
        this.nativeFunctions.put("minus_Float_MANY__Float_1_", minus);
        this.nativeFunctions.put("minus_Decimal_MANY__Decimal_1_", minus);
        this.nativeFunctions.put("minus_Number_MANY__Number_1_", minus);
        Plus plus = new Plus(repository);
        this.nativeFunctions.put("plus_Integer_MANY__Integer_1_", plus);
        this.nativeFunctions.put("plus_Float_MANY__Float_1_", plus);
        this.nativeFunctions.put("plus_Decimal_MANY__Decimal_1_", plus);
        this.nativeFunctions.put("plus_Number_MANY__Number_1_", plus);
        Times times = new Times(repository);
        this.nativeFunctions.put("times_Integer_MANY__Integer_1_", times);
        this.nativeFunctions.put("times_Float_MANY__Float_1_", times);
        this.nativeFunctions.put("times_Decimal_MANY__Decimal_1_", times);
        this.nativeFunctions.put("times_Number_MANY__Number_1_", times);

        // Multiplicity
        this.nativeFunctions.put("toOne_T_MANY__T_1_", new ToOne(repository));
        this.nativeFunctions.put("toOne_T_MANY__String_1__T_1_", new ToOne(repository));
        this.nativeFunctions.put("toOneMany_T_MANY__T_$1_MANY$_", new ToOneMany(repository));
        this.nativeFunctions.put("toOneMany_T_MANY__String_1__T_$1_MANY$_", new ToOneMany(repository));

        // String
        this.nativeFunctions.put("joinStrings_String_MANY__String_1__String_1__String_1__String_1_", new JoinStrings(repository));
    }


    private void registerEssentialNatives(ModelRepository repository)
    {
        //Collection
        //  Anonymous
        //    Map  
        this.nativeFunctions.put("newMap_Pair_MANY__Map_1_", new ConstructorForPairList(this, repository));
        this.nativeFunctions.put("newMap_Pair_MANY__Property_MANY__Map_1_", new ConstructorForPairList(this, repository));
        this.nativeFunctions.put("get_Map_1__U_1__V_$0_1$_", new org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.anonymous.map.Get(this, repository));
        this.nativeFunctions.put("getIfAbsentPutWithKey_Map_1__U_1__Function_1__V_$0_1$_", new GetIfAbsentPutWithKey(this, repository));
        this.nativeFunctions.put("getMapStats_Map_1__MapStats_$0_1$_", new GetMapStats(this, repository));
        this.nativeFunctions.put("keys_Map_1__U_MANY_", new Keys(this, repository));
        this.nativeFunctions.put("keyValues_Map_1__Pair_MANY_", new KeyValues(this, repository));
        this.nativeFunctions.put("put_Map_1__U_1__V_1__Map_1_", new Put(this, repository));
        this.nativeFunctions.put("putAll_Map_1__Map_1__Map_1_", new PutAllMaps(this, repository));
        this.nativeFunctions.put("putAll_Map_1__Pair_MANY__Map_1_", new PutAllPairs(this, repository));
        this.nativeFunctions.put("replaceAll_Map_1__Pair_MANY__Map_1_", new ReplaceAll(this, repository));
        this.nativeFunctions.put("values_Map_1__V_MANY_", new Values(this, repository));
        this.nativeFunctions.put("groupBy_X_MANY__Function_1__Map_1_", new GroupBy(this, repository));

        //    Tree
        this.nativeFunctions.put("replaceTreeNode_TreeNode_1__TreeNode_1__TreeNode_1__TreeNode_1_", new ReplaceTreeNode(this, repository));
        //  Index
        this.nativeFunctions.put("at_T_MANY__Integer_1__T_1_", new At());
        this.nativeFunctions.put("indexOf_T_MANY__T_1__Integer_1_", new IndexOf(this, repository));
        //  Iteration
        this.nativeFunctions.put("find_T_MANY__Function_1__T_$0_1$_", new Find(this, repository));
        this.nativeFunctions.put("fold_T_MANY__Function_1__V_m__V_m_", new Fold(this));
        //  Operation
        this.nativeFunctions.put("add_T_MANY__T_1__T_$1_MANY$_", new Add());
        this.nativeFunctions.put("add_T_MANY__Integer_1__T_1__T_$1_MANY$_", new Add());
        this.nativeFunctions.put("concatenate_T_MANY__T_MANY__T_MANY_", new Concatenate(this, repository));
        this.nativeFunctions.put("removeAllOptimized_T_MANY__T_MANY__T_MANY_", new RemoveAllOptimized(this, repository));
        this.nativeFunctions.put("removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_", new RemoveDuplicates(this));
        this.nativeFunctions.put("zip_T_MANY__U_MANY__Pair_MANY_", new Zip(this, repository));
        //  Order
        this.nativeFunctions.put("reverse_T_m__T_m_", new Reverse(this, repository));
        this.nativeFunctions.put("sort_T_m__Function_$0_1$__Function_$0_1$__T_m_", new Sort(this));
        //  Quantification
        this.nativeFunctions.put("exists_T_MANY__Function_1__Boolean_1_", new Exists(this, repository));
        this.nativeFunctions.put("forAll_T_MANY__Function_1__Boolean_1_", new ForAll(this, repository));
        //  Slice
        this.nativeFunctions.put("drop_T_MANY__Integer_1__T_MANY_", new Drop(this, repository));
        this.nativeFunctions.put("init_T_MANY__T_MANY_", new Init());
        this.nativeFunctions.put("last_T_MANY__T_$0_1$_", new Last(this, repository));
        this.nativeFunctions.put("slice_T_MANY__Integer_1__Integer_1__T_MANY_", new Slice(this, repository));
        this.nativeFunctions.put("tail_T_MANY__T_MANY_", new Tail());
        this.nativeFunctions.put("take_T_MANY__Integer_1__T_MANY_", new Take(this, repository));

        //Date
        //  Creation
        this.nativeFunctions.put("date_Integer_1__Date_1_", new NewDate(this, repository));
        this.nativeFunctions.put("date_Integer_1__Integer_1__Date_1_", new NewDate(this, repository));
        this.nativeFunctions.put("date_Integer_1__Integer_1__Integer_1__StrictDate_1_", new NewDate(this, repository));
        this.nativeFunctions.put("date_Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_", new NewDate(this, repository));
        this.nativeFunctions.put("date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_", new NewDate(this, repository));
        this.nativeFunctions.put("date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__Number_1__DateTime_1_", new NewDate(this, repository));
        //  Extract
        this.nativeFunctions.put("datePart_Date_1__Date_1_", new DatePart(this, repository));
        this.nativeFunctions.put("dayOfMonth_Date_1__Integer_1_", new DayOfMonth(this, repository));
        this.nativeFunctions.put("hour_Date_1__Integer_1_", new Hour(this, repository));
        this.nativeFunctions.put("minute_Date_1__Integer_1_", new Minute(this, repository));
        this.nativeFunctions.put("monthNumber_Date_1__Integer_1_", new MonthNumber(this, repository));
        this.nativeFunctions.put("second_Date_1__Integer_1_", new Second(this, repository));
        this.nativeFunctions.put("year_Date_1__Integer_1_", new Year(this, repository));
        //  Has
        this.nativeFunctions.put("hasDay_Date_1__Boolean_1_", new HasDay(this, repository));
        this.nativeFunctions.put("hasHour_Date_1__Boolean_1_", new HasHour(this, repository));
        this.nativeFunctions.put("hasMinute_Date_1__Boolean_1_", new HasMinute(this, repository));
        this.nativeFunctions.put("hasMonth_Date_1__Boolean_1_", new HasMonth(this, repository));
        this.nativeFunctions.put("hasSecond_Date_1__Boolean_1_", new HasSecond(this, repository));
        this.nativeFunctions.put("hasSubsecond_Date_1__Boolean_1_", new HasSubsecond(this, repository));
        this.nativeFunctions.put("hasSubsecondWithAtLeastPrecision_Date_1__Integer_1__Boolean_1_", new HasSubsecondWithAtLeastPrecision(this, repository));
        //  Operation
        this.nativeFunctions.put("adjust_Date_1__Integer_1__DurationUnit_1__Date_1_", new AdjustDate(this, repository));
        this.nativeFunctions.put("dateDiff_Date_1__Date_1__DurationUnit_1__Integer_1_", new DateDiff(this, repository));

        //IO
        this.nativeFunctions.put("print_Any_MANY__Integer_1__Nil_0_", new Print(this, repository));

        //Lang
        this.nativeFunctions.put("removeOverride_T_1__T_1_", new RemoveOverride(this, repository));
        //  Cast
        this.nativeFunctions.put("cast_Any_m__T_1__T_m_", new Cast(this, repository));
        this.nativeFunctions.put("toDecimal_Number_1__Decimal_1_", new ToDecimal(repository));
        this.nativeFunctions.put("toFloat_Number_1__Float_1_", new ToFloat(repository));
        this.nativeFunctions.put("toMultiplicity_T_MANY__Any_z__T_z_", new ToMultiplicity(this, repository));
        //  Creation
        this.nativeFunctions.put("dynamicNew_Class_1__KeyValue_MANY__Any_1_", new DynamicNew(this, repository));
        this.nativeFunctions.put("dynamicNew_GenericType_1__KeyValue_MANY__Any_1_", new DynamicNewGenericType(this, repository));
        this.nativeFunctions.put("dynamicNew_Class_1__KeyValue_MANY__Function_$0_1$__Function_$0_1$__Any_$0_1$__Any_1_", new DynamicNew(this, repository));
        this.nativeFunctions.put("dynamicNew_GenericType_1__KeyValue_MANY__Function_$0_1$__Function_$0_1$__Any_$0_1$__Any_1_", new DynamicNewGenericType(this, repository));
        this.nativeFunctions.put("dynamicNew_Class_1__KeyValue_MANY__Function_$0_1$__Function_$0_1$__Any_$0_1$__Function_$0_1$__Any_1_", new DynamicNew(this, repository));
        this.nativeFunctions.put("dynamicNew_GenericType_1__KeyValue_MANY__Function_$0_1$__Function_$0_1$__Any_$0_1$__Function_$0_1$__Any_1_", new DynamicNewGenericType(this, repository));
        //  Eval
        this.nativeFunctions.put("eval_Function_1__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__T_n__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__T_n__U_p__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__T_n__U_p__W_q__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__T_n__U_p__W_q__X_r__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__T_n__U_p__W_q__X_r__Y_s__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__T_n__U_p__W_q__X_r__Y_s__Z_t__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__S_n__T_o__U_p__W_q__X_r__Y_s__Z_t__V_m_", new Evaluate(this));
        this.nativeFunctions.put("evaluate_Function_1__List_MANY__Any_MANY_", new EvaluateAny(this, repository));
        this.nativeFunctions.put("rawEvalProperty_Property_1__Any_1__V_m_", new RawEvalProperty(this, repository));
        //  Flow
        this.nativeFunctions.put("if_Boolean_1__Function_1__Function_1__T_m_", new If(this));
        this.nativeFunctions.put("match_Any_MANY__Function_$1_MANY$__T_m_", new Match(this, repository));
        this.nativeFunctions.put("match_Any_MANY__Function_$1_MANY$__P_o__T_m_", new Match(this, repository));
        //  Unit
        this.nativeFunctions.put("getUnitValue_Any_1__Number_1_", new GetUnitValue());
        this.nativeFunctions.put("newUnit_Unit_1__Number_1__Any_1_", new NewUnit(repository));

        // Math
        this.nativeFunctions.put("random__Float_1_", new Random(this, repository));
        //  Exponential
        this.nativeFunctions.put("exp_Number_1__Float_1_", new Exp(this, repository));
        this.nativeFunctions.put("log_Number_1__Float_1_", new Log(this, repository));
        this.nativeFunctions.put("log10_Number_1__Float_1_", new Log10(this, repository));
        //  Operation
        Abs abs = new Abs(repository);
        this.nativeFunctions.put("abs_Integer_1__Integer_1_", abs);
        this.nativeFunctions.put("abs_Float_1__Float_1_", abs);
        this.nativeFunctions.put("abs_Decimal_1__Decimal_1_", abs);
        this.nativeFunctions.put("abs_Number_1__Number_1_", abs);
        this.nativeFunctions.put("mod_Integer_1__Integer_1__Integer_1_", new Mod(this, repository));
        this.nativeFunctions.put("rem_Number_1__Number_1__Number_1_", new Rem(repository));
        this.nativeFunctions.put("sign_Number_1__Integer_1_", new Sign(this, repository));
        //  Power
        this.nativeFunctions.put("cbrt_Number_1__Float_1_", new Cbrt(this, repository));
        this.nativeFunctions.put("pow_Number_1__Number_1__Number_1_", new Power(this, repository));
        this.nativeFunctions.put("sqrt_Number_1__Float_1_", new Sqrt(this, repository));
        //  Round
        this.nativeFunctions.put("ceiling_Number_1__Integer_1_", new Ceiling(this, repository));
        this.nativeFunctions.put("floor_Number_1__Integer_1_", new Floor(this, repository));
        this.nativeFunctions.put("round_Number_1__Integer_1_", new Round(this, repository));
        this.nativeFunctions.put("round_Decimal_1__Integer_1__Decimal_1_", new RoundWithScale(this, repository));
        this.nativeFunctions.put("round_Float_1__Integer_1__Float_1_", new RoundWithScale(this, repository));
        //  Trigonometry
        this.nativeFunctions.put("acos_Number_1__Float_1_", new ArcCosine(this, repository));
        this.nativeFunctions.put("asin_Number_1__Float_1_", new ArcSine(this, repository));
        this.nativeFunctions.put("atan_Number_1__Float_1_", new ArcTangent(this, repository));
        this.nativeFunctions.put("atan2_Number_1__Number_1__Float_1_", new ArcTangent2(this, repository));
        this.nativeFunctions.put("cos_Number_1__Float_1_", new Cosine(this, repository));
        this.nativeFunctions.put("cot_Number_1__Float_1_", new CoTangent(this, repository));
        this.nativeFunctions.put("tan_Number_1__Float_1_", new Tangent(this, repository));
        this.nativeFunctions.put("sin_Number_1__Float_1_", new Sine(this, repository));

        //Meta
        //  Graph
        this.nativeFunctions.put("elementPath_PackageableElement_1__PackageableElement_$1_MANY$_", new ElementPath());
        this.nativeFunctions.put("elementToPath_PackageableElement_1__String_1__Boolean_1__String_1_", new ElementToPath(repository));
        this.nativeFunctions.put("pathToElement_String_1__String_1__PackageableElement_1_", new PathToElement(false));
        this.nativeFunctions.put("lenientPathToElement_String_1__String_1__PackageableElement_$0_1$_", new PathToElement(true));
        //  Instance
        this.nativeFunctions.put("id_Any_1__String_1_", new Id(repository));
        //  Profile
        this.nativeFunctions.put("stereotype_Profile_1__String_1__Stereotype_1_", new Stereotype(this, repository));
        this.nativeFunctions.put("tag_Profile_1__String_1__Tag_1_", new Tag(this, repository));
        //  Reflect
        this.nativeFunctions.put("canReactivateDynamically_ValueSpecification_1__Boolean_1_", new CanReactivateDynamically(this, repository));
        this.nativeFunctions.put("deactivate_Any_MANY__ValueSpecification_1_", new Deactivate(this, repository));
        this.nativeFunctions.put("evaluateAndDeactivate_T_m__T_m_", new EvaluateAndDeactivate());
        this.nativeFunctions.put("openVariableValues_Function_1__Map_1_", new OpenVariableValues(this, repository));
        this.nativeFunctions.put("reactivate_ValueSpecification_1__Map_1__Any_MANY_", new Reactivate(this, repository));
        //  Source
        this.nativeFunctions.put("sourceInformation_Any_1__SourceInformation_$0_1$_", new SourceInformation(this, repository));
        //  Type
        this.nativeFunctions.put("genericType_Any_MANY__GenericType_1_", new GenericType());
        this.nativeFunctions.put("instanceOf_Any_1__Type_1__Boolean_1_", new InstanceOf(repository));
        this.nativeFunctions.put("generalizations_Type_1__Type_$1_MANY$_", new Generalizations(this, repository));
        this.nativeFunctions.put("subTypeOf_Type_1__Type_1__Boolean_1_", new SubTypeOf(this, repository));
        //    Class
        this.nativeFunctions.put("genericTypeClass_GenericType_1__Class_1_", new GenericTypeClass(repository));
        //    Enum
        this.nativeFunctions.put("enumName_Enumeration_1__String_1_", new EnumName(this, repository));
        this.nativeFunctions.put("enumValues_Enumeration_1__T_MANY_", new EnumValues(this, repository));

        // String
        //  Boolean
        this.nativeFunctions.put("contains_String_1__String_1__Boolean_1_", new Contains(this, repository));
        this.nativeFunctions.put("endsWith_String_1__String_1__Boolean_1_", new EndsWith(this, repository));
        this.nativeFunctions.put("startsWith_String_1__String_1__Boolean_1_", new StartsWith(repository));
        //  Index
        this.nativeFunctions.put("indexOf_String_1__String_1__Integer_1_", new IndexOfString(this, repository));
        this.nativeFunctions.put("indexOf_String_1__String_1__Integer_1__Integer_1_", new IndexOfString(this, repository));
        this.nativeFunctions.put("length_String_1__Integer_1_", new Length(repository));
        //  Operation
        this.nativeFunctions.put("toLower_String_1__String_1_", new ToLower(this, repository));
        this.nativeFunctions.put("toUpper_String_1__String_1_", new ToUpper(this, repository));
        this.nativeFunctions.put("replace_String_1__String_1__String_1__String_1_", new Replace(repository));
        this.nativeFunctions.put("reverseString_String_1__String_1_", new ReverseString(this, repository));
        SubString substring = new SubString(repository);
        this.nativeFunctions.put("substring_String_1__Integer_1__String_1_", substring);
        this.nativeFunctions.put("substring_String_1__Integer_1__Integer_1__String_1_", substring);
        //  Parse
        this.nativeFunctions.put("parseBoolean_String_1__Boolean_1_", new ParsePrimitiveBoolean(this, repository));
        this.nativeFunctions.put("parseDate_String_1__Date_1_", new ParsePrimitiveDate(this, repository));
        this.nativeFunctions.put("parseFloat_String_1__Float_1_", new ParsePrimitiveFloat(this, repository));
        this.nativeFunctions.put("parseDecimal_String_1__Decimal_1_", new ParsePrimitiveDecimal(this, repository));
        this.nativeFunctions.put("parseInteger_String_1__Integer_1_", new ParsePrimitiveInteger(this, repository));
        //  Split
        this.nativeFunctions.put("split_String_1__String_1__String_MANY_", new Split(repository));
        //  ToString
        this.nativeFunctions.put("format_String_1__Any_MANY__String_1_", new Format(repository, this));
        this.nativeFunctions.put("toString_Any_1__String_1_", new ToString(repository, this));
        //  Trim
        this.nativeFunctions.put("ltrim_String_1__String_1_", new LTrim(this, repository));
        this.nativeFunctions.put("rtrim_String_1__String_1_", new RTrim(this, repository));
        this.nativeFunctions.put("trim_String_1__String_1_", new Trim(this, repository));

        // Tests
        this.nativeFunctions.put("assert_Boolean_1__Function_1__Boolean_1_", new Assert(this));
        this.nativeFunctions.put("assertError_Function_1__String_1__Integer_$0_1$__Integer_$0_1$__Boolean_1_", new AssertError(this, repository));
    }

    public RepositoryCodeStorage getStorage()
    {
        return this.storage;
    }

    public Message getMessage()
    {
        return this.message;
    }

    public int getMaxSQLRows()
    {
        return this.maxSQLRows;
    }

    public ExecutionActivityListener getExecutionActivityListener()
    {
        return this.executionActivityListener;
    }

    public void addNativeFunction(String signature, NativeFunction function)
    {
        this.nativeFunctions.put(signature, function);
    }

    @Override
    public CoreInstance start(CoreInstance function, ListIterable<? extends CoreInstance> arguments)
    {
        this.cancelExecution.set(false);
        Exception isException = null;
        ExecutionSupport executionSupport = new ExecutionSupport();
        try
        {
            CoreInstance result = this.executeFunction(false, FunctionCoreInstanceWrapper.toFunction(function), arguments, new Stack<>(), new Stack<>(), VariableContext.newVariableContext(), Stacks.mutable.empty(), VoidProfiler.VOID_PROFILER, new InstantiationContext(), executionSupport);
            return result;
        }
        catch (Exception ex)
        {
            isException = ex;
            throw ex;
        }
        finally
        {
            executionSupport.executionEnd(isException);
        }
    }

    @Override
    public void start(CoreInstance func, ListIterable<? extends CoreInstance> arguments, OutputStream outputStream, OutputWriter writer)
    {
        CoreInstance result = this.start(func, arguments);

        try
        {
            ListIterable<? extends CoreInstance> values = result.getValueForMetaPropertyToMany(M3Properties.values);
            writer.write(values, outputStream);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Failed to write to output stream", e);
        }
    }

    @Override
    public Console getConsole()
    {
        return console;
    }

    public PureRuntime getPureRuntime()
    {
        return this.runtime;
    }

    NativeFunction getNativeFunction(String functionName)
    {
        return this.nativeFunctions.get(functionName);
    }

    public String printStack(Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, ProcessorSupport processorSupport)
    {
        int size = resolvedTypeParameters.size();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++)
        {
            builder.append(i).append(" :");
            int startLen = builder.length();
            resolvedTypeParameters.get(i).forEachKeyValue((key, value) ->
            {
                ((builder.length() > startLen) ? builder.append(", ") : builder).append(key).append('=');
                org.finos.legend.pure.m3.navigation.generictype.GenericType.print(builder, value, processorSupport);
            });
            builder.append('\n');
        }
        return builder.toString();
    }


    public CoreInstance executeLambda(LambdaFunction<?> function, ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext context, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport)
    {
        if (function instanceof LambdaWithContext)
        {
            context = ((LambdaWithContext) function).getVariableContext();
        }

        return this.executeFunctionExecuteParams(function, params, resolvedTypeParameters, resolvedMultiplicityParameters, context, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
    }

    /**
     * In this case the parameters have already been evaluated so no need to re-evaluate
     */
    public CoreInstance executeLambdaFromNative(CoreInstance function, ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext context, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport)
    {
        if (function instanceof LambdaWithContext)
        {
            context = ((LambdaWithContext) function).getVariableContext();
        }

        return this.executeFunction(false, LambdaFunctionCoreInstanceWrapper.toLambdaFunction(function), params, resolvedTypeParameters, resolvedMultiplicityParameters, context, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
    }

    public void cancelExecution()
    {
        this.cancelExecution.set(true);
    }

    public CoreInstance executeFunctionExecuteParams(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> function, ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext context, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport)
    {
        if (params.notEmpty())
        {
            ProcessorSupport processorSupport = this.getProcessorSupport();
            MutableList<CoreInstance> parameters = Lists.mutable.ofInitialCapacity(params.size());
            for (CoreInstance instance : params)
            {
                Executor executor = findValueSpecificationExecutor(instance, functionExpressionCallStack, processorSupport, this);
                parameters.add(executor.execute(instance, resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, context, profiler, instantiationContext, executionSupport, this, processorSupport));
            }
            params = parameters;
        }
        return this.executeFunction(false, function, params, resolvedTypeParameters, resolvedMultiplicityParameters, context, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
    }

    public CoreInstance executeFunction(boolean limitScope, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> function, ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext varContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport)
    {
        try
        {
            if (this.cancelExecution.compareAndSet(true, false))
            {
                throw new PureExecutionException("Cancelled!", functionExpressionCallStack);
            }

            ProcessorSupport processorSupport = this.runtime.getProcessorSupport();
            ListIterable<? extends CoreInstance> signatureVars = Instance.getValueForMetaPropertyToManyResolved(processorSupport.function_getFunctionType(function), M3Properties.parameters, processorSupport);
            if (signatureVars.size() != params.size())
            {
                StringBuilder builder = new StringBuilder();
                if (function._functionName() != null)
                {
                    Function.print(builder, function, processorSupport);
                }
                String message = "Error executing the function:" + builder + ". Mismatch between the number of function parameters (" + signatureVars.size() + ") and the number of supplied arguments (" + params.size() + ")\n" + params.collect(i -> i.printWithoutDebug("", 3)).makeString("\n");
                throw new PureExecutionException(functionExpressionCallStack.isEmpty() ? null : functionExpressionCallStack.peek().getSourceInformation(), message, functionExpressionCallStack);
            }

            VariableContext variableContext = this.moveParametersIntoVariableContext(varContext, signatureVars, params, functionExpressionCallStack);
            if (limitScope)
            {
                variableContext.markVariableScopeBoundary();
            }

            if (function instanceof PackageableFunction)
            {
                for (CoreInstance constraint : ((PackageableFunction<?>) function)._preConstraints())
                {
                    CoreInstance definition = Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.functionDefinition, processorSupport), M3Properties.expressionSequence, processorSupport);
                    String ruleId = Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.name, processorSupport).getName();
                    CoreInstance evaluatedConstraint = this.executeValueSpecification(definition, new Stack<>(), new Stack<>(), Stacks.mutable.empty(), variableContext, VoidProfiler.VOID_PROFILER, instantiationContext, executionSupport);
                    if (!PrimitiveUtilities.getBooleanValue(evaluatedConstraint.getValueForMetaPropertyToOne(M3Properties.values)))
                    {
                        throw new PureExecutionException(functionExpressionCallStack.isEmpty() ? null : functionExpressionCallStack.peek().getSourceInformation(), "Constraint (PRE):[" + ruleId + "] violated. (Function:" + function.getName() + ")", functionExpressionCallStack);
                    }
                }
            }

            // Execute
            CoreInstance result = null;
            if (Instance.instanceOf(function, M3Paths.NativeFunction, processorSupport))
            {
                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.NativeFunction<?> function1 = NativeFunctionCoreInstanceWrapper.toNativeFunction(function);
                NativeFunction nativeFunction = this.nativeFunctions.get(function1.getName());
                if (nativeFunction == null)
                {
                    throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), "The function '" + function1.getName() + "' is not supported by this execution platform", functionExpressionCallStack);
                }
                result = nativeFunction.execute(params, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport, this.runtime.getContext(), this.runtime.getProcessorSupport());
            }
            else if (org.finos.legend.pure.m3.navigation.property.Property.isProperty(function, processorSupport))
            {
                result = this.executeProperty(PropertyCoreInstanceWrapper.toProperty(function), true, resolvedTypeParameters, resolvedMultiplicityParameters, varContext, profiler, params, functionExpressionCallStack, instantiationContext, executionSupport);
            }
            //Qualified properties also go here
            else if (Instance.instanceOf(function, M3Paths.FunctionDefinition, processorSupport))
            {
                RichIterable<? extends CoreInstance> expressions = FunctionDefinitionCoreInstanceWrapper.toFunctionDefinition(function)._expressionSequence();

                CoreInstance returnVal = null;
                for (CoreInstance expression : expressions)
                {
                    Executor executor = findValueSpecificationExecutor(expression, functionExpressionCallStack, processorSupport, this);
                    returnVal = executor.execute(expression, resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, variableContext, profiler, instantiationContext, executionSupport, this, processorSupport);
                }
                result = returnVal;
            }
            List<CoreInstance> instances = this.extensions.collect(x -> x.getExtraFunctionExecution(function, params, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, this)).select(Objects::nonNull);

            if (instances.size() == 1)
            {
                result = instances.get(0);
            }
            else if (result == null)
            {
                throw new PureExecutionException("Unsupported function for execution " + function.getName() + " of type " + PackageableElement.getUserPathForPackageableElement(function.getClassifier()) + " (class " + function.getClass().getName() + ")", functionExpressionCallStack);
            }

            if (function instanceof PackageableFunction)
            {
                if (((PackageableFunction<?>) function)._postConstraints().notEmpty())
                {
                    try
                    {
                        variableContext.registerValue("return", result);
                    }
                    catch (VariableNameConflictException e)
                    {
                        throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), e.getMessage(), e, functionExpressionCallStack);
                    }
                    for (CoreInstance constraint : ((PackageableFunction<?>) function)._postConstraints())
                    {

                        CoreInstance definition = Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.functionDefinition, processorSupport), M3Properties.expressionSequence, processorSupport);
                        String ruleId = Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.name, processorSupport).getName();
                        CoreInstance evaluatedConstraint = this.executeValueSpecification(definition, new Stack<>(), new Stack<>(), Stacks.mutable.empty(), variableContext, VoidProfiler.VOID_PROFILER, instantiationContext, executionSupport);

                        if (!PrimitiveUtilities.getBooleanValue(evaluatedConstraint.getValueForMetaPropertyToOne(M3Properties.values)))
                        {
                            throw new PureExecutionException(functionExpressionCallStack.isEmpty() ? null : functionExpressionCallStack.peek().getSourceInformation(), "Constraint (POST):[" + ruleId + "] violated. (Function:" + function.getName() + ")", functionExpressionCallStack);
                        }
                    }
                }
            }
            return result;
        }
        catch (PureAssertFailException e)
        {
            org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInfo = (functionExpressionCallStack.isEmpty() ? null : functionExpressionCallStack.peek().getSourceInformation());
            if (e.getSourceInformation() == null && sourceInfo != null)
            {
                String testPurePlatformFileName = "/platform/pure/essential/tests/";
                boolean allFromAssert = true;
                for (org.finos.legend.pure.m4.coreinstance.SourceInformation si : e.getPureStackSourceInformation())
                {
                    allFromAssert = allFromAssert && si != null && si.getSourceId().startsWith(testPurePlatformFileName);
                }

                if (allFromAssert && !sourceInfo.getSourceId().startsWith(testPurePlatformFileName))
                {
                    throw new PureAssertFailException(sourceInfo, e.getInfo(), functionExpressionCallStack);
                }
                else
                {
                    throw new PureAssertFailException(sourceInfo, e.getInfo(), e, functionExpressionCallStack);
                }
            }
            else
            {
                throw e;
            }
        }
        catch (PureException e)
        {
            if (!functionExpressionCallStack.isEmpty())
            {
                org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInfo = functionExpressionCallStack.peek().getSourceInformation();
                if (e.getSourceInformation() == null && sourceInfo != null)
                {
                    throw new PureExecutionException(sourceInfo, e.getInfo(), e, functionExpressionCallStack);
                }
            }
            throw e;
        }
        catch (RuntimeException e)
        {
            if (!functionExpressionCallStack.isEmpty())
            {
                org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInfo = functionExpressionCallStack.peek().getSourceInformation();
                PureException pureException = PureException.findPureException(e);
                if (pureException == null)
                {
                    if (sourceInfo != null)
                    {
                        throw new PureExecutionException(sourceInfo, e.getMessage(), e, functionExpressionCallStack);
                    }
                }
                else if (pureException.getSourceInformation() == null && sourceInfo != null)
                {
                    if (pureException instanceof PureAssertFailException)
                    {
                        throw new PureAssertFailException(sourceInfo, pureException.getInfo(), (PureAssertFailException) pureException, functionExpressionCallStack);
                    }
                    else
                    {
                        throw new PureExecutionException(sourceInfo, pureException.getInfo(), pureException, functionExpressionCallStack);
                    }
                }
                else
                {
                    throw pureException;
                }
            }
            throw e;
        }
    }

    public CoreInstance executeProperty(Property<?, ?> property, boolean route, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, Profiler profiler, ListIterable<? extends CoreInstance> parameters, MutableStack<CoreInstance> functionExpressionCallStack, InstantiationContext instantiationContext, ExecutionSupport executionSupport) throws PureExecutionException
    {
        ProcessorSupport processorSupport = this.runtime.getProcessorSupport();

        CoreInstance source = parameters.get(0);
        boolean executable = ValueSpecification.isExecutable(source, processorSupport);
        CoreInstance multiplicity = property._multiplicity();

        CoreInstance evaluatedSource = Instance.getValueForMetaPropertyToOneResolved(executable ? findValueSpecificationExecutor(source, Stacks.mutable.with((CoreInstance)property), processorSupport, this).execute(source, resolvedTypeParameters, resolvedMultiplicityParameters, Stacks.mutable.with(property), variableContext, profiler, instantiationContext, executionSupport, this, processorSupport) : source, M3Properties.values, processorSupport);

        if (evaluatedSource == null)
        {
            throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), "The system can't execute a property function '" + property._name() + "' on a null instance.", functionExpressionCallStack);
        }
        CoreInstance overrides = Instance.getValueForMetaPropertyToOneResolved(evaluatedSource, M3Properties.elementOverride, processorSupport);
        if (Multiplicity.isToOne(multiplicity, false))
        {
            CoreInstance funcToOne = overrides != null ? Instance.getValueForMetaPropertyToOneResolved(overrides, M3Properties.getterOverrideToOne, processorSupport) : null;
            if (route && funcToOne != null && !M3Properties.elementOverride.equals(property._name()) && !M3Properties.hiddenPayload.equals(property._name()) && !Instance.instanceOf(Instance.getValueForMetaPropertyToOneResolved(property._classifierGenericType().getValueForMetaPropertyToMany(M3Properties.typeArguments).get(1), M3Properties.rawType, processorSupport), M3Paths.DataType, processorSupport))
            {
                return this.executeFunction(true, FunctionCoreInstanceWrapper.toFunction(funcToOne), Lists.mutable.with(ValueSpecificationBootstrap.wrapValueSpecification(evaluatedSource, executable, processorSupport), ValueSpecificationBootstrap.wrapValueSpecification(property, executable, processorSupport)), resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
            }
            else
            {
                CoreInstance result = Instance.getValueForMetaPropertyToOneResolved(evaluatedSource, property, processorSupport);
                return ValueSpecificationBootstrap.wrapValueSpecification(result, executable, processorSupport);
            }
        }
        else
        {
            CoreInstance funcToMany = overrides != null ? Instance.getValueForMetaPropertyToOneResolved(overrides, M3Properties.getterOverrideToMany, processorSupport) : null;
            if (route && funcToMany != null && !M3Properties.hiddenPayload.equals(property._name()) && !Instance.instanceOf(Instance.getValueForMetaPropertyToOneResolved(property._classifierGenericType().getValueForMetaPropertyToMany(M3Properties.typeArguments).get(1), M3Properties.rawType, processorSupport), M3Paths.DataType, processorSupport))
            {
                return this.executeFunction(true, FunctionCoreInstanceWrapper.toFunction(funcToMany), Lists.mutable.with(ValueSpecificationBootstrap.wrapValueSpecification(evaluatedSource, executable, processorSupport), ValueSpecificationBootstrap.wrapValueSpecification(property, executable, processorSupport)), resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
            }
            else
            {
                ListIterable<? extends CoreInstance> result = Instance.getValueForMetaPropertyToManyResolved(evaluatedSource, property, processorSupport);
                return ValueSpecificationBootstrap.wrapValueSpecification_ForFunctionReturnValue(org.finos.legend.pure.m3.navigation.generictype.GenericType.resolvePropertyReturnType(Instance.extractGenericTypeFromInstance(evaluatedSource, processorSupport), property, processorSupport),
                        result, executable, processorSupport);
            }
        }
    }

    public CoreInstance executeValueSpecification(CoreInstance instance, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, MutableStack<CoreInstance> functionExpressionCallStack, VariableContext variableContext, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport) throws PureExecutionException
    {
        ProcessorSupport processorSupport = this.getProcessorSupport();
        Executor executor = findValueSpecificationExecutor(instance, functionExpressionCallStack, processorSupport, this);
        return executor.execute(instance, resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, variableContext, profiler, instantiationContext, executionSupport, this, processorSupport);
    }

    public static Executor findValueSpecificationExecutor(CoreInstance instance, MutableStack<CoreInstance> functionExpressionCallStack, ProcessorSupport processorSupport, FunctionExecutionInterpreted functionExecutionInterpreted) throws PureExecutionException
    {
        if (!ValueSpecification.isExecutable(instance, processorSupport))
        {
            return NonExecutableValueSpecificationExecutor.INSTANCE;
        }
        if (functionExecutionInterpreted.cancelExecution.compareAndSet(true, false))
        {
            throw new PureExecutionException("Execution cancelled!", functionExpressionCallStack);
        }
        if (Instance.instanceOf(instance, M3Paths.FunctionExpression, processorSupport))
        {
            return FunctionExpressionExecutor.INSTANCE;
        }
        if (Instance.instanceOf(instance, M3Paths.VariableExpression, processorSupport))
        {
            return VariableExpressionExecutor.INSTANCE;
        }
        if (Instance.instanceOf(instance, M3Paths.InstanceValue, processorSupport))
        {
            return InstanceValueExecutor.INSTANCE;
        }
        if (Instance.instanceOf(instance, M3Paths.ClusteredValueSpecification, processorSupport))
        {
            return ClusteredValueSpecificationExecutor.INSTANCE;
        }
        if (Instance.instanceOf(instance, M3Paths.RoutedValueSpecification, processorSupport))
        {
            return RoutedValueSpecificationExecutor.INSTANCE;
        }
        else
        {
            throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), "A new type (" + processorSupport.getClassifier(instance).getName() + ") must have been introduced in the ValueSpecification tree.", functionExpressionCallStack);
        }
    }

    private VariableContext moveParametersIntoVariableContext(VariableContext variableContext, ListIterable<? extends CoreInstance> signatureVars, ListIterable<? extends CoreInstance> parameters, MutableStack<CoreInstance> functionExpressionCallStack)
    {
        VariableContext newVarContext = VariableContext.newVariableContext(variableContext);
        try
        {
            ProcessorSupport processorSupport = this.runtime.getProcessorSupport();
            for (int i = 0, length = signatureVars.size(); i < length; i++)
            {
                CoreInstance varName = Instance.getValueForMetaPropertyToOneResolved(signatureVars.get(i), M3Properties.name, processorSupport);
                newVarContext.registerValue(varName == null ? "Unknown" : varName.getName(), parameters.get(i));
            }
        }
        catch (VariableNameConflictException e)
        {
            throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), e.getMessage(), e, functionExpressionCallStack);
        }
        return newVarContext;
    }

    @Override
    public boolean isFullyInitializedForExecution()
    {
        return this.runtime != null && this.runtime.isFullyInitialized();
    }

    @Override
    public void resetEventHandlers()
    {
    }

    @Override
    public ProcessorSupport getProcessorSupport()
    {
        return this.processorSupport;
    }

    @Override
    public PureRuntime getRuntime()
    {
        return this.runtime;
    }

    @Override
    public OutputWriter newOutputWriter()
    {
        return new OutputWriterInterpreted();
    }
}
