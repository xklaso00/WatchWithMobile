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
        new SendMessageTEST("/path3",A_OKAY).start();
        //op.RegisterID(MYID);
        //byte[] loadedID=op.LoadID();
       // Log.i(TAG,"id is "+utils.bytesToHex(loadedID));
        Options.setSecurityLevel(1);
         //op.SaveServerKey(new BigInteger("02F72317633AED4A066FD70F0C90F8F0E8BBD4B9EAD81CD44A4F618F71",16));
         Options.setSecurityLevel(2);
        //op.SaveServerKey(new BigInteger("03CD58B4FAE7CD42D41A0AE52433143FAB6F43A15F5CD8D2B69E8F8ECDE72C2069",16));
        op.LoadServerKey();
        op.LoadKey();
        Options.LoadID();
        SendToMainAcc("IDUpdate","id");

        return START_NOT_STICKY;
    }

    long T2time;
    long Start1;
    long StartBlockWithWatch;
    long startApdu;
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

    private static final byte[] SERVERSIGCOM ={(byte)0x80,
            (byte)0x05};
    private static final byte[] SERVERSIGCOMWITHWATCH ={(byte)0x80,
            (byte)0x07};
    private static final byte[] AESTESTCOM ={(byte)0x80,
            (byte)0x06};
    private static final byte[] NOTYET ={(byte)0x88, //start of command signthis
            (byte)0x88};
    private static final byte[] REGISTER={(byte)0x80,
            (byte)0x09};
    private static final byte[] REGISTERDEV={(byte)0x80,
            (byte)0x10};
    private static final byte[] RESULTOFAUTH={(byte)0x80,
            (byte)0x20};
    private static final byte[] RESULTOFREG={(byte)0x80,
            (byte)0x21};
    private static final byte[] SERVERPUBKEYS={(byte)0x80,
            (byte)0x11};
    EccOperations eccOperations = new EccOperations();
    byte[] signedFromWatch=null;
    byte[] RandFromWatch=null;
    boolean serverLegit=false;
    byte[] halfMsg;
    byte[] keysFromWatch;
    boolean m1sent=false;
    boolean m2sent=false;
    Options op= new Options(this);

    public MyHostApduService() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        //op.LoadKey();
    }
    Bundle extras=null;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {


        // First command: select AID
        if (utils.isEqual(APDU_SELECT, commandApdu)) {
            Log.i(TAG, "incoming commandApdu: " + utils.bytesToHex(commandApdu));
            SendToMainAcc("APDUcoms","on");
            Example.end=true;
            startApdu=System.nanoTime();
            byte[] toGive=A_OKAY;
            if (!Options.isRegistered)
                return A_OKAY;
            try {
                toGive=utils.appendByteArray(Options.MYID,A_OKAY);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "APDU_SELECT triggered. Response: " + utils.bytesToHex(toGive));
            return toGive;
        }
        else if(utils.isCommand(SERVERPUBKEYS,commandApdu))
        {
            Log.i(TAG, "incoming commandApdu: " + utils.bytesToHex(commandApdu));
            op.SaveServerKeysFromCOM(commandApdu);
            Log.i(TAG,"length is "+commandApdu.length);
            return A_OKAY;
        }
        else if(utils.isCommand(REGISTER,commandApdu))
        {
            Log.i(TAG, "incoming commandApdu: " + utils.bytesToHex(commandApdu));
            byte[] newID=Arrays.copyOfRange(commandApdu,5,10);
            op.RegisterID(newID);
            byte[]comToSend;
            try {
                comToSend=eccOperations.registerDev(newID);
                Options.setSecurityLevel(0);
                op.SaveKey(new BigInteger(1,eccOperations.getPrivateKey160()));
                Options.setSecurityLevel(1);
                op.SaveKey(new BigInteger(1,eccOperations.getPrivateKey224()));
                Options.setSecurityLevel(2);
                op.SaveKey(new BigInteger(1,eccOperations.getPrivateKey256()));
                byte[] loadedID=op.LoadID();
                Log.i(TAG,"id is "+utils.bytesToHex(loadedID));
                SendToMainAcc("IDUpdate","id");
                return comToSend;

            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return A_OKAY;
        }
        else if (utils.isCommand(SERVERSIGCOM,commandApdu))
        {
            Log.i(TAG, "incoming commandApdu: " + utils.bytesToHex(commandApdu));
            byte secLevel=commandApdu[2];
            Options.setByteSecLevel(secLevel);
            try {
                byte [] ev= Arrays.copyOfRange(commandApdu,5,Options.BYTELENGHT+5);
                byte [] sv= Arrays.copyOfRange(commandApdu,Options.BYTELENGHT+5,Options.BYTELENGHT*2+5);
                byte[] timestamp=Arrays.copyOfRange(commandApdu,Options.BYTELENGHT*2+5,commandApdu.length-1);
                if(eccOperations.verifyServer(sv,ev,commandApdu,timestamp))
                {
                    Log.i(TAG,"It is super legit");
                    byte[] toGive=eccOperations.generateProof2();
                    //eccOperations.comuteInJavaProof();
                    return toGive;
                    //eccOperations.generateProof2();
                }
            }
            catch (Exception e) {
                Log.i(TAG,"Exception in verify");
                Log.i(TAG,e.getMessage());
            }

            return UNKNOWN_CMD_SW;
        }
        else if(utils.isCommand(REGISTERDEV,commandApdu)&&!Example.startedRegister)
        {
            Log.i(TAG,"I got command "+utils.bytesToHex(commandApdu));
            Log.i(TAG,"I got Start register of Watch ");
            if(Byte.compare(commandApdu[2],utils.intToHexByte(Options.MaxAltDev))>=1)
                return UNKNOWN_CMD_SW;
            //here you could implement different code for different devices, since we have Index of device in commapdu[2]
            //but we are only working with one other device
            new SendMessage("/pathRegister",A_OKAY).start();
            Example.startedRegister=true;
            return NOTYET;
        }
        else if(utils.isCommand(REGISTERDEV,commandApdu)&&Example.startedRegister)
        {
            if(!Example.gotRegister)
                return NOTYET;
            else {
                Example.startedRegister=false;
                return keysFromWatch;
            }
        }
        else if (utils.isCommand(AESTESTCOM,commandApdu))
        {
            try {
                Log.i(TAG,"Aes apdu "+utils.bytesToHex(commandApdu));
                long startAes=System.nanoTime();
                byte[] decrypted=eccOperations.decodeAESCommand(commandApdu);
                Log.i("Timer","Aes decrypt took "+(System.nanoTime()-startAes)/1000000+" ms");
                Log.i(TAG,"Decrypeted part is "+utils.bytesToHex(decrypted));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return A_OKAY;
        }
        else if (utils.isCommand(SERVERSIGCOMWITHWATCH,commandApdu)&&(!m1sent))
        {
            long T3time=System.nanoTime();
            T2time=System.nanoTime();
            Log.i(TAG, "incoming commandApdu: " + utils.bytesToHex(commandApdu));
            Log.i("Timer","To get second apdu it took "+(System.nanoTime()-startApdu)/1000000+" ms");
            StartBlockWithWatch=System.nanoTime();
            byte secLevel=commandApdu[2];
            Options.setByteSecLevel(secLevel);
            byte [] ev= Arrays.copyOfRange(commandApdu,5,Options.BYTELENGHT+5);
            byte [] sv= Arrays.copyOfRange(commandApdu,Options.BYTELENGHT+5,Options.BYTELENGHT*2+5);
            byte[] timestamp=Arrays.copyOfRange(commandApdu,Options.BYTELENGHT*2+5,commandApdu.length-1);
            try {

                if(eccOperations.verifyServer(sv,ev,commandApdu,timestamp))
                {
                    Log.i(TAG,"It is super legit");
                    serverLegit=true;
                    byte[]Tv=eccOperations.getTvPoint();
                    byte[] TvWithSec=utils.addFirstToByteArr(secLevel,Tv);
                    if(m1sent==false) {
                        m1sent=true;
                        Start1=System.nanoTime();
                        Log.i("TTimer","T3 time is  "+(System.nanoTime()-T3time)/1000000+" ms");
                        new SendMessage("/path1", TvWithSec).start();
                    }
                    m1sent=true;
                    if(Example.GotIt==false) {
                        Log.i("APDU","Sending notYet");
                        return NOTYET;
                    }

                }
                else return UNKNOWN_CMD_SW;
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return NOTYET;
        }
        else if(utils.isCommand(SERVERSIGCOMWITHWATCH,commandApdu)&&(m1sent)&&(!Example.GotIt))
        {
            Log.i(TAG,"NOTYET");
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
                Log.i("Timer","To sending last response it took "+(System.nanoTime()-StartBlockWithWatch)/1000000+" ms");
                Log.i("TTimer","T2 time is "+(System.nanoTime()-T2time)/1000000+" ms");
                Log.i(TAG,"FullMSG is "+utils.bytesToHex(FullMsg));
                return FullMsg;
            }
            catch (Exception e)
            {
                Log.i(TAG,"EXCEPTIon in final msg sent");
            }
            return NOTYET;

        }
        else if(utils.isCommand(RESULTOFAUTH,commandApdu))
        {
            String res;
            if (Byte.compare(commandApdu[2],(byte)0x00)==0)
                res="YES";
            else
                res="NO";
            Log.i(TAG,"Result of Authentication is "+res);
            SendToMainAcc("Result",res);
            return A_OKAY;
        }
        else if(utils.isCommand(RESULTOFREG,commandApdu))
        {
            String res;
            if (Byte.compare(commandApdu[2],(byte)0x00)==0)
                res="RYES";
            else
                res="RNO";
            Log.i(TAG,"Result of Registration is "+res);
            SendToMainAcc("Result",res);
            return A_OKAY;
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
    public void SendToMainAcc(String path, String Value)
    {
        Intent messageIntent = new Intent(); //we have to broadcast intent so create one
        messageIntent.setAction(Intent.ACTION_SEND);
        messageIntent.putExtra("path", path);
        messageIntent.putExtra("value", Value);
        LocalBroadcastManager.getInstance(MyHostApduService.this).sendBroadcastSync(messageIntent);
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
                Log.i("WatchComs","Mess sent");
                }
                if(nodes.isEmpty())
                    Log.i("WatchComs","Couldn't connect to the watch");

            } catch (ExecutionException exception) {
            }
            catch (InterruptedException exception) {
            }
            return;
        }
    }

    class SendMessageTEST extends Thread {
        String path;
        byte[] message;

        SendMessageTEST(String p, byte[] m) {
            path = p;
            message = m;
        }
        boolean exit = false;
        public void run() {

            //Task<List<Node>> wearableList = Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            boolean updatedView=false;
            Log.i("WatchComms", "Starting connection to watch");
            while(Example.end==false) {
                Task<List<Node>> wearableList = Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
                try {

                    List<Node> nodes = Tasks.await(wearableList);
                    for (Node node : nodes) {
                        Task<Integer> sendMessageTask = Wearable.getMessageClient(MyHostApduService.this).sendMessage(node.getId(), path, message);
                    }
                    if(nodes.isEmpty())
                    {
                        Log.i("WatchComs","Couldn't connect to the watch");

                        SendToMainAcc("watchUpdate","off");
                        Example.end=true;
                        break;
                    }
                    if(!updatedView) {
                        SendToMainAcc("watchUpdate","on");

                        updatedView=true;
                    }
                    sleep(1000);

                } catch (ExecutionException exception) {
                } catch (InterruptedException exception) {
                }
            }
            Log.i("WatchComms", "Ending connection to watch");
            return;
        }
    }
    public class Receiver extends BroadcastReceiver {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getStringExtra("path").equals("2")){
                Log.i(TAG,"Received proof from watch");
                if(Example.gotSecondLBM==true)
                    return;
                Example.gotSecondLBM=true;
                signedFromWatch = intent.getByteArrayExtra("data");
                Log.i("Timer","To second watch response it took "+(System.nanoTime()-StartBlockWithWatch)/1000000+" ms");
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
                long duration1=System.nanoTime()-Start1;
                Log.i("Timer","fist communication took "+duration1/1000000+" ms");
                Example.gotFirstLBM=true;
                byte[] Tk2T1=intent.getByteArrayExtra("data");
                byte [] Tk2=Arrays.copyOfRange(Tk2T1,0,Options.BYTELENGHT+1);
                Log.i(TAG,"Tk2 is "+utils.bytesToHex(Tk2));
                byte[] T1= Arrays.copyOfRange(Tk2T1,Options.BYTELENGHT+1,Tk2T1.length);
                Log.i(TAG,"T1 is "+utils.bytesToHex(T1));
                try {
                    Start1=System.nanoTime();
                    halfMsg=eccOperations.GenerateProofWithWatch(Tk2,T1);
                    duration1=System.nanoTime();
                    Log.i("Timer","Computing of e took "+(duration1-Start1)/1000000+" ms");
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
            else if(intent.getStringExtra("path").equals("4")) {
                startTestMsg();
                new SendMessage("/pathReset",A_OKAY).start();
            }
            else if(intent.getStringExtra("path").equals("pathRegister")){
                if(Example.gotRegister)
                    return;
                Example.gotRegister=true;
                keysFromWatch=intent.getByteArrayExtra("data");
                Example.readyToSendRegister=true;

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
    public void startTestMsg()
    {
        new SendMessageTEST("/path3",A_OKAY).start();
    }
    public native int[] randPoint();
    public native int [] randReturn();
}
