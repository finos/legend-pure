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

package org.finos.legend.pure.rust;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.enumeration.Enumeration;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.PrimitiveCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.StrictTimeFunctions;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The host side of the node bridge, shared by every transport: the Java
 * graph nodes the Rust side holds by id, their description in one record,
 * their reads (property values, keys, a lambda's captured values), the
 * elements and enum values Rust names by path, the dates and times it hands
 * back as components, and how a Java failure is reported to it. Node ids are
 * never reused: a node Rust no longer holds is forgotten and gets a fresh id
 * if it is described again. Every method only reads the Java graph; none
 * re-enters Rust.
 */
public class NodeBridge
{
    private final RustEngineHost host;
    private final ProcessorSupport processorSupport;
    private final IdentityHashMap<CoreInstance, Long> ids = new IdentityHashMap<>();
    private final Map<Long, CoreInstance> nodes = new HashMap<>();
    private final Map<Object, Long> definitions = new HashMap<>();
    private long nextId;
    private CoreInstance lambdaClass;

    public NodeBridge(RustEngineHost host)
    {
        this.host = host;
        this.processorSupport = host.processorSupport();
    }

    /**
     * The engine this bridge serves.
     *
     * @return the host
     */
    public RustEngineHost host()
    {
        return this.host;
    }

    /**
     * Forget every node and definition.
     */
    public void clear()
    {
        this.ids.clear();
        this.nodes.clear();
        this.definitions.clear();
    }

    private long idOf(CoreInstance node)
    {
        Long id = this.ids.get(node);
        if (id == null)
        {
            id = this.nextId++;
            this.nodes.put(id, node);
            this.ids.put(node, id);
        }
        return id;
    }

    /**
     * The Rust side holds no node under these ids any more.
     *
     * @param released the ids released
     */
    public void forget(long[] released)
    {
        for (long id : released)
        {
            CoreInstance node = this.nodes.remove(id);
            if (node != null)
            {
                this.ids.remove(node);
            }
        }
    }

    /**
     * Graph nodes the Rust side may still read, for tests.
     *
     * @return the number of nodes held by id
     */
    public int knownNodeCount()
    {
        return this.nodes.size();
    }

    private CoreInstance lambdaClass()
    {
        if (this.lambdaClass == null)
        {
            this.lambdaClass = this.processorSupport.package_getByUserPath(M3Paths.LambdaFunction);
        }
        return this.lambdaClass;
    }

    private static String userPath(CoreInstance element)
    {
        return PackageableElement.getUserPathForPackageableElement(element, "::");
    }

    /**
     * One node in one record: its kind, its id (assigned on first sight),
     * a primitive's payload, an element's path, a lambda value's definition.
     *
     * @param ci the node
     * @return its description
     */
    public NodeDescription describe(CoreInstance ci)
    {
        NodeDescription d = new NodeDescription();
        if (ci instanceof RustCoreInstance)
        {
            d.kind = NodeDescription.RUST_OBJECT;
            d.id = ((RustCoreInstance) ci).handle();
            return d;
        }
        CoreInstance classifier = this.processorSupport.getClassifier(ci);
        d.classifier = (classifier == null) ? null : userPath(classifier);
        d.name = ci.getName();
        SourceInformation source = ci.getSourceInformation();
        if (source != null)
        {
            d.sourceId = source.getSourceId();
            d.line = source.getLine();
            d.column = source.getColumn();
            d.endLine = source.getEndLine();
            d.endColumn = source.getEndColumn();
        }
        if (ci instanceof PrimitiveCoreInstance)
        {
            describePrimitive(d, ((PrimitiveCoreInstance<?>) ci).getValue());
        }
        else if ((classifier != null) && Instance.instanceOf(classifier, M3Paths.Enumeration, this.processorSupport))
        {
            d.kind = NodeDescription.ENUM_VALUE;
            d.id = idOf(ci);
        }
        else if (Instance.instanceOf(ci, M3Paths.PackageableElement, this.processorSupport))
        {
            d.kind = NodeDescription.PACKAGEABLE_ELEMENT;
            d.id = idOf(ci);
            d.userPath = userPath(ci);
        }
        else
        {
            d.kind = NodeDescription.NODE;
            d.id = idOf(ci);
            if (classifier == lambdaClass())
            {
                Object definition = this.host.lambdaDefinition(ci);
                if (definition != null)
                {
                    d.definition = this.definitions.computeIfAbsent(definition, k -> this.nextId++);
                }
            }
        }
        return d;
    }

    /**
     * A primitive by its classifier (the M3 primitive types are root-level
     * names) and its Java value.
     */
    private static void describePrimitive(NodeDescription d, Object value)
    {
        String type = d.classifier;
        if (M3Paths.Boolean.equals(type))
        {
            d.kind = NodeDescription.BOOLEAN;
            d.payload = value.toString();
        }
        else if (M3Paths.Integer.equals(type))
        {
            d.kind = NodeDescription.INTEGER;
            d.payload = value.toString();
        }
        else if (M3Paths.Float.equals(type))
        {
            d.kind = NodeDescription.FLOAT;
            d.payload = (value instanceof BigDecimal) ? ((BigDecimal) value).toPlainString() : value.toString();
        }
        else if (M3Paths.Decimal.equals(type))
        {
            d.kind = NodeDescription.DECIMAL;
            d.payload = (value instanceof BigDecimal) ? ((BigDecimal) value).toPlainString() : value.toString();
        }
        else if (M3Paths.String.equals(type))
        {
            d.kind = NodeDescription.STRING;
            d.payload = value.toString();
        }
        else if (value instanceof PureDate)
        {
            describeDate(d, (PureDate) value);
        }
        else if (value instanceof PureStrictTime)
        {
            describeStrictTime(d, (PureStrictTime) value);
        }
        else if (M3Paths.Byte.equals(type))
        {
            d.kind = NodeDescription.BYTE;
        }
        else
        {
            throw PureRustEvaluationException.unsupported("a primitive of type " + type + " across the boundary");
        }
    }

    private static void describeDate(NodeDescription d, PureDate date)
    {
        if (LatestDate.isLatestDate(date))
        {
            d.kind = NodeDescription.LATEST_DATE;
            return;
        }
        d.kind = NodeDescription.DATE;
        d.year = date.getYear();
        d.precision = 0;
        if (date.hasMonth())
        {
            d.month = date.getMonth();
            d.precision = 1;
        }
        if (date.hasDay())
        {
            d.day = date.getDay();
            d.precision = 2;
        }
        if (date.hasHour())
        {
            d.hour = date.getHour();
            d.precision = 3;
        }
        if (date.hasMinute())
        {
            d.minute = date.getMinute();
            d.precision = 4;
        }
        if (date.hasSecond())
        {
            d.second = date.getSecond();
            d.precision = 5;
        }
        if (date.hasSubsecond())
        {
            d.subsecond = date.getSubsecond();
            d.precision = 6;
        }
    }

    private static void describeStrictTime(NodeDescription d, PureStrictTime time)
    {
        d.kind = NodeDescription.STRICT_TIME;
        d.hour = time.getHour();
        d.minute = time.getMinute();
        d.precision = 4;
        if (time.hasSecond())
        {
            d.second = time.getSecond();
            d.precision = 5;
        }
        if (time.hasSubsecond())
        {
            d.subsecond = time.getSubsecond();
            d.precision = 6;
        }
    }

    /**
     * The values of one property of a node held by id, each described.
     *
     * @param id  the node's id
     * @param key the property name
     * @return one description per value
     */
    public NodeDescription[] propertyValues(long id, String key)
    {
        CoreInstance node = this.nodes.get(id);
        ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(node, key, this.processorSupport);
        NodeDescription[] out = new NodeDescription[values.size()];
        for (int i = 0; i < out.length; i++)
        {
            out[i] = describe(values.get(i));
        }
        return out;
    }

    /**
     * The property names of a node, read when the Rust side first asks for
     * them.
     *
     * @param id the node's id
     * @return the names, in no particular order
     */
    public String[] keysOf(long id)
    {
        return this.host.propertyKeys(this.nodes.get(id)).toArray(new String[0]);
    }

    /**
     * The values of a variable a lambda captured from the scope it was
     * evaluated in.
     *
     * @param id   the lambda node's id
     * @param name the open variable's name
     * @return the values described; {@code null} when the node is not such a lambda, empty when it holds no
     * variable of that name
     */
    public NodeDescription[] capturedValue(long id, String name)
    {
        ListIterable<? extends CoreInstance> values = this.host.capturedValues(this.nodes.get(id), name);
        if (values == null)
        {
            return null;
        }
        NodeDescription[] out = new NodeDescription[values.size()];
        for (int i = 0; i < out.length; i++)
        {
            out[i] = describe(values.get(i));
        }
        return out;
    }

    /**
     * A date from its components, as the host represents it.
     *
     * @param year      the year
     * @param month     the month, read when {@code precision >= 1}
     * @param day       the day, read when {@code precision >= 2}
     * @param hour      the hour, read when {@code precision >= 3}
     * @param minute    the minute, read when {@code precision >= 4}
     * @param second    the second, read when {@code precision >= 5}
     * @param subsecond the subsecond digits, read when {@code precision >= 6}
     * @param precision 0 year, 1 month, 2 day, 3 hour, 4 minute, 5 second, 6 subsecond
     * @return the date
     */
    public CoreInstance newDate(int year, int month, int day, int hour, int minute, int second, String subsecond, int precision)
    {
        PureDate date;
        switch (precision)
        {
            case 0:
                date = DateFunctions.newPureDate(year);
                break;
            case 1:
                date = DateFunctions.newPureDate(year, month);
                break;
            case 2:
                date = DateFunctions.newPureDate(year, month, day);
                break;
            case 3:
                date = DateFunctions.newPureDate(year, month, day, hour);
                break;
            case 4:
                date = DateFunctions.newPureDate(year, month, day, hour, minute);
                break;
            case 5:
                date = DateFunctions.newPureDate(year, month, day, hour, minute, second);
                break;
            default:
                date = DateFunctions.newPureDate(year, month, day, hour, minute, second, subsecond);
                break;
        }
        return this.host.newDate(date);
    }

    /**
     * A strict time from its components, as the host represents it.
     *
     * @param hour      the hour
     * @param minute    the minute
     * @param second    the second, read when {@code precision >= 5}
     * @param subsecond the subsecond digits, read when {@code precision >= 6}
     * @param precision 4 minute, 5 second, 6 subsecond
     * @return the strict time
     */
    public CoreInstance newStrictTime(int hour, int minute, int second, String subsecond, int precision)
    {
        StringBuilder text = new StringBuilder(String.format("%02d:%02d", hour, minute));
        if (precision >= 5)
        {
            text.append(String.format(":%02d", second));
        }
        if ((precision >= 6) && (subsecond != null) && !subsecond.isEmpty())
        {
            text.append('.').append(subsecond);
        }
        return this.host.newStrictTime(StrictTimeFunctions.parsePureStrictTime(text.toString()));
    }

    /**
     * A packageable element by path.
     *
     * @param userPath the path, {@code ::}-separated
     * @return the element, or {@code null} when the graph has none
     */
    public CoreInstance element(String userPath)
    {
        return _Package.getByUserPath(userPath, this.processorSupport);
    }

    /**
     * An enum value by its enumeration's path and its name.
     *
     * @param enumPath the enumeration's path
     * @param member   the value's name
     * @return the enum value, or {@code null} when the graph has none
     */
    public CoreInstance enumValue(String enumPath, String member)
    {
        CoreInstance enumeration = _Package.getByUserPath(enumPath, this.processorSupport);
        return (enumeration == null) ? null : Enumeration.findEnum(enumeration, member);
    }

    /**
     * A node held by id.
     *
     * @param id the node's id
     * @return the node, or {@code null} once forgotten
     */
    public CoreInstance node(long id)
    {
        return this.nodes.get(id);
    }

    /**
     * A Java failure raised while serving the Rust side, as {@code {kind, text}}:
     * a {@link PureRustEvaluationException} keeps its kind and info, anything
     * else is an execution error described by its {@code toString()}.
     *
     * @param failure the failure
     * @return the kind's name and the text
     */
    public String[] failureOf(Throwable failure)
    {
        if (failure instanceof PureRustEvaluationException)
        {
            PureRustEvaluationException e = (PureRustEvaluationException) failure;
            return new String[]{e.getKind().name(), e.getInfo()};
        }
        return new String[]{PureRustEvaluationException.Kind.EXECUTION_ERROR.name(), String.valueOf(failure)};
    }
}
