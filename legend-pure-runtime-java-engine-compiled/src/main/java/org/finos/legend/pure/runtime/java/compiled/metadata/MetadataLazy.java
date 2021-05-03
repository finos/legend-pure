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

package org.finos.legend.pure.runtime.java.compiled.metadata;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.EnumProcessor;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.DistributedBinaryGraphDeserializer;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Enum;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.EnumRef;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.ObjRef;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Primitive;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValue;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueMany;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueOne;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueVisitor;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.RValue;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.RValueVisitor;

import java.io.IOException;
import java.lang.reflect.Constructor;

public class MetadataLazy implements Metadata
{
    private static final PropertyValueVisitor VALUES_VISITOR = new PropertyValueVisitor()
    {
        @Override
        public Object accept(PropertyValueMany many)
        {
            return many.getValues();
        }

        @Override
        public Object accept(PropertyValueOne one)
        {
            return one.getValue();
        }
    };

    private final RValueVisitor valueToObjectVisitor = new RValueVisitor()
    {
        @Override
        public Object accept(Primitive primitive)
        {
            return primitive.getValue();
        }

        @Override
        public Object accept(ObjRef ref)
        {
            return toJavaObject(ref.getClassifierId(), ref.getId());
        }

        @Override
        public Object accept(EnumRef enumRef)
        {
            return getEnum(enumRef.getEnumerationId(), enumRef.getEnumName());
        }
    };

    private final ClassLoader classLoader;
    private final DistributedBinaryGraphDeserializer deserializer;
    private final ConcurrentMutableMap<String, Constructor<? extends CoreInstance>> constructors = ConcurrentHashMap.newMap();
    private final ConcurrentMutableMap<String, ConcurrentMutableMap<String, CoreInstance>> instanceCache = ConcurrentHashMap.newMap();
    private final ConcurrentMutableMap<String, MapIterable<String, CoreInstance>> enumCache = ConcurrentHashMap.newMap();

    private volatile Constructor<? extends CoreInstance> enumConstructor = null; //NOSONAR we actually want to protect the pointer

    public MetadataLazy(ClassLoader classLoader, DistributedBinaryGraphDeserializer deserializer)
    {
        this.classLoader = (classLoader == null) ? MetadataLazy.class.getClassLoader() : classLoader;
        this.deserializer = (deserializer == null) ? DistributedBinaryGraphDeserializer.fromClassLoader(this.classLoader) : deserializer;
    }

    public MetadataLazy(ClassLoader classLoader)
    {
        this(classLoader, null);
    }

    public MetadataLazy()
    {
        this(null, null);
    }

    @Override
    public void startTransaction()
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void commitTransaction()
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void rollbackTransaction()
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public CoreInstance getMetadata(String classifier, String id)
    {
        return this.deserializer.hasClassifier(classifier) ? toJavaObject(classifier, id) : null;
    }

    @Override
    public MapIterable<String, CoreInstance> getMetadata(String classifier)
    {
        if (!this.deserializer.hasClassifier(classifier))
        {
            return null;
        }
        loadAllClassifierInstances(classifier);
        return getClassifierInstanceCache(classifier).asUnmodifiable();
    }

    @Override
    public CoreInstance getEnum(String enumerationName, String enumName)
    {
        MapIterable<String, CoreInstance> enumerationCache = this.enumCache.getIfAbsentPutWithKey(enumerationName, this::indexEnumerationValues);
        if (enumerationCache == null)
        {
            throw new RuntimeException("Cannot find enum '" + enumName + "' in enumeration '" + enumerationName + "': unknown enumeration");
        }
        return enumerationCache.get(enumName);
    }

    private MapIterable<String, CoreInstance> indexEnumerationValues(String enumerationId)
    {
        MapIterable<String, CoreInstance> enums = getMetadata(enumerationId);
        return (enums == null) ? null : enums.groupByUniqueKey(CoreInstance::getName, Maps.mutable.withInitialCapacity(enums.size()));
    }

    public Object valueToObject(RValue value)
    {
        return (value == null) ? null : value.visit(this.valueToObjectVisitor);
    }

    public RichIterable<Object> valuesToObjects(ListIterable<RValue> values)
    {
        int size = (values == null) ? 0 : values.size();
        if (size == 0)
        {
            return Lists.immutable.empty();
        }
        if (size == 1)
        {
            return Lists.mutable.with(valueToObject(values.get(0)));
        }

        MutableSetMultimap<String, ObjRef> objRefsByClassifier = Multimaps.mutable.set.empty();
        values.forEachWith(RValue::visit, new RValueVisitor()
        {
            @Override
            public Object accept(Primitive primitive)
            {
                return null;
            }

            @Override
            public Object accept(ObjRef objRef)
            {
                objRefsByClassifier.put(objRef.getClassifierId(), objRef);
                return null;
            }

            @Override
            public Object accept(EnumRef enumRef)
            {
                return null;
            }
        });
        if (objRefsByClassifier.isEmpty())
        {
            return values.collect(this::valueToObject);
        }

        MutableMap<ObjRef, CoreInstance> objectByRef = Maps.mutable.withInitialCapacity(objRefsByClassifier.size());
        for (Pair<String, RichIterable<ObjRef>> pair : objRefsByClassifier.keyMultiValuePairsView())
        {
            String classifier = pair.getOne();
            RichIterable<ObjRef> objRefs = pair.getTwo();
            MutableList<String> idsToDeserialize = Lists.mutable.withInitialCapacity(objRefs.size());
            ConcurrentMutableMap<String, CoreInstance> classifierCache = getClassifierInstanceCache(classifier);
            for (ObjRef objRef : objRefs)
            {
                String id = objRef.getId();
                CoreInstance cachedInstance = classifierCache.get(id);
                if (cachedInstance == null)
                {
                    idsToDeserialize.add(id);
                }
                else
                {
                    objectByRef.put(objRef, cachedInstance);
                }
            }
            if (idsToDeserialize.notEmpty())
            {
                try
                {
                    for (Obj obj : this.deserializer.getInstances(classifier, idsToDeserialize))
                    {
                        CoreInstance cachedInstance = classifierCache.getIfAbsentPut(obj.getIdentifier(), () -> newInstance(classifier, obj));
                        objectByRef.put(new ObjRef(obj.getClassifier(), obj.getIdentifier()), cachedInstance);
                    }
                }
                catch (IOException e)
                {
                    throw new RuntimeException("Error deserializing instances of " + classifier, e);
                }
            }
        }
        return values.collectWith(RValue::visit, new RValueVisitor()
        {
            @Override
            public Object accept(Primitive primitive)
            {
                return primitive.getValue();
            }

            @Override
            public Object accept(ObjRef objRef)
            {
                return objectByRef.get(objRef);
            }

            @Override
            public Object accept(EnumRef enumRef)
            {
                return getEnum(enumRef.getEnumerationId(), enumRef.getEnumName());
            }
        });
    }

    public ImmutableMap<String, Object> buildMap(Obj instance)
    {
        return instance.getPropertyValues().toMap(PropertyValue::getProperty, pv -> pv.visit(VALUES_VISITOR)).toImmutable();
    }

    private void loadAllClassifierInstances(String classifier)
    {
        RichIterable<String> instanceIds = this.deserializer.getClassifierInstanceIds(classifier);
        ConcurrentMutableMap<String, CoreInstance> classifierCache = getClassifierInstanceCache(classifier);
        int notLoadedCount = instanceIds.size() - classifierCache.size();
        if (notLoadedCount > 0)
        {
            MutableList<String> instanceIdsToLoad = instanceIds.reject(classifierCache::containsKey, Lists.mutable.withInitialCapacity(notLoadedCount));
            ListIterable<Obj> objs;
            try
            {
                objs = this.deserializer.getInstances(classifier, instanceIdsToLoad);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error loading all instances for classifier: " + classifier, e);
            }
            objs.forEach(obj -> classifierCache.getIfAbsentPut(obj.getIdentifier(), () -> newInstance(classifier, obj)));
        }
    }

    private CoreInstance toJavaObject(String classifier, String id)
    {
        return getClassifierInstanceCache(classifier).getIfAbsentPut(id, () -> newInstance(classifier, id));
    }

    private ConcurrentMutableMap<String, CoreInstance> getClassifierInstanceCache(String classifier)
    {
        return this.instanceCache.getIfAbsentPut(classifier, ConcurrentHashMap::newMap);
    }

    private CoreInstance newInstance(String classifier, String id)
    {
        Obj obj;
        try
        {
            obj = this.deserializer.getInstance(classifier, id);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error loading instance '" + id + "' with classifier '" + classifier + "'", e);
        }
        return newInstance(classifier, obj);
    }

    private CoreInstance newInstance(String classifier, Obj obj)
    {
        Constructor<? extends CoreInstance> constructor = getConstructor(classifier, obj);
        try
        {
            return constructor.newInstance(obj, this);
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException("Error instantiating " + obj, e);
        }
    }

    private Constructor<? extends CoreInstance> getConstructor(String classifier, Obj obj)
    {
        if (obj instanceof Enum)
        {
            Constructor<? extends CoreInstance> constructor = this.enumConstructor;
            if (constructor == null)
            {
                synchronized (this)
                {
                    constructor = this.enumConstructor;
                    if (constructor == null)
                    {
                        this.enumConstructor = constructor = getLazyImplEnumConstructor();
                    }
                }
            }
            return constructor;
        }
        return this.constructors.getIfAbsentPutWithKey(classifier, this::getLazyImplClassConstructor);
    }

    private Constructor<? extends CoreInstance> getLazyImplClassConstructor(String classifier)
    {
        String lazyImplClassName = JavaPackageAndImportBuilder.buildLazyImplClassReferenceFromUserPath(classifier);
        try
        {
            Class<? extends CoreInstance> cls = (Class<? extends CoreInstance>) this.classLoader.loadClass(lazyImplClassName);
            return cls.getConstructor(Obj.class, MetadataLazy.class);
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException("Error getting constructor for " + classifier, e);
        }
    }

    private Constructor<? extends CoreInstance> getLazyImplEnumConstructor()
    {
        String lazyImplEnumName = JavaPackageAndImportBuilder.rootPackage() + '.' + EnumProcessor.ENUM_LAZY_CLASS_NAME;
        try
        {
            Class<? extends CoreInstance> cls = (Class<? extends CoreInstance>) this.classLoader.loadClass(lazyImplEnumName);
            Constructor<? extends CoreInstance> constructor = cls.getDeclaredConstructor(Obj.class, MetadataLazy.class);
            constructor.setAccessible(true);
            return constructor;
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException("Error getting constructor for " + lazyImplEnumName);
        }
    }
}
