package org.finos.legend.pure.ide.light.api;

import io.swagger.annotations.Api;
import org.finos.legend.pure.ide.light.session.PureSession;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.Optional;


@Api(tags = "Enable/disable external H2 for running tests")
@Path("/h2")
public class EmbeddedH2
{
    private final PureSession pureSession;

    public EmbeddedH2(PureSession pureSession)
    {
        this.pureSession = pureSession;
    }

    @GET
    @Path("isExternalH2Enabled")
    public boolean isExternalH2Enabled()
    {
        return !this.pureSession.isEmbeddedH2Enabled();
    }

    @GET
    @Path("enableExtenalH2")
    public void enableExternalH2()
    {
        this.pureSession.disableEmbeddedH2(Optional.empty());
    }
    @GET
    @Path("enableExternalH2/{port}")
    public void enableEmbeddedH2WithPort(@PathParam("port") String port)
    {
        this.pureSession.disableEmbeddedH2(Optional.of(port));
    }

    @GET
    @Path("disableExternalH2")
    public void disableExternalH2()
    {
        this.pureSession.enableEmbeddedH2();
    }
}
