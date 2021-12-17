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

package org.finos.legend.pure.m3.navigation.function;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.SafeAppendable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A function descriptor is a human readable expression which unambiguously
 * describes a function.  A function descriptor is a string of the form
 * PKG::NAME(P1[M1], P2[M2], ...):T[M], where PKG is the full package of the
 * function, NAME is the function name, Pn are the parameter types, Mn are
 * the parameter multiplicities, T is the return type, and M is the return
 * multiplicity.  For the parameter and return types, only the raw type or
 * type parameter is used.  Type arguments are not included.
 */
public class FunctionDescriptor
{
    private static final Pattern DESCRIPTOR_MAIN = Pattern.compile("^\\s*+([^(\\s]++)\\s*+\\(\\s*+([^)]*+)\\s*+\\)\\s*+:\\s*+(.*\\S)\\s*+$");
    private static final Pattern PARAMETER_DELIMITER = Pattern.compile("\\s*+,\\s*+");
    private static final Pattern TYPE_WITH_MULTIPLICITY_NUM = Pattern.compile("^\\s*+([~\\w]++)\\s*+\\[\\s*+(\\d++)\\s*+\\]\\s*+$");
    private static final Pattern TYPE_WITH_MULTIPLICITY_MANY = Pattern.compile("^\\s*+([~\\w]++)\\s*+\\[\\s*+(\\*)\\s*+\\]\\s*+$");
    private static final Pattern TYPE_WITH_MULTIPLICITY_NUM_TO_NUM = Pattern.compile("^\\s*+([~\\w]++)\\s*+\\[\\s*+(\\d++)\\s*+\\.\\.\\s*+(\\d++)\\]\\s*+$");
    private static final Pattern TYPE_WITH_MULTIPLICITY_NUM_TO_MANY = Pattern.compile("^\\s*+([~\\w]++)\\s*+\\[\\s*+(\\d++)\\s*+\\.\\.\\s*+(\\*)\\]\\s*+$");
    private static final Pattern TYPE_WITH_MULTIPLICITY_PARAMETER = Pattern.compile("^\\s*+([~\\w]++)\\s*+\\[\\s*+([a-zA-Z]\\w*+)\\s*+\\]\\s*+$");
    private static final Pattern FULL_MATCH = Pattern.compile("^\\s*+([\\w\\d_$]++::)*+[\\w\\d_$]++\\s*+\\(\\s*+(\\w++\\s*+\\[\\s*+(\\*|([a-zA-Z]\\w*+)|(\\d++(\\s*+\\.\\.\\s*+(\\d++|\\*))?+))\\s*+\\]\\s*+(?!,\\s*+\\)),?+\\s*+)*+\\s*+\\)\\s*+:\\s*+\\w++\\s*+\\[\\s*+(\\*|([a-zA-Z]\\w*+)|(\\d++(\\s*+\\.\\.\\s*+(\\d++|\\*))?+))\\s*+\\]\\s*+$");

    /**
     * Return whether string is possibly a function descriptor.  This
     * is a strictly syntactic test which can be used to weed out
     * strings which cannot possibly be function descriptors.  If this
     * function returns false for a string, then that string is not a
     * function descriptor.  If the function returns true, the string
     * may or may not be a function descriptor, but a more expensive
     * test will be required to determine.
     *
     * @param string possible function descriptor
     * @return whether string is possibly a function descriptor
     */
    public static boolean isPossiblyFunctionDescriptor(String string)
    {
        return DESCRIPTOR_MAIN.matcher(string).matches();
    }

    /**
     * Function to validate whether some input string matches function descriptor
     * format. This validation is stricter and more accurate than {@see isPossiblyFunctionDescriptor}
     * however might not be fully exhaustive for various edge cases.
     *
     * @param functionDescriptorCandidate string which we want to validate
     * @return whether input is valid function descriptor
     */
    public static boolean isValidFunctionDescriptor(String functionDescriptorCandidate)
    {
        return FULL_MATCH.matcher(functionDescriptorCandidate).matches();
    }

    /**
     * Get the function descriptor for a function.
     *
     * @param function         function
     * @param processorSupport processor support
     * @return function descriptor
     */
    public static String getFunctionDescriptor(CoreInstance function, ProcessorSupport processorSupport)
    {
        return writeFunctionDescriptor(new StringBuilder(), function, processorSupport).toString();
    }

    /**
     * Write the descriptor for a function to the given appendable.
     *
     * @param appendable       appendable to write to
     * @param function         function
     * @param processorSupport processor support
     */
    public static <T extends Appendable> T writeFunctionDescriptor(T appendable, CoreInstance function, ProcessorSupport processorSupport)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);

        // Write package
        CoreInstance pkg = function.getValueForMetaPropertyToOne(M3Properties._package);
        if ((pkg != null) && !M3Paths.Root.equals(pkg.getName()))
        {
            PackageableElement.writeUserPathForPackageableElement(appendable, pkg);
            safeAppendable.append(PackageableElement.DEFAULT_PATH_SEPARATOR);
        }

        // Write function name
        CoreInstance functionName = function.getValueForMetaPropertyToOne(M3Properties.functionName);
        if (functionName == null)
        {
            throw new IllegalArgumentException("Anonymous functions do not have descriptors");
        }
        safeAppendable.append(PrimitiveUtilities.getStringValue(functionName));

        // Write parameter types and multiplicities
        CoreInstance functionType = processorSupport.function_getFunctionType(function);
        safeAppendable.append('(');
        ListIterable<? extends CoreInstance> parameters = functionType.getValueForMetaPropertyToMany(M3Properties.parameters);
        if (parameters.notEmpty())
        {
            boolean first = true;
            for (CoreInstance parameter : parameters)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    safeAppendable.append(", ");
                }
                CoreInstance parameterType = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.genericType, processorSupport);
                CoreInstance parameterMultiplicity = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.multiplicity, processorSupport);
                writeDescriptorTypeAndMultiplicity(safeAppendable, parameterType, parameterMultiplicity, processorSupport);
            }
        }
        safeAppendable.append(')');

        // Write return type and multiplicity
        safeAppendable.append(':');
        CoreInstance returnType = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport);
        CoreInstance returnMultiplicity = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnMultiplicity, processorSupport);
        writeDescriptorTypeAndMultiplicity(safeAppendable, returnType, returnMultiplicity, processorSupport);
        return appendable;
    }

    private static void writeDescriptorTypeAndMultiplicity(SafeAppendable appendable, CoreInstance genericType, CoreInstance multiplicity, ProcessorSupport processorSupport)
    {
        if (GenericType.isGenericTypeConcrete(genericType))
        {
            CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
            if (FunctionType.isFunctionType(rawType, processorSupport))
            {
                FunctionType.print(appendable, rawType, processorSupport);
            }
            else
            {
                appendable.append(rawType.getName());
            }
        }
        else
        {
            appendable.append(GenericType.getTypeParameterName(genericType));
        }

        Multiplicity.print(appendable, multiplicity, true);
    }

    /**
     * Get a function by its descriptor.  Returns null if there is
     * no such function.
     *
     * @param functionDescriptor function descriptor
     * @return function
     * @throws InvalidFunctionDescriptorException if functionDescriptor is not a valid function descriptor
     */
    public static CoreInstance getFunctionByDescriptor(String functionDescriptor, ProcessorSupport processorSupport) throws InvalidFunctionDescriptorException
    {
        return processorSupport.package_getByUserPath(functionDescriptorToId(functionDescriptor));
    }

    /**
     * Convert a function descriptor to a function id, i.e., the
     * identifier that Pure generates for a function which uniquely
     * identifies it.
     *
     * @param functionDescriptor function descriptor
     * @return function id
     * @throws InvalidFunctionDescriptorException if functionDescriptor is not a valid function descriptor
     */
    public static String functionDescriptorToId(String functionDescriptor) throws InvalidFunctionDescriptorException
    {
        Matcher matcher = DESCRIPTOR_MAIN.matcher(functionDescriptor);
        if (!matcher.matches())
        {
            throw new InvalidFunctionDescriptorException(functionDescriptor);
        }

        StringBuilder builder = new StringBuilder(matcher.group(1));
        try
        {
            String paramsString = matcher.group(2);
            if (paramsString.isEmpty())
            {
                builder.append('_');
            }
            else
            {
                ArrayIterate.forEach(PARAMETER_DELIMITER.split(paramsString), p -> typeWithMultiplicityDescriptorToId(builder, p));
            }

            typeWithMultiplicityDescriptorToId(builder, matcher.group(3));
        }
        catch (IllegalArgumentException e)
        {
            throw new InvalidFunctionDescriptorException(functionDescriptor, e);
        }
        return builder.toString();
    }

    private static void typeWithMultiplicityDescriptorToId(StringBuilder builder, String typeWithMultiplicityDescriptor)
    {
        Matcher matcher = TYPE_WITH_MULTIPLICITY_NUM.matcher(typeWithMultiplicityDescriptor);
        if (matcher.matches())
        {
            builder.append('_').append(matcher.group(1)).append('_').append(Integer.parseInt(matcher.group(2))).append('_');
            return;
        }

        matcher = TYPE_WITH_MULTIPLICITY_MANY.matcher(typeWithMultiplicityDescriptor);
        if (matcher.matches())
        {
            builder.append('_').append(matcher.group(1)).append("_MANY_");
            return;
        }

        matcher = TYPE_WITH_MULTIPLICITY_NUM_TO_NUM.matcher(typeWithMultiplicityDescriptor);
        if (matcher.matches())
        {
            builder.append('_').append(matcher.group(1)).append('_');
            int num1 = Integer.parseInt(matcher.group(2));
            int num2 = Integer.parseInt(matcher.group(3));
            if (num1 == num2)
            {
                builder.append(num1);
            }
            else
            {
                builder.append('$').append(num1).append('_').append(num2).append('$');
            }
            builder.append('_');
            return;
        }

        matcher = TYPE_WITH_MULTIPLICITY_NUM_TO_MANY.matcher(typeWithMultiplicityDescriptor);
        if (matcher.matches())
        {
            builder.append('_').append(matcher.group(1)).append('_');
            int num = Integer.parseInt(matcher.group(2));
            if (num == 0)
            {
                builder.append("MANY_");
            }
            else
            {
                builder.append('$').append(num).append("_MANY$_");
            }
            return;
        }

        matcher = TYPE_WITH_MULTIPLICITY_PARAMETER.matcher(typeWithMultiplicityDescriptor);
        if (matcher.matches())
        {
            builder.append('_').append(matcher.group(1)).append('_').append(matcher.group(2)).append('_');
            return;
        }

        throw new IllegalArgumentException("Invalid type with multiplicity descriptor: '" + typeWithMultiplicityDescriptor + "'");
    }
}
