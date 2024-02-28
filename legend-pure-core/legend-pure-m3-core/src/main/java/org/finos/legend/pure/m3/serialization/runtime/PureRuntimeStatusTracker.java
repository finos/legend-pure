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

/**
 * Tracks the states in the runtime
 */
public class PureRuntimeStatusTracker implements PureRuntimeStatus
{
    private boolean compiling = false;


    @Override
    public void startRuntimeInitialization()
    {

    }

    @Override
    public void finishRuntimeInitialization()
    {

    }

    @Override
    public void startLoadingAndCompilingCore()
    {
        this.compiling = true;
    }

    @Override
    public void finishedLoadingAndCompilingCore()
    {
        this.compiling = false;
    }

    @Override
    public void startLoadingAndCompilingSystemFiles()
    {
        this.compiling = true;
    }

    @Override
    public void finishedLoadingAndCompilingSystemFiles()
    {
        this.compiling = false;
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

    public boolean isCompiling()
    {
        return this.compiling;
    }
}
