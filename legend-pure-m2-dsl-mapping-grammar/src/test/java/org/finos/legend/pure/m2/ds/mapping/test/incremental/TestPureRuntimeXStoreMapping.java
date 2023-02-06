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
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.pure.m2.ds.mapping.test.AbstractPureMappingTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeXStoreMapping extends AbstractPureMappingTestWithCoreCompiled
{
    public static String model =
            "Class Firm\n" +
                    "{\n" +
                    "   legalName : String[1];\n" +
                    "}\n" +
                    "\n" +
                    "Class Person\n" +
                    "{\n" +
                    "   lastName : String[1];\n" +
                    "}\n" +
                    "\n" +
                    "Association Firm_Person\n" +
                    "{\n" +
                    "   firm : Firm[1];\n" +
                    "   employees : Person[*];\n" +
                    "}\n";

    public static String modelInheritanceSuper =
            "Class SuperFirm" +
                    "{" +
                    "   id : String[1];\n" +
                    "}";

    public static String modelInheritanceSuper2 =
            "Class SuperFirm" +
                    "{" +
                    "   id2 : String[1];\n" +
                    "}";

    public static String modelInheritance =
            "Class Firm extends SuperFirm\n" +
                    "{\n" +
                    "   legalName : String[1];\n" +
                    "}\n" +
                    "\n" +
                    "Class Person\n" +
                    "{\n" +
                    "   lastName : String[1];\n" +
                    "}\n" +
                    "\n" +
                    "Association Firm_Person\n" +
                    "{\n" +
                    "   firm : Firm[1];\n" +
                    "   employees : Person[*];\n" +
                    "}\n";

    public static String coreMapping =
            "   Firm[f1] : Pure\n" +
                    "   {\n" +
                    "~src SrcFirm" +
                    "      +id:String[1] : $src.id,\n" +
                    "      legalName : $src.legalName\n" +
                    "   }\n" +
                    "   \n" +
                    "   Person[e] : Pure\n" +
                    "   {\n" +
                    "~src SrcPerson" +
                    "      +firmId:String[1] : $src.firmId,\n" +
                    "      lastName : $src.lastName\n" +
                    "   }\n";

    public static String coreMappingInheritance =
            "   Firm[f1] : Pure\n" +
                    "   {\n" +
                    "~src SrcFirm" +
                    "      id : $src.id,\n" +
                    "      legalName : $src.legalName\n" +
                    "   }\n" +
                    "   \n" +
                    "   Person[e] : Pure\n" +
                    "   {\n" +
                    "~src SrcPerson" +
                    "      +firmId:String[1] : $src.firmId,\n" +
                    "      lastName : $src.lastName\n" +
                    "   }\n";

    public static String assoMapping =
            "   Firm_Person : XStore\n" +
                    "   {\n" +
                    "      firm[e, f1] : $this.firmId == $that.id,\n" +
                    "      employees[f1, e] : $this.id == $that.firmId\n" +
                    "   }\n";


    public static String initialMapping = "###Mapping\nMapping FirmMapping\n(" + coreMapping + ")";

    public static String mappingWithAssociation = "###Mapping\nMapping FirmMapping\n(" + coreMapping + assoMapping + ")\n";

    public static String baseMapping = "###Mapping\nMapping ModelMapping\n(" + coreMapping + ")\n";

    public static String inheritanceMapping = "###Mapping\nMapping ModelMapping\n(" + coreMappingInheritance + assoMapping + ")\n";

    public static String baseMappingEmpty = "###Mapping\nMapping ModelMapping\n()\n";

    public static String mainMapping = "###Mapping\nMapping FirmMapping\n(\ninclude ModelMapping\n" + assoMapping + ")\n";

    public static String relational =
            "###Pure\n" +
                    "Class SrcFirm" +
                    "{" +
                    "   id: String[1];" +
                    "   legalName : String[1];" +
                    "}\n" +
                    "Class SrcPerson" +
                    "{" +
                    "   firmId: String[1];" +
                    "   lastName : String[1];" +
                    "}\n";

    @BeforeClass
    public static void setUp() {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("source1.pure");
        runtime.delete("source3.pure");
        runtime.delete("source4.pure");
        runtime.delete("source5.pure");
    }

    @Test
    public void testCreateAndDeleteMappingProperties() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source1.pure", model)).compile(),
                new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source3.pure", initialMapping, "source4.pure", relational)).compile().deleteSource("source3.pure").deleteSource("source4.pure").compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testCreateAndDeleteAssoXStoreMapping() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source1.pure", model, "source3.pure", initialMapping, "source4.pure", relational)).compile(),
                new RuntimeTestScriptBuilder().updateSource("source3.pure", mappingWithAssociation).compile().updateSource("source3.pure", initialMapping).compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testCreateAndDeleteAssoXStoreMappingWithInclude() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source1.pure", model, "source3.pure", baseMapping, "source4.pure", relational, "source5.pure", mainMapping)).compile(),
                new RuntimeTestScriptBuilder().deleteSource("source5.pure").compile().createInMemorySource("source5.pure", mainMapping).compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testCreateAndDeleteAssoXStoreMappingErrorDeleteParent() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source1.pure", model, "source3.pure", baseMapping, "source4.pure", relational, "source5.pure", mainMapping)).compile(),
                new RuntimeTestScriptBuilder().updateSource("source3.pure", baseMappingEmpty).compileWithExpectedCompileFailure("Unable to find source class mapping (id:e) for property 'firm' in Association mapping 'Firm_Person'. Make sure that you have specified a valid Class mapping id as the source id and target id, using the syntax 'property[sourceId, targetId]: ...'.", null, 7, 7).updateSource("source3.pure", baseMapping).compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testCreateAndDeleteAssoXStoreMappingErrorDeleteSuperType() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source1.pure", modelInheritance, "source3.pure", modelInheritanceSuper, "source4.pure", relational, "source5.pure", inheritanceMapping)).compile(),
                new RuntimeTestScriptBuilder().updateSource("source3.pure", modelInheritanceSuper2).compileWithExpectedCompileFailure("The property 'id' is unknown in the Element 'Firm'", null, 5, 19).updateSource("source3.pure", modelInheritanceSuper).compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }
}
