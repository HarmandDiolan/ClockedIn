package com.example.clockedin.views;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.clockedin.R;
import com.example.clockedin.model.User;
import com.example.clockedin.viewmodel.AuthViewModel;

public class SignUpFragment extends Fragment {
    private EditText emailEdit, passEdit, usernameEdit, contactEdit;
    private TextView signInText;
    private Button signUpBtn;
    private AuthViewModel viewModel;
    private NavController navController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory
                .getInstance(getActivity().getApplication())).get(AuthViewModel.class);
        viewModel.getUserData().observe(this, user -> {
            if (user != null){
                navController.navigate(R.id.action_signUpFragment_to_loginFragment22);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emailEdit = view.findViewById(R.id.email);
        passEdit = view.findViewById(R.id.password);
        usernameEdit = view.findViewById(R.id.username);
        contactEdit = view.findViewById(R.id.contact_number);
        signInText = view.findViewById(R.id.textView4);
        signUpBtn = view.findViewById(R.id.btnSignup);
        navController = Navigation.findNavController(view);

        signInText.setOnClickListener(v ->
                navController.navigate(R.id.action_signUpFragment_to_loginFragment22)
        );

        signUpBtn.setOnClickListener(v -> {
            registerUser();
        });
    }

    private void registerUser() {
        String email = emailEdit.getText().toString().trim();
        String password = passEdit.getText().toString().trim();
        String username = usernameEdit.getText().toString().trim();
        String contact = contactEdit.getText().toString().trim();

        // Validate institutional email
        if (!isValidInstitutionalEmail(email)) {
            Toast.makeText(requireContext(), 
                "Please use your BukSU institutional email", 
                Toast.LENGTH_LONG).show();
            return;
        }

        if (email.isEmpty() || password.isEmpty() || username.isEmpty() || contact.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Additional password validation
        if (password.length() < 6) {
            Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.register(email, password, username, contact);
    }

    private boolean isValidInstitutionalEmail(String email) {
        // Check if email ends with @student.buksu.edu.ph
        return email.matches("^[A-Za-z0-9+_.-]+@student\\.buksu\\.edu\\.ph$");
    }
}
