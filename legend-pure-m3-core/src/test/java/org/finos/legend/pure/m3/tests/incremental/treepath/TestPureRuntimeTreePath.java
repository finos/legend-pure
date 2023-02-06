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

package org.finos.legend.pure.m3.tests.incremental.treepath;

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m3.tests.RuntimeVerifier.FunctionExecutionStateVerifier;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeTreePath extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime() {
        runtime.delete("userId.pure");
        runtime.delete("sourceId.pure");
        runtime.delete("funcId.pure");
        runtime.delete("profile.pure");
        runtime.delete("treeSourceId.pure");
        runtime.delete("enumSourceId.pure");
    }

    @Test
    public void testPureRuntimeProperty()
    {
        String source = "Class A{version : Integer[1];}";
        String sourceId = "sourceId.pure";
        this.runtime.createInMemorySource(sourceId, source);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{print(#A{+[version]}#,0);true;}");
        this.runtime.compile();

        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(this.runtime, this.functionExecution,
                Lists.fixedSize.of(Tuples.pair(sourceId, source)), "A has not been defined!", "userId.pure", 1, 35);
    }

    @Test
    public void testPureRuntimePropertyChange()
    {
        String source = "Class A{version : Integer[1];}";
        String badSource = "Class A{vers : Integer[1];}";
        String sourceId = "sourceId.pure";
        this.runtime.createInMemorySource(sourceId, source);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{print(#A{+[version]}#,0);true;}");
        this.runtime.compile();

        RuntimeVerifier.replaceWithCompileErrorCompileAndReloadMultipleTimesIsStable(this.runtime,
                Lists.fixedSize.of(Tuples.pair(sourceId, badSource)), "The property 'version' can't be found in the type 'A' (or any supertype).",
                "userId.pure", 1, 39);

    }

    @Test
    public void testPureRuntimePropertyChangeReferencedFromDerivedProperty()
    {
        String source = "Class A{version : Integer[1];}";
        String badSource = "Class A{vers : Integer[1];}";
        String sourceId = "sourceId.pure";
        this.runtime.createInMemorySource(sourceId, source);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{print(#A{>myVersion[$this.version]}#,0);true;}");
        this.runtime.compile();

        RuntimeVerifier.replaceWithCompileErrorCompileAndReloadMultipleTimesIsStable(this.runtime,
                Lists.fixedSize.of(Tuples.pair(sourceId, badSource)), "Can't find the property 'version' in the class A",
                "userId.pure", 1, 54);

    }

    @Test
    public void testPureRuntimeFunctionChangeReferencedFromDerivedProperty()
    {
        String source = "Class A{version : String[1];}\n";
        String functionSource = "function nice(str:String[1]):String[1] \n" +
                "{" +
                "   'nice ' + $str;" +
                "}";
        String badSource = "";
        String sourceId = "sourceId.pure";
        String funcId = "funcId.pure";
        this.runtime.createInMemorySource(sourceId, source);
        this.runtime.createInMemorySource(funcId, functionSource);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{print(#A{>myVersion[$this.version->nice()]}#,0);true;}");
        this.runtime.compile();

        RuntimeVerifier.replaceWithCompileErrorCompileAndReloadMultipleTimesIsStable(this.runtime,
                Lists.fixedSize.of(Tuples.pair(funcId, badSource)), "The system can't find a match for the function: nice(_:String[1])",
                "userId.pure", 1, 63);

    }

    @Test
    public void testPureRuntimeWithQualifiedPropertyWithEnum()
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
                "}\n" +
                "Enum GeographicEntityType\n" +
                "{\n" +
                "    CITY,\n" +
                "    COUNTRY\n" +
                "}";
        String sourceId = "sourceId.pure";
        this.runtime.createInMemorySource(sourceId, source);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{print(#EntityWithLocations{locationsByType(GeographicEntityType[*]){*}}#,0);true;}");
        this.runtime.compile();

        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(this.runtime, this.functionExecution,
                Lists.fixedSize.of(Tuples.pair(sourceId, source)), "EntityWithLocations has not been defined!", "userId.pure", 1, 35);
    }

    @Test
    public void testTreePathWithSelfReferences() throws Exception
    {
        String source = "Class Person{ name: String[1];address:Address[1]; firm: Firm[1]; manager: Person[0..1]; } Class Firm {employees : Person[1];address:Address[1];} Class Address{ street:String[1]; }\n";
        String treeSource = "function test():Any[*]\n" +
                "{\n" +
                " let t = " +
                "#Person as SP" +
                "{ \n" +
                "      * \n" +
                "      manager as Manager\n " +
                "      {     \n " +
                "         +[name]    \n " +
                "         address as BigHome \n " +
                "      }     \n " +
                "      address as Home \n" +
                "}#;" +
                "}\n";
        String userId = "userId.pure";
        String sourceId = "sourceId.pure";
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.with(sourceId, source, userId, treeSource))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource(userId)
                        .compile()
                        .createInMemorySource(userId, treeSource)
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
    }


    @Test
    public void testPureRuntimeWithTreeRemoval()
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
                "}\n" +
                "Enum GeographicEntityType\n" +
                "{\n" +
                "    CITY,\n" +
                "    COUNTRY\n" +
                "}";
        String sourceId = "sourceId.pure";
        String treeSource = "function test():Boolean[1]{print(#EntityWithLocations{locationsByType(GeographicEntityType[*]){*}}#,0);true;}";
        String userId = "userId.pure";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.with(sourceId, source, userId, treeSource))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource(userId)
                        .compile()
                        .createInMemorySource(userId, treeSource)
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testPureRuntimeTreeWithStereotypeRemoval()
    {
        String profile = "Profile TestProfile\n" +
                "{\n" +
                "   stereotypes : [ Root, NewProp, ExistingProp ];\n" +
                "   tags : [ Id, Name, Description ];\n" +
                "}\n";
        String source =
                "Class EntityWithLocations\n" +
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
                        "}\n" +
                        "Enum GeographicEntityType\n" +
                        "{\n" +
                        "    CITY,\n" +
                        "    COUNTRY\n" +
                        "}";
        String sourceId = "sourceId.pure";
        String treeSource = "function test():Boolean[1]{print(#EntityWithLocations<<TestProfile.Root>>{locationsByType(GeographicEntityType[*]){*}}#,0);true;}";
        String treeSourceId = "treeSourceId.pure";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.with(sourceId, source, treeSourceId, treeSource, "profile.pure", profile))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("profile.pure")
                        .compileWithExpectedCompileFailure("TestProfile has not been defined!", treeSourceId, 1, 68)
                        .deleteSource(treeSourceId)
                        .createInMemorySource("profile.pure", profile)
                        .createInMemorySource(treeSourceId, treeSource)
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testAnnotatedChildNodeWithStereotypeRemoval()
    {
        String profile = "Profile TestProfile\n" +
                "{\n" +
                "   stereotypes : [ Root, NewProp, ExistingProp ];\n" +
                "   tags : [ Id, Name, Description ];\n" +
                "}\n";
        String source =
                "Class EntityWithLocations\n" +
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
                        "}\n" +
                        "Enum GeographicEntityType\n" +
                        "{\n" +
                        "    CITY,\n" +
                        "    COUNTRY\n" +
                        "}";
        String sourceId = "sourceId.pure";
        String treeSource = "function test():Boolean[1]{print(#EntityWithLocations<<TestProfile.Root>>{ \n" +
                " * \n" +
                " locationsByType(GeographicEntityType[*]){*} \n" +
                " locations <<TestProfile.ExistingProp>> {*}" +
                "\n" +
                "}#,0);true;}";
        String treeSourceId = "treeSourceId.pure";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.with(sourceId, source, treeSourceId, treeSource, "profile.pure", profile))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("profile.pure")
                        .compileWithExpectedCompileFailure("TestProfile has not been defined!", treeSourceId, 1, 68)
                        .deleteSource(treeSourceId)
                        .createInMemorySource("profile.pure", profile)
                        .createInMemorySource(treeSourceId, treeSource)
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testPureRuntimeWithQualifiedPropertyWithEnumCompileError()
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
        String userId = "userId.pure";
        String treePath = "function test():Boolean[1]{print(#EntityWithLocations{locationsByType(GeographicEntityType[*]){*}}#,0);true;}";

        String enumSourceChange = "Enum GeographicEntityType1\n" +
                "{\n" +
                "    CTIY,\n" +
                "    COUNTRY\n" +
                "}";
        String invalidEnumSourceId = "invalidEnumSourceId.pure";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.with(sourceId, source, enumSourceId, enumSource, userId, treePath))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource(enumSourceId)
                        .createInMemorySource(invalidEnumSourceId, enumSourceChange)
                        .compileWithExpectedCompileFailure("GeographicEntityType has not been defined!", sourceId, 11, 12)
                        .deleteSource(invalidEnumSourceId)
                        .createInMemorySource(enumSourceId, enumSource)
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testPureRuntimeWithStereotypesAndTaggedValues()
    {
        String source =
                "Profile TestProfile\n" +
                        "{\n" +
                        "   stereotypes : [ Root, NewProp, ExistingProp ];\n" +
                        "   tags : [ Id, Name, Description ];\n" +
                        "}" +
                        "Class EntityWithLocations\n" +
                        "{\n" +
                        "    name : String[1];\n" +
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
        String userId = "userId.pure";
        String treePath = "function test():Boolean[1]{print(#EntityWithLocations<<TestProfile.Root>>{TestProfile.Id='rootNode'}{ +[name<<TestProfile.ExistingProp>>] \nlocationsByType(GeographicEntityType[*])<<TestProfile.ExistingProp>>{TestProfile.Name='enum'}{*}}#,0);true;}";

        String enumSourceChange = "Enum GeographicEntityType1\n" +
                "{\n" +
                "    CTIY,\n" +
                "    COUNTRY\n" +
                "}";
        String invalidEnumSourceId = "invalidEnumSourceId.pure";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.with(sourceId, source, enumSourceId, enumSource, userId, treePath))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource(enumSourceId)
                        .createInMemorySource(invalidEnumSourceId, enumSourceChange)
                        .compileWithExpectedCompileFailure("GeographicEntityType has not been defined!", sourceId, 16, 12)
                        .deleteSource(invalidEnumSourceId)
                        .createInMemorySource(enumSourceId, enumSource)
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<FunctionExecutionStateVerifier>of());
    }
}

