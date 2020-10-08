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

package org.finos.legend.pure.runtime.java.interpreted;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.io.PrintStream;

public class VariableContext
{
    private final VariableContext parent;
    private MutableMap<String, CoreInstance> mapping;
    private boolean functionScopeLimitMarker = false;

    private VariableContext(VariableContext parent)
    {
        this.parent = parent;
    }

    private VariableContext(VariableContext parent, String name, CoreInstance value)
    {
        this(parent);
        this.mapping = Maps.mutable.with(name, value);
    }

    /**
     * Get the value for the named variable.  If the variable
     * has no local value, the parent contexts are searched
     * transitively.  Returns null if no value can be found.
     *
     * @param name variable name
     * @return variable value, or null if not present
     */
    public CoreInstance getValue(String name)
    {
        CoreInstance value = getLocalValue(name);
        return ((value != null) || (this.parent == null) || this.functionScopeLimitMarker) ? value : this.parent.getValue(name);
    }

    public void markVariableScopeBoundary()
    {
        this.functionScopeLimitMarker = true;
    }

    /**
     * Get the local value for the named variable.  That is,
     * the value in this context without searching parent
     * contexts.  Returns null if no value can be found.
     *
     * @param name variable name
     * @return local variable value, or null if not present
     */
    public CoreInstance getLocalValue(String name)
    {
        return (this.mapping == null) ? null : this.mapping.get(name);
    }

    /**
     * Register value for name.  Throws an exception if name
     * is already in use.  Note that this registers the value
     * locally and has no effect on parent contexts.
     *
     * @param name  variable name
     * @param value variable value
     * @throws VariableNameConflictException if name is already in use
     */
    public void registerValue(String name, CoreInstance value) throws VariableNameConflictException
    {
        if (this.mapping == null)
        {
            this.mapping = Maps.mutable.with(name, value);
        }
        else
        {
            CoreInstance previous = this.mapping.put(name, value);
            if (previous != null)
            {
                this.mapping.put(name, previous);
                throw new VariableNameConflictException(name);
            }
        }
    }

    /**
     * Get the parent context, if present.  Returns null
     * if there is no parent context.
     *
     * @return parent variable context
     */
    public VariableContext getParent()
    {
        return this.parent;
    }

    /**
     * Get the names of all variables in the context,
     * including those from parent contexts.
     *
     * @return variable names
     */
    public MutableSet<String> getVariableNames()
    {
        if (this.parent == null)
        {
            return getLocalVariableNames();
        }
        else
        {
            MutableSet<String> names = this.parent.getVariableNames();
            if (this.mapping != null)
            {
                names.addAllIterable(this.mapping.keysView());
            }
            return names;
        }
    }

    /**
     * Get the names of variables local to the context
     * (i.e., not including variables defined in parent
     * contexts).
     *
     * @return local variable names
     */
    public MutableSet<String> getLocalVariableNames()
    {
        return (this.mapping == null) ? Sets.mutable.<String>with() : Sets.mutable.withAll(this.mapping.keysView());
    }

    public void print(PrintStream printStream)
    {
        if (this.mapping != null)
        {
            printStream.println("   " + this.mapping.keysView().makeString(",") + ":freeze:" + this.functionScopeLimitMarker);
            if (this.parent != null && this.parent.mapping != null)
            {
                printStream.print("   parent:");
                this.parent.print(printStream);
            }
        }
    }

    /**
     * Return a new variable context with no parent context.
     *
     * @return new variable context
     */
    public static VariableContext newVariableContext()
    {
        return newVariableContext(null);
    }

    /**
     * Return a new variable context with the given parent, which
     * may be null.
     *
     * @param parent parent variable context
     * @return new variable context
     */
    public static VariableContext newVariableContext(VariableContext parent)
    {
        return new VariableContext(parent);
    }

    /**
     * Return a new variable context with the given parent, and register
     * the given variable value.
     *
     * @param parent parent variable context
     * @param name   variable name
     * @param value  variable value
     * @return new variable context
     */
    public static VariableContext newVariableContextWith(VariableContext parent, String name, CoreInstance value)
    {
        return new VariableContext(parent, name, value);
    }

    /**
     * Exception thrown when there is a variable name conflict.
     */
    public static class VariableNameConflictException extends Exception
    {
        private VariableNameConflictException(String name)
        {
            super("'" + name + "' has already been defined!");
        }
    }
}
