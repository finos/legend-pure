// Copyright 2022 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.filesystem.repository;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.Iterate;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class CodeRepositorySet
{
    private final ImmutableMap<String, CodeRepository> repositoriesByName;

    private CodeRepositorySet(ImmutableMap<String, CodeRepository> repositoriesByName)
    {
        this.repositoriesByName = repositoriesByName;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        return (other instanceof CodeRepositorySet) && this.repositoriesByName.equals(((CodeRepositorySet) other).repositoriesByName);
    }

    @Override
    public int hashCode()
    {
        return this.repositoriesByName.castToMap().keySet().hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append('{');
        this.repositoriesByName.keysView().appendString(builder, ", ");
        return builder.append('}').toString();
    }

    public boolean hasRepository(String repositoryName)
    {
        return this.repositoriesByName.containsKey(repositoryName);
    }

    public RichIterable<String> getRepositoryNames()
    {
        return this.repositoriesByName.keysView();
    }

    public RichIterable<CodeRepository> getRepositories()
    {
        return this.repositoriesByName.valuesView();
    }

    public CodeRepository getRepository(String repositoryName)
    {
        CodeRepository found = this.repositoriesByName.get(repositoryName);
        if (found == null)
        {
            throw new IllegalArgumentException("The code repository '" + repositoryName + "' can't be found!");
        }
        return found;
    }

    public Optional<CodeRepository> getOptionalRepository(String repositoryName)
    {
        return Optional.ofNullable(this.repositoriesByName.get(repositoryName));
    }

    public int size()
    {
        return this.repositoriesByName.size();
    }

    public void forEach(Consumer<? super CodeRepository> consumer)
    {
        this.repositoriesByName.forEachValue(consumer::accept);
    }

    /**
     * Return the minimal subset of this CodeRepositorySet which contains all the selected repositories and is closed
     * under repository visibility. That is, it is guaranteed to include all the selected repositories as well as all
     * repositories from the original set which are visible to any repository in the subset.
     *
     * @param selectedRepositories names of repositories to include in the subset
     * @return subset with selected repositories
     */
    public CodeRepositorySet subset(String... selectedRepositories)
    {
        return subset(ArrayAdapter.adapt(selectedRepositories));
    }

    /**
     * Return the minimal subset of this CodeRepositorySet which contains all the selected repositories and is closed
     * under repository visibility. That is, it is guaranteed to include all the selected repositories as well as all
     * repositories from the original set which are visible to any repository in the subset.
     *
     * @param selectedRepositories names of repositories to include in the subset
     * @return subset with selected repositories
     */
    public CodeRepositorySet subset(Iterable<? extends String> selectedRepositories)
    {
        MutableMap<String, CodeRepository> resolved = Maps.mutable.with("platform", getRepository("platform"));
        Deque<CodeRepository> deque = Iterate.collect(selectedRepositories, this::getRepository, new ArrayDeque<>(size()));
        while (!deque.isEmpty())
        {
            CodeRepository repository = deque.removeLast();
            if (resolved.put(repository.getName(), repository) == null)
            {
                if (repository instanceof GenericCodeRepository)
                {
                    ((GenericCodeRepository) repository).getDependencies().collect(this.repositoriesByName::get, deque);
                }
                else
                {
                    this.repositoriesByName.select(r -> (r != repository) && repository.isVisible(r), deque);
                }
            }
        }
        return (resolved.size() == this.repositoriesByName.size()) ? this : new CodeRepositorySet(resolved.toImmutable());
    }

    public static Builder newBuilder()
    {
        return builder();
    }

    public static Builder newBuilder(CodeRepositorySet manager)
    {
        return builder(manager);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static Builder builder(CodeRepositorySet manager)
    {
        return new Builder(manager);
    }

    public static class Builder
    {
        private final MutableMap<String, CodeRepository> repositories = Maps.mutable.empty();

        private Builder()
        {
        }

        private Builder(CodeRepositorySet manager)
        {
            this.repositories.putAll(manager.repositoriesByName.castToMap());
        }

        public void addCodeRepository(CodeRepository repository)
        {
            Objects.requireNonNull(repository, "repository may not be null");
            Objects.requireNonNull(repository.getName(), "repository must have a name");
            CodeRepository found = this.repositories.getIfAbsentPut(repository.getName(), repository);
            if (found != repository)
            {
                throw new IllegalStateException("The code repository " + repository.getName() + " already exists!");
            }
        }

        public void addCodeRepositories(Iterable<? extends CodeRepository> repositories)
        {
            repositories.forEach(this::addCodeRepository);
        }

        public void addCodeRepositories(CodeRepository... repositories)
        {
            ArrayIterate.forEach(repositories, this::addCodeRepository);
        }

        public void removeCodeRepository(String repositoryName)
        {
            this.repositories.removeKey(repositoryName);
        }

        public void removeCodeRepositories(Iterable<? extends String> repositoryNames)
        {
            repositoryNames.forEach(this::removeCodeRepositories);
        }

        public void removeCodeRepositories(String... repositoryNames)
        {
            ArrayIterate.forEach(repositoryNames, this::removeCodeRepository);
        }

        public Builder withCodeRepository(CodeRepository repository)
        {
            addCodeRepository(repository);
            return this;
        }

        public Builder withCodeRepositories(Iterable<? extends CodeRepository> repositories)
        {
            addCodeRepositories(repositories);
            return this;
        }

        public Builder withCodeRepositories(CodeRepository... repositories)
        {
            addCodeRepositories(repositories);
            return this;
        }

        public Builder withoutCodeRepository(String repositoryName)
        {
            removeCodeRepository(repositoryName);
            return this;
        }

        public Builder withoutCodeRepositories(Iterable<? extends String> repositoryNames)
        {
            removeCodeRepositories(repositoryNames);
            return this;
        }

        public Builder withoutCodeRepositories(String... repositoryNames)
        {
            removeCodeRepositories(repositoryNames);
            return this;
        }

        /**
         * Find the minimal subset of which contains all the selected repositories and is closed under repository
         * dependency and visibility. That is, it is guaranteed to include all the selected repositories as well as all
         * repositories which are visible to or a dependency of any repository in the subset. If a dependency is missing
         * that prevents computation of the subset, the builder will not be modified and an IllegalStateException will
         * be thrown.
         *
         * @param selectedRepositories names of repositories to include in the subset
         * @return this builder
         * @throws IllegalArgumentException if any of the selected repositories is not found
         * @throws IllegalStateException if the subset cannot be computed because of a missing dependency
         */
        public Builder subset(String... selectedRepositories)
        {
            return subset(ArrayAdapter.adapt(selectedRepositories));
        }

        /**
         * Find the minimal subset of which contains all the selected repositories and is closed under repository
         * dependency and visibility. That is, it is guaranteed to include all the selected repositories as well as all
         * repositories which are visible to or a dependency of any repository in the subset. If a dependency is missing
         * that prevents computation of the subset, the builder will not be modified and an IllegalStateException will
         * be thrown.
         *
         * @param selectedRepositories names of repositories to include in the subset
         * @return this builder
         * @throws IllegalArgumentException if any of the selected repositories is not found
         * @throws IllegalStateException if the subset cannot be computed because of a missing dependency
         */
        public Builder subset(Iterable<? extends String> selectedRepositories)
        {
            Deque<CodeRepository> deque = new ArrayDeque<>();
            selectedRepositories.forEach(repoName ->
            {
                CodeRepository repo = this.repositories.get(repoName);
                if (repo == null)
                {
                    throw new IllegalArgumentException("The code repository '" + repoName + "' can't be found!");
                }
                deque.add(repo);
            });

            MutableSet<String> selected = Sets.mutable.empty();
            while (!deque.isEmpty())
            {
                CodeRepository repository = deque.removeLast();
                if (selected.add(repository.getName()))
                {
                    if (repository instanceof GenericCodeRepository)
                    {
                        ((GenericCodeRepository) repository).getDependencies().forEach(depName ->
                        {
                            if (!selected.contains(depName))
                            {
                                CodeRepository repo = this.repositories.get(depName);
                                if (repo == null)
                                {
                                    throw new IllegalStateException("Cannot compute subset because dependency '" + depName + "' is missing for '" + repository.getName() + "'");
                                }
                                deque.add(repo);
                            }
                        });
                    }
                    else
                    {
                        this.repositories.select(r -> !selected.contains(r.getName()) && repository.isVisible(r), deque);
                    }
                }
            }

            this.repositories.removeIf((name, repo) -> !selected.contains(name));
            return this;
        }

        public CodeRepositorySet build()
        {
            // validate that all dependencies are present
            this.repositories.forEachValue(r ->
            {
                if (r instanceof GenericCodeRepository)
                {
                    MutableList<String> missingDependencies = ((GenericCodeRepository) r).getDependencies().reject(this.repositories::containsKey, Lists.mutable.empty());
                    if (missingDependencies.notEmpty())
                    {
                        StringBuilder builder = new StringBuilder("The ").append((missingDependencies.size() == 1) ? "dependency" : "dependencies");
                        missingDependencies.sortThis().appendString(builder, " '", "', '", "'");
                        builder.append(" required by the Code Repository '").append(r.getName()).append("' can't be found!");
                        throw new IllegalStateException(builder.toString());
                    }
                }
            });

            // build
            return new CodeRepositorySet(this.repositories.toImmutable());
        }
    }
}
