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

package org.finos.legend.pure.m4.coreinstance.primitive;

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
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.tools.SafeAppendable;
import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;

import java.math.BigDecimal;
import java.math.BigInteger;

public abstract class PrimitiveCoreInstance<T> extends AbstractCoreInstance
{
    private static final CompileStateSet COMPILE_STATES = CompileStateSet.with(CompileState.PROCESSED, CompileState.VALIDATED);

    private final T value;
    private final int internalSyntheticId;
    private final CoreInstance classifier;

    protected PrimitiveCoreInstance(T value, CoreInstance classifier, int internalSyntheticId)
    {
        if (value == null)
        {
            throw new IllegalArgumentException("Primitive value may not be null");
        }
        this.value = value;
        this.classifier = classifier;
        this.internalSyntheticId = internalSyntheticId;
    }

    public T getValue()
    {
        return this.value;
    }

    @Override
    public String toString()
    {
        return getName() + "(" + this.internalSyntheticId + ") instanceOf " + this.classifier.getName();
    }

    @Override
    public CoreInstance getClassifier()
    {
        return this.classifier;
    }

    @Override
    public ModelRepository getRepository()
    {
        return this.classifier.getRepository();
    }

    @Override
    public int getSyntheticId()
    {
        return this.internalSyntheticId;
    }

    @Override
    public void setName(String name)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setClassifier(CoreInstance classifier)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public SourceInformation getSourceInformation()
    {
        return null;
    }

    @Override
    public void setSourceInformation(SourceInformation sourceInformation)
    {
        throw new UnsupportedOperationException();
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
    public void addKeyWithEmptyList(ListIterable<String> key)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void modifyValueForToManyMetaProperty(String key, int offset, CoreInstance value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeProperty(String keyName)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public CoreInstance getKeyByName(String name)
    {
        return null;
    }

    @Override
    public CoreInstance getValueForMetaPropertyToOne(String propertyName)
    {
        return null;
    }

    @Override
    public CoreInstance getValueForMetaPropertyToOne(CoreInstance property)
    {
        return null;
    }

    @Override
    public ListIterable<CoreInstance> getValueForMetaPropertyToMany(String keyName)
    {
        return Lists.immutable.empty();
    }

    @Override
    public ListIterable<CoreInstance> getValueForMetaPropertyToMany(CoreInstance key)
    {
        return Lists.immutable.empty();
    }

    @Override
    public CoreInstance getValueInValueForMetaPropertyToMany(String keyName, String keyInMany)
    {
        return null;
    }

    @Override
    public CoreInstance getValueInValueForMetaPropertyToManyWithKey(String keyName, String key, String keyInMany)
    {
        return null;
    }

    @Override
    public <K> CoreInstance getValueInValueForMetaPropertyToManyByIDIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        return null;
    }

    @Override
    public <K> ListIterable<CoreInstance> getValueInValueForMetaPropertyToManyByIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        return Lists.immutable.empty();
    }

    @Override
    public boolean isValueDefinedForKey(String keyName)
    {
        return false;
    }

    @Override
    public void removeValueForMetaPropertyToMany(String keyName, CoreInstance coreInstance)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setKeyValues(ListIterable<String> key, ListIterable<? extends CoreInstance> value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addKeyValue(ListIterable<String> key, CoreInstance value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void printFull(Appendable appendable, String tab)
    {
        print(appendable, tab, true);
    }

    @Override
    public void print(Appendable appendable, String tab)
    {
        print(appendable, tab, false);
    }

    @Override
    public void print(Appendable appendable, String tab, int max)
    {
        print(appendable, tab);
    }

    @Override
    public void printWithoutDebug(Appendable appendable, String tab)
    {
        print(appendable, tab, false);
    }

    @Override
    public void printWithoutDebug(Appendable appendable, String tab, int max)
    {
        printWithoutDebug(appendable, tab);
    }

    @Override
    public void commit(ModelRepositoryTransaction transaction)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rollback(ModelRepositoryTransaction transaction)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void markProcessed()
    {
        // Do nothing
    }

    @Override
    public void markNotProcessed()
    {
        // Do nothing
    }

    @Override
    public boolean hasBeenProcessed()
    {
        return true;
    }

    @Override
    public void markValidated()
    {
        // Do nothing
    }

    @Override
    public void markNotValidated()
    {
        // Do nothing
    }

    @Override
    public boolean hasBeenValidated()
    {
        return true;
    }

    @Override
    public void addCompileState(CompileState state)
    {
        // Do nothing
    }

    @Override
    public void removeCompileState(CompileState state)
    {
        // Do nothing
        if (hasCompileState(state))
        {
            throw new IllegalArgumentException("Cannot remove compile state " + state + " from primitive instance");
        }
    }

    @Override
    public boolean hasCompileState(CompileState state)
    {
        return COMPILE_STATES.contains(state);
    }

    @Override
    public CompileStateSet getCompileStates()
    {
        return COMPILE_STATES;
    }

    @Override
    public void setCompileStatesFrom(CompileStateSet states)
    {
        if (!COMPILE_STATES.equals(states))
        {
            throw new IllegalArgumentException("Cannot set compile state to " + states + " for primitive instance");
        }
    }

    @Override
    public void validate(MutableSet<CoreInstance> doneList) throws PureCompilationException
    {
        // Do nothing
    }

    private void print(Appendable appendable, String tab, boolean full)
    {
        print(SafeAppendable.wrap(appendable), tab, full);
    }

    private void print(SafeAppendable appendable, String tab, boolean full)
    {
        appendable.append(tab).append(getName());
        if (full)
        {
            appendable.append('_').append(this.internalSyntheticId);
        }

        appendable.append(" instance ").append(this.classifier.getName());
        if (full)
        {
            appendable.append('_').append(this.classifier.getSyntheticId());
        }
    }

    public static BooleanCoreInstance newBooleanCoreInstance(boolean value, CoreInstance classifier, int internalSyntheticId)
    {
        return new BooleanCoreInstance(value, classifier, internalSyntheticId);
    }

    public static DateCoreInstance newDateCoreInstance(PureDate value, CoreInstance classifier, int internalSyntheticId)
    {
        return new DateCoreInstance(value, classifier, internalSyntheticId);
    }

    public static StrictTimeCoreInstance newStrictTimeCoreInstance(PureStrictTime value, CoreInstance classifier, int internalSyntheticId)
    {
        return new StrictTimeCoreInstance(value, classifier, internalSyntheticId);
    }

    public static FloatCoreInstance newFloatCoreInstance(BigDecimal value, CoreInstance classifier, int internalSyntheticId)
    {
        String plainString = value.toPlainString();
        int decimalIndex = plainString.indexOf('.');
        if (decimalIndex == -1)
        {
            value = new BigDecimal(plainString + ".0");
        }
        else
        {
            int index = plainString.length() - 1;
            while ((index > decimalIndex) && (plainString.charAt(index) == '0'))
            {
                index--;
            }
            index++;
            if (index < plainString.length())
            {
                value = new BigDecimal(plainString.substring(0, Math.max(index, decimalIndex + 2)));
            }
        }
        return new FloatCoreInstance(value, classifier, internalSyntheticId);
    }

    public static DecimalCoreInstance newDecimalCoreInstance(BigDecimal value, CoreInstance classifier, int internalSyntheticId)
    {
        return new DecimalCoreInstance(value, classifier, internalSyntheticId);
    }

    public static IntegerCoreInstance newIntegerCoreInstance(Integer value, CoreInstance classifier, int internalSyntheticId)
    {
        return new IntegerCoreInstance(value, classifier, internalSyntheticId);
    }

    public static IntegerCoreInstance newIntegerCoreInstance(Long value, CoreInstance classifier, int internalSyntheticId)
    {
        return new IntegerCoreInstance(value, classifier, internalSyntheticId);
    }

    public static IntegerCoreInstance newIntegerCoreInstance(BigInteger value, CoreInstance classifier, int internalSyntheticId)
    {
        return new IntegerCoreInstance(value, classifier, internalSyntheticId);
    }

    public static StringCoreInstance newStringCoreInstance(String value, CoreInstance classifier, int internalSyntheticId)
    {
        return new StringCoreInstance(value, classifier, internalSyntheticId);
    }

    public static ByteCoreInstance newByteCoreInstance(Byte value, CoreInstance classifier, int internalSyntheticId)
    {
        return new ByteCoreInstance(value, classifier, internalSyntheticId);
    }
}
