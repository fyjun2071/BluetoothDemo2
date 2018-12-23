package com.example.noone.bluetoothdemo2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onButtonClick(View view) {
        int buttonId = view.getId();
        Intent intent;
        switch (buttonId) {
            case R.id.button_cts:
                intent = new Intent(this, CtsServerActivity.class);
                break;
            case R.id.button_hid:
                intent = new Intent(this, HidServerActivity.class);
                break;
            default:
                intent = new Intent(this, MainActivity.class);
        }

        startActivity(intent);
    }
}
