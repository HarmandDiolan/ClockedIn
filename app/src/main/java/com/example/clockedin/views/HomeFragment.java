package com.example.clockedin.views;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.clockedin.PortraitScannerActivity;
import com.example.clockedin.R;
import com.example.clockedin.model.User;
import com.example.clockedin.viewmodel.AuthViewModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private DatabaseReference dbRef;
    private AuthViewModel authViewModel;
    private User currentUser;

    private TextView tvTotalWorked, tvRequired, tvRemaining, tvGreeting;
    private TextView studentPhone, studentId, studentName, studentEmail;
    private Button btnTimeIn, btnTimeOut;

    private long lastTimeInMillis = 0L;  // timestamp of last clock‑in
    private long totalWorkedMillis = 0L;  // accumulated work time
    private final long requiredMillis = 490L * 60 * 60 * 1000; // 490h in ms
    private String scanType = ""; // "IN" or "OUT"

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize AuthViewModel - use the activity scope to ensure we get the same instance
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        dbRef = FirebaseDatabase.getInstance().getReference("users"); // Changed to users reference

        // bind UI
        tvGreeting = view.findViewById(R.id.greetingText);
        tvTotalWorked = view.findViewById(R.id.tvTotalWorked);
        tvRequired = view.findViewById(R.id.tvRequired);
        tvRemaining = view.findViewById(R.id.tvRemaining);
        btnTimeIn = view.findViewById(R.id.btnTimeIn);
        btnTimeOut = view.findViewById(R.id.btnTimeOut);
        
        // bind student info views
        studentPhone = view.findViewById(R.id.studentPhone);
        studentId = view.findViewById(R.id.studentId);
        studentName = view.findViewById(R.id.studentName);
        studentEmail = view.findViewById(R.id.studentEmail);

        // Set default greeting while waiting for data
        tvGreeting.setText("Hello, User!");

        // Observe current user
        authViewModel.getUserData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                currentUser = user;
                Log.d(TAG, "User data received: " + user.username + ", Phone: " + user.contactNumber);
                requireActivity().runOnUiThread(() -> {
                    tvGreeting.setText("Hello, " + user.username + "!");
                    updateStudentInfo(user);
                });
                fetchStudentData();
            } else {
                Log.d(TAG, "User data is null");
            }
        });

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
    
    private void updateStudentInfo(User user) {
        if (user != null) {
            Log.d(TAG, "Updating student info - Name: " + user.username + ", Phone: " + user.contactNumber);
            
            studentName.setText("Name: " + user.username);
            studentEmail.setText("Email: " + user.email);
            
            // Handle phone number display
            String phoneNumber = user.contactNumber;
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                // Try to fetch from Firebase if not available in user object
                fetchUserPhoneNumber(user.uid);
            } else {
                studentPhone.setText("Phone: " + phoneNumber);
            }
            
            // Extract student ID from email (part before the @ symbol)
            String studentIdValue = "";
            if (user.email != null && user.email.contains("@")) {
                studentIdValue = user.email.split("@")[0];
            }
            studentId.setText("Student ID: " + studentIdValue);
        }
    }

    private void fetchUserPhoneNumber(String userId) {
        if (userId == null) return;
        
        dbRef.child(userId).child("contactNumber").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String phoneNumber = snapshot.getValue(String.class);
                    if (phoneNumber != null && !phoneNumber.isEmpty()) {
                        requireActivity().runOnUiThread(() -> {
                            studentPhone.setText("Phone: " + phoneNumber);
                        });
                    } else {
                        requireActivity().runOnUiThread(() -> {
                            studentPhone.setText("Phone: Not set");
                        });
                    }
                } else {
                    requireActivity().runOnUiThread(() -> {
                        studentPhone.setText("Phone: Not set");
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error fetching phone number: " + error.getMessage());
                requireActivity().runOnUiThread(() -> {
                    studentPhone.setText("Phone: Error loading");
                });
            }
        });
    }

    private void fetchStudentData() {
        if (currentUser == null || currentUser.uid == null) {
            Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        dbRef.child(currentUser.uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (snapshot.child("totalWorkedMillis").exists()) {
                        totalWorkedMillis = snapshot.child("totalWorkedMillis").getValue(Long.class);
                    }
                    if (snapshot.child("lastTimeInMillis").exists()) {
                        lastTimeInMillis = snapshot.child("lastTimeInMillis").getValue(Long.class);
                    }
                    updateHourDisplays();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(getContext(), "Error loading data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
            if ("IN".equals(scanType)) recordTimeIn();
            if ("OUT".equals(scanType)) recordTimeOut();
        } else {
            Toast.makeText(getContext(), "Scan Cancelled", Toast.LENGTH_SHORT).show();
        }
        scanType = "";
    }

    private void recordTimeIn() {
        lastTimeInMillis = System.currentTimeMillis();

        Map<String, Object> updates = new HashMap<>();
        updates.put("lastTimeInMillis", lastTimeInMillis);
        updates.put("lastStoredTimeIn", lastTimeInMillis);
        updates.put("lastStoredTimeOut", null);

        dbRef.child(currentUser.uid).updateChildren(updates)
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );

        Toast.makeText(getContext(),
                "Clocked IN at " + formatTime(lastTimeInMillis),
                Toast.LENGTH_SHORT).show();
    }

    private void recordTimeOut() {
        if (lastTimeInMillis == 0L) {
            Toast.makeText(getContext(),
                    "You must Time In first!",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        long now = System.currentTimeMillis();
        long session = now - lastTimeInMillis;
        totalWorkedMillis += session;

        Map<String, Object> updates = new HashMap<>();
        updates.put("totalWorkedMillis", totalWorkedMillis);
        updates.put("lastTimeInMillis", 0L);
        updates.put("lastStoredTimeOut", now);
        updates.put("currentTimeOut", new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date()));

        dbRef.child(currentUser.uid).updateChildren(updates)
                .addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );

        Toast.makeText(getContext(),
                "Clocked OUT at " + formatTime(now) +
                        "\nSession: " + formatDuration(session) +
                        "\nTotal:   " + formatDuration(totalWorkedMillis),
                Toast.LENGTH_LONG).show();

        lastTimeInMillis = 0L;
        updateHourDisplays();
    }

    private String formatTime(long millis) {
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
        return df.format(new Date(millis));
    }

    private String formatDuration(long millis) {
        long h = millis / (1000 * 60 * 60);
        long m = (millis / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%dh %02dm", h, m);
    }

    private String formatHours(long millis) {
        long h = millis / (1000 * 60 * 60);
        return String.format(Locale.getDefault(), "%dh", h);
    }
}
