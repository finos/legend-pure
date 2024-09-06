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
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.SafeAppendable;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class.ClassImplProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class.ClassLazyImplProcessor;
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

    public static <T extends Appendable> T  buildLazyImplClassReferenceFromUserPath(T appendable, String elementPath)
    {
        buildLazyImplClassNameFromUserPath(SafeAppendable.wrap(appendable).append(CODE_GEN_PACKAGE_NAME).append('.'), elementPath);
        return appendable;
    }


    public static <T extends Appendable> T buildLazyImplClassNameFromUserPath(T appendable, String elementPath)
    {
        return buildImplClassNameFromUserPath(appendable, elementPath, ClassLazyImplProcessor.CLASS_LAZYIMPL_SUFFIX);
    }

    public static <T extends Appendable> T buildImplClassReferenceFromUserPath(T appendable, String elementPath)
    {
        buildImplClassNameFromUserPath(SafeAppendable.wrap(appendable).append(CODE_GEN_PACKAGE_NAME).append('.'), elementPath);
        return appendable;
    }

    public static <T extends Appendable> T buildImplClassNameFromUserPath(T appendable, String elementPath)
    {
        return buildImplClassNameFromUserPath(appendable, elementPath, ClassImplProcessor.CLASS_IMPL_SUFFIX);
    }

    // Java classes from Pure types

    public static String buildImplClassReferenceFromType(CoreInstance type)
    {
        return buildImplClassReferenceFromType(new StringBuilder(CODE_GEN_PACKAGE_NAME.length() + 64), type).toString();
    }

    public static String buildImplClassReferenceFromType(CoreInstance type, String suffix)
    {
        return buildImplClassNameFromType(new StringBuilder(CODE_GEN_PACKAGE_NAME).append('.'), type, suffix).toString();
    }

    public static String buildImplClassNameFromType(CoreInstance element)
    {
        return buildImplClassNameFromType(new StringBuilder(64), element).toString();
    }

    public static String buildLazyImplClassReferenceFromType(CoreInstance element)
    {
        return buildLazyImplClassReferenceFromType(new StringBuilder(CODE_GEN_PACKAGE_NAME.length() + 64), element).toString();
    }

    public static String buildLazyImplClassNameFromType(CoreInstance element)
    {
        return buildLazyImplClassNameFromType(new StringBuilder(64), element).toString();
    }

    public static String buildImplClassNameFromType(CoreInstance element, String suffix)
    {
        return buildImplClassNameFromType(new StringBuilder(suffix.length() + 64), element, suffix).toString();
    }


    public static <T extends Appendable> T buildImplClassReferenceFromType(T appendable, CoreInstance element)
    {
        buildImplClassNameFromType(SafeAppendable.wrap(appendable).append(CODE_GEN_PACKAGE_NAME).append('.'), element);
        return appendable;
    }

    public static <T extends Appendable> T buildImplClassNameFromType(T appendable, CoreInstance element)
    {
        return buildImplClassNameFromType(appendable, element, ClassImplProcessor.CLASS_IMPL_SUFFIX);
    }

    public static <T extends Appendable> T  buildLazyImplClassReferenceFromType(T appendable, CoreInstance type)
    {
        buildLazyImplClassNameFromType(SafeAppendable.wrap(appendable).append(CODE_GEN_PACKAGE_NAME).append('.'), type);
        return appendable;
    }

    public static <T extends Appendable> T buildLazyImplClassNameFromType(T appendable, CoreInstance type)
    {
        return buildImplClassNameFromType(appendable, type, ClassLazyImplProcessor.CLASS_LAZYIMPL_SUFFIX);
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

        buildInterfaceNameFromUserPath(SafeAppendable.wrap(appendable).append(CODE_GEN_PACKAGE_NAME).append('.'), elementPath);
        return appendable;
    }

    public static <T extends Appendable> T buildInterfaceNameFromUserPath(T appendable, String elementPath)
    {
        SafeAppendable.wrap(appendable).append("Root_").append(convertUnitName(elementPath.replace("::", "_")));
        return appendable;
    }

    // Java interfaces from Pure types

    public static String buildInterfaceReferenceFromType(CoreInstance type)
    {
        return buildInterfaceReferenceFromType(new StringBuilder(64), type).toString();
    }

    public static String buildInterfaceNameFromType(CoreInstance type)
    {
        return buildInterfaceNameFromType(new StringBuilder(64), type).toString();
    }

    public static <T extends Appendable> T buildInterfaceReferenceFromType(T appendable, CoreInstance type)
    {
        if (ClassProcessor.isPlatformClass(type))
        {
            return M3ToJavaGenerator.appendFullyQualifiedM3InterfaceForCompiledModel(appendable, type);
        }

        buildInterfaceNameFromType(SafeAppendable.wrap(appendable).append(CODE_GEN_PACKAGE_NAME).append('.'), type);
        return appendable;
    }

    public static <T extends Appendable> T buildInterfaceNameFromType(T appendable, CoreInstance type)
    {
        buildClassOrInterfaceNameFromType(SafeAppendable.wrap(appendable), type);
        return appendable;
    }

    // HELPERS

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

    private static <T extends Appendable> T buildImplClassNameFromType(T appendable, CoreInstance type, String suffix)
    {
        buildClassOrInterfaceNameFromType(SafeAppendable.wrap(appendable), type).append(suffix);
        return appendable;
    }

    private static SafeAppendable buildClassOrInterfaceNameFromType(SafeAppendable appendable, CoreInstance type)
    {
        if (isUnitName(type.getName()))
        {
            CoreInstance pkg = type.getValueForMetaPropertyToOne(M3Properties.measure).getValueForMetaPropertyToOne(M3Properties._package);
            if (pkg != null)
            {
                PackageableElement.writeSystemPathForPackageableElement(appendable, pkg, "_").append('_');
            }
            return appendable.append(convertUnitName(type.getName()));
        }
        return PackageableElement.writeSystemPathForPackageableElement(appendable, type, "_");
    }

    private static boolean isUnitName(String name)
    {
        return (name != null) && (name.indexOf('~') != -1);
    }

    private static String convertUnitName(String unitName)
    {
        return unitName.replace('~', '$');
    }
}
