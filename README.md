# Dronazon - DPS course project 2021

## 1 Description of the project

The aim of the project is to create a system that manages the deliveries of orders received from the e-commerce site Dronazon, through the use of drones. Dronazon has at its disposal a flock of drones distributed within a smart-city to manage its deliveries. Smart city drones must organize themselves to elect a master drone. Each time a Dronazon customer places a new order, this information will be passed to the smart-city master drone. The master drone will then determine which drone will be responsible for this delivery. Each drone has installed a sensor that detects the level of air pollution. Furthermore, each delivery implies a battery drain. When a drone's remaining battery level is below 15%, the drone is forced to leave the system. Periodically, the drones must communicate to a remote server, called the Administrator Server, the information relating to the number of recorded deliveries, the number of kilometers traveled, the detected air pollution level and the remaining battery level. Dronazon administrators will be able to monitor their delivery system through the Administrator Server. Furthermore, through the Administrator Server it is also possible to register and remove drones from the system dynamically.

## 2 Implementation of the applications

For the project, the implementation of the following applications is required:

- Dronazon: application that simulates an e-commerce site, generating new deliveries from having to make the information relating to them to the flock of drones
- Drone: a specific drone of the system
- Administrator Server: REST server that receives statistics from drones and allows dynamic management of the drone network

Note that each Drone is a process of its own, and therefore does not need to be implemented as a thread. Details on the applications to be implemented are provided below.

## 3 Internal representation of the smart-city

The smart city is represented as a 10x10 grid. Each cell of the grid represents one square kilometer of the smart city. Upon registration within the system, a drone will be randomly placed in a chosen cell. For simplicity, a single cell can hold multiple drones.

## 4 Dronazon

Dronazon is a process that simulates an e-commerce site. In particular, this process must generate a new order for which a delivery is required every 5 seconds. Each order is characterized by:

- ID
- Pick up point
- Delivery point

Collection and delivery points are expressed as Cartesian coordinates of a cell of the grid representing the smart-city. Orders generated by Dronazon must be communicated via the MQTT protocol.

For simplicity, it is assumed that the smart-city MQTT broker is active at the following address tcp: // localhost: 1883. Dronazon must connect to that broker as an MQTT client and will assume the role of publisher. Whenever a new order is generated, Dronazon publishes this update on the following MQTT topic: dronazon / smartcity / orders /.

## 5 Drone

Each drone is simulated by a process that it deals with

- coordinate with other drones via gRPC to:
    - elect the smart-city master drone through the Chang & Roberts ring-based election algorithm
    - send statistics to the administrator server
- make the deliveries assigned by the master drone
- exit the system when your battery level is below 15%

### 5.1 Network structure

The drone network has a ring structure. The creation of this network must be managed in a decentralized way, without being guided by the administrator server. It is therefore necessary to take into account the possible distributed synchronization problems that may occur at the entrance and exit of the ring.

### 5.2 Initialization

A drone must be initialized by specifying

- ID
- Listening port for communication between drones
- Admin server address

Once started, the drone process must register with the system via the administrator server. If the entry is successful (that is, there are no other drones with the same ID), the drone receives from the server:

- one's starting position in the smart-city (ex: (8,6))
- the list of drones already present in the smart-city

Having received this information, the drone must then enter the ring network in a decentralized manner, without the help of the administrator server.
When the insertion of the drone into the smart-city is successful, it will have to start its own sensor for detecting air pollution. In the event that there are no other drones within the smart-city, the drone proclaims itself as a master. Otherwise, the drone must introduce itself to the other drones of the smart-city by sending its position in the grid and understanding who the master drone is. During this presentation phase, ildrone does not need to communicate its battery level as it is assumed to be 100%.

### 5.3 Battery consumption

At the end of each delivery a drone will lose a battery percentage equal to 10%. Note that for simplicity it is assumed that the percentage of battery consumed for a delivery is calculated only at the end of the latter.

### 5.4 Explicit closing

It is assumed that each drone can only terminate in a controlled manner. Specifically, each drone asks the admin server to exit the system as soon as its battery level is below 15%. Furthermore, a drone can independently request to exit the network via an explicit request (e.g., quit) from the command line. In both cases, to exit the network a non-master drone must perform the following steps:


1. complete any delivery you are dealing with, then communicating the information described in Section 5.6 to the master drone.
2. forcibly close communications with other drones, without bothering to communicate their exit from the network to them
3. ask the administrator server to log out of the network

If, on the other hand, the drone in question is the master, it is necessary that it takes the following steps:

1. complete any delivery you are dealing with, by storing the information described in Section 5.6.
2. log out of the smart-city MQTT broker
3. make sure to assign pending deliveries to smart-city drones
4. forcibly close communications with other drones, without bothering to communicate their exit from the network to them
5. send the global statistics of the smart city to the administrator server
6. Ask the administrator server to log out of the system

In order to simplify the development of the project, it is not necessary to manage the deliveries generated by Dronazon in the time interval between the current master drone leaving the network and the election of the next one. This implies that such deliveries will be lost. The drones in the network must be able to detect the absence of another drone. In this case, they will have to rebuild the ring. When a drone notices the absence of the master, it must initiate the election of a new master drone using the ChangandRoberts ring-based algorithm. As the master drone, the drone with the highest remaining battery level must be elected. In case of a tie, the drone with the highest ID is chosen. Note that if a drone stands
when making a delivery during an election, you must consider as the battery level what it will have at the end of the delivery. As soon as a new master drone is elected, all other drones will have to communicate their position within the smart city and their battery level. The detection of the absence of a drone, the reconstruction of the ring and the ring-based election must not be guided in any way by the administrator server.

### 5.5 Pollution sensors

Each pollution sensor periodically produces measurements relating to the level of fine dust in the air (PM10). Each individual sensor measurement is characterized by:

- PM10 value read
- Timestamp in milliseconds

The generation of these measurements is carried out by suitable simulators. Each simulator assigns the number of seconds since midnight as a timestamp to the measurements. In the initialization phase, each drone will then have to take care of starting the simulator necessary to generate sensor measurements. Each simulator is a thread consisting of an infinite loop that periodically simulates (with predefined frequency) the measurements, adding them to a data structure. Of this data structure, only the interface (Buffer) is provided, which exposes two methods:

- void add (M easurement m)
- List <M easurement> readAllAndClean ()

it is therefore necessary to create a class that implements the interface. Each drone will have a single sensor and therefore a single buffer. The simulation threads use the addMeasurement method to fill the data structure. Instead, the readAllAndClean method must be used to get all the measurements contained in the data structure. At the end of a reading, readAllAndClean has to take care of clearing the buffer in order to make room for new measurements. In particular, the sensor data must be processed using the sliding window technique, considering a buffer size of 8 measurements and an overlap of 50%. When the buffer size reaches 8 measurements, the data in the buffer should be averaged. The averages calculated in this way will then be transmitted together with the information on deliveries.

### 5.6 Distributed Synchronization

5.6.1 Delivery management

Whenever Dronazon generates a new delivery to be made, this is published by the e-commerce site via the MQTT protocol on the topic dronazon / smartcity / orders / (see Section 4). When a new master is elected, this one, before carrying out any operation, must wait to receive the position of the other drones of the smart-city, without the support of the administrator server. Subsequently, the master drone must receive the smart-city broker MQTT and register as a subscriber to the topic dronazon / smartcity / orders /, to receive new deliveries on the information. When a master drone receives a request for a new delivery, it will assign it to the smart-city drone (including itself) that meets the following criteria:

- the drone must not already be engaged in another delivery
- among the drones that meet the previous criteria, the one closest to the place of collection of the order with the highest level of remaining battery is chosen
- in the event that more than one drone meets all the criteria listed in the delivery, the drone with the highest ID will be selected for the

To calculate the distance between two points P1 = (x1, y1) and P2 = (x2, y2) of the smart-city, it is necessary to use the following formula:

``
d (P1, P2) = ??? ((x2 - x1) ^ 2 + (y2 - y1) ^ 2)
``
####
If there are no drones available to make a delivery, this is placed in a special queue managed by the master drone. Whenever a drone completes a delivery by communicating its statistics to the master drone, the latter uses the criteria previously described for pending deliveries that are in the queue. When a drone receives a delivery to be made, the time taken to deliver the order is simulated by a 5-second Thread.sleep (). After delivery, the drone communicates some information to the master, which will be delivered in Section 5.6.2. For simplicity, it is assumed that a drone always successfully completes the delivery assigned to it.

5.6.2 Calculation of drone information

Each time a drone makes a delivery it must transmit the following information to the master drone:

- timestamp of arrival at the place of delivery
- new position of the drone which coincides with the delivery position
- kilometers traveled to reach the collection and delivery points
- averages of measurements relating to the level of air pollution detected starting from the last delivery made (see Section 5.5)
- your remaining battery level

This information will then be used by the master drone to calculate the global smart-city statistics. Every 10 seconds a drone must print on the screen: the total number of deliveries made, the kilometers traveled and the percentage of remaining battery.

5.6.3 Sending information to the server

The drones must coordinate to periodically send the global statistics of the smart city to the Administrator Server. The master drone will calculate the global statistics every 10 seconds, considering the average of the information received by the individual drones at the time of their deliveries (see Section 5.6.2). Specifically, the global statistics are as follows:

- average number of deliveries made by drones
- average of the kilometers traveled by drones
- average of the level of pollution detected by drones
- average of the residual battery level of the drones

When the master drone finishes calculating the global statistics, it sends them to the admin server along with the timestamp in which the statistics were calculated.

## 6 Server Administrator

The Administrator Server is responsible for maintaining an internal representation of the smart-city and for receiving global statistics from the master drone (see Section 5.6.3). This information will then be queried by the Directors. This server must therefore offer two different REST interfaces for:

- manage the drone network and receive statistics
- allow administrators to carry out queries

### 6.1 Interface for drones

6.1.1 Insertion

When a drone wants to enter the system, it must communicate to the administrator server:

- ID
- IP address
- Port number on which it is available to communicate with other drones


At the time of registration within the system, a drone will be placed in a cell of the smart-city, chosen at random. It is possible to add a drone to the network only if there is no other drone with the same identifier. If the entry is successful, the administrator server returns to the drone the list of drones already present in the smart-city, specifying for each the ID, IP address and port number for communication. Note that the insertion of the new drone into the ring network must be managed in a distributed manner, without the support of the administrator server.

6.1.2 Removal

When a drone notices that its battery level has dropped below 15% it must ask the admin server to leave the network. Furthermore, a drone can independently request to exit the network via an explicit request (e.g., quit) from the command line. When the administrator server receives a removal request from a drone, it must remove it from the smart city. It is important to note that the administrator server will only have to take care of removing the drone from its data structure that represents the smart-city. The other drones will have to independently notice the exit of a drone and, consequently, be able to reconstruct the ring.

6.1.3 Statistics

The administrator server must set up an interface to receive the global statistics of the smart city from the master drone. It is sufficient to store this information in appropriate data structures that allow for subsequent analyzes.

### 6.2 Interface for administrators

The administrator server must provide methods to obtain the following information:

- The list of drones on the network
- Last global statistics (with timestamps) related to the smart-city (see Section 5.6.3)
- Average number of deliveries made by smart-city drones between two timestamps t1 and t2
- Average of the kilometers traveled by the smart-city drones between two timestamps t1 and t2


## 7 Administrator

The administrator application is a command line interface that deals with interacting with the REST interface provided by the administrator server. The application must then show the administrator a simple menu to choose one of the administrator services offered by the server (see Section 6.2), with the ability to enter any required parameters.

## 8 Simplifications and limitations

Please note that the purpose of the project is to demonstrate the ability to design and implement a distributed and pervasive application. Therefore aspects not related to the communication protocol, competition and sensor data management are considered secondary.
Furthermore, it can be assumed that:

- no knots behave in a mischievous way,
- no process ends uncontrollably

Instead, any possible data entry errors by the user should be managed. Furthermore, the code must be robust: all possible exceptions must be handled correctly.
Although the Java libraries provide multiple classes for handling concurrency situations, for educational purposes students are encouraged to make only use of the methods and classes explained during the laboratory course. Therefore, any necessary synchronization data structures (such as locks, semaphores or shared buffers) will have to be implemented from scratch and will be discussed during the project presentation. For the communication between processes it is necessary to use the gRPC framework. If broadcast communications are envisaged, these must be carried out in parallel and not sequentially.

## 9 Optional parts

### 9.1 First part

In the first optional part, the system must be extended to give the drones the possibility to recharge their battery level. This operation can be carried out by only one drone at a time. It is possible to request to recharge the battery level of a drone through an explicit request from the command line (e.g., recharge). When this event occurs, the drone must ask the other drones to be able to recharge its battery, using the Ricart and Agrawala algorithm. When a drone is given the ability to recharge, it will simulate the recharging process with a 10 second Thread.sleep (). During this phase, the drone cannot receive deliveries to be made. At the end of the charging phase, the drone communicates to the smart-city master:

- its new position within the smart-city, or (0.0). This is the location where drones can recharge their battery level
- its new remaining battery level, i.e. 100%

### 9.2 Second part

In the development of the project we assumed that the processes end only in a controlled manner. In this optional part, this assumption lapses for drones. Therefore, it will be necessary to implement a decentralized distributed protocol that allows the network to understand when a drone has finished its execution in an uncontrolled manner. In these cases, the drone network must update itself and notify the administrator server of the uncontrolled exit of the drone from the smart-city. All the distributed synchronization mechanisms envisaged by the project must take into account the possible uncontrolled termination of the other drones.

