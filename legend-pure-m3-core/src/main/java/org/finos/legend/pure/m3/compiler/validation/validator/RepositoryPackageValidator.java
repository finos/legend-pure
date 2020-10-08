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

package org.finos.legend.pure.m3.compiler.validation.validator;

import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElementCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class RepositoryPackageValidator implements MatchRunner
{
    @Override
    public String getClassName()
    {
        return M3Paths.PackageableElement;
    }

    @Override
    public void run(CoreInstance instance, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        PackageableElement packageableElement = PackageableElementCoreInstanceWrapper.toPackageableElement(instance);

        SourceInformation sourceInfo = packageableElement.getSourceInformation();
        if (sourceInfo != null)
        {
            checkValidPackage(packageableElement._package(), sourceInfo);
            String sourceId = sourceInfo.getSourceId();
            String repoName = PureCodeStorage.getSourceRepoName(sourceId);
            if (repoName != null)
            {
                CodeRepository repo = ((ValidatorState)state).getCodeStorage().getRepository(repoName);
                if (repo != null)
                {
                    if (!(packageableElement instanceof Package) && !(packageableElement instanceof ImportGroup))
                    {
                        Package pkg = packageableElement._package();
                        if (pkg != null)
                        {
                            String pkgName = org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(pkg);
                            if (!repo.isPackageAllowed(pkgName))
                            {
                                throw new PureCompilationException(sourceInfo, "Package " + pkgName + " is not allowed in " + repoName + "; only packages matching " + repo.getAllowedPackagesPattern().toString() + " are allowed");
                            }
                        }
                    }
                }
            }
        }
    }

    private static void checkValidPackage(Package pkg, SourceInformation sourceInformation)
    {
        if (pkg == null)
        {
            return;
        }
        if (M3Paths.Root.equals(pkg._name()) && pkg._package() != null)
        {
            throw new PureCompilationException(sourceInformation, M3Paths.Root + " is not a valid user-defined package");
        }
        checkValidPackage(pkg._package(), sourceInformation);
    }
}
