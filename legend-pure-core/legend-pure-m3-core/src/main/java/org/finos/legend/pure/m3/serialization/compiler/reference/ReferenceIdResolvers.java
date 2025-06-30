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

package org.finos.legend.pure.m3.serialization.compiler.reference;

import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.Objects;
import java.util.function.Function;

public class ReferenceIdResolvers extends ReferenceIds
{
    private final Function<? super String, ? extends CoreInstance> packagePathResolver;

    private ReferenceIdResolvers(ExtensionManager extensionManager, Function<? super String, ? extends CoreInstance> packagePathResolver)
    {
        super(extensionManager);
        this.packagePathResolver = packagePathResolver;
    }

    public ReferenceIdResolver resolver(int version)
    {
        return this.extensionManager.getExtensionCache(version).resolver(this.packagePathResolver);
    }

    public ReferenceIdResolver resolver(Integer version)
    {
        return this.extensionManager.getExtensionCache(version).resolver(this.packagePathResolver);
    }

    public ReferenceIdResolver resolver()
    {
        return this.extensionManager.getDefaultExtensionCache().resolver(this.packagePathResolver);
    }

    public Function<? super String, ? extends CoreInstance> packagePathResolver()
    {
        return this.packagePathResolver;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder extends AbstractBuilder<ReferenceIdResolvers>
    {
        private Function<? super String, ? extends CoreInstance> packagePathResolver;

        private Builder()
        {
        }

        public Builder withExtension(ReferenceIdExtension extension)
        {
            addExtension(extension);
            return this;
        }

        public Builder withExtensions(Iterable<? extends ReferenceIdExtension> extensions)
        {
            addExtensions(extensions);
            return this;
        }

        public Builder withAvailableExtensions(ClassLoader classLoader)
        {
            loadExtensions(classLoader);
            return this;
        }

        public Builder withAvailableExtensions()
        {
            loadExtensions();
            return this;
        }

        public Builder withDefaultVersion(Integer defaultVersion)
        {
            setDefaultVersion(defaultVersion);
            return this;
        }

        public void setPackagePathResolver(Function<? super String, ? extends CoreInstance> packagePathResolver)
        {
            this.packagePathResolver = packagePathResolver;
        }

        public void clearPackagePathResolver()
        {
            setPackagePathResolver(null);
        }

        public Builder withPackagePathResolver(Function<? super String, ? extends CoreInstance> packagePathResolver)
        {
            setPackagePathResolver(packagePathResolver);
            return this;
        }

        @Override
        ReferenceIdResolvers build(ExtensionManager extensionManager)
        {
            return new ReferenceIdResolvers(extensionManager, Objects.requireNonNull(this.packagePathResolver, "package path resolver is required"));
        }
    }
}
