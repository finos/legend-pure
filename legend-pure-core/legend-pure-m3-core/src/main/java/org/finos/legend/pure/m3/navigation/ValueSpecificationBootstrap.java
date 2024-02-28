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

package org.finos.legend.pure.m3.navigation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.valuespecification.InstanceValueProcessor;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;

import java.math.BigDecimal;
import java.math.BigInteger;

public class ValueSpecificationBootstrap
{

    private static final Predicate2<CoreInstance, ProcessorSupport> IS_NON_EXECUTABLE_VAL_SPEC = (coreInstance, processorSupport) -> processorSupport.instance_instanceOf(coreInstance, M3Paths.ValueSpecification) && !ValueSpecification.isExecutable(coreInstance, processorSupport);

    private static final Function<CoreInstance, ListIterable<CoreInstance>> EXTRACT_CORE_INSTANCE_VALUES = o -> (ListIterable<CoreInstance>) o.getValueForMetaPropertyToMany(M3Properties.values);

    public static CoreInstance wrapValueSpecification(CoreInstance value, boolean executable, ProcessorSupport processorSupport)
    {
        if (value != null && processorSupport.instance_instanceOf(value, M3Paths.ValueSpecification) && !ValueSpecification.isExecutable(value, processorSupport))
        {
            return value;
        }
        else if (Measure.isUnitOrMeasureInstance(value, processorSupport))
        {
            return value;
        }
        return wrap(value, getTypeForWrapping(executable), processorSupport);
    }

    public static CoreInstance wrapValueSpecification(RichIterable<? extends CoreInstance> values, boolean executable, final ProcessorSupport processorSupport)
    {
        return wrapValueSpecification(values, executable, null, processorSupport);
    }

    public static CoreInstance wrapValueSpecification(RichIterable<? extends CoreInstance> values, boolean executable, CoreInstance knownMostGeneralGenericTypeBound, final ProcessorSupport processorSupport)
    {
        if (values.size() == 1 && Measure.isUnitOrMeasureInstance(values.getFirst(), processorSupport))
        {
            return values.getFirst();
        }
        if (values.notEmpty() && values.allSatisfyWith(IS_NON_EXECUTABLE_VAL_SPEC, processorSupport))
        {
            return wrapInstanceMany(values.flatCollect(EXTRACT_CORE_INSTANCE_VALUES), null, knownMostGeneralGenericTypeBound, getTypeForWrapping(false), processorSupport);
        }
        return wrapInstanceMany(values, null, knownMostGeneralGenericTypeBound, getTypeForWrapping(executable), processorSupport);
    }

    public static CoreInstance wrapValueSpecification_ForFunctionReturnValue(CoreInstance functionReturnGenericType, ListIterable<? extends CoreInstance> values, boolean executable, final ProcessorSupport processorSupport)
    {
        if (values.size() > 2 && GenericType.isGenericTypeFullyConcrete(functionReturnGenericType, processorSupport))
        {
            CoreInstance firstValGenericType = Instance.extractGenericTypeFromInstance(values.getFirst(), processorSupport);

            //if the types are the same we know that this is the type we will use, (its not going to resolve to a subtype)
            //So take the fast path
            if (GenericType.genericTypesEqual(functionReturnGenericType, firstValGenericType, processorSupport))
            {
                //Take the fast path
                return wrapValueSpecification_ResultGenericTypeIsKnown(values, functionReturnGenericType, executable, processorSupport);
            }
            else
            {
                //if the first two types resolve to the same as the property return generic type
                // we know that this is the type we will use, (its not going to resolve to a subtype)
                //So take the fast path
                CoreInstance secondValGenericType = Instance.extractGenericTypeFromInstance(values.get(1), processorSupport);
                CoreInstance thirdValGenericType = Instance.extractGenericTypeFromInstance(values.get(2), processorSupport);
                CoreInstance firstCommonGenericType = GenericType.findBestCommonCovariantNonFunctionTypeGenericType(Lists.fixedSize.of(firstValGenericType, secondValGenericType, thirdValGenericType), null, null, processorSupport);

                if (GenericType.genericTypesEqual(functionReturnGenericType, firstCommonGenericType, processorSupport))
                {
                    return wrapValueSpecification_ResultGenericTypeIsKnown(values, functionReturnGenericType, executable, processorSupport);
                }
                else
                {
                    return wrapValueSpecification(values, executable, functionReturnGenericType, processorSupport);
                }
            }
        }
        else
        {
            return wrapValueSpecification(values, executable, functionReturnGenericType, processorSupport);
        }
    }

    public static CoreInstance wrapValueSpecification_ResultGenericTypeIsKnown(RichIterable<? extends CoreInstance> values, CoreInstance genericType, boolean executable, final ProcessorSupport processorSupport)
    {
        if (values.notEmpty() && values.allSatisfyWith(IS_NON_EXECUTABLE_VAL_SPEC, processorSupport))
        {
            return wrapInstanceMany(values.flatCollect(EXTRACT_CORE_INSTANCE_VALUES), genericType, null, getTypeForWrapping(false), processorSupport);
        }
        return wrapInstanceMany(values, genericType, null, getTypeForWrapping(executable), processorSupport);
    }

    private static String getTypeForWrapping(boolean executable)
    {
        return executable ? M3Paths.InstanceValue : M3Paths.NonExecutableValueSpecification;
    }

    public static CoreInstance newStringLiteral(ModelRepository repository, String value, ProcessorSupport processorSupport)
    {
        return wrapValueSpecification(repository.newStringCoreInstance(value), true, processorSupport);
    }

    public static CoreInstance newBooleanLiteral(ModelRepository repository, boolean value, ProcessorSupport processorSupport)
    {
        return wrapValueSpecification(repository.newBooleanCoreInstance(value), true, processorSupport);
    }

    public static CoreInstance newIntegerLiteral(ModelRepository repository, int value, ProcessorSupport processorSupport)
    {
        return wrapValueSpecification(repository.newIntegerCoreInstance(value), true, processorSupport);
    }

    public static CoreInstance newIntegerLiteral(ModelRepository repository, long value, ProcessorSupport processorSupport)
    {
        return wrapValueSpecification(repository.newIntegerCoreInstance(value), true, processorSupport);
    }

    public static CoreInstance newIntegerLiteral(ModelRepository repository, BigInteger value, ProcessorSupport processorSupport)
    {
        return wrapValueSpecification(repository.newIntegerCoreInstance(value), true, processorSupport);
    }

    public static CoreInstance newFloatLiteral(ModelRepository repository, BigDecimal value, ProcessorSupport processorSupport)
    {
        return wrapValueSpecification(repository.newFloatCoreInstance(value), true, processorSupport);
    }

    public static CoreInstance newDateLiteral(ModelRepository repository, PureDate value, ProcessorSupport processorSupport)
    {
        return wrapValueSpecification(repository.newDateCoreInstance(value), true, processorSupport);
    }

    private static CoreInstance wrap(CoreInstance value, String type, ProcessorSupport processorSupport)
    {
        if (value == null)
        {
            return wrapInstanceMany(Lists.immutable.with(), null, null, type, processorSupport);
        }
        else
        {
            CoreInstance inst = processorSupport.newEphemeralAnonymousCoreInstance(type);
            Instance.addValueToProperty(inst, M3Properties.values, value, processorSupport);
            Instance.addValueToProperty(inst, M3Properties.genericType, GenericType.copyGenericType(Instance.extractGenericTypeFromInstance(value, processorSupport), processorSupport), processorSupport);
            Instance.addValueToProperty(inst, M3Properties.multiplicity, processorSupport.package_getByUserPath(M3Paths.PureOne), processorSupport);
            return inst;
        }
    }

    private static CoreInstance wrapInstanceMany(RichIterable<? extends CoreInstance> values, CoreInstance knownGenericType, CoreInstance knownMostGeneralGenericTypeBound, String type, ProcessorSupport processorSupport)
    {
        CoreInstance inst = processorSupport.newEphemeralAnonymousCoreInstance(type);
        if (Iterate.isEmpty(values))
        {
            Instance.addValueToProperty(inst, M3Properties.genericType, Type.wrapGenericType(processorSupport.type_BottomType(), processorSupport), processorSupport);
            Instance.addValueToProperty(inst, M3Properties.multiplicity, processorSupport.package_getByUserPath(M3Paths.PureZero), processorSupport);
            Instance.addPropertyWithEmptyList(inst, M3Properties.values, processorSupport);
        }
        else if (knownGenericType != null)
        {
            Instance.setValueForProperty(inst, M3Properties.genericType, knownGenericType, processorSupport);
            Instance.addValueToProperty(inst, M3Properties.multiplicity, Multiplicity.newMultiplicity(values.size(), processorSupport), processorSupport);
            Instance.setValuesForProperty(inst, M3Properties.values, Lists.immutable.withAll(values), processorSupport);
        }
        else
        {
            Instance.setValuesForProperty(inst, M3Properties.values, Lists.immutable.withAll(values), processorSupport);
            InstanceValueProcessor.updateInstanceValue(inst, knownMostGeneralGenericTypeBound, processorSupport);
        }
        return inst;
    }
}
