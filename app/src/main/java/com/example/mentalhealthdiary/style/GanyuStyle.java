package com.example.mentalhealthdiary.style;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;

import com.example.mentalhealthdiary.R;

/**
 * 甘雨AI界面风格
 */
public class GanyuStyle implements AIPersonalityStyle {
    private Context context;
    
    public GanyuStyle(Context context) {
        this.context = context;
    }
    
    @Override
    public int getChatBubbleDrawable(boolean isUser) {
        if (isUser) {
            return R.drawable.chat_bubble_sent;
        } else {
            return context.getResources().getIdentifier(
                context.getString(R.string.ganyu_bubble_bg), 
                "drawable", 
                context.getPackageName()
            );
        }
    }
    
    @Override
    public int getInputBackgroundDrawable() {
        return context.getResources().getIdentifier(
            context.getString(R.string.ganyu_input_bg), 
            "drawable", 
            context.getPackageName()
        );
    }
    
    @Override
    public int getBackgroundDrawable() {
        return context.getResources().getIdentifier(
            context.getString(R.string.ganyu_chat_bg), 
            "drawable", 
            context.getPackageName()
        );
    }
    
    @Override
    public ColorStateList getPrimaryColor() {
        return ColorStateList.valueOf(context.getResources().getColor(R.color.ganyu_primary));
    }
    
    @Override
    public int getHintTextColor() {
        return context.getResources().getColor(R.color.ganyu_secondary);
    }
    
    @Override
    public Typeface getTypeface(Context context) {
        Typeface 甘雨 = FontHelper.loadTypeface(
                context,
                context.getString(R.string.ganyu_font),
                "甘雨"
        );
        return 甘雨;
    }
    
    @Override
    public String getInputHint() {
        return context.getString(R.string.ganyu_input_hint);
    }
    
    @Override
    public String getSendButtonText() {
        return context.getString(R.string.ganyu_send_button);
    }
    

    @Override
    public String transformText(String originalText) {
        if (originalText == null || originalText.isEmpty()) {
            return originalText;
        }

        // 如果已经应用了甘雨风格，则不再处理
        if (originalText.startsWith("璃月七星·") || originalText.contains("仙麟") || 
                originalText.contains("霜华矢") || originalText.contains("甘露餐")) {
            return originalText;
        }

        // 璃月特色问候语和结束语
        String[] qilinPhrases = {
            "唔…（微笑）", "嗯…（点头）", "（轻声）", "…唔", "云来海往…", 
            "（整理衣角）", "（调整发簪）", "（微微颔首）", "此事…已记录", 
            "荣誉与你同在", "夜泊群玉阁…", "甘雨已经记下了"
        };

        // 冰元素相关表达
        String[] iceExpressions = {
            "（凝雪轻落）", "（冰晶闪烁）", "（晶莹冰棱）", "（寒霜悄至）", "（霜华轻绕）"
        };

        // 选择随机结尾表达
        String endPhrase = qilinPhrases[(int)(Math.random() * qilinPhrases.length)];
        
        // 关键词替换 - 增加璃月特色和甘雨个性
        originalText = originalText
            // 人称替换
            .replaceAll("(?i)我认为", "甘雨以为")
            .replaceAll("(?i)我想", "甘雨思忖")
            .replaceAll("(?i)我会", "甘雨会")
            .replaceAll("(?i)我已", "甘雨已")
            .replaceAll("(?i)\\b我\\b", "甘雨")
            
            // 璃月风格用词
            .replaceAll("(?i)工作", "契约之责")
            .replaceAll("(?i)努力", "勤勉")
            .replaceAll("(?i)休息", "小憩片刻")
            .replaceAll("(?i)谢谢", "感恩")
            .replaceAll("(?i)朋友", "仙麟之谊")
            .replaceAll("(?i)帮助", "相助")
            .replaceAll("(?i)问题", "疑难")
            .replaceAll("(?i)快乐", "欢愉")
            .replaceAll("(?i)生气", "动怒")
            .replaceAll("(?i)担心", "忧心")
            
            // 甘雨个性化表达
            .replaceAll("(?i)很好", "颇为妥当")
            .replaceAll("(?i)不好", "不太妥当")
            .replaceAll("(?i)做完", "完成文书")
            .replaceAll("(?i)必须", "务必")
            .replaceAll("(?i)可能", "或许")
            .replaceAll("(?i)希望", "愿")
            .replaceAll("(?i)重要", "要紧");
        
        // 处理句尾表达
        if (originalText.endsWith("。") || originalText.endsWith(".")) {
            originalText = originalText.substring(0, originalText.length()-1) + "…" + endPhrase;
        } else if (originalText.endsWith("!") || originalText.endsWith("！")) {
            // 感叹句使用冰元素表达
            String icePhrase = iceExpressions[(int)(Math.random() * iceExpressions.length)];
            originalText = originalText.substring(0, originalText.length()-1) + "。" + icePhrase;
        } else if (originalText.endsWith("?") || originalText.endsWith("？")) {
            // 提问句保持原样，只添加甘雨特色结尾
            originalText = originalText + " " + qilinPhrases[0]; // 使用第一个表达
        } else if (!originalText.matches(".*[…）]$")) {
            originalText = originalText + "…" + endPhrase;
        }

        // 处理多段落文本
        String[] paragraphs = originalText.split("\n\n");
        if (paragraphs.length > 1) {
            for (int i = 0; i < paragraphs.length - 1; i++) {
                if (!paragraphs[i].matches(".*[…）]$")) {
                    String newPhrase = qilinPhrases[(int)(Math.random() * qilinPhrases.length)];
                    paragraphs[i] = paragraphs[i] + "…" + newPhrase;
                }
            }
            originalText = String.join("\n\n", paragraphs);
        }
        
        // 检测是否是打招呼
        if (originalText.toLowerCase().contains("早上好") || 
            originalText.toLowerCase().contains("中午好") ||
            originalText.toLowerCase().contains("晚上好") ||
            originalText.toLowerCase().contains("你好")) {
            return "璃月七星·甘雨向您问好。" + originalText;
        }
        
        // 处理感谢
        if (originalText.toLowerCase().contains("感谢") || originalText.toLowerCase().contains("谢谢")) {
            return "璃月七星·甘雨" + originalText + "（鞠躬）";
        }

        // 添加甘雨特色前缀
        return "璃月七星·" + originalText;
    }
    
    @Override
    public int getButtonCornerRadius() {
        return (int) context.getResources().getDimension(R.dimen.ganyu_button_radius);
    }
    
    @Override
    public int getButtonStrokeWidth() {
        return (int) context.getResources().getDimension(R.dimen.ganyu_button_stroke_width);
    }
} 