package com.example.letsdoit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        // Inflate the child activity's layout into the content frame
        FrameLayout contentFrame = findViewById(R.id.content_frame);
        LayoutInflater.from(this).inflate(getLayoutId(), contentFrame, true);

        bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setOnNavigationItemSelectedListener(navListener);
    }

    protected abstract int getLayoutId();

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {
                Class<? extends Activity> destinationActivity = getActivityClass(item.getItemId());

                if (this.getClass() != destinationActivity) {
                    startActivity(new Intent(this, destinationActivity));
                    overridePendingTransition(0, 0); // Remove default transition
                    finish(); // Close current activity
                }
                return true;
            };

    private Class<? extends Activity> getActivityClass(int menuItemId) {
        if (menuItemId == R.id.Home) {
            return MainActivity.class;
        } else if (menuItemId == R.id.Dupli) {
            return duplicate_main.class;
//        } else if (menuItemId == R.id.Conversion) {
//            return ConversionActivity.class;
        } else if (menuItemId == R.id.Files) {
            return Filesearch.class;
        }
        return MainActivity.class;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNavigationBarState();
    }

    private void updateNavigationBarState() {
        int menuItemId = getNavigationMenuItemId();
        bottomNav.getMenu().findItem(menuItemId).setChecked(true);
    }

    protected abstract int getNavigationMenuItemId();
}
