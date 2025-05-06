package com.example.clockedin.views;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.example.clockedin.R;
import com.example.clockedin.model.User;
import com.example.clockedin.viewmodel.AuthViewModel;
import com.google.android.material.navigation.NavigationView;

public class AppMainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private AuthViewModel authViewModel;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_main);

        // Initialize AuthViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        // Observe user data changes
        authViewModel.getUserData().observe(this, user -> {
            if (user != null) {
                currentUser = user;
                // Now we have the user data in the activity
                // Let's load the HomeFragment after we have the user data
                if (savedInstanceState == null) {
                    loadHomeFragment();
                }
            } else {
                // If user is null, navigate back to login
                finish();
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Apply styling to Sign Out menu item
        MenuItem signOutItem = navigationView.getMenu().findItem(R.id.signOut);
        if (signOutItem != null) {
            SpannableString styledTitle = new SpannableString(signOutItem.getTitle());

            // Set font size to 12sp
            styledTitle.setSpan(new AbsoluteSizeSpan(18, true), 0, styledTitle.length(), 0);
            // Set text color to gray
            styledTitle.setSpan(new ForegroundColorSpan(Color.GRAY), 0, styledTitle.length(), 0);
            // Set italic style
            styledTitle.setSpan(new StyleSpan(Typeface.ITALIC), 0, styledTitle.length(), 0);

            signOutItem.setTitle(styledTitle);
        }

        // We'll load HomeFragment when user data is ready
        navigationView.setCheckedItem(R.id.nav_home);
    }

    private void loadHomeFragment() {
        HomeFragment homeFragment = new HomeFragment();
        // If needed, could pass user data in a bundle here
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, homeFragment)
            .commit();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
        } else if (itemId == R.id.profile) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ProfileFragment()).commit();
        } else if (itemId == R.id.attendance) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new AttendanceFragment()).commit();
        } else if (itemId == R.id.signOut) {
            // Sign out using AuthViewModel
            authViewModel.signOut();

            // Show logout message
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

            // Close the activity to return to login screen
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
