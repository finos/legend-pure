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

package org.finos.legend.pure.runtime.java.extension.external.json.interpreted.natives;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ConstraintsOverride;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.EnumInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.BooleanCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.StringCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateTime;
import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.StrictDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.Year;
import org.finos.legend.pure.m4.coreinstance.primitive.date.YearMonth;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ObjectFactory;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.DefaultConstraintHandler;
import org.finos.legend.pure.runtime.java.interpreted.natives.basics.meta.NewUnit;
import org.finos.legend.pure.runtime.java.interpreted.natives.DeserializationUtils;
import org.finos.legend.pure.runtime.java.interpreted.natives.NumericUtilities;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonDeserializationCache;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonDeserializationContext;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonDeserializer;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

public class FromJson extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;
    private final ModelRepository repository;

    public FromJson(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.functionExecution = functionExecution;
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, final Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, final Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, final VariableContext variableContext, final CoreInstance functionExpressionToUseInStack, final Profiler profiler, final InstantiationContext instantiationContext, final ExecutionSupport executionSupport, final Context context, final ProcessorSupport processorSupport) throws PureExecutionException
    {
        final SourceInformation si = functionExpressionToUseInStack.getSourceInformation();

        Class startingClass = (Class) Instance.getValueForMetaPropertyToOneResolved(params.get(1) , M3Properties.values, processorSupport);
        CoreInstance config = Instance.getValueForMetaPropertyToOneResolved(params.get(2) , M3Properties.values, processorSupport);
        String typeKeyName = ((StringCoreInstance) config.getValueForMetaPropertyToOne("typeKeyName")).getValue();
        Map<String, Class> keyLookup = new UnifiedMap<String, Class>();
        for (CoreInstance o : config.getValueForMetaPropertyToMany("typeLookup"))
        {
            keyLookup.put(o.getValueForMetaPropertyToOne("first").getName(), (Class)_Package.getByUserPath(o.getValueForMetaPropertyToOne("second").getName(), functionExecution.getProcessorSupport()));
        }
        Boolean failOnUnknownProperties = ((BooleanCoreInstance) config.getValueForMetaPropertyToOne("failOnUnknownProperties")).getValue();
        final ConstraintsOverride constraintsOverride = (ConstraintsOverride)config.getValueForMetaPropertyToOne("constraintsHandler");

        String jsonText = PrimitiveUtilities.getStringValue(Instance.getValueForMetaPropertyToOneResolved(params.get(0) , M3Properties.values, processorSupport));
        return JsonDeserializer.fromJson(jsonText, startingClass, new JsonDeserializationContext(new JsonDeserializationCache(), si, processorSupport, typeKeyName, keyLookup, failOnUnknownProperties, new ObjectFactory()
        {
            @Override
            public <T extends Any> T newObject(Class<T> clazz, Map<String, RichIterable<?>> properties)
            {
                CoreInstance instance = FromJson.this.repository.newEphemeralAnonymousCoreInstance(functionExpressionToUseInStack.getSourceInformation(), clazz);

                for(Entry<String, RichIterable<?>> eachKeyValue : properties.entrySet())
                {
                    FastList<CoreInstance> values = new FastList<>();
                    for(Object eachValue : eachKeyValue.getValue())
                    {
                        if(eachValue instanceof String)
                        {
                            values.add(FromJson.this.repository.newStringCoreInstance((String) eachValue));
                        }
                        else if(eachValue instanceof Boolean)
                        {
                            values.add(FromJson.this.repository.newBooleanCoreInstance((Boolean) eachValue));
                        }
                        else if(eachValue instanceof Integer)
                        {
                            values.add(FromJson.this.repository.newIntegerCoreInstance((int) eachValue));
                        }
                        else if(eachValue instanceof Long)
                        {
                            values.add(FromJson.this.repository.newIntegerCoreInstance((long) eachValue));
                        }
                        else if(eachValue instanceof BigDecimal)
                        {
                            values.add(FromJson.this.repository.newDecimalCoreInstance((BigDecimal) eachValue));
                        }
                        else if(eachValue instanceof Double || eachValue instanceof Number)
                        {
                            values.add(FromJson.this.repository.newFloatCoreInstance(BigDecimal.valueOf((Double) eachValue)));
                        }
                        else if(eachValue instanceof Year || eachValue instanceof YearMonth)
                        {
                            values.add(FromJson.this.repository.newDateCoreInstance((PureDate) eachValue));
                        }
                        else if(eachValue instanceof StrictDate)
                        {
                            values.add(FromJson.this.repository.newStrictDateCoreInstance((PureDate) eachValue));
                        }
                        else if(eachValue instanceof DateTime || eachValue instanceof LatestDate || eachValue instanceof PureDate)
                        {
                            values.add(FromJson.this.repository.newDateTimeCoreInstance((PureDate) eachValue));
                        }
                        else if(eachValue instanceof InstanceValue)
                        {
                            InstanceValue asInstanceValue = (InstanceValue) eachValue;
                            if (Measure.isUnitOrMeasureInstance(asInstanceValue, processorSupport))
                            {
                                values.add(asInstanceValue);
                            }
                            else
                            {
                                values.add((CoreInstance)asInstanceValue._values().getFirst());
                            }
                        }
                        else if(eachValue instanceof EnumInstance)
                        {
                            values.add((CoreInstance) eachValue);
                        }
                        else
                        {
                            throw new PureExecutionException(si, "Unknown type from output of JsonDeserializer for property: " + eachKeyValue.getKey());
                        }
                    }
                    Instance.setValuesForProperty(instance, eachKeyValue.getKey(), values, processorSupport);
                }

                DeserializationUtils.replaceReverseProperties(instance, processorSupport, si);

                CoreInstance override =  processorSupport.newAnonymousCoreInstance(si, M3Paths.ConstraintsGetterOverride);
                if(constraintsOverride != null)
                {
                    Instance.addValueToProperty(override, M3Properties.constraintsManager, constraintsOverride._constraintsManager(), processorSupport);
                    Instance.addValueToProperty(instance, M3Properties.elementOverride, override, processorSupport);
                }
                CoreInstance value = ValueSpecificationBootstrap.wrapValueSpecification(instance, true, processorSupport);

                return (T) DefaultConstraintHandler.handleConstraints(clazz, value, si, functionExecution, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
            }

            public <T extends Any> T newUnitInstance(CoreInstance propertyType, String unitTypeString, Number unitValue)
            {
                CoreInstance retrievedUnit = processorSupport.package_getByUserPath(unitTypeString);
                if (!processorSupport.type_subTypeOf(retrievedUnit, propertyType))
                {
                    throw new PureExecutionException("Cannot match unit type: " + unitTypeString + " as subtype of type: " + PackageableElement.getUserPathForPackageableElement(propertyType));
                }

                FastList<CoreInstance> params = new FastList<>();
                params.add(ValueSpecificationBootstrap.wrapValueSpecification(retrievedUnit, false, processorSupport));
                params.add(NumericUtilities.toPureNumberValueExpression(unitValue, false, repository, processorSupport));
                return (T) new NewUnit(functionExecution, repository).execute(params, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, context, processorSupport);
            }
        }));
    }
}
