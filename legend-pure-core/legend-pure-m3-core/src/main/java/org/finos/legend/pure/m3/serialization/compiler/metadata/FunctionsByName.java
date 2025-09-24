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
import org.finos.legend.pure.m3.tools.ListHelper;

import java.util.Arrays;
import java.util.Objects;

public class FunctionsByName
{
    private final String functionName;
    private final ImmutableList<String> functions;

    private FunctionsByName(String functionName, ImmutableList<String> functions)
    {
        this.functionName = functionName;
        this.functions = functions;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof FunctionsByName))
        {
            return false;
        }

        FunctionsByName that = (FunctionsByName) other;
        return this.functionName.equals(that.functionName) &&
                this.functions.equals(that.functions);
    }

    @Override
    public int hashCode()
    {
        return this.functionName.hashCode() + (31 * this.functions.hashCode());
    }

    @Override
    public String toString()
    {
        return appendString(new StringBuilder("<").append(getClass().getSimpleName()).append(' ')).append('>').toString();
    }

    StringBuilder appendString(StringBuilder builder)
    {
        builder.append("functionName='").append(this.functionName);
        this.functions.appendString(builder, "' functions=[", ", ", "]");
        return builder;
    }

    public String getFunctionName()
    {
        return this.functionName;
    }

    public ImmutableList<String> getFunctions()
    {
        return this.functions;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static Builder builder(int functionCount)
    {
        return new Builder(functionCount);
    }

    public static Builder builder(FunctionsByName source)
    {
        return new Builder(source);
    }

    public static class Builder
    {
        private String functionName;
        private final MutableList<String> functions;

        private Builder()
        {
            this.functions = Lists.mutable.empty();
        }

        private Builder(int functionCount)
        {
            this.functions = Lists.mutable.withInitialCapacity(functionCount);
        }

        private Builder(FunctionsByName source)
        {
            this.functionName = source.functionName;
            this.functions = Lists.mutable.withAll(source.functions);
        }

        public void setFunctionName(String functionName)
        {
            this.functionName = functionName;
        }

        public void addFunction(String function)
        {
            this.functions.add(Objects.requireNonNull(function, "function may not be null"));
        }

        public void addFunctions(Iterable<? extends String> functions)
        {
            functions.forEach(this::addFunction);
        }

        public void addFunctions(String... functions)
        {
            addFunctions(Arrays.asList(functions));
        }

        public void addFunctionsByName(FunctionsByName functionsByName)
        {
            Objects.requireNonNull(functionsByName);
            if (this.functionName == null)
            {
                this.functionName = functionsByName.functionName;
            }
            else if (!this.functionName.equals(functionsByName.functionName))
            {
                throw new IllegalStateException("Mismatched function names: " + this.functionName + " and " + functionsByName.functionName);
            }
            this.functions.addAll(functionsByName.functions.castToList());
        }

        public Builder withFunctionName(String functionName)
        {
            setFunctionName(functionName);
            return this;
        }

        public Builder withFunction(String function)
        {
            addFunction(function);
            return this;
        }

        public Builder withFunctions(Iterable<? extends String> functions)
        {
            addFunctions(functions);
            return this;
        }

        public Builder withFunctions(String... functions)
        {
            addFunctions(functions);
            return this;
        }

        public Builder withFunctionsByName(FunctionsByName functionsByName)
        {
            addFunctionsByName(functionsByName);
            return this;
        }

        public FunctionsByName build()
        {
            return new FunctionsByName(Objects.requireNonNull(this.functionName, "function name may not be null"), ListHelper.sortAndRemoveDuplicates(this.functions).toImmutable());
        }
    }
}
