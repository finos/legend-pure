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

import java.util.Arrays;

public abstract class AbstractPureLogger implements PureLogger
{
    @Override
    public void log(String formatString, Object... formatArgs)
    {
        log(formatMessage(formatString, formatArgs));
    }

    @Override
    public void log(Throwable t)
    {
        log(t, null);
    }

    @Override
    public void log(Throwable t, String formatString, Object... formatArgs)
    {
        log(t, formatMessage(formatString, formatArgs));
    }

    protected String formatMessage(String formatString, Object... formatArgs)
    {
        try
        {
            return String.format(formatString, formatArgs);
        }
        catch (Exception e)
        {
            return "Error creating message from format string '" + formatString + "' and format args " + Arrays.toString(formatArgs) + ": " + e;
        }
    }
}
