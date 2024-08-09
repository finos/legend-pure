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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
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

    public CoreInstance getImportGroup()
    {
        return this.importGroup;
    }

    private static RichIterable<CoreInstance> getImportCandidates(String id, ModelRepository repository, ProcessorSupport processorSupport, CoreInstance importStubNode, RichIterable<CodeRepository> codeRepositories)
    {
        return SearchTools.findInAllPackages(id, repository).select(instance ->
        {
            String sourceId = (importStubNode.getSourceInformation() == null) ? null : importStubNode.getSourceInformation().getSourceId();
            return Visibility.isVisibleInSource(instance, sourceId, codeRepositories, processorSupport);
        });
    }

    private static String buildInfo(String idOrPath, RichIterable<CoreInstance> candidates)
    {
        StringBuilder builder = new StringBuilder(idOrPath).append(" has not been defined!");
        if (candidates.notEmpty())
        {
            MutableList<String> candidatePaths = candidates.collect(PackageableElement::getUserPathForPackageableElement, Lists.mutable.empty()).sortThis();
            builder.append(" The system found ").append(candidatePaths.size()).append(" possible matches:");
            candidatePaths.forEach(path -> builder.append("\n    ").append(path));
        }
        return builder.toString();
    }
}
