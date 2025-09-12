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
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileStateSet;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecifications;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;

/**
 * Core Pure instance
 */
public interface CoreInstance
{
    int DEFAULT_MAX_PRINT_DEPTH = 1;

    /**
     * Function to get the name of a core instance.
     */
    Function<CoreInstance, String> GET_NAME = CoreInstance::getName;

    /**
     * Function to get the classifier of a core instance.
     */
    Function<CoreInstance, CoreInstance> GET_CLASSIFIER = CoreInstance::getClassifier;

    /**
     * Function to get the source information of a core instance.
     */
    Function<CoreInstance, SourceInformation> GET_SOURCE_INFO = CoreInstance::getSourceInformation;

    Procedure2<CoreInstance, ModelRepositoryTransaction> COMMIT = CoreInstance::commit;

    Procedure2<CoreInstance, ModelRepositoryTransaction> ROLL_BACK = CoreInstance::rollback;

    /**
     * Get the model repository in which this instance is defined.
     *
     * @return model repository
     */
    ModelRepository getRepository();

    /**
     * Get the synthetic id of the instance.  This is usually
     * assigned by the model repository.
     *
     * @return synthetic id
     */
    int getSyntheticId();

    /**
     * Get the name of the instance.
     *
     * @return instance name
     */
    String getName();

    /**
     * Set the name of the instance.
     *
     * @param name instance name
     */
    void setName(String name);

    /**
     * Get the instance classifier.  This is the type
     * of the instance in the model.
     *
     * @return instance classifier
     */
    CoreInstance getClassifier();

    /**
     * Set the instance classifier.  This is the type
     * of the instance in the model.
     *
     * @param classifier classifier
     */
    void setClassifier(CoreInstance classifier);

    /**
     * Get the source information for the instance.  This
     * givens information on where the instance is defined.
     * This may be null.
     *
     * @return source information or null
     */
    SourceInformation getSourceInformation();

    /**
     * Set the source information for the instance.  This
     * may be null.
     *
     * @param sourceInformation source information
     */
    void setSourceInformation(SourceInformation sourceInformation);

    /**
     * Returns whether the instance is persistent.  That is,
     * whether it is intended to persist in the graph or
     * whether it is instead ephemeral.
     *
     * @return whether the instance is persistent
     */
    boolean isPersistent();

    RichIterable<String> getKeys();

    ListIterable<String> getRealKeyByName(String name);

    void addKeyWithEmptyList(ListIterable<String> key);

    void modifyValueForToManyMetaProperty(String key, int offset, CoreInstance value);

    default void removeProperty(CoreInstance propertyNameKey)
    {
        removeProperty(propertyNameKey.getName());
    }

    void removeProperty(String propertyNameKey);

    CoreInstance getKeyByName(String name);

    CoreInstance getValueForMetaPropertyToOne(String propertyName);

    default CoreInstance getValueForMetaPropertyToOne(CoreInstance property)
    {
        return getValueForMetaPropertyToOne(property.getName());
    }

    ListIterable<? extends CoreInstance> getValueForMetaPropertyToMany(String keyName);

    default ListIterable<? extends CoreInstance> getValueForMetaPropertyToMany(CoreInstance key)
    {
        return getValueForMetaPropertyToMany(key.getName());
    }

    default CoreInstance getValueInValueForMetaPropertyToMany(String keyName, String keyInMany)
    {
        return getValueInValueForMetaPropertyToManyByIDIndex(keyName, IndexSpecifications.getCoreInstanceNameIndexSpec(), keyInMany);
    }

    default CoreInstance getValueInValueForMetaPropertyToManyWithKey(String keyName, String key, String keyInMany)
    {
        return getValueInValueForMetaPropertyToManyByIDIndex(keyName, IndexSpecifications.getPropertyValueNameIndexSpec(key), keyInMany);
    }

    <K> CoreInstance getValueInValueForMetaPropertyToManyByIDIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex);

    <K> ListIterable<? extends CoreInstance> getValueInValueForMetaPropertyToManyByIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex);

    boolean isValueDefinedForKey(String keyName);

    void removeValueForMetaPropertyToMany(String keyName, CoreInstance coreInstance);

    void setKeyValues(ListIterable<String> key, ListIterable<? extends CoreInstance> value);

    void addKeyValue(ListIterable<String> key, CoreInstance value);

    default void printFull(Appendable appendable, String tab)
    {
        // by default, just call print
        print(appendable, tab);
    }

    default void print(Appendable appendable, String tab)
    {
        print(appendable, tab, DEFAULT_MAX_PRINT_DEPTH);
    }

    void print(Appendable appendable, String tab, int max);

    default void printWithoutDebug(Appendable appendable, String tab)
    {
        printWithoutDebug(appendable, tab, DEFAULT_MAX_PRINT_DEPTH);
    }

    default void printWithoutDebug(Appendable appendable, String tab, int max)
    {
        // by default, just call print
        print(appendable, tab, max);
    }

    default String printFull(String tab)
    {
        StringBuilder builder = new StringBuilder();
        printFull(builder, tab);
        return builder.toString();
    }

    default String print(String tab)
    {
        StringBuilder builder = new StringBuilder();
        print(builder, tab);
        return builder.toString();
    }

    default String print(String tab, int max)
    {
        StringBuilder builder = new StringBuilder();
        print(builder, tab, max);
        return builder.toString();
    }

    default String printWithoutDebug(String tab)
    {
        StringBuilder builder = new StringBuilder();
        printWithoutDebug(builder, tab);
        return builder.toString();
    }

    default String printWithoutDebug(String tab, int max)
    {
        StringBuilder builder = new StringBuilder();
        printWithoutDebug(builder, tab, max);
        return builder.toString();
    }

    void commit(ModelRepositoryTransaction transaction);

    default void rollback(ModelRepositoryTransaction transaction)
    {
        // By default, rolling back is simply not committing
    }

    default void markProcessed()
    {
        addCompileState(CompileState.PROCESSED);
    }

    default void markNotProcessed()
    {
        markNotValidated();
        removeCompileState(CompileState.PROCESSED);
    }

    default boolean hasBeenProcessed()
    {
        return hasCompileState(CompileState.PROCESSED);
    }

    default void markValidated()
    {
        markProcessed();
        addCompileState(CompileState.VALIDATED);
    }

    default void markNotValidated()
    {
        removeCompileState(CompileState.VALIDATED);
    }

    default boolean hasBeenValidated()
    {
        return hasCompileState(CompileState.VALIDATED);
    }

    void addCompileState(CompileState state);

    void removeCompileState(CompileState state);

    boolean hasCompileState(CompileState state);

    CompileStateSet getCompileStates();

    void setCompileStatesFrom(CompileStateSet states);

    void validate(MutableSet<CoreInstance> doneList) throws PureCompilationException;
}
