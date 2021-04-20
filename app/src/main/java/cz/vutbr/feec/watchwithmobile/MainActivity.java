package cz.vutbr.feec.watchwithmobile;



//import android.support.v7.app.AppCompatActivity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

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
    Button resetBttn;
    ImageView onWatch;
    ImageView offWatch;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textview = findViewById(R.id.textView);
        resetBttn=findViewById(R.id.ResetButton);
        onWatch=findViewById(R.id.watchOnline);
        offWatch=findViewById(R.id.watchOfflie);
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        HandlerThread handlerThread = new HandlerThread("htMA");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        Handler handler = new Handler(looper);
        //this.registerReceiver(messageReceiver, messageFilter,null,handler);
        //we are using modified LBM that should run on second thread, with classic LBM program gets stuck
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter,looper);



        //IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        //Receiver messageReceiver = new Receiver();
        //LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
        Log.i("sap", "starting");
        Intent intent=new Intent(this.getApplicationContext(),MyHostApduService.class);
        startService(intent); //start MyHostApduService

        Log.i("sap", "starting service?");
        //Options op= new Options(this);
        //op.SaveKey(new BigInteger("07B6272FC66B008BF4F772ACB2FAD14B0E08EC3A6DFD0F38716F47A8",16));
        //op.SaveKey(new BigInteger("B7E151628AED2A6ABF7158809CF4F3C762E7160F38B4DA56A784D9045190CFEF",16));
        //op.LoadKey();
        //op.SaveServerKey(new BigInteger("02F72317633AED4A066FD70F0C90F8F0E8BBD4B9EAD81CD44A4F618F71",16));
        //op.SaveServerKey(new BigInteger("03CD58B4FAE7CD42D41A0AE52433143FAB6F43A15F5CD8D2B69E8F8ECDE72C2069",16));
        //op.LoadServerKey();
        resetBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Example.end==true)
                {
                    reWatchConnection();
                }
                Example.Reset();
                Toast.makeText(getApplicationContext(),"Communication has been restarted",Toast.LENGTH_LONG).show();
            }
        });

    }
    public void reWatchConnection()
    {
        Intent messageIntent = new Intent(); //we have to broadcast intent so create one
        messageIntent.setAction(Intent.ACTION_SEND);
        messageIntent.putExtra("path", "4");
        LocalBroadcastManager.getInstance(this).sendBroadcastSync(messageIntent);
    }

    public class Receiver extends BroadcastReceiver {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getStringExtra("path").equals("watchUpdate")){
                Log.i("hello","got in main acc");
                if(intent.getStringExtra("value").equals("on")) {


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onWatch.setVisibility(View.VISIBLE);
                            offWatch.setVisibility(View.INVISIBLE);
                        }
                    });

                }
                else if(intent.getStringExtra("value").equals("off"))
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onWatch.setVisibility(View.INVISIBLE);
                            offWatch.setVisibility(View.VISIBLE);
                        }
                    });

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
    /*public void path1Response(Intent intent){
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
    }*/
    //program does this after receiving second message from watch
    
    public native boolean verSignC(int[] sign,int[] rand,int[] pub,int[]hash);
}
