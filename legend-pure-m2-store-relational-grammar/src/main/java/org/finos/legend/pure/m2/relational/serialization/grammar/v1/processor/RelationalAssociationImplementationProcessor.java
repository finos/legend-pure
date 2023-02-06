// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.m2.relational.serialization.grammar.v1.processor;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.map.MapIterable;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.validator.MappingValidator;
import org.finos.legend.pure.m2.relational.M2RelationalPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.EmbeddedRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalAssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.JoinTreeNode;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;

public class RelationalAssociationImplementationProcessor extends Processor<RelationalAssociationImplementation>
{
    private static final Function<PropertyMapping, Store> STORE = new Function<PropertyMapping, Store>()
    {
        @Override
        public Store valueOf(PropertyMapping propertyMapping)
        {
            return propertyMapping._store();
        }
    };

    @Override
    public String getClassName()
    {
        return M2RelationalPaths.RelationalAssociationImplementation;
    }

    @Override
    public void process(RelationalAssociationImplementation associationMapping, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        Mapping parentMapping = (Mapping)ImportStub.withImportStubByPass(associationMapping._parentCoreInstance(), processorSupport);
        MapIterable<String, SetImplementation> classMappingIndex = org.finos.legend.pure.m2.dsl.mapping.Mapping.getClassMappingsByIdIncludeEmbedded(parentMapping, processorSupport);

        RichIterable<? extends PropertyMapping> propertyMappings = associationMapping._propertyMappings();
        for (PropertyMapping propertyMapping : propertyMappings)
        {
            RelationalPropertyMappingProcessor.processRelationalPropertyMapping(propertyMapping, matcher, state, repository, "", processorSupport, associationMapping, associationMapping);

            RichIterable<JoinTreeNode> joinTreeNodes = RelationalPropertyMappingProcessor.collectJoinTreeNodes(propertyMapping);
            if (joinTreeNodes.notEmpty())
            {
                CoreInstance sourceSetImplementation = MappingValidator.validateId(associationMapping, propertyMapping, classMappingIndex, propertyMapping._sourceSetImplementationId(), "source", processorSupport);

                TableAlias mainTableAlias = findMainTableAlias(sourceSetImplementation, matcher, state, processorSupport);
                RelationalOperationElement mainTable = mainTableAlias != null ? mainTableAlias._relationalElement() : null;
                for (JoinTreeNode joinTreeNode : joinTreeNodes)
                {
                    RelationalOperationElementProcessor.processAliasForJoinTreeNode(joinTreeNode, mainTable, processorSupport);
                }
            }
        }

        associationMapping._stores(associationMapping._propertyMappings().collect(STORE).toSet().without(null));
    }

    @Override
    public void populateReferenceUsages(RelationalAssociationImplementation associationMapping, ModelRepository repository, ProcessorSupport processorSupport)
    {
        RichIterable<? extends PropertyMapping> propertyMappings = associationMapping._propertyMappings();
        RelationalPropertyMappingProcessor.populateReferenceUsagesForRelationalPropertyMappings(propertyMappings, repository, processorSupport);
    }

    private TableAlias findMainTableAlias(CoreInstance setImplementation, Matcher matcher, ProcessorState state, ProcessorSupport processorSupport)
    {
        PostProcessor.processElement(matcher, setImplementation, state, processorSupport);
        if (setImplementation instanceof RootRelationalInstanceSetImplementation)
        {
            return ((RootRelationalInstanceSetImplementation)setImplementation)._mainTableAlias();
        }
        if (setImplementation instanceof EmbeddedRelationalInstanceSetImplementation)
        {
            RootRelationalInstanceSetImplementation owner = ((EmbeddedRelationalInstanceSetImplementation)setImplementation)._setMappingOwner();
            return findMainTableAlias(owner, matcher, state, processorSupport);
        }
        throw new RuntimeException("Unhandled set implementation type: " + PackageableElement.getUserPathForPackageableElement(setImplementation.getClassifier()));
    }
}
