package com.example.bookapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.bookapp.R;
import com.example.bookapp.databinding.ActivityForgetPasswordBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;

public class ForgetPasswordActivity extends AppCompatActivity {

    //view binding
    private ActivityForgetPasswordBinding binding;

    //FirebaseAuth
    FirebaseAuth firebaseAuth;

    //progress dialog
    ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //init firebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();

        //setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //handle click go back
        binding.forgetPassBackIbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //handle click, begin recovery password
        binding.forgetPassSubmitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });
    }

    private String email ="";
    private void validateData() {
        //get data
        email = binding.forgetPassEmailEt.getText().toString().trim();

        //validate
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Enter your email...!", Toast.LENGTH_SHORT).show();
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
        {// email is either no entered or email pattern is invalid, don't allow to continue in that case
            Toast.makeText(this, "Invalid email Pattern...!", Toast.LENGTH_SHORT).show();
        }
        else {
            recoveryPassword();
        }

    }

    private void recoveryPassword() {
        //show progress
        progressDialog.setMessage("Sending password recovery instructions to "+email);
        progressDialog.show();

        //begin sending recovery
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //sent
                        progressDialog.dismiss();
                        Toast.makeText(ForgetPasswordActivity.this, "Instruction to reset password sent to "+email, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed to send
                        progressDialog.dismiss();
                        Toast.makeText(ForgetPasswordActivity.this, "Failed to send due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}