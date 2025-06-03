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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstanceWrapper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecifications;

public class Imports
{
    private Imports()
    {
    }

    public static boolean isImportGroup(CoreInstance instance, ProcessorSupport processorSupport)
    {
        if (instance == null)
        {
            return false;
        }
        if (instance instanceof ImportGroup)
        {
            return true;
        }
        return (!(instance instanceof Any) || (instance instanceof AbstractCoreInstanceWrapper)) && processorSupport.instance_instanceOf(instance, M3Paths.ImportGroup);
    }

    public static SetIterable<PackageableElement> getImportGroupPackages(ImportGroup importGroup, ProcessorSupport processorSupport)
    {
        MutableSet<PackageableElement> packages = Sets.mutable.empty();

        // Get core imports
        ImportGroup coreImport = (ImportGroup) processorSupport.package_getByUserPath(M3Paths.coreImport);
        coreImport._imports().collect(imp -> (PackageableElement) processorSupport.package_getByUserPath(imp._path()), packages);

        // Get import group imports
        importGroup._imports().collect(imp -> (PackageableElement) processorSupport.package_getByUserPath(imp._path()), packages);

        // Remove null
        return packages.without(null);
    }

    public static CoreInstance getImportPackage(CoreInstance imp, ProcessorSupport processorSupport)
    {
        String pkgPath = imp.getValueForMetaPropertyToOne(M3Properties.path).getName();
        return processorSupport.package_getByUserPath(pkgPath);
    }

    public static ListIterable<? extends CoreInstance> getImportGroupsForSource(String sourceId, ProcessorSupport processorSupport)
    {
        CoreInstance imports = processorSupport.package_getByUserPath("system::imports");
        return (imports == null) ? Lists.immutable.empty() : getImportGroupsForSourceFromSystemImports(imports, sourceId);
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
