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

package org.finos.legend.pure.m3.compiler;

import org.finos.legend.pure.m3.navigation.Instance;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecifications;

public class ReferenceUsage
{
    private static final IndexSpecification<ReferenceUsageIndexKey> REF_USAGE_INDEX_SPEC = new IndexSpecification<ReferenceUsageIndexKey>()
    {
        @Override
        public ReferenceUsageIndexKey getIndexKey(CoreInstance referenceUsage)
        {
            return new ReferenceUsageIndexKey(referenceUsage);
        }
    };

    /**
     * Add a ReferenceUsage to the used instance.
     *
     * @param used             instance being used (which will get the ReferenceUsage)
     * @param user             instance that uses the referenced instance
     * @param propertyName     name of the property for the reference
     * @param offset           offset of the reference in the list of property values
     * @param repository       model repository
     * @param processorSupport processor support
     */
    public static void addReferenceUsage(CoreInstance used, CoreInstance user, String propertyName, int offset, ModelRepository repository, ProcessorSupport processorSupport)
    {
        addReferenceUsage(used, user, propertyName, offset, repository, processorSupport, null);
    }

    /**
     * Add a ReferenceUsage to the used instance.
     *
     * @param used                      instance being used (which will get the ReferenceUsage)
     * @param user                      instance that uses the referenced instance
     * @param propertyName              name of the property for the reference
     * @param offset                    offset of the reference in the list of property values
     * @param repository                model repository
     * @param processorSupport          processor support
     * @param sourceInformationForUsage optional additional source information, useful if the owner is not the place the reference in code occurs
     */
    public static void addReferenceUsage(CoreInstance used, CoreInstance user, String propertyName, int offset, ModelRepository repository, ProcessorSupport processorSupport, SourceInformation sourceInformationForUsage)
    {
        if (!Instance.instanceOf(used, M3Paths.PrimitiveType, processorSupport))
        {
            addReferenceUsage_internal(used, user, propertyName, offset, repository, processorSupport, sourceInformationForUsage);
        }
    }

    private static void addReferenceUsage_internal(CoreInstance used, CoreInstance user, String propertyName, int offset, ModelRepository repository, ProcessorSupport processorSupport, SourceInformation sourceInformationForUsage)
    {
        CoreInstance usageReference = createReferenceUsage(user, propertyName, offset, repository, processorSupport);
        usageReference.setSourceInformation(sourceInformationForUsage);
        Instance.addValueToProperty(used, M3Properties.referenceUsages, usageReference, processorSupport);
    }

    /**
     * Remove all ReferenceUsages for user from used.  Returns the
     * number of ReferenceUsages actually removed.
     *
     * @param used             instance being used (which has the ReferenceUsages to be removed)
     * @param user             instance that uses the referenced instance
     * @param processorSupport processor support
     * @return number of ReferenceUsages removed from instanceBeingUsed
     */
    public static int removeReferenceUsagesForUser(CoreInstance used, CoreInstance user, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> userReferenceUsages = used.getValueInValueForMetaPropertyToManyByIndex(M3Properties.referenceUsages, IndexSpecifications.getPropertyValueIndexSpec(M3Properties.owner), user);
        int size = userReferenceUsages.size();
        if (size != 0)
        {
            for (CoreInstance referenceUsage : userReferenceUsages.toList())
            {
                used.removeValueForMetaPropertyToMany(M3Properties.referenceUsages, referenceUsage);
            }
            if (used.getValueForMetaPropertyToMany(M3Properties.referenceUsages).isEmpty())
            {
                Instance.removeProperty(used, M3Properties.referenceUsages, processorSupport);
            }
        }
        return size;
    }

    /**
     * Create a ReferenceUsage instance.  Note that this does add the
     * reference usage anywhere - it just creates the object.
     *
     * @param owner            instance that uses the referenced instance
     * @param propertyName     name of the property for the reference
     * @param offset           offset of the reference in the list of property values
     * @param repository       model repository
     * @param processorSupport processor support
     * @return ReferenceUsage instance
     */
    public static CoreInstance createReferenceUsage(CoreInstance owner, String propertyName, int offset, ModelRepository repository, ProcessorSupport processorSupport)
    {
        CoreInstance referenceUsage = processorSupport.newAnonymousCoreInstance(null, M3Paths.ReferenceUsage);
        referenceUsage.addKeyValue(M3PropertyPaths.owner_ReferenceUsage, owner);
        referenceUsage.addKeyValue(M3PropertyPaths.propertyName_ReferenceUsage, repository.newStringCoreInstance_cached(propertyName));
        referenceUsage.addKeyValue(M3PropertyPaths.offset_ReferenceUsage, repository.newIntegerCoreInstance(offset));
        return referenceUsage;
    }

    /**
     * Get the instance being used from a ReferenceUsage. This uses the ReferenceUsage
     * to access the named property on the owner and get the value at the offset.
     *
     * @param referenceUsage   reference usage
     * @param processorSupport processor support
     * @return instance being used
     */
    public static CoreInstance getUsed(CoreInstance referenceUsage, ProcessorSupport processorSupport)
    {
        try
        {
            CoreInstance owner = referenceUsage.getValueForMetaPropertyToOne(M3Properties.owner);
            String propertyName = PrimitiveUtilities.getStringValue(referenceUsage.getValueForMetaPropertyToOne(M3Properties.propertyName));
            int offset = PrimitiveUtilities.getIntegerValue(referenceUsage.getValueForMetaPropertyToOne(M3Properties.offset)).intValue();
            CoreInstance rawValue = owner.getValueForMetaPropertyToMany(propertyName).get(offset);
            return ImportStub.withImportStubByPass(rawValue, processorSupport);
        }
        catch (Exception e)
        {
            StringBuilder message = new StringBuilder("Error getting used instance for reference usage: ");
            writeReferenceUsage(message, referenceUsage, true);
            throw new RuntimeException(message.toString(), e);
        }
    }

    /**
     * Return whether two reference usages are equal to each other in terms
     * of property values.
     *
     * @param referenceUsage1 one reference usage
     * @param referenceUsage2 another reference usage
     * @return whether reference usages are equal
     */
    public static boolean referenceUsagesEqual(CoreInstance referenceUsage1, CoreInstance referenceUsage2)
    {
        if (referenceUsage1 == referenceUsage2)
        {
            return true;
        }

        int offset1 = PrimitiveUtilities.getIntegerValue(referenceUsage1.getValueForMetaPropertyToOne(M3Properties.offset)).intValue();
        int offset2 = PrimitiveUtilities.getIntegerValue(referenceUsage2.getValueForMetaPropertyToOne(M3Properties.offset)).intValue();
        if (offset1 != offset2)
        {
            return false;
        }

        String propertyName1 = PrimitiveUtilities.getStringValue(referenceUsage1.getValueForMetaPropertyToOne(M3Properties.propertyName));
        String propertyName2 = PrimitiveUtilities.getStringValue(referenceUsage2.getValueForMetaPropertyToOne(M3Properties.propertyName));
        if (!propertyName1.equals(propertyName2))
        {
            return false;
        }

        CoreInstance owner1 = referenceUsage1.getValueForMetaPropertyToOne(M3Properties.owner);
        CoreInstance owner2 = referenceUsage2.getValueForMetaPropertyToOne(M3Properties.owner);
        return owner1 == owner2;
    }

    /**
     * Return whether two reference usages are similar to each other. This is true
     * if the reference usages are equal to each other in terms of property values
     * or if the offsets and property names are equal and the owners are either the
     * same or are equal in terms of classifier and non-null source information. This
     * can be useful for comparing reference usages across different compilations.
     *
     * @param referenceUsage1 one reference usage
     * @param referenceUsage2 another reference usage
     * @return whether reference usages are similar
     */
    public static boolean referenceUsagesSimilar(CoreInstance referenceUsage1, CoreInstance referenceUsage2)
    {
        if (referenceUsage1 == referenceUsage2)
        {
            return true;
        }

        int offset1 = PrimitiveUtilities.getIntegerValue(referenceUsage1.getValueForMetaPropertyToOne(M3Properties.offset)).intValue();
        int offset2 = PrimitiveUtilities.getIntegerValue(referenceUsage2.getValueForMetaPropertyToOne(M3Properties.offset)).intValue();
        if (offset1 != offset2)
        {
            return false;
        }

        String propertyName1 = PrimitiveUtilities.getStringValue(referenceUsage1.getValueForMetaPropertyToOne(M3Properties.propertyName));
        String propertyName2 = PrimitiveUtilities.getStringValue(referenceUsage2.getValueForMetaPropertyToOne(M3Properties.propertyName));
        if (!propertyName1.equals(propertyName2))
        {
            return false;
        }

        CoreInstance owner1 = referenceUsage1.getValueForMetaPropertyToOne(M3Properties.owner);
        CoreInstance owner2 = referenceUsage2.getValueForMetaPropertyToOne(M3Properties.owner);
        if (owner1 == owner2)
        {
            return true;
        }

        CoreInstance owner1Classifier = owner1.getClassifier();
        CoreInstance owner2Classifier = owner2.getClassifier();
        if ((owner1Classifier != owner2Classifier) && !PackageableElement.getUserPathForPackageableElement(owner1Classifier).equals(PackageableElement.getUserPathForPackageableElement(owner2Classifier)))
        {
            return false;
        }

        SourceInformation owner1SourceInfo = owner1.getSourceInformation();
        SourceInformation owner2SourceInfo = owner2.getSourceInformation();
        return (owner1SourceInfo != null) && (owner2SourceInfo != null) && owner1SourceInfo.equals(owner2SourceInfo);
    }

    /**
     * Compare two reference usages, first based on owner, then on property
     * name, then on offset.
     *
     * @param referenceUsage1 first reference usage
     * @param referenceUsage2 second reference usage
     * @return comparison
     */
    public static int compareReferenceUsages(CoreInstance referenceUsage1, CoreInstance referenceUsage2)
    {
        if (referenceUsage1 == referenceUsage2)
        {
            return 0;
        }

        CoreInstance owner1 = referenceUsage1.getValueForMetaPropertyToOne(M3Properties.owner);
        CoreInstance owner2 = referenceUsage2.getValueForMetaPropertyToOne(M3Properties.owner);
        if (owner1 != owner2)
        {
            CoreInstance owner1Classifier = owner1.getClassifier();
            CoreInstance owner2Classifier = owner2.getClassifier();
            if (owner1Classifier != owner2Classifier)
            {
                int classifierCmp = PackageableElement.getUserPathForPackageableElement(owner1Classifier).compareTo(PackageableElement.getUserPathForPackageableElement(owner2Classifier));
                if (classifierCmp != 0)
                {
                    return classifierCmp;
                }
            }

            int sourceInfoCmp = Comparators.nullSafeCompare(owner1.getSourceInformation(), owner2.getSourceInformation());
            if (sourceInfoCmp != 0)
            {
                return sourceInfoCmp;
            }
        }

        String propertyName1 = PrimitiveUtilities.getStringValue(referenceUsage1.getValueForMetaPropertyToOne(M3Properties.propertyName));
        String propertyName2 = PrimitiveUtilities.getStringValue(referenceUsage2.getValueForMetaPropertyToOne(M3Properties.propertyName));
        if (propertyName1 != propertyName2) //NOSONAR pointer equality check for possible fast success before doing a more expensive String comparison
        {
            int propertyNameCmp = Comparators.nullSafeCompare(propertyName1, propertyName2);
            if (propertyNameCmp != 0)
            {
                return propertyNameCmp;
            }
        }

        int offset1 = PrimitiveUtilities.getIntegerValue(referenceUsage1.getValueForMetaPropertyToOne(M3Properties.offset)).intValue();
        int offset2 = PrimitiveUtilities.getIntegerValue(referenceUsage2.getValueForMetaPropertyToOne(M3Properties.offset)).intValue();
        return Integer.compare(offset1, offset2);
    }

    /**
     * Return whether reference has a reference usage with the given owner,
     * property name, and offset.
     *
     * @param reference    instance with reference usages to search
     * @param owner        reference usage owner
     * @param propertyName reference usage property name
     * @param offset       reference usage offset
     * @return whether reference has an appropriate reference usage
     */
    public static boolean hasReferenceUsage(CoreInstance reference, CoreInstance owner, String propertyName, int offset)
    {
        // TODO we should be able to use an ID index here, but there are cases of duplicate reference usages.
        ReferenceUsageIndexKey key = new ReferenceUsageIndexKey(owner, propertyName, offset);
        return reference.getValueInValueForMetaPropertyToManyByIndex(M3Properties.referenceUsages, REF_USAGE_INDEX_SPEC, key).notEmpty();
    }

    public static String printReferenceUsage(CoreInstance referenceUsage)
    {
        return printReferenceUsage(referenceUsage, false);
    }

    public static String printReferenceUsage(CoreInstance referenceUsage, boolean includeOwnerSourceInfo)
    {
        return printReferenceUsage(referenceUsage, includeOwnerSourceInfo, false);
    }

    public static String printReferenceUsage(CoreInstance referenceUsage, boolean includeOwnerSourceInfo, boolean includeName)
    {
        StringBuilder builder = new StringBuilder(96);
        writeReferenceUsage(builder, referenceUsage, includeOwnerSourceInfo, includeName);
        return builder.toString();
    }

    public static void writeReferenceUsage(StringBuilder builder, CoreInstance referenceUsage)
    {
        writeReferenceUsage(builder, referenceUsage, false);
    }

    public static void writeReferenceUsage(StringBuilder builder, CoreInstance referenceUsage, boolean includeOwnerSourceInfo)
    {
        writeReferenceUsage(builder, referenceUsage, includeOwnerSourceInfo, false);
    }

    public static void writeReferenceUsage(StringBuilder builder, CoreInstance referenceUsage, boolean includeOwnerSourceInfo, boolean includeName)
    {
        builder.append("<ReferenceUsage owner=");
        CoreInstance owner = referenceUsage.getValueForMetaPropertyToOne(M3Properties.owner);
        builder.append(owner);
        if (includeOwnerSourceInfo && (owner != null))
        {
            SourceInformation sourceInfo = owner.getSourceInformation();
            if (sourceInfo != null)
            {
                sourceInfo.appendMessage(builder.append(" (")).append(')');
            }
        }
        builder.append(", propertyName='").append(PrimitiveUtilities.getStringValue(referenceUsage.getValueForMetaPropertyToOne(M3Properties.propertyName), null));
        builder.append("', offset=").append(PrimitiveUtilities.getIntegerValue(referenceUsage.getValueForMetaPropertyToOne(M3Properties.offset), (Integer) null));
        if (includeName)
        {
            builder.append(", name='").append(referenceUsage.getName()).append('\'');
        }
        builder.append('>');
    }

    private static class ReferenceUsageIndexKey
    {
        private final CoreInstance owner;
        private final String propertyName;
        private final int offset;

        private ReferenceUsageIndexKey(CoreInstance owner, String propertyName, int offset)
        {
            this.owner = owner;
            this.propertyName = propertyName;
            this.offset = offset;
        }

        private ReferenceUsageIndexKey(CoreInstance referenceUsage)
        {
            this(referenceUsage.getValueForMetaPropertyToOne(M3Properties.owner), PrimitiveUtilities.getStringValue(referenceUsage.getValueForMetaPropertyToOne(M3Properties.propertyName)), PrimitiveUtilities.getIntegerValue(referenceUsage.getValueForMetaPropertyToOne(M3Properties.offset)).intValue());
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }

            if (!(other instanceof ReferenceUsageIndexKey))
            {
                return false;
            }

            ReferenceUsageIndexKey that = (ReferenceUsageIndexKey) other;
            return (this.owner == that.owner) && (this.offset == that.offset) && this.propertyName.equals(that.propertyName);
        }

        @Override
        public int hashCode()
        {
            int hashCode = this.owner.hashCode();
            hashCode = 31 * hashCode + this.propertyName.hashCode();
            hashCode = 31 * hashCode + this.offset;
            return hashCode;
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder(64);
            builder.append("<ReferenceUsageIndexKey owner='").append(this.owner).append("' ");
            SourceInformation ownerSourceInfo = this.owner.getSourceInformation();
            if (ownerSourceInfo != null)
            {
                ownerSourceInfo.appendMessage(builder.append('(')).append(") ");
            }
            builder.append("propertyName='").append(this.propertyName);
            builder.append("' offset=").append(this.offset).append('>');
            return builder.toString();
        }
    }
}
