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

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.bootstrap.generator.M3ToJavaGenerator;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class.ClassProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.measureUnit.UnitProcessor;

public class TypeProcessor
{
    public static String typeToJavaPrimitiveWithMul(CoreInstance genericType, CoreInstance multiplicity, boolean typeParameter, ProcessorContext processorContext)
    {
        if (Multiplicity.isToZeroOrOne(multiplicity))
        {
            return typeToJavaObjectSingleWithMul(genericType, multiplicity, typeParameter, processorContext.getSupport());
        }
        else
        {
            return "RichIterable<? extends " + typeToJavaObjectSingle(genericType, typeParameter, processorContext.getSupport()) + ">";
        }
    }

    public static String typeToJavaObjectWithMul(CoreInstance genericType, CoreInstance multiplicity, ProcessorSupport processorSupport)
    {
        return typeToJavaObjectWithMul(genericType, multiplicity, true, processorSupport);
    }

    public static String typeToJavaObjectWithMul(CoreInstance genericType, CoreInstance multiplicity, boolean typeParam, ProcessorSupport processorSupport)
    {
        if (Multiplicity.isToZeroOrOne(multiplicity))
        {
            return typeToJavaObjectSingle(genericType, typeParam, processorSupport);
        }
        else
        {
            return "RichIterable<? extends " + typeToJavaObjectSingle(genericType, typeParam, processorSupport) + ">";
        }
    }

    public static String typeToJavaObjectSingleWithMul(CoreInstance genericType, CoreInstance multiplicity, ProcessorSupport processorSupport)
    {
        return typeToJavaObjectSingleWithMul(genericType, multiplicity, false, processorSupport);
    }

    public static String typeToJavaObjectSingleWithMul(CoreInstance genericType, CoreInstance multiplicity, boolean typeParam, ProcessorSupport processorSupport)
    {
        return pureTypeToJava(genericType, typeParam, Multiplicity.isToOne(multiplicity, true), processorSupport);
    }

    public static String typeToJavaPrimitiveSingle(CoreInstance genericType, ProcessorSupport processorSupport)
    {
        return pureTypeToJava(genericType, true, true, processorSupport);
    }

    public static String typeToJavaObjectSingle(CoreInstance genericType, boolean typeParam, ProcessorSupport processorSupport)
    {
        return pureTypeToJava(genericType, typeParam, false, processorSupport);
    }

    public static String pureTypeToJava(CoreInstance genericType, boolean typeParam, boolean primitiveIfPossible, ProcessorSupport processorSupport)
    {
        return pureTypeToJava(genericType, typeParam, primitiveIfPossible, true, processorSupport);
    }

    public static String pureTypeToJava(CoreInstance genericType, boolean typeParam, boolean primitiveIfPossible, boolean fullyQualify, ProcessorSupport processorSupport)
    {
        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
        if (rawType == null)
        {
            return typeParam ? GenericType.getTypeParameterName(genericType) : "java.lang.Object";
        }
        if ("FunctionType".equals(processorSupport.getClassifier(rawType).getName()))
        {
            return "java.lang.Object";
        }
        String javaType = pureSystemPathToJava_simpleCases(PackageableElement.getUserPathForPackageableElement(rawType), primitiveIfPossible);
        if (javaType != null)
        {
            return javaType;
        }
        if (processorSupport.instance_instanceOf(rawType, M3Paths.Enumeration))
        {
            return FullJavaPaths.Enum;
        }
        String finalRawTypeSystemPath = fullyQualify || "Package".equals(rawType.getName()) ? fullyQualifiedJavaInterfaceNameForType(rawType) : javaInterfaceForType(rawType);
        if (rawType instanceof Unit)
        {
            return UnitProcessor.convertToJavaCompatibleClassName(finalRawTypeSystemPath) + "_Instance";
        }
        return typeParam ? (finalRawTypeSystemPath + buildTypeArgumentsString(genericType, true, processorSupport)) : finalRawTypeSystemPath;
    }

    public static String pureRawTypeToJava(CoreInstance rawType, boolean primitiveIfPossible, ProcessorSupport processorSupport)
    {
        if (rawType == null)
        {
            return "java.lang.Object";
        }
        if ("FunctionType".equals(processorSupport.getClassifier(rawType).getName()))
        {
            return "java.lang.Object";
        }
        String systemPath = fullyQualifiedJavaInterfaceNameForType(rawType);
        String javaType = pureSystemPathToJava_simpleCases(PackageableElement.getUserPathForPackageableElement(rawType), primitiveIfPossible);
        if (javaType != null)
        {
            return javaType;
        }
        if (processorSupport.instance_instanceOf(rawType, M3Paths.Enumeration))
        {
            return FullJavaPaths.Enum;
        }
        return systemPath;
    }


    public static String javaInterfaceForType(CoreInstance rawType)
    {
        return ClassProcessor.isPlatformClass(rawType) ? fullyQualifiedJavaInterfaceNameForType(rawType) : PackageableElement.getSystemPathForPackageableElement(rawType, "_");
    }

    public static String javaInterfaceNameForType(CoreInstance rawType)
    {
        return ClassProcessor.isPlatformClass(rawType) ? rawType.getName() : PackageableElement.getSystemPathForPackageableElement(rawType, "_");
    }

    public static String fullyQualifiedJavaInterfaceNameForType(final CoreInstance element)
    {
        return ClassProcessor.isPlatformClass(element) ? M3ToJavaGenerator.getFullyQualifiedM3InterfaceForCompiledModel(element) : JavaPackageAndImportBuilder.buildPackageForPackageableElement(element) + "." + PackageableElement.getSystemPathForPackageableElement(element, "_");
    }

    private static String pureSystemPathToJava_simpleCases(String fullUserPath, boolean primitiveIfPossible)
    {
        switch (fullUserPath)
        {
            case M3Paths.Any:
            case M3Paths.Nil:
            {
                return "java.lang.Object";
            }
            case M3Paths.Integer:
            {
                return primitiveIfPossible ? "long" : "java.lang.Long";
            }
            case M3Paths.Float:
            {
                return primitiveIfPossible ? "double" : "java.lang.Double";
            }
            case M3Paths.Decimal:
            {
                return "java.math.BigDecimal";
            }
            case M3Paths.Boolean:
            {
                return primitiveIfPossible ? "boolean" : "java.lang.Boolean";
            }
            case M3Paths.Date:
            {
                return "org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate";
            }
            case M3Paths.StrictDate:
            {
                return "org.finos.legend.pure.m4.coreinstance.primitive.date.StrictDate";
            }
            case M3Paths.DateTime:
            {
                return "org.finos.legend.pure.m4.coreinstance.primitive.date.DateTime";
            }
            case M3Paths.LatestDate:
            {
                return "org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate";
            }
            case M3Paths.String:
            {
                return "java.lang.String";
            }
            case M3Paths.Number:
            {
                return "java.lang.Number";
            }
            case M3Paths.Map:
            {
                return "org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap";
            }
            case M3Paths.Binary:
            {
                return "byte[]";
            }
            case M3Paths.ByteStream:
            {
                return "org.finos.legend.pure.m4.coreinstance.primitive.byteStream.PureByteStream";
            }
            default:
            {
                return null;
            }
        }
    }

    public static String buildTypeArgumentsString(CoreInstance genericType, boolean addExtends, final ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> typeArgs = Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.typeArguments, processorSupport);
        return typeArgs.isEmpty() ? "" : "<" + (addExtends ? "? extends " : "") + typeArgs.collect(arg -> typeToJavaObjectSingle(arg, true, processorSupport)).makeString("," + (addExtends ? "? extends " : "")) + ">";
    }

    public static boolean isJavaPrimitivePossible(CoreInstance genericType, ProcessorSupport processorSupport)
    {
        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
        if (rawType == null)
        {
            return false;
        }

        String rawTypeString = PackageableElement.getUserPathForPackageableElement(rawType);
        return M3Paths.Boolean.equals(rawTypeString) || M3Paths.Float.equals(rawTypeString) || M3Paths.Integer.equals(rawTypeString);
    }

    /**
     * Get default value allowing for Primitive types as per Java spec.
     * https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html
     *
     * @param rawType The Raw Type of an item
     * @return A Java-compatible String
     */
    public static String defaultValue(CoreInstance rawType)
    {
        if (rawType == null)
        {
            return "null";
        }

        switch (PackageableElement.getUserPathForPackageableElement(rawType, "."))
        {
            case M3Paths.Integer:
            {
                return "0L";
            }
            case M3Paths.Float:
            {
                return "0.0d";
            }
            case M3Paths.Boolean:
            {
                return "false";
            }
            default:
            {
                return "null";
            }
        }
    }
}
