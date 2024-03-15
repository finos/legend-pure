// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map;

import org.finos.legend.pure.runtime.java.shared.map.PureMapStats;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.MapAdapter;

import java.util.Map;

// PureMap is needed because MutableMap is a subtype of RichIterable
// RichIterable is a synonym of collection in Compiled...

public class PureMap
{
    private final MutableMap map;
    private final PureMapStats stats;

    public PureMap(Map map)
    {
        this(MapAdapter.adapt(map));
    }

    public PureMap(MutableMap map)
    {
        this.map = map;
        this.stats = new PureMapStats();
    }

    public MutableMap getMap()
    {
        return this.map;
    }

    public PureMapStats getStats()
    {
        return this.stats;
    }
}
