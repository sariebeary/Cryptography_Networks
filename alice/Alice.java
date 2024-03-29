/*
 Name: Sarah Helen Bednar
 Student number: A0179788X
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.net.Socket;
import java.security.PublicKey;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;
import javax.crypto.SealedObject;

/**
 * ********************************************************************
 * This skeleton program is prepared for *lazy and average students. * * If you
 * are very strong in programming, DIY! * * Feel free to modify this program. *
 * *******************************************************************
 */
// Alice knows Bob's public key
// Alice sends Bob session (AES) key
// Alice receives messages from Bob, decrypts and saves them to file
class Alice { // Alice is a TCP  client

    private ObjectOutputStream toBob;   // to send session key to Bob
    private ObjectInputStream fromBob;  // to read encrypted messages from Bob
    private Crypto crypto;        // object for encryption and decryption
    public static final String MESSAGE_FILE = "msgs.txt"; // file to store messages

    public static void main(String[] args) {

        // Check if the number of command line argument is 2
        if (args.length != 2) {
            System.err.println("Usage: java Alice BobIP BobPort");
            System.exit(1);
        }

        new Alice(args[0], args[1]);
    }

    // Constructor
    public Alice(String ipStr, String portStr) {

        this.crypto = new Crypto();

        Socket connectionSkt = null;     // socket used to talk to Bob

        try {
            connectionSkt = new Socket(ipStr, Integer.parseInt(portStr));
        } catch (IOException ioe) {
            System.out.println("Error creating connection socket");
            System.exit(1);
        }

        try {
            this.toBob = new ObjectOutputStream(connectionSkt.getOutputStream());
            this.fromBob = new ObjectInputStream(connectionSkt.getInputStream());
        } catch (IOException ioe) {
            System.out.println("Error: cannot get input/output streams");
            System.exit(1);
        }
        // Send session key to Bob
        sendSessionKey();

        // Receive encrypted messages from Bob,
        // decrypt and save them to file
        receiveMessages();
    }

    // Send session key to Bob
    public void sendSessionKey() {

        SealedObject sessionKeyObj = this.crypto.getSessionKey();
        try {
            toBob.writeObject(sessionKeyObj);
        } catch (IOException ex) {
            System.out.println("Error sending session key to Bob");
            System.exit(1);
        }
    }

    // Receive messages one by one from Bob, decrypt and write to file
    public void receiveMessages() {
        // How to detect Bob has no more data to send?

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(MESSAGE_FILE))) {
            while (true) {
                SealedObject encrypted_msg = (SealedObject) this.fromBob.readObject();
                String msg = this.crypto.decryptMessage(encrypted_msg);
                bw.write(msg);
            }
        } catch (IOException ex) {
            System.out.println("Eof");
            System.out.println("Message saved to file " + MESSAGE_FILE);

        } catch (ClassNotFoundException ex) {
            System.out.println("Error: cannot typecast to class SealedObject");
            System.exit(1);
        }

    }

    /**
     * **************
     */
    /**
     * inner class *
     */
    /**
     * **************
     */
    class Crypto {

        // Bob's public key, to be read from file
        private PublicKey pubKey;
        // Alice generates a new session key for each communication session
        private SecretKey sessionKey;
        // File that contains Bob' public key
        public static final String PUBLIC_KEY_FILE = "public.key";

        // Constructor
        public Crypto() {
            // Read Bob's public key from file
            readPublicKey();
            // Generate session key dynamically
            initSessionKey();
        }

        // Read Bob's public key from file
        public void readPublicKey() {
            // key is stored as an object and need to be read using ObjectInputStream.

            // See how Bob read his private key as an example.
            try {
                ObjectInputStream ois
                        = new ObjectInputStream(new FileInputStream(PUBLIC_KEY_FILE));
                this.pubKey = (PublicKey) ois.readObject();
                ois.close();
            } catch (IOException oie) {
                System.out.println("Error reading public key from file");
                System.exit(1);
            } catch (ClassNotFoundException cnfe) {
                System.out.println("Error: cannot typecast to class PublicKey");
                System.exit(1);
            }

            System.out.println("Public key read from file " + PUBLIC_KEY_FILE);
        }

        // Generate a session key
        public void initSessionKey() {
            // suggested AES key length is 128 bits
            KeyGenerator keyGen = null;
            try {
                keyGen = KeyGenerator.getInstance("AES");
            } catch (NoSuchAlgorithmException nsae) {
                System.out.println("Error: cannot generate AES key");
                System.exit(1);
            }

            keyGen.init(128); // key length is 128 bits
            sessionKey = keyGen.generateKey();

        }

        // Seal session key with RSA public key in a SealedObject and return
        public SealedObject getSessionKey() {
            SealedObject sessionKeyObj = null;
            try {
                // Alice must use the same RSA key/transformation as Bob specified
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.ENCRYPT_MODE, this.pubKey);
                byte[] key = this.sessionKey.getEncoded();
                sessionKeyObj = new SealedObject(key, cipher);

                // RSA imposes size restriction on the object being encrypted (117 bytes).
                // Instead of sealing a Key object which is way over the size restriction,
                // we shall encrypt AES key in its byte format (using getEncoded() method).
            } catch (GeneralSecurityException gse) {
                System.out.println("Error: wrong cipher to encrypt message");
                System.exit(1);
            } catch (IOException ioe) {
                System.out.println("Error creating SealedObject");
                System.exit(1);
            }
            return sessionKeyObj;

        }

        // Decrypt and extract a message from SealedObject
        public String decryptMessage(SealedObject encryptedMsgObject) {

            String plainText = null;

            // Alice and Bob use the same AES key/transformation
            try {
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, this.sessionKey);
                plainText = (String) encryptedMsgObject.getObject(cipher);
            } catch (GeneralSecurityException gse) {
                System.out.println("Error: wrong cipher to decrypt message");
                System.exit(1);
            } catch (IOException ioe) {
                System.out.println("Error receiving session key");
                System.exit(1);
            } catch (ClassNotFoundException ioe) {
                System.out.println("Error: cannot typecast to String");
                System.exit(1);
            }

            return plainText;
        }
    }
}
