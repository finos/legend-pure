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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.coreinstance.helper.AnyHelper;
import org.finos.legend.pure.m3.coreinstance.helper.ImportStubHelper;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;
import org.finos.legend.pure.m4.coreinstance.primitive.PrimitiveCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;

public abstract class ReflectiveCoreInstance extends AbstractCompiledCoreInstance
{
    private static final int DEFAULT_MAX_PRINT_DEPTH = 1;

    private static final Predicate2<Method, String> PROP_IS_TO_ONE = new Predicate2<Method, String>()
    {
        @Override
        public boolean accept(Method method, String propName)
        {
            return method.getName().equals(propName) && method.getParameterTypes().length == 1 && method.getParameterTypes()[0] != RichIterable.class;
        }
    };

    private static final Predicate<CoreInstance> IS_VALCOREINSTANCE = new Predicate<CoreInstance>()
    {
        @Override
        public boolean accept(CoreInstance coreInstance)
        {
            return coreInstance instanceof ValCoreInstance;
        }
    };

    private static final Function<CoreInstance, Object> VALCOREINSTANCE_TO_VALUE = new Function<CoreInstance, Object>()
    {
        @Override
        public Object valueOf(CoreInstance valCoreInstance)
        {
            return invokeMethodWithJavaType((ValCoreInstance)valCoreInstance);
        }
    };

    private SourceInformation sourceInformation;

    private final String __id;

    public ReflectiveCoreInstance(String id)
    {
        this.__id = id;
    }

    @Override
    public String toString()
    {
        return this.toString(null);
    }

    public String toString(ExecutionSupport executionSupport)
    {
        return ModelRepository.possiblyReplaceAnonymousId(this.__id);
    }

    @Override
    public ModelRepository getRepository()
    {
        return null;
        //TODO Clean up method signatures so CoreInstance.getRepository() is not invoked when using CompiledMode before enabling RuntimeException
        //throw new RuntimeException("TO CODE");
    }

    @Override
    public int getSyntheticId()
    {
        return -1;
    }

    @Override
    public String getName()
    {
        return __id;
    }

    @Override
    public void setName(String name)
    {
        throw new RuntimeException("TO CODE");
    }

    @Override
    public void setClassifier(CoreInstance classifier)
    {
        throw new RuntimeException("TO CODE");
    }

    @Override
    public SourceInformation getSourceInformation()
    {
        return this.sourceInformation;
    }

    @Override
    public void setSourceInformation(SourceInformation sourceInformation)
    {
        this.sourceInformation = sourceInformation;
    }

    @Override
    public boolean isPersistent()
    {
        return false;
    }

    @Override
    public void addKeyWithEmptyList(ListIterable<String> key)
    {
    }

    @Override
    public void modifyValueForToManyMetaProperty(String key, int offset, CoreInstance value)
    {
        //To enable for offset > 0, Check for the fieldType is List - key the map by key set the value at the offset
        //If not a list, validate that the offset is 0
        if (offset != 0)
        {
            throw new RuntimeException("TO CODE");
        }

        String methodName = "_" + key;
        Method[] methods = getClass().getMethods();
        for (int i = 0; i < methods.length; i++)
        {
            Method method = methods[i];
            if (methodName.equals(method.getName()))
            {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if ((parameterTypes.length == 1) && (parameterTypes[0] != RichIterable.class))
                {
                    try
                    {
                        method.invoke(this, value);
                    }
                    catch (ReflectiveOperationException e)
                    {
                        throw new RuntimeException(e);
                    }
                    return;
                }
            }
        }
        throw new IllegalArgumentException("Cannot find property '" + key + "'");
    }

    @Override
    public void removeProperty(CoreInstance propertyNameKey)
    {
        try
        {
            Method m = propertyNameKey.getClass().getMethod("_name");
            if (m != null)
            {
                final String propName = "_" + m.invoke(propertyNameKey);
                Method declMthd = ArrayAdapter.adapt(this.getClass().getMethods()).detect(new Predicate<Method>()
                {
                    @Override
                    public boolean accept(Method method)
                    {
                        return method.getName().equals(propName) && method.getParameterTypes().length == 1 && method.getParameterTypes()[0] != RichIterable.class;
                    }

                });
                Object value = null;
                if(declMthd != null)
                {
                    declMthd.invoke(this, value);
                }
            }
        }
        catch (Throwable e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CoreInstance getKeyByName(String name)
    {
        throw new RuntimeException("TO CODE");
    }

    @Override
    public CoreInstance getValueForMetaPropertyToOne(String propertyName)
    {
        Method method = getNoParameterMethodForKey(propertyName);
        if (method == null)
        {
            method = getNoParameterMethodForKey("_" + propertyName);
            if (method == null)
            {
                return null;
            }
        }

        try
        {
            Object result = method.invoke(this);
            if (result instanceof RichIterable)
            {
                RichIterable l = (RichIterable)result;
                switch (l.size())
                {
                    case 0:
                    {
                        return null;
                    }
                    case 1:
                    {
                        return ValCoreInstance.toCoreInstance(l.getFirst());
                    }
                    default:
                    {
                        throw new RuntimeException("More than one (" + l.size() + ") result is returned for the key '" + propertyName + "' for " + this);
                    }
                }
            }
            return ValCoreInstance.toCoreInstance(result);
        }
        catch (ReflectiveOperationException|RuntimeException e)
        {
            throw new RuntimeException("Error trying to access property '" + propertyName + "' for " + this, e);
        }
    }

    @Override
    public CoreInstance getValueForMetaPropertyToOne(CoreInstance property)
    {
        throw new RuntimeException("TO CODE");
    }

    @Override
    public ListIterable<CoreInstance> getValueForMetaPropertyToMany(String keyName)
    {
        Method method = getNoParameterMethodForKey(keyName);
        if (method == null)
        {
            return Lists.fixedSize.with();
        }

        try
        {
            RichIterable l = (RichIterable)method.invoke(this);
            if (l == null)
            {
                return Lists.fixedSize.with();
            }

            MutableList<CoreInstance> result = FastList.newList(l.size());
            for (Object object : l)
            {
                result.add(ValCoreInstance.toCoreInstance(object));
            }
            return result;
        }
        catch (ReflectiveOperationException|RuntimeException e)
        {
            throw new RuntimeException("Error trying to access property '" + keyName + "' for " + this, e);
        }
    }

    @Override
    public ListIterable<CoreInstance> getValueForMetaPropertyToMany(CoreInstance key)
    {
        throw new RuntimeException("TO CODE");
    }

    @Override
    public <K> CoreInstance getValueInValueForMetaPropertyToManyByIDIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        Object values;
        try
        {
            Method method = getClass().getMethod("_" + keyName);
            values = method.invoke(this);
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }

        // TODO think about how to handle non-CoreInstances
        if (values instanceof Iterable)
        {
            CoreInstance result = null;
            for (Object value : (Iterable)values)
            {
                if (value instanceof CoreInstance)
                {
                    CoreInstance instance = (CoreInstance)value;
                    if (keyInIndex.equals(indexSpec.getIndexKey(instance)))
                    {
                        if (result != null)
                        {
                            throw new RuntimeException("Invalid ID index: multiple values for key " + keyInIndex);
                        }
                        result = instance;
                    }
                }
            }
            return result;
        }
        if (values instanceof CoreInstance)
        {
            CoreInstance instance = (CoreInstance)values;
            return keyInIndex.equals(indexSpec.getIndexKey(instance)) ? instance : null;
        }
        return null;
    }

    @Override
    public <K> ListIterable<CoreInstance> getValueInValueForMetaPropertyToManyByIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        Object values;
        try
        {
            Method method = getClass().getMethod("_" + keyName);
            values = method.invoke(this);
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }

        // TODO think about how to handle non-CoreInstances
        if (values instanceof Iterable)
        {
            MutableList<CoreInstance> results = Lists.mutable.empty();
            for (Object value : (Iterable)values)
            {
                if (value instanceof CoreInstance)
                {
                    CoreInstance instance = (CoreInstance)value;
                    if (keyInIndex.equals(indexSpec.getIndexKey(instance)))
                    {
                        results.add(instance);
                    }
                }
            }
            return results;
        }
        if (values instanceof CoreInstance)
        {
            CoreInstance instance = (CoreInstance)values;
            return keyInIndex.equals(indexSpec.getIndexKey(instance)) ? Lists.immutable.with(instance) : Lists.immutable.<CoreInstance>empty();
        }
        return null;
    }


    @Override
    public boolean isValueDefinedForKey(String keyName)
    {
        throw new RuntimeException("TO CODE");
    }

    @Override
    public void removeValueForMetaPropertyToMany(String keyName, CoreInstance coreInstance)
    {
        try
        {
            Method m = this.getClass().getMethod("_" + keyName);
            MutableList l = (MutableList) m.invoke(this);
            l.remove(coreInstance);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RichIterable<String> getKeys()
    {
        throw new RuntimeException("TO CODE");
    }

    @Override
    public ListIterable<String> getRealKeyByName(String name)
    {
        throw new RuntimeException("TO CODE");
    }

    @Override
    public void validate(MutableSet<CoreInstance> doneList) throws PureCompilationException
    {
        throw new RuntimeException("TO CODE");
    }

    @Override
    public void printFull(Appendable appendable, String tab)
    {
        print(appendable, tab, true, true, DEFAULT_MAX_PRINT_DEPTH);
    }

    @Override
    public void print(Appendable appendable, String tab)
    {
        print(appendable, tab, DEFAULT_MAX_PRINT_DEPTH);
    }

    @Override
    public void print(Appendable appendable, String tab, int max)
    {
        print(appendable, tab, false, true, max);
    }

    @Override
    public void printWithoutDebug(Appendable appendable, String tab)
    {
        printWithoutDebug(appendable, tab, DEFAULT_MAX_PRINT_DEPTH);
    }

    @Override
    public void printWithoutDebug(Appendable appendable, String tab, int max)
    {
        print(appendable, tab, false, false, max);
    }

    private void print(Appendable appendable, String tab, boolean full, boolean addDebug, int max)
    {
        // TODO consider adding support for full and addDebug
        try
        {
            appendable.append(tab);
            appendable.append(ConsoleCompiled.toString(this, max));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setKeyValues(ListIterable<String> key, ListIterable<? extends CoreInstance> value)
    {
        try
        {
            String propName = "_" + key.getLast();
            Method m = ArrayIterate.detectWith(getClass().getMethods(), PROP_IS_TO_ONE, propName);
            ListIterable<?> args = value.allSatisfy(IS_VALCOREINSTANCE) ? value.collect(VALCOREINSTANCE_TO_VALUE) : value;
            if (m != null)
            {
                m.invoke(this, args.getFirst());
            }
            else
            {
                m = this.getClass().getMethod(propName, RichIterable.class);
                m.invoke(this, args);
            }
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addKeyValue(ListIterable<String> key, CoreInstance value)
    {
        try
        {
            final String propName = "_" + key.getLast();
            Method m = ArrayAdapter.adapt(this.getClass().getMethods()).detect(new Predicate<Method>()
            {
                @Override
                public boolean accept(Method method)
                {
                    return method.getName().equals(propName) && method.getParameterTypes().length == 1 && method.getParameterTypes()[0] != RichIterable.class;
                }
            });
            if (m == null)
            {
                m = ArrayAdapter.adapt(this.getClass().getMethods()).detect(new Predicate<Method>()
                {
                    @Override
                    public boolean accept(Method method)
                    {
                        return method.getName().equals(propName + "Add") && method.getParameterTypes().length == 1 && method.getParameterTypes()[0] != RichIterable.class;
                    }
                });
            }

            if (value instanceof ValCoreInstance)
            {
                m.invoke(this, invokeMethodWithJavaType((ValCoreInstance)value));
            }
            else if (value instanceof PrimitiveCoreInstance)
            {
                m.invoke(this, AnyHelper.UNWRAP_PRIMITIVES.valueOf(value));
            }
            else
            {
                m.invoke(this, ImportStubHelper.FROM_STUB_FN.valueOf(value));
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void commit(ModelRepositoryTransaction transaction)
    {
        throw new RuntimeException("TO CODE");
    }

    @Override
    public void rollback(ModelRepositoryTransaction transaction)
    {
        throw new RuntimeException("TO CODE");
    }

    private Method getNoParameterMethodForKey(String key)
    {
        try
        {
            return getClass().getMethod("_" + key);
        }
        catch (NoSuchMethodException e)
        {
            return null;
        }
    }

    private static Object invokeMethodWithJavaType(ValCoreInstance value)
    {
        String valueType = value.getType();
        if (M3Paths.String.equals(valueType))
        {
            return value.getName();
        }
        if (M3Paths.Integer.equals(valueType))
        {
            return Long.valueOf(value.getName());
        }
        if (M3Paths.Float.equals(valueType))
        {
            return Double.valueOf(value.getName());
        }
        if (M3Paths.Decimal.equals(valueType))
        {
            return new BigDecimal(value.getName());
        }
        if (M3Paths.Boolean.equals(valueType))
        {
            return Boolean.valueOf(value.getName());
        }
        if (M3Paths.Date.equals(valueType) || M3Paths.StrictDate.equals(valueType) || M3Paths.DateTime.equals(valueType))
        {
            return DateFunctions.parsePureDate(value.getName());
        }
        if (M3Paths.LatestDate.equals(valueType))
        {
            return LatestDate.instance;
        }
        throw new IllegalArgumentException("Type not supported to retrieve value from ReflectiveCoreInstance - " + valueType);
    }
}
