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

package org.finos.legend.pure.m3.serialization.runtime;

public class VoidPureRuntimeStatus implements PureRuntimeStatus
{
    public static final VoidPureRuntimeStatus VOID_PURE_RUNTIME_STATUS = new VoidPureRuntimeStatus();

    private VoidPureRuntimeStatus()
    {
        // Singleton
    }

    @Override
    public void startLoadingAndCompilingCore()
    {
    }

    @Override
    public void finishedLoadingAndCompilingCore()
    {
    }

    @Override
    public void startRuntimeInitialization()
    {
    }

    @Override
    public void finishRuntimeInitialization()
    {
    }

    @Override
    public void startLoadingAndCompilingSystemFiles()
    {
    }

    @Override
    public void finishedLoadingAndCompilingSystemFiles()
    {
    }

    @Override
    public void createOrUpdateMemorySource(String id, String content)
    {
    }

    @Override
    public void modifySource(String sourceId, String code)
    {
    }

    @Override
    public void deleteSource(String sourceId)
    {
    }

    @Override
    public void moveSource(String sourceId, String destinationId)
    {
    }
}
