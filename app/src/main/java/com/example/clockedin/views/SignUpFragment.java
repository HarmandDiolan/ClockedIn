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
            String email = emailEdit.getText().toString();
            String pass = passEdit.getText().toString();
            String username = usernameEdit.getText().toString();
            String contact = contactEdit.getText().toString();

            if (!email.isEmpty() && !pass.isEmpty() && !username.isEmpty() && !contact.isEmpty()) {
                viewModel.register(email, pass, username, contact);
            } else {
                Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
