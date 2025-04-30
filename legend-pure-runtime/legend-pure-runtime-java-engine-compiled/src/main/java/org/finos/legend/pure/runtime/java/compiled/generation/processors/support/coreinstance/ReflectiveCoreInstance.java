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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.coreinstance.helper.AnyHelper;
import org.finos.legend.pure.m3.coreinstance.helper.AnyStubHelper;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;

public abstract class ReflectiveCoreInstance extends AbstractCompiledCoreInstance
{
    private static final int DEFAULT_MAX_PRINT_DEPTH = 1;

    private final String __id;
    private SourceInformation sourceInformation;

    protected ReflectiveCoreInstance(String id, SourceInformation sourceInformation)
    {
        this.__id = id;
        this.sourceInformation = sourceInformation;
    }

    protected ReflectiveCoreInstance(String id)
    {
        this(id, null);
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
        return this.__id;
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
        String methodName = "_" + key;
        Method setMethod = ArrayIterate.detect(getClass().getMethods(), m -> (m.getParameterCount() == 1) && methodName.equals(m.getName()) && (m.getParameterTypes()[0] == RichIterable.class));
        if (setMethod == null)
        {
            throw new IllegalArgumentException("Cannot find property '" + key + "'");
        }

        Object newValue = toJavaForInvocation(value);
        Object rawCurrentValue = getRawValueForMetaProperty(key);
        MutableList<Object> newValues;
        if (rawCurrentValue == null)
        {
            newValues = Lists.mutable.empty();
        }
        else if (rawCurrentValue instanceof Iterable)
        {
            newValues = Lists.mutable.withAll((Iterable<?>) rawCurrentValue);
        }
        else
        {
            newValues = Lists.mutable.with(rawCurrentValue);
        }
        if ((offset == 0) && newValues.isEmpty())
        {
            newValues.add(newValue);
        }
        else
        {
            newValues.set(offset, newValue);
        }

        try
        {
            setMethod.invoke(this, newValues);
        }
        catch (InvocationTargetException e)
        {
            Throwable cause = e.getCause();
            StringBuilder builder = new StringBuilder("Error trying to modify value of property '").append(key).append("' at offset ").append(offset).append(" for ").append(this);
            String eMessage = cause.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw new RuntimeException(builder.toString(), cause);
        }
        catch (IllegalAccessException e)
        {
            StringBuilder builder = new StringBuilder("Error trying to modify value of property '").append(key).append("' at offset ").append(offset).append(" for ").append(this);
            String eMessage = e.getMessage();
            if (eMessage == null)
            {
                builder.append(": illegal access");
            }
            else
            {
                builder.append(": ").append(eMessage);
            }
            throw new RuntimeException(builder.toString(), e);
        }
        catch (Exception e)
        {
            StringBuilder builder = new StringBuilder("Error trying to modify value of property '").append(key).append("' at offset ").append(offset).append(" for ").append(this);
            String eMessage = e.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw new RuntimeException(builder.toString(), e);
        }
    }

    @Override
    public void removeProperty(String propertyName)
    {
        Method m = getRemoveAllMethodForKey(propertyName);
        if (m != null)
        {
            try
            {
                m.invoke(this);
            }
            catch (InvocationTargetException e)
            {
                Throwable cause = e.getCause();
                StringBuilder builder = new StringBuilder("Error trying to remove value for property '").append(propertyName).append("' for ").append(this);
                String eMessage = cause.getMessage();
                if (eMessage != null)
                {
                    builder.append(": ").append(eMessage);
                }
                throw new RuntimeException(builder.toString(), cause);
            }
            catch (IllegalAccessException e)
            {
                StringBuilder builder = new StringBuilder("Error trying to remove value for property '").append(propertyName).append("' for ").append(this);
                String eMessage = e.getMessage();
                if (eMessage == null)
                {
                    builder.append(": illegal access");
                }
                else
                {
                    builder.append(": ").append(eMessage);
                }
                throw new RuntimeException(builder.toString(), e);
            }
            catch (Exception e)
            {
                StringBuilder builder = new StringBuilder("Error trying to remove value for property '").append(propertyName).append("' for ").append(this);
                String eMessage = e.getMessage();
                if (eMessage != null)
                {
                    builder.append(": ").append(eMessage);
                }
                throw new RuntimeException(builder.toString(), e);
            }
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
        Object result = getRawValueForMetaProperty(propertyName);

        if (result == null)
        {
            return null;
        }

        if (!(result instanceof RichIterable))
        {
            return ValCoreInstance.toCoreInstance(result);
        }

        RichIterable<?> l = (RichIterable<?>) result;
        switch (l.size())
        {
            case 0:
            {
                return null;
            }
            case 1:
            {
                return ValCoreInstance.toCoreInstance(l.getAny());
            }
            default:
            {
                throw new RuntimeException("More than one (" + l.size() + ") result is returned for the key '" + propertyName + "' for " + this);
            }
        }
    }

    @Override
    public ListIterable<CoreInstance> getValueForMetaPropertyToMany(String keyName)
    {
        Object result = getRawValueForMetaProperty(keyName);

        if (result == null)
        {
            return Lists.fixedSize.empty();
        }

        if (!(result instanceof RichIterable))
        {
            return Lists.fixedSize.with(ValCoreInstance.toCoreInstance(result));
        }

        return ValCoreInstance.toCoreInstances((RichIterable<?>) result);
    }

    @Override
    public <K> CoreInstance getValueInValueForMetaPropertyToManyByIDIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        Object values = getRawValueForMetaProperty(keyName);

        // TODO think about how to handle non-CoreInstances
        if (values instanceof Iterable)
        {
            CoreInstance result = null;
            for (Object value : (Iterable<?>) values)
            {
                if (value instanceof CoreInstance)
                {
                    CoreInstance instance = (CoreInstance) value;
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
            CoreInstance instance = (CoreInstance) values;
            return keyInIndex.equals(indexSpec.getIndexKey(instance)) ? instance : null;
        }
        return null;
    }

    @Override
    public <K> ListIterable<CoreInstance> getValueInValueForMetaPropertyToManyByIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        Object values = getRawValueForMetaProperty(keyName);

        // TODO think about how to handle non-CoreInstances
        if (values instanceof Iterable)
        {
            MutableList<CoreInstance> results = Lists.mutable.empty();
            for (Object value : (Iterable<?>) values)
            {
                if (value instanceof CoreInstance)
                {
                    CoreInstance instance = (CoreInstance) value;
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
            CoreInstance instance = (CoreInstance) values;
            return keyInIndex.equals(indexSpec.getIndexKey(instance)) ? Lists.immutable.with(instance) : Lists.immutable.empty();
        }
        return null;
    }

    @Override
    public boolean isValueDefinedForKey(String keyName)
    {
        Object value = getRawValueForMetaProperty(keyName);
        return (value != null) && !((value instanceof Iterable) && Iterate.isEmpty((Iterable<?>) value));
    }

    @Override
    public void removeValueForMetaPropertyToMany(String keyName, CoreInstance coreInstance)
    {
        throw new RuntimeException("TO CODE");
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
    public void print(Appendable appendable, String tab, int max)
    {
        print(appendable, tab, false, true, max);
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
        String propertyName = key.getLast();
        String methodName = "_" + propertyName;
        Method method = ArrayIterate.detect(getClass().getMethods(), m -> (m.getParameterCount() == 1) && methodName.equals(m.getName()) && (m.getParameterTypes()[0] == RichIterable.class));
        if (method == null)
        {
            throw new IllegalArgumentException("Could not find property '" + propertyName + "' for " + this);
        }

        ListIterable<Object> args = value.collect(ReflectiveCoreInstance::toJavaForInvocation);
        try
        {
            method.invoke(this, args);
        }
        catch (InvocationTargetException e)
        {
            Throwable cause = e.getCause();
            StringBuilder builder = new StringBuilder("Error trying to set property '").append(propertyName).append("' for ").append(this);
            String eMessage = cause.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw new RuntimeException(builder.toString(), cause);
        }
        catch (IllegalAccessException e)
        {
            StringBuilder builder = new StringBuilder("Error trying to set property '").append(propertyName).append("' for ").append(this);
            String eMessage = e.getMessage();
            if (eMessage == null)
            {
                builder.append(": illegal access");
            }
            else
            {
                builder.append(": ").append(eMessage);
            }
            throw new RuntimeException(builder.toString(), e);
        }
        catch (Exception e)
        {
            StringBuilder builder = new StringBuilder("Error trying to set property '").append(propertyName).append("' for ").append(this);
            String eMessage = e.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw new RuntimeException(builder.toString(), e);
        }
    }

    @Override
    public void addKeyValue(ListIterable<String> key, CoreInstance value)
    {
        String propertyName = key.getLast();
        Method[] allMethods = getClass().getMethods();

        // Try to find the set value method for a to-one property
        String setOneMethodName = "_" + propertyName;
        Method method = ArrayIterate.detect(allMethods, m -> (m.getParameterCount() == 1) && setOneMethodName.equals(m.getName()) && (m.getParameterTypes()[0] != RichIterable.class));
        if (method == null)
        {
            // Try to find the add value method for a to-many property
            String addOneMethodName = setOneMethodName + "Add";
            method = ArrayIterate.detect(allMethods, m -> (m.getParameterCount() == 1) && addOneMethodName.equals(m.getName()) && (m.getParameterTypes()[0] != RichIterable.class));
            if (method == null)
            {
                throw new IllegalArgumentException("Unknown property '" + propertyName + "'");
            }
        }

        Object invocationValue = toJavaForInvocation(value);
        try
        {
            method.invoke(this, invocationValue);
        }
        catch (InvocationTargetException e)
        {
            Throwable cause = e.getCause();
            StringBuilder builder = new StringBuilder("Error trying to add value to property '").append(propertyName).append("' for ").append(this);
            String eMessage = cause.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw new RuntimeException(builder.toString(), cause);
        }
        catch (IllegalAccessException e)
        {
            StringBuilder builder = new StringBuilder("Error trying to add value to property '").append(propertyName).append("' for ").append(this);
            String eMessage = e.getMessage();
            if (eMessage == null)
            {
                builder.append(": illegal access");
            }
            else
            {
                builder.append(": ").append(eMessage);
            }
            throw new RuntimeException(builder.toString(), e);
        }
        catch (Exception e)
        {
            StringBuilder builder = new StringBuilder("Error trying to add value to property '").append(propertyName).append("' for ").append(this);
            String eMessage = e.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw new RuntimeException(builder.toString(), e);
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

    public abstract String getFullSystemPath();

    private Method getGetMethodForKey(String key)
    {
        return getNoParameterMethod("_" + key);
    }

    private Method getRemoveAllMethodForKey(String key)
    {
        return getNoParameterMethod("_" + key + "Remove");
    }

    private Method getNoParameterMethod(String methodName)
    {
        try
        {
            return getClass().getMethod(methodName);
        }
        catch (NoSuchMethodException e)
        {
            return null;
        }
    }

    private Object getRawValueForMetaProperty(String propertyName)
    {
        Method method = getGetMethodForKey(propertyName);
        if (method == null)
        {
            return null;
        }

        try
        {
            return method.invoke(this);
        }
        catch (InvocationTargetException e)
        {
            Throwable cause = e.getCause();
            StringBuilder builder = new StringBuilder("Error trying to access property '").append(propertyName).append("' for ").append(this);
            String eMessage = cause.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw new RuntimeException(builder.toString(), cause);
        }
        catch (IllegalAccessException e)
        {
            StringBuilder builder = new StringBuilder("Error trying to access property '").append(propertyName).append("' for ").append(this);
            String eMessage = e.getMessage();
            if (eMessage == null)
            {
                builder.append(": illegal access");
            }
            else
            {
                builder.append(": ").append(eMessage);
            }
            throw new RuntimeException(builder.toString(), e);
        }
        catch (Exception e)
        {
            StringBuilder builder = new StringBuilder("Error trying to access property '").append(propertyName).append("' for ").append(this);
            String eMessage = e.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw new RuntimeException(builder.toString(), e);
        }
    }

    private static Object toJavaForInvocation(CoreInstance instance)
    {
        if (instance instanceof ValCoreInstance)
        {
            return invokeMethodWithJavaType((ValCoreInstance) instance);
        }

        if (instance instanceof PrimitiveCoreInstance)
        {
            return AnyHelper.unwrapPrimitives(instance);
        }

        return AnyStubHelper.fromStub(instance);
    }

    private static Object invokeMethodWithJavaType(ValCoreInstance value)
    {
        String valueType = value.getType();
        if (valueType == null)
        {
            throw new IllegalArgumentException("value type may not be null");
        }
        switch (valueType)
        {
            case M3Paths.String:
            {
                return value.getName();
            }
            case M3Paths.Integer:
            {
                return Long.valueOf(value.getName());
            }
            case M3Paths.Float:
            {
                return Double.valueOf(value.getName());
            }
            case M3Paths.Decimal:
            {
                return new BigDecimal(value.getName());
            }
            case M3Paths.Boolean:
            {
                return Boolean.valueOf(value.getName());
            }
            case M3Paths.Date:
            case M3Paths.StrictDate:
            case M3Paths.DateTime:
            {
                return DateFunctions.parsePureDate(value.getName());
            }
            case M3Paths.LatestDate:
            {
                return LatestDate.instance;
            }
            default:
            {
                throw new IllegalArgumentException("Type not supported to retrieve value from ReflectiveCoreInstance - " + valueType);
            }
        }
    }
}
