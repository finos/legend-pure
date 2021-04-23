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

package org.finos.legend.pure.ide.light.session;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.ide.light.SourceLocationConfiguration;
import org.finos.legend.pure.ide.light.api.execution.test.CallBack;
import org.finos.legend.pure.ide.light.helpers.response.IDEResponse;
import org.finos.legend.pure.m3.SourceMutation;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.execution.test.TestRunner;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.fs.MutableFSCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.*;
import org.finos.legend.pure.m3.statelistener.VoidExecutionActivityListener;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.finos.legend.pure.ide.light.helpers.response.ExceptionTranslation.buildExceptionMessage;

public class PureSession
{
    private PureRuntime pureRuntime;
    public MutableCodeStorage codeStorage;
    private FunctionExecution functionExecution;
    private SourceLocationConfiguration sourceLocationConfiguration;
    private final ConcurrentMutableMap<Integer, TestRunnerWrapper> testRunnersById = ConcurrentHashMap.newMap();
    private final AtomicInteger executionCount = new AtomicInteger(0);

    public Message message = new Message("");

    public PureSession(SourceLocationConfiguration sourceLocationConfiguration)
    {
        this.sourceLocationConfiguration = sourceLocationConfiguration;
        this.initialize();
    }

    private static final Pattern LINE_SPLITTER = Pattern.compile("^", Pattern.MULTILINE);

    private FunctionExecution initialize()
    {
        String rootPath = Optional.ofNullable(sourceLocationConfiguration)
                .flatMap(s -> Optional.ofNullable(s.welcomeFileDirectory))
                .orElse(System.getProperty("java.io.tmpdir"));

        String ideFilesLocation = Optional.ofNullable(sourceLocationConfiguration)
                .flatMap(s -> Optional.ofNullable(s.ideFilesLocation))
                .orElse("legend-pure-ide-light/src/main/resources/pure_ide");

        this.functionExecution = new FunctionExecutionInterpreted(VoidExecutionActivityListener.VOID_EXECUTION_ACTIVITY_LISTENER);

        try
        {
            MutableList<RepositoryCodeStorage> repos = Lists.mutable
                    .<RepositoryCodeStorage>with(new ClassLoaderCodeStorage(CodeRepository.newPlatformCodeRepository()))
                    .with(this.buildCore(""))
                    .with(this.buildCore("relational"))
                    .with(new MutableFSCodeStorage(new PureIDECodeRepository(), Paths.get(ideFilesLocation)));

            this.codeStorage = new PureCodeStorage(Paths.get(rootPath), repos.toArray(new RepositoryCodeStorage[0]));
            this.pureRuntime = new PureRuntimeBuilder(this.codeStorage).withMessage(this.message).setUseFastCompiler(true).build();
            this.functionExecution.init(this.pureRuntime, this.message);
            this.codeStorage.initialize(this.message);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        return this.functionExecution;
    }

    public MutableFSCodeStorage buildCore(String suffix) throws IOException
    {
        String resources = "legend-pure-code-compiled-core" + (suffix.equals("") ? "" : "-" + suffix) + "/src/main/resources";
        String module = "core" + (suffix.equals("") ? "" : "_" + suffix);
        return new MutableFSCodeStorage(
                GenericCodeRepository.build(Files.newInputStream(Paths.get(resources + "/" + module + ".definition.json"))),
                Paths.get(resources + "/" + module)
        );
    }

    public MutableCodeStorage getCodeStorage()
    {
        return this.codeStorage;
    }

    public PureRuntime getPureRuntime()
    {
        return this.pureRuntime;
    }

    public FunctionExecution getFunctionExecution()
    {
        return this.functionExecution;
    }

    public TestRunner newTestRunner(int testRunId, TestCollection collection)
    {
        TestRunnerWrapper testRunnerWrapper = new TestRunnerWrapper(collection, this.getPureRuntime().executedTestTracker);
        this.testRunnersById.put(testRunId, testRunnerWrapper);
        return testRunnerWrapper.testRunner;
    }

    public CallBack getTestCallBack(int testRunId)
    {
        TestRunnerWrapper testRunnerWrapper = this.testRunnersById.get(testRunId);
        return (null == testRunnerWrapper) ? null : testRunnerWrapper.callBack;
    }

    public int getTestRunCount()
    {
        return this.testRunnersById.size();
    }

    public TestRunner removeTestRunner(int testRunId)
    {
        TestRunnerWrapper testRunnerWrapper = this.testRunnersById.remove(testRunId);
        return (null == testRunnerWrapper) ? null : testRunnerWrapper.testRunner;
    }

    public void saveFilesAndExecute(HttpServletRequest request, HttpServletResponse response, OutputStream outputStream, SimpleFunction func) throws IOException
    {
        try
        {
            executionCount.incrementAndGet();
            JSONObject mainObject = this.saveFiles(request, response);
            SourceMutation sourceMutation = this.getPureRuntime().compile();
            JSONArray array = null != mainObject.get("modifiedFiles") ? (JSONArray) mainObject.get("modifiedFiles") : new JSONArray();
            Iterate.addAllIterable(sourceMutation.getModifiedFiles(), array);
            mainObject.put("modifiedFiles", array);

            if (null != mainObject)
            {
                func.run(this, (JSONObject) mainObject.get("extraParams"), (JSONArray) mainObject.get("modifiedFiles"), response, outputStream);
            }
        }
        catch (Throwable t)
        {
            //todo: refactor this to not need the ByteArrayOutputStream
            ByteArrayOutputStream pureResponse = new ByteArrayOutputStream();
            outputStream.write(exceptionToJson(this, t, pureResponse).getBytes());
            if (t instanceof Error)
            {
                throw (Error) t;
            }
        }
        finally
        {
            executionCount.decrementAndGet();
        }
    }

    public void saveOnly(HttpServletRequest request, HttpServletResponse response, OutputStream outputStream, SimpleFunction func) throws IOException
    {
        JSONObject mainObj = null;
        ByteArrayOutputStream pureResponse = new ByteArrayOutputStream();
        try
        {
            executionCount.incrementAndGet();
            try
            {
                mainObj = this.saveFiles(request, response);
                if (null != mainObj)
                {
                    //file has been saved
                    JSONArray array = (null != mainObj.get("modifiedFiles")) ? (JSONArray) mainObj.get("modifiedFiles") : new JSONArray();
                    mainObj.put("modifiedFiles", array);

                    JSONObject extraParams = (JSONObject) mainObj.get("extraParams");
                    extraParams.put("saveOutcome", "saved");
                    func.run(this, extraParams, (JSONArray) mainObj.get("modifiedFiles"), response, outputStream);
                }
                else
                {
                    //Encountered Error trying to save
                    JSONObject extraParams = new JSONObject();
                    extraParams.put("saveOutcome", "Error");
                    func.run(this, extraParams, new JSONArray(), response, outputStream);
                }
            }
            catch (Exception e)
            {
                outputStream.write(exceptionToJson(this, e, pureResponse).getBytes());
            }

        }
        catch (Exception e)
        {
            outputStream.write(exceptionToJson(this, e, pureResponse).getBytes());
        }
        finally
        {
            executionCount.decrementAndGet();
        }
    }

    public JSONObject saveFiles(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        JSONObject mainObject = null;
        try
        {
            mainObject = (JSONObject) new JSONParser().parse(new InputStreamReader(request.getInputStream()));
            JSONArray openFiles = (JSONArray) mainObject.get("openFiles");
            if ((null != openFiles) && !openFiles.isEmpty())
            {
                PureRuntime pureRuntime = this.getPureRuntime();

                MutableList<JSONObject> diagrams = Lists.mutable.empty();
                for (Object rawOpenFile : openFiles)
                {
                    JSONObject openFile = (JSONObject) rawOpenFile;
                    if (null == openFile.get("diagram"))
                    {
                        String path = (String) openFile.get("path");
                        String code = (String) openFile.get("code");
                        code = code.replace("\n", "\r\n");
                        if (null == pureRuntime.getSourceById(path))
                        {
                            pureRuntime.loadSourceIfLoadable(path);
                        }
                        pureRuntime.modify(path, code);
                    }
                    else
                    {
                        diagrams.add(openFile);
                    }
                }

                if (diagrams.notEmpty())
                {
                    JSONArray modifiedFiles = new JSONArray();

                    for (JSONObject d : diagrams)
                    {
                        SourceMutation mutation = pureRuntime.compile();
                        Iterate.addAllIterable(mutation.getModifiedFiles(), modifiedFiles);
                        CoreInstance diagram = pureRuntime.getProcessorSupport().package_getByUserPath(d.get("diagram").toString());
                        if (null != diagram)
                        {
                            SourceInformation sourceInformation = diagram.getSourceInformation();
                            Source source = pureRuntime.getSourceById(sourceInformation.getSourceId());
                            String content = source.getContent();
                            String lines[] = LINE_SPLITTER.split(content);
                            StringBuilder buffer = new StringBuilder(content.length());
                            for (int i = 0; i < sourceInformation.getStartLine() - 1; i++)
                            {
                                buffer.append(lines[i]);
                            }
                            buffer.append(lines[sourceInformation.getStartLine() - 1].substring(0, diagram.getSourceInformation().getStartColumn() - 1));
                            buffer.append(d.get("code"));
                            buffer.append(lines[sourceInformation.getEndLine() - 1].substring(sourceInformation.getEndColumn()));
                            for (int i = sourceInformation.getEndLine(); i < lines.length; i++)
                            {
                                buffer.append(lines[i]);
                            }
                            pureRuntime.modify(sourceInformation.getSourceId(), buffer.toString());
                            modifiedFiles.add(diagram.getSourceInformation().getSourceId());
                        }
                    }
                    mainObject.put("modifiedFiles", modifiedFiles);
                }
            }
        }
        catch (Exception e)
        {
            try (OutputStream outputStream = response.getOutputStream())
            {
                outputStream.write(exceptionToJson(this, e, null).getBytes());
            }
        }
        return mainObject;
    }

    public int getCurrentExecutionCount()
    {
        return executionCount.get();
    }

    public static String exceptionToJson(PureSession session, Throwable t, ByteArrayOutputStream pureResponse)
    {
        IDEResponse response = buildExceptionMessage(session, t, pureResponse);
        return response.toJSONString();
    }

    private class TestRunnerWrapper
    {
        private TestRunner testRunner;
        private final CallBack callBack;

        private TestRunnerWrapper(TestCollection collection, CallBack callBack, final ExecutedTestTracker executedTestTracker)
        {
            this.testRunner = new TestRunner(collection, false, PureSession.this.getFunctionExecution(), callBack)
            {
                @Override
                public void run()
                {
                    super.run();
                    if (null != executedTestTracker)
                    {
                        executedTestTracker.notePassingTests(this.passedTests);
                        executedTestTracker.noteFailingTests(this.failedTests);
                    }
                }
            };
            this.callBack = callBack;
        }

        private TestRunnerWrapper(TestCollection collection, ExecutedTestTracker executedTestTracker)
        {
            this(collection, new CallBack(), executedTestTracker);
        }

        void stopAndClear()
        {
            TestRunner tr = this.testRunner;
            if (null != tr)
            {
                tr.stop();
                this.testRunner = null;
            }
            this.callBack.clear();
        }
    }


}
