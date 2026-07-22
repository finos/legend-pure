// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.lsp;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.empty.EmptyCodeStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryScanner
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryScanner.class);
    private static final String RESOURCES_MARKER = "src/main/resources";

    private final Map<String, Path> repoToResourcesRoot = new ConcurrentHashMap<>();
    private final Map<String, Path> repoToDefinitionFile = new ConcurrentHashMap<>();

    public void scan(Iterable<Path> workspaceRoots)
    {
        for (Path root : workspaceRoots)
        {
            scanRoot(root);
        }
        LOGGER.info("Repository scan complete: {} repositories mapped", this.repoToResourcesRoot.size());
        for (Map.Entry<String, Path> entry : this.repoToResourcesRoot.entrySet())
        {
            LOGGER.info("  {} -> {}", entry.getKey(), entry.getValue());
        }
    }

    public void scanRoot(Path root)
    {
        if (!Files.isDirectory(root))
        {
            LOGGER.warn("Workspace root is not a directory: {}", root);
            return;
        }

        LOGGER.info("Scanning workspace for Pure repositories: {}", root);
        try
        {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                {
                    String name = dir.getFileName().toString();
                    if (name.startsWith(".") || "target".equals(name) || "node_modules".equals(name))
                    {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                {
                    String fileName = file.getFileName().toString();
                    if (fileName.endsWith(".definition.json"))
                    {
                        processDefinitionFile(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc)
                {
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException e)
        {
            LOGGER.error("Error scanning workspace root: {}", root, e);
        }
    }

    private void processDefinitionFile(Path file)
    {
        Path parent = file.getParent();
        if (parent == null || !parent.toString().contains(RESOURCES_MARKER))
        {
            return;
        }

        String repoName = parseRepoName(file);
        if (repoName != null)
        {
            this.repoToResourcesRoot.put(repoName, parent);
            this.repoToDefinitionFile.put(repoName, file);
            LOGGER.debug("Found repository '{}' at {}", repoName, parent);
        }
    }

    static String parseRepoName(Path file)
    {
        try (Reader reader = Files.newBufferedReader(file))
        {
            JsonObject definition = JsonParser.parseReader(reader).getAsJsonObject();
            JsonElement name = definition.get("name");
            if (name != null && name.isJsonPrimitive())
            {
                return name.getAsString();
            }
        }
        catch (Exception e)
        {
            LOGGER.warn("Failed to parse definition file: {}", file, e);
        }
        return null;
    }

    public Path resolve(String sourceId)
    {
        if (sourceId == null || sourceId.isEmpty())
        {
            return null;
        }

        String path = sourceId.startsWith("/") ? sourceId.substring(1) : sourceId;
        int slashIdx = path.indexOf('/');
        if (slashIdx <= 0)
        {
            return null;
        }

        String repoName = path.substring(0, slashIdx);
        Path resourcesRoot = this.repoToResourcesRoot.get(repoName);
        if (resourcesRoot == null)
        {
            LOGGER.debug("No filesystem mapping for repository '{}' (source: {})", repoName, sourceId);
            return null;
        }

        Path fullPath = resourcesRoot.resolve(path);
        if (Files.exists(fullPath))
        {
            return fullPath;
        }

        LOGGER.debug("File not found on disk: {} (resolved from {})", fullPath, sourceId);
        return null;
    }

    public String resolveToUri(String sourceId)
    {
        Path path = resolve(sourceId);
        return (path != null) ? path.toUri().toString() : null;
    }

    public String deriveSourceIdFromPath(Path filePath)
    {
        Path normalized = filePath.toAbsolutePath().normalize();
        for (Map.Entry<String, Path> entry : this.repoToResourcesRoot.entrySet())
        {
            Path resourcesRoot = entry.getValue().toAbsolutePath().normalize();
            if (normalized.startsWith(resourcesRoot))
            {
                Path relative = resourcesRoot.relativize(normalized);
                return "/" + relative.toString().replace('\\', '/');
            }
        }
        return null;
    }

    public MutableList<RepositoryCodeStorage> buildWorkspaceStorages()
    {
        return buildWorkspaceStorages(Collections.emptySet());
    }

    public MutableList<RepositoryCodeStorage> buildWorkspaceStorages(Collection<String> additionalDependencies)
    {
        MutableList<RepositoryCodeStorage> storages = Lists.mutable.empty();
        for (Map.Entry<String, Path> entry : this.repoToDefinitionFile.entrySet())
        {
            String repoName = entry.getKey();
            Path definitionFile = entry.getValue();
            Path resourcesRoot = this.repoToResourcesRoot.get(repoName);

            try
            {
                CodeRepository repo = withAdditionalDependencies(
                        GenericCodeRepository.build(definitionFile),
                        additionalDependencies);
                Path repoDir = resourcesRoot.resolve(repoName);
                if (!java.nio.file.Files.isDirectory(repoDir))
                {
                    LOGGER.warn("Repo directory not found: {}", repoDir);
                    continue;
                }
                OverlayWorkspaceCodeStorage storage = new OverlayWorkspaceCodeStorage(repo, repoDir);
                storages.add(storage);
                LspLog.info("Workspace repo (overlay FS): " + repoName + " -> " + repoDir);
            }
            catch (Exception e)
            {
                LOGGER.warn("Failed to create overlay workspace storage for '{}': {}", repoName, e.getMessage());
            }
        }
        return storages;
    }

    public MutableList<RepositoryCodeStorage> buildWorkspaceDefinitionStorages(Collection<String> additionalDependencies)
    {
        return buildWorkspaceDefinitionStorages(additionalDependencies, Collections.emptySet());
    }

    public MutableList<RepositoryCodeStorage> buildWorkspaceDefinitionStorages(Collection<String> additionalDependencies,
                                                                               Collection<String> excludedRepositoryNames)
    {
        MutableList<CodeRepository> repositories = buildWorkspaceRepositories(additionalDependencies);
        if (excludedRepositoryNames != null && !excludedRepositoryNames.isEmpty())
        {
            repositories.removeIf(repository -> excludedRepositoryNames.contains(repository.getName()));
        }
        return repositories.isEmpty()
                ? Lists.mutable.empty()
                : Lists.mutable.with(new EmptyCodeStorage(repositories));
    }

    private MutableList<CodeRepository> buildWorkspaceRepositories(Collection<String> additionalDependencies)
    {
        MutableList<CodeRepository> repositories = Lists.mutable.empty();
        for (Path definitionFile : this.repoToDefinitionFile.values())
        {
            try
            {
                repositories.add(withAdditionalDependencies(
                        GenericCodeRepository.build(definitionFile),
                        additionalDependencies));
            }
            catch (Exception e)
            {
                LOGGER.warn("Failed to load repository definition '{}': {}", definitionFile, e.getMessage());
            }
        }
        return repositories;
    }

    private static CodeRepository withAdditionalDependencies(GenericCodeRepository repository, Collection<String> additionalDependencies)
    {
        if (additionalDependencies == null || additionalDependencies.isEmpty())
        {
            return repository;
        }

        MutableSet<String> dependencies = repository.getDependencies().toSet();
        dependencies.addAllIterable(additionalDependencies);
        return GenericCodeRepository.build(
                repository.getName(),
                repository.getAllowedPackagesPattern(),
                dependencies);
    }

    /**
     * Get the set of repo names found in the workspace.
     */
    public java.util.Set<String> getWorkspaceRepoNames()
    {
        return this.repoToResourcesRoot.keySet();
    }

    /**
     * Get the current repository mappings (for diagnostics/logging).
     */
    public Map<String, Path> getMappings()
    {
        return Collections.unmodifiableMap(this.repoToResourcesRoot);
    }

    public void clear()
    {
        this.repoToResourcesRoot.clear();
        this.repoToDefinitionFile.clear();
    }
}
