// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.m2.relational.incremental;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.pure.m2.relational.AbstractPureRelationalTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.junit.Before;
import org.junit.Test;

public class TestView extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final String GroupByModel =
            "###Pure\n" +
            "Class AccountPnl\n" +
            "{\n" +
            "   pnl:Float[1];\n" +
            "}";

    private static final String GroupByStoreTemplate =
            "###Relational\n" +
            "Database db(\n" +
            "   Table orderTable(ID INT PRIMARY KEY, accountID INT)\n" +
            "   Table orderPnlTable( ORDER_ID INT PRIMARY KEY, pnl FLOAT)\n" +
            "   Join OrderPnlTable_Order(orderPnlTable.ORDER_ID = orderTable.ID)\n" +
            "\n" +
            "   View accountOrderPnlView\n" +
            "   (\n" +
            "      ~groupBy (orderTable.accountID)\n" +
            "      accountId : orderTable.accountID PRIMARY KEY,\n" +
            "      %s : sum(@OrderPnlTable_Order | orderPnlTable.pnl)\n" +
            "   )\n" +
            ")";

    private static final String GroupByMappingTemplate =
            "###Mapping\n" +
            "Mapping testMapping\n" +
            "(\n" +
            "   AccountPnl : Relational  \n" +
            "   {\n" +
            "      pnl : [db]accountOrderPnlView.%s \n" +
            "   }\n" +
            ")";

    @Before
    @Override
    public void _setUp()
    {
        setUpRuntime(getFunctionExecution(), PureCodeStorage.createCodeStorage(getCodeStorageRoot(), getCodeRepositories()), getFactoryRegistryOverride(), getOptions(), getExtra());
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        MutableList<CodeRepository> repositories = org.eclipse.collections.api.factory.Lists.mutable.withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories());
        repositories.add(GenericCodeRepository.build("test", "((test)|(meta))(::.*)?", PlatformCodeRepository.NAME));
        return repositories;
    }

    @Test
    public void testGroupByIncrementalSyntaticStoreChange()
    {
        String viewDynaColName = "orderPnl";
        String groupByStore = String.format(GroupByStoreTemplate, viewDynaColName);
        String groupByMapping = String.format(GroupByMappingTemplate, viewDynaColName);
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                Maps.mutable.with("store.pure", groupByStore, "model.pure", GroupByModel, "mapping.pure", groupByMapping))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("store.pure", groupByStore + " ")
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testGroupByIncrementalModelChange()
    {
        String viewDynaColName = "orderPnl";
        String groupByStore = String.format(GroupByStoreTemplate, viewDynaColName);
        String groupByMapping = String.format(GroupByMappingTemplate, viewDynaColName);
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                Maps.mutable.with("store.pure", groupByStore, "model.pure", GroupByModel, "mapping.pure", groupByMapping))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("model.pure", GroupByModel + "\n\n\n")
                        .compile()
                        .updateSource("model.pure", GroupByModel)
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testGroupByIncrementalStoreAndMappingChange()
    {
        String viewDynaColName = "orderPnl";
        String groupByStore = String.format(GroupByStoreTemplate, viewDynaColName);
        String groupByMapping = String.format(GroupByMappingTemplate, viewDynaColName);
        String viewDynaColNameUpdated = "orderPnlUpdated";
        String groupByStoreUpdated = String.format(GroupByStoreTemplate, viewDynaColNameUpdated);
        String groupByMappingUpdated = String.format(GroupByMappingTemplate, viewDynaColNameUpdated);
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                Maps.mutable.with("store.pure", groupByStore, "model.pure", GroupByModel, "mapping.pure", groupByMapping))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("store.pure", groupByStoreUpdated).updateSource("mapping.pure", groupByMappingUpdated)
                        .compile()
                        .updateSource("store.pure", groupByStore).updateSource("mapping.pure", groupByMapping)
                        .compile()
                , runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testViewInSchemaReferencingTableInIncludedDB()
    {
        String includedDB1Source = "/test/includedDB1.pure";
        String includedDB1 = "###Relational\n" +
                "Database test::store::IncludedDB1\n" +
                "(\n" +
                "    Schema S1\n" +
                "    (\n" +
                "        Table T1 (ID INT PRIMARY KEY, NAME VARCHAR(200), OTHER_ID INT)\n" +
                "    )\n" +
                ")\n";
        String mainDBSource = "/test/mainDB.pure";
        String mainDB = "###Relational\n" +
                "Database test::store::MainDB\n" +
                "(\n" +
                "    include test::store::IncludedDB1\n" +
                "\n" +
                "    Schema S1\n" +
                "    (\n" +
                "        View V1\n" +
                "        (\n" +
                "            id: T1.ID PRIMARY KEY,\n" +
                "            name: T1.NAME\n" +
                "        )\n" +
                "    )\n" +
                ")\n";
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource(includedDB1Source, includedDB1)
                        .createInMemorySource(mainDBSource, mainDB)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource(includedDB1Source)
                        .createInMemorySource(includedDB1Source, includedDB1)
                        .compile(),
                runtime, functionExecution, Lists.immutable.empty());
    }
}