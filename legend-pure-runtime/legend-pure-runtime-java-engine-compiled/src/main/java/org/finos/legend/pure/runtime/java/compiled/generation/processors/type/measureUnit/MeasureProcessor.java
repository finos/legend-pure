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
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;

public class MeasureProcessor
{
    static String typeParameters(CoreInstance _class)
    {
        return _class.getValueForMetaPropertyToMany(M3Properties.typeParameters).collect(new Function<CoreInstance, String>()
        {
            @Override
            public String valueOf(CoreInstance coreInstance)
            {
                return coreInstance.getValueForMetaPropertyToOne(M3Properties.name).getName();
            }
        }).makeString(",");
    }

    public static RichIterable<CoreInstance> processMeasure(final CoreInstance measure, final ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        MutableList<StringJavaSource> classes = processorContext.getClasses();
        MutableSet<CoreInstance> processedMeasures = processorContext.getProcessedMeasures(org.finos.legend.pure.runtime.java.compiled.generation.processors.type.measureUnit.MeasureProcessor.class);
        String _package = JavaPackageAndImportBuilder.buildPackageForPackageableElement(measure);
        String imports = JavaPackageAndImportBuilder.buildImports(measure);

        if (!processedMeasures.contains(measure))
        {
            processedMeasures.add(measure);
            boolean useJavaInheritance = measure.getValueForMetaPropertyToMany(M3Properties.generalizations).size() == 1;
            CoreInstance genericType = Type.wrapGenericType(measure, null, processorSupport);

            classes.add(MeasureInterfaceProcessor.buildInterface(_package, imports, genericType, processorContext, processorSupport, useJavaInheritance));
            classes.add(MeasureImplProcessor.buildImplementation(_package, imports, genericType, processorContext, processorSupport, useJavaInheritance));
        }
        return processedMeasures;
    }

}
