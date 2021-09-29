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

package org.finos.legend.pure.runtime.java.extension.external.json.compiled;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.BaseCompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.EqualJsonStrings;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.Escape;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.FromJson;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.FromJsonDeprecated;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.ParseJSON;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.ToJsonBeta;

public class JsonExtensionCompiled extends BaseCompiledExtension
{
    public JsonExtensionCompiled()
    {
        super(
                Lists.mutable.with(
                        new EqualJsonStrings(),
                        new Escape(),
                        new FromJson(),
                        new FromJsonDeprecated(),
                        new ParseJSON(),
                        new ToJsonBeta()
                ),
                Lists.mutable.with(StringJavaSource.newStringJavaSource("org.finos.legend.pure.runtime.java.extension.external.json.compiled", "JsonGen",
                        "package org.finos.legend.pure.runtime.java.extension.external.json.compiled;\n" +
                                "\n" +
                                "import org.finos.legend.pure.generated.Root_meta_json_JSONArray;\n" +
                                "import org.finos.legend.pure.generated.Root_meta_json_JSONDeserializationConfig;\n" +
                                "import org.finos.legend.pure.generated.Root_meta_json_JSONSerializationConfig;\n" +
                                "import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;\n" +
                                "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;\n" +
                                "import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.JsonParserHelper;\n" +
                                "import org.eclipse.collections.api.RichIterable;\n" +
                                "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ConstraintsOverride;\n" +
                                "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n" +
                                "import org.finos.legend.pure.m4.coreinstance.CoreInstance;\n" +
                                "import org.finos.legend.pure.m4.coreinstance.SourceInformation;\n" +
                                "\n" +
                                "import static org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure.handleValidation;\n" +
                                "\n" +
                                "\n" +
                                "public class JsonGen\n" +
                                "{\n" +
                                "    @Deprecated\n" +
                                "    public static <T> T fromJsonDeprecated(String json, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<T> clazz, Root_meta_json_JSONDeserializationConfig config, SourceInformation si, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        java.lang.Class c = ((CompiledExecutionSupport)es).getClassCache().getIfAbsentPutInterfaceForType(clazz);\n" +
                                "        T obj = (T)JsonParserHelper.fromJson(json, c, \"\", \"\", ((CompiledExecutionSupport)es).getMetadataAccessor(), ((CompiledExecutionSupport)es).getClassLoader(), si, config._typeKeyName(), config._failOnUnknownProperties(), config._constraintsHandler(), es);\n" +
                                "        return (T)handleValidation(true, obj, si, es);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static String toJson(Object pureObject, Root_meta_json_JSONSerializationConfig jsonConfig, final SourceInformation si, final ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return toJson(CompiledSupport.toPureCollection(pureObject), jsonConfig, si, es);\n" +
                                "    }\n" +
                                "\n" +
                                "    private static String toJson(RichIterable<?> pureObject, Root_meta_json_JSONSerializationConfig jsonConfig, final SourceInformation si, final ExecutionSupport es)\n" +
                                "    {\n" +
                                "        String typeKeyName = jsonConfig._typeKeyName();\n" +
                                "        boolean includeType = jsonConfig._includeType() != null ? jsonConfig._includeType() : false;\n" +
                                "        boolean fullyQualifiedTypePath = jsonConfig._fullyQualifiedTypePath() != null ? jsonConfig._fullyQualifiedTypePath() : false;\n" +
                                "        boolean serializeQualifiedProperties = jsonConfig._serializeQualifiedProperties() != null ? jsonConfig._serializeQualifiedProperties() : false;\n" +
                                "        String dateTimeFormat = jsonConfig._dateTimeFormat();\n" +
                                "        boolean serializePackageableElementName = jsonConfig._serializePackageableElementName() != null ? jsonConfig._serializePackageableElementName() : false;\n" +
                                "        boolean removePropertiesWithEmptyValues = jsonConfig._removePropertiesWithEmptyValues() != null ? jsonConfig._removePropertiesWithEmptyValues() : false;\n" +
                                "        boolean serializeMultiplicityAsNumber = jsonConfig._serializeMultiplicityAsNumber() != null ? jsonConfig._serializeMultiplicityAsNumber() : false;\n" +
                                "        String encryptionKey = jsonConfig._encryptionKey();\n" +
                                "        String decryptionKey = jsonConfig._decryptionKey();\n" +
                                "        RichIterable<? extends CoreInstance> encryptionStereotypes = jsonConfig._encryptionStereotypes();\n" +
                                "        RichIterable<? extends CoreInstance> decryptionStereotypes = jsonConfig._decryptionStereotypes();\n" +
                                "\n" +
                                "        return org.finos.legend.pure.runtime.java.extension.external.json.compiled.JsonNativeImplementation._toJson(pureObject, si, es, typeKeyName, includeType, fullyQualifiedTypePath, serializeQualifiedProperties, dateTimeFormat, serializePackageableElementName, removePropertiesWithEmptyValues, serializeMultiplicityAsNumber, encryptionKey, decryptionKey, encryptionStereotypes, decryptionStereotypes);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static <T> T fromJson(String json, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<T> clazz, Root_meta_json_JSONDeserializationConfig config, final SourceInformation si, final ExecutionSupport es)\n" +
                                "    {\n" +
                                "        final ConstraintsOverride constraintsHandler = config._constraintsHandler();\n" +
                                "        final RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<? extends java.lang.String, ? extends java.lang.String>> _typeLookup = config._typeLookup();\n" +
                                "        return org.finos.legend.pure.runtime.java.extension.external.json.compiled.JsonNativeImplementation._fromJson(json, clazz, config._typeKeyName(), config._failOnUnknownProperties(), si, es, constraintsHandler, _typeLookup);\n" +
                                "    }\n" +
                                "\n" +
                                "}\n")),
                Lists.mutable.empty(),
                Lists.mutable.empty());
    }

    public static CompiledExtension extension()
    {
        return new JsonExtensionCompiled();
    }
}
