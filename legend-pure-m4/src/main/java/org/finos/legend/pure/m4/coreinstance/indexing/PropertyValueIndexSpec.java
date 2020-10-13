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

package org.finos.legend.pure.m4.coreinstance.indexing;

import org.finos.legend.pure.m4.coreinstance.CoreInstance;

class PropertyValueIndexSpec extends IndexSpecification<CoreInstance>
{
    private final String propertyName;

    PropertyValueIndexSpec(String propertyName)
    {
        if (propertyName == null)
        {
            throw new IllegalArgumentException("Property name may not be null");
        }
        this.propertyName = propertyName;
    }

    @Override
    public CoreInstance getIndexKey(CoreInstance value)
    {
        return value.getValueForMetaPropertyToOne(this.propertyName);
    }

    @Override
    public boolean equals(Object other)
    {
        return (this == other) || (other != null && (this.getClass() == other.getClass()) && this.propertyName.equals(((PropertyValueIndexSpec)other).propertyName));
    }

    @Override
    public int hashCode()
    {
        return this.propertyName.hashCode();
    }
}
