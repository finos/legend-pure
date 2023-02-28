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

package org.finos.legend.pure.runtime.java.compiled.extension;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaSourceCodeGenerator;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Bridge;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.Procedure3;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.Procedure4;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureFunction1;

import java.util.List;
import java.util.function.Function;

public interface CompiledExtension
{
    default List<StringJavaSource> getExtraJavaSources()
    {
        return Lists.fixedSize.empty();
    }

    default List<Native> getExtraNatives()
    {
        return Lists.fixedSize.empty();
    }

    default List<Procedure3<CoreInstance, JavaSourceCodeGenerator, ProcessorContext>> getExtraPackageableElementProcessors()
    {
        return Lists.fixedSize.empty();
    }

    default List<Procedure4<CoreInstance, CoreInstance, ProcessorContext, ProcessorSupport>> getExtraClassMappingProcessors()
    {
        return Lists.fixedSize.empty();
    }

    default RichIterable<? extends Pair<String, Function<? super CoreInstance, String>>> getExtraIdBuilders(ProcessorSupport processorSupport)
    {
        return Lists.immutable.empty();
    }

    default SetIterable<String> getExtraCorePath()
    {
        return Sets.immutable.empty();
    }

    default PureFunction1<Object, Object> getExtraFunctionEvaluation(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func, Bridge bridge, ExecutionSupport es)
    {
        return null;
    }

    String getRelatedRepository();
}
