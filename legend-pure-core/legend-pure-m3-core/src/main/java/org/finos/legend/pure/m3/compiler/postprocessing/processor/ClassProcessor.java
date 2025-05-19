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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassProjection;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class ClassProcessor extends Processor<Class<?>>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Class;
    }

    @Override
    public void process(Class<?> cls, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        if (!(cls instanceof ClassProjection))
        {
            processClass(cls, state, matcher, repository, processorSupport);
        }
    }

    private static void processClass(Class<?> cls, ProcessorState state, Matcher matcher, ModelRepository repository, ProcessorSupport processorSupport)
    {
        state.newTypeInferenceContext(cls);

        cls._typeVariables().forEach(v -> GenericType.resolveGenericTypeUsingImports(v._genericType(), repository, processorSupport));

        // TODO PURE-3436 Difficult to type this because of AbstractProperty hierarchy, plus PostProcessor.processElement takes CoreInstance
        MutableList<CoreInstance> propertiesProperties = Lists.mutable.empty();

        // Simple properties
        propertiesProperties.addAllIterable(cls._properties());
        propertiesProperties.addAllIterable(cls._propertiesFromAssociations());

        // Qualified properties
        propertiesProperties.addAllIterable(cls._qualifiedProperties());
        propertiesProperties.addAllIterable(cls._qualifiedPropertiesFromAssociations());

        // Orginal milestoned properties
        propertiesProperties.addAllIterable(cls._originalMilestonedProperties());

        for (CoreInstance property : propertiesProperties)
        {
            PostProcessor.processElement(matcher, property, state, processorSupport);
        }

        state.deleteTypeInferenceContext();
    }

    @Override
    public void populateReferenceUsages(Class<?> cls, ModelRepository repository, ProcessorSupport processorSupport)
    {
        if (!(cls instanceof ClassProjection))
        {
            TypeProcessor.processGeneralizationReferenceUsages(cls, repository, processorSupport);
        }
    }
}
