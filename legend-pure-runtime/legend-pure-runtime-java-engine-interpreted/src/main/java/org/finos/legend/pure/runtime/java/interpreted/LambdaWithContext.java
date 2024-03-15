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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.ReferenceUsage;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.constraint.Constraint;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.TaggedValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ElementOverride;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileStateSet;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;

public class LambdaWithContext implements LambdaFunction<CoreInstance>
{
    private final LambdaFunction lambda;
    private final VariableContext variableContext;

    LambdaWithContext(CoreInstance lambda, VariableContext variableContext)
    {
        this.lambda = LambdaFunctionCoreInstanceWrapper.toLambdaFunction(lambda);
        this.variableContext = variableContext;
    }

    public VariableContext getVariableContext()
    {
        return this.variableContext;
    }

    @Override
    public ModelRepository getRepository()
    {
        return this.lambda.getRepository();
    }

    @Override
    public int getSyntheticId()
    {
        return this.lambda.getSyntheticId();
    }

    @Override
    public String getName()
    {
        return this.lambda.getName();
    }

    @Override
    public void setName(String name)
    {
        this.lambda.setName(name);
    }

    @Override
    public CoreInstance getClassifier()
    {
        return this.lambda.getClassifier();
    }

    @Override
    public void setClassifier(CoreInstance classifier)
    {
        this.lambda.setClassifier(classifier);
    }

    @Override
    public SourceInformation getSourceInformation()
    {
        return this.lambda.getSourceInformation();
    }

    @Override
    public void setSourceInformation(SourceInformation sourceInformation)
    {
        this.lambda.setSourceInformation(sourceInformation);
    }

    @Override
    public boolean isPersistent()
    {
        return false;
    }

    @Override
    public void addKeyWithEmptyList(ListIterable<String> key)
    {
        this.lambda.addKeyWithEmptyList(key);
    }

    @Override
    public void modifyValueForToManyMetaProperty(String key, int offset, CoreInstance value)
    {
        this.lambda.modifyValueForToManyMetaProperty(key, offset, value);
    }

    @Override
    public void removeProperty(CoreInstance propertyNameKey)
    {
        this.lambda.removeProperty(propertyNameKey);
    }

    @Override
    public void removeProperty(String keyName)
    {
        this.lambda.removeProperty(keyName);
    }

    @Override
    public CoreInstance getKeyByName(String name)
    {
        return this.lambda.getKeyByName(name);
    }

    @Override
    public CoreInstance getValueForMetaPropertyToOne(String propertyName)
    {
        return this.lambda.getValueForMetaPropertyToOne(propertyName);
    }

    @Override
    public CoreInstance getValueForMetaPropertyToOne(CoreInstance property)
    {
        return this.lambda.getValueForMetaPropertyToOne(property);
    }

    @Override
    public ListIterable<? extends CoreInstance> getValueForMetaPropertyToMany(String keyName)
    {
        return this.lambda.getValueForMetaPropertyToMany(keyName);
    }

    @Override
    public ListIterable<? extends CoreInstance> getValueForMetaPropertyToMany(CoreInstance key)
    {
        return this.lambda.getValueForMetaPropertyToMany(key);
    }

    @Override
    public CoreInstance getValueInValueForMetaPropertyToMany(String keyName, String keyInMany)
    {
        return this.lambda.getValueInValueForMetaPropertyToMany(keyName, keyInMany);
    }

    @Override
    public CoreInstance getValueInValueForMetaPropertyToManyWithKey(String keyName, String key, String keyInMany)
    {
        return this.lambda.getValueInValueForMetaPropertyToManyWithKey(keyName, key, keyInMany);
    }

    @Override
    public <K> CoreInstance getValueInValueForMetaPropertyToManyByIDIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        return this.lambda.getValueInValueForMetaPropertyToManyByIDIndex(keyName, indexSpec, keyInIndex);
    }

    @Override
    public <K> ListIterable<? extends CoreInstance> getValueInValueForMetaPropertyToManyByIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        return this.lambda.getValueInValueForMetaPropertyToManyByIndex(keyName, indexSpec, keyInIndex);
    }

    @Override
    public boolean isValueDefinedForKey(String keyName)
    {
        return this.lambda.isValueDefinedForKey(keyName);
    }

    @Override
    public void removeValueForMetaPropertyToMany(String keyName, CoreInstance coreInstance)
    {
        this.lambda.removeValueForMetaPropertyToMany(keyName, coreInstance);
    }

    @Override
    public RichIterable<String> getKeys()
    {
        return this.lambda.getKeys();
    }

    @Override
    public ListIterable<String> getRealKeyByName(String name)
    {
        return this.lambda.getRealKeyByName(name);
    }

    @Override
    public void validate(MutableSet<CoreInstance> doneList) throws PureCompilationException
    {
        this.lambda.validate(doneList);
    }

    @Override
    public void printFull(Appendable appendable, String tab)
    {
        this.lambda.printFull(appendable, tab);
    }

    @Override
    public void print(Appendable appendable, String tab)
    {
        this.lambda.print(appendable, tab);
    }

    @Override
    public void print(Appendable appendable, String tab, int max)
    {
        this.lambda.print(appendable, tab, max);
    }

    @Override
    public void printWithoutDebug(Appendable appendable, String tab)
    {
        this.lambda.printWithoutDebug(appendable, tab);
    }

    @Override
    public void printWithoutDebug(Appendable appendable, String tab, int max)
    {
        this.lambda.printWithoutDebug(appendable, tab, max);
    }

    @Override
    public String printFull(String tab)
    {
        return this.lambda.printFull(tab);
    }

    @Override
    public String print(String tab)
    {
        return this.lambda.print(tab);
    }

    @Override
    public String print(String tab, int max)
    {
        return this.lambda.print(tab, max);
    }

    @Override
    public String printWithoutDebug(String tab)
    {
        return this.lambda.printWithoutDebug(tab);
    }

    @Override
    public String printWithoutDebug(String tab, int max)
    {
        return this.lambda.printWithoutDebug(tab, max);
    }

    @Override
    public void setKeyValues(ListIterable<String> key, ListIterable<? extends CoreInstance> value)
    {
        this.lambda.setKeyValues(key, value);
    }

    @Override
    public void addKeyValue(ListIterable<String> key, CoreInstance value)
    {
        this.lambda.addKeyValue(key, value);
    }

    @Override
    public void commit(ModelRepositoryTransaction transaction)
    {
        this.lambda.commit(transaction);
    }

    @Override
    public void rollback(ModelRepositoryTransaction transaction)
    {
        this.lambda.rollback(transaction);
    }

    @Override
    public void markProcessed()
    {
        this.lambda.markProcessed();
    }

    @Override
    public void markNotProcessed()
    {
        this.lambda.markNotProcessed();
    }

    @Override
    public boolean hasBeenProcessed()
    {
        return this.lambda.hasBeenProcessed();
    }

    @Override
    public void markValidated()
    {
        this.lambda.markValidated();
    }

    @Override
    public void markNotValidated()
    {
        this.lambda.markNotValidated();
    }

    @Override
    public boolean hasBeenValidated()
    {
        return this.lambda.hasBeenValidated();
    }

    @Override
    public void addCompileState(CompileState state)
    {
        this.lambda.addCompileState(state);
    }

    @Override
    public void removeCompileState(CompileState state)
    {
        this.lambda.removeCompileState(state);
    }

    @Override
    public boolean hasCompileState(CompileState state)
    {
        return this.lambda.hasCompileState(state);
    }

    @Override
    public CompileStateSet getCompileStates()
    {
        return this.lambda.getCompileStates();
    }

    @Override
    public void setCompileStatesFrom(CompileStateSet states)
    {
        this.lambda.setCompileStatesFrom(states);
    }

    @Override
    public RichIterable<? extends ValueSpecification> _expressionSequence()
    {
        return this.lambda._expressionSequence();
    }

    @Override
    public LambdaWithContext _expressionSequence(RichIterable<? extends ValueSpecification> values)
    {
        this.lambda._expressionSequence(values);
        return this;
    }

    @Override
    public String _functionName()
    {
        return this.lambda._functionName();
    }

    @Override
    public RichIterable<? extends FunctionExpression> _applications()
    {
        return this.lambda._applications();
    }

    @Override
    public LambdaWithContext _functionName(String value)
    {
        this.lambda._functionName(value);
        return this;
    }

    @Override
    public GenericType _classifierGenericType()
    {
        return this.lambda._classifierGenericType();
    }

    @Override
    public ElementOverride _elementOverride()
    {
        return this.lambda._elementOverride();
    }

    @Override
    public LambdaWithContext _classifierGenericType(GenericType value)
    {
        this.lambda._classifierGenericType(value);
        return this;
    }

    @Override
    public LambdaWithContext _elementOverride(ElementOverride value)
    {
        this.lambda._elementOverride(value);
        return this;
    }

    @Override
    public RichIterable<? extends String> _openVariables()
    {
        return this.lambda._openVariables();
    }

    @Override
    public LambdaWithContext _openVariables(RichIterable<? extends String> values)
    {
        this.lambda._openVariables(values);
        return this;
    }

    @Override
    public LambdaWithContext _openVariablesAdd(String value)
    {
        this.lambda._openVariablesAdd(value);
        return this;
    }

    @Override
    public LambdaWithContext _functionNameRemove()
    {
        this.lambda._functionNameRemove();
        return this;
    }

    @Override
    public LambdaWithContext _elementOverrideRemove()
    {
        this.lambda._elementOverrideRemove();
        return this;
    }

    @Override
    public LambdaWithContext _openVariablesRemove()
    {
        this.lambda._openVariablesRemove();
        return this;
    }

    @Override
    public LambdaFunction<CoreInstance> _expressionSequenceRemove()
    {
        this.lambda._expressionSequenceRemove();
        return this;
    }

    @Override
    public LambdaFunction<CoreInstance> _applicationsRemove()
    {
        this.lambda._applicationsRemove();
        return this;
    }

    @Override
    public LambdaFunction<CoreInstance> _classifierGenericTypeRemove()
    {
        this.lambda._classifierGenericTypeRemove();
        return this;
    }

    @Override
    public LambdaFunction<CoreInstance> _expressionSequenceAdd(ValueSpecification value)
    {
        this.lambda._expressionSequenceAdd(value);
        return this;
    }

    @Override
    public LambdaFunction<CoreInstance> _applicationsAdd(FunctionExpression value)
    {
        this.lambda._applicationsAdd(value);
        return this;
    }

    @Override
    public LambdaFunction _openVariablesAddAll(RichIterable<? extends String> values)
    {
        this.lambda._openVariablesAddAll(values);
        return this;
    }

    @Override
    public LambdaFunction _openVariablesRemove(String value)
    {
        this.lambda._openVariablesRemove(value);
        return this;
    }

    @Override
    public LambdaFunction<CoreInstance> _expressionSequenceAddAll(RichIterable<? extends ValueSpecification> values)
    {
        this.lambda._expressionSequenceAddAll(values);
        return this;
    }

    @Override
    public LambdaFunction<CoreInstance> _expressionSequenceRemove(ValueSpecification value)
    {
        this.lambda._expressionSequenceRemove(value);
        return this;
    }

    @Override
    public LambdaFunction<CoreInstance> _applicationsRemove(FunctionExpression value)
    {
        this.lambda._applicationsRemove(value);
        return this;
    }

    @Override
    public LambdaFunction<CoreInstance> _applications(RichIterable<? extends FunctionExpression> values)
    {
        this.lambda._applications(values);
        return this;
    }

    @Override
    public LambdaFunction<CoreInstance> _applicationsAddAll(RichIterable<? extends FunctionExpression> values)
    {
        this.lambda._applicationsAddAll(values);
        return this;
    }

    @Override
    public String _name()
    {
        return this.lambda._name();
    }

    @Override
    public LambdaFunction<CoreInstance> _name(String value)
    {
        this.lambda._name(value);
        return this;
    }

    @Override
    public LambdaFunction<CoreInstance> _nameRemove()
    {
        this.lambda._nameRemove();
        return this;
    }

    @Override
    public RichIterable<? extends ReferenceUsage> _referenceUsages()
    {
        return lambda._referenceUsages();
    }

    public LambdaFunction _referenceUsages(RichIterable values)
    {
        return lambda._referenceUsages(values);
    }

    @Override
    public LambdaFunction _referenceUsagesRemove()
    {
        return lambda._referenceUsagesRemove();
    }

    @Override
    public LambdaFunction _referenceUsagesAdd(ReferenceUsage value)
    {
        return lambda._referenceUsagesAdd(value);
    }

    public LambdaFunction _referenceUsagesAddAll(RichIterable values)
    {
        return lambda._referenceUsagesAddAll(values);
    }

    @Override
    public LambdaFunction _referenceUsagesRemove(ReferenceUsage value)
    {
        return lambda._referenceUsagesRemove(value);
    }

    @Override
    public LambdaFunction<CoreInstance> copy()
    {
        LambdaFunction<CoreInstance> copy = new LambdaWithContext(this.lambda, this.variableContext);
        copy._referenceUsages(this._referenceUsages());
        copy._openVariables(this._openVariables());
        copy._name(this._name());
        copy._functionName(this._functionName());
        copy._applications(this._applications());
        copy._expressionSequence(this._expressionSequence());
        copy._classifierGenericType(this._classifierGenericType());
        copy._elementOverride(this._elementOverride());
        return copy;
    }

    @Override
    public String getFullSystemPath()
    {
        return "Root::meta::pure::metamodel::function::LambdaFunction";
    }
}
