// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.lsp;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.eclipse.lsp4j.InitializeParams;
import org.junit.Assert;
import org.junit.Test;

public class LegendPureLspServerTest
{
    @Test
    public void extractClasspathRepositoryNames_readsDirectInitializationOption()
    {
        InitializeParams params = new InitializeParams();
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("classpathRepositories", Arrays.asList(" core ", "", "pure_ide", "core"));
        params.setInitializationOptions(options);

        Assert.assertEquals(
                Arrays.asList("core", "pure_ide"),
                LegendPureLspServer.extractClasspathRepositoryNames(params));
    }

    @Test
    public void extractClasspathRepositoryNames_readsNestedServerInitializationOption()
    {
        InitializeParams params = new InitializeParams();
        Map<String, Object> serverOptions = new LinkedHashMap<>();
        serverOptions.put("classpathRepositories", Arrays.asList("extension_repo"));
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("server", serverOptions);
        params.setInitializationOptions(options);

        Assert.assertEquals(
                Arrays.asList("extension_repo"),
                LegendPureLspServer.extractClasspathRepositoryNames(params));
    }

    @Test
    public void extractClasspathRepositoryNames_readsJsonInitializationOption()
    {
        InitializeParams params = new InitializeParams();
        JsonArray repositories = new JsonArray();
        repositories.add("core");
        repositories.add("core");
        repositories.add("pure_ide");
        JsonObject options = new JsonObject();
        options.add("classpathRepositories", repositories);
        params.setInitializationOptions(options);

        Assert.assertEquals(
                Arrays.asList("core", "pure_ide"),
                LegendPureLspServer.extractClasspathRepositoryNames(params));
    }

    @Test
    public void extractClasspathRepositoryNames_defaultsToEmptyList()
    {
        Assert.assertEquals(
                Collections.emptyList(),
                LegendPureLspServer.extractClasspathRepositoryNames(new InitializeParams()));
    }
}
