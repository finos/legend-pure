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

public final class ContractsCodeRepository extends SVNCodeRepository
{
    private static final String NAME = "contracts";
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("contracts::.*");

    ContractsCodeRepository()
    {
        super(NAME, PACKAGE_PATTERN);
    }

    @Override
    public boolean isVisible(CodeRepository other)
    {
        if (this == other)
        {
            return true;
        }

        return (other instanceof ContractsCodeRepository) || CodeRepositoryProviderHelper.isCoreRepository(other) || (other instanceof SystemCodeRepository) || (other != null && other.getName().startsWith("platform"));
    }
}
