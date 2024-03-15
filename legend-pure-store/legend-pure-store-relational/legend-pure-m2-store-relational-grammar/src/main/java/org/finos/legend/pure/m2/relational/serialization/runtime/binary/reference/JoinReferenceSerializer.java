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
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.AbstractReferenceWithOwner;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceDeserializationHelper;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceSerializationHelper;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceSerializer;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.Reference;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.UnresolvableReferenceException;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class JoinReferenceSerializer implements ExternalReferenceSerializer
{
    @Override
    public String getTypePath()
    {
        return M2RelationalPaths.Join;
    }

    @Override
    public void serialize(CoreInstance join, ExternalReferenceSerializationHelper helper)
    {
        CoreInstance database = helper.getPropertyValueToOne(join, M2RelationalProperties.database);
        if (database == null)
        {
            throw new RuntimeException("Cannot serialize join with no database");
        }
        helper.writeElementReference(database);
        helper.writeString(PrimitiveUtilities.getStringValue(helper.getPropertyValueToOne(join, M3Properties.name)));
    }

    @Override
    public Reference deserialize(ExternalReferenceDeserializationHelper helper)
    {
        Reference databaseReference = helper.readElementReference();
        String joinName = helper.readString();
        return new JoinExternalReference(databaseReference, joinName);
    }

    private static class JoinExternalReference extends AbstractReferenceWithOwner
    {
        private final String joinName;

        private JoinExternalReference(Reference databaseReference, String joinName)
        {
            super(databaseReference);
            this.joinName = joinName;
        }

        @Override
        protected CoreInstance resolveFromOwner(CoreInstance database) throws UnresolvableReferenceException
        {
            CoreInstance join = database.getValueInValueForMetaPropertyToManyWithKey(M2RelationalProperties.joins, M3Properties.name, this.joinName);
            if (join == null)
            {
                setFailureMessage("Could not find join '" + this.joinName + "' in " + PackageableElement.getUserPathForPackageableElement(database));
            }
            return join;
        }
    }
}
