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
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class GenericCodeRepository extends CodeRepository
{
    private final ImmutableSet<String> dependencies;

    private GenericCodeRepository(String name, Pattern pattern, Iterable<String> dependencies)
    {
        super(name, pattern);
        this.dependencies = Sets.immutable.withAll(dependencies);
    }

    public GenericCodeRepository(String name, String pattern, Iterable<String> dependencies)
    {
        this(name, Pattern.compile(pattern), dependencies);
    }

    public GenericCodeRepository(String name, String pattern, String... dependencies)
    {
        this(name, pattern, Sets.immutable.with(dependencies));
    }

    @Override
    public boolean isVisible(CodeRepository other)
    {
        return (this == other) || ((other != null) && this.dependencies.contains(other.getName()));
    }

    public SetIterable<String> getDependencies()
    {
        return this.dependencies;
    }

    public static GenericCodeRepository build(String name, Pattern pattern, Iterable<String> dependencies)
    {
        return new GenericCodeRepository(name, pattern, dependencies);
    }

    public static GenericCodeRepository build(String name, Pattern pattern, String... dependencies)
    {
        return build(name, pattern, Sets.immutable.with(dependencies));
    }

    public static GenericCodeRepository build(String name, String pattern, Iterable<String> dependencies)
    {
        return build(name, Pattern.compile(pattern), dependencies);
    }

    public static GenericCodeRepository build(String name, String pattern, String... dependencies)
    {
        return build(name, Pattern.compile(pattern), dependencies);
    }

    public static GenericCodeRepository build(String resourcePath)
    {
        return build(Thread.currentThread().getContextClassLoader(), resourcePath);
    }

    public static GenericCodeRepository build(ClassLoader classLoader, String resourcePath)
    {
        URL url = classLoader.getResource(resourcePath);
        if (url == null)
        {
            throw new RuntimeException("The resource \"" + resourcePath + "\" can't be found!");
        }
        try (Reader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)))
        {
            return buildFromReader(reader);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Error loading code repository definition from resource \"" + resourcePath + "\" (" + url + ")", e);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error loading code repository definition from resource \"" + resourcePath + "\" (" + url + ")", e);
        }
    }

    public static GenericCodeRepository build(File specFile)
    {
        return build(specFile.toPath());
    }

    public static GenericCodeRepository build(Path specFile)
    {
        try (Reader reader = Files.newBufferedReader(specFile, StandardCharsets.UTF_8))
        {
            return buildFromReader(reader);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Error loading code repository specification from " + specFile, e);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error loading code repository specification from " + specFile, e);
        }
    }

    public static GenericCodeRepository build(URL url)
    {
        try (Reader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)))
        {
            return buildFromReader(reader);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Error loading code repository definition from URL " + url, e);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error loading code repository definition from URL " + url, e);
        }
    }

    public static GenericCodeRepository build(InputStream inputStream)
    {
        try (Reader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8)))
        {
            return buildFromReader(reader);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        catch (ParseException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static GenericCodeRepository build(Reader reader)
    {
        try
        {
            return buildFromReader(reader);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        catch (ParseException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static GenericCodeRepository buildFromReader(Reader reader) throws IOException, ParseException
    {
        JSONObject spec = loadResource(Objects.requireNonNull(reader));
        return build(getName(spec), getPattern(spec), getDependencies(spec));
    }

    private static JSONObject loadResource(Reader reader) throws IOException, ParseException
    {
        return (JSONObject) new JSONParser().parse(reader);
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
