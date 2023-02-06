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

package org.finos.legend.pure.m3.tests.tools;

import org.finos.legend.pure.m3.tools.IdUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

public class IdUtilsTests
{

    @Test
    public void shouldBeAbleToConstructAndDeconstructId()
    {
        Long systemCurrentTime = System.currentTimeMillis();
        byte[] random = new byte[8];
        new SecureRandom().nextBytes(random);
        IdUtils.IdComponents components = new IdUtils.IdComponents(systemCurrentTime, IdUtils.getHostAddressBytes(), random);
        String id = IdUtils.constructId(components);
        try
        {
            IdUtils.IdComponents deconstructedComponents = IdUtils.deconstructId(id);
            Assert.assertEquals(components, deconstructedComponents);
        }
        catch (Exception exp)
        {
            Assert.fail(exp.getMessage());
        }

    }
}
