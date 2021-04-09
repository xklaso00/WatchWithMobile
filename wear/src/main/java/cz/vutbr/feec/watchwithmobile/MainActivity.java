package cz.vutbr.feec.watchwithmobile;

import android.content.BroadcastReceiver;
import android.graphics.Path;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    EccOperations eccOperations;
    private TextView textView;
    Button talkButton;
    byte[] randPoint=null;
    byte[] signedHash=null;
    boolean recieve=true;
    boolean doISend=true;
    final static String TAG= "WatchMainApp";
    private long allStart;
    private long allEnd;
    private static final byte[] A_OKAY ={ (byte)0x90,  //we send this to signalize everything is A_OKAY!
            (byte)0x00};

    public MainActivity() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView =  findViewById(R.id.text);
        talkButton =  findViewById(R.id.talkClick);
        Options op= new Options(this);
        //op.SaveKey(new BigInteger("E83FC87A037C19A2E606033F506A7035DD795F3B8E77064991EB125C234686DC",16));
        //op.SaveKey(new BigInteger("929DED4DF80925348838B9D9F73F4DBA99BF08474B8BF277BFB5BC7D",16));
        op.LoadKey();
        eccOperations=new EccOperations();
//Create an OnClickListener//
        talkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ResetComs();
            }
        });


       //local broadcast receiver
        IntentFilter newFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, newFilter);
    }
    public void ResetComs()
    {
        firstdone=false;
        seconddone=false;
        testDone=false;
        textView.setText("Communication has been reseted!");
    }

    boolean firstdone=false;
    boolean testDone= false;
    boolean seconddone=false;
    //receiver to receive LocalBroadcasts from messageService Class
    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //if (recieve==false)
                //return;

            if (intent.getStringExtra("path").equals("path1")){
                if (firstdone==true)
                    return;
                firstdone=true;
                allStart=System.nanoTime();
                Log.i(TAG,"I got path 1 in LBM");
                byte [] Tv= intent.getByteArrayExtra("data");
                byte SecurityByte=intent.getByteExtra("Security", (byte) 0x02);
                Options.setByteSecLevel(SecurityByte);
                byte[] T1=eccOperations.createT1();
                byte [] Tk2= eccOperations.createTK2(Tv);
                Log.i(TAG,"Tk2 is "+utils.bytesToHex(Tk2));
                Log.i(TAG,"T1 is "+utils.bytesToHex(T1));
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                try {
                    outputStream.write(Tk2);
                    outputStream.write(T1);
                    byte[] Tk2T1=outputStream.toByteArray();
                    outputStream.close();
                    new SendMessage("/path1",Tk2T1).start();
                    Log.i(TAG,"Tk2T1 has been sent");
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if(intent.getStringExtra("path").equals("path2"))
            {
                if (seconddone==true)
                    return;
                seconddone=true;
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
                if(testDone)
                    return;
                testDone=true;
                new SendMessage("/path3", A_OKAY).start();
                return;

            }
            else if(intent.getStringExtra("path").equals("pathReset"))
            {
                ResetComs();
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
                    Log.i(TAG,"MSG SENT FROM THREAD");
                }

            } catch (ExecutionException exception) {

            }
            catch (InterruptedException exception) {

            }
            return;
        }
    }

}


