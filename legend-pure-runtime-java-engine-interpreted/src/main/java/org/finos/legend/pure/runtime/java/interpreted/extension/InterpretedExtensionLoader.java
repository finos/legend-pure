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

package org.finos.legend.pure.runtime.java.interpreted.extension;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

import java.util.Iterator;
import java.util.ServiceLoader;

public class InterpretedExtensionLoader
{
    public static MutableList<InterpretedExtension> extensions()
    {
        MutableList<InterpretedExtension> result = Lists.mutable.empty();
        Iterator<InterpretedExtension> it = ServiceLoader.load(InterpretedExtension.class).iterator();

        while (it.hasNext())
        {
            try
            {
                InterpretedExtension e = it.next();
                result.add(e);
            }
            catch (Throwable z)
            {
                //z.printStackTrace();
                // Needs to be silent ... during the build process
            }
        }
        return result;
    }

}
