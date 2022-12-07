package org.finos.legend.pure.ide.light.api.find;

import io.swagger.annotations.Api;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.multimap.Multimap;
import org.finos.legend.pure.ide.light.session.PureSession;
import org.finos.legend.pure.m3.serialization.runtime.SourceCoordinates;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Api(tags = "Find")
@Path("/")
public class FindTextPreview
{
    private PureSession session;

    public FindTextPreview(PureSession session)
    {
        this.session = session;
    }

    @GET
    @Path("getTextPreview")
    public Response getTextPreview(@Context HttpServletRequest request, List<SourceCoordinates> coordinates, @Context HttpServletResponse response) throws IOException
    {
        return Response.ok((StreamingOutput) outputStream ->
        {
            try
            {
                response.setContentType("application/json");
                writeResultsJSON(outputStream, session.getPureRuntime().getSourceRegistry().getPreviewTextWithCoordinates(coordinates));
            }
            catch (IOException | RuntimeException | Error e)
            {
                throw e;
            }
        }).build();
    }

    private int writeResultsJSON(OutputStream stream, RichIterable<SourceCoordinates> results) throws IOException
    {
        stream.write("[".getBytes());
        int count = 0;
        if (results.notEmpty())
        {
            Multimap<String, SourceCoordinates> indexBySource = results.groupBy(SourceCoordinates.SOURCE_ID);
            boolean first = true;
            for (String sourceId : indexBySource.keysView().toSortedList())
            {
                if (first)
                {
                    first = false;
                    stream.write("{\"sourceId\":\"".getBytes());
                }
                else
                {
                    stream.write(",{\"sourceId\":\"".getBytes());
                }
                stream.write(JSONValue.escape(sourceId).getBytes());
                stream.write("\",\"coordinates\":[".getBytes());
                boolean firstSC = true;
                for (SourceCoordinates sourceCoordinates : indexBySource.get(sourceId))
                {
                    if (firstSC)
                    {
                        firstSC = false;
                    }
                    else
                    {
                        stream.write(",".getBytes());
                    }
                    writeSourceCoordinatesJSON(stream, sourceCoordinates);
                    count++;
                }
                stream.write("]}".getBytes());
            }
        }
        stream.write("]".getBytes());
        return count;
    }

    private void writeSourceCoordinatesJSON(OutputStream stream, SourceCoordinates sourceCoordinates) throws IOException
    {
        stream.write("{\"startLine\":".getBytes());
        stream.write(Integer.toString(sourceCoordinates.getStartLine()).getBytes());
        stream.write(",\"startColumn\":".getBytes());
        stream.write(Integer.toString(sourceCoordinates.getStartColumn()).getBytes());
        stream.write(",\"endLine\":".getBytes());
        stream.write(Integer.toString(sourceCoordinates.getEndLine()).getBytes());
        stream.write(",\"endColumn\":".getBytes());
        stream.write(Integer.toString(sourceCoordinates.getEndColumn()).getBytes());
        if (sourceCoordinates.getPreview() != null)
        {
            stream.write(",\"preview\":".getBytes());
            writePreviewJSON(stream, sourceCoordinates.getPreview());
        }
        stream.write("}".getBytes());
    }

    private void writePreviewJSON(OutputStream stream, SourceCoordinates.Preview preview) throws IOException
    {
        stream.write("{\"before\":\"".getBytes());
        stream.write(JSONValue.escape(preview.getBeforeText()).getBytes());
        stream.write("\",\"found\":\"".getBytes());
        stream.write(JSONValue.escape(preview.getFoundText()).getBytes());
        stream.write("\",\"after\":\"".getBytes());
        stream.write(JSONValue.escape(preview.getAfterText()).getBytes());
        stream.write("\"}".getBytes());
    }
}
