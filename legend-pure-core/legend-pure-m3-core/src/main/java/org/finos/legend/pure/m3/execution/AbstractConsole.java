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

package org.finos.legend.pure.m3.execution;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

import java.io.PrintStream;

public abstract class AbstractConsole implements Console
{
    private final MutableList<String> lines = Lists.mutable.empty();
    private PrintStream printStream = System.out;
    private boolean isEnabled = true;
    private boolean isConsole = true;
    private boolean bufferLines = false;

    @Override
    public String getLine(int lineNb)
    {
        if (!isEnabled())
        {
            throw new RuntimeException("Console is currently disabled");
        }
        return this.lines.get(lineNb);
    }

    @Override
    public void print(Object content)
    {
        if (isEnabled())
        {
            String line = getContentString(content);
            if (!(line == null) && !line.isEmpty())
            {
                if (this.bufferLines)
                {
                    this.lines.add(line);
                }
                this.printStream.print(line);
            }
        }
    }

    public void enableBufferLines()
    {
        this.bufferLines = true;
    }


    @Override
    public void setPrintStream(PrintStream printStream)
    {
        this.printStream = printStream;
        this.isConsole = false;
    }

    @Override
    public boolean isConsole()
    {
        return this.isConsole;
    }

    @Override
    public void setConsole(boolean b)
    {
        this.isConsole = b;
    }

    @Override
    public void clear()
    {
        this.lines.clear();
    }

    @Override
    public boolean isEnabled()
    {
        return this.isEnabled;
    }

    @Override
    public void enable()
    {
        this.isEnabled = true;
    }

    @Override
    public void disable()
    {
        this.isEnabled = false;
    }

    protected abstract String getContentString(Object content);
}
