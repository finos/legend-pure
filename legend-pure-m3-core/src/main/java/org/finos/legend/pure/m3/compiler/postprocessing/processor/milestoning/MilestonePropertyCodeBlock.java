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

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class MilestonePropertyCodeBlock
{
    private final MilestonePropertyHolderType milestonePropertyHolderType;
    private final String codeBlock;
    private final SourceInformation propertyGenericTypeSourceInformation;
    private final SourceInformation propertySourceInformation;
    private final CoreInstance sourceProperty;

    enum MilestonePropertyHolderType
    {
        REGULAR, QUALIFIED
    }

    MilestonePropertyCodeBlock(MilestonePropertyHolderType milestonePropertyHolderType, String codeBlock, CoreInstance sourceProperty, SourceInformation propertySourceInformation, SourceInformation propertyGenericTypeSourceInformation)
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

    ListIterable<CoreInstance> getNonMilestonedStereotypes(ProcessorSupport processorSupport){
        MutableList<CoreInstance> stereotypes = FastList.newList();
        if(this.sourceProperty != null)
        {
            ListIterable<? extends CoreInstance> sourceStereotypes = ImportStub.withImportStubByPasses(((AbstractProperty)sourceProperty)._stereotypesCoreInstance().toList(),  processorSupport);
            if(sourceStereotypes != null && !sourceStereotypes.isEmpty()){
                stereotypes.addAll(sourceStereotypes.select(Predicates.not(new Predicate<CoreInstance>()
                {
                    @Override
                    public boolean accept(CoreInstance stereotype)
                    {
                        return CoreInstance.GET_NAME.valueOf(stereotype).equals(MilestoningFunctions.GENERATED_MILESTONING_STEREOTYPE_VALUE) || CoreInstance.GET_NAME.valueOf(stereotype).equals(MilestoningFunctions.GENERATED_MILESTONING_DATE_STEREOTYPE_VALUE);
                    }
                })).toList());
            }
        }
        return stereotypes;
    }

    ListIterable<CoreInstance> getTaggedValues(ProcessorSupport processorSupport){
        MutableList<CoreInstance> taggedValues = FastList.newList();
        if(sourceProperty != null)
        {
            ListIterable<? extends CoreInstance> sourceTaggedValues = ImportStub.withImportStubByPasses(((AbstractProperty)sourceProperty)._taggedValues().toList(), processorSupport);
            if (sourceTaggedValues != null)
            {
                taggedValues.addAll(sourceTaggedValues.toList());
            }
        }
        return taggedValues;
    }

    CoreInstance getSourceProperty(){
        return sourceProperty;
    }
}
