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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.BaseCompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;

public class CoreExtensionCompiled extends BaseCompiledExtension
{
    public CoreExtensionCompiled()
    {
        super(
                Lists.fixedSize.empty(),
                Lists.fixedSize.with(StringJavaSource.newStringJavaSource("org.finos.legend.pure.generated", "CoreGen",
                        "package org.finos.legend.pure.generated;\n" +
                                "\n" +
                                "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Bridge;\n" +
                                "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.LambdaCompiledExtended;\n" +
                                "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;\n" +
                                "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureFunction2;\n" +
                                "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction;\n" +
                                "import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataAccessor;\n" +
                                "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;\n" +
                                "import org.eclipse.collections.api.RichIterable;\n" +
                                "import org.eclipse.collections.api.block.function.Function;\n" +
                                "import org.eclipse.collections.api.block.function.Function0;\n" +
                                "import org.eclipse.collections.api.block.function.Function2;\n" +
                                "import org.eclipse.collections.api.block.function.Function3;\n" +
                                "import org.eclipse.collections.api.factory.Lists;\n" +
                                "import org.eclipse.collections.api.list.ListIterable;\n" +
                                "import org.eclipse.collections.impl.map.mutable.UnifiedMap;\n" +
                                "import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List;\n" +
                                "import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyExpression;\n" +
                                "import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyValue;\n" +
                                "import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.meta.CompilationResult;\n" +
                                "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;\n" +
                                "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;\n" +
                                "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;\n" +
                                "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ElementOverride;\n" +
                                "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;\n" +
                                "import org.finos.legend.pure.m3.exception.PureExecutionException;\n" +
                                "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n" +
                                "import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;\n" +
                                "import org.finos.legend.pure.m3.navigation.ProcessorSupport;\n" +
                                "import org.finos.legend.pure.m3.tools.ListHelper;\n" +
                                "import org.finos.legend.pure.m4.coreinstance.SourceInformation;\n" +
                                "\n" +
                                "public class CoreGen\n" +
                                "{\n" +
                                "    private static final Bridge bridge = new BridgeImpl();\n" +
                                "\n" +
                                "    public static Object alloyTest(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function alloyTest, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function regular)\n" +
                                "    {\n" +
                                "        return Pure.alloyTest(es, alloyTest, regular, bridge);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static Object legendTest(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function alloyTest, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function regular)\n" +
                                "    {\n" +
                                "        return Pure.legendTest(es, alloyTest, regular, bridge);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType safeGetGenericType(Object val, MetadataAccessor ma, ProcessorSupport processorSupport)\n" +
                                "    {\n" +
                                "        return Pure.safeGetGenericType(val, ma, () -> new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"\"), processorSupport);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static SharedPureFunction getSharedPureFunction(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function func, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return Pure.getSharedPureFunction(func, bridge, es);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static Object evaluate(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function func, Object... instances)\n" +
                                "    {\n" +
                                "        return Pure.evaluate(es, func, bridge, instances);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static <U, V> RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>> zip(Object l1, Object l2)\n" +
                                "    {\n" +
                                "        return Pure.zip(l1, l2, () -> new Root_meta_pure_functions_collection_Pair_Impl<U, V>(\"\"));\n" +
                                "    }\n" +
                                "\n" +
                                "    public static <U, V> RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>> zip(RichIterable<? extends U> l1, RichIterable<? extends V> l2)\n" +
                                "    {\n" +
                                "        return Pure.zip(l1, l2, () -> new Root_meta_pure_functions_collection_Pair_Impl<U, V>(\"\"));\n" +
                                "    }\n" +
                                "\n" +
                                "    public static Object evaluateToMany(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function func, RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List> instances)\n" +
                                "    {\n" +
                                "        return Pure.evaluateToMany(es, bridge, func, instances);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static PureMap getOpenVariables(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func)\n" +
                                "    {\n" +
                                "        return Pure.getOpenVariables(func, bridge);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static CompilationResult compileCodeBlock(String source, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return Pure.compileCodeBlock(source, () -> new Root_meta_pure_functions_meta_CompilationResult_Impl(\"\"), () -> new Root_meta_pure_functions_meta_CompilationFailure_Impl(\"\"), () -> new Root_meta_pure_functions_meta_SourceInformation_Impl(\"\"), es);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static RichIterable<CompilationResult> compileCodeBlocks(RichIterable<? extends String> sources, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return Pure.compileCodeBlocks(sources, () -> new Root_meta_pure_functions_meta_CompilationResult_Impl(\"\"), () -> new Root_meta_pure_functions_meta_CompilationFailure_Impl(\"\"), () -> new Root_meta_pure_functions_meta_SourceInformation_Impl(\"\"), es);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static Object dynamicMatchWith(Object obj, RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?>> funcs, Object var, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return Pure.dynamicMatchWith(obj, funcs, var, bridge, es);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static Object dynamicMatch(Object obj, RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?>> funcs, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return Pure.dynamicMatch(obj, funcs, bridge, es);\n" +
                                "    }\n" +
                                "\n" +
                                "    private static Object dynamicMatch(Object obj, RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?>> funcs, Object var, boolean isMatchWith, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return Pure.dynamicMatch(obj, funcs, var, isMatchWith, bridge, es);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static <T, V> RichIterable<T> removeDuplicates(RichIterable<T> list, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function keyFn, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function eqlFn, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return Pure.removeDuplicates(list, keyFn, eqlFn, bridge, es);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static PureMap newMap(RichIterable pairs, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return Pure.newMap(pairs, es);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static PureMap newMap(org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair p, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return Pure.newMap(p, es);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static PureMap newMap(RichIterable pairs, Property property, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return Pure.newMap(pairs, property, es);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static PureMap newMap(org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair pair, Property property, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return Pure.newMap(pair, property, es);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static PureMap newMap(RichIterable pairs, RichIterable properties, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return Pure.newMap(pairs, properties, bridge, es);\n" +
                                "    }\n" +
                                "\n" +
                                "\n" +
                                "    public static Object traceSpan(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function function, String operationName, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function funcToGetTags, boolean tagsCritical)\n" +
                                "    {\n" +
                                "        return Pure.traceSpan(es, function, operationName, funcToGetTags, tagsCritical, bridge);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static boolean canReactivateWithoutJavaCompilation(ValueSpecification valueSpecification, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return Pure.canReactivateWithoutJavaCompilation(valueSpecification, es, new PureMap(UnifiedMap.newMap()), bridge);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static boolean canReactivateWithoutJavaCompilation(ValueSpecification valueSpecification, ExecutionSupport es, PureMap lambdaOpenVariablesMap)\n" +
                                "    {\n" +
                                "        return Pure.canReactivateWithoutJavaCompilation(valueSpecification, es, lambdaOpenVariablesMap, bridge);\n" +
                                "    }\n" +
                                "\n" +
                                "\n" +
                                "    public static Object reactivate(ValueSpecification valueSpecification, PureMap lambdaOpenVariablesMap, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return Pure.reactivate(valueSpecification, lambdaOpenVariablesMap, true, bridge, es);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static Object reactivate(ValueSpecification valueSpecification, PureMap lambdaOpenVariablesMap, boolean allowJavaCompilation, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return Pure.reactivate(valueSpecification, lambdaOpenVariablesMap, allowJavaCompilation, bridge, es);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static Object newObject(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType genericType, RichIterable<? extends KeyValue> root_meta_pure_functions_lang_keyExpressions, ElementOverride override, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function getterToOne, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function getterToMany, Object payload, PureFunction2 getterToOneExec, PureFunction2 getterToManyExec, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return Pure.newObject(genericType, root_meta_pure_functions_lang_keyExpressions, override, getterToOne, getterToMany, payload, getterToOneExec, getterToManyExec, es);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static Object newObject(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class aClass, RichIterable<? extends KeyValue> root_meta_pure_functions_lang_keyExpressions, ElementOverride override, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function getterToOne, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function getterToMany, Object payload, PureFunction2 getterToOneExec, PureFunction2 getterToManyExec, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return Pure.newObject(aClass, root_meta_pure_functions_lang_keyExpressions, override, getterToOne, getterToMany, payload, getterToOneExec, getterToManyExec, es);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static Object newObject(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class aClass, String name, RichIterable<? extends KeyExpression> root_meta_pure_functions_lang_keyExpressions, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return Pure.newObject(bridge, aClass, name, root_meta_pure_functions_lang_keyExpressions, es);\n" +
                                "\n" +
                                "    }\n" +
                                "\n" +
                                "    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<Any> newClass(String fullPathString, MetadataAccessor ma, SourceInformation si)\n" +
                                "    {\n" +
                                "        ListIterable<String> fullPath = PackageableElement.splitUserPath(fullPathString);\n" +
                                "        if (fullPath.isEmpty())\n" +
                                "        {\n" +
                                "            throw new PureExecutionException(null, \"Cannot create a new Class: '\" + fullPathString + \"'\");\n" +
                                "        }\n" +
                                "        String name = fullPath.getLast();\n" +
                                "        org.finos.legend.pure.m3.coreinstance.Package _package = Pure.buildPackageIfNonExistent(new Package_Impl(\"Root\")._name(\"Root\"), ListHelper.subList(fullPath, 0, fullPath.size() - 1), si, s -> new Package_Impl(s));\n" +
                                "        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<Any> _class = new Root_meta_pure_metamodel_type_Class_Impl(name)._name(name)._package(_package);\n" +
                                "        return _class._classifierGenericType(\n" +
                                "                new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"Anonymous_StripedId\")\n" +
                                "                        ._rawType(ma.getClass(\"Root::meta::pure::metamodel::type::Class\"))\n" +
                                "                        ._typeArguments(Lists.immutable.of(new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"Anonymous_StripedId\")._rawType(_class))))\n" +
                                "                ._generalizations(Lists.immutable.of(\n" +
                                "                        new Root_meta_pure_metamodel_relationship_Generalization_Impl(\"Anonymous_StripedId\")\n" +
                                "                                ._general(new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"Anonymous_StripedId\")._rawType(ma.getTopType()))\n" +
                                "                                ._specific(_class)));\n" +
                                "    }\n" +
                                "\n" +
                                "    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association newAssociation(String fullPathString, Property p1, Property p2, MetadataAccessor ma, SourceInformation si)\n" +
                                "    {\n" +
                                "        ListIterable<String> fullPath = PackageableElement.splitUserPath(fullPathString);\n" +
                                "        if (fullPath.isEmpty())\n" +
                                "        {\n" +
                                "            throw new PureExecutionException(null, \"Cannot create a new Association: '\" + fullPathString + \"'\");\n" +
                                "        }\n" +
                                "        String name = fullPath.getLast();\n" +
                                "        org.finos.legend.pure.m3.coreinstance.Package _package = Pure.buildPackageIfNonExistent(new Package_Impl(\"Root\")._name(\"Root\"), ListHelper.subList(fullPath, 0, fullPath.size() - 1), si, s -> new Package_Impl(s));\n" +
                                "        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association _association = new Root_meta_pure_metamodel_relationship_Association_Impl(name)._name(name)._package(_package);\n" +
                                "        return _association._propertiesAdd(p1)._propertiesAdd(p2)._classifierGenericType(\n" +
                                "                new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"Anonymous_StripedId\")\n" +
                                "                        ._rawType(ma.getClass(\"Root::meta::pure::metamodel::relationship::Association\")));\n" +
                                "    }\n" +
                                "\n" +
                                "    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration<Any> newEnumeration(String fullPathString, RichIterable values, MetadataAccessor ma, SourceInformation si)\n" +
                                "    {\n" +
                                "        ListIterable<String> fullPath = PackageableElement.splitUserPath(fullPathString);\n" +
                                "        if (fullPath.isEmpty())\n" +
                                "        {\n" +
                                "            throw new PureExecutionException(null, \"Cannot create a new Enumeration: '\" + fullPathString + \"'\");\n" +
                                "        }\n" +
                                "        String name = fullPath.getLast();\n" +
                                "        String packageName = ListHelper.subList(fullPath, 0, fullPath.size() - 1).makeString(\"::\");\n" +
                                "        org.finos.legend.pure.m3.coreinstance.Package _package = Pure.buildPackageIfNonExistent(new Package_Impl(\"Root\")._name(\"Root\"), ListHelper.subList(fullPath, 0, fullPath.size() - 1), si, s -> new Package_Impl(s));\n" +
                                "        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration<Any> _enumeration = new Root_meta_pure_metamodel_type_Enumeration_Impl<Any>(name)._name(name)._package(_package);\n" +
                                "        return _enumeration._classifierGenericType(\n" +
                                "                new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"Anonymous_StripedId\")\n" +
                                "                        ._rawType(ma.getClass(\"Root::meta::pure::metamodel::type::Enumeration\"))\n" +
                                "                        ._typeArguments(Lists.immutable.of(new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"Anonymous_StripedId\")._rawType(_enumeration))))\n" +
                                "                ._generalizations(Lists.immutable.of(\n" +
                                "                        new Root_meta_pure_metamodel_relationship_Generalization_Impl(\"Anonymous_StripedId\")\n" +
                                "                                ._general(new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"Anonymous_StripedId\")._rawType(ma.getClass(\"Root::meta::pure::metamodel::type::Enum\")))\n" +
                                "                                ._specific(_enumeration)))\n" +
                                "                ._values(values.collect(new Function<String, PureEnum>()\n" +
                                "                {\n" +
                                "                    public PureEnum valueOf(String valueName)\n" +
                                "                    {\n" +
                                "                        return new PureEnum(valueName, fullPathString);\n" +
                                "                    }\n" +
                                "                }));\n" +
                                "    }\n" +
                                "\n" +
                                "    private static class BridgeImpl implements Bridge\n" +
                                "    {\n" +
                                "        @Override\n" +
                                "        public <T> List<T> buildList()\n" +
                                "        {\n" +
                                "            return new Root_meta_pure_functions_collection_List_Impl<>(\"\");\n" +
                                "        }\n" +
                                "\n" +
                                "        @Override\n" +
                                "        public boolean hasToOneUpperBound(Multiplicity multiplicity, ExecutionSupport executionSupport)\n" +
                                "        {\n" +
                                "            return platform_pure_corefunctions_multiplicity.Root_meta_pure_functions_multiplicity_hasToOneUpperBound_Multiplicity_1__Boolean_1_(multiplicity, executionSupport);\n" +
                                "        }\n" +
                                "\n" +
                                "        @Override\n" +
                                "        public boolean isToOne(Multiplicity multiplicity, ExecutionSupport executionSupport)\n" +
                                "        {\n" +
                                "            return platform_pure_corefunctions_multiplicity.Root_meta_pure_functions_multiplicity_isToOne_Multiplicity_1__Boolean_1_(multiplicity, executionSupport);\n" +
                                "        }\n" +
                                "\n" +
                                "        @Override\n" +
                                "        public String elementToPath(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement element, String separator, ExecutionSupport executionSupport)\n" +
                                "        {\n" +
                                "            return platform_pure_corefunctions_meta.Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1__String_1_(element, separator, executionSupport);\n" +
                                "        }\n" +
                                "\n" +
                                "        @Override\n" +
                                "        public LambdaCompiledExtended buildLambda(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<Object> lambdaFunction, SharedPureFunction<Object> pureFunction)\n" +
                                "{\n" +
                                "            return new PureCompiledLambda(lambdaFunction, pureFunction);\n" +
                                "        }\n" +
                                "    }\n" +
                                "}\n")),
                Lists.fixedSize.empty(),
                Lists.fixedSize.empty());
    }

    public static CompiledExtension extension()
    {
        return new CoreExtensionCompiled();
    }
}
