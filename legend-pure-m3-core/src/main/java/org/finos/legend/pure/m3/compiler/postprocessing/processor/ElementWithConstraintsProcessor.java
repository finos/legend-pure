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
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.constraint.Constraint;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.ElementWithConstraints;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ClassConstraintValueSpecificationContext;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;


public class ElementWithConstraintsProcessor extends Processor<ElementWithConstraints>
{
    @Override
    public String getClassName()
    {
        return M3Paths.ElementWithConstraints;
    }

    @Override
    public void process(ElementWithConstraints instance, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        this.processConstraints(instance, state, matcher, processorSupport);
    }

    private void processConstraints(ElementWithConstraints cls, ProcessorState state, Matcher matcher, ProcessorSupport processorSupport)
    {

        RichIterable<? extends Constraint> constraints = cls._constraints();
        if (constraints.notEmpty())
        {
            state.getVariableContext().buildAndRegister("this", (GenericType) Type.wrapGenericType(cls, processorSupport), (Multiplicity) processorSupport.package_getByUserPath(M3Paths.PureOne), processorSupport);
            int i = 0;
            for (Constraint constraint : constraints)
            {
                FunctionDefinition<?> constraintFn = constraint._functionDefinition();
                ValueSpecification constraintFnExpressionSequence = constraintFn._expressionSequence().toList().getFirst();
                PostProcessor.processElement(matcher, constraintFnExpressionSequence, state, processorSupport);
                this.addConstraintUsageContext(constraintFnExpressionSequence, cls, i, processorSupport);
                i++;

                if (constraint._messageFunction() != null)
                {
                    FunctionDefinition<?> constraintMessage = constraint._messageFunction();
                    ValueSpecification constraintMessageExpressionSequence = constraintMessage._expressionSequence().toList().getFirst();
                    PostProcessor.processElement(matcher, constraintMessageExpressionSequence, state, processorSupport);
                    this.addConstraintUsageContext(constraintMessageExpressionSequence, cls, i, processorSupport);
                    i++;
                }
            }
        }
    }

    private void addConstraintUsageContext(ValueSpecification expressionSequence, ElementWithConstraints cls, int offset, ProcessorSupport processorSupport)
    {
        if (expressionSequence != null)
        {
            ClassConstraintValueSpecificationContext usageContext = (ClassConstraintValueSpecificationContext) processorSupport.newAnonymousCoreInstance(null, M3Paths.ClassConstraintValueSpecificationContext);
            usageContext._offset(offset);
            usageContext._classCoreInstance(cls);
            expressionSequence._usageContext(usageContext);
        }
    }

    @Override
    public void populateReferenceUsages(ElementWithConstraints instance, ModelRepository repository, ProcessorSupport processorSupport)
    {
    }
}
