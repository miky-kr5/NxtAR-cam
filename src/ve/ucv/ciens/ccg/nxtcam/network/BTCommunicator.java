package ve.ucv.ciens.ccg.nxtcam.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import ve.ucv.ciens.ccg.nxtcam.MainActivity;
import ve.ucv.ciens.ccg.nxtcam.utils.Logger;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * Basic Bluetooth communication manager class.
 * 
 * @author Miguel Angel Astor Romero <miguel.astor@ciens.ucv.ve>
 * @version 1.0
 * @since 2012-10-15
 */
public class BTCommunicator{
	private static final String TAG = "NXT_TEST_BTCOMM";
	private final String CLASS_NAME = MainActivity.class.getSimpleName();

	private static final UUID SERIAL_PORT_SERVICE_CLASS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final String OUI_LEGO = "00:16:53";

	private boolean connected;
	private BluetoothAdapter bt_adapter = null;
	private BluetoothSocket bt_socket = null;
	private OutputStream nxt_out_stream = null;
	private InputStream nxt_in_stream = null;
	
	private static class SingletonHolder{
		public static final BTCommunicator INSTANCE = new BTCommunicator();
	}

	/**
	 * Gets the singleton instance of this class.
	 * 
	 * @return The singleton instance of this class.
	 */
	public static BTCommunicator getInstance(){
		return SingletonHolder.INSTANCE;
	}

	/**
	 * Basic constructor.
	 */
	private BTCommunicator(){
		connected = false;
		bt_adapter = BluetoothAdapter.getDefaultAdapter();
		bt_socket = null;
		nxt_in_stream = null;
		nxt_out_stream = null;
	}

	/**
	 * Indicates if Bluetooth is available on the device.
	 * 
	 * @return true if the default Bluetooth adapter exists, otherwise false. 
	 */
	public boolean isBTSupported(){
		return bt_adapter != null;
	}

	/**
	 * Indicates if there is an active connection.
	 * 
	 * @return true if this device is connected to a NXT robot, otherwise false.
	 */
	public boolean isConnected(){
		return connected;
	}

	/**
	 * Wrapper to the standard method for checking if Bluetooth is enabled.
	 * 
	 * @see android.bluetooth.BluetoothAdapter
	 */
	public boolean isBTEnabled(){
		return bt_adapter.isEnabled();
	}

	/**
	 * Wrapper to the standard method for stopping this device's Bluetooth adapter.
	 * 
	 * @see android.bluetooth.BluetoothAdapter
	 */
	public void disableBT(){
		bt_adapter.disable();
	}

	/**
	 * Gets all the devices paired to the default Bluetooth adapter.
	 * 
	 * @return A set containing all devices paired to this device.
	 */
	public Set<BluetoothDevice> getPairedDevices(){
		return bt_adapter.getBondedDevices();
	}

	/**
	 * Sets up a connection with a NXT device.
	 * 
	 * Verifies if the target device is a valid NXT robot by checking agains Lego's OUI.
	 * Also creates the socket and the streams associated with the connection
	 * 
	 * @param mac_address	The mac address of the target device.
	 * @return true if the connection was established succesfully, otherwise false.
	 * @throws IOException
	 */
	public boolean establishConnection(String mac_address) throws IOException{
		if (!bt_adapter.isEnabled()){
			return false;
		}
		if(connected){
			return false;
		}
		if(bt_adapter.isEnabled()){
			if(mac_address == "NONE"){
				return false;
			}else{
				if(mac_address.substring(0, 8).compareTo(OUI_LEGO) != 0){
					Logger.log_d(TAG, CLASS_NAME + ".establishConnection() :: Not a Lego MAC. Prefix : " + mac_address.substring(0, 8) + " :: OUI : " + OUI_LEGO);
					return false;
				}else{
					try{
						Logger.log_d(TAG, CLASS_NAME + ".establishConnection() :: Getting device with mac address: " + mac_address);
						BluetoothDevice nxtDevice = null;
						nxtDevice = bt_adapter.getRemoteDevice(mac_address);
						if (nxtDevice == null) {
							Logger.log_e(TAG, CLASS_NAME + ".establishConnection() :: No device found.");
							throw new IOException();
						}

						Logger.log_d(TAG, CLASS_NAME + ".establishConnection() :: Opening socket.");
						bt_socket = nxtDevice.createRfcommSocketToServiceRecord(SERIAL_PORT_SERVICE_CLASS_UUID);
						Logger.log_d(TAG, CLASS_NAME + ".establishConnection() :: Connecting.");
						bt_socket.connect();

						Logger.log_d(TAG, CLASS_NAME + ".establishConnection() :: Opening IO streams.");
						nxt_in_stream = bt_socket.getInputStream();
						nxt_out_stream = bt_socket.getOutputStream();

						Logger.log_d(TAG, CLASS_NAME + ".establishConnection() :: Connection established.");
						connected = true;

					}catch(IOException e){
						Logger.log_e(TAG, CLASS_NAME + ".establishConnection() :: Connection failed.");
						Logger.log_e(TAG, Log.getStackTraceString(e));
						connected = false;
						throw e;
					}
					return connected;
				}
			}
		}
		return false;
	}

	/**
	 * Closes the active connection if any.
	 * 
	 * Additionally clears the socket and the streams associated to said connection.
	 * 
	 * @return true if the connection was succesfully closed; false if no connection exists.
	 * @throws IOException
	 */
	public boolean stopConnection() throws IOException{
		try{
			if(bt_socket != null){
				Logger.log_d(TAG, CLASS_NAME + ".stopConnection() :: Closing connection.");
				bt_socket.close();
				bt_socket = null;
				nxt_in_stream = null;
				nxt_out_stream = null;
				connected = false;
				Logger.log_d(TAG, CLASS_NAME + ".stopConnection() :: Connection closed.");
				return true;
			}
		}catch( IOException e){
			Logger.log_e(TAG, CLASS_NAME + ".stopConnection()");
			Logger.log_e(TAG, Log.getStackTraceString(e));
			throw e;
		}
		return false;
	}

	/**
	 * Sends a message to the NXT robot.
	 * 
	 * @param message	The data to be sent.
	 * @throws IOException
	 */
	public synchronized void writeMessage(byte[] message) throws IOException{
		if(connected){
			try{
				nxt_out_stream.write(message);
			}catch(IOException e){
				Logger.log_e(TAG, CLASS_NAME + ".writeMessage()");
				Logger.log_e(TAG, Log.getStackTraceString(e));
				throw e;
			}
		}
	}

	/**
	 * Reads a message sent by the NXT robot.
	 * 
	 * @return The data received as a byte[] if a valid connection exists, otherwise null. 
	 * @throws IOException
	 */
	public synchronized byte[] readMessage(int bytes) throws IOException{
		if(connected){
			try{
				byte[] message = new byte[bytes];
				for(int i = 0; i < message.length; ++i){
					message[i] = 0x00;
				}
				nxt_in_stream.read(message, 0, bytes);
				return message;
			}catch(IOException e){
				Logger.log_e(TAG, CLASS_NAME + ".readMessage()");
				Logger.log_e(TAG, Log.getStackTraceString(e));
				throw e;
			}
		}else{
			return null;
		}
	}
}
