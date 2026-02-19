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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingProperties;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.validator.RelationFunctionInstanceSetImplementationValidator;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;


public class RelationFunctionInstanceSetImplementationProcessor extends Processor<RelationFunctionInstanceSetImplementation>
{
    @Override
    public void process(RelationFunctionInstanceSetImplementation instance, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        CoreInstance relationFunction = ImportStub.withImportStubByPass(instance._relationFunctionCoreInstance(), processorSupport);
        PostProcessor.processElement(matcher, relationFunction, state, processorSupport);
        RelationFunctionInstanceSetImplementationValidator.validateRelationFunction(relationFunction, processorSupport);
        GenericType lastExpressionType = (GenericType) relationFunction.getValueForMetaPropertyToMany(M3Properties.expressionSequence).getLast().getValueForMetaPropertyToOne(M3Properties.genericType);
        RelationType<?> relationType = (RelationType<?>) Instance.getValueForMetaPropertyToOneResolved(lastExpressionType, M3Properties.typeArguments, M3Properties.rawType, processorSupport);
        processPropertyMapping(instance, relationType, processorSupport);
    }

    private void processPropertyMapping(RelationFunctionInstanceSetImplementation classMapping, RelationType<?> relationType, ProcessorSupport processorSupport)
    {
        for (PropertyMapping propertyMapping : classMapping._propertyMappings())
        {
            SourceInformation sourceInfo = propertyMapping.getSourceInformation();
            RelationFunctionPropertyMapping relationFunctionPropertyMapping = (RelationFunctionPropertyMapping) propertyMapping;
            Property<?, ?> property = relationFunctionPropertyMapping._property();

            // TODO: This is to workaround bugs with type inference. Using the property's type information when the RelationType/Column type is not inferred correctly from the mapping function.
            if (relationType != null)
            {
                Column<?, ?> column = (Column<?, ?>) _RelationType.findColumn(relationType, relationFunctionPropertyMapping._column()._name(), propertyMapping.getSourceInformation(), processorSupport);
                if (_Column.getColumnType(column)._rawType() == null)
                {
                    populateColumnFromProperty(relationFunctionPropertyMapping, property, sourceInfo, processorSupport);
                }
                else
                {
                    GenericType genericType = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericType(column._classifierGenericType(), processorSupport);
                    relationFunctionPropertyMapping._column()._classifierGenericType(genericType);
                }
            }
            else
            {
                populateColumnFromProperty(relationFunctionPropertyMapping, property, sourceInfo, processorSupport);
            }
        }
    }

    private void populateColumnFromProperty(RelationFunctionPropertyMapping propertyMapping, Property<?, ?> property, SourceInformation sourceInformation, ProcessorSupport processorSupport)
    {
        RelationType<?> newRelationType = _RelationType.build(Lists.mutable.with(_Column.getColumnInstance(propertyMapping._column()._name(), false, property._genericType(), property._multiplicity(), null, false, null, sourceInformation, processorSupport)), sourceInformation, processorSupport);
        propertyMapping._column(newRelationType._columns().toList().get(0));
    }

    @Override
    public void populateReferenceUsages(RelationFunctionInstanceSetImplementation instance, ModelRepository repository, ProcessorSupport processorSupport)
    {
        addReferenceUsageForToOneProperty(instance, instance._relationFunctionCoreInstance(), M2MappingProperties.relationFunction, repository, processorSupport);
    }

    @Override
    public String getClassName()
    {
        return M2MappingPaths.RelationFunctionInstanceSetImplementation;
    }
}
