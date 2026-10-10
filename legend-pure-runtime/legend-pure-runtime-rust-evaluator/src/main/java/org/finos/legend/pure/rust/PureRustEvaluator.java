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
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

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
 */
public final class PureRustEvaluator implements FunctionExecution, Closeable
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
    private ModelRepository repository;
    private RustNodeBridge bridge;
    private long contextPointer;
    private boolean closed;

    @Override
    public synchronized void init(PureRuntime pureRuntime, Message message)
    {
        if (this.contextPointer != 0)
        {
            throw new IllegalStateException("Evaluator is already initialised");
        }
        this.runtime = pureRuntime;
        this.processorSupport = pureRuntime.getProcessorSupport();
        this.repository = pureRuntime.getModelRepository();
        this.bridge = new RustNodeBridge(this.repository, this.processorSupport, this);
        this.contextPointer = nativeInitContext(this.bridge);
    }

    @Override
    public synchronized CoreInstance start(CoreInstance function, ListIterable<? extends CoreInstance> arguments)
    {
        checkOpen();
        CoreInstance[][] slots = new CoreInstance[arguments.size()][];
        for (int i = 0; i < slots.length; i++)
        {
            slots[i] = unwrap(arguments.get(i));
        }
        CoreInstance[] results;
        try
        {
            results = nativeStart(this.contextPointer, function, slots);
        }
        catch (PureRustEvaluationException e)
        {
            if (e.getKind() == PureRustEvaluationException.Kind.ASSERTION_FAILED)
            {
                e.initCause(new PureAssertFailException(sourceInformationOf(e.getSourceInfo()), e.getMessage()));
            }
            throw e;
        }
        return ValueSpecificationBootstrap.wrapValueSpecification(Lists.immutable.with(results), true, this.processorSupport);
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
        long handle = nativeNew(this.contextPointer, classifierPath, propertyNames, propertyValues);
        return this.bridge.rustObject(handle, classifierPath);
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

    synchronized String crossingKind(CoreInstance node)
    {
        checkOpen();
        return nativeCrossingKind(this.contextPointer, node);
    }

    synchronized ListIterable<? extends CoreInstance> propertyValues(RustCoreInstance proxy, String key)
    {
        checkOpen();
        return Lists.immutable.with(nativeGetProperty(this.contextPointer, proxy.handle(), key));
    }

    synchronized ListIterable<String> propertyNames(RustCoreInstance proxy)
    {
        checkOpen();
        return Lists.immutable.with(nativePropertyNames(this.contextPointer, proxy.handle()));
    }

    synchronized void release(RustCoreInstance proxy)
    {
        if (!this.closed)
        {
            nativeFreeInstance(this.contextPointer, proxy.handle());
            this.bridge.forgetProxy(proxy.handle());
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
            for (RustCoreInstance proxy : this.bridge.proxies())
            {
                nativeFreeInstance(this.contextPointer, proxy.handle());
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

    private static native void nativeFreeInstance(long contextPtr, long handle);

    private static native void nativeFreeContext(long contextPtr);
}
