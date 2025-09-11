// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.compiler.metadata;

import org.finos.legend.pure.m3.navigation.M3Paths;

public class VirtualPackageMetadata extends PackageableElementMetadata
{
    public VirtualPackageMetadata(String path)
    {
        super(path, M3Paths.Package);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof VirtualPackageMetadata))
        {
            return false;
        }

        VirtualPackageMetadata that = (VirtualPackageMetadata) other;
        return this.path.equals(that.path) &&
                this.classifierPath.equals(that.classifierPath);
    }

    @Override
    public int hashCode()
    {
        return this.path.hashCode() + (31 * this.classifierPath.hashCode());
    }
}
