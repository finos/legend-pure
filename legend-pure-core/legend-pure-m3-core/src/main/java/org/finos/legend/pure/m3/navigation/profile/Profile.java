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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.AnnotatedElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.TaggedValue;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class Profile
{

    public static boolean hasStereotype(CoreInstance annotatedElement, String profile, String stereotype, ProcessorSupport processorSupport)
    {
        CoreInstance testStereotype = Profile.findStereotype(processorSupport.package_getByUserPath(profile), stereotype);
        ListIterable<? extends CoreInstance> stereotypes = Instance.getValueForMetaPropertyToManyResolved(annotatedElement, M3Properties.stereotypes, processorSupport);
        return stereotypes.detect(x -> x == testStereotype) != null;
    }

    public static String getTaggedValue(CoreInstance annotatedElement, String profile, String tag, ProcessorSupport processorSupport)
    {
        return getTaggedValues(annotatedElement, profile, tag, processorSupport).getAny();
    }

    public static RichIterable<String> getTaggedValues(CoreInstance annotatedElement, String profile, String tag, ProcessorSupport processorSupport)
    {
        CoreInstance foundTag = Profile.findTag(processorSupport.package_getByUserPath(profile), tag);
        return ((AnnotatedElement)annotatedElement)
                ._taggedValues()
                .asLazy()
                .select(x -> x._tag() == foundTag)
                .collect(TaggedValue::_value);
    }

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
