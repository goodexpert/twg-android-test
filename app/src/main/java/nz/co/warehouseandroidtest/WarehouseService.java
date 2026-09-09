package nz.co.warehouseandroidtest;

import java.util.Map;

import nz.co.warehouseandroidtest.data.ProductDetail;
import nz.co.warehouseandroidtest.data.SearchResult;
import nz.co.warehouseandroidtest.data.User;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.QueryMap;

public interface WarehouseService {

    // Login is the only endpoint that requires these; Search and Product
    // authenticate on the subscription key alone.
    @Headers({
            "Authorization: Guest",
    })
    @GET("twgCSharpTest/Login.json")
    Call<User> loginAsGuest();

    @GET("twgCSharpTest/Product.json")
    Call<ProductDetail> getProductDetail(@QueryMap Map<String, String> paramMap);

    @GET("twgCSharpTest/Search.json")
    Call<SearchResult> getSearchResult(@QueryMap Map<String, String> paramMap);
}
