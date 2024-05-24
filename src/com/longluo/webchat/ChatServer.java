package com.longluo.webchat;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer extends Thread {
    ServerFrame serverFrame = null;
    ServerSocket serverSocket = null;

    public boolean bServerIsRunning = false;

    public ChatServer() {
        try {
            serverSocket = new ServerSocket(Constants.SERVER_PORT);
            bServerIsRunning = true;

            serverFrame = new ServerFrame();
            getServerIP();
            System.out.println("Server Port is:" + Constants.SERVER_PORT);
            serverFrame.taLog.setText("Server has started...");
            while (true) {
                Socket socket = serverSocket.accept();
                new ServerProcess(socket, serverFrame);
            }
        } catch (BindException e) {
            System.out.println("Port in use....");
            System.out.println("Please close related programs and re-run the server！");
            System.exit(0);
        } catch (IOException e) {
            System.out.println("[ERROR] Could not start server." + e);
        }

        this.start();
    }

    public void getServerIP() {
        try {
            InetAddress serverAddress = InetAddress.getLocalHost();
            byte[] ipAddress = serverAddress.getAddress();

            serverFrame.txtServerName.setText(serverAddress.getHostName());
            serverFrame.txtIP.setText(serverAddress.getHostAddress());
            serverFrame.txtPort.setText(String.valueOf(Constants.SERVER_PORT));

            System.out.println("Server IP is:" + (ipAddress[0] & 0xff) + "."
                    + (ipAddress[1] & 0xff) + "." + (ipAddress[2] & 0xff) + "."
                    + (ipAddress[3] & 0xff));
        } catch (Exception e) {
            System.out.println("###Cound not get Server IP." + e);
        }
    }

    public static void main(String args[]) {
        new ChatServer();
    }
}
