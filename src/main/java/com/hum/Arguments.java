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
    private final String username;
    private final String password;

    public Arguments(String[] args) throws ParseException, URISyntaxException {
        Options options = getOptions();

        CommandLine cmd = new DefaultParser().parse(options, args);

        if (cmd.hasOption("h") || cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("NginxRhythm", options);
            System.exit(0);
        }

        String defaultFormatTime = "\"[$requestTime]\" \"$requestUrl\" \"$statusCode\" \"$refererHeader\" \"$userAgentHeader\" \"$destinationHost\" \"$responseTime\"";

        this.nginxLogPath = Paths.get(cmd.getOptionValue("nginxLogPath"));
        this.logFormat = cmd.hasOption("logFormat") ? cmd.getOptionValue("logFormat") : defaultFormatTime;
        this.formatTime = cmd.hasOption("formatTime") ? DateTimeFormatter.ofPattern(cmd.getOptionValue("formatTime"), Locale.ENGLISH) : DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
        this.destinationHost = cmd.hasOption("destinationHost") ? cmd.getOptionValue("destinationHost") : null;
        this.httpProtocol = cmd.hasOption("httpProtocol") ? cmd.getOptionValue("httpProtocol") : "https";

        String defaultLogName;
        if (this.destinationHost != null) {
            defaultLogName = this.destinationHost.replace("https://", "").replace("http://", "") + "-nginx.log";
        } else {
            defaultLogName = "nginx.log";
        }

        Path resultFilePath = cmd.hasOption("resultFilePath") ? Paths.get(cmd.getOptionValue("resultFilePath")) : Paths.get(defaultLogName);
        this.speed = cmd.hasOption("speed") ? Float.parseFloat(cmd.getOptionValue("speed")) : null;
        this.scaleLoad = cmd.hasOption("scaleLoad") ? Float.parseFloat(cmd.getOptionValue("scaleLoad")) : null;
        this.startTimestamp = cmd.hasOption("startTimestamp") ? Long.parseLong(cmd.getOptionValue("startTimestamp")) : null;
        this.timeout = cmd.hasOption("timeout") ? Integer.parseInt(cmd.getOptionValue("timeout")) : 30;
        this.parserThreads = cmd.hasOption("parserThreads") ? Integer.parseInt(cmd.getOptionValue("parserThreads")) : Math.max(Runtime.getRuntime().availableProcessors(), 16);
        this.requestsThreads = cmd.hasOption("requestsThreads") ? Integer.parseInt(cmd.getOptionValue("requestsThreads")) : Math.max(Runtime.getRuntime().availableProcessors(), 16);
        this.requestQueueCapacity = cmd.hasOption("requestQueueCapacity") ? Integer.parseInt(cmd.getOptionValue("requestQueueCapacity")) : 1000;

        int queuePolicy = cmd.hasOption("queuePolicy") ? Integer.parseInt(cmd.getOptionValue("queuePolicy")) : 1;

        if (queuePolicy == 0) {
            this.queuePolicy = new ThreadPoolExecutor.AbortPolicy();
        } else if (queuePolicy == 1) {
            this.queuePolicy = new ThreadPoolExecutor.CallerRunsPolicy();
        } else if (queuePolicy == 2) {
            this.queuePolicy = new ThreadPoolExecutor.DiscardPolicy();
        } else if (queuePolicy == 3) {
            this.queuePolicy = new ThreadPoolExecutor.DiscardOldestPolicy();
        } else {
            throw new RuntimeException("Invalid queue policy");
        }

        this.ignoreSsl = cmd.hasOption("ignoreSsl");
        this.username = cmd.hasOption("username") ? cmd.getOptionValue("username") : null;
        this.password = cmd.hasOption("password") ? cmd.getOptionValue("password") : null;

        System.setProperty("logFilePath", resultFilePath.toString());

        if (cmd.hasOption("disableWriteToFile")) {
            System.setProperty("fileLogLevel", "OFF");
        } else {
            System.setProperty("fileLogLevel", "INFO");
        }

        if (cmd.hasOption("disableStats")) {
            System.setProperty("consoleLogLevel", "OFF");
        } else {
            System.setProperty("consoleLogLevel", "INFO");
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

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}