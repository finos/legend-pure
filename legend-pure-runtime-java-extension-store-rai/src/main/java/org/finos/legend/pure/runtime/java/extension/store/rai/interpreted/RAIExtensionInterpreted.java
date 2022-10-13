package org.finos.legend.pure.runtime.java.extension.store.rai.interpreted;

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.runtime.java.extension.store.rai.interpreted.natives.RAIExecute;
import org.finos.legend.pure.runtime.java.interpreted.extension.BaseInterpretedExtension;
import org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtension;

public class RAIExtensionInterpreted extends BaseInterpretedExtension {
    public RAIExtensionInterpreted() {
        super(Lists.mutable.with(
            Tuples.pair("RAIExecute_RAIConnection_1__String_1__RAIResult_1_", (e, r) -> new RAIExecute(r, e.getMessage()))
        ));
    }

    public static InterpretedExtension extension() { return new RAIExtensionInterpreted(); }
}
