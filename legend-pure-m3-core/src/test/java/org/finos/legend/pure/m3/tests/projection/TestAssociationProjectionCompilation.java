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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Test;

public class TestAssociationProjectionCompilation extends AbstractPureTestWithCoreCompiledPlatform
{
    @Test
    public void testExceptionScenario()
    {
        try
        {
            this.runtime.createInMemorySource("file.pure", "Class a::b::Person{ name: String[1]; yearsEmployed : Integer[1]; }\n" +
                    "Class a::b::Address{ street:String[1]; }\n" +
                    "Class a::b::PersonProjection projects #a::b::Person" +
                    "{" +
                    "   *   " +
                    "}#\n" +
                    "Class a::b::AddressProjection projects #a::b::Address" +
                    "{" +
                    "   *   " +
                    "}#\n" +
                    "Association a::b::PerAddProjection projects a::b::PersonAddress<a::b::PersonProjection, a::b::AddressProjection>\n" +
                    "Association a::b::PersonAddress \n" +
                    "{\n" +
                    "   person:  a::b::PersonProjection[1];\n" +
                    "   address: a::b::Address[*];" +
                    "}\n" +
                    "");
            this.runtime.compile();
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException("Invalid AssociationProjection. Projection for property 'person' is not specified.", 5, 19, e);
        }

        try
        {
            this.runtime.modify("file.pure", "Class a::b::Person{ name: String[1]; yearsEmployed : Integer[1]; }\n" +
                    "Class a::b::Address{ street:String[1]; }\n" +
                    "Class a::b::PersonProjection projects #a::b::Person" +
                    "{ \n" +
                    "   *   " +
                    "}#" +
                    "Class a::b::AddressProjection projects #a::b::Address" +
                    "{ \n" +
                    "   *   " +
                    "}#\n" +
                    "Association a::b::PerAddProjection projects a::b::PersonAddress<a::b::PersonProjection, a::b::Address>" +
                    "Association a::b::PersonAddress \n" +
                    "{\n" +
                    "   person:  a::b::Person[1];\n" +
                    "   address: a::b::Address[*];" +
                    "}\n" +
                    "");
            this.runtime.compile();
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException("AssociationProjection 'a::b::PerAddProjection' can only be applied to ClassProjections; 'a::b::Address' is not a ClassProjection", 6, 19, e);
        }

        try
        {
            this.runtime.modify("file.pure", "Class a::b::Person{ name: String[1]; yearsEmployed : Integer[1]; }\n" +
                    "Class a::b::Address{ street:String[1]; }\n" +
                    "Class a::b::Random{ arbit:String[1]; }\n" +
                    "Class a::b::RandomProjection projects # a::b::Random{ * } #\n" +
                    "Class a::b::PersonProjection projects #a::b::Person" +
                    "{ \n" +
                    "   *   " +
                    "}#" +
                    "Class a::b::AddressProjection projects #a::b::Address" +
                    "{ \n" +
                    "   *   " +
                    "}#\n" +
                    "Association a::b::PerAddProjection projects a::b::PersonAddress<a::b::PersonProjection, a::b::RandomProjection>" +
                    "Association a::b::PersonAddress \n" +
                    "{\n" +
                    "   person:  a::b::Person[1];\n" +
                    "   address: a::b::Address[*];" +
                    "}\n" +
                    "");
            this.runtime.compile();
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException("Invalid AssociationProjection. Projection for property 'address' is not specified.", 8, 19, e);
        }

        try
        {
            this.runtime.modify("file.pure", "Class a::b::Person{ name: String[1]; yearsEmployed : Integer[1]; }\n" +
                    "Class a::b::Address{ street:String[1]; }\n" +
                    "Class a::b::Random{ arbit:String[1]; }\n" +
                    "Class a::b::RandomProjection projects # a::b::Random{ * } #\n" +
                    "Class a::b::PersonProjection projects #a::b::Person" +
                    "{ \n" +
                    "   *   " +
                    "}#" +
                    "Class a::b::AddressProjection projects #a::b::Address" +
                    "{ \n" +
                    "   *   " +
                    "}#\n" +
                    "Association a::b::PerAddProjection projects a::b::PersonAddress<a::b::PersonProjection, a::b::PersonProjection>" +
                    "Association a::b::PersonAddress \n" +
                    "{\n" +
                    "   person:  a::b::Person[1];\n" +
                    "   address: a::b::Address[*];" +
                    "}\n" +
                    "");
            this.runtime.compile();
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException("Invalid AssociationProjection. Projection for property 'address' is not specified.", 8, 19, e);
        }
    }

    @Test
    public void testSimpleAssociationProjection()
    {
        this.runtime.createInMemorySource("file.pure", "Class a::b::Person{ name: String[1]; yearsEmployed : Integer[1]; }\n" +
                "Class a::b::Address{ street:String[1]; }\n" +
                "Class a::b::PersonProjection projects #a::b::Person" +
                "{ \n" +
                "   *   " +
                "}#" +
                "Class a::b::AddressProjection projects #a::b::Address" +
                "{ \n" +
                "   *   " +
                "}#" +
                "Association a::b::PerAddProjection projects a::b::PersonAddress<a::b::PersonProjection, a::b::AddressProjection>" +
                "Association a::b::PersonAddress \n" +
                "{\n" +
                "   person:  a::b::Person[1];\n" +
                "   address: a::b::Address[*];" +
                "}\n" +
                "");
        this.runtime.compile();

        CoreInstance personProjection = this.runtime.getCoreInstance("a::b::PersonProjection");
        CoreInstance addressProjection = this.runtime.getCoreInstance("a::b::AddressProjection");

        assertPropertiesFromAssociationProjection(personProjection, "address");
        assertPropertiesFromAssociationProjection(addressProjection, "person");
    }

    @Test
    public void testSimpleAssociationProjectionWithOrderFlipped()
    {
        this.runtime.createInMemorySource("file.pure", "Class a::b::Person{ name: String[1]; yearsEmployed : Integer[1]; }\n" +
                "Class a::b::Address{ street:String[1]; }\n" +
                "Class a::b::PersonProjection projects #a::b::Person" +
                "{ \n" +
                "   *   " +
                "}#" +
                "Class a::b::AddressProjection projects #a::b::Address" +
                "{ \n" +
                "   *   " +
                "}#" +
                "Association a::b::PerAddProjection projects a::b::PersonAddress<a::b::AddressProjection, a::b::PersonProjection>" +
                "Association a::b::PersonAddress \n" +
                "{\n" +
                "   person:  a::b::Person[1];\n" +
                "   address: a::b::Address[*];" +
                "}\n" +
                "");
        this.runtime.compile();

        CoreInstance personProjection = this.runtime.getCoreInstance("a::b::PersonProjection");
        CoreInstance addressProjection = this.runtime.getCoreInstance("a::b::AddressProjection");

        assertPropertiesFromAssociationProjection(personProjection, "address");
        assertPropertiesFromAssociationProjection(addressProjection, "person");
    }

    @Test
    public void testInheritedAssociationProjection()
    {
        this.runtime.createInMemorySource("file.pure", "Class a::b::Person{ name: String[1]; yearsEmployed : Integer[1]; }\n" +
                "Class a::b::Address{ street:String[1]; }\n" +
                "Class a::b::ZipAddress extends a::b::Address { zip:String[1]; }\n" +
                "Class a::b::PersonProjection projects #a::b::Person" +
                "{ \n" +
                "   *   " +
                "}#" +
                "Class a::b::AddressProjection projects #a::b::ZipAddress" +
                "{ \n" +
                "   *   " +
                "}#" +
                "Association a::b::PerAddProjection projects a::b::PersonAddress<a::b::PersonProjection, a::b::AddressProjection>" +
                "Association a::b::PersonAddress \n" +
                "{\n" +
                "   person:  a::b::Person[1];\n" +
                "   address: a::b::Address[*];" +
                "}\n" +
                "");
        this.runtime.compile();

        CoreInstance personProjection = this.runtime.getCoreInstance("a::b::PersonProjection");
        CoreInstance addressProjection = this.runtime.getCoreInstance("a::b::AddressProjection");

        assertPropertiesFromAssociationProjection(personProjection, "address");
        assertPropertiesFromAssociationProjection(addressProjection, "person");
    }

    public void assertPropertiesFromAssociationProjection(CoreInstance projection, String... properties)
    {
        Assert.assertNotNull(projection);
        RichIterable<? extends CoreInstance> propertiesFromAssociation = Instance.getValueForMetaPropertyToManyResolved(projection, M3Properties.propertiesFromAssociations, this.processorSupport);
        Assert.assertEquals("Missing properties", properties.length, propertiesFromAssociation.size());

        RichIterable<String> names = propertiesFromAssociation.collect(CoreInstance.GET_NAME);
        Verify.assertContainsAll(names.toList(), properties);
    }

    @Test
    public void testAssociationProjectionPropertiesReferencedInQualifiedProperties() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", "import meta::pure::tests::model::simple::*;\n" +
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
                "Class meta::pure::tests::model::simple::TradeProjection projects \n" +
                "#\n" +
                "   Trade\n" +
                "   {\n" +
                "      -[tradeDateEventType()]\n" +
                "   }\n" +
                "#\n" +
                "\n" +
                "Class meta::pure::tests::model::simple::TradeEventProjection projects \n" +
                "#\n" +
                "   TradeEvent\n" +
                "   {\n" +
                "      *\n" +
                "   }\n" +
                "#\n" +
                "\n" +
                "Association meta::pure::tests::model::simple::TP_TEP projects Trade_TradeEvent<TradeProjection, meta::pure::tests::model::simple::TradeEventProjection>\n" +
                "Association meta::pure::tests::model::simple::Trade_TradeEvent \n" +
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
                "      TradeProjection.properties->filter(p | $p.name=='events')->toOne()->functionReturnType().rawType->toOne()->cast(@TradeEventProjection)\n" +
                "}\n" +
                "");
        this.runtime.compile();
    }
}
