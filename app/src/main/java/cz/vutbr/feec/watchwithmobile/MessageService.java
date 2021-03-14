package cz.vutbr.feec.watchwithmobile;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
//import android.support.v4.content.LocalBroadcastManager;
//import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;


public class MessageService extends WearableListenerService {
    @Override
    public int onStartCommand(Intent intent,int flags, int startId) {
       getLooper();
        return START_NOT_STICKY;
    }
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        //if we get message with path 1, we pass data and path value to main activity
        if (messageEvent.getPath().equals("/path1")) {

            byte[] randPoint=messageEvent.getData();
            Intent messageIntent = new Intent(); //we have to broadcast intent so create one
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra("message", randPoint);
            messageIntent.putExtra("path", "1");
            LocalBroadcastManager.getInstance(this).sendBroadcastSync(messageIntent);
            //return;
        }
        else if (messageEvent.getPath().equals("/path2")){
            byte[] signed=messageEvent.getData();
            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra("message", signed);
            messageIntent.putExtra("path", "2");
            Log.i("APDU","I got the second one in message service");
            Example.GotIt=true;
            LocalBroadcastManager.getInstance(this).sendBroadcastSync(messageIntent);
            //return;
        }
        else if (messageEvent.getPath().equals("/path3")){
            byte[] watchRandom=messageEvent.getData();
            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra("message", watchRandom);
            messageIntent.putExtra("path", "3");

            LocalBroadcastManager.getInstance(this).sendBroadcastSync(messageIntent);
            //return;
        }
        else {
            super.onMessageReceived(messageEvent);
        }
    }

}

