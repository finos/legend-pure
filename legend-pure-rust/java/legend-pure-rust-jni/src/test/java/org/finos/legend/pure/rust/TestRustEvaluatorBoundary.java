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
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.StringCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.rust.pct.RustPlatformGraph;
import org.finos.legend.pure.runtime.java.interpreted.natives.MapCoreInstance;
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
        evaluator = newEvaluator();
        ps = RustPlatformGraph.processorSupport();
        repository = RustPlatformGraph.modelRepository();
    }

    @AfterClass
    public static void tearDown()
    {
        evaluator.close();
    }

    private static PureRustEvaluator newEvaluator()
    {
        PureRustEvaluator created = new PureRustEvaluator();
        created.init(RustPlatformGraph.runtime(), new Message(""));
        return created;
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

    /**
     * A graph node that is neither a value nor a packageable element: the
     * first expression of a PCT test's body.
     */
    private static CoreInstance anExpressionNode()
    {
        CoreInstance expression = firstPctTest().getValueForMetaPropertyToMany(M3Properties.expressionSequence).getFirst();
        Assert.assertNotNull(expression);
        return expression;
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

        CoreInstance expression = anExpressionNode();
        Assert.assertEquals("javaNode", evaluator.crossingKind(expression));
        Assert.assertSame(expression, roundTrip(expression));
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

    /**
     * A proxy Java no longer reaches is released on the evaluator's next
     * call: the interpreted engine reads a result through its proxy and
     * never closes it, and the Rust object must not outlive it.
     */
    @Test
    public void aProxyJavaNoLongerReachesIsReleasedOnTheNextCall() throws InterruptedException
    {
        evaluator.create("meta::pure::functions::collection::Pair", new String[]{"first", "second"}, new CoreInstance[][]{{repository.newIntegerCoreInstance(1L)}, {repository.newStringCoreInstance("a")}});
        Assert.assertEquals(1L, evaluator.liveHandleCount());
        for (int i = 0; (i < 100) && (evaluator.liveHandleCount() > 0); i++)
        {
            System.gc();
            Thread.sleep(10);
            evaluator.start(node(LET_FUNCTION), Lists.mutable.with(wrap(repository.newStringCoreInstance("x")), wrap(repository.newIntegerCoreInstance(1L))));
        }
        Assert.assertEquals(0L, evaluator.liveHandleCount());
    }

    /**
     * A Rust object that is not a model element has a name as a Java
     * instance does: anonymous in the repository's form, and its own.
     * Pure's {@code id()} (and with it {@code toRepresentation}, which
     * {@code assertEquals} calls on every collection it compares) reads it.
     */
    @Test
    public void aRustObjectThatIsNotAModelElementHasAnAnonymousName()
    {
        String pair = "meta::pure::functions::collection::Pair";
        CoreInstance first = evaluator.create(pair, new String[]{"first", "second"}, new CoreInstance[][]{{repository.newIntegerCoreInstance(1L)}, {repository.newStringCoreInstance("a")}});
        CoreInstance second = evaluator.create(pair, new String[]{"first", "second"}, new CoreInstance[][]{{repository.newIntegerCoreInstance(1L)}, {repository.newStringCoreInstance("a")}});
        Assert.assertNotNull(first.getName());
        Assert.assertTrue(first.getName(), ModelRepository.isAnonymousInstanceName(first.getName()));
        Assert.assertNotEquals(first.getName(), second.getName());
        ((RustCoreInstance) first).close();
        ((RustCoreInstance) second).close();
    }

    /**
     * A map built in Rust comes back as the interpreted engine's own
     * {@link MapCoreInstance}, its entries materialised like any value, so
     * the engine's equality and lookups apply. A Java map handed to a Rust
     * map native does not cross as a map: the native refuses it, typed.
     */
    @Test
    public void aRustMapComesBackAsTheEnginesMapAndAJavaMapDoesNotCrossIn()
    {
        String pair = "meta::pure::functions::collection::Pair";
        CoreInstance one = evaluator.create(pair, new String[]{"first", "second"}, new CoreInstance[][]{{repository.newStringCoreInstance("a")}, {repository.newIntegerCoreInstance(1L)}});
        CoreInstance two = evaluator.create(pair, new String[]{"first", "second"}, new CoreInstance[][]{{repository.newStringCoreInstance("b")}, {repository.newIntegerCoreInstance(2L)}});
        CoreInstance result = values(evaluator.start(node("meta::pure::functions::collection::newMap_Pair_MANY__Map_1_"), Lists.mutable.with(wrapAll(one, two)))).get(0);
        ((RustCoreInstance) one).close();
        ((RustCoreInstance) two).close();
        Assert.assertTrue(result.getClass().getName(), result instanceof MapCoreInstance);
        MapCoreInstance map = (MapCoreInstance) result;
        Assert.assertEquals(2, map.getMap().size());
        Assert.assertEquals("2", map.getMap().get(repository.newStringCoreInstance("b")).getName());
        Assert.assertSame(node(M3Paths.Map), map.getClassifier());

        PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class,
                () -> evaluator.start(node("meta::pure::functions::collection::keys_Map_1__U_MANY_"), Lists.mutable.with(wrap(map))));
        Assert.assertEquals(e.getMessage(), PureRustEvaluationException.Kind.EXECUTION_ERROR, e.getKind());
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("expected Map"));
        Assert.assertEquals(0L, evaluator.liveHandleCount());
    }

    /**
     * A function with a body handed to {@code start} is lowered with
     * everything it calls, and so are its arguments: a PCT test run with
     * the In-Memory adapter (itself a function with a body) runs entirely
     * in Rust and returns its result.
     */
    @Test
    public void aConcreteFunctionDefinitionLowersAndRunsWithItsArguments()
    {
        CoreInstance pctTest = pctTest(PlatformCodeRepositoryProvider.grammarFunctions, "testSimplePlus");
        CoreInstance adapter = node(PlatformCodeRepositoryProvider.nativeAdapter.function);
        CoreInstance result = evaluator.start(pctTest, Lists.mutable.with(wrap(adapter)));
        Assert.assertEquals("true", values(result).get(0).getName());
    }

    /**
     * A packageable element that is not a function cannot be started: it is
     * reported as a function not found, with its user path.
     */
    @Test
    public void anElementThatIsNotAFunctionIsFunctionNotFoundWithItsUserPath()
    {
        String profile = "meta::pure::profiles::doc";
        PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class,
                () -> evaluator.start(node(profile), Lists.immutable.empty()));
        Assert.assertEquals(PureRustEvaluationException.Kind.FUNCTION_NOT_FOUND, e.getKind());
        Assert.assertEquals("FunctionNotFound: " + profile, e.getMessage());
    }

    /**
     * A PCT lambda lowers through the bridge and runs: {@code |1 + 2} is 3.
     */
    @Test
    public void aPctLambdaLowersAndRuns()
    {
        CoreInstance lambda = findLambda(pctTest(PlatformCodeRepositoryProvider.grammarFunctions, "testSimplePlus"));
        CoreInstance result = evaluator.start(lambda, Lists.immutable.empty());
        Assert.assertEquals("3", values(result).get(0).getName());
    }

    /**
     * A lambda is lowered once per definition: the second call takes its
     * body from the closure cache and reads nothing from Java to lower it,
     * with the same result.
     */
    @Test
    public void aLambdaIsLoweredOnceAndRunsAgainFromTheClosureCache()
    {
        CoreInstance lambda = findLambda(pctTest(PlatformCodeRepositoryProvider.grammarFunctions, "testSimpleMinus"));
        CoreInstance first = evaluator.start(lambda, Lists.immutable.empty());
        Assert.assertTrue(PureRustEvaluator.lastTimings()[3] > 0L);
        CoreInstance second = evaluator.start(lambda, Lists.immutable.empty());
        Assert.assertEquals(0L, PureRustEvaluator.lastTimings()[3]);
        Assert.assertEquals(values(first).get(0).getName(), values(second).get(0).getName());
    }

    /**
     * A node kind the lowering does not know yet is reported as such, with
     * its classifier, and nothing else is evaluated. A unit of a measure
     * is one.
     */
    @Test
    public void anUnlowerableNodeIsUnsupportedByClassifier()
    {
        CoreInstance lambda = findLambda(node("meta::pure::functions::meta::tests::newUnit::testNewUnitIndirectUnit__Boolean_1_"));
        PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class,
                () -> evaluator.start(lambda, Lists.immutable.empty()));
        Assert.assertEquals(e.getMessage(), PureRustEvaluationException.Kind.UNSUPPORTED, e.getKind());
        Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("Unsupported: lowering meta::pure::metamodel::type::Unit"));
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
        CoreInstance lambda = findLambda(pctTest(PlatformCodeRepositoryProvider.essentialFunctions, "testMatchWithMixedReturnType"));
        PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class,
                () -> evaluator.start(lambda, Lists.immutable.empty()));
        Assert.assertEquals(e.getMessage(), PureRustEvaluationException.Kind.NOT_IMPLEMENTED, e.getKind());
        Assert.assertEquals("NotImplemented: deactivate_Any_MANY__ValueSpecification_1_", e.getMessage());
        Assert.assertEquals("deactivate_Any_MANY__ValueSpecification_1_", e.getInfo());
        PureException cause = PureException.findPureException(e);
        Assert.assertNotNull(cause);
        Assert.assertEquals("NotImplemented: deactivate_Any_MANY__ValueSpecification_1_", cause.getInfo());
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
        CoreInstance expression = anExpressionNode();
        Assert.assertSame(expression, roundTrip(expression));
        int knownBefore = evaluator.knownNodeCount();
        for (int i = 0; i < 10_000; i++)
        {
            CoreInstance created = evaluator.create(CLASS, new String[]{"name"}, new CoreInstance[][]{{repository.newStringCoreInstance("X" + i)}});
            Assert.assertNotNull(created.getClassifier());
            ((RustCoreInstance) created).close();
            Assert.assertSame(expression, roundTrip(expression));
        }
        Assert.assertEquals(0L, evaluator.liveHandleCount());
        Assert.assertEquals(knownBefore, evaluator.knownNodeCount());
    }

    /**
     * A Java exception raised while the bridge serves the Rust side is
     * taken and cleared before any other JNI call, arrives as a typed
     * failure carrying its text, and leaves the evaluator usable.
     */
    @Test
    public void aJavaFailureInsideTheBridgeArrivesTypedAndLeavesTheEvaluatorUsable()
    {
        PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class, () -> evaluator.crossingKind(null));
        Assert.assertEquals(PureRustEvaluationException.Kind.EXECUTION_ERROR, e.getKind());
        Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("ExecutionError: describe: java.lang.NullPointerException"));
        Assert.assertEquals("42", roundTrip(repository.newIntegerCoreInstance(42L)).getName());
    }

    /**
     * The bridge keeps an id only for the Java nodes the Rust side still
     * holds: hundreds of distinct nodes crossing one call at a time leave
     * its tables as they were.
     */
    @Test
    public void theBridgeForgetsTheNodesRustLetGoOf()
    {
        ReportScope scope = PlatformCodeRepositoryProvider.essentialFunctions;
        MutableList<CoreInstance> expressions = Lists.mutable.empty();
        for (CoreInstance test : TestCollection.buildPCTTestCollection(scope._package, scope.filePath, ps).getAllTestFunctions())
        {
            test.getValueForMetaPropertyToMany(M3Properties.expressionSequence).forEach(expressions::add);
        }
        Assert.assertTrue(expressions.size() > 300);
        int knownBefore = evaluator.knownNodeCount();
        for (CoreInstance expression : expressions)
        {
            Assert.assertSame(expression, roundTrip(expression));
        }
        Assert.assertEquals(knownBefore, evaluator.knownNodeCount());
    }

    @Test
    public void closedEvaluatorRefusesCalls()
    {
        PureRustEvaluator own = newEvaluator();
        Assert.assertTrue(own.isFullyInitializedForExecution());
        own.close();
        Assert.assertFalse(own.isFullyInitializedForExecution());
        PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class,
                () -> own.start(node(LET_FUNCTION), Lists.mutable.with(wrap(repository.newStringCoreInstance("x")), wrap(repository.newIntegerCoreInstance(1L)))));
        Assert.assertEquals("Evaluator has been closed", e.getMessage());
        own.close();
    }
}
