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

package org.finos.legend.pure.lsp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class SemanticTokensProvider
{
    public static final List<String> TOKEN_TYPES = Collections.unmodifiableList(Arrays.asList(
            "namespace", "class", "enum", "function", "property",
            "enumMember", "type", "parameter", "interface", "struct",
            "variable"
    ));

    public static final List<String> TOKEN_MODIFIERS = Collections.unmodifiableList(Arrays.asList(
            "definition", "declaration"
    ));

    private static final int TYPE_NAMESPACE = 0;
    private static final int TYPE_CLASS = 1;
    private static final int TYPE_ENUM = 2;
    private static final int TYPE_FUNCTION = 3;
    private static final int TYPE_PROPERTY = 4;
    private static final int TYPE_ENUM_MEMBER = 5;
    private static final int TYPE_TYPE = 6;
    private static final int TYPE_PARAMETER = 7;
    private static final int TYPE_INTERFACE = 8;
    private static final int TYPE_STRUCT = 9;
    private static final int TYPE_VARIABLE = 10;

    private static final int MOD_DEFINITION = 1;

    // Skipped — TextMate already colors these
    private static final Set<String> OPERATOR_FUNCTION_NAMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "letFunction", "if", "match", "new",
            "plus", "minus", "times", "divide",
            "equal", "not", "and", "or",
            "lessThan", "lessThanEqual", "greaterThan", "greaterThanEqual"
    )));

    private static final int MAX_WALK_DEPTH = 50;

    // Kept tight so a token can only ever move across adjacent punctuation ('.', '::', '->'),
    // never onto an unrelated identifier that happens to repeat the same name
    private static final int NAME_SNAP_WINDOW = 4;

    public static List<Integer> getTokens(PureRuntime runtime, String sourceId)
    {
        Source source = runtime.getSourceById(sourceId);
        if (source == null)
        {
            return Collections.emptyList();
        }

        List<RawToken> tokens = new ArrayList<>();
        collectTokensFromSource(new SourceContext(source), runtime, tokens);

        tokens.sort(Comparator.comparingInt((RawToken t) -> t.line)
                .thenComparingInt(t -> t.column));

        return encodeDelta(tokens);
    }

    private static void collectTokensFromSource(SourceContext source, PureRuntime runtime, List<RawToken> tokens)
    {
        ListIterable<? extends CoreInstance> newInstances = source.getNewInstances();
        if (newInstances == null)
        {
            return;
        }

        for (CoreInstance instance : newInstances)
        {
            if (instance instanceof Package)
            {
                continue;
            }

            String classifierName = instance.getClassifier().getName();
            SourceInformation si = instance.getSourceInformation();
            if (si == null || !sourceId(source).equals(si.getSourceId()))
            {
                continue;
            }

            switch (classifierName)
            {
                case "Class":
                    addDefinitionToken(tokens, instance, TYPE_CLASS, source);
                    addClassMembers(tokens, instance, runtime, source);
                    break;
                case "Enumeration":
                    addDefinitionToken(tokens, instance, TYPE_ENUM, source);
                    addEnumValues(tokens, instance, source);
                    break;
                case "ConcreteFunctionDefinition":
                    addDefinitionToken(tokens, instance, TYPE_FUNCTION, source);
                    addFunctionSignatureTokens(tokens, instance, runtime, source);
                    addFunctionBodyTokens(tokens, instance, runtime, source);
                    break;
                case "NativeFunction":
                    addDefinitionToken(tokens, instance, TYPE_FUNCTION, source);
                    break;
                case "Profile":
                    addDefinitionToken(tokens, instance, TYPE_INTERFACE, source);
                    break;
                case "Association":
                    addDefinitionToken(tokens, instance, TYPE_STRUCT, source);
                    addClassMembers(tokens, instance, runtime, source);
                    break;
                default:
                    break;
            }
        }
    }

    private static void addDefinitionToken(List<RawToken> tokens, CoreInstance element, int tokenType, SourceContext source)
    {
        SourceInformation si = element.getSourceInformation();
        if (si == null)
        {
            return;
        }

        // getName() returns mangled signatures for functions (e.g. "greet_String_1__String_1_")
        String name = element.getName();
        if (name == null || name.startsWith("@"))
        {
            return;
        }

        String shortName = name;
        if (tokenType == TYPE_FUNCTION)
        {
            int underscoreIdx = name.indexOf('_');
            if (underscoreIdx > 0)
            {
                shortName = name.substring(0, underscoreIdx);
            }
        }

        tokens.add(source.token(si.getLine(), si.getColumn(), shortName, tokenType, MOD_DEFINITION));
    }

    private static void addClassMembers(List<RawToken> tokens, CoreInstance classElement, PureRuntime runtime, SourceContext source)
    {
        try
        {
            ListIterable<? extends CoreInstance> properties = classElement.getValueForMetaPropertyToMany(M3Properties.properties);
            if (properties != null)
            {
                for (CoreInstance prop : properties)
                {
                    SourceInformation propSi = prop.getSourceInformation();
                    if (propSi == null || !sourceId(source).equals(propSi.getSourceId()))
                    {
                        continue;
                    }

                    String propName = prop.getName();
                    if (propName != null && !propName.startsWith("@"))
                    {
                        tokens.add(source.token(propSi.getStartLine(), propSi.getStartColumn(),
                                propName, TYPE_PROPERTY, MOD_DEFINITION));
                    }

                    addTypeReference(tokens, prop, runtime, source);
                }
            }
        }
        catch (Exception ignored)
        {
            // Properties not accessible
        }
    }

    private static void addTypeReference(List<RawToken> tokens, CoreInstance prop, PureRuntime runtime, SourceContext source)
    {
        try
        {
            CoreInstance genericType = prop.getValueForMetaPropertyToOne(M3Properties.genericType);
            if (genericType == null)
            {
                return;
            }
            CoreInstance rawType = genericType.getValueForMetaPropertyToOne(M3Properties.rawType);
            if (rawType != null)
            {
                rawType = ImportStub.withImportStubByPass(rawType, runtime.getProcessorSupport());
            }
            if (rawType == null)
            {
                return;
            }
            SourceInformation typeSi = genericType.getSourceInformation();
            if (typeSi == null || !sourceId(source).equals(typeSi.getSourceId()))
            {
                return;
            }
            String typeName = rawType.getName();
            if (typeName != null && !typeName.startsWith("@"))
            {
                tokens.add(source.token(typeSi.getStartLine(), typeSi.getStartColumn(),
                        typeName, TYPE_TYPE, 0));
            }
        }
        catch (Exception ignored)
        {
            // Type info not accessible
        }
    }

    private static void addEnumValues(List<RawToken> tokens, CoreInstance enumeration, SourceContext source)
    {
        try
        {
            ListIterable<? extends CoreInstance> values = enumeration.getValueForMetaPropertyToMany(M3Properties.values);
            if (values != null)
            {
                for (CoreInstance val : values)
                {
                    SourceInformation valSi = val.getSourceInformation();
                    if (valSi == null || !sourceId(source).equals(valSi.getSourceId()))
                    {
                        continue;
                    }
                    String valName = val.getName();
                    if (valName != null)
                    {
                        tokens.add(source.token(valSi.getStartLine(), valSi.getStartColumn(),
                                valName, TYPE_ENUM_MEMBER, 0));
                    }
                }
            }
        }
        catch (Exception ignored)
        {
            // Values not accessible
        }
    }

    private static void addFunctionSignatureTokens(List<RawToken> tokens, CoreInstance function,
                                                    PureRuntime runtime, SourceContext source)
    {
        try
        {
            CoreInstance classifierGT = function.getValueForMetaPropertyToOne(M3Properties.classifierGenericType);
            if (classifierGT == null)
            {
                return;
            }
            ListIterable<? extends CoreInstance> typeArgs = classifierGT.getValueForMetaPropertyToMany(M3Properties.typeArguments);
            if (typeArgs == null || typeArgs.isEmpty())
            {
                return;
            }
            CoreInstance functionTypeGT = typeArgs.get(0);
            CoreInstance functionType = functionTypeGT.getValueForMetaPropertyToOne(M3Properties.rawType);
            if (functionType == null)
            {
                return;
            }

            // param.getName() returns a synthetic hash; actual name is in M3Properties.name
            ListIterable<? extends CoreInstance> params = functionType.getValueForMetaPropertyToMany(M3Properties.parameters);
            if (params != null)
            {
                for (CoreInstance param : params)
                {
                    SourceInformation paramSi = param.getSourceInformation();
                    if (paramSi == null || !sourceId(source).equals(paramSi.getSourceId()))
                    {
                        continue;
                    }
                    CoreInstance nameCI = param.getValueForMetaPropertyToOne(M3Properties.name);
                    String paramName = nameCI != null ? nameCI.getName() : null;
                    if (paramName != null)
                    {
                        tokens.add(source.token(paramSi.getLine(), paramSi.getColumn(),
                                paramName, TYPE_PARAMETER, 0));
                    }
                    addTypeReference(tokens, param, runtime, source);
                }
            }

            CoreInstance returnTypeGT = functionType.getValueForMetaPropertyToOne(M3Properties.returnType);
            if (returnTypeGT != null)
            {
                CoreInstance rawReturnType = returnTypeGT.getValueForMetaPropertyToOne(M3Properties.rawType);
                if (rawReturnType != null)
                {
                    rawReturnType = ImportStub.withImportStubByPass(rawReturnType, runtime.getProcessorSupport());
                }
                if (rawReturnType != null)
                {
                    SourceInformation retSi = returnTypeGT.getSourceInformation();
                    if (retSi != null && sourceId(source).equals(retSi.getSourceId()))
                    {
                        String typeName = rawReturnType.getName();
                        if (typeName != null && !typeName.startsWith("@"))
                        {
                            tokens.add(source.token(retSi.getStartLine(), retSi.getStartColumn(),
                                    typeName, TYPE_TYPE, 0));
                        }
                    }
                }
            }
        }
        catch (Exception ignored)
        {
            // Function type info not accessible
        }
    }

    // ── Function body expression tree walker ──────────────────────────────────

    private static void addFunctionBodyTokens(List<RawToken> tokens, CoreInstance function,
                                               PureRuntime runtime, SourceContext source)
    {
        try
        {
            ListIterable<? extends CoreInstance> exprs = function.getValueForMetaPropertyToMany(M3Properties.expressionSequence);
            if (exprs != null)
            {
                for (CoreInstance expr : exprs)
                {
                    walkExpression(tokens, expr, runtime, source, 0);
                }
            }
        }
        catch (Exception ignored)
        {
        }
    }

    private static void walkExpression(List<RawToken> tokens, CoreInstance expr,
                                        PureRuntime runtime, SourceContext source, int depth)
    {
        if (expr == null || depth > MAX_WALK_DEPTH)
        {
            return;
        }

        String classifierName = expr.getClassifier().getName();

        switch (classifierName)
        {
            case "SimpleFunctionExpression":
                walkSimpleFunctionExpression(tokens, expr, runtime, source, depth);
                break;
            case "VariableExpression":
                walkVariableExpression(tokens, expr, source);
                break;
            case "InstanceValue":
                walkInstanceValue(tokens, expr, runtime, source, depth);
                break;
            case "LambdaFunction":
                walkLambdaFunction(tokens, expr, runtime, source, depth);
                break;
            default:
                walkChildren(tokens, expr, runtime, source, depth);
                break;
        }
    }

    private static void walkSimpleFunctionExpression(List<RawToken> tokens, CoreInstance expr,
                                                      PureRuntime runtime, SourceContext source, int depth)
    {
        try
        {
            SourceInformation si = expr.getSourceInformation();
            CoreInstance func = expr.getValueForMetaPropertyToOne(M3Properties.func);
            if (func != null)
            {
                func = ImportStub.withImportStubByPass(func, runtime.getProcessorSupport());
            }

            if (func != null && si != null && sourceId(source).equals(si.getSourceId()))
            {
                String funcClassifier = func.getClassifier().getName();

                if ("Property".equals(funcClassifier) || "QualifiedProperty".equals(funcClassifier))
                {
                    // Property access: $person.name → color "name" as property
                    String propName = func.getName();
                    if (propName != null && !propName.startsWith("@"))
                    {
                        tokens.add(source.token(si.getLine(), si.getColumn(), propName, TYPE_PROPERTY, 0));
                    }
                }
                else
                {
                    // Function call: ->toUpper(), ->map(), etc.
                    CoreInstance fnNameCI = expr.getValueForMetaPropertyToOne(M3Properties.functionName);
                    String fnName = fnNameCI != null ? fnNameCI.getName() : null;

                    if ("letFunction".equals(fnName))
                    {
                        // Handle let bindings: color the variable name, not the "let" keyword
                        addLetVariableName(tokens, expr, source);
                    }
                    else if (fnName != null && !OPERATOR_FUNCTION_NAMES.contains(fnName))
                    {
                        tokens.add(source.token(si.getLine(), si.getColumn(), fnName, TYPE_FUNCTION, 0));
                    }
                }
            }

            // Recurse into arguments
            ListIterable<? extends CoreInstance> args = expr.getValueForMetaPropertyToMany(M3Properties.parametersValues);
            if (args != null)
            {
                for (CoreInstance arg : args)
                {
                    walkExpression(tokens, arg, runtime, source, depth + 1);
                }
            }
        }
        catch (Exception ignored)
        {
        }
    }

    // In letFunction, the variable name is the first InstanceValue argument
    private static void addLetVariableName(List<RawToken> tokens, CoreInstance letExpr, SourceContext source)
    {
        try
        {
            ListIterable<? extends CoreInstance> args = letExpr.getValueForMetaPropertyToMany(M3Properties.parametersValues);
            if (args != null && args.size() >= 1)
            {
                CoreInstance nameIV = args.get(0);
                SourceInformation nameSi = nameIV.getSourceInformation();
                if (nameSi != null && sourceId(source).equals(nameSi.getSourceId()))
                {
                    ListIterable<? extends CoreInstance> vals = nameIV.getValueForMetaPropertyToMany("values");
                    if (vals != null && vals.size() == 1)
                    {
                        String varName = vals.get(0).getName();
                        if (varName != null)
                        {
                            tokens.add(source.token(nameSi.getLine(), nameSi.getColumn(),
                                    varName, TYPE_VARIABLE, MOD_DEFINITION));
                        }
                    }
                }
            }
        }
        catch (Exception ignored)
        {
        }
    }

    private static void walkVariableExpression(List<RawToken> tokens, CoreInstance expr, SourceContext source)
    {
        try
        {
            SourceInformation si = expr.getSourceInformation();
            if (si == null || !sourceId(source).equals(si.getSourceId()))
            {
                return;
            }
            CoreInstance nameCI = expr.getValueForMetaPropertyToOne(M3Properties.name);
            String varName = nameCI != null ? nameCI.getName() : null;
            if (varName != null)
            {
                tokens.add(source.token(si.getLine(), si.getColumn(), varName, TYPE_VARIABLE, 0));
            }
        }
        catch (Exception ignored)
        {
        }
    }

    private static void walkInstanceValue(List<RawToken> tokens, CoreInstance expr,
                                           PureRuntime runtime, SourceContext source, int depth)
    {
        try
        {
            // Recurse into values that are sub-expressions (lambdas, nested expressions)
            ListIterable<? extends CoreInstance> vals = expr.getValueForMetaPropertyToMany("values");
            if (vals != null)
            {
                for (CoreInstance val : vals)
                {
                    String vcn = val.getClassifier().getName();
                    if (vcn.contains("Expression") || vcn.contains("Function") || vcn.contains("Lambda"))
                    {
                        walkExpression(tokens, val, runtime, source, depth + 1);
                    }
                }
            }
        }
        catch (Exception ignored)
        {
        }
    }

    private static void walkLambdaFunction(List<RawToken> tokens, CoreInstance expr,
                                            PureRuntime runtime, SourceContext source, int depth)
    {
        try
        {
            addLambdaParameterTokens(tokens, expr, source);

            ListIterable<? extends CoreInstance> exprs = expr.getValueForMetaPropertyToMany(M3Properties.expressionSequence);
            if (exprs != null)
            {
                for (CoreInstance e : exprs)
                {
                    walkExpression(tokens, e, runtime, source, depth + 1);
                }
            }
        }
        catch (Exception ignored)
        {
        }
    }

    // A lambda's own bound parameters (e.g. "a" in "a | $a->..." ) live before the expressionSequence,
    // reached the same way a top-level function's parameters are in addFunctionSignatureTokens().
    private static void addLambdaParameterTokens(List<RawToken> tokens, CoreInstance lambda, SourceContext source)
    {
        try
        {
            CoreInstance classifierGT = lambda.getValueForMetaPropertyToOne(M3Properties.classifierGenericType);
            if (classifierGT == null)
            {
                return;
            }
            ListIterable<? extends CoreInstance> typeArgs = classifierGT.getValueForMetaPropertyToMany(M3Properties.typeArguments);
            if (typeArgs == null || typeArgs.isEmpty())
            {
                return;
            }
            CoreInstance functionTypeGT = typeArgs.get(0);
            CoreInstance functionType = functionTypeGT.getValueForMetaPropertyToOne(M3Properties.rawType);
            if (functionType == null)
            {
                return;
            }

            ListIterable<? extends CoreInstance> params = functionType.getValueForMetaPropertyToMany(M3Properties.parameters);
            if (params != null)
            {
                for (CoreInstance param : params)
                {
                    SourceInformation paramSi = param.getSourceInformation();
                    if (paramSi == null || !sourceId(source).equals(paramSi.getSourceId()))
                    {
                        continue;
                    }
                    CoreInstance nameCI = param.getValueForMetaPropertyToOne(M3Properties.name);
                    String paramName = nameCI != null ? nameCI.getName() : null;
                    if (paramName != null)
                    {
                        tokens.add(source.token(paramSi.getLine(), paramSi.getColumn(),
                                paramName, TYPE_PARAMETER, 0));
                    }
                }
            }
        }
        catch (Exception ignored)
        {
        }
    }

    private static void walkChildren(List<RawToken> tokens, CoreInstance expr,
                                      PureRuntime runtime, SourceContext source, int depth)
    {
        try
        {
            ListIterable<? extends CoreInstance> args = expr.getValueForMetaPropertyToMany(M3Properties.parametersValues);
            if (args != null)
            {
                for (CoreInstance arg : args)
                {
                    walkExpression(tokens, arg, runtime, source, depth + 1);
                }
            }
            ListIterable<? extends CoreInstance> exprs = expr.getValueForMetaPropertyToMany(M3Properties.expressionSequence);
            if (exprs != null)
            {
                for (CoreInstance e : exprs)
                {
                    walkExpression(tokens, e, runtime, source, depth + 1);
                }
            }
        }
        catch (Exception ignored)
        {
        }
    }

    private static String sourceId(SourceContext source)
    {
        return source.id;
    }

    /**
     * Resolves the 1-based column at which {@code name} actually starts on the given line.
     *
     * <p>A Pure {@code SourceInformation} column can point at the punctuation introducing the
     * identifier rather than the identifier itself (the {@code .} of a property access, the
     * {@code ::} before a function's simple name), which would paint the token's fixed length
     * starting too early. Falls back to the reported column when the name cannot be located,
     * so a token is never moved somewhere less accurate than before.</p>
     */
    static int snapToNameColumn(String[] sourceLines, int line1Based, int reportedColumn1Based, String name)
    {
        if (sourceLines == null || name == null || name.isEmpty()
                || line1Based < 1 || line1Based > sourceLines.length || reportedColumn1Based < 1)
        {
            return Math.max(1, reportedColumn1Based);
        }

        String lineText = sourceLines[line1Based - 1];
        int reportedIndex = reportedColumn1Based - 1;
        if (lineText == null || reportedIndex >= lineText.length())
        {
            return reportedColumn1Based;
        }

        if (lineText.startsWith(name, reportedIndex))
        {
            return reportedColumn1Based;
        }

        // Nearest occurrence within the window, preferring one after the reported column
        for (int offset = 1; offset <= NAME_SNAP_WINDOW; offset++)
        {
            if (lineText.startsWith(name, reportedIndex + offset))
            {
                return reportedIndex + offset + 1;
            }
            if (reportedIndex - offset >= 0 && lineText.startsWith(name, reportedIndex - offset))
            {
                return reportedIndex - offset + 1;
            }
        }
        return reportedColumn1Based;
    }

    /**
     * Measures the identifier that actually starts at the given column, so a token paints exactly
     * that identifier.
     *
     * <p>A name taken from the model can be longer than the text at the token's column — a call's
     * {@code functionName} is package-qualified and a qualified property's name carries its
     * parameter types — which would spill the token over the following punctuation. Falls back to
     * {@code fallbackLength} whenever the column does not begin an identifier.</p>
     *
     * <p>Matches the M3 grammar's {@code ValidString}: {@code [A-Za-z0-9_] [A-Za-z0-9_$]*}.</p>
     */
    static int identifierLengthAt(String[] sourceLines, int line1Based, int column1Based, int fallbackLength)
    {
        if (sourceLines == null || line1Based < 1 || line1Based > sourceLines.length || column1Based < 1)
        {
            return Math.max(1, fallbackLength);
        }

        String lineText = sourceLines[line1Based - 1];
        int start = column1Based - 1;
        if (lineText == null || start >= lineText.length() || !isIdentifierStart(lineText.charAt(start)))
        {
            return Math.max(1, fallbackLength);
        }

        int end = start + 1;
        while (end < lineText.length() && isIdentifierPart(lineText.charAt(end)))
        {
            end++;
        }
        return end - start;
    }

    private static boolean isIdentifierStart(char c)
    {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == '_');
    }

    private static boolean isIdentifierPart(char c)
    {
        return isIdentifierStart(c) || (c == '$');
    }

    // Splits the source once per request; snapping every named token would otherwise re-split the whole file
    private static final class SourceContext
    {
        private final String id;
        private final String[] lines;
        private final Source source;

        SourceContext(Source source)
        {
            this.source = source;
            this.id = source.getId();
            String content = source.getContent();
            this.lines = (content == null) ? new String[0] : content.split("\\R", -1);
        }

        ListIterable<? extends CoreInstance> getNewInstances()
        {
            return this.source.getNewInstances();
        }

        int snap(int line1Based, int reportedColumn1Based, String name)
        {
            return snapToNameColumn(this.lines, line1Based, reportedColumn1Based, name);
        }

        // Column and length are resolved together so a token always spans exactly the identifier it lands on
        RawToken token(int line1Based, int reportedColumn1Based, String name, int tokenType, int tokenModifiers)
        {
            int column = snap(line1Based, reportedColumn1Based, name);
            int length = identifierLengthAt(this.lines, line1Based, column, name.length());
            return new RawToken(line1Based, column, length, tokenType, tokenModifiers);
        }
    }

    /**
     * Encode tokens as LSP delta format: [deltaLine, deltaStartChar, length, tokenType, tokenModifiers]
     */
    private static List<Integer> encodeDelta(List<RawToken> tokens)
    {
        List<Integer> data = new ArrayList<>(tokens.size() * 5);
        int prevLine = 0;
        int prevCol = 0;

        for (RawToken token : tokens)
        {
            int line = token.line - 1; // Convert to 0-based
            int col = token.column - 1;

            int deltaLine = line - prevLine;
            int deltaCol = (deltaLine == 0) ? (col - prevCol) : col;

            data.add(deltaLine);
            data.add(deltaCol);
            data.add(token.length);
            data.add(token.tokenType);
            data.add(token.tokenModifiers);

            prevLine = line;
            prevCol = col;
        }
        return data;
    }

    private static class RawToken
    {
        final int line;     // 1-based
        final int column;   // 1-based
        final int length;
        final int tokenType;
        final int tokenModifiers;

        RawToken(int line, int column, int length, int tokenType, int tokenModifiers)
        {
            this.line = line;
            this.column = column;
            this.length = Math.max(1, length);
            this.tokenType = tokenType;
            this.tokenModifiers = tokenModifiers;
        }
    }
}
