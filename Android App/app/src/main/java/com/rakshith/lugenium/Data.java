package com.rakshith.lugenium;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Data {
    public static final String PREFERENCE_NAME = "com.rakshith.lugenium";
    public static final String IP_ADDRESS_KEY = "ip_address";
    public static final String PORT_KEY = "port";
    private static final List<String> toSend = new ArrayList<>();
    private static String ip_address;
    private static boolean running = false;
    private static int port;
    private static Socket socket = new Socket();
    private static Thread sendingThread;

    public static synchronized void attemptConnecting(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket.close();
                    socket = new Socket();
                    socket.setSendBufferSize(1024);
                    socket.connect(new InetSocketAddress(getIpAddress(), getPort()), 2500);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static synchronized boolean isConnected(){
        return socket.isConnected();
    }

    public static synchronized void sendData(){
        running = false;
        try {
            if(sendingThread != null)
                sendingThread.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        running = true;
        sendingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    running = true;
                    PrintWriter outWriter = new PrintWriter(socket.getOutputStream(), true);
                    while (socket.isConnected() && running){
                        if(toSend.size() > 0) {
                            synchronized (toSend) {
                                for (String data : toSend) {
                                    outWriter.println(data);
                                }
                                toSend.clear();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        sendingThread.start();
    }

    public static void send(String data){
        synchronized (toSend) {
            if (!toSend.contains(data))
                toSend.add(data);
        }
    }

    public static int getPort() {
        return port;
    }

    public static String getIpAddress() {
        return ip_address;
    }

    public static void loadData(Context context){
        SharedPreferences preference = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        ip_address = preference.getString(IP_ADDRESS_KEY, "127.0.0.1");
        port = preference.getInt(PORT_KEY, 8080);
    }

    public static void saveData(Context context, String ipAddress, int port) {
        ip_address = ipAddress;
        Data.port = port;
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(PORT_KEY, port);
        editor.putString(IP_ADDRESS_KEY, ipAddress);
        editor.apply();
    }
}
