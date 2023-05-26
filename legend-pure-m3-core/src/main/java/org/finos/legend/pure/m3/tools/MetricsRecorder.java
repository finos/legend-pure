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

package org.finos.legend.pure.m3.tools;

import io.prometheus.client.Collector;
import io.prometheus.client.Gauge;
import io.prometheus.client.Summary;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MetricsRecorder
{
    static final String METRIC_PREFIX = "pure_service";
    static MutableMap<String, Summary> recorded = Maps.mutable.empty();

    static final Gauge currentQueriesBeingExecuted = Gauge.build().name("pure_current_relational_executions")
            .labelNames("host").help("Current relational queries being executed ").register();

    static final Gauge allQueryExecutions = Gauge.build().name("pure_relational_executions")
            .labelNames("host").help("All relational query executions ").register();

    static final Gauge currentExecutions = Gauge.build()
            .name("pure_current_executions")
            .labelNames("host", "type")
            .help("Current executions being handled").register();

    static final Gauge allExecution = Gauge.build()
            .name("pure_all_executions")
            .labelNames("host", "type")
            .help("All Executions.").register();

    public static MutableList<Collector> executionMetrics = Lists.mutable.empty();

    public static InetAddress localMachine;

    static
    {
        try
        {
            localMachine = InetAddress.getLocalHost();
        }
        catch (UnknownHostException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void recordServiceExecution(String serviceId, double duration)
    {
        if (recorded.get(serviceId) == null)
        {
            //create metric
            Summary g = Summary.build().name(generateMetricName(serviceId))
//                    .labelNames("source")
                    .quantile(0.5, 0.05).quantile(0.9, 0.01).quantile(0.99, 0.001)
                    .help("service execution duration observations")
                    .register();
            //save so we don't recreate
            recorded.put(serviceId, g);
            //add observation
            g.observe(duration);

        }
        else
        {
            //add observation
            recorded.get(serviceId).observe(duration);
        }
    }

    public static void incrementRelationalExecutionCounters()
    {
        currentQueriesBeingExecuted.labels(localMachine.getCanonicalHostName()).inc();
        allQueryExecutions.labels(localMachine.getHostName()).inc();
    }

    public static void decrementCurrentRelationalExecutionCounter()
    {
        currentQueriesBeingExecuted.labels(localMachine.getCanonicalHostName()).dec();
    }

    public static void incrementExecutionCount(ExecutionMetricType type)
    {
        allExecution.labels(localMachine.getCanonicalHostName(), type.name).inc();
        currentExecutions.labels(localMachine.getCanonicalHostName(), type.name).inc();
    }

    public static void decrementCurrentExecutionCount(ExecutionMetricType type)
    {
        currentExecutions.labels(localMachine.getCanonicalHostName(), type.name).dec();
    }

    public enum ExecutionMetricType
    {
        EXECUTE("execute"),
        SERVICE_URL("service_url"),
        EXECUTE_IN_DB("execute_in_db"),
        PURE("pure");
        private final String name;

        ExecutionMetricType(String name)
        {
            this.name = name;
        }
    }

    public static String generateMetricName(String servicePattern)
    {
        return METRIC_PREFIX + servicePattern
                .replace("/", "_")
                .replace("-", "_")
                .replace("{", "")
                .replace("}", "");
    }
}
