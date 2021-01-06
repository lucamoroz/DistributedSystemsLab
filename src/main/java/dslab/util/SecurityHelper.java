package dslab.util;

import dslab.protocols.dmap.DMAPException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.IOException;
import java.security.*;
import java.util.Base64;

/**
 * Provides the security helper classes to generate Ciphers and encode/decode the resulting text
 */
public class SecurityHelper {

    /**
     * Generates a random number
     * @param length Length of the random
     * @return Random Number as byte array
     */
    public static byte[] generateRandom(int length){
        SecureRandom rdm = new SecureRandom();
        final byte[] number = new byte[length];
        rdm.nextBytes(number);
        return number;
    }

    public static SecretKey generateAESKey(int size) throws NoSuchAlgorithmException {
        KeyGenerator gene = KeyGenerator.getInstance("AES");
        gene.init(size);
        return gene.generateKey();
    }

    public static IvParameterSpec generateIv(int size) {
        byte[] iv = generateRandom(size);
        return new IvParameterSpec(iv);
    }

    /**
     * Generates a cipher used with asymmetric keyschemes
     * @param algorithm Used algorithm
     * @param mode Mode for the Cipher
     * @param key Key of the scheme
     * @return A Cipher with the given parameters
     * @throws DMAPException Throws if there is a issue with the creation of the cipher
     */
    public static Cipher generateCipher(String algorithm, int mode, Key key) throws DMAPException {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(algorithm);
            cipher.init(mode, key);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
            throw new DMAPException("Wrong Padding or Algorithm provided");
        } catch (InvalidKeyException e) {
            throw new DMAPException("Invalid key provided");
        }
        return cipher;
    }


    /**
     * Generates a cipher for symmetric encryption
     * @param algorithm Used algorithm
     * @param mode Mode for the Cipher
     * @param key Key of the scheme
     * @param iv IV for the encryption
     * @return A Cipher with the given parameters
     * @throws DMAPException Throws if there is a issue with the creation of the cipher
     */
    public static Cipher generateCipher(String algorithm, int mode, Key key, IvParameterSpec iv) throws DMAPException {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(algorithm);
            cipher.init(mode, key, iv);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new DMAPException("Wrong Padding or Algorithm provided");
        } catch (InvalidKeyException e) {
            throw new DMAPException("Invalid key provided");
        }
        return cipher;
    }

    public static PrivateKey getPrivateKey(String id) throws IOException {
        String path = "keys/server/" + id + ".der";
        File privKey = new File(path);
        PrivateKey key = Keys.readPrivateKey(privKey);
        return key;
    }

    public static PublicKey getPublicKey(String id) throws IOException{
        String compID = "keys/client/" + id + "_pub.der";
        File pubKey = new File(compID);
        PublicKey key = Keys.readPublicKey(pubKey);
        return key;
    }

    public static String enocdeToBase64(byte[] data) {
        String res = Base64.getEncoder().encodeToString(data);
        return res;
    }

    public static byte[] decodeBase64(String data){
        byte[] res = Base64.getDecoder().decode(data);
        return res;
    }

}
