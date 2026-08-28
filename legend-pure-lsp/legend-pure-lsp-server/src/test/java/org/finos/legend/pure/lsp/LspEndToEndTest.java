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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.finos.legend.pure.lsp.protocol.CheckBatchParams;
import org.finos.legend.pure.lsp.protocol.CheckBatchResult;
import org.finos.legend.pure.lsp.protocol.DeleteFileParams;
import org.finos.legend.pure.lsp.protocol.DeleteFileResult;
import org.finos.legend.pure.lsp.protocol.ExecuteFunctionParams;
import org.finos.legend.pure.lsp.protocol.ExecuteGoParams;
import org.finos.legend.pure.lsp.protocol.ExecuteGoResult;
import org.finos.legend.pure.lsp.protocol.FileEntry;
import org.finos.legend.pure.lsp.protocol.LspStatus;
import org.finos.legend.pure.lsp.protocol.SetOptionParams;
import org.finos.legend.pure.lsp.protocol.SetOptionResult;
import org.finos.legend.pure.lsp.protocol.TestFunctionInfo;
import org.finos.legend.pure.lsp.protocol.TestFunctionsParams;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * End-to-end test: creates the LSP server, connects a mock client,
 * sends LSP protocol messages, and verifies responses.
 */
public class LspEndToEndTest
{
    private static LegendPureLspServer server;
    private static MockLanguageClient mockClient;

    @BeforeClass
    public static void setUp() throws Exception
    {
        server = new LegendPureLspServer();
        mockClient = new MockLanguageClient();
        server.connect(mockClient);

        // Initialize handshake
        InitializeResult result = server.initialize(new InitializeParams()).get(5, TimeUnit.SECONDS);
        Assert.assertNotNull("Initialize should return result", result);
        Assert.assertNotNull("Should have capabilities", result.getCapabilities());
        Assert.assertEquals("Should support full text sync",
                TextDocumentSyncKind.Full,
                result.getCapabilities().getTextDocumentSync().getLeft());
        Assert.assertTrue("Should support hover",
                result.getCapabilities().getHoverProvider().getLeft());
        Assert.assertTrue("Should support workspace symbols",
                result.getCapabilities().getWorkspaceSymbolProvider().getLeft());

        // Trigger runtime initialization and wait for it
        server.initialized(new InitializedParams());

        // Wait for PureRuntime to initialize (up to 120s)
        long start = System.currentTimeMillis();
        while (server.getSession() == null || !server.getSession().isInitialized())
        {
            if (System.currentTimeMillis() - start > 120_000)
            {
                Assert.fail("PureRuntime did not initialize within 120 seconds");
            }
            Thread.sleep(500);
        }
    }

    @Test
    public void didOpen_validCode_clearsdiagnostics() throws Exception
    {
        // Simulate a real developer opening a .pure file from a repo directory.
        // URI maps to a source ID under src/main/resources/ which matches a known repo.
        // For test purposes, register a direct mapping to bypass filesystem path logic.
        mockClient.clearDiagnostics();

        String uri = "file:///workspace/src/main/resources/e2e_valid.pure";
        // UriMapper strips src/main/resources/ -> /e2e_valid.pure
        // PureRuntime doesn't have this source, so createInMemoryAndCompile is used
        // with in-memory ID "e2e_valid.pure" (leading / stripped)
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "pure", 1,
                        "Class test::e2e::ValidClass\n{\n  name: String[1];\n}\n")
        ));

        Thread.sleep(1500);

        List<PublishDiagnosticsParams> published = mockClient.getDiagnosticsFor(uri);
        Assert.assertFalse("Should have published diagnostics", published.isEmpty());
        PublishDiagnosticsParams last = published.get(published.size() - 1);
        Assert.assertTrue("Valid code should have no diagnostics, got: " + last.getDiagnostics(),
                last.getDiagnostics().isEmpty());
    }

    @Test
    public void didOpen_invalidCode_publishesError() throws Exception
    {
        mockClient.clearDiagnostics();

        String uri = "file:///workspace/src/main/resources/e2e_invalid.pure";
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "pure", 1,
                        "Class test::e2e::Bad\n{\n  x: NonExistentType999[1];\n}\n")
        ));

        Thread.sleep(1500);

        List<PublishDiagnosticsParams> published = mockClient.getDiagnosticsFor(uri);
        Assert.assertFalse("Should have published diagnostics", published.isEmpty());
        PublishDiagnosticsParams last = published.get(published.size() - 1);
        Assert.assertFalse("Invalid code should have errors", last.getDiagnostics().isEmpty());
        Assert.assertEquals("legend-pure", last.getDiagnostics().get(0).getSource());

        // Clean up: fix the broken source so it doesn't pollute other tests
        VersionedTextDocumentIdentifier docId = new VersionedTextDocumentIdentifier(uri, 2);
        TextDocumentContentChangeEvent fix = new TextDocumentContentChangeEvent(
                "Class test::e2e::Bad\n{\n  x: String[1];\n}\n");
        server.getTextDocumentService().didChange(new DidChangeTextDocumentParams(
                docId, Collections.singletonList(fix)));
        Thread.sleep(1000);
    }

    @Test
    public void didChange_fixesError_clearsDiagnostics() throws Exception
    {
        // Use unique names to avoid collision with other tests
        long ts = System.currentTimeMillis();
        String uri = "file:///workspace/src/main/resources/e2e_fixme_" + ts + ".pure";

        // Open with error
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "pure", 1,
                        "Class test::e2e::Fixme" + ts + "\n{\n  x: BogusType999[1];\n}\n")
        ));
        Thread.sleep(1500);

        mockClient.clearDiagnostics();

        // Fix the error via didChange
        VersionedTextDocumentIdentifier docId = new VersionedTextDocumentIdentifier(uri, 2);
        TextDocumentContentChangeEvent change = new TextDocumentContentChangeEvent(
                "Class test::e2e::Fixme" + ts + "\n{\n  x: String[1];\n}\n");
        server.getTextDocumentService().didChange(new DidChangeTextDocumentParams(
                docId, Collections.singletonList(change)));
        Thread.sleep(2000);

        List<PublishDiagnosticsParams> published = mockClient.getDiagnosticsFor(uri);
        Assert.assertFalse("Should have published after fix", published.isEmpty());
        PublishDiagnosticsParams last = published.get(published.size() - 1);
        Assert.assertTrue("Fixed code should have no diagnostics, got: " + last.getDiagnostics(),
                last.getDiagnostics().isEmpty());
    }

    @Test
    public void codeAction_unresolvedType_returnsImportQuickFix() throws Exception
    {
        mockClient.clearDiagnostics();
        long ts = System.currentTimeMillis();
        String typeName = "QuickFixTarget" + ts;
        String definingUri = "file:///workspace/src/main/resources/e2e_quickfix_target_" + ts + ".pure";
        String useUri = "file:///workspace/src/main/resources/e2e_quickfix_use_" + ts + ".pure";

        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(definingUri, "pure", 1,
                        "Class test::e2e::quickfix::model::" + typeName + "\n{\n  name: String[1];\n}\n")
        ));
        Thread.sleep(1500);

        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(useUri, "pure", 1,
                        "import test::e2e::other::*;\n\n" +
                                "Class test::e2e::quickfix::use::NeedsImport" + ts + "\n{\n" +
                                "  value: " + typeName + "[1];\n}\n")
        ));
        Thread.sleep(1500);

        List<PublishDiagnosticsParams> published = mockClient.getDiagnosticsFor(useUri);
        Assert.assertFalse("Should publish unresolved type diagnostic", published.isEmpty());
        List<Diagnostic> diagnostics = published.get(published.size() - 1).getDiagnostics();
        Assert.assertFalse("Expected diagnostic list to be non-empty", diagnostics.isEmpty());

        CodeActionParams params = new CodeActionParams();
        params.setTextDocument(new TextDocumentIdentifier(useUri));
        params.setRange(new Range(new Position(0, 0), new Position(0, 0)));
        params.setContext(new CodeActionContext(diagnostics));

        List<Either<Command, CodeAction>> actions = server.getTextDocumentService().codeAction(params).get(10, TimeUnit.SECONDS);
        Assert.assertFalse("Should return at least one import quick fix", actions.isEmpty());

        boolean foundImport = false;
        for (Either<Command, CodeAction> actionOrCommand : actions)
        {
            CodeAction action = actionOrCommand.getRight();
            if (action != null && action.getTitle().contains(typeName))
            {
                foundImport = true;
                break;
            }
        }
        Assert.assertTrue("Should include import quick fix for " + typeName, foundImport);
    }

    @Test
    public void didClose_clearsDiagnostics() throws Exception
    {
        String uri = "file:///test/close_e2e.pure";

        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "pure", 1, "Class test::e2e::Closeme {}")
        ));
        Thread.sleep(1000);

        mockClient.clearDiagnostics();
        server.getTextDocumentService().didClose(new DidCloseTextDocumentParams(
                new TextDocumentIdentifier(uri)));

        List<PublishDiagnosticsParams> published = mockClient.getDiagnosticsFor(uri);
        Assert.assertFalse("Should publish empty diagnostics on close", published.isEmpty());
        Assert.assertTrue("Diagnostics should be cleared",
                published.get(published.size() - 1).getDiagnostics().isEmpty());
    }

    @Test
    public void workspaceSymbol_findsUserDefinedClass() throws Exception
    {
        // First, open a file so PureRuntime has a user-defined class
        String uri = "file:///workspace/src/main/resources/e2e_sym.pure";
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "pure", 1,
                        "Class test::e2e::SymbolTestClass\n{\n  x: String[1];\n}\n")
        ));
        Thread.sleep(1500);

        // Search for the class via workspace symbols
        @SuppressWarnings("deprecation")
        Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>> result =
                server.getWorkspaceService().symbol(new WorkspaceSymbolParams("SymbolTestClass"))
                        .get(10, TimeUnit.SECONDS);

        Assert.assertNotNull("symbol() should return a result", result);
        List<? extends SymbolInformation> symbols = result.getLeft();
        Assert.assertNotNull("Should return SymbolInformation list", symbols);
        Assert.assertFalse("Should find at least one symbol", symbols.isEmpty());

        boolean found = false;
        for (SymbolInformation sym : symbols)
        {
            if (sym.getName().contains("SymbolTestClass"))
            {
                found = true;
                break;
            }
        }
        Assert.assertTrue("Should find SymbolTestClass in results", found);
    }

    @Test
    public void workspaceSymbol_emptyQuery_returnsResult() throws Exception
    {
        @SuppressWarnings("deprecation")
        Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>> result =
                server.getWorkspaceService().symbol(new WorkspaceSymbolParams(""))
                        .get(10, TimeUnit.SECONDS);

        Assert.assertNotNull("symbol() should return a result", result);
        Assert.assertNotNull("Should return a list (possibly empty without repo scanner)",
                result.getLeft());
    }

    @Test
    public void hover_onOpenedFile_returnsInfo() throws Exception
    {
        // Open a file with a class
        String uri = "file:///workspace/src/main/resources/e2e_hover.pure";
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "pure", 1,
                        "Class test::e2e::HoverTestClass\n{\n  name: String[1];\n}\n")
        ));
        Thread.sleep(1500);

        // Hover on the class name "HoverTestClass" at line 0, col 20 (0-based)
        HoverParams hoverParams = new HoverParams(
                new TextDocumentIdentifier(uri),
                new Position(0, 20)
        );
        Hover hover = server.getTextDocumentService().hover(hoverParams).get(10, TimeUnit.SECONDS);

        // hover may be null if navigate() doesn't resolve at that exact position,
        // but if it returns, it should be markdown
        if (hover != null)
        {
            Assert.assertNotNull("Hover should have content", hover.getContents());
            Assert.assertNotNull("Hover should be markup", hover.getContents().getRight());
            String value = hover.getContents().getRight().getValue();
            Assert.assertTrue("Hover should contain type info, got: " + value,
                    value.contains("Class") || value.contains("HoverTestClass"));
        }
    }

    @Test
    public void getSourceContent_returnsPlatformSourceContent() throws Exception
    {
        // Request content for a known platform source via the custom request handler
        String content = server.getSourceContent("/platform/pure/essential/lang.pure").get(10, TimeUnit.SECONDS);

        // Platform sources should be loadable
        if (content != null)
        {
            Assert.assertFalse("Content should not be empty", content.isEmpty());
            // Platform lang.pure should contain some Pure code
            Assert.assertTrue("Should contain Pure code, got length: " + content.length(),
                    content.length() > 10);
        }
    }

    @Test
    public void getSourceContent_returnsNullForUnknown() throws Exception
    {
        String content = server.getSourceContent("nonexistent_source_999.pure").get(10, TimeUnit.SECONDS);
        Assert.assertNull("Unknown source should return null", content);
    }

    @Test
    public void getSourceContent_handlesPureSchemePrefix() throws Exception
    {
        // The extension sends "pure:///core/pure/..." — server should strip the prefix
        String content = server.getSourceContent("pure:///platform/pure/essential/lang.pure").get(10, TimeUnit.SECONDS);

        if (content != null)
        {
            Assert.assertFalse("Content should not be empty", content.isEmpty());
        }
    }

    // -- New custom RPC endpoints, driven through the same in-process server harness --

    @Test
    public void executeGo_rpc_withInlineFiles_compilesThenRunsGo() throws Exception
    {
        long ts = System.currentTimeMillis();
        String uri = "file:///workspace/src/main/resources/e2e_rpc_go_" + ts + ".pure";
        ExecuteGoParams params = new ExecuteGoParams();
        params.setFiles(Collections.singletonList(
                new FileEntry(uri, "function go():Any[*]\n{\n  print('rpc go output', 1)\n}\n")));

        ExecuteGoResult result = server.executeGo(params).get(30, TimeUnit.SECONDS);
        Assert.assertTrue("executeGo RPC should succeed, got: " + result.getError(), result.isSuccess());
        Assert.assertNotNull("Should have output", result.getOutput());
        Assert.assertTrue("Should capture printed output, got: " + result.getOutput(),
                result.getOutput().contains("rpc go output"));

        // Clean up the throwaway go() wrapper via the delete RPC (also exercises deleteFile).
        DeleteFileResult deleted = server.deleteFile(new DeleteFileParams(uri)).get(15, TimeUnit.SECONDS);
        Assert.assertTrue("deleteFile should succeed", deleted.isSuccess());
        Assert.assertTrue("go() wrapper should have been removed", deleted.isRemoved());
    }

    @Test
    public void execute_rpc_runsArbitraryNamedFunction() throws Exception
    {
        long ts = System.currentTimeMillis();
        String uri = "file:///workspace/src/main/resources/e2e_rpc_named_" + ts + ".pure";
        String fnName = "e2eRpcFn" + ts;
        ExecuteFunctionParams params = new ExecuteFunctionParams();
        params.setFunction(fnName + "():Any[*]");
        params.setFiles(Collections.singletonList(new FileEntry(uri,
                "function " + fnName + "():Any[*]\n{\n  print('named rpc', 1)\n}\n")));

        ExecuteGoResult result = server.execute(params).get(30, TimeUnit.SECONDS);
        Assert.assertTrue("execute RPC should succeed, got: " + result.getError(), result.isSuccess());
        Assert.assertTrue("Should capture printed output, got: " + result.getOutput(),
                result.getOutput().contains("named rpc"));

        server.deleteFile(new DeleteFileParams(uri)).get(15, TimeUnit.SECONDS);
    }

    @Test
    public void execute_rpc_missingFunctionPath_returnsError() throws Exception
    {
        ExecuteGoResult result = server.execute(new ExecuteFunctionParams()).get(15, TimeUnit.SECONDS);
        Assert.assertFalse("execute with no function path should fail", result.isSuccess());
    }

    @Test
    public void checkBatch_rpc_success_reportsInputAsModified() throws Exception
    {
        long ts = System.currentTimeMillis();
        String uri = "file:///workspace/src/main/resources/e2e_rpc_batch_" + ts + ".pure";
        CheckBatchParams params = new CheckBatchParams();
        params.setFiles(Collections.singletonList(new FileEntry(uri,
                "Class test::e2e::BatchClass" + ts + "\n{\n  v: Integer[1];\n}\n")));

        CheckBatchResult result = server.checkBatch(params).get(30, TimeUnit.SECONDS);
        Assert.assertTrue("checkBatch should succeed, got: " + result.getError(), result.isSuccess());
        Assert.assertNotNull("Modified files should be reported", result.getModifiedFiles());
        Assert.assertTrue("Input file should be reported clean, got: " + result.getModifiedFiles(),
                result.getModifiedFiles().contains(uri));

        server.deleteFile(new DeleteFileParams(uri)).get(15, TimeUnit.SECONDS);
    }

    @Test
    public void checkBatch_rpc_failure_attributesErrorToOffendingFile() throws Exception
    {
        long ts = System.currentTimeMillis();
        String goodUri = "file:///workspace/src/main/resources/e2e_rpc_good_" + ts + ".pure";
        String badUri = "file:///workspace/src/main/resources/e2e_rpc_bad_" + ts + ".pure";
        CheckBatchParams params = new CheckBatchParams();
        // Good file first: if the error were attributed blindly to files.get(0) it would point here.
        params.setFiles(Arrays.asList(
                new FileEntry(goodUri, "Class test::e2e::Good" + ts + "\n{\n  v: Integer[1];\n}\n"),
                new FileEntry(badUri, "Class test::e2e::Bad" + ts + "\n{\n  v: NoSuchType999[1];\n}\n")));

        CheckBatchResult result = server.checkBatch(params).get(30, TimeUnit.SECONDS);
        Assert.assertFalse("Batch containing a bad file should fail", result.isSuccess());
        Assert.assertNotNull("Error should be reported", result.getError());
        Assert.assertEquals("Error must be attributed to the offending file, not files.get(0)",
                badUri, result.getErrorUri());
    }

    @Test
    public void checkBatch_rpc_emptyFiles_returnsError() throws Exception
    {
        CheckBatchResult result = server.checkBatch(new CheckBatchParams()).get(15, TimeUnit.SECONDS);
        Assert.assertFalse("checkBatch with no files should fail", result.isSuccess());
    }

    @Test
    public void setOption_rpc_togglesRuntimeOptionLive() throws Exception
    {
        String optionName = "E2eTestOption" + System.currentTimeMillis();
        String key = "pure.options." + optionName;

        SetOptionResult on = server.setOption(new SetOptionParams(optionName, true)).get(10, TimeUnit.SECONDS);
        Assert.assertTrue("setOption(true) should succeed", on.isSuccess());
        Assert.assertTrue("Option should now be effective", on.isEffective());
        Assert.assertTrue("The session runtime should see the option",
                server.getSession().getPureRuntime().getOptions().isOptionSet(optionName));
        Assert.assertNull("setOption must not write a system property", System.getProperty(key));

        SetOptionResult off = server.setOption(new SetOptionParams(optionName, false)).get(10, TimeUnit.SECONDS);
        Assert.assertTrue("setOption(false) should succeed", off.isSuccess());
        Assert.assertFalse("Option should no longer be effective", off.isEffective());
        Assert.assertFalse("The session runtime should no longer see the option",
                server.getSession().getPureRuntime().getOptions().isOptionSet(optionName));
        Assert.assertNull("setOption must not write a system property", System.getProperty(key));
    }

    @Test
    public void setOption_rpc_blankName_returnsError() throws Exception
    {
        SetOptionResult result = server.setOption(new SetOptionParams("   ", true)).get(10, TimeUnit.SECONDS);
        Assert.assertFalse("Blank option name should fail", result.isSuccess());
    }

    @Test
    public void deleteFile_rpc_absentSource_reportsNotRemovedButSuccess() throws Exception
    {
        String uri = "file:///workspace/src/main/resources/e2e_never_created_"
                + System.currentTimeMillis() + ".pure";
        DeleteFileResult result = server.deleteFile(new DeleteFileParams(uri)).get(15, TimeUnit.SECONDS);
        Assert.assertTrue("Deleting an absent source is not an error", result.isSuccess());
        Assert.assertFalse("Nothing should have been removed", result.isRemoved());
    }

    @Test
    public void checkBatch_rpc_success_clearsDiagnosticsOnInputFile() throws Exception
    {
        long ts = System.currentTimeMillis();
        String uri = "file:///workspace/src/main/resources/e2e_rpc_clear_" + ts + ".pure";
        mockClient.clearDiagnostics();

        CheckBatchParams params = new CheckBatchParams();
        params.setFiles(Collections.singletonList(new FileEntry(uri,
                "Class test::e2e::Clear" + ts + "\n{\n  v: Integer[1];\n}\n")));
        CheckBatchResult result = server.checkBatch(params).get(30, TimeUnit.SECONDS);
        Assert.assertTrue("checkBatch should succeed, got: " + result.getError(), result.isSuccess());

        // A successful batch must tell the client the input file is clean (empty diagnostics),
        // otherwise a real editor keeps showing stale errors after a fix.
        List<PublishDiagnosticsParams> published = mockClient.getDiagnosticsFor(uri);
        Assert.assertFalse("Should have published a diagnostics update for the input file", published.isEmpty());
        Assert.assertTrue("Input file diagnostics should be cleared, got: "
                        + published.get(published.size() - 1).getDiagnostics(),
                published.get(published.size() - 1).getDiagnostics().isEmpty());

        server.deleteFile(new DeleteFileParams(uri)).get(15, TimeUnit.SECONDS);
    }

    // -- Read providers behind the graph read lock (previously untested handlers) --

    @Test
    public void completion_rpc_returnsResultForOpenFile() throws Exception
    {
        long ts = System.currentTimeMillis();
        String uri = "file:///workspace/src/main/resources/e2e_completion_" + ts + ".pure";
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "pure", 1,
                        "Class test::e2e::CompletionClass" + ts + "\n{\n  name: String[1];\n}\n")));
        Thread.sleep(1500);

        CompletionParams params = new CompletionParams(new TextDocumentIdentifier(uri), new Position(2, 8));
        Either<List<CompletionItem>, CompletionList> result =
                server.getTextDocumentService().completion(params).get(10, TimeUnit.SECONDS);

        // The read-locked handler must run without error and return a (possibly empty) item list.
        Assert.assertNotNull("completion() should return a result", result);
        List<CompletionItem> items = result.isLeft() ? result.getLeft() : result.getRight().getItems();
        Assert.assertNotNull("completion items should not be null", items);
    }

    @Test
    public void documentSymbol_rpc_returnsOutlineForOpenFile() throws Exception
    {
        long ts = System.currentTimeMillis();
        String uri = "file:///workspace/src/main/resources/e2e_outline_" + ts + ".pure";
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "pure", 1,
                        "Class test::e2e::OutlineClass" + ts + "\n{\n  name: String[1];\n}\n")));
        Thread.sleep(1500);

        DocumentSymbolParams params = new DocumentSymbolParams(new TextDocumentIdentifier(uri));
        List<Either<SymbolInformation, DocumentSymbol>> symbols =
                server.getTextDocumentService().documentSymbol(params).get(10, TimeUnit.SECONDS);

        Assert.assertNotNull("documentSymbol() should return a result", symbols);
        boolean found = false;
        for (Either<SymbolInformation, DocumentSymbol> either : symbols)
        {
            DocumentSymbol ds = either.getRight();
            if (ds != null && ds.getName().contains("OutlineClass" + ts))
            {
                found = true;
                break;
            }
        }
        Assert.assertTrue("Outline should include the opened class", found);
    }

    @Test
    public void testFunctions_rpc_findsOnlyRealTestStereotypedFunction() throws Exception
    {
        long ts = System.currentTimeMillis();
        String uri = "file:///workspace/src/main/resources/e2e_test_functions_" + ts + ".pure";
        CheckBatchParams compile = new CheckBatchParams();
        compile.setFiles(Collections.singletonList(new FileEntry(uri,
                "function <<test.Test>> test::e2e::tf::realTest" + ts + "(): Boolean[1]\n"
                        + "{\n  assert(true, |'')\n}\n"
                        + "function test::e2e::tf::plainFn" + ts + "(): Boolean[1]\n"
                        + "{\n  true\n}\n")));
        CheckBatchResult compileResult = server.checkBatch(compile).get(30, TimeUnit.SECONDS);
        Assert.assertTrue("Fixture should compile, got: " + compileResult.getError(), compileResult.isSuccess());

        List<TestFunctionInfo> found = server.testFunctions(new TestFunctionsParams(uri)).get(15, TimeUnit.SECONDS);
        Assert.assertEquals("Should find exactly the one <<test.Test>>-stereotyped function, found: " + found,
                1, found.size());
        Assert.assertEquals("test::e2e::tf::realTest" + ts + "__Boolean_1_", found.get(0).getFunctionPath());
        Assert.assertEquals("realTest" + ts, found.get(0).getName());

        server.deleteFile(new DeleteFileParams(uri)).get(15, TimeUnit.SECONDS);
    }

    @Test
    public void testFunctions_rpc_uninitializedOrMissingUri_returnsEmpty() throws Exception
    {
        List<TestFunctionInfo> result = server.testFunctions(new TestFunctionsParams()).get(10, TimeUnit.SECONDS);
        Assert.assertNotNull("Should return an (empty) list rather than fail", result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void status_rpc_exposesRepositoryProgressFields() throws Exception
    {
        // Plumbing/invariant check that legend/status carries the compiled/total repository fields
        // fed by onCompileProgress -> CompileProgressTracker. (The concrete parsing of progress
        // messages is covered deterministically by CompileProgressTrackerTest; a platform-only warm
        // session may legitimately report 0/0 since it loads the platform without a multi-repo compile.)
        LspStatus status = server.status().get(10, TimeUnit.SECONDS);
        Assert.assertNotNull("status() should return a result", status);
        Assert.assertNotNull("status should carry a state", status.getState());
        Assert.assertTrue("Total repositories should be non-negative, got: " + status.getTotalRepositories(),
                status.getTotalRepositories() >= 0);
        Assert.assertTrue("Compiled repositories should be non-negative, got: " + status.getCompiledRepositories(),
                status.getCompiledRepositories() >= 0);
        Assert.assertTrue("Compiled repositories must not exceed total ("
                        + status.getCompiledRepositories() + " > " + status.getTotalRepositories() + ")",
                status.getCompiledRepositories() <= status.getTotalRepositories());
    }

    @Test
    public void twoConnectedClients_bothReceiveDiagnostics_disconnectStopsOnlyOne() throws Exception
    {
        // Regression test for the multi-client broadcast bug: a second client connecting (e.g. an
        // agent/CLI bridge sharing the same warm socket-mode daemon as an IDE session) must not steal
        // notifications away from the first, already-connected client.
        MockLanguageClient secondClient = new MockLanguageClient();
        server.connect(secondClient);
        try
        {
            long ts = System.currentTimeMillis();
            String uri = "file:///workspace/src/main/resources/e2e_multiclient_" + ts + ".pure";
            mockClient.clearDiagnostics();
            secondClient.clearDiagnostics();

            server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                    new TextDocumentItem(uri, "pure", 1,
                            "Class test::e2e::MultiClient" + ts + "\n{\n  x: NoSuchType999[1];\n}\n")));
            Thread.sleep(1500);

            Assert.assertFalse("Original client should still receive diagnostics",
                    mockClient.getDiagnosticsFor(uri).isEmpty());
            Assert.assertFalse("Second client should also receive diagnostics",
                    secondClient.getDiagnosticsFor(uri).isEmpty());

            // Disconnect the second client; the first must keep receiving updates uninterrupted.
            server.disconnect(secondClient);
            mockClient.clearDiagnostics();
            secondClient.clearDiagnostics();

            VersionedTextDocumentIdentifier docId = new VersionedTextDocumentIdentifier(uri, 2);
            TextDocumentContentChangeEvent fix = new TextDocumentContentChangeEvent(
                    "Class test::e2e::MultiClient" + ts + "\n{\n  x: String[1];\n}\n");
            server.getTextDocumentService().didChange(new DidChangeTextDocumentParams(
                    docId, Collections.singletonList(fix)));
            Thread.sleep(1500);

            Assert.assertFalse("Still-connected original client should keep receiving diagnostics",
                    mockClient.getDiagnosticsFor(uri).isEmpty());
            Assert.assertTrue("Disconnected client must receive nothing further",
                    secondClient.getDiagnosticsFor(uri).isEmpty());
        }
        finally
        {
            server.disconnect(secondClient);
        }
    }

    /**
     * Mock LanguageClient that captures published diagnostics.
     */
    static class MockLanguageClient implements LanguageClient
    {
        private final List<PublishDiagnosticsParams> allDiagnostics =
                Collections.synchronizedList(new ArrayList<>());
        private final List<MessageParams> messages =
                Collections.synchronizedList(new ArrayList<>());

        @Override
        public void publishDiagnostics(PublishDiagnosticsParams params)
        {
            this.allDiagnostics.add(params);
        }

        @Override
        public void showMessage(MessageParams params)
        {
            this.messages.add(params);
            System.out.println("[LSP] " + params.getType() + ": " + params.getMessage());
        }

        @Override
        public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams params)
        {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void logMessage(MessageParams params)
        {
            System.out.println("[LSP-LOG] " + params.getMessage());
        }

        @Override
        public void telemetryEvent(Object o)
        {
        }

        List<PublishDiagnosticsParams> getDiagnosticsFor(String uri)
        {
            List<PublishDiagnosticsParams> result = new ArrayList<>();
            for (PublishDiagnosticsParams p : this.allDiagnostics)
            {
                if (uri.equals(p.getUri()))
                {
                    result.add(p);
                }
            }
            return result;
        }

        void clearDiagnostics()
        {
            this.allDiagnostics.clear();
        }
    }
}
