// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.m2.relational.serialization.grammar.v1.processor;

import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.pure.m2.relational.M2RelationalPaths;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.EmbeddedRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Literal;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.SQLNull;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.BusinessMilestoning;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.ProcessingMilestoning;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class MilestoningPropertyMappingProcessor
{
    private MilestoningPropertyMappingProcessor()
    {
    }

    static void processMilestoningPropertyMapping(RelationalInstanceSetImplementation immediateRelationalParentSet, RootRelationalInstanceSetImplementation rootRelationalParentSet, ProcessorSupport processorSupport)
    {
        immediateRelationalParentSet._propertyMappings().asLazy()
                .selectInstancesOf(EmbeddedRelationalInstanceSetImplementation.class)
                .forEach(m -> processMilestoningPropertyMapping(m, rootRelationalParentSet, processorSupport));
        createMilestoningPropertyMapping(immediateRelationalParentSet, rootRelationalParentSet, processorSupport);
    }

    private static void createMilestoningPropertyMapping(RelationalInstanceSetImplementation immediateRelationalParentSet, RootRelationalInstanceSetImplementation rootRelationalParentSet, ProcessorSupport processorSupport)
    {
        if (shouldCreateMilestoningPropertyMapping(immediateRelationalParentSet))
        {
            EmbeddedRelationalInstanceSetImplementation embeddedRelationalInstance = createEmbeddedRelationalInstance(immediateRelationalParentSet, rootRelationalParentSet, processorSupport);
            rootRelationalParentSet._parent()._classMappingsAdd(embeddedRelationalInstance);
            immediateRelationalParentSet._propertyMappingsAdd(embeddedRelationalInstance);
        }
    }

    private static EmbeddedRelationalInstanceSetImplementation createEmbeddedRelationalInstance(RelationalInstanceSetImplementation immediateRelationalParentSet, RootRelationalInstanceSetImplementation rootRelationalParentSet, ProcessorSupport processorSupport)
    {
        EmbeddedRelationalInstanceSetImplementation embeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation) processorSupport.newEphemeralAnonymousCoreInstance(M2RelationalPaths.EmbeddedRelationalInstanceSetImplementation);
        embeddedRelationalInstance.setSourceInformation(immediateRelationalParentSet.getSourceInformation());
        embeddedRelationalInstance._root(false);
        embeddedRelationalInstance._sourceSetImplementationId(immediateRelationalParentSet._id());
        embeddedRelationalInstance._id(embeddedRelationalInstance._sourceSetImplementationId() + "_" + MilestoningFunctions.MILESTONING);
        embeddedRelationalInstance._targetSetImplementationId(embeddedRelationalInstance._id());
        embeddedRelationalInstance._property((Property<?, ?>) processorSupport.class_getSimplePropertiesByName(immediateRelationalParentSet._class()).get(MilestoningFunctions.MILESTONING));
        embeddedRelationalInstance._class((Class<?>) embeddedRelationalInstance._property()._genericType()._rawType());
        embeddedRelationalInstance._parent(rootRelationalParentSet._parent());
        embeddedRelationalInstance._owner(immediateRelationalParentSet);
        embeddedRelationalInstance._setMappingOwner(rootRelationalParentSet);
        embeddedRelationalInstance._propertyMappings(createRelationalPropertyMappings(immediateRelationalParentSet._class(), rootRelationalParentSet, embeddedRelationalInstance, processorSupport));
        return embeddedRelationalInstance;
    }

    private static ImmutableList<RelationalPropertyMapping> createRelationalPropertyMappings(Class<?> immediateRelationalParentClass, RootRelationalInstanceSetImplementation rootRelationalParentSet, EmbeddedRelationalInstanceSetImplementation embeddedRelationalInstance, ProcessorSupport processorSupport)
    {
        return MilestoningFunctions.getTemporalStereoTypesExcludingParents(immediateRelationalParentClass).getFirst()
                .getMilestoningPropertyNames()
                .collect(name -> createRelationalPropertyMapping(name, rootRelationalParentSet, embeddedRelationalInstance, processorSupport))
                .toImmutable();
    }

    private static RelationalPropertyMapping createRelationalPropertyMapping(String propertyName, RootRelationalInstanceSetImplementation rootRelationalParentSet, EmbeddedRelationalInstanceSetImplementation embeddedRelationalInstance, ProcessorSupport processorSupport)
    {
        RelationalPropertyMapping propertyMapping = (RelationalPropertyMapping) processorSupport.newEphemeralAnonymousCoreInstance(M2RelationalPaths.RelationalPropertyMapping);
        propertyMapping.setSourceInformation(embeddedRelationalInstance.getSourceInformation());
        propertyMapping._localMappingProperty(false);
        propertyMapping._property((Property<?, ?>) processorSupport.class_getSimplePropertiesByName(embeddedRelationalInstance._class()).get(propertyName));
        propertyMapping._owner(embeddedRelationalInstance);
        propertyMapping._sourceSetImplementationId(embeddedRelationalInstance._id());
        propertyMapping._targetSetImplementationId("");
        propertyMapping._relationalOperationElement(createRelationalOperationElement(propertyName, rootRelationalParentSet, embeddedRelationalInstance.getSourceInformation(), processorSupport));
        return propertyMapping;
    }

    private static RelationalOperationElement createRelationalOperationElement(String propertyName, RootRelationalInstanceSetImplementation rootRelationalParentSet, SourceInformation sourceInfo, ProcessorSupport processorSupport)
    {
        try
        {
            TableAlias mainTableAlias = rootRelationalParentSet._mainTableAlias();
            Column column = getColumn(propertyName, mainTableAlias);
            return createTableAliasColumn(mainTableAlias, rootRelationalParentSet, column, sourceInfo, processorSupport);
        }
        catch (Exception ignore)
        {
            return createSQLNullLiteral(processorSupport);
        }
    }

    private static TableAliasColumn createTableAliasColumn(TableAlias mainTableAlias, RootRelationalInstanceSetImplementation rootRelationalParentSet, Column column, SourceInformation sourceInfo, ProcessorSupport processorSupport)
    {
        TableAliasColumn tableAliasColumn = (TableAliasColumn) processorSupport.newEphemeralAnonymousCoreInstance(M2RelationalPaths.TableAliasColumn);
        tableAliasColumn.setSourceInformation(sourceInfo);
        tableAliasColumn._alias(createTableAlias(mainTableAlias, sourceInfo, processorSupport));
        tableAliasColumn._setMappingOwner(rootRelationalParentSet);
        tableAliasColumn._column(column);
        return tableAliasColumn;
    }

    private static TableAlias createTableAlias(TableAlias mainTableAlias, SourceInformation sourceInfo, ProcessorSupport processorSupport)
    {
        TableAlias tableAlias = (TableAlias) processorSupport.newEphemeralAnonymousCoreInstance(M2RelationalPaths.TableAlias);
        tableAlias.setSourceInformation(sourceInfo);
        tableAlias._database(mainTableAlias._database());
        tableAlias._relationalElement(mainTableAlias._relationalElement());
        tableAlias._name(mainTableAlias._name());
        return tableAlias;
    }

    private static Literal createSQLNullLiteral(ProcessorSupport processorSupport)
    {
        Literal literal = (Literal) processorSupport.newEphemeralAnonymousCoreInstance(M2RelationalPaths.Literal);
        SQLNull sqlNull = (SQLNull) processorSupport.newEphemeralAnonymousCoreInstance(M2RelationalPaths.SQLNull);
        literal._value(sqlNull);
        return literal;
    }

    private static boolean shouldCreateMilestoningPropertyMapping(RelationalInstanceSetImplementation immediateRelationalParentSet)
    {
        return (immediateRelationalParentSet._superSetImplementationId() == null) &&
                MilestoningFunctions.getTemporalStereoTypesExcludingParents(immediateRelationalParentSet._class()).notEmpty();
    }

    private static Column getColumn(String milestoningPropertyName, TableAlias tableAlias)
    {
        Table table = (Table) tableAlias._relationalElement();
        switch (milestoningPropertyName)
        {
            case "from":
            {
                return getBusinessMilestoning(table)._from();
            }
            case "thru":
            {
                return getBusinessMilestoning(table)._thru();
            }
            case "in":
            {
                return getProcessingMilestoning(table)._in();
            }
            case "out":
            {
                return getProcessingMilestoning(table)._out();
            }
            default:
            {
                return null;
            }
        }
    }

    private static BusinessMilestoning getBusinessMilestoning(Table table)
    {
        return (BusinessMilestoning) table._milestoning().detect(m -> m instanceof BusinessMilestoning);
    }

    private static ProcessingMilestoning getProcessingMilestoning(Table table)
    {
        return (ProcessingMilestoning) table._milestoning().detect(m -> m instanceof ProcessingMilestoning);
    }
}
