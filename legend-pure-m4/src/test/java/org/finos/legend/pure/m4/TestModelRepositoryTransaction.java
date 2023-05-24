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

import org.finos.legend.pure.m4.serialization.grammar.M4Parser;
import org.finos.legend.pure.m4.statelistener.VoidM4StateListener;
import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;
import org.finos.legend.pure.m4.transaction.framework.ThreadLocalTransactionContext;
import org.junit.Assert;
import org.junit.Test;

public class TestModelRepositoryTransaction
{
    @Test
    public void testTopLevelRollBack() throws Exception
    {
        ModelRepository repository = new ModelRepository();
        new M4Parser().parse("^Class Class\n" +
                "{\n" +
                "    Class.properties[properties] :\n" +
                "        [\n" +
                "            ^Property properties\n" +
                "                {\n" +
                "                    Property.properties[type] : Property\n" +
                "                }\n" +
                "        ]\n" +
                "}\n" +
                "\n" +
                "^Class Property\n" +
                "{\n" +
                "    Class.properties[properties] :\n" +
                "        [\n" +
                "            ^Property type\n" +
                "                {\n" +
                "                    Property.properties[type] : Class\n" +
                "                }\n" +
                "        ]\n" +
                "}", repository, new VoidM4StateListener());
        repository.validate(new VoidM4StateListener());

        Assert.assertEquals(2, repository.getTopLevels().size());
        ModelRepositoryTransaction transaction = repository.newTransaction(true);
        try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
        {
            new M4Parser().parse("^Class Other\n" +
                    "{\n" +
                    "    Class.properties[properties] :\n" +
                    "        [\n" +
                    "            ^Property type\n" +
                    "                {\n" +
                    "                    Property.properties[type] : Class\n" +
                    "                }\n" +
                    "        ]\n" +
                    "}", repository, new VoidM4StateListener());
            Assert.assertEquals(3, repository.getTopLevels().size());
        }
        Assert.assertEquals(2, repository.getTopLevels().size());
        transaction.rollback();
        Assert.assertEquals(2, repository.getTopLevels().size());
    }

    @Test
    public void testTopLevelCommit() throws Exception
    {
        ModelRepository repository = new ModelRepository();
        new M4Parser().parse("^Class Class\n" +
                "{\n" +
                "    Class.properties[properties] :\n" +
                "        [\n" +
                "            ^Property properties\n" +
                "                {\n" +
                "                    Property.properties[type] : Property\n" +
                "                }\n" +
                "        ]\n" +
                "}\n" +
                "\n" +
                "^Class Property\n" +
                "{\n" +
                "    Class.properties[properties] :\n" +
                "        [\n" +
                "            ^Property type\n" +
                "                {\n" +
                "                    Property.properties[type] : Class\n" +
                "                }\n" +
                "        ]\n" +
                "}", repository, new VoidM4StateListener());
        repository.validate(new VoidM4StateListener());

        Assert.assertEquals(2, repository.getTopLevels().size());
        ModelRepositoryTransaction transaction = repository.newTransaction(true);
        try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
        {
            new M4Parser().parse("^Class Other\n" +
                    "{\n" +
                    "    Class.properties[properties] :\n" +
                    "        [\n" +
                    "            ^Property type\n" +
                    "                {\n" +
                    "                    Property.properties[type] : Class\n" +
                    "                }\n" +
                    "        ]\n" +
                    "}", repository, new VoidM4StateListener());
            Assert.assertEquals(3, repository.getTopLevels().size());
        }
        Assert.assertEquals(2, repository.getTopLevels().size());
        transaction.commit();
        Assert.assertEquals(3, repository.getTopLevels().size());
    }
}
