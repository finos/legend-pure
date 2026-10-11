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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigDecimal;

/**
 * The host side of the C ABI: the shared {@link NodeBridge} plus what the FFM
 * transport adds. Inbound, a {@link NodeDescription} is written into a
 * {@code pure_description} segment the Rust side reads in place; outbound, the
 * {@code pure_leaf}s of a result are read into host objects through the engine's
 * factories, Rust objects as one {@link RustCoreInstance} per handle. The four
 * callbacks of the {@code pure_host_table} are upcall stubs bound to this
 * instance: each answers through {@code pure_reply_*} and returns a status, and
 * nothing thrown inside one ever reaches the native frame.
 */
final class FfmNodeBridge extends NodeBridge
{
    private final RustHeap owner;
    private final MutableMap<Long, RustCoreInstance> proxies = Maps.mutable.empty();

    FfmNodeBridge(RustEngineHost host, RustHeap owner)
    {
        super(host);
        this.owner = owner;
    }

    @Override
    public void clear()
    {
        super.clear();
        this.proxies.clear();
    }

    MutableList<Long> proxyHandles()
    {
        return Lists.mutable.withAll(this.proxies.keysView());
    }

    void forgetProxy(long handle)
    {
        this.proxies.remove(handle);
    }

    CoreInstance rustObject(long handle, String classifierPath)
    {
        return this.proxies.getIfAbsentPutWithKey(handle, h -> new RustCoreInstance(this.owner, h, classifierPath));
    }

    /**
     * The callback table a context is created over, with its four stubs; the
     * arena owns the stubs and must outlive the context.
     *
     * @param arena the evaluator's arena
     * @return a {@code pure_host_table} segment
     */
    MemorySegment hostTable(Arena arena)
    {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType values = MethodType.methodType(int.class, MemorySegment.class, long.class, MemorySegment.class, long.class, MemorySegment.class);
        MethodType keys = MethodType.methodType(int.class, MemorySegment.class, long.class, MemorySegment.class);
        MethodType forget = MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class, long.class);
        MemorySegment table = arena.allocate(PureAbi.HOST_TABLE);
        try
        {
            table.set(ValueLayout.ADDRESS, PureAbi.HOST_TABLE_HOST, MemorySegment.NULL);
            table.set(ValueLayout.ADDRESS, PureAbi.HOST_TABLE_PROPERTY_VALUES, stub(lookup.findVirtual(FfmNodeBridge.class, "answerPropertyValues", values), PureAbi.PROPERTY_VALUES_FN, arena));
            table.set(ValueLayout.ADDRESS, PureAbi.HOST_TABLE_CAPTURED_VALUES, stub(lookup.findVirtual(FfmNodeBridge.class, "answerCapturedValues", values), PureAbi.PROPERTY_VALUES_FN, arena));
            table.set(ValueLayout.ADDRESS, PureAbi.HOST_TABLE_KEYS, stub(lookup.findVirtual(FfmNodeBridge.class, "answerKeys", keys), PureAbi.KEYS_FN, arena));
            table.set(ValueLayout.ADDRESS, PureAbi.HOST_TABLE_FORGET, stub(lookup.findVirtual(FfmNodeBridge.class, "acceptForget", forget), PureAbi.FORGET_FN, arena));
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("the callback targets of " + FfmNodeBridge.class.getName(), e);
        }
        return table;
    }

    private MemorySegment stub(MethodHandle target, FunctionDescriptor descriptor, Arena arena)
    {
        return PureAbi.LINKER.upcallStub(target.bindTo(this), descriptor, arena);
    }

    /**
     * One node described into a fresh {@code pure_description}.
     *
     * @param node      the node
     * @param allocator where the segment and its strings live for the call
     * @return the segment
     */
    MemorySegment describeInto(CoreInstance node, SegmentAllocator allocator)
    {
        MemorySegment target = allocator.allocate(PureAbi.DESCRIPTION);
        write(target, describe(node), allocator);
        return target;
    }

    /**
     * Nodes described into a contiguous array of {@code pure_description}s.
     *
     * @param nodes     the nodes
     * @param allocator where the array and its strings live for the call
     * @return the array, {@code NULL} when there are no nodes
     */
    MemorySegment describeAll(CoreInstance[] nodes, SegmentAllocator allocator)
    {
        if (nodes.length == 0)
        {
            return MemorySegment.NULL;
        }
        MemorySegment items = allocator.allocate(PureAbi.DESCRIPTION, nodes.length);
        for (int i = 0; i < nodes.length; i++)
        {
            write(items.asSlice(i * PureAbi.DESCRIPTION.byteSize(), PureAbi.DESCRIPTION), describe(nodes[i]), allocator);
        }
        return items;
    }

    private static MemorySegment describedAll(NodeDescription[] described, SegmentAllocator allocator)
    {
        if (described.length == 0)
        {
            return MemorySegment.NULL;
        }
        MemorySegment items = allocator.allocate(PureAbi.DESCRIPTION, described.length);
        for (int i = 0; i < described.length; i++)
        {
            write(items.asSlice(i * PureAbi.DESCRIPTION.byteSize(), PureAbi.DESCRIPTION), described[i], allocator);
        }
        return items;
    }

    /**
     * Value slots, one per argument or property, each over its described values.
     *
     * @param slots     the values of each slot
     * @param allocator where everything lives for the call
     * @return the array of {@code pure_slot}s, {@code NULL} when there are none
     */
    MemorySegment slots(CoreInstance[][] slots, SegmentAllocator allocator)
    {
        if (slots.length == 0)
        {
            return MemorySegment.NULL;
        }
        MemorySegment array = allocator.allocate(PureAbi.SLOT, slots.length);
        for (int i = 0; i < slots.length; i++)
        {
            writeSlot(array.asSlice(i * PureAbi.SLOT.byteSize(), PureAbi.SLOT), slots[i], allocator);
        }
        return array;
    }

    /**
     * Properties of an object to allocate, each a name and a slot.
     *
     * @param names     the property names
     * @param values    the values of each property, parallel to {@code names}
     * @param allocator where everything lives for the call
     * @return the array of {@code pure_property}s, {@code NULL} when there are none
     */
    MemorySegment properties(String[] names, CoreInstance[][] values, SegmentAllocator allocator)
    {
        if (names.length == 0)
        {
            return MemorySegment.NULL;
        }
        MemorySegment array = allocator.allocate(PureAbi.PROPERTY, names.length);
        for (int i = 0; i < names.length; i++)
        {
            MemorySegment property = array.asSlice(i * PureAbi.PROPERTY.byteSize(), PureAbi.PROPERTY);
            PureAbi.writeString(property, PureAbi.PROPERTY_NAME, names[i], allocator);
            writeSlot(property.asSlice(PureAbi.PROPERTY_VALUES, PureAbi.SLOT), values[i], allocator);
        }
        return array;
    }

    private void writeSlot(MemorySegment slot, CoreInstance[] values, SegmentAllocator allocator)
    {
        slot.set(ValueLayout.ADDRESS, PureAbi.SLOT_ITEMS, describeAll(values, allocator));
        slot.set(ValueLayout.JAVA_LONG, PureAbi.SLOT_COUNT, values.length);
    }

    private static void write(MemorySegment target, NodeDescription d, SegmentAllocator allocator)
    {
        target.set(ValueLayout.JAVA_LONG, PureAbi.DESCRIPTION_ID, d.id);
        target.set(ValueLayout.JAVA_LONG, PureAbi.DESCRIPTION_DEFINITION, d.definition);
        PureAbi.writeString(target, PureAbi.DESCRIPTION_CLASSIFIER, d.classifier, allocator);
        PureAbi.writeString(target, PureAbi.DESCRIPTION_NAME, d.name, allocator);
        PureAbi.writeString(target, PureAbi.DESCRIPTION_USER_PATH, d.userPath, allocator);
        PureAbi.writeString(target, PureAbi.DESCRIPTION_PAYLOAD, d.payload, allocator);
        PureAbi.writeString(target, PureAbi.DESCRIPTION_SUBSECOND, d.subsecond, allocator);
        PureAbi.writeString(target, PureAbi.DESCRIPTION_SOURCE_ID, d.sourceId, allocator);
        target.set(ValueLayout.JAVA_INT, PureAbi.DESCRIPTION_KIND, d.kind);
        target.set(ValueLayout.JAVA_INT, PureAbi.DESCRIPTION_YEAR, d.year);
        target.set(ValueLayout.JAVA_INT, PureAbi.DESCRIPTION_MONTH, d.month);
        target.set(ValueLayout.JAVA_INT, PureAbi.DESCRIPTION_DAY, d.day);
        target.set(ValueLayout.JAVA_INT, PureAbi.DESCRIPTION_HOUR, d.hour);
        target.set(ValueLayout.JAVA_INT, PureAbi.DESCRIPTION_MINUTE, d.minute);
        target.set(ValueLayout.JAVA_INT, PureAbi.DESCRIPTION_SECOND, d.second);
        target.set(ValueLayout.JAVA_INT, PureAbi.DESCRIPTION_PRECISION, d.precision);
        target.set(ValueLayout.JAVA_INT, PureAbi.DESCRIPTION_LINE, d.line);
        target.set(ValueLayout.JAVA_INT, PureAbi.DESCRIPTION_COLUMN, d.column);
        target.set(ValueLayout.JAVA_INT, PureAbi.DESCRIPTION_END_LINE, d.endLine);
        target.set(ValueLayout.JAVA_INT, PureAbi.DESCRIPTION_END_COLUMN, d.endColumn);
    }

    /**
     * One {@code pure_leaf}, copied out of a Rust-owned result.
     */
    record Leaf(int kind, long integer, double real, String text, String name,
                int precision, int year, int month, int day, int hour, int minute, int second, Leaf[] entries)
    {
        static Leaf read(MemorySegment leaf)
        {
            long entryCount = leaf.get(ValueLayout.JAVA_LONG, PureAbi.LEAF_ENTRY_COUNT);
            Leaf[] entries = entryCount == 0 ? new Leaf[0] : readAll(leaf.get(ValueLayout.ADDRESS, PureAbi.LEAF_ENTRIES), 2 * entryCount);
            return new Leaf(
                    leaf.get(ValueLayout.JAVA_INT, PureAbi.LEAF_KIND),
                    leaf.get(ValueLayout.JAVA_LONG, PureAbi.LEAF_INTEGER),
                    leaf.get(ValueLayout.JAVA_DOUBLE, PureAbi.LEAF_REAL),
                    PureAbi.readString(leaf, PureAbi.LEAF_TEXT),
                    PureAbi.readString(leaf, PureAbi.LEAF_NAME),
                    leaf.get(ValueLayout.JAVA_INT, PureAbi.LEAF_PRECISION),
                    leaf.get(ValueLayout.JAVA_INT, PureAbi.LEAF_YEAR),
                    leaf.get(ValueLayout.JAVA_INT, PureAbi.LEAF_MONTH),
                    leaf.get(ValueLayout.JAVA_INT, PureAbi.LEAF_DAY),
                    leaf.get(ValueLayout.JAVA_INT, PureAbi.LEAF_HOUR),
                    leaf.get(ValueLayout.JAVA_INT, PureAbi.LEAF_MINUTE),
                    leaf.get(ValueLayout.JAVA_INT, PureAbi.LEAF_SECOND),
                    entries);
        }

        static Leaf[] readAll(MemorySegment leaves, long count)
        {
            Leaf[] out = new Leaf[Math.toIntExact(count)];
            MemorySegment array = leaves.reinterpret(count * PureAbi.LEAF.byteSize());
            for (int i = 0; i < out.length; i++)
            {
                out[i] = read(array.asSlice(i * PureAbi.LEAF.byteSize(), PureAbi.LEAF));
            }
            return out;
        }
    }

    /**
     * The leaves of a result, each as the host engine represents it.
     *
     * @param result a {@code pure_result *} the Rust side returned, not yet freed
     * @return one host object per leaf
     */
    CoreInstance[] materialize(MemorySegment result)
    {
        MemorySegment view = result.reinterpret(PureAbi.RESULT.byteSize());
        long count = view.get(ValueLayout.JAVA_LONG, PureAbi.RESULT_COUNT);
        if (count == 0)
        {
            return new CoreInstance[0];
        }
        Leaf[] leaves = Leaf.readAll(view.get(ValueLayout.ADDRESS, PureAbi.RESULT_LEAVES), count);
        CoreInstance[] out = new CoreInstance[leaves.length];
        for (int i = 0; i < out.length; i++)
        {
            out[i] = toHost(leaves[i]);
        }
        return out;
    }

    /**
     * The string leaves of a result.
     *
     * @param result a {@code pure_result *} of STRING leaves, not yet freed
     * @return the strings
     */
    static String[] strings(MemorySegment result)
    {
        MemorySegment view = result.reinterpret(PureAbi.RESULT.byteSize());
        long count = view.get(ValueLayout.JAVA_LONG, PureAbi.RESULT_COUNT);
        if (count == 0)
        {
            return new String[0];
        }
        Leaf[] leaves = Leaf.readAll(view.get(ValueLayout.ADDRESS, PureAbi.RESULT_LEAVES), count);
        String[] out = new String[leaves.length];
        for (int i = 0; i < out.length; i++)
        {
            out[i] = leaves[i].text();
        }
        return out;
    }

    private CoreInstance toHost(Leaf leaf)
    {
        return switch (leaf.kind())
        {
            case NodeDescription.BOOLEAN -> host().newBoolean(leaf.integer() != 0);
            case NodeDescription.INTEGER -> host().newInteger(leaf.integer());
            case NodeDescription.FLOAT -> host().newFloat(floatText(leaf.real()));
            case NodeDescription.DECIMAL -> host().newDecimal(leaf.text());
            case NodeDescription.STRING -> host().newString(leaf.text());
            case NodeDescription.LATEST_DATE -> host().newLatestDate();
            case NodeDescription.DATE -> newDate(leaf.year(), leaf.month(), leaf.day(), leaf.hour(), leaf.minute(), leaf.second(), leaf.text(), leaf.precision());
            case NodeDescription.STRICT_TIME -> newStrictTime(leaf.hour(), leaf.minute(), leaf.second(), leaf.text(), leaf.precision());
            case NodeDescription.PACKAGEABLE_ELEMENT -> elementOf(leaf.text());
            case NodeDescription.ENUM_VALUE -> enumValueOf(leaf.text(), leaf.name());
            case NodeDescription.NODE -> nodeOf(leaf.integer());
            case NodeDescription.RUST_OBJECT -> rustObject(leaf.integer(), leaf.text());
            case PureAbi.KIND_MAP -> map(leaf.entries());
            default -> throw new PureRustEvaluationException("a leaf of kind " + leaf.kind() + " crossing out of the Rust evaluator");
        };
    }

    /**
     * A finite double as Rust's {@code Display} prints it: the shortest digits that
     * round-trip, in plain notation, with no trailing zero fraction.
     */
    private static String floatText(double value)
    {
        return Double.isFinite(value) ? new BigDecimal(Double.toString(value)).stripTrailingZeros().toPlainString() : Double.toString(value);
    }

    private CoreInstance elementOf(String userPath)
    {
        CoreInstance element = element(userPath);
        if (element == null)
        {
            throw new PureRustEvaluationException("element " + userPath + " is not in the Java graph");
        }
        return element;
    }

    private CoreInstance enumValueOf(String enumPath, String member)
    {
        CoreInstance value = enumValue(enumPath, member);
        if (value == null)
        {
            throw new PureRustEvaluationException("enum value " + enumPath + "." + member + " is not in the Java graph");
        }
        return value;
    }

    private CoreInstance nodeOf(long id)
    {
        CoreInstance node = node(id);
        if (node == null)
        {
            throw new PureRustEvaluationException("host node " + id + " was forgotten before it crossed back");
        }
        return node;
    }

    private CoreInstance map(Leaf[] entries)
    {
        CoreInstance[] keys = new CoreInstance[entries.length / 2];
        CoreInstance[] values = new CoreInstance[keys.length];
        for (int i = 0; i < keys.length; i++)
        {
            keys[i] = toHost(entries[2 * i]);
            values[i] = toHost(entries[2 * i + 1]);
        }
        return host().newMap(keys, values);
    }

    int answerPropertyValues(MemorySegment host, long id, MemorySegment name, long nameLen, MemorySegment reply)
    {
        try (Arena push = Arena.ofConfined())
        {
            if (node(id) == null)
            {
                return PureAbi.STATUS_ABSENT;
            }
            NodeDescription[] values = propertyValues(id, PureAbi.readText(name, nameLen));
            return PureAbi.replyDescriptions(reply, describedAll(values, push), values.length);
        }
        catch (Throwable t)
        {
            return fail(reply, t);
        }
    }

    int answerCapturedValues(MemorySegment host, long id, MemorySegment name, long nameLen, MemorySegment reply)
    {
        try (Arena push = Arena.ofConfined())
        {
            if (node(id) == null)
            {
                return PureAbi.STATUS_ABSENT;
            }
            NodeDescription[] values = capturedValue(id, PureAbi.readText(name, nameLen));
            if (values == null)
            {
                return PureAbi.STATUS_ABSENT;
            }
            return PureAbi.replyDescriptions(reply, describedAll(values, push), values.length);
        }
        catch (Throwable t)
        {
            return fail(reply, t);
        }
    }

    int answerKeys(MemorySegment host, long id, MemorySegment reply)
    {
        try (Arena push = Arena.ofConfined())
        {
            if (node(id) == null)
            {
                return PureAbi.STATUS_ABSENT;
            }
            String[] keys = keysOf(id);
            if (keys.length == 0)
            {
                return PureAbi.STATUS_OK;
            }
            MemorySegment names = push.allocate(PureAbi.STRING, keys.length);
            for (int i = 0; i < keys.length; i++)
            {
                PureAbi.writeString(names, i * PureAbi.STRING.byteSize(), keys[i], push);
            }
            return PureAbi.replyNames(reply, names, keys.length);
        }
        catch (Throwable t)
        {
            return fail(reply, t);
        }
    }

    void acceptForget(MemorySegment host, MemorySegment ids, long count)
    {
        try
        {
            if (count > 0)
            {
                forget(ids.reinterpret(count * ValueLayout.JAVA_LONG.byteSize()).toArray(ValueLayout.JAVA_LONG));
            }
        }
        catch (Throwable ignored)
        {
        }
    }

    private int fail(MemorySegment reply, Throwable failure)
    {
        try (Arena push = Arena.ofConfined())
        {
            String[] described = failureOf(failure);
            PureAbi.Text message = PureAbi.Text.of(described[1], push);
            PureAbi.replyFail(reply, failureKindCode(described[0]), message.ptr(), message.len());
        }
        catch (Throwable ignored)
        {
        }
        return PureAbi.STATUS_FAILED;
    }

    private static int failureKindCode(String kindName)
    {
        return switch (PureRustEvaluationException.Kind.valueOf(kindName))
        {
            case EXECUTION_ERROR -> PureAbi.FAILURE_EXECUTION_ERROR;
            case ASSERTION_FAILED -> PureAbi.FAILURE_ASSERTION_FAILED;
            case CONSTRAINT_VIOLATION -> PureAbi.FAILURE_CONSTRAINT_VIOLATION;
            case FUNCTION_NOT_FOUND -> PureAbi.FAILURE_FUNCTION_NOT_FOUND;
            case UNSUPPORTED -> PureAbi.FAILURE_UNSUPPORTED;
            case NOT_IMPLEMENTED -> PureAbi.FAILURE_NOT_IMPLEMENTED;
        };
    }
}
