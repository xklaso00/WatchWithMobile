package cz.vutbr.feec.watchwithmobile;



//import android.support.v7.app.AppCompatActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;



public class MainActivity extends AppCompatActivity {
    static {
        System.loadLibrary("native-lib");
    }
    String toSend=null;
    final static String TAG="MOBILE APP";
    ImageView doneImage;
    ImageView circleImage,crossImage, crossCircle;
    AnimatedVectorDrawableCompat avdc;
    AnimatedVectorDrawable avd;
    ProgressBar APDUProgressImage;
    TextView introText;
    Button optionsBttn;

    long allTimeStart;
    long allTimeEnd;
    TextView ProgressTextView;
    Button resetBttn;
    ImageView onWatch;
    ImageView offWatch;
    Dialog optionsDialog;
    TextView IDtxt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        optionsDialog=new Dialog(this);
        doneImage=findViewById(R.id.done);
        IDtxt=findViewById(R.id.IDtext);
        crossImage=findViewById(R.id.Cross);
        circleImage=findViewById(R.id.Circle);
        crossCircle=findViewById(R.id.circleIMg);
        introText = findViewById(R.id.introText);
        ProgressTextView=findViewById(R.id.progressText);
        resetBttn=findViewById(R.id.ResetButton);
        onWatch=findViewById(R.id.watchOnline);
        offWatch=findViewById(R.id.watchOfflie);
        APDUProgressImage=findViewById(R.id.APDUProgress);
        optionsBttn=findViewById(R.id.optionsButton);
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


        resetBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Conditions.end==true)
                {
                    reWatchConnection();
                }
                Conditions.Reset();
                resetUI();
                Toast.makeText(getApplicationContext(),"Communication has been restarted",Toast.LENGTH_LONG).show();
            }
        });
        optionsBttn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                showOptions(v);

            }
        });

    }
    public void showOptions(View v) {
        optionsDialog.setContentView(R.layout.popup);
        ImageView closeThis;
        closeThis=optionsDialog.findViewById(R.id.closeWindow);
        Button deleteID;
        deleteID=optionsDialog.findViewById(R.id.DelDataBttn);
        final Button timeCheck;
        timeCheck=optionsDialog.findViewById(R.id.DisableTimeBttn);
        Button helpBttn=optionsDialog.findViewById(R.id.helpBttn);
        if(Options.timeChecking==true)
            timeCheck.setText("Disable TimeStamp Check");
        else
            timeCheck.setText("Enable TimeStamp Check");
        timeCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Options.timeChecking==true){
                    Options.enableTimeChecking(false);
                    timeCheck.setText("Enable TimeStamp Check");
                    Toast.makeText(getApplicationContext(),"Time stamp check disabled",Toast.LENGTH_LONG).show();
                }
                else{
                    Options.enableTimeChecking(true);
                    timeCheck.setText("Disable TimeStamp Check");
                    Toast.makeText(getApplicationContext(),"Time stamp check enabled",Toast.LENGTH_LONG).show();
                }

            }
        });

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle("Are you sure?");
        builder.setMessage("This will delete your registration data, you will be unable to login with this phone and will have to register again.");
        builder.setPositiveButton("Confirm",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Options.delIDForTest();
                        IDtxt.setText("");
                        Toast.makeText(getApplicationContext(),"Registration data deleted",Toast.LENGTH_LONG).show();
                    }
                });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        closeThis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                optionsDialog.dismiss();
            }
        });
        deleteID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
        helpBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HelpDialog hd= new HelpDialog();
                hd.show(getSupportFragmentManager(),"Help Dialog");
            }
        });
        optionsDialog.show();
    }
    public void resetUI()
    {
        circleImage.setVisibility(View.INVISIBLE);
        doneImage.setVisibility(View.INVISIBLE);
        crossImage.setVisibility(View.INVISIBLE);
        crossCircle.setVisibility(View.INVISIBLE);
        ProgressTextView.setText("");
        APDUProgressImage.setVisibility(View.INVISIBLE);
        introText.setVisibility(View.VISIBLE);
        Conditions.startedRegister=false;
        Conditions.gotRegister=false;
    }
    public void reWatchConnection()
    {
        Intent messageIntent = new Intent(); //we have to broadcast intent so create one
        messageIntent.setAction(Intent.ACTION_SEND);
        messageIntent.putExtra("path", "4");
        LocalBroadcastManager.getInstance(this).sendBroadcastSync(messageIntent);
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void showDone(ImageView toUse)
    {
        toUse.setVisibility(View.VISIBLE);

        Drawable drawable=toUse.getDrawable();
        if(drawable instanceof AnimatedVectorDrawableCompat)
        {
            avdc=(AnimatedVectorDrawableCompat) drawable;
            avdc.start();
        }
        else if(drawable instanceof  AnimatedVectorDrawable)
        {
            avd=(AnimatedVectorDrawable) drawable;
            avd.start();

        }
    }

    public class Receiver extends BroadcastReceiver {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getStringExtra("path").equals("watchUpdate")){
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
            else if(intent.getStringExtra("path").equals("IDUpdate")){
                if(Options.isRegistered)
                    IDtxt.setText("ID: "+utils.bytesToHex(Options.MYID));
            }
            else if(intent.getStringExtra("path").equals("Result"))
            {
                final String msgToShow;
                if(intent.getStringExtra("value").equals("YES")||intent.getStringExtra("value").equals("RYES"))
                {
                    if(intent.getStringExtra("value").equals("YES"))
                        msgToShow="Authentication successful!";
                    else
                        msgToShow="Registration Successful!";
                    runOnUiThread(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void run() {
                            APDUProgressImage.setVisibility(View.INVISIBLE);
                            ProgressTextView.setVisibility(View.VISIBLE);
                            ProgressTextView.setText(msgToShow);
                            circleImage.setVisibility(View.VISIBLE);
                            showDone(doneImage);
                        }
                    });
                }
                else {
                    if(intent.getStringExtra("value").equals("NO"))
                        msgToShow="Authentication Failed!";
                    else if(intent.getStringExtra("value").equals("RNO"))
                        msgToShow="Registration Failed!";
                    else
                        msgToShow="Could not verify server, try to disable timeStamp check";
                    runOnUiThread(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void run() {
                            APDUProgressImage.setVisibility(View.INVISIBLE);
                            crossCircle.setVisibility(View.VISIBLE);
                            ProgressTextView.setText(msgToShow);
                            showDone(crossImage);
                        }
                    });
                }
            }
            else if(intent.getStringExtra("path").equals("APDUcoms"))
            {
                if(intent.getStringExtra("value").equals("on"))
                {
                    runOnUiThread(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void run() {
                            resetUI();
                            introText.setVisibility(View.INVISIBLE);
                            ProgressTextView.setText("Authentication in progress...");
                            APDUProgressImage.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }


        }
    }

}
