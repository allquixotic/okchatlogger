# README for OKChatLogger

## Overview
OKChatLogger is a Kotlin-based GUI application designed to capture and log chat messages from a designated chat window on your screen. It periodically takes screenshots of the selected chat area, uses OCR (Optical Character Recognition) to extract text, and appends the text to a log file. This guide covers how to set up and use OKChatLogger.

## Features
- Customizable screenshot interval
- Selectable chat window region
- Integration with Microsoft Azure's Computer Vision API for OCR
- Real-time chat logging

## User Prerequisites
- Microsoft Azure account with Computer Vision API access (instructions under **Setting Up** section)
- To use pre-built executables:
  - **For a PC:** Windows 10 or 11 64-bit, and a recent Intel or AMD CPU
  - **For a Mac:** macOS Sonoma or later with Apple Silicon SoC (M1, M2, M3, etc.)
- To use the JAR release:
  - OpenJDK 21 or later (I recommend the latest LTS of [Azul Zulu](https://www.azul.com/downloads/#downloads-table-zulu))

## Setting Up
### Registering with Microsoft Azure
1. **Create an Azure Account**: Sign up at [Azure Portal](https://portal.azure.com/).
2. **Create a Resource**: Navigate to "Create a resource" and search for "Computer Vision". Follow the setup process.
3. **Get API Key**: Once the resource is created, go to the resource page, and note down your endpoint URL and API key.
4. **Enable Billing**: If you intend to use this tool more than the free tier limit of 5,000 images per month, you should put in your billing information in the Azure portal. See [pricing](https://azure.microsoft.com/en-us/pricing/details/cognitive-services/computer-vision/) for Computer Vision. Follow [this guide](https://learn.microsoft.com/en-us/azure/cost-management-billing/manage/change-credit-card) to manage your billing information in the Azure Portal.

### Running the Application
Execute the JAR file generated in the `build/libs` directory, or run the application through your IDE. You can also use Gradle to run the application with `./gradlew run`.

If you downloaded a native binary, simply double-click it or run it as you would any other application on your platform.

## Configuration
### Input Fields
- **Interval (sec)**: Set the time interval in seconds for taking screenshots.
- **Log File Path**: Enter the path where the chat log will be saved.
- **X, Y, Width, Height**: Specify the coordinates and size of the chat window region to capture.
- **Endpoint**: Paste the Azure Computer Vision endpoint URL from the Azure portal. This URL usually takes the form of `https://your_domain.cognitiveservices.azure.com` where `your_domain` is an instance name you are prompted to generate in the Azure portal during setup.
- **API Key**: Enter the Azure Computer Vision API key. This is usually a long string of letters ("a" through "h") and numbers.

### Start/Stop Logging
- Click **Start Logging** to begin capturing and logging chat.
- Click **Stop Logging** to halt the process.

## Building from Source
You only need to build from source if the binaries provided are out of date with the latest source code (and you need a bug fix or feature from the latest source), or if I didn't provide binaries for your platform.
Here are the instructions to build from source, in any case:

### Build Prerequisites
- Java JDK 17 or later (OpenJDK based VMs should work)
- Git
- Gradle, Kotlin. etc. are not required, as the Gradle wrapper (gradlew) will take care of acquiring them for you.

1. **Clone the Repository**: `git clone https://github.com/allquixotic/okchatlogger.git`
2. **Navigate to the Project Directory**: `cd okchatlogger`
3. **Build with Gradle**:
- `./gradlew shadowJar` will produce an all-inclusive JAR file.
- `./gradlew build` will produce a conventional JAR file with the dependency libraries excluded.
- `./gradlew instrument` will produce a native binary for your platform if you have [Liberica NIK](https://bell-sw.com/liberica-native-image-kit/) or [GraalVM](https://www.graalvm.org/). The native binary starts faster and doesn't require a JVM to run.

This will compile the project and create an executable JAR or native binary file.

## Troubleshooting
Ensure all fields are correctly filled out and that the Azure API key and endpoint are valid. If you encounter issues, consult the log output in the application for errors.

## License
Distributed under the Apache License 2.0. See `LICENSE.txt` in the repository for details.

## Repository
Source code is available at: [https://github.com/allquixotic/okchatlogger](https://github.com/allquixotic/okchatlogger)

---

**Note:** This README is a general guide. For specific issues or more detailed information, refer to the repository's documentation or submit an issue on GitHub.

