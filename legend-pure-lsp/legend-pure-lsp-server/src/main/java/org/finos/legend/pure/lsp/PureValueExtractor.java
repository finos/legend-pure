// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.lsp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.PrimitiveCoreInstance;

/**
 * Turns the {@code CoreInstance} returned by {@code FunctionExecutionInterpreted#start} into a
 * JSON-able value, so a function returning a primitive does not have to {@code print()} its result
 * just to get it back out of legend/execute.
 * <p>
 * Only primitives and enums are rendered. Anything else reports its type and element count with a
 * null value: walking an arbitrary instance graph risks cycles and unbounded payloads, and the
 * caller can still fall back to {@code print()}.
 * <p>
 * The unwrapping and type-naming helpers are shared with {@code LegendDebugState}, which renders
 * the same graph values as human-readable DAP variable strings.
 */
public final class PureValueExtractor
{
    /**
     * Collections above this are truncated. {@link ExtractedValue#getSize()} still reports the true
     * total, so a caller can tell a capped result from a complete one.
     */
    public static final int MAX_VALUES = 1000;

    public static final String KIND_PRIMITIVE = "primitive";
    public static final String KIND_ENUM = "enum";
    public static final String KIND_COLLECTION = "collection";
    public static final String KIND_EMPTY = "empty";
    public static final String KIND_COMPLEX = "complex";

    private PureValueExtractor()
    {
    }

    public static final class ExtractedValue
    {
        private final String kind;
        private final String type;
        private final Object value;
        private final int size;
        private final boolean truncated;

        private ExtractedValue(String kind, String type, Object value, int size, boolean truncated)
        {
            this.kind = kind;
            this.type = type;
            this.value = value;
            this.size = size;
            this.truncated = truncated;
        }

        public String getKind()
        {
            return this.kind;
        }

        public String getType()
        {
            return this.type;
        }

        public Object getValue()
        {
            return this.value;
        }

        public int getSize()
        {
            return this.size;
        }

        public boolean isTruncated()
        {
            return this.truncated;
        }
    }

    private static final ExtractedValue EMPTY = new ExtractedValue(KIND_EMPTY, null, null, 0, false);

    /**
     * Never throws: a value that cannot be inspected is reported as {@link #KIND_COMPLEX} rather
     * than failing an execution that otherwise succeeded.
     */
    public static ExtractedValue extract(CoreInstance result, ProcessorSupport processorSupport)
    {
        if (result == null || processorSupport == null)
        {
            return EMPTY;
        }
        try
        {
            return doExtract(result, processorSupport);
        }
        catch (Exception e)
        {
            return new ExtractedValue(KIND_COMPLEX, null, null, 0, false);
        }
    }

    private static ExtractedValue doExtract(CoreInstance result, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> values = valueSpecificationValues(result, processorSupport);
        List<CoreInstance> elements = new ArrayList<>();
        if (values == null)
        {
            // start() wraps its result, but a caller may hand us a bare value.
            elements.add(result);
        }
        else
        {
            values.forEach(elements::add);
        }

        int size = elements.size();
        if (size == 0)
        {
            return EMPTY;
        }

        int renderCount = Math.min(size, MAX_VALUES);
        List<Object> rendered = new ArrayList<>(renderCount);
        boolean allEnums = true;
        for (int i = 0; i < renderCount; i++)
        {
            CoreInstance element = elements.get(i);
            if (element instanceof PrimitiveCoreInstance)
            {
                rendered.add(normalize(((PrimitiveCoreInstance<?>) element).getValue()));
                allEnums = false;
            }
            else if (isEnumValue(element, processorSupport))
            {
                rendered.add(enumName(element));
            }
            else
            {
                return new ExtractedValue(KIND_COMPLEX, typeName(elements.get(0), processorSupport), null, size, false);
            }
        }

        String type = typeName(elements.get(0), processorSupport);
        boolean scalar = size == 1 && !isCollectionValueSpecification(result, processorSupport);
        if (scalar)
        {
            return new ExtractedValue(allEnums ? KIND_ENUM : KIND_PRIMITIVE, type, rendered.get(0), 1, false);
        }
        return new ExtractedValue(KIND_COLLECTION, type, rendered, size, size > MAX_VALUES);
    }

    /**
     * Pure values reach us as whatever the m4 primitive wrappers hold. Gson renders the number and
     * boolean types natively; everything else (notably PureDate and PureStrictTime) has no JSON
     * analogue and goes out as its Pure string form.
     */
    private static Object normalize(Object raw)
    {
        if (raw == null
                || raw instanceof String
                || raw instanceof Boolean
                || raw instanceof Integer
                || raw instanceof Long
                || raw instanceof Short
                || raw instanceof Byte
                || raw instanceof Double
                || raw instanceof Float
                || raw instanceof BigDecimal)
        {
            return raw;
        }
        if (raw instanceof BigInteger)
        {
            BigInteger big = (BigInteger) raw;
            return big.bitLength() < Long.SIZE ? (Object) big.longValue() : big.toString();
        }
        return String.valueOf(raw);
    }

    private static boolean isEnumValue(CoreInstance value, ProcessorSupport processorSupport)
    {
        try
        {
            return processorSupport.instance_instanceOf(value, M3Paths.Enum);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private static String enumName(CoreInstance value)
    {
        CoreInstance name = value.getValueForMetaPropertyToOne(M3Properties.name);
        return name == null ? value.getName() : name.getName();
    }

    /**
     * Shared with {@code LegendDebugState}: unwraps an InstanceValue/ValueSpecification to the
     * values it holds, or null when {@code value} is not a value specification at all.
     */
    public static ListIterable<? extends CoreInstance> valueSpecificationValues(CoreInstance value, ProcessorSupport processorSupport)
    {
        if (value == null)
        {
            return null;
        }
        try
        {
            return (ValueSpecification.isInstanceValue(value, processorSupport)
                    || ValueSpecification.isNonExecutableValueSpecification(value, processorSupport))
                    ? ValueSpecification.getValues(value, processorSupport)
                    : null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Shared with {@code LegendDebugState}: true when the value specification's multiplicity is not
     * to-one, which is what distinguishes {@code String[*]} holding one element from {@code String[1]}.
     */
    public static boolean isCollectionValueSpecification(CoreInstance value, ProcessorSupport processorSupport)
    {
        if (value == null)
        {
            return false;
        }
        try
        {
            if (!ValueSpecification.isValueSpecification(value, processorSupport))
            {
                return false;
            }
            CoreInstance multiplicity = value.getValueForMetaPropertyToOne(M3Properties.multiplicity);
            return multiplicity != null && !Multiplicity.isToOne(multiplicity);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * Shared with {@code LegendDebugState}: the value's Pure type, as a full path for packageable
     * classifiers and a bare name otherwise.
     */
    public static String typeName(CoreInstance value, ProcessorSupport processorSupport)
    {
        if (value == null)
        {
            return "Nil";
        }
        CoreInstance classifier = processorSupport.getClassifier(value);
        if (classifier == null)
        {
            return value.getClass().getSimpleName();
        }

        try
        {
            if (PackageableElement.isPackageableElement(classifier, processorSupport))
            {
                String path = PackageableElement.getUserPathForPackageableElement(classifier);
                if (path != null && !path.isEmpty())
                {
                    return path;
                }
            }
        }
        catch (Exception ignored)
        {
        }
        return classifier.getName() == null ? classifier.getClass().getSimpleName() : classifier.getName();
    }
}
