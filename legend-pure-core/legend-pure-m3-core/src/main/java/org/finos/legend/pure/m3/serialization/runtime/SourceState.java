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
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.Objects;

public class SourceState
{
    public static final Function<SourceState, ImmutableSet<CoreInstance>> SOURCE_STATE_INSTANCES = SourceState::getInstances;
    public static final Predicate2<SourceState, Source> IS_SOURCE_STATE_FOR_SOURCE = (sourceState, source) -> sourceState.getSource().equals(source);

    private final Source source;
    private final String content;
    private final ImmutableSet<CoreInstance> instances;
    private final ImmutableSet<Package> packages;
    private final ImmutableSet<? extends PackageableElement> importGroups;
    private final ImmutableMap<CoreInstance, String> instanceContentMap;
    private final ImmutableMap<CoreInstance, String> instanceImportGroupMap;

    SourceState(Source source, String content, MutableSet<CoreInstance> instances, MutableSet<? extends PackageableElement> importGroups)
    {
        this.source = source;
        this.content = content;
        this.instances = instances.toImmutable();
        this.importGroups = importGroups.toImmutable();
        this.packages = instances.collectIf(i -> i instanceof PackageableElement, i -> ((PackageableElement) i)._package()).toImmutable();

        String[] contentLines = content.split("\\R");
        this.instanceContentMap = this.instances.toMap(i -> i, i -> getInstanceContent(i, contentLines)).toImmutable();
        this.instanceImportGroupMap = this.instances.toMap(i -> i, i -> getInstanceImportId(i, this.importGroups)).toImmutable();
    }

    public Source getSource()
    {
        return this.source;
    }

    public String getContent()
    {
        return this.content;
    }

    public ImmutableSet<CoreInstance> getInstances()
    {
        return this.instances;
    }

    public ImmutableSet<Package> getPackages()
    {
        return this.packages;
    }

    public ImmutableSet<? extends PackageableElement> getImportGroups()
    {
        return this.importGroups;
    }

    public String instanceContentInSource(CoreInstance instance)
    {
        return this.instanceContentMap.get(instance);
    }

    public String instanceImportGroupInSource(CoreInstance instance)
    {
        return this.instanceImportGroupMap.get(instance);
    }

    public boolean instanceContentInSourceEqualsNewContent(CoreInstance instance, String newContent)
    {
        String cachedContent = this.instanceContentMap.get(instance);
        return (cachedContent != null) && !cachedContent.isEmpty() && cachedContent.equals(newContent);
    }

    public boolean instanceImportGroupInSourceEqualsNewImportGroup(CoreInstance instance, String newImportGroup)
    {
        String cachedImportGroup = this.instanceImportGroupMap.get(instance);
        return (cachedImportGroup != null) && !cachedImportGroup.isEmpty() && cachedImportGroup.equals(newImportGroup);
    }

    @Override
    public boolean equals(Object o)
    {
        return (this == o) || ((o instanceof SourceState) && Objects.equals(this.source, ((SourceState) o).source));
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(this.source);
    }

    private static String getInstanceContent(CoreInstance instance, String[] contentLines)
    {
        try
        {
            StringBuilder builder = new StringBuilder();

            SourceInformation sourceInformation = instance.getSourceInformation();
            if (sourceInformation.getStartLine() == sourceInformation.getEndLine())
            {
                String line = contentLines[sourceInformation.getStartLine() - 1];
                builder.append(line, Math.max(0, sourceInformation.getStartColumn() - 1), Math.min(sourceInformation.getEndColumn(), line.length())).append(System.lineSeparator());
            }
            else
            {
                // first line
                String startLine = contentLines[sourceInformation.getStartLine() - 1];
                builder.append(startLine, Math.max(0, sourceInformation.getStartColumn() - 1), startLine.length()).append(System.lineSeparator());

                // in between lines
                for (int line = sourceInformation.getStartLine(), end = sourceInformation.getEndLine() - 1; line < end; line++)
                {
                    builder.append(contentLines[line]).append(System.lineSeparator());
                }

                // last line
                String lastLine = contentLines[sourceInformation.getEndLine() - 1];
                builder.append(lastLine, 0, Math.min(sourceInformation.getEndColumn(), lastLine.length())).append(System.lineSeparator());
            }
            return builder.toString();
        }
        catch (Exception ignore)
        {
            return "";
        }
    }

    private static String getInstanceImportId(CoreInstance instance, RichIterable<? extends PackageableElement> importGroups)
    {
        if (instance.getSourceInformation() != null)
        {
            try
            {
                int instanceStartLine = instance.getSourceInformation().getStartLine();
                MutableList<? extends PackageableElement> possibleImportGroups = importGroups.select(ig -> ig.getSourceInformation().getEndLine() <= instanceStartLine, Lists.mutable.empty());
                if (possibleImportGroups.notEmpty())
                {
                    return possibleImportGroups.maxBy(ig -> ig.getSourceInformation().getEndLine())._name();
                }
            }
            catch (Exception ignore)
            {
                return "";
            }
        }
        return null;
    }
}
