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
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class ModuleMetadata
{
    private final ModuleManifest manifest;
    private final ModuleSourceMetadata sourceMetadata;
    private final ModuleExternalReferenceMetadata externalReferenceMetadata;
    private final ModuleBackReferenceMetadata backReferenceMetadata;
    private final ModuleFunctionNameMetadata functionNameMetadata;

    private ModuleMetadata(ModuleManifest manifest, ModuleSourceMetadata sourceMetadata, ModuleExternalReferenceMetadata externalReferenceMetadata, ModuleBackReferenceMetadata backReferenceMetadata, ModuleFunctionNameMetadata functionNameMetadata)
    {
        this.manifest = manifest;
        this.sourceMetadata = sourceMetadata;
        this.externalReferenceMetadata = externalReferenceMetadata;
        this.backReferenceMetadata = backReferenceMetadata;
        this.functionNameMetadata = functionNameMetadata;
    }

    public String getName()
    {
        return this.manifest.getModuleName();
    }

    public ModuleManifest getManifest()
    {
        return this.manifest;
    }

    public ModuleSourceMetadata getSourceMetadata()
    {
        return this.sourceMetadata;
    }

    public ModuleExternalReferenceMetadata getExternalReferenceMetadata()
    {
        return this.externalReferenceMetadata;
    }

    public ModuleBackReferenceMetadata getBackReferenceMetadata()
    {
        return this.backReferenceMetadata;
    }

    public ModuleFunctionNameMetadata getFunctionNameMetadata()
    {
        return this.functionNameMetadata;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof ModuleMetadata))
        {
            return false;
        }

        ModuleMetadata that = (ModuleMetadata) other;
        return this.manifest.equals(that.manifest) &&
                this.sourceMetadata.equals(that.sourceMetadata) &&
                this.externalReferenceMetadata.equals(that.externalReferenceMetadata) &&
                this.backReferenceMetadata.equals(that.backReferenceMetadata) &&
                this.functionNameMetadata.equals(that.functionNameMetadata);
    }

    @Override
    public int hashCode()
    {
        return getName().hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("<").append(getClass().getSimpleName())
                .append(" name='").append(getName()).append("' ");
        this.manifest.getDependencies().appendString(builder, "dependencies=[", ", ", "] ");
        ImmutableList<ConcreteElementMetadata> elements = this.manifest.getElements();
        builder.append("elements=[");
        if (elements.notEmpty())
        {
            elements.forEach(e -> e.appendString(builder.append('{')).append("}, "));
            builder.setLength(builder.length() - 2);
        }
        builder.append("] sources=[");
        ImmutableList<SourceMetadata> sources = this.sourceMetadata.getSources();
        if (sources.notEmpty())
        {
            sources.forEach(source -> source.appendString(builder.append('{')).append("}, "));
            builder.setLength(builder.length() - 2);
        }
        builder.append("] externalReferences=[");
        ImmutableList<ElementExternalReferenceMetadata> extRefs = this.externalReferenceMetadata.getExternalReferences();
        if (extRefs.notEmpty())
        {
            extRefs.forEach(extRef -> extRef.appendString(builder.append('{')).append("}, "));
            builder.setLength(builder.length() - 2);
        }
        builder.append("] backReferences=[");
        ImmutableList<ElementBackReferenceMetadata> backRefs = this.backReferenceMetadata.getBackReferences();
        if (backRefs.notEmpty())
        {
            backRefs.forEach(backRef -> backRef.appendString(builder.append('{')).append("}, "));
            builder.setLength(builder.length() - 2);
        }
        builder.append("] functionsByName=[");
        ImmutableList<FunctionsByName> functionsByNames = this.functionNameMetadata.getFunctionsByName();
        if (functionsByNames.notEmpty())
        {
            functionsByNames.forEach(fbn -> fbn.appendString(builder.append('{')).append("}, "));
            builder.setLength(builder.length() - 2);
        }
        return builder.append("]>").toString();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static Builder builder(String name)
    {
        return builder().withName(name);
    }

    public static Builder builder(ModuleMetadata metadata)
    {
        return new Builder(metadata);
    }

    public static class Builder
    {
        private String name;
        private final ModuleManifest.Builder manifestBuilder;
        private final ModuleSourceMetadata.Builder sourceMetadataBuilder;
        private final ModuleExternalReferenceMetadata.Builder extRefBuilder;
        private final ModuleBackReferenceMetadata.Builder backRefBuilder;
        private final MutableMap<String, MutableList<String>> functionsByName;

        private Builder()
        {
            this.manifestBuilder = ModuleManifest.builder();
            this.sourceMetadataBuilder = ModuleSourceMetadata.builder();
            this.extRefBuilder = ModuleExternalReferenceMetadata.builder();
            this.backRefBuilder = ModuleBackReferenceMetadata.builder();
            this.functionsByName = Maps.mutable.empty();
        }

        private Builder(ModuleMetadata metadata)
        {
            this.name = metadata.getName();
            this.manifestBuilder = ModuleManifest.builder(metadata.manifest);
            this.sourceMetadataBuilder = ModuleSourceMetadata.builder(metadata.sourceMetadata);
            this.extRefBuilder = ModuleExternalReferenceMetadata.builder(metadata.externalReferenceMetadata);
            this.backRefBuilder = ModuleBackReferenceMetadata.builder(metadata.backReferenceMetadata);
            this.functionsByName = Maps.mutable.ofInitialCapacity(metadata.functionNameMetadata.getFunctionsByName().size());
            metadata.functionNameMetadata.getFunctionsByName().forEach(fbn -> this.functionsByName.put(fbn.getFunctionName(), fbn.getFunctions().toList()));
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public void setReferenceIdVersion(Integer version)
        {
            this.extRefBuilder.setReferenceIdVersion(version);
            this.backRefBuilder.setReferenceIdVersion(version);
        }

        public void addElement(ConcreteElementMetadata element)
        {
            this.manifestBuilder.addElement(element);
        }

        public void addDependency(String dependency)
        {
            this.manifestBuilder.addDependency(dependency);
        }

        public void addDependencies(Iterable<? extends String> dependencies)
        {
            this.manifestBuilder.addDependencies(dependencies);
        }

        public void addDependencies(String... dependencies)
        {
            this.manifestBuilder.addDependencies(dependencies);
        }

        public boolean removeDependency(String toRemove)
        {
            return this.manifestBuilder.removeDependency(toRemove);
        }

        public boolean removeDependencies(Iterable<? extends String> toRemove)
        {
            return this.manifestBuilder.removeDependencies(toRemove);
        }

        public boolean removeDependencies(String... toRemove)
        {
            return this.manifestBuilder.removeDependencies(toRemove);
        }

        public boolean removeDependencies(Predicate<? super String> toRemove)
        {
            return this.manifestBuilder.removeDependencies(toRemove);
        }

        public void clearDependencies()
        {
            this.manifestBuilder.clearDependencies();
        }

        public void addElements(Iterable<? extends ConcreteElementMetadata> elements)
        {
            this.manifestBuilder.addElements(elements);
        }

        public void addElements(ConcreteElementMetadata... elements)
        {
            this.manifestBuilder.addElements(elements);
        }

        public void addSource(SourceMetadata source)
        {
            this.sourceMetadataBuilder.addSource(source);
        }

        public void addSources(Iterable<? extends SourceMetadata> sources)
        {
            this.sourceMetadataBuilder.addSources(sources);
        }

        public void addSources(SourceMetadata... sources)
        {
            this.sourceMetadataBuilder.addSources(sources);
        }

        public void addExternalReferences(ElementExternalReferenceMetadata externalReferenceMetadata)
        {
            this.extRefBuilder.addElementExternalReferenceMetadata(externalReferenceMetadata);
        }

        public void addExternalReferences(String elementPath, Iterable<? extends String> externalReferences)
        {
            ElementExternalReferenceMetadata extRef = ((externalReferences instanceof Collection) ?
                                                       ElementExternalReferenceMetadata.builder(((Collection<?>) externalReferences).size()) :
                                                       ElementExternalReferenceMetadata.builder())
                    .withElementPath(elementPath)
                    .withExternalReferences(externalReferences)
                    .build();
            addExternalReferences(extRef);
        }

        public void addExternalReferences(String elementPath, String... externalReferences)
        {
            ElementExternalReferenceMetadata extRef = ElementExternalReferenceMetadata.builder(externalReferences.length)
                    .withElementPath(elementPath)
                    .withExternalReferences(externalReferences)
                    .build();
            addExternalReferences(extRef);
        }

        public void addBackReferences(String elementPath, String instanceReferenceId, Iterable<? extends BackReference> backReferences)
        {
            this.backRefBuilder.addBackReferences(elementPath, instanceReferenceId, backReferences);
        }

        public void addBackReferences(String elementPath, String instanceReferenceId, BackReference... backReferences)
        {
            this.backRefBuilder.addBackReferences(elementPath, instanceReferenceId, backReferences);
        }

        public void addFunctionByName(String functionName, String function)
        {
            this.functionsByName.getIfAbsentPut(Objects.requireNonNull(functionName), Lists.mutable::empty).add(Objects.requireNonNull(function));
        }

        public void addFunctionsByName(String functionName, Iterable<? extends String> functions)
        {
            MutableList<String> functionsForName = this.functionsByName.getIfAbsentPut(Objects.requireNonNull(functionName), Lists.mutable::empty);
            functions.forEach(f -> functionsForName.add(Objects.requireNonNull(f)));
        }

        public void addFunctionsByName(String functionName, String... functions)
        {
            List<String> functionsList = Arrays.asList(functions);
            functionsList.forEach(Objects::requireNonNull);
            this.functionsByName.getIfAbsentPut(Objects.requireNonNull(functionName), Lists.mutable::empty).addAll(functionsList);
        }

        public Builder withName(String name)
        {
            setName(name);
            return this;
        }

        public Builder withReferenceIdVersion(Integer version)
        {
            setReferenceIdVersion(version);
            return this;
        }

        public Builder withElement(ConcreteElementMetadata element)
        {
            addElement(element);
            return this;
        }

        public Builder withDependency(String dependency)
        {
            addDependency(dependency);
            return this;
        }

        public Builder withDependencies(Iterable<? extends String> dependencies)
        {
            addDependencies(dependencies);
            return this;
        }

        public Builder withDependencies(String... dependencies)
        {
            addDependencies(dependencies);
            return this;
        }

        public Builder withoutDependency(String toRemove)
        {
            removeDependency(toRemove);
            return this;
        }

        public Builder withoutDependencies(Iterable<? extends String> toRemove)
        {
            removeDependencies(toRemove);
            return this;
        }

        public Builder withoutDependencies(String... toRemove)
        {
            removeDependencies(toRemove);
            return this;
        }

        public Builder withoutDependencies(Predicate<? super String> toRemove)
        {
            removeDependencies(toRemove);
            return this;
        }

        public Builder withNoDependencies()
        {
            clearDependencies();
            return this;
        }

        public Builder withElements(Iterable<? extends ConcreteElementMetadata> elements)
        {
            addElements(elements);
            return this;
        }

        public Builder withElements(ConcreteElementMetadata... elements)
        {
            addElements(elements);
            return this;
        }

        public Builder withSource(SourceMetadata source)
        {
            addSource(source);
            return this;
        }

        public Builder withSources(Iterable<? extends SourceMetadata> sources)
        {
            addSources(sources);
            return this;
        }

        public Builder withSources(SourceMetadata... sources)
        {
            addSources(sources);
            return this;
        }

        public Builder withExternalReferences(ElementExternalReferenceMetadata externalReferenceMetadata)
        {
            addExternalReferences(externalReferenceMetadata);
            return this;
        }

        public Builder withExternalReferences(String elementPath, Iterable<? extends String> externalReferences)
        {
            addExternalReferences(elementPath, externalReferences);
            return this;
        }

        public Builder withExternalReferences(String elementPath, String... externalReferences)
        {
            addExternalReferences(elementPath, externalReferences);
            return this;
        }

        public Builder withBackReferences(String elementPath, String instanceReferenceId, Iterable<? extends BackReference> backReferences)
        {
            addBackReferences(elementPath, instanceReferenceId, backReferences);
            return this;
        }

        public Builder withBackReferences(String elementPath, String instanceReferenceId, BackReference... backReferences)
        {
            addBackReferences(elementPath, instanceReferenceId, backReferences);
            return this;
        }

        public Builder withFunctionByName(String functionName, String function)
        {
            addFunctionByName(functionName, function);
            return this;
        }

        public Builder withFunctionsByName(String functionName, Iterable<? extends String> functions)
        {
            addFunctionsByName(functionName, functions);
            return this;
        }

        public Builder withFunctionsByName(String functionName, String... functions)
        {
            addFunctionsByName(functionName, functions);
            return this;
        }

        public ModuleMetadata build()
        {
            Objects.requireNonNull(this.name, "module name may not be null");
            ModuleManifest manifest = this.manifestBuilder.withModuleName(this.name).build();
            ModuleSourceMetadata sourceMetadata = this.sourceMetadataBuilder.withModuleName(this.name).build();
            ModuleExternalReferenceMetadata extRefMetadata = this.extRefBuilder.withModuleName(this.name).build();
            ModuleBackReferenceMetadata backRefMetadata = this.backRefBuilder.withModuleName(this.name).build();
            ModuleFunctionNameMetadata funcNameMetadata = ModuleFunctionNameMetadata.builder(this.functionsByName.size())
                    .withModuleName(this.name)
                    .withFunctionsByName(this.functionsByName)
                    .build();
            return new ModuleMetadata(manifest, sourceMetadata, extRefMetadata, backRefMetadata, funcNameMetadata);
        }
    }
}
