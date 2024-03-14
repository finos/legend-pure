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
        doGeneratePAR(Sets.mutable.with(repositories), Sets.mutable.empty(), Sets.mutable.empty(), purePlatformVersion, null, null, new File(outputDirectory), new Log()
        {
            @Override
            public void info(String txt)
            {
                System.out.println(txt);
            }

            @Override
            public void error(String txt, Exception e)
            {
                System.out.println("ERROR" + txt);
                e.printStackTrace();
            }
        });
    }


    public static void doGeneratePAR(Set<String> repositories, Set<String> excludedRepositories, Set<String> extraRepositories, String purePlatformVersion, String modelVersion, File sourceDirectory, File outputDirectory, Log log) throws Exception
    {
        long start = System.nanoTime();
        try
        {
            ParserService ps = new ParserService();

            log.info("Generating Pure PAR file(s)");
            log.info("  Requested repositories: " + repositories);
            log.info("  Excluded repositories: " + excludedRepositories);
            log.info("  Extra repositories: " + extraRepositories);
            CodeRepositorySet resolvedRepositories = resolveRepositories(repositories, excludedRepositories, extraRepositories, log);
            log.info("  Specified repositories (with resolved dependencies): " + resolvedRepositories.getRepositories().collect(CodeRepository::getName).makeString("[", ",", "]"));
            log.info("  Register DSLs: " + ps.parsers().collect(Parser::getName).makeString(", "));
            log.info("  Register in-line DSLs: " + ps.inlineDSLs().collect(InlineDSL::getName).makeString(", "));
            log.info("  Pure platform version: " + purePlatformVersion);
            log.info("  Model version: " + modelVersion);
            log.info("  Pure source directory: " + sourceDirectory);
            log.info("  Output directory: " + outputDirectory);

            log.info("  Starting compilation and generation of Pure PAR file(s)");
            PureJarSerializer.writePureRepositoryJars(outputDirectory.toPath(), (sourceDirectory == null) ? null : sourceDirectory.toPath(), purePlatformVersion, modelVersion, resolvedRepositories, log);
        }
        catch (Exception e)
        {
            log.error(String.format("  -> Pure PAR generation failed (%.9fs)", durationSinceInSeconds(start)), e);
            throw e;
        }
        log.info(String.format("  -> Finished Pure PAR generation in %.9fs", durationSinceInSeconds(start)));
    }

    private static CodeRepositorySet resolveRepositories(Set<String> repositories, Set<String> excludedRepositories, Set<String> extraRepositories, Log log)
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        RichIterable<CodeRepository> cpRepositories = CodeRepositoryProviderHelper.findCodeRepositories(classLoader, true);
        log.info("  Found repositories (in the classpath): " + cpRepositories.collect(CodeRepository::getName).makeString("[", ",", "]"));
        CodeRepositorySet.Builder builder = CodeRepositorySet.newBuilder().withCodeRepositories(cpRepositories);
        if (extraRepositories != null)
        {
            extraRepositories.forEach(r -> builder.addCodeRepository(getExtraRepository(classLoader, r)));
        }
        if (excludedRepositories != null)
        {
            builder.withoutCodeRepositories(excludedRepositories);
        }
        CodeRepositorySet newRepositories = builder.build();
        return ((repositories == null) || repositories.isEmpty()) ? newRepositories : newRepositories.subset(repositories);
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
