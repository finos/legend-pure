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
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.AppCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.ContractsCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.DatamartCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.ModelCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.ModelValidationCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.ScratchCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.SystemCodeRepository;

import java.util.Comparator;

public class RepositoryComparator implements Comparator<String>
{
    private static final ImmutableList<Class<? extends CodeRepository>> REPO_CLASS_SORT_ORDER = Lists.immutable.with(PlatformCodeRepository.class, SystemCodeRepository.class, ModelCodeRepository.class, ModelValidationCodeRepository.class, DatamartCodeRepository.class, AppCodeRepository.class, ContractsCodeRepository.class, ScratchCodeRepository.class);
    private final ImmutableMap<String, CodeRepository> repositories;
    private final MutableMap<CodeRepository, SetIterable<CodeRepository>> dependencies = Maps.mutable.empty();
    private final MutableMap<ModelCodeRepository, SetIterable<ModelCodeRepository>> modelRepoReverseDependencies = Maps.mutable.empty();

    public RepositoryComparator(RichIterable<? extends CodeRepository> repositories)
    {
        this.repositories = repositories.toMap(CodeRepository.GET_NAME, Functions.<CodeRepository>getPassThru()).toImmutable();
    }

    @Override
    public int compare(String repo1Name, String repo2Name)
    {
        if (null == repo1Name)
        {
            return null == repo2Name ? 0 : 1;
        }
        if (null == repo2Name)
        {
            return -1;
        }
        if (repo1Name.equals(repo2Name))
        {
            return 0;
        }
        CodeRepository repo1 = this.getRepositoryByName(repo1Name);
        CodeRepository repo2 = this.getRepositoryByName(repo2Name);
        if (this.dependsOn(repo2, repo1))
        {
            return -1;
        }
        if (this.dependsOn(repo1, repo2))
        {
            return 1;
        }
        Class<? extends CodeRepository> repoClass1 = repo1.getClass();
        Class<? extends CodeRepository> repoClass2 = repo2.getClass();
        if (repoClass1 == repoClass2)
        {
            if (repo1 instanceof ModelCodeRepository)
            {
                // HACK: This isn't really a valid comparison, but it's good enough for now:
                SetIterable<ModelCodeRepository> repo1ReverseVisibility = this.getModelRepoReverseVisibility((ModelCodeRepository)repo1);
                SetIterable<ModelCodeRepository> repo2ReverseVisibility = this.getModelRepoReverseVisibility((ModelCodeRepository)repo2);
                int repo1ReverseVisibilityCount = repo1ReverseVisibility.size();
                int repo2ReverseVisibilityCount = repo2ReverseVisibility.size();
                if (repo1ReverseVisibilityCount != repo2ReverseVisibilityCount)
                {
                    return Integer.compare(repo2ReverseVisibilityCount, repo1ReverseVisibilityCount);
                }
            }
            return repo1Name.compareTo(repo2Name);
        }
        return Integer.compare(REPO_CLASS_SORT_ORDER.indexOf(repoClass1), REPO_CLASS_SORT_ORDER.indexOf(repoClass2));
    }

    private CodeRepository getRepositoryByName(String repoName)
    {
        CodeRepository repository = this.repositories.get(repoName);
        if (null == repository)
        {
            throw new IllegalArgumentException("Unknown repository: " + repoName);
        }
        return repository;
    }

    private boolean dependsOn(CodeRepository repo1, CodeRepository repo2)
    {
        return this.getDependencies(repo1).contains(repo2);
    }

    private SetIterable<CodeRepository> getDependencies(CodeRepository repo)
    {
        return this.dependencies.getIfAbsentPutWithKey(repo, (CodeRepository c)-> PureCodeStorage.getRepositoryDependencies(this.repositories.valuesView(), c));
    }

    private SetIterable<ModelCodeRepository> getModelRepoReverseVisibility(ModelCodeRepository modelRepo)
    {
        return this.modelRepoReverseDependencies.getIfAbsentPutWithKey(modelRepo,  this::computeModelRepoReverseVisibility);
    }

    private ImmutableSet<ModelCodeRepository> computeModelRepoReverseVisibility(ModelCodeRepository modelRepo)
    {
        MutableSet<ModelCodeRepository> reverseVisibility = Sets.mutable.empty();
        String modelName = modelRepo.getModelName();
        for (CodeRepository otherRepo : this.repositories.valuesView())
        {
            if (otherRepo != modelRepo && otherRepo instanceof ModelCodeRepository)
            {
                ModelCodeRepository otherModelRepo = (ModelCodeRepository)otherRepo;
                if (otherModelRepo.getVisibleModels().contains(modelName))
                {
                    reverseVisibility.add(otherModelRepo);
                }
            }
        }
        return reverseVisibility.toImmutable();
    }
}
