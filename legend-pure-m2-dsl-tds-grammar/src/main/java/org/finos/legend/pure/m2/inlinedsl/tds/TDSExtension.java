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
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m2.inlinedsl.tds.processor.TDSProcessor;
import org.finos.legend.pure.m2.inlinedsl.tds.unloader.TDSUnbind;
import org.finos.legend.pure.m2.inlinedsl.tds.validation.TDSVisibilityValidator;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.TDSCoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.TDS;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.Instance;
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

public class TDSExtension implements InlineDSL
{
    private static VisibilityValidator VISIBILITY_VALIDATOR = new TDSVisibilityValidator();

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
    public CoreInstance parse(String code, ImportGroup importId, String fileName, int offsetX, int offsetY, ModelRepository modelRepository, Context context)
    {
        String val = code.substring("TDS".length()).trim();

        final CsvReader.Result result;
        try
        {
            result = CsvReader.read(CsvSpecs.csv(), new ByteArrayInputStream(val.getBytes()), SinkFactory.arrays());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        ProcessorSupport processorSupport = new M3ProcessorSupport(context, modelRepository);
        SourceInformation src = new SourceInformation(fileName, 0, 0, 0, 0);
        Class<?> tdsType = (Class<?>) processorSupport.package_getByUserPath(M2TDSPaths.TDS);
        TDS<?> tds = ((TDS<?>) modelRepository.newEphemeralCoreInstance("", tdsType, src));
        GenericType tdsGenericType = (GenericType) processorSupport.newAnonymousCoreInstance(src, M3Paths.GenericType);
        tdsGenericType._rawTypeCoreInstance(tdsType);
        GenericType typeParam = (GenericType) processorSupport.newAnonymousCoreInstance(src, M3Paths.GenericType);
        typeParam._rawType(_RelationType.build(ArrayIterate.collect(result.columns(), c -> _Column.getColumnInstance(c.name(), false, typeParam, convertType(c.dataType()), src, processorSupport)), src, processorSupport));
        tdsGenericType._typeArgumentsAdd(typeParam);
        tds._classifierGenericType(tdsGenericType);
        Instance.setValueForProperty(tds, "csv", modelRepository.newStringCoreInstance(val), processorSupport);
        return tds;
    }

    private String convertType(DataType dataType)
    {
        String value = "";
        switch (dataType)
        {
            case BOOLEAN_AS_BYTE:
                value = "Boolean";
                break;
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            case DATETIME_AS_LONG:
            case TIMESTAMP_AS_LONG:
                value = "Integer";
                break;
            case FLOAT:
            case DOUBLE:
                value = "Float";
                break;
            case STRING:
            case CHAR:
                value = "String";
                break;
            case CUSTOM:
                throw new RuntimeException("Not possible");
        }
        return value;
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
}
