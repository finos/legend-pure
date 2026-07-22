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

package org.finos.legend.pure.lsp.protocol;

import java.util.ArrayList;
import java.util.List;

public final class LegendDebug
{
    private LegendDebug()
    {
    }

    public static class Breakpoint
    {
        private String uri;
        private int line;

        public Breakpoint()
        {
        }

        public Breakpoint(String uri, int line)
        {
            this.uri = uri;
            this.line = line;
        }

        public String getUri()
        {
            return this.uri;
        }

        public void setUri(String uri)
        {
            this.uri = uri;
        }

        public int getLine()
        {
            return this.line;
        }

        public void setLine(int line)
        {
            this.line = line;
        }
    }

    public static class StartParams
    {
        private String function;
        private List<Breakpoint> breakpoints = new ArrayList<>();

        public StartParams()
        {
        }

        public String getFunction()
        {
            return this.function;
        }

        public void setFunction(String function)
        {
            this.function = function;
        }

        public List<Breakpoint> getBreakpoints()
        {
            return this.breakpoints;
        }

        public void setBreakpoints(List<Breakpoint> breakpoints)
        {
            this.breakpoints = breakpoints == null ? new ArrayList<>() : breakpoints;
        }
    }

    public static class EvaluateParams
    {
        private String expression;
        private int frameId;

        public EvaluateParams()
        {
        }

        public String getExpression()
        {
            return this.expression;
        }

        public void setExpression(String expression)
        {
            this.expression = expression;
        }

        public int getFrameId()
        {
            return this.frameId;
        }

        public void setFrameId(int frameId)
        {
            this.frameId = frameId;
        }
    }

    public static class VariablesParams
    {
        private int variablesReference;

        public VariablesParams()
        {
        }

        public int getVariablesReference()
        {
            return this.variablesReference;
        }

        public void setVariablesReference(int variablesReference)
        {
            this.variablesReference = variablesReference;
        }
    }

    public static class Response
    {
        private boolean success;
        private String state;
        private String reason;
        private String message;
        private String output;
        private List<StackFrame> stackFrames = new ArrayList<>();

        public Response()
        {
        }

        public static Response paused(String output, List<StackFrame> stackFrames, String reason)
        {
            Response response = new Response();
            response.success = true;
            response.state = "paused";
            response.reason = reason;
            response.output = output;
            response.stackFrames = stackFrames == null ? new ArrayList<>() : stackFrames;
            return response;
        }

        public static Response completed(String output)
        {
            Response response = new Response();
            response.success = true;
            response.state = "completed";
            response.output = output;
            return response;
        }

        public static Response error(String message)
        {
            Response response = new Response();
            response.success = false;
            response.state = "error";
            response.message = message;
            return response;
        }

        public boolean isSuccess()
        {
            return this.success;
        }

        public void setSuccess(boolean success)
        {
            this.success = success;
        }

        public String getState()
        {
            return this.state;
        }

        public void setState(String state)
        {
            this.state = state;
        }

        public String getReason()
        {
            return this.reason;
        }

        public void setReason(String reason)
        {
            this.reason = reason;
        }

        public String getMessage()
        {
            return this.message;
        }

        public void setMessage(String message)
        {
            this.message = message;
        }

        public String getOutput()
        {
            return this.output;
        }

        public void setOutput(String output)
        {
            this.output = output;
        }

        public List<StackFrame> getStackFrames()
        {
            return this.stackFrames;
        }

        public void setStackFrames(List<StackFrame> stackFrames)
        {
            this.stackFrames = stackFrames == null ? new ArrayList<>() : stackFrames;
        }
    }

    public static class StackFrame
    {
        private int id;
        private String name;
        private String uri;
        private int line;
        private int column;
        private int endLine;
        private int endColumn;
        private int variablesReference;

        public StackFrame()
        {
        }

        public StackFrame(int id, String name, String uri, int line, int column)
        {
            this(id, name, uri, line, column, line, column, id);
        }

        public StackFrame(int id, String name, String uri, int line, int column, int endLine, int endColumn,
                          int variablesReference)
        {
            this.id = id;
            this.name = name;
            this.uri = uri;
            this.line = line;
            this.column = column;
            this.endLine = endLine;
            this.endColumn = endColumn;
            this.variablesReference = variablesReference;
        }

        public int getId()
        {
            return this.id;
        }

        public void setId(int id)
        {
            this.id = id;
        }

        public String getName()
        {
            return this.name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getUri()
        {
            return this.uri;
        }

        public void setUri(String uri)
        {
            this.uri = uri;
        }

        public int getLine()
        {
            return this.line;
        }

        public void setLine(int line)
        {
            this.line = line;
        }

        public int getColumn()
        {
            return this.column;
        }

        public void setColumn(int column)
        {
            this.column = column;
        }

        public int getEndLine()
        {
            return this.endLine;
        }

        public void setEndLine(int endLine)
        {
            this.endLine = endLine;
        }

        public int getEndColumn()
        {
            return this.endColumn;
        }

        public void setEndColumn(int endColumn)
        {
            this.endColumn = endColumn;
        }

        public int getVariablesReference()
        {
            return this.variablesReference;
        }

        public void setVariablesReference(int variablesReference)
        {
            this.variablesReference = variablesReference;
        }
    }

    public static class Variable
    {
        private String name;
        private String value;
        private String type;
        private int variablesReference;

        public Variable()
        {
        }

        public Variable(String name, String value, String type)
        {
            this(name, value, type, 0);
        }

        public Variable(String name, String value, String type, int variablesReference)
        {
            this.name = name;
            this.value = value;
            this.type = type;
            this.variablesReference = variablesReference;
        }

        public String getName()
        {
            return this.name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getValue()
        {
            return this.value;
        }

        public void setValue(String value)
        {
            this.value = value;
        }

        public String getType()
        {
            return this.type;
        }

        public void setType(String type)
        {
            this.type = type;
        }

        public int getVariablesReference()
        {
            return this.variablesReference;
        }

        public void setVariablesReference(int variablesReference)
        {
            this.variablesReference = variablesReference;
        }
    }

    public static class EvaluateResult
    {
        private boolean success;
        private String result;
        private String error;
        private int variablesReference;

        public EvaluateResult()
        {
        }

        public static EvaluateResult success(String result)
        {
            return success(result, 0);
        }

        public static EvaluateResult success(String result, int variablesReference)
        {
            EvaluateResult response = new EvaluateResult();
            response.success = true;
            response.result = result;
            response.variablesReference = variablesReference;
            return response;
        }

        public static EvaluateResult error(String error)
        {
            EvaluateResult response = new EvaluateResult();
            response.success = false;
            response.error = error;
            return response;
        }

        public boolean isSuccess()
        {
            return this.success;
        }

        public void setSuccess(boolean success)
        {
            this.success = success;
        }

        public String getResult()
        {
            return this.result;
        }

        public void setResult(String result)
        {
            this.result = result;
        }

        public String getError()
        {
            return this.error;
        }

        public void setError(String error)
        {
            this.error = error;
        }

        public int getVariablesReference()
        {
            return this.variablesReference;
        }

        public void setVariablesReference(int variablesReference)
        {
            this.variablesReference = variablesReference;
        }
    }
}
