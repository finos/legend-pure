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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.profile.Profile;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import static org.junit.Assert.fail;

public class PCTTools
{
    public static final String PCT_PROFILE = "meta::pure::test::pct::PCT";
    public static final String DOC_PROFILE = "meta::pure::profiles::doc";

    public static boolean isPCTTest(CoreInstance node, ProcessorSupport processorSupport)
    {
        return Profile.hasStereotype(node, PCT_PROFILE, "test", processorSupport);
    }

    public static boolean isPCTFunction(CoreInstance node, ProcessorSupport processorSupport)
    {
        return Profile.hasStereotype(node, PCT_PROFILE, "function", processorSupport);
    }

    public static String getDoc(CoreInstance node, ProcessorSupport processorSupport)
    {
        return Profile.getTaggedValue(node, DOC_PROFILE, "doc", processorSupport);
    }

    public static String getGrammarDoc(CoreInstance node, ProcessorSupport processorSupport)
    {
        return Profile.getTaggedValue(node, PCT_PROFILE, "grammarDoc", processorSupport);
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
                fail("The PCT test runner expected an error containing: \"" + message + "\" but the the error was: \"" + e.getMessage().replace("\"", "\\\"").replace("\n", "\\n") + "\"\nTrace:\n" + ExceptionUtils.getStackTrace(e))
                ;
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
            System.out.println("Or " + (replace ? "replace" : "add to") + " expected failure:\n   one(\"" + PackageableElement.getUserPathForPackageableElement(testFunction, "::") + "\", \"" + e.getMessage().replace("\"", "\\\"").replace("\n", "\\n") + "\")");
        }
    }

    public static void displayExpectedErrorFailMessage(String message, CoreInstance testFunction, String pctExecutor)
    {
        debugHelper(testFunction, pctExecutor, false, null);
        fail("The PCT test runner expected an error containing: \"" + message + "\" but the test succeeded!");
    }
}
