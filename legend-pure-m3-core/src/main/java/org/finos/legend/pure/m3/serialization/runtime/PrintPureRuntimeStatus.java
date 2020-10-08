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

package org.finos.legend.pure.m3.serialization.runtime;

import org.eclipse.collections.api.block.function.Function;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.TimePrinter;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.io.PrintStream;

public class PrintPureRuntimeStatus implements PureRuntimeStatus
{
    private long time;
    private PrintStream printStream;
    private String space="";
    private String tab="  ";

    public PrintPureRuntimeStatus(PrintStream out)
    {
        this.printStream = out;
    }

    @Override
    public void startLoadingAndCompilingCore()
    {
        this.time = System.nanoTime();
        this.printStream.println("Start Core Compilation");
        this.space = this.space + this.tab;
    }

    @Override
    public void finishedLoadingAndCompilingCore()
    {
        this.printStream.println("Finished Core Compilation (" + TimePrinter.makeItHuman(System.nanoTime() - this.time) + ")\n");
        this.space = this.space.substring(0, this.space.length()-this.tab.length());
    }

    @Override
    public void startRuntimeInitialization()
    {
        this.time = System.nanoTime();
        this.printStream.println("Start Runtime Initialization");
        this.space = this.space + this.tab;
    }

    @Override
    public void finishRuntimeInitialization()
    {
        this.printStream.println("Finished Runtime Initialization (" + TimePrinter.makeItHuman(System.nanoTime() - this.time) + ")\n");
        this.space = this.space.substring(0, this.space.length()-this.tab.length());
    }

    @Override
    public void startLoadingAndCompilingSystemFiles()
    {
        this.time = System.nanoTime();
        this.printStream.println("Start System Files Compilation");
        this.space = this.space + this.tab;
    }

    @Override
    public void finishedLoadingAndCompilingSystemFiles()
    {
        this.printStream.println("Finished System Files Compilation (" + TimePrinter.makeItHuman(System.nanoTime() - this.time) + ")\n");
        this.space = this.space.substring(0, this.space.length()-this.tab.length());
    }

    @Override
    public void createOrUpdateMemorySource(String id, String content)
    {
        this.printStream.println(this.space+"Create or update '"+id+"' (Content size:"+content.length()+")");
    }

    private String print(CoreInstance coreInstance)
    {
        ProcessorSupport processorSupport = new M3ProcessorSupport(coreInstance.getRepository());
        if (Instance.instanceOf(coreInstance, M3Paths.InstanceValue, processorSupport))
        {
            return "[InstanceValue: '"+coreInstance.getValueForMetaPropertyToMany(M3Properties.values).collect(new Function<CoreInstance, Object>()
            {
                @Override
                public String valueOf(CoreInstance coreInstance)
                {
                    CoreInstance importStubId = coreInstance.getValueForMetaPropertyToOne(M3Properties.idOrPath);
                    return importStubId == null ? "Lambda" : importStubId.getName();
                }
            }).makeString(", ")+"']";
        }
        if (Instance.instanceOf(coreInstance, M3Paths.FunctionDefinition, processorSupport))
        {
            CoreInstance name = coreInstance.getValueForMetaPropertyToOne(M3Properties.functionName);
            return "[FunctionDefinition: '"+(name==null?"Lambda":name.getName())+"']";
        }
        if (Instance.instanceOf(coreInstance, M3Paths.FunctionExpression, processorSupport))
        {
            CoreInstance functionName = coreInstance.getValueForMetaPropertyToOne(M3Properties.functionName);
            CoreInstance propertyName = coreInstance.getValueForMetaPropertyToOne(M3Properties.propertyName);
            CoreInstance qualifiedPropertyName = coreInstance.getValueForMetaPropertyToOne(M3Properties.qualifiedPropertyName);
            SourceInformation sourceInformation = coreInstance.getSourceInformation();
            return "[FunctionExpression: "+(functionName==null?propertyName==null?qualifiedPropertyName==null?"Unknown":qualifiedPropertyName.getName():propertyName.getName():functionName.getName())+")]";
        }
        if (Instance.instanceOf(coreInstance, M3Paths.GenericType, processorSupport))
        {
            CoreInstance id = coreInstance.getValueForMetaPropertyToOne(M3Properties.rawType).getValueForMetaPropertyToOne(M3Properties.idOrPath);
            return "[GenericType "+id+")]";
        }
        return "[Unknown:'"+coreInstance+"']";
    }

    @Override
    public void modifySource(String sourceId, String code)
    {
        this.printStream.println(this.space+"Modify source '"+ sourceId +"' (size:"+code.length()+")\n");
    }

    @Override
    public void deleteSource(String sourceId)
    {
        this.printStream.println(this.space+"Deleting source '"+ sourceId +"'");
    }

    @Override
    public void moveSource(String sourceId, String destinationId)
    {
        this.printStream.println(this.space + "Moving source '" + sourceId + "' to '" + destinationId + "'");
    }
}
