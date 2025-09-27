package io.amcp.testing;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Test Configuration for AMCP Testing Framework
 * 
 * Centralized configuration for all testing scenarios including:
 * - TestContainers configuration
 * - Performance test parameters
 * - Security test settings
 * - Chaos engineering parameters
 * - Test environment setup
 * - Resource limits and timeouts
 */
public class TestConfiguration {
    
    // General test settings
    private final boolean enableTestContainers;
    private final boolean enablePerformanceTests;
    private final boolean enableSecurityTests;
    private final boolean enableChaosTests;
    private final boolean enableParallelExecution;
    
    // TestContainers settings
    private final Set<String> enabledContainers;
    private final Map<String, String> containerImages;
    private final Map<String, Integer> containerPorts;
    private final Map<String, Map<String, String>> containerEnvironment;
    
    // Performance test settings
    private final int maxConcurrentThreads;
    private final int performanceTestDuration;
    private final int maxEventsPerSecond;
    private final int latencyTestSamples;
    
    // Security test settings
    private final boolean enableAuthenticationTests;
    private final boolean enableAuthorizationTests;
    private final boolean enableEncryptionTests;
    private final boolean enableVulnerabilityTests;
    private final Map<String, String> securityTestCredentials;
    
    // Chaos engineering settings
    private final boolean enableNetworkChaos;
    private final boolean enableServiceChaos;
    private final boolean enableResourceChaos;
    private final double chaosInjectionRate;
    private final int chaosTestDuration;
    
    // Infrastructure settings
    private final Map<String, String> kafkaSettings;
    private final Map<String, String> databaseSettings;
    private final Map<String, String> redisSettings;
    private final Map<String, String> elasticsearchSettings;
    
    // Test execution settings
    private final int testTimeoutSeconds;
    private final int retryAttempts;
    private final boolean failFast;
    private final boolean generateReports;
    private final String reportOutputPath;
    
    private TestConfiguration(Builder builder) {
        this.enableTestContainers = builder.enableTestContainers;
        this.enablePerformanceTests = builder.enablePerformanceTests;
        this.enableSecurityTests = builder.enableSecurityTests;
        this.enableChaosTests = builder.enableChaosTests;
        this.enableParallelExecution = builder.enableParallelExecution;
        
        this.enabledContainers = new HashSet<>(builder.enabledContainers);
        this.containerImages = new HashMap<>(builder.containerImages);
        this.containerPorts = new HashMap<>(builder.containerPorts);
        this.containerEnvironment = new HashMap<>(builder.containerEnvironment);
        
        this.maxConcurrentThreads = builder.maxConcurrentThreads;
        this.performanceTestDuration = builder.performanceTestDuration;
        this.maxEventsPerSecond = builder.maxEventsPerSecond;
        this.latencyTestSamples = builder.latencyTestSamples;
        
        this.enableAuthenticationTests = builder.enableAuthenticationTests;
        this.enableAuthorizationTests = builder.enableAuthorizationTests;
        this.enableEncryptionTests = builder.enableEncryptionTests;
        this.enableVulnerabilityTests = builder.enableVulnerabilityTests;
        this.securityTestCredentials = new HashMap<>(builder.securityTestCredentials);
        
        this.enableNetworkChaos = builder.enableNetworkChaos;
        this.enableServiceChaos = builder.enableServiceChaos;
        this.enableResourceChaos = builder.enableResourceChaos;
        this.chaosInjectionRate = builder.chaosInjectionRate;
        this.chaosTestDuration = builder.chaosTestDuration;
        
        this.kafkaSettings = new HashMap<>(builder.kafkaSettings);
        this.databaseSettings = new HashMap<>(builder.databaseSettings);
        this.redisSettings = new HashMap<>(builder.redisSettings);
        this.elasticsearchSettings = new HashMap<>(builder.elasticsearchSettings);
        
        this.testTimeoutSeconds = builder.testTimeoutSeconds;
        this.retryAttempts = builder.retryAttempts;
        this.failFast = builder.failFast;
        this.generateReports = builder.generateReports;
        this.reportOutputPath = builder.reportOutputPath;
    }
    
    // Getters for general settings
    public boolean isTestContainersEnabled() { return enableTestContainers; }
    public boolean isPerformanceTestsEnabled() { return enablePerformanceTests; }
    public boolean isSecurityTestsEnabled() { return enableSecurityTests; }
    public boolean isChaosTestsEnabled() { return enableChaosTests; }
    public boolean isParallelExecutionEnabled() { return enableParallelExecution; }
    
    // Getters for TestContainers settings
    public Set<String> getEnabledContainers() { return new HashSet<>(enabledContainers); }
    public Map<String, String> getContainerImages() { return new HashMap<>(containerImages); }
    public Map<String, Integer> getContainerPorts() { return new HashMap<>(containerPorts); }
    public Map<String, Map<String, String>> getContainerEnvironment() { return new HashMap<>(containerEnvironment); }
    
    public String getContainerImage(String containerName) {
        return containerImages.get(containerName);
    }
    
    public Integer getContainerPort(String containerName) {
        return containerPorts.get(containerName);
    }
    
    public Map<String, String> getContainerEnv(String containerName) {
        return containerEnvironment.getOrDefault(containerName, new HashMap<>());
    }
    
    // Getters for performance settings
    public int getMaxConcurrentThreads() { return maxConcurrentThreads; }
    public int getPerformanceTestDuration() { return performanceTestDuration; }
    public int getMaxEventsPerSecond() { return maxEventsPerSecond; }
    public int getLatencyTestSamples() { return latencyTestSamples; }
    
    // Getters for security settings
    public boolean isAuthenticationTestsEnabled() { return enableAuthenticationTests; }
    public boolean isAuthorizationTestsEnabled() { return enableAuthorizationTests; }
    public boolean isEncryptionTestsEnabled() { return enableEncryptionTests; }
    public boolean isVulnerabilityTestsEnabled() { return enableVulnerabilityTests; }
    public Map<String, String> getSecurityTestCredentials() { return new HashMap<>(securityTestCredentials); }
    
    // Getters for chaos settings
    public boolean isNetworkChaosEnabled() { return enableNetworkChaos; }
    public boolean isServiceChaosEnabled() { return enableServiceChaos; }
    public boolean isResourceChaosEnabled() { return enableResourceChaos; }
    public double getChaosInjectionRate() { return chaosInjectionRate; }
    public int getChaosTestDuration() { return chaosTestDuration; }
    
    // Getters for infrastructure settings
    public Map<String, String> getKafkaSettings() { return new HashMap<>(kafkaSettings); }
    public Map<String, String> getDatabaseSettings() { return new HashMap<>(databaseSettings); }
    public Map<String, String> getRedisSettings() { return new HashMap<>(redisSettings); }
    public Map<String, String> getElasticsearchSettings() { return new HashMap<>(elasticsearchSettings); }
    
    // Getters for execution settings
    public int getTestTimeoutSeconds() { return testTimeoutSeconds; }
    public int getRetryAttempts() { return retryAttempts; }
    public boolean isFailFast() { return failFast; }
    public boolean isGenerateReports() { return generateReports; }
    public String getReportOutputPath() { return reportOutputPath; }
    
    /**
     * Builder for TestConfiguration
     */
    public static class Builder {
        // General test settings
        private boolean enableTestContainers = true;
        private boolean enablePerformanceTests = true;
        private boolean enableSecurityTests = true;
        private boolean enableChaosTests = false; // Disabled by default
        private boolean enableParallelExecution = true;
        
        // TestContainers settings
        private final Set<String> enabledContainers = new HashSet<>();
        private final Map<String, String> containerImages = new HashMap<>();
        private final Map<String, Integer> containerPorts = new HashMap<>();
        private final Map<String, Map<String, String>> containerEnvironment = new HashMap<>();
        
        // Performance test settings
        private int maxConcurrentThreads = 10;
        private int performanceTestDuration = 60; // seconds
        private int maxEventsPerSecond = 10000;
        private int latencyTestSamples = 1000;
        
        // Security test settings
        private boolean enableAuthenticationTests = true;
        private boolean enableAuthorizationTests = true;
        private boolean enableEncryptionTests = true;
        private boolean enableVulnerabilityTests = true;
        private final Map<String, String> securityTestCredentials = new HashMap<>();
        
        // Chaos engineering settings
        private boolean enableNetworkChaos = true;
        private boolean enableServiceChaos = true;
        private boolean enableResourceChaos = true;
        private double chaosInjectionRate = 0.1; // 10%
        private int chaosTestDuration = 120; // seconds
        
        // Infrastructure settings
        private final Map<String, String> kafkaSettings = new HashMap<>();
        private final Map<String, String> databaseSettings = new HashMap<>();
        private final Map<String, String> redisSettings = new HashMap<>();
        private final Map<String, String> elasticsearchSettings = new HashMap<>();
        
        // Test execution settings
        private int testTimeoutSeconds = 300; // 5 minutes
        private int retryAttempts = 3;
        private boolean failFast = false;
        private boolean generateReports = true;
        private String reportOutputPath = "./test-reports";
        
        public Builder enableTestContainers(boolean enable) {
            this.enableTestContainers = enable;
            return this;
        }
        
        public Builder enablePerformanceTests(boolean enable) {
            this.enablePerformanceTests = enable;
            return this;
        }
        
        public Builder enableSecurityTests(boolean enable) {
            this.enableSecurityTests = enable;
            return this;
        }
        
        public Builder enableChaosTests(boolean enable) {
            this.enableChaosTests = enable;
            return this;
        }
        
        public Builder enableParallelExecution(boolean enable) {
            this.enableParallelExecution = enable;
            return this;
        }
        
        public Builder enableContainer(String containerName) {
            this.enabledContainers.add(containerName);
            return this;
        }
        
        public Builder containerImage(String containerName, String image) {
            this.containerImages.put(containerName, image);
            return this;
        }
        
        public Builder containerPort(String containerName, int port) {
            this.containerPorts.put(containerName, port);
            return this;
        }
        
        public Builder containerEnv(String containerName, String key, String value) {
            this.containerEnvironment.computeIfAbsent(containerName, k -> new HashMap<>()).put(key, value);
            return this;
        }
        
        public Builder maxConcurrentThreads(int threads) {
            this.maxConcurrentThreads = threads;
            return this;
        }
        
        public Builder performanceTestDuration(int seconds) {
            this.performanceTestDuration = seconds;
            return this;
        }
        
        public Builder maxEventsPerSecond(int events) {
            this.maxEventsPerSecond = events;
            return this;
        }
        
        public Builder latencyTestSamples(int samples) {
            this.latencyTestSamples = samples;
            return this;
        }
        
        public Builder enableAuthenticationTests(boolean enable) {
            this.enableAuthenticationTests = enable;
            return this;
        }
        
        public Builder enableAuthorizationTests(boolean enable) {
            this.enableAuthorizationTests = enable;
            return this;
        }
        
        public Builder enableEncryptionTests(boolean enable) {
            this.enableEncryptionTests = enable;
            return this;
        }
        
        public Builder enableVulnerabilityTests(boolean enable) {
            this.enableVulnerabilityTests = enable;
            return this;
        }
        
        public Builder securityCredential(String key, String value) {
            this.securityTestCredentials.put(key, value);
            return this;
        }
        
        public Builder enableNetworkChaos(boolean enable) {
            this.enableNetworkChaos = enable;
            return this;
        }
        
        public Builder enableServiceChaos(boolean enable) {
            this.enableServiceChaos = enable;
            return this;
        }
        
        public Builder enableResourceChaos(boolean enable) {
            this.enableResourceChaos = enable;
            return this;
        }
        
        public Builder chaosInjectionRate(double rate) {
            this.chaosInjectionRate = rate;
            return this;
        }
        
        public Builder chaosTestDuration(int seconds) {
            this.chaosTestDuration = seconds;
            return this;
        }
        
        public Builder kafkaSetting(String key, String value) {
            this.kafkaSettings.put(key, value);
            return this;
        }
        
        public Builder databaseSetting(String key, String value) {
            this.databaseSettings.put(key, value);
            return this;
        }
        
        public Builder redisSetting(String key, String value) {
            this.redisSettings.put(key, value);
            return this;
        }
        
        public Builder elasticsearchSetting(String key, String value) {
            this.elasticsearchSettings.put(key, value);
            return this;
        }
        
        public Builder testTimeout(int seconds) {
            this.testTimeoutSeconds = seconds;
            return this;
        }
        
        public Builder retryAttempts(int attempts) {
            this.retryAttempts = attempts;
            return this;
        }
        
        public Builder failFast(boolean enable) {
            this.failFast = enable;
            return this;
        }
        
        public Builder generateReports(boolean enable) {
            this.generateReports = enable;
            return this;
        }
        
        public Builder reportOutputPath(String path) {
            this.reportOutputPath = path;
            return this;
        }
        
        public TestConfiguration build() {
            // Set default container images if not specified
            if (enableTestContainers) {
                containerImages.putIfAbsent("kafka", "confluentinc/cp-kafka:latest");
                containerImages.putIfAbsent("zookeeper", "confluentinc/cp-zookeeper:latest");
                containerImages.putIfAbsent("postgresql", "postgres:13");
                containerImages.putIfAbsent("redis", "redis:6-alpine");
                containerImages.putIfAbsent("elasticsearch", "elasticsearch:7.15.0");
                
                // Set default ports
                containerPorts.putIfAbsent("kafka", 9092);
                containerPorts.putIfAbsent("zookeeper", 2181);
                containerPorts.putIfAbsent("postgresql", 5432);
                containerPorts.putIfAbsent("redis", 6379);
                containerPorts.putIfAbsent("elasticsearch", 9200);
            }
            
            // Set default infrastructure settings
            kafkaSettings.putIfAbsent("bootstrap.servers", "localhost:9092");
            kafkaSettings.putIfAbsent("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            kafkaSettings.putIfAbsent("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            kafkaSettings.putIfAbsent("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            kafkaSettings.putIfAbsent("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            kafkaSettings.putIfAbsent("auto.offset.reset", "earliest");
            kafkaSettings.putIfAbsent("group.id", "amcp-test-group");
            
            databaseSettings.putIfAbsent("url", "jdbc:postgresql://localhost:5432/amcp_test");
            databaseSettings.putIfAbsent("username", "testuser");
            databaseSettings.putIfAbsent("password", "testpass");
            databaseSettings.putIfAbsent("driver", "org.postgresql.Driver");
            
            redisSettings.putIfAbsent("host", "localhost");
            redisSettings.putIfAbsent("port", "6379");
            redisSettings.putIfAbsent("timeout", "2000");
            
            elasticsearchSettings.putIfAbsent("host", "localhost");
            elasticsearchSettings.putIfAbsent("port", "9200");
            elasticsearchSettings.putIfAbsent("scheme", "http");
            
            // Set default security credentials
            securityTestCredentials.putIfAbsent("test.username", "testuser");
            securityTestCredentials.putIfAbsent("test.password", "testpass");
            securityTestCredentials.putIfAbsent("admin.username", "admin");
            securityTestCredentials.putIfAbsent("admin.password", "adminpass");
            
            return new TestConfiguration(this);
        }
    }
    
    /**
     * Create default configuration
     */
    public static TestConfiguration defaultConfiguration() {
        return new Builder()
            .enableContainer("kafka")
            .enableContainer("zookeeper")
            .enableContainer("postgresql")
            .enableContainer("redis")
            .enableContainer("elasticsearch")
            .build();
    }
    
    /**
     * Create minimal configuration (only essential components)
     */
    public static TestConfiguration minimalConfiguration() {
        return new Builder()
            .enableTestContainers(true)
            .enablePerformanceTests(false)
            .enableSecurityTests(false)
            .enableChaosTests(false)
            .enableContainer("kafka")
            .enableContainer("zookeeper")
            .maxConcurrentThreads(2)
            .testTimeout(60)
            .build();
    }
    
    /**
     * Create performance-focused configuration
     */
    public static TestConfiguration performanceConfiguration() {
        return new Builder()
            .enablePerformanceTests(true)
            .enableSecurityTests(false)
            .enableChaosTests(false)
            .maxConcurrentThreads(20)
            .maxEventsPerSecond(50000)
            .performanceTestDuration(120)
            .latencyTestSamples(5000)
            .enableContainer("kafka")
            .enableContainer("zookeeper")
            .enableContainer("redis")
            .build();
    }
    
    /**
     * Create security-focused configuration
     */
    public static TestConfiguration securityConfiguration() {
        return new Builder()
            .enablePerformanceTests(false)
            .enableSecurityTests(true)
            .enableChaosTests(false)
            .enableAuthenticationTests(true)
            .enableAuthorizationTests(true)
            .enableEncryptionTests(true)
            .enableVulnerabilityTests(true)
            .enableContainer("kafka")
            .enableContainer("postgresql")
            .build();
    }
    
    /**
     * Create chaos engineering configuration
     */
    public static TestConfiguration chaosConfiguration() {
        return new Builder()
            .enablePerformanceTests(false)
            .enableSecurityTests(false)
            .enableChaosTests(true)
            .enableNetworkChaos(true)
            .enableServiceChaos(true)
            .enableResourceChaos(true)
            .chaosInjectionRate(0.2) // 20% chaos rate
            .chaosTestDuration(180)
            .enableContainer("kafka")
            .enableContainer("zookeeper")
            .enableContainer("postgresql")
            .enableContainer("redis")
            .build();
    }
    
    /**
     * Create comprehensive configuration (all features enabled)
     */
    public static TestConfiguration comprehensiveConfiguration() {
        return new Builder()
            .enableTestContainers(true)
            .enablePerformanceTests(true)
            .enableSecurityTests(true)
            .enableChaosTests(true)
            .enableParallelExecution(true)
            .maxConcurrentThreads(15)
            .performanceTestDuration(180)
            .maxEventsPerSecond(25000)
            .latencyTestSamples(2000)
            .chaosInjectionRate(0.15)
            .chaosTestDuration(240)
            .enableContainer("kafka")
            .enableContainer("zookeeper")
            .enableContainer("postgresql")
            .enableContainer("redis")
            .enableContainer("elasticsearch")
            .testTimeout(600) // 10 minutes for comprehensive tests
            .build();
    }
}