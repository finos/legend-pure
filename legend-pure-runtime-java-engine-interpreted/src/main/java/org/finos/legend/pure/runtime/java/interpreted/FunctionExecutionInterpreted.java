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
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.tuple.Tuples;
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
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext.VariableNameConflictException;
import org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtension;
import org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtensionLoader;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.basics.collection.*;
import org.finos.legend.pure.runtime.java.interpreted.natives.basics.io.Print;
import org.finos.legend.pure.runtime.java.interpreted.natives.basics.lang.*;
import org.finos.legend.pure.runtime.java.interpreted.natives.basics.math.Abs;
import org.finos.legend.pure.runtime.java.interpreted.natives.basics.meta.*;
import org.finos.legend.pure.runtime.java.interpreted.natives.basics.string.*;
import org.finos.legend.pure.runtime.java.interpreted.natives.basics.tests.Assert;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar._boolean.conjunctions.And;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar._boolean.conjunctions.Not;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar._boolean.conjunctions.Or;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar._boolean.equality.Eq;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar._boolean.equality.Equal;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar._boolean.equality.Is;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar._boolean.inequalities.LessThan;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar._boolean.inequalities.LessThanOrEqualTo;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.collection.*;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.lang.*;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.math.*;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.multiplicity.ToOne;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.multiplicity.ToOneMany;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.string.JoinStrings;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;
import org.finos.legend.pure.runtime.java.interpreted.profiler.VoidProfiler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

@ExecutionPlatform(name = "Interpreted")
public class FunctionExecutionInterpreted implements FunctionExecution
{
    private static final int DEFAULT_MAX_SQL_ROWS = 200;

    private final AtomicBoolean cancelExecution = new AtomicBoolean(false);
    private final ExecutionActivityListener executionActivityListener;
    private PureRuntime runtime;

    private Console console = new ConsoleInterpreted();

    private ProcessorSupport processorSupport;

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

        this.nativeFunctions = UnifiedMap.newMap();

        ModelRepository repository = runtime.getModelRepository();
        this.storage = runtime.getCodeStorage();
        this.message = message;


        registerGrammarNatives(repository);
        registerBasicNatives(repository);


        for (Pair<String, Function2<FunctionExecutionInterpreted, ModelRepository, NativeFunction>> extraNative : extensions.flatCollect(e -> e.getExtraNatives()))
        {
            this.nativeFunctions.put(extraNative.getOne(), extraNative.getTwo().value(this, repository));
        }
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


    private void registerBasicNatives(ModelRepository repository)
    {
        // Collection
        this.nativeFunctions.put("concatenate_T_MANY__T_MANY__T_MANY_", new Concatenate(this, repository));
        this.nativeFunctions.put("add_T_MANY__T_1__T_$1_MANY$_", new Add());
        this.nativeFunctions.put("add_T_MANY__Integer_1__T_1__T_$1_MANY$_", new Add());
        this.nativeFunctions.put("at_T_MANY__Integer_1__T_1_", new At());
        this.nativeFunctions.put("fold_T_MANY__Function_1__V_m__V_m_", new Fold(this));
        this.nativeFunctions.put("init_T_MANY__T_MANY_", new Init());
        this.nativeFunctions.put("removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_", new RemoveDuplicates(this));
        this.nativeFunctions.put("sort_T_m__Function_$0_1$__Function_$0_1$__T_m_", new Sort(this));
        this.nativeFunctions.put("tail_T_MANY__T_MANY_", new Tail());

        // IO
        this.nativeFunctions.put("print_Any_MANY__Integer_1__Nil_0_", new Print(this, repository));


        // Lang
        this.nativeFunctions.put("cast_Any_m__T_1__T_m_", new Cast(repository));
        this.nativeFunctions.put("eval_Function_1__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__T_n__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__T_n__U_p__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__T_n__U_p__W_q__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__T_n__U_p__W_q__X_r__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__T_n__U_p__W_q__X_r__Y_s__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__T_n__U_p__W_q__X_r__Y_s__Z_t__V_m_", new Evaluate(this));
        this.nativeFunctions.put("eval_Function_1__S_n__T_o__U_p__W_q__X_r__Y_s__Z_t__V_m_", new Evaluate(this));
        this.nativeFunctions.put("if_Boolean_1__Function_1__Function_1__T_m_", new If(this));
        this.nativeFunctions.put("match_Any_MANY__Function_$1_MANY$__T_m_", new Match(this, repository));
        this.nativeFunctions.put("dynamicNew_Class_1__KeyValue_MANY__Any_1_", new DynamicNew(this, repository));
        this.nativeFunctions.put("dynamicNew_GenericType_1__KeyValue_MANY__Any_1_", new DynamicNewGenericType(this, repository));
        this.nativeFunctions.put("dynamicNew_Class_1__KeyValue_MANY__Function_$0_1$__Function_$0_1$__Any_$0_1$__Any_1_", new DynamicNew(this, repository));
        this.nativeFunctions.put("dynamicNew_GenericType_1__KeyValue_MANY__Function_$0_1$__Function_$0_1$__Any_$0_1$__Any_1_", new DynamicNewGenericType(this, repository));
        this.nativeFunctions.put("dynamicNew_Class_1__KeyValue_MANY__Function_$0_1$__Function_$0_1$__Any_$0_1$__Function_$0_1$__Any_1_", new DynamicNew(this, repository));
        this.nativeFunctions.put("dynamicNew_GenericType_1__KeyValue_MANY__Function_$0_1$__Function_$0_1$__Any_$0_1$__Function_$0_1$__Any_1_", new DynamicNewGenericType(this, repository));
        this.nativeFunctions.put("evaluate_Function_1__List_MANY__Any_MANY_", new EvaluateAny(this, repository));


        // Math
        Abs abs = new Abs(repository);
        this.nativeFunctions.put("abs_Integer_1__Integer_1_", abs);
        this.nativeFunctions.put("abs_Float_1__Float_1_", abs);
        this.nativeFunctions.put("abs_Decimal_1__Decimal_1_", abs);
        this.nativeFunctions.put("abs_Number_1__Number_1_", abs);

        // Meta
        this.nativeFunctions.put("evaluateAndDeactivate_T_m__T_m_", new EvaluateAndDeactivate());
        this.nativeFunctions.put("genericType_Any_MANY__GenericType_1_", new GenericType());
        this.nativeFunctions.put("genericTypeClass_GenericType_1__Class_1_", new GenericTypeClass(repository));
        this.nativeFunctions.put("getUnitValue_Any_1__Number_1_", new GetUnitValue(this, repository));
        this.nativeFunctions.put("id_Any_1__String_1_", new Id(repository));
        this.nativeFunctions.put("instanceOf_Any_1__Type_1__Boolean_1_", new InstanceOf(repository));
        this.nativeFunctions.put("newUnit_Unit_1__Number_1__Any_1_", new NewUnit(this, repository));

        // String
        this.nativeFunctions.put("format_String_1__Any_MANY__String_1_", new Format(repository, this));
        this.nativeFunctions.put("length_String_1__Integer_1_", new Length(repository));
        this.nativeFunctions.put("replace_String_1__String_1__String_1__String_1_", new Replace(repository));
        this.nativeFunctions.put("split_String_1__String_1__String_MANY_", new Split(repository));
        this.nativeFunctions.put("startsWith_String_1__String_1__Boolean_1_", new StartsWith(repository));
        SubString substring = new SubString(repository);
        this.nativeFunctions.put("substring_String_1__Integer_1__String_1_", substring);
        this.nativeFunctions.put("substring_String_1__Integer_1__Integer_1__String_1_", substring);
        this.nativeFunctions.put("toString_Any_1__String_1_", new ToString(repository, this));

        // Tests
        this.nativeFunctions.put("assert_Boolean_1__Function_1__Boolean_1_", new Assert(this));
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
            context = ((LambdaWithContext) function).getVariableContext();
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
            context = ((LambdaWithContext) function).getVariableContext();
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

            if (function instanceof PackageableFunction)
            {
                for (CoreInstance constraint : ((PackageableFunction<CoreInstance>) function)._preConstraints())
                {
                    CoreInstance definition = Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.functionDefinition, processorSupport), M3Properties.expressionSequence, processorSupport);
                    String ruleId = Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.name, processorSupport).getName();
                    CoreInstance evaluatedConstraint = this.executeValueSpecification(definition, new Stack<MutableMap<String, CoreInstance>>(), new Stack<MutableMap<String, CoreInstance>>(), null, variableContext, VoidProfiler.VOID_PROFILER, instantiationContext, executionSupport);
                    if ("false".equals(evaluatedConstraint.getValueForMetaPropertyToOne(M3Properties.values).getName()))
                    {
                        throw new PureExecutionException(functionExpressionToUseInStack == null ? null : functionExpressionToUseInStack.getSourceInformation(), "Constraint (PRE):[" + ruleId + "] violated. (Function:" + function.getName() + ")");
                    }
                }
            }

            // Execute
            CoreInstance result = null;
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
            List<CoreInstance> instances = this.extensions.collect(x -> x.getExtraFunctionExecution(function, params, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, this)).select(x -> x != null);

            if (instances.size() == 1)
            {
                result = instances.get(0);
            }
            else if (result == null)
            {
                throw new PureExecutionException("Unsupported function for execution " + function.getName());
            }

            if (function instanceof PackageableFunction)
            {
                if (((PackageableFunction<CoreInstance>) function)._postConstraints().notEmpty())
                {
                    try
                    {
                        variableContext.registerValue("return", result);
                    }
                    catch (VariableNameConflictException e)
                    {
                        throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), e.getMessage(), e);
                    }
                    for (CoreInstance constraint : ((PackageableFunction<CoreInstance>) function)._postConstraints())
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
            }
            return result;
        }
        catch (PureAssertFailException e)
        {
            org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInfo = (functionExpressionToUseInStack == null ? null : functionExpressionToUseInStack.getSourceInformation());
            if (sourceInfo != null && sourceInfo != e.getSourceInformation())
            {
                String testPurePlatformFileName = "/platform/pure/basics/tests/";
                boolean allFromAssert = true;
                for (org.finos.legend.pure.m4.coreinstance.SourceInformation si : e.getPureStackSourceInformation())
                {
                    allFromAssert = allFromAssert && si != null && si.getSourceId().startsWith(testPurePlatformFileName);
                }

                if (allFromAssert && !sourceInfo.getSourceId().startsWith(testPurePlatformFileName))
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
                        throw new PureAssertFailException(sourceInfo, pureException.getInfo(), (PureAssertFailException) pureException);
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
            throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), "A new type (" + processorSupport.getClassifier(instance).getName() + ") must have been introduced in the ValueSpecification tree.");
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