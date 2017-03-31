package org.a5calls.android.a5calls.controller;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.Contact;
import org.a5calls.android.a5calls.model.Issue;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * More details about an issue, including links to the phone app to call and buttons to record
 * your calls.
 */
public class IssueActivity extends AppCompatActivity {
    private static final String TAG = "IssueActivity";
    public static final String KEY_ISSUE = "key_issue";

    private final AccountManager accountManager = AccountManager.Instance;

    private Issue mIssue;
    private Tracker mTracker = null;
    private boolean mIsAnimating = false;

    @BindView(R.id.scroll_view) NestedScrollView scrollView;
    @BindView(R.id.issue_name) TextView issueName;
    @BindView(R.id.issue_description) TextView issueDescription;
    @BindView(R.id.no_calls_left) ViewGroup noCallsLeft;
    @BindView(R.id.update_location_btn) Button updateLocationBtn;
    @BindView(R.id.rep_prompt) ViewGroup repPrompt;
    @BindView(R.id.rep_list) LinearLayout repList;
    @BindView(R.id.bottom_sheet) NestedScrollView bottomSheet;
    @BindView(R.id.main_layout) ViewGroup issueTextSection;
    @BindView(R.id.expand_contacts_icon) ImageView expandContactsIcon;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIssue = getIntent().getParcelableExtra(KEY_ISSUE);
        if (mIssue == null) {
            // TODO handle this better? Is it even possible to get here?
            finish();
            return;
        }

        setContentView(R.layout.activity_issue);
        ButterKnife.bind(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(mIssue.name);
        }

        issueName.setText(mIssue.name);
        issueDescription.setText(mIssue.reason);

        final BottomSheetBehavior<NestedScrollView> behavior =
                BottomSheetBehavior.from(bottomSheet);
        repPrompt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED ||
                        behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    behavior.setState(behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED ?
                            BottomSheetBehavior.STATE_EXPANDED :
                            BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });
        final int collapsedSize = getResources().getDimensionPixelSize(R.dimen.accessibility_min_size);
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            private boolean wasAtBottom = false;

            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_SETTLING ||
                        newState == BottomSheetBehavior.STATE_DRAGGING) {
                    wasAtBottom = scrollView.getHeight() + scrollView.getScrollY() >=
                            issueTextSection.getMeasuredHeight();
                    mIsAnimating = true;
                } else {
                    mIsAnimating = false;
                    expandContactsIcon.setRotation(newState == BottomSheetBehavior.STATE_EXPANDED ?
                            180 : 0);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (behavior.getState() != BottomSheetBehavior.STATE_DRAGGING &&
                        behavior.getState() != BottomSheetBehavior.STATE_SETTLING) {
                    return;
                }
                expandContactsIcon.setRotation(slideOffset * 180);
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)
                        scrollView.getLayoutParams();
                params.bottomMargin = collapsedSize + (int) ((bottomSheet.getMeasuredHeight() -
                        collapsedSize) * slideOffset);
                scrollView.setLayoutParams(params);
                // Only auto-scroll up if we are already scrolled to the bottom.
                if (wasAtBottom) {
                    scrollView.fullScroll(View.FOCUS_DOWN);
                }
            }
        });

        scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY,
                                       int oldScrollX, int oldScrollY) {
                // If we are fully scrolled and it isn't open, open it.
                if (mIsAnimating) {
                    return;
                }
                if (scrollView.getHeight() + scrollView.getScrollY() >=
                        issueTextSection.getMeasuredHeight()) {
                    if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                }

            }
        });

        // We allow Analytics opt-out.
        if (accountManager.allowAnalytics(this)) {
            // Obtain the shared Tracker instance.
            FiveCallsApplication application = (FiveCallsApplication) getApplication();
            mTracker = application.getDefaultTracker();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_ISSUE, mIssue);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mTracker != null) {
            mTracker.setScreenName(TAG);
            mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        }
        if (mIssue.contacts == null || mIssue.contacts.length == 0) {
            noCallsLeft.setVisibility(View.VISIBLE);
            updateLocationBtn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(IssueActivity.this, LocationActivity.class);
                    startActivity(intent);
                }
            });
        } else {
            loadRepList();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_issue, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_share:
                sendShare();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        returnToMain();
    }

    private void returnToMain() {
        Intent intent = new Intent();
        intent.putExtra(KEY_ISSUE, mIssue);
        if (getParent() == null) {
            setResult(Activity.RESULT_OK, intent);
        } else {
            getParent().setResult(Activity.RESULT_OK, intent);
        }
        finish();
    }

    private void sendShare() {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(
                R.string.issue_share_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                String.format(getResources().getString(R.string.issue_share_content), mIssue.name,
                        mIssue.id));
        shareIntent.setType("text/plain");

        if (mTracker != null) {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Share")
                    .setAction("IssueShare")
                    .setLabel(mIssue.id)
                    .setValue(1)
                    .build());
        }

        startActivity(Intent.createChooser(shareIntent, getResources().getString(
                R.string.share_chooser_title)));
    }

    private void loadRepList() {
        repList.removeAllViews();
        for (int i = 0; i < mIssue.contacts.length; i++) {
            View repView = LayoutInflater.from(this).inflate(R.layout.rep_list_view, null);
            populateRepView(repView, mIssue.contacts[i], i);
            repList.addView(repView);
        }
    }

    private void populateRepView(View repView, Contact contact, final int index) {
        TextView contactName = (TextView) repView.findViewById(R.id.contact_name);
        final ImageView repImage = (ImageView) repView.findViewById(R.id.rep_image);
        ImageView contactChecked = (ImageView) repView.findViewById(R.id.contact_done_img);
        TextView contactReason = (TextView) repView.findViewById(R.id.contact_reason);
        contactName.setText(contact.name);
        if (!TextUtils.isEmpty(contact.area)) {
            contactReason.setText(contact.area);
            contactReason.setVisibility(View.VISIBLE);
        } else {
            contactReason.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(contact.photoURL)) {
            repImage.setVisibility(View.VISIBLE);
            Glide.with(getApplicationContext())
                    .load(contact.photoURL)
                    .asBitmap()
                    .centerCrop()
                    .into(new BitmapImageViewTarget(repImage) {
                        @Override
                        protected void setResource(Bitmap resource) {
                            RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(
                                    repImage.getContext().getResources(), resource);
                            drawable.setCircular(true);
                            drawable.setGravity(Gravity.TOP);
                            repImage.setImageDrawable(drawable);
                        }
                    });
        } else {
            repImage.setVisibility(View.GONE);
        }
        // Show a bit about whether they've been contacted yet
        final List<String> previousCalls = AppSingleton.getInstance(this).getDatabaseHelper()
                .getCallResults(mIssue.id, contact.id);
        if (previousCalls.size() > 0) {
            contactChecked.setImageLevel(1);
        } else {
            contactChecked.setImageLevel(0);
        }

        repView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), RepCallActivity.class);
                intent.putExtra(KEY_ISSUE, mIssue);
                intent.putExtra(RepCallActivity.KEY_ADDRESS,
                        getIntent().getStringExtra(RepCallActivity.KEY_ADDRESS));
                intent.putExtra(RepCallActivity.KEY_ACTIVE_CONTACT_INDEX, index);
                startActivity(intent);
            }
        });
    }
}
