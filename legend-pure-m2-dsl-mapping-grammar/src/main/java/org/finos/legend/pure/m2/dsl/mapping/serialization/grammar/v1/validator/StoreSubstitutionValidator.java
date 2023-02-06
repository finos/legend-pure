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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.validator;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.AssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.InstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SubstituteStore;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.m4.exception.PureCompilationException;

class StoreSubstitutionValidator
{
    private StoreSubstitutionValidator()
    {
    }

    private static final Function<Store, String> STORE_PATH = new Function<Store, String>()
    {
        @Override
        public String valueOf(Store store)
        {
            return PackageableElement.getUserPathForPackageableElement(store);
        }
    };

    private static final Function<SubstituteStore, Store> ORIGINAL_STORE = new Function<SubstituteStore, Store>()
    {
        @Override
        public Store valueOf(SubstituteStore substituteStore)
        {
            return substituteStore._original();
        }
    };

    private static final Function<SubstituteStore, Store> SUBSTITUTE_STORE = new Function<SubstituteStore, Store>()
    {
        @Override
        public Store valueOf(SubstituteStore substituteStore)
        {
            return substituteStore._substitute();
        }
    };

    private static final Function<SetImplementation, Iterable<Store>> CLASS_MAPPING_STORES = new Function<SetImplementation, Iterable<Store>>()
    {
        @Override
        public Iterable<Store> valueOf(SetImplementation setImplementation)
        {
            return setImplementation instanceof InstanceSetImplementation ? (Iterable<Store>)((InstanceSetImplementation)setImplementation)._stores() : Sets.mutable.<Store>empty();
        }
    };

    private static final Function<AssociationImplementation, Iterable<Store>> ASSOCIATION_MAPPING_STORES = new Function<AssociationImplementation, Iterable<Store>>()
    {
        @Override
        public Iterable<Store> valueOf(AssociationImplementation associationImplementation)
        {
            return (Iterable<Store>)associationImplementation._stores();
        }
    };

    private static final Function2<Store, MutableMap<Store, Store>, Store> SUBSTITUTE_STORE_FROM_STORE_SUBSTITUTION_MAPPING = new Function2<Store, MutableMap<Store, Store>, Store>()
    {
        @Override
        public Store value(Store store, MutableMap<Store, Store> storeSubstitutionMapping)
        {
            return storeSubstitutionMapping.containsKey(store) ? storeSubstitutionMapping.get(store) : store;
        }
    };

    private static final Function<MappingInclude, Iterable<Store>> ALL_STORES_ACCESSED_IN_INCLUDED_MAPPINGS = new Function<MappingInclude, Iterable<Store>>()
    {
        @Override
        public Iterable<Store> valueOf(MappingInclude mappingInclude)
        {
            return getAllStoresAccessedInMapping(mappingInclude._included()).collectWith(SUBSTITUTE_STORE_FROM_STORE_SUBSTITUTION_MAPPING, createStoreSubstitutionMapping(mappingInclude));
        }
    };

    private static final Procedure<MappingInclude> VALIDATE_STORE_SUBSTITUTION = new Procedure<MappingInclude>()
    {
        @Override
        public void value(MappingInclude mappingInclude)
        {
            MutableSet<Store> substitutionOriginalStores = getOriginalStores(mappingInclude);
            MutableSet<Store> includedMappingStores = getAllStoresAccessedInMapping(mappingInclude._included());

            MutableSet<Store> invalidOriginalStores = substitutionOriginalStores.difference(includedMappingStores);
            if (!invalidOriginalStores.isEmpty())
            {
                throw new PureCompilationException(mappingInclude.getSourceInformation(), "Store Substitution Error in mapping [" +
                        PackageableElement.getUserPathForPackageableElement(mappingInclude._owner()) + "] as " +
                        invalidOriginalStores.collect(STORE_PATH).makeString("[", ",", "]") + (invalidOriginalStores.size() == 1 ? " does " : " do ") +
                        "not exist in included mapping [" + PackageableElement.getUserPathForPackageableElement(mappingInclude._included()) + "]");
            }
        }
    };

    static void validateStoreSubstitutions(Mapping mapping)
    {
        mapping._includes().forEach(VALIDATE_STORE_SUBSTITUTION);
    }

    private static MutableSet<Store> getAllStoresAccessedInMapping(Mapping mapping)
    {
        return getAllStoresDirectlyAccessedInMapping(mapping).union(getAllStoresAccessedInIncludedMappings(mapping));
    }

    private static MutableSet<Store> getAllStoresDirectlyAccessedInMapping(Mapping mapping)
    {
        return getAllStoresDirectlyAccessedInClassMappings(mapping).union(getAllStoresDirectlyAccessedInAssociationMappings(mapping));
    }

    private static MutableSet<Store> getAllStoresDirectlyAccessedInClassMappings(Mapping mapping)
    {
        return mapping._classMappings().flatCollect(CLASS_MAPPING_STORES).toSet().without(null);
    }

    private static MutableSet<Store> getAllStoresDirectlyAccessedInAssociationMappings(Mapping mapping)
    {
        return mapping._associationMappings().flatCollect(ASSOCIATION_MAPPING_STORES).toSet().without(null);
    }

    private static MutableSet<Store> getAllStoresAccessedInIncludedMappings(Mapping mapping)
    {
        return mapping._includes().flatCollect(ALL_STORES_ACCESSED_IN_INCLUDED_MAPPINGS).toSet().without(null);
    }

    private static MutableSet<Store> getOriginalStores(MappingInclude mappingInclude)
    {
        return mappingInclude._storeSubstitutions().collect(ORIGINAL_STORE).toSet();
    }

    private static MutableMap<Store, Store> createStoreSubstitutionMapping(MappingInclude mappingInclude)
    {
        return mappingInclude._storeSubstitutions().toMap(ORIGINAL_STORE, SUBSTITUTE_STORE);
    }
}
