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

package org.finos.legend.pure.runtime.java.compiled.generation.processors;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.basics.io.Print;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.basics.collection.*;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.basics.lang.*;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.basics.math.Abs;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.basics.meta.*;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.basics.string.*;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.basics.tests.Assert;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar._boolean.conjunctions.And;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar._boolean.conjunctions.Not;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar._boolean.conjunctions.Or;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar._boolean.equality.Eq;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar._boolean.equality.Equal;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar._boolean.equality.Is;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar._boolean.inequalities.LessThan;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar._boolean.inequalities.LessThanEqual;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.collection.*;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.lang.*;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.math.*;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.mulitplicity.ToOne;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.mulitplicity.ToOneMany;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.mulitplicity.ToOneManyWithMessage;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.mulitplicity.ToOneWithMessage;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.string.JoinStrings;

public class NativeFunctionProcessor
{
    private static final ImmutableList<String> IMPORTS = Lists.immutable.with(
            "import org.eclipse.collections.api.list.ListIterable;\n",
            "import org.eclipse.collections.api.list.MutableList;\n",
            "import org.eclipse.collections.api.RichIterable;\n",
            "import org.eclipse.collections.api.map.MutableMap;\n",
            "import org.eclipse.collections.impl.factory.Lists;\n",
            "import org.eclipse.collections.impl.map.mutable.UnifiedMap;\n",
            "import org.finos.legend.pure.runtime.java.compiled.*;\n",
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.*;\n",
            "import org.eclipse.collections.api.block.function.Function2;\n",
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;\n",
            "import org.eclipse.collections.api.block.function.Function0;\n",
            "import org.eclipse.collections.api.block.function.Function;\n",
            "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n",
            "import org.eclipse.collections.impl.factory.Maps;\n",
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.*;\n",
            "import org.finos.legend.pure.runtime.java.compiled.*;\n",
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.*;\n",
            "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.*;\n",
            "import org.finos.legend.pure.runtime.java.compiled.execution.*;\n",
            "import org.finos.legend.pure.runtime.java.compiled.execution.sourceInformation.*;\n",
            //tests only
            "import org.junit.Test;\n");

    private final MutableMap<String, Native> natives;

    private NativeFunctionProcessor(Iterable<? extends Native> extraNatives)
    {
        this.natives = buildNativeMap(extraNatives);
    }

    public void buildNativeLambdaFunction(CoreInstance nativeFunction, ProcessorContext processorContext)
    {
        Native nat = this.natives.get(nativeFunction.getName());
        if (nat != null)
        {
            processorContext.registerNativeLambdaFunction(nativeFunction, nat);
        }
    }

    public String processNativeFunction(CoreInstance topLevelElement, CoreInstance functionExpression, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        CoreInstance nativeFunction = Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.func, processorSupport);
        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);

        Native nat = this.natives.get(nativeFunction.getName());
        if (nat != null)
        {
            ListIterable<String> transformedParams = nat.transformParameterValues(parametersValues, topLevelElement, processorSupport, processorContext);
            return nat.build(topLevelElement, functionExpression, transformedParams, processorContext);
        }

        throw new RuntimeException(nativeFunction.getName() + " Not supported yet");
    }

    public ListIterable<String> getImports()
    {
        return IMPORTS;
    }

    public static String buildM4SourceInformation(SourceInformation sourceInformation)
    {
        return sourceInformation == null ? "null" : "new org.finos.legend.pure.m4.coreinstance.SourceInformation(\"" + sourceInformation.getSourceId() + "\", "
                + sourceInformation.getStartLine()
                + ", " + sourceInformation.getStartColumn()
                + ", " + sourceInformation.getEndLine()
                + ", " + sourceInformation.getEndColumn() + ")";
    }

    public static String buildM4LineColumnSourceInformation(SourceInformation sourceInformation)
    {
        return sourceInformation == null ? "null" : "new org.finos.legend.pure.m4.coreinstance.SourceInformation(\"" + sourceInformation.getSourceId() + "\""
                + ", -1, -1"
                + ", " + sourceInformation.getLine()
                + ", " + sourceInformation.getColumn()
                + ", -1, -1)";
    }

    public static NativeFunctionProcessor newProcessor()
    {
        return newWithExtraNatives(Lists.immutable.empty());
    }

    public static NativeFunctionProcessor newWithExtraNatives(Iterable<? extends Native> extraNatives)
    {
        return new NativeFunctionProcessor(extraNatives);
    }

    public static NativeFunctionProcessor newWithCompiledExtensions(Iterable<? extends CompiledExtension> extensions)
    {
        return newWithExtraNatives(LazyIterate.flatCollect(extensions, CompiledExtension::getExtraNatives));
    }

    private static MutableMap<String, Native> buildNativeMap(Iterable<? extends Native> extraNatives)
    {
        MutableMap<String, Native> map = Maps.mutable.empty();
        registerGrammarCoreNatives(map);
        registerBasicsCoreNatives(map);
        if (extraNatives != null)
        {
            extraNatives.forEach(n -> registerNative(map, n));
        }
        return map;
    }

    private static void registerNative(MutableMap<String, Native> map, Native nativeFunc)
    {
        for (String signature : nativeFunc.signatures())
        {
            Native old = map.put(signature, nativeFunc);
            if (old != null)
            {
                throw new IllegalArgumentException("Error registering function " + nativeFunc + ": " + signature + " is already registered, using " + old);
            }
        }
    }

    private static void registerGrammarCoreNatives(MutableMap<String, Native> map)
    {
        //Boolean
        registerNative(map, new And());
        registerNative(map, new Not());
        registerNative(map, new Or());
        registerNative(map, new Eq());
        registerNative(map, new Equal());
        registerNative(map, new Is());
        registerNative(map, new LessThan());
        registerNative(map, new LessThanEqual());

        //Collection
        registerNative(map, new Filter());
        registerNative(map, new First());
        registerNative(map, new IsEmpty());
        registerNative(map, new Map());
        registerNative(map, new Range());
        registerNative(map, new Size());

        //Lang
        registerNative(map, new Compare());
        registerNative(map, new Copy());
        registerNative(map, new ExtractEnumValue());
        registerNative(map, new GetAll());
        registerNative(map, new Let());
        registerNative(map, new New());
        registerNative(map, new NewWithKeyExpr());

        //Math
        registerNative(map, new Divide());
        registerNative(map, new DivideDecimal());
        registerNative(map, new Minus());
        registerNative(map, new Plus());
        registerNative(map, new Times());

        //Multiplicity
        registerNative(map, new ToOne());
        registerNative(map, new ToOneWithMessage());
        registerNative(map, new ToOneMany());
        registerNative(map, new ToOneManyWithMessage());

        //String
        registerNative(map, new JoinStrings());
    }

    private static void registerBasicsCoreNatives(MutableMap<String, Native> map)
    {
        //Collection
        registerNative(map, new Concatenate());
        registerNative(map, new Add());
        registerNative(map, new At());
        registerNative(map, new Fold());
        registerNative(map, new Init());
        registerNative(map, new RemoveDuplicates());
        registerNative(map, new Sort());
        registerNative(map, new Tail());

        //IO
        registerNative(map, new Print());

        //Lang
        registerNative(map, new Cast());
        registerNative(map, new Eval());
        registerNative(map, new If());
        registerNative(map, new Match());
        registerNative(map, new DynamicNew());
        registerNative(map, new Evaluate());

        //Math
        registerNative(map, new Abs());

        //Meta
        registerNative(map, new EvaluateAndDeactivate());
        registerNative(map, new GenericType());
        registerNative(map, new GenericTypeClass());
        registerNative(map, new GetUnitValue());
        registerNative(map, new Id());
        registerNative(map, new InstanceOf());
        registerNative(map, new NewUnit());

        //String
        registerNative(map, new Format());
        registerNative(map, new Length());
        registerNative(map, new Replace());
        registerNative(map, new Split());
        registerNative(map, new StartsWith());
        registerNative(map, new Substring());
        registerNative(map, new ToString());

        //Tests
        registerNative(map, new Assert());
    }
}