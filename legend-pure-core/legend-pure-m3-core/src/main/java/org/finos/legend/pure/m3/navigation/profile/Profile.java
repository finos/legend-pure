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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.AnnotatedElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.AnnotationAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.TaggedValue;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class Profile
{
    public static boolean hasStereotype(CoreInstance annotatedElement, String profilePath, String stereotypeName, ProcessorSupport processorSupport)
    {
        CoreInstance stereotype = findStereotype(profilePath, stereotypeName, processorSupport, true);
        return Instance.getValueForMetaPropertyToManyResolved(annotatedElement, M3Properties.stereotypes, processorSupport).anySatisfy(x -> x == stereotype);
    }

    public static String getTaggedValue(CoreInstance annotatedElement, String profilePath, String tagName, ProcessorSupport processorSupport)
    {
        CoreInstance tag = findTag(profilePath, tagName, processorSupport, true);
        if (annotatedElement instanceof AnnotatedElement)
        {
            TaggedValue taggedValue = ((AnnotatedElement) annotatedElement)._taggedValues().detect(tv -> tv._tag() == tag);
            return (taggedValue == null) ? null : taggedValue._value();
        }
        CoreInstance taggedValue = annotatedElement.getValueForMetaPropertyToMany(M3Properties.taggedValues).detect(tv -> Instance.getValueForMetaPropertyToOneResolved(tv, M3Properties.tag, processorSupport) == tag);
        return (taggedValue == null) ? null : PrimitiveUtilities.getStringValue(taggedValue.getValueForMetaPropertyToOne(M3Properties.value));
    }

    public static RichIterable<String> getTaggedValues(CoreInstance annotatedElement, String profilePath, String tagName, ProcessorSupport processorSupport)
    {
        CoreInstance tag = findTag(profilePath, tagName, processorSupport, true);
        if (annotatedElement instanceof AnnotatedElement)
        {
            return ((AnnotatedElement) annotatedElement)._taggedValues().collectIf(tv -> tv._tag() == tag, TaggedValue::_value);
        }
        ListIterable<? extends CoreInstance> taggedValues = annotatedElement.getValueForMetaPropertyToMany(M3Properties.taggedValues);
        return taggedValues.isEmpty() ?
               Lists.immutable.empty() :
               taggedValues.collectIf(
                       tv -> Instance.getValueForMetaPropertyToOneResolved(tv, M3Properties.tag, processorSupport) == tag,
                       tv -> PrimitiveUtilities.getStringValue(tv.getValueForMetaPropertyToOne(M3Properties.value)));
    }

    public static boolean hasTaggedValue(CoreInstance annotatedElement, String profilePath, String tagName, String value, ProcessorSupport processorSupport)
    {
        CoreInstance tag = findTag(profilePath, tagName, processorSupport, true);
        return (annotatedElement instanceof AnnotatedElement) ?
               ((AnnotatedElement) annotatedElement)._taggedValues().anySatisfy(tv -> (tv._tag() == tag) && value.equals(tv._value())) :
               annotatedElement.getValueForMetaPropertyToMany(M3Properties.taggedValues).anySatisfy(tv -> (Instance.getValueForMetaPropertyToOneResolved(tv, M3Properties.tag, processorSupport) == tag) && value.equals(PrimitiveUtilities.getStringValue(tv.getValueForMetaPropertyToOne(M3Properties.value))));
    }

    public static CoreInstance findStereotype(String profilePath, String value, ProcessorSupport processorSupport)
    {
        return findStereotype(profilePath, value, processorSupport, false);
    }

    public static CoreInstance findStereotype(String profilePath, String value, ProcessorSupport processorSupport, boolean errorIfNotFound)
    {
        CoreInstance profile = processorSupport.package_getByUserPath(profilePath);
        if (profile == null)
        {
            if (errorIfNotFound)
            {
                throw new RuntimeException("Cannot find stereotype '" + profilePath + "." + value + "': profile '" + profilePath + "' not found");
            }
            return null;
        }
        CoreInstance stereotype = findStereotype(profile, value, false);
        if (errorIfNotFound && (stereotype == null))
        {
            throw new RuntimeException("Cannot find stereotype '" + profilePath + "." + value + "': profile '" + profilePath + "' does not contain stereotype '" + value + "'");
        }
        return stereotype;

    }

    public static CoreInstance findStereotype(CoreInstance profile, String value)
    {
        return findStereotype(profile, value, false);
    }

    public static CoreInstance findStereotype(CoreInstance profile, String value, boolean errorIfNotFound)
    {
        CoreInstance stereotype = (profile instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile) ?
               findAnnotation(((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile) profile)._p_stereotypes(), value) :
               findAnnotation(profile, M3Properties.p_stereotypes, value);
        if (errorIfNotFound && (stereotype == null))
        {
            throw new RuntimeException(PackageableElement.writeUserPathForPackageableElement(new StringBuilder("Cannot find stereotype '" + value + "' in profile '"), profile).append("'").toString());
        }
        return stereotype;
    }

    public static CoreInstance findTag(String profilePath, String value, ProcessorSupport processorSupport)
    {
        return findTag(profilePath, value, processorSupport, false);
    }

    public static CoreInstance findTag(String profilePath, String value, ProcessorSupport processorSupport, boolean errorIfNotFound)
    {
        CoreInstance profile = processorSupport.package_getByUserPath(profilePath);
        if (profile == null)
        {
            if (errorIfNotFound)
            {
                throw new RuntimeException("Cannot find tag '" + profilePath + "." + value + "': profile '" + profilePath + "' not found");
            }
            return null;
        }
        CoreInstance tag = findTag(profile, value, false);
        if (errorIfNotFound && (tag == null))
        {
            throw new RuntimeException("Cannot find tag '" + profilePath + "." + value + "': profile '" + profilePath + "' does not contain tag '" + value + "'");
        }
        return tag;
    }

    public static CoreInstance findTag(CoreInstance profile, String value)
    {
        return findTag(profile, value, false);
    }

    public static CoreInstance findTag(CoreInstance profile, String value, boolean errorIfNotFound)
    {
        CoreInstance tag = (profile instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile) ?
               findAnnotation(((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile) profile)._p_tags(), value) :
               findAnnotation(profile, M3Properties.p_tags, value);
        if (errorIfNotFound && (tag == null))
        {
            throw new RuntimeException(PackageableElement.writeUserPathForPackageableElement(new StringBuilder("Cannot find tag '" + value + "' in profile '"), profile).append("'").toString());
        }
        return tag;
    }

    private static CoreInstance findAnnotation(RichIterable<? extends AnnotationAccessor> annotations, String value)
    {
        return annotations.detect(a -> value.equals(a._value()));
    }

    private static CoreInstance findAnnotation(CoreInstance profile, String annotationProperty, String value)
    {
        return profile.getValueForMetaPropertyToMany(annotationProperty).detect(a -> value.equals(PrimitiveUtilities.getStringValue(a.getValueForMetaPropertyToOne(M3Properties.value))));
    }
}
