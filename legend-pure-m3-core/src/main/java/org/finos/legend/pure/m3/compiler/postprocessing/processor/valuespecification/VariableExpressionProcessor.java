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

package org.finos.legend.pure.m3.compiler.postprocessing.processor.valuespecification;

import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class VariableExpressionProcessor implements MatchRunner<VariableExpression>
{
    @Override
    public String getClassName()
    {
        return M3Paths.VariableExpression;
    }

    @Override
    public void run(VariableExpression instance, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorState processorState = (ProcessorState) state;
        ProcessorSupport processorSupport = processorState.getProcessorSupport();
        SourceInformation varSourceInfo = instance.getSourceInformation();

        String name = instance._name();
        processorState.addVariable(name);
        ValueSpecification source = (ValueSpecification) processorState.getVariableContext().getValue(name);
        if (source == null)
        {
            throw new PureCompilationException(varSourceInfo, "The variable '" + name + "' is unknown!");
        }

        GenericType sourceGenericType = source._genericType();
        GenericType varGenericType = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericType(sourceGenericType, varSourceInfo, processorSupport);
        instance._genericType(varGenericType);

        Multiplicity sourceMultiplicity = source._multiplicity();
        Multiplicity varMultiplicity = (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.copyMultiplicity(sourceMultiplicity, varSourceInfo, processorSupport);
        instance._multiplicity(varMultiplicity);
    }
}
