/*
 * Copyright (C) 2014 Miguel Angel Astor Romero
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

/**
 * <p>Bit masks used to code/decode the control instructions sent by NxtAR-cam to
 * NxtAR-bot.</p>
 * <p>Expansions 1-3 are currently unused.</p>
 */
public abstract class MotorMasks {
	public static final byte MOTOR_A     = (byte)0x01;
	public static final byte MOTOR_B     = (byte)0x02;
	public static final byte MOTOR_C     = (byte)0x04;
	public static final byte DIRECTION   = (byte)0x08;
	public static final byte RECENTER    = (byte)0x10;
	public static final byte EXPANSION_1 = (byte)0x20;
	public static final byte EXPANSION_2 = (byte)0x20;
	public static final byte EXPANSION_3 = (byte)0x20;
}
