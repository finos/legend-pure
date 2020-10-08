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

package org.finos.legend.pure.m3.navigation.multiplicity;

import org.finos.legend.pure.m3.navigation.generictype.match.NullMatchBehavior;
import org.finos.legend.pure.m3.navigation.generictype.match.ParameterMatchBehavior;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public abstract class MultiplicityMatch implements Comparable<MultiplicityMatch>
{
    private static final MultiplicityMatch EXACT_MATCH = new SimpleMultiplicityMatch(0, 0);
    private static final MultiplicityMatch NON_CONCRETE_MATCH = new NonConcreteMultiplicityMatch();
    private static final MultiplicityMatch NULL_MATCH = new NullMultiplicityMatch();

    private MultiplicityMatch()
    {
    }

    private static class SimpleMultiplicityMatch extends MultiplicityMatch
    {
        private final int lowerBoundDistance;
        private final int upperBoundDistance;

        private SimpleMultiplicityMatch(int lowerBoundDistance, int upperBoundDistance)
        {
            this.lowerBoundDistance = lowerBoundDistance;
            this.upperBoundDistance = upperBoundDistance;
        }

        @Override
        public int hashCode()
        {
            return this.lowerBoundDistance ^ this.upperBoundDistance;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }

            if (!(other instanceof SimpleMultiplicityMatch))
            {
                return false;
            }

            SimpleMultiplicityMatch otherMatch = (SimpleMultiplicityMatch)other;
            return (this.lowerBoundDistance == otherMatch.lowerBoundDistance) &&
                    (this.upperBoundDistance == otherMatch.upperBoundDistance);
        }

        @Override
        public String toString()
        {
            return "<MultiplicityMatch lowerBoundDistance=" + this.lowerBoundDistance + " upperBoundDistance=" + this.upperBoundDistance + ">";
        }

        @Override
        public int compareTo(MultiplicityMatch other)
        {
            if (this == other)
            {
                return 0;
            }

            if (other instanceof NullMultiplicityMatch)
            {
                return -1;
            }

            if (other instanceof NonConcreteMultiplicityMatch)
            {
                return ((this.lowerBoundDistance == 0) && (this.upperBoundDistance == 0)) ? -1 : 1;
            }

            SimpleMultiplicityMatch otherSimpleMatch = (SimpleMultiplicityMatch)other;
            int comparison = Integer.compare(this.upperBoundDistance, otherSimpleMatch.upperBoundDistance);
            return (comparison == 0) ? Integer.compare(this.lowerBoundDistance, otherSimpleMatch.lowerBoundDistance) : comparison;
        }
    }

    private static class NonConcreteMultiplicityMatch extends MultiplicityMatch
    {
        private NonConcreteMultiplicityMatch()
        {
        }

        @Override
        public int hashCode()
        {
            return NonConcreteMultiplicityMatch.class.hashCode();
        }

        @Override
        public boolean equals(Object other)
        {
            return (this == other) || (other instanceof NonConcreteMultiplicityMatch);
        }

        @Override
        public String toString()
        {
            return "<MultiplicityMatch non-concrete>";
        }

        @Override
        public int compareTo(MultiplicityMatch other)
        {
            if (this == other)
            {
                return 0;
            }

            if (other instanceof NullMultiplicityMatch)
            {
                return -1;
            }

            if (other instanceof NonConcreteMultiplicityMatch)
            {
                return 0;
            }

            SimpleMultiplicityMatch otherMatch = (SimpleMultiplicityMatch)other;
            return ((otherMatch.lowerBoundDistance == 0) && (otherMatch.upperBoundDistance == 0)) ? 1 : -1;
        }
    }

    private static class NullMultiplicityMatch extends MultiplicityMatch
    {
        private NullMultiplicityMatch()
        {
        }

        @Override
        public int hashCode()
        {
            return NullMultiplicityMatch.class.hashCode();
        }

        @Override
        public boolean equals(Object other)
        {
            return (this == other) || (other instanceof NullMultiplicityMatch);
        }

        @Override
        public String toString()
        {
            return "<MultiplicityMatch null>";
        }

        @Override
        public int compareTo(MultiplicityMatch other)
        {
            return equals(other) ? 0 : 1;
        }
    }

    private static MultiplicityMatch newExactMultiplicityMatch()
    {
        return EXACT_MATCH;
    }

    private static MultiplicityMatch newNonConcreteMultiplicityMatch()
    {
        return NON_CONCRETE_MATCH;
    }

    private static MultiplicityMatch newNullMultiplicityMatch()
    {
        return NULL_MATCH;
    }

    public static boolean multiplicityMatches(CoreInstance targetMultiplicity, CoreInstance valueMultiplicity, boolean covariant)
    {
        return multiplicityMatches(targetMultiplicity, valueMultiplicity, covariant, null, null, null);
    }

    public static boolean multiplicityMatches(CoreInstance targetMultiplicity, CoreInstance valueMultiplicity, boolean covariant, NullMatchBehavior valueNullMatchBehavior, ParameterMatchBehavior targetParameterMatchBehavior, ParameterMatchBehavior valueParameterMatchBehavior)
    {
        return newMultiplicityMatch(targetMultiplicity, valueMultiplicity, covariant, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior) != null;
    }

    public static MultiplicityMatch newMultiplicityMatch(CoreInstance targetMultiplicity, CoreInstance valueMultiplicity, boolean covariant, NullMatchBehavior valueNullMatchBehavior, ParameterMatchBehavior targetParameterMatchBehavior, ParameterMatchBehavior valueParameterMatchBehavior)
    {
        // Null checks
        if (targetMultiplicity == null)
        {
            throw new IllegalArgumentException("Target multiplicity cannot be null");
        }
        if (valueMultiplicity == null)
        {
            switch (getNullMatchBehavior(valueNullMatchBehavior))
            {
                case MATCH_ANYTHING:
                {
                    return newNullMultiplicityMatch();
                }
                case MATCH_NOTHING:
                {
                    return null;
                }
                case ERROR:
                {
                    throw new RuntimeException("Value multiplicity may not be null");
                }
            }
        }

        // Identical multiplicities
        if (targetMultiplicity == valueMultiplicity)
        {
            return newExactMultiplicityMatch();
        }

        // Target multiplicity is not concrete
        if (!Multiplicity.isMultiplicityConcrete(targetMultiplicity))
        {
            switch (getParameterMatchBehavior(targetParameterMatchBehavior))
            {
                case MATCH_ANYTHING:
                {
                    return newNonConcreteMultiplicityMatch();
                }
                case MATCH_CAUTIOUSLY:
                {
                    if (Multiplicity.isMultiplicityConcrete(valueMultiplicity))
                    {
                        if (!covariant && (Multiplicity.concreteMultiplicityLowerBoundToInt(targetMultiplicity) == 0) && (Multiplicity.concreteMultiplicityUpperBoundToInt(targetMultiplicity) == -1))
                        {
                            return newNonConcreteMultiplicityMatch();
                        }
                    }
                    else
                    {
                        String targetParam = Multiplicity.getMultiplicityParameter(targetMultiplicity);
                        if ((targetParam != null) && targetParam.equals(Multiplicity.getMultiplicityParameter(valueMultiplicity)))
                        {
                            return newNonConcreteMultiplicityMatch();
                        }
                    }
                    return null;
                }
                case MATCH_NOTHING:
                {
                    return null;
                }
                case ERROR:
                {
                    throw new RuntimeException("Target multiplicity must be concrete, got: " + Multiplicity.print(targetMultiplicity));
                }
            }
        }

        // Value multiplicity is not concrete (but target multiplicity is)
        if (!Multiplicity.isMultiplicityConcrete(valueMultiplicity))
        {
            switch (getParameterMatchBehavior(valueParameterMatchBehavior))
            {
                case MATCH_ANYTHING:
                {
                    return newNonConcreteMultiplicityMatch();
                }
                case MATCH_CAUTIOUSLY:
                {
                    if (covariant && (Multiplicity.concreteMultiplicityLowerBoundToInt(targetMultiplicity) == 0) && (Multiplicity.concreteMultiplicityUpperBoundToInt(targetMultiplicity) == -1))
                    {
                        return new SimpleMultiplicityMatch(Integer.MAX_VALUE, Integer.MAX_VALUE);
                    }
                    return null;
                }
                case MATCH_NOTHING:
                {
                    return null;
                }
                case ERROR:
                {
                    throw new RuntimeException("Value multiplicity must be concrete, got: " + Multiplicity.print(valueMultiplicity));
                }
            }
        }

        // Both multiplicities are concrete
        CoreInstance largeMultiplicity;
        CoreInstance smallMultiplicity;
        if (covariant)
        {
            largeMultiplicity = targetMultiplicity;
            smallMultiplicity = valueMultiplicity;
        }
        else
        {
            largeMultiplicity = valueMultiplicity;
            smallMultiplicity = targetMultiplicity;
        }

        int largeLowerBound = Multiplicity.concreteMultiplicityLowerBoundToInt(largeMultiplicity);
        int smallLowerBound = Multiplicity.concreteMultiplicityLowerBoundToInt(smallMultiplicity);
        int lowerBoundDistance = smallLowerBound - largeLowerBound;
        if (lowerBoundDistance < 0)
        {
            return null;
        }

        int largeUpperBound = Multiplicity.concreteMultiplicityUpperBoundToInt(largeMultiplicity);
        int smallUpperBound = Multiplicity.concreteMultiplicityUpperBoundToInt(smallMultiplicity);
        int upperBoundDistance;
        if (largeUpperBound < 0)
        {
            upperBoundDistance = (smallUpperBound < 0) ? 0 : Integer.MAX_VALUE;
        }
        else if (smallUpperBound < 0)
        {
            return null;
        }
        else
        {
            upperBoundDistance = largeUpperBound - smallUpperBound;
            if (upperBoundDistance < 0)
            {
                return null;
            }
        }

        return ((lowerBoundDistance == 0) && (upperBoundDistance == 0)) ? newExactMultiplicityMatch() : new SimpleMultiplicityMatch(lowerBoundDistance, upperBoundDistance);
    }

    private static NullMatchBehavior getNullMatchBehavior(NullMatchBehavior nullMatchBehavior)
    {
        return (nullMatchBehavior == null) ? NullMatchBehavior.ERROR : nullMatchBehavior;
    }

    private static ParameterMatchBehavior getParameterMatchBehavior(ParameterMatchBehavior parameterMatchBehavior)
    {
        return (parameterMatchBehavior == null) ? ParameterMatchBehavior.ERROR : parameterMatchBehavior;
    }
}
