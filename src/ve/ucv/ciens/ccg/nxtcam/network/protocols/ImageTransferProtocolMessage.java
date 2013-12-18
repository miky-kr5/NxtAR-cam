package ve.ucv.ciens.ccg.nxtcam.network.protocols;

import java.io.Serializable;

public final class ImageTransferProtocolMessage implements Serializable{
	private static final long serialVersionUID = 8898L;
	public static final int magicNumber = 0x20;

	public byte message;

	public ImageTransferProtocolMessage(){
		message = -1;
	}
}