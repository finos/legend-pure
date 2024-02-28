// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.m2.dsl.mapping.serialization.runtime.binary.reference;

import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingProperties;
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

public class EnumerationMappingReferenceSerializer implements ExternalReferenceSerializer
{
    @Override
    public String getTypePath()
    {
        return M2MappingPaths.EnumerationMapping;
    }

    @Override
    public void serialize(CoreInstance enumerationMapping, ExternalReferenceSerializationHelper helper)
    {
        CoreInstance mapping = helper.getPropertyValueToOne(enumerationMapping, M3Properties.parent);
        String name = PrimitiveUtilities.getStringValue(helper.getPropertyValueToOne(enumerationMapping, M3Properties.name));
        helper.writeElementReference(mapping);
        helper.writeString(name);
    }

    @Override
    public Reference deserialize(ExternalReferenceDeserializationHelper helper)
    {
        Reference mappingReference = helper.readElementReference();
        String enumerationMappingName = helper.readString();
        return new EnumerationMappingExternalReference(mappingReference, enumerationMappingName);
    }

    private static class EnumerationMappingExternalReference extends AbstractReferenceWithOwner
    {
        private final String enumerationMappingName;

        private EnumerationMappingExternalReference(Reference mappingReference, String enumerationMappingName)
        {
            super(mappingReference);
            this.enumerationMappingName = enumerationMappingName;
        }

        @Override
        protected CoreInstance resolveFromOwner(CoreInstance mapping) throws UnresolvableReferenceException
        {
            CoreInstance enumerationMapping = mapping.getValueInValueForMetaPropertyToManyWithKey(M2MappingProperties.enumerationMappings, M3Properties.name, this.enumerationMappingName);
            if (enumerationMapping == null)
            {
                setFailureMessage("Could not find enumeration mapping '" + this.enumerationMappingName + "' in " + PackageableElement.getUserPathForPackageableElement(mapping));
            }
            return enumerationMapping;
        }
    }
}
