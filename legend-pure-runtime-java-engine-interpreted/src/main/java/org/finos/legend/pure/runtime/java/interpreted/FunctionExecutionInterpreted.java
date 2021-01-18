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
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.function.checked.CheckedFunction;
import org.eclipse.collections.impl.block.function.checked.CheckedFunction2;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.PropertyCoreInstanceWrapper;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.Console;
import org.finos.legend.pure.m3.execution.ExecutionPlatform;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.execution.OutputWriter;
import org.finos.legend.pure.m3.navigation.*;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
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
import org.finos.legend.pure.runtime.java.interpreted.natives.core.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.core._boolean.Eq;
import org.finos.legend.pure.runtime.java.interpreted.natives.core._boolean.Equal;
import org.finos.legend.pure.runtime.java.interpreted.natives.core._boolean.Is;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.asserts.Assert;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.cipher.Cipher;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.cipher.Decipher;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.collection.Get;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.collection.*;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.collection.map.*;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.date.*;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.hash.Hash;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.io.Http;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.io.Print;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.io.ReadFile;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.lang.*;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.math.*;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.math.trigonometry.*;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.meta.*;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.string.*;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.tools.Profile;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.tracing.TraceSpan;
import org.finos.legend.pure.runtime.java.interpreted.natives.legend.AlloyTest;
import org.finos.legend.pure.runtime.java.interpreted.natives.legend.LegendTest;
import org.finos.legend.pure.runtime.java.interpreted.natives.runtime.CurrentUserId;
import org.finos.legend.pure.runtime.java.interpreted.natives.runtime.Guid;
import org.finos.legend.pure.runtime.java.interpreted.natives.runtime.IsOptionSet;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;
import org.finos.legend.pure.runtime.java.interpreted.profiler.VoidProfiler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

@ExecutionPlatform(name = "Interpreted")
public class FunctionExecutionInterpreted implements FunctionExecution
{
    private static final int DEFAULT_MAX_SQL_ROWS = 200;

    private final AtomicBoolean cancelExecution = new AtomicBoolean(false);
    private final ExecutionActivityListener executionActivityListener;
    private PureRuntime runtime;

    private CodeStorage storage;
    private Message message;

    private MutableMap<String, NativeFunction> nativeFunctions;
    private final int maxSQLRows;
    private MutableList<InterpretedExtension> extensions;

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
        this.maxSQLRows = maxSQLRows < 0 ? 0 : maxSQLRows;
        this.executionActivityListener = executionActivityListener == null ? VoidExecutionActivityListener.VOID_EXECUTION_ACTIVITY_LISTENER : executionActivityListener;
        this.extensions = InterpretedExtensionLoader.extensions();
    }



    @Override
    public void init(PureRuntime runtime, Message message)
    {
        this.runtime = runtime;

        this.nativeFunctions = UnifiedMap.newMap();

        ModelRepository repository = runtime.getModelRepository();
        this.storage = runtime.getCodeStorage();
        this.message = message;

        this.nativeFunctions.put("replaceTreeNode_TreeNode_1__TreeNode_1__TreeNode_1__TreeNode_1_", new ReplaceTreeNode(repository));
        this.nativeFunctions.put("letFunction_String_1__T_m__T_m_", new Let());
        this.nativeFunctions.put("extractEnumValue_Enumeration_1__String_1__T_1_", new ExtractEnumValue());
        this.nativeFunctions.put("new_Class_1__String_1__KeyExpression_MANY__T_1_", new New(repository, this));
        this.nativeFunctions.put("new_Class_1__String_1__T_1_", new New(repository, this));
        this.nativeFunctions.put("newUnit_Unit_1__Number_1__Any_1_", new NewUnit(repository, this));
        this.nativeFunctions.put("getUnitValue_Any_1__Number_1_", new GetUnitValue(repository, this));
        this.nativeFunctions.put("dynamicNew_Class_1__KeyValue_MANY__Any_1_", new DynamicNew(repository, false, this));
        this.nativeFunctions.put("dynamicNew_GenericType_1__KeyValue_MANY__Any_1_", new DynamicNew(repository, true, this));
        this.nativeFunctions.put("dynamicNew_Class_1__KeyValue_MANY__Function_$0_1$__Function_$0_1$__Any_$0_1$__Any_1_", new DynamicNew(repository, false, this));
        this.nativeFunctions.put("dynamicNew_GenericType_1__KeyValue_MANY__Function_$0_1$__Function_$0_1$__Any_$0_1$__Any_1_", new DynamicNew(repository, true, this));
        this.nativeFunctions.put("dynamicNew_Class_1__KeyValue_MANY__Function_$0_1$__Function_$0_1$__Any_$0_1$__Function_$0_1$__Any_1_", new DynamicNew(repository, false, this));
        this.nativeFunctions.put("dynamicNew_GenericType_1__KeyValue_MANY__Function_$0_1$__Function_$0_1$__Any_$0_1$__Function_$0_1$__Any_1_", new DynamicNew(repository, true, this));
        this.nativeFunctions.put("removeOverride_T_1__T_1_", new RemoveOverride());
        this.nativeFunctions.put("get_T_MANY__String_1__T_$0_1$_", new Get());
        this.nativeFunctions.put("at_T_MANY__Integer_1__T_1_", new At());
        this.nativeFunctions.put("filter_T_MANY__Function_1__T_MANY_", new Filter(this));

        Map c = new Map(this);
        this.nativeFunctions.put("map_T_m__Function_1__V_m_", c);
        this.nativeFunctions.put("map_T_MANY__Function_1__V_MANY_", c);
        this.nativeFunctions.put("map_T_$0_1$__Function_1__V_$0_1$_", c);

        this.nativeFunctions.put("exists_T_MANY__Function_1__Boolean_1_", new Exists(repository, this));
        this.nativeFunctions.put("forAll_T_MANY__Function_1__Boolean_1_", new ForAll(repository, this));
        this.nativeFunctions.put("isEmpty_Any_MANY__Boolean_1_", new IsEmpty(repository));
        this.nativeFunctions.put("fold_T_MANY__Function_1__V_m__V_m_", new Fold(this));
        this.nativeFunctions.put("getAll_Class_1__T_MANY_", new GetAll());
        this.nativeFunctions.put("getAllVersions_Class_1__T_MANY_", new GetAll());
        this.nativeFunctions.put("getAllVersionsInRange_Class_1__Date_1__Date_1__T_MANY_", new GetAll());
        this.nativeFunctions.put("getAll_Class_1__Date_1__T_MANY_", new GetAll());
        this.nativeFunctions.put("getAll_Class_1__Date_1__Date_1__T_MANY_", new GetAll());
        this.nativeFunctions.put("deepFetchGetAll_Class_1__DeepFetchTempTable_1__T_MANY_", new GetAll());
        this.nativeFunctions.put("indexOf_T_MANY__T_1__Integer_1_", new IndexOf(repository));
        this.nativeFunctions.put("add_T_MANY__T_1__T_$1_MANY$_", new Add());
        this.nativeFunctions.put("add_T_MANY__Integer_1__T_1__T_$1_MANY$_", new Add());
        this.nativeFunctions.put("concatenate_T_MANY__T_MANY__T_MANY_", new Concatenate());
        this.nativeFunctions.put("repeat_T_1__Integer_1__T_MANY_", new Repeat());
        this.nativeFunctions.put("first_T_MANY__T_$0_1$_", new First());
        this.nativeFunctions.put("last_T_MANY__T_$0_1$_", new Last());
        this.nativeFunctions.put("init_T_MANY__T_MANY_", new Init());
        this.nativeFunctions.put("tail_T_MANY__T_MANY_", new Tail());
        this.nativeFunctions.put("chunk_String_1__Integer_1__String_MANY_", new Chunk(repository));
        this.nativeFunctions.put("take_T_MANY__Integer_1__T_MANY_", new Take());
        this.nativeFunctions.put("drop_T_MANY__Integer_1__T_MANY_", new Drop());
        this.nativeFunctions.put("slice_T_MANY__Integer_1__Integer_1__T_MANY_", new Slice());
        this.nativeFunctions.put("zip_T_MANY__U_MANY__Pair_MANY_", new Zip(repository));

        Print p = new Print(repository);
        this.nativeFunctions.put("print_Any_MANY__Integer_1__Nil_0_", p);

        this.nativeFunctions.put("executeHTTPRaw_URL_1__HTTPMethod_1__String_$0_1$__String_$0_1$__HTTPResponse_1_", new Http(repository));

        TraceSpan traceSpan = new TraceSpan(this);
        this.nativeFunctions.put("traceSpan_Function_1__String_1__V_m_", traceSpan);
        this.nativeFunctions.put("traceSpan_Function_1__String_1__Function_1__V_m_", traceSpan);
        this.nativeFunctions.put("traceSpan_Function_1__String_1__Function_1__Boolean_1__V_m_", traceSpan);

        Plus plus = new Plus(repository);
        this.nativeFunctions.put("plus_Integer_MANY__Integer_1_", plus);
        this.nativeFunctions.put("plus_Float_MANY__Float_1_", plus);
        this.nativeFunctions.put("plus_Decimal_MANY__Decimal_1_", plus);
        this.nativeFunctions.put("plus_Number_MANY__Number_1_", plus);

        Abs abs = new Abs(repository);
        this.nativeFunctions.put("abs_Integer_1__Integer_1_", abs);
        this.nativeFunctions.put("abs_Float_1__Float_1_", abs);
        this.nativeFunctions.put("abs_Decimal_1__Decimal_1_", abs);
        this.nativeFunctions.put("abs_Number_1__Number_1_", abs);

        Minus minus = new Minus(repository);
        this.nativeFunctions.put("minus_Integer_MANY__Integer_1_", minus);
        this.nativeFunctions.put("minus_Float_MANY__Float_1_", minus);
        this.nativeFunctions.put("minus_Decimal_MANY__Decimal_1_", minus);
        this.nativeFunctions.put("minus_Number_MANY__Number_1_", minus);

        Times times = new Times(repository);
        this.nativeFunctions.put("times_Integer_MANY__Integer_1_", times);
        this.nativeFunctions.put("times_Float_MANY__Float_1_", times);
        this.nativeFunctions.put("times_Decimal_MANY__Decimal_1_", times);
        this.nativeFunctions.put("times_Number_MANY__Number_1_", times);

        this.nativeFunctions.put("floor_Number_1__Integer_1_", new Floor(repository));
        this.nativeFunctions.put("ceiling_Number_1__Integer_1_", new Ceiling(repository));
        this.nativeFunctions.put("round_Number_1__Integer_1_", new Round(repository));
        this.nativeFunctions.put("round_Decimal_1__Integer_1__Decimal_1_", new RoundWithScale(repository));
        this.nativeFunctions.put("round_Float_1__Integer_1__Float_1_", new RoundWithScale(repository));
        this.nativeFunctions.put("stdDev_Number_$1_MANY$__Boolean_1__Number_1_", new StdDev(repository));

        this.nativeFunctions.put("divide_Number_1__Number_1__Float_1_", new Divide(repository));
        this.nativeFunctions.put("divide_Decimal_1__Decimal_1__Integer_1__Decimal_1_", new DivideDecimal(repository));

        this.nativeFunctions.put("pow_Number_1__Number_1__Number_1_", new Power(repository));
        this.nativeFunctions.put("exp_Number_1__Float_1_", new Exp(repository));
        this.nativeFunctions.put("log_Number_1__Float_1_", new Log(repository));
        this.nativeFunctions.put("rem_Number_1__Number_1__Number_1_", new Rem(repository));
        this.nativeFunctions.put("mod_Integer_1__Integer_1__Integer_1_", new Mod(repository));

        this.nativeFunctions.put("sin_Number_1__Float_1_", new Sine(repository));
        this.nativeFunctions.put("asin_Number_1__Float_1_", new ArcSine(repository));
        this.nativeFunctions.put("cos_Number_1__Float_1_", new Cosine(repository));
        this.nativeFunctions.put("acos_Number_1__Float_1_", new ArcCosine(repository));
        this.nativeFunctions.put("tan_Number_1__Float_1_", new Tangent(repository));
        this.nativeFunctions.put("atan_Number_1__Float_1_", new ArcTangent(repository));
        this.nativeFunctions.put("atan2_Number_1__Number_1__Float_1_", new ArcTangent2(repository));
        this.nativeFunctions.put("sqrt_Number_1__Float_1_", new Sqrt(repository));

        this.nativeFunctions.put("lessThan_Number_1__Number_1__Boolean_1_", new LessThan(repository));
        this.nativeFunctions.put("lessThanEqual_Number_1__Number_1__Boolean_1_", new LessThanOrEqualTo(repository));

        this.nativeFunctions.put("toDecimal_Number_1__Decimal_1_", new ToDecimal(repository));
        this.nativeFunctions.put("toFloat_Number_1__Float_1_", new ToFloat(repository));

        this.nativeFunctions.put("is_Any_1__Any_1__Boolean_1_", new Is(repository));
        this.nativeFunctions.put("eq_Any_1__Any_1__Boolean_1_", new Eq(repository));
        this.nativeFunctions.put("equal_Any_MANY__Any_MANY__Boolean_1_", new Equal(repository));

        this.nativeFunctions.put("joinStrings_String_MANY__String_1__String_1__String_1__String_1_", new JoinStrings(repository));
        this.nativeFunctions.put("compileValueSpecification_String_m__CompilationResult_m_", new CompileValueSpecification(this.runtime));
        this.nativeFunctions.put("cast_Any_m__T_1__T_m_", new Cast(repository));

        this.nativeFunctions.put("evaluate_Function_1__List_MANY__Any_MANY_", new EvaluateAny(this));
        this.nativeFunctions.put("rawEvalProperty_Property_1__Any_1__V_m_", new RawEvalProperty(this));
        this.nativeFunctions.put("eval_Function_1__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__T_n__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__T_n__U_p__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__T_n__U_p__W_q__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__T_n__U_p__W_q__X_r__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__T_n__U_p__W_q__X_r__Y_s__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__T_n__U_p__W_q__X_r__Y_s__Z_t__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__S_n__T_o__U_p__W_q__X_r__Y_s__Z_t__V_m_", new Evaluate(this));
        this.nativeFunctions.put("genericType_Any_MANY__GenericType_1_", new GenericType());
        this.nativeFunctions.put("genericTypeClass_GenericType_1__Class_1_", new GenericTypeClass(repository));
        this.nativeFunctions.put("generalizations_Type_1__Type_$1_MANY$_", new Generalizations());
        this.nativeFunctions.put("sourceInformation_Any_1__SourceInformation_$0_1$_", new SourceInformation(repository));

        this.nativeFunctions.put("match_Any_MANY__Function_$1_MANY$__T_m_", new Match(this));
        this.nativeFunctions.put("match_Any_MANY__Function_$1_MANY$__P_o__T_m_", new Match(this));

        this.nativeFunctions.put("id_Any_1__String_1_", new Id(repository));
        this.nativeFunctions.put("if_Boolean_1__Function_1__Function_1__T_m_", new If(this));
        this.nativeFunctions.put("copy_T_1__String_1__KeyExpression_MANY__T_1_", new Copy(repository, this));
        this.nativeFunctions.put("copy_T_1__String_1__T_1_", new Copy(repository, this));
        this.nativeFunctions.put("size_Any_MANY__Integer_1_", new Size(repository));
        this.nativeFunctions.put("length_String_1__Integer_1_", new Length(repository));
        this.nativeFunctions.put("split_String_1__String_1__String_MANY_", new Split(repository));
        this.nativeFunctions.put("trim_String_1__String_1_", new Trim(repository));
        this.nativeFunctions.put("and_Boolean_1__Boolean_1__Boolean_1_", new And(repository, this));
        this.nativeFunctions.put("or_Boolean_1__Boolean_1__Boolean_1_", new Or(repository, this));
        this.nativeFunctions.put("not_Boolean_1__Boolean_1_", new Not(repository));
        this.nativeFunctions.put("profile_T_m__Boolean_1__ProfileResult_1_", new Profile(repository, p, this));
        this.nativeFunctions.put("sort_T_m__Function_$0_1$__Function_$0_1$__T_m_", new Sort(this));
        this.nativeFunctions.put("removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_", new RemoveDuplicates(this));
        this.nativeFunctions.put("removeAllOptimized_T_MANY__T_MANY__T_MANY_", new RemoveAllOptimized(this));
        this.nativeFunctions.put("range_Integer_1__Integer_1__Integer_1__Integer_MANY_", new Range(repository));
        this.nativeFunctions.put("compare_T_1__T_1__Integer_1_", new Compare(repository));
        this.nativeFunctions.put("instanceOf_Any_1__Type_1__Boolean_1_", new InstanceOf(repository));
        this.nativeFunctions.put("subTypeOf_Type_1__Type_1__Boolean_1_", new SubTypeOf(repository));
        this.nativeFunctions.put("deactivate_Any_MANY__ValueSpecification_1_", new Deactivate());
        this.nativeFunctions.put("reactivate_ValueSpecification_1__Map_1__Any_MANY_", new Reactivate(this));
        this.nativeFunctions.put("canReactivateDynamically_ValueSpecification_1__Boolean_1_", new CanReactivateDynamically(repository));
        this.nativeFunctions.put("evaluateAndDeactivate_T_m__T_m_", new EvaluateAndDeactivate());
        this.nativeFunctions.put("toString_Any_1__String_1_", new ToString(repository, this));
        this.nativeFunctions.put("mutateAdd_T_1__String_1__Any_MANY__T_1_", new MutateAdd());
        this.nativeFunctions.put("enumName_Enumeration_1__String_1_", new EnumName(repository));
        this.nativeFunctions.put("enumValues_Enumeration_1__T_MANY_", new EnumValues());
        this.nativeFunctions.put("newClass_String_1__Class_1_", new NewClass(repository));
        this.nativeFunctions.put("newAssociation_String_1__Property_1__Property_1__Association_1_", new NewAssociation(repository));
        this.nativeFunctions.put("newProperty_String_1__GenericType_1__GenericType_1__Multiplicity_1__Property_1_", new NewProperty(repository));
        this.nativeFunctions.put("newLambdaFunction_FunctionType_1__LambdaFunction_1_", new NewLambdaFunction(repository));
        this.nativeFunctions.put("openVariableValues_Function_1__Map_1_", new OpenVariableValues(repository));
        this.nativeFunctions.put("newEnumeration_String_1__String_MANY__Enumeration_1_", new NewEnumeration(repository));
        this.nativeFunctions.put("stereotype_Profile_1__String_1__Stereotype_1_", new Stereotype());
        this.nativeFunctions.put("tag_Profile_1__String_1__Tag_1_", new Tag());
        this.nativeFunctions.put("replace_String_1__String_1__String_1__String_1_", new Replace(repository));
        SubString substring = new SubString(repository);
        this.nativeFunctions.put("substring_String_1__Integer_1__String_1_", substring);
        this.nativeFunctions.put("substring_String_1__Integer_1__Integer_1__String_1_", substring);
        this.nativeFunctions.put("contains_String_1__String_1__Boolean_1_", new Contains(repository));
        this.nativeFunctions.put("startsWith_String_1__String_1__Boolean_1_", new StartsWith(repository));
        this.nativeFunctions.put("endsWith_String_1__String_1__Boolean_1_", new EndsWith(repository));
        this.nativeFunctions.put("matches_String_1__String_1__Boolean_1_", new Matches(repository));
        IndexOfString indexOf = new IndexOfString(repository);
        this.nativeFunctions.put("indexOf_String_1__String_1__Integer_1_", indexOf);
        this.nativeFunctions.put("indexOf_String_1__String_1__Integer_1__Integer_1_", indexOf);
        this.nativeFunctions.put("toOne_T_MANY__T_1_", new ToOne(repository));
        this.nativeFunctions.put("toOneMany_T_MANY__T_$1_MANY$_", new ToOneMany(repository));
        this.nativeFunctions.put("readFile_String_1__String_$0_1$_", new ReadFile(repository, storage));

        this.nativeFunctions.put("reverse_T_m__T_m_", new Reverse());
        this.nativeFunctions.put("parseBoolean_String_1__Boolean_1_", new ParsePrimitive(repository, ModelRepository.BOOLEAN_TYPE_NAME));
        this.nativeFunctions.put("parseDate_String_1__Date_1_", new ParsePrimitive(repository, ModelRepository.DATE_TYPE_NAME));
        this.nativeFunctions.put("parseFloat_String_1__Float_1_", new ParsePrimitive(repository, ModelRepository.FLOAT_TYPE_NAME));
        this.nativeFunctions.put("parseDecimal_String_1__Decimal_1_", new ParsePrimitive(repository, ModelRepository.DECIMAL_TYPE_NAME));
        this.nativeFunctions.put("parseInteger_String_1__Integer_1_", new ParsePrimitive(repository, ModelRepository.INTEGER_TYPE_NAME));
        this.nativeFunctions.put("toInteger_String_1__Integer_1_", new ToInteger(repository));
        this.nativeFunctions.put("encodeBase64_String_1__String_1_", new EncodeBase64(repository));
        this.nativeFunctions.put("decodeBase64_String_1__String_1_", new DecodeBase64(repository));
        this.nativeFunctions.put("today__StrictDate_1_", new Today(repository));
        this.nativeFunctions.put("now__DateTime_1_", new Now(repository));
        this.nativeFunctions.put("hasSubsecond_Date_1__Boolean_1_", new HasSubsecond(repository));
        this.nativeFunctions.put("hasSubsecondWithAtLeastPrecision_Date_1__Integer_1__Boolean_1_", new HasSubsecondWithAtLeastPrecision(repository));
        this.nativeFunctions.put("hasSecond_Date_1__Boolean_1_", new HasSecond(repository));
        this.nativeFunctions.put("hasMinute_Date_1__Boolean_1_", new HasMinute(repository));
        this.nativeFunctions.put("hasHour_Date_1__Boolean_1_", new HasHour(repository));
        this.nativeFunctions.put("hasDay_Date_1__Boolean_1_", new HasDay(repository));
        this.nativeFunctions.put("hasMonth_Date_1__Boolean_1_", new HasMonth(repository));
        this.nativeFunctions.put("second_Date_1__Integer_1_", new Second(repository));
        this.nativeFunctions.put("minute_Date_1__Integer_1_", new Minute(repository));
        this.nativeFunctions.put("hour_Date_1__Integer_1_", new Hour(repository));
        this.nativeFunctions.put("dayOfMonth_Date_1__Integer_1_", new DayOfMonth(repository));
        this.nativeFunctions.put("dayOfWeekNumber_Date_1__Integer_1_", new DayOfWeekNumber(repository));
        this.nativeFunctions.put("weekOfYear_Date_1__Integer_1_", new WeekOfYear(repository));
        this.nativeFunctions.put("monthNumber_Date_1__Integer_1_", new MonthNumber(repository));
        this.nativeFunctions.put("year_Date_1__Integer_1_", new Year(repository));
        this.nativeFunctions.put("datePart_Date_1__Date_1_", new DatePart(repository));
        this.nativeFunctions.put("adjust_Date_1__Integer_1__DurationUnit_1__Date_1_", new AdjustDate(repository));
        this.nativeFunctions.put("dateDiff_Date_1__Date_1__DurationUnit_1__Integer_1_", new DateDiff(repository));
        this.nativeFunctions.put("mayExecuteLegendTest_Function_1__Function_1__X_k_", new LegendTest(repository, this));
        this.nativeFunctions.put("mayExecuteAlloyTest_Function_1__Function_1__X_k_", new AlloyTest(repository, this));

        NewDate newDate = new NewDate(repository);
        this.nativeFunctions.put("date_Integer_1__Date_1_", newDate);
        this.nativeFunctions.put("date_Integer_1__Integer_1__Date_1_", newDate);
        this.nativeFunctions.put("date_Integer_1__Integer_1__Integer_1__StrictDate_1_", newDate);
        this.nativeFunctions.put("date_Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_", newDate);
        this.nativeFunctions.put("date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_", newDate);
        this.nativeFunctions.put("date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__Number_1__DateTime_1_", newDate);

        this.nativeFunctions.put("assert_Boolean_1__Function_1__Boolean_1_", new Assert(this));

        this.nativeFunctions.put("format_String_1__Any_MANY__String_1_", new Format(repository, this));
        this.nativeFunctions.put("toLower_String_1__String_1_", new ToLower(repository));
        this.nativeFunctions.put("toUpper_String_1__String_1_", new ToUpper(repository));

        this.nativeFunctions.put("isSourceReadOnly_String_1__Boolean_1_", new IsSourceReadOnly(this.runtime));
        this.nativeFunctions.put("currentUserId__String_1_", new CurrentUserId(repository));
        this.nativeFunctions.put("isOptionSet_String_1__Boolean_1_", new IsOptionSet(this));
        this.nativeFunctions.put("generateGuid__String_1_", new Guid(repository));



        this.nativeFunctions.put("newMap_Pair_MANY__Map_1_", new ConstructorForPairList(repository));
        this.nativeFunctions.put("newMap_Pair_MANY__Property_MANY__Map_1_", new ConstructorForPairList(repository));
        this.nativeFunctions.put("get_Map_1__U_1__V_$0_1$_", new org.finos.legend.pure.runtime.java.interpreted.natives.core.collection.map.Get());
        this.nativeFunctions.put("getIfAbsentPutWithKey_Map_1__U_1__Function_1__V_$0_1$_", new GetIfAbsentPutWithKey(this));
        this.nativeFunctions.put("getMapStats_Map_1__MapStats_$0_1$_", new GetMapStats(repository));
        this.nativeFunctions.put("put_Map_1__U_1__V_1__Map_1_", new Put());
        this.nativeFunctions.put("putAll_Map_1__Pair_MANY__Map_1_", new PutAllPairs());
        this.nativeFunctions.put("putAll_Map_1__Map_1__Map_1_", new PutAllMaps());
        this.nativeFunctions.put("keys_Map_1__U_MANY_", new Keys());
        this.nativeFunctions.put("keyValues_Map_1__Pair_MANY_", new KeyValues(repository));
        this.nativeFunctions.put("values_Map_1__V_MANY_", new Values());
        this.nativeFunctions.put("replaceAll_Map_1__Pair_MANY__Map_1_", new ReplaceAll());

        this.nativeFunctions.put("groupBy_X_MANY__Function_1__Map_1_", new GroupBy(this));
        this.nativeFunctions.put("functionDescriptorToId_String_1__String_1_", new FunctionDescriptorToId(repository));
        this.nativeFunctions.put("isValidFunctionDescriptor_String_1__Boolean_1_", new IsValidFunctionDescriptor(repository));
        Cipher cipher = new Cipher(repository);
        this.nativeFunctions.put("encrypt_String_1__String_1__String_1_", cipher);
        this.nativeFunctions.put("encrypt_Number_1__String_1__String_1_", cipher);
        this.nativeFunctions.put("encrypt_Boolean_1__String_1__String_1_", cipher);
        this.nativeFunctions.put("decrypt_String_1__String_1__String_1_", new Decipher(repository));

        this.nativeFunctions.put("hash_String_1__HashType_1__String_1_", new Hash(repository));

        for (Pair<String, Function2<FunctionExecutionInterpreted, ModelRepository, NativeFunction>> extraNative : extensions.flatCollect(e -> e.getExtraNatives()))
        {
            this.nativeFunctions.put(extraNative.getOne(), extraNative.getTwo().value(this, repository));
        }
    }

    public CodeStorage getStorage()
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
            CoreInstance result = this.executeFunction(false, FunctionCoreInstanceWrapper.toFunction(function), arguments, new Stack<MutableMap<String, CoreInstance>>(), new Stack<MutableMap<String, CoreInstance>>(), VariableContext.newVariableContext(), null, VoidProfiler.VOID_PROFILER, new InstantiationContext(), executionSupport);
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
            throw new RuntimeException("Failed to write to output stream", e);
        }
    }

    @Override
    public Console getConsole()
    {
        return ((Print)this.nativeFunctions.get("print_Any_MANY__Integer_1__Nil_0_")).getConsole();
    }

    public PureRuntime getPureRuntime()
    {
        return this.runtime;
    }

    NativeFunction getNativeFunction(String functionName)
    {
        return this.nativeFunctions.get(functionName);
    }

    public String printStack(Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, final ProcessorSupport processorSupport)
    {
        int size = resolvedTypeParameters.size();
        String result = "";
        for (int i = 0; i < size; i++)
        {
            MutableMap<String, CoreInstance> map = resolvedTypeParameters.get(i);
            result += i + " :" + map.keyValuesView().collect(new org.eclipse.collections.api.block.function.Function<Pair<String, CoreInstance>, String>()
            {
                @Override
                public String valueOf(Pair<String, CoreInstance> stringCoreInstancePair)
                {
                    return stringCoreInstancePair.getOne() + "=" + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(stringCoreInstancePair.getTwo(), processorSupport);
                }
            }).makeString() + "\n";
        }
        return result;
    }


    public CoreInstance executeLambda(LambdaFunction function, ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext context, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport)
    {
        if (function instanceof LambdaWithContext)
        {
            context = ((LambdaWithContext)function).getVariableContext();
        }

        return this.executeFunctionExecuteParams(function, params, resolvedTypeParameters, resolvedMultiplicityParameters, context, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
    }

    /**
     * In this case the parameters have already been evaluated so no need to re-evaluate
     */
    public CoreInstance executeLambdaFromNative(CoreInstance function, ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext context, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport)
    {
        if (function instanceof LambdaWithContext)
        {
            context = ((LambdaWithContext)function).getVariableContext();
        }

        return this.executeFunction(false, LambdaFunctionCoreInstanceWrapper.toLambdaFunction(function), params, resolvedTypeParameters, resolvedMultiplicityParameters, context, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
    }

    public void cancelExecution()
    {
        this.cancelExecution.set(true);
    }

    public CoreInstance executeFunctionExecuteParams(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function function, ListIterable<? extends CoreInstance> params, final Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, final Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, final VariableContext context, final CoreInstance functionExpressionToUseInStack, final Profiler profiler, final InstantiationContext instantiationContext, final ExecutionSupport executionSupport)
    {
        if (!params.isEmpty())
        {
            ProcessorSupport processorSupport = this.getProcessorSupport();
            MutableList<CoreInstance> parameters = FastList.newList(params.size());
            for (CoreInstance instance : params)
            {
                Executor executor = findValueSpecificationExecutor(instance, functionExpressionToUseInStack, processorSupport, this);
                parameters.add(executor.execute(instance, resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, context, profiler, instantiationContext, executionSupport, this, processorSupport));
            }
            params = parameters;
        }
        return this.executeFunction(false, function, params, resolvedTypeParameters, resolvedMultiplicityParameters, context, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
    }

    public CoreInstance executeFunction(boolean limitScope, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<CoreInstance> function, ListIterable<? extends CoreInstance> params, final Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, final Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, final VariableContext varContext, final CoreInstance functionExpressionToUseInStack, final Profiler profiler, final InstantiationContext instantiationContext, final ExecutionSupport executionSupport)
    {
        try
        {
            if (this.cancelExecution.compareAndSet(true, false))
            {
                throw new PureExecutionException("Cancelled!");
            }

            final ProcessorSupport processorSupport = this.runtime.getProcessorSupport();
            ListIterable<? extends CoreInstance> signatureVars = Instance.getValueForMetaPropertyToManyResolved(processorSupport.function_getFunctionType(function), M3Properties.parameters, processorSupport);
            if (signatureVars.size() != params.size())
            {
                StringBuilder builder = new StringBuilder();
                Function.print(builder, function, processorSupport);
                String message = "Error executing the function:" + builder + ". Mismatch between the number of function parameters (" + signatureVars.size() + ") and the number of supplied arguments (" + params.size() + ")\n" + params.collect(new org.eclipse.collections.api.block.function.Function<CoreInstance, String>()
                {
                    @Override
                    public String valueOf(CoreInstance coreInstance)
                    {
                        return coreInstance.printWithoutDebug("", 3);
                    }
                }).makeString("\n");
                throw new PureExecutionException(functionExpressionToUseInStack == null ? null : functionExpressionToUseInStack.getSourceInformation(), message);
            }

            final VariableContext variableContext = this.moveParametersIntoVariableContext(varContext, signatureVars, params, functionExpressionToUseInStack);
            if (limitScope)
            {
                variableContext.markVariableScopeBoundary();
            }

            for (CoreInstance constraint : function._preConstraints())
            {
                CoreInstance definition = Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.functionDefinition, processorSupport), M3Properties.expressionSequence, processorSupport);
                String ruleId = Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.name, processorSupport).getName();
                CoreInstance evaluatedConstraint = this.executeValueSpecification(definition, new Stack<MutableMap<String, CoreInstance>>(), new Stack<MutableMap<String, CoreInstance>>(), null, variableContext, VoidProfiler.VOID_PROFILER, instantiationContext, executionSupport);
                if ("false".equals(evaluatedConstraint.getValueForMetaPropertyToOne(M3Properties.values).getName()))
                {
                    throw new PureExecutionException(functionExpressionToUseInStack == null ? null : functionExpressionToUseInStack.getSourceInformation(), "Constraint (PRE):[" + ruleId + "] violated. (Function:" + function.getName() + ")");
                }
            }

            // Execute
            CoreInstance result;
            if (Instance.instanceOf(function, M3Paths.NativeFunction, processorSupport))
            {
                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.NativeFunction function1 = NativeFunctionCoreInstanceWrapper.toNativeFunction(function);
                NativeFunction nativeFunction = this.nativeFunctions.get(function1.getName());
                if (nativeFunction == null)
                {
                    throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), "The function '" + function1.getName() + "' is not supported by this execution platform");
                }
                result = nativeFunction.execute(params, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, this.runtime.getContext(), this.runtime.getProcessorSupport());
            }
            else if (Instance.instanceOf(function, M3Paths.Property, processorSupport))
            {
                result = this.executeProperty(PropertyCoreInstanceWrapper.toProperty(function), true, resolvedTypeParameters, resolvedMultiplicityParameters, varContext, profiler, params, functionExpressionToUseInStack, instantiationContext, executionSupport);
            }
            //Qualified properties also go here
            else if (Instance.instanceOf(function, M3Paths.FunctionDefinition, processorSupport))
            {
                RichIterable<? extends CoreInstance> expressions = FunctionDefinitionCoreInstanceWrapper.toFunctionDefinition(function)._expressionSequence();

                CoreInstance returnVal = null;
                for (CoreInstance expression : expressions)
                {
                    Executor executor = findValueSpecificationExecutor(expression, functionExpressionToUseInStack, processorSupport, this);
                    returnVal = executor.execute(expression, resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, variableContext, profiler, instantiationContext, executionSupport, this, processorSupport);
                }
                result = returnVal;
            }
            else if (Instance.instanceOf(function, M3Paths.Path, processorSupport))
            {
                final boolean executable = ValueSpecification.isExecutable(params.get(0), processorSupport);

                CoreInstance value = params.get(0).getValueForMetaPropertyToOne(M3Properties.values);

                MutableList<CoreInstance> res = function.getValueForMetaPropertyToMany(M3Properties.path).injectInto(FastList.newListWith(value), new CheckedFunction2<MutableList<CoreInstance>, CoreInstance, MutableList<CoreInstance>>()
                {
                    @Override
                    public MutableList<CoreInstance> safeValue(MutableList<CoreInstance> instances, final CoreInstance pathElement) throws Exception
                    {
                        return instances.flatCollect(new CheckedFunction<CoreInstance, Iterable<CoreInstance>>()
                        {
                            @Override
                            public Iterable<CoreInstance> safeValueOf(CoreInstance instance) throws Exception
                            {
                                CoreInstance property = Instance.getValueForMetaPropertyToOneResolved(pathElement, M3Properties.property, processorSupport);
                                MutableList<CoreInstance> parameters = FastList.newListWith(ValueSpecificationBootstrap.wrapValueSpecification(instance, executable, processorSupport));
                                parameters.addAllIterable(Instance.getValueForMetaPropertyToManyResolved(pathElement, M3Properties.parameters, processorSupport).collect(new org.eclipse.collections.api.block.function.Function<CoreInstance, CoreInstance>()
                                {
                                    @Override
                                    public CoreInstance valueOf(CoreInstance coreInstance)
                                    {
                                        return ValueSpecificationBootstrap.wrapValueSpecification(Instance.getValueForMetaPropertyToManyResolved(coreInstance, M3Properties.values, processorSupport), executable, processorSupport);
                                    }
                                }));
                                return (Iterable<CoreInstance>)FunctionExecutionInterpreted.this.executeFunction(false, PropertyCoreInstanceWrapper.toProperty(property), parameters, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport).getValueForMetaPropertyToMany(M3Properties.values);
                            }
                        });
                    }
                });
                result = ValueSpecificationBootstrap.wrapValueSpecification(res, executable, processorSupport);
            }
            else
            {
                throw new PureExecutionException("Unsupported function for execution");
            }

            if (function._postConstraints().notEmpty())
            {
                try
                {
                    variableContext.registerValue("return", result);
                }
                catch (VariableNameConflictException e)
                {
                    throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), e.getMessage(), e);
                }
                for (CoreInstance constraint : function._postConstraints())
                {

                    CoreInstance definition = Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.functionDefinition, processorSupport), M3Properties.expressionSequence, processorSupport);
                    String ruleId = Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.name, processorSupport).getName();
                    CoreInstance evaluatedConstraint = this.executeValueSpecification(definition, new Stack<MutableMap<String, CoreInstance>>(), new Stack<MutableMap<String, CoreInstance>>(), null, variableContext, VoidProfiler.VOID_PROFILER, instantiationContext, executionSupport);

                    if ("false".equals(evaluatedConstraint.getValueForMetaPropertyToOne(M3Properties.values).getName()))
                    {
                        throw new PureExecutionException(functionExpressionToUseInStack == null ? null : functionExpressionToUseInStack.getSourceInformation(), "Constraint (POST):[" + ruleId + "] violated. (Function:" + function.getName() + ")");
                    }
                }
            }

            return result;
        }
        catch (PureAssertFailException e)
        {
            org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInfo = (functionExpressionToUseInStack == null ? null : functionExpressionToUseInStack.getSourceInformation());
            if (sourceInfo != null && sourceInfo != e.getSourceInformation())
            {
                String testPurePlatformFileName = "/platform/pure/corefunctions/test.pure";

                boolean allFromAssert = true;
                for (org.finos.legend.pure.m4.coreinstance.SourceInformation si : e.getPureStackSourceInformation())
                {
                    allFromAssert = allFromAssert && si != null && testPurePlatformFileName.equals(si.getSourceId());
                }

                if (allFromAssert && !testPurePlatformFileName.equals(sourceInfo.getSourceId()))
                {
                    throw new PureAssertFailException(sourceInfo, e.getInfo());
                }
                else
                {
                    throw new PureAssertFailException(sourceInfo, e.getInfo(), e);
                }
            }
            else
            {
                throw e;
            }
        }
        catch (PureException e)
        {
            if (functionExpressionToUseInStack != null)
            {
                org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInfo = functionExpressionToUseInStack.getSourceInformation();
                if (sourceInfo != null && !sourceInfo.equals(e.getSourceInformation()))
                {
                    throw new PureExecutionException(sourceInfo, e.getInfo(), e);
                }
            }
            throw e;
        }
        catch (RuntimeException e)
        {
            if (functionExpressionToUseInStack != null)
            {
                org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInfo = functionExpressionToUseInStack.getSourceInformation();
                PureException pureException = PureException.findPureException(e);
                if (pureException == null)
                {
                    if (sourceInfo != null)
                    {
                        throw new PureExecutionException(sourceInfo, e.getMessage(), e);
                    }
                }
                else if (sourceInfo != null && sourceInfo != pureException.getSourceInformation())
                {
                    if (pureException instanceof PureAssertFailException)
                    {
                        throw new PureAssertFailException(sourceInfo, pureException.getInfo(), (PureAssertFailException)pureException);
                    }
                    else
                    {
                        throw new PureExecutionException(sourceInfo, pureException.getInfo(), pureException);
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

    public CoreInstance executeProperty(Property property, boolean route, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, Profiler profiler, ListIterable<? extends CoreInstance> parameters, CoreInstance functionExpressionToUseInStack, InstantiationContext instantiationContext, ExecutionSupport executionSupport) throws PureExecutionException
    {
        ProcessorSupport processorSupport = this.runtime.getProcessorSupport();

        CoreInstance source = parameters.get(0);
        boolean executable = ValueSpecification.isExecutable(source, processorSupport);
        CoreInstance multiplicity = property._multiplicity();

        CoreInstance evaluatedSource = Instance.getValueForMetaPropertyToOneResolved(executable ? findValueSpecificationExecutor(source, property, processorSupport, this).execute(source, resolvedTypeParameters, resolvedMultiplicityParameters, property, variableContext, profiler, instantiationContext, executionSupport, this, processorSupport) : source, M3Properties.values, processorSupport);

        if (evaluatedSource == null)
        {
            throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), "The system can't execute a property function '" + property._name() + "' on a null instance.");
        }
        CoreInstance overrides = Instance.getValueForMetaPropertyToOneResolved(evaluatedSource, M3Properties.elementOverride, processorSupport);
        if (Multiplicity.isToOne(multiplicity, false))
        {
            CoreInstance funcToOne = overrides != null ? Instance.getValueForMetaPropertyToOneResolved(overrides, M3Properties.getterOverrideToOne, processorSupport) : null;
            if (route && funcToOne != null && !M3Properties.elementOverride.equals(property._name()) && !M3Properties.hiddenPayload.equals(property._name()) && !Instance.instanceOf(Instance.getValueForMetaPropertyToOneResolved(property._classifierGenericType().getValueForMetaPropertyToMany(M3Properties.typeArguments).get(1), M3Properties.rawType, processorSupport), M3Paths.DataType, processorSupport))
            {
                return this.executeFunction(true, FunctionCoreInstanceWrapper.toFunction(funcToOne), FastList.newListWith(ValueSpecificationBootstrap.wrapValueSpecification(evaluatedSource, executable, processorSupport), ValueSpecificationBootstrap.wrapValueSpecification(property, executable, processorSupport)), resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
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
                return this.executeFunction(true, FunctionCoreInstanceWrapper.toFunction(funcToMany), FastList.newListWith(ValueSpecificationBootstrap.wrapValueSpecification(evaluatedSource, executable, processorSupport), ValueSpecificationBootstrap.wrapValueSpecification(property, executable, processorSupport)), resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
            }
            else
            {
                ListIterable<? extends CoreInstance> result = Instance.getValueForMetaPropertyToManyResolved(evaluatedSource, property, processorSupport);
                return ValueSpecificationBootstrap.wrapValueSpecification_ForFunctionReturnValue(org.finos.legend.pure.m3.navigation.generictype.GenericType.resolvePropertyReturnType(Instance.extractGenericTypeFromInstance(evaluatedSource, processorSupport), property, processorSupport),
                        result, executable, processorSupport);
            }
        }
    }

    public CoreInstance executeValueSpecification(CoreInstance instance, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, CoreInstance functionExpressionToUseInStack, VariableContext variableContext, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport) throws PureExecutionException
    {
        ProcessorSupport processorSupport = this.getProcessorSupport();
        Executor executor = findValueSpecificationExecutor(instance, functionExpressionToUseInStack, processorSupport, this);
        return executor.execute(instance, resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, variableContext, profiler, instantiationContext, executionSupport, this, processorSupport);
    }

    public static Executor findValueSpecificationExecutor(CoreInstance instance, CoreInstance functionExpressionToUseInStack, ProcessorSupport processorSupport, FunctionExecutionInterpreted functionExecutionInterpreted) throws PureExecutionException
    {

        if (!ValueSpecification.isExecutable(instance, processorSupport))
        {
            return NonExecutableValueSpecificationExecutor.INSTANCE;
        }
        if (functionExecutionInterpreted.cancelExecution.compareAndSet(true, false))
        {
            throw new PureExecutionException("Execution cancelled!");
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
            throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), "A new type (" + instance.getClassifier().getName() + ") must have been introduced in the ValueSpecification tree.");
        }
    }

    private VariableContext moveParametersIntoVariableContext(VariableContext variableContext, ListIterable<? extends CoreInstance> signatureVars, ListIterable<? extends CoreInstance> parameters, CoreInstance functionExpressionToUseInStack)
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
            throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), e.getMessage(), e);
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
        return new M3ProcessorSupport(this.runtime.getContext(), this.runtime.getModelRepository());
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