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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;

public interface CodeStorageNode
{
    Function<CodeStorageNode, String> GET_NAME = new Function<CodeStorageNode, String>()
    {
        @Override
        public String valueOf(CodeStorageNode node)
        {
            return node.getName();
        }
    };

    Function<CodeStorageNode, String> GET_PATH = new Function<CodeStorageNode, String>()
    {
        @Override
        public String valueOf(CodeStorageNode node)
        {
            return node.getPath();
        }
    };

    Predicate<CodeStorageNode> IS_DIRECTORY = new Predicate<CodeStorageNode>()
    {
        @Override
        public boolean accept(CodeStorageNode node)
        {
            return node.isDirectory();
        }
    };

    Predicate2<CodeStorageNode, CodeStorageNodeStatus> HAS_STATUS = new Predicate2<CodeStorageNode, CodeStorageNodeStatus>()
    {
        @Override
        public boolean accept(CodeStorageNode node, CodeStorageNodeStatus status)
        {
            return node.getStatus() == status;
        }
    };

    Predicate<CodeStorageNode> IS_REJECT_FILE = new Predicate<CodeStorageNode>()
    {
        @Override
        public boolean accept(CodeStorageNode node)
        {
            return !node.isDirectory() && node.getName().endsWith(".rej");
        }
    };

    boolean isDirectory();

    String getName();

    String getPath();

    CodeStorageNodeStatus getStatus();
}
