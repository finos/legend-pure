// Copyright 2026 Goldman Sachs
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
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public abstract class BaseModuleMetadataSerializerExtension implements ModuleMetadataSerializerExtension
{
    protected static MutableSet<String> collectStrings(ModuleManifest manifest)
    {
        MutableSet<String> stringSet = Sets.mutable.empty();
        stringSet.add(manifest.getModuleName());
        stringSet.addAll(manifest.getDependencies().castToList());
        manifest.forEachElement(element ->
        {
            stringSet.add(element.getPath());
            stringSet.add(element.getClassifierPath());
            stringSet.add(element.getSourceInformation().getSourceId());
        });
        return stringSet;
    }

    protected static MutableSet<String> collectStrings(ModuleSourceMetadata sourceMetadata)
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

    protected static MutableSet<String> collectStrings(ModuleExternalReferenceMetadata extRefs)
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

    protected static MutableSet<String> collectStrings(ElementBackReferenceMetadata elementBackRefs)
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

    protected static MutableSet<String> collectStrings(ModuleFunctionNameMetadata funcNames)
    {
        MutableSet<String> stringSet = Sets.mutable.empty();
        stringSet.add(funcNames.getModuleName());
        funcNames.getFunctionsByName().forEach(fbn ->
        {
            stringSet.add(fbn.getFunctionName());
            stringSet.addAll(fbn.getFunctions().castToList());
        });
        return stringSet;
    }

    protected static MutableSet<String> collectStrings(ModuleBackReferenceIndex backRefIndex)
    {
        MutableSet<String> stringSet = Sets.mutable.ofInitialCapacity(backRefIndex.size() + 1);
        stringSet.add(backRefIndex.getModuleName());
        stringSet.addAllIterable(backRefIndex.getElementPaths());
        return stringSet;
    }
}
