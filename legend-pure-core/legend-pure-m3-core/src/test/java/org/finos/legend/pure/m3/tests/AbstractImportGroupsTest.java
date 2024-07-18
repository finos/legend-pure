// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m3.tests;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Objects;

public abstract class AbstractImportGroupsTest
{
    private static PureRuntime runtime;
    private static Package systemImports;

    private final MutableList<String> testSourceIds = Lists.mutable.empty();

    protected static void initialize(String... repositories)
    {
        CodeRepositorySet repos = CodeRepositorySet.newBuilder()
                .withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories())
                .build()
                .subset(repositories);
        initialize(new CompositeCodeStorage(new ClassLoaderCodeStorage(repos.getRepositories())));
    }

    protected static void initialize(MutableRepositoryCodeStorage codeStorage)
    {
//        System.out.println(codeStorage.getAllRepositories().collect(r -> r.getName(), Lists.mutable.empty()).sortThis().makeString("Repositories: ", ", ", ""));

        runtime = new PureRuntimeBuilder(codeStorage).buildAndInitialize();
        systemImports = (Package) runtime.getProcessorSupport().package_getByUserPath("system::imports");
        Assert.assertNotNull(systemImports);
    }

    @AfterClass
    public static void cleanUp()
    {
        if (runtime != null)
        {
            runtime.reset();
        }
        runtime = null;
        systemImports = null;
    }

    @Before
    public void clearTestSources()
    {
        this.testSourceIds.clear();
    }

    @After
    public void deleteTestSources()
    {
        if (this.testSourceIds.notEmpty())
        {
            this.testSourceIds.forEach(runtime::delete);
            runtime.compile();
        }
    }

    @Test
    public void testEmptySource()
    {
        String sourceId = "/platform/empty/emptySource.pure";
        testImportGroups(
                sourceId,
                "",
                importGroup("import__platform_empty_emptySource_pure_1", sourceId, 0, 0, 0, 0));
    }

    protected void testImportGroups(String sourceId, String code, ImportGroupForAssert... expected)
    {
        MutableList<ImportGroup> importGroups = compileTestSource(sourceId, code);
        Assert.assertEquals(ArrayAdapter.adapt(expected), importGroups.collect(AbstractImportGroupsTest::importGroup));
    }

    protected MutableList<ImportGroup> compileTestSource(String sourceId, String code)
    {
        this.testSourceIds.add(sourceId);
        runtime.createInMemoryAndCompile(Maps.fixedSize.with(sourceId, code));
        MutableList<ImportGroup> importGroups = systemImports._children().collectIf(
                ig -> (ig.getSourceInformation() != null) && sourceId.equals(ig.getSourceInformation().getSourceId()),
                ig -> (ImportGroup) ig,
                Lists.mutable.empty());
        assertNoDuplicateImportGroups(importGroups);
        return importGroups;
    }

    private void assertNoDuplicateImportGroups(Iterable<? extends ImportGroup> importGroups)
    {
        MutableMap<String, MutableList<CoreInstance>> importGroupsByName = Maps.mutable.empty();
        importGroups.forEach(ig -> importGroupsByName.getIfAbsentPut(ig._name(), Lists.mutable::empty).add(ig));
        importGroupsByName.removeIf((name, list) -> list.size() == 1);
        if (importGroupsByName.notEmpty())
        {
            StringBuilder builder = new StringBuilder("The following ImportGroups have name conflicts:");
            importGroupsByName.keysView().toSortedList().forEach(name ->
            {
                MutableList<CoreInstance> list = importGroupsByName.get(name);
                builder.append(System.lineSeparator()).append(name).append(" (").append(list.size()).append("):");
                list.forEach(ig -> ig.getSourceInformation().appendMessage(builder.append(System.lineSeparator()).append("\t")));
            });
            Assert.fail(builder.toString());
        }
    }

    protected static ImportGroupForAssert importGroup(String name, String sourceId, int startLine, int startCol, int endLine, int endCol, String... importPaths)
    {
        SourceInformation sourceInfo = new SourceInformation(sourceId, startLine, startCol, endLine, endCol);
        ListIterable<String> importPathList = (importPaths == null) ? Lists.immutable.empty() : ArrayAdapter.adapt(importPaths);
        return new ImportGroupForAssert(name, importPathList, sourceInfo);
    }

    protected static ImportGroupForAssert importGroup(ImportGroup importGroup)
    {
        return new ImportGroupForAssert(importGroup._name(), importGroup._imports().collect(ImportAccessor::_path, Lists.mutable.empty()), importGroup.getSourceInformation());
    }

    protected static class ImportGroupForAssert
    {
        private final String name;
        private final ListIterable<String> importPaths;
        private final SourceInformation sourceInfo;

        private ImportGroupForAssert(String name, ListIterable<String> importPaths, SourceInformation sourceInfo)
        {
            this.name = Objects.requireNonNull(name);
            this.importPaths = (importPaths == null) ? Lists.immutable.empty() : importPaths;
            this.sourceInfo = sourceInfo;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }

            if (!(other instanceof ImportGroupForAssert))
            {
                return false;
            }

            ImportGroupForAssert that = (ImportGroupForAssert) other;
            return this.name.equals(that.name) &&
                    this.importPaths.equals(that.importPaths) &&
                    Objects.equals(this.sourceInfo, that.sourceInfo);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(this.name, this.importPaths, this.sourceInfo);
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder("ImportGroup{name='").append(this.name).append("' paths=[");
            if (this.importPaths.notEmpty())
            {
                this.importPaths.appendString(builder, "'", "', '", "'");
            }
            builder.append("] sourceInfo=");
            if (this.sourceInfo == null)
            {
                builder.append("null");
            }
            else
            {
                this.sourceInfo.appendMessage(builder);
            }
            return builder.append("}").toString();
        }
    }
}
