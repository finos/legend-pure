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

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.match.GenericTypeMatch;
import org.finos.legend.pure.m3.navigation.generictype.match.NullMatchBehavior;
import org.finos.legend.pure.m3.navigation.generictype.match.ParameterMatchBehavior;
import org.finos.legend.pure.m3.navigation.multiplicity.MultiplicityMatch;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.Arrays;

class FunctionMatch implements Comparable<FunctionMatch>
{
    private final GenericTypeMatch[] typeMatches;
    private final MultiplicityMatch[] multiplicityMatches;

    private FunctionMatch(GenericTypeMatch[] typeMatches, MultiplicityMatch[] multiplicityMatches)
    {
        this.typeMatches = typeMatches;
        this.multiplicityMatches = multiplicityMatches;
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(this.typeMatches) + (43 * Arrays.hashCode(this.multiplicityMatches));
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

        FunctionMatch otherMatch = (FunctionMatch) other;
        return Arrays.equals(this.typeMatches, otherMatch.typeMatches) && Arrays.equals(this.multiplicityMatches, otherMatch.multiplicityMatches);
    }

    @Override
    public int compareTo(FunctionMatch other)
    {
        if (this == other)
        {
            return 0;
        }

        int size = this.typeMatches.length;
        if (other.typeMatches.length != size)
        {
            return Integer.compare(size, other.typeMatches.length);
        }

        // First, compare type matches
        for (int i = 0; i < size; i++)
        {
            int comparison = this.typeMatches[i].compareTo(other.typeMatches[i]);
            if (comparison != 0)
            {
                return comparison;
            }
        }

        // If we still have a tie, compare multiplicity matches
        for (int i = 0; i < size; i++)
        {
            int comparison = this.multiplicityMatches[i].compareTo(other.multiplicityMatches[i]);
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
        ArrayIterate.appendString(this.typeMatches, builder, "typeMatches=[", ", ", "]");
        ArrayIterate.appendString(this.multiplicityMatches, builder, " multMatches=[", ", ", "]");
        return builder.append('>').toString();
    }

    static FunctionMatch newFunctionMatch(Function<?> function, String functionToFindName, ListIterable<? extends ValueSpecification> givenParameters, boolean lenient, ProcessorSupport processorSupport)
    {
        if (!functionToFindName.equals(function._functionName()))
        {
            return null;
        }

        FunctionType functionType = (FunctionType) processorSupport.function_getFunctionType(function);
        ListIterable<? extends CoreInstance> parameters = ListHelper.wrapListIterable(functionType._parameters());
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

        return new FunctionMatch(typeMatches, multiplicityMatches);
    }
}
