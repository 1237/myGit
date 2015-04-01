/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package secufilesender;

import java.io.*;
import java.net.*;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.sql.DataSource;

public class Server {

    @Resource(name = "jdbc/chatDataBase")
    String dbURL = "jdbc:derby://localhost:1527/chatDataBase";
    private DataSource ds;
    Connection dbCon = null;
    Security secure = new Security();
    File publicKey = null;
    File privateKey = null;
    boolean started = false;
    ServerSocket ss = null;
    List<Client> clients = new ArrayList<Client>();
    boolean isconnected = false;

    public static void main(String[] args) {
        Server cs = new Server();

        cs.start();
    }

    class Client implements Runnable {

        private Socket s = null;
        private DataInputStream dis = null;
        private DataOutputStream dos = null;
        private InputStream privateFIS = null;
        private InputStream publicIS = null;

        private String id;

        public Socket getS() {
            return s;
        }

        public void setS(Socket s) {
            this.s = s;
        }

        public String getName() {
            return id;
        }

        public Client(Socket s) {
            this.s = s;
            try {
                dis = new DataInputStream(s.getInputStream());
                dos = new DataOutputStream(s.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void send(String str) {
            try {
                dos.writeUTF(str);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(id + " already closed");
                //clients.remove(this);
            }
        }

        public void send(byte[] b, int len) {
            try {
                dos.write(b, 0, len);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(id + " already closed");
                //clients.remove(this);
            }
        }

        @Override
        public void run() {
            try {

                while (isconnected) {
                    String str = dis.readUTF();

                    System.out.println(str);
                    if (str.equals("/exit")) {
                        clients.remove(this);
                    } else {

                        if (str.length() >= 9) {
                            if (str.substring(0, 9).equals("register_")) {
                                dos.writeUTF(isRegister(str));
                                //sendPublicKeyToAllClient(id, publicIS);
                            } else if (str.substring(0, 6).equals("login_")) {
                                dos.writeUTF(isLogin(str));
                            } else {
                                sendMessage(str);
                            }

                        } else if (str.length() >= 6) {
                            if (str.substring(0, 6).equals("login_")) {
                                dos.writeUTF(isLogin(str));
                            } else {
                                sendMessage(str);
                            }
                        } else {
                            System.out.println("lalalal   " + str);

                            sendMessage(str);
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(id + " already closed");
                /*
                 * try { dis.close(); s.close(); isconnected = false; } catch
                 * (IOException e1) { e1.printStackTrace(); }
                 */
            } finally {
                try {
                    if (s != null) {
                        s.close();
                        s = null;
                    }
                    if (dis != null) {
                        dis.close();
                    }
                    if (dos != null) {
                        dos.close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println(id + " already closed !!!!!!!!");
                }
            }

        }

        private void sendMessage(String str) throws IOException {
            for (int i = 0; i < clients.size(); i++) {
                Client c = clients.get(i);
                c.send(str);
            }
            byte[] b = new byte[1024];
            int len = dis.read(b);
            for (int i = 0; i < clients.size(); i++) {
                Client c = clients.get(i);
                c.send(b, len);
            }
        }

        private void sendKeys(String username, String str) throws IOException, SQLException {

            if (dis.readUTF().equals("ok")) {

                PreparedStatement privateKeyQuery = dbCon.prepareStatement("select private_key, public_key from userinfo where user_id=?");
                privateKeyQuery.setString(1, username);
                ResultSet resultSet = privateKeyQuery.executeQuery();
                while (resultSet.next()) {
                    InputStream privateKey = resultSet.getBinaryStream(1);
                    byte[] bufFile = new byte[1024];
                    int len = 0;
                    len = privateKey.read(bufFile);
                    dos.write(bufFile, 0, len);
                    if (str.equals("register")) {
                        InputStream publicKey = resultSet.getBinaryStream(2);
                        byte[] bufFile2 = new byte[1024];
                        int len2 = 0;
                        len2 = publicKey.read(bufFile2);
                        for (int i = 0; i < clients.size() - 1; i++) {
                            Client c = clients.get(i);
                            System.out.println("Client id: " + c.getName());
                            c.send(username);
                            c.dos.write(bufFile2, 0, len2);

                        }
                    }
                }

            }

            if (dis.readUTF().equals("PrivateDone")) {
                PreparedStatement publicKeysQuery = dbCon.prepareStatement("select user_id, public_key from userinfo");
                ResultSet resultSet = publicKeysQuery.executeQuery();

                while (resultSet.next()) {
                    send("continue");

                    String name = resultSet.getString(1);
                    InputStream is = resultSet.getBinaryStream(2);
                    send(name);
                    if (dis.readUTF().equals("ok")) {
                        byte[] bufFile = new byte[1024];
                        int len = 0;
                        len = is.read(bufFile);
                        dos.write(bufFile, 0, len);
                    }

                }
                send("done");
            }

        }

        private String isRegister(String str) throws SQLException, IOException {

            String userId = str.substring(9);

            id = userId;
            dbCon = DriverManager.getConnection(dbURL, "app", "app");
            System.out.println("1");
//            if (ds == null) {
//                throw new SQLException("No Data Source");
//            }
//            Connection conn = ds.getConnection();
            if (dbCon == null) {
                throw new SQLException("No Connection");
            }

            try {
                dbCon.setAutoCommit(false);

                PreparedStatement userIDQuery = dbCon.prepareStatement(
                        "SELECT user_id from userinfo WHERE user_id = ?");
                userIDQuery.setString(1, userId);

                ResultSet result = userIDQuery.executeQuery();

                if (result.next()) {

                    return "exist";
                    //return;
                } else {

                    send("continue");
                    String password = dis.readUTF();
                    System.out.println(password);
                    String salt = secure.generateSalt();
                    System.out.println(salt);
                    int hashcode = secure.getSecurePassword(password, salt).hashCode();
                    String hashcodeToString = hashcode + "";
                    System.out.println(hashcode);
                    secure.generateKey();
                    publicKey = secure.GetPublicKey();
                    publicIS = new FileInputStream(publicKey);
                    privateKey = secure.getPrivateKey();
                    Scanner scan = new Scanner(publicKey);

                    privateFIS = new FileInputStream(privateKey);

                    PreparedStatement registerQuery = dbCon.prepareStatement(
                            "insert into userinfo values(?, ?, ?, ?, ?)");

                    registerQuery.setString(1, userId);
                    registerQuery.setString(2, salt);
                    registerQuery.setString(3, hashcodeToString);
                    registerQuery.setBlob(4, publicIS);
                    registerQuery.setBlob(5, privateFIS);

                    registerQuery.executeUpdate();
                    dbCon.commit();
                    System.out.println("new client: " + id + " is coming!!");

                    secure.deleteKeys();

                    sendKeys(userId, "register");

                    return "Successful!";

                }

            } finally {

                //dbCon.close();
            }
        }

        private String isLogin(String str) throws SQLException {

            String userId = str.substring(6);
            id = userId;
            dbCon = DriverManager.getConnection(dbURL, "app", "app");
//            if (ds == null) {
//                throw new SQLException("No Data Source");
//            }
//            Connection conn = ds.getConnection();
            if (dbCon == null) {
                throw new SQLException("No Connection");
            }

            try {
                dbCon.setAutoCommit(false);

                PreparedStatement userIDQuery = dbCon.prepareStatement(
                        "SELECT * from userinfo WHERE user_id = ?");
                userIDQuery.setString(1, userId);

                ResultSet result = userIDQuery.executeQuery();

                if (result.next()) {
                    send("continue");
                    String password = dis.readUTF();
                    int hashcode = result.getInt("hash_code");
                    String salt = result.getString("salt");
                    int hashcode2 = secure.getSecurePassword(password, salt).hashCode();
                    if (hashcode2 == hashcode) {
                        System.out.println(id + " is coming back!");
                        sendKeys(userId, "login");
                        return "Successful!";
                    } else {
                        return "password_incorrect";
                    }
                } else {
                    return "userId_incorrect";
                }

            } catch (Exception e) {
                return "failed";
            } finally {
                //dbCon.close();
            }

        }
    }

    public void start() {
        try {
            ss = new ServerSocket(8878);
            started = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            while (started) {
                Socket s = ss.accept();
                Client c = new Client(s);
                clients.add(c);
                isconnected = true;
                System.out.println(s);
                // c = new Client(s);
                new Thread(c).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                ss.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

}
