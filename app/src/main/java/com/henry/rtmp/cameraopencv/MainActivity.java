package com.henry.rtmp.cameraopencv;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.henry.rtmp.cameraopencv.databinding.ActivityMainBinding;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0x11);
    }

    public void record(View view) {
    }
}