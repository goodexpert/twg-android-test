package nz.co.warehouseandroidtest;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import nz.co.warehouseandroidtest.data.ProductWithoutPrice;

public class SearchResultAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ProductWithoutPrice> data = new ArrayList<ProductWithoutPrice>();

    private final int TYPE_ITEM = 1;
    private final int TYPE_FOOTER = 2;

    private int currentLoadState = 2;

    public final int LOADING = 1;
    public final int LOADING_COMPLETE = 2;
    public final int LOADING_END = 3;

    public void setData(List<ProductWithoutPrice> data) {
        if (data != null) {
            this.data.clear();
            this.data.addAll(data);
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position + 1 == getItemCount()) {
            return TYPE_FOOTER;
        } else {
            return TYPE_ITEM;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
            return new SearchResultViewHolder(view);
        } else if (viewType == TYPE_FOOTER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_refresh_footer, parent, false);
            return new FooterViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SearchResultViewHolder) {
            ((SearchResultViewHolder) holder).bind(data.get(position));
        } else if (holder instanceof FooterViewHolder) {
            FooterViewHolder footerViewHolder = (FooterViewHolder) holder;
            switch (currentLoadState) {
                case LOADING:
                    footerViewHolder.pbLoading.setVisibility(View.VISIBLE);
                    footerViewHolder.tvLoading.setVisibility(View.VISIBLE);
                    footerViewHolder.llEnd.setVisibility(View.GONE);
                    break;
                case LOADING_COMPLETE:
                    footerViewHolder.pbLoading.setVisibility(View.INVISIBLE);
                    footerViewHolder.tvLoading.setVisibility(View.INVISIBLE);
                    footerViewHolder.llEnd.setVisibility(View.GONE);
                    break;
                case LOADING_END:
                    footerViewHolder.pbLoading.setVisibility(View.GONE);
                    footerViewHolder.tvLoading.setVisibility(View.GONE);
                    footerViewHolder.llEnd.setVisibility(View.VISIBLE);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return data.size() + 1;
    }

    public void setLoadState(int loadState) {
        this.currentLoadState = loadState;
        notifyDataSetChanged();
    }
}
