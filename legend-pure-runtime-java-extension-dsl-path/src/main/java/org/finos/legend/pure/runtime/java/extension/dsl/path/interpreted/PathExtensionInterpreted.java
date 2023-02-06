package org.finos.legend.pure.runtime.java.extension.dsl.path.interpreted;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.block.function.checked.CheckedFunction;
import org.eclipse.collections.impl.block.function.checked.CheckedFunction2;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m2.inlinedsl.path.M2PathPaths;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.PropertyCoreInstanceWrapper;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.extension.BaseInterpretedExtension;
import org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtension;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class PathExtensionInterpreted extends BaseInterpretedExtension
{
    public PathExtensionInterpreted()
    {
        super(Lists.mutable.empty());
    }

    public static InterpretedExtension extension()
    {
        return new PathExtensionInterpreted();
    }


    @Override
    public CoreInstance getExtraFunctionExecution(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<CoreInstance> function, ListIterable<? extends CoreInstance> params, final Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, final Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, final VariableContext variableContext, final CoreInstance functionExpressionToUseInStack, final Profiler profiler, final InstantiationContext instantiationContext, final ExecutionSupport executionSupport, ProcessorSupport processorSupport, FunctionExecutionInterpreted interpreted)
    {
        if (Instance.instanceOf(function, M2PathPaths.Path, processorSupport))
        {
            final boolean executable = ValueSpecification.isExecutable(params.get(0), processorSupport);

            CoreInstance value = params.get(0).getValueForMetaPropertyToOne(M3Properties.values);

            MutableList<CoreInstance> res = function.getValueForMetaPropertyToMany(M3Properties.path).injectInto(FastList.newListWith(value), new CheckedFunction2<MutableList<CoreInstance>, CoreInstance, MutableList<CoreInstance>>()
            {
                @Override
                public MutableList<CoreInstance> safeValue(MutableList<CoreInstance> instances, final CoreInstance pathElement) throws Exception
                {
                    return instances.flatCollect(new CheckedFunction<CoreInstance, Iterable<CoreInstance>>()
                    {
                        @Override
                        public Iterable<CoreInstance> safeValueOf(CoreInstance instance) throws Exception
                        {
                            CoreInstance property = Instance.getValueForMetaPropertyToOneResolved(pathElement, M3Properties.property, processorSupport);
                            MutableList<CoreInstance> parameters = FastList.newListWith(ValueSpecificationBootstrap.wrapValueSpecification(instance, executable, processorSupport));
                            parameters.addAllIterable(Instance.getValueForMetaPropertyToManyResolved(pathElement, M3Properties.parameters, processorSupport).collect(new org.eclipse.collections.api.block.function.Function<CoreInstance, CoreInstance>()
                            {
                                @Override
                                public CoreInstance valueOf(CoreInstance coreInstance)
                                {
                                    return ValueSpecificationBootstrap.wrapValueSpecification(Instance.getValueForMetaPropertyToManyResolved(coreInstance, M3Properties.values, processorSupport), executable, processorSupport);
                                }
                            }));
                            return (Iterable<CoreInstance>) interpreted.executeFunction(false, PropertyCoreInstanceWrapper.toProperty(property), parameters, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport).getValueForMetaPropertyToMany(M3Properties.values);
                        }
                    });
                }
            });
            return ValueSpecificationBootstrap.wrapValueSpecification(res, executable, processorSupport);
        }
        return null;
    }
}
