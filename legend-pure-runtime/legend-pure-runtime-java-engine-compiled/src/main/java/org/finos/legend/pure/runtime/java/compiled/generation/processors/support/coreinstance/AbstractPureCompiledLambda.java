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

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.LambdaCompiledExtended;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction;

import java.util.Objects;

public abstract class AbstractPureCompiledLambda<T> extends ReflectiveCoreInstance implements LambdaCompiledExtended<T>
{
    private volatile CompiledExecutionSupport execSupport;
    private String lambdaId;
    private volatile LambdaFunction<T> lambdaFunction;
    private final SharedPureFunction<? extends T> pureFunction;

    private AbstractPureCompiledLambda(SharedPureFunction<? extends T> pureFunction, SourceInformation sourceInfo)
    {
        super("Anonymous_Lambda", sourceInfo);
        this.pureFunction = pureFunction;
    }

    protected AbstractPureCompiledLambda(LambdaFunction<T> lambdaFunction, SharedPureFunction<? extends T> pureFunction)
    {
        this(pureFunction, (lambdaFunction == null) ? null : lambdaFunction.getSourceInformation());
        this.lambdaFunction = lambdaFunction;
    }

    protected AbstractPureCompiledLambda(CompiledExecutionSupport executionSupport, String lambdaId, SharedPureFunction<? extends T> pureFunction)
    {
        this(pureFunction, null);
        this.execSupport = executionSupport;
        this.lambdaId = lambdaId;
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
                    if ((result != null) && (super.getSourceInformation() == null))
                    {
                        setSourceInformation(result.getSourceInformation());
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
    public CoreInstance getClassifier()
    {
        return lambdaFunction().getClassifier();
    }

    @Override
    public SourceInformation getSourceInformation()
    {
        SourceInformation sourceInfo = super.getSourceInformation();
        if ((sourceInfo == null) && (this.execSupport != null))
        {
            // initialize source info
            lambdaFunction();
            return super.getSourceInformation();
        }
        return sourceInfo;
    }

    @Override
    public String getFullSystemPath()
    {
        return "Root::" + M3Paths.LambdaFunction;
    }

    // TODO is this needed?
    public String __id()
    {
        LambdaFunction<T> lambda = lambdaFunction();
        return (lambda == null) ? "Anonymous_Lambda" : lambda.getName();
    }
}
