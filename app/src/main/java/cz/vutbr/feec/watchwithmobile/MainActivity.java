package cz.vutbr.feec.watchwithmobile;



//import android.support.v7.app.AppCompatActivity;
import android.content.BroadcastReceiver;
import android.os.Build;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.widget.TextView;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.wearable.Node;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Wearable;



import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity {
    static {
        System.loadLibrary("native-lib");
    }
    String toSend=null;
    final static String TAG="MOBILE APP";

    TextView textview;
    UtilsV2 utilsV2 = new UtilsV2();
    ECCOp eccOp=new ECCOp();
    byte [] hashToSign;
    byte[] randPoint;
    boolean recive=true;
    boolean recive1=true;
    long allTimeStart;
    long allTimeEnd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textview = findViewById(R.id.textView);
        /*try {
            EccOperations ec=new EccOperations();
            ec.genertateSecKey();
        } catch (NoSuchAlgorithmException e) {
            Log.i("apdu", "mis1");
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            Log.i("apdu", "mis2");
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            Log.i("apdu", "mis3");
            e.printStackTrace();
        }*/

        // IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
       // Receiver messageReceiver = new Receiver();
       // LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
        Log.i("sap", "starting");
        Intent intent=new Intent(this.getApplicationContext(),MyHostApduService.class);
        startService(intent); //start MyHostApduService

        Log.i("sap", "starting service?");
        Options op= new Options(this);
        //op.SaveKey(new BigInteger("B7E151628AED2A6ABF7158809CF4F3C762E7160F38B4DA56A784D9045190CFEF",16));
        op.LoadKey();
        //op.SaveServerKey(new BigInteger("03CD58B4FAE7CD42D41A0AE52433143FAB6F43A15F5CD8D2B69E8F8ECDE72C2069",16));
        op.LoadServerKey();
        try {
            Test t=new Test();
            t.doSth();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }


    public class Receiver extends BroadcastReceiver {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onReceive(Context context, Intent intent) {
            if (recive==false)
                return;
            if (intent.getStringExtra("path").equals("1")){
                if (recive1==false)
                    return;
                else
                {
                    path1Response(intent);
                }

            }
            else if(intent.getStringExtra("path").equals("2")){
                if (recive==false)
                    return;
               else
                {
                    path2Response(intent);
                }
            }

        }
    }


    //thread to send message trough data layer
    class SendMessageDef extends Thread {
        String path;
        byte[] message;

        SendMessageDef(String p, byte[] m) {
            path = p;
            message = m;
        }

        public void run() {

            Task<List<Node>> wearableList = Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {

                List<Node> nodes = Tasks.await(wearableList);
                for (Node node : nodes) {
                    Task<Integer> sendMessageTask = Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), path, message);

                }

            } catch (ExecutionException exception) {
            }
            catch (InterruptedException exception) {
            }
            return;
        }
    }
    //what the program does when we get first  message from the watch
    public void path1Response(Intent intent){
        allTimeStart=System.nanoTime();
        recive1=false; //simple boolean so if anything is send more times this function will not be triggered
        //randPoint=intent.getByteArrayExtra("message");
        Log.i(TAG,"Communication started...");
        //Log.i(TAG,"Random point from watch is: "+ utilsV2.bytesToHex(randPoint));

        // hashToSign= utilsV2.generateHashToSend(randPoint,eccOp.givePub()); //generate hash that prover has to sign
        hashToSign=intent.getByteArrayExtra("message");
        Log.i(TAG,"Hash for watch to sign is: "+ utilsV2.bytesToHex(hashToSign));
        //send the hash to watch with path 1
        new SendMessageDef("/path1", hashToSign).start();


        return;
    }
    //program does this after receiving second message from watch
    public void path2Response(Intent intent)
    {
        recive=false;
        byte[] signed = intent.getByteArrayExtra("message");
        BigInteger S= new BigInteger(1,signed);
        try {
            Log.i(TAG,"Signature from watch is: "+ utilsV2.bytesToHex(signed));
            long startTime=System.nanoTime();
            boolean isLegit=eccOp.signVer(S,randPoint,eccOp.givePub(),hashToSign); //ver in java for benchmark, will not be used in final app if verify on phone is needed
            long timeAll=System.nanoTime()-startTime;
            long start2=System.nanoTime();
            int[]pubInt= utilsV2.byteArrayToItArray(utilsV2.reverseByte(eccOp.givePubDecoded()));
            int[]signInt= utilsV2.byteArrayToItArray(utilsV2.reverseByte32(signed));
            int[]hashInt= utilsV2.byteArrayToItArray(utilsV2.reverseByte32(hashToSign));
            int [] randInt= utilsV2.byteArrayToItArray(utilsV2.reverseByte(eccOp.decodeEncoded(randPoint)));

            boolean isitlegitnow=verSignC(signInt,randInt,pubInt,hashInt); //verify in C, much faster
            long end2=System.nanoTime()-start2;
            //Log.i(TAG,"is it legit in C? "+isitlegitnow);
            Log.i(TAG, "In C verification took "+end2/1000000+"ms");
            Log.i(TAG,"In java verification took "+timeAll/1000000+"ms");
            Log.i(TAG,"Is the signature valid java and C? "+isLegit+isitlegitnow);
            allTimeEnd=System.nanoTime();
            Log.i(TAG,"Communication and verification took "+(allTimeEnd-allTimeStart)/1000000+"ms");
            textview.setText("Signature is here is it legit? "+isLegit);
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public native boolean verSignC(int[] sign,int[] rand,int[] pub,int[]hash);
}
