package org.finos.legend.pure.runtime.java.compiled.modeling.function;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
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
        setUpRuntime(getFunctionExecution(), PureCodeStorage.createCodeStorage(getCodeStorageRoot(), getCodeRepositories()), JavaModelFactoryRegistryLoader.loader());
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
        return org.eclipse.collections.api.factory.Lists.immutable.with(CodeRepository.newPlatformCodeRepository(),
                GenericCodeRepository.build("test", "test(::.*)?", PlatformCodeRepository.NAME, "system"));
    }
}
