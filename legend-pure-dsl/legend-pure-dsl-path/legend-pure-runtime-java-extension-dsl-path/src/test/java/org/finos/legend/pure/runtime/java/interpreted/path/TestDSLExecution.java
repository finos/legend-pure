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

package org.finos.legend.pure.runtime.java.interpreted.path;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestDSLExecution extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @Test
    public void testSimple() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", "Class Person{address:Address[1];} Class Firm<T> {employees : Person[1];address:Address[1];} Class Address{}\n" +
                               "function test():Any[*]\n" +
                               "{\n" +
                               "    print(#/Firm<Any>/employees/address#, 2);\n" +
                               "}\n");
        this.runtime.compile();
        this.execute("test():Any[*]");
        Assert.assertEquals("Anonymous_StripedId instance Path\n" +
                "    classifierGenericType(Property):\n" +
                "        Anonymous_StripedId instance GenericType\n" +
                "            multiplicityArguments(Property):\n" +
                "                [X] PureOne instance PackageableMultiplicity\n" +
                "            rawType(Property):\n" +
                "                [X] Path instance Class\n" +
                "            typeArguments(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        [~>] Firm instance Class\n" +
                "                    typeArguments(Property):\n" +
                "                        [>2] Anonymous_StripedId instance GenericType\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        [~>] Address instance Class\n" +
                "    name(Property):\n" +
                "         instance String\n" +
                "    path(Property):\n" +
                "        Anonymous_StripedId instance PropertyPathElement\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        [X] PropertyPathElement instance Class\n" +
                "            property(Property):\n" +
                "                Anonymous_StripedId instance PropertyStub\n" +
                "                    owner(Property):\n" +
                "                        [X] Firm instance Class\n" +
                "                    propertyName(Property):\n" +
                "                        [>2] employees instance String\n" +
                "                    resolvedProperty(Property):\n" +
                "                        [>2] employees instance Property\n" +
                "        Anonymous_StripedId instance PropertyPathElement\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        [X] PropertyPathElement instance Class\n" +
                "            property(Property):\n" +
                "                Anonymous_StripedId instance PropertyStub\n" +
                "                    owner(Property):\n" +
                "                        [X] Person instance Class\n" +
                "                    propertyName(Property):\n" +
                "                        [>2] address instance String\n" +
                "                    resolvedProperty(Property):\n" +
                "                        [>2] address instance Property\n" +
                "    referenceUsages(Property):\n" +
                "        Anonymous_StripedId instance ReferenceUsage\n" +
                "            offset(Property):\n" +
                "                0 instance Integer\n" +
                "            owner(Property):\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    genericType(Property):\n" +
                "                        [>2] Anonymous_StripedId instance GenericType\n" +
                "                    multiplicity(Property):\n" +
                "                        [X] PureOne instance PackageableMultiplicity\n" +
                "                    usageContext(Property):\n" +
                "                        [>2] Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                    values(Property):\n" +
                "                        [>2] Anonymous_StripedId instance Path\n" +
                "            propertyName(Property):\n" +
                "                values instance String\n" +
                "    start(Property):\n" +
                "        Anonymous_StripedId instance GenericType\n" +
                "            rawType(Property):\n" +
                "                [~>] Firm instance Class\n" +
                "            referenceUsages(Property):\n" +
                "                Anonymous_StripedId instance ReferenceUsage\n" +
                "                    offset(Property):\n" +
                "                        [>2] 0 instance Integer\n" +
                "                    owner(Property):\n" +
                "                        [>2] Anonymous_StripedId instance Path\n" +
                "                    propertyName(Property):\n" +
                "                        [>2] start instance String\n" +
                "            typeArguments(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        [~>] Any instance Class\n" +
                "                    referenceUsages(Property):\n" +
                "                        [>2] Anonymous_StripedId instance ReferenceUsage", this.functionExecution.getConsole().getLine(0));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
