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

package org.finos.legend.pure.m3.serialization.filesystem.repository;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class GenericCodeRepository extends CodeRepository
{
    private final ImmutableSet<String> dependencies;

    public GenericCodeRepository(String name, String pattern, Iterable<String> dependencies)
    {
        super(name, Pattern.compile(pattern));
        this.dependencies = Sets.immutable.withAll(dependencies);
    }

    public GenericCodeRepository(String name, String pattern, String... dependencies)
    {
        this(name, pattern, Sets.immutable.with(dependencies));
    }

    @Override
    public boolean isVisible(CodeRepository other)
    {
        return this == other || this.dependencies.contains(other.getName());
    }

    public SetIterable<String> getDependencies()
    {
        return this.dependencies;
    }

    public static GenericCodeRepository build(String resourcePath)
    {
        InputStream inputStream = GenericCodeRepository.class.getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null)
        {
            throw new RuntimeException("The resource "+resourcePath+" can't be found!");
        }
        return build(inputStream);
    }

    public static GenericCodeRepository build(InputStream inputStream)
    {
        JSONObject spec = loadResource(inputStream);
        return new GenericCodeRepository(getName(spec), getPattern(spec), getDependencies(spec));
    }

    private static JSONObject loadResource(InputStream inputStream)
    {
        try
        {
            return (JSONObject) new JSONParser().parse(new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream))));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static String getName(JSONObject obj)
    {
        return getString(obj, "name");
    }

    private static String getPattern(JSONObject obj)
    {
        return getString(obj, "pattern");
    }

    private static List<String> getDependencies(JSONObject obj)
    {
        return getStringArray(obj, "dependencies");
    }

    private static String getString(JSONObject obj, String name)
    {
        String val = (String) obj.get(name);
        if (val == null)
        {
            throw new RuntimeException("'" + name + "' has not been defined!");
        }
        return val;
    }

    private static List<String> getStringArray(JSONObject obj, String name)
    {
        JSONArray val = (JSONArray) obj.get(name);
        if (val == null)
        {
            throw new RuntimeException("'" + name + "' has not been defined!");
        }
        return val;
    }
}