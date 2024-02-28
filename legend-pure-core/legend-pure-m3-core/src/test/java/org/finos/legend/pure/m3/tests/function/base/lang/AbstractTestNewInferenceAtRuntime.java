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

package org.finos.legend.pure.m3.tests.function.base.lang;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.Test;

public abstract class AbstractTestNewInferenceAtRuntime extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testSimple() throws Exception
    {
        compileTestSource("fromString.pure", "Class Structure<Z,K>" +
                "{" +
                "     a:Z[1];" +
                "     b:K[1];" +
                "}" +
                "" +
                "function func<U,V>(p:Structure<U,V>[1]):String[1]" +
                "{" +
                "     ^Structure<U,V>(a = $p.a, b = $p.b);" +
                "     'ok';" +
                "}" +
                "" +
                "function test():Any[*]\n" +
                "{" +
                "   func(^Structure<Integer,String>(a=1,b='eeee'));\n" +
                "}");
        this.compileAndExecute("test():Any[*]");
    }

    @Test
    public void deeper() throws Exception
    {
        compileTestSource("fromString.pure", "Class Structure<Z,K>" +
                "{" +
                "     a:Z[1];" +
                "     b:K[1];" +
                "}" +
                "" +
                "function structure<A,B>(a:A[1],b:B[1]):Structure<A,B>[1]" +
                "{" +
                "  ^Structure<A,B>(a=$a, b=$b);" +
                "}" +
                "" +
                "function put<E,R>(a:E[1],b:R[1]):Structure<E,R>[1]" +
                "{" +
                "     structure($a,$b);" +
                "}" +
                "" +
                "function func<U,V>(p:Structure<U,V>[1]):String[1]" +
                "{" +
                "     ^Structure<U,V>(a = $p.a, b = $p.b);" +
                "     'ok';" +
                "}" +
                "" +
                "function test():Any[*]\n" +
                "{" +
                "   func(put(1,'eee'));\n" +
                "}");
        this.compileAndExecute("test():Any[*]");
    }


    @Test
    public void deeperEvenMore() throws Exception
    {
        compileTestSource("fromString.pure", "Class Structure<Z,K>" +
                "{" +
                "     a:Z[1];" +
                "     b:K[1];" +
                "}" +
                "" +
                "function structure<A,B>(a:A[1],b:B[1]):Structure<A,B>[1]" +
                "{" +
                "  ^Structure<A,B>(a=$a, b=$b);" +
                "}" +
                "" +
                "function put<E,R>(a:E[1],b:R[1]):Structure<E,R>[1]" +
                "{" +
                "     other(structure($a,$b));" +
                "}" +
                "" +
                "function other<W,Q>(s:Structure<W,Q>[1]):Structure<W,Q>[1]" +
                "{" +
                " $s" +
                "}" +
                "" +
                "function func<U,V>(p:Structure<U,V>[1]):String[1]" +
                "{" +
                "     ^Structure<U,V>(a = $p.a, b = $p.b);" +
                "     'ok';" +
                "}" +
                "" +
                "function test():Any[*]\n" +
                "{" +
                "   func(put(1,'eee'));\n" +
                "}");
        this.compileAndExecute("test():Any[*]");
    }


    @Test
    public void deeperEvenMoreUsingClassFunction() throws Exception
    {
        compileTestSource("fromString.pure", "Class M<U,V>" +
                "{" +
                "  vals : Structure<U,V>[*];" +
                "  put(t:U[1], v:V[1]){^$this(vals += structure($t,$v))}:M<U,V>[1];" +
                "}" +
                "" +
                "Class Structure<Z,K>" +
                "{" +
                "     a:Z[1];" +
                "     b:K[1];" +
                "}" +
                "" +
                "function structure<A,B>(a:A[1],b:B[1]):Structure<A,B>[1]" +
                "{" +
                "  ^Structure<A,B>(a=$a, b=$b);" +
                "}" +
                "" +
                "function func<U,V>(p:Structure<U,V>[1]):String[1]" +
                "{" +
                "     ^Structure<U,V>(a = $p.a, b = $p.b);" +
                "     'ok';" +
                "}" +
                "" +
                "function test():Any[*]\n" +
                "{" +
                "   ^M<String,Integer>(vals = ^Structure<String,Integer>(a='eee', b=2)).put('rr',55);" +
                "}");
        this.compileAndExecute("test():Any[*]");
    }

    @Test
    public void deeperEvenMoreUsingClassFunctionInFunc() throws Exception
    {
        compileTestSource("fromString.pure", "Class M<U,V>" +
                "{" +
                "  vals : Structure<U,V>[*];" +
                "  put(t:U[1], v:V[1]){let a = 'String';^$this(vals += structure($t,$v));}:M<U,V>[1];" +
                "}" +
                "" +
                "Class Structure<Z,K>" +
                "{" +
                "     a:Z[1];" +
                "     b:K[1];" +
                "}" +
                "" +
                "function structure<A,B>(a:A[1],b:B[1]):Structure<A,B>[1]" +
                "{" +
                "  ^Structure<A,B>(a=$a, b=$b);" +
                "}" +
                "" +
                "function func<U,V>(p:Structure<U,V>[1]):String[1]" +
                "{" +
                "     ^Structure<U,V>(a = $p.a, b = $p.b);" +
                "     'ok';" +
                "}" +
                "" +
                "function do<I,P>(i:I[1],p:P[1]):M<I,P>[1]" +
                "{" +
                " ^M<I,P>().put($i,$p);" +
                "}" +
                "" +
                "function test():Any[*]\n" +
                "{" +
                "   do('1','2');" +
                "}");
        this.compileAndExecute("test():Any[*]");
    }


    @Test
    public void usingLet() throws Exception
    {
        compileTestSource("fromString.pure", "Class M<U,V>" +
                "{" +
                //"  put(t:U[1], v:V[1]){$this}:M<U,V>[1];" +
                "}" +
                "" +
                "function put<U,V>(m:M<U,V>[1], t:U[1], v:V[1]):M<U,V>[1]" +
                "{" +
                " $m;" +
                "}" +
                "" +
                "function do<I,P>(i:I[1],p:P[1]):M<I,P>[1]" +
                "{" +
                " ^M<I,P>()" +
                "}" +
                "" +
                "function test():Any[*]\n" +
                "{" +
                "   let r = do('1','2');" +
                "   $r->put('1','3');" +
                "}");
        this.compileAndExecute("test():Any[*]");
    }

    @Test
    public void allWithTypeInference() throws Exception
    {
        compileTestSource("fromString.pure", "function test():Any[*]\n" +
                "{" +
                "   meta::pure::metamodel::function::ConcreteFunctionDefinition.all()->map(f|pair($f.name->toOne(),$f))" +
                "}" +
                "function meta::pure::functions::collection::pair<U,V>(first:U[1], second:V[1]):Pair<U,V>[1]\n" +
                "{\n" +
                "   ^Pair<U,V>(first=$first, second=$second);\n" +
                "}");
        this.compileAndExecute("test():Any[*]");
    }
}
