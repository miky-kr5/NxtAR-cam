package ve.ucv.ciens.ccg.nxtcam.network.protocols;

import java.io.Serializable;

public final class ImageDataMessage implements Serializable{
	private static final long serialVersionUID = 9989L;
	public static final int magicNumber = 0x10;

	public int imageWidth;
	public int imageHeight;
	public byte[] data;

	public ImageDataMessage(){
		imageWidth = -1;
		imageHeight = -1;
		data = null;
	}
}