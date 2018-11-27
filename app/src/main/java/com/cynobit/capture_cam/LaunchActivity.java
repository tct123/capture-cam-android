package com.cynobit.capture_cam;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.cynobit.capture_cam.OnActivityResultContract.*;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivityForResult(new Intent(this, MainActivity.class), RequestCodes.MAIN_ACTIVITY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RequestCodes.MAIN_ACTIVITY) {
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
