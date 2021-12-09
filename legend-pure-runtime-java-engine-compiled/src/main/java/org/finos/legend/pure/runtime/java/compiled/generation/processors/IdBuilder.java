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

package org.finos.legend.pure.runtime.java.compiled.generation.processors;

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Annotation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageTools;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.PrimitiveCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.ValCoreInstance;

public class IdBuilder
{
    private final String defaultIdPrefix;
    private final ProcessorSupport processorSupport;

    private IdBuilder(String defaultIdPrefix, ProcessorSupport processorSupport)
    {
        this.defaultIdPrefix = defaultIdPrefix;
        this.processorSupport = processorSupport;
    }

    public String buildId(CoreInstance instance)
    {
        if (instance instanceof Any)
        {
            return buildId((Any) instance);
        }

        if (isPrimitiveValue(instance))
        {
            return buildIdForPrimitiveValue(instance);
        }
        if (this.processorSupport.instance_instanceOf(instance, M3Paths.Enum))
        {
            return buildIdForEnumValue(instance);
        }
        if (this.processorSupport.instance_instanceOf(instance, M3Paths.AbstractProperty))
        {
            return buildIdForAbstractProperty(instance);
        }
        if (this.processorSupport.instance_instanceOf(instance, M3Paths.LambdaFunction))
        {
            return buildIdForLambdaFunction(instance);
        }
        if (this.processorSupport.instance_instanceOf(instance, M3Paths.Annotation))
        {
            return buildIdForAnnotation(instance);
        }
        if (this.processorSupport.instance_instanceOf(instance, M3Paths.PackageableElement))
        {
            return buildIdForPackageableElement(instance);
        }
        return buildDefaultId(instance);
    }

    private String buildId(Any instance)
    {
        if (instance instanceof Enum)
        {
            return buildIdForEnumValue(instance);
        }
        if (instance instanceof AbstractProperty)
        {
            return buildIdForAbstractProperty(instance);
        }
        if (instance instanceof LambdaFunction)
        {
            return buildIdForLambdaFunction(instance);
        }
        if (instance instanceof Annotation)
        {
            return buildIdForAnnotation(instance);
        }
        if (instance instanceof PackageableElement)
        {
            return buildIdForPackageableElement(instance);
        }
        return buildDefaultId(instance);
    }

    // Primitive values

    private boolean isPrimitiveValue(CoreInstance instance)
    {
        if ((instance instanceof PrimitiveCoreInstance) || (instance instanceof ValCoreInstance))
        {
            return true;
        }
        if (instance instanceof Any)
        {
            return false;
        }

        CoreInstance classifier = this.processorSupport.getClassifier(instance);
        return (classifier instanceof PrimitiveType) || this.processorSupport.instance_instanceOf(classifier, M3Paths.PrimitiveType);
    }

    private String buildIdForPrimitiveValue(CoreInstance instance)
    {
        return instance.getName();
    }

    // Enum value

    private String buildIdForEnumValue(CoreInstance instance)
    {
        return instance.getName();
    }

    // AbstractProperty

    private String buildIdForAbstractProperty(CoreInstance property)
    {
        StringBuilder builder = new StringBuilder();
        org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.writeUserPathForPackageableElement(builder, property.getValueForMetaPropertyToOne(M3Properties.owner));
        return builder.append('.').append(property.getName()).toString();
    }

    // LambdaFunction

    private String buildIdForLambdaFunction(CoreInstance lambda)
    {
        return lambda.getName();
    }

    // Annotation

    private String buildIdForAnnotation(CoreInstance annotation)
    {
        StringBuilder builder = new StringBuilder();
        org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.writeUserPathForPackageableElement(builder, annotation.getValueForMetaPropertyToOne(M3Properties.profile));
        return builder.append('.').append(annotation.getName()).toString();
    }

    // PackageableElement

    private String buildIdForPackageableElement(CoreInstance instance)
    {
        return org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getSystemPathForPackageableElement(instance);
    }

    // Default

    private String buildDefaultId(CoreInstance instance)
    {
        int syntheticId = instance.getSyntheticId();
        return (this.defaultIdPrefix == null) ? Integer.toString(syntheticId) : (this.defaultIdPrefix + syntheticId);
    }

    // Factory methods

    public static IdBuilder newIdBuilder(String defaultIdPrefix, ProcessorSupport processorSupport)
    {
        return new IdBuilder(defaultIdPrefix, processorSupport);
    }

    public static IdBuilder newIdBuilder(ProcessorSupport processorSupport)
    {
        return newIdBuilder(null, processorSupport);
    }

    @Deprecated
    public static String buildId(CoreInstance coreInstance, ProcessorSupport processorSupport)
    {
        return newIdBuilder(null, processorSupport).buildId(coreInstance);
    }

    public static String sourceToId(SourceInformation sourceInformation)
    {
        String sourceId = sourceInformation.getSourceId();
        if (Source.isInMemory(sourceId))
        {
            return CodeStorageTools.hasPureFileExtension(sourceId) ? sourceId.substring(0, sourceId.length() - CodeStorage.PURE_FILE_EXTENSION.length()) : sourceId;
        }

        int endIndex = CodeStorageTools.hasPureFileExtension(sourceId) ? (sourceId.length() - CodeStorage.PURE_FILE_EXTENSION.length()) : sourceId.length();
        return sourceId.substring(1, endIndex).replace('/', '_');
    }
}
