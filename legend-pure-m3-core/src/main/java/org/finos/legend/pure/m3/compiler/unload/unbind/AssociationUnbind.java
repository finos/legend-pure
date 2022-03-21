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

package org.finos.legend.pure.m3.compiler.unload.unbind;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.compiler.AssociationPropertyOwnerStrategy;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.PropertyOwnerStrategy;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.AssociationProjection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class AssociationUnbind implements MatchRunner<Association>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Association;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void run(Association association, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();
        ListIterable<? extends Property<?, ?>> properties = ListHelper.wrapListIterable(association._properties());

        Class<?> classA = getTargetClass(properties.get(0), processorSupport);
        if (classA != null)
        {
            ((Class) classA)._propertiesFromAssociationsRemove(properties.get(1));
            if (classA._propertiesFromAssociations().isEmpty())
            {
                classA._propertiesFromAssociationsRemove();
            }
        }

        Class<?> classB = getTargetClass(properties.get(1), processorSupport);
        if (classB != null)
        {
            ((Class) classB)._propertiesFromAssociationsRemove(properties.get(0));
            if (classB._propertiesFromAssociations().isEmpty())
            {
                classB._propertiesFromAssociationsRemove();
            }
        }

        removeClassQualifiedPropertiesFromAssociations(association, Sets.immutable.with(classA, classB), processorSupport);

        matchValue(state, matcher, association._properties());
        matchValue(state, matcher, association._originalMilestonedProperties());
        matchValue(state, matcher, association._qualifiedProperties());

        undoMilestoningProcessing(association, state, context);

        if (association instanceof AssociationProjection)
        {
            Shared.cleanUpReferenceUsage(((AssociationProjection) association)._projectedAssociationCoreInstance(), association, processorSupport);
            Shared.cleanImportStub(((AssociationProjection) association)._projectedAssociationCoreInstance(), processorSupport);
            association._propertiesRemove();
            RichIterable<? extends CoreInstance> projections = ((AssociationProjection) association)._projectionsCoreInstance();
            projections.forEach(projection -> Shared.cleanImportStub(projection, processorSupport));
        }
    }

    private void matchValue(MatcherState state, Matcher matcher, RichIterable<? extends CoreInstance> values)
    {
        for (CoreInstance value : values)
        {
            matcher.fullMatch(value, state);
        }
    }

    private void undoMilestoningProcessing(Association association, MatcherState state, Context context)
    {
        PropertyOwnerStrategy strategy = AssociationPropertyOwnerStrategy.ASSOCIATION_PROPERTY_OWNER_STRATEGY;
        MilestoningUnbind.removeGeneratedMilestoningProperties(association, state.getProcessorSupport(), association._properties(), strategy::propertiesRemove, (a, p) -> strategy.setProperties(a, p));
        MilestoningUnbind.removeGeneratedMilestoningProperties(association, state.getProcessorSupport(), association._originalMilestonedProperties(), strategy::originalMilestonedPropertiesRemove, (a, v) -> strategy.setOriginalMilestonedProperties(a, v));
        MilestoningUnbind.removeGeneratedMilestoningProperties(association, state.getProcessorSupport(), association._qualifiedProperties(), strategy::qualifiedPropertiesRemove, (a, v) -> strategy.setQualifiedProperties(a, v));
        MilestoningUnbind.undoMoveProcessedOriginalMilestonedProperties(association, context);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void removeClassQualifiedPropertiesFromAssociations(Association association, SetIterable<Class<?>> propertyOwners, ProcessorSupport processorSupport)
    {
        RichIterable<? extends QualifiedProperty<?>> qualifiedProperties = association._qualifiedProperties();
        qualifiedProperties.forEach(qualifiedProperty ->
        {
            Class<?> qualifiedPropertyTargetClass = getTargetClass(qualifiedProperty, processorSupport);
            Class<?> qualifiedPropertyOwner = propertyOwners.size() == 1 ? propertyOwners.getAny() : propertyOwners.detect(o -> !o.equals(qualifiedPropertyTargetClass));
            ((Class) qualifiedPropertyOwner)._qualifiedPropertiesFromAssociationsRemove(qualifiedProperty);
            if (qualifiedPropertyOwner._qualifiedPropertiesFromAssociations().isEmpty())
            {
                qualifiedPropertyOwner._qualifiedPropertiesFromAssociationsRemove();
            }
        });
    }

    private Class<?> getTargetClass(AbstractProperty<?> property, ProcessorSupport processorSupport)
    {
        return (Class<?>) ImportStub.withImportStubByPassDoNotResolve(property._genericType()._rawTypeCoreInstance(), processorSupport);
    }
}
