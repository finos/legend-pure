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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.InstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.imports.Imports;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public final class ModelJoinShared
{
    private ModelJoinShared()
    {
        // utility class
    }

    // -------------------------------------------------------------------------
    // Variable renaming
    // -------------------------------------------------------------------------
    static void applyRename(VariableExpression ve, String newName, GenericType newType)
    {
        ve._name(newName);
        if (newType != null)
        {
            ve._genericType(newType);
        }
        else
        {
            ve._genericTypeRemove();
        }
    }

    /**
     * Recursively walks a ValueSpecification tree and replaces VariableExpression nodes:
     * {@code oldName1 → newName1} (with type1) and {@code oldName2 → newName2} (with type2).
     * If a GenericType is null, the generic type on the VariableExpression is removed.
     * Within a nested lambda whose own parameter shadows an old name, references to that name
     * are the nested parameter's, and are left alone.
     */
    public static void replaceVariableReferences(ValueSpecification vs, String oldName1, String newName1, GenericType type1,
                                                 String oldName2, String newName2, GenericType type2)
    {
        // The test never accepts, so every node is visited.
        findFirst(vs, Sets.immutable.empty(), (node, boundNames) ->
        {
            if (node instanceof VariableExpression && !boundNames.contains(((VariableExpression) node)._name()))
            {
                VariableExpression ve = (VariableExpression) node;
                if (ve._name().equals(oldName1))
                {
                    applyRename(ve, newName1, type1);
                }
                else if (ve._name().equals(oldName2))
                {
                    applyRename(ve, newName2, type2);
                }
            }
            return false;
        });
    }

    // -------------------------------------------------------------------------
    // Expression-tree search
    // -------------------------------------------------------------------------

    /**
     * Depth-first search of an expression tree -- through function parameters, instance values, the values of
     * {@code ^Class(key=value)} key expressions and lambda bodies -- returning the first node {@code test}
     * accepts, or null. {@code test} also receives the names bound at that node: {@code boundNames} plus the
     * parameters of every enclosing nested lambda, so it can tell a free variable from one that a nested
     * lambda's own parameter shadows.
     */
    public static CoreInstance findFirst(CoreInstance node, ImmutableSet<String> boundNames, Predicate2<? super CoreInstance, ? super ImmutableSet<String>> test)
    {
        if (test.accept(node, boundNames))
        {
            return node;
        }
        Iterable<? extends CoreInstance> children;
        ImmutableSet<String> childBoundNames = boundNames;
        if (node instanceof FunctionExpression)
        {
            children = ((FunctionExpression) node)._parametersValues();
        }
        else if (node instanceof InstanceValue)
        {
            children = ((InstanceValue) node)._valuesCoreInstance();
        }
        else if (node instanceof KeyExpression)
        {
            // The key is just the property name; only the value can hold variables or calls.
            children = Lists.immutable.with(((KeyExpression) node)._expression());
        }
        else if (node instanceof LambdaFunction)
        {
            children = ((LambdaFunction<?>) node)._expressionSequence();
            childBoundNames = boundNames.newWithAll(lambdaParamNames((LambdaFunction<?>) node));
        }
        else
        {
            return null;
        }
        for (CoreInstance child : children)
        {
            CoreInstance found = findFirst(child, childBoundNames, test);
            if (found != null)
            {
                return found;
            }
        }
        return null;
    }

    // A lambda's FunctionType is anonymous, never behind an ImportStub.
    private static SetIterable<String> lambdaParamNames(LambdaFunction<?> lambda)
    {
        GenericType classifierGenericType = lambda._classifierGenericType();
        CoreInstance functionType = (classifierGenericType == null || classifierGenericType._typeArguments().isEmpty()) ? null : classifierGenericType._typeArguments().getFirst()._rawTypeCoreInstance();
        return (functionType instanceof FunctionType) ? ((FunctionType) functionType)._parameters().collect(VariableExpression::_name, Sets.mutable.<String>empty()) : Sets.immutable.<String>empty();
    }

    /**
     * Renames a lambda's two parameters (both at the {@link FunctionType} declaration site
     * and at every reference inside the expression body).
     */
    public static void renameLambdaParamsAndBody(LambdaFunction<?> lambda,
                                                 String oldName1, String newName1, GenericType type1,
                                                 String oldName2, String newName2, GenericType type2,
                                                 ProcessorSupport processorSupport)
    {
        FunctionType fType = (FunctionType) ImportStub.withImportStubByPass(
                lambda._classifierGenericType()._typeArguments().getOnly()._rawTypeCoreInstance(), processorSupport);
        for (VariableExpression param : fType._parameters())
        {
            String currentName = param._name();
            if (oldName1.equals(currentName))
            {
                applyRename(param, newName1, type1);
            }
            else if (oldName2.equals(currentName))
            {
                applyRename(param, newName2, type2);
            }
        }

        for (ValueSpecification expr : lambda._expressionSequence())
        {
            replaceVariableReferences(expr, oldName1, newName1, type1, oldName2, newName2, type2);
        }
    }

    // -------------------------------------------------------------------------
    // Structural pre-match disambiguation (modelJoin / navigationJoin / filterJoin)
    // -------------------------------------------------------------------------

    /**
     * The function name a call was written with, minus any package qualifier: "navigationJoin" for both
     * {@code navigationJoin(...)} and {@code meta::pure::functions::relation::navigationJoin(...)}.
     */
    public static String simpleFunctionName(String functionName)
    {
        int separator = functionName.lastIndexOf("::");
        return separator == -1 ? functionName : functionName.substring(separator + 2);
    }

    /**
     * True if a call written with {@code functionName} -- bare, or package-qualified -- names the platform
     * function of that name in {@code platformPackagePath}, following the name resolution of
     * FunctionExpressionMatcher: a qualified name must name the platform package itself; a bare name must
     * see, among the packages visible from {@code importGroup} (its imports plus Root), only same-named
     * functions in the platform package. Used by the structural pre-match detection of
     * modelJoin/navigationJoin/filterJoin, which must run before the normal matcher (and so before
     * $src/$row are registered), so that a user's own same-named function isn't mistaken for the platform
     * one. Full overload resolution is not replicated, so a visible same-named user function always
     * disqualifies a bare call, whatever its signature.
     */
    public static boolean isCallToPlatformFunction(String functionName, CoreInstance importGroup, String platformPackagePath, ProcessorSupport processorSupport)
    {
        int separator = functionName.lastIndexOf("::");
        if (separator != -1)
        {
            return platformPackagePath.equals(functionName.substring(0, separator));
        }
        ListIterable<PackageableFunction<?>> visible = findVisibleFunctions(functionName, importGroup, processorSupport);
        return visible.notEmpty() && visible.allSatisfy(f -> isInPackage(f, platformPackagePath));
    }

    /**
     * The functions named {@code bareFunctionName} that a bare call parsed under {@code importGroup} can
     * see: those in an imported package (including the core imports) or in Root, as for
     * FunctionExpressionMatcher.
     */
    public static ListIterable<PackageableFunction<?>> findVisibleFunctions(String bareFunctionName, CoreInstance importGroup, ProcessorSupport processorSupport)
    {
        RichIterable<CoreInstance> candidates = processorSupport.function_getFunctionsForName(bareFunctionName);
        if (candidates.isEmpty() || !(importGroup instanceof ImportGroup))
        {
            return Lists.immutable.empty();
        }
        SetIterable<? extends CoreInstance> importedPackages = Imports.getImportGroupPackages((ImportGroup) importGroup, processorSupport);
        CoreInstance root = processorSupport.repository_getTopLevel(M3Paths.Root);
        MutableList<PackageableFunction<?>> visible = Lists.mutable.empty();
        for (CoreInstance candidate : candidates)
        {
            PackageableFunction<?> function = (PackageableFunction<?>) candidate;
            CoreInstance functionPackage = function._package();
            if (functionPackage == root || importedPackages.contains(functionPackage))
            {
                visible.add(function);
            }
        }
        return visible;
    }

    public static boolean isInPackage(PackageableFunction<?> function, String packagePath)
    {
        return function._package() != null && packagePath.equals(PackageableElement.getUserPathForPackageableElement(function._package()));
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
     * Packageable elements, primitive types are shared references.
     */
    private static boolean isSharedReference(CoreInstance node, ProcessorSupport processorSupport)
    {
        CoreInstance classifier = node.getClassifier();
        if (classifier != null && Instance.instanceOf(classifier, M3Paths.PrimitiveType, processorSupport))
        {
            return true;
        }

        return Instance.instanceOf(node, M3Paths.PackageableElement, processorSupport);
    }

    public static MutableList<InstanceSetImplementation> findAllSetsForClassOrSubtypes(CoreInstance clazz, Mapping mapping, ProcessorSupport processorSupport)
    {
        MutableList<InstanceSetImplementation> result = Lists.mutable.empty();
        forEachMappingInIncludeGraph(mapping, processorSupport, m ->
        {
            for (SetImplementation si : m._classMappings())
            {
                if (si instanceof InstanceSetImplementation)
                {
                    CoreInstance mappedClass = ImportStub.withImportStubByPass(si._classCoreInstance(), processorSupport);
                    if (mappedClass.equals(clazz) || Type.subTypeOf(mappedClass, clazz, processorSupport))
                    {
                        result.add((InstanceSetImplementation) si);
                    }
                }
            }
        });
        return result;
    }

    /**
     * Visits {@code mapping} and then, depth-first, every mapping it (transitively) includes -- each
     * exactly once, so include cycles and diamonds are safe.
     */
    public static void forEachMappingInIncludeGraph(Mapping mapping, ProcessorSupport processorSupport, Procedure<? super Mapping> procedure)
    {
        forEachMappingInIncludeGraph(mapping, processorSupport, procedure, Sets.mutable.empty());
    }

    private static void forEachMappingInIncludeGraph(Mapping mapping, ProcessorSupport processorSupport, Procedure<? super Mapping> procedure, MutableSet<Mapping> visited)
    {
        if (!visited.add(mapping))
        {
            return;
        }
        procedure.value(mapping);

        for (MappingInclude include : mapping._includes())
        {
            Mapping includedMapping = (Mapping) ImportStub.withImportStubByPass(include._includedCoreInstance(), processorSupport);
            if (includedMapping != null)
            {
                forEachMappingInIncludeGraph(includedMapping, processorSupport, procedure, visited);
            }
        }
    }
}
