package com.example.bookapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookapp.MyApplication;
import com.example.bookapp.databinding.RowPdfAdminBinding;
import com.example.bookapp.models.ModelCategory;
import com.example.bookapp.models.ModelPdf;
import com.github.barteksc.pdfviewer.PDFView;

import java.util.ArrayList;

public class AdapterPdfAdmin extends RecyclerView.Adapter<AdapterPdfAdmin.HolderPdfAdmin>{

    //view binding
    private RowPdfAdminBinding binding;

    //context
    private Context context;
    //arrayList to hold list of data of type ModelPdf
    public ArrayList<ModelPdf> pdfArrayList, filterList;

    //constructor


    public AdapterPdfAdmin(Context context, ArrayList<ModelPdf> pdfArrayList) {
        this.context = context;
        this.pdfArrayList = pdfArrayList;
    }

    @NonNull
    @Override
    public HolderPdfAdmin onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //binding row_pdf_admin.xml
        binding = RowPdfAdminBinding.inflate(LayoutInflater.from(context),parent,false);

        return new HolderPdfAdmin(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderPdfAdmin holder, int position) {
        /*Get Data , Set Data, Handle clicks etc.*/

        //get data
        ModelPdf model = pdfArrayList.get(position);
        String title = model.getTitle();
        String description = model.getDescription();
        long timestamp = model.getTimestamp();
        //we need to convert timestamp dd/MM/yyyy format
        String formattedDate = MyApplication.formatTimestamp(timestamp);

        //set data
        holder.titleTv.setText(title);
        holder.descriptionTv.setText(description);
        holder.dateTv.setText(formattedDate);
    }

    @Override
    public int getItemCount() {
        return pdfArrayList.size();
    }

    /*view holder class to hold UI views for row_pdf_admin.xml*/
    class HolderPdfAdmin extends RecyclerView.ViewHolder{
        //UI views for row_pdf_admin.xml
        PDFView pdfView;
        ProgressBar progressBar;
        TextView titleTv, descriptionTv, categoryTv, sizeTv, dateTv;
        ImageButton moreBtn;

        public HolderPdfAdmin(@NonNull View itemView) {
            super(itemView);

            //init ui views
            pdfView = binding.rowPdfAdminPDFView;
            progressBar = binding.PDFViewProgressbar;
            titleTv = binding.rowPdfTitleTv;
            descriptionTv = binding.rowPdfDescriptionTv;
            categoryTv = binding.rowPdfCategoryTv;
            sizeTv = binding.rowPdfSizeTv;
            dateTv = binding.rowPdfDateTv;
            moreBtn = binding.rowPdfMoreIBtn;

        }
    }
}
