package ve.ucv.ciens.ccg.nxtcam.network.protocols;

import java.security.InvalidParameterException;

public abstract class LegoCommunicationProtocol{
	/**
	 * Command types. Byte 0;
	 */
	private static final byte DIRECT_COMMAND_REPLY    =       0x00;
	private static final byte SYSTEM_COMMAND_REPLY    =       0x01;
	private static final byte DIRECT_COMMAND_NO_REPLY = (byte)0x80;

	/**
	 * Comand bytes. Byte 1;
	 */
	private static final byte GET_FIRMWARE_VERSION    = (byte)0x88;
	private static final byte GET_DEVICE_INFO         = (byte)0x9B;
	private static final byte SET_OUTPUT_STATE        =       0x04;
	private static final byte SET_INPUT_MODE          =       0x05;
	private static final byte GET_OUTPUT_STATE        =       0x06;
	private static final byte GET_INPUT_VALUES        =       0x07;

	/**
	 * Ports for  get/setOutputState() and get/setInputMode().
	 */
	public static final byte PORT_0 = 0x00;
	public static final byte PORT_1 = 0x01;
	public static final byte PORT_2 = 0x02;
	public static final byte PORT_3 = 0x03;

	/**
	 * Mode bytes for setOutputState().
	 */
	public static final byte MOTORON   = 0x01;
	public static final byte BRAKE     = 0x02;
	public static final byte REGULATED = 0x04;

	/**
	 * Regulation modes for setOutputState().
	 */
	public static final byte REGULATION_MODE_IDLE        = 0x00;
	public static final byte REGULATION_MODE_MOTOR_SPEED = 0x01;
	public static final byte REGULATION_MODE_MOTOR_SYNC  = 0x02;

	/**
	 * Run states for setOutputState().
	 */
	public static final byte MOTOR_RUN_STATE_IDLE     = 0x00;
	public static final byte MOTOR_RUN_STATE_RAMPUP   = 0x10;
	public static final byte MOTOR_RUN_STATE_RUNNING  = 0x20;
	public static final byte MOTOR_RUN_STATE_RAMPDOWN = 0x40;

	/**
	 * Sensor types for setInputMode().
	 */
	public static final byte NO_SENSOR = 0x00;
	public static final byte SWITCH = 0x01;
	public static final byte TEMPERATURE = 0x02;
	public static final byte REFLECTION = 0x03;
	public static final byte ANGLE = 0x04;
	public static final byte LIGHT_ACTIVE = 0x05;
	public static final byte LIGHT_INACTIVE = 0x06;
	public static final byte SOUND_DB = 0x07;
	public static final byte SOUND_DBA = 0x08;
	public static final byte CUSTOM = 0x09;
	public static final byte LOWSPEED = 0x0A;
	public static final byte LOWSPEED_9V = 0x0B;
	public static final byte NO_OF_SENSOR_TYPES = 0x0C;

	/**
	 * Sensor modes for setInputMode().
	 */
	public static final byte RAWMODE = 0x00;
	public static final byte BOOLEANMODE = 0x20;
	public static final byte TRANSITIONCNTMODE = 0x40;
	public static final byte PERIODCOUNTERMODE = 0x60;
	public static final byte PCTFULLSCALEMODE = (byte)0x80;
	public static final byte CELSIUSMODE = (byte)0xA0;
	public static final byte FARENHEITMODE = (byte)0xC0;
	public static final byte ANGLESTEPMODE = (byte)0xE0;
	public static final byte SLOPEMASK = (byte)0x1F;
	public static final byte MODEMASK = (byte)0xE0;

	/**
	 * Firmware and protocol version request pdu. Page 11 of appendix 1.
	 * 
	 * @return byte[4], the pdu.
	 */
	public static byte[] getFirmwareVersion(){
		byte[] message = new byte[4];
		message[0] = 0x02;
		message[1] = 0x00;
		message[2] = SYSTEM_COMMAND_REPLY;
		message[3] = GET_FIRMWARE_VERSION;
		return message;
	}

	/**
	 * Device info request pdu. Page 14 of appendix 1.
	 * 
	 * @return byte[4], the pdu.
	 */
	public static byte[] getDeviceInfo(){
		byte[] message = new byte[4];
		message[0] = 0x02;
		message[1] = 0x00;
		message[2] = SYSTEM_COMMAND_REPLY;
		message[3] = GET_DEVICE_INFO;
		return message;
	}

	/**
	 * Set motor configuration pdu. Page 6 of appendix 2.
	 * 
	 * @param output_port The port in the brick the motor is connected to.
	 * @param power_set_point
	 * @param mode_byte
	 * @param regulation_mode
	 * @param turn_ratio
	 * @param run_state
	 * @return byte[15], the pdu.
	 * @throws InvalidParameterException When any parameter is out of range or is an invalid enum. Ranges defined in appendix 2.
	 */
	public static byte[] setOutputState(byte output_port, byte power_set_point, byte mode_byte, byte regulation_mode, byte turn_ratio, byte run_state) throws InvalidParameterException{
		byte[] message = new byte[15];

		if(output_port < PORT_0 || output_port > PORT_2){
			throw new InvalidParameterException("Output port out of range.");
		}
		if(power_set_point < -100 || power_set_point > 100){
			throw new InvalidParameterException("Power set point out of range.");
		}
		if(turn_ratio < -100 || turn_ratio > 100){
			throw new InvalidParameterException("Turn ratio out of range.");
		}
		if(mode_byte != MOTORON && mode_byte != BRAKE && mode_byte != REGULATED){
			throw new InvalidParameterException("Invalid mode byte.");
		}
		if(regulation_mode != REGULATION_MODE_IDLE && regulation_mode != REGULATION_MODE_MOTOR_SPEED && regulation_mode != REGULATION_MODE_MOTOR_SYNC){
			throw new InvalidParameterException("Invalid regulation mode.");
		}
		if(run_state != MOTOR_RUN_STATE_IDLE && run_state != MOTOR_RUN_STATE_RAMPUP && run_state != MOTOR_RUN_STATE_RUNNING && run_state != MOTOR_RUN_STATE_RAMPDOWN){
			throw new InvalidParameterException("Invalid run state.");
		}

		message[0] = 0x0C;
		message[1] = 0x00;
		message[2] = DIRECT_COMMAND_NO_REPLY;
		message[3] = SET_OUTPUT_STATE;
		message[4] = output_port;
		message[5] = power_set_point;
		message[6] = mode_byte;
		message[7] = regulation_mode;
		message[8] = turn_ratio;
		message[9] = run_state;
		message[10] = message[11] = message[12] = message[13] = message[14] = 0x00; 

		return message;
	}

	/**
	 * Motor configuration request pdu. Page 8 of appendix 2.
	 * 
	 * @param output_port The port in the brick the motor is connected to.
	 * @return byte[5], the pdu.
	 * @throws InvalidParameterException When any parameter is out of range or is an invalid enum. Ranges defined in appendix 2.
	 */
	public static byte[] getOutputState(byte output_port) throws InvalidParameterException{
		byte[] message = new byte[5];

		if(output_port < PORT_0 || output_port > PORT_2){
			throw new InvalidParameterException("Output port out of range.");
		}

		message[0] = 0x03;
		message[1] = 0x00;
		message[2] = DIRECT_COMMAND_REPLY;
		message[3] = GET_OUTPUT_STATE;
		message[4] = output_port;

		return message;
	}

	/**
	 * Sensor feed request pdu. Page 8 of appendix 2.
	 * 
	 * @param input_port The port in the brick the sensor is connected to.
	 * @return byte[5], the pdu.
	 * @throws InvalidParameterException When any parameter is out of range or is an invalid enum. Ranges defined in appendix 2.
	 */
	public static byte[] getInputValues(byte input_port) throws InvalidParameterException{
		byte[] message = new byte[5];

		if(input_port < PORT_0 || input_port > PORT_3){
			throw new InvalidParameterException("Input port is out of range.");
		}

		message[0] = 0x03;
		message[1] = 0x00;
		message[2] = DIRECT_COMMAND_REPLY;
		message[3] = GET_INPUT_VALUES;
		message[4] = input_port;

		return message;
	}

	/**
	 * Sensor configuration pdu. 
	 * 
	 * @param input_port The port in the brick the sensor is connected to.
	 * @param sensor_type The sensor to be configured.
	 * @param sensor_mode The configuration to set.
	 * @return byte[7], the pdu.
	 * @throws InvalidParameterException When any parameter is out of range or is an invalid enum. Ranges defined in appendix 2.
	 */
	public static byte[] setInputMode(byte input_port, byte sensor_type, byte sensor_mode) throws InvalidParameterException{
		byte[] message = new byte[7];

		if(input_port < PORT_0 || input_port > PORT_3){
			throw new InvalidParameterException("Input port is out of range.");
		}
		if(sensor_type < 0x00 || sensor_type > 0x0C){
			throw new InvalidParameterException("Invalid sensor type.");
		}

		message[0] = 0x05;
		message[1] = 0x00;
		message[2] = DIRECT_COMMAND_NO_REPLY;
		message[3] = SET_INPUT_MODE;
		message[4] = input_port;
		message[5] = sensor_type;
		switch(sensor_mode){
		case RAWMODE:
		case BOOLEANMODE:
		case TRANSITIONCNTMODE:
		case PERIODCOUNTERMODE:
		case PCTFULLSCALEMODE:
		case CELSIUSMODE:
		case FARENHEITMODE:
		case ANGLESTEPMODE: // Same case as MODEMASK. 
		case SLOPEMASK:
			message[6] = sensor_mode;
			break;
		}

		return message;
	}
}
