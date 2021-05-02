package cz.vutbr.feec.watchwithmobile;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

public class HelpDialog extends AppCompatDialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Information")
                .setMessage("If you are having trouble make sure your NFC is enabled and that Android has control over what app is used. \n\n" +
                        "To use smart watch as your secondary device you have to have that watch linked with WearOS app and have this application running on the watch.\n\n" +
                        "To operate modes (Register, Authentication, Security levels) use the PC app, the phone will know what it is supposed to send.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        return builder.create();

    }



}
