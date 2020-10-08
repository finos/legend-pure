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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.NativeFunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractNativeFunctionGeneric extends AbstractNative
{
    private final boolean hasSrcInformation;
    private final boolean hasExecutionSupport;
    private final boolean castReturnValue;
    private final Object methodName;
    private final Object[] parameterTypes;

    protected AbstractNativeFunctionGeneric(String methodName, Class<?>[] parameterTypes, String... signatures) {
        this(methodName, parameterTypes, false, false, false, signatures);
    }

    protected AbstractNativeFunctionGeneric(String methodName, Class<?>[] parameterTypes, boolean hasSrcInformation, boolean hasExecutionSupport, boolean castReturnValue,  String... signatures)
    {
        this(methodName, (Object[]) parameterTypes, hasSrcInformation, hasExecutionSupport, castReturnValue, signatures);
    }

    protected AbstractNativeFunctionGeneric(String methodName, Object[] parameterTypes, String... signatures) {
        this(methodName, parameterTypes, false, false, false, signatures);
    }

    protected AbstractNativeFunctionGeneric(String methodName, Object[] parameterTypes, boolean hasSrcInformation, boolean hasExecutionSupport, boolean castReturnValue,  String... signatures) {
        super(signatures);
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.hasSrcInformation = hasSrcInformation;
        this.hasExecutionSupport = hasExecutionSupport;
        this.castReturnValue = castReturnValue;
    }

    protected AbstractNativeFunctionGeneric(Method method, String... signatures) {
        this(method, false, false, false, signatures);
    }

    protected AbstractNativeFunctionGeneric(Method method, boolean hasSrcInformation, boolean hasExecutionSupport, boolean castReturnValue, String... signatures)
    {
        this(method.getDeclaringClass().getSimpleName() + "." + method.getName(), method.getParameterTypes(), hasSrcInformation, hasExecutionSupport, castReturnValue, signatures);
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        SourceInformation sourceInformation = functionExpression.getSourceInformation();
        String cast = "";

        if (this.castReturnValue) {
            ProcessorSupport processorSupport = processorContext.getSupport();
            CoreInstance nativeFunction = Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.func, processorSupport);
            CoreInstance functionType = processorSupport.function_getFunctionType(nativeFunction);
            String returnType = TypeProcessor.typeToJavaPrimitiveSingle(Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport), processorSupport);

            cast =  "(" + returnType + ")";
        }

        String sourceInformationStr = this.hasSrcInformation ? buildSourceInformationParameterValues(sourceInformation, transformedParams.size()) : "";
        String es = this.hasExecutionSupport ? buildEs(transformedParams.size()) : "";

        return cast + this.methodName + "(" + StringUtils.join(transformedParams, ", ") + sourceInformationStr + es + ")";
    }

    public String buildEs(int noParams) {
        return noParams > 0 || this.hasSrcInformation ? ", es" : "es";
    }

    protected String buildSourceInformationParameterValues(SourceInformation sourceInformation, int noParams)
    {
        String si = this.buildM4SourceInformation(sourceInformation);
        return noParams > 0 ? ", " + si : si;
    }

    protected String buildM4SourceInformation(SourceInformation sourceInformation)
    {
        return NativeFunctionProcessor.buildM4LineColumnSourceInformation(sourceInformation);
    }

    @Override
    public String buildBody()
    {
        List<Object> parameterTypes = Arrays.asList(this.parameterTypes);
        if (this.hasSrcInformation) {
            parameterTypes = parameterTypes.subList(0, parameterTypes.size() - 1);
        }

        if (this.hasExecutionSupport) {
            parameterTypes = parameterTypes.subList(0, parameterTypes.size() - 1);
        }

        List<String> parameterAccessString = FastList.newList(parameterTypes.size());

        int index = 0;
        for (Object type : parameterTypes) {
            String typeName = getClassName(type);
            String parameterType = type instanceof Class && ((Class)type).isArray() ? "ListIterable<" + typeName + ">" : typeName;

            parameterAccessString.add("(" + parameterType + ")vars.get(" + index + ")");
            index++;
        }

        if (this.hasSrcInformation) {
            parameterAccessString.add(buildSourceInformationParameterValues(null, 0));
        }

        if (this.hasExecutionSupport) {
            parameterAccessString.add("es");
        }

        return "new SharedPureFunction<Object>()\n" +
                "{\n" +
                "   @Override\n" +
                "   public Object execute(ListIterable vars, final ExecutionSupport es)\n" +
                "   {\n" +
                "       return " + this.methodName + "(" + StringUtils.join(parameterAccessString, ", ") +  ");" +
                "   }\n" +
                "\n}";
    }


    public static Method getMethod(Class clazz, String method, Class<?> ...parameterTypes) {
        try {
            return clazz.getMethod(method, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Method getMethod(Class clazz, final String methodName) {
        Method[] methods = clazz.getMethods();
        FastList<Method> candidates = FastList.newListWith(methods).select(new Predicate<Method>()
        {
            @Override
            public boolean accept(Method method)
            {
                return method.getName().equals(methodName);
            }
        });

        if (candidates.size() > 1) {
            throw new IllegalArgumentException("multiple functions found for  " + clazz.getSimpleName() + "." + methodName);
        }

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("cannot find function for " + clazz.getSimpleName() + "." + methodName);
        }

        return candidates.get(0);
    }

    private Class<?> box(Class<?> clazz) {
        return ClassUtils.primitiveToWrapper(clazz);
    }

    private String getClassName(Object obj) {
        if (obj instanceof Class) {
            return getClassName((Class) obj);
        }

        if (obj instanceof String) {
            return (String) obj;
        }

        throw new IllegalArgumentException("type must be a Class or String");
    }

    private String getClassName(Class<?> clazz) {
        Class<?> boxedType = box(clazz);
        String name = boxedType.getCanonicalName();
        return name.startsWith("java.lang.") ? boxedType.getSimpleName() : name;
    }
}
