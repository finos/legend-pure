// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m4.tools;

/**
 * Graph walk filter result, which filters what is returned during iteration and controls how graph traversal proceeds.
 * The default behavior is {@link #ACCEPT_AND_CONTINUE}.
 */
public enum GraphWalkFilterResult
{
    /**
     * Accept for iteration, and continue the walk.
     */
    ACCEPT_AND_CONTINUE(true, true),

    /**
     * Accept for iteration, but do not continue the walk from this point. Note that the walk may continue from other
     * points.
     */
    ACCEPT_AND_STOP(true, false),

    /**
     * Reject for iteration, but continue the walk.
     */
    REJECT_AND_CONTINUE(false, true),

    /**
     * Reject for iteration, and do not continue the walk from this point. Note that the walk may containue from other
     * points.
     */
    REJECT_AND_STOP(false, false);

    private final boolean accept;
    private final boolean cont;

    GraphWalkFilterResult(boolean accept, boolean cont)
    {
        this.accept = accept;
        this.cont = cont;
    }

    /**
     * Should accept for iteration.
     *
     * @return whether to accept for iteration
     */
    public boolean shouldAccept()
    {
        return this.accept;
    }

    /**
     * Should continue the graph walk from this point.
     *
     * @return whether to continue the walk
     */
    public boolean shouldContinue()
    {
        return this.cont;
    }

    public GraphWalkFilterResult conjoin(GraphWalkFilterResult other)
    {
        return get(this.accept && other.accept, this.cont && other.cont);
    }

    public GraphWalkFilterResult disjoin(GraphWalkFilterResult other)
    {
        return get(this.accept || other.accept, this.cont || other.cont);
    }

    public static GraphWalkFilterResult get(boolean accept, boolean cont)
    {
        return accept ? accept(cont) : reject(cont);
    }

    public static GraphWalkFilterResult accept(boolean cont)
    {
        return cont ? ACCEPT_AND_CONTINUE : ACCEPT_AND_STOP;
    }

    public static GraphWalkFilterResult reject(boolean cont)
    {
        return cont ? REJECT_AND_CONTINUE : REJECT_AND_STOP;
    }

    public static GraphWalkFilterResult cont(boolean accept)
    {
        return accept ? ACCEPT_AND_CONTINUE : REJECT_AND_CONTINUE;
    }

    public static GraphWalkFilterResult stop(boolean accept)
    {
        return accept ? ACCEPT_AND_STOP : REJECT_AND_STOP;
    }
}
