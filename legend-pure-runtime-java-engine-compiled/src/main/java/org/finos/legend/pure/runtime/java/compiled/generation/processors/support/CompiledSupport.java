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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.eclipse.collections.api.ordered.OrderedIterable;
import org.eclipse.collections.api.ordered.ReversibleIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.bootstrap.generator.M3ToJavaGenerator;
import org.finos.legend.pure.m3.coreinstance.BaseCoreInstance;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.io.http.HTTPResponse;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.io.http.URL;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ElementOverride;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.GetterOverride;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m3.navigation.function.InvalidFunctionDescriptorException;
import org.finos.legend.pure.m3.serialization.runtime.SourceRegistry;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m3.tools.StatisticsUtil;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstance;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateTime;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.StrictDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.Year;
import org.finos.legend.pure.m4.coreinstance.primitive.date.YearMonth;
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
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.GetterOverrideExecutor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.JavaCompiledCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.ReflectiveCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.ValCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureFunction2;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureFunction2Wrapper;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureEqualsHashingStrategy;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class.ClassProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;
import org.finos.legend.pure.runtime.java.compiled.metadata.ClassCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.JavaMethodWithParamsSharedPureFunction;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataAccessor;
import org.finos.legend.pure.runtime.java.shared.cipher.AESCipherUtil;
import org.finos.legend.pure.runtime.java.shared.http.HttpMethod;
import org.finos.legend.pure.runtime.java.shared.http.HttpRawHelper;
import org.finos.legend.pure.runtime.java.shared.identity.IdentityManager;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.UUID;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public class CompiledSupport
{
    public static final CompileState CONSTRAINTS_VALIDATED = CompileState.COMPILE_EVENT_EXTRA_STATE_3;

    private static final String TEMP_TYPE_NAME = "tempTypeName";

    private static final DecimalFormat DECIMAL_FORMAT;
    static
    {
        DecimalFormat format = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        format.setMaximumFractionDigits(340); // 340 = DecimalFormat.DOUBLE_FRACTION_DIGITS
        DECIMAL_FORMAT =  format;
    }


    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
    private static final ImmutableList<Class<?>> PRIMITIVE_CLASS_COMPARISON_ORDER = Lists.immutable.with(Long.class, Double.class, PureDate.class, Boolean.class, String.class);

    public static final Comparator<Object> DEFAULT_COMPARATOR = CompiledSupport::compareInt;

    public static <T> T copy(T coreInstance)
    {
        return copy(coreInstance, (coreInstance == null ? null : ((AbstractCoreInstance)coreInstance).getSourceInformation()));
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
            AbstractCoreInstance result = (AbstractCoreInstance)((AbstractCoreInstance)coreInstance).copy();
            result.setSourceInformation(sourceInformation);
            return (T)result;
        }
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
            CoreInstance coreInstance = (CoreInstance)obj;
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

    public static boolean isSourceReadOnly(String sourceName, ExecutionSupport es)
    {
        return isSourceReadOnly(((CompiledExecutionSupport)es).getSourceRegistry(), sourceName);
    }

    public static boolean isSourceReadOnly(SourceRegistry sourceRegistry, String sourceName)
    {
        if (sourceRegistry == null)
        {
            throw new RuntimeException("The source registry has not been defined... This function should probably not be used in your current environment.");
        }
        return sourceRegistry.getSource(sourceName).isImmutable();
    }

    public static RichIterable enumValues(CoreInstance coreInstance)
    {
        return coreInstance.getValueForMetaPropertyToMany(M3Properties.values);
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
            return (RichIterable<T>)objects;
        }
        return Lists.immutable.ofAll(objects);
    }

    public static <T> RichIterable<T> toPureCollection(T object)
    {
        if (object == null)
        {
            return Lists.immutable.empty();
        }
        // TODO remove this hack
        if (object instanceof RichIterable)
        {
            return (RichIterable<T>)object;
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
        return FastList.newListWith(StringUtils.splitByCharacterTypeCamelCase(string));
    }

    public static RichIterable<String> split(Object string, Object delimiter)
    {
        String str = (String)((string instanceof Iterable) ? Iterate.getFirst((Iterable<?>)string) : string);
        String delim = (String)((delimiter instanceof Iterable) ? Iterate.getFirst((Iterable<?>)delimiter) : delimiter);

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
            throw new PureExecutionException(sourceInformation, message);
        }
        if (list instanceof ListIterable)
        {
            try
            {
                return ((ListIterable<?>)list).get((int)offset);
            }
            catch (IndexOutOfBoundsException e)
            {
                String message = "The system is trying to get an element at offset " + offset + " where the collection is of size " + ((ListIterable<?>)list).size();
                throw new PureExecutionException(sourceInformation, message);
            }
        }
        if (list instanceof RichIterable)
        {
            if (offset < 0)
            {
                String message = "The system is trying to get an element at offset " + offset + " where the collection is of size " + ((RichIterable<?>)list).size();
                throw new PureExecutionException(sourceInformation, message);
            }
            int intOffset = (int)offset;
            int size = 0;
            for (Object item : (RichIterable<?>)list)
            {
                if (size == intOffset)
                {
                    return item;
                }
                size++;
            }
            String message = "The system is trying to get an element at offset " + offset + " where the collection is of size " + size;
            throw new PureExecutionException(sourceInformation, message);
        }
        if (offset != 0)
        {
            throw new PureExecutionException(sourceInformation, "The system is trying to get an element at offset " + offset + " where the collection is of size 1");
        }
        return list;
    }

    public static int safeHashCode(boolean value)
    {
        return value ? 1231 : 1237;
    }

    public static int safeHashCode(float value)
    {
        return Float.floatToIntBits(value);
    }

    public static int safeHashCode(double value)
    {
        return safeHashCode(Double.doubleToLongBits(value));
    }

    public static int safeHashCode(long value)
    {
        return (int)(value ^ (value >>> 32));
    }

    public static int safeHashCode(int value)
    {
        return value;
    }

    public static int safeHashCode(Object object)
    {
        if (object == null)
        {
            return 0;
        }
        if (object instanceof JavaCompiledCoreInstance)
        {
            return ((JavaCompiledCoreInstance)object).pureHashCode();
        }
        if (object instanceof Iterable)
        {
            int hashCode = 0;
            for (Object item : (Iterable<?>)object)
            {
                hashCode = 31 * hashCode + safeHashCode(item);
            }
            return hashCode;
        }
        return object.hashCode();
    }

    public static <T> T toOne(T object, SourceInformation sourceInformation)
    {
        if (object == null)
        {
            throw new PureExecutionException(sourceInformation, "Cannot cast a collection of size 0 to multiplicity [1]");
        }
        if (object instanceof RichIterable)
        {
            return toOne((RichIterable<? extends T>)object, sourceInformation);
        }
        return object;
    }

    public static <T> T toOne(RichIterable<? extends T> objects, SourceInformation sourceInformation)
    {
        if (objects == null || objects.size() != 1)
        {
            throw new PureExecutionException(sourceInformation,
                    "Cannot cast a collection of size " + (objects == null ? 0 : objects.size()) + " to multiplicity [1]");
        }
        return objects.getAny();
    }

    public static SourceInformation getSourceInformation(String sourceIdForError, int line, int column)
    {
        return sourceIdForError == null ? null : new SourceInformation(sourceIdForError, -1, -1, line, column, -1, -1);
    }

    public static Object makeOne(Object object)
    {
        if (object instanceof RichIterable)
        {
            return ((RichIterable<?>)object).getAny();
        }
        return object;
    }

    public static <T> T makeOne(RichIterable<? extends T> object)
    {
        if (object == null)
        {
            return null;
        }
        return object.getFirst();
    }


    public static <T> RichIterable<T> toReversed(RichIterable<T> collection)
    {
        if (collection == null || Iterate.isEmpty(collection))
        {
            return Lists.immutable.empty();
        }
        if (collection instanceof ReversibleIterable)
        {
            return ((ReversibleIterable<T>)collection).asReversed();
        }
        return collection.toList().reverseThis();
    }

    public static <T> RichIterable<T> toOneMany(T object, SourceInformation sourceInformation)
    {
        if (object == null)
        {
            throw new PureExecutionException(sourceInformation, "Cannot cast a collection of size 0 to multiplicity [1..*]");
        }
        // TODO remove this hack
        if (object instanceof RichIterable)
        {
            if (((RichIterable<?>)object).isEmpty())
            {
                throw new PureExecutionException(sourceInformation, "Cannot cast a collection of size 0 to multiplicity [1..*]");
            }
            return (RichIterable<T>)object;
        }
        // TODO remove this hack
        if (object instanceof Iterable)
        {
            if (Iterate.isEmpty((Iterable<?>)object))
            {
                throw new PureExecutionException(sourceInformation, "Cannot cast a collection of size 0 to multiplicity [1..*]");
            }
            return toPureCollection((Iterable<T>)object);
        }
        return Lists.immutable.with(object);
    }

    public static <T> RichIterable<T> toOneMany(RichIterable<T> objects, SourceInformation sourceInformation)
    {
        if (Iterate.isEmpty(objects))
        {
            throw new PureExecutionException(sourceInformation, "Cannot cast a collection of size 0 to multiplicity [1..*]");
        }
        return objects;
    }

    public static boolean isEmpty(RichIterable<?> objects)
    {
        return (objects == null) || objects.isEmpty();
    }

    public static boolean isEmpty(Object object)
    {
        return (object == null) || ((object instanceof Iterable) && Iterate.isEmpty((Iterable<?>)object));
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
            return ((LazyIterable<T>)list).take((int)number);
        }
        if (number >= list.size())
        {
            return list;
        }

        int end = (int)number;
        if (list instanceof ListIterable)
        {
            return ListHelper.subList((ListIterable<T>)list, 0, end);
        }

        MutableList<T> result = FastList.newList(end);
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
            return ((LazyIterable<T>)list).drop((int)number);
        }
        int size = list.size();
        if (number >= size)
        {
            return Lists.immutable.empty();
        }

        int toDrop = (int)number;
        if (list instanceof ListIterable)
        {
            return ListHelper.subList((ListIterable<T>)list, toDrop, size);
        }

        MutableList<T> result = FastList.newList(size - toDrop);
        result.addAllIterable(LazyIterate.drop(list, toDrop));
        return result;
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
        return (size <= 1) ? Lists.immutable.empty() : LazyIterate.take(list, size - 1).toList();
    }

    public static <T> RichIterable<T> tail(T list)
    {
        return Lists.immutable.empty();
    }

    public static <T> RichIterable<T> tail(RichIterable<T> list)
    {
        if (list == null || isEmpty(list))
        {
            return Lists.immutable.empty();
        }

        RichIterable<T> result = LazyIterate.drop(list, 1);
        return (list instanceof LazyIterable) ? result : result.toList();
    }

    public static <T> long compare(T left, T right)
    {
        return compareInt(left, right);
    }

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
                return compareUnmatchedNumbers((Number)left, (Number)right);
            }
            if ((left instanceof PureDate) && (right instanceof PureDate))
            {
                return ((PureDate)left).compareTo((PureDate)right);
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
            return ((Comparable)left).compareTo(right);
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
            return (BigDecimal)number;
        }
        if (number instanceof BigInteger)
        {
            return new BigDecimal((BigInteger)number);
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
        catch (final NumberFormatException e)
        {
            throw new RuntimeException("The given number (\"" + number + "\" of class " + number.getClass().getName() + ") does not have a parsable string representation", e);
        }
    }

    public static <T> RichIterable<T> add(RichIterable<? extends T> list, T element)
    {
        if (element == null)
        {
            return (RichIterable<T>)list;
        }
        MutableList<T> newList = list == null ? Lists.mutable.empty() : Lists.mutable.withAll(list);
        newList.add((element instanceof Iterable) ? Iterate.getFirst((Iterable<T>)element) : element);
        return newList;
    }

    public static <T> RichIterable<T> add(RichIterable<? extends T> list, long index, T element)
    {
        if (element == null)
        {
            return (RichIterable<T>)list;
        }
        MutableList<T> newList = list == null ? FastList.<T>newList() : Lists.mutable.withAll(list);
        newList.add((int)index, (element instanceof Iterable) ? Iterate.getFirst((Iterable<T>)element) : element);
        return newList;
    }

    public static RichIterable<?> concatenate(Object list1, Object list2)
    {
        if ((list1 == null) && (list2 == null))
        {
            return Lists.immutable.empty();
        }
        if (list1 == null)
        {
            return (list2 instanceof RichIterable) ? (RichIterable<?>)list2 : Lists.immutable.with(list2);
        }
        if (list2 == null)
        {
            return (list1 instanceof RichIterable) ? (RichIterable<?>)list1 : Lists.immutable.with(list1);
        }

        Iterable it1 = (list1 instanceof Iterable) ? (Iterable)list1 : Lists.immutable.with(list1);
        Iterable it2 = (list2 instanceof Iterable) ? (Iterable)list2 : Lists.immutable.with(list2);

        RichIterable<Object> result = LazyIterate.concatenate(it1, it2);
        return (it1 instanceof LazyIterable || it2 instanceof LazyIterable) ? result : result.toList();
    }

    public static RichIterable<Long> range(long start, long stop, long step, SourceInformation sourceInformation)
    {
        if (step == 0)
        {
            throw new PureExecutionException(sourceInformation, "range step must not be 0");
        }
        MutableList<Long> result = FastList.newList((int)Math.max(0, (stop - start) / step));
        for (; step > 0 ? start < stop : start > stop; start += step)
        {
            result.add(start);
        }
        return result;
    }

    public static <T> RichIterable<? extends T> removeAllOptimized(RichIterable<? extends T> main, RichIterable<? extends T> other)
    {
        Set<?> set = (other instanceof Set) ? (Set<?>) other : Sets.mutable.withAll(other);
        return main.reject(set::contains);
    }

    public static boolean exists(Object object, Predicate predicate)
    {
        if (object == null)
        {
            return false;
        }

        if (object instanceof Iterable)
        {
            return Iterate.anySatisfy((Iterable)object, predicate);
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
            return Iterate.allSatisfy((Iterable)object, predicate);
        }

        return predicate.accept(object);
    }

    public static <T, V> RichIterable<? extends T> mapToManyOverMany(RichIterable<? extends V> collection, Function2<? super V, ExecutionSupport, ? extends Iterable<? extends T>> function, final ExecutionSupport executionSupport)
    {
        return collection == null ? Lists.mutable.empty() : collection.flatCollect((Function<? super V, ? extends Iterable<T>>) object -> ((Function2<? super V, ExecutionSupport, ? extends Iterable<T>>) function).value(object, executionSupport));
    }

    public static <T, V> RichIterable<? extends T> mapToOneOverMany(RichIterable<? extends V> collection, Function2<? super V, ExecutionSupport, T> function, ExecutionSupport executionSupport)
    {
        if (collection == null)
        {
            return Lists.mutable.empty();
        }

        RichIterable<T> result = collection.asLazy().collectWith(function, executionSupport).select(Objects::nonNull);
        return collection instanceof LazyIterable ? result : result.toList();
    }

    public static <T, V> RichIterable<? extends T> mapToManyOverOne(V element, Function2<? super V, ExecutionSupport, ? extends RichIterable<? extends T>> function, ExecutionSupport executionSupport)
    {
        return (element == null) ? Lists.mutable.empty() : function.value(element, executionSupport);
    }

    public static <T, V> T mapToOneOverOne(V element, Function2<? super V, ExecutionSupport, T> function, ExecutionSupport executionSupport)
    {
        return (element == null) ? null : function.value(element, executionSupport);
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

    public static <T> T last(RichIterable<T> list)
    {
        return Iterate.isEmpty(list) ? null : list.getLast();
    }

    public static <T> T last(T instance)
    {
        return instance;
    }

    public static Object print(ConsoleCompiled console, Object content, Long max)
    {
        console.print(content, max.intValue());
        return null;
    }

    public static String joinStrings(Object strings, String prefix, String separator, String suffix)
    {
        return (strings instanceof RichIterable) ? joinStrings((RichIterable<String>)strings, prefix, separator, suffix) : joinStrings((String)strings, prefix, separator, suffix);
    }

    public static String joinStrings(String string, String prefix, String separator, String suffix)
    {
        return prefix + string + suffix;
    }

    public static String joinStrings(RichIterable<String> strings, String prefix, String separator, String suffix)
    {
        strings = strings == null ? Lists.mutable.empty() : strings;
        int size = strings.size();

        switch (size)
        {
            case 0:
            {
                return prefix + suffix;
            }
            case 1:
            {
                return prefix + strings.getFirst() + suffix;
            }
            default:
            {
                return strings.makeString(prefix, separator, suffix);
            }
        }
    }

    public static String format(String formatString, Object formatArgs, Function2 toRepresentationFunction, ExecutionSupport executionSupport)
    {
        return PureStringFormat.format(formatString, (formatArgs instanceof Iterable) ? (Iterable<?>)formatArgs : Lists.immutable.with(formatArgs), toRepresentationFunction, executionSupport);
    }

    /**
     * Return a copy of collection sorted by applying comparator
     * to the results of applying keyFunction to the elements of
     * collection.
     *
     * @param collection collection to sort
     * @param key        key function
     * @param comp       comparator
     * @param <T>        collection element type
     * @return sorted collection
     */
    public static <T, U> RichIterable<T> toSorted(RichIterable<T> collection, SharedPureFunction key, SharedPureFunction comp, ExecutionSupport es)
    {
        if (Iterate.isEmpty(collection))
        {
            return collection;
        }
        if ((key == null) && (comp == null))
        {
            return collection.toSortedList(CompiledSupport::compareInt);
        }
        if (key == null)
        {
            return toSortedWithComparison(collection, comp, es);
        }
        if (comp == null)
        {
            return toSortedWithKey(collection, key, es);
        }
        return toSortedWithKeyComparison(collection, key, comp, es);
    }

    private static <T> RichIterable<T> toSortedWithKeyComparison(RichIterable<T> collection, SharedPureFunction key, SharedPureFunction comp, ExecutionSupport es)
    {
        final MutableMap<T, Object> keyMap = new UnifiedMapWithHashingStrategy<>(PureEqualsHashingStrategy.HASHING_STRATEGY, collection.size());
        final Function<T, Object> keyFunction = element -> key.execute(Lists.immutable.with(element), null);
        return collection.toSortedList((left, right) ->
        {
            if (left == right)
            {
                Object leftKey = keyMap.getIfAbsentPutWithKey(left, keyFunction);
                return ((Long)comp.execute(Lists.immutable.with(leftKey, leftKey), es)).intValue();
            }

            Object leftKey = keyMap.getIfAbsentPutWithKey(left, keyFunction);
            Object rightKey = keyMap.getIfAbsentPutWithKey(right, keyFunction);
            return ((Long)comp.execute(Lists.immutable.with(leftKey, rightKey), es)).intValue();
        });
    }

    private static <T> RichIterable<T> toSortedWithKey(RichIterable<T> collection, SharedPureFunction key, ExecutionSupport es)
    {
        MutableMap<T, Object> keyMap = new UnifiedMapWithHashingStrategy<>(PureEqualsHashingStrategy.HASHING_STRATEGY, collection.size());
        Function<T, Object> keyFunction = element -> key.execute(Lists.immutable.with(element), es);
        return collection.toSortedList((left, right) ->
        {
            if (left == right)
            {
                return 0;
            }

            Object leftKey = keyMap.getIfAbsentPutWithKey(left, keyFunction);
            Object rightKey = keyMap.getIfAbsentPutWithKey(right, keyFunction);
            return compareInt(leftKey, rightKey);
        });
    }

    private static <T> RichIterable<T> toSortedWithComparison(RichIterable<T> collection, SharedPureFunction comp, final ExecutionSupport executionSupport)
    {
        return collection.toSortedList((left, right) -> ((Long)comp.execute(Lists.immutable.with(left, right), executionSupport)).intValue());
    }

    /**
     * Return a list consisting of element repeated n times.
     *
     * @param element element to repeat
     * @param n       number of times to repeat element
     * @param <T>     element type
     * @return element repeated n times
     */
    public static <T> RichIterable<T> repeat(T element, long n)
    {
        if (n <= 0)
        {
            return Lists.immutable.empty();
        }
        int num = (int)n;
        MutableList<T> elements = FastList.newList(num);
        for (; num > 0; num--)
        {
            elements.add(element);
        }
        return elements;
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
            return eq((Number)left, (Number)right);
        }
        if ((left instanceof String) || (left instanceof PureDate))
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

    public static double exp(Number n)
    {
        return Math.exp(n.doubleValue());
    }

    public static double log(Number n)
    {
        return Math.log(n.doubleValue());
    }

    public static boolean equal(Object left, Object right) //NOSONAR Function signature avoids confusion
    {
        if (left == right)
        {
            return true;
        }
        if (left == null)
        {
            return (right instanceof RichIterable) && ((RichIterable<?>)right).isEmpty();
        }
        if (right == null)
        {
            return (left instanceof RichIterable) && ((RichIterable<?>)left).isEmpty();
        }
        if (left instanceof LazyIterable)
        {
            Iterator<?> leftIterator = ((Iterable<?>)left).iterator();
            if (right instanceof Iterable)
            {
                return iteratorsEqual(leftIterator, ((Iterable<?>)right).iterator());
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
            Iterator<?> rightIterator = ((Iterable<?>)right).iterator();
            if (left instanceof Iterable)
            {
                return iteratorsEqual(((Iterable<?>)left).iterator(), rightIterator);
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
            RichIterable<?> leftList = (RichIterable<?>)left;
            int size = leftList.size();
            if (right instanceof RichIterable)
            {
                RichIterable<?> rightList = (RichIterable<?>)right;
                return (size == rightList.size()) && iteratorsEqual(leftList.iterator(), rightList.iterator());
            }
            return (size == 1) && equal(leftList.getFirst(), right);
        }
        if (right instanceof RichIterable)
        {
            RichIterable<?> rightList = (RichIterable<?>)right;
            return (rightList.size() == 1) && equal(left, rightList.getFirst());
        }
        if (left instanceof Number)
        {
            return (right instanceof Number) && eq((Number)left, (Number)right);
        }

        if (left instanceof JavaCompiledCoreInstance)
        {
            return ((JavaCompiledCoreInstance)left).pureEquals(right);
        }
        if (right instanceof JavaCompiledCoreInstance)
        {
            return ((JavaCompiledCoreInstance)right).pureEquals(left);
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
            throw new PureExecutionException(sourceInformation, "Failed to parse JSON string. Invalid JSON string. " + parseException.toString(), parseException);
        }
    }

    public static String pureToString(boolean value, ExecutionSupport es)
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
            return pureToString(((Boolean)instance).booleanValue(), es);
        }
        if (instance instanceof Number)
        {
            return pureToString((Number)instance, es);
        }
        if (instance instanceof PureDate)
        {
            return pureToString((PureDate)instance, es);
        }
        if (instance instanceof String)
        {
            return pureToString((String)instance, es);
        }
        if (instance instanceof ReflectiveCoreInstance)
        {
            return ((ReflectiveCoreInstance)instance).toString(es);
        }
        if (instance instanceof BaseCoreInstance)
        {
            String id = ((BaseCoreInstance)instance).getName();
            return ModelRepository.possiblyReplaceAnonymousId(id);
        }

        try
        {
            Method method = instance.getClass().getDeclaredMethod("toString", ExecutionSupport.class);
            try
            {
                return (String)method.invoke(instance, es);
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
        return primitiveToString((double)value);
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
            return primitiveToString((BigDecimal)value);
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
            return primitiveToString(((Boolean)value).booleanValue());
        }
        if (value instanceof Number)
        {
            return primitiveToString((Number)value);
        }
        if (value instanceof PureDate)
        {
            return primitiveToString((PureDate)value);
        }
        if (value instanceof String)
        {
            return primitiveToString((String)value);
        }
        throw new IllegalArgumentException("Unhandled primitive: " + value + " (" + value.getClass() + ")");
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
            return Math.abs((Integer)n);
        }
        else if (n instanceof Long)
        {
            return Math.abs((Long)n);
        }
        else if (n instanceof Float)
        {
            return Math.abs((Float)n);
        }
        else if (n instanceof Double)
        {
            return Math.abs((Double)n);
        }
        else if (n instanceof BigDecimal)
        {
            return ((BigDecimal)n).abs();
        }
        else if (n instanceof BigInteger)
        {
            return ((BigInteger)n).abs();
        }
        throw new IllegalArgumentException("Unhandled Number Type " + n);
    }

    public static Number stdDev(RichIterable<? extends Number> list, boolean isBiasCorrected, SourceInformation sourceInformation)
    {
        if (list == null || list.isEmpty())
        {
            throw new PureExecutionException(sourceInformation, "Unable to process empty list");
        }
        MutableList<Number> javaNumbers = Lists.mutable.withAll(list);
        double[] values = new double[javaNumbers.size()];
        for (int i = 0; i < javaNumbers.size(); i++)
        {
            values[i] = javaNumbers.get(i).doubleValue();
        }
        return StatisticsUtil.standardDeviation(values, isBiasCorrected);
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

    public static Long plus(Long left, Long right)
    {
        return left.longValue() + right.longValue();
    }

    public static Number plus(Number left, Number right)
    {
        if ((left instanceof Long) && (right instanceof Long))
        {
            return left.longValue() + right.longValue();
        }
        if (left instanceof BigDecimal && right instanceof BigDecimal)
        {
            return ((BigDecimal)left).add((BigDecimal)right);
        }
        if (left instanceof BigDecimal && right instanceof Long)
        {
            return ((BigDecimal)left).add(BigDecimal.valueOf(right.longValue()));
        }
        if (left instanceof BigDecimal && right instanceof Double)
        {
            return ((BigDecimal)left).add(BigDecimal.valueOf(right.doubleValue()));
        }
        if (right instanceof BigDecimal && left instanceof Long)
        {
            return BigDecimal.valueOf(left.longValue()).add((BigDecimal)right);
        }
        if (right instanceof BigDecimal && left instanceof Double)
        {
            return BigDecimal.valueOf(left.doubleValue()).add(((BigDecimal)right));
        }
        return left.doubleValue() + right.doubleValue();
    }

    public static <T extends Number> T plus(RichIterable<T> numbers)
    {
        Number sum = 0L;
        for (Number n : numbers)
        {
            sum = plus(sum, n);
        }
        return (T)sum;
    }

    public static Number minus(Number number)
    {
        if (number instanceof BigDecimal)
        {
            return ((BigDecimal)number).negate();
        }
        if (number instanceof Long)
        {
            return -number.longValue();
        }
        else
        {
            return -number.doubleValue();
        }
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
        if (left instanceof BigDecimal && right instanceof BigDecimal)
        {
            return ((BigDecimal)left).subtract((BigDecimal)right);
        }
        if (left instanceof BigDecimal && right instanceof Long)
        {
            return ((BigDecimal)left).subtract(BigDecimal.valueOf(right.longValue()));
        }
        if (left instanceof BigDecimal && right instanceof Double)
        {
            return ((BigDecimal)left).subtract(BigDecimal.valueOf(right.doubleValue()));
        }
        if (right instanceof BigDecimal && left instanceof Long)
        {
            return BigDecimal.valueOf(left.longValue()).subtract((BigDecimal)right);
        }
        if (right instanceof BigDecimal && left instanceof Double)
        {
            return BigDecimal.valueOf(left.doubleValue()).subtract(((BigDecimal)right));
        }
        return left.doubleValue() - right.doubleValue();
    }

    public static <T extends Number> T minus(ListIterable<T> numbers)
    {
        int size = numbers.size();
        if (size == 0)
        {
            return (T)Long.valueOf(0);
        }
        else
        {
            Number result = size == 1 ? 0L : numbers.get(0);
            int start = size == 1 ? 0 : 1;

            for (int i = start; i < size; i++)
            {
                result = minus(result, numbers.get(i));
            }
            return (T)result;
        }
    }

    public static <T extends Number> T minus(RichIterable<T> numbers)
    {
        ListIterable<T> numbersList = (ListIterable)numbers;
        return minus(numbersList);
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
        if ((left instanceof Long) && (right instanceof Long))
        {
            return left.longValue() * right.longValue();
        }
        if (left instanceof BigDecimal && right instanceof BigDecimal)
        {
            return ((BigDecimal)left).multiply((BigDecimal)right);
        }
        if (left instanceof BigDecimal && right instanceof Long)
        {
            return ((BigDecimal)left).multiply(BigDecimal.valueOf(right.longValue()));
        }
        if (left instanceof BigDecimal && right instanceof Double)
        {
            return ((BigDecimal)left).multiply(BigDecimal.valueOf(right.doubleValue()));
        }
        if (right instanceof BigDecimal && left instanceof Long)
        {
            return ((BigDecimal)right).multiply(BigDecimal.valueOf(left.longValue()));
        }
        if (right instanceof BigDecimal && left instanceof Double)
        {
            return ((BigDecimal)right).multiply(BigDecimal.valueOf(left.doubleValue()));
        }
        return left.doubleValue() * right.doubleValue();
    }

    public static <T extends Number> T times(RichIterable<T> numbers)
    {
        Number product = 1L;
        for (Number number : numbers)
        {
            product = times(product, number);
        }
        return (T)product;
    }

    public static Long floor(Number number)
    {
        if (number instanceof Long)
        {
            return (Long)number;
        }
        else
        {
            return (long)Math.floor(number.doubleValue());
        }
    }

    public static Long ceiling(Number number)
    {
        if (number instanceof Long)
        {
            return (Long)number;
        }
        else
        {
            return (long)Math.ceil(number.doubleValue());
        }
    }

    public static Long round(Number number)
    {
        if (number instanceof Long)
        {
            return (Long)number;
        }
        else
        {
            double toRound = number.doubleValue();
            if (toRound == 0x1.fffffffffffffp-2) // greatest double value less than 0.5
            {
                return 0L;
            }
            else
            {
                toRound += 0.5d;
                double floor = Math.floor(toRound);
                if ((floor == toRound) && ((floor % 2) != 0))
                {
                    return ((long)floor - 1);
                }
                else
                {
                    return (long)floor;
                }
            }
        }
    }

    public static Number round(Number number, long scale)
    {
        if (number instanceof Double)
        {
            return round((Double)number, scale);
        }
        else if (number instanceof BigDecimal)
        {
            return round((BigDecimal)number, scale);
        }

        throw new IllegalArgumentException("incorrect number type");
    }

    public static double round(Double number, long scale)
    {
        return round(BigDecimal.valueOf(number), scale).doubleValue();
    }

    public static BigDecimal round(BigDecimal number, long scale)
    {
        return number.setScale((int)scale, RoundingMode.HALF_UP);
    }

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
            throw new PureExecutionException("Unable to compute tan of " + input);
        }
        return result;
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
            throw new PureExecutionException(sourceInformation, "Unable to compute asin of " + input);
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
            throw new PureExecutionException(sourceInformation, "Unable to compute acos of " + input);
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
            throw new PureExecutionException(sourceInformation, "Unable to compute atan2 of " + input1 + " " + input2);
        }
        return result;
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
            throw new PureExecutionException(sourceInformation,
                    "Unable to compute sqrt of " + input);
        }
        return result;
    }

    public static Number rem(Number dividend, Number divisor, SourceInformation sourceInformation)
    {
        if (divisor.doubleValue() == 0)
        {
            throw new PureExecutionException(sourceInformation, "Cannot divide " + dividend.toString() + " by zero");
        }

        if (dividend instanceof Long && divisor instanceof Long)
        {
            return dividend.longValue() % divisor.longValue();
        }

        if (dividend instanceof BigDecimal && divisor instanceof BigDecimal)
        {
            return ((BigDecimal)dividend).remainder((BigDecimal)divisor);
        }

        if (dividend instanceof BigDecimal && divisor instanceof Long)
        {
            return ((BigDecimal)dividend).remainder(BigDecimal.valueOf((Long)divisor));
        }

        if (dividend instanceof BigDecimal && divisor instanceof Double)
        {
            return ((BigDecimal)dividend).remainder(BigDecimal.valueOf((Double)divisor));
        }

        if (dividend instanceof Long && divisor instanceof BigDecimal)
        {
            return BigDecimal.valueOf((Long)dividend).remainder((BigDecimal)divisor);
        }

        if (dividend instanceof Double && divisor instanceof BigDecimal)
        {
            return BigDecimal.valueOf((Double)dividend).remainder((BigDecimal)divisor);
        }

        return dividend.doubleValue() % divisor.doubleValue();
    }

    public static Double divide(Number left, Number right, SourceInformation sourceInformation)
    {
        if (right.doubleValue() == 0)
        {
            throw new PureExecutionException(sourceInformation, "Cannot divide " + right + " by zero");
        }

        if (left instanceof BigDecimal && right instanceof BigDecimal)
        {
            return ((BigDecimal)left).divide((BigDecimal)right, RoundingMode.HALF_UP).doubleValue();
        }
        if (left instanceof BigDecimal && right instanceof Long)
        {
            return ((BigDecimal)left).divide(BigDecimal.valueOf(right.longValue()), RoundingMode.HALF_UP).doubleValue();
        }
        if (left instanceof BigDecimal && right instanceof Double)
        {
            return ((BigDecimal)left).divide(BigDecimal.valueOf(right.doubleValue()), RoundingMode.HALF_UP).doubleValue();
        }
        if (right instanceof BigDecimal && left instanceof Long)
        {
            return BigDecimal.valueOf(left.longValue()).divide((BigDecimal)right, RoundingMode.HALF_UP).doubleValue();
        }
        if (right instanceof BigDecimal && left instanceof Double)
        {
            return BigDecimal.valueOf(left.doubleValue()).divide((BigDecimal)right, RoundingMode.HALF_UP).doubleValue();
        }
        return (left instanceof Long ? left.longValue() : left.doubleValue()) / (right instanceof Long ? right.longValue() : right.doubleValue());
    }

    public static BigDecimal divideDecimal(BigDecimal left, BigDecimal right, long scale)
    {
        return left.divide(right, (int)scale, RoundingMode.HALF_UP);
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

    public static boolean lessThan(Number left, Number right)
    {
        if ((left instanceof Long) && (right instanceof Long))
        {
            return left.longValue() < right.longValue();
        }
        else
        {
            return left.doubleValue() < right.doubleValue();
        }
    }

    public static boolean lessThanEqual(Number left, Number right)
    {
        if ((left instanceof Long) && (right instanceof Long))
        {
            return left.longValue() <= right.longValue();
        }
        else
        {
            return left.doubleValue() <= right.doubleValue();
        }
    }

    public static BigDecimal toDecimal(Number number)
    {
        if (number instanceof BigDecimal)
        {
            return (BigDecimal)number;
        }
        return new BigDecimal(number.toString());
    }

    public static double toFloat(Number number)
    {
        return number.doubleValue();
    }

    public static String substring(String str, Number start)
    {
        return substring(str, start, null);
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

    public static boolean endsWith(String str1, String str2)
    {
        return str1.endsWith(str2);
    }

    public static boolean matches(String str, String regexp)
    {
        return str.matches(regexp);
    }

    public static String replace(String str1, String str2, String str3)
    {
        return str1.replace(str2, str3);
    }

    public static boolean contains(String str1, String str2)
    {
        return str1.contains(str2);
    }

    public static Long length(String str)
    {
        return (long) str.length();
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

    public static String encodeBase64(String str)
    {
        return Base64.encodeBase64URLSafeString(str.getBytes());
    }

    public static String decodeBase64(String str)
    {
        return new String(Base64.decodeBase64(str));
    }

    public static BigDecimal parseDecimal(String str)
    {
        return new BigDecimal(str.endsWith("D") || str.endsWith("d") ? str.substring(0, str.length() - 1) : str);
    }

    public static boolean pureAssert(boolean condition, SharedPureFunction function, SourceInformation sourceInformation, ExecutionSupport es)
    {
        if (!condition)
        {
            String message = (String)function.execute(Lists.immutable.empty(), es);
            throw new PureAssertFailException(sourceInformation, message);
        }
        return true;
    }

    public static Object matchFailure(Object obj, SourceInformation sourceInformation)
    {
        throw new PureExecutionException(sourceInformation,
                "Match failure: " + ((obj instanceof RichIterable) ? ((RichIterable<?>)obj).collect(o -> o == null ? null : getErrorMessageForMatchFunctionBasedOnObjectType(o)).makeString("[", ", ", "]") : getErrorMessageForMatchFunctionBasedOnObjectType(obj)));
    }

    private static String getErrorMessageForMatchFunctionBasedOnObjectType(Object obj)
    {
        StringBuilder errorMessage = new StringBuilder();
        if (isTypeOfEnum(obj))
        {
            Enum enumVal = (Enum)obj;
            errorMessage.append(enumVal._name())
                    .append(" instanceOf ")
                    .append(getEnumClassifierName(enumVal));
            return errorMessage.toString();
        }
        if (obj instanceof BaseCoreInstance)
        {
            return obj.toString();
        }
        if (isPureGeneratedClass(obj))
        {
            String tempTypeName = getPureGeneratedClassName(obj);
            errorMessage.append(tempTypeName)
                    .append("Object instanceOf ")
                    .append(tempTypeName);
            return errorMessage.toString();
        }
        String primitiveJavaToPureType = JavaPurePrimitiveTypeMapping.getPureM3TypeFromJavaPrimitivesAndDates(obj);
        if (primitiveJavaToPureType != null)
        {
            errorMessage.append(obj.toString())
                    .append(" instanceOf ")
                    .append(primitiveJavaToPureType);
            return errorMessage.toString();
        }
        errorMessage.append(obj.toString())
                .append(" instanceOf ")
                .append(obj.getClass());
        return errorMessage.toString();
    }

    private static String getEnumClassifierName(Enum obj)
    {
        String[] classifierIdSplitted = obj.getFullSystemPath().split("::");
        return classifierIdSplitted[classifierIdSplitted.length - 1];
    }

    private static String getPureGeneratedClassName(Object obj)
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
        catch (NoSuchFieldException e)
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
                obj instanceof Class && ((Class<?>)obj).getCanonicalName().contains(JavaPackageAndImportBuilder.buildPackageFromSystemPath(null));
    }

    private static String getFieldValue(Object obj, String fieldName)
    {
        try
        {
            Field f = obj.getClass().getField(fieldName);
            f.setAccessible(true);
            return (String)f.get(obj);
        }
        catch (IllegalAccessException | NoSuchFieldException e)
        {
            return null;
        }
    }

    public static <T> T mutateAdd(T val, String property, RichIterable<? extends Object> vals, SourceInformation sourceInformation)
    {
        try
        {
            Method m = val.getClass().getMethod("_" + property);
            if (m.getReturnType() == RichIterable.class)
            {
                RichIterable l = (RichIterable)m.invoke(val);
                RichIterable newValues = Iterate.isEmpty(l) ? vals : LazyIterate.concatenate(l, vals).toList();

                m = val.getClass().getMethod("_" + property, RichIterable.class);
                m.invoke(val, newValues);
            }
            else
            {
                m = val.getClass().getMethod("_" + property, m.getReturnType());
                m.invoke(val, vals.getFirst());
            }
        }
        catch (NoSuchMethodException e)
        {
            throw new PureExecutionException(sourceInformation, "Cannot find property '" + property + "' on " + getPureGeneratedClassName(val));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        return val;
    }

    public static <T> RichIterable<T> slice(T element, long low, long high, SourceInformation sourceInformation)
    {
        return ((element == null) || (low > 0) || (high <= 0)) ? Lists.immutable.<T>empty() : Lists.immutable.with(element);
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
            return ((LazyIterable<T>)collection).drop((int)low).take((int)(high - low));
        }

        int collectionSize = collection.size();
        if (low >= collectionSize)
        {
            return Lists.immutable.empty();
        }
        int start = (int)low;
        int end = (high > collectionSize) ? collectionSize : (int)high;
        if (start > end)
        {
            throw new PureExecutionException(sourceInformation, "The low bound (" + start + ") can't be higher than the high bound (" + end + ") in a slice operation");
        }


        if (collection instanceof ListIterable)
        {
            return ListHelper.subList((ListIterable<T>)collection, start, end);
        }

        MutableList<T> result = FastList.newList(end - start);
        result.addAllIterable(LazyIterate.drop(collection, start).take(end - start));
        return result;
    }

    public static RichIterable<String> chunk(String text, long size, SourceInformation sourceInformation)
    {
        if (size < 1)
        {
            throw new PureExecutionException(sourceInformation, "Invalid chunk size: " + size);
        }
        return chunk(text, (int)size);
    }

    private static RichIterable<String> chunk(final String text, final int size)
    {
        final int length = text.length();
        if (length == 0)
        {
            return Lists.immutable.empty();
        }

        if (size >= length)
        {
            return Lists.immutable.with(text);
        }

        return new AbstractLazyIterable<String>()
        {
            @Override
            public boolean isEmpty()
            {
                return false;
            }

            @Override
            public int size()
            {
                return (length + size - 1) / size;
            }

            @Override
            public void each(Procedure<? super String> procedure)
            {
                for (int i = 0; i < length; i += size)
                {
                    procedure.value(text.substring(i, Math.min(i + size, length)));
                }
            }

            @Override
            public Iterator<String> iterator()
            {
                return new Iterator<String>()
                {
                    private int current = 0;

                    @Override
                    public boolean hasNext()
                    {
                        return this.current < length;
                    }

                    @Override
                    public String next()
                    {
                        if (!hasNext())
                        {
                            throw new NoSuchElementException();
                        }
                        int start = this.current;
                        int end = Math.min(start + size, length);
                        String next = text.substring(start, end);
                        this.current = end;
                        return next;
                    }
                };
            }
        };
    }

    public static Object dynamicallyBuildLambdaFunction(CoreInstance lambdaFunction, ExecutionSupport es)
    {
        ClassLoader globalClassLoader = ((CompiledExecutionSupport)es).getClassLoader();
        CompiledProcessorSupport compiledSupport = new CompiledProcessorSupport(globalClassLoader, ((CompiledExecutionSupport)es).getMetadata(), ((CompiledExecutionSupport)es).getExtraSupportedTypes());
        ProcessorContext processorContext = new ProcessorContext(compiledSupport);
        processorContext.setInLineAllLambda(true);

        String name = "DynamicLambdaGeneration";
        String _class = JavaSourceCodeGenerator.imports + "\nimport " + JavaPackageAndImportBuilder.rootPackage() + ".*;\npublic class " + name + "{" +
                "   public static PureCompiledLambda build(final MutableMap<String, Object> valMap, final IntObjectMap<CoreInstance> localLambdas){\n" +
                "return " + ValueSpecificationProcessor.processLambda(null, lambdaFunction, compiledSupport, processorContext) + ";" +
                "}" +
                "}";

        MemoryFileManager fileManager = ((CompiledExecutionSupport)es).getMemoryFileManager();
        MutableList<StringJavaSource> javaClasses = FastList.newList();
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


    public static Object dynamicallyEvaluateValueSpecification(final CoreInstance valueSpecification, PureMap lambdaOpenVariablesMap,
                                                               ExecutionSupport es)
    {
        MemoryFileManager fileManager = ((CompiledExecutionSupport)es).getMemoryFileManager();
        ClassLoader globalClassLoader = ((CompiledExecutionSupport)es).getClassLoader();

        final CompiledProcessorSupport compiledSupport = new CompiledProcessorSupport(globalClassLoader, ((CompiledExecutionSupport)es).getMetadata(), ((CompiledExecutionSupport)es).getExtraSupportedTypes());
        final ProcessorContext processorContext = new ProcessorContext(compiledSupport);

        // Don't do anything if the ValueSpecification is already resolved ----------------
        if (Instance.instanceOf(valueSpecification, M3Paths.InstanceValue, processorContext.getSupport()))
        {
            ListIterable<? extends CoreInstance> l = valueSpecification.getValueForMetaPropertyToMany(M3Properties.values);
            if (l.noneSatisfy(instance -> Instance.instanceOf(instance, M3Paths.ValueSpecification, processorContext.getSupport()) || Instance.instanceOf(instance, M3Paths.LambdaFunction, processorContext.getSupport())))
            {
                ListIterable<Object> result = l.collect(instance -> instance instanceof ValCoreInstance ? ((ValCoreInstance)instance).getValue() : instance);
                return result.size() == 1 ? result.get(0) : result;
            }
        }
        //---------------------------------------------------------------------------------

        processorContext.setInLineAllLambda(true);
        String processed = ValueSpecificationProcessor.processValueSpecification(valueSpecification, true, processorContext);
        String returnType = TypeProcessor.typeToJavaObjectWithMul(valueSpecification.getValueForMetaPropertyToOne(M3Properties.genericType), valueSpecification.getValueForMetaPropertyToOne(M3Properties.multiplicity), false, compiledSupport);

        String name = "DynaClass";
        RichIterable<Pair<String, CoreInstance>> values = lambdaOpenVariablesMap.getMap().keyValuesView();
        final MutableMap<String, Object> openVars = Maps.mutable.of();
        String _class = JavaSourceCodeGenerator.imports + "\npublic class " + name +
                "{\n" +
                "   public static " + returnType + " doProcess(final MapIterable<String, Object> vars, final MutableMap<String, Object> valMap, final IntObjectMap<CoreInstance> localLambdas, final ExecutionSupport es){\n" +
                values.collect(
                        new Function<Pair<String, CoreInstance>, String>()
                        {
                            @Override
                            public String valueOf(Pair<String, CoreInstance> pair)
                            {
                                final String name = pair.getOne();
                                CoreInstance valuesCoreInstance = pair.getTwo();
                                ListIterable<? extends CoreInstance> values = valuesCoreInstance.getValueForMetaPropertyToMany(M3Properties.values).select(coreInstance -> !Instance.instanceOf(coreInstance, "meta::pure::executionPlan::PlanVarPlaceHolder", compiledSupport) && !Instance.instanceOf(coreInstance, "meta::pure::executionPlan::PlanVariablePlaceHolder", compiledSupport));
                                String type = null;
                                openVars.put(name, valuesCoreInstance);
                                if (values.isEmpty())
                                {
                                    MutableList<CoreInstance> vars = FastList.newList();
                                    collectVars(valueSpecification, vars, compiledSupport);
                                    CoreInstance found = vars.detect(coreInstance -> coreInstance.getValueForMetaPropertyToOne("name").getName().equals(name));
                                    if (found != null)
                                    {
                                        type = TypeProcessor.typeToJavaObjectSingle(found.getValueForMetaPropertyToOne(M3Properties.genericType), false, compiledSupport);
                                        return "      final  " + type + "  _" + name + " = null;";
                                    }
                                    return "";
                                }
                                else
                                {
                                    type = TypeProcessor.pureRawTypeToJava(compiledSupport.getClassifier(values.getFirst()), false, compiledSupport);
                                    final String listImpl = JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.List);
                                    return (values.size() == 1) ? ("      final " + type + " _" + name + " = (" + type + ")((" + listImpl + ")vars.get(\"" + name + "\"))._values.getFirst();") : ("      final RichIterable<" + type + "> _" + name + " = ((" + listImpl + ")vars.get(\"" + name + "\"))._values;");
                                }
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

    public static StrictDate today()
    {
        return StrictDate.fromCalendar(new GregorianCalendar(GMT));
    }

    public static DateTime now()
    {
        return (DateTime)DateFunctions.fromDate(new Date());
    }

    public static PureDate datePart(PureDate date)
    {
        return date.hasHour() ? DateFunctions.newPureDate(date.getYear(), date.getMonth(), date.getDay()) : date;
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

    public static long weekOfYear(PureDate date, SourceInformation sourceInformation)
    {
        if (!date.hasDay())
        {
            throw new PureExecutionException(sourceInformation, "Cannot get week of year for " + date);
        }
        return date.getCalendar().get(Calendar.WEEK_OF_YEAR);
    }

    public static boolean hasDay(PureDate date)
    {
        return date.hasDay();
    }

    public static long dayOfWeekNumber(PureDate date, SourceInformation sourceInformation)
    {
        if (!date.hasDay())
        {
            throw new PureExecutionException(sourceInformation, "Cannot get day of week for " + date);
        }
        switch (date.getCalendar().get(Calendar.DAY_OF_WEEK))
        {
            case Calendar.MONDAY:
            {
                return 1;
            }
            case Calendar.TUESDAY:
            {
                return 2;
            }
            case Calendar.WEDNESDAY:
            {
                return 3;
            }
            case Calendar.THURSDAY:
            {
                return 4;
            }
            case Calendar.FRIDAY:
            {
                return 5;
            }
            case Calendar.SATURDAY:
            {
                return 6;
            }
            case Calendar.SUNDAY:
            {
                return 7;
            }
            default:
            {
                throw new PureExecutionException(sourceInformation, "Error getting day of week for " + date);
            }
        }
    }

    public static long dayOfMonth(PureDate date, SourceInformation sourceInformation)
    {
        if (!date.hasDay())
        {
            throw new PureExecutionException(sourceInformation, "Cannot get day of month for " + date);
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
            throw new PureExecutionException(sourceInformation, "Cannot get hour for " + date);
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
            throw new PureExecutionException(sourceInformation, "Cannot get minute for " + date);
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
            throw new PureExecutionException(sourceInformation, "Cannot get second for " + date);
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
            return Year.newYear((int)year);
        }
        catch (Exception e)
        {
            throw new PureExecutionException(sourceInformation, e.getMessage());
        }
    }

    public static PureDate newDate(long year, long month, SourceInformation sourceInformation)
    {
        try
        {
            return YearMonth.newYearMonth((int)year, (int)month);
        }
        catch (Exception e)
        {
            throw new PureExecutionException(sourceInformation, e.getMessage());
        }
    }

    public static StrictDate newDate(long year, long month, long day, SourceInformation sourceInformation)
    {
        try
        {
            return DateFunctions.newPureDate((int)year, (int)month, (int)day);
        }
        catch (Exception e)
        {
            throw new PureExecutionException(sourceInformation, e.getMessage());
        }
    }

    public static DateTime newDate(long year, long month, long day, long hour, SourceInformation sourceInformation)
    {
        try
        {
            return DateFunctions.newPureDate((int)year, (int)month, (int)day, (int)hour);
        }
        catch (Exception e)
        {
            throw new PureExecutionException(sourceInformation, e.getMessage());
        }
    }

    public static DateTime newDate(long year, long month, long day, long hour, long minute, SourceInformation sourceInformation)
    {
        try
        {
            return DateFunctions.newPureDate((int)year, (int)month, (int)day, (int)hour, (int)minute);
        }
        catch (Exception e)
        {
            throw new PureExecutionException(sourceInformation, e.getMessage());
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
            String string = ((BigDecimal)second).toPlainString();
            int index = string.indexOf('.');
            if (index != -1)
            {
                subsecond = string.substring(index + 1);
            }
        }
        else
        {
            throw new PureExecutionException(sourceInformation, "Unhandled number: " + second);
        }
        try
        {
            return (subsecond == null) ? DateFunctions.newPureDate((int)year, (int)month, (int)day, (int)hour, (int)minute, secondInt) : DateFunctions.newPureDate((int)year, (int)month, (int)day, (int)hour, (int)minute, secondInt, subsecond);
        }
        catch (Exception e)
        {
            throw new PureExecutionException(sourceInformation, e.getMessage());
        }
    }

    public static PureDate adjustDate(PureDate date, long number, Enum unit)
    {
        switch (unit._name())
        {
            case "YEARS":
            {
                return date.addYears((int)number);
            }
            case "MONTHS":
            {
                return date.addMonths((int)number);
            }
            case "WEEKS":
            {
                return date.addWeeks((int)number);
            }
            case "DAYS":
            {
                return date.addDays((int)number);
            }
            case "HOURS":
            {
                return date.addHours((int)number);
            }
            case "MINUTES":
            {
                return date.addMinutes((int)number);
            }
            case "SECONDS":
            {
                return date.addSeconds((int)number);
            }
            case "MILLISECONDS":
            {
                return date.addMilliseconds((int)number);
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
                throw new PureExecutionException("Unsupported duration unit: " + unit);
            }
        }
    }

    public static long dateDiff(PureDate date1, PureDate date2, Enum unit)
    {
        return date1.dateDifference(date2, unit._name());
    }

    public static String escapeJSON(String str)
    {
        return JSONValue.escape(str);
    }

    public static String currentUserId()
    {
        return IdentityManager.getAuthenticatedUserId();
    }

    public static boolean isOptionSet(String name, ExecutionSupport es)
    {
        return ((CompiledExecutionSupport)es).getRuntimeOptions().isOptionSet(name);
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

    public static String guid()
    {
        return UUID.randomUUID().toString();
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

    public static long indexOf(Object instances, Object object)
    {
        if (instances == null)
        {
            return -1L;
        }
        if (!(instances instanceof Iterable))
        {
            return instances.equals(object) ? 0L : -1L;
        }
        return indexOf((Iterable<?>)instances, object);
    }

    public static long indexOf(Iterable<?> instances, Object object)
    {
        if (instances == null)
        {
            return -1L;
        }
        if (instances instanceof OrderedIterable)
        {
            return ((OrderedIterable<?>)instances).indexOf(object);
        }
        if (instances instanceof List)
        {
            return ((List<?>)instances).indexOf(object);
        }
        long index = 0L;
        for (Object instance : instances)
        {
            if (Objects.equals(instance, object))
            {
                return index;
            }
            index++;
        }
        return -1L;
    }

    public static <T> T notSupportedYet()
    {
        throw new RuntimeException("Not supported yet!");
    }

    public static <T> T removeOverride(T instance)
    {
        return (T)((Any)instance)._elementOverrideRemove();
    }

    public static PureMap put(PureMap pureMap, Object key, Object val)
    {
        Map map = pureMap.getMap();
        MutableMap newOne = map instanceof UnifiedMapWithHashingStrategy ? new UnifiedMapWithHashingStrategy(((UnifiedMapWithHashingStrategy)map).hashingStrategy(), map) : new UnifiedMap(map);
        newOne.put(key, val);
        return new PureMap(newOne);
    }

    public static RichIterable values(PureMap map)
    {
        return map.getMap().valuesView().toList();
    }

    public static RichIterable keys(PureMap map)
    {
        return map.getMap().keysView().toList();
    }

    public static String readFile(String path, ExecutionSupport es)
    {
        return ((CompiledExecutionSupport)es).getCodeStorage().exists(path) ? ((CompiledExecutionSupport)es).getCodeStorage().getContentAsText(path) : null;
    }

    public static Object executeFunction(CoreInstance functionDefinition, Class<?>[] paramClasses, Object[] params, ExecutionSupport executionSupport)
    {
        return executeFunction(IdBuilder.sourceToId(functionDefinition.getSourceInformation()), (ConcreteFunctionDefinition<?>)functionDefinition, paramClasses, params, executionSupport);
    }


    public static Object executeFunction(String uniqueFunctionId, ConcreteFunctionDefinition<?> functionDefinition, Class<?>[] paramClasses, Object[] params, ExecutionSupport es)
    {
        SharedPureFunction<?> spf = ((CompiledExecutionSupport)es).getFunctionCache().getIfAbsentPutJavaFunctionForPureFunction(functionDefinition, () ->
        {
            try
            {
                Class<?> clazz = ((CompiledExecutionSupport)es).getClassLoader().loadClass(JavaPackageAndImportBuilder.rootPackage() + '.' + uniqueFunctionId);
                String functionName = FunctionProcessor.functionNameToJava(functionDefinition);
                Method method = getFunctionMethod(clazz, functionName, functionDefinition, paramClasses, params, es);
                return new JavaMethodWithParamsSharedPureFunction(method, paramClasses, functionDefinition.getSourceInformation());
            }
            catch (ClassNotFoundException e)
            {
                throw new PureExecutionException("Unable to execute " + uniqueFunctionId, e);
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
            throw new PureExecutionException(buildFunctionExecutionErrorMessage(functionDefinition, params, "Function was not found.", executionSupport), e);
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
            throw new PureExecutionException(buildFunctionExecutionErrorMessage(functionDefinition, params, "Input parameters are invalid.", executionSupport), iae);
        }
        catch (IllegalAccessException ex)
        {
            throw new PureExecutionException(buildFunctionExecutionErrorMessage(functionDefinition, params, "Failed to invoke java function.", executionSupport), ex);
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

    public static Object validate(boolean goDeep, final Object o, final SourceInformation si, final ExecutionSupport es)
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
            throw new PureExecutionException("Failed to invoke _validate function.", ex);
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

    private static String buildFunctionExecutionErrorMessage(CoreInstance functionDefinition, Object[] params, String reason, ExecutionSupport es)
    {
        StringBuilder builder = new StringBuilder("Error executing ");
        org.finos.legend.pure.m3.navigation.function.Function.print(builder, functionDefinition, ((CompiledExecutionSupport)es).getProcessorSupport());

        if (params != null)
        {
            builder.append(" with parameters ");
            MutableList<String> paramVals = new FastList<>(params.length);
            for (Object param : params)
            {
                paramVals.add(pureToString(param, es));
            }
            builder.append(paramVals.makeString("[", ", ", "]"));
        }

        builder.append(". ").append(reason);
        return builder.toString();
    }

    public static <T> RichIterable<? extends T> castWithExceptionHandling(RichIterable<?> sourceCollection, Class<?> targetType, SourceInformation sourceInformation)
    {
        return (sourceCollection == null) ? Lists.immutable.empty() : sourceCollection.collect(sourceObject -> castWithExceptionHandling(sourceObject, targetType, sourceInformation));
    }

    public static <T> T castWithExceptionHandling(Object sourceObject, Class<?> targetType, SourceInformation sourceInformation)
    {
        if (sourceObject != null && !targetType.isInstance(sourceObject))
        {
            String[] castTypeClassName = targetType.getSimpleName().split("_");
            String errorMessage = "Cast exception: " + getPureClassName(sourceObject) + " cannot be cast to " + castTypeClassName[castTypeClassName.length - 1];
            throw new PureExecutionException(sourceInformation, errorMessage);
        }
        return (T)sourceObject;
    }

    public static long safeSize(Object obj)
    {
        if (obj == null)
        {
            return 0L;
        }
        if (obj instanceof RichIterable)
        {
            return ((RichIterable<?>)obj).size();
        }
        return 1L;
    }

    public static String getPureClassName(Object obj)
    {
        if (isTypeOfEnum(obj))
        {
            return getEnumClassifierName((Enum)obj);
        }
        if (isPureGeneratedClass(obj))
        {
            if (!(obj instanceof Class && ((Class<?>)obj).isInterface()))
            {
                return getPureGeneratedClassName(obj);
            }
        }
        String primitiveJavaToPureType = obj instanceof Class ?
                JavaPurePrimitiveTypeMapping.getPureM3TypeFromJavaPrimitivesAndDates((Class<?>)obj) :
                JavaPurePrimitiveTypeMapping.getPureM3TypeFromJavaPrimitivesAndDates(obj);
        if (primitiveJavaToPureType != null)
        {
            return primitiveJavaToPureType;
        }
        String[] defaultName = obj.toString().split("_");
        return defaultName[defaultName.length - 1];
    }

    public static String fullyQualifiedJavaInterfaceNameForPackageableElement(final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement element)
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

    public static Type getType(Any val, final MetadataAccessor metadata)
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
        else
        {
            MutableList<String> pkgPath = getUserObjectPathForPackageableElement(pkg, includeRoot);
            pkgPath.add(packageableElement.getName());
            return pkgPath;
        }
    }

    public static String functionDescriptorToId(String functionDescriptor, SourceInformation sourceInformation)
    {
        String id;
        try
        {
            id = FunctionDescriptor.functionDescriptorToId(functionDescriptor);
        }
        catch (InvalidFunctionDescriptorException e)
        {
            throw new PureExecutionException(sourceInformation, "Invalid function descriptor: " + functionDescriptor, e);
        }
        return id;
    }

    public static boolean isValidFunctionDescriptor(String possiblyFunctionDescriptor)
    {
        return FunctionDescriptor.isValidFunctionDescriptor(possiblyFunctionDescriptor);
    }

    public static Object newObject(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> aClass, RichIterable<? extends KeyValue> keyExpressions, ElementOverride override, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function getterToOne, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function getterToMany, Object payload, PureFunction2 getterToOneExec, PureFunction2 getterToManyExec, ExecutionSupport es)
    {
        ClassCache classCache = ((CompiledExecutionSupport)es).getClassCache();
        Constructor<?> constructor = classCache.getIfAbsentPutConstructorForType(aClass);
        Any result;
        try
        {
            result = (Any) constructor.newInstance("");
        }
        catch (InvocationTargetException | InstantiationException | IllegalAccessException e)
        {
            Throwable cause = (e instanceof InvocationTargetException) ? e.getCause() : e;
            StringBuilder builder = new StringBuilder("Error instantiating ");
            PackageableElement.writeUserPathForPackageableElement(builder, aClass);
            String eMessage = cause.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw new RuntimeException(builder.toString(), cause);
        }
        keyExpressions.forEach(keyValue ->
        {
            Method m = classCache.getIfAbsentPutPropertySetterMethodForType(aClass, keyValue._key());
            try
            {
                m.invoke(result, keyValue._value());
            }
            catch (InvocationTargetException | IllegalAccessException e)
            {
                Throwable cause = (e instanceof InvocationTargetException) ? e.getCause() : e;
                StringBuilder builder = new StringBuilder("Error setting property '").append(keyValue._key()).append("' for instance of ");
                PackageableElement.writeUserPathForPackageableElement(builder, aClass);
                String eMessage = cause.getMessage();
                if (eMessage != null)
                {
                    builder.append(": ").append(eMessage);
                }
                throw new RuntimeException(builder.toString(), cause);
            }
        });
        PureFunction2Wrapper getterToOneExecFunc = getterToOneExec == null ? null : new PureFunction2Wrapper(getterToOneExec, es);
        PureFunction2Wrapper getterToManyExecFunc = getterToManyExec == null ? null : new PureFunction2Wrapper(getterToManyExec, es);
        ElementOverride elementOverride = override;
        if (override instanceof GetterOverride)
        {
            elementOverride = ((GetterOverride)elementOverride)._getterOverrideToOne(getterToOne)._getterOverrideToMany(getterToMany)._hiddenPayload(payload);
            ((GetterOverrideExecutor)elementOverride).__getterOverrideToOneExec(getterToOneExecFunc);
            ((GetterOverrideExecutor)elementOverride).__getterOverrideToManyExec(getterToManyExecFunc);
        }
        result._elementOverride(elementOverride);
        return result;
    }

    public static String encrypt(String value, String key)
    {
        return performEncryption(value, key);
    }

    public static String encrypt(Number value, String key)
    {
        return performEncryption(value.toString(), key);
    }

    public static String encrypt(Boolean value, String key)
    {
        return performEncryption(value.toString(), key);
    }

    private static String performEncryption(String value, String key)
    {
        try
        {
            return new String(AESCipherUtil.encrypt(key, value.getBytes()));
        }
        catch (Exception e)
        {
            throw new PureExecutionException("Error ciphering value '" + value + "' with key '" + key + "'.");
        }
    }

    public static String decrypt(String value, String key)
    {
        try
        {
            return new String(AESCipherUtil.decrypt(key, value.getBytes()));
        }
        catch (Exception e)
        {
            throw new PureExecutionException("Error deciphering value '" + value + "' with key '" + key + "'.");
        }
    }

    public static HTTPResponse executeHttpRaw(URL url, Object method, String mimeType, String body, ExecutionSupport executionSupport)
    {
        return (HTTPResponse)HttpRawHelper.toHttpResponseInstance(HttpRawHelper.executeHttpService(url._host(), (int)url._port(), url._path(), HttpMethod.valueOf(((Enum)method)._name()), mimeType, body), ((CompiledExecutionSupport)executionSupport).getProcessorSupport());
    }
}
