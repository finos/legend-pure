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
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

/**
 * Native implementation of {@code meta::pure::test::surveyor::executeTest}.
 *
 * <p>Executes a single test function in a sandbox, catches exceptions,
 * measures elapsed time, and returns a fully-populated {@code TestResult}.
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li>{@code <<test.ToFix>>} stereotype → {@code SKIP} (not executed)</li>
 *   <li>Clean return → {@code PASS}</li>
 *   <li>{@link PureAssertFailException} → {@code FAIL} with message + Pure stack trace</li>
 *   <li>Any other exception → {@code ERROR} with message + Pure stack trace</li>
 * </ul>
 *
 * <p>Progress is printed to the console as each test executes.
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
        // 1. Extract the test function
        CoreInstance testFn = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);

        // 2. Compute FQN
        String fqn = PackageableElement.getUserPathForPackageableElement(testFn);

        // 3. Check for <<test.ToFix>> stereotype → SKIP
        if (TestTools.hasToFixStereotype(testFn, processorSupport))
        {
            printToConsole("  SKIP  " + fqn + " (ToFix)\n");
            return buildTestResult(fqn, "SKIP", 0, "Marked as ToFix", functionExpressionCallStack, processorSupport);
        }

        // 4. Execute with timing + exception handling
        String runPrefix = "TEST  ";
        if (org.finos.legend.pure.m3.navigation.profile.Profile.hasStereotype(testFn, "meta::pure::profiles::test", "BeforePackage", processorSupport))
        {
            runPrefix = "BEFORE";
        }
        else if (org.finos.legend.pure.m3.navigation.profile.Profile.hasStereotype(testFn, "meta::pure::profiles::test", "AfterPackage", processorSupport))
        {
            runPrefix = "AFTER ";
        }
        printToConsole("  " + runPrefix + " " + fqn + " ... ");

        long startNs = System.nanoTime();
        String status;
        String message = null;

        try
        {
            this.functionExecution.executeFunction(
                    false,
                    FunctionCoreInstanceWrapper.toFunction(testFn),
                    Lists.mutable.empty(),
                    resolvedTypeParameters,
                    resolvedMultiplicityParameters,
                    getParentOrEmptyVariableContext(variableContext),
                    functionExpressionCallStack,
                    profiler,
                    instantiationContext,
                    executionSupport
            );
            status = "PASS";
        }
        catch (PureAssertFailException e)
        {
            status = "FAIL";
            message = e.getInfo();
            if (e.hasPureStackTrace())
            {
                message += "\n" + e.getPureStackTrace("        ");
            }
        }
        catch (Exception e)
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

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

        // 5. Print result to console
        printToConsole(status + " (" + elapsedMs + "ms)\n");
        if (message != null && !"PASS".equals(status))
        {
            printToConsole("        " + message + "\n");
        }

        // 6. Construct and return TestResult
        return buildTestResult(fqn, status, elapsedMs, message, functionExpressionCallStack, processorSupport);
    }

    /**
     * Build a {@code meta::pure::test::surveyor::TestResult} instance.
     */
    private CoreInstance buildTestResult(String fqn, String statusName, long elapsedMs, String message, MutableStack<CoreInstance> functionExpressionCallStack, ProcessorSupport processorSupport)
    {
        // Resolve TestResult class and TestStatus enum from the model
        CoreInstance testResultClass = processorSupport.package_getByUserPath("meta::pure::test::surveyor::TestResult");
        CoreInstance testStatusEnum = processorSupport.package_getByUserPath("meta::pure::test::surveyor::TestStatus");

        // Create TestResult instance
        CoreInstance instance = this.repository.newEphemeralAnonymousCoreInstance(
                functionExpressionCallStack.peek().getSourceInformation(),
                testResultClass
        );

        // Set fqn
        Instance.setValuesForProperty(instance, "fqn",
                Lists.mutable.with(this.repository.newStringCoreInstance(fqn)),
                processorSupport);

        // Set status (resolve enum value)
        CoreInstance enumValue = Enumeration.findEnum(testStatusEnum, statusName);
        Instance.setValuesForProperty(instance, "status",
                Lists.mutable.with(enumValue),
                processorSupport);

        // Set elapsed
        Instance.setValuesForProperty(instance, "elapsed",
                Lists.mutable.with(this.repository.newIntegerCoreInstance(elapsedMs)),
                processorSupport);

        // Set message (optional)
        if (message != null)
        {
            Instance.setValuesForProperty(instance, "message",
                    Lists.mutable.with(this.repository.newStringCoreInstance(message)),
                    processorSupport);
        }

        return ValueSpecificationBootstrap.wrapValueSpecification(instance, true, processorSupport);
    }

    /**
     * Print to the console if enabled.
     */
    private void printToConsole(String text)
    {
        Console console = this.functionExecution.getConsole();
        if (console.isEnabled())
        {
            console.print(text);
        }
    }
}
