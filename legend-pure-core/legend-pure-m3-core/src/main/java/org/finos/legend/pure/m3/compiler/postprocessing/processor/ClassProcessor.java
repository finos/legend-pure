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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.GenericTypeTraceability;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassProjection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class ClassProcessor extends Processor<Class>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Class;
    }

    @Override
    public void process(Class cls, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        if (!(cls instanceof ClassProjection))
        {
            processClass(cls, state, matcher, repository, processorSupport);
        }
    }

    private static void processClass(Class cls, ProcessorState state, Matcher matcher, ModelRepository repository, ProcessorSupport processorSupport)
    {
        state.newTypeInferenceContext(cls);

        // TODO PURE-3436 Difficult to type this because of AbstractProperty hierarchy, plus PostProcessor.processElement takes CoreInstance
        MutableList<CoreInstance> propertiesProperties = FastList.newList();

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
    public void populateReferenceUsages(Class cls, ModelRepository repository, ProcessorSupport processorSupport)
    {
        if (!(cls instanceof ClassProjection))
        {
            processClassReferenceUsages(cls, repository, processorSupport);
        }
    }

    public static void processClassReferenceUsages(Class cls, ModelRepository repository, ProcessorSupport processorSupport)
    {
        RichIterable<? extends Generalization> generalizations = cls._generalizations();
        if (generalizations.notEmpty())
        {
            Type topType = (Type)processorSupport.type_TopType();
            for (Generalization generalization : generalizations)
            {
                Type generalType = generalization._general() == null ? null : (Type)ImportStub.withImportStubByPass(generalization._general()._rawTypeCoreInstance(), processorSupport);
                if (generalType != topType)
                {
                    GenericTypeTraceability.addTraceForGeneralization(generalization, repository, processorSupport);
                }
            }
        }
    }

}
