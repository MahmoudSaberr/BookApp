package com.example.bookapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.bookapp.MyApplication;
import com.example.bookapp.R;
import com.example.bookapp.adapters.AdapterPdfFavorite;
import com.example.bookapp.databinding.ActivityProfileBinding;
import com.example.bookapp.models.ModelPdf;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity {

    //view binding
    private ActivityProfileBinding binding;

    //to load information of user using uid
    private FirebaseAuth auth;
    String userId ;

    //Firebase current user
    private FirebaseUser firebaseUser;

    //arrayList to hold the books
    private ArrayList<ModelPdf> pdfArrayList;
    //adapter ts set in recyclerview
    private AdapterPdfFavorite adapterPdfFavorite;

    //progress dialog
    private ProgressDialog progressDialog;

    private static final String TAG = "PROFILE_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //reset data of user info
        binding.profileAccountTv.setText("N/A");
        binding.profileMemberDateTv.setText("N/A");
        binding.profileFavoriteBooksCountTv.setText("N/A");
        binding.profileAccStatusTv.setText("N/A");

        //setup firebase auth
        auth = FirebaseAuth.getInstance();
        userId = auth.getUid();
        //get current user
         firebaseUser = auth.getCurrentUser();

        //setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        loadUserInfo();


        loadFavoriteBooks();

        //handle click, edit
        binding.profileEditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ProfileActivity.this,ProfileEditActivity.class));
            }
        });

        //handle click, verify user is not
        binding.profileAccStatusTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (firebaseUser.isEmailVerified()) {
                    Toast.makeText(ProfileActivity.this, "Already verified...", Toast.LENGTH_SHORT).show();
                }
                else {
                    //not verified
                    emailVerificationDialog();
                }
            }
        });

        //handle click, go back
        binding.profileBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void emailVerificationDialog() {
         // setup Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Verify Email")
                .setMessage("Are you sure you want to send email verification instructions to your emil "+firebaseUser.getEmail()+"?")
                .setPositiveButton("SEND", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendEmailVerification();
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void sendEmailVerification() {
        //show progress
        progressDialog.setMessage("Sending email verification instructions to your email "+firebaseUser.getEmail());
        progressDialog.show();

        firebaseUser.sendEmailVerification()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //sent
                        progressDialog.dismiss();
                        Toast.makeText(ProfileActivity.this, "Instructions sent, check your email: "+firebaseUser.getEmail(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed to send
                        progressDialog.dismiss();
                        Toast.makeText(ProfileActivity.this, "Failed due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadUserInfo() {
        Log.d(TAG, "loadUserInfo: Loading user info of user "+auth.getUid());

        //get email verification status, after verification you have to re login to get changes..
        if (firebaseUser.isEmailVerified()) {
            binding.profileAccStatusTv.setText("Verified");
        }
        else {
            binding.profileAccStatusTv.setText("Not Verified");
        }

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get all info of user
                        String email = ""+ snapshot.child("email").getValue();
                        String name = ""+ snapshot.child("name").getValue();
                        String profileImage = ""+ snapshot.child("profileImage").getValue();
                        String timestamp = ""+ snapshot.child("timestamp").getValue();
                        String uid = ""+ snapshot.child("uid").getValue();
                        String userType = ""+ snapshot.child("userType").getValue();

                        //format date to dd/MM/yyyy
                        String formattedDate = MyApplication.formatTimestamp(timestamp);

                        //set data to ui
                        binding.profileEmailTv.setText(email);
                        binding.profileNameTv.setText(name);
                        binding.profileMemberDateTv.setText(formattedDate);
                        binding.profileAccountTv.setText(userType);
                        //set image, using glide
                        Glide.with(ProfileActivity.this)
                                .load(profileImage)
                                .placeholder(R.drawable.ic_person_gray)
                                .into(binding.profileIv);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadFavoriteBooks() {
        //init list

        pdfArrayList = new ArrayList<>();

        //load favorite books from database
        //Users > userId > Favorites
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(userId).child("Favorite")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //clear arrayList before starting adding data
                        pdfArrayList.clear();
                        for (DataSnapshot ds: snapshot.getChildren()) {
                            //we will only get the bookId here , amd we got other details in adapter using that bookId
                            String bookId = ""+ds.child("bookId").getValue();

                            //set id to model
                            ModelPdf modelPdf = new ModelPdf();
                            modelPdf.setId(bookId);

                            //add model to list
                            pdfArrayList.add(modelPdf);
                        }

                        //set count of favorite books
                        binding.profileFavoriteBooksCountTv.setText(""+pdfArrayList.size()); //can't set int/long to text view so set String
                        //setup adapter
                        adapterPdfFavorite = new AdapterPdfFavorite(ProfileActivity.this,pdfArrayList);
                        //set adapter to recycler view
                        binding.profileFavoriteBooksRv.setAdapter(adapterPdfFavorite);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }

}