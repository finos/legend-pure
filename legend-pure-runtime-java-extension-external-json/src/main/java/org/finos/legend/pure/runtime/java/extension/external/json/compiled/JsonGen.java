package org.finos.legend.pure.runtime.java.extension.external.json.compiled;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.generated.Root_meta_json_JSONDeserializationConfig;
import org.finos.legend.pure.generated.Root_meta_json_JSONSerializationConfig;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ConstraintsOverride;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.JsonParserHelper;

import static org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure.handleValidation;

public class JsonGen
{
    @Deprecated
    public static <T> T fromJsonDeprecated(String json, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<T> clazz, Root_meta_json_JSONDeserializationConfig config, SourceInformation si, ExecutionSupport es)
    {
        java.lang.Class c = ((CompiledExecutionSupport) es).getClassCache().getIfAbsentPutInterfaceForType(clazz);
        T obj = (T) JsonParserHelper.fromJson(json, c, "", "", ((CompiledExecutionSupport) es).getMetadataAccessor(), ((CompiledExecutionSupport) es).getClassLoader(), si, config._typeKeyName(), config._failOnUnknownProperties(), config._constraintsHandler(), es);
        return (T) handleValidation(true, obj, si, es);
    }

    public static String toJson(Object pureObject, Root_meta_json_JSONSerializationConfig jsonConfig, final SourceInformation si, final ExecutionSupport es)
    {
        return toJson(CompiledSupport.toPureCollection(pureObject), jsonConfig, si, es);
    }

    private static String toJson(RichIterable<?> pureObject, Root_meta_json_JSONSerializationConfig jsonConfig, final SourceInformation si, final ExecutionSupport es)
    {
        String typeKeyName = jsonConfig._typeKeyName();
        boolean includeType = jsonConfig._includeType() != null ? jsonConfig._includeType() : false;
        boolean fullyQualifiedTypePath = jsonConfig._fullyQualifiedTypePath() != null ? jsonConfig._fullyQualifiedTypePath() : false;
        boolean serializeQualifiedProperties = jsonConfig._serializeQualifiedProperties() != null ? jsonConfig._serializeQualifiedProperties() : false;
        String dateTimeFormat = jsonConfig._dateTimeFormat();
        boolean serializePackageableElementName = jsonConfig._serializePackageableElementName() != null ? jsonConfig._serializePackageableElementName() : false;
        boolean removePropertiesWithEmptyValues = jsonConfig._removePropertiesWithEmptyValues() != null ? jsonConfig._removePropertiesWithEmptyValues() : false;
        boolean serializeMultiplicityAsNumber = jsonConfig._serializeMultiplicityAsNumber() != null ? jsonConfig._serializeMultiplicityAsNumber() : false;
        String encryptionKey = jsonConfig._encryptionKey();
        String decryptionKey = jsonConfig._decryptionKey();
        RichIterable<? extends CoreInstance> encryptionStereotypes = jsonConfig._encryptionStereotypes();
        RichIterable<? extends CoreInstance> decryptionStereotypes = jsonConfig._decryptionStereotypes();

        return org.finos.legend.pure.runtime.java.extension.external.json.compiled.JsonNativeImplementation._toJson(pureObject, si, es, typeKeyName, includeType, fullyQualifiedTypePath, serializeQualifiedProperties, dateTimeFormat, serializePackageableElementName, removePropertiesWithEmptyValues, serializeMultiplicityAsNumber, encryptionKey, decryptionKey, encryptionStereotypes, decryptionStereotypes);
    }

    public static <T> T fromJson(String json, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<T> clazz, Root_meta_json_JSONDeserializationConfig config, final SourceInformation si, final ExecutionSupport es)
    {
        final ConstraintsOverride constraintsHandler = config._constraintsHandler();
        final RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<? extends java.lang.String, ? extends java.lang.String>> _typeLookup = config._typeLookup();
        return org.finos.legend.pure.runtime.java.extension.external.json.compiled.JsonNativeImplementation._fromJson(json, clazz, config._typeKeyName(), config._failOnUnknownProperties(), si, es, constraintsHandler, _typeLookup);
    }

}
