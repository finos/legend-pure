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

import org.finos.legend.pure.m3.serialization.compiler.SerializerExtension;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;

public interface ModuleMetadataSerializerExtension extends SerializerExtension
{
    // Manifest

    void serializeManifest(Writer writer, ModuleManifest manifest);

    ModuleManifest deserializeManifest(Reader reader);

    // Source metadata

    void serializeSourceMetadata(Writer writer, ModuleSourceMetadata sourceMetadata);

    ModuleSourceMetadata deserializeSourceMetadata(Reader reader);

    // External reference metadata

    void serializeExternalReferenceMetadata(Writer writer, ModuleExternalReferenceMetadata externalReferenceMetadata);

    ModuleExternalReferenceMetadata deserializeExternalReferenceMetadata(Reader reader);

    // Back reference metadata

    void serializeBackReferenceMetadata(Writer writer, ElementBackReferenceMetadata backReferenceMetadata);

    ElementBackReferenceMetadata deserializeBackReferenceMetadata(Reader reader);
}
