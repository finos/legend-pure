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

package org.finos.legend.pure.m3.serialization.filesystem.repository;

import java.util.regex.Pattern;

public abstract class SVNCodeRepository extends VersionControlledCodeRepository
{
    protected SVNCodeRepository(String name, Pattern allowedPackagesPattern)
    {
        super(name, allowedPackagesPattern);
    }

    public static CodeRepository newAppCodeRepository(String appName, String appGroup)
    {
        return new AppCodeRepository(appName, appGroup, null);
    }

    public static CodeRepository newAppCodeRepository(String appName, String appPackageName, String appGroup, Iterable<String> visibleRepos)
    {
        return new AppCodeRepository(appName, appPackageName, appGroup, visibleRepos);
    }

    public static SVNCodeRepository newAppCodeRepository(String appName, String appGroup, Iterable<String> visibleRepos)
    {
        return new AppCodeRepository(appName, appGroup, visibleRepos);
    }

    public static SVNCodeRepository newContractsCodeRepository()
    {
        return new ContractsCodeRepository();
    }

    public static SVNCodeRepository newDatamartCodeRepository(String businessUnit, String businessUnitPackage)
    {
        return new DatamartCodeRepository(businessUnit, businessUnitPackage);
    }

    public static SVNCodeRepository newDatamartCodeRepository(String businessUnit)
    {
        return new DatamartCodeRepository(businessUnit);
    }

    public static SVNCodeRepository newModelValidationCodeRepository()
    {
        return new ModelValidationCodeRepository();
    }

    public static SVNCodeRepository newModelCodeRepository(String modelName, Iterable<String> visibleModelRepos)
    {
        return new ModelCodeRepository(modelName, visibleModelRepos);
    }

    public static SVNCodeRepository newModelCodeRepository(String modelName)
    {
        return newModelCodeRepository(modelName, null);
    }

    public static SVNCodeRepository newSystemCodeRepository()
    {
        return new SystemCodeRepository();
    }

    public static SVNCodeRepository newProtocolsVersionsRepository()
    {
        return new ProtocolsVersionsCodeRepository();
    }
}
