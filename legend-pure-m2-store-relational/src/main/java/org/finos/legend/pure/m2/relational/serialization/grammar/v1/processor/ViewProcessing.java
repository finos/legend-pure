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
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.finos.legend.pure.m2.relational.M2RelationalPaths;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.TreeNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.ColumnMapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.DynaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElementWithJoin;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Schema;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.datatype.DataType;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.Join;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.JoinTreeNode;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.View;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

import static org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.GET_NAME_VALUE_WITH_USER_PATH;

public class ViewProcessing extends RelationalMappingSpecificationProcessing
{
    private static final Function<TreeNode, Iterable<Join>> TREE_NODE_TO_JOIN_FUNCTION = new Function<TreeNode, Iterable<Join>>()
    {
        @Override
        public ListIterable<Join> valueOf(TreeNode coreInstance)
        {
            return getAllJoins((JoinTreeNode)coreInstance);
        }
    };

    private static final Procedure2<Column, MapIterable<String, DataType>> SET_COLUMN_TYPE_PROCEDURE = new Procedure2<Column, MapIterable<String, DataType>>()
    {
        @Override
        public void value(Column column, MapIterable<String, DataType> dataTypes)
        {
            String columnName = column._name();
            DataType colMappingType = dataTypes.get(columnName);
            if (colMappingType != null)
            {
                column._type(colMappingType);
            }
        }
    };

    static void processViewsInSchema(Database db, Schema schema, Matcher matcher, ProcessorState processorState, ModelRepository repository, ProcessorSupport processorSupport)
    {
        SetIterable<Database> dbsInHierarchy = org.finos.legend.pure.m2.relational.Database.getAllIncludedDBs(db, processorSupport);
        RichIterable<? extends View> views = schema._views();
        for (View view : views)
        {
            view._schema(schema);
            processView(dbsInHierarchy, view, matcher, processorState, repository, processorSupport);
        }
    }

    static void populateReferenceUsagesForViewsInSchema(Schema schema, ModelRepository repository, ProcessorSupport processorSupport)
    {
        RichIterable<? extends View> views = schema._views();
        for (View view : views)
        {
            populateReferenceUsagesForView(view, repository, processorSupport);
        }
    }

    private static void processView(SetIterable<Database> dbsInHierarchy, View view, Matcher matcher, ProcessorState processorState, ModelRepository repository, ProcessorSupport processorSupport)
    {
        DatabaseProcessor.processColumnsForTableOrView(view);

        RichIterable<? extends ColumnMapping> columnMappings = view._columnMappings();
        MutableMap<String, DataType> colMappingTypeByName = UnifiedMap.newMap(columnMappings.size());
        MutableSet<RelationalOperationElement> columnMappingRootTables = UnifiedSet.newSet();

        for (ColumnMapping columnMapping : columnMappings)
        {
            RelationalOperationElement columnMappingRelationalElement = columnMapping._relationalOperationElement();
            String columnMappingColumnName = columnMapping._columnName();
            RelationalOperationElementProcessor.processColumnExpr(columnMappingRelationalElement, view, null, Sets.mutable.<TableAlias>empty(), matcher, processorState, repository, processorSupport);

            RelationalOperationElement columnMappingRootTable = findMainTable(dbsInHierarchy, view, processorSupport, columnMappingRelationalElement);
            if (columnMappingRootTable != null)
            {
                columnMappingRootTables.add(columnMappingRootTable);
            }

            colMappingTypeByName.put(columnMappingColumnName, getColumnType(columnMappingRelationalElement, processorSupport, repository));
        }

        RelationalOperationElement mainTable = identifyMainTable(view, columnMappingRootTables);
        processDynaFunctionAliases(view, mainTable, processorSupport);
        processFilterMapping(view, null, mainTable, matcher, processorState, repository, processorSupport);
        processGroupByMapping(view, matcher, processorState, repository, mainTable, processorSupport);
        processPrimaryKeys(view, colMappingTypeByName, repository, processorSupport);
        setViewColumnsType(view, colMappingTypeByName, processorSupport);
        setViewMainTableAlias(view, mainTable, processorSupport);
    }

    private static void processDynaFunctionAliases(View view, final RelationalOperationElement mainTable, final ProcessorSupport processorSupport)
    {
        RichIterable<? extends ColumnMapping> columnMappings = view._columnMappings();
        columnMappings.forEach(new Procedure<ColumnMapping>()
        {
            @Override
            public void value(ColumnMapping coreInstance)
            {
                RelationalOperationElement roe = coreInstance._relationalOperationElement();
                MutableList<JoinTreeNode> joinTreeNodes = Lists.mutable.empty();
                RelationalOperationElementProcessor.collectJoinTreeNodes(joinTreeNodes, roe);
                for (JoinTreeNode joinTreeNode : joinTreeNodes)
                {
                    RelationalOperationElementProcessor.processAliasForJoinTreeNode(joinTreeNode, mainTable, processorSupport);
                }
            }
        });
    }

    private static void processGroupByMapping(View view, Matcher matcher, ProcessorState processorState, ModelRepository repository, CoreInstance mainTable, ProcessorSupport processorSupport)
    {
        MutableSet<TableAlias> groupByTableAliases = processGroupByMapping(view, null, processorState, matcher, repository, processorSupport).getTwo();
        SetIterable<RelationalOperationElement> groupByTables = groupByTableAliases.collect(RelationalInstanceSetImplementationProcessor.TABLE_ALIAS_TO_RELATIONAL_OPERATION_ELEMENT_FN).toSet();
        for (RelationalOperationElement groupByTable : groupByTables)
        {
            if (groupByTable != mainTable)
            {
                throw new PureCompilationException(view.getSourceInformation(), "View: " + GET_NAME_VALUE_WITH_USER_PATH.valueOf(view) + " has a groupBy which refers to table: '" + GET_NAME_VALUE_WITH_USER_PATH.valueOf(groupByTable) + "' which is not the mainTable: '" + GET_NAME_VALUE_WITH_USER_PATH.valueOf(mainTable) + "'");
            }
        }
    }

    private static RelationalOperationElement findMainTable(SetIterable<Database> dbsInHierarchy, View view, ProcessorSupport processorSupport, RelationalOperationElement impl)
    {
        ImmutableList<RelationalOperationElement> colMappingTablesRootFirst = findAllTablesRootFirst(impl);
        validateColumnReferencesOnlyReferToOneDB(view, dbsInHierarchy, colMappingTablesRootFirst, processorSupport);
        return colMappingTablesRootFirst.toList().getFirst();
    }

    private static void setViewMainTableAlias(View view, RelationalOperationElement mainTable, ProcessorSupport processorSupport)
    {
        TableAlias mainTableAlias = buildTableAlias(mainTable, processorSupport);
        view._mainTableAlias(mainTableAlias);
    }

    private static TableAlias buildTableAlias(RelationalOperationElement mainTable, ProcessorSupport processorSupport)
    {
        TableAlias mainTableAlias = (TableAlias)processorSupport.newAnonymousCoreInstance(null, M2RelationalPaths.TableAlias);
        mainTableAlias._name("");
        mainTableAlias._relationalElement(mainTable);
        return mainTableAlias;
    }

    private static RelationalOperationElement identifyMainTable(View view, SetIterable<RelationalOperationElement> mainTables)
    {
        if (mainTables.isEmpty())
        {
            throw new PureCompilationException(view.getSourceInformation(), "Unable to determine mainTable for View: " + GET_NAME_VALUE_WITH_USER_PATH.valueOf(view));
        }
        if (mainTables.size() > 1)
        {
            MutableList<String> tableNames = mainTables.collect(GET_NAME_VALUE_WITH_USER_PATH, FastList.<String>newList(mainTables.size())).sortThis();
            throw new PureCompilationException(view.getSourceInformation(), "View: " + GET_NAME_VALUE_WITH_USER_PATH.valueOf(view) + " contains multiple main tables: " + tableNames.makeString("[", ",", "]") + " there should be only one root Table for Views");
        }
        return mainTables.toList().getFirst();
    }

    private static void validateColumnReferencesOnlyReferToOneDB(View view, SetIterable<Database> dbsInHierarchy, ListIterable<RelationalOperationElement> allTables, ProcessorSupport processorSupport)
    {
        Database viewDB = view._schema() == null ? null : (Database)ImportStub.withImportStubByPass(view._schema()._databaseCoreInstance(), processorSupport);

        for (RelationalOperationElement table : allTables)
        {
            Database database = null;
            if (table instanceof Table)
            {
                database = ((Table)table)._schema() == null ? null : (Database)ImportStub.withImportStubByPass(((Table)table)._schema()._databaseCoreInstance(), processorSupport);
            }
            else if(table instanceof View)
            {
                database = ((View)table)._schema() == null ? null : (Database)ImportStub.withImportStubByPass(((View)table)._schema()._databaseCoreInstance(), processorSupport);
            }
            if (!dbsInHierarchy.contains(database))
            {
                throw new PureCompilationException(view.getSourceInformation(), "All tables referenced in View: " + GET_NAME_VALUE_WITH_USER_PATH.valueOf(view) + " should come from the View's owning or included DB: '" + GET_NAME_VALUE_WITH_USER_PATH.valueOf(viewDB) + "', table: '" + GET_NAME_VALUE_WITH_USER_PATH.valueOf(table) + "' does not");
            }
        }
    }

    private static void processPrimaryKeys(View view, MapIterable<String, DataType> colMappingTypeByName, ModelRepository repository, ProcessorSupport processorSupport)
    {
        RichIterable<? extends Column> primaryKeyCols = view._primaryKey();
        view._userDefinedPrimaryKey(primaryKeyCols.notEmpty());
        if (primaryKeyCols.notEmpty())
        {
            setColumnTypes(primaryKeyCols, colMappingTypeByName, processorSupport);
            for (Column col : primaryKeyCols)
            {
                col._owner(view);
            }
        }
    }

    private static void populateReferenceUsagesForView(View view, ModelRepository repository, ProcessorSupport processorSupport)
    {
        RichIterable<? extends ColumnMapping> columnMappings = view._columnMappings();
        for (ColumnMapping columnMapping : columnMappings)
        {
            RelationalOperationElement columnExpression = columnMapping._relationalOperationElement();
            RelationalOperationElementProcessor.populateColumnExpressionReferenceUsages(columnExpression, repository, processorSupport);
        }

        populateFilterMappingReferenceUsages(view, repository, processorSupport);
        populateGroupByMappingReferenceUsages(view, repository, processorSupport);
    }

    private static void setViewColumnsType(View view, MapIterable<String, DataType> colMappingTypeByName, ProcessorSupport processorSupport)
    {
        RichIterable<? extends Column> viewColumns = (RichIterable<? extends Column>)view._columns();
        setColumnTypes(viewColumns, colMappingTypeByName, processorSupport);
    }

    private static void setColumnTypes(RichIterable<? extends Column> sourceColumns, final MapIterable<String, DataType> colMappingTypeByName, final ProcessorSupport processorSupport)
    {
        sourceColumns.forEachWith(SET_COLUMN_TYPE_PROCEDURE, colMappingTypeByName);
    }

    private static DataType getColumnType(RelationalOperationElement relationalOperationElement, ProcessorSupport processorSupport, ModelRepository repository)
    {
        if (relationalOperationElement instanceof TableAliasColumn)
        {
            return ((TableAliasColumn)relationalOperationElement)._column() == null ? null : ((TableAliasColumn)relationalOperationElement)._column()._type();
        }
        if (relationalOperationElement  instanceof RelationalOperationElementWithJoin)
        {
            return getColumnType(((RelationalOperationElementWithJoin)relationalOperationElement)._relationalOperationElement(), processorSupport, repository);
        }
        if (relationalOperationElement instanceof  DynaFunction)
        {
            // type = Type.getTopType(processorSupport);
            return (DataType)repository.newAnonymousCoreInstance(null, processorSupport.package_getByUserPath("meta::relational::metamodel::datatype::DataType"));
        }
        throw new PureCompilationException(relationalOperationElement.getSourceInformation(), ((GenericType)(relationalOperationElement._classifierGenericType() == null ? Type.wrapGenericType(processorSupport.getClassifier(relationalOperationElement), processorSupport) : relationalOperationElement._classifierGenericType()))._rawTypeCoreInstance().getName() + " are not currently supported in Views");
    }

    private static ImmutableList<RelationalOperationElement> findAllTablesRootFirst(RelationalOperationElement relationalOperationElement)
    {
        MutableList<RelationalOperationElement> allTables = FastList.newList();
        if (relationalOperationElement instanceof TableAliasColumn)
        {
            allTables.add(((TableAliasColumn)relationalOperationElement)._alias() == null ? null : ((TableAliasColumn)relationalOperationElement)._alias()._relationalElement());
        }
        else if (relationalOperationElement instanceof RelationalOperationElementWithJoin)
        {
            RelationalOperationElement joinRelationalOperationElement = ((RelationalOperationElementWithJoin)relationalOperationElement)._relationalOperationElement();
            RelationalOperationElement targetTable = findAllTablesRootFirst(joinRelationalOperationElement).toList().getFirst();
            JoinTreeNode joinTreeNode = ((RelationalOperationElementWithJoin)relationalOperationElement)._joinTreeNode();
            allTables.addAllIterable(findAllJoinTreeNodeTablesRootFirst(joinTreeNode, targetTable));
        }
        else if (relationalOperationElement instanceof DynaFunction)
        {
            RichIterable<? extends RelationalOperationElement> params = ((DynaFunction)relationalOperationElement)._parameters();
            MutableList<RelationalOperationElement> tablesForParams = params.flatCollect(new Function<RelationalOperationElement, ImmutableList<RelationalOperationElement>>()
            {
                @Override
                public ImmutableList<RelationalOperationElement> valueOf(RelationalOperationElement coreInstance)
                {
                    return findAllTablesRootFirst(coreInstance);
                }
            }, Lists.mutable.<RelationalOperationElement>empty());
            if (tablesForParams.size() == 1)
            {
                //can't determine root from dyna function whose params refer to >1 table
                allTables.add(tablesForParams.toList().getFirst());
            }
        }
        return allTables.distinct().toImmutable();
    }

    private static ListIterable<RelationalOperationElement> findAllJoinTreeNodeTablesRootFirst(JoinTreeNode joinTreeNode, RelationalOperationElement targetTable)
    {
        ListIterable<Join> joins = getAllJoins(joinTreeNode);
        ListIterable<Join> joinsReversed = joins.toReversed();
        MutableList<RelationalOperationElement> allTables = FastList.newListWith(targetTable);
        for (Join join : joinsReversed)
        {
            ListIterable<? extends Pair> aliases = join._aliases().toList();
            ListIterable<RelationalOperationElement> others = aliases.collectWith(new Function2<Pair, RelationalOperationElement, RelationalOperationElement>()
            {
                @Override
                public RelationalOperationElement value(Pair aliasPair, RelationalOperationElement target)
                {
                    RelationalOperationElement first = aliasPair._first() == null ? null : ((TableAlias)aliasPair._first())._relationalElement();
                    RelationalOperationElement second = aliasPair._second() == null ? null : ((TableAlias)aliasPair._second())._relationalElement();
                    return (target != first) ? first : second;
                }
            }, targetTable);
            targetTable = others.getFirst();
            allTables.add(targetTable);
        }
        return allTables.toReversed();
    }

    private static ListIterable<Join> getAllJoins(JoinTreeNode joinTreeNode)
    {
        Join join = joinTreeNode._join();
        RichIterable<? extends TreeNode> children = joinTreeNode._childrenData();
        RichIterable<Join> childrenJoins = children.flatCollect(TREE_NODE_TO_JOIN_FUNCTION);
        return FastList.<Join>newList(childrenJoins.size() + 1).with(join).withAll(childrenJoins);
    }
}
