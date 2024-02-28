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

package org.finos.legend.pure.m2.relational.serialization.grammar.v1.unloader;

import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.compiler.unload.unbind.Shared;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.EmbeddedRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Literal;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;

class MilestoningPropertyMappingUnbind
{
    private MilestoningPropertyMappingUnbind()
    {
    }

    static void unbindMilestoningPropertyMapping(RelationalInstanceSetImplementation relationalInstance, ProcessorSupport processorSupport)
    {
        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation)relationalInstance._propertyMappings().detectWith(MILESTONING_PROPERTY_MAPPING, processorSupport);
        if (milestoningEmbeddedRelationalInstance != null)
        {
            relationalInstance._propertyMappingsRemove(milestoningEmbeddedRelationalInstance);
            unbindMilestoningEmbeddedRelationalInstance(milestoningEmbeddedRelationalInstance, processorSupport);
        }
    }

    private static void unbindMilestoningEmbeddedRelationalInstance(EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance, ProcessorSupport processorSupport)
    {
        ImmutableList<RelationalPropertyMapping> relationalPropertyMappings = (ImmutableList<RelationalPropertyMapping>)milestoningEmbeddedRelationalInstance._propertyMappings();
        milestoningEmbeddedRelationalInstance._rootRemove();
        milestoningEmbeddedRelationalInstance._idRemove();
        milestoningEmbeddedRelationalInstance._sourceSetImplementationIdRemove();
        milestoningEmbeddedRelationalInstance._targetSetImplementationIdRemove();
        milestoningEmbeddedRelationalInstance._classRemove();
        milestoningEmbeddedRelationalInstance._propertyRemove();
        milestoningEmbeddedRelationalInstance._parentRemove();
        milestoningEmbeddedRelationalInstance._ownerRemove();
        milestoningEmbeddedRelationalInstance._setMappingOwnerRemove();
        milestoningEmbeddedRelationalInstance._propertyMappings(Lists.immutable.<PropertyMapping>empty());
        relationalPropertyMappings.forEachWith(UNBIND_RELATIONAL_PROPERTY_MAPPING, processorSupport);
    }

    private static final Procedure2<RelationalPropertyMapping, ProcessorSupport> UNBIND_RELATIONAL_PROPERTY_MAPPING = new Procedure2<RelationalPropertyMapping, ProcessorSupport>()
    {
        @Override
        public void value(RelationalPropertyMapping relationalPropertyMapping, ProcessorSupport processorSupport)
        {
            unbindRelationalPropertyMappings(relationalPropertyMapping, processorSupport);
        }
    };

    private static void unbindRelationalPropertyMappings(RelationalPropertyMapping relationalPropertyMapping, ProcessorSupport processorSupport)
    {
        RelationalOperationElement relationalOperationElement = relationalPropertyMapping._relationalOperationElement();
        relationalPropertyMapping._targetSetImplementationIdRemove();
        relationalPropertyMapping._sourceSetImplementationIdRemove();
        relationalPropertyMapping._ownerRemove();
        relationalPropertyMapping._propertyRemove();
        relationalPropertyMapping._localMappingPropertyRemove();
        relationalPropertyMapping._relationalOperationElementRemove();
        if (relationalOperationElement instanceof Literal)
        {
            unbindLiteral((Literal)relationalOperationElement);
        }
        else
        {
            unbindTableAliasColumn((TableAliasColumn)relationalOperationElement, processorSupport);
        }
    }

    private static void unbindLiteral(Literal literal)
    {
        literal._valueRemove();
    }

    private static void unbindTableAliasColumn(TableAliasColumn tableAliasColumn, ProcessorSupport processorSupport)
    {
        TableAlias tableAlias = tableAliasColumn._alias();
        tableAliasColumn._setMappingOwnerRemove();
        tableAliasColumn._columnRemove();
        tableAliasColumn._columnNameRemove();
        tableAliasColumn._aliasRemove();
        Shared.cleanUpReferenceUsage(tableAlias._database(), tableAliasColumn, processorSupport);
        unbindTableAlias(tableAlias);
    }

    private static void unbindTableAlias(TableAlias tableAlias)
    {
        tableAlias._databaseRemove();
        tableAlias._relationalElementRemove();
        tableAlias._nameRemove();
    }

    private static final Predicate2<PropertyMapping, ProcessorSupport> MILESTONING_PROPERTY_MAPPING = new Predicate2<PropertyMapping, ProcessorSupport>()
    {
        @Override
        public boolean accept(PropertyMapping propertyMapping, ProcessorSupport processorSupport)
        {
            return MilestoningFunctions.isAutoGeneratedMilestoningNamedDateProperty(propertyMapping._property(), processorSupport);
        }
    };
}
