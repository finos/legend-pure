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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.generictype.GenericTypeWithXArguments;
import org.finos.legend.pure.m3.navigation.multiplicity.MultiplicityMatch;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class GenericTypeMatch implements Comparable<GenericTypeMatch>
{
    private static final GenericTypeMatch EXACT_MATCH = new GenericTypeMatch(TypeMatch.newExactTypeMatch());
    private static final GenericTypeMatch NON_CONCRETE_MATCH = new GenericTypeMatch(TypeMatch.newNonConcreteTypeMatch());
    private static final GenericTypeMatch NULL_MATCH = new GenericTypeMatch(TypeMatch.newNullTypeMatch());

    private final TypeMatch rawTypeMatch;
    private final ImmutableList<GenericTypeMatch> typeArgumentMatches;
    private final ImmutableList<MultiplicityMatch> multiplicityArgumentMatches;

    private GenericTypeMatch(TypeMatch rawTypeMatch, Iterable<? extends GenericTypeMatch> typeArgumentMatches, Iterable<? extends MultiplicityMatch> multiplicityArgumentMatches)
    {
        this.rawTypeMatch = rawTypeMatch;
        this.typeArgumentMatches = (typeArgumentMatches == null) ? Lists.immutable.empty() : Lists.immutable.withAll(typeArgumentMatches);
        this.multiplicityArgumentMatches = (multiplicityArgumentMatches == null) ? Lists.immutable.empty() : Lists.immutable.withAll(multiplicityArgumentMatches);
    }

    private GenericTypeMatch(TypeMatch rawTypeMatch)
    {
        this(rawTypeMatch, null, null);
    }

    @Override
    public int hashCode()
    {
        return this.rawTypeMatch.hashCode() + 47 * (this.typeArgumentMatches.hashCode() + (47 * this.multiplicityArgumentMatches.hashCode()));
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof GenericTypeMatch))
        {
            return false;
        }

        GenericTypeMatch otherMatch = (GenericTypeMatch) other;
        return this.rawTypeMatch.equals(otherMatch.rawTypeMatch) &&
                this.typeArgumentMatches.equals(otherMatch.typeArgumentMatches) &&
                this.multiplicityArgumentMatches.equals(otherMatch.multiplicityArgumentMatches);
    }

    @Override
    public int compareTo(GenericTypeMatch other)
    {
        // Compare raw type matches
        int compare = this.rawTypeMatch.compareTo(other.rawTypeMatch);
        if (compare != 0)
        {
            return compare;
        }

        // Compare type parameter matches
        compare = compareMatchLists(this.typeArgumentMatches, other.typeArgumentMatches);
        if (compare != 0)
        {
            return compare;
        }

        // Compare multiplicity parameter matches
        return compareMatchLists(this.multiplicityArgumentMatches, other.multiplicityArgumentMatches);
    }

    static <T extends Comparable<T>> int compareMatchLists(ListIterable<T> these, ListIterable<T> those)
    {
        int theseCount = these.size();
        int thoseCount = those.size();
        for (int i = 0, minCount = Math.min(theseCount, thoseCount); i < minCount; i++)
        {
            int compare = these.get(i).compareTo(those.get(i));
            if (compare != 0)
            {
                return compare;
            }
        }
        return Integer.compare(theseCount, thoseCount);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("<GenericTypeMatch rawTypeMatch=").append(this.rawTypeMatch);
        this.typeArgumentMatches.appendString(builder, " typeArgumentMatches=[", ", ", "]");
        this.multiplicityArgumentMatches.appendString(builder, " multiplicityParameters=[", ", ", "]>");
        return builder.toString();
    }

    public static boolean genericTypeMatches(CoreInstance targetGenericType, CoreInstance valueGenericType, boolean covariant, ParameterMatchBehavior targetParameterMatchBehavior, ParameterMatchBehavior valueParameterMatchBehavior, ProcessorSupport processorSupport)
    {
        return genericTypeMatches(targetGenericType, valueGenericType, covariant, null, targetParameterMatchBehavior, valueParameterMatchBehavior, processorSupport);
    }

    public static boolean genericTypeMatches(CoreInstance targetGenericType, CoreInstance valueGenericType, boolean covariant, NullMatchBehavior valueNullMatchBehavior, ParameterMatchBehavior targetParameterMatchBehavior, ParameterMatchBehavior valueParameterMatchBehavior, ProcessorSupport processorSupport)
    {
        return newGenericTypeMatch(targetGenericType, valueGenericType, covariant, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior, processorSupport) != null;
    }

    public static GenericTypeMatch newGenericTypeMatch(CoreInstance targetGenericType, CoreInstance valueGenericType, boolean covariant, NullMatchBehavior valueNullMatchBehavior, ParameterMatchBehavior targetParameterMatchBehavior, ParameterMatchBehavior valueParameterMatchBehavior, ProcessorSupport processorSupport)
    {
        // Null checks
        if (targetGenericType == null)
        {
            throw new IllegalArgumentException("Target generic type may not be null");
        }
        if (valueGenericType == null)
        {
            switch (getNullMatchBehavior(valueNullMatchBehavior))
            {
                case MATCH_ANYTHING:
                {
                    return NULL_MATCH;
                }
                case MATCH_NOTHING:
                {
                    return null;
                }
                case ERROR:
                {
                    throw new RuntimeException("Value generic type may not be null");
                }
            }
        }

        // Identical generic types
        if (GenericType.genericTypesEqual(targetGenericType, valueGenericType, processorSupport))
        {
            return EXACT_MATCH;
        }

        CoreInstance targetRawType = Instance.getValueForMetaPropertyToOneResolved(targetGenericType, M3Properties.rawType, processorSupport);
        CoreInstance valueRawType = Instance.getValueForMetaPropertyToOneResolved(valueGenericType, M3Properties.rawType, processorSupport);

        // Target generic type is not concrete (i.e., targetRawType == null)
        if (targetRawType == null)
        {
            switch (getParameterMatchBehavior(targetParameterMatchBehavior))
            {
                case MATCH_ANYTHING:
                {
                    return NON_CONCRETE_MATCH;
                }
                case MATCH_CAUTIOUSLY:
                {
                    if (valueRawType == null)
                    {
                        String targetTypeParameterName = GenericType.getTypeParameterName(targetGenericType);
                        String valueTypeParameterName = GenericType.getTypeParameterName(valueGenericType);
                        return (targetTypeParameterName != null) && targetTypeParameterName.equals(valueTypeParameterName) ? NON_CONCRETE_MATCH : null;
                    }
                    boolean willMatchParameterBinding = covariant ? Type.isBottomType(valueRawType, processorSupport) : Type.isTopType(valueRawType, processorSupport);
                    return willMatchParameterBinding ? NON_CONCRETE_MATCH : null;
                }
                case MATCH_NOTHING:
                {
                    return null;
                }
                case ERROR:
                {
                    throw new RuntimeException("Target generic type must be concrete, got: " + GenericType.print(targetGenericType, processorSupport));
                }
                default:
                {
                    throw new RuntimeException("Should not be possible");
                }
            }
        }

        // If the value generic type is not concrete (i.e., valueRawType == null), then it only matches Any (for covariant) or Nil (for contravariant)
        if (valueRawType == null)
        {
            switch (getParameterMatchBehavior(valueParameterMatchBehavior))
            {
                case MATCH_ANYTHING:
                {
                    return NON_CONCRETE_MATCH;
                }
                case MATCH_CAUTIOUSLY:
                {
                    boolean anyParameterBindingWillMatch = covariant ? Type.isTopType(targetRawType, processorSupport) : Type.isBottomType(targetRawType, processorSupport);
                    return anyParameterBindingWillMatch ? NON_CONCRETE_MATCH : null;
                }
                case MATCH_NOTHING:
                {
                    return null;
                }
                case ERROR:
                {
                    throw new RuntimeException("Value generic type must be concrete, got: " + GenericType.print(valueGenericType, processorSupport));
                }
            }
        }

        // Both generic types are concrete, so we calculate a match between the raw types
        TypeMatch rawTypeMatch = TypeMatch.newTypeMatch(targetRawType, valueRawType, covariant, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior, processorSupport);
        if (rawTypeMatch == null)
        {
            return null;
        }
        if (Type.isBottomType(covariant ? valueRawType : targetRawType, processorSupport) || Type.isTopType(covariant ? targetRawType : valueRawType, processorSupport))
        {
            return new GenericTypeMatch(rawTypeMatch);
        }

        ListIterable<? extends CoreInstance> targetTypeArguments = targetGenericType.getValueForMetaPropertyToMany(M3Properties.typeArguments);
        int typeParameterCount = targetTypeArguments.size();
        MutableList<GenericTypeMatch> typeArgumentMatches = Lists.mutable.ofInitialCapacity(typeParameterCount);

        if (typeParameterCount > 0)
        {
            GenericTypeWithXArguments homogenizedTypeArguments = GenericType.resolveClassTypeParameterUsingInheritance(valueGenericType, targetGenericType, processorSupport);
            if (homogenizedTypeArguments == null)
            {
                return null;
            }
            ImmutableMap<String, CoreInstance> valueTypeArgumentsByParam = homogenizedTypeArguments.getArgumentsByParameterName();
            if (typeParameterCount != valueTypeArgumentsByParam.size())
            {
                return null;
            }

            ListIterable<? extends CoreInstance> typeParameters = targetRawType.getValueForMetaPropertyToMany(M3Properties.typeParameters);
            for (int i = 0; i < typeParameterCount; i++)
            {
                CoreInstance typeParameter = typeParameters.get(i);
                boolean contravariant = PrimitiveUtilities.getBooleanValue(typeParameter.getValueForMetaPropertyToOne(M3Properties.contravariant), false);
                boolean paramCovariant = contravariant ? !covariant : covariant;
                CoreInstance valueTypeArgument = valueTypeArgumentsByParam.get(PrimitiveUtilities.getStringValue(typeParameter.getValueForMetaPropertyToOne(M3Properties.name)));
                GenericTypeMatch typeArgumentMatch = newGenericTypeMatch(targetTypeArguments.get(i), valueTypeArgument, paramCovariant, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior, processorSupport);
                if (typeArgumentMatch == null)
                {
                    return null;
                }
                typeArgumentMatches.add(typeArgumentMatch);
            }
        }

        ListIterable<? extends CoreInstance> targetMultArguments = Instance.getValueForMetaPropertyToManyResolved(targetGenericType, M3Properties.multiplicityArguments, processorSupport);
        int multParameterCount = targetMultArguments.size();
        MutableList<MultiplicityMatch> multArgumentMatches = Lists.mutable.ofInitialCapacity(multParameterCount);

        if (multParameterCount > 0)
        {
            GenericTypeWithXArguments homogenizedMultArguments = GenericType.resolveClassMultiplicityParameterUsingInheritance(valueGenericType, targetRawType, processorSupport);
            if (homogenizedMultArguments == null)
            {
                return null;
            }
            ListIterable<CoreInstance> valueMultArguments = homogenizedMultArguments.extractArgumentsAsMultiplicityParameters(processorSupport);
            if (multParameterCount != valueMultArguments.size())
            {
                return null;
            }

            for (int i = 0; i < multParameterCount; i++)
            {
                MultiplicityMatch multArgumentMatch = MultiplicityMatch.newMultiplicityMatch(targetMultArguments.get(i), valueMultArguments.get(i), covariant, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
                if (multArgumentMatch == null)
                {
                    return null;
                }
                multArgumentMatches.add(multArgumentMatch);
            }
        }
        return new GenericTypeMatch(rawTypeMatch, typeArgumentMatches, multArgumentMatches);
    }

    static NullMatchBehavior getNullMatchBehavior(NullMatchBehavior nullMatchBehavior)
    {
        return (nullMatchBehavior == null) ? NullMatchBehavior.ERROR : nullMatchBehavior;
    }

    static ParameterMatchBehavior getParameterMatchBehavior(ParameterMatchBehavior parameterMatchBehavior)
    {
        return (parameterMatchBehavior == null) ? ParameterMatchBehavior.ERROR : parameterMatchBehavior;
    }
}
