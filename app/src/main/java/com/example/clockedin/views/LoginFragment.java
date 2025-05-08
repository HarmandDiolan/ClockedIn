package com.example.clockedin.views;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;

public class LoginFragment extends Fragment {
    private static final String TAG = "LoginFragment";
    private static final int RC_SIGN_IN = 9001;
    
    private EditText emailEdit, passEdit;
    private TextView signUpText;
    private Button loginBtn;
    private MaterialButton googleSignInBtn;
    private AuthViewModel viewModel;
    private NavController navController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory
                .getInstance(getActivity().getApplication())).get(AuthViewModel.class);
        viewModel.getUserData().observe(this, new Observer<User>() {
            @Override
            public void onChanged(User user) {
                if (user != null){
                    Log.d(TAG, "User logged in: " + user.username);
                    navController.navigate(R.id.action_loginFragment2_to_appMainActivity);
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        navController = Navigation.findNavController(view);
        
        // Initialize views
        emailEdit = view.findViewById(R.id.email_login);
        passEdit = view.findViewById(R.id.password_login);
        loginBtn = view.findViewById(R.id.btnLogin);
        signUpText = view.findViewById(R.id.textView_login);
        googleSignInBtn = view.findViewById(R.id.btnGoogleSignIn);
        
        // Login button click listener
        loginBtn.setOnClickListener(v -> {
            String email = emailEdit.getText().toString().trim();
            String password = passEdit.getText().toString().trim();
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Attempt to sign in
            viewModel.signIn(email, password);
        });
        
        // Sign up text click listener
        signUpText.setOnClickListener(v -> {
            navController.navigate(R.id.action_loginFragment2_to_signUpFragment2);
        });

        // Google Sign In button click listener
        googleSignInBtn.setOnClickListener(v -> {
            GoogleSignInClient signInClient = viewModel.getGoogleSignInClient();
            Intent signInIntent = signInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            viewModel.handleGoogleSignInResult(task);
        }
    }
}