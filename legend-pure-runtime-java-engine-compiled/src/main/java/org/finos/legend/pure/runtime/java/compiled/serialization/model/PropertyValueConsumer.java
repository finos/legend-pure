package org.finos.legend.pure.runtime.java.compiled.serialization.model;

import java.util.function.Consumer;

public abstract class PropertyValueConsumer implements PropertyValueVisitor<Void>, Consumer<PropertyValue>
{
    @Override
    public final Void visit(PropertyValueMany many)
    {
        accept(many);
        return null;
    }

    @Override
    public final Void visit(PropertyValueOne one)
    {
        accept(one);
        return null;
    }

    @Override
    public final void accept(PropertyValue propertyValue)
    {
        propertyValue.visit(this);
    }

    protected abstract void accept(PropertyValueMany many);

    protected abstract void accept(PropertyValueOne one);
}
