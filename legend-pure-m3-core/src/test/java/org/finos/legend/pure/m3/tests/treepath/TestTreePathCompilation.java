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

package org.finos.legend.pure.m3.tests.treepath;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

// TODO: Investigate tests with setUp() added, those are causing other tests fail when runtime is shared
public class TestTreePathCompilation extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime() {
        runtime.delete("file.pure");
        runtime.delete("function.pure");
    }


    @Test
    public void testExceptionScenarios() throws Exception
    {
        try
        {
            this.runtime.createInMemorySource("file.pure", "Class Person{address:Address[1];} Class Firm<T> {employees : Person[1];address:Address[1];} Class Address{}\n" +
                    "function test():Any[*]\n" +
                    "{\n" +
                    "    print(#UnknownFirm" +
                    "          { " +
                    "             *" +
                    "          }#,2);\n" +
                    "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException("UnknownFirm has not been defined!", 4, 12, e);
        }

        try
        {
            this.runtime.modify("file.pure", "Class Person{address:Address[1];} Class Firm<T> {employees : Person[1];address:Address[1];} Class Address{}\n" +
                    "function test():Any[*]\n" +
                    "{\n" +
                    "    print(#Firm" +
                    "          {" +
                    "               *" +
                    "          }#,1)\n" +
                    "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException("Type argument mismatch for the class Firm<T> (expected 1, got 0): Firm", 4, 12, e);
        }


        try
        {
            this.runtime.modify("file.pure", "Class Person{address:Address[1];} Class Firm<T> {employees : Person[1];address:Address[1];} Class Address{}\n" +
                    "function test():Any[*]\n" +
                    "{\n" +
                    "    print(#Firm<BlaBla>{" +
                    "   *" +
                    "}#,2);\n" +
                    "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException("TreePath doesn't support GenericTypes", 4, 12, e);
        }

        try
        {
            final String code = "Class Person{address:Address[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{}\n" +
                    "function test():Any[*]\n" +
                    "{\n" +
                    "    print(#Firm{\n" +
                    "                 +[employee]  \n" +
                    "               }#,2);\n" +
                    "}\n";
            this.runtime.modify("file.pure", code);
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {

            assertPureException("The property 'employee' can't be found in the type 'Firm' (or any supertype).", 5, 20, e);
        }

        try
        {
            this.runtime.modify("file.pure", "Class Person{address:Address[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{}\n" +
                    "function test():Any[*]\n" +
                    "{\n" +
                    "    print(#\n" +
                    "           Firm\n" +
                    "           {\n" +
                    "               employees \n" +
                    "               {\n" +
                    "                   address2 {} \n" +
                    "               }" +
                    "           }" +
                    "          #,2);\n" +
                    "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException("The property 'address2' can't be found in the type 'Person' (or any supertype).", 9, 20, e);
        }
    }

    @Test
    public void testSimpleTreePath() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", "Class Person{ name: String[1];address:Address[1]; firm: Firm[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{ street:String[1]; }\n" +
                "function test():Any[*]\n" +
                "{\n" +
                " let t = " +
                "#Person" +
                "{ \n" +
                "             *   " +
                "             firm { employees as Person }" +
                "             address as MyKingdom { * }" +
                "}#;" +
                "}\n");
        this.runtime.compile();
        CoreInstance func = this.runtime.getCoreInstance("test__Any_MANY_");
        CoreInstance tree = func.getValueForMetaPropertyToMany("expressionSequence").getFirst().getValueForMetaPropertyToMany("parametersValues").getLast().getValueForMetaPropertyToMany("values").getFirst();

        Assert.assertNotNull(tree);
        RichIterable<? extends CoreInstance> children = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.children, processorSupport);
        Assert.assertEquals("Missing children", 2, children.size());

        RichIterable<? extends CoreInstance> simpleProperties = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.resolvedProperties, processorSupport);
        Assert.assertEquals("Missing simpleProperties", 1, simpleProperties.size());

        CoreInstance firm = children.getFirst();
        RichIterable<? extends CoreInstance> firmChildren = Instance.getValueForMetaPropertyToManyResolved(firm, M3Properties.children, processorSupport);
        Assert.assertEquals("Missing children", 1, firmChildren.size());
        CoreInstance address = children.getLast();
        RichIterable<? extends CoreInstance> addressChildren = Instance.getValueForMetaPropertyToManyResolved(address, M3Properties.children, processorSupport);
        Assert.assertEquals(0, addressChildren.size());

        RichIterable<? extends CoreInstance> simpleAddressProperties = Instance.getValueForMetaPropertyToManyResolved(address, M3Properties.resolvedProperties, processorSupport);
        Assert.assertEquals("Missing simpleProperties", 1, simpleAddressProperties.size());

        setUp();
    }


    @Test
    public void testTreePathIncludeAll() throws Exception
    {
        String code = "Class Person{ name: String[1];address:Address[1]; firm: Firm[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{ street:String[1]; }\n" +
                "function test():Any[*]\n" +
                "{\n" +
                " let t = " +
                "#Person" +
                "{ \n" +
                "             *   " +
                "             firm { employees as Person }" +
                "             address as MyKingdom {  }" +
                "}#;" +
                "}\n";
        this.runtime.createInMemorySource("file.pure", code);
        this.runtime.compile();
        CoreInstance func = this.runtime.getCoreInstance("test__Any_MANY_");
        CoreInstance tree = func.getValueForMetaPropertyToMany("expressionSequence").getFirst().getValueForMetaPropertyToMany("parametersValues").getLast().getValueForMetaPropertyToMany("values").getFirst();

        Assert.assertNotNull(tree);
        RichIterable<? extends CoreInstance> children = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.children, processorSupport);
        Assert.assertEquals("Missing children", 2, children.size());

        RichIterable<? extends CoreInstance> simpleProperties = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.resolvedProperties, processorSupport);
        Assert.assertEquals("Invalid simpleProperties", 1, simpleProperties.size());

        CoreInstance firm = children.getFirst();
        RichIterable<? extends CoreInstance> firmChildren = Instance.getValueForMetaPropertyToManyResolved(firm, M3Properties.children, processorSupport);
        Assert.assertEquals("Missing children", 1, firmChildren.size());
        CoreInstance address = children.getLast();
        RichIterable<? extends CoreInstance> addressChildren = Instance.getValueForMetaPropertyToManyResolved(address, M3Properties.children, processorSupport);
        Assert.assertEquals(0, addressChildren.size());

        RichIterable<? extends CoreInstance> simpleAddressProperties = Instance.getValueForMetaPropertyToManyResolved(address, M3Properties.resolvedProperties, processorSupport);
        Assert.assertEquals("Invalid simpleProperties", 0, simpleAddressProperties.size());

        setUp();
    }

    @Test
    public void testSimpleTreePathWithDerivedProperties() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", "Class Person{ name: String[1];address:Address[1]; firm: Firm[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{ street:String[1]; }\n" +
                "function test():Any[*]\n" +
                "{\n" +
                " let t = " +
                "#Person" +
                "{ \n" +
                "      >theName[ $this.name ]   " +
                "}#;" +
                "}\n");
        this.runtime.compile();
        CoreInstance func = this.runtime.getCoreInstance("test__Any_MANY_");
        CoreInstance tree = func.getValueForMetaPropertyToMany("expressionSequence").getFirst().getValueForMetaPropertyToMany("parametersValues").getLast().getValueForMetaPropertyToMany("values").getFirst();

        Assert.assertNotNull(tree);
        RichIterable<? extends CoreInstance> children = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.children, processorSupport);
        Assert.assertEquals("Missing children", 1, children.size());
        CoreInstance derivedProperty = children.getFirst();
        Assert.assertEquals("theName", derivedProperty.getValueForMetaPropertyToOne("name").getName());
        Assert.assertEquals("theName", derivedProperty.getValueForMetaPropertyToOne("propertyName").getName());
    }

    @Test
    public void testSimpleTreePathWithDerivedLiteralProperties() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", "Class Person{ name: String[1];address:Address[1]; firm: Firm[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{ street:String[1]; }\n" +
                "function test():Any[*]\n" +
                "{\n" +
                " let t = " +
                "#Person" +
                "{ \n" +
                "      >theName[ $this.name ]   " +
                "      >lit[ 'string' ]   " +
                "      >street[ $this.address.street ]   " +
                "}#;" +
                "}\n");
        this.runtime.compile();
        CoreInstance func = this.runtime.getCoreInstance("test__Any_MANY_");
        CoreInstance tree = func.getValueForMetaPropertyToMany("expressionSequence").getFirst().getValueForMetaPropertyToMany("parametersValues").getLast().getValueForMetaPropertyToMany("values").getFirst();

        Assert.assertNotNull(tree);
        ListIterable<? extends CoreInstance> children = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.children, processorSupport).toList();
        Assert.assertEquals("Missing children", 3, children.size());
        CoreInstance derivedProperty = children.getFirst();
        Assert.assertEquals("theName", derivedProperty.getValueForMetaPropertyToOne("name").getName());
        Assert.assertEquals("theName", derivedProperty.getValueForMetaPropertyToOne("propertyName").getName());

        Assert.assertEquals("lit", children.get(1).getValueForMetaPropertyToOne("name").getName());
        final CoreInstance street = children.get(2);
        Assert.assertEquals("street", street.getValueForMetaPropertyToOne("name").getName());
        CoreInstance streetGT = Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToManyResolved(street, M3Properties.specifications, processorSupport).getFirst(), M3Properties.genericType, processorSupport);
        CoreInstance streetRawType = Instance.getValueForMetaPropertyToOneResolved(streetGT, M3Properties.rawType, processorSupport);
        Assert.assertEquals("String", streetRawType.getName());
    }

    @Test
    public void testTreePathWithSameTypeMultipleDefinitions() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", "Class Person{ name: String[1];address:Address[1]; firm: Firm[1]; manager: Person[0..1]; } Class Firm {employees : Person[1];address:Address[1];} Class Address{ street:String[1]; }\n" +
                "function test():Any[*]\n" +
                "{\n" +
                " let t = " +
                "#Person as SP" +
                "{ \n" +
                "      * \n" +
                "      manager as Manager\n " +
                "      {     \n " +
                "         +[name]    \n " +
                "         address as BigHome \n " +
                "      }     \n " +
                "      address as Home \n" +
                "}#;" +
                "}\n");
        this.runtime.compile();
        CoreInstance func = this.runtime.getCoreInstance("test__Any_MANY_");
        CoreInstance tree = func.getValueForMetaPropertyToMany("expressionSequence").getFirst().getValueForMetaPropertyToMany("parametersValues").getLast().getValueForMetaPropertyToMany("values").getFirst();

        Assert.assertNotNull(tree);
        RichIterable<? extends CoreInstance> children = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.children, processorSupport);
        Assert.assertEquals("Missing children", 2, children.size());
        CoreInstance manager = children.getFirst();
        CoreInstance rootAddress = children.getLast();

        Assert.assertEquals("Home", rootAddress.getValueForMetaPropertyToOne("name").getName());
        Assert.assertEquals("address", rootAddress.getValueForMetaPropertyToOne("propertyName").getName());

        Assert.assertEquals("Manager", manager.getValueForMetaPropertyToOne("name").getName());
        Assert.assertEquals("manager", manager.getValueForMetaPropertyToOne("propertyName").getName());

        RichIterable<? extends CoreInstance> managersChildren = Instance.getValueForMetaPropertyToManyResolved(manager, M3Properties.children, processorSupport);
        Assert.assertEquals("Wrong number of children", 1, managersChildren.size());

        CoreInstance address = managersChildren.getFirst();
        Assert.assertEquals("BigHome", address.getValueForMetaPropertyToOne("name").getName());
    }

    @Test
    public void testTreePathWithSameTypeReferenced() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", "Class Person{ name: String[1];address:Address[1]; firm: Firm[1]; manager: Person[0..1]; } Class Firm {employees : Person[1];address:Address[1];} Class Address{ street:String[1]; }\n" +
                "function test():Any[*]\n" +
                "{\n" +
                " let t = " +
                "#Person as SP" +
                "{ \n" +
                "      * \n" +
                "      manager as SP\n " +
                "      address as Home \n" +
                "}#;" +
                "}\n");
        this.runtime.compile();
        CoreInstance func = this.runtime.getCoreInstance("test__Any_MANY_");
        CoreInstance tree = func.getValueForMetaPropertyToMany("expressionSequence").getFirst().getValueForMetaPropertyToMany("parametersValues").getLast().getValueForMetaPropertyToMany("values").getFirst();

        Assert.assertNotNull(tree);
        RichIterable<? extends CoreInstance> children = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.children, processorSupport);
        Assert.assertEquals("Missing children", 2, children.size());
        CoreInstance manager = children.getFirst();
        CoreInstance rootAddress = children.getLast();

        Assert.assertEquals("Home", rootAddress.getValueForMetaPropertyToOne("name").getName());
        Assert.assertEquals("address", rootAddress.getValueForMetaPropertyToOne("propertyName").getName());

        Assert.assertEquals("SP", manager.getValueForMetaPropertyToOne("name").getName());
        Assert.assertEquals("manager", manager.getValueForMetaPropertyToOne("propertyName").getName());

        RichIterable<? extends CoreInstance> managersChildren = Instance.getValueForMetaPropertyToManyResolved(manager, M3Properties.children, processorSupport);
        Assert.assertEquals("Wrong number of children", 2, managersChildren.size());

        CoreInstance managersManager = managersChildren.getFirst();
        Assert.assertEquals("SP", managersManager.getValueForMetaPropertyToOne("name").getName());
        Assert.assertEquals("manager", managersManager.getValueForMetaPropertyToOne("propertyName").getName());

        CoreInstance address = managersChildren.getLast();
        Assert.assertEquals("Home", address.getValueForMetaPropertyToOne("name").getName());

        setUp();
    }

    @Test
    public void testParameters() throws Exception
    {
        this.runtime.createInMemorySource("file.pure",
                "Class Person\n" +
                        "{\n" +
                        "    firstName : String[1];\n" +
                        "    lastName : String[1];\n" +
                        "    nameWithTitle(title:String[1]){$title+' '+$this.firstName+' '+$this.lastName}:String[1];" +
                        "nameWithPrefixAndSuffix(prefix:String[0..1], suffixes:String[*])\n" +
                        "    {\n" +
                        "        if($prefix->isEmpty(),\n" +
                        "           | if($suffixes->isEmpty(),\n" +
                        "                | $this.firstName + ' ' + $this.lastName,\n" +
                        "                | $this.firstName + ' ' + $this.lastName + ', ' + $suffixes->joinStrings(', ')),\n" +
                        "           | if($suffixes->isEmpty(),\n" +
                        "                | $prefix->toOne() + ' ' + $this.firstName + ' ' + $this.lastName,\n" +
                        "                | $prefix->toOne() + ' ' + $this.firstName + ' ' + $this.lastName + ', ' + $suffixes->joinStrings(', ')))\n" +
                        "    }:String[1];" +
                        "}\n");


        try
        {
            this.runtime.createInMemorySource("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    print(#Person {+[nameWithTitle()]}#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException("Error finding match for function 'nameWithTitle'. Incorrect number of parameters, function expects 1 parameters", 3, 22, e);
        }

        try
        {
            this.runtime.modify("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    print(#Person{+[nameWithTitle(Integer[1])]}#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException("Parameter type mismatch for function 'nameWithTitle'. Expected:String, Found:Integer", 3, 21, e);
        }

        this.runtime.modify("function.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    print(#Person{+[nameWithTitle(String[1])]}#,2);\n" +
                        "}\n");
        this.runtime.compile();
    }

    @Test
    public void testMultipleParameters() throws Exception
    {
        this.runtime.createInMemorySource("file.pure",
                "Class Person\n" +
                        "{\n" +
                        "    firstName : String[1];\n" +
                        "    lastName : String[1];\n" +
                        "    nameWithTitle(title:String[1]){$title+' '+$this.firstName+' '+$this.lastName}:String[1];" +
                        "    nameWithPrefixAndSuffix(prefix:String[0..1], suffixes:String[*])\n" +
                        "    {\n" +
                        "        if($prefix->isEmpty(),\n" +
                        "           | if($suffixes->isEmpty(),\n" +
                        "                | $this.firstName + ' ' + $this.lastName,\n" +
                        "                | $this.firstName + ' ' + $this.lastName + ', ' + $suffixes->joinStrings(', ')),\n" +
                        "           | if($suffixes->isEmpty(),\n" +
                        "                | $prefix->toOne() + ' ' + $this.firstName + ' ' + $this.lastName,\n" +
                        "                | $prefix->toOne() + ' ' + $this.firstName + ' ' + $this.lastName + ', ' + $suffixes->joinStrings(', ')))\n" +
                        "    }:String[1];" +
                        "    memberOf(org:Organization[1]){true}:Boolean[1];" +
                        "}\n" +
                        "Class Organization\n" +
                        "{\n" +
                        "}" +
                        "Class Team extends Organization\n" +
                        "{\n" +
                        "}");

        this.runtime.createInMemorySource("function.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "   let t = #Person{-[nameWithPrefixAndSuffix(String[0..1], String[*])]}#;\n" +
                        "}\n");
        this.runtime.compile();
        CoreInstance func = this.runtime.getCoreInstance("test__Any_MANY_");
        CoreInstance tree = func.getValueForMetaPropertyToMany("expressionSequence").getFirst().getValueForMetaPropertyToMany("parametersValues").getLast().getValueForMetaPropertyToMany("values").getFirst();

        Assert.assertNotNull(tree);
        RichIterable<? extends CoreInstance> properties = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.resolvedProperties, processorSupport);
        Assert.assertEquals(4, properties.size());


        this.runtime.modify("function.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    print(#Person{-[nameWithPrefixAndSuffix(String[0..1], String[*])]}#,2);\n" +
                        "}\n");
        this.runtime.compile();

        try
        {
            this.runtime.modify("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    print(#Person{+[nameWithPrefixAndSuffix(String[0..1], Integer[*])]}#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException("Parameter type mismatch for function 'nameWithPrefixAndSuffix'. Expected:String, Found:Integer", 3, 21, e);
        }

        try
        {
            this.runtime.modify("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    print(#Person{-[nameWithPrefixAndSuffix(String[0..1], Any[*])]}#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException("Parameter type mismatch for function 'nameWithPrefixAndSuffix'. Expected:String, Found:Any", 3, 21, e);
        }

        try
        {
            this.runtime.modify("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    print(#Person{+[nameWithPrefixAndSuffix(String[0..1])]}#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException("Error finding match for function 'nameWithPrefixAndSuffix'. Incorrect number of parameters, function expects 2 parameters", 3, 21, e);
        }

        this.runtime.modify("function.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    print(#Person{+[nameWithPrefixAndSuffix(String[0..1], String[*])]}#,2);\n" +
                        "}\n");
        this.runtime.compile();
    }

    @Test
    public void testSimplePropertiesWithStereotypesAndTaggedValues() throws Exception
    {
        this.runtime.createInMemorySource("file.pure",
                "Profile m::p::TestProfile\n" +
                        "{\n" +
                        "   stereotypes : [ Root, NewProp, ExistingProp ];\n" +
                        "   tags : [ Id, Name, Description ];\n" +
                        "}\n" +
                        "Class Person\n" +
                        "{\n" +
                        "    firstName : String[1];\n" +
                        "    lastName : String[1];\n" +
                        "    nameWithTitle(title:String[1]){$title+' '+$this.firstName+' '+$this.lastName}:String[1];" +
                        "    nameWithPrefixAndSuffix(prefix:String[0..1], suffixes:String[*])\n" +
                        "    {\n" +
                        "        if($prefix->isEmpty(),\n" +
                        "           | if($suffixes->isEmpty(),\n" +
                        "                | $this.firstName + ' ' + $this.lastName,\n" +
                        "                | $this.firstName + ' ' + $this.lastName + ', ' + $suffixes->joinStrings(', ')),\n" +
                        "           | if($suffixes->isEmpty(),\n" +
                        "                | $prefix->toOne() + ' ' + $this.firstName + ' ' + $this.lastName,\n" +
                        "                | $prefix->toOne() + ' ' + $this.firstName + ' ' + $this.lastName + ', ' + $suffixes->joinStrings(', ')))\n" +
                        "    }:String[1];" +
                        "    memberOf(org:Organization[1]){true}:Boolean[1];" +
                        "}\n" +
                        "Class Organization\n" +
                        "{\n" +
                        "}" +
                        "Class Team extends Organization\n" +
                        "{\n" +
                        "}");

        this.runtime.createInMemorySource("function.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    let t = #Person <<m::p::TestProfile.Root>> {m::p::TestProfile.Name = 'Stub_Person'} {+[nameWithPrefixAndSuffix(String[0..1], String[*]) <<m::p::TestProfile.ExistingProp>>]}#;\n" +
                        "}\n");
        this.runtime.compile();
        CoreInstance func = this.runtime.getCoreInstance("test__Any_MANY_");
        CoreInstance tree = func.getValueForMetaPropertyToMany("expressionSequence").getFirst().getValueForMetaPropertyToMany("parametersValues").getLast().getValueForMetaPropertyToMany("values").getFirst();

        Assert.assertNotNull(tree);
        this.assertContainsStereoType(tree, "Root");
        this.assertContainsTaggedValue(tree, "Stub_Person");
        this.assertContainsStereoType(tree.getValueForMetaPropertyToMany(M3Properties.included).getFirst(), "ExistingProp");


        this.runtime.modify("function.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    print(#Person{-[nameWithPrefixAndSuffix(String[0..1], String[*])]}#,2);\n" +
                        "}\n");
        this.runtime.compile();

        try
        {
            this.runtime.modify("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    print(#Person{+[nameWithPrefixAndSuffix(String[0..1], Integer[*])]}#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException("Parameter type mismatch for function 'nameWithPrefixAndSuffix'. Expected:String, Found:Integer", 3, 21, e);
        }

        try
        {
            this.runtime.modify("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    print(#Person{-[nameWithPrefixAndSuffix(String[0..1], Any[*])]}#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException("Parameter type mismatch for function 'nameWithPrefixAndSuffix'. Expected:String, Found:Any", 3, 21, e);
        }

        try
        {
            this.runtime.modify("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    print(#Person{+[nameWithPrefixAndSuffix(String[0..1])]}#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException("Error finding match for function 'nameWithPrefixAndSuffix'. Incorrect number of parameters, function expects 2 parameters", 3, 21, e);
        }

        this.runtime.modify("function.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    print(#Person{+[nameWithPrefixAndSuffix(String[0..1], String[*])]}#,2);\n" +
                        "}\n");
        this.runtime.compile();
    }

    @Test
    public void testIncludedPropertiesHaveOwnerNode() throws Exception
    {
        this.runtime.createInMemorySource("file.pure",
                "Profile m::p::TestProfile\n" +
                        "{\n" +
                        "   stereotypes : [ Root, NewProp, ExistingProp ];\n" +
                        "   tags : [ Id, Name, Description ];\n" +
                        "}\n" +
                        "Class Person\n" +
                        "{\n" +
                        "    firstName : String[1];\n" +
                        "    lastName : String[1];\n" +
                        "    nameWithTitle(title:String[1]){$title+' '+$this.firstName+' '+$this.lastName}:String[1];" +
                        "    nameWithPrefixAndSuffix(prefix:String[0..1], suffixes:String[*])\n" +
                        "    {\n" +
                        "        if($prefix->isEmpty(),\n" +
                        "           | if($suffixes->isEmpty(),\n" +
                        "                | $this.firstName + ' ' + $this.lastName,\n" +
                        "                | $this.firstName + ' ' + $this.lastName + ', ' + $suffixes->joinStrings(', ')),\n" +
                        "           | if($suffixes->isEmpty(),\n" +
                        "                | $prefix->toOne() + ' ' + $this.firstName + ' ' + $this.lastName,\n" +
                        "                | $prefix->toOne() + ' ' + $this.firstName + ' ' + $this.lastName + ', ' + $suffixes->joinStrings(', ')))\n" +
                        "    }:String[1];" +
                        "    memberOf(org:Organization[1]){true}:Boolean[1];" +
                        "    employer: Organization[1];" +
                        "}\n" +
                        "Class Organization\n" +
                        "{\n" +
                        "   legalName: String[1];" +
                        "}" +
                        "Class Team extends Organization\n" +
                        "{\n" +
                        "}");

        this.runtime.createInMemorySource("function.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    let t = #Person <<m::p::TestProfile.Root>> {m::p::TestProfile.Name = 'Stub_Person'} " +
                        "{ +[ firstName <<m::p::TestProfile.ExistingProp>> {m::p::TestProfile.Description = 'firstName'} , lastName <<m::p::TestProfile.ExistingProp>> {m::p::TestProfile.Description = 'lastName'} ]\n" +
                        "   employer " +
                        "   {" +
                        "       +[ legalName <<m::p::TestProfile.ExistingProp>> {m::p::TestProfile.Description = 'legalName'} ]" +
                        "   }" +
                        "}#;" +
                        "}\n");
        this.runtime.compile();
        CoreInstance func = this.runtime.getCoreInstance("test__Any_MANY_");
        CoreInstance tree = func.getValueForMetaPropertyToMany("expressionSequence").getFirst().getValueForMetaPropertyToMany("parametersValues").getLast().getValueForMetaPropertyToMany("values").getFirst();

        Assert.assertNotNull(tree);
        this.assertContainsStereoType(tree, "Root");
        this.assertContainsTaggedValue(tree, "Stub_Person");
        ListIterable<? extends CoreInstance> includedProperties = tree.getValueForMetaPropertyToMany(M3Properties.included);
        CoreInstance firstName = includedProperties.getFirst();
        this.assertContainsStereoType(firstName, "ExistingProp");
        this.assertContainsTaggedValue(firstName, "firstName");
        CoreInstance owner = firstName.getValueForMetaPropertyToOne(M3Properties.owner);
        Assert.assertTrue(Instance.instanceOf(owner, M3Paths.RootRouteNode, this.processorSupport));
    }

    @Test
    public void testComplexPropertiesWithStereoTypesAndTaggedValues() throws Exception
    {
        this.runtime.createInMemorySource("file.pure",
                "Profile TestProfile\n" +
                        "{\n" +
                        "   stereotypes : [ Root, NewProp, ExistingProp ];\n" +
                        "   tags : [ Id, Name, Description ];\n" +
                        "}\n" +
                        "Class Person{ name: String[1];address:Address[1]; firm: Firm[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{ street:String[1]; }\n" +
                        "function test():Any[*]\n" +
                        "{\n" +
                        " let t = " +
                        "#Person<<TestProfile.Root>>{TestProfile.Name='Stub_Person'}" +
                        "{ \n" +
                        "             *   " +
                        "             firm <<TestProfile.ExistingProp>>{TestProfile.Name='Stub_Firm'} { employees as Person <<TestProfile.ExistingProp>>{TestProfile.Name='Stub_Person'} }" +
                        "             >myAddress [$this.address] <<TestProfile.ExistingProp>>{TestProfile.Name='MyKingdom'}{ * }" +
                        "}#;" +
                        "}\n");
        this.runtime.compile();
        CoreInstance func = this.runtime.getCoreInstance("test__Any_MANY_");
        CoreInstance tree = func.getValueForMetaPropertyToMany("expressionSequence").getFirst().getValueForMetaPropertyToMany("parametersValues").getLast().getValueForMetaPropertyToMany("values").getFirst();

        Assert.assertNotNull(tree);
        this.assertContainsStereoType(tree, "Root");
        this.assertContainsTaggedValue(tree, "Stub_Person");

        RichIterable<? extends CoreInstance> children = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.children, this.processorSupport);
        Assert.assertEquals("Missing children", 2, children.size());
        for (CoreInstance child : children)
        {
            this.assertContainsStereoType(child, "ExistingProp");
        }
        this.assertContainsTaggedValue(children.getFirst(), "Stub_Firm");
        this.assertContainsTaggedValue(children.getLast(), "MyKingdom");

        RichIterable<? extends CoreInstance> simpleProperties = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.resolvedProperties, processorSupport);
        Assert.assertEquals("Missing simpleProperties", 1, simpleProperties.size());

        CoreInstance firm = children.getFirst();
        RichIterable<? extends CoreInstance> firmChildren = Instance.getValueForMetaPropertyToManyResolved(firm, M3Properties.children, processorSupport);
        Assert.assertEquals("Missing children", 1, firmChildren.size());
        CoreInstance employees = firmChildren.getFirst();
        this.assertContainsStereoType(employees, "ExistingProp");
        this.assertContainsTaggedValue(employees, "Stub_Person");

        CoreInstance address = children.getLast();
        RichIterable<? extends CoreInstance> addressChildren = Instance.getValueForMetaPropertyToManyResolved(address, M3Properties.children, processorSupport);
        Assert.assertEquals(0, addressChildren.size());

        RichIterable<? extends CoreInstance> simpleAddressProperties = Instance.getValueForMetaPropertyToManyResolved(address, M3Properties.resolvedProperties, processorSupport);
        Assert.assertEquals("Missing simpleProperties", 1, simpleAddressProperties.size());

        setUp();
    }

    private void assertContainsTaggedValue(CoreInstance element, String tag)
    {
        RichIterable<? extends CoreInstance> taggedValues = Instance.getValueForMetaPropertyToManyResolved(element, M3Properties.taggedValues, processorSupport);
        Assert.assertTrue("Missing Tagged Values", taggedValues.size() > 0);
        CoreInstance tv = taggedValues.getFirst();
        Assert.assertEquals(tag, tv.getValueForMetaPropertyToOne(M3Properties.value).getName());
    }

    private void assertContainsStereoType(CoreInstance element, String stereoType)
    {
        RichIterable<? extends CoreInstance> stereoTypes = Instance.getValueForMetaPropertyToManyResolved(element, M3Properties.stereotypes, processorSupport);
        Assert.assertTrue("Missing Stereotypes", stereoTypes.size() > 0);
        Assert.assertEquals(stereoType, stereoTypes.getFirst().getValueForMetaPropertyToOne(M3Properties.value).getName());
    }
}
