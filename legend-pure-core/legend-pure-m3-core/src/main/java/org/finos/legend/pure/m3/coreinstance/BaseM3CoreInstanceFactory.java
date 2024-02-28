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

package org.finos.legend.pure.m3.coreinstance;

import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.factory.CoreInstanceFactory;

public abstract class BaseM3CoreInstanceFactory implements CoreInstanceFactory
{
    public abstract boolean supports(String classifierPath);

    @Override
    public boolean supports(CoreInstance classifier)
    {
        return supports(getClassifierPath(classifier));
    }

    protected String getClassifierPath(CoreInstance classifier)
    {
        StringBuilder builder = new StringBuilder(64);
        writeClassifierPath(builder, classifier);
        return builder.toString();
    }

    protected void writeClassifierPath(StringBuilder builder, CoreInstance element)
    {
        CoreInstance pkg = element.getValueForMetaPropertyToOne("package");
        if ((pkg == null) || "Root".equals(pkg.getName()))
        {
            builder.append(element.getName());
        }
        else
        {
            writeClassifierPath(builder, pkg);
            builder.append("::");
            builder.append(element.getName());
        }
    }
}
