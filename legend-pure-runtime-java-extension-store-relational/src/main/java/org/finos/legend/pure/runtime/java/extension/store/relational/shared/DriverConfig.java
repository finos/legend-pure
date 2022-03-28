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

package org.finos.legend.pure.runtime.java.extension.store.relational.shared;

import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;

public interface DriverConfig
{
    String getDriverClassName();

    String buildDriverConnectionUrl(String login, String password, String authToken, String hostIP, String hostPort, String hostName);

    String buildDriverConnectionUrl(String login, String password, String authToken, String hostIP, String hostPort, String hostName, String verifyParam);

    String buildDriverConnectionUrl(String login, String password, String authToken, String schema, Iterable<? extends ObjectIntPair<String>> hostPortPairs);

    MapIterable<DataSourceProperty, String> getDatasourceProperties();

    MapIterable<String, String> getConnectionProperties();
}
