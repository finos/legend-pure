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

package org.finos.legend.pure.m4.coreinstance.simple;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.block.procedure.checked.CheckedProcedure;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Stacks;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstance;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileStateSet;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.indexing.IDConflictException;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;

import java.io.IOException;
import java.util.Stack;

public class SimpleCoreInstance extends AbstractCoreInstance
{
    public static final int DEFAULT_MAX_PRINT_DEPTH = 1;

    private final int internalSyntheticId;

    private String name;
    private final ModelRepository repository;
    private SourceInformation sourceInformation;
    private CoreInstance classifier;
    private final boolean persistent;

    private SimpleCoreInstanceMutableState state;

    protected SimpleCoreInstance(String name, SourceInformation sourceInformation, CoreInstance classifier, int internalSyntheticId, ModelRepository repository, boolean persistent)
    {
        this.name = name;
        this.classifier = classifier;
        this.repository = repository;
        this.internalSyntheticId = internalSyntheticId;
        this.sourceInformation = sourceInformation;
        this.state = new SimpleCoreInstanceMutableState();
        this.persistent = persistent;
    }

    @Override
    public void commit(ModelRepositoryTransaction transaction)
    {
        this.state = (SimpleCoreInstanceMutableState)transaction.getState(this);
    }

    @Override
    public void rollback(ModelRepositoryTransaction transaction)
    {
    }

    @Override
    public void addCompileState(CompileState state)
    {
        this.prepareForWrite();
        this.getState().addCompileState(state);
    }

    @Override
    public void removeCompileState(CompileState state)
    {
        this.prepareForWrite();
        this.getState().removeCompileState(state);
    }

    @Override
    public boolean hasCompileState(CompileState state)
    {
        return this.getState().hasCompileState(state);
    }

    @Override
    public CompileStateSet getCompileStates()
    {
        return this.getState().getCompileStates();
    }

    @Override
    public void setCompileStatesFrom(CompileStateSet states)
    {
        this.getState().setCompileStatesFrom(states);
    }

    @Override
    public int getSyntheticId()
    {
        return this.internalSyntheticId;
    }

    @Override
    public ModelRepository getRepository()
    {
        return this.repository;
    }

    @Override
    public CoreInstance getClassifier()
    {
        return this.classifier;
    }

    @Override
    public void setClassifier(CoreInstance classifier)
    {
        this.classifier = classifier;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
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
        return this.persistent;
    }


    // ------------
    //  Get To One
    //-------------
    @Override
    public CoreInstance getValueForMetaPropertyToOne(String propertyName)
    {
        try
        {
            return getState().getOneValue(propertyName);
        }
        catch (OneValueException e)
        {
            int size = e.getSize();
            StringBuilder builder = new StringBuilder(128);
            builder.append("More than one (");
            builder.append(size);
            builder.append(") result is returned for the key '");
            builder.append(propertyName);
            builder.append("' in CoreInstance:\n\n");
            print(builder, "   ", 0);
            if (size <= 100)
            {
                builder.append("\n\nValues:\n\n");
                for (CoreInstance value : getState().getValues(propertyName))
                {
                    builder.append("\n");
                    value.print(builder, "", 0);
                }
            }
            throw new RuntimeException(builder.toString());
        }
    }

    @Override
    public CoreInstance getValueForMetaPropertyToOne(CoreInstance property)
    {
        return this.getValueForMetaPropertyToOne(property.getName());
    }

    // -------------
    //  Get To Many
    //--------------
    @Override
    public ListIterable<CoreInstance> getValueForMetaPropertyToMany(String keyName)
    {
        ListIterable<CoreInstance> values = this.getState().getValues(keyName);
        return (values == null) ? Lists.immutable.<CoreInstance>with() : values;
    }

    @Override
    public ListIterable<CoreInstance> getValueForMetaPropertyToMany(CoreInstance key)
    {
        return this.getValueForMetaPropertyToMany(key.getName());
    }

    @Override
    public <K> CoreInstance getValueInValueForMetaPropertyToManyByIDIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        try
        {
            return getState().getValueInValueForMetaPropertyToManyByIDIndex(keyName, indexSpec, keyInIndex);
        }
        catch (IDConflictException e)
        {
            StringBuilder message = new StringBuilder("Invalid ID index for property '");
            message.append(keyName);
            message.append("' on ");
            message.append(this);
            if (this.sourceInformation != null)
            {
                message.append(" (");
                this.sourceInformation.writeMessage(message);
                message.append(')');
            }
            message.append(": multiple values for id ");
            message.append(e.getId());
            throw new RuntimeException(message.toString(), e);
        }
    }

    @Override
    public <K> ListIterable<CoreInstance> getValueInValueForMetaPropertyToManyByIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        return getState().getValueInValueForMetaPropertyToManyByIndex(keyName, indexSpec, keyInIndex);
    }

    // -----------
    //  Mutations
    //------------

    private void prepareForWrite()
    {
        // TODO?: shouldn't this be synchronized?
        ModelRepositoryTransaction transaction = this.repository.getTransaction();
        if (transaction != null && !transaction.isRegistered(this))
        {
            transaction.registerModified(this, this.state.copy());
        }
    }

    @Override
    public void modifyValueForToManyMetaProperty(String key, int offset, CoreInstance value)
    {
        this.prepareForWrite();
        this.getState().modifyValues(key, offset, value);
    }

    @Override
    public void removeValueForMetaPropertyToMany(String keyName, CoreInstance coreInstance)
    {
        this.prepareForWrite();
        this.getState().removeValue(keyName, coreInstance);
    }

    @Override
    public void addKeyWithEmptyList(ListIterable<String> key)
    {
        this.prepareForWrite();
        this.getState().addKeyWithNoValues(key);
    }

    @Override
    public void removeProperty(String keyName)
    {
        this.prepareForWrite();
        this.getState().removeKey(keyName);
    }

    @Override
    public void setKeyValues(ListIterable<String> key, ListIterable<? extends CoreInstance> values)
    {
        this.prepareForWrite();
        this.getState().setValues(key, values);
    }

    @Override
    public void addKeyValue(ListIterable<String> key, CoreInstance value)
    {
        this.prepareForWrite();
        this.getState().addValue(key, value);
    }

    public CoreInstance getOrCreateUnknownTypeNode(String key, String keyInArray, ModelRepository builder)
    {
        CoreInstance result = this.getValueInValueForMetaPropertyToMany(key, keyInArray);
        if (result == null)
        {
            this.prepareForWrite();
            result = builder.newUnknownTypeCoreInstance(keyInArray, null);
            this.getState().addValue(key, result);
        }
        return result;
    }

    @Override
    public CoreInstance copy()
    {
        throw new UnsupportedOperationException("Not supported");
    }



    //-------------
    // Key Queries
    //-------------

    @Override
    public ListIterable<String> getRealKeyByName(String name)
    {
        return this.getState().getRealKeyByName(name);
    }

    @Override
    public CoreInstance getKeyByName(String name)
    {
        return this.getState().getKeyByName(name, this);
    }

    @Override
    public RichIterable<String> getKeys()
    {
        return this.getState().getKeys();
    }

    @Override
    public boolean isValueDefinedForKey(String keyName)
    {
        return this.getState().hasValuesDefined(keyName);
    }


    //------------
    // Validation
    //------------

    @Override
    public void validate(MutableSet<CoreInstance> doneList) throws PureCompilationException
    {
        Stack<SimpleCoreInstance> stack = new Stack<SimpleCoreInstance>();
        stack.push(this);
        this.validate(doneList, stack);
    }

    private void validate(final MutableSet<CoreInstance> doneList, final Stack<SimpleCoreInstance> stack) throws PureCompilationException
    {
        while (!stack.isEmpty())
        {
            final SimpleCoreInstance element = stack.pop();
            doneList.add(element);

            if (element.getClassifier() == null)
            {
                SourceInformation foundSourceInformation = element.sourceInformation;
                if(stack.size() > 1)
                {
                    SourceInformation cursorSourceInformation = element.sourceInformation;
                    int cursor = stack.size()-1;
                    while (cursorSourceInformation == null && cursor >= 0)
                    {
                        cursorSourceInformation = stack.get(cursor--).sourceInformation;
                    }
                    foundSourceInformation =  stack.get(cursor+1).sourceInformation;
                }
                throw new PureCompilationException(foundSourceInformation, element.getName()+" has not been defined!");
            }

            try
            {
                element.getState().getKeys().forEach(new Procedure<String>()
                {
                    @Override
                    public void value(String keyName)
                    {
                        ImmutableList<String> realKey = element.getState().getRealKeyByName(keyName);
                        if (realKey == null)
                        {
                            throw new RuntimeException("No real key can be found for '"+keyName+"' in\n"+ element.getName()+" ("+element+")");
                        }

                        CoreInstance key = element.getKeyByName(keyName);
                        if (key.getClassifier() == null)
                        {
                            throw new RuntimeException("'"+key.getName()+"' used in '"+ element.name +"' has not been defined!\n"+ element.print("   "));
                        }

                        ListIterable<CoreInstance> values = element.getState().getValues(keyName);
                        if (values != null)
                        {
                            values.forEach(new CheckedProcedure<CoreInstance>()
                            {
                                @Override
                                public void safeValue(CoreInstance childElement) throws Exception
                                {
                                    if (!doneList.contains(childElement) || childElement.getClassifier() == null)
                                    {
                                        if (childElement instanceof SimpleCoreInstance)
                                        {
                                            stack.push((SimpleCoreInstance)childElement);
                                        }
                                    }
                                }
                            });
                        }
                    }
                });
            }
            catch (Exception e)
            {
                if (e.getCause() != null && e.getCause() instanceof PureCompilationException)
                {
                    throw (PureCompilationException)e.getCause();
                }
                throw new RuntimeException(e);
            }
        }
    }


    private SimpleCoreInstanceMutableState getState()
    {
        ModelRepositoryTransaction transaction = this.repository.getTransaction();
        if ((transaction != null) && transaction.isOpen())
        {
            SimpleCoreInstanceMutableState transactionState = (SimpleCoreInstanceMutableState)transaction.getState(this);
            if (transactionState != null)
            {
                return transactionState;
            }
        }
        return this.state;
    }


    //-------
    // Print
    //-------

    @Override
    public String toString()
    {
        return this.name + "(" + this.internalSyntheticId + ") instanceOf " + ((getClassifier() == null) ? null : getClassifier().getName());
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
        print(appendable, tab, false, false, DEFAULT_MAX_PRINT_DEPTH);
    }

    @Override
    public void printWithoutDebug(Appendable appendable, String tab, int max)
    {
        print(appendable, tab, false, false, max);
    }

    private void print(Appendable appendable, String tab, boolean full, boolean addDebug, int max)
    {
        try
        {
            print(appendable, tab, Stacks.mutable.<CoreInstance>with(), full, addDebug, max);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void print(Appendable appendable, String tab, MutableStack<CoreInstance> stack, boolean full, boolean addDebug, int max) throws IOException
    {
        stack.push(this);

        appendable.append(tab);
        this.printNodeName(appendable, this, full, addDebug);
        appendable.append(" instance ");
        this.printNodeName(appendable, this.classifier, full, addDebug);
        for (String key : this.getState().getKeys().toSortedList())
        {
            appendable.append('\n');
            this.printProperty(appendable, key, this.getState().getValues(key), tab, stack, full, addDebug, max);
        }

        stack.pop();
    }

    private void printNodeName(Appendable appendable, CoreInstance node, boolean full, boolean addDebug) throws IOException
    {
        if (node == null)
        {
            return;
        }
        String name = node.getName();
        if (full)
        {
            appendable.append(name);
            appendable.append('_');
            appendable.append(Integer.toString(node.getSyntheticId()));
        }
        else
        {
            appendable.append(ModelRepository.possiblyReplaceAnonymousId(name));
        }

        if (addDebug)
        {
            SourceInformation sourceInfo = node.getSourceInformation();
            if (sourceInfo != null)
            {
                appendable.append('(');
                appendable.append(sourceInfo.getSourceId());
                appendable.append(':');
                appendable.append(Integer.toString(sourceInfo.getStartLine()));
                appendable.append(',');
                appendable.append(Integer.toString(sourceInfo.getStartColumn()));
                appendable.append(',');
                appendable.append(Integer.toString(sourceInfo.getLine()));
                appendable.append(',');
                appendable.append(Integer.toString(sourceInfo.getColumn()));
                appendable.append(',');
                appendable.append(Integer.toString(sourceInfo.getEndLine()));
                appendable.append(',');
                appendable.append(Integer.toString(sourceInfo.getEndColumn()));
                appendable.append(')');
            }
        }
    }

    private void printProperty(Appendable appendable, String propertyName, ListIterable<CoreInstance> values, String tab, MutableStack<CoreInstance> stack, boolean full, boolean addDebug, int max) throws IOException
    {
        appendable.append(tab);
        appendable.append("    ");
        if (propertyName == null)
        {
            appendable.append("null:");
        }
        else
        {
            CoreInstance property = getKeyByName(propertyName);
            CoreInstance propertyClassifier = property.getClassifier();

            printNodeName(appendable, property, full, addDebug);
            appendable.append('(');
            if (propertyClassifier == null)
            {
                appendable.append("null");
            }
            else
            {
                printNodeName(appendable, propertyClassifier, full, addDebug);
            }
            appendable.append("):");
        }

        SetIterable<CoreInstance> excluded = this.repository.getExclusionSet();
        for (CoreInstance value : values)
        {
            appendable.append('\n');

            if (value == null)
            {
                appendable.append(tab);
                appendable.append("        NULL");
            }
            else
            {
                // TODO remove reference to "package" property, which is an M3 thing
                boolean excludeFlag = ((excluded != null) && (excluded.contains(value.getValueForMetaPropertyToOne("package")) || excluded.contains(value))) ||
                        (this.repository.getTopLevel(value.getName()) != null);
                if (full ? stack.contains(value) : (excludeFlag || (stack.size() > max) || stack.contains(value)))
                {
                    CoreInstance valueClassifier = value.getClassifier();

                    appendable.append(tab);
                    appendable.append("        ");
                    printNodeName(appendable, value, full, addDebug);
                    appendable.append(" instance ");
                    if (valueClassifier == null)
                    {
                        appendable.append("null");
                    }
                    else
                    {
                        printNodeName(appendable, valueClassifier, full, addDebug);
                        if (full)
                        {
                            appendable.append('\n');
                            appendable.append(tab);
                            appendable.append("            [...]");
                        }
                        else if (!excludeFlag && (stack.size() > max) && value.getKeys().notEmpty())
                        {
                            appendable.append('\n');
                            appendable.append(tab);
                            appendable.append("            [... >");
                            appendable.append(Integer.toString(max));
                            appendable.append(']');
                        }
                    }
                }
                else
                {
                    String newTab = tab + "        ";
                    if (value instanceof SimpleCoreInstance)
                    {
                        ((SimpleCoreInstance)value).print(appendable, newTab, stack, full, addDebug, max);
                    }
                    else if (full)
                    {
                        value.printFull(appendable, newTab);
                    }
                    else if (addDebug)
                    {
                        value.print(appendable, newTab, max);
                    }
                    else
                    {
                        value.printWithoutDebug(appendable, newTab, max);
                    }
                }

            }
        }
    }
}
