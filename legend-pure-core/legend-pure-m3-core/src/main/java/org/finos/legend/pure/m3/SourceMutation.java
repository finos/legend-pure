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

package org.finos.legend.pure.m3;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m3.serialization.runtime.SourceRegistry;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;

import java.util.regex.Pattern;

public class SourceMutation
{
    public static final Predicate<CoreInstance> IS_MARKED_FOR_DELETION = SourceMutation::isMarkedForDeletion;

    private static final CompileState MARKED_FOR_DELETION = CompileState.COMPILE_EVENT_EXTRA_STATE_2;
    private static final Pattern LINE_SPLITTER = Pattern.compile("^", Pattern.MULTILINE);

    private final MutableListMultimap<String, IntIntPair> lineRangesToRemoveByFile = Multimaps.mutable.list.empty();
    private final MutableSet<CoreInstance> markedForDeletion = Sets.mutable.empty();

    public ListMultimap<String, IntIntPair> getLineRangesToRemoveByFile()
    {
        return this.lineRangesToRemoveByFile;
    }

    public SetIterable<CoreInstance> getMarkedForDeletion()
    {
        return this.markedForDeletion.asUnmodifiable();
    }

    public RichIterable<String> getModifiedFiles()
    {
        return this.lineRangesToRemoveByFile.keysView();
    }

    public void delete(CoreInstance instance)
    {
        instance.addCompileState(MARKED_FOR_DELETION);
        if (this.markedForDeletion.add(instance))
        {
            SourceInformation sourceInfo = instance.getSourceInformation();
            addLinesToRemove(sourceInfo.getSourceId(), sourceInfo.getStartLine(), sourceInfo.getEndLine());
        }
    }

    public void merge(SourceMutation sourceMutation)
    {
        this.lineRangesToRemoveByFile.putAll(sourceMutation.lineRangesToRemoveByFile);
        this.markedForDeletion.addAll(sourceMutation.markedForDeletion);
    }

    public void perform(PureRuntime pureRuntime)
    {
        if (this.lineRangesToRemoveByFile.notEmpty())
        {
            SourceRegistry sourceRegistry = pureRuntime.getSourceRegistry();
            for (String sourceId : this.lineRangesToRemoveByFile.keysView())
            {
                IntSet set = calculateLinesToRemove(this.lineRangesToRemoveByFile.get(sourceId));
                Source source = sourceRegistry.getSource(sourceId);
                if (source == null)
                {
                    throw new RuntimeException("Unknown source: " + sourceId);
                }
                String file = source.getContent();
                String[] lines = LINE_SPLITTER.split(file);
                StringBuilder buffer = new StringBuilder(file.length());
                for (int i = 0; i < lines.length; i++)
                {
                    if (!set.contains(i + 1))
                    {
                        buffer.append(lines[i]);
                    }
                }
                pureRuntime.modify(sourceId, buffer.toString());
            }
            pureRuntime.compile();
        }
    }

    private IntSet calculateLinesToRemove(Iterable<? extends IntIntPair> ranges)
    {
        MutableIntSet set = IntSets.mutable.empty();
        for (IntIntPair range : ranges)
        {
            for (int i = range.getOne(), end = range.getTwo(); i <= end; i++)
            {
                set.add(i);
            }
        }
        return set;
    }

    private void addLinesToRemove(String sourceId, int from, int to)
    {
        this.lineRangesToRemoveByFile.put(sourceId, PrimitiveTuples.pair(from, to));
    }

    public static boolean isMarkedForDeletion(CoreInstance instance)
    {
        return instance.hasCompileState(MARKED_FOR_DELETION);
    }
}
