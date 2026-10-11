// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.rust.compiled;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.rust.PureRustEvaluationException;
import org.finos.legend.pure.rust.RustCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Rust objects as the compiled engine represents objects: instances of the
 * generated {@code _Impl} class of their classifier, which is what generated
 * code casts to. The interpreted engine works on any {@link CoreInstance}, so
 * a {@link RustCoreInstance} proxy serves it; generated code does not.
 *
 * <p>Objects are copied after the call that returned them has ended; one
 * conversion keeps identity and cycles, and releases the proxies it read.
 * Slots the evaluator keeps for itself ({@code __}-prefixed) are not
 * properties and are not copied; any other key the class does not declare
 * is a typed failure, not a dropped value.
 */
public final class CompiledObjects
{
    private static final String EVALUATOR_SLOT_PREFIX = "__";

    private final CompiledExecutionSupport executionSupport;
    private final ProcessorSupport processorSupport;
    private final Map<RustCoreInstance, Object> converted = new IdentityHashMap<>();

    public CompiledObjects(CompiledExecutionSupport executionSupport)
    {
        this.executionSupport = executionSupport;
        this.processorSupport = executionSupport.getProcessorSupport();
    }

    /**
     * A value the bridge materialised for the compiled engine, as generated
     * code holds it.
     */
    public Object toCompiled(CoreInstance value)
    {
        if (value instanceof RustCoreInstance)
        {
            return object((RustCoreInstance) value);
        }
        return CompiledValue.unwrap(value);
    }

    /**
     * Release every Rust object this conversion read: the compiled copies are
     * all that remains of them.
     */
    public void release()
    {
        for (RustCoreInstance proxy : this.converted.keySet())
        {
            proxy.close();
        }
        this.converted.clear();
    }

    private Object object(RustCoreInstance proxy)
    {
        Object done = this.converted.get(proxy);
        if (done != null)
        {
            return done;
        }
        Class<?> implClass = implClass(proxy.getClassifierPath());
        Object instance = newInstance(implClass, proxy.getName());
        this.converted.put(proxy, instance);
        Set<String> declared = this.processorSupport.class_getSimplePropertiesByName(proxy.getClassifier()).keysView().toSet();
        for (String key : proxy.getKeys())
        {
            if (key.startsWith(EVALUATOR_SLOT_PREFIX))
            {
                continue;
            }
            if (!declared.contains(key))
            {
                throw PureRustEvaluationException.unsupported("property `" + key + "` of a Rust " + proxy.getClassifierPath() + " object, which the class does not declare, crossing into the compiled engine");
            }
            ListIterable<? extends CoreInstance> values = proxy.getValueForMetaPropertyToMany(key);
            MutableList<Object> compiled = Lists.mutable.ofInitialCapacity(values.size());
            for (CoreInstance v : values)
            {
                compiled.add(toCompiled(v));
            }
            set(implClass, instance, key, compiled);
        }
        return instance;
    }

    private Class<?> implClass(String classifierPath)
    {
        try
        {
            return this.executionSupport.getClassLoader().loadClass(JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(classifierPath));
        }
        catch (ClassNotFoundException e)
        {
            throw PureRustEvaluationException.unsupported("a Rust " + classifierPath + " object crossing into the compiled engine, which has no generated class for it");
        }
    }

    private static Object newInstance(Class<?> implClass, String name)
    {
        try
        {
            return implClass.getConstructor(String.class).newInstance(name);
        }
        catch (InvocationTargetException e)
        {
            throw rethrow(e);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("cannot instantiate " + implClass.getName(), e);
        }
    }

    private static void set(Class<?> implClass, Object instance, String key, RichIterable<Object> values)
    {
        try
        {
            Method setter = implClass.getMethod("_" + key, RichIterable.class);
            setter.invoke(instance, values);
        }
        catch (InvocationTargetException e)
        {
            throw rethrow(e);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("cannot set `" + key + "` on " + implClass.getName(), e);
        }
    }

    private static RuntimeException rethrow(InvocationTargetException e)
    {
        Throwable cause = e.getCause();
        return (cause instanceof RuntimeException) ? (RuntimeException) cause : new IllegalStateException(cause);
    }
}
