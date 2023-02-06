package org.finos.legend.pure.m3.generator.par;

public interface Log
{
    void info(String txt);
    void error(String txt, Exception e);
}
