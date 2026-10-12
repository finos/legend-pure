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
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.rust.interpreted.InterpretedHost;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * The Rust evaluator as a {@link FunctionExecution}, whichever transport
 * carries the calls: created empty, bound to a {@link PureRuntime} by
 * {@link #init} or to any engine's {@link RustEngineHost} by
 * {@link #attach}, driven with {@link #start(CoreInstance, ListIterable)}
 * exactly like the interpreted engine, and closed once. The only value
 * currency across the boundary is {@link CoreInstance}. It is the
 * {@link RustHeap} the proxies of its Rust objects read through.
 *
 * <p>A transport supplies one native context per evaluator through the
 * protected methods below, each called with the evaluator's lock held, on an
 * evaluator that is bound and not closed ({@link #free} last, once). A typed
 * failure of the Rust side is a {@link PureRustEvaluationException}; anything
 * else it raises, a panic included, is a {@link PureRustException} and leaves
 * the context unusable.
 */
public abstract class RustEvaluator implements FunctionExecution, RustHeap, Closeable
{
    /**
     * System property naming the directory that holds a transport's cdylib; set by surefire.
     */
    public static final String LIBDIR_PROPERTY = "legend.pure.rust.libdir";

    /**
     * What a described node becomes on the Rust side.
     */
    public enum Crossing
    {
        /**
         * Unified with a model element of the Rust side.
         */
        ELEMENT,
        /**
         * A value of an enumeration the Rust side knows.
         */
        ENUM_VALUE,
        /**
         * A host node, read on demand.
         */
        HOST_NODE,
        /**
         * A Rust object the host holds a proxy of.
         */
        RUST_OBJECT,
        /**
         * Decoded to a primitive value.
         */
        PRIMITIVE
    }

    private final Console console = new RustConsole();
    private PureRuntime runtime;
    private ProcessorSupport processorSupport;
    private NodeBridge bridge;
    private boolean closed;

    @Override
    public synchronized void init(PureRuntime pureRuntime, Message message)
    {
        attach(new InterpretedHost(pureRuntime.getModelRepository(), pureRuntime.getProcessorSupport()));
        this.runtime = pureRuntime;
    }

    /**
     * Bind the evaluator to an engine's graph, once: the bridge and the
     * native context are created here.
     *
     * @param host the engine's side of the bridge
     * @throws IllegalStateException when the evaluator is already bound
     */
    protected final synchronized void attach(RustEngineHost host)
    {
        if (this.bridge != null)
        {
            throw new IllegalStateException("Evaluator is already initialised");
        }
        ProcessorSupport support = host.processorSupport();
        this.bridge = open(host);
        this.processorSupport = support;
    }

    /**
     * Create the bridge over the host and the native context over the bridge.
     *
     * @param host the engine's side of the bridge
     * @return the bridge; when this throws, the evaluator stays unbound and holds no native context
     */
    protected abstract NodeBridge open(RustEngineHost host);

    /**
     * Run a function node in the native context.
     *
     * @param function  the function node
     * @param arguments one slot of values per parameter
     * @return the result values, each as the host engine represents it
     */
    protected abstract CoreInstance[] run(CoreInstance function, CoreInstance[][] arguments);

    /**
     * Allocate a Rust object in the native context, test support.
     *
     * @param classifierPath the class path as the Rust side spells it
     * @param propertyNames  property names, parallel to {@code propertyValues}
     * @param propertyValues values per property
     * @return the object's proxy, from {@link NodeBridge#rustObject}
     */
    protected abstract CoreInstance allocate(String classifierPath, String[] propertyNames, CoreInstance[][] propertyValues);

    /**
     * The values of one property of a Rust object, each as the host engine represents it.
     *
     * @param handle the Rust object's handle
     * @param key    the property name
     * @return the values
     */
    protected abstract CoreInstance[] objectPropertyValues(long handle, String key);

    /**
     * The property names a Rust object holds values for.
     *
     * @param handle the Rust object's handle
     * @return the names
     */
    protected abstract String[] objectPropertyNames(long handle);

    /**
     * Release a handle on the Rust side; a released or unknown handle is ignored.
     *
     * @param handle the Rust object's handle
     */
    protected abstract void releaseHandle(long handle);

    /**
     * Handles the native context keeps alive in either direction.
     *
     * @return the count
     */
    protected abstract long liveHandles();

    /**
     * Which Rust shape a node becomes once described, test support.
     *
     * @param node the node
     * @return its crossing
     */
    protected abstract Crossing crossingOf(CoreInstance node);

    /**
     * Free the native context, after every handle this side held was released.
     */
    protected abstract void free();

    /**
     * The bridge of a bound evaluator.
     *
     * @return the bridge {@link #open} returned
     */
    protected final NodeBridge bridge()
    {
        return this.bridge;
    }

    /**
     * The Pure exception the host engine rethrows for a typed Rust failure.
     *
     * @param e the failure as the boundary reports it
     * @return an assertion failure for {@code ASSERTION_FAILED}; otherwise an execution error whose info is the
     * bare Pure text for the Pure-level kinds and the kind-prefixed message for the Rust-side diagnostics
     */
    private static PureException pureCause(PureRustEvaluationException e)
    {
        switch (e.getKind())
        {
            case ASSERTION_FAILED:
                return new PureAssertFailException(e.getSourceInformation(), e.getInfo());
            case EXECUTION_ERROR:
            case CONSTRAINT_VIOLATION:
                return new PureExecutionException(e.getSourceInformation(), e.getInfo());
            case FUNCTION_NOT_FOUND:
            case UNSUPPORTED:
            case NOT_IMPLEMENTED:
                return new PureExecutionException(e.getSourceInformation(), e.getMessage());
            default:
                throw new IllegalStateException("no Pure cause for the failure kind " + e.getKind());
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
    public final synchronized CoreInstance[] evaluate(CoreInstance function, CoreInstance[][] arguments)
    {
        checkOpen();
        this.bridge.releaseCollectedProxies();
        try
        {
            return run(function, arguments);
        }
        catch (PureRustEvaluationException e)
        {
            e.initCause(pureCause(e));
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
        return isOpen();
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
     * Each value slot holds the values of one property. Test support: it is
     * not Pure's {@code new}.
     *
     * @param classifierPath the class path as the Rust side spells it
     * @param propertyNames  property names, parallel to {@code propertyValues}
     * @param propertyValues values per property
     * @return the Java view of the new object
     */
    public final synchronized CoreInstance create(String classifierPath, String[] propertyNames, CoreInstance[][] propertyValues)
    {
        checkOpen();
        this.bridge.releaseCollectedProxies();
        return allocate(classifierPath, propertyNames, propertyValues);
    }

    /**
     * Heap objects the Rust side keeps alive for this evaluator in either
     * direction; zero once every proxy is closed and every call returned.
     *
     * @return the live handle count
     */
    public final synchronized long liveHandleCount()
    {
        return isOpen() ? liveHandles() : 0L;
    }

    /**
     * Java graph nodes the bridge keeps an id for because the Rust side may
     * still read them; nodes Rust has let go of are forgotten when a call ends.
     */
    final synchronized int knownNodeCount()
    {
        return isOpen() ? this.bridge.knownNodeCount() : 0;
    }

    final synchronized Crossing crossing(CoreInstance node)
    {
        checkOpen();
        return crossingOf(node);
    }

    @Override
    public final synchronized ListIterable<? extends CoreInstance> propertyValues(long handle, String key)
    {
        checkOpen();
        return Lists.immutable.with(objectPropertyValues(handle, key));
    }

    @Override
    public final synchronized ListIterable<String> propertyNames(long handle)
    {
        checkOpen();
        return Lists.immutable.with(objectPropertyNames(handle));
    }

    @Override
    public final synchronized void release(long handle)
    {
        if (isOpen())
        {
            releaseHandle(handle);
            this.bridge.forgetProxy(handle);
        }
    }

    /**
     * Release every handle Java holds and free the native context; a second
     * call does nothing, and every call after the first is refused.
     */
    @Override
    public final synchronized void close()
    {
        if (this.closed)
        {
            return;
        }
        try
        {
            if (this.bridge != null)
            {
                for (long handle : this.bridge.proxyHandles())
                {
                    releaseHandle(handle);
                }
                this.bridge.clear();
                free();
            }
        }
        finally
        {
            this.closed = true;
        }
    }

    private boolean isOpen()
    {
        return (this.bridge != null) && !this.closed;
    }

    private void checkOpen()
    {
        if (this.closed)
        {
            throw new PureRustEvaluationException("Evaluator has been closed");
        }
        if (this.bridge == null)
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

    private static final class RustConsole extends AbstractConsole
    {
        @Override
        protected String getContentString(Object content)
        {
            return String.valueOf(content);
        }
    }
}
