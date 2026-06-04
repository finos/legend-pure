// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.lsp.debug;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.lsp.UriMapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.constraint.Constraint;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.Executor;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext.VariableNameConflictException;
import org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtension;
import org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtensionLoader;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;
import org.finos.legend.pure.runtime.java.interpreted.profiler.VoidProfiler;

class LegendDebugFunctionExecution extends FunctionExecutionInterpreted
{
    private final Set<String> debuggableSourceIds;
    private final UriMapper uriMapper;
    private final MutableList<InterpretedExtension> interpretedExtensions = InterpretedExtensionLoader.extensions();
    private final LinkedHashSet<String> evaluationImports = new LinkedHashSet<>();
    private final ForkJoinPool executionPool;
    private final ThreadLocal<Boolean> pausesSuppressed = ThreadLocal.withInitial(() -> false);
    private final ThreadLocal<Deque<ActiveFrame>> activeFrames = ThreadLocal.withInitial(ArrayDeque::new);
    private final AtomicLong nextExecutionOrdinal = new AtomicLong();

    private volatile CompletableFuture<CoreInstance> currentExecution;
    private volatile CompletableFuture<CoreInstance> resultHandler;
    private volatile LegendDebugState debugState;

    LegendDebugFunctionExecution(Set<String> debuggableSourceIds, UriMapper uriMapper)
    {
        this.debuggableSourceIds = debuggableSourceIds == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new TreeSet<>(debuggableSourceIds));
        this.uriMapper = uriMapper;
        this.executionPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors(),
                new LspDebugForkJoinWorkerThreadFactory(), null, false);
    }

    LegendDebugState getDebugState()
    {
        return this.debugState;
    }

    synchronized void addEvaluationImports(Collection<String> imports)
    {
        if (imports != null)
        {
            this.evaluationImports.addAll(imports);
        }
    }

    synchronized List<String> getEvaluationImports()
    {
        return new ArrayList<>(this.evaluationImports);
    }

    void startDebug(CoreInstance function, ListIterable<? extends CoreInstance> arguments)
    {
        CompletableFuture<CoreInstance> handler = new CompletableFuture<>();
        this.resultHandler = handler;

        if (this.currentExecution == null)
        {
            CompletableFuture<CoreInstance> execution = CompletableFuture.supplyAsync(
                    () -> this.start(function, arguments),
                    this.executionPool);
            this.currentExecution = execution;
            execution.whenComplete((value, error) ->
            {
                CompletableFuture<CoreInstance> activeHandler = this.resultHandler;
                if (error == null)
                {
                    activeHandler.complete(value);
                }
                else
                {
                    activeHandler.completeExceptionally(error);
                }
                this.currentExecution = null;
            });
        }
        else
        {
            LegendDebugState state = this.debugState;
            if (state == null)
            {
                handler.completeExceptionally(new IllegalStateException("Debug execution is not paused"));
            }
            else
            {
                state.release();
            }
        }

        try
        {
            handler.join();
        }
        catch (CompletionException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException)
            {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    void abortDebug()
    {
        LegendDebugState state = this.debugState;
        if (state != null)
        {
            state.abort();
        }
        else if (this.currentExecution != null)
        {
            this.cancelExecution();
        }
    }

    <T> T withPausesSuppressed(Supplier<T> supplier)
    {
        Boolean previous = this.pausesSuppressed.get();
        this.pausesSuppressed.set(Boolean.TRUE);
        try
        {
            return supplier.get();
        }
        finally
        {
            this.pausesSuppressed.set(previous);
        }
    }

    void clearDebugState(LegendDebugState state)
    {
        if (this.debugState == state)
        {
            this.debugState = null;
        }
    }

    @Override
    public CoreInstance executeFunctionExecuteParams(Function<?> function, ListIterable<? extends CoreInstance> params,
                                                     Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters,
                                                     Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters,
                                                     VariableContext context,
                                                     MutableStack<CoreInstance> functionExpressionCallStack,
                                                     Profiler profiler, InstantiationContext instantiationContext,
                                                     ExecutionSupport executionSupport)
    {
        if (params.notEmpty())
        {
            MutableList<CoreInstance> parameters = Lists.mutable.ofInitialCapacity(params.size());
            for (CoreInstance instance : params)
            {
                parameters.add(this.executeValueSpecification(instance, resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, context, profiler, instantiationContext, executionSupport));
            }
            params = parameters;
        }
        return this.executeFunction(false, function, params, resolvedTypeParameters, resolvedMultiplicityParameters, context, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
    }

    @Override
    public CoreInstance executeFunction(boolean limitScope, Function<?> function,
                                        ListIterable<? extends CoreInstance> params,
                                        Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters,
                                        Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters,
                                        VariableContext varContext,
                                        MutableStack<CoreInstance> functionExpressionCallStack,
                                        Profiler profiler, InstantiationContext instantiationContext,
                                        ExecutionSupport executionSupport)
    {
        ProcessorSupport processorSupport = getProcessorSupport();
        if (org.finos.legend.pure.m3.navigation.function.Function.isNativeFunction(function, processorSupport)
                || org.finos.legend.pure.m3.navigation.property.Property.isProperty(function, processorSupport)
                || !org.finos.legend.pure.m3.navigation.function.Function.isFunctionDefinition(function, processorSupport))
        {
            return super.executeFunction(limitScope, function, params, resolvedTypeParameters, resolvedMultiplicityParameters, varContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
        }

        Function<?> executedFunction = function;
        try
        {
            ListIterable<? extends CoreInstance> signatureVars = Instance.getValueForMetaPropertyToManyResolved(processorSupport.function_getFunctionType(function), M3Properties.parameters, processorSupport);
            if (signatureVars.size() != params.size())
            {
                StringBuilder builder = new StringBuilder();
                if (function._functionName() != null)
                {
                    org.finos.legend.pure.m3.navigation.function.Function.print(builder, function, processorSupport);
                }
                String message = "Error executing the function:" + builder + ". Mismatch between the number of function parameters (" + signatureVars.size() + ") and the number of supplied arguments (" + params.size() + ")\n" + params.collect(i -> i.printWithoutDebug("", 3)).makeString("\n");
                throw new PureExecutionException(functionExpressionCallStack.isEmpty() ? null : functionExpressionCallStack.peek().getSourceInformation(), message, functionExpressionCallStack);
            }

            VariableContext variableContext = moveParametersIntoVariableContext(varContext, signatureVars, params, functionExpressionCallStack);
            if (limitScope)
            {
                variableContext.markVariableScopeBoundary();
            }

            enterFunctionFrame(function, variableContext, functionExpressionCallStack);
            try
            {
                executePreConstraints(function, variableContext, functionExpressionCallStack, instantiationContext, executionSupport, processorSupport);

                executedFunction = resolveFunctionDefinition(function, params);
                CoreInstance result = null;
                for (CoreInstance expression : executedFunction.getValueForMetaPropertyToMany(M3Properties.expressionSequence))
                {
                    result = this.executeValueSpecification(expression, resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, variableContext, profiler, instantiationContext, executionSupport);
                }

                Function<?> finalFunction = executedFunction;
                MutableList<CoreInstance> extensionResults = this.interpretedExtensions.collect(extension -> extension.getExtraFunctionExecution(
                        finalFunction,
                        params,
                        resolvedTypeParameters,
                        resolvedMultiplicityParameters,
                        variableContext,
                        functionExpressionCallStack,
                        profiler,
                        instantiationContext,
                        executionSupport,
                        processorSupport,
                        this)).select(Objects::nonNull);
                if (extensionResults.size() == 1)
                {
                    result = extensionResults.get(0);
                }
                else if (result == null)
                {
                    throw new PureExecutionException("Unsupported function for execution " + executedFunction.getName() + " of type " + PackageableElement.getUserPathForPackageableElement(executedFunction.getClassifier()) + " (class " + executedFunction.getClass().getName() + ")", functionExpressionCallStack);
                }

                executePostConstraints(executedFunction, result, variableContext, functionExpressionCallStack, instantiationContext, executionSupport, processorSupport);
                return result;
            }
            finally
            {
                exitFunctionFrame();
            }
        }
        catch (PureAssertFailException e)
        {
            SourceInformation sourceInfo = functionExpressionCallStack.isEmpty() ? null : functionExpressionCallStack.peek().getSourceInformation();
            if (e.getSourceInformation() == null && sourceInfo != null)
            {
                String testPurePlatformFileName = "/platform/pure/essential/tests/";
                boolean allFromAssert = true;
                for (SourceInformation si : e.getPureStackSourceInformation())
                {
                    allFromAssert = allFromAssert && si != null && si.getSourceId().startsWith(testPurePlatformFileName);
                }

                if (allFromAssert && !sourceInfo.getSourceId().startsWith(testPurePlatformFileName))
                {
                    throw new PureAssertFailException(sourceInfo, e.getInfo(), functionExpressionCallStack);
                }
                throw new PureAssertFailException(sourceInfo, e.getInfo(), e, functionExpressionCallStack);
            }
            throw e;
        }
        catch (PureException e)
        {
            if (!functionExpressionCallStack.isEmpty())
            {
                SourceInformation sourceInfo = functionExpressionCallStack.peek().getSourceInformation();
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
                SourceInformation sourceInfo = functionExpressionCallStack.peek().getSourceInformation();
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
                    throw new PureExecutionException(sourceInfo, pureException.getInfo(), pureException, functionExpressionCallStack);
                }
                else
                {
                    throw pureException;
                }
            }
            throw e;
        }
        catch (StackOverflowError stackOverflowError)
        {
            throw new PureExecutionException("Pure stackoverflow", stackOverflowError, functionExpressionCallStack);
        }
    }

    @Override
    public CoreInstance executeValueSpecification(CoreInstance instance,
                                                  Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters,
                                                  Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters,
                                                  MutableStack<CoreInstance> functionExpressionCallStack,
                                                  VariableContext variableContext, Profiler profiler,
                                                  InstantiationContext instantiationContext,
                                                  ExecutionSupport executionSupport)
    {
        pauseIfDebuggable(instance, variableContext, functionExpressionCallStack);
        Executor executor = FunctionExecutionInterpreted.findValueSpecificationExecutor(instance, functionExpressionCallStack, getProcessorSupport(), this);
        return executor.execute(instance, resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, variableContext, profiler, instantiationContext, executionSupport, this, getProcessorSupport());
    }

    private void executePreConstraints(Function<?> function, VariableContext variableContext,
                                       MutableStack<CoreInstance> functionExpressionCallStack,
                                       InstantiationContext instantiationContext, ExecutionSupport executionSupport,
                                       ProcessorSupport processorSupport)
    {
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
    }

    private void executePostConstraints(Function<?> function, CoreInstance result, VariableContext variableContext,
                                        MutableStack<CoreInstance> functionExpressionCallStack,
                                        InstantiationContext instantiationContext, ExecutionSupport executionSupport,
                                        ProcessorSupport processorSupport)
    {
        if (org.finos.legend.pure.m3.navigation.function.Function.isPackageableFunction(function, processorSupport))
        {
            PackageableFunction<?> packageableFunction = PackageableFunctionCoreInstanceWrapper.toPackageableFunction(function);
            RichIterable<? extends Constraint> postConstraints = packageableFunction._postConstraints();
            if (postConstraints.notEmpty())
            {
                try
                {
                    variableContext.registerValue("return", result);
                }
                catch (VariableNameConflictException e)
                {
                    throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), e.getMessage(), e, functionExpressionCallStack);
                }
                for (Constraint constraint : postConstraints)
                {
                    org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification definition = constraint._functionDefinition()._expressionSequence().getOnly();
                    String ruleId = constraint._name();
                    CoreInstance evaluatedConstraint = this.executeValueSpecification(definition, new Stack<>(), new Stack<>(), Stacks.mutable.empty(), variableContext, VoidProfiler.VOID_PROFILER, instantiationContext, executionSupport);

                    if (!PrimitiveUtilities.getBooleanValue(evaluatedConstraint.getValueForMetaPropertyToOne(M3Properties.values)))
                    {
                        throw new PureExecutionException(functionExpressionCallStack.isEmpty() ? null : functionExpressionCallStack.peek().getSourceInformation(), "Constraint (POST):[" + ruleId + "] violated. (Function:" + function.getName() + ")", functionExpressionCallStack);
                    }
                }
            }
        }
    }

    private Function<?> resolveFunctionDefinition(Function<?> function, ListIterable<? extends CoreInstance> params)
    {
        ProcessorSupport processorSupport = getProcessorSupport();
        if (org.finos.legend.pure.m3.navigation.property.Property.isQualifiedProperty(function, processorSupport))
        {
            String functionName = org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(function);
            CoreInstance functionType = processorSupport.function_getFunctionType(function);
            ListIterable<? extends CoreInstance> functionParams = functionType.getValueForMetaPropertyToMany(M3Properties.parameters);
            CoreInstance functionSourceType = Instance.getValueForMetaPropertyToOneResolved(functionParams.get(0), M3Properties.rawType, processorSupport);

            ListIterable<? extends CoreInstance> firstParamValues = params.get(0).getValueForMetaPropertyToMany(M3Properties.values);
            if (firstParamValues == null)
            {
                throw new IllegalStateException("Unexpected value specification type for first parameter: " + PackageableElement.getUserPathForPackageableElement(params.get(0).getClassifier()));
            }
            CoreInstance instance = firstParamValues.get(0);
            CoreInstance instanceType = instance.getClassifier();
            if (functionSourceType != instanceType)
            {
                for (CoreInstance type : Type.getGeneralizationResolutionOrder(instanceType, processorSupport))
                {
                    if (type == functionSourceType)
                    {
                        return function;
                    }
                    for (String qpProp : _Class.QUALIFIED_PROPERTIES_PROPERTIES)
                    {
                        for (CoreInstance qualProp : type.getValueForMetaPropertyToMany(qpProp))
                        {
                            if (functionName.equals(org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(qualProp)))
                            {
                                CoreInstance qpFT = processorSupport.function_getFunctionType(qualProp);
                                ListIterable<? extends CoreInstance> qpParams = qpFT.getValueForMetaPropertyToMany(M3Properties.parameters);
                                if ((functionParams.size() == qpParams.size()) &&
                                        ((functionParams.size() <= 1) ||
                                                org.eclipse.collections.impl.list.Interval.fromTo(1, functionParams.size() - 1).allSatisfy(i -> GenericType.genericTypesEqual(functionParams.get(i).getValueForMetaPropertyToOne(M3Properties.genericType), qpParams.get(i).getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport))))
                                {
                                    return (Function<?>) qualProp;
                                }
                            }
                        }
                    }
                }

                StringBuilder builder = new StringBuilder("Could not find qualified property ")
                        .append(org.finos.legend.pure.m3.navigation.property.Property.getQualifiedPropertyId(function, processorSupport))
                        .append(" for instance of type ");
                PackageableElement.writeUserPathForPackageableElement(builder, instanceType);
                throw new IllegalStateException(builder.toString());
            }
        }
        return function;
    }

    private VariableContext moveParametersIntoVariableContext(VariableContext variableContext,
                                                              ListIterable<? extends CoreInstance> signatureVars,
                                                              ListIterable<? extends CoreInstance> parameters,
                                                              MutableStack<CoreInstance> functionExpressionCallStack)
    {
        VariableContext newVarContext = VariableContext.newVariableContext(variableContext);
        try
        {
            ProcessorSupport processorSupport = getProcessorSupport();
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

    private void enterFunctionFrame(Function<?> function, VariableContext variableContext,
                                    MutableStack<CoreInstance> functionExpressionCallStack)
    {
        if (!this.pausesSuppressed.get())
        {
            this.activeFrames.get().addLast(new ActiveFrame(function, variableContext, currentFrameLocation(function, functionExpressionCallStack)));
        }
    }

    private void exitFunctionFrame()
    {
        if (!this.pausesSuppressed.get())
        {
            Deque<ActiveFrame> frames = this.activeFrames.get();
            if (!frames.isEmpty())
            {
                frames.removeLast();
            }
        }
    }

    private void pauseIfDebuggable(CoreInstance valueSpecification, VariableContext variableContext,
                                   MutableStack<CoreInstance> functionExpressionCallStack)
    {
        if (this.pausesSuppressed.get() || valueSpecification == null)
        {
            return;
        }

        SourceInformation sourceInformation = valueSpecification.getSourceInformation();
        if (sourceInformation == null || !this.debuggableSourceIds.contains(sourceInformation.getSourceId()))
        {
            return;
        }

        DebugExecutionLocation location = location(valueSpecification, sourceInformation, variableContext, functionExpressionCallStack);
        LegendDebugState state = new LegendDebugState(this, location, frameSnapshots(location, variableContext));
        setDebugState(state);
        state.await();
        if (state.aborted())
        {
            throw new PureExecutionException("Aborting execution...", functionExpressionCallStack);
        }
    }

    private List<DebugFrameSnapshot> frameSnapshots(DebugExecutionLocation currentLocation, VariableContext currentVariableContext)
    {
        List<ActiveFrame> active = new ArrayList<>(this.activeFrames.get());
        Collections.reverse(active);

        List<DebugFrameSnapshot> result = new ArrayList<>();
        result.add(new DebugFrameSnapshot(
                1,
                1,
                currentLocation.getName(),
                currentLocation,
                currentVariableContext));

        int id = 2;
        for (ActiveFrame frame : active)
        {
            if (sameFrameLocation(currentLocation, frame.location))
            {
                continue;
            }
            result.add(new DebugFrameSnapshot(id, id, frame.name(), frame.location, frame.variableContext));
            id++;
        }
        return result;
    }

    private boolean sameFrameLocation(DebugExecutionLocation currentLocation, DebugExecutionLocation frameLocation)
    {
        return currentLocation != null && frameLocation != null && currentLocation.sameRange(frameLocation);
    }

    private DebugExecutionLocation location(CoreInstance valueSpecification, SourceInformation sourceInformation,
                                            VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack)
    {
        String sourceId = sourceInformation.getSourceId();
        return new DebugExecutionLocation(
                sourceId,
                sourceId == null ? null : this.uriMapper.toUri(sourceId),
                positiveOrDefault(sourceInformation.getLine(), sourceInformation.getStartLine(), 1),
                positiveOrDefault(sourceInformation.getColumn(), sourceInformation.getStartColumn(), 1),
                positiveOrDefault(sourceInformation.getEndLine(), sourceInformation.getLine(), sourceInformation.getStartLine(), 1),
                positiveOrDefault(sourceInformation.getEndColumn(), sourceInformation.getColumn(), sourceInformation.getStartColumn(), 1),
                locationName(valueSpecification),
                this.activeFrames.get().size(),
                this.nextExecutionOrdinal.incrementAndGet(),
                isExplicitDebug(valueSpecification));
    }

    private DebugExecutionLocation currentFrameLocation(Function<?> function, MutableStack<CoreInstance> functionExpressionCallStack)
    {
        SourceInformation sourceInformation = null;
        if (functionExpressionCallStack != null && functionExpressionCallStack.notEmpty())
        {
            sourceInformation = functionExpressionCallStack.peek().getSourceInformation();
        }
        if (sourceInformation == null && function != null)
        {
            sourceInformation = function.getSourceInformation();
        }

        String sourceId = sourceInformation == null ? null : sourceInformation.getSourceId();
        return new DebugExecutionLocation(
                sourceId,
                sourceId == null ? null : this.uriMapper.toUri(sourceId),
                sourceInformation == null ? 1 : positiveOrDefault(sourceInformation.getLine(), sourceInformation.getStartLine(), 1),
                sourceInformation == null ? 1 : positiveOrDefault(sourceInformation.getColumn(), sourceInformation.getStartColumn(), 1),
                sourceInformation == null ? 1 : positiveOrDefault(sourceInformation.getEndLine(), sourceInformation.getLine(), sourceInformation.getStartLine(), 1),
                sourceInformation == null ? 1 : positiveOrDefault(sourceInformation.getEndColumn(), sourceInformation.getColumn(), sourceInformation.getStartColumn(), 1),
                functionName(function),
                this.activeFrames.get().size(),
                this.nextExecutionOrdinal.get(),
                false);
    }

    private boolean isExplicitDebug(CoreInstance valueSpecification)
    {
        CoreInstance function = functionExpressionFunction(valueSpecification);
        if (function == null)
        {
            return false;
        }
        String name = function.getName();
        if ("debug__Nil_0_".equals(name))
        {
            return true;
        }
        String path = safeUserPath(function);
        return path != null && path.contains("meta::pure::ide::debug");
    }

    private String locationName(CoreInstance valueSpecification)
    {
        CoreInstance function = functionExpressionFunction(valueSpecification);
        if (function != null)
        {
            return functionName(function);
        }
        String name = valueSpecification.getName();
        return (name == null || name.isEmpty()) ? "Pure expression" : name;
    }

    private CoreInstance functionExpressionFunction(CoreInstance valueSpecification)
    {
        try
        {
            ProcessorSupport processorSupport = getProcessorSupport();
            return Instance.instanceOf(valueSpecification, org.finos.legend.pure.m3.navigation.M3Paths.FunctionExpression, processorSupport)
                    ? Instance.getValueForMetaPropertyToOneResolved(valueSpecification, M3Properties.func, processorSupport)
                    : null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private String functionName(CoreInstance function)
    {
        if (function == null)
        {
            return "Pure frame";
        }
        String path = safeUserPath(function);
        if (path != null && !path.isEmpty())
        {
            return path;
        }
        String name = function.getName();
        return (name == null || name.isEmpty()) ? "Pure frame" : name;
    }

    private String safeUserPath(CoreInstance function)
    {
        try
        {
            return PackageableElement.getUserPathForPackageableElement(function);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static int positiveOrDefault(int first, int second, int third, int fallback)
    {
        if (first > 0)
        {
            return first;
        }
        if (second > 0)
        {
            return second;
        }
        return third > 0 ? third : fallback;
    }

    private static int positiveOrDefault(int first, int second, int fallback)
    {
        return positiveOrDefault(first, second, -1, fallback);
    }

    private void setDebugState(LegendDebugState state)
    {
        if (state != null && this.debugState != null)
        {
            throw new IllegalStateException("Debug session already exists");
        }

        this.debugState = state;
        if (state != null)
        {
            CompletableFuture<CoreInstance> handler = this.resultHandler;
            if (handler != null)
            {
                handler.complete(null);
            }
        }
    }

    private static class ActiveFrame
    {
        private final CoreInstance function;
        private final VariableContext variableContext;
        private final DebugExecutionLocation location;

        private ActiveFrame(CoreInstance function, VariableContext variableContext, DebugExecutionLocation location)
        {
            this.function = function;
            this.variableContext = variableContext;
            this.location = location;
        }

        private String name()
        {
            String name = this.function == null ? null : this.function.getName();
            return (name == null || name.isEmpty()) ? "Pure frame" : name;
        }
    }
}
