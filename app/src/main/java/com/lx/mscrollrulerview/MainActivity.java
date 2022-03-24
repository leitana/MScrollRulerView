package com.lx.mscrollrulerview;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private ScrollRulerView rulerView;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        rulerView = (ScrollRulerView) findViewById(R.id.rulerView);
        rulerView.setCurrentValue(5);
        rulerView.setOnValueChangedListener(new ScrollRulerView.OnValueChangedListener() {
            @Override
            public void onValueChanged(float value) {
                if (value % 10 == 0.0) {
                    vibrator.vibrate(10);
                } else {
                    vibrator.vibrate(5);
                }

            }
        });
    }
}