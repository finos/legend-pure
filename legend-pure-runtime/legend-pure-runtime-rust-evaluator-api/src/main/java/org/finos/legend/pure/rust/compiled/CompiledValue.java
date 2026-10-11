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
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstance;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileStateSet;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;
import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.ValCoreInstance;

import java.io.IOException;

/**
 * A value the compiled engine represents as a plain Java object that is not
 * a {@link CoreInstance} (a {@code PureMap}), carried across the boundary,
 * whose only currency is {@code CoreInstance}, and unwrapped by
 * {@link CompiledEvalRust} before generated code sees it. It has no
 * properties of its own.
 */
final class CompiledValue extends AbstractCoreInstance
{
    private final Object value;
    private final CoreInstance classifier;

    CompiledValue(Object value, CoreInstance classifier)
    {
        this.value = value;
        this.classifier = classifier;
    }

    /**
     * The value generated code expects for a result: the Java value of a
     * compiled primitive or of a carried value, any other instance as it is.
     *
     * @param instance a value the bridge materialised for the compiled engine
     * @return the value as compiled code holds it
     */
    static Object unwrap(CoreInstance instance)
    {
        if (instance instanceof ValCoreInstance)
        {
            return ((ValCoreInstance<?>) instance).getValue();
        }
        if (instance instanceof CompiledValue)
        {
            return ((CompiledValue) instance).value;
        }
        return instance;
    }

    @Override
    public String getName()
    {
        return String.valueOf(this.value);
    }

    @Override
    public CoreInstance getClassifier()
    {
        return this.classifier;
    }

    @Override
    public ModelRepository getRepository()
    {
        return null;
    }

    @Override
    public int getSyntheticId()
    {
        return -1;
    }

    @Override
    public SourceInformation getSourceInformation()
    {
        return null;
    }

    @Override
    public boolean isPersistent()
    {
        return false;
    }

    @Override
    public RichIterable<String> getKeys()
    {
        return Lists.immutable.empty();
    }

    @Override
    public ListIterable<String> getRealKeyByName(String name)
    {
        return null;
    }

    @Override
    public CoreInstance getKeyByName(String name)
    {
        return null;
    }

    @Override
    public ListIterable<? extends CoreInstance> getValueForMetaPropertyToMany(String keyName)
    {
        return Lists.immutable.empty();
    }

    @Override
    public CoreInstance getValueForMetaPropertyToOne(String propertyName)
    {
        return null;
    }

    @Override
    public boolean isValueDefinedForKey(String keyName)
    {
        return false;
    }

    @Override
    public <K> CoreInstance getValueInValueForMetaPropertyToManyByIDIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        return null;
    }

    @Override
    public <K> ListIterable<? extends CoreInstance> getValueInValueForMetaPropertyToManyByIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        return Lists.immutable.empty();
    }

    @Override
    public void print(Appendable appendable, String tab, int max)
    {
        try
        {
            appendable.append(tab).append(getName());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString()
    {
        return getName();
    }

    @Override
    public CoreInstance copy()
    {
        throw readOnly();
    }

    @Override
    public void setName(String name)
    {
        throw readOnly();
    }

    @Override
    public void setClassifier(CoreInstance classifier)
    {
        throw readOnly();
    }

    @Override
    public void setSourceInformation(SourceInformation sourceInformation)
    {
        throw readOnly();
    }

    @Override
    public void addKeyWithEmptyList(ListIterable<String> key)
    {
        throw readOnly();
    }

    @Override
    public void modifyValueForToManyMetaProperty(String key, int offset, CoreInstance value)
    {
        throw readOnly();
    }

    @Override
    public void removeProperty(String propertyNameKey)
    {
        throw readOnly();
    }

    @Override
    public void removeValueForMetaPropertyToMany(String keyName, CoreInstance coreInstance)
    {
        throw readOnly();
    }

    @Override
    public void setKeyValues(ListIterable<String> key, ListIterable<? extends CoreInstance> value)
    {
        throw readOnly();
    }

    @Override
    public void addKeyValue(ListIterable<String> key, CoreInstance value)
    {
        throw readOnly();
    }

    @Override
    public void commit(ModelRepositoryTransaction transaction)
    {
        throw readOnly();
    }

    @Override
    public void addCompileState(CompileState state)
    {
    }

    @Override
    public void removeCompileState(CompileState state)
    {
    }

    @Override
    public boolean hasCompileState(CompileState state)
    {
        return CompileStateSet.PROCESSED_VALIDATED.contains(state);
    }

    @Override
    public CompileStateSet getCompileStates()
    {
        return CompileStateSet.PROCESSED_VALIDATED;
    }

    @Override
    public void setCompileStatesFrom(CompileStateSet states)
    {
    }

    @Override
    public void validate(MutableSet<CoreInstance> doneList)
    {
    }

    private static UnsupportedOperationException readOnly()
    {
        return new UnsupportedOperationException("A carried compiled value is read-only");
    }
}
