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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageTools;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.runtime.navigation.NavigationHandler;
import org.finos.legend.pure.m4.serialization.Writer;

import java.io.OutputStream;
import java.util.List;
import java.util.regex.Pattern;

public class SourceRegistry
{
    private final MutableCodeStorage codeStorage;
    private final ImmutableList<SourceEventHandler> sourceEventHandlers;
    private final ConcurrentMutableMap<String, Source> sourcesById = ConcurrentHashMap.newMap();
    private final ParserLibrary parserLibrary;

    public SourceRegistry(MutableCodeStorage codeStorage, ParserLibrary parserLibrary, Iterable<? extends SourceEventHandler> sourceEventHandlers)
    {
        this.codeStorage = codeStorage;
        this.parserLibrary = parserLibrary;
        this.sourceEventHandlers = Lists.immutable.withAll(sourceEventHandlers);
    }

    public SourceRegistry(MutableCodeStorage codeStorage, ParserLibrary parserLibrary, SourceEventHandler... sourceEventHandlers)
    {
        this(codeStorage, parserLibrary, Lists.immutable.with(sourceEventHandlers));
    }

    public MutableCodeStorage getCodeStorage()
    {
        return this.codeStorage;
    }

    public RichIterable<String> getSourceIds()
    {
        return this.sourcesById.keysView();
    }

    public RichIterable<Source> getSources()
    {
        return this.sourcesById.valuesView();
    }

    public int getSourceCount()
    {
        return this.sourcesById.size();
    }

    public boolean hasSource(String id)
    {
        return this.sourcesById.containsKey(id);
    }

    public Source getSource(String id)
    {
        return this.sourcesById.get(id);
    }

    public RichIterable<SourceCoordinates> find(String string)
    {
        return find(string, true, null);
    }

    public RichIterable<SourceCoordinates> find(String string, boolean caseSensitive, Pattern sourceIdPattern)
    {
        MutableList<SourceCoordinates> results = Lists.mutable.empty();
        if (sourceIdPattern == null)
        {
            this.sourcesById.forEachValue(source -> results.addAllIterable(source.find(string, caseSensitive)));
        }
        else
        {
            this.sourcesById.forEachKeyValue((id, source) ->
            {
                if (sourceIdPattern.matcher(id).matches())
                {
                    results.addAllIterable(source.find(string, caseSensitive));
                }
            });
        }
        return results;
    }

    public RichIterable<SourceCoordinates> find(Pattern pattern)
    {
        return find(pattern, null);
    }

    public RichIterable<SourceCoordinates> find(Pattern pattern, Pattern sourceIdPattern)
    {
        MutableList<SourceCoordinates> results = Lists.mutable.empty();
        if (sourceIdPattern == null)
        {
            this.sourcesById.forEachValue(source -> results.addAllIterable(source.find(pattern)));
        }
        else
        {
            this.sourcesById.forEachKeyValue((id, source) ->
            {
                if (sourceIdPattern.matcher(id).matches())
                {
                    results.addAllIterable(source.find(pattern));
                }
            });
        }
        return results;
    }

    public RichIterable<SourceCoordinates> getPreviewTextWithCoordinates(Iterable<SourceCoordinates> coordinates)
    {
        MutableList<SourceCoordinates> results = Lists.mutable.empty();
        coordinates.forEach(coordinate ->
        {
            Source source = this.sourcesById.get(coordinate.getSourceId());
            if (source == null)
            {
                return;
            }
            results.add(new SourceCoordinates(
                    coordinate.getSourceId(),
                    coordinate.getStartLine(),
                    coordinate.getStartColumn(),
                    coordinate.getEndLine(),
                    coordinate.getEndColumn(),
                    source.getPreviewTextWithCoordinates(coordinate.getStartLine(), coordinate.getStartColumn(), coordinate.getEndLine(), coordinate.getEndColumn())
            ));
        });
        return results;
    }

    public RichIterable<String> findSourceIds(Pattern sourceIdPattern)
    {
        return this.sourcesById.keysView().select(id -> sourceIdPattern.matcher(id).matches(), Lists.mutable.empty());
    }

    public RichIterable<String> findSourceIds(String fileName)
    {
        String lowerCaseFileName = fileName.toLowerCase();
        return this.sourcesById.keysView().select(id -> id.toLowerCase().contains(lowerCaseFileName), Lists.mutable.empty());
    }

    void registerSource(Source source)
    {
        String id = source.getId();
        if (id == null)
        {
            throw new IllegalArgumentException("Source id cannot be null");
        }
        if (!CodeStorageTools.isPureFilePath(id))
        {
            throw new IllegalArgumentException("Invalid source id: " + id);
        }

        Source value = this.sourcesById.getIfAbsentPut(id, source);
        if (value != source)
        {
            throw new RuntimeException("Source id '" + id + "' is already in use");
        }

        source.setSourceRegistry(this);
    }

    void unregisterSource(String id)
    {
        this.sourcesById.remove(id);
    }

    void clear()
    {
        this.sourcesById.clear();
    }

    public void serialize(OutputStream stream)
    {
        BinarySourceSerializer.serialize(stream, this);
    }

    public void serialize(Writer writer)
    {
        BinarySourceSerializer.serialize(writer, this);
    }

    public RichIterable<SourceEventHandler> getSourceEventHandlers()
    {
        return this.sourceEventHandlers;
    }

    public NavigationHandler getNavigationHandler(String typePath)
    {
        return this.parserLibrary.getNavigationHandler(typePath);
    }
}
