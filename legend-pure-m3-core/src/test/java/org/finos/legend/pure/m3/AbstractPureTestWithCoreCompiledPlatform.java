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

package org.finos.legend.pure.m3;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

public class AbstractPureTestWithCoreCompiledPlatform extends AbstractPureTestWithCoreCompiled
{
    public static Pair<String, String> extra = Tuples.pair("/system/extra.pure",
            "Profile meta::pure::profiles::test\n" +
                    "{\n" +
                    "    stereotypes : [Test, BeforePackage, AfterPackage, ToFix, ExcludeAlloy, ExcludeLazy, AlloyOnly];\n" +
                    "    tags: [excludePlatform, sensitiveToStereotype];\n" +
                    "}" +
                    "Profile meta::pure::profiles::typemodifiers\n" +
                    "{\n" +
                    "    stereotypes: [abstract];\n" +
                    "}" +
                    "Enum meta::pure::functions::date::DurationUnit\n" +
                    "{\n" +
                    "    YEARS,\n" +
                    "    MONTHS,\n" +
                    "    WEEKS,\n" +
                    "    DAYS,\n" +
                    "    HOURS,\n" +
                    "    MINUTES,\n" +
                    "    SECONDS,\n" +
                    "    MILLISECONDS,\n" +
                    "    MICROSECONDS,\n" +
                    "    NANOSECONDS\n" +
                    "}" +
                    "Enum meta::pure::functions::hash::HashType\n" +
                    "{\n" +
                    "    MD5,\n" +
                    "    SHA1,\n" +
                    "    SHA256\n" +
                    "}" +
                    "native function meta::pure::functions::hash::hash(text: String[1], hashType: meta::pure::functions::hash::HashType[1]):String[1];" +
                    "native function meta::pure::functions::date::dateDiff(d1:Date[1], d2:Date[1], du:DurationUnit[1]):Integer[1];\n" +
                    "native function meta::pure::functions::collection::exists<T>(value:T[*], func:Function<{T[1]->Boolean[1]}>[1]):Boolean[1];\n" +
                    "native function meta::pure::functions::io::print(param:Any[*], max:Integer[1]):Nil[0];\n" +
                    "native function meta::pure::functions::collection::pair<U,V>(first:U[1], second:V[1]):Pair<U,V>[1];\n" +
                    "native function meta::pure::functions::meta::deactivate(var:Any[*]):ValueSpecification[1];\n" +
                    "native function meta::pure::functions::collection::concatenate<T>(set1:T[*], set2:T[*]):T[*];\n" +
                    "native function meta::pure::functions::meta::stereotype(profile:Profile[1], str:String[1]):Stereotype[1];\n" +
                    "native function meta::pure::functions::meta::tag(profile:Profile[1], str:String[1]):Tag[1];\n" +
                    "native function meta::pure::functions::asserts::assert(condition:Boolean[1], message:String[1]):Boolean[1];\n" +
                    "native function meta::pure::functions::asserts::fail(message:String[1]):Boolean[1];\n" +
                    "native function meta::pure::functions::string::startsWith(source:String[1], val:String[1]):Boolean[1];\n" +
                    "native function meta::pure::functions::string::trim(str:String[1]):String[1];\n" +
                    "native function meta::pure::functions::collection::removeDuplicates<T>(col:T[*]):T[*];\n" +
                    "native function meta::pure::functions::collection::forAll<T>(value:T[*], func:Function<{T[1]->Boolean[1]}>[1]):Boolean[1];\n" +
                    "native function meta::pure::functions::collection::removeDuplicates<T,V>(col:T[*], key:Function<{T[1]->V[1]}>[0..1], eql:Function<{V[1],V[1]->Boolean[1]}>[0..1]):T[*];\n" +
                    "native function meta::pure::functions::string::split(str:String[1], token:String[1]):String[*];\n" +
                    "native function meta::pure::functions::string::length(str:String[1]):Integer[1];\n" +
                    "native function meta::pure::functions::collection::fold<T,V|m>(value:T[*], func:Function<{T[1],V[m]->V[m]}>[1], accumulator:V[m]):V[m];\n" +
                    "native function meta::pure::functions::string::joinStrings(strings:String[*], separator:String[1]):String[1];\n" +
                    "native function meta::pure::functions::collection::contains(collection:Any[*], value:Any[1]):Boolean[1];\n" +
                    "native function meta::pure::functions::lang::if<T|m>(test:Boolean[1], valid:Function<{->T[m]}>[1], invalid:Function<{->T[m]}>[1]):T[m];\n" +
                    "native function meta::pure::functions::lang::eval<V|m>(func:Function<{->V[m]}>[1]):V[m];\n" +
                    "native function meta::pure::functions::lang::eval<T,V|m,n>(func:Function<{T[n]->V[m]}>[1], param:T[n]):V[m];\n" +
                    "native function meta::pure::functions::lang::eval<T,U,V|m,n,p>(func:Function<{T[n],U[p]->V[m]}>[1], param1:T[n], param2:U[p]):V[m];\n" +
                    "native function meta::pure::functions::lang::eval<T,U,V,W|m,n,p,q>(func:Function<{T[n],U[p],W[q]->V[m]}>[1], param1:T[n], param2:U[p], param3:W[q]):V[m];\n" +
                    "native function meta::pure::functions::lang::eval<T,U,V,W,X|m,n,p,q,r>(func:Function<{T[n],U[p],W[q],X[r]->V[m]}>[1], param1:T[n], param2:U[p], param3:W[q], param4:X[r]):V[m];\n" +
                    "native function meta::pure::functions::lang::eval<T,U,V,W,X,Y|m,n,p,q,r,s>(func:Function<{T[n],U[p],W[q],X[r],Y[s]->V[m]}>[1], param1:T[n], param2:U[p], param3:W[q], param4:X[r], param5:Y[s]):V[m];\n" +
                    "native function meta::pure::functions::lang::eval<T,U,V,W,X,Y,Z|m,n,p,q,r,s,t>(func:Function<{T[n],U[p],W[q],X[r],Y[s],Z[t]->V[m]}>[1], param1:T[n], param2:U[p], param3:W[q], param4:X[r], param5:Y[s], param6:Z[t]):V[m];\n" +
                    "native function meta::pure::functions::lang::eval<S,T,V,U,W,X,Y,Z|m,n,o,p,q,r,s,t>(func:Function<{S[n],T[o],U[p],W[q],X[r],Y[s],Z[t]->V[m]}>[1], param1:S[n], param2:T[o], param3:U[p], param4:W[q], param5:X[r], param6:Y[s], param7:Z[t]):V[m];");

    @Override
    public Pair<String, String> getExtra()
    {
        return extra;
    }
}
