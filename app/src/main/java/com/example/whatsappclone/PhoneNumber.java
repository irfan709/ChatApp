package com.example.whatsappclone;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.whatsappclone.databinding.ActivityPhoneNumberBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.hbb20.CountryCodePicker;

public class PhoneNumber extends AppCompatActivity {
ActivityPhoneNumberBinding binding;
FirebaseAuth auth;
String countrycode;
String number;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhoneNumberBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        countrycode = binding.ccp.getSelectedCountryCodeWithPlus();

        binding.number.requestFocus();

        if (auth.getCurrentUser() != null) {
            Intent intent = new Intent(PhoneNumber.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        binding.ccp.setOnCountryChangeListener(new CountryCodePicker.OnCountryChangeListener() {
            @Override
            public void onCountrySelected() {
                countrycode = binding.ccp.getSelectedCountryCodeWithPlus();
            }
        });

        binding.verifyptp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                number = binding.number.getText().toString().trim();
                String phoneNumber = countrycode + number;
                if (number.isEmpty()) {
                    binding.number.setError("Field cannot be empty");
                } else {
                    Intent intent = new Intent(PhoneNumber.this, OtpActivity.class);
                    intent.putExtra("phoneNumber", phoneNumber);
                    startActivity(intent);
                }
            }
        });
    }
}