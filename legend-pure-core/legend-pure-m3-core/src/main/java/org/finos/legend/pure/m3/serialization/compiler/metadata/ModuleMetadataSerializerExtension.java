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
import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexer;

import java.io.InputStream;
import java.io.OutputStream;

public interface ModuleMetadataSerializerExtension extends SerializerExtension
{
    // Manifest

    void serializeManifest(OutputStream stream, ModuleManifest manifest, StringIndexer stringIndexer);

    ModuleManifest deserializeManifest(InputStream stream, StringIndexer stringIndexer);

    // Source metadata

    void serializeSourceMetadata(OutputStream stream, ModuleSourceMetadata sourceMetadata, StringIndexer stringIndexer);

    ModuleSourceMetadata deserializeSourceMetadata(InputStream stream, StringIndexer stringIndexer);

    // External reference metadata

    void serializeExternalReferenceMetadata(OutputStream stream, ModuleExternalReferenceMetadata externalReferenceMetadata, StringIndexer stringIndexer);

    ModuleExternalReferenceMetadata deserializeExternalReferenceMetadata(InputStream stream, StringIndexer stringIndexer);

    // Back reference metadata

    void serializeBackReferenceMetadata(OutputStream stream, ElementBackReferenceMetadata backReferenceMetadata, StringIndexer stringIndexer);

    ElementBackReferenceMetadata deserializeBackReferenceMetadata(InputStream stream, StringIndexer stringIndexer);

    // Back reference index

    void serializeBackReferenceIndex(OutputStream stream, ModuleBackReferenceIndex backReferenceIndex, StringIndexer stringIndexer);

    ModuleBackReferenceIndex deserializeBackReferenceIndex(InputStream stream, StringIndexer stringIndexer);

    // Function name metadata

    void serializeFunctionNameMetadata(OutputStream stream, ModuleFunctionNameMetadata functionNameMetadata, StringIndexer stringIndexer);

    ModuleFunctionNameMetadata deserializeFunctionNameMetadata(InputStream stream, StringIndexer stringIndexer);
}
