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

package org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives;

/**
 * Relational Execution Properties
 */
public class RelationalExecutionProperties
{
    private static final int DEFAULT_ROW_LIMIT = 1_000_000;

    private static RelationalProperties INSTANCE = new RelationalProperties();


    private RelationalExecutionProperties()
    {

    }

    public static int getMaxRows()
    {
        return INSTANCE.rowLimit;
    }

    public static boolean shouldThrowIfMaxRowsExceeded()
    {
        return INSTANCE.shouldThrowIfMaxRowsExceeded;
    }

    public static void reset()
    {
        INSTANCE = new RelationalProperties();
    }

    private static class RelationalProperties
    {
        private final int rowLimit;
        private final boolean shouldThrowIfMaxRowsExceeded;

        private RelationalProperties()
        {
            this.rowLimit = Integer.valueOf(System.getProperty("pure.relational.rowLimit", String.valueOf(DEFAULT_ROW_LIMIT)));
            this.shouldThrowIfMaxRowsExceeded = Boolean.valueOf(System.getProperty("pure.relational.shouldThrowIfMaxRowsExceeded", Boolean.TRUE.toString()));
        }
    }
}
