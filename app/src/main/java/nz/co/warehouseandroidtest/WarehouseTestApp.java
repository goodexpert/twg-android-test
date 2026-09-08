package nz.co.warehouseandroidtest;

import android.app.Application;

import com.google.gson.GsonBuilder;
import com.uuzuche.lib_zxing.activity.ZXingLibrary;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WarehouseTestApp extends Application {

    private WarehouseService service;

    @Override
    public void onCreate() {
        super.onCreate();

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request().newBuilder().addHeader("Ocp-Apim-Subscription-Key", Constants.SUBSCRIPTION_KEY).build();
                return chain.proceed(request);
            }
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.HTTP_URL_ENDPOINT)
                .client(builder.build())
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().create()))
                .build();
        service = retrofit.create(WarehouseService.class);

        ZXingLibrary.initDisplayOpinion(this);
    }

    public WarehouseService getWarehouseService() {
        return service;
    }
}
