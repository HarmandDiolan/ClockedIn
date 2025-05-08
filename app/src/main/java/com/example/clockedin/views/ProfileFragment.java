package com.example.clockedin.views;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.clockedin.R;
import com.example.clockedin.model.User;
import com.example.clockedin.viewmodel.AuthViewModel;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private AuthViewModel authViewModel;
    private User currentUser;
    private TextView greetingText, studentId, studentName, studentEmail, studentPhone;
    private Button btnEditProfile;
    private ShapeableImageView profileImage;
    private DatabaseReference dbRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        greetingText = view.findViewById(R.id.greetingText);
        studentId = view.findViewById(R.id.studentId);
        studentName = view.findViewById(R.id.studentName);
        studentEmail = view.findViewById(R.id.studentEmail);
        studentPhone = view.findViewById(R.id.studentPhone);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        profileImage = view.findViewById(R.id.profileImage);

        // Initialize AuthViewModel
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        dbRef = FirebaseDatabase.getInstance().getReference("users");

        // Observe user data
        authViewModel.getUserData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                currentUser = user;
                Log.d(TAG, "User data received - Name: " + user.username + ", Phone: " + user.contactNumber);
                updateUI(user);
            } else {
                Log.d(TAG, "User data is null");
            }
        });

        // Set up edit profile button click listener
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
    }

    private void updateUI(User user) {
        if (user == null) {
            Log.d(TAG, "updateUI called with null user");
            return;
        }

        Log.d(TAG, "Updating UI with user data - Name: " + user.username + ", Phone: " + user.contactNumber);
        
        greetingText.setText(user.username);
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

    private void showEditProfileDialog() {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User data not available", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);

        EditText editName = dialogView.findViewById(R.id.editName);
        EditText editPhone = dialogView.findViewById(R.id.editPhone);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        // Pre-fill current values
        editName.setText(currentUser.username);
        editPhone.setText(currentUser.contactNumber != null ? currentUser.contactNumber : "");

        AlertDialog dialog = builder.create();
        dialog.show();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            String newPhone = editPhone.getText().toString().trim();

            if (newName.isEmpty() || newPhone.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update user data in Firebase
            if (currentUser != null && currentUser.uid != null) {
                dbRef.child(currentUser.uid).child("username").setValue(newName);
                dbRef.child(currentUser.uid).child("contactNumber").setValue(newPhone)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Failed to update profile: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    });
            }
        });
    }
}