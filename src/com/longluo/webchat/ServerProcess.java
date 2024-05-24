package com.longluo.webchat;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServerProcess extends Thread {
    private Socket socket = null;

    private BufferedReader in;
    private PrintWriter out;

    private static Vector onlineUser = new Vector(10, 5);
    private static Vector socketUser = new Vector(10, 5);

    private String strReceive, strKey;
    private StringTokenizer st;

    private final String USERLIST_FILE = System.getProperty("user.dir") + "_user.txt";
    private ServerFrame sFrame = null;

    public ServerProcess(Socket client, ServerFrame frame) throws IOException {
        socket = client;
        sFrame = frame;

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                socket.getOutputStream())), true);
        this.start();
    }

    public void run() {
        try {
            while (true) {
                strReceive = in.readLine();
                st = new StringTokenizer(strReceive, "|");
                strKey = st.nextToken();
                if (strKey.equals("login")) {
                    login();
                } else if (strKey.equals("talk")) {
                    talk();
                } else if (strKey.equals("init")) {
                    freshClientsOnline();
                } else if (strKey.equals("reg")) {
                    register();
                }
            }
        } catch (IOException e) {
            String leaveUser = closeSocket();
            Date t = new Date();
            log(Constants.USER + leaveUser + Constants.HAD_EXIT + Constants.EXIT_TIME + t.toLocaleString());
            try {
                freshClientsOnline();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            System.out.println("[SYSTEM] " + leaveUser + " leave chatroom!");
            sendAll("talk|>>>" + leaveUser + Constants.LEAVE_ROOM);
        }
    }

    private boolean isExistUser(String name) {
        String strRead;
        try {
            FileInputStream inputfile = new FileInputStream(USERLIST_FILE);
            DataInputStream inputdata = new DataInputStream(inputfile);
            while ((strRead = inputdata.readLine()) != null) {
                StringTokenizer stUser = new StringTokenizer(strRead, "|");
                if (stUser.nextToken().equals(name)) {
                    return true;
                }
            }
        } catch (FileNotFoundException fn) {
            System.out.println("[ERROR] User File has not exist!" + fn);
            out.println("warning|Error while reading or writing file!");
        } catch (IOException ie) {
            System.out.println("[ERROR] " + ie);
            out.println("warning|Error while reading or writing file!");
        }
        return false;
    }

    private boolean isUserLogin(String name, String password) {
        String strRead;
        try {
            FileInputStream inputfile = new FileInputStream(USERLIST_FILE);
            DataInputStream inputdata = new DataInputStream(inputfile);
            while ((strRead = inputdata.readLine()) != null) {
                if (strRead.equals(name + "|" + password)) {
                    return true;
                }
            }
        } catch (FileNotFoundException fn) {
            System.out.println("[ERROR] User File has not exist!" + fn);
            out.println("warning|Error while reading or writing file!");
        } catch (IOException ie) {
            System.out.println("[ERROR] " + ie);
            out.println("warning|Error while reading or writing file!");
        }
        return false;
    }

    private void register() throws IOException {
        String name = st.nextToken(); // 得到用户名称
        String password = st.nextToken().trim();
        Date t = new Date();

        if (isExistUser(name)) {
            System.out.println("[ERROR] " + name + " Register fail!");
            out.println("warning|This user already exists, please change the name!");
        } else {
            RandomAccessFile userFile = new RandomAccessFile(USERLIST_FILE,
                    "rw");
            userFile.seek(userFile.length());
            userFile.writeBytes(name + "|" + password + "\r\n");
            log(Constants.USER + name + "registration success, " + "Registration time:" + t.toLocaleString());
            userLoginSuccess(name);
        }
    }

    private void login() throws IOException {
        String name = st.nextToken();
        String password = st.nextToken().trim();
        boolean succeed = false;
        Date t = new Date();

        log(Constants.USER + name + "Registration time is logging in..." + "\n" + "password :" + password + "\n" + "port "
                + socket + t.toLocaleString());
        System.out.println("[USER LOGIN] " + name + ":" + password + ":"
                + socket);

        for (int i = 0; i < onlineUser.size(); i++) {
            if (onlineUser.elementAt(i).equals(name)) {
                System.out.println("[ERROR] " + name + " is logined!");
                out.println("warning|" + name + "Already logged into the chat room");
            }
        }
        if (isUserLogin(name, password)) {
            userLoginSuccess(name);
            succeed = true;
        }
        if (!succeed) {
            out.println("warning|" + name + "Login failed, please check your input!");
            log(Constants.USER + name + "Login failed！" + t.toLocaleString());
            System.out.println("[SYSTEM] " + name + " login fail!");
        }
    }

    private void userLoginSuccess(String name) throws IOException {
        Date t = new Date();
        out.println("login|succeed");
        sendAll("online|" + name);

        onlineUser.addElement(name);
        socketUser.addElement(socket);

        log(Constants.USER + name + "login successful，" + "Log in time:" + t.toLocaleString());

        freshClientsOnline();
        sendAll("talk|>>>welcome " + name + " Come in and chat with us!");
        System.out.println("[SYSTEM] " + name + " login succeed!");
    }

    private void talk() throws IOException {
        String strTalkInfo = st.nextToken();
        String strSender = st.nextToken();
        String strReceiver = st.nextToken();
        System.out.println("[TALK_" + strReceiver + "] " + strTalkInfo);
        Socket socketSend;
        PrintWriter outSend;
        Date t = new Date();

        GregorianCalendar calendar = new GregorianCalendar();
        String strTime = "(" + calendar.get(Calendar.HOUR) + ":"
                + calendar.get(Calendar.MINUTE) + ":"
                + calendar.get(Calendar.SECOND) + ")";
        strTalkInfo += strTime;

        log(Constants.USER + strSender + "right " + strReceiver + "explain:" + strTalkInfo
                + t.toLocaleString());

        if (strReceiver.equals("All")) {
            sendAll("talk|" + strSender + " say to everyone：" + strTalkInfo);
        } else {
            if (strSender.equals(strReceiver)) {
                out.println("talk|>>>You can't talk to yourself!");
            } else {
                for (int i = 0; i < onlineUser.size(); i++) {
                    if (strReceiver.equals(onlineUser.elementAt(i))) {
                        socketSend = (Socket) socketUser.elementAt(i);
                        outSend = new PrintWriter(new BufferedWriter(
                                new OutputStreamWriter(
                                        socketSend.getOutputStream())), true);
                        outSend.println("talk|" + strSender + " 对你说："
                                + strTalkInfo);
                    } else if (strSender.equals(onlineUser.elementAt(i))) {
                        socketSend = (Socket) socketUser.elementAt(i);
                        outSend = new PrintWriter(new BufferedWriter(
                                new OutputStreamWriter(
                                        socketSend.getOutputStream())), true);
                        outSend.println("talk|你对 " + strReceiver + "说："
                                + strTalkInfo);
                    }
                }
            }
        }
    }

    private void freshClientsOnline() throws IOException {
        String strOnline = "online";
        String[] userList = new String[20];
        String useName = null;

        for (int i = 0; i < onlineUser.size(); i++) {
            strOnline += "|" + onlineUser.elementAt(i);
            useName = " " + onlineUser.elementAt(i);
            userList[i] = useName;
        }

        sFrame.txtNumber.setText("" + onlineUser.size());
        sFrame.lstUser.setListData(userList);
        System.out.println(strOnline);
        out.println(strOnline);
    }

    private void sendAll(String strSend) {
        Socket socketSend;
        PrintWriter outSend;
        try {
            for (int i = 0; i < socketUser.size(); i++) {
                socketSend = (Socket) socketUser.elementAt(i);
                outSend = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socketSend.getOutputStream())),
                        true);
                outSend.println(strSend);
            }
        } catch (IOException e) {
            System.out.println("[ERROR] send all fail!");
        }
    }

    public void log(String log) {
        String newlog = sFrame.taLog.getText() + "\n" + log;
        sFrame.taLog.setText(newlog);
    }

    private String closeSocket() {
        String strUser = "";
        for (int i = 0; i < socketUser.size(); i++) {
            if (socket.equals((Socket) socketUser.elementAt(i))) {
                strUser = onlineUser.elementAt(i).toString();
                socketUser.removeElementAt(i);
                onlineUser.removeElementAt(i);
                try {
                    freshClientsOnline();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sendAll("remove|" + strUser);
            }
        }
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("[ERROR] " + e);
        }

        return strUser;
    }
}