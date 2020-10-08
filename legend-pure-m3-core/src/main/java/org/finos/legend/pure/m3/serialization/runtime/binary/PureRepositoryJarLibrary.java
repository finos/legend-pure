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
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.SetIterable;

public interface PureRepositoryJarLibrary
{
    String getPlatformVersion();
    String getModelVersion();

    boolean isKnownRepository(String repositoryName);
    boolean isKnownFile(String filePath);
    boolean isKnownInstance(String instancePath);

    byte[] readFile(String filePath);
    MapIterable<String, byte[]> readFiles(String... filePaths);
    MapIterable<String, byte[]> readFiles(Iterable<String> filePaths);

    MapIterable<String, byte[]> readRepositoryFiles(String repositoryName);
    MapIterable<String, byte[]> readRepositoryFiles(String... repositoryNames);
    MapIterable<String, byte[]> readRepositoryFiles(Iterable<String> repositoryNames);

    MapIterable<String, byte[]> readAllFiles();
    RichIterable<String> getAllFiles();
    RichIterable<String> getRepositoryFiles(String repositoryName);
    RichIterable<String> getDirectoryFiles(String directory);

    SetIterable<String> getRequiredFiles(String instancePath);
    SetIterable<String> getRequiredFiles(String... instancePaths);
    SetIterable<String> getRequiredFiles(Iterable<String> instancePaths);

    /**
     * Get the files that the given files depend on.
     *
     * @param filePaths file paths
     * @return files that the given files depend on
     */
    SetIterable<String> getFileDependencies(String... filePaths);

    /**
     * Get the files that the given files depend on.
     *
     * @param filePaths file paths
     * @return files that the given files depend on
     */
    SetIterable<String> getFileDependencies(Iterable<String> filePaths);

    /**
     * Get the files that depend on any of the given files.
     *
     * @param filePaths file paths
     * @return files that depend on any of the given files
     */
    SetIterable<String> getDependentFiles(String... filePaths);

    /**
     * Get the files that depend on any of the given files.
     *
     * @param filePaths file paths
     * @return files that depend on any of the given files
     */
    SetIterable<String> getDependentFiles(Iterable<String> filePaths);
}
