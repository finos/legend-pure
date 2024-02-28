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
import org.finos.legend.pure.m2.relational.M2RelationalPaths;
import org.finos.legend.pure.m2.relational.M2RelationalProperties;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.processor.DatabaseProcessor;
import org.finos.legend.pure.m3.compiler.ReferenceUsage;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.TreeNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMappingsImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.DynaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Literal;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElementWithJoin;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.Join;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.JoinTreeNode;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.coreinstance.primitive.StringCoreInstance;

import java.util.Collection;

class RelationalOperationElementProcessor
{
    private RelationalOperationElementProcessor()
    {
    }

    static void processColumnExpr(RelationalOperationElement impl, CoreInstance implementation, CoreInstance mappingOwner, MutableSet<TableAlias> tableAliases, Matcher matcher, ProcessorState state, ModelRepository repository, ProcessorSupport processorSupport) throws PureCompilationException
    {
        if (impl instanceof TableAliasColumn)
        {
            processTableAliasColumn((TableAliasColumn)impl, mappingOwner, tableAliases, processorSupport);
        }
        else if (impl instanceof DynaFunction)
        {
            processDynaFunction((DynaFunction)impl, implementation, mappingOwner, tableAliases, matcher, state, repository, processorSupport);
        }
        else if (impl instanceof RelationalOperationElementWithJoin)
        {
            RelationalOperationElement relationalOperationElement = ((RelationalOperationElementWithJoin)impl)._relationalOperationElement();
            if (relationalOperationElement != null)
            {
                if (relationalOperationElement instanceof DynaFunction)
                {
                    processDynaFunction((DynaFunction)relationalOperationElement, implementation, mappingOwner, tableAliases, matcher, state, repository, processorSupport);
                }
                else if (relationalOperationElement instanceof TableAliasColumn)
                {
                    processTableAliasColumn((TableAliasColumn)relationalOperationElement, mappingOwner, tableAliases, processorSupport);
                }
            }
            JoinTreeNode joinTreeNode = ((RelationalOperationElementWithJoin)impl)._joinTreeNode();
            if (joinTreeNode != null)
            {
                processJoinTreeNode(joinTreeNode, mappingOwner, matcher, state, repository, processorSupport);
                tableAliases.clear();
            }
        }
        else if (impl instanceof Literal)
        {
            // Do nothing
        }
        else
        {
            throw new PureCompilationException(impl.getSourceInformation(), "Not Supported Yet! " + impl);
        }
    }

    static void populateColumnExpressionReferenceUsages(CoreInstance columnExpression, ModelRepository repository, ProcessorSupport processorSupport)
    {
        if (columnExpression instanceof TableAliasColumn)
        {
            populateTableAliasColumnReferenceUsages((TableAliasColumn)columnExpression, repository, processorSupport);
        }
        else if (columnExpression instanceof DynaFunction)
        {
            populateDynaFunctionReferenceUsages((DynaFunction)columnExpression, repository, processorSupport);
        }
        else if (columnExpression instanceof RelationalOperationElementWithJoin)
        {
            RelationalOperationElement relationalOperationElement = ((RelationalOperationElementWithJoin)columnExpression)._relationalOperationElement();
            if (relationalOperationElement != null)
            {
                if (relationalOperationElement instanceof DynaFunction)
                {
                    populateDynaFunctionReferenceUsages((DynaFunction)relationalOperationElement, repository, processorSupport);
                }
                else if (relationalOperationElement instanceof TableAliasColumn)
                {
                    populateTableAliasColumnReferenceUsages((TableAliasColumn)relationalOperationElement, repository, processorSupport);
                }
            }

            JoinTreeNode joinTreeNode = ((RelationalOperationElementWithJoin)columnExpression)._joinTreeNode();
            if (joinTreeNode != null)
            {
                populateJoinTreeNodeReferenceUsages(joinTreeNode, repository, processorSupport);
            }
        }
    }

    private static void processDynaFunction(DynaFunction dynaFunction, CoreInstance implementation, CoreInstance mappingOwner, MutableSet<TableAlias> tableAliases, Matcher matcher, ProcessorState state, ModelRepository repository, ProcessorSupport processorSupport)
    {
        for (RelationalOperationElement val : dynaFunction._parameters())
        {
            processColumnExpr(val, implementation, mappingOwner, tableAliases, matcher, state, repository, processorSupport);
        }
    }

    private static void populateDynaFunctionReferenceUsages(DynaFunction dynaFunction, ModelRepository repository, ProcessorSupport processorSupport)
    {
        for (RelationalOperationElement columnExpression : dynaFunction._parameters())
        {
            populateColumnExpressionReferenceUsages(columnExpression, repository, processorSupport);
        }
    }

    private static void processTableAliasColumn(TableAliasColumn tableAliasColumn, CoreInstance mappingOwner, MutableSet<TableAlias> tableAliases, ProcessorSupport processorSupport) throws PureCompilationException
    {
        DatabaseProcessor.processTableAliasColumn(tableAliasColumn, null, processorSupport);
        PropertyMappingsImplementation owner = tableAliasColumn._setMappingOwner();
        if (owner == null && mappingOwner != null)
        {
            tableAliasColumn._setMappingOwner((PropertyMappingsImplementation)mappingOwner);
        }
        Database database = tableAliasColumn._alias() == null ? null : (Database)ImportStub.withImportStubByPass(tableAliasColumn._alias()._databaseCoreInstance(), processorSupport);
        if (database == null)
        {
            throw new PureCompilationException(tableAliasColumn.getSourceInformation(), "The system can't figure out which database to use.");
        }
        tableAliases.add(tableAliasColumn._alias());
    }

    private static void populateTableAliasColumnReferenceUsages(TableAliasColumn tableAliasColumn, ModelRepository repository, ProcessorSupport processorSupport)
    {
        Database database = tableAliasColumn._alias() == null ? null : (Database)ImportStub.withImportStubByPass(tableAliasColumn._alias()._databaseCoreInstance(), processorSupport);
        ReferenceUsage.addReferenceUsage(database, tableAliasColumn, M2RelationalProperties.database, 0, repository, processorSupport, tableAliasColumn._alias()._databaseCoreInstance().getSourceInformation());
    }

    static void collectJoinTreeNodes(Collection<? super JoinTreeNode> targetCollection, CoreInstance implementation)
    {
        if (implementation instanceof RelationalOperationElementWithJoin)
        {
            JoinTreeNode joinTreeNode = ((RelationalOperationElementWithJoin)implementation)._joinTreeNode();
            if (joinTreeNode != null)
            {
                targetCollection.add(joinTreeNode);
            }
        }
        else if (implementation instanceof DynaFunction)
        {
            for (RelationalOperationElement param : ((DynaFunction)implementation)._parameters())
            {
                collectJoinTreeNodes(targetCollection, param);
            }
        }
    }

    static void processJoinTreeNode(JoinTreeNode joinTreeNode, CoreInstance implementation, Matcher matcher, ProcessorState state, ModelRepository repository, ProcessorSupport processorSupport)
    {
        String joinName = joinTreeNode._joinName();
        Database database = (Database)ImportStub.withImportStubByPass(joinTreeNode._databaseCoreInstance(), processorSupport);
        if (database == null)
        {
            throw new PureCompilationException(joinTreeNode.getSourceInformation(), "The system can't figure out which database to use.");
        }
        PostProcessor.processElement(matcher, database, state, processorSupport);

        CoreInstance joinType = joinTreeNode._joinTypeCoreInstance();
        if (joinType != null)
        {
            StringCoreInstance joinTypeString = (StringCoreInstance) joinType;
            String type = "INNER".equals(joinTypeString.getValue()) ? "INNER" : "LEFT_OUTER";
            Enumeration joinTypeEnumeration = (Enumeration) processorSupport.package_getByUserPath(M2RelationalPaths.JoinType);
            Enum joinTypeEnumInstance = (Enum) org.finos.legend.pure.m3.navigation.enumeration.Enumeration.findEnum(joinTypeEnumeration, type);
            if (joinTypeEnumInstance == null)
            {
                throw new PureCompilationException(joinTreeNode.getSourceInformation(), "The enum value '" + type + "' can't be found in the enumeration " + PackageableElement.getUserPathForPackageableElement(joinTypeEnumeration, "::"));
            }
            joinTreeNode._joinTypeCoreInstance(joinTypeEnumInstance);
        }

        Join join = findJoin(joinTreeNode, database, joinName, processorSupport);
        joinTreeNode._join(join);

        PropertyMappingsImplementation owner = joinTreeNode._setMappingOwner();
        if (owner == null && implementation != null)
        {
            joinTreeNode._setMappingOwner((PropertyMappingsImplementation)implementation);
        }

        for (TreeNode child : joinTreeNode._childrenData())
        {
            processJoinTreeNode((JoinTreeNode)child, implementation, matcher, state, repository, processorSupport);
        }
    }

    static void populateJoinTreeNodeReferenceUsages(JoinTreeNode joinTreeNode, ModelRepository repository, ProcessorSupport processorSupport)
    {
        Database database = (Database)ImportStub.withImportStubByPass(joinTreeNode._databaseCoreInstance(), processorSupport);
        ReferenceUsage.addReferenceUsage(database, joinTreeNode, M2RelationalProperties.database, 0, repository, processorSupport, joinTreeNode._databaseCoreInstance().getSourceInformation());
        for (TreeNode child : joinTreeNode._childrenData())
        {
            populateJoinTreeNodeReferenceUsages((JoinTreeNode)child, repository, processorSupport);
        }
    }

    static void processAliasForJoinTreeNode(JoinTreeNode joinTreeNode, RelationalOperationElement startTable, ProcessorSupport processorSupport)
    {
        TableAlias alias = findAliasForJoinTreeNode(joinTreeNode, startTable, processorSupport);
        TableAlias aliasCopy = org.finos.legend.pure.m2.relational.TableAlias.copyTableAlias(alias, null, processorSupport);
        PropertyMappingsImplementation setMappingOwner = joinTreeNode._setMappingOwner();
        if (setMappingOwner != null)
        {
            aliasCopy._setMappingOwner(setMappingOwner);
        }

        joinTreeNode._alias(aliasCopy);

        RichIterable<? extends TreeNode> children = joinTreeNode._childrenData();
        if (children.notEmpty())
        {
            RelationalOperationElement newStartTable = alias._relationalElement();
            for (TreeNode child : children)
            {
                processAliasForJoinTreeNode((JoinTreeNode)child, newStartTable, processorSupport);
            }
        }
    }

    private static Join findJoin(CoreInstance joinTreeNode, Database database, String joinName, ProcessorSupport processorSupport)
    {
        Join join = org.finos.legend.pure.m2.relational.Database.findJoin(database, joinName, processorSupport);
        if (join == null)
        {
            throw new PureCompilationException(joinTreeNode.getSourceInformation(), "The join '" + joinName + "' has not been found in the database '" + database.getName() + "'");
        }
        return join;
    }

    private static TableAlias findAliasForJoinTreeNode(JoinTreeNode joinTreeNode, RelationalOperationElement startTable, ProcessorSupport processorSupport)
    {
        Join join = joinTreeNode._join();

        // If the join specifies a target, return that
        TableAlias target = join._target();
        if (target != null)
        {
            return target;
        }

        // If the join does not specify a target, figure it out from the alias pairs
        RichIterable<? extends Pair> aliases = join._aliases();
        if (aliases.notEmpty())
        {
            for (Pair pair : aliases)
            {
                if (startTable == (pair._first() == null ? null : ((TableAlias)pair._first())._relationalElement()))
                {
                    return (TableAlias)pair._second();
                }
            }
        }

        // Failed to find anything, throw an exception
        StringBuilder message = new StringBuilder("Mapping error: the join ");
        org.finos.legend.pure.m2.relational.Database.writeJoinId(message, join, true, processorSupport);
        message.append(" does not contain the source table ");
        org.finos.legend.pure.m2.relational.Database.writeTableId(message, startTable, processorSupport);
        throw new PureCompilationException(joinTreeNode.getSourceInformation(), message.toString());
    }
}
