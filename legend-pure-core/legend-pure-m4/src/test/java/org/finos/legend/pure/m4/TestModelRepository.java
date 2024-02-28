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

package org.finos.legend.pure.m4;

import org.finos.legend.pure.m4.ModelRepository;
import org.junit.Assert;
import org.junit.Test;

public class TestModelRepository
{
    @Test
    public void testIsAnonymousInstanceName()
    {
        Assert.assertTrue(ModelRepository.isAnonymousInstanceName("@_0000001234"));

        Assert.assertFalse(ModelRepository.isAnonymousInstanceName(null));
        Assert.assertFalse(ModelRepository.isAnonymousInstanceName(""));
        Assert.assertFalse(ModelRepository.isAnonymousInstanceName("Class"));
        Assert.assertFalse(ModelRepository.isAnonymousInstanceName("Package"));
        Assert.assertFalse(ModelRepository.isAnonymousInstanceName("@"));
        Assert.assertFalse(ModelRepository.isAnonymousInstanceName("@1234"));
        Assert.assertFalse(ModelRepository.isAnonymousInstanceName("@abcd"));
        Assert.assertFalse(ModelRepository.isAnonymousInstanceName("@_"));
        Assert.assertFalse(ModelRepository.isAnonymousInstanceName("@_abcd"));
        Assert.assertFalse(ModelRepository.isAnonymousInstanceName("@_a234"));
        Assert.assertFalse(ModelRepository.isAnonymousInstanceName("@_1234a"));
        Assert.assertFalse(ModelRepository.isAnonymousInstanceName("@_1_5_67"));
    }
}
