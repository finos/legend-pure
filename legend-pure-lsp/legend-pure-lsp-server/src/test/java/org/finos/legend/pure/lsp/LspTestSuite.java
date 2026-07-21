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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

@RunWith(LspTestSuite.JavaVersionRunner.class)
public class LspTestSuite
{
    // LSP4J 0.23.1 is compiled for Java 11.
    private static final int MINIMUM_JAVA_VERSION = 11;

    public static class JavaVersionRunner extends Runner
    {
        private final Runner delegate;
        private final Description description;
        private final Description skippedTest;

        public JavaVersionRunner(Class<?> suiteClass) throws Exception
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
                this.delegate = Request.classes(discoverTestClasses(suiteClass)).getRunner();
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

        private static Class<?>[] discoverTestClasses(Class<?> suiteClass) throws Exception
        {
            File testClassesDirectory = new File(suiteClass.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!testClassesDirectory.isDirectory())
            {
                throw new IllegalStateException("Expected test classes directory: " + testClassesDirectory);
            }

            List<String> testClassNames = new ArrayList<>();
            collectTestClassNames(testClassesDirectory, testClassesDirectory, suiteClass.getName(), testClassNames);
            Collections.sort(testClassNames);
            if (testClassNames.isEmpty())
            {
                throw new IllegalStateException("No LSP test classes found in: " + testClassesDirectory);
            }

            ClassLoader classLoader = suiteClass.getClassLoader();
            Class<?>[] testClasses = new Class<?>[testClassNames.size()];
            for (int i = 0; i < testClassNames.size(); i++)
            {
                testClasses[i] = Class.forName(testClassNames.get(i), false, classLoader);
            }
            return testClasses;
        }

        private static void collectTestClassNames(File rootDirectory, File directory, String suiteClassName, List<String> testClassNames)
        {
            File[] files = directory.listFiles();
            if (files == null)
            {
                throw new IllegalStateException("Cannot list test classes directory: " + directory);
            }
            Arrays.sort(files);
            for (File file : files)
            {
                if (file.isDirectory())
                {
                    collectTestClassNames(rootDirectory, file, suiteClassName, testClassNames);
                }
                else
                {
                    String relativePath = rootDirectory.toURI().relativize(file.toURI()).getPath();
                    if (relativePath.endsWith("Test.class") && (relativePath.indexOf('$') == -1))
                    {
                        String className = relativePath.substring(0, relativePath.length() - ".class".length()).replace('/', '.');
                        if (!suiteClassName.equals(className))
                        {
                            testClassNames.add(className);
                        }
                    }
                }
            }
        }
    }
}
