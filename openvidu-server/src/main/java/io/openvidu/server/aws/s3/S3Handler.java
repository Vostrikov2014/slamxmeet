//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.aws.s3;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import io.openvidu.server.pro.account.LambdaService;
import io.openvidu.server.config.OpenviduConfigPro;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3Handler {
    private static final Logger log = LoggerFactory.getLogger(S3Handler.class);
    private OpenviduConfigPro openviduConfigPro;
    private LambdaService lambdaService;
    public static String AWS_REGION;
    public static String AWS_BUCKET_NAME;
    public static String AWS_BUCKET_PATH;
    public static String AWS_BUCKET_AND_PATH;
    private static AWSCredentialsProvider awsCredentialsProvider;
    private static AmazonS3 S3CLIENT;
    private static final String randomTestFileName;

    public S3Handler(OpenviduConfigPro openviduConfigPro, LambdaService lambdaService) {
        this.openviduConfigPro = openviduConfigPro;
        this.lambdaService = lambdaService;
    }

    private void testReadBucketPermissions() {
        try {
            getS3Client();
        } catch (Exception var2) {
            this.handleAWSTestException(var2, "Read");
        }

    }

    private void testWriteBucketPermissions() {
        try {
            String randomTestFileName = RandomStringUtils.randomAlphanumeric(24);
            getS3Client().deleteObject(AWS_BUCKET_AND_PATH, randomTestFileName);
            log.info("AWS S3 client potentially has Write permissions on bucket \"{}\"", AWS_BUCKET_AND_PATH);
        } catch (Exception var2) {
            this.handleAWSTestException(var2, "Write");
        }

    }

    private void populateAwsRegion() {
        if (this.openviduConfigPro.getAwsRegion() != null) {
            if (this.openviduConfigPro.getAwsS3ServiceEndpoint() == null) {
                try {
                    Regions.fromName(this.openviduConfigPro.getAwsRegion());
                } catch (IllegalArgumentException var2) {
                    log.error("Region \"{}\" of bucket \"{}\" cannot be parsed into a known AWS region: {}", new Object[]{this.openviduConfigPro.getAwsRegion(), AWS_BUCKET_NAME, var2.getMessage()});
                    log.error("Set configuration property OPENVIDU_PRO_AWS_REGION to the right value");
                    this.waitForever();
                }
            }

            AWS_REGION = this.openviduConfigPro.getAwsRegion();
        } else {
            try {
                AWS_REGION = getS3Client().getBucketLocation(AWS_BUCKET_NAME);
            } catch (AmazonServiceException var3) {
                if (var3.getStatusCode() == 404) {
                    log.error("S3 client could not be initialized. Bucket \"{}\" does not exist", AWS_BUCKET_NAME);
                    this.waitForever();
                } else {
                    this.handleAWSTestException(var3, "GetBucketLocation");
                }
            } catch (Exception var4) {
                this.handleAWSTestException(var4, "GetBucketLocation");
            }
        }

    }

    private void handleAWSTestException(Exception e, String permissions) {
        if (e instanceof AmazonServiceException) {
            AmazonServiceException eAux = (AmazonServiceException)e;
            if (eAux.getStatusCode() == 403) {
                log.error("OpenVidu Server Pro does not have {} permissions on bucket \"{}\": {}", new Object[]{permissions, AWS_BUCKET_AND_PATH, e.getMessage()});
                this.waitForever();
            } else {
                if (eAux.getStatusCode() == 404) {
                    log.info("AWS S3 client potentially has {} permissions on bucket \"{}\"", permissions, AWS_BUCKET_AND_PATH);
                    AWS_REGION = this.openviduConfigPro.getAwsRegion();
                    return;
                }

                log.error("Unexpected AmazonServiceException checking bucket \"{}\" {} permissions: {}", new Object[]{AWS_BUCKET_AND_PATH, permissions, eAux.getErrorMessage()});
                this.waitForever();
            }
        } else {
            log.error("Unexpected error checking bucket \"{}\" {} permissions: {}", new Object[]{AWS_BUCKET_AND_PATH, permissions, e.getMessage()});
            this.waitForever();
        }

    }

    private void processAwsCredentialsAndBuildS3Client() {
        try {
            String accessKey = this.openviduConfigPro.getAwsAccessKey();
            String secretKey = this.openviduConfigPro.getAwsSecretKey();
            if (accessKey != null && secretKey != null) {
                AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
                awsCredentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
                log.info("AWS credentials provided through configuration properties OPENVIDU_PRO_AWS_ACCESS_KEY/OPENVIDU_PRO_AWS_SECRET_KEY. Using AWSStaticCredentialsProvider");
            } else {
                awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
                log.info("AWS credentials NOT provided through configuration properties OPENVIDU_PRO_AWS_ACCESS_KEY/OPENVIDU_PRO_AWS_SECRET_KEY. Using DefaultAWSCredentialsProviderChain");
            }

            AmazonS3ClientBuilder builder = (AmazonS3ClientBuilder)((AmazonS3ClientBuilder)AmazonS3ClientBuilder.standard().enableForceGlobalBucketAccess()).withCredentials(awsCredentialsProvider);
            if (this.openviduConfigPro.getAwsS3ServiceEndpoint() != null) {
                AwsClientBuilder.EndpointConfiguration endpointConfig = new AwsClientBuilder.EndpointConfiguration(this.openviduConfigPro.getAwsS3ServiceEndpoint(), this.openviduConfigPro.getAwsRegion());
                builder.withEndpointConfiguration(endpointConfig);
            } else if (this.openviduConfigPro.getAwsRegion() != null) {
                builder.withRegion(this.openviduConfigPro.getAwsRegion());
            }

            if (this.openviduConfigPro.getAwsS3PathStyleAccess() != null) {
                builder.withPathStyleAccessEnabled(this.openviduConfigPro.getAwsS3PathStyleAccess());
            }

            S3CLIENT = (AmazonS3)builder.build();
        } catch (Exception var5) {
            log.error("Error building AWS client: {}", var5.getMessage());
            this.waitForever();
        }

    }

    private void processBucketPath(String bucketAndPath) {
        bucketAndPath = bucketAndPath.trim();
        if (bucketAndPath.isEmpty()) {
            log.error("No S3 bucket defined. If property OPENVIDU_PRO_RECORDING_STORAGE=s3 then propety OPENVIDU_PRO_AWS_S3_BUCKET must have a value");
            this.waitForever();
        }

        bucketAndPath = bucketAndPath.startsWith("/") ? bucketAndPath.substring(1) : bucketAndPath;
        bucketAndPath = bucketAndPath.endsWith("/") ? bucketAndPath : bucketAndPath + "/";
        bucketAndPath = bucketAndPath.replaceAll("(/)\\1+$", "/");
        AWS_BUCKET_AND_PATH = bucketAndPath;
        long count = bucketAndPath.chars().filter((ch) -> {
            return ch == 47;
        }).count();
        if (count == 1L) {
            AWS_BUCKET_NAME = AWS_BUCKET_AND_PATH.substring(0, AWS_BUCKET_AND_PATH.length() - 1);
            AWS_BUCKET_PATH = "";
        } else {
            AWS_BUCKET_NAME = bucketAndPath.substring(0, bucketAndPath.indexOf("/"));
            AWS_BUCKET_PATH = bucketAndPath.substring(bucketAndPath.indexOf("/") + 1);
        }

    }

    public static AmazonS3 getS3Client() {
        try {
            log.debug("Testing if S3 client is still functional");
            S3CLIENT.getObject(AWS_BUCKET_AND_PATH, randomTestFileName).close();
        } catch (AmazonServiceException var1) {
            if ("NoSuchKey".equals(var1.getErrorCode())) {
                log.debug("S3 client still functional");
            } else {
                if (!"ExpiredToken".equals(var1.getErrorCode())) {
                    log.error("Unexpected AmazonServiceException when checking S3 client: {} ({}) - {}", new Object[]{var1.getErrorCode(), var1.getStatusCode(), var1.getMessage()});
                    throw var1;
                }

                log.warn("AmazonServiceException with error code 'ExpiredToken': {}", var1.getMessage());
                log.info("Calling AWSCredentialsProvider#refresh method");
                awsCredentialsProvider.refresh();
            }
        } catch (IOException var2) {
            log.error("IOException when checking S3 client· {}: {}", var2.getClass().getSimpleName(), var2.getMessage());
        } catch (Exception var3) {
            log.error("Unexpected Exception when checking S3 client. {}: {}", var3.getClass().getSimpleName(), var3.getMessage());
            throw var3;
        }

        return S3CLIENT;
    }

    @PostConstruct
    protected void init() {
        this.processBucketPath(this.openviduConfigPro.getAwsS3Bucket());
        this.processAwsCredentialsAndBuildS3Client();
        this.testReadBucketPermissions();
        this.testWriteBucketPermissions();
        this.populateAwsRegion();
        log.info("AWS S3 client successfully initialized ({})", AWS_REGION);
    }

    private void waitForever() {
        try {
            this.lambdaService.preDestroy();
            (new Semaphore(0)).acquire();
        } catch (InterruptedException var2) {
        }

    }

    static {
        String var10000 = RandomStringUtils.randomAlphanumeric(16);
        randomTestFileName = var10000 + System.currentTimeMillis();
    }
}
