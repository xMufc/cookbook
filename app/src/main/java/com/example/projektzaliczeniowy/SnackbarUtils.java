package com.example.projektzaliczeniowy;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.material.snackbar.Snackbar;

public class SnackbarUtils {

    public static void showTopSnackbar(View rootView, String message) {
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        snackbar.setDuration(5000); // 5 sekund
        snackbar.setAction("X", v -> snackbar.dismiss());

        View snackbarView = snackbar.getView();

        // Ustaw marginesy
        ViewGroup.LayoutParams params = snackbarView.getLayoutParams();
        if (params instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;
            marginParams.setMargins(marginParams.leftMargin, 50, marginParams.rightMargin, marginParams.bottomMargin);
            snackbarView.setLayoutParams(marginParams);
        }

        // Ustawienie pozycji na górze
        if (snackbarView.getLayoutParams() instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
            androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams layoutParams =
                    (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) snackbarView.getLayoutParams();
            layoutParams.gravity = Gravity.TOP;
            snackbarView.setLayoutParams(layoutParams);
        } else {
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            layoutParams.gravity = Gravity.TOP;
            layoutParams.topMargin = 100;
            snackbarView.setLayoutParams(layoutParams);
        }

        snackbar.show();
    }
}
