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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * The evaluators a native keeps, each bound to a scope (the interpreted
 * engine's runtime, a compiled execution support) and created on the first
 * call under it. A scope is held weakly and found by identity, so the holder
 * never keeps one alive; the evaluator of a scope the garbage collector
 * reclaimed is closed on the holder's next call, on the calling thread. A call
 * that fails with a {@link PureRustException} other than a typed
 * {@link PureRustEvaluationException} (a panic) discards its evaluator: it is
 * closed, and the next call under that scope creates another. Evaluators are
 * closed before the evaluator that replaces them is created.
 *
 * @param <S> the scope
 * @param <E> the evaluator
 */
public final class RustEvaluators<S, E extends RustEvaluator>
{
    private final Function<? super S, ? extends E> open;
    private final boolean latestOnly;
    private final List<Bound<S, E>> bound = new ArrayList<>();

    private RustEvaluators(Function<? super S, ? extends E> open, boolean latestOnly)
    {
        this.open = open;
        this.latestOnly = latestOnly;
    }

    /**
     * One evaluator at a time, for the scope of the latest call: a call under
     * another scope closes it. An interpreted native's, one per engine, bound
     * to the engine's current runtime.
     *
     * @param open a new evaluator bound to a scope
     * @param <S>  the scope
     * @param <E>  the evaluator
     * @return an empty holder
     */
    public static <S, E extends RustEvaluator> RustEvaluators<S, E> latest(Function<? super S, ? extends E> open)
    {
        return new RustEvaluators<>(open, true);
    }

    /**
     * One evaluator per scope the garbage collector has not reclaimed. A
     * compiled native's, one per execution support. The evaluator {@code open}
     * returns must not reach its scope, or the scope is never reclaimed.
     *
     * @param open a new evaluator bound to a scope
     * @param <S>  the scope
     * @param <E>  the evaluator
     * @return an empty holder
     */
    public static <S, E extends RustEvaluator> RustEvaluators<S, E> perScope(Function<? super S, ? extends E> open)
    {
        return new RustEvaluators<>(open, false);
    }

    /**
     * Run a call on the scope's evaluator.
     *
     * @param scope the scope, never {@code null}
     * @param call  what to do with the evaluator
     * @param <T>   the result
     * @return what the call returned
     */
    public <T> T call(S scope, Function<? super E, ? extends T> call)
    {
        E evaluator = evaluator(scope);
        try
        {
            return call.apply(evaluator);
        }
        catch (PureRustEvaluationException e)
        {
            throw e;
        }
        catch (PureRustException e)
        {
            discard(evaluator);
            throw e;
        }
    }

    /**
     * Evaluators held, open or not yet closed for a reclaimed scope.
     *
     * @return the count
     */
    public synchronized int size()
    {
        return this.bound.size();
    }

    private synchronized E evaluator(S scope)
    {
        Objects.requireNonNull(scope, "scope");
        E found = null;
        List<E> stale = new ArrayList<>();
        Iterator<Bound<S, E>> entries = this.bound.iterator();
        while (entries.hasNext())
        {
            Bound<S, E> entry = entries.next();
            S held = entry.scope.get();
            if (held == scope)
            {
                found = entry.evaluator;
            }
            else if ((held == null) || this.latestOnly)
            {
                entries.remove();
                stale.add(entry.evaluator);
            }
        }
        for (E evaluator : stale)
        {
            evaluator.close();
        }
        if (found == null)
        {
            found = this.open.apply(scope);
            this.bound.add(new Bound<>(scope, found));
        }
        return found;
    }

    private synchronized void discard(E poisoned)
    {
        this.bound.removeIf(entry -> entry.evaluator == poisoned);
        poisoned.close();
    }

    private static final class Bound<S, E>
    {
        private final WeakReference<S> scope;
        private final E evaluator;

        private Bound(S scope, E evaluator)
        {
            this.scope = new WeakReference<>(scope);
            this.evaluator = evaluator;
        }
    }
}
