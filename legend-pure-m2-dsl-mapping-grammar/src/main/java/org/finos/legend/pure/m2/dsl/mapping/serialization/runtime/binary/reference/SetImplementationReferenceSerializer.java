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

public class SetImplementationReferenceSerializer implements ExternalReferenceSerializer
{
    @Override
    public String getTypePath()
    {
        return M2MappingPaths.SetImplementation;
    }

    @Override
    public void serialize(CoreInstance setImplementation, ExternalReferenceSerializationHelper helper)
    {
        CoreInstance mapping = helper.getPropertyValueToOne(setImplementation, M3Properties.parent);
        String id = PrimitiveUtilities.getStringValue(helper.getPropertyValueToOne(setImplementation, M3Properties.id));
        helper.writeElementReference(mapping);
        helper.writeString(id);
    }

    @Override
    public Reference deserialize(ExternalReferenceDeserializationHelper helper)
    {
        Reference mappingReference = helper.readElementReference();
        String setImplementationId = helper.readString();
        return new SetImplementationExternalReference(mappingReference, setImplementationId);
    }

    private static class SetImplementationExternalReference extends AbstractReferenceWithOwner
    {
        private final String setImplementationId;

        private SetImplementationExternalReference(Reference mappingReference, String setImplementationId)
        {
            super(mappingReference);
            this.setImplementationId = setImplementationId;
        }

        @Override
        protected CoreInstance resolveFromOwner(CoreInstance mapping) throws UnresolvableReferenceException
        {
            CoreInstance setImplementation = mapping.getValueInValueForMetaPropertyToManyWithKey(M2MappingProperties.classMappings, M3Properties.id, this.setImplementationId);
            if (setImplementation == null)
            {
                setFailureMessage("Could not find set implementation '" + this.setImplementationId + "' in " + PackageableElement.getUserPathForPackageableElement(mapping));
            }
            return setImplementation;
        }
    }
}
