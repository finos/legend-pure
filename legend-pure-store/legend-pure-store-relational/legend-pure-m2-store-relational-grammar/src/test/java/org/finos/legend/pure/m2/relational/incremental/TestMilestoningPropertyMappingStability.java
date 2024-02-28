// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.m2.relational.incremental;

import org.finos.legend.pure.m2.relational.AbstractPureRelationalTestWithCoreCompiled;
import org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.Test;

public class TestMilestoningPropertyMappingStability extends AbstractPureRelationalTestWithCoreCompiled
{
    @Test
    public void testStabilityOnTemporalStereotypeUpdate()
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.MILESTONED_STORE_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.MAPPING_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.PROCESSING_MILESTONING_MODEL_CODE)
                        .compile()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnTemporalStereotypeRemoval()
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.MILESTONED_STORE_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.MAPPING_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.NON_MILESTONING_MODEL_CODE)
                        .compile()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnStoreUpdate()
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.MILESTONED_STORE_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.MAPPING_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.UPDATED_MILESTONED_STORE_CODE)
                        .compile()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.MILESTONED_STORE_CODE)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnMappingUpdate()
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.MILESTONED_STORE_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.MAPPING_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.UPDATED_MAPPING_CODE)
                        .compile()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.MAPPING_CODE)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnTemporalStereotypeUpdateWithNonMilestonedTable()
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.NON_MILESTONED_STORE_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.MAPPING_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.PROCESSING_MILESTONING_MODEL_CODE)
                        .compile()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnTemporalStereotypeRemovalWithNonMilestonedTable()
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.NON_MILESTONED_STORE_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.MAPPING_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.NON_MILESTONING_MODEL_CODE)
                        .compile()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnStoreUpdateWithNonMilestonedTable()
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.NON_MILESTONED_STORE_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.MAPPING_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.MILESTONED_STORE_CODE)
                        .compile()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.NON_MILESTONED_STORE_CODE)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnMappingUpdateWithNonMilestonedTable()
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.NON_MILESTONED_STORE_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.MAPPING_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.UPDATED_MAPPING_CODE)
                        .compile()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.MAPPING_CODE)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testEmbeddedStabilityOnTemporalStereotypeUpdate()
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MODEL_ID, MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.MILESTONED_STORE_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MAPPING_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.PROCESSING_MILESTONING_MODEL_CODE)
                        .compile()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testEmbeddedStabilityOnTemporalStereotypeRemoval()
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MODEL_ID, MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.MILESTONED_STORE_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MAPPING_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.NON_MILESTONING_MODEL_CODE)
                        .compile()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testEmbeddedStabilityOnStoreUpdate()
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MODEL_ID, MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.MILESTONED_STORE_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MAPPING_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.UPDATED_MILESTONED_STORE_CODE)
                        .compile()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.MILESTONED_STORE_CODE)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testEmbeddedStabilityOnMappingUpdate()
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MODEL_ID, MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.MILESTONED_STORE_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MAPPING_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.UPDATED_EMBEDDED_MAPPING_CODE)
                        .compile()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MAPPING_CODE)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testInlineEmbeddedStabilityOnTemporalStereotypeUpdate()
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MODEL_ID, MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.MILESTONED_STORE_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.INLINE_EMBEDDED_MAPPING_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.PROCESSING_MILESTONING_MODEL_CODE)
                        .compile()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testInlineEmbeddedStabilityOnTemporalStereotypeRemoval()
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MODEL_ID, MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.MILESTONED_STORE_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.INLINE_EMBEDDED_MAPPING_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.NON_MILESTONING_MODEL_CODE)
                        .compile()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testInlineEmbeddedStabilityOnStoreUpdate()
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MODEL_ID, MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.MILESTONED_STORE_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.INLINE_EMBEDDED_MAPPING_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.UPDATED_MILESTONED_STORE_CODE)
                        .compile()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.MILESTONED_STORE_CODE)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testInlineEmbeddedStabilityOnMappingUpdate()
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MODEL_ID, MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MODEL_ID, MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MODEL_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.STORE_ID, MilestoningPropertyMappingTestSourceCodes.MILESTONED_STORE_CODE)
                        .createInMemorySource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.INLINE_EMBEDDED_MAPPING_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.UPDATED_INLINE_EMBEDDED_MAPPING_CODE)
                        .compile()
                        .updateSource(MilestoningPropertyMappingTestSourceCodes.MAPPING_ID, MilestoningPropertyMappingTestSourceCodes.INLINE_EMBEDDED_MAPPING_CODE)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }
}
