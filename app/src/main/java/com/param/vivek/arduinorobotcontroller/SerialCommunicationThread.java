package com.param.vivek.arduinorobotcontroller;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;


import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Vivek on 4/22/2015.
 */
public class SerialCommunicationThread implements Runnable {
    UsbSerialPort sPort;
    boolean continueRunning;
    Context mContext;
    ScrollLog log;
    BlockingQueue<String> incomingEvents;
    Handler handler;

    public SerialCommunicationThread(Context mContext, ScrollLog log, BlockingQueue eventQueue, Handler handler) {
        continueRunning = true;
        this.mContext = mContext;
        this.log = log;
        this.incomingEvents = eventQueue;
        this.handler = handler;
    }

    void stopCommunication() {
        continueRunning = false;
    }

    @Override
    public void run() {
        UsbManager manager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            logItem("availableDrivers is Empty");
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
            logItem("connection is null");
            return;
        }

        // Read some data! Most have just one port (port 0).
        sPort = driver.getPorts().get(0);
        SerialInputOutputManager ioManager = new SerialInputOutputManager(sPort);
        try {
            sPort.open(connection);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            logItem("Setting parameters");
            sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            Thread t = new Thread(ioManager);
            t.start();
            int numLines = 0;
//            while(numLines < 10 && continueRunning) {
//                byte buffer[] = new byte[16];
//                int numBytesRead = sPort.read(buffer, 1000);
//                String message = "";
//                if(numBytesRead > 0) {
//                    for (byte letter : buffer) {
//                        message += (char) letter;
//                    }
//                    logItem("Read " + numBytesRead + " bytes:\t" + message);
//                    numLines++;
//                }
//            }
            logItem("Beginning loop");
            while(continueRunning) {
                logItem("Waiting for data.");
                String data = incomingEvents.take();
                logItem("Attempting to send: " + data);
//                sPort.write(data.getBytes(), 1000);
                ioManager.writeAsync(data.getBytes());
                logItem("Sent.");
            }
        } catch (IOException e) {
            logItem("IOException communicating with device:");
            logItem(e.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                sPort.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void logItem(final String message) {
        handler.post(new Runnable(){
            @Override
            public void run() {
                log.logItem(message);
            }
        });
        System.out.print(message);
    }
}
