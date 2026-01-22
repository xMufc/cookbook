package com.example.projektzaliczeniowy;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import org.json.JSONObject;

import java.util.*;

public class FavoritesFragment extends Fragment {

    GridView gridViewFavorites;
    RecipeGridAdapter adapter;
    List<Recipe> recipeList;
    RequestQueue requestQueue;
    DatabaseReference favoritesRef;
    FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        // Inicjalizacja widoków i adaptera
        gridViewFavorites = view.findViewById(R.id.gridViewResults);
        recipeList = new ArrayList<>();
        adapter = new RecipeGridAdapter(requireContext(), recipeList);
        gridViewFavorites.setAdapter(adapter);

        // Inicjalizacja kolejki zapytań i Firebase Auth oraz referencji do ulubionych
        requestQueue = Volley.newRequestQueue(requireContext());
        mAuth = FirebaseAuth.getInstance();
        favoritesRef = FirebaseDatabase.getInstance("https://projektzaliczeniowykonkel-default-rtdb.europe-west1.firebasedatabase.app").getReference("favorites");

        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference userFavorites = favoritesRef.child(userId);

        // Pobranie ulubionych przepisów użytkownika z bazy
        userFavorites.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recipeList.clear();
                adapter.notifyDataSetChanged();

                // Dla każdego ulubionego przepisu pobranie szczegółów
                for (DataSnapshot child : snapshot.getChildren()) {
                    String recipeId = child.getKey();
                    fetchRecipeDetails(recipeId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Pokazanie komunikatu o błędzie ładowania ulubionych
                SnackbarUtils.showTopSnackbar(requireView(), "Błąd ładowania ulubionych.");
            }
        });

        // Obsługa kliknięcia na przepis w gridzie - pobranie szczegółów i uruchomienie aktywności
        gridViewFavorites.setOnItemClickListener((parent, view1, position, id) -> {
            Recipe recipe = recipeList.get(position);
            String url = "https://tasty.p.rapidapi.com/recipes/get-more-info?id=" + recipe.getId();

            JsonObjectRequest detailRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        Intent intent = new Intent(getContext(), RecipeDetailActivity.class);
                        intent.putExtra("recipeId", recipe.getId());
                        intent.putExtra("name", recipe.getName());
                        intent.putExtra("rating", recipe.getRating());
                        intent.putExtra("imageUrl", recipe.getImageUrl());
                        intent.putExtra("recipeJson", response.toString());
                        startActivity(intent);
                    },
                    error -> {
                        // Pokazanie komunikatu o błędzie ładowania szczegółów
                        SnackbarUtils.showTopSnackbar(requireView(), "Błąd ładowania szczegółów.");
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("X-RapidAPI-Key", "80fe3757e0msh8225afb99cb14bep1c43efjsn1e5c26448063");
                    headers.put("X-RapidAPI-Host", "tasty.p.rapidapi.com");
                    return headers;
                }
            };

            requestQueue.add(detailRequest);
        });

        return view;
    }

    // Pobranie szczegółów przepisu po ID i dodanie do listy
    private void fetchRecipeDetails(String recipeId) {
        String url = "https://tasty.p.rapidapi.com/recipes/get-more-info?id=" + recipeId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    String name = response.optString("name", "Bez nazwy");
                    float rating = 0.0f;
                    JSONObject userRatings = response.optJSONObject("user_ratings");
                    if (userRatings != null) {
                        rating = (float) userRatings.optDouble("score", 0.0);
                    }

                    String imageUrl = response.optString("thumbnail_url", "");
                    JSONObject nutrition = response.optJSONObject("nutrition");
                    int calories = nutrition != null ? nutrition.optInt("calories", 0) : 0;
                    int time = response.optInt("total_time_minutes", 0);

                    Recipe recipe = new Recipe(name, rating, imageUrl, calories, time, recipeId);
                    recipeList.add(recipe);
                    adapter.notifyDataSetChanged();
                },
                error -> error.printStackTrace()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("X-RapidAPI-Key", "80fe3757e0msh8225afb99cb14bep1c43efjsn1e5c26448063");
                headers.put("X-RapidAPI-Host", "tasty.p.rapidapi.com");
                return headers;
            }
        };

        requestQueue.add(request);
    }
}