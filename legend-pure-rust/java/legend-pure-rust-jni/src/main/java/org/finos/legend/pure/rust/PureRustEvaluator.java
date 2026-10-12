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
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/**
 * The Rust evaluator as a {@link FunctionExecution}: created empty, bound to
 * a {@link PureRuntime} by {@link #init}, and driven with
 * {@link #start(CoreInstance, ListIterable)} exactly like the interpreted
 * engine. The only value currency across the boundary is {@link CoreInstance}.
 * The JNI transport: the native methods below are the exported symbols of
 * {@code libpure_rust_jni}, and the Rust objects it returns are read back
 * through it as the {@link RustHeap} of their proxies.
 */
public final class PureRustEvaluator implements FunctionExecution, RustHeap, Closeable
{
    /**
     * System property naming the directory that holds the cdylib; set by surefire.
     */
    public static final String LIBDIR_PROPERTY = "legend.pure.rust.libdir";

    static
    {
        String dir = System.getProperty(LIBDIR_PROPERTY);
        if ((dir == null) || dir.isEmpty())
        {
            System.loadLibrary("pure_rust_jni");
        }
        else
        {
            System.load(Paths.get(dir, System.mapLibraryName("pure_rust_jni")).toAbsolutePath().toString());
        }
    }

    private final Console console = new RustConsole();
    private PureRuntime runtime;
    private ProcessorSupport processorSupport;
    private RustNodeBridge bridge;
    private long contextPointer;
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
     * {@code evalRust}'s entry, {@code CompiledEvalRust}.
     *
     * @param host the engine's side of the bridge
     * @return an initialised evaluator; {@link #start} is for the interpreted engine, {@link #evaluate} for any
     */
    public static PureRustEvaluator over(RustEngineHost host)
    {
        PureRustEvaluator evaluator = new PureRustEvaluator();
        evaluator.attach(host);
        return evaluator;
    }

    private synchronized void attach(RustEngineHost host)
    {
        if (this.contextPointer != 0)
        {
            throw new IllegalStateException("Evaluator is already initialised");
        }
        this.processorSupport = host.processorSupport();
        this.bridge = new RustNodeBridge(host, this);
        this.contextPointer = nativeInitContext(this.bridge);
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
        switch (e.getKind())
        {
            case ASSERTION_FAILED:
                return new PureAssertFailException(source, e.getInfo());
            case EXECUTION_ERROR:
            case CONSTRAINT_VIOLATION:
                return new PureExecutionException(source, e.getInfo());
            default:
                return new PureExecutionException(source, e.getMessage());
        }
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
        this.bridge.releaseCollectedProxies();
        try
        {
            return nativeStart(this.contextPointer, function, arguments);
        }
        catch (PureRustEvaluationException e)
        {
            SourceInformation source = sourceInformationOf(e.getSourceInfo());
            e.initCause(pureCause(e, source));
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
        return (this.contextPointer != 0) && !this.closed;
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
        this.bridge.releaseCollectedProxies();
        long handle = nativeNew(this.contextPointer, classifierPath, propertyNames, propertyValues);
        return this.bridge.rustObject(handle, classifierPath);
    }

    /**
     * Where the last lowered call on this thread spent its time, for
     * measurement: the lowering (both phases, including every JNI read it
     * made) and the run of the lowered closure, in nanoseconds, then the
     * number of JNI reads made during the run (zero means the run was the
     * Rust evaluator alone) and during the lowering (zero means the lowered
     * lambda came from the closure cache).
     *
     * @return {@code [loweringNanos, runNanos, jniReadsDuringRun, jniReadsDuringLowering]}
     */
    public static long[] lastTimings()
    {
        return nativeLastTimings();
    }

    /**
     * Heap objects the Rust side keeps alive for this evaluator in either
     * direction; zero once every proxy is closed and every call returned.
     *
     * @return the live handle count
     */
    public synchronized long liveHandleCount()
    {
        return this.closed ? 0 : nativeLiveHandleCount(this.contextPointer);
    }

    /**
     * Java graph nodes the bridge keeps an id for because the Rust side may
     * still read them; nodes Rust has let go of are forgotten when a call ends.
     */
    synchronized int knownNodeCount()
    {
        return this.closed ? 0 : this.bridge.knownNodeCount();
    }

    synchronized String crossingKind(CoreInstance node)
    {
        checkOpen();
        return nativeCrossingKind(this.contextPointer, node);
    }

    @Override
    public synchronized ListIterable<? extends CoreInstance> propertyValues(long handle, String key)
    {
        checkOpen();
        return Lists.immutable.with(nativeGetProperty(this.contextPointer, handle, key));
    }

    @Override
    public synchronized ListIterable<String> propertyNames(long handle)
    {
        checkOpen();
        return Lists.immutable.with(nativePropertyNames(this.contextPointer, handle));
    }

    @Override
    public synchronized void release(long handle)
    {
        if (!this.closed)
        {
            nativeFreeInstance(this.contextPointer, handle);
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
        if (this.contextPointer != 0)
        {
            for (long handle : this.bridge.proxyHandles())
            {
                nativeFreeInstance(this.contextPointer, handle);
            }
            this.bridge.clear();
            nativeFreeContext(this.contextPointer);
        }
        this.closed = true;
    }

    private void checkOpen()
    {
        if (this.closed)
        {
            throw new PureRustEvaluationException("Evaluator has been closed");
        }
        if (this.contextPointer == 0)
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

    private static native long nativeInitContext(RustNodeBridge bridge);

    private static native CoreInstance[] nativeStart(long contextPtr, CoreInstance function, CoreInstance[][] arguments);

    private static native CoreInstance[] nativeGetProperty(long contextPtr, long handle, String propertyName);

    private static native String[] nativePropertyNames(long contextPtr, long handle);

    private static native String nativeGetClassifier(long contextPtr, long handle);

    private static native long nativeNew(long contextPtr, String classifierPath, String[] propertyNames, CoreInstance[][] propertyValues);

    private static native String nativeCrossingKind(long contextPtr, CoreInstance node);

    private static native long nativeLiveHandleCount(long contextPtr);

    private static native long[] nativeLastTimings();

    private static native void nativeFreeInstance(long contextPtr, long handle);

    private static native void nativeFreeContext(long contextPtr);
}
