package org.finos.legend.pure.code.core;

import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProvider;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;

public class RelationalStoreCodeRepositoryProvider implements CodeRepositoryProvider
{
    @Override
    public CodeRepository repository()
    {
        return GenericCodeRepository.build("platform_store_relational.definition.json");
    }
}

