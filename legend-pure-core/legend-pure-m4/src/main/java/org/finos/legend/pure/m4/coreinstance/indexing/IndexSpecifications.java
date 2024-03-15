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

public class IndexSpecifications
{
    private IndexSpecifications()
    {
        // Static factory
    }

    public static IndexSpecification<String> getCoreInstanceNameIndexSpec()
    {
        return CoreInstanceNameIndexSpec.INDEX_KEY_FUNCTION;
    }

    public static IndexSpecification<CoreInstance> getPropertyValueIndexSpec(String propertyName)
    {
        // TODO should we cache these?
        return new PropertyValueIndexSpec(propertyName);
    }

    public static IndexSpecification<String> getPropertyValueNameIndexSpec(String propertyName)
    {
        return compose(getPropertyValueIndexSpec(propertyName), getCoreInstanceNameIndexSpec());
    }

    public static IndexSpecification<String> getSourceInfoSourceIdIndexSpec()
    {
        return SourceInfoSourceIdIndexSpec.INDEX_KEY_FUNCTION;
    }

    public static <K> IndexSpecification<K> compose(IndexSpecification<? extends CoreInstance> first, IndexSpecification<K> second)
    {
        return new ComposedIndexSpec<>(first, second);
    }
}
