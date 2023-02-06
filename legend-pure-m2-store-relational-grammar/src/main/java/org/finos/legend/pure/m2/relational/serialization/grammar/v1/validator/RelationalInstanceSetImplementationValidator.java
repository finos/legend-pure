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
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.StringIterate;
import org.finos.legend.pure.m2.relational.M2RelationalPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.TreeNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.DataType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.EmbeddedRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.InlineEmbeddedRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.OtherwiseEmbeddedRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElementWithJoin;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.Join;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.JoinTreeNode;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class RelationalInstanceSetImplementationValidator implements MatchRunner<RootRelationalInstanceSetImplementation>
{
    @Override
    public String getClassName()
    {
        return M2RelationalPaths.RootRelationalInstanceSetImplementation;
    }

    @Override
    public void run(RootRelationalInstanceSetImplementation instance, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ValidatorState validatorState = (ValidatorState) state;
        ProcessorSupport processorSupport = validatorState.getProcessorSupport();

        RichIterable<? extends PropertyMapping> propertyMappings = instance._propertyMappings();

        this.validatePropertyMappings(instance, validatorState, null, propertyMappings, processorSupport);
    }

    private void validatePropertyMappings(RootRelationalInstanceSetImplementation mappingInstance, ValidatorState validatorState, Property<?, ?> embeddedProperty, RichIterable<? extends PropertyMapping> propertyMappings, ProcessorSupport processorSupport) throws PureCompilationException
    {
        MutableMap<String, PropertyMapping> properties = Maps.mutable.of();

        TableAlias tableAlias = mappingInstance._mainTableAlias();
        RelationalOperationElement startTable = (tableAlias == null) ? null : tableAlias._relationalElement();
        Mapping rootMapping = (Mapping) validatorState.getRootMapping();
        Mapping parentMapping = (Mapping) ImportStub.withImportStubByPass(mappingInstance._parentCoreInstance(), processorSupport);
        Mapping mappingForSearch = (rootMapping == null) ? parentMapping : rootMapping;

        for (PropertyMapping propertyMapping : propertyMappings)
        {
            Property<?, ?> property = (Property<?, ?>) ImportStub.withImportStubByPass(propertyMapping._propertyCoreInstance(), processorSupport);

            String targetId = propertyMapping._targetSetImplementationId();
            Type targetClass = (Type) ImportStub.withImportStubByPass(property._classifierGenericType()._typeArguments().toList().get(1)._rawTypeCoreInstance(), processorSupport);
            if (StringIterate.notEmpty(targetId) && targetClass instanceof DataType)
            {
                throw new PureCompilationException(propertyMapping.getSourceInformation(), "The property '" + property.getName() + "' returns a data type and thus should not have a targetId ('" + targetId + "')");
            }

            PropertyMapping existingPropertyMapping = properties.get(property.getName());
            if (existingPropertyMapping != null)
            {
                String existingTargetId = existingPropertyMapping._targetSetImplementationId();
                if (StringIterate.isEmpty(existingTargetId) || existingTargetId.equals(targetId))
                {
                    StringBuilder message = new StringBuilder("Duplicate mappings found for the property '").append(property.getName()).append("' (targetId: ").append(StringIterate.isEmpty(targetId) ? "?" : targetId).append(")");
                    if (embeddedProperty != null)
                    {
                        message.append(" in the embedded mapping for '").append(embeddedProperty.getName()).append("'");
                    }
                    message.append(" in the mapping for class ");
                    message.append(ImportStub.withImportStubByPass(mappingInstance._classCoreInstance(), processorSupport).getName());
                    message.append(", the property should have one mapping.");
                    throw new PureCompilationException(propertyMapping.getSourceInformation(), message.toString());
                }
            }
            properties.put(property.getName(), propertyMapping);

            this.validateEnumPropertyHasEnumMapping(property, propertyMapping, processorSupport);

            if (propertyMapping instanceof EmbeddedRelationalInstanceSetImplementation)
            {
                validatorState.validateSetImplementation(propertyMapping, true);
                RichIterable<? extends PropertyMapping> embeddedPropertyMappings = ((EmbeddedRelationalInstanceSetImplementation) propertyMapping)._propertyMappings();
                validatePropertyMappings(mappingInstance, validatorState, property, embeddedPropertyMappings, processorSupport);

                if (propertyMapping instanceof InlineEmbeddedRelationalInstanceSetImplementation)
                {
                    //get inlineSetId and check is not null
                    String inlineSetId = ((InlineEmbeddedRelationalInstanceSetImplementation) propertyMapping)._inlineSetImplementationId();
                    if (inlineSetId.isEmpty())
                    {
                        throw new PureCompilationException(propertyMapping.getSourceInformation(), "Invalid Inline mapping found: '" + property.getName() + "' mapping has not inline set defined, please use: " + property.getName() + "() Inline[setid].");
                    }
                    //check if exists
                    MapIterable<String, SetImplementation> classMappingIndex = org.finos.legend.pure.m2.dsl.mapping.Mapping.getClassMappingsByIdIncludeEmbedded(parentMapping, processorSupport);
                    SetImplementation inlineSetInstanceMapping = classMappingIndex.get(inlineSetId);
                    if (inlineSetInstanceMapping == null)
                    {
                        throw new PureCompilationException(propertyMapping.getSourceInformation(), "Invalid Inline mapping found: '" + property.getName() + "' property, inline set id " + inlineSetId + " does not exists.");
                    }
                    else
                    {
                        if (!org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(ImportStub.withImportStubByPass(inlineSetInstanceMapping._classCoreInstance(), processorSupport), targetClass, processorSupport))
                        {
                            throw new PureCompilationException(propertyMapping.getSourceInformation(), "Mapping Error! The inlineSetImplementationId '" + inlineSetId + "' is implementing the class '" + ((Class<?>) ImportStub.withImportStubByPass(inlineSetInstanceMapping._classCoreInstance(), processorSupport))._name() + "' which is not a subType of '" + targetClass._name() + "' (return type of the mapped property '" + property.getName() + "')");
                        }
                        //   startTable
                    }
                }
                if (propertyMapping instanceof OtherwiseEmbeddedRelationalInstanceSetImplementation)
                {
                    if (embeddedPropertyMappings.isEmpty())
                    {
                        throw new PureCompilationException(propertyMapping.getSourceInformation(), "Invalid Otherwise mapping found: '" + property.getName() + "' property has no embedded mappings defined, please use a property mapping with Join instead.");
                    }

                    PropertyMapping otherwiseTarget = ((OtherwiseEmbeddedRelationalInstanceSetImplementation) propertyMapping)._otherwisePropertyMapping();
                    String otherwiseTargetId = otherwiseTarget._targetSetImplementationId();
                    MapIterable<String, SetImplementation> classMappingIndex = org.finos.legend.pure.m2.dsl.mapping.Mapping.getClassMappingsByIdIncludeEmbedded(parentMapping, processorSupport);
                    SetImplementation targetInstanceMapping = classMappingIndex.get(otherwiseTargetId);
                    if (targetInstanceMapping == null)
                    {
                        throw new PureCompilationException(propertyMapping.getSourceInformation(), "Invalid Otherwise mapping found for '" + property.getName() + "' property, targetId " + otherwiseTargetId + " does not exists.");
                    }
                }
            }

            if (propertyMapping instanceof RelationalPropertyMapping)
            {
                RelationalOperationElement elem = ((RelationalPropertyMapping) propertyMapping)._relationalOperationElement();

                if (targetClass instanceof DataType)
                {
                    if (elem instanceof RelationalOperationElementWithJoin)
                    {
                        JoinTreeNode joinTreeNode = ((RelationalOperationElementWithJoin) elem)._joinTreeNode();
                        if (joinTreeNode != null)
                        {
                            JoinTreeNodeValidation.validateJoinTreeNode(joinTreeNode, startTable, processorSupport);
                        }
                        if (((RelationalOperationElementWithJoin) elem)._relationalOperationElement() == null)
                        {
                            throw new RuntimeException("Mapping error: The property '" + property.getName() + "' returns a data type. However it's mapped to a Join.");
                        }
                    }
                }
                else
                {
                    SetImplementation targetInstanceMapping = org.finos.legend.pure.m2.dsl.mapping.Mapping.getClassMappingById(mappingForSearch, targetId, processorSupport);

                    // Doesn't work because of embedded
//                    if (targetInstanceMapping == null)
//                    {
//                        throw new RuntimeException("Mapping error: The target instance mapping '"+targetId+"' for the property '"+property.getName()+"' can't be found in the mapping "+mappingForSearch.getName()+".");
//                    }

                    if (targetInstanceMapping != null && !org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(ImportStub.withImportStubByPass(targetInstanceMapping._classCoreInstance(), processorSupport), targetClass, processorSupport))
                    {
                        throw new PureCompilationException(propertyMapping.getSourceInformation(), "Mapping Error! The setImplementationId '" + targetId + "' is implementing the class '" + ((Class<?>) ImportStub.withImportStubByPass(targetInstanceMapping._classCoreInstance(), processorSupport))._name() + "' which is not a subType of '" + targetClass._name() + "' (return type of the mapped property '" + property.getName() + "'");
                    }

                    JoinTreeNode joinTreeNode = elem instanceof RelationalOperationElementWithJoin ? ((RelationalOperationElementWithJoin) elem)._joinTreeNode() : null;
                    if (joinTreeNode == null)
                    {
                        throw new PureCompilationException(propertyMapping.getSourceInformation(), "Mapping Error! The target type:'" + targetClass + "' is not a data type but the relationalOperation is not a join");
                    }
                    if (((RelationalOperationElementWithJoin) elem)._relationalOperationElement() != null)
                    {
                        throw new RuntimeException("Mapping error: The property '" + property.getName() + "' doesn't return a data type. However it's mapped to a column or a function.");
                    }

                    // TODO targetInstanceMapping should never be null (see above TODO)
                    if (targetInstanceMapping == null)
                    {
                        JoinTreeNodeValidation.validateJoinTreeNode(joinTreeNode, startTable, processorSupport);
                    }
                    else if (!(targetInstanceMapping instanceof RootRelationalInstanceSetImplementation))
                    {
                        // TODO should we throw an exception here?
                    }
                    else
                    {
                        RelationalOperationElement targetMainTable = ((RootRelationalInstanceSetImplementation) targetInstanceMapping)._mainTableAlias()._relationalElement();
                        JoinTreeNodeValidation.validateJoinTreeNode(joinTreeNode, startTable, targetMainTable, processorSupport);
                    }
                }
            }
        }
    }

    private void validateEnumPropertyHasEnumMapping(Property<?, ?> property, PropertyMapping propertyMapping, ProcessorSupport processorSupport) throws PureCompilationException
    {
        Type type = (Type) ImportStub.withImportStubByPass(property._genericType()._rawTypeCoreInstance(), processorSupport);

        if (type instanceof Enumeration)
        {
            if (!(propertyMapping instanceof RelationalPropertyMapping) || ((RelationalPropertyMapping) propertyMapping)._transformerCoreInstance() == null)
            {
                throw new PureCompilationException(propertyMapping.getSourceInformation(), "Missing an EnumerationMapping for the enum property '" + property.getName() + "'. Enum properties require an EnumerationMapping in order to transform the store values into the Enum.");
            }
        }
    }

    private static RelationalOperationElement followJoinTreeNode(JoinTreeNode joinTreeNode, RelationalOperationElement startTable)
    {
        RelationalOperationElement newStartTable = followJoin(joinTreeNode._join(), startTable);
        if (newStartTable == null)
        {
            return null;
        }

        ListIterable<? extends TreeNode> childrenData = joinTreeNode._childrenData().toList();
        // Is this logic correct? What if there are multiple children?
        return childrenData.isEmpty() ? newStartTable : followJoinTreeNode((JoinTreeNode) childrenData.get(0), newStartTable);
    }

    private static RelationalOperationElement followJoin(Join join, RelationalOperationElement start)
    {
        for (Pair<?, ?> pair : join._aliases())
        {
            if (start == ((TableAlias) pair._first())._relationalElement())
            {
                return ((TableAlias) pair._second())._relationalElement();
            }
        }
        return null;
    }

    private static boolean joinTreeNodeHasSourceTable(JoinTreeNode joinTreeNode, RelationalOperationElement table)
    {
        return joinHasSourceTable(joinTreeNode._join(), table);
    }

    private static boolean joinHasSourceTable(Join join, RelationalOperationElement table)
    {
        for (Pair<?, ?> pair : join._aliases())
        {
            if (table == ((TableAlias) pair._first())._relationalElement())
            {
                return true;
            }
        }
        return false;
    }
}
