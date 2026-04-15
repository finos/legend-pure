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
import org.finos.legend.pure.m3.pct.shared.PCTTools;
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
 * Native implementation of {@code meta::pure::test::surveyor::executePCTTest}.
 *
 * <p>Executes a single PCT test function with an adapter injected as the first
 * parameter.  Handles exclusions:
 * <ul>
 *   <li>If error matches exclusion entry: PASS (expected failure)</li>
 *   <li>If test passes but was in exclusions: FAIL (needs rebase)</li>
 * </ul>
 */
public class ExecutePCTTest extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;
    private final ModelRepository repository;

    public ExecutePCTTest(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.functionExecution = functionExecution;
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        // 1. Extract parameters: testFn, adapter, exclusions Map
        CoreInstance testFn = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);
        CoreInstance adapter = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
        CoreInstance exclusionsMap = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport);

        // 2. Compute FQN
        String fqn = PackageableElement.getUserPathForPackageableElement(testFn);

        // 3. Look up exclusion for this test
        String expectedError = lookupExclusion((MapCoreInstance) exclusionsMap, testFn);

        // 4. Check for <<test.ToFix>> stereotype: SKIP
        if (TestTools.hasToFixStereotype(testFn, processorSupport))
        {
            printToConsole("  SKIP  " + fqn + " (ToFix)\n");
            return buildTestResult(fqn, "SKIP", 0, "Marked as ToFix", functionExpressionCallStack, processorSupport);
        }

        // 5. Execute with adapter injected as first parameter
        printToConsole("  PCT   " + fqn + " ... ");

        MutableList<CoreInstance> fnParams = Lists.mutable.empty();
        fnParams.add(ValueSpecificationBootstrap.wrapValueSpecification(adapter, true, processorSupport));

        long startNs = System.nanoTime();
        String status;
        String message = null;

        try
        {
            this.functionExecution.executeFunction(
                    false,
                    FunctionCoreInstanceWrapper.toFunction(testFn),
                    fnParams,
                    resolvedTypeParameters,
                    resolvedMultiplicityParameters,
                    getParentOrEmptyVariableContext(variableContext),
                    functionExpressionCallStack,
                    profiler,
                    instantiationContext,
                    executionSupport
            );

            // Test passed
            if (expectedError != null)
            {
                // Was expected to fail but passed: needs rebase
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
                // Expected failure with matching error
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
            String errorMsg = PCTTools.getMessageFromError(e);
            if (expectedError != null && errorMsg.contains(expectedError))
            {
                // Expected failure with matching error
                status = "PASS";
                message = "Expected failure: " + errorMsg;
            }
            else
            {
                status = "ERROR";
                PureException pe = PureException.findPureException(e);
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

        // 6. Print result to console
        printToConsole(status + " (" + elapsedMs + "ms)\n");
        if (message != null && !"PASS".equals(status))
        {
            printToConsole("        " + message + "\n");
        }

        // 7. Construct and return TestResult
        return buildTestResult(fqn, status, elapsedMs, message, functionExpressionCallStack, processorSupport);
    }

    /**
     * Look up the exclusion message for a given test FQN from the Pure Map instance.
     * Returns null if the test is not in the exclusions.
     */
    private String lookupExclusion(MapCoreInstance mapCoreInstance, CoreInstance testFn)
    {
        CoreInstance val = mapCoreInstance.getMap().get(testFn);
        if (val != null)
        {
            return val.getName();
        }
        return null;
    }

    /**
     * Build a {@code meta::pure::test::surveyor::TestResult} instance.
     */
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
