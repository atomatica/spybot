package com.atomatica.spybot;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;

import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android_serialport_api.SerialPort;
import android_serialport_api.SerialPortFinder;

public class SpybotApplication extends android.app.Application {
    public SerialPortFinder mSerialPortFinder = new SerialPortFinder();
    private SerialPort mSerialPort = null;

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
