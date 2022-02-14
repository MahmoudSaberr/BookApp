package com.example.bookapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookapp.MyApplication;
import com.example.bookapp.activities.PdfDetailActivity;
import com.example.bookapp.databinding.RowPdfFavoriteBinding;
import com.example.bookapp.models.ModelPdf;
import com.github.barteksc.pdfviewer.PDFView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AdapterPdfFavorite extends RecyclerView.Adapter<AdapterPdfFavorite.HolderPdfFavorite> {

    private RowPdfFavoriteBinding binding;

    private static final String TAG = "FAVORITE_BOOK_TAG";

    private Context context;
    private ArrayList<ModelPdf> pdfArrayList;

    public AdapterPdfFavorite(Context context, ArrayList<ModelPdf> pdfArrayList) {
        this.context = context;
        this.pdfArrayList = pdfArrayList;
    }

    @NonNull
    @Override
    public HolderPdfFavorite onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //binding row_pdf_admin.xml
        binding = RowPdfFavoriteBinding.inflate(LayoutInflater.from(context),parent,false);

        return new HolderPdfFavorite(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderPdfFavorite holder, int position) {
        /*Get Data , Set Data, Handle clicks etc.*/

        //get data
        ModelPdf model = pdfArrayList.get(position);

        loadBookDetails(model,holder);

        //handle book/pdf click, open pdf details page, pass pdf/book id to get details of it
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PdfDetailActivity.class);
                intent.putExtra("bookId",model.getId());
                context.startActivity(intent);
            }
        });

        //handle click, remove from favorite
        holder.removeFavBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApplication.removeFromFavorite(context,model.getId());
            }
        });
    }

    private void loadBookDetails(ModelPdf model, HolderPdfFavorite holder) {
        String bookId = model.getId();
        Log.d(TAG, "loadBookDetails: Book Details of Book ID: "+bookId);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //Get book info
                        String bookTitle = ""+ snapshot.child("title").getValue();
                        String description = ""+ snapshot.child("description").getValue();
                        String categoryId = ""+ snapshot.child("categoryId").getValue();
                        String bookUrl = ""+ snapshot.child("url").getValue();
                        String timestamp = ""+ snapshot.child("timestamp").getValue();
                        String uid = ""+ snapshot.child("uid").getValue();
                        String viewsCount = ""+ snapshot.child("viewsCount").getValue();
                        String downloadsCount = ""+ snapshot.child("downloadsCount").getValue();

                        //Set to model
                        model.setFavorite(true);
                        model.setTitle(bookTitle);
                        model.setCategoryId(categoryId);
                        model.setTimestamp(timestamp);
                        model.setDescription(description);
                        model.setUrl(bookUrl);
                        model.setUid(uid);

                        //we need to convert timestamp dd/MM/yyyy format
                        String formattedDate = MyApplication.formatTimestamp(timestamp);

                        //load further details like category, pdf from url, pdf size in separate functions
                        MyApplication.loadCategory(
                                ""+categoryId,
                                holder.categoryTv);
                        //do't need page number here = pass null
                        MyApplication.loadPdfFromUrlSinglePage(
                                ""+bookUrl,
                                ""+bookTitle,
                                holder.pdfView,
                                holder.progressBar,
                                null
                        );
                        MyApplication.loadPdfSize(
                                ""+bookUrl,
                                ""+bookTitle,
                                holder.sizeTv
                        );

                        //set data to views
                        holder.titleTv.setText(bookTitle);
                        holder.descriptionTv.setText(description);
                        holder.dateTv.setText(formattedDate);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }

    @Override
    public int getItemCount() {
        return pdfArrayList.size();
    }

    /*view holder class to hold UI views for row_pdf_admin.xml*/
    class HolderPdfFavorite extends RecyclerView.ViewHolder{
        //UI views for row_pdf_admin.xml
        PDFView pdfView;
        ProgressBar progressBar;
        TextView titleTv, descriptionTv, categoryTv, sizeTv, dateTv;
        ImageButton removeFavBtn;

        public HolderPdfFavorite(@NonNull View itemView) {
            super(itemView);

            //init ui views
            pdfView = binding.rowPdfFavoritePDFView;
            progressBar = binding.rowPdfFavoritePDFViewProgressbar;
            titleTv = binding.rowPdfFavoriteTitleTv;
            descriptionTv = binding.rowPdfFavoriteDescriptionTv;
            categoryTv = binding.rowPdfFavoriteCategoryTv;
            sizeTv = binding.rowPdfFavoriteSizeTv;
            dateTv = binding.rowPdfFavoriteDateTv;
            removeFavBtn = binding.rowPdfFavoriteRemoveFavBtn;
        }
    }
}