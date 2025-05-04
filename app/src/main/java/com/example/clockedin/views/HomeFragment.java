package com.example.clockedin.views;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.clockedin.PortraitScannerActivity;
import com.example.clockedin.R;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class HomeFragment extends Fragment {

    private Button btnTimeIn, btnTimeOut;
    private String scanType = ""; // "IN" or "OUT"

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        btnTimeIn = view.findViewById(R.id.btnTimeIn);
        btnTimeOut = view.findViewById(R.id.btnTimeOut);

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

    private void startQRScan() {
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setCaptureActivity(PortraitScannerActivity.class); // Force portrait
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE); // Only QR
        integrator.setPrompt("Scan QR Code");
        integrator.setCameraId(0); // Rear camera
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null && result.getContents() != null) {
            String qrContent = result.getContents();
            String message = scanType.equals("IN") ? "Time In: " : "Time Out: ";
            Toast.makeText(getContext(), message + qrContent, Toast.LENGTH_LONG).show();

            // You can now log this to database or server
        } else {
            Toast.makeText(getContext(), "Scan Cancelled", Toast.LENGTH_SHORT).show();
        }

        scanType = ""; // Reset
    }
}
