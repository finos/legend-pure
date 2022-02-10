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

package org.finos.legend.pure.m3.navigation.importstub;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.exception.PureUnresolvedIdentifierException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.imports.Imports;
import org.finos.legend.pure.m3.navigation.profile.Profile;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.tools.SafeAppendable;

public class ImportStub
{
    public static void processImportStub(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub importStub, ModelRepository repository, ProcessorSupport processorSupport)
    {
        if (importStub.getValueForMetaPropertyToOne(M3Properties.resolvedNode) == null)
        {
            CoreInstance resolved = resolveImportStub(importStub, repository, processorSupport);
            Instance.addValueToProperty(importStub, M3Properties.resolvedNode, resolved, processorSupport);
        }
    }

    public static void processPropertyStub(CoreInstance propertyStub, ProcessorSupport processorSupport)
    {
        if (propertyStub.getValueForMetaPropertyToOne(M3Properties.resolvedProperty) == null)
        {
            CoreInstance resolved = resolvePropertyStub(propertyStub, processorSupport);
            Instance.addValueToProperty(propertyStub, M3Properties.resolvedProperty, resolved, processorSupport);
        }
    }

    public static void processEnumStub(CoreInstance enumStub, ProcessorSupport processorSupport)
    {
        if (enumStub.getValueForMetaPropertyToOne(M3Properties.resolvedEnum) == null)
        {
            CoreInstance resolved = resolveEnumStub(enumStub, processorSupport);
            Instance.addValueToProperty(enumStub, M3Properties.resolvedEnum, resolved, processorSupport);
        }
    }

    public static CoreInstance resolveImportStub(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub importStub, ModelRepository repository, ProcessorSupport processorSupport)
    {
        String idOrPath = importStub.getValueForMetaPropertyToOne(M3Properties.idOrPath).getName();
        if (idOrPath.indexOf('@') != -1)
        {
            return resolveStereotype(idOrPath, importStub, repository, processorSupport);
        }
        if (idOrPath.indexOf('%') != -1)
        {
            return resolveTag(idOrPath, importStub, repository, processorSupport);
        }
        return resolvePackageableElement(idOrPath, importStub, repository, processorSupport);
    }

    public static CoreInstance resolveStereotype(String idOrPath, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub importStubNode, ModelRepository repository, ProcessorSupport processorSupport)
    {
        int splitIndex = idOrPath.indexOf('@');
        if (splitIndex == -1)
        {
            throw new IllegalArgumentException("Invalid stereotype id: " + idOrPath);
        }
        String profileIdOrPath = idOrPath.substring(0, splitIndex);
        String stereotypeName = idOrPath.substring(splitIndex + 1);
        CoreInstance packageableElement = resolvePackageableElement(profileIdOrPath, importStubNode, repository, processorSupport);
        if ((packageableElement == null) || (packageableElement.getClassifier() != processorSupport.package_getByUserPath(M3Paths.Profile)))
        {
            throw new PureCompilationException(importStubNode.getSourceInformation(), idOrPath + " : " + profileIdOrPath + " is not a profile!");
        }

        CoreInstance result = Profile.findStereotype(packageableElement, stereotypeName);
        if (result == null)
        {
            throw new PureCompilationException(importStubNode.getSourceInformation(), "The stereotype '" + stereotypeName + "' can't be found in profile '" + profileIdOrPath + "'");
        }
        return result;
    }

    private static CoreInstance resolveTag(String idOrPath, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub importStubNode, ModelRepository repository, ProcessorSupport processorSupport)
    {
        int splitIndex = idOrPath.indexOf('%');
        if (splitIndex == -1)
        {
            throw new IllegalArgumentException("Invalid tag id: " + idOrPath);
        }
        String profileIdOrPath = idOrPath.substring(0, splitIndex);
        String tagName = idOrPath.substring(splitIndex + 1);
        CoreInstance packageableElement = resolvePackageableElement(profileIdOrPath, importStubNode, repository, processorSupport);
        if ((packageableElement == null) || (packageableElement.getClassifier() != processorSupport.package_getByUserPath(M3Paths.Profile)))
        {
            throw new PureCompilationException(importStubNode.getSourceInformation(), idOrPath + " : " + profileIdOrPath + " is not a profile!");
        }

        CoreInstance result = Profile.findTag(packageableElement, tagName);
        if (result == null)
        {
            throw new PureCompilationException(importStubNode.getSourceInformation(), "The tag '" + tagName + "' can't be found in profile '" + profileIdOrPath + "'");
        }
        return result;

    }

    private static CoreInstance resolvePackageableElement(String idOrPath, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub importStubNode, ModelRepository repository, ProcessorSupport processorSupport)
    {
        // Try the Special Types (not user-defined top level types!!!)
        if (_Package.SPECIAL_TYPES.contains(idOrPath))
        {
            return repository.getTopLevel(idOrPath);
        }

        ImportGroup importGroup = importStubNode._importGroup();
        // Check if a package is specified
        int lastIndex = idOrPath.lastIndexOf(':');
        if (lastIndex != -1)
        {
            CoreInstance node = processorSupport.package_getByUserPath(idOrPath);
            if (node == null)
            {
                String id = idOrPath.substring(lastIndex + 1);
                throw new PureUnresolvedIdentifierException(importStubNode.getSourceInformation(), idOrPath, id, repository, processorSupport, importStubNode, importGroup);
            }
            return node;
        }

        // Look in the imported packages
        MutableSet<CoreInstance> results = Sets.mutable.with();
        Imports.getImportGroupPackages(importGroup, processorSupport).forEach(pkg ->
        {
            CoreInstance found = _Package.findInPackage(pkg, idOrPath);
            if (found != null)
            {
                results.add(found);
            }
        });

        switch (results.size())
        {
            case 0:
            {
                // Try user defined top elements (important to do that last ... as doing it earlier could create conflicts...)
                CoreInstance node = processorSupport.package_getByUserPath(idOrPath);
                if (node == null)
                {
                    throw new PureUnresolvedIdentifierException(importStubNode.getSourceInformation(), idOrPath, idOrPath, repository, processorSupport, importStubNode, importGroup);
                }
                return node;
            }
            case 1:
            {
                return results.getAny();
            }
            default:
            {
                throw new PureCompilationException(importStubNode.getSourceInformation(), results.collect(PackageableElement::getUserPathForPackageableElement, Lists.mutable.ofInitialCapacity(results.size())).sortThis().makeString(idOrPath + " has been found more than one time in the imports: [", ", ", "]"));
            }
        }
    }

    private static CoreInstance resolvePropertyStub(CoreInstance propertyStub, ProcessorSupport processorSupport)
    {
        CoreInstance owner = Instance.getValueForMetaPropertyToOneResolved(propertyStub, M3Properties.owner, processorSupport);
        String propertyName = PrimitiveUtilities.getStringValue(propertyStub.getValueForMetaPropertyToOne(M3Properties.propertyName));

        CoreInstance resolvedProperty = processorSupport.class_findPropertyOrQualifiedPropertyUsingGeneralization(owner, propertyName);
        if (resolvedProperty == null)
        {
            throw new PureCompilationException(propertyStub.getSourceInformation(), "The property '" + propertyName + "' can't be found in the type '" + owner.getName() + "' (or any supertype).");
        }
        return resolvedProperty;
    }

    private static CoreInstance resolveEnumStub(CoreInstance enumStub, ProcessorSupport processorSupport)
    {
        CoreInstance enumeration = Instance.getValueForMetaPropertyToOneResolved(enumStub, M3Properties.enumeration, processorSupport);
        String enumName = PrimitiveUtilities.getStringValue(enumStub.getValueForMetaPropertyToOne(M3Properties.enumName));

        CoreInstance resolvedEnum = enumeration.getValueInValueForMetaPropertyToMany(M3Properties.values, enumName);
        if (resolvedEnum == null)
        {
            throw new PureCompilationException(enumStub.getSourceInformation(), "The enum value '" + enumName + "' can't be found in the enumeration " + PackageableElement.getUserPathForPackageableElement(enumeration));
        }
        return resolvedEnum;
    }

    /**
     * Return a list of instances with import stubs replaced by the
     * instances they resolve to.
     *
     * @param instances instances
     * @return instances with import stubs by-passed
     */
    public static ListIterable<? extends CoreInstance> withImportStubByPasses(ListIterable<? extends CoreInstance> instances, ProcessorSupport processorSupport)
    {
        int size = instances.size();
        switch (size)
        {
            case 0:
            {
                return instances;
            }
            case 1:
            {
                CoreInstance instance = instances.get(0);
                CoreInstance byPass = withImportStubByPass(instance, processorSupport);
                return (byPass == instance) ? instances : Lists.mutable.with(byPass);
            }
            default:
            {
                MutableList<CoreInstance> newList = null;
                for (int i = 0; i < size; i++)
                {
                    CoreInstance instance = instances.get(i);
                    CoreInstance byPass = withImportStubByPass(instance, processorSupport);
                    if (byPass != instance)
                    {
                        if (newList == null)
                        {
                            newList = Lists.mutable.withAll(instances);
                        }
                        newList.set(i, byPass);
                    }
                }
                return (newList == null) ? instances : newList;
            }
        }
    }

    /**
     * Will not try to resolve the import stub, if it is not already resolved
     * Use this in the Unbinders and Walkers
     */
    public static CoreInstance withImportStubByPassDoNotResolve(CoreInstance instance, ProcessorSupport processorSupport)
    {
        return withImportStubByPass(instance, processorSupport, false);
    }

    public static CoreInstance withImportStubByPass(CoreInstance instance, ProcessorSupport processorSupport)
    {
        return withImportStubByPass(instance, processorSupport, true);
    }

    private static CoreInstance withImportStubByPass(CoreInstance instance, ProcessorSupport processorSupport, boolean shouldResolve)
    {
        if (instance == null)
        {
            return null;
        }

        CoreInstance classifier = processorSupport.getClassifier(instance);
        if (classifier == null)
        {
            throw new PureCompilationException(instance.getSourceInformation(), "Instance has no classifier: " + instance);
        }

        switch (classifier.getName())
        {
            case "ImportStub":
            {
                CoreInstance resolvedNode = instance.getValueForMetaPropertyToOne(M3Properties.resolvedNode);
                if (resolvedNode == null && shouldResolve)
                {
                    processImportStub((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub)instance, instance.getRepository(), processorSupport);
                    resolvedNode = instance.getValueForMetaPropertyToOne(M3Properties.resolvedNode);
                }
                return resolvedNode;
            }
            case "PropertyStub":
            {
                CoreInstance resolvedProperty = instance.getValueForMetaPropertyToOne(M3Properties.resolvedProperty);
                if (resolvedProperty == null && shouldResolve)
                {
                    processPropertyStub(instance, processorSupport);
                    resolvedProperty = instance.getValueForMetaPropertyToOne(M3Properties.resolvedProperty);
                }
                return resolvedProperty;
            }
            case "EnumStub":
            {
                CoreInstance resolvedEnum = instance.getValueForMetaPropertyToOne(M3Properties.resolvedEnum);
                if (resolvedEnum == null && shouldResolve)
                {
                    processEnumStub(instance, processorSupport);
                    resolvedEnum = instance.getValueForMetaPropertyToOne(M3Properties.resolvedEnum);
                }
                return resolvedEnum;
            }
            case "GrammarInfoStub":
            {
                return instance.getValueForMetaPropertyToOne(M3Properties.value);
            }
            default:
            {
                return instance;
            }
        }
    }

    public static String printImportStub(CoreInstance importStub)
    {
        return writeImportStubInfo(new StringBuilder(64), importStub).toString();
    }

    public static <T extends Appendable> T writeImportStubInfo(T appendable, CoreInstance importStub)
    {
        return writeImportStubInfo(appendable, importStub, false);
    }

    public static <T extends Appendable> T writeImportStubInfo(T appendable, CoreInstance importStub, boolean includeImportGroupSourceInfo)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        safeAppendable.append("<ImportStub idOrPath=");

        String idOrPath = PrimitiveUtilities.getStringValue(importStub.getValueForMetaPropertyToOne(M3Properties.idOrPath), null);
        if (idOrPath == null)
        {
            safeAppendable.append(idOrPath);
        }
        else
        {
            safeAppendable.append('\'').append(idOrPath).append('\'');
        }

        CoreInstance importGroup = importStub.getValueForMetaPropertyToOne(M3Properties.importGroup);
        safeAppendable.append(" importGroup=");
        if (importGroup == null)
        {
            safeAppendable.append("null");
        }
        else
        {
            PackageableElement.writeUserPathForPackageableElement(safeAppendable, importGroup);
            if (includeImportGroupSourceInfo)
            {
                SourceInformation importGroupSourceInfo = importGroup.getSourceInformation();
                if (importGroupSourceInfo != null)
                {
                    importGroupSourceInfo.appendMessage(safeAppendable.append('[')).append(']');
                }
            }
        }

        SourceInformation sourceInfo = importStub.getSourceInformation();
        if (sourceInfo != null)
        {
            sourceInfo.appendMessage(safeAppendable.append(" sourceInfo="));
        }
        safeAppendable.append('>');

        return appendable;
    }
}
