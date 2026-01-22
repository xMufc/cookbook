package com.example.projektzaliczeniowy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class RecipeGridAdapter extends BaseAdapter {
    private Context context;
    private List<Recipe> recipes;
    private LayoutInflater inflater;

    public RecipeGridAdapter(Context context, List<Recipe> recipes) {
        this.context = context;
        this.recipes = recipes;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return recipes.size();
    }

    @Override
    public Object getItem(int position) {
        return recipes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.recipe_item_card, parent, false);
            holder = new ViewHolder();
            holder.textRecipeName = convertView.findViewById(R.id.textRecipeName);
            holder.imageRecipe = convertView.findViewById(R.id.imageRecipe);
            holder.textCalories = convertView.findViewById(R.id.textCalories);
            holder.textTime = convertView.findViewById(R.id.textTime);
            holder.ratingBar = convertView.findViewById(R.id.ratingBar);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Recipe recipe = recipes.get(position);

        // Ustawienie nazwy przepisu
        holder.textRecipeName.setText(recipe.getName());

        holder.ratingBar.setRating(recipe.getRating() * 5);

        // Załadowanie zdjęcia za pomocą Glide
        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(recipe.getImageUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.placeholder_recipe) // Dodaj placeholder w drawable
                            .error(R.drawable.placeholder_recipe)
                            .centerCrop())
                    .into(holder.imageRecipe);
        } else {
            holder.imageRecipe.setImageResource(R.drawable.placeholder_recipe);
        }

        // Ustawienie kalorii
        if (recipe.getCalories() > 0) {
            holder.textCalories.setText(recipe.getCalories() + " kcal");
        } else {
            holder.textCalories.setText("- kcal");
        }

        // Ustawienie czasu
        if (recipe.getTotalTimeMinutes() > 0) {
            holder.textTime.setText(recipe.getTotalTimeMinutes() + " min");
        } else {
            holder.textTime.setText("- min");
        }
        convertView.setAlpha(0f);
        convertView.animate().alpha(1f).setDuration(300).start();
        return convertView;
    }

    public void updateRecipes(List<Recipe> newRecipes) {
        this.recipes.clear();
        this.recipes.addAll(newRecipes);
        notifyDataSetChanged();
    }

    static class ViewHolder {
        TextView textRecipeName;
        ImageView imageRecipe;
        TextView textCalories;
        TextView textTime;
        RatingBar ratingBar;
    }
}