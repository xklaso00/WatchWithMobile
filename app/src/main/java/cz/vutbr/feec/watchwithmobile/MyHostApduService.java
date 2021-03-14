package cz.vutbr.feec.watchwithmobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.cardemulation.HostApduService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;
//import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class MyHostApduService extends HostApduService {
    static {
        System.loadLibrary("native-lib");
    }
    @Override
    public int onStartCommand(Intent intent,int flags, int startId) {
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MyHostApduService.Receiver messageReceiver = new MyHostApduService.Receiver();
        HandlerThread handlerThread = new HandlerThread("ht");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        //Handler handler = new Handler(looper);
        //this.registerReceiver(messageReceiver, messageFilter,null,handler);
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter,looper);
        return START_NOT_STICKY;
    }


    long Start;
    private static final String TAG= "APDUSERVICE";
    private static final byte[] UNKNOWN_CMD_SW = { (byte)0x00,
            (byte)0x00};
    private static final byte[] APDU_SELECT = {
            (byte)0x00,
            (byte)0xA4,
            (byte)0x04,
            (byte)0x00,
            (byte)0x07,
            (byte)0xF0, (byte)0x39, (byte)0x41, (byte)0x48, (byte)0x14, (byte)0x81, (byte)0x00, // APP name, for now from example, not unique
            (byte)0x00
    };
    private static final byte[] A_OKAY ={ (byte)0x90,  //we send this to signalize everything is A_OKAY!
            (byte)0x00};
    private static final byte[] GIVERAND ={(byte)0x80, //start of command instuction giverand
            (byte)0x01};
    private static final byte[] SIGNTHIS ={(byte)0x80, //start of command signthis
            (byte)0x02};
    private static final byte[] WATCHTHIS ={(byte)0x80, //start of command signthis
            (byte)0x03};
    private static final byte[] RANDWATCH ={(byte)0x80, //start of command signthis
            (byte)0x04};
    private static final byte[] NOTYET ={(byte)0x88, //start of command signthis
            (byte)0x88};
    EccOperations eccOperations = new EccOperations();
    byte[] signedFromWatch=null;
    byte[] RandFromWatch=null;
    public MyHostApduService() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
    }
    Bundle extras=null;

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {


        // First command: select AID
        if (utils.isEqual(APDU_SELECT, commandApdu)) {
            Log.i(TAG, "incoming commandApdu: " + utils.bytesToHex(commandApdu));
            Log.i(TAG, "APDU_SELECT triggered. Response: " + utils.bytesToHex(A_OKAY));

            return A_OKAY;

        }

        //reader wants random point, generate it and return it as byte
        else if (utils.isCommand(GIVERAND,commandApdu))
        {

            SendMessage sm=new SendMessage("/path3", GIVERAND);
            Log.i(TAG, "incoming commandApdu: " + utils.bytesToHex(commandApdu));
            long start=System.nanoTime(); //benchmarking
            int [] randIntArr=randPoint(); //calling C function randPoint()
            byte[] randPoint = utils.intArrtoByteArr(randIntArr); //conversion of int array to byte array
            randPoint=utils.reverseByte(randPoint); //reversing bytes to correct order
            int[] randNumArr=randReturn(); // C function returns the random number it used to generate random point
            byte[] randNum=utils.intArrtoByteArr(randNumArr);

            randNum=utils.reverseByte32(randNum);

            new SendMessage("/path1", randNum).start();
            eccOperations.setRand(new BigInteger(1,randNum)); //we will use that random number to generate proof
            byte [] RandPointResponse = eccOperations.getCompPointFromCord(randPoint);
            try {
                RandPointResponse=utils.appendByteArray(RandPointResponse,A_OKAY);
            } catch (IOException e) {
                e.printStackTrace();
            }

            long end=System.nanoTime();

            Log.i(TAG,"In C generating random number and calculating random point took "+(end-start)/1000000+" ms") ;
            Log.i(TAG,"APDU_COMMAND_0x01 triggered. Response: "+utils.bytesToHex(RandPointResponse));
            return RandPointResponse;
        }
        else if (utils.isCommand(RANDWATCH,commandApdu)&&Example.GotRandWatch==false)
        {
            Log.i(TAG, "incoming commandApdu: " + utils.bytesToHex(commandApdu));
            return NOTYET;
        }
        else if (utils.isCommand(RANDWATCH,commandApdu)&&Example.GotRandWatch==true)
        {
            Log.i(TAG, "incoming commandApdu: " + utils.bytesToHex(commandApdu));
            return RandFromWatch;
        }
        //reader wants us to sign the challenge
        else if (utils.isCommand(SIGNTHIS,commandApdu))
        {
            Log.i(TAG, "incoming commandApdu: " + utils.bytesToHex(commandApdu));
            //we take the data from the command, it's the hash we need to sign
            byte [] hash= Arrays.copyOfRange(commandApdu,5,37);
            Log.i(TAG,"APDU_COMMAND_0x02 triggered. Hash to sign is: "+utils.bytesToHex(hash)) ;


            SendMessage sm=new SendMessage("/path2", hash);
            sm.start();
            try {
                sm.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Start=System.nanoTime(); //for use of benchmarking
            final byte [] SignResponse = eccOperations.SignHash(hash);



            Log.i(TAG,"Response to 0x02: "+utils.bytesToHex(SignResponse)) ;
            return SignResponse;
        }
        else if (utils.isCommand(WATCHTHIS,commandApdu)&& Example.GotIt==false){
            return NOTYET ;
        }
        else if (utils.isCommand(WATCHTHIS,commandApdu)&& Example.GotIt==true)
        {

            if (signedFromWatch!=null)
            {
                Log.i(TAG,"Response to 0x03: "+utils.bytesToHex(signedFromWatch)) ;
                return signedFromWatch;
            }
            else
                Log.i(TAG,"it was not done in time ") ;
                return null;
        }

        else {

            return UNKNOWN_CMD_SW;
        }
    }
    @Override
    public void onDeactivated(int reason) {
        Log.i(TAG,"Communication is done.");
    }
    class Waiter extends  Thread{
        public void run()
        {

        }
    }

    class SendMessage extends Thread {
        String path;
        byte[] message;

        SendMessage(String p, byte[] m) {
            path = p;
            message = m;
        }
        boolean exit = false;
        public void run() {

            Task<List<Node>> wearableList = Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {

                List<Node> nodes = Tasks.await(wearableList);
                for (Node node : nodes) {
                    Task<Integer> sendMessageTask = Wearable.getMessageClient(MyHostApduService.this).sendMessage(node.getId(), path, message);
                Log.i("APDU","Mess sent");
                }

            } catch (ExecutionException exception) {
            }
            catch (InterruptedException exception) {
            }
           /*IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
            MyHostApduService.SendMessage.Receiver messageReceiver = new MyHostApduService.SendMessage.Receiver();
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(messageReceiver, messageFilter);*/

            return;

        }

        /*public class Receiver extends BroadcastReceiver {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getStringExtra("path").equals("2")){
                    signedFromWatch = intent.getByteArrayExtra("message");
                    Log.i(TAG,"I got it from the watch");
                    Log.i(TAG,"Signed from watch is : "+utils.bytesToHex(signedFromWatch)) ;
                    long End= System.nanoTime();
                    long Duration= End-Start;
                    Log.i(TAG, "It took "+Duration/1000000+"ms");
                    GotIt=true;
                    return;

                }

            }
        }*/
    }
    public class Receiver extends BroadcastReceiver {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getStringExtra("path").equals("2")){
                signedFromWatch = intent.getByteArrayExtra("message");
                Log.i(TAG,"I got it from the watch");
                Log.i(TAG,"Signed from watch is : "+utils.bytesToHex(signedFromWatch)) ;
                long End= System.nanoTime();
                long Duration= End-Start;
                Log.i(TAG, "It toooooook "+Duration/1000000+"ms");
                //notifyAll();
                Example.GotIt=true;
            }
            else if(intent.getStringExtra("path").equals("3")) {
                RandFromWatch = intent.getByteArrayExtra("message");
                Log.i(TAG,"I got RandFromWatch");
                Example.GotRandWatch= true;
            }

        }
    }
    /*public void waitForBroadcast() throws InterruptedException {
        final CountDownLatch gate = new CountDownLatch(1);
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);

        HandlerThread handlerThread = new HandlerThread("ht");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();

        final BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getStringExtra("path").equals("2")){
                    signedFromWatch = intent.getByteArrayExtra("message");
                    Log.i(TAG,"I got it from the watch");
                    Log.i(TAG,"Signed from watch is : "+utils.bytesToHex(signedFromWatch)) ;
                    long End= System.nanoTime();
                    long Duration= End-Start;
                    Log.i(TAG, "It toooooook "+Duration/1000000+"ms");
                    //notifyAll();
                    Example.GotIt=true;
                }
                gate.countDown();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(br, messageFilter,looper);
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(br);
        //InstrumentationRegistry.getTargetContext().registerReceiver(br, new IntentFilter("broadcast.action.TO_WAIT"));

        gate.await(5, TimeUnit.MILLISECONDS);
        //InstrumentationRegistry.getTargetContext().unregisterReceiver(br);
        //assertThat("broadcast's not broadcasted!", gate.getCount(), is(0L));
    }*/

    public native int[] randPoint();
    public native int [] randReturn();
}
