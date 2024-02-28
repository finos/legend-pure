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

package org.finos.legend.pure.m3.tests.function.base.math;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValueInstance;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public abstract class AbstractTestPow extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testPow() throws Exception
    {
        try
        {
            InstanceValueInstance base = InstanceValueInstance.createPersistent(this.repository, null, null);
            base._values(Lists.fixedSize.of(this.repository.newFloatCoreInstance(new BigDecimal(333333.3333))));
            InstanceValueInstance exponent = InstanceValueInstance.createPersistent(this.repository, null, null);
            exponent._values(Lists.fixedSize.of(this.repository.newFloatCoreInstance(new BigDecimal(999999))));
            compileAndExecute("meta::pure::functions::math::pow(Number[1], Number[1]):Number[1]", base, exponent);
            Assert.fail();
        }
        catch (NumberFormatException e)
        {
            e.printStackTrace();
            Assert.assertEquals("Infinite or NaN", e.getMessage());
        }
    }
}
