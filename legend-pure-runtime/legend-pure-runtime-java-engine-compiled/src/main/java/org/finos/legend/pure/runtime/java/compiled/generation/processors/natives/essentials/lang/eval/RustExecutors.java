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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.lang.eval;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Finds the executor a Rust evaluator native hands its function value to: the
 * first provider of the service on the context class loader, then on the
 * engine's own. Shared by {@link EvalRust} and {@link EvalRustFfm}.
 */
final class RustExecutors
{
    private RustExecutors()
    {
    }

    /**
     * The first provider of {@code service} on the class path, or null when there is none.
     *
     * @param service the executor interface the transport module implements
     * @param <T>     the executor interface
     * @return the executor, or null
     */
    static <T> T load(Class<T> service)
    {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        T found = (context == null) ? null : first(service, context);
        return (found == null) ? first(service, RustExecutors.class.getClassLoader()) : found;
    }

    /**
     * The failure a native raises when no executor is on the class path.
     *
     * @param signature the native's signature
     * @param module    the Maven module that provides the executor
     * @return the exception to throw
     */
    static UnsupportedOperationException missing(String signature, String module)
    {
        return new UnsupportedOperationException(signature + " needs the Rust evaluator (" + module + ") on the class path");
    }

    private static <T> T first(Class<T> service, ClassLoader loader)
    {
        Iterator<T> found = ServiceLoader.load(service, loader).iterator();
        return found.hasNext() ? found.next() : null;
    }
}
