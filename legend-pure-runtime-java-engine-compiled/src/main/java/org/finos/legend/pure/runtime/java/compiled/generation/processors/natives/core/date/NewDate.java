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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.date;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.NativeFunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;

public class NewDate extends AbstractNative
{
    public NewDate()
    {
        super("date_Integer_1__Date_1_",
                "date_Integer_1__Integer_1__Date_1_",
                "date_Integer_1__Integer_1__Integer_1__StrictDate_1_",
                "date_Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_",
                "date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_",
                "date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__Number_1__DateTime_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        SourceInformation sourceInformation = functionExpression.getSourceInformation();
        return "CompiledSupport.newDate(" + StringUtils.join(transformedParams, ", ") + ", " + NativeFunctionProcessor.buildM4LineColumnSourceInformation(sourceInformation) + ")";
    }

    @Override
    public String buildBody() {

        return "new SharedPureFunction<Object>()\n" +
                "{\n" +
                "   @Override\n" +
                "   public Object execute(ListIterable vars, final ExecutionSupport es)\n" +
                "   {\n" +
                "       if (vars.size() == 1) {" +
                "           return CompiledSupport.newDate((int) vars.get(0), null);" +
                "       } else if (vars.size() == 2) {" +
                "           return CompiledSupport.newDate((int) vars.get(0), (int) vars.get(1), null);" +
                "       } else if (vars.size() == 3) {" +
                "           return CompiledSupport.newDate((int) vars.get(0), (int) vars.get(1), (int) vars.get(2), null);" +
                "       } else if (vars.size() == 4) {" +
                "           return CompiledSupport.newDate((int) vars.get(0), (int) vars.get(1), (int) vars.get(2), (int) vars.get(3), null);" +
                "       } else if (vars.size() == 5) {" +
                "           return CompiledSupport.newDate((int) vars.get(0), (int) vars.get(1), (int) vars.get(2), (int) vars.get(3), (int) vars.get(4), null);" +
                "       }" +
                "       return CompiledSupport.newDate((int) vars.get(0), (int) vars.get(1), (int) vars.get(2), (int) vars.get(3), (int) vars.get(4), (Number) vars.get(5), null);" +
                "   }\n" +
                "\n}";
    }
}
