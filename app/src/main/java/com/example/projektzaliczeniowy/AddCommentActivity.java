package com.example.projektzaliczeniowy;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AddCommentActivity extends AppCompatActivity {

    private static final String TAG = "AddCommentActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_IMAGE_PICK = 102;
    private static final int REQUEST_IMAGE_CROP = 103;

    private Button takePhotoButton, selectPhotoButton, cropImageButton, submitCommentButton;
    private ImageView selectedImageView;
    private EditText commentEditText;
    private CardView imagePreviewCard;

    private Uri selectedImageUri;
    private String recipeId;

    private FirebaseAuth mAuth;
    private DatabaseReference commentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_comment);

        // Inicjalizacja widoków z layoutu
        initializeViews();
        // Konfiguracja połączenia z Firebase
        setupFirebase();
        // Ustawienie listenerów dla przycisków i pola tekstowego
        setupListeners();

        // Pobranie ID przepisu z Intentu, jeśli brak to pokazanie komunikatu i zakończenie aktywności
        recipeId = getIntent().getStringExtra("recipeId");
        if (recipeId == null) {
            SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Błąd: nie znaleziono ID przepisu");
            finish();
            return;
        }

        Log.d(TAG, "Recipe ID: " + recipeId);
    }

    // Przypisanie widoków do zmiennych
    private void initializeViews() {
        takePhotoButton = findViewById(R.id.takePhotoButton);
        selectPhotoButton = findViewById(R.id.selectPhotoButton);
        cropImageButton = findViewById(R.id.cropImageButton);
        submitCommentButton = findViewById(R.id.submitCommentButton);
        selectedImageView = findViewById(R.id.selectedImageView);
        commentEditText = findViewById(R.id.commentEditText);
        imagePreviewCard = findViewById(R.id.imagePreviewCard);
    }

    // Inicjalizacja Firebase Auth i referencji do bazy komentarzy
    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://projektzaliczeniowykonkel-default-rtdb.europe-west1.firebasedatabase.app");
        commentsRef = database.getReference("comments");
    }

    // Ustawienie obsługi kliknięć i nasłuchiwanie zmian tekstu w polu komentarza
    private void setupListeners() {
        takePhotoButton.setOnClickListener(v -> checkCameraPermissionAndTakePhoto());
        selectPhotoButton.setOnClickListener(v -> selectImageFromGallery());
        cropImageButton.setOnClickListener(v -> cropImage());
        submitCommentButton.setOnClickListener(v -> submitComment());

        commentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSubmitButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // Sprawdzenie uprawnień do aparatu i ewentualne ich zażądanie
    private void checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            takePhoto();
        }
    }

    // Uruchomienie aparatu do wykonania zdjęcia
    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    // Uruchomienie galerii do wyboru zdjęcia
    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    // Próba uruchomienia aplikacji do przycinania zdjęcia, jeśli dostępna
    private void cropImage() {
        if (selectedImageUri != null) {
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            cropIntent.setDataAndType(selectedImageUri, "image/*");
            cropIntent.putExtra("crop", "true");
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            cropIntent.putExtra("outputX", 300);
            cropIntent.putExtra("outputY", 300);
            cropIntent.putExtra("return-data", true);

            if (cropIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cropIntent, REQUEST_IMAGE_CROP);
            } else {
                SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Brak aplikacji do przycinania zdjęć");
            }
        }
    }

    // Aktualizacja stanu przycisku wysyłania komentarza w zależności od obecności tekstu i zdjęcia
    private void updateSubmitButtonState() {
        boolean hasText = !commentEditText.getText().toString().trim().isEmpty();
        boolean hasImage = selectedImageUri != null;
        submitCommentButton.setEnabled(hasText && hasImage);
    }

    // Obsługa wysłania komentarza - walidacja i rozpoczęcie procesu zapisu
    private void submitComment() {
        String commentText = commentEditText.getText().toString().trim();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Musisz być zalogowany");
            return;
        }

        if (selectedImageUri == null) {
            SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Wybierz zdjęcie");
            return;
        }

        if (commentText.isEmpty()) {
            SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Napisz komentarz");
            return;
        }

        submitCommentButton.setEnabled(false);
        submitCommentButton.setText("Dodawanie...");

        // Konwersja zdjęcia do Base64 i zapis komentarza w bazie
        convertImageToBase64AndSave(currentUser, commentText);
    }

    // Konwersja wybranego zdjęcia do Base64 i wywołanie zapisu komentarza
    private void convertImageToBase64AndSave(FirebaseUser currentUser, String commentText) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
            if (bitmap == null) {
                SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Błąd przetwarzania zdjęcia");
                resetSubmitButton();
                return;
            }

            Bitmap compressedBitmap = compressBitmap(bitmap, 800, 800);

            String base64Image = bitmapToBase64(compressedBitmap);
            if (base64Image == null) {
                SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Błąd konwersji zdjęcia");
                resetSubmitButton();
                return;
            }

            saveCommentToDatabase(currentUser, commentText, base64Image);

        } catch (IOException e) {
            Log.e(TAG, "Error processing image", e);
            SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Błąd przetwarzania zdjęcia");
            resetSubmitButton();
        }
    }

    // Skalowanie bitmapy do maksymalnych wymiarów zachowując proporcje
    private Bitmap compressBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }

        float scaleWidth = ((float) maxWidth) / width;
        float scaleHeight = ((float) maxHeight) / height;
        float scale = Math.min(scaleWidth, scaleHeight);

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    // Konwersja bitmapy do łańcucha Base64
    private String bitmapToBase64(Bitmap bitmap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error converting bitmap to Base64", e);
            return null;
        }
    }

    // Przywrócenie stanu przycisku wysyłania komentarza po błędzie
    private void resetSubmitButton() {
        submitCommentButton.setEnabled(true);
        submitCommentButton.setText("Dodaj komentarz");
    }

    // Zapis komentarza wraz ze zdjęciem w bazie Firebase
    private void saveCommentToDatabase(FirebaseUser user, String commentText, String base64Image) {
        try {
            String commentId = commentsRef.child(recipeId).push().getKey();

            if (commentId == null) {
                SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Błąd tworzenia komentarza");
                resetSubmitButton();
                return;
            }

            String userName = user.getDisplayName();
            if (userName == null || userName.isEmpty()) {
                userName = user.getEmail();
            }

            Comment comment = new Comment(
                    user.getUid(),
                    userName,
                    commentText,
                    base64Image,
                    System.currentTimeMillis()
            );

            Log.d(TAG, "Saving comment to database with ID: " + commentId);

            commentsRef.child(recipeId).child(commentId).setValue(comment)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Comment saved successfully");
                        SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Komentarz dodany!");
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save comment to database", e);
                        SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Błąd zapisywania komentarza " + e.getMessage());
                        resetSubmitButton();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception while saving comment", e);
            SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Błąd zapisywania komentarza");
            resetSubmitButton();
        }
    }

    // Obsługa wyniku żądania uprawnień do aparatu
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            } else {
                SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Potrzebne są uprawnienia do aparatu");
            }
        }
    }

    // Obsługa wyników aktywności: zdjęcie z aparatu, wybór z galerii, przycinanie
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE:
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        if (imageBitmap != null) {
                            selectedImageUri = saveBitmapToTempFile(imageBitmap);
                            displaySelectedImage();
                        }
                    }
                    break;

                case REQUEST_IMAGE_PICK:
                    selectedImageUri = data.getData();
                    displaySelectedImage();
                    break;

                case REQUEST_IMAGE_CROP:
                    Bundle cropExtras = data.getExtras();
                    if (cropExtras != null) {
                        Bitmap croppedBitmap = (Bitmap) cropExtras.get("data");
                        if (croppedBitmap != null) {
                            selectedImageUri = saveBitmapToTempFile(croppedBitmap);
                            displaySelectedImage();
                        }
                    }
                    break;
            }
        }
    }

    // Zapis bitmapy do tymczasowego pliku i zwrócenie URI
    private Uri saveBitmapToTempFile(Bitmap bitmap) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "Comment Image");
            values.put(MediaStore.Images.Media.DESCRIPTION, "Image for comment");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            ContentResolver resolver = getContentResolver();
            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                try (var outputStream = resolver.openOutputStream(uri)) {
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                        return uri;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving bitmap to file", e);
        }
        return null;
    }

    // Wyświetlenie wybranego zdjęcia w ImageView i aktualizacja stanu przycisku
    private void displaySelectedImage() {
        if (selectedImageUri != null) {
            imagePreviewCard.setVisibility(android.view.View.VISIBLE);
            Glide.with(this).load(selectedImageUri).into(selectedImageView);
            updateSubmitButtonState();
            Log.d(TAG, "Image displayed: " + selectedImageUri);
        }
    }
}