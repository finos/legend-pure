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
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState.VariableContextScope;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.InstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMappingValueSpecificationContext;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelJoin.ModelJoinAssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelJoin.ModelJoinPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class ModelJoinProcessor extends Processor<ModelJoinAssociationImplementation>
{
    /**
     * Canonical variable name for the source-side parameter in compiled ModelJoin lambdas.
     * Aligns with legend-engine's HelperMappingBuilder.MODEL_JOIN_SOURCE_VAR.
     */
    public static final String MJ_SOURCE_VAR = "_mj_src";

    /**
     * Canonical variable name for the target-side parameter in compiled ModelJoin lambdas.
     * Aligns with legend-engine's HelperMappingBuilder.MODEL_JOIN_TARGET_VAR.
     */
    public static final String MJ_TARGET_VAR = "_mj_tgt";

    @Override
    public String getClassName()
    {
        return M2MappingPaths.ModelJoinAssociationImplementation;
    }

    @Override
    public void process(ModelJoinAssociationImplementation instance, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        ModelJoinContext ctx = resolveContext(instance, processorSupport);
        processOriginalPropertyMappings(ctx, repository, processorSupport);
        MutableList<PropertyMapping> expanded = expandPropertyMappings(ctx, processorSupport);
        instance._propertyMappings(expanded);
        compileJoinConditions(instance, state, matcher, processorSupport);
    }

    // ------------------------------------------------------------------------------------------
    // Step 1 — resolve association + mapping + properties + the two parser-emitted PMs.
    // ------------------------------------------------------------------------------------------
    private ModelJoinContext resolveContext(ModelJoinAssociationImplementation instance, ProcessorSupport processorSupport)
    {
        Association association = (Association) ImportStub.withImportStubByPass(instance._associationCoreInstance(), processorSupport);
        if (association == null)
        {
            throw new PureCompilationException(instance.getSourceInformation(), "ModelJoin mapping missing association");
        }
        Mapping mapping = (Mapping) ImportStub.withImportStubByPass(instance._parentCoreInstance(), processorSupport);

        if (instance._id() == null)
        {
            instance._id(PackageableElement.getUserPathForPackageableElement(association, "_"));
        }

        ListIterable<? extends Property<?, ?>> properties = association._properties().toList();
        if (properties.size() != 2)
        {
            throw new PureCompilationException(instance.getSourceInformation(),
                    "ModelJoin requires an association with exactly 2 properties, found " + properties.size());
        }
        Property<?, ?> prop1 = properties.get(0);
        Property<?, ?> prop2 = properties.get(1);

        MutableList<PropertyMapping> originalPMs = Lists.mutable.withAll(instance._propertyMappings());
        if (originalPMs.size() != 2)
        {
            throw new PureCompilationException(instance.getSourceInformation(),
                    "ModelJoin expected 2 property mappings from parser, found " + originalPMs.size());
        }

        LambdaFunction<?> joinCondition = ((ModelJoinPropertyMapping) originalPMs.get(0))._joinCondition();

        // Extract lambda parameter names (parser enforces they match association property names).
        FunctionType joinFunctionType = (FunctionType) ImportStub.withImportStubByPass(
                joinCondition._classifierGenericType()._typeArguments().getOnly()._rawTypeCoreInstance(), processorSupport);
        ListIterable<? extends VariableExpression> lambdaParams = joinFunctionType._parameters().toList();

        String param1Name = lambdaParams.get(0)._name();
        String param2Name = lambdaParams.get(1)._name();

        // Resolve classes from association properties by matching param names.
        // When milestoning is active, we compare against the original (pre-milestoning) name.
        CoreInstance class1 = null;
        CoreInstance class2 = null;
        for (Property<?, ?> prop : properties)
        {
            String originalName = getOriginalPropertyName(prop, association, processorSupport);
            CoreInstance propReturnType = ImportStub.withImportStubByPass(
                    prop._genericType()._rawTypeCoreInstance(), processorSupport);
            if (originalName.equals(param1Name))
            {
                class1 = propReturnType;
            }
            else if (originalName.equals(param2Name))
            {
                class2 = propReturnType;
            }
        }
        if (class1 == null || class2 == null)
        {
            throw new PureCompilationException(instance.getSourceInformation(),
                    "ModelJoin: lambda parameter names {" + param1Name + ", " + param2Name
                            + "} do not match the association property names {"
                            + prop1._name() + ", " + prop2._name()
                            + "}. Lambda parameters must be named after the association properties.");
        }


        return new ModelJoinContext(instance, association, mapping, prop1, prop2, class1, class2,
                (ModelJoinPropertyMapping) originalPMs.get(0),
                (ModelJoinPropertyMapping) originalPMs.get(1),
                param1Name, param2Name);
    }

    // ------------------------------------------------------------------------------------------
    // Step 2 — run the standard PropertyMapping post-processing on the two parser-emitted PMs
    //          (sets _owner, resolves the property stub, defaults source/target IDs).
    // ------------------------------------------------------------------------------------------
    private void processOriginalPropertyMappings(ModelJoinContext ctx, ModelRepository repository, ProcessorSupport processorSupport)
    {
        PropertyMappingProcessor.processPropertyMapping(ctx.pm1, repository, processorSupport, ctx.association, ctx.instance);
        PropertyMappingProcessor.processPropertyMapping(ctx.pm2, repository, processorSupport, ctx.association, ctx.instance);
    }

    // ------------------------------------------------------------------------------------------
    // Step 3 — N×M expansion. For each direction, pair every source-class set with every
    //          target-class set (including subtypes via the include graph). Each PM gets its
    //          own freshly-parsed lambda with params renamed to _mj_src/_mj_tgt and typed to
    //          the correct MappingClass (or raw class).
    // ------------------------------------------------------------------------------------------
    private MutableList<PropertyMapping> expandPropertyMappings(ModelJoinContext ctx, ProcessorSupport processorSupport)
    {
        MutableList<InstanceSetImplementation> class1Sets = findAllSetsForClassOrSubtypes(ctx.class1, ctx.mapping, processorSupport);
        MutableList<InstanceSetImplementation> class2Sets = findAllSetsForClassOrSubtypes(ctx.class2, ctx.mapping, processorSupport);
        requireNonEmpty(class1Sets, ctx.class1, ctx.instance);
        requireNonEmpty(class2Sets, ctx.class2, ctx.instance);

        // Use the original (unmutated) lambda from PM1 as the clone source for all pairs.
        LambdaFunction<?> originalLambda = ctx.pm1._joinCondition();

        MutableList<PropertyMapping> all = Lists.mutable.empty();

        // For pm1 (property = prop1, returns class1): sources = class2 sets, targets = class1 sets
        String pm1PropertyName = resolveOriginalPropertyName(ctx.pm1, ctx.association, processorSupport);
        String pm1SrcParamName = pm1PropertyName.equals(ctx.param1Name) ? ctx.param2Name : ctx.param1Name;
        String pm1TgtParamName = pm1PropertyName.equals(ctx.param1Name) ? ctx.param1Name : ctx.param2Name;

        expandDirection(ctx.pm1, ctx.instance, class2Sets, class1Sets, pm1SrcParamName, pm1TgtParamName,
                originalLambda, processorSupport, all);

        // For pm2 (property = prop2, returns class2): sources = class1 sets, targets = class2 sets
        String pm2PropertyName = resolveOriginalPropertyName(ctx.pm2, ctx.association, processorSupport);
        String pm2SrcParamName = pm2PropertyName.equals(ctx.param1Name) ? ctx.param2Name : ctx.param1Name;
        String pm2TgtParamName = pm2PropertyName.equals(ctx.param1Name) ? ctx.param1Name : ctx.param2Name;

        expandDirection(ctx.pm2, ctx.instance, class1Sets, class2Sets, pm2SrcParamName, pm2TgtParamName,
                originalLambda, processorSupport, all);

        return all;
    }

    /**
     * Resolves the original (pre-milestoning) property name for a PM. When milestoning is active,
     * the association property may have been renamed to an edge-point (e.g., 'orderAllVersions').
     * We need the original name to match against the user's lambda param names.
     */
    private String resolveOriginalPropertyName(ModelJoinPropertyMapping pm, Association association, ProcessorSupport processorSupport)
    {
        Property<?, ?> resolvedProp = (Property<?, ?>) ImportStub.withImportStubByPass(pm._propertyCoreInstance(), processorSupport);
        return getOriginalPropertyName(resolvedProp, association, processorSupport);
    }

    /**
     * Returns the original (pre-milestoning) name of an association property. If the property
     * is an edge-point generated by milestoning, returns the name of the corresponding entry
     * in {@code originalMilestonedProperties}. Otherwise returns the property's current name.
     */
    private String getOriginalPropertyName(Property<?, ?> prop, Association association, ProcessorSupport processorSupport)
    {
        CoreInstance propReturnType = ImportStub.withImportStubByPass(
                prop._genericType()._rawTypeCoreInstance(), processorSupport);
        for (CoreInstance origProp : association.getValueForMetaPropertyToMany("originalMilestonedProperties"))
        {
            CoreInstance origReturnType = ImportStub.withImportStubByPass(
                    ((Property<?, ?>) origProp)._genericType()._rawTypeCoreInstance(), processorSupport);
            if (origReturnType != null && origReturnType.equals(propReturnType))
            {
                return ((Property<?, ?>) origProp)._name();
            }
        }
        return prop._name();
    }

    private void expandDirection(ModelJoinPropertyMapping templatePm, ModelJoinAssociationImplementation owner,
                                 MutableList<InstanceSetImplementation> sourceSets, MutableList<InstanceSetImplementation> targetSets,
                                 String srcParamName, String tgtParamName,
                                 LambdaFunction<?> originalLambda,
                                 ProcessorSupport processorSupport, MutableList<PropertyMapping> out)
    {
        boolean first = true;
        for (InstanceSetImplementation sourceSet : sourceSets)
        {
            for (InstanceSetImplementation targetSet : targetSets)
            {
                ModelJoinPropertyMapping pm;
                if (first)
                {
                    pm = templatePm;
                    first = false;
                }
                else
                {
                    pm = createClonePm(templatePm, owner, processorSupport);
                }
                pm._sourceSetImplementationId(sourceSet._id());
                pm._targetSetImplementationId(targetSet._id());

                // Deep-clone the original lambda for this pair — each PM gets independent nodes
                LambdaFunction<?> freshLambda = deepCloneLambda(originalLambda, processorSupport);
                replaceLambdaParams(freshLambda, srcParamName, tgtParamName, sourceSet, targetSet, processorSupport);
                pm._joinCondition(freshLambda);

                out.add(pm);
            }
        }
    }

    /**
     * Replaces the lambda's FunctionType params (named srcParamName/tgtParamName) with _mj_src/_mj_tgt
     * and sets their types to the appropriate MappingClass (or raw class from the set implementation).
     * Also walks the expression body to rename VariableExpression references (names only — the
     * compiler sets GenericTypes on body nodes during fullMatch in Step 5).
     */
    private void replaceLambdaParams(LambdaFunction<?> lambda, String srcParamName, String tgtParamName,
                                     InstanceSetImplementation sourceSet, InstanceSetImplementation targetSet,
                                     ProcessorSupport processorSupport)
    {
        if (lambda == null || lambda._classifierGenericType() == null)
        {
            return;
        }

        FunctionType fType = (FunctionType) ImportStub.withImportStubByPass(
                lambda._classifierGenericType()._typeArguments().getOnly()._rawTypeCoreInstance(), processorSupport);
        ListIterable<? extends VariableExpression> params = fType._parameters().toList();
        if (params.size() < 2)
        {
            return;
        }

        Class<?> srcClass = getSetImplementationClass(sourceSet, processorSupport);
        Class<?> tgtClass = getSetImplementationClass(targetSet, processorSupport);

        // Rename params and set types
        for (VariableExpression param : params)
        {
            String originalName = param._name();
            if (originalName.equals(srcParamName))
            {
                param._name(MJ_SOURCE_VAR);
                param._genericType((GenericType) Type.wrapGenericType(srcClass, param.getSourceInformation(), processorSupport));
            }
            else if (originalName.equals(tgtParamName))
            {
                param._name(MJ_TARGET_VAR);
                param._genericType((GenericType) Type.wrapGenericType(tgtClass, param.getSourceInformation(), processorSupport));
            }
        }

        // Walk expression body and rename variable references (names only — pass null for GenericTypes)
        for (ValueSpecification expr : lambda._expressionSequence())
        {
            ModelJoinShared.replaceVariableReferences(expr, srcParamName, MJ_SOURCE_VAR, null, tgtParamName, MJ_TARGET_VAR, null);
        }
    }

    /**
     * Deep-clones the lambda's expression tree so each PM gets independent CoreInstance nodes
     * that have never been visited by the compiler. The FunctionType and params are also cloned.
     * References to packageable elements (types, properties) are shared — only structural
     * nodes (ValueSpecification, GenericType wrapping, etc.) are cloned.
     */
    private LambdaFunction<?> deepCloneLambda(LambdaFunction<?> template, ProcessorSupport processorSupport)
    {
        return (LambdaFunction<?>) ModelJoinShared.deepClone(template, processorSupport);
    }

    /**
     * Creates a clone of the property mapping shell (without lambda — that's set by the caller).
     */
    private ModelJoinPropertyMapping createClonePm(ModelJoinPropertyMapping template, ModelJoinAssociationImplementation owner,
                                                   ProcessorSupport processorSupport)
    {
        ModelJoinPropertyMapping clone = (ModelJoinPropertyMapping) processorSupport.newAnonymousCoreInstance(
                template.getSourceInformation(), M2MappingPaths.ModelJoinPropertyMapping);
        clone._propertyCoreInstance(template._propertyCoreInstance());
        clone._owner(owner);
        return clone;
    }

    private Class<?> getSetImplementationClass(InstanceSetImplementation setImpl, ProcessorSupport processorSupport)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingClass<?> mappingClass = setImpl._mappingClass();
        if (mappingClass != null)
        {
            return mappingClass;
        }
        return (Class<?>) ImportStub.withImportStubByPass(setImpl._classCoreInstance(), processorSupport);
    }

    private static void requireNonEmpty(MutableList<InstanceSetImplementation> sets, CoreInstance clazz, ModelJoinAssociationImplementation instance)
    {
        if (sets.isEmpty())
        {
            throw new PureCompilationException(instance.getSourceInformation(),
                    "ModelJoin: no class mapping found for type '" + clazz.getName() + "' in mapping");
        }
    }

    // ------------------------------------------------------------------------------------------
    // Step 5 — compile the join condition lambdas and attach PropertyMappingValueSpecificationContext.
    //          Params have already been renamed and typed in expandDirection (step 4).
    // ------------------------------------------------------------------------------------------
    private void compileJoinConditions(ModelJoinAssociationImplementation instance, ProcessorState state, Matcher matcher, ProcessorSupport processorSupport)
    {
        int i = 0;
        for (PropertyMapping pm : instance._propertyMappings())
        {
            ModelJoinPropertyMapping mjPm = (ModelJoinPropertyMapping) pm;
            try (VariableContextScope ignore = state.withNewVariableContext())
            {
                matcher.fullMatch(mjPm._joinCondition(), state);

                ValueSpecification firstExpr = mjPm._joinCondition()._expressionSequence().toList().getFirst();
                if (firstExpr != null)
                {
                    PropertyMappingValueSpecificationContext usageContext = (PropertyMappingValueSpecificationContext)
                            processorSupport.newAnonymousCoreInstance(null, M2MappingPaths.PropertyMappingValueSpecificationContext);
                    usageContext._offset(i);
                    usageContext._propertyMapping(mjPm);
                    firstExpr._usageContext(usageContext);
                }
            }
            i++;
        }
    }

    // ------------------------------------------------------------------------------------------
    // Mapping traversal — find all class mappings whose mapped class equals or extends `clazz`.
    // ------------------------------------------------------------------------------------------
    private MutableList<InstanceSetImplementation> findAllSetsForClassOrSubtypes(CoreInstance clazz, Mapping mapping, ProcessorSupport processorSupport)
    {
        MutableList<InstanceSetImplementation> result = Lists.mutable.empty();
        collectSetsForClassOrSubtypes(clazz, mapping, processorSupport, result, Lists.mutable.empty());
        return result;
    }

    private void collectSetsForClassOrSubtypes(CoreInstance clazz, Mapping mapping, ProcessorSupport processorSupport, MutableList<InstanceSetImplementation> result, MutableList<Mapping> visited)
    {
        if (visited.contains(mapping))
        {
            return;
        }
        visited.add(mapping);

        for (SetImplementation si : mapping._classMappings())
        {
            if (si instanceof InstanceSetImplementation)
            {
                CoreInstance mappedClass = ImportStub.withImportStubByPass(si._classCoreInstance(), processorSupport);
                if (mappedClass.equals(clazz) || Type.subTypeOf(mappedClass, clazz, processorSupport))
                {
                    result.add((InstanceSetImplementation) si);
                }
            }
        }

        for (MappingInclude include : mapping._includes())
        {
            Mapping includedMapping = (Mapping) ImportStub.withImportStubByPass(include._includedCoreInstance(), processorSupport);
            if (includedMapping != null)
            {
                collectSetsForClassOrSubtypes(clazz, includedMapping, processorSupport, result, visited);
            }
        }
    }

    @Override
    public void populateReferenceUsages(ModelJoinAssociationImplementation classMapping, ModelRepository repository, ProcessorSupport processorSupport)
    {
    }

    /**
     * Per-invocation context bundling everything resolved in step 1.
     */
    private static final class ModelJoinContext
    {
        final ModelJoinAssociationImplementation instance;
        final Association association;
        final Mapping mapping;
        final Property<?, ?> prop1;
        final Property<?, ?> prop2;
        final CoreInstance class1;
        final CoreInstance class2;
        final ModelJoinPropertyMapping pm1;
        final ModelJoinPropertyMapping pm2;
        final String param1Name;
        final String param2Name;

        ModelJoinContext(ModelJoinAssociationImplementation instance, Association association, Mapping mapping,
                         Property<?, ?> prop1, Property<?, ?> prop2,
                         CoreInstance class1, CoreInstance class2,
                         ModelJoinPropertyMapping pm1, ModelJoinPropertyMapping pm2,
                         String param1Name, String param2Name)
        {
            this.instance = instance;
            this.association = association;
            this.mapping = mapping;
            this.prop1 = prop1;
            this.prop2 = prop2;
            this.class1 = class1;
            this.class2 = class2;
            this.pm1 = pm1;
            this.pm2 = pm2;
            this.param1Name = param1Name;
            this.param2Name = param2Name;
        }
    }
}