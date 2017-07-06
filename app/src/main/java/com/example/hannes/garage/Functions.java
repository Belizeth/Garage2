package com.example.hannes.garage;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

/**
 * Created by Hannes on 28.06.2017.
 */

public class Functions {
    //msg ausgeben
    public void showMsg(String title, String msg, final boolean endProgramm, Activity activity){
        //< Message erstellen >
        AlertDialog.Builder showMsg = new AlertDialog.Builder(activity);
        showMsg.setTitle(title);
        showMsg.setMessage(msg);
        showMsg.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (endProgramm) {
                            System.exit(99);//dismiss the dialog
                        }
                    }
                });
        showMsg.setCancelable(true);

        AlertDialog msgDialog = showMsg.create();
        msgDialog.show();
    }
}
