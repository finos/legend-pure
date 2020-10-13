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

package org.finos.legend.pure.m3.navigation.linearization;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Stacks;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * C3 linearization of a Pure M3 type hierarchy.
 */
public class C3Linearization implements Function<CoreInstance, ImmutableList<CoreInstance>>
{

    private final TypeSupport typeSupport;
    private final MutableStack<CoreInstance> stack = Stacks.mutable.with();
    private final ProcessorSupport processorSupport;

    private C3Linearization(TypeSupport typeSupport, ProcessorSupport processorSupport)
    {
        this.typeSupport = typeSupport;
        this.processorSupport = processorSupport;
    }

    private ListIterable<CoreInstance> getLinearization(CoreInstance type, ProcessorSupport processorSupport) throws InconsistentGeneralizationHierarchyException
    {
        if (this.stack.contains(type))
        {
            throw new InconsistentGeneralizationHierarchyException(type);
        }
        this.stack.push(type);
        ImmutableList<CoreInstance> linearization = this.typeSupport.getGeneralizations(type, this, processorSupport);
        this.stack.pop();
        return linearization;
    }

    /**
     * Calculate the linearization of the generalization hierarchy from the
     * given type.
     *
     * @param type type
     * @return linearization of type's generalization hierarchy
     * @throws InconsistentGeneralizationHierarchyException
     */
    private ImmutableList<CoreInstance> calculateLinearization(CoreInstance type, ProcessorSupport processorSupport) throws InconsistentGeneralizationHierarchyException
    {
        ListIterable<CoreInstance> generalizations = this.typeSupport.getDirectGeneralizations(type, processorSupport);
        if (generalizations.isEmpty())
        {
            return Lists.immutable.with(type);
        }
        else
        {
            LinkedList<TypeQueue> linearizations = new LinkedList<TypeQueue>();
            linearizations.add(new TypeQueue(Lists.immutable.with(type)));

            for (CoreInstance generalization : generalizations)
            {
                try
                {
                    linearizations.add(new TypeQueue(getLinearization(generalization, processorSupport)));
                }
                catch (InconsistentGeneralizationHierarchyException e)
                {
                    // An inconsistency was found in the generalization hierarchy of one of type's generalizations.
                    throw new InconsistentGeneralizationHierarchyException(type, e);
                }
            }
            linearizations.add(new TypeQueue(generalizations));
            try
            {
                return Lists.immutable.withAll(merge(linearizations, processorSupport));
            }
            catch (C3LinearizationConflictException e)
            {
                // An inconsistency was found in type's generalization hierarchy.
                throw new InconsistentGeneralizationHierarchyException(type);
            }
        }
    }

    private MutableList<CoreInstance> merge(LinkedList<TypeQueue> linearizations, ProcessorSupport processorSupport) throws C3LinearizationConflictException
    {
        MutableList<CoreInstance> result = Lists.mutable.with();
        while (!linearizations.isEmpty())
        {
            // Find next element
            CoreInstance candidate = null;
            for (TypeQueue linearization : linearizations)
            {
                candidate = linearization.peek();
                // Check if candidate appears in any other linearization in any position other than the first.
                // If so, we reject it as the next element in the result.
                for (TypeQueue other : linearizations)
                {
                    if ((other != linearization) && other.containsAfterFirst(candidate))
                    {
                        candidate = null;
                        break;
                    }
                }
                if (candidate != null)
                {
                    break;
                }
            }
            if (candidate == null)
            {
                throw new C3LinearizationConflictException();
            }
            result.add(candidate);

            // Remove element from other linearizations and remove empty linearizations
            ListIterator<TypeQueue> iterator = linearizations.listIterator();
            while (iterator.hasNext())
            {
                TypeQueue linearization = iterator.next();
                if (this.typeSupport.check_typeEquality(candidate, linearization.peek(), processorSupport))
                {
                    linearization.pop();
                }
                if (linearization.isEmpty())
                {
                    iterator.remove();
                }
            }
        }
        return result;
    }

    @Override
    public ImmutableList<CoreInstance> valueOf(CoreInstance type)
    {
        return calculateLinearization(type, this.processorSupport);
    }

    /**
     * Get the linearization of the generalization hierarchy starting
     * at the given type, according to the C3 linearization algorithm.
     * The first element of the list will always be the type itself.  An
     * exception will be thrown if the hierarchy is inconsistent.
     *
     * @param type             starting type
     * @param processorSupport processor support
     * @return generalization linearization
     * @throws PureCompilationException if the generalization hierarchy is inconsistent
     */
    public static ListIterable<CoreInstance> getTypeGeneralizationLinearization(CoreInstance type, ProcessorSupport processorSupport) throws PureCompilationException
    {
        return getGeneralizationLinearization(type, TypeTypeSupport.INSTANCE, processorSupport);
    }

    /**
     * Get the linearization of the generalization hierarchy starting
     * at the given generic type, according to the C3 linearization
     * algorithm.  The first element of the list will always be the
     * generic type itself.  An exception will be thrown if the
     * hierarchy is inconsistent.
     *
     * @param genericType      starting generic type
     * @param processorSupport processor support
     * @return generalization linearization
     * @throws PureCompilationException if the generalization hierarchy is inconsistent
     */
    public static ListIterable<CoreInstance> getGenericTypeGeneralizationLinearization(CoreInstance genericType, ProcessorSupport processorSupport) throws PureCompilationException
    {
        return getGeneralizationLinearization(genericType, GenericTypeTypeSupport.INSTANCE, processorSupport);
    }

    private static ListIterable<CoreInstance> getGeneralizationLinearization(CoreInstance type, TypeSupport typeSupport, ProcessorSupport processorSupport) throws PureCompilationException
    {
        try
        {
            return new C3Linearization(typeSupport, processorSupport).getLinearization(type, processorSupport);
        }
        catch (InconsistentGeneralizationHierarchyException e)
        {
            MutableList<CoreInstance> clsPath = e.getInconsistentTypePath();
            CoreInstance rootType = clsPath.getLast();
            if (clsPath.size() == 1)
            {
                throw new PureCompilationException(rootType.getSourceInformation(), e.getMessage());
            }
            else
            {
                throw new PureCompilationException(rootType.getSourceInformation(), e.getMessage() + "; root inconsistent class: " + rootType + "; path to root class: " + clsPath.makeString(", "));
            }
        }
    }

    private class TypeQueue
    {
        private final ListIterable<CoreInstance> list;
        private int start = 0;

        private TypeQueue(ListIterable<CoreInstance> list)
        {
            this.list = list;
        }

        public CoreInstance peek()
        {
            return this.list.get(this.start);
        }

        public CoreInstance pop()
        {
            return this.list.get(this.start++);
        }

        public boolean isEmpty()
        {
            return this.start >= this.list.size();
        }

        public boolean containsAfterFirst(CoreInstance type)
        {
            TypeSupport typeSupport = C3Linearization.this.typeSupport;
            for (int i = this.start + 1, size = this.list.size(); i < size; i++)
            {
                if (typeSupport.check_typeEquality(type, this.list.get(i), C3Linearization.this.processorSupport))
                {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Exception thrown when a type is determined to have an inconsistent
     * generalization hierarchy.
     */
    private static final class InconsistentGeneralizationHierarchyException extends RuntimeException
    {
        private final CoreInstance type;

        InconsistentGeneralizationHierarchyException(CoreInstance type, InconsistentGeneralizationHierarchyException e)
        {
            super("Inconsistent generalization hierarchy for " + type, e);
            this.type = type;
        }

        InconsistentGeneralizationHierarchyException(CoreInstance type)
        {
            this(type, null);
        }

        /**
         * Get the type whose hierarchy is inconsistent.
         *
         * @return type
         */
        CoreInstance getType()
        {
            return this.type;
        }

        /**
         * Get the generalization path from the type associated with this
         * exception to the type which is the root of the inconsistency.
         *
         * @return generalization path to root inconsistent type
         */
        MutableList<CoreInstance> getInconsistentTypePath()
        {
            Throwable cause = getCause();
            if (cause == null)
            {
                return Lists.mutable.with(getType());
            }
            else
            {
                MutableList<CoreInstance> types = ((InconsistentGeneralizationHierarchyException)cause).getInconsistentTypePath();
                types.add(0, getType());
                return types;
            }
        }
    }

    /**
     * Exception thrown when a linearization conflict is discovered.
     */
    private static final class C3LinearizationConflictException extends Exception
    {
    }
}
