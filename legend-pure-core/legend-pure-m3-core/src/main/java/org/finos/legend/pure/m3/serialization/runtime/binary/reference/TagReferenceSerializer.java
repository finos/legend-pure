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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class TagReferenceSerializer implements ExternalReferenceSerializer
{
    @Override
    public String getTypePath()
    {
        return M3Paths.Tag;
    }

    @Override
    public void serialize(CoreInstance instance, ExternalReferenceSerializationHelper helper)
    {
        Tag tag = (Tag)instance;
        helper.writeElementReference(tag._profile());
        helper.writeString(tag._value());
    }

    @Override
    public Reference deserialize(ExternalReferenceDeserializationHelper helper)
    {
        Reference profileReference = helper.readElementReference();
        String tagValue = helper.readString();
        return new TagExternalReference(profileReference, tagValue);
    }

    private static class TagExternalReference extends AbstractReferenceWithOwner
    {
        private final String tagValue;

        private TagExternalReference(Reference profileReference, String tagValue)
        {
            super(profileReference);
            this.tagValue = tagValue;
        }

        @Override
        protected CoreInstance resolveFromOwner(CoreInstance profile) throws UnresolvableReferenceException
        {
            CoreInstance tag = Profile.findTag(profile, this.tagValue);
            if (tag == null)
            {
                setFailureMessage("Could not find tag '" + this.tagValue + "' in " + PackageableElement.getUserPathForPackageableElement(profile));
            }
            return tag;
        }
    }
}
