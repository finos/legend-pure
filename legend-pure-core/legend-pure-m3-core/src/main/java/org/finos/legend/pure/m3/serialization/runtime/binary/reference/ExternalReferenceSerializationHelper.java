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

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public interface ExternalReferenceSerializationHelper
{
    CoreInstance getPropertyValueToOne(CoreInstance instance, String propertyName);

    ListIterable<? extends CoreInstance> getPropertyValueToMany(CoreInstance instance, String propertyName);

    void writeByte(byte b);

    void writeInt(int i);

    void writeString(String string);

    void writeElementReference(CoreInstance element);
}
