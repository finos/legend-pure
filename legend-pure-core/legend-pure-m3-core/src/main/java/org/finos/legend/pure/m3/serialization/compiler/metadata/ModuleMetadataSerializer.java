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

package org.finos.legend.pure.m3.serialization.compiler.metadata;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.serialization.compiler.ExtensibleSerializer;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexer;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;

import java.util.Arrays;

public class ModuleMetadataSerializer extends ExtensibleSerializer<ModuleMetadataSerializerExtension>
{
    private static final long PURE_MODULE_MANIFEST_SIGNATURE = Long.parseLong("PureManifest", 36);
    private static final long PURE_MODULE_SOURCE_METADATA_SIGNATURE = Long.parseLong("PureSource", 36);
    private static final long PURE_MODULE_EXT_REFS_SIGNATURE = Long.parseLong("PureExtRefs", 36);
    private static final long PURE_ELEMENT_BACK_REFS_SIGNATURE = Long.parseLong("PureBackRefs", 36);

    private final StringIndexer stringIndexer;

    private ModuleMetadataSerializer(Iterable<? extends ModuleMetadataSerializerExtension> extensions, int defaultVersion, StringIndexer stringIndexer)
    {
        super(extensions, defaultVersion);
        this.stringIndexer = stringIndexer;
    }

    // Manifest

    public void serializeManifest(Writer writer, ModuleManifest manifest)
    {
        serializeManifest(writer, manifest, getDefaultExtension());
    }

    public void serializeManifest(Writer writer, ModuleManifest manifest, int version)
    {
        serializeManifest(writer, manifest, getExtension(version));
    }

    private void serializeManifest(Writer writer, ModuleManifest manifest, ModuleMetadataSerializerExtension extension)
    {
        writer.writeLong(PURE_MODULE_MANIFEST_SIGNATURE);
        writer.writeInt(extension.version());
        Writer stringIndexedWriter = this.stringIndexer.writeStringIndex(writer, collectStrings(manifest));
        extension.serializeManifest(stringIndexedWriter, manifest);
    }

    public ModuleManifest deserializeManifest(Reader reader)
    {
        long signature = reader.readLong();
        if (signature != PURE_MODULE_MANIFEST_SIGNATURE)
        {
            throw new IllegalArgumentException("Invalid file format: not a Legend module manifest file");
        }
        int version = reader.readInt();
        ModuleMetadataSerializerExtension extension = getExtension(version);
        Reader stringIndexedReader = this.stringIndexer.readStringIndex(reader);
        return extension.deserializeManifest(stringIndexedReader);
    }

    // Source metadata

    public void serializeSourceMetadata(Writer writer, ModuleSourceMetadata sourceMetadata)
    {
        serializeSourceMetadata(writer, sourceMetadata, getDefaultExtension());
    }

    public void serializeSourceMetadata(Writer writer, ModuleSourceMetadata sourceMetadata, int version)
    {
        serializeSourceMetadata(writer, sourceMetadata, getExtension(version));
    }

    private void serializeSourceMetadata(Writer writer, ModuleSourceMetadata sourceMetadata, ModuleMetadataSerializerExtension extension)
    {
        writer.writeLong(PURE_MODULE_SOURCE_METADATA_SIGNATURE);
        writer.writeInt(extension.version());
        Writer stringIndexedWriter = this.stringIndexer.writeStringIndex(writer, collectStrings(sourceMetadata));
        extension.serializeSourceMetadata(stringIndexedWriter, sourceMetadata);
    }

    public ModuleSourceMetadata deserializeSourceMetadata(Reader reader)
    {
        long signature = reader.readLong();
        if (signature != PURE_MODULE_SOURCE_METADATA_SIGNATURE)
        {
            throw new IllegalArgumentException("Invalid file format: not a Legend module source metadata file");
        }
        int version = reader.readInt();
        ModuleMetadataSerializerExtension extension = getExtension(version);
        Reader stringIndexedReader = this.stringIndexer.readStringIndex(reader);
        return extension.deserializeSourceMetadata(stringIndexedReader);
    }

    // External reference metadata

    public void serializeExternalReferenceMetadata(Writer writer, ModuleExternalReferenceMetadata externalReferenceMetadata)
    {
        serializeExternalReferenceMetadata(writer, externalReferenceMetadata, getDefaultExtension());
    }

    public void serializeExternalReferenceMetadata(Writer writer, ModuleExternalReferenceMetadata externalReferenceMetadata, int version)
    {
        serializeExternalReferenceMetadata(writer, externalReferenceMetadata, getExtension(version));
    }

    private void serializeExternalReferenceMetadata(Writer writer, ModuleExternalReferenceMetadata externalReferenceMetadata, ModuleMetadataSerializerExtension extension)
    {
        writer.writeLong(PURE_MODULE_EXT_REFS_SIGNATURE);
        writer.writeInt(extension.version());
        Writer stringIndexedWriter = this.stringIndexer.writeStringIndex(writer, collectStrings(externalReferenceMetadata));
        extension.serializeExternalReferenceMetadata(stringIndexedWriter, externalReferenceMetadata);
    }

    public ModuleExternalReferenceMetadata deserializeExternalReferenceMetadata(Reader reader)
    {
        long signature = reader.readLong();
        if (signature != PURE_MODULE_EXT_REFS_SIGNATURE)
        {
            throw new IllegalArgumentException("Invalid file format: not a Legend module external reference metadata file");
        }
        int version = reader.readInt();
        ModuleMetadataSerializerExtension extension = getExtension(version);
        Reader stringIndexedReader = this.stringIndexer.readStringIndex(reader);
        return extension.deserializeExternalReferenceMetadata(stringIndexedReader);
    }

    // Element back references

    public void serializeBackReferenceMetadata(Writer writer, ElementBackReferenceMetadata backReferenceMetadata)
    {
        serializeBackReferenceMetadata(writer, backReferenceMetadata, getDefaultExtension());
    }

    public void serializeBackReferenceMetadata(Writer writer, ElementBackReferenceMetadata backReferenceMetadata, int version)
    {
        serializeBackReferenceMetadata(writer, backReferenceMetadata, getExtension(version));
    }

    private void serializeBackReferenceMetadata(Writer writer, ElementBackReferenceMetadata backReferenceMetadata, ModuleMetadataSerializerExtension extension)
    {
        writer.writeLong(PURE_ELEMENT_BACK_REFS_SIGNATURE);
        writer.writeInt(extension.version());
        Writer stringIndexedWriter = this.stringIndexer.writeStringIndex(writer, collectStrings(backReferenceMetadata));
        extension.serializeBackReferenceMetadata(stringIndexedWriter, backReferenceMetadata);
    }

    public ElementBackReferenceMetadata deserializeBackReferenceMetadata(Reader reader)
    {
        long signature = reader.readLong();
        if (signature != PURE_ELEMENT_BACK_REFS_SIGNATURE)
        {
            throw new IllegalArgumentException("Invalid file format: not a Legend element back reference metadata file");
        }
        int version = reader.readInt();
        ModuleMetadataSerializerExtension extension = getExtension(version);
        Reader stringIndexedReader = this.stringIndexer.readStringIndex(reader);
        return extension.deserializeBackReferenceMetadata(stringIndexedReader);
    }

    // Helpers

    private static MutableSet<String> collectStrings(ModuleManifest manifest)
    {
        MutableSet<String> stringSet = Sets.mutable.empty();
        stringSet.add(manifest.getModuleName());
        manifest.forEachElement(element ->
        {
            stringSet.add(element.getPath());
            stringSet.add(element.getClassifierPath());
            stringSet.add(element.getSourceInformation().getSourceId());
        });
        return stringSet;
    }

    private static MutableSet<String> collectStrings(ModuleSourceMetadata sourceMetadata)
    {
        MutableSet<String> stringSet = Sets.mutable.empty();
        stringSet.add(sourceMetadata.getModuleName());
        sourceMetadata.forEachSource(source ->
        {
            stringSet.add(source.getSourceId());
            source.getSections().forEach(section ->
            {
                stringSet.add(section.getParser());
                stringSet.addAll(section.getElements().castToList());
            });
        });
        return stringSet;
    }

    private static MutableSet<String> collectStrings(ModuleExternalReferenceMetadata extRefs)
    {
        MutableSet<String> stringSet = Sets.mutable.empty();
        stringSet.add(extRefs.getModuleName());
        extRefs.getExternalReferences().forEach(eltExtRefs ->
        {
            stringSet.add(eltExtRefs.getElementPath());
            stringSet.addAll(eltExtRefs.getExternalReferences().castToList());
        });
        return stringSet;
    }

    private static MutableSet<String> collectStrings(ElementBackReferenceMetadata elementBackRefs)
    {
        MutableSet<String> stringSet = Sets.mutable.empty();
        stringSet.add(elementBackRefs.getElementPath());
        elementBackRefs.getInstanceBackReferenceMetadata().forEach(instBackRefs ->
        {
            stringSet.add(instBackRefs.getInstanceReferenceId());
            instBackRefs.getBackReferences().forEach(new BackReferenceConsumer()
            {
                @Override
                protected void accept(BackReference.Application application)
                {
                    stringSet.add(application.getFunctionExpression());
                }

                @Override
                protected void accept(BackReference.ModelElement modelElement)
                {
                    stringSet.add(modelElement.getElement());
                }

                @Override
                protected void accept(BackReference.PropertyFromAssociation propertyFromAssociation)
                {
                    stringSet.add(propertyFromAssociation.getProperty());
                }

                @Override
                protected void accept(BackReference.QualifiedPropertyFromAssociation qualifiedPropertyFromAssociation)
                {
                    stringSet.add(qualifiedPropertyFromAssociation.getQualifiedProperty());
                }

                @Override
                protected void accept(BackReference.ReferenceUsage referenceUsage)
                {
                    stringSet.add(referenceUsage.getOwner());
                    stringSet.add(referenceUsage.getProperty());
                    SourceInformation sourceInfo = referenceUsage.getSourceInformation();
                    if (sourceInfo != null)
                    {
                        stringSet.add(sourceInfo.getSourceId());
                    }
                }

                @Override
                protected void accept(BackReference.Specialization specialization)
                {
                    stringSet.add(specialization.getGeneralization());
                }
            });
        });
        return stringSet;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder extends AbstractBuilder<ModuleMetadataSerializerExtension, ModuleMetadataSerializer>
    {
        private StringIndexer stringIndexer;

        private Builder()
        {
        }

        public Builder withExtension(ModuleMetadataSerializerExtension extension)
        {
            addExtension(extension);
            return this;
        }

        public Builder withExtensions(Iterable<? extends ModuleMetadataSerializerExtension> extensions)
        {
            addExtensions(extensions);
            return this;
        }

        public Builder withExtensions(ModuleMetadataSerializerExtension... extensions)
        {
            return withExtensions(Arrays.asList(extensions));
        }

        public Builder withLoadedExtensions(ClassLoader classLoader)
        {
            loadExtensions(classLoader);
            return this;
        }

        public Builder withLoadedExtensions()
        {
            loadExtensions();
            return this;
        }

        public Builder withDefaultVersion(int defaultVersion)
        {
            setDefaultVersion(defaultVersion);
            return this;
        }

        public Builder withStringIndexer(StringIndexer stringIndexer)
        {
            this.stringIndexer = stringIndexer;
            return this;
        }

        @Override
        protected ModuleMetadataSerializer build(Iterable<ModuleMetadataSerializerExtension> extensions, int defaultVersion)
        {
            // if no string indexer has been specified, use the default
            return new ModuleMetadataSerializer(extensions, defaultVersion, (this.stringIndexer == null) ? StringIndexer.defaultStringIndexer() : this.stringIndexer);
        }

        @Override
        protected Class<ModuleMetadataSerializerExtension> getExtensionClass()
        {
            return ModuleMetadataSerializerExtension.class;
        }
    }
}
