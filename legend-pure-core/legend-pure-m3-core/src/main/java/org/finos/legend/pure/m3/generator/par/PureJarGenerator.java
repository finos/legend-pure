// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.m3.generator.par;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.pure.m3.generator.Log;
import org.finos.legend.pure.m3.generator.LogToSystemOut;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.runtime.ParserService;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Set;

public class PureJarGenerator
{
    public static void main(String[] args) throws Exception
    {
        String purePlatformVersion = args[0];
        String repositories = args[1];
        String outputDirectory = args[2];
        doGeneratePAR(Sets.mutable.with(repositories), Sets.mutable.empty(), Sets.mutable.empty(), purePlatformVersion, null, null, new File(outputDirectory), new LogToSystemOut());
    }

    public static void doGeneratePAR(Set<String> repositories, Set<String> excludedRepositories, Set<String> extraRepositories, String purePlatformVersion, String modelVersion, File sourceDirectory, File outputDirectory, Log log) throws Exception
    {
        doGeneratePAR(repositories, excludedRepositories, extraRepositories, purePlatformVersion, modelVersion, sourceDirectory, outputDirectory, Thread.currentThread().getContextClassLoader(), log);
    }

    public static void doGeneratePAR(Set<String> repositories, Set<String> excludedRepositories, Set<String> extraRepositories, String purePlatformVersion, String modelVersion, File sourceDirectory, File outputDirectory, ClassLoader cl, Log log) throws Exception
    {
        long start = System.nanoTime();
        try
        {
            ParserService ps = new ParserService(cl);

            log.debug("Generating Pure PAR file(s)");
            log.debug("  Requested repositories: " + repositories);
            log.debug("  Excluded repositories: " + excludedRepositories);
            log.debug("  Extra repositories: " + extraRepositories);
            CodeRepositorySet resolvedRepositories = resolveRepositories(repositories, excludedRepositories, extraRepositories, cl, log);
            log.debug("  Specified repositories (with resolved dependencies): " + resolvedRepositories.getRepositories().collect(CodeRepository::getName).makeString("[", ",", "]"));
            log.debug("  Register DSLs: " + ps.parsers().collect(Parser::getName).makeString(", "));
            log.debug("  Register in-line DSLs: " + ps.inlineDSLs().collect(InlineDSL::getName).makeString(", "));
            log.debug("  Pure platform version: " + purePlatformVersion);
            log.debug("  Model version: " + modelVersion);
            log.debug("  Pure source directory: " + sourceDirectory);
            log.debug("  Output directory: " + outputDirectory);

            log.info("  Starting compilation and generation of Pure PAR file(s)");
            PureJarSerializer.writePureRepositoryJars(outputDirectory.toPath(), (sourceDirectory == null) ? null : sourceDirectory.toPath(), purePlatformVersion, modelVersion, resolvedRepositories, cl, log);
        }
        catch (Exception e)
        {
            log.error(String.format("  -> Pure PAR generation failed (%.9fs)", durationSinceInSeconds(start)), e);
            throw e;
        }
        log.info(String.format("  -> Finished Pure PAR generation in %.9fs", durationSinceInSeconds(start)));
    }

    private static CodeRepositorySet resolveRepositories(Set<String> repositories, Set<String> excludedRepositories, Set<String> extraRepositories, ClassLoader classLoader, Log log)
    {
        RichIterable<CodeRepository> cpRepositories = CodeRepositoryProviderHelper.findCodeRepositories(classLoader, true);
        log.debug("  Found repositories (in the classpath): " + cpRepositories.collect(CodeRepository::getName).makeString("[", ",", "]"));
        CodeRepositorySet.Builder builder = CodeRepositorySet.builder().withCodeRepositories(cpRepositories);
        if (extraRepositories != null)
        {
            extraRepositories.forEach(r -> builder.addCodeRepository(getExtraRepository(classLoader, r)));
        }
        if (excludedRepositories != null)
        {
            builder.withoutCodeRepositories(excludedRepositories);
        }
        if ((repositories != null) && !repositories.isEmpty())
        {
            builder.subset(repositories);
        }
        return builder.build();
    }

    private static GenericCodeRepository getExtraRepository(ClassLoader classLoader, String extraRepository)
    {
        // First check if this is a resource
        URL url = classLoader.getResource(extraRepository);
        if (url != null)
        {
            try
            {
                return GenericCodeRepository.build(url);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error loading extra repository \"" + extraRepository + "\" from resource " + url, e);
            }
        }

        // If it's not a resource, assume it is a file path
        try
        {
            return GenericCodeRepository.build(Paths.get(extraRepository));
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error loading extra repository \"" + extraRepository + "\"", e);
        }
    }

    private static double durationSinceInSeconds(long startNanos)
    {
        return durationInSeconds(startNanos, System.nanoTime());
    }

    private static double durationInSeconds(long startNanos, long endNanos)
    {
        return (endNanos - startNanos) / 1_000_000_000.0;
    }
}
