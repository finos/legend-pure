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

import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m3.bootstrap.generator.M3ToJavaGenerator;
import org.finos.legend.pure.m3.coreinstance.M3CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.M3PlatformCoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class.ClassImplProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class.ClassLazyImplProcessor;

/**
 * Centralized logic for building package for Java code
 */
public class JavaPackageAndImportBuilder
{
    private static final String BASE_PACKAGE_NAME = "org.finos.legend.pure";
    private static final String CODE_GEN_PACKAGE_NAME = BASE_PACKAGE_NAME + ".generated";
    private static final String CODE_GEN_PACKAGE_PREFIX = CODE_GEN_PACKAGE_NAME + ".";
    private static final String BASE_PACKAGE_FOLDER = CODE_GEN_PACKAGE_NAME.replace('.', '/');

    public final static SetIterable<String> M3_CLASSES = Sets.mutable
            .withAll(M3CoreInstanceFactoryRegistry.ALL_PATHS)
            .withAll(M3PlatformCoreInstanceFactoryRegistry.ALL_PATHS)
            .withAll(CompiledExtensionLoader.extensions().flatCollect(CompiledExtension::getExtraCorePath));

    private JavaPackageAndImportBuilder()
    {
    }

    public static String getRootPackage()
    {
        return CODE_GEN_PACKAGE_NAME;
    }

    public static String buildPackageForPackage(CoreInstance element)
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

    public static String buildImplClassReferenceFromUserPath(String elementPath)
    {
        return CODE_GEN_PACKAGE_NAME + "." + buildImplClassNameFromUserPath(elementPath);
    }

    public static String buildImplClassNameFromUserPath(String elementPath)
    {
        return buildImplClassNameFromUserPath(elementPath, ClassImplProcessor.CLASS_IMPL_SUFFIX);
    }

    public static String buildLazyImplClassReferenceFromUserPath(String elementPath)
    {
        return CODE_GEN_PACKAGE_NAME + "." + buildImplClassNameFromUserPath(elementPath, ClassLazyImplProcessor.CLASS_LAZYIMPL_SUFFIX);
    }

    private static String buildImplClassNameFromUserPath(String elementPath, String suffix)
    {
        return (M3Paths.Package.equals(elementPath) ? "" : "Root_") + elementPath.replace("::", "_") + suffix;
    }

    public static String buildImplClassReferenceFromType(CoreInstance element)
    {
        return CODE_GEN_PACKAGE_NAME + "." + buildImplClassNameFromType(element);
    }

    public static String buildImplClassReferenceFromType(CoreInstance element, String suffix)
    {
        return CODE_GEN_PACKAGE_NAME + "." + buildImplClassNameFromType(element, suffix);
    }

    public static String buildImplClassNameFromType(CoreInstance element)
    {
        return buildImplClassNameFromType(element, ClassImplProcessor.CLASS_IMPL_SUFFIX);
    }

    public static String buildLazyImplClassReferenceFromType(CoreInstance element)
    {
        return CODE_GEN_PACKAGE_NAME + "." + buildLazyImplClassNameFromType(element);
    }

    public static String buildLazyImplClassNameFromType(CoreInstance element)
    {
        return PackageableElement.getSystemPathForPackageableElement(element, "_") + ClassLazyImplProcessor.CLASS_LAZYIMPL_SUFFIX;
    }

    public static String buildImplClassNameFromType(CoreInstance element, String suffix)
    {
        return PackageableElement.getSystemPathForPackageableElement(element, "_") + suffix;
    }

    public static String buildImplUnitInstanceClassNameFromType(CoreInstance element)
    {
        return buildImplUnitInstanceClassNameFromType(element, ClassImplProcessor.CLASS_IMPL_SUFFIX);
    }

    public static String buildImplUnitInstanceClassNameFromType(CoreInstance element, String suffix)
    {
        return PackageableElement.getSystemPathForPackageableElement(element, "_") + "_Instance" + suffix;
    }

    public static String buildInterfaceReferenceFromUserPath(String elementPath)
    {
        return buildInterfaceReferenceFromUserPath(elementPath, null);
    }

    public static String buildInterfaceReferenceFromUserPath(String elementPath, SetIterable<String> extraSupportedTypes)
    {
        if (M3_CLASSES.contains(elementPath) || (extraSupportedTypes != null && extraSupportedTypes.contains(elementPath)))
        {
            return buildPlatformInterfaceReferenceFromUserPath(elementPath);
        }
        else
        {
            return CODE_GEN_PACKAGE_NAME + "." + buildInterfaceFromUserPath(elementPath);
        }
    }

    public static String buildPlatformInterfaceReferenceFromUserPath(String elementPath)
    {
        return M3ToJavaGenerator.getFullyQualifiedM3InterfaceForCompiledModel(elementPath);
    }

    public static String buildInterfaceFromUserPath(String elementPath)
    {
        return "Root_" + elementPath.replace("::", "_");
    }

    public static String platformJavaPackage()
    {
        return CODE_GEN_PACKAGE_NAME;
    }

    public static String rootPackage()
    {
        return CODE_GEN_PACKAGE_NAME;
    }

    public static String rootPackageFolder()
    {
        return BASE_PACKAGE_FOLDER;
    }

    public static String externalizablePackage()
    {
        return BASE_PACKAGE_NAME;
    }

    public static String buildImports(CoreInstance element)
    {
        return "";
    }
}
