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

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.serialization.GraphSerializer;
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

abstract class AbstractStringCache implements StringCache
{
    private final ObjectIntMap<String> classifierIds;
    private final ObjectIntMap<String> otherStrings;

    protected AbstractStringCache(ObjectIntMap<String> classifierIds, ObjectIntMap<String> otherStrings)
    {
        this.classifierIds = classifierIds;
        this.otherStrings = otherStrings;
    }

    @Override
    public int getStringId(String string)
    {
        int id = this.classifierIds.getIfAbsent(string, -1);
        if (id == -1)
        {
            id = this.otherStrings.getIfAbsent(string, -1);
            if (id == -1)
            {
                throw new IllegalStateException("Unknown string: '" + string + "'");
            }
        }
        return id;
    }

    protected String[] getClassifierStringArray()
    {
        return sequentialIdIndexToArray(this.classifierIds, 0);
    }

    protected String[] getOtherStringsArray()
    {
        return sequentialIdIndexToArray(this.otherStrings, this.classifierIds.size());
    }

    private static String[] sequentialIdIndexToArray(ObjectIntMap<String> stringIds, int idOffset)
    {
        String[] strings = new String[stringIds.size()];
        stringIds.forEachKeyValue((string, id) -> strings[id - idOffset] = string);
        return strings;
    }

    protected static ObjectIntMap<String> listToIndexIdMap(ListIterable<String> strings, int idOffset)
    {
        MutableObjectIntMap<String> index = ObjectIntMaps.mutable.ofInitialCapacity(strings.size());
        strings.forEachWithIndex((string, id) -> index.put(string, id + idOffset));
        return index;
    }

    protected static void collectStrings(StringCollector collector, Iterable<? extends CoreInstance> nodes, IdBuilder idBuilder, ProcessorSupport processorSupport)
    {
        AbstractStringCache.PropertyValueCollectorVisitor propertyValueVisitor = new AbstractStringCache.PropertyValueCollectorVisitor(collector);
        GraphSerializer.ClassifierCaches classifierCaches = new GraphSerializer.ClassifierCaches(processorSupport);
        nodes.forEach(node -> collectStringsFromObj(collector, propertyValueVisitor, GraphSerializer.buildObj(node, idBuilder, classifierCaches, processorSupport)));
    }

    protected static void collectStringsFromObj(StringCollector collector, PropertyValueCollectorVisitor propertyValueVisitor, Obj obj)
    {
        collector.collectObj(obj.getClassifier(), obj.getIdentifier(), obj.getName());
        SourceInformation sourceInfo = obj.getSourceInformation();
        if (sourceInfo != null)
        {
            collector.collectSourceId(sourceInfo.getSourceId());
        }
        ListIterable<PropertyValue> propertyValues = obj.getPropertyValues();
        if (propertyValues != null)
        {
            propertyValues.forEach(propertyValueVisitor);
        }
    }

    protected interface StringCollector
    {
        void collectObj(String classifierId, String identifier, String name);
        void collectSourceId(String sourceId);
        void collectProperty(String property);
        void collectRef(String classifierId, String identifier);
        void collectPrimitiveString(String string);
    }

    protected static class PropertyValueCollectorVisitor extends PropertyValueConsumer
    {
        private final StringCollector collector;
        private final RValueCollectorVisitor rValueVisitor;

        protected PropertyValueCollectorVisitor(StringCollector collector)
        {
            this.collector = collector;
            this.rValueVisitor = new RValueCollectorVisitor(collector);
        }

        @Override
        protected void accept(PropertyValueMany many)
        {
            commonCollection(many);
            ListIterable<RValue> values = many.getValues();
            if (values != null)
            {
                values.forEach(this.rValueVisitor);
            }
        }

        @Override
        protected void accept(PropertyValueOne one)
        {
            commonCollection(one);
            RValue value = one.getValue();
            if (value != null)
            {
                value.visit(this.rValueVisitor);
            }
        }

        private void commonCollection(PropertyValue value)
        {
            this.collector.collectProperty(value.getProperty());
        }
    }

    private static class RValueCollectorVisitor extends RValueConsumer
    {
        private final StringCollector collector;

        private RValueCollectorVisitor(StringCollector collector)
        {
            this.collector = collector;
        }

        @Override
        protected void accept(Primitive primitive)
        {
            Object value = primitive.getValue();
            if ((value instanceof String) || (value instanceof PureDate))
            {
                this.collector.collectPrimitiveString(value.toString());
            }
        }

        @Override
        protected void accept(ObjRef objRef)
        {
            this.collector.collectRef(objRef.getClassifierId(), objRef.getId());
        }

        @Override
        protected void accept(EnumRef enumRef)
        {
            this.collector.collectRef(enumRef.getEnumerationId(), enumRef.getEnumName());
        }
    }
}
