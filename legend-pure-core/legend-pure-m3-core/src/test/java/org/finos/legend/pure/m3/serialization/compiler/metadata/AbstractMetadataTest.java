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

import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public abstract class AbstractMetadataTest
{
    protected static ConcreteElementMetadata newClass(String path, String sourceId, int startLine, int startCol, int endLine, int endCol)
    {
        return newElement(path, M3Paths.Class, sourceId, startLine, startCol, endLine, endCol);
    }

    protected static ConcreteElementMetadata newAssociation(String path, String sourceId, int startLine, int startCol, int endLine, int endCol)
    {
        return newElement(path, M3Paths.Association, sourceId, startLine, startCol, endLine, endCol);
    }

    protected static ConcreteElementMetadata newEnumeration(String path, String sourceId, int startLine, int startCol, int endLine, int endCol)
    {
        return newElement(path, M3Paths.Enumeration, sourceId, startLine, startCol, endLine, endCol);
    }

    protected static ConcreteElementMetadata newElement(String path, String classifierPath, String sourceId, int startLine, int startCol, int endLine, int endCol)
    {
        return newElement(path, classifierPath, newSourceInfo(sourceId, startLine, startCol, endLine, endCol));
    }

    protected static ConcreteElementMetadata newElement(String path, String classifierPath, SourceInformation sourceInfo)
    {
        return new ConcreteElementMetadata(path, classifierPath, sourceInfo);
    }

    protected static VirtualPackageMetadata newVirtualPackage(String path)
    {
        return new VirtualPackageMetadata(path);
    }

    protected static BackReference.Application application(String funcExpr)
    {
        return BackReference.newApplication(funcExpr);
    }

    protected static BackReference.ModelElement modelElement(String element)
    {
        return BackReference.newModelElement(element);
    }

    protected static BackReference.PropertyFromAssociation propFromAssoc(String property)
    {
        return BackReference.newPropertyFromAssociation(property);
    }

    protected static BackReference.QualifiedPropertyFromAssociation qualPropFromAssoc(String qualifiedProperty)
    {
        return BackReference.newQualifiedPropertyFromAssociation(qualifiedProperty);
    }

    protected static BackReference.ReferenceUsage refUsage(String owner, String property)
    {
        return refUsage(owner, property, 0);
    }

    protected static BackReference.ReferenceUsage refUsage(String owner, String property, int offset)
    {
        return BackReference.newReferenceUsage(owner, property, offset);
    }

    protected static BackReference.ReferenceUsage refUsage(String owner, String property, int offset, String sourceId, int startLine, int startCol, int endLine, int endCol)
    {
        return refUsage(owner, property, offset, newSourceInfo(sourceId, startLine, startCol, endLine, endCol));
    }

    protected static BackReference.ReferenceUsage refUsage(String owner, String property, int offset, SourceInformation sourceInfo)
    {
        return BackReference.newReferenceUsage(owner, property, offset, sourceInfo);
    }

    protected static BackReference.Specialization specialization(String generalization)
    {
        return BackReference.newSpecialization(generalization);
    }

    protected static SourceInformation newSourceInfo(String sourceId, int startLine, int startCol, int endLine, int endCol)
    {
        return new SourceInformation(sourceId, startLine, startCol, startLine, startCol, endLine, endCol);
    }

    protected static SourceMetadata newSource(String sourceId, SourceSectionMetadata... sections)
    {
        return SourceMetadata.builder(sections.length).withSourceId(sourceId).withSections(sections).build();
    }

    protected static SourceMetadata newSource(String sourceId, String... elementPaths)
    {
        return newSource(sourceId, newSourceSection("Pure", elementPaths));
    }

    protected static SourceSectionMetadata newSourceSection(String parserName, String... elementPaths)
    {
        return SourceSectionMetadata.builder(elementPaths.length).withParser(parserName).withElements(elementPaths).build();
    }
}
