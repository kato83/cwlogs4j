package com.pu10g.cwlogs4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogStreamsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JsLogger {
    public static void sendLog(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var level = Optional.ofNullable(request.getParameter("level"))
                .filter(s -> !s.isEmpty())
                .orElse("info");
        var message = Optional.ofNullable(request.getParameterValues("messages"))
                .stream()
                .flatMap(Stream::of)
                .collect(Collectors.joining("\n"));
        var userAgent = Optional.ofNullable(request.getHeader("User-Agent"))
                .orElse("unknown");

        var regionId = getResourceValue.apply("aws.cloudwatch.logs.region");
        var logGroupName = getResourceValue.apply("aws.cloudwatch.logs.group");
        var logStreamName = getResourceValue.apply("aws.cloudwatch.logs.stream");

        var logsClient = CloudWatchLogsClient.builder()
                .region(Region.of(regionId))
                .build();

        var sequenceToken = getNextSequenceToken(logsClient, logGroupName, logStreamName);

        // Build a JSON log using the EmbeddedMetricFormat.
        var timestamp = System.currentTimeMillis();
        // 埋め込みメトリクスフォーマット形式で値を詰め込む
        var json = new ObjectMapper();
        var out = response.getWriter();

        try {
            InputLogEvent inputLogEvent = InputLogEvent.builder()
                    .message(json.writeValueAsString(new Aws(level, message, userAgent)))
                    .timestamp(timestamp)
                    .build();
            var putLogEventsRequest = PutLogEventsRequest.builder()
                    .overrideConfiguration(builder -> {
                                // provide the log-format header of json/emf
                                builder.headers(Map.of("x-amzn-logs-format", List.of("json/emf")));
                                overrideConfiguration.accept(builder);
                            }
                    )
                    .logEvents(List.of(inputLogEvent))
                    .logGroupName(logGroupName)
                    .logStreamName(logStreamName)
                    .sequenceToken(sequenceToken)
                    .overrideConfiguration(overrideConfiguration)
                    .build();

            logsClient.putLogEvents(putLogEventsRequest);
            out.println("{\"status\": \"success\"}");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            out.println("{\"status\": \"error\"}");
        }
    }

    private static String getNextSequenceToken(CloudWatchLogsClient logsClient, String logGroupName, String logStreamName) {
        var logStreamRequest = DescribeLogStreamsRequest.builder()
                .logGroupName(logGroupName)
                .logStreamNamePrefix(logStreamName)
                .overrideConfiguration(overrideConfiguration)
                .build();

        var describeLogStreamsResponse = logsClient.describeLogStreams(logStreamRequest);

        // Assume that a single stream is returned since a specific stream name was
        // specified in the previous request.
        return describeLogStreamsResponse.logStreams().get(0).uploadSequenceToken();
    }

    private static final Function<String, String> getResourceValue = key -> ResourceBundle.getBundle("cloud-watch-logs")
            .getString(key);

    private static final Consumer<AwsRequestOverrideConfiguration.Builder> overrideConfiguration = conf -> conf
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                    getResourceValue.apply("aws.cloudwatch.logs.accessKey"),
                    getResourceValue.apply("aws.cloudwatch.logs.secretKey")
            )));

    public static class LogModel {
        @JsonProperty("_aws")
        public Aws aws;

        public LogModel(String level, String message, String userAgent) {
            this.aws = new Aws(level, message, userAgent);
        }
    }

    private static class Aws {
        @JsonProperty("Timestamp")
        public String timeStamp = String.valueOf(System.currentTimeMillis());
        @JsonProperty("CloudWatchMetrics")
        public List<CloudWatchMetrics> cloudWatchMetricises = List.of(new CloudWatchMetrics());
        @JsonProperty("LogLevel")
        public String logLevel;
        @JsonProperty("Message")
        public String message;
        @JsonProperty("UserAgent")
        public String userAgent;

        public Aws(String level, String message, String userAgent) {
            this.logLevel = level;
            this.message = message;
            this.userAgent = userAgent;
        }
    }

    private static class CloudWatchMetrics {
        @JsonProperty("Namespace")
        public final String namespace = "JsLog";
        @JsonProperty("Dimensions")
        public final List<String> dimensions = List.of("LogLevel", "Message", "UserAgent");
        @JsonProperty("Metrics")
        public final List<Metrics> metricises = List.of();
    }

    private static class Metrics {
        @JsonProperty("Name")
        public String name;
        @JsonProperty("Unit")
        public String unit;
    }
}
