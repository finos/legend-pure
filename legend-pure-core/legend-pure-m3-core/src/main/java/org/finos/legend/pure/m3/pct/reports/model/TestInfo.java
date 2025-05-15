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

package org.finos.legend.pure.m3.pct.reports.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;

public class TestInfo
{
    public final String testName;
    public final boolean success;
    public final String errorMessage;
    public final Set<String> qualifiers;

    @JsonCreator
    public TestInfo(@JsonProperty("testName") String testName, @JsonProperty("success") boolean success, @JsonProperty("errorMessage") String errorMessage, @JsonProperty("qualifiers") Set<String> qualifiers)
    {
        this.testName = testName;
        this.success = success;
        this.errorMessage = errorMessage;
        this.qualifiers = qualifiers;
    }
}
