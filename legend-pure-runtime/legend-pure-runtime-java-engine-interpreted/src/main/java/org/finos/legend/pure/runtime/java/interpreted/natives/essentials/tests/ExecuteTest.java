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

package org.finos.legend.pure.runtime.java.interpreted.natives.essentials.tests;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.Console;
import org.finos.legend.pure.m3.execution.test.TestTools;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.enumeration.Enumeration;
import org.finos.legend.pure.m3.navigation.profile.Profile;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.MapCoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

/**
 * Native implementation of {@code meta::pure::test::surveyor::executeTest}.
 *
 * <p>Unified handler for both plain {@code <<test.Test>>} and PCT ({@code <<PCT.test>>}) tests.
 *
 * <h3>Parameters</h3>
 * <ol>
 *   <li>{@code testFn} — the test function to execute</li>
 *   <li>{@code adapter : Function[0..1]} — present for PCT; absent (empty) for plain tests</li>
 *   <li>{@code exclusions : Map[0..1]} — present for PCT; absent (empty) for plain tests</li>
 * </ol>
 */
public class ExecuteTest extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;
    private final ModelRepository repository;

    public ExecuteTest(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.functionExecution = functionExecution;
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        // 1. Extract parameters — adapter and exclusions are [0..1] and null when absent
        CoreInstance testFn = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);
        CoreInstance adapter = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
        CoreInstance exclusionsRaw = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport);
        MapCoreInstance exclusionsMap = exclusionsRaw instanceof MapCoreInstance ? (MapCoreInstance) exclusionsRaw : null;

        // 2. Compute FQN
        String fqn = PackageableElement.getUserPathForPackageableElement(testFn);

        // 3. PCT tests carry a non-null adapter; plain tests do not
        boolean isPCT = adapter != null;

        // 4. Look up exclusion (PCT only)
        String expectedError = isPCT ? lookupExclusion(exclusionsMap, testFn) : null;

        // 5. Check skip conditions (only ToFix in interpreted — plain and PCT alike)
        if (TestTools.hasToFixStereotype(testFn, processorSupport))
        {
            printToConsole("  SKIP  " + fqn + " (ToFix)\n");
            return buildTestResult(fqn, "SKIP", 0, "Marked as ToFix", functionExpressionCallStack, processorSupport);
        }

        // 6. Print console prefix and build argument list
        MutableList<CoreInstance> fnArgs = Lists.mutable.empty();
        if (isPCT)
        {
            printToConsole("  PCT   " + fqn + " ... ");
            fnArgs.add(ValueSpecificationBootstrap.wrapValueSpecification(adapter, true, processorSupport));
        }
        else
        {
            String runPrefix = "TEST  ";
            if (Profile.hasStereotype(testFn, "meta::pure::profiles::test", "BeforePackage", processorSupport))
            {
                runPrefix = "BEFORE";
            }
            else if (Profile.hasStereotype(testFn, "meta::pure::profiles::test", "AfterPackage", processorSupport))
            {
                runPrefix = "AFTER ";
            }
            printToConsole("  " + runPrefix + " " + fqn + " ... ");
        }

        // 7. Execute with timing + exception handling
        long startNs = System.nanoTime();
        String status;
        String message = null;

        try
        {
            if (isPCT)
            {
                this.functionExecution.executeFunction(
                        false,
                        FunctionCoreInstanceWrapper.toFunction(testFn),
                        fnArgs,
                        resolvedTypeParameters,
                        resolvedMultiplicityParameters,
                        getParentOrEmptyVariableContext(variableContext),
                        functionExpressionCallStack,
                        profiler,
                        instantiationContext,
                        executionSupport
                );
            }
            else
            {
                // emulate test case as entry point — isolates from framework context
                this.functionExecution.start(FunctionCoreInstanceWrapper.toFunction(testFn), Lists.mutable.empty());
            }

            if (expectedError != null)
            {
                status = "FAIL";
                message = "Test was expected to fail with \"" + expectedError + "\" but now passes — run with rebase to update the manifest.";
            }
            else
            {
                status = "PASS";
            }
        }
        catch (PureAssertFailException e)
        {
            String errorMsg = e.getInfo();
            if (expectedError != null && errorMsg != null && errorMsg.contains(expectedError))
            {
                status = "PASS";
                message = "Expected failure: " + errorMsg;
            }
            else
            {
                status = "FAIL";
                message = errorMsg;
                if (e.hasPureStackTrace())
                {
                    message += "\n" + e.getPureStackTrace("        ");
                }
            }
        }
        catch (Exception e)
        {
            PureException pe = PureException.findPureException(e);
            String errorMsg = pe != null ? pe.getInfo() : e.getMessage();
            if (expectedError != null && errorMsg != null && errorMsg.contains(expectedError))
            {
                status = "PASS";
                message = "Expected failure: " + errorMsg;
            }
            else
            {
                status = "ERROR";
                if (pe != null)
                {
                    message = pe.getInfo();
                    if (pe.hasPureStackTrace())
                    {
                        message += "\n" + pe.getPureStackTrace("        ");
                    }
                }
                else
                {
                    message = e.getMessage();
                }
            }
        }

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

        // 8. Print result
        printToConsole(status + " (" + elapsedMs + "ms)\n");
        if (message != null && !"PASS".equals(status))
        {
            printToConsole("        " + message + "\n");
        }

        // 9. Construct and return TestResult
        return buildTestResult(fqn, status, elapsedMs, message, functionExpressionCallStack, processorSupport);
    }

    private String lookupExclusion(MapCoreInstance exclusionsMap, CoreInstance testFn)
    {
        if (exclusionsMap == null)
        {
            return null;
        }
        CoreInstance val = exclusionsMap.getMap().get(testFn);
        return val != null ? val.getName() : null;
    }

    private CoreInstance buildTestResult(String fqn, String statusName, long elapsedMs, String message, MutableStack<CoreInstance> functionExpressionCallStack, ProcessorSupport processorSupport)
    {
        CoreInstance testResultClass = processorSupport.package_getByUserPath("meta::pure::test::surveyor::TestResult");
        CoreInstance testStatusEnum = processorSupport.package_getByUserPath("meta::pure::test::surveyor::TestStatus");

        CoreInstance instance = this.repository.newEphemeralAnonymousCoreInstance(functionExpressionCallStack.peek().getSourceInformation(), testResultClass);

        Instance.setValueForProperty(instance, "fqn", this.repository.newStringCoreInstance(fqn), processorSupport);

        CoreInstance enumValue = Enumeration.findEnum(testStatusEnum, statusName);
        Instance.setValueForProperty(instance, "status", enumValue, processorSupport);
        Instance.setValueForProperty(instance, "elapsed", this.repository.newIntegerCoreInstance(elapsedMs), processorSupport);

        if (message != null)
        {
            Instance.setValueForProperty(instance, "message", this.repository.newStringCoreInstance(message), processorSupport);
        }

        return ValueSpecificationBootstrap.wrapValueSpecification(instance, true, processorSupport);
    }

    private void printToConsole(String text)
    {
        Console console = this.functionExecution.getConsole();
        if (console.isEnabled())
        {
            console.print(text);
        }
    }
}
