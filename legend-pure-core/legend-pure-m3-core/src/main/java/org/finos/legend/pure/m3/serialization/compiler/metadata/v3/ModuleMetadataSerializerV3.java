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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
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
        try (CompressorPool.CloseableDeflater deflater = CompressorPool.getInstance().borrowDeflater(COMPRESSION_LEVEL, true))
        {
            DeflaterOutputStream zipStream = new DeflaterOutputStream(stream, deflater);
            this.v2.serializeManifest(zipStream, manifest, stringIndexer);
            zipStream.finish();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ModuleManifest deserializeManifest(InputStream stream, StringIndexer stringIndexer)
    {
        try (CompressorPool.CloseableInflater inflater = CompressorPool.getInstance().borrowInflater(true))
        {
            return this.v2.deserializeManifest(new InflaterInputStream(stream, inflater), stringIndexer);
        }
    }

    @Override
    public void serializeSourceMetadata(OutputStream stream, ModuleSourceMetadata sourceMetadata, StringIndexer stringIndexer)
    {
        try (CompressorPool.CloseableDeflater deflater = CompressorPool.getInstance().borrowDeflater(COMPRESSION_LEVEL, true))
        {
            DeflaterOutputStream zipStream = new DeflaterOutputStream(stream, deflater);
            this.v2.serializeSourceMetadata(zipStream, sourceMetadata, stringIndexer);
            zipStream.finish();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ModuleSourceMetadata deserializeSourceMetadata(InputStream stream, StringIndexer stringIndexer)
    {
        try (CompressorPool.CloseableInflater inflater = CompressorPool.getInstance().borrowInflater(true))
        {
            return this.v2.deserializeSourceMetadata(new InflaterInputStream(stream, inflater), stringIndexer);
        }
    }

    @Override
    public void serializeExternalReferenceMetadata(OutputStream stream, ModuleExternalReferenceMetadata externalReferenceMetadata, StringIndexer stringIndexer)
    {
        try (CompressorPool.CloseableDeflater deflater = CompressorPool.getInstance().borrowDeflater(COMPRESSION_LEVEL, true))
        {
            DeflaterOutputStream zipStream = new DeflaterOutputStream(stream, deflater);
            this.v2.serializeExternalReferenceMetadata(zipStream, externalReferenceMetadata, stringIndexer);
            zipStream.finish();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ModuleExternalReferenceMetadata deserializeExternalReferenceMetadata(InputStream stream, StringIndexer stringIndexer)
    {
        try (CompressorPool.CloseableInflater inflater = CompressorPool.getInstance().borrowInflater(true))
        {
            return this.v2.deserializeExternalReferenceMetadata(new InflaterInputStream(stream, inflater), stringIndexer);
        }
    }

    @Override
    public void serializeBackReferenceMetadata(OutputStream stream, ElementBackReferenceMetadata backReferenceMetadata, StringIndexer stringIndexer)
    {
        try (CompressorPool.CloseableDeflater deflater = CompressorPool.getInstance().borrowDeflater(COMPRESSION_LEVEL, true))
        {
            DeflaterOutputStream zipStream = new DeflaterOutputStream(stream, deflater);
            this.v2.serializeBackReferenceMetadata(zipStream, backReferenceMetadata, stringIndexer);
            zipStream.finish();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ElementBackReferenceMetadata deserializeBackReferenceMetadata(InputStream stream, StringIndexer stringIndexer)
    {
        try (CompressorPool.CloseableInflater inflater = CompressorPool.getInstance().borrowInflater(true))
        {
            return this.v2.deserializeBackReferenceMetadata(new InflaterInputStream(stream, inflater), stringIndexer);
        }
    }

    @Override
    public void serializeFunctionNameMetadata(OutputStream stream, ModuleFunctionNameMetadata functionNameMetadata, StringIndexer stringIndexer)
    {
        try (CompressorPool.CloseableDeflater deflater = CompressorPool.getInstance().borrowDeflater(COMPRESSION_LEVEL, true))
        {
            DeflaterOutputStream zipStream = new DeflaterOutputStream(stream, deflater);
            this.v2.serializeFunctionNameMetadata(zipStream, functionNameMetadata, stringIndexer);
            zipStream.finish();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ModuleFunctionNameMetadata deserializeFunctionNameMetadata(InputStream stream, StringIndexer stringIndexer)
    {
        try (CompressorPool.CloseableInflater inflater = CompressorPool.getInstance().borrowInflater(true))
        {
            return this.v2.deserializeFunctionNameMetadata(new InflaterInputStream(stream, inflater), stringIndexer);
        }
    }

    @Override
    public void serializeBackReferenceIndex(OutputStream stream, ModuleBackReferenceIndex backReferenceIndex, StringIndexer stringIndexer)
    {
        try (CompressorPool.CloseableDeflater deflater = CompressorPool.getInstance().borrowDeflater(COMPRESSION_LEVEL, true))
        {
            DeflaterOutputStream zipStream = new DeflaterOutputStream(stream, deflater);
            this.v2.serializeBackReferenceIndex(zipStream, backReferenceIndex, stringIndexer);
            zipStream.finish();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ModuleBackReferenceIndex deserializeBackReferenceIndex(InputStream stream, StringIndexer stringIndexer)
    {
        try (CompressorPool.CloseableInflater inflater = CompressorPool.getInstance().borrowInflater(true))
        {
            return this.v2.deserializeBackReferenceIndex(new InflaterInputStream(stream, inflater), stringIndexer);
        }
    }
}
