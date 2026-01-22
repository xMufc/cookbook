package com.example.projektzaliczeniowy;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.widget.ImageView;
import com.bumptech.glide.Glide;

public class ImageUtils {


    // Konwertuje string Base64 na Bitmap

    public static Bitmap base64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Ładuje obraz Base64 do ImageView używając Glide
    public static void loadBase64IntoImageView(String base64String, ImageView imageView) {
        if (base64String != null && !base64String.isEmpty()) {
            Bitmap bitmap = base64ToBitmap(base64String);
            if (bitmap != null) {
                Glide.with(imageView.getContext())
                        .load(bitmap)
                        .into(imageView);
            }
        }
    }


     //Sprawdza czy string Base64 jest prawidłowy

    public static boolean isValidBase64(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            return false;
        }

        try {
            Base64.decode(base64String, Base64.DEFAULT);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}