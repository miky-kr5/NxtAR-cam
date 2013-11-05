package ve.ucv.ciens.ccg.nxtcam.network.protocols;


public abstract class ImageTransferProtocol{
	public static enum ProtocolState{
		SALUTE, IMG_FOLLOWS, SEND_DATA, PAUSED, WAITING, GOODBYE
	}
	
	public static final byte MSG_HELLO = 			(byte)0x89;
	public static final byte MSG_GOODBYE = 		(byte)0x90;
	public static final byte MSG_IMG_DATA = 		(byte)0x42;
	public static final byte CMD_IMG_FOLLOWS = 	(byte)0x10;
	public static final byte CMD_PAUSE = 			(byte)0x15;
	public static final byte CMD_IMG_WAIT = 		(byte)0x20;
	public static final byte ACK_SEND_IMG = 		(byte)0x40;
	public static final byte ACK_IMG_RCVD = 		(byte)0x50;
}
