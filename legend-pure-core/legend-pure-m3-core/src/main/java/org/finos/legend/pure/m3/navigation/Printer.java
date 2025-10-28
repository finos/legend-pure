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

package org.finos.legend.pure.m3.navigation;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.SafeAppendable;

public class Printer
{
    private static final int DEFAULT_MAX_DEPTH = 10;
    private static final int DEFAULT_INITIAL_STRING_BUFFER_SIZE = 256;
    private static final String TAB = "    ";

    /**
     * Print a representation of a Pure instance to an Appendable.
     *
     * @param appendable appendable to print to
     * @param instance   Pure instance to print
     * @param linePrefix string printed at the start of every line
     * @param maxDepth   maximum depth to print to
     */
    public static void print(Appendable appendable, CoreInstance instance, String linePrefix, int maxDepth, ProcessorSupport processorSupport)
    {
        print(SafeAppendable.wrap(appendable), instance, linePrefix, 0, Stacks.mutable.empty(), instance.getRepository() == null ? Sets.immutable.empty() : instance.getRepository().getExclusionSet(), 0, maxDepth, processorSupport);
    }

    /**
     * Print a representation of a Pure instance to an Appendable.
     *
     * @param appendable appendable to print to
     * @param instance   Pure instance to print
     * @param linePrefix string printed at the start of every line
     */
    public static void print(Appendable appendable, CoreInstance instance, String linePrefix, ProcessorSupport processorSupport)
    {
        print(appendable, instance, linePrefix, DEFAULT_MAX_DEPTH, processorSupport);
    }

    /**
     * Print a representation of a Pure instance to an Appendable.
     *
     * @param appendable appendable to print to
     * @param instance   Pure instance to print
     * @param maxDepth   maximum depth to print to
     */
    public static void print(Appendable appendable, CoreInstance instance, int maxDepth, ProcessorSupport processorSupport)
    {
        print(appendable, instance, null, maxDepth, processorSupport);
    }

    /**
     * Print a representation of a Pure instance to an Appendable.
     *
     * @param appendable appendable to print to
     * @param instance   Pure instance to print
     */
    public static void print(Appendable appendable, CoreInstance instance, ProcessorSupport processorSupport)
    {
        print(appendable, instance, null, processorSupport);
    }

    /**
     * Print a representation of a Pure instance to a string.
     *
     * @param instance   Pure instance to print
     * @param linePrefix string printed at the start of every line
     * @param maxDepth   maximum depth to print to
     * @return string representation of instance
     */
    public static String print(CoreInstance instance, String linePrefix, int maxDepth, ProcessorSupport processorSupport)
    {
        StringBuilder builder = new StringBuilder(DEFAULT_INITIAL_STRING_BUFFER_SIZE);
        print(builder, instance, linePrefix, maxDepth, processorSupport);
        return builder.toString();
    }

    /**
     * Print a representation of a Pure instance to a string.
     *
     * @param instance   Pure instance to print
     * @param linePrefix string printed at the start of every line
     * @return string representation of instance
     */
    public static String print(CoreInstance instance, String linePrefix, ProcessorSupport processorSupport)
    {
        return print(instance, linePrefix, DEFAULT_MAX_DEPTH, processorSupport);
    }

    /**
     * Print a representation of a Pure instance to a string.
     *
     * @param instance Pure instance to print
     * @param maxDepth maximum depth to print to
     * @return string representation of instance
     */
    public static String print(CoreInstance instance, int maxDepth, ProcessorSupport processorSupport)
    {
        return print(instance, null, maxDepth, processorSupport);
    }

    /**
     * Print a representation of a Pure instance.
     *
     * @param instance Pure instance to print
     * @return string representation of instance
     */
    public static String print(CoreInstance instance, ProcessorSupport processorSupport)
    {
        return print(instance, null, processorSupport);
    }

    private static void print(SafeAppendable appendable, CoreInstance instance, String linePrefix, int indentLevel, MutableStack<CoreInstance> processed, SetIterable<CoreInstance> exclude, int depth, int maxDepth, ProcessorSupport processorSupport)
    {
        processed.push(instance);

        indent(appendable, linePrefix, indentLevel);
        printInstanceName(appendable, instance, processorSupport);
        for (String propertyName : instance.getKeys().toSortedList())
        {
            ListIterable<? extends CoreInstance> values = instance.getValueForMetaPropertyToMany(propertyName);
            if (values.notEmpty())
            {
                appendable.append('\n');
                indent(appendable, linePrefix, indentLevel + 1);
                appendable.append(propertyName).append("(Property):");

                for (CoreInstance value : values)
                {
                    appendable.append('\n');
                    if (processorSupport != null && processorSupport.getClassifier(value) != null && "ImportStub".equals(processorSupport.getClassifier(value).getName()))
                    {
                        indent(appendable, linePrefix, indentLevel + 2);
                        appendable.append("[~>] ").append(value.getValueForMetaPropertyToOne(M3Properties.idOrPath).getName());
                        CoreInstance resolvedNode = value.getValueForMetaPropertyToOne(M3Properties.resolvedNode);
                        if (resolvedNode == null)
                        {
                            appendable.append("'UNRESOLVED'");
                        }
                        else
                        {
                            appendable.append(" instance ").append(processorSupport.getClassifier(resolvedNode).getName());
                        }
                    }
                    else if (exclude.contains(value) || exclude.contains(value.getValueForMetaPropertyToOne(M3Properties._package)))
                    {
                        indent(appendable, linePrefix, indentLevel + 2);
                        printInstanceName(appendable.append("[X] "), value, processorSupport);
                    }
                    else if (depth >= maxDepth)
                    {
                        indent(appendable, linePrefix, indentLevel + 2);
                        appendable.append("[>").append(maxDepth).append("] ");
                        printInstanceName(appendable, value, processorSupport);
                    }
                    else if (processed.contains(value))
                    {
                        indent(appendable, linePrefix, indentLevel + 2);
                        printInstanceName(appendable.append("[_] "), value, processorSupport);
                    }
                    else
                    {
                        print(appendable, value, linePrefix, indentLevel + 2, processed, exclude, depth + 1, maxDepth, processorSupport);
                    }
                }
            }
        }
        processed.pop();
    }

    private static void indent(SafeAppendable appendable, String prefix, int level)
    {
        if (prefix != null)
        {
            appendable.append(prefix);
        }
        for (; level > 0; level--)
        {
            appendable.append(TAB);
        }
    }

    private static void printInstanceName(SafeAppendable appendable, CoreInstance instance, ProcessorSupport processorSupport)
    {
        String name = instance.getName();
        CoreInstance classifier = (processorSupport == null) ? instance.getClassifier() : processorSupport.getClassifier(instance);
        appendable.append(ModelRepository.possiblyReplaceAnonymousId(name))
                .append(" instance ")
                .append((classifier == null) ? "null" : classifier.getName());
    }
}
