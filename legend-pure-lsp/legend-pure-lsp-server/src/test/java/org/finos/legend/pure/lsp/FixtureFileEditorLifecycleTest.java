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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.junit.Assert;
import org.junit.Test;

/**
 * Regression test for the LSP daemon crash reproduced by opening and closing a non-module .pure fixture
 * file in an editor - see UriMapper#deriveSourceId's local-file exclusion. Before that fix, opening such
 * a file resolved it to a bare-filename scratch id and attempted (and failed) to compile it; closing it
 * then called SourceMutationService#restoreFromDisk with that same scratch id, which threw
 * ("'&lt;file&gt;' should not be in memory!") and triggered full session recovery every time.
 */
public class FixtureFileEditorLifecycleTest
{
    @Test
    public void openingAndClosingANonModuleFixtureFile_doesNotCrashTheSession() throws Exception
    {
        Path workspaceRoot = Files.createTempDirectory("pure-lsp-fixture-lifecycle-test");
        Path resourcesDir = workspaceRoot.resolve("real-module/src/main/resources");
        Path repoDir = resourcesDir.resolve("real_repo");
        Files.createDirectories(repoDir);
        Files.write(resourcesDir.resolve("real_repo.definition.json"),
                ("{\"name\":\"real_repo\","
                        + "\"pattern\":\"(test::reallife)(::.*)?\","
                        + "\"dependencies\":[\"platform\"]}").getBytes());

        // A fixture file in a *different* module with no definition.json of its own - the exact shape
        // of the alloy-lakehouse integration-test-compatibility crash.
        Path fixtureResourcesDir = workspaceRoot.resolve("fixture-module/src/main/resources");
        Path fixtureFile = fixtureResourcesDir.resolve("data/ingest/matview/catalog/matview.pure");
        Files.createDirectories(fixtureFile.getParent());
        String fixtureContent = "###Lakehouse\n<TEST_GROUP>";
        Files.write(fixtureFile, fixtureContent.getBytes());

        LegendPureLspServer server = new LegendPureLspServer();
        server.preconfigureAndWarm(Collections.singletonList(workspaceRoot), Collections.emptySet());
        Assert.assertTrue(server.getSession().isInitialized());

        String uri = fixtureFile.toUri().toString();
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "pure", 1, fixtureContent)));
        server.getTextDocumentService().didClose(new DidCloseTextDocumentParams(new TextDocumentIdentifier(uri)));

        Assert.assertTrue("Session must stay healthy, not enter recovery", server.getSession().isInitialized());
        Assert.assertNull("Nothing should ever be registered for the fixture's bare filename",
                server.getSession().getPureRuntime().getSourceById("matview.pure"));
    }
}
