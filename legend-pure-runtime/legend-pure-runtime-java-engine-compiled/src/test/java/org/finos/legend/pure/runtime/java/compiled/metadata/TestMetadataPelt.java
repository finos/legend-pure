// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.metadata;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.compiler.PureCompilerSerializer;
import org.finos.legend.pure.m3.serialization.compiler.element.ConcreteElementSerializer;
import org.finos.legend.pure.m3.serialization.compiler.file.FilePathProvider;
import org.finos.legend.pure.m3.serialization.compiler.file.FileSerializer;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataGenerator;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataSerializer;
import org.finos.legend.pure.m3.serialization.compiler.reference.AbstractReferenceTest;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProvider;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProviders;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.EnumProcessor;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class TestMetadataPelt extends AbstractReferenceTest
{
    @ClassRule
    public static TemporaryFolder TMP = new TemporaryFolder();

    private static Path serializationDir;
    private static SetIterable<String> repositories;
    private static ReferenceIdProviders referenceIdProviders;

    @BeforeClass
    public static void serialize() throws IOException
    {
        serializationDir = TMP.newFolder("files").toPath();

        referenceIdProviders = ReferenceIdProviders.builder().withProcessorSupport(processorSupport).withAvailableExtensions().build();
        FileSerializer fileSerializer = FileSerializer.builder()
                .withFilePathProvider(FilePathProvider.builder().withLoadedExtensions().build())
                .withSerializers(ConcreteElementSerializer.builder(processorSupport).withLoadedExtensions().withReferenceIdProviders(referenceIdProviders).build(), ModuleMetadataSerializer.builder().withLoadedExtensions().build())
                .build();

        PureCompilerSerializer.builder()
                .withFileSerializer(fileSerializer)
                .withModuleMetadataGenerator(ModuleMetadataGenerator.fromProcessorSupport(processorSupport))
                .withProcessorSupport(processorSupport)
                .build()
                .serializeAll(serializationDir);

        repositories = runtime.getCodeStorage().getAllRepositories().collect(CodeRepository::getName, Sets.mutable.empty()).asUnmodifiable();
    }

    @Test
    public void testMetadataFromDirectory()
    {
        testMetadata(MetadataPelt.builder()
                .withClassLoader(Thread.currentThread().getContextClassLoader())
                .withDirectory(serializationDir)
                .withRepositories(repositories)
                .build());
    }

    @Test
    public void testMetadataFromClassLoader()
    {
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{serializationDir.toUri().toURL()}, Thread.currentThread().getContextClassLoader()))
        {
            testMetadata(MetadataPelt.builder()
                    .withClassLoader(classLoader)
                    .withRepositories(repositories)
                    .build());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private void testMetadata(MetadataPelt metadata)
    {
        ReferenceIdProvider refIdProvider = referenceIdProviders.provider();
        GraphNodeIterable.fromModelRepository(repository)
                .select(refIdProvider::hasReferenceId)
                .forEach(instance ->
                {
                    String id = refIdProvider.getReferenceId(instance);
                    String classifierPath = PackageableElement.getUserPathForPackageableElement(instance.getClassifier());
                    CoreInstance loaded = metadata.getMetadata(classifierPath, id);
                    Assert.assertSame(id, loaded, metadata.getInstance(id));

                    Assert.assertEquals(id, classifierPath, PackageableElement.getUserPathForPackageableElement(loaded.getClassifier()));

                    String expectedJavaClassName = getExpectedJavaClassName(instance, classifierPath);
                    Assert.assertEquals(id, expectedJavaClassName, loaded.getClass().getName());
                });
    }

    private String getExpectedJavaClassName(CoreInstance instance, String classifierPath)
    {
        if (M3Paths.Package.equals(classifierPath) && (instance.getSourceInformation() == null))
        {
            return JavaPackageAndImportBuilder.buildLazyVirtualPackageClassReference();
        }
        if (instance instanceof Enum)
        {
            return JavaPackageAndImportBuilder.rootPackage() + "." + EnumProcessor.ENUM_LAZY_COMPONENT_CLASS_NAME;
        }
        return (instance instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) ?
               JavaPackageAndImportBuilder.buildLazyConcreteElementClassReferenceFromUserPath(classifierPath) :
               JavaPackageAndImportBuilder.buildLazyComponentInstanceClassReferenceFromUserPath(classifierPath);
    }
}
