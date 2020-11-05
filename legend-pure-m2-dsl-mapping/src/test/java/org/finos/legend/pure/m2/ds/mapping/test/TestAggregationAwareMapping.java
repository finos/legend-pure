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


import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.InstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregationAwareSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelToModel.PureInstanceSetImplementation;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Comparator;

public class TestAggregationAwareMapping extends AbstractPureMappingTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("mapping.pure");
    }

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
                "\n" +
                "Class Sales_By_Date\n" +
                "{\n" +
                "   salesDate: FiscalCalendar[1];\n" +
                "   netRevenue: Float[1];\n" +
                "}\n" +
                "\n" +
                "function meta::pure::functions::math::sum(numbers:Float[*]):Float[1]\n" +
                "{\n" +
                "    $numbers->plus();\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping map\n" +
                "(\n" +
                "   FiscalCalendar [b] : Pure {\n" +
                "      ~src FiscalCalendar\n" +
                "      date : $src.date,\n" +
                "      fiscalYear : $src.fiscalYear,\n" +
                "      fiscalMonth : $src.fiscalMonth,\n" +
                "      fiscalQtr : $src.fiscalQtr\n" +
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
                "            ~aggregateMapping : Pure {\n" +
                "               ~src Sales_By_Date\n" +
                "               salesDate [b] : $src.salesDate,\n" +
                "               revenue : $src.netRevenue\n" +
                "            }\n" +
                "         )\n" +
                "      ],\n" +
                "      ~mainMapping : Pure {\n" +
                "         ~src Sales\n" +
                "         salesDate [b] : $src.salesDate,\n" +
                "         revenue : $src.revenue\n" +
                "      }\n" +
                "   }\n" +
                ")";
        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.compile();

        InstanceSetImplementation setImpl = (InstanceSetImplementation)((org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping)this.runtime.getCoreInstance("map"))._classMappings().toSortedList(new Comparator<SetImplementation>()
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
        Assert.assertTrue(aggSetImpl._mainSetImplementation() instanceof PureInstanceSetImplementation);
        Assert.assertEquals("a_Main", aggSetImpl._mainSetImplementation()._id());

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().size() == 1);

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._canAggregate());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._groupByFunctions().size() == 1);
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._aggregateValues().size() == 1);

        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation() instanceof PureInstanceSetImplementation);
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
                "\n" +
                "Class Sales_By_Date\n" +
                "{\n" +
                "   salesDate: FiscalCalendar[1];\n" +
                "   netRevenue: Float[1];\n" +
                "}\n" +
                "\n" +
                "Class Sales_By_Qtr\n" +
                "{\n" +
                "   salesQtrFirstDate: FiscalCalendar[1];\n" +
                "   netRevenue: Float[1];\n" +
                "}\n" +
                "\n" +
                "function meta::pure::functions::math::sum(numbers:Float[*]):Float[1]\n" +
                "{\n" +
                "    $numbers->plus();\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping map\n" +
                "(\n" +
                "   FiscalCalendar [b] : Pure {\n" +
                "      ~src FiscalCalendar\n" +
                "      date : $src.date,\n" +
                "      fiscalYear : $src.fiscalYear,\n" +
                "      fiscalMonth : $src.fiscalMonth,\n" +
                "      fiscalQtr : $src.fiscalQtr\n" +
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
                "            ~aggregateMapping : Pure {\n" +
                "               ~src Sales_By_Qtr\n" +
                "               salesDate [b] : $src.salesQtrFirstDate,\n" +
                "               revenue : $src.netRevenue\n" +
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
                "            ~aggregateMapping : Pure {\n" +
                "               ~src Sales_By_Date\n" +
                "               salesDate [b] : $src.salesDate,\n" +
                "               revenue : $src.netRevenue\n" +
                "            }\n" +
                "         )\n" +
                "      ],\n" +
                "      ~mainMapping : Pure {\n" +
                "         ~src Sales\n" +
                "         salesDate [b] : $src.salesDate,\n" +
                "         revenue : $src.revenue\n" +
                "      }\n" +
                "   }\n" +
                ")";
        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.compile();

        InstanceSetImplementation setImpl = (InstanceSetImplementation)((org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping)this.runtime.getCoreInstance("map"))._classMappings().toSortedList(new Comparator<SetImplementation>()
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
        Assert.assertTrue(aggSetImpl._mainSetImplementation() instanceof PureInstanceSetImplementation);
        Assert.assertEquals("a_Main", aggSetImpl._mainSetImplementation()._id());

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().size() == 2);

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification());
        Assert.assertFalse(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._canAggregate());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._groupByFunctions().size() == 2);
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._aggregateValues().size() == 1);

        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation() instanceof PureInstanceSetImplementation);
        Assert.assertEquals("a_Aggregate_0", aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation()._id());

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._canAggregate());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._groupByFunctions().size() == 1);
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._aggregateValues().size() == 1);

        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._setImplementation() instanceof PureInstanceSetImplementation);
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
                "\n" +
                "Class Sales_By_Date\n" +
                "{\n" +
                "   salesDate: FiscalCalendar[1];\n" +
                "   netRevenue: Float[1];\n" +
                "}\n" +
                "\n" +
                "Class Sales_By_Qtr\n" +
                "{\n" +
                "   salesQtrFirstDate: FiscalCalendar[1];\n" +
                "   netRevenue: Float[1];\n" +
                "}\n" +
                "\n" +
                "function meta::pure::functions::math::sum(numbers:Float[*]):Float[1]\n" +
                "{\n" +
                "    $numbers->plus();\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping map\n" +
                "(\n" +
                "   FiscalCalendar [b] : Pure {\n" +
                "      ~src FiscalCalendar\n" +
                "      date : $src.date,\n" +
                "      fiscalYear : $src.fiscalYear,\n" +
                "      fiscalMonth : $src.fiscalMonth,\n" +
                "      fiscalQtr : $src.fiscalQtr\n" +
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
                "            ~aggregateMapping : Pure {\n" +
                "               ~src Sales_By_Qtr\n" +
                "               salesDate [b] : $src.salesQtrFirstDate,\n" +
                "               revenue : $src.netRevenue\n" +
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
                "            ~aggregateMapping : Pure {\n" +
                "               ~src Sales_By_Date\n" +
                "               salesDate [b] : $src.salesDate,\n" +
                "               revenue : $src.netRevenue\n" +
                "            }\n" +
                "         )\n" +
                "      ],\n" +
                "      ~mainMapping : Pure {\n" +
                "         ~src Sales\n" +
                "         salesDate [b] : $src.salesDate,\n" +
                "         revenue : $src.revenue\n" +
                "      }\n" +
                "   }\n" +
                ")";
        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.compile();

        InstanceSetImplementation setImpl = (InstanceSetImplementation)((org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping)this.runtime.getCoreInstance("map"))._classMappings().toSortedList(new Comparator<SetImplementation>()
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
        Assert.assertTrue(aggSetImpl._mainSetImplementation() instanceof PureInstanceSetImplementation);
        Assert.assertEquals("a_Main", aggSetImpl._mainSetImplementation()._id());

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().size() == 2);

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification());
        Assert.assertFalse(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._canAggregate());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._groupByFunctions().size() == 2);
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._aggregateValues().size() == 1);

        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation() instanceof PureInstanceSetImplementation);
        Assert.assertEquals("a_Aggregate_0", aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation()._id());

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._canAggregate());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._groupByFunctions().size() == 1);
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._aggregateValues().size() == 2);

        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._setImplementation() instanceof PureInstanceSetImplementation);
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
                        "\n" +
                        "Class Sales_By_Date\n" +
                        "{\n" +
                        "   salesDate: FiscalCalendar[1];\n" +
                        "   netRevenue: Float[1];\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::math::sum(numbers:Float[*]):Float[1]\n" +
                        "{\n" +
                        "    $numbers->plus();\n" +
                        "}\n" +
                        "###Mapping\n" +
                        "Mapping map\n" +
                        "(\n" +
                        "   FiscalCalendar [b] : Pure {\n" +
                        "      ~src FiscalCalendar\n" +
                        "      date : $src.date,\n" +
                        "      fiscalYear : $src.fiscalYear,\n" +
                        "      fiscalMonth : $src.fiscalMonth,\n" +
                        "      fiscalQtr : $src.fiscalQtr\n" +
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
                        "            ~aggregateMapping : Pure {\n" +
                        "               ~src Sales_By_Date\n" +
                        "               salesDate [b] : $src.salesDate,\n" +
                        "               revenue : $src.netRevenue\n" +
                        "            }\n" +
                        "         )\n" +
                        "      ],\n" +
                        "      ~mainMapping : Pure {\n" +
                        "         ~src Sales\n" +
                        "         salesDate [b] : $src.salesDate_NonExistent,\n" +
                        "         revenue : $src.revenue\n" +
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
            Assert.assertEquals("Compilation error at (resource:mapping.pure line:59 column:31), \"Can't find the property 'salesDate_NonExistent' in the class Sales\"", e.getMessage());
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
                        "\n" +
                        "Class Sales_By_Date\n" +
                        "{\n" +
                        "   salesDate: FiscalCalendar[1];\n" +
                        "   netRevenue: Float[1];\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::math::sum(numbers:Float[*]):Float[1]\n" +
                        "{\n" +
                        "    $numbers->plus();\n" +
                        "}\n" +
                        "###Mapping\n" +
                        "Mapping map\n" +
                        "(\n" +
                        "   FiscalCalendar [b] : Pure {\n" +
                        "      ~src FiscalCalendar\n" +
                        "      date : $src.date,\n" +
                        "      fiscalYear : $src.fiscalYear,\n" +
                        "      fiscalMonth : $src.fiscalMonth,\n" +
                        "      fiscalQtr : $src.fiscalQtr\n" +
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
                        "            ~aggregateMapping : Pure {\n" +
                        "               ~src Sales_By_Date\n" +
                        "               salesDate [b] : $src.salesDate,\n" +
                        "               revenue : $src.netRevenue\n" +
                        "            }\n" +
                        "         )\n" +
                        "      ],\n" +
                        "      ~mainMapping : Pure {\n" +
                        "         ~src Sales\n" +
                        "         salesDate_nonExistent [b] : $src.salesDate,\n" +
                        "         revenue : $src.revenue\n" +
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
            Assert.assertEquals("Compilation error at (resource:mapping.pure line:59 column:10), \"The property 'salesDate_nonExistent' is unknown in the Element 'Sales'\"", e.getMessage());
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
                        "Class Sales_By_Date\n" +
                        "{\n" +
                        "   salesDate: FiscalCalendar[1];\n" +
                        "   netRevenue: Float[1];\n" +
                        "}\n" +
                        "function meta::pure::functions::math::sum(numbers:Float[*]):Float[1]\n" +
                        "{\n" +
                        "    $numbers->plus();\n" +
                        "}\n" +
                        "\n" +
                        "###Mapping\n" +
                        "Mapping map\n" +
                        "(\n" +
                        "   FiscalCalendar [b] : Pure {\n" +
                        "      ~src FiscalCalendar\n" +
                        "      date : $src.date,\n" +
                        "      fiscalYear : $src.fiscalYear,\n" +
                        "      fiscalMonth : $src.fiscalMonth,\n" +
                        "      fiscalQtr : $src.fiscalQtr\n" +
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
                        "            ~aggregateMapping : Pure {\n" +
                        "               ~src Sales_By_Date\n" +
                        "               salesDate [b] : $src.salesDate_NonExistent,\n" +
                        "               revenue : $src.netRevenue\n" +
                        "            }\n" +
                        "         )\n" +
                        "      ],\n" +
                        "      ~mainMapping : Pure {\n" +
                        "         ~src Sales\n" +
                        "         salesDate [b] : $src.salesDate,\n" +
                        "         revenue : $src.revenue\n" +
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
            Assert.assertEquals("Compilation error at (resource:mapping.pure line:52 column:37), \"Can't find the property 'salesDate_NonExistent' in the class Sales_By_Date\"", e.getMessage());
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
                        "\n" +
                        "Class Sales_By_Date\n" +
                        "{\n" +
                        "   salesDate: FiscalCalendar[1];\n" +
                        "   netRevenue: Float[1];\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::math::sum(numbers:Float[*]):Float[1]\n" +
                        "{\n" +
                        "    $numbers->plus();\n" +
                        "}\n" +
                        "###Mapping\n" +
                        "Mapping map\n" +
                        "(\n" +
                        "   FiscalCalendar [b] : Pure {\n" +
                        "      ~src FiscalCalendar\n" +
                        "      date : $src.date,\n" +
                        "      fiscalYear : $src.fiscalYear,\n" +
                        "      fiscalMonth : $src.fiscalMonth,\n" +
                        "      fiscalQtr : $src.fiscalQtr\n" +
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
                        "            ~aggregateMapping : Pure {\n" +
                        "               ~src Sales_By_Date\n" +
                        "               salesDate_NonExistent [b] : $src.salesDate,\n" +
                        "               revenue : $src.netRevenue\n" +
                        "            }\n" +
                        "         )\n" +
                        "      ],\n" +
                        "      ~mainMapping : Pure {\n" +
                        "         ~src Sales\n" +
                        "         salesDate [b] : $src.salesDate,\n" +
                        "         revenue : $src.revenue\n" +
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
            Assert.assertEquals("Compilation error at (resource:mapping.pure line:52 column:16), \"The property 'salesDate_NonExistent' is unknown in the Element 'Sales'\"", e.getMessage());
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
                        "Class Sales_By_Date\n" +
                        "{\n" +
                        "   salesDate: FiscalCalendar[1];\n" +
                        "   netRevenue: Float[1];\n" +
                        "}\n" +
                        "\n" +
                        "\n" +
                        "###Mapping\n" +
                        "Mapping map\n" +
                        "(\n" +
                        "   FiscalCalendar [b] : Pure {\n" +
                        "      ~src FiscalCalendar\n" +
                        "      date : $src.date,\n" +
                        "      fiscalYear : $src.fiscalYear,\n" +
                        "      fiscalMonth : $src.fiscalMonth,\n" +
                        "      fiscalQtr : $src.fiscalQtr\n" +
                        "   }\n" +
                        "   \n" +
                        "   Sales [a] : AggregationAware {\n" +
                        "      Views : [\n" +
                        "         (\n" +
                        "            ~modelOperation : {\n" +
                        "               ~canAggregate true,\n" +
                        "               ~groupByFunctions (\n" +
                        "                  $this.salesDate_NonExistent\n" +
                        "               ),\n" +
                        "               ~aggregateValues (\n" +
                        "                  ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->sum() )\n" +
                        "               )\n" +
                        "            },\n" +
                        "            ~aggregateMapping : Pure {\n" +
                        "               ~src Sales_By_Date\n" +
                        "               salesDate [b] : $src.salesDate,\n" +
                        "               revenue : $src.netRevenue\n" +
                        "            }\n" +
                        "         )\n" +
                        "      ],\n" +
                        "      ~mainMapping : Pure {\n" +
                        "         ~src Sales\n" +
                        "         salesDate [b] : $src.salesDate,\n" +
                        "         revenue : $src.revenue\n" +
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
            Assert.assertEquals("Compilation error at (resource:mapping.pure line:41 column:25), \"Can't find the property 'salesDate_NonExistent' in the class Sales\"", e.getMessage());
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
                        "Class Sales_By_Date\n" +
                        "{\n" +
                        "   salesDate: FiscalCalendar[1];\n" +
                        "   netRevenue: Float[1];\n" +
                        "}\n" +
                        "\n" +
                        "\n" +
                        "###Mapping\n" +
                        "Mapping map\n" +
                        "(\n" +
                        "   FiscalCalendar [b] : Pure {\n" +
                        "      ~src FiscalCalendar\n" +
                        "      date : $src.date,\n" +
                        "      fiscalYear : $src.fiscalYear,\n" +
                        "      fiscalMonth : $src.fiscalMonth,\n" +
                        "      fiscalQtr : $src.fiscalQtr\n" +
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
                        "            ~aggregateMapping : Pure {\n" +
                        "               ~src Sales_By_Date\n" +
                        "               salesDate [b] : $src.salesDate,\n" +
                        "               revenue : $src.netRevenue\n" +
                        "            }\n" +
                        "         )\n" +
                        "      ],\n" +
                        "      ~mainMapping : Pure {\n" +
                        "         ~src Sales\n" +
                        "         salesDate [b] : $src.salesDate,\n" +
                        "         revenue : $src.revenue\n" +
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
            Assert.assertEquals("Compilation error at (resource:mapping.pure line:44 column:67), \"The system can't find a match for the function: summation(_:Float[*])\"", e.getMessage());
        }
    }
}
