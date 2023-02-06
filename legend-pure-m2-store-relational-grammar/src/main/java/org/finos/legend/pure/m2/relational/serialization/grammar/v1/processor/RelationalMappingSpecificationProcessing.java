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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m2.relational.M2RelationalProperties;
import org.finos.legend.pure.m3.compiler.ReferenceUsage;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.FilterMapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.GroupByMapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Filter;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalMappingSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.JoinTreeNode;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

class RelationalMappingSpecificationProcessing
{
    static FilterMapping processFilterMapping(RelationalMappingSpecification implementation, RelationalInstanceSetImplementation filterMappingOwner, RelationalOperationElement mainTable, Matcher matcher, ProcessorState state, ModelRepository repository, ProcessorSupport processorSupport)
    {
        FilterMapping filterMapping = implementation != null ? implementation._filter() : null;
        if (filterMapping != null)
        {
            Database database = (Database)ImportStub.withImportStubByPass(filterMapping._databaseCoreInstance(), processorSupport);
            if (database == null)
            {
                throw new PureCompilationException(filterMapping.getSourceInformation(), "The system can't figure out which database to use.");
            }

            JoinTreeNode joinTreeNode = filterMapping._joinTreeNode();
            if (joinTreeNode != null)
            {
                RelationalOperationElementProcessor.processJoinTreeNode(joinTreeNode, implementation, matcher, state, repository, processorSupport);
                RelationalOperationElementProcessor.processAliasForJoinTreeNode(joinTreeNode, mainTable, processorSupport);
            }

            String filterName = filterMapping._filterName();
            Filter foundFilter = (Filter) DatabaseProcessor.findFilter(database, filterName, processorSupport);
            if (foundFilter == null)
            {
                throw new PureCompilationException(filterMapping.getSourceInformation(), "The filter '" + filterName + "' has not been found.");
            }
            filterMapping._filter(foundFilter);

            RelationalInstanceSetImplementation owner = filterMapping._setMappingOwner();
            if (owner == null && filterMappingOwner != null)
            {
                filterMapping._setMappingOwner(filterMappingOwner);
            }
        }
        return filterMapping;
    }

    static void populateFilterMappingReferenceUsages(RelationalMappingSpecification implementation, ModelRepository repository, ProcessorSupport processorSupport)
    {
        FilterMapping filterMapping = implementation != null ? implementation._filter() : null;
        if (filterMapping != null)
        {
            Database database = (Database)ImportStub.withImportStubByPass(filterMapping._databaseCoreInstance(), processorSupport);
            ReferenceUsage.addReferenceUsage(database, filterMapping, M2RelationalProperties.database, 0, repository, processorSupport, filterMapping._databaseCoreInstance().getSourceInformation());

            JoinTreeNode joinTreeNode = filterMapping._joinTreeNode();
            if (joinTreeNode != null)
            {
                RelationalOperationElementProcessor.populateJoinTreeNodeReferenceUsages(joinTreeNode, repository, processorSupport);
            }
        }
    }

    static Pair<GroupByMapping, MutableSet<TableAlias>> processGroupByMapping(RelationalMappingSpecification implementation, CoreInstance mappingOwner, ProcessorState state, Matcher matcher, ModelRepository repository, ProcessorSupport processorSupport)
    {
        GroupByMapping groupByMapping = implementation != null ? implementation._groupBy() : null;
        MutableSet<TableAlias> groupByTableAliases = Sets.mutable.empty();
        if (groupByMapping != null)
        {
            RichIterable<? extends RelationalOperationElement> columns = groupByMapping._columns();
            for (RelationalOperationElement impl : columns)
            {
                RelationalOperationElementProcessor.processColumnExpr(impl, implementation, mappingOwner, groupByTableAliases, matcher, state, repository, processorSupport);
            }
        }
        return Tuples.pair(groupByMapping, groupByTableAliases);
    }

    static void populateGroupByMappingReferenceUsages(RelationalMappingSpecification implementation, ModelRepository repository, ProcessorSupport processorSupport)
    {
        GroupByMapping groupByMapping = implementation != null ? implementation._groupBy() : null;
        if (groupByMapping != null)
        {
            RichIterable<? extends RelationalOperationElement> columnExpressions = groupByMapping._columns();
            for (RelationalOperationElement columnExpression : columnExpressions)
            {
                RelationalOperationElementProcessor.populateColumnExpressionReferenceUsages(columnExpression, repository, processorSupport);
            }
        }
    }
}

