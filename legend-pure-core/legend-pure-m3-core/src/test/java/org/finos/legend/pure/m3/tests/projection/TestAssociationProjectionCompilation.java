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

package org.finos.legend.pure.m3.tests.projection;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.AssociationProjection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassProjection;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestAssociationProjectionCompilation extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("file.pure");
        runtime.compile();
    }

    @Test
    public void testTargetNotProjection()
    {
        runtime.createInMemorySource("file.pure",
                "Class a::b::Person\n" +
                        "{\n" +
                        "   name: String[1];\n" +
                        "   yearsEmployed : Integer[1];\n" +
                        "}\n" +
                        "Class a::b::Address\n" +
                        "{\n" +
                        "   street:String[1];\n" +
                        "}\n" +
                        "Class a::b::PersonProjection projects #a::b::Person\n" +
                        "{ \n" +
                        "   *\n" +
                        "}#\n" +
                        "Class a::b::AddressProjection projects #a::b::Address\n" +
                        "{\n" +
                        "   *\n" +
                        "}#\n" +
                        "Association a::b::PerAddProjection projects a::b::PersonAddress<a::b::PersonProjection, a::b::Address>\n" +
                        "Association a::b::PersonAddress \n" +
                        "{\n" +
                        "   person:  a::b::Person[1];\n" +
                        "   address: a::b::Address[*];\n" +
                        "}\n");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "AssociationProjection 'a::b::PerAddProjection' can only be applied to ClassProjections; 'a::b::Address' is not a ClassProjection", "file.pure", 18, 19, e);
    }

    @Test
    public void testMissingProperty1()
    {
        runtime.createInMemorySource("file.pure",
                "Class a::b::Person\n" +
                        "{\n" +
                        "   name: String[1];\n" +
                        "   yearsEmployed : Integer[1];\n" +
                        "}\n" +
                        "Class a::b::Address\n" +
                        "{\n" +
                        "   street:String[1];\n" +
                        "}\n" +
                        "Class a::b::PersonProjection projects #a::b::Person\n" +
                        "{\n" +
                        "   *\n" +
                        "}#\n" +
                        "Class a::b::AddressProjection projects #a::b::Address\n" +
                        "{\n" +
                        "   *\n" +
                        "}#\n" +
                        "Association a::b::PerAddProjection projects a::b::PersonAddress<a::b::PersonProjection, a::b::AddressProjection>\n" +
                        "Association a::b::PersonAddress \n" +
                        "{\n" +
                        "   person:  a::b::PersonProjection[1];\n" +
                        "   address: a::b::Address[*];\n" +
                        "}\n");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "Invalid AssociationProjection 'a::b::PerAddProjection'. Projection for property 'person' is not specified.", "file.pure", 18, 19, e);
    }

    @Test
    public void testMissingProperty2()
    {
        runtime.createInMemorySource("file.pure",
                "Class a::b::Person\n" +
                        "{\n" +
                        "   name: String[1];\n" +
                        "   yearsEmployed : Integer[1];\n" +
                        "}\n" +
                        "Class a::b::Address\n" +
                        "{\n" +
                        "   street:String[1];\n" +
                        "}\n" +
                        "Class a::b::Random\n" +
                        "{\n" +
                        "   arbit:String[1];\n" +
                        "}\n" +
                        "Class a::b::RandomProjection projects # a::b::Random\n" +
                        "{\n" +
                        "   *\n" +
                        "}#\n" +
                        "Class a::b::PersonProjection projects #a::b::Person\n" +
                        "{\n" +
                        "   *\n" +
                        "}#\n" +
                        "Class a::b::AddressProjection projects #a::b::Address\n" +
                        "{ \n" +
                        "   *\n" +
                        "}#\n" +
                        "Association a::b::PerAddProjection projects a::b::PersonAddress<a::b::PersonProjection, a::b::RandomProjection>\n" +
                        "Association a::b::PersonAddress\n" +
                        "{\n" +
                        "   person:  a::b::Person[1];\n" +
                        "   address: a::b::Address[*];\n" +
                        "}\n");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "Invalid AssociationProjection 'a::b::PerAddProjection'. Projection for property 'address' is not specified.", "file.pure", 26, 19, e);
    }

    @Test
    public void testMissingProperty3()
    {
        runtime.createInMemorySource("file.pure",
                "Class a::b::Person\n" +
                        "{\n" +
                        "   name: String[1];\n" +
                        "   yearsEmployed : Integer[1];\n" +
                        "}\n" +
                        "Class a::b::Address\n" +
                        "{\n" +
                        "   street:String[1];\n" +
                        "}\n" +
                        "Class a::b::Random\n" +
                        "{\n" +
                        "   arbit:String[1];\n" +
                        "}\n" +
                        "Class a::b::RandomProjection projects # a::b::Random\n" +
                        "{\n" +
                        "   *\n" +
                        "}#\n" +
                        "Class a::b::PersonProjection projects #a::b::Person\n" +
                        "{\n" +
                        "   *\n" +
                        "}#\n" +
                        "Class a::b::AddressProjection projects #a::b::Address\n" +
                        "{\n" +
                        "   *\n" +
                        "}#\n" +
                        "Association a::b::PerAddProjection projects a::b::PersonAddress<a::b::PersonProjection, a::b::PersonProjection>\n" +
                        "Association a::b::PersonAddress \n" +
                        "{\n" +
                        "   person:  a::b::Person[1];\n" +
                        "   address: a::b::Address[*];\n" +
                        "}\n");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "Invalid AssociationProjection 'a::b::PerAddProjection'. Projection for property 'address' is not specified.", "file.pure", 26, 19, e);
    }

    @Test
    public void testSimpleAssociationProjection()
    {
        runtime.createInMemorySource("file.pure",
                "Class a::b::Person\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  yearsEmployed: Integer[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class a::b::Address\n" +
                        "{\n" +
                        "  street:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class a::b::PersonProjection projects #a::b::Person\n" +
                        "{\n" +
                        "   *\n" +
                        "}#\n" +
                        "\n" +
                        "Class a::b::AddressProjection projects #a::b::Address\n" +
                        "{ \n" +
                        "   *\n" +
                        "}#\n" +
                        "\n" +
                        "Association a::b::PerAddProjection projects a::b::PersonAddress<a::b::PersonProjection, a::b::AddressProjection>\n" +
                        "\n" +
                        "Association a::b::PersonAddress \n" +
                        "{\n" +
                        "   person:  a::b::Person[1];\n" +
                        "   address: a::b::Address[*];\n" +
                        "}");
        runtime.compile();

        AssociationProjection perAddProjection = (AssociationProjection) runtime.getCoreInstance("a::b::PerAddProjection");
        Assert.assertEquals(new SourceInformation("file.pure", 22, 1, 22, 19, 22, 112), perAddProjection.getSourceInformation());

        assertPropertiesFromAssociationProjection("a::b::PersonProjection", "address");
        assertPropertiesFromAssociationProjection("a::b::AddressProjection", "person");
    }

    @Test
    public void testSimpleAssociationProjectionWithOrderFlipped()
    {
        runtime.createInMemorySource("file.pure",
                "Class a::b::Person\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  yearsEmployed: Integer[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class a::b::Address\n" +
                        "{\n" +
                        "  street:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class a::b::PersonProjection projects #a::b::Person\n" +
                        "{\n" +
                        "   *\n" +
                        "}#\n" +
                        "\n" +
                        "Class a::b::AddressProjection projects #a::b::Address\n" +
                        "{\n" +
                        "   *\n" +
                        "}#\n" +
                        "\n" +
                        "Association a::b::PerAddProjection projects\n" +
                        "   a::b::PersonAddress<a::b::AddressProjection, a::b::PersonProjection>\n" +
                        "Association a::b::PersonAddress\n" +
                        "{\n" +
                        "   person:  a::b::Person[1];\n" +
                        "   address: a::b::Address[*];\n" +
                        "}");
        runtime.compile();

        AssociationProjection perAddProjection = (AssociationProjection) runtime.getCoreInstance("a::b::PerAddProjection");
        Assert.assertEquals(new SourceInformation("file.pure", 22, 1, 22, 19, 23, 71), perAddProjection.getSourceInformation());

        assertPropertiesFromAssociationProjection("a::b::PersonProjection", "address");
        assertPropertiesFromAssociationProjection("a::b::AddressProjection", "person");
    }

    @Test
    public void testInheritedAssociationProjection()
    {
        runtime.createInMemorySource("file.pure",
                "Class a::b::Person\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  yearsEmployed: Integer[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class a::b::Address\n" +
                        "{\n" +
                        "  street:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class a::b::ZipAddress extends a::b::Address\n" +
                        "{\n" +
                        "  zip:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class a::b::PersonProjection projects #a::b::Person\n" +
                        "{\n" +
                        "   *\n" +
                        "}#\n" +
                        "\n" +
                        "Class a::b::AddressProjection projects #a::b::ZipAddress\n" +
                        "{\n" +
                        "   *\n" +
                        "}#\n" +
                        "\n" +
                        "Association a::b::PerAddProjection projects a::b::PersonAddress<a::b::PersonProjection, a::b::AddressProjection>\n" +
                        "\n" +
                        "Association a::b::PersonAddress\n" +
                        "{\n" +
                        "   person:  a::b::Person[1];\n" +
                        "   address: a::b::Address[*];\n" +
                        "}");
        runtime.compile();

        AssociationProjection perAddProjection = (AssociationProjection) runtime.getCoreInstance("a::b::PerAddProjection");
        Assert.assertEquals(new SourceInformation("file.pure", 27, 1, 27, 19, 27, 112), perAddProjection.getSourceInformation());

        assertPropertiesFromAssociationProjection("a::b::PersonProjection", "address");
        assertPropertiesFromAssociationProjection("a::b::AddressProjection", "person");
    }

    public void assertPropertiesFromAssociationProjection(String path, String property)
    {
        ClassProjection<?> projection = (ClassProjection<?>) runtime.getCoreInstance(path);
        Assert.assertNotNull(path, projection);
        Assert.assertEquals(Lists.mutable.with(property), projection._propertiesFromAssociations().collect(Property::_name, Lists.mutable.empty()));
    }

    @Test
    public void testAssociationProjectionPropertiesReferencedInQualifiedProperties()
    {
        runtime.createInMemorySource("file.pure",
                "import meta::pure::tests::model::simple::*;\n" +
                        "Class meta::pure::tests::model::simple::Trade\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "   date : Date[1];\n" +
                        "   quantity : Float[1];\n" +
                        "   settlementDateTime : Date[0..1];\n" +
                        "   latestEventDate : Date[0..1];\n" +
                        "\n" +
                        "   customerQuantity()\n" +
                        "   {\n" +
                        "      -$this.quantity;\n" +
                        "   }:Float[1];\n" +
                        "   \n" +
                        "   daysToLastEvent()\n" +
                        "   {\n" +
                        "      dateDiff($this.latestEventDate->toOne(), $this.date, DurationUnit.DAYS);\n" +
                        "   }:Integer[1];\n" +
                        "   \n" +
                        "   latestEvent()\n" +
                        "   {\n" +
                        "      $this.events->filter(e | $e.date == $this.latestEventDate)->toOne()\n" +
                        "   }:TradeEvent[1];\n" +
                        "   \n" +
                        "   eventsByDate(date:Date[1])\n" +
                        "   {\n" +
                        "      $this.events->filter(e | $e.date == $date)\n" +
                        "   }:TradeEvent[*];\n" +
                        "   \n" +
                        "   tradeDateEventType()\n" +
                        "   {\n" +
                        "      $this.eventsByDate($this.date->toOne()).eventType->toOne()\n" +
                        "   }:String[1];\n" +
                        "   \n" +
                        "   tradeDateEventTypeInlined()\n" +
                        "   {\n" +
                        "      $this.events->filter(e | $e.date == $this.date).eventType->toOne()\n" +
                        "   }:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class meta::pure::tests::model::simple::TradeEvent\n" +
                        "{\n" +
                        "   eventType : String[0..1];\n" +
                        "   date: Date[1];\n" +
                        "}\n" +
                        "Class meta::pure::tests::model::simple::TradeProjection projects\n" +
                        "#\n" +
                        "   Trade\n" +
                        "   {\n" +
                        "      -[tradeDateEventType()]\n" +
                        "   }\n" +
                        "#\n" +
                        "\n" +
                        "Class meta::pure::tests::model::simple::TradeEventProjection projects\n" +
                        "#\n" +
                        "   TradeEvent\n" +
                        "   {\n" +
                        "      *\n" +
                        "   }\n" +
                        "#\n" +
                        "\n" +
                        "Association meta::pure::tests::model::simple::TP_TEP projects Trade_TradeEvent<TradeProjection, meta::pure::tests::model::simple::TradeEventProjection>\n" +
                        "\n" +
                        "Association meta::pure::tests::model::simple::Trade_TradeEvent\n" +
                        "{\n" +
                        "   trade:  Trade[*];\n" +
                        "   events: TradeEvent [*];\n" +
                        "}\n" +
                        "function meta::pure::tests::model::simple::tradeEventProjectionType(): Property<TradeProjection, Any|*>[1]\n" +
                        "{\n" +
                        "      TradeProjection.properties->filter(p | $p.name=='events')->toOne()\n" +
                        "}\n" +
                        "function meta::pure::tests::model::simple::tradeEventProjectionReturnType(): TradeEventProjection[1]\n" +
                        "{\n" +
                        "      TradeProjection.properties->filter(p | $p.name=='events')->toOne()->genericType().typeArguments->at(0).rawType->toOne()->cast(@FunctionType).returnType.rawType->toOne()->cast(@TradeEventProjection)\n" +
                        "}");
        runtime.compile();

        AssociationProjection perAddProjection = (AssociationProjection) runtime.getCoreInstance("meta::pure::tests::model::simple::TP_TEP");
        Assert.assertEquals(new SourceInformation("file.pure", 62, 1, 62, 47, 62, 151), perAddProjection.getSourceInformation());
    }
}
