/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package secufilesender;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Random;
import javax.crypto.Cipher;


/**
 *
 * @author yinghuasun
 */
public class Security {
    private String rootPath = System.getProperty("user.dir");
     private String PRIVATE_KEY_FILE = getAbsolutePath() + "private.key";
     public  String PUBLIC_KEY_FILE = getAbsolutePath() + "public.key";
     public static final String ALGORITHM = "RSA";
     private File privateKeyFile = null;
     private File publicKeyFile = null;
    
    public Security(){
        privateKeyFile = new File(PRIVATE_KEY_FILE);
         publicKeyFile = new File(PUBLIC_KEY_FILE);
    }

    public String generateSalt() {
        char[] symbols;
        StringBuilder tmp = new StringBuilder();
        for (char ch = '0'; ch <= '9'; ++ch) {
            tmp.append(ch);
        }
        for (char ch = 'a'; ch <= 'z'; ++ch) {
            tmp.append(ch);
        }
        for (char ch = 'A'; ch <= 'Z'; ++ch) {
            tmp.append(ch);
        }
        symbols = tmp.toString().toCharArray();

        Random random = new SecureRandom();
        char[] result = new char[32];
        for (int i = 0; i < result.length; i++) {
            int randomCharIndex = random.nextInt(symbols.length);
            result[i] = symbols[randomCharIndex];
        }
        return new String(result);
    }

    
    public String getSecurePassword(String passwordToHash, String salt)
    {
        String generatedPassword = null;
        try {
            
            MessageDigest md = MessageDigest.getInstance("MD5");
            
            md.update(salt.getBytes());
            
            byte[] bytes = md.digest(passwordToHash.getBytes());
            
            StringBuilder sb = new StringBuilder();
            for(int i=0; i< bytes.length ;i++)
            {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            
            generatedPassword = sb.toString();
        } 
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return generatedPassword;
    }
    
    
    public String getKeyPath(){
       // return PRIVATE_KEY_FILE;
        String workingDir = System.getProperty("user.dir");
        return workingDir;
    }
    private String GetOS(){
        return System.getProperty("os.name");
    }
    private String getAbsolutePath(){
       String tempPath = rootPath + "\\Data\\";
        if (!GetOS().startsWith("Windows")){
            return tempPath.replaceAll("/", "\\\\/");
        }
        return tempPath;
       
    }
    /* Return Public Key file for Database storage*/
    public File GetPublicKey() throws FileNotFoundException{
        if(!publicKeyFile.exists()){
            throw new FileNotFoundException("No public Key file found");
        }
        return publicKeyFile;
    }
    
    public File getPrivateKey() throws FileNotFoundException{
        if(!privateKeyFile.exists()){
            throw new FileNotFoundException("No private Key file found");
        }
        return privateKeyFile;
    }
    
    public void deleteKeys(){
        privateKeyFile.delete();
        publicKeyFile.delete();
    }
    
   private  void createKey() {
        try {
            final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            keyGen.initialize(1024);
            final KeyPair key = keyGen.generateKeyPair();

            File privateKeyFile = new File(PRIVATE_KEY_FILE);
            File publicKeyFile = new File(PUBLIC_KEY_FILE);

            // Create files to store public and private key
//            if (privateKeyFile.getParentFile() != null) {
//                privateKeyFile.getParentFile().mkdirs();
//            }
//            privateKeyFile.createNewFile();
            try ( // Saving the Private key in a file
                    ObjectOutputStream privateKeyOS = new ObjectOutputStream(
                    new FileOutputStream(privateKeyFile))) {
                privateKeyOS.writeObject(key.getPrivate());
            }
            
            try (ObjectOutputStream publicKeyOS = new ObjectOutputStream(
                    new FileOutputStream(publicKeyFile))) {
                publicKeyOS.writeObject(key.getPublic());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
   
       public boolean isPrivateKeyPresent() {

        File privateKey = new File(PRIVATE_KEY_FILE);
      

        if (privateKey.exists()) {
            return true;
        }
        return false;
    }
       
    public void generateKey(){
        if (!isPrivateKeyPresent()) {
        // Method generates a pair of keys using the RSA algorithm and stores private key
         // in client application directory
          
                createKey();
        }
    }    
      
     public  byte[] encrypt(String text, PrivateKey key) {
        byte[] cipherText = null;
        try {
            // get an RSA cipher object and print the provider
            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            // encrypt the plain text using the public key
            cipher.init(Cipher.ENCRYPT_MODE, key);
            cipherText = cipher.doFinal(text.getBytes());
            int inputLen = text.getBytes().length;  
        ByteArrayOutputStream out = new ByteArrayOutputStream();  
        int offSet = 0;  
        byte[] cache;  
        int i = 0;  
        while (inputLen - offSet > 0) {  
            if (inputLen - offSet > 117) {  
                cache = cipher.doFinal(text.getBytes(), offSet, 117);  
            } else {  
                cache = cipher.doFinal(text.getBytes(), offSet, inputLen - offSet);  
            }  
            out.write(cache, 0, cache.length);  
            i++;  
            offSet = i * 117;  
        }  
        cipherText = out.toByteArray();  
        out.close();  
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cipherText;
    }

    /**
     * Decrypt text using private key.
     *
     * @param text :encrypted text
     * @param key :The private key
     * @return plain text
     * @throws java.lang.Exception
     */
    public  String decrypt(byte[] text, PublicKey key) {
        byte[] dectyptedText = null;
        try {
            // get an RSA cipher object and print the provider
            final Cipher cipher = Cipher.getInstance(ALGORITHM);

            // decrypt the text using the private key
            cipher.init(Cipher.DECRYPT_MODE, key);
            //dectyptedText = cipher.doFinal(text);
            
            int inputLen = text.length;  
        ByteArrayOutputStream out = new ByteArrayOutputStream();  
        int offSet = 0;  
        byte[] cache;  
        int i = 0;  
        while (inputLen - offSet > 0) {  
            if (inputLen - offSet > 128) {  
                cache = cipher.doFinal(text, offSet, 128);  
            } else {  
                cache = cipher.doFinal(text, offSet, inputLen - offSet);  
            }  
            out.write(cache, 0, cache.length);  
            i++;  
            offSet = i * 128;  
        }  
        dectyptedText= out.toByteArray();  
        out.close(); 
        

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return new String(dectyptedText);
    }
}
