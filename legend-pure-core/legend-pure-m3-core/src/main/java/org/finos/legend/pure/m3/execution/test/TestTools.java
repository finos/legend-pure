// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.m3.execution.test;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class TestTools
{
    public static final String TEST_PROFILE = "meta::pure::profiles::test";

    public static final String TEST_STEREOTYPE = "Test";
    public static final String TEST_COLLECTION_STEREOTYPE = "TestCollection";
    public static final String EXCLUDEALLOY_STEREOTYPE = "ExcludeAlloy";
    public static final String EXCLUDE_ALLOY_TEXT_MODE_STEREOTYPE = "ExcludeAlloyTextMode";
    public static final String BEFORE_PACKAGE_STEREOTYPE = "BeforePackage";
    public static final String AFTER_PACKAGE_STEREOTYPE = "AfterPackage";
    public static final String TO_FIX_STEREOTYPE = "ToFix";
    public  static  final String ALLOY_ONLY_STEROTYPE = "AlloyOnly";

    public static final String PLATFORM_EXCLUSION_TAG = "excludePlatform";

    public static final String TEST_COLLECTION_PKG = "pkg";
    public static final String TEST_COLLECTION_TEST_PARAMETERIZATION_ID = "testParameterizationId";
    public static final String TEST_COLLECTION_TEST_FUNCTION_PARAM = "testFunctionParam";
    public static final String TEST_COLLECTION_TEST_FUNCTION_PARAM_CUSTOMIZER = "testFunctionParamCustomizer";
    public static final String TEST_COLLECTION_TEST_FUNCTIONS = "testFunctions";
    public static final String TEST_COLLECTION_BEFORE_FUNCTIONS = "beforeFunctions";
    public static final String TEST_COLLECTION_AFTER_FUNCTIONS = "afterFunctions";
    public static final String TEST_COLLECTION_SUB_COLLECTIONS = "subCollections";

    public static boolean hasToFixStereotype(CoreInstance node, ProcessorSupport processorSupport)
    {
        return hasTestStereotypeWithValue(node, TO_FIX_STEREOTYPE, processorSupport);
    }

    public static boolean hasAlloyOnlyStereotype(CoreInstance node, ProcessorSupport processorSupport)
    {
        return hasTestStereotypeWithValue(node, ALLOY_ONLY_STEROTYPE, processorSupport);
    }

    public static boolean hasTestStereotype(CoreInstance node, ProcessorSupport processorSupport)
    {
        return hasTestStereotypeWithValue(node, TEST_STEREOTYPE, processorSupport);
    }

    public static boolean hasTestCollectionStereotype(CoreInstance node, ProcessorSupport processorSupport)
    {
        return hasTestStereotypeWithValue(node, TEST_COLLECTION_STEREOTYPE, processorSupport);
    }

    public static boolean hasExcludeAlloyStereotype(CoreInstance node, ProcessorSupport processorSupport)
    {
        return hasTestStereotypeWithValue(node, EXCLUDEALLOY_STEREOTYPE, processorSupport);
    }

    public static boolean hasExcludeAlloyTextModeStereotype(CoreInstance node, ProcessorSupport processorSupport)
    {
        return hasTestStereotypeWithValue(node, EXCLUDE_ALLOY_TEXT_MODE_STEREOTYPE, processorSupport);
    }

    public static boolean hasBeforePackageStereotype(CoreInstance node, ProcessorSupport processorSupport)
    {
        return hasTestStereotypeWithValue(node, BEFORE_PACKAGE_STEREOTYPE, processorSupport);
    }

    public static boolean hasAfterPackageStereotype(CoreInstance node, ProcessorSupport processorSupport)
    {
        return hasTestStereotypeWithValue(node, AFTER_PACKAGE_STEREOTYPE, processorSupport);
    }

    public static boolean hasAnyTestStereotype(CoreInstance node, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> stereotypes = Instance.getValueForMetaPropertyToManyResolved(node, M3Properties.stereotypes, processorSupport);
        if (stereotypes.notEmpty())
        {
            CoreInstance testProfile = processorSupport.package_getByUserPath(TEST_PROFILE);
            for (CoreInstance stereotype : stereotypes)
            {
                if (testProfile == Instance.getValueForMetaPropertyToOneResolved(stereotype, M3Properties.profile, processorSupport))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasPlatformExclusionTaggedValue(CoreInstance node, String excludedPlatformName, ProcessorSupport processorSupport)
    {
        return hasTestTaggedValue(node, PLATFORM_EXCLUSION_TAG, excludedPlatformName, processorSupport);
    }

    private static boolean hasTestStereotypeWithValue(CoreInstance node, String value, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> stereotypes = Instance.getValueForMetaPropertyToManyResolved(node, M3Properties.stereotypes, processorSupport);
        if (stereotypes.notEmpty())
        {
            CoreInstance testProfile = processorSupport.package_getByUserPath(TEST_PROFILE);
            for (CoreInstance stereotype : stereotypes)
            {
                if ((testProfile == Instance.getValueForMetaPropertyToOneResolved(stereotype, M3Properties.profile, processorSupport)) &&
                        value.equals(Instance.getValueForMetaPropertyToOneResolved(stereotype, M3Properties.value, processorSupport).getName()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasTestTaggedValue(CoreInstance node, String tag, String value, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> taggedValues = Instance.getValueForMetaPropertyToManyResolved(node, M3Properties.taggedValues, processorSupport);
        if (taggedValues.notEmpty())
        {
            CoreInstance testProfile = processorSupport.package_getByUserPath(TEST_PROFILE);
            for (CoreInstance taggedValue : taggedValues)
            {
                CoreInstance taggedValueTag = Instance.getValueForMetaPropertyToOneResolved(taggedValue, M3Properties.tag, processorSupport);
                if ((testProfile == Instance.getValueForMetaPropertyToOneResolved(taggedValueTag, M3Properties.profile, processorSupport)) &&
                        tag.equals(Instance.getValueForMetaPropertyToOneResolved(taggedValueTag, M3Properties.value, processorSupport).getName()) &&
                        value.equals(Instance.getValueForMetaPropertyToOneResolved(taggedValue, M3Properties.value, processorSupport).getName()))
                {
                    return true;
                }
            }
        }
        return false;
    }
}
