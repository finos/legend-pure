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
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.runtime.Message;

import java.io.InputStream;

public interface RepositoryCodeStorage
{
    String PATH_SEPARATOR = "/";
    String ROOT_PATH = PATH_SEPARATOR;
    String PURE_FILE_EXTENSION = ".pure";

    void initialize(Message message);

    RichIterable<CodeRepository> getAllRepositories();

    default RepositoryCodeStorage getOriginalCodeStorage(CodeRepository codeRepository)
    {
        if (getRepository(codeRepository.getName()) == null)
        {
            throw new RuntimeException("The code storage " + this.getClass() + " doesn't contain the repository '" + codeRepository.getName() + "'");
        }
        return this;
    }

    CodeRepository getRepository(String name);

    CodeRepository getRepositoryForPath(String path);

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
}
