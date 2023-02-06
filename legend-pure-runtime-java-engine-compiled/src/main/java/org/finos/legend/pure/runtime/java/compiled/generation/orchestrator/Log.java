package org.finos.legend.pure.runtime.java.compiled.generation.orchestrator;

public interface Log
{
    void info(String txt);
    void error(String txt, Exception e);

    void error(String format);

    void warn(String s);
}
