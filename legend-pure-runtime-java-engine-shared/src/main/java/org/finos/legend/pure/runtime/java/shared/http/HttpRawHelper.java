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

package org.finos.legend.pure.runtime.java.shared.http;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class HttpRawHelper
{

    public static SimpleHttpResponse executeHttpService(String host, int port, String path, HttpMethod httpMethod, String mimeType, String body)
    {
        final URIBuilder uriBuilder = new URIBuilder()
                .setScheme("http")
                .setHost(host)
                .setPort(port)
                .setPath(path);
        URI uri = null;
        try
        {
            uri = uriBuilder.build();
        }
        catch (URISyntaxException e)
        {
            String errMsg = String.format("Cannot build URI from url (%s, %d, %s)", host, port, path);
            throw new RuntimeException(errMsg, e);
        }
        return executeRequest(httpMethod, uri, mimeType, body);
    }

    private static SimpleHttpResponse executeRequest(HttpMethod httpMethod, URI uri, String mimeType, String body)
    {
        HttpUriRequest request;
        switch (httpMethod)
        {
            case GET:
            {
                request = new HttpGet(uri);
                break;
            }
            case PUT:
            {
                HttpEntityEnclosingRequestBase putRequest = new HttpPut(uri);
                setRequestBody(putRequest, mimeType, body);
                request = putRequest;
                break;
            }
            case POST:
            {
                HttpEntityEnclosingRequestBase postRequest = new HttpPost(uri);
                setRequestBody(postRequest, mimeType, body);
                request = postRequest;
                break;
            }
            case DELETE:
            {
                request = new HttpDelete(uri);
                break;
            }
            default:
            {
                throw new UnsupportedOperationException("The HTTP method " + httpMethod + " is not supported");
            }
        }

        CloseableHttpClient httpClient;

        BasicCookieStore cookieStore = new BasicCookieStore();
        HttpClientBuilder clientBuilder = HttpClients.custom().setDefaultCookieStore(cookieStore);

        httpClient = clientBuilder.build();

        try (CloseableHttpResponse httpResponse = httpClient.execute(request))
        {
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            HttpEntity entity = httpResponse.getEntity();
            String responseContent = (entity == null) ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);
            return new SimpleHttpResponse(statusCode, responseContent);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error: service call for URL: '" + request.getURI() + "' failed: " + e.getMessage(), e);
        }
    }

    private static void setRequestBody(HttpEntityEnclosingRequestBase putRequest, String mimeType, String body)
    {
        if (body != null)
        {
            ContentType contentType = mimeType == null ?
                    ContentType.APPLICATION_JSON :
                    ContentType.create(mimeType, "UTF-8");
            putRequest.setEntity(new StringEntity(body, contentType));
        }
    }

    public static CoreInstance toHttpResponseInstance(SimpleHttpResponse response, ProcessorSupport processorSupport)
    {
        CoreInstance statusInstance = processorSupport.newCoreInstance(Integer.toString(response.getStatusCode()), M3Paths.Integer, null);
        CoreInstance entityInstance = processorSupport.newCoreInstance(response.getEntityContent(), M3Paths.String, null);

        CoreInstance coreInstance = processorSupport.newEphemeralAnonymousCoreInstance("meta::pure::functions::io::http::HTTPResponse");
        Instance.setValuesForProperty(coreInstance, "statusCode", Lists.immutable.of(statusInstance), processorSupport);
        Instance.setValuesForProperty(coreInstance, "entity", Lists.immutable.of(entityInstance), processorSupport);

        return coreInstance;
    }
}
