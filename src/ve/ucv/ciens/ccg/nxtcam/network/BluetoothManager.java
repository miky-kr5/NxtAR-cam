package ve.ucv.ciens.ccg.nxtcam.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothManager{
	private static final UUID SERIAL_PORT_SERVICE_CLASS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final String OUI_LEGO = "00:16:53";
	private static final String TAG = "BTMNGR";
	
	private boolean connected;
	private BluetoothAdapter bt_adapter;
	private BluetoothSocket bt_socket = null;
	private OutputStream nxt_out_stream = null;
	private InputStream nxt_in_stream = null;

	private static class SingletonHolder{
		public static final BluetoothManager INSTANCE = new BluetoothManager();
	}

	private BluetoothManager(){
		connected = false;
		bt_adapter = BluetoothAdapter.getDefaultAdapter();
		bt_socket = null;
		nxt_in_stream = null;
		nxt_out_stream = null;
	}
	
	public static BluetoothManager getInstance(){
		return SingletonHolder.INSTANCE;
	}

	public boolean isBTSupported(){
		return bt_adapter != null;
	}

	public boolean isConnected(){
		return connected;
	}

	public boolean isBTEnabled(){
		return bt_adapter.isEnabled();
	}

	public void disableBT(){
		bt_adapter.disable();
	}

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
					Log.d(TAG, "establishConnection() :: Not a Lego MAC. Prefix : " + mac_address.substring(0, 8) + " :: OUI : " + OUI_LEGO);
					return false;
				}else{
					try{
						Log.d(TAG, "establishConnection() :: Getting device with mac address: " + mac_address);
						BluetoothDevice nxtDevice = null;
						nxtDevice = bt_adapter.getRemoteDevice(mac_address);
						if (nxtDevice == null) {
							Log.e(TAG, "establishConnection() :: No device found.");
							throw new IOException();
						}

						Log.d(TAG, "establishConnection() :: Opening socket.");
						bt_socket = nxtDevice.createRfcommSocketToServiceRecord(SERIAL_PORT_SERVICE_CLASS_UUID);
						Log.d(TAG, "establishConnection() :: Connecting.");
						bt_socket.connect();

						Log.d(TAG, "establishConnection() :: Opening IO streams.");
						nxt_in_stream = bt_socket.getInputStream();
						nxt_out_stream = bt_socket.getOutputStream();

						Log.d(TAG, "establishConnection() :: Connection established.");
						connected = true;

					}catch(IOException e){
						Log.e(TAG, "establishConnection() :: Connection failed.");
						Log.e(TAG, Log.getStackTraceString(e));
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
				Log.d(TAG, "stopConnection() :: Closing connection.");
				bt_socket.close();
				bt_socket = null;
				nxt_in_stream = null;
				nxt_out_stream = null;
				connected = false;
				Log.d(TAG, "stopConnection() :: Connection closed.");
				return true;
			}
		}catch( IOException e){
			Log.e(TAG, "stopConnection()");
			Log.e(TAG, Log.getStackTraceString(e));
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
				nxt_out_stream.flush();
			}catch(IOException e){
				Log.e(TAG, "writeMessage()");
				Log.e(TAG, Log.getStackTraceString(e));
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
				Log.e(TAG, "readMessage()");
				Log.e(TAG, Log.getStackTraceString(e));
				throw e;
			}
		}else{
			return null;
		}
	}
}
