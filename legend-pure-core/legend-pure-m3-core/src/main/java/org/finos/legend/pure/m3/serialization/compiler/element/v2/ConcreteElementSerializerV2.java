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

package org.finos.legend.pure.m3.serialization.compiler.element.v2;

import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.compiler.element.ConcreteElementSerializerExtension;
import org.finos.legend.pure.m3.serialization.compiler.element.DeserializedConcreteElement;
import org.finos.legend.pure.m3.serialization.compiler.element.v1.ConcreteElementSerializerV1;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProvider;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexer;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m3.tools.CompressorPool;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class ConcreteElementSerializerV2 implements ConcreteElementSerializerExtension
{
    private static final int COMPRESSION_LEVEL = 7;

    private final ConcreteElementSerializerV1 v1 = new ConcreteElementSerializerV1();

    @Override
    public int version()
    {
        return 2;
    }

    @Override
    public void serialize(OutputStream stream, CoreInstance element, StringIndexer stringIndexer, ReferenceIdProvider referenceIdProvider, ProcessorSupport processorSupport)
    {
        try (CompressorPool.CloseableDeflater deflater = CompressorPool.getInstance().borrowDeflater(COMPRESSION_LEVEL, true))
        {
            DeflaterOutputStream zipStream = new DeflaterOutputStream(stream, deflater);
            this.v1.serialize(zipStream, element, stringIndexer, referenceIdProvider, processorSupport);
            zipStream.finish();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public DeserializedConcreteElement deserialize(InputStream stream, StringIndexer stringIndexer, int referenceIdVersion)
    {
        try (CompressorPool.CloseableInflater inflater = CompressorPool.getInstance().borrowInflater(true))
        {
            return this.v1.deserialize(new InflaterInputStream(stream, inflater), stringIndexer, referenceIdVersion);
        }
    }
}
