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

package org.finos.legend.pure.m3.compiler.postprocessing.functionmatch;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.generictype.match.GenericTypeMatch;
import org.finos.legend.pure.m3.navigation.generictype.match.NullMatchBehavior;
import org.finos.legend.pure.m3.navigation.generictype.match.ParameterMatchBehavior;
import org.finos.legend.pure.m3.navigation.multiplicity.MultiplicityMatch;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

class FunctionMatch implements Comparable<FunctionMatch>
{
    private final ImmutableList<GenericTypeMatch> typeMatches;
    private final ImmutableList<MultiplicityMatch> multiplicityMatches;

    private FunctionMatch(ImmutableList<GenericTypeMatch> typeMatches, ImmutableList<MultiplicityMatch> multiplicityMatches)
    {
        this.typeMatches = typeMatches;
        this.multiplicityMatches = multiplicityMatches;
    }

    @Override
    public int hashCode()
    {
        return this.typeMatches.hashCode() ^ this.multiplicityMatches.hashCode();
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof FunctionMatch))
        {
            return false;
        }

        FunctionMatch otherMatch = (FunctionMatch)other;
        return this.typeMatches.equals(otherMatch.typeMatches) && this.multiplicityMatches.equals(otherMatch.multiplicityMatches);
    }

    @Override
    public int compareTo(FunctionMatch other)
    {
        if (this == other)
        {
            return 0;
        }

        int size = this.typeMatches.size();
        if (other.typeMatches.size() != size)
        {
            return Integer.compare(size, other.typeMatches.size());
        }

        // First, compare type matches
        for (int i = 0; i < size; i++)
        {
            int comparison = this.typeMatches.get(i).compareTo(other.typeMatches.get(i));
            if (comparison != 0)
            {
                return comparison;
            }
        }

        // If we still have a tie, compare multiplicity matches
        for (int i = 0; i < size; i++)
        {
            int comparison = this.multiplicityMatches.get(i).compareTo(other.multiplicityMatches.get(i));
            if (comparison != 0)
            {
                return comparison;
            }
        }

        return 0;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("<FunctionMatch ");
        this.typeMatches.appendString(builder, "typeMatches=[", ", ", "]");
        builder.append(' ');
        this.multiplicityMatches.appendString(builder, "multMatches=[", ", ", "]");
        builder.append('>');
        return builder.toString();
    }

    static FunctionMatch newFunctionMatch(Function function, String functionToFindName, ListIterable<? extends ValueSpecification> givenParameters, boolean lenient, ProcessorSupport processorSupport)
    {
        if (!functionToFindName.equals(function._functionName()))
        {
            return null;
        }

        CoreInstance functionType = processorSupport.function_getFunctionType(function);
        ListIterable<? extends CoreInstance> parameters = Instance.getValueForMetaPropertyToManyResolved(functionType, M3Properties.parameters, processorSupport);
        int parameterCount = parameters.size();
        if (parameterCount != givenParameters.size())
        {
            return null;
        }

        GenericTypeMatch[] typeMatches = new GenericTypeMatch[parameterCount];
        MultiplicityMatch[] multiplicityMatches = new MultiplicityMatch[parameterCount];
        NullMatchBehavior nullMatchBehavior = lenient ? NullMatchBehavior.MATCH_ANYTHING : NullMatchBehavior.MATCH_NOTHING;
        for (int i = 0; i < parameterCount; i++)
        {
            CoreInstance parameter = parameters.get(i);
            CoreInstance value = givenParameters.get(i);

            CoreInstance paramGenericType = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.genericType, processorSupport);
            CoreInstance valueGenericType = Instance.getValueForMetaPropertyToOneResolved(value, M3Properties.genericType, processorSupport);
            GenericTypeMatch typeMatch = GenericTypeMatch.newGenericTypeMatch(paramGenericType, valueGenericType, true, nullMatchBehavior, ParameterMatchBehavior.MATCH_ANYTHING, ParameterMatchBehavior.MATCH_CAUTIOUSLY, processorSupport);
            if (typeMatch == null)
            {
                return null;
            }
            typeMatches[i] = typeMatch;

            CoreInstance paramMultiplicity = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.multiplicity, processorSupport);
            CoreInstance valueMultiplicity = Instance.getValueForMetaPropertyToOneResolved(value, M3Properties.multiplicity, processorSupport);
            MultiplicityMatch multiplicityMatch = MultiplicityMatch.newMultiplicityMatch(paramMultiplicity, valueMultiplicity, true, nullMatchBehavior, ParameterMatchBehavior.MATCH_ANYTHING, ParameterMatchBehavior.MATCH_CAUTIOUSLY);
            if (multiplicityMatch == null)
            {
                return null;
            }
            multiplicityMatches[i] = multiplicityMatch;
        }

        return new FunctionMatch(Lists.immutable.with(typeMatches), Lists.immutable.with(multiplicityMatches));
    }
}
