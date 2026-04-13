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

package org.finos.legend.pure.m3.pct.shared.model;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;

import java.util.List;

/**
 * Plain Java model of a PCT manifest JSON file.
 *
 * <p>A manifest bundles the adapter function path and exclusions into a single
 * portable, language-agnostic file that can be consumed by Java (interpreted
 * and compiled), Rust, and any future Pure executor.
 *
 * <pre>{@code
 * {
 *   "adapter": "meta::pure::test::pct::testAdapterForInMemoryExecution_...",
 *   "exclusions": [
 *     { "test": "meta::pure::functions::...", "expectedError": "Assert failure" }
 *   ]
 * }
 * }</pre>
 */
public class PCTManifest
{
    public String adapter;
    public List<PCTManifestExclusion> exclusions;

    /**
     * Single exclusion entry within a manifest.
     */
    public static class PCTManifestExclusion
    {
        public String test;
        public String expectedError;
    }

    /**
     * Build a flat {@code Map<String, String>} from test FQN → expected error message.
     *
     * @return exclusions as a mutable map (empty if no exclusions defined)
     */
    public MutableMap<String, String> toExclusionMap()
    {
        MutableMap<String, String> map = Maps.mutable.empty();
        if (exclusions != null)
        {
            for (PCTManifestExclusion ex : exclusions)
            {
                map.put(ex.test, ex.expectedError);
            }
        }
        return map;
    }
}
