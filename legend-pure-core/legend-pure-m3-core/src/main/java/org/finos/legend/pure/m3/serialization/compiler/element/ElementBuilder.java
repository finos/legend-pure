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

package org.finos.legend.pure.m3.serialization.compiler.element;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReference;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReferenceProvider;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ConcreteElementMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.MetadataIndex;
import org.finos.legend.pure.m3.serialization.compiler.metadata.VirtualPackageMetadata;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolver;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolvers;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.function.IntFunction;
import java.util.function.Supplier;

public interface ElementBuilder
{
    default void initialize(ElementLoader elementLoader)
    {
        // do nothing by default
    }

    CoreInstance buildVirtualPackage(VirtualPackageMetadata metadata, MetadataIndex index, ReferenceIdResolvers referenceIds, Supplier<? extends BackReferenceProvider> backRefProviderDeserializer);

    CoreInstance buildConcreteElement(ConcreteElementMetadata metadata, MetadataIndex index, ReferenceIdResolvers referenceIds, Supplier<? extends DeserializedConcreteElement> deserializer, Supplier<? extends BackReferenceProvider> backRefProviderDeserializer);

    CoreInstance buildComponentInstance(InstanceData instanceData, ListIterable<? extends BackReference> backReferences, ReferenceIdResolver referenceIdResolver, IntFunction<? extends CoreInstance> internalIdResolver);
}
