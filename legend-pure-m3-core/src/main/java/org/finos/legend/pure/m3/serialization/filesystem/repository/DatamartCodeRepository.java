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

public final class DatamartCodeRepository extends SVNCodeRepository
{
    private static final String REPO_NAME_PREFIX = "datamart";

    DatamartCodeRepository(String businessUnit, String businessUnitPackage)
    {
        super(getRepositoryName(businessUnit), getPattern(businessUnitPackage));
    }

    DatamartCodeRepository(String businessUnit)
    {
        this(businessUnit, businessUnit);
    }

    @Override
    public boolean isVisible(CodeRepository other)
    {
        return (this == other) ||
                (other instanceof ModelCodeRepository) ||
                CodeRepositoryProviderHelper.isCoreRepository(other) ||
                (other instanceof SystemCodeRepository) ||
                (other != null && other.getName().startsWith("platform"));
    }

    private static String getRepositoryName(String businessUnit)
    {
        return REPO_NAME_PREFIX + "_" + businessUnit;
    }

    public static Pattern getPattern(String businessUnitPackage)
    {
        return Pattern.compile("((datamarts::" + businessUnitPackage + "::(domain|mapping|store))|(apps::" + businessUnitPackage + "))(::.*)?");
    }
}
