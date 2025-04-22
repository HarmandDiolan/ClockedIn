package com.example.clockedin.views;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.clockedin.R;
import com.example.clockedin.viewmodel.AuthViewModel;
import com.google.firebase.auth.FirebaseUser;

public class LoginFragment extends Fragment {
    private EditText emailEdit, passEdit;
    private TextView signUpText;
    private Button loginBtn;
    private AuthViewModel viewModel;
    private NavController navController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory
                .getInstance(getActivity().getApplication())).get(AuthViewModel.class);
        viewModel.getUserData().observe(this, new Observer<FirebaseUser>() {
            @Override
            public void onChanged(FirebaseUser firebaseUser) {
                if (firebaseUser != null){
                    navController.navigate(R.id.action_loginFragment2_to_signOutFragment);
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

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emailEdit = view.findViewById(R.id.email_login);
        passEdit = view.findViewById(R.id.password_login);
        signUpText = view.findViewById(R.id.textView_login);
        loginBtn = view.findViewById(R.id.btnLogin);

        navController = Navigation.findNavController(view);

        signUpText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.action_loginFragment2_to_signUpFragment2);
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEdit.getText().toString();
                String pass = passEdit.getText().toString();

                if(!email.isEmpty() && !pass.isEmpty()){
                    viewModel.signIn(email, pass);
                }
            }
        });
    }
}