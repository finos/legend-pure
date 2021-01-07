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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.core.lang;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
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

public class InstantiationHelpers
{
    public static String manageKeyValues(final CoreInstance genericType, final CoreInstance sourceClass, ListIterable<? extends CoreInstance> keyValues, final ProcessorContext processorContext)
    {
        final ProcessorSupport processorSupport = processorContext.getSupport();
        return keyValues.collect(new Function<CoreInstance, String>()
        {
            @Override
            public String valueOf(CoreInstance coreInstance)
            {
                if (processorContext.getSupport().instance_instanceOf(coreInstance, M3Paths.KeyExpression))
                {
                    CoreInstance addObj = Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties.add, processorSupport);
                    boolean add = addObj == null?false:Boolean.valueOf(addObj.getName());
                    ListIterable<String> propertyNames = coreInstance.getValueForMetaPropertyToOne(M3Properties.key).getValueForMetaPropertyToMany(M3Properties.values).collect(new Function<CoreInstance, String>()
                    {
                        @Override
                        public String valueOf(CoreInstance coreInstance)
                        {
                            return coreInstance.getName();
                        }
                    });

                    MutableList<Pair<CoreInstance, CoreInstance>> result = propertyNames.injectInto(FastList.newListWith(Tuples.pair(sourceClass, (CoreInstance) null)), new Function2<MutableList<Pair<CoreInstance, CoreInstance>>, String, MutableList<Pair<CoreInstance, CoreInstance>>>()
                    {
                        @Override
                        public MutableList<Pair<CoreInstance, CoreInstance>> value(MutableList<Pair<CoreInstance, CoreInstance>> coreInstanceObjectPair, String s)
                        {
                            CoreInstance prop = processorSupport.class_findPropertyUsingGeneralization(coreInstanceObjectPair.getLast().getOne(), s);
                            CoreInstance targetType = Instance.getValueForMetaPropertyToOneResolved(prop.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToMany(M3Properties.typeArguments).get(1), M3Properties.rawType, processorSupport);
                            coreInstanceObjectPair.add(Tuples.pair(targetType, prop));
                            return coreInstanceObjectPair;
                        }
                    });

                    CoreInstance property =  result.getLast().getTwo();
                    boolean propertyIsToOne = Multiplicity.isToOne(Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.multiplicity, processorSupport), false);

                    // Manage possible chain (Hack for compilation)
                    if (result.size() > 2)
                    {
                        property = result.get(1).getTwo();
                        return "._"+property.getName()+"(("+ TypeProcessor.typeToJavaObjectSingle(property.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToMany(M3Properties.typeArguments).get(1), false, processorContext.getSupport())+")null)";
                    }
                    else
                    {
                        CoreInstance expression = Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties.expression, processorSupport);

                        String value = ValueSpecificationProcessor.processValueSpecification(expression, processorContext);
                        CoreInstance propertyReturnGenericType = property.getValueForMetaPropertyToOne(M3Properties.genericType);//property.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToMany(M3Properties.typeArguments).get(1);
                        if (!GenericType.isGenericTypeConcrete(propertyReturnGenericType, processorSupport))
                        {
                            //Sometimes this isn't resolved
                            propertyReturnGenericType = GenericType.resolvePropertyReturnType(genericType, property, processorSupport);
                        }

                        String valueType = TypeProcessor.pureTypeToJava(propertyReturnGenericType, true, false, processorSupport);

                        if("this".equals(value))
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
                }
                else
                {
                    return "";
                }
            }
        }).makeString("");
    }

    public static ListIterable<String> manageDefaultValues(final Function2<String, String, String> formatString, final CoreInstance sourceClass, ListIterable<String> propertyNames, boolean doSingleWrap, final ProcessorContext processorContext)
    {
        final ProcessorSupport processorSupport = processorContext.getSupport();
        ListIterable<? extends CoreInstance> properties = sourceClass.getValueForMetaPropertyToMany(M3Properties.properties);

        return properties.collect(new Function<CoreInstance, String>()
        {
            @Override
            public String valueOf(CoreInstance coreInstance)
            {

                if (!propertyNames.contains(coreInstance.getName()) && coreInstance.getValueForMetaPropertyToOne(M3Properties.defaultValue) != null)
                {
                    boolean propertyIsToOne = Multiplicity.isToOne(Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties.multiplicity, processorSupport), false);

                    CoreInstance defaultValue = Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties.defaultValue, processorSupport);

                    CoreInstance expression = Property.getDefaultValueExpression(defaultValue);

                    String value = ValueSpecificationProcessor.processValueSpecification(Property.getDefaultValueExpression(defaultValue), processorContext);

                    if("this".equals(value))
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
                }
                else
                {
                    return "";
                }
            }
        });
    }

    public static String manageId(ListIterable<? extends CoreInstance> parametersValues, ProcessorSupport processorSupport)
    {
        String id = Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.values, processorSupport).getName();
        return "".equals(id) ? "Anonymous_NoCounter" : id;
    }

    public static String buildGenericType(CoreInstance genericType, final ProcessorSupport processorSupport)
    {
        if (GenericType.isGenericTypeConcrete(genericType, processorSupport))
        {
            CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
            ListIterable<? extends CoreInstance> typeArguments = Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.typeArguments, processorSupport);
            String rawTypeStr = "(" + FullJavaPaths.Type + ")((CompiledExecutionSupport)es).getMetadata(\"" + MetadataJavaPaths.buildMetadataKeyFromType(processorSupport.getClassifier(rawType)) + "\",\"" + PackageableElement.getSystemPathForPackageableElement(rawType, "::") + "\")";
            String base = "new " + JavaPackageAndImportBuilder.buildImplClassReferenceFromType(processorSupport.getClassifier(genericType)) + "(\"Anonymous_NoCounter\")._rawType(" + rawTypeStr + ")";
            if (typeArguments.notEmpty())
            {
                base += "._typeArguments(Lists.fixedSize.of(" + typeArguments.collect(new Function<CoreInstance, Object>()
                {
                    @Override
                    public String valueOf(CoreInstance coreInstance)
                    {
                        return buildGenericType(coreInstance, processorSupport);
                    }
                }).makeString(",") + "))";
            }
            ListIterable<? extends CoreInstance> multiplicityArguments = Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.multiplicityArguments, processorSupport);
            if (multiplicityArguments.notEmpty())
            {
                base += "._multiplicityArguments(Lists.fixedSize.of(" + multiplicityArguments.collect(new Function<CoreInstance, Object>()
                {
                    @Override
                    public String valueOf(CoreInstance coreInstance)
                    {
                        return buildMultiplicity(coreInstance, processorSupport);
                    }
                }).makeString(",") + "))";
            }
            return base;
        }
        else
        {
            CoreInstance typeParameter = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.typeParameter, processorSupport);
            String typeParam = "new org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_TypeParameter_Impl(\"Anonymous_NoCounter\")._name(\"" + typeParameter.getValueForMetaPropertyToOne(M3Properties.name).getName() + "\")";
            return "new " + JavaPackageAndImportBuilder.buildImplClassReferenceFromType(processorSupport.getClassifier(genericType)) + "(\"Anonymous_NoCounter\")._typeParameter("+typeParam+")";
        }
    }

    public static String buildMultiplicity(CoreInstance multiplicity, final ProcessorSupport processorSupport)
    {
        if (Instance.instanceOf(multiplicity, M3Paths.PackageableMultiplicity, processorSupport))
        {
            return "(" + FullJavaPaths.PackageableMultiplicity + ")((CompiledExecutionSupport)es).getMetadata(\"" + MetadataJavaPaths.PackageableMultiplicity + "\",\""+ PackageableElement.getSystemPathForPackageableElement(multiplicity, "::") +"\")";
        }
        else
        {
            CoreInstance lowerBound = Instance.getValueForMetaPropertyToOneResolved(multiplicity, M3Properties.lowerBound, processorSupport);
            CoreInstance upperBound = Instance.getValueForMetaPropertyToOneResolved(multiplicity, M3Properties.upperBound, processorSupport);
            CoreInstance mulParam = Instance.getValueForMetaPropertyToOneResolved(multiplicity, M3Properties.multiplicityParameter, processorSupport);
            return "new " + FullJavaPaths.Multiplicity_Impl + "(\"Anonymous_NoCounter\")" +
                    (lowerBound == null?"":"._lowerBound("+buildMultiplicityValue(lowerBound, processorSupport)+")") +
                    (upperBound == null?"":"._upperBound("+buildMultiplicityValue(upperBound, processorSupport)+")") +
                    (mulParam == null?"":"._multiplicityParameter(\""+mulParam.getName()+"\")");
        }
    }

    public static String buildMultiplicityValue(CoreInstance multiplicityValue, final ProcessorSupport processorSupport)
    {
        CoreInstance val = Instance.getValueForMetaPropertyToOneResolved(multiplicityValue, M3Properties.value, processorSupport);
        return "new " + FullJavaPaths.MultiplicityValue_Impl + "(\"Anonymous_NoCounter\")"+(val==null?"":"._value("+val.getName()+"l)");
    }
}
