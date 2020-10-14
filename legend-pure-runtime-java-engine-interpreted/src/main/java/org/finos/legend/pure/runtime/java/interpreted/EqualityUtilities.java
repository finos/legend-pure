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

package org.finos.legend.pure.runtime.java.interpreted;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy;
import org.eclipse.collections.impl.set.strategy.mutable.UnifiedSetWithHashingStrategy;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.PrimitiveCoreInstance;

/**
 * Utilities related to equality between Pure instances.  This is equality in
 * the sense of the "equal" function in Pure.
 */
public class EqualityUtilities
{
    private EqualityUtilities()
    {
        // Utility class
    }

    // Equality

    /**
     * Return whether two instances are equal according to
     * the Pure function "eq".
     *
     * @param left             left instance
     * @param right            right instance
     * @param processorSupport processor support
     * @return whether left and right are equal in Pure
     */
    public static boolean eq(CoreInstance left, CoreInstance right, ProcessorSupport processorSupport)
    {
        if (left == right)
        {
            return true;
        }

        if ((left instanceof PrimitiveCoreInstance<?>) && (right instanceof PrimitiveCoreInstance<?>))
        {
            return left.getClassifier().getName().equals(right.getClassifier().getName()) &&
                    ((PrimitiveCoreInstance<?>)left).getValue().equals(((PrimitiveCoreInstance<?>)right).getValue());
        }

        CoreInstance type = left.getClassifier();
        if (type != right.getClassifier())
        {
            return false;
        }

        return Type.isPrimitiveType(type, processorSupport) && left.getName().equals(right.getName());
    }

    /**
     * Return whether two instances are equal according to
     * the Pure function "equal".
     *
     * @param left             left instance
     * @param right            right instance
     * @param processorSupport processor support
     * @return whether left and right are equal in Pure
     */
    public static boolean equal(CoreInstance left, CoreInstance right, RichIterable<? extends CoreInstance> external, ProcessorSupport processorSupport) //NOSONAR signature is clear enough
    {
        if (left == right)
        {
            return true;
        }

        if ((left instanceof PrimitiveCoreInstance<?>) && (right instanceof PrimitiveCoreInstance<?>))
        {
            return left.getClassifier().getName().equals(right.getClassifier().getName()) &&
                    ((PrimitiveCoreInstance<?>)left).getValue().equals(((PrimitiveCoreInstance<?>)right).getValue());
        }

        CoreInstance type = left.getClassifier();
        if (type != right.getClassifier())
        {
            return false;
        }

        if (Type.isPrimitiveType(type, processorSupport))
        {
            return left.getName().equals(right.getName());
        }

        if (Instance.instanceOf(left, M3Paths.InstanceValue, processorSupport))
        {
            return equal(ValueSpecification.getValues(left, processorSupport), ValueSpecification.getValues(right, processorSupport), processorSupport);
        }

        ListIterable<CoreInstance> keys = _Class.getEqualityKeyProperties(type, processorSupport);
        if (external.isEmpty() && keys.isEmpty())
        {
            return false;
        }

        for (CoreInstance key : external.notEmpty()?external:keys)
        {
            if (!equal(Instance.getValueForMetaPropertyToManyResolved(left, key, processorSupport), Instance.getValueForMetaPropertyToManyResolved(right, key, processorSupport), processorSupport))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Return whether two lists of Pure instances are equal according
     * to the Pure function equal.
     *
     * @param left    left instances
     * @param right   right instances
     * @return whether the lists are equal in Pure
     */
    public static boolean equal(ListIterable<? extends CoreInstance> left, ListIterable<? extends CoreInstance> right, ProcessorSupport processorSupport) //NOSONAR signature is clear enough
    {
        int size = left.size();
        if (right.size() != size)
        {
            return false;
        }
        for (int i = 0; i < size; i++)
        {
            if (!equal(left.get(i), right.get(i), Lists.immutable.<CoreInstance>empty(), processorSupport))
            {
                return false;
            }
        }
        return true;
    }

    // Hashing

    /**
     * Return a hash code for a Pure instance consistent with the
     * Pure definition of equality.  So if two instances are equal
     * in Pure, then this function will return the same hash code
     * for both.
     *
     * @param instance Pure instance
     * @return Pure hash code
     */
    public static int coreInstanceHashCode(CoreInstance instance, ListIterable<? extends CoreInstance> externalKeys, ProcessorSupport processorSupport)
    {
        CoreInstance type = instance.getClassifier();
        if (Type.isPrimitiveType(type, processorSupport))
        {
            return type.hashCode() ^ instance.getName().hashCode();
        }

        if (Instance.instanceOf(instance, M3Paths.ValueSpecification, processorSupport))
        {
            int hashCode = type.hashCode();
            for (CoreInstance value : ValueSpecification.getValues(instance, processorSupport))
            {
                hashCode = (31 * hashCode) + coreInstanceHashCode(value, externalKeys, processorSupport);
            }
            return hashCode;
        }

        ListIterable<? extends CoreInstance> keys = ((externalKeys == null) || externalKeys.isEmpty()) ? _Class.getEqualityKeyProperties(instance.getClassifier(), processorSupport) : externalKeys;
        if (keys.isEmpty())
        {
            return instance.hashCode();
        }
        int hashCode = type.hashCode();
        for (CoreInstance key : keys)
        {
            for (CoreInstance value : Instance.getValueForMetaPropertyToManyResolved(instance, key, processorSupport))
            {
                hashCode = (31 * hashCode) + coreInstanceHashCode(value, externalKeys, processorSupport);
            }
        }
        return hashCode;
    }

    /**
     * Return a hashing strategy for Pure instances based on
     * Pure equality.
     *
     * @return Pure instance hashing strategy
     */
    public static HashingStrategy<CoreInstance> newCoreInstanceHashingStrategy(ProcessorSupport processorSupport)
    {
        return new CoreInstanceHashingStrategy(Lists.immutable.<CoreInstance>empty(), processorSupport);
    }

    /**
     * Return a hashing strategy for Pure instances based on
     * given keys.
     *
     * @return Pure instance hashing strategy
     */
    public static HashingStrategy<CoreInstance> newCoreInstanceHashingStrategy(ListIterable<? extends CoreInstance> keys, ProcessorSupport processorSupport)
    {
        return new CoreInstanceHashingStrategy(keys, processorSupport);
    }

    // Containers

    /**
     * Return whether an iterable contains a Pure instance according
     * to Pure equality.  Note that this will only take advantage of
     * hashing if the iterable is a set created by this class.
     *
     * @param iterable iterable
     * @param instance Pure instance
     * @return whether iterable contains instance
     */
    public static boolean containsCoreInstance(Iterable<? extends CoreInstance> iterable, CoreInstance instance, ProcessorSupport processorSupport)
    {
        if (iterable instanceof CoreInstanceMutableSet)
        {
            return ((CoreInstanceMutableSet)iterable).contains(instance);
        }
        else
        {
            for (CoreInstance ins : iterable)
            {
                if (equal(instance, ins, Lists.immutable.<CoreInstance>empty(), processorSupport))
                {
                    return true;
                }
            }
            return false;
        }
    }

    // Sets

    /**
     * Return a set for Pure instances with a hashing strategy
     * based on Pure equality.
     *
     * @param <T>     set element type
     * @return Pure instance set
     */
    public static <T extends CoreInstance> MutableSet<T> newCoreInstanceSet(ProcessorSupport processorSupport)
    {
        return new CoreInstanceMutableSet<T>(processorSupport);
    }

    /**
     * Return a set for Pure instances with a hashing strategy
     * based on Pure equality.
     *
     * @param <T>     set element type
     * @param size    initial size
     * @return Pure instance set
     */
    public static <T extends CoreInstance> MutableSet<T> newCoreInstanceSet(ProcessorSupport processorSupport, int size)
    {
        return new CoreInstanceMutableSet<T>(processorSupport, size);
    }

    // Maps

    /**
     * Return a map with Pure instances as keys and a hashing
     * strategy based on Pure equality.
     *
     * @param <K>     key type
     * @param <V>     value type
     * @return Pure instance map
     */
    public static <K extends CoreInstance, V> MutableMap<K, V> newCoreInstanceMap(ProcessorSupport processorSupport)
    {
        return new CoreInstanceMutableMap<K, V>(processorSupport);
    }

    /**
     * Return a map with Pure instances as keys and a hashing
     * strategy based on Pure equality.
     *
     * @param <K>     key type
     * @param <V>     value type
     * @param size    initial size
     * @return Pure instance map
     */
    public static <K extends CoreInstance, V> MutableMap<K, V> newCoreInstanceMap(ProcessorSupport processorSupport, int size)
    {
        return new CoreInstanceMutableMap<K, V>(processorSupport, size);
    }

    // Hashing

    private static class CoreInstanceHashingStrategy implements HashingStrategy<CoreInstance>
    {
        private final ProcessorSupport processorSupport;
        private final ListIterable<? extends CoreInstance> properties;

        private CoreInstanceHashingStrategy(ListIterable<? extends CoreInstance> properties, ProcessorSupport processorSupport)
        {
            this.processorSupport = processorSupport;
            this.properties = properties;
        }

        @Override
        public int computeHashCode(CoreInstance instance)
        {
            return coreInstanceHashCode(instance, this.properties, this.processorSupport);
        }

        @Override
        public boolean equals(CoreInstance instance, CoreInstance instance2)
        {
            return equal(instance, instance2, this.properties , this.processorSupport);
        }
    }

    private static class CoreInstanceMutableSet<T extends CoreInstance> extends UnifiedSetWithHashingStrategy<T>
    {
        public CoreInstanceMutableSet()
        {
        }

        private CoreInstanceMutableSet(ProcessorSupport processorSupport)
        {
            super(newCoreInstanceHashingStrategy(processorSupport));
        }

        private CoreInstanceMutableSet(ProcessorSupport processorSupport, int size)
        {
            super(newCoreInstanceHashingStrategy(processorSupport), size);
        }
    }

    private static class CoreInstanceMutableMap<K extends CoreInstance, V> extends UnifiedMapWithHashingStrategy<K, V>
    {
        public CoreInstanceMutableMap()
        {
        }

        private CoreInstanceMutableMap(ProcessorSupport processorSupport)
        {
            super(newCoreInstanceHashingStrategy(processorSupport));
        }

        private CoreInstanceMutableMap(ProcessorSupport processorSupport, int size)
        {
            super(newCoreInstanceHashingStrategy(processorSupport), size);
        }
    }
}
