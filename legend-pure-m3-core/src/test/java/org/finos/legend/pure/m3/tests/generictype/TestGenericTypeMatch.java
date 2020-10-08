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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestGenericTypeMatch extends AbstractPureTestWithCoreCompiledPlatform
{
    private CoreInstance any;
    private CoreInstance nil;
    private CoreInstance one;
    private CoreInstance zeroMany;

    @Before
    public void setUpBasicGenericTypes()
    {
        this.any = newGenericType(M3Paths.Any);
        this.nil = newGenericType(M3Paths.Nil);
        this.one = this.runtime.getCoreInstance(M3Paths.PureOne);
        this.zeroMany = this.runtime.getCoreInstance(M3Paths.ZeroMany);
    }

    @Test
    public void testMatches_ConcreteCovariantNoGenerics()
    {
        compileTestSource("Class test::A {}\n" +
                "Class test::B extends test::A {}\n" +
                "Class test::C {}\n");
        CoreInstance a = newGenericType("test::A");
        CoreInstance b = newGenericType("test::B");
        CoreInstance c = newGenericType("test::C");

        assertMatches(this.any, this.any, true, false);
        assertMatches(this.any, a, true, false);
        assertMatches(this.any, b, true, false);
        assertMatches(this.any, c, true, false);
        assertMatches(this.any, this.nil, true, false);

        assertNotMatches(a, this.any, true, false);
        assertMatches(a, a, true, false);
        assertMatches(a, b, true, false);
        assertNotMatches(a, c, true, false);
        assertMatches(a, this.nil, true, false);

        assertNotMatches(b, this.any, true, false);
        assertNotMatches(b, a, true, false);
        assertMatches(b, b, true, false);
        assertNotMatches(b, c, true, false);
        assertMatches(b, this.nil, true, false);

        assertNotMatches(c, this.any, true, false);
        assertNotMatches(c, a, true, false);
        assertNotMatches(c, b, true, false);
        assertMatches(c, c, true, false);
        assertMatches(c, this.nil, true, false);

        assertNotMatches(this.nil, this.any, true, false);
        assertNotMatches(this.nil, a, true, false);
        assertNotMatches(this.nil, b, true, false);
        assertNotMatches(this.nil, c, true, false);
        assertMatches(this.nil, this.nil, true, false);
    }

    @Test
    public void testMatches_ConcreteContravariantNoGenerics()
    {
        compileTestSource("Class test::A {}\n" +
                "Class test::B extends test::A {}\n" +
                "Class test::C {}\n");
        CoreInstance a = newGenericType("test::A");
        CoreInstance b = newGenericType("test::B");
        CoreInstance c = newGenericType("test::C");

        assertMatches(this.any, this.any, false, false);
        assertNotMatches(this.any, a, false, false);
        assertNotMatches(this.any, b, false, false);
        assertNotMatches(this.any, c, false, false);
        assertNotMatches(this.any, this.nil, false, false);

        assertMatches(a, this.any, false, false);
        assertMatches(a, a, false, false);
        assertNotMatches(a, b, false, false);
        assertNotMatches(a, c, false, false);
        assertNotMatches(a, this.nil, false, false);

        assertMatches(b, this.any, false, false);
        assertMatches(b, a, false, false);
        assertMatches(b, b, false, false);
        assertNotMatches(b, c, false, false);
        assertNotMatches(b, this.nil, false, false);

        assertMatches(c, this.any, false, false);
        assertNotMatches(c, a, false, false);
        assertNotMatches(c, b, false, false);
        assertMatches(c, c, false, false);
        assertNotMatches(c, this.nil, false, false);

        assertMatches(this.nil, this.any, false, false);
        assertMatches(this.nil, a, false, false);
        assertMatches(this.nil, b, false, false);
        assertMatches(this.nil, c, false, false);
        assertMatches(this.nil, this.nil, false, false);
    }

    @Test
    public void testMatches_ConcreteCovariantWithGenerics()
    {
        compileTestSource("Class test::A<X> {}\n" +
                "Class test::B<Y> extends test::A<Y> {}\n" +
                "Class test::C<Z> {}\n");

        CoreInstance aT = newGenericType("test::A", Lists.immutable.with(newNonConcreteGenericType("T")));
        CoreInstance aAny = newGenericType("test::A", Lists.immutable.with(this.any));
        CoreInstance aInteger = newGenericType("test::A", Lists.immutable.with(newGenericType(M3Paths.Integer)));
        CoreInstance aNil = newGenericType("test::A", Lists.immutable.with(this.nil));

        CoreInstance bT = newGenericType("test::B", Lists.immutable.with(newNonConcreteGenericType("T")));
        CoreInstance bAny = newGenericType("test::B", Lists.immutable.with(this.any));
        CoreInstance bInteger = newGenericType("test::B", Lists.immutable.with(newGenericType(M3Paths.Integer)));
        CoreInstance bNil = newGenericType("test::B", Lists.immutable.with(this.nil));

        CoreInstance cT = newGenericType("test::C", Lists.immutable.with(newNonConcreteGenericType("T")));
        CoreInstance cAny = newGenericType("test::C", Lists.immutable.with(this.any));
        CoreInstance cInteger = newGenericType("test::C", Lists.immutable.with(newGenericType(M3Paths.Integer)));
        CoreInstance cNil = newGenericType("test::C", Lists.immutable.with(this.nil));

        assertMatches(this.any, aT, true, false);
        assertMatches(this.any, aAny, true, false);
        assertMatches(this.any, aInteger, true, false);
        assertMatches(this.any, aNil, true, false);
        assertMatches(this.any, bT, true, false);
        assertMatches(this.any, bAny, true, false);
        assertMatches(this.any, bInteger, true, false);
        assertMatches(this.any, bNil, true, false);
        assertMatches(this.any, cT, true, false);
        assertMatches(this.any, cAny, true, false);
        assertMatches(this.any, cInteger, true, false);
        assertMatches(this.any, cNil, true, false);

        assertNotMatches(aT, this.any, true, false);
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
        assertMatches(aT, this.nil, true, false);

        assertNotMatches(aAny, this.any, true, false);
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
        assertMatches(aAny, this.nil, true, false);

        assertNotMatches(aInteger, this.any, true, false);
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
        assertMatches(aInteger, this.nil, true, false);

        assertNotMatches(aNil, this.any, true, false);
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
        assertMatches(aNil, this.nil, true, false);

        assertNotMatches(this.nil, aT, true, false);
        assertNotMatches(this.nil, aAny, true, false);
        assertNotMatches(this.nil, aInteger, true, false);
        assertNotMatches(this.nil, aNil, true, false);
        assertNotMatches(this.nil, bT, true, false);
        assertNotMatches(this.nil, bAny, true, false);
        assertNotMatches(this.nil, bInteger, true, false);
        assertNotMatches(this.nil, bNil, true, false);
        assertNotMatches(this.nil, cT, true, false);
        assertNotMatches(this.nil, cAny, true, false);
        assertNotMatches(this.nil, cInteger, true, false);
        assertNotMatches(this.nil, cNil, true, false);
    }

    @Test
    public void testMatches_ConcreteContravariantWithGenerics()
    {
        compileTestSource("Class test::A<X> {}\n" +
                "Class test::B<Y> extends test::A<Y> {}\n" +
                "Class test::C<Z> {}\n");

        CoreInstance aT = newGenericType("test::A", Lists.immutable.with(newNonConcreteGenericType("T")));
        CoreInstance aAny = newGenericType("test::A", Lists.immutable.with(this.any));
        CoreInstance aInteger = newGenericType("test::A", Lists.immutable.with(newGenericType(M3Paths.Integer)));
        CoreInstance aNil = newGenericType("test::A", Lists.immutable.with(this.nil));

        CoreInstance bT = newGenericType("test::B", Lists.immutable.with(newNonConcreteGenericType("T")));
        CoreInstance bAny = newGenericType("test::B", Lists.immutable.with(this.any));
        CoreInstance bInteger = newGenericType("test::B", Lists.immutable.with(newGenericType(M3Paths.Integer)));
        CoreInstance bNil = newGenericType("test::B", Lists.immutable.with(this.nil));

        CoreInstance cT = newGenericType("test::C", Lists.immutable.with(newNonConcreteGenericType("T")));
        CoreInstance cAny = newGenericType("test::C", Lists.immutable.with(this.any));
        CoreInstance cInteger = newGenericType("test::C", Lists.immutable.with(newGenericType(M3Paths.Integer)));
        CoreInstance cNil = newGenericType("test::C", Lists.immutable.with(this.nil));

        assertNotMatches(this.any, aT, false, false);
        assertNotMatches(this.any, aAny, false, false);
        assertNotMatches(this.any, aInteger, false, false);
        assertNotMatches(this.any, aNil, false, false);
        assertNotMatches(this.any, bT, false, false);
        assertNotMatches(this.any, bAny, false, false);
        assertNotMatches(this.any, bInteger, false, false);
        assertNotMatches(this.any, bNil, false, false);
        assertNotMatches(this.any, cT, false, false);
        assertNotMatches(this.any, cAny, false, false);
        assertNotMatches(this.any, cInteger, false, false);
        assertNotMatches(this.any, cNil, false, false);

        assertMatches(aT, this.any, false, false);
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
        assertNotMatches(aT, this.nil, false, false);

        assertMatches(aAny, this.any, false, false);
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
        assertNotMatches(aAny, this.nil, false, false);

        assertMatches(aInteger, this.any, false, false);
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
        assertNotMatches(aInteger, this.nil, false, false);

        assertMatches(aNil, this.any, false, false);
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
        assertNotMatches(aNil, this.nil, false, false);

        assertMatches(this.nil, aT, false, false);
        assertMatches(this.nil, aAny, false, false);
        assertMatches(this.nil, aInteger, false, false);
        assertMatches(this.nil, aNil, false, false);
        assertMatches(this.nil, bT, false, false);
        assertMatches(this.nil, bAny, false, false);
        assertMatches(this.nil, bInteger, false, false);
        assertMatches(this.nil, bNil, false, false);
        assertMatches(this.nil, cT, false, false);
        assertMatches(this.nil, cAny, false, false);
        assertMatches(this.nil, cInteger, false, false);
        assertMatches(this.nil, cNil, false, false);
    }

    @Test
    public void testMatches_NonConcreteTarget()
    {
        compileTestSource("Class test::A {}\n" +
                "Class test::B extends test::A {}\n" +
                "Class test::C<T> {}\n");
        CoreInstance a = newGenericType("test::A");
        CoreInstance b = newGenericType("test::B");
        CoreInstance cString = newGenericType("test::C", Lists.immutable.with(newGenericType(M3Paths.String)));

        CoreInstance t = newNonConcreteGenericType("T");
        CoreInstance u = newNonConcreteGenericType("U");

        // Covariant
        assertNotMatches(t, this.any, true, false);
        assertMatches(t, this.any, true, true);
        assertNotMatches(t, a, true, false);
        assertMatches(t, a, true, true);
        assertNotMatches(t, b, true, false);
        assertMatches(t, b, true, true);
        assertNotMatches(t, cString, true, false);
        assertMatches(t, cString, true, true);
        assertMatches(t, this.nil, true, false);
        assertMatches(t, this.nil, true, true);

        assertNotMatches(u, this.any, true, false);
        assertMatches(u, this.any, true, true);
        assertNotMatches(u, a, true, false);
        assertMatches(u, a, true, true);
        assertNotMatches(u, b, true, false);
        assertMatches(u, b, true, true);
        assertNotMatches(u, cString, true, false);
        assertMatches(u, cString, true, true);
        assertMatches(u, this.nil, true, false);
        assertMatches(u, this.nil, true, true);

        assertMatches(t, t, true, false);
        assertMatches(t, t, true, true);
        assertMatches(u, u, true, false);
        assertMatches(u, u, true, true);

        // Contravariant
        assertMatches(t, this.any, false, false);
        assertMatches(t, this.any, false, true);
        assertNotMatches(t, a, false, false);
        assertMatches(t, a, false, true);
        assertNotMatches(t, b, false, false);
        assertMatches(t, b, false, true);
        assertNotMatches(t, cString, false, false);
        assertMatches(t, cString, false, true);
        assertNotMatches(t, this.nil, false, false);
        assertMatches(t, this.nil, false, true);

        assertMatches(u, this.any, false, false);
        assertMatches(u, this.any, false, true);
        assertNotMatches(u, a, false, false);
        assertMatches(u, a, false, true);
        assertNotMatches(u, b, false, false);
        assertMatches(u, b, false, true);
        assertNotMatches(u, cString, false, false);
        assertMatches(u, cString, false, true);
        assertNotMatches(u, this.nil, false, false);
        assertMatches(u, this.nil, false, true);

        assertMatches(t, t, false, false);
        assertMatches(t, t, false, true);
        assertMatches(u, u, false, false);
        assertMatches(u, u, false, true);
    }

    @Test
    public void testMatches_ConcreteCovariantWithComplexGenerics()
    {
        compileTestSource("Class test::A\n" +
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

        CoreInstance propertyNilAnyOne = newGenericType(M3Paths.Property, Lists.immutable.with(this.nil, this.any), Lists.immutable.with(this.one));
        CoreInstance propertyNilAnyMany = newGenericType(M3Paths.Property, Lists.immutable.with(this.nil, this.any), Lists.immutable.with(this.zeroMany));
        CoreInstance propertyAStringOne = newGenericType(M3Paths.Property, Lists.immutable.with(newGenericType("test::A"), newGenericType(M3Paths.String)), Lists.immutable.with(this.one));
        CoreInstance propertyAStringMany = newGenericType(M3Paths.Property, Lists.immutable.with(newGenericType("test::A"), newGenericType(M3Paths.String)), Lists.immutable.with(this.zeroMany));
        CoreInstance propertyBStringOne = newGenericType(M3Paths.Property, Lists.immutable.with(newGenericType("test::B"), newGenericType(M3Paths.String)), Lists.immutable.with(this.one));
        CoreInstance propertyBStringMany = newGenericType(M3Paths.Property, Lists.immutable.with(newGenericType("test::B"), newGenericType(M3Paths.String)), Lists.immutable.with(this.zeroMany));
        CoreInstance propertyCStringOne = newGenericType(M3Paths.Property, Lists.immutable.with(newGenericType("test::C"), newGenericType(M3Paths.String)), Lists.immutable.with(this.one));
        CoreInstance propertyCStringMany = newGenericType(M3Paths.Property, Lists.immutable.with(newGenericType("test::C"), newGenericType(M3Paths.String)), Lists.immutable.with(this.zeroMany));

        CoreInstance functionNilOneAnyOne = newGenericType(M3Paths.Function, Lists.immutable.with(newFunctionTypeGenericType(this.nil, this.one, this.any, this.one)));
        CoreInstance functionNilOneAnyMany = newGenericType(M3Paths.Function, Lists.immutable.with(newFunctionTypeGenericType(this.nil, this.one, this.any, this.zeroMany)));
        CoreInstance functionNilManyAnyOne = newGenericType(M3Paths.Function, Lists.immutable.with(newFunctionTypeGenericType(this.nil, this.zeroMany, this.any, this.one)));
        CoreInstance functionNilManyAnyMany = newGenericType(M3Paths.Function, Lists.immutable.with(newFunctionTypeGenericType(this.nil, this.zeroMany, this.any, this.zeroMany)));
        CoreInstance functionAOneStringOne = newGenericType(M3Paths.Function, Lists.immutable.with(newFunctionTypeGenericType(newGenericType("test::A"), this.one, newGenericType(M3Paths.String), this.one)));
        CoreInstance functionAOneStringMany = newGenericType(M3Paths.Function, Lists.immutable.with(newFunctionTypeGenericType(newGenericType("test::A"), this.one, newGenericType(M3Paths.String), this.zeroMany)));
        CoreInstance functionBOneStringOne = newGenericType(M3Paths.Function, Lists.immutable.with(newFunctionTypeGenericType(newGenericType("test::B"), this.one, newGenericType(M3Paths.String), this.one)));
        CoreInstance functionBOneStringMany = newGenericType(M3Paths.Function, Lists.immutable.with(newFunctionTypeGenericType(newGenericType("test::B"), this.one, newGenericType(M3Paths.String), this.zeroMany)));
        CoreInstance functionCOneStringOne = newGenericType(M3Paths.Function, Lists.immutable.with(newFunctionTypeGenericType(newGenericType("test::C"), this.one, newGenericType(M3Paths.String), this.one)));
        CoreInstance functionCOneStringMany = newGenericType(M3Paths.Function, Lists.immutable.with(newFunctionTypeGenericType(newGenericType("test::C"), this.one, newGenericType(M3Paths.String), this.zeroMany)));

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
        if (!GenericTypeMatch.genericTypeMatches(targetGenericType, valueGenericType, covariant, everythingMatchesNonConcreteTarget ? ParameterMatchBehavior.MATCH_ANYTHING : ParameterMatchBehavior.MATCH_CAUTIOUSLY, ParameterMatchBehavior.MATCH_CAUTIOUSLY, this.processorSupport))
        {
            StringBuilder message = new StringBuilder("Expected a match: target=");
            GenericType.print(message, targetGenericType, this.processorSupport);
            message.append(", value=");
            GenericType.print(message, valueGenericType, this.processorSupport);
            message.append(", covariant=");
            message.append(covariant);
            message.append(", everythingMatchesNonConcreteTarget=");
            message.append(everythingMatchesNonConcreteTarget);
            Assert.fail(message.toString());
        }
    }

    private void assertNotMatches(CoreInstance targetGenericType, CoreInstance valueGenericType, boolean covariant, boolean everythingMatchesNonConcreteTarget)
    {
        if (GenericTypeMatch.genericTypeMatches(targetGenericType, valueGenericType, covariant, everythingMatchesNonConcreteTarget ? ParameterMatchBehavior.MATCH_ANYTHING : ParameterMatchBehavior.MATCH_CAUTIOUSLY, ParameterMatchBehavior.MATCH_CAUTIOUSLY, this.processorSupport))
        {
            StringBuilder message = new StringBuilder("Did not expect a match: target=");
            GenericType.print(message, targetGenericType, this.processorSupport);
            message.append(", value=");
            GenericType.print(message, valueGenericType, this.processorSupport);
            message.append(", covariant=");
            message.append(covariant);
            message.append(", everythingMatchesNonConcreteTarget=");
            message.append(everythingMatchesNonConcreteTarget);
            Assert.fail(message.toString());
        }
    }

    private CoreInstance newGenericType(String rawTypePath)
    {
        return newGenericType(rawTypePath, null, null);
    }

    private CoreInstance newGenericType(String rawTypePath, ListIterable<CoreInstance> typeArguments)
    {
        return newGenericType(rawTypePath, typeArguments, null);
    }

    private CoreInstance newGenericType(String rawTypePath, ListIterable<CoreInstance> typeArguments, ListIterable<CoreInstance> multiplicityArguments)
    {
        CoreInstance rawType = this.runtime.getCoreInstance(rawTypePath);
        if (rawType == null)
        {
            throw new RuntimeException("Unknown type: " + rawTypePath);
        }
        return newGenericType(rawType, typeArguments, multiplicityArguments);
    }

    private CoreInstance newGenericType(CoreInstance rawType, ListIterable<CoreInstance> typeArguments, ListIterable<CoreInstance> multiplicityArguments)
    {
        CoreInstance genericType = Type.wrapGenericType(rawType, this.processorSupport);
        if ((typeArguments != null) && typeArguments.notEmpty())
        {
            Instance.setValuesForProperty(genericType, M3Properties.typeArguments, typeArguments, this.processorSupport);
        }
        if ((multiplicityArguments != null) && multiplicityArguments.notEmpty())
        {
            Instance.setValuesForProperty(genericType, M3Properties.multiplicityArguments, multiplicityArguments, this.processorSupport);
        }
        return genericType;
    }

    private CoreInstance newNonConcreteGenericType(String typeParameterName)
    {
        CoreInstance typeParameter = this.repository.newAnonymousCoreInstance(null, this.runtime.getCoreInstance(M3Paths.TypeParameter));
        Instance.setValueForProperty(typeParameter, M3Properties.name, this.repository.newStringCoreInstance_cached(typeParameterName), this.processorSupport);

        CoreInstance genericType = this.repository.newAnonymousCoreInstance(null, this.runtime.getCoreInstance(M3Paths.GenericType));
        Instance.setValueForProperty(genericType, M3Properties.typeParameter, typeParameter, this.processorSupport);
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
        CoreInstance functionType = this.processorSupport.newAnonymousCoreInstance(null, M3Paths.FunctionType);

        int returnTypeIndex = length - 2;
        Instance.addValueToProperty(functionType, M3Properties.returnType, signature[returnTypeIndex], this.processorSupport);
        Instance.addValueToProperty(functionType, M3Properties.returnMultiplicity, signature[returnTypeIndex + 1], this.processorSupport);

        if (returnTypeIndex > 0)
        {
            MutableList<CoreInstance> parameters = FastList.newList(returnTypeIndex / 2);
            for (int i = 0; i < returnTypeIndex; i += 2)
            {
                CoreInstance parameter = this.processorSupport.newAnonymousCoreInstance(null, M3Paths.VariableExpression);
                Instance.addValueToProperty(parameter, M3Properties.genericType, signature[i], this.processorSupport);
                Instance.addValueToProperty(parameter, M3Properties.multiplicity, signature[i + 1], this.processorSupport);
                parameters.add(parameter);
            }
            Instance.setValuesForProperty(functionType, M3Properties.parameters, parameters, this.processorSupport);
        }
        return functionType;
    }
}
