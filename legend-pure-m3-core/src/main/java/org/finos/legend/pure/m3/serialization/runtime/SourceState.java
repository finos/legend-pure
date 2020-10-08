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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.Comparator;

public class SourceState
{
    public static final Function<SourceState, ImmutableSet<CoreInstance>> SOURCE_STATE_INSTANCES = new Function<SourceState, ImmutableSet<CoreInstance>>()
    {
        @Override
        public ImmutableSet<CoreInstance> valueOf(SourceState sourceState)
        {
            return sourceState.getInstances();
        }
    };

    public static final Predicate2<SourceState, Source> IS_SOURCE_STATE_FOR_SOURCE = new Predicate2<SourceState, Source>(){

        @Override
        public boolean accept(SourceState sourceState, Source source)
        {
            return sourceState.getSource().equals(source);
        }
    };

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

        MutableSet<Package> collection = Sets.mutable.with();
        for(CoreInstance instance : instances)
        {
            if(instance instanceof PackageableElement)
            {
                collection.add(((PackageableElement)instance)._package());
            }
        }
        this.packages = collection.toImmutable();

        MutableMap<CoreInstance, String> contentResultMap = UnifiedMap.newMapWith(this.instances.collect(new Function<CoreInstance, Pair<CoreInstance, String>>()
        {
            @Override
            public Pair<CoreInstance, String> valueOf(CoreInstance instance)
            {
                Pair<CoreInstance, String> instanceContent;
                try
                {
                    instanceContent = Tuples.pair(instance, SourceState.this.getInstanceContent(instance));
                }
                catch (Exception e)
                {
                    instanceContent = Tuples.pair(instance, "");
                }
                return instanceContent;
            }
        }));
        this.instanceContentMap = contentResultMap.toImmutable();

        MutableMap<CoreInstance, String> importGroupResultMap = UnifiedMap.newMapWith(this.instances.collect(new Function<CoreInstance, Pair<CoreInstance, String>>()
        {
            @Override
            public Pair<CoreInstance, String> valueOf(CoreInstance instance)
            {
                Pair<CoreInstance, String> instanceImportGroup;
                try
                {
                    instanceImportGroup = Tuples.pair(instance, SourceState.this.getInstanceImportId(instance));
                }
                catch (Exception e)
                {
                    instanceImportGroup = Tuples.pair(instance, "");
                }
                return instanceImportGroup;
            }
        }));
        this.instanceImportGroupMap = importGroupResultMap.toImmutable();
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

    private String getInstanceContent(CoreInstance instance)
    {
        SourceInformation sourceInformation = instance.getSourceInformation();

        String [] splitContent = this.content.split("\\r?\\n");
        StringBuilder contentStringBuilder = new StringBuilder();

        if (sourceInformation.getStartLine() == sourceInformation.getEndLine())
        {
            contentStringBuilder.append(splitContent[sourceInformation.getStartLine()-1].substring(Math.max(0, sourceInformation.getStartColumn()-1), Math.min(sourceInformation.getEndColumn(), splitContent[sourceInformation.getStartLine()-1].length())));
            contentStringBuilder.append(System.lineSeparator());
        }
        else
        {
            for (int line = sourceInformation.getStartLine() - 1; line < sourceInformation.getEndLine(); line++)
            {
                if (line == sourceInformation.getStartLine() - 1)
                {
                    contentStringBuilder.append(splitContent[line].substring(Math.max(0, sourceInformation.getStartColumn()-1)));
                }
                else if (line == sourceInformation.getEndLine() - 1)
                {
                    contentStringBuilder.append(splitContent[line].substring(0, Math.min(sourceInformation.getEndColumn(), splitContent[line].length())));
                }
                else
                {
                    contentStringBuilder.append(splitContent[line]);
                }
                contentStringBuilder.append(System.lineSeparator());
            }
        }
        return contentStringBuilder.toString();
    }

    private String getInstanceImportId(CoreInstance instance)
    {
        String importId = null;
        for (PackageableElement importGroup : this.importGroups.toSortedList(new Comparator<PackageableElement>()
        {
            @Override
            public int compare(PackageableElement o1, PackageableElement o2)
            {
                return o1.getSourceInformation().getEndLine() - o2.getSourceInformation().getEndLine();
            }
        }))
        {
            if (importGroup.getSourceInformation().getEndLine() <= instance.getSourceInformation().getStartLine())
            {
                importId = importGroup._name();
            }
        }
        return importId;
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
        return !"".equals(this.instanceContentMap.get(instance)) && this.instanceContentMap.get(instance).equals(newContent);
    }

    public boolean instanceImportGroupInSourceEqualsNewImportGroup(CoreInstance instance, String newImportGroup)
    {
        return !"".equals(this.instanceImportGroupMap.get(instance)) && this.instanceImportGroupMap.get(instance).equals(newImportGroup);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof SourceState))
        {
            return false;
        }

        SourceState that = (SourceState) o;

        return Comparators.nullSafeEquals(this.source, that.getSource());
    }

    @Override
    public int hashCode()
    {
        return this.source == null ? 0 : this.source.hashCode();
    }
}
