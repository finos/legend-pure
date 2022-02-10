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

package org.finos.legend.pure.m3.navigation.function;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;

public class Function
{
    public static CoreInstance getFunctionType(CoreInstance function, Context context, ProcessorSupport processorSupport)
    {
        return (context == null) ? computeFunctionType(function, processorSupport) : context.getIfAbsentPutFunctionType(function, func -> computeFunctionType(func, processorSupport));
    }

    private static CoreInstance computeFunctionType(CoreInstance function, ProcessorSupport processorSupport)
    {
        CoreInstance classifierGenericType = function.getValueForMetaPropertyToOne(M3Properties.classifierGenericType);
        if ((classifierGenericType != null) && processorSupport.instance_instanceOf(function, M3Paths.FunctionDefinition))
        {
            ListIterable<? extends CoreInstance> typeArguments = classifierGenericType.getValueForMetaPropertyToMany(M3Properties.typeArguments);
            if (typeArguments.size() == 1)
            {
                return typeArguments.get(0).getValueForMetaPropertyToOne(M3Properties.rawType);
            }
        }

        CoreInstance functionGenericType = (classifierGenericType == null) ? Instance.getValueForMetaPropertyToOneResolved(function, M3Properties.genericType, processorSupport) : classifierGenericType;
        try
        {
            return GenericType.resolveFunctionGenericType(functionGenericType, processorSupport);
        }
        catch (PureException e)
        {
            if (e.getSourceInformation() != null)
            {
                throw e;
            }
            if (e.getInfo() != null)
            {
                throw new PureCompilationException(function.getSourceInformation(), e);
            }
            throw new PureCompilationException(function.getSourceInformation(), PackageableElement.writeUserPathForPackageableElement(new StringBuilder("Error computing function type for "), function).toString(), e);
        }
        catch (Exception e)
        {
            StringBuilder builder = PackageableElement.writeUserPathForPackageableElement(new StringBuilder("Error computing function type for "), function);
            String eMessage = e.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw new PureCompilationException(function.getSourceInformation(), builder.toString(), e);
        }
    }

    public static boolean isLambda(CoreInstance function, ProcessorSupport processorSupport)
    {
        return Instance.instanceOf(function, M3Paths.LambdaFunction, processorSupport);
    }

    public static CoreInstance getParameterGenericType(CoreInstance function, int parameterIndex, ProcessorSupport processorSupport)
    {
        CoreInstance functionType = processorSupport.function_getFunctionType(function);
        return FunctionType.getParameterGenericType(functionType, parameterIndex);
    }

    public static String print(CoreInstance function, ProcessorSupport processorSupport)
    {
        return print(new StringBuilder(128), function, processorSupport).toString();
    }

    public static <T extends Appendable> T print(T appendable, CoreInstance function, ProcessorSupport processorSupport)
    {
        return FunctionDescriptor.writeFunctionDescriptor(appendable, function, processorSupport);
    }
}
