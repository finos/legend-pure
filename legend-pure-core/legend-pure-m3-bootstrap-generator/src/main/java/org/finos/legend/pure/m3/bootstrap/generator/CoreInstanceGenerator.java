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

package org.finos.legend.pure.m3.bootstrap.generator;

import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m3.tools.FileTools;
import org.finos.legend.pure.m3.tools.GeneratorTools;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.serialization.grammar.M4Parser;
import org.finos.legend.pure.m4.statelistener.VoidM4StateListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoreInstanceGenerator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreInstanceGenerator.class);

    private final String outputDir;
    private final String resourcesDir;
    private final String pathToM3;


    public static void main(String[] args)
    {
        LOGGER.info("Starting " + CoreInstanceGenerator.class.getSimpleName() + " execution");

        String outputDir = args[0];
        String resourcesDir = args[1];
        String m3PlatformFilePath = args[2];

        LOGGER.info("Starting command line generation");

        new CoreInstanceGenerator(outputDir, resourcesDir, m3PlatformFilePath).generate();
    }

    public CoreInstanceGenerator(String outputDir, String resourcesDir, String pathToM3)
    {
        this.resourcesDir = resourcesDir;
        this.pathToM3 = pathToM3;
        this.outputDir = outputDir;
    }

    public void generate()
    {
        Path m3filePath = Paths.get(this.resourcesDir + this.pathToM3);

        Optional<Path> m3fileOldestModifiedFile;
        Optional<Path> outputOldestModifiedFile;
        try
        {
            m3fileOldestModifiedFile = FileTools.findOldestModified(m3filePath);
            outputOldestModifiedFile = FileTools.findOldestModified(Paths.get(this.outputDir));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        String compareSummaryMsg = "outputOldestModifiedFile=" + outputOldestModifiedFile
                + "; " + "outputOldestModifiedFileTime=" + (outputOldestModifiedFile.isPresent() ? outputOldestModifiedFile.get().toFile().lastModified() : null)
                + ";" + "m3fileOldestModifiedFile=" + outputOldestModifiedFile
                + "; " + "m3fileOldestModifiedFileTime=" + (m3fileOldestModifiedFile.isPresent() ? m3fileOldestModifiedFile.get().toFile().lastModified() : null);

        if (GeneratorTools.skipGenerationForNonStaleOutput()
                && (outputOldestModifiedFile.isPresent() && m3fileOldestModifiedFile.isPresent()
                && (outputOldestModifiedFile.get().toFile().lastModified() > m3fileOldestModifiedFile.get().toFile().lastModified()))
            )
        {
            LOGGER.info("No changes detected, output is not is not stale - skipping generation (" + compareSummaryMsg + ")");
        }
        else
        {
            if (outputOldestModifiedFile.isPresent())
            {
                GeneratorTools.assertRegenerationPermitted(compareSummaryMsg);
            }

            LOGGER.info("Changes detected - regenerating the output (" + compareSummaryMsg + ")");

            String m3Source;
            try
            {
                m3Source = new String(Files.readAllBytes(m3filePath), StandardCharsets.UTF_8);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            ModelRepository repository = new ModelRepository();
            new M4Parser().parse(m3Source, this.pathToM3, repository, new VoidM4StateListener());
            repository.validate(new VoidM4StateListener());
            M3ToJavaGenerator m3ToJavaGenerator = new M3ToJavaGenerator(this.outputDir, "M3", true);
            m3ToJavaGenerator.generate(repository, Sets.fixedSize.of(this.pathToM3), null);
        }
    }
}
