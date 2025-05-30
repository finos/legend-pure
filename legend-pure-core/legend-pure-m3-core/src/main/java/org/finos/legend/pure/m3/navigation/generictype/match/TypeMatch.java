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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.FunctionType;
import org.finos.legend.pure.m3.navigation.multiplicity.MultiplicityMatch;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

abstract class TypeMatch implements Comparable<TypeMatch>
{
    private static final TypeMatch NULL_MATCH = new TypeMatch()
    {
        @Override
        public int compareTo(TypeMatch other)
        {
            return (this == other) ? 0 : 1;
        }

        @Override
        public String toString()
        {
            return "<TypeMatch null>";
        }
    };

    // Either type is non-concrete
    private static final TypeMatch NON_CONCRETE_MATCH = new TypeMatch()
    {
        @Override
        public int compareTo(TypeMatch other)
        {
            if (this == other)
            {
                return 0;
            }

            if (other instanceof SimpleTypeMatch)
            {
                return 1;
            }

            return -1;
        }

        @Override
        public String toString()
        {
            return "<TypeMatch non-concrete>";
        }
    };

    // Value type is bottom type
    private static final TypeMatch BOTTOM_TYPE_MATCH = new TypeMatch()
    {
        @Override
        public int compareTo(TypeMatch other)
        {
            if (this == other)
            {
                return 0;
            }

            if (other == NULL_MATCH)
            {
                return -1;
            }

            return 1;
        }

        @Override
        public String toString()
        {
            return "<TypeMatch Nil>";
        }
    };

    private TypeMatch()
    {
    }

    private static class SimpleTypeMatch extends TypeMatch
    {
        private static final SimpleTypeMatch EXACT_MATCH = new SimpleTypeMatch(0);

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
            return (this == other) || ((other instanceof SimpleTypeMatch) && (this.typeDistance == ((SimpleTypeMatch) other).typeDistance));
        }

        @Override
        public int compareTo(TypeMatch other)
        {
            if (this == other)
            {
                return 0;
            }

            if (other == NON_CONCRETE_MATCH)
            {
                return -1;
            }

            if (!(other instanceof SimpleTypeMatch))
            {
                return -1;
            }

            return Integer.compare(this.typeDistance, ((SimpleTypeMatch) other).typeDistance);
        }

        @Override
        public String toString()
        {
            return "<TypeMatch typeDistance=" + this.typeDistance + ">";
        }
    }

    private static class RelationTypeMatch extends TypeMatch
    {
        private final ImmutableList<MultiplicityMatch> columnsMultiplicityMatches;
        private final ImmutableList<GenericTypeMatch> columnsTypeMatches;

        private RelationTypeMatch(Iterable<? extends GenericTypeMatch> columnsTypeMatches, Iterable<? extends MultiplicityMatch> columnsMultiplicityMatches)
        {
            this.columnsTypeMatches = Lists.immutable.withAll(columnsTypeMatches);
            this.columnsMultiplicityMatches = Lists.immutable.withAll(columnsMultiplicityMatches);
        }

        @Override
        public int hashCode()
        {
            int hash = this.columnsTypeMatches.hashCode();
            return (43 * hash) + this.columnsMultiplicityMatches.hashCode();
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }

            if (!(other instanceof RelationTypeMatch))
            {
                return false;
            }

            RelationTypeMatch otherMatch = (RelationTypeMatch) other;
            return this.columnsTypeMatches.equals(otherMatch.columnsTypeMatches) &&
                    this.columnsMultiplicityMatches.equals(otherMatch.columnsMultiplicityMatches);
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder("<TypeMatch RelationType");
            this.columnsTypeMatches.appendString(builder, " columnsTypeMatches=[", ", ", "]");
            this.columnsMultiplicityMatches.appendString(builder, " columnsMultiplicityMatches=[", ", ", "]");
            return builder.append('>').toString();
        }

        @Override
        public int compareTo(TypeMatch other)
        {
            if (this == other)
            {
                return 0;
            }

            if ((other == NON_CONCRETE_MATCH) || (other instanceof SimpleTypeMatch))
            {
                return 1;
            }

            if (!(other instanceof RelationTypeMatch))
            {
                return -1;
            }

            RelationTypeMatch otherMatch = (RelationTypeMatch) other;
            int comparison = GenericTypeMatch.compareMatchLists(this.columnsTypeMatches, otherMatch.columnsTypeMatches);
            if (comparison != 0)
            {
                return comparison;
            }

            return GenericTypeMatch.compareMatchLists(this.columnsMultiplicityMatches, otherMatch.columnsMultiplicityMatches);
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
            int hash = this.parameterTypeMatches.hashCode();
            hash = (43 * hash) + this.parameterMultiplicityMatches.hashCode();
            hash = (43 * hash) + this.returnTypeMatch.hashCode();
            return (43 * hash) + this.returnMultiplicityMatch.hashCode();
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

            FunctionTypeMatch otherMatch = (FunctionTypeMatch) other;
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
            builder.append(" returnTypeMatch=").append(this.returnTypeMatch);
            builder.append(" returnMultiplicityMatch=").append(this.returnMultiplicityMatch);
            return builder.append('>').toString();
        }

        @Override
        public int compareTo(TypeMatch other)
        {
            if (this == other)
            {
                return 0;
            }

            if ((other == NON_CONCRETE_MATCH) || (other instanceof SimpleTypeMatch))
            {
                return 1;
            }

            if (!(other instanceof FunctionTypeMatch))
            {
                return -1;
            }

            FunctionTypeMatch otherMatch = (FunctionTypeMatch) other;
            int comparison;

            comparison = GenericTypeMatch.compareMatchLists(this.parameterTypeMatches, otherMatch.parameterTypeMatches);
            if (comparison != 0)
            {
                return comparison;
            }

            comparison = GenericTypeMatch.compareMatchLists(this.parameterMultiplicityMatches, otherMatch.parameterMultiplicityMatches);
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

    static TypeMatch newExactTypeMatch()
    {
        return SimpleTypeMatch.EXACT_MATCH;
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

        if (_RelationType.isRelationType(superType, processorSupport) && _RelationType.isRelationType(subType, processorSupport))
        {
            return newRelationTypeMatch((RelationType<?>) subType, (RelationType<?>) superType, covariant, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior, processorSupport);
        }

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

    private static TypeMatch newRelationTypeMatch(RelationType<?> candidate, RelationType<?> signature, boolean covariant, NullMatchBehavior valueNullMatchBehavior, ParameterMatchBehavior targetParameterMatchBehavior, ParameterMatchBehavior valueParameterMatchBehavior, ProcessorSupport processorSupport)
    {
        if (_RelationType.equalRelationType(candidate, signature, processorSupport))
        {
            return newExactTypeMatch();
        }

        RichIterable<? extends Column<?, ?>> candidateColumns = candidate._columns();
        RichIterable<? extends Column<?, ?>> signatureColumns = signature._columns();

        if (candidateColumns.size() < signatureColumns.size())
        {
            return null;
        }

        Pair<ListIterable<? extends Column<?, ?>>, ListIterable<? extends Column<?, ?>>> res = _RelationType.alignColumnSets(candidateColumns, signatureColumns, processorSupport);
        ListIterable<? extends Column<?, ?>> sortedCandidateSub = res.getOne();
        ListIterable<? extends Column<?, ?>> sortedSignatures = res.getTwo();

        int count = sortedSignatures.size();
        if (count != sortedCandidateSub.size())
        {
            return null;
        }

        MutableList<GenericTypeMatch> columnsTypeMatches = Lists.mutable.ofInitialCapacity(count);
        MutableList<MultiplicityMatch> columnsMultMatches = Lists.mutable.ofInitialCapacity(count);
        for (int i = 0; i < count; i++)
        {
            CoreInstance candidateColumn = sortedCandidateSub.get(i);
            CoreInstance signatureColumn = sortedSignatures.get(i);

            CoreInstance candidateColumnType = candidateColumn.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToMany(M3Properties.typeArguments).get(1);
            CoreInstance signatureColumnType = signatureColumn.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToMany(M3Properties.typeArguments).get(1);

            GenericTypeMatch paramTypeMatch = GenericTypeMatch.newGenericTypeMatch(signatureColumnType, candidateColumnType, covariant, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior, processorSupport);
            if (paramTypeMatch == null)
            {
                return null;
            }
            columnsTypeMatches.add(paramTypeMatch);

            CoreInstance candidateMult = Instance.getValueForMetaPropertyToOneResolved(candidateColumn, M3Properties.classifierGenericType, processorSupport).getValueForMetaPropertyToMany(M3Properties.multiplicityArguments).getFirst();
            CoreInstance signatureMult = Instance.getValueForMetaPropertyToOneResolved(signatureColumn, M3Properties.classifierGenericType, processorSupport).getValueForMetaPropertyToMany(M3Properties.multiplicityArguments).getFirst();
            MultiplicityMatch paramMultMatch = MultiplicityMatch.newMultiplicityMatch(signatureMult, candidateMult, covariant, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
            if (paramMultMatch == null)
            {
                return null;
            }
            columnsMultMatches.add(paramMultMatch);
        }
        return new RelationTypeMatch(columnsTypeMatches, columnsMultMatches);
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

        MutableList<GenericTypeMatch> paramTypeMatches = Lists.mutable.ofInitialCapacity(parameterCount);
        MutableList<MultiplicityMatch> paramMultMatches = Lists.mutable.ofInitialCapacity(parameterCount);
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
