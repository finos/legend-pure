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

package org.finos.legend.pure.m3.tests.validation.milestoning.functionExpression;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestGetAllVersionsValidator extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime() {
        runtime.delete("sourceId.pure");
    }

    @Rule
    public final ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testGetAllVersionsIsNotPermittedForNonTemporalTypes()
    {
        expectedEx.expect(PureCompilationException.class);
        expectedEx.expectMessage("The function 'getAllVersions' may only be used with temporal types: processingtemporal & businesstemporal, the type Product is  not temporal");
        validateGetAllVersionsIsNotPermittedForNonTemporalTypes(false);
    }

    @Test
    public void testGetAllVersionsIsPermittedForTemporalTypes()
    {
        validateGetAllVersionsIsNotPermittedForNonTemporalTypes(true);
    }

    public void validateGetAllVersionsIsNotPermittedForNonTemporalTypes(boolean isTemporal){

        String temporalStereotype = isTemporal ? "<<temporal.processingtemporal>>" : "";
        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;" +
                        "Class "+temporalStereotype+" meta::test::milestoning::domain::Product{\n" +
                        "   id : Integer[1];\n" +
                        "}" +
                        "function go():Any[*]\n" +
                        "{" +
                        "   let f={|Product.allVersions()};" +
                        "}" +
                        "");

        this.runtime.compile();
    }
}
