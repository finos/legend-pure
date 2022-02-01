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

package org.finos.legend.pure.m3.tests.incremental.measure;

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.RuntimeVerifier;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeMeasure extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("userId.pure");
        runtime.delete("sourceId.pure");
        runtime.delete("testFunc.pure");
        runtime.compile();
    }


    private final String measureSource = "Measure pkg::Mass \n" +
            "{\n" +
            "   *Gram: x -> $x; \n" +
            "   Kilogram: x -> $x*1000;\n" +
            "   Pound: x -> $x*453.59;\n" +
            "}\n";

    private final String updatedMeasure = "Measure pkg::Mass \n" +
            "{\n" +
            "   *Gram: x -> $x; \n" +
            "   Pound: x -> $x*453.59;\n" +
            "}\n";

    private final String nonConvertibleMeasure = "Measure pkg::Currency\n" +
            "{\n" +
            "   USD;\n" +
            "   GBP;\n" +
            "   EUR;\n" +
            "}\n";

    private final String updatedNonConvertibleMeasure = "Measure pkg::Currency\n" +
            "{\n" +
            "   USD;\n" +
            "   EUR;\n" +
            "}\n";

    @Test
    public void testMeasureAsFunctionParameterTypeIncremental() throws Exception
    {
        String testFunc = "function takesInMass(m:pkg::Mass[1]):pkg::Mass[1]{$m}";
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", this.measureSource)
                        .createInMemorySource("testFunc.pure", testFunc)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("pkg::Mass has not been defined!", "testFunc.pure", 1, 43)
                        .createInMemorySource("sourceId.pure", this.measureSource)
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testUnitAsFunctionExpressionParameterIncremental() throws Exception
    {
        String testFunc = "function instantiateUnit():String[1]{let a  = 10 pkg::Mass~Kilogram;'ok';}";
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", this.measureSource)
                        .createInMemorySource("testFunc.pure", testFunc)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("pkg::Mass~Kilogram has not been defined!", "testFunc.pure", 1, 60)
                        .createInMemorySource("sourceId.pure", this.measureSource)
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testDeleteReferencedUnitIncrementalThrowsError() throws Exception
    {
        String testFunc = "function instantiateUnit():String[1]{let a  = 10 pkg::Mass~Kilogram;'ok';}";
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", this.measureSource)
                        .createInMemorySource("testFunc.pure", testFunc)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("pkg::Mass~Kilogram has not been defined!", "testFunc.pure", 1, 60)
                        .createInMemorySource("sourceId.pure", this.updatedMeasure)
                        .compileWithExpectedCompileFailure("pkg::Mass~Kilogram has not been defined!", "testFunc.pure", 1, 60)
                        .updateSource("sourceId.pure", this.measureSource)
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testDeleteUnreferencedUnitIncremental() throws Exception
    {
        String testFunc = "function instantiateUnit():String[1]{let a  = 10 pkg::Mass~Pound;'ok';}";
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", this.measureSource)
                        .createInMemorySource("testFunc.pure", testFunc)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("sourceId.pure", this.updatedMeasure)
                        .compile()
                        .updateSource("sourceId.pure", this.measureSource)
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testCanUpdateIncremental() throws Exception
    {
        String testFunc1 = "function testFunc():Any[0..1]\n" +
                "{\n" +
                "   let a = 10 pkg::Mass~Pound;\n" +
                "}";
        String testFunc2 = "function testFunc():Any[0..1]\n" +
                "{\n" +
                "   let a = 10 pkg::Mass~Pound;\n" +
                "}";

        String source1 = this.measureSource + "\n" + testFunc1;
        String source2 = this.measureSource + "\n" + testFunc2;
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", source1)
                        .compile(), new RuntimeTestScriptBuilder()
                        .updateSource("sourceId.pure", source2)
                        .compile()
                        .updateSource("sourceId.pure", source1)
                        .compile()
                , runtime, functionExecution, this.getAdditionalVerifiers(), false, 1);
    }

    @Test
    public void testUpdateMeasureAsParameterMultiplicityManyIncremental() throws Exception
    {
        String updatedConversionFunc = "Measure pkg::Mass \n" +
                "{\n" +
                "   *Gram: x -> $x; \n" +
                "   Kilogram: x -> $x*100;\n" +
                "   Pound: x -> $x*400;\n" +
                "}\n";
        String testerFunc = "function testerFunc(masses: pkg::Mass[*]):pkg::Mass~Gram[1]\n" +
                "{\n" +
                "   10 pkg::Mass~Gram;\n" +
                "}";
        String testFunc = "function runTest():String[1]{let a  = 10 pkg::Mass~Pound; let b = 5 pkg::Mass~Gram; testerFunc([$a, $b]); 'ok';}";
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", this.measureSource)
                        .createInMemorySource("testFunc.pure", testFunc + testerFunc)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("sourceId.pure", updatedConversionFunc)
                        .compile()
                        .updateSource("sourceId.pure", this.measureSource)
                        .compile()
                        .updateSource("sourceId.pure", this.updatedMeasure)
                        .compile()
                        .updateSource("sourceId.pure", this.measureSource)
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testDeleteReferencedNonConvertibleUnitIncrementalThrowsError() throws Exception
    {
        String testFunc = "function instantiateUnit():String[1]{let a  = 10 pkg::Currency~GBP;'ok';}";
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", this.nonConvertibleMeasure)
                        .createInMemorySource("testFunc.pure", testFunc)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("pkg::Currency~GBP has not been defined!", "testFunc.pure", 1, 64)
                        .createInMemorySource("sourceId.pure", this.updatedNonConvertibleMeasure)
                        .compileWithExpectedCompileFailure("pkg::Currency~GBP has not been defined!", "testFunc.pure", 1, 64)
                        .updateSource("sourceId.pure", this.nonConvertibleMeasure)
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testDeleteUnreferencedNonConvertibleUnitIncremental() throws Exception
    {
        String testFunc = "function instantiateUnit():String[1]{let a  = 10 pkg::Currency~EUR;'ok';}";
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", this.nonConvertibleMeasure)
                        .createInMemorySource("testFunc.pure", testFunc)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("sourceId.pure", this.updatedNonConvertibleMeasure)
                        .compile()
                        .updateSource("sourceId.pure", this.nonConvertibleMeasure)
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }
}
