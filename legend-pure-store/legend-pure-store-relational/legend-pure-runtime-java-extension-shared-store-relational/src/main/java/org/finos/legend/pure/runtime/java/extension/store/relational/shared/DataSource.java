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

import org.eclipse.collections.impl.block.factory.Comparators;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public final class DataSource
{
    private final String host;
    private final Integer port;
    private final String dataSourceName;
    private final String serverPrincipal;
    private final DriverConfig driverConfig;

    public DataSource(String host, Integer port, String dataSourceName, DriverConfig driverConfig, String serverPrincipal)
    {
        this.host = host;
        this.port = port;
        this.dataSourceName = dataSourceName;
        this.driverConfig = driverConfig;
        this.serverPrincipal = serverPrincipal;
    }

    public DataSource(String host, Integer port, String dataSourceName, DriverConfig driverConfig)
    {
        this.host = host;
        this.port = port;
        this.dataSourceName = dataSourceName;
        this.driverConfig = driverConfig;
        this.serverPrincipal = null;
    }

    public String getHost()
    {
        return this.host;
    }

    public Integer getPort()
    {
        return this.port;
    }

    public String getDataSourceName()
    {
        return this.dataSourceName;
    }

    public DriverConfig getDriverConfig()
    {
        return this.driverConfig;
    }

    public String getServerPrincipal()
    {
        return this.serverPrincipal;
    }

    @Override
    public String toString()
    {
        return "DataSource{" +
                "host='" + this.host + '\'' +
                ", port=" + this.port +
                ", dataSourceName='" + this.dataSourceName + '\'' +
                ", driverName='" + (this.driverConfig == null ? "" : this.driverConfig.getDriverClassName()) + '\'' +
                ", serverPrincipal='" + (this.serverPrincipal == null ? "" : this.serverPrincipal) + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof DataSource))
        {
            return false;
        }

        DataSource that = (DataSource)o;
        return Comparators.nullSafeEquals(this.host, that.host) &&
                Comparators.nullSafeEquals(this.port, that.port) &&
                Comparators.nullSafeEquals(this.dataSourceName, that.dataSourceName) &&
                Comparators.nullSafeEquals(this.driverConfig, that.driverConfig) &&
                Comparators.nullSafeEquals(this.serverPrincipal, that.serverPrincipal);
    }

    @Override
    public int hashCode()
    {
        int result = nullSafeHashCode(this.host);
        result = 31 * result + nullSafeHashCode(this.port);
        result = 31 * result + nullSafeHashCode(this.dataSourceName);
        result = 31 * result + nullSafeHashCode(this.driverConfig);
        result = 31 * result + nullSafeHashCode(this.serverPrincipal);

        return result;
    }

    private int nullSafeHashCode(Object o)
    {
        return (o == null) ? 0 : o.hashCode();
    }

    public static DataSource newDataSource(CoreInstance dataSource, ProcessorSupport processorSupport, DriverConfig driverConfig)
    {
        String host = Instance.getValueForMetaPropertyToOneResolved(dataSource, "host", processorSupport).getName();
        Number port = PrimitiveUtilities.getIntegerValue(Instance.getValueForMetaPropertyToOneResolved(dataSource, "port", processorSupport));
        String dataSourceName = Instance.getValueForMetaPropertyToOneResolved(dataSource, M3Properties.name, processorSupport).getName();
        String serverPrincipal = PrimitiveUtilities.getStringValue(Instance.getValueForMetaPropertyToOneResolved(dataSource, "serverPrincipal", processorSupport), null);

        return new DataSource(host, (Integer)port, dataSourceName, driverConfig, serverPrincipal);
    }

    public static DataSource newDataSource(String host, int port, String dataSourceName, DriverConfig driverConfig)
    {
        return new DataSource(host, port, dataSourceName, driverConfig, null);
    }
}
