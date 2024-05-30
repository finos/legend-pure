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

package org.finos.legend.pure.m4.coreinstance;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileStateSet;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;

public class AbstractCoreInstanceWrapper implements CoreInstance
{
    protected CoreInstance instance;

    public AbstractCoreInstanceWrapper(CoreInstance instance)
    {
        this.instance = instance;
    }

    @Override
    public ModelRepository getRepository()
    {
        return this.instance.getRepository();
    }

    @Override
    public int getSyntheticId()
    {
        return this.instance.getSyntheticId();
    }

    @Override
    public String getName()
    {
        return this.instance.getName();
    }

    @Override
    public void setName(String name)
    {
        this.instance.setName(name);
    }

    @Override
    public CoreInstance getClassifier()
    {
        return this.instance.getClassifier();
    }

    @Override
    public void setClassifier(CoreInstance classifier)
    {
        this.instance.setClassifier(classifier);
    }

    @Override
    public SourceInformation getSourceInformation()
    {
        return this.instance.getSourceInformation();
    }

    @Override
    public void setSourceInformation(SourceInformation sourceInformation)
    {
        this.instance.setSourceInformation(sourceInformation);
    }

    @Override
    public boolean isPersistent()
    {
        return this.instance.isPersistent();
    }

    @Override
    public void addKeyWithEmptyList(ListIterable<String> key)
    {
        this.instance.addKeyWithEmptyList(key);
    }

    @Override
    public void modifyValueForToManyMetaProperty(String key, int offset, CoreInstance value)
    {
        this.instance.modifyValueForToManyMetaProperty(key, offset, value);
    }

    @Override
    public void removeProperty(CoreInstance propertyNameKey)
    {
        this.instance.removeProperty(propertyNameKey);
    }

    public void removeProperty(String keyName)
    {
        this.instance.removeProperty(keyName);
    }

    @Override
    public CoreInstance getKeyByName(String name)
    {
        return this.instance.getKeyByName(name);
    }

    @Override
    public CoreInstance getValueForMetaPropertyToOne(String propertyName)
    {
        return this.instance.getValueForMetaPropertyToOne(propertyName);
    }

    @Override
    public CoreInstance getValueForMetaPropertyToOne(CoreInstance property)
    {
        return this.instance.getValueForMetaPropertyToOne(property);
    }

    @Override
    public ListIterable<? extends CoreInstance> getValueForMetaPropertyToMany(String keyName)
    {
        return this.instance.getValueForMetaPropertyToMany(keyName);
    }

    @Override
    public ListIterable<? extends CoreInstance> getValueForMetaPropertyToMany(CoreInstance key)
    {
        return this.instance.getValueForMetaPropertyToMany(key);
    }

    @Override
    public CoreInstance getValueInValueForMetaPropertyToMany(String keyName, String keyInMany)
    {
        return this.instance.getValueInValueForMetaPropertyToMany(keyName, keyInMany);
    }

    @Override
    public CoreInstance getValueInValueForMetaPropertyToManyWithKey(String keyName, String key, String keyInMany)
    {
        return this.instance.getValueInValueForMetaPropertyToManyWithKey(keyName, key, keyInMany);
    }

    @Override
    public <K> CoreInstance getValueInValueForMetaPropertyToManyByIDIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        return this.instance.getValueInValueForMetaPropertyToManyByIDIndex(keyName, indexSpec, keyInIndex);
    }

    @Override
    public <K> ListIterable<? extends CoreInstance> getValueInValueForMetaPropertyToManyByIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        return this.instance.getValueInValueForMetaPropertyToManyByIndex(keyName, indexSpec, keyInIndex);
    }

    @Override
    public boolean isValueDefinedForKey(String keyName)
    {
        return this.instance.isValueDefinedForKey(keyName);
    }

    @Override
    public void removeValueForMetaPropertyToMany(String keyName, CoreInstance coreInstance)
    {
        this.instance.removeValueForMetaPropertyToMany(keyName, coreInstance);
    }

    @Override
    public RichIterable<String> getKeys()
    {
        return this.instance.getKeys();
    }

    @Override
    public ListIterable<String> getRealKeyByName(String name)
    {
        return this.instance.getRealKeyByName(name);
    }

    @Override
    public void validate(MutableSet<CoreInstance> doneList) throws PureCompilationException
    {
        this.instance.validate(doneList);
    }

    @Override
    public void printFull(Appendable appendable, String tab)
    {
        this.instance.printFull(appendable, tab);
    }

    @Override
    public void print(Appendable appendable, String tab)
    {
        this.instance.print(appendable, tab);
    }

    @Override
    public void print(Appendable appendable, String tab, int max)
    {
        this.instance.print(appendable, tab, max);
    }

    @Override
    public void printWithoutDebug(Appendable appendable, String tab)
    {
        this.instance.printWithoutDebug(appendable, tab);
    }

    @Override
    public void printWithoutDebug(Appendable appendable, String tab, int max)
    {
        this.instance.printWithoutDebug(appendable, tab, max);
    }

    @Override
    public String printFull(String tab)
    {
        return this.instance.printFull(tab);
    }

    @Override
    public String print(String tab)
    {
        return this.instance.print(tab);
    }

    @Override
    public String print(String tab, int max)
    {
        return this.instance.print(tab, max);
    }

    @Override
    public String printWithoutDebug(String tab)
    {
        return this.instance.printWithoutDebug(tab);
    }

    @Override
    public String printWithoutDebug(String tab, int max)
    {
        return this.instance.printWithoutDebug(tab, max);
    }

    @Override
    public void setKeyValues(ListIterable<String> key, ListIterable<? extends CoreInstance> value)
    {
        this.instance.setKeyValues(key, value);
    }

    @Override
    public void addKeyValue(ListIterable<String> key, CoreInstance value)
    {
        this.instance.addKeyValue(key, value);
    }

    @Override
    public void commit(ModelRepositoryTransaction transaction)
    {
        this.instance.commit(transaction);
    }

    @Override
    public void rollback(ModelRepositoryTransaction transaction)
    {
        this.instance.rollback(transaction);
    }

    @Override
    public void markProcessed()
    {
        this.instance.markProcessed();
    }

    @Override
    public void markNotProcessed()
    {
        this.instance.markNotProcessed();
    }

    @Override
    public boolean hasBeenProcessed()
    {
        return this.instance.hasBeenProcessed();
    }

    @Override
    public void markValidated()
    {
        this.instance.markValidated();
    }

    @Override
    public void markNotValidated()
    {
        this.instance.markNotValidated();
    }

    @Override
    public boolean hasBeenValidated()
    {
        return this.instance.hasBeenValidated();
    }

    @Override
    public void addCompileState(CompileState state)
    {
        this.instance.addCompileState(state);
    }

    @Override
    public void removeCompileState(CompileState state)
    {
        this.instance.removeCompileState(state);
    }

    @Override
    public boolean hasCompileState(CompileState state)
    {
        return this.instance.hasCompileState(state);
    }

    @Override
    public CompileStateSet getCompileStates()
    {
        return this.instance.getCompileStates();
    }

    @Override
    public void setCompileStatesFrom(CompileStateSet states)
    {
        this.instance.setCompileStatesFrom(states);
    }

    @Override
    public boolean equals(Object o)
    {
        return (this == o) || (this.instance == o) || this.instance.equals((o instanceof AbstractCoreInstanceWrapper) ? ((AbstractCoreInstanceWrapper)o).instance : o);
    }

    @Override
    public int hashCode()
    {
        return this.instance.hashCode();
    }
}
