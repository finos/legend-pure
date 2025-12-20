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

package org.finos.legend.pure.m3.generator.bootstrap;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.finos.legend.pure.m3.bootstrap.generator.M3ToJavaGenerator;
import org.finos.legend.pure.m3.bootstrap.generator.StubDef;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.helper.PropertyTypeHelper;
import org.finos.legend.pure.m3.coreinstance.lazy.generator.M3LazyCoreInstanceGenerator;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.tools.FileTools;
import org.finos.legend.pure.m3.tools.GeneratorTools;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class M3CoreInstanceGenerator
{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(M3CoreInstanceGenerator.class);

    public static void main(String[] args)
    {
        LOGGER.info("Starting " + M3CoreInstanceGenerator.class.getSimpleName() + " execution");

        String outputDir = args[0];
        String factoryNamePrefix = args[1];
        String fileNameStr = args[2];
        String fileNameStartsWith = args.length >= 4 ? args[3] : null;

        SetIterable<String> filePaths = fileNameStr == null ? Sets.immutable.empty() : Sets.mutable.with(fileNameStr.split("\\s*+,\\s*+"));
        generate(outputDir, factoryNamePrefix, filePaths, fileNameStartsWith);
    }

    public static void generate(String outputDir, String factoryNamePrefix, SetIterable<String> filePaths, String fileNamePrefix)
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        CodeRepositorySet.Builder builder = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(classLoader, true));
        RichIterable<CodeRepository> repositories = builder.build().getRepositories();
        CompositeCodeStorage codeStorage = new CompositeCodeStorage(new ClassLoaderCodeStorage(classLoader, repositories));

        long codeLastModified = codeStorage.lastModified();
        Optional<Path> outputEarliestModified;
        try
        {
            outputEarliestModified = FileTools.findOldestModified(Paths.get(outputDir));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        String modifiedMsgSummary = "outputEarliestModified=" + outputEarliestModified + "; " + (outputEarliestModified.isPresent() ? outputEarliestModified.get().toFile().lastModified() : null) + ";" + "codeLastModified=" + codeLastModified;

        if (GeneratorTools.skipGenerationForNonStaleOutput() && (outputEarliestModified.isPresent() && outputEarliestModified.get().toFile().lastModified() > codeLastModified))
        {
            LOGGER.info("No changes detected, output is not is not stale - skipping generation (" + modifiedMsgSummary + ")");
        }
        else
        {
            if (outputEarliestModified.isPresent())
            {
                GeneratorTools.assertRegenerationPermitted(modifiedMsgSummary);
            }

            LOGGER.info("Changes detected - regenerating the output (" + modifiedMsgSummary + ")");

            PureRuntime runtime = new PureRuntimeBuilder(codeStorage).setTransactionalByDefault(false).build();

            ModelRepository repository = runtime.getModelRepository();
            runtime.loadAndCompileCore();
            runtime.loadAndCompileSystem();

            LOGGER.info("Generating to: " + filePaths);

            M3ToJavaGenerator m3ToJavaGenerator = generator(outputDir, factoryNamePrefix, repository);
            m3ToJavaGenerator.generate(repository, filePaths, fileNamePrefix);
            M3LazyCoreInstanceGenerator.generate(Paths.get(outputDir), filePaths, fileNamePrefix, runtime.getProcessorSupport());
        }
    }

    public static M3ToJavaGenerator generator(String outputDir, String factoryNamePrefix, ModelRepository repository)
    {
        MutableMap<String, StubDef> additionalStubDefs = ArrayAdapter.adapt(
                StubDef.build("Mapping", "ImportStub"),
                StubDef.build("SetBasedStore", "ImportStub"),
                StubDef.build("Store", "ImportStub"),
                StubDef.build("SetRelation", "GrammarInfoStub"),
                StubDef.build("SetColumn", "GrammarInfoStub"),

                StubDef.build("Database", "ImportStub"),
                StubDef.build("Csv", "ImportStub"),
                StubDef.build("State", "GrammarInfoStub"),
                StubDef.build("ConfidentialityClassification", "GrammarInfoStub"),
                StubDef.build("ConfidentialitySubClassification", "GrammarInfoStub"),
                StubDef.build("DeDupeType", "GrammarInfoStub"),
                StubDef.build("ValueTransformer", "GrammarInfoStub"),
                StubDef.build("TypeView", "GrammarInfoStub"),
                StubDef.build("JoinType", "GrammarInfoStub")
        ).groupByUniqueKey(StubDef::getClassName);
        return new M3ToJavaGenerator(outputDir, factoryNamePrefix, false, new PropertyTypeResolverUsingInheritence(new M3ProcessorSupport(new Context(), repository)), additionalStubDefs);
    }

    static class PropertyTypeResolverUsingInheritence implements M3ToJavaGenerator.PropertyTypeResolver
    {
        private final ProcessorSupport processorSupport;

        PropertyTypeResolverUsingInheritence(ProcessorSupport processorSupport)
        {
            this.processorSupport = processorSupport;
        }

        @Override
        public CoreInstance getPropertyReturnType(CoreInstance classGenericType, CoreInstance property)
        {
            return PropertyTypeHelper.getPropertyResolvedReturnType(classGenericType, property, this.processorSupport);
        }

        @Override
        public CoreInstance getClassGenericType(CoreInstance coreInstance)
        {
            CoreInstance genericType = Type.wrapGenericType(coreInstance, null, this.processorSupport);
            Instance.addValueToProperty(genericType, M3Properties.typeArguments, coreInstance.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToOne(M3Properties.typeArguments).getValueForMetaPropertyToMany(M3Properties.typeArguments), this.processorSupport);
            Instance.addValueToProperty(genericType, M3Properties.multiplicityArguments, coreInstance.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToOne(M3Properties.typeArguments).getValueForMetaPropertyToMany(M3Properties.multiplicityArguments), this.processorSupport);
            return genericType;
        }
    }
}
