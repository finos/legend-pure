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

package org.finos.legend.pure.m3.serialization.compiler.metadata.v1;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReference;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReferenceConsumer;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BaseModuleMetadataSerializerExtension;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ConcreteElementMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ElementBackReferenceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ElementBackReferenceMetadata.InstanceBackReferenceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ElementExternalReferenceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.FunctionsByName;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleBackReferenceIndex;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleExternalReferenceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleFunctionNameMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleManifest;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleSourceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.SourceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.SourceSectionMetadata;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexer;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;

import java.io.InputStream;
import java.io.OutputStream;

public class ModuleMetadataSerializerV1 extends BaseModuleMetadataSerializerExtension
{
    private static final int BACK_REF_TYPE_MASK = 0b1110_0000;
    private static final int BACK_REF_APPLICATION = 0b0000_0000;
    private static final int BACK_REF_MODEL_ELEMENT = 0b1000_0000;
    private static final int BACK_REF_PROP_FROM_ASSOC = 0b0100_0000;
    private static final int BACK_REF_QUAL_PROP_FROM_ASSOC = 0b0010_0000;
    private static final int BACK_REF_REF_USAGE = 0b1100_0000;
    private static final int BACK_REF_SPEC = 0b1010_0000;

    private static final int INT_WIDTH_MASK = 0b0000_0011;
    private static final int BYTE_INT = 0b0000_0000;
    private static final int SHORT_INT = 0b0000_0001;
    private static final int INT_INT = 0b0000_0010;

    private static final int BOOLEAN_MASK = 0b0000_1000;
    private static final int BOOLEAN_TRUE = 0b0000_1000;
    private static final int BOOLEAN_FALSE = 0b0000_0000;

    @Override
    public int version()
    {
        return 1;
    }

    @Override
    public void serializeManifest(OutputStream stream, ModuleManifest manifest, StringIndexer stringIndexer)
    {
        try (Writer writer = BinaryWriters.newBinaryWriter(stream, false))
        {
            Writer stringIndexedWriter = stringIndexer.writeStringIndex(writer, collectStrings(manifest));
            stringIndexedWriter.writeString(manifest.getModuleName());
            ImmutableList<ConcreteElementMetadata> elements = manifest.getElements();
            stringIndexedWriter.writeInt(elements.size());
            elements.forEach(element -> writeElement(stringIndexedWriter, element));
        }
    }

    @Override
    public ModuleManifest deserializeManifest(InputStream stream, StringIndexer stringIndexer)
    {
        try (Reader reader = BinaryReaders.newBinaryReader(stream, false))
        {
            Reader stringIndexedReader = stringIndexer.readStringIndex(reader);
            String moduleName = stringIndexedReader.readString();
            int elementCount = stringIndexedReader.readInt();
            ModuleManifest.Builder builder = ModuleManifest.builder(0, elementCount).withModuleName(moduleName);
            for (int i = 0; i < elementCount; i++)
            {
                builder.addElement(readElement(stringIndexedReader));
            }
            return builder.build();
        }
    }

    @Override
    public void serializeSourceMetadata(OutputStream stream, ModuleSourceMetadata sourceMetadata, StringIndexer stringIndexer)
    {
        try (Writer writer = BinaryWriters.newBinaryWriter(stream, false))
        {
            Writer stringIndexedWriter = stringIndexer.writeStringIndex(writer, collectStrings(sourceMetadata));
            stringIndexedWriter.writeString(sourceMetadata.getModuleName());
            ImmutableList<SourceMetadata> sources = sourceMetadata.getSources();
            stringIndexedWriter.writeInt(sources.size());
            sources.forEach(source -> writeSource(stringIndexedWriter, source));
        }
    }

    @Override
    public ModuleSourceMetadata deserializeSourceMetadata(InputStream stream, StringIndexer stringIndexer)
    {
        try (Reader reader = BinaryReaders.newBinaryReader(stream, false))
        {
            Reader stringIndexedReader = stringIndexer.readStringIndex(reader);
            String moduleName = stringIndexedReader.readString();
            int sourceCount = stringIndexedReader.readInt();
            ModuleSourceMetadata.Builder builder = ModuleSourceMetadata.builder(sourceCount).withModuleName(moduleName);
            for (int i = 0; i < sourceCount; i++)
            {
                builder.addSource(readSource(stringIndexedReader));
            }
            return builder.build();
        }
    }

    @Override
    public void serializeExternalReferenceMetadata(OutputStream stream, ModuleExternalReferenceMetadata externalReferenceMetadata, StringIndexer stringIndexer)
    {
        try (Writer writer = BinaryWriters.newBinaryWriter(stream, false))
        {
            Writer stringIndexedWriter = stringIndexer.writeStringIndex(writer, collectStrings(externalReferenceMetadata));
            stringIndexedWriter.writeString(externalReferenceMetadata.getModuleName());
            stringIndexedWriter.writeInt(externalReferenceMetadata.getReferenceIdVersion());

            ImmutableList<ElementExternalReferenceMetadata> elementExternalReferences = externalReferenceMetadata.getExternalReferences();
            stringIndexedWriter.writeInt(elementExternalReferences.size());
            elementExternalReferences.forEach(elementExtRef -> writeElementExternalReferenceMetadata(stringIndexedWriter, elementExtRef));
        }
    }

    @Override
    public ModuleExternalReferenceMetadata deserializeExternalReferenceMetadata(InputStream stream, StringIndexer stringIndexer)
    {
        try (Reader reader = BinaryReaders.newBinaryReader(stream, false))
        {
            Reader stringIndexedReader = stringIndexer.readStringIndex(reader);
            String moduleName = stringIndexedReader.readString();
            int referenceIdVersion = stringIndexedReader.readInt();
            int elementExtRefCount = stringIndexedReader.readInt();
            ModuleExternalReferenceMetadata.Builder builder = ModuleExternalReferenceMetadata.builder(elementExtRefCount)
                    .withModuleName(moduleName)
                    .withReferenceIdVersion(referenceIdVersion);
            for (int i = 0; i < elementExtRefCount; i++)
            {
                builder.addElementExternalReferenceMetadata(readElementExternalReferenceMetadata(stringIndexedReader));
            }
            return builder.build();
        }
    }

    @Override
    public void serializeBackReferenceMetadata(OutputStream stream, ElementBackReferenceMetadata backReferenceMetadata, StringIndexer stringIndexer)
    {
        try (Writer writer = BinaryWriters.newBinaryWriter(stream, false))
        {
            Writer stringIndexedWriter = stringIndexer.writeStringIndex(writer, collectStrings(backReferenceMetadata));
            stringIndexedWriter.writeString(backReferenceMetadata.getElementPath());
            stringIndexedWriter.writeInt(backReferenceMetadata.getReferenceIdVersion());

            ImmutableList<InstanceBackReferenceMetadata> instanceBackRefs = backReferenceMetadata.getInstanceBackReferenceMetadata();
            stringIndexedWriter.writeInt(instanceBackRefs.size());
            BackReferenceConsumer backRefWriter = new BackReferenceConsumer()
            {
                @Override
                protected void accept(BackReference.Application application)
                {
                    stringIndexedWriter.writeByte((byte) BACK_REF_APPLICATION);
                    stringIndexedWriter.writeString(application.getFunctionExpression());
                }

                @Override
                protected void accept(BackReference.ModelElement modelElement)
                {
                    stringIndexedWriter.writeByte((byte) BACK_REF_MODEL_ELEMENT);
                    stringIndexedWriter.writeString(modelElement.getElement());
                }

                @Override
                protected void accept(BackReference.PropertyFromAssociation propertyFromAssociation)
                {
                    stringIndexedWriter.writeByte((byte) BACK_REF_PROP_FROM_ASSOC);
                    stringIndexedWriter.writeString(propertyFromAssociation.getProperty());
                }

                @Override
                protected void accept(BackReference.QualifiedPropertyFromAssociation qualifiedPropertyFromAssociation)
                {
                    stringIndexedWriter.writeByte((byte) BACK_REF_QUAL_PROP_FROM_ASSOC);
                    stringIndexedWriter.writeString(qualifiedPropertyFromAssociation.getQualifiedProperty());
                }

                @Override
                protected void accept(BackReference.ReferenceUsage referenceUsage)
                {
                    int offset = referenceUsage.getOffset();
                    int offsetIntWidth = getIntWidth(offset);
                    SourceInformation sourceInfo = referenceUsage.getSourceInformation();
                    int hasSourceInfo = (sourceInfo == null) ? BOOLEAN_FALSE : BOOLEAN_TRUE;
                    stringIndexedWriter.writeByte((byte) (BACK_REF_REF_USAGE | offsetIntWidth | hasSourceInfo));
                    stringIndexedWriter.writeString(referenceUsage.getOwner());
                    stringIndexedWriter.writeString(referenceUsage.getProperty());
                    writeIntOfWidth(stringIndexedWriter, offset, offsetIntWidth);
                    if (sourceInfo != null)
                    {
                        writeSourceInfo(stringIndexedWriter, sourceInfo);
                    }
                }

                @Override
                protected void accept(BackReference.Specialization specialization)
                {
                    stringIndexedWriter.writeByte((byte) BACK_REF_SPEC);
                    stringIndexedWriter.writeString(specialization.getGeneralization());
                }
            };
            instanceBackRefs.forEach(instBackRef ->
            {
                stringIndexedWriter.writeString(instBackRef.getInstanceReferenceId());
                ImmutableList<BackReference> backRefs = instBackRef.getBackReferences();
                stringIndexedWriter.writeInt(backRefs.size());
                backRefs.forEach(backRefWriter);
            });
        }
    }

    @Override
    public ElementBackReferenceMetadata deserializeBackReferenceMetadata(InputStream stream, StringIndexer stringIndexer)
    {
        try (Reader reader = BinaryReaders.newBinaryReader(stream, false))
        {
            Reader stringIndexedReader = stringIndexer.readStringIndex(reader);
            String elementPath = stringIndexedReader.readString();
            int referenceIdVersion = stringIndexedReader.readInt();

            int instanceBackRefCount = stringIndexedReader.readInt();
            ElementBackReferenceMetadata.Builder builder = ElementBackReferenceMetadata.builder(instanceBackRefCount)
                    .withElementPath(elementPath)
                    .withReferenceIdVersion(referenceIdVersion);
            for (int i = 0; i < instanceBackRefCount; i++)
            {
                String instanceRefId = stringIndexedReader.readString();
                int backRefCount = stringIndexedReader.readInt();
                BackReference[] backRefs = new BackReference[backRefCount];
                for (int j = 0; j < backRefCount; j++)
                {
                    backRefs[j] = readBackReference(stringIndexedReader);
                }
                builder.addInstanceBackReferenceMetadata(instanceRefId, backRefs);
            }
            return builder.build();
        }
    }

    @Override
    public void serializeFunctionNameMetadata(OutputStream stream, ModuleFunctionNameMetadata functionNameMetadata, StringIndexer stringIndexer)
    {
        try (Writer writer = BinaryWriters.newBinaryWriter(stream, false))
        {
            Writer stringIndexedWriter = stringIndexer.writeStringIndex(writer, collectStrings(functionNameMetadata));
            stringIndexedWriter.writeString(functionNameMetadata.getModuleName());
            ImmutableList<FunctionsByName> functionsByName = functionNameMetadata.getFunctionsByName();
            stringIndexedWriter.writeInt(functionsByName.size());
            functionsByName.forEach(fbn -> writeFunctionsByName(stringIndexedWriter, fbn));
        }
    }

    @Override
    public ModuleFunctionNameMetadata deserializeFunctionNameMetadata(InputStream stream, StringIndexer stringIndexer)
    {
        try (Reader reader = BinaryReaders.newBinaryReader(stream, false))
        {
            Reader stringIndexedReader = stringIndexer.readStringIndex(reader);
            String moduleName = stringIndexedReader.readString();
            int functionCount = stringIndexedReader.readInt();
            ModuleFunctionNameMetadata.Builder builder = ModuleFunctionNameMetadata.builder(functionCount).withModuleName(moduleName);
            for (int i = 0; i < functionCount; i++)
            {
                builder.addFunctionsByName(readFunctionsByName(stringIndexedReader));
            }
            return builder.build();
        }
    }

    @Override
    public void serializeBackReferenceIndex(OutputStream stream, ModuleBackReferenceIndex backReferenceIndex, StringIndexer stringIndexer)
    {
        try (Writer writer = BinaryWriters.newBinaryWriter(stream, false))
        {
            Writer stringIndexedWriter = stringIndexer.writeStringIndex(writer, collectStrings(backReferenceIndex));
            stringIndexedWriter.writeString(backReferenceIndex.getModuleName());
            ImmutableList<String> elementPaths = backReferenceIndex.getElementPaths();
            stringIndexedWriter.writeInt(elementPaths.size());
            elementPaths.forEach(stringIndexedWriter::writeString);
        }
    }

    @Override
    public ModuleBackReferenceIndex deserializeBackReferenceIndex(InputStream stream, StringIndexer stringIndexer)
    {
        try (Reader reader = BinaryReaders.newBinaryReader(stream, false))
        {
            Reader stringIndexedReader = stringIndexer.readStringIndex(reader);
            String moduleName = stringIndexedReader.readString();
            int elementCount = stringIndexedReader.readInt();
            ModuleBackReferenceIndex.Builder builder = ModuleBackReferenceIndex.builder(elementCount).withModuleName(moduleName);
            for (int i = 0; i < elementCount; i++)
            {
                builder.addElementPath(stringIndexedReader.readString());
            }
            return builder.build();
        }
    }

    private void writeElement(Writer writer, ConcreteElementMetadata element)
    {
        writer.writeString(element.getPath());
        writer.writeString(element.getClassifierPath());
        writeSourceInfo(writer, element.getSourceInformation());
    }

    private ConcreteElementMetadata readElement(Reader reader)
    {
        String path = reader.readString();
        String classifierPath = reader.readString();
        SourceInformation sourceInfo = readSourceInfo(reader);
        return new ConcreteElementMetadata(path, classifierPath, sourceInfo);
    }

    private void writeSourceInfo(Writer writer, SourceInformation sourceInfo)
    {
        writer.writeString(sourceInfo.getSourceId());
        int intType = getIntWidth(sourceInfo.getStartLine(), sourceInfo.getStartColumn(), sourceInfo.getLine(), sourceInfo.getColumn(), sourceInfo.getEndLine(), sourceInfo.getEndColumn());
        writer.writeByte((byte) intType);
        switch (intType)
        {
            case BYTE_INT:
            {
                writer.writeByte((byte) sourceInfo.getStartLine());
                writer.writeByte((byte) sourceInfo.getStartColumn());
                writer.writeByte((byte) sourceInfo.getLine());
                writer.writeByte((byte) sourceInfo.getColumn());
                writer.writeByte((byte) sourceInfo.getEndLine());
                writer.writeByte((byte) sourceInfo.getEndColumn());
                break;
            }
            case SHORT_INT:
            {
                writer.writeShort((short) sourceInfo.getStartLine());
                writer.writeShort((short) sourceInfo.getStartColumn());
                writer.writeShort((short) sourceInfo.getLine());
                writer.writeShort((short) sourceInfo.getColumn());
                writer.writeShort((short) sourceInfo.getEndLine());
                writer.writeShort((short) sourceInfo.getEndColumn());
                break;
            }
            case INT_INT:
            {
                writer.writeInt(sourceInfo.getStartLine());
                writer.writeInt(sourceInfo.getStartColumn());
                writer.writeInt(sourceInfo.getLine());
                writer.writeInt(sourceInfo.getColumn());
                writer.writeInt(sourceInfo.getEndLine());
                writer.writeInt(sourceInfo.getEndColumn());
                break;
            }
            default:
            {
                throw new RuntimeException(String.format("Unknown int type code: %02x", intType));
            }
        }
    }

    private SourceInformation readSourceInfo(Reader reader)
    {
        String sourceId = reader.readString();
        int intType = reader.readByte();
        int startLine;
        int startCol;
        int line;
        int col;
        int endLine;
        int endCol;
        switch (intType & INT_WIDTH_MASK)
        {
            case BYTE_INT:
            {
                startLine = reader.readByte();
                startCol = reader.readByte();
                line = reader.readByte();
                col = reader.readByte();
                endLine = reader.readByte();
                endCol = reader.readByte();
                break;
            }
            case SHORT_INT:
            {
                startLine = reader.readShort();
                startCol = reader.readShort();
                line = reader.readShort();
                col = reader.readShort();
                endLine = reader.readShort();
                endCol = reader.readShort();
                break;
            }
            case INT_INT:
            {
                startLine = reader.readInt();
                startCol = reader.readInt();
                line = reader.readInt();
                col = reader.readInt();
                endLine = reader.readInt();
                endCol = reader.readInt();
                break;
            }
            default:
            {
                throw new RuntimeException(String.format("Unknown int type code: %02x", intType & INT_WIDTH_MASK));
            }
        }
        return new SourceInformation(sourceId, startLine, startCol, line, col, endLine, endCol);
    }

    private void writeSource(Writer writer, SourceMetadata sourceMetadata)
    {
        writer.writeString(sourceMetadata.getSourceId());
        ImmutableList<SourceSectionMetadata> sections = sourceMetadata.getSections();
        writer.writeInt(sections.size());
        sections.forEach(section -> writeSourceSection(writer, section));
    }

    private SourceMetadata readSource(Reader reader)
    {
        String sourceId = reader.readString();
        int sectionCount = reader.readInt();
        SourceMetadata.Builder builder = SourceMetadata.builder(sectionCount).withSourceId(sourceId);
        for (int i = 0; i < sectionCount; i++)
        {
            builder.withSection(readSourceSection(reader));
        }
        return builder.build();
    }

    private void writeSourceSection(Writer writer, SourceSectionMetadata sectionMetadata)
    {
        writer.writeString(sectionMetadata.getParser());
        ImmutableList<String> elements = sectionMetadata.getElements();
        writer.writeInt(elements.size());
        elements.forEach(writer::writeString);
    }

    private SourceSectionMetadata readSourceSection(Reader reader)
    {
        String parser = reader.readString();
        int elementCount = reader.readInt();
        SourceSectionMetadata.Builder builder = SourceSectionMetadata.builder(elementCount).withParser(parser);
        for (int i = 0; i < elementCount; i++)
        {
            builder.withElement(reader.readString());
        }
        return builder.build();
    }

    private void writeElementExternalReferenceMetadata(Writer writer, ElementExternalReferenceMetadata elementExtRef)
    {
        writer.writeString(elementExtRef.getElementPath());
        ImmutableList<String> externalReferences = elementExtRef.getExternalReferences();
        writer.writeInt(externalReferences.size());
        externalReferences.forEach(writer::writeString);
    }

    private ElementExternalReferenceMetadata readElementExternalReferenceMetadata(Reader reader)
    {
        String elementPath = reader.readString();
        int extRefCount = reader.readInt();
        ElementExternalReferenceMetadata.Builder builder = ElementExternalReferenceMetadata.builder(extRefCount).withElementPath(elementPath);
        for (int i = 0; i < extRefCount; i++)
        {
            builder.withExternalReference(reader.readString());
        }
        return builder.build();
    }

    private BackReference readBackReference(Reader reader)
    {
        int code = reader.readByte();
        switch (code & BACK_REF_TYPE_MASK)
        {
            case BACK_REF_APPLICATION:
            {
                String functionExpression = reader.readString();
                return BackReference.newApplication(functionExpression);
            }
            case BACK_REF_MODEL_ELEMENT:
            {
                String element = reader.readString();
                return BackReference.newModelElement(element);
            }
            case BACK_REF_PROP_FROM_ASSOC:
            {
                String property = reader.readString();
                return BackReference.newPropertyFromAssociation(property);
            }
            case BACK_REF_QUAL_PROP_FROM_ASSOC:
            {
                String qualifiedProperty = reader.readString();
                return BackReference.newQualifiedPropertyFromAssociation(qualifiedProperty);
            }
            case BACK_REF_REF_USAGE:
            {
                String owner = reader.readString();
                String property = reader.readString();
                int offset = readIntOfWidth(reader, code);
                SourceInformation sourceInfo = ((code & BOOLEAN_MASK) == BOOLEAN_TRUE) ? readSourceInfo(reader) : null;
                return BackReference.newReferenceUsage(owner, property, offset, sourceInfo);
            }
            case BACK_REF_SPEC:
            {
                String specialization = reader.readString();
                return BackReference.newSpecialization(specialization);
            }
            default:
            {
                throw new RuntimeException(String.format("Unknown back reference type code: %02x", code & BACK_REF_TYPE_MASK));
            }
        }
    }

    private void writeFunctionsByName(Writer writer, FunctionsByName functionsByName)
    {
        writer.writeString(functionsByName.getFunctionName());
        ImmutableList<String> functions = functionsByName.getFunctions();
        writer.writeInt(functions.size());
        functions.forEach(writer::writeString);
    }

    private FunctionsByName readFunctionsByName(Reader reader)
    {
        String functionName = reader.readString();
        int functionCount = reader.readInt();
        FunctionsByName.Builder builder = FunctionsByName.builder(functionCount).withFunctionName(functionName);
        for (int i = 0; i < functionCount; i++)
        {
            builder.addFunction(reader.readString());
        }
        return builder.build();
    }

    protected static MutableSet<String> collectStrings(ModuleManifest manifest)
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

    private static int getIntWidth(int... ints)
    {
        int type = BYTE_INT;
        for (int i : ints)
        {
            switch (getIntWidth(i))
            {
                case INT_INT:
                {
                    return INT_INT;
                }
                case SHORT_INT:
                {
                    type = SHORT_INT;
                }
            }
        }
        return type;
    }

    private static int getIntWidth(int i)
    {
        return (i < 0) ?
               (i >= Byte.MIN_VALUE) ? BYTE_INT : ((i >= Short.MIN_VALUE) ? SHORT_INT : INT_INT) :
               (i <= Byte.MAX_VALUE) ? BYTE_INT : ((i <= Short.MAX_VALUE) ? SHORT_INT : INT_INT);
    }

    private static void writeIntOfWidth(Writer writer, int i, int intWidth)
    {
        switch (intWidth)
        {
            case BYTE_INT:
            {
                writer.writeByte((byte) i);
                break;
            }
            case SHORT_INT:
            {
                writer.writeShort((short) i);
                break;
            }
            case INT_INT:
            {
                writer.writeInt(i);
                break;
            }
            default:
            {
                throw new RuntimeException(String.format("Unknown int type code: %02x", intWidth));
            }
        }
    }

    private static int readIntOfWidth(Reader reader, int intWidth)
    {
        switch (intWidth & INT_WIDTH_MASK)
        {
            case BYTE_INT:
            {
                return reader.readByte();
            }
            case SHORT_INT:
            {
                return reader.readShort();
            }
            case INT_INT:
            {
                return reader.readInt();
            }
            default:
            {
                throw new RuntimeException(String.format("Unknown int type code: %02x", intWidth));
            }
        }
    }
}
