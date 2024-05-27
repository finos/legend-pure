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

package org.finos.legend.pure.m4.transaction;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.io.PrintStream;

public class PrintStreamTransactionObserver implements TransactionObserver
{
    private final PrintStream stream;
    private final String prefix;

    public PrintStreamTransactionObserver(PrintStream stream, String prefix)
    {
        this.stream = (stream == null) ? System.out : stream;
        this.prefix = (prefix == null) ? "" : prefix;
    }

    public PrintStreamTransactionObserver(PrintStream stream)
    {
        this(stream, null);
    }

    public PrintStreamTransactionObserver(String prefix)
    {
        this(null, prefix);
    }

    public PrintStreamTransactionObserver()
    {
        this(null, null);
    }

    @Override
    public void added(RichIterable<CoreInstance> instances)
    {
        this.stream.format("%sNew instances: %,d%n", this.prefix, instances.size());
    }

    @Override
    public void modified(RichIterable<CoreInstance> instances)
    {
        this.stream.format("%sModified instances: %,d%n", this.prefix, instances.size());
    }
}
