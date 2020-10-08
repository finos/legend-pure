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

package org.finos.legend.pure.runtime.java.compiled.serialization;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.MutableObjectBooleanMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.factory.Stacks;
import org.eclipse.collections.impl.factory.primitive.ObjectBooleanMaps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.m4.coreinstance.primitive.FloatCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.IntegerCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.PrimitiveCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.JavaCompiledCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.EnumProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.MetadataJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.metadata.CompiledCoreInstanceBuilder;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataEager;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Enum;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.EnumRef;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.ObjRef;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Primitive;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValue;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueMany;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueOne;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.RValue;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Serialized;

import java.lang.reflect.Field;
import java.math.BigDecimal;

public class GraphSerializer
{
    public static final CompileState SERIALIZED = CompileState.COMPILE_EVENT_EXTRA_STATE_1;

    /**
     * Starting from the given nodes, serialize graph nodes that have not already been
     * serialized.
     *
     * @param nodes            starting nodes
     * @param processorSupport processor support
     * @return serialization
     */
    public static Serialized serializeNew(Iterable<? extends CoreInstance> nodes, ProcessorSupport processorSupport)
    {
        return serialize(newCompiledStateSearchState(nodes, processorSupport), processorSupport);
    }

    /**
     * Starting from the given node, serialize graph nodes that have not already been
     * serialized.
     *
     * @param node             starting node
     * @param processorSupport processor support
     * @return serialization
     */
    public static Serialized serializeNew(CoreInstance node, ProcessorSupport processorSupport)
    {
        return serialize(newCompiledStateSearchState(node, processorSupport), processorSupport);
    }

    /**
     * Starting from the given nodes, serialize all graph nodes.
     *
     * @param nodes            starting nodes
     * @param processorSupport processor support
     * @return serialization
     */
    public static Serialized serializeAll(Iterable<? extends CoreInstance> nodes, ProcessorSupport processorSupport)
    {
        return serializeAll(nodes, processorSupport, false);
    }

    /**
     * Starting from the given nodes, serialize all graph nodes. If addSerializedCompileState is
     * true, then a CompileState is added to the serialized nodes to mark that they have been
     * serialized.
     *
     * @param nodes                     starting nodes
     * @param processorSupport          processor support
     * @param addSerializedCompileState whether to add a CompileState indicating a node has been serialized
     * @return serialization
     */
    public static Serialized serializeAll(Iterable<? extends CoreInstance> nodes, ProcessorSupport processorSupport, boolean addSerializedCompileState)
    {
        return serialize(newPrivateSetSearchState(nodes, processorSupport, addSerializedCompileState), processorSupport);
    }

    /**
     * Starting from the given node, serialize all graph nodes.
     *
     * @param node             starting node
     * @param processorSupport processor support
     * @return serialization
     */
    public static Serialized serializeAll(CoreInstance node, ProcessorSupport processorSupport)
    {
        return serializeAll(node, processorSupport, false);
    }

    /**
     * Starting from the given node, serialized all graph nodes. If addSerializedCompileState is
     * true, then a CompileState is added to the serialized nodes to mark that they have been
     * serialized.
     *
     * @param node                      starting node
     * @param processorSupport          processor support
     * @param addSerializedCompileState whether to add a CompileState indicating a node has been serialized
     * @return serialization
     */
    public static Serialized serializeAll(CoreInstance node, ProcessorSupport processorSupport, boolean addSerializedCompileState)
    {
        return serialize(newPrivateSetSearchState(node, processorSupport, addSerializedCompileState), processorSupport);
    }


    private static Serialized serialize(final SearchState state, final ProcessorSupport processorSupport)
    {
        FastList<Obj> added = FastList.newList();
        MutableSet<CoreInstance> addedPackages = Sets.mutable.empty();
        MutableListMultimap<CoreInstance, Obj> links = Multimaps.mutable.list.empty();

        while (state.hasNodes())
        {
            CoreInstance instance = state.nextNode();
            if (state.shouldVisit(instance))
            {
                state.noteVisited(instance);

                // Serialize
                Obj obj = buildObj(instance, state, processorSupport);

                if (Instance.instanceOf(instance, M3Paths.PackageableElement, processorSupport))
                {
                    CoreInstance packageCoreInstance = instance.getValueForMetaPropertyToOne(M3Properties._package);
                    // Don't need to add links if the package is also being serialized as part of this process
                    if ((packageCoreInstance != null) && !addedPackages.contains(packageCoreInstance))
                    {
                        links.put(packageCoreInstance, obj);
                    }
                    if (Instance.instanceOf(instance, M3Paths.Package, processorSupport))
                    {
                        addedPackages.add(instance);
                        links.removeAll(instance);
                    }
                }

                for (String key : instance.getKeys())
                {
                    ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(instance, key, processorSupport);
                    if (values.notEmpty())
                    {
                        CoreInstance property = instance.getKeyByName(key);
                        boolean isToOne = Multiplicity.isToOne(Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.multiplicity, processorSupport), false);

                        PropertyValue propertyValue;
                        if (isToOne)
                        {
                            CoreInstance value = values.get(0);
                            if (!state.isPrimitiveType(value.getClassifier()))
                            {
                                state.addNode(value);
                            }
                            RValue rValue = buildRValue(value, state, processorSupport);
                            propertyValue = new PropertyValueOne(key, rValue);
                        }
                        else
                        {
                            MutableList<RValue> rValues = FastList.newList(values.size());
                            for (CoreInstance value : values)
                            {
                                if (!state.isPrimitiveType(value.getClassifier()))
                                {
                                    state.addNode(value);
                                }
                                RValue rValue = buildRValue(value, state, processorSupport);
                                rValues.add(rValue);
                            }
                            propertyValue = new PropertyValueMany(key, rValues);
                        }
                        obj.addPropertyValue(propertyValue);
                    }
                }
                added.add(obj);
            }
        }
        added.trimToSize();

        final MutableList<Pair<Obj, Obj>> linksList = FastList.newList(links.sizeDistinct());
        links.forEachKeyMultiValues(new Procedure2<CoreInstance, Iterable<Obj>>()
        {
            @Override
            public void value(CoreInstance pkg, Iterable<Obj> objs)
            {
                Obj pkgObj = buildObj(pkg, state, processorSupport);
                for (Obj obj : objs)
                {
                    linksList.add(Tuples.pair(pkgObj, obj));
                }
            }
        });
        return new Serialized(added, linksList);
    }

    public static Obj buildObjWithProperties(CoreInstance instance, ClassifierCaches classifierCaches, ProcessorSupport processorSupport)
    {
        Obj obj = buildObj(instance, classifierCaches, processorSupport);
        collectPropertiesForObj(obj, instance, classifierCaches, processorSupport);
        return obj;
    }

    private static Obj buildObj(CoreInstance instance, ClassifierCaches state, ProcessorSupport processorSupport)
    {
        SourceInformation sourceInformation = instance.getSourceInformation();
        String identifier = IdBuilder.buildId(instance, processorSupport);
        String classifierString = state.getClassifierId(instance.getClassifier());
        return state.isEnum(instance) ? new Enum(sourceInformation, identifier, classifierString, instance.getName()) : new Obj(sourceInformation, identifier, classifierString, instance.getName());
    }

    private static void collectPropertiesForObj(Obj obj, CoreInstance instance, ClassifierCaches classifierCaches, ProcessorSupport processorSupport)
    {
        for (String key : instance.getKeys())
        {
            ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(instance, key, processorSupport);
            if (values.notEmpty())
            {
                CoreInstance property = instance.getKeyByName(key);
                boolean isToOne = Multiplicity.isToOne(Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.multiplicity, processorSupport), false);

                PropertyValue propertyValue;
                if (isToOne)
                {
                    CoreInstance value = values.get(0);
                    RValue rValue = buildRValue(value, classifierCaches, processorSupport);
                    propertyValue = new PropertyValueOne(key, rValue);
                }
                else
                {
                    MutableList<RValue> rValues = FastList.newList(values.size());
                    for (CoreInstance value : values)
                    {
                        RValue rValue = buildRValue(value, classifierCaches, processorSupport);
                        rValues.add(rValue);
                    }
                    propertyValue = new PropertyValueMany(key, rValues);
                }
                obj.addPropertyValue(propertyValue);
            }
        }
    }

    private static RValue buildRValue(CoreInstance value, ClassifierCaches state, ProcessorSupport processorSupport)
    {
        CoreInstance classifier = value.getClassifier();
        if (state.isPrimitiveType(classifier))
        {
            return new Primitive(processPrimitiveTypeJava(value, processorSupport));
        }
        String classifierId = state.getClassifierId(classifier);
        return state.isEnumeration(classifier) ? new EnumRef(classifierId, value.getName()) : new ObjRef(classifierId, IdBuilder.buildId(value, processorSupport));
    }

    public static Object processPrimitiveTypeJava(CoreInstance instance, ProcessorSupport processorSupport)
    {
        if (instance == null)
        {
            throw new IllegalArgumentException("Cannot process null as a primitive value");
        }

        // Special handling for PrimitiveCoreInstances
        if (instance instanceof FloatCoreInstance)
        {
            return ((FloatCoreInstance)instance).getValue().doubleValue();
        }
        if (instance instanceof IntegerCoreInstance)
        {
            return ((IntegerCoreInstance)instance).getValue().longValue();
        }
        if (instance instanceof PrimitiveCoreInstance<?>)
        {
            return ((PrimitiveCoreInstance<?>)instance).getValue();
        }

        // General handling
        if (Instance.instanceOf(instance, M3Paths.String, processorSupport))
        {
            return instance.getName();
        }
        if (Instance.instanceOf(instance, M3Paths.Boolean, processorSupport))
        {
            return Boolean.valueOf(instance.getName());
        }
        if (Instance.instanceOf(instance, M3Paths.LatestDate, processorSupport))
        {
            return LatestDate.instance;
        }
        if (Instance.instanceOf(instance, M3Paths.Date, processorSupport))
        {
            return DateFunctions.parsePureDate(instance.getName());
        }
        if (Instance.instanceOf(instance, M3Paths.Float, processorSupport))
        {
            return Double.valueOf(instance.getName());
        }
        if (Instance.instanceOf(instance, M3Paths.Decimal, processorSupport))
        {
            return new BigDecimal(instance.getName());
        }
        if (Instance.instanceOf(instance, M3Paths.Integer, processorSupport))
        {
            return Long.valueOf(instance.getName());
        }

        // Unknown type
        StringBuilder message = new StringBuilder("Unhandled primitive (type = ");
        PackageableElement.writeUserPathForPackageableElement(message, instance.getClassifier());
        message.append("): ");
        instance.print(message, "");
        throw new IllegalArgumentException(message.toString());
    }

    public static Object valueSpecToJavaObject(CoreInstance instance, Context context, ProcessorSupport processorSupport,
                                               Metadata metamodel)
    {
        ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(instance, M3Properties.values, processorSupport);
        switch (values.size())
        {
            case 0:
            {
                return FastList.newList();
            }
            case 1:
            {
                return valueSpecValueToJavaObject(values.get(0), context, processorSupport, metamodel);
            }
            default:
            {
                MutableList<Object> result = FastList.newList(values.size());
                for (CoreInstance value : values)
                {
                    result.add(valueSpecValueToJavaObject(value, context, processorSupport, metamodel));
                }
                return result;
            }
        }
    }

    private static Object valueSpecValueToJavaObject(CoreInstance value, Context context, ProcessorSupport processorSupport,
                                                     Metadata metamodel)
    {
        // TODO refactor this
        if (value instanceof JavaCompiledCoreInstance)
        {
            if (processorSupport.instance_instanceOf(value, M3Paths.String))
            {
                return value.getName();
            }
            if (processorSupport.instance_instanceOf(value, M3Paths.Boolean))
            {
                return Boolean.valueOf(value.getName());
            }
            if (processorSupport.instance_instanceOf(value, M3Paths.Integer))
            {
                return Long.valueOf(value.getName());
            }
            if (processorSupport.instance_instanceOf(value, M3Paths.Float))
            {
                return Double.valueOf(value.getName());
            }
            if (processorSupport.instance_instanceOf(value, M3Paths.LatestDate))
            {
                return LatestDate.instance;
            }
            if (processorSupport.instance_instanceOf(value, M3Paths.Date))
            {
                return DateFunctions.parsePureDate(value.getName());
            }
        }
        else
        {
            M3ProcessorSupport m3ProcessorSupport = new M3ProcessorSupport(context, value.getRepository());
            if (Type.isPrimitiveType(value.getClassifier(), m3ProcessorSupport))
            {
                return processPrimitiveTypeJava(value, m3ProcessorSupport);
            }
            else if (Instance.instanceOf(value.getClassifier(), M3Paths.Enumeration, m3ProcessorSupport))
            {
                return metamodel.getEnum(MetadataJavaPaths.buildMetadataKeyFromType(value.getClassifier()), value.getName());
            }
            else if (Instance.instanceOf(value, M3Paths.Class, m3ProcessorSupport))
            {
                return metamodel.getMetadata(MetadataJavaPaths.Class, PackageableElement.getSystemPathForPackageableElement(value, "::"));
            }
        }
        return value;
    }

    public static void buildGraph(Serialized serialized, MetadataEager metadata, CompiledCoreInstanceBuilder instanceBuilder, ClassLoader classLoader)
    {
        buildGraph(serialized, metadata, instanceBuilder, Sets.immutable.<CoreInstance>empty(), null, classLoader);
    }

    public static void buildGraph(Serialized serialized, MetadataEager metadata, CompiledCoreInstanceBuilder instanceBuilder, SetIterable<? extends CoreInstance> excluded, MutableSet<? super CoreInstance> resultExcluded, ClassLoader classLoader)
    {
        try
        {
            // Create instances ....
            for (Obj obj : serialized.getObjects())
            {
                if (obj instanceof Enum)
                {
                    CoreInstance instance = instanceBuilder.newEnumCoreInstance(obj.getClassifier(), obj.getName(), obj.getSourceInformation(), classLoader);
                    metadata.add(obj.getClassifier(), obj.getName(), instance);
                }
                else
                {
                    CoreInstance instance = instanceBuilder.newCoreInstance(obj.getClassifier(), obj.getName(), obj.getSourceInformation(), classLoader);
                    metadata.add(obj.getClassifier(), obj.getIdentifier(), instance);
                }
            }

            if ((resultExcluded != null) && excluded.notEmpty())
            {
                ModelRepository repository = excluded.getFirst().getRepository();
                ProcessorSupport processorSupport = new M3ProcessorSupport(repository);
                addExclude(repository.getTopLevel(M3Paths.Root), metadata, resultExcluded, processorSupport);
                for (Object excludedOneO : excluded)
                {
                    CoreInstance excludedOne = (CoreInstance)excludedOneO;
                    addExclude(excludedOne, metadata, resultExcluded, processorSupport);
                }
            }

            // Manage properties
            for (Obj obj : serialized.getObjects())
            {
                CoreInstance inst = null;
                MapIterable<String, Field> fields = null;
                if (obj instanceof Enum)
                {
                    inst = metadata.getMetadata(obj.getClassifier(), obj.getName());
                    fields = instanceBuilder.getFields(EnumProcessor.ENUM_CLASS_NAME);
                }
                else
                {
                    inst = metadata.getMetadata(obj.getClassifier(), obj.getIdentifier());
                    fields = instanceBuilder.getFields(obj.getClassifier());
                }
                for (PropertyValue val : obj.getPropertyValues())
                {
                    Field f = fields.get("_" + val.getProperty());
                    f.setAccessible(true);
                    if (val instanceof PropertyValueOne)
                    {
                        f.set(inst, getRValueValue(((PropertyValueOne)val).getValue(), metadata, classLoader));
                    }
                    else
                    {
                        ListIterable<RValue> values = ((PropertyValueMany)val).getValues();
                        MutableList<Object> newList = FastList.newList(values.size());
                        f.set(inst, newList);
                        for (RValue rValue : values)
                        {
                            newList.add(getRValueValue(rValue, metadata, classLoader));
                        }
                    }
                }
            }

            for (Pair<Obj, Obj> pair : serialized.getPackageLinks())
            {
                Obj packageObj = pair.getOne();
                Obj elementObj = pair.getTwo();
                metadata.addChild(packageObj.getClassifier(), packageObj.getIdentifier(), elementObj.getClassifier(), elementObj.getIdentifier());
            }
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException("Error building compiled mode graph", e);
        }
    }

    public static int serializeAllToMetadata(Iterable<? extends CoreInstance> startingNodes, final MetadataEager metadata, final CompiledCoreInstanceBuilder instanceBuilder,
                                             SetIterable<? extends CoreInstance> excluded, MutableSet<? super CoreInstance> resultExcluded, final ClassLoader classLoader, final ProcessorSupport processorSupport)
    {
        // Collect all nodes
        PrivateSetSearchState state = new PrivateSetSearchStateWithCompileStateMarking(Stacks.mutable.withAll(startingNodes), processorSupport);
        while (state.hasNodes())
        {
            CoreInstance instance = state.nextNode();
            if (state.shouldVisit(instance))
            {
                state.noteVisited(instance);
                for (String key : instance.getKeys())
                {
                    for (CoreInstance value : Instance.getValueForMetaPropertyToManyResolved(instance, key, processorSupport))
                    {
                        if (!state.isPrimitiveType(value.getClassifier()))
                        {
                            state.addNode(value);
                        }
                    }
                }
            }
        }
        ListIterable<CoreInstance> nodes = FastList.<CoreInstance>newList(state.visited.size()).withAll(state.visited);
        state = null; // allow garbage collection

        // Initialize metadata
        final ClassifierCaches classifierCaches = new ClassifierCaches(processorSupport);
        for (CoreInstance node : nodes)
        {
            CoreInstance classifier = node.getClassifier();
            String classifierId = classifierCaches.getClassifierId(classifier);
            if (classifierCaches.isEnumeration(classifier))
            {
                CoreInstance instance = instanceBuilder.newEnumCoreInstance(classifierId, node.getName(), node.getSourceInformation(), classLoader);
                metadata.add(classifierId, node.getName(), instance);
            }
            else
            {
                CoreInstance instance = instanceBuilder.newCoreInstance(classifierId, node.getName(), node.getSourceInformation(), classLoader);
                metadata.add(classifierId, IdBuilder.buildId(node, processorSupport), instance);
            }
        }

        if ((resultExcluded != null) && excluded.notEmpty())
        {
            ModelRepository repository = excluded.getFirst().getRepository();
            addExclude(repository.getTopLevel(M3Paths.Root), metadata, resultExcluded, processorSupport);
            for (Object excludedOneO : excluded)
            {
                CoreInstance excludedOne = (CoreInstance)excludedOneO;
                addExclude(excludedOne, metadata, resultExcluded, processorSupport);
            }
        }

        // Fill in property values
        Function<CoreInstance, Object> processValue = new Function<CoreInstance, Object>()
        {
            @Override
            public Object valueOf(CoreInstance value)
            {
                CoreInstance classifier = value.getClassifier();
                if (classifierCaches.isPrimitiveType(classifier))
                {
                    return processPrimitiveTypeJava(value, processorSupport);
                }
                if (classifierCaches.isEnumeration(classifier))
                {
                    return metadata.getEnum(classifierCaches.getClassifierId(classifier), value.getName());
                }
                return metadata.getMetadata(classifierCaches.getClassifierId(classifier), IdBuilder.buildId(value, processorSupport));
            }
        };
        try
        {
            for (CoreInstance node : nodes)
            {
                CoreInstance classifier = node.getClassifier();
                String classifierId = classifierCaches.getClassifierId(classifier);

                CoreInstance inst = null;
                MapIterable<String, Field> fields = null;
                if (classifierCaches.isEnumeration(classifier))
                {
                    inst = metadata.getMetadata(classifierId, node.getName());
                    fields = instanceBuilder.getFields(EnumProcessor.ENUM_CLASS_NAME);
                }
                else
                {
                    inst = metadata.getMetadata(classifierId, IdBuilder.buildId(node, processorSupport));
                    fields = instanceBuilder.getFields(classifierId);
                }

                for (String key : node.getKeys())
                {
                    ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(node, key, processorSupport);
                    if (values.notEmpty())
                    {
                        Field f = fields.get("_" + key);
                        f.setAccessible(true);

                        CoreInstance property = node.getKeyByName(key);
                        if (Multiplicity.isToOne(Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.multiplicity, processorSupport), false))
                        {
                            CoreInstance value = values.get(0);
                            Object processedValue = processValue.valueOf(value);
                            f.set(inst, processedValue);
                        }
                        else
                        {
                            MutableList<Object> newList = FastList.newList(values.size());
                            f.set(inst, newList);
                            values.collect(processValue, newList);
                        }
                    }
                }
            }
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }

        return nodes.size();
    }

    private static Object getRValueValue(RValue rValue, Metadata metadata, ClassLoader classLoader)
    {
        if (rValue instanceof Primitive)
        {
            return ((Primitive)rValue).getValue();
        }
        if (rValue instanceof EnumRef)
        {
            EnumRef enumRef = (EnumRef)rValue;
            return metadata.getEnum(enumRef.getEnumerationId(), enumRef.getEnumName());
        }
        if (rValue instanceof ObjRef)
        {
            ObjRef objRef = (ObjRef)rValue;
            Object result = metadata.getMetadata(objRef.getClassifierId(), objRef.getId());
            if (result == null)
            {
                throw new RuntimeException("ERROR: cannot find id " + objRef.getId() + " for type " + objRef.getClassifierId());
            }
            return result;
        }
        throw new RuntimeException("Unhandled RValue: " + rValue);
    }

    private static void addExclude(CoreInstance excludedOne, Metadata metadata, MutableSet<? super CoreInstance> resultExcluded, ProcessorSupport processorSupport)
    {
        if ((resultExcluded != null) && (excludedOne != null))
        {
            String classifierId = MetadataJavaPaths.buildMetadataKeyFromType(excludedOne.getClassifier());
            String identifier = IdBuilder.buildId(excludedOne, processorSupport);
            CoreInstance resultExcludedOne = metadata.getMetadata(classifierId, identifier);
            if (resultExcludedOne != null)
            {
                resultExcluded.add(resultExcludedOne);
            }

            for (CoreInstance excl : excludedOne.getValueForMetaPropertyToMany(M3Properties.children))
            {
                String exclClassifierId = MetadataJavaPaths.buildMetadataKeyFromType(excl.getClassifier());
                String exclIdentifier = IdBuilder.buildId(excl, processorSupport);
                CoreInstance resultExcl = metadata.getMetadata(exclClassifierId, exclIdentifier);
                if (resultExcl != null)
                {
                    resultExcluded.add(resultExcl);
                }
            }
        }
    }

    private static SearchState newCompiledStateSearchState(CoreInstance node, ProcessorSupport processorSupport)
    {
        return new CompiledStateSearchState(Stacks.mutable.with(node), processorSupport);
    }

    private static SearchState newCompiledStateSearchState(Iterable<? extends CoreInstance> nodes, ProcessorSupport processorSupport)
    {
        return new CompiledStateSearchState(Stacks.mutable.withAll(nodes), processorSupport);
    }

    private static SearchState newPrivateSetSearchState(CoreInstance node, ProcessorSupport processorSupport, boolean addSerializedCompileState)
    {
        return newPrivateSetSearchState(Stacks.mutable.with(node), processorSupport, addSerializedCompileState);
    }

    private static SearchState newPrivateSetSearchState(Iterable<? extends CoreInstance> nodes, ProcessorSupport processorSupport, boolean addSerializedCompileState)
    {
        return newPrivateSetSearchState(Stacks.mutable.withAll(nodes), processorSupport, addSerializedCompileState);
    }

    private static SearchState newPrivateSetSearchState(MutableStack<CoreInstance> stack, ProcessorSupport processorSupport, boolean addSerializedCompileState)
    {
        return addSerializedCompileState ? new PrivateSetSearchStateWithCompileStateMarking(stack, processorSupport) : new PrivateSetSearchState(stack, processorSupport);
    }

    public static class ClassifierCaches
    {
        private final Function<CoreInstance, String> newClassifierId = new Function<CoreInstance, String>()
        {
            @Override
            public String valueOf(CoreInstance classifier)
            {
                return MetadataJavaPaths.buildMetadataKeyFromType(classifier).intern();
            }
        };

        private final BooleanFunction<CoreInstance> isEnumeration = new BooleanFunction<CoreInstance>()
        {
            @Override
            public boolean booleanValueOf(CoreInstance classifier)
            {
                return classifier.getClassifier() == ClassifierCaches.this.enumerationClass;
            }
        };

        private final SetIterable<CoreInstance> primitiveTypes;
        private final CoreInstance enumerationClass;
        private final MutableObjectBooleanMap<CoreInstance> enumerationCache = ObjectBooleanMaps.mutable.empty();
        private final MutableMap<CoreInstance, String> classifierIdCache = Maps.mutable.empty();

        public ClassifierCaches(ProcessorSupport processorSupport)
        {
            this.enumerationClass = _Package.getByUserPath(M3Paths.Enumeration, processorSupport);
            this.primitiveTypes = PrimitiveUtilities.getPrimitiveTypes(processorSupport).toSet();
        }

        boolean isEnum(CoreInstance instance)
        {
            return isEnumeration(instance.getClassifier());
        }

        boolean isPrimitiveType(CoreInstance classifier)
        {
            return this.primitiveTypes.contains(classifier);
        }

        boolean isEnumeration(CoreInstance classifier)
        {
            return this.enumerationCache.getIfAbsentPutWithKey(classifier, this.isEnumeration);
        }

        public String getClassifierId(CoreInstance classifier)
        {
            return this.classifierIdCache.getIfAbsentPutWithKey(classifier, this.newClassifierId);
        }
    }

    private abstract static class SearchState extends ClassifierCaches
    {
        private final MutableStack<CoreInstance> stack;

        private SearchState(MutableStack<CoreInstance> stack, ProcessorSupport processorSupport)
        {
            super(processorSupport);
            this.stack = stack;
        }

        boolean hasNodes()
        {
            return this.stack.notEmpty();
        }

        CoreInstance nextNode()
        {
            return this.stack.pop();
        }

        void addNode(CoreInstance node)
        {
            this.stack.push(node);
        }

        boolean shouldVisit(CoreInstance node)
        {
            return !hasVisited(node) && !isPrimitiveType(node.getClassifier());
        }

        abstract boolean hasVisited(CoreInstance node);

        abstract void noteVisited(CoreInstance node);
    }

    private static class CompiledStateSearchState extends SearchState
    {
        private CompiledStateSearchState(MutableStack<CoreInstance> stack, ProcessorSupport processorSupport)
        {
            super(stack, processorSupport);
        }

        @Override
        boolean hasVisited(CoreInstance node)
        {
            return node.hasCompileState(SERIALIZED);
        }

        @Override
        void noteVisited(CoreInstance node)
        {
            node.addCompileState(SERIALIZED);
        }
    }

    private static class PrivateSetSearchState extends SearchState
    {
        private final MutableSet<CoreInstance> visited = Sets.mutable.with();

        private PrivateSetSearchState(MutableStack<CoreInstance> stack, ProcessorSupport processorSupport)
        {
            super(stack, processorSupport);
        }

        @Override
        boolean hasVisited(CoreInstance node)
        {
            return this.visited.contains(node);
        }

        @Override
        void noteVisited(CoreInstance node)
        {
            this.visited.add(node);
        }
    }

    private static class PrivateSetSearchStateWithCompileStateMarking extends PrivateSetSearchState
    {
        private PrivateSetSearchStateWithCompileStateMarking(MutableStack<CoreInstance> stack, ProcessorSupport processorSupport)
        {
            super(stack, processorSupport);
        }

        @Override
        void noteVisited(CoreInstance node)
        {
            super.noteVisited(node);
            node.addCompileState(SERIALIZED);
        }
    }
}
