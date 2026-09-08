package nz.co.warehouseandroidtest;

import java.util.Map;

import nz.co.warehouseandroidtest.data.ProductDetail;
import nz.co.warehouseandroidtest.data.SearchResult;
import nz.co.warehouseandroidtest.data.User;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;

public interface WarehouseService {

    @GET("bolt/newuser.json")
    Call<User> getNewUserId();

    @GET("bolt/price.json")
    Call<ProductDetail> getProductDetail(@QueryMap Map<String, String> paramMap);

    @GET("bolt/search.json")
    Call<SearchResult> getSearchResult(@QueryMap Map<String, String> paramMap);
}
