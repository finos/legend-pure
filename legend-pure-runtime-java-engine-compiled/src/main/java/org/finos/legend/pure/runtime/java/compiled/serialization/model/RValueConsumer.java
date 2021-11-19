package org.finos.legend.pure.runtime.java.compiled.serialization.model;

import java.util.function.Consumer;

public abstract class RValueConsumer implements RValueVisitor<Void>, Consumer<RValue>
{
    @Override
    public final Void visit(Primitive primitive)
    {
        accept(primitive);
        return null;
    }

    @Override
    public final Void visit(ObjRef objRef)
    {
        accept(objRef);
        return null;
    }

    @Override
    public final Void visit(EnumRef enumRef)
    {
        accept(enumRef);
        return null;
    }

    @Override
    public final void accept(RValue value)
    {
        value.visit(this);
    }

    protected abstract void accept(Primitive primitive);

    protected abstract void accept(ObjRef objRef);

    protected abstract void accept(EnumRef enumRef);
}
