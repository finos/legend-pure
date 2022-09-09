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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction;

public interface Bridge
{
    @Deprecated
    default Function0<List<Object>> listBuilder()
    {
        return this::buildList;
    }

    @Deprecated
    default Function2<Multiplicity, ExecutionSupport, Boolean> hasToOneUpperBound()
    {
        return this::hasToOneUpperBound;
    }

    @Deprecated
    default Function2<Multiplicity, ExecutionSupport, Boolean> isToOne()
    {
        return this::isToOne;
    }

    @Deprecated
    default Function3<PackageableElement, String, ExecutionSupport, String> elementToPath()
    {
        return this::elementToPath;
    }

    <T> List<T> buildList();
    boolean hasToOneUpperBound(Multiplicity multiplicity, ExecutionSupport executionSupport);
    boolean isToOne(Multiplicity multiplicity, ExecutionSupport executionSupport);
    String elementToPath(PackageableElement element, String separator, ExecutionSupport executionSupport);

    LambdaCompiledExtended buildLambda(LambdaFunction<Object> lambdaFunction, SharedPureFunction<Object> pureFunction);
}
