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

package org.finos.legend.pure.runtime.java.extension.external.json.shared;

import org.finos.legend.pure.runtime.java.extension.functions.shared.cipher.AESCipherUtil;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ClassConversion;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.Conversion;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.PropertySerialization;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

public abstract class JsonPropertySerialization<T> extends PropertySerialization<T, Object>
{
    static final Object CYCLE_DETECTED = new Object();
    private static final Predicate2<CoreInstance, Stereotype> STEREOTYPE_EQUAL_PREDICATE = new Predicate2<CoreInstance, Stereotype>()
    {
        @Override
        public boolean accept(CoreInstance each, Stereotype stereotype)
        {
            Stereotype eachStereotype = (Stereotype)each;
            return eachStereotype._value().equals(stereotype._value()) &&
                    PackageableElement.getUserPathForPackageableElement(eachStereotype._profile()).equals(PackageableElement.getUserPathForPackageableElement(stereotype._profile()));
        }

    };
    private Boolean hasCipherStereotype;
    private Boolean hasDecipherStereotype;

    public JsonPropertySerialization(AbstractProperty property, boolean isFromAssociation, Conversion<T, Object> conversion, Type type)
    {
        super(property, isFromAssociation, conversion, type);
    }

    Object serialize(T pureObject, JsonSerializationContext jsonSerializationContext)
    {
        if (jsonSerializationContext.isEnableEncryption())
        {
            return this.potentiallyEncryptSerializedValue(this.applyConversion(pureObject, jsonSerializationContext), jsonSerializationContext);
        }
        else if(jsonSerializationContext.isEnableDecryption())
        {
            return this.potentiallyDecryptSerializedValue(this.applyConversion(pureObject, jsonSerializationContext), jsonSerializationContext);
        }
        else
        {
            return this.applyConversion(pureObject, jsonSerializationContext);
        }
    }

    private Object applyConversion(T pureObject, JsonSerializationContext jsonSerializationContext)
    {
        if (this.type != null && this.type.equals(jsonSerializationContext.getProcessorSupport().type_TopType()))
        {
            /* processorSupport.type_TopType() returns Any - if property is defined in the graph as type Any we need to
             * resolve its concrete type from the instance at runtime as we do with generic type properties so invoking GenericAndAnyTypeConverter
             * which provides this functionality
             */
            return JsonGenericAndAnyTypeSerialization.JSON_GENERIC_AND_ANY_TYPE_SERIALIZATION.apply(pureObject, jsonSerializationContext);
        }
        else
        {
            if (this.type instanceof PrimitiveType)
            {
                return this.conversion.apply((T)jsonSerializationContext.extractPrimitiveValue(pureObject), jsonSerializationContext);
            }
            if (this.conversion instanceof ClassConversion && jsonSerializationContext.getVisitedInstances().contains(pureObject))
            {
                return CYCLE_DETECTED;
            }
            if (this.getName().equals(M3Properties.rawType) && this.conversion.pureTypeAsString().equals(M3Paths.Type))
            {
                return MetamodelSerializationOverrides.serializePackageableElement((Any)pureObject, false);
            }
            if (pureObject instanceof PureMap)
            {
                return PureMapSerializer.toJson(((PureMap)pureObject), jsonSerializationContext);
            }
            if (!(pureObject instanceof CoreInstance))
            {
                return pureObject; // pureObject as compiled mode object
            }
            return this.conversion.apply(pureObject, jsonSerializationContext);
        }
    }

    private Object potentiallyEncryptSerializedValue(Object serialized, JsonSerializationContext jsonSerializationContext)
    {
        if (this.hasEncryptStereotype(jsonSerializationContext.getEncryptionStereotypes()))
        {
            try
            {
                return new String(AESCipherUtil.encrypt(jsonSerializationContext.getEncryptionKey(), serialized.toString().getBytes()));
            }
            catch (Exception e)
            {
                throw new PureExecutionException("Failed to encrypt serialized json property: " + this.getName() + "\nwith key: " + jsonSerializationContext.getEncryptionKey(), e);
            }
        }
        return serialized;
    }

    private Object potentiallyDecryptSerializedValue(Object serialized, JsonSerializationContext jsonSerializationContext)
    {
        if (this.hasDecryptStereotype(jsonSerializationContext.getDecryptionStereotypes()))
        {
            try
            {
                return new String(AESCipherUtil.decrypt(jsonSerializationContext.getDecryptionKey(), serialized.toString().getBytes()));
            }
            catch (Exception e)
            {
                throw new PureExecutionException("Failed to decrypt serialized json property: " + this.getName() + "\nwith key: " + jsonSerializationContext.getDecryptionKey(), e);
            }
        }
        return serialized;
    }

    private boolean hasEncryptStereotype(final RichIterable<? extends CoreInstance> encryptionStereotypes)
    {
        if (this.hasCipherStereotype == null)
        {
            this.hasCipherStereotype = this.property._stereotypes().anySatisfy(new Predicate<Stereotype>()
            {
                @Override
                public boolean accept(Stereotype stereotype)
                {
                    return encryptionStereotypes.isEmpty() ? "Cipher".equals(stereotype._value()) : encryptionStereotypes.anySatisfyWith(STEREOTYPE_EQUAL_PREDICATE, stereotype);
                }
            });
        }
        return this.hasCipherStereotype;
    }

    private boolean hasDecryptStereotype(final RichIterable<? extends CoreInstance> decryptionStereotypes)
    {
        if (this.hasDecipherStereotype == null)
        {
            this.hasDecipherStereotype = this.property._stereotypes().anySatisfy(new Predicate<Stereotype>()
            {
                @Override
                public boolean accept(Stereotype stereotype)
                {
                    return decryptionStereotypes.isEmpty() ? "Decipher".equals(stereotype._value()) : decryptionStereotypes.anySatisfyWith(STEREOTYPE_EQUAL_PREDICATE, stereotype);
                }
            });
        }
        return this.hasDecipherStereotype;
    }
}
