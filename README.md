# WiFiWizard

This demo app showcases the following:

- How to connect to a WiFi programmatically and appropriate error handling
- How to scan a QR Code using ML Kit
- How to show adverts using AdMob

The app is done completely in Jetpack Compose with an example espresso UI test.

## Table of Contents

- [Introduction](#introduction)
- [Features](#features)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
- [Configuration](#configuration)
- [Contributing](#contributing)
- [License](#license)

## Introduction

Connecting to a WiFi programmatically can be a bit of a challenge and some Android 10 kernals are unfortunately not able to handle it correctly.
The provided solution should be able to handle all, if not most, Android devices and provideds an appropriate error handling and fallback method to maintain a high UX value.

## Features

- Connect to a WiFi programmatically
- QR code scanning for WiFi details
- Fallback methods & error handling
- Links to source code & articles
- AdMob example
- Jetpack Compose example

## Getting Started

To get started with WiFiWizard, follow the instructions below.

### Prerequisites

- Android Studio (version Giraffe or later)
- Git

### Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/lethalmaus/WiFiWizard.git
   ```

2. Open the project in Android Studio. 
3. Sync the project with Gradle files.

## Configuration

Before running the instrumented test, you need to add a known SSID & password to the project. In local.properties add `SSID="YOUR_SSID_NAME"` and `PASS="YOUR_SSID_PASSWORD"` and change the values accordingly.

Before running the release build you will need to register an account on AdMob and add the appropriate IDs to local.properties.

`
ADMOB_ID="ca-app-pub-3940256099942544~3347511713"
BANNER_AD="ca-app-pub-3940256099942544/6300978111"
REWARD_AD="ca-app-pub-3940256099942544/5224354917"
`

## Contributing
Contributions are welcome! If you encounter any issues or have suggestions for improvements, please feel free to submit a pull request or open an issue in the repository.

## License
This project is licensed under the MIT License. Feel free to use, modify, and distribute this code as per the terms of the license.
