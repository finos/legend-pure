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

package org.finos.legend.pure.runtime.java.extension.external.json.shared;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ClassConversion;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.Conversion;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.PropertyDeserialization;
import org.json.simple.JSONArray;

import java.util.Collection;

public abstract class JsonPropertyDeserialization<T> extends PropertyDeserialization<Object, T>
{
    public JsonPropertyDeserialization(AbstractProperty property, boolean isFromAssociation, Conversion<Object, T> conversion, Type type)
    {
        super(property, isFromAssociation, conversion, type);
    }

    protected RichIterable<T> applyConversion(JSONArray jsonValue, JsonDeserializationContext context)
    {
        FastList<T> values = new FastList<>();
        for (Object obj : jsonValue)
        {
            values.addAll((Collection<? extends T>)this.applyConversion(obj, context));
        }
        return values;
    }

    protected RichIterable<T> applyConversion(Object jsonValue, JsonDeserializationContext context)
    {
        FastList<T> values = new FastList<>();
        Conversion<Object, T> conversion = this.getConversion(jsonValue, context);
        T output = conversion.apply(jsonValue, context);
        if (output != null)
        {
            values.add(output);
        }
        return values;
    }

    private Conversion<Object, T> getConversion(Object jsonValue, JsonDeserializationContext context)
    {
        Conversion<Object, T> conversion = this.conversion;
        if (conversion instanceof ClassConversion)
        {
            Type resolvedType = JsonDeserializer.resolveType(this.type, jsonValue, context.getTypeKeyName(), context.getTypeLookup(), context.getSourceInformation());
            if (!resolvedType.equals(this.type))
            {
                conversion = (Conversion<Object, T>)context.getConversionCache().getConversion(resolvedType, context);
            }
        }
        return conversion;
    }
}
