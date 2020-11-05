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

package org.finos.legend.pure.m3.tests.incremental;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.RuntimeVerifier;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.TestCodeRepositoryWithDependencies;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.junit.Ignore;
import org.junit.Test;

public abstract class AbstractTestIncrementalCompilation extends AbstractPureTestWithCoreCompiled
{
    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        CodeRepository platform = CodeRepository.newPlatformCodeRepository();
        CodeRepository core = new TestCodeRepositoryWithDependencies("core", null, Sets.mutable.with(platform));
        CodeRepository system = new TestCodeRepositoryWithDependencies("system", null, Sets.mutable.with(platform, core));
        CodeRepository model = new TestCodeRepositoryWithDependencies("model", null, Sets.mutable.with(platform, core, system));
        CodeRepository other = new TestCodeRepositoryWithDependencies("datamart_other", null, Sets.mutable.with(platform, core, system, model));
        return Lists.immutable.with(platform, system, model, other);
    }

    protected static MutableCodeStorage getCodeStorage()
    {
        return new PureCodeStorage(null, new ClassLoaderCodeStorage(getCodeRepositories()));
    }

    @Test
    public void test1()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", "function myFunc1():Any[*]\n" +
                        "{\n" +
                        "   let a = 1;\n" +
                        "}\n" +
                        "\n" +
                        "Class myClass\n" +
                        "{\n" +
                        "   property1 : Integer[1];\n" +
                        "}")
                        .createInMemorySource("sourceId2.pure", "function myFunc2():Any[*]\n" +
                                "{\n" +
                                "   myFunc1();\n" +
                                "   let obj = ^myClass( property1 = 0 );\n" +
                                "}")
                        .createInMemorySource("sourceId3.pure", "function start():Any[*]\n" +
                                "{\n" +
                                "   myFunc2();\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId1.pure", "function myFunc1():Any[*]\n" +
                        "{\n" +
                        "   let a  1;\n" +
                        "}\n" +
                        "\n" +
                        "Class myClass\n" +
                        "{\n" +
                        "   property1 : Integer[1];\n" +
                        "}")
                        .compileWithExpectedParserFailureAndAssertions("expected: one of {'->', '}', '(', '.', ';', '&&', '||', '==', '!=', '+', '*', '-', '/', '<', '<=', '>', '>='} found: 'a'", "sourceId1.pure", 3, 8, FastList.newListWith("myFunc2__Any_MANY_", "start__Any_MANY_"), FastList.<String>newList(), FastList.newListWith("myClass", "myFunc1__Any_MANY_"))
                        .updateSource("sourceId1.pure", "function myFunc1():Any[*]\n" +
                                "{\n" +
                                "   let a = 1;\n" +
                                "}\n" +
                                "\n" +
                                "Class myClass\n" +
                                "{\n" +
                                "   property2 : Integer[1];\n" +
                                "}")
                        .compileWithExpectedCompileFailureAndAssertions("The property 'property1' can't be found in the type 'myClass' or in its hierarchy.", "sourceId2.pure", 4, 24, FastList.newListWith("myFunc2__Any_MANY_", "start__Any_MANY_"), FastList.<String>newList(), FastList.newListWith("myClass"))
                        .updateSource("sourceId1.pure", "function myFunc1():Any[*]\n" +
                                "{\n" +
                                "   let a = 1;\n" +
                                "}\n" +
                                "\n" +
                                "Class myClass\n" +
                                "{\n" +
                                "   property1 : Integer[1];\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test2()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", "Class myClass\n" +
                        "{\n" +
                        "   property1 : Integer[1];\n" +
                        "}\n")
                        .createInMemorySource("sourceId2.pure", "function myFunc():Any[*]\n" +
                                "{\n" +
                                "   let obj = ^myClass( property1 = 10 );\n" +
                                "   $obj.property1;\n" +
                                "}")
                        .createInMemorySource("sourceId3.pure", "function start():Any[*]\n" +
                                "{\n" +
                                "    assert(myFunc()==10, |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId1.pure", "Class myClass\n" +
                        "{\n" +
                        "   property1 : Integer[1];\n" +
                        "   property2 : Undefined[1];\n" +
                        "}")
                        .compileWithExpectedCompileFailureAndAssertions("Undefined has not been defined!", "sourceId1.pure", 4, 16, FastList.newListWith("start__Any_MANY_", "myFunc__Any_MANY_"), FastList.<String>newList(), FastList.newListWith("myClass"))
                        .updateSource("sourceId1.pure", "Class myClass\n" +
                                "{\n" +
                                "   property1 : Integer[1];\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test3()
    {
        FastList<String> processed = FastList.newList();
        FastList<String> notProcessed = FastList.newList();

        processed.add("start__Any_MANY_");
        notProcessed.add("myFunc__Any_MANY_");

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", "Enum my::Gender\n" +
                        "{\n" +
                        "   MALE, FEMALE\n" +
                        "}\n" +
                        "\n" +
                        "Class my::Person\n" +
                        "{\n" +
                        "   firstName   :   String[0..1];\n" +
                        "   lastName    :   String[0..1];\n" +
                        "   gender      :   my::Gender[0..1];\n" +
                        "}")
                        .createInMemorySource("sourceId2.pure", "function myFunc():Any[*]\n" +
                                "{\n" +
                                "   let set = \n" +
                                "   [\n" +
                                "      ^my::Person(firstName = 'Marie', lastName='Random', gender = my::Gender.FEMALE),\n" +
                                "      ^my::Person(firstName = 'John', lastName='Doe', gender = my::Gender.MALE)\n" +
                                "   ];\n" +
                                "   $set->filter(p|$p.gender == my::Gender.FEMALE).lastName;\n" +
                                "}")
                        .createInMemorySource("sourceId3.pure", "function start():Any[*]\n" +
                                "{\n" +
                                "   assert( myFunc() == 'Random', |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId1.pure", "Enum my::Gender\n" +
                        "{\n" +
                        "   FEMALE\n" +
                        "}\n" +
                        "\n" +
                        "Class my::Person\n" +
                        "{\n" +
                        "   firstName   :   String[0..1];\n" +
                        "   lastName    :   String[0];\n" +
                        "   gender      :   my::Gender[0..1];\n" +
                        "}\n")
                        .compileWithExpectedCompileFailureAndAssertions("Multiplicity Error: [1] is not compatible with [0]", "sourceId2.pure", 5, 48, FastList.newListWith("start__Any_MANY_", "myFunc__Any_MANY_"), FastList.<String>newList(), FastList.newListWith("my::Person, my::Gender"))
                        .updateSource("sourceId1.pure", "Enum my::Gender\n" +
                                "{\n" +
                                "   MALE, FEMALE\n" +
                                "}\n" +
                                "\n" +
                                "Class my::Person\n" +
                                "{\n" +
                                "   firstName   :   String[0..1];\n" +
                                "   lastName    :   String[0..1];\n" +
                                "   gender      :   my::Gender[0..1];\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test4()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", "import my::pkg2::*;\n" +
                        "\n" +
                        "Class my::pkg1::Firm\n" +
                        "{\n" +
                        "   legalName : String[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "Class my::pkg2::Person\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association my::pkg3::Firm_Person\n" +
                        "{\n" +
                        "   firm : my::pkg1::Firm[0..1];\n" +
                        "   employee : Person[*];\n" +
                        "}")
                        .createInMemorySource("sourceId2.pure", "import my::pkg1::*;\n" +
                                "import my::pkg2::*;\n" +
                                "\n" +
                                "function my::pkg4::myFunc():Any[*]\n" +
                                "{\n" +
                                "   let f = ^Firm( legalName='FirmX', employee = [^Person(name='David'),^Person(name='Pierre')]);\n" +
                                "   $f.legalName;\n" +
                                "}\n")
                        .createInMemorySource("sourceId3.pure", "function start():Any[*]\n" +
                                "{\n" +
                                "    assert(my::pkg4::myFunc() == 'FirmX', |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId1.pure", "import my::pkg2::*;\n" +
                        "import my::pkg5::*;\n" +
                        "Class my::pkg1::Firm\n" +
                        "{\n" +
                        "   legalName : String[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "Class my::pkg2::Person\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association my::pkg3::Firm_Person\n" +
                        "{\n" +
                        "   firm : my::pkg1::Firm[0..1];\n" +
                        "   employee : Person[*];\n" +
                        "}")
                        .compile()
                        .updateSource("sourceId1.pure", "import my::pkg2::*;\n" +
                                "\n" +
                                "Class my::pkg1::Firm\n" +
                                "{\n" +
                                "   legalName : String[0..1];\n" +
                                "}\n" +
                                "\n" +
                                "Class my::pkg2::Person\n" +
                                "{\n" +
                                "   name : String[1];\n" +
                                "}\n" +
                                "\n" +
                                "Association my::pkg3::Firm_Person\n" +
                                "{\n" +
                                "   firm : my::pkg1::Firm[0..1];\n" +
                                "   employee : Person[*];\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test5()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", "Class my::pkg1::X\n" +
                        "{\n" +
                        "   propertyX : Integer[1];\n" +
                        "}")
                        .createInMemorySource("sourceId2.pure", "Class my::pkg2::A extends my::pkg1::X\n" +
                                "{\n" +
                                "   propertyA : String[1];\n" +
                                "}")
                        .createInMemorySource("sourceId3.pure", "function start():Any[*]\n" +
                                "{\n" +
                                "   let a = ^my::pkg2::A( propertyA = '', propertyX = 0 );\n" +
                                "   assert($a.propertyX == 0, |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId1.pure", "Class my::pkg1::X\n" +
                        "{\n" +
                        "   propertyY : Integer[1];\n" +
                        "}")
                        .compileWithExpectedCompileFailureAndAssertions("Can't find the property 'propertyX' in the class my::pkg2::A", "sourceId3.pure", 4, 14, FastList.newListWith("my::pkg2::A"), FastList.<String>newList(), FastList.newListWith("my::pkg1::X"))
                        .updateSource("sourceId1.pure", "Class my::pkg1::X\n" +
                                "{\n" +
                                "   propertyX : Integer[1];\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test6()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/model/sourceId1.pure", "function myFunc1():Integer[1]\n" +
                        "{\n" +
                        "   1;\n" +
                        "}")
                        .createInMemorySource("/datamart_other/sourceId2.pure", "function myFunc2():Any[*]\n" +
                                "{\n" +
                                "   assert(myFunc1() == 1, |'');\n" +
                                "}")
                        .createInMemorySource("sourceId3.pure", "function start():Any[*]\n" +
                                "{\n" +
                                "   myFunc2();\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("/model/sourceId1.pure", "function myFunc():Integer[1]\n" +
                        "{\n" +
                        "   1;\n" +
                        "}")
                        .compileWithExpectedCompileFailureAndAssertions("The system can't find a match for the function: myFunc1()", "/datamart_other/sourceId2.pure", 3, 11, FastList.newListWith("myFunc2__Any_MANY_", "start__Any_MANY_"), FastList.<String>newList(), FastList.newListWith("myFunc__Any_MANY"))
                        .updateSource("/model/sourceId1.pure", "function myFunc1():Integer[1]\n" +
                                "{\n" +
                                "   1;\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test7()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", "import my::pkg2::*;\n" +
                        "\n" +
                        "Class my::pkg1::Firm\n" +
                        "{\n" +
                        "   legalName : String[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "Class my::pkg2::Person\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association my::pkg3::Firm_Person\n" +
                        "{\n" +
                        "   firm : my::pkg1::Firm[0..1];\n" +
                        "   employee : Person[*];\n" +
                        "}")
                        .createInMemorySource("sourceId2.pure", "import my::pkg1::*;\n" +
                                "import my::pkg2::*;\n" +
                                "\n" +
                                "function my::pkg4::myFunc():Any[*]\n" +
                                "{\n" +
                                "   let f = ^Firm( legalName='FirmX', employee = [^Person(name='David'),^Person(name='Pierre')]);\n" +
                                "   $f.legalName;\n" +
                                "}\n")
                        .createInMemorySource("sourceId3.pure", "function start():Any[*]\n" +
                                "{\n" +
                                "    assert(my::pkg4::myFunc() == 'FirmX', |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId1.pure", "Class my::pkg1::Firm\n" +
                        "{\n" +
                        "   legalName : String[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "Class my::pkg2::Person\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association my::pkg3::Firm_Person\n" +
                        "{\n" +
                        "   firm : my::pkg1::Firm[0..1];\n" +
                        "   employee : Person[*];\n" +
                        "}")
                        .compileWithExpectedCompileFailureAndAssertions("Person has not been defined! The system found 1 possible matches:\n" +
                                "    my::pkg2::Person", "sourceId1.pure", 14, 15, FastList.newListWith("my::pkg4::myFunc__Any_MANY_", "start__Any_MANY_"), FastList.<String>newList(), FastList.newListWith("my::pkg1::Firm", "my::pkg2::Person", "my::pkg3::Firm_Person"))
                        .updateSource("sourceId1.pure", "import my::pkg2::*;\n" +
                                "\n" +
                                "Class my::pkg1::Firm\n" +
                                "{\n" +
                                "   legalName : String[0..1];\n" +
                                "}\n" +
                                "\n" +
                                "Class my::pkg2::Person\n" +
                                "{\n" +
                                "   name : String[1];\n" +
                                "}\n" +
                                "\n" +
                                "Association my::pkg3::Firm_Person\n" +
                                "{\n" +
                                "   firm : my::pkg1::Firm[0..1];\n" +
                                "   employee : Person[*];\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test8()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", "function myFunc1():Any[*]\n" +
                        "{\n" +
                        "   myFunc2();\n" +
                        "}")
                        .createInMemorySource("sourceId2.pure", "function myFunc2():Any[*]\n" +
                                "{\n" +
                                "   print('inside myFunc2', 1);\n" +
                                "   print('Parse error test', 1);\n" +
                                "}")
                        .createInMemorySource("sourceId3.pure", "function start():Any[*]\n" +
                                "{\n" +
                                "   myFunc1();\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId2.pure", "function myFunc2():Any[*]\n" +
                        "{\n" +
                        "   print('inside myFunc2', 1)\n" +
                        "   print('Parse error test', 1);\n" +
                        "}")
                        .compileWithExpectedParserFailureAndAssertions("expected: one of {'->', '}', '.', ';', '&&', '||', '==', '!=', '+', '*', '-', '/', '<', '<=', '>', '>='} found: 'print'", "sourceId2.pure", 4, 4, FastList.newListWith("myFunc1__Any_MANY_", "start__Any_MANY_"), FastList.<String>newList(), FastList.newListWith("myFunc2__Any_MANY_"))
                        .updateSource("sourceId2.pure", "function myFunc2():Any[*]\n" +
                                "{\n" +
                                "   print('inside myFunc2', 1);\n" +
                                "   print('Parse error test', 1);\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test9()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", "Class myClass\n" +
                        "{\n" +
                        "   property1 : Integer[1];\n" +
                        "}")
                        .createInMemorySource("sourceId2.pure", "function myFunc():Integer[*]\n" +
                                "{\n" +
                                "   let obj = ^myClass(property1 = 0);\n" +
                                "   $obj.property1;\n" +
                                "}")
                        .createInMemorySource("sourceId3.pure", "function start():Any[*]\n" +
                                "{\n" +
                                "   assert(myFunc()==0, |'');\n" +
                                "}\n")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId2.pure", "function myFunc():Integer[*]\n" +
                        "{\n" +
                        "   let obj = ^myClass(property2 = 0);\n" +
                        "   $obj.property2;\n" +
                        "}")
                        .compileWithExpectedCompileFailureAndAssertions("Can't find the property 'property2' in the class myClass", "sourceId2.pure", 4, 9, FastList.newListWith("start__Any_MANY_", "myClass"), FastList.<String>newList(), FastList.newListWith("myFunc__Integer_MANY_"))
                        .updateSource("sourceId2.pure", "function myFunc():Integer[*]\n" +
                                "{\n" +
                                "   let obj = ^myClass(property1 = 0);\n" +
                                "   $obj.property1;\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test10()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", "Enum my::Gender\n" +
                        "{\n" +
                        "   MALE, FEMALE\n" +
                        "}\n" +
                        "\n" +
                        "Class my::Person\n" +
                        "{\n" +
                        "   firstName   :   String[0..1];\n" +
                        "   lastName    :   String[0..1];\n" +
                        "   gender      :   my::Gender[0..1];\n" +
                        "}")
                        .createInMemorySource("sourceId2.pure", "function myFunc():Any[*]\n" +
                                "{\n" +
                                "   let set = \n" +
                                "   [\n" +
                                "      ^my::Person(firstName = 'Marie', lastName='Random', gender = my::Gender.FEMALE),\n" +
                                "      ^my::Person(firstName = 'John', lastName='Doe', gender = my::Gender.MALE)\n" +
                                "   ];\n" +
                                "   $set->filter(p|$p.gender == my::Gender.FEMALE).lastName;\n" +
                                "}")
                        .createInMemorySource("sourceId3.pure", "function start():Any[*]\n" +
                                "{\n" +
                                "   assert( myFunc() == 'Random', |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId1.pure", "Enum my::Gender\n" +
                        "{\n" +
                        "   FEMALE\n" +
                        "}\n" +
                        "\n" +
                        "Class my::Person\n" +
                        "{\n" +
                        "   firstName   :   String[0..1];\n" +
                        "   lastName    :   String[0..1];\n" +
                        "   gender      :   my::Gender[0..1];\n" +
                        "}\n")
                        .compileWithExpectedCompileFailureAndAssertions("The enum value 'MALE' can't be found in the enumeration my::Gender", "sourceId2.pure", 6, 75, FastList.newListWith("start__Any_MANY_", "myFunc__Any_MANY_"), FastList.<String>newList(), FastList.newListWith("my::Person", "my::Gender"))
                        .updateSource("sourceId1.pure", "Enum my::Gender\n" +
                                "{\n" +
                                "   MALE, FEMALE\n" +
                                "}\n" +
                                "\n" +
                                "Class my::Person\n" +
                                "{\n" +
                                "   firstName   :   String[0..1];\n" +
                                "   lastName    :   String[0..1];\n" +
                                "   gender      :   my::Gender[0..1];\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test11()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", "Class my::pkg1::Firm\n" +
                        "{\n" +
                        "   legalName : String[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "Class my::pkg2::Person\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}")
                        .createInMemorySource("sourceId2.pure", "Association my::pkg3::Firm_Person\n" +
                                "{\n" +
                                "   firm : my::pkg1::Firm[0..1];\n" +
                                "   employee : my::pkg2::Person[*];\n" +
                                "}")
                        .createInMemorySource("sourceId3.pure", "import my::pkg1::*;\n" +
                                "import my::pkg2::*;\n" +
                                "import my::pkg3::*;\n" +
                                "\n" +
                                "function start():Any[*]\n" +
                                "{\n" +
                                "   let f = ^Firm(legalName='FirmX', employee = [^Person(name='David'),^Person(name='Peter')]);\n" +
                                "   assert($f.legalName == 'FirmX', |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().deleteSource("sourceId2.pure")
                        .compileWithExpectedCompileFailureAndAssertions("The property 'employee' can't be found in the type 'Firm' or in its hierarchy.", "sourceId3.pure", 7, 37, FastList.newListWith("my::pkg1::Firm", "my::pkg2::Person"), FastList.<String>newList(), FastList.newListWith("my::pkg3::Firm_Person"))
                        .createInMemorySource("sourceId2.pure", "Association my::pkg3::Firm_Person\n" +
                                "{\n" +
                                "   firm : my::pkg1::Firm[0..1];\n" +
                                "   employee : my::pkg2::Person[*];\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test12()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", "Class my::enterprise::Vehicle\n" +
                        "{\n" +
                        "   horsePower : Integer[0..1];\n" +
                        "   cost     : Float[1];\n" +
                        "   dateOfManufacture : StrictDate[0..1];\n" +
                        "   description   : String[*];\n" +
                        "   color      : my::enterprise::Color[1..*];\n" +
                        "}\n" +
                        "\n" +
                        "Class my::enterprise::Company\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Enum my::enterprise::Color\n" +
                        "{\n" +
                        "   RED, BLUE, BLACK, WHITE\n" +
                        "}")
                        .createInMemorySource("sourceId2.pure", "import my::enterprise::*;\n" +
                                "\n" +
                                "Class my::enterprise::vehicle::Car extends Vehicle\n" +
                                "{\n" +
                                "   model : String[1];\n" +
                                "   numberOfWheels(){4}:Integer[1];\n" +
                                "   discountedPrice(){$this.cost*.9}:Float[1];\n" +
                                "}\n" +
                                "\n" +
                                "Class my::enterprise::vehicle::Motorcycle extends Vehicle\n" +
                                "{\n" +
                                "   model : String[1];\n" +
                                "   numberOfWheels(){2}:Integer[1];\n" +
                                "   discountedPrice(){$this.cost*.75}:Float[1];\n" +
                                "}\n" +
                                "\n" +
                                "Association my::enterprise::Manufacture\n" +
                                "{\n" +
                                "   company : Company[1];\n" +
                                "   vehicle : Vehicle[*];\n" +
                                "}")
                        .createInMemorySource("sourceId3.pure", "import my::enterprise::vehicle::*;\n" +
                                "import my::enterprise::*;\n" +
                                "\n" +
                                "function start():Any[*]\n" +
                                "{ \n" +
                                "   let maruti_suzuki = ^Car(model='Suzuki', color = Color.BLUE, company = ^Company(name='Maruti'), cost = 10000.0, dateOfManufacture = %2015-01-09);\n" +
                                "   assert($maruti_suzuki.discountedPrice == 9000.0, |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().deleteSource("sourceId2.pure")
                        .compileWithExpectedCompileFailureAndAssertions("Car has not been defined!", "sourceId3.pure", 6, 25, FastList.newListWith("my::enterprise::Color", "start__Any_MANY_"), FastList.<String>newList(), FastList.newListWith("my::enterprise::vehicle::Car", "my::enterprise::vehicle::MotorCycle"))
                        .createInMemorySource("sourceId2.pure", "import my::enterprise::*;\n" +
                                "\n" +
                                "Class my::enterprise::vehicle::Car extends Vehicle\n" +
                                "{\n" +
                                "   model : String[1];\n" +
                                "   numberOfWheels(){4}:Integer[1];\n" +
                                "   discountedPrice(){$this.cost*.9}:Float[1];\n" +
                                "}\n" +
                                "\n" +
                                "Class my::enterprise::vehicle::Motorcycle extends Vehicle\n" +
                                "{\n" +
                                "   model : String[1];\n" +
                                "   numberOfWheels(){2}:Integer[1];\n" +
                                "   discountedPrice(){$this.cost*.75}:Float[1];\n" +
                                "}\n" +
                                "\n" +
                                "Association my::enterprise::Manufacture\n" +
                                "{\n" +
                                "   company : Company[1];\n" +
                                "   vehicle : Vehicle[*];\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test13()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", "Class my::enterprise::Vehicle\n" +
                        "{\n" +
                        "   horsePower : Integer[0..1];\n" +
                        "   cost     : Float[1];\n" +
                        "   dateOfManufacture : StrictDate[0..1];\n" +
                        "   description   : String[*];\n" +
                        "   color      : my::enterprise::Color[1..*];\n" +
                        "}\n" +
                        "\n" +
                        "Class my::enterprise::Company\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Enum my::enterprise::Color\n" +
                        "{\n" +
                        "   RED, BLUE, BLACK, WHITE\n" +
                        "}")
                        .createInMemorySource("sourceId2.pure", "import my::enterprise::*;\n" +
                                "\n" +
                                "Class my::enterprise::vehicle::Car extends Vehicle\n" +
                                "{\n" +
                                "   model : String[1];\n" +
                                "   numberOfWheels(){4}:Integer[1];\n" +
                                "   discountedPrice(){$this.cost*.9}:Float[1];\n" +
                                "}\n" +
                                "\n" +
                                "Class my::enterprise::vehicle::Motorcycle extends Vehicle\n" +
                                "{\n" +
                                "   model : String[1];\n" +
                                "   numberOfWheels(){2}:Integer[1];\n" +
                                "   discountedPrice(){$this.cost*.75}:Float[1];\n" +
                                "}\n" +
                                "\n" +
                                "Association my::enterprise::Manufacture\n" +
                                "{\n" +
                                "   company : Company[1];\n" +
                                "   vehicle : Vehicle[*];\n" +
                                "}")
                        .createInMemorySource("sourceId3.pure", "import my::enterprise::vehicle::*;\n" +
                                "import my::enterprise::*;\n" +
                                "\n" +
                                "function start():Any[*]\n" +
                                "{ \n" +
                                "   let maruti_suzuki = ^Car(model='Suzuki', color = Color.BLUE, company = ^Company(name='Maruti'), cost = 10000.0, dateOfManufacture = %2015-01-09);\n" +
                                "   assert($maruti_suzuki.discountedPrice == 9000.0, |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId1.pure", "Class my::enterprise::Vehicle\n" +
                        "{\n" +
                        "   horsePower : Integer[0..1];\n" +
                        "   cost     : Float[1];\n" +
                        "   dateOfManufacture : StrictDate[0..1];\n" +
                        "   description   : String[*];\n" +
                        "   color      : my::enterprise::Color[1..*];\n" +
                        "}\n" +
                        "\n" +
                        "Class my::enterprise::Company\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Enum my::enterprise::Color\n" +
                        "{\n" +
                        "   RED, BLUE BLACK, WHITE\n" +
                        "}")
                        .compileWithExpectedParserFailureAndAssertions("expected: one of {'}', ','} found: 'BLACK'", "sourceId1.pure", 17, 14, FastList.newListWith("my::enterprise::vehicle::Motorcycle", "start__Any_MANY_", "my::enterprise::vehicle::Car", "my::enterprise::Manufacture"), FastList.<String>newList(), FastList.newListWith("my::enterprise::Color", "my::enterprise::Vehicle"))
                        .updateSource("sourceId1.pure", "Class my::enterprise::Vehicle\n" +
                                "{\n" +
                                "   horsePower : Integer[0..1];\n" +
                                "   cost     : Float[1];\n" +
                                "   dateOfManufacture : StrictDate[0..1];\n" +
                                "   description   : String[*];\n" +
                                "   color      : my::enterprise::Color[1..*];\n" +
                                "}\n" +
                                "\n" +
                                "Class my::enterprise::Company\n" +
                                "{\n" +
                                "   name : String[1];\n" +
                                "}\n" +
                                "\n" +
                                "Enum my::enterprise::Color\n" +
                                "{\n" +
                                "   RED, BLUE, BLACK, WHITE\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    @Ignore("Inconsistency of new operator in compiled vs interpreted mode")
    public void test14()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", "Class my::enterprise::Vehicle\n" +
                        "{\n" +
                        "   horsePower : Integer[0..1];\n" +
                        "   cost     : Float[1];\n" +
                        "   dateOfManufacture : StrictDate[0..1];\n" +
                        "   description   : String[*];\n" +
                        "   color      : my::enterprise::Color[1..*];\n" +
                        "}\n" +
                        "\n" +
                        "Class my::enterprise::Company\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Enum my::enterprise::Color\n" +
                        "{\n" +
                        "   RED, BLUE, BLACK, WHITE\n" +
                        "}")
                        .createInMemorySource("sourceId2.pure", "import my::enterprise::*;\n" +
                                "\n" +
                                "Class my::enterprise::vehicle::Car extends Vehicle\n" +
                                "{\n" +
                                "   model : String[1];\n" +
                                "   numberOfWheels(){4}:Integer[1];\n" +
                                "   discountedPrice(){$this.cost*.9}:Float[1];\n" +
                                "}\n" +
                                "\n" +
                                "Class my::enterprise::vehicle::Motorcycle extends Vehicle\n" +
                                "{\n" +
                                "   model : String[1];\n" +
                                "   numberOfWheels(){2}:Integer[1];\n" +
                                "   discountedPrice(){$this.cost*.75}:Float[1];\n" +
                                "}\n" +
                                "\n" +
                                "Association my::enterprise::Manufacture\n" +
                                "{\n" +
                                "   company : Company[1];\n" +
                                "   vehicle : Vehicle[*];\n" +
                                "}")
                        .createInMemorySource("sourceId3.pure", "import my::enterprise::vehicle::*;\n" +
                                "import my::enterprise::*;\n" +
                                "\n" +
                                "function start():Any[*]\n" +
                                "{ \n" +
                                "   let maruti_suzuki = ^Car(model='Suzuki', color = Color.BLUE, company = ^Company(name='Maruti'), cost = 10000.0, dateOfManufacture = %2015-01-09);\n" +
                                "   assert($maruti_suzuki.discountedPrice == 9000.0, |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId3.pure", "import my::enterprise::vehicle::*;\n" +
                        "import my::enterprise::*;\n" +
                        "\n" +
                        "function start():Any[*]\n" +
                        "{ \n" +
                        "   let maruti_suzuki = ^Car(model='Suzuki', color = Color.BLUE, company = ^Company(name='Maruti'), cost = 10000.0, dateOfManufacture = %2015-01-09);\n" +
                        "   let maruti = ^Company(name='Maruti', vehicle = [$maruti_suzuki]);\n" +
                        "   assert($maruti_suzuki.discountedPrice == 9000.0, |'');\n" +
                        "}")
                        .executeFunctionWithExpectedExecutionFailureandAssertions("start():Any[*]", "Error instantiating the type 'Car'. The property 'company' has a multiplicity range of [1] when the given list has a cardinality equal to 2", "sourceId3.pure", 7, 17, FastList.newListWith("my::enterprise::vehicle::Motorcycle", "start__Any_MANY_", "my::enterprise::vehicle::Car", "my::enterprise::Manufacture"), FastList.<String>newList(), FastList.<String>newList())
                        .updateSource("sourceId3.pure", "import my::enterprise::vehicle::*;\n" +
                                "import my::enterprise::*;\n" +
                                "\n" +
                                "function start():Any[*]\n" +
                                "{ \n" +
                                "   let maruti_suzuki = ^Car(model='Suzuki', color = Color.BLUE, company = ^Company(name='Maruti'), cost = 10000.0, dateOfManufacture = %2015-01-09);\n" +
                                "   assert($maruti_suzuki.discountedPrice == 9000.0, |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test15()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/model/sourceId1.pure", "function myFunc1():Integer[1]\n" +
                        "{\n" +
                        "   1;\n" +
                        "}")
                        .createInMemorySource("/datamart_other/sourceId2.pure", "function myFunc2():Any[*]\n" +
                                "{\n" +
                                "   myFunc1();\n" +
                                "}")
                        .createInMemorySource("sourceId3.pure", "function start():Any[*]\n" +
                                "{\n" +
                                "   assert(myFunc2() == 1, |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().deleteSource("/datamart_other/sourceId2.pure")
                        .compileWithExpectedCompileFailureAndAssertions("The system can't find a match for the function: myFunc2()", "sourceId3.pure", 3, 11, FastList.newListWith("myFunc1__Integer_1_", "start__Any_MANY_"), FastList.<String>newList(), FastList.newListWith("myFunc2__Any_MANY_"))
                        .createInMemorySource("/datamart_other/sourceId2.pure", "function myFunc2():Any[*]\n" +
                                "{\n" +
                                "   myFunc1();\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test16()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/system/tests/sourceId1.pure", "Class my::enterprise::Vehicle\n" +
                        "{\n" +
                        "   horsePower : Integer[0..1];\n" +
                        "   cost     : Float[1];\n" +
                        "   dateOfManufacture : StrictDate[0..1];\n" +
                        "   description   : String[*];\n" +
                        "   color      : my::enterprise::Color[1..*];\n" +
                        "}\n" +
                        "\n" +
                        "Class my::enterprise::Company\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Enum my::enterprise::Color\n" +
                        "{\n" +
                        "   RED, BLUE, BLACK, WHITE\n" +
                        "}")
                        .createInMemorySource("/datamart_other/sourceId2.pure", "import my::enterprise::*;\n" +
                                "\n" +
                                "Class my::enterprise::vehicle::Car extends my::enterprise::Vehicle\n" +
                                "{\n" +
                                "   model : String[1];\n" +
                                "   numberOfWheels(){4}:Integer[1];\n" +
                                "   discountedPrice(){$this.cost*.9}:Float[1];\n" +
                                "}\n" +
                                "\n" +
                                "Class my::enterprise::vehicle::Motorcycle extends my::enterprise::Vehicle\n" +
                                "{\n" +
                                "   model : String[1];\n" +
                                "   numberOfWheels(){2}:Integer[1];\n" +
                                "   discountedPrice(){$this.cost*.75}:Float[1];\n" +
                                "}\n" +
                                "\n" +
                                "Association my::enterprise::Manufacture\n" +
                                "{\n" +
                                "   company : Company[1];\n" +
                                "   vehicle : Vehicle[*];\n" +
                                "}\n")
                        .createInMemorySource("/datamart_other/domain/sourceId3.pure", "import my::enterprise::vehicle::*;\n" +
                                "import my::enterprise::*;\n" +
                                "\n" +
                                "function start():Any[*]\n" +
                                "{ \n" +
                                "   let maruti_suzuki = ^Car(model='Suzuki', color = Color.BLUE, company = ^Company(name='Maruti'), cost = 10000.0, dateOfManufacture = %2015-01-09);\n" +
                                "   assert($maruti_suzuki.discountedPrice == 9000.0, |'');\n" +
                                "   assert($maruti_suzuki.company.vehicle->at(0)->cast(@Car).numberOfWheels == 4, |'');\n" +
                                "}\n")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("/datamart_other/sourceId2.pure", "import my::enterprise::*;\n" +
                        "\n" +
                        "Class my::enterprise::vehicle::Car \n" +
                        "{\n" +
                        "   model : String[1];\n" +
                        "   numberOfWheels(){4}:Integer[1];\n" +
                        "   discountedPrice(){$this.cost*.9}:Float[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class my::enterprise::vehicle::Motorcycle \n" +
                        "{\n" +
                        "   model : String[1];\n" +
                        "   numberOfWheels(){2}:Integer[1];\n" +
                        "   discountedPrice(){$this.cost*.75}:Float[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association my::enterprise::Manufacture\n" +
                        "{\n" +
                        "   company : Company[1];\n" +
                        "   vehicle : Vehicle[*];\n" +
                        "}\n")
                        .compileWithExpectedCompileFailureAndAssertions("Can't find the property 'company' in the class my::enterprise::vehicle::Car", "/datamart_other/domain/sourceId3.pure", 8, 26, FastList.newListWith("start__Any_MANY_", "my::enterprise::Color", "my::enterprise::Company", "my::enterprise::Vehicle"), FastList.<String>newList(), FastList.newListWith("my::enterprise::vehicle::Car", "my::enterprise::vehicle::Motorcycle"))
                        .updateSource("/datamart_other/sourceId2.pure", "import my::enterprise::*;\n" +
                                "\n" +
                                "Class my::enterprise::vehicle::Car extends my::enterprise::Vehicle\n" +
                                "{\n" +
                                "   model : String[1];\n" +
                                "   numberOfWheels(){4}:Integer[1];\n" +
                                "   discountedPrice(){$this.cost*.9}:Float[1];\n" +
                                "}\n" +
                                "\n" +
                                "Class my::enterprise::vehicle::Motorcycle extends my::enterprise::Vehicle\n" +
                                "{\n" +
                                "   model : String[1];\n" +
                                "   numberOfWheels(){2}:Integer[1];\n" +
                                "   discountedPrice(){$this.cost*.75}:Float[1];\n" +
                                "}\n" +
                                "\n" +
                                "Association my::enterprise::Manufacture\n" +
                                "{\n" +
                                "   company : Company[1];\n" +
                                "   vehicle : Vehicle[*];\n" +
                                "}\n")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test17()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/system/tests/sourceId1.pure", "Class my::enterprise::Vehicle\n" +
                        "{\n" +
                        "   horsePower : Integer[0..1];\n" +
                        "   cost     : Float[1];\n" +
                        "   dateOfManufacture : StrictDate[0..1];\n" +
                        "   description   : String[*];\n" +
                        "   color      : my::enterprise::Color[1..*];\n" +
                        "}\n" +
                        "\n" +
                        "Class my::enterprise::vehicle::Car extends my::enterprise::Vehicle\n" +
                        "{\n" +
                        "   model : String[1];\n" +
                        "   numberOfWheels(){4}:Integer[1];\n" +
                        "   discountedPrice(){$this.cost*.9}:Float[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class my::enterprise::vehicle::Motorcycle extends my::enterprise::Vehicle\n" +
                        "{\n" +
                        "   model : String[1];\n" +
                        "   numberOfWheels(){2}:Integer[1];\n" +
                        "   discountedPrice(){$this.cost*.75}:Float[1];\n" +
                        "}\n")
                        .createInMemorySource("/system/tests/resources/sourceId2.pure", "import my::enterprise::*;\n" +
                                "\n" +
                                "Class my::enterprise::Company\n" +
                                "{\n" +
                                "   name : String[1];\n" +
                                "}\n" +
                                "\n" +
                                "Association my::enterprise::Manufacture\n" +
                                "{\n" +
                                "   company : Company[1];\n" +
                                "   vehicle : Vehicle[*];\n" +
                                "}\n" +
                                "\n" +
                                "Enum my::enterprise::Color\n" +
                                "{\n" +
                                "   RED, BLUE, BLACK, WHITE\n" +
                                "}")
                        .createInMemorySource("/model/domain/sourceId3.pure", "import my::enterprise::vehicle::*;\n" +
                                "import my::enterprise::*;\n" +
                                "\n" +
                                "function start():Any[*]\n" +
                                "{ \n" +
                                "   let maruti_suzuki = ^Car(model='Suzuki', color = Color.BLUE, company = ^Company(name='Maruti'), cost = 10000.0, dateOfManufacture = %2015-01-09);\n" +
                                "   assert($maruti_suzuki.discountedPrice == 9000.0, |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().deleteSource("/system/tests/sourceId1.pure")
                        .compileWithExpectedCompileFailureAndAssertions("Vehicle has not been defined!", "/system/tests/resources/sourceId2.pure", 11, 14, FastList.newListWith("start__Any_MANY_", "my::enterprise::Color", "my::enterprise::Company", "my::enterprise::Manufacture"), FastList.<String>newList(), FastList.newListWith("my::enterprise::vehicle::Car", "my::enterprise::vehicle::Motorcycle", "my::enterprise::Vehicle"))
                        .createInMemorySource("/system/tests/sourceId1.pure", "Class my::enterprise::Vehicle\n" +
                                "{\n" +
                                "   horsePower : Integer[0..1];\n" +
                                "   cost     : Float[1];\n" +
                                "   dateOfManufacture : StrictDate[0..1];\n" +
                                "   description   : String[*];\n" +
                                "   color      : my::enterprise::Color[1..*];\n" +
                                "}\n" +
                                "\n" +
                                "Class my::enterprise::vehicle::Car extends my::enterprise::Vehicle\n" +
                                "{\n" +
                                "   model : String[1];\n" +
                                "   numberOfWheels(){4}:Integer[1];\n" +
                                "   discountedPrice(){$this.cost*.9}:Float[1];\n" +
                                "}\n" +
                                "\n" +
                                "Class my::enterprise::vehicle::Motorcycle extends my::enterprise::Vehicle\n" +
                                "{\n" +
                                "   model : String[1];\n" +
                                "   numberOfWheels(){2}:Integer[1];\n" +
                                "   discountedPrice(){$this.cost*.75}:Float[1];\n" +
                                "}\n")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test18()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/system/tests/sourceId1.pure", "Class A\n" +
                        "{\n" +
                        "   propertyA1 : Integer[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class B\n" +
                        "{\n" +
                        "   propertyB1 : StrictDate[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class C\n" +
                        "{\n" +
                        "   propertyC1 : String[0..1];\n" +
                        "}")
                        .createInMemorySource("/system/tests/resources/sourceId2.pure", "Association AB\n" +
                                "{\n" +
                                "   a   :   A[1];\n" +
                                "   b   :   B[1];\n" +
                                "}\n" +
                                "\n" +
                                "Association AC\n" +
                                "{\n" +
                                "   a   :   A[1];\n" +
                                "   c   :   C[*];\n" +
                                "}")
                        .createInMemorySource("/model/domain/sourceId3.pure", "function start():Any[*]\n" +
                                "{\n" +
                                "   let objA = ^A(b=^B(propertyB1=%2018-01-01));\n" +
                                "   assert($objA.b.propertyB1 == %2018-01-01, |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().deleteSource("/system/tests/resources/sourceId2.pure")
                        .compileWithExpectedCompileFailureAndAssertions("Can't find the property 'b' in the class A", "/model/domain/sourceId3.pure", 4, 17, FastList.newListWith("start__Any_MANY_", "A", "B", "C"), FastList.<String>newList(), FastList.newListWith("AB", "AC"))
                        .createInMemorySource("/system/tests/resources/sourceId2.pure", "Association AB\n" +
                                "{\n" +
                                "   a   :   A[1];\n" +
                                "   b   :   B[1];\n" +
                                "}\n" +
                                "\n" +
                                "Association AC\n" +
                                "{\n" +
                                "   a   :   A[1];\n" +
                                "   c   :   C[*];\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test19()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/system/tests/sourceId1.pure", "Class A\n" +
                        "{\n" +
                        "   propertyA1 : Integer[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class B\n" +
                        "{\n" +
                        "   propertyB1 : StrictDate[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class C\n" +
                        "{\n" +
                        "   propertyC1 : String[0..1];\n" +
                        "}")
                        .createInMemorySource("/system/tests/resources/sourceId2.pure", "Association AB\n" +
                                "{\n" +
                                "   a   :   A[1];\n" +
                                "   b   :   B[1];\n" +
                                "}\n" +
                                "\n" +
                                "Association AC\n" +
                                "{\n" +
                                "   a   :   A[1];\n" +
                                "   c   :   C[*];\n" +
                                "}")
                        .createInMemorySource("/model/domain/sourceId3.pure", "function start():Any[*]\n" +
                                "{\n" +
                                "   let objA = ^A(b=^B(propertyB1=%2018-01-01));\n" +
                                "   assert($objA.b.propertyB1 == %2018-01-01, |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("/system/tests/resources/sourceId2.pure", "Association AB\n" +
                        "{\n" +
                        "   a   :   A[1];\n" +
                        "   b   :   B[0];\n" +
                        "}\n" +
                        "\n" +
                        "Association AC\n" +
                        "{\n" +
                        "   a   :   A[1];\n" +
                        "   c   :   C[*];\n" +
                        "}")
                        .compileWithExpectedCompileFailureAndAssertions("Multiplicity Error: [1] is not compatible with [0]", "/model/domain/sourceId3.pure", 3, 19, FastList.newListWith("start__Any_MANY_", "A", "B", "C", "AB", "AC"), FastList.<String>newList(), FastList.<String>newList())
                        .updateSource("/system/tests/resources/sourceId2.pure", "Association AB\n" +
                                "{\n" +
                                "   a   :   A[1];\n" +
                                "   b   :   B[1];\n" +
                                "}\n" +
                                "\n" +
                                "Association AC\n" +
                                "{\n" +
                                "   a   :   A[1];\n" +
                                "   c   :   C[*];\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    @Ignore("Inconsistency of new operator in compiled vs interpreted mode")
    public void test20()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/system/tests/sourceId1.pure", "Class A\n" +
                        "{\n" +
                        "   propertyA1 : Integer[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class B\n" +
                        "{\n" +
                        "   propertyB1 : StrictDate[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class C\n" +
                        "{\n" +
                        "   propertyC1 : String[0..1];\n" +
                        "}")
                        .createInMemorySource("/system/tests/resources/sourceId2.pure", "Association AB\n" +
                                "{\n" +
                                "   a   :   A[1];\n" +
                                "   b   :   B[1];\n" +
                                "}\n" +
                                "\n" +
                                "Association AC\n" +
                                "{\n" +
                                "   a   :   A[1];\n" +
                                "   c   :   C[*];\n" +
                                "}")
                        .createInMemorySource("/model/domain/sourceId3.pure", "function start():Any[*]\n" +
                                "{\n" +
                                "   let objA = ^A(b=^B(propertyB1=%2018-01-01));\n" +
                                "   assert($objA.b.propertyB1 == %2018-01-01, |'');\n" +
                                "   \n" +
                                "   let objC = ^C(a=$objA);\n" +
                                "   assert($objC.a.b.propertyB1 == %2018-01-01, |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("/system/tests/sourceId1.pure", "Class A\n" +
                        "{\n" +
                        "   propertyA1 : Integer[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class B\n" +
                        "{\n" +
                        "   propertyB1 : StrictDate[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class C\n" +
                        "{\n" +
                        "   propertyC1 : String[1];\n" +
                        "}")
                        .compileWithExpectedCompileFailureAndAssertions("Missing value(s) for required property 'propertyC1' which has a multiplicity of [1] for type C", "/model/domain/sourceId3.pure", 6, 15, FastList.newListWith("start__Any_MANY_", "A", "B", "C", "AB", "AC"), Lists.immutable.<String>empty(), Lists.immutable.<String>empty())
                        .updateSource("/system/tests/sourceId1.pure", "Class A\n" +
                                "{\n" +
                                "   propertyA1 : Integer[*];\n" +
                                "}\n" +
                                "\n" +
                                "Class B\n" +
                                "{\n" +
                                "   propertyB1 : StrictDate[1];\n" +
                                "}\n" +
                                "\n" +
                                "Class C\n" +
                                "{\n" +
                                "   propertyC1 : String[0..1];\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    @Ignore("Inconsistency of new operator in compiled vs interpreted mode")
    public void test21()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/system/tests/sourceId1.pure", "Class A\n" +
                        "{\n" +
                        "   propertyA1 : Integer[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class B\n" +
                        "{\n" +
                        "   propertyB1 : StrictDate[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class C\n" +
                        "{\n" +
                        "   propertyC1 : String[0..1];\n" +
                        "}")
                        .createInMemorySource("/system/tests/resources/sourceId2.pure", "Association AB\n" +
                                "{\n" +
                                "   a   :   A[1];\n" +
                                "   b   :   B[1];\n" +
                                "}\n" +
                                "\n" +
                                "Association AC\n" +
                                "{\n" +
                                "   a   :   A[1];\n" +
                                "   c   :   C[*];\n" +
                                "}")
                        .createInMemorySource("/model/domain/sourceId3.pure", "function start():Any[*]\n" +
                                "{\n" +
                                "   let objA = ^A(b=^B(propertyB1=%2018-01-01));\n" +
                                "   assert($objA.b.propertyB1 == %2018-01-01, |'');\n" +
                                "   \n" +
                                "   let objC = ^C(a=$objA);\n" +
                                "   assert($objC.a.b.propertyB1 == %2018-01-01, |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("/system/tests/resources/sourceId2.pure", "Association AB\n" +
                        "{\n" +
                        "   a   :   A[1];\n" +
                        "   b   :   B[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association AC\n" +
                        "{\n" +
                        "   a   :   A[1];\n" +
                        "   c   :   C[*];\n" +
                        "}\n" +
                        "\n" +
                        "Association BC\n" +
                        "{\n" +
                        "   b   :   B[1];\n" +
                        "   c   :   C[*];\n" +
                        "}")
                        .executeFunctionWithExpectedExecutionFailureandAssertions("start():Any[*]", "Error instantiating class 'C'.  The following properties have multiplicity violations: 'b' requires 1 value, got 0", "/model/domain/sourceId3.pure", 6, 15, FastList.newListWith("start__Any_MANY_", "A", "B", "C", "AB", "AC", "BC"), FastList.<String>newList(), FastList.<String>newList())
                        .updateSource("/model/domain/sourceId3.pure", "function start():Any[*]\n" +
                                "{\n" +
                                "   let objA = ^A(b=^B(propertyB1=%2018-01-01));\n" +
                                "   assert($objA.b.propertyB1 == %2018-01-01, |'');\n" +
                                "   \n" +
                                "   let objC = ^C(a=$objA, b=$objA.b);\n" +
                                "   assert($objC.a.b.propertyB1 == %2018-01-01, |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]")
                        .updateSource("/system/tests/resources/sourceId2.pure", "Association AB\n" +
                                "{\n" +
                                "   a   :   A[1];\n" +
                                "   b   :   B[1];\n" +
                                "}\n" +
                                "\n" +
                                "Association AC\n" +
                                "{\n" +
                                "   a   :   A[1];\n" +
                                "   c   :   C[*];\n" +
                                "}")
                        .updateSource("/model/domain/sourceId3.pure", "function start():Any[*]\n" +
                                "{\n" +
                                "   let objA = ^A(b=^B(propertyB1=%2018-01-01));\n" +
                                "   assert($objA.b.propertyB1 == %2018-01-01, |'');\n" +
                                "   \n" +
                                "   let objC = ^C(a=$objA);\n" +
                                "   assert($objC.a.b.propertyB1 == %2018-01-01, |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test22()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/system/tests/sourceId1.pure", "Class A\n" +
                        "{\n" +
                        "   propertyA1 : Integer[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class B\n" +
                        "{\n" +
                        "   propertyB1 : StrictDate[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class C\n" +
                        "{\n" +
                        "   propertyC1 : String[0..1];\n" +
                        "}")
                        .createInMemorySource("/system/tests/resources/sourceId2.pure", "Association AB\n" +
                                "{\n" +
                                "   a   :   A[1];\n" +
                                "   b   :   B[1];\n" +
                                "}\n")
                        .createInMemorySource("/model/domain/sourceId3.pure", "function start():Any[*]\n" +
                                "{\n" +
                                "   let objA = ^A(b=^B(propertyB1=%2018-01-01));\n" +
                                "   assert($objA.b.propertyB1 == %2018-01-01, |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("/system/tests/sourceId1.pure", "Class my::pkgA::A\n" +
                        "{\n" +
                        "   propertyA1 : Integer[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class B\n" +
                        "{\n" +
                        "   propertyB1 : StrictDate[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class C\n" +
                        "{\n" +
                        "   propertyC1 : String[0..1];\n" +
                        "}")
                        .compileWithExpectedCompileFailureAndAssertions("A has not been defined! The system found 1 possible matches:\n    my::pkgA::A", "/system/tests/resources/sourceId2.pure", 3, 12, FastList.newListWith("start__Any_MANY_", "AB"), FastList.<String>newList(), FastList.newListWith("A"))
                        .updateSource("/model/domain/sourceId3.pure", "import my::pkgA::*;\n" +
                                "\n" +
                                "function start():Any[*]\n" +
                                "{\n" +
                                "   let objA = ^A(b=^B(propertyB1=%2018-01-01));\n" +
                                "   assert($objA.b.propertyB1 == %2018-01-01, |'');\n" +
                                "   \n" +
                                "   let objC = ^C(a=$objA, b=$objA.b);\n" +
                                "   assert($objC.a.b.propertyB1 == %2018-01-01, |'');\n" +
                                "}")
                        .updateSource("/system/tests/resources/sourceId2.pure", "import my::pkgA::*;\n" +
                                "\n" +
                                "Association AB\n" +
                                "{\n" +
                                "   a   :   A[1];\n" +
                                "   b   :   B[1];\n" +
                                "}\n" +
                                "\n" +
                                "Association AC\n" +
                                "{\n" +
                                "   a   :   A[1];\n" +
                                "   c   :   C[*];\n" +
                                "}\n" +
                                "\n" +
                                "Association BC\n" +
                                "{\n" +
                                "   b   :   B[1];\n" +
                                "   c   :   C[*];\n" +
                                "}")
                        .executeFunction("start():Any[*]")
                        .updateSource("/system/tests/sourceId1.pure", "Class A\n" +
                                "{\n" +
                                "   propertyA1 : Integer[*];\n" +
                                "}\n" +
                                "\n" +
                                "Class B\n" +
                                "{\n" +
                                "   propertyB1 : StrictDate[1];\n" +
                                "}\n" +
                                "\n" +
                                "Class C\n" +
                                "{\n" +
                                "   propertyC1 : String[0..1];\n" +
                                "}")
                        .updateSource("/system/tests/resources/sourceId2.pure", "Association AB\n" +
                                "{\n" +
                                "   a   :   A[1];\n" +
                                "   b   :   B[1];\n" +
                                "}\n"
                        )
                        .updateSource("/model/domain/sourceId3.pure", "function start():Any[*]\n" +
                                "{\n" +
                                "   let objA = ^A(b=^B(propertyB1=%2018-01-01));\n" +
                                "   assert($objA.b.propertyB1 == %2018-01-01, |'');\n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test23()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/model/domain/sourceId1.pure", "Class <<temporal.businesstemporal>> A\n" +
                        "{\n" +
                        "   propertyA1 : Integer[*];\n" +
                        "   qualifiedProp(){$this.b(%latest).propertyB1}:StrictDate[*];\n" +
                        "}\n" +
                        "\n")
                        .createInMemorySource("/model/domain/sourceId2.pure", "Association <<temporal.businesstemporal>> AB\n" +
                                "{\n" +
                                "   a   :   A[1];\n" +
                                "   b   :   B[*];\n" +
                                "}\n")
                        .createInMemorySource("/model/domain/sourceId3.pure", "Class <<temporal.businesstemporal>> B\n" +
                                "{\n" +
                                "   propertyB1 : StrictDate[1];\n" +
                                "}\n" +
                                "\n" +
                                "function start():Any[*]\n" +
                                "{\n" +
                                "   let objA = ^A(businessDate = [%2018-01-01]);\n" +
                                "   assert($objA.businessDate == [%2018-01-01], |'');\n" +
                                "   \n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("/model/domain/sourceId3.pure", "Class B\n" +
                        "{\n" +
                        "   propertyB1 : StrictDate[1];\n" +
                        "}\n" +
                        "\n" +
                        "function start():Any[*]\n" +
                        "{\n" +
                        "   let objA = ^A(businessDate = [%2018-01-01]);\n" +
                        "   assert($objA.businessDate == [%2018-01-01], |'');\n" +
                        "   \n" +
                        "}")
                        .compileWithExpectedCompileFailureAndAssertions("The system can't find a match for the function: b(_:A[1],_:LatestDate[1])", "/model/domain/sourceId1.pure", 4, 26, FastList.newListWith("A", "AB"), FastList.<String>newList(), FastList.newListWith("B"))
                        .updateSource("/model/domain/sourceId3.pure", "Class <<temporal.businesstemporal>> B\n" +
                                "{\n" +
                                "   propertyB1 : StrictDate[1];\n" +
                                "}\n" +
                                "\n" +
                                "function start():Any[*]\n" +
                                "{\n" +
                                "   let objA = ^A(businessDate = [%2018-01-01]);\n" +
                                "   assert($objA.businessDate == [%2018-01-01], |'');\n" +
                                "   \n" +
                                "}")
                        .executeFunction("start():Any[*]"),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test24()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("s1.pure", "" +
                        "Class <<temporal.businesstemporal>> A {a: String[1];} \n\n Class <<temporal.businesstemporal>> B {b: String[1];}")
                        .createInMemorySource("3.pure", "" +
                                "Class <<temporal.businesstemporal>> E {e: String[1];} \n\n Class <<temporal.businesstemporal>> F {f: String[1];}\n\nClass <<temporal.businesstemporal>>G {g: String[1]; gQualified(){$this.g}:String[1];}" +
                                "\nAssociation\nmyAssoc2{\n assoc2C: C[1]; \n assocF: F[1];\n}\n\n\nAssociation\nmyAssoc3{\n assoc3F: F[1]; \n assocG: G[1];\n}")
                        .createInMemorySource("2.pure", "Class <<temporal.businesstemporal>> {doc.doc='Hello'} C extends A {\nthisB: B[1];\n en: myEnum[1];\nthisE: E[1];\nthisE2: E[1];\nthisE3: E[*];}\nEnum myEnum{AAA,\nBBB}\n" +
                                "\nAssociation\nmyAssoc{\n assocC: C[1]; \n assocD: D[1];}\nClass <<temporal.businesstemporal>> D {d: String[1];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("2.pure", "Class\n <<temporal.businesstemporal>> {doc.doc='Hello'} C extends A {\nthisB: B[1];\n en: myEnum[1];\nthisE: E[1];\nthisE2: E[1];\nthisE3: E[*];}\nEnum myEnum{AAA,\nBBB}\n" +
                                "\nAssociation\n\nmyAssoc{\n assocC: C[1]; \n assocD: D[1];}\nClass <<temporal.businesstemporal>> D {d: String[1];}")
                        .compile()
                        .updateSource("2.pure", "Class <<temporal.businesstemporal>> {doc.doc='Hello'} C extends A {\nthisB: B[1];\n en: myEnum[1];\nthisE: E[1];\nthisE2: E[1];\nthisE3: E[*];}\nEnum myEnum{AAA,\nBBB}\n" +
                                "\nAssociation\nmyAssoc{\n assocC: C[1]; \n assocD: D[1];}\nClass <<temporal.businesstemporal>> D {d: String[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test25()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("s1.pure", "Class <<temporal.businesstemporal>> A {a: String[1]; c: C[1];} \n Class B{hubA: A[1];}")
                        .createInMemorySource("s2.pure", "Class C{c: String[1];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("s2.pure", "Class\n C{c: String[1];}")
                        .compile()
                        .updateSource("s2.pure", "Class C{c: String[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test26()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("s1.pure", "Class <<temporal.businesstemporal>> A {a: String[1];} \n Class  <<temporal.businesstemporal>> B{b: String[1];}\n Association AB {ab: B[1]; ba: A[1];}")
                        .createInMemorySource("s4.pure", "Class <<temporal.businesstemporal>> E {e: String[1];} ")
                        .createInMemorySource("s2.pure", "Class C{c: String[1]; a: A[1]; }")
                        .createInMemorySource("s3.pure", "Class <<temporal.businesstemporal>> D{c: C[1]; e:E[1];}")
                        .createInMemorySource("s5.pure", "function usage():Any[*]{{|D.all(%latest).e.e}}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("s2.pure", "Class\n C{c: String[1]; a: A[1]; }")
                        .compile()
                        .updateSource("s2.pure", "Class C{c: String[1]; a: A[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test27()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("s1.pure", "Class A {}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("s1.pure", "Class A {}\nClass A {}")
                        .compileWithExpectedParserFailure("The element 'A' already exists in the package '::'", "s1.pure", 2, 7)
                        .updateSource("s1.pure", "Class A {}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }
}
