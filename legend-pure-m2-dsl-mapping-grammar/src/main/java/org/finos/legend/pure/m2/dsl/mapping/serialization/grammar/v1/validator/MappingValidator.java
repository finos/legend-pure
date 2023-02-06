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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.validator;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelToModel.PureInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.compiler.validation.Validator;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.compiler.validation.VisibilityValidation;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.EnumStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;


public class MappingValidator implements MatchRunner<Mapping>
{
    private static final Predicate<SetImplementation> IS_ROOT = new Predicate<SetImplementation>()
    {
        @Override
        public boolean accept(SetImplementation instance)
        {
            return instance._root();
        }
    };

    @Override
    public String getClassName()
    {
        return M2MappingPaths.Mapping;
    }

    @Override
    public void run(Mapping mapping, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ValidatorState validatorState = (ValidatorState) state;
        ProcessorSupport processorSupport = validatorState.getProcessorSupport();

        validateIncludes(mapping, matcher, validatorState, processorSupport);

        validatorState.resetSetImplementationList();
        MutableListMultimap<Type, CoreInstance> setImplementationsByClass = Multimaps.mutable.list.empty();

        validateMappedEntitiesInIncludes(mapping, mapping, processorSupport, setImplementationsByClass, validatorState, matcher);
        validateMappedEntities(mapping, mapping._classMappings(), true, setImplementationsByClass, processorSupport, validatorState, matcher);
        validateMappedEntities(mapping, mapping._associationMappings(), true, setImplementationsByClass, processorSupport, validatorState, matcher);
        validateMappedEntities(mapping, mapping._enumerationMappings(), false, setImplementationsByClass, processorSupport, validatorState, matcher);
        validateStar(mapping, processorSupport);
        validateAssociationMappings(mapping, processorSupport);
        validateEnumerationMappings(mapping, processorSupport);
        validateSuperSetImplementationId(mapping, processorSupport);
        validateVisibility(mapping, context, validatorState, processorSupport);
        validateOperationMappings(mapping, setImplementationsByClass, processorSupport);
        StoreSubstitutionValidator.validateStoreSubstitutions(mapping);
    }

    private static void validateSuperSetImplementationId(Mapping mapping, final ProcessorSupport processorSupport)
    {
        ListIterable<? extends SetImplementation> mappedInstances = mapping._classMappings().toList();

        for (SetImplementation classMapping : mappedInstances)
        {
            String superSetId = classMapping._superSetImplementationId();
            if (superSetId != null)
            {
                if (superSetId.equals(classMapping._id()))
                {
                    throw new PureCompilationException(classMapping.getSourceInformation(), "Extend mapping id cannot reference self \'" + classMapping._id() + "\'");
                }
                MapIterable<String, ? extends SetImplementation> classMappingIndex = org.finos.legend.pure.m2.dsl.mapping.Mapping.getClassMappingsByIdIncludeEmbedded(mapping, processorSupport);
                SetImplementation superSetInstanceMapping = classMappingIndex.get(superSetId);
                if (superSetInstanceMapping == null)
                {
                    throw new PureCompilationException(classMapping.getSourceInformation(), "Extend mapping id not found \'" + superSetId + "\'");
                }
                Class targetClass = (Class) ImportStub.withImportStubByPass(classMapping._classCoreInstance(), processorSupport);
                Class extendsTargetClass = (Class) ImportStub.withImportStubByPass(superSetInstanceMapping._classCoreInstance(), processorSupport);

                if (!targetClass.equals(extendsTargetClass))
                {
                    //We allow extends mappings for subtypes that only have associations and no simple properties of their own
                    ListIterable<CoreInstance> generalizations = org.finos.legend.pure.m3.navigation.type.Type.getDirectGeneralizations(targetClass, processorSupport);
                    if (generalizations.contains(extendsTargetClass))
                    {
                        if (generalizations.size() != 1 || !generalizations.getFirst().equals(extendsTargetClass))
                        {
                            throw new PureCompilationException(classMapping.getSourceInformation(), "Invalid extends mapping. Class [" + targetClass._name() + "] extends more than one class. Extends mappings are only currently only allowed with single inheritance relationships");
                        }
                    } else
                    {
                        throw new PureCompilationException(classMapping.getSourceInformation(), "Class [" + targetClass._name() + "] != [" + extendsTargetClass._name() + "], when [" + classMapping._id() + "] extends [ \'" + superSetId + "\'] they must map the same class");
                    }
                }
                if (!classMapping.getClassifier().equals(superSetInstanceMapping.getClassifier()))
                {
                    throw new PureCompilationException(classMapping.getSourceInformation(), "When extending mappings must be of same type [" + classMapping.getClassifier().getName() + "]!= [" + superSetInstanceMapping.getClassifier().getName() + "]");
                }
            }
        }
    }

    private static void validateAssociationMappings(Mapping mapping, final ProcessorSupport processorSupport)
    {

        RichIterable<? extends AssociationImplementation> associationMappings = mapping._associationMappings();

        if (!associationMappings.isEmpty())
        {
            MapIterable<String, ? extends SetImplementation> classMappingIndex = org.finos.legend.pure.m2.dsl.mapping.Mapping.getClassMappingsByIdIncludeEmbedded(mapping, processorSupport);
            //Add association mappings to Class mappings
            for (AssociationImplementation associationMapping : associationMappings)
            {
                for (PropertyMapping propertyMapping : associationMapping._propertyMappings())
                {
                    Property property = (Property) ImportStub.withImportStubByPass(propertyMapping._propertyCoreInstance(), processorSupport);
                    final String propertyName = property.getName();

                    SetImplementation sourceClassMapping = validateId(associationMapping, propertyMapping, classMappingIndex, propertyMapping._sourceSetImplementationId(), "source", processorSupport);

                    String targetId = propertyMapping._targetSetImplementationId();
                    SetImplementation targetClassMapping = validateId(associationMapping, propertyMapping, classMappingIndex, propertyMapping._targetSetImplementationId(), "target", processorSupport);

                    if (targetClassMapping instanceof EmbeddedSetImplementation)
                    {
                        throw new PureCompilationException(propertyMapping.getSourceInformation(), "Invalid target class mapping for property \'"
                                + propertyName + "\' in Association mapping \'" + ImportStub.withImportStubByPass(associationMapping._associationCoreInstance(), processorSupport).getName()
                                + "\'. Target \'" + targetId + "\' is an embedded class mapping, embedded mappings are only allowed to be the source in an Association Mapping.");
                    }

                    ListIterable<? extends PropertyMapping> sourcePropertyMappings = ((InstanceSetImplementation) sourceClassMapping)._propertyMappings().toList();
                    PropertyMapping alreadyMapped = sourcePropertyMappings.detect(new Predicate<PropertyMapping>()
                    {
                        @Override
                        public boolean accept(PropertyMapping propertyMapping)
                        {
                            return propertyName.equals(ImportStub.withImportStubByPass(propertyMapping._propertyCoreInstance(), processorSupport).getName());
                        }
                    });
                    if (alreadyMapped != null)
                    {
                        throw new PureCompilationException(propertyMapping.getSourceInformation(), "Property \'" + propertyName + "\' is mapped twice, once in Association mapping \'" + ImportStub.withImportStubByPass(associationMapping._associationCoreInstance(), processorSupport).getName() +
                                "\' and once in Class mapping \'" + ((Class) ImportStub.withImportStubByPass(targetClassMapping._classCoreInstance(), processorSupport))._name() + "\'. Only one mapping is allowed.");
                    }
                }
            }
        }
    }

    private static void validateEnumerationMappings(Mapping mapping, final ProcessorSupport processorSupport)
    {

        RichIterable<? extends EnumerationMapping<? extends Object>> enumerationMappings = mapping._enumerationMappings();
        if (!enumerationMappings.isEmpty())
        {
            for (EnumerationMapping enumerationMapping : enumerationMappings)
            {
                RichIterable<CoreInstance> sourceTypes = enumerationMapping._enumValueMappings().selectInstancesOf(EnumValueMapping.class).flatCollect(GET_SOURCE_TYPES).toList().distinct();
                if (!(sourceTypes.isEmpty() || 1 == sourceTypes.size()))
                {
                    RichIterable<String> sourceTypePaths = sourceTypes.collect(new Function<CoreInstance, String>()
                    {
                        @Override
                        public String valueOf(CoreInstance testNode)
                        {
                            return "\'" + PackageableElement.getUserPathForPackageableElement(testNode) + "\'";
                        }
                    });
                    throw new PureCompilationException(enumerationMapping.getSourceInformation(), "Enumeration Mapping \'" + ImportStub.withImportStubByPass(enumerationMapping._enumerationCoreInstance(), processorSupport).getName() + "\' has Source Types: " +
                            sourceTypePaths.makeString(", ") + ". Only one source Type is allowed for an Enumeration Mapping");
                }
            }
        }
    }

    private static void validateOperationMappings(Mapping mapping, MutableListMultimap<Type, CoreInstance> setImplementationsByClass, final ProcessorSupport processorSupport)
    {
        RichIterable<? extends MergeOperationSetImplementation> mergeOperations = mapping._classMappings().select(s -> s instanceof MergeOperationSetImplementation).collect(s -> (MergeOperationSetImplementation) s);
        if (!mergeOperations.isEmpty())
        {
            mergeOperations.forEach(m -> {

                        FunctionType ft = (FunctionType) processorSupport.function_getFunctionType ( m._validationFunction()._expressionSequence().getFirst());
                        if (!ft._returnType()._rawType()._name().equals("Boolean"))
                        {
                            throw new PureCompilationException(m.getSourceInformation(), "Merge validation function for class: "+ ImportStub.withImportStubByPass(m._classCoreInstance(), processorSupport).getName() +  " does not return Boolean");
                        }

                        MutableList<PureInstanceSetImplementation> mergesrc = setImplementationsByClass.get(m._class()).select(s -> s instanceof PureInstanceSetImplementation).collect(s -> (PureInstanceSetImplementation) s);
                        MutableList<Type> mergeSrcClasses = mergesrc.collect(r -> r._srcClass());

                        RichIterable<Type> validationParams = ft._parameters().collect(p -> p._genericType()._rawType());
                        if (!validationParams.allSatisfy(p -> mergeSrcClasses.contains(p)))
                        {
                            throw new PureCompilationException(m.getSourceInformation(), "Merge validation function for class: " + ImportStub.withImportStubByPass(m._classCoreInstance(), processorSupport).getName() + " has an invalid parameter. All parameters must be a src class of a merged set");
                        }
                    }

            );

        }

    }

    private static final Function<EnumValueMapping, RichIterable<? extends CoreInstance>> GET_SOURCE_TYPES = new Function<EnumValueMapping, RichIterable<? extends CoreInstance>>()
    {
        @Override
        public RichIterable<? extends CoreInstance> valueOf(EnumValueMapping enumValueMapping)
        {
            return enumValueMapping._sourceValuesCoreInstance().collect(new Function<CoreInstance, CoreInstance>()
            {
                @Override
                public CoreInstance valueOf(CoreInstance sourceValue)
                {
                    return sourceValue instanceof EnumStub ? ((EnumStub) sourceValue)._enumeration() : sourceValue.getClassifier();
                }
            });
        }
    };

    public static SetImplementation validateId(AssociationImplementation associationMapping, PropertyMapping propertyMapping, MapIterable<String, ? extends SetImplementation> classMappingIndex, String setImplementationId, String sourceOrTarget, ProcessorSupport processorSupport)
    {
        SetImplementation setImplementation = classMappingIndex.get(setImplementationId);
        Property property = (Property) ImportStub.withImportStubByPass(propertyMapping._propertyCoreInstance(), processorSupport);
        if ("target".equals(sourceOrTarget))
        {
            //Target, so need to find the property on the other end
            Association association = (Association) ImportStub.withImportStubByPass(associationMapping._associationCoreInstance(), processorSupport);
            property = association._properties().select(Predicates.notEqual(property)).getFirst();
        }

        if (setImplementation == null)
        {
            throw new PureCompilationException(propertyMapping.getSourceInformation(), "Unable to find " + sourceOrTarget + " class mapping (id:" + setImplementationId + ") for property \'"
                    + propertyMapping._propertyCoreInstance().getName() + "\' in Association mapping \'" + ImportStub.withImportStubByPass(associationMapping._associationCoreInstance(), processorSupport).getName()
                    + "\'. Make sure that you have specified a valid Class mapping id as the source id and target id, using the syntax \'property[sourceId, targetId]: ...\'.");
        }

        Class _class = (Class) ImportStub.withImportStubByPass(setImplementation._classCoreInstance(), processorSupport);
        String propertyName = property.getName();

        if (processorSupport.class_findPropertyUsingGeneralization(_class, propertyName) == null)
        {
            throw new PureCompilationException(propertyMapping.getSourceInformation(), "Association mapping property \'" + propertyName + "\' in Association mapping \'"
                    + ImportStub.withImportStubByPass(associationMapping._associationCoreInstance(), processorSupport).getName() + "\' is not a property of source class \'" + _class.getName() + "\'. Make sure that you have specified a valid source id.");
        }
        return setImplementation;
    }

    private static void validateIncludes(Mapping mapping, Matcher matcher, ValidatorState state, ProcessorSupport processorSupport)
    {
        ListIterable<? extends MappingInclude> includes = mapping._includes().toList();
        int includeCount = includes.size();
        if (includeCount > 0)
        {
            // Validate includes individually
            MutableList<MapIterable<Store, Store>> storeSubstitutionMaps = FastList.newList(includeCount);
            for (MappingInclude include : includes)
            {
                Mapping owner = (Mapping) ImportStub.withImportStubByPass(include._ownerCoreInstance(), processorSupport);
                if (owner != mapping)
                {
                    StringBuilder message = new StringBuilder("Corrupt mapping include: owner should be ");
                    PackageableElement.writeUserPathForPackageableElement(message, mapping);
                    message.append(", found ");
                    PackageableElement.writeUserPathForPackageableElement(message, owner);
                    throw new PureCompilationException(include.getSourceInformation(), message.toString());
                }

                Mapping includedMapping = (Mapping) ImportStub.withImportStubByPass(include._includedCoreInstance(), processorSupport);
                Validator.validate(includedMapping, state, matcher, processorSupport);

                MapIterable<Store, Store> includeStoreSubstitutions = validateIncludeStoreSubstitutions(include, processorSupport);
                storeSubstitutionMaps.add(includeStoreSubstitutions);
            }
            if (includeCount > 1)
            {
                MutableMap<Store, Store> mergedStoreSubstitutions = Maps.mutable.empty();
                for (MapIterable<Store, Store> subMap : storeSubstitutionMaps)
                {
                    for (Pair<Store, Store> pair : subMap.keyValuesView())
                    {
                        Store original = pair.getOne();
                        Store substitute = pair.getTwo();
                        Store otherSubstitute = mergedStoreSubstitutions.put(original, substitute);
                        if (otherSubstitute != null && otherSubstitute != substitute)
                        {
                            StringBuilder message = new StringBuilder("Store substitution error: multiple substitutions for ");
                            PackageableElement.writeUserPathForPackageableElement(message, original);
                            throw new PureCompilationException(mapping.getSourceInformation(), message.toString());
                        }
                    }
                }
            }

            // Validate includes recursively
            validateInclusionHierarchy(mapping, Lists.mutable.<Mapping>empty(), processorSupport);
        }
    }

    private static MapIterable<Store, Store> validateIncludeStoreSubstitutions(MappingInclude include, ProcessorSupport processorSupport)
    {
        ListIterable<? extends SubstituteStore> storeSubstitutions = include._storeSubstitutions().toList();
        if (storeSubstitutions.isEmpty())
        {
            return Maps.immutable.empty();
        }

        MutableMap<Store, Store> substitutionMap = UnifiedMap.newMap(storeSubstitutions.size());
        for (SubstituteStore storeSubstitution : storeSubstitutions)
        {
            CoreInstance original = ImportStub.withImportStubByPass(storeSubstitution._originalCoreInstance(), processorSupport);
            if (!(original instanceof Store))
            {
                throwStoreSubstitutionError(include, original);
            }

            CoreInstance substitute = ImportStub.withImportStubByPass(storeSubstitution._substituteCoreInstance(), processorSupport);
            if (!(substitute instanceof Store))
            {
                throwStoreSubstitutionError(include, substitute);
            }

            if (substitutionMap.put((Store) original, (Store) substitute) != null)
            {
                StringBuilder message = new StringBuilder("Store substitution error: multiple substitutions for ");
                PackageableElement.writeUserPathForPackageableElement(message, original);
                throw new PureCompilationException(include.getSourceInformation(), message.toString());
            }
            if (!storeIncludes((Store) substitute, (Store) original, processorSupport))
            {
                StringBuilder message = new StringBuilder("Store substitution error: ");
                PackageableElement.writeUserPathForPackageableElement(message, substitute);
                message.append(" does not include ");
                PackageableElement.writeUserPathForPackageableElement(message, original);
                throw new PureCompilationException(include.getSourceInformation(), message.toString());
            }
        }
        for (Store substitute : substitutionMap.valuesView())
        {
            if (substitutionMap.containsKey(substitute))
            {
                StringBuilder message = new StringBuilder("Store substitution error: ");
                PackageableElement.writeUserPathForPackageableElement(message, substitute);
                message.append(" appears both as an original and a substitute");
                throw new PureCompilationException(include.getSourceInformation(), message.toString());
            }
        }
        return substitutionMap;
    }

    private static void throwStoreSubstitutionError(MappingInclude include, CoreInstance original)
    {
        StringBuilder message = new StringBuilder("Store substitution error: ");
        PackageableElement.writeUserPathForPackageableElement(message, original);
        message.append(" is not a Store");
        throw new PureCompilationException(include.getSourceInformation(), message.toString());
    }

    private static void validateInclusionHierarchy(Mapping mapping, MutableList<Mapping> visited, ProcessorSupport processorSupport)
    {
        ListIterable<? extends MappingInclude> includes = mapping._includes().toList();
        if (includes.notEmpty())
        {
            MutableSet<CoreInstance> includesSet = UnifiedSet.newSet(includes.size());
            visited.add(mapping);
            for (MappingInclude include : includes)
            {
                Mapping includedMapping = (Mapping) ImportStub.withImportStubByPass(include._includedCoreInstance(), processorSupport);
                // Validate that a single mapping is not directly included more than once
                if (!includesSet.add(includedMapping))
                {
                    StringBuilder message = new StringBuilder("Mapping ");
                    PackageableElement.writeUserPathForPackageableElement(message, includedMapping);
                    message.append(" is included multiple times in ");
                    PackageableElement.writeUserPathForPackageableElement(message, mapping);
                    throw new PureCompilationException(mapping.getSourceInformation(), message.toString());
                }

                // Validate that there are no include loops
                if (visited.contains(includedMapping))
                {
                    CoreInstance rootMapping = visited.getFirst();
                    StringBuilder message = new StringBuilder("Circular include in mapping ");
                    PackageableElement.writeUserPathForPackageableElement(message, rootMapping);
                    message.append(": ");
                    for (Mapping map : visited)
                    {
                        PackageableElement.writeUserPathForPackageableElement(message, map);
                        message.append(" -> ");
                    }
                    PackageableElement.writeUserPathForPackageableElement(message, includedMapping);
                    throw new PureCompilationException(rootMapping.getSourceInformation(), message.toString());
                }
                validateInclusionHierarchy(includedMapping, visited, processorSupport);
            }
            visited.remove(visited.size() - 1);
        }
    }

    private static void validateStar(Mapping mapping, ProcessorSupport processorSupport)
    {
        MutableListMultimap<Class, SetImplementation> directMappings = Multimaps.mutable.list.empty();
        for (SetImplementation classMapping : mapping._classMappings())
        {
            Class src = (Class) ImportStub.withImportStubByPass(classMapping._classCoreInstance(), processorSupport);
            if (src == null)
            {
                throw new PureCompilationException(classMapping.getSourceInformation(), "Class mapping is missing class");
            }
            if (!(classMapping instanceof EmbeddedSetImplementation))
            {
                directMappings.put(src, classMapping);
            }
        }

        for (Pair<Class, RichIterable<SetImplementation>> val : directMappings.keyMultiValuePairsView())
        {
            RichIterable<SetImplementation> classMappings = val.getTwo();
            if (classMappings.size() == 1)
            {
                classMappings.toList().get(0)._root(true);
            } else
            {
                int rootCount = classMappings.count(IS_ROOT);
                if (rootCount != 1)
                {
                    throw new PureCompilationException(mapping.getSourceInformation(), "The class '" + val.getOne().getName() + "' is mapped by " + classMappings.size() + " set implementations and has " + rootCount + " roots. There should be exactly one root set implementation for the class, and it should be marked with a '*'.");
                }
            }
        }
    }

    private static void validateMappedEntitiesInIncludes(Mapping rootMapping, Mapping mapping, ProcessorSupport processorSupport, MutableListMultimap<Type, CoreInstance> setImplementationsByClass, ValidatorState state, Matcher matcher)
    {
        for (MappingInclude include : mapping._includes())
        {
            Mapping includedMapping = (Mapping) ImportStub.withImportStubByPass(include._includedCoreInstance(), processorSupport);
            validateMappedEntitiesInIncludes(rootMapping, includedMapping, processorSupport, setImplementationsByClass, state, matcher);
            validateMappedEntities(rootMapping, includedMapping._classMappings(), true, setImplementationsByClass, processorSupport, state, matcher);
            validateMappedEntities(rootMapping, includedMapping._enumerationMappings(), false, setImplementationsByClass, processorSupport, state, matcher);
        }
    }

    private static void validateMappedEntities(CoreInstance rootMapping, RichIterable<? extends CoreInstance> mappedInstances, boolean isClassMapping, MutableListMultimap<Type, CoreInstance> setImplementationsByClass, ProcessorSupport processorSupport, ValidatorState state, Matcher matcher)
    {
        for (CoreInstance mappedInstance : mappedInstances)
        {
            state.setRootMapping(rootMapping);
            Validator.validate(mappedInstance, state, matcher, processorSupport);
            state.validateSetImplementation(mappedInstance, isClassMapping);

            Type src = null;
            if (mappedInstance instanceof SetImplementation)
            {
                src = (Class) ImportStub.withImportStubByPass(((SetImplementation) mappedInstance)._classCoreInstance(), processorSupport);
            } else if (mappedInstance instanceof EnumerationMapping)
            {
                src = (Enumeration) ImportStub.withImportStubByPass(((EnumerationMapping) mappedInstance)._enumerationCoreInstance(), processorSupport);
            }
            setImplementationsByClass.put(src, mappedInstance);
        }
    }

    private static boolean storeIncludes(Store store, Store includedStore, ProcessorSupport processorSupport)
    {
        return store == includedStore || storeIncludes(store, includedStore, Sets.mutable.<Store>empty(), processorSupport);
    }

    private static boolean storeIncludes(Store store, CoreInstance includedStore, MutableSet<Store> visited, ProcessorSupport processorSupport)
    {
        if (visited.add(store))
        {
            ListIterable<? extends Store> includes = (ListIterable<? extends Store>) ImportStub.withImportStubByPasses((ListIterable<? extends CoreInstance>) store._includesCoreInstance(), processorSupport);
            if (includes.contains(includedStore))
            {
                return true;
            }
            for (Store include : includes)
            {
                if (storeIncludes(include, includedStore, visited, processorSupport))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static void validateVisibility(Mapping mapping, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        Package pkg = mapping._package();
        if (pkg != null)
        {
            String sourceId = mapping.getSourceInformation().getSourceId();

            for (SetImplementation classMapping : mapping._classMappings())
            {
                Class _class = (Class) ImportStub.withImportStubByPass(classMapping._classCoreInstance(), processorSupport);
                VisibilityValidation.validatePackageAndSourceVisibility(classMapping, pkg, sourceId, context, validatorState, processorSupport, _class);
            }

            for (EnumerationMapping enumerationMapping : mapping._enumerationMappings())
            {
                Enumeration enumeration = (Enumeration) ImportStub.withImportStubByPass(enumerationMapping._enumerationCoreInstance(), processorSupport);
                VisibilityValidation.validatePackageAndSourceVisibility(enumerationMapping, pkg, sourceId, context, validatorState, processorSupport, enumeration);
            }

            for (MappingInclude include : mapping._includes())
            {
                Mapping included = (Mapping) ImportStub.withImportStubByPass(include._includedCoreInstance(), processorSupport);
                VisibilityValidation.validatePackageAndSourceVisibility(mapping, pkg, sourceId, context, validatorState, processorSupport, included);
            }
        }
    }
}
