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

package org.finos.legend.pure.ide.light;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.federecio.dropwizard.swagger.SwaggerResource;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.finos.legend.pure.ide.light.api.*;
import org.finos.legend.pure.ide.light.api.concept.Concept;
import org.finos.legend.pure.ide.light.api.execution.function.Execute;
import org.finos.legend.pure.ide.light.api.execution.go.ExecuteGo;
import org.finos.legend.pure.ide.light.api.execution.test.ExecuteTests;
import org.finos.legend.pure.ide.light.api.find.FindInSources;
import org.finos.legend.pure.ide.light.api.find.FindPureFile;
import org.finos.legend.pure.ide.light.session.PureSession;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;

public class PureIDEServer extends Application<ServerConfiguration>
{
    private final PureSession pureSession = new PureSession();

    public static void main(String[] args) throws Exception
    {
        new PureIDEServer().run(args);
    }

    @Override
    public void initialize(Bootstrap<ServerConfiguration> bootstrap)
    {
        bootstrap.addBundle(new SwaggerBundle<ServerConfiguration>()
        {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(
                    ServerConfiguration configuration)
            {
                return configuration.swagger;
            }
        });
        bootstrap.addBundle(new AssetsBundle("/ide", "/ide", "index.html", "static"));
    }

    @Override
    public void run(ServerConfiguration configuration, Environment environment) throws Exception
    {
        environment.jersey().setUrlPattern("/*");
        environment
                .jersey()
                .register(
                        new SwaggerResource(
                                "",
                                configuration.swagger.getSwaggerViewConfiguration(),
                                configuration.swagger.getSwaggerOAuth2Configuration(),
                                configuration.swagger.getContextRoot() +
                                        (configuration.swagger.getContextRoot().endsWith("/") ? "" : "/")
                                        + "api"));


        environment.jersey().register(new Concept(pureSession));

        environment.jersey().register(new Execute(pureSession));
        environment.jersey().register(new ExecuteGo(pureSession));
        environment.jersey().register(new ExecuteTests(pureSession));

        environment.jersey().register(new FindInSources(pureSession));
        environment.jersey().register(new FindPureFile(pureSession));

        environment.jersey().register(new Activities(pureSession));
        environment.jersey().register(new FileManagement(pureSession));
        environment.jersey().register(new LifeCycle(pureSession));

        enableCors(environment);
    }

    private void enableCors(Environment environment)
    {
        FilterRegistration.Dynamic corsFilter = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,PUT,POST,DELETE,OPTIONS");
        corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_TIMING_ORIGINS_PARAM, "*");
        corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin,Access-Control-Allow-Credentials,x-b3-parentspanid,x-b3-sampled,x-b3-spanid,x-b3-traceid");
        corsFilter.setInitParameter(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM, "false");
        corsFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "*");
    }
}
