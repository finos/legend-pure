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

package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.EnumRef;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.ObjRef;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Primitive;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValue;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueConsumer;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueMany;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueOne;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.RValue;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.RValueConsumer;

import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;

public abstract class StringCache extends StringCacheOrIndex
{
    private final ObjectIntMap<String> classifierIds;
    private final ObjectIntMap<String> otherStrings;

    protected StringCache(ListIterable<String> classifierIds, ListIterable<String> otherStrings)
    {
        this.classifierIds = listToIndexIdMap(classifierIds, StringCacheOrIndex::classifierIdStringIndexToId);
        this.otherStrings = listToIndexIdMap(otherStrings, StringCacheOrIndex::otherStringIndexToId);
    }

    public int getStringId(String string)
    {
        if (string == null)
        {
            return 0;
        }
        int id = this.classifierIds.getIfAbsent(string, 0);
        if (id == 0)
        {
            id = this.otherStrings.getIfAbsent(string, 0);
            if (id == 0)
            {
                throw new IllegalStateException("Unknown string: '" + string + "'");
            }
        }
        return id;
    }

    @Override
    public RichIterable<String> getClassifierIds()
    {
        return this.classifierIds.keysView();
    }

    public String[] getClassifierStringArray()
    {
        return sequentialIdIndexToArray(this.classifierIds, StringCacheOrIndex::classifierIdStringIdToIndex);
    }

    public String[] getOtherStringsArray()
    {
        return sequentialIdIndexToArray(this.otherStrings, StringCacheOrIndex::otherStringIdToIndex);
    }

    private static String[] sequentialIdIndexToArray(ObjectIntMap<String> stringIds, IntUnaryOperator idToIndex)
    {
        String[] strings = new String[stringIds.size()];
        stringIds.forEachKeyValue((string, id) -> strings[idToIndex.applyAsInt(id)] = string);
        return strings;
    }

    private static ObjectIntMap<String> listToIndexIdMap(ListIterable<String> strings, IntUnaryOperator indexToId)
    {
        MutableObjectIntMap<String> map = ObjectIntMaps.mutable.ofInitialCapacity(strings.size());
        strings.forEachWithIndex((string, index) -> map.put(string, indexToId.applyAsInt(index)));
        return map;
    }

    public abstract static class Builder<T extends StringCache> implements Consumer<Obj>
    {
        private final PropertyValueConsumer propertyValueConsumer = new PropertyValueConsumer()
        {
            @Override
            protected void accept(PropertyValueMany many)
            {
                collectPropertyValueMany(many);
            }

            @Override
            protected void accept(PropertyValueOne one)
            {
                collectPropertyValueOne(one);
            }
        };

        private final RValueConsumer rValueConsumer = new RValueConsumer()
        {
            @Override
            protected void accept(Primitive primitive)
            {
                collectPrimitive(primitive);
            }

            @Override
            protected void accept(ObjRef objRef)
            {
                collectObjRef(objRef);
            }

            @Override
            protected void accept(EnumRef enumRef)
            {
                collectEnumRef(enumRef);
            }
        };

        @Override
        public final void accept(Obj obj)
        {
            collectObj(obj.getClassifier(), obj.getIdentifier(), obj.getName());
            SourceInformation sourceInfo = obj.getSourceInformation();
            if (sourceInfo != null)
            {
                collectSourceId(sourceInfo.getSourceId());
            }
            ListIterable<PropertyValue> propertyValues = obj.getPropertyValues();
            if (propertyValues != null)
            {
                propertyValues.forEach(this.propertyValueConsumer);
            }
        }

        public final Builder<T> withObj(Obj obj)
        {
            accept(obj);
            return this;
        }

        public final Builder<T> withObjs(Iterable<? extends Obj> objs)
        {
            objs.forEach(this);
            return this;
        }

        public abstract T build();

        protected abstract void collectObj(String classifierId, String identifier, String name);

        protected abstract void collectSourceId(String sourceId);

        protected abstract void collectProperty(String property);

        protected abstract void collectRef(String classifierId, String identifier);

        protected abstract void collectPrimitiveString(String string);

        private void collectPropertyValueMany(PropertyValueMany many)
        {
            collectProperty(many.getProperty());
            ListIterable<RValue> values = many.getValues();
            if (values != null)
            {
                values.forEach(this.rValueConsumer);
            }
        }

        private void collectPropertyValueOne(PropertyValueOne one)
        {
            collectProperty(one.getProperty());
            RValue value = one.getValue();
            if (value != null)
            {
                value.visit(this.rValueConsumer);
            }
        }

        private void collectPrimitive(Primitive primitive)
        {
            Object value = primitive.getValue();
            if ((value instanceof String) || (value instanceof PureDate))
            {
                collectPrimitiveString(value.toString());
            }
        }

        private void collectObjRef(ObjRef objRef)
        {
            collectRef(objRef.getClassifierId(), objRef.getId());
        }

        private void collectEnumRef(EnumRef enumRef)
        {
            collectRef(enumRef.getEnumerationId(), enumRef.getEnumName());
        }
    }
}
