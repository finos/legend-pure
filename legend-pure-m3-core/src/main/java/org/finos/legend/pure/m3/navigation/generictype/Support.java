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

package org.finos.legend.pure.m3.navigation.generictype;

import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.set.strategy.mutable.UnifiedSetWithHashingStrategy;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.function.FunctionType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.navigation.typeparameter.TypeParameter;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

class Support
{
    static void resolveTypeArgumentsForGenericTypeToFindUsingInheritanceTree(GenericTypeWithXArguments current, GenericTypeWithXArguments multis, CoreInstance genericTypeToFind, MutableList<GenericTypeWithXArguments> results, ProcessorSupport processorSupport)
    {
        CoreInstance currentRawType = Instance.getValueForMetaPropertyToOneResolved(current.getGenericType(), M3Properties.rawType, processorSupport);
        CoreInstance rawTypeToFind = Instance.getValueForMetaPropertyToOneResolved(genericTypeToFind, M3Properties.rawType, processorSupport);
        if (currentRawType == rawTypeToFind)
        {
            results.add(current);
        }

        for (CoreInstance generalization : Instance.getValueForMetaPropertyToManyResolved(currentRawType, M3Properties.generalizations, processorSupport))
        {
            GenericTypeWithXArguments generalMulti = new GenericTypeWithXArguments(Instance.getValueForMetaPropertyToOneResolved(generalization, M3Properties.general, processorSupport), GenericType.bindMultiplicityParametersToMultiplicityArguments(Instance.getValueForMetaPropertyToOneResolved(generalization, M3Properties.general, processorSupport), multis.getArgumentsByParameterName(), processorSupport));
            GenericTypeWithXArguments general = new GenericTypeWithXArguments(Instance.getValueForMetaPropertyToOneResolved(generalization, M3Properties.general, processorSupport), GenericType.bindTypeParametersToTypeArguments(Instance.getValueForMetaPropertyToOneResolved(generalization, M3Properties.general, processorSupport), current.getArgumentsByParameterName(), multis.getArgumentsByParameterName(), processorSupport));
            resolveTypeArgumentsForGenericTypeToFindUsingInheritanceTree(general, generalMulti, genericTypeToFind, results, processorSupport);
        }
    }

    static void resolveMultiplicityArgumentsForGenericTypeToFindUsingInheritanceTree(GenericTypeWithXArguments current, CoreInstance rawTypeToFind, MutableList<GenericTypeWithXArguments> results, ProcessorSupport processorSupport)
    {
        CoreInstance currentRawType = Instance.getValueForMetaPropertyToOneResolved(current.getGenericType(), M3Properties.rawType, processorSupport);
        if (currentRawType == rawTypeToFind)
        {
            results.add(current);
        }

        for (CoreInstance generalization : Instance.getValueForMetaPropertyToManyResolved(currentRawType, M3Properties.generalizations, processorSupport))
        {
            GenericTypeWithXArguments general = new GenericTypeWithXArguments(Instance.getValueForMetaPropertyToOneResolved(generalization, M3Properties.general, processorSupport), GenericType.bindMultiplicityParametersToMultiplicityArguments(Instance.getValueForMetaPropertyToOneResolved(generalization, M3Properties.general, processorSupport), current.getArgumentsByParameterName(), processorSupport));
            resolveMultiplicityArgumentsForGenericTypeToFindUsingInheritanceTree(general, rawTypeToFind, results, processorSupport);
        }
    }

    static CoreInstance reprocessGenericTypeHavingNonConcreteTypeArguments(CoreInstance source, MapIterable<String, CoreInstance> genericTypeByTypeParameterNames, MapIterable<String, CoreInstance> sourceMulBinding, ProcessorSupport processorSupport)
    {
        CoreInstance result = processorSupport.newGenericType(source.getSourceInformation(), source, false);
        CoreInstance sourceRawType = source.getValueForMetaPropertyToOne(M3Properties.rawType);
        Instance.addValueToProperty(result, M3Properties.rawType, sourceRawType, processorSupport);
        for (CoreInstance typeArgument : Instance.getValueForMetaPropertyToManyResolved(source, M3Properties.typeArguments, processorSupport))
        {
            Instance.addValueToProperty(result, M3Properties.typeArguments, GenericType.makeTypeArgumentAsConcreteAsPossible(typeArgument, genericTypeByTypeParameterNames, sourceMulBinding, processorSupport), processorSupport);
        }
        for (CoreInstance multiplicityArgument : Instance.getValueForMetaPropertyToManyResolved(source, M3Properties.multiplicityArguments, processorSupport))
        {
            Instance.addValueToProperty(result, M3Properties.multiplicityArguments, Multiplicity.makeMultiplicityAsConcreteAsPossible(multiplicityArgument, sourceMulBinding), processorSupport);
        }
        return result;
    }

    static CoreInstance reprocessFunctionTypeReplaceTypeParamsByConcreteTypes(CoreInstance genericFunctionType, MapIterable<String, CoreInstance> resolved, MapIterable<String, CoreInstance> sourceMulBinding, ProcessorSupport processorSupport)
    {
        CoreInstance functionType = Instance.getValueForMetaPropertyToOneResolved(genericFunctionType, M3Properties.rawType, processorSupport);
        CoreInstance newFunctionType = processorSupport.newCoreInstance(functionType.getName(), processorSupport.getClassifier(functionType), functionType.getSourceInformation());

        for (CoreInstance instance : Instance.getValueForMetaPropertyToManyResolved(functionType, M3Properties.parameters, processorSupport))
        {
            if (Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.genericType, processorSupport) == null)
            {
                CoreInstance inst = processorSupport.newCoreInstance(instance.getName(), M3Paths.VariableExpression, null);
                Instance.addValueToProperty(inst, M3Properties.name, Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.name, processorSupport), processorSupport);
                Instance.addValueToProperty(newFunctionType, M3Properties.parameters, inst, processorSupport);
            }
            else if (GenericType.isGenericTypeConcrete(Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.genericType, processorSupport), processorSupport) && Multiplicity.isMultiplicityConcrete(Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.multiplicity, processorSupport)))
            {
                Instance.addValueToProperty(newFunctionType, M3Properties.parameters, instance, processorSupport);
            }
            else
            {
                CoreInstance inst = processorSupport.newCoreInstance(instance.getName(), M3Paths.VariableExpression, null);
                Instance.addValueToProperty(inst, M3Properties.name, Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.name, processorSupport), processorSupport);
                Instance.addValueToProperty(inst, M3Properties.multiplicity, Multiplicity.makeMultiplicityAsConcreteAsPossible(Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.multiplicity, processorSupport), sourceMulBinding), processorSupport);
                Instance.addValueToProperty(inst, M3Properties.genericType, GenericType.makeTypeArgumentAsConcreteAsPossible(Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.genericType, processorSupport), resolved, sourceMulBinding, processorSupport), processorSupport);
                Instance.addValueToProperty(newFunctionType, M3Properties.parameters, inst, processorSupport);
            }
        }
        Instance.addValueToProperty(newFunctionType, M3Properties.returnType, GenericType.makeTypeArgumentAsConcreteAsPossible(Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport), resolved, sourceMulBinding, processorSupport), processorSupport);
        Instance.addValueToProperty(newFunctionType, M3Properties.returnMultiplicity, Multiplicity.makeMultiplicityAsConcreteAsPossible(Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnMultiplicity, processorSupport), sourceMulBinding), processorSupport);

        return Type.wrapGenericType(newFunctionType, processorSupport);
    }


    static boolean isCovariant(CoreInstance genericType, CoreInstance otherGenericType, ProcessorSupport processorSupport)
    {
        return testVariance(genericType, otherGenericType, true, processorSupport);
    }

    static boolean isContravariant(CoreInstance genericType, CoreInstance otherGenericType, ProcessorSupport processorSupport)
    {
        return testVariance(genericType, otherGenericType, false, processorSupport);
    }

    static boolean testVariance(CoreInstance genericType, CoreInstance otherGenericType, boolean covariant, ProcessorSupport processorSupport)
    {
        if (GenericType.genericTypesEqual(genericType, otherGenericType, processorSupport))
        {
            return true;
        }

        if (!GenericType.isGenericTypeConcrete(genericType, processorSupport) || !GenericType.isGenericTypeConcrete(otherGenericType, processorSupport))
        {
            boolean a = !GenericType.isGenericTypeConcrete(genericType, processorSupport) && !GenericType.isGenericTypeConcrete(otherGenericType, processorSupport) && GenericType.getTypeParameterName(genericType, processorSupport).equals(GenericType.getTypeParameterName(otherGenericType, processorSupport));
            boolean b = GenericType.isGenericTypeConcrete(genericType, processorSupport) && (covariant?"Any":"Nil").equals(Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport).getName()) ||
                        GenericType.isGenericTypeConcrete(otherGenericType, processorSupport) && (covariant?"Any":"Nil").equals(Instance.getValueForMetaPropertyToOneResolved(otherGenericType, M3Properties.rawType, processorSupport).getName());
            // Use case b manages Comparison between Property<Employee, Any> (coming from the properties class def) and Property<Employee, T>  (coming fom a specific property added having T as a return type)
            // In this context we don't even know T .. we just want to add the property to the class (not even instantiated)
            // We check Any only but we should check the range of T at one point (T extends Vehicle for example ...)
            return a || b || !GenericType.isGenericTypeConcrete(otherGenericType, processorSupport);
        }

        if (!covariant && Type.isBottomType(Instance.getValueForMetaPropertyToOneResolved(otherGenericType, M3Properties.rawType, processorSupport), processorSupport))
        {
            return true;
        }

        boolean basicTypeTest = covariant?
                processorSupport.type_subTypeOf(Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport), Instance.getValueForMetaPropertyToOneResolved(otherGenericType, M3Properties.rawType, processorSupport)):
                processorSupport.type_subTypeOf(Instance.getValueForMetaPropertyToOneResolved(otherGenericType,M3Properties.rawType, processorSupport), Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport));

        if (basicTypeTest)
        {
            ListIterable<? extends CoreInstance> typeArguments = Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.typeArguments, processorSupport);
            ListIterable<? extends CoreInstance> otherTypeArguments = Instance.getValueForMetaPropertyToManyResolved(otherGenericType, M3Properties.typeArguments, processorSupport);
            boolean genericCompatibility = true;
            if (covariant)
            {
                if (otherTypeArguments.isEmpty())
                {
                    return genericCompatibility;
                }
                GenericTypeWithXArguments homogenizedTypeArgs = GenericType.resolveClassTypeParameterUsingInheritance(genericType, otherGenericType, processorSupport);
                ListIterable<? extends CoreInstance> otherTypeParameters = Instance.getValueForMetaPropertyToManyResolved(Instance.getValueForMetaPropertyToOneResolved(otherGenericType, M3Properties.rawType, processorSupport), M3Properties.typeParameters, processorSupport);
                for (int i = 0; i < otherTypeArguments.size(); i++)
                {
                    boolean isCovariant = TypeParameter.isCovariant(otherTypeParameters.get(i));
                    genericCompatibility &= GenericType.isGenericCompatibleWith(homogenizedTypeArgs.getArgumentsByParameterName().get(Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToManyResolved(Instance.getValueForMetaPropertyToOneResolved(otherGenericType, M3Properties.rawType, processorSupport), M3Properties.typeParameters, processorSupport).get(i), M3Properties.name, processorSupport).getName()), otherTypeArguments.get(i), isCovariant, processorSupport);
                }
            }
            else
            {
                if (typeArguments.isEmpty())
                {
                    return genericCompatibility;
                }
                GenericTypeWithXArguments homogenizedTypeArgs = GenericType.resolveClassTypeParameterUsingInheritance(otherGenericType, genericType, processorSupport);
                ListIterable<? extends CoreInstance> typeParameters = Instance.getValueForMetaPropertyToManyResolved(Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport), M3Properties.typeParameters, processorSupport);
                for (int i = 0; i < typeArguments.size(); i++)
                {
                    boolean isCovariant = TypeParameter.isCovariant(typeParameters.get(i));
                    genericCompatibility &= GenericType.isGenericCompatibleWith(typeArguments.get(i), homogenizedTypeArgs.getArgumentsByParameterName().get(Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToManyResolved(Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport), M3Properties.typeParameters, processorSupport).get(i), M3Properties.name, processorSupport).getName()), isCovariant, processorSupport);
                }
            }
            return genericCompatibility;
        }
        return false;
    }

    static CoreInstance getBestGenericTypeUsingContravariance(ListIterable<CoreInstance> genericTypeSet, ProcessorSupport processorSupport)
    {
        int genericTypeCount = genericTypeSet.size();
        if (genericTypeCount == 0)
        {
            // Empty set, so we return Any
            return Type.wrapGenericType(processorSupport.type_TopType(), processorSupport);
        }
        else if (genericTypeCount == 1)
        {
            // Only one generic type, so we return a copy of it
            return GenericType.copyGenericType(genericTypeSet.get(0), processorSupport);
        }

        CoreInstance bottomType = processorSupport.type_BottomType();

        MutableList<CoreInstance> rawTypes = FastList.newList(genericTypeCount);
        for (CoreInstance genericType : genericTypeSet)
        {
            CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
            if (rawType == bottomType)
            {
                // Nil is present, so that's going to be the result
                return Type.wrapGenericType(rawType, processorSupport);
            }
            rawTypes.add(rawType);
        }

        for (int i = 0; i < genericTypeCount; i++)
        {
            CoreInstance rawType = rawTypes.get(i);
            if (rawType != null)
            {
                boolean subTypeOfAllTypes = true;
                for (int j = 0; subTypeOfAllTypes && (j < genericTypeCount); j++)
                {
                    if (j != i)
                    {
                        CoreInstance otherRawType = rawTypes.get(j);
                        if ((otherRawType != null) && !processorSupport.type_subTypeOf(rawType, otherRawType))
                        {
                            subTypeOfAllTypes = false;
                        }
                    }
                }
                if (subTypeOfAllTypes)
                {
                    return GenericType.copyGenericType(genericTypeSet.get(i), processorSupport);
                }
            }
        }
        // Cannot find a common type, so we return Nil
        return Type.wrapGenericType(bottomType, processorSupport);
    }

    static CoreInstance getBestGenericTypeUsingCovariance(ListIterable<CoreInstance> genericTypeSet, CoreInstance knownMostGeneralGenericTypeBound, ProcessorSupport processorSupport)
    {
        int genericTypeCount = genericTypeSet.size();
        if (genericTypeCount == 0)
        {
            // Empty set, so we return Nil
            return Type.wrapGenericType(processorSupport.type_BottomType(), processorSupport);
        }
        if (genericTypeCount == 1)
        {
            // Only one generic type, so we return a copy of it
            return GenericType.copyGenericType(genericTypeSet.get(0), processorSupport);
        }

        boolean hasNonBottomGenericType = false;
        CoreInstance topType = processorSupport.type_TopType();
        CoreInstance bottomType = processorSupport.type_BottomType();
        MutableList<CoreInstance> nonBottomConcreteGenericTypeList = FastList.newList(Math.min(genericTypeCount, 16));
        AlmostGenericTypeMutableSet nonBottomConcreteGenericTypeSet = new AlmostGenericTypeMutableSet(processorSupport, Math.min(genericTypeCount, 16));
        for (CoreInstance genericType : genericTypeSet)
        {
            CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
            if (rawType == topType)
            {
                // Any is present, so that's going to be the result
                return Type.wrapGenericType(rawType, processorSupport);
            }
            if (knownMostGeneralGenericTypeBound != null && GenericType.genericTypesEqual(genericType, knownMostGeneralGenericTypeBound, processorSupport))
            {
                return GenericType.copyGenericType(genericType, processorSupport);
            }
            if (rawType != bottomType)
            {
                // Non-Nil (possibly null) raw type
                hasNonBottomGenericType = true;
                if ((rawType != null) && nonBottomConcreteGenericTypeSet.add(genericType))
                {
                    nonBottomConcreteGenericTypeList.add(genericType);
                }
            }
        }
        if (!hasNonBottomGenericType)
        {
            // All types are Nil, so we return Nil
            return Type.wrapGenericType(bottomType, processorSupport);
        }
        int nonBottomConcreteGenericTypesCount = nonBottomConcreteGenericTypeList.size();
        if (nonBottomConcreteGenericTypesCount == 0)
        {
            // All types are non-concrete (or Nil)
            // Check if they are the same
            String current = null;
            for (CoreInstance g : genericTypeSet)
            {
                String tp = GenericType.getTypeParameterName(g, processorSupport);
                if (tp != null)
                {
                    if ((current != null) && !tp.equals(current))
                    {
                        // No so we return Any
                        return Type.wrapGenericType(topType, processorSupport);
                    }
                    current = tp;
                }
            }
            return GenericType.copyGenericType(genericTypeSet.get(0), processorSupport);
        }
        if (nonBottomConcreteGenericTypesCount == 1)
        {
            // Only one concrete type, so we return a copy of it
            return GenericType.copyGenericType(nonBottomConcreteGenericTypeList.get(0), processorSupport);
        }

        // General case
        MutableList<ListIterable<CoreInstance>> generalizations = FastList.newList(nonBottomConcreteGenericTypesCount);
        for (CoreInstance genericType : nonBottomConcreteGenericTypeList)
        {
            ListIterable<CoreInstance> genls = GenericType.getAllSuperTypesIncludingSelf(genericType, processorSupport);
            generalizations.add(genls);
        }

        int shortestLength = generalizations.get(0).size();
        int shortestIndex = 0;
        for (int i = 1; i < nonBottomConcreteGenericTypesCount; i++)
        {
            int length = generalizations.get(i).size();
            if (length < shortestLength)
            {
                shortestLength = length;
                shortestIndex = i;
            }
        }

        CoreInstance commonRawType = null;
        MutableList<CoreInstance> genericTypesAtSameLevel = FastList.newList(nonBottomConcreteGenericTypesCount);
        ListIterable<CoreInstance> shortestGeneralizations = generalizations.remove(shortestIndex);
        // Special handling for FunctionTypes
        if (FunctionType.isFunctionType(Instance.getValueForMetaPropertyToOneResolved(shortestGeneralizations.get(0), M3Properties.rawType, processorSupport), processorSupport))
        {
            MutableList<CoreInstance> functionTypes = FastList.newList(nonBottomConcreteGenericTypeList).with(shortestGeneralizations.get(0));
            for (ListIterable<CoreInstance> genls : generalizations)
            {
                if (!FunctionType.isFunctionType(Instance.getValueForMetaPropertyToOneResolved(genls.get(0), M3Properties.rawType, processorSupport), processorSupport))
                {
                    // Any is the only thing in common between FunctionTypes and non-FunctionTypes
                    return Type.wrapGenericType(topType, processorSupport);
                }
                functionTypes.add(genls.get(0));
            }
            return GenericType.findBestCommonGenericType(functionTypes, true, true, processorSupport);
        }
        for (CoreInstance genericType : shortestGeneralizations)
        {
            genericTypesAtSameLevel.add(genericType);
            CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
            for (ListIterable<CoreInstance> genls : generalizations)
            {
                boolean present = false;
                for (CoreInstance general : genls)
                {
                    if (rawType == Instance.getValueForMetaPropertyToOneResolved(general, M3Properties.rawType, processorSupport))
                    {
                        genericTypesAtSameLevel.add(general);
                        present = true;
                        break;
                    }
                }
                if (!present)
                {
                    genericTypesAtSameLevel.clear();
                    break;
                }
            }
            if (genericTypesAtSameLevel.notEmpty())
            {
                commonRawType = rawType;
                break;
            }
        }
        if (commonRawType == null)
        {
            // If we can't figure out anything better, we return Any
            return Type.wrapGenericType(topType, processorSupport);
        }

        // We now have the generic types at the same raw type level
        CoreInstance resultGenericType = Type.wrapGenericType(commonRawType, processorSupport);
        ListIterable<? extends CoreInstance> typeParameters = commonRawType.getValueForMetaPropertyToMany(M3Properties.typeParameters);
        if (typeParameters.notEmpty())
        {
            MutableList<CoreInstance> newTypeArguments = FastList.newList(typeParameters.size());

            MutableList<ListIterable<? extends CoreInstance>> typeArguments = FastList.newList(nonBottomConcreteGenericTypesCount);
            for (CoreInstance genericType : genericTypesAtSameLevel)
            {
                typeArguments.add(genericType.getValueForMetaPropertyToMany(M3Properties.typeArguments));
            }
            for (int i = 0; i < typeParameters.size(); i++)
            {
                CoreInstance typeParameter = typeParameters.get(i);
                boolean covariant = TypeParameter.isCovariant(typeParameter);
                MutableList<CoreInstance> parameterTypeArguments = FastList.newList(nonBottomConcreteGenericTypesCount);
                for (ListIterable<? extends CoreInstance> args : typeArguments)
                {
                    CoreInstance typeArg = args.get(i);
                    parameterTypeArguments.add(typeArg);
                }
                CoreInstance typeArgument = covariant ? getBestGenericTypeUsingCovariance(parameterTypeArguments, null, processorSupport) : getBestGenericTypeUsingContravariance(parameterTypeArguments, processorSupport);
                newTypeArguments.add(typeArgument);
            }

            Instance.setValuesForProperty(resultGenericType, M3Properties.typeArguments, newTypeArguments, processorSupport);
        }
        ListIterable<? extends CoreInstance> multiplicityParameters = commonRawType.getValueForMetaPropertyToMany(M3Properties.multiplicityParameters);
        if (multiplicityParameters.notEmpty())
        {
            MutableList<CoreInstance> newMultiplicityArguments = FastList.newList(multiplicityParameters.size());

            MutableList<ListIterable<? extends CoreInstance>> multiplicityArguments = FastList.newList(nonBottomConcreteGenericTypesCount);
            for (CoreInstance genericType : genericTypesAtSameLevel)
            {
                multiplicityArguments.add(genericType.getValueForMetaPropertyToMany(M3Properties.multiplicityArguments));
            }
            for (int i = 0; i < multiplicityParameters.size(); i++)
            {
                MutableList<CoreInstance> multiplicities = FastList.newList(nonBottomConcreteGenericTypesCount);
                for (ListIterable<? extends CoreInstance> args : multiplicityArguments)
                {
                    multiplicities.add(args.get(i));
                }
                newMultiplicityArguments.add(multiplicities.contains(null) ? null : Multiplicity.minSubsumingMultiplicity(multiplicities, processorSupport));
            }

            Instance.setValuesForProperty(resultGenericType, M3Properties.multiplicityArguments, newMultiplicityArguments, processorSupport);
        }

        return resultGenericType;
    }

    static int findFunctionParametersCount(ListIterable<CoreInstance> genericTypeSet, ProcessorSupport processorSupport)
    {
        int size = -1;
        if (genericTypeSet.notEmpty())
        {
            CoreInstance topType = processorSupport.type_TopType();
            CoreInstance bottomType = processorSupport.type_BottomType();
            for (CoreInstance functionTypeGenericType : genericTypeSet)
            {
                // Validate multiplicity
                CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(functionTypeGenericType, M3Properties.rawType, processorSupport);
                if (rawType == topType)
                {
                    return -1;
                }
                if (rawType != bottomType)
                {
                    int newSize = rawType.getValueForMetaPropertyToMany(M3Properties.parameters).size();
                    if (size == -1)
                    {
                        size = newSize;
                    }
                    else if (newSize != size)
                    {
                        return -1;
                    }
                }
            }
        }
        return size;
    }

    /**
     * This set may contain multiple equal instances of generic types when the raw types
     * are function types.  The costs associated with properly hashing for function types
     * outweigh the benefits.
     */
    private static class AlmostGenericTypeMutableSet extends UnifiedSetWithHashingStrategy<CoreInstance>
    {
        private AlmostGenericTypeMutableSet(ProcessorSupport processorSupport, int size)
        {
            super(new AlmostGenericTypeHashingStrategy(processorSupport), size);
        }
    }

    /**
     * This is almost a valid generic type hashing strategy.  The caveat is that this may return
     * different hash codes for equal generic types when they have raw types that are function
     * types.  However, the costs associated with determining if the raw type is a function type
     * outweigh the benefits.
     */
    private static class AlmostGenericTypeHashingStrategy implements HashingStrategy<CoreInstance>
    {
        private final ProcessorSupport processorSupport;

        private AlmostGenericTypeHashingStrategy(ProcessorSupport processorSupport)
        {
            this.processorSupport = processorSupport;
        }

        @Override
        public int computeHashCode(CoreInstance genericType)
        {
            CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, this.processorSupport);
            return (rawType == null) ? GenericType.getTypeParameterName(genericType, this.processorSupport).hashCode() : rawType.hashCode();
        }

        @Override
        public boolean equals(CoreInstance genericType1, CoreInstance genericType2)
        {
            return GenericType.genericTypesEqual(genericType1, genericType2, this.processorSupport);
        }
    }
}
