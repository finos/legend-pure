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
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.UpdateReport;
import org.finos.legend.pure.m3.serialization.runtime.Message;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public interface MutableCodeStorage extends CodeStorage
{
    void initialize(Message message);

    boolean isRepositoryImmutable(CodeRepository repository);

    RichIterable<CodeStorageNode> getModifiedUserFiles();

    RichIterable<CodeStorageNode> getUnversionedFiles();

    OutputStream writeContent(String path);

    void writeContent(String path, String content);

    InputStream getBase(String path);

    void createFile(String filePath);

    void createFolder(String folderPath);

    void deleteFile(String filePath);

    void moveFile(String sourcePath, String destinationPath);

    void markAsResolved(String path);

    /**
     * Update all paths under version control.
     *
     * @return update report
     */
    UpdateReport update(long version);

    /**
     * Update the given path.
     *
     * @param path path to update
     * @param version
     * @return update report
     */
    UpdateReport update(String path, long version);

    RichIterable<String> revert(String path);

    InputStream getConflictOld(String path);

    InputStream getConflictNew(String path);

    void commit(ListIterable<String> paths, String message);

    /**
     * Perform any relevant version control cleanup.
     */
    void cleanup();

    void applyPatch(String path, File patchFile);

    boolean hasConflicts(String path);
}
