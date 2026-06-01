// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

/**
 * Shared utilities for ModelJoin processing:
 * <ul>
 *   <li>{@link #replaceVariableReferences} — replaces variable references inside a ValueSpecification tree.
 *       Used by both {@link ModelJoinProcessor} (forward rename to canonical names with types) and
 *       the unbinder (reverse rename back to user-provided names, clearing types).</li>
 *   <li>{@link #deepClone} — deep-clones a LambdaFunction's CoreInstance graph, producing fresh nodes
 *       that have never been visited by the compiler's Matcher. References to packageable elements
 *       (Class, Property, Association, Multiplicity, ImportStub, etc.) are shared — only structural
 *       nodes (ValueSpecification subtypes, GenericType wrappers, FunctionType) are cloned.
 *       This ensures each ModelJoin property mapping gets an independent expression tree that can be
 *       compiled against a different set of variable types.</li>
 * </ul>
 */
public final class ModelJoinShared
{
    private ModelJoinShared()
    {
        // utility class
    }

    // -------------------------------------------------------------------------
    // Variable renaming
    // -------------------------------------------------------------------------

    /**
     * Recursively walks a ValueSpecification tree and replaces VariableExpression nodes:
     * {@code oldName1 → newName1} (with type1) and {@code oldName2 → newName2} (with type2).
     * If a GenericType is null, the generic type on the VariableExpression is removed.
     */
    public static void replaceVariableReferences(ValueSpecification vs, String oldName1, String newName1, GenericType type1,
                                                 String oldName2, String newName2, GenericType type2)
    {
        if (vs instanceof VariableExpression)
        {
            VariableExpression ve = (VariableExpression) vs;
            if (oldName1.equals(ve._name()))
            {
                ve._name(newName1);
                if (type1 != null)
                {
                    ve._genericType(type1);
                }
                else
                {
                    ve._genericTypeRemove();
                }
            }
            else if (oldName2.equals(ve._name()))
            {
                ve._name(newName2);
                if (type2 != null)
                {
                    ve._genericType(type2);
                }
                else
                {
                    ve._genericTypeRemove();
                }
            }
        }
        else if (vs instanceof FunctionExpression)
        {
            FunctionExpression fe = (FunctionExpression) vs;
            for (ValueSpecification param : fe._parametersValues())
            {
                replaceVariableReferences(param, oldName1, newName1, type1, oldName2, newName2, type2);
            }
        }
        else if (vs instanceof InstanceValue)
        {
            InstanceValue iv = (InstanceValue) vs;
            for (CoreInstance value : iv._valuesCoreInstance())
            {
                if (value instanceof ValueSpecification)
                {
                    replaceVariableReferences((ValueSpecification) value, oldName1, newName1, type1, oldName2, newName2, type2);
                }
                else if (value instanceof LambdaFunction)
                {
                    LambdaFunction<?> nestedLambda = (LambdaFunction<?>) value;
                    for (ValueSpecification expr : nestedLambda._expressionSequence())
                    {
                        replaceVariableReferences(expr, oldName1, newName1, type1, oldName2, newName2, type2);
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Deep cloning
    // -------------------------------------------------------------------------

    /**
     * Deep-clones the given CoreInstance tree. Packageable elements and
     * primitive values are shared; structural/anonymous nodes are cloned.
     */
    public static CoreInstance deepClone(CoreInstance instance, ProcessorSupport processorSupport)
    {
        MutableMap<CoreInstance, CoreInstance> cloneMap = Maps.mutable.empty();
        return cloneNode(instance, cloneMap, processorSupport);
    }

    private static CoreInstance cloneNode(CoreInstance node, MutableMap<CoreInstance, CoreInstance> cloneMap, ProcessorSupport processorSupport)
    {
        if (node == null)
        {
            return null;
        }

        // Already cloned — return the cached copy (handles shared sub-trees within the lambda)
        CoreInstance cached = cloneMap.get(node);
        if (cached != null)
        {
            return cached;
        }

        // Don't clone packageable elements (Class, Property, Association, Enumeration, etc.)
        // or primitives — they are singletons in the graph.
        if (isSharedReference(node, processorSupport))
        {
            return node;
        }

        // Create a fresh anonymous instance of the same classifier
        CoreInstance classifier = node.getClassifier();
        String classifierPath = PackageableElement.getUserPathForPackageableElement(classifier);
        CoreInstance clone = processorSupport.newAnonymousCoreInstance(node.getSourceInformation(), classifierPath);
        cloneMap.put(node, clone);

        // Copy all properties
        for (String key : node.getKeys())
        {
            ListIterable<? extends CoreInstance> values = node.getValueForMetaPropertyToMany(key);
            if (values.notEmpty())
            {
                ListIterable<String> realKey = clone.getRealKeyByName(key);
                if (realKey == null)
                {
                    realKey = node.getRealKeyByName(key);
                }
                for (CoreInstance value : values)
                {
                    CoreInstance clonedValue = cloneNode(value, cloneMap, processorSupport);
                    clone.addKeyValue(realKey, clonedValue);
                }
            }
        }

        return clone;
    }

    /**
     * Determines if a node is a shared reference that should NOT be cloned.
     * Packageable elements, primitive types, multiplicities, and ImportStubs
     * are shared references.
     */
    private static boolean isSharedReference(CoreInstance node, ProcessorSupport processorSupport)
    {
        // Primitive values (String, Integer, Boolean, etc.) — identified by having a
        // PrimitiveType classifier
        CoreInstance classifier = node.getClassifier();
        if (classifier != null && Instance.instanceOf(classifier, M3Paths.PrimitiveType, processorSupport))
        {
            return true;
        }

        // PackageableElement instances (Class, Property, Association, Enum, etc.)
        // but NOT anonymous structural nodes like GenericType, FunctionType, RelationType, VariableExpression, etc.
        if (Instance.instanceOf(node, M3Paths.PackageableElement, processorSupport)
                && !Instance.instanceOf(node, M3Paths.FunctionType, processorSupport)
                && !Instance.instanceOf(node, M3Paths.RelationType, processorSupport)
                && !Instance.instanceOf(node, M3Paths.LambdaFunction, processorSupport))
        {
            return true;
        }

        // Multiplicity instances are shared singletons
        return Instance.instanceOf(node, M3Paths.PackageableMultiplicity, processorSupport);
    }
}

