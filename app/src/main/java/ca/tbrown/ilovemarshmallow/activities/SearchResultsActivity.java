package ca.tbrown.ilovemarshmallow.activities;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;

import butterknife.ButterKnife;
import ca.tbrown.ilovemarshmallow.Constants;
import ca.tbrown.ilovemarshmallow.R;
import ca.tbrown.ilovemarshmallow.adapters.ResultAdapter;
import ca.tbrown.ilovemarshmallow.api.Zappos;
import ca.tbrown.ilovemarshmallow.listeners.EndlessRecyclerViewOnScrollListener;
import ca.tbrown.ilovemarshmallow.pojo.Response;
import ca.tbrown.ilovemarshmallow.pojo.Result;
import retrofit.Callback;
import retrofit.RetrofitError;

public class SearchResultsActivity extends SearchBarActivity {

    // UI
    private RecyclerView rvResults;
    private ResultAdapter adapter;
    private LinearLayoutManager layoutManager;
    private ProgressBar progressBar;

    // Business Logic
    private ArrayList<Result> searchResults;
    public String currentPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);
        ButterKnife.bind(this);
        searchQuery = getSearchQuery();
        setupToolbar();
        launchProgressBar();
        if (savedInstanceState != null) {
            // activity launched via configutation change
            restoreSearchData(savedInstanceState);
            setupRecyclerView();
            populateRecyclerView();

        } else {
            // activity launched via parent or back nav
            currentPage = "1";
            setupRecyclerView();
            searchForProducts(searchQuery);
        }
    }

    private void launchProgressBar() {
        progressBar = (ProgressBar) findViewById(R.id.progressBarResults);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void restoreSearchData(Bundle savedInstanceState) {
        searchQuery = savedInstanceState.getString(Constants.QUERY);
        searchResults = savedInstanceState.getParcelableArrayList(Constants.SEARCH_RESULTS);
        currentPage = savedInstanceState.getString(Constants.PAGE);
    }

    private void populateRecyclerView() {
        adapter = new ResultAdapter(activityContext, searchResults, searchQuery);
        rvResults.setAdapter(adapter);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // save key info when activity destroyed
        outState.putString(Constants.QUERY, searchQuery);
        outState.putParcelableArrayList(Constants.SEARCH_RESULTS, searchResults);
        outState.putString(Constants.PAGE, currentPage);
        super.onSaveInstanceState(outState);
    }

    private void setupRecyclerView() {
        // setup but don't populate recyclerview
        rvResults = (RecyclerView) findViewById(R.id.rvResults);
        rvResults.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(activityContext);
        rvResults.setLayoutManager(layoutManager);
        setupEndlessScroll(rvResults);
    }

    private void setupEndlessScroll(RecyclerView rvResults) {
        rvResults.setOnScrollListener(new EndlessScrollListener(layoutManager));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        /*
        Necessary for activities with the launchMode="singleTop" attribute set in Manifest File.
        When there is a need to launch another instance of this activity, the current instance
        will be recycled and the onNewIntent method will be called to handle the intent used
        to launch the 'new' instance
         */
        setIntent(intent);
        searchQuery = getSearchQuery();
        if (intent.getBooleanExtra(Constants.IS_BACK_NAV, false) == true) {
            // when activity accessed via back nav
            Zappos.getAPI().searchProductsByPage(searchQuery, currentPage, new LoadMoreResponsesCallback());

        } else {
            currentPage = "1";
            setupRecyclerView();
            searchForProducts(searchQuery);
        }
    }
    @Override
    public void setupSearchBox(Menu menu) {
        super.setupSearchBox(menu);
        searchbox.requestFocus();
    }

    private String getSearchQuery() {
        String query = null;
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            // if activity launched do to execution of search bar
            query = intent.getStringExtra(SearchManager.QUERY);
        }

        if (query == null) {
            query = intent.getStringExtra(Constants.QUERY);
        }

        return query;
    }

    private void searchForProducts(String query) {
        Zappos.getAPI().searchProductsByPage(query, currentPage, new CallbackResponse());
    }


    private class EndlessScrollListener extends EndlessRecyclerViewOnScrollListener {
        // Extension of ScrollListener that notifies when user is at the end of the recyclerview
        public EndlessScrollListener(LinearLayoutManager linearLayoutManager) {
            super(linearLayoutManager);
        }

        @Override
        public void onLoadMore(final int current_page) {
            currentPage = Integer.toString(current_page);
            Zappos.getAPI().searchProductsByPage(searchQuery,currentPage, new LoadMoreResponsesCallback());
        }
    }

    class CallbackResponse implements Callback<Response> {

        @Override
        public void success(Response response, retrofit.client.Response fullResponse) {

            searchResults = response.getResults();
            adapter = new ResultAdapter(activityContext, searchResults, searchQuery);
            rvResults.setAdapter(adapter);
            progressBar.setVisibility(View.GONE);

        }

        @Override
        public void failure(RetrofitError error) {
            if (currentPage.equals("1")) {
                Toast.makeText(getApplicationContext(), "No search results found", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activityContext, "There are no more items to load", Toast.LENGTH_SHORT).show();
            }
            progressBar.setVisibility(View.GONE);
        }
    }

    class LoadMoreResponsesCallback implements Callback<Response> {

        @Override
        public void success(Response response, retrofit.client.Response fullResponse) {

            // add the new search results to the existing search results
            searchResults.addAll(response.getResults());
            adapter.notifyDataSetChanged();
        }

        @Override
        public void failure(RetrofitError error) {
            if (currentPage.equals("1")) {
                Toast.makeText(getApplicationContext(), "No search results found", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activityContext, "There are no more items to load", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
