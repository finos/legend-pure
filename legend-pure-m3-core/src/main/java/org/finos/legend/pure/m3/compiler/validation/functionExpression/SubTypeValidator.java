// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.m3.compiler.validation.functionExpression;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.generictype.match.GenericTypeMatch;
import org.finos.legend.pure.m3.navigation.generictype.match.NullMatchBehavior;
import org.finos.legend.pure.m3.navigation.generictype.match.ParameterMatchBehavior;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class SubTypeValidator
{
    public static void validateSubType(FunctionExpression instance, ProcessorSupport processorSupport) throws PureCompilationException
    {
        ListIterable<? extends ValueSpecification> valueSpecifications = instance._parametersValues().toList();
        GenericType sourceGenericType = valueSpecifications.get(0)._genericType();
        GenericType typeToCastTo = valueSpecifications.get(1)._genericType();
        if (!GenericTypeMatch.genericTypeMatches(sourceGenericType, typeToCastTo, true, NullMatchBehavior.ERROR, ParameterMatchBehavior.MATCH_CAUTIOUSLY, ParameterMatchBehavior.MATCH_CAUTIOUSLY, processorSupport))
        {
            throw new PureCompilationException(instance.getSourceInformation(), "The type "+ org.finos.legend.pure.m3.navigation.generictype.GenericType.print(sourceGenericType, processorSupport) + " is not compatible with "+ org.finos.legend.pure.m3.navigation.generictype.GenericType.print(typeToCastTo, processorSupport));
        }
    }
}
