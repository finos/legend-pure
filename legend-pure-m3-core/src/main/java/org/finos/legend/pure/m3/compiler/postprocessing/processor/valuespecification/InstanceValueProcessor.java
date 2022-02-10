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

package org.finos.legend.pure.m3.compiler.postprocessing.processor.valuespecification;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.GenericTypeTraceability;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.inference.TypeInference;
import org.finos.legend.pure.m3.compiler.postprocessing.inference.TypeInferenceContext;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RootRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValueSpecificationContext;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class InstanceValueProcessor extends Processor<InstanceValue>
{
    @Override
    public String getClassName()
    {
        return M3Paths.InstanceValue;
    }

    @Override
    public void process(InstanceValue instance, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        TypeInferenceContext typeInferenceContext = state.getTypeInferenceContext();
        ListIterable<? extends CoreInstance> values = ImportStub.withImportStubByPasses(ListHelper.wrapListIterable(instance._valuesCoreInstance()), processorSupport);
        boolean isCollection = values.size() > 1;
        values.forEachWithIndex((child, i) ->
        {
            if (isCollection)
            {
                typeInferenceContext.addStateForCollectionElement();
            }
            if (child instanceof ValueSpecification)
            {
                InstanceValueSpecificationContext usageContext = (InstanceValueSpecificationContext) processorSupport.newAnonymousCoreInstance(null, M3Paths.InstanceValueSpecificationContext);
                usageContext._offset(i);
                usageContext._instanceValue(instance);
                ((ValueSpecification) child)._usageContext(usageContext);
            }
            else if (child instanceof RootRouteNode)
            {
                ((RootRouteNode) child)._owner(instance);
            }

            if (child instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub)
            {
                ImportStub.processImportStub((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub) child, repository, processorSupport);
            }
            // TODO Is this check necessary? The post-processor keeps track of what has been processed.
            else if (!(child instanceof Class))
            {
                PostProcessor.processElement(matcher, child, state, processorSupport);
            }
        });
        if (isCollection)
        {
            TypeInference.potentiallyUpdateParentTypeParamForInstanceValueWithManyElements(instance, typeInferenceContext, state, processorSupport);
        }
        updateInstanceValue(instance, processorSupport);
    }

    @Override
    public void populateReferenceUsages(InstanceValue instance, ModelRepository repository, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> values = ImportStub.withImportStubByPasses(ListHelper.wrapListIterable(instance._valuesCoreInstance()), processorSupport);
        if (values.isEmpty() || Measure.isUnitInstance(instance, processorSupport))
        {
            GenericTypeTraceability.addTraceForEmptyInstanceValueGenericType(instance, repository, processorSupport);
        }
        else
        {
            values.forEachWithIndex((value, i) ->
            {
                //TODO use java instanceof operator once we sort out issue PURE-3418 with Path object being SimpleCoreInstance
                if (Instance.instanceOf(value, M3Paths.PackageableElement, processorSupport))
                {
                    addReferenceUsage(instance, value, M3Properties.values, i, repository, processorSupport);
                }
            });
        }
    }

    //TODO refactor methods to used typed interfaces once we sort out issue PURE-3419 with these static methods being called from compiled runtime code outside processors
    public static void updateInstanceValue(CoreInstance instanceValue, ProcessorSupport processorSupport)
    {
        if (!Measure.isUnitInstance(instanceValue, processorSupport)) //stop InstanceValue genericType setting to value type e.g. Integer
        {
            updateInstanceValue(instanceValue, null, processorSupport);
        }
    }

    public static void updateInstanceValue(CoreInstance instanceValue, CoreInstance knownMostGeneralGenericTypeBound, ProcessorSupport processorSupport)
    {
        boolean isExecutable = org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification.isExecutable(instanceValue, processorSupport);
        ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(instanceValue, M3Properties.values, processorSupport);
        if (values.size() == 1)
        {
            updateSingleInstanceValue(instanceValue, isExecutable, values.get(0), processorSupport);
        }
        else
        {
            updateCompositeInstanceValue(instanceValue, isExecutable, values, knownMostGeneralGenericTypeBound, processorSupport);
        }
    }

    private static void updateSingleInstanceValue(CoreInstance instanceValue, boolean isExecutable, CoreInstance value, ProcessorSupport processorSupport)
    {
        if (instanceValue.getValueForMetaPropertyToOne(M3Properties.genericType) == null)
        {
            CoreInstance genericType = getGenericType(value, isExecutable, processorSupport);
            Instance.addValueToProperty(instanceValue, M3Properties.genericType, GenericType.copyGenericType(genericType, instanceValue.getSourceInformation(), processorSupport), processorSupport);
        }
        if (instanceValue.getValueForMetaPropertyToOne(M3Properties.multiplicity) == null)
        {
            CoreInstance multiplicity = getMultiplicity(value, isExecutable, processorSupport);
            Instance.addValueToProperty(instanceValue, M3Properties.multiplicity, Multiplicity.copyMultiplicity(multiplicity, instanceValue.getSourceInformation(), processorSupport), processorSupport);
        }
    }

    private static CoreInstance getGenericType(CoreInstance value, boolean isExecutable, ProcessorSupport processorSupport)
    {
        if (isExecutable && processorSupport.instance_instanceOf(value, M3Paths.ValueSpecification))
        {
            return value.getValueForMetaPropertyToOne(M3Properties.genericType);
        }
        if (processorSupport.instance_instanceOf(value, M3Paths.Class))
        {
            ListIterable<? extends CoreInstance> typeParameters = value.getValueForMetaPropertyToMany(M3Properties.typeParameters);
            ListIterable<? extends CoreInstance> multiplicityParameters = value.getValueForMetaPropertyToMany(M3Properties.multiplicityParameters);
            if (typeParameters.notEmpty() || multiplicityParameters.notEmpty())
            {
                CoreInstance myGenericType = Type.wrapGenericType(value, processorSupport);

                if (typeParameters.notEmpty())
                {
                    MutableList<CoreInstance> typeArgs = FastList.newList(typeParameters.size());
                    for (CoreInstance typeParameter : typeParameters)
                    {
                        boolean isContravariant = PrimitiveUtilities.getBooleanValue(typeParameter.getValueForMetaPropertyToOne(M3Properties.contravariant), false);
                        typeArgs.add(Type.wrapGenericType(processorSupport.package_getByUserPath(isContravariant ? M3Paths.Nil : M3Paths.Any), processorSupport));
                    }
                    Instance.setValuesForProperty(myGenericType, M3Properties.typeArguments, typeArgs, processorSupport);
                }

                if (multiplicityParameters.notEmpty())
                {
                    ListIterable<CoreInstance> multiplicityArgs = multiplicityParameters.collect(Functions.getFixedValue(processorSupport.package_getByUserPath(M3Paths.ZeroMany)));
                    Instance.setValuesForProperty(myGenericType, M3Properties.multiplicityArguments, multiplicityArgs, processorSupport);
                }

                CoreInstance result = Type.wrapGenericType(processorSupport.package_getByUserPath(M3Paths.Class), processorSupport);
                Instance.addValueToProperty(result, M3Properties.typeArguments, myGenericType, processorSupport);
                return result;
            }
        }
        return Instance.extractGenericTypeFromInstance(value, processorSupport);
    }

    private static CoreInstance getMultiplicity(CoreInstance value, boolean isExecutable, ProcessorSupport processorSupport)
    {
        if (isExecutable && processorSupport.instance_instanceOf(value, M3Paths.ValueSpecification))
        {
            return Instance.getValueForMetaPropertyToOneResolved(value, M3Properties.multiplicity, processorSupport);
        }
        return Multiplicity.newMultiplicity(1, processorSupport);
    }

    private static void updateCompositeInstanceValue(CoreInstance instanceValue, boolean isExecutable, ListIterable<? extends CoreInstance> values, CoreInstance knownMostGeneralGenericTypeBound, ProcessorSupport processorSupport)
    {
        if (instanceValue.getValueForMetaPropertyToOne(M3Properties.genericType) == null)
        {
            MutableList<CoreInstance> genericTypeSet = values.collect(instance ->
            {
                CoreInstance genericType = getGenericType(instance, isExecutable, processorSupport);
                return GenericType.isGenericTypeConcrete(genericType) ? genericType : Type.wrapGenericType(processorSupport.type_TopType(), processorSupport);
            }, Lists.mutable.ofInitialCapacity(values.size()));

            CoreInstance commonGenericType = GenericType.findBestCommonCovariantNonFunctionTypeGenericType(genericTypeSet, knownMostGeneralGenericTypeBound, instanceValue.getSourceInformation(), processorSupport);
            Instance.setValueForProperty(instanceValue, M3Properties.genericType, commonGenericType, processorSupport);
        }
        if (instanceValue.getValueForMetaPropertyToOne(M3Properties.multiplicity) == null)
        {
            int size = Instance.getValueForMetaPropertyToManyResolved(instanceValue, M3Properties.values, processorSupport).size();
            Instance.addValueToProperty(instanceValue, M3Properties.multiplicity, Multiplicity.newMultiplicity(size, processorSupport), processorSupport);
        }
    }
}
