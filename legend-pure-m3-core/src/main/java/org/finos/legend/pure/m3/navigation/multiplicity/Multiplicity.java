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

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.tools.SafeAppendable;

import java.util.Iterator;
import java.util.Objects;

public class Multiplicity
{
    /**
     * Return whether multiplicity is concrete.  That is, it has
     * an actual range rather than simply a parameter.
     *
     * @param multiplicity multiplicity
     * @return whether multiplicity is concrete
     */
    public static boolean isMultiplicityConcrete(CoreInstance multiplicity)
    {
        return (multiplicity != null) && (multiplicity.getValueForMetaPropertyToOne(M3Properties.multiplicityParameter) == null);
    }

    /**
     * Get the parameter name for a non-concrete multiplicity.
     *
     * @param multiplicity non-concrete multiplicity
     * @return multiplicity parameter name
     */
    public static String getMultiplicityParameter(CoreInstance multiplicity)
    {
        CoreInstance parameter = Objects.requireNonNull(multiplicity).getValueForMetaPropertyToOne(M3Properties.multiplicityParameter);
        return (parameter == null) ? null : PrimitiveUtilities.getStringValue(parameter);
    }

    /**
     * Return whether a multiplicity requires exactly one value.  This
     * is equivalent to calling isToOne(multiplicity, true).
     *
     * @param multiplicity multiplicity
     * @return whether multiplicity requires exactly one
     */
    public static boolean isToOne(CoreInstance multiplicity)
    {
        return isToOne(multiplicity, true);
    }

    /**
     * Return whether a multiplicity is "to one".  If strict is true,
     * then "to one" means that both the lower and upper bound are 1.
     * Otherwise, it is sufficient for the upper bound to be 1.
     *
     * @param multiplicity multiplicity
     * @param strict       whether to interpret "to one" strictly
     * @return whether multiplicity is "to one"
     */
    public static boolean isToOne(CoreInstance multiplicity, boolean strict)
    {
        return isMultiplicityConcrete(multiplicity) &&
                (!strict || (concreteMultiplicityLowerBoundToInt(multiplicity) == 1)) &&
                (concreteMultiplicityUpperBoundToInt(multiplicity) == 1);
    }

    public static boolean isToZeroOrOne(CoreInstance multiplicity)
    {
        return isMultiplicityConcrete(multiplicity) && (concreteMultiplicityUpperBoundToInt(multiplicity) == 0 || concreteMultiplicityUpperBoundToInt(multiplicity) == 1);
    }

    public static boolean isToZero(CoreInstance multiplicity)
    {
        return isMultiplicityConcrete(multiplicity) && (concreteMultiplicityUpperBoundToInt(multiplicity) == 0);
    }

    /**
     * Get the lower bound of the given multiplicity as an int.  The int
     * is guaranteed to be non-negative.  If the multiplicity is not
     * concrete, 0 is returned.
     *
     * @param multiplicity concrete multiplicity
     * @return multiplicity lower bound
     */
    public static int multiplicityLowerBoundToInt(CoreInstance multiplicity)
    {
        return isMultiplicityConcrete(multiplicity) ? concreteMultiplicityLowerBoundToInt(multiplicity) : 0;
    }

    /**
     * Get the upper bound of the given multiplicity as an int.  If the
     * multiplicity has no upper bound (including the case where it is
     * not concrete), -1 is returned.  Otherwise, the int is guaranteed
     * to be non-negative.
     *
     * @param multiplicity multiplicity
     * @return multiplicity upper bound, or -1 if there is no upper bound
     */
    public static int multiplicityUpperBoundToInt(CoreInstance multiplicity)
    {
        return isMultiplicityConcrete(multiplicity) ? concreteMultiplicityUpperBoundToInt(multiplicity) : -1;
    }

    /**
     * Return a human readable string representation of a multiplicity.
     * If multiplicity is non-concrete, its parameter name is used.
     *
     * @param multiplicity multiplicity
     * @return human readable string representation of multiplicity
     */
    public static String print(CoreInstance multiplicity)
    {
        return print(multiplicity, true);
    }

    /**
     * Return a human readable string representation of a multiplicity.
     * If multiplicity is non-concrete, its parameter name is used.
     *
     * @param multiplicity  multiplicity
     * @param printBrackets whether to print square brackets around the multiplicity
     * @return human readable string representation of multiplicity
     */
    public static String print(CoreInstance multiplicity, boolean printBrackets)
    {
        return print(new StringBuilder(8), multiplicity, printBrackets).toString();
    }

    /**
     * Print a human readable string representation of a multiplicity to
     * an appendable.  If multiplicity is non-concrete, its parameter
     * name is used.
     *
     * @param appendable    appendable to print to
     * @param multiplicity  multiplicity
     * @param printBrackets whether to print square brackets around the multiplicity
     * @return the appendable
     */
    public static <T extends Appendable> T print(T appendable, CoreInstance multiplicity, boolean printBrackets)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        if (printBrackets)
        {
            safeAppendable.append('[');
        }
        if (multiplicity == null)
        {
            safeAppendable.append("NULL");
        }
        else if (isMultiplicityConcrete(multiplicity))
        {
            int lower = concreteMultiplicityLowerBoundToInt(multiplicity);
            int upper = concreteMultiplicityUpperBoundToInt(multiplicity);
            if (upper < 0)
            {
                if (lower == 0)
                {
                    safeAppendable.append('*');
                }
                else
                {
                    safeAppendable.append(lower).append("..*");
                }
            }
            else
            {
                safeAppendable.append(lower);
                if (upper != lower)
                {
                    safeAppendable.append("..").append(upper);
                }
            }
        }
        else
        {
            safeAppendable.append(getMultiplicityParameter(multiplicity));
        }
        if (printBrackets)
        {
            safeAppendable.append(']');
        }
        return appendable;
    }

    /**
     * Return a string representation of a multiplicity suitable for
     * use in a Pure function signature.
     *
     * @param multiplicity multiplicity
     * @return string representation of multiplicity for a function signature
     */
    public static String multiplicityToSignatureString(CoreInstance multiplicity)
    {
        if (isMultiplicityConcrete(multiplicity))
        {
            int lower = concreteMultiplicityLowerBoundToInt(multiplicity);
            int upper = concreteMultiplicityUpperBoundToInt(multiplicity);
            if (upper == -1)
            {
                return (lower == 0) ? "_MANY_" : ("_$" + lower + "_MANY$_");
            }
            else if (lower == upper)
            {
                return "_" + lower + "_";
            }
            else
            {
                return "_$" + lower + "_" + upper + "$_";
            }
        }
        else
        {
            return "_" + getMultiplicityParameter(multiplicity) + "_";
        }
    }

    /**
     * Make the given multiplicity as concrete as possible.  If multiplicity
     * is already concrete, it is simply returned.  Otherwise, its multiplicity
     * parameter is looked up in resolvedTypeAndMultiplicityParams (which should
     * be a map from parameters to concrete values).  If it is not present, then
     * multiplicity itself is returned.
     *
     * @param multiplicity                      multiplicity
     * @param resolvedTypeAndMultiplicityParams map of type and multiplicity parameters that have been resolved
     * @return concrete multiplicity if possible
     */
    public static CoreInstance makeMultiplicityAsConcreteAsPossible(CoreInstance multiplicity, MapIterable<String, CoreInstance> resolvedTypeAndMultiplicityParams)
    {
        if (multiplicity == null)
        {
            return null;
        }
        return isMultiplicityConcrete(multiplicity) ? multiplicity : resolvedTypeAndMultiplicityParams.getIfAbsentValue(getMultiplicityParameter(multiplicity), multiplicity);
    }

    /**
     * Create a new concrete multiplicity instance from a lower and upper bound.
     * The lower bound must be a non-negative integer.  A negative value for the
     * upper bound indicates that there is no upper bound.  Otherwise, the
     * upper bound must be no less than the lower bound.
     *
     * @param lowerBound       multiplicity lower bound (must be non-negative)
     * @param upperBound       multiplicity upper bound (negative indicates no upper bound)
     * @param processorSupport processor support
     * @return new concrete multiplicity
     */
    public static CoreInstance newMultiplicity(int lowerBound, int upperBound, ProcessorSupport processorSupport)
    {
        // Check for defined multiplicities
        if (lowerBound == 0)
        {
            if (upperBound < 0)
            {
                CoreInstance multiplicity = processorSupport.package_getByUserPath(M3Paths.ZeroMany);
                if (multiplicity != null)
                {
                    return multiplicity;
                }
            }
            else if (upperBound == 0)
            {
                CoreInstance multiplicity = processorSupport.package_getByUserPath(M3Paths.PureZero);
                if (multiplicity != null)
                {
                    return multiplicity;
                }
            }
            else if (upperBound == 1)
            {
                CoreInstance multiplicity = processorSupport.package_getByUserPath(M3Paths.ZeroOne);
                if (multiplicity != null)
                {
                    return multiplicity;
                }
            }
        }
        else if (lowerBound == 1)
        {
            if (upperBound < 0)
            {
                CoreInstance multiplicity = processorSupport.package_getByUserPath(M3Paths.OneMany);
                if (multiplicity != null)
                {
                    return multiplicity;
                }
            }
            else if (upperBound == 1)
            {
                CoreInstance multiplicity = processorSupport.package_getByUserPath(M3Paths.PureOne);
                if (multiplicity != null)
                {
                    return multiplicity;
                }
            }
        }

        // Check bounds for validity
        if (lowerBound < 0)
        {
            throw new IllegalArgumentException("Invalid multiplicity lower bound: " + lowerBound);
        }
        if ((upperBound >= 0) && (lowerBound > upperBound))
        {
            throw new IllegalArgumentException("Invalid multiplicity: lower bound (" + lowerBound + ") greater than upper bound (" + upperBound + ")");
        }

        // Create new multiplicity
        CoreInstance newMultiplicity = processorSupport.newAnonymousCoreInstance(null, M3Paths.Multiplicity);
        CoreInstance newLowerBound = processorSupport.newAnonymousCoreInstance(null, M3Paths.MultiplicityValue);
        CoreInstance newUpperBound = processorSupport.newAnonymousCoreInstance(null, M3Paths.MultiplicityValue);
        Instance.addValueToProperty(newLowerBound, M3Properties.value, processorSupport.newCoreInstance(Integer.toString(lowerBound), M3Paths.Integer, null), processorSupport);
        if (upperBound >= 0)
        {
            Instance.addValueToProperty(newUpperBound, M3Properties.value, processorSupport.newCoreInstance(Integer.toString(upperBound), M3Paths.Integer, null), processorSupport);
        }
        Instance.addValueToProperty(newMultiplicity, M3Properties.lowerBound, newLowerBound, processorSupport);
        Instance.addValueToProperty(newMultiplicity, M3Properties.upperBound, newUpperBound, processorSupport);
        return newMultiplicity;
    }

    /**
     * Create a new concrete multiplicity instance with a single
     * valid value.
     *
     * @param value            multiplicity value (must be non-negative)
     * @param processorSupport processor support
     * @return new concrete multiplicity
     */
    public static CoreInstance newMultiplicity(int value, ProcessorSupport processorSupport)
    {
        return newMultiplicity(value, value, processorSupport);
    }

    /**
     * Create a new non-concrete multiplicity instance with the given
     * parameter name.
     *
     * @param parameterName    multiplicity parameter name
     * @param processorSupport processor support
     * @return new non-concrete multiplicity
     */
    public static CoreInstance newMultiplicity(String parameterName, ProcessorSupport processorSupport)
    {
        CoreInstance newMultiplicity = processorSupport.newAnonymousCoreInstance(null, M3Paths.Multiplicity);
        Instance.addValueToProperty(newMultiplicity, M3Properties.multiplicityParameter, processorSupport.newCoreInstance(parameterName, M3Paths.String, null), processorSupport);
        return newMultiplicity;
    }

    /**
     * Create a new concrete multiplicity with the given lower
     * bound and no upper bound.
     *
     * @param lowerBound       multiplicity lower bound (must be non-negative)
     * @param processorSupport processor support
     * @return new concrete multiplicity
     */
    public static CoreInstance newUnboundedMultiplicity(int lowerBound, ProcessorSupport processorSupport)
    {
        return newMultiplicity(lowerBound, -1, processorSupport);
    }

    public static CoreInstance copyMultiplicity(CoreInstance multiplicity, boolean copySourceInfo, ProcessorSupport processorSupport)
    {
        return copyMultiplicity(multiplicity, !copySourceInfo, null, processorSupport);
    }

    public static CoreInstance copyMultiplicity(CoreInstance multiplicity, SourceInformation newSourceInfo, ProcessorSupport processorSupport)
    {
        return copyMultiplicity(multiplicity, true, newSourceInfo, processorSupport);
    }

    public static CoreInstance copyMultiplicity(CoreInstance multiplicity, boolean replaceSourceInfo, SourceInformation newSourceInfo, ProcessorSupport processorSupport)
    {
        // Don't copy packageable multiplicities
        if (Instance.instanceOf(multiplicity, M3Paths.PackageableMultiplicity, processorSupport))
        {
            return multiplicity;
        }

        // Copy non-packageable multiplicies
        CoreInstance copy = processorSupport.newAnonymousCoreInstance(replaceSourceInfo ? newSourceInfo : multiplicity.getSourceInformation(), M3Paths.Multiplicity);

        CoreInstance parameter = multiplicity.getValueForMetaPropertyToOne(M3Properties.multiplicityParameter);
        if (parameter == null)
        {
            // Concrete multiplicity
            CoreInstance lowerBoundCopy = copyMultiplicityValue(multiplicity.getValueForMetaPropertyToOne(M3Properties.lowerBound), replaceSourceInfo, newSourceInfo, processorSupport);
            Instance.addValueToProperty(copy, M3Properties.lowerBound, lowerBoundCopy, processorSupport);

            CoreInstance upperBoundCopy = copyMultiplicityValue(multiplicity.getValueForMetaPropertyToOne(M3Properties.upperBound), replaceSourceInfo, newSourceInfo, processorSupport);
            Instance.addValueToProperty(copy, M3Properties.upperBound, upperBoundCopy, processorSupport);
        }
        else
        {
            // Non-concrete multiplicity
            Instance.addValueToProperty(copy, M3Properties.multiplicityParameter, parameter, processorSupport);
        }

        return copy;
    }

    private static CoreInstance copyMultiplicityValue(CoreInstance multiplicityValue, boolean replaceSourceInfo, SourceInformation newSourceInfo, ProcessorSupport processorSupport)
    {
        CoreInstance copy = processorSupport.newAnonymousCoreInstance(replaceSourceInfo ? newSourceInfo : multiplicityValue.getSourceInformation(), M3Paths.MultiplicityValue);
        CoreInstance value = multiplicityValue.getValueForMetaPropertyToOne(M3Properties.value);
        if (value != null)
        {
            Instance.addValueToProperty(copy, M3Properties.value, value, processorSupport);
        }
        return copy;
    }

    /**
     * Return whether value is valid for multiplicity, which must be
     * concrete.
     *
     * @param multiplicity concrete multiplicity
     * @param value        value
     * @return whether value is valid for multiplicity
     */
    public static boolean isValid(CoreInstance multiplicity, int value)
    {
        validateConcrete(multiplicity, "Cannot determine validity for non-concrete multiplicity: %s");

        int lower = concreteMultiplicityLowerBoundToInt(multiplicity);
        if (value < lower)
        {
            return false;
        }
        if (value == lower)
        {
            return true;
        }

        int upper = concreteMultiplicityUpperBoundToInt(multiplicity);
        return (upper < 0) || (value <= upper);
    }

    /**
     * Return whether two multiplicities are equal.  If the multiplicities
     * are concrete, they are equal if their bounds are equal.  If the
     * multiplicities are non-concrete, they are equal if they are
     * identical.
     *
     * @param multiplicity1 first multiplicity
     * @param multiplicity2 second multiplicity
     * @return whether the multiplicities are equal
     */
    public static boolean multiplicitiesEqual(CoreInstance multiplicity1, CoreInstance multiplicity2)
    {
        return multiplicitiesEqual(multiplicity1, multiplicity2, false);
    }

    /**
     * Return whether two multiplicities are equal.  If the multiplicities
     * are concrete, they are equal if their bounds are equal.  If the
     * multiplicities are non-concrete, the behavior depends on the value
     * of nonConcreteEqualityByName.  If it is true, then they are equal
     * if they have the same name; otherwise, they are equal only if identical.
     *
     * @param multiplicity1             first multiplicity
     * @param multiplicity2             second multiplicity
     * @param nonConcreteEqualityByName whether to use multiplicity parameter name for non-concrete equality
     * @return whether the multiplicities are equal
     */
    public static boolean multiplicitiesEqual(CoreInstance multiplicity1, CoreInstance multiplicity2, boolean nonConcreteEqualityByName)
    {
        if (multiplicity1 == multiplicity2)
        {
            return true;
        }
        if (isMultiplicityConcrete(multiplicity1))
        {
            return isMultiplicityConcrete(multiplicity2) &&
                    (concreteMultiplicityLowerBoundToInt(multiplicity1) == concreteMultiplicityLowerBoundToInt(multiplicity2)) &&
                    (concreteMultiplicityUpperBoundToInt(multiplicity1) == concreteMultiplicityUpperBoundToInt(multiplicity2));
        }
        return !isMultiplicityConcrete(multiplicity2) &&
                nonConcreteEqualityByName &&
                Objects.equals(getMultiplicityParameter(multiplicity1), getMultiplicityParameter(multiplicity2));
    }

    /**
     * Return whether one multiplicity intersect another.  That is, whether
     * there is some value valid for both multiplicities.  Both multiplicities
     * must be concrete.  If either is not, an IllegalArgumentException will be
     * thrown.
     *
     * @param multiplicity1 first multiplicity
     * @param multiplicity2 second multiplicity
     * @return whether two multiplicities intersect
     */
    public static boolean intersect(CoreInstance multiplicity1, CoreInstance multiplicity2)
    {
        validateConcrete(multiplicity1, "Cannot determine intersection for non-concrete multiplicity: %s");
        validateConcrete(multiplicity2, "Cannot determine intersection for non-concrete multiplicity: %s");

        if (multiplicity1 == multiplicity2)
        {
            return true;
        }

        int lower1 = concreteMultiplicityLowerBoundToInt(multiplicity1);
        int lower2 = concreteMultiplicityLowerBoundToInt(multiplicity2);
        if (lower1 == lower2)
        {
            return true;
        }

        int upper1 = concreteMultiplicityUpperBoundToInt(multiplicity1);
        int upper2 = concreteMultiplicityUpperBoundToInt(multiplicity2);

        if (upper1 < 0)
        {
            // multiplicity1 is unbounded above
            return (upper2 < 0) || (upper2 >= lower1);
        }

        if (upper2 < 0)
        {
            // multiplicity2 is unbounded above (but multiplicity1 is not)
            return upper1 >= lower2;
        }

        // both are bounded
        return (lower1 <= upper2) && (lower2 <= upper1);
    }

    /**
     * Return whether a set of multiplicities intersect.  That is, whether
     * there is some value valid for all multiplicities.  All multiplicities
     * must be concrete.  If any is not, an IllegalArgumentException will be
     * thrown.
     *
     * @param multiplicities multiplicities
     * @return whether multiplicities intersect
     */
    public static boolean intersect(Iterable<? extends CoreInstance> multiplicities)
    {
        Iterator<? extends CoreInstance> iterator = multiplicities.iterator();
        if (iterator.hasNext())
        {
            CoreInstance multiplicity = iterator.next();
            validateConcrete(multiplicity, "Cannot determine intersection for non-concrete multiplicity: %s");
            int lower = concreteMultiplicityLowerBoundToInt(multiplicity);
            int upper = concreteMultiplicityUpperBoundToInt(multiplicity);
            while (iterator.hasNext())
            {
                multiplicity = iterator.next();
                validateConcrete(multiplicity, "Cannot determine intersection for non-concrete multiplicity: %s");
                lower = Math.max(lower, concreteMultiplicityLowerBoundToInt(multiplicity));
                if (upper < 0)
                {
                    upper = concreteMultiplicityUpperBoundToInt(multiplicity);
                }
                else if (upper > 0)
                {
                    int newUpper = concreteMultiplicityUpperBoundToInt(multiplicity);
                    if (newUpper >= 0)
                    {
                        upper = Math.min(upper, newUpper);
                    }
                }
                if ((upper >= 0) && (lower > upper))
                {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Return whether one multiplicity subsumes another.  That is, whether every
     * value valid for the first is also valid for the second.  Both multiplicities
     * must be concrete.  If either is not, an IllegalArgumentException will be thrown.
     *
     * @param subsuming possibly subsuming multiplicity
     * @param subsumed  possibly subsumed multiplicity
     * @return whether the first multiplicity subsumes the second
     */
    public static boolean subsumes(CoreInstance subsuming, CoreInstance subsumed)
    {
        validateConcrete(subsuming, "Cannot determine subsumption for non-concrete multiplicity: %s");
        validateConcrete(subsumed, "Cannot determine subsumption for non-concrete multiplicity: %s");

        int lower1 = concreteMultiplicityLowerBoundToInt(subsuming);
        int lower2 = concreteMultiplicityLowerBoundToInt(subsumed);
        if (lower2 < lower1)
        {
            return false;
        }

        int upper1 = concreteMultiplicityUpperBoundToInt(subsuming);
        if (upper1 < 0)
        {
            return true;
        }

        int upper2 = concreteMultiplicityUpperBoundToInt(subsumed);
        return (0 <= upper2) && (upper2 <= upper1);
    }

    /**
     * Return the minimal multiplicity which subsumes the given multiplicities.
     * If either of the multiplicities is non-concrete, then the minimal
     * subsuming multiplicity is [*].
     *
     * @param multiplicity1    first multiplicity
     * @param multiplicity2    second multiplicity
     * @param processorSupport processor support
     * @return minimal subsuming multiplicity
     */
    public static CoreInstance minSubsumingMultiplicity(CoreInstance multiplicity1, CoreInstance multiplicity2, ProcessorSupport processorSupport)
    {
        if (multiplicity1 == null || multiplicity2 == null)
        {
            return null;
        }
        if (!isMultiplicityConcrete(multiplicity1) || !isMultiplicityConcrete(multiplicity2))
        {
            return newUnboundedMultiplicity(0, processorSupport);
        }

        int lower1 = concreteMultiplicityLowerBoundToInt(multiplicity1);
        int lower2 = concreteMultiplicityLowerBoundToInt(multiplicity2);
        int lower = Math.min(lower1, lower2);

        int upper1 = concreteMultiplicityUpperBoundToInt(multiplicity1);
        int upper2 = concreteMultiplicityUpperBoundToInt(multiplicity2);
        int upper = ((upper1 < 0) || (upper2 < 0)) ? -1 : Math.max(upper1, upper2);

        return newMultiplicity(lower, upper, processorSupport);
    }

    /**
     * Return the minimal multiplicity which subsumes all the given multiplicities.
     * If any of the multiplicities is non-concrete, then the minimal subsuming
     * multiplicity is [*].
     *
     * @param multiplicities   multiplicities
     * @param processorSupport processor support
     * @return minimal subsuming multiplicity
     */
    public static CoreInstance minSubsumingMultiplicity(ListIterable<? extends CoreInstance> multiplicities, ProcessorSupport processorSupport)
    {
        int count = multiplicities.size();
        switch (count)
        {
            case 0:
            {
                throw new IllegalArgumentException("Cannot find minimal subsuming multiplicity for an empty set");
            }
            case 1:
            {
                CoreInstance multiplicity = multiplicities.getFirst();
                return isMultiplicityConcrete(multiplicity) ? multiplicity : newUnboundedMultiplicity(0, processorSupport);
            }
            case 2:
            {
                return minSubsumingMultiplicity(multiplicities.get(0), multiplicities.get(1), processorSupport);
            }
            default:
            {
                int lowerBound = Integer.MAX_VALUE;
                int upperBound = 0;
                for (CoreInstance multiplicity : multiplicities)
                {
                    if (!isMultiplicityConcrete(multiplicity))
                    {
                        return newUnboundedMultiplicity(0, processorSupport);
                    }
                    if (lowerBound > 0)
                    {
                        lowerBound = Math.min(lowerBound, concreteMultiplicityLowerBoundToInt(multiplicity));
                    }
                    if (upperBound >= 0)
                    {
                        int upper = concreteMultiplicityUpperBoundToInt(multiplicity);
                        upperBound = (upper < 0) ? -1 : Math.max(upperBound, upper);
                    }
                }
                return newMultiplicity(lowerBound, upperBound, processorSupport);
            }
        }
    }

    /**
     * Get the lower bound of the given concrete multiplicity as an int.  The
     * int is guaranteed to be non-negative.
     *
     * @param multiplicity concrete multiplicity
     * @return multiplicity lower bound
     */
    static int concreteMultiplicityLowerBoundToInt(CoreInstance multiplicity)
    {
        CoreInstance lowerBound = multiplicity.getValueForMetaPropertyToOne(M3Properties.lowerBound).getValueForMetaPropertyToOne(M3Properties.value);
        return PrimitiveUtilities.getIntegerValue(lowerBound).intValue();
    }

    /**
     * Get the upper bound of the given concrete multiplicity as an int.
     * If the multiplicity has no upper bound, -1 is returned.  Otherwise,
     * the int is guaranteed to be non-negative.
     *
     * @param multiplicity multiplicity
     * @return multiplicity upper bound, or -1 if there is no upper bound
     */
    static int concreteMultiplicityUpperBoundToInt(CoreInstance multiplicity)
    {
        CoreInstance upperBound = multiplicity.getValueForMetaPropertyToOne(M3Properties.upperBound).getValueForMetaPropertyToOne(M3Properties.value);
        return (upperBound == null) ? -1 : PrimitiveUtilities.getIntegerValue(upperBound).intValue();
    }

    /**
     * Validate that multiplicity is concrete.  If it is not, throw
     * an IllegalArgumentException.  The exception is made by calling
     * String.format(messageFormat, print(multiplicity)).
     *
     * @param multiplicity  multiplicity to validate
     * @param messageFormat exception message format.
     */
    private static void validateConcrete(CoreInstance multiplicity, String messageFormat)
    {
        if (!isMultiplicityConcrete(multiplicity))
        {
            throw new IllegalArgumentException(String.format(messageFormat, print(multiplicity)));
        }
    }

    public static boolean isLowerZero(CoreInstance multiplicity)
    {
        return isMultiplicityConcrete(multiplicity) && (concreteMultiplicityLowerBoundToInt(multiplicity) == 0);
    }

    public static boolean isZeroToOne(CoreInstance multiplicity)
    {
        return isMultiplicityConcrete(multiplicity) &&
                (concreteMultiplicityLowerBoundToInt(multiplicity) == 0) &&
                (concreteMultiplicityUpperBoundToInt(multiplicity) == 1);
    }

    public static boolean isZeroToMany(CoreInstance multiplicity)
    {
        return isMultiplicityConcrete(multiplicity) &&
                (concreteMultiplicityLowerBoundToInt(multiplicity) == 0) &&
                (concreteMultiplicityUpperBoundToInt(multiplicity) == -1);
    }
}
