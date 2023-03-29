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

package org.finos.legend.pure.ide.light.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import org.finos.legend.pure.ide.light.session.PureSession;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

@Api(tags = "Runtime Options")
@Path("/runtimeOptions")
public class RuntimeOptions {
    private PureSession pureSession;

    public RuntimeOptions(PureSession pureSession) {
        this.pureSession = pureSession;
    }

    @GET
    @Path("set/{name}/{value}")
    public void setRuntimeOption(@PathParam("name") String name, @PathParam("value") Boolean value) {
        this.pureSession.setRuntimeOption(name, value);
    }

    @GET
    @Path("get/{name}")
    public boolean getRuntimeOption(@PathParam("name") String name) {
        return this.pureSession.getRuntimeOption(name).orElse(false);
    }

    @GET
    @Path("getAll")
    public Response getAllPureOptions(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        return Response.ok((StreamingOutput) outputStream ->
        {
            ObjectMapper om = new ObjectMapper();
            outputStream.write(om.writeValueAsBytes(this.pureSession.getAllRuntimeOptions()));
            outputStream.close();
        }).build();
    }


}
