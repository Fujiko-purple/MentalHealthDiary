package com.example.mentalhealthdiary;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mentalhealthdiary.adapter.AIPersonalityAdapter;
import com.example.mentalhealthdiary.config.AIPersonalityConfig;
import com.example.mentalhealthdiary.model.AIPersonality;
import com.example.mentalhealthdiary.utils.PreferenceManager;

public class AIPersonalitySelectActivity extends AppCompatActivity 
        implements AIPersonalityAdapter.OnPersonalitySelectedListener {
    
    private RecyclerView recyclerView;
    private AIPersonalityAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_personality_select);

        // 设置Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("选择AI性格");
        }

        // 初始化RecyclerView
        recyclerView = findViewById(R.id.personalityRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // 获取当前选中的性格ID
        String currentPersonalityId = PreferenceManager.getCurrentPersonalityId(this);
        
        // 设置适配器
        adapter = new AIPersonalityAdapter(
            AIPersonalityConfig.getAllPersonalities(),
            currentPersonalityId,
            this
        );
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onPersonalitySelected(AIPersonality personality) {
        // 保存选择的性格ID
        PreferenceManager.saveCurrentPersonalityId(this, personality.getId());
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 