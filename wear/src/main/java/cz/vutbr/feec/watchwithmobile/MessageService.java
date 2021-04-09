package cz.vutbr.feec.watchwithmobile;

import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.wearable.MessageEvent;
//import android.support.v4.content.LocalBroadcastManager;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Arrays;

public class MessageService extends WearableListenerService {
utils utils= new utils();

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
            //we can pretty much just get one message now with path 1
        if (messageEvent.getPath().equals("/path1")) {

            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            Log.i("WatchApp","I got Tv from phone ");
            byte [] TvWithSec=messageEvent.getData();
            messageIntent.putExtra("path","path1");
            byte[] Tv= Arrays.copyOfRange(TvWithSec,1,TvWithSec.length);
            byte Security=TvWithSec[0];
            messageIntent.putExtra("data",Tv);
            messageIntent.putExtra("Security",Security);
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);

        }
        else if(messageEvent.getPath().equals("/path2")){
            byte[] hashToSign=messageEvent.getData();
            Log.i("WatchApp","hash to sign is "+utils.bytesToHex(hashToSign));
            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra("message", hashToSign);
            messageIntent.putExtra("path","path2");  //pass the received data and path to mainacc with localBroadcast
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
        }
        else if(messageEvent.getPath().equals("/path3"))
        {
            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            Log.i("WatchApp","I got testing msg ");
            messageIntent.putExtra("path","path3");
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
        }
        else if(messageEvent.getPath().equals("/pathReset"))
        {
            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            Log.i("WatchApp","I got RESET msg ");
            messageIntent.putExtra("path","pathReset");
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
        }
        else {
            super.onMessageReceived(messageEvent);
        }
    }

}