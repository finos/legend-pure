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

package org.finos.legend.pure.m2.relational.serialization.grammar.v1.navigation;

import org.finos.legend.pure.m2.relational.M2RelationalPaths;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Schema;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.runtime.navigation.NavigationHandler;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class TableAliasNavigationHandler implements NavigationHandler<TableAlias>
{
    @Override
    public String getClassName()
    {
        return M2RelationalPaths.TableAlias;
    }

    @Override
    public CoreInstance findNavigationElement(TableAlias instance, String sourceCode, int line, int column, ProcessorSupport processorSupport)
    {
        SourceInformation tableAliasSourceInformation = instance.getSourceInformation();

        StringBuilder tableAliasStringBuilder = new StringBuilder();
        String [] sourceCodeByLine = sourceCode.split("\n");
        if (tableAliasSourceInformation.getStartLine() == tableAliasSourceInformation.getEndLine())
        {
            tableAliasStringBuilder.append(sourceCodeByLine[tableAliasSourceInformation.getStartLine()-1].substring(tableAliasSourceInformation.getStartColumn()-1, tableAliasSourceInformation.getEndColumn()));
        }
        else
        {
            tableAliasStringBuilder.append(sourceCodeByLine[tableAliasSourceInformation.getStartLine()-1].substring(tableAliasSourceInformation.getStartColumn()-1).trim());
            for (int i = tableAliasSourceInformation.getStartLine(); i < tableAliasSourceInformation.getEndLine()-1 ; i++)
            {
                tableAliasStringBuilder.append(sourceCodeByLine[i].trim());
            }
            tableAliasStringBuilder.append(sourceCodeByLine[tableAliasSourceInformation.getEndLine()-1].substring(0, tableAliasSourceInformation.getEndColumn()).trim());
        }
        String tableAliasString = tableAliasStringBuilder.toString();
        if (tableAliasString.contains("."))
        {
            String schemaString = tableAliasString.substring(0, tableAliasString.indexOf("."));
            if (tableAliasSourceInformation.getStartLine() == line && tableAliasSourceInformation.getStartColumn() <= column && column < tableAliasSourceInformation.getStartColumn() + schemaString.length())
            {
                Schema schema = ((Table)instance._relationalElement())._schema();
                if (schema != null)
                {
                    return schema;
                }
            }
        }

        return instance._relationalElement();
    }
}
