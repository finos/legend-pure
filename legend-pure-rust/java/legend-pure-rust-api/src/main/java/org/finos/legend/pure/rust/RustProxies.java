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
import org.eclipse.collections.api.list.MutableList;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * The proxies of one evaluator's Rust objects: one {@link RustCoreInstance}
 * per handle for as long as anything on the Java side reaches it. A proxy is
 * held weakly, so the Rust object behind it is released once the host engine
 * lets go of it: the interpreted engine reads a result through its proxy and
 * drops it without closing it, and until released the Rust object stays alive
 * with every host node it holds. A collected proxy's handle is released by
 * {@link #releaseCollected()}, which the evaluator calls on its own thread at
 * the start of a call; a proxy closed explicitly is released at once.
 */
public final class RustProxies
{
    private final RustHeap owner;
    private final Map<Long, Proxy> proxies = new HashMap<>();
    private final ReferenceQueue<RustCoreInstance> collected = new ReferenceQueue<>();

    /**
     * @param owner the evaluator whose handles these are; it releases them
     */
    public RustProxies(RustHeap owner)
    {
        this.owner = owner;
    }

    /**
     * The proxy of a handle: the one Java still reaches, or a new one.
     *
     * @param handle         the Rust object's handle
     * @param classifierPath its classifier path as the Rust side spells it
     * @return the proxy
     */
    public RustCoreInstance proxy(long handle, String classifierPath)
    {
        Proxy reference = this.proxies.get(handle);
        RustCoreInstance proxy = (reference == null) ? null : reference.get();
        if (proxy == null)
        {
            proxy = new RustCoreInstance(this.owner, handle, classifierPath);
            this.proxies.put(handle, new Proxy(proxy, handle, this.collected));
        }
        return proxy;
    }

    /**
     * Forget a handle the owner released.
     *
     * @param handle the handle
     */
    public void forget(long handle)
    {
        this.proxies.remove(handle);
    }

    /**
     * Every handle not released yet, its proxy collected or not.
     *
     * @return the handles
     */
    public MutableList<Long> handles()
    {
        return Lists.mutable.withAll(this.proxies.keySet());
    }

    /**
     * Forget every proxy, once the owner released every handle.
     */
    public void clear()
    {
        this.proxies.clear();
        while (this.collected.poll() != null)
        {
            // already released with the rest
        }
    }

    /**
     * Release, through the owner, the handle of every proxy collected since
     * the last call. A handle that crossed again after its proxy was
     * collected has a newer proxy and is kept.
     */
    public void releaseCollected()
    {
        for (Reference<? extends RustCoreInstance> reference = this.collected.poll(); reference != null; reference = this.collected.poll())
        {
            long handle = ((Proxy) reference).handle;
            if (this.proxies.get(handle) == reference)
            {
                this.owner.release(handle);
            }
        }
    }

    private static final class Proxy extends WeakReference<RustCoreInstance>
    {
        private final long handle;

        private Proxy(RustCoreInstance proxy, long handle, ReferenceQueue<RustCoreInstance> queue)
        {
            super(proxy, queue);
            this.handle = handle;
        }
    }
}
