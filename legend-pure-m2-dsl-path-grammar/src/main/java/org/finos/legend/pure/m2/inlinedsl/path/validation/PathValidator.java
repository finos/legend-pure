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

package org.finos.legend.pure.m2.inlinedsl.path.validation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m2.inlinedsl.path.M2PathPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.validation.Validator;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.Path;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.PropertyPathElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class PathValidator implements MatchRunner<Path<?, ?>>
{
    @Override
    public String getClassName()
    {
        return M2PathPaths.Path;
    }

    @Override
    public void run(Path<?, ?> pathInstance, MatcherState state, Matcher matcher, ModelRepository modelRepository, final Context context) throws PureCompilationException
    {
        ValidatorState validatorState = (ValidatorState) state;
        GenericType genericType = pathInstance._start();
        ProcessorSupport processorSupport = validatorState.getProcessorSupport();
        Validator.validate(genericType, validatorState, matcher, processorSupport);

        pathInstance._path().forEach(pathElement ->
        {
            if (pathElement instanceof PropertyPathElement)
            {
                AbstractProperty<?> property = (AbstractProperty<?>) ImportStub.withImportStubByPass(((PropertyPathElement) pathElement)._propertyCoreInstance(), processorSupport);

                RichIterable<? extends VariableExpression> valueSpecifications = ((FunctionType) processorSupport.function_getFunctionType(property))._parameters();
                ListIterable<? extends VariableExpression> parameterSpecifications = valueSpecifications.toList().subList(1, valueSpecifications.size());
                ListIterable<? extends ValueSpecification> parameters = ListHelper.wrapListIterable(((PropertyPathElement) pathElement)._parameters());

                if (parameterSpecifications.size() != parameters.size())
                {
                    throw new PureCompilationException(pathInstance.getSourceInformation(), "Error finding match for function '" + property._functionName() + "'. Incorrect number of parameters, function expects " + parameterSpecifications.size() + " parameters");
                }

                parameterSpecifications.forEachWithIndex((valueSpecification, i) ->
                {
                    ValueSpecification parameter = parameters.get(i);
                    if (parameter instanceof InstanceValue)
                    {
                        ListIterable<? extends CoreInstance> values = ImportStub.withImportStubByPasses(ListHelper.wrapListIterable(((InstanceValue) parameter)._valuesCoreInstance()), processorSupport);

                        GenericType genericTypeSpecified = valueSpecification._genericType();
                        CoreInstance type = (values.size() == 1) ?
                                extractGenericType(values.getFirst(), processorSupport) :
                                org.finos.legend.pure.m3.navigation.generictype.GenericType.findBestCommonGenericType(values.collect(v -> extractGenericType(v, processorSupport)), true, false, processorSupport);

                        if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.subTypeOf(type, genericTypeSpecified, processorSupport))
                        {
                            throw new PureCompilationException(pathInstance.getSourceInformation(), "Parameter type mismatch for function '" + property._functionName()
                                    + "'. Expected:" + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(genericTypeSpecified, processorSupport)
                                    + ", Found:" + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(type, processorSupport));
                        }
                    }
                });
            }
        });
    }

    private static CoreInstance extractGenericType(CoreInstance instance, ProcessorSupport processorSupport)
    {
        GenericType classifierGenericType = instance instanceof Any ? ((Any) instance)._classifierGenericType() : null;
        return classifierGenericType == null ? Type.wrapGenericType(processorSupport.getClassifier(instance), processorSupport) : classifierGenericType;
    }
}
