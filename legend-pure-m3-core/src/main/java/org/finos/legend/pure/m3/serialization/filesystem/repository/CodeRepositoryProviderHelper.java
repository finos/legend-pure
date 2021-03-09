package org.finos.legend.pure.m3.serialization.filesystem.repository;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.util.ServiceLoader;

public class CodeRepositoryProviderHelper
{
    public static RichIterable<CodeRepository> findCodeRepositories()
    {
        MutableList<CodeRepository> repositories = Lists.mutable.empty();
        for (CodeRepositoryProvider codeRepositoryProvider : ServiceLoader.load(CodeRepositoryProvider.class))
        {
            repositories.add(codeRepositoryProvider.repository());
        }
        return repositories;
    }
}
