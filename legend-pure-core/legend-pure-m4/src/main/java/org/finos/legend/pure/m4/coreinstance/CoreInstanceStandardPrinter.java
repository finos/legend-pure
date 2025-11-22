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
    private final SafeAppendable appendable;
    private final boolean full;
    private final boolean addDebug;
    private final boolean printEmptyProperties;
    private final int max;
    private final MutableStack<CoreInstance> stack = Stacks.mutable.empty();

    private CoreInstanceStandardPrinter(SafeAppendable appendable, boolean full, boolean addDebug, boolean printEmptyProperties, int max)
    {
        this.appendable = appendable;
        this.full = full;
        this.addDebug = addDebug;
        this.printEmptyProperties = printEmptyProperties;
        this.max = max;
    }

    private void print(CoreInstance instance, String tab)
    {
        this.stack.push(instance);
        this.appendable.append(tab);
        printNodeName(instance);
        this.appendable.append(" instance ");
        printNodeName(instance.getClassifier());
        for (String key : instance.getKeys().toSortedList())
        {
            ListIterable<? extends CoreInstance> values = instance.getValueForMetaPropertyToMany(key);
            if (this.printEmptyProperties || values.notEmpty())
            {
                this.appendable.append('\n');
                printProperty(instance, key, values, tab);
            }
        }
        this.stack.pop();
    }

    private void printNodeName(CoreInstance node)
    {
        String name = node.getName();
        if (this.full)
        {
            this.appendable.append(name).append('_').append(node.getSyntheticId());
        }
        else
        {
            this.appendable.append(ModelRepository.possiblyReplaceAnonymousId(name));
        }

        if (this.addDebug)
        {
            SourceInformation sourceInfo = node.getSourceInformation();
            if (sourceInfo != null)
            {
                this.appendable.append('(').append(sourceInfo.getSourceId()).append(':')
                        .append(sourceInfo.getStartLine()).append(',')
                        .append(sourceInfo.getStartColumn()).append(',')
                        .append(sourceInfo.getLine()).append(',')
                        .append(sourceInfo.getColumn()).append(',')
                        .append(sourceInfo.getEndLine()).append(',')
                        .append(sourceInfo.getEndColumn()).append(')');
            }
        }
    }

    private void printProperty(CoreInstance instance, String propertyName, ListIterable<? extends CoreInstance> values, String tab)
    {
        this.appendable.append(tab).append("    ");
        if (propertyName == null)
        {
            this.appendable.append("null:");
        }
        else
        {
            CoreInstance property = instance.getKeyByName(propertyName);
            CoreInstance propertyClassifier = property.getClassifier();

            printNodeName(property);
            this.appendable.append('(');
            if (propertyClassifier == null)
            {
                this.appendable.append("null");
            }
            else
            {
                printNodeName(propertyClassifier);
            }
            this.appendable.append("):");
        }

        SetIterable<CoreInstance> excluded = instance.getRepository().getExclusionSet();
        for (CoreInstance value : values)
        {
            this.appendable.append('\n');

            if (value == null)
            {
                this.appendable.append(tab).append("        NULL");
            }
            else
            {
                // TODO remove reference to "package" property, which is an M3 thing
                boolean excludeFlag = ((excluded != null) && (excluded.contains(value.getValueForMetaPropertyToOne("package")) || excluded.contains(value))) ||
                        (instance.getRepository().getTopLevel(value.getName()) != null);
                if (this.full ? this.stack.contains(value) : (excludeFlag || (this.stack.size() > this.max) || this.stack.contains(value)))
                {
                    CoreInstance valueClassifier = value.getClassifier();

                    this.appendable.append(tab).append("        ");
                    printNodeName(value);
                    this.appendable.append(" instance ");
                    if (valueClassifier == null)
                    {
                        this.appendable.append("null");
                    }
                    else
                    {
                        printNodeName(valueClassifier);
                        if (this.full)
                        {
                            this.appendable.append('\n').append(tab).append("            [...]");
                        }
                        else if (!excludeFlag && (this.stack.size() > this.max) && value.getKeys().notEmpty())
                        {
                            this.appendable.append('\n').append(tab).append("            [... >").append(this.max).append(']');
                        }
                    }
                }
                else
                {
                    String newTab = tab + "        ";
                    if (value instanceof CoreInstanceWithStandardPrinting)
                    {
                        print(value, newTab);
                    }
                    else if (this.full)
                    {
                        value.printFull(this.appendable, newTab);
                    }
                    else if (this.addDebug)
                    {
                        value.print(this.appendable, newTab, this.max - this.stack.size());
                    }
                    else
                    {
                        value.printWithoutDebug(this.appendable, newTab, this.max - this.stack.size());
                    }
                }
            }
        }
    }

    public static <T extends Appendable> T printFull(T appendable, CoreInstance instance, String tab)
    {
        return print(appendable, instance, tab, true, true, true, CoreInstance.DEFAULT_MAX_PRINT_DEPTH);
    }

    public static <T extends Appendable> T print(T appendable, CoreInstance instance, String tab, int max)
    {
        return print(appendable, instance, tab, false, true, false, max);
    }

    public static <T extends Appendable> T printWithoutDebug(T appendable, CoreInstance instance, String tab, int max)
    {
        return print(appendable, instance, tab, false, false, false, max);
    }

    public static <T extends Appendable> T print(T appendable, CoreInstance instance, String tab, boolean full, boolean addDebug, boolean printEmptyProperties, int max)
    {
        new CoreInstanceStandardPrinter(SafeAppendable.wrap(appendable), full, addDebug, printEmptyProperties, max).print(instance, tab);
        return appendable;
    }
}
