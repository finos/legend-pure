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

package org.finos.legend.pure.m2.relational.incremental;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m2.relational.AbstractPureRelationalTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.Test;


public class TestPureRuntimeAggregationAwareMapping extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final String model =
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
                    "native function sum(f:Float[*]):Float[1];\n";

    private static final String modelWithSalesPersonDimension =
            "###Pure\n" +
                    "Class Sales\n" +
                    "{\n" +
                    "   id: Integer[1];\n" +
                    "   salesDate: FiscalCalendar[1];\n" +
                    "   salesPerson: Person[1];\n" +
                    "   revenue: Float[1];\n" +
                    "}\n" +
                    "Class Person\n" +
                    "{\n" +
                    "   lastName: String[1];\n" +
                    "}\n" +
                    "\n" +
                    "Class FiscalCalendar\n" +
                    "{\n" +
                    "   date: Date[1];\n" +
                    "   fiscalYear: Integer[1];\n" +
                    "   fiscalMonth: Integer[1];\n" +
                    "   fiscalQtr: Integer[1];\n" +
                    "}\n"+
                    "native function sum(f:Float[*]):Float[1];\n";

    private static final String mapping = "###Relational\n" +
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

    private static final String mappingWithSalesPersonDimension = "###Relational\n" +
            "Database db \n" +
            "(\n" +
            "   Table sales_base (id INT PRIMARY KEY, sales_date DATE, revenue FLOAT, personID INT)\n" +
            "   Table calendar (date DATE PRIMARY KEY, fiscal_year INT, fiscal_qtr INT, fiscal_month INT)\n" +
            "   Table person(ID INT PRIMARY KEY, last_name VARCHAR(100))\n" +
            "   \n" +
            "   Table sales_by_date (sales_date DATE, net_revenue FLOAT)\n" +
            "   Table sales_by_qtr (sales_qtr_first_date DATE, net_revenue FLOAT)\n" +
            "   \n" +
            "   Join sales_calendar (sales_base.sales_date = calendar.date)\n" +
            "   Join sales_person (sales_base.personID = person.ID)\n" +
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
            "   Person [c] : Relational {\n" +
            "      scope([db]person)\n" +
            "      (\n" +
            "         lastName: last_name\n" +
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
            "                  salesPerson [p] : [db]@sales_date_calendar,\n" +
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

    private static final String mappingWithFunction = "###Relational\n" +
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
            "               ~canAggregate true,\n" +
            "               ~groupByFunctions (\n" +
            "                  $this.salesDate->myFunction()\n" +
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

    private static final String function = "function myFunction(d: FiscalCalendar[1]) : FiscalCalendar[1] {$d}";

    @Test
    public void testCreateAndDeleteAggregationAwareMapping() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("source1.pure", model).compile(),
                new RuntimeTestScriptBuilder().createInMemorySource("source2.pure", mapping).compile().deleteSource("source2.pure").compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testCreateAndDeleteNewPropertyInModel() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("source1.pure", model).createInMemorySource("source2.pure", mapping).compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source1.pure", modelWithSalesPersonDimension)
                        .compile()
                        .updateSource("source1.pure", model)
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testCreateAndDeleteNewPropertyAndMapping() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("source1.pure", model).createInMemorySource("source2.pure", mapping).compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source1.pure", modelWithSalesPersonDimension).updateSource("source2.pure", mappingWithSalesPersonDimension)
                        .compile()
                        .updateSource("source1.pure", model).updateSource("source2.pure", mapping)
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testDeleteFunctionUsedInAggregationAwareMapping() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("source1.pure", model).createInMemorySource("source2.pure", mappingWithFunction).createInMemorySource("source3.pure", function).compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source3.pure", "//" + function)
                        .compileWithExpectedCompileFailure("The system can't find a match for the function: myFunction(_:FiscalCalendar[1])", "source2.pure", 35, 36)
                        .updateSource("source3.pure", function)
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }
}
