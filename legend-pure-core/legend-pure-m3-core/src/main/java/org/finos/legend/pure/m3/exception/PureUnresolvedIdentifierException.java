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

package org.finos.legend.pure.m3.exception;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.compiler.visibility.Visibility;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.tools.SearchTools;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class PureUnresolvedIdentifierException extends PureCompilationException
{
    private final String idOrPath;
    private final CoreInstance importGroup;
    private final CoreInstance importStubNode;
    private final String id;
    private final ModelRepository repository;
    private final ProcessorSupport processorSupport;

    public PureUnresolvedIdentifierException(SourceInformation sourceInformation, String idOrPath, String id, ModelRepository repository, ProcessorSupport processorSupport, CoreInstance importStubNode, CoreInstance importGroup)
    {
        super(sourceInformation, buildInfo(idOrPath, getImportCandidates(id, repository, processorSupport, importStubNode, null)));
        this.repository = repository;
        this.processorSupport = processorSupport;
        this.idOrPath = idOrPath;
        this.importStubNode = importStubNode;
        this.importGroup = importGroup;
        this.id = id;
    }

    public String getIdOrPath()
    {
        return this.idOrPath;
    }

    public RichIterable<CoreInstance> getImportCandidates(RichIterable<CodeRepository> codeRepositories)
    {
        return getImportCandidates(this.id, this.repository, this.processorSupport, this.importStubNode, codeRepositories);
    }

    private static RichIterable<CoreInstance> getImportCandidates(String id, ModelRepository repository, ProcessorSupport processorSupport, CoreInstance importStubNode, RichIterable<CodeRepository> codeRepositories)
    {
        return SearchTools.findInAllPackages(id, repository).select((CoreInstance instance) ->
        {
            String sourceId = null == importStubNode.getSourceInformation() ? null : importStubNode.getSourceInformation().getSourceId();
            return Visibility.isVisibleInSource(instance, sourceId, codeRepositories, processorSupport);
        });
    }

    public CoreInstance getImportGroup()
    {
        return this.importGroup;
    }

    private static String buildInfo(String idOrPath, RichIterable<CoreInstance> candidates)
    {
        StringBuilder builder = new StringBuilder(idOrPath);
        builder.append(" has not been defined!");
        if (Iterate.notEmpty(candidates))
        {
            MapIterable<String, CoreInstance> candidatesByPath = indexCandidatesByPath(candidates);
            builder.append(" The system found ");
            builder.append(candidatesByPath.size());
            builder.append(" possible matches:");
            for (String path : candidatesByPath.keysView().toSortedList())
            {
                builder.append("\n    ");
                builder.append(path);
            }
        }
        return builder.toString();
    }

    private static MapIterable<String, CoreInstance> indexCandidatesByPath(Iterable<? extends CoreInstance> candidates)
    {
        MutableMap<String, CoreInstance> index = Maps.mutable.empty();
        for (CoreInstance candidate : candidates)
        {
            index.put(PackageableElement.getUserPathForPackageableElement(candidate), candidate);
        }
        return index;
    }
}
