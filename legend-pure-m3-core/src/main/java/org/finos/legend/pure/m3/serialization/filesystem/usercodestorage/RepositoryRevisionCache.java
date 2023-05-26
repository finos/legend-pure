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
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.Revision;

public interface RepositoryRevisionCache
{
    LongList getAllRevisions(String path);

    RichIterable<Revision> getRevisionsByPath(RichIterable<String> paths);
}
