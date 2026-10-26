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

import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * The FFM transport of {@link RustEvaluator}: the C ABI of
 * {@code libpure_rust_ffi}, called with the Foreign Function and Memory API.
 * One evaluator is one {@code pure_context}, entered by one thread at a time;
 * one shared {@link Arena} holds its callback stubs for its lifetime and a
 * confined arena per call holds what the call writes for the Rust side to read
 * in place. Every status other than {@code PURE_STATUS_OK} is read through
 * {@code pure_last_failure} on the same thread and raised as a
 * {@link PureRustEvaluationException}, or a {@link PureRustException} for a
 * panic, which leaves the context poisoned, and for a failure kind the header
 * does not define.
 */
public final class PureRustFfmEvaluator extends RustEvaluator
{
    /**
     * Where the last call on this thread spent its time, in nanoseconds, and
     * how many host reads it made in each phase; zero reads during the lowering
     * means the lambda came from the closure cache, zero during the run means
     * the run was the Rust evaluator alone.
     *
     * @param lowerNanos     the lowering, both phases
     * @param runNanos       the run of the lowered closure
     * @param runHostReads   host reads made during the run
     * @param lowerHostReads host reads made during the lowering
     */
    public record Timings(long lowerNanos, long runNanos, long runHostReads, long lowerHostReads)
    {
        /**
         * @return {@code [lowerNanos, runNanos, runHostReads, lowerHostReads]}, the shape the benchmark reads
         */
        public long[] toArray()
        {
            return new long[]{this.lowerNanos, this.runNanos, this.runHostReads, this.lowerHostReads};
        }
    }

    private FfmNodeBridge bridge;
    private Arena arena;
    private MemorySegment context;

    /**
     * An evaluator over any engine's graph, for engines that are not a
     * {@code PureRuntime}.
     *
     * @param host the engine's side of the bridge
     * @return a bound evaluator; {@link #start} is for the interpreted engine, {@link #evaluate} for any
     */
    public static PureRustFfmEvaluator over(RustEngineHost host)
    {
        PureRustFfmEvaluator evaluator = new PureRustFfmEvaluator();
        evaluator.attach(host);
        return evaluator;
    }

    /**
     * Where the last call on this thread spent its time, for measurement.
     *
     * @return the Rust side's split of the last call
     */
    public static Timings lastTimings()
    {
        try (Arena call = Arena.ofConfined())
        {
            MemorySegment out = call.allocate(PureAbi.TIMINGS);
            PureAbi.lastTimings(out);
            return new Timings(
                    out.get(ValueLayout.JAVA_LONG, PureAbi.TIMINGS_LOWER_NS),
                    out.get(ValueLayout.JAVA_LONG, PureAbi.TIMINGS_RUN_NS),
                    out.get(ValueLayout.JAVA_LONG, PureAbi.TIMINGS_RUN_HOST_READS),
                    out.get(ValueLayout.JAVA_LONG, PureAbi.TIMINGS_LOWER_HOST_READS));
        }
    }

    @Override
    protected NodeBridge open(RustEngineHost host)
    {
        FfmNodeBridge created = new FfmNodeBridge(host, this);
        Arena shared = Arena.ofShared();
        try
        {
            MemorySegment table = created.hostTable(shared);
            MemorySegment out = shared.allocate(ValueLayout.ADDRESS);
            if (PureAbi.contextNew(table, out) != PureAbi.STATUS_OK)
            {
                throw lastFailure();
            }
            this.context = out.get(ValueLayout.ADDRESS, 0);
            this.arena = shared;
            this.bridge = created;
            return created;
        }
        catch (RuntimeException | Error e)
        {
            shared.close();
            throw e;
        }
    }

    @Override
    protected CoreInstance[] run(CoreInstance function, CoreInstance[][] arguments)
    {
        try (Arena call = Arena.ofConfined())
        {
            MemorySegment described = this.bridge.describeInto(function, call);
            MemorySegment slots = this.bridge.slots(arguments, call);
            MemorySegment out = call.allocate(ValueLayout.ADDRESS);
            if (PureAbi.start(this.context, described, slots, arguments.length, out) != PureAbi.STATUS_OK)
            {
                throw lastFailure();
            }
            return materialize(out.get(ValueLayout.ADDRESS, 0));
        }
    }

    /**
     * Through {@code pure_test_object_new}, its classifier read back through {@code pure_object_classifier}.
     */
    @Override
    protected CoreInstance allocate(String classifierPath, String[] propertyNames, CoreInstance[][] propertyValues)
    {
        try (Arena call = Arena.ofConfined())
        {
            PureAbi.Text classifier = PureAbi.Text.of(classifierPath, call);
            MemorySegment properties = this.bridge.properties(propertyNames, propertyValues, call);
            MemorySegment outHandle = call.allocate(ValueLayout.JAVA_LONG);
            if (PureAbi.testObjectNew(this.context, classifier.ptr(), classifier.len(), properties, propertyNames.length, outHandle) != PureAbi.STATUS_OK)
            {
                throw lastFailure();
            }
            long handle = outHandle.get(ValueLayout.JAVA_LONG, 0);
            MemorySegment out = call.allocate(ValueLayout.ADDRESS);
            if (PureAbi.objectClassifier(this.context, handle, out) != PureAbi.STATUS_OK)
            {
                throw lastFailure();
            }
            return this.bridge.rustObject(handle, strings(out.get(ValueLayout.ADDRESS, 0))[0]);
        }
    }

    @Override
    protected CoreInstance[] objectPropertyValues(long handle, String key)
    {
        try (Arena call = Arena.ofConfined())
        {
            PureAbi.Text name = PureAbi.Text.of(key, call);
            MemorySegment out = call.allocate(ValueLayout.ADDRESS);
            if (PureAbi.objectPropertyValues(this.context, handle, name.ptr(), name.len(), out) != PureAbi.STATUS_OK)
            {
                throw lastFailure();
            }
            return materialize(out.get(ValueLayout.ADDRESS, 0));
        }
    }

    @Override
    protected String[] objectPropertyNames(long handle)
    {
        try (Arena call = Arena.ofConfined())
        {
            MemorySegment out = call.allocate(ValueLayout.ADDRESS);
            if (PureAbi.objectPropertyNames(this.context, handle, out) != PureAbi.STATUS_OK)
            {
                throw lastFailure();
            }
            return strings(out.get(ValueLayout.ADDRESS, 0));
        }
    }

    @Override
    protected void releaseHandle(long handle)
    {
        PureAbi.objectRelease(this.context, handle);
    }

    @Override
    protected long liveHandles()
    {
        return PureAbi.liveHandleCount(this.context);
    }

    @Override
    protected Crossing crossingOf(CoreInstance node)
    {
        try (Arena call = Arena.ofConfined())
        {
            MemorySegment described = this.bridge.describeInto(node, call);
            MemorySegment out = call.allocate(ValueLayout.JAVA_INT);
            if (PureAbi.testCrossing(this.context, described, out) != PureAbi.STATUS_OK)
            {
                throw lastFailure();
            }
            int crossing = out.get(ValueLayout.JAVA_INT, 0);
            return switch (crossing)
            {
                case PureAbi.CROSSING_ELEMENT -> Crossing.ELEMENT;
                case PureAbi.CROSSING_ENUM_VALUE -> Crossing.ENUM_VALUE;
                case PureAbi.CROSSING_HOST_NODE -> Crossing.HOST_NODE;
                case PureAbi.CROSSING_RUST_OBJECT -> Crossing.RUST_OBJECT;
                case PureAbi.CROSSING_PRIMITIVE -> Crossing.PRIMITIVE;
                default -> throw new PureRustException("libpure_rust_ffi reported the crossing " + crossing + ", which pure_rust.h does not define");
            };
        }
    }

    /**
     * Frees the context, then the arena of its stubs; a context the Rust side
     * could not free keeps its stubs.
     */
    @Override
    protected void free()
    {
        if (PureAbi.contextFree(this.context) != PureAbi.STATUS_OK)
        {
            throw lastFailure();
        }
        this.context = null;
        this.arena.close();
    }

    private CoreInstance[] materialize(MemorySegment result)
    {
        try
        {
            return this.bridge.materialize(result);
        }
        finally
        {
            PureAbi.resultFree(result);
        }
    }

    private static String[] strings(MemorySegment result)
    {
        try
        {
            return FfmNodeBridge.strings(result);
        }
        finally
        {
            PureAbi.resultFree(result);
        }
    }

    /**
     * The failure of the call that just returned a status other than OK, as the
     * exception to throw: typed for every kind the host may see, untyped for a
     * panic, which has poisoned the context, and for a kind the header does not
     * define, a protocol error.
     */
    private static RuntimeException lastFailure()
    {
        MemorySegment failure = PureAbi.lastFailure();
        if (failure == null)
        {
            return new PureRustException("the Rust evaluator reported a failure and recorded none");
        }
        int code = failure.get(ValueLayout.JAVA_INT, PureAbi.FAILURE_KIND);
        String message = PureAbi.readString(failure, PureAbi.FAILURE_MESSAGE);
        if (code == PureAbi.FAILURE_PANIC)
        {
            return new PureRustException(message);
        }
        PureRustEvaluationException.Kind kind = switch (code)
        {
            case PureAbi.FAILURE_EXECUTION_ERROR -> PureRustEvaluationException.Kind.EXECUTION_ERROR;
            case PureAbi.FAILURE_ASSERTION_FAILED -> PureRustEvaluationException.Kind.ASSERTION_FAILED;
            case PureAbi.FAILURE_CONSTRAINT_VIOLATION -> PureRustEvaluationException.Kind.CONSTRAINT_VIOLATION;
            case PureAbi.FAILURE_FUNCTION_NOT_FOUND -> PureRustEvaluationException.Kind.FUNCTION_NOT_FOUND;
            case PureAbi.FAILURE_UNSUPPORTED -> PureRustEvaluationException.Kind.UNSUPPORTED;
            case PureAbi.FAILURE_NOT_IMPLEMENTED -> PureRustEvaluationException.Kind.NOT_IMPLEMENTED;
            default -> throw new PureRustException("libpure_rust_ffi reported the failure kind " + code + ", which pure_rust.h does not define: " + message);
        };
        long frames = failure.get(ValueLayout.JAVA_LONG, PureAbi.FAILURE_CALL_STACK_LEN);
        String[] callStack = new String[Math.toIntExact(frames)];
        if (frames > 0)
        {
            MemorySegment stack = failure.get(ValueLayout.ADDRESS, PureAbi.FAILURE_CALL_STACK).reinterpret(frames * PureAbi.FRAME.byteSize());
            for (int i = 0; i < callStack.length; i++)
            {
                long frame = i * PureAbi.FRAME.byteSize();
                long span = frame + PureAbi.FRAME_SPAN;
                callStack[i] = PureAbi.readString(stack, frame + PureAbi.FRAME_FUNCTION)
                        + " <- " + PureAbi.readString(stack, span + PureAbi.SPAN_SOURCE)
                        + ":" + stack.get(ValueLayout.JAVA_INT, span + PureAbi.SPAN_START_LINE)
                        + ":" + stack.get(ValueLayout.JAVA_INT, span + PureAbi.SPAN_START_COLUMN);
            }
        }
        long source = PureAbi.FAILURE_SOURCE;
        return new PureRustEvaluationException(
                message,
                PureAbi.readString(failure, PureAbi.FAILURE_INFO),
                kind.name(),
                PureAbi.readString(failure, source + PureAbi.SPAN_SOURCE),
                failure.get(ValueLayout.JAVA_INT, source + PureAbi.SPAN_START_LINE),
                failure.get(ValueLayout.JAVA_INT, source + PureAbi.SPAN_START_COLUMN),
                failure.get(ValueLayout.JAVA_INT, source + PureAbi.SPAN_END_LINE),
                failure.get(ValueLayout.JAVA_INT, source + PureAbi.SPAN_END_COLUMN),
                callStack,
                PureAbi.readString(failure, PureAbi.FAILURE_CONSTRAINT_ID),
                PureAbi.readString(failure, PureAbi.FAILURE_CONSTRAINT_KIND),
                PureAbi.readString(failure, PureAbi.FAILURE_OWNER));
    }
}
