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

package org.finos.legend.pure.m3.statelistener;

import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.stack.mutable.ArrayStack;
import org.finos.legend.pure.m3.tools.TimeTracker;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class StatsStateListener implements M3M4StateListener
{
    public MutableStack<TimeTracker> trackers = ArrayStack.newStack();
    public MutableStack<String> tab = ArrayStack.newStack();
    public String offset = "  ";

    public StatsStateListener()
    {
        this.tab.push("");
    }

    @Override
    public void startInit()
    {
        String tab = this.tab.peek();
        System.out.println(tab+"Starting init");
        this.trackers.push(new TimeTracker("Init"));
        this.tab.push(tab+this.offset);
    }

    @Override
    public void finishedInit()
    {
        this.tab.pop();
        System.out.println(this.tab.peek()+"Finished init in "+new TimeTracker("Init").diff(this.trackers.pop()));
    }

    @Override
    public void startPostProcessingGraph()
    {
        String tab = this.tab.peek();
        System.out.println(tab+"Starting Post Processing");
        this.trackers.push(new TimeTracker("PostProcessing"));
        this.tab.push(tab+this.offset);
    }

    @Override
    public void finishedPostProcessingGraph()
    {
        this.tab.pop();
        System.out.println(this.tab.peek()+"Finished Post Processing in "+new TimeTracker("PostProcessing").diff(this.trackers.pop()));
    }

    @Override
    public void startValidation()
    {
        String tab = this.tab.peek();
        System.out.println(tab+"Starting Validation");
        this.trackers.push(new TimeTracker("Validation"));
        this.tab.push(tab+this.offset);
    }

    @Override
    public void finishedValidation()
    {
        this.tab.pop();
        System.out.println(this.tab.peek()+"Finished Validation in "+new TimeTracker("Validation").diff(this.trackers.pop()));
    }

    @Override
    public void startProcessingIncludes()
    {
        String tab = this.tab.peek();
        System.out.println(tab+"Starting processing excludes");
        this.trackers.push(new TimeTracker("Excludes"));
        this.tab.push(tab+this.offset);
    }

    @Override
    public void finishedProcessingIncludes(SetIterable<? extends CoreInstance> coreImports)
    {
        this.tab.pop();
        System.out.println(this.tab.peek()+"Finished processing excludes (total:"+coreImports.size()+") "+new TimeTracker("Excludes").diff(this.trackers.pop()));
    }

    @Override
    public void startParsingM4(String fileLocation)
    {
        String tab = this.tab.peek();
        System.out.println(tab+"Starting parsing M4 ("+this.cut(fileLocation)+")");
        this.trackers.push(new TimeTracker("M4"));
        this.tab.push(tab+this.offset);
    }

    @Override
    public void finishedParsingM4(String fileLocation)
    {
        this.tab.pop();
        System.out.println(this.tab.peek()+"Finished parsing M4 ("+this.cut(fileLocation)+") in "+new TimeTracker("M4").diff(this.trackers.pop()));
    }

    @Override
    public void startParsingClassM4(String fileLocation)
    {
    }

    @Override
    public void finishedParsingClassM4(String fileLocation)
    {
    }

    @Override
    public void startParsingM3(String fileLocation)
    {
        String tab = this.tab.peek();
        this.trackers.push(new TimeTracker("M3"));
        System.out.println(tab + "Start parsing M3 (" + this.cut(fileLocation) + ")");
        this.tab.push(tab+this.offset);
    }

    @Override
    public void finishedParsingM3(String fileLocation)
    {
        this.tab.pop();
        System.out.println(this.tab.peek()+"Finished parsing M3 ("+this.cut(fileLocation)+") in "+new TimeTracker("M3").diff(this.trackers.pop()));
    }

    @Override
    public void startRepositorySimpleValidation()
    {
        String tab = this.tab.peek();
        this.trackers.push(new TimeTracker("RepoValidation"));
        System.out.println(tab + "Start Repo Simple Validation");
        this.tab.push(tab+this.offset);
    }

    @Override
    public void finishedRepositorySimpleValidation(SetIterable<? extends CoreInstance> visitedNodes)
    {
        this.tab.pop();
        System.out.println(this.tab.peek()+"Finished Repo Simple Validation ("+visitedNodes.size()+" visited nodes) in "+new TimeTracker("RepoValidation").diff(this.trackers.pop()));
    }

    private String cut(String src)
    {
        return src.substring(0, Math.min(src.length(), 30));
    }
}

