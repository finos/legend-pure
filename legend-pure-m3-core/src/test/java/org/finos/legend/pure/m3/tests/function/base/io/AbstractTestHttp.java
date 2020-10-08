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

package org.finos.legend.pure.m3.tests.function.base.io;

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.junit.Ignore;
import org.junit.Test;

public class AbstractTestHttp extends AbstractPureTestWithCoreCompiled
{
    // Remove Ignore when we have an available HTTP server for test...
    @Test @Ignore
    public void testHttpGet()
    {
        compileTestSource("function testHttp():Any[*]\n" +
                "{\n" +
                "    assert(200 == meta::pure::functions::io::http::executeHTTPRaw(^meta::pure::functions::io::http::URL(host='127.0.0.1', port=9090, path='/api/server/v1/currentUser'), meta::pure::functions::io::http::HTTPMethod.GET, [], []).statusCode, |'error');\n" +
                "}\n");
        this.execute("testHttp():Any[*]");
    }
}
