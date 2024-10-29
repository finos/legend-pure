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
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class PathToElement extends NativeFunction
{
    private final boolean allowNotFound;

    public PathToElement(boolean allowNotFound)
    {
        this.allowNotFound = allowNotFound;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        String path = PrimitiveUtilities.getStringValue(Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport));
        String separator = PrimitiveUtilities.getStringValue(Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport));
        String pathForLookup = PackageableElement.DEFAULT_PATH_SEPARATOR.equals(separator) ? path : path.replace(separator, PackageableElement.DEFAULT_PATH_SEPARATOR);
        CoreInstance instance = processorSupport.package_getByUserPath(pathForLookup);
        if ((instance == null) && !this.allowNotFound)
        {
            throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), notFoundErrorMessage(path, separator, processorSupport), functionExpressionCallStack);
        }
        return ValueSpecificationBootstrap.wrapValueSpecification(instance, false, processorSupport);
    }

    private String notFoundErrorMessage(String path, String separator, ProcessorSupport processorSupport)
    {
        if (path.equals(separator))
        {
            return "Could not find " + path;
        }

        ListIterable<String> pathElements = PackageableElement.splitUserPath(path, separator);
        if (pathElements.size() == 1)
        {
            return "'" + path + "' is not a valid PackageableElement";
        }

        CoreInstance element = processorSupport.repository_getTopLevel(M3Paths.Root);
        if (element == null)
        {
            return "Cannot find " + M3Paths.Root;
        }

        StringBuilder builder = new StringBuilder(path.length() * 2).append('\'').append(path).append("' is not a valid PackageableElement");
        int i = 0;
        for (String name : pathElements)
        {
            element = _Package.findInPackage(element, name);
            if (element == null)
            {
                builder.append(": could not find '").append(name).append("' in ");
                if (i == 0)
                {
                    builder.append(M3Paths.Root);
                }
                else
                {
                    pathElements.subList(0, i).appendString(builder, separator);
                }
                return builder.toString();
            }
            i++;
        }
        return builder.toString();
    }
}
