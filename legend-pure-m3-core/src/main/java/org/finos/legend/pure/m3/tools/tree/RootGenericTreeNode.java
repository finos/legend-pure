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

import java.util.ArrayList;
import java.util.List;

public class RootGenericTreeNode extends GenericTreeNode
{
    private final String name;

    public RootGenericTreeNode()
    {
        this("Root");
    }

    public RootGenericTreeNode(String name)
    {
        super(null);
        this.name = name;
    }

    @Override
    public String toString()
    {
        return this.name;
    }

    public void breakLastPath(char separator)
    {
        List<GenericTreeNode> nodes = new ArrayList<GenericTreeNode>();
        this.findNodeToBreak(nodes, separator);
        for (GenericTreeNode node : nodes)
        {
            CutPathAndEncapsulatedObject cutPathAndEncapsulatedObject = (CutPathAndEncapsulatedObject) node.getObject();
            int cut = cutPathAndEncapsulatedObject.getCutPath().substring(0, cutPathAndEncapsulatedObject.getCutPath().length() - 1).lastIndexOf(separator);
            Object object = cutPathAndEncapsulatedObject.getEncapsulatedObjects().remove(cutPathAndEncapsulatedObject.getEncapsulatedObjects().size() - 1);
            node.setObject(new CutPathAndEncapsulatedObject(cutPathAndEncapsulatedObject.getCutPath().substring(0, cut + 1), cutPathAndEncapsulatedObject.getEncapsulatedObjects()));
            GenericTreeNode child = node.build(new CutPathAndEncapsulatedObject(cutPathAndEncapsulatedObject.getCutPath().substring(cut + 1), object));
            child.addChildren(node.getChildren());
            node.getChildren().clear();
            node.addChild(child);
        }
    }

    @Override
    public GenericTreeNode build(Object object)
    {
        throw new RuntimeException("Not Supported");
    }
}
