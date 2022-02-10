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
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.SafeAppendable;

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

        ListIterable<? extends CoreInstance> params1 = functionType1.getValueForMetaPropertyToMany(M3Properties.parameters);
        ListIterable<? extends CoreInstance> params2 = functionType2.getValueForMetaPropertyToMany(M3Properties.parameters);
        int paramsCount = params1.size();
        if (paramsCount != params2.size())
        {
            return false;
        }
        for (int i = 0; i < paramsCount; i++)
        {
            CoreInstance param1 = params1.get(i);
            CoreInstance param2 = params2.get(i);

            CoreInstance genericType1 = param1.getValueForMetaPropertyToOne(M3Properties.genericType);
            CoreInstance genericType2 = param2.getValueForMetaPropertyToOne(M3Properties.genericType);
            if ((genericType1 == null) || (genericType2 == null) || !GenericType.genericTypesEqual(genericType1, genericType2, processorSupport))
            {
                return false;
            }

            CoreInstance multiplicity1 = Instance.getValueForMetaPropertyToOneResolved(param1, M3Properties.multiplicity, processorSupport);
            CoreInstance multiplicity2 = Instance.getValueForMetaPropertyToOneResolved(param2, M3Properties.multiplicity, processorSupport);
            if ((multiplicity1 == null) || (multiplicity2 == null) || !Multiplicity.multiplicitiesEqual(multiplicity1, multiplicity2))
            {
                return false;
            }
        }

        CoreInstance returnType1 = functionType1.getValueForMetaPropertyToOne(M3Properties.returnType);
        CoreInstance returnType2 = functionType2.getValueForMetaPropertyToOne(M3Properties.returnType);
        if ((returnType1 == null) || (returnType2 == null) || !GenericType.genericTypesEqual(returnType1, returnType2, processorSupport))
        {
            return false;
        }

        CoreInstance returnMult1 = Instance.getValueForMetaPropertyToOneResolved(functionType1, M3Properties.returnMultiplicity, processorSupport);
        CoreInstance returnMult2 = Instance.getValueForMetaPropertyToOneResolved(functionType2, M3Properties.returnMultiplicity, processorSupport);
        return (returnMult1 != null) && (returnMult2 != null) && Multiplicity.multiplicitiesEqual(returnMult1, returnMult2);
    }

    public static boolean isFunctionTypeFullyConcrete(CoreInstance functionType, ProcessorSupport processorSupport)
    {
        return Multiplicity.isMultiplicityConcrete(Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnMultiplicity, processorSupport)) &&
                GenericType.isGenericTypeFullyConcrete(functionType.getValueForMetaPropertyToOne(M3Properties.returnType), true, processorSupport) &&
                functionType.getValueForMetaPropertyToMany(M3Properties.parameters).allSatisfy(p ->
                        Multiplicity.isMultiplicityConcrete(Instance.getValueForMetaPropertyToOneResolved(p, M3Properties.multiplicity, processorSupport)) &&
                                GenericType.isGenericTypeFullyConcrete(p.getValueForMetaPropertyToOne(M3Properties.genericType), true, processorSupport));
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
        return (Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnMultiplicity, processorSupport) != null) &&
                GenericType.isGenericTypeFullyDefined(functionType.getValueForMetaPropertyToOne(M3Properties.returnType), processorSupport) &&
                functionType.getValueForMetaPropertyToMany(M3Properties.parameters).allSatisfy(p ->
                        (Instance.getValueForMetaPropertyToOneResolved(p, M3Properties.multiplicity, processorSupport) != null) &&
                                GenericType.isGenericTypeFullyDefined(p.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
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
        functionType.getValueForMetaPropertyToMany(M3Properties.parameters).forEach(parameter ->
        {
            CoreInstance parameterGenericType = parameter.getValueForMetaPropertyToOne(M3Properties.genericType);
            if (parameterGenericType != null)
            {
                GenericType.resolveImportStubs(parameterGenericType, processorSupport);
            }
            Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.multiplicity, processorSupport);
        });

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
        return print(new StringBuilder(), functionType, fullPaths, markImportStubs, processorSupport).toString();
    }

    /**
     * Print a human readable representation of a function type to
     * the given appendable.
     *
     * @param appendable       appendable to print to
     * @param functionType     function type to print
     * @param processorSupport processor support
     * @return the appendable
     */
    public static <T extends Appendable> T print(T appendable, CoreInstance functionType, ProcessorSupport processorSupport)
    {
        return print(appendable, functionType, false, processorSupport);
    }

    /**
     * Print a human readable representation of a function type to
     * the given appendable.
     *
     * @param appendable       appendable to print to
     * @param functionType     function type to print
     * @param fullPaths        whether to print full paths
     * @param processorSupport processor support
     * @return the appendable
     */
    public static <T extends Appendable> T print(T appendable, CoreInstance functionType, boolean fullPaths, ProcessorSupport processorSupport)
    {
        return print(appendable, functionType, fullPaths, false, processorSupport);
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
     * @return the appendable
     */
    public static <T extends Appendable> T print(T appendable, CoreInstance functionType, boolean fullPaths, boolean markImportStubs, ProcessorSupport processorSupport)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        safeAppendable.append('{');
        ListIterable<? extends CoreInstance> params = functionType.getValueForMetaPropertyToMany(M3Properties.parameters);
        int size = params.size();
        if (size > 0)
        {
            CoreInstance param = params.get(0);
            GenericType.print(safeAppendable, param.getValueForMetaPropertyToOne(M3Properties.genericType), fullPaths, markImportStubs, processorSupport);
            Multiplicity.print(safeAppendable, Instance.getValueForMetaPropertyToOneResolved(param, M3Properties.multiplicity, processorSupport), true);
            for (int i = 1; i < size; i++)
            {
                safeAppendable.append(", ");
                param = params.get(i);
                GenericType.print(safeAppendable, param.getValueForMetaPropertyToOne(M3Properties.genericType), fullPaths, markImportStubs, processorSupport);
                Multiplicity.print(safeAppendable, Instance.getValueForMetaPropertyToOneResolved(param, M3Properties.multiplicity, processorSupport), true);
            }
        }
        safeAppendable.append("->");
        GenericType.print(safeAppendable, functionType.getValueForMetaPropertyToOne(M3Properties.returnType), fullPaths, markImportStubs, processorSupport);
        Multiplicity.print(safeAppendable, Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnMultiplicity, processorSupport), true);
        safeAppendable.append('}');
        return appendable;
    }
}
