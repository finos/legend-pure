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

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/**
 * The C ABI of {@code libpure_rust_ffi} as {@code pure_rust.h} declares it: the
 * constants, the struct layouts with their sizes asserted once, the field
 * offsets, the seventeen downcalls resolved once, and the descriptors of the
 * four callbacks. The library is loaded when this class initialises and its
 * {@code pure_abi_version()} must equal {@link #ABI_VERSION}.
 */
final class PureAbi
{
    /**
     * {@code PURE_ABI_VERSION}: the version of the header this client is written from.
     */
    static final int ABI_VERSION = 1;

    /**
     * System property naming the directory that holds the cdylib; set by surefire.
     */
    static final String LIBDIR_PROPERTY = "legend.pure.rust.libdir";

    static final int STATUS_OK = 0;
    static final int STATUS_FAILED = 1;
    static final int STATUS_ABSENT = 2;

    /**
     * {@code PURE_KIND_MAP}, the one leaf-only kind beside {@link NodeDescription}'s.
     */
    static final int KIND_MAP = 14;

    static final int FAILURE_EXECUTION_ERROR = 0;
    static final int FAILURE_ASSERTION_FAILED = 1;
    static final int FAILURE_CONSTRAINT_VIOLATION = 2;
    static final int FAILURE_FUNCTION_NOT_FOUND = 3;
    static final int FAILURE_UNSUPPORTED = 4;
    static final int FAILURE_NOT_IMPLEMENTED = 5;
    static final int FAILURE_PANIC = 6;

    static final StructLayout STRING = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("ptr"),
            ValueLayout.JAVA_LONG.withName("len"));

    static final StructLayout DESCRIPTION = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG.withName("id"),
            ValueLayout.JAVA_LONG.withName("definition"),
            ValueLayout.JAVA_LONG.withName("integer"),
            ValueLayout.JAVA_DOUBLE.withName("real"),
            STRING.withName("classifier"),
            STRING.withName("name"),
            STRING.withName("user_path"),
            STRING.withName("payload"),
            STRING.withName("subsecond"),
            STRING.withName("source_id"),
            ValueLayout.JAVA_INT.withName("kind"),
            ValueLayout.JAVA_INT.withName("year"),
            ValueLayout.JAVA_INT.withName("month"),
            ValueLayout.JAVA_INT.withName("day"),
            ValueLayout.JAVA_INT.withName("hour"),
            ValueLayout.JAVA_INT.withName("minute"),
            ValueLayout.JAVA_INT.withName("second"),
            ValueLayout.JAVA_INT.withName("precision"),
            ValueLayout.JAVA_INT.withName("line"),
            ValueLayout.JAVA_INT.withName("column"),
            ValueLayout.JAVA_INT.withName("end_line"),
            ValueLayout.JAVA_INT.withName("end_column"));

    static final StructLayout SLOT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("items"),
            ValueLayout.JAVA_LONG.withName("count"));

    static final StructLayout PROPERTY = MemoryLayout.structLayout(
            STRING.withName("name"),
            SLOT.withName("values"));

    static final StructLayout LEAF = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG.withName("integer"),
            ValueLayout.JAVA_DOUBLE.withName("real"),
            STRING.withName("text"),
            STRING.withName("name"),
            ValueLayout.ADDRESS.withName("entries"),
            ValueLayout.JAVA_LONG.withName("entry_count"),
            ValueLayout.JAVA_INT.withName("kind"),
            ValueLayout.JAVA_INT.withName("precision"),
            ValueLayout.JAVA_INT.withName("year"),
            ValueLayout.JAVA_INT.withName("month"),
            ValueLayout.JAVA_INT.withName("day"),
            ValueLayout.JAVA_INT.withName("hour"),
            ValueLayout.JAVA_INT.withName("minute"),
            ValueLayout.JAVA_INT.withName("second"));

    static final StructLayout RESULT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("leaves"),
            ValueLayout.JAVA_LONG.withName("count"));

    static final StructLayout SPAN = MemoryLayout.structLayout(
            STRING.withName("source"),
            ValueLayout.JAVA_INT.withName("start_line"),
            ValueLayout.JAVA_INT.withName("start_column"),
            ValueLayout.JAVA_INT.withName("end_line"),
            ValueLayout.JAVA_INT.withName("end_column"));

    static final StructLayout FRAME = MemoryLayout.structLayout(
            STRING.withName("function"),
            SPAN.withName("span"));

    static final StructLayout FAILURE = MemoryLayout.structLayout(
            STRING.withName("message"),
            STRING.withName("info"),
            SPAN.withName("source"),
            STRING.withName("constraint_id"),
            STRING.withName("constraint_kind"),
            STRING.withName("owner"),
            ValueLayout.ADDRESS.withName("call_stack"),
            ValueLayout.JAVA_LONG.withName("call_stack_len"),
            ValueLayout.JAVA_INT.withName("kind"),
            MemoryLayout.paddingLayout(4));

    static final StructLayout TIMINGS = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG.withName("lower_ns"),
            ValueLayout.JAVA_LONG.withName("run_ns"),
            ValueLayout.JAVA_LONG.withName("run_host_reads"),
            ValueLayout.JAVA_LONG.withName("lower_host_reads"));

    static final StructLayout HOST_TABLE = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("host"),
            ValueLayout.ADDRESS.withName("property_values"),
            ValueLayout.ADDRESS.withName("captured_values"),
            ValueLayout.ADDRESS.withName("keys"),
            ValueLayout.ADDRESS.withName("forget"));

    static final long STRING_PTR = offset(STRING, "ptr");
    static final long STRING_LEN = offset(STRING, "len");

    static final long DESCRIPTION_ID = offset(DESCRIPTION, "id");
    static final long DESCRIPTION_DEFINITION = offset(DESCRIPTION, "definition");
    static final long DESCRIPTION_INTEGER = offset(DESCRIPTION, "integer");
    static final long DESCRIPTION_REAL = offset(DESCRIPTION, "real");
    static final long DESCRIPTION_CLASSIFIER = offset(DESCRIPTION, "classifier");
    static final long DESCRIPTION_NAME = offset(DESCRIPTION, "name");
    static final long DESCRIPTION_USER_PATH = offset(DESCRIPTION, "user_path");
    static final long DESCRIPTION_PAYLOAD = offset(DESCRIPTION, "payload");
    static final long DESCRIPTION_SUBSECOND = offset(DESCRIPTION, "subsecond");
    static final long DESCRIPTION_SOURCE_ID = offset(DESCRIPTION, "source_id");
    static final long DESCRIPTION_KIND = offset(DESCRIPTION, "kind");
    static final long DESCRIPTION_YEAR = offset(DESCRIPTION, "year");
    static final long DESCRIPTION_MONTH = offset(DESCRIPTION, "month");
    static final long DESCRIPTION_DAY = offset(DESCRIPTION, "day");
    static final long DESCRIPTION_HOUR = offset(DESCRIPTION, "hour");
    static final long DESCRIPTION_MINUTE = offset(DESCRIPTION, "minute");
    static final long DESCRIPTION_SECOND = offset(DESCRIPTION, "second");
    static final long DESCRIPTION_PRECISION = offset(DESCRIPTION, "precision");
    static final long DESCRIPTION_LINE = offset(DESCRIPTION, "line");
    static final long DESCRIPTION_COLUMN = offset(DESCRIPTION, "column");
    static final long DESCRIPTION_END_LINE = offset(DESCRIPTION, "end_line");
    static final long DESCRIPTION_END_COLUMN = offset(DESCRIPTION, "end_column");

    static final long SLOT_ITEMS = offset(SLOT, "items");
    static final long SLOT_COUNT = offset(SLOT, "count");

    static final long PROPERTY_NAME = offset(PROPERTY, "name");
    static final long PROPERTY_VALUES = offset(PROPERTY, "values");

    static final long LEAF_INTEGER = offset(LEAF, "integer");
    static final long LEAF_REAL = offset(LEAF, "real");
    static final long LEAF_TEXT = offset(LEAF, "text");
    static final long LEAF_NAME = offset(LEAF, "name");
    static final long LEAF_ENTRIES = offset(LEAF, "entries");
    static final long LEAF_ENTRY_COUNT = offset(LEAF, "entry_count");
    static final long LEAF_KIND = offset(LEAF, "kind");
    static final long LEAF_PRECISION = offset(LEAF, "precision");
    static final long LEAF_YEAR = offset(LEAF, "year");
    static final long LEAF_MONTH = offset(LEAF, "month");
    static final long LEAF_DAY = offset(LEAF, "day");
    static final long LEAF_HOUR = offset(LEAF, "hour");
    static final long LEAF_MINUTE = offset(LEAF, "minute");
    static final long LEAF_SECOND = offset(LEAF, "second");

    static final long RESULT_LEAVES = offset(RESULT, "leaves");
    static final long RESULT_COUNT = offset(RESULT, "count");

    static final long SPAN_SOURCE = offset(SPAN, "source");
    static final long SPAN_START_LINE = offset(SPAN, "start_line");
    static final long SPAN_START_COLUMN = offset(SPAN, "start_column");
    static final long SPAN_END_LINE = offset(SPAN, "end_line");
    static final long SPAN_END_COLUMN = offset(SPAN, "end_column");

    static final long FRAME_FUNCTION = offset(FRAME, "function");
    static final long FRAME_SPAN = offset(FRAME, "span");

    static final long FAILURE_MESSAGE = offset(FAILURE, "message");
    static final long FAILURE_INFO = offset(FAILURE, "info");
    static final long FAILURE_SOURCE = offset(FAILURE, "source");
    static final long FAILURE_CONSTRAINT_ID = offset(FAILURE, "constraint_id");
    static final long FAILURE_CONSTRAINT_KIND = offset(FAILURE, "constraint_kind");
    static final long FAILURE_OWNER = offset(FAILURE, "owner");
    static final long FAILURE_CALL_STACK = offset(FAILURE, "call_stack");
    static final long FAILURE_CALL_STACK_LEN = offset(FAILURE, "call_stack_len");
    static final long FAILURE_KIND = offset(FAILURE, "kind");

    static final long TIMINGS_LOWER_NS = offset(TIMINGS, "lower_ns");
    static final long TIMINGS_RUN_NS = offset(TIMINGS, "run_ns");
    static final long TIMINGS_RUN_HOST_READS = offset(TIMINGS, "run_host_reads");
    static final long TIMINGS_LOWER_HOST_READS = offset(TIMINGS, "lower_host_reads");

    static final long HOST_TABLE_HOST = offset(HOST_TABLE, "host");
    static final long HOST_TABLE_PROPERTY_VALUES = offset(HOST_TABLE, "property_values");
    static final long HOST_TABLE_CAPTURED_VALUES = offset(HOST_TABLE, "captured_values");
    static final long HOST_TABLE_KEYS = offset(HOST_TABLE, "keys");
    static final long HOST_TABLE_FORGET = offset(HOST_TABLE, "forget");

    /**
     * {@code pure_property_values_fn} and {@code pure_captured_values_fn}.
     */
    static final FunctionDescriptor PROPERTY_VALUES_FN = FunctionDescriptor.of(ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS);

    /**
     * {@code pure_keys_fn}.
     */
    static final FunctionDescriptor KEYS_FN = FunctionDescriptor.of(ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS);

    /**
     * {@code pure_forget_fn}.
     */
    static final FunctionDescriptor FORGET_FN = FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG);

    static final Linker LINKER = Linker.nativeLinker();

    private static final MethodHandle ABI_VERSION_FN;
    private static final MethodHandle CONTEXT_NEW;
    private static final MethodHandle CONTEXT_FREE;
    private static final MethodHandle START;
    private static final MethodHandle RESULT_FREE;
    private static final MethodHandle OBJECT_PROPERTY_VALUES;
    private static final MethodHandle OBJECT_PROPERTY_NAMES;
    private static final MethodHandle OBJECT_CLASSIFIER;
    private static final MethodHandle TEST_OBJECT_NEW;
    private static final MethodHandle OBJECT_RELEASE;
    private static final MethodHandle LIVE_HANDLE_COUNT;
    private static final MethodHandle TEST_CROSSING;
    private static final MethodHandle LAST_TIMINGS;
    private static final MethodHandle LAST_FAILURE;
    private static final MethodHandle REPLY_DESCRIPTIONS;
    private static final MethodHandle REPLY_NAMES;
    private static final MethodHandle REPLY_FAIL;

    static
    {
        assertSize(STRING, 16);
        assertSize(DESCRIPTION, 176);
        assertSize(SLOT, 16);
        assertSize(PROPERTY, 32);
        assertSize(LEAF, 96);
        assertSize(RESULT, 16);
        assertSize(SPAN, 32);
        assertSize(FRAME, 48);
        assertSize(FAILURE, 136);
        assertSize(TIMINGS, 32);
        assertSize(HOST_TABLE, 40);

        String dir = System.getProperty(LIBDIR_PROPERTY);
        if ((dir == null) || dir.isEmpty())
        {
            System.loadLibrary("pure_rust_ffi");
        }
        else
        {
            System.load(Paths.get(dir, System.mapLibraryName("pure_rust_ffi")).toAbsolutePath().toString());
        }
        SymbolLookup lookup = SymbolLookup.loaderLookup();

        ABI_VERSION_FN = downcall(lookup, "pure_abi_version", FunctionDescriptor.of(ValueLayout.JAVA_INT));
        CONTEXT_NEW = downcall(lookup, "pure_context_new", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        CONTEXT_FREE = downcall(lookup, "pure_context_free", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        START = downcall(lookup, "pure_start", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        RESULT_FREE = downcall(lookup, "pure_result_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        OBJECT_PROPERTY_VALUES = downcall(lookup, "pure_object_property_values", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        OBJECT_PROPERTY_NAMES = downcall(lookup, "pure_object_property_names", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        OBJECT_CLASSIFIER = downcall(lookup, "pure_object_classifier", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        TEST_OBJECT_NEW = downcall(lookup, "pure_test_object_new", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        OBJECT_RELEASE = downcall(lookup, "pure_object_release", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        LIVE_HANDLE_COUNT = downcall(lookup, "pure_live_handle_count", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        TEST_CROSSING = downcall(lookup, "pure_test_crossing", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        LAST_TIMINGS = downcall(lookup, "pure_last_timings", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        LAST_FAILURE = downcall(lookup, "pure_last_failure", FunctionDescriptor.of(ValueLayout.ADDRESS));
        REPLY_DESCRIPTIONS = downcall(lookup, "pure_reply_descriptions", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        REPLY_NAMES = downcall(lookup, "pure_reply_names", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        REPLY_FAIL = downcall(lookup, "pure_reply_fail", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

        int implemented = abiVersion();
        if (implemented != ABI_VERSION)
        {
            throw new IllegalStateException("libpure_rust_ffi implements ABI version " + implemented + "; this client is written from version " + ABI_VERSION);
        }
    }

    private PureAbi()
    {
    }

    private static long offset(StructLayout layout, String field)
    {
        return layout.byteOffset(PathElement.groupElement(field));
    }

    private static void assertSize(StructLayout layout, long expected)
    {
        if (layout.byteSize() != expected)
        {
            throw new IllegalStateException("layout " + layout + " is " + layout.byteSize() + " bytes; the header says " + expected);
        }
    }

    private static MethodHandle downcall(SymbolLookup lookup, String symbol, FunctionDescriptor descriptor)
    {
        MemorySegment address = lookup.find(symbol).orElseThrow(() -> new IllegalStateException("libpure_rust_ffi exports no " + symbol));
        return LINKER.downcallHandle(address, descriptor);
    }

    private static Error linkage(String symbol, Throwable cause)
    {
        return new LinkageError("calling " + symbol, cause);
    }

    static int abiVersion()
    {
        try
        {
            return (int) ABI_VERSION_FN.invokeExact();
        }
        catch (Throwable t)
        {
            throw linkage("pure_abi_version", t);
        }
    }

    static int contextNew(MemorySegment table, MemorySegment out)
    {
        try
        {
            return (int) CONTEXT_NEW.invokeExact(table, out);
        }
        catch (Throwable t)
        {
            throw linkage("pure_context_new", t);
        }
    }

    static int contextFree(MemorySegment context)
    {
        try
        {
            return (int) CONTEXT_FREE.invokeExact(context);
        }
        catch (Throwable t)
        {
            throw linkage("pure_context_free", t);
        }
    }

    static int start(MemorySegment context, MemorySegment function, MemorySegment args, long argc, MemorySegment out)
    {
        try
        {
            return (int) START.invokeExact(context, function, args, argc, out);
        }
        catch (Throwable t)
        {
            throw linkage("pure_start", t);
        }
    }

    static void resultFree(MemorySegment result)
    {
        try
        {
            RESULT_FREE.invokeExact(result);
        }
        catch (Throwable t)
        {
            throw linkage("pure_result_free", t);
        }
    }

    static int objectPropertyValues(MemorySegment context, long handle, MemorySegment name, long nameLen, MemorySegment out)
    {
        try
        {
            return (int) OBJECT_PROPERTY_VALUES.invokeExact(context, handle, name, nameLen, out);
        }
        catch (Throwable t)
        {
            throw linkage("pure_object_property_values", t);
        }
    }

    static int objectPropertyNames(MemorySegment context, long handle, MemorySegment out)
    {
        try
        {
            return (int) OBJECT_PROPERTY_NAMES.invokeExact(context, handle, out);
        }
        catch (Throwable t)
        {
            throw linkage("pure_object_property_names", t);
        }
    }

    static int objectClassifier(MemorySegment context, long handle, MemorySegment out)
    {
        try
        {
            return (int) OBJECT_CLASSIFIER.invokeExact(context, handle, out);
        }
        catch (Throwable t)
        {
            throw linkage("pure_object_classifier", t);
        }
    }

    /**
     * {@code pure_test_object_new}: test support, outside the ABI's contract.
     */
    static int testObjectNew(MemorySegment context, MemorySegment classifier, long classifierLen, MemorySegment properties, long count, MemorySegment outHandle)
    {
        try
        {
            return (int) TEST_OBJECT_NEW.invokeExact(context, classifier, classifierLen, properties, count, outHandle);
        }
        catch (Throwable t)
        {
            throw linkage("pure_test_object_new", t);
        }
    }

    static void objectRelease(MemorySegment context, long handle)
    {
        try
        {
            OBJECT_RELEASE.invokeExact(context, handle);
        }
        catch (Throwable t)
        {
            throw linkage("pure_object_release", t);
        }
    }

    static long liveHandleCount(MemorySegment context)
    {
        try
        {
            return (long) LIVE_HANDLE_COUNT.invokeExact(context);
        }
        catch (Throwable t)
        {
            throw linkage("pure_live_handle_count", t);
        }
    }

    /**
     * {@code pure_test_crossing}: test support, outside the ABI's contract.
     */
    static int testCrossing(MemorySegment context, MemorySegment node, MemorySegment out)
    {
        try
        {
            return (int) TEST_CROSSING.invokeExact(context, node, out);
        }
        catch (Throwable t)
        {
            throw linkage("pure_test_crossing", t);
        }
    }

    static void lastTimings(MemorySegment out)
    {
        try
        {
            LAST_TIMINGS.invokeExact(out);
        }
        catch (Throwable t)
        {
            throw linkage("pure_last_timings", t);
        }
    }

    /**
     * {@code pure_last_failure()}: the failure of the last call on this thread, resized to
     * its layout, or {@code null} when that call succeeded.
     */
    static MemorySegment lastFailure()
    {
        MemorySegment failure;
        try
        {
            failure = (MemorySegment) LAST_FAILURE.invokeExact();
        }
        catch (Throwable t)
        {
            throw linkage("pure_last_failure", t);
        }
        return failure.equals(MemorySegment.NULL) ? null : failure.reinterpret(FAILURE.byteSize());
    }

    static int replyDescriptions(MemorySegment reply, MemorySegment items, long count)
    {
        try
        {
            return (int) REPLY_DESCRIPTIONS.invokeExact(reply, items, count);
        }
        catch (Throwable t)
        {
            throw linkage("pure_reply_descriptions", t);
        }
    }

    static int replyNames(MemorySegment reply, MemorySegment items, long count)
    {
        try
        {
            return (int) REPLY_NAMES.invokeExact(reply, items, count);
        }
        catch (Throwable t)
        {
            throw linkage("pure_reply_names", t);
        }
    }

    static int replyFail(MemorySegment reply, int kind, MemorySegment message, long messageLen)
    {
        try
        {
            return (int) REPLY_FAIL.invokeExact(reply, kind, message, messageLen);
        }
        catch (Throwable t)
        {
            throw linkage("pure_reply_fail", t);
        }
    }

    /**
     * A string as the ABI takes it: UTF-8 bytes the Rust side reads in place, never
     * NUL-terminated, plus their count. The empty string points at one byte so its
     * pointer is never NULL.
     *
     * @param ptr the bytes
     * @param len the byte count
     */
    record Text(MemorySegment ptr, long len)
    {
        static Text of(String value, SegmentAllocator allocator)
        {
            byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
            MemorySegment ptr = encoded.length == 0 ? allocator.allocate(1) : allocator.allocateFrom(ValueLayout.JAVA_BYTE, encoded);
            return new Text(ptr, encoded.length);
        }
    }

    /**
     * Write a {@code pure_string} field: NULL for the host's null.
     *
     * @param struct    the struct holding the field
     * @param offset    the field's offset
     * @param value     the string, or {@code null} for absent
     * @param allocator where the bytes live for the call
     */
    static void writeString(MemorySegment struct, long offset, String value, SegmentAllocator allocator)
    {
        if (value == null)
        {
            struct.set(ValueLayout.ADDRESS, offset + STRING_PTR, MemorySegment.NULL);
            struct.set(ValueLayout.JAVA_LONG, offset + STRING_LEN, 0L);
            return;
        }
        Text text = Text.of(value, allocator);
        struct.set(ValueLayout.ADDRESS, offset + STRING_PTR, text.ptr());
        struct.set(ValueLayout.JAVA_LONG, offset + STRING_LEN, text.len());
    }

    /**
     * Read a {@code pure_string} field of a Rust-owned struct.
     *
     * @param struct the struct holding the field
     * @param offset the field's offset
     * @return the string, or {@code null} when absent
     */
    static String readString(MemorySegment struct, long offset)
    {
        MemorySegment ptr = struct.get(ValueLayout.ADDRESS, offset + STRING_PTR);
        if (ptr.equals(MemorySegment.NULL))
        {
            return null;
        }
        long len = struct.get(ValueLayout.JAVA_LONG, offset + STRING_LEN);
        return len == 0 ? "" : new String(ptr.reinterpret(len).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8);
    }

    /**
     * Read a {@code (const char *, size_t)} argument pair of a callback.
     *
     * @param ptr the bytes, as the zero-length segment the stub received
     * @param len the byte count
     * @return the string
     */
    static String readText(MemorySegment ptr, long len)
    {
        return len == 0 ? "" : new String(ptr.reinterpret(len).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8);
    }
}
