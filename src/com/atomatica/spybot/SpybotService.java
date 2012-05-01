package com.atomatica.spybot;

import java.io.*;
import java.net.*;
import java.security.InvalidParameterException;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import android_serialport_api.SerialPort;

public class SpybotService extends Service {
    protected static final String TAG = "Spybot Service";
    
    protected SpybotApplication mApplication;

    protected SerialThread mSerialThread;
    protected SerialPort mSerialPort;
    protected InputStream mSerialInput;
    protected OutputStream mSerialOutput;
    
    protected NetworkThread mNetworkThread;
    protected Socket mNetworkPort;
    protected BufferedReader mNetworkInput;
    protected PrintWriter mNetworkOutput;

    protected SerialSendingThread mSerialSendingThread;
    
    class SerialSendingThread extends Thread {
        @Override
        public void run() {
            Log.d(TAG, "Serial sending thread running");
            
            while (!isInterrupted()) {
                if (mSerialOutput != null) {
                    try {
                        mSerialOutput.write(Protocol.maintain);
                        mSerialOutput.write((byte)0);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }

                try {
                    Thread.sleep(500);
                }
                catch (InterruptedException e) {
                }
            }
        }
    }
    
    class SerialThread extends Thread {
        @Override
        public void run() {
        	Log.d(TAG, "Serial thread running");
        	
        	try {
        	    while(!isInterrupted()) {
                    byte[] buffer = new byte[64];
                    int size = mSerialInput.read(buffer);
                    if (size > 0) {
                        String message = new String(buffer, 0, size);
                        
                        Log.d(TAG, "arduino - " + message);
                        
                        // TODO: update status on Monitor Activity
                        /*runOnUiThread(new Runnable() {
                            public void run() {
                                if (mSerialText != null) {
                                    mSerialText.setText(message);
                                }
                            }
                        });*/
                    }
                }
        	}

            catch (EOFException e) {
                Log.d(TAG, "Connection interrupted with Arduino");
            }
        	
            catch (IOException e) {
                Log.e(TAG, "Error reading from serial port");
            }
        	
        	finally {
                mApplication.closeSerialPort();
                Log.d(TAG, "Closing serial port");
        	}
        }
    }

    class NetworkThread extends Thread {
        @Override
        public void run() {
        	Log.d(TAG, "Network thread running");
            
            try {
                mNetworkPort = new Socket(InetAddress.getByName(
                        mApplication.getServerName()), mApplication.getServerPortNumber());
                Log.d(TAG, "Connected to server");
            }
            
            catch (IOException e) {
                Log.e(TAG, "Could not connect to server " + mApplication.getServerName());
                return;
            }

            try {
                mNetworkInput = new BufferedReader(new InputStreamReader(mNetworkPort.getInputStream()));
                mNetworkOutput = new PrintWriter(mNetworkPort.getOutputStream(), true);
                Log.d(TAG, "Got server I/O streams");
            }

            catch (IOException e) {
                Log.e(TAG, "Error getting I/O streams");
            }
            
            try {
                mNetworkOutput.println("ANNOUNCE " + mApplication.getSpybotName() + " " + mApplication.getSpybotPassphrase());
                
                while (!isInterrupted()) {
                    String message = mNetworkInput.readLine();
                    if (message == null) {
                        Log.d(TAG, "Connection interrupted with server");
                        break;
                    }

                    Log.d(TAG, "server - " + message);
                    
                    String tokens[] = message.split("\\s");
                    if (tokens[0].equals(Protocol.protocolHeader)) {
                        // TODO: parse response code
                    }
                    
                    else {
                        byte value = 0;
                        if (tokens.length == 2) {
                            value = (byte)Integer.parseInt(tokens[1]);
                        }
                        
                        if (tokens[0].equals("LED1")) {
                            mSerialOutput.write(Protocol.led1);
                            mSerialOutput.write(value);
                        }

                        else if (tokens[0].equals("LED2")) {
                            mSerialOutput.write(Protocol.led2);
                            mSerialOutput.write(value);
                        }

                        else if (tokens[0].equals("LED3")) {
                            mSerialOutput.write(Protocol.led3);
                            mSerialOutput.write(value);
                        }

                        else if (tokens[0].equals("SERVO")) {
                            mSerialOutput.write(Protocol.servo);
                            mSerialOutput.write(value);
                        }

                        else if (tokens[0].equals("LEFTMOTOR")) {
                            mSerialOutput.write(Protocol.leftMotor);
                            mSerialOutput.write(value);
                        }

                        else if (tokens[0].equals("RIGHTMOTOR")) {
                            mSerialOutput.write(Protocol.rightMotor);
                            mSerialOutput.write(value);
                        }

                        else if (tokens[0].equals("MAINTAIN")) {
                            mSerialOutput.write(Protocol.maintain);
                            mSerialOutput.write(value);
                        }
                    }
                }
            }

            catch (EOFException e) {
                Log.d(TAG, "Connection with server interrupted");
            }

            catch (IOException e) {
                Log.e(TAG, "Error communicating with server");
            }
            
            finally {
                try {
                    mNetworkInput.close();
                    mNetworkOutput.close();
                    mNetworkPort.close();
                    Log.d(TAG, "Closed network connection");
                }
                
                catch (IOException e) {
                    Log.e(TAG, "Error closing network connection");
                }
            }
        }
    }
    
    @Override
    public void onCreate() {
        mApplication = (SpybotApplication)getApplication();
        
        // setup serial connection
        try {
            mSerialPort = mApplication.getSerialPort();
            Log.d(TAG, "Opened serial port");
            
            mSerialInput = mSerialPort.getInputStream();
            mSerialOutput = mSerialPort.getOutputStream();
            Log.d(TAG, "Got serial port I/O streams");
        }
        
        catch (SecurityException e) {
            Log.e(TAG, "Security error while opening serial port");
        }

        catch (InvalidParameterException e) {
            Log.e(TAG, "Configuration error while opening serial port");
        }
        
        catch (IOException e) {
            Log.e(TAG, "IO error while opening serial port");
        }
        mSerialSendingThread = new SerialSendingThread();
        mSerialSendingThread.start();
        
        // setup network connection
        mNetworkThread = new NetworkThread();
        mNetworkThread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flagsint, int startid) {
        Toast.makeText(this, "Starting Spybot Service", Toast.LENGTH_SHORT).show();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // do not allow binding
        return null;
    }
    
    @Override
    public void onDestroy() {
        Toast.makeText(this, "Stopping Spybot Service", Toast.LENGTH_SHORT).show();
        if (mSerialThread != null) {
            mSerialThread.interrupt();
        }

        if (mNetworkPort != null) {
            try {
                mNetworkPort.shutdownInput();
            }
            catch (IOException e) {
            }
        }
        
        mSerialThread = null;
        mNetworkThread = null;

        super.onDestroy();
    }
}
