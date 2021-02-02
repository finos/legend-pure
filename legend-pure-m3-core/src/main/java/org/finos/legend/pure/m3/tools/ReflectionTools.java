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

package org.finos.legend.pure.m3.tools;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

public class ReflectionTools
{
    public static void set(Field f, Object o, Object val)
    {
        try
        {
            f.set(o, val);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static Class load(String str)
    {
        try
        {
            ClassLoader classLoader = ReflectionTools.class.getClassLoader();
            return classLoader.loadClass(str);
        }
        catch(Exception e)
        {
            //e.printStackTrace();
        }
        return null;
    }

    public static Field field(Class cl, String prop)
    {
        try
        {
            Field[] fs = cl.getDeclaredFields();
            Field f = null;
            for (Field of: fs)
            {
                if(of.getName().equals(prop))
                {
                    f = of;
                    break;
                }
            }

            if (f == null)
            {
                throw new RuntimeException("Property '"+prop+"' not found in class "+cl.getName());
            }

            f.setAccessible(true);

            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);

            return f;
        }
        catch(Exception e)
        {
            //e.printStackTrace();
        }
        return null;
    }

    public static void clearStatic(String str, String prop)
    {
        try
        {
            Class<?> x = load(str);
            Objects.requireNonNull(field(x, prop)).set(null, null);
        }
        catch(Exception e)
        {
            //e.printStackTrace();
        }
    }

}
