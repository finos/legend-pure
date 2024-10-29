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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map;

import org.eclipse.collections.api.factory.Stacks;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m4.exception.PureException;

public class PureCacheMapGetException extends PureExecutionException
{
    private final Object key;

    PureCacheMapGetException(Object key, Throwable cause)
    {
        super(generateInfo(key, cause), cause, Stacks.mutable.empty());
        this.key = key;
    }

    public Object getKey()
    {
        return this.key;
    }

    private static String generateInfo(Object key, Throwable cause)
    {
        StringBuilder info = new StringBuilder("Exception fetching Cache value for Key ");
        info.append(key);
        String subInfo = (cause instanceof PureException) ? ((PureException)cause).getInfo() : cause.getMessage();
        if (subInfo != null)
        {
            info.append(": ");
            info.append(subInfo);
        }
        return info.toString();
    }
}
