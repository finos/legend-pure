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

package org.finos.legend.pure.m3.serialization.runtime;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class BinarySourceSerializer
{
    private BinarySourceSerializer()
    {
    }

    // Serialize

    public static void serialize(OutputStream stream, SourceRegistry sourceRegistry)
    {
        try (Writer writer = BinaryWriters.newBinaryWriter(stream))
        {
            MutableObjectIntMap<String> parserIds = ObjectIntMaps.mutable.empty();
            ListIterable<String> parserNames = getParserNames(sourceRegistry).toSortedList();
            writer.writeInt(parserNames.size());
            for (String parserName : parserNames)
            {
                writer.writeString(parserName);
                parserIds.put(parserName, parserIds.size());
            }
            MutableList<Source> sortedSources = sourceRegistry.getSources().toSortedListBy(Source::getId);
            writer.writeInt(sortedSources.size());
            for (Source source : sortedSources)
            {
                try
                {
                    serializeSource(writer, source, parserIds);
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Error serializing source: " + source.getId(), e);
                }
            }
        }
    }

    private static SetIterable<String> getParserNames(SourceRegistry sourceRegistry)
    {
        MutableSet<String> parserNames = Sets.mutable.empty();
        for (Source source : sourceRegistry.getSources())
        {
            ListMultimap<Parser, CoreInstance> elementsByParser = source.getElementsByParser();
            if (elementsByParser != null)
            {
                elementsByParser.keysView().collect(Parser::getName, parserNames);
            }
        }
        return parserNames;
    }

    private static void serializeSource(Writer writer, Source source, ObjectIntMap<String> parserIds)
    {
        // Write id, immutable, and compiled
        writer.writeString(source.getId());
        writer.writeBoolean(source.isImmutable());
        writer.writeBoolean(source.isCompiled());

        // Write content
        writer.writeString(source.getContent());

        // Write elements by parser
        ListMultimap<Parser, CoreInstance> elementsByParser = source.getElementsByParser();
        if (elementsByParser == null)
        {
            writer.writeInt(-1);
        }
        else
        {
            MutableList<Parser> parsers = elementsByParser.keysView().toSortedListBy(Parser::getName);
            writer.writeInt(parsers.size());
            for (Parser parser : parsers)
            {
                writer.writeInt(parserIds.getOrThrow(parser.getName()));
                ListIterable<CoreInstance> instances = elementsByParser.get(parser);
                writer.writeInt(instances.size());
                for (CoreInstance instance : instances)
                {
                    writer.writeInt(instance.getSyntheticId());
                }
            }
        }
    }

    // Build

    public static void build(InputStream stream, SourceRegistry registry, IntObjectMap<CoreInstance> instances, ParserLibrary library, Context context)
    {
        try (Reader reader = BinaryReaders.newBinaryReader(stream))
        {
            String[] parserNames = reader.readStringArray();
            int sourceCount = reader.readInt();
            for (int i = 0; i < sourceCount; i++)
            {
                Source source;
                try
                {
                    source = buildSource(reader, instances, parserNames, library, context);
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Error building source " + (i + 1) + " of " + sourceCount, e);
                }
                registry.registerSource(source);
            }
        }
    }

    public static void build(byte[] serialized, SourceRegistry registry, IntObjectMap<CoreInstance> instances, ParserLibrary library, Context context)
    {
        build(new ByteArrayInputStream(serialized), registry, instances, library, context);
    }

    private static Source buildSource(Reader reader, IntObjectMap<CoreInstance> instancesById, String[] parserNames, ParserLibrary library, Context context)
    {
        // Read id
        String id = reader.readString();
        try
        {
            // Read immutable and compiled
            boolean immutable = reader.readBoolean();
            boolean compiled = reader.readBoolean();

            // Read content
            String content = reader.readString();

            // Read elements by parser
            MutableListMultimap<Parser, CoreInstance> elementsByParser;
            int parserCount = reader.readInt();
            if (parserCount == -1)
            {
                elementsByParser = null;
            }
            else
            {
                elementsByParser = Multimaps.mutable.list.empty();
                for (int i = 0; i < parserCount; i++)
                {
                    String parserName = parserNames[reader.readInt()];
                    Parser parser = library.getParser(parserName);
                    if (parser == null)
                    {
                        throw new RuntimeException("Unknown parser: " + parserName);
                    }
                    elementsByParser.putAll(parser, readCoreInstancesById(reader, instancesById));
                }
            }

            // Build the source
            Source source = new Source(id, immutable, false, content);
            source.setCompiled(compiled);
            if (elementsByParser != null)
            {
                source.linkInstances(elementsByParser);
                context.registerInstancesByClassifier(source.getNewInstances());
                context.registerFunctionsByName(source.getNewInstances());
            }
            return source;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error building source " + id, e);
        }
    }

    private static ListIterable<CoreInstance> readCoreInstancesById(Reader reader, IntObjectMap<CoreInstance> instancesById)
    {
        int count = reader.readInt();
        if (count == -1)
        {
            return null;
        }
        MutableList<CoreInstance> instances = FastList.newList(count);
        for (int i = 0; i < count; i++)
        {
            instances.add(readCoreInstanceById(reader, instancesById));
        }
        return instances;
    }

    private static CoreInstance readCoreInstanceById(Reader reader, IntObjectMap<CoreInstance> instancesById)
    {
        int syntheticId = reader.readInt();
        CoreInstance instance = instancesById.get(syntheticId);
        if (instance == null)
        {
            throw new RuntimeException("Cannot find instance with synthetic id: " + syntheticId);
        }
        return instance;
    }
}
