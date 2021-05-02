package cz.vutbr.feec.watchwithmobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.math.BigInteger;

public class Options {
    public static int SECURITY_LEVEL=2;
    public static int BYTELENGHT=32;
    private static BigInteger SecKey256;
    public static String TAG="Options";
    private static BigInteger SecKey224;
    private static BigInteger SecKey160;
    private static boolean keysLoaded=false;
    private Context context;
    public Options(Context context)
    {
        this.context=context;
    }
    public static  void setSecurityLevel(int level)
    {
        if (level>-1&&level<53)
        {
            SECURITY_LEVEL=level;
        }
        switch (SECURITY_LEVEL){
            case 1:
                BYTELENGHT=28;
                break;
            case 0:
                BYTELENGHT=20;
                break;
            default:
                BYTELENGHT=32;
                break;
        }
    }
    public void SaveKey(BigInteger privateKey)
    {
        keysLoaded=false;
        SharedPreferences sharedPref = context.getSharedPreferences("cz.vutbr.feec.watchwithmobile.Watchkeys", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        switch (SECURITY_LEVEL){
            case 0:
                editor.putString("key160",utils.bytesToHex(utils.bytesFromBigInteger(privateKey)));
                editor.commit();
                break;
            case 1:
                editor.putString("key224",utils.bytesToHex(utils.bytesFromBigInteger(privateKey)));
                editor.commit();
                break;
            case 2:
                editor.putString("key256",utils.bytesToHex(utils.bytesFromBigInteger(privateKey)));
                editor.commit();
                break;
        }
        LoadKey();


    }
    public void LoadKey()
    {
        try {
            if(keysLoaded)
                return;
            keysLoaded=true;
            SharedPreferences sharedPref = context.getSharedPreferences("cz.vutbr.feec.watchwithmobile.Watchkeys", Context.MODE_PRIVATE);
            String keyString;
            keyString = sharedPref.getString("key224", String.valueOf(0));
            SecKey224 = new BigInteger(keyString, 16);
            Log.i("APDU", "I loaded the key224 it is " + utils.bytesToHex(utils.bytesFromBigInteger(SecKey224)));

            keyString = sharedPref.getString("key256", String.valueOf(0));
            SecKey256 = new BigInteger(keyString, 16);
            Log.i("APDU", "I loaded the key256 it is " + utils.bytesToHex(utils.bytesFromBigInteger(SecKey256)));
            keyString = sharedPref.getString("key160", String.valueOf(0));
            SecKey160 = new BigInteger(keyString, 16);
            Log.i("APDU", "I loaded the key160 it is " + utils.bytesToHex(utils.bytesFromBigInteger(SecKey160)));
            Log.i("APDU", "I loaded the key160 it is " + keyString);
        }
        catch ( Exception e)
        {
            Log.i("WatchKeys","Exception in load keys");
        }



    }
    public static  BigInteger getPrivateKey()
    {
        if(SECURITY_LEVEL==2)
            return SecKey256;
        else if(SECURITY_LEVEL==1)
            return SecKey224;
        else
        {
            Log.i(TAG,"GIVING KEY"+utils.bytesToHex(utils.bytesFromBigInteger(SecKey160)));
            return SecKey160;
        }

    }
    public static void setByteSecLevel(byte secByte)
    {
        if(Byte.compare(secByte,(byte)0x01)==0)
        {
            setSecurityLevel(1);
            Log.i(TAG,"secutity has been set to 1");
        }
        else if(secByte == (byte) 0x02)
        {
            setSecurityLevel(2);
            Log.i(TAG,"security has been set to 2");
        }
        else if(secByte == (byte) 0x00)
        {
            setSecurityLevel(0);
            Log.i(TAG,"security has been set to 0");
        }
    }
    public static String getHashName()
    {
        if(SECURITY_LEVEL==2)
            return "SHA-256";
        else if(SECURITY_LEVEL==0)
            return  "SHA-1";
        else
            return "SHA-224";
    }


}
