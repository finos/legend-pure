// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.compiler.metadata;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.tools.ListHelper;
import java.util.Objects;

/**
 * Index of element paths that have back-reference data in a module. This is used to avoid unnecessary
 * {@code classLoader.getResource()} calls when loading back references: if the index is available for a module,
 * only elements listed in the index need to be probed for back-reference files.
 */
public class ModuleBackReferenceIndex
{
    private final String moduleName;
    private final ImmutableList<String> elementPaths;

    private ModuleBackReferenceIndex(String moduleName, ImmutableList<String> elementPaths)
    {
        this.moduleName = moduleName;
        this.elementPaths = elementPaths;
    }

    /**
     * Get the module name.
     *
     * @return module name
     */
    public String getModuleName()
    {
        return this.moduleName;
    }

    /**
     * Get the element paths that have back-reference data in this module.
     *
     * @return element paths with back-reference data
     */
    public ImmutableList<String> getElementPaths()
    {
        return this.elementPaths;
    }


    /**
     * Check whether the given element path has back-reference data in this module.
     *
     * @param elementPath element path
     * @return true if the element has back-reference data in this module
     */
    public boolean hasElement(String elementPath)
    {
        return this.elementPaths.contains(elementPath);
    }

    /**
     * Get the number of element paths with back-reference data.
     *
     * @return number of element paths
     */
    public int size()
    {
        return this.elementPaths.size();
    }

    /**
     * Check whether this index is empty (no elements have back-reference data).
     *
     * @return true if the index is empty
     */
    public boolean isEmpty()
    {
        return this.elementPaths.isEmpty();
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof ModuleBackReferenceIndex))
        {
            return false;
        }
        ModuleBackReferenceIndex that = (ModuleBackReferenceIndex) other;
        return this.moduleName.equals(that.moduleName) && this.elementPaths.equals(that.elementPaths);
    }

    @Override
    public int hashCode()
    {
        return this.moduleName.hashCode();
    }

    @Override
    public String toString()
    {
        return "<" + getClass().getSimpleName() + " module='" + this.moduleName + "' elements=" + this.elementPaths.size() + ">";
    }

    /**
     * Create a back-reference index from a {@link ModuleBackReferenceMetadata}, extracting the element paths
     * from its back references.
     *
     * @param metadata module back-reference metadata
     * @return back-reference index for the module
     */
    public static ModuleBackReferenceIndex fromBackReferenceMetadata(ModuleBackReferenceMetadata metadata)
    {
        MutableList<String> paths = Lists.mutable.ofInitialCapacity(metadata.getBackReferences().size());
        metadata.getBackReferences().forEach(br -> paths.add(br.getElementPath()));
        return new ModuleBackReferenceIndex(metadata.getModuleName(), ListHelper.sortAndRemoveDuplicates(paths).toImmutable());
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static Builder builder(int elementCount)
    {
        return new Builder(elementCount);
    }

    public static class Builder
    {
        private String moduleName;
        private final MutableList<String> elementPaths;

        private Builder()
        {
            this.elementPaths = Lists.mutable.empty();
        }

        private Builder(int elementCount)
        {
            this.elementPaths = Lists.mutable.ofInitialCapacity(elementCount);
        }

        public Builder withModuleName(String moduleName)
        {
            this.moduleName = Objects.requireNonNull(moduleName);
            return this;
        }

        public Builder addElementPath(String elementPath)
        {
            this.elementPaths.add(Objects.requireNonNull(elementPath));
            return this;
        }

        public Builder addElementPaths(Iterable<String> elementPaths)
        {
            elementPaths.forEach(this::addElementPath);
            return this;
        }

        public ModuleBackReferenceIndex build()
        {
            Objects.requireNonNull(this.moduleName, "module name is required");
            return new ModuleBackReferenceIndex(this.moduleName, ListHelper.sortAndRemoveDuplicates(this.elementPaths).toImmutable());
        }
    }
}
