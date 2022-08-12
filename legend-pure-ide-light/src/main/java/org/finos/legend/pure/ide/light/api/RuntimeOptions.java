package org.finos.legend.pure.ide.light.api;

import io.swagger.annotations.Api;
import org.finos.legend.pure.ide.light.session.PureSession;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Api(
        tags = {"RuntimeOptions"}
)
@Path("/")
public class RuntimeOptions {
    private PureSession pureSession;

    public RuntimeOptions(PureSession pureSession) {
        this.pureSession = pureSession;
    }

    @GET
    @Path("setOption/{name}/{value}")
    public void setRuntimeOption(@PathParam("name") String name, @PathParam("value") Boolean value) {
        this.pureSession.setRuntimeOption(name, value);
    }

    @GET
    @Path("getOption/{name}")
    public boolean getRuntimeOption(@PathParam("name") String name)
    {
        return this.pureSession.getRuntimeOption(name).orElse(false);
    }

}
