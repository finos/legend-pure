package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.helper.AnyStubHelper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.ReferenceUsage;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.AnnotatedElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Annotation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.imports.Imports;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m3.tools.PackageTreeIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.m4.tools.GraphNodeIterable.NodeFilterResult;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.EnumRef;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.ObjRef;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Primitive;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValue;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueMany;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueOne;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.RValue;

import java.util.function.Consumer;

class DistributedBinaryRepositorySerializer extends DistributedBinaryGraphSerializer
{
    DistributedBinaryRepositorySerializer(DistributedMetadataSpecification metadataSpecification, PureRuntime runtime)
    {
        super(runtime, metadataSpecification);
    }

    @Override
    protected void collectInstancesForSerialization(SerializationCollector serializationCollector)
    {
        CoreInstance packageClassifier = this.processorSupport.package_getByUserPath(M3Paths.Package);
        CoreInstance refUsageClassifier = this.processorSupport.package_getByUserPath(M3Paths.ReferenceUsage);
        MutableSet<CoreInstance> stubClassifiers = AnyStubHelper.getStubClasses().collect(this.processorSupport::package_getByUserPath, Sets.mutable.empty());
        MutableSet<CoreInstance> primitiveTypes = PrimitiveUtilities.getPrimitiveTypes(this.processorSupport).toSet();
        this.runtime.getSourceRegistry().getSources().asLazy()
                .select(this::isInRepository)
                .flatCollect(source -> GraphNodeIterable.fromNodes(getSourceElements(source), instance ->
                {
                    CoreInstance classifier = instance.getClassifier();
                    if (packageClassifier == classifier)
                    {
                        return isFromSource(instance, source) ? NodeFilterResult.ACCEPT_AND_CONTINUE : NodeFilterResult.REJECT_AND_STOP;
                    }
                    if (refUsageClassifier == classifier)
                    {
                        return NodeFilterResult.ACCEPT_AND_CONTINUE;
                    }
                    if (stubClassifiers.contains(classifier))
                    {
                        return NodeFilterResult.REJECT_AND_CONTINUE;
                    }
                    if (primitiveTypes.contains(classifier) || isFromDifferentSource(instance, source))
                    {
                        return NodeFilterResult.REJECT_AND_STOP;
                    }
                    return NodeFilterResult.ACCEPT_AND_CONTINUE;
                }))
                .forEach(serializationCollector::collectInstanceForSerialization);
        collectPackageUpdates(serializationCollector);

        PackageTreeIterable.newRootPackageTreeIterable(this.runtime.getModelRepository(), false)
                .flatCollect(Package::_children)
                .reject(c -> (c instanceof Package) || isInRepository(c))
                .collect(this::computeUpdate)
                .forEach(u -> u.collectForSerialization(serializationCollector));
    }

    private String getRepositoryName()
    {
        return getMetadataName();
    }

    private ListIterable<? extends CoreInstance> getSourceElements(Source source)
    {
        ListIterable<? extends CoreInstance> importGroups = Imports.getImportGroupsForSource(source.getId(), this.processorSupport);
        ListMultimap<Parser, CoreInstance> elementsByParser = source.getElementsByParser();
        if (elementsByParser == null)
        {
            return importGroups;
        }
        return Lists.mutable.<CoreInstance>ofInitialCapacity(elementsByParser.size() + importGroups.size())
                .withAll(elementsByParser.valuesView())
                .withAll(importGroups);
    }

    private PackageableElementUpdate<?> computeUpdate(PackageableElement element)
    {
        if (element instanceof Type)
        {
            return new TypeUpdate((Type) element);
        }
        if (element instanceof Profile)
        {
            return new ProfileUpdate((Profile) element);
        }
        if (element instanceof PackageableFunction)
        {
            return new PackageableFunctionUpdate<>((PackageableFunction<?>) element);
        }
        return new PackageableElementUpdate<>(element);
    }

    private void collectPackageUpdates(SerializationCollector serializationCollector)
    {
        // Collect package children and reference usages for this repository
        MutableMap<Package, PackageUpdate> packageUpdates = Maps.mutable.empty();
        PackageTreeIterable.newRootPackageTreeIterable(this.runtime.getModelRepository(), false)
                .reject(this::isInRepository)
                .collect(PackageUpdate::new)
                .reject(PackageUpdate::isEmpty)
                .forEach(u -> packageUpdates.put(u.getInstance(), u));

        if (packageUpdates.isEmpty())
        {
            return;
        }

        // Populate parent packages
        Lists.mutable.withAll(packageUpdates.keySet()).forEach(pkg ->
        {
            Package child = pkg;
            Package parent;
            while ((parent = child._package()) != null)
            {
                if (isInRepository(parent))
                {
                    // if parent is in the repository, there is no need to compute updates
                    return;
                }

                if (packageUpdates.containsKey(parent))
                {
                    // since the parent is already in the map of updates, we can add the child ref and stop
                    packageUpdates.get(parent).addChild(child);
                    return;
                }

                packageUpdates.put(parent, new PackageUpdate(parent).withChild(child));
                child = parent;
            }
        });

        // Compute updates
        packageUpdates.forEachValue(u -> u.collectForSerialization(serializationCollector));
    }

    private boolean isInRepository(CoreInstance instance)
    {
        SourceInformation sourceInfo = instance.getSourceInformation();
        return (sourceInfo != null) && isInRepository(sourceInfo);
    }

    private boolean isInDifferentRepository(CoreInstance instance)
    {
        SourceInformation sourceInfo = instance.getSourceInformation();
        return (sourceInfo != null) && !isInRepository(sourceInfo);
    }

    private boolean isInRepository(SourceInformation sourceInfo)
    {
        return isInRepository(sourceInfo.getSourceId());
    }

    private boolean isInRepository(Source source)
    {
        return isInRepository(source.getId());
    }

    private boolean isInRepository(String sourceId)
    {
        return PureCodeStorage.isSourceInRepository(sourceId, getRepositoryName());
    }

    private static boolean isFromSource(CoreInstance instance, Source source)
    {
        SourceInformation sourceInfo = instance.getSourceInformation();
        return (sourceInfo != null) && source.getId().equals(sourceInfo.getSourceId());
    }

    private static boolean isFromDifferentSource(CoreInstance instance, Source source)
    {
        SourceInformation sourceInfo = instance.getSourceInformation();
        return (sourceInfo != null) && !source.getId().equals(sourceInfo.getSourceId());
    }

    private static int compareObjRefs(ObjRef ref1, ObjRef ref2)
    {
        if (ref1 == ref2)
        {
            return 0;
        }

        int cmp = ref1.getClassifierId().compareTo(ref2.getClassifierId());
        return (cmp != 0) ? cmp : ref1.getId().compareTo(ref2.getId());
    }

    private abstract class InstanceUpdate<T extends CoreInstance>
    {
        private final T instance;

        protected InstanceUpdate(T instance)
        {
            this.instance = instance;
        }

        T getInstance()
        {
            return this.instance;
        }

        abstract boolean isEmpty();

        boolean notEmpty()
        {
            return !isEmpty();
        }

        void collectForSerialization(SerializationCollector serializationCollector)
        {
            if (notEmpty())
            {
                collectInstancesForSerialization(serializationCollector::collectInstanceForSerialization);
                MutableList<PropertyValue> propertyValues = Lists.mutable.empty();
                collectPropertyValues(propertyValues::add);
                if (propertyValues.notEmpty())
                {
                    propertyValues.sortThisBy(PropertyValue::getProperty);
                    Obj objUpdate = Obj.newObj(buildClassifierId(this.instance), buildInstanceId(this.instance), includeName() ? this.instance.getName() : null, propertyValues.toImmutable(), null, (this.instance instanceof Enum));
                    serializationCollector.collectObjUpdate(objUpdate);
                }
            }
        }

        protected abstract void collectInstancesForSerialization(Consumer<? super CoreInstance> instanceConsumer);

        protected abstract void collectPropertyValues(Consumer<PropertyValue> consumer);

        protected boolean includeName()
        {
            return false;
        }

        protected PropertyValue newPropertyValue(String property, String value)
        {
            return newPropertyValue(property, new Primitive(value));
        }

        protected PropertyValue newPropertyValue(String property, CoreInstance value)
        {
            return newPropertyValue(property, (value instanceof Enum) ? buildEnumRef(value) : buildObjRef(value));
        }

        protected PropertyValue newPropertyValue(String property, RValue value)
        {
            return new PropertyValueOne(property, value);
        }

        protected PropertyValue newPropertyValue(String property, MutableCollection<? extends CoreInstance> values)
        {
            MutableList<ObjRef> list = values.collect(this::buildObjRef, Lists.mutable.withInitialCapacity(values.size()));
            if (list.size() == 1)
            {
                return newPropertyValue(property, list.get(0));
            }
            list.sortThis(DistributedBinaryRepositorySerializer::compareObjRefs);
            return new PropertyValueMany(property, Lists.immutable.withAll(list));
        }

        private ObjRef buildObjRef(CoreInstance instance)
        {
            String classifierId = buildClassifierId(instance);
            String identifier = buildInstanceId(instance);
            return new ObjRef(classifierId, identifier);
        }

        private EnumRef buildEnumRef(CoreInstance enumValue)
        {
            String classifierId = buildClassifierId(instance);
            return new EnumRef(classifierId, enumValue.getName());
        }
    }

    private class PackageableElementUpdate<E extends CoreInstance> extends InstanceUpdate<E>
    {
        private final MutableSet<ReferenceUsage> refUsages = Sets.mutable.empty();

        protected PackageableElementUpdate(E instance)
        {
            super(instance);
            ((PackageableElement)instance)._referenceUsages().asLazy()
                    .select(r -> isInRepository(AnyStubHelper.fromStub(r._ownerCoreInstance())))
                    .forEach(this.refUsages::add);
        }

        @Override
        boolean isEmpty()
        {
            return this.refUsages.isEmpty();
        }

        @Override
        protected void collectInstancesForSerialization(Consumer<? super CoreInstance> instanceConsumer)
        {
            this.refUsages.forEach(instanceConsumer);
        }

        @Override
        protected void collectPropertyValues(Consumer<PropertyValue> consumer)
        {
            if (this.refUsages.notEmpty())
            {
                consumer.accept(newPropertyValue(M3Properties.referenceUsages, this.refUsages));
            }
        }
    }

    private class PackageUpdate extends PackageableElementUpdate<Package>
    {
        private final MutableSet<PackageableElement> children = Sets.mutable.empty();

        private PackageUpdate(Package pkg)
        {
            super(pkg);
            pkg._children().asLazy()
                    .select(DistributedBinaryRepositorySerializer.this::isInRepository)
                    .forEach(this.children::add);
        }

        @Override
        boolean isEmpty()
        {
            return super.isEmpty() && this.children.isEmpty();
        }

        @Override
        protected boolean includeName()
        {
            return true;
        }

        @Override
        protected void collectPropertyValues(Consumer<PropertyValue> consumer)
        {
            super.collectPropertyValues(consumer);

            Package pkg = getInstance();

            // name
            consumer.accept(newPropertyValue(M3Properties.name, pkg.getName()));

            // package
            CoreInstance parent = pkg.getValueForMetaPropertyToOne(M3Properties._package);
            if (parent != null)
            {
                consumer.accept(newPropertyValue(M3Properties._package, parent));
            }

            // children
            if (this.children.notEmpty())
            {
                consumer.accept(newPropertyValue(M3Properties.children, this.children));
            }
        }

        void addChild(PackageableElement child)
        {
            this.children.add(child);
        }

        PackageUpdate withChild(PackageableElement child)
        {
            addChild(child);
            return this;
        }
    }

    private class TypeUpdate extends PackageableElementUpdate<Type>
    {
        private final MutableSet<Generalization> specializations = Sets.mutable.empty();

        private TypeUpdate(Type type)
        {
            super(type);
            type._specializations().asLazy()
                    .select(s -> isInRepository(s._specific()))
                    .forEach(this.specializations::add);
        }

        @Override
        boolean isEmpty()
        {
            return super.isEmpty() && this.specializations.isEmpty();
        }

        @Override
        protected void collectInstancesForSerialization(Consumer<? super CoreInstance> instanceConsumer)
        {
            super.collectInstancesForSerialization(instanceConsumer);
            this.specializations.forEach(instanceConsumer);
            // TODO do we need to add the general generic type?
        }

        @Override
        protected void collectPropertyValues(Consumer<PropertyValue> consumer)
        {
            super.collectPropertyValues(consumer);
            if (this.specializations.notEmpty())
            {
                consumer.accept(newPropertyValue(M3Properties.specializations, this.specializations));
            }
        }
    }

    private class ProfileUpdate extends PackageableElementUpdate<Profile>
    {
        private final MutableList<AnnotationUpdate> annotationUpdates = Lists.mutable.empty();

        private ProfileUpdate(Profile profile)
        {
            super(profile);
            profile._p_stereotypes().collect(AnnotationUpdate::new, this.annotationUpdates);
            profile._p_tags().collect(AnnotationUpdate::new, this.annotationUpdates);
        }

        @Override
        void collectForSerialization(SerializationCollector serializationCollector)
        {
            super.collectForSerialization(serializationCollector);
            this.annotationUpdates.forEach(u -> u.collectForSerialization(serializationCollector));
        }
    }

    private class AnnotationUpdate extends InstanceUpdate<Annotation>
    {
        private final MutableSet<AnnotatedElement> modelElements = Sets.mutable.empty();

        private AnnotationUpdate(Annotation annotation)
        {
            super(annotation);
            annotation._modelElements().asLazy()
                    .select(DistributedBinaryRepositorySerializer.this::isInRepository)
                    .forEach(this.modelElements::add);
        }

        @Override
        boolean isEmpty()
        {
            return this.modelElements.isEmpty();
        }

        @Override
        protected void collectInstancesForSerialization(Consumer<? super CoreInstance> instanceConsumer)
        {
            // No need to collect any
        }

        @Override
        protected void collectPropertyValues(Consumer<PropertyValue> consumer)
        {
            if (this.modelElements.notEmpty())
            {
                consumer.accept(newPropertyValue(M3Properties.modelElements, this.modelElements));
            }
        }
    }

    private class PackageableFunctionUpdate<F extends PackageableFunction<?>> extends PackageableElementUpdate<F>
    {
        private final MutableSet<FunctionExpression> applications = Sets.mutable.empty();

        private PackageableFunctionUpdate(F function)
        {
            super(function);
            function._applications().asLazy()
                    .select(DistributedBinaryRepositorySerializer.this::isInRepository)
                    .forEach(this.applications::add);
        }

        @Override
        boolean isEmpty()
        {
            return super.isEmpty() && this.applications.isEmpty();
        }

        @Override
        protected void collectPropertyValues(Consumer<PropertyValue> consumer)
        {
            if (this.applications.notEmpty())
            {
                consumer.accept(newPropertyValue(M3Properties.applications, this.applications));
            }
        }
    }
}
