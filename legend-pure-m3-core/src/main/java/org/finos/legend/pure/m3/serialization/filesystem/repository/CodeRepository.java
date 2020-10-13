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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;

public abstract class CodeRepository
{
    public static final Function<CodeRepository, String> GET_NAME = new Function<CodeRepository, String>()
    {
        @Override
        public String valueOf(CodeRepository codeRepository)
        {
            return codeRepository.getName();
        }
    };

    private static final Pattern VALID_REPO_NAME_PATTERN = Pattern.compile("[a-z]+(_[a-z]+)*");

    public final Predicate<CodeRepository> isVisible = new Predicate<CodeRepository>()
    {
        @Override
        public boolean accept(CodeRepository other)
        {
            return CodeRepository.this.isVisible(other);
        }
    };

    private final String name;
    private final Pattern allowedPackagesPattern;

    protected CodeRepository(String name, Pattern allowedPackagesPattern)
    {
        if (!isValidRepositoryName(name))
        {
            throw new IllegalArgumentException("Invalid repository name: " + name);
        }
        this.name = name;
        this.allowedPackagesPattern = allowedPackagesPattern;
    }

    public String getName()
    {
        return this.name;
    }

    public Pattern getAllowedPackagesPattern()
    {
        return this.allowedPackagesPattern;
    }

    public boolean isPackageAllowed(String pkg)
    {
        return (this.allowedPackagesPattern == null) || this.allowedPackagesPattern.matcher(pkg).matches();
    }

    /**
     * Return whether the other repository is visible to this
     * repository.  That is, is code in this repository
     * allowed to reference code in the other repository.
     *
     * @param other other code repository
     * @return whether other is visible to this
     */
    abstract public boolean isVisible(CodeRepository other);

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other == null || this.getClass() != other.getClass())
        {
            return false;
        }

        return this.name.equals(((CodeRepository)other).getName());
    }

    @Override
    public int hashCode()
    {
        return this.name.hashCode();
    }

    @Override
    public String toString()
    {
        return "<" + getClass().getSimpleName() + " \"" + this.name + "\">";
    }

    protected boolean isValidRepositoryName(String name)
    {
        return VALID_REPO_NAME_PATTERN.matcher(name).matches();
    }

    public static CodeRepository newPlatformCodeRepository()
    {
        return new PlatformCodeRepository();
    }

    public static CodeRepository newCoreCodeRepository()
    {
        return new CoreCodeRepository();
    }

    public static CodeRepository newScratchCodeRepository()
    {
        return new ScratchCodeRepository();
    }
}
