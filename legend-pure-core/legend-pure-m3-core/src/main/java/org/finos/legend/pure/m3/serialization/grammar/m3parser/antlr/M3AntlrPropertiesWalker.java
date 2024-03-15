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

package org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.PropertiesContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;

public class M3AntlrPropertiesWalker extends org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3ParserBaseVisitor<Void>
{

    private final ImportGroup importId;

    private final AntlrContextToM3CoreInstance antlrContextToM3Builder;

    private final MutableList<Property<? extends CoreInstance, ?>> properties;

    private final MutableList<QualifiedProperty<? extends CoreInstance>> qualifiedProperties;

    private final ImportStub typeOwner;

    private final int startingQualifiedPropertyIndex;

    public M3AntlrPropertiesWalker(AntlrSourceInformation antlrSourceInformation, InlineDSLLibrary inlineDSLLibrary, ModelRepository repository, Context context, ImportGroup importId, MutableList<Property<? extends CoreInstance, ?>> properties, MutableList<QualifiedProperty<? extends CoreInstance>> qualifiedProperties, ImportStub typeOwner, int startingQualifiedPropertyIndex)
    {
        this.startingQualifiedPropertyIndex = startingQualifiedPropertyIndex;
        this.antlrContextToM3Builder = new AntlrContextToM3CoreInstance(context, repository, new M3ProcessorSupport(context, repository), antlrSourceInformation, inlineDSLLibrary, Lists.mutable.<CoreInstance>empty(), 0, true, null);
        this.importId = importId;
        this.properties = properties;
        this.qualifiedProperties = qualifiedProperties;
        this.typeOwner = typeOwner;
    }

    @Override
    public Void visitProperties(PropertiesContext ctx)
    {
        this.antlrContextToM3Builder.propertyParser(ctx, this.properties, this.qualifiedProperties, Lists.mutable.<String>empty(), Lists.mutable.<String>empty(), this.typeOwner, this.importId, this.startingQualifiedPropertyIndex);
        return null;
    }
}
