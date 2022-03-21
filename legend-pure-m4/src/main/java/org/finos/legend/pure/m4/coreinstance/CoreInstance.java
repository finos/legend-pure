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
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;

/**
 * Core Pure instance
 */
public interface CoreInstance
{
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

    void removeProperty(CoreInstance propertyNameKey);

    void removeProperty(String propertyNameKey);

    CoreInstance getKeyByName(String name);

    CoreInstance getValueForMetaPropertyToOne(String propertyName);

    CoreInstance getValueForMetaPropertyToOne(CoreInstance property);

    ListIterable<? extends CoreInstance> getValueForMetaPropertyToMany(String keyName);

    ListIterable<? extends CoreInstance> getValueForMetaPropertyToMany(CoreInstance key);

    CoreInstance getValueInValueForMetaPropertyToMany(String keyName, String keyInMany);

    CoreInstance getValueInValueForMetaPropertyToManyWithKey(String keyName, String key, String keyInMany);

    <K> CoreInstance getValueInValueForMetaPropertyToManyByIDIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex);

    <K> ListIterable<? extends CoreInstance> getValueInValueForMetaPropertyToManyByIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex);

    boolean isValueDefinedForKey(String keyName);

    void removeValueForMetaPropertyToMany(String keyName, CoreInstance coreInstance);

    void setKeyValues(ListIterable<String> key, ListIterable<? extends CoreInstance> value);

    void addKeyValue(ListIterable<String> key, CoreInstance value);

    void printFull(Appendable appendable, String tab);

    void print(Appendable appendable, String tab);

    void print(Appendable appendable, String tab, int max);

    void printWithoutDebug(Appendable appendable, String tab);

    void printWithoutDebug(Appendable appendable, String tab, int max);

    String printFull(String tab);

    String print(String tab);

    String print(String tab, int max);

    String printWithoutDebug(String tab);

    String printWithoutDebug(String tab, int max);

    void commit(ModelRepositoryTransaction transaction);

    void rollback(ModelRepositoryTransaction transaction);

    void markProcessed();

    void markNotProcessed();

    boolean hasBeenProcessed();

    void markValidated();

    void markNotValidated();

    boolean hasBeenValidated();

    void addCompileState(CompileState state);

    void removeCompileState(CompileState state);

    boolean hasCompileState(CompileState state);

    CompileStateSet getCompileStates();

    void setCompileStatesFrom(CompileStateSet states);

    void validate(MutableSet<CoreInstance> doneList) throws PureCompilationException;
}
