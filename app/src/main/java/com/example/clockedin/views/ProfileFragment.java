package com.example.clockedin.views;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.clockedin.R;
import com.example.clockedin.model.User;
import com.example.clockedin.viewmodel.AuthViewModel;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private AuthViewModel authViewModel;
    private User currentUser;
    private TextView greetingText, studentId, studentName, studentEmail, studentPhone;
    private Button btnEditProfile;
    private ShapeableImageView profileImage;
    private DatabaseReference dbRef;
    private StorageReference storageRef;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                if (selectedImageUri != null) {
                    uploadProfileImage(selectedImageUri);
                }
            }
        }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                openImagePicker();
            } else {
                Toast.makeText(requireContext(), "Permission required to select image", Toast.LENGTH_SHORT).show();
            }
        });

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

        // Initialize Firebase references
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        dbRef = FirebaseDatabase.getInstance().getReference("users");
        storageRef = FirebaseStorage.getInstance().getReference().child("profile_images");
        
        // Set default profile image immediately
        loadDefaultProfileImage();

        // Set up profile image click listener
        profileImage.setOnClickListener(v -> checkPermissionAndPickImage());

        // Observe user data
        authViewModel.getUserData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                currentUser = user;
                Log.d(TAG, "User data received - Name: " + user.username + ", Email: " + user.email);
                updateUI(user);
                loadProfileImage(user.email);
            } else {
                Log.d(TAG, "User data is null");
            }
        });

        // Set up edit profile button click listener
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
    }

    private void checkPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            } else {
                openImagePicker();
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                openImagePicker();
            }
        }
    }

    private void openImagePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImage.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening image picker: " + e.getMessage());
            Toast.makeText(requireContext(), "Error opening image picker", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadProfileImage(Uri imageUri) {
        if (currentUser == null || currentUser.email == null) {
            Toast.makeText(requireContext(), "User data not available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create a unique filename using the user's email
            String filename = currentUser.email.replace(".", "_") + ".jpg";
            StorageReference imageRef = storageRef.child(filename);

            // Show loading state
            Glide.with(this)
                .load(R.drawable.default_profile)
                .circleCrop()
                .into(profileImage);

            // Upload the image
            UploadTask uploadTask = imageRef.putFile(imageUri);
            
            uploadTask
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Image uploaded successfully");
                    // Get the download URL
                    imageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            Log.d(TAG, "Got download URL: " + uri.toString());
                            // Update the UI with the new image
                            requireActivity().runOnUiThread(() -> {
                                Glide.with(requireContext())
                                    .load(uri)
                                    .circleCrop()
                                    .into(profileImage);
                                Toast.makeText(requireContext(), "Profile picture updated", Toast.LENGTH_SHORT).show();
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error getting download URL: " + e.getMessage());
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Error updating profile picture", Toast.LENGTH_SHORT).show();
                            });
                        });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error uploading image: " + e.getMessage());
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error uploading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    Log.d(TAG, "Upload progress: " + progress + "%");
                });
        } catch (Exception e) {
            Log.e(TAG, "Error in uploadProfileImage: " + e.getMessage());
            Toast.makeText(requireContext(), "Error uploading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfileImage(String email) {
        if (email == null || email.isEmpty()) {
            Log.e(TAG, "Email is null or empty, cannot load profile image");
            return;
        }

        try {
            String filename = email.replace(".", "_") + ".jpg";
            StorageReference imageRef = storageRef.child(filename);
            Log.d(TAG, "Attempting to load image: " + filename);

            imageRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    Log.d(TAG, "Successfully got download URL: " + uri.toString());
                    requireActivity().runOnUiThread(() -> {
                        Glide.with(requireContext())
                            .load(uri)
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .circleCrop()
                            .into(profileImage);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading profile image: " + e.getMessage());
                    loadDefaultProfileImage();
                });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadProfileImage: " + e.getMessage());
            loadDefaultProfileImage();
        }
    }

    private void loadDefaultProfileImage() {
        Log.d(TAG, "Loading default profile image");
        requireActivity().runOnUiThread(() -> {
            Glide.with(requireContext())
                .load(R.drawable.default_profile)
                .circleCrop()
                .into(profileImage);
        });
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