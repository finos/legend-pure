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

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

public class ModuleFunctionNameMetadata
{
    private final String moduleName;
    private final ImmutableList<FunctionsByName> functionsByNames;

    private ModuleFunctionNameMetadata(String moduleName, ImmutableList<FunctionsByName> functionsByNames)
    {
        this.moduleName = moduleName;
        this.functionsByNames = functionsByNames;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof ModuleFunctionNameMetadata))
        {
            return false;
        }

        ModuleFunctionNameMetadata that = (ModuleFunctionNameMetadata) other;
        return this.moduleName.equals(that.moduleName) &&
                this.functionsByNames.equals(that.functionsByNames);
    }

    @Override
    public int hashCode()
    {
        return this.moduleName.hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("<").append(getClass().getSimpleName())
                .append(" moduleName='").append(this.moduleName).append("' functionsByName=[");
        if (this.functionsByNames.notEmpty())
        {
            this.functionsByNames.forEach(fbn -> fbn.appendString(builder.append('{')).append("}, "));
            builder.setLength(builder.length() - 2);
        }
        return builder.append("]>").toString();
    }

    public String getModuleName()
    {
        return this.moduleName;
    }

    public ImmutableList<FunctionsByName> getFunctionsByName()
    {
        return this.functionsByNames;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static Builder builder(int functionNameCount)
    {
        return new Builder(functionNameCount);
    }

    public static Builder builder(ModuleFunctionNameMetadata moduleFunctionNameMetadata)
    {
        Builder builder = new Builder(moduleFunctionNameMetadata.functionsByNames.size());
        builder.setModuleName(moduleFunctionNameMetadata.moduleName);
        moduleFunctionNameMetadata.functionsByNames.forEach(builder::addFunctionsByName);
        return builder;
    }

    public static class Builder
    {
        private String moduleName;
        private final MutableList<FunctionsByName> functionsByName;

        private Builder()
        {
            this.functionsByName = Lists.mutable.empty();
        }

        private Builder(int functionNameCount)
        {
            this.functionsByName = Lists.mutable.ofInitialCapacity(functionNameCount);
        }

        private Builder(ModuleFunctionNameMetadata metadata)
        {
            this.moduleName = metadata.moduleName;
            this.functionsByName = Lists.mutable.withAll(metadata.functionsByNames);
        }

        public void setModuleName(String moduleName)
        {
            this.moduleName = moduleName;
        }

        public void addFunctionsByName(FunctionsByName functionsByName)
        {
            this.functionsByName.add(Objects.requireNonNull(functionsByName));
        }

        public void addFunctionsByName(String functionName, Iterable<? extends String> functions)
        {
            addFunctionsByName(FunctionsByName.builder().withFunctionName(functionName).withFunctions(functions).build());
        }

        public void addFunctionsByName(Map<? extends String, ? extends Iterable<? extends String>> functionsByName)
        {
            functionsByName.forEach(this::addFunctionsByName);
        }

        public Builder withModuleName(String moduleName)
        {
            setModuleName(moduleName);
            return this;
        }

        public Builder withFunctionsByName(FunctionsByName functionsByName)
        {
            addFunctionsByName(functionsByName);
            return this;
        }

        public Builder withFunctionsByName(String functionName, Iterable<? extends String> functions)
        {
            addFunctionsByName(functionName, functions);
            return this;
        }

        public Builder withFunctionsByName(Map<? extends String, ? extends Iterable<? extends String>> functionsByName)
        {
            addFunctionsByName(functionsByName);
            return this;
        }

        public ModuleFunctionNameMetadata build()
        {
            return new ModuleFunctionNameMetadata(Objects.requireNonNull(this.moduleName, "module name may not be null"), buildFunctionsByName());
        }

        private ImmutableList<FunctionsByName> buildFunctionsByName()
        {
            if (this.functionsByName.size() > 1)
            {
                this.functionsByName.sort(Comparator.comparing(FunctionsByName::getFunctionName));
                int index = 0;
                while (index < this.functionsByName.size())
                {
                    int start = index++;
                    FunctionsByName current = this.functionsByName.get(start);
                    String currentFunctionName = current.getFunctionName();
                    while ((index < this.functionsByName.size()) && currentFunctionName.equals(this.functionsByName.get(index).getFunctionName()))
                    {
                        index++;
                    }
                    if (index > start + 1)
                    {
                        // Multiple FunctionsByName objects for the same function name: merge them
                        FunctionsByName.Builder builder = FunctionsByName.builder(current);
                        for (int i = start + 1; i < index; i++)
                        {
                            // merge and set to null to mark for removal
                            builder.addFunctionsByName(this.functionsByName.set(i, null));
                        }
                        this.functionsByName.set(start, builder.build());
                    }
                }
                this.functionsByName.removeIf(Objects::isNull);
            }
            return this.functionsByName.toImmutable();
        }
    }
}
