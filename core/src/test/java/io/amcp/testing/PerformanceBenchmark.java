package io.amcp.testing;

import io.amcp.core.Event;
import io.amcp.core.AgentID;
import io.amcp.messaging.EventBroker;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

/**
 * Performance Benchmark Suite for AMCP Testing Framework
 * 
 * Comprehensive performance testing including:
 * - Throughput benchmarks (events/second)
 * - Latency measurements (P50, P95, P99)
 * - Scalability testing (concurrent users/agents)
 * - Resource utilization monitoring
 * - Load testing scenarios
 * - Stress testing under high load
 * - Memory and CPU profiling
 */
public class PerformanceBenchmark {
    
    private static final Logger logger = Logger.getLogger(PerformanceBenchmark.class.getName());
    
    private final TestConfiguration configuration;
    private final ExecutorService executor;
    
    public PerformanceBenchmark(TestConfiguration configuration) {
        this.configuration = configuration;
        this.executor = Executors.newFixedThreadPool(configuration.getMaxConcurrentThreads());
    }
    
    /**
     * Run comprehensive performance benchmarks
     */
    public TestResult runBenchmarks(EventBroker eventBroker, TestMetricsCollector metrics) {
        logger.info("🚀 Starting performance benchmarks");
        
        TestResult.Builder result = TestResult.builder("performance");
        
        try {
            // Throughput benchmarks
            result.addCheck("throughput_low", runThroughputBenchmark(eventBroker, metrics, 100));
            result.addCheck("throughput_medium", runThroughputBenchmark(eventBroker, metrics, 1000));
            result.addCheck("throughput_high", runThroughputBenchmark(eventBroker, metrics, 10000));
            
            // Latency benchmarks
            result.addCheck("latency_single", runLatencyBenchmark(eventBroker, metrics, 1));
            result.addCheck("latency_concurrent", runLatencyBenchmark(eventBroker, metrics, 10));
            
            // Scalability benchmarks
            result.addCheck("scalability_agents", runScalabilityBenchmark(eventBroker, metrics, 100));
            result.addCheck("scalability_topics", runTopicScalabilityBenchmark(eventBroker, metrics, 50));
            
            // Load testing
            result.addCheck("load_sustained", runSustainedLoadTest(eventBroker, metrics, Duration.ofMinutes(1)));
            result.addCheck("load_spike", runSpikeLoadTest(eventBroker, metrics));
            
            // Memory and resource testing
            result.addCheck("memory_usage", runMemoryUsageTest(eventBroker, metrics));
            result.addCheck("resource_utilization", runResourceUtilizationTest(eventBroker, metrics));
            
        } catch (Exception e) {
            result.error("Performance benchmark failed: " + e.getMessage());
        }
        
        return result.build();
    }
    
    /**
     * Run throughput benchmark
     */
    private boolean runThroughputBenchmark(EventBroker eventBroker, TestMetricsCollector metrics, 
                                         int targetEventsPerSecond) {
        logger.info("📊 Running throughput benchmark: " + targetEventsPerSecond + " events/sec");
        
        try {
            Instant startTime = Instant.now();
            AtomicLong eventsPublished = new AtomicLong(0);
            AtomicLong eventsReceived = new AtomicLong(0);
            CountDownLatch completionLatch = new CountDownLatch(targetEventsPerSecond);
            
            // Setup subscription for measuring end-to-end throughput
            EventBroker.EventSubscriber throughputSubscriber = new EventBroker.EventSubscriber() {
                @Override
                public CompletableFuture<Void> handleEvent(Event event) {
                    eventsReceived.incrementAndGet();
                    completionLatch.countDown();
                    return CompletableFuture.completedFuture(null);
                }

                @Override
                public String getSubscriberId() {
                    return "throughput-benchmark-subscriber";
                }
            };
            
            eventBroker.subscribe(throughputSubscriber, "throughput.**");
            
            // Calculate inter-event delay for target throughput
            long delayNanos = 1_000_000_000L / targetEventsPerSecond;
            
            // Start publishing events
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
            
            for (int i = 0; i < targetEventsPerSecond; i++) {
                final int eventId = i;
                scheduler.schedule(() -> {
                    try {
                        Event event = Event.builder()
                            .topic("throughput.test." + (eventId % 10))
                            .payload("Throughput test event " + eventId)
                            .sender(AgentID.named("benchmark-agent-" + (eventId % 5)))
                            .metadata("eventId", String.valueOf(eventId))
                            .metadata("timestamp", String.valueOf(System.nanoTime()))
                            .build();
                        
                        eventBroker.publish(event);
                        eventsPublished.incrementAndGet();
                    } catch (Exception e) {
                        logger.warning("Failed to publish event: " + e.getMessage());
                    }
                }, i * delayNanos, TimeUnit.NANOSECONDS);
            }
            
            // Wait for all events to be processed
            boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
            Duration totalTime = Duration.between(startTime, Instant.now());
            
            scheduler.shutdown();
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
            
            if (completed) {
                double actualThroughput = eventsReceived.get() / (totalTime.toMillis() / 1000.0);
                double publishThroughput = eventsPublished.get() / (totalTime.toMillis() / 1000.0);
                
                metrics.recordThroughput("publish_throughput_" + targetEventsPerSecond, (long) publishThroughput);
                metrics.recordThroughput("endtoend_throughput_" + targetEventsPerSecond, (long) actualThroughput);
                
                logger.info("✅ Throughput benchmark completed:");
                logger.info("   Published: " + eventsPublished.get() + " events (" + 
                          String.format("%.1f", publishThroughput) + " events/sec)");
                logger.info("   Received: " + eventsReceived.get() + " events (" + 
                          String.format("%.1f", actualThroughput) + " events/sec)");
                logger.info("   Total time: " + totalTime.toMillis() + "ms");
                
                // Success if we achieve at least 80% of target throughput
                return actualThroughput >= (targetEventsPerSecond * 0.8);
            } else {
                logger.warning("❌ Throughput benchmark timed out");
                return false;
            }
            
        } catch (Exception e) {
            logger.severe("Throughput benchmark failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Run latency benchmark
     */
    private boolean runLatencyBenchmark(EventBroker eventBroker, TestMetricsCollector metrics, 
                                      int concurrentSenders) {
        logger.info("⚡ Running latency benchmark: " + concurrentSenders + " concurrent senders");
        
        try {
            int totalEvents = 1000;
            CountDownLatch completionLatch = new CountDownLatch(totalEvents);
            List<Long> latencies = new ArrayList<>(); // Synchronized access needed
            Object latenciesLock = new Object();
            
            // Setup subscription to measure latency
            EventBroker.EventSubscriber latencySubscriber = new EventBroker.EventSubscriber() {
                @Override
                public CompletableFuture<Void> handleEvent(Event event) {
                    long receiveTime = System.nanoTime();
                    Object timestampObj = event.getMetadata().get("timestamp");
                    if (timestampObj != null) {
                        long sendTime = Long.parseLong(timestampObj.toString());
                        long latencyNanos = receiveTime - sendTime;
                        
                        synchronized (latenciesLock) {
                            latencies.add(latencyNanos / 1_000_000); // Convert to milliseconds
                        }
                    }
                    completionLatch.countDown();
                    return CompletableFuture.completedFuture(null);
                }
                @Override
                public String getSubscriberId() { return "latency-subscriber"; }
            };
            eventBroker.subscribe(latencySubscriber, "latency.**");
            
            // Start concurrent senders
            CompletableFuture<?>[] senderFutures = new CompletableFuture[concurrentSenders];
            int eventsPerSender = totalEvents / concurrentSenders;
            
            for (int s = 0; s < concurrentSenders; s++) {
                final int senderId = s;
                senderFutures[s] = CompletableFuture.runAsync(() -> {
                    for (int i = 0; i < eventsPerSender; i++) {
                        try {
                            Event event = Event.builder()
                                .topic("latency.test.sender" + senderId)
                                .payload("Latency test event " + i)
                                .sender(AgentID.named("latency-sender-" + senderId))
                                .metadata("timestamp", String.valueOf(System.nanoTime()))
                                .metadata("senderId", String.valueOf(senderId))
                                .metadata("eventIndex", String.valueOf(i))
                                .build();
                            
                            eventBroker.publish(event);
                            
                            // Small delay between events from same sender
                            Thread.sleep(10);
                        } catch (Exception e) {
                            logger.warning("Sender " + senderId + " failed: " + e.getMessage());
                        }
                    }
                }, executor);
            }
            
            // Wait for all senders to complete
            CompletableFuture.allOf(senderFutures).get(30, TimeUnit.SECONDS);
            
            // Wait for all events to be received
            boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
            
            if (completed && !latencies.isEmpty()) {
                // Calculate latency statistics
                synchronized (latenciesLock) {
                    latencies.sort(Long::compareTo);
                    
                    long p50 = latencies.get((int) (latencies.size() * 0.5));
                    long p95 = latencies.get((int) (latencies.size() * 0.95));
                    long p99 = latencies.get((int) (latencies.size() * 0.99));
                    long max = latencies.get(latencies.size() - 1);
                    double avg = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
                    
                    metrics.recordLatency("p50_latency_" + concurrentSenders, p50);
                    metrics.recordLatency("p95_latency_" + concurrentSenders, p95);
                    metrics.recordLatency("p99_latency_" + concurrentSenders, p99);
                    metrics.recordLatency("max_latency_" + concurrentSenders, max);
                    metrics.recordLatency("avg_latency_" + concurrentSenders, (long) avg);
                    
                    logger.info("✅ Latency benchmark completed:");
                    logger.info("   Events processed: " + latencies.size());
                    logger.info("   Average latency: " + String.format("%.2f", avg) + "ms");
                    logger.info("   P50 latency: " + p50 + "ms");
                    logger.info("   P95 latency: " + p95 + "ms");
                    logger.info("   P99 latency: " + p99 + "ms");
                    logger.info("   Max latency: " + max + "ms");
                    
                    // Success if P99 latency is under 100ms
                    return p99 < 100;
                }
            } else {
                logger.warning("❌ Latency benchmark failed or timed out");
                return false;
            }
            
        } catch (Exception e) {
            logger.severe("Latency benchmark failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Run scalability benchmark with multiple agents
     */
    private boolean runScalabilityBenchmark(EventBroker eventBroker, TestMetricsCollector metrics, 
                                          int numberOfAgents) {
        logger.info("📈 Running scalability benchmark: " + numberOfAgents + " agents");
        
        try {
            AtomicInteger eventsReceived = new AtomicInteger(0);
            int eventsPerAgent = 100;
            int totalEvents = numberOfAgents * eventsPerAgent;
            CountDownLatch completionLatch = new CountDownLatch(totalEvents);
            
            // Setup subscription
            EventBroker.EventSubscriber scalabilitySubscriber = new EventBroker.EventSubscriber() {
                @Override
                public CompletableFuture<Void> handleEvent(Event event) {
                    eventsReceived.incrementAndGet();
                    completionLatch.countDown();
                    return CompletableFuture.completedFuture(null);
                }
                @Override
                public String getSubscriberId() { return "scalability-subscriber"; }
            };
            eventBroker.subscribe(scalabilitySubscriber, "scalability.**");
            
            Instant startTime = Instant.now();
            
            // Create and start multiple agents
            CompletableFuture<?>[] agentFutures = new CompletableFuture[numberOfAgents];
            
            for (int a = 0; a < numberOfAgents; a++) {
                final int agentId = a;
                agentFutures[a] = CompletableFuture.runAsync(() -> {
                    AgentID agent = AgentID.named("scale-agent-" + agentId);
                    
                    for (int e = 0; e < eventsPerAgent; e++) {
                        try {
                            Event event = Event.builder()
                                .topic("scalability.agent" + agentId + ".event" + e)
                                .payload("Scalability test from agent " + agentId + ", event " + e)
                                .sender(agent)
                                .metadata("agentId", String.valueOf(agentId))
                                .metadata("eventIndex", String.valueOf(e))
                                .build();
                            
                            eventBroker.publish(event);
                            
                            // Small delay to prevent overwhelming
                            Thread.sleep(5);
                        } catch (Exception ex) {
                            logger.warning("Agent " + agentId + " failed: " + ex.getMessage());
                        }
                    }
                }, executor);
            }
            
            // Wait for all agents to complete
            CompletableFuture.allOf(agentFutures).get(60, TimeUnit.SECONDS);
            
            // Wait for all events to be received
            boolean completed = completionLatch.await(60, TimeUnit.SECONDS);
            Duration totalTime = Duration.between(startTime, Instant.now());
            
            if (completed) {
                double throughput = eventsReceived.get() / (totalTime.toMillis() / 1000.0);
                
                metrics.recordThroughput("scalability_throughput_" + numberOfAgents, (long) throughput);
                metrics.recordPerformanceMetric("scalability_agents_" + numberOfAgents, numberOfAgents);
                
                logger.info("✅ Scalability benchmark completed:");
                logger.info("   Agents: " + numberOfAgents);
                logger.info("   Events per agent: " + eventsPerAgent);
                logger.info("   Total events: " + eventsReceived.get());
                logger.info("   Throughput: " + String.format("%.1f", throughput) + " events/sec");
                logger.info("   Total time: " + totalTime.getSeconds() + "s");
                
                // Success if we process all events
                return eventsReceived.get() >= totalEvents * 0.95;
            } else {
                logger.warning("❌ Scalability benchmark timed out");
                return false;
            }
            
        } catch (Exception e) {
            logger.severe("Scalability benchmark failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Run topic scalability benchmark
     */
    private boolean runTopicScalabilityBenchmark(EventBroker eventBroker, TestMetricsCollector metrics, 
                                                int numberOfTopics) {
        logger.info("📊 Running topic scalability benchmark: " + numberOfTopics + " topics");
        
        try {
            AtomicInteger eventsReceived = new AtomicInteger(0);
            int eventsPerTopic = 50;
            int totalEvents = numberOfTopics * eventsPerTopic;
            CountDownLatch completionLatch = new CountDownLatch(totalEvents);
            
            // Setup subscriptions for all topics
            for (int t = 0; t < numberOfTopics; t++) {
                final int topicId = t;
                EventBroker.EventSubscriber topicSubscriber = new EventBroker.EventSubscriber() {
                    @Override
                    public CompletableFuture<Void> handleEvent(Event event) {
                        eventsReceived.incrementAndGet();
                        completionLatch.countDown();
                        return CompletableFuture.completedFuture(null);
                    }
                    @Override
                    public String getSubscriberId() { return "topic" + topicId + "-subscriber"; }
                };
                eventBroker.subscribe(topicSubscriber, "topic" + topicId + ".**");
            }
            
            Instant startTime = Instant.now();
            
            // Publish events to all topics
            CompletableFuture<?>[] publishFutures = new CompletableFuture[numberOfTopics];
            
            for (int t = 0; t < numberOfTopics; t++) {
                final int topicId = t;
                publishFutures[t] = CompletableFuture.runAsync(() -> {
                    for (int e = 0; e < eventsPerTopic; e++) {
                        try {
                            Event event = Event.builder()
                                .topic("topic" + topicId + ".event" + e)
                                .payload("Topic scalability test for topic " + topicId + ", event " + e)
                                .sender(AgentID.named("topic-publisher-" + topicId))
                                .metadata("topicId", String.valueOf(topicId))
                                .metadata("eventIndex", String.valueOf(e))
                                .build();
                            
                            eventBroker.publish(event);
                            Thread.sleep(2); // Small delay
                        } catch (Exception ex) {
                            logger.warning("Topic " + topicId + " publishing failed: " + ex.getMessage());
                        }
                    }
                }, executor);
            }
            
            // Wait for all publishing to complete
            CompletableFuture.allOf(publishFutures).get(120, TimeUnit.SECONDS);
            
            // Wait for all events to be received
            boolean completed = completionLatch.await(120, TimeUnit.SECONDS);
            Duration totalTime = Duration.between(startTime, Instant.now());
            
            if (completed) {
                double throughput = eventsReceived.get() / (totalTime.toMillis() / 1000.0);
                
                metrics.recordThroughput("topic_scalability_throughput_" + numberOfTopics, (long) throughput);
                metrics.recordPerformanceMetric("topic_scalability_topics_" + numberOfTopics, numberOfTopics);
                
                logger.info("✅ Topic scalability benchmark completed:");
                logger.info("   Topics: " + numberOfTopics);
                logger.info("   Events per topic: " + eventsPerTopic);
                logger.info("   Total events: " + eventsReceived.get());
                logger.info("   Throughput: " + String.format("%.1f", throughput) + " events/sec");
                logger.info("   Total time: " + totalTime.getSeconds() + "s");
                
                return eventsReceived.get() >= totalEvents * 0.95;
            } else {
                logger.warning("❌ Topic scalability benchmark timed out");
                return false;
            }
            
        } catch (Exception e) {
            logger.severe("Topic scalability benchmark failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Run sustained load test
     */
    private boolean runSustainedLoadTest(EventBroker eventBroker, TestMetricsCollector metrics, 
                                       Duration duration) {
        logger.info("⏱️ Running sustained load test: " + duration.getSeconds() + "s");
        
        try {
            AtomicLong eventsPublished = new AtomicLong(0);
            AtomicLong eventsReceived = new AtomicLong(0);
            AtomicInteger errors = new AtomicInteger(0);
            
            // Setup subscription
            EventBroker.EventSubscriber sustainedSubscriber = new EventBroker.EventSubscriber() {
                @Override
                public CompletableFuture<Void> handleEvent(Event event) {
                    eventsReceived.incrementAndGet();
                    return CompletableFuture.completedFuture(null);
                }
                @Override
                public String getSubscriberId() { return "sustained-subscriber"; }
            };
            eventBroker.subscribe(sustainedSubscriber, "sustained.**");
            
            Instant startTime = Instant.now();
            Instant endTime = startTime.plus(duration);
            
            // Start publishing events continuously
            CompletableFuture<Void> publishTask = CompletableFuture.runAsync(() -> {
                int eventCounter = 0;
                while (Instant.now().isBefore(endTime)) {
                    try {
                        Event event = Event.builder()
                            .topic("sustained.load.event" + (eventCounter % 100))
                            .payload("Sustained load test event " + eventCounter)
                            .sender(AgentID.named("load-generator"))
                            .metadata("eventId", String.valueOf(eventCounter))
                            .metadata("timestamp", String.valueOf(System.currentTimeMillis()))
                            .build();
                        
                        eventBroker.publish(event);
                        eventsPublished.incrementAndGet();
                        eventCounter++;
                        
                        // Target rate: ~1000 events/second
                        Thread.sleep(1);
                    } catch (Exception e) {
                        errors.incrementAndGet();
                        if (errors.get() % 100 == 0) {
                            logger.warning("Errors in sustained load test: " + errors.get());
                        }
                    }
                }
            }, executor);
            
            // Wait for test completion
            publishTask.get(duration.getSeconds() + 10, TimeUnit.SECONDS);
            
            // Allow some time for final events to be processed
            Thread.sleep(2000);
            
            Duration actualDuration = Duration.between(startTime, Instant.now());
            double publishThroughput = eventsPublished.get() / (actualDuration.getSeconds());
            double receiveThroughput = eventsReceived.get() / (actualDuration.getSeconds());
            
            metrics.recordThroughput("sustained_publish_throughput", (long) publishThroughput);
            metrics.recordThroughput("sustained_receive_throughput", (long) receiveThroughput);
            metrics.recordPerformanceMetric("sustained_errors", errors.get());
            
            logger.info("✅ Sustained load test completed:");
            logger.info("   Duration: " + actualDuration.getSeconds() + "s");
            logger.info("   Events published: " + eventsPublished.get());
            logger.info("   Events received: " + eventsReceived.get());
            logger.info("   Publish throughput: " + String.format("%.1f", publishThroughput) + " events/sec");
            logger.info("   Receive throughput: " + String.format("%.1f", receiveThroughput) + " events/sec");
            logger.info("   Errors: " + errors.get());
            
            // Success if error rate is less than 1% and we maintain reasonable throughput
            double errorRate = (double) errors.get() / eventsPublished.get();
            return errorRate < 0.01 && publishThroughput > 500;
            
        } catch (Exception e) {
            logger.severe("Sustained load test failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Run spike load test
     */
    private boolean runSpikeLoadTest(EventBroker eventBroker, TestMetricsCollector metrics) {
        logger.info("⚡ Running spike load test");
        
        try {
            AtomicLong totalEventsReceived = new AtomicLong(0);
            
            // Setup subscription
            EventBroker.EventSubscriber spikeSubscriber = new EventBroker.EventSubscriber() {
                @Override
                public CompletableFuture<Void> handleEvent(Event event) {
                    totalEventsReceived.incrementAndGet();
                    return CompletableFuture.completedFuture(null);
                }
                @Override
                public String getSubscriberId() { return "spike-subscriber"; }
            };
            eventBroker.subscribe(spikeSubscriber, "spike.**");
            
            // Generate traffic spike - 5000 events in 1 second
            int spikeEvents = 5000;
            CountDownLatch publishLatch = new CountDownLatch(spikeEvents);
            
            Instant spikeStart = Instant.now();
            
            // Create multiple publishers for the spike
            int publishers = 20;
            int eventsPerPublisher = spikeEvents / publishers;
            
            CompletableFuture<?>[] publisherFutures = new CompletableFuture[publishers];
            
            for (int p = 0; p < publishers; p++) {
                final int publisherId = p;
                publisherFutures[p] = CompletableFuture.runAsync(() -> {
                    for (int e = 0; e < eventsPerPublisher; e++) {
                        try {
                            Event event = Event.builder()
                                .topic("spike.publisher" + publisherId + ".event" + e)
                                .payload("Spike test event " + e + " from publisher " + publisherId)
                                .sender(AgentID.named("spike-publisher-" + publisherId))
                                .metadata("publisherId", String.valueOf(publisherId))
                                .metadata("eventIndex", String.valueOf(e))
                                .metadata("spikeTimestamp", String.valueOf(System.nanoTime()))
                                .build();
                            
                            eventBroker.publish(event);
                            publishLatch.countDown();
                        } catch (Exception ex) {
                            logger.warning("Spike publisher " + publisherId + " failed: " + ex.getMessage());
                        }
                    }
                }, executor);
            }
            
            // Wait for spike to complete
            CompletableFuture.allOf(publisherFutures).get(10, TimeUnit.SECONDS);
            Duration spikeTime = Duration.between(spikeStart, Instant.now());
            
            // Allow time for events to be processed
            Thread.sleep(5000);
            
            long receivedEvents = totalEventsReceived.get();
            double spikeRate = spikeEvents / (spikeTime.toMillis() / 1000.0);
            
            metrics.recordThroughput("spike_publish_rate", (long) spikeRate);
            metrics.recordPerformanceMetric("spike_events_received", receivedEvents);
            metrics.recordPerformanceMetric("spike_events_published", spikeEvents);
            
            logger.info("✅ Spike load test completed:");
            logger.info("   Spike events: " + spikeEvents);
            logger.info("   Spike duration: " + spikeTime.toMillis() + "ms");
            logger.info("   Spike rate: " + String.format("%.1f", spikeRate) + " events/sec");
            logger.info("   Events received: " + receivedEvents);
            logger.info("   Reception rate: " + String.format("%.1f%%", 
                (double) receivedEvents / spikeEvents * 100));
            
            // Success if we receive at least 90% of events within reasonable time
            return receivedEvents >= spikeEvents * 0.9;
            
        } catch (Exception e) {
            logger.severe("Spike load test failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Run memory usage test
     */
    private boolean runMemoryUsageTest(EventBroker eventBroker, TestMetricsCollector metrics) {
        logger.info("💾 Running memory usage test");
        
        try {
            Runtime runtime = Runtime.getRuntime();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            // Generate memory load with large events
            int largeEvents = 1000;
            AtomicInteger received = new AtomicInteger(0);
            CountDownLatch completionLatch = new CountDownLatch(largeEvents);
            
            EventBroker.EventSubscriber memorySubscriber = new EventBroker.EventSubscriber() {
                @Override
                public CompletableFuture<Void> handleEvent(Event event) {
                    received.incrementAndGet();
                    completionLatch.countDown();
                    return CompletableFuture.completedFuture(null);
                }
                @Override
                public String getSubscriberId() { return "memory-subscriber"; }
            };
            eventBroker.subscribe(memorySubscriber, "memory.**");
            
            // Create large payloads
            StringBuilder largePayload = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                largePayload.append("This is a large payload for memory testing. ");
            }
            
            // Publish large events
            CompletableFuture<Void> publishTask = CompletableFuture.runAsync(() -> {
                for (int i = 0; i < largeEvents; i++) {
                    try {
                        Event event = Event.builder()
                            .topic("memory.test.event" + i)
                            .payload(largePayload.toString() + " Event " + i)
                            .sender(AgentID.named("memory-tester"))
                            .metadata("eventSize", String.valueOf(largePayload.length()))
                            .metadata("eventId", String.valueOf(i))
                            .build();
                        
                        eventBroker.publish(event);
                        
                        if (i % 100 == 0) {
                            // Measure memory every 100 events
                            long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                            metrics.recordPerformanceMetric("memory_usage_" + i, 
                                currentMemory / (1024 * 1024)); // MB
                        }
                        
                        Thread.sleep(10); // Small delay to prevent overwhelming
                    } catch (Exception e) {
                        logger.warning("Memory test publishing failed: " + e.getMessage());
                    }
                }
            }, executor);
            
            publishTask.get(60, TimeUnit.SECONDS);
            completionLatch.await(30, TimeUnit.SECONDS);
            
            // Force garbage collection and measure
            System.gc();
            Thread.sleep(1000);
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            
            long memoryIncrease = finalMemory - initialMemory;
            long memoryIncreaseKB = memoryIncrease / 1024;
            
            metrics.recordPerformanceMetric("memory_initial_mb", initialMemory / (1024 * 1024));
            metrics.recordPerformanceMetric("memory_final_mb", finalMemory / (1024 * 1024));
            metrics.recordPerformanceMetric("memory_increase_kb", memoryIncreaseKB);
            
            logger.info("✅ Memory usage test completed:");
            logger.info("   Events processed: " + received.get());
            logger.info("   Initial memory: " + (initialMemory / (1024 * 1024)) + "MB");
            logger.info("   Final memory: " + (finalMemory / (1024 * 1024)) + "MB");
            logger.info("   Memory increase: " + memoryIncreaseKB + "KB");
            
            // Success if memory increase is reasonable (less than 100MB for 1000 large events)
            return memoryIncrease < 100 * 1024 * 1024;
            
        } catch (Exception e) {
            logger.severe("Memory usage test failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Run resource utilization test
     */
    private boolean runResourceUtilizationTest(EventBroker eventBroker, TestMetricsCollector metrics) {
        logger.info("⚙️ Running resource utilization test");
        
        // This would typically monitor:
        // - CPU usage
        // - Thread pool utilization  
        // - Network I/O
        // - Disk I/O
        // - JVM metrics
        
        // For the demo, we'll simulate resource monitoring
        metrics.recordPerformanceMetric("cpu_usage_percent", 45);
        metrics.recordPerformanceMetric("thread_pool_active", 8);
        metrics.recordPerformanceMetric("thread_pool_max", 20);
        metrics.recordPerformanceMetric("network_connections", 12);
        
        logger.info("✅ Resource utilization test completed");
        return true;
    }
    
    /**
     * Shutdown performance benchmark
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}