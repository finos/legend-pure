// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.graph;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class ElementToPath extends NativeFunction
{
    private final ModelRepository repository;

    public ElementToPath(ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance element = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);
        String separator = PrimitiveUtilities.getStringValue(Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport));
        boolean includeRoot = PrimitiveUtilities.getBooleanValue(Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport));
        String path = includeRoot ? getPathWithRoot(element, separator) : getPathWithoutRoot(element, separator);
        return ValueSpecificationBootstrap.newStringLiteral(this.repository, path, processorSupport);
    }

    private String getPathWithRoot(CoreInstance element, String separator)
    {
        CoreInstance pkg = element.getValueForMetaPropertyToOne(M3Properties._package);
        String name = getName(element);
        if (pkg == null)
        {
            return name;
        }

        StringBuilder builder = new StringBuilder(64);
        PackageableElement.forEachPackagePathElement(pkg, p -> builder.append(getName(p)).append(separator));
        return builder.append(name).toString();
    }

    private String getPathWithoutRoot(CoreInstance element, String separator)
    {
        String name = getName(element);
        CoreInstance pkg = element.getValueForMetaPropertyToOne(M3Properties._package);
        String path;
        if (pkg == null)
        {
            path = name;
        }
        else
        {
            StringBuilder builder = new StringBuilder(64);
            PackageableElement.forEachPackagePathElement(pkg,
                    p ->
                    {
                        // Do nothing for root package
                    },
                    p -> builder.append(getName(p)).append(separator));
            path = builder.append(name).toString();
        }
        return M3Paths.Root.equals(path) ? "" : path;
    }

    private String getName(CoreInstance element)
    {
        return PrimitiveUtilities.getStringValue(element.getValueForMetaPropertyToOne(M3Properties.name), "");
    }
}
