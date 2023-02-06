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

package org.finos.legend.pure.m2.ds.mapping.test.incremental;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m2.ds.mapping.test.AbstractPureMappingTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;


public class TestPureRuntimeAggregationAwareMapping extends AbstractPureMappingTestWithCoreCompiled
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
                    "\n" +
                    "Class Sales_By_Date\n" +
                    "{\n" +
                    "   salesDate: FiscalCalendar[1];\n" +
                    "   netRevenue: Float[1];\n" +
                    "}\n"+
                    "function meta::pure::functions::math::sum(numbers:Float[*]):Float[1]\n" +
                    "{\n" +
                    "    $numbers->plus();\n" +
                    "}\n";

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
                    "}\n" +
                    "\n" +
                    "Class Sales_By_Date\n" +
                    "{\n" +
                    "   salesDate: FiscalCalendar[1];\n" +
                    "   netRevenue: Float[1];\n" +
                    "}"+
                    "function meta::pure::functions::math::sum(numbers:Float[*]):Float[1]\n" +
                    "{\n" +
                    "    $numbers->plus();\n" +
                    "}\n";

    private static final String mapping = "###Mapping\n" +
            "Mapping map\n" +
            "(\n" +
            "   FiscalCalendar : Pure {\n" +
            "      ~src FiscalCalendar\n" +
            "      date : $src.date,\n" +
            "      fiscalYear : $src.fiscalYear,\n" +
            "      fiscalMonth : $src.fiscalMonth,\n" +
            "      fiscalQtr : $src.fiscalQtr\n" +
            "   }\n" +
            "   \n" +
            "   Sales : AggregationAware {\n" +
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
            "               salesDate : $src.salesDate,\n" +
            "               revenue : $src.netRevenue\n" +
            "            }\n" +
            "         )\n" +
            "      ],\n" +
            "      ~mainMapping : Pure {\n" +
            "         ~src Sales\n" +
            "         salesDate : $src.salesDate,\n" +
            "         revenue : $src.revenue\n" +
            "      }\n" +
            "   }\n" +
            ")";

    private static final String mappingWithSalesPersonDimension = "###Mapping\n" +
            "Mapping map\n" +
            "(\n" +
            "   FiscalCalendar : Pure {\n" +
            "      ~src FiscalCalendar\n" +
            "      date : $src.date,\n" +
            "      fiscalYear : $src.fiscalYear,\n" +
            "      fiscalMonth : $src.fiscalMonth,\n" +
            "      fiscalQtr : $src.fiscalQtr\n" +
            "   }\n" +
            "   Person : Pure {\n" +
            "      ~src Person\n" +
            "      lastName : $src.lastName\n" +
            "   }\n" +
            "   \n" +
            "   Sales : AggregationAware {\n" +
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
            "               salesDate : $src.salesDate,\n" +
            "               revenue : $src.netRevenue\n" +
            "            }\n" +
            "         )\n" +
            "      ],\n" +
            "      ~mainMapping : Pure {\n" +
            "         ~src Sales\n" +
            "         salesDate : $src.salesDate,\n" +
            "         salesPerson : $src.salesPerson,\n" +
            "         revenue : $src.revenue\n" +
            "      }\n" +
            "   }\n" +
            ")";

    private static final String mappingWithFunction = "###Mapping\n" +
            "Mapping map\n" +
            "(\n" +
            "   FiscalCalendar : Pure {\n" +
            "      ~src FiscalCalendar\n" +
            "      date : $src.date,\n" +
            "      fiscalYear : $src.fiscalYear,\n" +
            "      fiscalMonth : $src.fiscalMonth,\n" +
            "      fiscalQtr : $src.fiscalQtr\n" +
            "   }\n" +
            "   \n" +
            "   Sales : AggregationAware {\n" +
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
            "            ~aggregateMapping : Pure {\n" +
            "               ~src Sales_By_Date\n" +
            "               salesDate : $src.salesDate,\n" +
            "               revenue : $src.netRevenue\n" +
            "            }\n" +
            "         )\n" +
            "      ],\n" +
            "      ~mainMapping : Pure {\n" +
            "         ~src Sales\n" +
            "         salesDate : $src.salesDate,\n" +
            "         revenue : $src.revenue\n" +
            "      }\n" +
            "   }\n" +
            ")";

    private static final String function = "function myFunction(d: FiscalCalendar[1]) : FiscalCalendar[1] {$d}";

    @BeforeClass
    public static void setUp() {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("source1.pure");
        runtime.delete("source2.pure");
        runtime.delete("source3.pure");
    }

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
                        .compileWithExpectedCompileFailure("The system can't find a match for the function: myFunction(_:FiscalCalendar[1])", "source2.pure", 18, 36)
                        .updateSource("source3.pure", function)
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }
}
