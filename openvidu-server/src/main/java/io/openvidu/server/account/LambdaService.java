//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.pro.account;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.openvidu.server.pro.account.usage.ClusterUsageInfo;
import io.openvidu.server.config.OpenviduConfigPro;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PreDestroy;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.ParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class LambdaService {
    private static final Logger log = LoggerFactory.getLogger(LambdaService.class);
    @Autowired
    private ClusterUsageService clusterUsageService;
    private CloseableHttpClient httpClient;
    private int licenseCheckCount = 0;
    private int failedLicenseCheckAttempts = 0;
    private final List<LicenseAndUsageInterval> LICENSE_INTERVALS = Arrays.asList(new LicenseAndUsageInterval(5, 180, 0, 1200, 0), new LicenseAndUsageInterval(5, 540, 1, 1200, 1), new LicenseAndUsageInterval(8, 1800, 2, 1200, 3), new LicenseAndUsageInterval(Integer.MAX_VALUE, 3600, 3, 1200, 9));
    private Iterator<LicenseAndUsageInterval> intervalIterator;
    private LicenseAndUsageInterval activeInterval;
    private String license;
    private String licenseUrl;
    private String publicKey;
    private final String BEGIN;
    private final String END;
    private final String LICENSE_PATH;
    private final String USAGE_PATH;
    private final int LAMBDA_TIMEOUT;
    private PublicKey pKey;
    private Thread licenseThread;
    private AtomicBoolean isLicenseThreadInterrupted;
    private Thread usageThread;
    private AtomicBoolean isUsageThreadInterrupted;

    public LambdaService(OpenviduConfigPro openviduConfigPro) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException {
        this.intervalIterator = this.LICENSE_INTERVALS.iterator();
        this.activeInterval = (LicenseAndUsageInterval)this.intervalIterator.next();
        this.publicKey = "-----BEGIN PUBLIC KEY-----\nMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCos/KpNDYiWx32rnwK1qmoZwq/\nrNoRe3BlGYHsS9uSP58sX7OrfW2LBnh/KSGbmjRPhI3gCn6hPGta5iqTdXUvHqMI\nTd4d3f5FOl2TshMmzB2BGCRi/h0sIPAdUc5zhPXK/J1VwAqT3sjuMEqkVhuTn+Qi\n5tcZ08duEV6K3F4ULwIDAQAB\n-----END PUBLIC KEY-----";
        this.BEGIN = "-----BEGIN PUBLIC KEY-----";
        this.END = "-----END PUBLIC KEY-----";
        this.LICENSE_PATH = "license";
        this.USAGE_PATH = "usage";
        this.LAMBDA_TIMEOUT = 20000;
        this.isLicenseThreadInterrupted = new AtomicBoolean(false);
        this.isUsageThreadInterrupted = new AtomicBoolean(false);
        if (openviduConfigPro.isLicenseHttpProxyDefined()) {
            HttpHost proxy = new HttpHost(openviduConfigPro.getLicenseHttpProxyHost(), Integer.parseInt(openviduConfigPro.getLicenseHttpProxyPort()));
            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
            this.httpClient = HttpClients.custom().setRoutePlanner(routePlanner).build();
        } else {
            this.httpClient = HttpClients.createDefault();
        }

        this.license = openviduConfigPro.getLicense();
        String licenseApiUrl = openviduConfigPro.getLicenseApiUrl();
        licenseApiUrl = licenseApiUrl.endsWith("/") ? licenseApiUrl : licenseApiUrl + "/";
        this.licenseUrl = licenseApiUrl;
        this.publicKey = this.toBase64DER(this.publicKey);
        this.pKey = this.loadPublicKey(this.publicKey);
    }

    public void startLicenseThread() {
        Thread.UncaughtExceptionHandler h = this.getUncaughtExceptionHandler("OpenVidu Server Pro license check thread uncaught exception");
        this.licenseThread = new Thread(() -> {
            while(!this.isLicenseThreadInterrupted.get()) {
                ++this.licenseCheckCount;
                boolean licenseActive = false;

                try {
                    licenseActive = this.isLicenseActive();
                    this.failedLicenseCheckAttempts = 0;
                } catch (Exception var4) {
                    ++this.failedLicenseCheckAttempts;
                    Logger var10000 = log;
                    String var10001 = var4.getClass().getSimpleName();
                    var10000.error("License check thread threw a " + var10001 + " when consuming GET " + this.licenseUrl + ": " + var4.getMessage());
                    if (this.failedLicenseCheckAttempts > this.activeInterval.getLicenseAllowedConsecutiveFailures()) {
                        log.error("Shutting down OpenVidu Server");
                        Runtime.getRuntime().halt(2);
                    } else {
                        log.error("WARNING! OpenVidu Server will soon shut down automatically if license check cannot be performed");
                        licenseActive = true;
                    }
                }

                if (!licenseActive) {
                    log.error("License \"{}\" is not active", this.license);
                    log.error("Shutting down OpenVidu Server");
                    Runtime.getRuntime().halt(2);
                }

                try {
                    this.adjustIntervalTimes();
                    TimeUnit.SECONDS.sleep((long)this.activeInterval.getLicenseSleepTimeInSeconds());
                } catch (InterruptedException var3) {
                    this.isLicenseThreadInterrupted.set(true);
                    log.warn("License check thread interrupted while sleeping");
                }
            }

            log.warn("License check thread is now stopped");
        });
        this.licenseThread.setUncaughtExceptionHandler(h);
        this.licenseThread.start();
    }

    public void startUsageThread() {
        this.sendFirstUsage();
        Thread.UncaughtExceptionHandler h = this.getUncaughtExceptionHandler("OpenVidu Server Pro usage thread uncaught exception");
        this.usageThread = new Thread(() -> {
            while(!this.isUsageThreadInterrupted.get()) {
                try {
                    int sleepSeconds = this.activeInterval.getUsageSleepTimeInSeconds();
                    this.clusterUsageService.setCurrentUsageIntervalSleepSeconds(sleepSeconds);
                    TimeUnit.SECONDS.sleep((long)sleepSeconds);
                } catch (InterruptedException var8) {
                    this.isUsageThreadInterrupted.set(true);
                    log.warn("Usage post thread interrupted while sleeping");
                    continue;
                }

                ClusterUsageInfo clusterUsageInfo = this.clusterUsageService.obtainUsage();
                boolean failedUsagesPosted = this.sendFailedUsages();
                if (!failedUsagesPosted) {
                    try {
                        this.postUsage(clusterUsageInfo);
                    } catch (Exception var6) {
                        log.info("Couldn't sent usage info. {}: {}", var6.getClass().getSimpleName(), var6.getMessage());

                        try {
                            this.addFailedUsage(clusterUsageInfo);
                            log.error("WARNING! OpenVidu Server will soon shut down automatically if usage cannot be sent");
                        } catch (FailedUsageInfoAttemptsExceeded var5) {
                            log.error("Exceeded max number of attempts of usage post requests");
                            log.error("Shutting down OpenVidu Server");
                            Runtime.getRuntime().halt(3);
                        }
                    }
                } else {
                    try {
                        this.addFailedUsage(clusterUsageInfo);
                    } catch (FailedUsageInfoAttemptsExceeded var7) {
                        log.error("Exceeded max number of attempts of usage post requests");
                        log.error("Shutting down OpenVidu Server");
                        Runtime.getRuntime().halt(3);
                    }
                }
            }

            log.warn("Usage post thread is now stopped");
        });
        this.usageThread.setUncaughtExceptionHandler(h);
        this.usageThread.start();
    }

    protected boolean isLicenseActive() throws IOException {
        HttpGet httpGet = new HttpGet(this.licenseUrl + "license?license=" + this.license);
        httpGet.setConfig(RequestConfig.custom().setConnectTimeout(20000).setSocketTimeout(20000).setConnectionRequestTimeout(20000).build());
        CloseableHttpResponse response = null;
        HttpEntity entity = null;

        boolean var8;
        try {
            response = this.httpClient.execute(httpGet);
            this.checkLambdaResponseStatus(response, "License GET");
            entity = response.getEntity();
            String encryptedResponse = EntityUtils.toString(entity, "UTF-8");
            String decryptedResponse = this.decrypt(encryptedResponse);
            JsonObject jsonResponse = JsonParser.parseString(decryptedResponse).getAsJsonObject();
            boolean isActive = jsonResponse.get("isActive").getAsBoolean();
            var8 = isActive;
        } finally {
            if (entity != null) {
                EntityUtils.consume(entity);
            }

            if (response != null) {
                response.close();
            }

        }

        return var8;
    }

    protected void postUsage(ClusterUsageInfo usage) throws Exception {
        long FINAL_QUANTITY = usage.getQuantity();
        long FINAL_TIMESTAMP = usage.getTimestamp();
        String FINAL_EDITION = usage.getEdition().name();
        boolean FINAL_IS_INIITAL_USAGE = usage.isInitialUsage();
        HttpPost httpPost = new HttpPost(this.licenseUrl + "usage?license=" + this.license);
        StringEntity requestEntity = new StringEntity(usage.toJsonString(), ContentType.APPLICATION_JSON);
        httpPost.setEntity(requestEntity);
        httpPost.setConfig(RequestConfig.custom().setConnectTimeout(20000).setSocketTimeout(20000).setConnectionRequestTimeout(20000).build());
        CloseableHttpResponse response = null;
        HttpEntity entity = null;

        try {
            response = this.httpClient.execute(httpPost);
            this.checkLambdaResponseStatus(response, "Usage POST");
            entity = response.getEntity();
            String encryptedResponse = EntityUtils.toString(entity, "UTF-8");
            String decryptedResponse = this.decrypt(encryptedResponse);
            JsonObject responseJson = JsonParser.parseString(decryptedResponse).getAsJsonObject();
            long responseQuantity = responseJson.get("quantity").getAsLong();
            long responseTimestamp = responseJson.get("timestamp").getAsLong();
            String responseEdition = responseJson.get("edition").getAsString();
            boolean responseIsInitialUsage = responseJson.get("isInitialUsage").getAsBoolean();
            if (responseQuantity != FINAL_QUANTITY) {
                log.error("OpenVidu account response error. Possible man-in-the-middle");
                Runtime.getRuntime().halt(3);
            }

            if (responseTimestamp != FINAL_TIMESTAMP) {
                log.error("OpenVidu account response error. Possible man-in-the-middle");
                Runtime.getRuntime().halt(3);
            }

            if (!responseEdition.equals(FINAL_EDITION)) {
                log.error("OpenVidu account response error. Possible man-in-the-middle");
                Runtime.getRuntime().halt(3);
            }

            if (responseIsInitialUsage != FINAL_IS_INIITAL_USAGE) {
                log.error("OpenVidu account response error. Possible man-in-the-middle");
                Runtime.getRuntime().halt(3);
            }

            log.info("Usage successfully posted ({})", responseQuantity);
        } finally {
            if (entity != null) {
                EntityUtils.consume(entity);
            }

            if (response != null) {
                response.close();
            }

        }

    }

    private void checkLambdaResponseStatus(CloseableHttpResponse response, String requestName) throws LambdaStatusInvalidException {
        int status = response.getStatusLine().getStatusCode();
        if (status != 200) {
            String errorMessage = null;

            try {
                errorMessage = EntityUtils.toString(response.getEntity(), "UTF-8");
            } catch (IOException | ParseException var6) {
                log.error("Error getting message response from invalid HTTP status response ({}): {}", status, var6.getMessage());
            }

            log.warn("{} HTTP status: {}", requestName, response.getStatusLine());
            throw new LambdaStatusInvalidException(status, errorMessage);
        } else {
            log.info("{} HTTP status: {}", requestName, response.getStatusLine());
        }
    }

    private String decrypt(String strToDecrypt) throws LicenseInvalidException {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(2, this.pKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } catch (IllegalBlockSizeException | BadPaddingException | IllegalArgumentException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException var3) {
            throw new LicenseInvalidException("Error decrypting OpenVidu account message: " + var3.getMessage());
        }
    }

    private String toBase64DER(String pem) {
        return pem.replace("\n", "").replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
    }

    private PublicKey loadPublicKey(String stored) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] data = Base64.getDecoder().decode(stored.getBytes());
        X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
        KeyFactory fact = KeyFactory.getInstance("RSA");
        return fact.generatePublic(spec);
    }

    private void adjustIntervalTimes() {
        if (this.intervalIterator.hasNext() && this.licenseCheckCount > this.activeInterval.getLicensePetitions()) {
            this.licenseCheckCount = 1;
            this.activeInterval = (LicenseAndUsageInterval)this.intervalIterator.next();
        }

    }

    private void addFailedUsage(ClusterUsageInfo info) throws FailedUsageInfoAttemptsExceeded {
        if (this.clusterUsageService.failedUsages.size() < this.activeInterval.getUsageAllowedFailures()) {
            this.clusterUsageService.failedUsages.add(info);
        } else {
            throw new FailedUsageInfoAttemptsExceeded();
        }
    }

    private void returnFailedUsage(ClusterUsageInfo info) throws FailedUsageInfoAttemptsExceeded {
        if (this.clusterUsageService.failedUsages.size() < this.activeInterval.getUsageAllowedFailures()) {
            this.clusterUsageService.failedUsages.addFirst(info);
        } else {
            throw new FailedUsageInfoAttemptsExceeded();
        }
    }

    private boolean sendFailedUsages() {
        boolean failed = false;

        while(!this.clusterUsageService.failedUsages.isEmpty() && !failed) {
            log.info("There were {} unsent usage messages", this.clusterUsageService.failedUsages.size());
            ClusterUsageInfo failedUsageInfo = (ClusterUsageInfo)this.clusterUsageService.failedUsages.poll();

            try {
                this.postUsage(failedUsageInfo);
            } catch (Exception var6) {
                try {
                    this.returnFailedUsage(failedUsageInfo);
                    failed = true;
                    log.info("Still cannot send cluster usage messages");
                } catch (FailedUsageInfoAttemptsExceeded var5) {
                    log.error("Exceeded max number of attempts of usage post requests");
                    log.error("Shutting down OpenVidu Server");
                    Runtime.getRuntime().halt(3);
                }
            }
        }

        return failed;
    }

    private void sendFirstUsage() {
        try {
            this.postUsage(this.clusterUsageService.obtainFirstUsage());
        } catch (Exception var2) {
            log.error("Initial usage post request failed");
            log.error("Shutting down OpenVidu Server");
            Runtime.getRuntime().halt(3);
        }

    }

    @PreDestroy
    public void preDestroy() {
        this.isUsageThreadInterrupted.set(true);
        this.isLicenseThreadInterrupted.set(true);
        if (this.usageThread != null) {
            this.usageThread.interrupt();
        }

        if (this.licenseThread != null) {
            this.licenseThread.interrupt();
        }

    }

    private Thread.UncaughtExceptionHandler getUncaughtExceptionHandler(final String errorMessage) {
        return new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread th, Throwable ex) {
                LambdaService.log.error("{}: {}", errorMessage, ex.getClass().getCanonicalName());
                if (ThreadDeath.class.equals(ex.getClass())) {
                    LambdaService.log.error("Shutting down OpenVidu Server");
                    Runtime.getRuntime().halt(2);
                }

            }
        };
    }
}
