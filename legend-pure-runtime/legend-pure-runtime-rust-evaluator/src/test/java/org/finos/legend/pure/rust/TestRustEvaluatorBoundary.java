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
     * The body of every PCT test hands a lambda to its adapter; until Step 3
     * lowers Java lambdas through the bridge, that lambda is Unsupported, and
     * this is the message the Rust_JNI manifests record.
     */
    @Test
    public void aJavaLambdaIsUnsupportedUntilTheBridgeLowersIt()
    {
        CoreInstance lambda = findLambda(firstPctTest());
        Assert.assertNotNull(lambda);
        PureRustEvaluationException e = Assert.assertThrows(PureRustEvaluationException.class,
                () -> evaluator.start(lambda, Lists.immutable.empty()));
        Assert.assertEquals(PureRustEvaluationException.Kind.UNSUPPORTED, e.getKind());
        Assert.assertEquals("Unsupported: evaluating a Java LambdaFunction through the bridge", e.getMessage());
    }

    private static CoreInstance findLambda(CoreInstance node)
    {
        if (LAMBDA_FUNCTION.equals(PackageableElement.getUserPathForPackageableElement(node.getClassifier())))
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
