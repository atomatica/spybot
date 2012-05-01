package com.atomatica.spybot;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidParameterException;

import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android_serialport_api.SerialPort;
import android_serialport_api.SerialPortFinder;

public class SpybotApplication extends android.app.Application {
    public SerialPortFinder mSerialPortFinder = new SerialPortFinder();
    
    private String mSpybotName;
    private String mSpybotPassphrase;
    private String mServerName;
    private int mServerPortNumber;
    private SerialPort mSerialPort = null;
    
    public String getSpybotName() {
        mSpybotName = "spybotone";
        return mSpybotName;
    }
    
    public String getSpybotPassphrase() {
        mSpybotPassphrase = "teamspybot";
        return mSpybotPassphrase;
    }
    
    public String getServerName() {
        mServerName = "atomatica.com";
        return mServerName;
    }

    public int getServerPortNumber() {
        mServerPortNumber = 9103;
        return mServerPortNumber;
    }
    
    public SerialPort getSerialPort() throws SecurityException, IOException,
            InvalidParameterException {
        if (mSerialPort == null) {
            // get serial port parameters
            SharedPreferences sp = PreferenceManager
                    .getDefaultSharedPreferences(this);
            String path = sp.getString("DEVICE", "");
            int baudrate = Integer.decode(sp.getString("BAUDRATE", "-1"));

            // check parameters
            if ((path.length() == 0) || (baudrate == -1)) {
                throw new InvalidParameterException();
            }

            // open serial port
            mSerialPort = new SerialPort(new File(path), baudrate, 0);
        }

        return mSerialPort;
    }

    public void closeSerialPort() {
        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
    }
}
