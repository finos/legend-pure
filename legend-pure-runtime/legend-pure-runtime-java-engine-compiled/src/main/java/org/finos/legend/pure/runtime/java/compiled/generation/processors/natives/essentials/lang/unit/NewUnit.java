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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.lang.unit;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.QuantityCoreInstance;

public class NewUnit extends AbstractNative
{
    public NewUnit()
    {
        super("newUnit_Unit_1__Number_1__Any_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        CoreInstance unit = Instance.getValueForMetaPropertyToOneResolved(functionExpression.getValueForMetaPropertyToMany(M3Properties.parametersValues).getFirst(), M3Properties.values, processorContext.getSupport());
        return Measure.isUnit(unit, processorContext.getSupport()) ?
               // concretely specified unit: we can generate the Java instantiation directly
               ("new " + JavaPackageAndImportBuilder.buildImplClassReferenceFromType(unit, processorContext.getSupport()) + "(" + transformedParams.get(1) + ", es)") :
               // unit comes from a variable or function expression or something like that: we have to instantiate reflectively
               ("CompiledSupport.newUnitInstance(" + transformedParams.get(0) + ", " + transformedParams.get(1) + ", es)");
    }

    @Override
    public String buildBody()
    {
        return "new DefendedPureFunction2<" + Unit.class.getName() + ", " + Number.class.getName() + ", " + QuantityCoreInstance.class.getSimpleName() + ">()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public " + QuantityCoreInstance.class.getSimpleName() + " value(" + Unit.class.getName() + " unit, " + Number.class.getName() + " value, ExecutionSupport es)\n" +
                "            {\n" +
                "                return CompiledSupport.newUnitInstance(unit, value, es);\n" +
                "            }\n" +
                "        }";
    }
}
