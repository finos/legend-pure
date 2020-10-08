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

package org.finos.legend.pure.m4.logs;

public interface PureLogger
{
    /**
     * Log a message.
     *
     * @param message log message
     */
    void log(String message);

    /**
     * Log a formatted message.
     *
     * @param formatString log message format string
     * @param formatArgs   log message format arguments
     */
    void log(String formatString, Object... formatArgs);

    /**
     * Log an error or exception.
     *
     * @param t error or exception
     */
    void log(Throwable t);

    /**
     * Log an error or exception with a message.
     *
     * @param t       error or exception
     * @param message log message
     */
    void log(Throwable t, String message);

    /**
     * Log an error or exception with a formatted message.
     *
     * @param t            error or exception
     * @param formatString log message format string
     * @param formatArgs   log message format arguments
     */
    void log(Throwable t, String formatString, Object... formatArgs);
}
