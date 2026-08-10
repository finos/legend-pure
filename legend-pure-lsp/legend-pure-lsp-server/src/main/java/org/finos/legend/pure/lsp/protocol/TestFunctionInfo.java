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

package org.finos.legend.pure.lsp.protocol;

/**
 * One function found by legend/testFunctions: a real compiled function carrying the
 * meta::pure::profiles::test "Test" stereotype (see
 * org.finos.legend.pure.m3.execution.test.TestTools#hasTestStereotype) in the requested source.
 * {@code functionPath} is the fully-qualified Pure path, suitable for execution (e.g. as
 * PureDebugRunConfiguration.functionName); {@code line} (1-based) is where the client should
 * anchor a gutter icon. {@code isPCTTest} additionally distinguishes a PCT test (see
 * org.finos.legend.pure.m3.pct.shared.PCTTools#isPCTTest) - one that must be run with an adapter
 * path (see ExecuteFunctionParams#pctAdapterPath, legend/getPCTAdapters) - from a plain test, so a
 * client can decide whether to offer a "Run PCTs" action for it.
 * <p>
 * {@code isBeforeFunction}/{@code isAfterFunction} mark a function carrying the
 * {@code <<test.BeforePackage>>}/{@code <<test.AfterPackage>>} stereotype instead of {@code Test} -
 * included here (rather than only via legend/getSetupTeardown) so a Before/After function itself
 * gets its own gutter marker, letting setup/teardown be run/debugged directly. At most one of
 * isPCTTest/isBeforeFunction/isAfterFunction is ever true for a given entry.
 */
public class TestFunctionInfo
{
    private String functionPath;
    private String name;
    private int line;
    private boolean isPCTTest;
    private boolean isBeforeFunction;
    private boolean isAfterFunction;

    public TestFunctionInfo()
    {
    }

    public TestFunctionInfo(String functionPath, String name, int line)
    {
        this(functionPath, name, line, false);
    }

    public TestFunctionInfo(String functionPath, String name, int line, boolean isPCTTest)
    {
        this(functionPath, name, line, isPCTTest, false, false);
    }

    public TestFunctionInfo(String functionPath, String name, int line, boolean isPCTTest, boolean isBeforeFunction, boolean isAfterFunction)
    {
        this.functionPath = functionPath;
        this.name = name;
        this.line = line;
        this.isPCTTest = isPCTTest;
        this.isBeforeFunction = isBeforeFunction;
        this.isAfterFunction = isAfterFunction;
    }

    public String getFunctionPath()
    {
        return this.functionPath;
    }

    public void setFunctionPath(String functionPath)
    {
        this.functionPath = functionPath;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public int getLine()
    {
        return this.line;
    }

    public void setLine(int line)
    {
        this.line = line;
    }

    public boolean isPCTTest()
    {
        return this.isPCTTest;
    }

    public void setPCTTest(boolean isPCTTest)
    {
        this.isPCTTest = isPCTTest;
    }

    public boolean isBeforeFunction()
    {
        return this.isBeforeFunction;
    }

    public void setBeforeFunction(boolean isBeforeFunction)
    {
        this.isBeforeFunction = isBeforeFunction;
    }

    public boolean isAfterFunction()
    {
        return this.isAfterFunction;
    }

    public void setAfterFunction(boolean isAfterFunction)
    {
        this.isAfterFunction = isAfterFunction;
    }
}
