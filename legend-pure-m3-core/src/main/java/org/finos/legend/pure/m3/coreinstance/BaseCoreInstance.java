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

package org.finos.legend.pure.m3.coreinstance;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstance;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.indexing.IDConflictException;
import org.finos.legend.pure.m4.coreinstance.indexing.IDIndex;
import org.finos.legend.pure.m4.coreinstance.indexing.Index;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.tools.SafeAppendable;

public abstract class BaseCoreInstance extends AbstractCoreInstance
{
    public static final int DEFAULT_MAX_PRINT_DEPTH = 1;

    private final int internalSyntheticId;

    private String name;
    private final ModelRepository repository;
    private SourceInformation sourceInformation;
    private CoreInstance classifier;
    private final boolean persistent;

    public BaseCoreInstance(String name, SourceInformation sourceInformation, CoreInstance classifier, int internalSyntheticId, ModelRepository repository, boolean persistent)
    {
        this.name = name;
        this.classifier = classifier;
        this.repository = repository;
        this.internalSyntheticId = internalSyntheticId;
        this.sourceInformation = sourceInformation;
        this.persistent = persistent;
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

    @Override
    public CoreInstance getValueForMetaPropertyToOne(CoreInstance key)
    {
        return getValueForMetaPropertyToOne(key.getName());
    }

    @Override
    public ListIterable<? extends CoreInstance> getValueForMetaPropertyToMany(CoreInstance key)
    {
        return getValueForMetaPropertyToMany(key.getName());
    }

    @Override
    public void addKeyWithEmptyList(ListIterable<String> key)
    {
        setKeyValues(key, Lists.immutable.empty());
    }

    protected <V extends CoreInstance> ListIterable<V> getValuesFromToOnePropertyValue(V value)
    {
        return (value == null) ? Lists.immutable.empty() : Lists.immutable.with(value);
    }

    protected <V extends CoreInstance, K> V getValueByIDIndexFromToOnePropertyValue(V value, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        return (value != null) && Comparators.nullSafeEquals(keyInIndex, indexSpec.getIndexKey(value)) ? value : null;
    }

    protected <V extends CoreInstance, K> ListIterable<V> getValuesByIndexFromToOnePropertyValue(V value, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        return (value != null) && Comparators.nullSafeEquals(keyInIndex, indexSpec.getIndexKey(value)) ? Lists.immutable.with(value) : null;
    }

    protected <V extends CoreInstance> V getOneValueFromToManyPropertyValues(String keyName, ToManyPropertyValues<V> values)
    {
        if (values == null)
        {
            return null;
        }

        ListIterable<V> valuesList = values.getValues();
        int size = valuesList.size();
        switch (size)
        {
            case 0:
            {
                return null;
            }
            case 1:
            {
                return valuesList.get(0);
            }
            default:
            {
                StringBuilder builder = new StringBuilder(128);
                builder.append("More than one (").append(size).append(") result is returned for the key '").append(keyName).append("' in CoreInstance:\n\n");
                print(builder, "   ", 0);
                if (size <= 100)
                {
                    builder.append("\n\nValues:\n\n");
                    values.getValues().forEach(v -> v.print(builder.append("\n"), "", 0));
                }
                throw new RuntimeException(builder.toString());
            }
        }
    }

    protected <V extends CoreInstance> ListIterable<V> getValuesFromToManyPropertyValues(ToManyPropertyValues<V> values)
    {
        return (values == null) ? Lists.immutable.empty() : values.getValues();
    }

    protected <V extends CoreInstance, K> V getValueByIDIndexFromToManyPropertyValues(String keyName, ToManyPropertyValues<V> values, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        try
        {
            return (values == null) ? null : values.getValueByIDIndex(indexSpec, keyInIndex);
        }
        catch (IDConflictException e)
        {
            StringBuilder message = new StringBuilder("Invalid ID index for property '").append(keyName).append("' on ").append(this);
            if (this.sourceInformation != null)
            {
                this.sourceInformation.appendMessage(message.append(" (")).append(')');
            }
            message.append(": multiple values for id ").append(e.getId());
            throw new RuntimeException(message.toString(), e);
        }
    }

    protected <V extends CoreInstance, K> ListIterable<V> getValuesByIndexFromToManyPropertyValues(ToManyPropertyValues<V> values, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        return (values == null) ? Lists.immutable.empty() : values.getValuesByIndex(indexSpec, keyInIndex);
    }


    //------------
    // Validation
    //------------

    @Override
    public void validate(MutableSet<CoreInstance> doneList) throws PureCompilationException
    {
        this.validate(doneList, Stacks.mutable.with(this));
    }

    private void validate(MutableSet<CoreInstance> doneList, MutableStack<BaseCoreInstance> stack) throws PureCompilationException
    {
        while (stack.notEmpty())
        {
            final BaseCoreInstance element = stack.pop();
            doneList.add(element);

            if (element.getClassifier() == null)
            {
                SourceInformation foundSourceInformation = element.sourceInformation;
                if (stack.size() > 1)
                {
                    SourceInformation cursorSourceInformation = element.sourceInformation;
                    int cursor = stack.size() - 1;
                    while (cursorSourceInformation == null && cursor >= 0)
                    {
                        cursorSourceInformation = stack.peekAt(cursor--).sourceInformation;
                    }
                    foundSourceInformation = stack.peekAt(cursor + 1).sourceInformation;
                }
                throw new PureCompilationException(foundSourceInformation, element.getName() + " has not been defined!");
            }

            try
            {
                element.getKeys().forEach(keyName ->
                {
                    ListIterable<String> realKey = element.getRealKeyByName(keyName);
                    if (realKey == null)
                    {
                        throw new RuntimeException("No real key can be found for '" + keyName + "' in\n" + element.getName() + " (" + element + ")");
                    }

                    CoreInstance key = element.getKeyByName(keyName);
                    if (key.getClassifier() == null)
                    {
                        throw new RuntimeException("'" + key.getName() + "' used in '" + element.name + "' has not been defined!\n" + element.print("   "));
                    }

                    ListIterable<? extends CoreInstance> values = element.getValueForMetaPropertyToMany(keyName);
                    if (values != null)
                    {
                        values.forEach(childElement ->
                        {
                            if ((!doneList.contains(childElement) || (childElement.getClassifier() == null)) && (childElement instanceof BaseCoreInstance))
                            {
                                stack.push((BaseCoreInstance) childElement);
                            }
                        });
                    }
                });
            }
            catch (Exception e)
            {
                PureException pe = PureException.findPureException(e);
                if (pe != null)
                {
                    throw pe;
                }
                throw e;
            }
        }
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
        print(SafeAppendable.wrap(appendable), tab, Stacks.mutable.empty(), full, addDebug, max);
    }

    private void print(SafeAppendable appendable, String tab, MutableStack<CoreInstance> stack, boolean full, boolean addDebug, int max)
    {
        stack.push(this);
        printNodeName(appendable.append(tab), this, full, addDebug);
        printNodeName(appendable.append(" instance "), this.classifier, full, addDebug);
        getKeys().toSortedList().forEach(key -> printProperty(appendable.append('\n'), key, this.getValueForMetaPropertyToMany(key), tab, stack, full, addDebug, max));
        stack.pop();
    }

    private void printNodeName(SafeAppendable appendable, CoreInstance node, boolean full, boolean addDebug)
    {
        String name = node.getName();
        if (full)
        {
            appendable.append(name).append('_').append(node.getSyntheticId());
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
                appendable.append('(').append(sourceInfo.getSourceId()).append(':');
                appendable.append(sourceInfo.getStartLine()).append(',');
                appendable.append(sourceInfo.getStartColumn()).append(',');
                appendable.append(sourceInfo.getLine()).append(',');
                appendable.append(sourceInfo.getColumn()).append(',');
                appendable.append(sourceInfo.getEndLine()).append(',');
                appendable.append(sourceInfo.getEndColumn()).append(')');
            }
        }
    }

    private void printProperty(SafeAppendable appendable, String propertyName, ListIterable<? extends CoreInstance> values, String tab, MutableStack<CoreInstance> stack, boolean full, boolean addDebug, int max)
    {
        appendable.append(tab).append("    ");
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
        values.forEach(value ->
        {
            appendable.append('\n');

            if (value == null)
            {
                appendable.append(tab).append("        NULL");
            }
            else
            {
                // TODO remove reference to "package" property, which is an M3 thing
                boolean excludeFlag = ((excluded != null) && (excluded.contains(value.getValueForMetaPropertyToOne("package")) || excluded.contains(value))) ||
                        (this.repository.getTopLevel(value.getName()) != null);
                if (full ? stack.contains(value) : (excludeFlag || (stack.size() > max) || stack.contains(value)))
                {
                    CoreInstance valueClassifier = value.getClassifier();

                    appendable.append(tab).append("        ");
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
                            appendable.append('\n').append(tab).append("            [...]");
                        }
                        else if (!excludeFlag && (stack.size() > max) && value.getKeys().notEmpty())
                        {
                            appendable.append('\n').append(tab).append("            [... >").append(max).append(']');
                        }
                    }
                }
                else
                {
                    String newTab = tab + "        ";
                    if (value instanceof BaseCoreInstance)
                    {
                        ((BaseCoreInstance) value).print(appendable, newTab, stack, full, addDebug, max);
                    }
                    else if (full)
                    {
                        value.printFull(appendable, newTab);
                    }
                    else if (addDebug)
                    {
                        value.print(appendable, newTab, 0);
                    }
                    else
                    {
                        value.printWithoutDebug(appendable, newTab, 0);
                    }
                }
            }
        });
    }

    protected static <V extends CoreInstance> ToManyPropertyValues<V> newToManyPropertyValues()
    {
        return newToManyPropertyValues(Lists.immutable.empty());
    }

    protected static <V extends CoreInstance> ToManyPropertyValues<V> newToManyPropertyValues(V value)
    {
        return newToManyPropertyValues(Lists.immutable.with(value));
    }

    protected static <V extends CoreInstance> ToManyPropertyValues<V> newToManyPropertyValues(Iterable<? extends V> values)
    {
        return new ToManyPropertyValues<>(Lists.immutable.withAll(values));
    }

    protected static class ToManyPropertyValues<V extends CoreInstance>
    {
        private static final int MAX_NON_INDEXING_SIZE = 10;
        private static final int MIN_INDEXING_SIZE = 6;

        private ListIterable<V> values;
        private Indexes<V> indexes;

        private ToManyPropertyValues(ImmutableList<V> values)
        {
            this.values = values;
        }

        public int size()
        {
            return this.values.size();
        }

        public ListIterable<V> getValues() //NOSONAR
        {
            ListIterable<V> localValues = this.values;
            if (localValues instanceof MutableList<?>)
            {
                synchronized (this)
                {
                    if (this.values instanceof MutableList<?>)
                    {
                        this.values = this.values.toImmutable();
                    }
                    localValues = this.values;
                }
            }
            return localValues;
        }

        public <K> V getValueByIDIndex(IndexSpecification<K> indexSpec, K key) throws IDConflictException
        {
            synchronized (this)
            {
                if (this.indexes == null)
                {
                    if (!shouldBuildIndexes())
                    {
                        return getValueByIDIndex_small(indexSpec, key);
                    }
                    this.indexes = new Indexes<>();
                }
                return this.indexes.getValueByIDIndex(indexSpec, key, this.values);
            }
        }

        private <K> V getValueByIDIndex_small(IndexSpecification<K> indexSpec, K key) throws IDConflictException
        {
            V result = null;
            for (V value : this.values)
            {
                if (key.equals(indexSpec.getIndexKey(value)))
                {
                    if (result != null)
                    {
                        throw new IDConflictException(key);
                    }
                    result = value;
                }
            }
            return result;
        }

        public <K> ListIterable<V> getValuesByIndex(IndexSpecification<K> indexSpec, K key)
        {
            synchronized (this)
            {
                if (this.indexes == null)
                {
                    if (!shouldBuildIndexes())
                    {
                        return getValuesByIndex_small(indexSpec, key);
                    }
                    this.indexes = new Indexes<>();
                }
                return this.indexes.getValuesByIndex(indexSpec, key, this.values);
            }
        }

        private <K> ListIterable<V> getValuesByIndex_small(IndexSpecification<K> indexSpec, K key)
        {
            return this.values.select(v -> key.equals(indexSpec.getIndexKey(v)), Lists.mutable.empty());
        }

        public void setValues(Iterable<? extends V> values)
        {
            synchronized (this)
            {
                this.values = Lists.immutable.withAll(values);
                this.indexes = null;
            }
        }

        public void setValue(int offset, V value)
        {
            synchronized (this)
            {
                V oldValue = this.values.get(offset);
                if (!Comparators.nullSafeEquals(oldValue, value))
                {
                    if (!(this.values instanceof MutableList<?>))
                    {
                        this.values = this.values.toList();
                    }
                    ((MutableList<V>) this.values).set(offset, value);
                    if (this.indexes != null)
                    {
                        this.indexes.replaceValue(oldValue, value);
                    }
                }
            }
        }

        public void addValue(V value)
        {
            synchronized (this)
            {
                if (this.values.isEmpty())
                {
                    this.values = Lists.immutable.with(value);
                }
                else
                {
                    if (!(this.values instanceof MutableList<?>))
                    {
                        this.values = this.values.toList();
                    }
                    ((MutableList<V>) this.values).add(value);
                }
                if (this.indexes != null)
                {
                    this.indexes.addValue(value);
                }
            }
        }

        public void addValues(Iterable<? extends V> values)
        {
            if (Iterate.notEmpty(values))
            {
                synchronized (this)
                {
                    if (this.values.isEmpty())
                    {
                        this.values = Lists.mutable.withAll(values);
                    }
                    else
                    {
                        if (!(this.values instanceof MutableList<?>))
                        {
                            this.values = this.values.toList();
                        }
                        ((MutableList<V>) this.values).addAllIterable(values);
                    }
                    if (this.indexes != null)
                    {
                        this.indexes.addValues(values);
                    }
                }
            }
        }

        public void removeValue(V value)
        {
            synchronized (this)
            {
                if (this.values.notEmpty())
                {
                    boolean removed;
                    if (this.values instanceof MutableList<?>)
                    {
                        removed = ((MutableList<V>) this.values).remove(value);
                    }
                    else
                    {
                        int index = this.values.indexOf(value);
                        if (index < 0)
                        {
                            removed = false;
                        }
                        else
                        {
                            removed = true;
                            int oldSize = this.values.size();
                            MutableList<V> newValues = Lists.mutable.ofInitialCapacity(oldSize - 1);
                            for (int i = 0; i < oldSize; i++)
                            {
                                if (i != index)
                                {
                                    newValues.add(this.values.get(i));
                                }
                            }
                            this.values = newValues;
                        }
                    }
                    if (removed)
                    {
                        if (this.indexes != null)
                        {
                            if (shouldDropIndexes())
                            {
                                this.indexes = null;
                            }
                            else
                            {
                                this.indexes.removeValue(value);
                            }
                        }
                    }
                }
            }
        }

        public void removeValues(Iterable<? extends V> values)
        {
            if (Iterate.notEmpty(values))
            {
                synchronized (this)
                {
                    if (this.values.notEmpty())
                    {
                        if (!(this.values instanceof MutableList<?>))
                        {
                            this.values = this.values.toList();
                        }
                        boolean removed = ((MutableList<V>) this.values).removeAllIterable(values);
                        if (removed)
                        {
                            if (this.indexes != null)
                            {
                                if (shouldDropIndexes())
                                {
                                    this.indexes = null;
                                }
                                else
                                {
                                    this.indexes.removeValues(values);
                                }
                            }
                        }
                    }
                }
            }
        }

        public ToManyPropertyValues<V> copy()
        {
            return new ToManyPropertyValues<>(this.values.toImmutable());
        }

        private boolean shouldBuildIndexes()
        {
            return this.values.size() > MAX_NON_INDEXING_SIZE;
        }

        private boolean shouldDropIndexes()
        {
            return this.values.size() < MIN_INDEXING_SIZE;
        }
    }

    private static class Indexes<V extends CoreInstance>
    {
        private MutableMap<IndexSpecification<?>, IDIndex<?, V>> idIndexes;
        private MutableMap<IndexSpecification<?>, Index<?, V>> indexes;

        <K> V getValueByIDIndex(IndexSpecification<K> indexSpec, K key, ListIterable<V> values) throws IDConflictException
        {
            if (this.idIndexes == null)
            {
                this.idIndexes = Maps.mutable.empty();
            }
            IDIndex<?, V> idIndex = this.idIndexes.get(indexSpec);
            if (idIndex == null)
            {
                idIndex = IDIndex.newIDIndex(indexSpec, values);
                this.idIndexes.put(indexSpec, idIndex);
            }
            return idIndex.get(key);
        }

        <K> ListIterable<V> getValuesByIndex(IndexSpecification<K> indexSpec, K key, ListIterable<V> values)
        {
            if (this.indexes == null)
            {
                this.indexes = Maps.mutable.empty();
            }
            Index<?, V> index = this.indexes.getIfAbsentPut(indexSpec, () -> Index.newIndex(indexSpec, values));
            // TODO consider whether we should do this
            return index.get(key).toImmutable();
        }

        void addValue(V value)
        {
            if (this.idIndexes != null)
            {
                MutableList<IDIndex<?, V>> invalidIdIndexes = Lists.mutable.empty();
                this.idIndexes.forEachValue(idIndex ->
                {
                    try
                    {
                        idIndex.add(value);
                    }
                    catch (IDConflictException e)
                    {
                        invalidIdIndexes.add(idIndex);
                    }
                });
                invalidIdIndexes.forEach(invalidIdIndex -> this.idIndexes.remove(invalidIdIndex.getSpecification()));
            }
            if (this.indexes != null)
            {
                this.indexes.forEachValue(index -> index.add(value));
            }
        }

        void addValues(Iterable<? extends V> values)
        {
            if (this.idIndexes != null)
            {
                MutableList<IDIndex<?, V>> invalidIdIndexes = Lists.mutable.empty();
                this.idIndexes.forEachValue(idIndex ->
                {
                    try
                    {
                        idIndex.add(values);
                    }
                    catch (IDConflictException e)
                    {
                        invalidIdIndexes.add(idIndex);
                    }
                });
                invalidIdIndexes.forEach(invalidIdIndex -> this.idIndexes.remove(invalidIdIndex.getSpecification()));
            }
            if (this.indexes != null)
            {
                this.indexes.forEachValue(index -> index.add(values));
            }
        }

        void removeValue(V value)
        {
            if (this.idIndexes != null)
            {
                this.idIndexes.forEachValue(idIndex -> idIndex.remove(value));
            }
            if (this.indexes != null)
            {
                this.indexes.forEachValue(index -> index.remove(value));
            }
        }

        void removeValues(Iterable<? extends V> values)
        {
            if (this.idIndexes != null)
            {
                this.idIndexes.forEachValue(idIndex -> idIndex.remove(values));
            }
            if (this.indexes != null)
            {
                this.indexes.forEachValue(index -> index.remove(values));
            }
        }

        void replaceValue(V oldValue, V newValue)
        {
            if (this.idIndexes != null)
            {
                MutableList<IDIndex<?, V>> invalidIdIndexes = Lists.mutable.empty();
                this.idIndexes.forEachValue(idIndex ->
                {
                    idIndex.remove(oldValue);
                    try
                    {
                        idIndex.add(newValue);
                    }
                    catch (IDConflictException e)
                    {
                        invalidIdIndexes.add(idIndex);
                    }
                });
                invalidIdIndexes.forEach(invalidIdIndex -> this.idIndexes.remove(invalidIdIndex.getSpecification()));
            }
            if (this.indexes != null)
            {
                this.indexes.forEachValue(index ->
                {
                    index.remove(oldValue);
                    index.add(newValue);
                });
            }
        }
    }
}
