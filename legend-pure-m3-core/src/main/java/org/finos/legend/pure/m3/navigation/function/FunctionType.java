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
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.io.IOException;

public class FunctionType
{
    public static boolean isFunctionType(CoreInstance instance, ProcessorSupport processorSupport)
    {
        return processorSupport.instance_instanceOf(instance, M3Paths.FunctionType);
    }

    public static boolean functionTypesEqual(CoreInstance functionType1, CoreInstance functionType2, ProcessorSupport processorSupport)
    {
        if (functionType1 == functionType2)
        {
            return true;
        }

        ListIterable<? extends CoreInstance> params1 = Instance.getValueForMetaPropertyToManyResolved(functionType1, M3Properties.parameters, processorSupport);
        ListIterable<? extends CoreInstance> params2 = Instance.getValueForMetaPropertyToManyResolved(functionType2, M3Properties.parameters, processorSupport);
        int paramsCount = params1.size();
        if (paramsCount != params2.size())
        {
            return false;
        }
        for (int i = 0; i < paramsCount; i++)
        {
            CoreInstance param1 = params1.get(i);
            CoreInstance param2 = params2.get(i);

            CoreInstance genericType1 = Instance.getValueForMetaPropertyToOneResolved(param1, M3Properties.genericType, processorSupport);
            if (genericType1 == null)
            {
                return false;
            }

            CoreInstance genericType2 = Instance.getValueForMetaPropertyToOneResolved(param2, M3Properties.genericType, processorSupport);
            if (genericType2 == null)
            {
                return false;
            }

            if (!GenericType.genericTypesEqual(genericType1, genericType2, processorSupport))
            {
                return false;
            }

            CoreInstance multiplicity1 = Instance.getValueForMetaPropertyToOneResolved(param1, M3Properties.multiplicity, processorSupport);
            if (multiplicity1 == null)
            {
                return false;
            }

            CoreInstance multiplicity2 = Instance.getValueForMetaPropertyToOneResolved(param2, M3Properties.multiplicity, processorSupport);
            if (multiplicity2 == null)
            {
                return false;
            }

            if (!Multiplicity.multiplicitiesEqual(multiplicity1, multiplicity2))
            {
                return false;
            }
        }

        CoreInstance returnType1 = Instance.getValueForMetaPropertyToOneResolved(functionType1, M3Properties.returnType, processorSupport);
        if (returnType1 == null)
        {
            return false;
        }

        CoreInstance returnType2 = Instance.getValueForMetaPropertyToOneResolved(functionType2, M3Properties.returnType, processorSupport);
        if (returnType2 == null)
        {
            return false;
        }

        if (!GenericType.genericTypesEqual(returnType1, returnType2, processorSupport))
        {
            return false;
        }

        CoreInstance returnMult1 = Instance.getValueForMetaPropertyToOneResolved(functionType1, M3Properties.returnMultiplicity, processorSupport);
        if (returnMult1 == null)
        {
            return false;
        }

        CoreInstance returnMult2 = Instance.getValueForMetaPropertyToOneResolved(functionType2, M3Properties.returnMultiplicity, processorSupport);
        if (returnMult2 == null)
        {
            return false;
        }

        return Multiplicity.multiplicitiesEqual(returnMult1, returnMult2);
    }

    public static boolean isFunctionTypeFullyConcrete(CoreInstance functionType, ProcessorSupport processorSupport)
    {
        for (CoreInstance parameter : functionType.getValueForMetaPropertyToMany(M3Properties.parameters))
        {
            CoreInstance parameterGenericType = parameter.getValueForMetaPropertyToOne(M3Properties.genericType);
            if (!GenericType.isGenericTypeFullyConcrete(parameterGenericType, true, processorSupport))
            {
                return false;
            }

            CoreInstance parameterMultiplicity = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.multiplicity, processorSupport);
            if (!Multiplicity.isMultiplicityConcrete(parameterMultiplicity))
            {
                return false;
            }
        }

        CoreInstance returnType = functionType.getValueForMetaPropertyToOne(M3Properties.returnType);
        if (!GenericType.isGenericTypeFullyConcrete(returnType, true, processorSupport))
        {
            return false;
        }

        CoreInstance returnMultiplicity = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnMultiplicity, processorSupport);
        return Multiplicity.isMultiplicityConcrete(returnMultiplicity);
    }

    /**
     * Return whether the given function type is fully defined.  That is,
     * all of its parameters have fully defined generic types and
     * multiplicities, and it has a fully defined return type and
     * multiplicity.
     *
     * @param functionType     function type
     * @param processorSupport processor support
     * @return whether functionType is fully defined
     */
    public static boolean isFunctionTypeFullyDefined(CoreInstance functionType, ProcessorSupport processorSupport)
    {
        for (CoreInstance parameter : Instance.getValueForMetaPropertyToManyResolved(functionType, M3Properties.parameters, processorSupport))
        {
            CoreInstance parameterGenericType = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.genericType, processorSupport);
            if (!GenericType.isGenericTypeFullyDefined(parameterGenericType, processorSupport))
            {
                return false;
            }

            CoreInstance parameterMultiplicity = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.multiplicity, processorSupport);
            if (parameterMultiplicity == null)
            {
                return false;
            }
        }

        CoreInstance returnType = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport);
        if (!GenericType.isGenericTypeFullyDefined(returnType, processorSupport))
        {
            return false;
        }

        CoreInstance returnMultiplicity = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnMultiplicity, processorSupport);
        return returnMultiplicity != null;
    }

    public static CoreInstance getParameterGenericType(CoreInstance functionType, int parameterIndex)
    {
        ListIterable<? extends CoreInstance> parameters = functionType.getValueForMetaPropertyToMany(M3Properties.parameters);
        CoreInstance parameter;
        try
        {
            parameter = parameters.get(parameterIndex);
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new RuntimeException("Error getting generic type for parameter " + parameterIndex, e);
        }
        return parameter.getValueForMetaPropertyToOne(M3Properties.genericType);
    }

    public static void resolveImportStubs(CoreInstance functionType, ProcessorSupport processorSupport)
    {
        for (CoreInstance parameter : functionType.getValueForMetaPropertyToMany(M3Properties.parameters))
        {
            CoreInstance parameterGenericType = parameter.getValueForMetaPropertyToOne(M3Properties.genericType);
            if (parameterGenericType != null)
            {
                GenericType.resolveImportStubs(parameterGenericType, processorSupport);
            }
            Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.multiplicity, processorSupport);
        }

        CoreInstance returnType = functionType.getValueForMetaPropertyToOne(M3Properties.returnType);
        if (returnType != null)
        {
            GenericType.resolveImportStubs(returnType, processorSupport);
        }
        Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnMultiplicity, processorSupport);
    }

    /**
     * Print a human readable representation of a function type to
     * a string.
     *
     * @param functionType     function type to print
     * @param processorSupport processor support
     * @return human readable representation of functionType
     */
    public static String print(CoreInstance functionType, ProcessorSupport processorSupport)
    {
        return print(functionType, false, processorSupport);
    }

    /**
     * Print a human readable representation of a function type to
     * a string.
     *
     * @param functionType     function type to print
     * @param fullPaths        whether to print full paths
     * @param processorSupport processor support
     * @return human readable representation of functionType
     */
    public static String print(CoreInstance functionType, boolean fullPaths, ProcessorSupport processorSupport)
    {
        return print(functionType, fullPaths, false, processorSupport);
    }

    /**
     * Print a human readable representation of a function type to
     * a string.
     *
     * @param functionType     function type to print
     * @param fullPaths        whether to print full paths
     * @param markImportStubs  whether to mark import stubs with ~
     * @param processorSupport processor support
     * @return human readable representation of functionType
     */
    public static String print(CoreInstance functionType, boolean fullPaths, boolean markImportStubs, ProcessorSupport processorSupport)
    {
        StringBuilder builder = new StringBuilder();
        print(builder, functionType, fullPaths, markImportStubs, processorSupport);
        return builder.toString();
    }

    /**
     * Print a human readable representation of a function type to
     * the given appendable.
     *
     * @param appendable       appendable to print to
     * @param functionType     function type to print
     * @param processorSupport processor support
     */
    public static void print(Appendable appendable, CoreInstance functionType, ProcessorSupport processorSupport)
    {
        print(appendable, functionType, false, processorSupport);
    }

    /**
     * Print a human readable representation of a function type to
     * the given appendable.
     *
     * @param appendable       appendable to print to
     * @param functionType     function type to print
     * @param fullPaths        whether to print full paths
     * @param processorSupport processor support
     */
    public static void print(Appendable appendable, CoreInstance functionType, boolean fullPaths, ProcessorSupport processorSupport)
    {
        print(appendable, functionType, fullPaths, false, processorSupport);
    }

    /**
     * Print a human readable representation of a function type to
     * the given appendable.
     *
     * @param appendable       appendable to print to
     * @param functionType     function type to print
     * @param fullPaths        whether to print full paths
     * @param markImportStubs  whether to mark import stubs with ~
     * @param processorSupport processor support
     */
    public static void print(Appendable appendable, CoreInstance functionType, boolean fullPaths, boolean markImportStubs, ProcessorSupport processorSupport)
    {
        try
        {
            appendable.append('{');
            ListIterable<? extends CoreInstance> params = functionType.getValueForMetaPropertyToMany(M3Properties.parameters);
            int size = params.size();
            if (size > 0)
            {
                CoreInstance param = params.get(0);
                GenericType.print(appendable, param.getValueForMetaPropertyToOne(M3Properties.genericType), fullPaths, markImportStubs, processorSupport);
                Multiplicity.print(appendable, Instance.getValueForMetaPropertyToOneResolved(param, M3Properties.multiplicity, processorSupport), true);
                for (int i = 1; i < size; i++)
                {
                    appendable.append(", ");
                    param = params.get(i);
                    GenericType.print(appendable, param.getValueForMetaPropertyToOne(M3Properties.genericType), fullPaths, markImportStubs, processorSupport);
                    Multiplicity.print(appendable, Instance.getValueForMetaPropertyToOneResolved(param, M3Properties.multiplicity, processorSupport), true);
                }
            }
            appendable.append("->");
            GenericType.print(appendable, functionType.getValueForMetaPropertyToOne(M3Properties.returnType), fullPaths, markImportStubs, processorSupport);
            Multiplicity.print(appendable, Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnMultiplicity, processorSupport), true);
            appendable.append('}');
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
