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
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.runtime.navigation.NavigationHandler;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class TableAliasColumnNavigationHandler implements NavigationHandler<TableAliasColumn>
{
    @Override
    public String getClassName()
    {
        return M2RelationalPaths.TableAliasColumn;
    }

    @Override
    public CoreInstance findNavigationElement(TableAliasColumn instance, String sourceCode, int line, int column, ProcessorSupport processorSupport)
    {
        return instance._column();
    }
}
