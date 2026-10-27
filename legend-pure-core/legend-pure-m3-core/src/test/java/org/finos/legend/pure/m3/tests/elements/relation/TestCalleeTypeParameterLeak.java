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

package org.finos.legend.pure.m3.tests.elements.relation;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestCalleeTypeParameterLeak extends AbstractPureTestWithCoreCompiledPlatform
{
    private static final String SOURCE_ID = "calleeTypeParameterLeak.pure";

    private static final String RENAME_LIKE =
            "import meta::pure::metamodel::relation::*;\n" +
                    "function test::renameLike<T,Z,K,V>(r:Relation<T>[1], old:ColSpec<Z=(?:K)⊆T>[1], new:ColSpec<V=(?:K)>[1]):Relation<T-Z+V>[1]\n" +
                    "{\n" +
                    "  $r->cast(@Relation<T-Z+V>)\n" +
                    "}\n";

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete(SOURCE_ID);
        runtime.compile();
    }

    @Test
    public void testCallerWithDifferentTypeParameterNames()
    {
        compileTestSource(SOURCE_ID, RENAME_LIKE +
                "function test::caller<A,B,C,D>(r:Relation<A>[1], old:ColSpec<B=(?:C)⊆A>[1], new:ColSpec<D=(?:C)>[1]):Relation<A-B+D>[1]\n" +
                "{\n" +
                "  $r->test::renameLike($old, $new)\n" +
                "}\n");
    }

    @Test
    public void testCallerWithSameTypeParameterNames()
    {
        compileTestSource(SOURCE_ID, RENAME_LIKE +
                "function test::caller<T,Z,K,V>(r:Relation<T>[1], old:ColSpec<Z=(?:K)⊆T>[1], new:ColSpec<V=(?:K)>[1]):Relation<T-Z+V>[1]\n" +
                "{\n" +
                "  $r->test::renameLike($old, $new)\n" +
                "}\n");
    }

    @Test
    public void testCallerWithSwappedTypeParameterNames()
    {
        compileTestSource(SOURCE_ID, RENAME_LIKE +
                "function test::caller<V,K,Z,T>(r:Relation<V>[1], old:ColSpec<K=(?:Z)⊆V>[1], new:ColSpec<T=(?:Z)>[1]):Relation<V-K+T>[1]\n" +
                "{\n" +
                "  $r->test::renameLike($old, $new)\n" +
                "}\n");
    }

    @Test
    public void testNestedGenericCallers()
    {
        compileTestSource(SOURCE_ID, RENAME_LIKE +
                "function test::middle<A,B,C,D>(r:Relation<A>[1], old:ColSpec<B=(?:C)⊆A>[1], new:ColSpec<D=(?:C)>[1]):Relation<A-B+D>[1]\n" +
                "{\n" +
                "  $r->test::renameLike($old, $new)\n" +
                "}\n" +
                "function test::outer<P,Q,R,S>(r:Relation<P>[1], old:ColSpec<Q=(?:R)⊆P>[1], new:ColSpec<S=(?:R)>[1]):Relation<P-Q+S>[1]\n" +
                "{\n" +
                "  $r->test::middle($old, $new)\n" +
                "}\n");
    }

    @Test
    public void testEqualBoundColumnTypeParameterResolvesToCallers()
    {
        compileTestSource(SOURCE_ID,
                "import meta::pure::metamodel::relation::*;\n" +
                        "native function test::firstValue<T,Z,K>(r:Relation<T>[1], col:ColSpec<Z=(?:K)⊆T>[1]):K[1];\n" +
                        "function test::caller<A,B,C>(r:Relation<A>[1], col:ColSpec<B=(?:C)⊆A>[1]):C[1]\n" +
                        "{\n" +
                        "  $r->test::firstValue($col)\n" +
                        "}\n");
    }

    @Test
    public void testEqualBoundPlainRelationReturn()
    {
        compileTestSource(SOURCE_ID,
                "import meta::pure::metamodel::relation::*;\n" +
                        "function test::pickLike<T,Z,K>(r:Relation<T>[1], old:ColSpec<Z=(?:K)⊆T>[1]):Relation<Z>[1]\n" +
                        "{\n" +
                        "  $r->cast(@Relation<Z>)\n" +
                        "}\n" +
                        "function test::caller<A,B,C>(r:Relation<A>[1], old:ColSpec<B=(?:C)⊆A>[1]):Relation<B>[1]\n" +
                        "{\n" +
                        "  $r->test::pickLike($old)\n" +
                        "}\n");
    }

    @Test
    public void testEqualBoundColSpecArray()
    {
        compileTestSource(SOURCE_ID,
                "import meta::pure::metamodel::relation::*;\n" +
                        "native function test::dropAll<T,Z,K>(r:Relation<T>[1], cols:ColSpecArray<Z=(?:K)⊆T>[1]):Relation<T-Z>[1];\n" +
                        "function test::caller<A,B,C>(r:Relation<A>[1], cols:ColSpecArray<B=(?:C)⊆A>[1]):Relation<A-B>[1]\n" +
                        "{\n" +
                        "  $r->test::dropAll($cols)\n" +
                        "}\n");
    }

    @Test
    public void testSubsetBoundCallerArgumentIntoEqualBoundCallee()
    {
        compileTestSource(SOURCE_ID,
                "import meta::pure::metamodel::relation::*;\n" +
                        "native function test::dropOne<T,Z,K>(r:Relation<T>[1], col:ColSpec<Z=(?:K)⊆T>[1]):Relation<T-Z>[1];\n" +
                        "function test::caller<A,B>(r:Relation<A>[1], col:ColSpec<B⊆A>[1]):Relation<A-B>[1]\n" +
                        "{\n" +
                        "  $r->test::dropOne($col)\n" +
                        "}\n");
    }

    @Test
    public void testSubsetBoundWithoutEqualStillWorks()
    {
        compileTestSource(SOURCE_ID,
                "import meta::pure::metamodel::relation::*;\n" +
                        "function test::dropLike<T,Z>(r:Relation<T>[1], old:ColSpec<Z⊆T>[1]):Relation<T-Z>[1]\n" +
                        "{\n" +
                        "  $r->cast(@Relation<T-Z>)\n" +
                        "}\n" +
                        "function test::caller<A,B>(r:Relation<A>[1], old:ColSpec<B⊆A>[1]):Relation<A-B>[1]\n" +
                        "{\n" +
                        "  $r->test::dropLike($old)\n" +
                        "}\n");
    }

    @Test
    public void testColumnLiteralArgumentsStillResolve()
    {
        compileTestSource(SOURCE_ID, RENAME_LIKE +
                "function test::caller(r:Relation<(id:Integer, name:String)>[1]):Relation<(id:Integer, label:String)>[1]\n" +
                "{\n" +
                "  $r->test::renameLike(~name, ~label)\n" +
                "}\n");
    }
}
