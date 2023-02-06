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

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.processor.RelationalInstanceSetImplementationProcessor;
import org.finos.legend.pure.m3.compiler.unload.unbind.Shared;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.FilterMapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.GroupByMapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalMappingSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

class RelationalMappingSpecificationUnbind
{
    private RelationalMappingSpecificationUnbind()
    {
    }

    static void cleanRelationalMappingSpecification(RelationalMappingSpecification relationalMappingSpecification, ModelRepository repository, ProcessorSupport processorSupport)
    {
        cleanMainTableAlias(relationalMappingSpecification, processorSupport);
        cleanFilter(relationalMappingSpecification, repository, processorSupport);
        cleanGroupBy(relationalMappingSpecification, repository, processorSupport);
    }

    private static void cleanMainTableAlias(RelationalMappingSpecification relationalMappingSpecification, ProcessorSupport processorSupport)
    {
        TableAlias mainTableAlias = relationalMappingSpecification._mainTableAlias();
        if (mainTableAlias != null)
        {
            if (RelationalInstanceSetImplementationProcessor.isMainTableAliasUserDefined(mainTableAlias))
            {
                ImportStub mainTableAliasDB = (ImportStub)mainTableAlias._databaseCoreInstance();
                Shared.cleanUpReferenceUsage(mainTableAliasDB, mainTableAlias, processorSupport);
                Shared.cleanImportStub(mainTableAliasDB, processorSupport);
                mainTableAlias._relationalElementRemove();
                mainTableAlias._setMappingOwnerRemove();
            }
            else
            {
                relationalMappingSpecification._mainTableAliasRemove();
            }
        }
    }

    private static void cleanFilter(RelationalMappingSpecification relationalMappingSpecification, ModelRepository repository, ProcessorSupport processorSupport) throws PureCompilationException
    {
        FilterMapping filterMapping = relationalMappingSpecification._filter();
        if (filterMapping != null)
        {
            Shared.cleanUpReferenceUsage(filterMapping._databaseCoreInstance(), filterMapping, processorSupport);
            Shared.cleanImportStub(filterMapping._databaseCoreInstance(), processorSupport);
            filterMapping._filterRemove();
            RelationalOperationElementUnbind.cleanJoinTreeNode(filterMapping._joinTreeNode(), repository, processorSupport);
        }
    }

    private static void cleanGroupBy(RelationalMappingSpecification relationalMappingSpecification, ModelRepository repository, ProcessorSupport processorSupport) throws PureCompilationException
    {
        GroupByMapping groupByMapping = relationalMappingSpecification._groupBy();
        if (groupByMapping != null)
        {
            RichIterable<? extends RelationalOperationElement> columns = groupByMapping._columns();
            for (RelationalOperationElement column : columns)
            {
                RelationalOperationElementUnbind.cleanNode(column, repository, processorSupport);
            }
        }
    }
}
