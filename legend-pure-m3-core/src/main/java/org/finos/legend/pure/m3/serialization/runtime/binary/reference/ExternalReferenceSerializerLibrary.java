// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.runtime.binary.reference;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;

public class ExternalReferenceSerializerLibrary
{
    private final ImmutableMap<String, ExternalReferenceSerializer> serializers;

    private ExternalReferenceSerializerLibrary(ImmutableMap<String, ExternalReferenceSerializer> serializers)
    {
        this.serializers = serializers;
    }

    public ExternalReferenceSerializer getSerializer(String typePath)
    {
        return this.serializers.get(typePath);
    }

    public static ExternalReferenceSerializerLibrary newLibrary(Iterable<? extends ExternalReferenceSerializer> serializers)
    {
        return new ExternalReferenceSerializerLibrary(indexByType(serializers));
    }

    public static ExternalReferenceSerializerLibrary newLibrary(ParserLibrary parsers)
    {
        return newLibrary(parsers.getParsers().flatCollect(Parser::getExternalReferenceSerializers));
    }

    public static ExternalReferenceSerializerLibrary newLibrary(PureRuntime runtime)
    {
        return newLibrary(runtime.getIncrementalCompiler().getParserLibrary());
    }

    private static ImmutableMap<String, ExternalReferenceSerializer> indexByType(Iterable<? extends ExternalReferenceSerializer> serializers)
    {
        MutableMap<String, ExternalReferenceSerializer> serializersByType = Maps.mutable.empty();
        serializers.forEach(serializer ->
        {
            String typePath = serializer.getTypePath();
            ExternalReferenceSerializer old = serializersByType.put(typePath, serializer);
            if ((old != null) && (old != serializer))
            {
                throw new RuntimeException("Multiple serializers for " + typePath);
            }
        });
        return serializersByType.toImmutable();
    }
}
