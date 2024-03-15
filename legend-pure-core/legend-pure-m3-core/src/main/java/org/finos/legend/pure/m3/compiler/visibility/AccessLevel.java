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

package org.finos.legend.pure.m3.compiler.visibility;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.ElementWithStereotypes;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

/**
 * Access levels for packageable elements.
 */
public enum AccessLevel
{
    PRIVATE("private"),
    PROTECTED("protected"),
    PUBLIC("public"),
    EXTERNALIZABLE("externalizable");

    private final String name;

    AccessLevel(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    public CoreInstance getStereotype(ProcessorSupport processorSupport)
    {
        CoreInstance profile = processorSupport.package_getByUserPath(M3Paths.access);
        if (profile == null)
        {
            throw new RuntimeException("Cannot find Profile: " + M3Paths.access);
        }
        CoreInstance stereotype = org.finos.legend.pure.m3.navigation.profile.Profile.findStereotype(profile, this.name);
        if (stereotype == null)
        {
            throw new RuntimeException("Cannot find stereotype '" + this.name + "' in Profile " + M3Paths.access);
        }
        return stereotype;
    }

    /**
     * Return whether a Pure instance has an access level explicitly
     * stated.
     *
     * @param instance         Pure instance
     * @param processorSupport processor support
     * @return whether the instance has an explicit access level
     */
    public static boolean hasExplicitAccessLevel(PackageableFunction<?> instance, ProcessorSupport processorSupport)
    {
        ListIterable<? extends Stereotype> stereotypes = (ListIterable<? extends Stereotype>) ImportStub.withImportStubByPasses((ListIterable<? extends CoreInstance>) instance._stereotypesCoreInstance(), processorSupport);
        if (stereotypes.notEmpty())
        {
            Profile profile = (Profile) processorSupport.package_getByUserPath(M3Paths.access);
            for (Stereotype st : stereotypes)
            {
                if (st._profile() == profile)
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the access level stereotypes for a packageable element.
     *
     * @param packageableElement packageable element
     * @param processorSupport   processor support
     * @return access level stereotypes
     */
    public static ListIterable<Stereotype> getAccessLevelStereotypes(ElementWithStereotypes packageableElement, ProcessorSupport processorSupport)
    {
        ListIterable<Stereotype> stereotypes = (ListIterable<Stereotype>) ImportStub.withImportStubByPasses((ListIterable<? extends CoreInstance>) packageableElement._stereotypesCoreInstance(), processorSupport);
        if (stereotypes.isEmpty())
        {
            return Lists.immutable.empty();
        }

        MutableList<Stereotype> accessStereotypes = Lists.mutable.empty();
        CoreInstance profile = processorSupport.package_getByUserPath(M3Paths.access);
        for (Stereotype stereotype : stereotypes)
        {
            if (stereotype._profile() == profile)
            {
                accessStereotypes.add(stereotype);
            }
        }
        return accessStereotypes;
    }

    /**
     * Get the access level for a packageable element.  If no access
     * level is explicitly specified, then it is assumed to be public.
     *
     * @param packageableElement packageable element
     * @param context            context
     * @param processorSupport   processor support
     * @return access level
     */
    public static AccessLevel getAccessLevel(ElementWithStereotypes packageableElement, Context context, ProcessorSupport processorSupport)
    {
        return (context == null) ?
                calculateAccessLevel(packageableElement, processorSupport) :
                context.getIfAbsentPutAccessLevel(packageableElement, instance -> calculateAccessLevel((ElementWithStereotypes) instance, processorSupport));
    }

    private static AccessLevel calculateAccessLevel(ElementWithStereotypes packageableElement, ProcessorSupport processorSupport)
    {
        ListIterable<? extends Stereotype> stereotypes = (ListIterable<? extends Stereotype>) ImportStub.withImportStubByPasses((ListIterable<? extends CoreInstance>) packageableElement._stereotypesCoreInstance(), processorSupport);
        if (stereotypes.notEmpty())
        {
            Profile accessProfile = (Profile) processorSupport.package_getByUserPath(M3Paths.access);
            for (Stereotype st : stereotypes)
            {
                if (st._profile() == accessProfile)
                {
                    return getLevelFromAccessStereotype(st);
                }
            }
        }
        return PUBLIC;
    }

    /**
     * Get the access level from an access stereotype, i.e., a
     * stereotype with the profile meta::pure::profiles::access.
     * It is assumed (and not verified) that the stereotype has
     * the appropriate profile.
     *
     * @param stereotype access stereotype
     * @return access level
     */
    private static AccessLevel getLevelFromAccessStereotype(Stereotype stereotype)
    {
        String accessLevelName = stereotype._value();
        for (AccessLevel accessLevel : AccessLevel.values())
        {
            if (accessLevel.name.equals(accessLevelName))
            {
                return accessLevel;
            }
        }
        throw new IllegalArgumentException("Unknown access level: " + accessLevelName);
    }
}
