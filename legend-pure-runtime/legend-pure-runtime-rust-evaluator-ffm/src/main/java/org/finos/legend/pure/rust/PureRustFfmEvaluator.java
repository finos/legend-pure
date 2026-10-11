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
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.AbstractConsole;
import org.finos.legend.pure.m3.execution.Console;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.execution.OutputWriter;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.rust.interpreted.InterpretedHost;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

/**
 * A {@link FunctionExecution} over the Rust evaluator through the C ABI of
 * {@code libpure_rust_ffi}, called with the Foreign Function and Memory API.
 * One evaluator is one {@code pure_context}, entered by one thread at a time;
 * one shared {@link Arena} holds its callback stubs for its lifetime and a
 * confined arena per call holds what the call writes for the Rust side to read
 * in place. Every status other than {@code PURE_STATUS_OK} is read through
 * {@code pure_last_failure} on the same thread and raised as a
 * {@link PureRustEvaluationException}, or a {@link PureRustException} for a
 * panic, which leaves the context poisoned.
 */
public final class PureRustFfmEvaluator implements FunctionExecution, RustHeap, Closeable
{
    /**
     * System property naming the directory that holds the cdylib; set by surefire.
     */
    public static final String LIBDIR_PROPERTY = PureAbi.LIBDIR_PROPERTY;

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

    /**
     * What a described node becomes on the Rust side, in the order of {@code PURE_CROSSING_*}.
     */
    enum Crossing
    {
        ELEMENT,
        ENUM_VALUE,
        HOST_NODE,
        RUST_OBJECT,
        PRIMITIVE
    }

    private final Console console = new RustConsole();
    private PureRuntime runtime;
    private ProcessorSupport processorSupport;
    private FfmNodeBridge bridge;
    private Arena arena;
    private MemorySegment context;
    private boolean closed;

    @Override
    public synchronized void init(PureRuntime pureRuntime, Message message)
    {
        this.runtime = pureRuntime;
        attach(new InterpretedHost(pureRuntime.getModelRepository(), pureRuntime.getProcessorSupport()));
    }

    /**
     * An evaluator over any engine's graph, for engines that are not a
     * {@link PureRuntime}: the compiled engine reaches it through
     * {@code evalRustFfm}'s executor.
     *
     * @param host the engine's side of the bridge
     * @return an initialised evaluator; {@link #start} is for the interpreted engine, {@link #evaluate} for any
     */
    public static PureRustFfmEvaluator over(RustEngineHost host)
    {
        PureRustFfmEvaluator evaluator = new PureRustFfmEvaluator();
        evaluator.attach(host);
        return evaluator;
    }

    private synchronized void attach(RustEngineHost host)
    {
        if (this.context != null)
        {
            throw new IllegalStateException("Evaluator is already initialised");
        }
        this.processorSupport = host.processorSupport();
        this.bridge = new FfmNodeBridge(host, this);
        Arena created = Arena.ofShared();
        try
        {
            MemorySegment table = this.bridge.hostTable(created);
            MemorySegment out = created.allocate(ValueLayout.ADDRESS);
            if (PureAbi.contextNew(table, out) != PureAbi.STATUS_OK)
            {
                throw lastFailure();
            }
            this.context = out.get(ValueLayout.ADDRESS, 0);
            this.arena = created;
        }
        catch (RuntimeException | Error e)
        {
            created.close();
            throw e;
        }
    }

    /**
     * The Pure exception the interpreted engine rethrows for a Rust failure.
     *
     * @param e      the failure as the boundary reports it
     * @param source the Rust anchor, or null
     * @return an assertion failure for {@code ASSERTION_FAILED}; otherwise an execution error whose info is the
     * bare Pure text for the Pure-level kinds and the kind-prefixed message for the Rust-side diagnostics
     */
    private static PureException pureCause(PureRustEvaluationException e, SourceInformation source)
    {
        return switch (e.getKind())
        {
            case ASSERTION_FAILED -> new PureAssertFailException(source, e.getInfo());
            case EXECUTION_ERROR, CONSTRAINT_VIOLATION -> new PureExecutionException(source, e.getInfo());
            default -> new PureExecutionException(source, e.getMessage());
        };
    }

    @Override
    public synchronized CoreInstance start(CoreInstance function, ListIterable<? extends CoreInstance> arguments)
    {
        CoreInstance[][] slots = new CoreInstance[arguments.size()][];
        for (int i = 0; i < slots.length; i++)
        {
            slots[i] = unwrap(arguments.get(i));
        }
        CoreInstance[] results = evaluate(function, slots);
        return ValueSpecificationBootstrap.wrapValueSpecification(Lists.immutable.with(results), true, this.processorSupport);
    }

    /**
     * Run a function node with its arguments and return the raw results, each
     * as the host engine represents it.
     *
     * @param function  the function node: a lambda, a function element or a native
     * @param arguments one slot of values per parameter
     * @return the result values
     * @throws PureRustEvaluationException a typed failure, carrying the Pure exception as its cause
     */
    public synchronized CoreInstance[] evaluate(CoreInstance function, CoreInstance[][] arguments)
    {
        checkOpen();
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
        catch (PureRustEvaluationException e)
        {
            e.initCause(pureCause(e, sourceInformationOf(e.getSourceInfo())));
            throw e;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void start(CoreInstance func, ListIterable<? extends CoreInstance> arguments, OutputStream outputStream, OutputWriter writer)
    {
        try
        {
            writer.write(start(func, arguments), outputStream);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Console getConsole()
    {
        return this.console;
    }

    @Override
    public boolean isFullyInitializedForExecution()
    {
        return (this.context != null) && !this.closed;
    }

    @Override
    public void resetEventHandlers()
    {
    }

    @Override
    public ProcessorSupport getProcessorSupport()
    {
        return this.processorSupport;
    }

    @Override
    public PureRuntime getRuntime()
    {
        return this.runtime;
    }

    @Override
    public OutputWriter<CoreInstance> newOutputWriter()
    {
        return (result, outputStream) -> outputStream.write(String.valueOf(result).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Allocate a Rust heap object and return it as a {@link CoreInstance}.
     * Each value slot holds the values of one property.
     *
     * @param classifierPath the class path as the Rust side spells it
     * @param propertyNames  property names, parallel to {@code propertyValues}
     * @param propertyValues values per property
     * @return the Java view of the new object
     */
    public synchronized CoreInstance create(String classifierPath, String[] propertyNames, CoreInstance[][] propertyValues)
    {
        checkOpen();
        try (Arena call = Arena.ofConfined())
        {
            PureAbi.Text classifier = PureAbi.Text.of(classifierPath, call);
            MemorySegment properties = this.bridge.properties(propertyNames, propertyValues, call);
            MemorySegment outHandle = call.allocate(ValueLayout.JAVA_LONG);
            if (PureAbi.objectNew(this.context, classifier.ptr(), classifier.len(), properties, propertyNames.length, outHandle) != PureAbi.STATUS_OK)
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

    /**
     * Heap objects the Rust side keeps alive for this evaluator in either
     * direction; zero once every proxy is closed and every call returned.
     *
     * @return the live handle count
     */
    public synchronized long liveHandleCount()
    {
        return this.closed ? 0 : PureAbi.liveHandleCount(this.context);
    }

    /**
     * Java graph nodes the bridge keeps an id for because the Rust side may
     * still read them; nodes Rust has let go of are forgotten when a call ends.
     */
    synchronized int knownNodeCount()
    {
        return this.closed ? 0 : this.bridge.knownNodeCount();
    }

    synchronized Crossing crossing(CoreInstance node)
    {
        checkOpen();
        try (Arena call = Arena.ofConfined())
        {
            MemorySegment described = this.bridge.describeInto(node, call);
            MemorySegment out = call.allocate(ValueLayout.JAVA_INT);
            if (PureAbi.crossing(this.context, described, out) != PureAbi.STATUS_OK)
            {
                throw lastFailure();
            }
            return Crossing.values()[out.get(ValueLayout.JAVA_INT, 0)];
        }
    }

    @Override
    public synchronized ListIterable<? extends CoreInstance> propertyValues(long handle, String key)
    {
        checkOpen();
        try (Arena call = Arena.ofConfined())
        {
            PureAbi.Text name = PureAbi.Text.of(key, call);
            MemorySegment out = call.allocate(ValueLayout.ADDRESS);
            if (PureAbi.objectPropertyValues(this.context, handle, name.ptr(), name.len(), out) != PureAbi.STATUS_OK)
            {
                throw lastFailure();
            }
            return Lists.immutable.with(materialize(out.get(ValueLayout.ADDRESS, 0)));
        }
    }

    @Override
    public synchronized ListIterable<String> propertyNames(long handle)
    {
        checkOpen();
        try (Arena call = Arena.ofConfined())
        {
            MemorySegment out = call.allocate(ValueLayout.ADDRESS);
            if (PureAbi.objectPropertyNames(this.context, handle, out) != PureAbi.STATUS_OK)
            {
                throw lastFailure();
            }
            return Lists.immutable.with(strings(out.get(ValueLayout.ADDRESS, 0)));
        }
    }

    @Override
    public synchronized void release(long handle)
    {
        if (!this.closed)
        {
            PureAbi.objectRelease(this.context, handle);
            this.bridge.forgetProxy(handle);
        }
    }

    @Override
    public synchronized void close()
    {
        if (this.closed)
        {
            return;
        }
        if (this.context != null)
        {
            for (long handle : this.bridge.proxyHandles())
            {
                PureAbi.objectRelease(this.context, handle);
            }
            this.bridge.clear();
            if (PureAbi.contextFree(this.context) != PureAbi.STATUS_OK)
            {
                throw lastFailure();
            }
            this.context = null;
            this.arena.close();
        }
        this.closed = true;
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
     * panic, which has poisoned the context.
     */
    private static RuntimeException lastFailure()
    {
        MemorySegment failure = PureAbi.lastFailure();
        if (failure == null)
        {
            return new PureRustException("the Rust evaluator reported a failure and recorded none");
        }
        int kind = failure.get(ValueLayout.JAVA_INT, PureAbi.FAILURE_KIND);
        String message = PureAbi.readString(failure, PureAbi.FAILURE_MESSAGE);
        if (kind == PureAbi.FAILURE_PANIC)
        {
            return new PureRustException(message);
        }
        long frames = failure.get(ValueLayout.JAVA_LONG, PureAbi.FAILURE_CALL_STACK_LEN);
        String[] callStack = new String[Math.toIntExact(frames)];
        if (frames > 0)
        {
            MemorySegment stack = failure.get(ValueLayout.ADDRESS, PureAbi.FAILURE_CALL_STACK).reinterpret(frames * PureAbi.STRING.byteSize());
            for (int i = 0; i < callStack.length; i++)
            {
                callStack[i] = PureAbi.readString(stack, i * PureAbi.STRING.byteSize());
            }
        }
        return new PureRustEvaluationException(
                message,
                PureAbi.readString(failure, PureAbi.FAILURE_INFO),
                kindName(kind),
                PureAbi.readString(failure, PureAbi.FAILURE_SOURCE),
                callStack,
                PureAbi.readString(failure, PureAbi.FAILURE_CONSTRAINT_ID),
                PureAbi.readString(failure, PureAbi.FAILURE_CONSTRAINT_KIND),
                PureAbi.readString(failure, PureAbi.FAILURE_OWNER));
    }

    private static String kindName(int kind)
    {
        PureRustEvaluationException.Kind named = switch (kind)
        {
            case PureAbi.FAILURE_ASSERTION_FAILED -> PureRustEvaluationException.Kind.ASSERTION_FAILED;
            case PureAbi.FAILURE_CONSTRAINT_VIOLATION -> PureRustEvaluationException.Kind.CONSTRAINT_VIOLATION;
            case PureAbi.FAILURE_FUNCTION_NOT_FOUND -> PureRustEvaluationException.Kind.FUNCTION_NOT_FOUND;
            case PureAbi.FAILURE_UNSUPPORTED -> PureRustEvaluationException.Kind.UNSUPPORTED;
            case PureAbi.FAILURE_NOT_IMPLEMENTED -> PureRustEvaluationException.Kind.NOT_IMPLEMENTED;
            default -> PureRustEvaluationException.Kind.EXECUTION_ERROR;
        };
        return named.name();
    }

    private void checkOpen()
    {
        if (this.closed)
        {
            throw new PureRustEvaluationException("Evaluator has been closed");
        }
        if (this.context == null)
        {
            throw new PureRustEvaluationException("Evaluator has not been initialised: call init(runtime, message) first");
        }
    }

    private CoreInstance[] unwrap(CoreInstance argument)
    {
        if (argument == null)
        {
            return new CoreInstance[0];
        }
        if (Instance.instanceOf(argument, M3Paths.InstanceValue, this.processorSupport))
        {
            return argument.getValueForMetaPropertyToMany(M3Properties.values).toArray(new CoreInstance[0]);
        }
        return new CoreInstance[]{argument};
    }

    private static SourceInformation sourceInformationOf(String text)
    {
        if (text == null)
        {
            return null;
        }
        int lastColon = text.lastIndexOf(':');
        int middleColon = (lastColon < 0) ? -1 : text.lastIndexOf(':', lastColon - 1);
        if (middleColon < 0)
        {
            return new SourceInformation(text, 0, 0, 0, 0);
        }
        try
        {
            int line = Integer.parseInt(text.substring(middleColon + 1, lastColon));
            int column = Integer.parseInt(text.substring(lastColon + 1));
            return new SourceInformation(text.substring(0, middleColon), line, column, line, column);
        }
        catch (NumberFormatException e)
        {
            return new SourceInformation(text, 0, 0, 0, 0);
        }
    }

    private static final class RustConsole extends AbstractConsole
    {
        @Override
        protected String getContentString(Object content)
        {
            return String.valueOf(content);
        }
    }
}
