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

import java.util.List;

/**
 * Params for legend/executeTests: discover and run every test in a scope, rather than one function
 * at a time via legend/execute.
 * <p>
 * Exactly one of {@code packagePath} / {@code uri} / {@code functions} / {@code invocations}
 * selects the scope:
 * <ul>
 *   <li>{@code packagePath} - a Pure package. With {@code recursive} (default true) every subpackage
 *       is included, each keeping its own before/after-package bracketing.</li>
 *   <li>{@code uri} - a file:// uri, pure:// uri, or raw sourceId; runs only that file's tests, with
 *       their full inherited hook chain.</li>
 *   <li>{@code functions} - an explicit list of zero-arg function paths run as one flat group, no
 *       discovery and no hook bracketing.</li>
 *   <li>{@code invocations} - the same flat group, but each entry carries its own {@code String[1]}
 *       arguments and a display label, for fanning out over one parameterised function.</li>
 * </ul>
 * {@code includeVanilla}/{@code includePct} (both default true) narrow to plain {@code <<test.Test>>}
 * or {@code <<PCT.test>>} functions. {@code pctAdapterPath} is the one adapter applied to every PCT
 * test in the run, required if the scope contains any. {@code parallel} fans out tests within each
 * package node concurrently; the tree walk itself always stays serial since sibling subtrees may
 * share state set up by a before hook. {@code runId} is echoed back on every streamed {@link TestEvent}.
 */
public class ExecuteTestsParams
{
    private String packagePath;
    private String uri;
    private List<String> functions;
    private List<TestInvocation> invocations;
    private Boolean recursive;
    private boolean parallel;
    private Boolean includeVanilla;
    private Boolean includePct;
    private String pctAdapterPath;
    private List<FileEntry> files;
    private String runId;

    public ExecuteTestsParams()
    {
    }

    public String getPackagePath()
    {
        return this.packagePath;
    }

    public void setPackagePath(String packagePath)
    {
        this.packagePath = packagePath;
    }

    public String getUri()
    {
        return this.uri;
    }

    public void setUri(String uri)
    {
        this.uri = uri;
    }

    public List<String> getFunctions()
    {
        return this.functions;
    }

    public void setFunctions(List<String> functions)
    {
        this.functions = functions;
    }

    public List<TestInvocation> getInvocations()
    {
        return this.invocations;
    }

    public void setInvocations(List<TestInvocation> invocations)
    {
        this.invocations = invocations;
    }

    /**
     * Defaults to true when the client omits it - a boxed Boolean rather than a primitive so that
     * "absent" is distinguishable from an explicit false.
     */
    public boolean isRecursive()
    {
        return this.recursive == null || this.recursive;
    }

    public void setRecursive(Boolean recursive)
    {
        this.recursive = recursive;
    }

    public boolean isParallel()
    {
        return this.parallel;
    }

    public void setParallel(boolean parallel)
    {
        this.parallel = parallel;
    }

    public boolean isIncludeVanilla()
    {
        return this.includeVanilla == null || this.includeVanilla;
    }

    public void setIncludeVanilla(Boolean includeVanilla)
    {
        this.includeVanilla = includeVanilla;
    }

    public boolean isIncludePct()
    {
        return this.includePct == null || this.includePct;
    }

    public void setIncludePct(Boolean includePct)
    {
        this.includePct = includePct;
    }

    public String getPctAdapterPath()
    {
        return this.pctAdapterPath;
    }

    public void setPctAdapterPath(String pctAdapterPath)
    {
        this.pctAdapterPath = pctAdapterPath;
    }

    public List<FileEntry> getFiles()
    {
        return this.files;
    }

    public void setFiles(List<FileEntry> files)
    {
        this.files = files;
    }

    public String getRunId()
    {
        return this.runId;
    }

    public void setRunId(String runId)
    {
        this.runId = runId;
    }
}
