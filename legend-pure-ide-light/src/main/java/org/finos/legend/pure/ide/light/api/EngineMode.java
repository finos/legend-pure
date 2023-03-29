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
import java.util.Optional;

@Api(tags = "Engine Execution Mode Options")
@Path("/engineMode")
public class EngineMode
{
    private final PureSession pureSession;

    public EngineMode(PureSession session)
    {
        this.pureSession = session;
    }

    @GET
    @Path("getAllProperties")
    public Response getAllEngineProperties(@Context HttpServletRequest request, @Context HttpServletResponse response)
    {
        return Response.ok((StreamingOutput) outputStream ->
        {
            ObjectMapper om = new ObjectMapper();
            outputStream.write(om.writeValueAsBytes(this.pureSession.getEngineProperties()));
            outputStream.close();
        }).build();
    }

    @GET
    @Path("enable")
    public void enableEngineMode()
    {
        this.pureSession.enableEngineMode();
    }

    @GET
    @Path("disable")
    public void disableEngineMode()
    {
        this.pureSession.disableEngineMode();
    }

    @GET
    @Path("isEnabled")
    public boolean isEngineModeEnabled()
    {
        return this.pureSession.isEngineModeEnabled();
    }

    @GET
    @Path("setTestServerHost/{host}")
    public void setEngineTestServerHost(@PathParam("host") String host)
    {
        this.pureSession.setLegendTestServerHost(host);
    }

    @GET
    @Path("setTestServerPort/{port}")
    public void setEngineTestServerPort(@PathParam("port") String port)
    {
        this.pureSession.setLegendTestServerPort(port);
    }

    @GET
    @Path("setTestClientVersion/{clientVersion}")
    public void setEngineTestClientVersion(@PathParam("clientVersion") String clientVersion)
    {
        this.pureSession.setLegendTestClientVersion(clientVersion);
    }

    @GET
    @Path("setTestServerVersion/{serverVersion}")
    public void setEngineTestServerVersion(@PathParam("serverVersion") String serverVersion)
    {
        this.pureSession.setLegendTestServerVersion(serverVersion);
    }

    @GET
    @Path("setTestSerializationKind/{kind}")
    public void setEngineTestSerializationKind(@PathParam("kind") String kind)
    {
        this.pureSession.setLegendTestSerializationKind(kind);
    }

    @GET
    @Path("getTestServerHost")
    public String getEngineTestServerHost()
    {
        return this.pureSession.getLegendTestServerHost();
    }

    @GET
    @Path("getTestServerPort")
    public String getEngineTestServerPort()
    {
        return this.pureSession.getLegendTestServerPort();
    }

    @GET
    @Path("getTestClientVersion")
    public String getEngineTestClientVersion()
    {
        return this.pureSession.getLegendTestClientVersion();
    }

    @GET
    @Path("getTestServerVersion")
    public String getEngineTestServerVersion()
    {
        return this.pureSession.getLegendTestServerVersion();
    }

    @GET
    @Path("getTestSerializationKind")
    public String getEngineTestSerializationKind()
    {
        return this.pureSession.getLegendTestSerializationKind();
    }
}
