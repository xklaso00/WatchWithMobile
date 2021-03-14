package cz.vutbr.feec.watchwithmobile;

import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;
import android.view.View;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.Node;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class MainActivity extends WearableActivity {
    static {
        System.loadLibrary("native-lib");
    }
    utils utils= new utils();
    EccOperations eccOperations=new EccOperations();
    private TextView textView;
    Button talkButton;
    byte[] randPoint=null;
    byte[] signedHash=null;
    boolean recieve=true;
    boolean doISend=true;
    final static String TAG= "WatchMainApp";
    private long allStart;
    private long allEnd;


    public MainActivity() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView =  findViewById(R.id.text);
        talkButton =  findViewById(R.id.talkClick);

//Create an OnClickListener//

        /*talkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(doISend) {
                    doISend=false;
                    allStart=System.nanoTime();
                    String onClickMessage = "Communication started... ";
                    textView.setText(onClickMessage);

                    long start = System.nanoTime();
                    int[] intRandPoint = randPoint();
                    randPoint = utils.intArrtoByteArr(intRandPoint);
                    randPoint = utils.reverseByte(randPoint);
                    long end = System.nanoTime();
                    Log.i(TAG, "Communication started...");
                    Log.i(TAG, "Generating random point on watch in C took " + (end - start)/1000000 + " ms");
                    Log.i(TAG, "Random point is: "+utils.bytesToHex(eccOperations.getCompPointFromCord(randPoint)));
                    int[] intRandNUm = randReturn();
                    byte[] randNum = utils.intArrtoByteArr(intRandNUm);
                    randNum = utils.reverseByte32(randNum);
                    eccOperations.setRand(new BigInteger(1, randNum));
                    byte[] RandPointResponse = eccOperations.getCompPointFromCord(randPoint);
                    //long start= System.nanoTime();
                    // randPoint= eccOperations.getRandPoint();

                    // long end = System.nanoTime();
                    // Log.i(TAG,"Generating random point on watch in Java took "+ (end-start)+ " ns");
                    String datapath = "/path1";

                    new SendMessage(datapath, RandPointResponse).start();

                }


            }
        });*/

       //local broadcast receiver
        IntentFilter newFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, newFilter);
    }


    //receiver to receive LocalBroadcasts from messageService Class
    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //if (recieve==false)
                //return;

            if (intent.getStringExtra("path").equals("path1")){
                int[] intRandPoint = randPoint();
                randPoint = utils.intArrtoByteArr(intRandPoint);
                randPoint = utils.reverseByte(randPoint);

                int[] intRandNUm = randReturn();
                byte[] randNum = utils.intArrtoByteArr(intRandNUm);
                randNum = utils.reverseByte32(randNum);
                eccOperations.setRand(new BigInteger(1, randNum));
                byte[] RandPointResponse = eccOperations.getCompPointFromCord(randPoint);
                String datapath = "/path3";
                new SendMessage(datapath, RandPointResponse).start();
                return;
            }
            else if(intent.getStringExtra("path").equals("path2"))
            {
                byte[] hash= intent.getByteArrayExtra("message");
                long startTime=System.nanoTime();
                signedHash= eccOperations.SignHash(hash);
                long endTime= System.nanoTime();
                Log.i(TAG,"Signing of the hash on watch took "+(endTime-startTime)+" ns");
                Log.i(TAG,"Signature is "+utils.bytesToHex(signedHash));
                new SendMessage("/path2", signedHash).start();
                Log.i(TAG,"Signature has been sent.");
                allEnd=System.nanoTime();
                Log.i(TAG,"Communication on my end took "+(allEnd-allStart)/1000000+" ms");
                textView.setText("Communication ended.");
                return;
            }
            else if(intent.getStringExtra("path").equals("path3"))
            {
                int[] intRandPoint = randPoint();
                randPoint = utils.intArrtoByteArr(intRandPoint);
                randPoint = utils.reverseByte(randPoint);

                int[] intRandNUm = randReturn();
                byte[] randNum = utils.intArrtoByteArr(intRandNUm);
                randNum = utils.reverseByte32(randNum);
                eccOperations.setRand(new BigInteger(1, randNum));
                byte[] RandPointResponse = eccOperations.getCompPointFromCord(randPoint);
                String datapath = "/path3";
                new SendMessage(datapath, RandPointResponse).start();
                return;
            }



        }
    }

    class SendMessage extends Thread {
        String path;
        byte[] message;


        SendMessage(String p, byte [] m) {
            path = p;
            message = m;
        }


        public void run() {

//send to connected node/s if there are multiple

            Task<List<Node>> nodeListTask = Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {

                List<Node> nodes = Tasks.await(nodeListTask);

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
    public native int[] randPoint();
    public native int [] randReturn();
}


