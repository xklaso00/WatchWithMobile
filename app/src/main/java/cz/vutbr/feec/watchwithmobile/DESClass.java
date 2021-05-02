package cz.vutbr.feec.watchwithmobile;

import android.os.Build;

import androidx.annotation.RequiresApi;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DESClass {
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static byte[] encrypt(byte[] plaintext, SecretKey key, byte[] IV) throws Exception
    {
        // Get Cipher Instance
        Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");

        // Create SecretKeySpec
        SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "DESede");

        // Create GCMParameterSpec
       // GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, IV);

        // Initialize Cipher for ENCRYPT_MODE
        IvParameterSpec iv = new IvParameterSpec(IV);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);

        // Perform Encryption
        byte[] cipherText = cipher.doFinal(plaintext);

        return cipherText;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static byte[] decrypt(byte[] cipherText, SecretKey key, byte[] IV) throws Exception
    {
        // Get Cipher Instance
        Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");

        // Create SecretKeySpec
        SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "DESede");

        // Create GCMParameterSpec
        //GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, IV);
        IvParameterSpec iv = new IvParameterSpec(IV);
        // Initialize Cipher for DECRYPT_MODE
        cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);

        // Perform Decryption
        byte[] decryptedText = cipher.doFinal(cipherText);

        return decryptedText;
    }
}
