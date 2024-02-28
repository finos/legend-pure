// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.store.relational.shared;

import org.eclipse.collections.api.map.primitive.ImmutableIntObjectMap;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;

import java.lang.reflect.Field;
import java.sql.Types;

public class JavaSqlTypeNames
{
    public static final ImmutableIntObjectMap<String> SqlTypeNames = JavaSqlTypeNames.initializeSQLTypeNames();

    private static ImmutableIntObjectMap<String> initializeSQLTypeNames()
    {
        MutableIntObjectMap<String> sqlTypeNames = IntObjectMaps.mutable.empty();
        for (Field field : Types.class.getFields())
        {
            try
            {
                sqlTypeNames.put((int)field.get(null), field.getName());
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        }
        return sqlTypeNames.toImmutable();
    }
}
