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

package org.finos.legend.pure.m3.compiler;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.compiler.visibility.AccessLevel;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.ConcurrentHashSet;

import java.util.function.Supplier;

public class Context
{
    private final ConcurrentMutableMap<String, CoreInstance> coreInstanceByPath = ConcurrentHashMap.newMap();
    private final ConcurrentMutableMap<CoreInstance, CoreInstance> functionTypes = ConcurrentHashMap.newMap();
    private final ConcurrentMutableMap<CoreInstance, ImmutableMap<String, CoreInstance>> classPropertiesByName = ConcurrentHashMap.newMap();
    private final ConcurrentMutableMap<CoreInstance, ImmutableList<CoreInstance>> generalizations = ConcurrentHashMap.newMap();
    private final ConcurrentMutableMap<CoreInstance, AccessLevel> accessLevels = ConcurrentHashMap.newMap();
    private final ConcurrentMutableMap<CoreInstance, ImmutableList<String>> propertyPaths = ConcurrentHashMap.newMap();
    private final ConcurrentMutableMap<CoreInstance, MutableSet<CoreInstance>> instancesByClassifier = ConcurrentHashMap.newMap();
    private final ConcurrentMutableMap<String, MutableSet<CoreInstance>> functionsByName = ConcurrentHashMap.newMap();
    private final ConcurrentMutableMap<CoreInstance, ImmutableSet<CoreInstance>> typeGeneralizationSets = ConcurrentHashMap.newMap();

    private CoreInstance anyType;
    private CoreInstance nilType;

    /**
     * Get the access level for a packageable element.  If it is not currently
     * stored in the context, then compute it
     *
     * @param element   packageable element
     * @param generator function to get the element's access level
     * @return access level
     */
    public AccessLevel getIfAbsentPutAccessLevel(CoreInstance element, Function<? super CoreInstance, AccessLevel> generator)
    {
        return element.isPersistent() ? this.accessLevels.getIfAbsentPutWithKey(element, generator) : generator.valueOf(element);
    }

    /**
     * Get the generalizations for type.  If this is not currently stored
     * in the context, then calculate it by evaluating generator on type,
     * store it, and return it.  This is an atomic operation.
     *
     * @param type      type
     * @param generator function to calculate generalizations if not present
     * @return generalizations of type
     */
    public ImmutableList<CoreInstance> getIfAbsentPutGeneralizations(CoreInstance type, Function<? super CoreInstance, ? extends ImmutableList<CoreInstance>> generator)
    {
        return type.isPersistent() ? this.generalizations.getIfAbsentPutWithKey(type, generator) : generator.valueOf(type);
    }

    /**
     * Get the properties of a class, including those inherited from
     * generalizations, indexed by name.  If this is not stored in the
     * context, then calculate it by evaluating generator on classifier,
     * store it, and return it.  This is an atomic operation.
     *
     * @param classifier class
     * @param generator  function to compute properties by name
     * @return properties by name
     */
    public ImmutableMap<String, CoreInstance> getIfAbsentPutPropertiesByName(CoreInstance classifier, Function<? super CoreInstance, ? extends ImmutableMap<String, CoreInstance>> generator)
    {
        return classifier.isPersistent() ? this.classPropertiesByName.getIfAbsentPutWithKey(classifier, generator) : generator.valueOf(classifier);
    }

    /**
     * Get the path for a property.  If this is not stored in the
     * context, then calculate it by evaluating generator on property,
     * store it, and return it.  This is an atomic operation.
     *
     * @param property  property
     * @param generator function to compute the property path
     * @return property path
     */
    public ImmutableList<String> getIfAbsentPutPropertyPath(CoreInstance property, Function<? super CoreInstance, ? extends ImmutableList<String>> generator)
    {
        return property.isPersistent() ? this.propertyPaths.getIfAbsentPutWithKey(property, generator) : generator.valueOf(property);
    }

    /**
     * Get a Pure element by its path.  If this is not stored in the context,
     * find it by evaluating function on the path, store it in the context if
     * non-null, and return it.  This is an atomic operation.
     *
     * @param path    element path
     * @param factory function to find the element
     * @return element with the given path
     */
    public CoreInstance getIfAbsentPutElementByPath(String path, Supplier<? extends CoreInstance> factory)
    {
        CoreInstance instance = this.coreInstanceByPath.get(path);
        if (instance == null)
        {
            CoreInstance newInstance = factory.get();
            if (newInstance != null)
            {
                instance = this.coreInstanceByPath.getIfAbsentPut(path, newInstance);
            }
        }
        return instance;
    }

    /**
     * Get the type of a function.  If this is not stored in the context,
     * calculate it by evaluating generator on function, store it in the
     * context, and return it.  This is an atomic operation.
     *
     * @param function  Pure function
     * @param generator function to find the function type
     * @return function type
     */
    public CoreInstance getIfAbsentPutFunctionType(CoreInstance function, Function<? super CoreInstance, ? extends CoreInstance> generator)
    {
        return function.isPersistent() ? this.functionTypes.getIfAbsentPutWithKey(function, generator) : generator.valueOf(function);
    }

    public ImmutableSet<CoreInstance> getIfAbsentPutTypeGeneralizationSet(CoreInstance type, Function<CoreInstance, ? extends ImmutableSet<CoreInstance>> generator)
    {
        return type.isPersistent() ? this.typeGeneralizationSets.getIfAbsentPutWithKey(type, generator) : generator.valueOf(type);
    }

    /**
     * Register the given instance by its classifier.  Throws an
     * exception if the classifier is null.
     *
     * @param instance Pure instance
     */
    public void registerInstanceByClassifier(CoreInstance instance)
    {
        CoreInstance classifier = instance.getClassifier();
        if (classifier == null)
        {
            throw new IllegalArgumentException("Null classifier for " + instance);
        }
        this.instancesByClassifier.getIfAbsentPut(classifier, ConcurrentHashSet::newSet).add(instance);
    }

    /**
     * Register several instances by their classifiers.  Throws
     * an exception if any instance has a null classifier.  In
     * that case, the context is not updated at all.
     *
     * @param instances Pure instances
     */
    public void registerInstancesByClassifier(Iterable<? extends CoreInstance> instances)
    {
        instances.forEach(this::registerInstanceByClassifier);
    }

    /**
     * Get all the instances of classifier registered in the context.
     *
     * @param classifier Pure classifier
     * @return classifier instances
     */
    public SetIterable<CoreInstance> getClassifierInstances(CoreInstance classifier)
    {
        MutableSet<CoreInstance> instances = this.instancesByClassifier.get(classifier);
        return (instances == null) ? Sets.immutable.empty() : instances.asUnmodifiable();
    }

    /**
     * Get all instances registered to any classifier in the context.
     *
     * @return all instances registered to any classifier
     */
    public SetIterable<CoreInstance> getAllInstances()
    {
        MutableSet<CoreInstance> allInstances = Sets.mutable.empty();
        this.instancesByClassifier.valuesView().forEach(allInstances::addAll);
        return allInstances;
    }

    public MapIterable<String, Integer> countInstancesByClassifier()
    {
        MutableMap<String, Integer> result = Maps.mutable.ofInitialCapacity(this.instancesByClassifier.size());
        this.instancesByClassifier.forEachKeyValue((classifier, instances) -> result.put(PackageableElement.getUserPathForPackageableElement(classifier), instances.size()));
        return result;
    }

    /**
     * Register a function by its name.  If the function does not
     * have a name, it is not registered.  Similarly, if the instance
     * passed in is not actually a function, it is not registered.
     *
     * @param function Pure function
     */
    public void registerFunctionByName(CoreInstance function)
    {
        String functionName = getFunctionName(function);
        if (functionName != null)
        {
            registerFunctionByName(functionName, function);
        }
    }

    public void registerFunctionByName(String functionName, CoreInstance function)
    {
        this.functionsByName.getIfAbsentPut(functionName, ConcurrentHashSet::newSet).add(function);
    }

    public void registerFunctionsByName(String functionName, Iterable<? extends CoreInstance> functions)
    {
        if (Iterate.notEmpty(functions))
        {
            this.functionsByName.getIfAbsentPut(functionName, ConcurrentHashSet::newSet).addAllIterable(functions);
        }
    }

    /**
     * Register several functions by name.  Any function which
     * does not have a name is not registered.  Similarly, any
     * non-function passed in is not registered.
     *
     * @param functions Pure functions
     */
    public void registerFunctionsByName(Iterable<? extends CoreInstance> functions)
    {
        functions.forEach(this::registerFunctionByName);
    }

    /**
     * Get all the functions with the given name.
     *
     * @param functionName function name
     * @return functions with the given name
     */
    public SetIterable<CoreInstance> getFunctionsForName(String functionName)
    {
        MutableSet<CoreInstance> functions = this.functionsByName.get(functionName);
        return (functions == null) ? Sets.immutable.empty() : functions.asUnmodifiable();
    }

    /**
     * Get all of the function names that functions are
     * registered for.
     *
     * @return function names
     */
    public RichIterable<String> getAllFunctionNames()
    {
        return this.functionsByName.keysView();
    }

    public CoreInstance getIfAbsentPutAny(Supplier<? extends CoreInstance> factory)
    {
        if (this.anyType == null)
        {
            this.anyType = getIfAbsentPutElementByPath(M3Paths.Any, factory);
        }
        return this.anyType;
    }

    public CoreInstance getIfAbsentPutNil(Supplier<? extends CoreInstance> factory)
    {
        if (this.nilType == null)
        {
            this.nilType = getIfAbsentPutElementByPath(M3Paths.Nil, factory);
        }
        return this.nilType;
    }

    /**
     * Remove the given instance from the context.
     *
     * @param coreInstance instance to remove
     */
    public void remove(CoreInstance coreInstance)
    {
        CoreInstance funcName = coreInstance.getValueForMetaPropertyToOne(M3Properties.functionName);
        if (funcName != null)
        {
            MutableSet<CoreInstance> functions = this.functionsByName.get(funcName.getName());
            if (functions != null)
            {
                functions.remove(coreInstance);
            }
        }
        MutableSet<CoreInstance> instances = this.instancesByClassifier.get(coreInstance.getClassifier());
        if (instances != null)
        {
            instances.remove(coreInstance);
        }
        this.update(coreInstance);
    }

    /**
     * Update the context for the given instance.  This clears
     * any information cached about it.
     *
     * @param coreInstance instance to update
     */
    public void update(CoreInstance coreInstance)
    {
        this.coreInstanceByPath.remove(PackageableElement.getUserPathForPackageableElement(coreInstance));
        this.functionTypes.remove(coreInstance);
        this.accessLevels.remove(coreInstance);
        // If there is a change to the generalization hierarchy, then we have to invalidate everything depending on the hierarchy.
        if (this.generalizations.containsKey(coreInstance))
        {
            this.classPropertiesByName.clear();
            this.generalizations.clear();
            this.propertyPaths.clear();
            this.typeGeneralizationSets.clear();
        }
        if (this.anyType == coreInstance)
        {
            this.anyType = null;
        }
        if (this.nilType == coreInstance)
        {
            this.nilType = null;
        }
    }

    /**
     * Clear the context.
     */
    public void clear()
    {
        this.coreInstanceByPath.clear();
        this.functionTypes.clear();
        this.classPropertiesByName.clear();
        this.functionsByName.clear();
        this.generalizations.clear();
        this.accessLevels.clear();
        this.propertyPaths.clear();
        this.instancesByClassifier.clear();
        this.typeGeneralizationSets.clear();
        this.anyType = null;
        this.nilType = null;
    }

    private static String getFunctionName(CoreInstance function)
    {
        CoreInstance functionName = function.getValueForMetaPropertyToOne(M3Properties.functionName);

        // If there is no function name, return null
        if (functionName == null)
        {
            return null;
        }

        // If this is not the functionName property from Function, return null
        if (!M3PropertyPaths.functionName_Function.equals(function.getRealKeyByName(M3Properties.functionName)))
        {
            return null;
        }

        // If this is not in a package, return null
        if (function.getValueForMetaPropertyToOne(M3Properties._package) == null)
        {
            return null;
        }

        return PrimitiveUtilities.getStringValue(functionName);
    }
}
