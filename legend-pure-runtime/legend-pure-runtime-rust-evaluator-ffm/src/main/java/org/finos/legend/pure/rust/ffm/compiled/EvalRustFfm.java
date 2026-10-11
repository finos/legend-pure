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

package org.finos.legend.pure.rust.ffm.compiled;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

/**
 * The compiled engine's code generation for {@code meta::pure::rust::ffm::evalRustFfm}: a call compiles to
 * {@link CompiledEvalRustFfm#evaluate}, which runs the function value in the Rust evaluator over the Foreign
 * Function and Memory API. Registered through {@link RustFfmCompiledExtension}.
 */
public class EvalRustFfm extends AbstractNative
{
    public static final String SIGNATURE = "evalRustFfm_Function_1__V_m_";

    private static final String EVALUATE = CompiledEvalRustFfm.class.getName() + ".evaluate";

    public EvalRustFfm()
    {
        super(SIGNATURE);
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        String type = TypeProcessor.typeToJavaObjectWithMul(Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.genericType, processorSupport), Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.multiplicity, processorSupport), processorSupport);
        CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.multiplicity, processorSupport);
        String eval = EVALUATE + "(es, (" + FullJavaPaths.Function + "<?>)" + transformedParams.get(0) + ")";
        return "((" + type + ")(Object)" + (Multiplicity.isToOne(multiplicity, false) ? "CompiledSupport.makeOne(" + eval + ")" : "CompiledSupport.toPureCollection(" + eval + ")") + ")";
    }

    @Override
    public String buildBody()
    {
        return "new SharedPureFunction<Object>()\n" +
                "{\n" +
                "        @Override\n" +
                "        public Object execute(ListIterable<?> vars, final ExecutionSupport es)\n" +
                "        {\n" +
                "           Object value = " + EVALUATE + "(es, (" + FullJavaPaths.Function + "<?>) vars.get(0));\n" +
                "           return value instanceof Iterable ? CompiledSupport.toPureCollection(value) : value;\n" +
                "        }\n" +
                "}";
    }
}
