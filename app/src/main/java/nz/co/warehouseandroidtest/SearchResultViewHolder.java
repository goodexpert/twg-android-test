package nz.co.warehouseandroidtest;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import nz.co.warehouseandroidtest.data.ProductWithoutPrice;

public class SearchResultViewHolder extends RecyclerView.ViewHolder {

    private View mItemView;
    private ImageView mIvProduct;
    private TextView mTvProductName;

    public SearchResultViewHolder(View itemView) {
        super(itemView);
        mItemView = itemView;

        mIvProduct = mItemView.findViewById(R.id.iv_product);
        mTvProductName = mItemView.findViewById(R.id.tv_product_name);
    }

    public void bind(final ProductWithoutPrice product) {
        if (product == null) return;

        if (!TextUtils.isEmpty(product.Description)) {
            mTvProductName.setText(product.Description);
        }

        if (!TextUtils.isEmpty(product.ImageURL)) {
            Glide.with(mIvProduct.getContext()).load(product.ImageURL).into(mIvProduct);
        }

        mItemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent  = new Intent();
                intent.setClass(mItemView.getContext(), ProductDetailActivity.class);
                intent.putExtra(ProductDetailActivity.FLAG_BAR_CODE, product.Barcode);
                mItemView.getContext().startActivity(intent);
            }
        });
    }

}
