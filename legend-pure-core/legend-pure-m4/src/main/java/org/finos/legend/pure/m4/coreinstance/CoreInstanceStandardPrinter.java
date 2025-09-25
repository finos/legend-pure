// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m4.coreinstance;

import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.tools.SafeAppendable;

public class CoreInstanceStandardPrinter
{
    public static <T extends Appendable> T printFull(T appendable, CoreInstance instance, String tab)
    {
        return print(appendable, instance, tab, true, true, CoreInstance.DEFAULT_MAX_PRINT_DEPTH);
    }

    public static <T extends Appendable> T print(T appendable, CoreInstance instance, String tab, int max)
    {
        return print(appendable, instance, tab, false, true, max);
    }

    public static <T extends Appendable> T printWithoutDebug(T appendable, CoreInstance instance, String tab, int max)
    {
        return print(appendable, instance, tab, false, false, max);
    }

    public static <T extends Appendable> T print(T appendable, CoreInstance instance, String tab, boolean full, boolean addDebug, int max)
    {
        print(SafeAppendable.wrap(appendable), instance, tab, Stacks.mutable.empty(), full, addDebug, max);
        return appendable;
    }

    private static void print(SafeAppendable appendable, CoreInstance instance, String tab, MutableStack<CoreInstance> stack, boolean full, boolean addDebug, int max)
    {
        stack.push(instance);
        printNodeName(appendable.append(tab), instance, full, addDebug);
        printNodeName(appendable.append(" instance "), instance.getClassifier(), full, addDebug);
        for (String key : instance.getKeys().toSortedList())
        {
            printProperty(appendable.append('\n'), instance, key, instance.getValueForMetaPropertyToMany(key), tab, stack, full, addDebug, max);
        }
        stack.pop();
    }

    private static void printNodeName(SafeAppendable appendable, CoreInstance node, boolean full, boolean addDebug)
    {
        String name = node.getName();
        if (full)
        {
            appendable.append(name).append('_').append(node.getSyntheticId());
        }
        else
        {
            appendable.append(ModelRepository.possiblyReplaceAnonymousId(name));
        }

        if (addDebug)
        {
            SourceInformation sourceInfo = node.getSourceInformation();
            if (sourceInfo != null)
            {
                appendable.append('(').append(sourceInfo.getSourceId()).append(':')
                        .append(sourceInfo.getStartLine()).append(',')
                        .append(sourceInfo.getStartColumn()).append(',')
                        .append(sourceInfo.getLine()).append(',')
                        .append(sourceInfo.getColumn()).append(',')
                        .append(sourceInfo.getEndLine()).append(',')
                        .append(sourceInfo.getEndColumn()).append(')');
            }
        }
    }

    private static void printProperty(SafeAppendable appendable, CoreInstance instance, String propertyName, ListIterable<? extends CoreInstance> values, String tab, MutableStack<CoreInstance> stack, boolean full, boolean addDebug, int max)
    {
        appendable.append(tab).append("    ");
        if (propertyName == null)
        {
            appendable.append("null:");
        }
        else
        {
            CoreInstance property = instance.getKeyByName(propertyName);
            CoreInstance propertyClassifier = property.getClassifier();

            printNodeName(appendable, property, full, addDebug);
            appendable.append('(');
            if (propertyClassifier == null)
            {
                appendable.append("null");
            }
            else
            {
                printNodeName(appendable, propertyClassifier, full, addDebug);
            }
            appendable.append("):");
        }

        SetIterable<CoreInstance> excluded = instance.getRepository().getExclusionSet();
        for (CoreInstance value : values)
        {
            appendable.append('\n');

            if (value == null)
            {
                appendable.append(tab).append("        NULL");
            }
            else
            {
                // TODO remove reference to "package" property, which is an M3 thing
                boolean excludeFlag = ((excluded != null) && (excluded.contains(value.getValueForMetaPropertyToOne("package")) || excluded.contains(value))) ||
                        (instance.getRepository().getTopLevel(value.getName()) != null);
                if (full ? stack.contains(value) : (excludeFlag || (stack.size() > max) || stack.contains(value)))
                {
                    CoreInstance valueClassifier = value.getClassifier();

                    appendable.append(tab).append("        ");
                    printNodeName(appendable, value, full, addDebug);
                    appendable.append(" instance ");
                    if (valueClassifier == null)
                    {
                        appendable.append("null");
                    }
                    else
                    {
                        printNodeName(appendable, valueClassifier, full, addDebug);
                        if (full)
                        {
                            appendable.append('\n').append(tab).append("            [...]");
                        }
                        else if (!excludeFlag && (stack.size() > max) && value.getKeys().notEmpty())
                        {
                            appendable.append('\n').append(tab).append("            [... >").append(max).append(']');
                        }
                    }
                }
                else
                {
                    String newTab = tab + "        ";
                    if (value instanceof CoreInstanceWithStandardPrinting)
                    {
                        print(appendable, value, newTab, stack, full, addDebug, max);
                    }
                    else if (full)
                    {
                        value.printFull(appendable, newTab);
                    }
                    else if (addDebug)
                    {
                        value.print(appendable, newTab, max - stack.size());
                    }
                    else
                    {
                        value.printWithoutDebug(appendable, newTab, max - stack.size());
                    }
                }
            }
        }
    }
}
