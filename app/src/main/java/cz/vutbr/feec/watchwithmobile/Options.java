package cz.vutbr.feec.watchwithmobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.math.BigInteger;
import java.util.Arrays;


public class Options {
    public static String TAG="Options";
    protected static boolean timeChecking=true;
    public static int SECURITY_LEVEL=2;
    public static int BYTELENGHT=32;
    private static BigInteger SecKey256;
    private static BigInteger ServerPubKey160;
    private static BigInteger SecKey160;
    private static BigInteger ServerPubKey256;
    private static BigInteger SecKey224;
    private static BigInteger ServerPubKey224;
    public static boolean isRegistered=false;
    public static byte[] MYID;
    public static int MaxAltDev=1;
    private static Context context;
    public Options(Context context)
    {
        this.context=context;
        //LoadKey();
    }
    public static void enableTimeChecking(boolean Enable)
    {
        timeChecking=Enable;
    }

    public static  void setSecurityLevel(int level)
    {
        if (level>-1&&level<3)
        {
            SECURITY_LEVEL=level;
        }
        switch (SECURITY_LEVEL){
            case 0:
                BYTELENGHT=20;
                break;
            case 1:
                BYTELENGHT=28;
                break;
            default:
                BYTELENGHT=32;
                break;
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static void setByteSecLevel(byte secByte)
    {
        if(Byte.compare(secByte,(byte)0x01)==0)
        {
            setSecurityLevel(1);
            Log.i(TAG,"secutity has been set to 1");
        }
        else if(Byte.compare(secByte,(byte)0x02)==0)
        {
            setSecurityLevel(2);
            Log.i(TAG,"security has been set to 2");
        }
        else if(Byte.compare(secByte,(byte)0x00)==0)
        {
            setSecurityLevel(0);
            Log.i(TAG,"security has been set to 0");
        }
    }
    public void RegisterID(byte[] ID)
    {
        SharedPreferences sharedPref = context.getSharedPreferences("cz.vutbr.feec.watchwithmobile.keys", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("ID",utils.bytesToHex(ID));
        editor.commit();
        LoadID();
    }
    public static void delIDForTest()
    {
        SharedPreferences sharedPref = context.getSharedPreferences("cz.vutbr.feec.watchwithmobile.keys", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("ID","0");
        editor.commit();
        LoadID();
    }
    public void SaveServerKeysFromCOM(byte[] commandApdu)
    {
        byte[] Pub32= Arrays.copyOfRange(commandApdu,5,38);
        byte[] Pub28=Arrays.copyOfRange(commandApdu,38,67);
        byte[] Pub20=Arrays.copyOfRange(commandApdu,67,commandApdu.length-1);
        int oldSec=SECURITY_LEVEL;
        setSecurityLevel(2);
        SaveServerKey(new BigInteger(1,Pub32));
        setSecurityLevel(1);
        SaveServerKey(new BigInteger(1,Pub28));
        setSecurityLevel(0);
        SaveServerKey(new BigInteger(1,Pub20));
        setSecurityLevel(oldSec);
        Log.i(TAG,"Server Keys saved from command");
        LoadServerKey();
    }
    public void SaveKey(BigInteger privateKey)
    {
        SharedPreferences sharedPref = context.getSharedPreferences("cz.vutbr.feec.watchwithmobile.keys", Context.MODE_PRIVATE);
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
    public static byte[] LoadID()
    {
        try {
            SharedPreferences sharedPref = context.getSharedPreferences("cz.vutbr.feec.watchwithmobile.keys", Context.MODE_PRIVATE);
            String IDString;
            IDString = sharedPref.getString("ID", String.valueOf(0));
            Log.i(TAG,"ID is "+IDString);
            if(IDString.equals("0"))
                isRegistered=false;
            else
                isRegistered=true;
            Log.i(TAG,"Registered is " +isRegistered);
            MYID=utils.hexStringToByteArray(IDString);
            return MYID;
        }
        catch (Exception e)
        {
            Log.i(TAG,"Exception in loadID");
        }
        return null;
    }
    public void LoadKey()
    {
        try {
            SharedPreferences sharedPref = context.getSharedPreferences("cz.vutbr.feec.watchwithmobile.keys", Context.MODE_PRIVATE);
            String keyString;
            //switch (SECURITY_LEVEL)
            //{
            // case 1:
            keyString = sharedPref.getString("key224", String.valueOf(0));
            SecKey224 = new BigInteger(keyString, 16);
            Log.i("APDU", "I loaded the key224 it is " + utils.bytesToHex(utils.bytesFromBigInteger2(SecKey224)));
            //break;
            //case 2:
            keyString = sharedPref.getString("key256", String.valueOf(0));
            SecKey256 = new BigInteger(keyString, 16);
            Log.i("APDU", "I loaded the key256 it is " + utils.bytesToHex(utils.bytesFromBigInteger(SecKey256)));

            keyString = sharedPref.getString("key160", String.valueOf(0));
            SecKey160 = new BigInteger(keyString, 16);
            Log.i("APDU", "I loaded the key160 it is " + utils.bytesToHex(utils.bytesFromBigInteger(SecKey160)));
            //isRegistered=true;
        }
       catch (Exception e)
       {
           Log.i(TAG,"Exception in loadKey");
       }
                //break;

        //}

    }
    public void SaveServerKey(BigInteger publicServerKey)
    {
        SharedPreferences sharedPref = context.getSharedPreferences("cz.vutbr.feec.watchwithmobile.serverKeys", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        switch (SECURITY_LEVEL){
            case 0:
                editor.putString("serverKey160",utils.bytesToHex(publicServerKey.toByteArray()));
                editor.commit();
                break;
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
        //switch (SECURITY_LEVEL)
       // {
           // case 1:
                keyString = sharedPref.getString("serverKey224", String.valueOf(0));
                ServerPubKey224 = new BigInteger(keyString, 16);
                Log.i("APDU", "I loaded the public key224 it is " + utils.bytesToHex(ServerPubKey224.toByteArray()));
               // break;
           // case 2:
                keyString = sharedPref.getString("serverKey256", String.valueOf(0));
                ServerPubKey256 = new BigInteger(keyString, 16);
                Log.i("APDU", "I loaded the public key256 it is " + utils.bytesToHex(ServerPubKey256.toByteArray()));
              //  break;
        keyString = sharedPref.getString("serverKey160", String.valueOf(0));
        ServerPubKey160 = new BigInteger(keyString, 16);
        Log.i("APDU", "I loaded the public key160 it is " + utils.bytesToHex(ServerPubKey160.toByteArray()));

       // }
    }
    public static  BigInteger getPrivateKey()
    {
        if(SECURITY_LEVEL==2)
            return SecKey256;
        else if(SECURITY_LEVEL==0)
            return  SecKey160;
        else
            return SecKey224;
    }
    public static BigInteger getServerPubKey()
    {
        if(SECURITY_LEVEL==2)
            return ServerPubKey256;
        else if(SECURITY_LEVEL==0)
            return  ServerPubKey160;
        else
            return ServerPubKey224;
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
