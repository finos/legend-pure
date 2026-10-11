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
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;

/**
 * What the bridge needs from the Java engine it serves: the graph's
 * {@link ProcessorSupport}, the property names of a node, and the engine's
 * own representation of each primitive the Rust evaluator hands back.
 */
public interface RustEngineHost
{
    ProcessorSupport processorSupport();

    /**
     * The property names a node holds, as the engine can tell them.
     *
     * @param node a graph node that is neither a primitive nor a Rust object
     * @return the names, in no particular order
     */
    ListIterable<String> propertyKeys(CoreInstance node);

    /**
     * The values a lambda captured from the scope it was created in, as the
     * engine holds them.
     *
     * @param lambda a lambda node
     * @param name   the open variable's name
     * @return {@code null} when the node is not a lambda carrying its context; empty when it holds no such variable
     */
    ListIterable<? extends CoreInstance> capturedValues(CoreInstance lambda, String name);

    /**
     * The lambda definition a lambda value stands for: a key, equal for every
     * value of one lambda (the engine may create a new object per evaluation,
     * wrapping the lambda with the scope it was evaluated in) and unique to
     * that lambda, which the Rust side caches the lowered lambda under. The
     * value itself is then read for its captured values only.
     *
     * @param lambda a graph node classified by {@code LambdaFunction}
     * @return the definition key, or {@code null} when the engine cannot name one (nothing is cached)
     */
    Object lambdaDefinition(CoreInstance lambda);

    CoreInstance newBoolean(boolean value);

    CoreInstance newInteger(long value);

    CoreInstance newFloat(String text);

    CoreInstance newDecimal(String text);

    CoreInstance newString(String value);

    CoreInstance newDate(PureDate date);

    CoreInstance newLatestDate();

    CoreInstance newStrictTime(PureStrictTime time);

    /**
     * The engine's own map over entries the Rust side materialised, each key
     * and value as this host represents it. The Rust map carries no type
     * arguments, so the map's generic type is {@code Map} with none.
     *
     * @param keys   the keys, parallel to {@code values}
     * @param values one value per key
     * @return the map as a graph value of this engine
     */
    CoreInstance newMap(CoreInstance[] keys, CoreInstance[] values);
}
