package com.example.whatsappclone.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.whatsappclone.Models.Message;
import com.example.whatsappclone.Models.User;
import com.example.whatsappclone.R;
import com.example.whatsappclone.databinding.DeleteDialogBinding;
import com.example.whatsappclone.databinding.ItemReceiveBinding;
import com.example.whatsappclone.databinding.ItemSendBinding;
import com.github.pgreze.reactions.ReactionPopup;
import com.github.pgreze.reactions.ReactionsConfig;
import com.github.pgreze.reactions.ReactionsConfigBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class MessageAdapter extends RecyclerView.Adapter {

    Context context;
    ArrayList<Message> messages;
    final int ITEM_SEND = 1;
    final int ITEM_RECEIVE = 2;
    String senderRoom;
    String receiveRoom;
    User senderUser;

    public MessageAdapter(Context context, ArrayList<Message> messages, String senderRoom, String receiveRoom, User senderUser) {
        this.context = context;
        this.messages = messages;
        this.senderRoom = senderRoom;
        this.receiveRoom = receiveRoom;
        this.senderUser = senderUser;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_SEND) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_send, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_receive, parent, false);
            return new ReceiverViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

//        final boolean[] longClickConsumed = {false};

        int reactions[] = new int[]{
                R.drawable.like,
                R.drawable.love,
                R.drawable.laugh,
                R.drawable.wow,
                R.drawable.sad,
                R.drawable.angry
        };

        ReactionsConfig config = new ReactionsConfigBuilder(context)
                .withReactions(reactions)
                .build();

        ReactionPopup popup = new ReactionPopup(context, config, (pos) -> {

            if (pos < 0)
                return false;

            if (holder.getClass() == SentViewHolder.class) {
                SentViewHolder sentViewHolder = (SentViewHolder) holder;
                sentViewHolder.binding.feeling.setImageResource(reactions[pos]);
                sentViewHolder.binding.feeling.setVisibility(View.VISIBLE);
            } else {
                ReceiverViewHolder receiverViewHolder = (ReceiverViewHolder) holder;
                receiverViewHolder.binding.feeling.setImageResource(reactions[pos]);
                receiverViewHolder.binding.feeling.setVisibility(View.VISIBLE);
            }

            message.setFeelings(pos);

            FirebaseDatabase.getInstance().getReference()
                    .child("chats")
                    .child(senderRoom)
                    .child("messages")
                    .child(message.getMessageId()).setValue(message);

            FirebaseDatabase.getInstance().getReference()
                    .child("chats")
                    .child(receiveRoom)
                    .child("messages")
                    .child(message.getMessageId()).setValue(message);

            return true; // true is closing popup, false is requesting a new selection
        });


        if (holder.getClass() == SentViewHolder.class) {
            SentViewHolder sentViewHolder = (SentViewHolder) holder;

//             Glide.with(context)
//                 .load(senderUser.getProfileImage())
//                 .placeholder(R.drawable.avatar)
//                 .into(receiverViewHolder.binding.recimg);


            if (message.getMessage().equals("photo")) {
                sentViewHolder.binding.image.setVisibility(View.VISIBLE);
                sentViewHolder.binding.message.setVisibility(View.GONE);
                Glide.with(context)
                        .load(message.getImageUri())
                        .placeholder(R.drawable.avatar)
                        .into(sentViewHolder.binding.image);
            }

            sentViewHolder.binding.message.setText(message.getMessage());

            if (message.getFeelings() >= 0) {
                sentViewHolder.binding.feeling.setImageResource(reactions[message.getFeelings()]);
                sentViewHolder.binding.feeling.setVisibility(View.VISIBLE);
            } else {
                sentViewHolder.binding.feeling.setVisibility(View.INVISIBLE);
            }

            sentViewHolder.binding.message.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
//                    if (!longClickConsumed[0]) {
//                        popup.onTouch(view, motionEvent);
//                    }
                    popup.onTouch(view, motionEvent);
                    return false;
                }
            });

            sentViewHolder.binding.image.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    popup.onTouch(view, motionEvent);
                    return false;
                }
            });

            sentViewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    Log.d("LongClick", "Long click listener triggered");
                    View view1 = LayoutInflater.from(context).inflate(R.layout.delete_dialog, null);
                    Log.d("Context", "Context: " + context);
                    DeleteDialogBinding binding = DeleteDialogBinding.bind(view1);
                    AlertDialog dialog = new AlertDialog.Builder(context)
                            .setTitle("Delete Message")
                            .setView(binding.getRoot())
                            .create();

                    binding.everyone.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            message.setMessage("This message was deleted");
                            message.setFeelings(-1);
                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(message.getMessageId())
                                    .setValue(message);

                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(receiveRoom)
                                    .child("messages")
                                    .child(message.getMessageId())
                                    .setValue(message);

                            dialog.dismiss();
                        }
                    });

                    binding.delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(message.getMessageId())
                                    .setValue(null);

                            dialog.dismiss();
                        }
                    });

                    binding.cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.dismiss();
                        }
                    });

                    dialog.show();

                    return false;
                }
            });

        } else {
            ReceiverViewHolder receiverViewHolder = (ReceiverViewHolder) holder;

            Glide.with(context)
                    .load(senderUser.getProfileImage())
                    .placeholder(R.drawable.avatar)
                    .into(receiverViewHolder.binding.recimg);

            if (message.getMessage().equals("photo")) {
                receiverViewHolder.binding.image.setVisibility(View.VISIBLE);
                receiverViewHolder.binding.message.setVisibility(View.GONE);
                Glide.with(context)
                        .load(message.getImageUri())
                        .placeholder(R.drawable.avatar)
                        .into(receiverViewHolder.binding.image);
            }

            receiverViewHolder.binding.message.setText(message.getMessage());

            if (message.getFeelings() >= 0) {
                receiverViewHolder.binding.feeling.setImageResource(reactions[message.getFeelings()]);
                receiverViewHolder.binding.feeling.setVisibility(View.VISIBLE);
            } else {
                receiverViewHolder.binding.feeling.setVisibility(View.INVISIBLE);
            }

            receiverViewHolder.binding.message.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
//                    if (!longClickConsumed[0]) {
//                        popup.onTouch(view, motionEvent);
//                    }
                    popup.onTouch(view, motionEvent);
                    return false;
                }
            });

            receiverViewHolder.binding.image.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    popup.onTouch(view, motionEvent);
                    return false;
                }
            });

            receiverViewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    Log.d("LongClick", "Long click listener triggered");
                    View view1 = LayoutInflater.from(context).inflate(R.layout.delete_dialog, null);
                    Log.d("Context", "Context: " + context);
                    DeleteDialogBinding binding = DeleteDialogBinding.bind(view1);
                    AlertDialog dialog = new AlertDialog.Builder(context)
                            .setTitle("Delete Message")
                            .setView(binding.getRoot())
                            .create();

                    binding.everyone.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            message.setMessage("This message was deleted");
                            message.setFeelings(-1);
                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(message.getMessageId())
                                    .setValue(message);

                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(receiveRoom)
                                    .child("messages")
                                    .child(message.getMessageId())
                                    .setValue(message);

                            dialog.dismiss();
                        }
                    });

                    binding.delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(message.getMessageId())
                                    .setValue(null);

                            dialog.dismiss();
                        }
                    });

                    binding.cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.dismiss();
                        }
                    });

                    dialog.show();

                    return false;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }


    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);

        if (FirebaseAuth.getInstance().getUid().equals(message.getSenderId())) {
            return ITEM_SEND;
        } else {
            return ITEM_RECEIVE;
        }
    }

    public class SentViewHolder extends RecyclerView.ViewHolder {
        ItemSendBinding binding;

        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ItemSendBinding.bind(itemView);
            Log.d("ViewHolder", "SentViewHolder constructor called");
        }
    }

    public class ReceiverViewHolder extends RecyclerView.ViewHolder {
        ItemReceiveBinding binding;

        public ReceiverViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ItemReceiveBinding.bind(itemView);
            Log.d("ViewHolder", "ReceiverViewHolder constructor called");
        }
    }
}