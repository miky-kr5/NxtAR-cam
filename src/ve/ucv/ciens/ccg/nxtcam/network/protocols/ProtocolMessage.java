package ve.ucv.ciens.ccg.nxtcam.network.protocols;

import java.io.Serializable;

@SuppressWarnings("serial")
public abstract class ProtocolMessage implements Serializable {
	public static int magicNumber = 0x00;
}
