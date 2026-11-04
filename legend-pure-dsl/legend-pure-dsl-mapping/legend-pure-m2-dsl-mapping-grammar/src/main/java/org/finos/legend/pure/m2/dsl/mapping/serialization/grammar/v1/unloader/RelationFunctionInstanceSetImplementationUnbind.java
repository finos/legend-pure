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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.unloader;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor.ModelJoinProcessor;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor.ModelJoinShared;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.unload.unbind.Shared;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelJoin.ModelJoinPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.EmbeddedRelationFunctionSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStubInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.InferredGenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.tools.GrammarInfoStub;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class RelationFunctionInstanceSetImplementationUnbind implements MatchRunner<RelationFunctionInstanceSetImplementation>
{
    @Override
    public void run(RelationFunctionInstanceSetImplementation instance, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport ps = state.getProcessorSupport();
        CoreInstance relationFunction = instance._relationFunctionCoreInstance();
        if (relationFunction != null)
        {
            if (ps.instance_instanceOf(relationFunction, M3Paths.ImportStub))
            {
                // ~func form: clean the import-stub indirection.
                Shared.cleanUpReferenceUsage(relationFunction, instance, ps);
                Shared.cleanImportStub(relationFunction, ps);
            }
            else if (relationFunction instanceof LambdaFunction)
            {
                // ~src form: the relationFunction holds an anonymous lambda;
                // recursively unbind its body and clear inferred function-type
                // metadata (mirrors PureInstanceSetImplementationUnbind for
                // `transform`/`filter`).
                cleanLambda((LambdaFunction<?>) relationFunction, matcher, state, ps);
            }
        }
        if (instance._primaryKey().notEmpty())
        {
            instance._primaryKey(instance._primaryKey().collect(c -> primaryKeyPlaceholder(c, ps)).toList());
        }
        CoreInstance classReference = instance._classCoreInstance();
        ImportGroup importGroup = (classReference != null && ps.instance_instanceOf(classReference, M3Paths.ImportStub)) ? (ImportGroup) classReference.getValueForMetaPropertyToOne(M3Properties.importGroup) : null;
        cleanPropertyMappings(instance._propertyMappings(), importGroup, matcher, state, modelRepository);
    }

    private static Column<?, ?> primaryKeyPlaceholder(Column<?, ?> column, ProcessorSupport ps)
    {
        Column<?, ?> placeholder = (Column<?, ?>) ps.newAnonymousCoreInstance(column.getSourceInformation(), M3Paths.Column);
        return placeholder._name(column._name());
    }

    private static void cleanPropertyMappings(Iterable<? extends PropertyMapping> propertyMappings, ImportGroup importGroup, Matcher matcher, MatcherState state, ModelRepository modelRepository) throws PureCompilationException
    {
        ProcessorSupport ps = state.getProcessorSupport();
        for (PropertyMapping propertyMapping : propertyMappings)
        {
            if (propertyMapping._localMappingPropertyTypeCoreInstance() != null)
            {
                Shared.cleanImportStub(propertyMapping._localMappingPropertyTypeCoreInstance(), ps);
            }
            if (propertyMapping instanceof RelationFunctionPropertyMapping)
            {
                RelationFunctionPropertyMapping rpm = (RelationFunctionPropertyMapping) propertyMapping;
                LambdaFunction<?> valueFn = rpm._valueFn();
                if (valueFn != null)
                {
                    cleanLambda(valueFn, matcher, state, ps);
                }
                if (rpm._transformerCoreInstance() != null)
                {
                    GrammarInfoStub transformerStub = (GrammarInfoStub) rpm._transformerCoreInstance();
                    transformerStub._value(transformerStub._original());
                    transformerStub._originalRemove();
                }
            }
            else if (propertyMapping instanceof ModelJoinPropertyMapping)
            {
                // Inline modelJoin: unbind the condition and clear its param types and resolved target, so
                // RelationFunctionInstanceSetImplementationProcessor re-resolves both. The params keep their
                // positional _mj_src/_mj_tgt names (as ModelJoinUnbind can't restore user names either), but a
                // declared param type is restored, so that the recompile re-validates it as a full compile would.
                ModelJoinPropertyMapping mjpm = (ModelJoinPropertyMapping) propertyMapping;
                LambdaFunction<?> joinCondition = mjpm._joinCondition();
                if (joinCondition != null)
                {
                    ListIterable<? extends VariableExpression> params = ((FunctionType) ps.function_getFunctionType(joinCondition))._parameters().toList();
                    ListIterable<GenericType> declaredTypes = params.collect(p -> declaredParamType(p, importGroup, modelRepository, ps));
                    matcher.fullMatch(joinCondition, state);
                    ModelJoinShared.renameLambdaParamsAndBody(joinCondition,
                            ModelJoinProcessor.MJ_SOURCE_VAR, ModelJoinProcessor.MJ_SOURCE_VAR, null,
                            ModelJoinProcessor.MJ_TARGET_VAR, ModelJoinProcessor.MJ_TARGET_VAR, null,
                            ps);
                    params.forEachWithIndex((p, i) ->
                    {
                        if (declaredTypes.get(i) != null)
                        {
                            p._genericType(declaredTypes.get(i));
                        }
                    });
                }
                mjpm._targetSetImplementationIdRemove();
            }
            else if (propertyMapping instanceof EmbeddedRelationFunctionSetImplementation)
            {
                cleanPropertyMappings(((EmbeddedRelationFunctionSetImplementation) propertyMapping)._propertyMappings(), importGroup, matcher, state, modelRepository);
            }
        }
    }

    /**
     * The declared type of an inline modelJoin condition's param, as a fresh by-path reference, or null if its type
     * was inferred. RelationFunctionInstanceSetImplementationProcessor marks an inferred type as an
     * InferredGenericType, and leaves a declared one typed to the declared class itself or to that class's MappingClass.
     */
    private static GenericType declaredParamType(VariableExpression param, ImportGroup importGroup, ModelRepository modelRepository, ProcessorSupport ps)
    {
        GenericType genericType = param._genericType();
        if (genericType == null || genericType instanceof InferredGenericType)
        {
            return null;
        }
        CoreInstance type = genericType._rawTypeCoreInstance();
        if (ps.instance_instanceOf(type, M2MappingPaths.MappingClass))
        {
            type = type.getValueForMetaPropertyToMany(M3Properties.generalizations).getOnly()
                    .getValueForMetaPropertyToOne(M3Properties.general)
                    .getValueForMetaPropertyToOne(M3Properties.rawType);
        }
        ImportStubInstance reference = ps.instance_instanceOf(type, M3Paths.ImportStub)
                ? ImportStubInstance.createPersistent(modelRepository, genericType.getSourceInformation(), type.getValueForMetaPropertyToOne(M3Properties.idOrPath).getName(), (ImportGroup) type.getValueForMetaPropertyToOne(M3Properties.importGroup))
                : ImportStubInstance.createPersistent(modelRepository, genericType.getSourceInformation(), PackageableElement.getUserPathForPackageableElement(type), importGroup);
        return (GenericType) Type.wrapGenericType(reference, genericType.getSourceInformation(), ps);
    }

    private static void cleanLambda(LambdaFunction<?> lambda, Matcher matcher, MatcherState state, ProcessorSupport ps)
    {
        matcher.fullMatch(lambda, state);
        FunctionType ft = (FunctionType) ps.function_getFunctionType(lambda);
        ft._parametersRemove();
        ft._functionRemove();
    }

    @Override
    public String getClassName()
    {
        return M2MappingPaths.RelationFunctionInstanceSetImplementation;
    }
}

