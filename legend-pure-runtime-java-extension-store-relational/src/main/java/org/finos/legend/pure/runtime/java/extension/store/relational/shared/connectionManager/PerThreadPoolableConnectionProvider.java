// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.store.relational.shared.connectionManager;

import org.apache.commons.dbcp2.BasicDataSource;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;

public abstract class PerThreadPoolableConnectionProvider
{
    protected final ConcurrentMutableMap<String, Pair<ThreadLocal<PerThreadPoolableConnectionWrapper>, BasicDataSource>> connectionPoolByUser = ConcurrentHashMap.newMap();

    void removePerThreadConnections(String user)
    {
        Pair<ThreadLocal<PerThreadPoolableConnectionWrapper>, BasicDataSource> userConnectionPool = this.connectionPoolByUser.get(user);
        if (userConnectionPool != null)
        {
            ThreadLocal<PerThreadPoolableConnectionWrapper> tlConnectionWrapper = userConnectionPool.getOne();
            tlConnectionWrapper.remove();
        }
    }
}