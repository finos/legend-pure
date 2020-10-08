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


public class PureRuntimeStatusPair implements PureRuntimeStatus
{
    private final PureRuntimeStatus first;
    private final PureRuntimeStatus second;

    public PureRuntimeStatusPair(PureRuntimeStatus first, PureRuntimeStatus second)
    {
        this.first = first;
        this.second = second;
    }

    @Override
    public void startRuntimeInitialization()
    {
        this.first.startRuntimeInitialization();
        this.second.startRuntimeInitialization();
    }

    @Override
    public void finishRuntimeInitialization()
    {
        this.first.finishRuntimeInitialization();
        this.second.finishRuntimeInitialization();
    }

    @Override
    public void startLoadingAndCompilingCore()
    {
        this.first.startLoadingAndCompilingCore();
        this.second.startLoadingAndCompilingCore();
    }

    @Override
    public void finishedLoadingAndCompilingCore()
    {
        this.first.finishedLoadingAndCompilingCore();
        this.second.finishedLoadingAndCompilingCore();
    }

    @Override
    public void startLoadingAndCompilingSystemFiles()
    {
        this.first.startLoadingAndCompilingSystemFiles();
        this.second.startLoadingAndCompilingSystemFiles();
    }

    @Override
    public void finishedLoadingAndCompilingSystemFiles()
    {
        this.first.finishedLoadingAndCompilingSystemFiles();
        this.second.finishedLoadingAndCompilingSystemFiles();
    }

    @Override
    public void createOrUpdateMemorySource(String id, String content)
    {
        this.first.createOrUpdateMemorySource(id, content);
        this.second.createOrUpdateMemorySource(id, content);
    }

    @Override
    public void modifySource(String sourceId, String code)
    {
        this.first.modifySource(sourceId, code);
        this.second.modifySource(sourceId, code);
    }

    @Override
    public void deleteSource(String sourceId)
    {
        this.first.deleteSource(sourceId);
        this.second.deleteSource(sourceId);
    }

    @Override
    public void moveSource(String sourceId, String destinationId)
    {
        this.first.moveSource(sourceId, destinationId);
        this.second.moveSource(sourceId, destinationId);
    }
}
