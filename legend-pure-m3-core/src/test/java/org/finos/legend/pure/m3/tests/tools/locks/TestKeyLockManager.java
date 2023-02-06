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

package org.finos.legend.pure.m3.tests.tools.locks;

import org.finos.legend.pure.m3.tools.locks.KeyLockManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestKeyLockManager
{
    private static final String KEY1 = "key1";
    private static final String KEY2 = "key2";

    private KeyLockManager<String> manager;

    @Before
    public void setUp()
    {
        this.manager = KeyLockManager.newManager();
    }

    @Test
    public void testGetLock()
    {
        Object lock1 = this.manager.getLock(KEY1);
        Object lock2 = this.manager.getLock(KEY2);

        Assert.assertSame(lock1, this.manager.getLock(KEY1));
        Assert.assertSame(lock2, this.manager.getLock(KEY2));
        Assert.assertFalse(lock1.equals(lock2));
    }
}
