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
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m2.relational.M2RelationalPaths;
import org.finos.legend.pure.m2.relational.M2RelationalProperties;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMappingAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.ModelElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.EmbeddedRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.GroupByMapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.AliasAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElementWithJoin;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.JoinTreeNode;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.NamedRelation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Relation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.View;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

import java.util.function.Consumer;

public class RelationalInstanceSetImplementationProcessor extends Processor<RootRelationalInstanceSetImplementation>
{
    @Deprecated
    public static final Function<TableAlias, RelationalOperationElement> TABLE_ALIAS_TO_RELATIONAL_OPERATION_ELEMENT_FN = AliasAccessor::_relationalElement;

    @Override
    public String getClassName()
    {
        return M2RelationalPaths.RootRelationalInstanceSetImplementation;
    }

    @Override
    public void process(RootRelationalInstanceSetImplementation implementation, ProcessorState state, Matcher matcher, final ModelRepository repository, final Context context, final ProcessorSupport processorSupport)
    {
        // No need to cross-reference... ClassMapping is responsible for that
        ModelElement cls = (ModelElement) ImportStub.withImportStubByPass(implementation._classCoreInstance(), processorSupport);

        GenericType instanceGenericType = cls._classifierGenericType() == null ? (GenericType) Type.wrapGenericType(processorSupport.getClassifier(cls), processorSupport) : cls._classifierGenericType();

        CoreInstance propertyReturnGenericType = org.finos.legend.pure.m3.navigation.generictype.GenericType.resolvePropertyReturnType((implementation._classifierGenericType() == null ? (GenericType) Type.wrapGenericType(processorSupport.getClassifier(implementation), processorSupport) : implementation._classifierGenericType()), implementation.getKeyByName(M3Properties._class), processorSupport);

        if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.subTypeOf(instanceGenericType, propertyReturnGenericType, processorSupport))
        {
            throw new PureCompilationException(implementation.getSourceInformation(), "Trying to map an unsupported type in Relational: Type Error: '" + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(instanceGenericType, processorSupport) + "' not a subtype of '" + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(propertyReturnGenericType, processorSupport) + "'");
        }

        RichIterable<? extends PropertyMapping> propertyMappings = implementation._propertyMappings();

        MutableSet<TableAlias> tableAliases = RelationalPropertyMappingProcessor.processRelationalPropertyMappings(propertyMappings, implementation, implementation, implementation._id(), matcher, state, repository, processorSupport);

        if (implementation._id().equals(implementation._superSetImplementationId()))
        {
            throw new PureCompilationException(implementation.getSourceInformation(), "Extend mapping id cannot reference self '" + implementation._id() + "'");
        }

        TableAlias mainTableAlias;
        TableAlias userDefinedMainTable = implementation._mainTableAlias();
        if (userDefinedMainTable == null)
        {
            MutableSet<RelationalOperationElement> tables = tableAliases.collect(AliasAccessor::_relationalElement);
            MutableSet<Database> databases = tableAliases.collect(tableAlias -> (Database) ImportStub.withImportStubByPass(tableAlias._databaseCoreInstance(), processorSupport));

            if (implementation._superSetImplementationId() != null)
            {
                RootRelationalInstanceSetImplementation superImplementation = getSuperMapping(implementation, processorSupport);
                PostProcessor.processElement(matcher, superImplementation, state, processorSupport);
                collectTableAndDatabaseFromSuperImplementation(superImplementation, implementation, tables, databases, processorSupport);
            }
            if (tables.size() != 1)
            {
                throw new PureCompilationException(implementation.getSourceInformation(), "Can't find the main table for class '" + cls._name() + "'. Please specify a main table using the ~mainTable directive.");
            }
            if (databases.size() != 1)
            {
                throw new PureCompilationException(implementation.getSourceInformation(), "Can't find the main table for class '" + cls._name() + "'. Inconsistent database definitions for the mapping");
            }

            Database database = databases.getAny();
            PostProcessor.processElement(matcher, database, state, processorSupport);

            mainTableAlias = (TableAlias) processorSupport.newAnonymousCoreInstance(null, M2RelationalPaths.TableAlias);
            mainTableAlias._name("");
            mainTableAlias._relationalElement(tables.getAny());
            mainTableAlias._databaseCoreInstance(database);
            implementation._mainTableAlias(mainTableAlias);
        }
        else if (implementation._superSetImplementationId() == null)
        {
            mainTableAlias = userDefinedMainTable;
            Database database = (Database) ImportStub.withImportStubByPass(mainTableAlias._databaseCoreInstance(), processorSupport);
            PostProcessor.processElement(matcher, database, state, processorSupport);
            NamedRelation table = (NamedRelation) DatabaseProcessor.findTableForAlias(database, mainTableAlias, processorSupport);
            mainTableAlias._relationalElement(table);
            mainTableAlias._setMappingOwner(implementation);
        }
        else
        {
            throw new PureCompilationException(implementation.getSourceInformation(), "Cannot specify main table explicitly for extended mapping [" + implementation._id() + "]");
        }

        RelationalOperationElement mainTable = mainTableAlias._relationalElement();
        for (JoinTreeNode joinTreeNode : RelationalPropertyMappingProcessor.collectJoinTreeNodes(propertyMappings))
        {
            RelationalOperationElementProcessor.processAliasForJoinTreeNode(joinTreeNode, mainTable, processorSupport);
        }

        RelationalMappingSpecificationProcessing.processFilterMapping(implementation, implementation, mainTable, matcher, state, repository, processorSupport);

        GroupByMapping groupByMapping = RelationalMappingSpecificationProcessing.processGroupByMapping(implementation, implementation, state, matcher, repository, processorSupport).getOne();

        Boolean distinct = implementation._distinct();
        if (groupByMapping != null)
        {
            implementation._primaryKey(groupByMapping._columns());
        }
        else if (distinct)
        {
            RichIterable<RelationalOperationElement> pks = propertyMappingsToRelationalOperationElements(propertyMappings);
            RichIterable<RelationalOperationElement> pksWithDistinctColumns = pks.groupBy(roe -> (roe instanceof TableAliasColumn) ? ((TableAliasColumn) roe)._column() : roe).keyMultiValuePairsView().collect(pair -> pair.getTwo().getFirst());

            implementation._primaryKey(pksWithDistinctColumns.select(pk -> !(pk instanceof RelationalOperationElementWithJoin) || ((RelationalOperationElementWithJoin) pk)._relationalOperationElement() != null));
        }
        else if (implementation._primaryKey().isEmpty())
        {
            Relation relation = (Relation) mainTableAlias._relationalElement();
            final TableAlias finalMainTable = mainTableAlias;
            DatabaseProcessor.processTable(relation, matcher, state, processorSupport);
            RichIterable<? extends Column> columns;
            if (relation instanceof Table)
            {
                columns = ((Table) relation)._primaryKey();
            }
            else if (relation instanceof View)
            {
                columns = ((View) relation)._primaryKey();
            }
            else
            {
                columns = Lists.mutable.empty();
            }
            RichIterable<TableAliasColumn> primaryKey = columns.collect(column ->
            {
                TableAliasColumn tableAliasColumn = (TableAliasColumn) repository.newEphemeralAnonymousCoreInstance(null, processorSupport.package_getByUserPath(M2RelationalPaths.TableAliasColumn));
                tableAliasColumn._column(column);
                tableAliasColumn._alias(finalMainTable);
                return tableAliasColumn;
            });
            implementation._primaryKey(primaryKey);
        }
//        else if (Instance.getValueForMetaPropertyToManyResolved(implementation, M2RelationalProperties.primaryKey, context, processorSupport).isEmpty())
//        {
////            throw new PureCompilationException(implementation.getSourceInformation(), "Please provide a primaryKey");
//        }
        else
        {
            processUserDefinedPrimaryKey(implementation, implementation, matcher, state, repository, processorSupport);
        }

        implementation._stores(implementation._propertyMappings().collect(PropertyMappingAccessor::_store, Sets.mutable.empty()).with(implementation._mainTableAlias()._database()).without(null));
        MilestoningPropertyMappingProcessor.processMilestoningPropertyMapping(implementation, implementation, processorSupport);
    }

    @Override
    public void populateReferenceUsages(RootRelationalInstanceSetImplementation implementation, ModelRepository repository, ProcessorSupport processorSupport)
    {
        RichIterable<? extends PropertyMapping> propertyMappings = implementation._propertyMappings();
        RelationalPropertyMappingProcessor.populateReferenceUsagesForRelationalPropertyMappings(propertyMappings, repository, processorSupport);

        TableAlias mainTableAlias = implementation._mainTableAlias();
        if (isMainTableAliasUserDefined(mainTableAlias))
        {
            addReferenceUsageForToOneProperty(mainTableAlias, mainTableAlias._databaseCoreInstance(), M2RelationalProperties.database, repository, processorSupport, mainTableAlias._databaseCoreInstance().getSourceInformation());
        }

        RelationalMappingSpecificationProcessing.populateFilterMappingReferenceUsages(implementation, repository, processorSupport);
        RelationalMappingSpecificationProcessing.populateGroupByMappingReferenceUsages(implementation, repository, processorSupport);

        if (implementation._userDefinedPrimaryKey())
        {
            populateReferenceUsagesForUserDefinedPrimaryKey(implementation, repository, processorSupport);
        }
    }

    public static void processUserDefinedPrimaryKey(RelationalInstanceSetImplementation implementation, CoreInstance mappingOwner, Matcher matcher, ProcessorState state, ModelRepository repository, ProcessorSupport processorSupport) throws PureCompilationException
    {
        RichIterable<? extends RelationalOperationElement> primaryKeys = implementation._primaryKey();
        for (RelationalOperationElement primaryKey : primaryKeys)
        {
            RelationalOperationElementProcessor.processColumnExpr(primaryKey, implementation, mappingOwner, Sets.mutable.empty(), matcher, state, repository, processorSupport);
        }
        // TODO figure out why we are setting the property values back to the same list
        implementation._primaryKey(primaryKeys);
    }

    public static void populateReferenceUsagesForUserDefinedPrimaryKey(CoreInstance implementation, ModelRepository repository, ProcessorSupport processorSupport)
    {
        if (implementation instanceof RelationalInstanceSetImplementation)
        {
            for (RelationalOperationElement primaryKey : ((RelationalInstanceSetImplementation) implementation)._primaryKey())
            {
                RelationalOperationElementProcessor.populateColumnExpressionReferenceUsages(primaryKey, repository, processorSupport);
            }
        }
    }

    public static boolean isMainTableAliasUserDefined(TableAlias mainTableAlias)
    {
        // TODO find a better way to do this
        return !"".equals(mainTableAlias._name());
    }

    private static RootRelationalInstanceSetImplementation getSuperMapping(RootRelationalInstanceSetImplementation implementation, ProcessorSupport processorSupport)
    {
        Mapping parentMapping = (Mapping) ImportStub.withImportStubByPass(implementation._parentCoreInstance(), processorSupport);
        SetImplementation superMapping = org.finos.legend.pure.m2.dsl.mapping.Mapping.getClassMappingById(parentMapping, implementation._superSetImplementationId(), processorSupport);
        if (superMapping instanceof RootRelationalInstanceSetImplementation)
        {
            return (RootRelationalInstanceSetImplementation) superMapping;
        }
        throw new PureCompilationException(implementation.getSourceInformation(), "Invalid superMapping for mapping [" + implementation._id() + "]");
    }

    private static void collectTableAndDatabaseFromSuperImplementation(RootRelationalInstanceSetImplementation superImplementation, RootRelationalInstanceSetImplementation implementation, MutableSet<RelationalOperationElement> tables, MutableSet<Database> databases, ProcessorSupport processorSupport)
    {
        RelationalOperationElement superImplementationMainTable = superImplementation._mainTableAlias()._relationalElement();
        tables.add(superImplementationMainTable);

        Database superImplementationDatabase = (Database) ImportStub.withImportStubByPass(superImplementation._mainTableAlias()._databaseCoreInstance(), processorSupport);
        Database superImplementationDatabaseAfterSubstitutions = DatabaseSubstitutionHandler.getDatabaseAfterStoreSubstitution(implementation, superImplementation, superImplementationDatabase);
        databases.add(superImplementationDatabaseAfterSubstitutions);
    }

    private static RichIterable<RelationalOperationElement> propertyMappingsToRelationalOperationElements(Iterable<? extends PropertyMapping> propertyMappings)
    {
        MutableList<RelationalOperationElement> result = Lists.mutable.empty();
        propertyMappings.forEach(pm -> forEachRelationalOperationElement(pm, result::add));
        return result;
    }

    private static void forEachRelationalOperationElement(PropertyMapping propertyMapping, Consumer<? super RelationalOperationElement> consumer)
    {
        if (propertyMapping instanceof RelationalPropertyMapping)
        {
            consumer.accept(((RelationalPropertyMapping) propertyMapping)._relationalOperationElement());
        }
        else if (propertyMapping instanceof EmbeddedRelationalInstanceSetImplementation)
        {
            ((EmbeddedRelationalInstanceSetImplementation) propertyMapping)._propertyMappings().forEach(pm -> forEachRelationalOperationElement(pm, consumer));
        }
    }
}
