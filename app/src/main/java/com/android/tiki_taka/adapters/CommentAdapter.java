package com.android.tiki_taka.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.tiki_taka.R;
import com.android.tiki_taka.models.dtos.CommentItem;

import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    private ArrayList<CommentItem> commentItems;


    public CommentAdapter(ArrayList<CommentItem> commentItems) {
        this.commentItems = commentItems;
    }

    @NonNull
    @Override
    public CommentAdapter.CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_storycard_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentAdapter.CommentViewHolder holder, int position) {
        CommentItem comment = commentItems.get(position);
        //유저 이미지 뷰 렌더링
        holder.commentTextView.setText(comment.getCommentText());
        holder.createdAtTextView.setText(comment.getCreatedAt());

    }

    @Override
    public int getItemCount() {
        return commentItems.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder{
        ImageView userImgView;
        TextView commentTextView;
        TextView createdAtTextView;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            userImgView = itemView.findViewById(R.id.imageView41);
            commentTextView = itemView.findViewById(R.id.textView37);
            createdAtTextView = itemView.findViewById(R.id.textView38);

        }
    }
    @SuppressLint("NotifyDataSetChanged")
    public void setCommentsData(List<CommentItem> newCommentsData){
        commentItems.clear();
        commentItems.addAll(newCommentsData);
        notifyDataSetChanged();
    }
}
