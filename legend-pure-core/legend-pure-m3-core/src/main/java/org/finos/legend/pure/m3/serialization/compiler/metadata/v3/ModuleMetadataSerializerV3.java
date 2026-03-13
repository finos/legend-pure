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
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleExternalReferenceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleFunctionNameMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleManifest;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataSerializerExtension;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleSourceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.v2.ModuleMetadataSerializerV2;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class ModuleMetadataSerializerV3 implements ModuleMetadataSerializerExtension
{
    private final ModuleMetadataSerializerV2 v2 = new ModuleMetadataSerializerV2();

    @Override
    public int version()
    {
        return 3;
    }

    @Override
    public void serializeManifest(OutputStream stream, ModuleManifest manifest, StringIndexer stringIndexer)
    {
        Deflater deflater = newDeflater();
        try
        {
            DeflaterOutputStream zipStream = new DeflaterOutputStream(stream, deflater);
            this.v2.serializeManifest(zipStream, manifest, stringIndexer);
            zipStream.finish();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        finally
        {
            deflater.end();
        }
    }

    @Override
    public ModuleManifest deserializeManifest(InputStream stream, StringIndexer stringIndexer)
    {
        Inflater inflater = newInflater();
        try
        {
            return this.v2.deserializeManifest(new InflaterInputStream(stream, inflater), stringIndexer);
        }
        finally
        {
            inflater.end();
        }
    }

    @Override
    public void serializeSourceMetadata(OutputStream stream, ModuleSourceMetadata sourceMetadata, StringIndexer stringIndexer)
    {
        Deflater deflater = newDeflater();
        try
        {
            DeflaterOutputStream zipStream = new DeflaterOutputStream(stream, deflater);
            this.v2.serializeSourceMetadata(zipStream, sourceMetadata, stringIndexer);
            zipStream.finish();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        finally
        {
            deflater.end();
        }
    }

    @Override
    public ModuleSourceMetadata deserializeSourceMetadata(InputStream stream, StringIndexer stringIndexer)
    {
        Inflater inflater = newInflater();
        try
        {
            return this.v2.deserializeSourceMetadata(new InflaterInputStream(stream, inflater), stringIndexer);
        }
        finally
        {
            inflater.end();
        }
    }

    @Override
    public void serializeExternalReferenceMetadata(OutputStream stream, ModuleExternalReferenceMetadata externalReferenceMetadata, StringIndexer stringIndexer)
    {
        Deflater deflater = newDeflater();
        try
        {
            DeflaterOutputStream zipStream = new DeflaterOutputStream(stream, deflater);
            this.v2.serializeExternalReferenceMetadata(zipStream, externalReferenceMetadata, stringIndexer);
            zipStream.finish();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        finally
        {
            deflater.end();
        }
    }

    @Override
    public ModuleExternalReferenceMetadata deserializeExternalReferenceMetadata(InputStream stream, StringIndexer stringIndexer)
    {
        Inflater inflater = newInflater();
        try
        {
            return this.v2.deserializeExternalReferenceMetadata(new InflaterInputStream(stream, inflater), stringIndexer);
        }
        finally
        {
            inflater.end();
        }
    }

    @Override
    public void serializeBackReferenceMetadata(OutputStream stream, ElementBackReferenceMetadata backReferenceMetadata, StringIndexer stringIndexer)
    {
        Deflater deflater = newDeflater();
        try
        {
            DeflaterOutputStream zipStream = new DeflaterOutputStream(stream, deflater);
            this.v2.serializeBackReferenceMetadata(zipStream, backReferenceMetadata, stringIndexer);
            zipStream.finish();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        finally
        {
            deflater.end();
        }
    }

    @Override
    public ElementBackReferenceMetadata deserializeBackReferenceMetadata(InputStream stream, StringIndexer stringIndexer)
    {
        Inflater inflater = newInflater();
        try
        {
            return this.v2.deserializeBackReferenceMetadata(new InflaterInputStream(stream, inflater), stringIndexer);
        }
        finally
        {
            inflater.end();
        }
    }

    @Override
    public void serializeFunctionNameMetadata(OutputStream stream, ModuleFunctionNameMetadata functionNameMetadata, StringIndexer stringIndexer)
    {
        Deflater deflater = newDeflater();
        try
        {
            DeflaterOutputStream zipStream = new DeflaterOutputStream(stream, deflater);
            this.v2.serializeFunctionNameMetadata(zipStream, functionNameMetadata, stringIndexer);
            zipStream.finish();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        finally
        {
            deflater.end();
        }
    }

    @Override
    public ModuleFunctionNameMetadata deserializeFunctionNameMetadata(InputStream stream, StringIndexer stringIndexer)
    {
        Inflater inflater = newInflater();
        try
        {
            return this.v2.deserializeFunctionNameMetadata(new InflaterInputStream(stream, inflater), stringIndexer);
        }
        finally
        {
            inflater.end();
        }
    }

    private Deflater newDeflater()
    {
        return new Deflater(Deflater.BEST_COMPRESSION, true);
    }

    private Inflater newInflater()
    {
        return new Inflater(true);
    }
}
