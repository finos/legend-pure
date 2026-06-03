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

package org.finos.legend.pure.lsp;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.lsp.mutation.SourceMutationService;
import org.finos.legend.pure.m3.execution.Console;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegendPureSession
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendPureSession.class);
    private static final String DEBUG_REPOSITORY_NAME = "pure_ide_debug";

    private volatile PureRuntime pureRuntime;
    private volatile FunctionExecution functionExecution;
    private volatile boolean initialized;
    private final SourceMutationService mutationService = new SourceMutationService(this);

    private volatile RepositoryScanner workspaceScanner;
    private volatile Set<String> classpathRepositoryNames = Collections.emptySet();

    public void initialize()
    {
        initialize(null);
    }

    public void initialize(RepositoryScanner scanner)
    {
        initialize(scanner, this.classpathRepositoryNames);
    }

    public void initialize(RepositoryScanner scanner, Collection<String> classpathRepositoryNames)
    {
        long start = System.currentTimeMillis();
        this.workspaceScanner = scanner;
        this.classpathRepositoryNames = normalizeRepositoryNames(classpathRepositoryNames);

        this.pureRuntime = newRuntime(scanner, true, this.classpathRepositoryNames);

        this.functionExecution = new StackPreservingFunctionExecutionInterpreted();
        this.functionExecution.init(this.pureRuntime, new Message(""));
        LspLog.info("StackPreservingFunctionExecutionInterpreted initialized");

        this.initialized = true;
        long elapsed = (System.currentTimeMillis() - start) / 1000;
        LOGGER.info("Pure runtime initialized in {}s", elapsed);
    }

    public static PureRuntime newRuntime(RepositoryScanner scanner, boolean includeWorkspaceStorages, Collection<String> classpathRepositoryNames)
    {
        return newRuntime(scanner, includeWorkspaceStorages, classpathRepositoryNames, Collections.emptySet());
    }

    public static PureRuntime newDebugRuntime(RepositoryScanner scanner, Collection<String> classpathRepositoryNames)
    {
        Set<String> normalizedClasspathRepositoryNames = normalizeRepositoryNames(classpathRepositoryNames);
        return newRuntime(scanner, true, normalizedClasspathRepositoryNames, Collections.singleton(DEBUG_REPOSITORY_NAME),
                true, normalizedClasspathRepositoryNames);
    }

    private static PureRuntime newRuntime(RepositoryScanner scanner, boolean includeWorkspaceStorages, Collection<String> classpathRepositoryNames,
                                          Collection<String> additionalWorkspaceDependencies)
    {
        return newRuntime(scanner, includeWorkspaceStorages, classpathRepositoryNames, additionalWorkspaceDependencies, false);
    }

    private static PureRuntime newRuntime(RepositoryScanner scanner, boolean includeWorkspaceStorages, Collection<String> classpathRepositoryNames,
                                          Collection<String> additionalWorkspaceDependencies, boolean workspaceDefinitionsOnly)
    {
        return newRuntime(scanner, includeWorkspaceStorages, classpathRepositoryNames, additionalWorkspaceDependencies,
                workspaceDefinitionsOnly, Collections.emptySet());
    }

    private static PureRuntime newRuntime(RepositoryScanner scanner, boolean includeWorkspaceStorages, Collection<String> classpathRepositoryNames,
                                          Collection<String> additionalWorkspaceDependencies, boolean workspaceDefinitionsOnly,
                                          Collection<String> excludedWorkspaceRepositoryNames)
    {
        Set<String> normalizedClasspathRepositoryNames = normalizeRepositoryNames(classpathRepositoryNames);
        MutableList<RepositoryCodeStorage> storages = Lists.mutable.empty();
        Set<String> workspaceRepoNames = Collections.emptySet();

        if (includeWorkspaceStorages && scanner != null && !scanner.getMappings().isEmpty())
        {
            MutableList<RepositoryCodeStorage> workspaceStorages = workspaceDefinitionsOnly
                    ? scanner.buildWorkspaceDefinitionStorages(additionalWorkspaceDependencies, excludedWorkspaceRepositoryNames)
                    : scanner.buildWorkspaceStorages(additionalWorkspaceDependencies);
            storages.addAll(workspaceStorages);
            workspaceRepoNames = workspaceDefinitionsOnly
                    ? filteredWorkspaceRepoNames(scanner.getWorkspaceRepoNames(), excludedWorkspaceRepositoryNames)
                    : scanner.getWorkspaceRepoNames();
            LspLog.debug("Loaded " + workspaceStorages.size()
                    + (workspaceDefinitionsOnly
                    ? " workspace repo definition storage(s)"
                    : " workspace repos (MutableFS from disk)"));
        }

        org.eclipse.collections.api.RichIterable<CodeRepository> classpathRepos =
                CodeRepositoryProviderHelper.findCodeRepositories();
        Set<String> finalWorkspaceNames = workspaceRepoNames;
        Set<String> unresolvedClasspathRepositoryNames = new LinkedHashSet<>(normalizedClasspathRepositoryNames);
        MutableList<CodeRepository> classpathStorageRepos = Lists.mutable.empty();
        for (CodeRepository repo : classpathRepos)
        {
            String name = repo.getName();
            if (shouldLoadClasspathRepository(name, finalWorkspaceNames, normalizedClasspathRepositoryNames))
            {
                classpathStorageRepos.add(repo);
                if (name != null)
                {
                    unresolvedClasspathRepositoryNames.remove(name);
                }
            }
            else if (name != null && normalizedClasspathRepositoryNames.contains(name) && finalWorkspaceNames.contains(name))
            {
                unresolvedClasspathRepositoryNames.remove(name);
                LspLog.debug("Configured classpath repo is loaded from workspace instead: " + name);
            }
            else if (!finalWorkspaceNames.contains(name))
            {
                LspLog.debug("Skipping repo not on disk: " + name);
            }
        }
        if (!classpathStorageRepos.isEmpty())
        {
            storages.add(new ClassLoaderCodeStorage(classpathStorageRepos));
            LspLog.debug("Loaded " + classpathStorageRepos.size()
                    + " classpath repos (platform/default + configured)");
        }
        if (!unresolvedClasspathRepositoryNames.isEmpty())
        {
            LspLog.warn("Configured classpath repo(s) not found on runtime classpath: "
                    + unresolvedClasspathRepositoryNames);
        }

        LOGGER.info("Building PureRuntime with {} storage(s)...", storages.size());
        CompositeCodeStorage codeStorage = new CompositeCodeStorage(storages.toArray(new RepositoryCodeStorage[0]));

        PureRuntime runtime = new PureRuntimeBuilder(codeStorage)
                .withMessage(new Message(""))
                .setUseFastCompiler(true)
                .build();

        LOGGER.info("Initializing Pure runtime...");
        runtime.initialize(new Message("")
        {
            @Override
            public void setMessage(String message)
            {
                super.setMessage(message);
                LOGGER.info(message);
            }
        });

        return runtime;
    }

    private static Set<String> filteredWorkspaceRepoNames(Set<String> workspaceRepositoryNames, Collection<String> excludedRepositoryNames)
    {
        if (workspaceRepositoryNames == null || workspaceRepositoryNames.isEmpty())
        {
            return Collections.emptySet();
        }
        if (excludedRepositoryNames == null || excludedRepositoryNames.isEmpty())
        {
            return workspaceRepositoryNames;
        }
        Set<String> filtered = new LinkedHashSet<>(workspaceRepositoryNames);
        filtered.removeAll(excludedRepositoryNames);
        return Collections.unmodifiableSet(filtered);
    }

    public synchronized void reinitialize()
    {
        this.initialized = false;
        this.pureRuntime = null;
        initialize(this.workspaceScanner, this.classpathRepositoryNames);
    }

    public synchronized void setClasspathRepositoryNames(Collection<String> classpathRepositoryNames)
    {
        this.classpathRepositoryNames = normalizeRepositoryNames(classpathRepositoryNames);
    }

    public SourceMutationService getMutationService()
    {
        return this.mutationService;
    }

    public synchronized void restoreFromDisk(String sourceId)
    {
        this.mutationService.restoreFromDisk(sourceId);
    }

    public synchronized CompileResult modifyAndCompile(String sourceId, String content)
    {
        return this.mutationService.modifyAndCompile(sourceId, content);
    }

    public synchronized CompileResult applyBulkChangesAndCompile(List<FileChange> changes)
    {
        return this.mutationService.applyBulkChangesAndCompile(changes);
    }

    public synchronized ExecuteResult executeGo()
    {
        if (!this.initialized)
        {
            return new ExecuteResult(false, "Runtime not initialized", null);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Console console = this.functionExecution.getConsole();
        try
        {
            this.pureRuntime.compile();

            CoreInstance goFunction = this.pureRuntime.getFunction("go():Any[*]");
            if (goFunction == null)
            {
                goFunction = this.pureRuntime.getFunction("go():String[*]");
            }
            if (goFunction == null)
            {
                goFunction = this.pureRuntime.getFunction("go():String[1]");
            }
            if (goFunction == null)
            {
                LOGGER.info("Could not find go() with standard signatures, searching model...");
                CoreInstance goByPath = this.pureRuntime.getCoreInstance("go__Any_MANY_");
                if (goByPath == null)
                {
                    goByPath = this.pureRuntime.getCoreInstance("go__String_MANY_");
                }
                if (goByPath == null)
                {
                    goByPath = this.pureRuntime.getCoreInstance("go__String_1_");
                }
                goFunction = goByPath;
            }
            if (goFunction == null)
            {
                LspLog.info("executeGo: no go() function found");
                return new ExecuteResult(false, "No go() function found in compiled sources. " +
                        "Define: function go():Any[*] { ... }", null);
            }
            LspLog.debug("executeGo: found function " + goFunction.getClassifier().getName()
                    + " at " + (goFunction.getSourceInformation() != null ? goFunction.getSourceInformation().getSourceId() : "unknown"));

            PrintStream capturePrintStream = new PrintStream(baos, true);
            console.setPrintStream(capturePrintStream);
            console.setConsole(true);

            this.functionExecution.start(goFunction, FastList.newList());

            String consoleOutput = baosToString(baos);
            if (consoleOutput.isEmpty())
            {
                consoleOutput = "(go() returned successfully with no console output. Use print() to see results.)";
            }

            LspLog.debug("executeGo completed, output length: " + consoleOutput.length());
            return new ExecuteResult(true, null, consoleOutput);
        }
        catch (Exception e)
        {
            LOGGER.error("executeGo failed", e);
            String errorText = formatExecutionFailure(e, baos);
            LspLog.debug("executeGo failed: " + errorText);
            return new ExecuteResult(false, errorText, errorText);
        }
        finally
        {
            console.setPrintStream(new PrintStream(new ByteArrayOutputStream(), true));
            console.setConsole(false);
        }
    }

    private String formatExecutionFailure(Exception e, ByteArrayOutputStream consoleOutput)
    {
        StringBuilder builder = new StringBuilder();
        String printed = baosToString(consoleOutput);
        if (!printed.isEmpty())
        {
            builder.append(printed);
            if (!printed.endsWith("\n"))
            {
                builder.append('\n');
            }
        }

        PureException pureException = PureException.findPureException(e);
        if (pureException != null)
        {
            PureException original = pureException.getOriginatingPureException();
            if (original == null)
            {
                original = pureException;
            }
            if (pureException instanceof PureExecutionException)
            {
                builder.append(original.getMessage()).append('\n');
                StringBuffer buffer = new StringBuffer();
                ((PureExecutionException) pureException).printPureStackTrace(
                        buffer, "", this.functionExecution.getProcessorSupport());
                builder.append(buffer);
            }
            else if (pureException.hasPureStackTrace())
            {
                builder.append(original.getMessage()).append('\n')
                        .append(pureException.getPureStackTrace("    "));
            }
            else
            {
                builder.append(original.getMessage());
            }
        }
        else
        {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            builder.append(writer);
        }

        return builder.toString();
    }

    private static String baosToString(ByteArrayOutputStream baos)
    {
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    private static boolean isPlatformRepo(String name)
    {
        return "platform".equals(name) || name.startsWith("platform_");
    }

    static boolean shouldLoadClasspathRepository(String name, Set<String> workspaceRepoNames, Set<String> classpathRepositoryNames)
    {
        if (name == null)
        {
            return true;
        }
        if (workspaceRepoNames.contains(name))
        {
            return false;
        }
        return isPlatformRepo(name) || DEBUG_REPOSITORY_NAME.equals(name) || classpathRepositoryNames.contains(name);
    }

    private static Set<String> normalizeRepositoryNames(Collection<String> repositoryNames)
    {
        if (repositoryNames == null || repositoryNames.isEmpty())
        {
            return Collections.emptySet();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String repositoryName : repositoryNames)
        {
            if (repositoryName != null)
            {
                String trimmed = repositoryName.trim();
                if (!trimmed.isEmpty())
                {
                    normalized.add(trimmed);
                }
            }
        }
        return normalized.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(normalized);
    }

    public PureRuntime getPureRuntime()
    {
        return this.pureRuntime;
    }

    public FunctionExecution getFunctionExecution()
    {
        return this.functionExecution;
    }

    public Set<String> getClasspathRepositoryNames()
    {
        return this.classpathRepositoryNames;
    }

    public boolean isInitialized()
    {
        return this.initialized;
    }

    public String resolveSourceId(String sourceId)
    {
        if (this.pureRuntime.getSourceById(sourceId) != null)
        {
            return sourceId;
        }
        String alt = sourceId.startsWith("/") ? sourceId.substring(1) : "/" + sourceId;
        if (this.pureRuntime.getSourceById(alt) != null)
        {
            return alt;
        }
        return null;
    }

    public static class ExecuteResult
    {
        private final boolean success;
        private final String error;
        private final String output;

        ExecuteResult(boolean success, String error, String output)
        {
            this.success = success;
            this.error = error;
            this.output = output;
        }

        public boolean isSuccess()
        {
            return this.success;
        }

        public String getError()
        {
            return this.error;
        }

        public String getOutput()
        {
            return this.output;
        }
    }

    public enum FileChangeType
    {
        CREATE_OR_MODIFY,
        DELETE
    }

    public static class FileChange
    {
        private final String sourceId;
        private final String content;
        private final FileChangeType type;

        public FileChange(String sourceId, String content, FileChangeType type)
        {
            this.sourceId = sourceId;
            this.content = content;
            this.type = type;
        }

        public String getSourceId()
        {
            return this.sourceId;
        }

        public String getContent()
        {
            return this.content;
        }

        public FileChangeType getType()
        {
            return this.type;
        }
    }

    public static class CompileResult
    {
        private final boolean ready;
        private final boolean success;
        private final boolean internalError;
        private final Exception error;
        private final List<String> modifiedFiles;

        private CompileResult(boolean ready, boolean success, boolean internalError, Exception error, List<String> modifiedFiles)
        {
            this.ready = ready;
            this.success = success;
            this.internalError = internalError;
            this.error = error;
            this.modifiedFiles = modifiedFiles;
        }

        public static CompileResult notReady()
        {
            return new CompileResult(false, false, false, null, Collections.emptyList());
        }

        public static CompileResult success(Iterable<String> modifiedFiles)
        {
            return new CompileResult(true, true, false, null, Lists.mutable.withAll(modifiedFiles));
        }

        public static CompileResult error(Exception e, boolean internal)
        {
            return new CompileResult(true, false, internal, e, Collections.emptyList());
        }

        public boolean isReady()
        {
            return this.ready;
        }

        public boolean isSuccess()
        {
            return this.success;
        }

        public boolean isInternalError()
        {
            return this.internalError;
        }

        public Exception getError()
        {
            return this.error;
        }

        public List<String> getModifiedFiles()
        {
            return this.modifiedFiles;
        }
    }
}
