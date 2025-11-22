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
import org.finos.legend.pure.m4.tools.SafeAppendable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
        return (content == null) ? "null" : append(new StringBuilder(), content, max, isExcluded).toString();
    }

    public static <T extends Appendable> T append(T appendable, Object content, int max)
    {
        return append(appendable, content, max, o -> false);
    }

    public static <T extends Appendable> T append(T appendable, Object content, int max, Predicate<Object> isExcluded)
    {
        append(SafeAppendable.wrap(appendable), content, max, isExcluded);
        return appendable;
    }

    private static SafeAppendable append(SafeAppendable appendable, Object content, int max, Predicate<Object> isExcluded)
    {
        if (content == null)
        {
            return appendable.append("null");
        }
        if (content instanceof RichIterable)
        {
            RichIterable<?> collection = (RichIterable<?>) content;
            switch (collection.size())
            {
                case 0:
                {
                    return appendable.append("[]");
                }
                case 1:
                {
                    return appendTopLevelValue(appendable, collection.getAny(), max, isExcluded);
                }
                default:
                {
                    appendable.append("[");
                    collection.forEach(o -> appendTopLevelValue(appendable.append("\n   "), o, max, isExcluded));
                    return appendable.append("\n]");
                }
            }
        }
        return appendTopLevelValue(appendable, content, max, isExcluded);
    }

    private static SafeAppendable appendTopLevelValue(SafeAppendable appendable, Object object, int max, Predicate<Object> isExcluded)
    {
        if (object instanceof String)
        {
            String objectStr = object.toString();
            return ("\n".equals(objectStr) || objectStr.isEmpty()) ?
                   appendable.append(objectStr) :
                   appendable.append('\'').append(objectStr).append('\'');
        }
        if ((object instanceof Boolean) || (object instanceof Number))
        {
            return appendable.append(object);
        }
        if (object instanceof PureDate)
        {
            return ((PureDate) object).appendString(appendable);
        }
        return appendOneValue(appendable, object, Sets.mutable.empty(), "", 0, max, isExcluded);
    }

    private static SafeAppendable appendOneValue(SafeAppendable appendable, Object content, MutableSet<Object> processed, String space, int current, int max, Predicate<Object> isExcluded)
    {
        if (content == null)
        {
            return appendable.append("NULL");
        }
        if (content instanceof String || content instanceof Boolean || content instanceof Long)
        {
            return appendable.append(space).append(content).append(" instance ").append(content.getClass().getSimpleName());
        }
        if (content instanceof RichIterable)
        {
            boolean[] first = {true};
            ((RichIterable<?>) content).forEach(o ->
            {
                if (first[0])
                {
                    first[0] = false;
                }
                else
                {
                    appendable.append("\n");
                }
                appendOneValue(appendable, o, processed, space, current + 1, max, isExcluded);
            });
            return appendable;
        }
        if (content instanceof BaseCoreInstance)
        {
            Printer.print(appendable, (BaseCoreInstance) content, space, max, new CompiledProcessorSupport(null, null, null));
            return appendable;
        }
        if (processed.add(content))
        {
            if (current > max)
            {
                return appendable.append(space).append("[>").append(max).append("] ").append(safeGetId(content)).append(" instance ").append(getType(content));
            }
            appendable.append(space).append(safeGetId(content)).append(" instance ").append(getType(content));
            ArrayIterate.select(content.getClass().getFields(), ConsoleCompiled::selectField)
                    .sortThisBy(Field::getName)
                    .forEach(field ->
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
                        if ((result != null) && !((result instanceof RichIterable) && ((RichIterable<?>) result).isEmpty()))
                        {
                            appendable.append(space).append(TAB).append(field.getName().substring(1)).append("(Property):\n");
                            if (isExcluded.test(result))
                            {
                                appendable.append(space).append(TAB).append(TAB).append("[X] ").append(safeGetId(result)).append(" instance ").append(getType(result));
                            }
                            else
                            {
                                appendOneValue(appendable, result, processed, space + TAB + TAB, current + 1, max, isExcluded);
                            }
                        }
                    });
            return appendable;
        }
        return appendable.append(space).append("[_] ").append(safeGetId(content)).append(" instance ").append(getType(content));
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
