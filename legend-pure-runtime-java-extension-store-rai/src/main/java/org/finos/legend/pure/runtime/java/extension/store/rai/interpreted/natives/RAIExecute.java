package org.finos.legend.pure.runtime.java.extension.store.rai.interpreted.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;
import com.relationalai.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Stack;


public class RAIExecute extends NativeFunction {
    private final ModelRepository repository;

    private static final String PURE_RAI_RESULT = "meta::rel::execute::RAIResult";
    private static final String PURE_COLUMN = "meta::rel::execute::Column";
    private static final String PURE_OUTPUT = "meta::rel::execute::Output";
    private static final String PURE_PROBLEM = "meta::rel::execute::Problem";
    private static final String PURE_REL_KEY = "meta::rel::execute::RelKey";

    public RAIExecute(ModelRepository repository, Message message) {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(
            ListIterable<? extends CoreInstance> params,
            Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters,
            Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters,
            VariableContext variableContext,
            CoreInstance functionExpressionToUseInStack,
            Profiler profiler,
            InstantiationContext instantiationContext,
            ExecutionSupport executionSupport,
            Context context,
            ProcessorSupport processorSupport
    ) throws PureExecutionException {
        CoreInstance connection = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);
        String source = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport).getName();


        CoreInstance pureResult = this.doExecute(connection, source, functionExpressionToUseInStack, processorSupport);

        return ValueSpecificationBootstrap.wrapValueSpecification(pureResult, true, processorSupport);
    }

    private CoreInstance instance(String userPath, CoreInstance functionExpressionToUseInStack, ProcessorSupport processorSupport) {
        return this.repository.newAnonymousCoreInstance(
                functionExpressionToUseInStack.getSourceInformation(),
                processorSupport.package_getByUserPath(userPath)
        );
    }

    private FastList<CoreInstance> stringInstances(String[] values) {
        FastList<CoreInstance> ret = FastList.newList(values.length);
        for (String value : values) {
            ret.add(this.repository.newStringCoreInstance(value));
        }

        return ret;
    }

    private CoreInstance doExecute(
            CoreInstance connection,
            String source,
            CoreInstance functionExpressionToUseInStack,
            ProcessorSupport processorSupport
    ) {
        CoreInstance pResult = this.instance(PURE_RAI_RESULT, functionExpressionToUseInStack, processorSupport);

        String host = Instance.getValueForMetaPropertyToOneResolved(connection, "host", processorSupport).getName();
        String port = Instance.getValueForMetaPropertyToOneResolved(connection, "port", processorSupport).getName();
        String scheme = Instance.getValueForMetaPropertyToOneResolved(connection, "scheme", processorSupport).getName();
        String database = Instance.getValueForMetaPropertyToOneResolved(connection, "database", processorSupport).getName();
        String engine = Instance.getValueForMetaPropertyToOneResolved(connection, "engine", processorSupport).getName();

        CoreInstance clientCredentials = Instance.getValueForMetaPropertyToOneResolved(connection, "credentials", processorSupport);
        String clientId =
                clientCredentials == null ? null : Instance.getValueForMetaPropertyToOneResolved(clientCredentials, "clientId", processorSupport).getName();
        String clientSecret =
                clientCredentials == null ? null : Instance.getValueForMetaPropertyToOneResolved(clientCredentials, "clientSecret", processorSupport).getName();
        CoreInstance iClientCredentialsURL = clientCredentials != null ? Instance.getValueForMetaPropertyToOneResolved(clientCredentials, "clientCredentialsURL", processorSupport) : null;

        String clientCredentialsURL = iClientCredentialsURL == null ? null : iClientCredentialsURL.getName();

        Config config;
        try {
            String contents = String.format("[default]\nhost = %s\nport = %s\nscheme = %s", host, port, scheme);
            if (clientCredentials != null) {
                contents = String.format("%s\nclient_id = %s\nclient_secret = %s", contents, clientId, clientSecret);

                if (clientCredentialsURL != null)
                    contents = String.format("%s\nclient_credentials_url = %s", contents, clientCredentialsURL);
            }

            config = Config.loadConfig(new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new PureExecutionException(e.getMessage());
        }

        Client client = new Client(config);
        TransactionResult response;
        try {
            response = client.executeV1(database, engine, source, false);
        } catch (HttpError e) {
            throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), e.toString(), e);
        } catch (InterruptedException | IOException e) {
            throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), e.getMessage(), e);
        }

        MutableList<CoreInstance> pProblems = FastList.newList(response.problems.length);
        for (int i = 0; i < response.problems.length; i++) {
            CoreInstance pProblem = this.instance(PURE_PROBLEM, functionExpressionToUseInStack, processorSupport);
            Instance.setValueForProperty(pProblem, "type", repository.newStringCoreInstance(response.problems[i].type), processorSupport);
            Instance.setValueForProperty(pProblem, "message", repository.newStringCoreInstance(response.problems[i].message), processorSupport);
            Instance.setValueForProperty(pProblem, "report", repository.newStringCoreInstance(response.problems[i].report), processorSupport);
            Instance.setValueForProperty(pProblem, "errorCode", repository.newStringCoreInstance(response.problems[i].errorCode), processorSupport);
            Instance.setValueForProperty(pProblem, "isError", repository.newBooleanCoreInstance(response.problems[i].isError), processorSupport);
            Instance.setValueForProperty(pProblem, "isException", repository.newBooleanCoreInstance(response.problems[i].isException), processorSupport);
            pProblems.add(pProblem);
        }

        Instance.setValuesForProperty(pResult, "problems", pProblems, processorSupport);

        for (int k = 0; k < response.output.length; k++) {
            Relation output = response.output[k];
            CoreInstance pOutput = this.instance(PURE_OUTPUT, functionExpressionToUseInStack, processorSupport);

            CoreInstance pRelKey = this.instance(PURE_REL_KEY, functionExpressionToUseInStack, processorSupport);
            Instance.setValuesForProperty(pRelKey, "keys", stringInstances(output.relKey.keys), processorSupport);
            Instance.setValuesForProperty(pRelKey, "values", stringInstances(output.relKey.values), processorSupport);
            Instance.setValueForProperty(pRelKey, "name", repository.newStringCoreInstance(output.relKey.name), processorSupport);

            Instance.setValueForProperty(pOutput, "relKey", pRelKey, processorSupport);

            int offset = output.relKey.keys.length + output.relKey.values.length - output.columns.length;

            Object[][] columns = output.columns;
            MutableList<CoreInstance> pColumns = FastList.newList();
            for (int i = 0; i < columns.length && offset >= 0; i++) {
                Object[] values = columns[i];
                MutableList<CoreInstance> pValues = FastList.newList();

                CoreInstance pColumn = this.instance(PURE_COLUMN, functionExpressionToUseInStack, processorSupport);

                int typeIndex = offset + i;
                String type;
                if (typeIndex < output.relKey.keys.length)
                    type = output.relKey.keys[typeIndex];
                else
                    type = output.relKey.values[typeIndex - output.relKey.keys.length];

                for (Object value : values) {
                    switch (type) {
                        case "Int64":
                            /*values[j] instanceof Integer*/
                            // TODO(gbrgr): this is only a hotfix to support integers (https://github.com/RelationalAI/rai-sdk-java/issues/4)
                            pValues.add(repository.newIntegerCoreInstance(((Double) value).intValue()));
                            break;
                        case "String":
                        case "RelationalAITypes.HashValue":
                            /*values[j] instanceof String*/
                            pValues.add(repository.newStringCoreInstance((String) value));
                            break;
                        case "Float64":
                            /*values[j] instanceof Double*/
                            pValues.add(repository.newFloatCoreInstance(BigDecimal.valueOf((Double) value)));
                            break;
                        case "Dates.DateTime":
                            DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");
                            DateTime time = format.parseDateTime((String)value);

                            pValues.add(
                                    repository.newDateCoreInstance(
                                            DateFunctions.newPureDate(
                                                    time.getYear(),
                                                    time.getMonthOfYear(),
                                                    time.getDayOfMonth(),
                                                    time.getHourOfDay(),
                                                    time.getMinuteOfHour(),
                                                    time.getSecondOfMinute()
                                            )
                                    )
                            );
                            break;
                        case "Dates.Date":
                            format = DateTimeFormat.forPattern("yyyy-MM-dd");
                            time = format.parseDateTime((String)value);
                            pValues.add(
                                    repository.newDateCoreInstance(
                                            DateFunctions.newPureDate(
                                                    time.getYear(),
                                                    time.getMonthOfYear(),
                                                    time.getDayOfMonth()
                                            )
                                    )
                            );
                            break;
                        default:
                            throw new PureExecutionException(String.format("Encountered value of unknown type `%s` while retrieving results.", type));
                    }
                }

                Instance.setValuesForProperty(pColumn, "values", pValues, processorSupport);
                pColumns.add(pColumn);
            }

            Instance.setValuesForProperty(pOutput, "columns", pColumns, processorSupport);
            Instance.addValueToProperty(pResult, "output", pOutput, processorSupport);
        }

        return pResult;
    }
}
