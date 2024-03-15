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

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.m3.tools.TimePrinter;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataEventObserver;

import java.io.PrintStream;

public class PrintJavaCompilerEventObserver implements JavaCompilerEventObserver, MetadataEventObserver
{
    private long time;
    private final PrintStream printStream;

    public PrintJavaCompilerEventObserver(PrintStream out)
    {
        this.printStream = out;
    }

    @Override
    public void startSerializingCoreCompiledGraph()
    {
        this.time = System.nanoTime();
        this.printStream.println("Start Serializing Core Java Graph");
    }

    @Override
    public void endSerializingCoreCompiledGraph()
    {
        this.printStream.println("Finished Serializing Core Java Graph (" + TimePrinter.makeItHuman(System.nanoTime() - this.time) + ")\n");
    }

    @Override
    public void startGeneratingJavaFiles(String compileGroup)
    {
        this.time = System.nanoTime();
        this.printStream.println("Start Generating \"" + compileGroup + "\" Java Files");
    }

    @Override
    public void endGeneratingJavaFiles(String compileGroup, RichIterable<StringJavaSource> sources)
    {
        this.printStream.println("Finished Generating " + sources.size() + " \"" + compileGroup + "\" Java Files (" + TimePrinter.makeItHuman(System.nanoTime() - this.time) + ") " + sources.sumOfInt(StringJavaSource::size) + " bytes\n");
    }

    @Override
    public void startCompilingJavaFiles(String compileGroup)
    {
        this.time = System.nanoTime();
        this.printStream.println("Start Compiling \"" + compileGroup + "\" Java Files");
    }

    @Override
    public void endCompilingJavaFiles(String compileGroup)
    {
        this.printStream.println("Finished Compiling \"" + compileGroup + "\" Java Files (" + TimePrinter.makeItHuman(System.nanoTime() - this.time) + ")\n");
    }

    @Override
    public void startSerializingSystemCompiledGraph()
    {
        this.time = System.nanoTime();
        this.printStream.println("Start Serializing System Java Files");
    }

    @Override
    public void endSerializingSystemCompiledGraph(int objectCount, int packageLinkCount)
    {
        this.printStream.println("Finished Serializing System Java Files (" + TimePrinter.makeItHuman(System.nanoTime() - this.time) + ") " + objectCount + " Objects and " + packageLinkCount + " Package Links\n");
    }
}
