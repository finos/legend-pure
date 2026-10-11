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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.lang.eval;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.extension.EvalRustExecutor;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * {@code evalRust}: a call compiles to {@link #evaluate(ExecutionSupport, Function)}, which hands the function value
 * to the {@link EvalRustExecutor} found on the class path at run time. Without one the call fails at run time; the
 * compiled platform itself needs no Rust evaluator to build.
 */
public class EvalRust extends AbstractNative
{
    public static final String SIGNATURE = "evalRust_Function_1__V_m_";

    public EvalRust()
    {
        super(SIGNATURE);
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        String type = TypeProcessor.typeToJavaObjectWithMul(Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.genericType, processorSupport), Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.multiplicity, processorSupport), processorSupport);
        CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.multiplicity, processorSupport);
        String eval = EvalRust.class.getName() + ".evaluate(es, (" + FullJavaPaths.Function + "<?>)" + transformedParams.get(0) + ")";
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
                "           Object value = " + EvalRust.class.getName() + ".evaluate(es, (" + FullJavaPaths.Function + "<?>) vars.get(0));\n" +
                "           return value instanceof Iterable ? CompiledSupport.toPureCollection(value) : value;\n" +
                "        }\n" +
                "}";
    }

    /**
     * The run-time entry the generated code calls.
     *
     * @param executionSupport the execution support of the calling function
     * @param function         the function value to evaluate
     * @return the result as compiled values
     */
    public static Object evaluate(ExecutionSupport executionSupport, Function<?> function)
    {
        EvalRustExecutor executor = Holder.EXECUTOR;
        if (executor == null)
        {
            throw new UnsupportedOperationException(SIGNATURE + " needs the Rust evaluator (legend-pure-runtime-rust-evaluator) on the class path");
        }
        return executor.evaluate(executionSupport, function);
    }

    private static final class Holder
    {
        private static final EvalRustExecutor EXECUTOR = load();

        private static EvalRustExecutor load()
        {
            ClassLoader context = Thread.currentThread().getContextClassLoader();
            EvalRustExecutor found = (context == null) ? null : first(context);
            return (found == null) ? first(EvalRust.class.getClassLoader()) : found;
        }

        private static EvalRustExecutor first(ClassLoader loader)
        {
            Iterator<EvalRustExecutor> found = ServiceLoader.load(EvalRustExecutor.class, loader).iterator();
            return found.hasNext() ? found.next() : null;
        }
    }
}
