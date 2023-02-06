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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.basics.meta;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class InstanceOf extends AbstractNative
{
    public InstanceOf()
    {
        super("instanceOf_Any_1__Type_1__Boolean_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        CoreInstance typeExpression = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport).get(1);
        if (Instance.instanceOf(typeExpression, M3Paths.InstanceValue, processorSupport))
        {
            CoreInstance type = Instance.getValueForMetaPropertyToOneResolved(typeExpression, M3Properties.values, processorSupport);
            if (Instance.instanceOf(type, M3Paths.Enumeration, processorSupport))
            {
                String typeSystemPath = PackageableElement.getSystemPathForPackageableElement(type);
                return "Pure.instanceOfEnumeration(" + transformedParams.get(0) + ", \"" + typeSystemPath + "\")";
            }

            String theClass = TypeProcessor.typeToJavaObjectSingle(Type.wrapGenericType(type, processorSupport), false, processorSupport);
            return theClass + ".class.isInstance(" + transformedParams.get(0) + ")";
        }
        else
        {
            return "Pure.instanceOf(" + transformedParams.get(0) + "," + transformedParams.get(1) + ", es)";
        }

    }
}