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

package org.finos.legend.pure.m3.compiler.postprocessing.processor;

import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public enum NativeFunctionIdentifier
{
    Map("map", Sets.immutable.with("map_T_m__Function_1__V_m_", "map_T_MANY__Function_1__V_MANY_"), true),
    Filter("filter", Sets.immutable.with("filter_T_MANY__Function_1__T_MANY_"), true),
    MilestonedAllSingleDate("getAll", Sets.immutable.with("getAll_Class_1__Date_1__T_MANY_"), false),
    MilestonedAllBiTemporal("getAll", Sets.immutable.with("getAll_Class_1__Date_1__Date_1__T_MANY_"), false),
    Exists("exists", Sets.immutable.with("exists_T_MANY__Function_1__Boolean_1_"), true),
    SubType("subType", Sets.immutable.with("subType_Any_1__T_1__T_$0_1$_", "subType_Any_$0_1$__T_1__T_$0_1$_", "subType_Any_MANY__T_1__T_MANY_"), false),
    Project("project", Sets.immutable.with(""), true);

    private static final Predicate2<NativeFunctionIdentifier, String> HAS_LAMBDA_PARAM_AND_NAME = new Predicate2<NativeFunctionIdentifier, String>()
    {
        @Override
        public boolean accept(NativeFunctionIdentifier nativeFunctionIdentifier, String functionName)
        {
            return nativeFunctionIdentifier.hasLambdaParam && nativeFunctionIdentifier.equalFunctionName(functionName);
        }
    };

    private final ImmutableSet<String> functionIds;
    private final String functionName;
    private final boolean hasLambdaParam;

    NativeFunctionIdentifier(String functionName, Iterable<String> functionIds, boolean hasLambdaParam)
    {
        this.functionName = functionName;
        this.functionIds = Sets.immutable.withAll(functionIds);
        this.hasLambdaParam = hasLambdaParam;
    }

    public boolean ofType(CoreInstance function)
    {
        return this.functionIds.contains(function.getName());
    }

    private boolean equalFunctionName(String functionName)
    {
        return this.functionName.equals(functionName);
    }

    public static ListIterable<NativeFunctionIdentifier> getNativeFunctionIdentifiersWithLambdaParamsAndMatchingFunctionName(String functionName)
    {
        return ArrayIterate.selectWith(NativeFunctionIdentifier.values(), HAS_LAMBDA_PARAM_AND_NAME, functionName);
    }
}
