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

package org.finos.legend.pure.lsp.mcp;

import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

@RunWith(McpTestSuite.JavaVersionRunner.class)
public class McpTestSuite
{
    // LSP4J 0.23.1 is compiled for Java 11, so on older JVMs the tools that
    // touch LSP4J types fail with UnsupportedClassVersionError. Skip the whole
    // suite there, the same way LspTestSuite does in legend-pure-lsp-server.
    private static final int MINIMUM_JAVA_VERSION = 11;

    public static class JavaVersionRunner extends Runner
    {
        private final Runner delegate;
        private final Description description;
        private final Description skippedTest;

        public JavaVersionRunner(Class<?> suiteClass)
        {
            if (getJavaMajorVersion() < MINIMUM_JAVA_VERSION)
            {
                this.delegate = null;
                this.skippedTest = Description.createTestDescription(suiteClass, "requiresJava11OrLater");
                this.description = Description.createSuiteDescription(suiteClass);
                this.description.addChild(this.skippedTest);
            }
            else
            {
                this.delegate = Request.classes(
                        PureToolRegistryTest.class,
                        McpStdioServerTest.class,
                        WorkspaceSyncTest.class,
                        PureToolsIntegrationTest.class,
                        LegendPureMcpServerTest.class,
                        McpEndToEndTest.class).getRunner();
                this.description = this.delegate.getDescription();
                this.skippedTest = null;
            }
        }

        @Override
        public Description getDescription()
        {
            return this.description;
        }

        @Override
        public void run(RunNotifier notifier)
        {
            if (this.delegate == null)
            {
                notifier.fireTestIgnored(this.skippedTest);
            }
            else
            {
                this.delegate.run(notifier);
            }
        }

        private static int getJavaMajorVersion()
        {
            String version = System.getProperty("java.specification.version");
            int start = version.startsWith("1.") ? 2 : 0;
            int end = start;
            while ((end < version.length()) && Character.isDigit(version.charAt(end)))
            {
                end++;
            }
            if (end == start)
            {
                throw new IllegalStateException("Cannot parse Java specification version: " + version);
            }
            return Integer.parseInt(version.substring(start, end));
        }
    }
}
