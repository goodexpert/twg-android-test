package nz.co.warehouseandroidtest;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.HashMap;

import nz.co.warehouseandroidtest.Utils.PreferenceUtil;
import nz.co.warehouseandroidtest.data.ProductDetail;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends AppCompatActivity {

    public static final int FLAG_INTENT_SOURCE_SCAN = 10001;
    public static final int FLAG_INTENT_SOURCE_SEARCH = 10002;

    public static final String FLAG_BAR_CODE = "barCode";
    public static final String FLAG_INTENT_SOURCE = "intentSource";

    private ImageView ivProduct;
    private TextView tvProduct;
    private ImageView ivClearance;
    private TextView tvPrice;
    private TextView tvBarcode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        ivProduct = (ImageView) findViewById(R.id.iv_product);
        tvProduct = (TextView) findViewById(R.id.tv_product);
        ivClearance = (ImageView) findViewById(R.id.iv_clearance);
        tvPrice = (TextView) findViewById(R.id.tv_price);
        tvBarcode = (TextView) findViewById(R.id.tv_barcode);

        String barCode = getIntent().getExtras().getString(FLAG_BAR_CODE);
        HashMap paramMap = new HashMap<String, String>();
        paramMap.put("BarCode", barCode);
        paramMap.put("MachineID", Constants.MACHINE_ID);
        paramMap.put("UserID", PreferenceUtil.getUserId(this));
        paramMap.put("Branch", Constants.BRANCH_ID);

        ((WarehouseTestApp)getApplicationContext()).getWarehouseService().getProductDetail(paramMap).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    ProductDetail productDetail = (ProductDetail) response.body();
                    Glide.with(ProductDetailActivity.this).load(productDetail.Product.ImageURL).into(ivProduct);
                    tvProduct.setText(productDetail.Product.ItemDescription);
                    tvPrice.setText("$" + productDetail.Product.Price.Price);
                    tvBarcode.setText(productDetail.Product.Barcode);
                    if (productDetail.Product.Price.Type.equals("CLR")) {
                        ivClearance.setVisibility(View.VISIBLE);
                    } else {
                        ivClearance.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(ProductDetailActivity.this, "Get product detail failed!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                Toast.makeText(ProductDetailActivity.this, "Get product detail failed!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
