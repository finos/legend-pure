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

public class FilterReferenceSerializer implements ExternalReferenceSerializer
{
    @Override
    public String getTypePath()
    {
        return M2RelationalPaths.Filter;
    }

    @Override
    public void serialize(CoreInstance filter, ExternalReferenceSerializationHelper helper)
    {
        CoreInstance database = helper.getPropertyValueToOne(filter, M2RelationalProperties.database);
        if (database == null)
        {
            throw new RuntimeException("Cannot serialize filter with no database");
        }
        helper.writeElementReference(database);
        helper.writeString(PrimitiveUtilities.getStringValue(helper.getPropertyValueToOne(filter, M3Properties.name)));
    }

    @Override
    public Reference deserialize(ExternalReferenceDeserializationHelper helper)
    {
        Reference databaseReference = helper.readElementReference();
        String filterName = helper.readString();
        return new FilterExternalReference(databaseReference, filterName);
    }

    private static class FilterExternalReference extends AbstractReferenceWithOwner
    {
        private final String filterName;

        private FilterExternalReference(Reference databaseReference, String filterName)
        {
            super(databaseReference);
            this.filterName = filterName;
        }

        @Override
        protected CoreInstance resolveFromOwner(CoreInstance database) throws UnresolvableReferenceException
        {
            CoreInstance filter = database.getValueInValueForMetaPropertyToManyWithKey(M2RelationalProperties.filters, M3Properties.name, this.filterName);
            if (filter == null)
            {
                setFailureMessage("Could not find filter '" + this.filterName + "' in " + PackageableElement.getUserPathForPackageableElement(database));
            }
            return filter;
        }
    }
}
