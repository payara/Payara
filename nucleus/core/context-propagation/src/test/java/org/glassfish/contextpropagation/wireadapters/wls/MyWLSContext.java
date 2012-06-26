package org.glassfish.contextpropagation.wireadapters.wls;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.glassfish.contextpropagation.SerializableContextFactory.WLSContext;

public class MyWLSContext implements WLSContext {
    public long l;

    @Override
    public void writeContext(ObjectOutput out) throws IOException {
      out.writeLong(200L);
    }

    @Override
    public void readContext(ObjectInput in) throws IOException {
      l = in.readLong();
    }

  
}
