package com.example.bookapp.adapters;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bookapp.MyApplication;
import com.example.bookapp.R;
import com.example.bookapp.databinding.RowCommentBinding;
import com.example.bookapp.models.ModelComment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AdapterComment extends RecyclerView.Adapter<AdapterComment.HolderComment>{

    private RowCommentBinding binding;

    private static final String PICTURE_TAG ="PICTURE_COMMENT_TAG";
    private static final String DELETE_TAG ="DELETE_COMMENT_TAG";

    private FirebaseAuth firebaseAuth;
    private String currentUserId;

    private ProgressDialog progressDialog;

    private Context context;
    private ArrayList<ModelComment> commentArrayList;

    public AdapterComment(Context context, ArrayList<ModelComment> commentArrayList) {
        this.context = context;
        this.commentArrayList = commentArrayList;

        firebaseAuth = FirebaseAuth.getInstance();
        currentUserId =firebaseAuth.getUid();

        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);
    }

    @NonNull
    @Override
    public HolderComment onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        binding = RowCommentBinding.inflate(LayoutInflater.from(context), parent,false);

        return new HolderComment(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderComment holder, int position) {
        /*Get Data , Set Data, Handle clicks etc.*/

        //get data
        ModelComment model = commentArrayList.get(position);
        String id = model.getId();
        String bookId = model.getBookId();
        String comment = model.getComment();
        String userId= model.getUid();
        String timestamp = model.getTimestamp();

        /*Format date (Already made function in MyApplication class)*/
        String date = MyApplication.formatTimestamp(timestamp);

        //set data
        holder.dateTv.setText(date);
        holder.commentTv.setText(comment);

        /*We don't have user's name ,profile picture, so we will load it using uid (we stored in each comment)*/
        loadUserDetails(model,holder);

        /*Handle click, show option to delete comment*/
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*Requirements to delete a comment
                    1) User must be logged in
                    2) uid in comment (to be deleted) must be same as uid of logged in user*/
                if (firebaseAuth.getCurrentUser() != null && userId.equals(currentUserId)) {
                    deleteComment(model,holder);
                }

            }
        });

    }

    private void deleteComment(ModelComment model, HolderComment holder) {

        //show confirm dialog before deleting comment
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //begin delete

                        //show progress
                        progressDialog.setMessage("Deleting comment...");
                        progressDialog.show();

                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
                        ref.child(model.getBookId())
                                .child("Comments")
                                .child(model.getId())
                                .removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        progressDialog.dismiss();
                                        Log.d(DELETE_TAG, "onSuccess: deleted comment :"+model.getComment());
                                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        progressDialog.dismiss();
                                        Log.d(DELETE_TAG, "onFailure: Failed to delete comment due to "+e.getMessage());
                                        Toast.makeText(context, "Failed to delete comment due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });


                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();


    }

    private void loadUserDetails(ModelComment model, HolderComment holder) {
        String uid= model.getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get data
                        String name = ""+snapshot.child("name").getValue();
                        String profileImage = ""+snapshot.child("profileImage").getValue();

                        //set data
                        holder.nameTv.setText(name);
                        try {
                            Glide.with(context)
                                    .load(profileImage)
                                    .placeholder(R.drawable.ic_person_gray)
                                    .into(holder.profileIv);
                            Log.d(PICTURE_TAG, "onDataChange: Picture of user uploaded successfully");
                        }
                        catch (Exception e) {
                            Log.d(PICTURE_TAG, "onDataChange: Filed to add picture of user in comment due to "+e.getMessage());
                            holder.profileIv.setImageResource(R.drawable.ic_person_gray);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    public int getItemCount() {
        return commentArrayList.size();
    }

    /*view holder class to hold UI views for row_comment.xml*/
    class HolderComment extends RecyclerView.ViewHolder {
        //UI views for row_comment.xml
        ShapeableImageView profileIv ;
        TextView nameTv, dateTv, commentTv;

        public HolderComment(@NonNull View itemView) {
            super(itemView);

            profileIv = binding.rowCommentProfileIv;
            nameTv = binding.rowCommentNameTv;
            dateTv = binding.rowCommentDateTv;
            commentTv = binding.rowCommentTv;
        }
    }
}
