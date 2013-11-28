package ve.ucv.ciens.ccg.nxtcam.utils;

import java.util.UUID;

public abstract class ProjectConstants {
	public static final int SERVER_UDP_PORT = 8889;
	public static final int SERVER_TCP_PORT_1 = 9989;
	public static final int SERVER_TCP_PORT_2 = 9990;
	public static final UUID SERIAL_PORT_SERVICE_CLASS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	public static final String OUI_LEGO = "00:16:53";
	public static final String MULTICAST_ADDRESS = "230.0.0.1";

	public static final boolean DEBUG = true;
}
