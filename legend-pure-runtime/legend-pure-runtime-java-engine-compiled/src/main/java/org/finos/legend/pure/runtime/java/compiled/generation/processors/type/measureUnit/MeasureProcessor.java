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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.type.measureUnit;

import org.apache.commons.lang3.StringEscapeUtils;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.AbstractQuantityCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.QuantityCoreInstance;

public class MeasureProcessor
{
    public static void processMeasure(CoreInstance measure, ProcessorContext processorContext)
    {
        String packageName = JavaPackageAndImportBuilder.buildPackageForPackageableElement(measure);
        String measureInterfaceName = JavaPackageAndImportBuilder.buildInterfaceNameFromType(measure);
        processorContext.addJavaSource(buildMeasureInterface(packageName, measureInterfaceName));

        CoreInstance canonicalUnit = measure.getValueForMetaPropertyToOne(M3Properties.canonicalUnit);
        if (canonicalUnit != null)
        {
            processUnit(packageName, measureInterfaceName, canonicalUnit, processorContext);
        }
        measure.getValueForMetaPropertyToMany(M3Properties.nonCanonicalUnits).forEach(unit -> processUnit(packageName, measureInterfaceName, unit, processorContext));
    }

    private static void processUnit(String packageName, String measureInterfaceName, CoreInstance unit, ProcessorContext processorContext)
    {
        String unitInterfaceName = JavaPackageAndImportBuilder.buildInterfaceNameFromType(unit);
        processorContext.addJavaSource(buildUnitInterface(packageName, measureInterfaceName, unitInterfaceName, PackageableElement.getUserPathForPackageableElement(unit)));

        String unitImplClassName = JavaPackageAndImportBuilder.buildImplClassNameFromType(unit);
        processorContext.addJavaSource(buildUnitImplClass(packageName, unitInterfaceName, unitImplClassName));
    }

    private static StringJavaSource buildMeasureInterface(String packageName, String interfaceName)
    {
        String code = "package " + packageName + ";\n" +
                "\n" +
                "import " + QuantityCoreInstance.class.getName() +  ";\n" +
                "\n" +
                "public interface " + interfaceName + " extends " + QuantityCoreInstance.class.getSimpleName() + "\n" +
                "{\n" +
                "    @Override\n" +
                "    " + interfaceName + " copy();\n" +
                "}\n";
        return StringJavaSource.newStringJavaSource(packageName, interfaceName, code);
    }

    private static StringJavaSource buildUnitInterface(String packageName, String measureInterfaceName, String unitInterfaceName, String unitPath)
    {
        String code = "package " + packageName + ";\n" +
                "\n" +
                "public interface " + unitInterfaceName + " extends " + measureInterfaceName + "\n" +
                "{\n" +
                "    String UNIT_PATH = \"" + StringEscapeUtils.escapeJava(unitPath) + "\";\n" +
                "\n" +
                "    @Override\n" +
                "    " + unitInterfaceName + " copy();\n" +
                "}\n";
        return StringJavaSource.newStringJavaSource(packageName, unitInterfaceName, code);
    }

    private static StringJavaSource buildUnitImplClass(String packageName, String unitInterfaceName, String unitClassImplName)
    {
        String code = "package " + packageName + ";\n" +
                "\n" +
                "import " + ExecutionSupport.class.getName() + ";\n" +
                "import " + CompiledExecutionSupport.class.getName() + ";\n" +
                "import " + AbstractQuantityCoreInstance.class.getName() + ";\n" +
                "\n" +
                "public class " + unitClassImplName + " extends " + AbstractQuantityCoreInstance.class.getSimpleName() + " implements " + unitInterfaceName + "\n" +
                "{\n" +
                "    public " + unitClassImplName + "(Number value, " + CompiledExecutionSupport.class.getSimpleName() + " executionSupport)\n" +
                "    {\n" +
                "        super(value, UNIT_PATH, executionSupport);\n" +
                "    }\n" +
                "\n" +
                "    public " + unitClassImplName + "(Number value, " + ExecutionSupport.class.getSimpleName() + " executionSupport)\n" +
                "    {\n" +
                "        super(value, UNIT_PATH, executionSupport);\n" +
                "    }\n" +
                "\n" +
                "    private " + unitClassImplName + "(" + unitClassImplName + " src)\n" +
                "    {\n" +
                "        super(src);\n" +
                "    }\n" +
                "    \n" +
                "    private " + unitClassImplName + "(" + unitClassImplName + " src, boolean wrapped)\n" +
                "    {\n" +
                "        super(src, wrapped);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public String getUnitPath()\n" +
                "    {\n" +
                "        return UNIT_PATH;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public " + unitInterfaceName + " copy()\n" +
                "    {\n" +
                "        return new " + unitClassImplName + "(this);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected " + unitInterfaceName + " unwrap()\n" +
                "    {\n" +
                "        return new " + unitClassImplName + "(this, false);\n" +
                "    }\n" +
                "}\n";
        return StringJavaSource.newStringJavaSource(packageName, unitClassImplName, code);
    }
}
