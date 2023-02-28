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

package org.finos.legend.pure.runtime.java.compiled.extension;

import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaSourceCodeGenerator;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.Procedure3;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.Procedure4;

import java.util.List;
import java.util.function.Supplier;

public abstract class BaseCompiledExtension implements CompiledExtension
{
    private final List<StringJavaSource> extraJavaSources;
    private final Supplier<? extends List<Native>> extraNatives;
    private final List<Procedure3<CoreInstance, JavaSourceCodeGenerator, ProcessorContext>> extraPackageableElementProcessors;
    private final List<Procedure4<CoreInstance, CoreInstance, ProcessorContext, ProcessorSupport>> extraClassMappingProcessors;

    private final String relatedRepository;

    protected BaseCompiledExtension(String relatedRepository, Supplier<? extends List<Native>> extraNatives, List<StringJavaSource> extraJavaSources, List<Procedure3<CoreInstance, JavaSourceCodeGenerator, ProcessorContext>> extraPackageableElementProcessors, List<Procedure4<CoreInstance, CoreInstance, ProcessorContext, ProcessorSupport>> extraClassMappingProcessors)
    {
        this.extraNatives = extraNatives;
        this.extraJavaSources = extraJavaSources;
        this.extraPackageableElementProcessors = extraPackageableElementProcessors;
        this.extraClassMappingProcessors = extraClassMappingProcessors;
        this.relatedRepository = relatedRepository;
    }

    @Override
    public List<StringJavaSource> getExtraJavaSources()
    {
        return this.extraJavaSources;
    }

    @Override
    public List<Native> getExtraNatives()
    {
        return this.extraNatives.get();
    }

    @Override
    public List<Procedure3<CoreInstance, JavaSourceCodeGenerator, ProcessorContext>> getExtraPackageableElementProcessors()
    {
        return this.extraPackageableElementProcessors;
    }

    @Override
    public List<Procedure4<CoreInstance, CoreInstance, ProcessorContext, ProcessorSupport>> getExtraClassMappingProcessors()
    {
        return this.extraClassMappingProcessors;
    }

    @Override
    public String getRelatedRepository()
    {
        return this.relatedRepository;
    }
}
