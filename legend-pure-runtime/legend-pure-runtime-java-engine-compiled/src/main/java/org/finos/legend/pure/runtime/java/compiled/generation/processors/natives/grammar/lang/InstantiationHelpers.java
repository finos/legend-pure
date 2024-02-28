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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.lang;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.MetadataJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;

import java.util.function.BiFunction;

public class InstantiationHelpers
{
    public static String manageKeyValues(CoreInstance genericType, CoreInstance sourceClass, ListIterable<? extends CoreInstance> keyValues, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        return keyValues.collect(keyValue ->
        {
            if (!processorSupport.instance_instanceOf(keyValue, M3Paths.KeyExpression))
            {
                return "";
            }

            boolean add = PrimitiveUtilities.getBooleanValue(Instance.getValueForMetaPropertyToOneResolved(keyValue, M3Properties.add, processorSupport), false);
            ListIterable<String> propertyNames = keyValue.getValueForMetaPropertyToOne(M3Properties.key).getValueForMetaPropertyToMany(M3Properties.values).collect(CoreInstance::getName);

            MutableList<Pair<CoreInstance, CoreInstance>> result = propertyNames.injectInto(Lists.mutable.with(Tuples.pair(sourceClass, null)), (pairs, propertyName) ->
            {
                CoreInstance prop = processorSupport.class_findPropertyUsingGeneralization(pairs.getLast().getOne(), propertyName);
                CoreInstance targetType = Instance.getValueForMetaPropertyToOneResolved(prop.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToMany(M3Properties.typeArguments).get(1), M3Properties.rawType, processorSupport);
                pairs.add(Tuples.pair(targetType, prop));
                return pairs;
            });

            CoreInstance property = result.getLast().getTwo();
            boolean propertyIsToOne = Multiplicity.isToOne(Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.multiplicity, processorSupport), false);

            // Manage possible chain (Hack for compilation)
            if (result.size() > 2)
            {
                property = result.get(1).getTwo();
                return "._" + property.getName() + "((" + TypeProcessor.typeToJavaObjectSingle(property.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToMany(M3Properties.typeArguments).get(1), false, processorContext.getSupport()) + ")null)";
            }
            else
            {
                CoreInstance expression = Instance.getValueForMetaPropertyToOneResolved(keyValue, M3Properties.expression, processorSupport);

                String value = ValueSpecificationProcessor.processValueSpecification(expression, processorContext);
                CoreInstance propertyReturnGenericType = property.getValueForMetaPropertyToOne(M3Properties.genericType);//property.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToMany(M3Properties.typeArguments).get(1);
                if (!GenericType.isGenericTypeConcrete(propertyReturnGenericType))
                {
                    //Sometimes this isn't resolved
                    propertyReturnGenericType = GenericType.resolvePropertyReturnType(genericType, property, processorSupport);
                }

                String valueType = TypeProcessor.pureTypeToJava(propertyReturnGenericType, true, false, processorSupport);

                if ("this".equals(value))
                {
                    CoreInstance expressionRawType = Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToOneResolved(expression, M3Properties.genericType, processorSupport), M3Properties.rawType, processorSupport);
                    value = PackageableElement.getSystemPathForPackageableElement(expressionRawType, "_") + processorContext.getClassImplSuffix() + "." + value;
                }

                CoreInstance expressionMultiplicity = Instance.getValueForMetaPropertyToOneResolved(expression, M3Properties.multiplicity, processorSupport);
                boolean expressionIsToOne = Multiplicity.isToOne(expressionMultiplicity, false);

                String methodName = "_" + propertyNames.getLast();

                boolean isNull = Multiplicity.isToZero(expressionMultiplicity);

                if (propertyIsToOne)
                {
                    if (isNull)
                    {
                        value = "(" + valueType + ")" + value;
                    }
                }
                else
                {
                    if (isNull)
                    {
                        value = "Lists.immutable.<" + valueType + ">empty()";
                    }

                    if (add)
                    {
                        methodName += expressionIsToOne ? "Add" : "AddAll";
                    }
                    else if (!isNull && (Multiplicity.isLowerZero(expressionMultiplicity) || Multiplicity.isToOne(expressionMultiplicity)))
                    {
                        //wrap
                        value = "CompiledSupport.toPureCollection(" + value + ")";
                    }
                }

                return "." + methodName + "(" + value + ")";
            }
        }).makeString("");
    }

    public static ListIterable<String> manageDefaultValues(BiFunction<String, String, String> formatString, CoreInstance sourceClass, boolean doSingleWrap, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        ListIterable<? extends CoreInstance> properties = sourceClass.getValueForMetaPropertyToMany(M3Properties.properties);

        return properties.collect(coreInstance ->
        {
            if (coreInstance.getValueForMetaPropertyToOne(M3Properties.defaultValue) == null)
            {
                return "";
            }

            boolean propertyIsToOne = Multiplicity.isToOne(Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties.multiplicity, processorSupport), false);
            CoreInstance expression = Property.getDefaultValueExpression(Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties.defaultValue, processorSupport));
            String value = ValueSpecificationProcessor.processValueSpecification(expression, processorContext);
            if ("this".equals(value))
            {
                CoreInstance expressionRawType = Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToOneResolved(expression, M3Properties.genericType, processorSupport), M3Properties.rawType, processorSupport);
                value = PackageableElement.getSystemPathForPackageableElement(expressionRawType, "_") + processorContext.getClassImplSuffix() + "." + value;
            }

            CoreInstance expressionMultiplicity = Multiplicity.newMultiplicity(expression.getValueForMetaPropertyToMany(M3Properties.values).size(), processorSupport);

            if ((doSingleWrap || !propertyIsToOne)
                    && (Multiplicity.isLowerZero(expressionMultiplicity) || Multiplicity.isToOne(expressionMultiplicity)))
            {
                //wrap
                value = "CompiledSupport.toPureCollection(" + value + ")";
            }

            return formatString.apply(coreInstance.getName(), value);
        });
    }

    public static String manageId(ListIterable<? extends CoreInstance> parametersValues, ProcessorSupport processorSupport)
    {
        String id = Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.values, processorSupport).getName();
        return "".equals(id) ? "Anonymous_NoCounter" : id;
    }

    public static String buildGenericType(CoreInstance genericType, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        if (GenericType.isGenericTypeConcrete(genericType))
        {
            CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
            ListIterable<? extends CoreInstance> typeArguments = Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.typeArguments, processorSupport);
            String rawTypeStr = "(" + FullJavaPaths.Type + ")((CompiledExecutionSupport)es).getMetadata(\"" + MetadataJavaPaths.buildMetadataKeyFromType(processorSupport.getClassifier(rawType)) + "\", \"" + processorContext.getIdBuilder().buildId(rawType) + "\")";
            String base = "new " + JavaPackageAndImportBuilder.buildImplClassReferenceFromType(processorSupport.getClassifier(genericType)) + "(\"Anonymous_NoCounter\")._rawType(" + rawTypeStr + ")";
            if (typeArguments.notEmpty())
            {
                base += "._typeArguments(Lists.fixedSize.of(" + typeArguments.collect(gt -> buildGenericType(gt, processorContext)).makeString(",") + "))";
            }
            ListIterable<? extends CoreInstance> multiplicityArguments = Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.multiplicityArguments, processorSupport);
            if (multiplicityArguments.notEmpty())
            {
                base += "._multiplicityArguments(Lists.fixedSize.of(" + multiplicityArguments.collect(mult -> buildMultiplicity(mult, processorSupport)).makeString(",") + "))";
            }
            return base;
        }
        else
        {
            CoreInstance typeParameter = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.typeParameter, processorSupport);
            String typeParam = "new org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_TypeParameter_Impl(\"Anonymous_NoCounter\")._name(\"" + typeParameter.getValueForMetaPropertyToOne(M3Properties.name).getName() + "\")";
            return "new " + JavaPackageAndImportBuilder.buildImplClassReferenceFromType(processorSupport.getClassifier(genericType)) + "(\"Anonymous_NoCounter\")._typeParameter(" + typeParam + ")";
        }
    }

    public static String buildMultiplicity(CoreInstance multiplicity, ProcessorSupport processorSupport)
    {
        if (Instance.instanceOf(multiplicity, M3Paths.PackageableMultiplicity, processorSupport))
        {
            return "(" + FullJavaPaths.PackageableMultiplicity + ")((CompiledExecutionSupport)es).getMetadata(\"" + MetadataJavaPaths.PackageableMultiplicity + "\",\"" + PackageableElement.getSystemPathForPackageableElement(multiplicity, "::") + "\")";
        }

        CoreInstance lowerBound = Instance.getValueForMetaPropertyToOneResolved(multiplicity, M3Properties.lowerBound, processorSupport);
        CoreInstance upperBound = Instance.getValueForMetaPropertyToOneResolved(multiplicity, M3Properties.upperBound, processorSupport);
        CoreInstance mulParam = Instance.getValueForMetaPropertyToOneResolved(multiplicity, M3Properties.multiplicityParameter, processorSupport);
        return "new " + FullJavaPaths.Multiplicity_Impl + "(\"Anonymous_NoCounter\")" +
                (lowerBound == null ? "" : "._lowerBound(" + buildMultiplicityValue(lowerBound, processorSupport) + ")") +
                (upperBound == null ? "" : "._upperBound(" + buildMultiplicityValue(upperBound, processorSupport) + ")") +
                (mulParam == null ? "" : "._multiplicityParameter(\"" + mulParam.getName() + "\")");
    }

    public static String buildMultiplicityValue(CoreInstance multiplicityValue, ProcessorSupport processorSupport)
    {
        CoreInstance val = Instance.getValueForMetaPropertyToOneResolved(multiplicityValue, M3Properties.value, processorSupport);
        return "new " + FullJavaPaths.MultiplicityValue_Impl + "(\"Anonymous_NoCounter\")" + (val == null ? "" : "._value(" + val.getName() + "l)");
    }
}
