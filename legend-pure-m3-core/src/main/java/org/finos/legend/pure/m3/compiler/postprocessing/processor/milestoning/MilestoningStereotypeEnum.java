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

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestonePropertyCodeBlock.MilestonePropertyHolderType;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public enum MilestoningStereotypeEnum implements MilestoningStereotype
{
    businesstemporal
            {
                @Override
                public ListIterable<String> getTemporalDatePropertyNames()
                {
                    return FastList.newListWith("businessDate");
                }

                @Override
                public MutableList<String> getMilestoningPropertyNames()
                {
                    return Lists.mutable.of("from", "thru");
                }

                @Override
                public String getMilestoningPropertyClassName()
                {
                    return "meta::pure::milestoning::BusinessDateMilestoning";
                }

                @Override
                public String getPurePlatformStereotypeName()
                {
                    return this.name();
                }

                @Override
                public int positionInTemporalParameterValues()
                {
                    return 1;
                }

                @Override
                public ListIterable<MilestonePropertyCodeBlock> getQualifiedPropertyCodeBlocks(ListIterable<MilestoningStereotypeEnum> ownerMilestoneStereotypes, CoreInstance property, String multiplicity, String returnType)
                {
                    return getSingleDateQualifiedPropertyCodeBlocks(ownerMilestoneStereotypes, this, property, multiplicity, returnType);
                }
            },

    processingtemporal
            {
                @Override
                public ListIterable<String> getTemporalDatePropertyNames()
                {
                    return FastList.newListWith("processingDate");
                }

                @Override
                public MutableList<String> getMilestoningPropertyNames()
                {
                    return Lists.mutable.of("in", "out");
                }

                @Override
                public String getMilestoningPropertyClassName()
                {
                    return "meta::pure::milestoning::ProcessingDateMilestoning";
                }

                @Override
                public String getPurePlatformStereotypeName()
                {
                    return this.name();
                }

                @Override
                public int positionInTemporalParameterValues()
                {
                    return 0;
                }

                @Override
                public ListIterable<MilestonePropertyCodeBlock> getQualifiedPropertyCodeBlocks(ListIterable<MilestoningStereotypeEnum> ownerMilestoneStereotypes, CoreInstance property, String multiplicity, String returnType)
                {
                    return getSingleDateQualifiedPropertyCodeBlocks(ownerMilestoneStereotypes, this, property, multiplicity, returnType);
                }
            },

    bitemporal
            {
                @Override
                public ListIterable<String> getTemporalDatePropertyNames()
                {
                    return LazyIterate.concatenate(processingtemporal.getTemporalDatePropertyNames(), businesstemporal.getTemporalDatePropertyNames()).toList();
                }

                @Override
                public MutableList<String> getMilestoningPropertyNames()
                {
                    return processingtemporal.getMilestoningPropertyNames().withAll(businesstemporal.getMilestoningPropertyNames());
                }

                @Override
                public String getMilestoningPropertyClassName()
                {
                    return "meta::pure::milestoning::BiTemporalMilestoning";
                }

                @Override
                public String getPurePlatformStereotypeName()
                {
                    return this.name();
                }

                @Override
                public int positionInTemporalParameterValues()
                {
                    throw new UnsupportedOperationException("bitemporal specifys two dates, position information is not appropriate");
                }

                @Override
                public ListIterable<MilestonePropertyCodeBlock> getQualifiedPropertyCodeBlocks(ListIterable<MilestoningStereotypeEnum> ownerMilestoneStereotypes, CoreInstance property, String multiplicity, String returnType)
                {
                    MilestoningStereotypeEnum sourceEnum = ownerMilestoneStereotypes.distinct().getFirst();
                    if (sourceEnum == null)
                    {
                        return FastList.newListWith(getBiTemporalMilestoningPropertyCodeBlockWithParams(new MilestoningPropertyCodeBlockMetaData(property, returnType, multiplicity)));
                    }
                    else if (sourceEnum == bitemporal)
                    {
                        return FastList.newListWith(getBiTemporalMilestoningPropertyCodeBlockWithParams(new MilestoningPropertyCodeBlockMetaData(property, returnType, multiplicity)),
                                getBiTemporalMilestoningPropertyCodeBlockWithOneTemporalDateParam(sourceEnum, new MilestoningPropertyCodeBlockMetaData(property, returnType, multiplicity)), getBiTemporalMilestoningPropertyCodeBlockNoParams(new MilestoningPropertyCodeBlockMetaData(property, returnType, multiplicity)));
                    }
                    else if (sourceEnum == businesstemporal || sourceEnum == processingtemporal)
                    {
                        return FastList.newListWith(getBiTemporalMilestoningPropertyCodeBlockWithParams(new MilestoningPropertyCodeBlockMetaData(property, returnType, multiplicity)),
                                getBiTemporalMilestoningPropertyCodeBlockWithOneTemporalDateParam(sourceEnum, new MilestoningPropertyCodeBlockMetaData(property, returnType, multiplicity)));
                    }
                    else
                    {
                        throw new PureCompilationException("Unexpected source milestoning state: " + sourceEnum);
                    }
                }
            };

    @Override
    public ListIterable<MilestonePropertyCodeBlock> getDatePropertyCodeBlocks(final SourceInformation sourceInformation)
    {
        return Lists.mutable.withAll(this.getTemporalDatePropertyNames().collectWith(DATE_PROPERTY_CODE_BLOCK, sourceInformation)).with(getMilestoningDataPropertyCodeBlock(this.getMilestoningPropertyClassName(), sourceInformation));
    }

    private static final Function2<String, SourceInformation, MilestonePropertyCodeBlock> DATE_PROPERTY_CODE_BLOCK = new Function2<String, SourceInformation, MilestonePropertyCodeBlock>()
    {
        @Override
        public MilestonePropertyCodeBlock value(String temporalDatePropertyName, SourceInformation sourceInformation)
        {
            String propertyTemplate = MilestoningFunctions.GENERATED_MILESTONING_DATE_STEREOTYPE + " %s: Date[1];\n";
            String codeBlock = String.format(propertyTemplate, temporalDatePropertyName);
            return new MilestonePropertyCodeBlock(MilestonePropertyHolderType.REGULAR, codeBlock, sourceInformation);
        }
    };

    private static MilestonePropertyCodeBlock getMilestoningDataPropertyCodeBlock(String propertyClassName, SourceInformation sourceInformation)
    {
        String propertyTemplate = MilestoningFunctions.GENERATED_MILESTONING_DATE_STEREOTYPE + " " + MilestoningFunctions.MILESTONING + " : %s[0..1];\n";
        String codeBlock = String.format(propertyTemplate, propertyClassName);
        return new MilestonePropertyCodeBlock(MilestonePropertyHolderType.REGULAR, codeBlock, sourceInformation);
    }

    private static ListIterable<MilestonePropertyCodeBlock> getSingleDateQualifiedPropertyCodeBlocks(ListIterable<MilestoningStereotypeEnum> ownerMilestoneStereotypes, MilestoningStereotypeEnum targetMilestoneingStereotype, CoreInstance property, String multiplicity, String returnType)
    {
        FastList<MilestonePropertyCodeBlock> qualifiedCodeBlocks = FastList.newList();
        MilestoningStereotypeEnum ownerMilestoningStereotype = ownerMilestoneStereotypes.getFirst();
        MilestoningPropertyCodeBlockMetaData milestoningPropertyCodeBlockMetaData = new MilestoningPropertyCodeBlockMetaData(property, returnType, multiplicity);

        if (ownerMilestoningStereotype == targetMilestoneingStereotype || ownerMilestoningStereotype == bitemporal)
        {
            qualifiedCodeBlocks.add(getSingleTemporalMilestoningPropertyCodeBlockNoParam(targetMilestoneingStereotype, milestoningPropertyCodeBlockMetaData));
        }
        qualifiedCodeBlocks.add(getSingleTemporalMilestoningPropertyCodeBlockWithParam(targetMilestoneingStereotype, milestoningPropertyCodeBlockMetaData));
        qualifiedCodeBlocks.add(getSingleDateQualifiedRangePropertyCodeBlock(targetMilestoneingStereotype, milestoningPropertyCodeBlockMetaData));

        return qualifiedCodeBlocks;
    }

    private static MilestonePropertyCodeBlock getSingleTemporalMilestoningPropertyCodeBlockWithParam(MilestoningStereotypeEnum milestoningStereotype, MilestoningPropertyCodeBlockMetaData milestoningPropertyCodeBlockMetaData)
    {
        String functionTemplateWithArg = MilestoningFunctions.GENERATED_MILESTONING_STEREOTYPE + "%s(td:Date[1]){$this.%s->filter(%s| $%s.%s->eq($td))%s } : %s[%s];";
        String temporalDatePropertyName = milestoningStereotype.getTemporalDatePropertyNames().getFirst();
        String function = String.format(functionTemplateWithArg, milestoningPropertyCodeBlockMetaData.propertyName, milestoningPropertyCodeBlockMetaData.edgepointPropertyName, milestoningPropertyCodeBlockMetaData.varName, milestoningPropertyCodeBlockMetaData.varName, temporalDatePropertyName, milestoningPropertyCodeBlockMetaData.multiplicityFunctionCall, milestoningPropertyCodeBlockMetaData.returnType, milestoningPropertyCodeBlockMetaData.possiblyOverridenMultiplicity);
        return new MilestonePropertyCodeBlock(MilestonePropertyHolderType.QUALIFIED, function, milestoningPropertyCodeBlockMetaData.property, milestoningPropertyCodeBlockMetaData.propertySourceInformation, milestoningPropertyCodeBlockMetaData.propertyGenericTypeSourceInformation);
    }

    private static MilestonePropertyCodeBlock getSingleTemporalMilestoningPropertyCodeBlockNoParam(MilestoningStereotypeEnum milestoningStereotype, MilestoningPropertyCodeBlockMetaData milestoningPropertyCodeBlockMetaData)
    {
        String functionTemplateWithArg = MilestoningFunctions.GENERATED_MILESTONING_STEREOTYPE + "%s(){$this.%s->filter(%s| $%s.%s->eq($this.%s))%s } : %s[%s];";
        String temporalDatePropertyName = milestoningStereotype.getTemporalDatePropertyNames().getFirst();
        String function = String.format(functionTemplateWithArg, milestoningPropertyCodeBlockMetaData.propertyName, milestoningPropertyCodeBlockMetaData.edgepointPropertyName, milestoningPropertyCodeBlockMetaData.varName, milestoningPropertyCodeBlockMetaData.varName, temporalDatePropertyName, temporalDatePropertyName, milestoningPropertyCodeBlockMetaData.multiplicityFunctionCall, milestoningPropertyCodeBlockMetaData.returnType, milestoningPropertyCodeBlockMetaData.possiblyOverridenMultiplicity);
        return new MilestonePropertyCodeBlock(MilestonePropertyHolderType.QUALIFIED, function, milestoningPropertyCodeBlockMetaData.property, milestoningPropertyCodeBlockMetaData.propertySourceInformation, milestoningPropertyCodeBlockMetaData.propertyGenericTypeSourceInformation);
    }

    private static MilestonePropertyCodeBlock getSingleDateQualifiedRangePropertyCodeBlock(MilestoningStereotypeEnum milestoningStereotype, MilestoningPropertyCodeBlockMetaData milestoningPropertyCodeBlockMetaData)
    {
        String functionTemplate = MilestoningFunctions.GENERATED_MILESTONING_STEREOTYPE + "%s(start:Date[1], end:Date[1]){$this.%s->filter(%s| $%s.%s->eq($start))%s } : %s[%s];";
        String temporalDatePropertyName = milestoningStereotype.getTemporalDatePropertyNames().getFirst();
        String function = String.format(functionTemplate, milestoningPropertyCodeBlockMetaData.rangePropertyName, milestoningPropertyCodeBlockMetaData.edgepointPropertyName, milestoningPropertyCodeBlockMetaData.varName, milestoningPropertyCodeBlockMetaData.varName, temporalDatePropertyName, milestoningPropertyCodeBlockMetaData.multiplicityFunctionCall, milestoningPropertyCodeBlockMetaData.returnType, milestoningPropertyCodeBlockMetaData.possiblyOverridenMultiplicity);
        return new MilestonePropertyCodeBlock(MilestonePropertyHolderType.QUALIFIED, function, milestoningPropertyCodeBlockMetaData.property, milestoningPropertyCodeBlockMetaData.propertySourceInformation, milestoningPropertyCodeBlockMetaData.propertyGenericTypeSourceInformation);
    }

    private static MilestonePropertyCodeBlock getBiTemporalMilestoningPropertyCodeBlockNoParams(MilestoningPropertyCodeBlockMetaData milestoningPropertyCodeBlockMetaData)
    {
        String functionTemplateWithArg = MilestoningFunctions.GENERATED_MILESTONING_STEREOTYPE + "%s(){$this.%s->filter(%s| $%s.processingDate->eq($this.processingDate) && $%s.businessDate->eq($this.businessDate) )%s } : %s[%s];";
        String function = String.format(functionTemplateWithArg, milestoningPropertyCodeBlockMetaData.propertyName, milestoningPropertyCodeBlockMetaData.edgepointPropertyName, milestoningPropertyCodeBlockMetaData.varName, milestoningPropertyCodeBlockMetaData.varName, milestoningPropertyCodeBlockMetaData.varName, milestoningPropertyCodeBlockMetaData.multiplicityFunctionCall, milestoningPropertyCodeBlockMetaData.returnType, milestoningPropertyCodeBlockMetaData.possiblyOverridenMultiplicity);
        return new MilestonePropertyCodeBlock(MilestonePropertyHolderType.QUALIFIED, function, milestoningPropertyCodeBlockMetaData.property, milestoningPropertyCodeBlockMetaData.propertySourceInformation, milestoningPropertyCodeBlockMetaData.propertyGenericTypeSourceInformation);
    }

    private static MilestonePropertyCodeBlock getBiTemporalMilestoningPropertyCodeBlockWithOneTemporalDateParam(MilestoningStereotypeEnum availableTemporalDate, MilestoningPropertyCodeBlockMetaData milestoningPropertyCodeBlockMetaData)
    {
        String sourceProcessingDate = availableTemporalDate == processingtemporal ? "$this.processingDate" : "$td";
        String sourceBusinessDate = availableTemporalDate == businesstemporal ? "$this.businessDate" : "$td";
        String functionTemplateWithArg = MilestoningFunctions.GENERATED_MILESTONING_STEREOTYPE + "%s(td:Date[1]){$this.%s->filter(%s| $%s.processingDate->eq(%s) && $%s.businessDate->eq(%s) )%s } : %s[%s];";
        String function = String.format(functionTemplateWithArg, milestoningPropertyCodeBlockMetaData.propertyName, milestoningPropertyCodeBlockMetaData.edgepointPropertyName, milestoningPropertyCodeBlockMetaData.varName, milestoningPropertyCodeBlockMetaData.varName, sourceProcessingDate, milestoningPropertyCodeBlockMetaData.varName, sourceBusinessDate, milestoningPropertyCodeBlockMetaData.multiplicityFunctionCall, milestoningPropertyCodeBlockMetaData.returnType, milestoningPropertyCodeBlockMetaData.possiblyOverridenMultiplicity);
        return new MilestonePropertyCodeBlock(MilestonePropertyHolderType.QUALIFIED, function, milestoningPropertyCodeBlockMetaData.property, milestoningPropertyCodeBlockMetaData.propertySourceInformation, milestoningPropertyCodeBlockMetaData.propertyGenericTypeSourceInformation);
    }

    private static MilestonePropertyCodeBlock getBiTemporalMilestoningPropertyCodeBlockWithParams(MilestoningPropertyCodeBlockMetaData milestoningPropertyCodeBlockMetaData)
    {
        String functionTemplateWithArg = MilestoningFunctions.GENERATED_MILESTONING_STEREOTYPE + "%s(pd:Date[1], bd:Date[1]){$this.%s->filter(%s| $%s.processingDate->eq($pd) && $%s.businessDate->eq($bd) )%s } : %s[%s];";
        String function = String.format(functionTemplateWithArg, milestoningPropertyCodeBlockMetaData.propertyName, milestoningPropertyCodeBlockMetaData.edgepointPropertyName, milestoningPropertyCodeBlockMetaData.varName, milestoningPropertyCodeBlockMetaData.varName, milestoningPropertyCodeBlockMetaData.varName, milestoningPropertyCodeBlockMetaData.multiplicityFunctionCall, milestoningPropertyCodeBlockMetaData.returnType, milestoningPropertyCodeBlockMetaData.possiblyOverridenMultiplicity);
        return new MilestonePropertyCodeBlock(MilestonePropertyHolderType.QUALIFIED, function, milestoningPropertyCodeBlockMetaData.property, milestoningPropertyCodeBlockMetaData.propertySourceInformation, milestoningPropertyCodeBlockMetaData.propertyGenericTypeSourceInformation);
    }
}