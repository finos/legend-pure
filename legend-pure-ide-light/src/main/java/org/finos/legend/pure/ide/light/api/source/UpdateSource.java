// Copyright 2022 Goldman Sachs
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

package org.finos.legend.pure.ide.light.api.source;

import io.swagger.annotations.Api;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.ide.light.session.PureSession;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

@Api(tags = "Source")
@Path("/")
public class UpdateSource
{
    private static final Pattern LINE_SPLITTER = Pattern.compile("^", Pattern.MULTILINE);

    private PureSession session;

    public UpdateSource(PureSession session)
    {
        this.session = session;
    }

    @POST
    @Path("updateSource")
    public Response updateSource(@Context HttpServletRequest request, List<UpdateSourceInput> updateInputs, @Context HttpServletResponse response) throws IOException
    {
        return Response.ok((StreamingOutput) outStream ->
        {
            response.setContentType("application/json");
            Multimap<String, UpdateSourceInput> indexByPath = ListIterate.groupBy(updateInputs, UpdateSourceInput.PATH);

            for (String path : indexByPath.keysView().toSortedList())
            {
                RichIterable<UpdateSourceInput> inputs = indexByPath.get(path);
                Source source = this.session.getPureRuntime().getSourceRegistry().getSource(path);
                MutableMap<Integer, UpdateSourceInput> messageToAddByLines = Maps.mutable.empty();
                MutableMap<Integer, UpdateSourceInput> messageToRemoveByLines = Maps.mutable.empty();
                MutableSet<Integer> linesToAdd = Sets.mutable.empty();
                MutableSet<Integer> linesToRemove = Sets.mutable.empty();

                // index source updates by line number
                for (UpdateSourceInput input : inputs)
                {
                    if (input.isAdd())
                    {
                        // add
                        if (linesToAdd.add(input.getLine())) // check for duplication
                        {
                            messageToAddByLines.put(input.getLine(), input);
                        }
                        else
                        {
                            throw new IllegalArgumentException("Invalid file update request - Please combine the same line change");
                        }
                    }
                    else
                    {
                        // remove
                        if (linesToRemove.add(input.getLine()))  // check for duplication
                        {
                            messageToRemoveByLines.put(input.getLine(), input);
                        }
                        {
                            throw new IllegalArgumentException("Invalid file update request - Please combine the same line change");
                        }
                    }
                }

                String file = source.getContent();
                String[] lines = LINE_SPLITTER.split(file);
                StringBuilder buffer = new StringBuilder();

                for (int i = 0; i < lines.length; i++)
                {
                    int lineNumber = i + 1;
                    // perform add
                    if (linesToAdd.contains(lineNumber))
                    {
                        linesToAdd.remove(lineNumber);
                        buffer.append(messageToAddByLines.get(lineNumber).getMessage()).append("\r\n");
                    }

                    // perform remove
                    if (linesToRemove.contains(lineNumber))
                    {
                        linesToRemove.remove(lineNumber);
                    }

                    // append original line
                    else
                    {
                        buffer.append(lines[i]);
                    }
                }

                while (linesToAdd.notEmpty())
                {
                    int line = linesToAdd.min();
                    linesToAdd.remove(line);
                    if (line < 1)
                    {
                        throw new IllegalArgumentException("Invalid file update request - Line number must be greater than 0");
                    }
                    else if (line >= lines.length)
                    {
                        buffer.append("\r\n");
                        buffer.append(messageToAddByLines.get(line).getMessage());
                        buffer.append("\r\n");
                    }
                    else
                    {
                        throw new IllegalArgumentException("Invalid file update request - Line number out of range");
                    }
                }

                if (linesToRemove.notEmpty())
                {
                    throw new IllegalArgumentException("Invalid file update request - Line number out of range");
                }

                session.getPureRuntime().modify(path, buffer.toString());
            }

            outStream.write("{".getBytes());
            outStream.write(("\"text\":\"").getBytes());
            outStream.write(JSONValue.escape("Successfully updated source(s)!").getBytes());
            outStream.write(JSONValue.escape("\r\nPlease press F9 to compile the code again").getBytes());
            outStream.write("\",".getBytes());
            outStream.write(("\"modifiedFiles\":[\"").getBytes());
            outStream.write(JSONValue.escape(indexByPath.valuesView().collect(UpdateSourceInput::getPath).makeString(",")).getBytes());
            outStream.write(("\"]").getBytes());
            outStream.write("}".getBytes());
            outStream.close();
        }).build();
    }
}