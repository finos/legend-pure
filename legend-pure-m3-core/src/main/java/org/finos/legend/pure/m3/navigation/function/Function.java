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
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.tools.SafeAppendable;

import static org.finos.legend.pure.m3.navigation.function.FunctionDescriptor.writeDescriptorTypeAndMultiplicity;

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

    public static String prettyPrint(CoreInstance function, ProcessorSupport processorSupport)
    {
        return prettyPrint(new StringBuilder(128), function, processorSupport).toString();
    }

    private static <T extends Appendable> T prettyPrint(T appendable, CoreInstance function, ProcessorSupport processorSupport)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);

        // Write function name
        CoreInstance functionName = function.getValueForMetaPropertyToOne(M3Properties.functionName);
        safeAppendable.append(functionName != null ? PrimitiveUtilities.getStringValue(functionName) : "_ANONYMOUS_FN_");

        CoreInstance functionType = processorSupport.function_getFunctionType(function);

        // Write generics
        ListIterable<? extends CoreInstance> typeParameters = functionType.getValueForMetaPropertyToMany(M3Properties.typeParameters);
        if (typeParameters.notEmpty())
        {
            typeParameters.collect(typeParameter -> typeParameter.getValueForMetaPropertyToOne(M3Properties.name).getName()).appendString(safeAppendable, "<", ",", "");
            ListIterable<? extends CoreInstance> multiplicityParameters = functionType.getValueForMetaPropertyToMany(M3Properties.multiplicityParameters);
            if (multiplicityParameters.notEmpty())
            {
                multiplicityParameters.asLazy().collect(multiplicityParameter -> multiplicityParameter.getValueForMetaPropertyToOne(M3Properties.values).getName()).appendString(safeAppendable, "|", ",", "");
            }
            safeAppendable.append(">");
        }

        // Write parameter types and multiplicities

        safeAppendable.append('(');
        ListIterable<? extends CoreInstance> parameters = functionType.getValueForMetaPropertyToMany(M3Properties.parameters);
        if (parameters.notEmpty())
        {
            boolean first = true;
            for (CoreInstance parameter : parameters)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    safeAppendable.append(", ");
                }

                safeAppendable.append(parameter.getValueForMetaPropertyToOne(M3Properties.name).getName());
                safeAppendable.append(": ");
                CoreInstance parameterType = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.genericType, processorSupport);
                CoreInstance parameterMultiplicity = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.multiplicity, processorSupport);
                writeDescriptorTypeAndMultiplicity(safeAppendable, parameterType, parameterMultiplicity, processorSupport);
            }
        }
        safeAppendable.append(')');

        // Write return type and multiplicity
        safeAppendable.append(": ");
        CoreInstance returnType = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport);
        CoreInstance returnMultiplicity = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnMultiplicity, processorSupport);
        writeDescriptorTypeAndMultiplicity(safeAppendable, returnType, returnMultiplicity, processorSupport);
        return appendable;
    }
}
