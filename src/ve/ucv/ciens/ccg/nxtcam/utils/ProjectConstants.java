/*
 * Copyright (C) 2013 Miguel Angel Astor Romero
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ve.ucv.ciens.ccg.nxtcam.utils;

import java.util.UUID;

import android.app.Activity;

public abstract class ProjectConstants {
	// Network related constants.
	public static final int SERVER_UDP_PORT = 8889;
	public static final int SERVER_TCP_PORT_1 = 9989;
	public static final int SERVER_TCP_PORT_2 = 9990;
	public static final int SERVER_TCP_PORT_3 = 9991;
	public static final UUID SERIAL_PORT_SERVICE_CLASS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	public static final String OUI_LEGO = "00:16:53";
	public static final String MULTICAST_ADDRESS = "230.0.0.1";
	public static final String SERVICE_DISCOVERY_MESSAGE = "NxtAR server here!";

	// Activity results.
	public static final int RESULT_CAMERA_FAILURE = Activity.RESULT_FIRST_USER + 1;

	public static final boolean DEBUG = true;
}
