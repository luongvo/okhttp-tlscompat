package net.gotev.okhttptlscompat;

import android.os.Build;
import android.util.Log;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;


public class OkHttpTlsCompat {

    // private constructor to avoid instantiation
    private OkHttpTlsCompat() { }

    /**
     * Enables TLS 1.2 on Pre-Lollipop devices (16 (Jelly Bean) >= Android API <= 21 (Lollipop)
     * Check: https://github.com/square/okhttp/issues/2372#issuecomment-244807676
     * @param client OkHttpClient.Builder on which to apply the patch
     * @param otherConnSpecs other connection specs to enable other than TLS 1.2. Example:
     *                       ConnectionSpec.COMPATIBLE_TLS or ConnectionSpec.CLEARTEXT
     *
     * @return OkHttpClient.Builder with the patch applied
     */
    public static OkHttpClient.Builder apply(OkHttpClient.Builder client, ConnectionSpec... otherConnSpecs) {
        // the problem still persists on some samsung devices with API 21, so it's necessary
        // to apply the patch till API 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            try {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory
                        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init((KeyStore) null);

                TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

                if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                    throw new IllegalStateException("Unexpected default trust managers:"
                            + Arrays.toString(trustManagers));
                }

                X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

                client.sslSocketFactory(TLSSocketFactory.getSocketFactory(), trustManager);

                List<ConnectionSpec> specs = new ArrayList<>();

                specs.add(new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build());

                if (otherConnSpecs != null && otherConnSpecs.length > 0) {
                    specs.addAll(Arrays.asList(otherConnSpecs));
                }

                client.connectionSpecs(specs);

                client.connectionSpecs(specs);
            } catch (Throwable exc) {
                Log.e(OkHttpTlsCompat.class.getSimpleName(), "Error while setting TLS 1.2", exc);
            }

        }

        return client;
    }
}
