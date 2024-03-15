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

import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.block.function.checked.CheckedFunction0;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.EnumProcessor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * Creates core instances in compiled mode
 */
public final class CompiledCoreInstanceBuilder
{
    private final ClassCacheByClassifier classCacheByClassifier = new ClassCacheByClassifier();

    private final ThreadLocal<ClassCacheByClassifier> added = new ThreadLocal<>();


    public void startTransaction()
    {
        this.added.set(new ClassCacheByClassifier());
    }


    public void commitTransaction()
    {
        ClassCacheByClassifier trans = this.added.get();
        if (trans != null)
        {
            this.added.remove();
            this.classCacheByClassifier.commitChanges(trans);
        }
    }

    public void rollbackTransaction()
    {
        this.added.remove();
    }

    public void clear()
    {
        this.classCacheByClassifier.clear();
        this.added.remove();
    }


    public MapIterable<String, Field> getFields(String classifier)
    {
        MapIterable<String, Field> fields = this.classCacheByClassifier.getFields(classifier);

        if (fields == null && this.isInTransaction())
        {
            fields = this.added.get().getFields(classifier);
        }

        return fields;
    }

    public CoreInstance newCoreInstance(String classifier, String name, SourceInformation sourceInformation,
                                        ClassLoader classLoader)
    {
        String nameWithoutPath = name;
        if (nameWithoutPath.lastIndexOf("::") != -1)
        {
            nameWithoutPath = nameWithoutPath.substring(nameWithoutPath.lastIndexOf("::") + 2);
        }

        ClassCache classCache = this.getClassCache(classifier);

        if (classCache == null)
        {
            classCache = this.classCacheByClassifier.addToClassCache(classifier, classLoader);
        }

        try
        {
            CoreInstance inst = (CoreInstance)classCache.constructor.newInstance(nameWithoutPath);
            inst.setSourceInformation(sourceInformation);
            //this.getMetamodel().add(classifier, identifier, inst);
            return inst;
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    public CoreInstance newEnumCoreInstance(String classifier, String name, SourceInformation sourceInformation,
                                            ClassLoader classLoader)
    {
        ClassCache classCache = this.getClassCache(EnumProcessor.ENUM_CLASS_NAME);
        if (classCache == null)
        {
            classCache = this.classCacheByClassifier.addEnumToClassCache(EnumProcessor.ENUM_CLASS_NAME, classLoader);
        }

        try
        {
            CoreInstance inst = (CoreInstance)classCache.constructor.newInstance(name, classifier);
            inst.setSourceInformation(sourceInformation);
            //this.getMetamodel().add(classifier, name, inst);
            return inst;
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    private ClassCache getClassCache(String classifier)
    {
        ClassCache classCache = this.classCacheByClassifier.classCache.get(classifier);

        if (classCache == null && this.isInTransaction())
        {
            classCache = this.added.get().classCache.get(classifier);
        }
        return classCache;
    }

    private boolean isInTransaction()
    {
        return this.added.get() != null;
    }

    public int getClassCacheSize()
    {
        return this.classCacheByClassifier.classCache.size();
    }

    private static class ClassCacheByClassifier
    {

        private final MutableMap<String, ClassCache> classCache = UnifiedMap.newMap();

        private void clear()
        {
            this.classCache.clear();
        }

        private MapIterable<String, Field> getFields(String classifier)
        {
            return this.classCache.get(classifier).fields;
        }

        private ClassCache addToClassCache(final String classifier, final ClassLoader classLoader)
        {
            try
            {
                return this.classCache.getIfAbsentPut(classifier, new CheckedFunction0<ClassCache>()
                {
                    @Override
                    public ClassCache safeValue() throws Exception
                    {
                        Class cl = classLoader.loadClass(JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(classifier));
                        return new ClassCache(cl, true, String.class);
                    }
                });

            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }
        }


        private ClassCache addEnumToClassCache(final String classifier, final ClassLoader classLoader)
        {
            try
            {
                return this.classCache.getIfAbsentPut(classifier, new CheckedFunction0<ClassCache>()
                {
                    @Override
                    public ClassCache safeValue() throws Exception
                    {
                        Class<?> cl = classLoader.loadClass(JavaPackageAndImportBuilder.buildPackageFromSystemPath(classifier) + "." + classifier);
                        return new ClassCache(cl, true, String.class, String.class);
                    }
                });
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }
        }

        private void commitChanges(ClassCacheByClassifier toBeAdded)
        {
            toBeAdded.classCache.forEachKeyValue(new Procedure2<String, ClassCache>()
            {
                @Override
                public void value(String classifer, ClassCache classCache)
                {
                    ClassCacheByClassifier.this.classCache.getIfAbsentPut(classifer, classCache);
                }
            });
        }

    }

    private static class ClassCache
    {
        private final Constructor<?> constructor;
        private final ImmutableMap<String, Field> fields;

        private ClassCache(Class<?> cls, boolean cacheConstructor, Class<?>... parameterTypes)
        {
            this.constructor = cacheConstructor ? getConstructor(cls, parameterTypes) : null;
            this.fields = indexFieldsByName(cls);
        }

        private static Constructor<?> getConstructor(Class<?> cls, Class<?>... parameterTypes)
        {
            try
            {
                Constructor<?> constructor = cls.getDeclaredConstructor(parameterTypes);
                constructor.setAccessible(true);
                return constructor;
            }
            catch (NoSuchMethodException | SecurityException e)
            {
                throw new RuntimeException("Error finding constructor for " + cls.getCanonicalName(), e);
            }
        }

        private static ImmutableMap<String, Field> indexFieldsByName(Class<?> cls)
        {
            // Is this logic correct? What about overrides and non-override name clashes?
            MutableMap<String, Field> fieldsByName = Maps.mutable.empty();
            for (Class<?> current = cls; current != null; current = current.getSuperclass())
            {
                for (Field field : current.getDeclaredFields())
                {
                    fieldsByName.put(field.getName(), field);
                }
            }
            return fieldsByName.toImmutable();
        }
    }
}
