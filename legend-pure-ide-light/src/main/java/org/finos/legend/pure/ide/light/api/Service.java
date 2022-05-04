package org.finos.legend.pure.ide.light.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.pure.ide.light.api.execution.function.manager.ContentType;
import org.finos.legend.pure.ide.light.api.execution.function.manager.ExecutionManager;
import org.finos.legend.pure.ide.light.api.execution.function.manager.ExecutionRequest;
import org.finos.legend.pure.ide.light.api.execution.function.manager.HttpServletResponseWriter;
import org.finos.legend.pure.ide.light.session.PureSession;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.util.Map;

@Api(tags = "Service")
@Path("")
public class Service
{
    private PureSession pureSession;

    public Service(PureSession pureSession)
    {
        this.pureSession = pureSession;
    }

    @GET
    @Path("{path:.+}")
    @ApiOperation(value = "")
    public void exec(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("path") String path) throws IOException
    {
        Pair<CoreInstance, Map<String, String[]>> result =  this.pureSession.getPureRuntime().getURLPatternLibrary().tryExecution("/"+path, this.pureSession.getPureRuntime().getProcessorSupport(), request.getParameterMap());
        if (result == null)
        {
            response.sendError(404, "The service '"+path+"' can't be found!");
            return;
        }

        Map<String, String[]> requestParams = result.getTwo();
        requestParams.putAll(request.getParameterMap());
        requestParams.put(ExecutionManager.OUTPUT_FORMAT_PARAMETER, new String[]{ExecutionManager.OUTPUT_FORMAT_RAW});

        ExecutionManager executionManager = new ExecutionManager(pureSession.getFunctionExecution());
        executionManager.execute(new ExecutionRequest(result.getTwo()), new HttpServletResponseWriter(response), ContentType.text);
    }

}
