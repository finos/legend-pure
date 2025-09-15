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
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileStateSet;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.simple.SimplePrimitiveCoreInstances;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;

import java.math.BigDecimal;
import java.math.BigInteger;

public interface PrimitiveCoreInstance<T> extends CoreInstance
{
    T getValue();

    @Override
    default void setName(String name)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    default SourceInformation getSourceInformation()
    {
        return null;
    }

    @Override
    default void setSourceInformation(SourceInformation sourceInformation)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean isPersistent()
    {
        return false;
    }

    @Override
    default RichIterable<String> getKeys()
    {
        return Lists.immutable.empty();
    }

    @Override
    default ListIterable<String> getRealKeyByName(String name)
    {
        return null;
    }

    @Override
    default void addKeyWithEmptyList(ListIterable<String> key)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    default void modifyValueForToManyMetaProperty(String key, int offset, CoreInstance value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    default void removeProperty(String keyName)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    default CoreInstance getKeyByName(String name)
    {
        return null;
    }

    @Override
    default CoreInstance getValueForMetaPropertyToOne(String propertyName)
    {
        return null;
    }

    @Override
    default ListIterable<CoreInstance> getValueForMetaPropertyToMany(String keyName)
    {
        return Lists.immutable.empty();
    }

    @Override
    default CoreInstance getValueInValueForMetaPropertyToMany(String keyName, String keyInMany)
    {
        return null;
    }

    @Override
    default CoreInstance getValueInValueForMetaPropertyToManyWithKey(String keyName, String key, String keyInMany)
    {
        return null;
    }

    @Override
    default <K> CoreInstance getValueInValueForMetaPropertyToManyByIDIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        return null;
    }

    @Override
    default <K> ListIterable<CoreInstance> getValueInValueForMetaPropertyToManyByIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        return Lists.immutable.empty();
    }

    @Override
    default boolean isValueDefinedForKey(String keyName)
    {
        return false;
    }

    @Override
    default void removeValueForMetaPropertyToMany(String keyName, CoreInstance coreInstance)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    default void setKeyValues(ListIterable<String> key, ListIterable<? extends CoreInstance> value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    default void addKeyValue(ListIterable<String> key, CoreInstance value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    default void commit(ModelRepositoryTransaction transaction)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    default void markProcessed()
    {
        // Do nothing
    }

    @Override
    default void markNotProcessed()
    {
        // Do nothing
    }

    @Override
    default boolean hasBeenProcessed()
    {
        return true;
    }

    @Override
    default void markValidated()
    {
        // Do nothing
    }

    @Override
    default void markNotValidated()
    {
        // Do nothing
    }

    @Override
    default boolean hasBeenValidated()
    {
        return true;
    }

    @Override
    default void addCompileState(CompileState state)
    {
        // Do nothing
    }

    @Override
    default void removeCompileState(CompileState state)
    {
        // Do nothing
        if (hasCompileState(state))
        {
            throw new IllegalArgumentException("Cannot remove compile state " + state + " from primitive instance");
        }
    }

    @Override
    default boolean hasCompileState(CompileState state)
    {
        return CompileStateSet.PROCESSED_VALIDATED.contains(state);
    }

    @Override
    default CompileStateSet getCompileStates()
    {
        return CompileStateSet.PROCESSED_VALIDATED;
    }

    @Override
    default void setCompileStatesFrom(CompileStateSet states)
    {
        if (!CompileStateSet.PROCESSED_VALIDATED.equals(states))
        {
            throw new IllegalArgumentException("Cannot set compile state to " + states + " for primitive instance");
        }
    }

    @Override
    default void validate(MutableSet<CoreInstance> doneList) throws PureCompilationException
    {
        // Do nothing
    }

    PrimitiveCoreInstance<T> copy();

    @Deprecated
    static BooleanCoreInstance newBooleanCoreInstance(boolean value, CoreInstance classifier, int internalSyntheticId)
    {
        return SimplePrimitiveCoreInstances.newBooleanCoreInstance(value, classifier, internalSyntheticId);
    }

    @Deprecated
    static ByteCoreInstance newByteCoreInstance(Byte value, CoreInstance classifier, int internalSyntheticId)
    {
        return SimplePrimitiveCoreInstances.newByteCoreInstance(value, classifier, internalSyntheticId);
    }

    @Deprecated
    static DateCoreInstance newDateCoreInstance(PureDate value, CoreInstance classifier, int internalSyntheticId)
    {
        return SimplePrimitiveCoreInstances.newDateCoreInstance(value, classifier, internalSyntheticId);
    }

    @Deprecated
    static DecimalCoreInstance newDecimalCoreInstance(BigDecimal value, CoreInstance classifier, int internalSyntheticId)
    {
        return SimplePrimitiveCoreInstances.newDecimalCoreInstance(value, classifier, internalSyntheticId);
    }

    @Deprecated
    static FloatCoreInstance newFloatCoreInstance(BigDecimal value, CoreInstance classifier, int internalSyntheticId)
    {
        return SimplePrimitiveCoreInstances.newFloatCoreInstance(value, classifier, internalSyntheticId);
    }

    @Deprecated
    static IntegerCoreInstance newIntegerCoreInstance(Integer value, CoreInstance classifier, int internalSyntheticId)
    {
        return SimplePrimitiveCoreInstances.newIntegerCoreInstance(value, classifier, internalSyntheticId);
    }

    @Deprecated
    static IntegerCoreInstance newIntegerCoreInstance(Long value, CoreInstance classifier, int internalSyntheticId)
    {
        return SimplePrimitiveCoreInstances.newIntegerCoreInstance(value, classifier, internalSyntheticId);
    }

    @Deprecated
    static IntegerCoreInstance newIntegerCoreInstance(BigInteger value, CoreInstance classifier, int internalSyntheticId)
    {
        return SimplePrimitiveCoreInstances.newIntegerCoreInstance(value, classifier, internalSyntheticId);
    }

    @Deprecated
    static StrictTimeCoreInstance newStrictTimeCoreInstance(PureStrictTime value, CoreInstance classifier, int internalSyntheticId)
    {
        return SimplePrimitiveCoreInstances.newStrictTimeCoreInstance(value, classifier, internalSyntheticId);
    }

    @Deprecated
    static StringCoreInstance newStringCoreInstance(String value, CoreInstance classifier, int internalSyntheticId)
    {
        return SimplePrimitiveCoreInstances.newStringCoreInstance(value, classifier, internalSyntheticId);
    }
}
