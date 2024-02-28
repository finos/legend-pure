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

package org.finos.legend.pure.runtime.java.compiled.execution;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.coreinstance.BaseCoreInstance;
import org.finos.legend.pure.m3.execution.AbstractConsole;
import org.finos.legend.pure.m3.navigation.Printer;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.Predicate;

public class ConsoleCompiled extends AbstractConsole
{
    private static final String TAB = "    ";

    private final Predicate<Object> isExcluded;

    public ConsoleCompiled(Predicate<Object> isExcluded)
    {
        this.isExcluded = isExcluded;
    }

    public ConsoleCompiled()
    {
        this(o -> false);
    }

    public void print(Object content, int max)
    {
        if (isEnabled())
        {
            print(toString(content, max, this.isExcluded));
        }
    }

    public static String toString(Object content, int max)
    {
        return toString(content, max, o -> false);
    }

    public static String toString(Object content, int max, Predicate<Object> isExcluded)
    {
        if (content == null)
        {
            return "null";
        }
        if (content instanceof RichIterable)
        {
            RichIterable<?> collection = (RichIterable<?>) content;
            int size = collection.size();
            switch (size)
            {
                case 0:
                {
                    return "[]";
                }
                case 1:
                {
                    Object element = collection.getAny();
                    return processTopLevelValue(element, max, isExcluded);
                }
                default:
                {
                    StringBuilder builder = new StringBuilder("[");
                    collection.forEach(o -> builder.append("\n   ").append(processTopLevelValue(o, max, isExcluded)));
                    builder.append("\n]");
                    return builder.toString();
                }
            }
        }
        return processTopLevelValue(content, max, isExcluded);
    }

    private static String processTopLevelValue(Object object, int max, Predicate<Object> isExcluded)
    {
        if (object instanceof String)
        {
            String objectStr = object.toString();
            return ("\n".equals(objectStr) || objectStr.isEmpty()) ? objectStr : "'" + objectStr + "'";
        }
        if (object instanceof Boolean || object instanceof Number || object instanceof PureDate)
        {
            return object.toString();
        }

        return processOneValue(object, Sets.mutable.empty(), "", 0, max, isExcluded);
    }

    private static String processOneValue(Object content, MutableSet<Object> processed, String space, int current, int max, Predicate<Object> isExcluded)
    {
        if (content == null)
        {
            return "NULL";
        }
        if (content instanceof String || content instanceof Boolean || content instanceof Long)
        {
            return space + content + " instance " + content.getClass().getSimpleName();
        }
        if (content instanceof RichIterable)
        {
            return ((RichIterable<?>) content).collect(o -> processOneValue(o, processed, space, current + 1, max, isExcluded)).makeString("\n");
        }
        if (content instanceof BaseCoreInstance)
        {
            return Printer.print((BaseCoreInstance) content, space, max, new CompiledProcessorSupport(null, null, null));
        }
        if (processed.add(content))
        {
            if (current > max)
            {
                return space + "[>" + max + "] " + safeGetId(content) + " instance " + getType(content);
            }
            return ArrayIterate.select(content.getClass().getFields(), ConsoleCompiled::selectField)
                    .sortThisBy(Field::getName)
                    .collect(field ->
                    {
                        field.setAccessible(true);
                        Object result;
                        try
                        {
                            result = field.get(content);
                        }
                        catch (IllegalAccessException e)
                        {
                            throw new RuntimeException(e);
                        }
                        if (result == null)
                        {
                            return null;
                        }
                        if (result instanceof RichIterable)
                        {
                            if (((RichIterable<?>) result).isEmpty())
                            {
                                return null;
                            }
                        }

                        String toPrint;
                        if (isExcluded.test(result))
                        {
                            toPrint = space + TAB + TAB + "[X] " + safeGetId(result) + " instance " + getType(result);
                        }
                        else
                        {
                            toPrint = processOneValue(result, processed, space + TAB + TAB, current + 1, max, isExcluded);
                        }

                        return space + TAB + field.getName().substring(1) + "(Property):\n" + toPrint;
                    })
                    .select(Objects::nonNull)
                    .makeString(space + safeGetId(content) + " instance " + getType(content) + "\n", "\n", "");
        }
        return space + "[_] " + safeGetId(content) + " instance " + getType(content);
    }

    private static boolean selectField(Field field)
    {
        String fieldName = field.getName();
        return !Modifier.isStatic(field.getModifiers()) && !"__id".equals(fieldName) && !"package".equals(fieldName);
    }

    private static String getType(Object content)
    {
        try
        {
            Field f = content.getClass().getField("tempTypeName");
            f.setAccessible(true);
            return (String) f.get(null);
        }
        catch (NoSuchFieldException e)
        {
            return content.getClass().getName();
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static String getId(Object obj) throws IllegalAccessException
    {
        try
        {
            Field field = obj.getClass().getField("__id");
            field.setAccessible(true);
            String result = (String) field.get(obj);
            return ModelRepository.possiblyReplaceAnonymousId(result);
        }
        catch (NoSuchFieldException e)
        {
            return obj.toString();
        }
    }

    private static String safeGetId(Object obj)
    {
        try
        {
            return getId(obj);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String getContentString(Object content)
    {
        return String.valueOf(content);
    }
}
