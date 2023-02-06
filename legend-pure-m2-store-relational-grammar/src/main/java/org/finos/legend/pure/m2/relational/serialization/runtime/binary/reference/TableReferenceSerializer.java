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

package org.finos.legend.pure.m2.relational.serialization.runtime.binary.reference;

import org.finos.legend.pure.m2.relational.M2RelationalPaths;
import org.finos.legend.pure.m2.relational.M2RelationalProperties;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.processor.DatabaseProcessor;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.AbstractReferenceWithOwner;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceDeserializationHelper;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceSerializationHelper;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceSerializer;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.Reference;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.UnresolvableReferenceException;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class TableReferenceSerializer implements ExternalReferenceSerializer
{
    @Override
    public String getTypePath()
    {
        return M2RelationalPaths.Table;
    }

    @Override
    public void serialize(CoreInstance table, ExternalReferenceSerializationHelper helper)
    {
        CoreInstance schema = helper.getPropertyValueToOne(table, M2RelationalProperties.schema);
        CoreInstance database = helper.getPropertyValueToOne(schema, M2RelationalProperties.database);
        helper.writeElementReference(database);
        helper.writeString(PrimitiveUtilities.getStringValue(helper.getPropertyValueToOne(schema, M3Properties.name)));
        helper.writeString(PrimitiveUtilities.getStringValue(helper.getPropertyValueToOne(table, M3Properties.name)));
    }

    @Override
    public Reference deserialize(ExternalReferenceDeserializationHelper helper)
    {
        Reference databaseReference = helper.readElementReference();
        String schemaName = helper.readString();
        String tableName = helper.readString();
        return new TableExternalReference(databaseReference, schemaName, tableName);
    }

    private static class TableExternalReference extends AbstractReferenceWithOwner
    {
        private final String schemaName;
        private final String tableName;

        private TableExternalReference(Reference databaseReference, String schemaName, String tableName)
        {
            super(databaseReference);
            this.schemaName = schemaName;
            this.tableName = tableName;
        }

        @Override
        protected CoreInstance resolveFromOwner(CoreInstance database) throws UnresolvableReferenceException
        {
            CoreInstance schema = database.getValueInValueForMetaPropertyToManyWithKey(M2RelationalProperties.schemas, M3Properties.name, this.schemaName);
            if (schema == null)
            {
                setFailureMessage("Could not find schema '" + this.schemaName + "' in " + PackageableElement.getUserPathForPackageableElement(database));
                return null;
            }

            CoreInstance table = schema.getValueInValueForMetaPropertyToManyWithKey(M2RelationalProperties.tables, M3Properties.name, this.tableName);
            if (table == null)
            {
                StringBuilder message = new StringBuilder("Could not find table '");
                message.append(this.tableName);
                message.append("' in ");
                PackageableElement.writeUserPathForPackageableElement(message, database);
                if (!DatabaseProcessor.DEFAULT_SCHEMA_NAME.equals(this.schemaName))
                {
                    message.append('.');
                    message.append(this.schemaName);
                }
                setFailureMessage(message.toString());
            }
            return table;
        }
    }
}
