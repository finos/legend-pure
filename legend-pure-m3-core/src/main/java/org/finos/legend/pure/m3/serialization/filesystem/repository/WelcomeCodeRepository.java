package org.finos.legend.pure.m3.serialization.filesystem.repository;

public class WelcomeCodeRepository extends CodeRepository
{
    public static final String NAME = null;

    WelcomeCodeRepository()
    {
        super(NAME, null);
    }

    @Override
    public boolean isVisible(CodeRepository other)
    {
        return true;
    }
}
