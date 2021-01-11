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

package org.finos.legend.pure.m3.compiler.validation.validator;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class PropertyValidator implements MatchRunner<Property>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Property;
    }

    @Override
    public void run(Property instance, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        validateProperty(instance, state.getProcessorSupport());
    }

    public static void validateProperty(Property property, ProcessorSupport processorSupport) throws PureCompilationException
    {
        GenericTypeValidator.validateGenericType(property._genericType(), processorSupport);
        validateNonPrimitiveBinaryType(property, processorSupport);
    }

    public static void validateTypeRange(CoreInstance coreInstance, CoreInstance property, CoreInstance instance, ProcessorSupport processorSupport) throws PureCompilationException
    {
        CoreInstance propertyReturnGenericType = GenericType.resolvePropertyReturnType(Instance.extractGenericTypeFromInstance(coreInstance, processorSupport), property, processorSupport);
        CoreInstance instanceGenericType = Instance.extractGenericTypeFromInstance(instance, processorSupport);
        if (!GenericType.subTypeOf(instanceGenericType, propertyReturnGenericType, processorSupport))
        {
            throw new PureCompilationException(instance.getSourceInformation(), "Property: '" + org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(property) + "' / Type Error: '" + GenericType.print(instanceGenericType, processorSupport) + "' not a subtype of '" +  GenericType.print(propertyReturnGenericType, processorSupport) + "'");
        }
    }

    public static void validateMultiplicityRange(CoreInstance coreInstance, CoreInstance property, ListIterable<? extends CoreInstance> values, ProcessorSupport processorSupport) throws PureCompilationException
    {
        // Check Multiplicity Range
        CoreInstance multiplicity = org.finos.legend.pure.m3.navigation.property.Property.resolveInstancePropertyReturnMultiplicity(coreInstance, property, processorSupport);
        if (!Multiplicity.isValid(multiplicity, values.size()))
        {
            throw new PureCompilationException(coreInstance.getSourceInformation(), "Error instantiating the type '" + coreInstance.getClassifier().getName() + "'. The property '" + org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(property) + "' has a multiplicity range of " + Multiplicity.print(multiplicity) + " when the given list has a cardinality equal to '" + values.size() + "'");
        }
    }

    private static void validateNonPrimitiveBinaryType(Property property, ProcessorSupport processorSupport) throws PureCompilationException
    {
        CoreInstance type = ImportStub.withImportStubByPass(property._genericType()._rawTypeCoreInstance(), processorSupport);
        if (processorSupport.type_isPrimitiveType(type) && ModelRepository.BINARY_TYPE_NAME.equals(type.getName()))
        {
            throw new PureCompilationException(property.getSourceInformation(), "The property '" + org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(property) + "' has type of 'Binary'. 'Binary' type is not supported for property.");
        }
    }
}
