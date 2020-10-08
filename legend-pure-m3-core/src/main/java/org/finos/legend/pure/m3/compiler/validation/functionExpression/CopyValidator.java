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

package org.finos.legend.pure.m3.compiler.validation.functionExpression;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class CopyValidator
{
    public static void validateCopy(Matcher matcher, ValidatorState state, FunctionExpression expression, ProcessorSupport processorSupport) throws PureCompilationException
    {
        validateProperties(matcher, state, expression, expression._parametersValues().getFirst()._genericType(), processorSupport);
    }

    private static void validateProperties(Matcher matcher, ValidatorState state, FunctionExpression expression, GenericType genericType, ProcessorSupport processorSupport)
    {
        Type classifier = (Type)ImportStub.withImportStubByPass(genericType._rawTypeCoreInstance(),  processorSupport);
        ListIterable<? extends ValueSpecification> parametersValues = expression._parametersValues().toList();
        if (parametersValues.size() > 2)
        {
            if (parametersValues.get(2) instanceof InstanceValue)
            {
                for (CoreInstance keyValue : ((InstanceValue)parametersValues.get(2))._valuesCoreInstance())
                {
                    if (keyValue instanceof KeyExpression)
                    {
                        // Validate key is an actual property
                        GenericType previousGenericType = genericType;
                        GenericType currentGenericType = null;
                        Type currentClassifier = classifier;
                        AbstractProperty property = null;
                        RichIterable<? extends CoreInstance> keys = ((KeyExpression)keyValue)._key()._valuesCoreInstance();
                        for (CoreInstance key : keys)
                        {
                            property = (AbstractProperty)processorSupport.class_findPropertyUsingGeneralization(currentClassifier, key.getName());
                            if (property == null)
                            {
                                throw new PureCompilationException(((KeyExpression)keyValue)._key().getSourceInformation(), "The property '" + key.getName() + "' can't be found in the type '" + classifier.getName() + "' or in its hierarchy.");
                            }
                            if (currentGenericType != null)
                            {
                                previousGenericType = currentGenericType;
                            }
                            currentGenericType = property._genericType();
                            currentClassifier = (Type)ImportStub.withImportStubByPass(currentGenericType._rawTypeCoreInstance(), processorSupport);
                        }

                        NewValidator.validatePropertyValue(matcher, state, expression, (KeyExpression)keyValue, previousGenericType, property, processorSupport);
                    }
                }
            }
        }
    }
}
