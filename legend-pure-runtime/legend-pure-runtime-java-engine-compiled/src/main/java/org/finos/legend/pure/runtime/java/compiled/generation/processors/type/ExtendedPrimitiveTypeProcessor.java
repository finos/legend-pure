// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.type;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class.ClassImplProcessor;

import static org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class.ClassImplProcessor.validate;

public class ExtendedPrimitiveTypeProcessor
{
    public static void processExtendedPrimitiveType(CoreInstance extendedPrimitiveType, ProcessorContext processorContext)
    {
        if (extendedPrimitiveType.getValueForMetaPropertyToMany(M3Properties.constraints).notEmpty())
        {
            ProcessorSupport processorSupport = processorContext.getSupport();
            String _package = JavaPackageAndImportBuilder.buildPackageForPackageableElement(extendedPrimitiveType);
            String imports = JavaPackageAndImportBuilder.buildImports(extendedPrimitiveType);
            String className = JavaPackageAndImportBuilder.buildImplClassNameFromType(extendedPrimitiveType, processorSupport);

            CoreInstance realPrimitiveType = Type.findPrimitiveTypeFromExtendedPrimitiveType(extendedPrimitiveType, processorSupport);

            ListIterable<? extends CoreInstance> params = extendedPrimitiveType.getValueForMetaPropertyToMany(M3Properties.typeVariables);

            MutableList<String> typeVariablesSign = params.collect(ci -> "final " + TypeProcessor.typeToJavaPrimitiveWithMul(Instance.getValueForMetaPropertyToOneResolved(ci, M3Properties.genericType, processorSupport), Instance.getValueForMetaPropertyToOneResolved(ci, M3Properties.multiplicity, processorSupport), false, processorContext) + " _" + Instance.getValueForMetaPropertyToOneResolved(ci, M3Properties.name, processorSupport).getName()).toList();
            MutableList<String> typeVariablesValues = params.collect(ci -> " _" + Instance.getValueForMetaPropertyToOneResolved(ci, M3Properties.name, processorSupport).getName()).toList();

            String stringParams = typeVariablesSign.with("final " + TypeProcessor.typeToJavaObjectSingle(Type.wrapGenericType(realPrimitiveType, processorSupport), false, processorSupport) + " _this").makeString(", ");
            String stringValues = "Lists.mutable.with(" + typeVariablesValues.with("_this").makeString(", ") + ")";

            CoreInstance classGenericType = processorSupport.type_wrapGenericType(extendedPrimitiveType);
            String code = ClassImplProcessor.IMPORTS + imports +
                    "public class " + className + "\n" +
                    "{" +
                    validate(false, extendedPrimitiveType, className, classGenericType, processorContext, processorSupport.class_getSimpleProperties(extendedPrimitiveType), stringParams, stringValues) +
                    "}";

            processorContext.addJavaSource(StringJavaSource.newStringJavaSource(_package, className, code));
        }
    }
}
