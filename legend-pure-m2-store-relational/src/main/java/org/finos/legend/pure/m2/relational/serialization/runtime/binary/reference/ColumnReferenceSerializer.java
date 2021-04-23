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
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.AbstractReferenceWithOwner;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceDeserializationHelper;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceSerializationHelper;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceSerializer;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.Reference;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.UnresolvableReferenceException;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class ColumnReferenceSerializer implements ExternalReferenceSerializer
{
    @Override
    public String getTypePath()
    {
        return M2RelationalPaths.Column;
    }

    @Override
    public void serialize(CoreInstance column, ExternalReferenceSerializationHelper helper)
    {
        CoreInstance owner = helper.getPropertyValueToOne(column, M3Properties.owner);
        if (owner == null)
        {
            throw new RuntimeException("Cannot serialize column with no owner");
        }
        helper.writeElementReference(owner);
        helper.writeString(PrimitiveUtilities.getStringValue(helper.getPropertyValueToOne(column, M3Properties.name)));
    }

    @Override
    public Reference deserialize(ExternalReferenceDeserializationHelper helper)
    {
        Reference ownerReference = helper.readElementReference();
        String columnName = helper.readString();
        return new ColumnExternalReference(ownerReference, columnName);
    }

    private static class ColumnExternalReference extends AbstractReferenceWithOwner
    {
        private final String columnName;

        private ColumnExternalReference(Reference ownerReference, String columnName)
        {
            super(ownerReference);
            this.columnName = columnName;
        }

        @Override
        protected CoreInstance resolveFromOwner(CoreInstance owner) throws UnresolvableReferenceException
        {
            CoreInstance column = owner.getValueInValueForMetaPropertyToManyWithKey(M2RelationalProperties.columns, M3Properties.name, this.columnName);
            if (column == null)
            {
                setFailureMessage("Could not find column '" + this.columnName + "' in " + owner);
            }
            return column;
        }
    }
}
