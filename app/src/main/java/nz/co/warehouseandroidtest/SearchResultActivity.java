package nz.co.warehouseandroidtest;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nz.co.warehouseandroidtest.Utils.PreferenceUtil;
import nz.co.warehouseandroidtest.data.ProductWithoutPrice;
import nz.co.warehouseandroidtest.data.SearchResult;
import nz.co.warehouseandroidtest.data.SearchResultItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchResultActivity extends AppCompatActivity {

    public static String FLAG_KEY_WORD = "keyWord";

    private String mKeyWord;

    private RecyclerView recyclerView;

    private SwipeRefreshLayout swipeRefreshLayout;

    private SearchResultAdapter searchResultAdapter;

    private List<ProductWithoutPrice> data = new ArrayList<>();

    private String totalItemNum;

    private int startIndex = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                data.clear();
                loadData(0, 20);
                startIndex = 0;
                swipeRefreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                }, 1000);
            }
        });

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL) {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                            RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                outRect.top = SearchResultActivity.this.getResources().getDimensionPixelOffset(R.dimen.recyclerview_out_rec_top);
            }
        });

        searchResultAdapter = new SearchResultAdapter();
        searchResultAdapter.setData(data);

        recyclerView.setAdapter(searchResultAdapter);

        recyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener() {
            @Override
            public void onLoadMore() {
                searchResultAdapter.setLoadState(searchResultAdapter.LOADING);

                if (data.size() < Integer.parseInt(totalItemNum)) {
                    loadData(startIndex, 20);
                } else {
                    searchResultAdapter.setLoadState(searchResultAdapter.LOADING_END);
                }
            }
        });

        mKeyWord = getIntent().getExtras().getString(FLAG_KEY_WORD);

        loadData(startIndex, 20);
    }

    private void loadData(int startIndex, int itemsPerPage) {
        Map paramsMap = new HashMap<String, String>();
        paramsMap.put("Search", mKeyWord);
        paramsMap.put("MachineID", Constants.MACHINE_ID);
        paramsMap.put("UserID", PreferenceUtil.getUserId(this));
        paramsMap.put("Branch", Constants.BRANCH_ID);
        paramsMap.put("Start", String.valueOf(startIndex));
        paramsMap.put("Limit", String.valueOf(itemsPerPage));
        ((WarehouseTestApp)getApplicationContext()).getWarehouseService().getSearchResult(paramsMap).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    SearchResult searchResult = (SearchResult) response.body();
                    String ifFound = searchResult.Found;
                    if (ifFound.equals("Y")) {
                        totalItemNum = searchResult.HitCount;
                        SearchResultActivity.this.startIndex += 20;
                        for (int i = 0; i < searchResult.Results.size(); i++) {
                            SearchResultItem item = searchResult.Results.get(i);
                            data.add(item.Products.get(0));
                        }
                        searchResultAdapter.setData(data);
                        searchResultAdapter.setLoadState(searchResultAdapter.LOADING_COMPLETE);
                    }
                } else {
                    Toast.makeText(SearchResultActivity.this, "Search failed!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                Toast.makeText(SearchResultActivity.this, "Search failed!", Toast.LENGTH_LONG).show();
            }
        });
    }
}
