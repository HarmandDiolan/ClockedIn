package com.example.clockedin.views;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.clockedin.PortraitScannerActivity;
import com.example.clockedin.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private TextView tvTotalWorked, tvRequired, tvRemaining, tvGreeting;
    private Button btnTimeIn, btnTimeOut;

    private long lastTimeInMillis   = 0L;  // timestamp of last clock‑in
    private long totalWorkedMillis  = 0L;  // accumulated work time
    private final long requiredMillis = 490L * 60 * 60 * 1000; // 490h in ms
    private String scanType = ""; // "IN" or "OUT"

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // bind UI
        tvGreeting     = view.findViewById(R.id.greetingText);
        tvTotalWorked  = view.findViewById(R.id.tvTotalWorked);
        tvRequired     = view.findViewById(R.id.tvRequired);
        tvRemaining    = view.findViewById(R.id.tvRemaining);
        btnTimeIn      = view.findViewById(R.id.btnTimeIn);
        btnTimeOut     = view.findViewById(R.id.btnTimeOut);

        // load persisted data from Firestore
        fetchStudentData();

        btnTimeIn.setOnClickListener(v -> {
            scanType = "IN";
            startQRScan();
        });

        btnTimeOut.setOnClickListener(v -> {
            scanType = "OUT";
            startQRScan();
        });

        return view;
    }

    private void fetchStudentData() {
        String uid = mAuth.getCurrentUser().getUid();
        if (uid == null) {
            Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference ref = db.collection("students").document(uid);
        ref.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("name");
                if (name != null) tvGreeting.setText("Hello, " + name + "!");

                Long savedTotal = doc.getLong("totalWorkedMillis");
                Long savedIn    = doc.getLong("lastTimeInMillis");
                totalWorkedMillis = (savedTotal != null ? savedTotal : 0L);
                lastTimeInMillis  = (savedIn    != null ? savedIn    : 0L);

                updateHourDisplays();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Error loading data", Toast.LENGTH_SHORT).show()
        );
    }

    private void updateHourDisplays() {
        // total worked
        String totalStr = formatDuration(totalWorkedMillis);
        tvTotalWorked.setText("Total Hours Worked\n" + totalStr);

        // required
        String requiredStr = formatHours(requiredMillis);
        tvRequired.setText("Required OJT Time\n" + requiredStr);

        // remaining
        long rem = Math.max(0L, requiredMillis - totalWorkedMillis);
        tvRemaining.setText("Remaining Time\n" + formatHours(rem));
    }

    private void startQRScan() {
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setCaptureActivity(PortraitScannerActivity.class);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan QR Code");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult r = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (r != null && r.getContents() != null) {
            if ("IN".equals(scanType))  recordTimeIn();
            if ("OUT".equals(scanType)) recordTimeOut();
        } else {
            Toast.makeText(getContext(), "Scan Cancelled", Toast.LENGTH_SHORT).show();
        }
        scanType = "";
    }

    private void recordTimeIn() {
        lastTimeInMillis = System.currentTimeMillis();
        Toast.makeText(getContext(),
                "Clocked IN at " + formatTime(lastTimeInMillis),
                Toast.LENGTH_SHORT).show();
        persistState();
    }

    private void recordTimeOut() {
        if (lastTimeInMillis == 0L) {
            Toast.makeText(getContext(),
                    "You must Time In first!",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        long now     = System.currentTimeMillis();
        long session = now - lastTimeInMillis;
        totalWorkedMillis += session;

        Toast.makeText(getContext(),
                "Clocked OUT at " + formatTime(now) +
                        "\nSession: " + formatDuration(session) +
                        "\nTotal:   " + formatDuration(totalWorkedMillis),
                Toast.LENGTH_LONG).show();

        lastTimeInMillis = 0L;
        updateHourDisplays();
        persistState();
    }

    private void persistState() {
        String uid = mAuth.getCurrentUser().getUid();
        if (uid == null) return;
        DocumentReference ref = db.collection("students").document(uid);
        ref.update(
                "totalWorkedMillis", totalWorkedMillis,
                "lastTimeInMillis",  lastTimeInMillis
        );
    }

    private String formatTime(long millis) {
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
        return df.format(new Date(millis));
    }

    private String formatDuration(long millis) {
        long h = millis / (1000*60*60);
        long m = (millis / (1000*60)) % 60;
        return String.format(Locale.getDefault(), "%dh %02dm", h, m);
    }

    private String formatHours(long millis) {
        long h = millis / (1000*60*60);
        return String.format(Locale.getDefault(), "%dh", h);
    }
}
