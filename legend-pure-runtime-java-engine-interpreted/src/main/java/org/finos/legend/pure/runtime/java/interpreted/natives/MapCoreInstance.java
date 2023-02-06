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

package org.finos.legend.pure.runtime.java.interpreted.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.simple.SimpleCoreInstance;
import org.finos.legend.pure.runtime.java.shared.map.PureMapStats;
import org.finos.legend.pure.runtime.java.interpreted.EqualityUtilities;

public class MapCoreInstance extends SimpleCoreInstance
{
    private final MutableMap<CoreInstance, CoreInstance> map;
    private final PureMapStats stats;

    public MapCoreInstance(ListIterable<? extends CoreInstance> params, String name, SourceInformation sourceInformation, CoreInstance classifier, int internalSyntheticId, ModelRepository repository, boolean persistent, ProcessorSupport processorSupport)
    {
        super(name, sourceInformation, classifier, internalSyntheticId, repository, persistent);
        this.map = UnifiedMapWithHashingStrategy.newMap(EqualityUtilities.newCoreInstanceHashingStrategy(params, processorSupport));
        this.stats = new PureMapStats();
    }

    public MapCoreInstance(MapCoreInstance map, boolean copyData, ProcessorSupport processorSupport)
    {
        super(map.getName(), null, processorSupport.getClassifier(map), -1, map.getRepository(), false);
        Instance.addValueToProperty(this, M3Properties.classifierGenericType, map.getValueForMetaPropertyToOne(M3Properties.classifierGenericType), processorSupport);
        this.map = copyData ? map.map.clone() : map.map.newEmpty();
        this.stats = copyData ? new PureMapStats(map.getStats()) : new PureMapStats();
    }

    public MutableMap<CoreInstance, CoreInstance> getMap()
    {
        return this.map;
    }

    public PureMapStats getStats(){
        return stats;
    }
}
