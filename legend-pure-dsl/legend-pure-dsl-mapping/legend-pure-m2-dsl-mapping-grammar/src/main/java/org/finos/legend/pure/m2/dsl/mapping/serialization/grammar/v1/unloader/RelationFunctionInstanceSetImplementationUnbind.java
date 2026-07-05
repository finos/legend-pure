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

import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.unload.unbind.Shared;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.EmbeddedRelationFunctionSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.tools.GrammarInfoStub;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
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
        cleanPropertyMappings(instance._propertyMappings(), matcher, state);
    }

    private static void cleanPropertyMappings(Iterable<? extends PropertyMapping> propertyMappings, Matcher matcher, MatcherState state) throws PureCompilationException
    {
        ProcessorSupport ps = state.getProcessorSupport();
        for (PropertyMapping propertyMapping : propertyMappings)
        {
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
            else if (propertyMapping instanceof EmbeddedRelationFunctionSetImplementation)
            {
                cleanPropertyMappings(((EmbeddedRelationFunctionSetImplementation) propertyMapping)._propertyMappings(), matcher, state);
            }
        }
    }

    private static void cleanLambda(LambdaFunction<?> lambda, Matcher matcher, MatcherState state, ProcessorSupport ps)
    {
        matcher.fullMatch(lambda, state);
        FunctionType ft = (FunctionType) org.finos.legend.pure.m3.navigation.importstub.ImportStub.withImportStubByPass(
                lambda._classifierGenericType()._typeArguments().toList().get(0)._rawTypeCoreInstance(), ps);
        ft._parametersRemove();
        ft._functionRemove();
    }

    @Override
    public String getClassName()
    {
        return M2MappingPaths.RelationFunctionInstanceSetImplementation;
    }
}

