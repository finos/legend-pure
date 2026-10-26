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

import java.nio.file.Paths;

/**
 * The JNI transport of {@link RustEvaluator}: the native methods below are
 * the exported symbols of {@code libpure_rust_jni}, over one context pointer
 * per evaluator and a {@link RustNodeBridge} the Rust side calls back into.
 */
public final class PureRustEvaluator extends RustEvaluator
{
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

    private long contextPointer;

    /**
     * An evaluator over any engine's graph, for engines that are not a
     * {@code PureRuntime}.
     *
     * @param host the engine's side of the bridge
     * @return a bound evaluator; {@link #start} is for the interpreted engine, {@link #evaluate} for any
     */
    public static PureRustEvaluator over(RustEngineHost host)
    {
        PureRustEvaluator evaluator = new PureRustEvaluator();
        evaluator.attach(host);
        return evaluator;
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

    @Override
    protected NodeBridge open(RustEngineHost host)
    {
        RustNodeBridge bridge = new RustNodeBridge(host, this);
        this.contextPointer = nativeInitContext(bridge);
        return bridge;
    }

    @Override
    protected CoreInstance[] run(CoreInstance function, CoreInstance[][] arguments)
    {
        return nativeStart(this.contextPointer, function, arguments);
    }

    @Override
    protected CoreInstance allocate(String classifierPath, String[] propertyNames, CoreInstance[][] propertyValues)
    {
        long handle = nativeNew(this.contextPointer, classifierPath, propertyNames, propertyValues);
        return bridge().rustObject(handle, classifierPath);
    }

    @Override
    protected CoreInstance[] objectPropertyValues(long handle, String key)
    {
        return nativeGetProperty(this.contextPointer, handle, key);
    }

    @Override
    protected String[] objectPropertyNames(long handle)
    {
        return nativePropertyNames(this.contextPointer, handle);
    }

    @Override
    protected void releaseHandle(long handle)
    {
        nativeFreeInstance(this.contextPointer, handle);
    }

    @Override
    protected long liveHandles()
    {
        return nativeLiveHandleCount(this.contextPointer);
    }

    @Override
    protected Crossing crossingOf(CoreInstance node)
    {
        String kind = nativeCrossingKind(this.contextPointer, node);
        switch (kind)
        {
            case "element":
                return Crossing.ELEMENT;
            case "enum":
                return Crossing.ENUM_VALUE;
            case "javaNode":
                return Crossing.HOST_NODE;
            case "rustObject":
                return Crossing.RUST_OBJECT;
            case "primitive":
                return Crossing.PRIMITIVE;
            default:
                throw new PureRustException("libpure_rust_jni reported the crossing " + kind + ", which this class does not know");
        }
    }

    @Override
    protected void free()
    {
        nativeFreeContext(this.contextPointer);
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
