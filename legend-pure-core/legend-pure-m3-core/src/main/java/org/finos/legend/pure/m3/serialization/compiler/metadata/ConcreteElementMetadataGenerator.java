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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.Referenceable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Annotation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.graph.GraphPath;
import org.finos.legend.pure.m3.navigation.graph.GraphPathIterable;
import org.finos.legend.pure.m3.navigation.graph.ResolvedGraphPath;
import org.finos.legend.pure.m3.navigation.imports.Imports;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProvider;
import org.finos.legend.pure.m3.tools.ContainingElementIndex;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstanceWrapper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.m4.tools.GraphWalkFilterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

class ConcreteElementMetadataGenerator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ConcreteElementMetadataGenerator.class);
    private static final int BACK_REF_CACHE_THRESHOLD = 100;

    private final ReferenceIdProvider referenceIdProvider;
    private final ContainingElementIndex containingElementIndex;
    private final ProcessorSupport processorSupport;
    private final MapIterable<String, ImmutableList<String>> backReferenceProperties = M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.groupByUniqueKey(ImmutableList::getLast, Maps.mutable.ofInitialCapacity(M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.size()));
    private final ConcurrentMutableMap<CoreInstance, String> elementPathCache = ConcurrentHashMap.newMap();
    private final ConcurrentMutableMap<CoreInstance, BackReferenceCollector> backRefCollectorCache = ConcurrentHashMap.newMap();

    ConcreteElementMetadataGenerator(ReferenceIdProvider referenceIdProvider, ContainingElementIndex containingElementIndex, ProcessorSupport processorSupport)
    {
        this.referenceIdProvider = referenceIdProvider;
        this.containingElementIndex = containingElementIndex;
        this.processorSupport = processorSupport;
    }

    ConcreteElementMetadataGenerator(ReferenceIdProvider referenceIdProvider, ProcessorSupport processorSupport)
    {
        this(referenceIdProvider, ContainingElementIndex.builder(processorSupport).withAllElements().build(), processorSupport);
    }

    ModuleMetadata.Builder computeMetadata(ModuleMetadata.Builder builder, CoreInstance concreteElement)
    {
        long start = System.nanoTime();
        if (!PackageableElement.isPackageableElement(concreteElement, this.processorSupport))
        {
            throw new IllegalArgumentException("Not a PackageableElement: " + concreteElement);
        }

        String elementPath = getElementPath(concreteElement);
        LOGGER.debug("Generating metadata for {}", elementPath);
        try
        {
            computeConcreteElementMetadata(builder, elementPath, concreteElement);
            computeExternalReferences(builder, elementPath, concreteElement);
            return builder;
        }
        catch (Throwable t)
        {
            LOGGER.debug("Error generating metadata for {}", elementPath, t);
            throw t;
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished generating metadata for {} in {}s", elementPath, (end - start) / 1_000_000_000.0);
        }
    }

    private void computeConcreteElementMetadata(ModuleMetadata.Builder builder, String elementPath, CoreInstance concreteElement)
    {
        SourceInformation sourceInfo = concreteElement.getSourceInformation();
        if (sourceInfo == null)
        {
            throw new IllegalArgumentException("Missing source information for " + elementPath);
        }

        CoreInstance classifier = this.processorSupport.getClassifier(concreteElement);
        if (classifier == null)
        {
            throw new IllegalArgumentException("Cannot get classifier for " + elementPath);
        }

        builder.addElement(new ConcreteElementMetadata(elementPath, getElementPath(classifier), sourceInfo));
    }

    private void computeExternalReferences(ModuleMetadata.Builder builder, String elementPath, CoreInstance concreteElement)
    {
        MutableSet<CoreInstance> internalNodes = Sets.mutable.empty();
        MutableList<CoreInstance> externalNodes = Lists.mutable.empty();
        GraphNodeIterable.builder()
                .withStartingNode(concreteElement)
                .withKeyFilter(this::propertyFilter)
                .withNodeFilter(node -> isExternal(concreteElement, node) ? GraphWalkFilterResult.stop(!Imports.isImportGroup(node, this.processorSupport)) : GraphWalkFilterResult.ACCEPT_AND_CONTINUE)
                .build()
                .forEach(node -> (isExternal(concreteElement, node) ? externalNodes : internalNodes).add(node));
        LOGGER.debug("{} has {} internal nodes and {} external references", elementPath, internalNodes.size(), externalNodes.size());
        if (externalNodes.notEmpty())
        {
            boolean elementIsAssociation = isAssociation(concreteElement);
            MutableMap<CoreInstance, String> internalIdCache = Maps.mutable.empty();
            ElementExternalReferenceMetadata.Builder extRefBuilder = ElementExternalReferenceMetadata.builder(externalNodes.size()).withElementPath(elementPath);
            externalNodes.forEach(externalNode ->
            {
                String extNodeRefId = this.referenceIdProvider.getReferenceId(externalNode);
                extRefBuilder.addExternalReference(extNodeRefId);

                MutableList<BackReference> backRefs = getBackReferenceCollector(externalNode, extNodeRefId).getBackReferences(concreteElement, internalNodes, internalIdCache, elementIsAssociation);
                if (backRefs.notEmpty())
                {
                    CoreInstance containingElement = getContainingElement(externalNode);
                    if (containingElement == null)
                    {
                        throw new RuntimeException("Cannot find containing element for external reference " + extNodeRefId + " used in " + elementPath);
                    }
                    String containingElementPath = getElementPath(containingElement);
                    builder.addBackReferences(containingElementPath, extNodeRefId, backRefs);
                }
            });
            builder.addExternalReferences(extRefBuilder.build());
        }
    }

    private boolean isExternal(CoreInstance concreteElement, CoreInstance node)
    {
        if (concreteElement == node)
        {
            return false;
        }

        SourceInformation sourceInfo = node.getSourceInformation();
        return (sourceInfo == null) ? _Package.isPackage(node, this.processorSupport) : !concreteElement.getSourceInformation().subsumes(sourceInfo);
    }

    private boolean isAnnotation(CoreInstance instance)
    {
        return isInstanceOf(instance, Annotation.class, M3Paths.Annotation);
    }

    private boolean isAssociation(CoreInstance instance)
    {
        return isInstanceOf(instance, Association.class, M3Paths.Association);
    }

    private boolean isClass(CoreInstance instance)
    {
        return isInstanceOf(instance, Class.class, M3Paths.Class);
    }

    private boolean isFunction(CoreInstance instance)
    {
        return isInstanceOf(instance, Function.class, M3Paths.Function);
    }

    private boolean isReferenceable(CoreInstance instance)
    {
        return isInstanceOf(instance, Referenceable.class, M3Paths.Referenceable);
    }

    private boolean isType(CoreInstance instance)
    {
        return isInstanceOf(instance, Type.class, M3Paths.Type);
    }

    private boolean isInstanceOf(CoreInstance instance, java.lang.Class<? extends Any> javaClass, String pureClassPath)
    {
        return javaClass.isInstance(instance) ||
                ((!(instance instanceof Any) || (instance instanceof AbstractCoreInstanceWrapper)) && this.processorSupport.instance_instanceOf(instance, pureClassPath));
    }

    private boolean propertyFilter(CoreInstance node, String property)
    {
        ImmutableList<String> propertyPath = this.backReferenceProperties.get(property);
        return (propertyPath == null) || !propertyPath.equals(node.getRealKeyByName(property));
    }

    private String getReferenceIdForInternalNode(CoreInstance internalNode, MutableMap<CoreInstance, String> cache, CoreInstance concreteElement, String externalNodeId, String backRefProperty)
    {
        return cache.getIfAbsentPut(internalNode, () -> getReferenceIdForInternalNode(internalNode, concreteElement, externalNodeId, backRefProperty));
    }

    private String getReferenceIdForInternalNode(CoreInstance internalNode, CoreInstance concreteElement, String externalNodeId, String backRefProperty)
    {
        try
        {
            return this.referenceIdProvider.getReferenceId(internalNode);
        }
        catch (Exception e)
        {
            StringBuilder message = new StringBuilder("Error computing ").append(backRefProperty).append(" for ").append(externalNodeId).append(" from ").append(getElementPath(concreteElement));
            tryFindPathToInternalNode(concreteElement, internalNode).ifPresent(path -> path.writeDescription(message.append(": error computing reference id for instance at ")));
            throw new RuntimeException(message.toString(), e);
        }
    }

    private Optional<GraphPath> tryFindPathToInternalNode(CoreInstance concreteElement, CoreInstance internalNode)
    {
        try
        {
            MutableSet<CoreInstance> visited = Sets.mutable.empty();
            return Optional.ofNullable(GraphPathIterable.builder(this.processorSupport)
                            .withStartNode(concreteElement)
                            .withPropertyFilter((rgp, property) -> propertyFilter(rgp.getLastResolvedNode(), property))
                            .withPathFilter(rgp ->
                            {
                                CoreInstance node = rgp.getLastResolvedNode();
                                if (node == internalNode)
                                {
                                    return GraphWalkFilterResult.ACCEPT_AND_STOP;
                                }
                                if (isExternal(concreteElement, node))
                                {
                                    return GraphWalkFilterResult.REJECT_AND_STOP;
                                }
                                return GraphWalkFilterResult.get(false, visited.add(node));
                            })
                            .build()
                            .getAny())
                    .map(ResolvedGraphPath::getGraphPath);
        }
        catch (Exception ignore)
        {
            return Optional.empty();
        }
    }

    private String getElementPath(CoreInstance instance)
    {
        return this.elementPathCache.getIfAbsentPutWithKey(instance, PackageableElement::getUserPathForPackageableElement);
    }

    private CoreInstance getContainingElement(CoreInstance instance)
    {
        return this.containingElementIndex.findContainingElement(instance);
    }

    private BackReferenceCollector getBackReferenceCollector(CoreInstance extRef, String extRefId)
    {
        BackReferenceCollector cached = this.backRefCollectorCache.get(extRef);
        if (cached != null)
        {
            return cached;
        }

        ListIterable<? extends CoreInstance> functionApplications = isFunction(extRef) ? extRef.getValueForMetaPropertyToMany(M3Properties.applications) : Lists.immutable.empty();
        ListIterable<? extends CoreInstance> modelElements = isAnnotation(extRef) ? extRef.getValueForMetaPropertyToMany(M3Properties.modelElements) : Lists.immutable.empty();
        ListIterable<? extends CoreInstance> propertiesFromAssociations;
        ListIterable<? extends CoreInstance> qualifiedPropertiesFromAssociations;
        ListIterable<? extends CoreInstance> referenceUsages;
        ListIterable<? extends CoreInstance> specializations;
        if (isClass(extRef))
        {
            propertiesFromAssociations = extRef.getValueForMetaPropertyToMany(M3Properties.propertiesFromAssociations);
            qualifiedPropertiesFromAssociations = extRef.getValueForMetaPropertyToMany(M3Properties.qualifiedPropertiesFromAssociations);
            referenceUsages = extRef.getValueForMetaPropertyToMany(M3Properties.referenceUsages);
            specializations = extRef.getValueForMetaPropertyToMany(M3Properties.specializations);
        }
        else
        {
            propertiesFromAssociations = Lists.immutable.empty();
            qualifiedPropertiesFromAssociations = Lists.immutable.empty();
            referenceUsages = isReferenceable(extRef) ? extRef.getValueForMetaPropertyToMany(M3Properties.referenceUsages) : Lists.immutable.empty();
            specializations = isType(extRef) ? extRef.getValueForMetaPropertyToMany(M3Properties.specializations) : Lists.immutable.empty();
        }

        if ((shouldCache(functionApplications) || shouldCache(modelElements) || shouldCache(propertiesFromAssociations) || shouldCache(qualifiedPropertiesFromAssociations) || shouldCache(referenceUsages) || shouldCache(specializations)))
        {
            return this.backRefCollectorCache.getIfAbsentPut(extRef, () -> new CachedBackReferenceCollector(extRefId, functionApplications, modelElements, propertiesFromAssociations, qualifiedPropertiesFromAssociations, referenceUsages, specializations));
        }
        return new SimpleBackReferenceCollector(extRefId, functionApplications, modelElements, propertiesFromAssociations, qualifiedPropertiesFromAssociations, referenceUsages, specializations);
    }

    private boolean shouldCache(ListIterable<? extends CoreInstance> values)
    {
        return values.size() >= BACK_REF_CACHE_THRESHOLD;
    }

    private abstract static class BackReferenceCollector
    {
        final String externalRefId;

        private BackReferenceCollector(String externalRefId)
        {
            this.externalRefId = externalRefId;
        }

        MutableList<BackReference> getBackReferences(CoreInstance concreteElement, SetIterable<? extends CoreInstance> internalNodes, MutableMap<CoreInstance, String> idCache, boolean elementIsAssociation)
        {
            MutableList<BackReference> backReferences = Lists.mutable.empty();
            collectBackReferences(concreteElement, internalNodes, idCache, elementIsAssociation, backReferences);
            return backReferences.sortThis();
        }

        abstract void collectBackReferences(CoreInstance concreteElement, SetIterable<? extends CoreInstance> internalNodes, MutableMap<CoreInstance, String> idCache, boolean elementIsAssociation, MutableList<BackReference> backReferences);
    }

    private class SimpleBackReferenceCollector extends BackReferenceCollector
    {
        private final ListIterable<? extends CoreInstance> functionApplications;
        private final ListIterable<? extends CoreInstance> modelElements;
        private final ListIterable<? extends CoreInstance> propertiesFromAssociations;
        private final ListIterable<? extends CoreInstance> qualifiedPropertiesFromAssociations;
        private final ListIterable<? extends CoreInstance> referenceUsages;
        private final ListIterable<? extends CoreInstance> specializations;

        private SimpleBackReferenceCollector(String externalRefId,
                                             ListIterable<? extends CoreInstance> functionApplications,
                                             ListIterable<? extends CoreInstance> modelElements,
                                             ListIterable<? extends CoreInstance> propertiesFromAssociations,
                                             ListIterable<? extends CoreInstance> qualifiedPropertiesFromAssociations,
                                             ListIterable<? extends CoreInstance> referenceUsages,
                                             ListIterable<? extends CoreInstance> specializations)
        {
            super(externalRefId);
            this.functionApplications = functionApplications;
            this.modelElements = modelElements;
            this.propertiesFromAssociations = propertiesFromAssociations;
            this.qualifiedPropertiesFromAssociations = qualifiedPropertiesFromAssociations;
            this.referenceUsages = referenceUsages;
            this.specializations = specializations;
        }

        @Override
        void collectBackReferences(CoreInstance concreteElement, SetIterable<? extends CoreInstance> internalNodes, MutableMap<CoreInstance, String> idCache, boolean elementIsAssociation, MutableList<BackReference> backReferences)
        {
            if (this.functionApplications.notEmpty())
            {
                this.functionApplications.collectIf(
                        internalNodes::contains,
                        fe -> BackReference.newApplication(getReferenceIdForInternalNode(fe, idCache, concreteElement, this.externalRefId, M3Properties.applications)),
                        backReferences);
            }
            if (this.modelElements.notEmpty())
            {
                this.modelElements.collectIf(
                        internalNodes::contains,
                        e -> BackReference.newModelElement(getReferenceIdForInternalNode(e, idCache, concreteElement, this.externalRefId, M3Properties.modelElements)),
                        backReferences);
            }
            if (elementIsAssociation)
            {
                if (this.propertiesFromAssociations.notEmpty())
                {
                    this.propertiesFromAssociations.collectIf(
                            internalNodes::contains,
                            p -> BackReference.newPropertyFromAssociation(getReferenceIdForInternalNode(p, idCache, concreteElement, this.externalRefId, M3Properties.propertiesFromAssociations)),
                            backReferences);
                }
                if (this.qualifiedPropertiesFromAssociations.notEmpty())
                {
                    this.qualifiedPropertiesFromAssociations.collectIf(
                            internalNodes::contains,
                            qp -> BackReference.newQualifiedPropertyFromAssociation(getReferenceIdForInternalNode(qp, idCache, concreteElement, this.externalRefId, M3Properties.qualifiedPropertiesFromAssociations)),
                            backReferences);
                }
            }
            if (this.referenceUsages.notEmpty())
            {
                this.referenceUsages.forEach(refUsage ->
                {
                    CoreInstance owner = refUsage.getValueForMetaPropertyToOne(M3Properties.owner);
                    if (internalNodes.contains(owner))
                    {
                        String ownerId = getReferenceIdForInternalNode(owner, idCache, concreteElement, this.externalRefId, M3Properties.referenceUsages);
                        String propertyName = PrimitiveUtilities.getStringValue(refUsage.getValueForMetaPropertyToOne(M3Properties.propertyName));
                        int offset = PrimitiveUtilities.getIntegerValue(refUsage.getValueForMetaPropertyToOne(M3Properties.offset)).intValue();
                        SourceInformation sourceInfo = refUsage.getSourceInformation();
                        backReferences.add(BackReference.newReferenceUsage(ownerId, propertyName, offset, sourceInfo));
                    }
                });
            }
            if (this.specializations.notEmpty())
            {
                this.specializations.collectIf(
                        internalNodes::contains,
                        s -> BackReference.newSpecialization(getReferenceIdForInternalNode(s, idCache, concreteElement, this.externalRefId, M3Properties.specializations)),
                        backReferences);
            }
        }
    }

    private class CachedBackReferenceCollector extends BackReferenceCollector
    {
        private final SetIterable<CoreInstance> functionApplications;
        private final SetIterable<CoreInstance> modelElements;
        private final SetIterable<CoreInstance> propertiesFromAssociations;
        private final SetIterable<CoreInstance> qualifiedPropertiesFromAssociations;
        private final MapIterable<CoreInstance, ? extends ListIterable<CoreInstance>> referenceUsages;
        private final SetIterable<CoreInstance> specializations;

        private CachedBackReferenceCollector(String externalRefId,
                                             ListIterable<? extends CoreInstance> functionApplications,
                                             ListIterable<? extends CoreInstance> modelElements,
                                             ListIterable<? extends CoreInstance> propertiesFromAssociations,
                                             ListIterable<? extends CoreInstance> qualifiedPropertiesFromAssociations,
                                             ListIterable<? extends CoreInstance> referenceUsages,
                                             ListIterable<? extends CoreInstance> specializations)
        {
            super(externalRefId);
            this.functionApplications = functionApplications.isEmpty() ? Sets.immutable.empty() : Sets.mutable.withAll(functionApplications);
            this.modelElements = modelElements.isEmpty() ? Sets.immutable.empty() : Sets.mutable.withAll(modelElements);
            this.propertiesFromAssociations = propertiesFromAssociations.isEmpty() ? Sets.immutable.empty() : Sets.mutable.withAll(propertiesFromAssociations);
            this.qualifiedPropertiesFromAssociations = qualifiedPropertiesFromAssociations.isEmpty() ? Sets.immutable.empty() : Sets.mutable.withAll(qualifiedPropertiesFromAssociations);
            this.referenceUsages = indexRefUsages(referenceUsages);
            this.specializations = specializations.isEmpty() ? Sets.immutable.empty() : Sets.mutable.withAll(specializations);
        }

        @Override
        void collectBackReferences(CoreInstance concreteElement, SetIterable<? extends CoreInstance> internalNodes, MutableMap<CoreInstance, String> idCache, boolean elementIsAssociation, MutableList<BackReference> backReferences)
        {
            if (this.functionApplications.notEmpty())
            {
                SetIterable<? extends CoreInstance> iterSet;
                SetIterable<? extends CoreInstance> containsSet;
                if (internalNodes.size() > this.functionApplications.size())
                {
                    iterSet = this.functionApplications;
                    containsSet = internalNodes;
                }
                else
                {
                    iterSet = internalNodes;
                    containsSet = this.functionApplications;
                }
                iterSet.collectIf(
                        containsSet::contains,
                        n -> BackReference.newApplication(getReferenceIdForInternalNode(n, idCache, concreteElement, this.externalRefId, M3Properties.applications)),
                        backReferences);
            }
            if (this.modelElements.notEmpty())
            {
                SetIterable<? extends CoreInstance> iterSet;
                SetIterable<? extends CoreInstance> containsSet;
                if (internalNodes.size() > this.modelElements.size())
                {
                    iterSet = this.modelElements;
                    containsSet = internalNodes;
                }
                else
                {
                    iterSet = internalNodes;
                    containsSet = this.modelElements;
                }
                iterSet.collectIf(
                        containsSet::contains,
                        n -> BackReference.newModelElement(getReferenceIdForInternalNode(n, idCache, concreteElement, this.externalRefId, M3Properties.modelElements)),
                        backReferences);
            }
            if (elementIsAssociation)
            {
                if (this.propertiesFromAssociations.notEmpty())
                {
                    SetIterable<? extends CoreInstance> iterSet;
                    SetIterable<? extends CoreInstance> containsSet;
                    if (internalNodes.size() > this.propertiesFromAssociations.size())
                    {
                        iterSet = this.propertiesFromAssociations;
                        containsSet = internalNodes;
                    }
                    else
                    {
                        iterSet = internalNodes;
                        containsSet = this.propertiesFromAssociations;
                    }
                    iterSet.collectIf(
                            containsSet::contains,
                            n -> BackReference.newPropertyFromAssociation(getReferenceIdForInternalNode(n, idCache, concreteElement, this.externalRefId, M3Properties.propertiesFromAssociations)),
                            backReferences);
                }
                if (this.qualifiedPropertiesFromAssociations.notEmpty())
                {
                    SetIterable<? extends CoreInstance> iterSet;
                    SetIterable<? extends CoreInstance> containsSet;
                    if (internalNodes.size() > this.qualifiedPropertiesFromAssociations.size())
                    {
                        iterSet = this.qualifiedPropertiesFromAssociations;
                        containsSet = internalNodes;
                    }
                    else
                    {
                        iterSet = internalNodes;
                        containsSet = this.qualifiedPropertiesFromAssociations;
                    }
                    iterSet.collectIf(
                            containsSet::contains,
                            n -> BackReference.newQualifiedPropertyFromAssociation(getReferenceIdForInternalNode(n, idCache, concreteElement, this.externalRefId, M3Properties.qualifiedPropertiesFromAssociations)),
                            backReferences);
                }
            }
            if (this.referenceUsages.notEmpty())
            {
                if (internalNodes.size() > this.referenceUsages.size())
                {
                    this.referenceUsages.forEachKeyValue((owner, refUsages) ->
                    {
                        if (internalNodes.contains(owner))
                        {
                            refUsages.forEach(refUsage ->
                            {
                                String ownerId = getReferenceIdForInternalNode(owner, idCache, concreteElement, this.externalRefId, M3Properties.referenceUsages);
                                String propertyName = PrimitiveUtilities.getStringValue(refUsage.getValueForMetaPropertyToOne(M3Properties.propertyName));
                                int offset = PrimitiveUtilities.getIntegerValue(refUsage.getValueForMetaPropertyToOne(M3Properties.offset)).intValue();
                                SourceInformation sourceInfo = refUsage.getSourceInformation();
                                backReferences.add(BackReference.newReferenceUsage(ownerId, propertyName, offset, sourceInfo));
                            });
                        }
                    });
                }
                else
                {
                    internalNodes.forEach(n ->
                    {
                        ListIterable<CoreInstance> refUsages = this.referenceUsages.get(n);
                        if (refUsages != null)
                        {
                            refUsages.forEach(refUsage ->
                            {
                                String ownerId = getReferenceIdForInternalNode(n, idCache, concreteElement, this.externalRefId, M3Properties.referenceUsages);
                                String propertyName = PrimitiveUtilities.getStringValue(refUsage.getValueForMetaPropertyToOne(M3Properties.propertyName));
                                int offset = PrimitiveUtilities.getIntegerValue(refUsage.getValueForMetaPropertyToOne(M3Properties.offset)).intValue();
                                SourceInformation sourceInfo = refUsage.getSourceInformation();
                                backReferences.add(BackReference.newReferenceUsage(ownerId, propertyName, offset, sourceInfo));
                            });
                        }
                    });
                }
            }
            if (this.specializations.notEmpty())
            {
                SetIterable<? extends CoreInstance> iterSet;
                SetIterable<? extends CoreInstance> containsSet;
                if (internalNodes.size() > this.specializations.size())
                {
                    iterSet = this.specializations;
                    containsSet = internalNodes;
                }
                else
                {
                    iterSet = internalNodes;
                    containsSet = this.specializations;
                }
                iterSet.collectIf(
                        containsSet::contains,
                        n -> BackReference.newSpecialization(getReferenceIdForInternalNode(n, idCache, concreteElement, this.externalRefId, M3Properties.specializations)),
                        backReferences);
            }
        }

        private MapIterable<CoreInstance, ? extends ListIterable<CoreInstance>> indexRefUsages(ListIterable<? extends CoreInstance> referenceUsages)
        {
            if (referenceUsages.isEmpty())
            {
                return Maps.immutable.empty();
            }

            MutableMap<CoreInstance, MutableList<CoreInstance>> index = Maps.mutable.empty();
            referenceUsages.forEach(ru -> index.getIfAbsentPut(ru.getValueForMetaPropertyToOne(M3Properties.owner), Lists.mutable::empty).add(ru));
            return index;
        }
    }
}
