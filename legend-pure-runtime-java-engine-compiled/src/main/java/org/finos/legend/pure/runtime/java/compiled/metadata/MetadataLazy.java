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
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.block.factory.Procedures;
import org.eclipse.collections.impl.block.function.checked.CheckedFunction;
import org.eclipse.collections.impl.block.function.checked.CheckedFunction0;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
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
    private static final Function0<ConcurrentMutableMap<String, CoreInstance>> NEW_CLASSIFIER_INSTANCE_CACHE = new Function0<ConcurrentMutableMap<String, CoreInstance>>()
    {
        @Override
        public ConcurrentMutableMap<String, CoreInstance> value()
        {
            return ConcurrentHashMap.newMap();
        }
    };

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

    private final Function<String, Class<?>> loadClass = new CheckedFunction<String, Class<?>>()
    {
        @Override
        public Class<?> safeValueOf(String className) throws ClassNotFoundException
        {
            return MetadataLazy.this.classLoader.loadClass(JavaPackageAndImportBuilder.buildPackageFromSystemPath(className) + '.' + className);
        }
    };

    private final Function<RValue, Object> valueToObject = new Function<RValue, Object>()
    {
        @Override
        public Object valueOf(RValue value)
        {
            return valueToObject(value);
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

    private final Function<String, MapIterable<String, CoreInstance>> indexEnumerationValues = new Function<String, MapIterable<String, CoreInstance>>()
    {
        @Override
        public MapIterable<String, CoreInstance> valueOf(String enumerationId)
        {
            MapIterable<String, CoreInstance> enums = getMetadata(enumerationId);
            return (enums == null) ? null : enums.groupByUniqueKey(CoreInstance.GET_NAME, UnifiedMap.<String, CoreInstance>newMap(enums.size()));
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
        MapIterable<String, CoreInstance> enumerationCache = this.enumCache.getIfAbsentPutWithKey(enumerationName, this.indexEnumerationValues);
        if (enumerationCache == null)
        {
            throw new RuntimeException("Cannot find enum '" + enumName + "' in enumeration '" + enumerationName + "': unknown enumeration");
        }
        return enumerationCache.get(enumName);
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

        final MutableSetMultimap<String, ObjRef> objRefsByClassifier = Multimaps.mutable.set.empty();
        values.forEach(Procedures.bind(RValue.VISIT_PROCEDURE, new RValueVisitor()
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
        }));
        if (objRefsByClassifier.isEmpty())
        {
            return values.collect(this.valueToObject);
        }

        final MutableMap<ObjRef, CoreInstance> objectByRef = UnifiedMap.newMap(objRefsByClassifier.size());
        for (Pair<String, RichIterable<ObjRef>> pair : objRefsByClassifier.keyMultiValuePairsView())
        {
            final String classifier = pair.getOne();
            RichIterable<ObjRef> objRefs = pair.getTwo();
            MutableList<String> idsToDeserialize = FastList.newList(objRefs.size());
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
                    for (final Obj obj : this.deserializer.getInstances(classifier, idsToDeserialize))
                    {
                        CoreInstance cachedInstance = classifierCache.getIfAbsentPut(obj.getIdentifier(), new Function0<CoreInstance>()
                        {
                            @Override
                            public CoreInstance value()
                            {
                                return newInstance(classifier, obj);
                            }
                        });
                        objectByRef.put(new ObjRef(obj.getClassifier(), obj.getIdentifier()), cachedInstance);
                    }
                }
                catch (IOException e)
                {
                    throw new RuntimeException("Error deserializing instances of " + classifier, e);
                }
            }
        }
        return values.collect(Functions.bind(RValue.VISIT, new RValueVisitor()
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
        }));
    }

    public ImmutableMap<String, Object> buildMap(Obj instance)
    {
        return instance.getPropertyValues().toMap(PropertyValue.GET_PROPERY, Functions.bind(PropertyValue.VISIT, VALUES_VISITOR)).toImmutable();
    }

    private void loadAllClassifierInstances(String classifier)
    {
        RichIterable<String> instanceIds = this.deserializer.getClassifierInstanceIds(classifier);
        ConcurrentMutableMap<String, CoreInstance> classifierCache = getClassifierInstanceCache(classifier);
        int notLoadedCount = instanceIds.size() - classifierCache.size();
        if (notLoadedCount > 0)
        {
            MutableList<String> instanceIdsToLoad = instanceIds.reject(Predicates.in(classifierCache.keySet()), FastList.<String>newList(notLoadedCount));
            ListIterable<Obj> objs;
            try
            {
                objs = this.deserializer.getInstances(classifier, instanceIdsToLoad);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error loading all instances for classifier: " + classifier, e);
            }
            for (Obj obj : objs)
            {
                toJavaObject(obj);
            }
        }
    }

    private CoreInstance toJavaObject(final String classifier, final String id)
    {
        return toJavaObject(classifier, id, new CheckedFunction0<CoreInstance>()
        {
            @Override
            public CoreInstance safeValue() throws Exception
            {
                return newInstance(classifier, MetadataLazy.this.deserializer.getInstance(classifier, id));
            }
        });
    }

    private CoreInstance toJavaObject(final Obj obj)
    {
        final String classifier = obj.getClassifier();
        String id = obj.getIdentifier();
        return toJavaObject(classifier, id, new Function0<CoreInstance>()
        {
            @Override
            public CoreInstance value()
            {
                return newInstance(classifier, obj);
            }
        });
    }

    private CoreInstance toJavaObject(String classifier, String id, Function0<CoreInstance> builder)
    {
        return getClassifierInstanceCache(classifier).getIfAbsentPut(id, builder);
    }

    private ConcurrentMutableMap<String, CoreInstance> getClassifierInstanceCache(String classifier)
    {
        return this.instanceCache.getIfAbsentPut(classifier, NEW_CLASSIFIER_INSTANCE_CACHE);
    }

    private Constructor<? extends CoreInstance> getConstructor(final String _class)
    {
        return this.constructors.getIfAbsentPut(_class, new CheckedFunction0<Constructor<? extends CoreInstance>>()
        {
            @Override
            public Constructor<? extends CoreInstance> safeValue() throws Exception
            {
                return ((Class<? extends CoreInstance>)MetadataLazy.this.classLoader.loadClass(JavaPackageAndImportBuilder.buildLazyImplClassReferenceFromUserPath(_class))).getConstructor(Obj.class, MetadataLazy.class);
            }
        });
    }

    private CoreInstance newInstance(String classifier, Obj obj)
    {
        try
        {
            if (obj instanceof Enum)
            {
                if (this.enumConstructor == null)
                {
                    synchronized (this)
                    {
                        if (this.enumConstructor == null)
                        {
                            Constructor<? extends CoreInstance> constructor = (Constructor<? extends CoreInstance>)this.classLoader.loadClass(JavaPackageAndImportBuilder.rootPackage() + '.' + EnumProcessor.ENUM_LAZY_CLASS_NAME).getDeclaredConstructor(Obj.class, MetadataLazy.class);
                            constructor.setAccessible(true);
                            this.enumConstructor = constructor;
                        }
                    }
                }
                return this.enumConstructor.newInstance(obj, this);
            }
            else
            {
                return getConstructor(classifier).newInstance(obj, this);
            }
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException("Error instantiating " + obj, e);
        }
    }
}
