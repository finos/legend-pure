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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;

public class CutPathAndEncapsulatedObject<T>
{
    private final MutableList<T> encapsulatedObjects = FastList.newList();
    private final String cutPath;

    public CutPathAndEncapsulatedObject(String cutPath)
    {
        this.cutPath = cutPath;
    }

    public CutPathAndEncapsulatedObject(String cutPath, T encapsulatedObject)
    {
        this.encapsulatedObjects.add(encapsulatedObject);
        this.cutPath = cutPath;
    }

    public CutPathAndEncapsulatedObject(String cutPath, MutableList<T> encapsulatedObjects)
    {
        this.encapsulatedObjects.addAll(encapsulatedObjects);
        this.cutPath = cutPath;
    }

    public MutableList<T> getEncapsulatedObjects()
    {
        return this.encapsulatedObjects;
    }

    public String getCutPath()
    {
        return this.cutPath;
    }

    public static <T> void addAndPossiblyCutPath(GenericTreeNode node, String givenPath, T encapsulatedObject, char sep)
    {
        String commonString = "";
        GenericTreeNode mostCommonNode = null;
        for (GenericTreeNode child : node.getChildren())
        {
            if (commonString.isEmpty())
            {
                commonString = mostCommonPart(((CutPathAndEncapsulatedObject) child.getObject()).getCutPath(), givenPath, sep);
                mostCommonNode = child;
            }
        }

        if (commonString.isEmpty())
        {
            node.addChild(node.build(new CutPathAndEncapsulatedObject(givenPath, encapsulatedObject)));
        }
        // Do we have more text to add?
        else if (commonString.length() < givenPath.length())
        {
            String mostCommonNodeText = ((CutPathAndEncapsulatedObject) mostCommonNode.getObject()).getCutPath();
            // Is the common text exactly the node
            if (commonString.length() == mostCommonNodeText.length())
            {
                if (mostCommonNode.isLeaf())
                {
                    mostCommonNode.setObject(new CutPathAndEncapsulatedObject(givenPath, encapsulatedObject));
                }
                else
                {
                    addAndPossiblyCutPath(mostCommonNode, givenPath.substring(commonString.length()), encapsulatedObject, sep);
                }
            }
            else if (commonString.length() < mostCommonNodeText.length())
            {
                String splitMostCommonNodeText = ((CutPathAndEncapsulatedObject) mostCommonNode.getObject()).getCutPath().substring(commonString.length());
                MutableList<Object> mostCommonNodeEncapsulatedObjects = ((CutPathAndEncapsulatedObject) mostCommonNode.getObject()).getEncapsulatedObjects();
                mostCommonNode.setObject(new CutPathAndEncapsulatedObject(commonString));
                GenericTreeNode replacementNode = node.build(new CutPathAndEncapsulatedObject(splitMostCommonNodeText, mostCommonNodeEncapsulatedObjects));
                mostCommonNode.moveChildren(replacementNode);
                mostCommonNode.addChild(replacementNode);
                mostCommonNode.addChild(node.build(new CutPathAndEncapsulatedObject(givenPath.substring(commonString.length()), encapsulatedObject)));
            }
        }
        else
        {
            ((CutPathAndEncapsulatedObject) mostCommonNode.getObject()).encapsulatedObjects.add(0, encapsulatedObject);
        }
    }

    private static String mostCommonPart(String a, String b, char sep)
    {
        if (a.equals(b))
        {
            return a;
        }
        if (a.isEmpty() || b.isEmpty())
        {
            return "";
        }
        StringBuffer temp = new StringBuffer();
        StringBuilder result = new StringBuilder();
        int i = 0;
        boolean equal = true;
        while (i < a.length() && i < b.length() && equal)
        {
            equal = a.charAt(i) == b.charAt(i);
            if (equal)
            {
                temp.append(a.charAt(i));
                if (a.charAt(i) == sep)
                {
                    result.append(temp);
                    temp = new StringBuffer();
                }
            }
            i++;
        }
        return result.toString();
    }
}
