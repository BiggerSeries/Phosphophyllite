package net.roguelogix.phosphophyllite.debug;

import javax.annotation.Nullable;

public interface IDebuggable {
    @Nullable
    String getDebugString();
}
