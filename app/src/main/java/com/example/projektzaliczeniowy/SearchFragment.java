package com.example.projektzaliczeniowy;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchFragment extends Fragment {

    EditText inputCalories, inputTime;
    Spinner spinnerCategory, spinnerCuisine;
    Button buttonSearch;
    GridView gridViewResults;
    RequestQueue requestQueue;
    RecipeGridAdapter adapter;
    List<Recipe> recipeList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Załadowanie layoutu fragmentu i inicjalizacja widoków
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // Przypisanie widoków do zmiennych
        inputCalories = view.findViewById(R.id.inputCalories);
        inputTime = view.findViewById(R.id.inputTime);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        spinnerCuisine = view.findViewById(R.id.spinnerCuisine);
        buttonSearch = view.findViewById(R.id.buttonSearch);
        gridViewResults = view.findViewById(R.id.gridViewResults);

        // Inicjalizacja kolejki zapytań sieciowych Volley
        requestQueue = Volley.newRequestQueue(requireContext());

        // Utworzenie listy przepisów i adaptera, przypisanie adaptera do GridView
        recipeList = new ArrayList<>();
        adapter = new RecipeGridAdapter(requireContext(), recipeList);
        gridViewResults.setAdapter(adapter);

        // Ustawienie obsługi kliknięcia na element listy przepisów
        gridViewResults.setOnItemClickListener((parent, view1, position, id) -> {
            Recipe recipe = recipeList.get(position);
            String url = "https://tasty.p.rapidapi.com/recipes/get-more-info?id=" + recipe.getId();

            // Utworzenie zapytania o szczegóły przepisu
            JsonObjectRequest detailRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        // Po otrzymaniu odpowiedzi uruchomienie aktywności z detalami przepisu
                        Intent intent = new Intent(getContext(), RecipeDetailActivity.class);
                        intent.putExtra("recipeId", recipe.getId());
                        intent.putExtra("name", recipe.getName());
                        intent.putExtra("rating", recipe.getRating());
                        intent.putExtra("imageUrl", recipe.getImageUrl());
                        intent.putExtra("recipeJson", response.toString());
                        startActivity(intent);
                    },
                    error -> {
                        // W przypadku błędu wyświetlenie komunikatu w Snackbarze
                        error.printStackTrace();
                        SnackbarUtils.showTopSnackbar(requireView(), "Błąd ładowania szczegółów.");
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    // Dodanie nagłówków wymaganych przez API RapidAPI
                    Map<String, String> headers = new HashMap<>();
                    headers.put("X-RapidAPI-Key", "80fe3757e0msh8225afb99cb14bep1c43efjsn1e5c26448063");
                    headers.put("X-RapidAPI-Host", "tasty.p.rapidapi.com");
                    return headers;
                }
            };

            // Dodanie zapytania do kolejki
            requestQueue.add(detailRequest);
        });

        // Ustawienie adapterów spinnerów z zasobów stringów
        ArrayAdapter<CharSequence> adapterCat = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.recipe_categories,
                android.R.layout.simple_spinner_item
        );
        adapterCat.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapterCat);

        ArrayAdapter<CharSequence> adapterCuisine = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.cuisine_types,
                android.R.layout.simple_spinner_item
        );
        adapterCuisine.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCuisine.setAdapter(adapterCuisine);

        // Ustawienie obsługi kliknięcia przycisku wyszukiwania
        buttonSearch.setOnClickListener(v -> performSearch());

        return view;
    }

    // Metoda wykonująca wyszukiwanie przepisów na podstawie wprowadzonych filtrów
    private void performSearch() {
        // Pobranie i przycięcie tekstu z pól kalorie i czas
        String caloriesStr = inputCalories.getText().toString().trim();
        String timeStr = inputTime.getText().toString().trim();
        // Pobranie wybranych wartości z spinnerów kategorii i kuchni
        String category = spinnerCategory.getSelectedItem().toString();
        String cuisine = spinnerCuisine.getSelectedItem().toString();

        // Parsowanie wartości maksymalnych kalorii i czasu lub ustawienie na maksymalną wartość jeśli puste
        int maxCalories = caloriesStr.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(caloriesStr);
        int maxTime = timeStr.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(timeStr);

        // Bazowy URL do API listy przepisów
        String url = "https://tasty.p.rapidapi.com/recipes/list?from=0&size=20&q=";

        // Mapowanie wybranej kategorii i kuchni na tagi API
        String categoryTag = mapCategoryToTag(category);
        List<String> tags = new ArrayList<>();
        if (!categoryTag.isEmpty()) tags.add(categoryTag);

        String cuisineTag = mapCuisineToTag(cuisine);
        if (!cuisineTag.isEmpty()) tags.add(cuisineTag);

        // Dodanie tagów do URL, jeśli istnieją
        if (!tags.isEmpty()) {
            try {
                url += "&tags=" + URLEncoder.encode(String.join(",", tags), "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Utworzenie zapytania GET do API z filtrowaniem
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        List<Recipe> filteredRecipes = new ArrayList<>();

                        // Iteracja po wynikach i filtrowanie przepisów według kalorii i czasu
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject recipe = results.getJSONObject(i);

                            String name = recipe.optString("name", "Bez nazwy");
                            String id = recipe.optString("id", "");

                            JSONObject nutrition = recipe.optJSONObject("nutrition");
                            int calories = nutrition != null ? nutrition.optInt("calories", 0) : 0;

                            int prepTime = recipe.optInt("total_time_minutes", 0);

                            JSONObject userRatings = recipe.optJSONObject("user_ratings");
                            float rating = 0.0f;
                            if (userRatings != null) {
                                rating = (float) userRatings.optDouble("score", 0.0);
                            }

                            String imageUrl = "";
                            if (recipe.has("thumbnail_url")) {
                                imageUrl = recipe.optString("thumbnail_url", "");
                            } else if (recipe.has("renditions")) {
                                JSONArray renditions = recipe.optJSONArray("renditions");
                                if (renditions != null && renditions.length() > 0) {
                                    JSONObject firstRendition = renditions.optJSONObject(0);
                                    if (firstRendition != null) {
                                        imageUrl = firstRendition.optString("url", "");
                                    }
                                }
                            }

                            // Dodanie przepisu do listy, jeśli spełnia kryteria filtrów
                            if (calories <= maxCalories && prepTime <= maxTime) {
                                Recipe recipeObj = new Recipe(name, rating, imageUrl, calories, prepTime, id);
                                filteredRecipes.add(recipeObj);
                            }
                        }

                        // Wyświetlenie komunikatu, jeśli brak wyników
                        if (filteredRecipes.isEmpty()) {
                            SnackbarUtils.showTopSnackbar(requireView(), "Brak wyników spełniających kryteria.");
                        }

                        // Aktualizacja listy i powiadomienie adaptera o zmianach
                        recipeList.clear();
                        recipeList.addAll(filteredRecipes);
                        adapter.notifyDataSetChanged();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        SnackbarUtils.showTopSnackbar(requireView(), "Błąd parsowania odpowiedzi.");
                    }
                },
                error -> {
                    error.printStackTrace();
                    SnackbarUtils.showTopSnackbar(requireView(), "Błąd sieci.");
                }) {
            @Override
            public Map<String, String> getHeaders() {
                // Dodanie nagłówków wymaganych przez API RapidAPI
                Map<String, String> headers = new HashMap<>();
                headers.put("X-RapidAPI-Key", "80fe3757e0msh8225afb99cb14bep1c43efjsn1e5c26448063");
                headers.put("X-RapidAPI-Host", "tasty.p.rapidapi.com");
                return headers;
            }
        };

        // Dodanie zapytania do kolejki Volley
        requestQueue.add(request);
    }

    // Mapowanie polskich nazw kuchni na tagi API
    private String mapCuisineToTag(String cuisinePL) {
        switch (cuisinePL) {
            case "Brytyjska": return "british";
            case "Włoska": return "italian";
            case "Meksykańska": return "mexican";
            case "Bliskowschodnia": return "middle_eastern";
            case "Grecka": return "greek";
            case "Indyjska": return "indian";
            case "Morska": return "seafood";
            case "Tajska": return "thai";
            case "Hawajska": return "hawaiian";
            case "Etiopska": return "ethiopian";
            case "Zachodnioafrykańska": return "west_african";
            case "Peruwiańska": return "peruvian";
            case "Kubańska": return "cuban";
            case "Dominikańska": return "dominican";
            case "Portorykańska": return "puerto_rican";
            case "Soul Food": return "soul_food";
            case "Filipińska": return "filipino";
            case "Południowoafrykańska": return "south_african";
            case "Jamajska": return "jamaican";
            case "Szwedzka": return "swedish";
            case "Perska": return "persian";
            case "Libańska": return "lebanese";
            case "Rdzenna": return "indigenous";
            case "Wietnamska": return "vietnamese";
            case "Afrykańska": return "african";
            case "Karaibska": return "caribbean";
            case "Niemiecka": return "german";
            case "Chińska": return "chinese";
            case "Francuska": return "french";
            case "Latynoamerykańska": return "latin_american";
            case "Amerykańska": return "american";
            case "BBQ": return "bbq";
            default: return "";
        }
    }

    // Mapowanie polskich nazw kategorii na tagi API
    private String mapCategoryToTag(String catPL) {
        switch (catPL) {
            case "Śniadanie": return "breakfast";
            case "Obiad":     return "lunch";
            case "Kolacja":   return "dinner";
            case "Deser":     return "dessert";
            default:           return "";
        }
    }
}