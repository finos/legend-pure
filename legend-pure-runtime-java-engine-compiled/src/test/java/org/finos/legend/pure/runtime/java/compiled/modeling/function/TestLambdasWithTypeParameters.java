package org.finos.legend.pure.runtime.java.compiled.modeling.function;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestLambdasWithTypeParameters extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), PureCodeStorage.createCodeStorage(getCodeStorageRoot(), getCodeRepositories()));
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("/test/testSource1.pure");
        runtime.compile();
    }

    @Test
    public void testLambdaWithNotFullyConcreteReturnType()
    {
        compileTestSource(
                "/test/testSource1.pure",
                "import test::*;\n" +
                        "\n" +
                        "Class test::Result<T|m>\n" +
                        "{\n" +
                        "  values:T[m];\n" +
                        "  other:Any[*];\n" +
                        "}\n" +
                        "\n" +
                        "function test::singleOther<T>(other:Any[1], object:T[0..1]):Result<T|0..1>[1]\n" +
                        "{\n" +
                        "  ^Result<T|0..1>(other=$other)\n" +
                        "}\n" +
                        "\n" +
                        "function test::singleValue<T>(value:T[1]):Result<T|0..1>[1]\n" +
                        "{\n" +
                        "  ^Result<T|0..1>(values=$value)\n" +
                        "}\n" +
                        "\n" +
                        "function test::expand<T>(result:Result<T|*>[1]):Result<T|0..1>[*]\n" +
                        "{\n" +
                        "  if($result.values->isEmpty(),\n" +
                        "     | $result.other->map(o | singleOther($o, @T)),\n" +
                        "     | $result.values->map(v | singleValue($v)))\n" +
                        "}\n" +
                        "\n" +
                        "function test::test():Any[*]\n" +
                        "{\n" +
                        "  expand(^Result<String|0>(other=[1, 2, 3, 4]))\n" +
                        "}\n");
        CoreInstance test = runtime.getFunction("test::test():Any[*]");
        Assert.assertNotNull(test);
        CoreInstance result = functionExecution.start(test, Lists.immutable.empty());
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof InstanceValue);
        Assert.assertEquals(4, ((InstanceValue) result)._values().size());
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
