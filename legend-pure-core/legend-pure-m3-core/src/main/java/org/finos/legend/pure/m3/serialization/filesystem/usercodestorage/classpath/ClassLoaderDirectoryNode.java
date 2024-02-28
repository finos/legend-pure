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

package org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNode;

class ClassLoaderDirectoryNode extends ClassLoaderCodeStorageNode
{
    private ImmutableList<CodeStorageNode> childNodes;
    private ImmutableList<ClassLoaderCodeStorageNode> descendantNodes;

    protected ClassLoaderDirectoryNode(String path)
    {
        super(path);
    }

    @Override
    public boolean isDirectory()
    {
        return true;
    }

    void initializeChildren(RichIterable<ClassLoaderCodeStorageNode> allNodes)
    {
        if (this.childNodes == null)
        {
            final String prefix = getPath() + "/";
            final int prefixLength = prefix.length();
            this.childNodes = Lists.immutable.<CodeStorageNode>withAll(allNodes.select(new Predicate<ClassLoaderCodeStorageNode>()
            {
                @Override
                public boolean accept(ClassLoaderCodeStorageNode node)
                {
                    String path = node.getPath();
                    return path.startsWith(prefix) && (path.length() > prefixLength) && (path.indexOf('/', prefixLength) == -1);
                }
            }));
        }
    }

    RichIterable<CodeStorageNode> getChildren()
    {
        return this.childNodes;
    }

    void initializeDescendents(RichIterable<ClassLoaderCodeStorageNode> allNodes)
    {
        if (this.descendantNodes == null)
        {
            final String prefix = getPath() + "/";
            final int prefixLength = prefix.length();
            this.descendantNodes = Lists.immutable.withAll(allNodes.select(new Predicate<ClassLoaderCodeStorageNode>()
            {
                @Override
                public boolean accept(ClassLoaderCodeStorageNode node)
                {
                    String path = node.getPath();
                    return path.startsWith(prefix) && (path.length() > prefixLength);
                }
            }));
        }
    }

    RichIterable<ClassLoaderCodeStorageNode> getDescendants()
    {
        return this.descendantNodes;
    }
}
