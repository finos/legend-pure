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
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;

import java.io.File;
import java.io.InputStream;

public interface MutableVersionControlledCodeStorage extends VersionControlledCodeStorage, MutableRepositoryCodeStorage
{
    void update(UpdateReport report, long version);

    void update(UpdateReport report, String path, long version);

    RichIterable<String> revert(String path);

    void commit(ListIterable<String> paths, String message);

    InputStream getBase(String path);

    InputStream getConflictOld(String path);

    InputStream getConflictNew(String path);

    void markAsResolved(String path);

    void cleanup();

    void applyPatch(String path, File patchFile);

    boolean hasConflicts(String path);
}
