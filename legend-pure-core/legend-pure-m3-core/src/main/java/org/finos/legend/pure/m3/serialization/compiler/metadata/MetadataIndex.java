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
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MetadataIndex
{
    private final ModuleIndex modules;
    private final ElementIndex elements;
    private final PackageIndex packages;

    MetadataIndex(Iterable<? extends ModuleManifest> modules)
    {
        this.modules = ModuleIndex.buildIndex(modules);
        this.elements = ElementIndex.buildIndex(this.modules);
        this.packages = PackageIndex.buildIndex(this.elements);
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
}
