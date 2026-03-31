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

import org.finos.legend.pure.m3.serialization.compiler.ExtensibleSerializer;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexer;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class ModuleMetadataSerializer extends ExtensibleSerializer<ModuleMetadataSerializerExtension>
{
    private static final long PURE_MODULE_MANIFEST_SIGNATURE = Long.parseLong("PureManifest", 36);
    private static final long PURE_MODULE_SOURCE_METADATA_SIGNATURE = Long.parseLong("PureSource", 36);
    private static final long PURE_MODULE_EXT_REFS_SIGNATURE = Long.parseLong("PureExtRefs", 36);
    private static final long PURE_ELEMENT_BACK_REFS_SIGNATURE = Long.parseLong("PureBackRefs", 36);
    private static final long PURE_BACK_REF_INDEX_SIGNATURE = Long.parseLong("PureBRIndex", 36);
    private static final long PURE_FUNCTION_NAMES_SIGNATURE = Long.parseLong("PureFuncName", 36);

    private final StringIndexer stringIndexer;

    private ModuleMetadataSerializer(Iterable<? extends ModuleMetadataSerializerExtension> extensions, int defaultVersion, StringIndexer stringIndexer)
    {
        super(extensions, defaultVersion);
        this.stringIndexer = stringIndexer;
    }

    // Manifest

    public void serializeManifest(OutputStream stream, ModuleManifest manifest)
    {
        serializeManifest(stream, manifest, getDefaultExtension());
    }

    public void serializeManifest(OutputStream stream, ModuleManifest manifest, int version)
    {
        serializeManifest(stream, manifest, getExtension(version));
    }

    private void serializeManifest(OutputStream stream, ModuleManifest manifest, ModuleMetadataSerializerExtension extension)
    {
        try (Writer writer = BinaryWriters.newBinaryWriter(stream, false))
        {
            writer.writeLong(PURE_MODULE_MANIFEST_SIGNATURE);
            writer.writeInt(extension.version());
        }
        extension.serializeManifest(stream, manifest, this.stringIndexer);
    }

    public ModuleManifest deserializeManifest(InputStream stream)
    {
        int version;
        try (Reader reader = BinaryReaders.newBinaryReader(stream, false))
        {
            long signature = reader.readLong();
            if (signature != PURE_MODULE_MANIFEST_SIGNATURE)
            {
                throw new IllegalArgumentException("Invalid file format: not a Legend module manifest file");
            }
            version = reader.readInt();
        }
        ModuleMetadataSerializerExtension extension = getExtension(version);
        return extension.deserializeManifest(stream, this.stringIndexer);
    }

    // Source metadata

    public void serializeSourceMetadata(OutputStream stream, ModuleSourceMetadata sourceMetadata)
    {
        serializeSourceMetadata(stream, sourceMetadata, getDefaultExtension());
    }

    public void serializeSourceMetadata(OutputStream stream, ModuleSourceMetadata sourceMetadata, int version)
    {
        serializeSourceMetadata(stream, sourceMetadata, getExtension(version));
    }

    private void serializeSourceMetadata(OutputStream stream, ModuleSourceMetadata sourceMetadata, ModuleMetadataSerializerExtension extension)
    {
        try (Writer writer = BinaryWriters.newBinaryWriter(stream, false))
        {
            writer.writeLong(PURE_MODULE_SOURCE_METADATA_SIGNATURE);
            writer.writeInt(extension.version());
        }
        extension.serializeSourceMetadata(stream, sourceMetadata, this.stringIndexer);
    }

    public ModuleSourceMetadata deserializeSourceMetadata(InputStream stream)
    {
        int version;
        try (Reader reader = BinaryReaders.newBinaryReader(stream, false))
        {
            long signature = reader.readLong();
            if (signature != PURE_MODULE_SOURCE_METADATA_SIGNATURE)
            {
                throw new IllegalArgumentException("Invalid file format: not a Legend module source metadata file");
            }
            version = reader.readInt();
        }
        ModuleMetadataSerializerExtension extension = getExtension(version);
        return extension.deserializeSourceMetadata(stream, this.stringIndexer);
    }

    // External reference metadata

    public void serializeExternalReferenceMetadata(OutputStream stream, ModuleExternalReferenceMetadata externalReferenceMetadata)
    {
        serializeExternalReferenceMetadata(stream, externalReferenceMetadata, getDefaultExtension());
    }

    public void serializeExternalReferenceMetadata(OutputStream stream, ModuleExternalReferenceMetadata externalReferenceMetadata, int version)
    {
        serializeExternalReferenceMetadata(stream, externalReferenceMetadata, getExtension(version));
    }

    private void serializeExternalReferenceMetadata(OutputStream stream, ModuleExternalReferenceMetadata externalReferenceMetadata, ModuleMetadataSerializerExtension extension)
    {
        try (Writer writer = BinaryWriters.newBinaryWriter(stream, false))
        {
            writer.writeLong(PURE_MODULE_EXT_REFS_SIGNATURE);
            writer.writeInt(extension.version());
        }
        extension.serializeExternalReferenceMetadata(stream, externalReferenceMetadata, this.stringIndexer);
    }

    public ModuleExternalReferenceMetadata deserializeExternalReferenceMetadata(InputStream stream)
    {
        int version;
        try (Reader reader = BinaryReaders.newBinaryReader(stream, false))
        {
            long signature = reader.readLong();
            if (signature != PURE_MODULE_EXT_REFS_SIGNATURE)
            {
                throw new IllegalArgumentException("Invalid file format: not a Legend module external reference metadata file");
            }
            version = reader.readInt();
        }
        ModuleMetadataSerializerExtension extension = getExtension(version);
        return extension.deserializeExternalReferenceMetadata(stream, this.stringIndexer);
    }

    // Element back references

    public void serializeBackReferenceMetadata(OutputStream stream, ElementBackReferenceMetadata backReferenceMetadata)
    {
        serializeBackReferenceMetadata(stream, backReferenceMetadata, getDefaultExtension());
    }

    public void serializeBackReferenceMetadata(OutputStream stream, ElementBackReferenceMetadata backReferenceMetadata, int version)
    {
        serializeBackReferenceMetadata(stream, backReferenceMetadata, getExtension(version));
    }

    private void serializeBackReferenceMetadata(OutputStream stream, ElementBackReferenceMetadata backReferenceMetadata, ModuleMetadataSerializerExtension extension)
    {
        try (Writer writer = BinaryWriters.newBinaryWriter(stream, false))
        {
            writer.writeLong(PURE_ELEMENT_BACK_REFS_SIGNATURE);
            writer.writeInt(extension.version());
        }
        extension.serializeBackReferenceMetadata(stream, backReferenceMetadata, this.stringIndexer);
    }

    public ElementBackReferenceMetadata deserializeBackReferenceMetadata(InputStream stream)
    {
        int version;
        try (Reader reader = BinaryReaders.newBinaryReader(stream, false))
        {
            long signature = reader.readLong();
            if (signature != PURE_ELEMENT_BACK_REFS_SIGNATURE)
            {
                throw new IllegalArgumentException("Invalid file format: not a Legend element back reference metadata file");
            }
            version = reader.readInt();
        }
        ModuleMetadataSerializerExtension extension = getExtension(version);
        return extension.deserializeBackReferenceMetadata(stream, this.stringIndexer);
    }

    // Back reference index

    public void serializeBackReferenceIndex(OutputStream stream, ModuleBackReferenceIndex backReferenceIndex)
    {
        serializeBackReferenceIndex(stream, backReferenceIndex, getDefaultExtension());
    }

    public void serializeBackReferenceIndex(OutputStream stream, ModuleBackReferenceIndex backReferenceIndex, int version)
    {
        serializeBackReferenceIndex(stream, backReferenceIndex, getExtension(version));
    }

    private void serializeBackReferenceIndex(OutputStream stream, ModuleBackReferenceIndex backReferenceIndex, ModuleMetadataSerializerExtension extension)
    {
        try (Writer writer = BinaryWriters.newBinaryWriter(stream, false))
        {
            writer.writeLong(PURE_BACK_REF_INDEX_SIGNATURE);
            writer.writeInt(extension.version());
        }
        extension.serializeBackReferenceIndex(stream, backReferenceIndex, this.stringIndexer);
    }

    public ModuleBackReferenceIndex deserializeBackReferenceIndex(InputStream stream)
    {
        int version;
        try (Reader reader = BinaryReaders.newBinaryReader(stream, false))
        {
            long signature = reader.readLong();
            if (signature != PURE_BACK_REF_INDEX_SIGNATURE)
            {
                throw new IllegalArgumentException("Invalid file format: not a Legend back reference index file");
            }
            version = reader.readInt();
        }
        ModuleMetadataSerializerExtension extension = getExtension(version);
        return extension.deserializeBackReferenceIndex(stream, this.stringIndexer);
    }
    
    // Function name metadata
    
    public void serializeFunctionNameMetadata(OutputStream stream, ModuleFunctionNameMetadata functionNameMetadata)
    {
        serializeFunctionNameMetadata(stream, functionNameMetadata, getDefaultExtension());
    }

    public void serializeFunctionNameMetadata(OutputStream stream, ModuleFunctionNameMetadata functionNameMetadata, int version)
    {
        serializeFunctionNameMetadata(stream, functionNameMetadata, getExtension(version));
    }

    private void serializeFunctionNameMetadata(OutputStream stream, ModuleFunctionNameMetadata functionNameMetadata, ModuleMetadataSerializerExtension extension)
    {
        try (Writer writer = BinaryWriters.newBinaryWriter(stream, false))
        {
            writer.writeLong(PURE_FUNCTION_NAMES_SIGNATURE);
            writer.writeInt(extension.version());
        }
        extension.serializeFunctionNameMetadata(stream, functionNameMetadata, this.stringIndexer);
    }

    public ModuleFunctionNameMetadata deserializeFunctionNameMetadata(InputStream stream)
    {
        int version;
        try (Reader reader = BinaryReaders.newBinaryReader(stream, false))
        {
            long signature = reader.readLong();
            if (signature != PURE_FUNCTION_NAMES_SIGNATURE)
            {
                throw new IllegalArgumentException("Invalid file format: not a Legend module function name metadata file");
            }
            version = reader.readInt();
        }
        ModuleMetadataSerializerExtension extension = getExtension(version);
        return extension.deserializeFunctionNameMetadata(stream, this.stringIndexer);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder extends AbstractBuilder<ModuleMetadataSerializerExtension, ModuleMetadataSerializer>
    {
        private StringIndexer stringIndexer;

        private Builder()
        {
        }

        public Builder withExtension(ModuleMetadataSerializerExtension extension)
        {
            addExtension(extension);
            return this;
        }

        public Builder withExtensions(Iterable<? extends ModuleMetadataSerializerExtension> extensions)
        {
            addExtensions(extensions);
            return this;
        }

        public Builder withExtensions(ModuleMetadataSerializerExtension... extensions)
        {
            return withExtensions(Arrays.asList(extensions));
        }

        public Builder withLoadedExtensions(ClassLoader classLoader)
        {
            loadExtensions(classLoader);
            return this;
        }

        public Builder withLoadedExtensions()
        {
            loadExtensions();
            return this;
        }

        public Builder withDefaultVersion(int defaultVersion)
        {
            setDefaultVersion(defaultVersion);
            return this;
        }

        public Builder withStringIndexer(StringIndexer stringIndexer)
        {
            this.stringIndexer = stringIndexer;
            return this;
        }

        @Override
        protected ModuleMetadataSerializer build(Iterable<ModuleMetadataSerializerExtension> extensions, int defaultVersion)
        {
            // if no string indexer has been specified, use the default
            return new ModuleMetadataSerializer(extensions, defaultVersion, (this.stringIndexer == null) ? StringIndexer.defaultStringIndexer() : this.stringIndexer);
        }

        @Override
        protected Class<ModuleMetadataSerializerExtension> getExtensionClass()
        {
            return ModuleMetadataSerializerExtension.class;
        }
    }
}
