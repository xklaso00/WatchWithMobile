package cz.vutbr.feec.watchwithmobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.math.BigInteger;



public class Options {
    public static int SECURITY_LEVEL=2;
    public static int BYTELENGHT=32;
    private static BigInteger SecKey256;
    private static BigInteger ServerPubKey256;
    private static BigInteger SecKey224;
    private static BigInteger ServerPubKey224;
    private Context context;
    public Options(Context context)
    {
        this.context=context;
    }

    private static  void setSecurityLevel(int level)
    {
        if (level>0&&level<5)
        {
            SECURITY_LEVEL=level;
        }
        switch (SECURITY_LEVEL){
            case 1:
                BYTELENGHT=28;
                break;
            default:
                BYTELENGHT=32;
                break;
        }
    }
    public void SaveKey(BigInteger privateKey)
    {
        SharedPreferences sharedPref = context.getSharedPreferences("cz.vutbr.feec.watchwithmobile.keys", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        switch (SECURITY_LEVEL){
            case 1:
                editor.putString("key224",utils.bytesToHex(utils.bytesFromBigInteger(privateKey)));
                editor.commit();
                break;
            case 2:
                editor.putString("key256",utils.bytesToHex(utils.bytesFromBigInteger(privateKey)));
                editor.commit();
                break;
        }



    }
    public void LoadKey()
    {
        SharedPreferences sharedPref = context.getSharedPreferences("cz.vutbr.feec.watchwithmobile.keys", Context.MODE_PRIVATE);
        String keyString;
        switch (SECURITY_LEVEL)
        {
            case 1:
                keyString = sharedPref.getString("key224", String.valueOf(0));
                SecKey224 = new BigInteger(keyString, 16);
                Log.i("APDU", "I loaded the key224 it is " + utils.bytesToHex(utils.bytesFromBigInteger(SecKey224)));
                break;
            case 2:
                keyString = sharedPref.getString("key256", String.valueOf(0));
                SecKey256 = new BigInteger(keyString, 16);
                Log.i("APDU", "I loaded the key256 it is " + utils.bytesToHex(utils.bytesFromBigInteger(SecKey256)));
                break;

        }

    }
    public void SaveServerKey(BigInteger publicServerKey)
    {
        SharedPreferences sharedPref = context.getSharedPreferences("cz.vutbr.feec.watchwithmobile.serverKeys", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        switch (SECURITY_LEVEL){
            case 1:
                editor.putString("serverKey224",utils.bytesToHex(publicServerKey.toByteArray()));
                editor.commit();
                break;
            case 2:
                editor.putString("serverKey256",utils.bytesToHex(publicServerKey.toByteArray()));
                editor.commit();
                break;
        }
    }
    public void LoadServerKey()
    {
        SharedPreferences sharedPref = context.getSharedPreferences("cz.vutbr.feec.watchwithmobile.serverKeys", Context.MODE_PRIVATE);
        String keyString;
        switch (SECURITY_LEVEL)
        {
            case 1:
                keyString = sharedPref.getString("serverKey224", String.valueOf(0));
                ServerPubKey224 = new BigInteger(keyString, 16);
                Log.i("APDU", "I loaded the public key224 it is " + utils.bytesToHex(ServerPubKey224.toByteArray()));
                break;
            case 2:
                keyString = sharedPref.getString("serverKey256", String.valueOf(0));
                ServerPubKey256 = new BigInteger(keyString, 16);
                Log.i("APDU", "I loaded the public key256 it is " + utils.bytesToHex(ServerPubKey256.toByteArray()));
                break;

        }
    }
    public static  BigInteger getPrivateKey()
    {
        if(SECURITY_LEVEL==2)
            return SecKey256;
        else
            return SecKey224;
    }
    public static BigInteger getServerPubKey()
    {
        if(SECURITY_LEVEL==2)
            return ServerPubKey256;
        else
            return ServerPubKey224;
    }
}