package com.example.projektzaliczeniowy;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.mlkit.nl.translate.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

public class RecipeDetailActivity extends AppCompatActivity {

    private static final int REQUEST_ADD_COMMENT = 1001;

    // Deklaracja widoków UI
    TextView recipeName, textCalories, textTime;
    ImageView recipeImage;
    RatingBar recipeRatingBar;
    LinearLayout ingredientsLayout, nutritionLayout, instructionsLayout, commentsLayout;
    Button favoriteButton, addCommentButton;

    // Firebase Auth i referencje do bazy danych
    FirebaseAuth mAuth;
    DatabaseReference favoritesRef, commentsRef;

    // Identyfikator przepisu oraz stan ulubionych
    String recipeId;
    boolean isFavorite = false;

    // Tłumacz ML Kit
    private Translator translator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        // Inicjalizacja widoków z layoutu
        recipeName = findViewById(R.id.recipeName);
        recipeImage = findViewById(R.id.recipeImage);
        textCalories = findViewById(R.id.textCalories);
        textTime = findViewById(R.id.textTime);
        recipeRatingBar = findViewById(R.id.recipeRatingBar);
        favoriteButton = findViewById(R.id.favoriteButton);
        addCommentButton = findViewById(R.id.addCommentButton);
        ingredientsLayout = findViewById(R.id.ingredientsLayout);
        nutritionLayout = findViewById(R.id.nutritionLayout);
        instructionsLayout = findViewById(R.id.instructionsLayout);
        commentsLayout = findViewById(R.id.commentsLayout);

        // Inicjalizacja Firebase Auth i referencji do bazy danych
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://projektzaliczeniowykonkel-default-rtdb.europe-west1.firebasedatabase.app");
        favoritesRef = database.getReference("favorites");
        commentsRef = database.getReference("comments");

        // Pobranie danych przekazanych przez Intent
        String name = getIntent().getStringExtra("name");
        float rating = getIntent().getFloatExtra("rating", 0f);
        String imageUrl = getIntent().getStringExtra("imageUrl");
        String recipeJson = getIntent().getStringExtra("recipeJson");
        recipeId = getIntent().getStringExtra("recipeId");

        // Ustawienie podstawowych danych przepisu w widokach
        recipeName.setText(name);
        recipeRatingBar.setRating(rating * 5); // Przeliczenie oceny na skalę 5-gwiazdkową
        Glide.with(this).load(imageUrl).into(recipeImage); // Załadowanie obrazu za pomocą Glide

        // Konfiguracja przycisku ulubionych, przycisku dodawania komentarza,
        // załadowanie komentarzy oraz wyświetlenie przepisu z tłumaczeniem
        setupFavoriteButton();
        setupAddCommentButton();
        loadComments();
        setupTranslationAndDisplayRecipe(recipeJson);
    }

    /**
     * Konfiguracja przycisku ulubionych:
     * - Sprawdzenie czy użytkownik jest zalogowany
     * - Pobranie stanu ulubionych z bazy Firebase
     * - Obsługa kliknięcia do dodania/usunięcia przepisu z ulubionych
     */
    private void setupFavoriteButton() {
        if (mAuth.getCurrentUser() == null) {
            favoriteButton.setVisibility(View.GONE); // Ukrycie przycisku jeśli brak zalogowanego użytkownika
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference userFavoriteRef = favoritesRef.child(userId).child(recipeId);

        // Sprawdzenie czy przepis jest już w ulubionych
        userFavoriteRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isFavorite = snapshot.exists();
                updateFavoriteButton(); // Aktualizacja wyglądu przycisku
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Obsługa kliknięcia przycisku ulubionych
        favoriteButton.setOnClickListener(v -> {
            if (isFavorite) {
                userFavoriteRef.removeValue(); // Usunięcie z ulubionych
                isFavorite = false;
            } else {
                Log.d("FIREBASE", "recipeId: " + recipeId);
                Log.d("FIREBASE", "Ścieżka do bazy: " + userFavoriteRef.toString());

                userFavoriteRef.setValue(true).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FIREBASE", "Zapisano do ulubionych.");
                    } else {
                        Log.e("FIREBASE", "Błąd zapisu", task.getException());
                    }
                });
                isFavorite = true;
            }
            updateFavoriteButton(); // Odświeżenie wyglądu przycisku
        });
    }

    /**
     * Konfiguracja przycisku dodawania komentarza:
     * - Sprawdzenie czy użytkownik jest zalogowany
     * - Uruchomienie aktywności dodawania komentarza z przekazaniem ID przepisu
     * - Wyświetlenie komunikatu w Snackbarze jeśli użytkownik nie jest zalogowany
     */
    private void setupAddCommentButton() {
        addCommentButton.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Musisz być zalogowany aby dodać komentarz");
                return;
            }

            Intent intent = new Intent(this, AddCommentActivity.class);
            intent.putExtra("recipeId", recipeId);
            startActivityForResult(intent, REQUEST_ADD_COMMENT);
        });
    }

    /**
     * Załadowanie komentarzy z bazy Firebase:
     * - Usunięcie poprzednich widoków komentarzy
     * - Jeśli brak komentarzy, wyświetlenie informacji o braku
     * - Dla każdego komentarza utworzenie widoku i dodanie do layoutu
     */
    private void loadComments() {
        commentsRef.child(recipeId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                commentsLayout.removeAllViews();

                if (!snapshot.exists()) {
                    TextView noCommentsView = new TextView(RecipeDetailActivity.this);
                    noCommentsView.setText("Brak komentarzy. Dodaj pierwszy!");
                    noCommentsView.setTextColor(Color.GRAY);
                    noCommentsView.setPadding(16, 16, 16, 16);
                    commentsLayout.addView(noCommentsView);
                    return;
                }

                for (DataSnapshot commentSnapshot : snapshot.getChildren()) {
                    Comment comment = commentSnapshot.getValue(Comment.class);
                    if (comment != null) {
                        addCommentView(comment, commentSnapshot.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("COMMENTS", "Błąd ładowania komentarzy", error.toException());
            }
        });
    }

    /**
     * Utworzenie widoku pojedynczego komentarza:
     * - Ustawienie danych użytkownika, tekstu i daty
     * - Załadowanie i obsługa zdjęcia w komentarzu (jeśli jest)
     * - Dodanie możliwości usunięcia komentarza przez właściciela
     * - Dodanie widoku do layoutu komentarzy
     */
    @SuppressLint("ClickableViewAccessibility")
    private void addCommentView(Comment comment, String commentId) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View commentView = inflater.inflate(R.layout.comment_item, commentsLayout, false);

        ImageView userImage = commentView.findViewById(R.id.userImage);
        TextView userName = commentView.findViewById(R.id.userName);
        TextView commentText = commentView.findViewById(R.id.commentText);
        TextView commentDate = commentView.findViewById(R.id.commentDate);
        Button deleteButton = commentView.findViewById(R.id.deleteCommentButton);

        userName.setText(comment.getUserName());
        commentText.setText(comment.getCommentText());

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        Date date = new Date(comment.getTimestamp());
        commentDate.setText(sdf.format(date));

        if (comment.getImageBase64() != null && !comment.getImageBase64().isEmpty()) {
            if (ImageUtils.isValidBase64(comment.getImageBase64())) {
                ImageUtils.loadBase64IntoImageView(comment.getImageBase64(), userImage);

                // Obsługa kliknięcia na zdjęcie - powiększenie
                userImage.setOnClickListener(v -> {
                    Intent intent = new Intent(RecipeDetailActivity.this, FullScreenImageActivity.class);
                    intent.putExtra("imageBase64", comment.getImageBase64());
                    intent.putExtra("userName", comment.getUserName());
                    startActivity(intent);
                });

                // Efekt wizualny przy dotknięciu zdjęcia (zmiana przezroczystości)
                userImage.setOnTouchListener((v, event) -> {
                    switch (event.getAction()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            userImage.setAlpha(0.7f);
                            break;
                        case android.view.MotionEvent.ACTION_UP:
                        case android.view.MotionEvent.ACTION_CANCEL:
                            userImage.setAlpha(1.0f);
                            break;
                    }
                    return false;
                });

            } else {
                Log.e("IMAGE", "Nieprawidłowy format Base64 dla komentarza");
                userImage.setImageResource(R.drawable.circle_background);
            }
        } else {
            userImage.setImageResource(R.drawable.circle_background);
        }

        // Pokazanie przycisku usuwania tylko dla właściciela komentarza
        if (mAuth.getCurrentUser() != null &&
                mAuth.getCurrentUser().getUid().equals(comment.getUserId())) {
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(v -> deleteComment(commentId));
        }

        commentsLayout.addView(commentView);
    }

    /**
     * Usunięcie komentarza z bazy Firebase:
     * - Pokazanie snackbar z informacją o sukcesie lub błędzie
     */
    private void deleteComment(String commentId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete entry")
                .setMessage("Czy na pewno chcesz usunąć komentarz?")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        commentsRef.child(recipeId).child(commentId).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Komentarz usunięty");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("DELETE_COMMENT", "Błąd usuwania komentarza", e);
                                    SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Błąd usuwania komentarza");
                                });
                    }
                })

                .setNegativeButton(android.R.string.cancel, null)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .show();

    }

    /**
     * Inicjalizacja tłumacza ML Kit i rozpoczęcie pobierania modelu tłumaczenia
     * Po pobraniu modelu wywołanie metody tłumaczącej i wyświetlającej przepis
     * W przypadku błędu pobrania modelu wyświetlenie przepisu bez tłumaczenia
     */
    private void setupTranslationAndDisplayRecipe(String recipeJson) {
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.POLISH)
                .build();
        translator = Translation.getClient(options);

        translator.downloadModelIfNeeded()
                .addOnSuccessListener(unused -> {
                    Log.d("TRANSLATE", "Model tłumaczenia pobrany.");
                    try {
                        JSONObject recipe = new JSONObject(recipeJson);
                        translateAndDisplayRecipe(recipe);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TRANSLATE", "Błąd pobierania modelu tłumaczenia", e);
                    try {
                        JSONObject recipe = new JSONObject(recipeJson);
                        displayRecipeWithoutTranslation(recipe);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
    }

    /**
     * Tłumaczenie i wyświetlanie przepisu:
     * - Wyświetlenie kalorii i czasu bez tłumaczenia
     * - Tłumaczenie i wyświetlanie składników, wartości odżywczych i instrukcji krok po kroku
     * - Każdy element jest dodawany dynamicznie do odpowiednich layoutów
     */
    private void translateAndDisplayRecipe(JSONObject recipe) {
        try {
            int calories = recipe.optJSONObject("nutrition").optInt("calories", 0);
            int time = recipe.optInt("total_time_minutes", 0);
            runOnUiThread(() -> {
                textCalories.setText(calories + " kcal");
                textTime.setText(time + " min");
            });

            JSONArray ingredients = recipe.optJSONArray("sections")
                    .optJSONObject(0).optJSONArray("components");
            if (ingredients != null) {
                for (int i = 0; i < ingredients.length(); i++) {
                    JSONObject ingredient = ingredients.getJSONObject(i);
                    String rawText = ingredient.optString("raw_text");
                    translateText(rawText, translatedText -> {
                        runOnUiThread(() -> {
                            TextView textView = new TextView(this);
                            textView.setText("• " + translatedText);
                            textView.setTextColor(Color.WHITE);
                            ingredientsLayout.addView(textView);
                        });
                    });
                }
            }

            JSONObject nutrition = recipe.optJSONObject("nutrition");
            if (nutrition != null) {
                Iterator<String> keys = nutrition.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (key.equals("calories") || key.equals("updated_at")) continue;
                    String value = nutrition.optString(key);
                    String textToTranslate;
                    if(key.equals("fiber")){
                        textToTranslate = "Błonnik: " + value;
                    }
                    else{
                        textToTranslate = key + ": " + value;
                    }
                    translateText(textToTranslate, translatedText -> {
                        runOnUiThread(() -> {
                            TextView textView = new TextView(this);
                            textView.setText(translatedText);
                            textView.setTextColor(Color.WHITE);
                            nutritionLayout.addView(textView);
                        });
                    });
                }
            }

            JSONArray instructions = recipe.optJSONArray("instructions");
            if (instructions != null) {
                for (int i = 0; i < instructions.length(); i++) {
                    JSONObject step = instructions.getJSONObject(i);
                    String displayText = step.optString("display_text");
                    int stepNumber = i + 1;
                    translateText(displayText, translatedText -> {
                        runOnUiThread(() -> {
                            TextView stepView = new TextView(this);
                            stepView.setText(stepNumber + ". " + translatedText);
                            stepView.setTextColor(Color.WHITE);
                            instructionsLayout.addView(stepView);
                        });
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Wyświetlanie przepisu bez tłumaczenia:
     * - Wyświetlenie kalorii i czasu
     * - Wyświetlenie składników, wartości odżywczych i instrukcji w oryginalnym języku
     */
    private void displayRecipeWithoutTranslation(JSONObject recipe) {
        try {
            int calories = recipe.optJSONObject("nutrition").optInt("calories", 0);
            int time = recipe.optInt("total_time_minutes", 0);
            runOnUiThread(() -> {
                textCalories.setText(calories + " kcal");
                textTime.setText(time + " min");
            });

            JSONArray ingredients = recipe.optJSONArray("sections")
                    .optJSONObject(0).optJSONArray("components");
            if (ingredients != null) {
                for (int i = 0; i < ingredients.length(); i++) {
                    JSONObject ingredient = ingredients.getJSONObject(i);
                    String rawText = ingredient.optString("raw_text");
                    runOnUiThread(() -> {
                        TextView textView = new TextView(this);
                        textView.setText("• " + rawText);
                        textView.setTextColor(Color.WHITE);
                        ingredientsLayout.addView(textView);
                    });
                }
            }

            JSONObject nutrition = recipe.optJSONObject("nutrition");
            if (nutrition != null) {
                Iterator<String> keys = nutrition.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = nutrition.optString(key);
                    String textToDisplay = key + ": " + value;
                    runOnUiThread(() -> {
                        TextView textView = new TextView(this);
                        textView.setText(textToDisplay);
                        textView.setTextColor(Color.WHITE);
                        nutritionLayout.addView(textView);
                    });
                }
            }

            JSONArray instructions = recipe.optJSONArray("instructions");
            if (instructions != null) {
                for (int i = 0; i < instructions.length(); i++) {
                    JSONObject step = instructions.getJSONObject(i);
                    String displayText = step.optString("display_text");
                    int stepNumber = i + 1;
                    runOnUiThread(() -> {
                        TextView stepView = new TextView(this);
                        stepView.setText(stepNumber + ". " + displayText);
                        stepView.setTextColor(Color.WHITE);
                        instructionsLayout.addView(stepView);
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Interfejs callback do obsługi wyniku tłumaczenia
    private interface TranslationCallback {
        void onTranslated(String translatedText);
    }

    /**
     * Metoda tłumacząca tekst za pomocą ML Kit:
     * - Jeśli tłumacz nie jest zainicjalizowany, zwraca oryginalny tekst
     * - W przypadku błędu tłumaczenia zwraca oryginalny tekst
     */
    private void translateText(String text, TranslationCallback callback) {
        if (translator == null) {
            callback.onTranslated(text);
            return;
        }
        translator.translate(text)
                .addOnSuccessListener(callback::onTranslated)
                .addOnFailureListener(e -> {
                    Log.e("TRANSLATE", "Błąd tłumaczenia: " + text, e);
                    callback.onTranslated(text);
                });
    }

    /**
     * Aktualizacja wyglądu przycisku ulubionych:
     * - Zmiana tekstu i koloru tła w zależności od stanu ulubionych
     */
    private void updateFavoriteButton() {
        if (isFavorite) {
            favoriteButton.setText("Usuń z ulubionych");
            favoriteButton.setBackgroundColor(Color.RED);
        } else {
            favoriteButton.setText("Dodaj do ulubionych");
            favoriteButton.setBackgroundColor(Color.GREEN);
        }
    }

    /**
     * Obsługa wyniku aktywności dodawania komentarza:
     * - Po pomyślnym dodaniu komentarza wyświetla snackbar z potwierdzeniem
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_COMMENT && resultCode == RESULT_OK) {
            SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Komentarz został dodany!");
        }
    }

    /**
     * Zwolnienie zasobów tłumacza przy zamknięciu aktywności
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (translator != null) {
            translator.close();
        }
    }
}