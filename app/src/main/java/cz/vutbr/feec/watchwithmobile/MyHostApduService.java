package cz.vutbr.feec.watchwithmobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
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

import java.io.ByteArrayOutputStream;
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
        Handler handler = new Handler(looper);
        //this.registerReceiver(messageReceiver, messageFilter,null,handler);
        //we are using modified LBM that should run on second thread, with classic LBM program gets stuck
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter,looper);
        new SendMessage("/path3",A_OKAY).start();

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
    private static final byte[] RANDWATCH ={(byte)0x80,
            (byte)0x04};
    private static final byte[] SERVERSIGCOM ={(byte)0x80,
            (byte)0x05};
    private static final byte[] SERVERSIGCOMWITHWATCH ={(byte)0x80,
            (byte)0x07};
    private static final byte[] AESTESTCOM ={(byte)0x80,
            (byte)0x06};
    private static final byte[] NOTYET ={(byte)0x88, //start of command signthis
            (byte)0x88};
    EccOperations eccOperations = new EccOperations();
    byte[] signedFromWatch=null;
    byte[] RandFromWatch=null;
    boolean serverLegit=false;
    byte[] halfMsg;
    boolean m1sent=false;
    boolean m2sent=false;
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

        else if (utils.isCommand(SERVERSIGCOM,commandApdu))
        {
            Log.i(TAG, "incoming commandApdu: " + utils.bytesToHex(commandApdu));
            byte [] ev= Arrays.copyOfRange(commandApdu,5,37);
            byte [] sv= Arrays.copyOfRange(commandApdu,37,69);
            try {
                if(eccOperations.verifyServer(sv,ev,commandApdu))
                {
                    Log.i(TAG,"It is super legit");
                    return eccOperations.generateProof2();
                     //eccOperations.generateProof2();
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return NOTYET;
        }
        else if (utils.isCommand(AESTESTCOM,commandApdu))
        {
            try {
                byte[] decrypted=eccOperations.decodeAESCommand(commandApdu);
                Log.i(TAG,"Decrypeted part is "+utils.bytesToHex(decrypted));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return A_OKAY;
        }
        else if (utils.isCommand(SERVERSIGCOMWITHWATCH,commandApdu)&&(!m1sent))
        {
            Log.i(TAG, "incoming commandApdu: " + utils.bytesToHex(commandApdu));

            byte [] ev= Arrays.copyOfRange(commandApdu,5,Options.BYTELENGHT+5);
            byte [] sv= Arrays.copyOfRange(commandApdu,Options.BYTELENGHT+5,Options.BYTELENGHT*2+5);
            try {

                if(eccOperations.verifyServer(sv,ev,commandApdu))
                {
                    Log.i(TAG,"It is super legit");
                    serverLegit=true;
                    byte[]Tv=eccOperations.getTvPoint();
                    if(m1sent==false) {
                        m1sent=true;
                        new SendMessage("/path1", Tv).start();
                    }
                    m1sent=true;
                    if(Example.GotIt==false)
                        return NOTYET;
                   /* else
                    {
                        ByteArrayOutputStream outputStream= new ByteArrayOutputStream();
                        outputStream.write(halfMsg);
                        outputStream.write(signedFromWatch);
                        byte[] FullMsg=outputStream.toByteArray();
                        outputStream.close();
                        Log.i(TAG,"FullMSG is "+utils.bytesToHex(FullMsg));
                        return FullMsg;
                    }*/
                    //eccOperations.generateProof2();
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return NOTYET;
        }
        else if(utils.isCommand(SERVERSIGCOMWITHWATCH,commandApdu)&&(m1sent)&&(!Example.GotIt))
        {
            return NOTYET;
        }
        else if(utils.isCommand(SERVERSIGCOMWITHWATCH,commandApdu)&&(Example.GotIt))
        {
            try {
                ByteArrayOutputStream outputStream= new ByteArrayOutputStream();
                outputStream.write(halfMsg);
                outputStream.write(signedFromWatch);
                byte[] FullMsg=outputStream.toByteArray();
                outputStream.close();
                Log.i(TAG,"FullMSG is "+utils.bytesToHex(FullMsg));
                return FullMsg;
            }
            catch (Exception e)
            {
                Log.i(TAG,"EXCEPTIon in final msg sent");
            }
            return NOTYET;

        }
        else {

            return UNKNOWN_CMD_SW;
        }
    }
    @Override
    public void onDeactivated(int reason) {
        Log.i(TAG,"Communication is done.");
        m1sent=false;
        Example.GotIt=false;
        Example.gotSecondLBM=false;
        Example.gotFirstLBM=false;
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


    }
    public class Receiver extends BroadcastReceiver {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getStringExtra("path").equals("2")){
                Log.i(TAG,"I got it from the watch");
                if(Example.gotSecondLBM==true)
                    return;
                Example.gotSecondLBM=true;
                signedFromWatch = intent.getByteArrayExtra("data");

                Log.i(TAG,"Signed from watch is : "+utils.bytesToHex(signedFromWatch)) ;
               // long End= System.nanoTime();
               // long Duration= End-Start;
               // Log.i(TAG, "It toooooook "+Duration/1000000+"ms");
                //notifyAll();
                Example.GotIt=true;
            }
            else if(intent.getStringExtra("path").equals("1"))
            {
                Log.i(TAG,"got path1 in LBM");
                if(Example.gotFirstLBM==true)
                    return;
                Example.gotFirstLBM=true;
                Log.i(TAG,"got path1 in LBM");
                byte[] Tk2T1=intent.getByteArrayExtra("data");
                byte [] Tk2=Arrays.copyOfRange(Tk2T1,0,Options.BYTELENGHT+1);
                Log.i(TAG,"Tk2 is "+utils.bytesToHex(Tk2));
                byte[] T1= Arrays.copyOfRange(Tk2T1,Options.BYTELENGHT+1,Tk2T1.length);
                Log.i(TAG,"T1 is "+utils.bytesToHex(T1));
                try {
                    halfMsg=eccOperations.GenerateProofWithWatch(Tk2,T1);
                    new SendMessage("/path2",eccOperations.getHashForBoth()).start();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if(intent.getStringExtra("path").equals("3")) {
                RandFromWatch = intent.getByteArrayExtra("message");
                Log.i(TAG,"I got RandFromWatch");
                Example.GotRandWatch= true;
            }

        }
    }
    //just some thread test that dowsnt work, but mby it will be usefull in the future, or get deleted lol
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
