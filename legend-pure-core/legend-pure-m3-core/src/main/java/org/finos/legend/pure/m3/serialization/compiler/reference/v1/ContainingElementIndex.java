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

package org.finos.legend.pure.m3.serialization.compiler.reference.v1;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

class ContainingElementIndex
{
    private final MapIterable<String, ImmutableList<CoreInstance>> sourceIndex;
    private final SetIterable<CoreInstance> virtualPackages;

    private ContainingElementIndex(MapIterable<String, ImmutableList<CoreInstance>> sourceIndex, SetIterable<CoreInstance> virtualPackages)
    {
        this.sourceIndex = sourceIndex;
        this.virtualPackages = virtualPackages;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof ContainingElementIndex))
        {
            return false;
        }
        ContainingElementIndex that = (ContainingElementIndex) other;
        return this.sourceIndex.equals(that.sourceIndex) && this.virtualPackages.equals(that.virtualPackages);
    }

    @Override
    public int hashCode()
    {
        return this.sourceIndex.hashCode() + (31 * this.virtualPackages.hashCode());
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append('{');
        this.sourceIndex.forEachKeyValue((source, elements) ->
        {
            builder.append(source).append(":[");
            elements.forEach(element ->
            {
                PackageableElement.writeUserPathForPackageableElement(builder, element);
                element.getSourceInformation().appendIntervalMessage(builder.append(" (")).append("), ");
            });
            builder.setLength(builder.length() - 2);
            builder.append("], ");
        });
        if (this.virtualPackages.isEmpty())
        {
            builder.setLength(builder.length() - 2);
            builder.append('}');
        }
        else
        {
            builder.append("virtual packages:[");
            this.virtualPackages.forEach(pkg -> PackageableElement.writeUserPathForPackageableElement(builder, pkg).append(", "));
            builder.setLength(builder.length() - 2);
            builder.append("]}");
        }
        return builder.toString();
    }

    CoreInstance findContainingElement(CoreInstance instance)
    {
        SourceInformation sourceInfo = instance.getSourceInformation();
        if (sourceInfo != null)
        {
            ImmutableList<CoreInstance> sourceElements = this.sourceIndex.get(sourceInfo.getSourceId());
            if (sourceElements != null)
            {
                // Binary search among elements sorted by position in the source
                int low = 0;
                int high = sourceElements.size() - 1;
                while (low <= high)
                {
                    int mid = (low + high) >>> 1;
                    CoreInstance element = sourceElements.get(mid);
                    if (element == instance)
                    {
                        return element;
                    }

                    SourceInformation elementSourceInfo = element.getSourceInformation();
                    if (SourceInformation.isBefore(sourceInfo.getEndLine(), sourceInfo.getEndColumn(), elementSourceInfo.getStartLine(), elementSourceInfo.getStartColumn()))
                    {
                        // instance ends before element starts
                        high = mid - 1;
                    }
                    else if (SourceInformation.isAfter(sourceInfo.getStartLine(), sourceInfo.getStartColumn(), elementSourceInfo.getEndLine(), elementSourceInfo.getEndColumn()))
                    {
                        // instance starts after element ends
                        low = mid + 1;
                    }
                    else
                    {
                        // instance and element must intersect, which by assumption means that element contains instance
                        return element;
                    }
                }
            }
        }
        else if (this.virtualPackages.contains(instance))
        {
            return instance;
        }
        return null;
    }

    static Builder builder(ProcessorSupport processorSupport)
    {
        return new Builder(processorSupport);
    }

    static class Builder
    {
        private final MutableMap<String, MutableList<CoreInstance>> elementsBySource = Maps.mutable.empty();
        private final MutableList<CoreInstance> virtualPackages = Lists.mutable.empty();
        private final ProcessorSupport processorSupport;

        private Builder(ProcessorSupport processorSupport)
        {
            this.processorSupport = processorSupport;
        }

        void addElement(CoreInstance element)
        {
            SourceInformation sourceInfo = Objects.requireNonNull(element, "element may not be null").getSourceInformation();
            if (sourceInfo == null)
            {
                if (_Package.isPackage(element, this.processorSupport))
                {
                    this.virtualPackages.add(element);
                    return;
                }
                throw new IllegalArgumentException("Invalid element, no source information: " + element);
            }
            if (!sourceInfo.isValid())
            {
                throw new IllegalArgumentException("Element with invalid source information: " + element + ", " + sourceInfo.getMessage());
            }
            this.elementsBySource.getIfAbsentPut(sourceInfo.getSourceId(), Lists.mutable::empty).add(element);
        }

        Builder withElement(CoreInstance element)
        {
            addElement(element);
            return this;
        }

        void addElements(Iterable<? extends CoreInstance> elements)
        {
            elements.forEach(this::addElement);
        }

        Builder withElements(Iterable<? extends CoreInstance> elements)
        {
            addElements(elements);
            return this;
        }

        void addAllElements()
        {
            addElement(this.processorSupport.repository_getTopLevel(M3Paths.Package));
            PrimitiveUtilities.forEachPrimitiveType(this.processorSupport, this::addElement);
            Deque<CoreInstance> packages = new ArrayDeque<>();
            packages.add(this.processorSupport.repository_getTopLevel(M3Paths.Root));
            while (!packages.isEmpty())
            {
                CoreInstance pkg = packages.pollFirst();
                addElement(pkg);
                pkg.getValueForMetaPropertyToMany(M3Properties.children).forEach(child ->
                {
                    if (_Package.isPackage(child, this.processorSupport))
                    {
                        packages.addLast(child);
                    }
                    else
                    {
                        addElement(child);
                    }
                });
            }
        }

        Builder withAllElements()
        {
            addAllElements();
            return this;
        }

        ContainingElementIndex build()
        {
            MutableMap<String, ImmutableList<CoreInstance>> index = Maps.mutable.ofInitialCapacity(this.elementsBySource.size());
            this.elementsBySource.forEachKeyValue((source, elements) ->
            {
                if (elements.size() > 1)
                {
                    elements.sortThis((e1, e2) -> SourceInformation.compareByStartPosition(e1.getSourceInformation(), e2.getSourceInformation()));
                    CoreInstance[] prev = new CoreInstance[1];
                    elements.removeIf(current ->
                    {
                        CoreInstance previous = prev[0];
                        if (current == previous)
                        {
                            return true;
                        }
                        if ((previous != null) && overlaps(current, previous))
                        {
                            StringBuilder builder = new StringBuilder("Distinct elements with overlapping source information: ");
                            PackageableElement.writeUserPathForPackageableElement(builder, previous);
                            previous.getSourceInformation().appendMessage(builder.append(" (")).append("), ");
                            PackageableElement.writeUserPathForPackageableElement(builder, current);
                            current.getSourceInformation().appendMessage(builder.append(" (")).append(")");
                            throw new IllegalStateException(builder.toString());
                        }
                        prev[0] = current;
                        return false;
                    });
                }
                if (elements.notEmpty())
                {
                    index.put(source, elements.toImmutable());
                }
            });
            return new ContainingElementIndex(index, this.virtualPackages.isEmpty() ? Sets.immutable.empty() : Sets.mutable.withAll(this.virtualPackages));
        }

        private static boolean overlaps(CoreInstance element1, CoreInstance element2)
        {
            SourceInformation sourceInfo1 = element1.getSourceInformation();
            SourceInformation sourceInfo2 = element2.getSourceInformation();
            return SourceInformation.isNotAfter(sourceInfo1.getStartLine(), sourceInfo1.getStartColumn(), sourceInfo2.getEndLine(), sourceInfo2.getEndColumn()) &&
                    SourceInformation.isNotBefore(sourceInfo1.getEndLine(), sourceInfo1.getEndColumn(), sourceInfo2.getStartLine(), sourceInfo2.getStartColumn());
        }
    }
}
