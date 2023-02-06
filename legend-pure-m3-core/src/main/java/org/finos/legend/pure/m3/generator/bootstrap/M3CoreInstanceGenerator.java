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
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.utility.StringIterate;
import org.finos.legend.pure.m3.bootstrap.generator.M3ToJavaGenerator;
import org.finos.legend.pure.m3.bootstrap.generator.StubDef;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.helper.PropertyTypeHelper;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.nio.file.Paths;

public class M3CoreInstanceGenerator
{
    public static void main(String[] args)
    {
        String outputDir = args[0];
        String factoryNamePrefix = args[1];
        String fileNameStr = args[2];
        String fileNameStartsWith = args.length >= 4 ? args[3] : null;

        SetIterable<String> filePaths = fileNameStr == null ? Sets.mutable.empty() : StringIterate.tokensToSet(fileNameStr.replaceAll("\\r", "").replaceAll("\\n", "").replaceAll(" ", ""), ",");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        CodeRepositorySet.Builder builder = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(classLoader, true));
        RichIterable<CodeRepository> repositories = builder.build().getRepositories();
        PureRuntime runtime = new PureRuntimeBuilder(new PureCodeStorage(Paths.get(""), new ClassLoaderCodeStorage(repositories))).setTransactionalByDefault(false).build();

        ModelRepository repository = runtime.getModelRepository();
        runtime.loadAndCompileCore();
        runtime.loadAndCompileSystem();

        M3ToJavaGenerator m3ToJavaGenerator = generator(outputDir, factoryNamePrefix, repository);
        m3ToJavaGenerator.generate(repository, filePaths, fileNameStartsWith);
    }

    public static M3ToJavaGenerator generator(String outputFir, String factoryNamePrefix, ModelRepository repository)
    {
        MutableMap<String, StubDef> STUB_DEFS = Lists.immutable.with(
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
                StubDef.build("JoinType", "GrammarInfoStub")).injectInto(Maps.mutable.empty(), (map, stubDef) ->
        {
            map.put(stubDef.getClassName(), stubDef);
            return map;
        });
        return new M3ToJavaGenerator(outputFir, factoryNamePrefix, false, new PropertyTypeResolverUsingInheritence(new M3ProcessorSupport(new Context(), repository)), STUB_DEFS);
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
