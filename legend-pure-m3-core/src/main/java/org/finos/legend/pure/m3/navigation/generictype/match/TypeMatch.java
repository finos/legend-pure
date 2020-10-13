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

package org.finos.legend.pure.m3.navigation.generictype.match;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.function.FunctionType;
import org.finos.legend.pure.m3.navigation.multiplicity.MultiplicityMatch;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

abstract class TypeMatch implements Comparable<TypeMatch>
{
    private static final TypeMatch EXACT_MATCH = new SimpleTypeMatch(0); // Types are identical
    private static final TypeMatch NON_CONCRETE_MATCH = new NonConcreteTypeMatch(); // Either type is non-concrete
    private static final TypeMatch BOTTOM_TYPE_MATCH = new BottomTypeMatch(); // Value type is bottom type
    private static final TypeMatch NULL_MATCH = new NullTypeMatch();

    private TypeMatch()
    {
    }

    private static class SimpleTypeMatch extends TypeMatch
    {
        private final int typeDistance;

        private SimpleTypeMatch(int typeDistance)
        {
            this.typeDistance = typeDistance;
        }

        @Override
        public int hashCode()
        {
            return this.typeDistance;
        }

        @Override
        public boolean equals(Object other)
        {
            return (this == other) || ((other instanceof SimpleTypeMatch) && (this.typeDistance == ((SimpleTypeMatch)other).typeDistance));
        }

        @Override
        public int compareTo(TypeMatch other)
        {
            if (this == other)
            {
                return 0;
            }

            if (other instanceof NonConcreteTypeMatch)
            {
                return (this.typeDistance == 0) ? -1 : 1;
            }

            if (!(other instanceof SimpleTypeMatch))
            {
                return -1;
            }

            return Integer.compare(this.typeDistance, ((SimpleTypeMatch)other).typeDistance);
        }

        @Override
        public String toString()
        {
            return "<TypeMatch typeDistance=" + this.typeDistance + ">";
        }
    }

    private static class NonConcreteTypeMatch extends TypeMatch
    {
        private NonConcreteTypeMatch()
        {
        }

        @Override
        public int hashCode()
        {
            return NonConcreteTypeMatch.class.hashCode();
        }

        @Override
        public boolean equals(Object other)
        {
            return (this == other) || (other instanceof NonConcreteTypeMatch);
        }

        @Override
        public String toString()
        {
            return "<TypeMatch non-concrete>";
        }

        @Override
        public int compareTo(TypeMatch other)
        {
            if (this == other)
            {
                return 0;
            }

            if (other instanceof SimpleTypeMatch)
            {
                return (((SimpleTypeMatch)other).typeDistance == 0) ? 1 : -1;
            }

            return (other instanceof NonConcreteTypeMatch) ? 0 : -1;
        }
    }

    private static class FunctionTypeMatch extends TypeMatch
    {
        private final ImmutableList<GenericTypeMatch> parameterTypeMatches;
        private final ImmutableList<MultiplicityMatch> parameterMultiplicityMatches;
        private final GenericTypeMatch returnTypeMatch;
        private final MultiplicityMatch returnMultiplicityMatch;

        private FunctionTypeMatch(Iterable<? extends GenericTypeMatch> paramTypeMatches, Iterable<? extends MultiplicityMatch> paramMultMatches, GenericTypeMatch returnTypeMatch, MultiplicityMatch returnMultMatch)
        {
            this.parameterTypeMatches = Lists.immutable.withAll(paramTypeMatches);
            this.parameterMultiplicityMatches = Lists.immutable.withAll(paramMultMatches);
            this.returnTypeMatch = returnTypeMatch;
            this.returnMultiplicityMatch = returnMultMatch;
        }

        @Override
        public int hashCode()
        {
            return this.parameterTypeMatches.hashCode() ^
                    this.parameterMultiplicityMatches.hashCode() ^
                    this.returnTypeMatch.hashCode() ^
                    this.returnMultiplicityMatch.hashCode();
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }

            if (!(other instanceof FunctionTypeMatch))
            {
                return false;
            }

            FunctionTypeMatch otherMatch = (FunctionTypeMatch)other;
            return this.parameterTypeMatches.equals(otherMatch.parameterTypeMatches) &&
                    this.parameterMultiplicityMatches.equals(otherMatch.parameterMultiplicityMatches) &&
                    this.returnTypeMatch.equals(otherMatch.returnTypeMatch) &&
                    this.returnMultiplicityMatch.equals(otherMatch.returnMultiplicityMatch);
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder("<TypeMatch FunctionType");
            this.parameterTypeMatches.appendString(builder, " parameterTypeMatches=[", ", ", "]");
            this.parameterMultiplicityMatches.appendString(builder, " parameterMultiplicityMatches=[", ", ", "]");
            builder.append(" returnTypeMatch=");
            builder.append(this.returnTypeMatch);
            builder.append(" returnMultiplicityMatch=");
            builder.append(this.returnMultiplicityMatch);
            builder.append('>');
            return builder.toString();
        }

        @Override
        public int compareTo(TypeMatch other)
        {
            if (this == other)
            {
                return 0;
            }

            if ((other instanceof SimpleTypeMatch) || (other instanceof NonConcreteTypeMatch))
            {
                return 1;
            }

            if (!(other instanceof FunctionTypeMatch))
            {
                return -1;
            }

            FunctionTypeMatch otherMatch = (FunctionTypeMatch)other;
            int comparison = 0;

            int thisParamTypeCount = this.parameterTypeMatches.size();
            int otherParamTypeCount = otherMatch.parameterTypeMatches.size();
            for (int i = 0, minCount = Math.min(thisParamTypeCount, otherParamTypeCount); i < minCount; i++)
            {
                comparison = this.parameterTypeMatches.get(i).compareTo(otherMatch.parameterTypeMatches.get(i));
                if (comparison != 0)
                {
                    return comparison;
                }
            }
            comparison = Integer.compare(thisParamTypeCount, otherParamTypeCount);
            if (comparison != 0)
            {
                return comparison;
            }

            int thisParamMultCount = this.parameterMultiplicityMatches.size();
            int otherParamMultCount = otherMatch.parameterMultiplicityMatches.size();
            for (int i = 0, minCount = Math.min(thisParamMultCount, otherParamMultCount); i < minCount; i++)
            {
                comparison = this.parameterMultiplicityMatches.get(i).compareTo(otherMatch.parameterMultiplicityMatches.get(i));
                if (comparison != 0)
                {
                    return comparison;
                }
            }
            comparison = Integer.compare(thisParamMultCount, otherParamMultCount);
            if (comparison != 0)
            {
                return comparison;
            }

            comparison = this.returnTypeMatch.compareTo(otherMatch.returnTypeMatch);
            if (comparison != 0)
            {
                return comparison;
            }

            return this.returnMultiplicityMatch.compareTo(otherMatch.returnMultiplicityMatch);
        }
    }

    private static class BottomTypeMatch extends TypeMatch
    {
        private BottomTypeMatch()
        {
        }

        @Override
        public int hashCode()
        {
            return BottomTypeMatch.class.hashCode();
        }

        @Override
        public boolean equals(Object other)
        {
            return (this == other) || (other instanceof BottomTypeMatch);
        }

        @Override
        public String toString()
        {
            return "<TypeMatch Nil>";
        }

        @Override
        public int compareTo(TypeMatch other)
        {
            if (this == other)
            {
                return 0;
            }

            return (other instanceof BottomTypeMatch) ? 0 : ((other instanceof NullTypeMatch) ? -1 : 1);
        }
    }

    private static class NullTypeMatch extends TypeMatch
    {
        private NullTypeMatch()
        {
        }

        @Override
        public int hashCode()
        {
            return NullTypeMatch.class.hashCode();
        }

        @Override
        public boolean equals(Object other)
        {
            return (this == other) || (other instanceof NullTypeMatch);
        }

        @Override
        public String toString()
        {
            return "<TypeMatch null>";
        }

        @Override
        public int compareTo(TypeMatch other)
        {
            return equals(other) ? 0 : 1;
        }
    }

    static TypeMatch newExactTypeMatch()
    {
        return EXACT_MATCH;
    }

    static TypeMatch newBottomTypeMatch()
    {
        return BOTTOM_TYPE_MATCH;
    }

    static TypeMatch newNonConcreteTypeMatch()
    {
        return NON_CONCRETE_MATCH;
    }

    static TypeMatch newNullTypeMatch()
    {
        return NULL_MATCH;
    }

    static TypeMatch newTypeMatch(CoreInstance targetType, CoreInstance valueType, boolean covariant, NullMatchBehavior valueNullMatchBehavior, ParameterMatchBehavior targetParameterMatchBehavior, ParameterMatchBehavior valueParameterMatchBehavior, ProcessorSupport processorSupport)
    {
        // Check null
        if (targetType == null)
        {
            throw new IllegalArgumentException("Target type cannot be null");
        }
        if (valueType == null)
        {
            switch (GenericTypeMatch.getNullMatchBehavior(valueNullMatchBehavior))
            {
                case MATCH_ANYTHING:
                {
                    return newNullTypeMatch();
                }
                case MATCH_NOTHING:
                {
                    return null;
                }
                case ERROR:
                {
                    throw new RuntimeException("Value type may not be null");
                }
                default:
                {
                    throw new RuntimeException("Should not be possible!");
                }
            }
        }

        if (targetType.equals(valueType))
        {
            return newExactTypeMatch();
        }

        CoreInstance superType = covariant ? targetType : valueType;
        CoreInstance subType = covariant ? valueType : targetType;

        if (Type.isBottomType(subType, processorSupport))
        {
            return newBottomTypeMatch();
        }

        if (FunctionType.isFunctionType(superType, processorSupport))
        {
            if (!FunctionType.isFunctionType(subType, processorSupport))
            {
                return null;
            }
            if (FunctionType.functionTypesEqual(subType, superType, processorSupport))
            {
                return newExactTypeMatch();
            }
            return newFunctionTypeMatch(targetType, valueType, covariant, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior, processorSupport);
        }

        // This is a hack because FunctionTypes do not currently have any generalizations
        if (FunctionType.isFunctionType(subType, processorSupport))
        {
            return Type.isTopType(superType, processorSupport) ? new SimpleTypeMatch(1) : null;
        }

        int distance = Type.getGeneralizationResolutionOrder(subType, processorSupport).indexOf(superType);
        switch (distance)
        {
            case -1:
            {
                return null;
            }
            case 0:
            {
                return newExactTypeMatch();
            }
            default:
            {
                return new SimpleTypeMatch(distance);
            }
        }
    }

    private static TypeMatch newFunctionTypeMatch(CoreInstance targetType, CoreInstance valueType, boolean covariant, NullMatchBehavior valueNullMatchBehavior, ParameterMatchBehavior targetParameterMatchBehavior, ParameterMatchBehavior valueParameterMatchBehavior, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> targetParameters = targetType.getValueForMetaPropertyToMany(M3Properties.parameters);
        ListIterable<? extends CoreInstance> valueParameters = valueType.getValueForMetaPropertyToMany(M3Properties.parameters);

        int parameterCount = targetParameters.size();
        if (parameterCount != valueParameters.size())
        {
            return null;
        }

        MutableList<GenericTypeMatch> paramTypeMatches = FastList.newList(parameterCount);
        MutableList<MultiplicityMatch> paramMultMatches = FastList.newList(parameterCount);
        for (int i = 0; i < parameterCount; i++)
        {
            CoreInstance paramParam = targetParameters.get(i);
            CoreInstance valueParam = valueParameters.get(i);

            CoreInstance paramParamType = paramParam.getValueForMetaPropertyToOne(M3Properties.genericType);
            CoreInstance valueParamType = valueParam.getValueForMetaPropertyToOne(M3Properties.genericType);
            GenericTypeMatch paramTypeMatch = GenericTypeMatch.newGenericTypeMatch(paramParamType, valueParamType, !covariant, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior, processorSupport);
            if (paramTypeMatch == null)
            {
                return null;
            }
            paramTypeMatches.add(paramTypeMatch);

            CoreInstance paramParamMult = Instance.getValueForMetaPropertyToOneResolved(paramParam, M3Properties.multiplicity, processorSupport);
            CoreInstance valueParamMult = Instance.getValueForMetaPropertyToOneResolved(valueParam, M3Properties.multiplicity, processorSupport);
            MultiplicityMatch paramMultMatch = MultiplicityMatch.newMultiplicityMatch(paramParamMult, valueParamMult, !covariant, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
            if (paramMultMatch == null)
            {
                return null;
            }
            paramMultMatches.add(paramMultMatch);
        }

        CoreInstance targetReturnType = targetType.getValueForMetaPropertyToOne(M3Properties.returnType);
        CoreInstance valueReturnType = valueType.getValueForMetaPropertyToOne(M3Properties.returnType);
        GenericTypeMatch returnTypeMatch = GenericTypeMatch.newGenericTypeMatch(targetReturnType, valueReturnType, covariant, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior, processorSupport);
        if (returnTypeMatch == null)
        {
            return null;
        }

        CoreInstance targetReturnMult = Instance.getValueForMetaPropertyToOneResolved(targetType, M3Properties.returnMultiplicity, processorSupport);
        CoreInstance valueReturnMult = Instance.getValueForMetaPropertyToOneResolved(valueType, M3Properties.returnMultiplicity, processorSupport);
        MultiplicityMatch returnMultMatch = MultiplicityMatch.newMultiplicityMatch(targetReturnMult, valueReturnMult, covariant, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        if (returnMultMatch == null)
        {
            return null;
        }

        return new FunctionTypeMatch(paramTypeMatches, paramMultMatches, returnTypeMatch, returnMultMatch);
    }
}
