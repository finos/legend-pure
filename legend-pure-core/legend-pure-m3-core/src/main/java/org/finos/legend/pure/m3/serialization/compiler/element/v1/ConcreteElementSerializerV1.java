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

package org.finos.legend.pure.m3.serialization.compiler.element.v1;

import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.compiler.element.ConcreteElementSerializerExtension;
import org.finos.legend.pure.m3.serialization.compiler.element.DeserializedConcreteElement;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProvider;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexer;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;

public class ConcreteElementSerializerV1 implements ConcreteElementSerializerExtension
{
    @Override
    public int version()
    {
        return 1;
    }

    @Override
    public void serialize(Writer writer, CoreInstance element, StringIndexer stringIndexer, ReferenceIdProvider referenceIdProvider, ProcessorSupport processorSupport)
    {
        new SerializerV1(element, stringIndexer, referenceIdProvider, processorSupport).serialize(writer);
    }

    @Override
    public DeserializedConcreteElement deserialize(Reader reader, StringIndexer stringIndexer, int referenceIdVersion)
    {
        return new DeserializerV1().deserialize(reader, stringIndexer, referenceIdVersion);
    }
}
