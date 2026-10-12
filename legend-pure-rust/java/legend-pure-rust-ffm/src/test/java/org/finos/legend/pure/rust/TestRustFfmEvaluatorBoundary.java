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
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.StringCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.StrictTimeFunctions;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.rust.interpreted.InterpretedHost;
import org.finos.legend.pure.rust.pct.RustPlatformGraph;
import org.finos.legend.pure.runtime.java.interpreted.natives.MapCoreInstance;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import java.math.BigDecimal;
import java.math.BigInteger;

public class TestRustFfmEvaluatorBoundary
{
    private static final String LET_FUNCTION = "meta::pure::functions::lang::letFunction_String_1__T_m__T_m_";
    private static final String ASSERT = "meta::pure::functions::asserts::assert_Boolean_1__Function_1__Boolean_1_";
    private static final String CLASS = "meta::pure::metamodel::type::Class";
    private static final String LAMBDA_FUNCTION = "meta::pure::metamodel::function::LambdaFunction";
    private static final String PAIR = "meta::pure::functions::collection::Pair";

    private static PureRustFfmEvaluator evaluator;
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

    private static PureRustFfmEvaluator newEvaluator()
    {
        PureRustFfmEvaluator created = new PureRustFfmEvaluator();
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
    private static CoreInstance roundTrip(PureRustFfmEvaluator through, CoreInstance value)
    {
        ListIterable<? extends CoreInstance> out = values(through.start(node(LET_FUNCTION), Lists.mutable.with(wrap(repository.newStringCoreInstance("x")), wrap(value))));
        Assert.assertEquals(1, out.size());
        return out.get(0);
    }

    private static CoreInstance roundTrip(CoreInstance value)
    {
        return roundTrip(evaluator, value);
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

    /**
     * The library is loaded with native access granted, so neither the load
     * nor a restricted method prints the JEP 472 warning.
     */
    @Test
    public void nativeAccessIsEnabledForTheModule()
    {
        Assert.assertTrue(PureRustFfmEvaluator.class.getModule().isNativeAccessEnabled());
    }

    @Test
    public void primitivesRoundTripWithTheirClassifier()
    {
        assertPrimitive(repository.newBooleanCoreInstance(true), "Boolean", "true");
        assertPrimitive(repository.newIntegerCoreInstance(42L), "Integer", "42");
        assertPrimitive(repository.newFloatCoreInstance(new BigDecimal("2.5")), "Float", "2.5");
        assertPrimitive(repository.newDecimalCoreInstance(new BigDecimal("1.50")), "Decimal", "1.50");
        assertPrimitive(repository.newStringCoreInstance("hello"), "String", "hello");
        assertPrimitive(repository.newStringCoreInstance(""), "String", "");
        assertPrimitive(repository.newStringCoreInstance("café ☃"), "String", "café ☃");
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

    /**
     * Integers and floats cross in typed, an Integer at either end of the
     * 64-bit range included; one beyond it crosses as its digits, which the
     * Rust side cannot hold and reports unsupported.
     */
    @Test
    public void numbersCrossInTypedAndAnIntegerBeyond64BitsIsUnsupported()
    {
        Assert.assertEquals(String.valueOf(Long.MAX_VALUE), roundTrip(repository.newIntegerCoreInstance(Long.MAX_VALUE)).getName());
        Assert.assertEquals(String.valueOf(Long.MIN_VALUE), roundTrip(repository.newIntegerCoreInstance(BigInteger.valueOf(Long.MIN_VALUE))).getName());
        Assert.assertEquals("-0.125", roundTrip(repository.newFloatCoreInstance(new BigDecimal("-0.125"))).getName());
        PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class,
                () -> roundTrip(repository.newIntegerCoreInstance(BigInteger.ONE.shiftLeft(64))));
        Assert.assertEquals(e.getMessage(), PureRustEvaluationException.Kind.UNSUPPORTED, e.getKind());
        Assert.assertEquals("Unsupported: an Integer outside the 64-bit range", e.getMessage());
    }

    /**
     * A Float crosses out as a double and reaches the host as the text Rust
     * prints for it: plain notation, no exponent, no trailing zero fraction.
     */
    @Test
    public void floatsCrossOutAsRustPrintsThem()
    {
        for (String[] pair : new String[][]{{"1.0", "1.0"}, {"100.0", "100.0"}, {"0.1", "0.1"}, {"2.5", "2.5"}, {"1E21", "1000000000000000000000.0"}, {"0.00000015", "0.00000015"}})
        {
            CoreInstance out = roundTrip(repository.newFloatCoreInstance(new BigDecimal(pair[0])));
            Assert.assertEquals(pair[0], pair[1], out.getName());
        }
    }

    @Test
    public void aStrictTimeRoundTrips()
    {
        PureStrictTime time = StrictTimeFunctions.parsePureStrictTime("10:30:15.5");
        CoreInstance out = roundTrip(repository.newStrictTimeCoreInstance(time));
        Assert.assertEquals("StrictTime", out.getClassifier().getName());
        Assert.assertEquals("10:30:15.5", out.getName());
        PureDate date = DateFunctions.newPureDate(2024, 2, 29, 10, 30);
        Assert.assertEquals("2024-02-29T10:30+0000", roundTrip(repository.newDateCoreInstance(date)).getName());
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
        Assert.assertEquals(PureRustFfmEvaluator.Crossing.ELEMENT, evaluator.crossing(classNode));
        Assert.assertSame(classNode, roundTrip(classNode));

        CoreInstance expression = anExpressionNode();
        Assert.assertEquals(PureRustFfmEvaluator.Crossing.HOST_NODE, evaluator.crossing(expression));
        Assert.assertSame(expression, roundTrip(expression));

        Assert.assertEquals(PureRustFfmEvaluator.Crossing.PRIMITIVE, evaluator.crossing(repository.newIntegerCoreInstance(7L)));
    }

    /**
     * A Rust object is read through its proxy over the ABI's object calls:
     * its classifier, its property values and names; once released its
     * handle is stale and the live count is back to zero.
     */
    @Test
    public void rustObjectsAreCoreInstancesWithAJavaClassifier()
    {
        CoreInstance created = evaluator.create(CLASS, new String[]{"name"}, new CoreInstance[][]{{repository.newStringCoreInstance("X")}});
        Assert.assertTrue(created instanceof RustCoreInstance);
        Assert.assertSame(node(CLASS), created.getClassifier());
        Assert.assertEquals(CLASS, ((RustCoreInstance) created).getClassifierPath());
        CoreInstance name = created.getValueForMetaPropertyToOne("name");
        Assert.assertTrue(name instanceof StringCoreInstance);
        Assert.assertEquals("X", PrimitiveUtilities.getStringValue(name));
        Assert.assertEquals("X", created.getName());
        Assert.assertTrue(created.getKeys().contains("name"));
        Assert.assertTrue(created.getValueForMetaPropertyToMany("other").isEmpty());
        Assert.assertEquals(1L, evaluator.liveHandleCount());
        Assert.assertEquals(PureRustFfmEvaluator.Crossing.RUST_OBJECT, evaluator.crossing(created));

        CoreInstance result = evaluator.start(node(LET_FUNCTION), Lists.mutable.with(wrap(repository.newStringCoreInstance("o")), wrap(created)));
        Assert.assertSame(created, values(result).get(0));
        CoreInstance rawType = result.getValueForMetaPropertyToOne(M3Properties.genericType).getValueForMetaPropertyToOne(M3Properties.rawType);
        Assert.assertSame(node(CLASS), rawType);

        ((RustCoreInstance) created).close();
        Assert.assertEquals(0L, evaluator.liveHandleCount());
        PureRustEvaluationException stale = Assert.assertThrows(PureRustEvaluationException.class, () -> created.getValueForMetaPropertyToOne("name"));
        Assert.assertEquals(PureRustEvaluationException.Kind.EXECUTION_ERROR, stale.getKind());
        Assert.assertTrue(stale.getMessage(), stale.getMessage().contains("Stale or unknown handle:"));
        ((RustCoreInstance) created).close();
        Assert.assertEquals(0L, evaluator.liveHandleCount());
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

    @Test
    public void aRustObjectThatIsNotAModelElementHasAnAnonymousName()
    {
        CoreInstance first = evaluator.create(PAIR, new String[]{"first", "second"}, new CoreInstance[][]{{repository.newIntegerCoreInstance(1L)}, {repository.newStringCoreInstance("a")}});
        CoreInstance second = evaluator.create(PAIR, new String[]{"first", "second"}, new CoreInstance[][]{{repository.newIntegerCoreInstance(1L)}, {repository.newStringCoreInstance("a")}});
        Assert.assertNotNull(first.getName());
        Assert.assertTrue(first.getName(), ModelRepository.isAnonymousInstanceName(first.getName()));
        Assert.assertNotEquals(first.getName(), second.getName());
        ((RustCoreInstance) first).close();
        ((RustCoreInstance) second).close();
    }

    /**
     * A map built in Rust crosses out as one MAP leaf over its entries and
     * comes back as the interpreted engine's own {@link MapCoreInstance}; a
     * Java map handed to a Rust map native does not cross as a map.
     */
    @Test
    public void aRustMapComesBackAsTheEnginesMapAndAJavaMapDoesNotCrossIn()
    {
        CoreInstance one = evaluator.create(PAIR, new String[]{"first", "second"}, new CoreInstance[][]{{repository.newStringCoreInstance("a")}, {repository.newIntegerCoreInstance(1L)}});
        CoreInstance two = evaluator.create(PAIR, new String[]{"first", "second"}, new CoreInstance[][]{{repository.newStringCoreInstance("b")}, {repository.newIntegerCoreInstance(2L)}});
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

    @Test
    public void aConcreteFunctionDefinitionLowersAndRunsWithItsArguments()
    {
        CoreInstance pctTest = pctTest(PlatformCodeRepositoryProvider.grammarFunctions, "testSimplePlus");
        CoreInstance adapter = node(PlatformCodeRepositoryProvider.nativeAdapter.function);
        CoreInstance result = evaluator.start(pctTest, Lists.mutable.with(wrap(adapter)));
        Assert.assertEquals("true", values(result).get(0).getName());
    }

    @Test
    public void anElementThatIsNotAFunctionIsFunctionNotFoundWithItsUserPath()
    {
        String profile = "meta::pure::profiles::doc";
        PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class,
                () -> evaluator.start(node(profile), Lists.immutable.empty()));
        Assert.assertEquals(PureRustEvaluationException.Kind.FUNCTION_NOT_FOUND, e.getKind());
        Assert.assertEquals("FunctionNotFound: " + profile, e.getMessage());
        Assert.assertEquals(profile, e.getInfo());
    }

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
     * with the same result; the timings say so.
     */
    @Test
    public void aLambdaIsLoweredOnceAndRunsAgainFromTheClosureCache()
    {
        CoreInstance lambda = findLambda(pctTest(PlatformCodeRepositoryProvider.grammarFunctions, "testSimpleMinus"));
        CoreInstance first = evaluator.start(lambda, Lists.immutable.empty());
        PureRustFfmEvaluator.Timings cold = PureRustFfmEvaluator.lastTimings();
        Assert.assertTrue(cold.toString(), cold.lowerHostReads() > 0L);
        Assert.assertTrue(cold.toString(), cold.lowerNanos() > 0L);
        CoreInstance second = evaluator.start(lambda, Lists.immutable.empty());
        PureRustFfmEvaluator.Timings warm = PureRustFfmEvaluator.lastTimings();
        Assert.assertEquals(0L, warm.lowerHostReads());
        Assert.assertEquals(0L, warm.runHostReads());
        Assert.assertEquals(values(first).get(0).getName(), values(second).get(0).getName());
    }

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

    @Test
    public void aClassConstructionLowersTheClassAndBuildsAnInstance()
    {
        CoreInstance lambda = findLambda(pctTest(PlatformCodeRepositoryProvider.grammarFunctions, "testAssignNewInstance"));
        CoreInstance result = evaluator.start(lambda, Lists.immutable.empty());
        CoreInstance person = values(result).get(0);
        Assert.assertEquals("LA_Person", person.getClassifier().getName());
        Assert.assertEquals("John", person.getValueForMetaPropertyToOne("firstName").getName());
        Assert.assertEquals("Doe", person.getValueForMetaPropertyToOne("lastName").getName());
        Assert.assertEquals(1L, evaluator.liveHandleCount());
        ((RustCoreInstance) person).close();
        Assert.assertEquals(0L, evaluator.liveHandleCount());
    }

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

    @Test
    public void anEnumValueLowersItsEnumeration()
    {
        CoreInstance lambda = findLambda(pctTest(PlatformCodeRepositoryProvider.essentialFunctions, "testAdjustByYears"));
        CoreInstance result = evaluator.start(lambda, Lists.immutable.empty());
        Assert.assertEquals("2016", values(result).get(0).getName());
    }

    /**
     * A failure raised inside a lowered lambda carries the Java source span
     * of the call that raised it, start and end, through the
     * {@code pure_span} of {@code pure_failure.source}, and the Pure cause
     * reports that span with the bare Pure text.
     */
    @Test
    public void failuresInsideALoweredLambdaCarryTheJavaSpan()
    {
        CoreInstance lambda = findLambda(pctTest(PlatformCodeRepositoryProvider.essentialFunctions, "testAtError"));
        SourceInformation call = lambda.getValueForMetaPropertyToMany(M3Properties.expressionSequence).getFirst().getSourceInformation();
        Assert.assertTrue(call.getMessage(), call.getEndColumn() > call.getColumn());
        PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class,
                () -> evaluator.start(lambda, Lists.immutable.empty()));
        Assert.assertEquals(PureRustEvaluationException.Kind.EXECUTION_ERROR, e.getKind());
        Assert.assertEquals("ExecutionError: The system is trying to get an element at offset 2 where the collection is of size 2", e.getMessage());
        Assert.assertEquals("The system is trying to get an element at offset 2 where the collection is of size 2", e.getInfo());
        assertSpan(call, e.getSourceInformation());
        PureException cause = PureException.findPureException(e);
        Assert.assertNotNull(cause);
        Assert.assertEquals("The system is trying to get an element at offset 2 where the collection is of size 2", cause.getInfo());
        Assert.assertEquals("/platform/pure/essential/collection/index/at.pure", cause.getSourceInformation().getSourceId());
        assertSpan(call, cause.getSourceInformation());
    }

    /**
     * The anchor and the end of a span, as the Java node had them.
     */
    private static void assertSpan(SourceInformation expected, SourceInformation actual)
    {
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.getSourceId(), actual.getSourceId());
        Assert.assertEquals(expected.getLine(), actual.getLine());
        Assert.assertEquals(expected.getColumn(), actual.getColumn());
        Assert.assertEquals(expected.getEndLine(), actual.getEndLine());
        Assert.assertEquals(expected.getEndColumn(), actual.getEndColumn());
    }

    @Test
    public void pureBodiedFunctionsLowerTransitively()
    {
        CoreInstance lambda = findLambda(pctTest(PlatformCodeRepositoryProvider.grammarFunctions, "testLetAsLastStatement"));
        CoreInstance result = evaluator.start(lambda, Lists.immutable.empty());
        Assert.assertEquals("last statement string", values(result).get(0).getName());
    }

    @Test
    public void aBareLambdaWithOpenVariablesIsUnsupported()
    {
        CoreInstance lambda = aLambdaWithOpenVariables();
        PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class,
                () -> evaluator.start(lambda, Lists.immutable.empty()));
        Assert.assertEquals(PureRustEvaluationException.Kind.UNSUPPORTED, e.getKind());
        Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("Unsupported: lowering captured variables"));
    }

    /**
     * A Java failure inside a callback never crosses the native frame: it is
     * recorded on the reply with the kind {@code failureOf} gives it, the
     * Rust side names the read it was answering, and the evaluator stays
     * usable afterwards.
     */
    @Test
    public void aJavaFailureInsideAnUpcallArrivesTypedAndLeavesTheEvaluatorUsable()
    {
        InterpretedHost inner = new InterpretedHost(repository, ps);
        RustEngineHost failing = new ForwardingHost(inner)
        {
            @Override
            public ListIterable<? extends CoreInstance> capturedValues(CoreInstance lambda, String name)
            {
                throw new IllegalStateException("boom reading " + name);
            }
        };
        try (PureRustFfmEvaluator own = PureRustFfmEvaluator.over(failing))
        {
            CoreInstance lambda = aLambdaWithOpenVariables();
            PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class,
                    () -> own.start(lambda, Lists.immutable.empty()));
            Assert.assertEquals(e.getMessage(), PureRustEvaluationException.Kind.EXECUTION_ERROR, e.getKind());
            Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("ExecutionError: reading `"));
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("(captured values): java.lang.IllegalStateException: boom reading "));
            Assert.assertEquals("42", roundTrip(own, repository.newIntegerCoreInstance(42L)).getName());

            RustEngineHost unsupported = new ForwardingHost(inner)
            {
                @Override
                public ListIterable<? extends CoreInstance> capturedValues(CoreInstance lambda, String name)
                {
                    throw PureRustEvaluationException.unsupported("captures of " + name);
                }
            };
            try (PureRustFfmEvaluator typed = PureRustFfmEvaluator.over(unsupported))
            {
                PureRustEvaluationException kept = Assert.assertThrows(PureRustEvaluationException.class,
                        () -> typed.start(lambda, Lists.immutable.empty()));
                Assert.assertEquals(kept.getMessage(), PureRustEvaluationException.Kind.UNSUPPORTED, kept.getKind());
                Assert.assertTrue(kept.getMessage(), kept.getMessage().startsWith("Unsupported: captures of "));
                Assert.assertEquals(kept.getMessage().substring("Unsupported: ".length()), kept.getInfo());
                Assert.assertEquals(0L, typed.liveHandleCount());
            }
        }
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
        Assert.assertEquals("boom", e.getInfo());

        CoreInstance ok = evaluator.start(node(ASSERT), Lists.mutable.with(wrap(repository.newBooleanCoreInstance(true)), wrap(repository.newStringCoreInstance("unused"))));
        Assert.assertEquals("true", values(ok).get(0).getName());
    }

    /**
     * Handles return to zero after a run: objects created and released,
     * nodes crossing in and out, ten thousand times, leave nothing held on
     * either side.
     */
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
        Assert.assertEquals(0L, evaluator.liveHandleCount());
    }

    /**
     * {@code close()} frees the context and the arena once; a second close
     * does nothing, and every call after it is refused before any downcall.
     */
    @Test
    public void closedEvaluatorRefusesCallsAndClosesOnce()
    {
        PureRustFfmEvaluator own = newEvaluator();
        Assert.assertTrue(own.isFullyInitializedForExecution());
        CoreInstance created = own.create(CLASS, new String[]{"name"}, new CoreInstance[][]{{repository.newStringCoreInstance("X")}});
        Assert.assertEquals(1L, own.liveHandleCount());
        own.close();
        Assert.assertFalse(own.isFullyInitializedForExecution());
        Assert.assertEquals(0L, own.liveHandleCount());
        PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class,
                () -> own.start(node(LET_FUNCTION), Lists.mutable.with(wrap(repository.newStringCoreInstance("x")), wrap(repository.newIntegerCoreInstance(1L)))));
        Assert.assertEquals("Evaluator has been closed", e.getMessage());
        PureRustEvaluationException read = Assert.assertThrows(PureRustEvaluationException.class, () -> created.getValueForMetaPropertyToOne("name"));
        Assert.assertEquals("Evaluator has been closed", read.getMessage());
        ((RustCoreInstance) created).close();
        own.close();
    }

    private static CoreInstance aLambdaWithOpenVariables()
    {
        CoreInstance test = pctTest(PlatformCodeRepositoryProvider.essentialFunctions, "testSimpleSortWithFunctionVariables");
        CoreInstance lambda = findLambdaWithOpenVariables(test);
        Assert.assertNotNull(lambda);
        return lambda;
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

    /**
     * A host that forwards everything to another, for a test to override one read.
     */
    private static class ForwardingHost implements RustEngineHost
    {
        private final RustEngineHost inner;

        ForwardingHost(RustEngineHost inner)
        {
            this.inner = inner;
        }

        @Override
        public ProcessorSupport processorSupport()
        {
            return this.inner.processorSupport();
        }

        @Override
        public ListIterable<String> propertyKeys(CoreInstance node)
        {
            return this.inner.propertyKeys(node);
        }

        @Override
        public ListIterable<? extends CoreInstance> capturedValues(CoreInstance lambda, String name)
        {
            return this.inner.capturedValues(lambda, name);
        }

        @Override
        public Object lambdaDefinition(CoreInstance lambda)
        {
            return this.inner.lambdaDefinition(lambda);
        }

        @Override
        public CoreInstance newBoolean(boolean value)
        {
            return this.inner.newBoolean(value);
        }

        @Override
        public CoreInstance newInteger(long value)
        {
            return this.inner.newInteger(value);
        }

        @Override
        public CoreInstance newFloat(String text)
        {
            return this.inner.newFloat(text);
        }

        @Override
        public CoreInstance newDecimal(String text)
        {
            return this.inner.newDecimal(text);
        }

        @Override
        public CoreInstance newString(String value)
        {
            return this.inner.newString(value);
        }

        @Override
        public CoreInstance newDate(PureDate date)
        {
            return this.inner.newDate(date);
        }

        @Override
        public CoreInstance newLatestDate()
        {
            return this.inner.newLatestDate();
        }

        @Override
        public CoreInstance newStrictTime(PureStrictTime time)
        {
            return this.inner.newStrictTime(time);
        }

        @Override
        public CoreInstance newMap(CoreInstance[] keys, CoreInstance[] values)
        {
            return this.inner.newMap(keys, values);
        }
    }
}
