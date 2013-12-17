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
package ve.ucv.ciens.ccg.nxtcam.network.protocols;

public final class ImageTransferProtocol{
	public static final byte STREAM_CONTROL_END = 0x10;
	public static final byte ACK_SEND_NEXT = 0x20;
	public static final byte ACK_WAIT = 0x30;
	public static final byte FLOW_CONTROL_WAIT = 0x40;
	public static final byte FLOW_CONTROL_CONTINUE = 0x50;
	public static final byte IMAGE_DATA = 0x60;
	public static final byte UNRECOGNIZED = (byte)0xFF;

	public static boolean checkValidityOfMessage(byte message){
		boolean validity;

		switch(message){
		case STREAM_CONTROL_END:
		case ACK_SEND_NEXT:
		case ACK_WAIT:
		case FLOW_CONTROL_WAIT:
		case FLOW_CONTROL_CONTINUE:
		case IMAGE_DATA:
		case UNRECOGNIZED:
			validity = true;
		default:
			validity = false;
		}

		return validity;
	}
}
