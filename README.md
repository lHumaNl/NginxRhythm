# NginxRhythm

NginxRhythm is a utility to parse and simulate requests from an Nginx log file.

## Cloning the Project from Git

1. **Clone the repository**:
   - Open your terminal or command prompt.
   - Run the following command with your repository URL:

   ```bash
   git clone https://github.com/lHumaNl/NginxRhythm.git
   
2. **Navigate to the project directory**:
   ```bash
   cd path/to/your/project

## Prerequisites

- Java 8 or newer.

## Building the Project

### Building with IntelliJ IDEA

1. **Open the Project**:
    - Start IntelliJ IDEA.
    - Click on `File` > `Open`.
    - Navigate to the project directory and select the `pom.xml` file.
    - Click `OK` to open the project.

2. **Build the Project**:
   - Open the `Maven` tool window (usually located on the right side of the IDE).
   - Expand the project's tree to see the `Lifecycle` section.
   - Double-click on `clean` to clean the project, and then on `install` to build the project.
   - After building, the fat-jar `NginxRhythm-{version}.jar` will appear in the `target` directory.

### Building with Maven CLI

1. **Navigate to the Project Directory**:
    - Open your terminal or command prompt.
    - Navigate to the project directory using the `cd` command.

   ```bash
   cd path/to/your/project

2. **Run the following command**:
    ```bash
   mvn clean install
3. **After building, the fat-jar `NginxRhythm-{version}.jar` will appear in the `target` directory**

## Usage

java -jar NginxRhythm.jar [options]

## Options

- `--nginxLogPath <path>`: **(Required)** Path to the Nginx log file.
- `--logFormat <format>`: Format of the Nginx log structure. Default
  is `"[$requestTime]" "$requestUrl" "$statusCode" "$refererHeader" "$userAgentHeader" "$destinationHost" "$responseTime"`.
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

## Vars in log format

- `$requestTime`: **(Required)** Time of request.
- `$requestUrl`: **(Required)** HTTP method and endpoint.
- `$statusCode`: Status code.
- `$destinationHost`: Host.
- `$refererHeader`: Referer header.
- `$userAgentHeader`: User-Agent header.
- `$responseTime`: Response time from Nginx to client.

Other field may be filled by `"-"`

## Examples

Parse a specific Nginx log file and send requests to a specified host:

java -jar NginxRhythm.jar --nginxLogPath /path/to/nginx.log --destinationHost example.com

Replay the logs from a specific timestamp with a custom log format:

java -jar NginxRhythm.jar --nginxLogPath /path/to/nginx.log --startTimestamp 1691387480000 --logFormat "[$requestTime]
$requestUrl"

## Contributing

For any suggestions or bug reports, please open an issue on GitHub.

## Author

lHumaNl
