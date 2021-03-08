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

package org.finos.legend.pure.m2.relational;

import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMappingsImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class TableAlias
{
    private TableAlias()
    {
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias copyTableAlias(org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias tableAlias, SourceInformation newSourceInfo, ProcessorSupport processorSupport)
    {
        return copyTableAlias(tableAlias, true, newSourceInfo, processorSupport);
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias copyTableAlias(org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias tableAlias, boolean copySourceInfo, ProcessorSupport processorSupport)
    {
        return copyTableAlias(tableAlias, !copySourceInfo, null, processorSupport);
    }

    private static org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias copyTableAlias(org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias tableAlias, boolean replaceSourceInfo, SourceInformation newSourceInfo, ProcessorSupport processorSupport)
    {
        org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias copy = (org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias)processorSupport.newAnonymousCoreInstance(replaceSourceInfo ? newSourceInfo : tableAlias.getSourceInformation(), M2RelationalPaths.TableAlias);

        copy._name(tableAlias._name());
        copy._relationalElement(tableAlias._relationalElement());

        PropertyMappingsImplementation setMappingOwner = tableAlias._setMappingOwner();
        if (setMappingOwner != null)
        {
            copy._setMappingOwner(setMappingOwner);
        }

        Database database = (Database)ImportStub.withImportStubByPass(tableAlias._databaseCoreInstance(), processorSupport);
        if (database != null)
        {
            copy._databaseCoreInstance(database);
        }

        String schema = tableAlias._schema();
        if (schema != null)
        {
            copy._schema(schema);
        }

        return copy;
    }
}
