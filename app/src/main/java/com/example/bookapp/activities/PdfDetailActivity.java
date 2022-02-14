package com.example.bookapp.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.example.bookapp.MyApplication;
import com.example.bookapp.R;
import com.example.bookapp.adapters.AdapterComment;
import com.example.bookapp.databinding.ActivityPdfDetailBinding;
import com.example.bookapp.databinding.DialogCommentAddBinding;
import com.example.bookapp.models.ModelComment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class PdfDetailActivity extends AppCompatActivity {

    //view binding
    ActivityPdfDetailBinding binding;

    //pdf id, get from intent
    String bookId,bookTitle,bookUrl;


    boolean isInMyFavorite = false;

    private FirebaseAuth firebaseAuth;
    private String currentUserId;

    private static final String TAG ="DOWNLOAD_TAG";
    private static final String COMMENT_TAG ="ADD_COMMENT_TAG";

    //progress dialog
    private ProgressDialog progressDialog;

    //arrayList to hold comments
    private ArrayList<ModelComment> commentArrayList;
    //adapter to set in recyclerview
    private AdapterComment adapterComment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //pdf id, get from intent
        Intent intent = getIntent();
        bookId = intent.getStringExtra("bookId");

        //at start hide download button, because we need book url that we will load later in function loadBookDetails();
        binding.pdfDetailDownloadBtn.setVisibility(View.GONE);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUserId = firebaseAuth.getUid();
        if (firebaseAuth.getCurrentUser() != null) {
            checkIsFavorite();
        }

        loadBookDetails();
        loadComments();

        //increment book view count, whenever this page starts
        MyApplication.incrementBookViewCount(bookId);


        //handle click, go to previous screen
        binding.pdfDetailBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //handle click, read book
        binding.pdfDetailReadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i= new Intent(PdfDetailActivity.this, PdfViewActivity.class);
                i.putExtra("bookId",bookId);
                startActivity(i);
            }
        });

        //handle click, download pdf
        binding.pdfDetailDownloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Checking Permission");
                if (ContextCompat.checkSelfPermission(PdfDetailActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onClick: Permission already granted, can download book");
                    MyApplication.downloadBook(PdfDetailActivity.this,""+bookId,""+bookTitle,""+bookUrl);
                }
                else {
                    Log.d(TAG, "onClick: Permission was not granted, request permission...");
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
        });

        //handle click, add/remove favorite
        binding.pdfDetailFavoriteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (firebaseAuth.getCurrentUser() == null) {
                    Toast.makeText(PdfDetailActivity.this, "You 're not logged in", Toast.LENGTH_SHORT).show();
                }
                else {
                    if (isInMyFavorite) {
                        //true,in favourite, remove from favorite
                        MyApplication.removeFromFavorite(PdfDetailActivity.this,bookId);
                    }
                    else {
                        //false, not in favorite, add to favorite
                        MyApplication.addToFavourite(PdfDetailActivity.this,bookId);

                    }
                }
            }
        });

        //handle click, show comment dialog
        binding.pdfDetailAddCommentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*user must logged in to add comment*/
                if (firebaseAuth.getCurrentUser() == null) {
                    Toast.makeText(PdfDetailActivity.this, "Login First..!", Toast.LENGTH_SHORT).show();
                }
                else {
                    addCommentDialog();
                }
            }
        });

    }

    private void loadComments() {
        /*init arrayList before adding data into it*/
        commentArrayList = new ArrayList<>();

        //DB path to load comments
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .child("Comments")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //clear list before start adding into it
                        commentArrayList.clear();

                        for (DataSnapshot ds: snapshot.getChildren()) {
                            //get data
                            ModelComment model = ds.getValue(ModelComment.class);

                            //set data to arrayList
                            commentArrayList.add(model);
                        }
                        //setup adapter
                        adapterComment = new AdapterComment(PdfDetailActivity.this,commentArrayList);
                        binding.pdfDetailCommentsRv.setAdapter(adapterComment);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private String comment= "";
    private void addCommentDialog() {
        //inflate view bind for dialog
        DialogCommentAddBinding commentAddBinding = DialogCommentAddBinding.inflate(LayoutInflater.from(this));

        //setup alert dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.customDialog);
        builder.setView(commentAddBinding.getRoot());

        //create and show alert dialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        //handle click, dismiss dialog
        commentAddBinding.dialogCommentBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        //handle click, add comment
        commentAddBinding.dialogCommentSubmitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get data
                comment = commentAddBinding.dialogCommentCommentEt.getText().toString().trim();
                //validate data
                if (TextUtils.isEmpty(comment)) {
                    Toast.makeText(PdfDetailActivity.this, "Enter your comment...", Toast.LENGTH_SHORT).show();
                }
                else {
                    alertDialog.dismiss();
                    addComment();
                }
            }
        });

    }

    private void addComment() {
        Log.d(COMMENT_TAG, "addComment: uploading comment...");
        
        //show progress dialog
        progressDialog.setMessage("Adding Comment...");
        progressDialog.show();

        //timestamp for comment id,time
        String timestamp = ""+System.currentTimeMillis();

        //setup data to add in db for comment
        HashMap<String,Object> commentDate = new HashMap<>();
        commentDate.put("id",""+timestamp);
        commentDate.put("bookId",""+bookId);
        commentDate.put("timestamp",""+timestamp);
        commentDate.put("comment",""+comment);
        commentDate.put("uid",""+currentUserId);

        //DB path to add data into it: Books > bookId > Comments > commentId >commentData
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId).child("Comments").child(timestamp)
                .setValue(commentDate)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(COMMENT_TAG, "onSuccess: Uploaded comment");
                        Toast.makeText(PdfDetailActivity.this, "Comment Added...", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.d(COMMENT_TAG, "onFailure: Filed to add this comment <"+comment+">, into DB of user:"+currentUserId+", due to : "+e.getMessage());
                        Toast.makeText(PdfDetailActivity.this, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });


    }


    //Request storage permission
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Permission Granted");
                    MyApplication.downloadBook(
                            this,
                            ""+bookId,
                            ""+bookTitle,
                            ""+bookUrl);
                }
                else {
                    Log.d(TAG, "Permission was denied...");
                    Toast.makeText(this, "Permission was denied...", Toast.LENGTH_SHORT).show();
                }

            });


    private void loadBookDetails() {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get data
                        bookTitle = ""+snapshot.child("title").getValue();
                        String description = ""+snapshot.child("description").getValue();
                        String categoryId = ""+snapshot.child("categoryId").getValue();
                        String viewsCount = ""+snapshot.child("viewsCount").getValue();
                        String downloadsCount = ""+snapshot.child("downloadsCount").getValue();
                        bookUrl = ""+snapshot.child("url").getValue();
                        String timestamp = ""+snapshot.child("timestamp").getValue();

                        //required data is loaded, show download button
                        binding.pdfDetailDownloadBtn.setVisibility(View.VISIBLE);

                        //format date
                        String date = MyApplication.formatTimestamp(timestamp);
                        MyApplication.loadCategory(
                                ""+categoryId,
                                binding.pdfDetailCategoryTv
                        );
                        MyApplication.loadPdfFromUrlSinglePage(
                                ""+bookUrl,
                                ""+bookTitle,
                                binding.pdfDetailPdfView,
                                binding.pdfDetailProgressBar,
                                binding.pdfDetailPagesTv
                        );
                        MyApplication.loadPdfSize(
                                ""+bookUrl,
                                ""+bookTitle,
                                binding.pdfDetailSizeTv
                        );
                  /*      MyApplication.loadPdfPageCount(
                                PdfDetailActivity.this,
                                ""+bookUrl,
                                binding.pdfDetailPagesTv);
*/
                        //set data
                        binding.pdfDetailTitleTv.setText(bookTitle);
                        binding.pdfDetailDescriptionTv.setText(description);
                        binding.pdfDetailViewsTv.setText(viewsCount.replace("null","N/A"));
                        binding.pdfDetailDownloadsTv.setText(downloadsCount.replace("null","N/A"));
                        binding.pdfDetailDateTv.setText(date);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }

    private void checkIsFavorite() {
        //check if its in favorite or not
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(currentUserId).child("Favorite").child(bookId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        isInMyFavorite = snapshot.exists(); //true: if exist, false: if not exist
                        if (isInMyFavorite) {
                            //true
                            binding.pdfDetailFavoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_favorite_white,0,0);
                            binding.pdfDetailFavoriteBtn.setText("Remove Favorite");
                        }
                        else {
                            //false
                            binding.pdfDetailFavoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_favorite_border_white,0,0);
                            binding.pdfDetailFavoriteBtn.setText("Add Favorite");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}