package aqua.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * Created by Lars on 26.04.2016.
 */
public final class NeighbourUpdate implements Serializable {
    private final InetSocketAddress left;
    private final InetSocketAddress right;

    public NeighbourUpdate(InetSocketAddress left, InetSocketAddress right) {
        this.left = left;
        this.right = right;
    }

    public InetSocketAddress getLeft() {
        return left;
    }

    public InetSocketAddress getRight() {
        return right;
    }
}
