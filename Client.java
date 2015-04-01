package secufilesender;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.text.*;

public class Client extends JFrame {

    TextArea ta = new TextArea();
    JTextPane tp;
    JTextField tf = new JTextField();
    Socket s = null;
    String str;
    Security se = new Security();
    String receivedMessage = "";
    DataOutputStream dos = null;
    DataInputStream dis = null;
    boolean isreceived = false;
    String username;
    StyleContext sc;
    DefaultStyledDocument doc;
    Style otherUsers;
    Style me;
    File privateKey;
    ArrayList publicKeys = new ArrayList();
    String rootPath = System.getProperty("user.dir");
    String path = null;

    public static void main(String[] args) {

        //new Register().setVisible(true);
        //new Client().LanchFrame();
    }

    public void LanchFrame() {

        JFrame f = new JFrame("welcome to UIchat 1.0!!");

        sc = new StyleContext();
        doc = new DefaultStyledDocument(sc);
        tp = new JTextPane(doc);
        otherUsers = sc.addStyle("Heading2", null);
        otherUsers.addAttribute(StyleConstants.Foreground, Color.red);
        otherUsers.addAttribute(StyleConstants.FontSize, new Integer(16));
        otherUsers.addAttribute(StyleConstants.FontFamily, "serif");
        otherUsers.addAttribute(StyleConstants.Bold, new Boolean(true));
        me = sc.addStyle("Heading2", null);
        me.addAttribute(StyleConstants.Foreground, Color.blue);
        me.addAttribute(StyleConstants.FontSize, new Integer(16));
        me.addAttribute(StyleConstants.FontFamily, "serif");
        me.addAttribute(StyleConstants.Bold, new Boolean(true));

        //add(ta, BorderLayout.NORTH);
        f.setLocation(400, 300);
        f.setSize(400, 400);
        f.getContentPane().add(new JScrollPane(tp), BorderLayout.CENTER);
        f.getContentPane().add(tf, BorderLayout.SOUTH);
        tp.setEditable(false);
        pack();

        f.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                disconnect();
//                privateKey.delete();
                System.out.println("als");
//                privateKey.deleteOnExit();
//                int count=0;
//                while(!publicKeys.isEmpty()){
//                    File f=(File) publicKeys.get(count);
//                    f.delete();
//                    publicKeys.remove(count);
//                }
                System.exit(0);

            }

            public void windowClosed(WindowEvent e) {
                new File(rootPath + "\\" + username).delete();
            }
        });
        tf.addActionListener(new TfAction());
        f.setVisible(true);
        connect();
        isreceived = true;
        ReceiveThread rt = new ReceiveThread();
        new Thread(rt).start();
        //f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

    public void connect() {
        try {
            s = new Socket("127.0.0.1", 8878);
            dos = new DataOutputStream(s.getOutputStream());
            dis = new DataInputStream(s.getInputStream());
            System.out.println("connected!");
            System.out.println(s);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class TfAction implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            str = tf.getText().trim();
            //ta.setText(str);
            tf.setText("");
            send(username);
            System.out.println(username);
            try {
                ObjectInputStream is = new ObjectInputStream(new FileInputStream(privateKey));
                PrivateKey pk = (PrivateKey) is.readObject();
                byte[] b = se.encrypt(str, pk);
                for (int i = 0; i < b.length; i++) {
                    System.out.print(b[i] + " ");
                }
                System.out.println("");
                dos.write(b);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    public void send(String str) {

        try {
            //System.out.println(s);

            dos.writeUTF(str);
            dos.flush();
            // dos.close();
        } catch (Exception ea) {
            ea.printStackTrace();
        }
    }

    private void disconnect() {
        try {

            new File(path).delete();
            System.out.println("lala");
            send("/exit");
            dos.close();
            s.close();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private class ReceiveThread implements Runnable {

        @Override
        public void run() {
            String string = null;

            try {
                while (isreceived) {

                    string = dis.readUTF();

                    byte[] b = new byte[1024];
                    int len = 0;
                    File file = new File(path + string + "_publicKey");
                    if (file.exists()) {
                        System.out.println(file.getAbsolutePath());
                        ObjectInputStream is = new ObjectInputStream(new FileInputStream(file));
                        PublicKey pk = (PublicKey) is.readObject();
                        len = dis.read(b);
                        byte[] bb = new byte[len];

                        for (int i = 0; i < len; i++) {
                            bb[i] = b[i];
                            System.out.print(b[i] + " ");
                        }
                        System.out.println("");
                        String text = se.decrypt(bb, pk);
                        doc.insertString(0, text, null);
                        if(string.equals(username)){
                        doc.insertString(0, "I says:  ", me);
                        }else{
                        doc.insertString(0, string + " says:  ", otherUsers);
                        }
                        doc.insertString(0, "\n", null);
                    } else {
                        FileOutputStream fos = new FileOutputStream(file);
                       
                        len = dis.read(b);
                        System.out.println(len);
                        fos.write(b, 0, len);
                        fos.close();

                    }

//						int divider = string.indexOf("**&&");
//						String userName = string.substring(divider+4);
//						String messageContent = string.substring(0, divider);
//						ta.getColorModel();
                    //ta.setText(ta.getText() + string + "\n");
                }
            } catch (IOException e) {
                System.out.println("exit");
                //e.printStackTrace();
                /*try {
                 dis.close();
                 dos.close();
                 s.close();
                 } catch (IOException ea) {
                 ea.printStackTrace();
                 }*/
            } catch (BadLocationException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    public String register(String userId, String password) throws IOException {

        dos = new DataOutputStream(s.getOutputStream());
        dis = new DataInputStream(s.getInputStream());
        send("register_" + userId);
        username = userId;
        String command = dis.readUTF();
        System.out.println(command);
        if (command.equals("continue")) {
            send(password);
            System.out.println("11");
            downloadPrivateKey();
            downloadPublicKeys();
            dis = new DataInputStream(s.getInputStream());
            command = dis.readUTF();

            return command;
        } else if (command.equals("exist")) {
            return command;
        } else {
            return "register failed";
        }
    }

    public String login(String userId, String password) throws IOException {
        send("login_" + userId);
        username = userId;
        String command = dis.readUTF();
        if (command.equals("continue")) {
            send(password);
            downloadPrivateKey();
            downloadPublicKeys();
            command = dis.readUTF();
            return command;
        } else if (command.equals("userId_incorrect")) {
            return command;
        } else {
            return "login failed";
        }
    }

    private void downloadPrivateKey() throws IOException {

        path = rootPath + "\\" + username + "\\";
        new File(path).mkdir();
        privateKey = new File(path + username + "_privateKey");
//        String length = dis.readUTF();
//        long total = Long.parseLong(length);
        System.out.println("2222");
        send("ok");
        System.out.println("33333");
        FileOutputStream fos = new FileOutputStream(privateKey);
        byte[] bufFile = new byte[1024];
        int len = 0;
        len = dis.read(bufFile);
        System.out.println(len);
        fos.write(bufFile, 0, len);

        dos.writeUTF("PrivateDone");
        fos.close();
        privateKey.deleteOnExit();

    }

    private void downloadPublicKeys() throws IOException {
        System.out.println("333");
        while (!dis.readUTF().equals("done")) {
            //send("user_id");
            System.out.println("555");
            String name = dis.readUTF();
            System.out.println("444");

            File file = new File(path + name + "_publicKey");
            FileOutputStream fos = new FileOutputStream(file);
            send("ok");
            byte[] bufFile = new byte[1024];
            int len = 0;
            len = dis.read(bufFile);
            System.out.println(len);
            fos.write(bufFile, 0, len);
            publicKeys.add(file);
            fos.close();
            file.deleteOnExit();

        }

    }
}
