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

import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.compiler.unload.unbind.Shared;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.TreeNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.DynaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Literal;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElementWithJoin;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.JoinTreeNode;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.coreinstance.primitive.StringCoreInstance;


public class RelationalOperationElementUnbind
{

    private RelationalOperationElementUnbind()
    {
    }

    public static void cleanNode(RelationalOperationElement element, ModelRepository modelRepository, ProcessorSupport processorSupport) throws PureCompilationException
    {
        if (element instanceof TableAliasColumn)
        {
            TableAliasColumn tableAliasColumn = (TableAliasColumn)element;
            ImportStub database = (ImportStub)tableAliasColumn._alias()._databaseCoreInstance();
            Shared.cleanUpReferenceUsage(database, tableAliasColumn, processorSupport);
            Shared.cleanImportStub(database, processorSupport);
            tableAliasColumn._columnRemove();
            TableAlias alias = tableAliasColumn._alias();
            alias._relationalElementRemove();
        }
        else if (element instanceof DynaFunction)
        {
            for (RelationalOperationElement val : ((DynaFunction)element)._parameters())
            {
                cleanNode(val, modelRepository, processorSupport);
            }
        }
        else if (element instanceof RelationalOperationElementWithJoin)
        {
            if (((RelationalOperationElementWithJoin)element)._relationalOperationElement() != null)
            {
                cleanNode(((RelationalOperationElementWithJoin)element)._relationalOperationElement(), modelRepository, processorSupport);
            }
            if (((RelationalOperationElementWithJoin)element)._joinTreeNode() != null)
            {
                cleanJoinTreeNode(((RelationalOperationElementWithJoin)element)._joinTreeNode(), modelRepository, processorSupport);
            }
        }
        else if (element instanceof Literal)
        {
            // Nothing to do
        }
        else
        {
            throw new RuntimeException("TO CODE: clean relational operation element node of type " + PackageableElement.getUserPathForPackageableElement(element.getClassifier()));
        }
    }

    public static void cleanJoinTreeNode(JoinTreeNode joinTreeNode, ModelRepository modelRepository, ProcessorSupport processorSupport) throws PureCompilationException
    {
        if (joinTreeNode != null)
        {
            Shared.cleanUpReferenceUsage(joinTreeNode._databaseCoreInstance(), joinTreeNode, processorSupport);
            Shared.cleanImportStub(joinTreeNode._databaseCoreInstance(), processorSupport);

            joinTreeNode._joinRemove();
            joinTreeNode._aliasRemove();
            CoreInstance joinTypeCoreInstance = joinTreeNode._joinTypeCoreInstance();
            if (joinTypeCoreInstance instanceof Enum)
            {
                Enum joinType = (Enum) joinTypeCoreInstance;
                joinTreeNode._joinTypeRemove();
                StringCoreInstance joinTypeString = modelRepository.newStringCoreInstance_cached(joinType.getName());
                joinTreeNode._joinTypeCoreInstance(joinTypeString);
            }

            for (TreeNode child : joinTreeNode._childrenData())
            {
                cleanJoinTreeNode((JoinTreeNode)child, modelRepository, processorSupport);
            }
        }
    }
}
