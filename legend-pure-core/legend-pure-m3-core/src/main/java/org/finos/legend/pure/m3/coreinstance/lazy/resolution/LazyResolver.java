// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.coreinstance.lazy.resolution;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This class provides a thread-safe lazy resolution mechanism. The given {@link Supplier} will be called at most once
 * to obtain the value, and subsequent calls to {@link #get()} will return the cached value.
 *
 * @param <T> value type
 */
public class LazyResolver<T> implements Supplier<T>
{
    private volatile Supplier<T> supplier;
    private volatile T value;

    private LazyResolver(Supplier<T> supplier)
    {
        this.supplier = Objects.requireNonNull(supplier);
    }

    @Override
    public T get()
    {
        if (this.supplier != null)
        {
            synchronized (this)
            {
                Supplier<T> local = this.supplier;
                if (local != null)
                {
                    T result = this.value = local.get();
                    this.supplier = null;
                    return result;
                }
            }
        }
        return this.value;
    }

    /**
     * Return whether the value has been resolved. Once the value has been resolved, this method will always return
     * true. Generally, this method should be called before calling {@link #getResolvedValue()}.
     *
     * @return whether the value has been resolved
     */
    public boolean isResolved()
    {
        return this.supplier == null;
    }

    /**
     * Get the value if it has already been resolved; returns null otherwise. This method differs from {@link #get()} in
     * that it does not attempt to resolve the value if it has not already been resolved. Generally, this method should
     * be used in conjunction with {@link #isResolved()}, and should only be called if that method returns true.
     *
     * @return the resolved value, or null if not yet resolved
     */
    public T getResolvedValue()
    {
        return this.value;
    }

    /**
     * If the value has been resolved, perform the given action with the resolved value and return true; otherwise,
     * do nothing and return false.
     *
     * @param action action to perform if the value has been resolved
     * @return whether the action was performed
     */
    public boolean ifResolved(Consumer<? super T> action)
    {
        if (this.supplier == null)
        {
            action.accept(this.value);
            return true;
        }
        return false;
    }

    public static <T> LazyResolver<T> fromSupplier(Supplier<T> supplier)
    {
        return (supplier instanceof LazyResolver) ? (LazyResolver<T>) supplier : new LazyResolver<>(supplier);
    }
}
