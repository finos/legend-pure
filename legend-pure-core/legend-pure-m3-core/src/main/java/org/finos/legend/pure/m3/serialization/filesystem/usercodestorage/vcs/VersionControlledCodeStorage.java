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

package org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.primitive.LongList;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNode;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;

import java.util.List;
import java.util.Optional;

public interface VersionControlledCodeStorage extends RepositoryCodeStorage
{
    /**
     * Return whether the path is under version control.
     *
     * @param path Pure path
     * @return whether path is under version control
     */
    boolean isVersioned(String path);

    /**
     * Return the current revision number of the given path. Returns
     * -1 if the path is not under version control or otherwise has
     * no revisions.
     *
     * @param path Pure path
     * @return current revision number or -1
     */
    Optional<String> getCurrentRevision(String path);

    /**
     * Get all revision numbers at which changes were made to the
     * given path. Returns an empty list if the path is not under
     * version control or otherwise has no revisions.
     *
     * @param path Pure path
     * @return all revision numbers for path
     */
    List<String> getAllRevisions(String path);

    RichIterable<Revision> getAllRevisionLogs(RichIterable<String> path);

    String getDiff(RichIterable<String> paths);

    RichIterable<CodeStorageNode> getModifiedUserFiles();

    RichIterable<CodeStorageNode> getUnversionedFiles();
}
