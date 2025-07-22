// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.LambdaCompiledExtended;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction;

import java.util.Objects;

public abstract class AbstractPureCompiledLambda<T> extends AbstractCompiledCoreInstance implements LambdaCompiledExtended<T>
{
    private volatile CompiledExecutionSupport execSupport;
    private String lambdaId;
    private volatile LambdaFunction<T> lambdaFunction;
    private final SharedPureFunction<? extends T> pureFunction;
    private volatile SourceInformation sourceInfo;

    protected AbstractPureCompiledLambda(LambdaFunction<T> lambdaFunction, SharedPureFunction<? extends T> pureFunction)
    {
        this.lambdaFunction = lambdaFunction;
        this.pureFunction = pureFunction;
        if (lambdaFunction != null)
        {
            this.sourceInfo = lambdaFunction.getSourceInformation();
        }
    }

    protected AbstractPureCompiledLambda(CompiledExecutionSupport executionSupport, String lambdaId, SharedPureFunction<? extends T> pureFunction)
    {
        this.execSupport = executionSupport;
        this.lambdaId = lambdaId;
        this.pureFunction = pureFunction;
    }

    protected AbstractPureCompiledLambda(ExecutionSupport executionSupport, String lambdaId, SharedPureFunction<? extends T> pureFunction)
    {
        this((CompiledExecutionSupport) executionSupport, lambdaId, pureFunction);
    }

    @Override
    public SharedPureFunction<? extends T> pureFunction()
    {
        return this.pureFunction;
    }

    @SuppressWarnings("unchecked")
    protected LambdaFunction<T> lambdaFunction()
    {
        if (this.execSupport != null)
        {
            synchronized (this)
            {
                CompiledExecutionSupport localExecSupport = this.execSupport;
                if (localExecSupport != null)
                {
                    LambdaFunction<T> result = this.lambdaFunction = (LambdaFunction<T>) localExecSupport.getMetadataAccessor().getLambdaFunction(this.lambdaId);
                    if ((result != null) && (this.sourceInfo == null))
                    {
                        this.sourceInfo = result.getSourceInformation();
                    }
                    this.execSupport = null;
                    this.lambdaId = null;
                    return result;
                }
            }
        }
        return this.lambdaFunction;
    }

    @Override
    public boolean pureEquals(Object obj)
    {
        return (this == obj) || ((obj != null) && Objects.equals(lambdaFunction(), obj));
    }

    @Override
    public int pureHashCode()
    {
        return Objects.hashCode(lambdaFunction());
    }

    @Override
    public ModelRepository getRepository()
    {
        return lambdaFunction().getRepository();
    }

    @Override
    public int getSyntheticId()
    {
        return lambdaFunction().getSyntheticId();
    }

    @Override
    public String getName()
    {
        return "Anonymous_Lambda";
    }

    @Override
    public void setName(String name)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public CoreInstance getClassifier()
    {
        return lambdaFunction().getClassifier();
    }

    @Override
    public void setClassifier(CoreInstance classifier)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public SourceInformation getSourceInformation()
    {
        SourceInformation local = this.sourceInfo;
        if ((local == null) && (this.execSupport != null))
        {
            // initialize source info
            lambdaFunction();
            return this.sourceInfo;
        }
        return local;
    }

    @Override
    public void setSourceInformation(SourceInformation sourceInformation)
    {
        this.sourceInfo = sourceInformation;
    }

    @Override
    public boolean isPersistent()
    {
        return lambdaFunction().isPersistent();
    }

    @Override
    public RichIterable<String> getKeys()
    {
        return lambdaFunction().getKeys();
    }

    @Override
    public ListIterable<String> getRealKeyByName(String name)
    {
        return lambdaFunction().getRealKeyByName(name);
    }

    @Override
    public void addKeyWithEmptyList(ListIterable<String> key)
    {
        lambdaFunction().addKeyWithEmptyList(key);
    }

    @Override
    public void modifyValueForToManyMetaProperty(String key, int offset, CoreInstance value)
    {
        lambdaFunction().modifyValueForToManyMetaProperty(key, offset, value);
    }

    @Override
    public CoreInstance getKeyByName(String name)
    {
        return lambdaFunction().getKeyByName(name);
    }

    @Override
    public CoreInstance getValueForMetaPropertyToOne(String propertyName)
    {
        return lambdaFunction().getValueForMetaPropertyToOne(propertyName);
    }

    @Override
    public ListIterable<? extends CoreInstance> getValueForMetaPropertyToMany(String keyName)
    {
        return lambdaFunction().getValueForMetaPropertyToMany(keyName);
    }

    @Override
    public <K> CoreInstance getValueInValueForMetaPropertyToManyByIDIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        return lambdaFunction().getValueInValueForMetaPropertyToManyByIDIndex(keyName, indexSpec, keyInIndex);
    }

    @Override
    public <K> ListIterable<? extends CoreInstance> getValueInValueForMetaPropertyToManyByIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        return lambdaFunction().getValueInValueForMetaPropertyToManyByIndex(keyName, indexSpec, keyInIndex);
    }

    @Override
    public boolean isValueDefinedForKey(String keyName)
    {
        return lambdaFunction().isValueDefinedForKey(keyName);
    }

    @Override
    public void removeValueForMetaPropertyToMany(String keyName, CoreInstance coreInstance)
    {
        lambdaFunction().removeValueForMetaPropertyToMany(keyName, coreInstance);
    }

    @Override
    public void setKeyValues(ListIterable<String> key, ListIterable<? extends CoreInstance> value)
    {
        lambdaFunction().setKeyValues(key, value);
    }

    @Override
    public void addKeyValue(ListIterable<String> key, CoreInstance value)
    {
        lambdaFunction().addKeyValue(key, value);
    }

    @Override
    public void commit(ModelRepositoryTransaction transaction)
    {
        lambdaFunction().commit(transaction);
    }

    @Override
    public void rollback(ModelRepositoryTransaction transaction)
    {
        lambdaFunction().rollback(transaction);
    }

    @Override
    public void validate(MutableSet<CoreInstance> doneList) throws PureCompilationException
    {
        lambdaFunction().validate(doneList);
    }

    @Override
    public String getFullSystemPath()
    {
        return "Root::" + M3Paths.LambdaFunction;
    }
}
