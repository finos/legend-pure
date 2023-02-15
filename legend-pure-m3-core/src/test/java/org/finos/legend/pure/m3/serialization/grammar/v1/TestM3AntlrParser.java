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

package org.finos.legend.pure.m3.serialization.grammar.v1;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.NativeFunctionInstance;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.statelistener.StatsStateListener;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestM3AntlrParser extends AbstractPureTestWithCoreCompiledPlatform
{
    MutableList<CoreInstance> newInstances = Lists.fixedSize.empty();
    StatsStateListener stateListener = new StatsStateListener();

    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @Before
    public void setup()
    {
        this.newInstances = FastList.newList();
        stateListener = new StatsStateListener();
    }


    @Test
    public void testImports()
    {
        String imports = "import a::b::c::*;\n" +
                "import aa::bb::*;\n";
        String code = "Class a::b::c::Person\n" +
                "      {\n" +
                "         firstName: String[1];\n" +
                "         lastName: String[1];\n" +
                "         fullName(title: String[*]) {" +
                "               title + ' ' +  this.firstName + ' ' + this.lastName" +
                "          }: String[1];" +
                "      }";
        new M3AntlrParser(null).parse(imports + code, "test", true, 0, this.repository, this.newInstances, this.stateListener, this.context, 0, null);
    }

    @Test
    public void testSimpleClass()
    {
        String code = "Class Person\n" +
                "      {\n" +
                "         firstName: String[1];" +
                "         lastName: String[1];" +
                "      }\n" +
                "Class datamarts::DataM23::domain::BatchIDMilestone\n" +
                "{   \n" +
                "   inZ: Date[1];\n" +
                "   outZ: Date[1];\n" +
                "   vlfBatchIdIn: Integer[1];\n" +
                "   vlfBatchIdOut: Integer[1];   \n" +
                "   vlfDigest : String[1];\n" +
                "   \n" +
                "   filterByBatchID(batchId: Integer[1]) { $this.vlfBatchIdIn == $batchId; } : Boolean[1];\n" +
                "   filterByBatchIDRange(batchId: Integer[1]) { ($batchId >= $this.vlfBatchIdIn) && ($batchId <= $this.vlfBatchIdOut) ; } : Boolean[1];\n" +
                "   // Pure does not support contains or in today. Hence, difficul to support multiple batch id\n" +
                "   //filterByBatchIDs(batchIds: Float[*]) { and ($batchIds->map(batchId|$batchId == $this.vlfBatchIdIn)); } : Boolean[1];\n" +
                "   filterByProcessingTime(processingTime: Date[1]) { ($processingTime >= $this.inZ) && ($processingTime <= $this.outZ); } : Boolean[1];\n" +
                "}";
        new M3AntlrParser(null).parse(code, "test", true, 0, this.repository, this.newInstances, this.stateListener, this.context, 0, null);
    }

    @Test
    public void testClassClassRef()
    {
        String code = "Class meta::pure::mapping::test::Mapping extends PackageableElement\n" +
                "{\n" +
                "    includes : MappingInclude[*];\n" +
                "    classMappings : SetImplementation[*];\n" +
                "    enumerationMappings : EnumerationMapping<Any>[*];\n" +
                "    associationMappings : AssociationImplementation[*];\n" +
                "\n" +
                "    enumerationMappingByName(name:String[1])\n" +
                "    {\n" +
                "        $this.includes->map(i | $i.included)\n" +
                "                      ->map(m | $m.enumerationMappingByName($name))\n" +
                "                      ->concatenate($this.enumerationMappings->filter(em|$em.name == $name))\n" +
                "    }:EnumerationMapping<Any>[*];\n" +
                "\n" +
                "    classMappingByClassName(name:String[1])\n" +
                "    {\n" +
                "        let assocPropertyMappings = $this.associationMappings.propertyMappings;\n" +
                "        $this.includes->map(i | $i.included)\n" +
                "                      ->map(m | $m.classMappingByClassName($name))\n" +
                "                      ->concatenate($this.classMappings->filter(cm|$cm.class.name == $name))\n" +
                "                      ->map(cm | $cm->addAssociationMappingsIfRequired($assocPropertyMappings));\n" +
                "    }:SetImplementation[*];\n" +
                "\n" +
                "    rootClassMappingByClassName(name:String[1])\n" +
                "    {\n" +
                "        $this.classMappingByClassName($name)->filter(s|$s.root == true)->last();\n" +
                "    }:SetImplementation[0..1];\n" +
                "\n" +
                "    classMappingByClass(class:Class<Any>[1])\n" +
                "    {\n" +
                "        let assocPropertyMappings = $this.associationMappings.propertyMappings;\n" +
                "        $this.includes->map(i | $i.included)\n" +
                "                      ->map(m | $m.classMappingByClass($class))\n" +
                "                      ->concatenate($this.classMappings->filter(cm|$cm.class == $class))\n" +
                "                      ->map(cm | $cm->addAssociationMappingsIfRequired($assocPropertyMappings));\n" +
                "    }:SetImplementation[*];\n" +
                "\n" +
                "    rootClassMappingByClass(class:Class<Any>[1])\n" +
                "    {\n" +
                "        $this.classMappingByClass($class)->filter(s|$s.root == true)->last();\n" +
                "    }:SetImplementation[0..1];\n" +
                "\n" +
                "\n" +
                "    _classMappingByIdRecursive(id:String[*])\n" +
                "    {\n" +
                "        let result = $this.includes->map(i | $i.included)\n" +
                "                                   ->map(m | $m._classMappingByIdRecursive($id))\n" +
                "                                   ->concatenate($this.classMappings->filter(cm|$cm.id == $id));\n" +
                "    }:SetImplementation[*];\n" +
                "\n" +
                "    classMappingById(id:String[1])\n" +
                "    {\n" +
                "        let assocPropertyMappings = $this._associationPropertyMappingsByIdRecursive($id)->removeDuplicates();\n" +
                "        let allClassMappings = $this._classMappingByIdRecursive($id)->removeDuplicates();\n" +
                "        let result = $allClassMappings->toOne()->addAssociationMappingsIfRequired($assocPropertyMappings);\n" +
                "        if($result->isEmpty(),|[],|$result->removeDuplicates()->toOne());\n" +
                "    }:SetImplementation[0..1];\n" +
                "\n" +
                "    _associationPropertyMappingsByIdRecursive(id:String[1])\n" +
                "    {\n" +
                "        let result = $this.includes->map(i | $i.included)\n" +
                "                                   ->map(m | $m._associationPropertyMappingsByIdRecursive($id))\n" +
                "                                   ->concatenate($this.associationMappings.propertyMappings->filter(pm | $pm.sourceSetImplementationId == $id));\n" +
                "    }:PropertyMapping[*];\n" +
                "\n" +
                "    classMappings()\n" +
                "    {\n" +
                "        let assocPropertyMappings = $this.associationMappings.propertyMappings;\n" +
                "        $this.includes->map(i | $i.included)\n" +
                "                      ->map(m | $m.classMappings())\n" +
                "                      ->concatenate($this.classMappings)\n" +
                "                      ->map(cm | $cm->addAssociationMappingsIfRequired($assocPropertyMappings));\n" +
                "    }:SetImplementation[*];\n" +
                "\n" +
                "    findSubstituteStore(store:Store[1])\n" +
                "    {\n" +
                "        $this.includes->fold({inc:MappingInclude[1], sub:Store[0..1] | if($sub->isEmpty(), |$inc.findSubstituteStore($store), |$sub)}, [])\n" +
                "    }:Store[0..1];\n" +
                "\n" +
                "    resolveStore(store:Store[1])\n" +
                "    {\n" +
                "        let substitute = $this.findSubstituteStore($store);\n" +
                "        if($substitute->isEmpty(), |$store, |$substitute->toOne());\n" +
                "    }:Store[1];\n" +
                "}";
        new M3AntlrParser(null).parse(code, "test", true, 0, this.repository, this.newInstances, this.stateListener, this.context, 0, null);
    }

    @Test
    public void testGenericClass()
    {
        String code = "import meta::pure::metamodel::path::*;\n" +
                "\n" +
                "Class meta::pure::metamodel::path::test::Path<-U,V|m> extends Function<{U[1]->V[m]}>\n" +
                "{\n" +
                "   start : GenericType[1];\n" +
                "   path : PathElement[1..*];\n" +
                "   referenceUsages : ReferenceUsage[*];\n" +
                "   name : String[0..1];\n" +
                "}";
        new M3AntlrParser(null).parse(code, "test", true, 0, this.repository, this.newInstances, this.stateListener, this.context, 0, null);
    }


    @Test
    public void testClassWithQualifiedProperty()
    {
        String code = "Class Person2\n" +
                "      {\n" +
                "         firstName: String[1];\n" +
                "         lastName: String[1];\n" +
                "         fullName(title: String[*]) {" +
                "               title + ' ' +  this.firstName + ' ' + this.lastName" +
                "          }: String[1];" +
                "      }";
        new M3AntlrParser(null).parse(code, "test", true, 0, this.repository, this.newInstances, this.stateListener, this.context, 0, null);
    }

    @Test
    public void testFunctionDefinition()
    {
        String code = "Class Person3\n" +
                "      {\n" +
                "         firstName: String[1];\n" +
                "         lastName: String[1];\n" +
                "         fullName(title: String[*]) {" +
                "               title + ' ' +  this.firstName + ' ' + this.lastName" +
                "          }: String[1];\n" +
                "      }\n" +
                "      function myT():String[1]" +
                "      {" +
                "           'hello world'" +
                "      }" +
                "";
        new M3AntlrParser(null).parse(code, "test", true, 0, this.repository, this.newInstances, this.stateListener, this.context, 0, null);
    }

    @Test
    public void testComplexFunctions()
    {
        String code = "import meta::json::*;\n" +
                "import meta::PR1::*;\n" +
                "import datamarts::dm::mapping::*;\n" +
                "import datamarts::dm::store::*;\n" +
                "import datamarts::dm::domain::pactnext::reporting::subdom1::*;\n" +
                "import datamarts::dm::domain::pactnext::reporting::*;\n" +
                "import datamarts::dm::mapping::pact::*;\n" +
                "import meta::pure::mapping::*;\n" +
                "\n" +
                "function \n" +
                "{service.url='/pact/reporting/sessions/{start}/{end}'}\n" +
                "datamarts::dm::domain::pactnext::reporting::queries::PACTSessions(start: String[1], end: String[1]):String[1]\n" +
                "{\n" +
                "   datamarts::dm::domain::pactnext::reporting::queries::PACTSessions(parseDate($start), parseDate($end))->toJSONStringStream([],true)->makeString()\n" +
                "}\n" +
                "\n" +
                "function datamarts::dm::domain::pactnext::reporting::queries::PACTSessions(start: Date[1], end: Date[1]):TabularDataSet[1]\n" +
                "{\n" +
                "   datamarts::dm::domain::pactnext::reporting::queries::PACTSessions($start, $end, 'union')\n" +
                "}\n" +
                "\n" +
                "function datamarts::dm::domain::pactnext::reporting::queries::PACTSessions(start: Date[1], end: Date[1], environment: String[1]):TabularDataSet[1]\n" +
                "{\n" +
                "   execute( \n" +
                "      | datamarts::dm::domain::pactnext::reporting::Session.all()\n" +
                "         ->filter(s | greaterThanEqual(datePart($s.startTime), $start) && lessThanEqual(datePart($s.startTime), $end))\n" +
                "         ->filter(s | isNotEmpty($s.eventType))\n" +
                "         ->filter(s | isNotEmpty($s.accessType))      \n" +
                "//TODO:         ->filter(s | !$s.device->isEmpty())\n" +
                "         ->project(\n" +
                "            [\n" +
                "              x | dateDiff($x.endTime, $x.startTime, meta::pure::functions::date::DurationUnit.SECONDS),\n" +
                " \t\t      x | $x.endTime,\n" +
                " \t\t      x | $x.environment,\n" +
                "              x | $x.pactImplementation,\n" +
                "\t\t      x | $x.sessionId,\n" +
                "\t\t      x | $x.reasonForAccess,\n" +
                "\t\t      x | $x.reviewable,\n" +
                "\t\t      x | $x.sourceSystemName,\n" +
                "\t\t      x | $x.startTime,\n" +
                "\t\t      x | $x.accessType,\n" +
                "              \n" +
                "              x | if(isNotEmpty($x.device.hostname),|'APPLICATION_SERVER',|if(isNotEmpty($x.device.dataserver),|'DATABASE_SERVER',|if(isNotEmpty($x.device.database),|'DATABASE',|'UNKNOWN'))),\n" +
                "\t\t      x | $x.device.hostname,\n" +
                "              x | $x.device.dataserver,\n" +
                "              x | $x.device.database,\n" +
                "              \n" +
                "\t\t      x | $x.eventType,\n" +
                "        \n" +
                "\t\t      x | $x.review.comment,\n" +
                "\t\t      x | $x.review.closureTime,\n" +
                "\t\t      x | $x.review.reviewer.businessUnitName,\n" +
                "\t\t      x | $x.review.reviewer.city,\n" +
                "\t\t      x | $x.review.reviewer.departmentName,\n" +
                "              x | $x.review.reviewer.departmentCode,\n" +
                "\t\t      x | $x.review.reviewer.divisionName,\n" +
                "\t\t      x | $x.review.reviewer.firstName,\n" +
                "\t\t      x | $x.review.reviewer.kerberos,\n" +
                "\t\t      x | $x.review.reviewer.lastName,\n" +
                "\t\t      x | $x.review.reviewer.title,\n" +
                "        \n" +
                "\t\t      x | $x.requestor.businessUnitName,\n" +
                "\t\t      x | $x.requestor.city,\n" +
                "\t\t      x | $x.requestor.departmentName,\n" +
                "              x | $x.requestor.departmentCode,\n" +
                "\t\t      x | $x.requestor.divisionName,\n" +
                "\t\t      x | '', //employeeNumber does not exist\n" +
                "\t\t      x | $x.requestor.firstName,\n" +
                "\t\t      x | $x.requestor.lastName,\n" +
                "\t\t      x | $x.requestor.kerberos,\n" +
                "\t\t      x | $x.requestor.title,\n" +
                "        \n" +
                "              x | $x.systemAccountName,\n" +
                "              x | $x.accessType,\n" +
                "              x | $x.userKerberosId\n" +
                "           ],\n" +
                "\t       [\n" +
                "              'Duration',\n" +
                "\t\t      'end',\n" +
                "\t\t      'environment',\n" +
                "              'pact.implementation',\n" +
                "   \t\t      'id',\n" +
                "\t\t      'reasonForAccess',\n" +
                "\t\t      'reviewabilityStatus',\n" +
                "\t\t      'sourceSystemName',\n" +
                "\t\t      'start',\n" +
                "\t\t      'accessType.name',\n" +
                "              \n" +
                "              'device.type',\n" +
                "\t\t      'device.hostname',\n" +
                "\t\t      'device.dataserver',\n" +
                "\t\t      'device.database',\n" +
                "              \n" +
                "\t\t      'eventType.name',\n" +
                "              \n" +
                "\t\t      'review.comment',\n" +
                "\t\t      'review.time',\n" +
                "\t\t      'review.reviewer.businessUnitName',\n" +
                "\t\t      'review.reviewer.city',\n" +
                "\t\t      'review.reviewer.departmentName',\n" +
                "              'review.reviewer.departmentCode',\n" +
                "\t\t      'review.reviewer.divisionName',\n" +
                "\t\t      'review.reviewer.firstName',\n" +
                "\t\t      'review.reviewer.kerberos',\n" +
                "\t\t      'review.reviewer.lastName',\n" +
                "\t\t      'review.reviewer.title',\n" +
                "              \n" +
                "\t\t      'user.businessUnitName',\n" +
                "\t\t      'requestor.city',\n" +
                "\t\t      'requestor.departmentName',\n" +
                "              'requestor.departmentCode',\n" +
                "\t\t      'requestor.divisionName',\n" +
                "\t\t      'requestor.employeeNumber',\n" +
                "\t\t      'requestor.firstName',\n" +
                "\t\t      'requestor.lastName',\n" +
                "\t\t      'requestor.kerberos',\n" +
                "\t\t      'requestor.title',\n" +
                "              \n" +
                "              'targetUserId',\n" +
                "              'accessTypeCode',\n" +
                "              'eventLogin'\n" +
                "           ]\n" +
                "   ), datamarts::dm::domain::pactnext::reporting::queries::determineMapping($environment), datamarts::dm::store::EPDAIQRuntime('EP DA IQ CDC')).values->at(0);\n" +
                "}\n" +
                "\n" +
                "function \n" +
                "{service.url='/pact/reporting/ccmpactsessionsexploded/{start}/{end}'}\n" +
                "datamarts::dm::domain::pactnext::reporting::queries::CCMPACTSessionsExploded(start: String[1], end: String[1]):String[1]\n" +
                "{\n" +
                "   datamarts::dm::domain::pactnext::reporting::queries::CCMPACTSessionsExploded(parseDate($start), parseDate($end))->toJSONStringStream([],true)->makeString()\n" +
                "}\n" +
                "\n" +
                "function datamarts::dm::domain::pactnext::reporting::queries::CCMPACTSessionsExploded(start: Date[1], end: Date[1]):TabularDataSet[1]\n" +
                "{\n" +
                "   datamarts::dm::domain::pactnext::reporting::queries::CCMPACTSessionsExploded($start, $end, 'union')\n" +
                "}\n" +
                "\n" +
                "function datamarts::dm::domain::pactnext::reporting::queries::CCMPACTSessionsExploded(start: Date[1], end: Date[1], environment: String[1]):TabularDataSet[1]\n" +
                "{\n" +
                "   execute( \n" +
                "      | datamarts::dm::domain::pactnext::reporting::Session.all()\n" +
                "         ->filter(s | greaterThanEqual(datePart($s.startTime), $start) && lessThanEqual(datePart($s.startTime), $end))\n" +
                "         ->filter(s | isNotEmpty($s.eventType))\n" +
                "         ->filter(s | isNotEmpty($s.accessType))      \n" +
                "//TODO:         ->filter(s | !$s.device->isEmpty())\n" +
                "         ->project(\n" +
                "            [\n" +
                "              x | dateDiff($x.endTime, $x.startTime, meta::pure::functions::date::DurationUnit.SECONDS),\n" +
                " \t\t      x | $x.endTime,\n" +
                " \t\t      x | $x.environment,\n" +
                "              x | $x.pactImplementation,\n" +
                "\t\t      x | $x.sessionId,\n" +
                "\t\t      x | $x.reasonForAccess,\n" +
                "\t\t      x | $x.reviewable,\n" +
                "\t\t      x | $x.sourceSystemName,\n" +
                "\t\t      x | $x.startTime,\n" +
                "\t\t      x | $x.accessType,\n" +
                "              \n" +
                "              x | if(isNotEmpty($x.device.hostname),|'APPLICATION_SERVER',|if(isNotEmpty($x.device.dataserver),|'DATABASE_SERVER',|if(isNotEmpty($x.device.database),|'DATABASE',|'UNKNOWN'))),\n" +
                "\t\t      x | $x.device.hostname,\n" +
                "\t\t      x | $x.device.dataserver,\n" +
                "\t\t      x | $x.device.database,\n" +
                "              \n" +
                "\t\t      x | $x.eventType,\n" +
                "              \n" +
                "\t\t      x | $x.review.comment,\n" +
                "\t\t      x | $x.review.closureTime,\n" +
                "\t\t      x | $x.review.reviewer.businessUnitName,\n" +
                "\t\t      x | $x.review.reviewer.city,\n" +
                "\t\t      x | $x.review.reviewer.departmentName,\n" +
                "              x | $x.review.reviewer.departmentCode,\n" +
                "\t\t      x | $x.review.reviewer.divisionName,\n" +
                "\t\t      x | $x.review.reviewer.firstName,\n" +
                "\t\t      x | $x.review.reviewer.kerberos,\n" +
                "\t\t      x | $x.review.reviewer.lastName,\n" +
                "\t\t      x | $x.review.reviewer.title,\n" +
                "              \n" +
                "\t\t\t  x | $x.requestor.businessUnitName,\n" +
                "\t\t\t  x | $x.requestor.city,\n" +
                "\t\t\t  x | $x.requestor.departmentName,\n" +
                "\t\t\t  x | $x.requestor.departmentCode,\n" +
                "\t\t\t  x | $x.requestor.divisionName,\n" +
                "\t\t\t  x | '', //employeeNumber does not exist\n" +
                "\t\t\t  x | $x.requestor.firstName,\n" +
                "\t\t\t  x | $x.requestor.lastName,\n" +
                "\t\t\t  x | $x.requestor.kerberos,\n" +
                "\t\t\t  x | $x.requestor.title,\n" +
                "              \n" +
                "              x | $x.systemAccountName,\n" +
                "              \n" +
                "              x | $x.reviewabilityList.reviewable,\n" +
                "              x | $x.reviewabilityList.deployment.deploymentName,\n" +
                "              x | $x.reviewabilityList.deployment.deploymentId,\n" +
                "              x | $x.reviewabilityList.deployment.application.applicationName,\n" +
                "              x | $x.reviewabilityList.deployment.application.applicationId,\n" +
                "              x | $x.reviewabilityList.deployment.application.family.familyName,\n" +
                "              x | $x.reviewabilityList.deployment.application.family.familyId,\n" +
                "              x | $x.reviewabilityList.deployment.application.family.subBu.subBuName,\n" +
                "              x | $x.reviewabilityList.deployment.application.family.subBu.subBuId,\n" +
                "              x | $x.reviewabilityList.deployment.application.family.subBu.bu.buName,\n" +
                "              x | $x.reviewabilityList.deployment.application.family.subBu.bu.buId,\n" +
                "              x | $x.reviewabilityList.deployment.environment,\n" +
                "              \n" +
                "              x | $x.accessType,\n" +
                "              x | $x.userKerberosId\n" +
                "           ],\n" +
                "\t\t   [\n" +
                "              'Duration',\n" +
                "\t\t      'end',\n" +
                "\t\t      'environment',\n" +
                "              'pact.implementation',\n" +
                "\t\t      'id',\n" +
                "\t\t      'reasonForAccess',\n" +
                "\t\t      'overallReviewabilityStatus',\n" +
                "\t\t      'sourceSystemName',\n" +
                "\t\t      'start',\n" +
                "\t\t      'accessType.name',\n" +
                "              \n" +
                "              'device.type',\n" +
                "\t\t      'device.hostname',\n" +
                "\t\t      'device.dataserver',\n" +
                "\t\t      'device.database',\n" +
                "              \n" +
                "\t\t      'eventType.name',\n" +
                "              \n" +
                "\t\t      'review.comment',\n" +
                "\t\t      'review.time',\n" +
                "\t\t      'review.reviewer.businessUnitName',\n" +
                "\t\t      'review.reviewer.city',\n" +
                "\t\t      'review.reviewer.departmentName',\n" +
                "              'review.reviewer.departmentCode',\n" +
                "\t\t      'review.reviewer.divisionName',\n" +
                "\t\t      'review.reviewer.firstName',\n" +
                "\t\t      'review.reviewer.kerberos',\n" +
                "\t\t      'review.reviewer.lastName',\n" +
                "\t\t      'review.reviewer.title',\n" +
                "\t\t      \n" +
                "              'requestor.businessUnitName',\n" +
                "\t\t      'requestor.city',\n" +
                "\t\t      'requestor.departmentName',\n" +
                "              'requestor.departmentCode',\n" +
                "\t\t      'requestor.divisionName',\n" +
                "\t\t      'requestor.employeeNumber',\n" +
                "\t\t      'requestor.firstName',\n" +
                "\t\t      'requestor.lastName',\n" +
                "\t\t      'requestor.kerberos',\n" +
                "\t\t      'requestor.title',\n" +
                "              \n" +
                "              'targetUserId',\n" +
                "              \n" +
                "              'reviewable',\n" +
                "              'deployment.name',\n" +
                "              'deployment.id',\n" +
                "              'application.name',\n" +
                "              'application.id',\n" +
                "              'family.name',\n" +
                "              'family.id',\n" +
                "              'subBU.name',\n" +
                "              'subBU.id',\n" +
                "              'BU.name',\n" +
                "              'BU.id',\n" +
                "              'deploymentEnvironment',\n" +
                "              \n" +
                "              'accessTypeCode',\n" +
                "              'eventLogin'\n" +
                "           ]\n" +
                "\t), datamarts::dm::domain::pactnext::reporting::queries::determineMapping($environment), EPDAIQRuntime('EP DA IQ CDC')).values->at(0);  \n" +
                "}\n" +
                "\n" +
                "function datamarts::dm::domain::pactnext::reporting::queries::determineMapping(environment: String[1]):Mapping[1]\n" +
                "{\n" +
                "   if($environment == 'legacy',\n" +
                "      | datamarts::dm::mapping::ParLegacyMapping, \n" +
                "      | if($environment == 'up',\n" +
                "            | datamarts::dm::mapping::ParUpMapping,\n" +
                "            | datamarts::dm::mapping::ParUnionMapping))\n" +
                "}";
        new M3AntlrParser(null).parse(code, "test", true, 0, this.repository, this.newInstances, this.stateListener, this.context, 0, null);
    }

    @Test
    public void testEnum()
    {
        String code = "Enum apps::global::dsb::domain::uiModel::DataSetRelativeDateUnit\n" +
                "{\n" +
                "   {JsonSerializationInfo.enumValue = 'Days'} Days,\n" +
                "   {JsonSerializationInfo.enumValue = 'Weeks'} Weeks,\n" +
                "   {JsonSerializationInfo.enumValue = 'Months'} Months,\n" +
                "   {JsonSerializationInfo.enumValue = 'Years'} Years\n" +
                "}\n" +
                "\n" +
                "Enum apps::global::dsb::domain::uiModel::DataSetRelativeDateStart\n" +
                "{\n" +
                "   {JsonSerializationInfo.enumValue = 'Today'} Today,\n" +
                "   {JsonSerializationInfo.enumValue = 'StartOfWeek'} StartOfWeek,\n" +
                "   {JsonSerializationInfo.enumValue = 'StartOfMonth'} StartOfMonth,\n" +
                "   {JsonSerializationInfo.enumValue = 'StartOfYear'} StartOfYear,\n" +
                "   {JsonSerializationInfo.enumValue = 'PreviousFriday'} PreviousFriday,\n" +
                "   {JsonSerializationInfo.enumValue = 'StartOfQuarter'} StartOfQuarter,\n" +
                "   {JsonSerializationInfo.enumValue = 'RunDate'} RunDate\n" +
                "}\n" +
                "Enum apps::global::dsb::domain::uiModel::DataSetCompositeOperation\n" +
                "{\n" +
                "    {JsonSerializationInfo.enumValue = 'AND'} And,\n" +
                "    {JsonSerializationInfo.enumValue = 'OR'} Or\n" +
                "}" +
                "Enum apps::global::dsb::domain::uiModel::DataSetComparisonOperation\n" +
                "{\n" +
                "    {JsonSerializationInfo.enumValue = '=='} Equal,\n" +
                "    {JsonSerializationInfo.enumValue = '!='} NotEqual,\n" +
                "    {JsonSerializationInfo.enumValue = '>'} GreaterThan, \n" +
                "    {JsonSerializationInfo.enumValue = '>='} GreaterThanOrEqual, \n" +
                "    {JsonSerializationInfo.enumValue = '<'} LessThan, \n" +
                "    {JsonSerializationInfo.enumValue = '<='} LessThanOrEqual,\n" +
                "    {JsonSerializationInfo.enumValue = 'startsWith'} StartsWith, \n" +
                "    {JsonSerializationInfo.enumValue = 'doesNotStartWith'} DoesNotStartWith, \n" +
                "    {JsonSerializationInfo.enumValue = 'endsWith'} EndsWith, \n" +
                "    {JsonSerializationInfo.enumValue = 'doesNotEndWith'} DoesNotEndWith, \n" +
                "    {JsonSerializationInfo.enumValue = 'contains'} Contains,\n" +
                "    {JsonSerializationInfo.enumValue = 'doesNotContain'} DoesNotContain,\n" +
                "    {JsonSerializationInfo.enumValue = 'in'} In, \n" +
                "    {JsonSerializationInfo.enumValue = 'notIn'} NotIn, \n" +
                "    {JsonSerializationInfo.enumValue = 'isEmpty'} IsEmpty, \n" +
                "    {JsonSerializationInfo.enumValue = 'isNotEmpty'} IsNotEmpty \n" +
                "}\n";
        new M3AntlrParser(null).parse(code, "test", true, 0, this.repository, this.newInstances, this.stateListener, this.context, 0, null);
    }

    @Test
    public void testAssociation()
    {
        String code = "Association datamarts::dm::domain::sayo::Product_Technical_Owner_Product\n" +
                "{\n" +
                "    technical_owner_product: Product[1];\n" +
                "    {up.relationshipType = 'shared'}\n" +
                "    product_technical_owners: ProductPrimaryOwner[*];\n" +
                "}\n" +
                "\n" +
                "Association datamarts::dm::domain::sayo::Product_Technical_Owner_Person\n" +
                "{\n" +
                "    technical_owner_person: GsPerson[1];\n" +
                "    {up.relationshipType = 'shared'}\n" +
                "    product_technical_owners: ProductPrimaryOwner[*];\n" +
                "}\n" +
                "\n" +
                "Association datamarts::dm::domain::sayo::Test_Maturity_Champion\n" +
                "{\n" +
                "    {up.relationshipType = 'shared'}\n" +
                "    test_maturity_champion: GsPerson[1];\n" +
                "    test_maturity_champion_products: Product[*];\n" +
                "}\n" +
                "\n" +
                "Association datamarts::dm::domain::sayo::Test_Maturity_Advocate\n" +
                "{\n" +
                "    {up.relationshipType = 'shared'}\n" +
                "    test_maturity_advocate: GsPerson[1];\n" +
                "    test_maturity_advocate_products: Product[*];\n" +
                "}\n";
        new M3AntlrParser(null).parse(code, "test", true, 0, this.repository, this.newInstances, this.stateListener, this.context, 0, null);
    }

    @Test
    public void testProfile()
    {
        String code = "\n" +
                "Profile apps::global::dsb::mapping::json::JsonSerializationInfo\n" +
                "{\n" +
                "    stereotypes: [ignore, execute];\n" +
                "    tags: [enumValue, propertyName];\n" +
                "}\n";
        new M3AntlrParser(null).parse(code, "test", true, 0, this.repository, this.newInstances, this.stateListener, this.context, 0, null);
        code = "\n" +
                "Profile apps::global::dsb::mapping::json::JsonSerializationInfoST\n" +
                "{\n" +
                "    stereotypes: [ignore, execute];\n" +
                "}\n";
        new M3AntlrParser(null).parse(code, "test", true, 0, this.repository, this.newInstances, this.stateListener, this.context, 0, null);
        code = "\n" +
                "Profile apps::global::dsb::mapping::json::JsonSerializationInfoTO\n" +
                "{\n" +
                "    tags: [enumValue, propertyName];\n" +
                "}\n";
        new M3AntlrParser(null).parse(code, "test", true, 0, this.repository, this.newInstances, this.stateListener, this.context, 0, null);
        code = "\n" +
                "Profile apps::global::dsb::mapping::json::JsonSerializationInfoBlank\n" +
                "{\n" +
                "}\n";
        new M3AntlrParser(null).parse(code, "test", true, 0, this.repository, this.newInstances, this.stateListener, this.context, 0, null);
    }

    @Test
    public void testNativeFunction()
    {
        String code = "// Functions for constructing dates\n" +
                "native function meta::pure::functions::date::date(year:Integer[1]):Date[1];\n" +
                "native function meta::pure::functions::date::date(year:Integer[1], month:Integer[1]):Date[1];\n" +
                "native function meta::pure::functions::date::date(year:Integer[1], month:Integer[1], day:Integer[1]):Date[1];\n" +
                "native function meta::pure::functions::date::date(year:Integer[1], month:Integer[1], day:Integer[1], hour:Integer[1]):Date[1];\n" +
                "native function meta::pure::functions::date::date(year:Integer[1], month:Integer[1], day:Integer[1], hour:Integer[1], minute:Integer[1]):Date[1];\n" +
                "native function meta::pure::functions::date::date(year:Integer[1], month:Integer[1], day:Integer[1], hour:Integer[1], minute:Integer[1], second:Number[1]):Date[1];\n";
        new M3AntlrParser(null).parse(code, "test", true, 0, this.repository, this.newInstances, this.stateListener, this.context, 0, null);
    }

    @Test
    public void testStereotypeOnNativeFunction()
    {
        String code =
                "\n" +
                "Profile meta::pure::function::dummyProfile\n" +
                "{\n" +
                "   stereotypes : [Dummy];" +
                "}\n" +
                "native function <<dummyProfile.Dummy>> meta::pure::functions::date::date(year:Integer[1]):Date[1];\n";
        new M3AntlrParser(null).parse(code, "test", true, 0, this.repository, this.newInstances, this.stateListener, this.context, 0, null);
        Assert.assertEquals(1, this.newInstances.selectInstancesOf(NativeFunctionInstance.class).getOnly()._stereotypesCoreInstance().size());
    }

    @Test
    public void testInstanceParsingWithRootPackageReference()
    {
        String code = "^meta::pure::functions::lang::KeyValue(key='pkg', value=::)";
        new M3AntlrParser(null).parse(code, "test", true, 0, this.repository, this.newInstances, this.stateListener, this.context, 0, null);
    }
}
