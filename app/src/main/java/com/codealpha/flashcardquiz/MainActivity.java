package com.codealpha.flashcardquiz;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final String PREFS_NAME = "flashcard_storage";
    private static final String CARDS_KEY = "cards";

    private final ArrayList<Flashcard> cards = new ArrayList<>();
    private int currentIndex = 0;
    private boolean showingAnswer = false;

    private TextView cardText;
    private TextView labelText;
    private TextView counterText;
    private TextView hintText;
    private TextView emptyText;
    private ProgressBar progressBar;
    private Button showAnswerButton;
    private Button previousButton;
    private Button nextButton;
    private Button editButton;
    private Button deleteButton;
    private FrameLayout cardPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        styleSystemBars();
        loadCards();
        buildUi();
        renderCard(false);
    }

    private void styleSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(Color.parseColor("#FAF8F4"));
        window.setNavigationBarColor(Color.parseColor("#FAF8F4"));
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackground(backgroundGradient());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(24));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        header.addView(titleBlock, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this);
        title.setText("Flashcards");
        title.setTextColor(Color.parseColor("#161616"));
        title.setTextSize(34);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleBlock.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Simple practice. Clean focus.");
        subtitle.setTextColor(Color.parseColor("#74716C"));
        subtitle.setTextSize(15);
        subtitle.setPadding(0, dp(2), 0, 0);
        titleBlock.addView(subtitle);

        Button addButton = circleButton("+");
        addButton.setOnClickListener(v -> showCardDialog(false));
        header.addView(addButton, new LinearLayout.LayoutParams(dp(54), dp(54)));

        counterText = new TextView(this);
        counterText.setTextColor(Color.parseColor("#67635D"));
        counterText.setTextSize(14);
        counterText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        counterText.setGravity(Gravity.CENTER);
        counterText.setBackground(chipBackground());
        counterText.setPadding(dp(14), dp(8), dp(14), dp(8));
        LinearLayout.LayoutParams counterParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        counterParams.gravity = Gravity.CENTER_HORIZONTAL;
        counterParams.setMargins(0, dp(18), 0, dp(12));
        root.addView(counterText, counterParams);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressDrawable(progressDrawable());
        root.addView(progressBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(5)
        ));

        cardPanel = new FrameLayout(this);
        cardPanel.setBackground(cardBackground(false));
        cardPanel.setPadding(dp(26), dp(28), dp(26), dp(28));
        cardPanel.setElevation(dp(7));
        cardPanel.setOnClickListener(v -> {
            if (!cards.isEmpty()) {
                showingAnswer = !showingAnswer;
                renderCard(true);
            }
        });
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(330)
        );
        cardParams.setMargins(0, dp(22), 0, dp(16));
        root.addView(cardPanel, cardParams);

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.VERTICAL);
        cardContent.setGravity(Gravity.CENTER_HORIZONTAL);
        cardPanel.addView(cardContent, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        labelText = new TextView(this);
        labelText.setTextSize(12);
        labelText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        labelText.setGravity(Gravity.CENTER);
        labelText.setPadding(dp(12), dp(7), dp(12), dp(7));
        cardContent.addView(labelText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        cardText = new TextView(this);
        cardText.setTextColor(Color.parseColor("#161616"));
        cardText.setTextSize(27);
        cardText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        cardText.setGravity(Gravity.CENTER);
        cardText.setLineSpacing(dp(5), 1f);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        textParams.setMargins(0, dp(22), 0, dp(12));
        cardContent.addView(cardText, textParams);

        hintText = new TextView(this);
        hintText.setTextColor(Color.parseColor("#8D8982"));
        hintText.setTextSize(13);
        hintText.setGravity(Gravity.CENTER);
        cardContent.addView(hintText);

        emptyText = new TextView(this);
        emptyText.setText("No cards yet. Tap + to add your first question.");
        emptyText.setTextColor(Color.parseColor("#74716C"));
        emptyText.setTextSize(15);
        emptyText.setGravity(Gravity.CENTER);
        cardContent.addView(emptyText);

        showAnswerButton = primaryButton("Reveal Answer");
        showAnswerButton.setOnClickListener(v -> {
            showingAnswer = !showingAnswer;
            renderCard(true);
        });
        root.addView(showAnswerButton, fullWidthButtonParams());

        LinearLayout navRow = new LinearLayout(this);
        navRow.setOrientation(LinearLayout.HORIZONTAL);
        navRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        navParams.setMargins(0, dp(12), 0, 0);
        root.addView(navRow, navParams);

        previousButton = secondaryButton("< Previous");
        previousButton.setOnClickListener(v -> moveCard(-1));
        navRow.addView(previousButton, rowButtonParams(true));

        nextButton = secondaryButton("Next >");
        nextButton.setOnClickListener(v -> moveCard(1));
        navRow.addView(nextButton, rowButtonParams(false));

        LinearLayout manageRow = new LinearLayout(this);
        manageRow.setOrientation(LinearLayout.HORIZONTAL);
        manageRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams manageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        manageParams.setMargins(0, dp(12), 0, 0);
        root.addView(manageRow, manageParams);

        editButton = ghostButton("Edit");
        editButton.setOnClickListener(v -> showCardDialog(true));
        manageRow.addView(editButton, rowButtonParams(true));

        deleteButton = ghostButton("Delete");
        deleteButton.setTextColor(Color.parseColor("#D84A4A"));
        deleteButton.setOnClickListener(v -> confirmDelete());
        manageRow.addView(deleteButton, rowButtonParams(false));

        setContentView(scrollView);
    }

    private void moveCard(int amount) {
        if (cards.isEmpty()) {
            return;
        }
        currentIndex += amount;
        if (currentIndex < 0) {
            currentIndex = cards.size() - 1;
        } else if (currentIndex >= cards.size()) {
            currentIndex = 0;
        }
        showingAnswer = false;
        renderCard(true);
    }

    private void renderCard(boolean animate) {
        boolean hasCards = !cards.isEmpty();
        cardText.setVisibility(hasCards ? View.VISIBLE : View.GONE);
        labelText.setVisibility(hasCards ? View.VISIBLE : View.GONE);
        hintText.setVisibility(hasCards ? View.VISIBLE : View.GONE);
        emptyText.setVisibility(hasCards ? View.GONE : View.VISIBLE);
        showAnswerButton.setEnabled(hasCards);
        previousButton.setEnabled(hasCards && cards.size() > 1);
        nextButton.setEnabled(hasCards && cards.size() > 1);
        editButton.setEnabled(hasCards);
        deleteButton.setEnabled(hasCards);

        if (!hasCards) {
            counterText.setText("0 cards");
            progressBar.setProgress(0);
            showAnswerButton.setText("Reveal Answer");
            cardPanel.setBackground(cardBackground(false));
            return;
        }

        Flashcard card = cards.get(currentIndex);
        labelText.setText(showingAnswer ? "ANSWER" : "QUESTION");
        labelText.setTextColor(showingAnswer ? Color.parseColor("#007A5E") : Color.parseColor("#6F5BFF"));
        labelText.setBackground(tinyPillBackground(showingAnswer));
        cardText.setText(showingAnswer ? card.answer : card.question);
        showAnswerButton.setText(showingAnswer ? "Back to Question" : "Reveal Answer");
        hintText.setText(showingAnswer ? "Tap the card to return to the question" : "Tap the card or button to reveal");
        counterText.setText("Card " + (currentIndex + 1) + " of " + cards.size());
        progressBar.setProgress(Math.round(((currentIndex + 1) * 100f) / cards.size()));
        cardPanel.setBackground(cardBackground(showingAnswer));

        if (animate) {
            cardPanel.setAlpha(0.28f);
            cardPanel.setScaleX(0.985f);
            cardPanel.setScaleY(0.985f);
            cardPanel.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(210)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
    }

    private void showCardDialog(boolean editing) {
        if (editing && cards.isEmpty()) {
            return;
        }

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(10), dp(20), 0);

        EditText questionInput = input("Question");
        EditText answerInput = input("Answer");

        if (editing) {
            Flashcard card = cards.get(currentIndex);
            questionInput.setText(card.question);
            answerInput.setText(card.answer);
        }

        box.addView(questionInput, dialogInputParams());
        box.addView(answerInput, dialogInputParams());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editing ? "Edit flashcard" : "Add flashcard")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(editing ? "Save" : "Add", null)
                .create();

        dialog.setOnShowListener(view -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#6F5BFF"));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#74716C"));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String question = questionInput.getText().toString().trim();
                String answer = answerInput.getText().toString().trim();
                if (question.isEmpty() || answer.isEmpty()) {
                    Toast.makeText(this, "Please enter both question and answer.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (editing) {
                    cards.set(currentIndex, new Flashcard(question, answer));
                } else {
                    cards.add(new Flashcard(question, answer));
                    currentIndex = cards.size() - 1;
                }
                showingAnswer = false;
                saveCards();
                renderCard(true);
                dialog.dismiss();
            });
        });

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private void confirmDelete() {
        if (cards.isEmpty()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Delete this flashcard?")
                .setMessage("This removes the current question and answer from your deck.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    cards.remove(currentIndex);
                    if (currentIndex >= cards.size()) {
                        currentIndex = Math.max(0, cards.size() - 1);
                    }
                    showingAnswer = false;
                    saveCards();
                    renderCard(true);
                })
                .show();
    }

    private EditText input(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setTextSize(16);
        editText.setTextColor(Color.parseColor("#161616"));
        editText.setHintTextColor(Color.parseColor("#9A958D"));
        editText.setSingleLine(false);
        editText.setMinLines(2);
        editText.setMaxLines(4);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setBackground(inputBackground());
        editText.setPadding(dp(14), dp(10), dp(14), dp(10));
        return editText;
    }

    private LinearLayout.LayoutParams dialogInputParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(10), 0, dp(8));
        return params;
    }

    private void loadCards() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String stored = prefs.getString(CARDS_KEY, null);
        if (stored == null) {
            seedCards();
            saveCards();
            return;
        }
        try {
            JSONArray array = new JSONArray(stored);
            cards.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                cards.add(new Flashcard(item.getString("question"), item.getString("answer")));
            }
            if (cards.isEmpty()) {
                seedCards();
            }
        } catch (Exception ignored) {
            cards.clear();
            seedCards();
        }
    }

    private void seedCards() {
        cards.add(new Flashcard("What is Android?", "Android is Google's mobile operating system for phones, tablets, TVs, and more."));
        cards.add(new Flashcard("What does XML do in Android?", "XML usually defines app layouts, colors, text, and other resources."));
        cards.add(new Flashcard("What is an Activity?", "An Activity is one screen of an Android app where the user can interact."));
    }

    private void saveCards() {
        try {
            JSONArray array = new JSONArray();
            for (Flashcard card : cards) {
                JSONObject item = new JSONObject();
                item.put("question", card.question);
                item.put("answer", card.answer);
                array.put(item);
            }
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(CARDS_KEY, array.toString())
                    .apply();
        } catch (Exception ignored) {
        }
    }

    private Button primaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(Color.WHITE);
        button.setBackground(primaryBackground());
        button.setElevation(dp(4));
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(Color.parseColor("#222222"));
        button.setBackground(secondaryBackground());
        return button;
    }

    private Button ghostButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(Color.parseColor("#625E58"));
        button.setBackground(ghostBackground());
        return button;
    }

    private Button circleButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(26);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER);
        button.setBackground(primaryBackground());
        button.setElevation(dp(5));
        return button;
    }

    private LinearLayout.LayoutParams fullWidthButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
        );
        params.setMargins(0, 0, 0, 0);
        return params;
    }

    private LinearLayout.LayoutParams rowButtonParams(boolean left) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(52), 1f);
        if (left) {
            params.setMargins(0, 0, dp(7), 0);
        } else {
            params.setMargins(dp(7), 0, 0, 0);
        }
        return params;
    }

    private GradientDrawable cardBackground(boolean answerMode) {
        int start = answerMode ? Color.parseColor("#F2FFF9") : Color.parseColor("#FFFFFF");
        int end = answerMode ? Color.parseColor("#FFFFFF") : Color.parseColor("#FFFDF9");
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{start, end}
        );
        drawable.setCornerRadius(dp(34));
        drawable.setStroke(dp(1), answerMode ? Color.parseColor("#B5E9D8") : Color.parseColor("#EEE8DE"));
        return drawable;
    }

    private GradientDrawable primaryBackground() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#111111"), Color.parseColor("#6F5BFF")}
        );
        drawable.setCornerRadius(dp(18));
        return drawable;
    }

    private GradientDrawable secondaryBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#FFFFFF"));
        drawable.setCornerRadius(dp(18));
        drawable.setStroke(dp(1), Color.parseColor("#EEE8DE"));
        return drawable;
    }

    private GradientDrawable ghostBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#00FFFFFF"));
        drawable.setCornerRadius(dp(16));
        drawable.setStroke(dp(1), Color.parseColor("#E9E2D7"));
        return drawable;
    }

    private GradientDrawable inputBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#FAF8F4"));
        drawable.setCornerRadius(dp(16));
        drawable.setStroke(dp(1), Color.parseColor("#E9E2D7"));
        return drawable;
    }

    private GradientDrawable backgroundGradient() {
        return new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#FAF8F4"), Color.parseColor("#F2F6FF")}
        );
    }

    private GradientDrawable chipBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#DFFFFFFF"));
        drawable.setCornerRadius(dp(18));
        drawable.setStroke(dp(1), Color.parseColor("#ECE4D8"));
        return drawable;
    }

    private GradientDrawable tinyPillBackground(boolean answerMode) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(answerMode ? Color.parseColor("#E4F8EF") : Color.parseColor("#F0EEFF"));
        drawable.setCornerRadius(dp(18));
        return drawable;
    }

    private LayerDrawable progressDrawable() {
        GradientDrawable track = new GradientDrawable();
        track.setColor(Color.parseColor("#E6DED2"));
        track.setCornerRadius(dp(5));

        GradientDrawable fill = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.parseColor("#111111"), Color.parseColor("#6F5BFF")}
        );
        fill.setCornerRadius(dp(5));

        ClipDrawable progressClip = new ClipDrawable(fill, Gravity.LEFT, ClipDrawable.HORIZONTAL);
        LayerDrawable layers = new LayerDrawable(new android.graphics.drawable.Drawable[]{track, progressClip});
        layers.setId(0, android.R.id.background);
        layers.setId(1, android.R.id.progress);
        return layers;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class Flashcard {
        final String question;
        final String answer;

        Flashcard(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }
    }
}
