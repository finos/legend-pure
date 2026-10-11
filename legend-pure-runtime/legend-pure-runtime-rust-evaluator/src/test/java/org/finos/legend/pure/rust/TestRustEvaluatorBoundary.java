// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.rust;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.StringCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.rust.pct.RustPlatformGraph;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * The boundary over the bootstrap-only Rust model: graph nodes cross both
 * ways, failures arrive typed, handles are released. The Java graph is the
 * platform loaded from {@code .par}; the only callables on the Rust side are
 * registry natives, reached through their {@code NativeFunction} nodes.
 */
public class TestRustEvaluatorBoundary
{
    private static final String LET_FUNCTION = "meta::pure::functions::lang::letFunction_String_1__T_m__T_m_";
    private static final String ASSERT = "meta::pure::functions::asserts::assert_Boolean_1__Function_1__Boolean_1_";
    private static final String CLASS = "meta::pure::metamodel::type::Class";
    private static final String LAMBDA_FUNCTION = "meta::pure::metamodel::function::LambdaFunction";

    private static PureRustEvaluator evaluator;
    private static ProcessorSupport ps;
    private static ModelRepository repository;

    @BeforeClass
    public static void setUp()
    {
        evaluator = RustPlatformGraph.newEvaluator();
        ps = RustPlatformGraph.processorSupport();
        repository = RustPlatformGraph.modelRepository();
    }

    @AfterClass
    public static void tearDown()
    {
        evaluator.close();
    }

    private static CoreInstance node(String path)
    {
        CoreInstance node = _Package.getByUserPath(path, ps);
        Assert.assertNotNull(path, node);
        return node;
    }

    private static CoreInstance wrap(CoreInstance value)
    {
        return ValueSpecificationBootstrap.wrapValueSpecification(value, true, ps);
    }

    private static CoreInstance wrapAll(CoreInstance... values)
    {
        return ValueSpecificationBootstrap.wrapValueSpecification(Lists.immutable.with(values), true, ps);
    }

    private static ListIterable<? extends CoreInstance> values(CoreInstance result)
    {
        return result.getValueForMetaPropertyToMany(M3Properties.values);
    }

    /**
     * {@code letFunction("x", value)} returns its value: the round-trip vehicle.
     */
    private static CoreInstance roundTrip(CoreInstance value)
    {
        ListIterable<? extends CoreInstance> out = values(evaluator.start(node(LET_FUNCTION), Lists.mutable.with(wrap(repository.newStringCoreInstance("x")), wrap(value))));
        Assert.assertEquals(1, out.size());
        return out.get(0);
    }

    private static CoreInstance firstPctTest()
    {
        ReportScope scope = PlatformCodeRepositoryProvider.essentialFunctions;
        MutableList<CoreInstance> tests = TestCollection.buildPCTTestCollection(scope._package, scope.filePath, ps).getAllTestFunctions();
        Assert.assertFalse(tests.isEmpty());
        return tests.getFirst();
    }

    @Test
    public void primitivesRoundTripWithTheirClassifier()
    {
        assertPrimitive(repository.newBooleanCoreInstance(true), "Boolean", "true");
        assertPrimitive(repository.newIntegerCoreInstance(42L), "Integer", "42");
        assertPrimitive(repository.newFloatCoreInstance(new BigDecimal("2.5")), "Float", "2.5");
        assertPrimitive(repository.newDecimalCoreInstance(new BigDecimal("1.50")), "Decimal", "1.50");
        assertPrimitive(repository.newStringCoreInstance("hello"), "String", "hello");
        assertPrimitive(repository.newDateCoreInstance(DateFunctions.newPureDate(2024)), "Date", "2024");
        assertPrimitive(repository.newDateCoreInstance(DateFunctions.newPureDate(2024, 3)), "Date", "2024-03");
        assertPrimitive(repository.newDateCoreInstance(DateFunctions.newPureDate(2024, 2, 29)), "StrictDate", "2024-02-29");
        assertPrimitive(repository.newDateCoreInstance(DateFunctions.newPureDate(2024, 2, 29, 10, 30, 15, "123")), "DateTime", "2024-02-29T10:30:15.123+0000");
        assertPrimitive(repository.newLatestDateCoreInstance(), "LatestDate", "%latest");
    }

    private static void assertPrimitive(CoreInstance in, String expectedClassifier, String expectedName)
    {
        CoreInstance out = roundTrip(in);
        Assert.assertEquals(expectedClassifier, out.getClassifier().getName());
        Assert.assertEquals(expectedName, out.getName());
    }

    @Test
    public void collectionsRoundTrip()
    {
        CoreInstance three = evaluator.start(node(LET_FUNCTION), Lists.mutable.with(
                wrap(repository.newStringCoreInstance("xs")),
                wrapAll(repository.newIntegerCoreInstance(1L), repository.newIntegerCoreInstance(2L), repository.newIntegerCoreInstance(3L))));
        Assert.assertEquals(Lists.mutable.with("1", "2", "3"), values(three).collect(CoreInstance::getName));

        CoreInstance none = evaluator.start(node(LET_FUNCTION), Lists.mutable.with(wrap(repository.newStringCoreInstance("ys")), wrapAll()));
        Assert.assertTrue(values(none).isEmpty());
    }

    @Test
    public void elementsUnifyAndNodesKeepTheirIdentity()
    {
        CoreInstance classNode = node(CLASS);
        Assert.assertEquals("element", evaluator.crossingKind(classNode));
        Assert.assertSame(classNode, roundTrip(classNode));

        CoreInstance pctTest = firstPctTest();
        Assert.assertEquals("javaNode", evaluator.crossingKind(pctTest));
        Assert.assertSame(pctTest, roundTrip(pctTest));
    }

    @Test
    public void rustObjectsAreCoreInstancesWithAJavaClassifier()
    {
        CoreInstance created = evaluator.create(CLASS, new String[]{"name"}, new CoreInstance[][]{{repository.newStringCoreInstance("X")}});
        Assert.assertTrue(created instanceof RustCoreInstance);
        Assert.assertSame(node(CLASS), created.getClassifier());
        CoreInstance name = created.getValueForMetaPropertyToOne("name");
        Assert.assertTrue(name instanceof StringCoreInstance);
        Assert.assertEquals("X", PrimitiveUtilities.getStringValue(name));
        Assert.assertEquals("X", created.getName());
        Assert.assertTrue(created.getKeys().contains("name"));
        Assert.assertEquals(1L, evaluator.liveHandleCount());

        CoreInstance result = evaluator.start(node(LET_FUNCTION), Lists.mutable.with(wrap(repository.newStringCoreInstance("o")), wrap(created)));
        Assert.assertSame(created, values(result).get(0));
        CoreInstance rawType = result.getValueForMetaPropertyToOne(M3Properties.genericType).getValueForMetaPropertyToOne(M3Properties.rawType);
        Assert.assertSame(node(CLASS), rawType);

        ((RustCoreInstance) created).close();
        Assert.assertEquals(0L, evaluator.liveHandleCount());
        PureRustEvaluationException stale = Assert.assertThrows(PureRustEvaluationException.class, () -> created.getValueForMetaPropertyToOne("name"));
        Assert.assertTrue(stale.getMessage(), stale.getMessage().contains("Stale or unknown JNI handle:"));
    }

    @Test
    public void aPctTestIsFunctionNotFoundWithItsUserPath()
    {
        CoreInstance pctTest = firstPctTest();
        CoreInstance adapter = node(PlatformCodeRepositoryProvider.nativeAdapter.function);
        PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class,
                () -> evaluator.start(pctTest, Lists.mutable.with(wrap(adapter))));
        Assert.assertEquals(PureRustEvaluationException.Kind.FUNCTION_NOT_FOUND, e.getKind());
        String expectedPath = PackageableElement.getUserPathForPackageableElement(pctTest, "::");
        Assert.assertEquals("FunctionNotFound: " + expectedPath, e.getMessage());
    }

    /**
     * A PCT lambda lowers through the bridge and runs: {@code |1 + 2} is 3.
     * The native it calls became a model element named by its mangled
     * signature, so dispatch hit the registry exactly.
     */
    @Test
    public void aPctLambdaLowersAndRuns()
    {
        CoreInstance lambda = findLambda(pctTest(PlatformCodeRepositoryProvider.grammarFunctions, "testSimplePlus"));
        CoreInstance result = evaluator.start(lambda, Lists.immutable.empty());
        Assert.assertEquals("3", values(result).get(0).getName());
    }

    /**
     * A node kind the lowering does not know yet is reported as such, with
     * its classifier, and nothing else is evaluated. A primitive type
     * declared outside the bootstrap is one.
     */
    @Test
    public void anUnlowerableNodeIsUnsupportedByClassifier()
    {
        CoreInstance lambda = findLambda(pctTest(PlatformCodeRepositoryProvider.grammarFunctions, "testEqPrimitiveExtension"));
        PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class,
                () -> evaluator.start(lambda, Lists.immutable.empty()));
        Assert.assertEquals(PureRustEvaluationException.Kind.UNSUPPORTED, e.getKind());
        Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("Unsupported: lowering meta::pure::metamodel::type::PrimitiveType"));
        Assert.assertEquals(e.getMessage().substring("Unsupported: ".length()), e.getInfo());
    }

    /**
     * A class the lambda constructs is lowered with it; the instance comes
     * back as a Rust object whose properties Java reads through the proxy.
     */
    @Test
    public void aClassConstructionLowersTheClassAndBuildsAnInstance()
    {
        CoreInstance lambda = findLambda(pctTest(PlatformCodeRepositoryProvider.grammarFunctions, "testAssignNewInstance"));
        CoreInstance result = evaluator.start(lambda, Lists.immutable.empty());
        CoreInstance person = values(result).get(0);
        Assert.assertEquals("LA_Person", person.getClassifier().getName());
        Assert.assertEquals("John", person.getValueForMetaPropertyToOne("firstName").getName());
        Assert.assertEquals("Doe", person.getValueForMetaPropertyToOne("lastName").getName());
        ((RustCoreInstance) person).close();
    }

    /**
     * A native registered under its key before its body is fished reports
     * {@code NOT_IMPLEMENTED}; the Pure cause keeps the kind prefix, as every
     * Rust-side diagnostic does.
     */
    @Test
    public void aWiredButUnfishedNativeReportsNotImplemented()
    {
        CoreInstance lambda = findLambda(pctTest(PlatformCodeRepositoryProvider.grammarFunctions, "testSinglePlusType"));
        PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class,
                () -> evaluator.start(lambda, Lists.immutable.empty()));
        Assert.assertEquals(e.getMessage(), PureRustEvaluationException.Kind.NOT_IMPLEMENTED, e.getKind());
        Assert.assertEquals("NotImplemented: genericType_Any_MANY__GenericType_1_", e.getMessage());
        Assert.assertEquals("genericType_Any_MANY__GenericType_1_", e.getInfo());
        PureException cause = PureException.findPureException(e);
        Assert.assertNotNull(cause);
        Assert.assertEquals("NotImplemented: genericType_Any_MANY__GenericType_1_", cause.getInfo());
    }

    /**
     * An enum value in a lambda lowers its enumeration, and the native
     * that takes it (`adjust` with a `DurationUnit`) reads the member.
     */
    @Test
    public void anEnumValueLowersItsEnumeration()
    {
        CoreInstance lambda = findLambda(pctTest(PlatformCodeRepositoryProvider.essentialFunctions, "testAdjustByYears"));
        CoreInstance result = evaluator.start(lambda, Lists.immutable.empty());
        Assert.assertEquals("2016", values(result).get(0).getName());
    }

    /**
     * A failure raised inside a lowered lambda is anchored at the Java
     * source span of the call that raised it, and that anchor is what the
     * Pure cause reports; the cause's info is the bare Pure text, which is
     * what {@code assertError} compares.
     */
    @Test
    public void failuresInsideALoweredLambdaCarryTheJavaSpan()
    {
        CoreInstance lambda = findLambda(pctTest(PlatformCodeRepositoryProvider.essentialFunctions, "testAtError"));
        PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class,
                () -> evaluator.start(lambda, Lists.immutable.empty()));
        Assert.assertEquals(PureRustEvaluationException.Kind.EXECUTION_ERROR, e.getKind());
        Assert.assertEquals("ExecutionError: The system is trying to get an element at offset 2 where the collection is of size 2", e.getMessage());
        PureException cause = PureException.findPureException(e);
        Assert.assertNotNull(cause);
        Assert.assertEquals("The system is trying to get an element at offset 2 where the collection is of size 2", cause.getInfo());
        Assert.assertNotNull(cause.getSourceInformation());
        Assert.assertEquals("/platform/pure/essential/collection/index/at.pure", cause.getSourceInformation().getSourceId());
        Assert.assertEquals(lambda.getSourceInformation().getLine(), cause.getSourceInformation().getLine());
        Assert.assertTrue(cause.getSourceInformation().getColumn() > 0);
    }

    /**
     * A Pure-bodied function the lambda calls is lowered with it,
     * transitively, and runs through the evaluator's own dispatch.
     */
    @Test
    public void pureBodiedFunctionsLowerTransitively()
    {
        CoreInstance lambda = findLambda(pctTest(PlatformCodeRepositoryProvider.grammarFunctions, "testLetAsLastStatement"));
        CoreInstance result = evaluator.start(lambda, Lists.immutable.empty());
        Assert.assertEquals("last statement string", values(result).get(0).getName());
    }

    /**
     * A lambda that closes over its test's variables can only run with
     * its context (the interpreted engine's {@code LambdaWithContext});
     * the bare graph node reports what it lacks.
     */
    @Test
    public void aBareLambdaWithOpenVariablesIsUnsupported()
    {
        CoreInstance test = pctTest(PlatformCodeRepositoryProvider.essentialFunctions, "testSimpleSortWithFunctionVariables");
        CoreInstance lambda = findLambdaWithOpenVariables(test);
        Assert.assertNotNull(lambda);
        PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class,
                () -> evaluator.start(lambda, Lists.immutable.empty()));
        Assert.assertEquals(PureRustEvaluationException.Kind.UNSUPPORTED, e.getKind());
        Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("Unsupported: lowering captured variables"));
    }

    private static CoreInstance findLambdaWithOpenVariables(CoreInstance node)
    {
        if (LAMBDA_FUNCTION.equals(PackageableElement.getUserPathForPackageableElement(node.getClassifier()))
                && node.getValueForMetaPropertyToMany(M3Properties.openVariables).notEmpty()
                && node.getValueForMetaPropertyToMany(M3Properties.openVariables).noneSatisfy(v -> "f".equals(v.getName())))
        {
            return node;
        }
        for (String property : new String[]{M3Properties.expressionSequence, M3Properties.parametersValues, M3Properties.values})
        {
            for (CoreInstance child : node.getValueForMetaPropertyToMany(property))
            {
                CoreInstance found = findLambdaWithOpenVariables(child);
                if (found != null)
                {
                    return found;
                }
            }
        }
        return null;
    }

    private static CoreInstance pctTest(ReportScope scope, String simpleName)
    {
        MutableList<CoreInstance> tests = TestCollection.buildPCTTestCollection(scope._package, scope.filePath, ps).getAllTestFunctions();
        CoreInstance test = tests.detect(t -> t.getName().startsWith(simpleName + "_"));
        Assert.assertNotNull(simpleName, test);
        return test;
    }

    /**
     * The first lambda in a test body that closes over nothing: the one a
     * PCT test hands to {@code $f->eval(...)}. An {@code assertError}
     * wrapper lambda captures {@code $f} and is skipped.
     */
    private static CoreInstance findLambda(CoreInstance node)
    {
        if (LAMBDA_FUNCTION.equals(PackageableElement.getUserPathForPackageableElement(node.getClassifier()))
                && node.getValueForMetaPropertyToMany(M3Properties.openVariables).isEmpty())
        {
            return node;
        }
        for (String property : new String[]{M3Properties.expressionSequence, M3Properties.parametersValues, M3Properties.values})
        {
            for (CoreInstance child : node.getValueForMetaPropertyToMany(property))
            {
                CoreInstance found = findLambda(child);
                if (found != null)
                {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    public void assertionFailuresCarryAPureAssertFailExceptionCause()
    {
        PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class,
                () -> evaluator.start(node(ASSERT), Lists.mutable.with(wrap(repository.newBooleanCoreInstance(false)), wrap(repository.newStringCoreInstance("boom")))));
        Assert.assertEquals(PureRustEvaluationException.Kind.ASSERTION_FAILED, e.getKind());
        Assert.assertTrue(e.getCause() instanceof PureAssertFailException);
        Assert.assertNotNull(PureException.findPureException(e));
        Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("AssertionFailed: "));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("boom"));

        CoreInstance ok = evaluator.start(node(ASSERT), Lists.mutable.with(wrap(repository.newBooleanCoreInstance(true)), wrap(repository.newStringCoreInstance("unused"))));
        Assert.assertEquals("true", values(ok).get(0).getName());
    }

    @Test
    public void handlesAreReleasedAcrossTenThousandRoundTrips()
    {
        CoreInstance pctTest = firstPctTest();
        for (int i = 0; i < 10_000; i++)
        {
            CoreInstance created = evaluator.create(CLASS, new String[]{"name"}, new CoreInstance[][]{{repository.newStringCoreInstance("X" + i)}});
            Assert.assertNotNull(created.getClassifier());
            ((RustCoreInstance) created).close();
            Assert.assertSame(pctTest, roundTrip(pctTest));
        }
        Assert.assertEquals(0L, evaluator.liveHandleCount());
    }

    @Test
    public void closedEvaluatorRefusesCalls()
    {
        PureRustEvaluator own = RustPlatformGraph.newEvaluator();
        Assert.assertTrue(own.isFullyInitializedForExecution());
        own.close();
        Assert.assertFalse(own.isFullyInitializedForExecution());
        PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class,
                () -> own.start(node(LET_FUNCTION), Lists.mutable.with(wrap(repository.newStringCoreInstance("x")), wrap(repository.newIntegerCoreInstance(1L)))));
        Assert.assertEquals("Evaluator has been closed", e.getMessage());
        own.close();
    }
}
