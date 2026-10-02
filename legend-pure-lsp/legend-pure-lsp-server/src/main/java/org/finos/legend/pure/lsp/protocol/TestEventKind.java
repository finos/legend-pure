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

package org.finos.legend.pure.lsp.protocol;

/**
 * Kind of a streamed {@link TestEvent}. The SUITE_* pair brackets each package node and nests like
 * the TestCollection tree being walked, mapping onto IntelliJ's nested
 * testSuiteStarted/testSuiteFinished service messages.
 */
public enum TestEventKind
{
    RUN_STARTED("runStarted"),
    SUITE_STARTED("suiteStarted"),
    TEST_STARTED("testStarted"),
    TEST_FINISHED("testFinished"),
    SUITE_FINISHED("suiteFinished"),
    RUN_FINISHED("runFinished");

    private final String protocolValue;

    TestEventKind(String protocolValue)
    {
        this.protocolValue = protocolValue;
    }

    public String getProtocolValue()
    {
        return this.protocolValue;
    }
}
