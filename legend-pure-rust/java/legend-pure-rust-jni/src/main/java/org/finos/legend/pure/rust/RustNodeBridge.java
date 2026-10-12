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

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

/**
 * The JNI-called surface of the node bridge: the class {@code crates/jni}
 * resolves its method ids on, by this name. Rust holds one global reference
 * to it per context. The shared reads and descriptions are inherited from
 * {@link NodeBridge}; this class adds what the JNI crate also calls, the
 * host's primitive factories and the proxies for Rust objects, one per
 * handle while Java reaches it ({@link RustProxies}).
 */
final class RustNodeBridge extends NodeBridge
{
    private final RustProxies proxies;

    RustNodeBridge(RustEngineHost host, RustHeap owner)
    {
        super(host);
        this.proxies = new RustProxies(owner);
    }

    @Override
    public void clear()
    {
        super.clear();
        this.proxies.clear();
    }

    MutableList<Long> proxyHandles()
    {
        return this.proxies.handles();
    }

    void forgetProxy(long handle)
    {
        this.proxies.forget(handle);
    }

    void releaseCollectedProxies()
    {
        this.proxies.releaseCollected();
    }

    CoreInstance newBoolean(boolean value)
    {
        return host().newBoolean(value);
    }

    CoreInstance newInteger(long value)
    {
        return host().newInteger(value);
    }

    CoreInstance newFloat(String text)
    {
        return host().newFloat(text);
    }

    CoreInstance newDecimal(String text)
    {
        return host().newDecimal(text);
    }

    CoreInstance newString(String value)
    {
        return host().newString(value);
    }

    CoreInstance newLatestDate()
    {
        return host().newLatestDate();
    }

    CoreInstance newMap(CoreInstance[] keys, CoreInstance[] values)
    {
        return host().newMap(keys, values);
    }

    CoreInstance rustObject(long handle, String classifierPath)
    {
        return this.proxies.proxy(handle, classifierPath);
    }
}
