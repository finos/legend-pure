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

package org.finos.legend.pure.m3.tests.incremental._class;

import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeClass_FunctionParamType extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime() {
        runtime.delete("sourceId.pure");
        runtime.delete("sourceId2.pure");
        runtime.delete("userId.pure");
        runtime.delete("other.pure");
    }

    @Test
    public void testPureRuntimeClassAsFunctionParameterType() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{name:String[1];}")
                        .createInMemorySource("userId.pure", "function f(c:A[0]):A[0]{$c}" +
                                "function test():Boolean[1]{assert(f([])->isEmpty(),|'')}" )
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 20)
                        .createInMemorySource("sourceId.pure", "Class A{name:String[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassAsFunctionParameterTypeError() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "function f(c:A[0]):A[0]{$c}" +
                                "function test():Boolean[1]{assert(f([])->isEmpty(),|'')}" )
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 20)
                        .createInMemorySource("sourceId.pure", "Class B{}")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 20)
                        .updateSource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassAsLambdaParameter() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "function test():Nil[0]{f({a:A[1]|[]->cast(@A)}->eval(^A()));}")
                        .createInMemorySource("other.pure", "function f(a:A[*]):Nil[0]{[];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", null, 1, 14)
                        .createInMemorySource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassAsLambdaParameterError() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "function test():Nil[0]{f({a:A[1]|[]->cast(@A)}->eval(^A()));}" )
                        .createInMemorySource("other.pure", "function f(a:A[*]):Nil[0]{[];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", null, 1, 14)
                        .createInMemorySource("sourceId.pure", "Class B{}")
                        .compileWithExpectedCompileFailure("A has not been defined!", null, 1, 14)
                        .updateSource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }


    @Test
    public void testPureRuntimeClassAsLambdaParameterAndReturn() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "function test():Nil[0]{f({a:A[1]|[]->cast(@A)}->eval(^A()));}" )
                        .createInMemorySource("other.pure", "function f(a:A[*]):Nil[0]{[];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", null, 1, 14)
                        .createInMemorySource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassAsLambdaParameterAndReturnError() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "function test():Nil[0]{f({a:A[1]|$a}->eval(^A()));}")
                        .createInMemorySource("other.pure", "function f(a:A[1]):Nil[0]{[];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", null, 1, 14)
                        .createInMemorySource("sourceId.pure", "Class B{}")
                        .compileWithExpectedCompileFailure("A has not been defined!", null, 1, 14)
                        .updateSource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeClassAsLambdaParameterWithFuncInside() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "function test():Nil[0]{^A()->match(a:A[1]|f($a));}" )
                        .createInMemorySource("other.pure", "function f(a:A[1]):Nil[0]{[];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", null, 1, 14)
                        .createInMemorySource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassAsLambdaParameterWithFuncInsideError() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{} Class B{a:A[1];}")
                        .createInMemorySource("userId.pure", "function test():Nil[0]{^B(a=^A())->match(b:B[1]|f($b.a));}")
                        .createInMemorySource("other.pure", "function f(a:A[1]):Nil[0]{[];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", null, 1, 14)
                        .createInMemorySource("sourceId.pure", "Class C{}")
                        .compileWithExpectedCompileFailure("A has not been defined!", null, 1, 14)
                        .updateSource("sourceId.pure", "Class A{} Class B{a:A[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassAsLambda() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{} function k(a:Any[1]):Nil[0]{[]}")
                        .createInMemorySource("userId.pure", "function test():Nil[0]{k(a:A[1]|f($a));}")
                        .createInMemorySource("other.pure", "function f(a:Any[1]):Nil[0]{[];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 28)
                        .createInMemorySource("sourceId.pure", "Class A{} function k(a:Any[1]):Nil[0]{[]}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassAsFunctionParameterTypeReverse() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("otherId.pure", "Class TK{}")
                        .createInMemorySource("sourceId.pure", "function isContract(p:TK[1]):Nil[0]\n" +
                                "{\n" +
                                "    [];\n" +
                                "}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compile()
                        .createInMemorySource("sourceId.pure", "function isContract(p:TK[1]):Nil[0]\n" +
                                "{\n" +
                                "    [];\n" +
                                "}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassAsQualifiedPropertyParameter() throws Exception
    {
        ImmutableMap<String, String> sources = Maps.immutable.with(
                "sourceId.pure", "Class A{name:String[1];}",
                "sourceId2.pure", "Class B{prop(a:A[1]){$a.name}:String[1];}"
        );
        this.runtime.createInMemorySource("sourceId.pure", sources.get("sourceId.pure"));
        this.runtime.createInMemorySource("sourceId2.pure", sources.get("sourceId2.pure"));
        this.runtime.compile();

        int size = this.repository.serialize().length;
        CoreInstance classA = this.processorSupport.package_getByUserPath("A");
        CoreInstance classB = this.processorSupport.package_getByUserPath("B");
        CoreInstance prop = _Class.getQualifiedPropertiesByName(classB, this.processorSupport).get("prop(A)");
        Assert.assertSame(classA, Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToManyResolved(this.processorSupport.function_getFunctionType(prop), M3Properties.parameters, this.processorSupport).get(1), M3Properties.genericType, M3Properties.rawType, this.processorSupport));

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete("sourceId.pure");
            try
            {
                this.runtime.compile();
                Assert.fail("Expected a compile exception");
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "A has not been defined!", "sourceId2.pure", e);
            }

            this.runtime.createInMemorySource("sourceId.pure", sources.get("sourceId.pure"));
            this.runtime.compile();

            classA = this.processorSupport.package_getByUserPath("A");
            classB = this.processorSupport.package_getByUserPath("B");
            prop = _Class.getQualifiedPropertiesByName(classB, this.processorSupport).get("prop(A)");
            Assert.assertSame(classA, Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToManyResolved(this.processorSupport.function_getFunctionType(prop), M3Properties.parameters, this.processorSupport).get(1), M3Properties.genericType, M3Properties.rawType, this.processorSupport));

            Assert.assertEquals(size, this.repository.serialize().length);
        }
    }
}
