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

package org.finos.legend.pure.m3.tools.tree;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class GenericTreeNode implements TreeNode<GenericTreeNode>, Comparable<GenericTreeNode>
{
    public static final Function<GenericTreeNode, Object> TO_OBJECT = genericTreeNode -> genericTreeNode.object;

    protected Object object;
    private GenericTreeNode parent;
    private final MutableList<GenericTreeNode> children = Lists.mutable.empty();

    protected GenericTreeNode(Object object)
    {
        this.object = object;
    }

    public abstract GenericTreeNode build(Object object);

    public GenericTreeNode findOrCreateDirectChild(GenericTreeNode obj)
    {
        int index = 0;
        if ((index = this.indexOf(obj)) == -1)
        {
            this.addChild(obj);
            return obj;
        }
        return this.getChildAt(index);
    }

    public Object getObject()
    {
        return this.object;
    }

    public void setObject(Object object)
    {
        this.object = object;
    }

    public void addChild(GenericTreeNode child)
    {
        this.children.add(child);
        child.parent = this;
    }

    @Override
    public MutableList<GenericTreeNode> getChildren()
    {
        return this.children;
    }

    @Override
    public GenericTreeNode getChildAt(int index)
    {
        return this.children.get(index);
    }

    @Override
    public boolean isLeaf()
    {
        return this.children.isEmpty();
    }

    @Override
    public int indexOf(GenericTreeNode node)
    {
        return this.children.indexOf(node);
    }

    public void sort()
    {
        Collections.sort(this.children);
        for (GenericTreeNode child : this.children)
        {
            child.sort();
        }
    }

    protected void findNodeToBreak(List<GenericTreeNode> nodesToReprocess, char separator)
    {
        if (this.object instanceof CutPathAndEncapsulatedObject && (this.isLeaf() || ((CutPathAndEncapsulatedObject) this.object).getEncapsulatedObjects().size() > 1))
        {
            String oldPath = ((CutPathAndEncapsulatedObject) this.object).getCutPath();
            if (oldPath.indexOf(separator) != -1 && oldPath.indexOf(separator) != oldPath.length() - 1)
            {
                nodesToReprocess.add(this);
            }
        }
        for (GenericTreeNode child : this.children)
        {
            child.findNodeToBreak(nodesToReprocess, separator);
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || this.getClass() != o.getClass())
        {
            return false;
        }

        GenericTreeNode that = (GenericTreeNode) o;

        if (!Objects.equals(this.object, that.object))
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return this.object == null ? 0 : this.object.hashCode();
    }

    @Override
    public int compareTo(GenericTreeNode o)
    {
        return ((this.isLeaf() ? "~" : "") + this.toString()).compareTo((o.isLeaf() ? "~" : "") + o.toString());
    }

    public void moveChildren(GenericTreeNode replacementNode)
    {
        replacementNode.children.addAll(this.children);
        this.children.clear();
        for (GenericTreeNode child : replacementNode.children)
        {
            child.parent = replacementNode;
        }
    }

    public void addChildren(MutableList<GenericTreeNode> children)
    {
        for (GenericTreeNode node : children)
        {
            node.parent = this;
        }
        this.children.addAll(children);
    }
}
