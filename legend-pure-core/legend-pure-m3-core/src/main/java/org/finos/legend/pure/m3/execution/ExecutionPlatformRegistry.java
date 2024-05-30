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

package org.finos.legend.pure.m3.execution;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.multimap.list.FastListMultimap;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ExecutionPlatformRegistry
{
    private static final Function<String, Class<?>> findExecutionPlatform = new Function<String, Class<?>>()
    {
        @Override
        public Class<?> valueOf(String name)
        {
            if (name != null)
            {
                for (Class<?> executionPlatformClass : getExecutionPlatformClasses())
                {
                    if (name.equals(getExecutionPlatformName(executionPlatformClass)))
                    {
                        return executionPlatformClass;
                    }
                }
            }
            return null;
        }
    };

    private static final ConcurrentMutableMap<String, Class<?>> cache = new ConcurrentHashMap<String, Class<?>>();

    private ExecutionPlatformRegistry()
    {
    }

    /**
     * Return whether the given class is an execution platform class.
     *
     * @param cls class
     * @return whether cls is an execution platform class
     */
    public static boolean isExecutionPlatformClass(Class<?> cls)
    {
        return cls.isAnnotationPresent(ExecutionPlatform.class);
    }

    /**
     * Get the execution platform class with the given name or null if there
     * is no such execution platform class.
     *
     * @param name execution platform name
     * @return execution platform class or null
     */
    public static Class<?> getClassByExecutionPlatformName(String name)
    {
        return cache.getIfAbsentPutWithKey(name, findExecutionPlatform);
    }

    /**
     * Get the execution platform name for the given class.
     *
     * @param executionPlatformClass execution platform class
     * @return execution platform name
     */
    public static String getExecutionPlatformName(Class<?> executionPlatformClass)
    {
        ExecutionPlatform annotation = executionPlatformClass.getAnnotation(ExecutionPlatform.class);
        return (annotation == null) ? null : annotation.name();
    }

    /**
     * Validate the set of execution platforms.
     *
     * @return true if the set of execution platforms is valid
     * @throws ExecutionPlatformValidationException if there is some problem with the set of execution platforms
     */
    public static boolean validateExecutionPlatforms() throws ExecutionPlatformValidationException
    {
        MutableListMultimap<String, Class<?>> names = new FastListMultimap<String, Class<?>>();
        MutableList<String> errors = Lists.mutable.with();

        for (Class<?> cls : getExecutionPlatformClasses())
        {
            if (!FunctionExecution.class.isAssignableFrom(cls))
            {
                errors.add("Execution platform class " + cls + " does not implement " + FunctionExecution.class);
            }
            String name = getExecutionPlatformName(cls);
            if (name == null)
            {
                errors.add("Execution platform class missing name: " + cls);
            }
            else
            {
                names.put(name, cls);
            }
        }

        for (Pair<String, RichIterable<Class<?>>> pair : names.keyMultiValuePairsView())
        {
            if (pair.getTwo().size() > 1)
            {
                errors.add("Name '" + pair.getOne() + "' used for multiple execution platforms: " + pair.getTwo().makeString(", "));
            }
        }

        if (errors.notEmpty())
        {
            String message;
            if (errors.size() == 1)
            {
                message = "Execution platform validation failed with the following error: " + errors.getFirst();
            }
            else
            {
                message = "Execution platform validation failed with the following errors:" + errors.makeString("\n\t", "\n\t", "");
            }
            throw new ExecutionPlatformValidationException(message);
        }
        return true;
    }

    /**
     * Get an iterable of all execution platform classes.
     *
     * @return execution platform classes
     */
    private static Iterable<? extends Class<?>> getExecutionPlatformClasses()
    {
        return new ExecutionPlatformClassIterable();
    }

    /**
     * Exception thrown when execution platform validation fails.
     */
    public static class ExecutionPlatformValidationException extends Exception
    {
        public ExecutionPlatformValidationException(String message)
        {
            super(message);
        }
    }

    private static class ExecutionPlatformClassIterable implements Iterable<Class<?>>
    {
        @Override
        public Iterator<Class<?>> iterator()
        {
            return new ExecutionPlatformClassIterator();
        }
    }

    private static class ExecutionPlatformClassIterator implements Iterator<Class<?>>
    {
        private final Class<?>[] classes;
        private int index;
        private Class<?> nextExecutionPlatform;

        private ExecutionPlatformClassIterator()
        {
            this.classes = Object.class.getClasses();
            this.index = 0;
            findNextExecutionPlatform();
        }

        @Override
        public boolean hasNext()
        {
            return this.nextExecutionPlatform != null;
        }

        @Override
        public Class<?> next()
        {
            if (!hasNext())
            {
                throw new NoSuchElementException();
            }
            Class<?> result = this.nextExecutionPlatform;
            findNextExecutionPlatform();
            return result;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        private void findNextExecutionPlatform()
        {
            for (this.nextExecutionPlatform = null; (this.nextExecutionPlatform == null) && (this.index < this.classes.length); this.index++)
            {
                if (isExecutionPlatform(this.classes[this.index]))
                {
                    this.nextExecutionPlatform = this.classes[this.index];
                }
            }
        }

        private boolean isExecutionPlatform(Class<?> cls)
        {
            return cls.isAnnotationPresent(ExecutionPlatform.class);
        }
    }
}
