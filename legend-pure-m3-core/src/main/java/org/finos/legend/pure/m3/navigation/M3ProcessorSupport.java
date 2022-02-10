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

package org.finos.legend.pure.m3.navigation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class M3ProcessorSupport implements ProcessorSupport
{
    private final Context context;
    private final ModelRepository modelRepository;

    public M3ProcessorSupport(ModelRepository modelRepository)
    {
        this(new Context(), modelRepository);
    }

    public M3ProcessorSupport(Context context, ModelRepository modelRepository)
    {
        this.context = context;
        this.modelRepository = modelRepository;
    }

    @Override
    public boolean instance_instanceOf(CoreInstance valueSpecification, String className)
    {
        return Instance.instanceOf(valueSpecification, className, this);
    }

    @Override
    public boolean type_isPrimitiveType(CoreInstance type)
    {
        return Type.isPrimitiveType(type, this);
    }

    @Override
    public boolean valueSpecification_instanceOf(CoreInstance valueSpecification, String type)
    {
        return ValueSpecification.instanceOf(valueSpecification, type, this);
    }

    @Override
    public CoreInstance type_wrapGenericType(CoreInstance aClass)
    {
        return Type.wrapGenericType(aClass, this);
    }

    @Override
    public CoreInstance function_getFunctionType(CoreInstance function)
    {
        return Function.getFunctionType(function, this.context, this);
    }

    @Override
    public CoreInstance newGenericType(SourceInformation sourceInformation, CoreInstance source, boolean inferred)
    {
        ModelRepository repository = source.getRepository();
        String type = inferred ? M3Paths.InferredGenericType : M3Paths.GenericType;
        return repository.newAnonymousCoreInstance(sourceInformation, this.package_getByUserPath(type));
    }

    @Override
    public CoreInstance package_getByUserPath(final String path)
    {
        if (this.context == null)
        {
            return _Package.getByUserPath(path, this);
        }
        return this.context.getIfAbsentPutElementByPath(path, () -> _Package.getByUserPath(path, this));

    }

    @Override
    public CoreInstance repository_getTopLevel(String root)
    {
        return this.modelRepository.getTopLevel(root);
    }

    @Override
    public CoreInstance newEphemeralAnonymousCoreInstance(String type)
    {
        return this.modelRepository.newEphemeralAnonymousCoreInstance(null, this.package_getByUserPath(type));
    }

    @Override
    public CoreInstance newAnonymousCoreInstance(SourceInformation sourceInformation, String type)
    {
        return this.modelRepository.newAnonymousCoreInstance(sourceInformation, this.package_getByUserPath(type));
    }

    @Override
    public CoreInstance newCoreInstance(String name, String typeName, SourceInformation sourceInformation)
    {
        return this.modelRepository.newCoreInstance(name, this.package_getByUserPath(typeName), sourceInformation);
    }

    @Override
    public SetIterable<CoreInstance> function_getFunctionsForName(String functionName)
    {
        return this.context.getFunctionsForName(functionName);
    }

    @Override
    public CoreInstance newCoreInstance(String name, CoreInstance classifier, SourceInformation sourceInformation)
    {
        return this.modelRepository.newCoreInstance(name, classifier, sourceInformation);
    }

    @Override
    public ImmutableList<CoreInstance> type_getTypeGeneralizations(CoreInstance type, org.eclipse.collections.api.block.function.Function<? super CoreInstance, ? extends ImmutableList<CoreInstance>> generator)
    {
        return this.context.getIfAbsentPutGeneralizations(type, generator);
    }

    @Override
    public CoreInstance class_findPropertyUsingGeneralization(CoreInstance classifier, String propertyName)
    {
        return this.class_getSimplePropertiesByName(classifier).get(propertyName);
    }

    @Override
    public CoreInstance class_findPropertyOrQualifiedPropertyUsingGeneralization(CoreInstance owner, String propertyName)
    {
        CoreInstance resolvedProperty = this.class_findPropertyUsingGeneralization(owner, propertyName);
        if (resolvedProperty == null)
        {
            // TODO What if there are multiple qualified properties with the given name?
            resolvedProperty = _Class.findQualifiedPropertiesUsingGeneralization(owner, propertyName, this).getFirst();
        }
        return resolvedProperty;
    }

    @Override
    public RichIterable<CoreInstance> class_getSimpleProperties(CoreInstance classifier)
    {
        return this.class_getSimplePropertiesByName(classifier).valuesView();
    }

    @Override
    public MapIterable<String, CoreInstance> class_getSimplePropertiesByName(CoreInstance classifier)
    {
        return this.context.getIfAbsentPutPropertiesByName(classifier, cls -> _Class.computePropertiesByName(cls, _Class.SIMPLE_PROPERTIES_PROPERTIES, this).toImmutable());
    }

    @Override
    public RichIterable<CoreInstance> class_getQualifiedProperties(CoreInstance classifier)
    {
        return this.class_getQualifiedPropertiesByName(classifier).valuesView();
    }

    @Override
    public MapIterable<String, CoreInstance> class_getQualifiedPropertiesByName(CoreInstance classifier)
    {
        return _Class.getQualifiedPropertiesByName(classifier, this);
    }

    @Override
    public ListIterable<String> property_getPath(CoreInstance property)
    {
        // Example: [Root, children, core, children, Any, properties, classifierGenericType]
        return this.context.getIfAbsentPutPropertyPath(property, prop -> Property.calculatePropertyPath(prop, this).toImmutable());
    }

    @Override
    public CoreInstance getClassifier(CoreInstance instance)
    {
        return instance.getClassifier();
    }

    @Override
    public boolean type_subTypeOf(CoreInstance type, CoreInstance possibleSuperType)
    {
        return (type == possibleSuperType) ||
                (type == type_BottomType()) ||
                (possibleSuperType == type_TopType()) ||
                this.context.getIfAbsentPutTypeGeneralizationSet(type, t -> Sets.immutable.withAll(Type.getGeneralizationResolutionOrder(t, this))).contains(possibleSuperType);
    }

    @Override
    public CoreInstance type_BottomType()
    {
        return this.context.getIfAbsentPutNil(() -> _Package.getByUserPath(M3Paths.Nil, this));
    }

    @Override
    public CoreInstance type_TopType()
    {
        return this.context.getIfAbsentPutAny(() -> _Package.getByUserPath(M3Paths.Any, this));
    }
}
