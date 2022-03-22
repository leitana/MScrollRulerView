package com.lx.mscrollrulerview;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    private ScrollRulerView rulerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rulerView = (ScrollRulerView) findViewById(R.id.rulerView);
        rulerView.setCurrentValue(5);
    }
}