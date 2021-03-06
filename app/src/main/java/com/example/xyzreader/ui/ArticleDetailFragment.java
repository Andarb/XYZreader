package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.NestedScrollView;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private String mCurrentAuthor;
    private TextView mBodyTextView;
    private TextView mBylineView;
    private NestedScrollView mScrollView;
    private FloatingActionButton mFabMoreOrLess;

    private AppBarLayout mAppBar;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mAppBar = mRootView.findViewById(R.id.appbar_detail);
        mBodyTextView = mRootView.findViewById(R.id.article_body);
        mScrollView = mRootView.findViewById(R.id.body_scroll_view);
        mBylineView = mRootView.findViewById(R.id.article_byline);

        getActivity().findViewById(R.id.share_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Check out this article: ")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        // Setup logic for expanding and collapsing long body text via animated Fab button
        mFabMoreOrLess = mRootView.findViewById(R.id.more_less_fab);
        mFabMoreOrLess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int maxLines = getResources().getInteger(R.integer.detail_body_maxlines);
                Animation rotateIcon;

                if (mBodyTextView.getMaxLines() <= maxLines) {
                    mBodyTextView.setMaxLines(Integer.MAX_VALUE);
                    mAppBar.setExpanded(false);
                    mScrollView.scrollTo(0, 0);

                    rotateIcon = AnimationUtils.loadAnimation(getActivity(), R.anim.rotation_clockwise);
                    mFabMoreOrLess.setContentDescription(getString(R.string.less_fab));
                } else {
                    mScrollView.scrollTo(0, 0);
                    mAppBar.setExpanded(true);
                    mBodyTextView.setMaxLines(maxLines);

                    rotateIcon = AnimationUtils.loadAnimation(getActivity(), R.anim.rotation_anticlockwise);
                    mFabMoreOrLess.setContentDescription(getString(R.string.more_fab));
                }

                rotateIcon.setFillAfter(true);
                v.startAnimation(rotateIcon);
            }
        });

        // Set animation for subtitle text to fade into view
        ViewPager viewPager = getActivity().findViewById(R.id.pager);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                mBylineView.setAlpha(0f);
                mBylineView.setVisibility(View.VISIBLE);
                mBylineView.animate()
                        .alpha(1f)
                        .setDuration(700);
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        bindViews();


        updateStatusBar();
        return mRootView;
    }

    private void updateStatusBar() {

    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);

        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);


        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Merriweather-Light.ttf"));

        if (mCursor != null) {
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            mCurrentAuthor = mCursor.getString(ArticleLoader.Query.AUTHOR);
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                mBylineView.setText(Html.fromHtml(
                        "by " + mCurrentAuthor + " (" +
                                DateUtils.getRelativeTimeSpanString(
                                        publishedDate.getTime(),
                                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                        DateUtils.FORMAT_ABBREV_ALL).toString() + ")"));
            } else {
                // If date is before 1902, just show the string
                mBylineView.setText(Html.fromHtml("by " +
                        mCurrentAuthor +
                        " (" + outputFormat.format(publishedDate) + ")"));
            }
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)
                    .replaceAll("(\r\n|\n)", "<br />")));

            Glide.with(this).load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
                    .apply(RequestOptions.centerCropTransform())
                    .into((ImageView) mRootView.findViewById(R.id.appbar_background));
        }
        mBylineView.setAlpha(0f);
        mBylineView.setVisibility(View.VISIBLE);
        mBylineView.animate()
                .alpha(1f)
                .setDuration(700);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }
}
