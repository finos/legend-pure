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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.bootstrap.generator.M3ToJavaGenerator;
import org.finos.legend.pure.m3.coreinstance.BaseCoreInstance;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstance;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.runtime.java.compiled.compiler.MemoryClassLoader;
import org.finos.legend.pure.runtime.java.compiled.compiler.MemoryFileManager;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompileException;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompiler;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPurePrimitiveTypeMapping;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaSourceCodeGenerator;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.JavaCompiledCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.QuantityCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.ReflectiveCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.ValCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.DefendedFunction0;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class.ClassProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;
import org.finos.legend.pure.runtime.java.compiled.metadata.JavaMethodWithParamsSharedPureFunction;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataAccessor;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.function.BiFunction;

public class CompiledSupport
{
    public static final CompileState CONSTRAINTS_VALIDATED = CompileState.COMPILE_EVENT_EXTRA_STATE_3;

    private static final String TEMP_TYPE_NAME = "tempTypeName";

    private static final DecimalFormat DECIMAL_FORMAT;

    static
    {
        DecimalFormat format = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        format.setMaximumFractionDigits(340); // 340 = DecimalFormat.DOUBLE_FRACTION_DIGITS
        DECIMAL_FORMAT = format;
    }


    private static final ImmutableList<Class<?>> PRIMITIVE_CLASS_COMPARISON_ORDER = Lists.immutable.with(Long.class, Double.class, PureDate.class, Boolean.class, String.class);

    public static final Comparator<Object> DEFAULT_COMPARATOR = CompiledSupport::compareInt;

    public static <T> T copy(T coreInstance)
    {
        return copy(coreInstance, (coreInstance == null ? null : ((AbstractCoreInstance) coreInstance).getSourceInformation()));
    }

    @SuppressWarnings("unchecked")
    public static <T> T copy(T coreInstance, SourceInformation sourceInformation)
    {
        if (coreInstance == null)
        {
            return null;
        }
        else
        {
            AbstractCoreInstance result = (AbstractCoreInstance) ((AbstractCoreInstance) coreInstance).copy();
            result.setSourceInformation(sourceInformation);
            return (T) result;
        }
    }

    public static <T> RichIterable<T> toPureCollection(RichIterable<T> objects)
    {
        return (objects == null) ? Lists.immutable.empty() : objects;
    }

    public static <T> RichIterable<T> toPureCollection(Iterable<T> objects)
    {
        if (objects == null)
        {
            return Lists.immutable.empty();
        }
        if (objects instanceof RichIterable)
        {
            return (RichIterable<T>) objects;
        }
        return Lists.immutable.ofAll(objects);
    }

    @SuppressWarnings("unchecked")
    public static <T> RichIterable<T> toPureCollection(T object)
    {
        if (object == null)
        {
            return Lists.immutable.empty();
        }
        // TODO remove this hack
        if (object instanceof RichIterable)
        {
            return (RichIterable<T>) object;
        }
        // TODO remove this hack
        if (object instanceof Iterable)
        {
            return toPureCollection((Iterable<T>) object);
        }
        return Lists.immutable.with(object);
    }

    public static RichIterable<String> splitOnCamelCase(String string)
    {
        return Lists.mutable.with(StringUtils.splitByCharacterTypeCamelCase(string));
    }

    public static RichIterable<String> split(Object string, Object delimiter)
    {
        String str = (String) ((string instanceof Iterable) ? Iterate.getFirst((Iterable<?>) string) : string);
        String delim = (String) ((delimiter instanceof Iterable) ? Iterate.getFirst((Iterable<?>) delimiter) : delimiter);

        MutableList<String> result = Lists.mutable.with();
        StringTokenizer tokenizer = new StringTokenizer(str, delim);
        while (tokenizer.hasMoreTokens())
        {
            result.add(tokenizer.nextToken());
        }
        return result;
    }

    public static Object safeGet(Object list, long offset, SourceInformation sourceInformation)
    {
        if (list == null)
        {
            String message = "The system is trying to get an element at offset " + offset + " where the collection is of size 0";
            throw new PureExecutionException(sourceInformation, message, Stacks.mutable.empty());
        }
        if (list instanceof ListIterable)
        {
            try
            {
                return ((ListIterable<?>) list).get((int) offset);
            }
            catch (IndexOutOfBoundsException e)
            {
                String message = "The system is trying to get an element at offset " + offset + " where the collection is of size " + ((ListIterable<?>) list).size();
                throw new PureExecutionException(sourceInformation, message, Stacks.mutable.empty());
            }
        }
        if (list instanceof List)
        {
            try
            {
                return ((List<?>) list).get((int) offset);
            }
            catch (IndexOutOfBoundsException e)
            {
                String message = "The system is trying to get an element at offset " + offset + " where the collection is of size " + ((List<?>) list).size();
                throw new PureExecutionException(sourceInformation, message, Stacks.mutable.empty());
            }
        }
        if (list instanceof Iterable)
        {
            if (offset < 0)
            {
                String message = "The system is trying to get an element at offset " + offset + " where the collection is of size " + Iterate.sizeOf((Iterable<?>) list);
                throw new PureExecutionException(sourceInformation, message, Stacks.mutable.empty());
            }
            long size = 0;
            for (Object item : (Iterable<?>) list)
            {
                if (size == offset)
                {
                    return item;
                }
                size++;
            }
            String message = "The system is trying to get an element at offset " + offset + " where the collection is of size " + size;
            throw new PureExecutionException(sourceInformation, message, Stacks.mutable.empty());
        }
        if (offset != 0)
        {
            throw new PureExecutionException(sourceInformation, "The system is trying to get an element at offset " + offset + " where the collection is of size 1", Stacks.mutable.empty());
        }
        return list;
    }

    public static int safeHashCode(boolean value)
    {
        return Boolean.hashCode(value);
    }

    public static int safeHashCode(float value)
    {
        return Float.hashCode(value);
    }

    public static int safeHashCode(double value)
    {
        return Double.hashCode(value);
    }

    public static int safeHashCode(long value)
    {
        return Long.hashCode(value);
    }

    public static int safeHashCode(int value)
    {
        return Integer.hashCode(value);
    }

    public static int safeHashCode(Object object)
    {
        if (object == null)
        {
            return 0;
        }
        if (object instanceof JavaCompiledCoreInstance)
        {
            return ((JavaCompiledCoreInstance) object).pureHashCode();
        }
        if (object instanceof Iterable)
        {
            int hashCode = 0;
            for (Object item : (Iterable<?>) object)
            {
                hashCode = 31 * hashCode + safeHashCode(item);
            }
            return hashCode;
        }
        return object.hashCode();
    }

    public static <T> T toOne(T object, SourceInformation sourceInformation)
    {
        return toOneWithMessage(object, null, sourceInformation);
    }

    public static <T> T toOne(RichIterable<? extends T> objects, SourceInformation sourceInformation)
    {
        return toOneWithMessage(objects, null, sourceInformation);
    }

    @SuppressWarnings("unchecked")
    public static <T> T toOneWithMessage(T object, String message, SourceInformation sourceInformation)
    {
        if (object == null)
        {
            throw new PureExecutionException(sourceInformation, message != null ? message : "Cannot cast a collection of size 0 to multiplicity [1]", Stacks.mutable.empty());
        }
        if (object instanceof RichIterable)
        {
            return toOne((RichIterable<? extends T>) object, sourceInformation);
        }
        return object;
    }

    public static <T> T toOneWithMessage(RichIterable<? extends T> objects, String message, SourceInformation sourceInformation)
    {
        int size = (objects == null) ? 0 : objects.size();
        if (size != 1)
        {
            throw new PureExecutionException(sourceInformation, message != null ? message : "Cannot cast a collection of size " + size + " to multiplicity [1]", Stacks.mutable.empty());
        }
        return objects.getAny();
    }

    public static SourceInformation getSourceInformation(String sourceIdForError, int line, int column)
    {
        return sourceIdForError == null ? null : new SourceInformation(sourceIdForError, -1, -1, line, column, -1, -1);
    }

    public static Object makeOne(Object object)
    {
        return makeOne(object, null);
    }

    public static Object makeOne(Object object, SourceInformation sourceInfo)
    {
        return (object instanceof Iterable) ? makeOne((Iterable<?>) object, sourceInfo) : object;
    }

    public static <T> T makeOne(Iterable<? extends T> object)
    {
        return makeOne(object, null);
    }

    public static <T> T makeOne(Iterable<? extends T> object, SourceInformation sourceInfo)
    {
        if (object == null)
        {
            return null;
        }

        Iterator<? extends T> iterator = object.iterator();
        if (!iterator.hasNext())
        {
            return null;
        }
        T value = iterator.next();
        if (iterator.hasNext())
        {
            throw new PureExecutionException(sourceInfo, "Expected at most one object, but found many", Stacks.mutable.empty());
        }
        return value;
    }

    public static <T> T toMultiplicityOne(T object, int lowerBound, int upperBound, SourceInformation sourceInformation)
    {
        if (object == null && lowerBound > 0)
        {
            throw new PureExecutionException(sourceInformation, "Cannot cast a collection of size 0 to multiplicity " + print(lowerBound, upperBound), Stacks.mutable.empty());
        }
        if (object instanceof RichIterable)
        {
            int size = ((RichIterable<?>) object).size();
            if (size > 1)
            {
                throw new PureExecutionException(sourceInformation, "Cannot cast a collection of size " + size + " to multiplicity " + print(lowerBound, upperBound), Stacks.mutable.empty());
            }
        }
        return object;
    }

    public static <T> RichIterable<T> toMultiplicityMany(T object, int lowerBound, int upperBound, SourceInformation sourceInformation)
    {
        if (object == null && lowerBound > 0)
        {
            throw new PureExecutionException(sourceInformation, "Cannot cast a collection of size 0 to multiplicity " + print(lowerBound, upperBound), Stacks.mutable.empty());
        }
        if (object instanceof RichIterable)
        {
            int size = ((RichIterable<?>) object).size();
            if (lowerBound > size || (upperBound != -1 && size > upperBound))
            {
                throw new PureExecutionException(sourceInformation, "Cannot cast a collection of size " + size + " to multiplicity " + print(lowerBound, upperBound), Stacks.mutable.empty());
            }
        }
        return CompiledSupport.toPureCollection(object);
    }

    private static String print(int lowerBound, int upperBound)
    {
        if (lowerBound == upperBound)
        {
            return "[" + lowerBound + "]";
        }
        return "[" + lowerBound + ".." + (upperBound == -1 ? "*" : upperBound) + "]";
    }

    public static <T> RichIterable<T> toOneMany(T object, SourceInformation sourceInformation)
    {
        return toOneManyWithMessage(object, null, sourceInformation);
    }

    public static <T> RichIterable<T> toOneMany(RichIterable<T> objects, SourceInformation sourceInformation)
    {
        return toOneManyWithMessage(objects, null, sourceInformation);
    }

    @SuppressWarnings("unchecked")
    public static <T> RichIterable<T> toOneManyWithMessage(T object, String message, SourceInformation sourceInformation)
    {
        if (object == null)
        {
            throw new PureExecutionException(sourceInformation, message != null ? message : "Cannot cast a collection of size 0 to multiplicity [1..*]", Stacks.mutable.empty());
        }
        // TODO remove this hack
        if (object instanceof RichIterable)
        {
            if (((RichIterable<?>) object).isEmpty())
            {
                throw new PureExecutionException(sourceInformation, message != null ? message : "Cannot cast a collection of size 0 to multiplicity [1..*]", Stacks.mutable.empty());
            }
            return (RichIterable<T>) object;
        }
        // TODO remove this hack
        if (object instanceof Iterable)
        {
            if (Iterate.isEmpty((Iterable<?>) object))
            {
                throw new PureExecutionException(sourceInformation, message != null ? message : "Cannot cast a collection of size 0 to multiplicity [1..*]", Stacks.mutable.empty());
            }
            return toPureCollection((Iterable<T>) object);
        }
        return Lists.immutable.with(object);
    }

    public static <T> RichIterable<T> toOneManyWithMessage(RichIterable<T> objects, String message, SourceInformation sourceInformation)
    {
        if (Iterate.isEmpty(objects))
        {
            throw new PureExecutionException(sourceInformation, message != null ? message : "Cannot cast a collection of size 0 to multiplicity [1..*]", Stacks.mutable.empty());
        }
        return objects;
    }

    public static boolean isEmpty(RichIterable<?> objects)
    {
        return (objects == null) || objects.isEmpty();
    }

    public static boolean isEmpty(Object object)
    {
        return (object == null) || ((object instanceof Iterable) && Iterate.isEmpty((Iterable<?>) object));
    }


    public static <T> RichIterable<T> init(T elem)
    {
        return Lists.immutable.empty();
    }

    public static <T> RichIterable<T> init(RichIterable<T> list)
    {
        if (list == null)
        {
            return Lists.immutable.empty();
        }

        int size = list.size();
        return (size <= 1) ?
                Lists.immutable.empty() :
                Lists.mutable.<T>ofInitialCapacity(size - 1).withAll(LazyIterate.take(list, size - 1));
    }

    public static <T> RichIterable<T> tail(T list)
    {
        return Lists.immutable.empty();
    }

    public static <T> RichIterable<T> tail(RichIterable<T> list)
    {
        if (list == null)
        {
            return Lists.immutable.empty();
        }

        if (list instanceof LazyIterable)
        {
            return list.isEmpty() ? Lists.immutable.empty() : ((LazyIterable<T>) list).drop(1);
        }

        int size = list.size();
        return (size <= 1) ?
                Lists.immutable.empty() :
                Lists.mutable.<T>ofInitialCapacity(size - 1).withAll(LazyIterate.drop(list, 1));
    }

    public static <T> long compare(T left, T right)
    {
        return compareInt(left, right);
    }

    @SuppressWarnings("unchecked")
    private static <T> int compareInt(T left, T right)
    {
        if (left == right)
        {
            return 0;
        }

        Class<?> leftClass = left.getClass();
        Class<?> rightClass = right.getClass();
        if (leftClass != rightClass)
        {
            if ((left instanceof Number) && (right instanceof Number))
            {
                return compareUnmatchedNumbers((Number) left, (Number) right);
            }
            if ((left instanceof PureDate) && (right instanceof PureDate))
            {
                return ((PureDate) left).compareTo((PureDate) right);
            }

            int leftIndex = PRIMITIVE_CLASS_COMPARISON_ORDER.indexOf(leftClass);
            int rightIndex = PRIMITIVE_CLASS_COMPARISON_ORDER.indexOf(rightClass);

            if (leftIndex == -1)
            {
                return (rightIndex == -1) ? leftClass.getCanonicalName().compareTo(rightClass.getCanonicalName()) : 1;
            }

            return (rightIndex == -1) ? -1 : Integer.compare(leftIndex, rightIndex);
        }

        if (left instanceof Comparable)
        {
            return ((Comparable<? super T>) left).compareTo(right);
        }

        // TODO maybe do something smarter here
        return Integer.compare(safeHashCode(left), safeHashCode(right));
    }

    private static int compareUnmatchedNumbers(Number x, Number y)
    {
        if (isSpecial(x) || isSpecial(y))
        {
            return Double.compare(x.doubleValue(), y.doubleValue());
        }

        return toBigDecimal(x).compareTo(toBigDecimal(y));
    }

    private static boolean isSpecial(Number number)
    {
        if (number instanceof Double)
        {
            Double d = (Double) number;
            return d.isNaN() || d.isInfinite();
        }
        if (number instanceof Float)
        {
            Float f = (Float) number;
            return f.isNaN() || f.isInfinite();
        }
        return false;
    }

    private static BigDecimal toBigDecimal(Number number)
    {
        if (number instanceof BigDecimal)
        {
            return (BigDecimal) number;
        }
        if (number instanceof BigInteger)
        {
            return new BigDecimal((BigInteger) number);
        }
        if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long)
        {
            return new BigDecimal(number.longValue());
        }
        if (number instanceof Float || number instanceof Double)
        {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try
        {
            return new BigDecimal(number.toString());
        }
        catch (NumberFormatException e)
        {
            throw new RuntimeException("The given number (\"" + number + "\" of class " + number.getClass().getName() + ") does not have a parsable string representation", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> RichIterable<T> add(RichIterable<? extends T> list, T element)
    {
        if (element == null)
        {
            return (RichIterable<T>) list;
        }
        MutableList<T> newList = (list == null) ? Lists.mutable.empty() : Lists.mutable.withAll(list);
        newList.add((element instanceof Iterable) ? Iterate.getFirst((Iterable<T>) element) : element);
        return newList;
    }

    @SuppressWarnings("unchecked")
    public static <T> RichIterable<T> add(RichIterable<? extends T> list, long index, T element)
    {
        if (element == null)
        {
            return (RichIterable<T>) list;
        }
        MutableList<T> newList = (list == null) ? Lists.mutable.empty() : Lists.mutable.withAll(list);
        newList.add((int) index, (element instanceof Iterable) ? Iterate.getFirst((Iterable<T>) element) : element);
        return newList;
    }

    @SuppressWarnings("rawtypes")
    public static RichIterable<?> concatenate(Object list1, Object list2)
    {
        if ((list1 == null) && (list2 == null))
        {
            return Lists.immutable.empty();
        }
        if (list1 == null)
        {
            return (list2 instanceof RichIterable) ? (RichIterable<?>) list2 : Lists.immutable.with(list2);
        }
        if (list2 == null)
        {
            return (list1 instanceof RichIterable) ? (RichIterable<?>) list1 : Lists.immutable.with(list1);
        }

        if ((list1 instanceof LazyIterable) || (list2 instanceof LazyIterable))
        {
            Iterable it1 = (list1 instanceof Iterable) ? (Iterable) list1 : Lists.immutable.with(list1);
            Iterable it2 = (list2 instanceof Iterable) ? (Iterable) list2 : Lists.immutable.with(list2);
            return LazyIterate.concatenate(it1, it2);
        }

        MutableList<Object> result = Lists.mutable.empty();
        if (list1 instanceof Iterable)
        {
            result.addAllIterable((Iterable<?>) list1);
        }
        else
        {
            result.add(list1);
        }
        if (list2 instanceof Iterable)
        {
            result.addAllIterable((Iterable<?>) list2);
        }
        else
        {
            result.add(list2);
        }
        return result;
    }

    public static RichIterable<Long> range(long start, long stop, long step, SourceInformation sourceInformation)
    {
        if (step == 0)
        {
            throw new PureExecutionException(sourceInformation, "range step must not be 0", Stacks.mutable.empty());
        }

        if ((step > 0) ? (start >= stop) : (start <= stop))
        {
            return Lists.immutable.empty();
        }

        long longSize = ((stop - start - Long.signum(step)) / step) + 1L;
        if (longSize > Integer.MAX_VALUE)
        {
            throw new PureExecutionException(sourceInformation, "range [" + start + ":" + stop + ":" + step + "] too long: " + longSize, Stacks.mutable.empty());
        }
        MutableList<Long> result = Lists.mutable.ofInitialCapacity((int) longSize);
        for (long i = start; (step > 0) ? (i < stop) : (i > stop); i += step)
        {
            result.add(i);
        }
        return result;
    }

    public static <T, V> RichIterable<? extends T> mapToManyOverMany(RichIterable<? extends V> collection, BiFunction<? super V, ExecutionSupport, ? extends Iterable<? extends T>> function, ExecutionSupport executionSupport)
    {
        return (collection == null) ? Lists.mutable.empty() : collection.flatCollect(e -> (Iterable<? extends T>) function.apply(e, executionSupport));
    }

    public static <T, V> RichIterable<? extends T> mapToOneOverMany(RichIterable<? extends V> collection, BiFunction<? super V, ExecutionSupport, T> function, ExecutionSupport executionSupport)
    {
        if (collection == null)
        {
            return Lists.mutable.empty();
        }

        if (collection instanceof LazyIterable)
        {
            return collection.collect(e -> function.apply(e, executionSupport)).select(Objects::nonNull);
        }

        MutableList<T> result = Lists.mutable.ofInitialCapacity(collection.size());
        collection.forEach(e ->
        {
            T value = function.apply(e, executionSupport);
            if (value != null)
            {
                result.add(value);
            }
        });
        return result;
    }

    public static <T, V> RichIterable<? extends T> mapToManyOverOne(V element, BiFunction<? super V, ExecutionSupport, ? extends RichIterable<? extends T>> function, ExecutionSupport executionSupport)
    {
        return (element == null) ? Lists.mutable.empty() : function.apply(element, executionSupport);
    }

    public static <T, V> T mapToOneOverOne(V element, BiFunction<? super V, ExecutionSupport, T> function, ExecutionSupport executionSupport)
    {
        return (element == null) ? null : function.apply(element, executionSupport);
    }

    public static <T, V> V fold(RichIterable<? extends T> value, Function2<V, T, ? extends V> function, V accumulator)
    {
        return value == null ? accumulator : value.injectInto(accumulator, function);
    }

    public static <T> T first(RichIterable<T> list)
    {
        return Iterate.isEmpty(list) ? null : list.getFirst();
    }

    public static <T> T first(T instance)
    {
        return instance;
    }


    public static int abs(int n)
    {
        return Math.abs(n);
    }

    public static long abs(long n)
    {
        return Math.abs(n);
    }

    public static float abs(float n)
    {
        return Math.abs(n);
    }

    public static double abs(double n)
    {
        return Math.abs(n);
    }

    public static Integer abs(Integer n)
    {
        return Math.abs(n);
    }

    public static Long abs(Long n)
    {
        return Math.abs(n);
    }

    public static Float abs(Float n)
    {
        return Math.abs(n);
    }

    public static Double abs(Double n)
    {
        return Math.abs(n);
    }

    public static BigDecimal abs(BigDecimal bd)
    {
        return bd.abs();
    }

    public static BigInteger abs(BigInteger bi)
    {
        return bi.abs();
    }

    public static Number abs(Number n)
    {
        if (n instanceof Integer)
        {
            return Math.abs(n.intValue());
        }
        if (n instanceof Long)
        {
            return Math.abs(n.longValue());
        }
        if (n instanceof Float)
        {
            return Math.abs(n.floatValue());
        }
        if (n instanceof Double)
        {
            return Math.abs(n.doubleValue());
        }
        if (n instanceof BigDecimal)
        {
            return ((BigDecimal) n).abs();
        }
        if (n instanceof BigInteger)
        {
            return ((BigInteger) n).abs();
        }
        throw new IllegalArgumentException("Unhandled Number Type " + n);
    }


    public static Object print(ConsoleCompiled console, Object content, Long max)
    {
        console.print(content, max.intValue());
        return null;
    }

    public static String joinStrings(Object strings, String prefix, String separator, String suffix)
    {
        return (strings instanceof RichIterable) ? joinStrings((RichIterable<String>) strings, prefix, separator, suffix) : joinStrings((String) strings, prefix, separator, suffix);
    }

    public static String joinStrings(String string, String prefix, String separator, String suffix)
    {
        return prefix + string + suffix;
    }

    public static String joinStrings(RichIterable<String> strings, String prefix, String separator, String suffix)
    {
        return ((strings == null) || strings.isEmpty()) ? (prefix + suffix) : strings.makeString(prefix, separator, suffix);
    }

    public static String format(String formatString, Object formatArgs, BiFunction<Object, ? super ExecutionSupport, ? extends String> toRepresentationFunction, ExecutionSupport executionSupport)
    {
        return PureStringFormat.format(formatString, (formatArgs instanceof Iterable) ? (Iterable<?>) formatArgs : Lists.immutable.with(formatArgs), toRepresentationFunction, executionSupport);
    }

    /**
     * Return a copy of collection sorted by applying comparator
     * to the results of applying keyFunction to the elements of
     * collection.
     *
     * @param collection collection to sort
     * @param keyFn      key function
     * @param comp       comparator
     * @param <T>        collection element type
     * @return sorted collection
     */
    public static <T> RichIterable<T> toSorted(RichIterable<T> collection, SharedPureFunction<?> keyFn, SharedPureFunction<? extends Number> comp, ExecutionSupport es)
    {
        if (collection == null)
        {
            return Lists.immutable.empty();
        }

        if (keyFn == null)
        {
            Comparator<T> comparator = (comp == null) ?
                    CompiledSupport::compareInt :
                    (left, right) -> comp.execute(Lists.immutable.with(left, right), es).intValue();
            return collection.toSortedList(comparator);
        }

        class ElementWithKey
        {
            private final T value;
            private Object key;
            private boolean keyComputed = false;

            private ElementWithKey(T value)
            {
                this.value = value;
            }

            Object getKey()
            {
                if (!this.keyComputed)
                {
                    this.key = keyFn.execute(Lists.immutable.with(this.value), es);
                    this.keyComputed = true;
                }
                return this.key;
            }

            T getValue()
            {
                return this.value;
            }
        }

        Comparator<ElementWithKey> comparator = (comp == null) ?
                (left, right) -> compareInt(left.getKey(), right.getKey()) :
                (left, right) -> comp.execute(Lists.immutable.with(left.getKey(), right.getKey()), es).intValue();
        return collection.collect(ElementWithKey::new, Lists.mutable.empty())
                .sortThis(comparator)
                .collect(ElementWithKey::getValue);
    }


    /**
     * Implementation of the Pure "eq" function.  This returns
     * true if left and right are identical (point equality) or
     * if they are both Pure primitives (instances of the Pure
     * types String, Integer, Float, Date, or Boolean) and have
     * the same value.
     *
     * @param left  left instance
     * @param right right instance
     * @return eq(left, right)
     */
    public static boolean eq(Object left, Object right)
    {
        if (left == right)
        {
            return true;
        }
        if ((left == null) || (right == null))
        {
            return false;
        }
        if (left.getClass() != right.getClass())
        {
            return false;
        }
        if (left instanceof Number)
        {
            return eq((Number) left, (Number) right);
        }
        if ((left instanceof String) || (left instanceof PureDate) || (left instanceof QuantityCoreInstance))
        {
            return left.equals(right);
        }
        return false;
    }

    public static boolean eq(Number left, Number right)
    {
        // TODO make this more sophisticated
        if (left instanceof BigDecimal && right instanceof Double ||
                left instanceof Double && right instanceof BigDecimal)
        {
            return false;
        }

        if ((left instanceof Byte) || (right instanceof Byte))
        {
            return (left.getClass() == right.getClass()) && (left.byteValue() == right.byteValue());
        }

        left = left.equals(-0.0d) ? 0.0d : left;
        right = right.equals(-0.0d) ? 0.0d : right;
        return left.equals(right) || left.toString().equals(right.toString());
    }

    public static boolean eq_Integer_1(long left, long right)
    {
        return left == right;
    }

    public static boolean eq_Float_1(double left, double right)
    {
        return left == right;
    }


    public static boolean equal(Object left, Object right) //NOSONAR Function signature avoids confusion
    {
        if (left == right)
        {
            return true;
        }
        if (left == null)
        {
            return (right instanceof RichIterable) && ((RichIterable<?>) right).isEmpty();
        }
        if (right == null)
        {
            return (left instanceof RichIterable) && ((RichIterable<?>) left).isEmpty();
        }
        if (left instanceof LazyIterable)
        {
            Iterator<?> leftIterator = ((Iterable<?>) left).iterator();
            if (right instanceof Iterable)
            {
                return iteratorsEqual(leftIterator, ((Iterable<?>) right).iterator());
            }
            if (!leftIterator.hasNext())
            {
                return false;
            }
            Object leftFirst = leftIterator.next();
            return !leftIterator.hasNext() && equal(leftFirst, right);
        }
        if (right instanceof LazyIterable)
        {
            Iterator<?> rightIterator = ((Iterable<?>) right).iterator();
            if (left instanceof Iterable)
            {
                return iteratorsEqual(((Iterable<?>) left).iterator(), rightIterator);
            }
            if (!rightIterator.hasNext())
            {
                return false;
            }
            Object rightFirst = rightIterator.next();
            return !rightIterator.hasNext() && equal(left, rightFirst);
        }
        if (left instanceof RichIterable)
        {
            RichIterable<?> leftList = (RichIterable<?>) left;
            int size = leftList.size();
            if (right instanceof RichIterable)
            {
                RichIterable<?> rightList = (RichIterable<?>) right;
                return (size == rightList.size()) && iteratorsEqual(leftList.iterator(), rightList.iterator());
            }
            return (size == 1) && equal(leftList.getAny(), right);
        }
        if (right instanceof RichIterable)
        {
            RichIterable<?> rightList = (RichIterable<?>) right;
            return (rightList.size() == 1) && equal(left, rightList.getAny());
        }
        if (left instanceof Number)
        {
            return (right instanceof Number) && eq((Number) left, (Number) right);
        }

        if (left instanceof JavaCompiledCoreInstance)
        {
            return ((JavaCompiledCoreInstance) left).pureEquals(right);
        }
        if (right instanceof JavaCompiledCoreInstance)
        {
            return ((JavaCompiledCoreInstance) right).pureEquals(left);
        }

        return left.equals(right);
    }

    private static boolean iteratorsEqual(Iterator<?> leftIterator, Iterator<?> rightIterator)
    {
        while (leftIterator.hasNext() && rightIterator.hasNext())
        {
            if (!equal(leftIterator.next(), rightIterator.next()))
            {
                return false;
            }
        }
        return !leftIterator.hasNext() && !rightIterator.hasNext();
    }

    public static boolean jsonStringEquals(String left, String right, SourceInformation sourceInformation)
    {
        try
        {
            JSONParser jp = new JSONParser();
            Object deserializedLeft = jp.parse(left);
            Object deserializedRight = jp.parse(right);
            return deserializedLeft.equals(deserializedRight);
        }
        catch (ParseException parseException)
        {
            throw new PureExecutionException(sourceInformation, "Failed to parse JSON string. Invalid JSON string. " + parseException.toString(), parseException, Stacks.mutable.empty());
        }
    }

    public static String pureToString(boolean value, ExecutionSupport es)
    {
        return primitiveToString(value);
    }

    public static String pureToString(byte value, ExecutionSupport es)
    {
        return primitiveToString(value);
    }

    public static String pureToString(int value, ExecutionSupport es)
    {
        return primitiveToString(value);
    }

    public static String pureToString(long value, ExecutionSupport es)
    {
        return primitiveToString(value);
    }

    public static String pureToString(float value, ExecutionSupport es)
    {
        return primitiveToString(value);
    }

    public static String pureToString(double value, ExecutionSupport es)
    {
        return primitiveToString(value);
    }

    public static String pureToString(BigDecimal value, ExecutionSupport es)
    {
        return primitiveToString(value);
    }

    public static String pureToString(Number value, ExecutionSupport es)
    {
        return primitiveToString(value);
    }

    public static String pureToString(PureDate value, ExecutionSupport es)
    {
        return value.toString();
    }

    public static String pureToString(String value, ExecutionSupport es)
    {
        return primitiveToString(value);
    }

    public static String pureToString(ReflectiveCoreInstance value, ExecutionSupport es)
    {
        return value.toString(es);
    }

    public static String pureToString(Object instance, ExecutionSupport es)
    {
        if (instance == null)
        {
            return "NULL";
        }
        if (instance instanceof Boolean)
        {
            return pureToString(((Boolean) instance).booleanValue(), es);
        }
        if (instance instanceof Number)
        {
            return pureToString((Number) instance, es);
        }
        if (instance instanceof PureDate)
        {
            return pureToString((PureDate) instance, es);
        }
        if (instance instanceof String)
        {
            return pureToString((String) instance, es);
        }
        if (instance instanceof JavaCompiledCoreInstance)
        {
            return ((JavaCompiledCoreInstance) instance).toString(es);
        }
        if (instance instanceof BaseCoreInstance)
        {
            String id = ((CoreInstance) instance).getName();
            return ModelRepository.possiblyReplaceAnonymousId(id);
        }

        try
        {
            Method method = instance.getClass().getDeclaredMethod("toString", ExecutionSupport.class);
            try
            {
                return (String) method.invoke(instance, es);
            }
            catch (IllegalAccessException | InvocationTargetException e)
            {
                throw new RuntimeException(e);
            }
        }
        catch (NoSuchMethodException e)
        {
            try
            {
                return ConsoleCompiled.getId(instance);
            }
            catch (IllegalAccessException e1)
            {
                throw new RuntimeException(e1);
            }
        }
    }

    public static String primitiveToString(boolean value)
    {
        return value ? ModelRepository.BOOLEAN_TRUE : ModelRepository.BOOLEAN_FALSE;
    }

    public static String primitiveToString(byte value)
    {
        return Byte.toString(value);
    }

    public static String primitiveToString(int value)
    {
        return Integer.toString(value);
    }

    public static String primitiveToString(long value)
    {
        return Long.toString(value);
    }

    public static String primitiveToString(float value)
    {
        return primitiveToString((double) value);
    }

    public static String primitiveToString(double value)
    {
        return (value == 0.0d) ? "0.0" : DECIMAL_FORMAT.format(value);
    }

    public static String primitiveToString(BigDecimal value)
    {
        return value.toPlainString();
    }

    public static String primitiveToString(PureDate value)
    {
        return value.toString();
    }

    public static String primitiveToString(Number value)
    {
        if ((value instanceof Float) || (value instanceof Double))
        {
            return primitiveToString(value.doubleValue());
        }
        if (value instanceof BigDecimal)
        {
            return primitiveToString((BigDecimal) value);
        }
        return value.toString();
    }

    public static String primitiveToString(String value)
    {
        return value;
    }

    public static String primitiveToString(Object value)
    {
        if (value instanceof Boolean)
        {
            return primitiveToString(((Boolean) value).booleanValue());
        }
        if (value instanceof Number)
        {
            return primitiveToString((Number) value);
        }
        if (value instanceof PureDate)
        {
            return primitiveToString((PureDate) value);
        }
        if (value instanceof String)
        {
            return primitiveToString((String) value);
        }
        throw new IllegalArgumentException("Unhandled primitive: " + value + " (" + value.getClass() + ")");
    }


    public static <T extends Number> T plus(T number)
    {
        return number;
    }

    public static long plus(long number)
    {
        return number;
    }

    public static double plus(double number)
    {
        return number;
    }

    public static long plus(Long left, Long right)
    {
        return left + right;
    }

    public static Number plus(Number left, Number right)
    {
        if (((left instanceof Long) || (left instanceof Integer)) && ((right instanceof Long) || (right instanceof Integer)))
        {
            return left.longValue() + right.longValue();
        }
        if ((left instanceof BigDecimal) || (right instanceof BigDecimal))
        {
            return toBigDecimal(left).add(toBigDecimal(right));
        }
        return left.doubleValue() + right.doubleValue();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Number> T plus(RichIterable<T> numbers)
    {
        Number sum = 0L;
        for (Number n : numbers)
        {
            sum = plus(sum, n);
        }
        return (T) sum;
    }

    public static Number minus(Number number)
    {
        if (number instanceof BigDecimal)
        {
            return ((BigDecimal) number).negate();
        }
        if ((number instanceof Long) || (number instanceof Integer))
        {
            return -number.longValue();
        }
        return -number.doubleValue();
    }

    public static long minus(long integer)
    {
        return -integer;
    }

    public static double minus(double number)
    {
        return -number;
    }

    public static Number minus(Number left, Number right)
    {
        if ((left instanceof Long) && (right instanceof Long))
        {
            return left.longValue() - right.longValue();
        }
        if ((left instanceof BigDecimal) || (right instanceof BigDecimal))
        {
            return toBigDecimal(left).subtract(toBigDecimal(right));
        }
        return left.doubleValue() - right.doubleValue();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Number> T minus(ListIterable<T> numbers)
    {
        int size = numbers.size();
        switch (size)
        {
            case 0:
            {
                return (T) Long.valueOf(0L);
            }
            case 1:
            {
                return (T) minus(numbers.get(0));
            }
            default:
            {
                return (T) numbers.injectInto((Number) null, (result, n) -> (result == null) ? n : minus(result, n));
            }
        }
    }

    public static <T extends Number> T minus(RichIterable<T> numbers)
    {
        return minus((ListIterable<T>) numbers);
    }

    public static <T extends Number> T times(T number)
    {
        return number;
    }

    public static long times(long integer)
    {
        return integer;
    }

    public static double times(double number)
    {
        return number;
    }

    public static Number times(Number left, Number right)
    {
        if (((left instanceof Long) || (left instanceof Integer)) && ((right instanceof Long) || (right instanceof Integer)))
        {
            return left.longValue() * right.longValue();
        }
        if ((left instanceof BigDecimal) || (right instanceof BigDecimal))
        {
            return toBigDecimal(left).multiply(toBigDecimal(right));
        }
        return left.doubleValue() * right.doubleValue();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Number> T times(RichIterable<T> numbers)
    {
        Number product = 1L;
        for (Number number : numbers)
        {
            product = times(product, number);
        }
        return (T) product;
    }


    public static Double divide(Number left, Number right, SourceInformation sourceInformation)
    {
        if (right.doubleValue() == 0)
        {
            throw new PureExecutionException(sourceInformation, "Cannot divide " + right + " by zero", Stacks.mutable.empty());
        }

        if ((left instanceof BigDecimal) || (right instanceof BigDecimal))
        {
            return toBigDecimal(left).divide(toBigDecimal(right), RoundingMode.HALF_UP).doubleValue();
        }

        return left.doubleValue() / right.doubleValue();
    }

    public static BigDecimal divideDecimal(BigDecimal left, BigDecimal right, long scale)
    {
        return left.divide(right, (int) scale, RoundingMode.HALF_UP);
    }

    public static boolean lessThan(Number left, Number right)
    {
        if ((left instanceof BigDecimal) || (right instanceof BigDecimal))
        {
            return toBigDecimal(left).compareTo(toBigDecimal(right)) < 0;
        }

        if (((left instanceof Long) || (left instanceof Integer)) && ((right instanceof Long) || (right instanceof Integer)))
        {
            return left.longValue() < right.longValue();
        }

        return left.doubleValue() < right.doubleValue();
    }

    public static boolean lessThanEqual(Number left, Number right)
    {
        if ((left instanceof BigDecimal) || (right instanceof BigDecimal))
        {
            return toBigDecimal(left).compareTo(toBigDecimal(right)) <= 0;
        }

        if (((left instanceof Long) || (left instanceof Integer)) && ((right instanceof Long) || (right instanceof Integer)))
        {
            return left.longValue() <= right.longValue();
        }

        return left.doubleValue() <= right.doubleValue();
    }

    public static Long indexOf(String str, String toFind)
    {
        return (long) str.indexOf(toFind);
    }

    public static Long indexOf(String str, String toFind, Number from)
    {
        return (long) str.indexOf(toFind, from.intValue());
    }

    public static String substring(String str, Number start)
    {
        return str.substring(start.intValue());
    }

    public static String substring(String str, Number start, Number end)
    {
        return end == null
                ? str.substring(start.intValue())
                : str.substring(start.intValue(), end.intValue());
    }

    public static boolean startsWith(String str1, String str2)
    {
        return str1.startsWith(str2);
    }


    public static String replace(String str1, String str2, String str3)
    {
        return str1.replace(str2, str3);
    }

    public static long length(String str)
    {
        return str.length();
    }


    public static boolean pureAssert(boolean condition, SharedPureFunction<? extends String> function, SourceInformation sourceInformation, ExecutionSupport es)
    {
        if (!condition)
        {
            String message = function.execute(Lists.immutable.empty(), es);
            throw new PureAssertFailException(sourceInformation, message, Stacks.mutable.empty());
        }
        return true;
    }

    public static Object matchFailure(Object obj, SourceInformation sourceInformation)
    {
        throw new PureExecutionException(sourceInformation,
                "Match failure: " + (obj == null ? null : ((obj instanceof RichIterable) ? ((RichIterable<?>) obj).collect(o -> o == null ? null : getErrorMessageForMatchFunctionBasedOnObjectType(o)).makeString("[", ", ", "]") : getErrorMessageForMatchFunctionBasedOnObjectType(obj))), Stacks.mutable.empty());
    }

    private static String getErrorMessageForMatchFunctionBasedOnObjectType(Object obj)
    {
        if (isTypeOfEnum(obj))
        {
            Enum enumVal = (Enum) obj;
            return enumVal._name() + " instanceOf " + getEnumClassifierName(enumVal);
        }
        if (obj instanceof BaseCoreInstance)
        {
            return obj.toString();
        }
        if (isPureGeneratedClass(obj))
        {
            String tempTypeName = getPureGeneratedClassName(obj);
            return tempTypeName + "Object instanceOf " + tempTypeName;
        }

        String primitiveJavaToPureType = JavaPurePrimitiveTypeMapping.getPureM3TypeFromJavaPrimitivesAndDates(obj);
        return obj + " instanceOf " + ((primitiveJavaToPureType == null) ? obj.getClass() : primitiveJavaToPureType);
    }

    private static String getEnumClassifierName(Enum obj)
    {
        String classifierPath = obj.getFullSystemPath();
        int index = classifierPath.lastIndexOf(':');
        return classifierPath.substring(index + 1);
    }

    public static String getPureGeneratedClassName(Object obj)
    {
        return getFieldValue(obj, TEMP_TYPE_NAME);
    }

    private static boolean hasField(Object obj, String fieldName)
    {
        try
        {
            obj.getClass().getField(fieldName);
            return true;
        }
        catch (NoSuchFieldException ignore)
        {
            return false;
        }
    }

    private static boolean isTypeOfEnum(Object obj)
    {
        return obj instanceof Enum;
    }

    private static boolean isPureGeneratedClass(Object obj)
    {
        return hasField(obj, TEMP_TYPE_NAME) ||
                obj instanceof Class && ((Class<?>) obj).getCanonicalName().contains(JavaPackageAndImportBuilder.buildPackageFromSystemPath(null));
    }

    private static String getFieldValue(Object obj, String fieldName)
    {
        try
        {
            Field f = obj.getClass().getField(fieldName);
            f.setAccessible(true);
            return (String) f.get(obj);
        }
        catch (IllegalAccessException | NoSuchFieldException ignore)
        {
            return null;
        }
    }


    public static Object dynamicallyBuildLambdaFunction(CoreInstance lambdaFunction, ExecutionSupport es)
    {
        ClassLoader globalClassLoader = ((CompiledExecutionSupport) es).getClassLoader();
        CompiledProcessorSupport compiledSupport = new CompiledProcessorSupport(globalClassLoader, ((CompiledExecutionSupport) es).getMetadata(), ((CompiledExecutionSupport) es).getExtraSupportedTypes());
        ProcessorContext processorContext = new ProcessorContext(compiledSupport);
        processorContext.setInLineAllLambda(true);

        String name = "DynamicLambdaGeneration";
        String _class = JavaSourceCodeGenerator.imports + "\nimport " + JavaPackageAndImportBuilder.rootPackage() + ".*;\npublic class " + name + "{" +
                "   public static PureCompiledLambda build(final MutableMap<String, Object> valMap, final IntObjectMap<CoreInstance> localLambdas){\n" +
                "return " + ValueSpecificationProcessor.processLambda(null, lambdaFunction, compiledSupport, processorContext) + ";" +
                "}" +
                "}";

        MemoryFileManager fileManager = ((CompiledExecutionSupport) es).getMemoryFileManager();
        MutableList<StringJavaSource> javaClasses = Lists.mutable.empty();
        javaClasses.add(StringJavaSource.newStringJavaSource("temp", name, _class));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        MemoryFileManager manager = new MemoryFileManager(compiler, fileManager, null);

        try
        {
            PureJavaCompiler.compile(compiler, javaClasses, manager);
        }
        catch (PureJavaCompileException e)
        {
            throw new RuntimeException(e);
        }

        ClassLoader cl = new MemoryClassLoader(manager, globalClassLoader);
        try
        {
            Class<?> realClass = cl.loadClass("temp" + "." + name);
            return realClass.getMethod("build", MutableMap.class, IntObjectMap.class).invoke(null, processorContext.getObjectToPassToDynamicallyGeneratedCode(), processorContext.getLocalLambdas());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }


    public static Object dynamicallyEvaluateValueSpecification(CoreInstance valueSpecification, PureMap lambdaOpenVariablesMap, ExecutionSupport es)
    {
        MemoryFileManager fileManager = ((CompiledExecutionSupport) es).getMemoryFileManager();
        ClassLoader globalClassLoader = ((CompiledExecutionSupport) es).getClassLoader();

        CompiledProcessorSupport compiledSupport = new CompiledProcessorSupport(globalClassLoader, ((CompiledExecutionSupport) es).getMetadata(), ((CompiledExecutionSupport) es).getExtraSupportedTypes());
        ProcessorContext processorContext = new ProcessorContext(compiledSupport);

        // Don't do anything if the ValueSpecification is already resolved ----------------
        if (Instance.instanceOf(valueSpecification, M3Paths.InstanceValue, processorContext.getSupport()))
        {
            ListIterable<? extends CoreInstance> l = valueSpecification.getValueForMetaPropertyToMany(M3Properties.values);
            if (l.noneSatisfy(instance -> Instance.instanceOf(instance, M3Paths.ValueSpecification, processorContext.getSupport()) || Instance.instanceOf(instance, M3Paths.LambdaFunction, processorContext.getSupport())))
            {
                ListIterable<Object> result = l.collect(instance -> instance instanceof ValCoreInstance ? ((ValCoreInstance) instance).getValue() : instance);
                return result.size() == 1 ? result.get(0) : result;
            }
        }
        //---------------------------------------------------------------------------------

        processorContext.setInLineAllLambda(true);
        String processed = ValueSpecificationProcessor.processValueSpecification(valueSpecification, true, processorContext);
        String returnType = TypeProcessor.typeToJavaObjectWithMul(valueSpecification.getValueForMetaPropertyToOne(M3Properties.genericType), valueSpecification.getValueForMetaPropertyToOne(M3Properties.multiplicity), false, compiledSupport);

        String name = "DynaClass";
        RichIterable<Pair<String, CoreInstance>> values = lambdaOpenVariablesMap.getMap().keyValuesView();
        MutableMap<String, Object> openVars = Maps.mutable.of();
        String _class = JavaSourceCodeGenerator.imports + "\npublic class " + name +
                "{\n" +
                "   public static " + returnType + " doProcess(final MapIterable<String, Object> vars, final MutableMap<String, Object> valMap, final IntObjectMap<CoreInstance> localLambdas, final ExecutionSupport es){\n" +
                values.collect(pair ->
                {
                    String name1 = pair.getOne();
                    CoreInstance valuesCoreInstance = pair.getTwo();
                    ListIterable<? extends CoreInstance> values1 = valuesCoreInstance.getValueForMetaPropertyToMany(M3Properties.values).select(coreInstance -> !Instance.instanceOf(coreInstance, "meta::pure::executionPlan::PlanVarPlaceHolder", compiledSupport) && !Instance.instanceOf(coreInstance, "meta::pure::executionPlan::PlanVariablePlaceHolder", compiledSupport));
                    openVars.put(name1, valuesCoreInstance);
                    if (values1.isEmpty())
                    {
                        MutableList<CoreInstance> vars = Lists.mutable.empty();
                        collectVars(valueSpecification, vars, compiledSupport);
                        CoreInstance found = vars.detect(v -> name1.equals(v.getValueForMetaPropertyToOne("name").getName()));
                        if (found != null)
                        {
                            String type = TypeProcessor.typeToJavaObjectSingle(found.getValueForMetaPropertyToOne(M3Properties.genericType), false, compiledSupport);
                            return "      final  " + type + "  _" + name1 + " = null;";
                        }
                        return "";
                    }
                    else
                    {
                        String type = TypeProcessor.pureRawTypeToJava(compiledSupport.getClassifier(values1.getFirst()), false, compiledSupport);
                        String listImpl = JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.List);
                        return (values1.size() == 1) ? ("      final " + type + " _" + name1 + " = (" + type + ")((" + listImpl + ")vars.get(\"" + name1 + "\"))._values.getFirst();") : ("      final RichIterable<" + type + "> _" + name1 + " = ((" + listImpl + ")vars.get(\"" + name1 + "\"))._values;");
                    }
                }).makeString("\n") +
                "       return " + processed + ";\n" +
                "   }\n" +
                "}\n";

        String javaPackage = JavaPackageAndImportBuilder.buildPackageForPackageableElement(valueSpecification);
        ListIterable<StringJavaSource> javaClasses = Lists.immutable.with(StringJavaSource.newStringJavaSource(javaPackage, name, _class));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        MemoryFileManager manager = new MemoryFileManager(compiler, fileManager, null);

        try
        {
            PureJavaCompiler.compile(compiler, javaClasses, manager);
        }
        catch (Exception e)
        {
            StringBuilder message = new StringBuilder("Error dynamically evaluating value specification");
            SourceInformation valueSpecSourceInfo = valueSpecification.getSourceInformation();
            if (valueSpecSourceInfo != null)
            {
                valueSpecSourceInfo.appendMessage(message.append(" (from ")).append(')');
            }
            message.append("; error compiling generated Java code:\n").append(_class);
            throw new RuntimeException(message.toString(), e);
        }

        ClassLoader cl = new MemoryClassLoader(manager, globalClassLoader);
        try
        {
            Class<?> realClass = cl.loadClass(javaPackage + "." + name);
            return realClass.getMethod("doProcess", MapIterable.class, MutableMap.class, IntObjectMap.class, ExecutionSupport.class).invoke(null, openVars, processorContext.getObjectToPassToDynamicallyGeneratedCode(), processorContext.getLocalLambdas(), es);
        }
        catch (Exception e)
        {
            StringBuilder message = new StringBuilder("Error dynamically evaluating value specification");
            SourceInformation valueSpecSourceInfo = valueSpecification.getSourceInformation();
            if (valueSpecSourceInfo != null)
            {
                valueSpecSourceInfo.appendMessage(message.append(" (from ")).append(')');
            }
            String errorMessage = e.getMessage();
            if (errorMessage != null)
            {
                message.append(": ").append(errorMessage);
            }
            throw new RuntimeException(message.toString(), e);
        }
    }

    private static void collectVars(CoreInstance valueSpecification, MutableList<CoreInstance> vars, ProcessorSupport processorSupport)
    {
        if (Instance.instanceOf(valueSpecification, M3Paths.FunctionExpression, processorSupport))
        {
            for (CoreInstance param : valueSpecification.getValueForMetaPropertyToMany(M3Properties.parametersValues))
            {
                collectVars(param, vars, processorSupport);
            }
        }

        if (Instance.instanceOf(valueSpecification, "meta::pure::router::RoutedValueSpecification", processorSupport))
        {
            collectVars(valueSpecification.getValueForMetaPropertyToOne(M3Properties.value), vars, processorSupport);
        }

        if (Instance.instanceOf(valueSpecification, M3Paths.InstanceValue, processorSupport))
        {
            for (CoreInstance val : valueSpecification.getValueForMetaPropertyToMany(M3Properties.values))
            {
                collectVars(val, vars, processorSupport);
            }
        }

        if (Instance.instanceOf(valueSpecification, M3Paths.LambdaFunction, processorSupport))
        {
            for (CoreInstance expression : valueSpecification.getValueForMetaPropertyToMany(M3Properties.expressionSequence))
            {
                collectVars(expression, vars, processorSupport);
            }
        }

        if (Instance.instanceOf(valueSpecification, M3Paths.KeyExpression, processorSupport))
        {
            collectVars(valueSpecification.getValueForMetaPropertyToOne(M3Properties.expression), vars, processorSupport);
        }

        if (Instance.instanceOf(valueSpecification, M3Paths.VariableExpression, processorSupport))
        {
            vars.add(valueSpecification);
        }
    }


    public static String escapeJSON(String str)
    {
        return JSONValue.escape(str);
    }


    public static Class<?> convertFunctionTypeStringToClass(String type, ClassLoader classLoader)
    {
        switch (type)
        {
            case "long":
            {
                return long.class;
            }
            case "boolean":
            {
                return boolean.class;
            }
            case "double":
            {
                return double.class;
            }
            default:
            {
                return loadClass(fullClassName(type), classLoader);
            }
        }
    }

    public static Class<?> loadClass(String classToLoad, ClassLoader classLoader)
    {
        try
        {
            return classLoader.loadClass(classToLoad);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException("Could not find the class:" + classToLoad, e);
        }
    }


    public static String fullClassName(String className)
    {
        String classToLoad = className;

        // Strip out the generics, you can't look up java methods reflectively with generics
        int genericsIndex = classToLoad.indexOf('<');
        if (genericsIndex != -1)
        {
            classToLoad = classToLoad.substring(0, genericsIndex);
        }

        // If there is not already a package, add the standard one for generated code
        if (classToLoad.indexOf('.') == -1)
        {
            classToLoad = JavaPackageAndImportBuilder.buildPackageFromSystemPath(className) + '.' + classToLoad;
        }
        return classToLoad;
    }


    public static <T> T notSupportedYet()
    {
        throw new RuntimeException("Not supported yet!");
    }


    public static Object executeFunction(CoreInstance functionDefinition, Class<?>[] paramClasses, Object[] params, ExecutionSupport executionSupport)
    {
        return executeFunction(IdBuilder.sourceToId(functionDefinition.getSourceInformation()), (ConcreteFunctionDefinition<?>) functionDefinition, paramClasses, params, executionSupport);
    }


    public static Object executeFunction(String uniqueFunctionId, ConcreteFunctionDefinition<?> functionDefinition, Class<?>[] paramClasses, Object[] params, ExecutionSupport es)
    {
        CompiledExecutionSupport ces = (CompiledExecutionSupport) es;
        SharedPureFunction<?> spf = ces.getFunctionCache().getIfAbsentPutJavaFunctionForPureFunction(functionDefinition, () ->
        {
            try
            {
                Class<?> clazz = ces.getClassLoader().loadClass(JavaPackageAndImportBuilder.rootPackage() + '.' + uniqueFunctionId);
                String functionName = FunctionProcessor.functionNameToJava(functionDefinition);
                Method method = getFunctionMethod(clazz, functionName, functionDefinition, paramClasses, params, es);
                return new JavaMethodWithParamsSharedPureFunction<>(method, paramClasses, functionDefinition.getSourceInformation());
            }
            catch (ClassNotFoundException e)
            {
                throw new PureExecutionException("Unable to execute " + uniqueFunctionId, e, Stacks.mutable.empty());
            }
        });

        return spf.execute(Lists.fixedSize.of(params).with(es), es);
    }

    public static Object executeMethod(Class<?> clazz, String methodName, CoreInstance functionDefinition, Class<?>[] paramClasses, Object objectWhichHasMethod, Object[] params, ExecutionSupport executionSupport)
    {
        Method method = getFunctionMethod(clazz, methodName, functionDefinition, paramClasses, params, executionSupport);
        return executeMethod(functionDefinition, method, objectWhichHasMethod, params, executionSupport);
    }

    private static Method getFunctionMethod(Class<?> clazz, String methodName, CoreInstance functionDefinition, Class<?>[] paramClasses, Object[] params, ExecutionSupport executionSupport)
    {
        Class<?>[] newParamClasses = (paramClasses == null) ? new Class[1] : Arrays.copyOf(paramClasses, paramClasses.length + 1);
        newParamClasses[newParamClasses.length - 1] = ExecutionSupport.class;

        try
        {
            return clazz.getMethod(methodName, newParamClasses);
        }
        catch (NoSuchMethodException e)
        {
            throw new PureExecutionException(buildFunctionExecutionErrorMessage(functionDefinition, params, "Function was not found.", executionSupport), e, Stacks.mutable.empty());
        }
    }


    private static Object executeMethod(CoreInstance functionDefinition, Method method, Object objectWhichHasMethod, Object[] params, ExecutionSupport executionSupport)
    {
        try
        {
            Object[] newParams;
            if (params == null)
            {
                newParams = new Object[]{executionSupport};
            }
            else
            {
                newParams = Arrays.copyOf(params, params.length + 1);
                newParams[newParams.length - 1] = executionSupport;
            }
            return method.invoke(objectWhichHasMethod, newParams);
        }
        catch (IllegalArgumentException iae)
        {
            throw new PureExecutionException(buildFunctionExecutionErrorMessage(functionDefinition, params, "Input parameters are invalid.", executionSupport), iae, Stacks.mutable.empty());
        }
        catch (IllegalAccessException ex)
        {
            throw new PureExecutionException(buildFunctionExecutionErrorMessage(functionDefinition, params, "Failed to invoke java function.", executionSupport), ex, Stacks.mutable.empty());
        }
        catch (InvocationTargetException ex)
        {
            PureException pureException = PureException.findPureException(ex);
            if (pureException != null)
            {
                throw pureException;
            }
            else
            {
                throw new RuntimeException("Unexpected error executing function" + ((params != null) && (params.length > 0) ? " with params " + Arrays.toString(params) : ""), ex);
            }
        }
    }

    public static Object validate(boolean goDeep, Object o, SourceInformation si, ExecutionSupport es)
    {
        try
        {
            Method validateMethod = o.getClass().getMethod("_validate", boolean.class, SourceInformation.class, ExecutionSupport.class);
            return validateMethod.invoke(o, goDeep, si, es);
        }
        catch (NoSuchMethodException ns)
        {
            return o;
        }
        catch (IllegalAccessException | IllegalArgumentException ex)
        {
            throw new PureExecutionException("Failed to invoke _validate function.", ex, Stacks.mutable.empty());
        }
        catch (InvocationTargetException ex)
        {
            PureException pureException = PureException.findPureException(ex);
            if (pureException != null)
            {
                throw pureException;
            }
            else
            {
                throw new RuntimeException("Unexpected error executing function _validate", ex);
            }
        }
    }

    //TODO extend this for all types, currently only handles precise primitives.
    public static void validate(RichIterable<?> instances, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType type, SourceInformation si, ExecutionSupport es)
    {
        instances.forEach(instance -> validate(instance, type, si, es));
    }

    public static void validate(Object instance, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType type, SourceInformation si, ExecutionSupport es)
    {
        try
        {
            CoreInstance rawType = type.getValueForMetaPropertyToOne("rawType");
            ProcessorSupport processorSupport = ((CompiledExecutionSupport) es).getProcessorSupport();
            boolean isExtendedPrimitive = org.finos.legend.pure.m3.navigation.type.Type.isExtendedPrimitiveType(rawType, processorSupport);

            if (isExtendedPrimitive)
            {
                Optional<Pair<Type, ListIterable<Object>>> p = detectValidatingExtendedPrimitive(type, null);

                if (!p.isPresent())
                {
                    return;
                }

                MutableList<Object> values = p.get().getTwo().toList();
                values.add(instance);
                values.add(si);
                values.add(es);

                String className = JavaPackageAndImportBuilder.buildImplClassReferenceFromType(p.get().getOne(), processorSupport);

                Method method = getMethod(Class.forName(className), "_validate");
                method.invoke(null, values.toArray());
            }
        }
        catch (InvocationTargetException e)
        {
            if (e.getTargetException() instanceof PureExecutionException)
            {
                throw (PureExecutionException) e.getTargetException();
            }
            else
            {
                throw new RuntimeException("Unexpected error validating object", e.getTargetException());
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unexpected error validating object", e);
        }
    }

    private static Optional<Pair<Type, ListIterable<Object>>> detectValidatingExtendedPrimitive(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType type, ListIterable<? extends CoreInstance> variableValues)
    {
        CoreInstance rawType = type.getValueForMetaPropertyToOne("rawType");

        ListIterable<? extends CoreInstance> typeVariableValues = variableValues != null ? variableValues : type.getValueForMetaPropertyToMany("typeVariableValues");

        boolean hasConstraints = rawType.getValueForMetaPropertyToMany(M3Properties.constraints).notEmpty();

        if (hasConstraints)
        {
            return Optional.of(Tuples.pair((Type) rawType, typeVariableValues.flatCollect(v -> v.getValueForMetaPropertyToMany("values")).collect(v -> ((ValCoreInstance) v).getValue())));
        }

        CoreInstance generalizations = rawType.getValueForMetaPropertyToOne("generalizations");
        CoreInstance general = generalizations == null ? null : generalizations.getValueForMetaPropertyToOne("general");

        if (general == null)
        {
            return Optional.empty();
        }

        return detectValidatingExtendedPrimitive((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType) general, general.getValueForMetaPropertyToMany("typeVariableValues"));
    }

    public static Method getMethod(Class<?> clazz, String methodName)
    {
        Method[] methods = clazz.getMethods();
        MutableList<Method> candidates = ArrayIterate.select(methods, m -> methodName.equals(m.getName()));

        if (candidates.size() > 1)
        {
            throw new IllegalArgumentException("multiple functions found for  " + clazz.getSimpleName() + "." + methodName);
        }

        if (candidates.isEmpty())
        {
            throw new IllegalArgumentException("cannot find function for " + clazz.getSimpleName() + "." + methodName);
        }

        return candidates.get(0);
    }

    private static String buildFunctionExecutionErrorMessage(CoreInstance functionDefinition, Object[] params, String reason, ExecutionSupport es)
    {
        StringBuilder builder = new StringBuilder("Error executing ");
        org.finos.legend.pure.m3.navigation.function.Function.print(builder, functionDefinition, ((CompiledExecutionSupport) es).getProcessorSupport());

        if (params != null)
        {
            ArrayAdapter.adapt(params).asLazy()
                    .collect(p -> pureToString(p, es))
                    .appendString(builder, " with parameters [", ", ", "]");
        }

        return builder.append(". ").append(reason).toString();
    }

    public static <T> RichIterable<? extends T> castWithExceptionHandling(RichIterable<?> sourceCollection, Class<?> targetType, SourceInformation sourceInformation)
    {
        return (sourceCollection == null) ? Lists.immutable.empty() : sourceCollection.collect(sourceObject -> castWithExceptionHandling(sourceObject, targetType, sourceInformation));
    }

    public static Object castExtendedPrimitive(Object sourceObject, Class<?> targetType, String typeName, DefendedFunction0<Object> run, SourceInformation sourceInformation)
    {
        if (sourceObject != null && !targetType.isInstance(sourceObject))
        {
            throw new PureExecutionException(sourceInformation, "Cast exception: " + getPureClassName(sourceObject) + " cannot be cast to " + typeName, Stacks.mutable.empty());
        }
        return run.get();
    }

    @SuppressWarnings("unchecked")
    public static <T> T castWithExceptionHandling(Object sourceObject, Class<?> targetType, SourceInformation sourceInformation)
    {
        if (sourceObject != null && !targetType.isInstance(sourceObject))
        {
            String targetSimpleName = targetType.getSimpleName();
            String castTypeClassName = targetSimpleName.substring(targetSimpleName.lastIndexOf('_') + 1);

            String errorMessage = "Cast exception: " + getPureClassName(sourceObject) + " cannot be cast to " + castTypeClassName;
            throw new PureExecutionException(sourceInformation, errorMessage, Stacks.mutable.empty());
        }
        return (T) sourceObject;
    }

    public static long safeSize(Object obj)
    {
        if (obj == null)
        {
            return 0L;
        }
        if (obj instanceof RichIterable)
        {
            return ((RichIterable<?>) obj).size();
        }
        return 1L;
    }

    public static String getPureClassName(Object obj)
    {
        if (isTypeOfEnum(obj))
        {
            return getEnumClassifierName((Enum) obj);
        }
        if (isPureGeneratedClass(obj))
        {
            if (!(obj instanceof Class && ((Class<?>) obj).isInterface()))
            {
                return getPureGeneratedClassName(obj);
            }
        }
        String primitiveJavaToPureType = obj instanceof Class ?
                JavaPurePrimitiveTypeMapping.getPureM3TypeFromJavaPrimitivesAndDates((Class<?>) obj) :
                JavaPurePrimitiveTypeMapping.getPureM3TypeFromJavaPrimitivesAndDates(obj);
        if (primitiveJavaToPureType != null)
        {
            return primitiveJavaToPureType;
        }
        String[] defaultName = obj.toString().split("_");
        return defaultName[defaultName.length - 1];
    }

    @Deprecated
    public static String fullyQualifiedJavaInterfaceNameForPackageableElement(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement element)
    {
        if (ClassProcessor.isPlatformClass(element))
        {
            Package pkg = element._package();
            ListIterable<String> packagePath = pkg == null ? Lists.fixedSize.empty() : getUserObjectPathForPackageableElement(pkg, false);
            return M3ToJavaGenerator.getFullyQualifiedM3InterfaceForCompiledModel(packagePath, element);
        }
        else
        {
            return JavaPackageAndImportBuilder.buildPackageForPackageableElement(element) + "." + PackageableElement.getSystemPathForPackageableElement(element, "_");
        }
    }

    public static Type getType(Any val, MetadataAccessor metadata)
    {
        String fullSystemPath = val.getFullSystemPath();
        return val instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum ? metadata.getEnumeration(fullSystemPath) : metadata.getClass(fullSystemPath);
    }

    private static MutableList<String> getUserObjectPathForPackageableElement(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement, boolean includeRoot)
    {
        Package pkg = packageableElement._package();
        if (pkg == null)
        {
            MutableList<String> pkgPath = Lists.mutable.empty();
            if (includeRoot || !M3Paths.Root.equals(packageableElement.getName()))
            {
                pkgPath.add(packageableElement.getName());
            }
            return pkgPath;
        }
        return getUserObjectPathForPackageableElement(pkg, includeRoot).with(packageableElement.getName());
    }

    public static QuantityCoreInstance newUnitInstance(CoreInstance unit, Number value, ExecutionSupport executionSupport)
    {
        return newUnitInstance(unit, value, (CompiledExecutionSupport) executionSupport);
    }

    @SuppressWarnings("unchecked")
    public static QuantityCoreInstance newUnitInstance(CoreInstance unit, Number value, CompiledExecutionSupport executionSupport)
    {
        Class<? extends QuantityCoreInstance> unitImplClass;
        try
        {
            String javaClassImplName = JavaPackageAndImportBuilder.buildImplClassReferenceFromType(unit, executionSupport.getProcessorSupport());
            ClassLoader classLoader = executionSupport.getClassLoader();
            unitImplClass = (Class<? extends QuantityCoreInstance>) classLoader.loadClass(javaClassImplName);
        }
        catch (ClassNotFoundException e)
        {
            StringBuilder builder = new StringBuilder("Could not find Java class for unit ");
            PackageableElement.writeUserPathForPackageableElement(builder, unit);
            throw new PureExecutionException(builder.toString(), e, Stacks.mutable.empty());
        }
        catch (Exception e)
        {
            StringBuilder builder = new StringBuilder("Error finding Java class for unit ");
            PackageableElement.writeUserPathForPackageableElement(builder, unit);
            String eMessage = e.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw new PureExecutionException(builder.toString(), e, Stacks.mutable.empty());
        }

        try
        {
            Constructor<? extends QuantityCoreInstance> constructor = unitImplClass.getConstructor(Number.class, CompiledExecutionSupport.class);
            return constructor.newInstance(value, executionSupport);
        }
        catch (Exception e)
        {
            StringBuilder builder = new StringBuilder("Error instantiating Java class ").append(unitImplClass.getName()).append(" for unit ");
            PackageableElement.writeUserPathForPackageableElement(builder, unit);
            String eMessage = e.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw new PureExecutionException(builder.toString(), (e instanceof InvocationTargetException) ? e.getCause() : e, Stacks.mutable.empty());
        }
    }
}
