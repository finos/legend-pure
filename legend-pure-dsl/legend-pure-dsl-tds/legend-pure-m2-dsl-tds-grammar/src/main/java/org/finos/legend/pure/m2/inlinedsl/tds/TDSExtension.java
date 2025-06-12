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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m2.inlinedsl.tds.processor.TDSProcessor;
import org.finos.legend.pure.m2.inlinedsl.tds.unloader.TDSUnbind;
import org.finos.legend.pure.m2.inlinedsl.tds.validation.TDSVisibilityValidator;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.TDSCoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.TDS;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.MilestoningDatesVarNamesExtractor;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.VisibilityValidator;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

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
        SourceInformation sourceInfo = getSourceInfo(code, fileName, columnOffset, lineOffset);
        ProcessorSupport processorSupport = new M3ProcessorSupport(context, modelRepository);
        return parse(text, sourceInfo, processorSupport);
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

    public static TDS<?> parse(String text, ProcessorSupport processorSupport)
    {
        return parse(text, (SourceInformation) null, processorSupport);
    }

    public static TDS<?> parse(String text, String fileName, ProcessorSupport processorSupport)
    {
        return parse(text, (fileName == null) ? null : getSourceInfo(text, fileName, 0, 0), processorSupport);
    }

    public static TDS<?> parse(String text, SourceInformation sourceInfo, ProcessorSupport processorSupport)
    {
        CsvReader.Result result;
        try
        {
            result = CsvReader.read(makePureCsvSpecs(), new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), makePureSinkFactory());
        }
        catch (CsvReaderException e)
        {
            throw new RuntimeException(e);
        }

        Class<?> tdsType = (Class<?>) processorSupport.package_getByUserPath(M2TDSPaths.TDS);
        GenericType typeParam = ((GenericType) processorSupport.newAnonymousCoreInstance(sourceInfo, M3Paths.GenericType))
                ._rawType(_RelationType.build(ArrayIterate.collect(result.columns(), c ->
                {
                    Pair<String, String> nameAndType = getNameAndType(c);
                    return _Column.getColumnInstance(nameAndType.getOne(), false, nameAndType.getTwo(), (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.newMultiplicity(0, 1, processorSupport), sourceInfo, processorSupport);
                }), sourceInfo, processorSupport));
        GenericType tdsGenericType = ((GenericType) processorSupport.newAnonymousCoreInstance(sourceInfo, M3Paths.GenericType))
                ._rawType(tdsType)
                ._typeArgumentsAdd(typeParam);

        return ((TDS<?>) processorSupport.newAnonymousCoreInstance(sourceInfo, M2TDSPaths.TDS))
                ._classifierGenericType(tdsGenericType)
                ._csv(text);
    }

    private static Pair<String, String> getNameAndType(CsvReader.ResultColumn c)
    {
        String name;
        String type;
        int typeSplit = c.name().indexOf(':');

        if (typeSplit != -1)
        {
            name = c.name().substring(0, typeSplit);
            type = c.name().substring(typeSplit + 1).trim();
            // todo check compatibility of inferred type vs explicit type
        }
        else
        {
            name = c.name();
            type = convertType(c.dataType());
        }

        return Tuples.pair(name, type);
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
