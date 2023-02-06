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

package org.finos.legend.pure.m3.tests.incremental.milestoning;


import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestMilestoning extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), PureCodeStorage.createCodeStorage(getCodeStorageRoot(), getCodeRepositories()), getFactoryRegistryOverride(), getOptions(), getExtra());
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        MutableList<CodeRepository> repositories = org.eclipse.collections.impl.factory.Lists.mutable.withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories());
        CodeRepository system = GenericCodeRepository.build("system", "((meta)|(system)|(apps::pure))(::.*)?", PlatformCodeRepository.NAME);
        CodeRepository test = GenericCodeRepository.build("test", "test(::.*)?", PlatformCodeRepository.NAME, "system");
        repositories.add(test);
        repositories.add(system);
        return repositories;
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("A.pure");
        runtime.delete("association.pure");
        runtime.delete("associationAB.pure");
        runtime.delete("B.pure");
        runtime.delete("classes.pure");
        runtime.delete("classA.pure");
        runtime.delete("classB.pure");
        runtime.delete("go.pure");
        runtime.delete("location.pure");
        runtime.delete("profiles.pure");
        runtime.delete("sourceA.pure");
        runtime.delete("sourceB.pure");
        runtime.delete("sourceId.pure");
        runtime.delete("sourceId1.pure");
        runtime.delete("sourceId2.pure");
        runtime.delete("sourceIdClassA.pure");
        runtime.delete("sourceIdClassB.pure");
        runtime.delete("test.pure");
        runtime.delete("testFunc.pure");
        runtime.delete("trader.pure");
        runtime.delete("userId.pure");
        runtime.delete("/test/associationB_C.pure");
        runtime.delete("/test/associationB_C.pure");
        runtime.delete("/test/baseClass.pure");
        runtime.delete("/test/go.pure");
        runtime.delete("/test/myAssociation.pure");
        runtime.delete("/test/myClass.pure");
        runtime.delete("/test/specializationClassB.pure");
        runtime.delete("/test/specializationClassC.pure");
        runtime.compile();
    }

    @Test
    public void testBusinessTemporalClassStability()
    {
        String classAWithBusinessTemporalStereotype = "Class <<temporal.businesstemporal>> test::A{}";
        String classAWithNoStereotype = "Class test::A{}";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", classAWithBusinessTemporalStereotype)
                        .createInMemorySource("userId.pure", "function test():Any[*]{let a =^test::A(businessDate=%2018-12-18T14:03:00);$a.businessDate;}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", classAWithNoStereotype)
                        .compileWithExpectedCompileFailure("Can't find the property 'businessDate' in the class test::A", "userId.pure", 1, 78)
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", classAWithBusinessTemporalStereotype)
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testBusinessTemporalClassToNonTemporalClassStability()
    {
        String businessTemporalClassB = "Class <<temporal.businesstemporal>> B{}";
        String bDependentClassA = "Class A{b : B[0..1];}";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceB.pure", businessTemporalClassB)
                        .createInMemorySource("sourceA.pure", bDependentClassA)
                        .createInMemorySource("userId.pure", "function test():Any[*]{let a = ^A(); $a.b(%9999-12-31);}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceA.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 33)
                        .createInMemorySource("sourceA.pure", bDependentClassA)
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testBusinessTemporalClassToNonTemporalClassStabilityViaAssociation()
    {
        String classA = "Class A{}";
        String businessTemporalClassB = "Class <<temporal.businesstemporal>> B{}";
        String association = "Association A_B{ a:A[1]; b:B[1]; }";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("sourceA.pure", classA)
                        .createInMemorySource("sourceB.pure", businessTemporalClassB)
                        .createInMemorySource("association.pure", association)
                        .createInMemorySource("userId.pure", "function test():Any[*]{let a = ^A(); $a.b(%9999-12-31);}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("association.pure")
                        .compileWithExpectedCompileFailure("The system can't find a match for the function: b(_:A[1],_:StrictDate[1])", "userId.pure", 1, 41)
                        .createInMemorySource("association.pure", association)
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testNonTemporalClassToBusinessTemporalClassStability()
    {
        String businessTemporalClassB = "Class <<temporal.businesstemporal>> B{}";
        String bDependentClassA = "Class A{     \n" +
                " b : B[0..1];\n" +
                "}            \n";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("sourceB.pure", businessTemporalClassB)
                        .createInMemorySource("sourceA.pure", bDependentClassA)
                        .createInMemorySource("userId.pure", "function test():Any[*]{let a = ^A(); $a.b(%9999-12-31);}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceB.pure")
                        .compileWithExpectedCompileFailure("B has not been defined!", "sourceA.pure", 2, 6)
                        .createInMemorySource("sourceB.pure", businessTemporalClassB)
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testNonTemporalClassToBusinessTemporalClassStabilityViaAssociationRemoveAssn()
    {
        String classes = "Class A{}\n" +
                "Class <<temporal.businesstemporal>> B{}\n";
        String association = "Association AB{" +
                "  a: A[0..1];" +
                "  b: B[0..1];" +
                "}";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("classes.pure", classes)
                        .createInMemorySource("association.pure", association)
                        .createInMemorySource("userId.pure", "function test():Any[*]{let a = ^A(); $a.b(%9999-12-31);}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("association.pure")
                        .compileWithExpectedCompileFailure("The system can't find a match for the function: b(_:A[1],_:StrictDate[1])", "userId.pure", 1, 41)
                        .createInMemorySource("association.pure", association)
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testNonTemporalClassToBusinessTemporalClassStabilityViaAssociationRemoveTargetType()
    {
        String classA = "Class A{}\n";
        String classB = "Class <<temporal.businesstemporal>> B{}\n";
        String association = "Association AB{\n" +
                "  a: A[0..1];\n" +
                "  b: B[0..1];\n" +
                "}";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("classes.pure", classA)
                        .createInMemorySource("classB.pure", classB)
                        .createInMemorySource("association.pure", association)
                        .createInMemorySource("userId.pure", "function test():Any[*]{let a = ^A(); $a.b(%9999-12-31);}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("classB.pure")
                        .compileWithExpectedCompileFailure("B has not been defined!", "association.pure", 3, 6)
                        .createInMemorySource("classB.pure", classB)
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }


    @Test
    public void testFunctionToBusinessTemporalClassStability()
    {
        String classAndBWithBusinessTemporalStereotype = "Class A{b : B[0..1];} Class <<temporal.businesstemporal>> B{}";
        String classAndBNoStereotype = "Class A{b : B[0..1];} Class  B{}";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("sourceId.pure", classAndBWithBusinessTemporalStereotype)
                        .createInMemorySource("userId.pure", "function test():Any[*]{let a = ^A(); $a.b(%9999-12-31);}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", classAndBNoStereotype)
                        .compileWithExpectedCompileFailure("The system can't find a match for the function: b(_:A[1],_:StrictDate[1])", "userId.pure", 1, 41)
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", classAndBWithBusinessTemporalStereotype)
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testStabilityOfEnablingDisablingBusinessTemporalStereotype()
    {
        String classA = "Class A{b:B[0..1];}";
        String classBNoStereotype = "Class B{}";
        String classBWithStereotype = "Class <<temporal.businesstemporal>> B{}";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("sourceId1.pure", classA)
                        .createInMemorySource("sourceId2.pure", classBNoStereotype)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId2.pure")
                        .createInMemorySource("sourceId2.pure", classBWithStereotype)
                        .compile()
                        .deleteSource("sourceId2.pure")
                        .createInMemorySource("sourceId2.pure", classBNoStereotype)
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testStabilityOfMilestonedQualifiedPropertyUnbindingWithAutomap()
    {
        String classes = "Class A {b:B[*];} " +
                "Class B {c:C[1];}" +
                "Class <<temporal.businesstemporal>> C { idt:Integer[1];}";
        String functions = "function testLambda():Any[*]{" +
                "   let bd=%999-12-31;" +
                "   {a:A[1]|$a.b.c($bd).idt};" +
                "}";
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("sourceId.pure", classes)
                        .createInMemorySource("userId.pure", functions)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", classes)
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testStabilityOfNoArgMilestonedProperty()
    {
        String classes = "Class <<temporal.businesstemporal>> A {b:B[*];} " +
                "Class <<temporal.businesstemporal>> B {attr:Integer[1];}";
        String functions = "function noArgQualifiedPropertyUsage():Any[*]{" +
                "   let bd=%999-12-31;" +
                "   {|A.all($bd).b.attr};" +
                "}";
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("sourceId.pure", classes)
                        .createInMemorySource("userId.pure", functions)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", classes)
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testStabilityOfMilestonedQualifiedPropertyInFunctionExpressionWithoutAutoMap()
    {
        testStabilityOfMilestonedQualifiedPropertyInFunctionExpressionWithAutoMap(false);
    }

    @Test
    public void testStabilityOfMilestonedQualifiedPropertyInFunctionExpressionWithAutoMap()
    {
        testStabilityOfMilestonedQualifiedPropertyInFunctionExpressionWithAutoMap(true);
    }

    private void testStabilityOfMilestonedQualifiedPropertyInFunctionExpressionWithAutoMap(boolean withAutoMap)
    {
        String classA = "Class <<temporal.businesstemporal>> A {b:B[" + (withAutoMap ? "*" : "1") + "];} ";
        String classBNonTemporal = "Class B {attr:Integer[1];}";
        String classBTemporal = "Class <<temporal.businesstemporal>> B {attr:Integer[1];}";

        String functions = "function noArgQualifiedPropertyUsage():Any[*]{" +
                "   let bd=%999-12-31;" +
                "   {|A.all($bd).b.attr};" +
                "}";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("sourceIdClassA.pure", classA)
                        .createInMemorySource("sourceIdClassB.pure", classBNonTemporal)
                        .createInMemorySource("userId.pure", functions)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceIdClassB.pure")
                        .createInMemorySource("sourceIdClassB.pure", classBTemporal)
                        .compile()
                        .deleteSource("sourceIdClassB.pure")
                        .createInMemorySource("sourceIdClassB.pure", classBNonTemporal)
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testUpdatingPropertiesWithNonMilestoningStereotypes()
    {
        String profiles = "Profile datamarts::DataM23::domain::cmaoperationalstore::OperationalStore{ stereotypes:[ datasetKey ]; }\n";
        String classes = "Class <<meta::pure::service::service.disableStreaming>> A {<<datamarts::DataM23::domain::cmaoperationalstore::OperationalStore.datasetKey>> str:String[1];} ";
        runtime.createInMemorySource("classes.pure", classes);
        runtime.createInMemorySource("profiles.pure", profiles);

        runtime.compile();
        int size = runtime.getModelRepository().serialize().length;

        runtime.getIncrementalCompiler().updateSource(runtime.getSourceById("profiles.pure"), "");
        runtime.getIncrementalCompiler().updateSource(runtime.getSourceById("classes.pure"), "");
        runtime.compile();

        Assert.assertEquals("Graph size mismatch", size, repository.serialize().length);
    }

    @Test
    public void testProcessingTemporalPropertyUnbindStability()
    {
        String a = "Class <<temporal.processingtemporal>> A{version : Integer[1]; b:B[*];}";
        String b = "Class <<temporal.processingtemporal>> B{value : String[1];}";
        String bNoMilestoning = "Class B{}";
        String go = "function go():Any[*]{" +
                "{|A.all(%2016)->filter(a|$a.b.value == '')}" +
                "}";

        runtime.createInMemorySource("A.pure", a);
        runtime.createInMemorySource("B.pure", b);
        runtime.createInMemorySource("go.pure", go);
        runtime.compile();

        RuntimeVerifier.replaceWithCompileErrorCompileAndReloadMultipleTimesIsStable(runtime,
                Lists.fixedSize.of(Tuples.pair("B.pure", bNoMilestoning)), "Can't find the property 'value' in the class B",
                "go.pure", 1, 52);
    }

    @Test
    public void testFunctionExpressionUnbindingOfSynthetizedMilestonedPropertyDoesNotReResolveUnboundProperties()
    {
        String baseClass = "Class <<temporal.businesstemporal>> test::A{id:Integer[0..1];}\n";
        String specializationClassB = "Class <<temporal.businesstemporal>> test::B extends test::A{value:String[0..1];}\n";
        String specializationClassC = "Class <<temporal.businesstemporal>> test::C extends test::A{}\n";
        String associationB_C = "Association test::controllers::B_C{\n" +
                "   b: test::B[*];\n" +
                "   c: test::C[0..1];" +
                "}\n";
        String go = "function test::go():Any[*]{" + "{|test::C.all(%2016)->filter(c|$c.b.value == '')}" + "}";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("/test/baseClass.pure", baseClass)
                        .createInMemorySource("/test/specializationClassB.pure", specializationClassB)
                        .createInMemorySource("/test/specializationClassC.pure", specializationClassC)
                        .createInMemorySource("/test/associationB_C.pure", associationB_C)
                        .createInMemorySource("/test/go.pure", go)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("/test/associationB_C.pure")
                        .deleteSource("/test/go.pure")
                        .compile()
                        .createInMemorySource("/test/associationB_C.pure", associationB_C)
                        .createInMemorySource("/test/go.pure", go)
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
        int size = runtime.getModelRepository().serialize().length;

        runtime.modify("/test/baseClass.pure", baseClass + " ");
        runtime.compile();

        Assert.assertEquals("Graph size mismatch", size, repository.serialize().length);
    }

    @Test
    public void testMilestonedSelfAssociationUnload()
    {
        String classSourceId = "/test/myClass.pure";
        String classSource = "Class <<temporal.businesstemporal>> test::Class1 {}\n";
        String assocSourceId = "/test/myAssociation.pure";
        String assocSource = "Association test::SelfAssoc\n" +
                "{\n" +
                "    parent : test::Class1[1];\n" +
                "    children : test::Class1[*];\n" +
                "}\n";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource(classSourceId, classSource)
                        .createInMemorySource(assocSourceId, assocSource)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource(classSourceId)
                        .deleteSource(assocSourceId)
                        .compile()
                        .createInMemorySource(classSourceId, classSource)
                        .createInMemorySource(assocSourceId, assocSource)
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testSingleTemporalToBiTemporalMilestoningUnload()
    {
        String trader = "Class <<temporal.processingtemporal>> test::Trader {location:test::Location[0..1];}\n";
        String location = "Class <<temporal.bitemporal>> test::Location {place:String[0..1];}\n";
        String go = "function go():Any[*]{" + "{| test::Trader.all(%2015-10-16).location(%latest).place}" + "}";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("trader.pure", trader)
                        .createInMemorySource("location.pure", location)
                        .createInMemorySource("go.pure", go)
                        .compile(),
                new RuntimeTestScriptBuilder().deleteSource("trader.pure").compileIgnoreExceptions().createInMemorySource("trader.pure", trader).compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testBiTemporalToBiTemporalMilestoningUnload()
    {
        String trader = "Class <<temporal.bitemporal>> test::Trader {location:test::Location[0..1];}\n";
        String location = "Class <<temporal.bitemporal>> test::Location {place:String[0..1];}\n";
        String go = "function go():Any[*]{" + "{| test::Trader.all(%9999-12-31, %2015-10-16).location(%2015-10-17).place}" + "}";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("trader.pure", trader)
                        .createInMemorySource("location.pure", location)
                        .createInMemorySource("go.pure", go)
                        .compile(),
                new RuntimeTestScriptBuilder().deleteSource("trader.pure").compileIgnoreExceptions().createInMemorySource("trader.pure", trader).compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testFunctionExpressionBindUnbindViaTemporalClassDependency()
    {
        String businessTemporalTrader = "Class <<temporal.businesstemporal>> test::Trader {location:test::Location[0..1];}\n";
        String location = "Class test::Location {place:String[0..1];}\n";
        String businessTemporalLocation = "Class <<temporal.businesstemporal>> test::Location {place:String[0..1];}\n";

        String go = "function go():Any[*]{" + "{| test::Trader.all(%999-12-31).location.place}" + "}\n";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("trader.pure", businessTemporalTrader)
                        .createInMemorySource("location.pure", location)
                        .createInMemorySource("go.pure", go)
                        .compile(),
                new RuntimeTestScriptBuilder().compile().deleteSource("location.pure").createInMemorySource("location.pure", businessTemporalLocation).compile().deleteSource("location.pure").createInMemorySource("location.pure", location).compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testStabilityOnTemporalStereotypeUpdate()
    {
        String sourceId = "test.pure";
        String sourceCode = "Class <<temporal.businesstemporal>> milestoning::A{aId : Integer[1];}\n";
        String updatedSourceCode = "Class <<temporal.processingtemporal>> milestoning::A{aId : Integer[1];}\n";

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(sourceId, sourceCode)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource(sourceId, updatedSourceCode)
                        .compile()
                        .updateSource(sourceId, sourceCode)
                        .compile(),
                runtime,
                functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnTemporalStereotypeRemoval()
    {
        String sourceId = "test.pure";
        String sourceCode = "Class <<temporal.businesstemporal>> milestoning::A{aId : Integer[1];}\n";
        String updatedSourceCode = "Class milestoning::A{aId : Integer[1];}\n";

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(sourceId, sourceCode)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource(sourceId, updatedSourceCode)
                        .compile()
                        .updateSource(sourceId, sourceCode)
                        .compile(),
                runtime,
                functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnAssociationRemoval()
    {
        String classA = "Class <<temporal.businesstemporal>> milestoning::A{aId : Integer[1];}\n";
        String classB = "Class <<temporal.businesstemporal>> milestoning::B{bId : Integer[1];}\n";
        String associationAB = "Association milestoning::A_B{a : milestoning::A[1];\nb : milestoning::B[1];}\n";

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("classA.pure", classA)
                        .createInMemorySource("classB.pure", classB)
                        .createInMemorySource("associationAB.pure", associationAB)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("associationAB.pure")
                        .compile()
                        .createInMemorySource("associationAB.pure", associationAB)
                        .compile(),
                runtime,
                functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnTemporalStereotypeUpdateWithAssociation()
    {
        String classA = "Class <<temporal.businesstemporal>> milestoning::A{aId : Integer[1];}\n";
        String classB = "Class <<temporal.businesstemporal>> milestoning::B{bId : Integer[1];}\n";
        String associationAB = "Association milestoning::A_B{a : milestoning::A[1];\nb : milestoning::B[1];}\n";
        String updatedClassB = "Class <<temporal.processingtemporal>> milestoning::B{bId : Integer[1];}\n";

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("classA.pure", classA)
                        .createInMemorySource("classB.pure", classB)
                        .createInMemorySource("associationAB.pure", associationAB)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("classB.pure", updatedClassB)
                        .compile()
                        .updateSource("classB.pure", classB)
                        .compile(),
                runtime,
                functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnTemporalStereotypeRemovalWithAssociation()
    {
        String classA = "Class <<temporal.businesstemporal>> milestoning::A{aId : Integer[1];}\n";
        String classB = "Class <<temporal.businesstemporal>> milestoning::B{bId : Integer[1];}\n";
        String associationAb = "Association milestoning::A_B{a : milestoning::A[1];b : milestoning::B[1];}\n";
        String updatedClassB = "Class milestoning::B{bId : Integer[1];}\n";

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("classA.pure", classA)
                        .createInMemorySource("classB.pure", classB)
                        .createInMemorySource("associationAB.pure", associationAb)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("classB.pure", updatedClassB)
                        .compile()
                        .updateSource("classB.pure", classB)
                        .compile(),
                runtime,
                functionExecution,
                this.getAdditionalVerifiers()
        );
    }
}
