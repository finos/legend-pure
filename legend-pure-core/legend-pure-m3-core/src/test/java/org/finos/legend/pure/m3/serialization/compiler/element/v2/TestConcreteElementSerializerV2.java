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

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.compiler.element.AbstractTestConcreteElementSerializerExtension;
import org.finos.legend.pure.m3.serialization.compiler.element.ConcreteElementSerializerExtension;
import org.finos.legend.pure.m3.serialization.compiler.element.v1.ConcreteElementSerializerV1;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProvider;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProviders;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexer;
import org.finos.legend.pure.m3.tools.GraphTools;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;

public class TestConcreteElementSerializerV2 extends AbstractTestConcreteElementSerializerExtension
{
    // the default size of a BufferedOutputStream's buffer
    private static final int STREAM_BUFFER_SIZE = 8192;

    @Override
    protected ConcreteElementSerializerExtension getExtension()
    {
        return new ConcreteElementSerializerV2();
    }

    @Test
    public void testPayloadIsDeflatedV1Payload()
    {
        // The V2 payload is the V1 payload as raw deflate at level 7. How the V1 payload is fed to the deflater must not
        // change the output, since files written earlier must stay byte-identical.
        ConcreteElementSerializerV1 v1 = new ConcreteElementSerializerV1();
        ConcreteElementSerializerV2 v2 = new ConcreteElementSerializerV2();
        StringIndexer stringIndexer = StringIndexer.defaultStringIndexer();
        ReferenceIdProvider referenceIdProvider = ReferenceIdProviders.builder()
                .withAvailableExtensions()
                .withProcessorSupport(processorSupport)
                .build()
                .provider();

        ListIterable<? extends CoreInstance> elements = GraphTools.getTopLevelAndPackagedElements(processorSupport)
                .select(e -> e.getSourceInformation() != null)
                .toList();
        Assert.assertNotEquals(0, elements.size());

        int largePayloadCount = 0;
        for (CoreInstance element : elements)
        {
            String path = PackageableElement.getUserPathForPackageableElement(element);

            ByteArrayOutputStream v1Stream = new ByteArrayOutputStream();
            v1.serialize(v1Stream, element, stringIndexer, referenceIdProvider, processorSupport);
            byte[] v1Payload = v1Stream.toByteArray();

            ByteArrayOutputStream v2Stream = new ByteArrayOutputStream();
            v2.serialize(v2Stream, element, stringIndexer, referenceIdProvider, processorSupport);

            Assert.assertArrayEquals(path, deflate(v1Payload), v2Stream.toByteArray());
            if (v1Payload.length > STREAM_BUFFER_SIZE)
            {
                largePayloadCount++;
            }
        }

        // some payloads must be larger than a stream buffer for the test to cover more than one buffer's worth
        Assert.assertNotEquals(0, largePayloadCount);
    }

    private static byte[] deflate(byte[] bytes)
    {
        Deflater deflater = new Deflater(7, true);
        try
        {
            deflater.setInput(bytes);
            deflater.finish();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            byte[] buffer = new byte[STREAM_BUFFER_SIZE];
            while (!deflater.finished())
            {
                int count = deflater.deflate(buffer);
                stream.write(buffer, 0, count);
            }
            return stream.toByteArray();
        }
        finally
        {
            deflater.end();
        }
    }
}
