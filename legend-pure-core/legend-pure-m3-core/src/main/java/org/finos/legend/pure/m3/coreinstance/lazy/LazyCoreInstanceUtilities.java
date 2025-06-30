// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.coreinstance.lazy;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.serialization.compiler.element.ElementBuilder;
import org.finos.legend.pure.m3.serialization.compiler.element.InstanceData;
import org.finos.legend.pure.m3.serialization.compiler.element.PropertyValues;
import org.finos.legend.pure.m3.serialization.compiler.element.Reference;
import org.finos.legend.pure.m3.serialization.compiler.element.Value;
import org.finos.legend.pure.m3.serialization.compiler.element.ValueOrReference;
import org.finos.legend.pure.m3.serialization.compiler.element.ValueOrReferenceVisitor;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReference;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReferenceConsumer;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolver;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolvers;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileStateSet;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public class LazyCoreInstanceUtilities
{
    // Property value handling

    public static <V extends CoreInstance> OneValue<V> newToOnePropertyValue(PropertyValues propertyValues, ReferenceIdResolver extResolver, IntFunction<? extends CoreInstance> intResolver, PrimitiveValueResolver primitiveValueResolver, boolean allowEagerValueResolution)
    {
        if (propertyValues == null)
        {
            return OneValue.fromValue(null);
        }
        ListIterable<ValueOrReference> values = propertyValues.getValues();
        if (values.size() > 1)
        {
            throw new IllegalStateException("Cannot create to-one property value for property '" + propertyValues.getPropertyName() + "': " + values.size() + " values present");
        }
        return newToOnePropertyValue(values.getFirst(), extResolver, intResolver, primitiveValueResolver, allowEagerValueResolution);
    }

    public static <V extends CoreInstance> OneValue<V> newToOnePropertyValue(ValueOrReference propertyValue, ReferenceIdResolver extResolver, IntFunction<? extends CoreInstance> intResolver, PrimitiveValueResolver primitiveValueResolver, boolean allowEagerValueResolution)
    {
        return newToOnePropertyValue_internal(propertyValue, extResolver, intResolver, primitiveValueResolver, allowEagerValueResolution, OneValue::fromValue, OneValue::fromSupplier);
    }

    public static <V extends CoreInstance> OneValue<V> newToOnePropertyValue(PropertyValues propertyValues, ReferenceIdResolver extResolver, IntFunction<? extends CoreInstance> intResolver, PrimitiveValueResolver primitiveValueResolver, boolean allowEagerValueResolution, Function<? super CoreInstance, ? extends V> fromCoreInstanceFn)
    {
        if (propertyValues == null)
        {
            return OneValue.fromValue(null);
        }
        ListIterable<ValueOrReference> values = propertyValues.getValues();
        if (values.size() > 1)
        {
            throw new IllegalStateException("Cannot create to-one property value for property '" + propertyValues.getPropertyName() + "': " + values.size() + " values present");
        }
        return newToOnePropertyValue(values.getFirst(), extResolver, intResolver, primitiveValueResolver, allowEagerValueResolution, fromCoreInstanceFn);
    }

    public static <V extends CoreInstance> OneValue<V> newToOnePropertyValue(ValueOrReference propertyValue, ReferenceIdResolver extResolver, IntFunction<? extends CoreInstance> intResolver, PrimitiveValueResolver primitiveValueResolver, boolean allowEagerValueResolution, Function<? super CoreInstance, ? extends V> fromCoreInstanceFn)
    {
        return newToOnePropertyValue_internal(propertyValue, extResolver, intResolver, primitiveValueResolver, allowEagerValueResolution, v -> OneValue.fromValue(v, fromCoreInstanceFn), s -> OneValue.fromSupplier(s, fromCoreInstanceFn));
    }

    public static <V> OneValue<V> newToOnePropertyValue(PropertyValues propertyValues, ReferenceIdResolver extResolver, IntFunction<? extends CoreInstance> intResolver, PrimitiveValueResolver primitiveValueResolver, boolean allowEagerValueResolution, Function<? super V, ? extends CoreInstance> toCoreInstanceFn, Function<? super CoreInstance, ? extends V> fromCoreInstanceFn)
    {
        Objects.requireNonNull(toCoreInstanceFn);
        if (propertyValues == null)
        {
            return OneValue.fromValue(null, toCoreInstanceFn, fromCoreInstanceFn);
        }
        ListIterable<ValueOrReference> values = propertyValues.getValues();
        if (values.size() > 1)
        {
            throw new IllegalStateException("Cannot create to-one property value for property '" + propertyValues.getPropertyName() + "': " + values.size() + " values present");
        }
        return newToOnePropertyValue(values.getFirst(), extResolver, intResolver, primitiveValueResolver, allowEagerValueResolution, toCoreInstanceFn, fromCoreInstanceFn);
    }

    public static <V> OneValue<V> newToOnePropertyValue(ValueOrReference propertyValue, ReferenceIdResolver extResolver, IntFunction<? extends CoreInstance> intResolver, PrimitiveValueResolver primitiveValueResolver, boolean allowEagerValueResolution, Function<? super V, ? extends CoreInstance> toCoreInstanceFn, Function<? super CoreInstance, ? extends V> fromCoreInstanceFn)
    {
        return newToOnePropertyValue_internal(propertyValue, extResolver, intResolver, primitiveValueResolver, allowEagerValueResolution, v -> OneValue.fromValue(v, toCoreInstanceFn, fromCoreInstanceFn), s -> OneValue.fromSupplier(s, toCoreInstanceFn, fromCoreInstanceFn));
    }

    @SuppressWarnings("unchecked")
    private static <V> OneValue<V> newToOnePropertyValue_internal(ValueOrReference propertyValue, ReferenceIdResolver extResolver, IntFunction<? extends CoreInstance> intResolver, PrimitiveValueResolver primitiveValueResolver, boolean allowEagerValueResolution, Function<V, OneValue<V>> fromValue, Function<Supplier<? extends V>, OneValue<V>> fromSupplier)
    {
        return (propertyValue == null) ? fromValue.apply(null) : propertyValue.visit(new ValueOrReferenceVisitor<OneValue<V>>()
        {
            @Override
            public OneValue<V> visit(Reference.ExternalReference reference)
            {
                String id = reference.getId();
                return fromSupplier.apply(() -> (V) extResolver.resolveReference(id));
            }

            @Override
            public OneValue<V> visit(Reference.InternalReference reference)
            {
                int id = reference.getId();
                return fromSupplier.apply(() -> (V) intResolver.apply(id));
            }

            @Override
            public OneValue<V> visit(Value.BooleanValue value)
            {
                boolean b = value.getValue();
                return allowEagerValueResolution ?
                       fromValue.apply((V) primitiveValueResolver.resolveBoolean(b)) :
                       fromSupplier.apply(() -> (V) primitiveValueResolver.resolveBoolean(b));
            }

            @Override
            public OneValue<V> visit(Value.ByteValue value)
            {
                byte b = value.getValue();
                return allowEagerValueResolution ?
                       fromValue.apply((V) primitiveValueResolver.resolveByte(b)) :
                       fromSupplier.apply(() -> (V) primitiveValueResolver.resolveByte(b));
            }

            @Override
            public OneValue<V> visit(Value.DateValue value)
            {
                PureDate date = value.getValue();
                return allowEagerValueResolution ?
                       fromValue.apply((V) primitiveValueResolver.resolveDate(date)) :
                       fromSupplier.apply(() -> (V) primitiveValueResolver.resolveDate(date));
            }

            @Override
            public OneValue<V> visit(Value.DateTimeValue value)
            {
                PureDate dateTime = value.getValue();
                return allowEagerValueResolution ?
                       fromValue.apply((V) primitiveValueResolver.resolveDateTime(dateTime)) :
                       fromSupplier.apply(() -> (V) primitiveValueResolver.resolveDateTime(dateTime));
            }

            @Override
            public OneValue<V> visit(Value.StrictDateValue value)
            {
                PureDate strictDate = value.getValue();
                return allowEagerValueResolution ?
                       fromValue.apply((V) primitiveValueResolver.resolveStrictDate(strictDate)) :
                       fromSupplier.apply(() -> (V) primitiveValueResolver.resolveStrictDate(strictDate));
            }

            @Override
            public OneValue<V> visit(Value.LatestDateValue value)
            {
                return allowEagerValueResolution ?
                       fromValue.apply((V) primitiveValueResolver.resolveLatestDate()) :
                       fromSupplier.apply(() -> (V) primitiveValueResolver.resolveLatestDate());
            }

            @Override
            public OneValue<V> visit(Value.DecimalValue value)
            {
                BigDecimal d = value.getValue();
                return allowEagerValueResolution ?
                       fromValue.apply((V) primitiveValueResolver.resolveDecimal(d)) :
                       fromSupplier.apply(() -> (V) primitiveValueResolver.resolveDecimal(d));
            }

            @Override
            public OneValue<V> visit(Value.FloatValue value)
            {
                BigDecimal f = value.getValue();
                return allowEagerValueResolution ?
                       fromValue.apply((V) primitiveValueResolver.resolveFloat(f)) :
                       fromSupplier.apply(() -> (V) primitiveValueResolver.resolveFloat(f));
            }

            @Override
            public OneValue<V> visit(Value.IntegerValue value)
            {
                Number i = value.getValue();
                return allowEagerValueResolution ?
                       fromValue.apply((V) primitiveValueResolver.resolveInteger(i)) :
                       fromSupplier.apply(() -> (V) primitiveValueResolver.resolveInteger(i));
            }

            @Override
            public OneValue<V> visit(Value.StrictTimeValue value)
            {
                PureStrictTime strictTime = value.getValue();
                return allowEagerValueResolution ?
                       fromValue.apply((V) primitiveValueResolver.resolveStrictTime(strictTime)) :
                       fromSupplier.apply(() -> (V) primitiveValueResolver.resolveStrictTime(strictTime));
            }

            @Override
            public OneValue<V> visit(Value.StringValue value)
            {
                String string = value.getValue();
                return allowEagerValueResolution ?
                       fromValue.apply((V) primitiveValueResolver.resolveString(string)) :
                       fromSupplier.apply(() -> (V) primitiveValueResolver.resolveString(string));
            }
        });
    }

    public static <V extends CoreInstance> ManyValues<V> newToManyPropertyValue(PropertyValues propertyValues, ReferenceIdResolver extResolver, IntFunction<? extends CoreInstance> intResolver, PrimitiveValueResolver primitiveValueResolver, boolean allowEagerValueResolution)
    {
        return newToManyPropertyValue(propertyValues, extResolver, intResolver, primitiveValueResolver, allowEagerValueResolution, (ListIterable<? extends Supplier<? extends V>>) null);
    }

    public static <V extends CoreInstance> ManyValues<V> newToManyPropertyValue(PropertyValues propertyValues, ReferenceIdResolver extResolver, IntFunction<? extends CoreInstance> intResolver, PrimitiveValueResolver primitiveValueResolver, boolean allowEagerValueResolution, Function<? super CoreInstance, ? extends V> fromCoreInstanceFn)
    {
        return newToManyPropertyValue(propertyValues, extResolver, intResolver, primitiveValueResolver, allowEagerValueResolution, (ListIterable<? extends Supplier<? extends V>>) null, fromCoreInstanceFn);
    }

    public static <V extends CoreInstance> ManyValues<V> newToManyPropertyValue(PropertyValues propertyValues, ReferenceIdResolver extResolver, IntFunction<? extends CoreInstance> intResolver, PrimitiveValueResolver primitiveValueResolver, boolean allowEagerValueResolution, ListIterable<? extends Supplier<? extends V>> extraSuppliers)
    {
        return newToManyPropertyValue_internal(propertyValues, extResolver, intResolver, primitiveValueResolver, allowEagerValueResolution, extraSuppliers, ManyValues::fromValues, ManyValues::fromSuppliers);
    }

    public static <V extends CoreInstance> ManyValues<V> newToManyPropertyValue(PropertyValues propertyValues, ReferenceIdResolver extResolver, IntFunction<? extends CoreInstance> intResolver, PrimitiveValueResolver primitiveValueResolver, boolean allowEagerValueResolution, ListIterable<? extends Supplier<? extends V>> extraSuppliers, Function<? super CoreInstance, ? extends V> fromCoreInstanceFn)
    {
        return newToManyPropertyValue_internal(propertyValues, extResolver, intResolver, primitiveValueResolver, allowEagerValueResolution, extraSuppliers, v -> ManyValues.fromValues(v, fromCoreInstanceFn), s -> ManyValues.fromSuppliers(s, fromCoreInstanceFn));
    }

    public static <V> ManyValues<V> newToManyPropertyValue(PropertyValues propertyValues, ReferenceIdResolver extResolver, IntFunction<? extends CoreInstance> intResolver, PrimitiveValueResolver primitiveValueResolver, boolean allowEagerValueResolution, Function<? super V, ? extends CoreInstance> toCoreInstanceFn, Function<? super CoreInstance, ? extends V> fromCoreInstanceFn)
    {
        return newToManyPropertyValue(propertyValues, extResolver, intResolver, primitiveValueResolver, allowEagerValueResolution, null, toCoreInstanceFn, fromCoreInstanceFn);
    }

    public static <V> ManyValues<V> newToManyPropertyValue(PropertyValues propertyValues, ReferenceIdResolver extResolver, IntFunction<? extends CoreInstance> intResolver, PrimitiveValueResolver primitiveValueResolver, boolean allowEagerValueResolution, ListIterable<? extends Supplier<? extends V>> extraSuppliers, Function<? super V, ? extends CoreInstance> toCoreInstanceFn, Function<? super CoreInstance, ? extends V> fromCoreInstanceFn)
    {
        return newToManyPropertyValue_internal(propertyValues, extResolver, intResolver, primitiveValueResolver, allowEagerValueResolution, extraSuppliers, v -> ManyValues.fromValues(v, toCoreInstanceFn, fromCoreInstanceFn), v -> ManyValues.fromSuppliers(v, toCoreInstanceFn, fromCoreInstanceFn));
    }

    private static <V> ManyValues<V> newToManyPropertyValue_internal(PropertyValues propertyValues, ReferenceIdResolver extResolver, IntFunction<? extends CoreInstance> intResolver, PrimitiveValueResolver primitiveValueResolver, boolean allowEagerValueResolution, ListIterable<? extends Supplier<? extends V>> extraSuppliers, Function<ListIterable<? extends V>, ManyValues<V>> fromValues, Function<ListIterable<? extends Supplier<? extends V>>, ManyValues<V>> fromSuppliers)
    {
        ListIterable<ValueOrReference> pvList = (propertyValues == null) ? null : propertyValues.getValues();
        if ((extraSuppliers != null) && extraSuppliers.notEmpty())
        {
            if ((pvList == null) || pvList.isEmpty())
            {
                return fromSuppliers.apply(extraSuppliers.toImmutable());
            }
            ValueOrReferenceVisitor<Supplier<V>> visitor = getSupplierVisitor(extResolver, intResolver, primitiveValueResolver, allowEagerValueResolution);
            MutableList<Supplier<? extends V>> suppliers = Lists.mutable.ofInitialCapacity(pvList.size() + extraSuppliers.size());
            pvList.collect(visitor, suppliers);
            suppliers.addAllIterable(extraSuppliers);
            return fromSuppliers.apply(suppliers.toImmutable());
        }
        if ((pvList == null) || pvList.isEmpty())
        {
            return fromValues.apply(null);
        }
        if (allowEagerValueResolution && pvList.noneSatisfy(v -> v instanceof Reference))
        {
            ValueOrReferenceVisitor<V> visitor = getValueVisitor(primitiveValueResolver);
            return fromValues.apply(pvList.collect(visitor, Lists.mutable.ofInitialCapacity(pvList.size())));
        }
        ValueOrReferenceVisitor<Supplier<V>> visitor = getSupplierVisitor(extResolver, intResolver, primitiveValueResolver, allowEagerValueResolution);
        return fromSuppliers.apply(pvList.collect(visitor, Lists.mutable.ofInitialCapacity(pvList.size())));
    }

    @SuppressWarnings("unchecked")
    private static <V> ValueOrReferenceVisitor<V> getValueVisitor(PrimitiveValueResolver primitiveValueResolver)
    {
        return new ValueOrReferenceVisitor<V>()
        {
            @Override
            public V visit(Value.BooleanValue value)
            {
                return (V) primitiveValueResolver.resolveBoolean(value.getValue());
            }

            @Override
            public V visit(Value.ByteValue value)
            {
                return (V) primitiveValueResolver.resolveByte(value.getValue());
            }

            @Override
            public V visit(Value.DateValue value)
            {
                return (V) primitiveValueResolver.resolveDate(value.getValue());
            }

            @Override
            public V visit(Value.DateTimeValue value)
            {
                return (V) primitiveValueResolver.resolveDateTime(value.getValue());
            }

            @Override
            public V visit(Value.StrictDateValue value)
            {
                return (V) primitiveValueResolver.resolveStrictDate(value.getValue());
            }

            @Override
            public V visit(Value.LatestDateValue value)
            {
                return (V) primitiveValueResolver.resolveLatestDate();
            }

            @Override
            public V visit(Value.DecimalValue value)
            {
                return (V) primitiveValueResolver.resolveDecimal(value.getValue());
            }

            @Override
            public V visit(Value.FloatValue value)
            {
                return (V) primitiveValueResolver.resolveFloat(value.getValue());
            }

            @Override
            public V visit(Value.IntegerValue value)
            {
                return (V) primitiveValueResolver.resolveInteger(value.getValue());
            }

            @Override
            public V visit(Value.StrictTimeValue value)
            {
                return (V) primitiveValueResolver.resolveStrictTime(value.getValue());
            }

            @Override
            public V visit(Value.StringValue value)
            {
                return (V) primitiveValueResolver.resolveString(value.getValue());
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <V> ValueOrReferenceVisitor<Supplier<V>> getSupplierVisitor(ReferenceIdResolver extResolver, IntFunction<? extends CoreInstance> intResolver, PrimitiveValueResolver primitiveValueResolver, boolean allowEagerValueResolution)
    {
        return allowEagerValueResolution ?
               new ValueOrReferenceVisitor<Supplier<V>>()
               {
                   @Override
                   public Supplier<V> visit(Reference.ExternalReference reference)
                   {
                       String id = reference.getId();
                       return () -> (V) extResolver.resolveReference(id);
                   }

                   @Override
                   public Supplier<V> visit(Reference.InternalReference reference)
                   {
                       int id = reference.getId();
                       return () -> (V) intResolver.apply(id);
                   }

                   @Override
                   public Supplier<V> visit(Value.BooleanValue value)
                   {
                       return valueSupplier((V) primitiveValueResolver.resolveBoolean(value.getValue()));
                   }

                   @Override
                   public Supplier<V> visit(Value.ByteValue value)
                   {
                       return valueSupplier((V) primitiveValueResolver.resolveByte(value.getValue()));
                   }

                   @Override
                   public Supplier<V> visit(Value.DateValue value)
                   {
                       return valueSupplier((V) primitiveValueResolver.resolveDate(value.getValue()));
                   }

                   @Override
                   public Supplier<V> visit(Value.DateTimeValue value)
                   {
                       return valueSupplier((V) primitiveValueResolver.resolveDateTime(value.getValue()));
                   }

                   @Override
                   public Supplier<V> visit(Value.StrictDateValue value)
                   {
                       return valueSupplier((V) primitiveValueResolver.resolveStrictDate(value.getValue()));
                   }

                   @Override
                   public Supplier<V> visit(Value.LatestDateValue value)
                   {
                       return valueSupplier((V) primitiveValueResolver.resolveLatestDate());
                   }

                   @Override
                   public Supplier<V> visit(Value.DecimalValue value)
                   {
                       return valueSupplier((V) primitiveValueResolver.resolveDecimal(value.getValue()));
                   }

                   @Override
                   public Supplier<V> visit(Value.FloatValue value)
                   {
                       return valueSupplier((V) primitiveValueResolver.resolveFloat(value.getValue()));
                   }

                   @Override
                   public Supplier<V> visit(Value.IntegerValue value)
                   {
                       return valueSupplier((V) primitiveValueResolver.resolveInteger(value.getValue()));
                   }

                   @Override
                   public Supplier<V> visit(Value.StrictTimeValue value)
                   {
                       return valueSupplier((V) primitiveValueResolver.resolveStrictTime(value.getValue()));
                   }

                   @Override
                   public Supplier<V> visit(Value.StringValue value)
                   {
                       return valueSupplier((V) primitiveValueResolver.resolveString(value.getValue()));
                   }

                   private Supplier<V> valueSupplier(V value)
                   {
                       return () -> value;
                   }
               } :
               new ValueOrReferenceVisitor<Supplier<V>>()
               {
                   @Override
                   public Supplier<V> visit(Reference.ExternalReference reference)
                   {
                       String id = reference.getId();
                       return () -> (V) extResolver.resolveReference(id);
                   }

                   @Override
                   public Supplier<V> visit(Reference.InternalReference reference)
                   {
                       int id = reference.getId();
                       return () -> (V) intResolver.apply(id);
                   }

                   @Override
                   public Supplier<V> visit(Value.BooleanValue value)
                   {
                       boolean b = value.getValue();
                       return () -> (V) primitiveValueResolver.resolveBoolean(b);
                   }

                   @Override
                   public Supplier<V> visit(Value.ByteValue value)
                   {
                       byte b = value.getValue();
                       return () -> (V) primitiveValueResolver.resolveByte(b);
                   }

                   @Override
                   public Supplier<V> visit(Value.DateValue value)
                   {
                       PureDate date = value.getValue();
                       return () -> (V) primitiveValueResolver.resolveDate(date);
                   }

                   @Override
                   public Supplier<V> visit(Value.DateTimeValue value)
                   {
                       PureDate dateTime = value.getValue();
                       return () -> (V) primitiveValueResolver.resolveDateTime(dateTime);
                   }

                   @Override
                   public Supplier<V> visit(Value.StrictDateValue value)
                   {
                       PureDate strictDate = value.getValue();
                       return () -> (V) primitiveValueResolver.resolveStrictDate(strictDate);
                   }

                   @Override
                   public Supplier<V> visit(Value.LatestDateValue value)
                   {
                       return () -> (V) primitiveValueResolver.resolveLatestDate();
                   }

                   @Override
                   public Supplier<V> visit(Value.DecimalValue value)
                   {
                       BigDecimal d = value.getValue();
                       return () -> (V) primitiveValueResolver.resolveDecimal(d);
                   }

                   @Override
                   public Supplier<V> visit(Value.FloatValue value)
                   {
                       BigDecimal f = value.getValue();
                       return () -> (V) primitiveValueResolver.resolveFloat(f);
                   }

                   @Override
                   public Supplier<V> visit(Value.IntegerValue value)
                   {
                       Number i = value.getValue();
                       return () -> (V) primitiveValueResolver.resolveInteger(i);
                   }

                   @Override
                   public Supplier<V> visit(Value.StrictTimeValue value)
                   {
                       PureStrictTime strictTime = value.getValue();
                       return () -> (V) primitiveValueResolver.resolveStrictTime(strictTime);
                   }

                   @Override
                   public Supplier<V> visit(Value.StringValue value)
                   {
                       String string = value.getValue();
                       return () -> (V) primitiveValueResolver.resolveString(string);
                   }
               };
    }

    // Back references

    public static <FE extends CoreInstance, AE extends CoreInstance, P extends CoreInstance, QP extends CoreInstance, RU extends CoreInstance, G extends CoreInstance>
    void collectBackReferences(ListIterable<? extends BackReference> backReferences,
                               ReferenceIdResolvers referenceIds,
                               ElementBuilder elementBuilder,
                               Collection<? super Supplier<? extends FE>> applications,
                               Collection<? super Supplier<? extends AE>> modelElements,
                               Collection<? super Supplier<? extends P>> propertiesFromAssociations,
                               Collection<? super Supplier<? extends QP>> qualifiedPropertiesFromAssociations,
                               Collection<? super Supplier<? extends RU>> refUsages,
                               Collection<? super Supplier<? extends G>> specializations)
    {
        collectBackReferences(backReferences, referenceIds.resolver(), null, elementBuilder, applications, modelElements, propertiesFromAssociations, qualifiedPropertiesFromAssociations, refUsages, specializations);
    }

    public static <FE extends CoreInstance, AE extends CoreInstance, P extends CoreInstance, QP extends CoreInstance, RU extends CoreInstance, G extends CoreInstance>
    void collectBackReferences(ListIterable<? extends BackReference> backReferences,
                               ReferenceIdResolver refIdResolver,
                               IntFunction<? extends CoreInstance> internalIdResolver,
                               ElementBuilder elementBuilder,
                               Collection<? super Supplier<? extends FE>> applications,
                               Collection<? super Supplier<? extends AE>> modelElements,
                               Collection<? super Supplier<? extends P>> propertiesFromAssociations,
                               Collection<? super Supplier<? extends QP>> qualifiedPropertiesFromAssociations,
                               Collection<? super Supplier<? extends RU>> refUsages,
                               Collection<? super Supplier<? extends G>> specializations)
    {
        IntFunction<? extends CoreInstance> intIdResolver = (internalIdResolver == null) ? getVacuousInternalIdResolver() : internalIdResolver;
        backReferences.forEach(new BackReferenceConsumer()
        {
            @Override
            protected void accept(BackReference.Application application)
            {
                if (applications != null)
                {
                    applications.add(getApplicationSupplier(application, refIdResolver));
                }
            }

            @Override
            protected void accept(BackReference.ModelElement modelElement)
            {
                if (modelElements != null)
                {
                    modelElements.add(getModelElementSupplier(modelElement, refIdResolver));
                }
            }

            @Override
            protected void accept(BackReference.PropertyFromAssociation propertyFromAssociation)
            {
                if (propertiesFromAssociations != null)
                {
                    propertiesFromAssociations.add(getPropertyFromAssociationSupplier(propertyFromAssociation, refIdResolver));
                }
            }

            @Override
            protected void accept(BackReference.QualifiedPropertyFromAssociation qualifiedPropertyFromAssociation)
            {
                if (qualifiedPropertiesFromAssociations != null)
                {
                    qualifiedPropertiesFromAssociations.add(getQualifiedPropertyFromAssociationSupplier(qualifiedPropertyFromAssociation, refIdResolver));
                }
            }

            @Override
            protected void accept(BackReference.ReferenceUsage referenceUsage)
            {
                if (refUsages != null)
                {
                    refUsages.add(getRefUsageSupplier(referenceUsage, elementBuilder, refIdResolver, intIdResolver));
                }
            }

            @Override
            protected void accept(BackReference.Specialization specialization)
            {
                if (specializations != null)
                {
                    specializations.add(getSpecializationSupplier(specialization, refIdResolver));
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <FE extends CoreInstance> Supplier<FE> getApplicationSupplier(BackReference.Application application, ReferenceIdResolver idResolver)
    {
        String funcExprId = application.getFunctionExpression();
        return () -> (FE) idResolver.resolveReference(funcExprId);
    }

    @SuppressWarnings("unchecked")
    public static <AE extends CoreInstance> Supplier<AE> getModelElementSupplier(BackReference.ModelElement modelElement, ReferenceIdResolver idResolver)
    {
        String elementId = modelElement.getElement();
        return () -> (AE) idResolver.resolveReference(elementId);
    }

    @SuppressWarnings("unchecked")
    public static <P extends CoreInstance> Supplier<P> getPropertyFromAssociationSupplier(BackReference.PropertyFromAssociation propertyFromAssociation, ReferenceIdResolver idResolver)
    {
        String propertyId = propertyFromAssociation.getProperty();
        return () -> (P) idResolver.resolveReference(propertyId);
    }

    @SuppressWarnings("unchecked")
    public static <QP extends CoreInstance> Supplier<QP> getQualifiedPropertyFromAssociationSupplier(BackReference.QualifiedPropertyFromAssociation propertyFromAssociation, ReferenceIdResolver idResolver)
    {
        String propertyId = propertyFromAssociation.getQualifiedProperty();
        return () -> (QP) idResolver.resolveReference(propertyId);
    }

    @SuppressWarnings("unchecked")
    public static <RU extends CoreInstance> Supplier<RU> getRefUsageSupplier(BackReference.ReferenceUsage referenceUsage, ElementBuilder elementBuilder, ReferenceIdResolver idResolver, IntFunction<? extends CoreInstance> internalIdResolver)
    {
        PropertyValues offset = PropertyValues.newPropertyValues(M3Properties.offset, M3Paths.ReferenceUsage, Value.newIntegerValue(referenceUsage.getOffset()));
        PropertyValues owner = PropertyValues.newPropertyValues(M3Properties.owner, M3Paths.ReferenceUsage, Reference.newExternalReference(referenceUsage.getOwner()));
        PropertyValues property = PropertyValues.newPropertyValues(M3Properties.propertyName, M3Paths.ReferenceUsage, Value.newStringValue(referenceUsage.getProperty()));

        InstanceData instanceData = InstanceData.newInstanceData(null, M3Paths.ReferenceUsage, referenceUsage.getSourceInformation(), null, CompileStateSet.PROCESSED_VALIDATED.toBitSet(), Lists.immutable.with(offset, owner, property));
        return () -> (RU) elementBuilder.buildComponentInstance(instanceData, Lists.fixedSize.empty(), idResolver, internalIdResolver);
    }

    @SuppressWarnings("unchecked")
    public static <G extends CoreInstance> Supplier<G> getSpecializationSupplier(BackReference.Specialization specialization, ReferenceIdResolver idResolver)
    {
        String generalizationId = specialization.getGeneralization();
        return () -> (G) idResolver.resolveReference(generalizationId);
    }

    // General helpers

    @SuppressWarnings("unchecked")
    public static <T extends CoreInstance> Supplier<T> packageableElementSupplier(Function<? super String, ? extends CoreInstance> packagePathResolver, String packagePath)
    {
        Objects.requireNonNull(packagePathResolver);
        Objects.requireNonNull(packagePath);
        return () ->
        {
            T result;
            try
            {
                result = (T) packagePathResolver.apply(packagePath);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error resolving '" + packagePath + "'", e);
            }
            if (result == null)
            {
                throw new RuntimeException("Cannot resolve '" + packagePath + "'");
            }
            return result;
        };
    }

    @SuppressWarnings("unchecked")
    public static <T extends CoreInstance> Supplier<T> packageableElementSupplier(ReferenceIdResolver refIdResolver, String packagePath)
    {
        Objects.requireNonNull(refIdResolver);
        Objects.requireNonNull(packagePath);
        return () ->
        {
            T result;
            try
            {
                result = (T) refIdResolver.resolvePackagePath(packagePath);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error resolving '" + packagePath + "'", e);
            }
            if (result == null)
            {
                throw new RuntimeException("Cannot resolve '" + packagePath + "'");
            }
            return result;
        };
    }

    public static IntFunction<CoreInstance> getVacuousInternalIdResolver()
    {
        return i ->
        {
            throw new IllegalArgumentException("Invalid internal id: " + i);
        };
    }
}
