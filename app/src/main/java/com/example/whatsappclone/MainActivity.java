package com.example.whatsappclone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.whatsappclone.Adapters.UserAdapter;
import com.example.whatsappclone.Models.User;
import com.example.whatsappclone.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    ArrayList<User> users;
    User user;
    UserAdapter userAdapter;
    FirebaseDatabase database;
    private static final int PERMISSION_REQUEST_READ_CONTACTS = 1;
    SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("darkMode", MODE_PRIVATE);
        // Set the app's theme based on the user's preference
        boolean isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

//        retrievePhoneNumbers();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_REQUEST_READ_CONTACTS);
        } else {
            // Permission is granted, you can retrieve phone numbers
            retrievePhoneNumbers();
        }


        database = FirebaseDatabase.getInstance();

        FirebaseMessaging.getInstance()
                .getToken()
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String token) {
                        HashMap<String, Object> object = new HashMap<>();
                        object.put("token", token);

                        database.getReference()
                                .child("users")
                                .child(FirebaseAuth.getInstance().getUid())
                                .updateChildren(object);

//                        Toast.makeText(MainActivity.this, token, Toast.LENGTH_SHORT).show();
                    }
                });

        users = new ArrayList<>();


        database.getReference().child("users").child(FirebaseAuth.getInstance().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        user = snapshot.getValue(User.class);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        userAdapter = new UserAdapter(this, users);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        binding.userslist.setLayoutManager(layoutManager);
        binding.userslist.setAdapter(userAdapter);

//        ArrayList<String> phoneNumbers = retrievePhoneNumbers();

        ArrayList<User> phoneUsers = retrievePhoneNumbers();

        database.getReference().child("users").addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                users.clear();
//                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
//                    User user = snapshot1.getValue(User.class);
//                    if (!user.getUid().equals(FirebaseAuth.getInstance().getUid())) {
//                        users.add(user);
//                    }
//                }
//                userAdapter.notifyDataSetChanged();
//            }

//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                users.clear();
//                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
//                    User user = snapshot1.getValue(User.class);
//                    if (!user.getUid().equals(FirebaseAuth.getInstance().getUid()) &&
//                            phoneNumbers.contains(user.getNumber())) {
//                        users.add(user);
//                    }
//                }
//                userAdapter.notifyDataSetChanged();
//            }

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                users.clear();
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    User user = snapshot1.getValue(User.class);
                    if (!user.getUid().equals(FirebaseAuth.getInstance().getUid())) {
                        // Check if the user's phone number is in the list of phone users
                        boolean phoneNumberFound = false;
                        for (User phoneUser : phoneUsers) {
                            if (phoneUser.getNumber().equals(user.getNumber())) {
                                // If the phone number matches, use the name from contacts
                                user.setName(phoneUser.getName());
                                phoneNumberFound = true;
                                break;
                            }
                        }
                        if (phoneNumberFound) {
                            users.add(user);
                        }
                    }
                }
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.profile_menu) {
                    startActivity(new Intent(MainActivity.this, Profile.class));
                }

                return false;
            }
        });
    }

//    private ArrayList<String> retrievePhoneNumbers() {
//        ArrayList<String> phoneNumbers = new ArrayList<>();
//
//        Cursor cursor = getContentResolver().query(
//                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
//                null,
//                null,
//                null,
//                null
//        );
//
//        if (cursor != null) {
//            int phoneNumberColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
//            while (cursor.moveToNext()) {
//                String phoneNumber = cursor.getString(phoneNumberColumnIndex);
//                // You may need to format and sanitize the phone number to match your database format.
//                phoneNumbers.add(phoneNumber);
//            }
//            cursor.close();
//        }
//
//        return phoneNumbers;
//    }

    private ArrayList<User> retrievePhoneNumbers() {
        ArrayList<User> users = new ArrayList<>();

        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                null,
                null,
                null
        );

        if (cursor != null) {
            int nameColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int phoneNumberColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

            while (cursor.moveToNext()) {
                String name = cursor.getString(nameColumnIndex);
                String phoneNumber = cursor.getString(phoneNumberColumnIndex);

                // You may need to format and sanitize the phone number to match your database format.
                User user = new User("default_uid", name, phoneNumber, "default_profile_image");
                users.add(user);
            }
            cursor.close();
        }

        return users;
    }


    @Override
    protected void onResume() {
        super.onResume();
        String currentId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        String currentId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Offline");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now retrieve phone numbers
                retrievePhoneNumbers();
            } else {
                Toast.makeText(this, "Permissions denied...", Toast.LENGTH_SHORT).show();
            }
        }
    }
}