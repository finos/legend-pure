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

package org.finos.legend.pure.runtime.java.compiled.statelistener;

import org.finos.legend.pure.m3.statelistener.ExecutionActivityListener;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SL4JExecutionActivityListener implements ExecutionActivityListener
{
    private final Logger logger = LoggerFactory.getLogger("Pure_SQL_logger");

    @Override
    public void relationalActivityCompleted(String dbHost, Integer dbPort, String dbName, String dbType, String sql, String planInfo, Long executionTime, Long sqlGenerationTime, Long connectionAcquisitionTime)
    {
        this.logger.info("{\"threadId\":{},\"sql\":\"{}\",\"db\":{\"dbHost\":\"{}\",\"dbPort\":{},\"dbName\":\"{}\",\"dbType\":\"{}\"},\"sqlGenInMillis\":{},\"execInMillis\":{}}",
                    Thread.currentThread().getId(),
                    JSONValue.escape(sql),
                    JSONValue.escape(dbHost),
                    dbPort,
                    JSONValue.escape(dbName),
                    JSONValue.escape(dbType),
                    sqlGenerationTime/1000000,
                    executionTime/1000000);
    }

    @Override
    public void routingActivityCompleted(Long routingTimeInNanoSeconds)
    {
    }
}
