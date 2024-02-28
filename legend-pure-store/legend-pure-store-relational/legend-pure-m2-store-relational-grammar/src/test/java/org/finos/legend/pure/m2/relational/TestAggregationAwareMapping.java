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

package org.finos.legend.pure.m2.relational;

import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.InstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregationAwareSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Comparator;

public class TestAggregationAwareMapping extends AbstractPureRelationalTestWithCoreCompiled
{
    @Test
    public void testAggregationAwareMappingGrammarSingleAggregate()
    {
        String source = "###Pure\n" +
                "Class Sales\n" +
                "{\n" +
                "   id: Integer[1];\n" +
                "   salesDate: FiscalCalendar[1];\n" +
                "   revenue: Float[1];\n" +
                "}\n" +
                "\n" +
                "Class FiscalCalendar\n" +
                "{\n" +
                "   date: Date[1];\n" +
                "   fiscalYear: Integer[1];\n" +
                "   fiscalMonth: Integer[1];\n" +
                "   fiscalQtr: Integer[1];\n" +
                "}\n" +
                "native function sum(f:Float[*]):Float[1];\n" +
                "\n" +
                "###Relational\n" +
                "Database db \n" +
                "(\n" +
                "   Table sales_base (id INT PRIMARY KEY, sales_date DATE, revenue FLOAT)\n" +
                "   Table calendar (date DATE PRIMARY KEY, fiscal_year INT, fiscal_qtr INT, fiscal_month INT)\n" +
                "   \n" +
                "   Table sales_by_date (sales_date DATE, net_revenue FLOAT)\n" +
                "   \n" +
                "   Join sales_calendar (sales_base.sales_date = calendar.date)\n" +
                "   Join sales_date_calendar (sales_by_date.sales_date = calendar.date)\n" +
                ")\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping map\n" +
                "(\n" +
                "   FiscalCalendar [b] : Relational {\n" +
                "      scope([db]calendar)\n" +
                "      (\n" +
                "         date : date,\n" +
                "         fiscalYear : fiscal_year,\n" +
                "         fiscalQtr : fiscal_qtr,\n" +
                "         fiscalMonth : fiscal_month\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Sales [a] : AggregationAware {\n" +
                "      Views : [\n" +
                "         (\n" +
                "            ~modelOperation : {\n" +
                "               ~canAggregate true,\n" +
                "               ~groupByFunctions (\n" +
                "                  $this.salesDate\n" +
                "               ),\n" +
                "               ~aggregateValues (\n" +
                "                  ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->sum() )\n" +
                "               )\n" +
                "            },\n" +
                "            ~aggregateMapping : Relational {\n" +
                "               scope([db]sales_by_date)\n" +
                "               (\n" +
                "                  salesDate [b] : [db]@sales_date_calendar,\n" +
                "                  revenue : net_revenue\n" +
                "               )\n" +
                "            }\n" +
                "         )\n" +
                "      ],\n" +
                "      ~mainMapping : Relational {\n" +
                "         scope([db]sales_base)\n" +
                "         (\n" +
                "            salesDate [b] : [db]@sales_calendar,\n" +
                "            revenue : revenue\n" +
                "         )\n" +
                "      }\n" +
                "   }\n" +
                ")";
        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.compile();

        InstanceSetImplementation setImpl = (InstanceSetImplementation)((Mapping)this.runtime.getCoreInstance("map"))._classMappings().toSortedList(new Comparator<SetImplementation>()
        {
            @Override
            public int compare(SetImplementation o1, SetImplementation o2)
            {
                return o1._id().compareTo(o2._id());
            }
        }).get(0);

        Assert.assertTrue(setImpl instanceof AggregationAwareSetImplementation);

        AggregationAwareSetImplementation aggSetImpl = (AggregationAwareSetImplementation) setImpl;
        Assert.assertEquals("a", aggSetImpl._id());

        Assert.assertNotNull(aggSetImpl._mainSetImplementation());
        Assert.assertTrue(aggSetImpl._mainSetImplementation() instanceof RootRelationalInstanceSetImplementation);
        Assert.assertEquals("a_Main", aggSetImpl._mainSetImplementation()._id());

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().size() == 1);

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._canAggregate());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._groupByFunctions().size() == 1);
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._aggregateValues().size() == 1);

        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation() instanceof RootRelationalInstanceSetImplementation);
        Assert.assertEquals("a_Aggregate_0", aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation()._id());
    }

    @Test
    public void testAggregationAwareMappingGrammarMultiAggregate()
    {
        String source = "###Pure\n" +
                "Class Sales\n" +
                "{\n" +
                "   id: Integer[1];\n" +
                "   salesDate: FiscalCalendar[1];\n" +
                "   revenue: Float[1];\n" +
                "}\n" +
                "\n" +
                "Class FiscalCalendar\n" +
                "{\n" +
                "   date: Date[1];\n" +
                "   fiscalYear: Integer[1];\n" +
                "   fiscalMonth: Integer[1];\n" +
                "   fiscalQtr: Integer[1];\n" +
                "}\n" +
                "native function sum(f:Float[*]):Float[1];\n" +
                "\n" +
                "###Relational\n" +
                "Database db \n" +
                "(\n" +
                "   Table sales_base (id INT PRIMARY KEY, sales_date DATE, revenue FLOAT)\n" +
                "   Table calendar (date DATE PRIMARY KEY, fiscal_year INT, fiscal_qtr INT, fiscal_month INT)\n" +
                "   \n" +
                "   Table sales_by_date (sales_date DATE, net_revenue FLOAT)\n" +
                "   Table sales_by_qtr (sales_qtr_first_date DATE, net_revenue FLOAT)\n" +
                "   \n" +
                "   Join sales_calendar (sales_base.sales_date = calendar.date)\n" +
                "   Join sales_date_calendar (sales_by_date.sales_date = calendar.date)\n" +
                "   Join sales_qtr_calendar (sales_by_qtr.sales_qtr_first_date = calendar.date)\n" +
                "\n" +
                ")\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping map\n" +
                "(\n" +
                "   FiscalCalendar [b] : Relational {\n" +
                "      scope([db]calendar)\n" +
                "      (\n" +
                "         date : date,\n" +
                "         fiscalYear : fiscal_year,\n" +
                "         fiscalQtr : fiscal_qtr,\n" +
                "         fiscalMonth : fiscal_month\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Sales [a] : AggregationAware {\n" +
                "      Views : [\n" +
                "         (\n" +
                "            ~modelOperation : {\n" +
                "               ~canAggregate false,\n" +
                "               ~groupByFunctions (\n" +
                "                  $this.salesDate.fiscalYear,\n" +
                "                  $this.salesDate.fiscalQtr\n" +
                "               ),\n" +
                "               ~aggregateValues (\n" +
                "                  ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->sum() )\n" +
                "               )\n" +
                "            },\n" +
                "            ~aggregateMapping : Relational {\n" +
                "               salesDate ( \n" +
                "                     fiscalQtr : [db]@sales_qtr_calendar | calendar.fiscal_qtr, \n" +
                "                     fiscalYear : [db]@sales_qtr_calendar | calendar.fiscal_year\n" +
                "                  ),\n" +
                "               revenue : [db]sales_by_qtr.net_revenue\n" +
                "            }\n" +
                "         ),\n" +
                "         (\n" +
                "            ~modelOperation : {\n" +
                "               ~canAggregate true,\n" +
                "               ~groupByFunctions (\n" +
                "                  $this.salesDate\n" +
                "               ),\n" +
                "               ~aggregateValues (\n" +
                "                  ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->sum() )\n" +
                "               )\n" +
                "            },\n" +
                "            ~aggregateMapping : Relational {\n" +
                "               scope([db]sales_by_date)\n" +
                "               (\n" +
                "                  salesDate [b] : [db]@sales_date_calendar,\n" +
                "                  revenue : net_revenue\n" +
                "               )\n" +
                "            }\n" +
                "         )\n" +
                "      ],\n" +
                "      ~mainMapping : Relational {\n" +
                "         scope([db]sales_base)\n" +
                "         (\n" +
                "            salesDate [b] : [db]@sales_calendar,\n" +
                "            revenue : revenue\n" +
                "         )\n" +
                "      }\n" +
                "   }\n" +
                ")";
        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.compile();

        InstanceSetImplementation setImpl = (InstanceSetImplementation)((Mapping)this.runtime.getCoreInstance("map"))._classMappings().toSortedList(new Comparator<SetImplementation>()
        {
            @Override
            public int compare(SetImplementation o1, SetImplementation o2)
            {
                return o1._id().compareTo(o2._id());
            }
        }).get(0);

        Assert.assertTrue(setImpl instanceof AggregationAwareSetImplementation);

        AggregationAwareSetImplementation aggSetImpl = (AggregationAwareSetImplementation) setImpl;
        Assert.assertEquals("a", aggSetImpl._id());

        Assert.assertNotNull(aggSetImpl._mainSetImplementation());
        Assert.assertTrue(aggSetImpl._mainSetImplementation() instanceof RootRelationalInstanceSetImplementation);
        Assert.assertEquals("a_Main", aggSetImpl._mainSetImplementation()._id());

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().size() == 2);

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification());
        Assert.assertFalse(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._canAggregate());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._groupByFunctions().size() == 2);
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._aggregateValues().size() == 1);

        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation() instanceof RootRelationalInstanceSetImplementation);
        Assert.assertEquals("a_Aggregate_0", aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation()._id());

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._canAggregate());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._groupByFunctions().size() == 1);
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._aggregateValues().size() == 1);

        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._setImplementation() instanceof RootRelationalInstanceSetImplementation);
        Assert.assertEquals("a_Aggregate_1", aggSetImpl._aggregateSetImplementations().toList().get(1)._setImplementation()._id());
    }

    @Test
    public void testAggregationAwareMappingGrammarMultiViewsMultiAggregateValues()
    {
        String source = "###Pure\n" +
                "Class Sales\n" +
                "{\n" +
                "   id: Integer[1];\n" +
                "   salesDate: FiscalCalendar[1];\n" +
                "   revenue: Float[1];\n" +
                "}\n" +
                "\n" +
                "Class FiscalCalendar\n" +
                "{\n" +
                "   date: Date[1];\n" +
                "   fiscalYear: Integer[1];\n" +
                "   fiscalMonth: Integer[1];\n" +
                "   fiscalQtr: Integer[1];\n" +
                "}\n" +
                "native function sum(f:Float[*]):Float[1];\n" +
                "\n" +
                "###Relational\n" +
                "Database db \n" +
                "(\n" +
                "   Table sales_base (id INT PRIMARY KEY, sales_date DATE, revenue FLOAT)\n" +
                "   Table calendar (date DATE PRIMARY KEY, fiscal_year INT, fiscal_qtr INT, fiscal_month INT)\n" +
                "   \n" +
                "   Table sales_by_date (sales_date DATE, net_revenue FLOAT)\n" +
                "   Table sales_by_qtr (sales_qtr_first_date DATE, net_revenue FLOAT)\n" +
                "   \n" +
                "   Join sales_calendar (sales_base.sales_date = calendar.date)\n" +
                "   Join sales_date_calendar (sales_by_date.sales_date = calendar.date)\n" +
                "   Join sales_qtr_calendar (sales_by_qtr.sales_qtr_first_date = calendar.date)\n" +
                "\n" +
                ")\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping map\n" +
                "(\n" +
                "   FiscalCalendar [b] : Relational {\n" +
                "      scope([db]calendar)\n" +
                "      (\n" +
                "         date : date,\n" +
                "         fiscalYear : fiscal_year,\n" +
                "         fiscalQtr : fiscal_qtr,\n" +
                "         fiscalMonth : fiscal_month\n" +
                "      )\n" +
                "   }\n" +
                "   \n" +
                "   Sales [a] : AggregationAware {\n" +
                "      Views : [\n" +
                "         (\n" +
                "            ~modelOperation : {\n" +
                "               ~canAggregate false,\n" +
                "               ~groupByFunctions (\n" +
                "                  $this.salesDate.fiscalYear,\n" +
                "                  $this.salesDate.fiscalQtr\n" +
                "               ),\n" +
                "               ~aggregateValues (\n" +
                "                  ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->sum() )\n" +
                "               )\n" +
                "            },\n" +
                "            ~aggregateMapping : Relational {\n" +
                "               salesDate ( \n" +
                "                     fiscalQtr : [db]@sales_qtr_calendar | calendar.fiscal_qtr, \n" +
                "                     fiscalYear : [db]@sales_qtr_calendar | calendar.fiscal_year\n" +
                "                  ),\n" +
                "               revenue : [db]sales_by_qtr.net_revenue\n" +
                "            }\n" +
                "         ),\n" +
                "         (\n" +
                "            ~modelOperation : {\n" +
                "               ~canAggregate true,\n" +
                "               ~groupByFunctions (\n" +
                "                  $this.salesDate\n" +
                "               ),\n" +
                "               ~aggregateValues (\n" +
                "                  ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->sum() ),\n" +
                "                  ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->sum() )\n" +
                "               )\n" +
                "            },\n" +
                "            ~aggregateMapping : Relational {\n" +
                "               scope([db]sales_by_date)\n" +
                "               (\n" +
                "                  salesDate [b] : [db]@sales_date_calendar,\n" +
                "                  revenue : net_revenue\n" +
                "               )\n" +
                "            }\n" +
                "         )\n" +
                "      ],\n" +
                "      ~mainMapping : Relational {\n" +
                "         scope([db]sales_base)\n" +
                "         (\n" +
                "            salesDate [b] : [db]@sales_calendar,\n" +
                "            revenue : revenue\n" +
                "         )\n" +
                "      }\n" +
                "   }\n" +
                ")";
        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.compile();

        InstanceSetImplementation setImpl = (InstanceSetImplementation)((Mapping)this.runtime.getCoreInstance("map"))._classMappings().toSortedList(new Comparator<SetImplementation>()
        {
            @Override
            public int compare(SetImplementation o1, SetImplementation o2)
            {
                return o1._id().compareTo(o2._id());
            }
        }).get(0);

        Assert.assertTrue(setImpl instanceof AggregationAwareSetImplementation);

        AggregationAwareSetImplementation aggSetImpl = (AggregationAwareSetImplementation) setImpl;
        Assert.assertEquals("a", aggSetImpl._id());

        Assert.assertNotNull(aggSetImpl._mainSetImplementation());
        Assert.assertTrue(aggSetImpl._mainSetImplementation() instanceof RootRelationalInstanceSetImplementation);
        Assert.assertEquals("a_Main", aggSetImpl._mainSetImplementation()._id());

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().size() == 2);

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification());
        Assert.assertFalse(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._canAggregate());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._groupByFunctions().size() == 2);
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._aggregateValues().size() == 1);

        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation() instanceof RootRelationalInstanceSetImplementation);
        Assert.assertEquals("a_Aggregate_0", aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation()._id());

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._canAggregate());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._groupByFunctions().size() == 1);
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._aggregateValues().size() == 2);

        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._setImplementation() instanceof RootRelationalInstanceSetImplementation);
        Assert.assertEquals("a_Aggregate_1", aggSetImpl._aggregateSetImplementations().toList().get(1)._setImplementation()._id());
    }

    @Test
    public void testAggregationAwareMappingErrorInMainSetImplementationTarget()
    {
        this.runtime.createInMemorySource("mapping.pure",
                "###Pure\n" +
                        "Class Sales\n" +
                        "{\n" +
                        "   id: Integer[1];\n" +
                        "   salesDate: FiscalCalendar[1];\n" +
                        "   revenue: Float[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class FiscalCalendar\n" +
                        "{\n" +
                        "   date: Date[1];\n" +
                        "   fiscalYear: Integer[1];\n" +
                        "   fiscalMonth: Integer[1];\n" +
                        "   fiscalQtr: Integer[1];\n" +
                        "}\n" +
                        "native function sum(f:Float[*]):Float[1];\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database db \n" +
                        "(\n" +
                        "   Table sales_base (id INT PRIMARY KEY, sales_date DATE, revenue FLOAT)\n" +
                        "   Table calendar (date DATE PRIMARY KEY, fiscal_year INT, fiscal_qtr INT, fiscal_month INT)\n" +
                        "   \n" +
                        "   Table sales_by_date (sales_date DATE, net_revenue FLOAT)\n" +
                        "   \n" +
                        "   Join sales_calendar (sales_base.sales_date = calendar.date)\n" +
                        "   Join sales_date_calendar (sales_by_date.sales_date = calendar.date)\n" +
                        ")\n" +
                        "\n" +
                        "###Mapping\n" +
                        "Mapping map\n" +
                        "(\n" +
                        "   FiscalCalendar [b] : Relational {\n" +
                        "      scope([db]calendar)\n" +
                        "      (\n" +
                        "         date : date,\n" +
                        "         fiscalYear : fiscal_year,\n" +
                        "         fiscalQtr : fiscal_qtr,\n" +
                        "         fiscalMonth : fiscal_month\n" +
                        "      )\n" +
                        "   }\n" +
                        "   \n" +
                        "   Sales [a] : AggregationAware {\n" +
                        "      Views : [\n" +
                        "         (\n" +
                        "            ~modelOperation : {\n" +
                        "               ~canAggregate true,\n" +
                        "               ~groupByFunctions (\n" +
                        "                  $this.salesDate\n" +
                        "               ),\n" +
                        "               ~aggregateValues (\n" +
                        "                  ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->sum() )\n" +
                        "               )\n" +
                        "            },\n" +
                        "            ~aggregateMapping : Relational {\n" +
                        "               scope([db]sales_by_date)\n" +
                        "               (\n" +
                        "                  salesDate [b] : [db]@sales_date_calendar,\n" +
                        "                  revenue : net_revenue\n" +
                        "               )\n" +
                        "            }\n" +
                        "         )\n" +
                        "      ],\n" +
                        "      ~mainMapping : Relational {\n" +
                        "         scope([db]sales_base)\n" +
                        "         (\n" +
                        "            salesDate [b] : [db]@sales_calendar_nonExistent,\n" +
                        "            revenue : revenue\n" +
                        "         )\n" +
                        "      }\n" +
                        "   }\n" +
                        ")");
        try
        {
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:mapping.pure line:67 column:34), \"The join 'sales_calendar_nonExistent' has not been found in the database 'db'\"", e.getMessage());
        }
    }

    @Test
    public void testAggregationAwareMappingErrorInMainSetImplementationProperty()
    {
        this.runtime.createInMemorySource("mapping.pure",
                "###Pure\n" +
                        "Class Sales\n" +
                        "{\n" +
                        "   id: Integer[1];\n" +
                        "   salesDate: FiscalCalendar[1];\n" +
                        "   revenue: Float[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class FiscalCalendar\n" +
                        "{\n" +
                        "   date: Date[1];\n" +
                        "   fiscalYear: Integer[1];\n" +
                        "   fiscalMonth: Integer[1];\n" +
                        "   fiscalQtr: Integer[1];\n" +
                        "}\n" +
                        "native function sum(f:Float[*]):Float[1];\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database db \n" +
                        "(\n" +
                        "   Table sales_base (id INT PRIMARY KEY, sales_date DATE, revenue FLOAT)\n" +
                        "   Table calendar (date DATE PRIMARY KEY, fiscal_year INT, fiscal_qtr INT, fiscal_month INT)\n" +
                        "   \n" +
                        "   Table sales_by_date (sales_date DATE, net_revenue FLOAT)\n" +
                        "   \n" +
                        "   Join sales_calendar (sales_base.sales_date = calendar.date)\n" +
                        "   Join sales_date_calendar (sales_by_date.sales_date = calendar.date)\n" +
                        ")\n" +
                        "\n" +
                        "###Mapping\n" +
                        "Mapping map\n" +
                        "(\n" +
                        "   FiscalCalendar [b] : Relational {\n" +
                        "      scope([db]calendar)\n" +
                        "      (\n" +
                        "         date : date,\n" +
                        "         fiscalYear : fiscal_year,\n" +
                        "         fiscalQtr : fiscal_qtr,\n" +
                        "         fiscalMonth : fiscal_month\n" +
                        "      )\n" +
                        "   }\n" +
                        "   \n" +
                        "   Sales [a] : AggregationAware {\n" +
                        "      Views : [\n" +
                        "         (\n" +
                        "            ~modelOperation : {\n" +
                        "               ~canAggregate true,\n" +
                        "               ~groupByFunctions (\n" +
                        "                  $this.salesDate\n" +
                        "               ),\n" +
                        "               ~aggregateValues (\n" +
                        "                  ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->sum() )\n" +
                        "               )\n" +
                        "            },\n" +
                        "            ~aggregateMapping : Relational {\n" +
                        "               scope([db]sales_by_date)\n" +
                        "               (\n" +
                        "                  salesDate [b] : [db]@sales_date_calendar,\n" +
                        "                  revenue : net_revenue\n" +
                        "               )\n" +
                        "            }\n" +
                        "         )\n" +
                        "      ],\n" +
                        "      ~mainMapping : Relational {\n" +
                        "         scope([db]sales_base)\n" +
                        "         (\n" +
                        "            salesDate_nonExistent [b] : [db]@sales_calendar,\n" +
                        "            revenue : revenue\n" +
                        "         )\n" +
                        "      }\n" +
                        "   }\n" +
                        ")");
        try
        {
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:mapping.pure line:67 column:13), \"The property 'salesDate_nonExistent' is unknown in the Element 'Sales'\"", e.getMessage());
        }
    }

    @Test
    public void testAggregationAwareMappingErrorInAggregateViewTarget()
    {
        this.runtime.createInMemorySource("mapping.pure",
                "###Pure\n" +
                        "Class Sales\n" +
                        "{\n" +
                        "   id: Integer[1];\n" +
                        "   salesDate: FiscalCalendar[1];\n" +
                        "   revenue: Float[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class FiscalCalendar\n" +
                        "{\n" +
                        "   date: Date[1];\n" +
                        "   fiscalYear: Integer[1];\n" +
                        "   fiscalMonth: Integer[1];\n" +
                        "   fiscalQtr: Integer[1];\n" +
                        "}\n" +
                        "\n" +
                        "native function sum(f:Float[*]):Float[1];\n" +
                        "###Relational\n" +
                        "Database db \n" +
                        "(\n" +
                        "   Table sales_base (id INT PRIMARY KEY, sales_date DATE, revenue FLOAT)\n" +
                        "   Table calendar (date DATE PRIMARY KEY, fiscal_year INT, fiscal_qtr INT, fiscal_month INT)\n" +
                        "   \n" +
                        "   Table sales_by_date (sales_date DATE, net_revenue FLOAT)\n" +
                        "   \n" +
                        "   Join sales_calendar (sales_base.sales_date = calendar.date)\n" +
                        "   Join sales_date_calendar (sales_by_date.sales_date = calendar.date)\n" +
                        ")\n" +
                        "\n" +
                        "###Mapping\n" +
                        "Mapping map\n" +
                        "(\n" +
                        "   FiscalCalendar [b] : Relational {\n" +
                        "      scope([db]calendar)\n" +
                        "      (\n" +
                        "         date : date,\n" +
                        "         fiscalYear : fiscal_year,\n" +
                        "         fiscalQtr : fiscal_qtr,\n" +
                        "         fiscalMonth : fiscal_month\n" +
                        "      )\n" +
                        "   }\n" +
                        "   \n" +
                        "   Sales [a] : AggregationAware {\n" +
                        "      Views : [\n" +
                        "         (\n" +
                        "            ~modelOperation : {\n" +
                        "               ~canAggregate true,\n" +
                        "               ~groupByFunctions (\n" +
                        "                  $this.salesDate\n" +
                        "               ),\n" +
                        "               ~aggregateValues (\n" +
                        "                  ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->sum() )\n" +
                        "               )\n" +
                        "            },\n" +
                        "            ~aggregateMapping : Relational {\n" +
                        "               scope([db]sales_by_date)\n" +
                        "               (\n" +
                        "                  salesDate [b] : [db]@sales_date_calendar_nonExistent,\n" +
                        "                  revenue : net_revenue\n" +
                        "               )\n" +
                        "            }\n" +
                        "         )\n" +
                        "      ],\n" +
                        "      ~mainMapping : Relational {\n" +
                        "         scope([db]sales_base)\n" +
                        "         (\n" +
                        "            salesDate [b] : [db]@sales_calendar,\n" +
                        "            revenue : revenue\n" +
                        "         )\n" +
                        "      }\n" +
                        "   }\n" +
                        ")");
        try
        {
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:mapping.pure line:58 column:40), \"The join 'sales_date_calendar_nonExistent' has not been found in the database 'db'\"", e.getMessage());
        }
    }

    @Test
    public void testAggregationAwareMappingErrorInAggregateViewProperty()
    {
        this.runtime.createInMemorySource("mapping.pure",
                "###Pure\n" +
                        "Class Sales\n" +
                        "{\n" +
                        "   id: Integer[1];\n" +
                        "   salesDate: FiscalCalendar[1];\n" +
                        "   revenue: Float[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class FiscalCalendar\n" +
                        "{\n" +
                        "   date: Date[1];\n" +
                        "   fiscalYear: Integer[1];\n" +
                        "   fiscalMonth: Integer[1];\n" +
                        "   fiscalQtr: Integer[1];\n" +
                        "}\n" +
                        "native function sum(f:Float[*]):Float[1];\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database db \n" +
                        "(\n" +
                        "   Table sales_base (id INT PRIMARY KEY, sales_date DATE, revenue FLOAT)\n" +
                        "   Table calendar (date DATE PRIMARY KEY, fiscal_year INT, fiscal_qtr INT, fiscal_month INT)\n" +
                        "   \n" +
                        "   Table sales_by_date (sales_date DATE, net_revenue FLOAT)\n" +
                        "   \n" +
                        "   Join sales_calendar (sales_base.sales_date = calendar.date)\n" +
                        "   Join sales_date_calendar (sales_by_date.sales_date = calendar.date)\n" +
                        ")\n" +
                        "\n" +
                        "###Mapping\n" +
                        "Mapping map\n" +
                        "(\n" +
                        "   FiscalCalendar [b] : Relational {\n" +
                        "      scope([db]calendar)\n" +
                        "      (\n" +
                        "         date : date,\n" +
                        "         fiscalYear : fiscal_year,\n" +
                        "         fiscalQtr : fiscal_qtr,\n" +
                        "         fiscalMonth : fiscal_month\n" +
                        "      )\n" +
                        "   }\n" +
                        "   \n" +
                        "   Sales [a] : AggregationAware {\n" +
                        "      Views : [\n" +
                        "         (\n" +
                        "            ~modelOperation : {\n" +
                        "               ~canAggregate true,\n" +
                        "               ~groupByFunctions (\n" +
                        "                  $this.salesDate\n" +
                        "               ),\n" +
                        "               ~aggregateValues (\n" +
                        "                  ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->sum() )\n" +
                        "               )\n" +
                        "            },\n" +
                        "            ~aggregateMapping : Relational {\n" +
                        "               scope([db]sales_by_date)\n" +
                        "               (\n" +
                        "                  salesDate_nonExistent [b] : [db]@sales_date_calendar,\n" +
                        "                  revenue : net_revenue\n" +
                        "               )\n" +
                        "            }\n" +
                        "         )\n" +
                        "      ],\n" +
                        "      ~mainMapping : Relational {\n" +
                        "         scope([db]sales_base)\n" +
                        "         (\n" +
                        "            salesDate [b] : [db]@sales_calendar,\n" +
                        "            revenue : revenue\n" +
                        "         )\n" +
                        "      }\n" +
                        "   }\n" +
                        ")");
        try
        {
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:mapping.pure line:58 column:19), \"The property 'salesDate_nonExistent' is unknown in the Element 'Sales'\"", e.getMessage());
        }
    }

    @Test
    public void testAggregationAwareMappingErrorInAggregateViewModelOperationGroupByFunction()
    {
        this.runtime.createInMemorySource("mapping.pure",
                "###Pure\n" +
                        "Class Sales\n" +
                        "{\n" +
                        "   id: Integer[1];\n" +
                        "   salesDate: FiscalCalendar[1];\n" +
                        "   revenue: Float[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class FiscalCalendar\n" +
                        "{\n" +
                        "   date: Date[1];\n" +
                        "   fiscalYear: Integer[1];\n" +
                        "   fiscalMonth: Integer[1];\n" +
                        "   fiscalQtr: Integer[1];\n" +
                        "}\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database db \n" +
                        "(\n" +
                        "   Table sales_base (id INT PRIMARY KEY, sales_date DATE, revenue FLOAT)\n" +
                        "   Table calendar (date DATE PRIMARY KEY, fiscal_year INT, fiscal_qtr INT, fiscal_month INT)\n" +
                        "   \n" +
                        "   Table sales_by_date (sales_date DATE, net_revenue FLOAT)\n" +
                        "   \n" +
                        "   Join sales_calendar (sales_base.sales_date = calendar.date)\n" +
                        "   Join sales_date_calendar (sales_by_date.sales_date = calendar.date)\n" +
                        ")\n" +
                        "\n" +
                        "###Mapping\n" +
                        "Mapping map\n" +
                        "(\n" +
                        "   FiscalCalendar [b] : Relational {\n" +
                        "      scope([db]calendar)\n" +
                        "      (\n" +
                        "         date : date,\n" +
                        "         fiscalYear : fiscal_year,\n" +
                        "         fiscalQtr : fiscal_qtr,\n" +
                        "         fiscalMonth : fiscal_month\n" +
                        "      )\n" +
                        "   }\n" +
                        "   \n" +
                        "   Sales [a] : AggregationAware {\n" +
                        "      Views : [\n" +
                        "         (\n" +
                        "            ~modelOperation : {\n" +
                        "               ~canAggregate true,\n" +
                        "               ~groupByFunctions (\n" +
                        "                  $this.salesDate_nonExistent\n" +
                        "               ),\n" +
                        "               ~aggregateValues (\n" +
                        "                  ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->sum() )\n" +
                        "               )\n" +
                        "            },\n" +
                        "            ~aggregateMapping : Relational {\n" +
                        "               scope([db]sales_by_date)\n" +
                        "               (\n" +
                        "                  salesDate [b] : [db]@sales_date_calendar,\n" +
                        "                  revenue : net_revenue\n" +
                        "               )\n" +
                        "            }\n" +
                        "         )\n" +
                        "      ],\n" +
                        "      ~mainMapping : Relational {\n" +
                        "         scope([db]sales_base)\n" +
                        "         (\n" +
                        "            salesDate [b] : [db]@sales_calendar,\n" +
                        "            revenue : revenue\n" +
                        "         )\n" +
                        "      }\n" +
                        "   }\n" +
                        ")");
        try
        {
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:mapping.pure line:48 column:25), \"Can't find the property 'salesDate_nonExistent' in the class Sales\"", e.getMessage());
        }
    }

    @Test
    public void testAggregationAwareMappingErrorInAggregateViewModelOperationAggregateFunction()
    {
        this.runtime.createInMemorySource("mapping.pure",
                "###Pure\n" +
                        "Class Sales\n" +
                        "{\n" +
                        "   id: Integer[1];\n" +
                        "   salesDate: FiscalCalendar[1];\n" +
                        "   revenue: Float[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class FiscalCalendar\n" +
                        "{\n" +
                        "   date: Date[1];\n" +
                        "   fiscalYear: Integer[1];\n" +
                        "   fiscalMonth: Integer[1];\n" +
                        "   fiscalQtr: Integer[1];\n" +
                        "}\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database db \n" +
                        "(\n" +
                        "   Table sales_base (id INT PRIMARY KEY, sales_date DATE, revenue FLOAT)\n" +
                        "   Table calendar (date DATE PRIMARY KEY, fiscal_year INT, fiscal_qtr INT, fiscal_month INT)\n" +
                        "   \n" +
                        "   Table sales_by_date (sales_date DATE, net_revenue FLOAT)\n" +
                        "   \n" +
                        "   Join sales_calendar (sales_base.sales_date = calendar.date)\n" +
                        "   Join sales_date_calendar (sales_by_date.sales_date = calendar.date)\n" +
                        ")\n" +
                        "\n" +
                        "###Mapping\n" +
                        "Mapping map\n" +
                        "(\n" +
                        "   FiscalCalendar [b] : Relational {\n" +
                        "      scope([db]calendar)\n" +
                        "      (\n" +
                        "         date : date,\n" +
                        "         fiscalYear : fiscal_year,\n" +
                        "         fiscalQtr : fiscal_qtr,\n" +
                        "         fiscalMonth : fiscal_month\n" +
                        "      )\n" +
                        "   }\n" +
                        "   \n" +
                        "   Sales [a] : AggregationAware {\n" +
                        "      Views : [\n" +
                        "         (\n" +
                        "            ~modelOperation : {\n" +
                        "               ~canAggregate true,\n" +
                        "               ~groupByFunctions (\n" +
                        "                  $this.salesDate\n" +
                        "               ),\n" +
                        "               ~aggregateValues (\n" +
                        "                  ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->summation() )\n" +
                        "               )\n" +
                        "            },\n" +
                        "            ~aggregateMapping : Relational {\n" +
                        "               scope([db]sales_by_date)\n" +
                        "               (\n" +
                        "                  salesDate [b] : [db]@sales_date_calendar,\n" +
                        "                  revenue : net_revenue\n" +
                        "               )\n" +
                        "            }\n" +
                        "         )\n" +
                        "      ],\n" +
                        "      ~mainMapping : Relational {\n" +
                        "         scope([db]sales_base)\n" +
                        "         (\n" +
                        "            salesDate [b] : [db]@sales_calendar,\n" +
                        "            revenue : revenue\n" +
                        "         )\n" +
                        "      }\n" +
                        "   }\n" +
                        ")");
        try
        {
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:mapping.pure line:51 column:67), \"The system can't find a match for the function: summation(_:Float[*])\"", e.getMessage());
        }
    }
}
