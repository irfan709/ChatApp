package com.example.whatsappclone;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.whatsappclone.Models.User;
import com.example.whatsappclone.databinding.ActivityProfileBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Date;
import java.util.HashMap;

public class Profile extends AppCompatActivity {
    ActivityProfileBinding binding;
    User user;
    ActivityResultLauncher<Intent> galleryLauncher;
    Uri selectedImage;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.progressbar.setVisibility(View.INVISIBLE);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("darkMode", MODE_PRIVATE);

        // Set the switch to the user's last selected mode
        boolean isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        binding.darkswitch.setChecked(isDarkMode);

        // Set the app's theme based on the user's preference
        setAppTheme(isDarkMode);

        binding.darkswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                // Update the user's dark mode preference
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isDarkMode", b); // Replace isChecked with b
                editor.apply();

                // Apply the selected theme immediately
                setAppTheme(b);

                if (b) {
                    setTheme(R.style.AppThemeDark); // Apply dark theme
                } else {
                    setTheme(R.style.AppThemeLight); // Apply light theme
                }

                recreate();

                // Provide feedback to the user
                String modeMessage = b ? "Dark mode enabled" : "Dark mode disabled";
                Toast.makeText(Profile.this, modeMessage, Toast.LENGTH_SHORT).show();
            }
        });


        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference userReference = database.getReference("users").child(auth.getUid());

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference  = storage.getReference().child("profiles").child(auth.getUid());

        userReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    user = dataSnapshot.getValue(User.class);

                    // Set the username
                    binding.etuname.setText(user.getName());

                    // Load the profile image using Glide
                    Glide.with(Profile.this)
                            .load(user.getProfileImage())
                            .placeholder(R.drawable.avatar)
                            .into(binding.upprofile);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle the error if necessary
            }
        });

        binding.updatebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.progressbar.setVisibility(View.VISIBLE);
                updateNameInFirebase();
            }
        });

        binding.upprofile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                galleryLauncher.launch(intent);
            }
        });

        binding.deleteprofile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Profile.this);
                builder.setTitle("Delete Account");
                builder.setMessage("Are you sure you want to delete your account?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Delete the user's account and data
                        deleteAccountAndData();
                    }
                });
                builder.setNegativeButton("No", null);
                builder.show();
            }
        });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                FirebaseStorage storage1 = FirebaseStorage.getInstance();
                long time = new Date().getTime();
                StorageReference reference = storage1.getReference().child("profiles").child(time + "");
                reference.putFile(uri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    DatabaseReference userReference = FirebaseDatabase.getInstance()
                                            .getReference("users")
                                            .child(FirebaseAuth.getInstance().getUid());

                                    HashMap<String, Object> updateData = new HashMap<>();
                                    updateData.put("profileImage", uri.toString());

                                    userReference.updateChildren(updateData).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            binding.progressbar.setVisibility(View.INVISIBLE);
                                        }
                                    });
                                    user.setProfileImage(uri.toString());

                                    Glide.with(Profile.this).load(user.getProfileImage()).into(binding.upprofile);

                                }
                            });
                        }
                    }
                });
            }
        });
    }


    private void updateNameInFirebase() {
        String newName = binding.etuname.getText().toString();
        if (!TextUtils.isEmpty(newName)) {
            // Get a reference to the current user's data in the Firebase Realtime Database
            DatabaseReference userReference = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(FirebaseAuth.getInstance().getUid());

            // Create a HashMap to update the name field
            HashMap<String, Object> updateData = new HashMap<>();
            updateData.put("name", newName);

            // Update the name field with the new name
            userReference.updateChildren(updateData).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        binding.progressbar.setVisibility(View.INVISIBLE);
                    }
                }
            });

            // Optionally, update the name in the User object as well
            user.setName(newName);
        }
    }

    private void deleteAccountAndData() {
        // Delete user data from the Realtime Database
        DatabaseReference userReference = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(FirebaseAuth.getInstance().getUid());

        userReference.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Data in the Realtime Database deleted successfully
                    // Now, delete the files from Firebase Storage

                    FirebaseStorage storage = FirebaseStorage.getInstance();
                    StorageReference storageReference = storage.getReference().child("profiles").child(FirebaseAuth.getInstance().getUid());

                    storageReference.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> storageTask) {
                            if (storageTask.isSuccessful()) {
                                // Storage files deleted successfully

                                // Delete the user's account
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                if (user != null) {
                                    user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                // Account deleted successfully
                                                Toast.makeText(Profile.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();

                                                // Sign the user out
                                                FirebaseAuth.getInstance().signOut();

                                                // Redirect to the PhoneNumber activity
                                                Intent intent = new Intent(Profile.this, PhoneNumber.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                // Handle account deletion failure
                                                Toast.makeText(Profile.this, "Account deletion failed. Please try again.", Toast.LENGTH_SHORT).show();
                                                // Log the error message
                                                Log.e("AccountDeletion", "Account deletion failed", task.getException());
                                            }
                                        }
                                    });
                                }
                            } else {
                                // Handle Firebase Storage file deletion failure
                                Toast.makeText(Profile.this, "Failed to delete files from Firebase Storage.", Toast.LENGTH_SHORT).show();
                                // Log the error message
                                Log.e("AccountDeletion", "Failed to delete files from Firebase Storage.", storageTask.getException());
                            }
                        }
                    });
                } else {
                    // Handle Realtime Database data deletion failure
                    Toast.makeText(Profile.this, "Failed to delete data from the Realtime Database.", Toast.LENGTH_SHORT).show();
                    // Log the error message
                    Log.e("AccountDeletion", "Failed to delete data from the Realtime Database.", task.getException());
                }
            }
        });
    }


    private void setAppTheme(boolean isDarkMode) {
        // Set the app's theme based on the user's preference
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
