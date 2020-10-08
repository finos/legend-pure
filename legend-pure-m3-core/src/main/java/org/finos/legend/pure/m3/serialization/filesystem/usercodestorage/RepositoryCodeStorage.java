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
import org.eclipse.collections.api.block.function.Function;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.runtime.Message;

import java.io.InputStream;

public interface RepositoryCodeStorage
{
    Function<RepositoryCodeStorage, RichIterable<CodeRepository>> GET_REPOSITORIES = new Function<RepositoryCodeStorage, RichIterable<CodeRepository>>()
    {
        @Override
        public RichIterable<CodeRepository> valueOf(RepositoryCodeStorage codeStorage)
        {
            return codeStorage.getRepositories();
        }
    };

    void initialize(Message message);

    RichIterable<CodeRepository> getRepositories();

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
