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
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.profile.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class StereotypeReferenceSerializer implements ExternalReferenceSerializer
{
    @Override
    public String getTypePath()
    {
        return M3Paths.Stereotype;
    }

    @Override
    public void serialize(CoreInstance instance, ExternalReferenceSerializationHelper helper)
    {
        Stereotype stereotype = (Stereotype)instance;
        helper.writeElementReference(stereotype._profile());
        helper.writeString(PrimitiveUtilities.getStringValue(instance));
    }

    @Override
    public Reference deserialize(ExternalReferenceDeserializationHelper helper)
    {
        Reference profileReference = helper.readElementReference();
        String stereotypeValue = helper.readString();
        return new StereotypeExternalReference(profileReference, stereotypeValue);
    }

    private static class StereotypeExternalReference extends AbstractReferenceWithOwner
    {
        private final String stereotypeValue;

        private StereotypeExternalReference(Reference profileReference, String stereotypeValue)
        {
            super(profileReference);
            this.stereotypeValue = stereotypeValue;
        }

        @Override
        protected CoreInstance resolveFromOwner(CoreInstance profile) throws UnresolvableReferenceException
        {
            CoreInstance stereotype = Profile.findStereotype(profile, this.stereotypeValue);
            if (stereotype == null)
            {
                setFailureMessage("Could not find stereotype '" + this.stereotypeValue + "' in " + PackageableElement.getUserPathForPackageableElement(profile));
            }
            return stereotype;
        }
    }
}
