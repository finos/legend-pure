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

package org.finos.legend.pure.m2.relational.serialization.grammar.v1.processor;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SubstituteStore;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m4.exception.PureCompilationException;

class DatabaseSubstitutionHandler
{
    private DatabaseSubstitutionHandler()
    {
    }

    private static final Function<SubstituteStore, Database> ORIGINAL_DATABASE = new Function<SubstituteStore, Database>()
    {
        @Override
        public Database valueOf(SubstituteStore substituteStore)
        {
            return (Database)substituteStore._original();
        }
    };

    private static final Function<SubstituteStore, Database> SUBSTITUTE_DATABASE = new Function<SubstituteStore, Database>()
    {
        @Override
        public Database valueOf(SubstituteStore substituteStore)
        {
            return (Database)substituteStore._substitute();
        }
    };

    private static Database getDatabaseAfterStoreSubstitution(MutableMap<Database, Database> databaseSubstitutionMap, Database database, MutableSet<Database> visitedDatabases)
    {
        if (visitedDatabases.contains(database))
        {
            throw new PureCompilationException(database.getSourceInformation(), "Cyclic Store Substitution for store [" + PackageableElement.getUserPathForPackageableElement(database) + "] in mapping hierarchy");
        }
        visitedDatabases.add(database);
        if (databaseSubstitutionMap.containsKey(database))
        {
            return getDatabaseAfterStoreSubstitution(databaseSubstitutionMap, databaseSubstitutionMap.get(database), visitedDatabases);
        }
        visitedDatabases.remove(database);
        return database;
    }

    private static Database getDatabaseAfterStoreSubstitution(MutableMap<Database, Database> databaseSubstitutionMap, Database database)
    {
        return getDatabaseAfterStoreSubstitution(databaseSubstitutionMap, database, Sets.mutable.<Database>empty());
    }

    private static void collectStoreSubstitutionsAlongPathFromMappingToSuperMappping(Mapping currentMapping, Mapping superMapping, MutableSet<SubstituteStore> storeSubstitutionsAlongPathFromMappingToCurrentMapping, MutableSet<Mapping> visitedIncludedMappings, MutableSet<SubstituteStore> storeSubstitutionsAlongPathFromMappingToSuperMapping)
    {
        if (visitedIncludedMappings.contains(currentMapping))
        {
            throw new PureCompilationException(currentMapping.getSourceInformation(), "Cyclic mapping include for mapping [" + PackageableElement.getUserPathForPackageableElement(currentMapping) + "] in mapping hierarchy");
        }

        visitedIncludedMappings.add(currentMapping);
        if (currentMapping.equals(superMapping))
        {
            storeSubstitutionsAlongPathFromMappingToSuperMapping.addAll(storeSubstitutionsAlongPathFromMappingToCurrentMapping);
        }
        for (MappingInclude mappingInclude : currentMapping._includes())
        {
            storeSubstitutionsAlongPathFromMappingToCurrentMapping.addAllIterable(mappingInclude._storeSubstitutions());
            collectStoreSubstitutionsAlongPathFromMappingToSuperMappping(mappingInclude._included(), superMapping, storeSubstitutionsAlongPathFromMappingToCurrentMapping, visitedIncludedMappings, storeSubstitutionsAlongPathFromMappingToSuperMapping);
            storeSubstitutionsAlongPathFromMappingToCurrentMapping.removeAllIterable(mappingInclude._storeSubstitutions());
        }
        visitedIncludedMappings.remove(currentMapping);
    }

    private static MutableMap<Database, Database> getStoreSubstitutionMap(RootRelationalInstanceSetImplementation implementation, RootRelationalInstanceSetImplementation superImplementation)
    {
        MutableSet<SubstituteStore> storeSubstitutionSet = Sets.mutable.empty();
        collectStoreSubstitutionsAlongPathFromMappingToSuperMappping(implementation._parent(), superImplementation._parent(), Sets.mutable.<SubstituteStore>empty(), Sets.mutable.<Mapping>empty(), storeSubstitutionSet);
        return storeSubstitutionSet.toMap(ORIGINAL_DATABASE, SUBSTITUTE_DATABASE);
    }

    static Database getDatabaseAfterStoreSubstitution(RootRelationalInstanceSetImplementation implementation, RootRelationalInstanceSetImplementation superImplementation, Database superImplementationDatabase)
    {
        MutableMap<Database, Database> databaseSubstitutionMap = getStoreSubstitutionMap(implementation, superImplementation);
        return getDatabaseAfterStoreSubstitution(databaseSubstitutionMap, superImplementationDatabase);
    }
}
