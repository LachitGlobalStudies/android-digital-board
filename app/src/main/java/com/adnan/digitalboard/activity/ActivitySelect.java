package com.adnan.digitalboard.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.adnan.digitalboard.R;



public class ActivitySelect extends AppCompatActivity {
private Button wtboard,help;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.select_activity);

        wtboard=findViewById(R.id.whiteboard);
        help=findViewById(R.id.help);
        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(ActivitySelect.this, HelpActivity.class);
                startActivity(intent);
            }
        });
        wtboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialogSelect();
               // Toast.makeText(getApplicationContext(),"White Board is clicked",Toast.LENGTH_SHORT).show();
            }
        });



    }

    private void showDialogSelect() {
        Dialog dialog=new Dialog(this);
        dialog.setContentView(R.layout.dialog_select);
        // Set the dialog window background to be transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        ImageView portraitmode= dialog.findViewById(R.id.portrait);
        ImageView landscapemode=dialog.findViewById(R.id.landscape);
        portraitmode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent= new Intent(ActivitySelect.this,MainActivity.class);
                intent.putExtra("orientation", ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                startActivity(intent);
                dialog.dismiss();
            }
        });
        landscapemode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(ActivitySelect.this, MainActivity.class);
                intent.putExtra("orientation",ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                startActivity(intent);
                dialog.dismiss();
            }
        });
        dialog.show(); // Show the dialog
    }
}