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

package org.finos.legend.pure.m3.serialization.runtime.pattern;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.apache.commons.lang3.StringUtils;

import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PurePattern
{
    public static final org.eclipse.collections.api.block.function.Function<PurePattern, String> GET_SRC_PATTERN = new org.eclipse.collections.api.block.function.Function<PurePattern, String>()
    {
        @Override
        public String valueOf(PurePattern pattern)
        {
            return pattern.getSrcPattern();
        }
    };

    public static final org.eclipse.collections.api.block.function.Function<PurePattern, String> GET_REAL_PATTERN = new org.eclipse.collections.api.block.function.Function<PurePattern, String>()
    {
        @Override
        public String valueOf(PurePattern pattern)
        {
            return pattern.pattern.pattern();
        }
    };

    public static final org.eclipse.collections.api.block.function.Function<PurePattern, CoreInstance> GET_FUNCTION = new org.eclipse.collections.api.block.function.Function<PurePattern, CoreInstance>()
    {
        @Override
        public CoreInstance valueOf(PurePattern pattern)
        {
            return pattern.getFunction();
        }
    };

    private final Pattern pattern;
    private final CoreInstance function;
    private final String key;
    private final String srcPattern;
    private final List<String> urlArguments;

    PurePattern(String key, String srcPattern, Pattern pattern, CoreInstance function, List<String> urlArguments)
    {
        this.pattern = pattern;
        this.function = function;
        this.key = key;
        this.srcPattern = srcPattern;
        this.urlArguments = urlArguments;
    }

    public String getKey()
    {
        return this.key;
    }

    public String getSrcPattern()
    {
        return this.srcPattern;
    }

    public Pair<CoreInstance, Map<String, String[]>> execute(String url, ProcessorSupport processorSupport, Map<String, String[]> params)
    {
        Matcher matcher = this.pattern.matcher(url);

        if (matcher.matches())
        {
            Map<String, String[]> requestParams = UnifiedMap.newMap();

            requestParams.put("func", new String[]{FunctionDescriptor.getFunctionDescriptor(this.function, processorSupport)});

            RichIterable<? extends VariableExpression> parameters = ((FunctionType)processorSupport.function_getFunctionType(this.function))._parameters();
            String[] toReturnParams = new String[parameters.size()];

            CoreInstance stringType = processorSupport.package_getByUserPath(M3Paths.String);
            ImmutableSet<CoreInstance> dateTypes = Sets.immutable.with(
                    processorSupport.package_getByUserPath(M3Paths.Date),
                    processorSupport.package_getByUserPath(M3Paths.DateTime),
                    processorSupport.package_getByUserPath(M3Paths.StrictDate));

            int i = 0;
            for (VariableExpression c : parameters)
            {
                String name = c._name();
                Type type = (Type)ImportStub.withImportStubByPass(c._genericType()._rawTypeCoreInstance(), processorSupport);

                String value;
                if (urlArguments.contains(name)) {
                    String arg = URLDecoder.decode(matcher.group(name));
                    value = convertValue(arg, type, stringType, dateTypes, processorSupport);
                } else {
                    value = convertValues(params.get(name), type, stringType, dateTypes, processorSupport);
                }

                toReturnParams[i] = value;
                i++;
            }
            requestParams.put("param", toReturnParams);

            return Tuples.pair(this.function, requestParams);
        }

        return null;
    }

    private String convertValues(String[] values, final CoreInstance type, final CoreInstance stringType, final ImmutableSet<CoreInstance> dateTypes, final ProcessorSupport processorSupport) {
        values = values == null ? new String[]{} : values;

        switch(values.length) {
            case 0: return "[]";
            case 1: return convertValue(values[0], type, stringType, dateTypes, processorSupport);
            default:
                MutableList<String> args = FastList.newListWith(values).collect(new Function<String, String>() {

                    @Override
                    public String valueOf(String value)
                    {
                        return convertValue(value, type, stringType, dateTypes, processorSupport);
                    }
                });
                return "[" + StringUtils.join(args, ",") + "]";
        }
    }

    private static String convertValue(String value, CoreInstance type, CoreInstance stringType, ImmutableSet<CoreInstance> dateTypes, ProcessorSupport processorSupport) {
        if (type == stringType) {
            return convertString(value);
        } else if (dateTypes.contains(type)) {
            return convertDate(value);
        } else if (Instance.instanceOf(type, M3Paths.Enumeration, processorSupport)) {
            return convertEnum(value, type);
        }

        return value;
    }

    private static String convertString(String value) {
        String part = value.startsWith("'") && value.endsWith("'")
                ? StringUtils.substring(value, 1, value.length() - 1)
                : value;
        return "'" + part.replace("'", "\'") + "'";
    }

    private static String convertDate(String value) {
        return value.startsWith("%") ? value : "%" + value;
    }

    private static String convertEnum(String value, CoreInstance type){
        String prefix = PackageableElement.getUserPathForPackageableElement(type) + '.';
        return value.startsWith(prefix) ? value : prefix + value;
    }

    public CoreInstance getFunction()
    {
        return this.function;
    }
}
