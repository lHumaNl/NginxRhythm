package com.hum;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public class Arguments {
    private final Path nginxLogPath;
    private final String logFormat;
    private final DateTimeFormatter formatTime;
    private final String destinationHost;
    private final String httpProtocol;
    private final Float speed;
    private final Float scaleLoad;
    private final Long startTimestamp;
    private final int timeout;
    private final Integer parserThreads;
    private final Integer requestsThreads;
    private final int requestQueueCapacity;
    private final RejectedExecutionHandler queuePolicy;
    private final boolean ignoreSsl;
    private final boolean closeConnectionAfterFirstByte;
    private final String username;
    private final String password;

    public Arguments(String[] args) throws ParseException, URISyntaxException {
        Options options = getOptions();
        CommandLine cmd = new DefaultParser().parse(options, args);

        if (cmd.hasOption("h") || cmd.hasOption("help")) {
            displayHelpAndExit(options);
        }

        this.nginxLogPath = Paths.get(getOptionValue(cmd, "nginxLogPath", true));
        this.logFormat = getOptionValue(cmd, "logFormat", false, "\"[$requestTime]\" \"$requestUrl\" \"$statusCode\" \"$refererHeader\" \"$userAgentHeader\" \"$destinationHost\" \"$responseTime\"");
        this.formatTime = DateTimeFormatter.ofPattern(getOptionValue(cmd, "formatTime", false, "dd/MMM/yyyy:HH:mm:ss Z"), Locale.ENGLISH);
        this.destinationHost = getOptionValue(cmd, "destinationHost", false, null);
        this.httpProtocol = getOptionValue(cmd, "httpProtocol", false, "https");
        this.speed = Float.valueOf(getOptionValue(cmd, "speed", false, "1.0"));
        this.scaleLoad = Float.valueOf(getOptionValue(cmd, "scaleLoad", false, "1.0"));
        String startTimestamp = getOptionValue(cmd, "startTimestamp", false, null);
        this.startTimestamp = startTimestamp != null ? Long.valueOf(startTimestamp) : null;
        this.timeout = Integer.parseInt(getOptionValue(cmd, "timeout", false, "30"));
        this.parserThreads = Integer.parseInt(getOptionValue(cmd, "parserThreads", false, String.valueOf(Math.max(Runtime.getRuntime().availableProcessors(), 16))));
        this.requestsThreads = Integer.parseInt(getOptionValue(cmd, "requestsThreads", false, String.valueOf(Math.max(Runtime.getRuntime().availableProcessors(), 16))));
        this.requestQueueCapacity = Integer.parseInt(getOptionValue(cmd, "requestQueueCapacity", false, "1000"));
        this.queuePolicy = determineQueuePolicy(cmd);
        this.ignoreSsl = cmd.hasOption("ignoreSsl");
        this.closeConnectionAfterFirstByte = cmd.hasOption("closeConnectionAfterFirstByte");
        this.username = getOptionValue(cmd, "username", false, null);
        this.password = getOptionValue(cmd, "password", false, null);

        String defaultLogPath = this.destinationHost != null ? this.destinationHost
                .replace("https://", "")
                .replace("http://", "") + "-nginx.log" : "nginx.log";
        String errorLogPath = Paths.get("errors-" + defaultLogPath).toString();
        String resultFilePath = Paths.get(getOptionValue(cmd, "resultFilePath", false, defaultLogPath)).toString();

        System.setProperty("logFilePath", resultFilePath);
        System.setProperty("errorLogFilePath", errorLogPath);

        if (cmd.hasOption("disableWriteToFile")) {
            System.setProperty("fileLogLevel", "OFF");
            System.setProperty("errorFileLogLevel", "OFF");
        } else {
            System.setProperty("fileLogLevel", "INFO");
            System.setProperty("errorFileLogLevel", "ERROR");
        }

        if (cmd.hasOption("disableStats")) {
            System.setProperty("consoleLogLevel", "ERROR");
        } else {
            System.setProperty("consoleLogLevel", "INFO");
        }
    }

    private String getStringForRequiredArg(String arg) {
        return "Required arg \"" + arg + "\" missed!";
    }

    private void displayHelpAndExit(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("NginxRhythm", options);
        System.exit(0);
    }

    private String getOptionValue(CommandLine cmd, String option, boolean required, String defaultValue) {
        if (cmd.hasOption(option)) {
            return cmd.getOptionValue(option);
        } else if (required) {
            throw new IllegalArgumentException(getStringForRequiredArg(option));
        } else {
            return defaultValue;
        }
    }

    private String getOptionValue(CommandLine cmd, String option, boolean required) {
        return getOptionValue(cmd, option, required, null);
    }

    private RejectedExecutionHandler determineQueuePolicy(CommandLine cmd) {
        int queuePolicy = Integer.parseInt(getOptionValue(cmd, "queuePolicy", false, "1"));
        switch (queuePolicy) {
            case 0:
                return new ThreadPoolExecutor.AbortPolicy();
            case 1:
                return new ThreadPoolExecutor.CallerRunsPolicy();
            case 2:
                return new ThreadPoolExecutor.DiscardPolicy();
            case 3:
                return new ThreadPoolExecutor.DiscardOldestPolicy();
            default:
                throw new RuntimeException("Invalid queue policy");
        }
    }

    private static Options getOptions() {
        Options options = new Options();

        Option nginxLogPath = new Option(null, "nginxLogPath", true, "Path to the nginx log file");
        nginxLogPath.setRequired(true);
        options.addOption(nginxLogPath);

        Option logFormat = new Option(null, "logFormat", true, "Format of the Nginx log structure (default: \"[$requestTime]\" \"$requestUrl\" \"$statusCode\" \"$refererHeader\" \"$userAgentHeader\" \"$destinationHost\" \"$responseTime\")");
        logFormat.setRequired(false);
        options.addOption(logFormat);

        Option formatTime = new Option(null, "formatTime", true, "Nginx log time format (default: \"dd/MMM/yyyy:HH:mm:ss Z\")");
        formatTime.setRequired(false);
        options.addOption(formatTime);

        Option destinationHost = new Option(null, "destinationHost", true, "Host to send requests");
        destinationHost.setRequired(false);
        options.addOption(destinationHost);

        Option httpProtocol = new Option(null, "httpProtocol", true, "HTTP protocol for requests (default: https)");
        httpProtocol.setRequired(false);
        options.addOption(httpProtocol);

        Option resultFilePath = new Option(null, "resultFilePath", true, "Name of the file to save the result (default: nginx.log)");
        resultFilePath.setRequired(false);
        options.addOption(resultFilePath);

        Option speed = new Option(null, "speed", true, "Acceleration/deceleration of request sending speed, eg: 2, 0.5 (default: 1)");
        speed.setRequired(false);
        options.addOption(speed);

        Option scaleLoad = new Option(null, "scaleLoad", true, "Scale load (default: 1.0)");
        scaleLoad.setRequired(false);
        options.addOption(scaleLoad);

        Option startTimestamp = new Option(null, "startTimestamp", true, "Start replaying the log from a specific timestamp");
        startTimestamp.setRequired(false);
        options.addOption(startTimestamp);

        Option timeout = new Option(null, "timeout", true, "Timeout for the requests (default: 30)");
        timeout.setRequired(false);
        options.addOption(timeout);

        Option parserThreads = new Option(null, "parserThreads", true, "Count of parser threads (default: Max cores count or 16)");
        parserThreads.setRequired(false);
        options.addOption(parserThreads);

        Option requestsThreads = new Option(null, "requestsThreads", true, "Count of requests threads (default: Max cores count or 16)");
        requestsThreads.setRequired(false);
        options.addOption(requestsThreads);

        Option requestQueueCapacity = new Option(null, "requestQueueCapacity", true, "Capacity of request queue (default: 1000)");
        requestQueueCapacity.setRequired(false);
        options.addOption(requestQueueCapacity);

        Option queuePolicy = new Option(null, "queuePolicy", true, "Policy of request queue (AbortPolicy: 0, CallerRunsPolicy: 1, DiscardPolicy: 2, DiscardOldestPolicy: 3) (default: 1)");
        queuePolicy.setRequired(false);
        options.addOption(queuePolicy);

        Option ignoreSsl = new Option(null, "ignoreSsl", false, "Ignore SSL (default: false)");
        ignoreSsl.setRequired(false);
        options.addOption(ignoreSsl);

        Option closeConnectionAfterFirstByte = new Option(null, "closeConnectionAfterFirstByte", false, "Close connection with host after getting first byte (default: false)");
        closeConnectionAfterFirstByte.setRequired(false);
        options.addOption(closeConnectionAfterFirstByte);

        Option disableStats = new Option(null, "disableStats", false, "Disable display the execution progress in the console (default: false)");
        disableStats.setRequired(false);
        options.addOption(disableStats);

        Option disableWriteToFile = new Option(null, "disableWriteToFile", false, "Disable write results to file (default: false)");
        disableWriteToFile.setRequired(false);
        options.addOption(disableWriteToFile);

        Option username = new Option(null, "username", true, "Username for basic auth");
        username.setRequired(false);
        options.addOption(username);

        Option password = new Option(null, "password", true, "Password for basic auth");
        password.setRequired(false);
        options.addOption(password);

        Option help = new Option("h", "help", false, "display help for command");
        help.setRequired(false);
        options.addOption(help);

        return options;
    }

    public Path getNginxLogPath() {
        return nginxLogPath;
    }

    public String getLogFormat() {
        return logFormat;
    }

    public DateTimeFormatter getFormatTime() {
        return formatTime;
    }

    public String getDestinationHost() {
        return destinationHost;
    }

    public String getHttpProtocol() {
        return httpProtocol;
    }

    public Float getSpeed() {
        return speed;
    }

    public Float getScaleLoad() {
        return scaleLoad;
    }

    public Long getStartTimestamp() {
        return startTimestamp;
    }

    public int getTimeout() {
        return timeout;
    }

    public Integer getParserThreads() {
        return parserThreads;
    }

    public Integer getRequestsThreads() {
        return requestsThreads;
    }

    public int getRequestQueueCapacity() {
        return requestQueueCapacity;
    }

    public RejectedExecutionHandler getQueuePolicy() {
        return queuePolicy;
    }

    public boolean isIgnoreSsl() {
        return ignoreSsl;
    }

    public boolean isCloseConnectionAfterFirstByte() {
        return closeConnectionAfterFirstByte;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}