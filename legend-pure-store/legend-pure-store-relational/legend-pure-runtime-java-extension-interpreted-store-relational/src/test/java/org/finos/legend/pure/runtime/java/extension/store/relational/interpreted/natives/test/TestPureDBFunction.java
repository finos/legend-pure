// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.store.relational.interpreted.natives.test;

import junit.framework.TestSuite;
import org.finos.legend.pure.runtime.java.interpreted.testHelper.PureTestBuilderInterpreted;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

/**
 * Runs every Pure {@code <<test.Test>>} function under
 * {@code meta::relational::tests::*} through the Pure-native surveyor.
 *
 * <p>The tests themselves ship inside the {@code platform_store_relational}
 * repo under {@code legend-pure-m2-store-relational-pure/src/main/resources/
 * platform_store_relational/tests/}. Both this (interpreted) shim and the
 * sibling compiled shim hand off to the surveyor, so a single Pure-source
 * change covers both runtimes — there's no Java-side test logic to drift
 * between stacks.
 */
@RunWith(AllTests.class)
public class TestPureDBFunction
{
    public static TestSuite suite()
    {
        return PureTestBuilderInterpreted.buildSurveyorSuite("meta::relational::tests", "");
    }
}
