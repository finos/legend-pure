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

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Lexer;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.FunctionDescriptorContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.FunctionDescriptorTypeMultiplicityContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.MultiplicityArgumentContext;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.SafeAppendable;

import java.util.List;

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
    /**
     * Deprecated. Use {@link #isValidFunctionDescriptor} instead.
     */
    @Deprecated
    public static boolean isPossiblyFunctionDescriptor(String string)
    {
        return isValidFunctionDescriptor(string);
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
        try
        {
            parse(functionDescriptorCandidate);
            return true;
        }
        catch (InvalidFunctionDescriptorException e)
        {
            return false;
        }
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
            PackageableElement.writeUserPathForPackageableElement(safeAppendable, pkg, PackageableElement.DEFAULT_PATH_SEPARATOR).append(PackageableElement.DEFAULT_PATH_SEPARATOR);
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
        safeAppendable.append("):");

        // Write return type and multiplicity
        CoreInstance returnType = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport);
        CoreInstance returnMultiplicity = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnMultiplicity, processorSupport);
        writeDescriptorTypeAndMultiplicity(safeAppendable, returnType, returnMultiplicity, processorSupport);
        return appendable;
    }

    static void writeDescriptorTypeAndMultiplicity(SafeAppendable appendable, CoreInstance genericType, CoreInstance multiplicity, ProcessorSupport processorSupport)
    {
        GenericType.print(appendable, genericType, false, processorSupport);
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
        FunctionDescriptorContext context = parse(functionDescriptor);

        StringBuilder builder = new StringBuilder(context.qualifiedName().getText());
        List<FunctionDescriptorTypeMultiplicityContext> typeMults = context.functionDescriptorTypeMultiplicity();
        if (typeMults.size() == 1)
        {
            // no parameters, only return type
            descriptorTypeAndMultToId(builder.append('_'), typeMults.get(0));
        }
        else
        {
            typeMults.forEach(p -> descriptorTypeAndMultToId(builder, p));
        }
        return builder.toString();
    }

    private static void descriptorTypeAndMultToId(StringBuilder builder, FunctionDescriptorTypeMultiplicityContext typeMultContext)
    {
        builder.append('_').append(typeMultContext.functionDescriptorType().identifier().getText()).append('_');
        descriptorMultToId(builder, typeMultContext.multiplicity().multiplicityArgument()).append('_');
    }

    private static StringBuilder descriptorMultToId(StringBuilder builder, MultiplicityArgumentContext multContext)
    {
        if (multContext.identifier() != null)
        {
            return builder.append(multContext.identifier().getText());
        }

        String fromMult = (multContext.fromMultiplicity() == null) ? null : multContext.fromMultiplicity().getText();
        String toMult = multContext.toMultiplicity().getText();
        if ("*".equals(toMult))
        {
            long lower = (fromMult == null) ? 0L : Long.parseLong(fromMult);
            return (lower == 0) ?
                   builder.append("MANY") :
                   builder.append('$').append(lower).append("_MANY$");
        }

        long upper = Long.parseLong(toMult);
        long lower = (fromMult == null) ? upper : Long.parseLong(fromMult);
        return (lower == upper) ?
               builder.append(upper) :
               builder.append('$').append(lower).append('_').append(upper).append('$');
    }

    private static FunctionDescriptorContext parse(String functionDescriptor) throws InvalidFunctionDescriptorException
    {
        if ((functionDescriptor == null) || functionDescriptor.isEmpty())
        {
            throw new InvalidFunctionDescriptorException(functionDescriptor);
        }

        M3Lexer lexer = new M3Lexer(CharStreams.fromString(functionDescriptor));
        lexer.removeErrorListeners();

        M3Parser parser = new M3Parser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.setErrorHandler(new BailErrorStrategy());
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);

        FunctionDescriptorContext result;
        try
        {
            result = parser.functionDescriptor();
        }
        catch (ParseCancellationException e)
        {
            throw new InvalidFunctionDescriptorException(functionDescriptor, e.getCause());
        }
        catch (Exception e)
        {
            throw new InvalidFunctionDescriptorException(functionDescriptor, e);
        }

        // ensure there's no unparsed text left over
        for (int i = result.getStop().getStopIndex() + 1, len = functionDescriptor.length(); i < len; i++)
        {
            if (!Character.isWhitespace(functionDescriptor.charAt(i)))
            {
                throw new InvalidFunctionDescriptorException(functionDescriptor);
            }
        }
        return result;
    }
}
