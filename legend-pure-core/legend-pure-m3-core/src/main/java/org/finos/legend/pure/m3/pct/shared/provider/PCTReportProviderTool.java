// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m3.pct.shared.provider;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;

public class PCTReportProviderTool
{
    public static <T> MutableList<T> load(ClassLoader classLoader, Class<T> _class, String... locations)
    {
        try
        {
            MutableList<T> result = Lists.mutable.empty();
            ObjectReader reader = null;
            for (String location : locations)
            {
                URL url = classLoader.getResource(location);
                if (url != null)
                {
                    if (reader == null)
                    {
                        reader = JsonMapper.builder().build().readerFor(_class);
                    }
                    try (InputStream stream = url.openStream())
                    {
                        result.add(reader.readValue(stream));
                    }
                }
            }
            return result;
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
