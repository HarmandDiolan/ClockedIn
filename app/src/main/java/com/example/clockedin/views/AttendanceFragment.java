package com.example.clockedin.views;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;

import com.example.clockedin.R;
import com.example.clockedin.viewmodel.AuthViewModel;
import com.example.clockedin.model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class AttendanceFragment extends Fragment {
    private TextView dateText, timeInText, timeOutText, sessionTimerText;
    private DatabaseReference dbRef;
    private AuthViewModel authViewModel;
    private User currentUser;
    private Handler timerHandler;
    private long lastTimeInMillis = 0L;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_attendance, container, false);

        // Initialize views
        dateText = view.findViewById(R.id.dateText);
        timeInText = view.findViewById(R.id.timeInText);
        timeOutText = view.findViewById(R.id.timeOutText);
        sessionTimerText = view.findViewById(R.id.sessionTimerText);

        // Initialize AuthViewModel and database reference
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        dbRef = FirebaseDatabase.getInstance().getReference("attendance");
        timerHandler = new Handler(Looper.getMainLooper());

        // Set default values
        dateText.setText("Date: --");
        timeInText.setText("Time In: --");
        timeOutText.setText("Time Out: --");
        sessionTimerText.setText("0 Hour 0 Minutes 0 Seconds");

        // Observe current user
        authViewModel.getUserData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                currentUser = user;
                startListeningToAttendance();
            }
        });

        return view;
    }

    private void startListeningToAttendance() {
        if (currentUser == null || currentUser.uid == null) return;

        dbRef.child(currentUser.uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Get the last time in
                    Long timeIn = snapshot.child("lastTimeInMillis").getValue(Long.class);
                    Long storedTimeIn = snapshot.child("lastStoredTimeIn").getValue(Long.class);
                    Long storedTimeOut = snapshot.child("lastStoredTimeOut").getValue(Long.class);
                    lastTimeInMillis = timeIn != null ? timeIn : 0L;

                    if (lastTimeInMillis > 0) {
                        // User is currently clocked in
                        dateText.setText("Date: " + formatDate(lastTimeInMillis));
                        timeInText.setText("Time In: " + formatTime(lastTimeInMillis));
                        timeOutText.setText("Time Out: --");
                        startSessionTimer();
                    } else {
                        // User is clocked out
                        if (storedTimeIn != null && storedTimeIn > 0) {
                            dateText.setText("Date: " + formatDate(storedTimeIn));
                            timeInText.setText("Time In: " + formatTime(storedTimeIn));
                            
                            if (storedTimeOut != null && storedTimeOut > 0) {
                                timeOutText.setText("Time Out: " + formatTime(storedTimeOut));
                            }
                        }
                        sessionTimerText.setText("0 Hour 0 Minutes 0 Seconds");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle error
            }
        });
    }

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (lastTimeInMillis > 0) {
                long currentSession = System.currentTimeMillis() - lastTimeInMillis;
                sessionTimerText.setText(formatDuration(currentSession));
                timerHandler.postDelayed(this, 1000); // Update every second
            }
        }
    };

    private void startSessionTimer() {
        timerHandler.removeCallbacks(timerRunnable);
        if (lastTimeInMillis > 0) {
            timerRunnable.run();
        }
    }

    private String formatTime(long millis) {
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
        return df.format(new Date(millis));
    }

    private String formatDate(long millis) {
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
        return df.format(new Date(millis));
    }

    private String formatDuration(long millis) {
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = millis / (1000 * 60 * 60);
        return String.format(Locale.getDefault(), 
            "%d Hour %d Minutes %d Seconds", hours, minutes, seconds);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHandler.removeCallbacks(timerRunnable);
    }
}