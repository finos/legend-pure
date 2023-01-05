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
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.Functions0;
import org.eclipse.collections.impl.block.factory.Predicates2;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m2.relational.M2RelationalPaths;
import org.finos.legend.pure.m2.relational.M2RelationalProperties;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.ReferenceUsage;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.DynaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Filter;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Schema;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.datatype.Date;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.Join;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.operation.BinaryOperation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.operation.Operation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.operation.UnaryOperation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.BusinessMilestoning;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.BusinessSnapshotMilestoning;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Milestoning;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.NamedRelation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.NamedRelationAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.ProcessingMilestoning;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Relation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecifications;
import org.finos.legend.pure.m4.exception.PureCompilationException;

import java.util.ArrayDeque;
import java.util.Queue;

public class DatabaseProcessor extends Processor<Database>
{
    public static final String DEFAULT_SCHEMA_NAME = "default";
    public static final String SELF_JOIN_TABLE_NAME = "{target}";

    public static final Predicate2<Schema, Object> SCHEMA_NAME_PREDICATE = Predicates2.attributeEqual(Schema::_name);
    public static final Predicate2<RelationalOperationElement, Object> COLUMN_NAME_PREDICATE = Predicates2.attributeEqual(c -> ((Column) c)._name());
    public static final Predicate2<NamedRelation, Object> NAMED_RELATION_NAME_PREDICATE = Predicates2.attributeEqual(NamedRelationAccessor::_name);

    @Override
    public String getClassName()
    {
        return M2RelationalPaths.Database;
    }

    @Override
    public void process(Database database, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        for (CoreInstance db : database._includesCoreInstance())
        {
            Database resolvedDb = (Database) ImportStub.withImportStubByPass(db, processorSupport);
            PostProcessor.processElement(matcher, resolvedDb, state, processorSupport);
        }

        RichIterable<? extends Schema> schemas = database._schemas();
        checkForDuplicatesByName(schemas, (Function<Schema, String>) Schema::_name, "database");

        for (Schema schema : database._schemas())
        {
            schema._databaseCoreInstance(database);
            RichIterable<? extends CoreInstance> tables = schema._tables();
            RichIterable<? extends CoreInstance> views = schema._views();
            checkForDuplicatesByName(LazyIterate.concatenate((Iterable<CoreInstance>) tables, (Iterable<CoreInstance>) views), (Function<NamedRelation, String>) NamedRelation::_name, DEFAULT_SCHEMA_NAME.equals(schema._name()) ? "database" : "schema");
            for (CoreInstance table : tables)
            {
                ((Table) table)._schema(schema);
                processTable(table, matcher, state, processorSupport);
            }
        }
        RichIterable<? extends Join> joins = database._joins();
        checkForDuplicatesByName(joins, (Function<Join, String>) Join::_name, "database");
        for (Join join : joins)
        {
            join._databaseCoreInstance(database);
            processJoin(join, database, repository, processorSupport);
        }

        RichIterable<? extends Filter> filters = database._filters();
        checkForDuplicatesByName(filters, (Function<Filter, String>) Filter::_name, "database");
        for (Filter filter : database._filters())
        {
            filter._databaseCoreInstance(database);
            this.processFilter(filter, database, processorSupport);
        }
        for (Schema schema : database._schemas())
        {
            ViewProcessing.processViewsInSchema(database, schema, matcher, state, repository, processorSupport);
            schema._relationsCoreInstance(schema._tables());
        }
        database._namespaces(database._schemas());
    }

    @Override
    public void populateReferenceUsages(Database database, ModelRepository repository, ProcessorSupport processorSupport)
    {
        this.addReferenceUsagesForToManyProperty(database, database._includesCoreInstance(), M2RelationalProperties.includes, repository, processorSupport, database._includesCoreInstance().collect(new Function<CoreInstance, SourceInformation>()
        {
            @Override
            public SourceInformation valueOf(CoreInstance coreInstance)
            {
                return coreInstance.getSourceInformation();
            }
        }));
        for (Schema schema : database._schemas())
        {
            ViewProcessing.populateReferenceUsagesForViewsInSchema(schema, repository, processorSupport);
        }
    }

    private static <T extends CoreInstance> void checkForDuplicatesByName(RichIterable<? extends CoreInstance> instances, Function<T, String> nameFn, String scope)
    {
        MutableMap<String, CoreInstance> instancesByName = UnifiedMap.newMap(instances.size());
        for (T instance : (RichIterable<T>) instances)
        {
            String name = nameFn.valueOf(instance);
            CoreInstance other = instancesByName.put(name, instance);
            if (other != null)
            {
                String message = "More than one " + instance.getClassifier().getName() +
                        " found with the name '" +
                        name +
                        "': " +
                        instance.getClassifier().getName() +
                        " names must be unique within a " +
                        scope;
                throw new PureCompilationException(instance.getSourceInformation(), message);
            }
        }
    }

    static void processTable(CoreInstance table, Matcher matcher, ProcessorState processorState, ProcessorSupport processorSupport)
    {
        MapIterable<String, Column> columnsByName = processColumnsForTableOrView(table);

        if (table instanceof Table)
        {
            MutableList<Column> primaryKey = (MutableList<Column>) ((Table) table)._primaryKey().toList();
            for (int i = 0; i < primaryKey.size(); i++)
            {
                CoreInstance key = primaryKey.get(i);
                if (processorSupport.instance_instanceOf(key, M3Paths.String))
                {
                    String columnName = key.getName();
                    Column column = columnsByName.get(columnName);
                    if (column == null)
                    {
                        throw new PureCompilationException(table.getSourceInformation(), "Could not find column " + columnName + " in table " + ((NamedRelation) table)._name());
                    }
                    primaryKey.set(i, column);
                }
            }
            ((Table) table)._primaryKey(primaryKey);
        }

        if (table instanceof Relation)
        {
            Relation relation = (Relation) table;
            relation._setColumnsCoreInstance(relation._columns().collect(relationalOperationElement -> (Column) relationalOperationElement));
        }
        processTableMilestoning(table, columnsByName, matcher, processorState, processorSupport);
    }

    public static MapIterable<String, Column> processColumnsForTableOrView(CoreInstance tableOrView)
    {
        if (tableOrView instanceof Relation)
        {
            ListIterable<? extends RelationalOperationElement> columns = ((Relation) tableOrView)._columns().toList();
            MutableMap<String, Column> columnsByName = UnifiedMap.newMap(columns.size());
            for (RelationalOperationElement column : columns)
            {
                if (!(column instanceof Column))
                {
                    throw new PureCompilationException(column.getSourceInformation(), "Expected an instance of " + M2RelationalPaths.Column + ", found " + PackageableElement.getUserPathForPackageableElement(column.getClassifier()));
                }
                String columnName = ((Column) column)._name();
                RelationalOperationElement old = columnsByName.put(columnName, (Column) column);
                if (old != null)
                {
                    throw new PureCompilationException(column.getSourceInformation(), "Multiple columns named '" + columnName + "' found in " +
                            (tableOrView instanceof NamedRelation
                                    ? (tableOrView.getClassifier().getName().toLowerCase() + " " + ((NamedRelation) tableOrView)._name())
                                    : "relation"));
                }
                ((Column) column)._owner((Relation) tableOrView);
            }
            return columnsByName;
        }
        return Maps.immutable.empty();
    }

    private static void processTableMilestoning(CoreInstance tableCoreInstance, MapIterable<String, Column> columnsByName, Matcher matcher, ProcessorState processorState, ProcessorSupport processorSupport)
    {
        if (tableCoreInstance instanceof Table)
        {
            Table table = (Table) tableCoreInstance;
            for (Milestoning milestoningInfo : table._milestoning())
            {
                milestoningInfo._owner(table);

                if (milestoningInfo instanceof BusinessMilestoning)
                {
                    BusinessMilestoning businessMilestoning = (BusinessMilestoning) milestoningInfo;

                    if (processorSupport.instance_instanceOf(businessMilestoning._from(), M3Paths.String))
                    {
                        String businessFrom = businessMilestoning._from() == null ? "" : businessMilestoning._from().getName();
                        Column businessFromColumn = getColumn(table, columnsByName, businessFrom);
                        businessMilestoning._from(businessFromColumn);
                    }

                    if (processorSupport.instance_instanceOf(businessMilestoning._thru(), M3Paths.String))
                    {
                        String businessThru = businessMilestoning._thru() == null ? "" : businessMilestoning._thru().getName();
                        Column businessThruColumn = getColumn(table, columnsByName, businessThru);
                        businessMilestoning._thru(businessThruColumn);
                    }
                }
                else if (milestoningInfo instanceof BusinessSnapshotMilestoning)
                {
                    BusinessSnapshotMilestoning businessSnapshotMilestoning = (BusinessSnapshotMilestoning) milestoningInfo;
                    if (processorSupport.instance_instanceOf(businessSnapshotMilestoning._snapshotDate(), M3Paths.String))
                    {
                        String snapshotDateColumnName = businessSnapshotMilestoning._snapshotDate().getName();
                        Column snapshotDateColumn = getColumn(table, columnsByName, snapshotDateColumnName);
                        if (!(snapshotDateColumn._type() instanceof Date))
                        {
                            throw new PureCompilationException(snapshotDateColumn.getSourceInformation(), "Column set as BUS_SNAPSHOT_DATE can only be of type : [Date]");
                        }
                        businessSnapshotMilestoning._snapshotDate(snapshotDateColumn);
                    }
                }
                else if (milestoningInfo instanceof ProcessingMilestoning)
                {
                    ProcessingMilestoning processingMilestoning = (ProcessingMilestoning) milestoningInfo;

                    if (processorSupport.instance_instanceOf(processingMilestoning._in(), M3Paths.String))
                    {
                        String processingIn = processingMilestoning._in() == null ? "" : processingMilestoning._in().getName();
                        Column processingInColumn = getColumn(table, columnsByName, processingIn);
                        processingMilestoning._in(processingInColumn);
                    }

                    if (processorSupport.instance_instanceOf(processingMilestoning._out(), M3Paths.String))
                    {
                        String processingOut = processingMilestoning._out() == null ? "" : processingMilestoning._out().getName();
                        Column processingOutColumn = getColumn(table, columnsByName, processingOut);
                        processingMilestoning._out(processingOutColumn);
                    }
                }
                else
                {
                    PostProcessor.processElement(matcher, milestoningInfo, processorState, processorSupport);
                    if (!milestoningInfo.hasBeenProcessed())
                    {
                        throw new PureCompilationException(milestoningInfo.getSourceInformation(), "Unknown milestoning type: " + PackageableElement.getUserPathForPackageableElement(milestoningInfo.getClassifier()));
                    }
                }
            }
        }
    }

    public static Column getColumn(Table table, MapIterable<String, Column> columnsByName, String columnName)
    {
        Column column = columnsByName.get(columnName);
        if (column == null)
        {
            String tableName = table._name();
            throw new PureCompilationException("Column: " + columnName + " not found in Table: " + tableName);
        }
        return column;
    }

    private void processFilter(Filter filter, Database defaultDb, ProcessorSupport processorSupport) throws PureCompilationException
    {
        MutableMap<String, MutableMap<String, CoreInstance>> tableByAlias = UnifiedMap.newMap();
        Operation operation = filter._operation();
        MutableList<TableAliasColumn> selfJoinTarget = FastList.newList();
        scanOperation(operation, tableByAlias, selfJoinTarget, defaultDb, null, processorSupport, false);
    }

    private static void processJoin(Join join, Database defaultDb, ModelRepository repository, ProcessorSupport processorSupport)
    {
        MutableMap<String, MutableMap<String, CoreInstance>> tableByAliasBySchema = Maps.mutable.empty();
        Operation operation = join._operation();
        MutableList<TableAliasColumn> selfJoinTargets = Lists.mutable.empty();
        scanOperation(operation, tableByAliasBySchema, selfJoinTargets, defaultDb, repository, processorSupport, true);

        Class<?> tableAliasClass = (Class<?>) processorSupport.package_getByUserPath(M2RelationalPaths.TableAlias);

        MutableList<TableAlias> tableAliases = Lists.mutable.empty();
        for (MutableMap<String, CoreInstance> schemaTableByAlias : tableByAliasBySchema)
        {
            schemaTableByAlias.forEachKeyValue((alias, table) ->
            {
                TableAlias tableAlias = (TableAlias) repository.newAnonymousCoreInstance(join.getSourceInformation(), tableAliasClass);
                tableAlias._name(repository.newStringCoreInstance_cached(alias).getName());
                tableAlias._relationalElement((RelationalOperationElement) table);
                tableAliases.add(tableAlias);
            });
        }

        if (tableAliases.size() > 2)
        {
            throw new PureCompilationException(join.getSourceInformation(), "A join can only contain 2 tables. Please use Join chains (using '>') in your mapping in order to compose many of them.");
        }
        if (tableAliases.size() == 1)
        {
            // Self Join
            if (selfJoinTargets.isEmpty())
            {
                throw new PureCompilationException(join.getSourceInformation(), "The system can only find one table in the join. Please use the '{target}' notation in order to define a directed self join.");
            }
            TableAlias existingAlias = tableAliases.get(0);
            String existingAliasName = existingAlias._name();
            RelationalOperationElement existingRelationalElement = existingAlias._relationalElement();

            TableAlias tableAlias = (TableAlias) repository.newAnonymousCoreInstance(existingAlias.getSourceInformation(), tableAliasClass);
            tableAlias._name(repository.newStringCoreInstance_cached("t_" + existingAliasName).getName());
            tableAlias._relationalElement(existingRelationalElement);
            tableAliases.add(tableAlias);

            join._target(tableAlias);

            for (TableAliasColumn selfJoinTarget : selfJoinTargets)
            {
                selfJoinTarget._alias(tableAlias);
                String columnName = selfJoinTarget._columnName();
                Column col = null;
                if (existingRelationalElement instanceof Relation)
                {
                    col = (Column) ((Relation) existingRelationalElement)._columns().selectWith(COLUMN_NAME_PREDICATE, columnName).toList().getFirst();
                }
                if (col == null)
                {
                    throw new PureCompilationException(selfJoinTarget.getSourceInformation(), "The column '" + columnName + "' can't be found in the table '" + ((NamedRelation) existingRelationalElement)._name() + "'");
                }
                selfJoinTarget._column(col);
            }
        }
        else if (selfJoinTargets.notEmpty())
        {
            throw new PureCompilationException(join.getSourceInformation(), "A self join can only contain 1 table, found " + selfJoinTargets.size());
        }

        // All Joins
        Pair<TableAlias, TableAlias> pair1 = newPair(tableAliasClass, tableAliasClass, repository, processorSupport);
        pair1._first(tableAliases.get(0));
        pair1._second(tableAliases.get(1));

        Pair<TableAlias, TableAlias> pair2 = newPair(tableAliasClass, tableAliasClass, repository, processorSupport);
        pair2._first(tableAliases.get(1));
        pair2._second(tableAliases.get(0));

        join._aliases(Lists.immutable.with(pair1, pair2));
    }

    private static void scanOperation(RelationalOperationElement element, MutableMap<String, MutableMap<String, CoreInstance>> tableByAliasBySchema, MutableList<TableAliasColumn> selfJoinTarget, Database defaultDb, ModelRepository repository, ProcessorSupport processorSupport, boolean isJoin)
    {
        if (element instanceof TableAliasColumn)
        {
            processTableAliasColumn((TableAliasColumn) element, tableByAliasBySchema, selfJoinTarget, defaultDb, repository, processorSupport, isJoin);
        }
        else if (element instanceof BinaryOperation)
        {
            scanOperation(((BinaryOperation) element)._left(), tableByAliasBySchema, selfJoinTarget, defaultDb, repository, processorSupport, isJoin);
            scanOperation(((BinaryOperation) element)._right(), tableByAliasBySchema, selfJoinTarget, defaultDb, repository, processorSupport, isJoin);
        }
        else if (element instanceof UnaryOperation)
        {
            scanOperation(((UnaryOperation) element)._nested(), tableByAliasBySchema, selfJoinTarget, defaultDb, repository, processorSupport, isJoin);
        }
        else if (element instanceof DynaFunction)
        {
            for (RelationalOperationElement param : ((DynaFunction) element)._parameters())
            {
                scanOperation(param, tableByAliasBySchema, selfJoinTarget, defaultDb, repository, processorSupport, isJoin);
            }
        }
    }

    private static <U, V> Pair<U, V> newPair(Class<?> type1, Class<?> type2, ModelRepository repository, ProcessorSupport processorSupport)
    {
        Pair<U, V> pair = (Pair<U, V>) repository.newAnonymousCoreInstance(null, processorSupport.package_getByUserPath(M3Paths.Pair));

        // Generics
        Class<?> genericTypeType = (Class<?>) processorSupport.package_getByUserPath(M3Paths.GenericType);
        GenericType classifierGenericType = (GenericType) repository.newAnonymousCoreInstance(null, genericTypeType);
        classifierGenericType._rawTypeCoreInstance(processorSupport.package_getByUserPath(M3Paths.Pair));

        GenericType typeArgument = (GenericType) repository.newAnonymousCoreInstance(null, processorSupport.package_getByUserPath(M3Paths.GenericType));
        typeArgument._rawTypeCoreInstance(type1);
        GenericType typeArgument2 = (GenericType) repository.newAnonymousCoreInstance(null, processorSupport.package_getByUserPath(M3Paths.GenericType));
        typeArgument2._rawTypeCoreInstance(type2);
        classifierGenericType._typeArguments(Lists.immutable.of(typeArgument, typeArgument2));

        pair._classifierGenericType(classifierGenericType);

        return pair;
    }

    static void processTableAliasColumn(TableAliasColumn tableAliasColumn, Database defaultDb, ProcessorSupport processorSupport)
    {
        processTableAliasColumn(tableAliasColumn, null, null, defaultDb, null, processorSupport, false);
    }

    private static void processTableAliasColumn(TableAliasColumn tableAliasColumn, MutableMap<String, MutableMap<String, CoreInstance>> tableByAliasBySchema, MutableList<TableAliasColumn> selfJoinTarget, Database defaultDb, ModelRepository repository, ProcessorSupport processorSupport, boolean isJoin)
    {
        String aliasName = tableAliasColumn._alias() == null ? null : tableAliasColumn._alias()._name();
        if (SELF_JOIN_TABLE_NAME.equals(aliasName))
        {
            if (selfJoinTarget != null)
            {
                selfJoinTarget.add(tableAliasColumn);
                addReferenceUsageForJoin(defaultDb, tableAliasColumn._alias(), repository, processorSupport);
            }
        }
        else
        {
            TableAlias tableAlias = tableAliasColumn._alias();
            Database database = (Database) ImportStub.withImportStubByPass(tableAlias._databaseCoreInstance(), processorSupport);
            if (database == null)
            {
                if (defaultDb == null)
                {
                    throw new PureCompilationException(tableAliasColumn.getSourceInformation(), "The system can't figure out which database to use.");
                }
                database = defaultDb;
            }

            String schemaName = getTableAliasSchemaName(tableAlias);
            String alias = tableAlias._name();

            CoreInstance table = null;
            if (tableByAliasBySchema != null)
            {
                MutableMap<String, CoreInstance> tableByAlias = tableByAliasBySchema.get(schemaName);
                if (tableByAlias != null)
                {
                    table = tableByAlias.get(alias);
                }
            }

            if (table == null)
            {
                table = findTableForAlias(database, tableAlias, processorSupport);
                if (tableByAliasBySchema != null)
                {
                    tableByAliasBySchema.getIfAbsentPut(schemaName, Functions0.<String, CoreInstance>newUnifiedMap()).put(alias, table);
                }
            }
            tableAlias._relationalElement((RelationalOperationElement) table);

            String columnName = tableAliasColumn._columnName();
            Column col = null;
            if (table instanceof Relation)
            {
                col = (Column) ((Relation) table)._columns().selectWith(COLUMN_NAME_PREDICATE, columnName).toList().getFirst();
            }
            if (col == null)
            {
                throw new PureCompilationException(tableAliasColumn.getSourceInformation(), "The column '" + columnName + "' can't be found in the table '" + ((NamedRelation) table)._name() + "'");
            }
            tableAliasColumn._column(col);

            if (isJoin)
            {
                addReferenceUsageForJoin(defaultDb, tableAlias, repository, processorSupport);
            }
        }
    }

    private static void addReferenceUsageForJoin(Database defaultDb, TableAlias tableAlias, ModelRepository repository, ProcessorSupport processorSupport)
    {
        if (tableAlias._databaseCoreInstance() != null)
        {
            CoreInstance user = ImportStub.withImportStubByPass(defaultDb, processorSupport);
            CoreInstance used = ImportStub.withImportStubByPass(tableAlias._databaseCoreInstance(), processorSupport);
            SourceInformation sourceInformation = tableAlias._databaseCoreInstance().getSourceInformation();
            ReferenceUsage.addReferenceUsage(used, user, M2RelationalProperties.database, 0, repository, processorSupport, sourceInformation);
        }
    }

    private static boolean schemaExists(Database database, String schemaName, ProcessorSupport processorSupport)
    {
        return DEFAULT_SCHEMA_NAME.equals(schemaName) || schemaExists(Sets.mutable.<CoreInstance>empty(), database, schemaName, processorSupport);
    }

    private static boolean schemaExists(MutableSet<CoreInstance> visited, Database database, String schemaName, ProcessorSupport processorSupport)
    {
        if (visited.add(database))
        {
            if (database._schemas().collect(Schema::_name).contains(schemaName))
            {
                return true;
            }
            for (CoreInstance includedDB : ImportStub.withImportStubByPasses(database._includesCoreInstance().toList(), processorSupport))
            {
                if (schemaExists(visited, (Database) includedDB, schemaName, processorSupport))
                {
                    return true;
                }
            }
        }
        return false;
    }

    static CoreInstance findTableForAlias(Database database, TableAlias tableAlias, ProcessorSupport processorSupport) throws PureCompilationException
    {
        String schemaName = getTableAliasSchemaName(tableAlias);
        if (!schemaExists(database, schemaName, processorSupport))
        {
            throw new PureCompilationException(tableAlias.getSourceInformation(), "The schema '" + schemaName + "' can't be found in the database '" + database.getName() + "'");
        }
        String alias = tableAlias._name();
        CoreInstance table = org.finos.legend.pure.m2.relational.Database.findTable(database, schemaName, alias, processorSupport);
        if (table == null)
        {
            throw new PureCompilationException(tableAlias.getSourceInformation(), "The table '" + alias + "' can't be found in the schema '" + schemaName + "' in the database '" + database.getName() + "'");
        }
        return table;
    }

    private static String getTableAliasSchemaName(TableAlias tableAlias)
    {
        return tableAlias._schema() == null ? DEFAULT_SCHEMA_NAME : tableAlias._schema();
    }

    static CoreInstance findFilter(Database database, String filterName, ProcessorSupport processorSupport)
    {
        Queue<Database> queue = new ArrayDeque<>();
        queue.add(database);
        return breadthFirstFilterSearch(queue, filterName, processorSupport);
    }

    private static CoreInstance breadthFirstFilterSearch(Queue<Database> databases, String filterName, ProcessorSupport processorSupport)
    {
        IndexSpecification<String> indexSpec = IndexSpecifications.getPropertyValueNameIndexSpec(M3Properties.name);
        MutableSet<Database> visitedDBs = Sets.mutable.empty();
        while (!databases.isEmpty())
        {
            Database database = databases.remove();
            if (visitedDBs.add(database))
            {
                CoreInstance filter = database.getValueInValueForMetaPropertyToManyByIDIndex(M2RelationalProperties.filters, indexSpec, filterName);
                if (filter != null)
                {
                    return filter;
                }
                ListIterable<? extends Database> includes = (ListIterable<? extends Database>) ImportStub.withImportStubByPasses(database._includesCoreInstance().toList(), processorSupport);
                Iterate.addAllIterable(includes, databases);
            }
        }
        return null;
    }
}
