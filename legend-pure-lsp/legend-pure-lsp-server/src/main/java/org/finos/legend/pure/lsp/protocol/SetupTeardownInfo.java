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
 * Result of legend/getSetupTeardown: the nearest {@code <<test.BeforePackage>>}/
 * {@code <<test.AfterPackage>>} functions to the requested test function (see
 * org.finos.legend.pure.m3.execution.test.TestTools#findNearestBeforePackageFunction), found by
 * walking up its package chain and stopping at the first match in each direction. Either pair of
 * fields may be null if no such function exists anywhere up the chain. {@code beforeFunctionPath}/
 * {@code afterFunctionPath} are suitable for {@link ExecuteFunctionParams#setBeforeFunctionPath}/
 * {@link ExecuteFunctionParams#setAfterFunctionPath}.
 */
public class SetupTeardownInfo
{
    private String beforeFunctionPath;
    private String beforeFunctionName;
    private String afterFunctionPath;
    private String afterFunctionName;

    public SetupTeardownInfo()
    {
    }

    public SetupTeardownInfo(String beforeFunctionPath, String beforeFunctionName, String afterFunctionPath, String afterFunctionName)
    {
        this.beforeFunctionPath = beforeFunctionPath;
        this.beforeFunctionName = beforeFunctionName;
        this.afterFunctionPath = afterFunctionPath;
        this.afterFunctionName = afterFunctionName;
    }

    public String getBeforeFunctionPath()
    {
        return this.beforeFunctionPath;
    }

    public void setBeforeFunctionPath(String beforeFunctionPath)
    {
        this.beforeFunctionPath = beforeFunctionPath;
    }

    public String getBeforeFunctionName()
    {
        return this.beforeFunctionName;
    }

    public void setBeforeFunctionName(String beforeFunctionName)
    {
        this.beforeFunctionName = beforeFunctionName;
    }

    public String getAfterFunctionPath()
    {
        return this.afterFunctionPath;
    }

    public void setAfterFunctionPath(String afterFunctionPath)
    {
        this.afterFunctionPath = afterFunctionPath;
    }

    public String getAfterFunctionName()
    {
        return this.afterFunctionName;
    }

    public void setAfterFunctionName(String afterFunctionName)
    {
        this.afterFunctionName = afterFunctionName;
    }
}
