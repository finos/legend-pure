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

public class TreePrinter
{
    public static String printTree(TreeNode<? extends TreeNode> tree, String space)
    {
        String result = "";
        result = result + space + "##---------------------------- TreeStart\n";
        result = printNode(tree, space + "+ ", result);
        result = result + space + "##---------------------------- TreeEnd";
        return result;
    }

    private static String printNode(TreeNode<? extends TreeNode> node, String space, String result)
    {
        if (node != null)
        {
            String nodeContent = node.toString().replaceAll("\\n", "\n" + space);
            result = result + space + nodeContent + "\n";
            for (TreeNode child : node.getChildren())
            {
                result = printNode(child, space + "    ", result);
            }
            return result;
        }
        else
        {
            result = result + space + "null\n";
            return result;
        }
    }
}
