package dslab.util;

import dslab.protocols.dmap.DMAPException;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;

/**
 * Cipherclass to accomodate the encryption and decryption ciphers
 */
public class CipherDMAP {

    private Cipher encryption;
    private Cipher decryption;

    private IvParameterSpec iv;

    private Key key;


    /**
     * Constructor for the symmetric ciphers
     * @param ivSize Size for the Init Vector
     * @param skSize Size of the SecretKey
     * @param algorithm Used Algorithm for the encryption
     * @throws DMAPException Throws if there a issue with the creation of the Ciphers
     */
    public CipherDMAP(int ivSize, int skSize, String algorithm) throws DMAPException {
        iv = SecurityHelper.generateIv(ivSize);

        try {
            key = SecurityHelper.generateAESKey(skSize);
        } catch (NoSuchAlgorithmException e) {
            throw new DMAPException("Algorithm doesn't exist");
        }

        encryption = SecurityHelper.generateCipher(algorithm, Cipher.ENCRYPT_MODE, key, iv);
        decryption = SecurityHelper.generateCipher(algorithm, Cipher.DECRYPT_MODE, key, iv);
    }

    /**
     * Constructor for the Cipher is the SecretKey and the IV are already known
     * @param key SecretKey
     * @param iv IV
     * @param algorithm Used Algorithm
     * @throws DMAPException Throws if there a issue with the creation of the Ciphers
     */
    public CipherDMAP(byte[] key, byte[] iv, String algorithm) throws DMAPException {
        this.iv = new IvParameterSpec(iv);
        this.key = new SecretKeySpec(key, 0, key.length, "AES");

        encryption = SecurityHelper.generateCipher(algorithm, Cipher.ENCRYPT_MODE, this.key, this.iv);
        decryption = SecurityHelper.generateCipher(algorithm, Cipher.DECRYPT_MODE, this.key, this.iv);
    }


    /**
     * Constructor for the asymmetric Ciphers
     * @param algorithm Used Algorithm
     * @param key Key for the Ciphers (Can either be a PrivateKey or a PublicKey)
     * @throws DMAPException Throws if there a issue with the creation of the Ciphers
     */
    public CipherDMAP(String algorithm, Key key) throws DMAPException{
        this.key = key;
        encryption = SecurityHelper.generateCipher(algorithm, Cipher.ENCRYPT_MODE, key);
        decryption = SecurityHelper.generateCipher(algorithm, Cipher.DECRYPT_MODE, key);
    }

    public byte[] encrypt(byte[] input) throws DMAPException{
        try {
            return encryption.doFinal(input);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new DMAPException("Error with Blocksize or Padding");
        }
    }

    public String encryptString(String input) throws DMAPException {
        return SecurityHelper.enocdeToBase64(encrypt(input.getBytes()));
    }

    public String decryptString(String input) throws DMAPException{
        try {
            return new String(decryption.doFinal(SecurityHelper.decodeBase64(input)));
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new DMAPException("Error with Blocksize or Padding");
        }
    }

    public byte[] decrypt(byte[] input) throws DMAPException{
        try {
            return decryption.doFinal(input);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new DMAPException("Error with Blocksize or Padding");
        }
    }

    public void destroy() throws DMAPException{
        try {
            if (key instanceof SecretKey && !((SecretKey) key).isDestroyed()){
                // set Key to null
                // There is no implementation for SecretKey.destroy()
                key = null;
            }else if(key instanceof PrivateKey && !((PrivateKey) key).isDestroyed()){
                ((PrivateKey) key).destroy();
            }
        } catch (DestroyFailedException e) {
            throw new DMAPException("Could not destroy secret key");
        }
    }

    public IvParameterSpec getIv() {
        return iv;
    }

    public Key getKey() {
        return key;
    }
}
