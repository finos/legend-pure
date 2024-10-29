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

package org.finos.legend.pure.runtime.java.compiled;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.ordered.ReversibleIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.TreeNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Nil;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateTime;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.StrictDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.Year;
import org.finos.legend.pure.m4.coreinstance.primitive.date.YearMonth;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Bridge;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureEqualsHashingStrategy;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TimeZone;
import java.util.function.Consumer;

public class CoreHelper
{
    // TRIGO ------------------------------------------------------------------
    public static double sin(Number input)
    {
        return sin(input.doubleValue());
    }

    public static double sin(double input)
    {
        return Math.sin(input);
    }

    public static double cos(Number input)
    {
        return cos(input.doubleValue());
    }

    public static double cos(double input)
    {
        return Math.cos(input);
    }

    public static double tan(Number input)
    {
        return tan(input.doubleValue());
    }

    public static double tan(double input)
    {
        double result = Math.tan(input);
        if (Double.isNaN(result))
        {
            throw new PureExecutionException("Unable to compute tan of " + input, Stacks.mutable.empty());
        }
        return result;
    }

    public static double coTangent(Number input)
    {
        return coTangent(input.doubleValue());
    }

    public static double coTangent(double input)
    {
        return 1.0 / tan(input);
    }

    public static double asin(Number input, SourceInformation sourceInformation)
    {
        return asin(input.doubleValue(), sourceInformation);
    }

    public static double asin(double input, SourceInformation sourceInformation)
    {
        double result = Math.asin(input);
        if (Double.isNaN(result))
        {
            throw new PureExecutionException(sourceInformation, "Unable to compute asin of " + input, Stacks.mutable.empty());
        }
        return result;
    }

    public static double acos(Number input, SourceInformation sourceInformation)
    {
        return acos(input.doubleValue(), sourceInformation);
    }

    public static double acos(double input, SourceInformation sourceInformation)
    {
        double result = Math.acos(input);
        if (Double.isNaN(result))
        {
            throw new PureExecutionException(sourceInformation, "Unable to compute acos of " + input, Stacks.mutable.empty());
        }
        return result;
    }

    public static double atan(Number input)
    {
        return atan(input.doubleValue());
    }

    public static double atan(double input)
    {
        return Math.atan(input);
    }

    public static double atan2(Number input1, Number input2, SourceInformation sourceInformation)
    {
        return atan2(input1.doubleValue(), input2.doubleValue(), sourceInformation);
    }

    public static double atan2(double input1, double input2, SourceInformation sourceInformation)
    {
        double result = Math.atan2(input1, input2);
        if (Double.isNaN(result))
        {
            throw new PureExecutionException(sourceInformation, "Unable to compute atan2 of " + input1 + " " + input2, Stacks.mutable.empty());
        }
        return result;
    }
    // TRIGO ------------------------------------------------------------------


    // DATE-TIME --------------------------------------------------------------
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    public static PureDate adjustDate(PureDate date, long number, Enum unit)
    {
        switch (unit._name())
        {
            case "YEARS":
            {
                return date.addYears(number);
            }
            case "MONTHS":
            {
                return date.addMonths(number);
            }
            case "WEEKS":
            {
                return date.addWeeks(number);
            }
            case "DAYS":
            {
                return date.addDays(number);
            }
            case "HOURS":
            {
                return date.addHours(number);
            }
            case "MINUTES":
            {
                return date.addMinutes(number);
            }
            case "SECONDS":
            {
                return date.addSeconds(number);
            }
            case "MILLISECONDS":
            {
                return date.addMilliseconds(number);
            }
            case "MICROSECONDS":
            {
                return date.addMicroseconds(number);
            }
            case "NANOSECONDS":
            {
                return date.addNanoseconds(number);
            }
            default:
            {
                throw new PureExecutionException("Unsupported duration unit: " + unit, Stacks.mutable.empty());
            }
        }
    }

    public static long dateDiff(PureDate date1, PureDate date2, Enum unit)
    {
        return date1.dateDifference(date2, unit._name());
    }

    public static long year(PureDate date)
    {
        return date.getYear();
    }

    public static boolean hasMonth(PureDate date)
    {
        return date.hasMonth();
    }

    public static long monthNumber(PureDate date)
    {
        return date.getMonth();
    }

    public static boolean hasDay(PureDate date)
    {
        return date.hasDay();
    }

    public static PureDate datePart(PureDate date)
    {
        return date.hasHour() ? DateFunctions.newPureDate(date.getYear(), date.getMonth(), date.getDay()) : date;
    }


    public static long dayOfMonth(PureDate date, SourceInformation sourceInformation)
    {
        if (!date.hasDay())
        {
            throw new PureExecutionException(sourceInformation, "Cannot get day of month for " + date, Stacks.mutable.empty());
        }
        return date.getDay();
    }

    public static boolean hasHour(PureDate date)
    {
        return date.hasHour();
    }

    public static long hour(PureDate date, SourceInformation sourceInformation)
    {
        if (!date.hasHour())
        {
            throw new PureExecutionException(sourceInformation, "Cannot get hour for " + date, Stacks.mutable.empty());
        }
        return date.getHour();
    }

    public static boolean hasMinute(PureDate date)
    {
        return date.hasMinute();
    }

    public static long minute(PureDate date, SourceInformation sourceInformation)
    {
        if (!date.hasMinute())
        {
            throw new PureExecutionException(sourceInformation, "Cannot get minute for " + date, Stacks.mutable.empty());
        }
        return date.getMinute();
    }

    public static boolean hasSecond(PureDate date)
    {
        return date.hasSecond();
    }

    public static long second(PureDate date, SourceInformation sourceInformation)
    {
        if (!date.hasSecond())
        {
            throw new PureExecutionException(sourceInformation, "Cannot get second for " + date, Stacks.mutable.empty());
        }
        return date.getSecond();
    }

    public static boolean hasSubsecond(PureDate date)
    {
        return date.hasSubsecond();
    }

    public static boolean hasSubsecondWithAtLeastPrecision(PureDate date, long minPrecision)
    {
        return date.hasSubsecond() && (date.getSubsecond().length() >= minPrecision);
    }

    public static PureDate newDate(long year, SourceInformation sourceInformation)
    {
        try
        {
            return Year.newYear((int) year);
        }
        catch (Exception e)
        {
            throw new PureExecutionException(sourceInformation, e.getMessage(), Stacks.mutable.empty());
        }
    }

    public static PureDate newDate(long year, long month, SourceInformation sourceInformation)
    {
        try
        {
            return YearMonth.newYearMonth((int) year, (int) month);
        }
        catch (Exception e)
        {
            throw new PureExecutionException(sourceInformation, e.getMessage(), Stacks.mutable.empty());
        }
    }

    public static StrictDate newDate(long year, long month, long day, SourceInformation sourceInformation)
    {
        try
        {
            return DateFunctions.newPureDate((int) year, (int) month, (int) day);
        }
        catch (Exception e)
        {
            throw new PureExecutionException(sourceInformation, e.getMessage(), Stacks.mutable.empty());
        }
    }

    public static DateTime newDate(long year, long month, long day, long hour, SourceInformation sourceInformation)
    {
        try
        {
            return DateFunctions.newPureDate((int) year, (int) month, (int) day, (int) hour);
        }
        catch (Exception e)
        {
            throw new PureExecutionException(sourceInformation, e.getMessage(), e, Stacks.mutable.empty());
        }
    }

    public static DateTime newDate(long year, long month, long day, long hour, long minute, SourceInformation sourceInformation)
    {
        try
        {
            return DateFunctions.newPureDate((int) year, (int) month, (int) day, (int) hour, (int) minute);
        }
        catch (Exception e)
        {
            throw new PureExecutionException(sourceInformation, e.getMessage(), e, Stacks.mutable.empty());
        }
    }

    public static DateTime newDate(long year, long month, long day, long hour, long minute, Number second, SourceInformation sourceInformation)
    {
        int secondInt;
        String subsecond = null;
        if ((second instanceof Integer) || (second instanceof Long) || (second instanceof BigInteger))
        {
            // TODO check if the number is too large for an int
            secondInt = second.intValue();
        }
        else if ((second instanceof Float) || (second instanceof Double))
        {
            secondInt = second.intValue();
            String string = BigDecimal.valueOf(second.doubleValue()).toPlainString();
            int index = string.indexOf('.');
            subsecond = (index == -1) ? "0" : string.substring(index + 1);
        }
        else if (second instanceof BigDecimal)
        {
            secondInt = second.intValue();
            String string = ((BigDecimal) second).toPlainString();
            int index = string.indexOf('.');
            if (index != -1)
            {
                subsecond = string.substring(index + 1);
            }
        }
        else
        {
            throw new PureExecutionException(sourceInformation, "Unhandled number: " + second, Stacks.mutable.empty());
        }
        try
        {
            return (subsecond == null) ? DateFunctions.newPureDate((int) year, (int) month, (int) day, (int) hour, (int) minute, secondInt) : DateFunctions.newPureDate((int) year, (int) month, (int) day, (int) hour, (int) minute, secondInt, subsecond);
        }
        catch (Exception e)
        {
            throw new PureExecutionException(sourceInformation, e.getMessage(), e, Stacks.mutable.empty());
        }
    }
    // DATE-TIME --------------------------------------------------------------


    // MATH --------------------------------------------------------------------
    public static double exp(Number n)
    {
        return Math.exp(n.doubleValue());
    }

    public static double log(Number n)
    {
        return Math.log(n.doubleValue());
    }

    public static double log10(Number n)
    {
        return Math.log10(n.doubleValue());
    }

    public static long sign(Number number)
    {
        return (long) Math.signum(number.doubleValue());
    }

    public static Long floor(Number number)
    {
        if (number instanceof Long)
        {
            return (Long) number;
        }
        return (long) Math.floor(number.doubleValue());
    }

    public static Long ceiling(Number number)
    {
        if (number instanceof Long)
        {
            return (Long) number;
        }
        return (long) Math.ceil(number.doubleValue());
    }

    public static Long round(Number number)
    {
        if (number instanceof Long)
        {
            return (Long) number;
        }

        double toRound = number.doubleValue();
        if (toRound == 0x1.fffffffffffffp-2) // greatest double value less than 0.5
        {
            return 0L;
        }

        toRound += 0.5d;
        double floor = Math.floor(toRound);
        if ((floor == toRound) && ((floor % 2) != 0))
        {
            return ((long) floor - 1);
        }
        return (long) floor;
    }

    public static Number round(Number number, long scale)
    {
        if (number instanceof Double)
        {
            return round((Double) number, scale);
        }
        if (number instanceof BigDecimal)
        {
            return round((BigDecimal) number, scale);
        }

        throw new IllegalArgumentException("incorrect number type");
    }

    public static double round(Double number, long scale)
    {
        return round(BigDecimal.valueOf(number), scale).doubleValue();
    }

    public static BigDecimal round(BigDecimal number, long scale)
    {
        return number.setScale((int) scale, RoundingMode.HALF_UP);
    }

    public static double sqrt(Number input, SourceInformation sourceInformation)
    {
        return sqrt(input.doubleValue(), sourceInformation);
    }

    public static double sqrt(double input, SourceInformation sourceInformation)
    {
        double result = Math.sqrt(input);
        if (Double.isNaN(result))
        {
            throw new PureExecutionException(sourceInformation, "Unable to compute sqrt of " + input, Stacks.mutable.empty());
        }
        return result;
    }

    public static double cbrt(Number input, SourceInformation sourceInformation)
    {
        return cbrt(input.doubleValue(), sourceInformation);
    }

    public static double cbrt(double input, SourceInformation sourceInformation)
    {
        double result = Math.cbrt(input);
        if (Double.isNaN(result))
        {
            throw new PureExecutionException(sourceInformation, "Unable to compute cbrt of " + input, Stacks.mutable.empty());
        }
        return result;
    }

    public static Number rem(Number dividend, Number divisor, SourceInformation sourceInformation)
    {
        if (divisor.doubleValue() == 0)
        {
            throw new PureExecutionException(sourceInformation, "Cannot divide " + dividend.toString() + " by zero", Stacks.mutable.empty());
        }

        if (dividend instanceof Long && divisor instanceof Long)
        {
            return dividend.longValue() % divisor.longValue();
        }

        if (dividend instanceof BigDecimal && divisor instanceof BigDecimal)
        {
            return ((BigDecimal) dividend).remainder((BigDecimal) divisor);
        }

        if (dividend instanceof BigDecimal && divisor instanceof Long)
        {
            return ((BigDecimal) dividend).remainder(BigDecimal.valueOf((Long) divisor));
        }

        if (dividend instanceof BigDecimal && divisor instanceof Double)
        {
            return ((BigDecimal) dividend).remainder(BigDecimal.valueOf((Double) divisor));
        }

        if (dividend instanceof Long && divisor instanceof BigDecimal)
        {
            return BigDecimal.valueOf((Long) dividend).remainder((BigDecimal) divisor);
        }

        if (dividend instanceof Double && divisor instanceof BigDecimal)
        {
            return BigDecimal.valueOf((Double) dividend).remainder((BigDecimal) divisor);
        }

        return dividend.doubleValue() % divisor.doubleValue();
    }

    public static long mod(long dividend, long divisor)
    {
        return BigInteger.valueOf(dividend).mod(BigInteger.valueOf(divisor)).longValue();
    }

    public static Double pow(Number number, Number power)
    {
        return StrictMath.pow(
                number instanceof Long ? number.longValue() : number.doubleValue(),
                power instanceof Long ? power.longValue() : power.doubleValue()
        );
    }

    public static BigDecimal toDecimal(Number number)
    {
        if (number instanceof BigDecimal)
        {
            return (BigDecimal) number;
        }
        return new BigDecimal(number.toString());
    }

    public static double toFloat(Number number)
    {
        return number.doubleValue();
    }
    // MATH --------------------------------------------------------------------


    // COLLECTION ---------------------------------------------------------------
    public static <T> RichIterable<T> toReversed(RichIterable<T> collection)
    {
        if (collection == null || Iterate.isEmpty(collection))
        {
            return Lists.immutable.empty();
        }
        if (collection instanceof ReversibleIterable)
        {
            return ((ReversibleIterable<T>) collection).asReversed();
        }
        return collection.toList().reverseThis();
    }

    public static <T> RichIterable<T> take(T element, long number)
    {
        return ((element == null) || (number < 1)) ? Lists.immutable.empty() : Lists.immutable.with(element);
    }

    public static <T> RichIterable<T> take(RichIterable<T> list, long number)
    {
        if ((list == null) || (number <= 0))
        {
            return Lists.immutable.empty();
        }
        if (list instanceof LazyIterable)
        {
            return ((LazyIterable<T>) list).take((int) number);
        }
        if (number >= list.size())
        {
            return list;
        }

        int end = (int) number;
        if (list instanceof ListIterable)
        {
            return ((ListIterable<T>) list).subList(0, end);
        }

        MutableList<T> result = Lists.mutable.ofInitialCapacity(end);
        result.addAllIterable(LazyIterate.take(list, end));
        return result;
    }

    public static <T> RichIterable<T> drop(T element, long number)
    {
        return ((element == null) || (number >= 1)) ? Lists.immutable.<T>empty() : Lists.immutable.with(element);
    }

    public static <T> RichIterable<T> drop(RichIterable<T> list, long number)
    {
        if (list == null)
        {
            return Lists.immutable.empty();
        }
        if (number <= 0)
        {
            return list;
        }
        if (list instanceof LazyIterable)
        {
            return ((LazyIterable<T>) list).drop((int) number);
        }
        int size = list.size();
        if (number >= size)
        {
            return Lists.immutable.empty();
        }

        int toDrop = (int) number;
        if (list instanceof ListIterable)
        {
            return ((ListIterable<T>) list).subList(toDrop, size);
        }

        MutableList<T> result = Lists.mutable.ofInitialCapacity(size - toDrop);
        result.addAllIterable(LazyIterate.drop(list, toDrop));
        return result;
    }

    public static boolean exists(Object object, Predicate predicate)
    {
        if (object == null)
        {
            return false;
        }

        if (object instanceof Iterable)
        {
            return Iterate.anySatisfy((Iterable) object, predicate);
        }

        return predicate.accept(object);
    }

    public static boolean forAll(Object object, Predicate predicate)
    {
        if (object == null)
        {
            return true;
        }

        if (object instanceof Iterable)
        {
            return Iterate.allSatisfy((Iterable) object, predicate);
        }

        return predicate.accept(object);
    }

    public static <T> T last(RichIterable<T> list)
    {
        return Iterate.isEmpty(list) ? null : list.getLast();
    }

    public static <T> T last(T instance)
    {
        return instance;
    }

    public static <T> RichIterable<T> slice(T element, long low, long high, SourceInformation sourceInformation)
    {
        return ((element == null) || (low > 0) || (high <= 0)) ? Lists.immutable.empty() : Lists.immutable.with(element);
    }

    public static <T> RichIterable<T> slice(RichIterable<T> collection, long low, long high, SourceInformation sourceInformation)
    {
        low = (low < 0) ? 0 : low;
        high = (high < 0) ? 0 : high;

        if ((collection == null) || (high == 0) || (high == low))
        {
            return Lists.immutable.empty();
        }

        if (collection instanceof LazyIterable)
        {
            return ((LazyIterable<T>) collection).drop((int) low).take((int) (high - low));
        }

        int collectionSize = collection.size();
        if (low >= collectionSize)
        {
            return Lists.immutable.empty();
        }
        int start = (int) low;
        int end = (high > collectionSize) ? collectionSize : (int) high;
        if (start > end)
        {
            throw new PureExecutionException(sourceInformation, "The low bound (" + start + ") can't be higher than the high bound (" + end + ") in a slice operation", Stacks.mutable.empty());
        }


        if (collection instanceof ListIterable)
        {
            return ((ListIterable<T>) collection).subList(start, end);
        }

        MutableList<T> result = Lists.mutable.ofInitialCapacity(end - start);
        result.addAllIterable(LazyIterate.drop(collection, start).take(end - start));
        return result;
    }

    public static long indexOf(Object instances, Object object)
    {
        if (instances == null)
        {
            return -1L;
        }
        if (!(instances instanceof Iterable))
        {
            return CompiledSupport.equal(object, instances) ? 0L : -1L;
        }
        return indexOf((Iterable<?>) instances, object);
    }

    public static long indexOf(Iterable<?> instances, Object object)
    {
        return (instances == null) ? -1L : Iterate.detectIndex(instances, i -> CompiledSupport.equal(object, i));
    }

    public static <T> RichIterable<? extends T> removeAllOptimized(RichIterable<? extends T> main, RichIterable<? extends T> other)
    {
        MutableSet<Object> toRemove = PureEqualsHashingStrategy.newMutableSet().withAll(other);
        return main.reject(toRemove::contains);
    }

    public static TreeNode replaceTreeNode(TreeNode instance, TreeNode targetNode, TreeNode subTree)
    {
        if (instance == targetNode)
        {
            return subTree;
        }
        TreeNode result = instance.copy();
        replaceTreeNodeCopy(instance, result, targetNode, subTree);
        return result;
    }

    public static void replaceTreeNodeCopy(TreeNode instance, TreeNode result, TreeNode targetNode, TreeNode subTree)
    {
        MutableList<TreeNode> newChildren = Lists.mutable.empty();
        instance._childrenData().forEach(child ->
        {
            if (child == targetNode)
            {
                newChildren.add(subTree);
            }
            else
            {
                TreeNode newCopy = child.copy();
                replaceTreeNodeCopy(child, newCopy, targetNode, subTree);
                newChildren.add(newCopy);
            }
        });
        result._childrenData(newChildren);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static PureMap putAllPairs(PureMap pureMap, RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<?, ?>> pairs)
    {
        Map map = pureMap.getMap();
        MutableMap<Object, Object> newOne = (map instanceof UnifiedMapWithHashingStrategy) ? new UnifiedMapWithHashingStrategy<>(((UnifiedMapWithHashingStrategy) map).hashingStrategy(), map) : Maps.mutable.withMap(map);
        pairs.forEach(p -> newOne.put(p._first(), p._second()));
        return new PureMap(newOne);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static PureMap putAllPairs(PureMap pureMap, org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<?, ?> pair)
    {
        Map map = pureMap.getMap();
        MutableMap<Object, Object> newOne = (map instanceof UnifiedMapWithHashingStrategy) ? new UnifiedMapWithHashingStrategy<>(((UnifiedMapWithHashingStrategy) map).hashingStrategy(), map) : Maps.mutable.withMap(map);
        newOne.put(pair._first(), pair._second());
        return new PureMap(newOne);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static PureMap putAllMaps(PureMap pureMap, PureMap other)
    {
        Map map = pureMap.getMap();
        MutableMap<Object, Object> newOne = (map instanceof UnifiedMapWithHashingStrategy) ? new UnifiedMapWithHashingStrategy<>(((UnifiedMapWithHashingStrategy) map).hashingStrategy(), map) : Maps.mutable.withMap(map);
        newOne.putAll(other.getMap());
        return new PureMap(newOne);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static PureMap replaceAll(PureMap pureMap, RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<?, ?>> pairs)
    {
        Map map = pureMap.getMap();
        MutableMap<Object, Object> newOne = map instanceof UnifiedMapWithHashingStrategy ? new UnifiedMapWithHashingStrategy<>(((UnifiedMapWithHashingStrategy) map).hashingStrategy()) : Maps.mutable.empty();
        pairs.forEach(p -> newOne.put(p._first(), p._second()));
        return new PureMap(newOne);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static PureMap replaceAll(PureMap pureMap, org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<?, ?> pair)
    {
        Map map = pureMap.getMap();
        MutableMap<Object, Object> newOne = map instanceof UnifiedMapWithHashingStrategy ? new UnifiedMapWithHashingStrategy<>(((UnifiedMapWithHashingStrategy) map).hashingStrategy()) : Maps.mutable.empty();
        newOne.put(pair._first(), pair._second());
        return new PureMap(newOne);
    }

    public static PureMap newMap(RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<?, ?>> pairs, ExecutionSupport es)
    {
        MutableMap<Object, Object> map = PureEqualsHashingStrategy.newMutableMap();
        pairs.forEach(p -> map.put(p._first(), p._second()));
        return new PureMap(map);
    }

    public static PureMap newMap(org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<?, ?> p, ExecutionSupport es)
    {
        MutableMap<Object, Object> map = PureEqualsHashingStrategy.newMutableMap();
        if (p != null)
        {
            map.put(p._first(), p._second());
        }
        return new PureMap(map);
    }

    private static class PropertyHashingStrategy implements HashingStrategy<Object>
    {
        private final RichIterable<? extends Property<?, ?>> properties;
        private final ExecutionSupport es;
        private final Bridge bridge;

        PropertyHashingStrategy(RichIterable<? extends Property<?, ?>> properties, Bridge bridge, ExecutionSupport es)
        {
            this.properties = properties;
            this.bridge = bridge;
            this.es = es;
        }

        PropertyHashingStrategy(Property<?, ?> property, ExecutionSupport es)
        {
            this(Lists.immutable.with(property), null, es);
        }

        @Override
        public int computeHashCode(Object o)
        {
            int hashCode = 0;
            for (Property<?, ?> property : this.properties)
            {
                hashCode = (31 * hashCode) + CompiledSupport.safeHashCode(evaluateProperty(property, o));
            }
            return hashCode;
        }

        @Override
        public boolean equals(Object obj1, Object obj2)
        {
            return this.properties.allSatisfy(p -> CompiledSupport.equal(evaluateProperty(p, obj1), evaluateProperty(p, obj2)));
        }

        private Object evaluateProperty(Property<?, ?> property, Object obj)
        {
            return Pure.evaluate(this.es, property, this.bridge, obj);
        }
    }

    public static PureMap newMap(RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<?, ?>> pairs, Property<?, ?> property, ExecutionSupport es)
    {
        MutableMap<Object, Object> map = UnifiedMapWithHashingStrategy.newMap(new PropertyHashingStrategy(property, es));
        pairs.forEach(p -> map.put(p._first(), p._second()));
        return new PureMap(map);
    }

    public static PureMap newMap(org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<?, ?> pair, Property<?, ?> property, ExecutionSupport es)
    {
        MutableMap<Object, Object> map = UnifiedMapWithHashingStrategy.newMap(new PropertyHashingStrategy(property, es));
        if (pair != null)
        {
            map.put(pair._first(), pair._second());
        }
        return new PureMap(map);
    }

    public static PureMap newMap(RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<?, ?>> pairs, RichIterable<? extends Property<?, ?>> properties, Bridge bridge, ExecutionSupport es)
    {
        MutableMap<Object, Object> map = UnifiedMapWithHashingStrategy.newMap(new PropertyHashingStrategy(properties, bridge, es));
        pairs.forEach(p -> map.put(p._first(), p._second()));
        return new PureMap(map);
    }

    public static PureMap put(PureMap pureMap, Object key, Object val)
    {
        Map map = pureMap.getMap();
        MutableMap<Object, Object> newOne = map instanceof UnifiedMapWithHashingStrategy ? new UnifiedMapWithHashingStrategy<>(((UnifiedMapWithHashingStrategy<?, ?>) map).hashingStrategy(), map) : Maps.mutable.withMap(map);
        newOne.put(key, val);
        return new PureMap(newOne);
    }

    public static RichIterable values(PureMap map)
    {
        return Lists.mutable.withAll(map.getMap().values());
    }

    public static RichIterable keys(PureMap map)
    {
        return Lists.mutable.withAll(map.getMap().keySet());
    }

    // COLLECTION ---------------------------------------------------------------


    // META ---------------------------------------------------------------------
    public static RichIterable enumValues(CoreInstance coreInstance)
    {
        return coreInstance.getValueForMetaPropertyToMany(M3Properties.values);
    }

    public static Object buildSourceInformation(RichIterable<?> coreInstance, ClassLoader globalClassLoader)
    {
        return buildSourceInformation(coreInstance.getAny(), globalClassLoader);
    }

    public static Object buildSourceInformation(Object obj, ClassLoader globalClassLoader)
    {
        Object result = null;
        if (obj instanceof CoreInstance)
        {
            CoreInstance coreInstance = (CoreInstance) obj;
            SourceInformation sourceInfo = coreInstance.getSourceInformation();
            if (sourceInfo != null)
            {
                try
                {
                    Class<?> sourceInfoClass = globalClassLoader.loadClass(FullJavaPaths.SourceInformation_Impl);
                    result = sourceInfoClass.getConstructor(String.class).newInstance("NOID");
                    sourceInfoClass.getField("_source").set(result, sourceInfo.getSourceId());
                    sourceInfoClass.getField("_startLine").set(result, sourceInfo.getStartLine());
                    sourceInfoClass.getField("_startColumn").set(result, sourceInfo.getStartColumn());
                    sourceInfoClass.getField("_line").set(result, sourceInfo.getLine());
                    sourceInfoClass.getField("_column").set(result, sourceInfo.getColumn());
                    sourceInfoClass.getField("_endLine").set(result, sourceInfo.getEndLine());
                    sourceInfoClass.getField("_endColumn").set(result, sourceInfo.getEndColumn());
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Error getting source information for " + obj, e);
                }
            }
        }
        return result;
    }

    public static RichIterable<Type> getGeneralizations(Type type, ExecutionSupport es)
    {
        return org.finos.legend.pure.m3.navigation.type.Type.getGeneralizationResolutionOrder(type, ((CompiledExecutionSupport) es).getProcessorSupport()).collect(t -> (Type) t);
    }

    public static Tag tag(Profile profile, String tag)
    {
        return profile._p_tags().detect(t -> tag.equals(t._value()));
    }

    public static Stereotype stereotype(Profile profile, String stereotype)
    {
        return profile._p_stereotypes().detect(st -> stereotype.equals(st._value()));
    }

    public static boolean subTypeOf(Type subType, Type superType, ExecutionSupport es)
    {
        if (subType.equals(superType))
        {
            return true;
        }

        // NOTE: ClassNotFoundException can occur when we use subTypeOf() in engine where some
        // Java classes are not available during plan generation. There is a potentially
        // less performant alternative which is to use type_subTypeOf() as this will use the
        // metamodel graph instead of Java classes to test subtype; but this alternative is more reliable.
        // As such, to be defensive, we should fallback to the latter when the former fails with ClassNotFoundException
        // See https://github.com/finos/legend-pure/issues/324
        Class<?> theSubTypeClass;
        try
        {
            theSubTypeClass = Pure.pureTypeToJavaClass(subType, es);
        }
        catch (Exception e)
        {
            return ((CompiledExecutionSupport) es).getProcessorSupport().type_subTypeOf(subType, superType);
        }
        if (theSubTypeClass == Nil.class)
        {
            return true;
        }

        Class<?> theSuperTypeClass;
        try
        {
            theSuperTypeClass = Pure.pureTypeToJavaClass(superType, es);
        }
        catch (Exception e)
        {
            return ((CompiledExecutionSupport) es).getProcessorSupport().type_subTypeOf(subType, superType);
        }
        return (theSuperTypeClass == Any.class) || theSuperTypeClass.isAssignableFrom(theSubTypeClass);
    }
    // META ---------------------------------------------------------------------

    // STRING ---------------------------------------------------------------------
    public static BigDecimal parseDecimal(String str)
    {
        return new BigDecimal(str.endsWith("D") || str.endsWith("d") ? str.substring(0, str.length() - 1) : str);
    }

    public static boolean contains(String str1, String str2)
    {
        return str1.contains(str2);
    }

    public static boolean endsWith(String str1, String str2)
    {
        return str1.endsWith(str2);
    }

    public static String reverse(String str)
    {
        return new StringBuilder(str).reverse().toString();
    }

    public static String toLowerCase(String str)
    {
        return str.toLowerCase();
    }

    public static String toUpperCase(String str)
    {
        return str.toUpperCase();
    }

    public static String trim(String str)
    {
        return str.trim();
    }

    public static String ltrim(String str)
    {
        return StringUtils.stripStart(str, null);
    }

    public static String rtrim(String str)
    {
        return StringUtils.stripEnd(str, null);
    }

    // STRING ---------------------------------------------------------------------


    public static Object dynamicMatchWith(Object obj, RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?>> funcs, Object var, Bridge bridge, ExecutionSupport es)
    {
        return Pure.dynamicMatch(obj, funcs, var, true, bridge, es);
    }

    public static Object rawEvalProperty(Property<?, ?> property, Object value, SourceInformation sourceInformation)
    {
        try
        {
            return value.getClass().getField("_" + property._name()).get(value);
        }
        catch (NoSuchFieldException e)
        {
            throw new PureExecutionException(sourceInformation, "Can't find the property '" + property._name() + "' in the class " + CompiledSupport.getPureClassName(value), Stacks.mutable.empty());
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static <T> T removeOverride(T instance)
    {
        return (T) ((Any) instance)._elementOverrideRemove();
    }

}
