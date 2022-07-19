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

package org.finos.legend.pure.m2.ds.mapping.test;

import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelToModel.PureInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelToModel.PurePropertyMapping;
import org.finos.legend.pure.m3.tools.test.ToFix;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.*;

public class TestModelMapping extends AbstractPureMappingTestWithCoreCompiled
{
    GraphWalker graphWalker;
    @BeforeClass
    public static void setUp() {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("mapping.pure");
        runtime.delete("model.pure");
        runtime.delete("projection.pure");
    }

    @Before
    public void setUpRelational()
    {
        this.graphWalker = new GraphWalker(this.runtime, this.processorSupport);
    }

    @Test
    public void testLiteralMapping()
    {
        this.runtime.createInMemorySource("model.pure",
                        "Enum myEnum" +
                        "{" +
                        "   a,b" +
                        "}" +
                        "Class Firm" +
                        "{" +
                        "  legalName : String[1];" +
                        "  count : Integer[1];" +
                        "  flag : Boolean[1];" +
                        "  date : Date[1];" +
                        "  f_val :Float[1];" +
                        "  enumVal : myEnum[1];" +
                        "}");

        String source = "###Mapping\n" +
                "Mapping test::TestMapping\n" +
                "(\n" +
                "  Firm : Pure\n" +
                "  {\n" +
                "    legalName : 'name',\n" +
                "    count : 2," +
                "    flag : true," +
                "    f_val : 1.0," +
                "    date : %2005-10-10," +
                "    enumVal : myEnum.a\n" +
                "  }\n" +
                ")";
        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.compile();
        assertSetSourceInformation(source, "Firm");
    }

    @Test
    public void testLiteralEnumErrorMapping()
    {
        this.runtime.createInMemorySource("model.pure",
                "Enum myEnum" +
                        "{" +
                        "   a,b" +
                        "}" +
                        "Class Firm" +
                        "{" +
                        "  enumVal : myEnum[1];" +
                        "}");

        this.runtime.createInMemorySource("mapping.pure",
                "###Mapping\n" +
                        "Mapping test::TestMapping\n" +
                        "(\n" +
                        "  Firm : Pure\n" +
                        "  {\n" +
                        "    enumVal : myEnum.z\n" +
                        "  }\n" +
                        ")");
        try
        {
            this.runtime.compile();
            Assert.fail("Expected compilation exception");
        }
        catch (PureCompilationException e)
        {
                this.assertPureException(PureCompilationException.class,
                        "The enum value 'z' can't be found in the enumeration myEnum",
                        "mapping.pure", 6, 22, 6, 22, 6, 22, e);

        }
    }

    @Test
    public void testCompileFailureWithMappingTests()
    {
        try
        {
            this.runtime.createInMemorySource("model.pure",
                    "###Mapping\n" +
                            "Mapping my::query::TestMapping\n" +
                            "(\n" +
                            "MappingTests\n" +
                            "   [\n" +
                            "      test1\n" +
                            "      (\n" +
                            "         ~query: {x:ui::ClassA|$x.propA},\n" +
                            "         ~inputData: [],\n" +
                            "         ~assert: 'assertString'\n" +
                            "      ),\n" +
                            "      defaultTest\n" +
                            "      (\n" +
                            "         ~query: {|model::domain::Target.all()->graphFetchChecked(#{ClassNotHere{name}}#)->serialize(#{model::domain::Target{name}}#)},\n" +
                            "         ~inputData: \n" +
                            "           [" +
                            "           <Object,model::domain::Source, '{\"oneName\":\"oneName 2\",\"anotherName\":\"anotherName 16\",\"oneDate\":\"2020-02-05\",\"anotherDate\":\"2020-04-13\",\"oneNumber\":24,\"anotherNumber\":29}'>," +
                            "           <Object,SourceClass, '{\"oneName\":\"oneName 2\",\"anotherName\":\"anotherName 16\",\"oneDate\":\"2020-02-05\",\"anotherDate\":\"2020-04-13\",\"oneNumber\":24,\"anotherNumber\":29}'>" +
                            "           ]," +
                            "         ~assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 2\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 2\"},\"source\":{\"number\":1,\"record\":\"{\"oneName\":\"oneName 2\",\"anotherName\":\"anotherName 16\",\"oneDate\":\"2020-02-05\",\"anotherDate\":\"2020-04-13\",\"oneNumber\":24,\"anotherNumber\":29}\"}}}'\n" +
                            "      )\n" +
                            "   ]" +
                            ")");
            this.runtime.compile();
            Assert.fail("Expected compilation exception");
        }
        catch (PureParserException e)
        {
            this.assertPureException(PureParserException.class,
                    "Grammar Tests in Mapping currently not supported in Pure",
                    "model.pure", 4, 1, 4, 1, 4, 12, e);
        }
    }

    @Test
    public void testMappingWithSource()
    {
        this.runtime.createInMemorySource("model.pure",
                        "Class Firm" +
                        "{" +
                        "  legalName : String[1];" +
                        "}" +
                        "" +
                        "Class pack::FirmSource" +
                        "{" +
                        "   name : String[1];" +
                        "}");

        String source = "###Mapping\n" +
                "Mapping test::TestMapping\n" +
                "(\n" +
                "  Firm : Pure\n" +
                "  {" +
                "    ~src pack::FirmSource\n" +
                "    legalName : $src.name\n" +
                "  }\n" +
                ")";
        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.compile();
        assertSetSourceInformation(source, "Firm");
    }

    @Test
    public void testMappingWithSourceInt()
    {
        this.runtime.createInMemorySource("model.pure",
                "Class Firm" +
                        "{" +
                        "  legalName : String[1];" +
                        "  val : Integer[1];" +
                        "}" +
                        "" +
                        "Class AB" +
                        "{" +
                        "   vale : Integer[1];" +
                        "}");

        String source = "###Mapping\n" +
                "Mapping test::TestMapping\n" +
                "(\n" +
                "  Firm : Pure\n" +
                "  {" +
                "    ~src AB\n" +
                "    legalName : ['a','b']->map(k|$k+'Yeah!')->joinStrings(','),\n" +
                "    val : $src.vale\n" +
                "  }\n" +
                ")";
        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.compile();
        assertSetSourceInformation(source, "Firm");
    }

    @Test
    public void testProjectionClassMapping()
    {
        this.runtime.createInMemorySource("model.pure",
                "Enum myEnum" +
                        "{" +
                        "   a,b" +
                        "}" +
                        "Class Firm" +
                        "{" +
                        "  legalName : String[1];" +
                        "  count : Integer[1];" +
                        "  flag : Boolean[1];" +
                        "  date : Date[1];" +
                        "  f_val :Float[1];" +
                        "  enumVal : myEnum[1];" +
                        "}" +
                        "");

        String source = "###Mapping\n" +
                "Mapping test::TestMapping\n" +
                "(\n" +
                "  FirmProjection : Pure\n" +
                "  {\n" +
                "    legalName : 'name',\n" +
                "    count : 2," +
                "    flag : true," +
                "    f_val : 1.0," +
                "    date : %2005-10-10," +
                "    enumVal : myEnum.a\n" +
                "  }\n" +
                ")";
        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.createInMemorySource("projection.pure", "Class FirmProjection projects Firm\n" +
                "{\n" +
                "   *" +
                "}");
        this.runtime.compile();
        assertSetSourceInformation(source, "FirmProjection");
    }

    @Test
    public void testMappingWithSourceWrongProperty()
    {
        this.runtime.createInMemorySource("model.pure",
                "Class Firm" +
                        "{" +
                        "  legalName : String[1];" +
                        "}" +
                        "" +
                        "Class pack::FirmSource" +
                        "{" +
                        "   name : String[1];" +
                        "}");

        this.runtime.createInMemorySource("mapping.pure",
                "###Mapping\n" +
                        "Mapping test::TestMapping\n" +
                        "(\n" +
                        "  Firm : Pure\n" +
                        "  {" +
                        "    ~src pack::FirmSource\n" +
                        "    legalName : $src.nameX\n" +
                        "  }\n" +
                        ")");
        try
        {
            this.runtime.compile();
            Assert.fail("Expected compilation exception");
        }
        catch (PureCompilationException e)
        {
            this.assertPureException(PureCompilationException.class,
                    "Can't find the property 'nameX' in the class pack::FirmSource",
                    "mapping.pure", 6, 22, 6, 22, 6, 26, e);

        }
    }

    @Test
    public void testMappingWithSourceError()
    {
        this.runtime.createInMemorySource("model.pure",
                        "Class Firm" +
                        "{" +
                        "  legalName : String[1];" +
                        "}");

        this.runtime.createInMemorySource("mapping.pure",
                "###Mapping\n" +
                        "Mapping test::TestMapping\n" +
                        "(\n" +
                        "  Firm : Pure\n" +
                        "  {" +
                        "    ~src pack::FirmSource\n" +
                        "    legalName : 'name'\n" +
                        "  }\n" +
                        ")");
        try
        {
            this.runtime.compile();
            Assert.fail("Expected compilation exception");
        }
        catch (PureCompilationException e)
        {
            this.assertPureException(PureCompilationException.class,
                    "pack::FirmSource has not been defined!",
                    "mapping.pure", 5, 10, 5, 10, 5, 13, e);

        }
    }

    @Test
    public void testMappingWithTypeMismatch()
    {
        this.runtime.createInMemorySource("model.pure",
                "Class Firm" +
                        "{" +
                        "  legalName : String[1];" +
                        "}");

        this.runtime.createInMemorySource("mapping.pure",
                "###Mapping\n" +
                        "Mapping test::TestMapping\n" +
                        "(\n" +
                        "  Firm : Pure\n" +
                        "  {" +
                        "    legalName : 1\n" +
                        "  }\n" +
                        ")");
        try
        {
            this.runtime.compile();
            Assert.fail("Expected compilation exception");
        }
        catch (PureCompilationException e)
        {
            this.assertPureException(PureCompilationException.class,
                    "Type Error: 'Integer' not a subtype of 'String'",
                    "mapping.pure", 5, 17, 5, 17, 5, 17, e);

        }
    }


    @Test
    public void testMappingWithMultiplicityMismatch()
    {
        this.runtime.createInMemorySource("model.pure",
                "Class Firm" +
                        "{" +
                        "  legalName : String[1];" +
                        "}");

        this.runtime.createInMemorySource("mapping.pure",
                "###Mapping\n" +
                        "Mapping test::TestMapping\n" +
                        "(\n" +
                        "  Firm : Pure\n" +
                        "  {" +
                        "    legalName : ['a','b']\n" +
                        "  }\n" +
                        ")");
        try
        {
            this.runtime.compile();
            Assert.fail("Expected compilation exception");
        }
        catch (PureCompilationException e)
        {
            this.assertPureException(PureCompilationException.class,
                    "Multiplicity Error ' The property 'legalName' has a multiplicity range of [1] when the given expression has a multiplicity range of [2]",
                    "mapping.pure", 5, 17, 5, 17, 5, 25, e);

        }
    }


    @Test
    public void testFilter()
    {
        this.runtime.createInMemorySource("model.pure",
                        "Class Firm" +
                        "{" +
                        "  legalName : String[1];" +
                        "}" +
                        "Class FirmSource" +
                        "{" +
                        "   val : String[1];" +
                        "}");

        String source = "###Mapping\n" +
                "Mapping test::TestMapping\n" +
                "(\n" +
                "  Firm[firm] : Pure\n" +
                "  {\n" +
                "    ~src FirmSource\n" +
                "    ~filter $src.val == 'ok'\n" +
                "    legalName : $src.val\n" +
                "  }\n" +
                ")";
        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.compile();
        assertSetSourceInformation(source, "Firm");
        CoreInstance mapping = this.graphWalker.getMapping("test::TestMapping");
        Assert.assertNotNull(mapping);
        Assert.assertNotNull(mapping.getSourceInformation());
        Assert.assertEquals(2,mapping.getSourceInformation().getStartLine());
        Assert.assertEquals(10,mapping.getSourceInformation().getEndLine());
        CoreInstance classMapping = this.graphWalker.getClassMappingById(mapping,"firm");
        Assert.assertNotNull(classMapping);
        Assert.assertNotNull(classMapping.getSourceInformation());
        Assert.assertEquals(4,classMapping.getSourceInformation().getStartLine());
        Assert.assertEquals(9,classMapping.getSourceInformation().getEndLine());
    }

    @Test
    public void testFilterError()
    {
        this.runtime.createInMemorySource("model.pure",
                "Class Firm" +
                        "{" +
                        "  legalName : String[1];" +
                        "}" +
                        "Class FirmSource" +
                        "{" +
                        "   val : String[1];" +
                        "}");

        this.runtime.createInMemorySource("mapping.pure",
                "###Mapping\n" +
                        "Mapping test::TestMapping\n" +
                        "(\n" +
                        "  Firm : Pure\n" +
                        "  {" +
                        "    ~src FirmSource\n" +
                        "    ~filter $src.valX == 'ok'" +
                        "    legalName : $src.val\n" +
                        "  }\n" +
                        ")");
        try
        {
            this.runtime.compile();
            Assert.fail("Expected a compilation error");
        }
        catch (PureCompilationException e)
        {
            this.assertPureException(PureCompilationException.class,
                    "Can't find the property 'valX' in the class FirmSource",
                    "mapping.pure", 6, 18, 6, 18, 6, 21, e);

        }
    }


    @Test
    public void testFilterTypeError()
    {
        this.runtime.createInMemorySource("model.pure",
                "Class Firm" +
                        "{" +
                        "  legalName : String[1];" +
                        "}" +
                        "Class FirmSource" +
                        "{" +
                        "   val : String[1];" +
                        "}");

        this.runtime.createInMemorySource("mapping.pure",
                "###Mapping\n" +
                        "Mapping test::TestMapping\n" +
                        "(\n" +
                        "  Firm : Pure\n" +
                        "  {" +
                        "    ~src FirmSource\n" +
                        "    ~filter $src.val" +
                        "    legalName : $src.val\n" +
                        "  }\n" +
                        ")");
        try
        {
            this.runtime.compile();
            Assert.fail("Expected a compilation error");
        }
        catch (PureCompilationException e)
        {
            this.assertPureException(PureCompilationException.class,
                    "A filter should be a Boolean expression",
                    "mapping.pure", 6, 18, 6, 18, 6, 20, e);

        }
    }

    @Test
    public void testComplexTypePropertyMapping()
    {
        this.runtime.createInMemorySource("model.pure",
                "Class Person" +
                        "{" +
                        "   firms : Firm[*];" +
                        "}" +
                        "Class Firm" +
                        "{" +
                        "  legalName : String[1];" +
                        "}"+
                        "Class _Person" +
                        "{" +
                        "   firms : _Firm[*];" +
                        "}" +
                        "Class _Firm" +
                        "{" +
                        "  legalName : String[1];" +
                        "}");

        String source = "###Mapping\n" +
                "Mapping test::TestMapping\n" +
                "(\n" +
                "  Firm : Pure\n" +
                "  {\n" +
                "    ~src _Firm\n" +
                "    legalName : $src.legalName" +
                "  }\n" +
                "  Person : Pure\n" +
                "  {\n" +
                "    ~src _Person\n" +
                "    firms : $src.firms" +
                "  }\n" +
                ")";
        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.compile();
        assertSetSourceInformation(source, "Firm");
    }

    @Test
    public void testComplexTypePropertyMappingError()
    {
        this.runtime.createInMemorySource("model.pure",
                "Class Person" +
                        "{" +
                        "   firms : Firm[*];" +
                        "}" +
                        "Class Firm" +
                        "{" +
                        "  legalName : String[1];" +
                        "}"+
                        "Class _Person" +
                        "{" +
                        "   name : String[1];" +
                        "   firms : _Firm[*];" +
                        "}" +
                        "Class _Firm" +
                        "{" +
                        "  legalName : String[1];" +
                        "}");

        this.runtime.createInMemorySource("mapping.pure",
                "###Mapping\n" +
                        "Mapping test::TestMapping\n" +
                        "(\n" +
                        "  Firm : Pure\n" +
                        "  {\n" +
                        "    ~src _Person" +
                        "    legalName  : $src.name\n" +
                        "  }\n" +
                        "  Person : Pure\n" +
                        "  {\n" +
                        "    ~src _Person\n" +
                        "    firms : $src.firms" +
                        "  }\n" +
                        ")");
        try
        {
            this.runtime.compile();
            Assert.fail("Expected a compilation error");
        }
        catch (PureCompilationException e)
        {
            this.assertPureException(PureCompilationException.class,
                    "Type Error: '_Person' is not '_Firm'",
                    "mapping.pure", 11, 18, 11, 18, 11, 22, e);

        }
    }

    @Test
    public void testComplexTypePropertyMappingWithWrongTargetIdError()
    {
        this.runtime.createInMemorySource("model.pure",
                "Class Person" +
                        "{" +
                        "   firms : Firm[*];" +
                        "}" +
                        "Class Firm" +
                        "{" +
                        "  legalName : String[1];" +
                        "}"+
                        "Class _Person" +
                        "{" +
                        "   name : String[1];" +
                        "   firms : _Firm[*];" +
                        "}" +
                        "Class _Firm" +
                        "{" +
                        "  legalName : String[1];" +
                        "}");

        this.runtime.createInMemorySource("mapping.pure",
                "###Mapping\n" +
                        "Mapping test::TestMapping\n" +
                        "(\n" +
                        "  Firm : Pure\n" +
                        "  {\n" +
                        "    ~src _Person" +
                        "    legalName  : $src.name\n" +
                        "  }\n" +
                        "  Person : Pure\n" +
                        "  {\n" +
                        "    ~src _Person\n" +
                        "    firms[f2] : $src.firms" +
                        "  }\n" +
                        ")");
        try
        {
            this.runtime.compile();
            Assert.fail("Expected a compilation error");
        }
        catch (PureCompilationException e)
        {
            this.assertPureException(PureCompilationException.class,
                    "The set implementation 'f2' is unknown in the mapping 'TestMapping'",
                    "mapping.pure", 11, 5, 11, 5, 11, 9, e);

        }
    }

    @Test
    public void testMilestonedMappingWithLatestDate()
    {
        this.runtime.createInMemorySource("model.pure",
                        "Class Firm\n" +
                        "{\n" +
                        "  legalName : String[1];\n" +
                        "  employees : Person[*];\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> Person\n" +
                        "{\n" +
                        "  name : String[1];\n" +
                        "}\n" +
                        "Class TargetFirm\n" +
                        "{\n" +
                        "  legalName : String[1];\n" +
                        "  employeeNames : String[*];\n" +
                        "}\n");

        String source = "###Mapping\n" +
                "Mapping test::TestMapping\n" +
                "(\n" +
                "  TargetFirm : Pure\n" +
                "  {\n" +
                "    ~src Firm" +
                "    legalName : $src.legalName,\n" +
                "    employeeNames : $src.employees(%latest)->map(e | $e.name)\n" +
                "  }\n" +
                ")";
        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.compile();
        assertSetSourceInformation(source, "TargetFirm");
    }

    @Test
    public void testM2MMappingWithEnumerationMapping()
    {
        this.runtime.createInMemorySource("model.pure",
                "###Pure\n" +
                        "import my::*;\n" +
                        "\n" +
                        "Class my::SourceProduct\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "   state : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class my::TargetProduct\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "   state : State[1];\n" +
                        "}\n" +
                        "\n" +
                        "Enum my::State\n" +
                        "{\n" +
                        "   ACTIVE,\n" +
                        "   INACTIVE\n" +
                        "}"
        );

        this.runtime.createInMemorySource("mapping.pure",
                "###Mapping\n" +
                        "import my::*;\n" +
                        "\n" +
                        "Mapping my::modelMapping\n" +
                        "(\n" +
                        "   TargetProduct : Pure\n" +
                        "   {\n" +
                        "      ~src SourceProduct\n" +
                        "      id : $src.id,\n" +
                        "      state : EnumerationMapping StateMapping : $src.state\n" +
                        "   }\n" +
                        "   \n" +
                        "   State : EnumerationMapping StateMapping\n" +
                        "   {\n" +
                        "      ACTIVE : 'ACTIVE',\n" +
                        "      INACTIVE : 'INACTIVE'\n" +
                        "   }\n" +
                        ")"
        );

        try
        {
            this.runtime.compile();
            Mapping mapping = (Mapping)this.runtime.getCoreInstance("my::modelMapping");
            PureInstanceSetImplementation m2mMapping = mapping._classMappings().selectInstancesOf(PureInstanceSetImplementation.class).getFirst();

            PurePropertyMapping purePropertyMapping1 = m2mMapping._propertyMappings().selectInstancesOf(PurePropertyMapping.class).getFirst();
            Assert.assertNull(purePropertyMapping1._transformer());

            PurePropertyMapping purePropertyMapping2 = m2mMapping._propertyMappings().selectInstancesOf(PurePropertyMapping.class).getLast();
            Assert.assertNotNull(purePropertyMapping2._transformer());
            Assert.assertTrue(purePropertyMapping2._transformer() instanceof EnumerationMapping);

            EnumerationMapping transformer = (EnumerationMapping)purePropertyMapping2._transformer();
            Assert.assertEquals("StateMapping", transformer._name());
            Assert.assertEquals("my::State", PackageableElement.getUserPathForPackageableElement(transformer._enumeration()));
            Assert.assertEquals(2, transformer._enumValueMappings().size());
        }
        catch (Exception e)
        {
            Assert.fail();
        }
    }

    @Test
    public void testM2MMappingWithInvalidEnumerationMapping()
    {
        this.runtime.createInMemorySource("model.pure",
                "###Pure\n" +
                        "import my::*;\n" +
                        "\n" +
                        "Class my::SourceProduct\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "   state : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class my::TargetProduct\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "   state : State[1];\n" +
                        "}\n" +
                        "\n" +
                        "Enum my::State\n" +
                        "{\n" +
                        "   ACTIVE,\n" +
                        "   INACTIVE\n" +
                        "}\n" +
                        "\n" +
                        "Enum my::Option\n" +
                        "{\n" +
                        "   CALL,\n" +
                        "   PUT\n" +
                        "}\n"
        );

        this.runtime.createInMemorySource("mapping.pure",
                "###Mapping\n" +
                        "import my::*;\n" +
                        "\n" +
                        "Mapping my::modelMapping\n" +
                        "(\n" +
                        "   TargetProduct : Pure\n" +
                        "   {\n" +
                        "      ~src SourceProduct\n" +
                        "      id : $src.id,\n" +
                        "      state : EnumerationMapping OptionMapping : $src.state\n" +
                        "   }\n" +
                        "   \n" +
                        "   Option : EnumerationMapping OptionMapping\n" +
                        "   {\n" +
                        "      CALL : 'ACTIVE',\n" +
                        "      PUT : 'INACTIVE'\n" +
                        "   }\n" +
                        ")"
        );

        try
        {
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Property : [state] is of type : [my::State] but enumeration mapping : [OptionMapping] is defined on enumeration : [my::Option].", "mapping.pure", 10, 7, e);
        }
    }

    @Test
    @Ignore
    @ToFix
    public void testMissingRequiredPropertyError()
    {
        compileTestSource("/test/model.pure",
                "Class test::SourceClass\n" +
                        "{\n" +
                        "    prop1 : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::TargetClass\n" +
                        "{\n" +
                        "    prop2 : String[1];\n" +
                        "    prop3 : Integer[1];\n" +
                        "}\n");
        try
        {
            compileTestSource("/test/mapping.pure",
                    "###Mapping\n" +
                            "import test::*;\n" +
                            "Mapping test::TestMapping\n" +
                            "(\n" +
                            "    TargetClass : Pure\n" +
                            "    {\n" +
                            "        ~src SourceClass\n" +
                            "        prop2 : $src.prop1\n" +
                            "    }\n" +
                            ")\n");
            Assert.fail("Expected a compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The following required properties for test::TargetClass are not mapped: prop3", "/test/mapping.pure", 5, 5, 5, 5, 5, 15, e);
        }
    }

    @Test
    public void testMappingWithMerge()
    {
    String  source =
               "Class  example::SourcePersonWithFirstName\n" +
                "{\n" +
                "   id:Integer[1];\n" +
                "   firstName:String[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "Class example::SourcePersonWithLastName\n" +
                "{\n" +
                "   id:Integer[1];\n" +
                "   lastName:String[1];\n" +
                "}\n" +
              "Class example::Person\n" +
                "{\n" +
                "   firstName:String[1];\n" +
                "   lastName:String[1];\n" +
                "}\n" +

                "\n" +
              "function meta::pure::router::operations::merge(o:meta::pure::mapping::OperationSetImplementation[1]):meta::pure::mapping::SetImplementation[*] {[]}\n" +

                "###Mapping\n" +
                "Mapping  example::MergeModelMappingSourceWithMatch\n" +
                "(\n" +
                "   *example::Person : Operation\n" +
                "           {\n" +
          "             meta::pure::router::operations::merge_OperationSetImplementation_1__SetImplementation_MANY_([p1,p2,p3],{p1:example::SourcePersonWithFirstName[1], p2:example::SourcePersonWithLastName[1],p4:example::SourcePersonWithLastName[1] | $p1.id ==  $p2.id })\n" +

                "           }\n" +
                "\n" +
                "   example::Person[p1] : Pure\n" +
                "            {\n" +
                "               ~src example::SourcePersonWithFirstName\n" +
                "               firstName : $src.firstName\n" +
                "            }\n" +
                "\n" +
                "   example::Person[p2] : Pure\n" +
                "            {\n" +
                "               ~src example::SourcePersonWithLastName\n" +
                       "        lastName :  $src.lastName\n" +
                "            }\n" +
                "   example::Person[p3] : Pure\n" +
                "            {\n" +
                "               ~src example::SourcePersonWithLastName\n" +
                "        lastName :  $src.lastName\n" +
                "            }\n" +

                "\n" +
                ")";


        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.compile();
    }

 @Test
    public void testMappingWithMergeInvalidReturn()
    {
    String  source =
               "Class  example::SourcePersonWithFirstName\n" +
                "{\n" +
                "   id:Integer[1];\n" +
                "   firstName:String[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "Class example::SourcePersonWithLastName\n" +
                "{\n" +
                "   id:Integer[1];\n" +
                "   lastName:String[1];\n" +
                "}\n" +
              "Class example::Person\n" +
                "{\n" +
                "   firstName:String[1];\n" +
                "   lastName:String[1];\n" +
                "}\n" +

                "\n" +
              "function meta::pure::router::operations::merge(o:meta::pure::mapping::OperationSetImplementation[1]):meta::pure::mapping::SetImplementation[*] {[]}\n" +

                "###Mapping\n" +
                "Mapping  example::MergeModelMappingSourceWithMatch\n" +
                "(\n" +
                "   *example::Person : Operation\n" +
                "           {\n" +
          "             meta::pure::router::operations::merge_OperationSetImplementation_1__SetImplementation_MANY_([p1,p2,p3],{p1:example::SourcePersonWithFirstName[1], p2:example::SourcePersonWithLastName[1],p4:example::SourcePersonWithLastName[1] |  'test' })\n" +

                "           }\n" +
                "\n" +
                "   example::Person[p1] : Pure\n" +
                "            {\n" +
                "               ~src example::SourcePersonWithFirstName\n" +
                "               firstName : $src.firstName\n" +
                "            }\n" +
                "\n" +
                "   example::Person[p2] : Pure\n" +
                "            {\n" +
                "               ~src example::SourcePersonWithLastName\n" +
                       "        lastName :  $src.lastName\n" +
                "            }\n" +
                "   example::Person[p3] : Pure\n" +
                "            {\n" +
                "               ~src example::SourcePersonWithLastName\n" +
                "        lastName :  $src.lastName\n" +
                "            }\n" +

                "\n" +
                ")";



        this.runtime.createInMemorySource("mapping.pure", source);

        try
        {
            this.runtime.compile();
            Assert.fail("Expected compilation exception");
        }
        catch (PureCompilationException e)
        {
                this.assertPureException(PureCompilationException.class,
                        "Merge validation function for class: Person does not return Boolean",
                        "mapping.pure", 23, 5, 23, 14, 26, 12, e);

        }
    }

     @Test
    public void testMappingWithMergeInvalidParameter()
    {
    String  source =
               "Class  example::SourcePersonWithFirstName\n" +
                "{\n" +
                "   id:Integer[1];\n" +
                "   firstName:String[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "Class example::SourcePersonWithLastName\n" +
                "{\n" +
                "   id:Integer[1];\n" +
                "   lastName:String[1];\n" +
                "}\n" +
              "Class example::Person\n" +
                "{\n" +
                "   id:Integer[1];\n" +
                "   firstName:String[1];\n" +
                "   lastName:String[1];\n" +
                "}\n" +

                "\n" +
              "function meta::pure::router::operations::merge(o:meta::pure::mapping::OperationSetImplementation[1]):meta::pure::mapping::SetImplementation[*] {[]}\n" +

                "###Mapping\n" +
                "Mapping  example::MergeModelMappingSourceWithMatch\n" +
                "(\n" +
                "   *example::Person : Operation\n" +
                "           {\n" +
          "             meta::pure::router::operations::merge_OperationSetImplementation_1__SetImplementation_MANY_([p1,p2,p3],{p1:example::Person[1], p2:example::SourcePersonWithLastName[1],p4:example::SourcePersonWithLastName[1] | $p1.id ==  $p2.id })\n" +

                "           }\n" +
                "\n" +
                "   example::Person[p1] : Pure\n" +
                "            {\n" +
                "               ~src example::SourcePersonWithFirstName\n" +
                "               firstName : $src.firstName\n" +
                "            }\n" +
                "\n" +
                "   example::Person[p2] : Pure\n" +
                "            {\n" +
                "               ~src example::SourcePersonWithLastName\n" +
                       "        lastName :  $src.lastName\n" +
                "            }\n" +
                "   example::Person[p3] : Pure\n" +
                "            {\n" +
                "               ~src example::SourcePersonWithLastName\n" +
                "        lastName :  $src.lastName\n" +
                "            }\n" +

                "\n" +
                ")";



        this.runtime.createInMemorySource("mapping.pure", source);

        try
        {
            this.runtime.compile();
            Assert.fail("Expected compilation exception");
        }
        catch (PureCompilationException e)
        {
                this.assertPureException(PureCompilationException.class,
                        "Merge validation function for class: Person has an invalid parameter. All parameters must be a src class of a merged set",
                        "mapping.pure", 24, 5, 24, 14, 27, 12, e);

        }
    }

}
