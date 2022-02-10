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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.linearization.C3Linearization;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.tools.SafeAppendable;

import java.util.Objects;

public class GenericType
{
    public static GenericTypeWithXArguments resolveClassTypeParameterUsingInheritance(CoreInstance genericTypeSource, CoreInstance genericTypeToFind, ProcessorSupport processorSupport)
    {
        MutableList<GenericTypeWithXArguments> results = Lists.mutable.empty();
        Support.resolveTypeArgumentsForGenericTypeToFindUsingInheritanceTree(
                new GenericTypeWithXArguments(genericTypeSource, bindTypeParametersToTypeArguments(genericTypeSource, Maps.immutable.empty(), Maps.immutable.empty(), processorSupport)),
                new GenericTypeWithXArguments(genericTypeSource, bindMultiplicityParametersToMultiplicityArguments(genericTypeSource, Maps.immutable.empty(), processorSupport)),
                genericTypeToFind, results, processorSupport);

        if (results.size() > 1)
        {
            results.toSet().cartesianProduct(results.toSet())
                    .select(pair -> pair.getOne() != pair.getTwo())
                    .forEach(pair -> testFoundGenerics(pair.getOne(), pair.getTwo(), genericTypeSource, genericTypeToFind, processorSupport));
        }
        return results.getFirst();
    }

    private static void testFoundGenerics(GenericTypeWithXArguments first, GenericTypeWithXArguments second, CoreInstance genericTypeSource, CoreInstance genericTypeToFind, ProcessorSupport processorSupport)
    {
        if (!first.getArgumentsByParameterName().keysView().toSet().equals(second.getArgumentsByParameterName().keysView().toSet()))
        {
            throw new RuntimeException("Error: " + first.getArgumentsByParameterName().keysView().toSet() + " / " + second.getArgumentsByParameterName().keysView().toSet());
        }

        first.getArgumentsByParameterName().forEachKey(typeParam ->
        {
            CoreInstance g1 = first.getArgumentsByParameterName().get(typeParam);
            CoreInstance g2 = second.getArgumentsByParameterName().get(typeParam);
            if (!isGenericCompatibleWith(g1, g2, processorSupport))
            {
                StringBuilder message = new StringBuilder("Diamond inheritance error! '");
                print(message, g1, processorSupport);
                print(message.append("' is not compatible with '"), g2, processorSupport);
                print(message.append("' going from '"), genericTypeSource, processorSupport);
                print(message.append("' to '"), genericTypeToFind, processorSupport).append('\'');
                throw new RuntimeException(new PureExecutionException(genericTypeSource.getSourceInformation(), message.toString()));
            }
        });
    }

    public static GenericTypeWithXArguments resolveClassMultiplicityParameterUsingInheritance(CoreInstance genericTypeSource, CoreInstance rawTypeToFind, ProcessorSupport processorSupport)
    {
        MutableList<GenericTypeWithXArguments> results = Lists.mutable.empty();
        Support.resolveMultiplicityArgumentsForGenericTypeToFindUsingInheritanceTree(new GenericTypeWithXArguments(genericTypeSource,
                        bindMultiplicityParametersToMultiplicityArguments(genericTypeSource, Maps.immutable.empty(), processorSupport)),
                rawTypeToFind, results, processorSupport);
//        if (results.size() != 1)
//        {
//            throw new RuntimeException("The genericType was collected '" + results.size() + "' time(s) while walking the inheritance tree up. Starting from '" + print(genericTypeSource, context, processorSupport) + "' looking for '" + print(rawTypeToFind, context, processorSupport) + "'");
//        }
        return results.getFirst();
    }

    public static CoreInstance makeTypeArgumentAsConcreteAsPossible(CoreInstance typeArgument, MapIterable<String, CoreInstance> genericTypeByTypeParameterNames, MapIterable<String, CoreInstance> sourceMulBinding, ProcessorSupport processorSupport)
    {
        if (typeArgument == null)
        {
            return null;
        }

        genericTypeByTypeParameterNames = genericTypeByTypeParameterNames.reject((s, coreInstance) -> typeArgument.equals(coreInstance));

        if (!isGenericTypeConcrete(typeArgument))
        {
            if (!hasFullyConcreteTypeArguments(typeArgument, processorSupport) || !hasConcreteMultiplicityArguments(typeArgument, processorSupport))
            {
                throw new RuntimeException("TO HANDLE! Don't want to process the replaced one ... but may want to process the original!");
            }
            return genericTypeByTypeParameterNames.getIfAbsentValue(getTypeParameterName(typeArgument), typeArgument);
        }

        if (org.finos.legend.pure.m3.navigation.function.FunctionType.isFunctionType(Instance.getValueForMetaPropertyToOneResolved(typeArgument, M3Properties.rawType, processorSupport), processorSupport))
        {
            return Support.reprocessFunctionTypeReplaceTypeParamsByConcreteTypes(typeArgument, genericTypeByTypeParameterNames, sourceMulBinding, processorSupport);
        }

        return Support.reprocessGenericTypeHavingNonConcreteTypeArguments(typeArgument, genericTypeByTypeParameterNames, sourceMulBinding, processorSupport);
    }

    @Deprecated
    public static String getTypeParameterName(CoreInstance genericType, ProcessorSupport processorSupport)
    {
        return getTypeParameterName(genericType);
    }

    public static String getTypeParameterName(CoreInstance genericType)
    {
        CoreInstance typeParameter = genericType.getValueForMetaPropertyToOne(M3Properties.typeParameter);
        return (typeParameter == null) ? null : PrimitiveUtilities.getStringValue(typeParameter.getValueForMetaPropertyToOne(M3Properties.name));
    }

    public static CoreInstance resolvePropertyReturnType(CoreInstance classGenericType, CoreInstance property, ProcessorSupport processorSupport)
    {
        CoreInstance functionType = processorSupport.function_getFunctionType(property);
        CoreInstance returnGenericType = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport);
        if (!isGenericTypeConcrete(returnGenericType))
        {
            GenericTypeWithXArguments res = resolveClassTypeParameterUsingInheritance(classGenericType, Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToManyResolved(functionType, M3Properties.parameters, processorSupport).get(0), M3Properties.genericType, processorSupport), processorSupport);
            returnGenericType = res.getArgumentByParameterName(getTypeParameterName(returnGenericType));
        }
        return reprocessTypeParametersUsingGenericTypeOwnerContext(classGenericType, returnGenericType, processorSupport);
    }

    public static CoreInstance reprocessTypeParametersUsingGenericTypeOwnerContext(CoreInstance ownerGenericType, CoreInstance valToReprocess, ProcessorSupport processorSupport)
    {
        return makeTypeArgumentAsConcreteAsPossible(valToReprocess,
                bindTypeParametersToTypeArguments(ownerGenericType, Maps.immutable.empty(), Maps.immutable.empty(), processorSupport),
                bindMultiplicityParametersToMultiplicityArguments(ownerGenericType, Maps.immutable.empty(), processorSupport),
                processorSupport);
    }


    public static CoreInstance resolveFunctionGenericType(CoreInstance functionGenericType, ProcessorSupport processorSupport)
    {
        if (!Type.isBottomType(Instance.getValueForMetaPropertyToOneResolved(functionGenericType, M3Properties.rawType, processorSupport), processorSupport))
        {
            CoreInstance functionTypeType = Type.wrapGenericType(processorSupport.package_getByUserPath(M3Paths.Function), processorSupport);
            GenericTypeWithXArguments genericTypeWithXArguments = resolveClassTypeParameterUsingInheritance(functionGenericType, functionTypeType, processorSupport);
            if (genericTypeWithXArguments == null)
            {
                throw new PureCompilationException(functionGenericType.getSourceInformation(), print(new StringBuilder("Could not resolve function generic type: "), functionGenericType, processorSupport).toString());
            }
            CoreInstance i = genericTypeWithXArguments.getArgumentsByParameterName().get("T");
            if (isGenericTypeConcrete(i))
            {
                CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(i, M3Properties.rawType, processorSupport);
                if (!Type.isTopType(rawType, processorSupport))
                {
                    return rawType;
                }
            }
        }
        return null;
    }

    public static ImmutableMap<String, CoreInstance> bindTypeParametersToTypeArguments(CoreInstance genericType, MapIterable<String, CoreInstance> sourceBinding, MapIterable<String, CoreInstance> sourceMulBinding, ProcessorSupport processorSupport)
    {
        if (!isGenericTypeConcrete(genericType))
        {
            return Maps.immutable.empty();
        }

        MutableMap<String, CoreInstance> result = Maps.mutable.empty();
        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
        ListIterable<? extends CoreInstance> typeParameters = Instance.getValueForMetaPropertyToManyResolved(rawType, M3Properties.typeParameters, processorSupport);
        ListIterable<? extends CoreInstance> typeArguments = Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.typeArguments, processorSupport);

        int size = typeParameters.size();
        if (typeArguments.size() != size)
        {
            StringBuilder message = _Class.print(new StringBuilder("Type argument mismatch for "), rawType);
            print(message.append("; got: "), genericType, processorSupport);
            throw new RuntimeException(message.toString());
        }
        for (int i = 0; i < size; i++)
        {
            CoreInstance typeParameter = typeParameters.get(i);
            CoreInstance typeArgument = typeArguments.get(i);
            result.put(Instance.getValueForMetaPropertyToOneResolved(typeParameter, M3Properties.name, processorSupport).getName(), makeTypeArgumentAsConcreteAsPossible(typeArgument, sourceBinding, sourceMulBinding, processorSupport));
        }
        return result.toImmutable();
    }

    public static ImmutableMap<String, CoreInstance> bindMultiplicityParametersToMultiplicityArguments(CoreInstance genericType, MapIterable<String, CoreInstance> sourceBinding, ProcessorSupport processorSupport)
    {
        if (!isGenericTypeConcrete(genericType))
        {
            return Maps.immutable.empty();
        }

        MutableMap<String, CoreInstance> result = Maps.mutable.empty();
        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
        ListIterable<? extends CoreInstance> multiplicityParameters = Instance.getValueForMetaPropertyToManyResolved(rawType, M3Properties.multiplicityParameters, processorSupport);
        ListIterable<? extends CoreInstance> multiplicityArguments = Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.multiplicityArguments, processorSupport);

        int size = multiplicityParameters.size();
        if (multiplicityArguments.size() != size)
        {
            StringBuilder message = _Class.print(new StringBuilder("Multiplicity argument mismatch for "), rawType);
            print(message.append("; got: "), genericType, processorSupport);
            throw new RuntimeException(message.toString());
        }

        for (int i = 0; i < size; i++)
        {
            CoreInstance multiplicityParameter = multiplicityParameters.get(i);
            CoreInstance multiplicityArgument = multiplicityArguments.get(i);
            result.put(Instance.getValueForMetaPropertyToOneResolved(multiplicityParameter, M3Properties.values, processorSupport).getName(), Multiplicity.makeMultiplicityAsConcreteAsPossible(multiplicityArgument, sourceBinding));
        }
        return result.toImmutable();
    }

    public static boolean subTypeOf(CoreInstance genericType, CoreInstance possibleGenericSuperType, ProcessorSupport processorSupport)
    {
        // TODO check type arguments
        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
        CoreInstance rawPossibleSuperType = Instance.getValueForMetaPropertyToOneResolved(possibleGenericSuperType, M3Properties.rawType, processorSupport);
        if (rawType == null)
        {
            return (rawPossibleSuperType != null) && Type.isTopType(rawPossibleSuperType, processorSupport);
        }
        if (rawPossibleSuperType == null)
        {
            return Type.isBottomType(rawType, processorSupport);
        }
        return processorSupport.type_subTypeOf(rawType, rawPossibleSuperType);
    }

    public static boolean isGenericCompatibleWith(CoreInstance genericType, CoreInstance otherGenericType, ProcessorSupport processorSupport)
    {
        return isGenericCompatibleWith(genericType, otherGenericType, true, processorSupport);
    }

    public static boolean isGenericCompatibleWith(CoreInstance genericType, CoreInstance otherGenericType, boolean covariant, ProcessorSupport processorSupport)
    {
        if (genericTypesEqual(genericType, otherGenericType, processorSupport))
        {
            return true;
        }

        CoreInstance rawType;
        CoreInstance otherRawType;
        if (covariant)
        {
            rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
            otherRawType = Instance.getValueForMetaPropertyToOneResolved(otherGenericType, M3Properties.rawType, processorSupport);
        }
        else
        {
            rawType = Instance.getValueForMetaPropertyToOneResolved(otherGenericType, M3Properties.rawType, processorSupport);
            otherRawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
        }

        if ((rawType == null) || (otherRawType == null))
        {
//            if (rawType == otherRawType)
//            {
//                return Objects.equals(getTypeParameterName(genericType), getTypeParameterName(otherGenericType));
//            }
//            StringBuilder message = new StringBuilder("Make sure that the typeParameters are resolved before testing compatibility between generic types: ");
//            print(message, genericType, processorSupport);
//            message.append(" vs ");
//            print(message, otherGenericType, processorSupport);
//            throw new RuntimeException(message.toString());
            return true;
        }

        if (Type.isBottomType(rawType, processorSupport) || Type.isTopType(otherRawType, processorSupport))
        {
            return true;
        }

        CoreInstance functionClass = processorSupport.package_getByUserPath(M3Paths.Function);
        boolean isRawTypeAFunction = processorSupport.type_subTypeOf(rawType, functionClass);
        boolean isOtherRawTypeAFunction = processorSupport.type_subTypeOf(otherRawType, functionClass);

        if (isRawTypeAFunction || isOtherRawTypeAFunction)
        {
            if (!(isRawTypeAFunction && isOtherRawTypeAFunction))
            {
                return processorSupport.type_subTypeOf(rawType, otherRawType);
            }

            // Check the type (i.e. Property is not the same as FunctionDefinition)
            if (!processorSupport.type_subTypeOf(rawType, otherRawType))
            {
                return false;
            }

            // Function: we need to check Covariance and ContraVariance
            CoreInstance functionType = resolveFunctionGenericType(genericType, processorSupport);
            CoreInstance otherFunctionType = resolveFunctionGenericType(otherGenericType, processorSupport);

            // Manages the Function<Any> use case
            if (otherFunctionType == null)
            {
                return true;
            }
            if (functionType == null)
            {
                return false;
            }
            // -----------------------------------

            ListIterable<? extends CoreInstance> parameters = Instance.getValueForMetaPropertyToManyResolved(functionType, M3Properties.parameters, processorSupport);
            ListIterable<? extends CoreInstance> otherParameters = Instance.getValueForMetaPropertyToManyResolved(otherFunctionType, M3Properties.parameters, processorSupport);
            if (parameters.size() != otherParameters.size())
            {
                return false;
            }
            boolean contravariant = true;
            for (int i = 0; i < parameters.size(); i++)
            {
                contravariant &= isGenericCompatibleWith(Instance.getValueForMetaPropertyToOneResolved(parameters.get(i), M3Properties.genericType, processorSupport), Instance.getValueForMetaPropertyToOneResolved(otherParameters.get(i), M3Properties.genericType, processorSupport), false, processorSupport);
            }
            return contravariant && isGenericCompatibleWith(Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport), Instance.getValueForMetaPropertyToOneResolved(otherFunctionType, M3Properties.returnType, processorSupport), true, processorSupport);
        }
        else
        {
            return covariant ? Support.isCovariant(genericType, otherGenericType, processorSupport) : Support.isContravariant(genericType, otherGenericType, processorSupport);
        }
    }

    public static ListIterable<CoreInstance> getAllSuperTypesIncludingSelf(CoreInstance genericType, ProcessorSupport processorSupport)
    {
        return C3Linearization.getGenericTypeGeneralizationLinearization(genericType, processorSupport);
    }

    /**
     * Return whether the given generic type is concrete.  That is,
     * it has an actual type rather than a type parameter.  Note,
     * however, that a concrete generic type may have non-concrete
     * type or multiplicity arguments.
     *
     * @param genericType generic type
     * @return whether genericType is concrete
     */
    public static boolean isGenericTypeConcrete(CoreInstance genericType)
    {
        return genericType.getValueForMetaPropertyToOne(M3Properties.rawType) != null;
    }

    @Deprecated
    public static boolean isGenericTypeConcrete(CoreInstance genericType, ProcessorSupport processorSupport)
    {
        return isGenericTypeConcrete(genericType);
    }

    /**
     * Return whether all the generic type's type arguments are
     * concrete.  Note that this is not recursive.  That is,
     * neither the type nor multiplicity arguments of type arguments
     * are checked for concreteness.
     *
     * @param genericType generic type
     * @return whether all the generic type's type arguments are concrete
     */
    public static boolean hasConcreteTypeArguments(CoreInstance genericType)
    {
        return genericType.getValueForMetaPropertyToMany(M3Properties.typeArguments).allSatisfy(GenericType::isGenericTypeConcrete);
    }

    @Deprecated
    public static boolean hasConcreteTypeArguments(CoreInstance genericType, ProcessorSupport processorSupport)
    {
        return hasConcreteTypeArguments(genericType);
    }

    /**
     * Return whether all the generic type's type arguments are
     * fully concrete.  Note that this is recursive.  That is, both
     * the type and multiplicity arguments of the type arguments
     * are checked for concreteness.  (Note, however, that the
     * multiplicity arguments of the generic type are not checked.)
     *
     * @param genericType      generic type
     * @param processorSupport processor support
     * @return whether all the generic type's type arguments are fully concrete
     */
    public static boolean hasFullyConcreteTypeArguments(CoreInstance genericType, ProcessorSupport processorSupport)
    {
        return hasFullyConcreteTypeArguments(genericType, true, processorSupport);
    }

    /**
     * Return whether all the generic type's type arguments are
     * fully concrete.  Note that this is recursive.  That is, both
     * the type and multiplicity arguments of the type arguments
     * are checked for concreteness.  (Note, however, that the
     * multiplicity arguments of the generic type are not checked.)
     *
     * @param genericType        generic type
     * @param checkFunctionTypes whether to check if function types are fully concrete
     * @param processorSupport   processor support
     * @return whether all the generic type's type arguments are fully concrete
     */
    public static boolean hasFullyConcreteTypeArguments(CoreInstance genericType, boolean checkFunctionTypes, ProcessorSupport processorSupport)
    {
        return genericType.getValueForMetaPropertyToMany(M3Properties.typeArguments).allSatisfy(t -> isGenericTypeFullyConcrete(t, checkFunctionTypes, processorSupport));
    }

    /**
     * Return whether all the generic type's multiplicity arguments
     * are concrete.
     *
     * @param genericType generic type
     * @return whether all the generic type's multiplicity arguments are concrete
     */
    public static boolean hasConcreteMultiplicityArguments(CoreInstance genericType, ProcessorSupport processorSupport)
    {
        return Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.multiplicityArguments, processorSupport).allSatisfy(Multiplicity::isMultiplicityConcrete);
    }

    /**
     * Return whether the given generic type is fully concrete.  That is,
     * it has an actual type rather than a type parameter, all of its
     * type arguments are fully concrete, and all of its multiplicity
     * arguments are concrete.
     *
     * @param genericType      generic type
     * @param processorSupport processor support
     * @return whether genericType is fully concrete
     */
    public static boolean isGenericTypeFullyConcrete(CoreInstance genericType, ProcessorSupport processorSupport)
    {
        return isGenericTypeFullyConcrete(genericType, true, processorSupport);
    }

    /**
     * Return whether the given generic type is fully concrete.  That is,
     * it has an actual type rather than a type parameter, all of its
     * type arguments are fully concrete, and all of its multiplicity
     * arguments are concrete.  If checkFunctionTypes is true, then any
     * function type must also be fully concrete; otherwise, function
     * types are not checked.
     *
     * @param genericType        generic type
     * @param checkFunctionTypes whether to check if function types are fully concrete
     * @param processorSupport   processor support
     * @return whether genericType is fully concrete
     */
    public static boolean isGenericTypeFullyConcrete(CoreInstance genericType, boolean checkFunctionTypes, ProcessorSupport processorSupport)
    {
        CoreInstance rawType = genericType.getValueForMetaPropertyToOne(M3Properties.rawType);
        if (rawType == null)
        {
            return false;
        }
        if (checkFunctionTypes && org.finos.legend.pure.m3.navigation.function.FunctionType.isFunctionType(rawType, processorSupport) && !org.finos.legend.pure.m3.navigation.function.FunctionType.isFunctionTypeFullyConcrete(rawType, processorSupport))
        {
            return false;
        }

        return hasConcreteMultiplicityArguments(genericType, processorSupport) && hasFullyConcreteTypeArguments(genericType, checkFunctionTypes, processorSupport);
    }

    /**
     * Return whether the given generic type is fully defined.  That is,
     * all of its constituent parts have been filled in (whether concretely
     * or not).
     *
     * @param genericType      generic type
     * @param processorSupport processor support
     * @return whether genericType is fully defined
     */
    public static boolean isGenericTypeFullyDefined(CoreInstance genericType, ProcessorSupport processorSupport)
    {
        if (genericType == null)
        {
            return false;
        }

        // Check raw type and type parameter
        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
        if (rawType == null)
        {
            CoreInstance typeParameter = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.typeParameter, processorSupport);
            if (typeParameter == null)
            {
                return false;
            }
        }
        else if (org.finos.legend.pure.m3.navigation.function.FunctionType.isFunctionType(rawType, processorSupport) && !org.finos.legend.pure.m3.navigation.function.FunctionType.isFunctionTypeFullyDefined(rawType, processorSupport))
        {
            return false;
        }

        // Check type and multiplicity arguments are fully defined
        return Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.typeArguments, processorSupport).allSatisfy(t -> isGenericTypeFullyDefined(t, processorSupport)) &&
                Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.multiplicityArguments, processorSupport).allSatisfy(Objects::nonNull);
    }

    /**
     * Return whether two generic types are equal.
     *
     * @param genericType1 first generic type
     * @param genericType2 second generic type
     * @return whether genericType1 and genericType2 are equal
     */
    public static boolean genericTypesEqual(CoreInstance genericType1, CoreInstance genericType2, ProcessorSupport processorSupport)
    {
        // Check identity
        if (genericType1 == genericType2)
        {
            return true;
        }

        // Check raw type
        CoreInstance rawType1 = Instance.getValueForMetaPropertyToOneResolved(genericType1, M3Properties.rawType, processorSupport);
        CoreInstance rawType2 = Instance.getValueForMetaPropertyToOneResolved(genericType2, M3Properties.rawType, processorSupport);
        if (rawType1 == null)
        {
            return (rawType2 == null) && Objects.equals(getTypeParameterName(genericType1), getTypeParameterName(genericType2));
        }
        if (rawType2 == null)
        {
            return false;
        }

        if (!rawType1.equals(rawType2))
        {
            CoreInstance functionTypeClass = processorSupport.package_getByUserPath(M3Paths.FunctionType);
            if (!Instance.instanceOf(rawType1, functionTypeClass, processorSupport) ||
                    !Instance.instanceOf(rawType2, functionTypeClass, processorSupport) ||
                    !org.finos.legend.pure.m3.navigation.function.FunctionType.functionTypesEqual(rawType1, rawType2, processorSupport))
            {
                return false;
            }
        }

        // Check multiplicity arguments
        ListIterable<? extends CoreInstance> multiplicityArguments1 = Instance.getValueForMetaPropertyToManyResolved(genericType1, M3Properties.multiplicityArguments, processorSupport);
        ListIterable<? extends CoreInstance> multiplicityArguments2 = Instance.getValueForMetaPropertyToManyResolved(genericType2, M3Properties.multiplicityArguments, processorSupport);
        int multiplicityArgumentsSize = multiplicityArguments1.size();
        if (multiplicityArgumentsSize != multiplicityArguments2.size())
        {
            return false;
        }

        for (int i = 0; i < multiplicityArgumentsSize; i++)
        {
            if (!Multiplicity.multiplicitiesEqual(multiplicityArguments1.get(i), multiplicityArguments2.get(i), true))
            {
                return false;
            }
        }

        // Check type arguments
        ListIterable<? extends CoreInstance> typeArguments1 = genericType1.getValueForMetaPropertyToMany(M3Properties.typeArguments);
        ListIterable<? extends CoreInstance> typeArguments2 = genericType2.getValueForMetaPropertyToMany(M3Properties.typeArguments);
        int typeArgumentsSize = typeArguments1.size();
        if (typeArgumentsSize != typeArguments2.size())
        {
            return false;
        }

        for (int i = 0; i < typeArgumentsSize; i++)
        {
            if (!genericTypesEqual(typeArguments1.get(i), typeArguments2.get(i), processorSupport))
            {
                return false;
            }
        }

        return true;
    }

    public static void resolveImportStubs(CoreInstance genericType, ProcessorSupport processorSupport)
    {
        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
        if ((rawType != null) && org.finos.legend.pure.m3.navigation.function.FunctionType.isFunctionType(rawType, processorSupport))
        {
            org.finos.legend.pure.m3.navigation.function.FunctionType.resolveImportStubs(rawType, processorSupport);
        }

        genericType.getValueForMetaPropertyToMany(M3Properties.multiplicityArguments).asLazy()
                .select(Objects::nonNull)
                .forEach(arg -> org.finos.legend.pure.m3.navigation.importstub.ImportStub.withImportStubByPass(arg, processorSupport));
        genericType.getValueForMetaPropertyToMany(M3Properties.typeArguments).asLazy()
                .select(Objects::nonNull)
                .forEach(arg -> resolveImportStubs(arg, processorSupport));
    }

    /**
     * Print a human readable representation of a generic type to a
     * string.
     *
     * @param genericType      generic type to print
     * @param processorSupport processor support
     * @return human readable representation of genericType
     */
    public static String print(CoreInstance genericType, ProcessorSupport processorSupport)
    {
        return print(genericType, false, processorSupport);
    }

    /**
     * Print a human readable representation of a generic type to a
     * string.
     *
     * @param genericType      generic type to print
     * @param fullPaths        whether to print full paths
     * @param processorSupport processor support
     * @return human readable representation of genericType
     */
    public static String print(CoreInstance genericType, boolean fullPaths, ProcessorSupport processorSupport)
    {
        return print(genericType, fullPaths, false, processorSupport);
    }

    /**
     * Print a human readable representation of a generic type to a
     * string.
     *
     * @param genericType      generic type to print
     * @param fullPaths        whether to print full paths
     * @param markImportStubs  whether to mark import stubs with ~
     * @param processorSupport processor support
     * @return human readable representation of genericType
     */
    public static String print(CoreInstance genericType, boolean fullPaths, boolean markImportStubs, ProcessorSupport processorSupport)
    {
        return print(new StringBuilder(), genericType, fullPaths, markImportStubs, processorSupport).toString();
    }

    /**
     * Print a human readable representation of a generic type to the
     * given appendable.
     *
     * @param appendable       appendable to print to
     * @param genericType      generic type to print
     * @param processorSupport processor support
     * @return the appendable
     */
    public static <T extends Appendable> T print(T appendable, CoreInstance genericType, ProcessorSupport processorSupport)
    {
        return print(appendable, genericType, false, processorSupport);
    }

    /**
     * Print a human readable representation of a generic type to the
     * given appendable.
     *
     * @param appendable       appendable to print to
     * @param genericType      generic type to print
     * @param fullPaths        whether to print full paths
     * @param processorSupport processor support
     * @return the appendable
     */
    public static <T extends Appendable> T print(T appendable, CoreInstance genericType, boolean fullPaths, ProcessorSupport processorSupport)
    {
        return print(appendable, genericType, fullPaths, false, processorSupport);
    }

    /**
     * Print a human readable representation of a generic type to the
     * given appendable.
     *
     * @param appendable       appendable to print to
     * @param genericType      generic type to print
     * @param fullPaths        whether to print full paths
     * @param markImportStubs  whether to mark import stubs with ~
     * @param processorSupport processor support
     * @return the appendable
     */
    public static <T extends Appendable> T print(T appendable, CoreInstance genericType, boolean fullPaths, boolean markImportStubs, ProcessorSupport processorSupport)
    {
        print(SafeAppendable.wrap(appendable), genericType, fullPaths, markImportStubs, processorSupport);
        return appendable;
    }

    public static <T extends Appendable> T print(T appendable, CoreInstance rawType, ListIterable<? extends CoreInstance> typeArguments, ListIterable<? extends CoreInstance> multiplicityArguments, ProcessorSupport processorSupport)
    {
        return print(appendable, rawType, typeArguments, multiplicityArguments, false, false, processorSupport);
    }

    public static <T extends Appendable> T print(T appendable, CoreInstance rawType, ListIterable<? extends CoreInstance> typeArguments, ListIterable<? extends CoreInstance> multiplicityArguments, boolean fullPaths, boolean markImportStubs, ProcessorSupport processorSupport)
    {
        print(SafeAppendable.wrap(appendable), rawType, typeArguments, multiplicityArguments, fullPaths, markImportStubs, processorSupport);
        return appendable;
    }

    private static void print(SafeAppendable appendable, CoreInstance genericType, boolean fullPaths, boolean markImportStubs, ProcessorSupport processorSupport)
    {
        if (genericType == null)
        {
            appendable.append("NULL");
        }
        else
        {
            CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
            ListIterable<? extends CoreInstance> typeArguments = genericType.getValueForMetaPropertyToMany(M3Properties.typeArguments);
            ListIterable<? extends CoreInstance> multiplicityArguments = Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.multiplicityArguments, processorSupport);
            if (rawType == null)
            {
                String typeParameterName = getTypeParameterName(genericType);
                appendable.append((typeParameterName == null) ? "NULL" : typeParameterName);
                printTypeAndMultiplicityArguments(appendable, typeArguments, multiplicityArguments, fullPaths, markImportStubs, processorSupport);
            }
            else
            {
                if (markImportStubs && processorSupport.instance_instanceOf(genericType.getValueForMetaPropertyToOne(M3Properties.rawType), M3Paths.ImportStub))
                {
                    appendable.append('~');
                }
                print(appendable, rawType, typeArguments, multiplicityArguments, fullPaths, markImportStubs, processorSupport);
            }
        }
    }

    private static void print(SafeAppendable appendable, CoreInstance rawType, ListIterable<? extends CoreInstance> typeArguments, ListIterable<? extends CoreInstance> multiplicityArguments, boolean fullPaths, boolean markImportStubs, ProcessorSupport processorSupport)
    {
        if (org.finos.legend.pure.m3.navigation.function.FunctionType.isFunctionType(rawType, processorSupport))
        {
            org.finos.legend.pure.m3.navigation.function.FunctionType.print(appendable, rawType, fullPaths, markImportStubs, processorSupport);
        }
        else if (fullPaths)
        {
            PackageableElement.writeUserPathForPackageableElement(appendable, rawType);
        }
        else if (ModelRepository.isAnonymousInstanceName(rawType.getName()))
        {
            appendable.append(rawType.toString());
        }
        else
        {
            appendable.append(rawType.getName());
        }

        printTypeAndMultiplicityArguments(appendable, typeArguments, multiplicityArguments, fullPaths, markImportStubs, processorSupport);
    }

    private static void printTypeAndMultiplicityArguments(SafeAppendable appendable, ListIterable<? extends CoreInstance> typeArguments, ListIterable<? extends CoreInstance> multiplicityArguments, boolean fullPaths, boolean markImportStubs, ProcessorSupport processorSupport)
    {
        int typeArgumentsSize = typeArguments.size();
        int multiplicityArgumentsSize = multiplicityArguments.size();

        if ((typeArgumentsSize > 0) || (multiplicityArgumentsSize > 0))
        {
            appendable.append('<');
            if (typeArgumentsSize > 0)
            {
                print(appendable, typeArguments.get(0), fullPaths, markImportStubs, processorSupport);
                for (int i = 1; i < typeArgumentsSize; i++)
                {
                    print(appendable.append(", "), typeArguments.get(i), fullPaths, markImportStubs, processorSupport);
                }
            }

            if (multiplicityArgumentsSize > 0)
            {
                appendable.append('|');
                Multiplicity.print(appendable, multiplicityArguments.get(0), false);
                for (int i = 1; i < multiplicityArgumentsSize; i++)
                {
                    Multiplicity.print(appendable.append(", "), multiplicityArguments.get(i), false);
                }
            }
            appendable.append('>');
        }
    }

    public static void resolveGenericTypeUsingImports(CoreInstance genericType, ModelRepository repository, ProcessorSupport processorSupport) throws PureCompilationException
    {
        if (genericType.getValueForMetaPropertyToOne(M3Properties.rawType) != null && genericType.getValueForMetaPropertyToOne(M3Properties.rawType) instanceof ImportStub)
        {
            org.finos.legend.pure.m3.navigation.importstub.ImportStub.processImportStub((ImportStub) genericType.getValueForMetaPropertyToOne(M3Properties.rawType), repository, processorSupport);
        }
        if (genericType.getValueForMetaPropertyToOne(M3Properties.rawType) != null && genericType.getValueForMetaPropertyToOne(M3Properties.rawType) instanceof FunctionType)
        {
            Instance.getValueForMetaPropertyToManyResolved(genericType.getValueForMetaPropertyToOne(M3Properties.rawType), M3Properties.parameters, processorSupport).asLazy()
                    .collect(p -> p.getValueForMetaPropertyToOne(M3Properties.genericType))
                    .select(Objects::nonNull)
                    .forEach(gt -> resolveGenericTypeUsingImports(gt, repository, processorSupport));
            if (genericType.getValueForMetaPropertyToOne(M3Properties.rawType).getValueForMetaPropertyToOne(M3Properties.returnType) != null)
            {
                resolveGenericTypeUsingImports(genericType.getValueForMetaPropertyToOne(M3Properties.rawType).getValueForMetaPropertyToOne(M3Properties.returnType), repository, processorSupport);
            }
        }
        Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.typeArguments, processorSupport).forEach(arg -> resolveGenericTypeUsingImports(arg, repository, processorSupport));
    }

    public static boolean isUnprocessedLambda(CoreInstance genericType, ProcessorSupport processorSupport)
    {
        return "LambdaFunction".equals(Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport).getName()) &&
                Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.typeArguments, M3Properties.rawType, M3Properties.returnType, processorSupport) == null;
    }

    public static CoreInstance copyGenericType(CoreInstance genericType, ProcessorSupport processorSupport)
    {
        return copyGenericType(genericType, false, processorSupport);
    }

    public static CoreInstance copyGenericType(CoreInstance genericType, SourceInformation newSourceInformation, ProcessorSupport processorSupport)
    {
        return copyGenericType(genericType, true, newSourceInformation, processorSupport, false);
    }

    public static CoreInstance copyGenericType(CoreInstance genericType, boolean copySourceInfo, ProcessorSupport processorSupport)
    {
        return copyGenericType(genericType, !copySourceInfo, null, processorSupport, false);
    }

    static CoreInstance copyGenericType(CoreInstance genericType, boolean replaceSourceInfo, SourceInformation newSourceInfo, ProcessorSupport processorSupport)
    {
        return copyGenericType(genericType, replaceSourceInfo, newSourceInfo, processorSupport, false);
    }

    public static CoreInstance copyGenericTypeAsInferredGenericType(CoreInstance genericType, ProcessorSupport processorSupport)
    {
        return copyGenericType(genericType, false, null, processorSupport, true);
    }

    public static CoreInstance copyGenericTypeAsInferredGenericType(CoreInstance genericType, SourceInformation newSourceInformation, ProcessorSupport processorSupport)
    {
        return copyGenericType(genericType, true, newSourceInformation, processorSupport, true);
    }

    private static CoreInstance copyGenericType(CoreInstance genericType, boolean replaceSourceInfo, SourceInformation newSourceInfo, ProcessorSupport processorSupport, boolean inferred)
    {
        return isGenericTypeConcrete(genericType) ?
                copyConcreteGenericType(genericType, replaceSourceInfo, newSourceInfo, processorSupport, inferred) :
                copyNonConcreteGenericType(genericType, replaceSourceInfo, newSourceInfo, processorSupport, inferred);
    }

    private static CoreInstance copyConcreteGenericType(CoreInstance genericType, boolean replaceSourceInfo, SourceInformation newSourceInfo, ProcessorSupport processorSupport, boolean inferred)
    {
        CoreInstance rawTypeCopy = copy(genericType.getValueForMetaPropertyToOne(M3Properties.rawType), replaceSourceInfo, newSourceInfo, processorSupport, inferred);
        CoreInstance copy = Type.wrapGenericType(rawTypeCopy, replaceSourceInfo ? newSourceInfo : genericType.getSourceInformation(), processorSupport, inferred);

        ListIterable<? extends CoreInstance> typeArguments = genericType.getValueForMetaPropertyToMany(M3Properties.typeArguments);
        if (typeArguments.notEmpty())
        {
            MutableList<CoreInstance> typeArgumentCopies = typeArguments.collect(ta -> copyGenericType(ta, replaceSourceInfo, newSourceInfo, processorSupport, inferred), Lists.mutable.ofInitialCapacity(typeArguments.size()));
            Instance.setValuesForProperty(copy, M3Properties.typeArguments, typeArgumentCopies, processorSupport);
        }
        ListIterable<? extends CoreInstance> multArguments = genericType.getValueForMetaPropertyToMany(M3Properties.multiplicityArguments);
        if (multArguments.notEmpty())
        {
            MutableList<CoreInstance> multArgumentCopies = multArguments.collect(ma -> Multiplicity.copyMultiplicity(ma, replaceSourceInfo, newSourceInfo, processorSupport), Lists.mutable.ofInitialCapacity(multArguments.size()));
            Instance.setValuesForProperty(copy, M3Properties.multiplicityArguments, multArgumentCopies, processorSupport);
        }
        return copy;
    }

    private static CoreInstance copyNonConcreteGenericType(CoreInstance genericType, boolean replaceSourceInfo, SourceInformation newSourceInfo, ProcessorSupport processorSupport, boolean inferred)
    {
        CoreInstance typeParameter = genericType.getValueForMetaPropertyToOne(M3Properties.typeParameter);
        CoreInstance typeParameterCopy = copyTypeParameter(typeParameter, replaceSourceInfo, newSourceInfo, processorSupport, inferred);

        CoreInstance copy = processorSupport.newGenericType(replaceSourceInfo ? newSourceInfo : genericType.getSourceInformation(), genericType, inferred);
        Instance.setValueForProperty(copy, M3Properties.typeParameter, typeParameterCopy, processorSupport);
        return copy;
    }

    private static CoreInstance copyTypeParameter(CoreInstance typeParameter, boolean replaceSourceInfo, SourceInformation newSourceInfo, ProcessorSupport processorSupport, boolean inferred)
    {
        CoreInstance copy = processorSupport.newAnonymousCoreInstance(replaceSourceInfo ? newSourceInfo : typeParameter.getSourceInformation(), M3Paths.TypeParameter);

        CoreInstance name = typeParameter.getValueForMetaPropertyToOne(M3Properties.name);
        Instance.setValueForProperty(copy, M3Properties.name, name, processorSupport);

        CoreInstance contravariant = Instance.getValueForMetaPropertyToOneResolved(typeParameter, M3Properties.contravariant, processorSupport);
        if (contravariant != null)
        {
            Instance.setValueForProperty(copy, M3Properties.contravariant, contravariant, processorSupport);
        }

        CoreInstance lowerBound = Instance.getValueForMetaPropertyToOneResolved(typeParameter, M3Properties.lowerBound, processorSupport);
        if (lowerBound != null)
        {
            Instance.setValueForProperty(copy, M3Properties.lowerBound, copyGenericType(lowerBound, replaceSourceInfo, newSourceInfo, processorSupport, inferred), processorSupport);
        }

        CoreInstance upperBound = Instance.getValueForMetaPropertyToOneResolved(typeParameter, M3Properties.lowerBound, processorSupport);
        if (lowerBound != null)
        {
            Instance.setValueForProperty(copy, M3Properties.upperBound, copyGenericType(upperBound, replaceSourceInfo, newSourceInfo, processorSupport, inferred), processorSupport);
        }

        return copy;
    }

    private static CoreInstance copy(CoreInstance type, boolean replaceSourceInfo, SourceInformation newSourceInfo, ProcessorSupport processorSupport, boolean inferred)
    {
        if (processorSupport.instance_instanceOf(type, M3Paths.FunctionType))
        {
            CoreInstance functionType = processorSupport.newAnonymousCoreInstance(replaceSourceInfo ? newSourceInfo : type.getSourceInformation(), M3Paths.FunctionType);
            CoreInstance returnType = type.getValueForMetaPropertyToOne(M3Properties.returnType);
            if (returnType != null)
            {
                Instance.addValueToProperty(functionType, M3Properties.returnType, copyGenericType(returnType, replaceSourceInfo, newSourceInfo, processorSupport, inferred), processorSupport);
                Instance.addValueToProperty(functionType, M3Properties.returnMultiplicity, Multiplicity.copyMultiplicity(type.getValueForMetaPropertyToOne(M3Properties.returnMultiplicity), replaceSourceInfo, newSourceInfo, processorSupport), processorSupport);
            }
            ListIterable<? extends CoreInstance> typeParameters = type.getValueForMetaPropertyToMany(M3Properties.typeParameters);
            if (typeParameters.notEmpty())
            {
                Instance.setValuesForProperty(functionType, M3Properties.typeParameters, typeParameters, processorSupport);
            }
            ListIterable<? extends CoreInstance> parameters = type.getValueForMetaPropertyToMany(M3Properties.parameters);
            if (parameters.notEmpty())
            {
                MutableList<CoreInstance> processedParameters = parameters.collect(parameter ->
                {
                    CoreInstance varExpression = processorSupport.newAnonymousCoreInstance(replaceSourceInfo ? newSourceInfo : parameter.getSourceInformation(), M3Paths.VariableExpression);
                    CoreInstance varName = parameter.getValueForMetaPropertyToOne(M3Properties.name);
                    if (varName != null)
                    {
                        Instance.addValueToProperty(varExpression, M3Properties.name, varName, processorSupport);
                    }
                    CoreInstance varGenericType = parameter.getValueForMetaPropertyToOne(M3Properties.genericType);
                    if (varGenericType != null)
                    {
                        Instance.addValueToProperty(varExpression, M3Properties.genericType, copyGenericType(varGenericType, replaceSourceInfo, newSourceInfo, processorSupport, inferred), processorSupport);
                        Instance.addValueToProperty(varExpression, M3Properties.multiplicity, Multiplicity.copyMultiplicity(parameter.getValueForMetaPropertyToOne(M3Properties.multiplicity), replaceSourceInfo, newSourceInfo, processorSupport), processorSupport);
                    }
                    return varExpression;
                }, Lists.mutable.ofInitialCapacity(parameters.size()));
                Instance.setValuesForProperty(functionType, M3Properties.parameters, processedParameters, processorSupport);
            }
            return functionType;
        }
        if (processorSupport.instance_instanceOf(type, M3Paths.ImportStub))
        {
/*            if (inferred)
            {
                //Inferred types are cleaned during unbinding, so don't need to keep the importstub
                return type.getValueForMetaPropertyToOne(M3Properties.resolvedNode);
            }
            else
            {*/
            CoreInstance importStub = processorSupport.newAnonymousCoreInstance(replaceSourceInfo ? newSourceInfo : type.getSourceInformation(), M3Paths.ImportStub);
            Instance.addValueToProperty(importStub, M3Properties.idOrPath, type.getValueForMetaPropertyToOne(M3Properties.idOrPath), processorSupport);
            Instance.addValueToProperty(importStub, M3Properties.importGroup, type.getValueForMetaPropertyToOne(M3Properties.importGroup), processorSupport);
            Instance.addValueToProperty(importStub, M3Properties.resolvedNode, type.getValueForMetaPropertyToOne(M3Properties.resolvedNode), processorSupport);
            return importStub;
//            }
        }
        return type;
    }

    public static CoreInstance findBestCommonGenericType(ListIterable<CoreInstance> genericTypeSet, boolean covariant, boolean isFunction, ProcessorSupport processorSupport)
    {
        return findBestCommonGenericType(genericTypeSet, null, covariant, isFunction, false, null, processorSupport);
    }

    public static CoreInstance findBestCommonGenericType(ListIterable<CoreInstance> genericTypeSet, boolean covariant, boolean isFunction, SourceInformation sourceInformation, ProcessorSupport processorSupport)
    {
        return findBestCommonGenericType(genericTypeSet, null, covariant, isFunction, sourceInformation != null, sourceInformation, processorSupport);
    }

    static CoreInstance findBestCommonGenericType(ListIterable<CoreInstance> genericTypeSet, boolean covariant, boolean isFunction, boolean replaceSourceInfo, SourceInformation newSourceInfo, ProcessorSupport processorSupport)
    {
        return findBestCommonGenericType(genericTypeSet, null, covariant, isFunction, replaceSourceInfo, newSourceInfo, processorSupport);
    }

    private static CoreInstance findBestCommonGenericType(ListIterable<CoreInstance> genericTypeSet, CoreInstance knownMostGeneralGenericTypeBound, boolean covariant, boolean isFunction, boolean replaceSourceInfo, SourceInformation newSourceInfo, ProcessorSupport processorSupport)
    {
        if (genericTypeSet.contains(null))
        {
            return null;
        }

        if (isFunction)
        {
            // Get Param count
            int size = Support.findFunctionParametersCount(genericTypeSet, processorSupport);

            if (size == -1)
            {
                // The functions all have a different number of parameters ... we can't find a common function type
                return Type.wrapGenericType(covariant ? processorSupport.package_getByUserPath(M3Paths.Any) : processorSupport.package_getByUserPath(M3Paths.Nil), processorSupport);
            }

            CoreInstance functionType = processorSupport.newAnonymousCoreInstance(replaceSourceInfo ? newSourceInfo : null, M3Paths.FunctionType);
            for (int i = 0; i < size; i++)
            {
                MutableList<CoreInstance> parameterTypes = Lists.mutable.ofInitialCapacity(genericTypeSet.size());
                MutableList<CoreInstance> parameterMultiplicities = Lists.mutable.ofInitialCapacity(genericTypeSet.size());
                for (CoreInstance genericType : genericTypeSet)
                {
                    CoreInstance param = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport).getValueForMetaPropertyToMany(M3Properties.parameters).get(i);
                    parameterTypes.add(param.getValueForMetaPropertyToOne(M3Properties.genericType));
                    parameterMultiplicities.add(Instance.getValueForMetaPropertyToOneResolved(param, M3Properties.multiplicity, processorSupport));
                }
                CoreInstance inst = processorSupport.newAnonymousCoreInstance(null, M3Paths.VariableExpression);
                Instance.addValueToProperty(inst, M3Properties.name, processorSupport.newCoreInstance("", M3Paths.String, null), processorSupport);
                Instance.addValueToProperty(inst, M3Properties.multiplicity, parameterMultiplicities.contains(null) ? null : Multiplicity.minSubsumingMultiplicity(parameterMultiplicities, processorSupport), processorSupport);
                Instance.addValueToProperty(inst, M3Properties.genericType, findBestCommonGenericType(parameterTypes, null, false, false, replaceSourceInfo, newSourceInfo, processorSupport), processorSupport);
                Instance.addValueToProperty(functionType, M3Properties.parameters, inst, processorSupport);
            }

            MutableList<CoreInstance> returnTypes = Lists.mutable.ofInitialCapacity(genericTypeSet.size());
            MutableList<CoreInstance> returnMultiplicities = Lists.mutable.ofInitialCapacity(genericTypeSet.size());
            genericTypeSet.forEach(genericType ->
            {
                CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
                returnTypes.add(Instance.getValueForMetaPropertyToOneResolved(rawType, M3Properties.returnType, processorSupport));
                returnMultiplicities.add(Instance.getValueForMetaPropertyToOneResolved(rawType, M3Properties.returnMultiplicity, processorSupport));
            });
            Instance.addValueToProperty(functionType, M3Properties.returnType, findBestCommonGenericType(returnTypes, true, false, replaceSourceInfo, newSourceInfo, processorSupport), processorSupport);
            Instance.addValueToProperty(functionType, M3Properties.returnMultiplicity, returnMultiplicities.contains(null) ? null : Multiplicity.minSubsumingMultiplicity(returnMultiplicities, processorSupport), processorSupport);

            return Type.wrapGenericType(functionType, processorSupport);
        }
        else
        {
            return covariant ?
                    Support.getBestGenericTypeUsingCovariance(genericTypeSet, knownMostGeneralGenericTypeBound, replaceSourceInfo, newSourceInfo, processorSupport) :
                    Support.getBestGenericTypeUsingContravariance(genericTypeSet, replaceSourceInfo, newSourceInfo, processorSupport);
        }
    }

    public static CoreInstance findBestCommonCovariantNonFunctionTypeGenericType(ListIterable<CoreInstance> genericTypeSet, CoreInstance knownMostGeneralGenericTypeBound, SourceInformation sourceInformation, ProcessorSupport processorSupport)
    {
        switch (genericTypeSet.size())
        {
            case 0:
            {
                return Type.wrapGenericType(processorSupport.type_BottomType(), sourceInformation, processorSupport);
            }
            case 1:
            {
                return copyGenericType(genericTypeSet.get(0), sourceInformation, processorSupport);
            }
            default:
            {
                return findBestCommonGenericType(genericTypeSet, knownMostGeneralGenericTypeBound, true, false, sourceInformation != null, sourceInformation, processorSupport);
            }
        }
    }
}
