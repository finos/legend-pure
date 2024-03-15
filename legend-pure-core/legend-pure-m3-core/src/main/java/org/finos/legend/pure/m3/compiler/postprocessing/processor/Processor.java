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

package org.finos.legend.pure.m3.compiler.postprocessing.processor;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.compiler.ReferenceUsage;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public abstract class Processor<T extends CoreInstance> implements MatchRunner<T>
{
    @Override
    public void run(T instance, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorState processorState = (ProcessorState)state;
        ProcessorSupport processorSupport = processorState.getProcessorSupport();
        this.process(instance, processorState, matcher, modelRepository, context, processorSupport);
        this.populateReferenceUsages(instance, modelRepository, processorSupport);
    }

    public abstract void process(T instance, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport);

    public abstract void populateReferenceUsages(T instance, ModelRepository repository, ProcessorSupport processorSupport);

    protected void possiblyAddReferenceUsageForToOneProperty(CoreInstance instance, CoreInstance reference, String property, ModelRepository repository, ProcessorSupport processorSupport)
    {
        CoreInstance resolvedReference = ImportStub.withImportStubByPass(reference, processorSupport);
        if (resolvedReference != null)
        {
            this.addReferenceUsage(instance, resolvedReference, property, 0, repository, processorSupport);
        }
    }

    protected void addReferenceUsageForToOneProperty(CoreInstance instance, CoreInstance reference, String property, ModelRepository repository, ProcessorSupport processorSupport)
    {
        this.addReferenceUsageForToOneProperty(instance, reference, property, repository, processorSupport, null);
    }

    protected void addReferenceUsageForToOneProperty(CoreInstance instance, CoreInstance reference, String property, ModelRepository repository, ProcessorSupport processorSupport, SourceInformation sourceInformationForUsage)
    {
        CoreInstance resolvedReference = ImportStub.withImportStubByPass(reference, processorSupport);
        if (resolvedReference == null)
        {
            throw new PureCompilationException(instance.getSourceInformation(), "Could not add reference usage for property '" + property + "' on " + instance + " because no value was found");
        }
        this.addReferenceUsage(instance, resolvedReference, property, 0, repository, processorSupport, sourceInformationForUsage);
    }

    protected void addReferenceUsagesForToManyProperty(CoreInstance instance, RichIterable<? extends CoreInstance> values, String property, ModelRepository repository, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> valuesResolved = ImportStub.withImportStubByPasses(ListHelper.wrapListIterable(values), processorSupport);
        if (valuesResolved.notEmpty())
        {
            CoreInstance packageableElementClass = processorSupport.package_getByUserPath(M3Paths.PackageableElement);
            valuesResolved.forEachWithIndex((value, i) ->
            {
                if (Instance.instanceOf(value, packageableElementClass, processorSupport))
                {
                    this.addReferenceUsage(instance, value, property, i, repository, processorSupport);
                }
            });
        }
    }

    protected void addReferenceUsagesForToManyProperty(CoreInstance instance, RichIterable<? extends CoreInstance> values, String property, ModelRepository repository, ProcessorSupport processorSupport, RichIterable<? extends SourceInformation> sourceInformations)
    {
        ListIterable<? extends CoreInstance> valuesResolved = ImportStub.withImportStubByPasses(ListHelper.wrapListIterable(values), processorSupport);
        ListIterable<? extends SourceInformation> sourceInformationList = ListHelper.wrapListIterable(sourceInformations);
        if (valuesResolved.notEmpty())
        {
            CoreInstance packageableElementClass = processorSupport.package_getByUserPath(M3Paths.PackageableElement);
            valuesResolved.forEachWithIndex((value, i) ->
            {
                if (Instance.instanceOf(value, packageableElementClass, processorSupport))
                {
                    this.addReferenceUsage(instance, value, property, i, repository, processorSupport, sourceInformationList.get(i));
                }
            });
        }
    }

    protected void addReferenceUsage(CoreInstance instance, CoreInstance reference, String property, int offset, ModelRepository repository, ProcessorSupport processorSupport, SourceInformation optionalSourceInfo)
    {
        ReferenceUsage.addReferenceUsage(reference, instance, property, offset, repository, processorSupport, optionalSourceInfo);
    }

    protected void addReferenceUsage(CoreInstance instance, CoreInstance reference, String property, int offset, ModelRepository repository, ProcessorSupport processorSupport)
    {
        ReferenceUsage.addReferenceUsage(reference, instance, property, offset, repository, processorSupport);
    }
}
