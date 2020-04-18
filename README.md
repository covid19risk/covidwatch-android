![](https://github.com/covid19risk/covidwatch-android/workflows/Develop%20Branch%20CI/badge.svg)

# CovidWatch Android POC
Android POC for www.covid-watch.org

BLE is a low power protocol with cross-platform OS support for background operation. The app will be able to constantly run, detecting and logging all contact events with other nearby app users.

The approach currently being investigated utilizes BLE functionality for background advertisement and scanning. Due to different system requirements for Android and iOS, the protocol works differently depending on the operating systems of the devices involved. The key challenges are:

  1) iOS devices acting as peripherals in the background can only be found by centrals that are scanning for their specific service UUID. These peripherals must establish a connection to transfer any data.
  2) Android devices have an unfixed bug (https://issuetracker.google.com/issues/125138967) where subsequent connections with many devices can cause the bluetooth system to lock up.

The current solution is a hybrid model that is asymmetric for communication between iOS and Android. All devices will simultaneously act as peripherals and centrals, but only some devices will be able to detect others, and only some devices will need to establish a connection to exchange data. 

Android Peripheral
- Android devices will act as BLE peripherals in the background and they will advertise a service UUID specific to this app.
- In the advertisement packet they will use the service data field [https://developer.android.com/reference/android/bluetooth/le/AdvertiseData.Builder] to advertise a randomly generated Contact Event Number. 
- They will log all previously advertised CENs, and periodically update the CEN when the app is woken up for Bluetooth or timer events.
- Functionality will soon be added for iOS centrals to connect to Android peripherals. During this process the iOS central will transmit a CEN and the Android device will log it.

iOS Peripheral
- iOS devices will act as BLE peripherals in the background and they will advertise a service UUID specific to this app with a readable characteristic exposed. 
- If a central establishes a connection and requests to read the characteristic field the peripheral will randomly generate a CEN, log it locally, and transmit it to the central.

Android Central
- Android devices will act as BLE centrals in the background and they will scan for the service UUID specific to this app. 
- When they read an advertisement packet from an Android peripheral they will log the information in the service data field as the CEN. 
- Android centrals are unable to detect iOS peripherals.

iOS Central
- iOS devices will act as BLE centrals in the background and they will scan for the service UUID specific to this app. 
- When they read an advertisement packet from an Android peripheral they will log the attached service field as the CEN. 
- When they read an advertisement packet from an iOS peripheral they will establish a connection and request the characteristic field. They will then log the returned characteristic field as the CEN and disconnect.
- Functionality will soon be added for iOS centrals to connect to Android peripherals. During this process the iOS central will transmit a CEN and the Android device will log it.

Under the current model, Android devices will be able to detect other Android devices and iOS devices will be able to detect all devices. The asymmetry of the model is not ideal, but all phones will be able to locally share a Contact Event Number when in the vicinity of other phones, so the requirements specified in the privacy model are achieved. For Android to Android or iOS to iOS detection, two contact event numbers will likely be generated for each interaction (because each phone acts as both a peripheral and a central). This can be trivially adjusted for in the risk model. 

In an upcoming update, the ability for iOS centrals to write to Android peripherals will be added. This will enable symmetric communication between all device types and enable an updated cryptographic system.


