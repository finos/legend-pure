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

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class QualifiedPropertyReferenceSerializer implements ExternalReferenceSerializer
{
    @Override
    public String getTypePath()
    {
        return M3Paths.QualifiedProperty;
    }

    @Override
    public void serialize(CoreInstance instance, ExternalReferenceSerializationHelper helper)
    {
        QualifiedProperty property = (QualifiedProperty)instance;
        CoreInstance owner = property._owner();
        helper.writeElementReference(owner);
        helper.writeString(instance.getName());
    }

    @Override
    public Reference deserialize(ExternalReferenceDeserializationHelper helper)
    {
        Reference ownerReference = helper.readElementReference();
        String qualifiedPropertyName = helper.readString();
        return new QualifiedPropertyExternalReference(ownerReference, qualifiedPropertyName);
    }

    private static class QualifiedPropertyExternalReference extends AbstractReferenceWithOwner
    {
        private final String qualifiedPropertyName;

        private QualifiedPropertyExternalReference(Reference ownerReference, String qualifiedPropertyName)
        {
            super(ownerReference);
            this.qualifiedPropertyName = qualifiedPropertyName;
        }

        @Override
        protected CoreInstance resolveFromOwner(CoreInstance owner) throws UnresolvableReferenceException
        {
            CoreInstance qualifiedProperty = owner.getValueInValueForMetaPropertyToMany(M3Properties.qualifiedProperties, this.qualifiedPropertyName);
            if (qualifiedProperty == null)
            {
                setFailureMessage("Could not find qualified property '" + this.qualifiedPropertyName + "' in " + PackageableElement.getUserPathForPackageableElement(owner));
            }
            return qualifiedProperty;
        }
    }
}
