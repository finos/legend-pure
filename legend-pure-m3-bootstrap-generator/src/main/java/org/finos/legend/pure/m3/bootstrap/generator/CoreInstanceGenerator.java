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
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.serialization.grammar.M4Parser;
import org.finos.legend.pure.m4.statelistener.VoidM4StateListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CoreInstanceGenerator
{
    private final String outputDir;
    private final String resourcesDir;
    private final String pathToM3;


    public static void main(String[] args)
    {
        String outputDir = args[0];
        String resourcesDir = args[1];
        String m3PlatformFilePath = args[2];
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

        String m3Source;
        try
        {
            m3Source = new String(Files.readAllBytes(Paths.get(this.resourcesDir + this.pathToM3)), StandardCharsets.UTF_8);
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
