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

package org.finos.legend.pure.runtime.java.compiled.generation;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.StringIterate;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateTime;
import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.StrictDate;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;

public class JavaPurePrimitiveTypeMapping
{
    private JavaPurePrimitiveTypeMapping()
    {
    }

    public static String getPureM3TypeFromJavaPrimitivesAndDates(Object obj)
    {
        return getPureM3TypeFromJavaPrimitivesAndDates(obj.getClass());
    }

    public static String getPureM3TypeFromJavaPrimitivesAndDates(Class<?> clazz)
    {
        if (String.class.equals(clazz))
        {
            return M3Paths.String;
        }
        if (Boolean.class.equals(clazz) || boolean.class.equals(clazz))
        {
            return M3Paths.Boolean;
        }
        if (Long.class.equals(clazz) || long.class.equals(clazz))
        {
            return M3Paths.Integer;
        }
        if (Double.class.equals(clazz) || double.class.equals(clazz))
        {
            return M3Paths.Float;
        }
        if (Number.class.equals(clazz))
        {
            return M3Paths.Number;
        }
        if (LatestDate.class.equals(clazz))
        {
            return M3Paths.LatestDate;
        }
        if (StrictDate.class.equals(clazz))
        {
            return M3Paths.StrictDate;
        }
        if (DateTime.class.isAssignableFrom(clazz))
        {
            return M3Paths.DateTime;
        }
        if (PureDate.class.isAssignableFrom(clazz))
        {
            return M3Paths.Date;
        }
        return null;
    }

    public static String convertPureCoreInstanceToJavaType(CoreInstance instance, ProcessorContext processorContext)
    {
        ProcessorSupport support = processorContext.getSupport();

        if (support.instance_instanceOf(instance, M3Paths.String))
        {
            return createStringConstant(instance);
        }
        if (support.instance_instanceOf(instance, M3Paths.Boolean))
        {
            return Boolean.parseBoolean(instance.getName()) ? "true" : "false";
        }
        if (support.instance_instanceOf(instance, M3Paths.Integer))
        {
            try
            {
                Long.parseLong(instance.getName());
                return instance.getName() + 'l';
            }
            catch (NumberFormatException e)
            {
                return "-1l";
            }
        }
        if (support.instance_instanceOf(instance, M3Paths.Float))
        {
            return "java.lang.Double.valueOf(" + instance.getName() + ")";
        }
        if (support.instance_instanceOf(instance, M3Paths.Decimal))
        {
            return "new java.math.BigDecimal(\"" + instance.getName() + "\")";
        }
        if (support.instance_instanceOf(instance, M3Paths.StrictDate))
        {
            return "org.finos.legend.pure.m4.coreinstance.primitive.date.DateFormat.parseStrictDate(\"" + instance.getName() + "\")";
        }
        if (support.instance_instanceOf(instance, M3Paths.DateTime))
        {
            return "org.finos.legend.pure.m4.coreinstance.primitive.date.DateFormat.parseDateTime(\"" + instance.getName() + "\")";
        }
        if (support.instance_instanceOf(instance, M3Paths.Date))
        {
            return "org.finos.legend.pure.m4.coreinstance.primitive.date.DateFormat.parsePureDate(\"" + instance.getName() + "\")";
        }
        if (support.instance_instanceOf(instance, M3Paths.LatestDate))
        {
            return "LatestDate.instance";
        }
        if (support.instance_instanceOf(instance, M3Paths.InstanceValue))
        {
            return ValueSpecificationProcessor.processValueSpecification(instance, processorContext);
        }
        if (support.instance_instanceOf(instance, M3Paths.VariableExpression))
        {
            return ValueSpecificationProcessor.processValueSpecification(instance, processorContext);
        }

        throw new RuntimeException("To Code! " + instance.print("") + ", class=" + instance.getClass().getName() +
                ", \nsupport=" + support.getClass().getName() +
                ", \nclassifierId=" + support.getClassifier(instance).getSyntheticId() +
                ", \nclassifierClass=" + support.getClassifier(instance).getClass().getName() + "@" + support.getClassifier(instance).hashCode() +
                ", \nclassifier=" + support.getClassifier(instance).print("", 1) +
                ", \ninstanceRepository=" + support.getClassifier(instance).getRepository().toString());
    }

    private static String createStringConstant(CoreInstance instance)
    {
        //In java string constants must be under the limit 65536
        int CHUNK_SIZE = 65000;

        String value = StringEscapeUtils.escapeJava(instance.getName());
        String output;

        if (value.length() > CHUNK_SIZE)
        {
            ListIterable<String> chunks = chunk(value, CHUNK_SIZE);
            output = chunks.makeString("new StringBuilder().append(\"", "\").append(\"", "\").toString()");
        }
        else
        {
            output = "\"" + value + "\"";
        }
        return output;
    }

    //we need to ensure the chunks do not split escape characters
    //note: this function was deliberately written without recursion.
    public static ListIterable<String> chunk(String value, int size) {
        MutableList<String> chunks = StringIterate.chunk(value, size);

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            boolean isInvalid = chunk.endsWith("\\") && !chunk.endsWith("\\\\");

            if (isInvalid) {
                String nextChunk = "\\" + chunks.get(i + 1);
                chunk = chunk.substring(0, chunk.length() - 1);

                chunks.set(i, chunk);
                chunks.set(i + 1, nextChunk);
            }
        }

        return chunks;
    }
}
