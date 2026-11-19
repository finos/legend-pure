// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.compiler.metadata.v3;

import org.finos.legend.pure.m3.serialization.compiler.metadata.ElementBackReferenceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleBackReferenceIndex;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleExternalReferenceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleFunctionNameMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleManifest;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataSerializerExtension;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleSourceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.v2.ModuleMetadataSerializerV2;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexer;
import org.finos.legend.pure.m3.tools.CompressorPool;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class ModuleMetadataSerializerV3 implements ModuleMetadataSerializerExtension
{
    private static final int COMPRESSION_LEVEL = 7;

    private final ModuleMetadataSerializerV2 v2 = new ModuleMetadataSerializerV2();

    @Override
    public int version()
    {
        return 3;
    }

    @Override
    public void serializeManifest(OutputStream stream, ModuleManifest manifest, StringIndexer stringIndexer)
    {
        compress(stream, s -> this.v2.serializeManifest(s, manifest, stringIndexer));
    }

    @Override
    public ModuleManifest deserializeManifest(InputStream stream, StringIndexer stringIndexer)
    {
        return decompress(stream, s -> this.v2.deserializeManifest(s, stringIndexer));
    }

    @Override
    public void serializeSourceMetadata(OutputStream stream, ModuleSourceMetadata sourceMetadata, StringIndexer stringIndexer)
    {
        compress(stream, s -> this.v2.serializeSourceMetadata(s, sourceMetadata, stringIndexer));
    }

    @Override
    public ModuleSourceMetadata deserializeSourceMetadata(InputStream stream, StringIndexer stringIndexer)
    {
        return decompress(stream, s -> this.v2.deserializeSourceMetadata(s, stringIndexer));
    }

    @Override
    public void serializeExternalReferenceMetadata(OutputStream stream, ModuleExternalReferenceMetadata externalReferenceMetadata, StringIndexer stringIndexer)
    {
        compress(stream, s -> this.v2.serializeExternalReferenceMetadata(s, externalReferenceMetadata, stringIndexer));
    }

    @Override
    public ModuleExternalReferenceMetadata deserializeExternalReferenceMetadata(InputStream stream, StringIndexer stringIndexer)
    {
        return decompress(stream, s -> this.v2.deserializeExternalReferenceMetadata(s, stringIndexer));
    }

    @Override
    public void serializeBackReferenceMetadata(OutputStream stream, ElementBackReferenceMetadata backReferenceMetadata, StringIndexer stringIndexer)
    {
        compress(stream, s -> this.v2.serializeBackReferenceMetadata(s, backReferenceMetadata, stringIndexer));
    }

    @Override
    public ElementBackReferenceMetadata deserializeBackReferenceMetadata(InputStream stream, StringIndexer stringIndexer)
    {
        return decompress(stream, s -> this.v2.deserializeBackReferenceMetadata(s, stringIndexer));
    }

    @Override
    public void serializeFunctionNameMetadata(OutputStream stream, ModuleFunctionNameMetadata functionNameMetadata, StringIndexer stringIndexer)
    {
        compress(stream, s -> this.v2.serializeFunctionNameMetadata(s, functionNameMetadata, stringIndexer));
    }

    @Override
    public ModuleFunctionNameMetadata deserializeFunctionNameMetadata(InputStream stream, StringIndexer stringIndexer)
    {
        return decompress(stream, s -> this.v2.deserializeFunctionNameMetadata(s, stringIndexer));
    }

    @Override
    public void serializeBackReferenceIndex(OutputStream stream, ModuleBackReferenceIndex backReferenceIndex, StringIndexer stringIndexer)
    {
        compress(stream, s -> this.v2.serializeBackReferenceIndex(s, backReferenceIndex, stringIndexer));
    }

    @Override
    public ModuleBackReferenceIndex deserializeBackReferenceIndex(InputStream stream, StringIndexer stringIndexer)
    {
        return decompress(stream, s -> this.v2.deserializeBackReferenceIndex(s, stringIndexer));
    }

    private static void compress(OutputStream stream, Consumer<? super OutputStream> v2Serializer)
    {
        try (CompressorPool.CloseableDeflater deflater = CompressorPool.getInstance().borrowDeflater(COMPRESSION_LEVEL, true))
        {
            // V2 writes a primitive at a time: the buffer saves a call to the deflater for each one
            DeflaterOutputStream zipStream = new DeflaterOutputStream(stream, deflater);
            BufferedOutputStream bufferedStream = new BufferedOutputStream(zipStream);
            v2Serializer.accept(bufferedStream);
            bufferedStream.flush();
            zipStream.finish();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static <T> T decompress(InputStream stream, Function<? super InputStream, ? extends T> v2Deserializer)
    {
        try (CompressorPool.CloseableInflater inflater = CompressorPool.getInstance().borrowInflater(true))
        {
            // V2 reads a primitive at a time: the buffer saves a call to the inflater for each one
            return v2Deserializer.apply(new BufferedInputStream(new InflaterInputStream(stream, inflater)));
        }
    }
}
