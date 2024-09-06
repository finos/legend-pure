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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.bootstrap.generator.M3ToJavaGenerator;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class ClassInterfaceProcessor
{
    private static final String IMPORTS = "import org.eclipse.collections.api.RichIterable;\n" +
            "import org.finos.legend.pure.m4.coreinstance.CoreInstance;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.execution.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.execution.sourceInformation.*;\n" +
            "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n";

    public static StringJavaSource buildInterface(String _package, String imports, CoreInstance classGenericType, ProcessorContext processorContext, ProcessorSupport processorSupport, boolean useJavaInheritance)
    {
        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        String interfaceName = TypeProcessor.javaInterfaceForType(_class);
        String typeParams = ClassProcessor.typeParameters(_class);
        String typeParamsString = typeParams.isEmpty() ? "" : "<" + typeParams + ">";
        String interfaceNamePlusTypeParams = interfaceName + typeParamsString;

        boolean isGetterOverride = M3Paths.GetterOverride.equals(PackageableElement.getUserPathForPackageableElement(_class)); //Type.subTypeOf(_class, processorSupport.package_getByUserPath(M3Paths.GetterOverride), processorSupport);

        String generalization = "";
        boolean hasGeneralization = Instance.getValueForMetaPropertyToManyResolved(_class, M3Properties.generalizations, processorContext.getSupport()).notEmpty();
        if (hasGeneralization)
        {
            ListIterable<String> allGeneralizations = getAllGeneralizations(processorContext, processorSupport, _class, "");
            generalization = ", " + allGeneralizations.makeString(",");
        }

        CoreInstance associationClass = processorSupport.package_getByUserPath(M3Paths.Association);

        return StringJavaSource.newStringJavaSource(_package, interfaceName, IMPORTS + imports + "public interface " + interfaceNamePlusTypeParams + " extends CoreInstance" + generalization + "\n{\n" +
                (isGetterOverride ? "    " + interfaceNamePlusTypeParams + "  __getterOverrideToOneExec(PureFunction2Wrapper f2);\n" +
                        "    " + interfaceNamePlusTypeParams + "  __getterOverrideToManyExec(PureFunction2Wrapper f2);\n" : "") +
                "    " + interfaceNamePlusTypeParams + " _validate(boolean goDeep, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation, final ExecutionSupport es);\n" +
                processorSupport.class_getSimpleProperties(_class).collect(property ->
                {
                    CoreInstance propertyOwner = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.owner, processorSupport);

                    boolean includeGettors = !useJavaInheritance || propertyOwner == _class || Instance.instanceOf(propertyOwner, associationClass, processorSupport);

                    CoreInstance rawReturnType = ClassProcessor.getPropertyUnresolvedReturnType(property, processorSupport);
                    CoreInstance returnType = ClassProcessor.getPropertyResolvedReturnType(classGenericType, property, processorSupport);

                    String name = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.name, processorSupport).getName();
                    CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.multiplicity, processorSupport);
                    boolean makePrimitiveIfPossible = GenericType.isGenericTypeConcrete(rawReturnType) && Multiplicity.isToOne(multiplicity, true);
                    String type = TypeProcessor.pureTypeToJava(returnType, true, makePrimitiveIfPossible, processorSupport);
                    String typeObject = TypeProcessor.pureTypeToJava(returnType, true, false, processorSupport);


                    String reversePropertyName = null;
                    if (Instance.instanceOf(propertyOwner, associationClass, processorSupport))
                    {
                        ListIterable<? extends CoreInstance> associationProperties = Instance.getValueForMetaPropertyToManyResolved(propertyOwner, M3Properties.properties, processorSupport);
                        CoreInstance reverseProperty = (property == associationProperties.get(0)) ? associationProperties.get(1) : associationProperties.get(0);
                        reversePropertyName = Property.getPropertyName(reverseProperty);
                    }

                    if (Multiplicity.isToOne(multiplicity, false))
                    {
                        return (reversePropertyName == null || !includeGettors ? "" :
                                "    void _reverse_" + name + "(" + type + " val);\n" +
                                        "    void _sever_reverse_" + name + "(" + type + " val);\n") +
                                "    " + interfaceNamePlusTypeParams + " _" + name + "(" + type + " val);\n" +
                                "    " + interfaceNamePlusTypeParams + " _" + name + "(RichIterable<? extends " + typeObject + "> val);\n" +
                                "    " + interfaceNamePlusTypeParams + " _" + name + "Remove();\n" +
                                (includeGettors ? "    " + type + " _" + name + "();\n" : "");
                    }
                    else
                    {
                        return (reversePropertyName == null || !includeGettors ? "" :
                                "    void _reverse_" + name + "(" + type + " val);\n" +
                                        "    void _sever_reverse_" + name + "(" + type + " val);\n") +
                                "    " + interfaceNamePlusTypeParams + " _" + name + "(RichIterable<? extends " + typeObject + "> val);\n" +
                                "    " + interfaceNamePlusTypeParams + " _" + name + "Add(" + typeObject + " val);\n" +
                                "    " + interfaceNamePlusTypeParams + " _" + name + "AddAll(RichIterable<? extends " + typeObject + "> val);\n" +
                                "    " + interfaceNamePlusTypeParams + " _" + name + "Remove();\n" +
                                (includeGettors ? "    RichIterable<? extends " + typeObject + "> _" + name + "();\n" : "");
                    }
                }).makeString("") +
                "    " + M3ToJavaGenerator.createInterfaceCopyMethod(interfaceName, typeParamsString) +
                LazyIterate.concatenate((Iterable<CoreInstance>) Instance.getValueForMetaPropertyToManyResolved(_class, M3Properties.qualifiedProperties, processorContext.getSupport()), (Iterable<CoreInstance>) Instance.getValueForMetaPropertyToManyResolved(_class, M3Properties.qualifiedPropertiesFromAssociations, processorContext.getSupport())).collect(qp -> "    " + FunctionProcessor.functionSignature(qp, false, false, true, "", processorContext, true) + ";\n").makeString("") +
                "}");

    }

    static ListIterable<String> getAllGeneralizations(ProcessorContext processorContext, ProcessorSupport processorSupport, CoreInstance _class, String suffix)
    {
        MutableList<String> allGeneralizations = Lists.mutable.empty();
        for (CoreInstance oneGeneralization : Instance.getValueForMetaPropertyToManyResolved(_class, M3Properties.generalizations, processorContext.getSupport()))
        {
            CoreInstance generalGenericType = Instance.getValueForMetaPropertyToOneResolved(oneGeneralization, M3Properties.general, processorSupport);
            String typeArgs = generalGenericType.getValueForMetaPropertyToMany(M3Properties.typeArguments).collect(ci -> TypeProcessor.typeToJavaObjectSingle(ci, true, processorContext.getSupport())).makeString(",");
            allGeneralizations.add(typeName(processorSupport, suffix, generalGenericType, typeArgs));
        }
        return allGeneralizations;
    }

    private static String typeName(ProcessorSupport processorSupport, String suffix, CoreInstance generalGenericType, String typeArgs)
    {
        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(generalGenericType, M3Properties.rawType, processorSupport);
        String typeArgsString = typeArgs.isEmpty() ? "" : "<" + typeArgs + ">";
        if (suffix.isEmpty())
        {
            return TypeProcessor.javaInterfaceForType(rawType) + typeArgsString;
        }
        else
        {
            return PackageableElement.getSystemPathForPackageableElement(rawType, "_") + suffix + typeArgsString;
        }
    }

}
