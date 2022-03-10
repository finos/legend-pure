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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.*;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class OperationSetImplementationProcessor extends Processor<OperationSetImplementation>
{
    @Override
    public String getClassName()
    {
        return M2MappingPaths.OperationSetImplementation;
    }

    @Override
    public void process(OperationSetImplementation operationSetImplementation, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        Mapping mapping = (Mapping) ImportStub.withImportStubByPass(operationSetImplementation._parentCoreInstance(), processorSupport);
        RichIterable<? extends SetImplementationContainer> parameters = operationSetImplementation._parameters();
        if (parameters.notEmpty())
        {
            MapIterable<String, SetImplementation> classMappingsById = (MapIterable<String, SetImplementation>) org.finos.legend.pure.m2.dsl.mapping.Mapping.getClassMappingsById(mapping, processorSupport);
            for (SetImplementationContainer param : parameters)
            {
                String id = param._id();
                SetImplementation found = classMappingsById.get(id);
                if (found == null)
                {
                    StringBuilder builder = new StringBuilder("The SetImplementation '");
                    builder.append(id);
                    builder.append("' can't be found in the mapping '");
                    PackageableElement.writeUserPathForPackageableElement(builder, mapping);
                    builder.append('\'');
                    throw new PureCompilationException(operationSetImplementation.getSourceInformation(), builder.toString());
                }
                param._setImplementation(found);
            }
        }

        if (operationSetImplementation instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MergeOperationSetImplementation)
        {
            matcher.fullMatch(((MergeOperationSetImplementation) operationSetImplementation)._validationFunction(), state);
        }
    }

    @Override
    public void populateReferenceUsages(OperationSetImplementation operationSetImplementation, ModelRepository repository, ProcessorSupport processorSupport)
    {
        addReferenceUsageForToOneProperty(operationSetImplementation, operationSetImplementation._operationCoreInstance(), M3Properties.operation, repository, processorSupport);
    }
}

