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

package org.finos.legend.pure.runtime.java.extension.store.relational.compiled;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m2.relational.AbstractTestRelationalTimeZoneResolution;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.navigation.enumeration.Enumeration;
import org.finos.legend.pure.m3.serialization.runtime.MutableRuntimeOptions;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.time.TimeZoneResolution;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiled;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.ResultSetRowIterableProvider;
import org.finos.legend.pure.runtime.java.shared.canstreamstate.CanStreamState;
import org.finos.legend.pure.runtime.java.shared.time.TimeZoneResolutionOption;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestRelationalTimeZoneResolutionCompiled extends AbstractTestRelationalTimeZoneResolution
{
    private static final MutableRuntimeOptions OPTIONS = MutableRuntimeOptions.empty();

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), OPTIONS, JavaModelFactoryRegistryLoader.loader());
    }

    /**
     * A zone that does not resolve fails the query before anything reads the result set, so the
     * connection has to be released then, whether the result set would have been streamed or not.
     */
    @Test
    public void testAnUnknownZoneReleasesTheConnection() throws SQLException
    {
        CoreInstance pureConnection = repository.newEphemeralAnonymousCoreInstance(null, processorSupport.package_getByUserPath("meta::external::store::relational::runtime::TestDatabaseConnection"));
        pureConnection.setKeyValues(Lists.immutable.with("type"), Lists.immutable.with(Enumeration.findEnum(processorSupport.package_getByUserPath("meta::relational::runtime::DatabaseType"), "H2")));
        CompiledExecutionSupport executionSupport = ((FunctionExecutionCompiled) functionExecution).getExecutionSupport();
        for (boolean streams : new boolean[]{false, true})
        {
            CanStreamState.setCanStream(streams);
            try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:", "sa", ""))
            {
                IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> ResultSetRowIterableProvider.createResultSetIterator(pureConnection, connection, "SELECT TIMESTAMP '2014-03-10 12:00:00'", 0, false, 30, 100, row -> null, null, "Not/AZone", null, executionSupport, null));
                Assert.assertEquals("Unknown time zone: Not/AZone", e.getMessage());
                Assert.assertTrue("streams: " + streams, connection.isClosed());
            }
            finally
            {
                CanStreamState.resetCanStream();
            }
        }
    }

    @Override
    protected void setTimeZoneResolution(TimeZoneResolution timeZoneResolution)
    {
        TimeZoneResolutionOption.setTimeZoneResolution(OPTIONS, timeZoneResolution);
    }

    public static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }
}
