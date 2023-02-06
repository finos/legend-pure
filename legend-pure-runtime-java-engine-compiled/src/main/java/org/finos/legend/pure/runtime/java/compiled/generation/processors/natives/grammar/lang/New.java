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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.lang;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.SourceInfoProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class New extends AbstractNative
{
    public New()
    {
        super("new_Class_1__String_1__T_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();

        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorContext.getSupport());

        CoreInstance genericType = Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(0), M3Properties.genericType, M3Properties.typeArguments, processorSupport);
        boolean addGenericType = Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.typeArguments, processorSupport).notEmpty();
        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
        if (_class.equals(processorSupport.package_getByUserPath(M3Paths.Map)))
        {
            return "new PureMap(new org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy(org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureEqualsHashingStrategy.HASHING_STRATEGY))";
        }
        else
        {
            String newId = InstantiationHelpers.manageId(parametersValues, processorSupport);
            return "new " + JavaPackageAndImportBuilder.buildImplClassReferenceFromType(_class) + (addGenericType ? TypeProcessor.buildTypeArgumentsString(genericType, false, processorSupport) : "")
                    + "(\"" + newId + "\")" + (addGenericType ? "._classifierGenericType("
                    + InstantiationHelpers.buildGenericType(genericType, processorContext) + ")" : "") + (_Class.computeConstraintsInHierarchy(_class, processorSupport).isEmpty() ? "" : "._validate(false, " + SourceInfoProcessor.sourceInfoToString(functionExpression.getSourceInformation()) + ", es)")
                    + InstantiationHelpers.manageDefaultValues(this::formatDefaultValueString, Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport), false, processorContext).makeString("");
        }
    }

    @Override
    public String buildBody()
    {
        return "new DefendedPureFunction2<" + FullJavaPaths.Class + ", String, Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object value(" + FullJavaPaths.Class + " clazz, String name, ExecutionSupport es)\n" +
                "            {\n" +
                "                return CoreGen.newObject(clazz, name, Lists.immutable.<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyExpression>empty(), es);\n" +
                "            }\n" +
                "        }";
    }

    private String formatDefaultValueString(String methodName, String value)
    {
        return "._" + methodName + "(" + value + ")";
    }
}
