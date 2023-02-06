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

package org.finos.legend.pure.m3.tests.incremental.profile;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeStereotype extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }


    @After
    public void cleanRuntime() {
        runtime.delete("userId.pure");
        runtime.delete("sourceId.pure");
        runtime.delete("classId.pure");
    }

    @Test
    public void testPureRuntimeProfileStereotypeClass() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Profile testProfile{stereotypes:[s1,s2];}")
                        .createInMemorySource("userId.pure", "Class <<testProfile.s1>> A{}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("testProfile has not been defined!", "userId.pure", 1, 21)
                        .createInMemorySource("sourceId.pure", "Profile testProfile{stereotypes:[s1,s2];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }

    @Test
    public void testPureRuntimeProfileStereotypeClassError() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Profile testProfile{stereotypes:[s1,s2];}")
                        .createInMemorySource("userId.pure", "Class <<testProfile.s1>> A{}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("testProfile has not been defined!", "userId.pure", 1, 21)
                        .createInMemorySource("sourceId.pure", "Profile testProfile44{stereotypes:[s1,s2];}")
                        .compileWithExpectedCompileFailure("testProfile has not been defined!", "userId.pure", 1, 21)
                        .updateSource("sourceId.pure", "Profile testProfile{stereotypes:[s1,s2];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeProfileStereotypeClassValueError() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Profile testProfile{stereotypes:[s1,s2];}")
                        .createInMemorySource("userId.pure", "Class <<testProfile.s1>> A{}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("testProfile has not been defined!", "userId.pure", 1, 21)
                        .createInMemorySource("sourceId.pure", "Profile testProfile{stereotypes:[s4,s2];}")
                        .compileWithExpectedCompileFailure("The stereotype 's1' can't be found in profile 'testProfile'", "userId.pure", 1, 21)
                        .updateSource("sourceId.pure", "Profile testProfile{stereotypes:[s1,s2];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }

    @Test
    public void testPureRuntimeProfileStereotypeClassInverse() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class <<testProfile.s1>> A{}")
                        .createInMemorySource("userId.pure", "Profile testProfile{stereotypes:[s1,s2];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compile()
                        .createInMemorySource("sourceId.pure", "Class <<testProfile.s1>> A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }


    @Test
    public void testPureRuntimeProfileUsedInFunction() throws Exception
    {

        this.runtime.createInMemorySource("userId.pure", "Profile my::profile::testProfile{stereotypes:[s1,s2];tags: [name];}");
        this.runtime.createInMemorySource("sourceId.pure", "import meta::pure::functions::meta::*;\n" +
                "function meta::pure::functions::meta::stereotype(profile:Profile[1], str:String[1]):Stereotype[1]" +
                "{" +
                "   $profile.stereotypes->at(0);" +
                "}\n" +
                "function meta::pure::functions::meta::hasStereotype(f:ElementWithStereotypes[1], stereotype:String[1], profile:Profile[1]):Boolean[1]\n" +
                "{\n" +
                "    let st = $profile->stereotype($stereotype);\n" +
                "    !$f.stereotypes->filter(s | $s == $st)->isEmpty();\n" +
                "}\nfunction hasStereo(a:ElementWithStereotypes[1]):Boolean[1]{ if($a->hasStereotype('s1',my::profile::testProfile), | true, | false)}");
        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.modify("userId.pure", "////My Comment\n" +
                    "Profile my::profile::testProfile{stereotypes:[s1,s2];tags: [name];}");
            this.runtime.compile();
            Assert.assertEquals("Graph size mismatch", size, this.repository.serialize().length);
        }
    }


    @Test
    public void testPureRuntimeProfileUsedInNew() throws Exception
    {

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("userId.pure", "Profile my::pack::testProfile{stereotypes:[s1,s2];tags: [name];}")
                        .createInMemorySource("sourceId.pure", "import meta::pure::functions::meta::*;\n" +
                                "import my::pack::*;\n" +
                                "Class my::pack::A{ value:Any[0..1]; }\n" +
                                "function meta::pure::functions::meta::stereotype(profile:Profile[1], str:String[1]):Stereotype[1]" +
                                "{" +
                                "   $profile.stereotypes->at(0);" +
                                "}\n" +
                                "function my::pack::newObj(a:ElementWithStereotypes[1]):A[1]{ ^A(value=if(!$a.stereotypes->filter(x|$x == my::pack::testProfile->stereotype('s1'))->isEmpty(), | true, | false))}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("userId.pure", "////My Comment\n" +
                                "Profile my::pack::testProfile{stereotypes:[s1,s2];tags: [name];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeProfileWithEnumWithReferenceToEnum() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("userId.pure", "Profile my::pack::testProfile{stereotypes:[s1,s2];tags: [name];}")
                        .createInMemorySource("sourceId.pure", "import my::pack::*;\n" + "Enum my::pack::myEnum{ <<testProfile.s1>> VAL1, VAL2}")
                        .createInMemorySource("classId.pure", "Class my::pack::A{ value:my::pack::myEnum[0..1]; }\n")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("userId.pure", "////My Comment\n" +
                                "Profile my::pack::testProfile{stereotypes:[s1,s2];tags: [name];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }



}
