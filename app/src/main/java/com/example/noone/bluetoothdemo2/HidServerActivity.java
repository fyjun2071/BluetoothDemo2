package com.example.noone.bluetoothdemo2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.example.noone.bluetoothdemo2.gatt.HidProfile;

public class HidServerActivity extends AppCompatActivity {

    private HidProfile hidProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hid_server);
    }

    public void sendKeyCode(View view) {
        EditText editText = findViewById(R.id.keycode);
        String text = editText.getText().toString();
        hidProfile.sendKeyCode(Byte.parseByte(text));
    }

    public void startService(View view) {
        hidProfile = new HidProfile(this);
        hidProfile.startService();
    }

    public void stopService(View view) {
        hidProfile.stopService();
    }
}
