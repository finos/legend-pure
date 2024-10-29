// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.runtime.modeling.function;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tools.test.ToFix;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestFunctionEval extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories())), JavaModelFactoryRegistryLoader.loader());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("/test/testSource1.pure");
        runtime.compile();
    }

    @Test
    @Ignore
    @ToFix
    public void testFunctionEvalWithUnusedResult()
    {
        compileTestSource(
                "/test/testSource1.pure",
                "import test::*;\n" +
                        "\n" +
                        "function test::inspect<T|m>(values:T[m], fn:Function<{T[m]->Any[*]}>[1]):T[m]\n" +
                        "{\n" +
                        "    $fn->eval($values);\n" +
                        "    $values;\n" +
                        "}\n" +
                        "\n" +
                        "function test::test():Any[*]\n" +
                        "{\n" +
                        "  inspect([1, 2, 3, 4], v | $v->map(i | $i->toString())->joinStrings('[', ', ', ']\\n'))\n" +
                        "}\n");
        CoreInstance test = runtime.getFunction("test::test():Any[*]");
        Assert.assertNotNull(test);
        CoreInstance result = functionExecution.start(test, Lists.immutable.empty());
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof InstanceValue);
        Assert.assertEquals(Lists.fixedSize.with(1L, 2L, 3L, 4L), ((InstanceValue) result)._values());
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }


    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        return org.eclipse.collections.api.factory.Lists.immutable.with(CodeRepositoryProviderHelper.findPlatformCodeRepository(),
                GenericCodeRepository.build("test", "test(::.*)?", "platform", "system"));
    }
}
