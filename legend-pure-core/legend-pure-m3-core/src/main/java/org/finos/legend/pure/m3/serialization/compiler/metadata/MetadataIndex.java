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

package org.finos.legend.pure.m3.serialization.compiler.metadata;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MetadataIndex
{
    private final ModuleIndex modules;
    private final ElementIndex elements;
    private final PackageIndex packages;
    private final MapIterable<String, String> elementModuleIndex;
    private final MapIterable<String, ImmutableList<String>> moduleReverseDependencyIndex;

    MetadataIndex(Iterable<? extends ModuleManifest> modules)
    {
        this.modules = ModuleIndex.buildIndex(modules);
        this.elements = ElementIndex.buildIndex(this.modules);
        this.packages = PackageIndex.buildIndex(this.elements);
        this.elementModuleIndex = buildElementModuleIndex(this.modules);
        this.moduleReverseDependencyIndex = buildModuleReverseDependencyIndex(this.modules);
    }

    @Override
    public boolean equals(Object other)
    {
        return (this == other) || ((other instanceof MetadataIndex) && this.modules.equals(((MetadataIndex) other).modules));
    }

    @Override
    public int hashCode()
    {
        return this.modules.hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("<").append(getClass().getSimpleName());
        this.modules.getAllModuleNames().appendString(builder, " modules=[", ", ", "]>");
        this.elements.getAllElementPaths().appendString(builder, " elements=[", ", ", "]>");
        return builder.toString();
    }

    // Modules

    public boolean hasModule(String moduleName)
    {
        return this.modules.hasModule(moduleName);
    }

    public Iterable<String> getAllModuleNames()
    {
        return this.modules.getAllModuleNames();
    }

    public ModuleManifest getModule(String moduleName)
    {
        return this.modules.getModule(moduleName);
    }

    public Iterable<ModuleManifest> getAllModules()
    {
        return this.modules.getAllModules();
    }

    public void forEachModule(Consumer<? super ModuleManifest> consumer)
    {
        this.modules.forEachModule(consumer);
    }

    // Elements by path

    public int getElementCount()
    {
        return this.elements.getElementCount();
    }

    public boolean hasElement(String path)
    {
        return this.elements.hasElement(path);
    }

    public Iterable<String> getAllElementPaths()
    {
        return this.elements.getAllElementPaths();
    }

    public ConcreteElementMetadata getElement(String path)
    {
        return this.elements.getElement(path);
    }

    public Iterable<ConcreteElementMetadata> getAllElements()
    {
        return this.elements.getAllElements();
    }

    public void forEachElement(Consumer<? super ConcreteElementMetadata> consumer)
    {
        this.elements.forEachElement(consumer);
    }

    // Elements by source

    public boolean hasSource(String sourceId)
    {
        return this.elements.hasSource(sourceId);
    }

    public Iterable<String> getAllSources()
    {
        return this.elements.getAllSources();
    }

    public ImmutableList<ConcreteElementMetadata> getSourceElements(String sourceId)
    {
        return this.elements.getSourceElements(sourceId);
    }

    public void forEachSourceWithElements(BiConsumer<? super String, ? super ImmutableList<ConcreteElementMetadata>> consumer)
    {
        this.elements.forEachSourceWithElements(consumer);
    }

    // Elements by classifier

    public boolean hasClassifier(String path)
    {
        return this.elements.hasClassifier(path);
    }

    public Iterable<String> getAllClassifiers()
    {
        return this.elements.getAllClassifiers();
    }

    public ImmutableList<ConcreteElementMetadata> getClassifierElements(String classifierPath)
    {
        return this.elements.getClassifierElements(classifierPath);
    }

    public void forEachClassifierWithElements(BiConsumer<? super String, ? super ImmutableList<ConcreteElementMetadata>> consumer)
    {
        this.elements.forEachClassifierWithElements(consumer);
    }

    // Packages

    public int getTopLevelElementCount()
    {
        return this.packages.getTopLevelElementCount();
    }

    public ImmutableList<ConcreteElementMetadata> getTopLevelElements()
    {
        return this.packages.getTopLevelElements();
    }

    public int getPackageCount()
    {
        return this.packages.getPackageCount();
    }

    public boolean hasPackage(String packagePath)
    {
        return this.packages.hasPackage(packagePath);
    }

    public Iterable<String> getAllPackagePaths()
    {
        return this.packages.getAllPackagePaths();
    }

    public Iterable<PackageableElementMetadata> getAllPackageMetadata()
    {
        return this.packages.getAllPackageMetadata();
    }

    public PackageableElementMetadata getPackageMetadata(String packagePath)
    {
        return this.packages.getPackageMetadata(packagePath);
    }

    public ImmutableList<PackageableElementMetadata> getPackageChildren(String packagePath)
    {
        return this.packages.getPackageChildren(packagePath);
    }

    public void forEachPackage(Consumer<? super PackageableElementMetadata> consumer)
    {
        this.packages.forEachPackage(consumer);
    }

    public void forEachVirtualPackage(Consumer<? super VirtualPackageMetadata> consumer)
    {
        this.packages.forEachVirtualPackage(consumer);
    }

    // Element module

    /**
     * Get the name of the module that contains the given element.
     *
     * @param elementPath element path
     * @return module name, or null if the element is not found
     */
    public String getElementModuleName(String elementPath)
    {
        return this.elementModuleIndex.get(elementPath);
    }

    // Back reference modules

    /**
     * Get the module names that could have back-reference data for the given element path. For a concrete element, this
     * is the element's own module plus all modules that transitively depend on it. For a virtual package or unknown
     * element, this returns all module names.
     *
     * @param elementPath element path
     * @return module names that could have back-reference data
     */
    public Iterable<String> getBackReferenceModuleNames(String elementPath)
    {
        String moduleName = this.elementModuleIndex.get(elementPath);
        if (moduleName != null)
        {
            ImmutableList<String> result = this.moduleReverseDependencyIndex.get(moduleName);
            if (result != null)
            {
                return result;
            }
        }
        // Fall back to all modules for virtual packages or unknown elements
        return getAllModuleNames();
    }

    // Builder

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private final MutableList<ModuleManifest> modules = Lists.mutable.empty();

        private Builder()
        {
        }

        public Builder withModule(ModuleManifest module)
        {
            this.modules.add(Objects.requireNonNull(module));
            return this;
        }

        public Builder withModules(Iterable<? extends ModuleManifest> modules)
        {
            modules.forEach(this::withModule);
            return this;
        }

        public Builder withModules(ModuleManifest... modules)
        {
            return withModules(Arrays.asList(modules));
        }

        public MetadataIndex build()
        {
            return new MetadataIndex(this.modules);
        }
    }

    private static MapIterable<String, String> buildElementModuleIndex(ModuleIndex modules)
    {
        MutableMap<String, String> index = Maps.mutable.empty();
        modules.forEachModule(module ->
        {
            String moduleName = module.getModuleName();
            module.forEachElement(element -> index.put(element.getPath(), moduleName));
        });
        return index;
    }

    private static MapIterable<String, ImmutableList<String>> buildModuleReverseDependencyIndex(ModuleIndex modules)
    {
        // Step 1: Build direct reverse dependencies (module -> modules that directly depend on it)
        MutableMap<String, MutableList<String>> reverseDeps = Maps.mutable.empty();
        modules.forEachModule(module ->
        {
            String moduleName = module.getModuleName();
            reverseDeps.getIfAbsentPut(moduleName, Lists.mutable::empty);
            module.getDependencies().forEach(dep -> reverseDeps.getIfAbsentPut(dep, Lists.mutable::empty).add(moduleName));
        });

        // Step 2: Compute transitive closure for each module
        // For module M, the result is {M} ∪ {all modules that transitively depend on M}
        MutableMap<String, ImmutableList<String>> result = Maps.mutable.ofInitialCapacity(reverseDeps.size());
        reverseDeps.forEachKey(moduleName ->
        {
            MutableSet<String> visited = Sets.mutable.empty();
            Deque<String> queue = new ArrayDeque<>();
            queue.add(moduleName);
            while (!queue.isEmpty())
            {
                String current = queue.poll();
                if (visited.add(current))
                {
                    MutableList<String> dependents = reverseDeps.get(current);
                    if (dependents != null)
                    {
                        dependents.reject(visited::contains, queue);
                    }
                }
            }
            result.put(moduleName, visited.toSortedList().toImmutable());
        });

        return result;
    }
}
