// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m3.pct.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.AnnotatedElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.AnnotationAccessor;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.profile.Profile;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class PCTTools
{
    public static final String PCT_PROFILE = "meta::pure::test::pct::PCT";
    public static final String DOC_PROFILE = "meta::pure::profiles::doc";
    public static final String TEST_PROFILE = "meta::pure::profiles::test";

    public static boolean isPCTTest(CoreInstance node, ProcessorSupport processorSupport)
    {
        return Profile.hasStereotype(node, PCT_PROFILE, "test", processorSupport);
    }

    public static Set<String> getPCTQualifiers(CoreInstance testFunction, ProcessorSupport processorSupport)
    {
        return ((AnnotatedElement) testFunction)._stereotypes().collectIf(
                x -> isTestQualifierProfile(x._profile(), processorSupport),
                AnnotationAccessor::_value,
                Sets.mutable.empty());
    }

    public static boolean isTestQualifierProfile(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile profile, ProcessorSupport processorSupport)
    {
        return Profile.hasStereotype(profile, PCT_PROFILE, "testQualifierProfile", processorSupport);
    }

    public static boolean isPCTFunction(CoreInstance node, ProcessorSupport processorSupport)
    {
        return Profile.hasStereotype(node, PCT_PROFILE, "function", processorSupport);
    }

    public static boolean isPlatformOnly(CoreInstance node, ProcessorSupport processorSupport)
    {
        return Profile.hasStereotype(node, PCT_PROFILE, "platformOnly", processorSupport);
    }

    public static String getDoc(CoreInstance node, ProcessorSupport processorSupport)
    {
        return Profile.getTaggedValue(node, DOC_PROFILE, "doc", processorSupport);
    }

    public static String getGrammarDoc(CoreInstance node, ProcessorSupport processorSupport)
    {
        return Profile.getTaggedValue(node, PCT_PROFILE, "grammarDoc", processorSupport);
    }

    public static String getGrammarCharacters(CoreInstance node, ProcessorSupport processorSupport)
    {
        return Profile.getTaggedValue(node, PCT_PROFILE, "grammarCharacters", processorSupport);
    }

    public static void displayErrorMessage(String message, CoreInstance testFunction, String pctExecutor, ProcessorSupport ps, Throwable e)
    {
        if (PCTTools.isPCTTest(testFunction, ps))
        {
            if (message == null)
            {
                debugHelper(testFunction, pctExecutor, false, e);
            }
            else
            {
                debugHelper(testFunction, pctExecutor, true, e);
                Assert.fail("The PCT test runner expected an error containing: \"" + message + "\" but the the error was: \"" + getMessageFromError(e).replace("\"", "\\\"").replace("\n", "\\n") + "\"\nTrace:\n" + ExceptionUtils.getStackTrace(e));
            }
        }
    }

    private static void debugHelper(CoreInstance testFunction, String pctExecutor, boolean replace, Throwable e)
    {
        System.out.println("\nDebug at:\n   " + PackageableElement.getUserPathForPackageableElement(((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) testFunction)._package(), "::") + "::" + testFunction.getValueForMetaPropertyToOne("functionName").getName() + "(" + pctExecutor + ");");
        if (e == null)
        {
            System.out.println("Or remove the expected failure for: " + PackageableElement.getUserPathForPackageableElement(testFunction, "::"));
        }
        else
        {
            System.out.println("Or " + (replace ? "replace" : "add to") + " expected failure:\n   one(\"" + PackageableElement.getUserPathForPackageableElement(testFunction, "::") + "\", \"" + cleanMessage(e) + "\"),");
        }
    }

    public static String getMessageFromError(Throwable e)
    {
        if (e instanceof NullPointerException)
        {
            return "NullPointer exception";
        }
        String message = e.getMessage();
        if (message == null)
        {
            // Maintaining this for backward compatibility. However, this is deceptive since we know this is not a
            // NullPointerException. Consider returning something more informative, like e.getClass().getName().
            return "NullPointer exception";
        }

        int expectedStarts = message.indexOf("expected: '");
        int expectedEnds = message.indexOf("'\nactual:");
        int actualStarts = message.indexOf("'", expectedEnds);
        int actualEnds = message.lastIndexOf("'");
        if (expectedStarts >= 0 && actualStarts > 0 && expectedEnds > 0 && actualEnds > 0)
        {
            String before = message.substring(0, expectedStarts);
            String expectedValue = message.substring(expectedStarts, expectedEnds);
            String actualValue = message.substring(actualStarts, actualEnds);
            String after = message.substring(actualEnds);
            return before + expectedValue.replace("\\n", "\n") + actualValue.replace("\\n", "\n") + after;
        }

        return message;
    }

    private static String cleanMessage(Throwable e)
    {
        String message = getMessageFromError(e);
        int quotes = message.indexOf('"');
        boolean shouldCut = quotes > -1 && (message.contains("Execution error at ") || message.contains("Assert failure at "));
        message = shouldCut ? message.substring(quotes) : message;
        return message.replace("\"", "\\\"").replace("\n", "\\n");
    }

    public static void displayExpectedErrorFailMessage(String message, CoreInstance testFunction, String pctExecutor)
    {
        debugHelper(testFunction, pctExecutor, false, null);
        Assert.fail("The PCT test runner expected an error containing: \"" + message + "\" but the test succeeded!");
    }

    public static boolean isTest(CoreInstance node, ProcessorSupport processorSupport)
    {
        return Profile.hasStereotype(node, TEST_PROFILE, "Test", processorSupport);
    }

    // Jackson writer pinned to Unix "\n" line endings so buildManifestExclusionSnippet
    // produces byte-identical output regardless of the OS System.lineSeparator().
    private static final ObjectWriter SNIPPET_WRITER = new ObjectMapper().writer(new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("  ", "\n")));

    /**
     * Unwraps the generic wrapper(s) that the runtime attaches around the real underlying error,
     * returning the first cause whose message is the actual (diagnostic) error.
     * This keeps PCT expected-failure matching and reporting consistent: an exclusion's
     * expectedError is compared against the underlying error (e.g. the store's SQL error)
     * rather than the opaque wrapper message. Both the legacy runner and the surveyor runner rely on
     * this so a single manifest string works across execution paths and platforms.
     */
    public static Throwable unwrapExecutionError(Throwable e)
    {
        Throwable current = e;
        while (current != null
                && current.getCause() != null
                && current.getCause() != current
                && isUnwrappableWrapper(current))
        {
            current = current.getCause();
        }
        return current;
    }

    // Whether the given throwable is a pass-through wrapper with no diagnostic value of its own
    private static boolean isUnwrappableWrapper(Throwable t)
    {
        // Reflection plumbing carries no message of its own — it only wraps the real cause.
        return t instanceof InvocationTargetException
                || t instanceof UndeclaredThrowableException
                || t instanceof ExceptionInInitializerError
                || isGenericExecutionErrorMessage(t.getMessage());
    }

    /**
     * Whether the given message is the runtime's generic wrapper (any variant).
     * Such a message carries no diagnostic value and shouldn't be surfaced
     */
    public static boolean isGenericExecutionErrorMessage(String message)
    {
        return message != null && message.contains("Unexpected error executing function");
    }

    // Builds a copy-paste-ready JSON exclusion entry for a PCT manifest.
    public static String buildManifestExclusionSnippet(String fqn, String expectedError)
    {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("test", fqn == null ? "" : fqn);
        entry.put("expectedError", expectedError == null ? "" : expectedError);
        try
        {
            return SNIPPET_WRITER.writeValueAsString(entry);
        }
        catch (JsonProcessingException e)
        {
            // Serialising a Map<String, String> with a stock ObjectMapper cannot fail; rethrow defensively.
            throw new RuntimeException("Failed to build PCT manifest exclusion snippet", e);
        }
    }

    /**
     * Formats the failure message shown when a PCT test errors but does not match its manifest
     * exclusion (or has no exclusion). Shows the expected vs actual error and a ready-to-paste
     * manifest snippet using the actual error.
     */
    public static String formatExpectedFailureMismatch(String fqn, String expectedError, String actualError)
    {
        String expectedLine = expectedError == null ? "(none - no exclusion for this test)" : expectedError;
        return "PCT expected-failure mismatch for " + fqn + "\n" +
                "  expected : " + expectedLine + "\n" +
                "  actual   : " + actualError + "\n" +
                "To record this as an expected failure, set the exclusion in the manifest to:\n" +
                buildManifestExclusionSnippet(fqn, actualError);
    }

    /**
     * Formats the failure message shown when a PCT test has a manifest exclusion but now passes,
     * telling the developer to remove the stale exclusion.
     */
    public static String formatUnexpectedPass(String fqn, String expectedError)
    {
        return "PCT test " + fqn + " was expected to fail but now PASSES.\n" +
                "Remove its exclusion from the manifest:\n" +
                buildManifestExclusionSnippet(fqn, expectedError);
    }
}
