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

package org.finos.legend.pure.m3.serialization.runtime.binary.reference;

import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class EnumReferenceSerializer implements ExternalReferenceSerializer
{
    @Override
    public String getTypePath()
    {
        return M3Paths.Enum;
    }

    @Override
    public void serialize(CoreInstance instance, ExternalReferenceSerializationHelper helper)
    {
        helper.writeElementReference(instance.getClassifier());
        helper.writeString(instance.getName());
    }

    @Override
    public Reference deserialize(ExternalReferenceDeserializationHelper helper)
    {
        Reference enumerationReference = helper.readElementReference();
        String enumName = helper.readString();
        return new EnumExternalReference(enumerationReference, enumName);
    }

    private static class EnumExternalReference extends AbstractReferenceWithOwner
    {
        private final String enumName;

        private EnumExternalReference(Reference enumerationReference, String enumName)
        {
            super(enumerationReference);
            this.enumName = enumName;
        }

        @Override
        protected CoreInstance resolveFromOwner(CoreInstance enumeration) throws UnresolvableReferenceException
        {
            CoreInstance enumValue = enumeration.getValueInValueForMetaPropertyToMany(M3Properties.values, this.enumName);
            if (enumValue == null)
            {
                setFailureMessage("Could not find enum value '" + this.enumName + "' in " + PackageableElement.getUserPathForPackageableElement(enumeration));
            }
            return enumValue;
        }
    }
}
