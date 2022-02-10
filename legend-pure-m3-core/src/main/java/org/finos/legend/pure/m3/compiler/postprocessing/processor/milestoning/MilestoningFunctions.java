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

package org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.SetAdapter;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.PropertyOwnerStrategy;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.ElementWithStereotypes;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.profile.Profile;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

import java.util.EnumSet;

public class MilestoningFunctions
{
    public static final String MILESTONING = "milestoning";
    private static final String MILESTONING_PATH = "meta::pure::profiles::" + MILESTONING;
    static final String MILESTONE_LAMBDA_VARIABLE_NAME = "v_milestone";
    static final String GENERATED_MILESTONING_STEREOTYPE_VALUE = "generatedmilestoningproperty";
    static final String GENERATED_MILESTONING_DATE_STEREOTYPE_VALUE = "generatedmilestoningdateproperty";
    static final String GENERATED_MILESTONING_STEREOTYPE = "<<" + MILESTONING_PATH + "." + GENERATED_MILESTONING_STEREOTYPE_VALUE + ">>";
    static final String GENERATED_MILESTONING_DATE_STEREOTYPE = "<<" + MILESTONING_PATH + "." + GENERATED_MILESTONING_DATE_STEREOTYPE_VALUE + ">>";
    private static final String GENERATED_MILESTONING_PATH_SUFFIX = MILESTONING_PATH + "@" + GENERATED_MILESTONING_STEREOTYPE_VALUE;
    private static final String GENERATED_MILESTONING_DATE_PATH_SUFFIX = MILESTONING_PATH + "@" + GENERATED_MILESTONING_DATE_STEREOTYPE_VALUE;
    private static final String EDGEPOINT_PROPERTY_NAME_SUFFIX = "AllVersions";
    private static final String RANGE_PROPERTY_NAME_SUFFIX = "AllVersionsInRange";
    public static final String MILESTONING_GET_ALL_FUNCTION_PATH = "meta::pure::functions::collection::getAll_Class_1__Date_1__T_MANY_";
    public static final String BITEMPORAL_MILESTONING_GET_ALL_FUNCTION_PATH = "meta::pure::functions::collection::getAll_Class_1__Date_1__Date_1__T_MANY_";

    public static class IsMilestonePropertyPredicate implements Predicate<CoreInstance>
    {
        private final ProcessorSupport processorSupport;

        public IsMilestonePropertyPredicate(ProcessorSupport processorSupport)
        {
            this.processorSupport = processorSupport;
        }

        @Override
        public boolean accept(CoreInstance property)
        {
            return isGeneratedMilestoningProperty(property, this.processorSupport);
        }
    }

    public static class IsMilestoneDatePropertyPredicate implements Predicate<CoreInstance>
    {
        private final ProcessorSupport processorSupport;

        public IsMilestoneDatePropertyPredicate(ProcessorSupport processorSupport)
        {
            this.processorSupport = processorSupport;
        }

        @Override
        public boolean accept(CoreInstance property)
        {
            return isGeneratedMilestoningDateProperty(property, this.processorSupport);
        }
    }

    @Deprecated
    public static Function<CoreInstance, ListIterable<CoreInstance>> toInstanceValues(ProcessorSupport processorSupport)
    {
        return MilestoningFunctions::toInstanceValues;
    }

    @Deprecated
    public static Predicate<CoreInstance> isLatestDate(ProcessorSupport processorSupport)
    {
        return instance -> isLatestDate(instance, processorSupport);
    }

    @Deprecated
    public static final Function<MilestoningStereotype, String> GET_PLATFORM_NAME = MilestoningStereotype::getPurePlatformStereotypeName;

    private MilestoningFunctions()
    {
    }

    @SuppressWarnings("unchecked")
    public static ListIterable<CoreInstance> toInstanceValues(CoreInstance instance)
    {
        return (instance instanceof InstanceValue) ?
                (ListIterable<CoreInstance>) ((InstanceValue) instance)._valuesCoreInstance() :
                Lists.immutable.with(instance);
    }

    public static boolean isLatestDate(CoreInstance instance, ProcessorSupport processorSupport)
    {
        return Instance.instanceOf(instance, M3Paths.LatestDate, processorSupport);
    }

    private static String getStereotypeName(CoreInstance stereotype)
    {
        if (stereotype instanceof ImportStub)
        {
            String idOrPath = ((ImportStub) stereotype)._idOrPath();
            return idOrPath.substring(idOrPath.indexOf('@') + 1);
        }
        return stereotype.getName();
    }

    public static boolean isEdgePointProperty(CoreInstance property, ProcessorSupport processorSupport)
    {
        return isGeneratedMilestoningProperty(property, processorSupport) &&
                Instance.instanceOf(property, M3Paths.Property, processorSupport) &&
                property.getName().endsWith(EDGEPOINT_PROPERTY_NAME_SUFFIX);
    }

    public static boolean isAllVersionsInRangeProperty(CoreInstance property, ProcessorSupport processorSupport)
    {
        return isGeneratedMilestoningProperty(property, processorSupport) && property.getValueForMetaPropertyToOne(M3Properties.name).getName().endsWith(RANGE_PROPERTY_NAME_SUFFIX);
    }

    public static boolean isGeneratedMilestoningProperty(CoreInstance property, ProcessorSupport processorSupport, String stereotype, String milestoningPathSuffix)
    {
        if (property instanceof ElementWithStereotypes)
        {
            RichIterable<? extends CoreInstance> stereotypes = ((ElementWithStereotypes) property)._stereotypesCoreInstance();
            if (stereotypes.notEmpty())
            {
                CoreInstance profile = processorSupport.package_getByUserPath(M3Paths.Milestoning);
                CoreInstance milestoningStereotype = Profile.findStereotype(profile, stereotype);
                return stereotypes.anySatisfy(st -> (st instanceof ImportStub) ? ((ImportStub) st)._idOrPath().endsWith(milestoningPathSuffix) : milestoningStereotype.equals(st));
            }
        }
        return false;
    }

    public static boolean isGeneratedMilestoningProperty(CoreInstance property, ProcessorSupport processorSupport)
    {
        return isGeneratedMilestoningProperty(property, processorSupport, GENERATED_MILESTONING_STEREOTYPE_VALUE, GENERATED_MILESTONING_PATH_SUFFIX);
    }

    public static boolean isGeneratedMilestoningDateProperty(CoreInstance property, ProcessorSupport processorSupport)
    {
        return isGeneratedMilestoningProperty(property, processorSupport, GENERATED_MILESTONING_DATE_STEREOTYPE_VALUE, GENERATED_MILESTONING_DATE_PATH_SUFFIX);
    }

    public static boolean isAutoGeneratedMilestoningNamedDateProperty(Property<?, ?> property, ProcessorSupport processorSupport)
    {
        return isGeneratedMilestoningDateProperty(property, processorSupport) && MILESTONING.equals(property._name());
    }

    public static MilestonedPropertyMetaData getMilestonedMetaDataForProperty(QualifiedProperty<?> property, ProcessorSupport processorSupport)
    {
        CoreInstance returnType = org.finos.legend.pure.m3.navigation.importstub.ImportStub.withImportStubByPass(property._genericType()._rawTypeCoreInstance(), processorSupport);
        MutableSet<MilestoningStereotypeEnum> stereotypes = getTemporalStereotypeSetFromTopMostNonTopTypeGeneralizations(returnType, processorSupport);
        ListIterable<String> classTemporalStereotypes = stereotypes.collect(MilestoningStereotypeEnum::getPurePlatformStereotypeName, Lists.mutable.ofInitialCapacity(stereotypes.size()));
        ListIterable<String> temporalDatePropertyNamesForStereotypes = stereotypes.flatCollect(MilestoningStereotype::getTemporalDatePropertyNames).toSortedList(MilestoningStereotypeEnum::compareTemporalDatePropertyNames);
        return new MilestonedPropertyMetaData(classTemporalStereotypes, temporalDatePropertyNamesForStereotypes);
    }

    public static boolean isGeneratedQualifiedProperty(CoreInstance property, ProcessorSupport processorSupport)
    {
        return Instance.instanceOf(property, M3Paths.QualifiedProperty, processorSupport) && isGeneratedMilestoningProperty(property, processorSupport) && !isAllVersionsInRangeProperty(property, processorSupport);
    }

    public static boolean isGeneratedQualifiedPropertyWithWithAllMilestoningDatesSpecified(CoreInstance property, ProcessorSupport processorSupport)
    {
        if (isGeneratedQualifiedProperty(property, processorSupport))
        {
            return getParametersCount((QualifiedProperty<?>) property, processorSupport) == getCountOfParametersSatisfyingMilestoningDateRequirments((QualifiedProperty<?>) property, processorSupport);
        }
        return false;
    }

    public static boolean isGeneratedMilestonedQualifiedPropertyWithMissingDates(CoreInstance property, ProcessorSupport processorSupport)
    {
        if (isGeneratedQualifiedProperty(property, processorSupport))
        {
            return getParametersCount((QualifiedProperty<?>) property, processorSupport) != getCountOfParametersSatisfyingMilestoningDateRequirments((QualifiedProperty<?>) property, processorSupport);
        }
        return false;
    }

    private static int getParametersCount(QualifiedProperty<?> qualifiedProperty, ProcessorSupport processorSupport)
    {
        FunctionType functionType = (FunctionType) processorSupport.function_getFunctionType(qualifiedProperty);
        return functionType._parameters().size();
    }

    private static int getCountOfParametersSatisfyingMilestoningDateRequirments(QualifiedProperty<?> milestonedQualifiedProperty, ProcessorSupport processorSupport)
    {
        if (!isGeneratedMilestoningProperty(milestonedQualifiedProperty, processorSupport))
        {
            throw new PureCompilationException("Unable to get milestoning date parameters for non milestoned QualifiedProperty: " + milestonedQualifiedProperty.getName());
        }
        Class<?> returnType = (Class<?>) org.finos.legend.pure.m3.navigation.importstub.ImportStub.withImportStubByPass(milestonedQualifiedProperty._genericType()._rawTypeCoreInstance(), processorSupport);
        MilestoningStereotypeEnum milestoningStereotype = getTemporalStereotypeSetFromTopMostNonTopTypeGeneralizations(returnType, processorSupport).getAny();
        return 1 + milestoningStereotype.getTemporalDatePropertyNames().size();
    }

    public static ListIterable<MilestoningStereotypeEnum> getTemporalStereoTypesFromTopMostNonTopTypeGeneralizations(CoreInstance cls, ProcessorSupport processorSupport)
    {
        return getTemporalStereotypeSetFromTopMostNonTopTypeGeneralizations(cls, processorSupport).toList();
    }

    private static MutableSet<MilestoningStereotypeEnum> getTemporalStereotypeSetFromTopMostNonTopTypeGeneralizations(CoreInstance cls, ProcessorSupport processorSupport)
    {
        return SetAdapter.adapt(Type.getTopMostNonTopTypeGeneralizations(cls, processorSupport).flatCollect(MilestoningFunctions::getTemporalStereoTypesExcludingParents, EnumSet.noneOf(MilestoningStereotypeEnum.class)));
    }

    private static MutableSet<String> getTemporalStereotypePropertyNameSetFromTopMostNonTopTypeGeneralizations(CoreInstance cls, ProcessorSupport processorSupport)
    {
        return getTemporalStereotypeSetFromTopMostNonTopTypeGeneralizations(cls, processorSupport).flatCollect(MilestoningStereotype::getTemporalDatePropertyNames);
    }

    public static ListIterable<String> getTemporalStereoTypePropertyNamesFromTopMostNonTopTypeGeneralizations(CoreInstance cls, ProcessorSupport processorSupport)
    {
        return getTemporalStereotypePropertyNameSetFromTopMostNonTopTypeGeneralizations(cls, processorSupport).toSortedList(MilestoningStereotypeEnum::compareTemporalDatePropertyNames);
    }

    public static ListIterable<String> getAllTemporalStereoTypePropertyNamesFromTopMostNonTopTypeGeneralizations(CoreInstance cls, ProcessorSupport processorSupport)
    {
        return getAllTemporalStereotypePropertyNameSetFromTopMostNonTopTypeGeneralizations(cls, processorSupport).toList();
    }

    public static MutableSet<String> getAllTemporalStereotypePropertyNameSetFromTopMostNonTopTypeGeneralizations(CoreInstance cls, ProcessorSupport processorSupport)
    {
        return getTemporalStereotypePropertyNameSetFromTopMostNonTopTypeGeneralizations(cls, processorSupport).with(MILESTONING);
    }


    @Deprecated
    public static ListIterable<MilestoningStereotypeEnum> getTemporalStereoTypesExcludingParents(CoreInstance cls, ProcessorSupport processorSupport)
    {
        return getTemporalStereoTypesExcludingParents(cls);
    }

    public static ListIterable<MilestoningStereotypeEnum> getTemporalStereoTypesExcludingParents(CoreInstance cls)
    {
        if (!(cls instanceof ElementWithStereotypes))
        {
            return Lists.immutable.empty();
        }
        MutableSet<String> clsStereotypeNames = ((ElementWithStereotypes) cls)._stereotypesCoreInstance().collect(MilestoningFunctions::getStereotypeName, Sets.mutable.empty());
        return ArrayIterate.select(MilestoningStereotypeEnum.values(), st -> clsStereotypeNames.contains(st.getPurePlatformStereotypeName()));
    }

    public static String getEdgePointPropertyName(String propertyName)
    {
        return propertyName + EDGEPOINT_PROPERTY_NAME_SUFFIX;
    }

    public static String getRangePropertyName(String propertyName)
    {
        return propertyName + RANGE_PROPERTY_NAME_SUFFIX;
    }

    public static String getSourceEdgePointPropertyName(String propertyName)
    {
        return propertyName.replaceAll(EDGEPOINT_PROPERTY_NAME_SUFFIX, "");
    }

    static void setProperties(ListIterable<? extends CoreInstance> properties, PropertyOwner propertyOwner, Context context)
    {
        if (!properties.equals(PropertyOwnerStrategy.getPropertyOwnerStrategy(propertyOwner).properties(propertyOwner)))
        {
            propertyOwner.setKeyValues(M3PropertyPaths.properties, properties);
            updateAndInvalidate(propertyOwner, context);
        }
    }

    static void setQualifiedProperties(ListIterable<? extends CoreInstance> properties, PropertyOwner propertyOwner, Context context)
    {
        if (!properties.equals(PropertyOwnerStrategy.getPropertyOwnerStrategy(propertyOwner).qualifiedProperties(propertyOwner)))
        {
            propertyOwner.setKeyValues(M3PropertyPaths.qualifiedProperties, properties);
            updateAndInvalidate(propertyOwner, context);
        }
    }

    public static void updateAndInvalidate(CoreInstance ci, Context context)
    {
        context.update(ci);
        if (ci.hasBeenValidated())
        {
            ci.markNotValidated();
        }
    }
}