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

package org.finos.legend.pure.m2.inlinedsl.graph.incremental;

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m3.tests.RuntimeVerifier.FunctionExecutionStateVerifier;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeGraph extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("sourceId.pure");
        runtime.delete("source1.pure");
        runtime.delete("source2.pure");
        runtime.delete("userId.pure");
        runtime.delete("enumSourceId.pure");
    }

    @Test
    public void testPureRuntimeGraphRemoveClass()
    {


        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("source1.pure", "Class XA{version : Integer[1];}")
                        .createInMemorySource("source2.pure", "function test():Boolean[1]{print(#{XA{version}}#,0);true;}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source1.pure", "Class A2{version : Integer[1];}")
                        .compileWithExpectedCompileFailure("XA has not been defined!", "source2.pure", 1, 36)
                        .updateSource("source1.pure", "Class XA{version : Integer[1];}")
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testPureRuntimeGraphDeleteClass()
    {


        String source = "Class XA{version : Integer[1];}";
        String sourceId = "sourceId.pure";
        this.runtime.createInMemorySource(sourceId, source);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{print(#{XA{version}}#,0);true;}");
        this.runtime.compile();

        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(this.runtime, this.functionExecution,
                Lists.fixedSize.of(Tuples.pair(sourceId, source)), "XA has not been defined!", "userId.pure", 1, 36);
    }

    @Test
    public void testPureRuntimeGraphChangeClass()
    {


        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("source1.pure", "Class A{version : Integer[1];}")
                        .createInMemorySource("source2.pure", "function test():Boolean[1]{print(#{A{version}}#,0);true;}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source1.pure", "Class A{vers : Integer[1];}")
                        .compileWithExpectedCompileFailure("The system can't find a match for the property / qualified property: version()", "source2.pure", 1, 38)
                        .updateSource("source1.pure", "Class A{version : Integer[1];}")
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testPureRuntimeGraphAddVariable()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("source1.pure", "Class A{version : Integer[1]; version2(i:Integer[1]){$i+$this.version}:Integer[1];}")
                        .createInMemorySource("source2.pure", "function test():Boolean[1]{print(#{A{version2(0)}}#,0);true;}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source2.pure", "function test():Boolean[1]{let x = 0;print(#{A{version2($x)}}#,0);true;}")
                        .compile()
                        .updateSource("source2.pure", "function test():Boolean[1]{print(#{A{version2(0)}}#,0);true;}")
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testPureRuntimeGraphRemoveVariable()
    {


        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("source1.pure", "Class A{version : Integer[1]; version2(i:Integer[1]){$i+$this.version}:Integer[1];}")
                        .createInMemorySource("source2.pure", "function test():Boolean[1]{let x = 0;print(#{A{version2($x)}}#,0);true;}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source2.pure", "function test():Boolean[1]{print(#{A{version2(0)}}#,0);true;}")
                        .compile()
                        .updateSource("source2.pure", "function test():Boolean[1]{let x = 0;print(#{A{version2($x)}}#,0);true;}")
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testPureRuntimeGraphRemoveSubType()
    {


        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("source1.pure", "Class A{version : Integer[1];b:B[1];}Class B{} Class XC extends B{}")
                        .createInMemorySource("source2.pure", "function test():Boolean[1]{print(#{A{version,b->subType(@XC)}}#,0);true;}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source1.pure", "Class A{version : Integer[1];b:B[1];}Class B{}")
                        .compileWithExpectedCompileFailure("XC has not been defined!", "source2.pure", 1, 58)
                        .updateSource("source1.pure", "Class A{version : Integer[1];b:B[1];}Class B{} Class XC extends B{}")
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testPureRuntimeGraphChangeSubType()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("source1.pure", "Class A{version : Integer[1];b:B[1];}Class B{} Class C extends B{c:String[1];}")
                        .createInMemorySource("source2.pure", "function test():Boolean[1]{print(#{A{version,b->subType(@C){c}}}#,0);true;}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source1.pure", "Class A{version : Integer[1];b:B[1];}Class B{} Class C extends B{j:String[1];}")
                        .compileWithExpectedCompileFailure("The system can't find a match for the property / qualified property: c()", "source2.pure", 1, 61)
                        .updateSource("source1.pure", "Class A{version : Integer[1];b:B[1];}Class B{} Class C extends B{c:String[1];}")
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testPureRuntimeGraphQualifiedPropertyWithEnumCompileError()
    {
        String source = "Class EntityWithLocations\n" +
                "{\n" +
                "    locations : Location[*];\n" +
                "    locationsByType(types:GeographicEntityType[*])\n" +
                "    {\n" +
                "        $this.locations->filter(l | $types->exists(type | is($l.type, $type)))\n" +
                "    }:Location[*];\n" +
                "}\n" +
                "Class Location\n" +
                "{\n" +
                "    type : GeographicEntityType[1];\n" +
                "}\n";

        String enumSource = "Enum GeographicEntityType\n" +
                "{\n" +
                "    CITY,\n" +
                "    COUNTRY\n" +
                "}";
        String sourceId = "sourceId.pure";
        String enumSourceId = "enumSourceId.pure";
        this.runtime.createInMemorySource(sourceId, source);
        this.runtime.createInMemorySource(enumSourceId, enumSource);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{print(#{EntityWithLocations{locationsByType(GeographicEntityType.CITY)}}#,0);true;}");
        this.runtime.compile();

        String enumSourceChange = "Enum GeographicEntityType\n" +
                "{\n" +
                "    CTIY,\n" +
                "    COUNTRY\n" +
                "}";

        RuntimeVerifier.replaceWithCompileErrorCompileAndReloadMultipleTimesIsStable(this.runtime,
                Lists.fixedSize.of(Tuples.pair(enumSourceId, enumSourceChange)), "The enum value 'CITY' can't be found in the enumeration GeographicEntityType", "userId.pure", 1, 93);
    }

    @Test
    public void testPureRuntimeChangeEnumeration()
    {
        String source = "Class EntityWithLocations\n" +
                "{\n" +
                "    locations : Location[*];\n" +
                "    locationsByType(types:GeographicEntityType[*])\n" +
                "    {\n" +
                "        $this.locations->filter(l | $types->exists(type | is($l.type, $type)))\n" +
                "    }:Location[*];\n" +
                "}\n" +
                "Class Location\n" +
                "{\n" +
                "    type : GeographicEntityType[1];\n" +
                "}\n";

        String enumSource = "Enum GeographicEntityType\n" +
                "{\n" +
                "    CITY,\n" +
                "    COUNTRY\n" +
                "}";

        String enumSource2 = "Enum GeographicEntityType\n" +
                "{\n" +
                "    CITY,\n" +
                "    COUNTRY,\n" +
                "    REGION\n" +
                "}";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("source1.pure", source)
                        .createInMemorySource("source2.pure", enumSource)
                        .createInMemorySource("userId.pure", "function test():Boolean[1]{print(#{EntityWithLocations{locationsByType(GeographicEntityType.CITY)}}#,0);true;}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source2.pure", enumSource2)
                        .compile()
                        .updateSource("source2.pure", enumSource)
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testPureRuntimeAddEnumVar()
    {
        String source = "Class EntityWithLocations\n" +
                "{\n" +
                "    locations : Location[*];\n" +
                "    locationsByType(types:GeographicEntityType[*])\n" +
                "    {\n" +
                "        $this.locations->filter(l | $types->exists(type | is($l.type, $type)))\n" +
                "    }:Location[*];\n" +
                "}\n" +
                "Class Location\n" +
                "{\n" +
                "    type : GeographicEntityType[1];\n" +
                "}\n";

        String enumSource = "Enum GeographicEntityType\n" +
                "{\n" +
                "    CITY,\n" +
                "    COUNTRY\n" +
                "}";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("source1.pure", source)
                        .createInMemorySource("source2.pure", enumSource)
                        .createInMemorySource("userId.pure", "function test():Boolean[1]{print(#{EntityWithLocations{locationsByType(GeographicEntityType.CITY)}}#,0);true;}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("userId.pure", "function test():Boolean[1]{let x = GeographicEntityType.CITY; print(#{EntityWithLocations{locationsByType($x)}}#,0);true;}")
                        .compile()
                        .updateSource("userId.pure", "function test():Boolean[1]{print(#{EntityWithLocations{locationsByType(GeographicEntityType.CITY)}}#,0);true;}")
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testPureRuntimeChangeEnum()
    {
        String source = "Class EntityWithLocations\n" +
                "{\n" +
                "    locations : Location[*];\n" +
                "    locationsByType(types:GeographicEntityType[*])\n" +
                "    {\n" +
                "        $this.locations->filter(l | $types->exists(type | is($l.type, $type)))\n" +
                "    }:Location[*];\n" +
                "}\n" +
                "Class Location\n" +
                "{\n" +
                "    type : GeographicEntityType[1];\n" +
                "}\n";

        String enumSource = "Enum GeographicEntityType\n" +
                "{\n" +
                "    CITY,\n" +
                "    COUNTRY\n" +
                "}";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("source1.pure", source)
                        .createInMemorySource("source2.pure", enumSource)
                        .createInMemorySource("userId.pure", "function test():Boolean[1]{print(#{EntityWithLocations{locationsByType(GeographicEntityType.CITY)}}#,0);true;}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("userId.pure", "function test():Boolean[1]{print(#{EntityWithLocations{locationsByType(GeographicEntityType.COUNTRY)}}#,0);true;}")
                        .compile()
                        .updateSource("userId.pure", "function test():Boolean[1]{print(#{EntityWithLocations{locationsByType(GeographicEntityType.CITY)}}#,0);true;}")
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testPureRuntimeGraphRemoveTree()
    {


        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("source1.pure", "Class A{version : Integer[1];}")
                        .createInMemorySource("source2.pure", "function test():Boolean[1]{print(#{A{version}}#,0);true;}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source2.pure", "function test():Boolean[1]{print('Hello',0);true;}")
                        .compile()
                        .updateSource("source2.pure", "function test():Boolean[1]{print(#{A{version}}#,0);true;}")
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testPureRuntimeGraphChangeTreeAddProperty()
    {


        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("source1.pure", "Class A{version : Integer[1]; version2: Integer[1];}")
                        .createInMemorySource("source2.pure", "function test():Boolean[1]{print(#{A{version}}#,0);true;}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source2.pure", "function test():Boolean[1]{print(#{A{version2}}#,0);true;}")
                        .compile()
                        .updateSource("source2.pure", "function test():Boolean[1]{print(#{A{version}}#,0);true;}")
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testPureRuntimeGraphChangeTreeMainClass()
    {


        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("source1.pure", "Class A{version : Integer[1]; version2: Integer[1];} Class A2{version : Integer[1]; version2: Integer[1];}")
                        .createInMemorySource("source2.pure", "function test():Boolean[1]{print(#{A{version}}#,0);true;}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source2.pure", "function test():Boolean[1]{print(#{A2{version2}}#,0);true;}")
                        .compile()
                        .updateSource("source2.pure", "function test():Boolean[1]{print(#{A{version}}#,0);true;}")
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testPureRuntimeGraphChangeSubTypeClass()
    {


        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("source1.pure", "Class A{version : Integer[1];b:B[1];}Class B{} Class C extends B{} Class D extends B{}")
                        .createInMemorySource("source2.pure", "function test():Boolean[1]{print(#{A{version,b->subType(@C)}}#,0);true;}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source2.pure", "function test():Boolean[1]{print(#{A{version,b->subType(@D)}}#,0);true;}")
                        .compile()
                        .updateSource("source2.pure", "function test():Boolean[1]{print(#{A{version,b->subType(@C)}}#,0);true;}")
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
    }
}
