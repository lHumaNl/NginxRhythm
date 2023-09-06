# NginxRhythm

NginxRhythm is a utility to parse and simulate requests from an Nginx log file.

## Prerequisites

- Java 8 or newer.

## Usage

java -jar NginxRhythm.jar [options]

## Options

- `--nginxLogPath <path>`: **(Required)** Path to the Nginx log file.
- `--logFormat <format>`: Format of the Nginx log structure. Default is `"[$requestTime]" "$requestUrl" "$statusCode" "$refererHeader" "$userAgentHeader" "$destinationHost" "$responseTime"`.
- `--formatTime <format>`: Nginx log time format. Default is `dd/MMM/yyyy:HH:mm:ss Z`.
- `--destinationHost <host>`: Host to send requests to.
- `--httpProtocol <protocol>`: HTTP protocol for requests. Default is `https`.
- `--resultFilePath <path>`: Name of the file to save the result. Default is `nginx.log`.
- `--speed <factor>`: Acceleration/deceleration of request sending speed. For example: `2`, `0.5`. Default is `1`.
- `--scaleLoad <factor>`: Scale load. Default is `1.0`.
- `--startTimestamp <timestamp>`: Start replaying the log from a specific timestamp.
- `--timeout <seconds>`: Timeout for the requests in seconds. Default is `30`.
- `--parserThreads <count>`: Count of parser threads. Default is the higher of available processor cores or `8`.
- `--requestsThreads <count>`: Count of request threads. Default is the higher of available processor cores or `8`.
- `--ignoreSsl`: If specified, will ignore SSL. Default is `false`.
- `--disableStats`: If specified, will disable displaying the execution progress in the console. Default is `false`.
- `--disableWriteToFile`: If specified, will disable writing results to file. Default is `false`.
- `--username <username>`: Username for basic authentication.
- `--password <password>`: Password for basic authentication.
- `-h`, `--help`: Display help for the command.

## Examples

Parse a specific Nginx log file and send requests to a specified host:

java -jar NginxRhythm.jar --nginxLogPath /path/to/nginx.log --destinationHost example.com

Replay the logs from a specific timestamp with a custom log format:

java -jar NginxRhythm.jar --nginxLogPath /path/to/nginx.log --startTimestamp 1691387480000 --logFormat "[$requestTime] $requestUrl"

## Contributing

For any suggestions or bug reports, please open an issue on GitHub.

## Author

lHumaNl
