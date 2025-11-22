// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.compiler.validation.validator;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.type.ExtendedPrimitiveType;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class PrimitiveTypeValidator implements MatchRunner<PrimitiveType>
{
    @Override
    public String getClassName()
    {
        return M3Paths.PrimitiveType;
    }

    @Override
    public void run(PrimitiveType primitiveType, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        validateTypeVariables(primitiveType, state.getProcessorSupport());
    }

    private void validateTypeVariables(PrimitiveType primitiveType, ProcessorSupport processorSupport)
    {
        if (primitiveType._typeVariables().notEmpty())
        {
            // check immediate generalizations
            Generalization genlWithTypeVar = primitiveType._generalizations().detect(genl -> genl._general()._rawType().getValueForMetaPropertyToMany(M3Properties.typeVariables).notEmpty());
            if (genlWithTypeVar != null)
            {
                StringBuilder builder = new StringBuilder("Primitive type with type variables ");
                ExtendedPrimitiveType.print(builder, primitiveType, true, processorSupport);
                builder.append(" extends a primitive type with type variables ");
                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type superType = genlWithTypeVar._general()._rawType();
                ExtendedPrimitiveType.print(builder, superType, true, processorSupport);
                SourceInformation superTypeSourceInfo = superType.getSourceInformation();
                if (superTypeSourceInfo != null)
                {
                    superTypeSourceInfo.appendMessage(builder.append(" at "));
                }
                builder.append(": this is not currently supported");
                throw new PureCompilationException(genlWithTypeVar.getSourceInformation(), builder.toString());
            }

            // check all super types
            CoreInstance superTypeWithTypeVars = Type.getGeneralizationResolutionOrder(primitiveType, processorSupport).detect(t -> (t != primitiveType) && t.getValueForMetaPropertyToMany(M3Properties.typeVariables).notEmpty());
            if (superTypeWithTypeVars != null)
            {
                StringBuilder builder = new StringBuilder("Primitive type with type variables ");
                ExtendedPrimitiveType.print(builder, primitiveType, true, processorSupport);
                builder.append(" extends a primitive type with type variables ");
                ExtendedPrimitiveType.print(builder, superTypeWithTypeVars, true, processorSupport);
                SourceInformation superTypeSourceInfo = superTypeWithTypeVars.getSourceInformation();
                if (superTypeSourceInfo != null)
                {
                    superTypeSourceInfo.appendMessage(builder.append(" at "));
                }
                builder.append(": this is not currently supported");
                throw new PureCompilationException(primitiveType.getSourceInformation(), builder.toString());
            }
        }

        MutableMap<String, VariableExpression> typeVariablesByName = Maps.mutable.empty();
        primitiveType._typeVariables().forEach(v ->
        {
            VariableExpression old = typeVariablesByName.put(v._name(), v);
            if (old != null)
            {
                StringBuilder builder = new StringBuilder("Type variable '").append(v._name()).append("' is already defined");
                SourceInformation oldSourceInfo = old.getSourceInformation();
                if (oldSourceInfo != null)
                {
                    oldSourceInfo.appendMessage(builder.append(" (at ")).append(')');
                }
                throw new PureCompilationException(v.getSourceInformation(), builder.toString());
            }
        });

        Type.getGeneralizationResolutionOrder(primitiveType, processorSupport).drop(1).forEach(genl ->
        {
            if (genl instanceof PrimitiveType)
            {
                ((PrimitiveType) genl)._typeVariables().forEach(v ->
                {
                    VariableExpression conflict = typeVariablesByName.get(v._name());
                    if (conflict != null)
                    {
                        StringBuilder builder = new StringBuilder("Type variable '").append(conflict._name()).append("' is already defined in supertype ");
                        PackageableElement.writeUserPathForPackageableElement(builder, genl);
                        SourceInformation oldSourceInfo = v.getSourceInformation();
                        if (oldSourceInfo != null)
                        {
                            oldSourceInfo.appendMessage(builder.append(" at "));
                        }
                        throw new PureCompilationException(conflict.getSourceInformation(), builder.toString());
                    }
                });
            }
        });
    }
}
