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

package org.finos.legend.pure.lsp.diagnostics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.finos.legend.pure.lsp.LegendPureSession;
import org.finos.legend.pure.lsp.SourceInfoUtil;
import org.finos.legend.pure.lsp.UriMapper;
import org.finos.legend.pure.m3.exception.PureUnmatchedFunctionException;
import org.finos.legend.pure.m3.exception.PureUnresolvedIdentifierException;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiagnosticService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticService.class);
    private static final int MAX_CODE_ACTIONS = 20;

    private final LanguageClient client;
    private final UriMapper uriMapper;
    private final Map<String, List<Either<org.eclipse.lsp4j.Command, CodeAction>>> codeActionsByUri = new ConcurrentHashMap<>();

    public DiagnosticService(LanguageClient client, UriMapper uriMapper)
    {
        this.client = client;
        this.uriMapper = uriMapper;
    }

    public List<Diagnostic> fromException(Exception e)
    {
        PureException pureException = PureException.findPureException(e);
        SourceInformation sourceInfo = (pureException != null) ? pureException.getSourceInformation() : null;
        Range range = (sourceInfo != null)
                ? SourceInfoUtil.toRange(sourceInfo)
                : new Range(new Position(0, 0), new Position(0, 0));

        String message = (pureException != null) ? pureException.getInfo() : e.getMessage();
        if (message == null || message.isEmpty())
        {
            message = e.getMessage();
        }

        Diagnostic diagnostic = new Diagnostic(range, message, DiagnosticSeverity.Error, "legend-pure");
        diagnostic.setCode(codeFor(pureException));
        return Collections.singletonList(diagnostic);
    }

    public String resolveErrorUri(Exception e)
    {
        PureException pureException = PureException.findPureException(e);
        if (pureException != null)
        {
            SourceInformation sourceInformation = pureException.getSourceInformation();
            if (sourceInformation != null && sourceInformation.getSourceId() != null)
            {
                return this.uriMapper.toUri(sourceInformation.getSourceId());
            }
        }
        return null;
    }

    public void publishException(String fallbackUri, Exception e, LegendPureSession session)
    {
        String errorUri = resolveErrorUri(e);
        if (errorUri == null)
        {
            errorUri = fallbackUri;
        }
        List<Diagnostic> diagnostics = fromException(e);
        this.codeActionsByUri.put(errorUri, quickFixes(e, diagnostics, session, errorUri));
        publish(errorUri, diagnostics);
    }

    public void publish(String uri, List<Diagnostic> diagnostics)
    {
        if (this.client != null)
        {
            this.client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
        }
    }

    public void clear(String uri)
    {
        this.codeActionsByUri.remove(uri);
        this.client.publishDiagnostics(new PublishDiagnosticsParams(uri, Collections.emptyList()));
    }

    public List<Either<org.eclipse.lsp4j.Command, CodeAction>> codeActions(String uri)
    {
        return this.codeActionsByUri.getOrDefault(uri, Collections.emptyList());
    }

    public List<Either<org.eclipse.lsp4j.Command, CodeAction>> codeActions(String uri, List<Diagnostic> diagnostics)
    {
        List<Either<org.eclipse.lsp4j.Command, CodeAction>> cached = codeActions(uri);
        if (!cached.isEmpty() || diagnostics == null || diagnostics.isEmpty())
        {
            return cached;
        }

        List<ImportCandidate> imports = new ArrayList<>();
        for (Diagnostic diagnostic : diagnostics)
        {
            addImportsFromExceptionInfo(imports, diagnostic.getMessage());
        }
        return toCodeActionsForUri(imports, uri, 1, diagnostics);
    }

    private List<Either<org.eclipse.lsp4j.Command, CodeAction>> quickFixes(Exception e, List<Diagnostic> diagnostics,
                                                                           LegendPureSession session, String targetUri)
    {
        if (session == null || !session.isInitialized())
        {
            return Collections.emptyList();
        }
        PureException pureException = PureException.findPureException(e);
        if (pureException instanceof PureUnresolvedIdentifierException)
        {
            return unresolvedIdentifierFixes((PureUnresolvedIdentifierException) pureException, diagnostics, session, targetUri);
        }
        if (pureException instanceof PureUnmatchedFunctionException)
        {
            return unmatchedFunctionFixes((PureUnmatchedFunctionException) pureException, diagnostics, session, targetUri);
        }
        if (pureException != null)
        {
            List<ImportCandidate> imports = new ArrayList<>();
            addImportsFromExceptionInfo(imports, pureException.getInfo());
            addImportsFromExceptionInfo(imports, pureException.getMessage());
            if (!imports.isEmpty())
            {
                return toCodeActions(imports, pureException.getSourceInformation(), 1, diagnostics, session, targetUri);
            }
        }
        return Collections.emptyList();
    }

    private List<Either<org.eclipse.lsp4j.Command, CodeAction>> unresolvedIdentifierFixes(PureUnresolvedIdentifierException exception,
                                                                                          List<Diagnostic> diagnostics,
                                                                                          LegendPureSession session,
                                                                                          String targetUri)
    {
        PureRuntime runtime = session.getPureRuntime();
        RichIterable<CoreInstance> candidates = exception.getImportCandidates(runtime.getCodeStorage().getAllRepositories());
        List<ImportCandidate> imports = new ArrayList<>();
        for (CoreInstance candidate : candidates)
        {
            SourceInformation sourceInformation = candidate.getSourceInformation();
            if (sourceInformation != null)
            {
                String path = PackageableElement.getUserPathForPackageableElement(candidate);
                String id = simpleName(exception.getIdOrPath());
                int index = path.lastIndexOf(id);
                if (index >= 0)
                {
                    imports.add(new ImportCandidate(path, path.substring(0, index) + "*;"));
                }
            }
        }
        if (imports.isEmpty())
        {
            addImportsFromExceptionInfo(imports, exception.getInfo());
            addImportsFromExceptionInfo(imports, exception.getMessage());
        }
        SourceInformation target = importGroupSource(exception.getImportGroup());
        int insertionLine = importInsertionLine(target);
        if (target == null)
        {
            target = exception.getSourceInformation();
            insertionLine = 1;
        }
        return toCodeActions(imports, target, insertionLine, diagnostics, session, targetUri);
    }

    private List<Either<org.eclipse.lsp4j.Command, CodeAction>> unmatchedFunctionFixes(PureUnmatchedFunctionException exception,
                                                                                       List<Diagnostic> diagnostics,
                                                                                       LegendPureSession session,
                                                                                       String targetUri)
    {
        PureRuntime runtime = session.getPureRuntime();
        List<ImportCandidate> imports = new ArrayList<>();
        addFunctionImports(imports, exception.getFunctionName(), exception.getImportCandidatesWithPackageNotImported(), runtime);
        SourceInformation target = importGroupSource(exception.getImportGroup());
        int insertionLine = importInsertionLine(target);
        if (target == null)
        {
            target = exception.getSourceInformation();
            insertionLine = 1;
        }
        return toCodeActions(imports, target, insertionLine, diagnostics, session, targetUri);
    }

    private void addFunctionImports(List<ImportCandidate> imports, String functionName,
                                    ListIterable<CoreInstance> candidates, PureRuntime runtime)
    {
        for (CoreInstance candidate : candidates)
        {
            SourceInformation sourceInformation = candidate.getSourceInformation();
            if (sourceInformation == null)
            {
                continue;
            }
            StringBuilder builder = new StringBuilder();
            try
            {
                Function.print(builder, candidate, runtime.getProcessorSupport());
            }
            catch (Exception e)
            {
                LOGGER.debug("Could not print candidate function for quick fix", e);
                continue;
            }
            String path = builder.toString();
            String id = simpleName(functionName);
            int index = path.lastIndexOf(id);
            if (index >= 0)
            {
                imports.add(new ImportCandidate(path, path.substring(0, index) + "*;"));
            }
        }
    }

    private static void addImportsFromExceptionInfo(List<ImportCandidate> imports, String info)
    {
        if (info == null || !info.contains("possible matches"))
        {
            return;
        }
        String[] lines = info.split("\\R");
        for (String line : lines)
        {
            String candidate = line.trim();
            if (!candidate.contains("::"))
            {
                continue;
            }
            int end = candidate.length();
            while (end > 0)
            {
                char ch = candidate.charAt(end - 1);
                if (Character.isJavaIdentifierPart(ch) || ch == ':')
                {
                    break;
                }
                end--;
            }
            candidate = candidate.substring(0, end);
            int lastPackageSeparator = candidate.lastIndexOf("::");
            if (lastPackageSeparator > 0)
            {
                imports.add(new ImportCandidate(candidate, candidate.substring(0, lastPackageSeparator + 2) + "*;"));
            }
        }
    }

    private List<Either<org.eclipse.lsp4j.Command, CodeAction>> toCodeActions(List<ImportCandidate> imports,
                                                                               SourceInformation target,
                                                                               int insertionLine,
                                                                               List<Diagnostic> diagnostics,
                                                                               LegendPureSession session,
                                                                               String targetUri)
    {
        if (target == null || target.getSourceId() == null || imports.isEmpty())
        {
            return Collections.emptyList();
        }
        String targetSourceId = session.resolveSourceId(target.getSourceId());
        Source targetSource = targetSourceId == null ? null : session.getPureRuntime().getSourceById(targetSourceId);
        if (targetSource == null || targetSource.isImmutable())
        {
            return Collections.emptyList();
        }
        String uri = this.uriMapper.toUri(targetSourceId);
        if (uri == null)
        {
            uri = targetUri;
        }
        if (uri == null || uri.startsWith("pure://"))
        {
            return Collections.emptyList();
        }

        imports.sort(Comparator.comparing(candidate -> candidate.foundName));
        Map<String, ImportCandidate> uniqueImports = new LinkedHashMap<>();
        for (ImportCandidate candidate : imports)
        {
            uniqueImports.putIfAbsent(candidate.importStatement, candidate);
        }

        return toCodeActionsForUri(new ArrayList<>(uniqueImports.values()), uri, insertionLine, diagnostics);
    }

    private List<Either<org.eclipse.lsp4j.Command, CodeAction>> toCodeActionsForUri(List<ImportCandidate> imports, String uri,
                                                                                     int insertionLine,
                                                                                     List<Diagnostic> diagnostics)
    {
        if (uri == null || uri.startsWith("pure://") || imports.isEmpty())
        {
            return Collections.emptyList();
        }

        imports.sort(Comparator.comparing(candidate -> candidate.foundName));
        Map<String, ImportCandidate> uniqueImports = new LinkedHashMap<>();
        for (ImportCandidate candidate : imports)
        {
            uniqueImports.putIfAbsent(candidate.importStatement, candidate);
        }

        List<Either<org.eclipse.lsp4j.Command, CodeAction>> actions = new ArrayList<>();
        Position position = new Position(Math.max(insertionLine - 1, 0), 0);
        Range range = new Range(position, position);
        for (ImportCandidate candidate : uniqueImports.values())
        {
            CodeAction action = new CodeAction("Import " + candidate.foundName);
            action.setKind(CodeActionKind.QuickFix);
            action.setDiagnostics(diagnostics);
            action.setIsPreferred(actions.isEmpty());
            action.setEdit(new WorkspaceEdit(Collections.singletonMap(
                    uri,
                    Collections.singletonList(new TextEdit(range, "import " + candidate.importStatement + "\n"))
            )));
            actions.add(Either.forRight(action));
            if (actions.size() >= MAX_CODE_ACTIONS)
            {
                break;
            }
        }
        return actions;
    }

    private static SourceInformation importGroupSource(CoreInstance importGroup)
    {
        return importGroup == null ? null : importGroup.getSourceInformation();
    }

    private static int importInsertionLine(SourceInformation importGroupSource)
    {
        if (importGroupSource == null)
        {
            return 1;
        }
        int line = importGroupSource.getStartLine();
        return importGroupSource.getStartColumn() == 0 ? line + 1 : line;
    }

    private static String simpleName(String path)
    {
        int index = path == null ? -1 : path.lastIndexOf("::");
        return index < 0 ? path : path.substring(index + 2);
    }

    private static String codeFor(PureException pureException)
    {
        if (pureException instanceof PureUnresolvedIdentifierException)
        {
            return "pure.unresolvedIdentifier";
        }
        if (pureException instanceof PureUnmatchedFunctionException)
        {
            return "pure.unmatchedFunction";
        }
        if (pureException instanceof PureParserException)
        {
            return "pure.parser";
        }
        if (pureException instanceof PureCompilationException)
        {
            return "pure.compiler";
        }
        return pureException == null ? "pure.internal" : "pure.error";
    }

    private static class ImportCandidate
    {
        private final String foundName;
        private final String importStatement;

        private ImportCandidate(String foundName, String importStatement)
        {
            this.foundName = foundName;
            this.importStatement = importStatement;
        }
    }
}
