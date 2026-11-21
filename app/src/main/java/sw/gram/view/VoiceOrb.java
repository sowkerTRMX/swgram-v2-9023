package sw.gram.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import androidx.appcompat.widget.AppCompatTextView;
import java.util.ArrayList;
import java.util.Locale;
import sw.gram.R;
import sw.gram.core.Mods;
import sw.gram.core.Prefs;
import sw.gram.core.Ui;
import sw.gram.ui.SettingsActivity;

public class VoiceOrb extends AppCompatTextView {
    private float dx, dy;
    private boolean moved;
    private long downAt;
    private SpeechRecognizer sr;

    public VoiceOrb(final Activity a) {
        super(a);
        setText("\uD83C\uDFA4");
        setTextSize(26);
        setGravity(Gravity.CENTER);
        setBackgroundResource(R.drawable.orb);
        setElevation(14f);
        setOnTouchListener(new OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dx = e.getRawX() - getX(); dy = e.getRawY() - getY();
                        moved = false; downAt = System.currentTimeMillis();
                        setBackgroundResource(R.drawable.orb_rec);
                        listen(a);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float nx = e.getRawX() - dx, ny = e.getRawY() - dy;
                        if (Math.abs(nx - getX()) > 8 || Math.abs(ny - getY()) > 8) moved = true;
                        setX(nx); setY(ny);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        setBackgroundResource(R.drawable.orb);
                        stop();
                        if (!moved && System.currentTimeMillis() - downAt < 250) Ui.toast("Hold the orb and speak");
                        return true;
                }
                return false;
            }
        });
    }

    private void listen(final Activity a) {
        if (!SpeechRecognizer.isRecognitionAvailable(a)) { Ui.toast("Speech engine unavailable"); return; }
        sr = SpeechRecognizer.createSpeechRecognizer(a);
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        sr.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle b) { }
            @Override public void onBeginningOfSpeech() { }
            @Override public void onRmsChanged(float r) { }
            @Override public void onBufferReceived(byte[] b) { }
            @Override public void onEndOfSpeech() { }
            @Override public void onError(int e) { release(); }
            @Override public void onResults(Bundle b) {
                ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (r != null && !r.isEmpty()) command(a, r.get(0));
                release();
            }
            @Override public void onPartialResults(Bundle b) { }
            @Override public void onEvent(int t, Bundle b) { }
        });
        sr.startListening(i);
    }

    private void stop() { if (sr != null) try { sr.stopListening(); } catch (Throwable ignored) { } }
    private void release() {
        setBackgroundResource(R.drawable.orb);
        if (sr != null) { try { sr.destroy(); } catch (Throwable ignored) { } sr = null; }
    }

    private void command(Activity a, String raw) {
        String s = raw.toLowerCase(Locale.ROOT).trim();
        Ui.toast("\u201C" + raw + "\u201D");
        if (s.contains("settings") || s.contains("настрой")) { a.startActivity(new Intent(a, SettingsActivity.class)); return; }
        if (s.contains("ghost") || s.contains("невидим")) {
            Prefs.set("ghost", !Prefs.on("ghost")); Mods.applyGhost();
            Ui.toast("Ghost " + (Prefs.on("ghost") ? "on" : "off")); return;
        }
        if (s.contains("troll") || s.contains("тролл")) {
            Prefs.set("trolling", !Prefs.on("trolling"));
            Ui.toast("Trolling " + (Prefs.on("trolling") ? "on" : "off")); return;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("(?:write to|send to|напиши)\\s+(.+?)\\s+(.+)").matcher(s);
        if (m.find()) { Ui.toast("Target: " + m.group(1) + " / " + m.group(2)); return; }
        Ui.toast("Unknown command");
    }
}
