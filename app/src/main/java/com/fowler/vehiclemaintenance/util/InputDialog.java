package com.fowler.vehiclemaintenance.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;

public class InputDialog {

    private AlertDialog.Builder builder;
    private EditText input;

    public InputDialog(Context context, String title, int inputType) {
        builder = new AlertDialog.Builder(context);
        builder.setTitle(title);

        input = new EditText(context);
        input.setInputType(inputType);
        builder.setView(input);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
    }

    public void setOkClickListener(final OkClickListener listener) {
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.okClicked(input.getText().toString());
            }
        });
    }

    public void show() {
        builder.show();
    }
}
