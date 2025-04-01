package com.example.mentalhealthdiary.adapter;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mentalhealthdiary.R;
import com.example.mentalhealthdiary.model.MoodEntry;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MoodEntryAdapter extends RecyclerView.Adapter<MoodEntryAdapter.ViewHolder> {
    private List<MoodEntry> entries = new ArrayList<>();
    private List<MoodEntry> originalEntries = new ArrayList<>();
    private OnEntryClickListener listener;
    private OnEntryDeleteListener deleteListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());

    // 心情常量定义 - 匹配MoodEntry中的现有分数
    private static final int MOOD_AWFUL = 1;
    private static final int MOOD_SAD = 2;
    private static final int MOOD_NEUTRAL = 3;
    private static final int MOOD_GOOD = 4;
    private static final int MOOD_HAPPY = 5;

    // 跟踪记录的展开状态
    private final List<Integer> expandedPositions = new ArrayList<>();

    // 在类开头添加新的常量
    private static final int VIEW_TYPE_NORMAL = 0;
    private static final int VIEW_TYPE_COMPACT = 1;

    // 添加一个模式标志
    private boolean gridMode = false;

    // 添加一个变量跟踪选中的位置
    private int selectedPosition = RecyclerView.NO_POSITION;

    public interface OnEntryClickListener {
        void onEntryClick(MoodEntry entry);
    }

    public interface OnEntryDeleteListener {
        void onEntryDelete(MoodEntry entry);
    }

    public void setOnEntryClickListener(OnEntryClickListener listener) {
        this.listener = listener;
    }

    public void setOnEntryDeleteListener(OnEntryDeleteListener listener) {
        this.deleteListener = listener;
    }

    // 添加设置模式的方法
    public void setGridMode(boolean gridMode) {
        this.gridMode = gridMode;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        // 根据当前模式返回不同的视图类型
        return gridMode ? VIEW_TYPE_COMPACT : VIEW_TYPE_NORMAL;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 根据视图类型加载不同的布局
        View view;
        if (viewType == VIEW_TYPE_COMPACT) {
            view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mood_entry_compact, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mood_entry, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MoodEntry entry = entries.get(position);
        
        // 设置基本数据（两种模式下都存在的元素）
        SimpleDateFormat sdf;
        if (gridMode) {
            // 紧凑模式下使用简化日期格式
            sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
        } else {
            sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());
        }
        holder.dateText.setText(sdf.format(entry.getDate()));
        
        // 设置表情和颜色（两种模式都有）
        holder.moodEmoji.setText(getMoodEmoji(entry.getMoodScore()));
        holder.moodColorIndicator.setBackgroundColor(getMoodColor(entry.getMoodScore()));
        
        // 设置心情气泡背景色（仅在普通模式存在）
        if (holder.moodBubbleBackground != null) {
            GradientDrawable bubbleBackground = (GradientDrawable) holder.moodBubbleBackground.getBackground();
            bubbleBackground.setColor(getMoodBubbleColor(entry.getMoodScore()));
        }
        
        // 处理内容文本
        String content = entry.getDiaryContent();
        if (content != null) {
            // 查找图片标记
            Pattern pattern = Pattern.compile("\\[\\[IMG:(.*?)\\]\\]");
            Matcher matcher = pattern.matcher(content);
            
            // 检查是否存在图片标记
            if (matcher.find()) {
                // 处理第一张图片（使用之前的代码）
                String fileName = matcher.group(1);
                File imageFile = new File(new File(holder.itemView.getContext().getFilesDir(), "diary_images"), fileName);
                if (imageFile.exists()) {
                    try {
                        // 加载图片
                        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                        if (bitmap != null) {
                            // 设置图片
                            holder.contentImage.setImageBitmap(bitmap);
                            holder.contentImage.setVisibility(View.VISIBLE);
                            
                            // 让图片填满整个宽度，消除边距
                            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) holder.contentImage.getLayoutParams();
                            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                            layoutParams.height = (int)(120 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
                            layoutParams.setMargins(0, 8, 0, 8); // 只保留上下边距，左右边距设为0
                            holder.contentImage.setLayoutParams(layoutParams);
                            
                            // 确保填满整个视图宽度
                            holder.contentImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            
                            // 圆角效果
                            holder.contentImage.setOutlineProvider(new ViewOutlineProvider() {
                                @Override
                                public void getOutline(View view, Outline outline) {
                                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 
                                            8f * view.getContext().getResources().getDisplayMetrics().density);
                                }
                            });
                            holder.contentImage.setClipToOutline(true);
                            
                            // 添加点击查看大图功能
                            holder.contentImage.setOnClickListener(v -> {
                                showFullImage(holder.itemView.getContext(), imageFile);
                            });
                        } else {
                            holder.contentImage.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {
                        holder.contentImage.setVisibility(View.GONE);
                    }
                } else {
                    holder.contentImage.setVisibility(View.GONE);
                }
                
                // 替换所有图片标记为空白
                String cleanContent = content.replaceAll("\\[\\[IMG:.*?\\]\\]", "").trim();
                holder.contentText.setText(cleanContent);
            } else {
                // 没有图片标记，直接设置原始内容
                holder.contentText.setText(content);
                holder.contentImage.setVisibility(View.GONE);
            }
        } else {
            holder.contentText.setText("");
            holder.contentImage.setVisibility(View.GONE);
        }
        
        // 紧凑模式下不进行以下操作
        if (!gridMode) {
            // 仅在非紧凑模式下处理展开按钮
            if (holder.expandButton != null) {
                boolean isExpanded = expandedPositions.contains(position);
                int contentLength = content != null ? content.length() : 0;
                
                if (contentLength > 100) {
                    holder.expandButton.setVisibility(View.VISIBLE);
                    holder.expandButton.setRotation(isExpanded ? 180 : 0);
                    
                    // 根据展开状态设置内容
                    if (isExpanded) {
                        holder.contentText.setMaxLines(Integer.MAX_VALUE);
                        holder.contentText.setEllipsize(null);
                    } else {
                        holder.contentText.setMaxLines(3);
                        holder.contentText.setEllipsize(TextUtils.TruncateAt.END);
                    }
                    
                    holder.expandButton.setOnClickListener(v -> {
                        // 切换展开状态
                        if (isExpanded) {
                            expandedPositions.remove(Integer.valueOf(position));
                        } else {
                            expandedPositions.add(position);
                        }
                        notifyItemChanged(position);
                    });
                } else {
                    holder.expandButton.setVisibility(View.GONE);
                    holder.contentText.setMaxLines(Integer.MAX_VALUE);
                    holder.contentText.setEllipsize(null);
                }
            }
            
            // 仅在非紧凑模式下处理天气
            if (holder.weatherContainer != null && holder.weatherText != null && holder.weatherEmoji != null) {
                String weather = entry.getWeather();
                
                if (!TextUtils.isEmpty(weather)) {
                    holder.weatherContainer.setVisibility(View.VISIBLE);
                    holder.weatherText.setText(weather);
                    holder.weatherEmoji.setText(getWeatherEmoji(weather));
                } else {
                    holder.weatherContainer.setVisibility(View.GONE);
                }
            }
        } else {
            // 紧凑模式下设置最大行数限制
            holder.contentText.setMaxLines(2);
            holder.contentText.setEllipsize(TextUtils.TruncateAt.END);
        }
        
        // 设置选中状态
        View selectionBackground = holder.itemView.findViewById(R.id.selectionBackground);
        if (selectionBackground != null) {
            selectionBackground.setSelected(position == selectedPosition);
        }
        
        // 点击整个卡片时的处理
        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            
            // 刷新之前选中的项
            if (previousSelected != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousSelected);
            }
            
            // 刷新当前选中的项
            if (selectedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(selectedPosition);
            }
            
            // 调用原有的点击处理
            if (listener != null) {
                listener.onEntryClick(entry);
            }
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public void setEntries(List<MoodEntry> entries) {
        this.originalEntries = new ArrayList<>(entries);
        this.entries = new ArrayList<>(entries);
        expandedPositions.clear(); // 重置所有展开状态
        notifyDataSetChanged();
    }

    public void resetFilter() {
        entries = new ArrayList<>(originalEntries);
        notifyDataSetChanged();
    }

    public void resetTimeFilter() {
        resetFilter();  // 重用现有的重置方法
    }

    public void filterByDate(Date startDate) {
        List<MoodEntry> filteredList = new ArrayList<>();
        for (MoodEntry entry : originalEntries) {
            if (entry.getDate().after(startDate)) {
                filteredList.add(entry);
            }
        }
        entries = filteredList;
        notifyDataSetChanged();
    }

    public void sortByDateDesc() {
        entries.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        notifyDataSetChanged();
    }

    public void sortByDateAsc() {
        entries.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        notifyDataSetChanged();
    }

    public void sortByMoodDesc() {
        entries.sort((a, b) -> b.getMoodScore() - a.getMoodScore());
        notifyDataSetChanged();
    }

    public void sortByMoodAsc() {
        entries.sort((a, b) -> a.getMoodScore() - b.getMoodScore());
        notifyDataSetChanged();
    }

    public void filterByMood(int moodScore) {
        List<MoodEntry> filteredList = new ArrayList<>();
        for (MoodEntry entry : originalEntries) {
            if (entry.getMoodScore() == moodScore) {
                filteredList.add(entry);
            }
        }
        entries = filteredList;
        notifyDataSetChanged();
    }

    /**
     * 根据心情分数获取对应的颜色
     */
    private int getMoodColor(int mood) {
        switch (mood) {
            case MOOD_HAPPY:
                return Color.parseColor("#FF8A65"); // 橙色
            case MOOD_GOOD:
                return Color.parseColor("#4FC3F7"); // 蓝色
            case MOOD_NEUTRAL:
                return Color.parseColor("#81C784"); // 绿色
            case MOOD_SAD:
                return Color.parseColor("#9575CD"); // 紫色
            case MOOD_AWFUL:
                return Color.parseColor("#E57373"); // 红色
            default:
                return Color.parseColor("#BDBDBD"); // 灰色（默认）
        }
    }
    
    /**
     * 根据心情分数获取对应的气泡背景色（更浅的色调）
     */
    private int getMoodBubbleColor(int mood) {
        switch (mood) {
            case MOOD_HAPPY:
                return Color.parseColor("#FFF3E0"); // 淡橙色
            case MOOD_GOOD:
                return Color.parseColor("#E1F5FE"); // 淡蓝色
            case MOOD_NEUTRAL:
                return Color.parseColor("#E8F5E9"); // 淡绿色
            case MOOD_SAD:
                return Color.parseColor("#EDE7F6"); // 淡紫色
            case MOOD_AWFUL:
                return Color.parseColor("#FFEBEE"); // 淡红色
            default:
                return Color.parseColor("#F5F5F5"); // 淡灰色（默认）
        }
    }
    
    /**
     * 根据心情分数获取对应的表情
     */
    private String getMoodEmoji(int mood) {
        switch (mood) {
            case MOOD_HAPPY:
                return "😄";
            case MOOD_GOOD:
                return "😊";
            case MOOD_NEUTRAL:
                return "\uD83D\uDE10";
            case MOOD_SAD:
                return "😔";
            case MOOD_AWFUL:
                return "😩";
            default:
                return "😶";
        }
    }
    
    /**
     * 根据天气类型获取对应的表情
     */
    private String getWeatherEmoji(String weather) {
        if (weather.contains("晴")) return "☀️";
        if (weather.contains("多云")) return "⛅";
        if (weather.contains("阴")) return "☁️";
        if (weather.contains("雨")) return "🌧️";
        if (weather.contains("雪")) return "❄️";
        if (weather.contains("雾")) return "🌫️";
        return "🌤️"; // 默认
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // 所有视图模式下都存在的元素
        public final TextView dateText;
        public final TextView contentText; 
        public final TextView moodEmoji;
        public final View moodColorIndicator;
        public final ImageView contentImage;
        
        // 可能只在普通视图中存在的元素
        public final ImageButton editButton;
        public final ImageButton deleteButton;
        public final View moodBubbleBackground;
        public final LinearLayout weatherContainer;
        public final TextView weatherText;
        public final TextView weatherEmoji;
        public final ImageButton expandButton;
        
        public ViewHolder(View view) {
            super(view);
            // 所有视图都必须有的基本元素
            dateText = view.findViewById(R.id.dateText);
            contentText = view.findViewById(R.id.contentText);
            moodEmoji = view.findViewById(R.id.moodEmoji);
            moodColorIndicator = view.findViewById(R.id.moodColorIndicator);
            contentImage = view.findViewById(R.id.contentImage);
            
            // 可能在紧凑视图中不存在的元素
            editButton = view.findViewById(R.id.editButton);
            deleteButton = view.findViewById(R.id.deleteButton);
            moodBubbleBackground = view.findViewById(R.id.moodBubbleBackground);
            weatherContainer = view.findViewById(R.id.weatherContainer);
            weatherText = view.findViewById(R.id.weatherText);
            weatherEmoji = view.findViewById(R.id.weatherEmoji);
            expandButton = view.findViewById(R.id.expandButton);
            
            // 设置点击事件（只为存在的按钮）
            if (editButton != null) {
                editButton.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onEntryClick(entries.get(position));
                    }
                });
            }
            
            if (deleteButton != null) {
                deleteButton.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && deleteListener != null) {
                        deleteListener.onEntryDelete(entries.get(position));
                    }
                });
            }
        }
    }

    // 显示完整大图的方法
    private void showFullImage(Context context, File imageFile) {
        Dialog dialog = new Dialog(context, R.style.FullImageDialogTheme);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.black);
        
        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setBackgroundColor(Color.BLACK);
        
        // 加载并显示原始大图
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        imageView.setImageBitmap(bitmap);
        
        // 点击关闭对话框
        imageView.setOnClickListener(v -> dialog.dismiss());
        
        dialog.setContentView(imageView);
        dialog.show();
    }

    // 添加一个方法来清除选中状态
    public void clearSelection() {
        int previousSelected = selectedPosition;
        selectedPosition = RecyclerView.NO_POSITION;
        if (previousSelected != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousSelected);
        }
    }

    // 添加获取选中项的方法
    public MoodEntry getSelectedEntry() {
        if (selectedPosition != RecyclerView.NO_POSITION && selectedPosition < entries.size()) {
            return entries.get(selectedPosition);
        }
        return null;
    }
} 