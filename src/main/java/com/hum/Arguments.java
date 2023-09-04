package com.hum;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

public class Arguments {
    private final Path nginxLogPath;
    private final String logFormat;
    private final SimpleDateFormat formatTime;
    private final String destinationHost;
    private final Path resultFilePath;
    private final Float speed;
    private final Float scaleLoad;
    private final Long startTimestamp;
    private final int timeout;
    private final boolean ignoreSsl;
    private final boolean isStats;
    private final boolean isWriteToFile;
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

        String defaultFormatTime = "\"[$time_local]\" \"$request\" \"$status\" \"$remote_addr\" \"$http_referer\" \"$http_user_agent\" \"$request_time\"";

        this.nginxLogPath = Paths.get(new URI(cmd.getOptionValue("nginxLogPath")));
        this.logFormat = cmd.hasOption("logFormat") ? cmd.getOptionValue("logFormat") : defaultFormatTime;
        this.formatTime = cmd.hasOption("formatTime") ? new SimpleDateFormat(cmd.getOptionValue("formatTime")) : new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");
        this.destinationHost = cmd.hasOption("destinationHost") ? cmd.getOptionValue("destinationHost") : null;
        this.resultFilePath = cmd.hasOption("resultFilePath") ? Paths.get(cmd.getOptionValue("resultFilePath")) : Paths.get("nginx.log");
        this.speed = cmd.hasOption("speed") ? Float.parseFloat(cmd.getOptionValue("speed")) : null;
        this.scaleLoad = cmd.hasOption("scaleLoad") ? Float.parseFloat(cmd.getOptionValue("scaleLoad")) : null;
        this.startTimestamp = cmd.hasOption("startTimestamp") ? Long.parseLong(cmd.getOptionValue("startTimestamp")) : null;
        this.timeout = cmd.hasOption("timeout") ? Integer.parseInt(cmd.getOptionValue("timeout")) : 30;
        this.ignoreSsl = cmd.hasOption("ignoreSsl") && Boolean.parseBoolean(cmd.getOptionValue("ignoreSsl"));
        this.isStats = cmd.hasOption("isStats") && Boolean.parseBoolean(cmd.getOptionValue("isStats"));
        this.isWriteToFile = !cmd.hasOption("isWriteToFile") || Boolean.parseBoolean(cmd.getOptionValue("isWriteToFile"));
        this.username = cmd.hasOption("username") ? cmd.getOptionValue("username") : null;
        this.password = cmd.hasOption("password") ? cmd.getOptionValue("password") : null;

        System.setProperty("logFilePath", this.resultFilePath.toString());
        System.setProperty("logToFile", Boolean.toString(this.isWriteToFile));
        System.setProperty("logToConsole", Boolean.toString(this.isStats));
    }

    private static Options getOptions() {
        Options options = new Options();

        Option filePath = new Option("nginxLogPath", true, "Path to the nginx log file");
        filePath.setRequired(true);
        options.addOption(filePath);

        Option format = new Option("logFormat", true, "Format of the Nginx log structure (default: \"[$requestTime]\" \"$request\" \"$statusCode\" \"$requestHost\" \"$refererHeader\" \"$userAgentHeader\" \"$responseTime\")");
        format.setRequired(false);
        options.addOption(format);

        Option formatTime = new Option("formatTime", true, "Nginx log time format (default: \"DD/MMM/YYYY:HH:mm:ss Z\")");
        formatTime.setRequired(false);
        options.addOption(formatTime);

        Option prefix = new Option("destinationHost", true, "Host to send requests");
        prefix.setRequired(false);
        options.addOption(prefix);

        Option logFile = new Option("resultFilePath", true, "Name of the file to save the result (default: nginx.log)");
        logFile.setRequired(false);
        options.addOption(logFile);

        Option ratio = new Option("speed", true, "Acceleration/deceleration of request sending speed, eg: 2, 0.5 (default: 1)");
        ratio.setRequired(false);
        options.addOption(ratio);

        Option scaleLoad = new Option("scaleLoad", true, "Scale load (default: 1.0)");
        scaleLoad.setRequired(false);
        options.addOption(scaleLoad);

        Option startTimestamp = new Option("startTimestamp", true, "Start replaying the log from a specific timestamp");
        startTimestamp.setRequired(false);
        options.addOption(startTimestamp);

        Option timeout = new Option("timeout", true, "Timeout for the requests  (default: 30)");
        timeout.setRequired(false);
        options.addOption(timeout);

        Option skipSsl = new Option("ignoreSsl", false, "Ignore SSL (default: false)");
        skipSsl.setRequired(false);
        options.addOption(skipSsl);

        Option isStats = new Option("isStats", false, "Display the execution progress in the console (default: false)");
        isStats.setRequired(false);
        options.addOption(isStats);

        Option isWriteToFile = new Option("isWriteToFile", false, "Write results to file (default: true)");
        isWriteToFile.setRequired(false);
        options.addOption(isWriteToFile);

        Option username = new Option("username", true, "Username for basic auth");
        username.setRequired(false);
        options.addOption(username);

        Option password = new Option("password", true, "Password for basic auth");
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

    public SimpleDateFormat getFormatTime() {
        return formatTime;
    }

    public String getDestinationHost() {
        return destinationHost;
    }

    public Path getResultFilePath() {
        return resultFilePath;
    }

    public float getSpeed() {
        return speed;
    }

    public float getScaleLoad() {
        return scaleLoad;
    }

    public Long getStartTimestamp() {
        return startTimestamp;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isIgnoreSsl() {
        return ignoreSsl;
    }

    public boolean isStats() {
        return isStats;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}