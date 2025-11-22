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
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.relation._Column;
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
        for (PropertyMapping propertyMapping : instance._propertyMappings())
        {
            RelationFunctionPropertyMapping relationFunctionPropertyMapping = (RelationFunctionPropertyMapping) propertyMapping;
            Property<?, ?> property = relationFunctionPropertyMapping._property();
            Column<?, ?> column = relationFunctionPropertyMapping._column();
            validateProperty(property, column, propertyMapping.getSourceInformation(), state.getProcessorSupport());
        }
    }

    private void validateProperty(Property<?, ?> property, Column<?, ?> column, SourceInformation sourceInformation, ProcessorSupport processorSupport)
    {
        Multiplicity propertyMultiplicity = property._multiplicity();
        if (!org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isToOne(propertyMultiplicity) && !org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isZeroToOne(propertyMultiplicity))
        {
            throw new PureCompilationException(sourceInformation, "Properties in relation mappings can only have multiplicity 1 or 0..1, but the property '" + org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(property) + "' has multiplicity " + org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(propertyMultiplicity) + ".");
        }

        Type propertyType = property._genericType()._rawType();
        if (!processorSupport.type_isPrimitiveType(propertyType))
        {
            throw new PureCompilationException(sourceInformation, "Relation mapping is only supported for primitive properties, but the property '" + org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(property) + "' has type " + propertyType._name() + ".");
        }

        Type columnType = _Column.getColumnType(column)._rawType();
        if (!processorSupport.type_subTypeOf(columnType, propertyType))
        {
            throw new PureCompilationException(sourceInformation, "Mismatching property and relation column types. Property type is " + propertyType._name() + ", but relation column it is mapped to has type " + columnType._name() + ".");
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
