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

package org.finos.legend.pure.m2.relational.serialization.grammar.v1.validator;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.validator.MappingValidator;
import org.finos.legend.pure.m2.relational.M2RelationalPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.EmbeddedRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalAssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElementWithJoin;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.JoinTreeNode;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class RelationalAssociationImplementationValidator implements MatchRunner<RelationalAssociationImplementation>
{
    @Override
    public String getClassName()
    {
        return M2RelationalPaths.RelationalAssociationImplementation;
    }

    @Override
    public void run(RelationalAssociationImplementation associationMapping, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();

        Mapping parentMapping = (Mapping)ImportStub.withImportStubByPass(associationMapping._parentCoreInstance(), processorSupport);
        MapIterable<String, SetImplementation> classMappingIndex = org.finos.legend.pure.m2.dsl.mapping.Mapping.getClassMappingsByIdIncludeEmbedded(parentMapping, processorSupport);

        RichIterable<? extends PropertyMapping> propertyMappings = associationMapping._propertyMappings();
        for (PropertyMapping propertyMapping : propertyMappings)
        {
            RelationalInstanceSetImplementation sourceSetImplementation = (RelationalInstanceSetImplementation)MappingValidator.validateId(associationMapping, propertyMapping, classMappingIndex, propertyMapping._sourceSetImplementationId(), "source", processorSupport);

            RelationalInstanceSetImplementation targetSetImplementation = (RelationalInstanceSetImplementation)MappingValidator.validateId(associationMapping, propertyMapping, classMappingIndex, propertyMapping._targetSetImplementationId(), "target", processorSupport);

            JoinTreeNode joinTreeNode = propertyMapping instanceof RelationalPropertyMapping && ((RelationalPropertyMapping)propertyMapping)._relationalOperationElement() instanceof RelationalOperationElementWithJoin ? ((RelationalOperationElementWithJoin)((RelationalPropertyMapping)propertyMapping)._relationalOperationElement())._joinTreeNode() : null;
            if (joinTreeNode == null)
            {
                throw new PureCompilationException(propertyMapping.getSourceInformation(), "Mapping Error: expected a join");
            }

            RelationalOperationElement sourceTable = findMainTable(sourceSetImplementation);
            RelationalOperationElement targetTable = findMainTable(targetSetImplementation);

            org.finos.legend.pure.m2.relational.serialization.grammar.v1.validator.JoinTreeNodeValidation.validateJoinTreeNode(joinTreeNode, sourceTable, targetTable, processorSupport);
        }
    }

    private RelationalOperationElement findMainTable(RelationalInstanceSetImplementation setImplementation)
    {
        TableAlias mainTableAlias = findMainTableAlias(setImplementation);
        return mainTableAlias._relationalElement();
    }

    private TableAlias findMainTableAlias(RelationalInstanceSetImplementation setImplementation)
    {
        if (setImplementation instanceof RootRelationalInstanceSetImplementation)
        {
            return ((RootRelationalInstanceSetImplementation)setImplementation)._mainTableAlias();
        }
        if (setImplementation instanceof EmbeddedRelationalInstanceSetImplementation)
        {
            RootRelationalInstanceSetImplementation owner = ((EmbeddedRelationalInstanceSetImplementation)setImplementation)._setMappingOwner();
            return findMainTableAlias(owner);
        }
        throw new RuntimeException("Unhandled set implementation type: " + PackageableElement.getUserPathForPackageableElement(setImplementation.getClassifier()));
    }
}
