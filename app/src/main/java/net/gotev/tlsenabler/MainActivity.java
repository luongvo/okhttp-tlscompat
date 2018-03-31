package net.gotev.tlsenabler;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import net.gotev.okhttptlscompat.OkHttpTlsCompat;

import java.io.IOException;
import java.security.Provider;
import java.security.Security;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class MainActivity extends AppCompatActivity implements Callback {

    // The server can be anything, but it has to support only TLS 1.2 and NOT TLS 1.1 or TLS 1.0
    private static final String BASE_URL = "https://fancyssl.hboeck.de/";

    private OkHttpClient standardClient;
    private OkHttpClient tlsEnabledClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.default_client)
    public void testWithDefaultClient() {
        Log.i("Client", "Test with default client");
        getStandardClient().newCall(getRequest()).enqueue(this);
    }

    @OnClick(R.id.tls_enabled_client)
    public void testWithTLSEnabledClient() {
        Log.i("Client", "Test with TLS enabled client");
        getTlsEnabledClient().newCall(getRequest()).enqueue(this);
    }

    @OnClick(R.id.cipher_suites)
    public void onCipherSuites() {

        StringBuilder builder = new StringBuilder();

        for (Provider provider : Security.getProviders()) {
            builder.append(String.format(Locale.getDefault(), "Provider: %s", provider.getName()));

            for (Object o : provider.keySet()) {
                String entry = (String) o;
                builder.append(String.format(Locale.getDefault(),
                        "\n%s \t %s", entry, provider.getProperty(entry)));
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Cipher suites")
                .setMessage(builder.toString())
                .show();
    }

    private Request getRequest() {
        return new Request.Builder().url(BASE_URL).build();
    }

    private OkHttpClient getStandardClient() {
        if (standardClient == null) {
            standardClient = getOkHttpBuilder().build();
        }

        return standardClient;
    }

    private OkHttpClient getTlsEnabledClient() {
        if (tlsEnabledClient == null) {
            tlsEnabledClient = OkHttpTlsCompat.apply(getOkHttpBuilder(), ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT).build();
        }

        return tlsEnabledClient;
    }

    private OkHttpClient.Builder getOkHttpBuilder() {
        return new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .writeTimeout(2, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .addNetworkInterceptor(new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                    @Override
                    public void log(String message) {
                        Log.i("OkHttp", message);
                    }
                }))
                .cache(null);
    }

    @Override
    public void onFailure(Call call, final IOException e) {
        e.printStackTrace();
        Log.e("Client", "Error while performing request!");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error")
                        .setMessage(e.getMessage())
                        .show();
            }
        });
    }

    @Override
    public void onResponse(Call call, final Response response) throws IOException {

        final String body = response.body().string();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (response.isSuccessful()) {
                        Log.i("Client", "Request successful:\n " + body);

                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Success")
                                .setMessage(body)
                                .show();

                    } else {
                        Log.e("Client", "Response is not successful. Code: " + response.code());

                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Error")
                                .setMessage("HTTP " + response.code())
                                .show();
                    }
                } catch (Exception exc) {
                    Log.e("Client", "Exception on response", exc);
                }
            }
        });

    }
}
