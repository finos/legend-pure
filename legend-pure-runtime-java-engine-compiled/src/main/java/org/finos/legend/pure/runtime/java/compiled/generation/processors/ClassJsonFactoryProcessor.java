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

package org.finos.legend.pure.runtime.java.compiled.generation.processors;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.coreinstance.helper.PropertyTypeHelper;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.generictype.GenericTypeWithXArguments;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class ClassJsonFactoryProcessor
{
    private static final String imports = "import org.eclipse.collections.api.RichIterable;\n" +
            "import org.eclipse.collections.api.set.MutableSet;\n" +
            "import org.eclipse.collections.impl.set.mutable.UnifiedSet;\n" +
            "import org.eclipse.collections.impl.factory.Sets;\n" +
            "import org.eclipse.collections.impl.list.mutable.FastList;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.execution.sourceInformation.PureCompiledExecutionException;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataAccessor;\n" +
            "import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.JsonParserHelper;\n" +
            "import org.finos.legend.pure.m4.exception.PureException;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.*;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.execution.*;\n"+
            "import org.finos.legend.pure.runtime.java.compiled.execution.sourceInformation.*;\n"+
            "import org.finos.legend.pure.m4.coreinstance.SourceInformation;\n" +
            "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n" +
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.*;\n" +
            "import org.finos.legend.pure.m3.exception.PureExecutionException;\n\n";

    /**
     * Generates code for checking the multiplicity of properties extracted from JSON
     *
     * @param getPropertyCode The code for extracting the property from the JSON, e.g. json.get(...)
     * @param property        The property of the Pure class to be compared against
     * @return Java code for running the multiplicity check within the JSON factory
     */
    private static String multiplicityCheckCode(String getPropertyCode, CoreInstance property, ProcessorSupport processorSupport)
    {
        CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.multiplicity, processorSupport);

        return
                "   int lowerBound = " + Multiplicity.multiplicityLowerBoundToInt(multiplicity) + ";\n" +
                        "   int upperBound = " + Multiplicity.multiplicityUpperBoundToInt(multiplicity) + ";\n" +
                        "   JsonParserHelper.multiplicityIsInRange(" + getPropertyCode + ", lowerBound, upperBound, \"" + Multiplicity.print(multiplicity) + "\", si);\n";
    }

    private static String associationCycleConditionCode(String thisPropertyType, String parentClassCode, ProcessorSupport processorSupport, CoreInstance property)
    {
        CoreInstance associationClass = processorSupport.package_getByUserPath(M3Paths.Association);
        CoreInstance propertyOwner = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.owner, processorSupport);

        if (Instance.instanceOf(propertyOwner, associationClass, processorSupport))
        {
            return "!\"" + thisPropertyType + "\".equals(" + parentClassCode + ")";
        }
        return "true";
    }

    public static void processClass(final CoreInstance classGenericType, ProcessorContext processorContext)
    {
        final ProcessorSupport processorSupport = processorContext.getSupport();

        MutableList<StringJavaSource> classes = processorContext.getClasses();
        MutableSet<CoreInstance> processedClasses = processorContext.getProcessedClasses(ClassJsonFactoryProcessor.class);

        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);
        if (!processedClasses.contains(_class) && !processorSupport.instance_instanceOf(_class, M3Paths.DataType))
        {
            processedClasses.add(_class);
            final String className = TypeProcessor.javaInterfaceForType(_class);
            final String thisClassName = className;
            final String userDefinedClassName = PackageableElement.getUserPathForPackageableElement(_class);
            String typeParams = typeParameters(_class);
            // Factory to create objects from Json
            if (shouldGenerate(className))
            {
                classes.add(StringJavaSource.newStringJavaSource(JavaPackageAndImportBuilder.buildPackageForPackageableElement(_class), className + "_JsonFactory", imports +
                        "public class " + className + "_JsonFactory" + (typeParams.isEmpty() ? "" : "<" + typeParams + ">\n") +
                        "{\n" +
                        (
                                "\n\n    public static " + className + " fromJson(final org.json.simple.JSONObject json, final MetadataAccessor md, final ClassLoader classLoader, SourceInformation si, final String typeKey, final boolean failOnUnknownProperties, final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ConstraintsOverride constraintsOverride, final ExecutionSupport es, final String parentClass) {\n" +
                                        "         final " + className + "_Impl result = new " + className + "_Impl(\"Anonymous_NoCounter\");\n" +
                                        "         final String pureClassName=\"" + userDefinedClassName + "\";\n" +
                                        "         MutableSet<String> notFound = UnifiedSet.newSet(json.keySet());\n" +
                                        "         notFound.remove(typeKey);\n"
                        ) +
                        processorSupport.class_getSimpleProperties(_class)
                                .select(new Predicate<CoreInstance>()
                                {
                                    @Override
                                    public boolean accept(CoreInstance property)
                                    {
                                        String name = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.name, processorSupport).getName();
                                        return !M3Properties.elementOverride.equals(name) && !M3Properties.getterOverrideToOne.equals(name)
                                                && !M3Properties.getterOverrideToMany.equals(name)
                                                && !"classifierGenericType".equals(name)
                                                && !M3Properties.hiddenPayload.equals(name)
                                                && !M3Properties.constraintsManager.equals(name);

                                    }
                                })
                                .collect(new Function<CoreInstance, Object>()
                                {
                                    @Override
                                    public Object valueOf(CoreInstance property)
                                    {
                                        CoreInstance resolved = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.name, processorSupport);
                                        String name = resolved.getName();

                                        CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.multiplicity, processorSupport);

                                        CoreInstance returnType = getPropertyResolvedReturnType(classGenericType, property, processorSupport);
                                        String typeObject = TypeProcessor.typeToJavaObjectSingle(returnType, false, processorSupport);

                                        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(returnType, M3Properties.rawType, processorSupport);
                                        String classFullName = rawType == null ? null : PackageableElement.getSystemPathForPackageableElement(rawType, "::");
                                        String classFullUserPath = rawType == null ? null : PackageableElement.getUserPathForPackageableElement(rawType);
                                        String classFullName2 = rawType == null ? null : TypeProcessor.javaInterfaceNameForType(rawType);

                                        boolean isToOne = Multiplicity.isToOne(multiplicity, false);

                                        String assignment =
                                                "if(res != null)\n" +
                                                        "{\n" +
                                                        "result._" + name + (isToOne ? "" : "Add") + "((" + typeObject + ")res);\n" +
                                                        "}\n";

                                        return "\n" +
                                                "           notFound.remove(\"" + name + "\");\n" +
                                                "           if(" + associationCycleConditionCode(classFullName2, "parentClass", processorSupport, property) + ")\n" +
                                                "           {\n" +
                                                "               try\n" +
                                                "               {\n" +
                                                "                   Object propertyValue = json.get(\"" + MilestoningFunctions.getSourceEdgePointPropertyName(resolved.getName()) + "\");\n" +
                                                "                   Class __" + name + "Type = " + typeObject + ".class;\n" +
                                                "                   if (propertyValue instanceof org.json.simple.JSONArray )\n" +
                                                "                   {\n" +
                                                "                       org.json.simple.JSONArray jsonArray = (org.json.simple.JSONArray) propertyValue;\n" +
                                                (shouldPerformMultiplicityChecks(className) ? multiplicityCheckCode("jsonArray", property, processorSupport) : "") +
                                                "                       for (Object jsonElement : jsonArray)\n" +
                                                "                       {\n" +
                                                (shouldGenerate(typeObject) ?
                                                        "                           if (jsonElement instanceof org.json.simple.JSONObject)\n" +
                                                                "                           {\n" +
                                                                "                               org.json.simple.JSONObject jsonObject = (org.json.simple.JSONObject) jsonElement;\n" +
                                                                "                               __" + name + "Type = Pure.fromJsonResolveType(jsonObject, \"" + classFullName + "\", " + typeObject + ".class, md, typeKey, classLoader);\n" +
                                                                "                               Object res = JsonParserHelper.fromJson(jsonElement, __" + name + "Type, \"" + classFullName2 + "\", \"" + classFullUserPath + "\", md, classLoader, si, typeKey, failOnUnknownProperties, constraintsOverride, es, \"" + thisClassName + "\");\n" +
                                                                "                               " + assignment +
                                                                "                           }\n" +
                                                                "                           else\n" +
                                                                "                           {\n"
                                                        :
                                                        "                           {\n"
                                                ) +
                                                "                               Object res = JsonParserHelper.fromJson(jsonElement, __" + name + "Type, \"" + classFullName2 + "\", \"" + classFullUserPath + "\", md, classLoader, si, typeKey, failOnUnknownProperties, constraintsOverride, es, \"" + thisClassName + "\");\n" +
                                                "                               " + assignment +
                                                "                           }\n" +
                                                "                       }\n" +
                                                "                   }\n" +
                                                "                   else\n" +
                                                "                   {\n" +
                                                (shouldPerformMultiplicityChecks(className) ? multiplicityCheckCode("propertyValue", property, processorSupport) : "") +
                                                (shouldGenerate(typeObject) ?
                                                        "                       if (propertyValue instanceof org.json.simple.JSONObject)\n" +
                                                                "                       {\n" +
                                                                "                           org.json.simple.JSONObject jsonObject = (org.json.simple.JSONObject) propertyValue;\n" +
                                                                "                           __" + name + "Type = Pure.fromJsonResolveType(jsonObject , \"" + classFullName + "\", " + typeObject + ".class, md, typeKey, classLoader);\n" +
                                                                "                       }\n" : ""
                                                ) +
                                                "                       Object res = JsonParserHelper.fromJson(propertyValue, __" + name + "Type, \"" + classFullName2 + "\", \"" + classFullUserPath + "\", md, classLoader, si, typeKey, failOnUnknownProperties, constraintsOverride, es, \"" + thisClassName + "\");\n" +
                                                "                       " + assignment +
                                                "                   }\n" +
                                                "               }\n" +
                                                "               catch (PureException e)\n" +
                                                "               {\n" +
                                                "                   throw new PureCompiledExecutionException(si, \"Error populating property '" + name + "' on class '" + userDefinedClassName + "': \" + e.getInfo());\n" +
                                                "               } \n" +
                                                "               catch (Exception e)\n" +
                                                "               {\n" +
                                                "                   e.printStackTrace();\n" +
                                                "                   throw new PureCompiledExecutionException(si, \"Error populating property '" + name + "' on class '" + userDefinedClassName + "'\", e);\n" +
                                                "               }\n" +
                                                "           }\n";
                                    }
                                }).makeString("")
                        + "\n          if(failOnUnknownProperties && !notFound.isEmpty())\n" +
                        "          {\n" +
                        "              String errMsg = (notFound.size() == 1 ? \"Property \" : \"Properties \") + notFound.makeString(\"'\", \"', '\" , \"'\") + \" can't be found in class \" + pureClassName;\n" +
                        "              throw new PureCompiledExecutionException(si, errMsg, null);\n" +
                        "          }\n" +
                        "          result._elementOverride(constraintsOverride);\n" +
                        "          return result;\n" +
                        "    }\n" +
                        "} "));
            }
        }
    }


    private static CoreInstance getPropertyResolvedReturnType(CoreInstance classGenericType, CoreInstance property, ProcessorSupport processorSupport)
    {
        return PropertyTypeHelper.getPropertyResolvedReturnType(classGenericType, property, processorSupport);
    }

    private static String typeParameters(CoreInstance _class)
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

    private static boolean shouldGenerate(String className)
    {
        return className.contains("Root_model_producers_operations_contractFactory_1_5_0");
    }

    private static boolean shouldPerformMultiplicityChecks(String className)
    {
        return !className.contains("Root_model_producers_operations_contractFactory_1_5_0");
    }
}
