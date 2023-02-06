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

package org.finos.legend.pure.m2.relational.serialization.grammar.v1.validator;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.m2.relational.Database;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.TreeNode;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.Join;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.JoinTreeNode;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class JoinTreeNodeValidation
{
    private JoinTreeNodeValidation()
    {
    }

    public static void validateJoinTreeNode(JoinTreeNode joinTreeNode, RelationalOperationElement sourceTable, ProcessorSupport processorSupport)
    {
        validateJoinTreeNode(joinTreeNode, sourceTable, null, processorSupport);
    }

    public static void validateJoinTreeNode(JoinTreeNode joinTreeNode, RelationalOperationElement sourceTable, RelationalOperationElement targetTable, ProcessorSupport processorSupport)
    {
        Join join = joinTreeNode._join();
        RelationalOperationElement newSourceTable = followJoin(join, sourceTable);
        if (newSourceTable == null)
        {
            StringBuilder message = new StringBuilder("Mapping error: the join ");
            Database.writeJoinId(message, join, true, processorSupport);
            message.append(" does not contain the source table ");
            Database.writeTableId(message, sourceTable, processorSupport);
            throw new PureCompilationException(joinTreeNode.getSourceInformation(), message.toString());
        }

        RichIterable<? extends TreeNode> childrenData = joinTreeNode._childrenData();
        if (childrenData.isEmpty())
        {
            if ((targetTable != null) && (targetTable != newSourceTable))
            {
                StringBuilder message = new StringBuilder("Mapping error: the join ");
                Database.writeJoinId(message, join, true, processorSupport);
                message.append(" does not connect from the source table ");
                Database.writeTableId(message, sourceTable, processorSupport);
                message.append(" to the target table ");
                Database.writeTableId(message, targetTable, processorSupport);
                message.append("; instead it connects to ");
                Database.writeTableId(message, newSourceTable, processorSupport);
                throw new PureCompilationException(joinTreeNode.getSourceInformation(), message.toString());
            }
        }
        else
        {
            for (TreeNode child : childrenData)
            {
                validateJoinTreeNode((JoinTreeNode)child, newSourceTable, targetTable, processorSupport);
            }
        }
    }

    private static RelationalOperationElement followJoin(Join join, RelationalOperationElement sourceTable)
    {
        for (Pair pair : join._aliases())
        {
            if (sourceTable == ((TableAlias)pair._first())._relationalElement())
            {
                return ((TableAlias)pair._second())._relationalElement();
            }
        }
        return null;
    }
}
