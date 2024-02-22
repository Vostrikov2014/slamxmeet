//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.server.config.OpenviduBuildInfo;
import io.openvidu.server.utils.CommandExecutor;
import io.openvidu.server.utils.UpdatableTimerTask;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax0.license3j.License;
import javax0.license3j.crypto.LicenseKeyPair;
import javax0.license3j.crypto.LicenseKeyPair.Create;
import javax0.license3j.io.LicenseReader;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.Baseboard;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HardwareAbstractionLayer;

@Component("specialLicenseConfig")
@ConditionalOnProperty(
        name = {"OPENVIDU_PRO_LICENSE_OFFLINE"},
        havingValue = "true"
)
public class SpecialLicenseConfig {
    private static final Logger log = LoggerFactory.getLogger(SpecialLicenseConfig.class);
    @Autowired
    private OpenviduConfigPro openviduConfigPro;
    @Autowired
    private OpenviduBuildInfo openviduBuildInfo;
    private final String licenseFileName = "license.bin";
    private long validity;
    public static int numCores = 0;
    private UpdatableTimerTask dateValidationTimer;
    private byte[] key = new byte[]{82, 83, 65, 47, 69, 67, 66, 47, 80, 75, 67, 83, 49, 80, 97, 100, 100, 105, 110, 103, 0, 48, -127, -97, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -127, -115, 0, 48, -127, -119, 2, -127, -127, 0, -33, 53, -36, 81, -48, -100, 102, 102, 90, 41, -71, -121, -27, 123, -1, 43, 58, 59, -73, 89, -31, -80, 29, 70, 61, -55, -54, 66, 31, 96, 19, 108, -91, -110, 95, 26, 32, 48, -81, 7, 75, -60, -55, 96, 16, 119, -79, 96, -103, 29, -25, 87, -127, 124, -50, 64, 34, 35, 73, -75, 98, -77, -94, 110, -91, 107, -113, 127, 121, 83, 84, -76, -113, -69, 112, -122, -1, 28, 37, -63, 6, 54, -32, -1, 99, 46, -44, 18, -53, -74, -33, 57, -85, 86, 19, 13, 107, -39, -121, 56, -76, -106, -52, 10, 54, -1, 36, -31, -65, 94, 118, 37, 7, 3, 1, 97, -67, 71, -94, -80, -101, 118, 74, 18, -79, 15, -100, 3, 2, 3, 1, 0, 1};

    public SpecialLicenseConfig() {
    }

    @PostConstruct
    void init() throws InterruptedException {
        License license = null;

        try {
            license = this.readLicense();
        } catch (Exception var6) {
            Runtime.getRuntime().halt(1);
        }

        if (license != null) {
            try {
                this.checkLicense(license);
            } catch (Exception var5) {
                System.err.println("Error with license key " + this.getLicensePath().toString());
                Runtime.getRuntime().halt(1);
            }

            this.initDateValidationTask();
        } else {
            String uniqueId = null;

            try {
                uniqueId = this.getEncryptedMachineId();
            } catch (Exception var4) {
                System.err.println(var4.getMessage());
                Runtime.getRuntime().halt(1);
            }

            String msg = "\n\n\n   Offline licensing enabled\n";
            msg = msg + "   License not found at expected path " + this.getLicensePath().toString() + "\n";
            msg = msg + "   If you need the license file, send the following information by e-mail\n   --------------------\n";
            msg = msg + "   TO:       pro.support@openvidu.io\n";
            msg = msg + "   SUBJECT:  OpenVidu Pro offline licensing\n";
            msg = msg + "   CONTENT: (copy-paste the below lines and fill the required values)\n";
            msg = msg + "             - OpenVidu Pro account email: <THE EMAIL YOUR COMPANY USED TO REGISTER AT https://openvidu.io/account>\n";
            msg = msg + "             - Number of total cores of the cluster: <NUMBER>\n";
            msg = msg + "             - Period of validity of the licence: <3 MONTHS, 1 YEAR>\n";
            msg = msg + "             - Unique ID (DO NOT MODIFY THIS VALUE): " + uniqueId + "\n";
            log.info(msg);
            (new Semaphore(0)).acquire();
        }

    }

    private License readLicense() throws Exception {
        Path path = this.getLicensePath();
        if (Files.exists(path, new LinkOption[0]) && Files.isReadable(path)) {
            try {
                LicenseReader reader = new LicenseReader(path.toString());
                License license = reader.read();
                return license;
            } catch (FileNotFoundException var4) {
                System.err.println("License file not found at expected path " + path.toString());
                throw var4;
            } catch (Exception var5) {
                System.err.println("Error reading license file " + var5);
                throw var5;
            }
        } else {
            return null;
        }
    }

    private Path getLicensePath() {
        File offlineLicenseFile = new File(this.openviduConfigPro.getClusterPath() + "license.bin");
        return Paths.get(offlineLicenseFile.getAbsolutePath());
    }

    private void checkLicense(License license) throws Exception {
        if (!license.isOK(this.key)) {
            throw new Exception();
        } else if (license.get("OVMachineId") == null) {
            throw new Exception();
        } else if (license.get("OVCores") == null) {
            throw new Exception();
        } else if (license.get("OVVersion") == null) {
            throw new Exception();
        } else if (license.get("OVInitDate") == null) {
            throw new Exception();
        } else if (license.get("OVEndDate") == null) {
            throw new Exception();
        } else {
            String machineId = license.get("OVMachineId").getString();
            int numCores = license.get("OVCores").getInt();
            String version = license.get("OVVersion").getString();
            Date initDate = license.get("OVInitDate").getDate();
            Date endDate = license.get("OVEndDate").getDate();
            if (!machineId.equals(this.getMachineId())) {
                throw new Exception();
            } else if (Runtime.getRuntime().availableProcessors() > numCores) {
                throw new Exception();
            } else if (!version.equals(this.openviduBuildInfo.getVersion())) {
                throw new Exception();
            } else {
                Date currentDate = new Date(System.currentTimeMillis());
                if (!currentDate.before(initDate) && !currentDate.after(endDate)) {
                    SpecialLicenseConfig.numCores = numCores;
                    this.validity = endDate.getTime();
                } else {
                    throw new Exception();
                }
            }
        }
    }

    private String getMachineId() throws Exception {
        String machineId = this.getMachineIdJson().toString();
        return Base64.getEncoder().withoutPadding().encodeToString(machineId.getBytes(StandardCharsets.UTF_8));
    }

    private String getEncryptedMachineId() throws Exception {
        String machineId = this.getMachineIdJson().toString();
        byte[] encryptedData = this.encrypt(machineId, this.key);
        return Base64.getEncoder().withoutPadding().encodeToString(encryptedData);
    }

    private JsonObject getMachineIdJson() throws Exception {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        ComputerSystem system = hal.getComputerSystem();
        CentralProcessor cpu = hal.getProcessor();
        CentralProcessor.ProcessorIdentifier cpuId = cpu.getProcessorIdentifier();
        Baseboard bb = system.getBaseboard();
        JsonObject machineIdJson = new JsonObject();
        JsonObject computerSystem = new JsonObject();
        computerSystem.addProperty("manufacturer", system.getManufacturer());
        computerSystem.addProperty("model", system.getModel());
        computerSystem.addProperty("serialNumber", system.getSerialNumber());
        machineIdJson.add("computerSystem", computerSystem);
        JsonObject baseboard = new JsonObject();
        baseboard.addProperty("manufacturer", bb.getManufacturer());
        baseboard.addProperty("model", bb.getModel());
        baseboard.addProperty("serialNumber", bb.getSerialNumber());
        baseboard.addProperty("version", bb.getVersion());
        machineIdJson.add("baseboard", baseboard);
        JsonObject centralProcessor = new JsonObject();
        JsonObject processorIdentifier = new JsonObject();
        processorIdentifier.addProperty("family", cpuId.getFamily());
        processorIdentifier.addProperty("identifier", cpuId.getIdentifier());
        processorIdentifier.addProperty("microarchitecture", cpuId.getMicroarchitecture());
        processorIdentifier.addProperty("model", cpuId.getModel());
        processorIdentifier.addProperty("name", cpuId.getName());
        processorIdentifier.addProperty("processorID", cpuId.getProcessorID());
        processorIdentifier.addProperty("stepping", cpuId.getStepping());
        processorIdentifier.addProperty("vendor", cpuId.getVendor());
        processorIdentifier.addProperty("vendorFreq", cpuId.getVendorFreq());
        processorIdentifier.addProperty("isCpu64bit", cpuId.isCpu64bit());
        centralProcessor.add("processorIdentifier", processorIdentifier);
        centralProcessor.addProperty("logicalProcessorCount", cpu.getLogicalProcessorCount());
        centralProcessor.addProperty("physicalProcessorCount", cpu.getPhysicalProcessorCount());
        machineIdJson.add("centralProcessor", centralProcessor);
        JsonObject other = new JsonObject();
        String hostname = CommandExecutor.execCommand(500L, new String[]{"hostname"});
        other.addProperty("hostname", hostname);
        machineIdJson.add("other", other);
        Map<String, String> pyhsicalNIFs = this.getPhysicalNetworkInterfaces();
        JsonArray networkInterfaces = new JsonArray();
        pyhsicalNIFs.entrySet().forEach((entry) -> {
            JsonObject physicalInterface = new JsonObject();
            physicalInterface.addProperty("name", (String)entry.getKey());
            physicalInterface.addProperty("mac", (String)entry.getValue());
            networkInterfaces.add(physicalInterface);
        });
        machineIdJson.add("networkInterfaces", networkInterfaces);
        return machineIdJson;
    }

    private byte[] encrypt(String plaintext, byte[] publicKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException {
        LicenseKeyPair keyPair = Create.from(publicKey, 1);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(1, keyPair.getPair().getPublic());
        byte[] textBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        byte[][] textBytesDivided = this.splitBytes(textBytes, 50);
        List<Byte> finalEncryptedList = new ArrayList();
        byte[][] var8 = textBytesDivided;
        int var9 = textBytesDivided.length;

        for(int var10 = 0; var10 < var9; ++var10) {
            byte[] chunk = var8[var10];
            byte[] array = cipher.doFinal(chunk);
            byte[] var13 = array;
            int var14 = array.length;

            for(int var15 = 0; var15 < var14; ++var15) {
                byte b = var13[var15];
                finalEncryptedList.add(b);
            }
        }

        byte[] encryptedBytes = ArrayUtils.toPrimitive((Byte[])finalEncryptedList.toArray(new Byte[finalEncryptedList.size()]));
        return encryptedBytes;
    }

    private byte[][] splitBytes(byte[] data, int chunkSize) {
        int length = data.length;
        byte[][] dest = new byte[(length + chunkSize - 1) / chunkSize][];
        int destIndex = 0;
        int stopIndex = 0;

        for(int startIndex = 0; startIndex + chunkSize <= length; startIndex += chunkSize) {
            stopIndex += chunkSize;
            dest[destIndex++] = Arrays.copyOfRange(data, startIndex, stopIndex);
        }

        if (stopIndex < length) {
            dest[destIndex] = Arrays.copyOfRange(data, stopIndex, length);
        }

        return dest;
    }

    private void initDateValidationTask() {
        this.dateValidationTimer = new UpdatableTimerTask(() -> {
            if (System.currentTimeMillis() > this.validity) {
                System.err.println("Invalid license");
                Runtime.getRuntime().halt(1);
            }

        }, () -> {
            return 43200000L;
        });
        this.dateValidationTimer.updateTimer();
    }

    private Map<String, String> getPhysicalNetworkInterfaces() {
        Map<String, String> allInterfaces = this.getAllLinuxNetworkInterfaces();
        List<Map.Entry<String, String>> networkInterfaces = new ArrayList();
        allInterfaces.entrySet().forEach((entry) -> {
            String ethtoolResult = null;

            try {
                ethtoolResult = CommandExecutor.execCommand(500L, new String[]{"/bin/sh", "-c", "ethtool -P " + (String)entry.getKey()}).trim();
            } catch (InterruptedException | IOException var4) {
            }

            if (ethtoolResult != null && ethtoolResult.endsWith((String)entry.getValue())) {
                networkInterfaces.add(entry);
            }

        });
        List<String> networkDevices = new ArrayList();

        try {
            networkDevices = CommandExecutor.execCommandReturnList(500L, new String[]{"/bin/sh", "-c", "ls -l /sys/class/net/"});
            Iterator<String> it = ((List)networkDevices).iterator();
            if (((String)it.next()).startsWith("total ")) {
                it.remove();
            }
        } catch (InterruptedException | IOException var8) {
        }

        ListIterator<Map.Entry<String, String>> it = networkInterfaces.listIterator();

        while(it.hasNext()) {
            Map.Entry<String, String> next = (Map.Entry)it.next();
            Optional<String> opt = ((List)networkDevices).stream().filter((str) -> {
                return str.contains("/" + (String)next.getKey());
            }).findFirst();
            if (opt.isPresent()) {
                String folderStr = (String)opt.get();
                if (folderStr.contains("devices/virtual/net/" + (String)next.getKey())) {
                    it.remove();
                }
            }
        }

        Collections.sort(networkInterfaces, (i1, i2) -> {
            return ((String)i1.getValue()).compareTo((String)i2.getValue());
        });
        Map<String, String> result = new HashMap();
        networkInterfaces.forEach((i) -> {
            result.put((String)i.getKey(), (String)i.getValue());
        });
        return result;
    }

    private Map<String, String> getAllLinuxNetworkInterfaces() {
        Map<String, String> interfaces = new HashMap();
        List<String> devices = new ArrayList();
        Pattern pattern = Pattern.compile("^ *(.*):");

        try {
            FileReader reader = new FileReader("/proc/net/dev");

            try {
                BufferedReader in = new BufferedReader(reader);
                String line = null;

                while((line = in.readLine()) != null) {
                    Matcher m = pattern.matcher(line);
                    if (m.find()) {
                        devices.add(m.group(1).trim());
                    }
                }
            } catch (Throwable var13) {
                try {
                    reader.close();
                } catch (Throwable var12) {
                    var13.addSuppressed(var12);
                }

                throw var13;
            }

            reader.close();
        } catch (IOException var14) {
        }

        Iterator var15 = devices.iterator();

        while(var15.hasNext()) {
            String device = (String)var15.next();

            try {
                FileReader reader = new FileReader("/sys/class/net/" + device + "/address");

                try {
                    BufferedReader in = new BufferedReader(reader);
                    String addr = in.readLine().trim();
                    interfaces.put(device, addr);
                } catch (Throwable var10) {
                    try {
                        reader.close();
                    } catch (Throwable var9) {
                        var10.addSuppressed(var9);
                    }

                    throw var10;
                }

                reader.close();
            } catch (IOException var11) {
            }
        }

        return interfaces;
    }

    public JsonObject getLicenseDetails() throws Exception {
        License license = null;

        try {
            license = this.readLicense();
        } catch (Exception var4) {
            String error = "Error reading license details at " + this.getLicensePath().toString();
            log.error(error);
            throw new Exception(error);
        }

        DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss' UTC'").withZone(ZoneId.systemDefault());
        JsonObject json = new JsonObject();
        json.addProperty("initDate", DATE_TIME_FORMATTER.format(license.get("OVInitDate").getDate().toInstant().atZone(ZoneOffset.UTC)));
        json.addProperty("endDate", DATE_TIME_FORMATTER.format(license.get("OVEndDate").getDate().toInstant().atZone(ZoneOffset.UTC)));
        json.addProperty("maxCores", license.get("OVCores").getInt());
        json.addProperty("openviduVersion", license.get("OVVersion").getString());
        json.addProperty("UUID", license.getLicenseId().toString());
        return json;
    }

    @PreDestroy
    public void preDestroy() {
        if (this.dateValidationTimer != null) {
            this.dateValidationTimer.cancelTimer();
        }

        numCores = 0;
    }
}
