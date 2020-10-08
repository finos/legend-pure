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
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.PropertyOwnerStrategy;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.AssociationProjection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class AssociationUnbind implements MatchRunner<Association>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Association;
    }

    @Override
    public void run(Association association, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();
        ListIterable<? extends Property<?, ?>> properties = association._properties().toList();

        Type classA = getTargetClass(properties.get(0), processorSupport);
        if (classA != null)
        {
            ((Class)classA)._propertiesFromAssociationsRemove(properties.get(1));
            if(((Class)classA)._propertiesFromAssociations().isEmpty())
            {
                ((Class)classA)._propertiesFromAssociationsRemove();
            }
        }

        Type classB = getTargetClass(properties.get(1), processorSupport);
        if (classB != null)
        {
            ((Class)classB)._propertiesFromAssociationsRemove(properties.get(0));
            if(((Class)classB)._propertiesFromAssociations().isEmpty())
            {
                ((Class)classB)._propertiesFromAssociationsRemove();
            }
        }

        removeClassQualifiedPropertiesFromAssociations(association, FastList.newListWith(classA, classB), state, matcher, processorSupport);

        RichIterable<? extends Property<?, ?>> values0 = association._properties();
        matchValue(state, matcher, values0);

        RichIterable<? extends Property<?, ?>> values1 = association._originalMilestonedProperties();
        matchValue(state, matcher, values1);

        RichIterable<? extends QualifiedProperty<?>> values2 = association._qualifiedProperties();
        matchValue(state, matcher, values2);

        undoMilestoningProcessing(association, state, context);

        if (association instanceof AssociationProjection)
        {
            Shared.cleanUpReferenceUsage(((AssociationProjection)association)._projectedAssociationCoreInstance(), association, processorSupport);
            Shared.cleanImportStub(((AssociationProjection)association)._projectedAssociationCoreInstance(), processorSupport);
            association._propertiesRemove();
            RichIterable<? extends CoreInstance> projections = ((AssociationProjection)association)._projectionsCoreInstance();
            for (CoreInstance projection : projections)
            {
                Shared.cleanImportStub(projection, processorSupport);
            }
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
        MilestoningUnbind.removeGeneratedMilestoningProperties(association, state.getProcessorSupport(), association._properties(), PropertyOwnerStrategy.PROPERTIES_REMOVE, PropertyOwnerStrategy.PROPERTIES_SET);
        MilestoningUnbind.removeGeneratedMilestoningProperties(association, state.getProcessorSupport(), association._originalMilestonedProperties(), PropertyOwnerStrategy.ORIGINAL_MILESTONED_PROPERTIES_REMOVE, PropertyOwnerStrategy.ORIGINAL_MILESTONED_PROPERTIES_SET);
        MilestoningUnbind.removeGeneratedMilestoningProperties(association, state.getProcessorSupport(), association._qualifiedProperties(), PropertyOwnerStrategy.QUALIFIED_PROPERTIES_REMOVE, PropertyOwnerStrategy.QUALIFIED_PROPERTIES_SET);
        MilestoningUnbind.undoMoveProcessedOriginalMilestonedProperties(association, context);
    }

    private void removeClassQualifiedPropertiesFromAssociations(Association association, final ListIterable<Type> propertyOwners, final MatcherState state, final Matcher matcher, final ProcessorSupport processorSupport)
    {
        RichIterable<? extends QualifiedProperty> qualifiedProperties = association._qualifiedProperties();
        qualifiedProperties.forEach(new Procedure<QualifiedProperty>()
        {
            @Override
            public void value(QualifiedProperty qualifiedProperty)
            {
                Type qualifiedPropertyGenericType = (Type)ImportStub.withImportStubByPass(qualifiedProperty._genericType()._rawTypeCoreInstance(), processorSupport);
                Type qualifiedPropertyOwner = propertyOwners.distinct().size() == 1 ? propertyOwners.get(0) : propertyOwners.detect(Predicates.notEqual(qualifiedPropertyGenericType));
                ((Class)qualifiedPropertyOwner)._qualifiedPropertiesFromAssociationsRemove(qualifiedProperty);
                if(((Class)qualifiedPropertyOwner)._qualifiedPropertiesFromAssociations().isEmpty())
                {
                    ((Class)qualifiedPropertyOwner)._qualifiedPropertiesFromAssociationsRemove();
                }
            }
        });
    }

    private Type getTargetClass(AbstractProperty property, ProcessorSupport processorSupport)
    {
        try
        {
            return (Type)ImportStub.withImportStubByPass(property._genericType()._rawTypeCoreInstance(), processorSupport);
        }
        catch (PureCompilationException e)
        {
            return null;
        }
    }
}
