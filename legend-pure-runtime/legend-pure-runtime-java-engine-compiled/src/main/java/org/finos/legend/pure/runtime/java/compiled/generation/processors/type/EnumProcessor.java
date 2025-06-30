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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.type;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.bootstrap.generator.M3ToJavaGenerator;
import org.finos.legend.pure.m3.coreinstance.lazy.PrimitiveValueResolver;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.serialization.compiler.element.ElementBuilder;
import org.finos.legend.pure.m3.serialization.compiler.element.InstanceData;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReference;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolver;
import org.finos.legend.pure.m3.tools.JavaTools;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;

import java.util.function.IntFunction;

public class EnumProcessor
{
    public static final String ENUM_CLASS_NAME = "PureEnum";
    public static final String ENUM_LAZY_CLASS_NAME = ENUM_CLASS_NAME + "_LazyImpl";
    public static final String ENUM_LAZY_COMPONENT_CLASS_NAME = ENUM_CLASS_NAME + "_LazyComponent";

    public static StringJavaSource processEnum()
    {
        String className = ENUM_CLASS_NAME;
        String superClassName = JavaPackageAndImportBuilder.buildImplClassNameFromUserPath(M3Paths.Enum);
        return StringJavaSource.newStringJavaSource(JavaPackageAndImportBuilder.rootPackage(), className,
                "import " + JavaPackageAndImportBuilder.buildPackageFromUserPath(M3Paths.Enum) + ".*;\n" +
                        "public class " + className + " extends " + superClassName + " implements Comparable<" + FullJavaPaths.Enum + ">" +
                        "{\n" +
                        "    private String fullSystemPath;\n" +
                        "    \n" +
                        "    " + className + "(String id, String enumerationFullName)\n" +
                        "    {\n" +
                        "        super(id);\n" +
                        "        this._name = id;\n" +
                        "        this.fullSystemPath = \"Root::\" + enumerationFullName;\n" +
                        "    }\n" +
                        M3ToJavaGenerator.enumToStringAndCompareOverrides(FullJavaPaths.Enum) +
                        "    @Override\n" +
                        "    public String getFullSystemPath()\n" +
                        "    {\n" +
                        "         return this.fullSystemPath;\n" +
                        "    }\n" +
                        "}\n");
    }

    public static StringJavaSource processEnumLazy()
    {
        String className = ENUM_LAZY_CLASS_NAME;
        String superClassName =  JavaPackageAndImportBuilder.buildInterfaceNameFromUserPath(M3Paths.Enum) + "_LazyImpl";
        return StringJavaSource.newStringJavaSource(JavaPackageAndImportBuilder.rootPackage(), className,
                "import " + JavaPackageAndImportBuilder.buildPackageFromUserPath(M3Paths.Enum) + ".*;\n" +
                "import org.finos.legend.pure.m4.coreinstance.SourceInformation;\n" +
                "import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy;\n" +
                "import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;\n" +
                "import org.eclipse.collections.api.map.ImmutableMap;\n" +
                "\n" +
                "public class " + className + " extends " + superClassName + " implements Comparable<" + FullJavaPaths.Enum + ">" +
                "{\n" +
                "    private final String fullSystemPath;\n" +
                "    " + className + "(Obj obj, MetadataLazy metadataLazy)\n" +
                "    {\n" +
                "        super(obj, metadataLazy);\n" +
                "        this.fullSystemPath = \"Root::\" + obj.getClassifier();\n" +
                "    }\n" +
                "    @Override\n" +
                "    public int compareTo(" + FullJavaPaths.Enum + " o)\n" +
                "    {\n" +
                "        return this.getName().compareTo(o.getName());\n" +
                "    }\n" +
                "    @Override\n" +
                "    public String getFullSystemPath()\n" +
                "    {\n" +
                "         return this.fullSystemPath;\n" +
                "    }\n" +
                "}\n");
    }

    public static StringJavaSource processEnumLazyComponent()
    {
        String className = ENUM_LAZY_COMPONENT_CLASS_NAME;
        String javaPackage = JavaPackageAndImportBuilder.rootPackage();
        MutableList<String> imports = Lists.mutable.with(
                BackReference.class.getName(),
                CoreInstance.class.getName(),
                ElementBuilder.class.getName(),
                Enum.class.getName(),
                InstanceData.class.getName(),
                IntFunction.class.getName(),
                ListIterable.class.getName(),
                ModelRepository.class.getName(),
                PrimitiveValueResolver.class.getName(),
                ReferenceIdResolver.class.getName()
        );
        StringBuilder builder = new StringBuilder("package ").append(javaPackage).append(";\n\n");
        if (imports.notEmpty())
        {
            JavaTools.sortReduceAndPrintImports(builder, imports).append('\n');
        }
        builder.append("public class ").append(className).append(" extends ");
        JavaPackageAndImportBuilder.buildLazyComponentInstanceClassNameFromUserPath(builder, M3Paths.Enum).append(" implements Comparable<").append(Enum.class.getSimpleName()).append(">\n");
        builder.append("{\n");
        builder.append("    private final String fullSystemPath;\n");
        builder.append("\n");
        builder.append("    public ").append(className).append("(ModelRepository repository, InstanceData instanceData, ListIterable<? extends BackReference> backReferences, ReferenceIdResolver referenceIdResolver, IntFunction<? extends CoreInstance> internalIdResolver, PrimitiveValueResolver primitiveValueResolver, ElementBuilder elementBuilder)\n");
        builder.append("    {\n");
        builder.append("        super(repository, instanceData, backReferences, referenceIdResolver, internalIdResolver, primitiveValueResolver, elementBuilder);\n");
        builder.append("        this.fullSystemPath = \"Root::\" + instanceData.getClassifierPath();\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    public ").append(className).append("(").append(className).append(" source)\n");
        builder.append("    {\n");
        builder.append("        super(source);\n");
        builder.append("        this.fullSystemPath = source.fullSystemPath;\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    @Override\n");
        builder.append("    public int compareTo(").append(Enum.class.getSimpleName()).append(" other)\n");
        builder.append("    {\n");
        builder.append("        return getName().compareTo(other.getName());\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    @Override\n");
        builder.append("    public String getFullSystemPath()\n");
        builder.append("    {\n");
        builder.append("         return this.fullSystemPath;\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    @Override\n");
        builder.append("    public ").append(Enum.class.getSimpleName()).append(" copy()\n");
        builder.append("    {\n");
        builder.append("        return new ").append(className).append("(this);\n");
        builder.append("    }\n");
        builder.append("}\n");
        return StringJavaSource.newStringJavaSource(javaPackage, className, builder.toString());
    }
}
