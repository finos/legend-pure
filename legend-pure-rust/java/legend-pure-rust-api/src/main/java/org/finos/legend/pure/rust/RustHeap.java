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
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

/**
 * One evaluator's Rust heap as its {@link RustCoreInstance} proxies read it:
 * an object's property values and names by handle, the Java graph its
 * classifier path resolves in, and the release of a handle Java no longer
 * needs. Each transport's evaluator implements it.
 */
public interface RustHeap
{
    /**
     * The graph the proxies' classifier paths resolve in.
     *
     * @return the processor support of the engine the evaluator serves
     */
    ProcessorSupport getProcessorSupport();

    /**
     * The values of one property of a Rust object, each as the host engine
     * represents it.
     *
     * @param handle the Rust object's handle
     * @param key    the property name
     * @return the values, empty when the object holds none
     */
    ListIterable<? extends CoreInstance> propertyValues(long handle, String key);

    /**
     * The property names a Rust object holds values for.
     *
     * @param handle the Rust object's handle
     * @return the names, in no particular order
     */
    ListIterable<String> propertyNames(long handle);

    /**
     * Release a handle: the Rust side may free the object, and the proxy is
     * forgotten.
     *
     * @param handle the Rust object's handle
     */
    void release(long handle);
}
