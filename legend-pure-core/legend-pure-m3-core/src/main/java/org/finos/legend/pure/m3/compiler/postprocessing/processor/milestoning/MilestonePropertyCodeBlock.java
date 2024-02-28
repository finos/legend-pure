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

package org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.TaggedValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class MilestonePropertyCodeBlock
{
    private final MilestonePropertyHolderType milestonePropertyHolderType;
    private final String codeBlock;
    private final SourceInformation propertyGenericTypeSourceInformation;
    private final SourceInformation propertySourceInformation;
    private final AbstractProperty<?> sourceProperty;

    enum MilestonePropertyHolderType
    {
        REGULAR, QUALIFIED
    }

    MilestonePropertyCodeBlock(MilestonePropertyHolderType milestonePropertyHolderType, String codeBlock, AbstractProperty<?> sourceProperty, SourceInformation propertySourceInformation, SourceInformation propertyGenericTypeSourceInformation)
    {
        this.milestonePropertyHolderType = milestonePropertyHolderType;
        this.codeBlock = codeBlock;
        this.propertySourceInformation = propertySourceInformation;
        this.propertyGenericTypeSourceInformation = propertyGenericTypeSourceInformation;
        this.sourceProperty = sourceProperty;
    }

    MilestonePropertyCodeBlock(MilestonePropertyHolderType milestonePropertyHolderType, String codeBlock, SourceInformation propertySourceInformation)
    {
        this(milestonePropertyHolderType, codeBlock, null, propertySourceInformation, null);
    }

    boolean isPropertyGenericTypeSourceInformationIsAvailable()
    {
        return this.propertyGenericTypeSourceInformation != null;
    }

    SourceInformation getPropertyGenericTypeSourceInformation()
    {
        return this.propertyGenericTypeSourceInformation;
    }

    SourceInformation getPropertySourceInformation()
    {
        return this.propertySourceInformation;
    }

    String getCodeBlock()
    {
        return this.codeBlock;
    }

    RichIterable<? extends Stereotype> getNonMilestonedStereotypes(ProcessorSupport processorSupport)
    {
        if (this.sourceProperty == null)
        {
            return Lists.immutable.empty();
        }
        return this.sourceProperty._stereotypesCoreInstance().asLazy()
                .collect(st -> (Stereotype) ImportStub.withImportStubByPass(st, processorSupport))
                .reject(this::isMilestoningStereotype, Lists.mutable.empty());
    }

    private boolean isMilestoningStereotype(Stereotype stereotype)
    {
        return MilestoningFunctions.GENERATED_MILESTONING_DATE_STEREOTYPE.equals(stereotype._value()) || MilestoningFunctions.GENERATED_MILESTONING_DATE_STEREOTYPE_VALUE.equals(stereotype._value());
    }

    RichIterable<? extends TaggedValue> getTaggedValues()
    {
        return (this.sourceProperty == null) ? Lists.immutable.empty() : this.sourceProperty._taggedValues();
    }

    AbstractProperty<?> getSourceProperty()
    {
        return this.sourceProperty;
    }
}
