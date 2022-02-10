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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;

public class UnitProcessor
{
    public static RichIterable<CoreInstance> processUnit(CoreInstance unit, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        MutableList<StringJavaSource> classes = processorContext.getClasses();
        MutableSet<CoreInstance> processedUnits = processorContext.getProcessedUnits(org.finos.legend.pure.runtime.java.compiled.generation.processors.type.measureUnit.UnitProcessor.class);
        String _package = JavaPackageAndImportBuilder.buildPackageForPackageableElement(unit);
        String imports = JavaPackageAndImportBuilder.buildImports(unit);

        if (!processedUnits.contains(unit))
        {
            processedUnits.add(unit);
            boolean useJavaInheritance = unit.getValueForMetaPropertyToMany(M3Properties.generalizations).size() == 1;
            CoreInstance genericType = Type.wrapGenericType(unit, null, processorSupport);

            classes.add(UnitInterfaceProcessor.buildInterface(_package, imports, genericType, processorContext, processorSupport, useJavaInheritance));
            classes.add(UnitInstanceInterfaceProcessor.buildInterface(_package, imports, genericType, processorContext, processorSupport, useJavaInheritance));
            classes.add(UnitImplProcessor.buildImplementation(_package, imports, genericType, processorContext, processorSupport, useJavaInheritance));
            classes.add(UnitInstanceImplProcessor.buildImplementation(_package, imports, genericType, processorContext, processorSupport, useJavaInheritance));
        }
        return processedUnits;
    }

    static String typeParameters(CoreInstance _class)
    {
        return _class.getValueForMetaPropertyToMany(M3Properties.typeParameters).collect(i -> PrimitiveUtilities.getStringValue(i.getValueForMetaPropertyToOne(M3Properties.name))).makeString(",");
    }

    public static String convertToJavaCompatibleClassName(String tildeName)
    {
        return tildeName.replace("~", "_Tilde_");
    }
}
