package com.example.clockedin.views;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import android.content.Context;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import com.example.clockedin.R;
import com.example.clockedin.viewmodel.AuthViewModel;
import com.example.clockedin.model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.app.Activity;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class AttendanceFragment extends Fragment {
    private static final int PERMISSION_REQUEST_CODE = 200;
    private TextView dateText, timeInText, timeOutText, sessionTimerText;
    private Button downloadPdfButton;
    private DatabaseReference dbRef;
    private AuthViewModel authViewModel;
    private User currentUser;
    private Handler timerHandler;
    private long lastTimeInMillis = 0L;
    private List<AttendanceRecord> attendanceHistory;
    private ActivityResultLauncher<Intent> createDocumentLauncher;

    private static class AttendanceRecord {
        long timeIn;
        Long timeOut;

        AttendanceRecord(long timeIn, Long timeOut) {
            this.timeIn = timeIn;
            this.timeOut = timeOut;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_attendance, container, false);

        try {
            // Initialize views
            dateText = view.findViewById(R.id.dateText);
            timeInText = view.findViewById(R.id.timeInText);
            timeOutText = view.findViewById(R.id.timeOutText);
            sessionTimerText = view.findViewById(R.id.sessionTimerText);
            downloadPdfButton = view.findViewById(R.id.downloadPdfButton);

            // Initialize AuthViewModel and database reference
            authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
            dbRef = FirebaseDatabase.getInstance().getReference("users"); // Changed to "users"
            timerHandler = new Handler(Looper.getMainLooper());
            attendanceHistory = new ArrayList<>();

            // Initialize the document launcher
            createDocumentLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null) {
                                Uri uri = data.getData();
                                if (uri != null) {
                                    generatePdf(uri);
                                }
                            }
                        }
                    }
            );

            // Set default values
            dateText.setText("Date: --");
            timeInText.setText("Time In: --");
            timeOutText.setText("Time Out: --");
            sessionTimerText.setText("0 Hour 0 Minutes 0 Seconds");

            // Set button click listener
            downloadPdfButton.setOnClickListener(v -> createPdf());

            // Observe current user
            authViewModel.getUserData().observe(getViewLifecycleOwner(), user -> {
                if (user != null) {
                    currentUser = user;
                    startListeningToAttendance();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(),
                    "Error initializing attendance view: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

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

    private void createPdf() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "DTR_" + timeStamp + ".pdf";

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_TITLE, fileName);

            createDocumentLauncher.launch(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(),
                    "Error initiating PDF creation: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void generatePdf(Uri uri) {
        try {
            OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                Document document = new Document(PageSize.A4);
                PdfWriter.getInstance(document, outputStream);
                document.open();

                // Add Title
                Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
                Paragraph title = new Paragraph("DAILY TIME RECORD", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                document.add(title);
                document.add(new Paragraph("\n"));

                // Add User Information
                Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
                if (currentUser != null) {
                    document.add(new Paragraph("Employee Name: " + currentUser.username, normalFont));
                    document.add(new Paragraph("Email: " + currentUser.email, normalFont));
                    document.add(new Paragraph("\n"));
                }

                // Create Table
                PdfPTable table = new PdfPTable(3);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{2f, 1.5f, 1.5f});

                // Add Table Headers
                Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
                PdfPCell headerCell1 = new PdfPCell(new Phrase("Date", headerFont));
                PdfPCell headerCell2 = new PdfPCell(new Phrase("Time In", headerFont));
                PdfPCell headerCell3 = new PdfPCell(new Phrase("Time Out", headerFont));

                headerCell1.setHorizontalAlignment(Element.ALIGN_CENTER);
                headerCell2.setHorizontalAlignment(Element.ALIGN_CENTER);
                headerCell3.setHorizontalAlignment(Element.ALIGN_CENTER);
                headerCell1.setBackgroundColor(BaseColor.LIGHT_GRAY);
                headerCell2.setBackgroundColor(BaseColor.LIGHT_GRAY);
                headerCell3.setBackgroundColor(BaseColor.LIGHT_GRAY);

                table.addCell(headerCell1);
                table.addCell(headerCell2);
                table.addCell(headerCell3);

                // Fetch all attendance records
                dbRef.child(currentUser.uid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        try {
                            List<AttendanceRecord> records = new ArrayList<>();

                            // Get stored records
                            if (snapshot.child("timeRecords").exists()) {
                                for (DataSnapshot recordSnapshot : snapshot.child("timeRecords").getChildren()) {
                                    Long timeIn = recordSnapshot.child("timeIn").getValue(Long.class);
                                    Long timeOut = recordSnapshot.child("timeOut").getValue(Long.class);
                                    
                                    if (timeIn != null) {
                                        records.add(new AttendanceRecord(timeIn, timeOut));
                                    }
                                }
                            }

                            // Add current session if clocked in
                            Long currentTimeIn = snapshot.child("lastStoredTimeIn").getValue(Long.class);
                            Long currentTimeOut = snapshot.child("lastStoredTimeOut").getValue(Long.class);
                            if (currentTimeIn != null) {
                                records.add(new AttendanceRecord(currentTimeIn, currentTimeOut));
                            }

                            // Sort records by date (newest first)
                            Collections.sort(records, (a, b) -> Long.compare(b.timeIn, a.timeIn));

                            // Add records to table
                            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

                            for (AttendanceRecord record : records) {
                                Date timeInDate = new Date(record.timeIn);

                                PdfPCell dateCell = new PdfPCell(new Phrase(dateFormat.format(timeInDate), normalFont));
                                PdfPCell timeInCell = new PdfPCell(new Phrase(timeFormat.format(timeInDate), normalFont));
                                PdfPCell timeOutCell;

                                if (record.timeOut != null) {
                                    Date timeOutDate = new Date(record.timeOut);
                                    timeOutCell = new PdfPCell(new Phrase(timeFormat.format(timeOutDate), normalFont));
                                } else {
                                    timeOutCell = new PdfPCell(new Phrase("--", normalFont));
                                }

                                dateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                                timeInCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                                timeOutCell.setHorizontalAlignment(Element.ALIGN_CENTER);

                                table.addCell(dateCell);
                                table.addCell(timeInCell);
                                table.addCell(timeOutCell);
                            }

                            document.add(table);

                            // Add Footer
                            document.add(new Paragraph("\n\n"));
                            SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss", Locale.getDefault());
                            document.add(new Paragraph("Generated on: " + sdf.format(new Date()), normalFont));

                            document.close();
                            outputStream.close();

                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "PDF created successfully!", Toast.LENGTH_LONG).show();
                                openPdf(uri);
                            });

                        } catch (Exception e) {
                            e.printStackTrace();
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), 
                                    "Error generating PDF: " + e.getMessage(), 
                                    Toast.LENGTH_LONG).show();
                            });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), 
                                "Error fetching data: " + error.getMessage(), 
                                Toast.LENGTH_LONG).show();
                        });
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), 
                "Error creating PDF: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
        }
    }

    private void openPdf(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(),
                    "Unable to open PDF. Please check if you have a PDF viewer installed.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createPdf();
            } else {
                Toast.makeText(requireContext(),
                        "Permission denied. Cannot create PDF.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}