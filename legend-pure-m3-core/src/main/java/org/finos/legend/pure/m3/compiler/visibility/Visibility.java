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

package org.finos.legend.pure.m3.compiler.visibility;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.ElementWithStereotypes;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class Visibility
{
    /**
     * Return whether the given instance is visible in the
     * identified source.  This is true if the instance is
     * visible to the repository the source is in.  It is
     * also true if sourceId is null or if no repository can
     * be found for the identified source.
     *
     * @param instance         Pure instance
     * @param sourceId         source identifier
     * @param processorSupport processor support
     * @return whether instance is visible in the given source
     */
    public static boolean isVisibleInSource(CoreInstance instance, String sourceId, RichIterable<CodeRepository> codeRepositories, ProcessorSupport processorSupport)
    {
        if (sourceId == null)
        {
            return true;
        }
        String repositoryName = PureCodeStorage.getSourceRepoName(sourceId);
        CodeRepository repository = codeRepositories == null || repositoryName == null ? null : codeRepositories.select(r -> r.getName().equals(repositoryName)).getFirst();
        return isVisibleInRepository(instance, repository, codeRepositories, processorSupport);
    }

    /**
     * Return whether the given instance is visible in the
     * given repository.  This is true if the repository
     * the instance is defined in is visible to the given
     * repository.  It is also true if the given repository
     * is null.
     *
     * @param instance         Pure instance
     * @param repository       repository
     * @param processorSupport processor support
     * @return whether instance is visible in repository
     */
    private static boolean isVisibleInRepository(CoreInstance instance, CodeRepository repository, RichIterable<CodeRepository> codeRepositories, ProcessorSupport processorSupport)
    {
        if (codeRepositories == null || repository == null)
        {
            return true;
        }

        // Packages must be handled specially since they are not defined in a source
        if (processorSupport.instance_instanceOf(instance, M3Paths.Package))
        {
            String packagePath = PackageableElement.getUserPathForPackageableElement(instance, "::");
            if (M3Paths.Root.equals(packagePath))
            {
                return true;
            }
            for (CodeRepository repo : PureCodeStorage.getVisibleRepositories(codeRepositories, repository))
            {
                if (repo.isPackageAllowed(packagePath))
                {
                    return true;
                }
            }
            return false;
        }

        SourceInformation sourceInfo = instance.getSourceInformation();
        if (sourceInfo == null)
        {
            throw new RuntimeException("Cannot test visibility for an instance with no source information: " + instance);
        }

        String instanceRepositoryName = PureCodeStorage.getSourceRepoName(sourceInfo.getSourceId());
        if (instanceRepositoryName == null)
        {
            return false;
        }

        CodeRepository instanceRepository = codeRepositories.select(r -> r.getName().equals(instanceRepositoryName)).getFirst();
        return (instanceRepository != null) && repository.isVisible(instanceRepository);
    }

    /**
     * Return whether the given instance is visible in the given package
     * based on its access level.  Public elements are visible in all
     * packages.  Private elements are visible only in their own package.
     * Protected elements are visible in their own package and sub-packages.
     *
     * @param instance   instance
     * @param pkg        package
     * @param context    context
     * @return whether the element is visible in the package
     */
    public static boolean isVisibleInPackage(ElementWithStereotypes instance, CoreInstance pkg, Context context, ProcessorSupport processorSupport)
    {
        if (pkg == null)
        {
            throw new IllegalArgumentException("Cannot test visibility for an instance with no package");
        }
        AccessLevel accessLevel = AccessLevel.getAccessLevel(instance, context, processorSupport);
        switch (accessLevel)
        {
            case PUBLIC:
            case EXTERNALIZABLE:
            {
                return true;
            }
            case PROTECTED:
            {
                CoreInstance instancePkg = instance.getValueForMetaPropertyToOne(M3Properties._package);
                if (instancePkg != null)
                {
                    for (CoreInstance p = pkg; p != null; p = p.getValueForMetaPropertyToOne(M3Properties._package))
                    {
                        if (p == instancePkg)
                        {
                            return true;
                        }
                    }
                }
                return false;
            }
            case PRIVATE:
            {
                CoreInstance instancePkg = instance.getValueForMetaPropertyToOne(M3Properties._package);
                return (instancePkg != null) && (pkg == instancePkg);
            }
            default:
            {
                throw new RuntimeException("Unknown access level: " + accessLevel);
            }
        }
    }
}
