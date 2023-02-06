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

package org.finos.legend.pure.runtime.java.compiled.factory;

import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m4.coreinstance.factory.CoreInstanceFactory;

import java.util.ServiceLoader;

public class JavaModelFactoryRegistryLoader
{
    public static void main(String args[])
    {
        System.out.println(loader().allManagedTypes().toSortedList().makeString("\n"));
    }

    public static CoreInstanceFactoryRegistry loader()
    {
        CoreInstanceFactoryRegistry result = new CoreInstanceFactoryRegistry(IntObjectMaps.immutable.<CoreInstanceFactory>empty(), Maps.immutable.<String, CoreInstanceFactory>empty(), Maps.immutable.<String, Class>empty());
        for (JavaModelFactoryRegistry l : ServiceLoader.load(JavaModelFactoryRegistry.class))
        {
            try
            {
                result = result.combine((CoreInstanceFactoryRegistry)l.getClass().getDeclaredField("REGISTRY").get(null));
            }
            catch (Throwable e)
            {
                // Catch and do nothing (during build time)
            }
        }
        return result;
    }
}