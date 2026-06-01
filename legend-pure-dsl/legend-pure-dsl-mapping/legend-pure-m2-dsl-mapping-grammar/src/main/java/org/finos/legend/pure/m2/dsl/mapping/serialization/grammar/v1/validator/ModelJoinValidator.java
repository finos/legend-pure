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

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelJoin.ModelJoinAssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelJoin.ModelJoinPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class ModelJoinValidator implements MatchRunner<ModelJoinAssociationImplementation>
{
    @Override
    public String getClassName()
    {
        return M2MappingPaths.ModelJoinAssociationImplementation;
    }

    @Override
    public void run(ModelJoinAssociationImplementation instance, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();
        CoreInstance booleanType = processorSupport.package_getByUserPath(M3Paths.Boolean);

        // The expanded property mapping list (after N*M subtype pairing) shares lambda references
        // across clones; we only need to validate each distinct lambda once.
        MutableSet<LambdaFunction<?>> seen = Sets.mutable.empty();
        for (PropertyMapping pm : instance._propertyMappings())
        {
            LambdaFunction<?> joinCondition = ((ModelJoinPropertyMapping) pm)._joinCondition();
            if (joinCondition == null || !seen.add(joinCondition))
            {
                continue;
            }
            validateReturnsBoolean(joinCondition, instance, booleanType, processorSupport);
        }
    }

    private static void validateReturnsBoolean(LambdaFunction<?> joinCondition, ModelJoinAssociationImplementation instance, CoreInstance booleanType, ProcessorSupport processorSupport)
    {
        ValueSpecification lastExpression = lastConditionExpression(joinCondition);
        if (lastExpression == null)
        {
            return;
        }
        CoreInstance rawType = rawReturnType(lastExpression, processorSupport);
        if (rawType == booleanType)
        {
            return;
        }
        throw new PureCompilationException(
                lastExpression.getSourceInformation() != null ? lastExpression.getSourceInformation() : instance.getSourceInformation(),
                "ModelJoin join condition must return Boolean[1], found: " + (rawType != null ? rawType.getName() : "unknown"));
    }

    /**
     * Returns the last expression in the join condition's body.
     */
    private static ValueSpecification lastConditionExpression(LambdaFunction<?> joinCondition)
    {
        ListIterable<? extends ValueSpecification> body = joinCondition._expressionSequence().toList();
        return body.isEmpty() ? null : body.getLast();
    }

    private static CoreInstance rawReturnType(ValueSpecification expression, ProcessorSupport processorSupport)
    {
        if (expression._genericType() == null)
        {
            return null;
        }
        return ImportStub.withImportStubByPass(expression._genericType()._rawTypeCoreInstance(), processorSupport);
    }
}