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

package org.finos.legend.pure.m3.serialization.filesystem.usercodestorage;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.primitive.LongList;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.Revision;

import java.io.InputStream;

public interface CodeStorage
{
    String PATH_SEPARATOR = "/";
    String ROOT_PATH = PATH_SEPARATOR;
    String PURE_FILE_EXTENSION = ".pure";

    /**
     * Get the name of the repository that the
     * path is in.
     *
     * @param path file or directory path
     * @return path repository
     */
    String getRepoName(String path);

    RichIterable<String> getAllRepoNames();

    boolean isRepoName(String name);

    RichIterable<CodeRepository> getAllRepositories();

    CodeRepository getRepository(String name);

    default CodeRepository getRepositoryForPath(String path)
    {
        throw new UnsupportedOperationException("Not Supported");
    }

    CodeStorageNode getNode(String path);

    RichIterable<CodeStorageNode> getFiles(String path);

    RichIterable<String> getUserFiles();

    RichIterable<String> getFileOrFiles(String path);

    InputStream getContent(String path);

    byte[] getContentAsBytes(String path);

    String getContentAsText(String path);

    boolean exists(String path);

    boolean isFile(String path);

    boolean isFolder(String path);

    boolean isEmptyFolder(String path);

    /**
     * Return whether the given file or directory is
     * under version control.
     *
     * @param path file or directory path
     * @return whether path is under version control
     */
    boolean isVersioned(String path);

    /**
     * Get the current revision number of the given file
     * or directory.  Returns -1 if the file or directory
     * is not under version control.
     *
     * @param path file or directory path
     * @return current revision number for path
     */
    long getCurrentRevision(String path);

    /**
     * Get the list of all revision numbers for the given
     * file or directory.  These are the revision numbers
     * at which some change was made to the file or
     * directory.  Returns an empty list if the file or
     * directory is not under version control.
     *
     * @param path file or directory path
     * @return all revision numbers for path
     */
    LongList getAllRevisions(String path);

    /**
     * Get the list of all revision logs for the given
     * files and directories.  These are the revision logs
     * at which some change was made to any of the files or
     * directories.  Returns an empty list if none of the
     * files or directorues is under version control.
     *
     * @param paths file and directory paths
     * @return all revision logs for paths
     */
    RichIterable<Revision> getAllRevisionLogs(RichIterable<String> paths);
}
