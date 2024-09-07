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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class;

import org.finos.legend.pure.m3.bootstrap.generator.M3ToJavaGenerator;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

/**
 * Extends the M3 core instances and implements qualified properties
 */
public class ClassImplIncrementalCompilationProcessor
{
    public static final String CLASS_IMPL_SUFFIX = "_CompImpl";

    private ClassImplIncrementalCompilationProcessor()
    {
    }

    public static StringJavaSource buildImplementation(String _package, String imports, CoreInstance classGenericType, ProcessorContext processorContext, ProcessorSupport processorSupport)
    {
        processorContext.setClassImplSuffix(CLASS_IMPL_SUFFIX);
        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        String className = JavaPackageAndImportBuilder.buildImplClassNameFromType(_class, CLASS_IMPL_SUFFIX, processorSupport);
        String typeParams = ClassProcessor.typeParameters(_class);
        String typeParamsString = typeParams.isEmpty() ? "" : "<" + typeParams + ">";
        String classNamePlusTypeParams = className + typeParamsString;

        String _extends = M3ToJavaGenerator.getFullyQualifiedM3ImplForCompiledModel(_class);
        String systemPath = PackageableElement.getSystemPathForPackageableElement(_class, "::");
        String interfaceName = TypeProcessor.javaInterfaceForType(_class, processorSupport);
        boolean specialEquals = !_Class.getEqualityKeyProperties(_class, processorContext.getSupport()).isEmpty();

        return StringJavaSource.newStringJavaSource(_package, className, ClassImplProcessor.IMPORTS + ClassImplProcessor.FUNCTION_IMPORTS + imports +
                "import org.finos.legend.pure.m3.coreinstance.BaseM3CoreInstanceFactory;\n" +
                "public class " + classNamePlusTypeParams + " extends " + _extends + (specialEquals ? " implements org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.JavaCompiledCoreInstance" : "") + "\n" +
                "{\n" +
                M3ToJavaGenerator.createClassFactory(className, systemPath) +
                createClassConstructors(className) +
                ClassImplProcessor.buildQualifiedProperties(classGenericType, processorContext, processorSupport) +
                M3ToJavaGenerator.createClassCopyMethod(className, interfaceName + M3ToJavaGenerator.getTypeParams(_class, true)) +
                ClassImplProcessor.buildEquality(classGenericType, CLASS_IMPL_SUFFIX, true, true, false, processorContext, processorSupport) +
                "}");
    }

    public static String createClassConstructors(String className)
    {
        return "    protected " + className + "(String name, SourceInformation sourceInformation, CoreInstance classifier, int internalSyntheticId, ModelRepository repository, boolean persistent)\n" +
                "    {\n" +
                "        super(name, sourceInformation, classifier, internalSyntheticId, repository, persistent);\n" +
                "    }\n" +
                "\n" +
                "    protected " + className + "(" + className + " instance)\n" +
                "    {\n" +
                "        super(instance);\n" +
                "    }\n";
    }
}
