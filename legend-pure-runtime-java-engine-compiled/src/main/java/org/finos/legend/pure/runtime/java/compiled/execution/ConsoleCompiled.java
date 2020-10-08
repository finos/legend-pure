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
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.block.function.checked.CheckedFunction;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.finos.legend.pure.m3.coreinstance.BaseCoreInstance;
import org.finos.legend.pure.m3.execution.AbstractConsole;
import org.finos.legend.pure.m3.navigation.Printer;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ConsoleCompiled extends AbstractConsole
{
    private static final String TAB = "    ";

    private static final Predicate<Field> fieldSelection = new Predicate<Field>()
    {
        @Override
        public boolean accept(Field field)
        {
            String fieldName = field.getName();
            return !Modifier.isStatic(field.getModifiers()) && !"__id".equals(fieldName) && !"package".equals(fieldName);
        }
    };

    private static final Function<Field, String> fieldName = new Function<Field, String>()
    {
        @Override
        public String valueOf(Field field)
        {
            return field.getName();
        }
    };

    private final Predicate<Object> isExcluded;

    public ConsoleCompiled(Predicate<Object> isExcluded)
    {
        this.isExcluded = isExcluded;
    }

    public ConsoleCompiled()
    {
        this(Predicates.alwaysFalse());
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
        return toString(content, max, Predicates.alwaysFalse());
    }

    public static String toString(Object content, int max, Predicate<Object> isExcluded)
    {
        if (content == null)
        {
            return "null";
        }
        if (content instanceof RichIterable)
        {
            RichIterable<?> collection = (RichIterable<?>)content;
            int size = collection.size();
            switch (size)
            {
                case 0:
                {
                    return "[]";
                }
                case 1:
                {
                    Object element = collection.getFirst();
                    return processTopLevelValue(element, max, isExcluded);
                }
                default:
                {
                    StringBuilder builder = new StringBuilder("[");
                    for (Object object : collection)
                    {
                        builder.append("\n   ");
                        builder.append(processTopLevelValue(object, max, isExcluded));
                    }
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
            return ("\n".equals(objectStr) || objectStr.isEmpty()) ? objectStr : "\'" + objectStr + "\'";
        }
        if (object instanceof Boolean || object instanceof Number || object instanceof PureDate)
        {
            return object.toString();
        }

        return processOneValue(object, UnifiedSet.newSet(), "", 0, max, isExcluded);
    }

    private static String processOneValue(final Object content, final MutableSet<Object> processed, final String space, final int current, final int max, final Predicate<Object> isExcluded)
    {
        if (content == null)
        {
            return "NULL";
        }
        if (content instanceof String || content instanceof Boolean || content instanceof Long)
        {
            return space+content+" instance "+content.getClass().getSimpleName();
        }
        if (content instanceof RichIterable)
        {
            return ((RichIterable<?>)content).collect(new Function<Object, String>()
            {
                @Override
                public String valueOf(Object o)
                {
                    return processOneValue(o, processed, space, current + 1, max, isExcluded);
                }
            }).makeString("\n");
        }
        if (content instanceof BaseCoreInstance)
        {
            return Printer.print((BaseCoreInstance)content, space, max);
        }
        try
        {
            if (processed.add(content))
            {
                return (current <= max) ? space + getId(content) + " instance " + getType(content) + "\n" +
                        ArrayAdapter.adapt(content.getClass().getFields()).select(fieldSelection).sortThisBy(fieldName).collect(new CheckedFunction<Field, String>()
                        {
                            @Override
                            public String safeValueOf(Field field) throws IllegalAccessException
                            {
                                field.setAccessible(true);
                                Object result = field.get(content);
                                if (result != null)
                                {
                                    if (result instanceof RichIterable)
                                    {
                                        if (((RichIterable)result).isEmpty())
                                        {
                                            return null;
                                        }
                                    }

                                    String toPrint;
                                    if (isExcluded.accept(result))
                                    {
                                        toPrint = space + TAB + TAB + "[X] " + getId(result) + " instance " + getType(result);
                                    }
                                    else
                                    {
                                        toPrint = processOneValue(result, processed, space + TAB + TAB, current + 1, max, isExcluded);
                                    }

                                    return space + TAB + field.getName().substring(1) + "(Property):\n" + toPrint;
                                }
                                return null;
                            }
                        }).select(Predicates.notNull()).makeString("\n") : space + "[>" + max + "] " + getId(content) + " instance " + getType(content);
            }
            return space + "[_] " + getId(content) + " instance " + getType(content);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static String getType(Object content) throws IllegalAccessException
    {
        String typeName;
        try
        {
            Field f = content.getClass().getField("tempTypeName");
            f.setAccessible(true);
            typeName = (String)f.get(null);
        }
        catch (NoSuchFieldException e)
        {
            typeName = content.getClass().getName();
        }
        return typeName;
    }

    public static String getId(Object obj) throws IllegalAccessException
    {
        try
        {
            Field field = obj.getClass().getField("__id");
            field.setAccessible(true);
            String result = (String)field.get(obj);
            return ModelRepository.possiblyReplaceAnonymousId(result);
        }
        catch (NoSuchFieldException e)
        {
            return obj.toString();
        }
    }

    @Override
    protected String getContentString(Object content)
    {
        return String.valueOf(content);
    }
}
