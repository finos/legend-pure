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

package org.finos.legend.pure.m3.serialization.runtime.binary;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.set.ImmutableSetMultimap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.Version;

import java.util.Objects;

public abstract class AbstractPureRepositoryJarLibrary implements PureRepositoryJarLibrary
{
    private final String pureModelVersion;
    private final Index index;

    protected AbstractPureRepositoryJarLibrary(Iterable<? extends PureRepositoryJar> jars)
    {
        this.pureModelVersion = validateRepositoryJarsAndGetModelVersion(jars);
        this.index = buildIndex(jars);
    }

    @Override
    public String getPlatformVersion()
    {
        return Version.PLATFORM;
    }

    @Override
    public String getModelVersion()
    {
        return this.pureModelVersion;
    }

    @Override
    public boolean isKnownFile(String filePath)
    {
        return this.index.isKnownFile(filePath);
    }

    @Override
    public boolean isKnownInstance(String instancePath)
    {
        return this.index.isKnownInstance(instancePath);
    }

    @Override
    public MapIterable<String, byte[]> readFiles(String... filePaths)
    {
        return readFiles(ArrayAdapter.adapt(filePaths));
    }

    @Override
    public MapIterable<String, byte[]> readRepositoryFiles(String... repositoryNames)
    {
        return readRepositoryFiles(ArrayAdapter.adapt(repositoryNames));
    }

    @Override
    public RichIterable<String> getAllFiles()
    {
        return this.index.getAllFiles();
    }

    @Override
    public RichIterable<String> getDirectoryFiles(String directory)
    {
        if (directory == null)
        {
            throw new IllegalArgumentException("directory may not be null");
        }

        if (directory.isEmpty() || "/".equals(directory))
        {
            // root directory
            return getAllFiles();
        }

        boolean startsWithSlash = directory.charAt(0) == '/';
        int repoNameStart = startsWithSlash ? 1 : 0;
        int repoNameEnd = directory.indexOf('/', repoNameStart);
        String repositoryName = (repoNameEnd == -1) ? directory.substring(repoNameStart) : directory.substring(repoNameStart, repoNameEnd);
        if (!isKnownRepository(repositoryName))
        {
            // an unknown repo is treated as an empty directory
            return Lists.immutable.empty();
        }

        String prefix = startsWithSlash ? directory.substring(1) : directory;
        if (prefix.charAt(prefix.length() - 1) != '/')
        {
            prefix += '/';
        }
        return getRepositoryFiles(repositoryName).selectWith(String::startsWith, prefix);
    }

    @Override
    public SetIterable<String> getRequiredFiles(String instancePath)
    {
        String filePath = this.index.getInstanceDefinitionFile(instancePath);
        if (filePath == null)
        {
            throw new IllegalArgumentException("Cannot find file for instance: " + instancePath);
        }
        return getFileDependencies(Stacks.mutable.with(filePath));
    }

    @Override
    public SetIterable<String> getRequiredFiles(String... instancePaths)
    {
        return getRequiredFiles(ArrayAdapter.adapt(instancePaths));
    }

    @Override
    public SetIterable<String> getRequiredFiles(Iterable<String> instancePaths)
    {
        MutableStack<String> filePaths = Stacks.mutable.empty();
        for (String instancePath : instancePaths)
        {
            String filePath = this.index.getInstanceDefinitionFile(instancePath);
            if (filePath == null)
            {
                throw new IllegalArgumentException("Cannot find file for instance: " + instancePath);
            }
            filePaths.push(filePath);
        }
        return getFileDependencies(filePaths);
    }

    @Override
    public SetIterable<String> getFileDependencies(String... filePaths)
    {
        return getFileDependencies(Stacks.mutable.with(filePaths));
    }

    @Override
    public SetIterable<String> getFileDependencies(Iterable<String> filePaths)
    {
        return getFileDependencies(Stacks.mutable.withAll(filePaths));
    }

    /**
     * Get the files that the given files depend on.
     *
     * @param filePaths file paths
     * @return files that the given files depend on
     */
    private SetIterable<String> getFileDependencies(MutableStack<String> filePaths)
    {
        MutableSet<String> results = Sets.mutable.withInitialCapacity(Math.max(filePaths.size(), 16));
        while (filePaths.notEmpty())
        {
            String filePath = filePaths.pop();
            if (results.add(filePath))
            {
                ImmutableSet<String> externalReferences = this.index.getExternalReferencesInFile(filePath);
                if (externalReferences == null)
                {
                    throw new RuntimeException("Could not find external references for: " + filePath);
                }
                for (String externalReference : externalReferences)
                {
                    String definitionFilePath = this.index.getInstanceDefinitionFile(externalReference);
                    if (definitionFilePath == null)
                    {
                        throw new RuntimeException("Cannot find definition for: " + externalReference + " (referenced from " + filePath + ")");
                    }
                    filePaths.push(definitionFilePath);
                }
            }
        }
        return results.asUnmodifiable();
    }

    @Override
    public SetIterable<String> getDependentFiles(String... filePaths)
    {
        return getDependentFiles(Stacks.mutable.with(filePaths));
    }

    @Override
    public SetIterable<String> getDependentFiles(Iterable<String> filePaths)
    {
        return getDependentFiles(Stacks.mutable.withAll(filePaths));
    }

    /**
     * Get the files that depend on any of the given files.
     *
     * @param filePaths file paths
     * @return files that depend on any of the given files
     */
    private SetIterable<String> getDependentFiles(MutableStack<String> filePaths)
    {
        MutableSet<String> results = Sets.mutable.withInitialCapacity(filePaths.size());
        while (filePaths.notEmpty())
        {
            String filePath = filePaths.pop();
            if (results.add(filePath))
            {
                ImmutableSet<String> definedInstances = this.index.getInstancesDefinedInFile(filePath);
                for (String definedInstance : definedInstances)
                {
                    ImmutableSet<String> referencingFiles = this.index.getFilesReferencingInstance(definedInstance);
                    for (String referencingFile : referencingFiles)
                    {
                        filePaths.push(referencingFile);
                    }
                }
            }
        }
        return results.asUnmodifiable();
    }

    protected static String getFileRepository(String filePath)
    {
        int index = filePath.indexOf('/');
        return (index == -1) ? "root" : filePath.substring(0, index);
    }

    private static String validateRepositoryJarsAndGetModelVersion(Iterable<? extends PureRepositoryJar> jars)
    {
        MutableSet<String> repositoryNames = Sets.mutable.empty();
        MutableSet<String> platformVersions = Sets.mutable.empty();
        MutableSet<String> modelVersions = Sets.mutable.empty();

        // Validate repositories
        for (PureRepositoryJar jar : jars)
        {
            PureRepositoryJarMetadata metadata = jar.getMetadata();
            String repositoryName = metadata.getRepositoryName();
            if (!repositoryNames.add(repositoryName))
            {
                throw new IllegalArgumentException("Multiple Pure repository jars for " + repositoryName);
            }
            String platformVersion = metadata.getPurePlatformVersion();
            if (platformVersion != null)
            {
                platformVersions.add(platformVersion);
            }
            String modelVersion = metadata.getPureModelVersion();
            if (modelVersion != null)
            {
                modelVersions.add(modelVersion);
            }
        }

        // Validate platform versions
        switch (platformVersions.size())
        {
            case 0:
            {
                if (Version.PLATFORM != null)
                {
                    throw new IllegalArgumentException("Pure platform version mismatch: cannot load a jar for with unknown platform version into a system at version " + Version.PLATFORM);
                }
                break;
            }
            case 1:
            {
                String platformVersion = platformVersions.getAny();
                if (!Objects.equals(Version.PLATFORM, platformVersion))
                {
                    throw new IllegalArgumentException("Pure platform version mismatch: cannot load a jar for " + platformVersion + " into a system at version " + Version.PLATFORM);
                }
                break;
            }
            default:
            {
                //Iterable<? extends PureRepositoryJar> jars
                String all = Lists.mutable.ofAll(jars).collect(jar -> jar.getMetadata().getRepositoryName()+" "+jar.getMetadata().getPurePlatformVersion()).makeString("\n");
                throw new IllegalArgumentException(platformVersions.toSortedList().makeString("Platform version mismatch: ", ", ", "")+"\nAll repositories:\n"+all);
            }
        }

        // Validate model versions
        if (modelVersions.size() > 1)
        {
            throw new IllegalArgumentException(modelVersions.toSortedList().makeString("Model version mismatch: ", ", ", ""));
        }
        return modelVersions.getAny();
    }

    private static Index buildIndex(Iterable<? extends PureRepositoryJar> jars)
    {
        MutableMap<String, String> instanceDefinitions = Maps.mutable.empty();
        MutableMap<String, ImmutableSet<String>> externalReferencesByFile = Maps.mutable.empty();
        MutableSetMultimap<String, String> filesByExternalReference = Multimaps.mutable.set.empty();
        jars.forEach(jar ->
        {
            PureRepositoryJarMetadata metadata = jar.getMetadata();
            metadata.getDefinitionIndex().forEachKeyValue((instancePath, filePath) ->
            {
                String old = instanceDefinitions.put(instancePath, filePath);
                if (old != null)
                {
                    throw new IllegalArgumentException("Multiple definition files for " + instancePath + ": " + old + " and " + filePath);
                }
            });
            metadata.getExternalReferenceIndex().forEachKeyValue((filePath, instancePaths) ->
            {
                ImmutableSet<String> old = externalReferencesByFile.put(filePath, instancePaths);
                if (old != null)
                {
                    throw new IllegalArgumentException("Multiple external reference indexes for " + filePath);
                }
                instancePaths.forEach(instancePath -> filesByExternalReference.put(instancePath, filePath));
            });
        });

        return new Index(instanceDefinitions.toImmutable(), externalReferencesByFile.toImmutable(), filesByExternalReference.toImmutable());
    }

    private static class Index
    {
        private final ImmutableMap<String, String> instanceDefinitions;
        private final ImmutableSetMultimap<String, String> instanceDefinitionsByFile;

        private final ImmutableMap<String, ImmutableSet<String>> externalReferencesByFile;
        private final ImmutableSetMultimap<String, String> filesByExternalReference;

        private Index(ImmutableMap<String, String> instanceDefinitions, ImmutableMap<String, ImmutableSet<String>> externalReferencesByFile, ImmutableSetMultimap<String, String> filesByExternalReference)
        {
            this.instanceDefinitions = instanceDefinitions;
            this.instanceDefinitionsByFile = instanceDefinitions.flip();

            this.externalReferencesByFile = externalReferencesByFile;
            this.filesByExternalReference = filesByExternalReference;
        }

        /**
         * Return whether the given path identifies a known instance.
         *
         * @param instancePath instance path
         * @return whether instancePath denotes a known instance.
         */
        private boolean isKnownInstance(String instancePath)
        {
            return this.instanceDefinitions.containsKey(instancePath);
        }

        /**
         * Return whether the given path identifies a known file.
         *
         * @param filePath file path
         * @return whether filePath denotes a known file
         */
        private boolean isKnownFile(String filePath)
        {
            return this.externalReferencesByFile.containsKey(filePath);
        }

        /**
         * Get an iterable of all files in the jar library.
         *
         * @return all files
         */
        private RichIterable<String> getAllFiles()
        {
            return this.externalReferencesByFile.keysView();
        }

        /**
         * Get the file an instance is defined in.
         *
         * @param instancePath instance path
         * @return file the instance is defined in
         */
        private String getInstanceDefinitionFile(String instancePath)
        {
            return this.instanceDefinitions.get(instancePath);
        }

        /**
         * Get the set of instances defined in the given file.
         *
         * @param filePath file path
         * @return instances defined in file
         */
        private ImmutableSet<String> getInstancesDefinedInFile(String filePath)
        {
            return this.instanceDefinitionsByFile.get(filePath);
        }

        /**
         * Get the set of external references in the given file.
         *
         * @param filePath file path
         * @return external references for file
         */
        private ImmutableSet<String> getExternalReferencesInFile(String filePath)
        {
            return this.externalReferencesByFile.get(filePath);
        }

        /**
         * Get the files for which the given instance is an
         * external reference. These are files where the instance
         * is referenced but not defined.
         *
         * @param instancePath instance path
         * @return files for which the instance is an external reference
         */
        private ImmutableSet<String> getFilesReferencingInstance(String instancePath)
        {
            return this.filesByExternalReference.get(instancePath);
        }
    }
}
