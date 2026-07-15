// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.validator;

import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.EmbeddedRelationFunctionSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class RelationFunctionInstanceSetImplementationValidator implements MatchRunner<RelationFunctionInstanceSetImplementation>
{
    @Override
    public void run(RelationFunctionInstanceSetImplementation instance, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        validatePropertyMappings(instance._propertyMappings(), state.getProcessorSupport());
    }

    private void validatePropertyMappings(Iterable<? extends PropertyMapping> propertyMappings, ProcessorSupport processorSupport) throws PureCompilationException
    {
        for (PropertyMapping propertyMapping : propertyMappings)
        {
            if (propertyMapping instanceof RelationFunctionPropertyMapping)
            {
                RelationFunctionPropertyMapping rpm = (RelationFunctionPropertyMapping) propertyMapping;
                Property<?, ?> property = rpm._property();
                validateValueFn(rpm, property, rpm._transformer() instanceof EnumerationMapping, propertyMapping.getSourceInformation(), processorSupport);
            }
            else if (propertyMapping instanceof EmbeddedRelationFunctionSetImplementation)
            {
                validatePropertyMappings(((EmbeddedRelationFunctionSetImplementation) propertyMapping)._propertyMappings(), processorSupport);
            }
        }
    }

    private void validateValueFn(RelationFunctionPropertyMapping rpm, Property<?, ?> property, boolean hasEnumTransformer, SourceInformation sourceInformation, ProcessorSupport processorSupport)
    {
        LambdaFunction<?> valueFn = rpm._valueFn();
        if (valueFn == null)
        {
            throw new PureCompilationException(sourceInformation, "Relation mapping property '" + org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(property) + "' has no valueFn lambda.");
        }
        ValueSpecification last = valueFn._expressionSequence().toList().getLast();
        if (last == null)
        {
            throw new PureCompilationException(sourceInformation, "Relation mapping property '" + org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(property) + "' valueFn lambda has no expression body.");
        }

        String propertyName = org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(property);

        // Multiplicity compatibility — the valueFn's multiplicity range must be subsumed
        // by the property's. Both bounds must be concrete to compare; if either is
        // non-concrete (parameterised) we let downstream type-checking surface any issue.
        Multiplicity propertyMultiplicity = property._multiplicity();
        Multiplicity resultMultiplicity = last._multiplicity();
        if (org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isMultiplicityConcrete(propertyMultiplicity)
                && org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isMultiplicityConcrete(resultMultiplicity)
                && !org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.subsumes(propertyMultiplicity, resultMultiplicity))
        {
            throw new PureCompilationException(sourceInformation,
                    "Multiplicity Error: The property '" + propertyName
                            + "' has a multiplicity range of " + org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(propertyMultiplicity)
                            + " when the given expression has a multiplicity range of " + org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(resultMultiplicity));
        }

        // Type compatibility — when an EnumerationMapping transformer is present the
        // valueFn produces the transformer's source type (typically a primitive) and
        // the transformer bridges it to the property's enum type, so the direct
        // subtype check does not apply.
        if (!hasEnumTransformer)
        {
            GenericType propertyGenericType = property._genericType();
            GenericType resultGenericType = last._genericType();
            if (propertyGenericType != null
                    && resultGenericType != null
                    && propertyGenericType._rawType() != null
                    && resultGenericType._rawType() != null
                    && !org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericCompatibleWith(resultGenericType, propertyGenericType, processorSupport))
            {
                String exprTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(resultGenericType, false, processorSupport);
                String propertyTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(propertyGenericType, false, processorSupport);
                // Two distinct raw types can still print identically when their
                // simple names collide (e.g. two `Foo` classes from different
                // packages); fall back to fully-qualified printing to keep the
                // error meaningful in that case.
                if (exprTypeString.equals(propertyTypeString))
                {
                    exprTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(resultGenericType, true, processorSupport);
                    propertyTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(propertyGenericType, true, processorSupport);
                }
                throw new PureCompilationException(sourceInformation,
                        "Mismatching property and relation expression types. Property '" + propertyName
                                + "' is of type '" + propertyTypeString
                                + "', but the expression mapped to it is of type '" + exprTypeString + "'.");
            }
        }
    }

    public static void validateRelationFunction(CoreInstance relationFunction, ProcessorSupport processorSupport)
    {
        FunctionType functionType = (FunctionType) processorSupport.function_getFunctionType(relationFunction);
        if (functionType._parameters().size() != 0)
        {
            throw new PureCompilationException(relationFunction.getSourceInformation(), "Relation mapping function expecting arguments is not supported!");
        }
        if (!processorSupport.type_subTypeOf(functionType._returnType()._rawType(), processorSupport.package_getByUserPath(M3Paths.Relation)))
        {
            throw new PureCompilationException(relationFunction.getSourceInformation(), "Relation mapping function should return a Relation! Found a " + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(functionType._returnType(), processorSupport) + " instead.");
        }
    }

    @Override
    public String getClassName()
    {
        return M2MappingPaths.RelationFunctionInstanceSetImplementation;
    }
}

