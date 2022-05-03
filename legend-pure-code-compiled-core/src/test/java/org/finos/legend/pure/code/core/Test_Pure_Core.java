package org.finos.legend.pure.code.core;

import junit.framework.TestSuite;
import org.finos.legend.pure.code.core.compiled.test.PureTestBuilderHelper;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

public class Test_Pure_Core
{
    public static TestSuite suite()
    {
        CompiledExecutionSupport executionSupport = PureTestBuilderHelper.getClassLoaderExecutionSupport();
        TestSuite suite = new TestSuite();
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::json", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::protocols", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::pure", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::alloy", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::legend", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::external::shared::format", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::external::language::morphir", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::external::format::yaml", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::external::format::rosetta", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::external::format::avro", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::external::language::java", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        return suite;
    }

}
