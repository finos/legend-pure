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

package org.finos.legend.pure.m3.navigation.imports;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.Import;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecifications;

public class Imports
{
    public static final Predicate2<CoreInstance, String> IS_IMPORT_GROUP_FOR_SOURCE = new Predicate2<CoreInstance, String>()
    {
        @Override
        public boolean accept(CoreInstance importGroup, String sourceId)
        {
            return isImportGroupForSource(importGroup, sourceId);
        }
    };

    private Imports()
    {
    }

    public static SetIterable<PackageableElement> getImportGroupPackages(ImportGroup importGroup, ProcessorSupport processorSupport)
    {

        // Get core imports
        ImportGroup coreImport = (ImportGroup)processorSupport.package_getByUserPath(M3Paths.coreImport);
        RichIterable<? extends Import> coreImports =  coreImport._imports();

        // Get import group imports
        RichIterable<? extends Import> importGroupImports = importGroup._imports();

        // Combine all imports and get packages
        MutableSet<PackageableElement> pkgs = UnifiedSet.newSet(coreImports.size() + importGroupImports.size());
        for (Import imp : LazyIterate.concatenate((ListIterable<Import>) coreImports, (ListIterable<Import>) importGroupImports))
        {
            PackageableElement pkg = (PackageableElement)getImportPackage(imp, processorSupport);
            if (pkg != null)
            {
                pkgs.add(pkg);
            }
        }
        return pkgs;
    }

    public static CoreInstance getImportPackage(CoreInstance imp, ProcessorSupport processorSupport)
    {
        String pkgPath = imp.getValueForMetaPropertyToOne(M3Properties.path).getName();
        return processorSupport.package_getByUserPath(pkgPath);
    }

    public static ListIterable<? extends CoreInstance> getImportGroupsForSource(String sourceId, ProcessorSupport processorSupport)
    {
        CoreInstance imports = processorSupport.package_getByUserPath("system::imports");
        return (imports == null) ? Lists.immutable.<CoreInstance>empty() : getImportGroupsForSourceFromSystemImports(imports, sourceId);
    }

    public static ListIterable<? extends CoreInstance> getImportGroupsForSourceFromSystemImports(CoreInstance systemImports, String sourceId)
    {
        return systemImports.getValueInValueForMetaPropertyToManyByIndex(M3Properties.children, IndexSpecifications.getSourceInfoSourceIdIndexSpec(), sourceId);
    }

    public static boolean isImportGroupForSource(CoreInstance importGroup, String sourceId)
    {
        SourceInformation sourceInfo = importGroup.getSourceInformation();
        return (sourceInfo != null) && sourceId.equals(sourceInfo.getSourceId());
    }

    public static String getImportGroupBaseName(CoreInstance importGroup)
    {
        String name = importGroup.getName();
        int firstUnderscore = name.indexOf('_');
        return (firstUnderscore == -1) ? name : name.substring(firstUnderscore + 1, name.lastIndexOf('_'));
    }
}
