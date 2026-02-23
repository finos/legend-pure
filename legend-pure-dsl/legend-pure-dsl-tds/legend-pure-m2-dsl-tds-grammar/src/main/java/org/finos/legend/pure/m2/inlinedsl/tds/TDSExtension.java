// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.m2.inlinedsl.tds;

import io.deephaven.csv.CsvSpecs;
import io.deephaven.csv.parsers.DataType;
import io.deephaven.csv.reading.CsvReader;
import io.deephaven.csv.sinks.SinkFactory;
import io.deephaven.csv.util.CsvReaderException;

import java.util.Arrays;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m2.inlinedsl.tds.processor.TDSProcessor;
import org.finos.legend.pure.m2.inlinedsl.tds.unloader.TDSUnbind;
import org.finos.legend.pure.m2.inlinedsl.tds.validation.TDSVisibilityValidator;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.valuespecification.InstanceValueProcessor;
import org.finos.legend.pure.m3.compiler.validation.validator.GenericTypeValidator;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.TDSCoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.TDS;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.MilestoningDatesVarNamesExtractor;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.VisibilityValidator;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TDSExtension implements InlineDSL
{
    private static final VisibilityValidator VISIBILITY_VALIDATOR = new TDSVisibilityValidator();

    @Override
    public String getName()
    {
        return "TDS";
    }

    @Override
    public boolean match(String code)
    {
        return code.startsWith("TDS");
    }

    @Override
    public CoreInstance parse(String code, ImportGroup importId, String fileName, int columnOffset, int lineOffset, ModelRepository modelRepository, Context context)
    {
        String text = code.substring("TDS".length()).trim();
        Pair<String, GenericType> res = extractBodyAndType(text, header ->
                {
                    CoreInstance expression = new M3AntlrParser().parseExpression(true, "~[" + header + "]", fileName, columnOffset, lineOffset, importId, modelRepository, context);
                    return (GenericType) expression.getValueForMetaPropertyToMany("parametersValues").get(1).getValueForMetaPropertyToOne("genericType");
                }
        );
        return parse(res.getTwo(), res.getOne(), getSourceInfo(code, fileName, columnOffset, lineOffset), new M3ProcessorSupport(context, modelRepository));
    }

    @Override
    public RichIterable<MatchRunner> getValidators()
    {
        return Lists.mutable.empty();
    }

    @Override
    public RichIterable<MatchRunner> getProcessors()
    {
        return Lists.immutable.with(new TDSProcessor());
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadWalkers()
    {
        return Lists.mutable.empty();
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadUnbinders()
    {
        return Lists.immutable.with(new TDSUnbind());
    }

    @Override
    public RichIterable<CoreInstanceFactoryRegistry> getCoreInstanceFactoriesRegistry()
    {
        return Lists.immutable.with(TDSCoreInstanceFactoryRegistry.REGISTRY);
    }

    @Override
    public VisibilityValidator getVisibilityValidator()
    {
        return VISIBILITY_VALIDATOR;
    }

    @Override
    public MilestoningDatesVarNamesExtractor getMilestoningDatesVarNamesExtractor()
    {
        return null;
    }

    public static TDS<?> parse(String text, SourceInformation sourceInfo, ProcessorSupport processorSupport)
    {
        Pair<String, GenericType> res = extractBodyAndType(text, header ->
                {
                    AntlrSourceInformation sourceInformation = new AntlrSourceInformation(0, 0, "", true);
                    org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser parser = M3AntlrParser.initAntlrParser(true, "~[" + header + "]", sourceInformation);

                    return (GenericType) processorSupport.type_wrapGenericType(_RelationType.build(ListIterate.collect(parser.columnBuilders().oneColSpec(), oneColSpec ->
                            {
                                String name = oneColSpec.columnName().getText().trim();
                                if (name.startsWith("'"))
                                {
                                    name = name.substring(1, name.length() - 1).trim();
                                }
                                Multiplicity multiplicity = oneColSpec.multiplicity() == null ? null : processMultiplicity(oneColSpec.multiplicity().multiplicityArgument(), processorSupport);
                                GenericType type = processType(oneColSpec.type(), processorSupport);
                                return _Column.getColumnInstance(name.trim(), false, type, multiplicity, sourceInfo, processorSupport);
                            }
                    ), null, processorSupport));
                }
        );
        return parse(res.getTwo(), res.getOne(), sourceInfo, processorSupport);
    }

    private static GenericType processType(M3Parser.TypeContext type, ProcessorSupport processorSupport)
    {
        GenericType target = (GenericType) processorSupport.newAnonymousCoreInstance(null, M3Paths.GenericType);
        if (type == null)
        {
            target._rawType(null);
        }
        else
        {
            CoreInstance _type = _Package.getByUserPath(type.qualifiedName().getText(), processorSupport);
            if (_type == null)
            {
                throw new PureCompilationException(type.qualifiedName().getText() + " not found!  (imports are not scan for TDS column type resolution)");
            }
            target._rawType((Type) _type)
                    ._typeVariableValues(type.typeVariableValues() == null ? Lists.mutable.empty() : ListIterate.collect(type.typeVariableValues().instanceLiteral(), x ->
                    {
                        InstanceValue res = (InstanceValue)processorSupport.newAnonymousCoreInstance(null, M3Paths.InstanceValue);
                        res._valuesAdd(Integer.valueOf(x.instanceLiteralToken().INTEGER().getText()));
                        InstanceValueProcessor.updateInstanceValue(res, processorSupport);
                        return res;
                    })
                    );
        }
        return target;
    }

    private static Multiplicity processMultiplicity(M3Parser.MultiplicityArgumentContext ctx, ProcessorSupport processorSupport)
    {
        if (ctx.identifier() == null)
        {
            if ((ctx.fromMultiplicity() == null || "1".equals(ctx.fromMultiplicity().getText())) && "1".equals(ctx.toMultiplicity().getText()))
            {
                return (Multiplicity) processorSupport.package_getByUserPath(M3Paths.PureOne);
            }
            else if (ctx.fromMultiplicity() != null && "0".equals(ctx.fromMultiplicity().getText()) && "1".equals(ctx.toMultiplicity().getText()))
            {
                return (Multiplicity) processorSupport.package_getByUserPath(M3Paths.ZeroOne);
            }
            else
            {
                throw new RuntimeException("Not supported yet");
            }
        }
        return null;
    }

    public static Pair<String, GenericType> extractBodyAndType(String text, Function<String, GenericType> res)
    {
        int headerHead = text.indexOf("\n");
        headerHead = headerHead == -1 ? text.length() - 1 : headerHead;
        String header = text.substring(0, headerHead);
        String body = text.substring(headerHead + 1);
        return Tuples.pair(body, res.apply(header));
    }

    public static TDS<?> parse(GenericType headerType, String body, SourceInformation sourceInfo, ProcessorSupport processorSupport)
    {
        RelationType<?> givenRelationType = ((RelationType<?>) headerType._rawType());

        String fullText = givenRelationType._columns().collect(FunctionAccessor::_name).makeString(", ") + "\n" + body;

        CsvReader.Result result;
        try
        {
            result = CsvReader.read(makePureCsvSpecs(), new ByteArrayInputStream(fullText.getBytes(StandardCharsets.UTF_8)), makePureSinkFactory());
        }
        catch (CsvReaderException e)
        {
            throw new PureCompilationException(sourceInfo, e.getCause().getMessage());
        }

        RelationType<?> relationType = _RelationType.build(ListIterate.zip(Arrays.asList(result.columns()), givenRelationType._columns()).collect(c ->
        {
            GenericType columnType = _Column.getColumnType(c.getTwo());
            Multiplicity multiplicity = _Column.getColumnMultiplicity(c.getTwo());
            if (multiplicity == null)
            {
                multiplicity = (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.newMultiplicity(0, 1, processorSupport);
            }
            if (columnType == null || columnType.getValueForMetaPropertyToOne("rawType") == null)
            {
                return _Column.getColumnInstance(c.getTwo()._name(), false, convertType(c.getOne().dataType()), multiplicity, c.getTwo()._stereotypesCoreInstance(), false, c.getTwo()._taggedValues(), sourceInfo, processorSupport);
            }
            else
            {
                return _Column.getColumnInstance(c.getTwo()._name(), false, (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericType(columnType, sourceInfo, processorSupport), multiplicity, c.getTwo()._stereotypesCoreInstance(), false, c.getTwo()._taggedValues(), sourceInfo, processorSupport);
            }
        }), sourceInfo, processorSupport);

        Class<?> tdsType = (Class<?>) processorSupport.package_getByUserPath(M2TDSPaths.TDS);
        GenericType typeParam = ((GenericType) processorSupport.newAnonymousCoreInstance(sourceInfo, M3Paths.GenericType))._rawType(relationType);
        GenericType tdsGenericType = ((GenericType) processorSupport.newAnonymousCoreInstance(sourceInfo, M3Paths.GenericType))
                ._rawType(tdsType)
                ._typeArgumentsAdd(typeParam);
        GenericTypeValidator.validateGenericType(tdsGenericType, processorSupport);
        return ((TDS<?>) processorSupport.newAnonymousCoreInstance(sourceInfo, M2TDSPaths.TDS))
                ._classifierGenericType(tdsGenericType)
                ._csv(fullText.replace("\r\n", "\n"));
    }

    private static SourceInformation getSourceInfo(String text, String fileName, int columnOffset, int lineOffset)
    {
        int endLine = lineOffset;
        int endLineIndex = 0;
        Matcher matcher = Pattern.compile("\\R").matcher(text);
        while (matcher.find())
        {
            endLine++;
            endLineIndex = matcher.end();
        }

        int endColumn = (endLine == lineOffset) ? (text.length() + columnOffset - 1) : (text.length() - endLineIndex);
        return new SourceInformation(fileName, lineOffset, columnOffset, endLine, endColumn);
    }

    private static String convertType(DataType dataType)
    {
        switch (dataType)
        {
            case BOOLEAN_AS_BYTE:
            {
                return M3Paths.Boolean;
            }
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            {
                return M3Paths.Integer;
            }
            case DATETIME_AS_LONG:
            {
                return M3Paths.Date;
            }
            case FLOAT:
            case DOUBLE:
            {
                return M3Paths.Float;
            }
            case STRING:
            case CHAR:
            {
                return M3Paths.String;
            }
            case TIMESTAMP_AS_LONG:
            case CUSTOM:
            {
                throw new RuntimeException("Not possible");
            }
            default:
            {
                // TODO is this correct?
                return "";
            }
        }
    }

    public static CsvSpecs makePureCsvSpecs()
    {
        return CsvSpecs.builder().nullValueLiterals(Arrays.asList("", "null")).build();
    }

    public static SinkFactory makePureSinkFactory()
    {
        return SinkFactory.arrays(
                null,
                null,
                2_147_483_647, //largest prime for 32 signed numbers
                9_223_372_036_854_775_783L, //largest prime for 64 signed numbers
                Float.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Byte.MIN_VALUE,
                Character.MIN_VALUE,
                null,
                Long.MIN_VALUE,
                Long.MIN_VALUE);
    }
}
