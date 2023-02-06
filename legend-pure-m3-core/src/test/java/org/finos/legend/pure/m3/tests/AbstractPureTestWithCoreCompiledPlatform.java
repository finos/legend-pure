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

package org.finos.legend.pure.m3.tests;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;

public class AbstractPureTestWithCoreCompiledPlatform extends AbstractPureTestWithCoreCompiled
{
    public static final Pair<String, String> EXTRA = Tuples.pair("/system/extra.pure",
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
                    "native function meta::pure::functions::collection::pair<U,V>(first:U[1], second:V[1]):Pair<U,V>[1];\n" +
                    "native function meta::pure::functions::meta::deactivate(var:Any[*]):ValueSpecification[1];\n" +
                    "native function meta::pure::functions::string::trim(str:String[1]):String[1];\n" +
                    "native function meta::pure::functions::collection::forAll<T>(value:T[*], func:Function<{T[1]->Boolean[1]}>[1]):Boolean[1];\n" +
                    "native function meta::pure::functions::collection::contains(collection:Any[*], value:Any[1]):Boolean[1];\n");

    public static Pair<String, String> getExtra()
    {
        return EXTRA;
    }

    public static void setUpRuntime()
    {
        setUpRuntime(getFunctionExecution(), getCodeStorage(), getFactoryRegistryOverride(), getOptions(), getExtra());
    }

    public static void setUpRuntime(Pair<String, String> extra)
    {
        setUpRuntime(getFunctionExecution(), getCodeStorage(), getFactoryRegistryOverride(), getOptions(), extra);
    }

    public static void setUpRuntime(Pair<String, String> extra, boolean usecache)
    {
        setUpRuntime(getFunctionExecution(), getCodeStorage(), getFactoryRegistryOverride(), getOptions(), extra, usecache);
    }

    public static void setUpRuntime(Pair<String, String> extra, RichIterable<? extends CodeRepository> codeRepositories)
    {
        setUpRuntime(getFunctionExecution(), PureCodeStorage.createCodeStorage(getCodeStorageRoot(), codeRepositories), getFactoryRegistryOverride(), getOptions(), extra);
    }

    protected static MutableCodeStorage getCodeStorage()
    {
        return PureCodeStorage.createCodeStorage(getCodeStorageRoot(), getCodeRepositories());
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        return Lists.immutable.with(CodeRepository.newPlatformCodeRepository(), GenericCodeRepository.build("system", "((meta)|(system)|(apps::pure))(::.*)?", PlatformCodeRepository.NAME));
    }

}
