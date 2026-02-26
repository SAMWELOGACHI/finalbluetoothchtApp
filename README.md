
                 Problem Statement
Modern communication relies heavily on internet connectivity, but in many environments (e.g., remote areas, emergencies, or offline situations), network access is limited or unavailable. There is a need for a lightweight communication tool that allows users to chat without depending on mobile data or Wi-Fi.
The goal of this project is to design and develop a Bluetooth-enabled chat platform that enables two or more devices to exchange messages securely and efficiently using Bluetooth technology. The system should provide a simple user interface, reliable message delivery, device discovery, and seamless connection management.
 
                 Functional Requirements (What the system should do)
1. Device Discovery
         •	The system shall scan for nearby Bluetooth-enabled devices.
         •	The system shall display a list of available devices for the user to connect to.
2. Connection Management
         •	The system shall allow users to establish a secure Bluetooth connection with another device.
         •	The system shall notify the user when the connection is successful or fails.
         •	The system shall allow users to disconnect from a device.
3. User Authentication / Identification
         •	The system shall allow users to set a display name or username.
         •	The system shall identify connected devices by their Bluetooth name.
4. Chat Messaging
         •	The system shall allow users to send text messages over Bluetooth.
         •	The system shall receive and display incoming messages in real time.
         •	The system shall maintain a chat history during the active session.
5. Notifications
         •	The system shall alert the user when a new message arrives.
         •	The system shall notify users when the other device disconnects.
6. Error Handling
         •	The system shall inform users when Bluetooth is turned off.
         •	The system shall display appropriate error messages for failed connections or message delivery.


                 Non-Functional Requirements (How the system should behave)
1. Performance
        •	Message delivery should not exceed 1–2 seconds under normal Bluetooth signal conditions.
        •	The system should be able to handle at least one active chat session without lag.
2. Security
        •	The system should encrypt messages using Bluetooth’s built-in encryption.
        •	The system should not store user data permanently unless required.
3. Usability
        •	The user interface must be simple and easy to understand.
        •	The app should guide users to enable Bluetooth if it is turned off.
4. Reliability
        •	The connection should remain stable as long as both devices are within Bluetooth range.
        •	The app should automatically attempt reconnection if the signal drops temporarily.
5. Compatibility
        •	The system should run on Android / iOS (choose depending on your assignment).
        •	The system should support devices with Bluetooth 4.0 or later.
6. Maintainability
        •	The code should be modular and documented for easy updates.
        •	The system should allow future extensions (e.g., file sharing, group chat).
7. Scalability (Optional)
        •	The design should consider future support for multi-device group chats.


