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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingProperties;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.validator.MappingValidator;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.validator.RelationFunctionInstanceSetImplementationValidator;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.ReferenceUsage;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EmbeddedSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.InstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.OperationSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelJoin.ModelJoinPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.EmbeddedRelationFunctionSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.tools.GrammarInfoStub;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;


public class RelationFunctionInstanceSetImplementationProcessor extends Processor<RelationFunctionInstanceSetImplementation>
{
    private static final String RELATION_FUNCTIONS_PACKAGE = "meta::pure::functions::relation";
    private static final String JOIN_KIND = RELATION_FUNCTIONS_PACKAGE + "::JoinKind";

    @Override
    public void process(RelationFunctionInstanceSetImplementation instance, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        // The relationFunction slot may hold either an ImportStub (for `~func`)
        // or an anonymous LambdaFunction (for `~src`).  withImportStubByPass
        // returns the underlying instance for both shapes.
        CoreInstance relationFunction = ImportStub.withImportStubByPass(instance._relationFunctionCoreInstance(), processorSupport);
        PostProcessor.processElement(matcher, relationFunction, state, processorSupport);
        RelationFunctionInstanceSetImplementationValidator.validateRelationFunction(relationFunction, processorSupport);

        // The relation function's last expression has generic type Relation<T>
        // where T is the structural row type (a RelationType<...>).  We bind
        // the valueFn `row` parameter to T so `$row.<col>` resolves against
        // the columns of the row, mirroring how `extend`/`filter` lambdas
        // type their row parameter.
        GenericType lastExprType = (GenericType) relationFunction
                .getValueForMetaPropertyToMany(M3Properties.expressionSequence)
                .getLast()
                .getValueForMetaPropertyToOne(M3Properties.genericType);
        org.eclipse.collections.api.list.ListIterable<? extends GenericType> typeArgs = lastExprType._typeArguments().toList();
        if (typeArgs.isEmpty())
        {
            throw new org.finos.legend.pure.m4.exception.PureCompilationException(instance.getSourceInformation(), "Relation function's last expression has no Relation type argument; cannot bind property mapping `$row` parameter.");
        }
        GenericType rowType = typeArgs.getFirst();
        resolvePrimaryKey(instance, relationFunction, rowType, processorSupport);

        processPropertyMappings(instance._propertyMappings(), instance, rowType, lastExprType, matcher, state, repository, processorSupport);
    }

    private static void resolvePrimaryKey(RelationFunctionInstanceSetImplementation instance, CoreInstance relationFunction, GenericType rowType, ProcessorSupport processorSupport)
    {
        if (instance._primaryKey().isEmpty())
        {
            return;
        }
        CoreInstance rawType = ImportStub.withImportStubByPass(rowType._rawTypeCoreInstance(), processorSupport);
        if (!(rawType instanceof RelationType))
        {
            String functionName = relationFunction instanceof ConcreteFunctionDefinition ? ((ConcreteFunctionDefinition<?>) relationFunction)._functionName() : "~src";
            throw new PureCompilationException(instance.getSourceInformation(), "Cannot resolve primary key columns: relation function '" + functionName + "' does not return a concrete RelationType. Ensure the function body produces a typed relation (e.g., #>{db.table}#).");
        }
        RichIterable<? extends Column<?, ?>> relationColumns = ((RelationType<?>) rawType)._columns();
        MutableList<Column<?, ?>> resolved = Lists.mutable.empty();
        for (Column<?, ?> placeholder : instance._primaryKey())
        {
            Column<?, ?> column = relationColumns.detect(c -> placeholder._name().equals(c._name()));
            if (column == null)
            {
                String mappingPath = PackageableElement.getUserPathForPackageableElement(ImportStub.withImportStubByPass(instance._parentCoreInstance(), processorSupport));
                throw new PureCompilationException(placeholder.getSourceInformation(), "Primary key column '" + placeholder._name() + "' declared in class mapping '" + instance._id() + "' (mapping '" + mappingPath + "') is not part of the columns returned by the relation function."
                        + " Available columns: [" + relationColumns.collect(Column::_name).makeString(", ") + "]."
                        + " Use `~primaryKey: [col1, col2, ...]` referencing only columns from the relation function's output.");
            }
            GenericType columnType = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericType(_Column.getColumnType(column), processorSupport);
            Column<?, ?> primaryKeyColumn = _Column.getColumnInstance(column._name(), false, columnType, _Column.getColumnMultiplicity(column), placeholder.getSourceInformation(), processorSupport);
            GenericType anySource = (GenericType) processorSupport.newAnonymousCoreInstance(placeholder.getSourceInformation(), M3Paths.GenericType);
            anySource._rawTypeCoreInstance(processorSupport.package_getByUserPath(M3Paths.Any));
            resolved.add(_Column.updateSource(primaryKeyColumn, anySource));
        }
        instance._primaryKey(resolved);
    }

    private void processPropertyMappings(Iterable<? extends PropertyMapping> propertyMappings, RelationFunctionInstanceSetImplementation owner, GenericType rowType, GenericType relationType, Matcher matcher, ProcessorState state, ModelRepository repository, ProcessorSupport processorSupport)
    {
        MutableList<PropertyMapping> resolvedMappings = Lists.mutable.withAll(propertyMappings);
        int offset = 0;
        for (PropertyMapping propertyMapping : propertyMappings)
        {
            if (propertyMapping instanceof RelationFunctionPropertyMapping)
            {
                RelationFunctionPropertyMapping relationPm = (RelationFunctionPropertyMapping) propertyMapping;
                CoreInstance condition = inlineModelJoinCondition(relationPm._valueFn(), relationPm.getSourceInformation(), processorSupport);
                if (condition == null)
                {
                    processRelationFunctionPropertyMapping(relationPm, rowType, relationType, matcher, state, processorSupport);
                }
                else
                {
                    if (relationPm._transformerCoreInstance() != null)
                    {
                        throw new PureCompilationException(relationPm.getSourceInformation(), "modelJoin() cannot be combined with an EnumerationMapping transformer.");
                    }
                    ModelJoinPropertyMapping modelJoin = (ModelJoinPropertyMapping) processorSupport.newAnonymousCoreInstance(relationPm.getSourceInformation(), "meta::pure::mapping::modelJoin::ModelJoinPropertyMapping");
                    modelJoin._propertyCoreInstance(relationPm._propertyCoreInstance())._owner(owner)
                            ._sourceSetImplementationId(relationPm._sourceSetImplementationId())
                            ._localMappingProperty(relationPm._localMappingProperty())
                            ._localMappingPropertyTypeCoreInstance(relationPm._localMappingPropertyTypeCoreInstance())
                            ._localMappingPropertyMultiplicity(relationPm._localMappingPropertyMultiplicity());
                    modelJoin._joinCondition((LambdaFunction<?>) condition);
                    // The generic set processor may already have recorded a reference from the property
                    // to the original mapping. Replace it so the discarded valueFn wrapper is not retained.
                    ReferenceUsage.removeReferenceUsagesForUser(ImportStub.withImportStubByPass(relationPm._propertyCoreInstance(), processorSupport), relationPm, processorSupport);
                    PropertyMappingProcessor.populateReferenceUsagesForPropertyMapping(modelJoin, repository, processorSupport);
                    resolvedMappings.set(offset, modelJoin);
                    owner._propertyMappings(resolvedMappings);
                    processInlineModelJoin(modelJoin, owner, offset, matcher, state, processorSupport);
                }
            }
            else if (propertyMapping instanceof ModelJoinPropertyMapping)
            {
                processInlineModelJoin((ModelJoinPropertyMapping) propertyMapping, owner, offset, matcher, state, processorSupport);
            }
            else if (propertyMapping instanceof EmbeddedRelationFunctionSetImplementation)
            {
                EmbeddedRelationFunctionSetImplementation embeddedSet = (EmbeddedRelationFunctionSetImplementation) propertyMapping;
                Property<?, ?> property = (Property<?, ?>) ImportStub.withImportStubByPass(embeddedSet._propertyCoreInstance(), processorSupport);
                CoreInstance targetClass = property._classifierGenericType() == null ||
                        property._classifierGenericType()._typeArguments() == null ||
                        property._classifierGenericType()._typeArguments().size() < 2 ||
                        property._classifierGenericType()._typeArguments().toList().get(1) == null ? null : ImportStub.withImportStubByPass(property._classifierGenericType()._typeArguments().toList().get(1)._rawTypeCoreInstance(), processorSupport);
                embeddedSet._classCoreInstance(targetClass);
                embeddedSet._relationFunctionCoreInstance(owner._relationFunctionCoreInstance());

                processPropertyMappings(embeddedSet._propertyMappings(), embeddedSet, rowType, relationType, matcher, state, repository, processorSupport);
            }
            offset++;
        }
    }

    /**
     * An inline {@code modelJoin({src, tgt | <condition>})} is a one-sided ModelJoin: the condition is
     * model-level, over the owning class and the property's class, exactly like an association ModelJoin's.
     * The params are positional (the first is the owning class, the second the property's class) and may
     * optionally declare their types. The target is the single root class mapping of the property's class,
     * whatever its store.
     */
    private void processInlineModelJoin(ModelJoinPropertyMapping pm, RelationFunctionInstanceSetImplementation owner, int offset, Matcher matcher, ProcessorState state, ProcessorSupport processorSupport)
    {
        Property<?, ?> property = (Property<?, ?>) ImportStub.withImportStubByPass(pm._propertyCoreInstance(), processorSupport);
        String propertyName = property._name();
        CoreInstance targetClass = ImportStub.withImportStubByPass(property._genericType()._rawTypeCoreInstance(), processorSupport);
        if (!(targetClass instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class))
        {
            throw new PureCompilationException(pm.getSourceInformation(), "modelJoin() can only map a property whose type is a class; property '"
                    + propertyName + "' is of type '" + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(property._genericType(), processorSupport) + "'.");
        }
        CoreInstance sourceClass = ImportStub.withImportStubByPass(owner._classCoreInstance(), processorSupport);

        LambdaFunction<?> condition = pm._joinCondition();
        ListIterable<? extends VariableExpression> conditionParams = getFunctionType(condition, processorSupport)._parameters().toList();
        if (conditionParams.size() != 2)
        {
            throw new PureCompilationException(condition.getSourceInformation(), "modelJoin()'s join condition must take exactly 2 parameters (a " + sourceClass.getName()
                    + " and a " + targetClass.getName() + "), found " + conditionParams.size() + ".");
        }
        String sourceParamName = conditionParams.get(0)._name();
        String targetParamName = conditionParams.get(1)._name();
        if (sourceParamName.equals(targetParamName))
        {
            throw new PureCompilationException(condition.getSourceInformation(), "modelJoin()'s join condition parameters share the same name '" + sourceParamName + "'; rename one of them.");
        }
        GenericType declaredSourceType = validateDeclaredParamType(conditionParams.get(0), "first", sourceClass, "owning class", processorSupport);
        GenericType declaredTargetType = validateDeclaredParamType(conditionParams.get(1), "second", targetClass, "class of property '" + propertyName + "'", processorSupport);
        rejectRelationVariables(condition);

        InstanceSetImplementation target = resolveInlineModelJoinTarget(pm, targetClass, owner._parent(), processorSupport);
        // The target's MappingClass (its local `+` properties) must exist before its param is typed to it.
        PostProcessor.processElement(matcher, target, state, processorSupport);

        Multiplicity pureOne = (Multiplicity) processorSupport.package_getByUserPath(M3Paths.PureOne);
        conditionParams.forEach(p -> p._multiplicity(pureOne));
        ModelJoinProcessor.typeJoinConditionParams(condition, sourceParamName, targetParamName, owner, target, processorSupport);
        keepDeclaredParamType(conditionParams.get(0), declaredSourceType, sourceClass, processorSupport);
        keepDeclaredParamType(conditionParams.get(1), declaredTargetType, targetClass, processorSupport);
        pm._sourceSetImplementationId(owner._id());
        pm._targetSetImplementationId(target._id());
        ModelJoinProcessor.compileJoinCondition(pm, offset, state, matcher, processorSupport);

        CoreInstance selfReference = ModelJoinShared.findFirst(condition, Sets.immutable.empty(), (node, boundNames) -> node instanceof FunctionExpression && ((FunctionExpression) node)._funcCoreInstance() == property);
        if (selfReference != null)
        {
            throw new PureCompilationException(selfReference.getSourceInformation(), "modelJoin()'s join condition cannot use the property '" + propertyName + "' that it maps.");
        }
    }

    /**
     * If the property's value is a {@code modelJoin(<condition>)} call, returns its condition lambda;
     * otherwise null. {@code modelJoin} is only meaningful as the entire value, so a call nested anywhere
     * else, or a relation-level {@code $src->modelJoin(...)} or {@code $row->modelJoin(...)} form, is rejected here.
     */
    private CoreInstance inlineModelJoinCondition(LambdaFunction<?> valueFn, SourceInformation sourceInfo, ProcessorSupport ps)
    {
        ListIterable<? extends CoreInstance> body = valueFn.getValueForMetaPropertyToMany(M3Properties.expressionSequence);
        CoreInstance topLevel = body.size() == 1 && isModelJoinCall(body.get(0), ps) ? body.get(0) : null;
        for (CoreInstance expression : body)
        {
            CoreInstance nested = ModelJoinShared.findFirst(expression, Sets.immutable.empty(), (node, boundNames) -> node != topLevel && isModelJoinCall(node, ps));
            if (nested != null)
            {
                throw new PureCompilationException(sourceInfoOr(nested, sourceInfo), "modelJoin() must be the entire value of a property mapping; it cannot be used inside another expression.");
            }
        }
        if (topLevel == null)
        {
            return null;
        }

        ListIterable<? extends CoreInstance> arguments = topLevel.getValueForMetaPropertyToMany(M3Properties.parametersValues);
        // $src (the relation) and $row (the mapped row) are relation-level; the join is model-level.
        String relationVariable = arguments.isEmpty() ? null : variableName(arguments.get(0), ps);
        if ("src".equals(relationVariable) || "row".equals(relationVariable))
        {
            throw new PureCompilationException(sourceInfoOr(topLevel, sourceInfo), "modelJoin() takes only a join condition between the owning class and the property's class, "
                    + "e.g. modelJoin({p, f | $p.firmId == $f.id}); it does not take $" + relationVariable + ".");
        }
        CoreInstance condition = arguments.size() == 1 ? lambdaArgument(arguments.get(0), ps) : null;
        if (condition == null)
        {
            throw new PureCompilationException(sourceInfoOr(topLevel, sourceInfo), "modelJoin() expects exactly one argument: a 2-parameter join condition lambda between the owning class and the property's class, "
                    + "e.g. modelJoin({p, f | $p.firmId == $f.id}).");
        }
        return condition;
    }

    private static boolean isModelJoinCall(CoreInstance node, ProcessorSupport ps)
    {
        if (!ps.instance_instanceOf(node, M3Paths.SimpleFunctionExpression))
        {
            return false;
        }
        CoreInstance functionName = node.getValueForMetaPropertyToOne(M3Properties.functionName);
        if (functionName == null || !"modelJoin".equals(ModelJoinShared.simpleFunctionName(functionName.getName())))
        {
            return false;
        }
        // Every function in this compile batch is registered now. Leave user calls intact
        // so ordinary function matching handles their arguments and overloads.
        CoreInstance importGroup = node.getValueForMetaPropertyToOne(M3Properties.importGroup);
        return ModelJoinShared.isCallToPlatformFunction(functionName.getName(), importGroup, "meta::pure::functions::relation", ps);
    }

    // The name of the variable that `node` references, or null if it isn't a variable reference.
    private static String variableName(CoreInstance node, ProcessorSupport ps)
    {
        if (!ps.instance_instanceOf(node, M3Paths.VariableExpression))
        {
            return null;
        }
        CoreInstance variableName = node.getValueForMetaPropertyToOne(M3Properties.name);
        return variableName == null ? null : variableName.getName();
    }

    private static CoreInstance lambdaArgument(CoreInstance argument, ProcessorSupport ps)
    {
        if (ps.instance_instanceOf(argument, M3Paths.LambdaFunction))
        {
            return argument;
        }
        if (ps.instance_instanceOf(argument, M3Paths.InstanceValue))
        {
            ListIterable<? extends CoreInstance> values = argument.getValueForMetaPropertyToMany(M3Properties.values);
            if (values.size() == 1 && ps.instance_instanceOf(values.get(0), M3Paths.LambdaFunction))
            {
                return values.get(0);
            }
        }
        return null;
    }

    private static SourceInformation sourceInfoOr(CoreInstance node, SourceInformation fallback)
    {
        return node.getSourceInformation() == null ? fallback : node.getSourceInformation();
    }

    // Returns the param's declared type, or null if it is left to inference. After an unbind the param carries its
    // positional _mj_src/_mj_tgt name rather than the user's, so it is then named by position instead.
    private static GenericType validateDeclaredParamType(VariableExpression param, String position, CoreInstance expectedClass, String description, ProcessorSupport processorSupport)
    {
        GenericType declaredType = param._genericType();
        if (declaredType != null)
        {
            CoreInstance declared = ImportStub.withImportStubByPass(declaredType._rawTypeCoreInstance(), processorSupport);
            if (declared != expectedClass && !org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(expectedClass, declared, processorSupport))
            {
                String paramDescription = (ModelJoinProcessor.MJ_SOURCE_VAR.equals(param._name()) || ModelJoinProcessor.MJ_TARGET_VAR.equals(param._name()))
                        ? "'s " + position + " parameter"
                        : " parameter '" + param._name() + "'";
                throw new PureCompilationException(param.getSourceInformation(), "modelJoin()'s join condition" + paramDescription + " is declared as '"
                        + PackageableElement.getUserPathForPackageableElement(declared) + "', but the " + description + " is '"
                        + PackageableElement.getUserPathForPackageableElement(expectedClass) + "'.");
            }
        }
        if (param._multiplicity() != null && !org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isToOne(param._multiplicity(), false))
        {
            throw new PureCompilationException(param.getSourceInformation(), "modelJoin()'s join condition parameter '" + param._name() + "' must have multiplicity [1], found "
                    + org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(param._multiplicity()) + ".");
        }
        return declaredType;
    }

    /**
     * typeJoinConditionParams types each param to its set's MappingClass (or class), whatever was declared, so a
     * declared supertype is only checked, never narrows the param. An inferred type is marked as such, so that
     * unbind strips it as for any lambda. A type declared as exactly the expected class must survive unbind, so
     * that a recompile re-validates it: RelationFunctionInstanceSetImplementationUnbind restores the class from the
     * param's (MappingClass) type. A declared supertype can't be recovered from that type, so it is treated as
     * inferred and dropped on unbind, rather than restored as a narrower declaration than the user wrote.
     */
    private static void keepDeclaredParamType(VariableExpression param, GenericType declaredType, CoreInstance expectedClass, ProcessorSupport processorSupport)
    {
        if (declaredType == null || ImportStub.withImportStubByPass(declaredType._rawTypeCoreInstance(), processorSupport) != expectedClass)
        {
            param._genericType((GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericTypeAsInferredGenericType(param._genericType(), param.getSourceInformation(), processorSupport));
        }
    }

    // The condition is compiled in its own variable scope, so $row/$src would otherwise only fail as "unknown".
    // The condition's own params, and any nested lambda's, are bound where they apply, so a legitimately-scoped
    // inner {row | ...} or {src | ...} isn't mistaken for the forbidden relation-level $row/$src.
    private static void rejectRelationVariables(LambdaFunction<?> condition)
    {
        VariableExpression relationVariable = findFreeVariable(condition, Sets.immutable.with("row", "src"));
        if (relationVariable != null)
        {
            throw new PureCompilationException(relationVariable.getSourceInformation(), "modelJoin()'s join condition is over the two classes, not the relation's rows: $"
                    + relationVariable._name() + " is not available. Use the condition's parameters, e.g. {p, f | $p.firmId == $f.id}.");
        }
    }

    // The first reference in `expression` to a variable named in `names` that no lambda within `expression` binds.
    private static VariableExpression findFreeVariable(CoreInstance expression, SetIterable<String> names)
    {
        return (VariableExpression) ModelJoinShared.findFirst(expression, Sets.immutable.empty(), (node, boundNames) ->
                node instanceof VariableExpression && names.contains(((VariableExpression) node)._name()) && !boundNames.contains(((VariableExpression) node)._name()));
    }

    private static InstanceSetImplementation resolveInlineModelJoinTarget(ModelJoinPropertyMapping pm, CoreInstance targetClass, Mapping mapping, ProcessorSupport processorSupport)
    {
        MutableList<SetImplementation> candidates = collectEffectiveRootClassMappings(targetClass, mapping, processorSupport);
        if (candidates.size() != 1)
        {
            throw new PureCompilationException(pm.getSourceInformation(), "modelJoin: expected exactly one root class mapping for type '"
                    + PackageableElement.getUserPathForPackageableElement(targetClass) + "' in this mapping or its includes, found " + candidates.size()
                    + (candidates.isEmpty() ? "." : ": " + candidates.collect(SetImplementation::_id).makeString("[", ", ", "]") + "."));
        }
        SetImplementation root = candidates.getOnly();
        if (root instanceof OperationSetImplementation)
        {
            throw new PureCompilationException(pm.getSourceInformation(), "modelJoin: Operation mappings are not supported as the target of modelJoin; the root class mapping for type '"
                    + PackageableElement.getUserPathForPackageableElement(targetClass) + "' ('" + root._id() + "') is an Operation.");
        }
        return (InstanceSetImplementation) root;
    }

    /**
     * Collects, from {@code mapping} and its includes, every exact (non-embedded) class mapping for
     * {@code targetClass} that is -- or will become -- a root, Operation mappings included, so that a starred
     * Operation is the root rather than an unstarred instance class mapping beside it being picked instead.
     * Within its declaring mapping, a class's sole class mapping is its root even without '*', and otherwise the
     * starred one is. MappingValidator.validateStar's own promotion runs later, in the Validator phase
     * (PostProcessor.process runs before Validator.validateM3 -- see IncrementalCompiler#finishRepoCompilation),
     * so the rule (MappingValidator.getEffectiveRoots) is applied here rather than asking a candidate's _root().
     */
    private static MutableList<SetImplementation> collectEffectiveRootClassMappings(CoreInstance targetClass, Mapping mapping, ProcessorSupport processorSupport)
    {
        MutableList<SetImplementation> result = Lists.mutable.empty();
        ModelJoinShared.forEachMappingInIncludeGraph(mapping, processorSupport, m ->
        {
            MutableList<SetImplementation> directClassMappings = Lists.mutable.empty();
            for (SetImplementation si : m._classMappings())
            {
                if (!(si instanceof EmbeddedSetImplementation) && (targetClass == ImportStub.withImportStubByPass(si._classCoreInstance(), processorSupport)))
                {
                    directClassMappings.add(si);
                }
            }
            result.addAllIterable(MappingValidator.getEffectiveRoots(directClassMappings));
        });
        return result;
    }

    private void processRelationFunctionPropertyMapping(RelationFunctionPropertyMapping pm, GenericType rowType, GenericType relationType, Matcher matcher, ProcessorState state, ProcessorSupport processorSupport)
    {
        if (pm._transformerCoreInstance() != null)
        {
            GrammarInfoStub transformerStub = (GrammarInfoStub) pm._transformerCoreInstance();
            EnumerationMappingProcessor.processsEnumerationTransformer(transformerStub, pm, processorSupport);
        }

        LambdaFunction<?> valueFn = pm._valueFn();
        if (valueFn == null)
        {
            return;
        }

        ListIterable<SimpleFunctionExpression> joinChain;
        try (ProcessorState.VariableContextScope ignore = state.withNewVariableContext())
        {
            // Inject the `row` parameter typed at the relation function's
            // last-expression row type (the first type-arg of Relation<T>) so
            // `$row.<col>` resolves against the columns of the row.
            FunctionType ft = getFunctionType(valueFn, processorSupport);
            ft._parametersAdd(createRowParameter(rowType, ft.getSourceInformation(), pm.getSourceInformation(), processorSupport));

            // `src` is deliberately not a parameter of the valueFn: it only types the source of a
            // navigationJoin/filterJoin chain, which is lowered to SQL (never evaluated in memory) with
            // the class mapping's own relation in its place. preResolveNavigationFilterJoinUsage rejects
            // every other use, so no other free `src` reference survives into the processed valueFn.
            GenericType srcType = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericType(relationType, processorSupport);
            srcType.setSourceInformation(pm.getSourceInformation());
            state.getVariableContext().buildAndRegister("src", srcType, (Multiplicity) processorSupport.package_getByUserPath(M3Paths.PureOne), processorSupport);

            joinChain = preResolveNavigationFilterJoinUsage(valueFn, matcher, state, processorSupport);

            matcher.fullMatch(valueFn, state);
        }
        removeSrcOpenVariable(valueFn);
        // The JoinKind literal is only typed once matched; every join call is in the chain by now.
        joinChain.forEach(call -> rejectUnsupportedJoinKind(call, processorSupport));
    }

    /**
     * {@code src} is registered in the enclosing variable context rather than as a parameter, so
     * LambdaFunctionProcessor records the join chain's {@code $src} as an open variable of the valueFn. It isn't
     * one: there is no binding to capture, as the chain is lowered with the class mapping's own relation in its
     * place. LambdaFunctionProcessor only computes open variables when there are none, so after an unbind (which
     * leaves them as they are) the rebind recomputes them and this drops {@code src} again.
     */
    private static void removeSrcOpenVariable(LambdaFunction<?> valueFn)
    {
        RichIterable<? extends String> openVariables = valueFn._openVariables();
        if (openVariables.contains("src"))
        {
            MutableList<String> remaining = Lists.mutable.<String>withAll(openVariables).without("src");
            if (remaining.isEmpty())
            {
                valueFn._openVariablesRemove();
            }
            else
            {
                valueFn._openVariables(remaining);
            }
        }
    }

    // RIGHT/FULL would keep target rows with no source row, which a property of the source class can't represent.
    private static void rejectUnsupportedJoinKind(SimpleFunctionExpression call, ProcessorSupport processorSupport)
    {
        ListIterable<? extends ValueSpecification> params = ListHelper.wrapListIterable(call._parametersValues());
        if (params.size() == 4)
        {
            String functionName = joinFunctionName(call);
            String kind = joinKindLiteral(params.get(2), processorSupport);
            if (kind == null)
            {
                // Anything other than a literal JoinKind.XXX (e.g. a variable or a computed
                // expression) can't be checked here, so the RIGHT/FULL gate below could otherwise be
                // silently bypassed; require a literal instead of letting it through unchecked.
                throw new PureCompilationException(params.get(2).getSourceInformation(), functionName
                        + "()'s JoinKind argument must be a literal value (e.g. JoinKind.LEFT), not a computed expression.");
            }
            if ("RIGHT".equals(kind) || "FULL".equals(kind))
            {
                // navigationJoin defaults to LEFT; filterJoin defaults to INNER (its gate) -- name whichever
                // one is this function's actual default first, so the message doesn't mislead either way.
                String suggestion = "filterJoin".equals(functionName) ? "JoinKind.INNER (the default) or JoinKind.LEFT" : "JoinKind.LEFT (the default) or JoinKind.INNER";
                throw new PureCompilationException(params.get(2).getSourceInformation(), "JoinKind." + kind + " is not supported by "
                        + functionName + "(); use " + suggestion + ".");
            }
        }
    }

    private static String joinKindLiteral(ValueSpecification kind, ProcessorSupport processorSupport)
    {
        if (!(kind instanceof SimpleFunctionExpression) || !"extractEnumValue".equals(((SimpleFunctionExpression) kind)._functionName()))
        {
            return null;
        }
        ListIterable<? extends ValueSpecification> params = ListHelper.wrapListIterable(((SimpleFunctionExpression) kind)._parametersValues());
        if (params.size() != 2 || !(params.get(1) instanceof InstanceValue)
                || kind._genericType() == null || ImportStub.withImportStubByPass(kind._genericType()._rawTypeCoreInstance(), processorSupport) != processorSupport.package_getByUserPath(JOIN_KIND))
        {
            return null;
        }
        CoreInstance value = ((InstanceValue) params.get(1))._valuesCoreInstance().getAny();
        return value == null ? null : value.getName();
    }

    /**
     * Validates where navigationJoin/filterJoin and {@code $src} appear in a property's value expression, and
     * returns the join chain (outermost call first) so its JoinKinds can be checked once matched. The only
     * supported shapes are a chain as the entire value expression, or a chain followed by a single column
     * access; the chain's innermost source must be {@code $src}, {@code $src} may appear nowhere else, and
     * {@code $row} may appear in none of the joins' arguments (their join conditions included).
     */
    private ListIterable<SimpleFunctionExpression> preResolveNavigationFilterJoinUsage(LambdaFunction<?> valueFn, Matcher matcher, ProcessorState state, ProcessorSupport processorSupport)
    {
        SimpleFunctionExpression chainHead = null;
        SimpleFunctionExpression columnAccess = null;
        ListIterable<? extends ValueSpecification> body = ListHelper.wrapListIterable(valueFn._expressionSequence());
        if (body.size() == 1)
        {
            ValueSpecification topLevel = body.get(0);

            if (topLevel instanceof SimpleFunctionExpression && isNavigationOrFilterJoinCall((SimpleFunctionExpression) topLevel, processorSupport))
            {
                chainHead = (SimpleFunctionExpression) topLevel;
            }
            else if (topLevel instanceof SimpleFunctionExpression && ((SimpleFunctionExpression) topLevel)._propertyName() != null)
            {
                SimpleFunctionExpression propertyAccess = (SimpleFunctionExpression) topLevel;
                ListIterable<? extends ValueSpecification> params = ListHelper.wrapListIterable(propertyAccess._parametersValues());
                ValueSpecification source = params.isEmpty() ? null : params.get(0);
                if (source instanceof SimpleFunctionExpression && isNavigationOrFilterJoinCall((SimpleFunctionExpression) source, processorSupport))
                {
                    chainHead = (SimpleFunctionExpression) source;
                    columnAccess = propertyAccess;
                }
                else if (isSrcVariable(source))
                {
                    String propertyName = ImportStub.withImportStubByPass(propertyAccess._propertyName()._valuesCoreInstance().getAny(), processorSupport).getName();
                    throw new PureCompilationException(source.getSourceInformation(), "Can't find the property '" + propertyName + "' in the class " + M3Paths.Relation + "; use $row." + propertyName + " to access a column of the mapped row.");
                }
            }
        }

        MutableList<SimpleFunctionExpression> joinChain = Lists.mutable.empty();
        VariableExpression chainSource = chainHead == null ? null : collectJoinChain(chainHead, joinChain, processorSupport);
        rejectMisplacedJoinUsage(valueFn, joinChain, chainSource, processorSupport);
        // Every join call is in the chain by now, whatever the value's shape.
        joinChain.forEach(RelationFunctionInstanceSetImplementationProcessor::rejectRowVariable);

        if (columnAccess != null)
        {
            matcher.fullMatch(chainHead, state);

            CoreInstance rowRawType = ImportStub.withImportStubByPass(chainHead._genericType()._typeArguments().getFirst()._rawTypeCoreInstance(), processorSupport);
            if (rowRawType instanceof RelationType)
            {
                String propertyName = ImportStub.withImportStubByPass(columnAccess._propertyName()._valuesCoreInstance().getAny(), processorSupport).getName();
                columnAccess._funcCoreInstance(_RelationType.findColumn((RelationType<?>) rowRawType, propertyName, columnAccess.getSourceInformation(), processorSupport));
            }
        }
        return joinChain;
    }

    // Collects the chain from `call` down through each join's source, and returns the `$src` at its root.
    private static VariableExpression collectJoinChain(SimpleFunctionExpression call, MutableList<SimpleFunctionExpression> chain, ProcessorSupport processorSupport)
    {
        chain.add(call);
        ListIterable<? extends ValueSpecification> params = ListHelper.wrapListIterable(call._parametersValues());
        ValueSpecification source = params.isEmpty() ? null : params.get(0);
        if (isSrcVariable(source))
        {
            return (VariableExpression) source;
        }
        if (source instanceof SimpleFunctionExpression && isNavigationOrFilterJoinCall((SimpleFunctionExpression) source, processorSupport))
        {
            return collectJoinChain((SimpleFunctionExpression) source, chain, processorSupport);
        }
        throw new PureCompilationException(call.getSourceInformation(), "The source of '" + joinFunctionName(call) + "' must be $src or another navigationJoin/filterJoin call.");
    }

    // A single walk over the value expression: any join call outside the chain, or any `$src` other than the
    // chain's own source, is rejected. A nested lambda's own {src | ...} is bound within it, so isn't mistaken
    // for the relation-level $src.
    private static void rejectMisplacedJoinUsage(LambdaFunction<?> valueFn, ListIterable<SimpleFunctionExpression> joinChain, VariableExpression chainSource, ProcessorSupport processorSupport)
    {
        CoreInstance misplaced = ModelJoinShared.findFirst(valueFn, Sets.immutable.empty(), (node, boundNames) ->
                (node != chainSource && isSrcVariable(node) && !boundNames.contains("src"))
                        || (node instanceof SimpleFunctionExpression && !joinChain.anySatisfy(c -> c == node) && isNavigationOrFilterJoinCall((SimpleFunctionExpression) node, processorSupport)));
        if (misplaced instanceof VariableExpression)
        {
            throw new PureCompilationException(misplaced.getSourceInformation(), "$src is the whole source relation and may only be used as the source of navigationJoin/filterJoin (e.g. $src->navigationJoin(...).COLUMN); use $row to access the columns of the mapped row.");
        }
        if (misplaced != null)
        {
            String functionName = joinFunctionName((SimpleFunctionExpression) misplaced);
            throw new PureCompilationException(misplaced.getSourceInformation(), "'" + functionName
                    + "' may only be used as the property's entire value expression, or immediately followed by a single column access (e.g. $src->" + functionName
                    + "(...).COLUMN); calling it from inside a helper function or a more complex expression is not supported.");
        }
    }

    // A join is lowered over the whole source relation, not evaluated per mapped row, so the valueFn's `row`
    // parameter means nothing in its arguments. Unchecked, a $row there would compile when the chain is the
    // whole value (the valueFn binds row), yet fail only as "unknown" when it is followed by a column access
    // (the chain is then matched on its own, outside the valueFn). The source (the first argument) is the
    // chain itself, or $src at its root. A nested lambda's own {row | ...} is bound within it, so is allowed.
    private static void rejectRowVariable(SimpleFunctionExpression call)
    {
        ListIterable<? extends ValueSpecification> params = ListHelper.wrapListIterable(call._parametersValues());
        for (int i = 1; i < params.size(); i++)
        {
            VariableExpression row = findFreeVariable(params.get(i), Sets.immutable.with("row"));
            if (row != null)
            {
                String functionName = joinFunctionName(call);
                throw new PureCompilationException(row.getSourceInformation(), (i == params.size() - 1)
                        ? functionName + "()'s join condition is over the source and target rows: $row is not available. Use the condition's parameters, e.g. {p, f | $p.FIRMID == $f.ID}."
                        : "$row is not available in the arguments of " + functionName + "(): the join is over the whole source relation, not the mapped row.");
            }
        }
    }

    private static boolean isSrcVariable(CoreInstance node)
    {
        return node instanceof VariableExpression && "src".equals(((VariableExpression) node)._name());
    }

    // Recognises both the bare and the fully-qualified form, so qualifying the call can't bypass the checks above.
    private static boolean isNavigationOrFilterJoinCall(SimpleFunctionExpression call, ProcessorSupport processorSupport)
    {
        if (call._functionName() == null)
        {
            return false;
        }
        String name = joinFunctionName(call);
        return ("navigationJoin".equals(name) || "filterJoin".equals(name))
                && ModelJoinShared.isCallToPlatformFunction(call._functionName(), call._importGroup(), RELATION_FUNCTIONS_PACKAGE, processorSupport);
    }

    private static String joinFunctionName(SimpleFunctionExpression call)
    {
        return ModelJoinShared.simpleFunctionName(call._functionName());
    }

    private VariableExpression createRowParameter(GenericType rowType, SourceInformation genericTypeSourceInfo, SourceInformation parameterSourceInformation, ProcessorSupport processorSupport)
    {
        GenericType copy = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericType(rowType, processorSupport);
        copy.setSourceInformation(genericTypeSourceInfo);
        VariableExpression rowParam = (VariableExpression) processorSupport.newAnonymousCoreInstance(parameterSourceInformation, M3Paths.VariableExpression);
        return rowParam._name("row")
                ._genericType(copy)
                ._multiplicity((Multiplicity) processorSupport.package_getByUserPath(M3Paths.PureOne));
    }

    private static FunctionType getFunctionType(LambdaFunction<?> lambdaFunction, ProcessorSupport processorSupport)
    {
        GenericType typeArgument = lambdaFunction._classifierGenericType()._typeArguments().getAny();
        return (FunctionType) ImportStub.withImportStubByPass(typeArgument._rawTypeCoreInstance(), processorSupport);
    }

    @Override
    public void populateReferenceUsages(RelationFunctionInstanceSetImplementation instance, ModelRepository repository, ProcessorSupport processorSupport)
    {
        // Only emit the reference-usage when relationFunction is an ImportStub
        // (i.e. `~func` form).  For `~src`, the LambdaFunction is owned by this
        // mapping and not referenced from elsewhere.
        CoreInstance rfRaw = instance._relationFunctionCoreInstance();
        if (rfRaw != null && processorSupport.instance_instanceOf(rfRaw, M3Paths.ImportStub))
        {
            addReferenceUsageForToOneProperty(instance, rfRaw, M2MappingProperties.relationFunction, repository, processorSupport);
        }
    }

    @Override
    public String getClassName()
    {
        return M2MappingPaths.RelationFunctionInstanceSetImplementation;
    }
}

