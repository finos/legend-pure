// Copyright 2024 Goldman Sachs
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

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class UnitReferenceSerializer implements ExternalReferenceSerializer
{
    @Override
    public String getTypePath()
    {
        return M3Paths.Unit;
    }

    @Override
    public void serialize(CoreInstance instance, ExternalReferenceSerializationHelper helper)
    {
        Unit unit = (Unit) instance;
        helper.writeElementReference(unit._measure());
        helper.writeString(unit._name());
    }

    @Override
    public Reference deserialize(ExternalReferenceDeserializationHelper helper)
    {
        Reference measureReference = helper.readElementReference();
        String unitName = helper.readString();
        return new UnitExternalReference(measureReference, unitName);
    }

    private static class UnitExternalReference extends AbstractReferenceWithOwner
    {
        private final String unitName;

        private UnitExternalReference(Reference measureReference, String unitName)
        {
            super(measureReference);
            this.unitName = unitName;
        }

        @Override
        protected CoreInstance resolveFromOwner(CoreInstance measure) throws UnresolvableReferenceException
        {
            CoreInstance unit = Measure.findUnit(measure, this.unitName);
            if (unit == null)
            {
                setFailureMessage(PackageableElement.writeUserPathForPackageableElement(new StringBuilder("Could not find unit '").append(this.unitName).append("' in "), measure).toString());
            }
            return unit;
        }
    }
}
