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

package org.finos.legend.pure.m3.compiler.postprocessing;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.set.mutable.UnmodifiableMutableSet;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.tools.SafeAppendable;

public class VariableContext
{
    private final VariableContext parent;
    private MutableMap<String, CoreInstance> mapping;

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
        for (VariableContext context = this; context != null; context = context.parent)
        {
            CoreInstance value = context.getLocalValue(name);
            if (value != null)
            {
                return value;
            }
        }
        return null;
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

    @Deprecated
    public void buildAndRegister(String name, GenericType genericType, Multiplicity multiplicity, ModelRepository modelRepository, ProcessorSupport processorSupport) throws PureCompilationException
    {
        buildAndRegister(name, genericType, multiplicity, processorSupport);
    }

    public void buildAndRegister(String name, GenericType genericType, Multiplicity multiplicity, ProcessorSupport processorSupport)
    {
        VariableExpression variableExpression = (VariableExpression) processorSupport.newEphemeralAnonymousCoreInstance(M3Paths.VariableExpression);
        variableExpression._name(name);
        variableExpression._genericType(genericType);
        variableExpression._multiplicity(multiplicity);
        try
        {
            registerValue(name, variableExpression);
        }
        catch (VariableContext.VariableNameConflictException e)
        {
            throw new PureCompilationException(null, e.getMessage(), e);
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
     * Get the depth of the context, i.e, the number of parent
     * contexts counted recursively.  If the context has no
     * parent, then the depth is 0.  If it has a parent, then
     * the depth is the parent's depth plus 1.
     *
     * @return parent depth
     */
    public int getDepth()
    {
        return (this.parent == null) ? 0 : this.parent.getDepth() + 1;
    }

    /**
     * Get the names of all variables in the context,
     * including those from parent contexts.
     *
     * @return variable names
     */
    public SetIterable<String> getVariableNames()
    {
        if (this.parent == null)
        {
            return getLocalVariableNames();
        }

        MutableSet<String> names = Sets.mutable.empty();
        for (VariableContext context = this; context != null; context = context.parent)
        {
            if (context.mapping != null)
            {
                names.addAll(context.mapping.keySet());
            }
        }
        return names;
    }

    /**
     * Get the names of variables local to the context
     * (i.e., not including variables defined in parent
     * contexts).
     *
     * @return local variable names
     */
    public SetIterable<String> getLocalVariableNames()
    {
        return (this.mapping == null) ? Sets.immutable.empty() : UnmodifiableMutableSet.of(this.mapping.keySet());
    }

    /**
     * Print the variables and their values from this context.
     * Variables owned by parent contexts which are overridden
     * by child contexts are not printed.
     */
    public void printVariables()
    {
        printVariables(null);
    }

    /**
     * Print the variables and their values from this context.
     * Variables owned by parent contexts which are overridden
     * by child contexts are not printed.
     *
     * @param indent indentation string used at the start of each line
     */
    public void printVariables(String indent)
    {
        writeVariables(System.out, indent);
    }

    /**
     * Write the variables and their values from this context
     * to a string.  Variables owned by parent contexts which
     * are overridden by child contexts are not printed.
     *
     * @return context variables string
     */
    public String writeVariablesToString()
    {
        return writeVariablesToString(null);
    }

    /**
     * Write the variables and their values from this context
     * to a string.  Variables owned by parent contexts which
     * are overridden by child contexts are not printed.
     *
     * @param indent indentation string used at the start of each line
     * @return context variables string
     */
    public String writeVariablesToString(String indent)
    {
        return writeVariables(new StringBuilder(), indent).toString();
    }

    /**
     * Write the variables and their values from this context
     * to the given appendable.  Variables owned by parent contexts
     * which are overridden by child contexts are not printed.
     *
     * @param appendable appendable to write to
     */
    public <T extends Appendable> T writeVariables(T appendable)
    {
        return writeVariables(appendable, null);
    }

    /**
     * Write the variables and their values from this context
     * to the given appendable.  Variables owned by parent contexts
     * which are overridden by child contexts are not printed.
     *
     * @param appendable appendable to write to
     * @param indent     indentation string used at the start of each line
     */
    public <T extends Appendable> T writeVariables(T appendable, String indent)
    {
        String resolvedIndent = (indent == null) ? "" : indent;

        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        SetIterable<String> variables = getVariableNames();
        if (variables.isEmpty())
        {
            safeAppendable.append(resolvedIndent).append("Empty variable context\n");
        }
        else
        {
            variables.toSortedList().forEach(v -> safeAppendable.append(resolvedIndent).append(v).append(": ").append(getValue(v)).append('\n'));
        }
        return appendable;
    }

    /**
     * Print the variables and their values from this context
     * and all parent contexts, grouped by context.  All variables
     * from parent contexts are printed, even if they are overridden
     * by child contexts.
     */
    public void printVariablesByContext()
    {
        printVariablesByContext(null, null);
    }

    /**
     * Print the variables and their values from this context
     * and all parent contexts, grouped by context.  All variables
     * from parent contexts are printed, even if they are overridden
     * by child contexts.
     *
     * @param initialIndent    initial indentation string used at the start of each line
     * @param additionalIndent indentation string used (on top of initialIndent) on lines that require further indentation
     */
    public void printVariablesByContext(String initialIndent, String additionalIndent)
    {
        writeVariablesByContext(System.out, initialIndent, additionalIndent);
    }

    /**
     * Write to a string the variables and their values from this
     * context and all parent contexts, grouped by context.  All
     * variables from parent contexts are written, even if they are
     * overridden by child contexts.
     *
     * @return variables by context string
     */
    public String writeVariablesByContextToString()
    {
        return writeVariablesByContextToString(null, null);
    }

    /**
     * Write to a string the variables and their values from this
     * context and all parent contexts, grouped by context.  All
     * variables from parent contexts are written, even if they are
     * overridden by child contexts.
     *
     * @param initialIndent    initial indentation string used at the start of each line
     * @param additionalIndent indentation string used (on top of initialIndent) on lines that require further indentation
     * @return variables by context string
     */
    public String writeVariablesByContextToString(String initialIndent, String additionalIndent)
    {
        return writeVariablesByContext(new StringBuilder(), initialIndent, additionalIndent).toString();
    }

    /**
     * Write to the given appendable the variables and their values
     * from this context and all parent contexts, grouped by context.
     * All variables from parent contexts are written, even if they are
     * overridden by child contexts.
     *
     * @param appendable       appendable to write to
     * @param initialIndent    initial indentation string used at the start of each line
     * @param additionalIndent indentation string used (on top of initialIndent) on lines that require further indentation
     */
    public <T extends Appendable> T writeVariablesByContext(T appendable, String initialIndent, String additionalIndent)
    {
        String resolvedInitialIndent = (initialIndent == null) ? "" : initialIndent;
        String resolvedAdditionalIndent = (additionalIndent == null) ? (resolvedInitialIndent.isEmpty() ? "\t" : resolvedInitialIndent) : additionalIndent;

        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        int level = 0;
        for (VariableContext context = this; context != null; context = context.parent)
        {
            safeAppendable.append(resolvedInitialIndent).append("Variable Context Level ").append(level++).append('\n');
            if (context.mapping == null)
            {
                safeAppendable.append(resolvedInitialIndent).append(resolvedAdditionalIndent).append("None\n");
            }
            else
            {
                context.mapping.keyValuesView()
                        .toSortedListBy(Pair::getOne)
                        .forEach(p -> safeAppendable.append(resolvedInitialIndent).append(resolvedAdditionalIndent).append(p.getOne()).append(": ").append(p.getTwo()).append('\n'));
            }
        }
        return appendable;
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
