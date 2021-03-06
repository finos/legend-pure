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
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m2.relational.M2RelationalPaths;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.processor.DatabaseProcessor;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.unload.unbind.Shared;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.ColumnMapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.DynaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Filter;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Schema;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.Join;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.operation.BinaryOperation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.operation.Operation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.operation.UnaryOperation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.NamedRelation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.View;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class DatabaseUnloadUnbind implements MatchRunner<Database>
{
    @Override
    public String getClassName()
    {
        return M2RelationalPaths.Database;
    }

    @Override
    public void run(Database database, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();
        for (CoreInstance db : database._includesCoreInstance())
        {
            Shared.cleanUpReferenceUsage(db, database, state.getProcessorSupport());
            Shared.cleanImportStub(db, state.getProcessorSupport());
        }

        for (Schema schema : database._schemas())
        {
            schema._databaseRemove();
            RichIterable<? extends Table> tables = schema._tables();
            for (Table table : tables)
            {
                processTable(table);
            }
            RichIterable<? extends View> views = schema._views();
            for (View view : views)
            {
                processView(view, modelRepository, processorSupport);
            }
            schema._relationsRemove();
        }

        RichIterable<? extends Join> joins = database._joins();
        for (Join join : joins)
        {
            join._databaseRemove();
            this.processJoin(join, processorSupport);
        }

        RichIterable<? extends Filter> filters = database._filters();
        for (Filter filter : filters)
        {
            filter._databaseRemove();
            this.processFilter(filter, processorSupport);
        }
        database._namespacesRemove();
    }

    private void processFilter(Filter filter, ProcessorSupport processorSupport)
    {
        Operation operation = filter._operation();
        scanOperation(operation, null, processorSupport);
    }

    private void processJoin(Join join, ProcessorSupport processorSupport)
    {
        Operation operation = join._operation();
        TableAlias target = join._target();
        if (target != null)
        {
            join._targetRemove();
        }
        join._aliasesRemove();
        scanOperation(operation, target, processorSupport);
    }

    private void scanOperation(RelationalOperationElement element, TableAlias joinTarget, ProcessorSupport processorSupport)
    {
        if (element instanceof TableAliasColumn)
        {
            processTableAliasColumn((TableAliasColumn)element, joinTarget, processorSupport);
        }
        else if (element instanceof BinaryOperation)
        {
            scanOperation(((BinaryOperation)element)._left(), joinTarget, processorSupport);
            scanOperation(((BinaryOperation)element)._right(), joinTarget, processorSupport);
        }
        else if (element instanceof UnaryOperation)
        {
            scanOperation(((UnaryOperation)element)._nested(), joinTarget, processorSupport);
        }
        else if (element instanceof DynaFunction)
        {
            for (RelationalOperationElement param : ((DynaFunction)element)._parameters())
            {
                scanOperation(param, joinTarget, processorSupport);
            }
        }
    }

    private void processTable(Table table)
    {
        table._schemaRemove();
        this.processTableOrViewColumns(table);
        table._setColumnsRemove();
    }

    private void processView(View view, ModelRepository repository, ProcessorSupport processorSupport)
    {
        view._schemaRemove();
        this.processTableOrViewColumns(view);
        view._userDefinedPrimaryKeyRemove();
        RelationalMappingSpecificationUnbind.cleanRelationalMappingSpecification(view, repository, processorSupport);
        for (Column column : LazyIterate.concatenate((Iterable<Column>)view._primaryKey(), (Iterable<Column>)view._columns()))
        {
            column._typeRemove();
        }
        for (ColumnMapping columnMapping : view._columnMappings())
        {
            RelationalOperationElement columnMappingRelationalElement = columnMapping._relationalOperationElement();
            RelationalOperationElementUnbind.cleanNode(columnMappingRelationalElement, repository, processorSupport);
        }
    }

    private void processTableOrViewColumns(NamedRelation tableOrView)
    {
        for (RelationalOperationElement column : tableOrView._columns())
        {
            ((Column)column)._ownerRemove();
        }
    }

    private static void processTableAliasColumn(TableAliasColumn tableAliasColumn, TableAlias joinTarget, ProcessorSupport processorSupport) throws PureCompilationException
    {
        TableAlias tableAlias = tableAliasColumn._alias();
        if ((joinTarget != null) && (tableAlias == joinTarget))
        {
            // Replace the join target with a {target} alias
            ModelRepository repository = tableAliasColumn.getRepository();
            TableAlias targetAlias = (TableAlias)repository.newAnonymousCoreInstance(tableAliasColumn.getSourceInformation(), joinTarget.getClassifier());
            targetAlias._name(DatabaseProcessor.SELF_JOIN_TABLE_NAME);
            tableAliasColumn._alias(targetAlias);
        }
        else
        {
            tableAlias._relationalElementRemove();
            ImportStub tableAliasDatabase = (ImportStub)tableAlias._databaseCoreInstance();
            if (tableAliasDatabase != null)
            {
                Shared.cleanImportStub(tableAliasDatabase, processorSupport);
            }
        }
        tableAliasColumn._columnRemove();
    }
}
