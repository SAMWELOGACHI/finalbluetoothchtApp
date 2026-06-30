package com.example.finalbluetoothchtapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivAvatar;
    private EditText etName;
    private EditText etAbout;
    private String currentBase64Avatar = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ivAvatar = findViewById(R.id.ivAvatar);
        etName = findViewById(R.id.etName);
        etAbout = findViewById(R.id.etAbout);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabChangePhoto).setOnClickListener(v -> pickImage());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveProfile());

        loadCurrentProfile();
    }

    private void loadCurrentProfile() {
        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
        String savedName = prefs.getString("display_name", "");
        String savedAbout = prefs.getString("about", "Hey there! I am using BT Chat.");
        currentBase64Avatar = prefs.getString("avatar_base64", "");

        etName.setText(savedName);
        etAbout.setText(savedAbout);

        if (!currentBase64Avatar.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(currentBase64Avatar, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivAvatar.setImageBitmap(decodedByte);
                ivAvatar.setPadding(0, 0, 0, 0); // Remove padding when there's an image
                ivAvatar.setImageTintList(null); // Remove placeholder tint
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

                    // Compress and scale image for Bluetooth transmission
                    int size = 300; // Profile pics can be small
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(selectedImage, size, size, true);
                    
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    byte[] imageBytes = baos.toByteArray();
                    currentBase64Avatar = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

                    ivAvatar.setImageBitmap(scaledBitmap);
                    ivAvatar.setPadding(0, 0, 0, 0);
                    ivAvatar.setImageTintList(null);
                    
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String about = etAbout.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a display name", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("display_name", name);
        editor.putString("about", about);
        editor.putString("avatar_base64", currentBase64Avatar);
        editor.apply();

        Toast.makeText(this, "Profile Saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
