package dslab.util;

import dslab.protocols.dmap.DMAPException;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;

public class CipherDMAP {

    private Cipher encryption;
    private Cipher decryption;

    private IvParameterSpec iv;

    private Key key;

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

    public CipherDMAP(byte[] key, byte[] iv, String algorithm) throws DMAPException {
        this.iv = new IvParameterSpec(iv);
        this.key = new SecretKeySpec(key, 0, key.length, "AES");

        encryption = SecurityHelper.generateCipher(algorithm, Cipher.ENCRYPT_MODE, this.key, this.iv);
        decryption = SecurityHelper.generateCipher(algorithm, Cipher.DECRYPT_MODE, this.key, this.iv);
    }

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
                ((SecretKey) key).destroy();
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
