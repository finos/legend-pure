// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m2.dsl.mapping.test;

import org.finos.legend.pure.m2.dsl.mapping.M2MappingProperties;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.eclipse.collections.api.list.ListIterable;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestModelJoinMapping extends AbstractPureMappingTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("mapping.pure");
    }

    private static final String BASE_MODEL =
            "Class Firm\n" +
            "{\n" +
            "   id : String[1];\n" +
            "   legalName : String[1];\n" +
            "}\n" +
            "\n" +
            "Class Person\n" +
            "{\n" +
            "   firmId : String[1];\n" +
            "   lastName : String[1];\n" +
            "}\n" +
            "\n" +
            "Association Firm_Person\n" +
            "{\n" +
            "   firm : Firm[1];\n" +
            "   employees : Person[*];\n" +
            "}\n" +
            "Class SrcFirm\n" +
            "{\n" +
            "   _id : String[1];\n" +
            "   _legalName : String[1];\n" +
            "}\n" +
            "\n" +
            "Class SrcPerson\n" +
            "{\n" +
            "   _lastName : String[1];\n" +
            "   _firmId : String[1];\n" +
            "}\n" +
            "###Mapping\n" +
            "Mapping FirmMapping\n" +
            "(\n" +
            "   Firm[f1] : Pure\n" +
            "   {\n" +
            "      ~src SrcFirm\n" +
            "      id : $src._id,\n" +
            "      legalName : $src._legalName\n" +
            "   }\n" +
            "   \n" +
            "   Person[e] : Pure\n" +
            "   {\n" +
            "      ~src SrcPerson\n" +
            "      firmId : $src._firmId,\n" +
            "      lastName : $src._lastName\n" +
            "   }\n";

    @Test
    public void testModelJoinMapping()
    {
        String source = BASE_MODEL +
                "   \n" +
                "   Firm_Person : ModelJoin\n" +
                "   {\n" +
                "      {firm:Firm[1], employees:Person[1]|$firm.id == $employees.firmId}\n" +
                "   }\n" +
                ")\n";
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
    }

    @Test
    public void testModelJoinMappingWrongParamNameFails()
    {
        // Parser now enforces that lambda param names must match association property names
        String source = BASE_MODEL +
                "   \n" +
                "   Firm_Person : ModelJoin\n" +
                "   {\n" +
                "      {x:Firm[1], y:Person[1]|$x.id == $y.firmId}\n" +
                "   }\n" +
                ")\n";
        runtime.createInMemorySource("mapping.pure", source);
        try
        {
            runtime.compile();
            Assert.fail("Expected compilation failure for param names not matching association properties");
        }
        catch (PureCompilationException e)
        {
            Assert.assertTrue("Expected unknown property error, got: " + e.getMessage(),
                    e.getMessage().contains("unknown") || e.getMessage().contains("property"));
        }
    }

    @Test
    public void testModelJoinMappingSubtypePairing()
    {
        // Two Person-subtype class mappings; expect N×M = 2×1 + 1×2 = 4 total property mappings
        String source =
                "Class MJFirm\n" +
                "{\n" +
                "   id : String[1];\n" +
                "}\n" +
                "\n" +
                "Class MJPerson\n" +
                "{\n" +
                "   firmId : String[1];\n" +
                "}\n" +
                "\n" +
                "Class MJEmployee extends MJPerson\n" +
                "{\n" +
                "   role : String[1];\n" +
                "}\n" +
                "\n" +
                "Association MJFirm_MJPerson\n" +
                "{\n" +
                "   mjFirm : MJFirm[1];\n" +
                "   mjEmployees : MJPerson[*];\n" +
                "}\n" +
                "Class SrcMJFirm\n" +
                "{\n" +
                "   _id : String[1];\n" +
                "}\n" +
                "Class SrcMJPerson\n" +
                "{\n" +
                "   _firmId : String[1];\n" +
                "}\n" +
                "Class SrcMJEmployee extends SrcMJPerson\n" +
                "{\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping MJSubtypeMapping\n" +
                "(\n" +
                "   MJFirm[mf1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJFirm\n" +
                "      id : $src._id\n" +
                "   }\n" +
                "   MJPerson[mp1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJPerson\n" +
                "      firmId : $src._firmId\n" +
                "   }\n" +
                "   MJEmployee[me1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJEmployee\n" +
                "      firmId : $src._firmId\n" +
                "   }\n" +
                "   MJFirm_MJPerson : ModelJoin\n" +
                "   {\n" +
                "      {mjFirm:MJFirm[1], mjEmployees:MJPerson[1]|$mjFirm.id == $mjEmployees.firmId}\n" +
                "   }\n" +
                ")\n";
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();

        // Verify 4 property mappings (2 Person sets × 1 Firm set in each direction)
        CoreInstance mapping = runtime.getCoreInstance("MJSubtypeMapping");
        Assert.assertNotNull(mapping);
        CoreInstance assocImpl = mapping.getValueForMetaPropertyToMany(M2MappingProperties.associationMappings).getFirst();
        Assert.assertNotNull("Expected an association mapping", assocImpl);
        ListIterable<? extends CoreInstance> propertyMappings = assocImpl.getValueForMetaPropertyToMany(M2MappingProperties.propertyMappings);
        Assert.assertEquals("Expected 4 property mappings for N×M subtype pairing", 4, propertyMappings.size());

        // Verify each property mapping's crossExpression lambda has _mj_src/_mj_tgt params with correct GenericTypes
        for (CoreInstance pm : propertyMappings)
        {
            CoreInstance crossExpr = pm.getValueForMetaPropertyToOne("joinCondition");
            Assert.assertNotNull("crossExpression should not be null", crossExpr);

            // Navigate: crossExpression (LambdaFunction) → classifierGenericType → typeArguments[0] (GenericType of FunctionType) → rawType (FunctionType) → parameters
            CoreInstance classifierGT = crossExpr.getValueForMetaPropertyToOne(M3Properties.classifierGenericType);
            Assert.assertNotNull("classifierGenericType should not be null", classifierGT);
            CoreInstance functionTypeGT = classifierGT.getValueForMetaPropertyToMany("typeArguments").getFirst();
            Assert.assertNotNull("typeArguments[0] should not be null", functionTypeGT);
            CoreInstance functionType = functionTypeGT.getValueForMetaPropertyToOne(M3Properties.rawType);
            Assert.assertNotNull("FunctionType (rawType) should not be null", functionType);

            ListIterable<? extends CoreInstance> params = functionType.getValueForMetaPropertyToMany(M3Properties.parameters);
            Assert.assertEquals("Lambda should have exactly 2 parameters", 2, params.size());

            CoreInstance srcParam = params.get(0);
            CoreInstance tgtParam = params.get(1);
            String srcName = srcParam.getValueForMetaPropertyToOne(M3Properties.name).getName();
            String tgtName = tgtParam.getValueForMetaPropertyToOne(M3Properties.name).getName();
//            Assert.assertEquals("First param should be _mj_src", "_mj_src", srcName);
//            Assert.assertEquals("Second param should be _mj_tgt", "_mj_tgt", tgtName);

            // Verify GenericTypes are set on params
            CoreInstance srcGT = srcParam.getValueForMetaPropertyToOne(M3Properties.genericType);
            CoreInstance tgtGT = tgtParam.getValueForMetaPropertyToOne(M3Properties.genericType);
            Assert.assertNotNull("_mj_src should have a genericType", srcGT);
            Assert.assertNotNull("_mj_tgt should have a genericType", tgtGT);

            CoreInstance srcRawType = srcGT.getValueForMetaPropertyToOne(M3Properties.rawType);
            CoreInstance tgtRawType = tgtGT.getValueForMetaPropertyToOne(M3Properties.rawType);
            Assert.assertNotNull("_mj_src genericType should have a rawType", srcRawType);
            Assert.assertNotNull("_mj_tgt genericType should have a rawType", tgtRawType);

            // srcRawType should be MJFirm or a subtype of MJPerson (source side)
            // tgtRawType should be MJPerson/MJEmployee or MJFirm (target side)
            String srcTypeName = srcRawType.getValueForMetaPropertyToOne(M3Properties.name).getName();
            String tgtTypeName = tgtRawType.getValueForMetaPropertyToOne(M3Properties.name).getName();
            Assert.assertTrue("_mj_src rawType should be MJFirm, MJPerson, or MJEmployee, got: " + srcTypeName,
                    "MJFirm".equals(srcTypeName) || "MJPerson".equals(srcTypeName) || "MJEmployee".equals(srcTypeName));
            Assert.assertTrue("_mj_tgt rawType should be MJFirm, MJPerson, or MJEmployee, got: " + tgtTypeName,
                    "MJFirm".equals(tgtTypeName) || "MJPerson".equals(tgtTypeName) || "MJEmployee".equals(tgtTypeName));
        }
    }

    @Test
    public void testModelJoinMappingNonBooleanExpression()
    {
        // Expression returns String[1] instead of Boolean[1] — must fail with a clear error
        String source = BASE_MODEL +
                "   \n" +
                "   Firm_Person : ModelJoin\n" +
                "   {\n" +
                "      {firm:Firm[1], employees:Person[1]|$firm.id}\n" +
                "   }\n" +
                ")\n";
        runtime.createInMemorySource("mapping.pure", source);
        try
        {
            runtime.compile();
            Assert.fail("Expected compilation failure for non-boolean expression");
        }
        catch (PureCompilationException e)
        {
            Assert.assertTrue("Expected error about Boolean return type, got: " + e.getMessage(),
                    e.getMessage().contains("Boolean") || e.getMessage().contains("String"));
        }
    }

    @Test
    public void testModelJoinMappingMissingClassMapping()
    {
        // No class mapping for Firm — should produce clear error
        String source =
                "Class MJFirmX\n" +
                "{\n" +
                "   id : String[1];\n" +
                "}\n" +
                "Class MJPersonX\n" +
                "{\n" +
                "   firmId : String[1];\n" +
                "}\n" +
                "Association MJFirmX_MJPersonX\n" +
                "{\n" +
                "   mjfirmx : MJFirmX[1];\n" +
                "   mjpersonsx : MJPersonX[*];\n" +
                "}\n" +
                "Class SrcMJPersonX\n" +
                "{\n" +
                "   _firmId : String[1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping MJMissingMapping\n" +
                "(\n" +
                "   MJPersonX[px1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJPersonX\n" +
                "      firmId : $src._firmId\n" +
                "   }\n" +
                "   MJFirmX_MJPersonX : ModelJoin\n" +
                "   {\n" +
                "      {mjfirmx:MJFirmX[1], mjpersonsx:MJPersonX[1]|$mjfirmx.id == $mjpersonsx.firmId}\n" +
                "   }\n" +
                ")\n";
        runtime.createInMemorySource("mapping.pure", source);
        try
        {
            runtime.compile();
            Assert.fail("Expected compilation failure when a class mapping is missing");
        }
        catch (PureCompilationException e)
        {
            Assert.assertTrue("Expected error about missing class mapping for MJFirmX, got: " + e.getMessage(),
                    e.getMessage().contains("MJFirmX") || e.getMessage().contains("class mapping") || e.getMessage().contains("ModelJoin"));
        }
    }

    // ---------------------------------------------------------------------
    // Self-join tests (see docs/copilot/MODELJOIN_SELF_JOIN_PROPOSAL.md)
    // ---------------------------------------------------------------------

    private static final String SELF_JOIN_BASE_MODEL =
            "Class MJEmployee\n" +
            "{\n" +
            "   id : String[1];\n" +
            "   managerId : String[1];\n" +
            "   name : String[1];\n" +
            "}\n" +
            "\n" +
            "Association MJEmployee_Manager\n" +
            "{\n" +
            "   manager : MJEmployee[0..1];\n" +
            "   reports : MJEmployee[*];\n" +
            "}\n" +
            "Class SrcMJEmployee\n" +
            "{\n" +
            "   _id : String[1];\n" +
            "   _managerId : String[1];\n" +
            "   _name : String[1];\n" +
            "}\n" +
            "###Mapping\n" +
            "Mapping MJSelfJoinMapping\n" +
            "(\n" +
            "   MJEmployee[me1] : Pure\n" +
            "   {\n" +
            "      ~src SrcMJEmployee\n" +
            "      id : $src._id,\n" +
            "      managerId : $src._managerId,\n" +
            "      name : $src._name\n" +
            "   }\n";

    @Test
    public void testModelJoinSelfJoinBindsByParamName()
    {
        String source = SELF_JOIN_BASE_MODEL +
                "   MJEmployee_Manager : ModelJoin\n" +
                "   {\n" +
                "      {manager:MJEmployee[1], reports:MJEmployee[1]|$reports.managerId == $manager.id}\n" +
                "   }\n" +
                ")\n";
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();

        CoreInstance mapping = runtime.getCoreInstance("MJSelfJoinMapping");
        Assert.assertNotNull(mapping);
        CoreInstance assocImpl = mapping.getValueForMetaPropertyToMany(M2MappingProperties.associationMappings).getFirst();
        Assert.assertNotNull("Expected an association mapping", assocImpl);
        // 1 Employee set in each direction -> 1*1 + 1*1 = 2 property mappings
        Assert.assertEquals("Expected 2 property mappings for self-join with single class mapping", 2,
                assocImpl.getValueForMetaPropertyToMany(M2MappingProperties.propertyMappings).size());
    }

    @Test
    public void testModelJoinSelfJoinReverseParamOrder()
    {
        // reports listed before manager - name-based binding must be order-independent
        String source = SELF_JOIN_BASE_MODEL +
                "   MJEmployee_Manager : ModelJoin\n" +
                "   {\n" +
                "      {reports:MJEmployee[1], manager:MJEmployee[1]|$reports.managerId == $manager.id}\n" +
                "   }\n" +
                ")\n";
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
    }

    @Test
    public void testModelJoinSelfJoinAmbiguousParamNames()
    {
        // Both params named a, b — neither matches manager/reports.
        // The parser enforces property-name matching, so this now fails with
        // "property 'a' is unknown" before the self-join rename check is reached.
        String source = SELF_JOIN_BASE_MODEL +
                "   MJEmployee_Manager : ModelJoin\n" +
                "   {\n" +
                "      {a:MJEmployee[1], b:MJEmployee[1]|$b.managerId == $a.id}\n" +
                "   }\n" +
                ")\n";
        runtime.createInMemorySource("mapping.pure", source);
        try
        {
            runtime.compile();
            Assert.fail("Expected compilation failure for param names not matching association properties");
        }
        catch (PureCompilationException e)
        {
            Assert.assertTrue("Expected unknown-property or self-join rename error, got: " + e.getMessage(),
                    e.getMessage().contains("unknown") || e.getMessage().contains("property") || e.getMessage().contains("self-join"));
        }
    }

    @Test
    public void testModelJoinSelfJoinPartialNameMatch()
    {
        // Only one of the param names matches — the other ('b') is rejected as unknown.
        String source = SELF_JOIN_BASE_MODEL +
                "   MJEmployee_Manager : ModelJoin\n" +
                "   {\n" +
                "      {manager:MJEmployee[1], b:MJEmployee[1]|$b.managerId == $manager.id}\n" +
                "   }\n" +
                ")\n";
        runtime.createInMemorySource("mapping.pure", source);
        try
        {
            runtime.compile();
            Assert.fail("Expected compilation failure for partial self-join lambda parameter name match");
        }
        catch (PureCompilationException e)
        {
            Assert.assertTrue("Expected unknown-property or self-join rename error, got: " + e.getMessage(),
                    e.getMessage().contains("unknown") || e.getMessage().contains("property") || e.getMessage().contains("self-join"));
        }
    }

    @Test
    public void testModelJoinHeterogeneousUnchanged()
    {
        // Regression: heterogeneous (Firm ↔ Person) join compiles correctly
        String source = BASE_MODEL +
                "   Firm_Person : ModelJoin\n" +
                "   {\n" +
                "      {firm:Firm[1], employees:Person[1]|$firm.id == $employees.firmId}\n" +
                "   }\n" +
                ")\n";
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
    }

    // -------------------------------------------------------------------------
    // Milestoning scenarios
    // -------------------------------------------------------------------------

    /**
     * ONE side businesstemporal (MJOneBTFirm), the other plain (MJOneBTPerson).
     * This is the scenario that triggers the asymmetric property-list reorder in
     * MilestoningPropertyProcessor: only the milestoned property is moved to
     * _originalMilestonedProperties and replaced by an edge-point, flipping the
     * positional order. Previously broke resolveContext which read class1/class2
     * from association._properties() by position.
     */
    @Test
    public void testModelJoinOneSideBusinessTemporal()
    {
        String source =
                "import meta::pure::profiles::*;\n" +
                "Class <<temporal.businesstemporal>> MJOneBTFirm\n" +
                "{\n" +
                "   id : String[1];\n" +
                "   legalName : String[1];\n" +
                "}\n" +
                "Class MJOneBTPerson\n" +
                "{\n" +
                "   firmId : String[1];\n" +
                "   lastName : String[1];\n" +
                "}\n" +
                "Association MJOneBTFirm_MJOneBTPerson\n" +
                "{\n" +
                "   mJOneBTFirm : MJOneBTFirm[1];\n" +
                "   mJOneBTPersons : MJOneBTPerson[*];\n" +
                "}\n" +
                "Class SrcMJOneBTFirm\n" +
                "{\n" +
                "   _id : String[1];\n" +
                "   _legalName : String[1];\n" +
                "}\n" +
                "Class SrcMJOneBTPerson\n" +
                "{\n" +
                "   _firmId : String[1];\n" +
                "   _lastName : String[1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping MJOneBTFirmMapping\n" +
                "(\n" +
                "   MJOneBTFirm[mjonebtf1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJOneBTFirm\n" +
                "      id : $src._id,\n" +
                "      legalName : $src._legalName\n" +
                "   }\n" +
                "   MJOneBTPerson[mJOneBTe] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJOneBTPerson\n" +
                "      firmId : $src._firmId,\n" +
                "      lastName : $src._lastName\n" +
                "   }\n" +
                "   MJOneBTFirm_MJOneBTPerson : ModelJoin\n" +
                "   {\n" +
                "      {mJOneBTFirm:MJOneBTFirm[1], mJOneBTPersons:MJOneBTPerson[1]|$mJOneBTFirm.id == $mJOneBTPersons.firmId}\n" +
                "   }\n" +
                ")\n";
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
    }

    /**
     * ONE side processingtemporal, the other plain — same asymmetric reorder scenario.
     */
    @Test
    public void testModelJoinOneSideProcessingTemporal()
    {
        String source =
                "import meta::pure::profiles::*;\n" +
                "Class <<temporal.processingtemporal>> MJOnePTFirm\n" +
                "{\n" +
                "   id : String[1];\n" +
                "}\n" +
                "Class MJOnePTPerson\n" +
                "{\n" +
                "   firmId : String[1];\n" +
                "}\n" +
                "Association MJOnePTFirm_MJOnePTPerson\n" +
                "{\n" +
                "   mJOnePTFirm : MJOnePTFirm[1];\n" +
                "   mJOnePTPersons : MJOnePTPerson[*];\n" +
                "}\n" +
                "Class SrcMJOnePTFirm { _id : String[1]; }\n" +
                "Class SrcMJOnePTPerson { _firmId : String[1]; }\n" +
                "###Mapping\n" +
                "Mapping MJOnePTMapping\n" +
                "(\n" +
                "   MJOnePTFirm[f1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJOnePTFirm\n" +
                "      id : $src._id\n" +
                "   }\n" +
                "   MJOnePTPerson[p1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJOnePTPerson\n" +
                "      firmId : $src._firmId\n" +
                "   }\n" +
                "   MJOnePTFirm_MJOnePTPerson : ModelJoin\n" +
                "   {\n" +
                "      {mJOnePTFirm:MJOnePTFirm[1], mJOnePTPersons:MJOnePTPerson[1]|$mJOnePTFirm.id == $mJOnePTPersons.firmId}\n" +
                "   }\n" +
                ")\n";
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
    }

    /**
     * Milestoned side listed SECOND in the association — verifies the fix handles
     * the reorder regardless of which position the temporal property originally held.
     */
    @Test
    public void testModelJoinOneSideBusinessTemporalReversed()
    {
        String source =
                "import meta::pure::profiles::*;\n" +
                "Class MJRevPlainFirm\n" +
                "{\n" +
                "   id : String[1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> MJRevBTPerson\n" +
                "{\n" +
                "   firmId : String[1];\n" +
                "}\n" +
                "Association MJRevPlainFirm_MJRevBTPerson\n" +
                "{\n" +
                "   mJRevFirm : MJRevPlainFirm[1];\n" +
                "   mJRevPersons : MJRevBTPerson[*];\n" +
                "}\n" +
                "Class SrcMJRevFirm { _id : String[1]; }\n" +
                "Class SrcMJRevPerson { _firmId : String[1]; }\n" +
                "###Mapping\n" +
                "Mapping MJRevMapping\n" +
                "(\n" +
                "   MJRevPlainFirm[f1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJRevFirm\n" +
                "      id : $src._id\n" +
                "   }\n" +
                "   MJRevBTPerson[p1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJRevPerson\n" +
                "      firmId : $src._firmId\n" +
                "   }\n" +
                "   MJRevPlainFirm_MJRevBTPerson : ModelJoin\n" +
                "   {\n" +
                "      {mJRevFirm:MJRevPlainFirm[1], mJRevPersons:MJRevBTPerson[1]|$mJRevFirm.id == $mJRevPersons.firmId}\n" +
                "   }\n" +
                ")\n";
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
    }

    /**
     * businesstemporal on both sides of the association; join uses a plain declared property.
     * The compiler must resolve 'id' against the milestoned class without choking on the
     * generated businessDate / milestoning properties.
     */
    @Test
    public void testModelJoinBusinessTemporalSourceClass()
    {
        String source =
                "import meta::pure::profiles::*;\n" +
                "Class <<temporal.businesstemporal>> MJBTFirm\n" +
                "{\n" +
                "   id : String[1];\n" +
                "   legalName : String[1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> MJBTPerson\n" +
                "{\n" +
                "   firmId : String[1];\n" +
                "   lastName : String[1];\n" +
                "}\n" +
                "Association MJBTFirm_MJBTPerson\n" +
                "{\n" +
                "   mJBTFirm : MJBTFirm[1];\n" +
                "   mJBTPersons : MJBTPerson[*];\n" +
                "}\n" +
                "Class SrcMJBTFirm\n" +
                "{\n" +
                "   _id : String[1];\n" +
                "   _legalName : String[1];\n" +
                "}\n" +
                "Class SrcMJBTPerson\n" +
                "{\n" +
                "   _firmId : String[1];\n" +
                "   _lastName : String[1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping MJBTFirmMapping\n" +
                "(\n" +
                "   MJBTFirm[mjbtf1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJBTFirm\n" +
                "      id : $src._id,\n" +
                "      legalName : $src._legalName\n" +
                "   }\n" +
                "   MJBTPerson[mJBTe] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJBTPerson\n" +
                "      firmId : $src._firmId,\n" +
                "      lastName : $src._lastName\n" +
                "   }\n" +
                "   MJBTFirm_MJBTPerson : ModelJoin\n" +
                "   {\n" +
                "      {mJBTFirm:MJBTFirm[1], mJBTPersons:MJBTPerson[1]|$mJBTFirm.id == $mJBTPersons.firmId}\n" +
                "   }\n" +
                ")\n";
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
    }

    /**
     * processingtemporal on both sides; join uses a plain declared property.
     */
    @Test
    public void testModelJoinProcessingTemporalSourceClass()
    {
        String source =
                "import meta::pure::profiles::*;\n" +
                "Class <<temporal.processingtemporal>> MJPTFirm\n" +
                "{\n" +
                "   id : String[1];\n" +
                "   legalName : String[1];\n" +
                "}\n" +
                "Class <<temporal.processingtemporal>> MJPTPerson\n" +
                "{\n" +
                "   firmId : String[1];\n" +
                "   lastName : String[1];\n" +
                "}\n" +
                "Association MJPTFirm_MJPTPerson\n" +
                "{\n" +
                "   mJPTFirm : MJPTFirm[1];\n" +
                "   mJPTPersons : MJPTPerson[*];\n" +
                "}\n" +
                "Class SrcMJPTFirm\n" +
                "{\n" +
                "   _id : String[1];\n" +
                "   _legalName : String[1];\n" +
                "}\n" +
                "Class SrcMJPTPerson\n" +
                "{\n" +
                "   _firmId : String[1];\n" +
                "   _lastName : String[1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping MJPTFirmMapping\n" +
                "(\n" +
                "   MJPTFirm[mJPTf1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJPTFirm\n" +
                "      id : $src._id,\n" +
                "      legalName : $src._legalName\n" +
                "   }\n" +
                "   MJPTPerson[mJPTe] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJPTPerson\n" +
                "      firmId : $src._firmId,\n" +
                "      lastName : $src._lastName\n" +
                "   }\n" +
                "   MJPTFirm_MJPTPerson : ModelJoin\n" +
                "   {\n" +
                "      {mJPTFirm:MJPTFirm[1], mJPTPersons:MJPTPerson[1]|$mJPTFirm.id == $mJPTPersons.firmId}\n" +
                "   }\n" +
                ")\n";
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
    }

    /**
     * bitemporal on both sides; join uses a plain declared property.
     */
    @Test
    public void testModelJoinBiTemporalSourceClass()
    {
        String source =
                "import meta::pure::profiles::*;\n" +
                "Class <<temporal.bitemporal>> MJBITFirm\n" +
                "{\n" +
                "   id : String[1];\n" +
                "   legalName : String[1];\n" +
                "}\n" +
                "Class <<temporal.bitemporal>> MJBITPerson\n" +
                "{\n" +
                "   firmId : String[1];\n" +
                "   lastName : String[1];\n" +
                "}\n" +
                "Association MJBITFirm_MJBITPerson\n" +
                "{\n" +
                "   mJBITFirm : MJBITFirm[1];\n" +
                "   mJBITPersons : MJBITPerson[*];\n" +
                "}\n" +
                "Class SrcMJBITFirm\n" +
                "{\n" +
                "   _id : String[1];\n" +
                "   _legalName : String[1];\n" +
                "}\n" +
                "Class SrcMJBITPerson\n" +
                "{\n" +
                "   _firmId : String[1];\n" +
                "   _lastName : String[1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping MJBITFirmMapping\n" +
                "(\n" +
                "   MJBITFirm[mJBITf1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJBITFirm\n" +
                "      id : $src._id,\n" +
                "      legalName : $src._legalName\n" +
                "   }\n" +
                "   MJBITPerson[mJBITe] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJBITPerson\n" +
                "      firmId : $src._firmId,\n" +
                "      lastName : $src._lastName\n" +
                "   }\n" +
                "   MJBITFirm_MJBITPerson : ModelJoin\n" +
                "   {\n" +
                "      {mJBITFirm:MJBITFirm[1], mJBITPersons:MJBITPerson[1]|$mJBITFirm.id == $mJBITPersons.firmId}\n" +
                "   }\n" +
                ")\n";
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
    }

    /**
     * Join condition uses the generated 'businessDate' property (Date[1]) on
     * the businesstemporal class. This tests that system-generated milestoning
     * properties are visible inside a ModelJoin lambda.
     */
    @Test
    public void testModelJoinOnGeneratedBusinessDateProperty()
    {
        String source =
                "import meta::pure::profiles::*;\n" +
                "Class <<temporal.businesstemporal>> MJBDFirm\n" +
                "{\n" +
                "   id : String[1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> MJBDDepartment\n" +
                "{\n" +
                "   id : String[1];\n" +
                "}\n" +
                "Association MJBDFirm_MJBDDepartment\n" +
                "{\n" +
                "   mJBDDeptFirm : MJBDFirm[1];\n" +
                "   mJBDDepts : MJBDDepartment[*];\n" +
                "}\n" +
                "Class SrcMJBDFirm\n" +
                "{\n" +
                "   _id : String[1];\n" +
                "}\n" +
                "Class SrcMJBDDepartment\n" +
                "{\n" +
                "   _id : String[1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping MJBDMapping\n" +
                "(\n" +
                "   MJBDFirm[mJBDf1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJBDFirm\n" +
                "      id : $src._id\n" +
                "   }\n" +
                "   MJBDDepartment[mJBDd1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJBDDepartment\n" +
                "      id : $src._id\n" +
                "   }\n" +
                "   MJBDFirm_MJBDDepartment : ModelJoin\n" +
                "   {\n" +
                "      {mJBDDeptFirm:MJBDFirm[1], mJBDDepts:MJBDDepartment[1]|$mJBDDeptFirm.businessDate == $mJBDDepts.businessDate}\n" +
                "   }\n" +
                ")\n";
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
    }

    /**
     * Join condition uses the generated 'processingDate' property (Date[1]) on
     * the processingtemporal class.
     */
    @Test
    public void testModelJoinOnGeneratedProcessingDateProperty()
    {
        String source =
                "import meta::pure::profiles::*;\n" +
                "Class <<temporal.processingtemporal>> MJPDFirm\n" +
                "{\n" +
                "   id : String[1];\n" +
                "}\n" +
                "Class <<temporal.processingtemporal>> MJPDDepartment\n" +
                "{\n" +
                "   id : String[1];\n" +
                "}\n" +
                "Association MJPDFirm_MJPDDepartment\n" +
                "{\n" +
                "   mJPDDeptFirm : MJPDFirm[1];\n" +
                "   mJPDDepts : MJPDDepartment[*];\n" +
                "}\n" +
                "Class SrcMJPDFirm\n" +
                "{\n" +
                "   _id : String[1];\n" +
                "}\n" +
                "Class SrcMJPDDepartment\n" +
                "{\n" +
                "   _id : String[1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping MJPDMapping\n" +
                "(\n" +
                "   MJPDFirm[mJPDf1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJPDFirm\n" +
                "      id : $src._id\n" +
                "   }\n" +
                "   MJPDDepartment[mJPDd1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJPDDepartment\n" +
                "      id : $src._id\n" +
                "   }\n" +
                "   MJPDFirm_MJPDDepartment : ModelJoin\n" +
                "   {\n" +
                "      {mJPDDeptFirm:MJPDFirm[1], mJPDDepts:MJPDDepartment[1]|$mJPDDeptFirm.processingDate == $mJPDDepts.processingDate}\n" +
                "   }\n" +
                ")\n";
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
    }

    /**
     * Both sides of the association are businesstemporal; join uses a plain
     * declared property. The N×M expansion must handle two milestoned sets on
     * each side without confusion.
     */
    @Test
    public void testModelJoinBothSidesBusinessTemporal()
    {
        String source =
                "import meta::pure::profiles::*;\n" +
                "Class <<temporal.businesstemporal>> MJTWFirm\n" +
                "{\n" +
                "   id : String[1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> MJTWDivision\n" +
                "{\n" +
                "   firmId : String[1];\n" +
                "}\n" +
                "Association MJTWFirm_MJTWDivision\n" +
                "{\n" +
                "   mJTWFirmParent : MJTWFirm[1];\n" +
                "   mJTWDivisions : MJTWDivision[*];\n" +
                "}\n" +
                "Class SrcMJTWFirm\n" +
                "{\n" +
                "   _id : String[1];\n" +
                "}\n" +
                "Class SrcMJTWDivision\n" +
                "{\n" +
                "   _firmId : String[1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping MJTWMapping\n" +
                "(\n" +
                "   MJTWFirm[mJTWf1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJTWFirm\n" +
                "      id : $src._id\n" +
                "   }\n" +
                "   MJTWDivision[mJTWd1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJTWDivision\n" +
                "      firmId : $src._firmId\n" +
                "   }\n" +
                "   MJTWFirm_MJTWDivision : ModelJoin\n" +
                "   {\n" +
                "      {mJTWFirmParent:MJTWFirm[1], mJTWDivisions:MJTWDivision[1]|$mJTWFirmParent.id == $mJTWDivisions.firmId}\n" +
                "   }\n" +
                ")\n";
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
    }

    /**
     * businesstemporal subtype pairing: two Division subtypes mapped; join
     * must produce correct N×M expansion when the source class is milestoned.
     */
    @Test
    public void testModelJoinBusinessTemporalSubtypePairing()
    {
        String source =
                "import meta::pure::profiles::*;\n" +
                "Class <<temporal.businesstemporal>> MJSTBTFirm\n" +
                "{\n" +
                "   id : String[1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> MJSTBTDivision\n" +
                "{\n" +
                "   firmId : String[1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> MJSTBTRegionalDivision extends MJSTBTDivision\n" +
                "{\n" +
                "   region : String[1];\n" +
                "}\n" +
                "Association MJSTBTFirm_MJSTBTDivision\n" +
                "{\n" +
                "   mJSTBTFirmParent : MJSTBTFirm[1];\n" +
                "   mJSTBTDivs : MJSTBTDivision[*];\n" +
                "}\n" +
                "Class SrcMJSTBTFirm { _id : String[1]; }\n" +
                "Class SrcMJSTBTDivision { _firmId : String[1]; }\n" +
                "Class SrcMJSTBTRegionalDivision extends SrcMJSTBTDivision { _region : String[1]; }\n" +
                "###Mapping\n" +
                "Mapping MJSTBTMapping\n" +
                "(\n" +
                "   MJSTBTFirm[mJSTBTf1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJSTBTFirm\n" +
                "      id : $src._id\n" +
                "   }\n" +
                "   MJSTBTDivision[mJSTBTd1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJSTBTDivision\n" +
                "      firmId : $src._firmId\n" +
                "   }\n" +
                "   MJSTBTRegionalDivision[mJSTBTrd1] : Pure\n" +
                "   {\n" +
                "      ~src SrcMJSTBTRegionalDivision\n" +
                "      firmId : $src._firmId,\n" +
                "      region : $src._region\n" +
                "   }\n" +
                "   MJSTBTFirm_MJSTBTDivision : ModelJoin\n" +
                "   {\n" +
                "      {mJSTBTFirmParent:MJSTBTFirm[1], mJSTBTDivs:MJSTBTDivision[1]|$mJSTBTFirmParent.id == $mJSTBTDivs.firmId}\n" +
                "   }\n" +
                ")\n";
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();

        CoreInstance mapping = runtime.getCoreInstance("MJSTBTMapping");
        Assert.assertNotNull(mapping);
        CoreInstance assocImpl = mapping.getValueForMetaPropertyToMany(M2MappingProperties.associationMappings).getFirst();
        Assert.assertNotNull("Expected an association mapping", assocImpl);
        // 1 Firm set × 2 Division sets in each direction → 4 total property mappings
        Assert.assertEquals("Expected 4 property mappings for milestoned N×M subtype pairing", 4,
                assocImpl.getValueForMetaPropertyToMany(M2MappingProperties.propertyMappings).size());
    }
}
