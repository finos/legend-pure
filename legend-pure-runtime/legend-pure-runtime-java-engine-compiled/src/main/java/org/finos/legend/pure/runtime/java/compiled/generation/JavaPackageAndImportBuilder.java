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

package org.finos.legend.pure.runtime.java.compiled.generation;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.bootstrap.generator.M3ToJavaGenerator;
import org.finos.legend.pure.m3.coreinstance.M3CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.M3PlatformCoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.SafeAppendable;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class.ClassImplProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class.ClassLazyImplProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class.ClassPeltImplProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class.ClassProcessor;

import java.util.function.Predicate;

/**
 * Centralized logic for building package for Java code
 */
public class JavaPackageAndImportBuilder
{
    private static final String BASE_PACKAGE_NAME = "org.finos.legend.pure";
    private static final String CODE_GEN_PACKAGE_NAME = BASE_PACKAGE_NAME + ".generated";
    private static final String BASE_PACKAGE_FOLDER = CODE_GEN_PACKAGE_NAME.replace('.', '/');

    public static final SetIterable<String> M3_CLASSES = Sets.mutable
            .withAll(M3CoreInstanceFactoryRegistry.ALL_PATHS)
            .withAll(M3PlatformCoreInstanceFactoryRegistry.ALL_PATHS)
            .withAll(CompiledExtensionLoader.extensions().flatCollect(CompiledExtension::getExtraCorePath))
            .asUnmodifiable();

    private JavaPackageAndImportBuilder()
    {
    }

    // JAVA PACKAGES

    public static String getRootPackage()
    {
        return CODE_GEN_PACKAGE_NAME;
    }

    public static String rootPackage()
    {
        return CODE_GEN_PACKAGE_NAME;
    }

    public static String externalizablePackage()
    {
        return BASE_PACKAGE_NAME;
    }

    public static String platformJavaPackage()
    {
        return CODE_GEN_PACKAGE_NAME;
    }

    @Deprecated
    public static String rootPackageFolder()
    {
        return BASE_PACKAGE_FOLDER;
    }

    public static String buildPackageForPackage(CoreInstance pkg)
    {
        return CODE_GEN_PACKAGE_NAME;
    }

    public static String buildPackageForPackageableElement(CoreInstance element)
    {
        return CODE_GEN_PACKAGE_NAME;
    }

    public static String buildPackageFromUserPath(String elementPath)
    {
        return CODE_GEN_PACKAGE_NAME;
    }

    public static String buildPackageFromSystemPath(String elementPath)
    {
        return CODE_GEN_PACKAGE_NAME;
    }

    public static String buildImports(CoreInstance element)
    {
        return "";
    }

    // JAVA CLASSES

    // Java classes from Pure user paths

    public static String buildImplClassReferenceFromUserPath(String elementPath)
    {
        return buildImplClassReferenceFromUserPath(new StringBuilder(elementPath.length() + CODE_GEN_PACKAGE_NAME.length()), elementPath).toString();
    }

    public static String buildImplClassNameFromUserPath(String elementPath)
    {
        return buildImplClassNameFromUserPath(new StringBuilder(elementPath.length()), elementPath).toString();
    }


    public static String buildLazyImplClassReferenceFromUserPath(String elementPath)
    {
        return buildLazyImplClassReferenceFromUserPath(new StringBuilder(elementPath.length() + CODE_GEN_PACKAGE_NAME.length()), elementPath).toString();
    }

    public static String buildLazyImplClassNameFromUserPath(String elementPath)
    {
        return buildLazyImplClassNameFromUserPath(new StringBuilder(elementPath.length()), elementPath).toString();
    }


    public static String buildLazyConcreteElementClassReferenceFromUserPath(String elementPath)
    {
        return buildLazyConcreteElementClassReferenceFromUserPath(new StringBuilder(elementPath.length() + CODE_GEN_PACKAGE_NAME.length()), elementPath).toString();
    }

    public static String buildLazyConcreteElementClassNameFromUserPath(String elementPath)
    {
        return buildLazyConcreteElementClassNameFromUserPath(new StringBuilder(elementPath.length() + CODE_GEN_PACKAGE_NAME.length()), elementPath).toString();
    }

    public static String buildLazyConcreteElementCompClassReferenceFromUserPath(String elementPath)
    {
        return buildLazyConcreteElementCompClassReferenceFromUserPath(new StringBuilder(elementPath.length() + CODE_GEN_PACKAGE_NAME.length()), elementPath).toString();
    }

    public static String buildLazyConcreteElementCompClassNameFromUserPath(String elementPath)
    {
        return buildLazyConcreteElementCompClassNameFromUserPath(new StringBuilder(elementPath.length() + CODE_GEN_PACKAGE_NAME.length()), elementPath).toString();
    }

    public static String buildLazyComponentInstanceClassReferenceFromUserPath(String elementPath)
    {
        return buildLazyComponentInstanceClassReferenceFromUserPath(new StringBuilder(elementPath.length() + CODE_GEN_PACKAGE_NAME.length()), elementPath).toString();
    }

    public static String buildLazyComponentInstanceClassNameFromUserPath(String elementPath)
    {
        return buildLazyComponentInstanceClassNameFromUserPath(new StringBuilder(elementPath.length() + CODE_GEN_PACKAGE_NAME.length()), elementPath).toString();
    }

    public static String buildLazyComponentInstanceCompClassReferenceFromUserPath(String elementPath)
    {
        return buildLazyComponentInstanceCompClassReferenceFromUserPath(new StringBuilder(elementPath.length() + CODE_GEN_PACKAGE_NAME.length()), elementPath).toString();
    }

    public static String buildLazyComponentInstanceCompClassNameFromUserPath(String elementPath)
    {
        return buildLazyComponentInstanceCompClassNameFromUserPath(new StringBuilder(elementPath.length() + CODE_GEN_PACKAGE_NAME.length()), elementPath).toString();
    }

    public static String buildLazyVirtualPackageClassReference()
    {
        return buildLazyVirtualPackageClassReference(new StringBuilder(CODE_GEN_PACKAGE_NAME.length() + M3Paths.Package.length() + ClassPeltImplProcessor.CLASS_VIRTUAL_PACKAGE_SUFFIX.length())).toString();
    }

    public static String buildLazyVirtualPackageClassName()
    {
        return buildLazyVirtualPackageClassName(new StringBuilder(M3Paths.Package.length() + ClassPeltImplProcessor.CLASS_VIRTUAL_PACKAGE_SUFFIX.length())).toString();
    }

    public static <T extends Appendable> T buildLazyImplClassReferenceFromUserPath(T appendable, String elementPath)
    {
        buildLazyImplClassNameFromUserPath(appendCodeGenPackage(appendable), elementPath);
        return appendable;
    }

    public static <T extends Appendable> T buildLazyImplClassNameFromUserPath(T appendable, String elementPath)
    {
        return buildImplClassNameFromUserPath(appendable, elementPath, ClassLazyImplProcessor.CLASS_LAZYIMPL_SUFFIX);
    }


    public static <T extends Appendable> T buildImplClassReferenceFromUserPath(T appendable, String elementPath)
    {
        buildImplClassNameFromUserPath(appendCodeGenPackage(appendable), elementPath);
        return appendable;
    }

    public static <T extends Appendable> T buildImplClassNameFromUserPath(T appendable, String elementPath)
    {
        return buildImplClassNameFromUserPath(appendable, elementPath, ClassImplProcessor.CLASS_IMPL_SUFFIX);
    }


    public static <T extends Appendable> T buildLazyConcreteElementClassReferenceFromUserPath(T appendable, String elementPath)
    {
        buildLazyConcreteElementClassNameFromUserPath(appendCodeGenPackage(appendable), elementPath);
        return appendable;
    }

    public static <T extends Appendable> T buildLazyConcreteElementClassNameFromUserPath(T appendable, String elementPath)
    {
        return buildImplClassNameFromUserPath(appendable, elementPath, ClassPeltImplProcessor.CLASS_LAZY_CONCRETE_SUFFIX);
    }

    public static <T extends Appendable> T buildLazyConcreteElementCompClassReferenceFromUserPath(T appendable, String elementPath)
    {
        buildLazyConcreteElementCompClassNameFromUserPath(appendCodeGenPackage(appendable), elementPath);
        return appendable;
    }

    public static <T extends Appendable> T buildLazyConcreteElementCompClassNameFromUserPath(T appendable, String elementPath)
    {
        return buildImplClassNameFromUserPath(appendable, elementPath, ClassPeltImplProcessor.CLASS_LAZY_CONCRETE_COMP_SUFFIX);
    }

    public static <T extends Appendable> T buildLazyComponentInstanceClassReferenceFromUserPath(T appendable, String elementPath)
    {
        buildLazyComponentInstanceClassNameFromUserPath(appendCodeGenPackage(appendable), elementPath);
        return appendable;
    }

    public static <T extends Appendable> T buildLazyComponentInstanceClassNameFromUserPath(T appendable, String elementPath)
    {
        return buildImplClassNameFromUserPath(appendable, elementPath, ClassPeltImplProcessor.CLASS_LAZY_COMPONENT_SUFFIX);
    }

    public static <T extends Appendable> T buildLazyComponentInstanceCompClassReferenceFromUserPath(T appendable, String elementPath)
    {
        buildLazyComponentInstanceCompClassNameFromUserPath(appendCodeGenPackage(appendable), elementPath);
        return appendable;
    }

    public static <T extends Appendable> T buildLazyComponentInstanceCompClassNameFromUserPath(T appendable, String elementPath)
    {
        return buildImplClassNameFromUserPath(appendable, elementPath, ClassPeltImplProcessor.CLASS_LAZY_COMPONENT_COMP_SUFFIX);
    }

    public static <T extends Appendable> T buildLazyVirtualPackageClassReference(T appendable)
    {
        buildLazyVirtualPackageClassName(appendCodeGenPackage(appendable));
        return appendable;
    }

    public static <T extends Appendable> T buildLazyVirtualPackageClassName(T appendable)
    {
        return buildImplClassNameFromUserPath(appendable, M3Paths.Package, ClassPeltImplProcessor.CLASS_VIRTUAL_PACKAGE_SUFFIX);
    }

    // Java classes from Pure types

    public static String buildImplClassReferenceFromType(CoreInstance type, ProcessorSupport processorSupport)
    {
        return buildImplClassReferenceFromType(new StringBuilder(CODE_GEN_PACKAGE_NAME.length() + 64), type, processorSupport).toString();
    }

    public static String buildImplClassReferenceFromType(CoreInstance type, String suffix, ProcessorSupport processorSupport)
    {
        return buildImplClassNameFromType(new StringBuilder(CODE_GEN_PACKAGE_NAME).append('.'), type, suffix, processorSupport).toString();
    }

    public static String buildImplClassNameFromType(CoreInstance element, ProcessorSupport processorSupport)
    {
        return buildImplClassNameFromType(new StringBuilder(64), element, processorSupport).toString();
    }

    public static String buildLazyImplClassReferenceFromType(CoreInstance element, ProcessorSupport processorSupport)
    {
        return buildLazyImplClassReferenceFromType(new StringBuilder(CODE_GEN_PACKAGE_NAME.length() + 64), element, processorSupport).toString();
    }

    public static String buildLazyImplClassNameFromType(CoreInstance element, ProcessorSupport processorSupport)
    {
        return buildLazyImplClassNameFromType(new StringBuilder(64), element, processorSupport).toString();
    }

    public static String buildImplClassNameFromType(CoreInstance element, String suffix, ProcessorSupport processorSupport)
    {
        return buildImplClassNameFromType(new StringBuilder(suffix.length() + 64), element, suffix, processorSupport).toString();
    }

    public static String buildLazyConcreteElementClassReferenceFromType(CoreInstance type, ProcessorSupport processorSupport)
    {
        return buildLazyConcreteElementClassReferenceFromType(new StringBuilder(64), type, processorSupport).toString();
    }

    public static String buildLazyConcreteElementCompClassReferenceFromType(CoreInstance type, ProcessorSupport processorSupport)
    {
        return buildLazyConcreteElementCompClassReferenceFromType(new StringBuilder(64), type, processorSupport).toString();
    }

    public static String buildLazyConcreteElementClassNameFromType(CoreInstance type, ProcessorSupport processorSupport)
    {
        return buildLazyConcreteElementClassNameFromType(new StringBuilder(64), type, processorSupport).toString();
    }

    public static String buildLazyConcreteElementCompClassNameFromType(CoreInstance type, ProcessorSupport processorSupport)
    {
        return buildLazyConcreteElementCompClassNameFromType(new StringBuilder(64), type, processorSupport).toString();
    }

    public static String buildLazyComponentInstanceClassReferenceFromType(CoreInstance type, ProcessorSupport processorSupport)
    {
        return buildLazyComponentInstanceClassReferenceFromType(new StringBuilder(64), type, processorSupport).toString();
    }

    public static String buildLazyComponentInstanceCompClassReferenceFromType(CoreInstance type, ProcessorSupport processorSupport)
    {
        return buildLazyComponentInstanceCompClassReferenceFromType(new StringBuilder(64), type, processorSupport).toString();
    }

    public static String buildLazyComponentInstanceClassNameFromType(CoreInstance type, ProcessorSupport processorSupport)
    {
        return buildLazyComponentInstanceClassNameFromType(new StringBuilder(64), type, processorSupport).toString();
    }

    public static String buildLazyComponentInstanceCompClassNameFromType(CoreInstance type, ProcessorSupport processorSupport)
    {
        return buildLazyComponentInstanceCompClassNameFromType(new StringBuilder(64), type, processorSupport).toString();
    }


    public static <T extends Appendable> T buildImplClassReferenceFromType(T appendable, CoreInstance element, ProcessorSupport processorSupport)
    {
        buildImplClassNameFromType(appendCodeGenPackage(appendable), element, processorSupport);
        return appendable;
    }

    public static <T extends Appendable> T buildImplClassNameFromType(T appendable, CoreInstance element, ProcessorSupport processorSupport)
    {
        return buildImplClassNameFromType(appendable, element, ClassImplProcessor.CLASS_IMPL_SUFFIX, processorSupport);
    }

    public static <T extends Appendable> T buildLazyImplClassReferenceFromType(T appendable, CoreInstance type, ProcessorSupport processorSupport)
    {
        buildLazyImplClassNameFromType(appendCodeGenPackage(appendable), type, processorSupport);
        return appendable;
    }

    public static <T extends Appendable> T buildLazyImplClassNameFromType(T appendable, CoreInstance type, ProcessorSupport processorSupport)
    {
        return buildImplClassNameFromType(appendable, type, ClassLazyImplProcessor.CLASS_LAZYIMPL_SUFFIX, processorSupport);
    }


    public static <T extends Appendable> T buildLazyConcreteElementClassReferenceFromType(T appendable, CoreInstance type, ProcessorSupport processorSupport)
    {
        buildLazyConcreteElementClassNameFromType(appendCodeGenPackage(appendable), type, processorSupport);
        return appendable;
    }

    public static <T extends Appendable> T buildLazyConcreteElementCompClassReferenceFromType(T appendable, CoreInstance type, ProcessorSupport processorSupport)
    {
        buildLazyConcreteElementCompClassNameFromType(appendCodeGenPackage(appendable), type, processorSupport);
        return appendable;
    }

    public static <T extends Appendable> T buildLazyConcreteElementClassNameFromType(T appendable, CoreInstance type, ProcessorSupport processorSupport)
    {
        return buildImplClassNameFromType(appendable, type, ClassPeltImplProcessor.CLASS_LAZY_CONCRETE_SUFFIX, processorSupport);
    }

    public static <T extends Appendable> T buildLazyConcreteElementCompClassNameFromType(T appendable, CoreInstance type, ProcessorSupport processorSupport)
    {
        return buildImplClassNameFromType(appendable, type, ClassPeltImplProcessor.CLASS_LAZY_CONCRETE_COMP_SUFFIX, processorSupport);
    }

    public static <T extends Appendable> T buildLazyComponentInstanceClassReferenceFromType(T appendable, CoreInstance type, ProcessorSupport processorSupport)
    {
        buildLazyComponentInstanceClassNameFromType(appendCodeGenPackage(appendable), type, processorSupport);
        return appendable;
    }

    public static <T extends Appendable> T buildLazyComponentInstanceCompClassReferenceFromType(T appendable, CoreInstance type, ProcessorSupport processorSupport)
    {
        buildLazyComponentInstanceCompClassNameFromType(appendCodeGenPackage(appendable), type, processorSupport);
        return appendable;
    }

    public static <T extends Appendable> T buildLazyComponentInstanceClassNameFromType(T appendable, CoreInstance type, ProcessorSupport processorSupport)
    {
        return buildImplClassNameFromType(appendable, type, ClassPeltImplProcessor.CLASS_LAZY_COMPONENT_SUFFIX, processorSupport);
    }

    public static <T extends Appendable> T buildLazyComponentInstanceCompClassNameFromType(T appendable, CoreInstance type, ProcessorSupport processorSupport)
    {
        return buildImplClassNameFromType(appendable, type, ClassPeltImplProcessor.CLASS_LAZY_COMPONENT_COMP_SUFFIX, processorSupport);
    }

    // JAVA INTERFACES

    // Java interfaces from Pure user paths

    public static String buildInterfaceReferenceFromUserPath(String elementPath)
    {
        return buildInterfaceReferenceFromUserPath(elementPath, null);
    }

    public static String buildInterfaceReferenceFromUserPath(String elementPath, SetIterable<String> extraSupportedTypes)
    {
        return buildInterfaceReferenceFromUserPath(new StringBuilder(elementPath.length() + CODE_GEN_PACKAGE_NAME.length()), elementPath, (extraSupportedTypes == null) ? null : extraSupportedTypes::contains).toString();
    }

    @Deprecated
    public static String buildPlatformInterfaceReferenceFromUserPath(String elementPath)
    {
        return M3ToJavaGenerator.getFullyQualifiedM3InterfaceForCompiledModel(elementPath);
    }

    /**
     * Deprecated. Use {@link #buildInterfaceNameFromUserPath(String)} instead.
     */
    @Deprecated
    public static String buildInterfaceFromUserPath(String elementPath)
    {
        return buildInterfaceNameFromUserPath(elementPath);
    }

    public static String buildInterfaceNameFromUserPath(String elementPath)
    {
        return buildInterfaceNameFromUserPath(new StringBuilder(elementPath.length()), elementPath).toString();
    }

    public static <T extends Appendable> T buildInterfaceReferenceFromUserPath(T appendable, String elementPath, Predicate<? super String> isExtraSupportedType)
    {
        if (M3_CLASSES.contains(elementPath) || ((isExtraSupportedType != null) && isExtraSupportedType.test(elementPath)))
        {
            return M3ToJavaGenerator.appendFullyQualifiedM3InterfaceForCompiledModel(appendable, elementPath);
        }

        buildInterfaceNameFromUserPath(appendCodeGenPackage(appendable), elementPath);
        return appendable;
    }

    public static <T extends Appendable> T buildInterfaceNameFromUserPath(T appendable, String elementPath)
    {
        SafeAppendable.wrap(appendable).append("Root_").append(convertUnitName(elementPath.replace("::", "_")));
        return appendable;
    }

    // Java interfaces from Pure types

    public static String buildInterfaceReferenceFromType(CoreInstance type, ProcessorSupport processorSupport)
    {
        return buildInterfaceReferenceFromType(new StringBuilder(64), type, processorSupport).toString();
    }

    public static String buildInterfaceNameFromType(CoreInstance type, ProcessorSupport processorSupport)
    {
        return buildInterfaceNameFromType(new StringBuilder(64), type, processorSupport).toString();
    }

    public static <T extends Appendable> T buildInterfaceReferenceFromType(T appendable, CoreInstance type, ProcessorSupport processorSupport)
    {
        if (ClassProcessor.isPlatformClass(type))
        {
            return M3ToJavaGenerator.appendFullyQualifiedM3InterfaceForCompiledModel(appendable, type);
        }

        buildInterfaceNameFromType(appendCodeGenPackage(appendable), type, processorSupport);
        return appendable;
    }

    public static <T extends Appendable> T buildInterfaceNameFromType(T appendable, CoreInstance type, ProcessorSupport processorSupport)
    {
        buildClassOrInterfaceNameFromType(SafeAppendable.wrap(appendable), type, processorSupport);
        return appendable;
    }

    // HELPERS

    private static SafeAppendable appendCodeGenPackage(Appendable appendable)
    {
        return SafeAppendable.wrap(appendable).append(CODE_GEN_PACKAGE_NAME).append('.');
    }

    private static <T extends Appendable> T  buildImplClassNameFromUserPath(T appendable, String elementPath, String suffix)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        if (M3Paths.Package.equals(elementPath))
        {
            safeAppendable.append(elementPath);
        }
        else
        {
            safeAppendable.append("Root_").append(convertUnitName(elementPath.replace("::", "_")));
        }
        safeAppendable.append(suffix);
        return appendable;
    }

    private static <T extends Appendable> T buildImplClassNameFromType(T appendable, CoreInstance type, String suffix, ProcessorSupport processorSupport)
    {
        buildClassOrInterfaceNameFromType(SafeAppendable.wrap(appendable), type, processorSupport).append(suffix);
        return appendable;
    }

    private static SafeAppendable buildClassOrInterfaceNameFromType(SafeAppendable appendable, CoreInstance type, ProcessorSupport processorSupport)
    {
        if (Measure.isUnit(type, processorSupport))
        {
            return buildClassOrInterfaceNameForUnit(appendable, type);
        }
        if (PackageableElement.isPackageableElement(type, processorSupport))
        {
            return PackageableElement.writeSystemPathForPackageableElement(appendable, type, "_");
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    private static SafeAppendable buildClassOrInterfaceNameForUnit(SafeAppendable appendable, CoreInstance unit)
    {
        CoreInstance measure = unit.getValueForMetaPropertyToOne(M3Properties.measure);
        return PackageableElement.writeSystemPathForPackageableElement(appendable, measure, "_").append('$').append(unit.getName());
    }

    private static String convertUnitName(String unitName)
    {
        return unitName.replace('~', '$');
    }
}
