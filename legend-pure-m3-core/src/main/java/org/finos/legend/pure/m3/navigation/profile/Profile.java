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

package org.finos.legend.pure.m3.navigation.profile;

import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class Profile
{
    public static CoreInstance findStereotype(CoreInstance profile, String value)
    {
        return findAnnotation(profile, M3Properties.p_stereotypes, value);
    }

    public static CoreInstance findTag(CoreInstance profile, String value)
    {
        return findAnnotation(profile, M3Properties.p_tags, value);
    }

    private static CoreInstance findAnnotation(CoreInstance profile, String annotationProperty, String value)
    {
        for (CoreInstance annotation : profile.getValueForMetaPropertyToMany(annotationProperty))
        {
            if (value.equals(annotation.getValueForMetaPropertyToOne(M3Properties.value).getName()))
            {
                return annotation;
            }
        }
        return null;
    }
}
