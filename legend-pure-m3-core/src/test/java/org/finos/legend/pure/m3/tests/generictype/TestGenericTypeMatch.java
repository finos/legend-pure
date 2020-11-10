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

package org.finos.legend.pure.m3.tests.generictype;

import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.generictype.match.GenericTypeMatch;
import org.finos.legend.pure.m3.navigation.generictype.match.ParameterMatchBehavior;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.*;

public class TestGenericTypeMatch extends AbstractPureTestWithCoreCompiledPlatform
{
    private static CoreInstance any;
    private static CoreInstance nil;
    private static CoreInstance one;
    private static CoreInstance zeroMany;

    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
        any = newGenericType(M3Paths.Any);
        nil = newGenericType(M3Paths.Nil);
        one = runtime.getCoreInstance(M3Paths.PureOne);
        zeroMany = runtime.getCoreInstance(M3Paths.ZeroMany);
    }

    @After
    public void clearRuntime() {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testMatches_ConcreteCovariantNoGenerics()
    {
        compileTestSource("fromString.pure","Class test::A {}\n" +
                "Class test::B extends test::A {}\n" +
                "Class test::C {}\n");
        CoreInstance a = newGenericType("test::A");
        CoreInstance b = newGenericType("test::B");
        CoreInstance c = newGenericType("test::C");

        assertMatches(any, any, true, false);
        assertMatches(any, a, true, false);
        assertMatches(any, b, true, false);
        assertMatches(any, c, true, false);
        assertMatches(any, nil, true, false);

        assertNotMatches(a, any, true, false);
        assertMatches(a, a, true, false);
        assertMatches(a, b, true, false);
        assertNotMatches(a, c, true, false);
        assertMatches(a, nil, true, false);

        assertNotMatches(b, any, true, false);
        assertNotMatches(b, a, true, false);
        assertMatches(b, b, true, false);
        assertNotMatches(b, c, true, false);
        assertMatches(b, nil, true, false);

        assertNotMatches(c, any, true, false);
        assertNotMatches(c, a, true, false);
        assertNotMatches(c, b, true, false);
        assertMatches(c, c, true, false);
        assertMatches(c, nil, true, false);

        assertNotMatches(nil, any, true, false);
        assertNotMatches(nil, a, true, false);
        assertNotMatches(nil, b, true, false);
        assertNotMatches(nil, c, true, false);
        assertMatches(nil, nil, true, false);
    }

    @Test
    public void testMatches_ConcreteContravariantNoGenerics()
    {
        compileTestSource("fromString.pure","Class test::A {}\n" +
                "Class test::B extends test::A {}\n" +
                "Class test::C {}\n");
        CoreInstance a = newGenericType("test::A");
        CoreInstance b = newGenericType("test::B");
        CoreInstance c = newGenericType("test::C");

        assertMatches(any, any, false, false);
        assertNotMatches(any, a, false, false);
        assertNotMatches(any, b, false, false);
        assertNotMatches(any, c, false, false);
        assertNotMatches(any, nil, false, false);

        assertMatches(a, any, false, false);
        assertMatches(a, a, false, false);
        assertNotMatches(a, b, false, false);
        assertNotMatches(a, c, false, false);
        assertNotMatches(a, nil, false, false);

        assertMatches(b, any, false, false);
        assertMatches(b, a, false, false);
        assertMatches(b, b, false, false);
        assertNotMatches(b, c, false, false);
        assertNotMatches(b, nil, false, false);

        assertMatches(c, any, false, false);
        assertNotMatches(c, a, false, false);
        assertNotMatches(c, b, false, false);
        assertMatches(c, c, false, false);
        assertNotMatches(c, nil, false, false);

        assertMatches(nil, any, false, false);
        assertMatches(nil, a, false, false);
        assertMatches(nil, b, false, false);
        assertMatches(nil, c, false, false);
        assertMatches(nil, nil, false, false);
    }

    @Test
    public void testMatches_ConcreteCovariantWithGenerics()
    {
        compileTestSource("fromString.pure","Class test::A<X> {}\n" +
                "Class test::B<Y> extends test::A<Y> {}\n" +
                "Class test::C<Z> {}\n");

        CoreInstance aT = newGenericType("test::A", Lists.immutable.with(newNonConcreteGenericType("T")));
        CoreInstance aAny = newGenericType("test::A", Lists.immutable.with(any));
        CoreInstance aInteger = newGenericType("test::A", Lists.immutable.with(newGenericType(M3Paths.Integer)));
        CoreInstance aNil = newGenericType("test::A", Lists.immutable.with(nil));

        CoreInstance bT = newGenericType("test::B", Lists.immutable.with(newNonConcreteGenericType("T")));
        CoreInstance bAny = newGenericType("test::B", Lists.immutable.with(any));
        CoreInstance bInteger = newGenericType("test::B", Lists.immutable.with(newGenericType(M3Paths.Integer)));
        CoreInstance bNil = newGenericType("test::B", Lists.immutable.with(nil));

        CoreInstance cT = newGenericType("test::C", Lists.immutable.with(newNonConcreteGenericType("T")));
        CoreInstance cAny = newGenericType("test::C", Lists.immutable.with(any));
        CoreInstance cInteger = newGenericType("test::C", Lists.immutable.with(newGenericType(M3Paths.Integer)));
        CoreInstance cNil = newGenericType("test::C", Lists.immutable.with(nil));

        assertMatches(any, aT, true, false);
        assertMatches(any, aAny, true, false);
        assertMatches(any, aInteger, true, false);
        assertMatches(any, aNil, true, false);
        assertMatches(any, bT, true, false);
        assertMatches(any, bAny, true, false);
        assertMatches(any, bInteger, true, false);
        assertMatches(any, bNil, true, false);
        assertMatches(any, cT, true, false);
        assertMatches(any, cAny, true, false);
        assertMatches(any, cInteger, true, false);
        assertMatches(any, cNil, true, false);

        assertNotMatches(aT, any, true, false);
        assertMatches(aT, aT, true, false);
        assertNotMatches(aT, aAny, true, false);
        assertNotMatches(aT, aInteger, true, false);
        assertMatches(aT, aNil, true, false);
        assertMatches(aT, bT, true, false);
        assertNotMatches(aT, bAny, true, false);
        assertNotMatches(aT, bInteger, true, false);
        assertMatches(aT, bNil, true, false);
        assertNotMatches(aT, cT, true, false);
        assertNotMatches(aT, cAny, true, false);
        assertNotMatches(aT, cInteger, true, false);
        assertNotMatches(aT, cNil, true, false);
        assertMatches(aT, nil, true, false);

        assertNotMatches(aAny, any, true, false);
        assertMatches(aAny, aT, true, false);
        assertMatches(aAny, aAny, true, false);
        assertMatches(aAny, aInteger, true, false);
        assertMatches(aAny, aNil, true, false);
        assertMatches(aAny, bT, true, false);
        assertMatches(aAny, bAny, true, false);
        assertMatches(aAny, bInteger, true, false);
        assertMatches(aAny, bNil, true, false);
        assertNotMatches(aAny, cT, true, false);
        assertNotMatches(aAny, cAny, true, false);
        assertNotMatches(aAny, cInteger, true, false);
        assertNotMatches(aAny, cNil, true, false);
        assertMatches(aAny, nil, true, false);

        assertNotMatches(aInteger, any, true, false);
        assertNotMatches(aInteger, aT, true, false);
        assertNotMatches(aInteger, aAny, true, false);
        assertMatches(aInteger, aInteger, true, false);
        assertMatches(aInteger, aNil, true, false);
        assertNotMatches(aInteger, bT, true, false);
        assertNotMatches(aInteger, bAny, true, false);
        assertMatches(aInteger, bInteger, true, false);
        assertMatches(aInteger, bNil, true, false);
        assertNotMatches(aInteger, cT, true, false);
        assertNotMatches(aInteger, cAny, true, false);
        assertNotMatches(aInteger, cInteger, true, false);
        assertNotMatches(aInteger, cNil, true, false);
        assertMatches(aInteger, nil, true, false);

        assertNotMatches(aNil, any, true, false);
        assertNotMatches(aNil, aT, true, false);
        assertNotMatches(aNil, aAny, true, false);
        assertNotMatches(aNil, aInteger, true, false);
        assertMatches(aNil, aNil, true, false);
        assertNotMatches(aNil, bT, true, false);
        assertNotMatches(aNil, bAny, true, false);
        assertNotMatches(aNil, bInteger, true, false);
        assertMatches(aNil, bNil, true, false);
        assertNotMatches(aNil, cT, true, false);
        assertNotMatches(aNil, cAny, true, false);
        assertNotMatches(aNil, cInteger, true, false);
        assertNotMatches(aNil, cNil, true, false);
        assertMatches(aNil, nil, true, false);

        assertNotMatches(nil, aT, true, false);
        assertNotMatches(nil, aAny, true, false);
        assertNotMatches(nil, aInteger, true, false);
        assertNotMatches(nil, aNil, true, false);
        assertNotMatches(nil, bT, true, false);
        assertNotMatches(nil, bAny, true, false);
        assertNotMatches(nil, bInteger, true, false);
        assertNotMatches(nil, bNil, true, false);
        assertNotMatches(nil, cT, true, false);
        assertNotMatches(nil, cAny, true, false);
        assertNotMatches(nil, cInteger, true, false);
        assertNotMatches(nil, cNil, true, false);
    }

    @Test
    public void testMatches_ConcreteContravariantWithGenerics()
    {
        compileTestSource("fromString.pure","Class test::A<X> {}\n" +
                "Class test::B<Y> extends test::A<Y> {}\n" +
                "Class test::C<Z> {}\n");

        CoreInstance aT = newGenericType("test::A", Lists.immutable.with(newNonConcreteGenericType("T")));
        CoreInstance aAny = newGenericType("test::A", Lists.immutable.with(any));
        CoreInstance aInteger = newGenericType("test::A", Lists.immutable.with(newGenericType(M3Paths.Integer)));
        CoreInstance aNil = newGenericType("test::A", Lists.immutable.with(nil));

        CoreInstance bT = newGenericType("test::B", Lists.immutable.with(newNonConcreteGenericType("T")));
        CoreInstance bAny = newGenericType("test::B", Lists.immutable.with(any));
        CoreInstance bInteger = newGenericType("test::B", Lists.immutable.with(newGenericType(M3Paths.Integer)));
        CoreInstance bNil = newGenericType("test::B", Lists.immutable.with(nil));

        CoreInstance cT = newGenericType("test::C", Lists.immutable.with(newNonConcreteGenericType("T")));
        CoreInstance cAny = newGenericType("test::C", Lists.immutable.with(any));
        CoreInstance cInteger = newGenericType("test::C", Lists.immutable.with(newGenericType(M3Paths.Integer)));
        CoreInstance cNil = newGenericType("test::C", Lists.immutable.with(nil));

        assertNotMatches(any, aT, false, false);
        assertNotMatches(any, aAny, false, false);
        assertNotMatches(any, aInteger, false, false);
        assertNotMatches(any, aNil, false, false);
        assertNotMatches(any, bT, false, false);
        assertNotMatches(any, bAny, false, false);
        assertNotMatches(any, bInteger, false, false);
        assertNotMatches(any, bNil, false, false);
        assertNotMatches(any, cT, false, false);
        assertNotMatches(any, cAny, false, false);
        assertNotMatches(any, cInteger, false, false);
        assertNotMatches(any, cNil, false, false);

        assertMatches(aT, any, false, false);
        assertMatches(aT, aT, false, false);
        assertMatches(aT, aAny, false, false);
        assertNotMatches(aT, aInteger, false, false);
        assertNotMatches(aT, aNil, false, false);
        assertNotMatches(aT, bT, false, false);
        assertNotMatches(aT, bAny, false, false);
        assertNotMatches(aT, bInteger, false, false);
        assertNotMatches(aT, bNil, false, false);
        assertNotMatches(aT, cT, false, false);
        assertNotMatches(aT, cAny, false, false);
        assertNotMatches(aT, cInteger, false, false);
        assertNotMatches(aT, cNil, false, false);
        assertNotMatches(aT, nil, false, false);

        assertMatches(aAny, any, false, false);
        assertNotMatches(aAny, aT, false, false);
        assertMatches(aAny, aAny, false, false);
        assertNotMatches(aAny, aInteger, false, false);
        assertNotMatches(aAny, aNil, false, false);
        assertNotMatches(aAny, bT, false, false);
        assertNotMatches(aAny, bAny, false, false);
        assertNotMatches(aAny, bInteger, false, false);
        assertNotMatches(aAny, bNil, false, false);
        assertNotMatches(aAny, cT, false, false);
        assertNotMatches(aAny, cAny, false, false);
        assertNotMatches(aAny, cInteger, false, false);
        assertNotMatches(aAny, cNil, false, false);
        assertNotMatches(aAny, nil, false, false);

        assertMatches(aInteger, any, false, false);
        assertNotMatches(aInteger, aT, false, false);
        assertMatches(aInteger, aAny, false, false);
        assertMatches(aInteger, aInteger, false, false);
        assertNotMatches(aInteger, aNil, false, false);
        assertNotMatches(aInteger, bT, false, false);
        assertNotMatches(aInteger, bAny, false, false);
        assertNotMatches(aInteger, bInteger, false, false);
        assertNotMatches(aInteger, bNil, false, false);
        assertNotMatches(aInteger, cT, false, false);
        assertNotMatches(aInteger, cAny, false, false);
        assertNotMatches(aInteger, cInteger, false, false);
        assertNotMatches(aInteger, cNil, false, false);
        assertNotMatches(aInteger, nil, false, false);

        assertMatches(aNil, any, false, false);
        assertMatches(aNil, aT, false, false);
        assertMatches(aNil, aAny, false, false);
        assertMatches(aNil, aInteger, false, false);
        assertMatches(aNil, aNil, false, false);
        assertNotMatches(aNil, bT, false, false);
        assertNotMatches(aNil, bAny, false, false);
        assertNotMatches(aNil, bInteger, false, false);
        assertNotMatches(aNil, bNil, false, false);
        assertNotMatches(aNil, cT, false, false);
        assertNotMatches(aNil, cAny, false, false);
        assertNotMatches(aNil, cInteger, false, false);
        assertNotMatches(aNil, cNil, false, false);
        assertNotMatches(aNil, nil, false, false);

        assertMatches(nil, aT, false, false);
        assertMatches(nil, aAny, false, false);
        assertMatches(nil, aInteger, false, false);
        assertMatches(nil, aNil, false, false);
        assertMatches(nil, bT, false, false);
        assertMatches(nil, bAny, false, false);
        assertMatches(nil, bInteger, false, false);
        assertMatches(nil, bNil, false, false);
        assertMatches(nil, cT, false, false);
        assertMatches(nil, cAny, false, false);
        assertMatches(nil, cInteger, false, false);
        assertMatches(nil, cNil, false, false);
    }

    @Test
    public void testMatches_NonConcreteTarget()
    {
        compileTestSource("fromString.pure","Class test::A {}\n" +
                "Class test::B extends test::A {}\n" +
                "Class test::C<T> {}\n");
        CoreInstance a = newGenericType("test::A");
        CoreInstance b = newGenericType("test::B");
        CoreInstance cString = newGenericType("test::C", Lists.immutable.with(newGenericType(M3Paths.String)));

        CoreInstance t = newNonConcreteGenericType("T");
        CoreInstance u = newNonConcreteGenericType("U");

        // Covariant
        assertNotMatches(t, any, true, false);
        assertMatches(t, any, true, true);
        assertNotMatches(t, a, true, false);
        assertMatches(t, a, true, true);
        assertNotMatches(t, b, true, false);
        assertMatches(t, b, true, true);
        assertNotMatches(t, cString, true, false);
        assertMatches(t, cString, true, true);
        assertMatches(t, nil, true, false);
        assertMatches(t, nil, true, true);

        assertNotMatches(u, any, true, false);
        assertMatches(u, any, true, true);
        assertNotMatches(u, a, true, false);
        assertMatches(u, a, true, true);
        assertNotMatches(u, b, true, false);
        assertMatches(u, b, true, true);
        assertNotMatches(u, cString, true, false);
        assertMatches(u, cString, true, true);
        assertMatches(u, nil, true, false);
        assertMatches(u, nil, true, true);

        assertMatches(t, t, true, false);
        assertMatches(t, t, true, true);
        assertMatches(u, u, true, false);
        assertMatches(u, u, true, true);

        // Contravariant
        assertMatches(t, any, false, false);
        assertMatches(t, any, false, true);
        assertNotMatches(t, a, false, false);
        assertMatches(t, a, false, true);
        assertNotMatches(t, b, false, false);
        assertMatches(t, b, false, true);
        assertNotMatches(t, cString, false, false);
        assertMatches(t, cString, false, true);
        assertNotMatches(t, nil, false, false);
        assertMatches(t, nil, false, true);

        assertMatches(u, any, false, false);
        assertMatches(u, any, false, true);
        assertNotMatches(u, a, false, false);
        assertMatches(u, a, false, true);
        assertNotMatches(u, b, false, false);
        assertMatches(u, b, false, true);
        assertNotMatches(u, cString, false, false);
        assertMatches(u, cString, false, true);
        assertNotMatches(u, nil, false, false);
        assertMatches(u, nil, false, true);

        assertMatches(t, t, false, false);
        assertMatches(t, t, false, true);
        assertMatches(u, u, false, false);
        assertMatches(u, u, false, true);
    }

    @Test
    public void testMatches_ConcreteCovariantWithComplexGenerics()
    {
        compileTestSource("fromString.pure","Class test::A\n" +
                "{\n" +
                "  aPropToOne : String[1];\n" +
                "  aPropToMany : String[*];\n" +
                "}\n" +
                "Class test::B extends test::A\n" +
                "{\n" +
                "  bPropToOne : String[1];\n" +
                "  bPropToMany : String[*];\n" +
                "}\n" +
                "Class test::C\n" +
                "{\n" +
                "  cPropToOne : String[1];\n" +
                "  cPropToMany : String[*];\n" +
                "}\n");

        CoreInstance propertyNilAnyOne = newGenericType(M3Paths.Property, Lists.immutable.with(nil, any), Lists.immutable.with(one));
        CoreInstance propertyNilAnyMany = newGenericType(M3Paths.Property, Lists.immutable.with(nil, any), Lists.immutable.with(zeroMany));
        CoreInstance propertyAStringOne = newGenericType(M3Paths.Property, Lists.immutable.with(newGenericType("test::A"), newGenericType(M3Paths.String)), Lists.immutable.with(one));
        CoreInstance propertyAStringMany = newGenericType(M3Paths.Property, Lists.immutable.with(newGenericType("test::A"), newGenericType(M3Paths.String)), Lists.immutable.with(zeroMany));
        CoreInstance propertyBStringOne = newGenericType(M3Paths.Property, Lists.immutable.with(newGenericType("test::B"), newGenericType(M3Paths.String)), Lists.immutable.with(one));
        CoreInstance propertyBStringMany = newGenericType(M3Paths.Property, Lists.immutable.with(newGenericType("test::B"), newGenericType(M3Paths.String)), Lists.immutable.with(zeroMany));
        CoreInstance propertyCStringOne = newGenericType(M3Paths.Property, Lists.immutable.with(newGenericType("test::C"), newGenericType(M3Paths.String)), Lists.immutable.with(one));
        CoreInstance propertyCStringMany = newGenericType(M3Paths.Property, Lists.immutable.with(newGenericType("test::C"), newGenericType(M3Paths.String)), Lists.immutable.with(zeroMany));

        CoreInstance functionNilOneAnyOne = newGenericType(M3Paths.Function, Lists.immutable.with(newFunctionTypeGenericType(nil, one, any, one)));
        CoreInstance functionNilOneAnyMany = newGenericType(M3Paths.Function, Lists.immutable.with(newFunctionTypeGenericType(nil, one, any, zeroMany)));
        CoreInstance functionNilManyAnyOne = newGenericType(M3Paths.Function, Lists.immutable.with(newFunctionTypeGenericType(nil, zeroMany, any, one)));
        CoreInstance functionNilManyAnyMany = newGenericType(M3Paths.Function, Lists.immutable.with(newFunctionTypeGenericType(nil, zeroMany, any, zeroMany)));
        CoreInstance functionAOneStringOne = newGenericType(M3Paths.Function, Lists.immutable.with(newFunctionTypeGenericType(newGenericType("test::A"), one, newGenericType(M3Paths.String), one)));
        CoreInstance functionAOneStringMany = newGenericType(M3Paths.Function, Lists.immutable.with(newFunctionTypeGenericType(newGenericType("test::A"), one, newGenericType(M3Paths.String), zeroMany)));
        CoreInstance functionBOneStringOne = newGenericType(M3Paths.Function, Lists.immutable.with(newFunctionTypeGenericType(newGenericType("test::B"), one, newGenericType(M3Paths.String), one)));
        CoreInstance functionBOneStringMany = newGenericType(M3Paths.Function, Lists.immutable.with(newFunctionTypeGenericType(newGenericType("test::B"), one, newGenericType(M3Paths.String), zeroMany)));
        CoreInstance functionCOneStringOne = newGenericType(M3Paths.Function, Lists.immutable.with(newFunctionTypeGenericType(newGenericType("test::C"), one, newGenericType(M3Paths.String), one)));
        CoreInstance functionCOneStringMany = newGenericType(M3Paths.Function, Lists.immutable.with(newFunctionTypeGenericType(newGenericType("test::C"), one, newGenericType(M3Paths.String), zeroMany)));

        assertMatches(propertyNilAnyOne, propertyNilAnyOne, true, false);
        assertNotMatches(propertyNilAnyOne, propertyNilAnyMany, true, false);
        assertMatches(propertyNilAnyOne, propertyAStringOne, true, false);
        assertNotMatches(propertyNilAnyOne, propertyAStringMany, true, false);
        assertMatches(propertyNilAnyOne, propertyBStringOne, true, false);
        assertNotMatches(propertyNilAnyOne, propertyBStringMany, true, false);
        assertMatches(propertyNilAnyOne, propertyCStringOne, true, false);
        assertNotMatches(propertyNilAnyOne, propertyCStringMany, true, false);

        assertMatches(propertyNilAnyMany, propertyNilAnyOne, true, false);
        assertMatches(propertyNilAnyMany, propertyNilAnyMany, true, false);
        assertMatches(propertyNilAnyMany, propertyAStringOne, true, false);
        assertMatches(propertyNilAnyMany, propertyAStringMany, true, false);
        assertMatches(propertyNilAnyMany, propertyBStringOne, true, false);
        assertMatches(propertyNilAnyMany, propertyBStringMany, true, false);
        assertMatches(propertyNilAnyMany, propertyCStringOne, true, false);
        assertMatches(propertyNilAnyMany, propertyCStringMany, true, false);

        assertNotMatches(propertyAStringOne, propertyNilAnyOne, true, false);
        assertNotMatches(propertyAStringOne, propertyNilAnyMany, true, false);
        assertMatches(propertyAStringOne, propertyAStringOne, true, false);
        assertNotMatches(propertyAStringOne, propertyAStringMany, true, false);
        assertNotMatches(propertyAStringOne, propertyBStringOne, true, false);
        assertNotMatches(propertyAStringOne, propertyBStringMany, true, false);
        assertNotMatches(propertyAStringOne, propertyCStringOne, true, false);
        assertNotMatches(propertyAStringOne, propertyCStringMany, true, false);

        assertNotMatches(propertyAStringMany, propertyNilAnyOne, true, false);
        assertNotMatches(propertyAStringMany, propertyNilAnyMany, true, false);
        assertMatches(propertyAStringMany, propertyAStringOne, true, false);
        assertMatches(propertyAStringMany, propertyAStringMany, true, false);
        assertNotMatches(propertyAStringMany, propertyBStringOne, true, false);
        assertNotMatches(propertyAStringMany, propertyBStringMany, true, false);
        assertNotMatches(propertyAStringMany, propertyCStringOne, true, false);
        assertNotMatches(propertyAStringMany, propertyCStringMany, true, false);

        assertNotMatches(propertyBStringOne, propertyNilAnyOne, true, false);
        assertNotMatches(propertyBStringOne, propertyNilAnyMany, true, false);
        assertMatches(propertyBStringOne, propertyAStringOne, true, false);
        assertNotMatches(propertyBStringOne, propertyAStringMany, true, false);
        assertMatches(propertyBStringOne, propertyBStringOne, true, false);
        assertNotMatches(propertyBStringOne, propertyBStringMany, true, false);
        assertNotMatches(propertyBStringOne, propertyCStringOne, true, false);
        assertNotMatches(propertyBStringOne, propertyCStringMany, true, false);

        assertNotMatches(propertyBStringMany, propertyNilAnyOne, true, false);
        assertNotMatches(propertyBStringMany, propertyNilAnyMany, true, false);
        assertMatches(propertyBStringMany, propertyAStringOne, true, false);
        assertMatches(propertyBStringMany, propertyAStringMany, true, false);
        assertMatches(propertyBStringMany, propertyBStringOne, true, false);
        assertMatches(propertyBStringMany, propertyBStringMany, true, false);
        assertNotMatches(propertyBStringMany, propertyCStringOne, true, false);
        assertNotMatches(propertyBStringMany, propertyCStringMany, true, false);

        assertNotMatches(propertyCStringOne, propertyNilAnyOne, true, false);
        assertNotMatches(propertyCStringOne, propertyNilAnyMany, true, false);
        assertNotMatches(propertyCStringOne, propertyAStringOne, true, false);
        assertNotMatches(propertyCStringOne, propertyAStringMany, true, false);
        assertNotMatches(propertyCStringOne, propertyBStringOne, true, false);
        assertNotMatches(propertyCStringOne, propertyBStringMany, true, false);
        assertMatches(propertyCStringOne, propertyCStringOne, true, false);
        assertNotMatches(propertyCStringOne, propertyCStringMany, true, false);

        assertNotMatches(propertyCStringMany, propertyNilAnyOne, true, false);
        assertNotMatches(propertyCStringMany, propertyNilAnyMany, true, false);
        assertNotMatches(propertyCStringMany, propertyAStringOne, true, false);
        assertNotMatches(propertyCStringMany, propertyAStringMany, true, false);
        assertNotMatches(propertyCStringMany, propertyBStringOne, true, false);
        assertNotMatches(propertyCStringMany, propertyBStringMany, true, false);
        assertMatches(propertyCStringMany, propertyCStringOne, true, false);
        assertMatches(propertyCStringMany, propertyCStringMany, true, false);

        assertMatches(functionNilOneAnyOne, propertyNilAnyOne, true, false);
        assertNotMatches(functionNilOneAnyOne, propertyNilAnyMany, true, false);
        assertMatches(functionNilOneAnyOne, propertyAStringOne, true, false);
        assertNotMatches(functionNilOneAnyOne, propertyAStringMany, true, false);
        assertMatches(functionNilOneAnyOne, propertyBStringOne, true, false);
        assertNotMatches(functionNilOneAnyOne, propertyBStringMany, true, false);
        assertMatches(functionNilOneAnyOne, propertyCStringOne, true, false);
        assertNotMatches(functionNilOneAnyOne, propertyCStringMany, true, false);

        assertMatches(functionNilOneAnyMany, propertyNilAnyOne, true, false);
        assertMatches(functionNilOneAnyMany, propertyNilAnyMany, true, false);
        assertMatches(functionNilOneAnyMany, propertyAStringOne, true, false);
        assertMatches(functionNilOneAnyMany, propertyAStringMany, true, false);
        assertMatches(functionNilOneAnyMany, propertyBStringOne, true, false);
        assertMatches(functionNilOneAnyMany, propertyBStringMany, true, false);
        assertMatches(functionNilOneAnyMany, propertyCStringOne, true, false);
        assertMatches(functionNilOneAnyMany, propertyCStringMany, true, false);

        assertNotMatches(functionNilManyAnyOne, propertyNilAnyOne, true, false);
        assertNotMatches(functionNilManyAnyOne, propertyNilAnyMany, true, false);
        assertNotMatches(functionNilManyAnyOne, propertyAStringOne, true, false);
        assertNotMatches(functionNilManyAnyOne, propertyAStringMany, true, false);
        assertNotMatches(functionNilManyAnyOne, propertyBStringOne, true, false);
        assertNotMatches(functionNilManyAnyOne, propertyBStringMany, true, false);
        assertNotMatches(functionNilManyAnyOne, propertyCStringOne, true, false);
        assertNotMatches(functionNilManyAnyOne, propertyCStringMany, true, false);

        assertNotMatches(functionNilManyAnyMany, propertyNilAnyOne, true, false);
        assertNotMatches(functionNilManyAnyMany, propertyNilAnyMany, true, false);
        assertNotMatches(functionNilManyAnyMany, propertyAStringOne, true, false);
        assertNotMatches(functionNilManyAnyMany, propertyAStringMany, true, false);
        assertNotMatches(functionNilManyAnyMany, propertyBStringOne, true, false);
        assertNotMatches(functionNilManyAnyMany, propertyBStringMany, true, false);
        assertNotMatches(functionNilManyAnyMany, propertyCStringOne, true, false);
        assertNotMatches(functionNilManyAnyMany, propertyCStringMany, true, false);

        assertNotMatches(functionAOneStringOne, propertyNilAnyOne, true, false);
        assertNotMatches(functionAOneStringOne, propertyNilAnyMany, true, false);
        assertMatches(functionAOneStringOne, propertyAStringOne, true, false);
        assertNotMatches(functionAOneStringOne, propertyAStringMany, true, false);
        assertNotMatches(functionAOneStringOne, propertyBStringOne, true, false);
        assertNotMatches(functionAOneStringOne, propertyBStringMany, true, false);
        assertNotMatches(functionAOneStringOne, propertyCStringOne, true, false);
        assertNotMatches(functionAOneStringOne, propertyCStringMany, true, false);

        assertNotMatches(functionAOneStringMany, propertyNilAnyOne, true, false);
        assertNotMatches(functionAOneStringMany, propertyNilAnyMany, true, false);
        assertMatches(functionAOneStringMany, propertyAStringOne, true, false);
        assertMatches(functionAOneStringMany, propertyAStringMany, true, false);
        assertNotMatches(functionAOneStringMany, propertyBStringOne, true, false);
        assertNotMatches(functionAOneStringMany, propertyBStringMany, true, false);
        assertNotMatches(functionAOneStringMany, propertyCStringOne, true, false);
        assertNotMatches(functionAOneStringMany, propertyCStringMany, true, false);

        assertNotMatches(functionBOneStringOne, propertyNilAnyOne, true, false);
        assertNotMatches(functionBOneStringOne, propertyNilAnyMany, true, false);
        assertMatches(functionBOneStringOne, propertyAStringOne, true, false);
        assertNotMatches(functionBOneStringOne, propertyAStringMany, true, false);
        assertMatches(functionBOneStringOne, propertyBStringOne, true, false);
        assertNotMatches(functionBOneStringOne, propertyBStringMany, true, false);
        assertNotMatches(functionBOneStringOne, propertyCStringOne, true, false);
        assertNotMatches(functionBOneStringOne, propertyCStringMany, true, false);

        assertNotMatches(functionBOneStringMany, propertyNilAnyOne, true, false);
        assertNotMatches(functionBOneStringMany, propertyNilAnyMany, true, false);
        assertMatches(functionBOneStringMany, propertyAStringOne, true, false);
        assertMatches(functionBOneStringMany, propertyAStringMany, true, false);
        assertMatches(functionBOneStringMany, propertyBStringOne, true, false);
        assertMatches(functionBOneStringMany, propertyBStringMany, true, false);
        assertNotMatches(functionBOneStringMany, propertyCStringOne, true, false);
        assertNotMatches(functionBOneStringMany, propertyCStringMany, true, false);

        assertNotMatches(functionCOneStringOne, propertyNilAnyOne, true, false);
        assertNotMatches(functionCOneStringOne, propertyNilAnyMany, true, false);
        assertNotMatches(functionCOneStringOne, propertyAStringOne, true, false);
        assertNotMatches(functionCOneStringOne, propertyAStringMany, true, false);
        assertNotMatches(functionCOneStringOne, propertyBStringOne, true, false);
        assertNotMatches(functionCOneStringOne, propertyBStringMany, true, false);
        assertMatches(functionCOneStringOne, propertyCStringOne, true, false);
        assertNotMatches(functionCOneStringOne, propertyCStringMany, true, false);

        assertNotMatches(functionCOneStringMany, propertyNilAnyOne, true, false);
        assertNotMatches(functionCOneStringMany, propertyNilAnyMany, true, false);
        assertNotMatches(functionCOneStringMany, propertyAStringOne, true, false);
        assertNotMatches(functionCOneStringMany, propertyAStringMany, true, false);
        assertNotMatches(functionCOneStringMany, propertyBStringOne, true, false);
        assertNotMatches(functionCOneStringMany, propertyBStringMany, true, false);
        assertMatches(functionCOneStringMany, propertyCStringOne, true, false);
        assertMatches(functionCOneStringMany, propertyCStringMany, true, false);
    }

    private void assertMatches(CoreInstance targetGenericType, CoreInstance valueGenericType, boolean covariant, boolean everythingMatchesNonConcreteTarget)
    {
        if (!GenericTypeMatch.genericTypeMatches(targetGenericType, valueGenericType, covariant, everythingMatchesNonConcreteTarget ? ParameterMatchBehavior.MATCH_ANYTHING : ParameterMatchBehavior.MATCH_CAUTIOUSLY, ParameterMatchBehavior.MATCH_CAUTIOUSLY, processorSupport))
        {
            StringBuilder message = new StringBuilder("Expected a match: target=");
            GenericType.print(message, targetGenericType, processorSupport);
            message.append(", value=");
            GenericType.print(message, valueGenericType, processorSupport);
            message.append(", covariant=");
            message.append(covariant);
            message.append(", everythingMatchesNonConcreteTarget=");
            message.append(everythingMatchesNonConcreteTarget);
            Assert.fail(message.toString());
        }
    }

    private void assertNotMatches(CoreInstance targetGenericType, CoreInstance valueGenericType, boolean covariant, boolean everythingMatchesNonConcreteTarget)
    {
        if (GenericTypeMatch.genericTypeMatches(targetGenericType, valueGenericType, covariant, everythingMatchesNonConcreteTarget ? ParameterMatchBehavior.MATCH_ANYTHING : ParameterMatchBehavior.MATCH_CAUTIOUSLY, ParameterMatchBehavior.MATCH_CAUTIOUSLY, processorSupport))
        {
            StringBuilder message = new StringBuilder("Did not expect a match: target=");
            GenericType.print(message, targetGenericType, processorSupport);
            message.append(", value=");
            GenericType.print(message, valueGenericType, processorSupport);
            message.append(", covariant=");
            message.append(covariant);
            message.append(", everythingMatchesNonConcreteTarget=");
            message.append(everythingMatchesNonConcreteTarget);
            Assert.fail(message.toString());
        }
    }

    private static CoreInstance newGenericType(String rawTypePath)
    {
        return newGenericType(rawTypePath, null, null);
    }

    private CoreInstance newGenericType(String rawTypePath, ListIterable<CoreInstance> typeArguments)
    {
        return newGenericType(rawTypePath, typeArguments, null);
    }

    private static CoreInstance newGenericType(String rawTypePath, ListIterable<CoreInstance> typeArguments, ListIterable<CoreInstance> multiplicityArguments)
    {
        CoreInstance rawType = runtime.getCoreInstance(rawTypePath);
        if (rawType == null)
        {
            throw new RuntimeException("Unknown type: " + rawTypePath);
        }
        return newGenericType(rawType, typeArguments, multiplicityArguments);
    }

    private static CoreInstance newGenericType(CoreInstance rawType, ListIterable<CoreInstance> typeArguments, ListIterable<CoreInstance> multiplicityArguments)
    {
        CoreInstance genericType = Type.wrapGenericType(rawType, processorSupport);
        if ((typeArguments != null) && typeArguments.notEmpty())
        {
            Instance.setValuesForProperty(genericType, M3Properties.typeArguments, typeArguments, processorSupport);
        }
        if ((multiplicityArguments != null) && multiplicityArguments.notEmpty())
        {
            Instance.setValuesForProperty(genericType, M3Properties.multiplicityArguments, multiplicityArguments, processorSupport);
        }
        return genericType;
    }

    private CoreInstance newNonConcreteGenericType(String typeParameterName)
    {
        CoreInstance typeParameter = repository.newAnonymousCoreInstance(null, runtime.getCoreInstance(M3Paths.TypeParameter));
        Instance.setValueForProperty(typeParameter, M3Properties.name, repository.newStringCoreInstance_cached(typeParameterName), processorSupport);

        CoreInstance genericType = repository.newAnonymousCoreInstance(null, runtime.getCoreInstance(M3Paths.GenericType));
        Instance.setValueForProperty(genericType, M3Properties.typeParameter, typeParameter, processorSupport);
        return genericType;
    }

    private CoreInstance newFunctionTypeGenericType(CoreInstance... signature)
    {
        return newGenericType(newFunctionType(signature), null, null);
    }

    private CoreInstance newFunctionType(CoreInstance... signature)
    {
        int length = signature.length;
        if (length < 2)
        {
            throw new IllegalArgumentException("Must be at least a return type and multiplicity");
        }
        if ((length % 2) != 0)
        {
            throw new IllegalArgumentException("Must be an even number of signature elements, got: " + length);
        }
        CoreInstance functionType = processorSupport.newAnonymousCoreInstance(null, M3Paths.FunctionType);

        int returnTypeIndex = length - 2;
        Instance.addValueToProperty(functionType, M3Properties.returnType, signature[returnTypeIndex], processorSupport);
        Instance.addValueToProperty(functionType, M3Properties.returnMultiplicity, signature[returnTypeIndex + 1], processorSupport);

        if (returnTypeIndex > 0)
        {
            MutableList<CoreInstance> parameters = FastList.newList(returnTypeIndex / 2);
            for (int i = 0; i < returnTypeIndex; i += 2)
            {
                CoreInstance parameter = processorSupport.newAnonymousCoreInstance(null, M3Paths.VariableExpression);
                Instance.addValueToProperty(parameter, M3Properties.genericType, signature[i], processorSupport);
                Instance.addValueToProperty(parameter, M3Properties.multiplicity, signature[i + 1], processorSupport);
                parameters.add(parameter);
            }
            Instance.setValuesForProperty(functionType, M3Properties.parameters, parameters, processorSupport);
        }
        return functionType;
    }
}
